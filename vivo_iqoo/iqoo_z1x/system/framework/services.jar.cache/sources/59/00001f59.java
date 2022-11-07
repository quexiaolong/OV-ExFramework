package com.android.server.wm;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.proto.ProtoOutputStream;
import android.view.DisplayInfo;
import android.view.SurfaceControl;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ClipRectAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;
import com.android.server.wm.LocalAnimationAdapter;
import java.io.PrintWriter;

/* loaded from: classes2.dex */
public class WindowChangeAnimationSpec implements LocalAnimationAdapter.AnimationSpec {
    static final int ANIMATION_DURATION = 336;
    private Animation mAnimation;
    private final Rect mEndBounds;
    private final boolean mIsAppAnimation;
    private final boolean mIsThumbnail;
    private final Rect mStartBounds;
    private final ThreadLocal<TmpValues> mThreadLocalTmps = ThreadLocal.withInitial($$Lambda$WindowChangeAnimationSpec$J5jIvng4nctFR8T6L2f_W3o1KU.INSTANCE);
    private final Rect mTmpRect = new Rect();

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ TmpValues lambda$new$0() {
        return new TmpValues();
    }

    public WindowChangeAnimationSpec(Rect startBounds, Rect endBounds, DisplayInfo displayInfo, float durationScale, boolean isAppAnimation, boolean isThumbnail) {
        this.mStartBounds = new Rect(startBounds);
        this.mEndBounds = new Rect(endBounds);
        this.mIsAppAnimation = isAppAnimation;
        this.mIsThumbnail = isThumbnail;
        createBoundsInterpolator((int) (336.0f * durationScale), displayInfo);
    }

    @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
    public boolean getShowWallpaper() {
        return false;
    }

    @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
    public long getDuration() {
        return this.mAnimation.getDuration();
    }

    private void createBoundsInterpolator(long duration, DisplayInfo displayInfo) {
        boolean growing = ((this.mEndBounds.width() - this.mStartBounds.width()) + this.mEndBounds.height()) - this.mStartBounds.height() >= 0;
        long scalePeriod = ((float) duration) * 0.7f;
        float startScaleX = ((this.mStartBounds.width() * 0.7f) / this.mEndBounds.width()) + (1.0f - 0.7f);
        float startScaleY = ((this.mStartBounds.height() * 0.7f) / this.mEndBounds.height()) + (1.0f - 0.7f);
        if (this.mIsThumbnail) {
            AnimationSet animSet = new AnimationSet(true);
            Animation anim = new AlphaAnimation(1.0f, 0.0f);
            anim.setDuration(scalePeriod);
            if (!growing) {
                anim.setStartOffset(duration - scalePeriod);
            }
            animSet.addAnimation(anim);
            float endScaleX = 1.0f / startScaleX;
            float endScaleY = 1.0f / startScaleY;
            Animation anim2 = new ScaleAnimation(endScaleX, endScaleX, endScaleY, endScaleY);
            anim2.setDuration(duration);
            animSet.addAnimation(anim2);
            this.mAnimation = animSet;
            animSet.initialize(this.mStartBounds.width(), this.mStartBounds.height(), this.mEndBounds.width(), this.mEndBounds.height());
            return;
        }
        AnimationSet animSet2 = new AnimationSet(true);
        Animation scaleAnim = new ScaleAnimation(startScaleX, 1.0f, startScaleY, 1.0f);
        scaleAnim.setDuration(scalePeriod);
        if (!growing) {
            scaleAnim.setStartOffset(duration - scalePeriod);
        }
        animSet2.addAnimation(scaleAnim);
        Animation translateAnim = new TranslateAnimation(this.mStartBounds.left, this.mEndBounds.left, this.mStartBounds.top, this.mEndBounds.top);
        translateAnim.setDuration(duration);
        animSet2.addAnimation(translateAnim);
        Rect startClip = new Rect(this.mStartBounds);
        Rect endClip = new Rect(this.mEndBounds);
        startClip.offsetTo(0, 0);
        endClip.offsetTo(0, 0);
        ClipRectAnimation clipRectAnimation = new ClipRectAnimation(startClip, endClip);
        clipRectAnimation.setDuration(duration);
        animSet2.addAnimation(clipRectAnimation);
        this.mAnimation = animSet2;
        animSet2.initialize(this.mStartBounds.width(), this.mStartBounds.height(), displayInfo.appWidth, displayInfo.appHeight);
    }

    @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
    public void apply(SurfaceControl.Transaction t, SurfaceControl leash, long currentPlayTime) {
        TmpValues tmp = this.mThreadLocalTmps.get();
        if (this.mIsThumbnail) {
            this.mAnimation.getTransformation(currentPlayTime, tmp.mTransformation);
            t.setMatrix(leash, tmp.mTransformation.getMatrix(), tmp.mFloats);
            t.setAlpha(leash, tmp.mTransformation.getAlpha());
            return;
        }
        this.mAnimation.getTransformation(currentPlayTime, tmp.mTransformation);
        Matrix matrix = tmp.mTransformation.getMatrix();
        t.setMatrix(leash, matrix, tmp.mFloats);
        float[] fArr = tmp.mVecs;
        tmp.mVecs[2] = 0.0f;
        fArr[1] = 0.0f;
        float[] fArr2 = tmp.mVecs;
        tmp.mVecs[3] = 1.0f;
        fArr2[0] = 1.0f;
        matrix.mapVectors(tmp.mVecs);
        tmp.mVecs[0] = 1.0f / tmp.mVecs[0];
        tmp.mVecs[3] = 1.0f / tmp.mVecs[3];
        Rect clipRect = tmp.mTransformation.getClipRect();
        this.mTmpRect.left = (int) ((clipRect.left * tmp.mVecs[0]) + 0.5f);
        this.mTmpRect.right = (int) ((clipRect.right * tmp.mVecs[0]) + 0.5f);
        this.mTmpRect.top = (int) ((clipRect.top * tmp.mVecs[3]) + 0.5f);
        this.mTmpRect.bottom = (int) ((clipRect.bottom * tmp.mVecs[3]) + 0.5f);
        t.setWindowCrop(leash, this.mTmpRect);
    }

    @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
    public long calculateStatusBarTransitionStartTime() {
        long uptime = SystemClock.uptimeMillis();
        return Math.max(uptime, ((((float) this.mAnimation.getDuration()) * 0.99f) + uptime) - 120);
    }

    @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
    public boolean canSkipFirstFrame() {
        return false;
    }

    @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
    public boolean needsEarlyWakeup() {
        return this.mIsAppAnimation;
    }

    @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.println(this.mAnimation.getDuration());
    }

    @Override // com.android.server.wm.LocalAnimationAdapter.AnimationSpec
    public void dumpDebugInner(ProtoOutputStream proto) {
        long token = proto.start(1146756268033L);
        proto.write(1138166333441L, this.mAnimation.toString());
        proto.end(token);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class TmpValues {
        final float[] mFloats;
        final Transformation mTransformation;
        final float[] mVecs;

        private TmpValues() {
            this.mTransformation = new Transformation();
            this.mFloats = new float[9];
            this.mVecs = new float[4];
        }
    }
}