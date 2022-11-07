package com.google.android.startop.iorap;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.proto.ProtoOutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Objects;

/* loaded from: classes2.dex */
public abstract class AppLaunchEvent implements Parcelable {
    public static Parcelable.Creator<AppLaunchEvent> CREATOR = new Parcelable.Creator<AppLaunchEvent>() { // from class: com.google.android.startop.iorap.AppLaunchEvent.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public AppLaunchEvent createFromParcel(Parcel source) {
            int typeIndex = source.readInt();
            Class<?> kls = AppLaunchEvent.getClassFromTypeIndex(typeIndex);
            if (kls == null) {
                throw new IllegalArgumentException("Invalid type index: " + typeIndex);
            }
            try {
                return (AppLaunchEvent) kls.getConstructor(Parcel.class).newInstance(source);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            } catch (InstantiationException e2) {
                throw new AssertionError(e2);
            } catch (NoSuchMethodException e3) {
                throw new AssertionError(e3);
            } catch (InvocationTargetException e4) {
                throw new AssertionError(e4);
            }
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public AppLaunchEvent[] newArray(int size) {
            return new AppLaunchEvent[0];
        }
    };
    private static Class<?>[] sTypes = {IntentStarted.class, IntentFailed.class, ActivityLaunched.class, ActivityLaunchFinished.class, ActivityLaunchCancelled.class, ReportFullyDrawn.class};
    public final long sequenceId;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes2.dex */
    public @interface SequenceId {
    }

    protected AppLaunchEvent(long sequenceId) {
        this.sequenceId = sequenceId;
    }

    public boolean equals(Object other) {
        if (other instanceof AppLaunchEvent) {
            return equals((AppLaunchEvent) other);
        }
        return false;
    }

    protected boolean equals(AppLaunchEvent other) {
        return this.sequenceId == other.sequenceId;
    }

    public String toString() {
        return getClass().getSimpleName() + "{sequenceId=" + Long.toString(this.sequenceId) + toStringBody() + "}";
    }

    protected String toStringBody() {
        return "";
    }

    /* loaded from: classes2.dex */
    public static final class IntentStarted extends AppLaunchEvent {
        public final Intent intent;
        public final long timestampNs;

        public IntentStarted(long sequenceId, Intent intent, long timestampNs) {
            super(sequenceId);
            this.intent = intent;
            this.timestampNs = timestampNs;
            Objects.requireNonNull(intent, "intent");
        }

        @Override // com.google.android.startop.iorap.AppLaunchEvent
        public boolean equals(Object other) {
            return (other instanceof IntentStarted) && this.intent.equals(((IntentStarted) other).intent) && this.timestampNs == ((IntentStarted) other).timestampNs && super.equals(other);
        }

        @Override // com.google.android.startop.iorap.AppLaunchEvent
        protected String toStringBody() {
            return ", intent=" + this.intent.toString() + " , timestampNs=" + Long.toString(this.timestampNs);
        }

        @Override // com.google.android.startop.iorap.AppLaunchEvent
        protected void writeToParcelImpl(Parcel p, int flags) {
            super.writeToParcelImpl(p, flags);
            IntentProtoParcelable.write(p, this.intent, flags);
            p.writeLong(this.timestampNs);
        }

        IntentStarted(Parcel p) {
            super(p);
            this.intent = IntentProtoParcelable.create(p);
            this.timestampNs = p.readLong();
        }
    }

    /* loaded from: classes2.dex */
    public static final class IntentFailed extends AppLaunchEvent {
        public IntentFailed(long sequenceId) {
            super(sequenceId);
        }

        @Override // com.google.android.startop.iorap.AppLaunchEvent
        public boolean equals(Object other) {
            if (other instanceof IntentFailed) {
                return super.equals(other);
            }
            return false;
        }

