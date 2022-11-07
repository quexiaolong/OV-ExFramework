package com.android.server.wm.utils;

import android.graphics.Rect;
import android.util.Size;
import android.view.DisplayCutout;
import java.util.Objects;

/* loaded from: classes2.dex */
public class WmDisplayCutout {
    public static final WmDisplayCutout NO_CUTOUT = new WmDisplayCutout(DisplayCutout.NO_CUTOUT, null);
    private final Size mFrameSize;
    private final DisplayCutout mInner;

    public WmDisplayCutout(DisplayCutout inner, Size frameSize) {
        this.mInner = inner;
        this.mFrameSize = frameSize;
    }

    public static WmDisplayCutout computeSafeInsets(DisplayCutout inner, int displayWidth, int displayHeight) {
        if (inner == DisplayCutout.NO_CUTOUT) {
            return NO_CUTOUT;
        }
        Size displaySize = new Size(displayWidth, displayHeight);
        Rect safeInsets = computeSafeInsets(displaySize, inner);
        return new WmDisplayCutout(inner.replaceSafeInsets(safeInsets), displaySize);
    }

    public WmDisplayCutout inset(int insetLeft, int insetTop, int insetRight, int insetBottom) {
        DisplayCutout newInner = this.mInner.inset(insetLeft, insetTop, insetRight, insetBottom);
        if (this.mInner == newInner) {
            return this;
        }
        Size frame = this.mFrameSize == null ? null : new Size((this.mFrameSize.getWidth() - insetLeft) - insetRight, (this.mFrameSize.getHeight() - insetTop) - insetBottom);
        return new WmDisplayCutout(newInner, frame);
    }

    public WmDisplayCutout calculateRelativeTo(Rect frame) {
        Size size = this.mFrameSize;
        if (size == null) {
            return this;
        }
        int insetRight = size.getWidth() - frame.right;
        int insetBottom = this.mFrameSize.getHeight() - frame.bottom;
        if (frame.left == 0 && frame.top == 0 && insetRight == 0 && insetBottom == 0) {
            return this;
        }
        if (frame.left >= this.mInner.getSafeInsetLeft() && frame.top >= this.mInner.getSafeInsetTop() && insetRight >= this.mInner.getSafeInsetRight() && insetBottom >= this.mInner.getSafeInsetBottom()) {
            return NO_CUTOUT;
        }
        if (this.mInner.isEmpty()) {
            return this;
        }
        return inset(frame.left, frame.top, insetRight, insetBottom);
    }

    public WmDisplayCutout computeSafeInsets(int width, int height) {
        return computeSafeInsets(this.mInner, width, height);
    }

    private static Rect computeSafeInsets(Size displaySize, DisplayCutout cutout) {
        if (displaySize.getWidth() == displaySize.getHeight()) {
            throw new UnsupportedOperationException("not implemented: display=" + displaySize + " cutout=" + cutout);
        }
        int leftInset = Math.max(cutout.getWaterfallInsets().left, findCutoutInsetForSide(displaySize, cutout.getBoundingRectLeft(), 3));
        int topInset = Math.max(cutout.getWaterfallInsets().top, findCutoutInsetForSide(displaySize, cutout.getBoundingRectTop(), 48));
        int rightInset = Math.max(cutout.getWaterfallInsets().right, findCutoutInsetForSide(displaySize, cutout.getBoundingRectRight(), 5));
        int bottomInset = Math.max(cutout.getWaterfallInsets().bottom, findCutoutInsetForSide(displaySize, cutout.getBoundingRectBottom(), 80));
        return new Rect(leftInset, topInset, rightInset, bottomInset);
    }

    private static int findCutoutInsetForSide(Size display, Rect boundingRect, int gravity) {
        if (boundingRect.isEmpty()) {
            return 0;
        }
        if (gravity != 3) {
            if (gravity != 5) {
                if (gravity != 48) {
                    if (gravity == 80) {
                        return Math.max(0, display.getHeight() - boundingRect.top);
                    }
                    throw new IllegalArgumentException("unknown gravity: " + gravity);
                }
                return Math.max(0, boundingRect.bottom);
            }
            return Math.max(0, display.getWidth() - boundingRect.left);
        }
        return Math.max(0, boundingRect.right);
    }

    public DisplayCutout getDisplayCutout() {
        return this.mInner;
    }

    public boolean equals(Object o) {
        if (o instanceof WmDisplayCutout) {
            WmDisplayCutout that = (WmDisplayCutout) o;
            return Objects.equals(this.mInner, that.mInner) && Objects.equals(this.mFrameSize, that.mFrameSize);
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(this.mInner, this.mFrameSize);
    }

    public String toString() {
        return "WmDisplayCutout{" + this.mInner + ", mFrameSize=" + this.mFrameSize + '}';
    }
}