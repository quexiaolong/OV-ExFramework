package com.android.server.clipboard;

import android.content.ClipData;
import android.content.Context;
import android.os.IBinder;
import vivo.app.clipboard.IClipboardDialogListener;

/* loaded from: classes.dex */
public interface IVivoClipboardService {
    boolean checkPolicyPermisson(int i);

    void clipboardInit(Context context, int i);

    String getPackageName();

    void hideClipboardDialog();

    boolean isClipboardDialogShowing();

    void setClipboardListener(IClipboardDialogListener iClipboardDialogListener);

    void setPackageName(String str);

    void setPrimaryClip(ClipData clipData, String str);

    boolean shouldShowClipboardDialog(int i);

    void showClipboardDialog(IBinder iBinder);

    void writeClipdataToFile();

    void writeDataToSetting(String str, String str2);

    void writePropertyToSetting();
}