package com.android.server.utils.quota;

import android.util.proto.ProtoOutputStream;

/* loaded from: classes2.dex */
public final class Category {
    public static final Category SINGLE_CATEGORY = new Category("SINGLE");
    private final int mHash;
    private final String mName;

    public Category(String name) {
        this.mName = name;
        this.mHash = name.hashCode();
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof Category) {
            return this.mName.equals(((Category) other).mName);
        }
        return false;
    }

    public int hashCode() {
        return this.mHash;
    }

    public String toString() {
        return "Category{" + this.mName + "}";
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(1138166333441L, this.mName);
        proto.end(token);
    }
}