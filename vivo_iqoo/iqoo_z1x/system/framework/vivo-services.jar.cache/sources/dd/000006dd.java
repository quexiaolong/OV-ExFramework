package com.vivo.services.rms;

import android.media.AudioSystem;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.SparseArray;
import com.android.server.RmsKeepQuietListener;
import com.vivo.face.common.data.Constants;
import com.vivo.services.rms.appmng.AppManager;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class PreloadedAppRecordMgr {
    private static PreloadedAppRecordMgr INSTANCE = null;
    private static final String TAG = "RMS-Preload";
    final RmsKeepQuietListener mAudioListener;
    private final HashMap<PreloadedAppRecord, SparseArray<ProcessInfo>> mRecords = new HashMap<>();
    private final boolean NORMAL_APP_ONLY = true;
    private final PreloadedAppRecord COMMON_KEY = new PreloadedAppRecord(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, 0);
    private final HashSet<RmsKeepQuietListener> mListener = new HashSet<>();
    private int mFlag = Integer.MAX_VALUE;

    private PreloadedAppRecordMgr() {
        RmsKeepQuietListener rmsKeepQuietListener = new RmsKeepQuietListener("sound", 8) { // from class: com.vivo.services.rms.PreloadedAppRecordMgr.1
            @Override // com.android.server.RmsKeepQuietListener
            public void onQuietStateChanged(String pkgName, int uid, boolean newState) {
                if (newState) {
                    AudioSystem.setParameters("setMuteUid=" + uid);
                    return;
                }
                AudioSystem.setParameters("clearMuteUid=" + uid);
            }
        };
        this.mAudioListener = rmsKeepQuietListener;
        registerListener(rmsKeepQuietListener);
    }

    public static synchronized PreloadedAppRecordMgr getInstance() {
        PreloadedAppRecordMgr preloadedAppRecordMgr;
        synchronized (PreloadedAppRecordMgr.class) {
            if (INSTANCE == null) {
                INSTANCE = new PreloadedAppRecordMgr();
            }
            preloadedAppRecordMgr = INSTANCE;
        }
        return preloadedAppRecordMgr;
    }

    public void add(ProcessInfo pi) {
        if (!UserHandle.isApp(pi.mUid)) {
            return;
        }
        synchronized (this.mRecords) {
            String pkgName = pi.mPkgName;
            PreloadedAppRecord key = obtainKeyLocked(pkgName, pi.mUid);
            Iterator<Map.Entry<PreloadedAppRecord, SparseArray<ProcessInfo>>> it = this.mRecords.entrySet().iterator();
            boolean newRecord = true;
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                Map.Entry<PreloadedAppRecord, SparseArray<ProcessInfo>> map = it.next();
                PreloadedAppRecord pr = map.getKey();
                if (key.equals(pr)) {
                    newRecord = false;
                    if (pi.needKeepQuiet) {
                        pr.keepQuiet = true;
                    }
                    SparseArray<ProcessInfo> procs = map.getValue();
                    if (pr.keepQuiet && hasNoSameUidLocked(pi.mUid, procs)) {
                        notifyQuietChangeLocked(pr.pkgName, pi.mUid, true);
                    }
                    if (procs != null) {
                        procs.put(pi.mPid, pi);
                    }
                }
            }
            if (newRecord && pi.rmsPreloaded) {
                PreloadedAppRecord ar = new PreloadedAppRecord(pkgName, pi.mUid);
                ar.keepQuiet = pi.needKeepQuiet;
                ar.rmsPreloadPeriod = true;
                if (ar.keepQuiet) {
                    notifyQuietChangeLocked(ar.pkgName, pi.mUid, true);
                }
                SparseArray<ProcessInfo> procs2 = new SparseArray<>();
                procs2.put(pi.mPid, pi);
                this.mRecords.put(ar, procs2);
                if (pi.mCreateReason != null && pi.mCreateReason.contains("rms-t4")) {
                    ar.type = 1;
                }
            }
        }
    }

    public void remove(ProcessInfo pi) {
        if (!UserHandle.isApp(pi.mUid)) {
            return;
        }
        synchronized (this.mRecords) {
            String pkgName = pi.mPkgName;
            PreloadedAppRecord key = obtainKeyLocked(pkgName, pi.mUid);
            Iterator<Map.Entry<PreloadedAppRecord, SparseArray<ProcessInfo>>> it = this.mRecords.entrySet().iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                Map.Entry<PreloadedAppRecord, SparseArray<ProcessInfo>> map = it.next();
                PreloadedAppRecord pr = map.getKey();
                if (key.equals(pr)) {
                    SparseArray<ProcessInfo> procs = map.getValue();
                    if (procs != null) {
                        procs.remove(pi.mPid);
                        if (pr.keepQuiet && hasNoSameUidLocked(pi.mUid, procs)) {
                            notifyQuietChangeLocked(pr.pkgName, pi.mUid, false);
                        }
                        if (procs.size() <= 0) {
                            it.remove();
                        }
                    }
                }
            }
        }
    }

    public void setRmsPreload(String pkgName, int uid, boolean isRmsPreload, boolean keepQuiet) {
        if (TextUtils.isEmpty(pkgName)) {
            return;
        }
        if (uid > 0 && !UserHandle.isApp(uid)) {
            return;
        }
        synchronized (this.mRecords) {
            PreloadedAppRecord key = obtainKeyLocked(pkgName, uid);
            Iterator<Map.Entry<PreloadedAppRecord, SparseArray<ProcessInfo>>> it = this.mRecords.entrySet().iterator();
            VSlog.d(TAG, "setRmsPreload pkgName:" + pkgName + " uid " + uid + ", isRmsPreload = " + isRmsPreload + ", keepQuiet =" + keepQuiet);
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                Map.Entry<PreloadedAppRecord, SparseArray<ProcessInfo>> map = it.next();
                PreloadedAppRecord pr = map.getKey();
                VSlog.d(TAG, "setRmsPreload pr:" + pr.toString());
                if (key.equals(pr)) {
                    VSlog.d(TAG, "setRmsPreload found:" + pr.keepQuiet + " keepQuiet" + keepQuiet);
                    pr.rmsPreloadPeriod = isRmsPreload;
                    if (keepQuiet != pr.keepQuiet) {
                        SparseArray<ProcessInfo> procs = map.getValue();
                        int size = procs != null ? procs.size() : 0;
                        if (size > 0) {
                            HashSet<Integer> uids = new HashSet<>(1);
                            for (int i = 0; i < size; i++) {
                                uids.add(Integer.valueOf(procs.valueAt(i).mUid));
                            }
                            Iterator<Integer> it2 = uids.iterator();
                            while (it2.hasNext()) {
                                int setUid = it2.next().intValue();
                                notifyQuietChangeLocked(pr.pkgName, setUid, keepQuiet);
                            }
                        }
                        pr.keepQuiet = keepQuiet;
                    }
                }
            }
        }
    }

    public void clearUiPreloadMute() {
        synchronized (this.mRecords) {
            VSlog.d(TAG, "clearUiPreloadMute begin");
            for (Map.Entry<PreloadedAppRecord, SparseArray<ProcessInfo>> map : this.mRecords.entrySet()) {
                PreloadedAppRecord pr = map.getKey();
                VSlog.d(TAG, "clearUiPreloadMute1 pkgName:" + pr.toString());
                if (pr.keepQuiet && pr.type == 1) {
                    VSlog.d(TAG, "clearUiPreloadMute pkgName:" + pr.toString());
                    SparseArray<ProcessInfo> procs = map.getValue();
                    int size = procs != null ? procs.size() : 0;
                    if (size > 0) {
                        HashSet<Integer> uids = new HashSet<>(1);
                        for (int i = 0; i < size; i++) {
                            uids.add(Integer.valueOf(procs.valueAt(i).mUid));
                        }
                        Iterator<Integer> it = uids.iterator();
                        while (it.hasNext()) {
                            int setUid = it.next().intValue();
                            notifyQuietChangeLocked(pr.pkgName, setUid, false);
                        }
                        pr.keepQuiet = false;
                        pr.rmsPreloadPeriod = false;
                    }
                }
            }
        }
    }

    public boolean isRmsPreload(String pkgName, int uid) {
        if (TextUtils.isEmpty(pkgName) || (uid > 0 && !UserHandle.isApp(uid))) {
            return false;
        }
        synchronized (this.mRecords) {
            PreloadedAppRecord key = obtainKeyLocked(pkgName, uid);
            for (PreloadedAppRecord pr : this.mRecords.keySet()) {
                if (key.equals(pr) && pr.rmsPreloadPeriod) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean isRmsUIPreload(String pkgName, int uid) {
        if (TextUtils.isEmpty(pkgName) || (uid > 0 && !UserHandle.isApp(uid))) {
            return false;
        }
        synchronized (this.mRecords) {
            PreloadedAppRecord key = obtainKeyLocked(pkgName, uid);
            for (Map.Entry<PreloadedAppRecord, SparseArray<ProcessInfo>> map : this.mRecords.entrySet()) {
                PreloadedAppRecord pr = map.getKey();
                if (key.equals(pr)) {
                    if (pr.rmsPreloadPeriod) {
                        SparseArray<ProcessInfo> procs = map.getValue();
                        int size = procs != null ? procs.size() : 0;
                        for (int i = 0; i < size; i++) {
                            if (procs.valueAt(i).hasShownUi()) {
                                return true;
                            }
                        }
                        return false;
                    }
                    return true;
                }
            }
            return false;
        }
    }

    public void setNeedKeepQuiet(String pkgName, int userId, int uid, boolean keepQuiet) {
        if (TextUtils.isEmpty(pkgName)) {
            return;
        }
        if (uid > 0 && !UserHandle.isApp(uid)) {
            return;
        }
        synchronized (this.mRecords) {
            PreloadedAppRecord key = obtainKeyLocked(pkgName, uid);
            Iterator<Map.Entry<PreloadedAppRecord, SparseArray<ProcessInfo>>> it = this.mRecords.entrySet().iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                Map.Entry<PreloadedAppRecord, SparseArray<ProcessInfo>> map = it.next();
                PreloadedAppRecord pr = map.getKey();
                if (key.equals(pr)) {
                    if (pr.keepQuiet != keepQuiet) {
                        VSlog.d(TAG, "setNeedKeepQuiet pkgName=" + pkgName + ",userId=" + userId + " uid=" + uid + " ,keepQuiet=" + keepQuiet);
                        SparseArray<ProcessInfo> procs = map.getValue();
                        int size = procs != null ? procs.size() : 0;
                        if (size > 0) {
                            HashSet<Integer> uids = new HashSet<>(1);
                            for (int i = 0; i < size; i++) {
                                uids.add(Integer.valueOf(procs.valueAt(i).mUid));
                            }
                            Iterator<Integer> it2 = uids.iterator();
                            while (it2.hasNext()) {
                                int setUid = it2.next().intValue();
                                notifyQuietChangeLocked(pr.pkgName, setUid, keepQuiet);
                            }
                        }
                        pr.keepQuiet = keepQuiet;
                    }
                }
            }
        }
    }

    public void setNeedKeepQuiet(int pid, int uid, boolean keepQuiet) {
        if (!UserHandle.isApp(uid)) {
            return;
        }
        ProcessInfo pi = AppManager.getInstance().getProcessInfo(pid);
        if (pi != null) {
            setNeedKeepQuiet(pi.mPkgName, UserHandle.getUserId(pi.mUid), uid, keepQuiet);
            return;
        }
        List<ProcessInfo> procs = AppManager.getInstance().getProcessInfoListForUid(uid);
        if (procs != null && procs.size() > 0) {
            setNeedKeepQuiet(procs.get(0).mPkgName, UserHandle.getUserId(procs.get(0).mUid), 0, keepQuiet);
        }
    }

    public boolean needKeepQuiet(String pkgName, int userId, int uid, int flag) {
        if (TextUtils.isEmpty(pkgName) || ((uid > 0 && !UserHandle.isApp(uid)) || (this.mFlag & flag) == 0)) {
            return false;
        }
        synchronized (this.mRecords) {
            PreloadedAppRecord key = obtainKeyLocked(pkgName, uid);
            for (PreloadedAppRecord pr : this.mRecords.keySet()) {
                if (key.equals(pr) && pr.keepQuiet) {
                    VSlog.i(TAG, "Make " + pkgName + " quiet by RMS since it was preloaded.");
                    return true;
                }
            }
            return false;
        }
    }

    public boolean needKeepQuiet(int pid, int uid, int flag) {
        if (UserHandle.isApp(uid) && (this.mFlag & flag) != 0) {
            ProcessInfo pi = AppManager.getInstance().getProcessInfo(pid);
            if (pi != null) {
                return needKeepQuiet(pi.mPkgName, UserHandle.getUserId(pi.mUid), uid, flag);
            }
            List<ProcessInfo> procs = AppManager.getInstance().getProcessInfoListForUid(uid);
            if (procs == null || procs.size() <= 0) {
                return false;
            }
            return needKeepQuiet(procs.get(0).mPkgName, UserHandle.getUserId(procs.get(0).mUid), procs.get(0).mUid, flag);
        }
        return false;
    }

    public void registerListener(RmsKeepQuietListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (this.mListener) {
            this.mListener.add(listener);
        }
    }

    public void unRegisterListener(RmsKeepQuietListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (this.mListener) {
            this.mListener.remove(listener);
        }
    }

    public void setKeepQuietType(int flag) {
        VSlog.d(TAG, "setKeepQuietType flag =" + flag);
        this.mFlag = flag;
    }

    private void notifyQuietChangeLocked(String pkgName, int uid, boolean newState) {
        synchronized (this.mListener) {
            Iterator<RmsKeepQuietListener> it = this.mListener.iterator();
            while (it.hasNext()) {
                RmsKeepQuietListener listener = it.next();
                if (!newState || (this.mFlag & listener.ownerFlag) != 0) {
                    listener.onQuietStateChanged(pkgName, uid, newState);
                }
            }
        }
    }

    private boolean hasNoSameUidLocked(int uid, SparseArray<ProcessInfo> procs) {
        int size = procs != null ? procs.size() : 0;
        for (int i = 0; i < size; i++) {
            if (procs.valueAt(i).mUid == uid) {
                return false;
            }
        }
        return true;
    }

    public void dump(PrintWriter pw) {
        synchronized (this.mRecords) {
            StringBuilder builder = new StringBuilder(256);
            for (PreloadedAppRecord pr : this.mRecords.keySet()) {
                pr.buildString(builder);
                SparseArray<ProcessInfo> procs = this.mRecords.get(pr);
                if (procs == null) {
                    builder.append("no process list\n");
                }
                for (int i = 0; i < procs.size(); i++) {
                    builder.append(" --- ");
                    builder.append(procs.valueAt(i).toString());
                    builder.append("\n");
                }
            }
            pw.print(builder.toString());
        }
    }

    private PreloadedAppRecord obtainKeyLocked(String pkgName, int uid) {
        return this.COMMON_KEY.obtain(pkgName, uid);
    }

    /* loaded from: classes.dex */
    public class PreloadedAppRecord {
        private boolean keepQuiet = false;
        private String pkgName;
        private boolean rmsPreloadPeriod;
        private int type;
        private int uid;
        private int userId;

        public PreloadedAppRecord(String pkgName, int uid) {
            this.pkgName = pkgName;
            this.uid = uid;
        }

        public boolean equals(Object arg0) {
            if (arg0 != null && (arg0 instanceof PreloadedAppRecord)) {
                PreloadedAppRecord ar = (PreloadedAppRecord) arg0;
                String str = this.pkgName;
                if (str != null && str.equals(ar.pkgName) && this.uid == ar.uid) {
                    return true;
                }
                return false;
            }
            return false;
        }

        public int hashCode() {
            return this.userId;
        }

        public PreloadedAppRecord obtain(String pkgName, int uid) {
            this.pkgName = pkgName;
            this.uid = uid;
            return this;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(256);
            buildString(sb);
            return sb.toString();
        }

        public void buildString(StringBuilder sb) {
            sb.append(this.pkgName);
            sb.append(" ");
            sb.append(this.uid);
            sb.append(" ");
            sb.append(" type=" + this.type);
            sb.append(" keepQuiet=");
            sb.append(this.keepQuiet);
            sb.append(" ");
            sb.append(this.rmsPreloadPeriod);
            sb.append("\n");
        }
    }
}