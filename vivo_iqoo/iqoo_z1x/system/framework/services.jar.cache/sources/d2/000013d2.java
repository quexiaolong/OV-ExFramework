package com.android.server.notification;

import android.util.StatsEvent;

/* loaded from: classes.dex */
public class SysUiStatsEvent {

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class Builder {
        private final StatsEvent.Builder mBuilder;

        protected Builder(StatsEvent.Builder builder) {
            this.mBuilder = builder;
        }

        public StatsEvent build() {
            return this.mBuilder.build();
        }

        public Builder setAtomId(int atomId) {
            this.mBuilder.setAtomId(atomId);
            return this;
        }

        public Builder writeInt(int value) {
            this.mBuilder.writeInt(value);
            return this;
        }

        public Builder addBooleanAnnotation(byte annotation, boolean value) {
            this.mBuilder.addBooleanAnnotation(annotation, value);
            return this;
        }

        public Builder writeString(String value) {
            this.mBuilder.writeString(value);
            return this;
        }

        public Builder writeBoolean(boolean value) {
            this.mBuilder.writeBoolean(value);
            return this;
        }

        public Builder writeByteArray(byte[] value) {
            this.mBuilder.writeByteArray(value);
            return this;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class BuilderFactory {
        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder newBuilder() {
            return new Builder(StatsEvent.newBuilder());
        }
    }
}