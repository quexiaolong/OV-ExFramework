package com.vivo.services.rms.sdk.args;

import android.os.Parcel;
import android.os.Parcelable;
import com.vivo.services.rms.sdk.IntArrayFactory;

/* loaded from: classes.dex */
public abstract class Args implements Parcelable {
    public static final Parcelable.Creator<Args> CREATOR = new Parcelable.Creator<Args>() { // from class: com.vivo.services.rms.sdk.args.Args.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public Args createFromParcel(Parcel source) {
            String className = source.readString();
            Args data = ArgsFactory.create(className);
            if (data != null) {
                data.readFromParcel(source);
            }
            return data;
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public Args[] newArray(int size) {
            return new Args[size];
        }
    };
    private final String mClassName = getClass().getSimpleName();

    public abstract void readFromParcel(Parcel parcel);

    public abstract void recycle();

    public abstract void writeToParcel(Parcel parcel);

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mClassName);
        writeToParcel(dest);
    }

    public String getClassName() {
        return this.mClassName;
    }

    public void writeIntArray(Parcel dest, int[] val) {
        if (val != null) {
            int N = val.length;
            dest.writeInt(N);
            for (int i : val) {
                dest.writeInt(i);
            }
            return;
        }
        dest.writeInt(-1);
    }

    public int[] readIntArray(Parcel data) {
        int N = data.readInt();
        int[] val = null;
        if (N >= 0) {
            val = IntArrayFactory.create(N);
            for (int i = 0; i < N; i++) {
                val[i] = data.readInt();
            }
        }
        return val;
    }
}