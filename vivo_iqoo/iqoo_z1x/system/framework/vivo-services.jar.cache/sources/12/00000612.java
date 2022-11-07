package com.vivo.services.autorecover;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.graphics.common.V1_0.Dataspace;
import android.os.Debug;
import android.os.FileUtils;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.SurfaceControl;
import android.view.ViewDebug;
import android.view.WindowManager;
import com.android.server.am.EmergencyBroadcastManager;
import com.android.server.policy.VivoPolicyUtil;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.WindowState;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import com.vivo.services.rms.sdk.Consts;
import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import vivo.app.configuration.ContentValuesList;

/* loaded from: classes.dex */
public abstract class InvalidWindowRecord {
    public static final int CHECKING_SOURCE_FOCUS_CHANGED = 3;
    public static final int CHECKING_SOURCE_INPUT = 1;
    public static final int CHECKING_SOURCE_NO_FOCUS_TIME_OUT = 4;
    @Deprecated
    public static final int CHECKING_SOURCE_SHOW_ABOVE_INCALL = 2;
    private static final int JAVA_DUMP_MINIMUM_SIZE = 100;
    public static final int REASON_BLACK_WINDOW = 6;
    public static final int REASON_INVALID_WINDOW_SIZE = 1;
    public static final int REASON_NO_ERROR = -1;
    public static final int REASON_NO_FOCUSED_WINDOW_TIME_OUT = 4;
    public static final int REASON_STARTING_WINDOW_BACK_KEY = 5;
    public static final int REASON_TRANSPARENT_ACTIVITY = 3;
    public static final int REASON_TRANSPARENT_WINDOW = 2;
    public static final int RECOVER_BY_ARS_BACKGROUND = 4;
    public static final int RECOVER_BY_ARS_FOREGROUND = 8;
    public static final int RECOVER_BY_FOCUS_CHANGED = 1;
    public static final int RECOVER_BY_FOCUS_KILLED_OR_DIED = 2;
    public static final int RECOVER_BY_SCREEN_OFF = 16;
    private static final String TAG_ARS_DROPBOX_ENABLED = "ars_dropbox_enabled";
    protected int mCheckingSource;
    protected int mDisplayId;
    protected ExceptionInfo mExceptionInfo;
    protected boolean mIsOpaque;
    protected final SystemAutoRecoverService mService;
    protected WindowState mWin;
    private static final String TAG_PROP_DROPBOX_ENABLE = "persist.vivo.ars.dropbox";
    private static final boolean DEFAULT_ARS_DROPBOX_ENABLED = "true".equals(SystemProperties.get(TAG_PROP_DROPBOX_ENABLE, "false"));
    public static float sScreenShotScale = SystemProperties.getInt("persist.vivo.screenshot.scale", 25) / 100.0f;
    private static boolean sReportDropBoxEnabled = DEFAULT_ARS_DROPBOX_ENABLED;
    protected String mScene = "UNKNOWN";
    protected int mReason = -1;

    protected abstract void appendDropBoxContent(StringBuilder sb);

    public abstract boolean checkingEnabled();

    public abstract void createException();

    public abstract String getExceptionPackage();

    public abstract int getExceptionPid();

    public abstract String getExceptionSource();

    public abstract boolean isExceptionOccurs();

    public abstract void recover(boolean z);

    public abstract int recoverWay();

    public abstract boolean shouldRecoverImmediately();

    public InvalidWindowRecord(SystemAutoRecoverService systemAutoRecoverService, int checkingSource, boolean isOpaque, WindowState win, int displayId) {
        this.mService = systemAutoRecoverService;
        this.mCheckingSource = checkingSource;
        this.mWin = win;
        this.mIsOpaque = isOpaque;
        this.mDisplayId = displayId;
    }

    public static void setConfig(ContentValuesList list) {
        boolean z;
        try {
            String tagValue = list.getValue(TAG_ARS_DROPBOX_ENABLED);
            if (!DEFAULT_ARS_DROPBOX_ENABLED && !Boolean.parseBoolean(tagValue)) {
                z = false;
                sReportDropBoxEnabled = z;
            }
            z = true;
            sReportDropBoxEnabled = z;
        } catch (Exception e) {
            VLog.e(SystemAutoRecoverService.TAG, "InvalidWindowRecord setConfig cause exception:" + e.getMessage());
        }
    }

