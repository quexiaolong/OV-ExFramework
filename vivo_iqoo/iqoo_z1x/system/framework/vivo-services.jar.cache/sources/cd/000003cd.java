package com.android.server.policy;

import android.content.ContentValues;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import vivo.app.epm.ExceptionPolicyManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class InputExceptionReport {
    public static final String DEFAULT_LEVEL = "1";
    public static final String DEFAULT_TROUBLE = "1";
    public static final int EVENT_TYPE = 20;
    public static final String LEVEL_HIGH = "2";
    public static final String LEVEL_LOW = "4";
    public static final String LEVEL_MEDIUM = "3";
    public static final String LEVEL_VERY_HIGH = "1";
    public static final String LEVEL_VERY_LOW = "5";
    private static final int MSG_REPORT_INTERCEPT = 1;
    private static final int MSG_REPORT_NORMAL_INPUT_EXCEPTION = 2;
    private static final long POST_MSG_DELAY = 10000;
    private static final String SEPARATOR = "_";
    public static final int SUB_EVENT_ACCESSIBILITY_SERVICE_TIMEOUT = 3;
    public static final int SUB_EVENT_APP_PROCESS_INPUT_TIMEOUT = 2;
    public static final int SUB_EVENT_FORCE_BACK = 4;
    public static final int SUB_EVENT_FORCE_BACK_NOT_TRIGGER = 5;
    public static final int SUB_EVENT_INPUTFILTER = 1;
    public static final int SUB_EVENT_INPUT_FROZEN_TIMEOUT = 7;
    public static final int SUB_EVENT_INVALID_WINDOW = 6;
    public static final int SUB_EVENT_NO_FOCUSED_WINDOW = 8;
    public static final int SUB_EVENT_STARTING_WINDOW_BACK_KEY = 10;
    private static final String TAG = "IFE";
    public static final String TROUBLE_NO = "0";
    public static final String TROUBLE_YES = "1";
    private static final String VERSION_INVALID_WINDOW = "2";
    private static InputExceptionReport sInstance = null;
    private Handler inputReportHandler = null;
    public HandlerThread mHandlerThread;

    private InputExceptionReport() {
        this.mHandlerThread = null;
        HandlerThread handlerThread = new HandlerThread(TAG);
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        initHandler();
    }

    public static synchronized InputExceptionReport getInstance() {
        InputExceptionReport inputExceptionReport;
        synchronized (InputExceptionReport.class) {
            if (sInstance == null) {
                sInstance = new InputExceptionReport();
            }
            inputExceptionReport = sInstance;
        }
        return inputExceptionReport;
    }

    private void initHandler() {
        this.inputReportHandler = new Handler(this.mHandlerThread.getLooper()) { // from class: com.android.server.policy.InputExceptionReport.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                int i = msg.what;
                if (i == 1) {
                    InputExceptionReport.this.reportEventToEpm((String) msg.obj);
                } else if (i == 2) {
                    InputExceptionReport.this.reportEventToEpm((String) msg.obj, "20_" + msg.arg1);
                }
            }
        };
    }

    public void recordInputFilter(String packageName, boolean isBegin) {
        if ("com.vivo.daemonService".equals(packageName) || "com.vivo.minscreen".equals(packageName)) {
            return;
        }
        if (isBegin) {
            this.inputReportHandler.removeMessages(1);
            Message msg = Message.obtain();
            msg.what = 1;
            msg.obj = packageName;
            this.inputReportHandler.sendMessageDelayed(msg, 10000L);
            return;
        }
        this.inputReportHandler.removeMessages(1);
    }

    public void recordAppProcessTimeout(String windowName) {
        if (TextUtils.isEmpty(windowName)) {
            return;
        }
        reportEventToEpm(windowName, 2);
    }

    public void recordAccessibilityServiceTimeOut(String nodeInfo) {
        if (TextUtils.isEmpty(nodeInfo)) {
            return;
        }
        reportEventToEpm(nodeInfo, 3);
    }

    public void reportForceBack(String windowTitle, boolean triggered) {
        reportEventToEpm(windowTitle, triggered ? 4 : 5);
    }

    public void reportInputFrozenTimeout(String inputFreezeReason) {
        reportEventToEpm(inputFreezeReason, 7);
    }

    private void reportEventToEpm(String expsrc, int subType) {
        Message msg = Message.obtain();
        msg.what = 2;
        msg.obj = expsrc;
        msg.arg1 = subType;
        this.inputReportHandler.sendMessageDelayed(msg, 10000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportEventToEpm(String packageName) {
        VSlog.e(TAG, "InputFilter Exception reportEventToEpm packageName:" + packageName);
        ContentValues cv = new ContentValues();
        cv.put("expsrc", packageName);
        cv.put("subtype", "20_1");
        cv.put("level", LEVEL_MEDIUM);
        cv.put("trouble", "1");
        ExceptionPolicyManager.getInstance().recordEvent(20, System.currentTimeMillis(), cv);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportEventToEpm(String expsrc, String subtype) {
        VSlog.e(TAG, "input exception report:" + expsrc + " subtype:" + subtype);
        ContentValues cv = new ContentValues();
        cv.put("expsrc", expsrc);
        cv.put("subtype", subtype);
        cv.put("level", LEVEL_MEDIUM);
        cv.put("trouble", "1");
        ExceptionPolicyManager.getInstance().recordEvent(20, System.currentTimeMillis(), cv);
    }

    public void reportEventToEpm(String expsrc, int subtype, String scene, String reason, String trouble, String level, String... datas) {
        VSlog.e(TAG, "input exception report: exception source:" + expsrc + " subtype:" + subtype + " reason = " + reason);
        ContentValues cv = new ContentValues();
        cv.put("expsrc", expsrc);
        cv.put("expose", scene);
        StringBuilder sb = new StringBuilder();
        sb.append("20_");
        sb.append(subtype);
        cv.put("subtype", sb.toString());
        if (subtype == 6) {
            cv.put("version", "2");
        }
        cv.put("level", level);
        cv.put("trouble", trouble);
        cv.put("reason", reason);
        for (int i = 0; i < Math.min(datas.length, 20); i++) {
            cv.put("data" + (i + 1), datas[i]);
        }
        ExceptionPolicyManager.getInstance().recordEvent(20, System.currentTimeMillis(), cv);
    }

    public static String getSubType(int subTypeId) {
        return "20_" + subTypeId;
    }
}