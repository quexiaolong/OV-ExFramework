package com.android.server.integrity.model;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/* loaded from: classes.dex */
public class BitOutputStream {
    private static final int BUFFER_SIZE = 4096;
    private final byte[] mBuffer = new byte[4096];
    private int mNextBitIndex = 0;
    private final OutputStream mOutputStream;

    public BitOutputStream(OutputStream outputStream) {
        this.mOutputStream = outputStream;
    }

    public void setNext(int numOfBits, int value) throws IOException {
        if (numOfBits <= 0) {
            return;
        }
        int nextBitMask = 1 << (numOfBits - 1);
        while (true) {
            int numOfBits2 = numOfBits - 1;
            if (numOfBits > 0) {
                setNext((value & nextBitMask) != 0);
                nextBitMask >>>= 1;
                numOfBits = numOfBits2;
            } else {
                return;
            }
        }
    }

    public void setNext(boolean value) throws IOException {
        int byteToWrite = this.mNextBitIndex / 8;
        if (byteToWrite == 4096) {
            this.mOutputStream.write(this.mBuffer);
            reset();
            byteToWrite = 0;
        }
        if (value) {
            byte[] bArr = this.mBuffer;
            bArr[byteToWrite] = (byte) (bArr[byteToWrite] | (1 << (7 - (this.mNextBitIndex % 8))));
        }
        this.mNextBitIndex++;
    }

    public void setNext() throws IOException {
        setNext(true);
    }

    public void flush() throws IOException {
        int i = this.mNextBitIndex;
        int endByte = i / 8;
        if (i % 8 != 0) {
            endByte++;
        }
        this.mOutputStream.write(this.mBuffer, 0, endByte);
        reset();
    }

    private void reset() {
        this.mNextBitIndex = 0;
        Arrays.fill(this.mBuffer, (byte) 0);
    }
}