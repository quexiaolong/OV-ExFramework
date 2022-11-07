package com.android.server.integrity.parser;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/* loaded from: classes.dex */
public class LimitInputStream extends FilterInputStream {
    private final int mLimit;
    private int mReadBytes;

    public LimitInputStream(InputStream in, int limit) {
        super(in);
        if (limit < 0) {
            throw new IllegalArgumentException("limit " + limit + " cannot be negative");
        }
        this.mReadBytes = 0;
        this.mLimit = limit;
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public int available() throws IOException {
        return Math.min(super.available(), this.mLimit - this.mReadBytes);
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public int read() throws IOException {
        int i = this.mReadBytes;
        if (i == this.mLimit) {
            return -1;
        }
        this.mReadBytes = i + 1;
        return super.read();
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public int read(byte[] b, int off, int len) throws IOException {
        if (len <= 0) {
            return 0;
        }
        int available = available();
        if (available <= 0) {
            return -1;
        }
        int result = super.read(b, off, Math.min(len, available));
        this.mReadBytes += result;
        return result;
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public long skip(long n) throws IOException {
        int available;
        if (n <= 0 || (available = available()) <= 0) {
            return 0L;
        }
        int bytesToSkip = (int) Math.min(available, n);
        long bytesSkipped = super.skip(bytesToSkip);
        this.mReadBytes += (int) bytesSkipped;
        return bytesSkipped;
    }
}