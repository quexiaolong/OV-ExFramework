package com.android.server.am;

import android.content.pm.SharedLibraryInfo;
import java.util.function.Function;

/* compiled from: lambda */
/* renamed from: com.android.server.am.-$$Lambda$jVSWDZTj55yxOQmZSLdNsbUkMb4  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$jVSWDZTj55yxOQmZSLdNsbUkMb4 implements Function {
    public static final /* synthetic */ $$Lambda$jVSWDZTj55yxOQmZSLdNsbUkMb4 INSTANCE = new $$Lambda$jVSWDZTj55yxOQmZSLdNsbUkMb4();

    private /* synthetic */ $$Lambda$jVSWDZTj55yxOQmZSLdNsbUkMb4() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        return ((SharedLibraryInfo) obj).getName();
    }
}