package com.android.server.people.data;

import android.content.LocusId;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Slog;
import android.util.proto.ProtoInputStream;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.Preconditions;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/* loaded from: classes.dex */
public class ConversationInfo {
    private static final int FLAG_BUBBLED = 4;
    private static final int FLAG_CONTACT_STARRED = 32;
    private static final int FLAG_DEMOTED = 64;
    private static final int FLAG_IMPORTANT = 1;
    private static final int FLAG_NOTIFICATION_SILENCED = 2;
    private static final int FLAG_PERSON_BOT = 16;
    private static final int FLAG_PERSON_IMPORTANT = 8;
    private static final String TAG = ConversationInfo.class.getSimpleName();
    private String mContactPhoneNumber;
    private Uri mContactUri;
    private int mConversationFlags;
    private LocusId mLocusId;
    private String mNotificationChannelId;
    private int mShortcutFlags;
    private String mShortcutId;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    private @interface ConversationFlags {
    }

    private ConversationInfo(Builder builder) {
        this.mShortcutId = builder.mShortcutId;
        this.mLocusId = builder.mLocusId;
        this.mContactUri = builder.mContactUri;
        this.mContactPhoneNumber = builder.mContactPhoneNumber;
        this.mNotificationChannelId = builder.mNotificationChannelId;
        this.mShortcutFlags = builder.mShortcutFlags;
        this.mConversationFlags = builder.mConversationFlags;
    }

