package com.android.server.audio;

/* loaded from: classes.dex */
public interface PlayerFocusEnforcer {
    boolean duckPlayers(FocusRequester focusRequester, FocusRequester focusRequester2, boolean z);

    void mutePlayersForCall(int[] iArr);

    void unduckPlayers(FocusRequester focusRequester);

    void unmutePlayersForCall();
}