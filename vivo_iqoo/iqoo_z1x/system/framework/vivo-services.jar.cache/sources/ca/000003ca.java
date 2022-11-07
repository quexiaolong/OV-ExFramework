package com.android.server.policy;

import android.view.KeyEvent;

/* loaded from: classes.dex */
public interface IVivoKeyCallback {
    void cancelPendingKeyAction();

    boolean onCheckDoubleClickEnabled(int i, KeyEvent keyEvent);

    boolean onCheckForward(int i, KeyEvent keyEvent);

    void onKeyDoubleClick(int i, KeyEvent keyEvent);

    int onKeyDown(int i, KeyEvent keyEvent);

    void onKeyLongPress(int i, KeyEvent keyEvent);

    int onKeyUp(int i, KeyEvent keyEvent);
}