package com.google.android.startop.iorap;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

/* loaded from: classes2.dex */
public class ActivityInfo implements Parcelable {
    public static final Parcelable.Creator<ActivityInfo> CREATOR = new Parcelable.Creator<ActivityInfo>() { // from class: com.google.android.startop.iorap.ActivityInfo.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ActivityInfo createFromParcel(Parcel in) {
            return new ActivityInfo(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ActivityInfo[] newArray(int size) {
            return new ActivityInfo[size];
        }
    };
    public final String activityName;
    public final String packageName;

    public ActivityInfo(String packageName, String activityName) {
        this.packageName = packageName;
        this.activityName = activityName;
        checkConstructorArguments();
    }

    private void checkConstructorArguments() {
        Objects.requireNonNull(this.packageName, "packageName");
        Objects.requireNonNull(this.activityName, "activityName");
    }

    public String toString() {
        return String.format("{packageName: %s, activityName: %s}", this.packageName, this.activityName);
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof ActivityInfo) {
            return equals((ActivityInfo) other);
        }
        return false;
    }

    private boolean equals(ActivityInfo other) {
        return Objects.equals(this.packageName, other.packageName) && Objects.equals(this.activityName, other.activityName);
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.packageName);
        out.writeString(this.activityName);
    }

    private ActivityInfo(Parcel in) {
        this.packageName = in.readString();
        this.activityName = in.readString();
        checkConstructorArguments();
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }
}