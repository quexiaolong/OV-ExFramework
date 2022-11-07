package com.android.server.people.data;

import java.io.File;
import java.io.FileFilter;

/* compiled from: lambda */
/* renamed from: com.android.server.people.data.-$$Lambda$k1LMnpJLlrYtcSsQvSbPW-daMgg  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$k1LMnpJLlrYtcSsQvSbPWdaMgg implements FileFilter {
    public static final /* synthetic */ $$Lambda$k1LMnpJLlrYtcSsQvSbPWdaMgg INSTANCE = new $$Lambda$k1LMnpJLlrYtcSsQvSbPWdaMgg();

    private /* synthetic */ $$Lambda$k1LMnpJLlrYtcSsQvSbPWdaMgg() {
    }

    @Override // java.io.FileFilter
    public final boolean accept(File file) {
        return file.isDirectory();
    }
}