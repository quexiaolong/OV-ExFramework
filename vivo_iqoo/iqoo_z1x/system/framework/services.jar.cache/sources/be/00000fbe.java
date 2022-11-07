package com.android.server.integrity.parser;

import java.io.IOException;
import java.io.InputStream;

/* loaded from: classes.dex */
public class RandomAccessInputStream extends InputStream {
    private int mPosition = 0;
    private final RandomAccessObject mRandomAccessObject;

    public RandomAccessInputStream(RandomAccessObject object) throws IOException {
        this.mRandomAccessObject = object;
    }

    public int getPosition() {
        return this.mPosition;
    }

    public void seek(int position) throws IOException {
        this.mRandomAccessObject.seek(position);
        this.mPosition = position;
    }

    @Override // java.io.InputStream
    public int available() throws IOException {
        return this.mRandomAccessObject.length() - this.mPosition;
    }

    @Override // java.io.InputStream, java.io.Closeable, java.lang.AutoCloseable
    public void close() throws IOException {
        this.mRandomAccessObject.close();
    }

    @Override // java.io.InputStream
    public int read() throws IOException {
        if (available() <= 0) {
            return -1;
        }
        this.mPosition++;
        return this.mRandomAccessObject.read();
    }

    @Override // java.io.InputStream
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override // java.io.InputStream
    public int read(byte[] b, int off, int len) throws IOException {
        if (len <= 0) {
            return 0;
        }
        int available = available();
        if (available <= 0) {
            return -1;
        }
        int result = this.mRandomAccessObject.read(b, off, Math.min(len, available));
        this.mPosition += result;
        return result;
    }

    @Override // java.io.InputStream
    public long skip(long n) throws IOException {
        int available;
        if (n <= 0 || (available = available()) <= 0) {
            return 0L;
        }
        int skipAmount = (int) Math.min(available, n);
        int i = this.mPosition + skipAmount;
        this.mPosition = i;
        this.mRandomAccessObject.seek(i);
        return skipAmount;
    }
}