package com.android.server.power;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.provider.Settings;

/* loaded from: classes.dex */
public class FaceWakeHandler extends Handler {
    static final boolean DBG = false;
    static final int FACE_WAKE_ADVANCED_TIME = 9000;
    static final int FACE_WAKE_CONTINUED_TIME = 7000;
    static final int FACE_WAKE_LOCK_SCREEN_DELAY = 8000;
    static final int MSG_FACE_WAKE_BLOCK_CHANGED = 4;
    static final int MSG_FACE_WAKE_ENABLE_CHANGED = 3;
    static final int MSG_START_FACE_WAKE = 1;
    static final int MSG_STOP_FACE_WAKE = 2;
    private static final String TAG = "FaceWake";
    private boolean mBlock;
    private int mBlockCount;
    private Context mContext;
    private boolean mFaceWakeEnabled;
    private int mScreenOffTimeout;
    private SettingsObserver mSettingsObserver;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void log(String msg) {
    }

    /* loaded from: classes.dex */
    class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            FaceWakeHandler.log("observe....");
            ContentResolver cr = FaceWakeHandler.this.mContext.getContentResolver();
            cr.registerContentObserver(Settings.System.getUriFor("bbk_keep_screen_enable_setting"), false, this);
            cr.registerContentObserver(Settings.System.getUriFor("screen_off_timeout"), false, this);
            FaceWakeHandler.this.updateSettings();
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            FaceWakeHandler.log("onChange....");
            FaceWakeHandler.this.notifyChanged();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyChanged() {
        updateSettings();
        settingsChanged();
    }

    private void settingsChanged() {
        if (this.mFaceWakeEnabled) {
            handleFaceWake();
        } else {
            stopFaceWake();
        }
    }

    private void tellThreadSettings() {
        removeMessages(3);
        boolean z = this.mFaceWakeEnabled;
        sendMessage(obtainMessage(3, z ? 1 : 0, this.mScreenOffTimeout));
    }

    private void tellThreadBlock() {
        removeMessages(4);
        sendMessage(obtainMessage(4, this.mBlock ? 1 : 0, 0));
    }

    public FaceWakeHandler(Context context, Looper looper, Handler.Callback cb) {
        super(looper, cb);
        this.mFaceWakeEnabled = false;
        this.mBlockCount = 0;
        this.mBlock = false;
        this.mContext = context;
        post(new Runnable() { // from class: com.android.server.power.FaceWakeHandler.1
            @Override // java.lang.Runnable
            public void run() {
                FaceWakeHandler faceWakeHandler = FaceWakeHandler.this;
                FaceWakeHandler faceWakeHandler2 = FaceWakeHandler.this;
                faceWakeHandler.mSettingsObserver = new SettingsObserver(faceWakeHandler2);
                FaceWakeHandler.this.mSettingsObserver.observe();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateSettings() {
        log("updateSettings....");
        ContentResolver cr = this.mContext.getContentResolver();
        this.mFaceWakeEnabled = Settings.System.getInt(cr, "bbk_keep_screen_enable_setting", 0) == 1;
        this.mScreenOffTimeout = Settings.System.getInt(cr, "screen_off_timeout", 0);
        tellThreadSettings();
    }

    public void handleFaceWake() {
        if (this.mFaceWakeEnabled) {
            log("handleFaceWake....");
            stopFaceWake();
            startFaceWake();
        }
    }

    public void handleWakeLock(boolean Acquire, int flags) {
        if ((flags & 10) != 0 || (flags & 26) != 0) {
            blockFaceWake(Acquire);
        }
    }

    private void blockFaceWake(boolean block) {
        log("blockFaceWake block: " + block);
        if (block) {
            this.mBlockCount++;
        } else {
            this.mBlockCount--;
        }
        boolean tempBlock = this.mBlockCount != 0;
        if (this.mBlock != tempBlock) {
            this.mBlock = tempBlock;
            blockChanged();
        }
    }

    private void blockChanged() {
        tellThreadBlock();
        if (this.mBlock) {
            stopFaceWake();
        } else {
            handleFaceWake();
        }
    }

    public void stopFaceWake() {
        removeMessages(1);
        if (hasMessages(2)) {
            return;
        }
        sendMessage(obtainMessage(2));
    }

    public void startFaceWake() {
        if (this.mFaceWakeEnabled && !this.mBlock) {
            int delay = this.mScreenOffTimeout - 9000;
            if ("PD1401V".equals(SystemProperties.get("ro.product.model.bbk", "unknown"))) {
                delay -= 2000;
            }
            if ("PD1304CL".equals(SystemProperties.get("ro.product.model.bbk", "unknown"))) {
                delay -= 2000;
            }
            log("startFaceWake start delay: " + delay);
            removeMessages(1);
            sendMessageDelayed(obtainMessage(1), (long) delay);
        }
    }
}