package com.android.server.wm;

import com.android.internal.util.function.QuadConsumer;

/* compiled from: lambda */
/* renamed from: com.android.server.wm.-$$Lambda$YyoufxRFhCQlzSCunWXg01W0IPM  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$YyoufxRFhCQlzSCunWXg01W0IPM implements QuadConsumer {
    public static final /* synthetic */ $$Lambda$YyoufxRFhCQlzSCunWXg01W0IPM INSTANCE = new $$Lambda$YyoufxRFhCQlzSCunWXg01W0IPM();

    private /* synthetic */ $$Lambda$YyoufxRFhCQlzSCunWXg01W0IPM() {
    }

    public final void accept(Object obj, Object obj2, Object obj3, Object obj4) {
        ((VivoActivityTaskManagerServiceImpl) obj).reparentTaskForSwap((Task) obj2, (ActivityStack) obj3, (Task) obj4);
    }
}