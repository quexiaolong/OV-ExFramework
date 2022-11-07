package com.vivo.services.rms.sdk;

import com.vivo.common.utils.VLog;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;

/* loaded from: classes.dex */
public final class ObjectCache<T> {
    private static final String TAG = "ObjectCache";
    private Stack<T> mCache = new Stack<>();
    private final int mCapacity;
    private final Class<? extends T> mClass;

    public ObjectCache(Class<? extends T> clazz, int capacity) {
        this.mClass = clazz;
        this.mCapacity = capacity;
    }

    public T pop() {
        synchronized (this) {
            if (!this.mCache.isEmpty()) {
                return this.mCache.pop();
            }
            try {
                T o = this.mClass.newInstance();
                return o;
            } catch (Exception e) {
                VLog.e(TAG, "instance : " + e);
                return null;
            }
        }
    }

    public void put(T cache) {
        synchronized (this) {
            if (cache == null) {
                return;
            }
            for (int i = this.mCache.size() - 1; i >= 0; i--) {
                if (this.mCache.get(i) == cache) {
                    return;
                }
            }
            if (this.mCache.size() < this.mCapacity) {
                this.mCache.push(cache);
            }
        }
    }

    public void put(ArrayList<T> caches) {
        synchronized (this) {
            Iterator<T> it = caches.iterator();
            while (it.hasNext()) {
                T cache = it.next();
                if (this.mCache.size() >= this.mCapacity) {
                    break;
                }
                put((ObjectCache<T>) cache);
            }
        }
    }

    public void clear() {
        synchronized (this) {
            this.mCache.clear();
        }
    }
}