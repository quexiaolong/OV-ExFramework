package com.android.server.policy.key;

import android.content.Context;
import android.media.AudioManager;
import android.view.KeyEvent;
import com.android.server.policy.AVivoInterceptKeyCallback;
import com.android.server.policy.IVivoAdjustmentPolicy;
import com.android.server.policy.VivoWMPHook;
import com.vivo.common.utils.VLog;

/* loaded from: classes.dex */
public final class VivoVolumeKeyHandler extends AVivoInterceptKeyCallback {
    private static byte[] mLock = new byte[0];
    private Context mContext;
    private IVivoAdjustmentPolicy mVivoPolicy;
    private boolean mVolumeDownKeyLongPressConsumed;
    private boolean mVolumeDownKeyTriggered;
    private boolean mVolumeUpKeyLongPressConsumed;
    private boolean mVolumeUpKeyTriggered;

    public VivoVolumeKeyHandler(Context context, IVivoAdjustmentPolicy vivoPolicy) {
        this.mContext = context;
        this.mVivoPolicy = vivoPolicy;
    }

    public void resetState() {
        this.mVolumeDownKeyTriggered = false;
        this.mVolumeUpKeyTriggered = false;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public boolean onCheckForward(int keyCode, KeyEvent event) {
        return true;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback
    public boolean onCheckNeedWakeLockWhenScreenOff(int keyCode, KeyEvent event) {
        return true;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public int onKeyDown(int keyCode, KeyEvent event) {
        int result = -100;
        synchronized (mLock) {
            int i = this.mState;
            if (i != 0) {
                if (i == 1) {
                    if (keyCode == 25) {
                        if (this.mVolumeDownKeyTriggered && !this.mVolumeDownKeyLongPressConsumed) {
                            result = 50;
                        }
                        if (this.mVolumeDownKeyLongPressConsumed) {
                            result = -1;
                        }
                    } else if (keyCode == 24) {
                        if (this.mVolumeUpKeyTriggered && !this.mVolumeUpKeyLongPressConsumed) {
                            result = 50;
                        }
                        if (this.mVolumeUpKeyLongPressConsumed) {
                            result = -1;
                        }
                    }
                }
            } else if (keyCode == 25) {
                if (!this.mVolumeDownKeyTriggered) {
                    this.mVolumeDownKeyTriggered = true;
                    this.mVolumeDownKeyLongPressConsumed = false;
                }
            } else if (keyCode == 24 && !this.mVolumeUpKeyTriggered) {
                this.mVolumeUpKeyTriggered = true;
                this.mVolumeUpKeyLongPressConsumed = false;
            }
        }
        printf("VivoVolumeKeyHandler::onKeyDown will return=" + result);
        return result;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public int onKeyUp(int keyCode, KeyEvent event) {
        int result = -100;
        synchronized (mLock) {
            int i = this.mState;
            if (i != 0) {
                if (i == 1) {
                    if (keyCode == 25) {
                        if (this.mVolumeDownKeyLongPressConsumed) {
                            this.mVolumeDownKeyLongPressConsumed = false;
                            result = -1;
                        }
                    } else if (keyCode == 24 && this.mVolumeUpKeyLongPressConsumed) {
                        this.mVolumeUpKeyLongPressConsumed = false;
                        result = -1;
                    }
                }
            } else if (keyCode == 25) {
                this.mVolumeDownKeyTriggered = false;
                if (this.mVolumeDownKeyLongPressConsumed) {
                    result = 1073741824;
                }
            } else if (keyCode == 24) {
                this.mVolumeUpKeyTriggered = false;
                if (this.mVolumeUpKeyLongPressConsumed) {
                    result = 1073741824;
                }
            }
        }
        printf("VivoVolumeKeyHandler::onKeyUp will return=" + result);
        return result;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public void onKeyLongPress(int keyCode, KeyEvent event) {
        synchronized (mLock) {
            if (keyCode != 24) {
                if (keyCode == 25 && this.mVolumeDownKeyTriggered) {
                    printf("KeyEvent.KEYCODE_MEDIA_NEXT will be simulated sent.");
                    this.mVolumeDownKeyLongPressConsumed = true;
                    simulateMediaKeyEvent(87);
                }
            } else if (this.mVolumeUpKeyTriggered) {
                printf("KeyEvent.KEYCODE_MEDIA_PREVIOUS will be simulated sent.");
                this.mVolumeUpKeyLongPressConsumed = true;
                simulateMediaKeyEvent(88);
            }
        }
    }

    private void performHapticFeedback() {
        this.mVivoPolicy.performHapticFeedback(0, false, true);
    }

    private void simulateMediaKeyEvent(int keyCode) {
        this.mVivoPolicy.sendMediaKeyEvent(new KeyEvent(0, keyCode));
        this.mVivoPolicy.sendMediaKeyEvent(new KeyEvent(1, keyCode));
    }

    private boolean isMusicActive() {
        AudioManager am = (AudioManager) this.mContext.getSystemService("audio");
        if (am != null) {
            return am.isMusicActive() || am.isMusicActiveRemotely();
        }
        VLog.w(VivoWMPHook.TAG, "isMusicActive: couldn't get AudioManager reference");
        return false;
    }

    private boolean isMusicActiveExt() {
        return false;
    }

    private void printf(String msg) {
        VivoWMPHook.printf(msg);
    }
}