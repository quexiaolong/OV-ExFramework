package com.vivo.services.proxcali;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.WindowManager;

/* loaded from: classes.dex */
public final class SensorErrorDialog extends BaseSensorDialog {
    private static final int FORCE_CLOSE = 1;
    private static final String TAG = "SensorErrorDialog";
    private static final int WAIT = 2;
    private final Handler mHandler;

    @Override // com.vivo.services.proxcali.BaseSensorDialog, android.app.Dialog, android.view.Window.Callback
    public /* bridge */ /* synthetic */ boolean dispatchKeyEvent(KeyEvent keyEvent) {
        return super.dispatchKeyEvent(keyEvent);
    }

    @Override // com.vivo.services.proxcali.BaseSensorDialog, android.app.Dialog
    public /* bridge */ /* synthetic */ void onStart() {
        super.onStart();
    }

    public SensorErrorDialog(Context context, boolean aboveSystem, String message) {
        super(context);
        this.mHandler = new Handler() { // from class: com.vivo.services.proxcali.SensorErrorDialog.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                int i = msg.what;
                SensorErrorDialog.this.dismiss();
            }
        };
        Resources res = context.getResources();
        setCancelable(true);
        setMessage(message);
        setButton(-1, res.getText(17040302), this.mHandler.obtainMessage(1));
        if (aboveSystem) {
            getWindow().setType(2010);
        }
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.privateFlags = 256;
        getWindow().setAttributes(attrs);
    }

    @Override // android.app.AlertDialog, android.app.Dialog
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}