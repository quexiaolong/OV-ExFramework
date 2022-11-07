package com.android.server.wm;

import android.content.pm.ActivityInfo;
import android.multidisplay.MultiDisplayManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoDisplayRotationImpl implements IVivoDisplayRotation {
    static final String TAG = "VivoDisplayRotationImpl";
    private DisplayRotation mDisplayRotation;
    private boolean mRotationChanging = false;
    private WindowManagerService mService;

    public VivoDisplayRotationImpl(DisplayRotation displayRotation, WindowManagerService service) {
        if (displayRotation == null) {
            VSlog.i(TAG, "container is " + displayRotation);
        }
        this.mDisplayRotation = displayRotation;
        this.mService = service;
    }

    public void dummy() {
        VSlog.i(TAG, "dummy, this=" + this);
    }

    public boolean directReturnLastRotationInFreeform(int lastRotation) {
        WindowManagerService windowManagerService = this.mService;
        if (windowManagerService != null && windowManagerService.isVivoFreeFormValid() && this.mService.isInVivoFreeform() && !this.mService.isVivoFreeFormStackMax() && this.mDisplayRotation.isAnyPortrait(lastRotation)) {
            return true;
        }
        return false;
    }

    public int getSensorOrientation() {
        if (this.mDisplayRotation.mOrientationListener == null) {
            return -1;
        }
        return this.mDisplayRotation.mOrientationListener.getProposedRotation();
    }

    public boolean isRotationChanging() {
        return this.mRotationChanging;
    }

    public void setRotationChanging(boolean rotationChanging) {
        this.mRotationChanging = rotationChanging;
        if (rotationChanging) {
            this.mService.mAtmService.setCurrentRotationHasResized(false);
        }
    }

    public boolean needSensorRunningForSuggestion() {
        if (this.mService.isVivoMultiWindowSupport() && this.mService.isInVivoMultiWindowIgnoreVisibilityFocusedDisplay() && VivoMultiWindowConfig.IS_VIVO_ROTATE_SUGGESTION) {
            return true;
        }
        return false;
    }

    public void sendProposedRotationChangeToMultiWindowInternal(int rotation, int currentRotation, boolean isValid) {
        if (VivoMultiWindowConfig.IS_VIVO_ROTATE_SUGGESTION) {
            this.mService.sendProposedRotationChangeToDockedDivider(rotation, isValid);
            if (VivoMultiWindowConfig.DEBUG) {
                VSlog.d(TAG, "sendProposedRotationChangeToDockedDivider, mRotation=" + rotation + " mCurrentAppOrientation=" + currentRotation + " isValid=" + isValid);
            }
        }
    }

    public boolean deferUpdateRotationForSplit(int userRotationMode, int lastOrientation) {
        WindowState focus = this.mDisplayRotation.mDisplayContent.mCurrentFocus;
        if (VivoMultiWindowConfig.IS_VIVO_ROTATE_SUGGESTION && focus != null && focus.getTask() != null && userRotationMode == 1 && this.mService.getDefaultDisplayContentLocked().isVivoMultiWindowExitedJustWithDisplay()) {
            int currentFocusOrientation = focus.getTask().getOrientation();
            if (ActivityInfo.isFixedOrientationLandscape(currentFocusOrientation) && currentFocusOrientation != lastOrientation) {
                VSlog.d(TAG, "Deferring rotation, CurrentFocus =" + focus + " LandScape.");
                return true;
            }
            return false;
        }
        return false;
    }

    public int rotationForCast() {
        if (MultiDisplayManager.SUPPORT_CAR_NETWORKING) {
            DisplayContent displayContent = this.mDisplayRotation.mDisplayContent;
            if (displayContent.mDisplayId != 90000 || displayContent.getCastOrientation() == -2) {
                return -100;
            }
            int castOrientation = displayContent.getCastOrientation();
            if (castOrientation != 0) {
                if (castOrientation == 1) {
                    return this.mDisplayRotation.mPortraitRotation;
                }
                return 0;
            }
            return this.mDisplayRotation.mLandscapeRotation;
        }
        return -100;
    }
}