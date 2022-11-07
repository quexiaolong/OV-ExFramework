package com.android.server.wm;

import android.graphics.Path;
import android.graphics.PointF;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;

/* loaded from: classes.dex */
public class VivoDimmerImpl implements IVivoDimmer {
    private final Path mBezierPath = VivoBezierUtil.buildPath(new PointF(0.33f, 0.0f), new PointF(0.67f, 1.0f));
    private final Interpolator mVivoDimlayerInterpolator = new PathInterpolator(this.mBezierPath);

    public void dummy() {
    }

    public Interpolator getVivoDimlayerInterpolator() {
        return this.mVivoDimlayerInterpolator;
    }
}