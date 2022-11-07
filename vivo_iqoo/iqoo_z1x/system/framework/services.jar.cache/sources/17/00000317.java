package android.sysprop;

import java.util.function.Function;

/* compiled from: lambda */
/* renamed from: android.sysprop.-$$Lambda$SurfaceFlingerProperties$jxj1aANBXH483fjcw36qrxGbYu8  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$SurfaceFlingerProperties$jxj1aANBXH483fjcw36qrxGbYu8 implements Function {
    public static final /* synthetic */ $$Lambda$SurfaceFlingerProperties$jxj1aANBXH483fjcw36qrxGbYu8 INSTANCE = new $$Lambda$SurfaceFlingerProperties$jxj1aANBXH483fjcw36qrxGbYu8();

    private /* synthetic */ $$Lambda$SurfaceFlingerProperties$jxj1aANBXH483fjcw36qrxGbYu8() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        Double tryParseDouble;
        tryParseDouble = SurfaceFlingerProperties.tryParseDouble((String) obj);
        return tryParseDouble;
    }
}