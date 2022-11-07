package com.android.server.display.color.displayenhance;

import java.io.PrintWriter;

/* loaded from: classes.dex */
public interface DisplayEnhanceListener {
    void dump(PrintWriter printWriter);

    void onForegroundActivityChanged(String str, int i);

    void onForegroundPackageChanged(String str);

    void onScreenStateChanged(int i);
}