package com.android.server.am;

import android.content.Context;
import android.provider.Settings;
import vivo.content.res.VivoGlobalThemeManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoUserControllerImpl implements IVivoUserController {
    static final String TAG = "VivoUserControllerImpl";

    public void startUserInForeground(Context context, int targetUserId) {
        try {
            String theme_id = Settings.System.getStringForUser(context.getContentResolver(), "theme_id", targetUserId);
            if (theme_id == null) {
                theme_id = "2";
            }
            VivoGlobalThemeManager.updateThemeConfiguration(Integer.parseInt(theme_id));
        } catch (Exception e) {
            VSlog.e(TAG, "MultiUser updateThemeConfiguration Exception");
        }
    }
}