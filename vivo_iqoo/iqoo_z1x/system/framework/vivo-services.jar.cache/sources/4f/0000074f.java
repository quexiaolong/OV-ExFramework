package com.vivo.services.rms.sdk.args;

import android.os.Parcel;
import java.util.ArrayList;

/* loaded from: classes.dex */
public class LongStringListArgs extends Args {
    public long mLong0;
    public ArrayList<String> mStringList0;

    @Override // com.vivo.services.rms.sdk.args.Args
    public void writeToParcel(Parcel dest) {
        dest.writeLong(this.mLong0);
        dest.writeStringList(this.mStringList0);
    }

    @Override // com.vivo.services.rms.sdk.args.Args
    public void readFromParcel(Parcel data) {
        this.mLong0 = data.readLong();
        this.mStringList0 = data.createStringArrayList();
    }

    @Override // com.vivo.services.rms.sdk.args.Args
    public void recycle() {
        this.mStringList0 = null;
    }
}