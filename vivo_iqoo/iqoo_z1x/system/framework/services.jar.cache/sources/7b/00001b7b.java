package com.android.server.utils.quota;

import android.util.ArrayMap;
import android.util.SparseArrayMap;
import java.util.function.Consumer;
import java.util.function.Function;

/* loaded from: classes2.dex */
public class UptcMap<T> {
    private final SparseArrayMap<ArrayMap<String, T>> mData = new SparseArrayMap<>();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public interface UptcDataConsumer<D> {
        void accept(int i, String str, String str2, D d);
    }

    public void add(int userId, String packageName, String tag, T obj) {
        ArrayMap<String, T> data = (ArrayMap) this.mData.get(userId, packageName);
        if (data == null) {
            data = new ArrayMap<>();
            this.mData.add(userId, packageName, data);
        }
        data.put(tag, obj);
    }

    public void clear() {
        this.mData.clear();
    }

    public boolean contains(int userId, String packageName) {
        return this.mData.contains(userId, packageName);
    }

    public boolean contains(int userId, String packageName, String tag) {
        ArrayMap<String, T> data = (ArrayMap) this.mData.get(userId, packageName);
        return data != null && data.containsKey(tag);
    }

    public void delete(int userId) {
        this.mData.delete(userId);
    }

    public void delete(int userId, String packageName, String tag) {
        ArrayMap<String, T> data = (ArrayMap) this.mData.get(userId, packageName);
        if (data != null) {
            data.remove(tag);
            if (data.size() == 0) {
                this.mData.delete(userId, packageName);
            }
        }
    }

    public ArrayMap<String, T> delete(int userId, String packageName) {
        return (ArrayMap) this.mData.delete(userId, packageName);
    }

    public ArrayMap<String, T> get(int userId, String packageName) {
        return (ArrayMap) this.mData.get(userId, packageName);
    }

    public T get(int userId, String packageName, String tag) {
        ArrayMap<String, T> data = (ArrayMap) this.mData.get(userId, packageName);
        if (data != null) {
            return data.get(tag);
        }
        return null;
    }

    public T getOrCreate(int userId, String packageName, String tag, Function<Void, T> creator) {
        ArrayMap<String, T> data = (ArrayMap) this.mData.get(userId, packageName);
        if (data == null || !data.containsKey(tag)) {
            T val = creator.apply(null);
            add(userId, packageName, tag, val);
            return val;
        }
        return data.get(tag);
    }

    private int getUserIdAtIndex(int index) {
        return this.mData.keyAt(index);
    }

    private String getPackageNameAtIndex(int userIndex, int packageIndex) {
        return this.mData.keyAt(userIndex, packageIndex);
    }

    private String getTagAtIndex(int userIndex, int packageIndex, int tagIndex) {
        return (String) ((ArrayMap) this.mData.valueAt(userIndex, packageIndex)).keyAt(tagIndex);
    }

    public int userCount() {
        return this.mData.numMaps();
    }

    public int packageCountForUser(int userId) {
        return this.mData.numElementsForKey(userId);
    }

    public int tagCountForUserAndPackage(int userId, String packageName) {
        ArrayMap data = (ArrayMap) this.mData.get(userId, packageName);
        if (data != null) {
            return data.size();
        }
        return 0;
    }

    public T valueAt(int userIndex, int packageIndex, int tagIndex) {
        ArrayMap<String, T> data = (ArrayMap) this.mData.valueAt(userIndex, packageIndex);
        if (data != null) {
            return data.valueAt(tagIndex);
        }
        return null;
    }

    public void forEach(final Consumer<T> consumer) {
        this.mData.forEach(new Consumer() { // from class: com.android.server.utils.quota.-$$Lambda$UptcMap$VIYMMrjbnqShO606s52uuyAgdlU
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                UptcMap.lambda$forEach$0(consumer, (ArrayMap) obj);
            }
        });
    }

    /* JADX WARN: Multi-variable type inference failed */
    public static /* synthetic */ void lambda$forEach$0(Consumer consumer, ArrayMap tagMap) {
        for (int i = tagMap.size() - 1; i >= 0; i--) {
            consumer.accept(tagMap.valueAt(i));
        }
    }

    public void forEach(UptcDataConsumer<T> consumer) {
        int uCount = userCount();
        for (int u = 0; u < uCount; u++) {
            int userId = getUserIdAtIndex(u);
            int pkgCount = packageCountForUser(userId);
            for (int p = 0; p < pkgCount; p++) {
                String pkgName = getPackageNameAtIndex(u, p);
                int tagCount = tagCountForUserAndPackage(userId, pkgName);
                for (int t = 0; t < tagCount; t++) {
                    String tag = getTagAtIndex(u, p, t);
                    consumer.accept(userId, pkgName, tag, get(userId, pkgName, tag));
                }
            }
        }
    }
}