package com.android.server.wm;

import java.util.ArrayList;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class WindowList<E> extends ArrayList<E> {
    /* JADX INFO: Access modifiers changed from: package-private */
    public void addFirst(E e) {
        add(0, e);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public E peekLast() {
        if (size() > 0) {
            return get(size() - 1);
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public E peekFirst() {
        if (size() > 0) {
            return get(0);
        }
        return null;
    }
}