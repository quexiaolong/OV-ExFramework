package com.android.server.textclassifier;

import com.android.internal.util.Preconditions;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;

/* loaded from: classes2.dex */
public final class FixedSizeQueue<E> {
    private final Queue<E> mDelegate;
    private final int mMaxSize;
    private final OnEntryEvictedListener<E> mOnEntryEvictedListener;

    /* loaded from: classes2.dex */
    public interface OnEntryEvictedListener<E> {
        void onEntryEvicted(E e);
    }

    public FixedSizeQueue(int maxSize, OnEntryEvictedListener<E> onEntryEvictedListener) {
        Preconditions.checkArgument(maxSize > 0, "maxSize (%s) must > 0", new Object[]{Integer.valueOf(maxSize)});
        this.mDelegate = new ArrayDeque(maxSize);
        this.mMaxSize = maxSize;
        this.mOnEntryEvictedListener = onEntryEvictedListener;
    }

    public int size() {
        return this.mDelegate.size();
    }

    public boolean add(E element) {
        Objects.requireNonNull(element);
        if (size() == this.mMaxSize) {
            E removed = this.mDelegate.remove();
            OnEntryEvictedListener<E> onEntryEvictedListener = this.mOnEntryEvictedListener;
            if (onEntryEvictedListener != null) {
                onEntryEvictedListener.onEntryEvicted(removed);
            }
        }
        this.mDelegate.add(element);
        return true;
    }

    public E poll() {
        return this.mDelegate.poll();
    }

    public boolean remove(E element) {
        Objects.requireNonNull(element);
        return this.mDelegate.remove(element);
    }

    public boolean isEmpty() {
        return this.mDelegate.isEmpty();
    }
}