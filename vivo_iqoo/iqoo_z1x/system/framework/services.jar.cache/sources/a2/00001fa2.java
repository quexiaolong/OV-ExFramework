package com.android.server.wm.animation;

import android.animation.KeyframeSet;
import android.animation.PathKeyframes;
import android.graphics.Path;
import android.graphics.PointF;
import android.view.animation.Animation;
import android.view.animation.Transformation;

/* loaded from: classes2.dex */
public class CurvedTranslateAnimation extends Animation {
    private final PathKeyframes mKeyframes;

    public CurvedTranslateAnimation(Path path) {
        this.mKeyframes = KeyframeSet.ofPath(path);
    }

    @Override // android.view.animation.Animation
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        PointF location = (PointF) this.mKeyframes.getValue(interpolatedTime);
        t.getMatrix().setTranslate(location.x, location.y);
    }
}