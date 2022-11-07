package com.google.android.startop.iorap;

import android.app.job.JobParameters;
import android.os.Parcel;
import android.os.Parcelable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/* loaded from: classes2.dex */
public class JobScheduledEvent implements Parcelable {
    public static final Parcelable.Creator<JobScheduledEvent> CREATOR = new Parcelable.Creator<JobScheduledEvent>() { // from class: com.google.android.startop.iorap.JobScheduledEvent.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public JobScheduledEvent createFromParcel(Parcel in) {
            return new JobScheduledEvent(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public JobScheduledEvent[] newArray(int size) {
            return new JobScheduledEvent[size];
        }
    };
    public static final int SORT_IDLE_MAINTENANCE = 0;
    private static final int SORT_MAX = 0;
    private static final int TYPE_MAX = 1;
    public static final int TYPE_START_JOB = 0;
    public static final int TYPE_STOP_JOB = 1;
    public final int jobId;
    public final int sort;
    public final int type;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface Sort {
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface Type {
    }

    public static JobScheduledEvent createIdleMaintenance(int type, JobParameters jobParams) {
        return new JobScheduledEvent(type, jobParams.getJobId(), 0);
    }

    private JobScheduledEvent(int type, int jobId, int sort) {
        this.type = type;
        this.jobId = jobId;
        this.sort = sort;
        checkConstructorArguments();
    }

    private void checkConstructorArguments() {
        CheckHelpers.checkTypeInRange(this.type, 1);
        CheckHelpers.checkTypeInRange(this.sort, 0);
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof JobScheduledEvent) {
            return equals((JobScheduledEvent) other);
        }
        return false;
    }

    private boolean equals(JobScheduledEvent other) {
        return this.type == other.type && this.jobId == other.jobId && this.sort == other.sort;
    }

    public String toString() {
        return String.format("{type: %d, jobId: %d, sort: %d}", Integer.valueOf(this.type), Integer.valueOf(this.jobId), Integer.valueOf(this.sort));
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.type);
        out.writeInt(this.jobId);
        out.writeInt(this.sort);
    }

    private JobScheduledEvent(Parcel in) {
        this.type = in.readInt();
        this.jobId = in.readInt();
        this.sort = in.readInt();
        checkConstructorArguments();
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }
}