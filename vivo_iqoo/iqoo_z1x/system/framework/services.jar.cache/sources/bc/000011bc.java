package com.android.server.locksettings;

import android.app.admin.PasswordMetrics;
import com.android.internal.widget.LockscreenCredential;
import java.nio.ByteBuffer;

/* loaded from: classes.dex */
public class VersionedPasswordMetrics {
    private static final int VERSION_1 = 1;
    private final PasswordMetrics mMetrics;
    private final int mVersion;

    private VersionedPasswordMetrics(int version, PasswordMetrics metrics) {
        this.mMetrics = metrics;
        this.mVersion = version;
    }

    public VersionedPasswordMetrics(LockscreenCredential credential) {
        this(1, PasswordMetrics.computeForCredential(credential));
    }

    public int getVersion() {
        return this.mVersion;
    }

    public PasswordMetrics getMetrics() {
        return this.mMetrics;
    }

    public byte[] serialize() {
        ByteBuffer buffer = ByteBuffer.allocate(44);
        buffer.putInt(this.mVersion);
        buffer.putInt(this.mMetrics.credType);
        buffer.putInt(this.mMetrics.length);
        buffer.putInt(this.mMetrics.letters);
        buffer.putInt(this.mMetrics.upperCase);
        buffer.putInt(this.mMetrics.lowerCase);
        buffer.putInt(this.mMetrics.numeric);
        buffer.putInt(this.mMetrics.symbols);
        buffer.putInt(this.mMetrics.nonLetter);
        buffer.putInt(this.mMetrics.nonNumeric);
        buffer.putInt(this.mMetrics.seqLength);
        return buffer.array();
    }

    public static VersionedPasswordMetrics deserialize(byte[] data) {
        ByteBuffer buffer = ByteBuffer.allocate(data.length);
        buffer.put(data, 0, data.length);
        buffer.flip();
        int version = buffer.getInt();
        PasswordMetrics metrics = new PasswordMetrics(buffer.getInt(), buffer.getInt(), buffer.getInt(), buffer.getInt(), buffer.getInt(), buffer.getInt(), buffer.getInt(), buffer.getInt(), buffer.getInt(), buffer.getInt());
        return new VersionedPasswordMetrics(version, metrics);
    }
}