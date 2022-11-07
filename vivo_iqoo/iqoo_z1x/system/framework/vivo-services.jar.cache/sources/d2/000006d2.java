package com.vivo.services.rms;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class AppPreviewAdjuster {
    private static final String TAG = "AppPreviewManager";
    private JSONObject mPreviewJson = null;
    private static AppPreviewAdjuster INSTANCE = new AppPreviewAdjuster();
    private static final Object mLock = new Object();

    private AppPreviewAdjuster() {
    }

    public static AppPreviewAdjuster getInstance() {
        return INSTANCE;
    }

    public void setBundle(Bundle bundle) {
        synchronized (mLock) {
            try {
                this.mPreviewJson = new JSONObject(bundle.getString("key_preview_setting"));
            } catch (Exception e) {
                this.mPreviewJson = null;
            }
        }
    }

    public void adjustPreview(String packageName, View view, WindowManager.LayoutParams params) {
        synchronized (mLock) {
            try {
                if (this.mPreviewJson != null && this.mPreviewJson.has(packageName)) {
                    view.setBackgroundColor(this.mPreviewJson.getInt(packageName));
                    params.format = -1;
                }
            } catch (Exception e) {
            }
        }
    }
}