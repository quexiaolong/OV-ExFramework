package com.android.server.usb.descriptors;

/* loaded from: classes2.dex */
public final class ByteStream {
    private static final String TAG = "ByteStream";
    private final byte[] mBytes;
    private int mIndex;
    private int mReadCount;

    public ByteStream(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException();
        }
        this.mBytes = bytes;
    }

    public void resetReadCount() {
        this.mReadCount = 0;
    }

    public int getReadCount() {
        return this.mReadCount;
    }

    public byte peekByte() {
        if (available() > 0) {
            return this.mBytes[this.mIndex + 1];
        }
        throw new IndexOutOfBoundsException();
    }

    public byte getByte() {
        if (available() > 0) {
            this.mReadCount++;
            byte[] bArr = this.mBytes;
            int i = this.mIndex;
            this.mIndex = i + 1;
            return bArr[i];
        }
        throw new IndexOutOfBoundsException();
    }

    public int getUnsignedByte() {
        if (available() > 0) {
            this.mReadCount++;
            byte[] bArr = this.mBytes;
            int i = this.mIndex;
            this.mIndex = i + 1;
            return bArr[i] & 255;
        }
        throw new IndexOutOfBoundsException();
    }

    public int unpackUsbShort() {
        if (available() >= 2) {
            int b0 = getUnsignedByte();
            int b1 = getUnsignedByte();
            return (b1 << 8) | b0;
        }
        throw new IndexOutOfBoundsException();
    }

    public int unpackUsbTriple() {
        if (available() >= 3) {
            int b0 = getUnsignedByte();
            int b1 = getUnsignedByte();
            int b2 = getUnsignedByte();
            return (b2 << 16) | (b1 << 8) | b0;
        }
        throw new IndexOutOfBoundsException();
    }

    public int unpackUsbInt() {
        if (available() >= 4) {
            int b0 = getUnsignedByte();
            int b1 = getUnsignedByte();
            int b2 = getUnsignedByte();
            int b3 = getUnsignedByte();
            return (b3 << 24) | (b2 << 16) | (b1 << 8) | b0;
        }
        throw new IndexOutOfBoundsException();
    }

    public void advance(int numBytes) {
        if (numBytes < 0) {
            throw new IllegalArgumentException();
        }
        int i = this.mIndex;
        long longNewIndex = i + numBytes;
        byte[] bArr = this.mBytes;
        if (longNewIndex <= bArr.length) {
            this.mReadCount += numBytes;
            this.mIndex = i + numBytes;
            return;
        }
        this.mIndex = bArr.length;
        throw new IndexOutOfBoundsException();
    }

    public void reverse(int numBytes) {
        if (numBytes < 0) {
            throw new IllegalArgumentException();
        }
        int i = this.mIndex;
        if (i >= numBytes) {
            this.mReadCount -= numBytes;
            this.mIndex = i - numBytes;
            return;
        }
        this.mIndex = 0;
        throw new IndexOutOfBoundsException();
    }

    public int available() {
        return this.mBytes.length - this.mIndex;
    }
}