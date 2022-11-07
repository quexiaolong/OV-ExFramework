package com.vivo.services.rms.sdk.args;

import android.os.Parcel;

/* loaded from: classes.dex */
public class Int2Args extends Args {
    public int mInt0;
    public int mInt1;

    @Override // com.vivo.services.rms.sdk.args.Args
    public void writeToParcel(Parcel dest) {
        dest.writeInt(this.mInt0);
        dest.writeInt(this.mInt1);
    }

    @Override // com.vivo.services.rms.sdk.args.Args
    public void readFromParcel(Parcel data) {
        this.mInt0 = data.readInt();
        this.mInt1 = data.readInt();
    }

    @Override // com.vivo.services.rms.sdk.args.Args
    public void recycle() {
    }
}