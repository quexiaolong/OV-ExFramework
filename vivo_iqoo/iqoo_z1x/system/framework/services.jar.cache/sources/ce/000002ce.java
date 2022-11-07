package android.net.shared;

import java.util.function.Function;

/* compiled from: lambda */
/* renamed from: android.net.shared.-$$Lambda$SYWvjOUPlAZ_O2Z6yfFU9np1858  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$SYWvjOUPlAZ_O2Z6yfFU9np1858 implements Function {
    public static final /* synthetic */ $$Lambda$SYWvjOUPlAZ_O2Z6yfFU9np1858 INSTANCE = new $$Lambda$SYWvjOUPlAZ_O2Z6yfFU9np1858();

    private /* synthetic */ $$Lambda$SYWvjOUPlAZ_O2Z6yfFU9np1858() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        return IpConfigurationParcelableUtil.unparcelAddress((String) obj);
    }
}