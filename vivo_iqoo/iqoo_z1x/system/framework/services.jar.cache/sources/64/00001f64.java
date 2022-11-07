package com.android.server.wm;

import android.graphics.Rect;
import android.util.proto.ProtoOutputStream;
import com.android.server.wm.utils.InsetUtils;
import com.android.server.wm.utils.WmDisplayCutout;
import java.io.PrintWriter;

/* loaded from: classes2.dex */
public class WindowFrames {
    private static final StringBuilder sTmpSB = new StringBuilder();
    private boolean mContentChanged;
    private boolean mContentInsetsChanged;
    private boolean mDisplayCutoutChanged;
    private boolean mParentFrameWasClippedByDisplayCutout;
    private boolean mStableInsetsChanged;
    private boolean mVisibleInsetsChanged;
    public final Rect mParentFrame = new Rect();
    public final Rect mDisplayFrame = new Rect();
    public final Rect mVisibleFrame = new Rect();
    public final Rect mDecorFrame = new Rect();
    public final Rect mContentFrame = new Rect();
    public final Rect mStableFrame = new Rect();
    final Rect mContainingFrame = new Rect();
    final Rect mFrame = new Rect();
    final Rect mLastFrame = new Rect();
    final Rect mRelFrame = new Rect();
    final Rect mLastRelFrame = new Rect();
    private boolean mFrameSizeChanged = false;
    final Rect mCompatFrame = new Rect();
    WmDisplayCutout mDisplayCutout = WmDisplayCutout.NO_CUTOUT;
    private WmDisplayCutout mLastDisplayCutout = WmDisplayCutout.NO_CUTOUT;
    final Rect mStableInsets = new Rect();
    final Rect mLastStableInsets = new Rect();
    final Rect mVisibleInsets = new Rect();
    final Rect mLastVisibleInsets = new Rect();
    final Rect mContentInsets = new Rect();
    final Rect mLastContentInsets = new Rect();
    private final Rect mTmpRect = new Rect();

    public WindowFrames() {
    }

    public WindowFrames(Rect parentFrame, Rect displayFrame, Rect contentFrame, Rect visibleFrame, Rect decorFrame, Rect stableFrame) {
        setFrames(parentFrame, displayFrame, contentFrame, visibleFrame, decorFrame, stableFrame);
    }

    public void setFrames(Rect parentFrame, Rect displayFrame, Rect contentFrame, Rect visibleFrame, Rect decorFrame, Rect stableFrame) {
        this.mParentFrame.set(parentFrame);
        this.mDisplayFrame.set(displayFrame);
        this.mContentFrame.set(contentFrame);
        this.mVisibleFrame.set(visibleFrame);
        this.mDecorFrame.set(decorFrame);
        this.mStableFrame.set(stableFrame);
    }

