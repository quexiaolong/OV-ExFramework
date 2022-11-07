package com.android.server;

/* compiled from: lambda */
/* renamed from: com.android.server.-$$Lambda$DisplayThread$f5MRs-rGyBEbIMjOX5lqvMSkf2g  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$DisplayThread$f5MRsrGyBEbIMjOX5lqvMSkf2g implements Runnable {
    public static final /* synthetic */ $$Lambda$DisplayThread$f5MRsrGyBEbIMjOX5lqvMSkf2g INSTANCE = new $$Lambda$DisplayThread$f5MRsrGyBEbIMjOX5lqvMSkf2g();

    private /* synthetic */ $$Lambda$DisplayThread$f5MRsrGyBEbIMjOX5lqvMSkf2g() {
    }

    @Override // java.lang.Runnable
    public final void run() {
        DisplayThread.sInstance.quit();
    }
}