package com.android.server.accessibility;

import com.android.internal.util.function.TriConsumer;

/* compiled from: lambda */
/* renamed from: com.android.server.accessibility.-$$Lambda$AccessibilityWindowManager$Ky3Q5Gg_NEaXwBlFb7wxyjIUci0  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$AccessibilityWindowManager$Ky3Q5Gg_NEaXwBlFb7wxyjIUci0 implements TriConsumer {
    public static final /* synthetic */ $$Lambda$AccessibilityWindowManager$Ky3Q5Gg_NEaXwBlFb7wxyjIUci0 INSTANCE = new $$Lambda$AccessibilityWindowManager$Ky3Q5Gg_NEaXwBlFb7wxyjIUci0();

    private /* synthetic */ $$Lambda$AccessibilityWindowManager$Ky3Q5Gg_NEaXwBlFb7wxyjIUci0() {
    }

    public final void accept(Object obj, Object obj2, Object obj3) {
        ((AccessibilityWindowManager) obj).clearAccessibilityFocusMainThread(((Integer) obj2).intValue(), ((Integer) obj3).intValue());
    }
}