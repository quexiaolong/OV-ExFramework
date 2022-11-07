package android.sysprop;

import java.util.function.Function;

/* compiled from: lambda */
/* renamed from: android.sysprop.-$$Lambda$SurfaceFlingerProperties$uEcQnNtGA6nu3Sq3OLtSVMFDQrc  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$SurfaceFlingerProperties$uEcQnNtGA6nu3Sq3OLtSVMFDQrc implements Function {
    public static final /* synthetic */ $$Lambda$SurfaceFlingerProperties$uEcQnNtGA6nu3Sq3OLtSVMFDQrc INSTANCE = new $$Lambda$SurfaceFlingerProperties$uEcQnNtGA6nu3Sq3OLtSVMFDQrc();

    private /* synthetic */ $$Lambda$SurfaceFlingerProperties$uEcQnNtGA6nu3Sq3OLtSVMFDQrc() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        Double tryParseDouble;
        tryParseDouble = SurfaceFlingerProperties.tryParseDouble((String) obj);
        return tryParseDouble;
    }
}