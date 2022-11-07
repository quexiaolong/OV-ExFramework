package com.android.server.policy.key;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.UserHandle;
import android.view.KeyEvent;
import com.android.server.policy.AVivoInterceptKeyCallback;
import com.vivo.common.utils.VLog;

/* loaded from: classes.dex */
public class VivoIMusicCollectKeyHandler extends AVivoInterceptKeyCallback {
    private static final int IMUSIC_COLLECT_LONG_PRESS_TIME_OUT = 500;
    private static final String TAG = "VivoIMusicCollectKeyHandler";
    private Context mContext;
    private Handler mHandler = new Handler();
    private boolean mImusicCollectKeyHandled = false;
    private Runnable mLongPressRunnable = new Runnable() { // from class: com.android.server.policy.key.VivoIMusicCollectKeyHandler.1
        @Override // java.lang.Runnable
        public void run() {
            VivoIMusicCollectKeyHandler.this.mImusicCollectKeyHandled = true;
            VivoIMusicCollectKeyHandler.this.startAgent();
        }
    };

    public VivoIMusicCollectKeyHandler(Context context) {
        this.mContext = context;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public boolean onCheckForward(int keyCode, KeyEvent event) {
        return false;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public int onKeyDown(int keyCode, KeyEvent event) {
        if (this.mState != 0) {
            return -100;
        }
        this.mImusicCollectKeyHandled = false;
        this.mHandler.removeCallbacks(this.mLongPressRunnable);
        this.mHandler.postDelayed(this.mLongPressRunnable, 500L);
        return 0;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public int onKeyUp(int keyCode, KeyEvent event) {
        if (this.mState != 0) {
            return -100;
        }
        this.mHandler.removeCallbacks(this.mLongPressRunnable);
        if (!this.mImusicCollectKeyHandled) {
            startBBKMusic(event);
            sendBroadcastToCamera(event);
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startAgent() {
        VLog.d(TAG, "startAgent");
        try {
            Intent intent = new Intent("vivo.intent.action.HEADSETHOOK_KEY_LONG_PRESS");
            intent.setPackage("com.vivo.agent");
            this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
        } catch (Exception e) {
            VLog.e(TAG, "startService error:" + e);
        }
    }

    private void startBBKMusic(KeyEvent keyEvent) {
        VLog.d(TAG, "startBBKMusic");
        try {
            Intent intent = new Intent("android.intent.action.MEDIA_BUTTON");
            intent.addFlags(268435456);
            intent.putExtra("android.intent.extra.KEY_EVENT", keyEvent);
            intent.setPackage("com.android.bbkmusic");
            this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
        } catch (Exception e) {
            VLog.e(TAG, "startService error:" + e);
        }
    }

    private void sendBroadcastToCamera(KeyEvent keyEvent) {
        try {
            Intent intent = new Intent("com.vivo.favorite.action.OPERATE_CAMERA");
            intent.addFlags(268435456);
            intent.putExtra("keycode", keyEvent.getKeyCode());
            intent.setPackage("com.android.camera");
            this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
        } catch (Exception e) {
            VLog.e(TAG, "send broadcast cause error:" + e);
        }
    }
}