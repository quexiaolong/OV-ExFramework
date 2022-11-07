package com.vivo.services.rms.sdk.args;

import android.os.Bundle;
import android.os.Parcel;

/* loaded from: classes.dex */
public class BundleArgs extends Args {
    public Bundle mBundle;

    @Override // com.vivo.services.rms.sdk.args.Args
    public void writeToParcel(Parcel dest) {
        this.mBundle.writeToParcel(dest, 0);
    }

    @Override // com.vivo.services.rms.sdk.args.Args
    public void readFromParcel(Parcel data) {
        Bundle bundle = new Bundle();
        this.mBundle = bundle;
        bundle.readFromParcel(data);
    }

    @Override // com.vivo.services.rms.sdk.args.Args
    public void recycle() {
        this.mBundle = null;
    }
}