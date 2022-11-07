package com.android.server.location;

import com.android.server.location.VivoLocConf;
import vivo.app.configuration.ContentValuesList;

/* compiled from: lambda */
/* renamed from: com.android.server.location.-$$Lambda$Y0z0HAc6zIaC1P8ByquHV7RZ5HM  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$Y0z0HAc6zIaC1P8ByquHV7RZ5HM implements VivoLocConf.ContentValuesListChangedListener {
    public static final /* synthetic */ $$Lambda$Y0z0HAc6zIaC1P8ByquHV7RZ5HM INSTANCE = new $$Lambda$Y0z0HAc6zIaC1P8ByquHV7RZ5HM();

    private /* synthetic */ $$Lambda$Y0z0HAc6zIaC1P8ByquHV7RZ5HM() {
    }

    @Override // com.android.server.location.VivoLocConf.ContentValuesListChangedListener
    public final void onConfigChanged(ContentValuesList contentValuesList) {
        VivoCn0WeakManager.parseCn0WeakConfig(contentValuesList);
    }
}