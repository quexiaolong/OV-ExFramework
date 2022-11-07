package com.vivo.services.rms.sdk;

import android.util.SparseArray;
import java.util.Stack;

/* loaded from: classes.dex */
public final class IntArrayFactory {
    private static final int MAX_SIZE = 16;
    private static final SparseArray<Stack<int[]>> sCaches = new SparseArray<>(16);

    public static int[] create(int length) {
        synchronized (sCaches) {
            if (length <= 0) {
                return null;
            }
            Stack<int[]> stack = sCaches.get(length);
            if (stack == null) {
                stack = new Stack<>();
                sCaches.put(length, stack);
            }
            if (!stack.isEmpty()) {
                return stack.pop();
            }
            return new int[length];
        }
    }

    public static void recycle(int[] array) {
        synchronized (sCaches) {
            if (array == null) {
                return;
            }
            int length = array.length;
            Stack<int[]> stack = sCaches.get(length);
            if (stack == null) {
                stack = new Stack<>();
                sCaches.put(length, stack);
            }
            if (stack.size() < 16) {
                stack.push(array);
            }
        }
    }
}