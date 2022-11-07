package com.android.server.pm;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import com.vivo.face.common.data.Constants;
import vivo.app.epm.ExceptionPolicyManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public final class VivoPKMSReportMgr {
    public static final int EXCEPTION_TYPE_GET_INSTALL_PACKAGE_LIST = 13;
    public static final int EXCEPTION_TYPE_INSTALLED_APP_LOST = 15;
    public static final int EXCEPTION_TYPE_PKMS_LOG_REPORT = 39;
    public static final int EXCEPTION_TYPE_UNINSTALL = 14;
    private static final int MSG_INSTALL_APK_FAILED = 100;
    private static final int MSG_SCAN_APK_FAILED = 101;
    private static final int MSG_SCHEDULE_REPORT = 102;
    public static final String TAG = "pkmsReportMgr";
    private static volatile VivoPKMSReportMgr sInstance;
    private static volatile WorkHandler sWorkHandler;
    private Context mContext;
    private boolean mSwitch = true;

    private VivoPKMSReportMgr() {
        VSlog.i(TAG, "pkmsReportMgr init.");
    }

    public static final VivoPKMSReportMgr getInstance() {
        if (sInstance == null) {
            synchronized (VivoPKMSReportMgr.class) {
                if (sInstance == null) {
                    sInstance = new VivoPKMSReportMgr();
                }
            }
        }
        return sInstance;
    }

    public void init(Context context, Looper looper) {
        if (sWorkHandler == null) {
            synchronized (VivoPKMSReportMgr.class) {
                if (sWorkHandler == null) {
                    sWorkHandler = new WorkHandler(looper);
                }
            }
            this.mContext = context;
        }
    }

    public void systemReady() {
    }

    public void scheduleReportScanFailedException(String packageName, int returnCode, String errorMsg) {
    }

    public void scheduleReportInstallFailedException(InstallReportInfo reportInfo) {
        Message msg = sWorkHandler.obtainMessage(100);
        msg.obj = reportInfo;
        sWorkHandler.sendMessage(msg);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class WorkHandler extends Handler {
        public WorkHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            VSlog.i(VivoPKMSReportMgr.TAG, "--handleMessage " + msg.what);
            switch (msg.what) {
                case 100:
                    VivoPKMSReportMgr.this.handlerReportApkFailed((InstallReportInfo) msg.obj);
                    return;
                case 101:
                    VivoPKMSReportMgr.this.handleScanApkFailedException((ScanFailedInfo) msg.obj);
                    return;
                case 102:
                    VivoPKMSReportMgr.this.handlerReport(msg.arg1, msg.obj);
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleScanApkFailedException(ScanFailedInfo info) {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handlerReportApkFailed(InstallReportInfo rf) {
        if (rf == null) {
            return;
        }
        reportInstallFailedExceptionToVivoEpm(rf.ipN, rf.ipCN, rf.ipVN, rf.pN, rf.pNVN, rf.update, rf.subt, rf.returnCode, rf.errorMsg, rf.versionName);
    }

    private void reportInstallFailedExceptionToVivoEpm(String ipN, String ipCN, String ipVN, String pN, String pNVN, int update, int subt, int returnCode, String errorMsg, String versionName) {
        ContentValues cv;
        try {
            cv = new ContentValues();
            try {
                cv.put("data1", ipN);
                try {
                    cv.put("data2", ipCN);
                    try {
                        cv.put("data3", ipVN);
                        try {
                            cv.put("expose", pN);
                        } catch (Exception e) {
                            e = e;
                            VSlog.w(TAG, "report to epm catch exception" + e.toString());
                        }
                    } catch (Exception e2) {
                        e = e2;
                        VSlog.w(TAG, "report to epm catch exception" + e.toString());
                    }
                } catch (Exception e3) {
                    e = e3;
                    VSlog.w(TAG, "report to epm catch exception" + e.toString());
                }
            } catch (Exception e4) {
                e = e4;
                VSlog.w(TAG, "report to epm catch exception" + e.toString());
            }
        } catch (Exception e5) {
            e = e5;
        }
        try {
            cv.put("data4", pNVN);
            cv.put("data5", Integer.valueOf(update));
            try {
                cv.put("data6", versionName);
                cv.put("subtype", Integer.valueOf(subt));
                cv.put("infokey", Integer.valueOf(returnCode));
                try {
                    cv.put("reason", buildErrorMsg(returnCode, errorMsg));
                    cv.put("level", (Integer) 3);
                    VSlog.i(TAG, "reportInfo " + cv);
                    ExceptionPolicyManager.getInstance().reportEvent(1, System.currentTimeMillis(), cv);
                } catch (Exception e6) {
                    e = e6;
                    VSlog.w(TAG, "report to epm catch exception" + e.toString());
                }
            } catch (Exception e7) {
                e = e7;
            }
        } catch (Exception e8) {
            e = e8;
            VSlog.w(TAG, "report to epm catch exception" + e.toString());
        }
    }

    private String buildErrorMsg(int returnCode, String errorMsg) {
        if (!TextUtils.isEmpty(errorMsg)) {
            return errorMsg;
        }
        String msg = PackageManager.installStatusToString(returnCode);
        return msg;
    }

    /* loaded from: classes.dex */
    static class ScanFailedInfo {
        String errorMsg;
        String packageName;
        int returnCode;

        public ScanFailedInfo(String packageName, int returnCode, String errorMsg) {
            this.packageName = packageName;
            this.returnCode = returnCode;
            this.errorMsg = errorMsg;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class InstallReportInfo {
        String errorMsg;
        String ipCN;
        String ipN;
        String ipVN;
        String pN;
        String pNVN;
        int returnCode;
        int subt;
        int update;
        String versionName;

        public InstallReportInfo(String ipN, String ipCN, String ipVN, String pN, String pNVN, int update, int subt, int returnCode, String errorMsg) {
            this.ipN = ipN;
            this.ipCN = ipCN;
            this.ipVN = ipVN;
            this.pN = pN;
            this.pNVN = pNVN;
            this.update = update;
            this.subt = subt;
            this.returnCode = returnCode;
            this.errorMsg = errorMsg;
        }
    }

    /* loaded from: classes.dex */
    public static class UninstallReportInfo {
        String pn;
        int returnCode;

        public UninstallReportInfo(String pn, int returnCode) {
            this.pn = pn;
            this.returnCode = returnCode;
        }
    }

    /* loaded from: classes.dex */
    public static class InstalledAppLostReportInfo {
        String APPstate;
        String codePath;
        String dexoptState;
        String pn;
        String primaryCpuAbi;
        String versionCode;

        public InstalledAppLostReportInfo(String pn, String codePath, String primaryCpuAbi, String versionCode, String dexoptState, String APPstate) {
            this.pn = pn;
            this.codePath = codePath;
            this.primaryCpuAbi = primaryCpuAbi;
            this.versionCode = versionCode;
            this.dexoptState = dexoptState;
            this.APPstate = APPstate;
        }
    }

    /* loaded from: classes.dex */
    public static class GetInstalledPackagesFailReportInfo {
        int callinguid;
        int flags;
        int size;

        public GetInstalledPackagesFailReportInfo(int callinguid, int flags, int size) {
            this.callinguid = callinguid;
            this.flags = flags;
            this.size = size;
        }
    }

    public void scheduleReport(int type, Bundle reportInfo) {
        Message msg = sWorkHandler.obtainMessage(102);
        msg.arg1 = type;
        msg.obj = reportInfo;
        sWorkHandler.sendMessage(msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handlerReport(int type, Object object) {
        if (object == null) {
            return;
        }
        try {
            Bundle reportInfo = (Bundle) object;
            ContentValues cv = new ContentValues();
            if (type != 39) {
                switch (type) {
                    case 13:
                        cv.put("infokey", Integer.valueOf(reportInfo.getInt("callinguid", -1)));
                        cv.put("infokey", Integer.valueOf(reportInfo.getInt("flags", -1)));
                        cv.put("data1", Integer.valueOf(reportInfo.getInt("size", -1)));
                        cv.put("level", (Integer) 3);
                        break;
                    case 14:
                        cv.put("expsrc", reportInfo.getString("pn", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK));
                        cv.put("reason", Integer.valueOf(reportInfo.getInt("returnCode", -1)));
                        cv.put("level", (Integer) 3);
                        break;
                    case 15:
                        cv.put("expose", reportInfo.getString("pn", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK));
                        cv.put("infokey", reportInfo.getString("codePath", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK));
                        cv.put("data1", reportInfo.getString("primaryCpuAbi", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK));
                        cv.put("data2", reportInfo.getString("versionCode", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK));
                        cv.put("data3", reportInfo.getString("dexoptState", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK));
                        cv.put("data4", reportInfo.getString("APPstate", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK));
                        cv.put("level", (Integer) 2);
                        break;
                }
            } else {
                cv.put("subtype", reportInfo.getString("subtype", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK));
                cv.put("expose", reportInfo.getString("expose", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK));
                cv.put("expsrc", reportInfo.getString("expsrc", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK));
                cv.put("infokey", Integer.valueOf(reportInfo.getInt("infokey", -1)));
                cv.put("infovalue", Integer.valueOf(reportInfo.getInt("infovalue", -1)));
                cv.put("data1", reportInfo.getString("data1", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK));
                cv.put("data2", reportInfo.getString("data2", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK));
                cv.put("data3", reportInfo.getString("data3", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK));
                cv.put("data4", reportInfo.getString("data4", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK));
                cv.put("level", (Integer) 5);
            }
            VSlog.i(TAG, "handlerReport reportInfo " + cv);
            ExceptionPolicyManager.getInstance().recordEvent(type, System.currentTimeMillis(), cv);
        } catch (Exception e) {
            VSlog.w(TAG, "report to epm catch exception" + e.toString());
        }
    }
}