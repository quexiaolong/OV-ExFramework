package android.sysprop;

import java.util.function.Function;

/* compiled from: lambda */
/* renamed from: android.sysprop.-$$Lambda$SurfaceFlingerProperties$WvTfK7vefne56zQH-S13TZl7XsQ  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$SurfaceFlingerProperties$WvTfK7vefne56zQHS13TZl7XsQ implements Function {
    public static final /* synthetic */ $$Lambda$SurfaceFlingerProperties$WvTfK7vefne56zQHS13TZl7XsQ INSTANCE = new $$Lambda$SurfaceFlingerProperties$WvTfK7vefne56zQHS13TZl7XsQ();

    private /* synthetic */ $$Lambda$SurfaceFlingerProperties$WvTfK7vefne56zQHS13TZl7XsQ() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        Double tryParseDouble;
        tryParseDouble = SurfaceFlingerProperties.tryParseDouble((String) obj);
        return tryParseDouble;
    }
}