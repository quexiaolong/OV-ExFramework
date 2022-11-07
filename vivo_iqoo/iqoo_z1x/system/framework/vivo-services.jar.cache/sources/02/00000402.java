package com.android.server.policy;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import com.vivo.common.utils.VLog;

/* loaded from: classes.dex */
public final class VivoPowerKeyOLPListener {
    private static final int OVER_LONG_PRESS_TIMEOUT = 8000;
    private static final String TAG = "VivoPowerKeyOLPHandler";
    private Context mContext;
    private Handler mHandler;
    private Runnable mRunable = new Runnable() { // from class: com.android.server.policy.VivoPowerKeyOLPListener.1
        @Override // java.lang.Runnable
        public void run() {
            VivoPowerKeyOLPListener.this.sendBroadcast();
        }
    };

    public VivoPowerKeyOLPListener(Context context, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
    }

    public void startObserve() {
        this.mHandler.removeCallbacks(this.mRunable);
        this.mHandler.postDelayed(this.mRunable, 8000L);
    }

    public void stopObserve() {
        this.mHandler.removeCallbacks(this.mRunable);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendBroadcast() {
        VLog.i(TAG, "sendBroadcast:vivo.intent.action.POWER_KEY_OVER_LONG_PRESS");
        Intent intent = new Intent(VivoPolicyConstant.ACTION_POWER_KEY_OVER_LONG_PRESS);
        this.mContext.sendBroadcast(intent);
    }
}