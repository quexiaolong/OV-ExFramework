package com.android.server.backup.remote;

import com.android.internal.util.Preconditions;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/* loaded from: classes.dex */
public class RemoteResult {
    private final int mType;
    private final long mValue;
    public static final RemoteResult FAILED_TIMED_OUT = new RemoteResult(1, 0);
    public static final RemoteResult FAILED_CANCELLED = new RemoteResult(2, 0);
    public static final RemoteResult FAILED_THREAD_INTERRUPTED = new RemoteResult(3, 0);

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    private @interface Type {
        public static final int FAILED_CANCELLED = 2;
        public static final int FAILED_THREAD_INTERRUPTED = 3;
        public static final int FAILED_TIMED_OUT = 1;
        public static final int SUCCESS = 0;
    }

    public static RemoteResult of(long value) {
        return new RemoteResult(0, value);
    }

    private RemoteResult(int type, long value) {
        this.mType = type;
        this.mValue = value;
    }

    public boolean isPresent() {
        return this.mType == 0;
    }

    public long get() {
        Preconditions.checkState(isPresent(), "Can't obtain value of failed result");
        return this.mValue;
    }

    public String toString() {
        return "RemoteResult{" + toStringDescription() + "}";
    }

    private String toStringDescription() {
        int i = this.mType;
        if (i != 0) {
            if (i != 1) {
                if (i != 2) {
                    if (i == 3) {
                        return "FAILED_THREAD_INTERRUPTED";
                    }
                    throw new AssertionError("Unknown type");
                }
                return "FAILED_CANCELLED";
            }
            return "FAILED_TIMED_OUT";
        }
        return Long.toString(this.mValue);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof RemoteResult) {
            RemoteResult that = (RemoteResult) o;
            return this.mType == that.mType && this.mValue == that.mValue;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mType), Long.valueOf(this.mValue));
    }
}