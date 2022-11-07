package com.android.server.timezonedetector;

import com.android.internal.util.IndentingPrintWriter;
import java.util.ArrayDeque;
import java.util.Iterator;

/* loaded from: classes2.dex */
public final class ReferenceWithHistory<V> {
    private static final Object NULL_MARKER = "{null marker}";
    private final int mMaxHistorySize;
    private ArrayDeque<Object> mValues;

    public ReferenceWithHistory(int maxHistorySize) {
        if (maxHistorySize < 1) {
            throw new IllegalArgumentException("maxHistorySize < 1: " + maxHistorySize);
        }
        this.mMaxHistorySize = maxHistorySize;
    }

    public V get() {
        ArrayDeque<Object> arrayDeque = this.mValues;
        if (arrayDeque == null || arrayDeque.isEmpty()) {
            return null;
        }
        Object value = this.mValues.getFirst();
        return unpackNullIfRequired(value);
    }

    public V set(V newValue) {
        if (this.mValues == null) {
            this.mValues = new ArrayDeque<>(this.mMaxHistorySize);
        }
        if (this.mValues.size() >= this.mMaxHistorySize) {
            this.mValues.removeLast();
        }
        V previous = get();
        Object nullSafeValue = packNullIfRequired(newValue);
        this.mValues.addFirst(nullSafeValue);
        return previous;
    }

    public void dump(IndentingPrintWriter ipw) {
        ArrayDeque<Object> arrayDeque = this.mValues;
        if (arrayDeque == null) {
            ipw.println("{Empty}");
        } else {
            int i = 0;
            Iterator<Object> it = arrayDeque.iterator();
            while (it.hasNext()) {
                Object value = it.next();
                ipw.println(i + ": " + unpackNullIfRequired(value));
                i++;
            }
        }
        ipw.flush();
    }

    public int getHistoryCount() {
        ArrayDeque<Object> arrayDeque = this.mValues;
        if (arrayDeque == null) {
            return 0;
        }
        return arrayDeque.size();
    }

    public String toString() {
        return String.valueOf(get());
    }

    /* JADX WARN: Multi-variable type inference failed */
    private V unpackNullIfRequired(Object value) {
        if (value == NULL_MARKER) {
            return null;
        }
        return value;
    }

    private Object packNullIfRequired(V value) {
        return value == null ? NULL_MARKER : value;
    }
}