        IntentFailed(Parcel p) {
            super(p);
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class BaseWithActivityRecordData extends AppLaunchEvent {
        public final byte[] activityRecordSnapshot;

        protected BaseWithActivityRecordData(long sequenceId, byte[] snapshot) {
            super(sequenceId);
            this.activityRecordSnapshot = snapshot;
            Objects.requireNonNull(snapshot, "snapshot");
        }

        @Override // com.google.android.startop.iorap.AppLaunchEvent
        public boolean equals(Object other) {
            return (other instanceof BaseWithActivityRecordData) && Arrays.equals(this.activityRecordSnapshot, ((BaseWithActivityRecordData) other).activityRecordSnapshot) && super.equals(other);
        }

        @Override // com.google.android.startop.iorap.AppLaunchEvent
        protected String toStringBody() {
            return ", " + new String(this.activityRecordSnapshot);
        }

        @Override // com.google.android.startop.iorap.AppLaunchEvent
        protected void writeToParcelImpl(Parcel p, int flags) {
            super.writeToParcelImpl(p, flags);
            ActivityRecordProtoParcelable.write(p, this.activityRecordSnapshot, flags);
        }

        BaseWithActivityRecordData(Parcel p) {
            super(p);
            this.activityRecordSnapshot = ActivityRecordProtoParcelable.create(p);
        }
    }

    /* loaded from: classes2.dex */
    public static final class ActivityLaunched extends BaseWithActivityRecordData {
        public final int temperature;

        public ActivityLaunched(long sequenceId, byte[] snapshot, int temperature) {
            super(sequenceId, snapshot);
            this.temperature = temperature;
        }

        @Override // com.google.android.startop.iorap.AppLaunchEvent.BaseWithActivityRecordData, com.google.android.startop.iorap.AppLaunchEvent
        public boolean equals(Object other) {
            return (other instanceof ActivityLaunched) && this.temperature == ((ActivityLaunched) other).temperature && super.equals(other);
        }

        @Override // com.google.android.startop.iorap.AppLaunchEvent.BaseWithActivityRecordData, com.google.android.startop.iorap.AppLaunchEvent
        protected String toStringBody() {
            return super.toStringBody() + ", temperature=" + Integer.toString(this.temperature);
        }

        @Override // com.google.android.startop.iorap.AppLaunchEvent.BaseWithActivityRecordData, com.google.android.startop.iorap.AppLaunchEvent
        protected void writeToParcelImpl(Parcel p, int flags) {
            super.writeToParcelImpl(p, flags);
            p.writeInt(this.temperature);
        }

        ActivityLaunched(Parcel p) {
            super(p);
            this.temperature = p.readInt();
        }
    }

    /* loaded from: classes2.dex */
    public static final class ActivityLaunchFinished extends BaseWithActivityRecordData {
        public final long timestampNs;

        public ActivityLaunchFinished(long sequenceId, byte[] snapshot, long timestampNs) {
            super(sequenceId, snapshot);
            this.timestampNs = timestampNs;
        }

        @Override // com.google.android.startop.iorap.AppLaunchEvent.BaseWithActivityRecordData, com.google.android.startop.iorap.AppLaunchEvent
        public boolean equals(Object other) {
            return (other instanceof ActivityLaunchFinished) && this.timestampNs == ((ActivityLaunchFinished) other).timestampNs && super.equals(other);
        }

        @Override // com.google.android.startop.iorap.AppLaunchEvent.BaseWithActivityRecordData, com.google.android.startop.iorap.AppLaunchEvent
        protected String toStringBody() {
            return super.toStringBody() + ", timestampNs=" + Long.toString(this.timestampNs);
        }

        @Override // com.google.android.startop.iorap.AppLaunchEvent.BaseWithActivityRecordData, com.google.android.startop.iorap.AppLaunchEvent
        protected void writeToParcelImpl(Parcel p, int flags) {
            super.writeToParcelImpl(p, flags);
            p.writeLong(this.timestampNs);
        }

        ActivityLaunchFinished(Parcel p) {
            super(p);
            this.timestampNs = p.readLong();
        }
    }

    /* loaded from: classes2.dex */
    public static class ActivityLaunchCancelled extends AppLaunchEvent {
        public final byte[] activityRecordSnapshot;

        public ActivityLaunchCancelled(long sequenceId, byte[] snapshot) {
            super(sequenceId);
            this.activityRecordSnapshot = snapshot;
        }

        @Override // com.google.android.startop.iorap.AppLaunchEvent
        public boolean equals(Object other) {
            return (other instanceof ActivityLaunchCancelled) && Arrays.equals(this.activityRecordSnapshot, ((ActivityLaunchCancelled) other).activityRecordSnapshot) && super.equals(other);
        }

        @Override // com.google.android.startop.iorap.AppLaunchEvent
        protected String toStringBody() {
            return super.toStringBody() + ", " + new String(this.activityRecordSnapshot);
        }

        @Override // com.google.android.startop.iorap.AppLaunchEvent
        protected void writeToParcelImpl(Parcel p, int flags) {
            super.writeToParcelImpl(p, flags);
            if (this.activityRecordSnapshot != null) {
                p.writeBoolean(true);
                ActivityRecordProtoParcelable.write(p, this.activityRecordSnapshot, flags);
                return;
            }
            p.writeBoolean(false);
        }

        ActivityLaunchCancelled(Parcel p) {
            super(p);
            if (p.readBoolean()) {
                this.activityRecordSnapshot = ActivityRecordProtoParcelable.create(p);
            } else {
                this.activityRecordSnapshot = null;
            }
        }
    }

    /* loaded from: classes2.dex */
    public static final class ReportFullyDrawn extends BaseWithActivityRecordData {
        public final long timestampNs;

        public ReportFullyDrawn(long sequenceId, byte[] snapshot, long timestampNs) {
            super(sequenceId, snapshot);
            this.timestampNs = timestampNs;
        }

        @Override // com.google.android.startop.iorap.AppLaunchEvent.BaseWithActivityRecordData, com.google.android.startop.iorap.AppLaunchEvent
        public boolean equals(Object other) {
            return (other instanceof ReportFullyDrawn) && this.timestampNs == ((ReportFullyDrawn) other).timestampNs && super.equals(other);
        }

        @Override // com.google.android.startop.iorap.AppLaunchEvent.BaseWithActivityRecordData, com.google.android.startop.iorap.AppLaunchEvent
        protected String toStringBody() {
            return super.toStringBody() + ", timestampNs=" + Long.toString(this.timestampNs);
        }

        @Override // com.google.android.startop.iorap.AppLaunchEvent.BaseWithActivityRecordData, com.google.android.startop.iorap.AppLaunchEvent
        protected void writeToParcelImpl(Parcel p, int flags) {
            super.writeToParcelImpl(p, flags);
            p.writeLong(this.timestampNs);
        }

        ReportFullyDrawn(Parcel p) {
            super(p);
            this.timestampNs = p.readLong();
        }
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel p, int flags) {
        p.writeInt(getTypeIndex());
        writeToParcelImpl(p, flags);
    }

    protected void writeToParcelImpl(Parcel p, int flags) {
        p.writeLong(this.sequenceId);
    }

    protected AppLaunchEvent(Parcel p) {
        this.sequenceId = p.readLong();
    }

    private int getTypeIndex() {
        int i = 0;
        while (true) {
            Class<?>[] clsArr = sTypes;
            if (i < clsArr.length) {
                if (!clsArr[i].equals(getClass())) {
                    i++;
                } else {
                    return i;
                }
            } else {
                throw new AssertionError("sTypes did not include this type: " + getClass());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Class<?> getClassFromTypeIndex(int typeIndex) {
        if (typeIndex >= 0) {
            Class<?>[] clsArr = sTypes;
            if (typeIndex < clsArr.length) {
                return clsArr[typeIndex];
            }
            return null;
        }
        return null;
    }

    /* loaded from: classes2.dex */
    public static class ActivityRecordProtoParcelable {
        public static void write(Parcel p, byte[] activityRecordSnapshot, int flags) {
            p.writeByteArray(activityRecordSnapshot);
        }

        public static byte[] create(Parcel p) {
            byte[] data = p.createByteArray();
            return data;
        }
    }

    /* loaded from: classes2.dex */
    public static class IntentProtoParcelable {
        private static final int INTENT_PROTO_CHUNK_SIZE = 1024;

        public static void write(Parcel p, Intent intent, int flags) {
            ProtoOutputStream protoOutputStream = new ProtoOutputStream(1024);
            intent.dumpDebug(protoOutputStream);
            byte[] bytes = protoOutputStream.getBytes();
            p.writeByteArray(bytes);
        }

        public static Intent create(Parcel p) {
            p.createByteArray();
            return new Intent("<cannot deserialize IntentProto>");
        }
    }
}