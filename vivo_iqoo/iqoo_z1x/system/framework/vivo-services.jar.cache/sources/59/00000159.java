package com.android.server.biometrics.fingerprint;

import android.content.Context;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.IHwBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.JsonReader;
import com.android.internal.logging.MetricsLogger;
import com.android.server.ServiceThread;
import com.android.server.SystemServerInitThreadPool;
import com.android.server.SystemService;
import com.android.server.Watchdog;
import com.android.server.biometrics.fingerprint.AnalysisService;
import com.vivo.face.common.data.Constants;
import com.vivo.fingerprint.analysis.AnalysisEvent;
import com.vivo.fingerprint.analysis.AnalysisSession;
import com.vivo.fingerprint.analysis.IAnalysisEventCallback;
import com.vivo.fingerprint.analysis.IAnalysisService;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import vendor.vivo.hardware.biometrics.analysis.V1_1.IAnalysis;
import vendor.vivo.hardware.biometrics.analysis.V1_1.IAnalysisClientCallback;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class AnalysisService extends SystemService implements IHwBinder.DeathRecipient {
    private static final boolean DEBUG;
    private static final boolean DEBUGGABLE;
    private static final int MAX_EVENT_NAME_LENGTH = 32;
    private static final int MAX_SAVED_SESSION_COUNT = 512;
    private static final int MSG_INSERT_STUB = 1;
    private static final String TAG = "AnalysisService";
    private static final String TAG_EVENT = "event";
    private static final String TAG_EVENTS = "events";
    private static final String TAG_FLAGS = "flags";
    private static final String TAG_ID = "id";
    private static final String TAG_SESSION = "session";
    private static final String TAG_TIME = "time";
    private static final String TAG_TIMEBASE = "timebase";
    private static final String TAG_TYPE = "type";
    private static final String TAG_VALUE = "value";
    private final Map<String, Callback> mCallbackList;
    private IAnalysis mDaemon;
    private IAnalysisClientCallback mDaemonCallback;
    private ArrayDeque<SessionExtend> mFinishedSessions;
    private Handler mHandler;
    private final ServiceThread mHandlerThread;
    private int mReceivedSessionCount;

    static /* synthetic */ int access$008(AnalysisService x0) {
        int i = x0.mReceivedSessionCount;
        x0.mReceivedSessionCount = i + 1;
        return i;
    }

    static {
        DEBUG = SystemProperties.getBoolean("persist.sys.log.ctrl", Build.IS_DEBUGGABLE) || Build.IS_DEBUGGABLE;
        DEBUGGABLE = SystemProperties.getBoolean("persist.sys.fingerprint.analysis_debuggable", Build.IS_DEBUGGABLE);
    }

    public AnalysisService(Context context) {
        super(context);
        this.mDaemonCallback = new AnonymousClass1();
        ServiceThread serviceThread = new ServiceThread(TAG, 10, true);
        this.mHandlerThread = serviceThread;
        serviceThread.start();
        this.mHandler = new AnalysisHandler(this.mHandlerThread.getLooper());
        Watchdog.getInstance().addThread(this.mHandler);
        this.mCallbackList = new HashMap();
        this.mFinishedSessions = new ArrayDeque<>(512);
        this.mReceivedSessionCount = 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized IAnalysis getAnalysisDaemon() {
        if (this.mDaemon == null) {
            try {
                VSlog.i(TAG, "reconnect to hwbinder...");
                this.mDaemon = IAnalysis.getService();
            } catch (RemoteException e) {
                VSlog.e(TAG, "Failed to get analysis interface", e);
            } catch (NoSuchElementException e2) {
            }
            if (this.mDaemon == null) {
                VSlog.w(TAG, "analysis HIDL not available");
                return null;
            }
            this.mDaemon.asBinder().linkToDeath(this, 0L);
            try {
                int ret = this.mDaemon.setClientCallback(this.mDaemonCallback);
                if (ret != 0) {
                    VSlog.w(TAG, "Failed to connect to hwbinder!");
                    this.mDaemon = null;
                }
            } catch (RemoteException e3) {
                VSlog.e(TAG, "Failed to connect to hwbinder!", e3);
                this.mDaemon = null;
            }
            if (this.mDaemon == null) {
                MetricsLogger.count(getContext(), "analysisd_connect_error", 1);
            } else {
                VSlog.i(TAG, "hwbinder connected");
            }
        }
        return this.mDaemon;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.biometrics.fingerprint.AnalysisService$1  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass1 extends IAnalysisClientCallback.Stub {
        AnonymousClass1() {
        }

        public void onSessionFinished(final String jsonData) {
            AnalysisService.access$008(AnalysisService.this);
            if (AnalysisService.DEBUG) {
                VSlog.d(AnalysisService.TAG, "received session count: " + AnalysisService.this.mReceivedSessionCount);
            }
            AnalysisService.this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.fingerprint.-$$Lambda$AnalysisService$1$mDLFpKXYZ7UyOpj7O8sMqxPm_SM
                @Override // java.lang.Runnable
                public final void run() {
                    AnalysisService.AnonymousClass1.this.lambda$onSessionFinished$0$AnalysisService$1(jsonData);
                }
            });
        }

        public /* synthetic */ void lambda$onSessionFinished$0$AnalysisService$1(String jsonData) {
            AnalysisService.this.handleEvent(jsonData);
        }

        public void onSessionEvent(int action, String senssionName, String eventName, int flags) {
        }
    }

    public void onStart() {
        VSlog.d(TAG, "analysis hwbinder connected , publish binder service.");
        publishBinderService("analysis", new AnalysisServiceWrapper(this, null));
        SystemServerInitThreadPool.submit(new Runnable() { // from class: com.android.server.biometrics.fingerprint.-$$Lambda$AnalysisService$oTAwqzhynxrfHXwNLZt42L_UoNI
            @Override // java.lang.Runnable
            public final void run() {
                AnalysisService.this.getAnalysisDaemon();
            }
        }, "AnalysisService.onStart");
    }

    public void serviceDied(long cookie) {
        VSlog.w(TAG, "analysis hwbinder died");
        MetricsLogger.count(getContext(), "analysisd_died", 1);
        synchronized (this) {
            this.mDaemon = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void insertStub(long timeStamp, String name, String value, String target) {
        IAnalysis daemon;
        if (!TextUtils.isEmpty(name) && (daemon = getAnalysisDaemon()) != null) {
            if (value == null) {
                value = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            }
            if (target == null) {
                target = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            }
            try {
                if (DEBUGGABLE) {
                    if (!TextUtils.isEmpty(value)) {
                        if (!TextUtils.isEmpty(target)) {
                            VSlog.d(TAG, String.format("insertStub: %s, %s, %s", name, value, target));
                        } else {
                            VSlog.d(TAG, String.format("insertStub: %s, %s", name, value));
                        }
                    } else {
                        VSlog.d(TAG, String.format("insertStub: %s", name));
                    }
                }
                daemon.insertStub(timeStamp, name, value, target);
            } catch (RemoteException | IllegalArgumentException e) {
                VSlog.w(TAG, "insertStub failed : name = " + name, e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleEvent(String json) {
        SessionExtend session;
        if (DEBUGGABLE) {
            VSlog.d(TAG, String.format("RECEIVED JSON: %s", json));
        }
        StringReader reader = new StringReader(json);
        try {
            try {
                session = readJsonStream(reader);
            } catch (IOException e) {
                VSlog.e(TAG, "parse result failed:" + e.getMessage());
                reader.close();
                session = null;
            }
            if (session == null) {
                VSlog.w(TAG, "session decode failed!");
                return;
            }
            synchronized (this.mCallbackList) {
                try {
                    Callback callback = this.mCallbackList.get(session.name);
                    if (callback != null && callback.callback != null) {
                        callback.callback.onSessionFinished(session);
                    } else if (DEBUGGABLE) {
                        VSlog.i(TAG, "no callback handle session " + session.name);
                    }
                } catch (RemoteException e2) {
                    VSlog.e(TAG, "client died when notify finished:" + e2.getMessage());
                    this.mCallbackList.remove(session.name);
                }
            }
            if (DEBUGGABLE) {
                dumpSession(session);
            }
            if (this.mFinishedSessions.size() >= 512) {
                this.mFinishedSessions.removeFirst();
            }
            SessionExtend sessionCopy = new SessionExtend(session);
            sessionCopy.json = json;
            this.mFinishedSessions.addLast(sessionCopy);
        } finally {
            reader.close();
        }
    }

    public SessionExtend readJsonStream(Reader in) throws IOException {
        JsonReader reader = new JsonReader(in);
        try {
            return readSession(reader);
        } finally {
            reader.close();
        }
    }

    public SessionExtend readSession(JsonReader reader) throws IOException {
        SessionExtend session = new SessionExtend();
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals(TAG_SESSION)) {
                session.name = reader.nextString();
            } else if (name.equals(TAG_TYPE)) {
                session.type = reader.nextInt();
            } else if (name.equals(TAG_EVENTS)) {
                session.events = readEventArray(reader);
            } else if (name.equals(TAG_ID)) {
                session.id = reader.nextLong();
            } else {
                VSlog.w(TAG, "Unknown tag " + name);
                reader.skipValue();
            }
        }
        reader.endObject();
        return session;
    }

    private ArrayList<AnalysisEvent> readEventArray(JsonReader reader) throws IOException {
        ArrayList<AnalysisEvent> events = new ArrayList<>();
        reader.beginArray();
        while (reader.hasNext()) {
            AnalysisEvent event = readEvent(reader);
            events.add(event);
        }
        reader.endArray();
        return events;
    }

    private AnalysisEvent readEvent(JsonReader reader) throws IOException {
        AnalysisEvent event = new AnalysisEvent();
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals(TAG_EVENT)) {
                event.name = reader.nextString();
            } else if (name.equals(TAG_VALUE)) {
                event.value = reader.nextString();
            } else if (name.equals(TAG_TIMEBASE)) {
                event.timebase = reader.nextString();
            } else if (name.equals(TAG_FLAGS)) {
                event.flags = reader.nextInt();
            } else if (name.equals(TAG_TIME)) {
                event.timestamp = readTime(reader);
            } else {
                VSlog.w(TAG, "Unknown tag " + name);
                reader.skipValue();
            }
        }
        reader.endObject();
        return event;
    }

    private long[] readTime(JsonReader reader) throws IOException {
        int index = 0;
        long[] time = new long[2];
        reader.beginArray();
        while (reader.hasNext()) {
            if (index < time.length) {
                time[index] = reader.nextLong();
                index++;
            } else {
                VSlog.w(TAG, "overflow when read timestamp");
                reader.skipValue();
            }
        }
        reader.endArray();
        return time;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeCallback(String name) {
        synchronized (this.mCallbackList) {
            this.mCallbackList.remove(name);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkPermission(String permission) {
        Context context = getContext();
        context.enforceCallingOrSelfPermission(permission, "Must hava " + permission + " permission.");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpInternal(PrintWriter pw) {
        pw.printf("session count: %d", Integer.valueOf(this.mFinishedSessions.size()));
        pw.println();
        Iterator<SessionExtend> itr = this.mFinishedSessions.iterator();
        while (itr.hasNext()) {
            SessionExtend session = itr.next();
            dumpSession(pw, session);
            pw.flush();
        }
        pw.flush();
    }

    private void dumpSession(PrintWriter pw, SessionExtend session) {
        pw.println(String.format("session: %s, id: %d, type: %d", session.name, Long.valueOf(session.id), Integer.valueOf(session.type)));
        if (DEBUG && session.json != null && session.json.length() > 0) {
            pw.println(session.json);
        }
        if (session.events == null || session.events.size() == 0) {
            pw.println("----NO EVENT");
            return;
        }
        Iterator it = session.events.iterator();
        while (it.hasNext()) {
            AnalysisEvent event = (AnalysisEvent) it.next();
            pw.println(String.format("----event: %s, value: %s, timebase: %s, flags: %d, time:[%d - %d]", event.name, event.value, event.timebase, Integer.valueOf(event.flags), Long.valueOf(event.timestamp[0]), Long.valueOf(event.timestamp[1])));
        }
    }

    private void dumpSession(SessionExtend session) {
        ArrayList<AnalysisEvent> events;
        ArrayList<AnalysisEvent> events2 = new ArrayList<>(session.events);
        int i = 1;
        int i2 = 0;
        if (events2.size() == 0) {
            VSlog.d(TAG, String.format("session: %s, type: %d", session.name, Integer.valueOf(session.type)));
            VSlog.d(TAG, "----NO EVENT");
            return;
        }
        Collections.sort(events2, $$Lambda$AnalysisService$QBuQAXNSfjG4LEMoBYbXH_2JFo.INSTANCE);
        events2.size();
        long timeStart = events2.get(0).timestamp[0];
        long timeEnd = 0;
        StringBuilder builder = new StringBuilder();
        Iterator<AnalysisEvent> it = events2.iterator();
        while (it.hasNext()) {
            AnalysisEvent event = it.next();
            int i3 = event.timestamp[i] > event.timestamp[i2] ? i : i2;
            long[] jArr = event.timestamp;
            long finishTime = i3 != 0 ? jArr[i] : jArr[i2];
            timeEnd = Math.max(timeEnd, finishTime);
            Object[] objArr = new Object[i];
            objArr[0] = Long.valueOf(ns2ms(finishTime - timeStart));
            builder.append(String.format(" %4d ", objArr));
            builder.append("---- ");
            builder.append(event.name);
            if (i3 == 0) {
                events = events2;
            } else {
                builder.append("(");
                long during = ns2ms(event.timestamp[1] - event.timestamp[0]);
                builder.append(during);
                events = events2;
                builder.append(")");
            }
            if (!TextUtils.isEmpty(event.value)) {
                builder.append(":");
                builder.append(event.value);
            }
            if (!TextUtils.isEmpty(event.timebase)) {
                builder.append("|");
                builder.append(event.timebase);
            }
            i2 = 0;
            builder.append(String.format(" flags:0x%04x\n", Integer.valueOf(event.flags)));
            events2 = events;
            i = 1;
        }
        Object[] objArr2 = new Object[3];
        objArr2[i2] = session.name;
        objArr2[1] = Integer.valueOf(session.type);
        objArr2[2] = Long.valueOf(ns2ms(timeEnd - timeStart));
        VSlog.d(TAG, String.format("session: %s, type: %d, during:%d", objArr2));
        VSlog.d(TAG, builder.toString());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ int lambda$dumpSession$0(AnalysisEvent lhs, AnalysisEvent rhs) {
        long lhsFinishedTime = lhs.timestamp[1] > lhs.timestamp[0] ? lhs.timestamp[1] : lhs.timestamp[0];
        long rhsFinishedTime = rhs.timestamp[1] > rhs.timestamp[0] ? rhs.timestamp[1] : rhs.timestamp[0];
        return Long.compare(lhsFinishedTime, rhsFinishedTime);
    }

    private static long ns2ms(long nano) {
        return nano / 1000000;
    }

    /* loaded from: classes.dex */
    private final class AnalysisHandler extends Handler {
        AnalysisHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                StubReq req = (StubReq) msg.obj;
                if (req != null) {
                    AnalysisService.this.insertStub(req.timeStamp, req.name, req.value, req.target);
                    return;
                }
                return;
            }
            VSlog.w(AnalysisService.TAG, "Unknown message:" + msg.what);
        }
    }

    /* loaded from: classes.dex */
    private final class AnalysisServiceWrapper extends IAnalysisService.Stub {
        private AnalysisServiceWrapper() {
        }

        /* synthetic */ AnalysisServiceWrapper(AnalysisService x0, AnonymousClass1 x1) {
            this();
        }

        public void insertStub(long timeStamp, String name, String value, String target) {
            long timeStart = 0;
            if (AnalysisService.DEBUGGABLE) {
                timeStart = SystemClock.uptimeMillis();
            }
            AnalysisService.this.insertStub(timeStamp, name, value, target);
            if (AnalysisService.DEBUGGABLE) {
                long during = SystemClock.uptimeMillis() - timeStart;
                VSlog.d(AnalysisService.TAG, "insertStub cost " + during + " ms");
            }
        }

        public void insertStubAsync(long timeStamp, String name, String value, String target) {
            Message.obtain(AnalysisService.this.mHandler, 1, 0, 0, new StubReq(timeStamp, name, value, target)).sendToTarget();
        }

        public void registerEventCallback(IBinder token, String name, IAnalysisEventCallback cb) {
            AnalysisService.this.checkPermission("android.permission.USE_BIOMETRIC_INTERNAL");
            if (!TextUtils.isEmpty(name) && name.length() <= 32) {
                synchronized (AnalysisService.this.mCallbackList) {
                    if (AnalysisService.this.mCallbackList.containsKey(name)) {
                        VSlog.w(AnalysisService.TAG, "JUST support register only one callback for one event, remove previous callback " + name);
                        Callback removedCallback = (Callback) AnalysisService.this.mCallbackList.remove(name);
                        if (removedCallback != null) {
                            removedCallback.destroy();
                        }
                    }
                    Callback callback = new Callback(token, name, cb);
                    AnalysisService.this.mCallbackList.put(name, callback);
                }
                return;
            }
            throw new IllegalArgumentException("name is null or length longer than 32");
        }

        public void unregisterEventCallback(IBinder token, String name) {
            AnalysisService.this.checkPermission("android.permission.USE_BIOMETRIC_INTERNAL");
            synchronized (AnalysisService.this.mCallbackList) {
                if (AnalysisService.this.mCallbackList.containsKey(name)) {
                    Callback callback = (Callback) AnalysisService.this.mCallbackList.get(name);
                    callback.destroy();
                    AnalysisService.this.mCallbackList.remove(name);
                }
            }
        }

        public void registerSession(IBinder token, String name, String xmlData, int flags) {
        }

        public void unregisterSession(String name) {
        }

        public void setSessionEnabled(String name, boolean enabled) {
        }

        public boolean isSessionExist(String name) {
            return false;
        }

        public boolean isSessionEnabeld(String name) {
            return false;
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            Context context = AnalysisService.this.getContext();
            if (context.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
                pw.println("Permission Denial: can't dump AnalysisService from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
                return;
            }
            long ident = Binder.clearCallingIdentity();
            try {
                AnalysisService.this.dumpInternal(pw);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class Callback implements IBinder.DeathRecipient {
        IAnalysisEventCallback callback;
        String name;
        IBinder token;

        Callback(IBinder token, String name, IAnalysisEventCallback callback) {
            this.token = token;
            this.name = name;
            this.callback = callback;
            try {
                token.linkToDeath(this, 0);
            } catch (RemoteException e) {
                VSlog.w(AnalysisService.TAG, "caught remote exception in linkToDeath: ", e);
            }
        }

        void destroy() {
            IBinder iBinder = this.token;
            if (iBinder != null) {
                try {
                    iBinder.unlinkToDeath(this, 0);
                } catch (NoSuchElementException e) {
                    VSlog.e(AnalysisService.TAG, "destroy(): " + this + ":", new Exception("here"));
                }
                this.token = null;
            }
            this.callback = null;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            this.callback = null;
            this.token = null;
            AnalysisService.this.removeCallback(this.name);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class SessionExtend extends AnalysisSession {
        long id;
        String json;

        SessionExtend() {
        }

        SessionExtend(SessionExtend _o) {
            super(_o);
            this.id = _o.id;
        }

        SessionExtend(int _id, AnalysisSession _session) {
            super(_session);
            this.id = _id;
        }
    }

    /* loaded from: classes.dex */
    private static class StubReq {
        public String name;
        public String target;
        public long timeStamp;
        public String value;

        public StubReq(long time, String n, String v, String t) {
            this.timeStamp = time;
            this.name = n;
            this.value = v;
            this.target = t;
        }
    }
}