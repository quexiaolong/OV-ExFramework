package com.vivo.services.autorecover;

import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import com.android.server.policy.InputExceptionReport;
import com.android.server.wm.WindowState;
import com.vivo.common.utils.VLog;

/* loaded from: classes.dex */
public class InvalidSizeWindowRecord extends InvalidWindowRecord {
    public static final int INVALID_WINDOW_SIZE_THRESHOLD = SystemProperties.getInt("persist.vivo.invalid.window.size", 10);

    public InvalidSizeWindowRecord(SystemAutoRecoverService systemAutoRecoverService, WindowState win, int checkingSource) {
        super(systemAutoRecoverService, checkingSource, true, win, win.getDisplayId());
        this.mIsOpaque = isOpaque();
    }

    @Override // com.vivo.services.autorecover.InvalidWindowRecord
    public boolean checkingEnabled() {
        if (this.mCheckingSource == 1) {
            return this.mService.isInputCheckEnabled();
        }
        if (this.mCheckingSource == 3) {
            return this.mService.isFocusChangeCheckEnabled();
        }
        return false;
    }

    @Override // com.vivo.services.autorecover.InvalidWindowRecord
    public boolean shouldRecoverImmediately() {
        return this.mService.isPunishEnabled() && this.mWin != null && this.mService.shouldRecover(getReason(), this.mWin.getOwningPackage(), this.mWin.getAttrs().getTitle().toString());
    }

    @Override // com.vivo.services.autorecover.InvalidWindowRecord
    public void recover(boolean background) {
        String recoverReason;
        if (this.mWin != null) {
            if (isActivity() && this.mWin.getActivityRecord() != null) {
                this.mService.finishActivity(this.mWin.getActivityRecord().getActivityToken(), getReason());
                recoverReason = "FINISH_ACTIVITY";
            } else {
                this.mService.forceStopPackage(this.mWin.getOwningPackage(), UserHandle.getUserId(this.mWin.getOwningUid()), getReason());
                recoverReason = "FORCE_STOP";
            }
            String recoveredReason = InvalidWindowRecord.recoverWayToString(background ? 4 : 8, recoverReason);
            this.mExceptionInfo.setRecoveredTime(SystemClock.elapsedRealtime());
            this.mExceptionInfo.setRecoveredReason(recoveredReason);
        }
    }

    @Override // com.vivo.services.autorecover.InvalidWindowRecord
    public boolean isExceptionOccurs() {
        if (this.mWin == null || !this.mWin.isVisibleLw()) {
            VLog.d(SystemAutoRecoverService.TAG, this.mWin + " is not visible");
            return false;
        } else if (isInvalidWindowSize()) {
            this.mReason = 1;
            return true;
        } else {
            return false;
        }
    }

    @Override // com.vivo.services.autorecover.InvalidWindowRecord
    public void createException() {
        if (this.mWin != null) {
            String touble = "1";
            String level = "1";
            if (this.mCheckingSource == 3) {
                touble = "0";
                level = InputExceptionReport.LEVEL_MEDIUM;
            }
            this.mExceptionInfo = new ExceptionInfo();
            appendCommonExceptionInfo(this.mExceptionInfo);
            this.mExceptionInfo.setExceptionOse(this.mScene);
            this.mExceptionInfo.setExceptionLevel(level);
            this.mExceptionInfo.setExceptionTrouble(touble);
            this.mExceptionInfo.setWindowType(windowTypeToString());
            this.mExceptionInfo.setWindowRect(windowRectToString());
        }
    }

    @Override // com.vivo.services.autorecover.InvalidWindowRecord
    public int recoverWay() {
        return 3;
    }

    private boolean isInvalidWindowSize() {
        int i;
        int width = this.mWin.getFrameLw().right - this.mWin.getFrameLw().left;
        int height = this.mWin.getFrameLw().bottom - this.mWin.getFrameLw().top;
        if (width >= 0 && width <= (i = INVALID_WINDOW_SIZE_THRESHOLD) && height >= 0 && height <= i) {
            VLog.d(SystemAutoRecoverService.TAG, "Invalid window size : " + this.mWin + " width = " + width + " , height = " + height);
            return true;
        }
        return false;
    }

    private boolean isInvalidAlpha() {
        float surfaceAlpha = this.mService.getSurfaceAlpha(this.mWin.getInputInfo(), this.mWin.getDisplayId());
        return surfaceAlpha == 0.0f;
    }

    @Override // com.vivo.services.autorecover.InvalidWindowRecord
    public String getExceptionSource() {
        return this.mWin.getAttrs().getTitle().toString();
    }

    @Override // com.vivo.services.autorecover.InvalidWindowRecord
    public String getExceptionPackage() {
        return this.mWin.getOwningPackage();
    }

    @Override // com.vivo.services.autorecover.InvalidWindowRecord
    public int getExceptionPid() {
        return this.mWin.getCurrentSessionPid();
    }

    @Override // com.vivo.services.autorecover.InvalidWindowRecord
    protected void appendDropBoxContent(StringBuilder sb) {
        if (isActivity() && this.mWin.getActivityRecord() != null) {
            String activity = this.mWin.getOwningPackage() + "/" + this.mWin.getActivityRecord().getActivityComponent().getShortClassName();
            appendDumpsysActivity(activity, sb);
        }
    }
}