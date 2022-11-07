package com.android.server.people.data;

import java.io.File;
import java.io.FileFilter;

/* compiled from: lambda */
/* renamed from: com.android.server.people.data.-$$Lambda$AuvQl7mzpTuCl6KGI2jmWCB7WvI  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$AuvQl7mzpTuCl6KGI2jmWCB7WvI implements FileFilter {
    public static final /* synthetic */ $$Lambda$AuvQl7mzpTuCl6KGI2jmWCB7WvI INSTANCE = new $$Lambda$AuvQl7mzpTuCl6KGI2jmWCB7WvI();

    private /* synthetic */ $$Lambda$AuvQl7mzpTuCl6KGI2jmWCB7WvI() {
    }

    @Override // java.io.FileFilter
    public final boolean accept(File file) {
        return file.isFile();
    }
}