package com.vivo.services.autorecover;

import android.os.SystemClock;
import android.os.UserHandle;
import com.android.server.policy.InputExceptionReport;
import com.android.server.policy.VivoPolicyUtil;
import com.android.server.wm.WindowState;
import com.vivo.common.utils.VLog;

/* loaded from: classes.dex */
public class InvalidColorSpaceWindowRecord extends InvalidWindowRecord {
    public InvalidColorSpaceWindowRecord(SystemAutoRecoverService systemAutoRecoverService, WindowState win, int checkingSource, boolean isOpaque) {
        super(systemAutoRecoverService, checkingSource, isOpaque, win, win.getDisplayId());
    }

    @Override // com.vivo.services.autorecover.InvalidWindowRecord
    public boolean checkingEnabled() {
        if (this.mCheckingSource == 1) {
            if (!this.mIsOpaque || !this.mService.isGameMode()) {
                return (!this.mService.isInputCheckEnabled() || this.mWin == null || this.mService.inTransparentWhiteList(this.mWin)) ? false : true;
            }
            if (VivoPolicyUtil.IS_LOG_OPEN) {
                VLog.d(SystemAutoRecoverService.TAG, "Ignore checking black screen when in game!");
            }
            return false;
        }
        return false;
    }

    @Override // com.vivo.services.autorecover.InvalidWindowRecord
    public boolean isExceptionOccurs() {
        if (isInvalidWindowColorSpace()) {
            if (this.mIsOpaque) {
                this.mReason = 6;
                return true;
            } else if (isActivity()) {
                this.mReason = 3;
                return true;
            } else {
                this.mReason = 2;
                return true;
            }
        }
        return false;
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
            this.mExceptionInfo.setIncallState(getIncallStatus());
        }
    }

    @Override // com.vivo.services.autorecover.InvalidWindowRecord
    public boolean shouldRecoverImmediately() {
        if (this.mService.isPunishEnabled() && this.mWin != null && this.mService.shouldRecover(getReason(), this.mWin.getOwningPackage(), this.mWin.getAttrs().getTitle().toString())) {
            return true;
        }
        return false;
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
    public int recoverWay() {
        if (this.mWin != null && this.mService.isFocused(this.mWin)) {
            return 3;
        }
        return 2;
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
            return;
        }
        this.mWin.appendWindowViewHierarchy(sb);
    }
}