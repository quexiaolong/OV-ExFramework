package com.android.server.wm;

/* compiled from: lambda */
/* renamed from: com.android.server.wm.-$$Lambda$SurfaceAnimationThread$frZMbXAzhUBmX-wz0SwbLTXpw9k  reason: invalid class name */
/* loaded from: classes2.dex */
public final /* synthetic */ class $$Lambda$SurfaceAnimationThread$frZMbXAzhUBmXwz0SwbLTXpw9k implements Runnable {
    public static final /* synthetic */ $$Lambda$SurfaceAnimationThread$frZMbXAzhUBmXwz0SwbLTXpw9k INSTANCE = new $$Lambda$SurfaceAnimationThread$frZMbXAzhUBmXwz0SwbLTXpw9k();

    private /* synthetic */ $$Lambda$SurfaceAnimationThread$frZMbXAzhUBmXwz0SwbLTXpw9k() {
    }

    @Override // java.lang.Runnable
    public final void run() {
        SurfaceAnimationThread.sInstance.quit();
    }
}