package com.android.server.wm;

import android.app.ActivityManagerInternal;
import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import com.android.internal.util.function.HeptConsumer;

/* compiled from: lambda */
/* renamed from: com.android.server.wm.-$$Lambda$qMFJUmfG50ZSjk7Tac67xBia0d4  reason: invalid class name */
/* loaded from: classes2.dex */
public final /* synthetic */ class $$Lambda$qMFJUmfG50ZSjk7Tac67xBia0d4 implements HeptConsumer {
    public static final /* synthetic */ $$Lambda$qMFJUmfG50ZSjk7Tac67xBia0d4 INSTANCE = new $$Lambda$qMFJUmfG50ZSjk7Tac67xBia0d4();

    private /* synthetic */ $$Lambda$qMFJUmfG50ZSjk7Tac67xBia0d4() {
    }

    public final void accept(Object obj, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6, Object obj7) {
        ((ActivityManagerInternal) obj).startProcess((String) obj2, (ApplicationInfo) obj3, ((Boolean) obj4).booleanValue(), ((Boolean) obj5).booleanValue(), (String) obj6, (ComponentName) obj7);
    }
}