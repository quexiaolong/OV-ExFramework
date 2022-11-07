package com.android.server;

import android.common.OplusFeatureList;
import android.content.Context;
import android.util.Slog;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.IOplusActivityManagerServiceEx;
import com.android.server.display.DisplayManagerService;
import com.android.server.display.IOplusDisplayManagerServiceEx;
import com.android.server.display.IOplusEyeProtectManager;
import com.android.server.inputmethod.InputMethodManagerService;

/* loaded from: classes.dex */
public class OplusServiceFactory extends OplusCommonServiceFactory {
    private static final String CLASSNAME2 = "com.android.server.OplusServiceFactoryImpl";
    private static final String TAG = "OplusServiceFactory";
    private static OplusServiceFactory sInstance;

    public static OplusServiceFactory getInstance() {
        if (sInstance == null) {
            synchronized (OplusServiceFactory.class) {
                try {
                    if (sInstance == null) {
                        sInstance = (OplusServiceFactory) newInstance(CLASSNAME2);
                    }
                } catch (Exception e) {
                    Slog.e(TAG, "WindowManagerService Reflect exception getInstance: " + e.toString());
                    if (sInstance == null) {
                        sInstance = new OplusServiceFactory();
                    }
                }
            }
        }
        return sInstance;
    }

    public boolean isValid(int index) {
        boolean validOplus = index < OplusFeatureList.OplusIndex.EndOplusServiceFactory.ordinal() && index > OplusFeatureList.OplusIndex.StartOplusServiceFactory.ordinal();
        boolean vaildOplusOs = index < OplusFeatureList.OplusIndex.EndOplusOsServiceFactory.ordinal() && index > OplusFeatureList.OplusIndex.StartOplusOsServiceFactory.ordinal();
        return vaildOplusOs || validOplus;
    }

    public int getColorSystemThemeEx(int theme) {
        warn("getColorSystemThemeEx dummy");
        return theme;
    }

    public InputMethodManagerService getOplusInputMethodManagerService(Context context) {
        warn("getInputMethodManagerService");
        return new InputMethodManagerService(context);
    }

    public IOplusEyeProtectManager getOplusEyeProtectManager() {
        return IOplusEyeProtectManager.DEFAULT;
    }

    public IOplusDisplayManagerServiceEx getColorDisplayManagerServiceEx(Context context, DisplayManagerService dms) {
        warn("getColorDisplayManagerServiceEx dummy");
        return IOplusDisplayManagerServiceEx.DEFAULT;
    }

    public IOplusActivityManagerServiceEx getOplusActivityManagerServiceEx(Context context, ActivityManagerService ams) {
        warn("getOplusActivityManagerServiceEx dummy");
        return IOplusActivityManagerServiceEx.DEFAULT;
    }
}