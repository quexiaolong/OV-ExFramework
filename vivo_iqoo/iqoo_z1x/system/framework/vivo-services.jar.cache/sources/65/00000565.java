package com.android.server.wm;

import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Debug;
import android.os.FtBuild;
import android.view.SurfaceControl;
import com.android.server.wm.IVivoLetterbox;
import com.android.server.wm.Letterbox;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoLetterboxImpl implements IVivoLetterbox {
    private static final boolean DEBUG = false;
    static final String TAG = "VivoLetterboxImpl";
    private Letterbox mLetterbox;
    private final Point ZERO_POINT = new Point(0, 0);
    private Rect temp = new Rect();

    public VivoLetterboxImpl(Letterbox letterbox) {
        this.mLetterbox = letterbox;
    }

    public void dummy() {
        VSlog.i(TAG, "dummy, this=" + this);
    }

    public void applySurfaceChanges(SurfaceControl.Transaction t, WindowState win) {
        this.mLetterbox.mTop.applySurfaceChanges(t, win);
        this.mLetterbox.mLeft.applySurfaceChanges(t, win);
        this.mLetterbox.mBottom.applySurfaceChanges(t, win);
        this.mLetterbox.mRight.applySurfaceChanges(t, win);
    }

    public void setColors(SurfaceControl.Transaction t, int left, int top, int right, int bottom) {
        this.mLetterbox.mLeft.setColor(t, left);
        this.mLetterbox.mTop.setColor(t, top);
        this.mLetterbox.mRight.setColor(t, right);
        this.mLetterbox.mBottom.setColor(t, bottom);
    }

    public void resetLetterboxOfLeftRegion() {
        Letterbox letterbox = this.mLetterbox;
        if (letterbox != null && letterbox.mLeft != null && this.mLetterbox.mLeft.mLayoutFrameRelative != null) {
            this.temp.set(this.mLetterbox.mLeft.mLayoutFrameRelative);
            this.mLetterbox.mLeft.layout(this.temp.left, this.temp.top, this.temp.left, this.temp.bottom, this.ZERO_POINT);
        }
    }

    /* loaded from: classes.dex */
    public static class VivoLetterboxSurfaceImpl implements IVivoLetterbox.IVivoLetterboxSurface {
        private Letterbox.LetterboxSurface mLetterboxSurface;
        private int mColor = -16777216;
        WindowState mWin = null;
        boolean relativeLayer = false;

        public VivoLetterboxSurfaceImpl(Letterbox.LetterboxSurface letterboxSurface) {
            this.mLetterboxSurface = letterboxSurface;
        }

        public void dummy() {
            VSlog.i(VivoLetterboxImpl.TAG, "dummy, this=" + this);
        }

        public void applySurfaceChanges(SurfaceControl.Transaction t, WindowState win) {
            if (this.mLetterboxSurface.mSurfaceFrameRelative.equals(this.mLetterboxSurface.mLayoutFrameRelative) && this.mWin == win) {
                return;
            }
            this.mLetterboxSurface.mSurfaceFrameRelative.set(this.mLetterboxSurface.mLayoutFrameRelative);
            if (!this.mLetterboxSurface.mSurfaceFrameRelative.isEmpty()) {
                if (this.mLetterboxSurface.mSurface == null) {
                    this.mLetterboxSurface.createSurface(t);
                }
                if (this.mWin != win) {
                    this.mWin = win;
                    if (win != null) {
                        this.relativeLayer = true;
                        t.setRelativeLayer(this.mLetterboxSurface.mSurface, win.getSurfaceControl(), 0);
                    } else {
                        this.relativeLayer = false;
                        t.setLayer(this.mLetterboxSurface.mSurface, -1);
                    }
                }
                t.setPosition(this.mLetterboxSurface.mSurface, this.mLetterboxSurface.mSurfaceFrameRelative.left, this.mLetterboxSurface.mSurfaceFrameRelative.top);
                t.setWindowCrop(this.mLetterboxSurface.mSurface, this.mLetterboxSurface.mSurfaceFrameRelative.width(), this.mLetterboxSurface.mSurfaceFrameRelative.height());
                t.show(this.mLetterboxSurface.mSurface);
            } else if (this.mLetterboxSurface.mSurface != null) {
                if (this.relativeLayer) {
                    this.mWin = null;
                    this.relativeLayer = false;
                    t.setLayer(this.mLetterboxSurface.mSurface, -1);
                }
                t.hide(this.mLetterboxSurface.mSurface);
            }
            if (this.mLetterboxSurface.mSurface != null && this.mLetterboxSurface.mInputInterceptor != null) {
                this.mLetterboxSurface.mInputInterceptor.updateTouchableRegion(this.mLetterboxSurface.mSurfaceFrameRelative);
                t.setInputWindowInfo(this.mLetterboxSurface.mSurface, this.mLetterboxSurface.mInputInterceptor.mWindowHandle);
            }
        }

        public void setColor(SurfaceControl.Transaction t, int color) {
            if (this.mColor != color) {
                this.mColor = color;
                setColorInner(t, color);
            }
        }

        public void onSurfaceCreated(SurfaceControl.Transaction t) {
            setColorInner(t, this.mColor);
        }

        private void setColorInner(SurfaceControl.Transaction t, int color) {
            if ("vos".equals(FtBuild.getOsName())) {
                return;
            }
            if (this.mLetterboxSurface.mSurface != null) {
                VSlog.i("Letterbox", "setColorInner, color = " + Integer.toHexString(color) + ", callers = " + Debug.getCallers(5));
                t.setColor(this.mLetterboxSurface.mSurface, new float[]{((float) Color.red(this.mColor)) / 255.0f, ((float) Color.green(this.mColor)) / 255.0f, ((float) Color.blue(this.mColor)) / 255.0f});
                return;
            }
            VSlog.i("Letterbox", "null setColorInner, color = " + Integer.toHexString(color) + ", callers = " + Debug.getCallers(5));
        }
    }
}