package com.vivo.services.rms.sdk.args;

import android.os.Parcel;
import com.vivo.services.rms.sdk.IntArrayFactory;

/* loaded from: classes.dex */
public class IntArrayStringArrayArgs extends Args {
    public int[] mIntArray0;
    public String[] mStringArray0;

    @Override // com.vivo.services.rms.sdk.args.Args
    public void writeToParcel(Parcel dest) {
        writeIntArray(dest, this.mIntArray0);
        dest.writeStringArray(this.mStringArray0);
    }

    @Override // com.vivo.services.rms.sdk.args.Args
    public void readFromParcel(Parcel data) {
        this.mIntArray0 = readIntArray(data);
        this.mStringArray0 = data.createStringArray();
    }

    @Override // com.vivo.services.rms.sdk.args.Args
    public void recycle() {
        IntArrayFactory.recycle(this.mIntArray0);
        this.mStringArray0 = null;
    }
}