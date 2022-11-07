package com.vivo.services.popupcamera;

import android.app.AlertDialog;
import android.content.Context;
import android.hardware.graphics.common.V1_0.Dataspace;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Button;
import vivo.util.VivoThemeUtil;

/* loaded from: classes.dex */
class BaseErrorDialog extends AlertDialog {
    private static final int DISABLE_BUTTONS = 1;
    private static final int ENABLE_BUTTONS = 0;
    private boolean mConsuming;
    private Handler mHandler;

    public BaseErrorDialog(Context context) {
        super(context, VivoThemeUtil.getSystemThemeStyle(VivoThemeUtil.ThemeType.DIALOG_ALERT));
        this.mConsuming = true;
        this.mHandler = new Handler() { // from class: com.vivo.services.popupcamera.BaseErrorDialog.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                if (msg.what == 0) {
                    BaseErrorDialog.this.mConsuming = false;
                    BaseErrorDialog.this.setEnabled(true);
                } else if (msg.what == 1) {
                    BaseErrorDialog.this.setEnabled(false);
                }
            }
        };
        getWindow().setType(2003);
        getWindow().setFlags(Dataspace.STANDARD_BT601_625, Dataspace.STANDARD_BT601_625);
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.setTitle("Error Dialog");
        getWindow().setAttributes(attrs);
    }

    @Override // android.app.Dialog
    public void onStart() {
        super.onStart();
    }

    @Override // android.app.Dialog, android.view.Window.Callback
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (this.mConsuming) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setEnabled(boolean enabled) {
        Button b = (Button) findViewById(16908313);
        if (b != null) {
            b.setEnabled(enabled);
        }
        Button b2 = (Button) findViewById(16908314);
        if (b2 != null) {
            b2.setEnabled(enabled);
        }
        Button b3 = (Button) findViewById(16908315);
        if (b3 != null) {
            b3.setEnabled(enabled);
        }
    }
}