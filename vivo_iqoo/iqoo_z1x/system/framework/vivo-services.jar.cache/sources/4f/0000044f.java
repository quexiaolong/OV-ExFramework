package com.android.server.policy.key;

import android.content.Context;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.view.KeyEvent;
import com.android.server.policy.AVivoInterceptKeyCallback;
import com.android.server.policy.IVivoAdjustmentPolicy;
import com.android.server.policy.VivoPolicyConstant;
import com.android.server.policy.VivoWMPHook;

/* loaded from: classes.dex */
public final class VivoCustomKeyHandler extends AVivoInterceptKeyCallback {
    private Context mContext;
    private IVivoAdjustmentPolicy mVivoPolicy;

    public VivoCustomKeyHandler(Context context, IVivoAdjustmentPolicy vivoPolicy) {
        this.mContext = context;
        this.mVivoPolicy = vivoPolicy;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public boolean onCheckForward(int keyCode, KeyEvent event) {
        int i = this.mState;
        return false;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public int onKeyDown(int keyCode, KeyEvent event) {
        if (this.mState == 0 && VivoPolicyConstant.KEYCODE_TS_LARGE_SUPPRESSION == keyCode) {
            silenceRinger();
        }
        return -100;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public int onKeyUp(int keyCode, KeyEvent event) {
        int i = this.mState;
        return -100;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public void onKeyLongPress(int keyCode, KeyEvent event) {
    }

    private void silenceRinger() {
        boolean silenceRingerEnabled = Settings.System.getInt(this.mContext.getContentResolver(), "bbk_cover_screen_mute_setting", 0) == 1;
        if (silenceRingerEnabled) {
            printf("silenceRinger");
            TelecomManager telecomManager = (TelecomManager) this.mContext.getSystemService("telecom");
            if (telecomManager != null && telecomManager.isRinging()) {
                telecomManager.silenceRinger();
            }
        }
    }

    private void printf(String msg) {
        VivoWMPHook.printf(msg);
    }
}