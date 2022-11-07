package com.android.server;

import android.util.Slog;
import java.io.Closeable;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class RandomBlock {
    private static final int BLOCK_SIZE = 512;
    private static final boolean DEBUG = false;
    private static final String TAG = "RandomBlock";
    private byte[] block = new byte[512];

    private RandomBlock() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static RandomBlock fromFile(String filename) throws IOException {
        InputStream stream = null;
        try {
            stream = new FileInputStream(filename);
            return fromStream(stream);
        } finally {
            close(stream);
        }
    }

    private static RandomBlock fromStream(InputStream in) throws IOException {
        RandomBlock retval = new RandomBlock();
        int total = 0;
        while (total < 512) {
            int result = in.read(retval.block, total, 512 - total);
            if (result == -1) {
                throw new EOFException();
            }
            total += result;
        }
        return retval;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void toFile(String filename, boolean sync) throws IOException {
        RandomAccessFile out = null;
        try {
            out = new RandomAccessFile(filename, sync ? "rws" : "rw");
            toDataOut(out);
            truncateIfPossible(out);
        } finally {
            close(out);
        }
    }

    private static void truncateIfPossible(RandomAccessFile f) {
        try {
            f.setLength(512L);
        } catch (IOException e) {
        }
    }

    private void toDataOut(DataOutput out) throws IOException {
        out.write(this.block);
    }

    private static void close(Closeable c) {
        if (c == null) {
            return;
        }
        try {
            c.close();
        } catch (IOException e) {
            Slog.w(TAG, "IOException thrown while closing Closeable", e);
        }
    }
}