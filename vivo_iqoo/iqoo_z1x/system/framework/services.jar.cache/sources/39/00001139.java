package com.android.server.location.gnss;

import android.location.IGnssStatusListener;
import android.os.IBinder;
import java.util.function.Function;

/* compiled from: lambda */
/* renamed from: com.android.server.location.gnss.-$$Lambda$hu439-4T6QBT8QyZnspMtXqICWs  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$hu4394T6QBT8QyZnspMtXqICWs implements Function {
    public static final /* synthetic */ $$Lambda$hu4394T6QBT8QyZnspMtXqICWs INSTANCE = new $$Lambda$hu4394T6QBT8QyZnspMtXqICWs();

    private /* synthetic */ $$Lambda$hu4394T6QBT8QyZnspMtXqICWs() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        return IGnssStatusListener.Stub.asInterface((IBinder) obj);
    }
}