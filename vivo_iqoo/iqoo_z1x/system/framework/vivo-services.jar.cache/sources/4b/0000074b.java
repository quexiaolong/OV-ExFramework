package com.vivo.services.rms.sdk.args;

import android.os.Parcel;
import com.vivo.services.rms.sdk.IntArrayFactory;

/* loaded from: classes.dex */
public class IntArray2Args extends Args {
    public int[] mIntArray0;
    public int[] mIntArray1;

    @Override // com.vivo.services.rms.sdk.args.Args
    public void writeToParcel(Parcel dest) {
        writeIntArray(dest, this.mIntArray0);
        writeIntArray(dest, this.mIntArray1);
    }

    @Override // com.vivo.services.rms.sdk.args.Args
    public void readFromParcel(Parcel data) {
        this.mIntArray0 = readIntArray(data);
        this.mIntArray1 = readIntArray(data);
    }

    @Override // com.vivo.services.rms.sdk.args.Args
    public void recycle() {
        IntArrayFactory.recycle(this.mIntArray0);
        IntArrayFactory.recycle(this.mIntArray1);
        this.mIntArray0 = null;
        this.mIntArray1 = null;
    }
}