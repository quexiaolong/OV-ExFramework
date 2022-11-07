package com.android.server.biometrics.fingerprint;

import com.vivo.fingerprint.analysis.AnalysisEvent;
import java.util.Comparator;

/* compiled from: lambda */
/* renamed from: com.android.server.biometrics.fingerprint.-$$Lambda$AnalysisService$QBuQAX-NSfjG4LEMoBYbXH_2JFo  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$AnalysisService$QBuQAXNSfjG4LEMoBYbXH_2JFo implements Comparator {
    public static final /* synthetic */ $$Lambda$AnalysisService$QBuQAXNSfjG4LEMoBYbXH_2JFo INSTANCE = new $$Lambda$AnalysisService$QBuQAXNSfjG4LEMoBYbXH_2JFo();

    private /* synthetic */ $$Lambda$AnalysisService$QBuQAXNSfjG4LEMoBYbXH_2JFo() {
    }

    @Override // java.util.Comparator
    public final int compare(Object obj, Object obj2) {
        return AnalysisService.lambda$dumpSession$0((AnalysisEvent) obj, (AnalysisEvent) obj2);
    }
}