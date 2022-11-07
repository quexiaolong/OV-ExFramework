package com.vivo.services.autorecover;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.text.TextUtils;
import com.android.server.policy.InputExceptionReport;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import java.util.ArrayList;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class ExceptionInfo {
    private static final String CDS_FLAG = "cdsFlag";
    private static final String DEBUG_STATE = "data8";
    private static final String EVENT_ID_VALUE = "00077|012";
    private static final String EXCEPTION_REASON = "reason";
    private static final String EXCEPTION_SOURCE = "expsrc";
    private static final String EXCEPTION_TIME = "otime";
    private static final String EXCEPTION_TIMES = "times";
    private static final String EXCEPTION_TYPE = "extype";
    private static final String INCALL_STATE = "data7";
    private static final int MODULE_ID_VALUE = 400;
    private static final String MONKEY_STATE = "data3";
    private static final String PACKAGE_NAME = "data6";
    private static final String RECOVERED_DURATION = "data4";
    private static final String RECOVERED_REASON = "data5";
    private static final String SARS_VERSION = "version";
    private static final String SUB_TYPE = "subtype";
    private static final String SYS_VERSION = "osysversion";
    private static final String VERSION_INVALID_WINDOW = "5";
    private static final String WINDOW_RECT = "data2";
    private static final String WINDOW_TYPE = "data1";
    private String mExceptionLevel;
    private String mExceptionOse;
    private String mExceptionPackageName;
    private String mExceptionSource;
    private String mExceptionTrouble;
    private String mReason;
    private String mRecoveredReason;
    private String mWindowRect;
    private String mWindowType;
    private int mPid = -1;
    private String mMonkeyState = "false";
    private long mRecoveredElapsedRealtime = -1;
    private String mIncallState = "false";
    private String mDebugState = "DISABLED";
    private String mSubType = InputExceptionReport.getSubType(6);
    private String mVersion = "5";
    private final long mOccuredTime = System.currentTimeMillis();
    private final long mOccuredElapsedRealtime = SystemClock.elapsedRealtime();

    public void setExceptionSource(String exceptionSource) {
        this.mExceptionSource = exceptionSource;
    }

    public void setExceptionPackageName(String exceptionPackageName) {
        this.mExceptionPackageName = exceptionPackageName;
    }

    public void setExceptionOse(String exceptionOse) {
        this.mExceptionOse = exceptionOse;
    }

    public void setExceptionLevel(String exceptionLevel) {
        this.mExceptionLevel = exceptionLevel;
    }

    public void setExceptionTrouble(String exceptionTrouble) {
        this.mExceptionTrouble = exceptionTrouble;
    }

    public String getExceptionSource() {
        return this.mExceptionSource;
    }

    public String getExceptionOse() {
        return this.mExceptionOse;
    }

    public String getReason() {
        return this.mReason;
    }

    public void setReason(String reason) {
        this.mReason = reason;
    }

    public void setPid(int pid) {
        this.mPid = pid;
    }

    public int getPid() {
        return this.mPid;
    }

    public long getOccuredElapsedTime() {
        return this.mOccuredElapsedRealtime;
    }

    public void setWindowType(String windowType) {
        this.mWindowType = windowType;
    }

    public void setWindowRect(String windowRect) {
        this.mWindowRect = windowRect;
    }

    public void setMonkeyState(String monkeyState) {
        this.mMonkeyState = monkeyState;
    }

    public void setRecoveredTime(long recoveredTime) {
        this.mRecoveredElapsedRealtime = recoveredTime;
    }

    public void setRecoveredReason(String recoveredReason) {
        this.mRecoveredReason = recoveredReason;
    }

    public void setIncallState(String incallState) {
        this.mIncallState = incallState;
    }

    public void setDebugState(String debugState) {
        this.mDebugState = debugState;
    }

    public void reportException(Context context) {
        try {
            ArrayList<String> data = new ArrayList<>();
            JSONObject dt = new JSONObject();
            dt.put(EXCEPTION_TYPE, reasonToType(this.mReason));
            dt.put(SUB_TYPE, "2");
            dt.put(SYS_VERSION, SystemProperties.get("ro.build.version.bbk"));
            dt.put(EXCEPTION_TIME, this.mOccuredTime);
            dt.put(EXCEPTION_TIMES, "1");
            dt.put(EXCEPTION_REASON, this.mReason);
            dt.put(SARS_VERSION, this.mVersion);
            dt.put(EXCEPTION_SOURCE, this.mExceptionSource);
            if (this.mWindowType != null) {
                dt.put(WINDOW_TYPE, this.mWindowType);
            }
            if (this.mWindowRect != null) {
                dt.put(WINDOW_RECT, this.mWindowRect);
            }
            dt.put(MONKEY_STATE, this.mMonkeyState);
            if (this.mRecoveredElapsedRealtime != -1) {
                long recoverDuration = (this.mRecoveredElapsedRealtime - this.mOccuredElapsedRealtime) / 1000;
                dt.put(RECOVERED_DURATION, recoverDuration + Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                dt.put(RECOVERED_REASON, this.mRecoveredReason);
            }
            dt.put(PACKAGE_NAME, this.mExceptionPackageName);
            dt.put(INCALL_STATE, this.mIncallState);
            dt.put(DEBUG_STATE, this.mDebugState);
            dt.put(CDS_FLAG, "1");
            data.add(iod(EVENT_ID_VALUE, dt));
            sd(context, 400, data);
        } catch (Exception e) {
            VLog.d(SystemAutoRecoverService.TAG, "report exception cause exception: " + e);
        }
    }

    private static String iod(String eventId, JSONObject dt) {
        if (TextUtils.isEmpty(eventId) || dt == null || dt.length() <= 0) {
            VLog.e(SystemAutoRecoverService.TAG, "Waring, invalid id or dt!!!");
            return null;
        }
        try {
            JSONObject data = new JSONObject();
            data.put("eventId", eventId);
            data.put("dt", dt);
            return data.toString();
        } catch (JSONException e) {
            return null;
        }
    }

    private void sd(Context context, int moduleId, ArrayList<String> data) {
        if (moduleId <= 0 || data == null || data.size() <= 0) {
            VLog.e(SystemAutoRecoverService.TAG, "Waring, invalid info!!!");
            return;
        }
        Intent it = new Intent("com.vivo.intent.action.CLOUD_DIAGNOSIS");
        it.putExtra("attr", 1);
        it.putExtra("module", moduleId);
        it.putStringArrayListExtra("data", data);
        it.setPackage("com.bbk.iqoo.logsystem");
        context.sendBroadcast(it);
    }

    public static String diedReasonCodeToString(int reason) {
        switch (reason) {
            case 1:
                return "EXIT_SELF";
            case 2:
                return "SIGNALED";
            case 3:
                return "LOW_MEMORY";
            case 4:
                return "APP CRASH(EXCEPTION)";
            case 5:
                return "APP CRASH(NATIVE)";
            case 6:
                return "ANR";
            case 7:
                return "INITIALIZATION FAILURE";
            case 8:
                return "PERMISSION CHANGE";
            case 9:
                return "EXCESSIVE RESOURCE USAGE";
            case 10:
                return "USER REQUESTED";
            case 11:
                return "USER STOPPED";
            case 12:
                return "DEPENDENCY DIED";
            case 13:
                return "OTHER KILLS BY SYSTEM";
            default:
                return "UNKNOWN";
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private static String reasonToType(String reason) {
        char c;
        switch (reason.hashCode()) {
            case -1753230693:
                if (reason.equals("BLACK_WINDOW_CHECK_FROM_INPUT")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case -1743281937:
                if (reason.equals("TRANSPARENT_ACTIVITY_CHECK_FROM_INPUT")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case -662738626:
                if (reason.equals("INVALID_WINDOW_SIZE_CHECK_FROM_FOCUS_CHANGE")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 63319395:
                if (reason.equals("INVALID_WINDOW_SIZE_CHECK_FROM_INPUT")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 1768177170:
                if (reason.equals("BLACK_WINDOW_CHECK_FROM_NO_FOCUS_TIME_OUT")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 1972904686:
                if (reason.equals("TRANSPARENT_WINDOW_CHECK_FROM_INPUT")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        if (c != 0) {
            if (c != 1) {
                if (c != 3) {
                    if (c != 4) {
                        if (c == 5) {
                            return "106";
                        }
                        return "103";
                    }
                    return "105";
                }
                return "104";
            }
            return "102";
        }
        return "101";
    }

    public String toString() {
        return "ExceptionInfo{mPid=" + this.mPid + ", mSubType='" + this.mSubType + "', mExceptionSource='" + this.mExceptionSource + "', mExceptionPackageName='" + this.mExceptionPackageName + "', mWindowType='" + this.mWindowType + "', mWindowRect='" + this.mWindowRect + "', mExceptionOse='" + this.mExceptionOse + "', mVersion='" + this.mVersion + "', mExceptionLevel='" + this.mExceptionLevel + "', mExceptionTrouble='" + this.mExceptionTrouble + "', mMonkeyState='" + this.mMonkeyState + "', mReason='" + this.mReason + "', mOccuredElapsedRealtime=" + this.mOccuredElapsedRealtime + ", mRecoveredElapsedRealtime=" + this.mRecoveredElapsedRealtime + ", mRecoveredReason='" + this.mRecoveredReason + "', mIncallState=" + this.mIncallState + ", mDebugState=" + this.mDebugState + '}';
    }
}