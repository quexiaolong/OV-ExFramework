package com.android.server.wm;

import android.view.Choreographer;

/* compiled from: lambda */
/* renamed from: com.android.server.wm.-$$Lambda$SurfaceAnimationRunner$9Wa9MhcrSX12liOouHtYXEkDU60  reason: invalid class name */
/* loaded from: classes2.dex */
public final /* synthetic */ class $$Lambda$SurfaceAnimationRunner$9Wa9MhcrSX12liOouHtYXEkDU60 implements Choreographer.FrameCallback {
    public final /* synthetic */ SurfaceAnimationRunner f$0;

    public /* synthetic */ $$Lambda$SurfaceAnimationRunner$9Wa9MhcrSX12liOouHtYXEkDU60(SurfaceAnimationRunner surfaceAnimationRunner) {
        this.f$0 = surfaceAnimationRunner;
    }

    @Override // android.view.Choreographer.FrameCallback
    public final void doFrame(long j) {
        SurfaceAnimationRunner.lambda$9Wa9MhcrSX12liOouHtYXEkDU60(this.f$0, j);
    }
}