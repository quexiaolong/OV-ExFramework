package com.android.server.devicepolicy.utils;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Slog;
import com.vivo.vcodetransbase.EventTransfer;
import java.text.SimpleDateFormat;
import java.util.HashMap;

/* loaded from: classes.dex */
public class VcodeUtils {
    public static final String MODULE_ID = "F290";
    private static final String TAG = "VDPMS_vcode";
    public static final String TYPE_ALARM = "F290|10002";
    public static final String TYPE_EXCEPTION = "F290|10004";
    public static final String TYPE_INVOKE = "F290|10001";
    public static final String TYPE_LOCATION = "F290|10003";
    public static final String TYPE_POWER = "F290|10005";
    public static final String TYPE_WAKELOCK = "F290|10006";
    private EventTransfer mVcodeEventTransfer;
    private Handler mVcodeHanlder;
    private HandlerThread mVcodeThread;
    private static VcodeUtils sInstance = null;
    private static SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private VcodeUtils() {
        this.mVcodeThread = null;
        this.mVcodeHanlder = null;
        this.mVcodeEventTransfer = null;
        HandlerThread handlerThread = new HandlerThread("VcodeUtils");
        this.mVcodeThread = handlerThread;
        handlerThread.start();
        this.mVcodeHanlder = new Handler(this.mVcodeThread.getLooper()) { // from class: com.android.server.devicepolicy.utils.VcodeUtils.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                VcodeUtils.this.handleVcodeMessage(msg);
            }
        };
        this.mVcodeEventTransfer = EventTransfer.getInstance();
    }

    public static VcodeUtils getInstance() {
        if (sInstance == null) {
            synchronized (VcodeUtils.class) {
                if (sInstance == null) {
                    sInstance = new VcodeUtils();
                }
            }
        }
        return sInstance;
    }

    private void sendVcodeMessage(VcodeInfo info) {
        Message msg = this.mVcodeHanlder.obtainMessage();
        msg.obj = info;
        this.mVcodeHanlder.sendMessage(msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleVcodeMessage(Message msg) {
        VcodeInfo info = (VcodeInfo) msg.obj;
        Slog.d(TAG, "handleVcodeMessage info: " + info.toString());
        if (this.mVcodeEventTransfer != null && info != null && info.mData != null) {
            try {
                this.mVcodeEventTransfer.singleEvent(info.mModuleId, info.mEventId, info.mStartTime, info.mDuration, info.mData);
                return;
            } catch (Exception e) {
                Slog.e(TAG, "handleVcodeMessage Exception: " + e.getMessage());
                return;
            }
        }
        Slog.e(TAG, "handleVcodeMessage " + info.mEventId + " ID: " + info.mModuleId + " handle fail");
    }

    public static void report(String type, HashMap<String, String> data) {
        VcodeUtils vcode = getInstance();
        if (vcode != null) {
            vcode.report(MODULE_ID, type, data);
        }
    }

    public void report(String id, String type, HashMap<String, String> data) {
        try {
            long time = System.currentTimeMillis();
            VcodeInfo info = new VcodeInfo(id, type, time, data);
            sendVcodeMessage(info);
        } catch (Exception e) {
            Slog.e(TAG, "report Exception: " + e.getMessage());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class VcodeInfo {
        public HashMap<String, String> mData;
        public long mDuration = 0;
        public String mEventId;
        public String mModuleId;
        public long mStartTime;

        public VcodeInfo(String id, String label, long time, HashMap<String, String> data) {
            this.mData = null;
            this.mModuleId = id;
            this.mEventId = label;
            this.mStartTime = time;
            this.mData = data;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(256);
            sb.append("VcodeInfo{ ");
            sb.append(" moduleId: " + this.mModuleId);
            sb.append(" eventId: " + this.mEventId);
            sb.append(" startTime: " + VcodeUtils.dateformat.format(Long.valueOf(this.mStartTime)));
            sb.append(" duration: " + this.mDuration);
            sb.append(" data: " + this.mData.toString());
            sb.append(" }");
            return sb.toString();
        }
    }
}