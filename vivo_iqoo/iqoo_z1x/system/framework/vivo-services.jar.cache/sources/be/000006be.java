package com.vivo.services.proxy;

import java.util.ArrayList;

/* loaded from: classes.dex */
public class RefValue {
    private int mRef = 0;
    private final ArrayList<String> mModules = new ArrayList<>();

    public boolean ref(String module) {
        if (!this.mModules.contains(module)) {
            this.mModules.add(module);
            this.mRef++;
            return true;
        }
        return false;
    }

    public boolean unref(String module) {
        if (this.mModules.contains(module)) {
            this.mModules.remove(module);
            this.mRef--;
            return true;
        }
        return false;
    }

    public int refCount() {
        return this.mRef;
    }

    public boolean contains(String module) {
        return this.mModules.contains(module);
    }

    public String toString() {
        return String.format("ref=%d module=%s", Integer.valueOf(this.mRef), this.mModules.toString());
    }
}