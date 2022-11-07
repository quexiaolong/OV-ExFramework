package com.android.server.blob;

import android.provider.DeviceConfig;
import com.android.server.blob.BlobStoreConfig;

/* compiled from: lambda */
/* renamed from: com.android.server.blob.-$$Lambda$BlobStoreConfig$puwdUOAux6q8DMSxBpGh5jGtgZA  reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$BlobStoreConfig$puwdUOAux6q8DMSxBpGh5jGtgZA implements DeviceConfig.OnPropertiesChangedListener {
    public static final /* synthetic */ $$Lambda$BlobStoreConfig$puwdUOAux6q8DMSxBpGh5jGtgZA INSTANCE = new $$Lambda$BlobStoreConfig$puwdUOAux6q8DMSxBpGh5jGtgZA();

    private /* synthetic */ $$Lambda$BlobStoreConfig$puwdUOAux6q8DMSxBpGh5jGtgZA() {
    }

    public final void onPropertiesChanged(DeviceConfig.Properties properties) {
        BlobStoreConfig.DeviceConfigProperties.refresh(properties);
    }
}