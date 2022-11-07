package com.android.server.timezone;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/* loaded from: classes2.dex */
final class CheckToken {
    final int mOptimisticLockId;
    final PackageVersions mPackageVersions;

    /* JADX INFO: Access modifiers changed from: package-private */
    public CheckToken(int optimisticLockId, PackageVersions packageVersions) {
        this.mOptimisticLockId = optimisticLockId;
        if (packageVersions == null) {
            throw new NullPointerException("packageVersions == null");
        }
        this.mPackageVersions = packageVersions;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public byte[] toByteArray() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(12);
        try {
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeInt(this.mOptimisticLockId);
            dos.writeLong(this.mPackageVersions.mUpdateAppVersion);
            dos.writeLong(this.mPackageVersions.mDataAppVersion);
            dos.close();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Unable to write into a ByteArrayOutputStream", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static CheckToken fromByteArray(byte[] tokenBytes) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(tokenBytes);
        DataInputStream dis = new DataInputStream(bais);
        try {
            int versionId = dis.readInt();
            long updateAppVersion = dis.readLong();
            long dataAppVersion = dis.readLong();
            CheckToken checkToken = new CheckToken(versionId, new PackageVersions(updateAppVersion, dataAppVersion));
            dis.close();
            return checkToken;
        } catch (Throwable th) {
            try {
                dis.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CheckToken checkToken = (CheckToken) o;
        if (this.mOptimisticLockId != checkToken.mOptimisticLockId) {
            return false;
        }
        return this.mPackageVersions.equals(checkToken.mPackageVersions);
    }

    public int hashCode() {
        int result = this.mOptimisticLockId;
        return (result * 31) + this.mPackageVersions.hashCode();
    }

    public String toString() {
        return "Token{mOptimisticLockId=" + this.mOptimisticLockId + ", mPackageVersions=" + this.mPackageVersions + '}';
    }
}