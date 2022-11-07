package com.android.server.autofill;

import android.content.res.Resources;
import com.android.server.autofill.FieldClassificationStrategy;

/* compiled from: lambda */
/* renamed from: com.android.server.autofill.-$$Lambda$FieldClassificationStrategy$vGIL1YGX_9ksoSV74T7gO4fkEBE  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$FieldClassificationStrategy$vGIL1YGX_9ksoSV74T7gO4fkEBE implements FieldClassificationStrategy.MetadataParser {
    public static final /* synthetic */ $$Lambda$FieldClassificationStrategy$vGIL1YGX_9ksoSV74T7gO4fkEBE INSTANCE = new $$Lambda$FieldClassificationStrategy$vGIL1YGX_9ksoSV74T7gO4fkEBE();

    private /* synthetic */ $$Lambda$FieldClassificationStrategy$vGIL1YGX_9ksoSV74T7gO4fkEBE() {
    }

    @Override // com.android.server.autofill.FieldClassificationStrategy.MetadataParser
    public final Object get(Resources resources, int i) {
        String string;
        string = resources.getString(i);
        return string;
    }
}