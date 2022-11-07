package com.android.server.wm;

import android.content.Context;
import android.os.SystemClock;
import android.widget.Toast;

/* loaded from: classes.dex */
public class ToastUtils {
    private static long lastToastTime = 0;
    private static final long minDelay = 2000;

    public static void show(Context context, int resourceId, String info) {
        long now = SystemClock.uptimeMillis();
        if (now - lastToastTime < minDelay) {
            return;
        }
        String msg = context.getResources().getString(resourceId, info);
        Toast.makeText(context, msg, 0).show();
        lastToastTime = SystemClock.uptimeMillis();
    }
}