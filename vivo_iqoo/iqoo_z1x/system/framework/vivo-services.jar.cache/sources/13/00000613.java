package com.vivo.services.autorecover;

import android.content.ComponentName;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.FtFeature;
import com.android.server.policy.InputExceptionReport;
import com.android.server.wm.ActivityRecord;
import com.vivo.common.utils.VLog;
import java.io.PrintWriter;
import vivo.app.configuration.ContentValuesList;

/* loaded from: classes.dex */
public class NoFocusWindowRecord extends InvalidWindowRecord {
    private static final boolean DEFAULT_FORCE_BG_RECOVER_ENABLED = "true".equals(FtFeature.getFeatureAttribute("vivo.opt.atypicalhang", "no_focus_time_out_force_bg_recover", "false"));
    private static final boolean DEFAULT_FORCE_FG_RECOVER_ENABLED;
    private static final String TAG_BACKGROUND_RECOVER_TIMEOUT = "background_recover_timeout";
    private static final String TAG_FORCE_BACKGROUND_RECOVER_ENABLED = "force_background_recover_enabled";
    private static final String TAG_FORCE_FOREGROUND_RECOVER_ENABLED = "force_foreground_recover_enabled";
    private static final String TAG_FOREGROUND_RECOVER_TIMEOUT = "foreground_recover_timeout";
    private static int sBackgroundRecoverTimeout;
    private static boolean sForceBgRecoverEnabled;
    private static boolean sForceFgRecoverEnabled;
    private static int sForegroundRecoverTimeout;
    public ActivityRecord mActivityRecord;

    static {
        boolean equals = "true".equals(FtFeature.getFeatureAttribute("vivo.opt.atypicalhang", "no_focus_time_out_force_fg_recover", "false"));
        DEFAULT_FORCE_FG_RECOVER_ENABLED = equals;
        sBackgroundRecoverTimeout = 10000;
        sForegroundRecoverTimeout = 20000;
        sForceBgRecoverEnabled = DEFAULT_FORCE_BG_RECOVER_ENABLED;
        sForceFgRecoverEnabled = equals;
    }

    public NoFocusWindowRecord(SystemAutoRecoverService systemAutoRecoverService, int checkingSource, boolean isOpaque, ActivityRecord activityRecord, int displayId) {
        super(systemAutoRecoverService, checkingSource, isOpaque, null, displayId);
        this.mActivityRecord = activityRecord;
    }

    public static void setConfig(ContentValuesList list) {
        try {
            String tagValue = list.getValue(TAG_FORCE_BACKGROUND_RECOVER_ENABLED);
            boolean z = true;
            sForceBgRecoverEnabled = DEFAULT_FORCE_BG_RECOVER_ENABLED && tagValue != null && Boolean.parseBoolean(tagValue);
            String tagValue2 = list.getValue(TAG_FORCE_FOREGROUND_RECOVER_ENABLED);
            if (!DEFAULT_FORCE_FG_RECOVER_ENABLED || tagValue2 == null || !Boolean.parseBoolean(tagValue2)) {
                z = false;
            }
            sForceFgRecoverEnabled = z;
            String tagValue3 = list.getValue(TAG_BACKGROUND_RECOVER_TIMEOUT);
            sBackgroundRecoverTimeout = tagValue3 != null ? Integer.parseInt(tagValue3) : 10000;
            String tagValue4 = list.getValue(TAG_FOREGROUND_RECOVER_TIMEOUT);
            sForegroundRecoverTimeout = tagValue4 != null ? Integer.parseInt(tagValue4) : 20000;
        } catch (Exception e) {
            VLog.d(SystemAutoRecoverService.TAG, "NoFocusWindowRecord setConfig cause exception: " + e.fillInStackTrace());
        }
    }

    public static void forceEnabled(boolean enabled) {
        sForceBgRecoverEnabled = enabled;
        sForceFgRecoverEnabled = enabled;
    }

    public static void forceEnabled(String tag, boolean enabled) {
        char c;
        int hashCode = tag.hashCode();
        if (hashCode != -293400624) {
            if (hashCode == 492653396 && tag.equals("nofocus-force-bg-recover")) {
                c = 0;
            }
            c = 65535;
        } else {
            if (tag.equals("nofocus-force-fg-recover")) {
                c = 1;
            }
            c = 65535;
        }
        if (c == 0) {
            sForceBgRecoverEnabled = enabled;
        } else if (c == 1) {
            sForceFgRecoverEnabled = enabled;
        }
    }

    public static void setParam(String tag, String value) {
        char c;
        int hashCode = tag.hashCode();
        if (hashCode != -293400624) {
            if (hashCode == 492653396 && tag.equals("nofocus-force-bg-recover")) {
                c = 0;
            }
            c = 65535;
        } else {
            if (tag.equals("nofocus-force-fg-recover")) {
                c = 1;
            }
            c = 65535;
        }
        if (c == 0) {
            sBackgroundRecoverTimeout = Integer.valueOf(value).intValue();
        } else if (c == 1) {
            sForegroundRecoverTimeout = Integer.valueOf(value).intValue();
        }
    }

    @Override // com.vivo.services.autorecover.InvalidWindowRecord
    public boolean checkingEnabled() {
        return true;
    }

