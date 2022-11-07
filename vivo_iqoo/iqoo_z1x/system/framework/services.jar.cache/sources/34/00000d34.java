package com.android.server.display;

import android.hardware.display.DisplayManagerGlobal;
import android.view.DisplayInfo;

/* loaded from: classes.dex */
public class DisplayInfoProxy {
    private DisplayInfo mInfo;

    public DisplayInfoProxy(DisplayInfo info) {
        this.mInfo = info;
    }

    public void set(DisplayInfo info) {
        this.mInfo = info;
        DisplayManagerGlobal.invalidateLocalDisplayInfoCaches();
    }

    public DisplayInfo get() {
        return this.mInfo;
    }
}