package com.android.server.statusbar;

import android.app.ITransientNotificationCallback;
import android.os.Bundle;
import android.os.IBinder;
import com.android.internal.view.AppearanceRegion;
import com.android.server.notification.NotificationDelegate;

/* loaded from: classes2.dex */
public interface StatusBarManagerInternal {
    void abortTransient(int i, int[] iArr);

    void appTransitionCancelled(int i);

    void appTransitionFinished(int i);

    void appTransitionPending(int i);

    void appTransitionStarting(int i, long j, long j2);

    void cancelPreloadRecentApps();

    void dismissKeyboardShortcutsMenu();

    void hideRecentApps(boolean z, boolean z2);

    void hideToast(String str, IBinder iBinder);

    void onCameraLaunchGestureDetected(int i);

    void onDisplayReady(int i);

    void onProposedRotationChanged(int i, boolean z);

    void onRecentsAnimationStateChanged(boolean z);

    void onSystemBarAppearanceChanged(int i, int i2, AppearanceRegion[] appearanceRegionArr, boolean z);

    void preloadRecentApps();

    void setCurrentUser(int i);

    void setDisableFlags(int i, int i2, String str);

    void setNotificationDelegate(NotificationDelegate notificationDelegate);

    void setTopAppHidesStatusBar(boolean z);

    void setWindowState(int i, int i2, int i3);

    void showAssistDisclosure();

    void showChargingAnimation(int i);

    void showPictureInPictureMenu();

    void showRecentApps(boolean z);

    void showScreenPinningRequest(int i);

    boolean showShutdownUi(boolean z, String str);

    void showToast(int i, String str, IBinder iBinder, CharSequence charSequence, IBinder iBinder2, int i2, ITransientNotificationCallback iTransientNotificationCallback);

    void showTransient(int i, int[] iArr);

    void startAssist(Bundle bundle);

    void toggleKeyboardShortcutsMenu(int i);

    void toggleRecentApps();

    void toggleSplitScreen();

    void topAppWindowChanged(int i, boolean z, boolean z2);
}