package com.vivo.services.rms.sdk.args;

import android.os.Parcel;

/* loaded from: classes.dex */
public class IntStringArgs extends Args {
    public int mInt0;
    public String mString0;

    @Override // com.vivo.services.rms.sdk.args.Args
    public void writeToParcel(Parcel dest) {
        dest.writeInt(this.mInt0);
        dest.writeString(this.mString0);
    }

    @Override // com.vivo.services.rms.sdk.args.Args
    public void readFromParcel(Parcel data) {
        this.mInt0 = data.readInt();
        this.mString0 = data.readString();
    }

    @Override // com.vivo.services.rms.sdk.args.Args
    public void recycle() {
        this.mString0 = null;
    }
}