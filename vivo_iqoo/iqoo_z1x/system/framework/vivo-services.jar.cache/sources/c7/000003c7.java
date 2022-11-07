package com.android.server.policy;

import android.view.Display;
import android.view.KeyEvent;
import com.android.server.wm.WindowState;

/* loaded from: classes.dex */
public interface IVivoAdjustmentPolicy {
    void doCustomKeyHandler(int i, int i2);

    Display getDisplay();

    WindowState getFocusedWindow();

    int getLastKeyCode();

    void handleMetaKeyEvent();

    boolean isAIKeyHandled();

    boolean isAIKeyTriggered();

    boolean isRightSideKeyTriggered();

    void keyguardDone(boolean z, boolean z2);

    boolean performHapticFeedback(int i, boolean z, boolean z2);

    void requestScreenShot();

    void sendMediaKeyEvent(KeyEvent keyEvent);
}