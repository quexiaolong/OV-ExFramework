package com.android.server.notification;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.service.notification.ZenModeConfig;
import android.util.SparseArray;
import java.io.PrintWriter;

/* loaded from: classes.dex */
public interface IVivoZenModeHelper {
    void dump(PrintWriter printWriter, String str);

    void evaluateZenMode(String str, boolean z, int i, int i2);

    boolean hasCtsVInstall();

    boolean isAlarmAndMusicAffectedByRingerMode();

    void observe(ContentResolver contentResolver, ContentObserver contentObserver);

    void onUserSwitched();

    void readXmlForZenModeConfigUpgrade(ZenModeConfig zenModeConfig, ZenModeConfig zenModeConfig2);

    void resetOsFlag();

    boolean shouldIntercept();

    boolean shouldInterceptByGameMode();

    void update(Uri uri, ZenModeConfig zenModeConfig, SparseArray<ZenModeConfig> sparseArray);

    void updateCtsVInstallState(boolean z);

    /* loaded from: classes.dex */
    public interface IVivoZenModeHelperExport {
        IVivoZenModeHelper getVivoInjectInstance();

        default void resetOsFlag() {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().resetOsFlag();
            }
        }
    }
}