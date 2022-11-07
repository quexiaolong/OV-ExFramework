package com.android.server.wm;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.os.Debug;
import android.os.IBinder;
import android.view.Surface;
import android.view.SurfaceControl;
import com.vivo.face.common.data.Constants;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class DragStateAttachFrame {
    private static String TAG = "DragStateAttachFrame";
    private DisplayContent mDisplayContent;
    private int mIndWidthPx;
    private IBinder mLocalWin;
    private WindowManagerService mService;
    SurfaceControl mSurfaceControl;
    private Surface mIndSurface = new Surface();
    boolean bEnableAttach = true;

    public DragStateAttachFrame(DisplayContent displayContent, WindowManagerService service, IBinder localWin) {
        this.mIndWidthPx = 0;
        this.mDisplayContent = displayContent;
        this.mService = service;
        this.mLocalWin = localWin;
        this.mIndWidthPx = service.mContext.getResources().getDimensionPixelSize(51118543);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getWidthPx() {
        return this.mIndWidthPx;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setupSurfaceAndShow(float x, float y, int layer) {
        int i = this.mIndWidthPx;
        return setupSurfaceAndShow(i, i, x, y, layer);
    }

    boolean setupSurfaceAndShow(int w, int h, float x, float y, int layer) {
        if (this.mSurfaceControl != null) {
            if (WindowManagerDebugConfig.DEBUG_WINDOW_TRACE) {
                VSlog.i(TAG, "not create new in setupSurfaceAndShow");
            }
            return false;
        }
        if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS || WindowManagerDebugConfig.DEBUG_WINDOW_TRACE) {
            String str = TAG;
            VSlog.i(str, ">>> OPEN TRANSACTION DragStateAttachFrame setupSurfaceAndShow w = " + w + " h = " + h + " " + Thread.currentThread().getId() + " " + Thread.currentThread().getName() + " " + Debug.getCallers(5));
        }
        SurfaceControl.openTransaction();
        try {
            if (this.mSurfaceControl == null) {
                this.mSurfaceControl = creatIndDrawSurface(w, h, layer);
            }
            if (this.mSurfaceControl == null) {
                if (WindowManagerDebugConfig.DEBUG_WINDOW_TRACE) {
                    VSlog.i(TAG, "failed to create new in setupSurfaceAndShow");
                }
                return false;
            }
            this.mSurfaceControl.setBufferSize(w, h);
            this.mSurfaceControl.setPosition(x, y);
            this.mSurfaceControl.show();
            SurfaceControl.closeTransaction();
            if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS || WindowManagerDebugConfig.DEBUG_WINDOW_TRACE) {
                VSlog.i(TAG, ">>> CLOSE TRANSACTION DragStateAttachFrame setupSurfaceAndShow");
                return true;
            }
            return true;
        } finally {
            SurfaceControl.closeTransaction();
            if (WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS || WindowManagerDebugConfig.DEBUG_WINDOW_TRACE) {
                VSlog.i(TAG, ">>> CLOSE TRANSACTION DragStateAttachFrame setupSurfaceAndShow");
            }
        }
    }

    void hide() {
        hide(null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void hide(String reason) {
        String str;
        if (this.mSurfaceControl != null) {
            if (WindowManagerDebugConfig.DEBUG_WINDOW_TRACE) {
                String str2 = TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("DragStateAttachFrame hide surface ");
                if (reason != null) {
                    str = "for " + reason;
                } else {
                    str = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                }
                sb.append(str);
                sb.append(Thread.currentThread().getId());
                sb.append(" ");
                sb.append(Thread.currentThread().getName());
                sb.append(" ");
                sb.append(Debug.getCallers(5));
                VSlog.i(str2, sb.toString());
            }
            this.mSurfaceControl.hide();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void show() {
        if (this.mSurfaceControl != null) {
            if (WindowManagerDebugConfig.DEBUG_WINDOW_TRACE) {
                String str = TAG;
                VSlog.i(str, "DragStateAttachFrame show surface " + Thread.currentThread().getId() + " " + Thread.currentThread().getName() + " " + Debug.getCallers(5));
            }
            this.mSurfaceControl.show();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void destroySurface(SurfaceControl.Transaction transaction) {
        if (WindowManagerDebugConfig.DEBUG_WINDOW_TRACE || VivoMultiWindowConfig.DEBUG) {
            String str = TAG;
            VSlog.i(str, "DragStateAttachFrame destroy surface indSurface: " + this.mIndSurface + " mSurfaceControl:" + this.mSurfaceControl + "in Thread " + Thread.currentThread().getId() + " " + Thread.currentThread().getName() + " " + Debug.getCallers(5));
        }
        Surface surface = this.mIndSurface;
        if (surface != null) {
            surface.release();
        }
        SurfaceControl surfaceControl = this.mSurfaceControl;
        if (surfaceControl != null) {
            surfaceControl.hide();
            transaction.remove(this.mSurfaceControl);
            this.mSurfaceControl = null;
        }
    }

    void setPositionInTransaction(float x, float y) {
        SurfaceControl surfaceControl;
        Surface surface = this.mIndSurface;
        if (surface != null && surface.isValid() && (surfaceControl = this.mSurfaceControl) != null) {
            surfaceControl.setPosition(x, y);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setPositionInTransaction(SurfaceControl.Transaction transaction, float x, float y) {
        SurfaceControl surfaceControl;
        Surface surface = this.mIndSurface;
        if (surface != null && surface.isValid() && (surfaceControl = this.mSurfaceControl) != null && transaction != null) {
            transaction.setPosition(surfaceControl, x, y);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isSurfaceValid() {
        Surface surface = this.mIndSurface;
        return (surface == null || !surface.isValid() || this.mSurfaceControl == null) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setPositionByTransaction(SurfaceControl.Transaction transaction, float x, float y) {
        transaction.setPosition(this.mSurfaceControl, x, y);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAlphaByTransaction(SurfaceControl.Transaction transaction, float alpha) {
        transaction.setAlpha(this.mSurfaceControl, alpha);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setMatrixByTransactionNotApply(SurfaceControl.Transaction transaction, float dsdx, float dtdx, float dtdy, float dsdy) {
        transaction.setMatrix(this.mSurfaceControl, dsdx, dtdx, dtdy, dsdy);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void vivoDrawSplitInd() {
        Surface surface = this.mIndSurface;
        if (surface == null || !surface.isValid() || this.mSurfaceControl == null) {
            return;
        }
        Canvas canvas = null;
        try {
            canvas = this.mIndSurface.lockCanvas(null);
        } catch (Surface.OutOfResourcesException e) {
            String str = TAG;
            VSlog.w(str, " vivoDrawSplitInd exception " + e);
        } catch (IllegalArgumentException e2) {
            String str2 = TAG;
            VSlog.w(str2, " vivoDrawSplitInd exception " + e2);
        }
        if (canvas == null) {
            VSlog.w(TAG, "surface draw split Ind canvas is null");
            return;
        }
        Paint p = new Paint();
        canvas.drawColor(0);
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawPaint(p);
        Drawable drawable = getIndDrawable();
        if (drawable != null) {
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        }
        this.mIndSurface.unlockCanvasAndPost(canvas);
    }

    private SurfaceControl creatIndDrawSurface(int w, int h, int layer) {
        if (WindowManagerDebugConfig.DEBUG_WINDOW_TRACE || VivoMultiWindowConfig.DEBUG) {
            String str = TAG;
            VSlog.d(str, "DragStateAttachFrame createFocusedStackFrameSurface in thread " + Thread.currentThread().getId());
        }
        SurfaceControl ctrl = null;
        try {
            ctrl = this.mDisplayContent.makeOverlay().setName("dragInd").setBufferSize(w, h).setFormat(-3).build();
            ctrl.setLayer(layer);
            ctrl.setAlpha(1.0f);
            this.mIndSurface.copyFrom(ctrl);
            return ctrl;
        } catch (Surface.OutOfResourcesException e) {
            return ctrl;
        }
    }

    Drawable getIndDrawable() {
        try {
            Drawable drawable = this.mService.mContext.getResources().getDrawable(50463085);
            return drawable;
        } catch (Resources.NotFoundException e) {
            String str = TAG;
            VSlog.w(str, "DragStateAttachFrame not found dragInd drawable " + e);
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isAttachEnable() {
        return this.bEnableAttach;
    }

    void disableAttach() {
        this.bEnableAttach = false;
        if (VivoMultiWindowConfig.DEBUG) {
            String str = TAG;
            VSlog.i(str, "dragstate disableAttach " + this.bEnableAttach);
        }
    }
}