package com.android.server.integrity.model;

import java.io.IOException;
import java.io.OutputStream;

/* loaded from: classes.dex */
public class ByteTrackedOutputStream extends OutputStream {
    private final OutputStream mOutputStream;
    private int mWrittenBytesCount = 0;

    public ByteTrackedOutputStream(OutputStream outputStream) {
        this.mOutputStream = outputStream;
    }

    @Override // java.io.OutputStream
    public void write(int b) throws IOException {
        this.mWrittenBytesCount++;
        this.mOutputStream.write(b);
    }

    @Override // java.io.OutputStream
    public void write(byte[] bytes) throws IOException {
        write(bytes, 0, bytes.length);
    }

    @Override // java.io.OutputStream
    public void write(byte[] b, int off, int len) throws IOException {
        this.mWrittenBytesCount += len;
        this.mOutputStream.write(b, off, len);
    }

    public int getWrittenBytesCount() {
        return this.mWrittenBytesCount;
    }
}