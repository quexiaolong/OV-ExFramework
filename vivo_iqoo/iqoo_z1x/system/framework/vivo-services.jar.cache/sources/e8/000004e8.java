package com.android.server.wm;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Binder;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceSession;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class MultiWindowFreezeWindowFrame {
    private static final String TAG = "MultiWindowFreezeWindowFrame";
    SurfaceControl mBGSurface;
    int mRotation;
    SurfaceControl mSurface;
    SurfaceControl.Transaction mTransaction;
    private boolean DEBUG_FRAME = VivoMultiWindowTransManager.DEBUG;
    Rect mRect = new Rect();
    float[] mTmpFloats = new float[9];
    Matrix mTmpMatrix = new Matrix();

    /* JADX INFO: Access modifiers changed from: package-private */
    public MultiWindowFreezeWindowFrame(DisplayContent dc, SurfaceSession session, int left, int top, int right, int bottom, int layerStack, int rotation, boolean bgsurface, int minlayer, int maxlayer) throws Surface.OutOfResourcesException {
        int i;
        char c;
        this.mBGSurface = null;
        this.mSurface = null;
        this.mTransaction = null;
        int width = right - left;
        int height = bottom - top;
        this.mRect.set(left, top, right, bottom);
        this.mRotation = rotation;
        this.mTransaction = new SurfaceControl.Transaction();
        this.mSurface = dc.makeChildSurface((WindowContainer) null).setName(TAG).setBufferSize(width, height).setFormat(4).setFlags(4).setParent(dc.mSurfaceControl).setMetadata(-1, Binder.getCallingUid()).build();
        if (bgsurface) {
            this.mBGSurface = dc.makeChildSurface((WindowContainer) null).setName("MultiWindowFreezeWindowFrame_Background").setBufferSize(width, height).setFormat(4).setFlags(4).setParent(dc.mSurfaceControl).setMetadata(-1, Binder.getCallingUid()).build();
        }
        if (VivoMultiWindowConfig.DEBUG) {
            VSlog.d(TAG, "create MultiWindowFreezeWindowFrame width = " + width + ", height = " + height + ", mRotation = " + this.mRotation + " mSurface =" + this.mSurface);
        }
        if (this.mSurface == null) {
            i = 1;
            c = 2;
        } else {
            Surface surface = new Surface();
            surface.copyFrom(this.mSurface);
            SurfaceControl.screenshotWithSecureLayersUnsafe(SurfaceControl.getInternalDisplayToken(), surface, new Rect(), width, height, false, 0);
            surface.destroy();
            this.mTransaction.setLayer(this.mSurface, 2000001);
            int i2 = this.mRotation;
            if (i2 != 0) {
                i = 1;
                if (i2 != 1) {
                    c = 2;
                    if (i2 == 2) {
                        this.mTransaction.setPosition(this.mSurface, width, height);
                        this.mTransaction.setMatrix(this.mSurface, -1.0f, 0.0f, 0.0f, -1.0f);
                    } else if (i2 == 3) {
                        this.mTransaction.setPosition(this.mSurface, height, 0.0f);
                        this.mTransaction.setMatrix(this.mSurface, 0.0f, 1.0f, -1.0f, 0.0f);
                    }
                } else {
                    c = 2;
                    this.mTransaction.setPosition(this.mSurface, 0.0f, width);
                    this.mTransaction.setMatrix(this.mSurface, 0.0f, -1.0f, 1.0f, 0.0f);
                }
            } else {
                c = 2;
                i = 1;
                this.mTransaction.setPosition(this.mSurface, 0.0f, 0.0f);
                this.mTransaction.setMatrix(this.mSurface, 1.0f, 0.0f, 0.0f, 1.0f);
            }
        }
        if (this.mBGSurface != null) {
            Surface surface2 = new Surface();
            surface2.copyFrom(this.mBGSurface);
            int i3 = i;
            SurfaceControl.screenshotWithSecureLayersUnsafe(SurfaceControl.getInternalDisplayToken(), surface2, new Rect(), width, height, false, 0);
            surface2.destroy();
            this.mTransaction.setLayer(this.mBGSurface, 10999);
            int i4 = this.mRotation;
            if (i4 == 0) {
                this.mTransaction.setPosition(this.mBGSurface, 0.0f, 0.0f);
                this.mTransaction.setMatrix(this.mBGSurface, 1.0f, 0.0f, 0.0f, 1.0f);
            } else if (i4 == i3) {
                this.mTransaction.setPosition(this.mBGSurface, 0.0f, width);
                this.mTransaction.setMatrix(this.mBGSurface, 0.0f, -1.0f, 1.0f, 0.0f);
            } else if (i4 == 2) {
                this.mTransaction.setPosition(this.mBGSurface, width, height);
                this.mTransaction.setMatrix(this.mBGSurface, -1.0f, 0.0f, 0.0f, -1.0f);
            } else if (i4 == 3) {
                this.mTransaction.setPosition(this.mBGSurface, height, 0.0f);
                this.mTransaction.setMatrix(this.mBGSurface, 0.0f, 1.0f, -1.0f, 0.0f);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void destroyAll(SurfaceControl.Transaction transaction) {
        if (VivoMultiWindowConfig.DEBUG) {
            VSlog.d(TAG, "destroyAll mSurface= " + this.mSurface);
        }
        SurfaceControl surfaceControl = this.mSurface;
        if (surfaceControl != null) {
            this.mTransaction.remove(surfaceControl);
            this.mSurface = null;
        }
        SurfaceControl surfaceControl2 = this.mBGSurface;
        if (surfaceControl2 != null) {
            this.mTransaction.remove(surfaceControl2);
            this.mBGSurface = null;
        }
        this.mTransaction.apply();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void show() {
        SurfaceControl surfaceControl = this.mSurface;
        if (surfaceControl != null) {
            this.mTransaction.show(surfaceControl);
        }
        SurfaceControl surfaceControl2 = this.mBGSurface;
        if (surfaceControl2 != null) {
            this.mTransaction.show(surfaceControl2);
        }
        this.mTransaction.apply();
    }

    public String toString() {
        return this.mSurface.toString();
    }
}