package com.android.server.wm;

import android.view.SurfaceControl;
import android.view.SurfaceSession;
import java.util.function.Function;

/* compiled from: lambda */
/* renamed from: com.android.server.wm.-$$Lambda$1Hjf_Nn5x4aIy9rIBTwVrtrzWFA  reason: invalid class name */
/* loaded from: classes2.dex */
public final /* synthetic */ class $$Lambda$1Hjf_Nn5x4aIy9rIBTwVrtrzWFA implements Function {
    public static final /* synthetic */ $$Lambda$1Hjf_Nn5x4aIy9rIBTwVrtrzWFA INSTANCE = new $$Lambda$1Hjf_Nn5x4aIy9rIBTwVrtrzWFA();

    private /* synthetic */ $$Lambda$1Hjf_Nn5x4aIy9rIBTwVrtrzWFA() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        return new SurfaceControl.Builder((SurfaceSession) obj);
    }
}