    public static void reloadConfig() {
        sScreenShotScale = SystemProperties.getInt("persist.vivo.screenshot.scale", 25) / 100.0f;
        String reportToDropBoxProp = SystemProperties.get(TAG_PROP_DROPBOX_ENABLE);
        if (!TextUtils.isEmpty(reportToDropBoxProp)) {
            sReportDropBoxEnabled = "true".equals(reportToDropBoxProp);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isActivity() {
        WindowState windowState = this.mWin;
        return windowState != null && windowState.getAttrs().type == 1;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isOpaque() {
        WindowState windowState = this.mWin;
        if (windowState != null) {
            WindowManager.LayoutParams attrs = windowState.getAttrs();
            boolean isActivity = attrs.type == 1;
            if (!isActivity) {
                return attrs.format == -1;
            }
            ActivityRecord activityRecord = this.mWin.getActivityRecord();
            return activityRecord == null || activityRecord.isWindowOpaque() || attrs.format == -1;
        }
        return true;
    }

    public static String reasonToString(int reason) {
        switch (reason) {
            case 1:
                return "INVALID_WINDOW_SIZE";
            case 2:
                return "TRANSPARENT_WINDOW";
            case 3:
                return "TRANSPARENT_ACTIVITY";
            case 4:
                return "NO_FOCUSED_WINDOW_TIME_OUT";
            case 5:
                return "STARTING_WINDOW_BACK_KEY";
            case 6:
                return "BLACK_WINDOW";
            default:
                return "UNKNOWN";
        }
    }

    public static String checkSourceToString(int checkingSource) {
        if (checkingSource != 1) {
            if (checkingSource != 3) {
                if (checkingSource == 4) {
                    return "CHECK_FROM_NO_FOCUS_TIME_OUT";
                }
                return "CHECK_FROM_UNKNOWN";
            }
            return "CHECK_FROM_FOCUS_CHANGE";
        }
        return "CHECK_FROM_INPUT";
    }

    public static String recoverWayToString(int recoverMethod, String subReason) {
        String recoverMethodString;
        if (recoverMethod == 1) {
            recoverMethodString = "FOCUS_CHANGED";
        } else if (recoverMethod == 2) {
            recoverMethodString = "KILLED_OR_DIED";
        } else if (recoverMethod == 4) {
            recoverMethodString = "ARS_BACKGROUND";
        } else if (recoverMethod == 8) {
            recoverMethodString = "ARS_FOREGROUND";
        } else if (recoverMethod == 16) {
            recoverMethodString = "SCREEN_OFF";
        } else {
            recoverMethodString = "UNKNOWN";
        }
        if (subReason != null) {
            return recoverMethodString + "_" + subReason;
        }
        return recoverMethodString;
    }

    public String getReason() {
        return reasonToString(this.mReason) + "_" + checkSourceToString(this.mCheckingSource);
    }

    protected int getExceptionReportDelayMills() {
        return 30000;
    }

    protected SystemAutoRecoverService getService() {
        return this.mService;
    }

    public boolean checkException() {
        VLog.d(SystemAutoRecoverService.TAG, "start checking " + getClass().toString());
        if (checkingEnabled() && isExceptionOccurs()) {
            createException();
            boolean recovered = false;
            if (shouldRecoverImmediately()) {
                recover(false);
                recovered = true;
            }
            reportPendingException(recovered);
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void appendCommonExceptionInfo(ExceptionInfo exceptionInfo) {
        exceptionInfo.setExceptionSource(getExceptionSource());
        exceptionInfo.setExceptionPackageName(getExceptionPackage());
        exceptionInfo.setReason(getReason());
        exceptionInfo.setPid(getExceptionPid());
    }

    public void reportPendingException(boolean recovered) {
        ExceptionInfo exceptionInfo = this.mExceptionInfo;
        if (exceptionInfo != null) {
            exceptionInfo.setMonkeyState(getMonkeyStatus());
            this.mExceptionInfo.setDebugState(this.mService.getDebugState(getExceptionPackage()));
            this.mService.reportException(this, recovered ? 0 : getExceptionReportDelayMills());
            reportToDropBox();
        }
    }

    public boolean canRecover(boolean force, boolean background, int recoverWay, long now) {
        return ((recoverWay() & recoverWay) == 0 || force) ? false : true;
    }

    public ExceptionInfo getExceptionInfo() {
        return this.mExceptionInfo;
    }

    public void report() {
        ExceptionInfo exceptionInfo = this.mExceptionInfo;
        if (exceptionInfo != null) {
            exceptionInfo.reportException(this.mService.mContext);
        }
    }

    private void reportToDropBox() {
        if (sReportDropBoxEnabled) {
            long startTime = SystemClock.uptimeMillis();
            this.mService.addToDropBox(getReason(), getDropBoxData());
            VLog.d(SystemAutoRecoverService.TAG, "reportToDropBox took " + (SystemClock.uptimeMillis() - startTime) + " ms");
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isInvalidWindowColorSpace() {
        boolean isInvalidColorSpace;
        WindowState windowState = this.mWin;
        if (windowState != null) {
            if (!windowState.isVisibleLw()) {
                VLog.d(SystemAutoRecoverService.TAG, this.mWin + " is not visible");
                return false;
            } else if (hasSecureFlag()) {
                VLog.d(SystemAutoRecoverService.TAG, this.mWin + " has secure flag, ignore checking!");
                return false;
            } else if (this.mService.isPasswordMode()) {
                VLog.d(SystemAutoRecoverService.TAG, "Password mode! Ignore checking!");
                return false;
            }
        }
        long startTime = SystemClock.uptimeMillis();
        Bitmap bitmap = null;
        if (this.mIsOpaque) {
            if (!this.mService.shouldCheckAppDrawBlack()) {
                VLog.d(SystemAutoRecoverService.TAG, "check black feature not enabled!");
                return false;
            }
            bitmap = takeScreenShot(this.mDisplayId);
        } else {
            WindowState windowState2 = this.mWin;
            if (windowState2 != null) {
                bitmap = takeLayerScreenShot(windowState2);
            }
        }
        if (bitmap == null) {
            VLog.d(SystemAutoRecoverService.TAG, "Failed to wrap bitmap from screen shot");
            return false;
        }
        if (VivoPolicyUtil.IS_LOG_OPEN) {
            VLog.d(SystemAutoRecoverService.TAG, "Take screenshot cost " + (SystemClock.uptimeMillis() - startTime) + " mills");
        }
        long startTime2 = SystemClock.uptimeMillis();
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (VivoPolicyUtil.IS_LOG_OPEN) {
            VLog.d(SystemAutoRecoverService.TAG, "Screenshot bitmap width = " + width + " height = " + height);
        }
        if (width <= 1 || height <= 1) {
            VLog.d(SystemAutoRecoverService.TAG, "Ignore invalid bitmap size!");
            return false;
        }
        Trace.traceBegin(32L, "checkColoSpace");
        if (this.mIsOpaque) {
            isInvalidColorSpace = AlgorithmUtil.isBlack(bitmap);
        } else {
            isInvalidColorSpace = AlgorithmUtil.isTransparent(bitmap);
        }
        Trace.traceEnd(32L);
        if (VivoPolicyUtil.IS_LOG_OPEN) {
            VLog.d(SystemAutoRecoverService.TAG, "Check color space cost " + (SystemClock.uptimeMillis() - startTime2) + " mills");
            StringBuilder sb = new StringBuilder();
            String str = this.mWin;
            if (str == null) {
                str = "Screen";
            }
            sb.append((Object) str);
            sb.append(" is");
            sb.append(isInvalidColorSpace ? Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK : " not");
            sb.append(this.mIsOpaque ? " black" : " transparent");
            VLog.d(SystemAutoRecoverService.TAG, sb.toString());
        }
        if (AlgorithmUtil.DEBUG_SCREEN_SHOT) {
            debugScreenShot(bitmap, isInvalidColorSpace, this.mIsOpaque);
        }
        if (bitmap != null) {
            bitmap.recycle();
        }
        return isInvalidColorSpace;
    }

    private boolean hasSecureFlag() {
        WindowState windowState = this.mWin;
        return (windowState == null || (windowState.getAttrs().flags & EmergencyBroadcastManager.FLAG_RECEIVER_KEYAPP) == 0) ? false : true;
    }

    private Bitmap takeScreenShot(int displayId) {
        try {
            Display display = this.mService.getDisplay(displayId);
            if (display != null) {
                DisplayMetrics displayMetrics = new DisplayMetrics();
                display.getRealMetrics(displayMetrics);
                int rotation = display.getRotation();
                Rect outRect = new Rect(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);
                Rect stableInset = new Rect();
                this.mService.getStableInsets(displayId, stableInset);
                outRect.set(outRect.left + stableInset.left, outRect.top + stableInset.top, outRect.right - stableInset.right, outRect.bottom - stableInset.bottom);
                VLog.d(SystemAutoRecoverService.TAG, "takeScreenShot outRect = " + outRect + " rotation = " + rotation);
                convertCropForSurfaceFlinger(outRect, rotation, outRect.width(), outRect.height());
                Bitmap bitmap = SurfaceControl.screenshot(outRect, (int) (((float) outRect.width()) * sScreenShotScale), (int) (((float) outRect.height()) * sScreenShotScale), rotation);
                if (bitmap != null) {
                    if (bitmap.getConfig() != Bitmap.Config.ARGB_8888) {
                        return bitmap.copy(Bitmap.Config.ARGB_8888, true);
                    }
                    return bitmap;
                }
                return null;
            }
            return null;
        } catch (Exception e) {
            VLog.d(SystemAutoRecoverService.TAG, "takeScreenShot cause exception: " + e);
            return null;
        }
    }

    private void getStableRect(Rect outRect, int displayId, int rotation) {
        try {
            Rect stableInset = new Rect();
            this.mService.getStableInsetsWithoutCutout(displayId, stableInset);
            outRect.set(outRect.left + stableInset.left, outRect.top + stableInset.top, outRect.right - stableInset.right, outRect.bottom - stableInset.bottom);
            VLog.d(SystemAutoRecoverService.TAG, "getStableRect outRect = " + outRect + " stableInset = " + stableInset + " rotation = " + rotation);
        } catch (RemoteException e) {
            VLog.d(SystemAutoRecoverService.TAG, "getStableInsets cause exception:" + e);
        }
    }

    private static void convertCropForSurfaceFlinger(Rect crop, int rot, int dw, int dh) {
        if (rot == 1) {
            crop.offsetTo(crop.left, 0);
        } else if (rot == 2) {
            int tmp = crop.top;
            crop.top = dh - crop.bottom;
            crop.bottom = dh - tmp;
            int tmp2 = crop.right;
            crop.right = dw - crop.left;
            crop.left = dw - tmp2;
        }
    }

    private Bitmap takeLayerScreenShot(WindowState windowState) {
        int displayId;
        Display display;
        Rect bounds = windowState.getBounds();
        bounds.offsetTo(0, 0);
        int width = windowState.getFrameLw().right - windowState.getFrameLw().left;
        int height = windowState.getFrameLw().bottom - windowState.getFrameLw().top;
        bounds.set(0, 0, width, height);
        if (isActivity() && (display = this.mService.getDisplay((displayId = windowState.getDisplayId()))) != null) {
            getStableRect(bounds, displayId, display.getRotation());
        }
        if (VivoPolicyUtil.IS_LOG_OPEN) {
            VLog.d(SystemAutoRecoverService.TAG, windowState + " bounds = " + bounds);
        }
        if (bounds.width() <= 0 || bounds.height() <= 0) {
            VLog.d(SystemAutoRecoverService.TAG, "Invalid bounds size bounds = " + bounds);
            return null;
        }
        if (windowState.getSurfaceControl() != null) {
            SurfaceControl.ScreenshotGraphicBuffer windowBuffer = this.mService.captureLayers(windowState.getSurfaceControl(), bounds, sScreenShotScale);
            if (windowBuffer != null) {
                Bitmap bitmap = Bitmap.wrapHardwareBuffer(windowBuffer.getGraphicBuffer(), windowBuffer.getColorSpace());
                if (bitmap != null) {
                    int bitmapWidth = bitmap.getWidth();
                    int bitmapHeight = bitmap.getHeight();
                    if (bitmapWidth > bounds.width() || bitmapHeight > bounds.height()) {
                        VLog.d(SystemAutoRecoverService.TAG, "layer bitmap is large than window bounds, maybe something wrong!");
                        return null;
                    } else if (bitmap.getConfig() != Bitmap.Config.ARGB_8888) {
                        return bitmap.copy(Bitmap.Config.ARGB_8888, true);
                    } else {
                        return bitmap;
                    }
                }
            } else {
                VLog.d(SystemAutoRecoverService.TAG, "Failed to screenshot window: captureLayers got null");
            }
        } else {
            VLog.d(SystemAutoRecoverService.TAG, "Failed to screenshot window: getSurfaceControl() got null");
        }
        return null;
    }

    private void debugScreenShot(Bitmap bitmap, boolean invalid, boolean opaque) {
        if (bitmap == null) {
            return;
        }
        try {
            StringBuilder tmpStringBuilder = new StringBuilder(128);
            if (invalid) {
                if (opaque) {
                    tmpStringBuilder.append("BlackScreen");
                } else {
                    tmpStringBuilder.append("TransparentWindow");
                }
                tmpStringBuilder.append("_");
            }
            tmpStringBuilder.append(getExceptionSource());
            tmpStringBuilder.append("_");
            Date stamp = new Date(System.currentTimeMillis());
            tmpStringBuilder.append(new SimpleDateFormat("yyyyMMdd-HHmmss").format(stamp));
            String tmpString = tmpStringBuilder.toString().replace("/", "_").replace(":", "_").replace(" ", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK).replace("{", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK).replace("}", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
            String[] splits = tmpString.split("_");
            ArrayList<String> splitArray = new ArrayList<>();
            for (String subString : splits) {
                if (!splitArray.contains(subString)) {
                    splitArray.add(subString);
                }
            }
            StringBuilder fileName = new StringBuilder(128);
            for (int i = 0; i < splitArray.size(); i++) {
                fileName.append(splitArray.get(i));
                if (i != splitArray.size() - 1) {
                    fileName.append("_");
                } else {
                    fileName.append(".png");
                }
            }
            AlgorithmUtil.debugScreenShot(fileName.toString(), bitmap);
        } catch (Exception e) {
            VLog.d(SystemAutoRecoverService.TAG, "debugScreenShot cause exception : " + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public String windowRectToString() {
        WindowState windowState = this.mWin;
        if (windowState != null) {
            return windowState.getFrameLw().toShortString();
        }
        return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public String windowTypeToString() {
        WindowState windowState = this.mWin;
        if (windowState != null) {
            return ViewDebug.intToString(WindowManager.LayoutParams.class, "type", windowState.getAttrs().type);
        }
        return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public String getMonkeyStatus() {
        return String.valueOf(this.mService.isUserAMonkey());
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public String getIncallStatus() {
        return String.valueOf(this.mService.isIncall());
    }

    protected String getDropBoxData() {
        StringBuilder sb = new StringBuilder((int) Consts.ProcessStates.FOCUS);
        appendDropBoxHeader(sb);
        appendDropBoxContent(sb);
        return sb.toString();
    }

    protected void appendDropBoxHeader(StringBuilder sb) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        Date timestamp = new Date();
        String vivoVersion = SystemProperties.get("ro.build.version.bbk", "Unavailable");
        sb.append("\n");
        sb.append("Time: ");
        sb.append(sdf.format(timestamp));
        sb.append("\n");
        sb.append("Softversion: ");
        sb.append(vivoVersion);
        sb.append("\n");
        sb.append("Process: ");
        sb.append(getExceptionPackage());
        sb.append("\n");
        sb.append("Exception Source: ");
        sb.append(getExceptionSource());
        sb.append("\n");
        sb.append("Reason: ");
        sb.append(getReason());
        sb.append("\n");
        sb.append("\n");
    }

    public void appendStackTrace(StringBuilder sb) {
        StringBuilder sb2;
        File tracesDir = new File("/data/anr");
        File tracesFile = null;
        try {
            try {
                tracesFile = File.createTempFile(getReason(), null, tracesDir);
                dumpJavaTracesTombstoned(getExceptionPid(), tracesFile.getAbsolutePath());
                int maxDataFileSize = Dataspace.STANDARD_BT601_625_UNADJUSTED - sb.length();
                sb.append(FileUtils.readTextFile(tracesFile, maxDataFileSize, "\n\n[[TRUNCATED]]"));
                if (tracesFile != null) {
                    try {
                        tracesFile.delete();
                    } catch (Exception e) {
                        e = e;
                        sb2 = new StringBuilder();
                        sb2.append("delete traces cause exception:");
                        sb2.append(e);
                        VLog.d(SystemAutoRecoverService.TAG, sb2.toString());
                    }
                }
            } catch (Throwable th) {
                if (tracesFile != null) {
                    try {
                        tracesFile.delete();
                    } catch (Exception e2) {
                        VLog.d(SystemAutoRecoverService.TAG, "delete traces cause exception:" + e2);
                    }
                }
                throw th;
            }
        } catch (Exception e3) {
            VLog.d(SystemAutoRecoverService.TAG, "appendStackTrace cause exception:" + e3);
            if (tracesFile != null) {
                try {
                    tracesFile.delete();
                } catch (Exception e4) {
                    e = e4;
                    sb2 = new StringBuilder();
                    sb2.append("delete traces cause exception:");
                    sb2.append(e);
                    VLog.d(SystemAutoRecoverService.TAG, sb2.toString());
                }
            }
        }
    }

    public static void dumpJavaTracesTombstoned(int pid, String fileName) {
        boolean javaSuccess = Debug.dumpJavaBacktraceToFileTimeout(pid, fileName, 10);
        if (javaSuccess) {
            try {
                long size = new File(fileName).length();
                if (size < 100) {
                    VLog.w(SystemAutoRecoverService.TAG, "Successfully created Java ANR file is empty!");
                }
            } catch (Exception e) {
                VLog.w(SystemAutoRecoverService.TAG, "Unable to get ANR file size", e);
            }
        }
    }

    public void appendDumpsysActivity(String activity, StringBuilder sb) {
        StringBuilder sb2;
        File dumpDir = new File("/data/anr");
        File dumpFile = null;
        try {
            try {
                dumpFile = File.createTempFile(getReason(), null, dumpDir);
                FileUtils.setPermissions(dumpFile.getPath(), 438, -1, -1);
                String dumpsysActivity = "Gspc1F1qZ/qUbGf8BrWYrtzTbpAEv0H6n1AWTyO/QT5jR42+YqhK6oSVbV60I79ckQxXLwTMp74T+bsaC/KVSNsWqcZmi110se/Y5m5GbX4ZVhKT6dkFO+VQbaRSGW+lhtEJOr0O1rifR74u0FsjnOdnpHQ5ECje9cSyEKah3/QN7L++CXbmkfTAsE35Z6yrd3rlxevvCS9Jm7h3lOJlSsuC6zb2mcriiuQ+Ze8WUgs8QkxrAx4ISsMNbqJt/E26zWzWGIDM2PAY3yT0U/B48Jd6fHOltupDgzm5Y6jmgE3IjLHRphkaOWJWxWTEk2IJg3NPifeLv+qf4ASQSbUQlw==?" + activity + "?" + dumpFile.getAbsolutePath();
                this.mService.runShellCommand(dumpsysActivity);
                int maxDataFileSize = Dataspace.STANDARD_BT601_625_UNADJUSTED - sb.length();
                sb.append(FileUtils.readTextFile(dumpFile, maxDataFileSize, "\n\n[[TRUNCATED]]"));
                if (dumpFile != null) {
                    try {
                        dumpFile.delete();
                    } catch (Exception e) {
                        e = e;
                        sb2 = new StringBuilder();
                        sb2.append("delete dump file cause exception: ");
                        sb2.append(e);
                        VLog.d(SystemAutoRecoverService.TAG, sb2.toString());
                    }
                }
            } catch (Exception e2) {
                VLog.d(SystemAutoRecoverService.TAG, "appendDumpsysActivity cause exception: " + e2);
                if (dumpFile != null) {
                    try {
                        dumpFile.delete();
                    } catch (Exception e3) {
                        e = e3;
                        sb2 = new StringBuilder();
                        sb2.append("delete dump file cause exception: ");
                        sb2.append(e);
                        VLog.d(SystemAutoRecoverService.TAG, sb2.toString());
                    }
                }
            }
        } catch (Throwable th) {
            if (dumpFile != null) {
                try {
                    dumpFile.delete();
                } catch (Exception e4) {
                    VLog.d(SystemAutoRecoverService.TAG, "delete dump file cause exception: " + e4);
                }
            }
            throw th;
        }
    }

    public static void dump(String prefix, PrintWriter pw) {
        pw.println(prefix + "InvalidWindowConfig");
        String prefix2 = prefix + "    ";
        pw.println(prefix2 + "sScreenShotScale = " + sScreenShotScale);
        pw.println(prefix2 + "sReportDropBoxEnabled = " + sReportDropBoxEnabled);
    }

    public String toString() {
        return "InvalidWindowRecord{mExceptionInfo=" + this.mExceptionInfo + '}';
    }
}