    public void setParentFrameWasClippedByDisplayCutout(boolean parentFrameWasClippedByDisplayCutout) {
        this.mParentFrameWasClippedByDisplayCutout = parentFrameWasClippedByDisplayCutout;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean parentFrameWasClippedByDisplayCutout() {
        return this.mParentFrameWasClippedByDisplayCutout;
    }

    public void setDisplayCutout(WmDisplayCutout displayCutout) {
        this.mDisplayCutout = displayCutout;
    }

    private boolean didFrameSizeChange() {
        return (this.mLastFrame.width() == this.mFrame.width() && this.mLastFrame.height() == this.mFrame.height()) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void calculateDockedDividerInsets(Rect cutoutInsets) {
        this.mTmpRect.set(this.mDisplayFrame);
        this.mTmpRect.inset(cutoutInsets);
        this.mTmpRect.intersectUnchecked(this.mStableFrame);
        InsetUtils.insetsBetweenFrames(this.mDisplayFrame, this.mTmpRect, this.mStableInsets);
        this.mContentInsets.setEmpty();
        this.mVisibleInsets.setEmpty();
        this.mDisplayCutout = WmDisplayCutout.NO_CUTOUT;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void calculateInsets(boolean windowsAreFloating, boolean inFullscreenContainer, Rect windowBounds) {
        boolean overrideBottomInset = true;
        boolean overrideRightInset = (windowsAreFloating || inFullscreenContainer || this.mFrame.right <= windowBounds.right) ? false : true;
        if (windowsAreFloating || inFullscreenContainer || this.mFrame.bottom <= windowBounds.bottom) {
            overrideBottomInset = false;
        }
        this.mTmpRect.set(this.mFrame.left, this.mFrame.top, overrideRightInset ? windowBounds.right : this.mFrame.right, overrideBottomInset ? windowBounds.bottom : this.mFrame.bottom);
        InsetUtils.insetsBetweenFrames(this.mTmpRect, this.mContentFrame, this.mContentInsets);
        InsetUtils.insetsBetweenFrames(this.mTmpRect, this.mVisibleFrame, this.mVisibleInsets);
        InsetUtils.insetsBetweenFrames(this.mTmpRect, this.mStableFrame, this.mStableInsets);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scaleInsets(float scale) {
        this.mContentInsets.scale(scale);
        this.mVisibleInsets.scale(scale);
        this.mStableInsets.scale(scale);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void offsetFrames(int layoutXDiff, int layoutYDiff) {
        this.mFrame.offset(layoutXDiff, layoutYDiff);
        this.mContentFrame.offset(layoutXDiff, layoutYDiff);
        this.mVisibleFrame.offset(layoutXDiff, layoutYDiff);
        this.mStableFrame.offset(layoutXDiff, layoutYDiff);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setReportResizeHints() {
        this.mContentInsetsChanged |= !this.mLastContentInsets.equals(this.mContentInsets);
        this.mVisibleInsetsChanged |= !this.mLastVisibleInsets.equals(this.mVisibleInsets);
        this.mStableInsetsChanged |= !this.mLastStableInsets.equals(this.mStableInsets);
        this.mFrameSizeChanged |= didFrameSizeChange();
        boolean z = this.mDisplayCutoutChanged | (!this.mLastDisplayCutout.equals(this.mDisplayCutout));
        this.mDisplayCutoutChanged = z;
        return this.mContentInsetsChanged || this.mVisibleInsetsChanged || this.mStableInsetsChanged || this.mFrameSizeChanged || z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetInsetsChanged() {
        this.mContentInsetsChanged = false;
        this.mVisibleInsetsChanged = false;
        this.mStableInsetsChanged = false;
        this.mFrameSizeChanged = false;
        this.mDisplayCutoutChanged = false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateLastInsetValues() {
        this.mLastContentInsets.set(this.mContentInsets);
        this.mLastVisibleInsets.set(this.mVisibleInsets);
        this.mLastStableInsets.set(this.mStableInsets);
        this.mLastDisplayCutout = this.mDisplayCutout;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resetLastContentInsets() {
        this.mLastContentInsets.set(-1, -1, -1, -1);
    }

    public void setContentChanged(boolean contentChanged) {
        this.mContentChanged = contentChanged;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasContentChanged() {
        return this.mContentChanged;
    }

    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        this.mParentFrame.dumpDebug(proto, 1146756268040L);
        this.mContentFrame.dumpDebug(proto, 1146756268034L);
        this.mDisplayFrame.dumpDebug(proto, 1146756268036L);
        this.mVisibleFrame.dumpDebug(proto, 1146756268041L);
        this.mDecorFrame.dumpDebug(proto, 1146756268035L);
        this.mContainingFrame.dumpDebug(proto, 1146756268033L);
        this.mFrame.dumpDebug(proto, 1146756268037L);
        this.mDisplayCutout.getDisplayCutout().dumpDebug(proto, 1146756268042L);
        this.mContentInsets.dumpDebug(proto, 1146756268043L);
        this.mVisibleInsets.dumpDebug(proto, 1146756268045L);
        this.mStableInsets.dumpDebug(proto, 1146756268046L);
        proto.end(token);
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.println(prefix + "Frames: containing=" + this.mContainingFrame.toShortString(sTmpSB) + " parent=" + this.mParentFrame.toShortString(sTmpSB));
        StringBuilder sb = new StringBuilder();
        sb.append(prefix);
        sb.append("    display=");
        sb.append(this.mDisplayFrame.toShortString(sTmpSB));
        pw.println(sb.toString());
        pw.println(prefix + "    content=" + this.mContentFrame.toShortString(sTmpSB) + " visible=" + this.mVisibleFrame.toShortString(sTmpSB));
        StringBuilder sb2 = new StringBuilder();
        sb2.append(prefix);
        sb2.append("    decor=");
        sb2.append(this.mDecorFrame.toShortString(sTmpSB));
        pw.println(sb2.toString());
        pw.println(prefix + "mFrame=" + this.mFrame.toShortString(sTmpSB) + " last=" + this.mLastFrame.toShortString(sTmpSB));
        pw.println(prefix + " cutout=" + this.mDisplayCutout.getDisplayCutout() + " last=" + this.mLastDisplayCutout.getDisplayCutout());
        pw.print(prefix + "Cur insets: content=" + this.mContentInsets.toShortString(sTmpSB) + " visible=" + this.mVisibleInsets.toShortString(sTmpSB) + " stable=" + this.mStableInsets.toShortString(sTmpSB));
        pw.println(prefix + "Lst insets: content=" + this.mLastContentInsets.toShortString(sTmpSB) + " visible=" + this.mLastVisibleInsets.toShortString(sTmpSB) + " stable=" + this.mLastStableInsets.toShortString(sTmpSB));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getInsetsInfo() {
        return "ci=" + this.mContentInsets.toShortString() + " vi=" + this.mVisibleInsets.toShortString() + " si=" + this.mStableInsets.toShortString();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getInsetsChangedInfo() {
        return "contentInsetsChanged=" + this.mContentInsetsChanged + " " + this.mContentInsets.toShortString() + " visibleInsetsChanged=" + this.mVisibleInsetsChanged + " " + this.mVisibleInsets.toShortString() + " stableInsetsChanged=" + this.mStableInsetsChanged + " " + this.mStableInsets.toShortString() + " displayCutoutChanged=" + this.mDisplayCutoutChanged;
    }
}