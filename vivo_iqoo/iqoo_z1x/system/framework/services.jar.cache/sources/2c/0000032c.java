package com.android.server;

/* compiled from: lambda */
/* renamed from: com.android.server.-$$Lambda$AnimationThread$mMqvPqhsYaiy1Cu67m4VbAgabsM  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$AnimationThread$mMqvPqhsYaiy1Cu67m4VbAgabsM implements Runnable {
    public static final /* synthetic */ $$Lambda$AnimationThread$mMqvPqhsYaiy1Cu67m4VbAgabsM INSTANCE = new $$Lambda$AnimationThread$mMqvPqhsYaiy1Cu67m4VbAgabsM();

    private /* synthetic */ $$Lambda$AnimationThread$mMqvPqhsYaiy1Cu67m4VbAgabsM() {
    }

    @Override // java.lang.Runnable
    public final void run() {
        AnimationThread.sInstance.quit();
    }
}