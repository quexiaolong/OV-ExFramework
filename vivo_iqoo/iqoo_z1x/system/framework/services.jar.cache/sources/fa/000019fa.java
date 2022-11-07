package com.android.server.timezonedetector;

import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.util.IndentingPrintWriter;

/* loaded from: classes2.dex */
public final class ArrayMapWithHistory<K, V> {
    private static final String TAG = "ArrayMapWithHistory";
    private ArrayMap<K, ReferenceWithHistory<V>> mMap;
    private final int mMaxHistorySize;

    public ArrayMapWithHistory(int maxHistorySize) {
        if (maxHistorySize < 1) {
            throw new IllegalArgumentException("maxHistorySize < 1: " + maxHistorySize);
        }
        this.mMaxHistorySize = maxHistorySize;
    }

    public V put(K key, V value) {
        if (this.mMap == null) {
            this.mMap = new ArrayMap<>();
        }
        ReferenceWithHistory<V> valueHolder = this.mMap.get(key);
        if (valueHolder == null) {
            valueHolder = new ReferenceWithHistory<>(this.mMaxHistorySize);
            this.mMap.put(key, valueHolder);
        } else if (valueHolder.getHistoryCount() == 0) {
            Log.w(TAG, "History for \"" + key + "\" was unexpectedly empty");
        }
        return valueHolder.set(value);
    }

    public V get(Object key) {
        ReferenceWithHistory<V> valueHolder;
        ArrayMap<K, ReferenceWithHistory<V>> arrayMap = this.mMap;
        if (arrayMap == null || (valueHolder = arrayMap.get(key)) == null) {
            return null;
        }
        if (valueHolder.getHistoryCount() == 0) {
            Log.w(TAG, "History for \"" + key + "\" was unexpectedly empty");
        }
        return valueHolder.get();
    }

    public int size() {
        ArrayMap<K, ReferenceWithHistory<V>> arrayMap = this.mMap;
        if (arrayMap == null) {
            return 0;
        }
        return arrayMap.size();
    }

    public K keyAt(int index) {
        ArrayMap<K, ReferenceWithHistory<V>> arrayMap = this.mMap;
        if (arrayMap == null) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return arrayMap.keyAt(index);
    }

    public V valueAt(int index) {
        ArrayMap<K, ReferenceWithHistory<V>> arrayMap = this.mMap;
        if (arrayMap == null) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        ReferenceWithHistory<V> valueHolder = arrayMap.valueAt(index);
        if (valueHolder == null || valueHolder.getHistoryCount() == 0) {
            Log.w(TAG, "valueAt(" + index + ") was unexpectedly null or empty");
            return null;
        }
        return valueHolder.get();
    }

    public void dump(IndentingPrintWriter ipw) {
        if (this.mMap == null) {
            ipw.println("{Empty}");
        } else {
            for (int i = 0; i < this.mMap.size(); i++) {
                ipw.println("key idx: " + i + "=" + this.mMap.keyAt(i));
                ReferenceWithHistory<V> value = this.mMap.valueAt(i);
                ipw.println("val idx: " + i + "=" + value);
                ipw.increaseIndent();
                ipw.println("Historic values=[");
                ipw.increaseIndent();
                value.dump(ipw);
                ipw.decreaseIndent();
                ipw.println("]");
                ipw.decreaseIndent();
            }
        }
        ipw.flush();
    }

    public int getHistoryCountForKeyForTests(K key) {
        ReferenceWithHistory<V> valueHolder;
        ArrayMap<K, ReferenceWithHistory<V>> arrayMap = this.mMap;
        if (arrayMap == null || (valueHolder = arrayMap.get(key)) == null) {
            return 0;
        }
        if (valueHolder.getHistoryCount() == 0) {
            Log.w(TAG, "getValuesSizeForKeyForTests(\"" + key + "\") was unexpectedly empty");
            return 0;
        }
        return valueHolder.getHistoryCount();
    }

    public String toString() {
        return "ArrayMapWithHistory{mHistorySize=" + this.mMaxHistorySize + ", mMap=" + this.mMap + '}';
    }
}