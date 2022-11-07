package com.android.server.integrity.parser;

import com.android.server.wm.ActivityTaskManagerService;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

/* loaded from: classes.dex */
public abstract class RandomAccessObject {
    public abstract void close() throws IOException;

    public abstract int length();

    public abstract int read() throws IOException;

    public abstract int read(byte[] bArr, int i, int i2) throws IOException;

    public abstract void seek(int i) throws IOException;

    public static RandomAccessObject ofFile(File file) throws IOException {
        return new RandomAccessFileObject(file);
    }

    public static RandomAccessObject ofBytes(byte[] bytes) {
        return new RandomAccessByteArrayObject(bytes);
    }

    /* loaded from: classes.dex */
    private static class RandomAccessFileObject extends RandomAccessObject {
        private final int mLength;
        private final RandomAccessFile mRandomAccessFile;

        RandomAccessFileObject(File file) throws IOException {
            long length = file.length();
            if (length > 2147483647L) {
                throw new IOException("Unsupported file size (too big) " + length);
            }
            this.mRandomAccessFile = new RandomAccessFile(file, ActivityTaskManagerService.DUMP_RECENTS_SHORT_CMD);
            this.mLength = (int) length;
        }

        @Override // com.android.server.integrity.parser.RandomAccessObject
        public void seek(int position) throws IOException {
            this.mRandomAccessFile.seek(position);
        }

        @Override // com.android.server.integrity.parser.RandomAccessObject
        public int read() throws IOException {
            return this.mRandomAccessFile.read();
        }

        @Override // com.android.server.integrity.parser.RandomAccessObject
        public int read(byte[] bytes, int off, int len) throws IOException {
            return this.mRandomAccessFile.read(bytes, off, len);
        }

        @Override // com.android.server.integrity.parser.RandomAccessObject
        public void close() throws IOException {
            this.mRandomAccessFile.close();
        }

        @Override // com.android.server.integrity.parser.RandomAccessObject
        public int length() {
            return this.mLength;
        }
    }

    /* loaded from: classes.dex */
    private static class RandomAccessByteArrayObject extends RandomAccessObject {
        private final ByteBuffer mBytes;

        RandomAccessByteArrayObject(byte[] bytes) {
            this.mBytes = ByteBuffer.wrap(bytes);
        }

        @Override // com.android.server.integrity.parser.RandomAccessObject
        public void seek(int position) throws IOException {
            this.mBytes.position(position);
        }

        @Override // com.android.server.integrity.parser.RandomAccessObject
        public int read() throws IOException {
            if (!this.mBytes.hasRemaining()) {
                return -1;
            }
            return this.mBytes.get() & 255;
        }

        @Override // com.android.server.integrity.parser.RandomAccessObject
        public int read(byte[] bytes, int off, int len) throws IOException {
            int bytesToCopy = Math.min(len, this.mBytes.remaining());
            if (bytesToCopy <= 0) {
                return 0;
            }
            this.mBytes.get(bytes, off, len);
            return bytesToCopy;
        }

        @Override // com.android.server.integrity.parser.RandomAccessObject
        public void close() throws IOException {
        }

        @Override // com.android.server.integrity.parser.RandomAccessObject
        public int length() {
            return this.mBytes.capacity();
        }
    }
}