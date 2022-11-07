package com.android.server;

import android.app.ActivityThread;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Slog;

/* loaded from: classes.dex */
public class WallpaperUpdateReceiver extends BroadcastReceiver {
    private static final boolean DEBUG = false;
    private static final String TAG = "WallpaperUpdateReceiver";

    public static /* synthetic */ void lambda$U0nVive5QwEBqcnNmDq5uiouKcg(WallpaperUpdateReceiver wallpaperUpdateReceiver) {
        wallpaperUpdateReceiver.updateWallpaper();
    }

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        if (intent != null && "android.intent.action.DEVICE_CUSTOMIZATION_READY".equals(intent.getAction())) {
            AsyncTask.execute(new Runnable() { // from class: com.android.server.-$$Lambda$WallpaperUpdateReceiver$U0nVive5QwEBqcnNmDq5uiouKcg
                @Override // java.lang.Runnable
                public final void run() {
                    WallpaperUpdateReceiver.lambda$U0nVive5QwEBqcnNmDq5uiouKcg(WallpaperUpdateReceiver.this);
                }
            });
        }
    }

    public void updateWallpaper() {
        try {
            ActivityThread currentActivityThread = ActivityThread.currentActivityThread();
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(currentActivityThread.getSystemUiContext());
            Bitmap blank = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8);
            wallpaperManager.setBitmap(blank);
            wallpaperManager.setResource(17302154);
        } catch (Exception e) {
            Slog.w(TAG, "Failed to customize system wallpaper." + e);
        }
    }
}