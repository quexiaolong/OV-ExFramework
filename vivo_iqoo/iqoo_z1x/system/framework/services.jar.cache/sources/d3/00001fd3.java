package com.google.android.startop.iorap;

/* loaded from: classes2.dex */
public class CheckHelpers {
    public static void checkTypeInRange(int type, int maxValue) {
        if (type < 0) {
            throw new IllegalArgumentException(String.format("type must be non-negative (value=%d)", Integer.valueOf(type)));
        }
        if (type > maxValue) {
            throw new IllegalArgumentException(String.format("type out of range (value=%d, max=%d)", Integer.valueOf(type), Integer.valueOf(maxValue)));
        }
    }

    public static void checkStateInRange(int state, int maxValue) {
        if (state < 0) {
            throw new IllegalArgumentException(String.format("state must be non-negative (value=%d)", Integer.valueOf(state)));
        }
        if (state > maxValue) {
            throw new IllegalArgumentException(String.format("state out of range (value=%d, max=%d)", Integer.valueOf(state), Integer.valueOf(maxValue)));
        }
    }
}