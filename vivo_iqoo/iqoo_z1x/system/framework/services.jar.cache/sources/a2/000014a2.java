package com.android.server.pm;

import android.content.pm.ShortcutInfo;
import java.util.function.Predicate;

/* compiled from: lambda */
/* renamed from: com.android.server.pm.-$$Lambda$2VdstM0DO8CNjons0WtDfT1btWE  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$2VdstM0DO8CNjons0WtDfT1btWE implements Predicate {
    public static final /* synthetic */ $$Lambda$2VdstM0DO8CNjons0WtDfT1btWE INSTANCE = new $$Lambda$2VdstM0DO8CNjons0WtDfT1btWE();

    private /* synthetic */ $$Lambda$2VdstM0DO8CNjons0WtDfT1btWE() {
    }

    @Override // java.util.function.Predicate
    public final boolean test(Object obj) {
        return ((ShortcutInfo) obj).isNonManifestVisible();
    }
}