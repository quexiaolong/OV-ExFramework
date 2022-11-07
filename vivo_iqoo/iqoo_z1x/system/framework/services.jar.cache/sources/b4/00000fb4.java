package com.android.server.integrity.model;

import java.io.IOException;
import java.io.InputStream;

/* loaded from: classes.dex */
public class BitInputStream {
    private long mBitsRead;
    private byte mCurrentByte;
    private InputStream mInputStream;

    public BitInputStream(InputStream inputStream) {
        this.mInputStream = inputStream;
    }

    public int getNext(int numOfBits) throws IOException {
        int component = 0;
        int offset = 0;
        while (true) {
            int count = offset + 1;
            if (offset < numOfBits) {
                if (this.mBitsRead % 8 == 0) {
                    this.mCurrentByte = getNextByte();
                }
                long j = this.mBitsRead;
                int offset2 = 7 - ((int) (j % 8));
                component = (component << 1) | ((this.mCurrentByte >>> offset2) & 1);
                this.mBitsRead = j + 1;
                offset = count;
            } else {
                return component;
            }
        }
    }

    public boolean hasNext() throws IOException {
        return this.mInputStream.available() > 0;
    }

    private byte getNextByte() throws IOException {
        return (byte) this.mInputStream.read();
    }
}