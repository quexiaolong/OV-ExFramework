package com.android.server.wm;

import android.graphics.Rect;
import android.util.proto.ProtoOutputStream;
import android.view.DisplayCutout;
import android.view.DisplayInfo;
import com.android.server.IVivoRmsInjector;
import com.android.server.wm.utils.WmDisplayCutout;
import java.io.PrintWriter;

/* loaded from: classes2.dex */
public class DisplayFrames {
    public int mDisplayHeight;
    public final int mDisplayId;
    public int mDisplayWidth;
    public int mRotation;
    public final Rect mUnrestricted = new Rect();
    public final Rect mRestricted = new Rect();
    public final Rect mSystem = new Rect();
    public final Rect mStable = new Rect();
    public final Rect mStableFullscreen = new Rect();
    public final Rect mCurrent = new Rect();
    public final Rect mContent = new Rect();
    public final Rect mVoiceContent = new Rect();
    public final Rect mDock = new Rect();
    public WmDisplayCutout mDisplayCutout = WmDisplayCutout.NO_CUTOUT;
    public WmDisplayCutout mDisplayInfoCutout = WmDisplayCutout.NO_CUTOUT;
    public final Rect mDisplayCutoutSafe = new Rect();

    public DisplayFrames(int displayId, DisplayInfo info, WmDisplayCutout displayCutout) {
        this.mDisplayId = displayId;
        onDisplayInfoUpdated(info, displayCutout);
    }

    public void onDisplayInfoUpdated(DisplayInfo info, WmDisplayCutout displayCutout) {
        this.mDisplayWidth = info.logicalWidth;
        this.mDisplayHeight = info.logicalHeight;
        this.mRotation = info.rotation;
        this.mDisplayInfoCutout = displayCutout != null ? displayCutout : WmDisplayCutout.NO_CUTOUT;
    }

    public void onBeginLayout() {
        this.mUnrestricted.set(0, 0, this.mDisplayWidth, this.mDisplayHeight);
        this.mRestricted.set(this.mUnrestricted);
        this.mSystem.set(this.mUnrestricted);
        this.mDock.set(this.mUnrestricted);
        this.mContent.set(this.mUnrestricted);
        this.mVoiceContent.set(this.mUnrestricted);
        this.mStable.set(this.mUnrestricted);
        this.mStableFullscreen.set(this.mUnrestricted);
        this.mCurrent.set(this.mUnrestricted);
        this.mDisplayCutout = this.mDisplayInfoCutout;
        this.mDisplayCutoutSafe.set(Integer.MIN_VALUE, Integer.MIN_VALUE, IVivoRmsInjector.QUIET_TYPE_ALL, IVivoRmsInjector.QUIET_TYPE_ALL);
        if (!this.mDisplayCutout.getDisplayCutout().isEmpty()) {
            DisplayCutout c = this.mDisplayCutout.getDisplayCutout();
            if (c.getSafeInsetLeft() > 0) {
                this.mDisplayCutoutSafe.left = this.mUnrestricted.left + c.getSafeInsetLeft();
            }
            if (c.getSafeInsetTop() > 0) {
                this.mDisplayCutoutSafe.top = this.mUnrestricted.top + c.getSafeInsetTop();
            }
            if (c.getSafeInsetRight() > 0) {
                this.mDisplayCutoutSafe.right = this.mUnrestricted.right - c.getSafeInsetRight();
            }
            if (c.getSafeInsetBottom() > 0) {
                this.mDisplayCutoutSafe.bottom = this.mUnrestricted.bottom - c.getSafeInsetBottom();
            }
        }
    }

    public int getInputMethodWindowVisibleHeight() {
        return this.mDock.bottom - this.mCurrent.bottom;
    }

    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        this.mStable.dumpDebug(proto, 1146756268033L);
        proto.end(token);
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.println(prefix + "DisplayFrames w=" + this.mDisplayWidth + " h=" + this.mDisplayHeight + " r=" + this.mRotation);
        StringBuilder sb = new StringBuilder();
        sb.append(prefix);
        sb.append("  ");
        String myPrefix = sb.toString();
        dumpFrame(this.mStable, "mStable", myPrefix, pw);
        dumpFrame(this.mStableFullscreen, "mStableFullscreen", myPrefix, pw);
        dumpFrame(this.mDock, "mDock", myPrefix, pw);
        dumpFrame(this.mCurrent, "mCurrent", myPrefix, pw);
        dumpFrame(this.mSystem, "mSystem", myPrefix, pw);
        dumpFrame(this.mContent, "mContent", myPrefix, pw);
        dumpFrame(this.mVoiceContent, "mVoiceContent", myPrefix, pw);
        dumpFrame(this.mRestricted, "mRestricted", myPrefix, pw);
        dumpFrame(this.mUnrestricted, "mUnrestricted", myPrefix, pw);
        pw.println(myPrefix + "mDisplayCutout=" + this.mDisplayCutout);
    }

    private void dumpFrame(Rect frame, String name, String prefix, PrintWriter pw) {
        pw.print(prefix + name + "=");
        frame.printShortString(pw);
        pw.println();
    }
}