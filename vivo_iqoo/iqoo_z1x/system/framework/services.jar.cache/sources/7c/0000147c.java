package com.android.server.people.data;

import android.text.format.DateFormat;
import android.util.ArraySet;
import android.util.Slog;
import android.util.proto.ProtoInputStream;
import android.util.proto.ProtoOutputStream;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;
import java.util.Set;

/* loaded from: classes.dex */
public class Event {
    public static final int TYPE_CALL_INCOMING = 11;
    public static final int TYPE_CALL_MISSED = 12;
    public static final int TYPE_CALL_OUTGOING = 10;
    public static final int TYPE_IN_APP_CONVERSATION = 13;
    public static final int TYPE_NOTIFICATION_OPENED = 3;
    public static final int TYPE_NOTIFICATION_POSTED = 2;
    public static final int TYPE_SHARE_IMAGE = 5;
    public static final int TYPE_SHARE_OTHER = 7;
    public static final int TYPE_SHARE_TEXT = 4;
    public static final int TYPE_SHARE_VIDEO = 6;
    public static final int TYPE_SHORTCUT_INVOCATION = 1;
    public static final int TYPE_SMS_INCOMING = 9;
    public static final int TYPE_SMS_OUTGOING = 8;
    private final int mDurationSeconds;
    private final long mTimestamp;
    private final int mType;
    private static final String TAG = Event.class.getSimpleName();
    public static final Set<Integer> NOTIFICATION_EVENT_TYPES = new ArraySet();
    public static final Set<Integer> SHARE_EVENT_TYPES = new ArraySet();
    public static final Set<Integer> SMS_EVENT_TYPES = new ArraySet();
    public static final Set<Integer> CALL_EVENT_TYPES = new ArraySet();
    public static final Set<Integer> ALL_EVENT_TYPES = new ArraySet();

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface EventType {
    }

    static {
        NOTIFICATION_EVENT_TYPES.add(2);
        NOTIFICATION_EVENT_TYPES.add(3);
        SHARE_EVENT_TYPES.add(4);
        SHARE_EVENT_TYPES.add(5);
        SHARE_EVENT_TYPES.add(6);
        SHARE_EVENT_TYPES.add(7);
        SMS_EVENT_TYPES.add(9);
        SMS_EVENT_TYPES.add(8);
        CALL_EVENT_TYPES.add(11);
        CALL_EVENT_TYPES.add(10);
        CALL_EVENT_TYPES.add(12);
        ALL_EVENT_TYPES.add(1);
        ALL_EVENT_TYPES.add(13);
        ALL_EVENT_TYPES.addAll(NOTIFICATION_EVENT_TYPES);
        ALL_EVENT_TYPES.addAll(SHARE_EVENT_TYPES);
        ALL_EVENT_TYPES.addAll(SMS_EVENT_TYPES);
        ALL_EVENT_TYPES.addAll(CALL_EVENT_TYPES);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Event(long timestamp, int type) {
        this.mTimestamp = timestamp;
        this.mType = type;
        this.mDurationSeconds = 0;
    }

    private Event(Builder builder) {
        this.mTimestamp = builder.mTimestamp;
        this.mType = builder.mType;
        this.mDurationSeconds = builder.mDurationSeconds;
    }

    public long getTimestamp() {
        return this.mTimestamp;
    }

    public int getType() {
        return this.mType;
    }

    public int getDurationSeconds() {
        return this.mDurationSeconds;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writeToProto(ProtoOutputStream protoOutputStream) {
        protoOutputStream.write(1120986464257L, this.mType);
        protoOutputStream.write(1112396529666L, this.mTimestamp);
        protoOutputStream.write(1120986464259L, this.mDurationSeconds);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Event readFromProto(ProtoInputStream protoInputStream) throws IOException {
        Builder builder = new Builder();
        while (protoInputStream.nextField() != -1) {
            int fieldNumber = protoInputStream.getFieldNumber();
            if (fieldNumber == 1) {
                builder.setType(protoInputStream.readInt(1120986464257L));
            } else if (fieldNumber == 2) {
                builder.setTimestamp(protoInputStream.readLong(1112396529666L));
            } else if (fieldNumber == 3) {
                builder.setDurationSeconds(protoInputStream.readInt(1120986464259L));
            } else {
                String str = TAG;
                Slog.w(str, "Could not read undefined field: " + protoInputStream.getFieldNumber());
            }
        }
        return builder.build();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Event) {
            Event other = (Event) obj;
            return this.mTimestamp == other.mTimestamp && this.mType == other.mType && this.mDurationSeconds == other.mDurationSeconds;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Long.valueOf(this.mTimestamp), Integer.valueOf(this.mType), Integer.valueOf(this.mDurationSeconds));
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Event {");
        sb.append("timestamp=");
        sb.append(DateFormat.format("yyyy-MM-dd HH:mm:ss", this.mTimestamp));
        sb.append(", type=");
        sb.append(this.mType);
        if (this.mDurationSeconds > 0) {
            sb.append(", durationSeconds=");
            sb.append(this.mDurationSeconds);
        }
        sb.append("}");
        return sb.toString();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class Builder {
        private int mDurationSeconds;
        private long mTimestamp;
        private int mType;

        private Builder() {
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder(long timestamp, int type) {
            this.mTimestamp = timestamp;
            this.mType = type;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setDurationSeconds(int durationSeconds) {
            this.mDurationSeconds = durationSeconds;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Builder setTimestamp(long timestamp) {
            this.mTimestamp = timestamp;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public Builder setType(int type) {
            this.mType = type;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Event build() {
            return new Event(this);
        }
    }
}