    public String getShortcutId() {
        return this.mShortcutId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public LocusId getLocusId() {
        return this.mLocusId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Uri getContactUri() {
        return this.mContactUri;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getContactPhoneNumber() {
        return this.mContactPhoneNumber;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getNotificationChannelId() {
        return this.mNotificationChannelId;
    }

    public boolean isShortcutLongLived() {
        return hasShortcutFlags(8192);
    }

    public boolean isShortcutCachedForNotification() {
        return hasShortcutFlags(16384);
    }

    public boolean isImportant() {
        return hasConversationFlags(1);
    }

    public boolean isNotificationSilenced() {
        return hasConversationFlags(2);
    }

    public boolean isBubbled() {
        return hasConversationFlags(4);
    }

    public boolean isDemoted() {
        return hasConversationFlags(64);
    }

    public boolean isPersonImportant() {
        return hasConversationFlags(8);
    }

    public boolean isPersonBot() {
        return hasConversationFlags(16);
    }

    public boolean isContactStarred() {
        return hasConversationFlags(32);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ConversationInfo) {
            ConversationInfo other = (ConversationInfo) obj;
            return Objects.equals(this.mShortcutId, other.mShortcutId) && Objects.equals(this.mLocusId, other.mLocusId) && Objects.equals(this.mContactUri, other.mContactUri) && Objects.equals(this.mContactPhoneNumber, other.mContactPhoneNumber) && Objects.equals(this.mNotificationChannelId, other.mNotificationChannelId) && this.mShortcutFlags == other.mShortcutFlags && this.mConversationFlags == other.mConversationFlags;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(this.mShortcutId, this.mLocusId, this.mContactUri, this.mContactPhoneNumber, this.mNotificationChannelId, Integer.valueOf(this.mShortcutFlags), Integer.valueOf(this.mConversationFlags));
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ConversationInfo {");
        sb.append("shortcutId=");
        sb.append(this.mShortcutId);
        sb.append(", locusId=");
        sb.append(this.mLocusId);
        sb.append(", contactUri=");
        sb.append(this.mContactUri);
        sb.append(", phoneNumber=");
        sb.append(this.mContactPhoneNumber);
        sb.append(", notificationChannelId=");
        sb.append(this.mNotificationChannelId);
        sb.append(", shortcutFlags=0x");
        sb.append(Integer.toHexString(this.mShortcutFlags));
        sb.append(" [");
        if (isShortcutLongLived()) {
            sb.append("Liv");
        }
        if (isShortcutCachedForNotification()) {
            sb.append("Cac");
        }
        sb.append("]");
        sb.append(", conversationFlags=0x");
        sb.append(Integer.toHexString(this.mConversationFlags));
        sb.append(" [");
        if (isImportant()) {
            sb.append("Imp");
        }
        if (isNotificationSilenced()) {
            sb.append("Sil");
        }
        if (isBubbled()) {
            sb.append("Bub");
        }
        if (isDemoted()) {
            sb.append("Dem");
        }
        if (isPersonImportant()) {
            sb.append("PIm");
        }
        if (isPersonBot()) {
            sb.append("Bot");
        }
        if (isContactStarred()) {
            sb.append("Sta");
        }
        sb.append("]}");
        return sb.toString();
    }

    private boolean hasShortcutFlags(int flags) {
        return (this.mShortcutFlags & flags) == flags;
    }

    private boolean hasConversationFlags(int flags) {
        return (this.mConversationFlags & flags) == flags;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writeToProto(ProtoOutputStream protoOutputStream) {
        protoOutputStream.write(1138166333441L, this.mShortcutId);
        if (this.mLocusId != null) {
            long locusIdToken = protoOutputStream.start(1146756268034L);
            protoOutputStream.write(1138166333441L, this.mLocusId.getId());
            protoOutputStream.end(locusIdToken);
        }
        Uri uri = this.mContactUri;
        if (uri != null) {
            protoOutputStream.write(1138166333443L, uri.toString());
        }
        String str = this.mNotificationChannelId;
        if (str != null) {
            protoOutputStream.write(1138166333444L, str);
        }
        protoOutputStream.write(1120986464261L, this.mShortcutFlags);
        protoOutputStream.write(1120986464262L, this.mConversationFlags);
        String str2 = this.mContactPhoneNumber;
        if (str2 != null) {
            protoOutputStream.write(1138166333447L, str2);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public byte[] getBackupPayload() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);
        try {
            out.writeUTF(this.mShortcutId);
            out.writeUTF(this.mLocusId != null ? this.mLocusId.getId() : "");
            out.writeUTF(this.mContactUri != null ? this.mContactUri.toString() : "");
            out.writeUTF(this.mNotificationChannelId != null ? this.mNotificationChannelId : "");
            out.writeInt(this.mShortcutFlags);
            out.writeInt(this.mConversationFlags);
            out.writeUTF(this.mContactPhoneNumber != null ? this.mContactPhoneNumber : "");
            return baos.toByteArray();
        } catch (IOException e) {
            Slog.e(TAG, "Failed to write fields to backup payload.", e);
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ConversationInfo readFromProto(ProtoInputStream protoInputStream) throws IOException {
        Builder builder = new Builder();
        while (protoInputStream.nextField() != -1) {
            switch (protoInputStream.getFieldNumber()) {
                case 1:
                    builder.setShortcutId(protoInputStream.readString(1138166333441L));
                    break;
                case 2:
                    long locusIdToken = protoInputStream.start(1146756268034L);
                    while (protoInputStream.nextField() != -1) {
                        if (protoInputStream.getFieldNumber() == 1) {
                            builder.setLocusId(new LocusId(protoInputStream.readString(1138166333441L)));
                        }
                    }
                    protoInputStream.end(locusIdToken);
                    break;
                case 3:
                    builder.setContactUri(Uri.parse(protoInputStream.readString(1138166333443L)));
                    break;
                case 4:
                    builder.setNotificationChannelId(protoInputStream.readString(1138166333444L));
                    break;
                case 5:
                    builder.setShortcutFlags(protoInputStream.readInt(1120986464261L));
                    break;
                case 6:
                    builder.setConversationFlags(protoInputStream.readInt(1120986464262L));
                    break;
                case 7:
                    builder.setContactPhoneNumber(protoInputStream.readString(1138166333447L));
                    break;
                default:
                    String str = TAG;
                    Slog.w(str, "Could not read undefined field: " + protoInputStream.getFieldNumber());
                    break;
            }
        }
        return builder.build();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ConversationInfo readFromBackupPayload(byte[] payload) {
        Builder builder = new Builder();
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload));
        try {
            builder.setShortcutId(in.readUTF());
            String locusId = in.readUTF();
            if (!TextUtils.isEmpty(locusId)) {
                builder.setLocusId(new LocusId(locusId));
            }
            String contactUri = in.readUTF();
            if (!TextUtils.isEmpty(contactUri)) {
                builder.setContactUri(Uri.parse(contactUri));
            }
            String notificationChannelId = in.readUTF();
            if (!TextUtils.isEmpty(notificationChannelId)) {
                builder.setNotificationChannelId(notificationChannelId);
            }
            builder.setShortcutFlags(in.readInt());
            builder.setConversationFlags(in.readInt());
            String contactPhoneNumber = in.readUTF();
            if (!TextUtils.isEmpty(contactPhoneNumber)) {
                builder.setContactPhoneNumber(contactPhoneNumber);
            }
            return builder.build();
        } catch (IOException e) {
            Slog.e(TAG, "Failed to read conversation info fields from backup payload.", e);
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class Builder {
        private String mContactPhoneNumber;
        private Uri mContactUri;
        private int mConversationFlags;
        private LocusId mLocusId;
        private String mNotificationChannelId;
        private int mShortcutFlags;
        private String mShortcutId;

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder() {
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder(ConversationInfo conversationInfo) {
            String str = this.mShortcutId;
            if (str == null) {
                this.mShortcutId = conversationInfo.mShortcutId;
            } else {
                Preconditions.checkArgument(str.equals(conversationInfo.mShortcutId));
            }
            this.mLocusId = conversationInfo.mLocusId;
            this.mContactUri = conversationInfo.mContactUri;
            this.mContactPhoneNumber = conversationInfo.mContactPhoneNumber;
            this.mNotificationChannelId = conversationInfo.mNotificationChannelId;
            this.mShortcutFlags = conversationInfo.mShortcutFlags;
            this.mConversationFlags = conversationInfo.mConversationFlags;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setShortcutId(String shortcutId) {
            this.mShortcutId = shortcutId;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setLocusId(LocusId locusId) {
            this.mLocusId = locusId;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setContactUri(Uri contactUri) {
            this.mContactUri = contactUri;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setContactPhoneNumber(String phoneNumber) {
            this.mContactPhoneNumber = phoneNumber;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setNotificationChannelId(String notificationChannelId) {
            this.mNotificationChannelId = notificationChannelId;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setShortcutFlags(int shortcutFlags) {
            this.mShortcutFlags = shortcutFlags;
            return this;
        }

        Builder setConversationFlags(int conversationFlags) {
            this.mConversationFlags = conversationFlags;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setImportant(boolean value) {
            return setConversationFlag(1, value);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setNotificationSilenced(boolean value) {
            return setConversationFlag(2, value);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setBubbled(boolean value) {
            return setConversationFlag(4, value);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setDemoted(boolean value) {
            return setConversationFlag(64, value);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setPersonImportant(boolean value) {
            return setConversationFlag(8, value);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setPersonBot(boolean value) {
            return setConversationFlag(16, value);
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public Builder setContactStarred(boolean value) {
            return setConversationFlag(32, value);
        }

        private Builder setConversationFlag(int flags, boolean value) {
            if (value) {
                return addConversationFlags(flags);
            }
            return removeConversationFlags(flags);
        }

        private Builder addConversationFlags(int flags) {
            this.mConversationFlags |= flags;
            return this;
        }

        private Builder removeConversationFlags(int flags) {
            this.mConversationFlags &= ~flags;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public ConversationInfo build() {
            Objects.requireNonNull(this.mShortcutId);
            return new ConversationInfo(this);
        }
    }
}