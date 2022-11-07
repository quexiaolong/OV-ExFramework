package com.android.server.media;

import android.os.ResultReceiver;
import android.view.KeyEvent;
import java.io.PrintWriter;

/* loaded from: classes.dex */
public interface MediaSessionRecordImpl extends AutoCloseable {
    void adjustVolume(String str, String str2, int i, int i2, boolean z, int i3, int i4, boolean z2);

    boolean checkPlaybackActiveState(boolean z);

    @Override // java.lang.AutoCloseable
    void close();

    void dump(PrintWriter printWriter, String str);

    String getPackageName();

    int getSessionPolicies();

    int getUid();

    int getUserId();

    boolean isActive();

    boolean isClosed();

    boolean isPlaybackTypeLocal();

    boolean isSystemPriority();

    boolean sendMediaButton(String str, int i, int i2, boolean z, KeyEvent keyEvent, int i3, ResultReceiver resultReceiver);

    void setSessionPolicies(int i);
}