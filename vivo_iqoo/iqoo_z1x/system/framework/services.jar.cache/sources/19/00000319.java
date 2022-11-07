package android.sysprop;

import java.util.function.Function;

/* compiled from: lambda */
/* renamed from: android.sysprop.-$$Lambda$SurfaceFlingerProperties$ujhN5-VXpsRSABl9ZdmqOp-pPQ4  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$SurfaceFlingerProperties$ujhN5VXpsRSABl9ZdmqOppPQ4 implements Function {
    public static final /* synthetic */ $$Lambda$SurfaceFlingerProperties$ujhN5VXpsRSABl9ZdmqOppPQ4 INSTANCE = new $$Lambda$SurfaceFlingerProperties$ujhN5VXpsRSABl9ZdmqOppPQ4();

    private /* synthetic */ $$Lambda$SurfaceFlingerProperties$ujhN5VXpsRSABl9ZdmqOppPQ4() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        Double tryParseDouble;
        tryParseDouble = SurfaceFlingerProperties.tryParseDouble((String) obj);
        return tryParseDouble;
    }
}