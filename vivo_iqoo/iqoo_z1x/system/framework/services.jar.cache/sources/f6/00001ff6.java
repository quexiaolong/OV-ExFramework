package com.google.android.startop.iorap;

import android.os.Parcel;
import android.os.Parcelable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/* loaded from: classes2.dex */
public class SystemServiceUserEvent implements Parcelable {
    public static final Parcelable.Creator<SystemServiceUserEvent> CREATOR = new Parcelable.Creator<SystemServiceUserEvent>() { // from class: com.google.android.startop.iorap.SystemServiceUserEvent.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SystemServiceUserEvent createFromParcel(Parcel in) {
            return new SystemServiceUserEvent(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SystemServiceUserEvent[] newArray(int size) {
            return new SystemServiceUserEvent[size];
        }
    };
    public static final int TYPE_CLEANUP_USER = 4;
    private static final int TYPE_MAX = 4;
    public static final int TYPE_START_USER = 0;
    public static final int TYPE_STOP_USER = 3;
    public static final int TYPE_SWITCH_USER = 2;
    public static final int TYPE_UNLOCK_USER = 1;
    public final int type;
    public final int userHandle;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface Type {
    }

    public SystemServiceUserEvent(int type, int userHandle) {
        this.type = type;
        this.userHandle = userHandle;
        checkConstructorArguments();
    }

    private void checkConstructorArguments() {
        CheckHelpers.checkTypeInRange(this.type, 4);
        if (this.userHandle < 0) {
            throw new IllegalArgumentException("userHandle must be non-negative");
        }
    }

    public String toString() {
        return String.format("{type: %d, userHandle: %d}", Integer.valueOf(this.type), Integer.valueOf(this.userHandle));
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof SystemServiceUserEvent) {
            return equals((SystemServiceUserEvent) other);
        }
        return false;
    }

    private boolean equals(SystemServiceUserEvent other) {
        return this.type == other.type && this.userHandle == other.userHandle;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.type);
        out.writeInt(this.userHandle);
    }

    private SystemServiceUserEvent(Parcel in) {
        this.type = in.readInt();
        this.userHandle = in.readInt();
        checkConstructorArguments();
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }
}