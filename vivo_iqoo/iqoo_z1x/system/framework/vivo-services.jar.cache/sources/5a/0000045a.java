package com.android.server.policy.key;

import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import com.android.server.policy.AVivoInterceptKeyCallback;
import com.android.server.policy.IVivoAdjustmentPolicy;
import com.android.server.policy.VivoWMPHook;

/* loaded from: classes.dex */
public final class VivoOTGKeyHandler extends AVivoInterceptKeyCallback {
    public static final String ACTION_PHONE_INSTRUCTION = "com.android.bbk_phoneInstructions";
    public static final String KEY_ACTIVITY_INPUT_METHOD = "com.android.settings.Settings$InputMethodAndLanguageSettingsActivity";
    public static final String KEY_PACKAGE_SETTINGS = "com.android.settings";
    private Context mContext;
    private IVivoAdjustmentPolicy mVivoPolicy;

    public VivoOTGKeyHandler(Context context, IVivoAdjustmentPolicy vivoPolicy) {
        this.mContext = context;
        this.mVivoPolicy = vivoPolicy;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public int onKeyDown(int keyCode, KeyEvent event) {
        if (this.mState != 0 || keyCode != 131) {
            return -100;
        }
        performF1KeyDownAction();
        return 0;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public int onKeyUp(int keyCode, KeyEvent event) {
        if (this.mState != 1) {
            return -100;
        }
        if (keyCode != 117 && keyCode != 118) {
            return -100;
        }
        this.mVivoPolicy.handleMetaKeyEvent();
        return -1;
    }

    private void performF1KeyDownAction() {
        printf("performF1KeyDownAction");
        Intent intent = new Intent(ACTION_PHONE_INSTRUCTION);
        intent.setFlags(268435456);
        try {
            this.mContext.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void printf(String msg) {
        VivoWMPHook.printf(msg);
    }
}