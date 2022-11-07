package com.android.server.integrity.parser;

import android.content.integrity.IntegrityUtils;
import com.android.server.integrity.model.BitInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/* loaded from: classes.dex */
public class BinaryFileOperations {
    public static String getStringValue(BitInputStream bitInputStream) throws IOException {
        boolean isHashedValue = bitInputStream.getNext(1) == 1;
        int valueSize = bitInputStream.getNext(8);
        return getStringValue(bitInputStream, valueSize, isHashedValue);
    }

    public static String getStringValue(BitInputStream bitInputStream, int valueSize, boolean isHashedValue) throws IOException {
        if (!isHashedValue) {
            StringBuilder value = new StringBuilder();
            while (true) {
                int valueSize2 = valueSize - 1;
                if (valueSize > 0) {
                    value.append((char) bitInputStream.getNext(8));
                    valueSize = valueSize2;
                } else {
                    return value.toString();
                }
            }
        } else {
            ByteBuffer byteBuffer = ByteBuffer.allocate(valueSize);
            while (true) {
                int valueSize3 = valueSize - 1;
                if (valueSize > 0) {
                    byteBuffer.put((byte) (bitInputStream.getNext(8) & 255));
                    valueSize = valueSize3;
                } else {
                    return IntegrityUtils.getHexDigest(byteBuffer.array());
                }
            }
        }
    }

    public static int getIntValue(BitInputStream bitInputStream) throws IOException {
        return bitInputStream.getNext(32);
    }

    public static boolean getBooleanValue(BitInputStream bitInputStream) throws IOException {
        return bitInputStream.getNext(1) == 1;
    }
}