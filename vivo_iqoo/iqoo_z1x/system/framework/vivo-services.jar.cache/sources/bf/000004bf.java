package com.android.server.wm;

import java.util.function.Predicate;

/* compiled from: lambda */
/* renamed from: com.android.server.wm.-$$Lambda$VivoActivityTaskManagerServiceImpl$CBBodT5_5EKL3dXot536U9Mtkrs  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$VivoActivityTaskManagerServiceImpl$CBBodT5_5EKL3dXot536U9Mtkrs implements Predicate {
    public static final /* synthetic */ $$Lambda$VivoActivityTaskManagerServiceImpl$CBBodT5_5EKL3dXot536U9Mtkrs INSTANCE = new $$Lambda$VivoActivityTaskManagerServiceImpl$CBBodT5_5EKL3dXot536U9Mtkrs();

    private /* synthetic */ $$Lambda$VivoActivityTaskManagerServiceImpl$CBBodT5_5EKL3dXot536U9Mtkrs() {
    }

    @Override // java.util.function.Predicate
    public final boolean test(Object obj) {
        boolean isMultiWindowAppListActivity;
        isMultiWindowAppListActivity = ((ActivityRecord) obj).isMultiWindowAppListActivity();
        return isMultiWindowAppListActivity;
    }
}