    @Override // com.vivo.services.autorecover.InvalidWindowRecord
    public boolean isExceptionOccurs() {
        if (isInvalidWindowColorSpace()) {
            this.mReason = 6;
            return true;
        }
        return true;
    }

    @Override // com.vivo.services.autorecover.InvalidWindowRecord
    public void createException() {
        if (this.mActivityRecord != null) {
            this.mExceptionInfo = new ExceptionInfo();
            appendCommonExceptionInfo(this.mExceptionInfo);
            this.mExceptionInfo.setExceptionOse(getExceptionPackage());
            this.mExceptionInfo.setExceptionLevel("1");
            this.mExceptionInfo.setExceptionTrouble("1");
        }
    }

    @Override // com.vivo.services.autorecover.InvalidWindowRecord
    public boolean shouldRecoverImmediately() {
        return this.mService.isPunishEnabled() && this.mReason == 6 && this.mActivityRecord != null && this.mService.shouldRecover(getReason(), this.mActivityRecord.getActivityComponent().getPackageName(), this.mActivityRecord.getActivityComponent().getClassName());
    }

    @Override // com.vivo.services.autorecover.InvalidWindowRecord
    public void recover(boolean background) {
        if (this.mActivityRecord != null) {
            this.mService.forceStopPackage(this.mActivityRecord.getActivityComponent().getPackageName(), UserHandle.getUserId(this.mActivityRecord.getUidForPublic()), getReason());
            String recoveredReason = InvalidWindowRecord.recoverWayToString(background ? 4 : 8, "FORCE_STOP");
            this.mExceptionInfo.setRecoveredTime(SystemClock.elapsedRealtime());
            this.mExceptionInfo.setRecoveredReason(recoveredReason);
        }
    }

    @Override // com.vivo.services.autorecover.InvalidWindowRecord
    public void reportPendingException(boolean recovered) {
        if (this.mActivityRecord != null) {
            if (this.mReason == 6) {
                super.reportPendingException(recovered);
            } else {
                InputExceptionReport.getInstance().reportEventToEpm(this.mActivityRecord.getActivityComponent().toShortString(), 8, this.mActivityRecord.getActivityComponent().getPackageName(), getReason(), "1", "1", getMonkeyStatus());
            }
        }
    }

    @Override // com.vivo.services.autorecover.InvalidWindowRecord
    public int recoverWay() {
        return 31;
    }

    @Override // com.vivo.services.autorecover.InvalidWindowRecord
    public boolean canRecover(boolean force, boolean background, int recoverWay, long now) {
        long duration = now - this.mExceptionInfo.getOccuredElapsedTime();
        VLog.d(SystemAutoRecoverService.TAG, "try to recover exception force = " + force + " background = " + background + " recoverway = " + recoverWayToString(recoverWay, null) + " duration = " + duration);
        if ((recoverWay() & recoverWay) != 0) {
            if (!force) {
                return true;
            }
            String packageName = this.mActivityRecord.getActivityComponent().getPackageName();
            if (this.mService.isCtsPackage(packageName)) {
                VLog.d(SystemAutoRecoverService.TAG, "Ignore no focus time out exception of cts package!");
                return false;
            } else if (this.mService.isDebugEnabled(packageName)) {
                VLog.d(SystemAutoRecoverService.TAG, "Ignore no focus time out exception when debug!");
                return false;
            } else if (background) {
                if (sForceBgRecoverEnabled && duration > sBackgroundRecoverTimeout) {
                    return true;
                }
            } else if (sForceFgRecoverEnabled && duration > sForegroundRecoverTimeout) {
                return true;
            }
        }
        return false;
    }

    @Override // com.vivo.services.autorecover.InvalidWindowRecord
    protected int getExceptionReportDelayMills() {
        return sForegroundRecoverTimeout;
    }

    public static void dump(String prefix, PrintWriter pw) {
        pw.println(prefix + "NoFocusWindowConfig");
        String prefix2 = prefix + "    ";
        pw.println(prefix2 + "sForceBackgroundRecoverEnabled = " + sForceBgRecoverEnabled);
        pw.println(prefix2 + "sForceForegroundRecoverEnabled = " + sForceFgRecoverEnabled);
        pw.println(prefix2 + "sBackgroundRecoverTimeout = " + sBackgroundRecoverTimeout);
        pw.println(prefix2 + "sForegroundRecoverTimeout = " + sForegroundRecoverTimeout);
    }

    @Override // com.vivo.services.autorecover.InvalidWindowRecord
    public String getExceptionSource() {
        ComponentName componentName = this.mActivityRecord.getActivityComponent();
        return componentName.getPackageName() + "/" + componentName.getClassName();
    }

    @Override // com.vivo.services.autorecover.InvalidWindowRecord
    public String getExceptionPackage() {
        return this.mActivityRecord.getActivityComponent().getPackageName();
    }

    @Override // com.vivo.services.autorecover.InvalidWindowRecord
    public int getExceptionPid() {
        return this.mActivityRecord.getPidForPublic();
    }

    @Override // com.vivo.services.autorecover.InvalidWindowRecord
    protected void appendDropBoxContent(StringBuilder sb) {
        this.mService.appendCurrentCpuState(sb);
        appendStackTrace(sb);
    }
}