package com.android.server.connectivity;

import java.util.concurrent.atomic.AtomicReference;

/* loaded from: classes.dex */
public class AutodestructReference<T> {
    private final AtomicReference<T> mHeld;

    public AutodestructReference(T obj) {
        if (obj == null) {
            throw new NullPointerException("Autodestruct reference to null");
        }
        this.mHeld = new AtomicReference<>(obj);
    }

    public T getAndDestroy() {
        T obj = this.mHeld.getAndSet(null);
        if (obj == null) {
            throw new NullPointerException("Already autodestructed");
        }
        return obj;
    }
}