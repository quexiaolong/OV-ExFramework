package com.vivo.services.proxy.broadcast;

import android.util.Pair;
import java.util.ArrayList;
import java.util.Iterator;

/* loaded from: classes.dex */
public class PairList<K> extends ArrayList<Pair<K, K>> {
    private static final long serialVersionUID = 1;

    public void put(K k1, K k2) {
        add(Pair.create(k1, k2));
    }

    public void removeKeyorValue(K k) {
        if (k == null) {
            return;
        }
        Iterator<Pair<K, K>> it = iterator();
        while (it.hasNext()) {
            Pair<K, K> value = it.next();
            if (k.equals(value.first) || k.equals(value.second)) {
                it.remove();
            }
        }
    }

    public K getOther(K k) {
        Iterator<Pair<K, K>> it = iterator();
        while (it.hasNext()) {
            Pair<K, K> value = it.next();
            if (k.equals(value.first)) {
                return (K) value.second;
            }
            if (k.equals(value.second)) {
                return (K) value.first;
            }
        }
        return null;
    }

    @Override // java.util.AbstractCollection
    public String toString() {
        Iterator<Pair<K, K>> it = iterator();
        if (!it.hasNext()) {
            return "[]";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        while (it.hasNext()) {
            Pair<K, K> value = it.next();
            builder.append(value.first);
            builder.append("<->");
            builder.append(value.second);
            if (it.hasNext()) {
                builder.append(",");
            } else {
                builder.append("]");
            }
        }
        return builder.toString();
    }
}