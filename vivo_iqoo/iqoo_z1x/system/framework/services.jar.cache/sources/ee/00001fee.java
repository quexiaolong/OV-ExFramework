package com.google.android.startop.iorap;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/* loaded from: classes2.dex */
public class PackageEvent implements Parcelable {
    public static final Parcelable.Creator<PackageEvent> CREATOR = new Parcelable.Creator<PackageEvent>() { // from class: com.google.android.startop.iorap.PackageEvent.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public PackageEvent createFromParcel(Parcel in) {
            return new PackageEvent(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public PackageEvent[] newArray(int size) {
            return new PackageEvent[size];
        }
    };
    private static final int TYPE_MAX = 0;
    public static final int TYPE_REPLACED = 0;
    public final String packageName;
    public final Uri packageUri;
    public final int type;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface Type {
    }

    public static PackageEvent createReplaced(Uri packageUri, String packageName) {
        return new PackageEvent(0, packageUri, packageName);
    }

    private PackageEvent(int type, Uri packageUri, String packageName) {
        this.type = type;
        this.packageUri = packageUri;
        this.packageName = packageName;
        checkConstructorArguments();
    }

    private void checkConstructorArguments() {
        CheckHelpers.checkTypeInRange(this.type, 0);
        Objects.requireNonNull(this.packageUri, "packageUri");
        Objects.requireNonNull(this.packageName, "packageName");
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof PackageEvent) {
            return equals((PackageEvent) other);
        }
        return false;
    }

    private boolean equals(PackageEvent other) {
        return this.type == other.type && Objects.equals(this.packageUri, other.packageUri) && Objects.equals(this.packageName, other.packageName);
    }

    public String toString() {
        return String.format("{packageUri: %s, packageName: %s}", this.packageUri, this.packageName);
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.type);
        this.packageUri.writeToParcel(out, flags);
        out.writeString(this.packageName);
    }

    private PackageEvent(Parcel in) {
        this.type = in.readInt();
        this.packageUri = (Uri) Uri.CREATOR.createFromParcel(in);
        this.packageName = in.readString();
        checkConstructorArguments();
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }
}