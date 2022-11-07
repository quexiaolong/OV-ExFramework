package com.android.server.wm;

import android.graphics.Rect;
import android.hardware.biometrics.fingerprint.FingerprintUIManagerInternal;
import android.view.SurfaceControl;
import com.android.server.LocalServices;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoWindowSurfaceControllerImpl implements IVivoWindowSurfaceController {
    static final String TAG = "VivoWindowSurfaceControllerImpl";
    private FingerprintUIManagerInternal mFingerprintUIManagerInternal;
    private Rect mTmpRect = new Rect();

    public void initFingerprintUIManager(int ownerUid, String title) {
        if (this.mFingerprintUIManagerInternal == null) {
            this.mFingerprintUIManagerInternal = (FingerprintUIManagerInternal) LocalServices.getService(FingerprintUIManagerInternal.class);
        }
    }

    public void notifyFingerprintWindowStatus(SurfaceControl surfaceControl, WindowStateAnimator animator, int windowType, String title, boolean surfaceShown) {
        if (this.mFingerprintUIManagerInternal != null) {
            try {
                animator.mWin.getVisibleBounds(this.mTmpRect);
                this.mFingerprintUIManagerInternal.setWindowStatus(Integer.toString(System.identityHashCode(surfaceControl)), animator.mWin.getOwningPackage(), title, windowType, animator.mWin.mBaseLayer, this.mTmpRect, surfaceShown, animator.mWin.mAttrs.format, animator.mWin.mAttrs.flags, animator.mWin.canAddInternalSystemWindow());
            } catch (RuntimeException e) {
                VSlog.w(TAG, "Failure setWindowStatus " + surfaceControl + " in " + this, e);
            }
        }
    }
}