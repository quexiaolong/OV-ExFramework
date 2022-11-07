package com.google.android.startop.iorap;

import android.os.Parcel;
import android.os.Parcelable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/* loaded from: classes2.dex */
public class SystemServiceEvent implements Parcelable {
    public static final Parcelable.Creator<SystemServiceEvent> CREATOR = new Parcelable.Creator<SystemServiceEvent>() { // from class: com.google.android.startop.iorap.SystemServiceEvent.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SystemServiceEvent createFromParcel(Parcel in) {
            return new SystemServiceEvent(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SystemServiceEvent[] newArray(int size) {
            return new SystemServiceEvent[size];
        }
    };
    public static final int TYPE_BOOT_PHASE = 0;
    private static final int TYPE_MAX = 1;
    public static final int TYPE_START = 1;
    public final int type;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface Type {
    }

    public SystemServiceEvent(int type) {
        this.type = type;
        checkConstructorArguments();
    }

    private void checkConstructorArguments() {
        CheckHelpers.checkTypeInRange(this.type, 1);
    }

    public String toString() {
        return String.format("{type: %d}", Integer.valueOf(this.type));
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof SystemServiceEvent) {
            return equals((SystemServiceEvent) other);
        }
        return false;
    }

    private boolean equals(SystemServiceEvent other) {
        return this.type == other.type;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.type);
    }

    private SystemServiceEvent(Parcel in) {
        this.type = in.readInt();
        checkConstructorArguments();
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }
}