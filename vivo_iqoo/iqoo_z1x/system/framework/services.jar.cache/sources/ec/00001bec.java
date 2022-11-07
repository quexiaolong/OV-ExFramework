package com.android.server.wm;

import java.util.function.Consumer;

/* compiled from: lambda */
/* renamed from: com.android.server.wm.-$$Lambda$9Kj79s-YFqaGRhFHazfExnbZExw  reason: invalid class name */
/* loaded from: classes2.dex */
public final /* synthetic */ class $$Lambda$9Kj79sYFqaGRhFHazfExnbZExw implements Consumer {
    public static final /* synthetic */ $$Lambda$9Kj79sYFqaGRhFHazfExnbZExw INSTANCE = new $$Lambda$9Kj79sYFqaGRhFHazfExnbZExw();

    private /* synthetic */ $$Lambda$9Kj79sYFqaGRhFHazfExnbZExw() {
    }

    @Override // java.util.function.Consumer
    public final void accept(Object obj) {
        ((WindowProcessListener) obj).clearProfilerIfNeeded();
    }
}