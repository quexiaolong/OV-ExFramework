package com.google.android.startop.iorap;

import android.os.Parcel;
import android.os.Parcelable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/* loaded from: classes2.dex */
public class DexOptEvent implements Parcelable {
    public static final Parcelable.Creator<DexOptEvent> CREATOR = new Parcelable.Creator<DexOptEvent>() { // from class: com.google.android.startop.iorap.DexOptEvent.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public DexOptEvent createFromParcel(Parcel in) {
            return new DexOptEvent(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public DexOptEvent[] newArray(int size) {
            return new DexOptEvent[size];
        }
    };
    private static final int TYPE_MAX = 0;
    public static final int TYPE_PACKAGE_UPDATE = 0;
    public final String packageName;
    public final int type;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface Type {
    }

    public static DexOptEvent createPackageUpdate(String packageName) {
        return new DexOptEvent(0, packageName);
    }

    private DexOptEvent(int type, String packageName) {
        this.type = type;
        this.packageName = packageName;
        checkConstructorArguments();
    }

    private void checkConstructorArguments() {
        CheckHelpers.checkTypeInRange(this.type, 0);
        Objects.requireNonNull(this.packageName, "packageName");
    }

    public String toString() {
        return String.format("{DexOptEvent: packageName: %s}", this.packageName);
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof DexOptEvent) {
            return equals((DexOptEvent) other);
        }
        return false;
    }

    private boolean equals(DexOptEvent other) {
        return this.packageName.equals(other.packageName);
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.type);
        out.writeString(this.packageName);
    }

    private DexOptEvent(Parcel in) {
        this.type = in.readInt();
        this.packageName = in.readString();
        checkConstructorArguments();
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }
}