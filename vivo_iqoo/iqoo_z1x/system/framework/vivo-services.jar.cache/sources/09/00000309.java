package com.android.server.media;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.view.KeyEvent;
import com.android.server.am.VivoFrozenPackageSupervisor;
import com.android.server.am.firewall.VivoFirewall;
import com.android.server.media.MediaSessionService;
import com.vivo.common.utils.VLog;

/* loaded from: classes.dex */
public class VivoMediaSessionServiceImpl implements IVivoMediaSessionService {
    static final int HEADSET_LPRESS_DEFAULT = 0;
    static final int HEADSET_LPRESS_VIVOICE = 1;
    static final String TAG = "VivoMediaSessionServiceImpl";
    Context mContext;
    private int mHeadsetLpressMode;
    private KeyguardManager mKeyguardManager;
    private long[] mLongPressVibePattern;
    private final PowerManager.WakeLock mMediaEventWakeLock;
    protected MediaSessionService mMss;
    private Vibrator mVibrator;

    private long[] getLongIntArray(Resources r, int resid) {
        int[] ar = r.getIntArray(resid);
        if (ar == null) {
            return null;
        }
        long[] out = new long[ar.length];
        for (int i = 0; i < ar.length; i++) {
            out[i] = ar[i];
        }
        return out;
    }

    public VivoMediaSessionServiceImpl(MediaSessionService mss, Context context) {
        this.mMss = null;
        this.mMss = mss;
        this.mContext = context;
        this.mMediaEventWakeLock = mss.getMediaEventWakeLockFromMediaSessionService();
        this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
        if ("yes".equals(SystemProperties.get("ro.vivo.product.overseas", "no")) || "CMCC".equals(SystemProperties.get("ro.vivo.op.entry", "no"))) {
            this.mHeadsetLpressMode = 0;
        } else {
            this.mHeadsetLpressMode = 1;
        }
        this.mVibrator = (Vibrator) context.getSystemService("vibrator");
        this.mLongPressVibePattern = getLongIntArray(context.getResources(), 17236049);
    }

    public void dummy() {
    }

    public void handleVoiceKeyVivoCust(MediaSessionService.SessionManagerImpl smi, boolean needWakeLock) {
        int i = this.mHeadsetLpressMode;
        if (i == 0) {
            smi.startVoiceInput(needWakeLock);
        } else if (i == 1) {
            if (smartVoiceIsInstalled()) {
                VLog.d(TAG, "startAgent:");
                startAgent(needWakeLock);
            } else if (isVoiceAssitantEnabled()) {
                VLog.d(TAG, "startVivoice:");
                startVivoice(needWakeLock);
            } else if (!isVoiceAssitantRemind()) {
                VLog.d(TAG, "startVivoiceAssitantRemind:");
                startVivoiceAssitantRemind(needWakeLock);
            }
        }
    }

    private boolean isVoiceAssitantEnabled() {
        int val = Settings.Global.getInt(this.mContext.getContentResolver(), "com.bbk.VoiceAssistant.SWITCH", -1);
        VLog.d(TAG, "isVoiceAssitantEnabled:" + val);
        return val == 1;
    }

    private boolean smartVoiceIsInstalled() {
        return Settings.System.getInt(this.mContext.getContentResolver(), "vivo_jovi_smart_voice_installed", -1) == 1;
    }

    private boolean isVoiceAssitantRemind() {
        int val = Settings.Global.getInt(this.mContext.getContentResolver(), "com.bbk.VoiceAssistant.TIP", -1);
        VLog.d(TAG, "isVoiceAssitantRemind :" + val);
        return val == 1;
    }

    private boolean isSuperPowerSaverMode() {
        return SystemProperties.getBoolean("sys.super_power_save", false);
    }

    private boolean isVivoiceLaunchable() {
        return Settings.System.getInt(this.mContext.getContentResolver(), "vivo_vivoice_launchable", 1) > 0;
    }

    private TelecomManager getTelecommService() {
        return (TelecomManager) this.mContext.getSystemService("telecom");
    }

    private boolean isTelephonyIdle() {
        try {
            TelecomManager telecomManager = getTelecommService();
            if (telecomManager != null) {
                if (telecomManager.getCallState() != 0) {
                    return false;
                }
                return true;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    private boolean isSecureLocked() {
        KeyguardManager keyguardManager = this.mKeyguardManager;
        if (keyguardManager != null && keyguardManager.isKeyguardSecure() && this.mKeyguardManager.isKeyguardLocked()) {
            return true;
        }
        return false;
    }

    private boolean performHapticFeedback() {
        if (this.mVibrator.hasVibrator()) {
            boolean hapticsDisabled = Settings.System.getIntForUser(this.mContext.getContentResolver(), "haptic_feedback_enabled", 0, -2) == 0;
            if (hapticsDisabled) {
                return false;
            }
            this.mVibrator.vibrate(this.mLongPressVibePattern, -1);
            return true;
        }
        return false;
    }

    private void startVivoice(boolean needWakeLock) {
        if (isSuperPowerSaverMode() || !isTelephonyIdle() || isSecureLocked() || !isVivoiceLaunchable()) {
            return;
        }
        if (needWakeLock) {
            this.mMediaEventWakeLock.acquire();
        }
        try {
            try {
                Intent intent = new Intent("vivo.intent.action.HEADSETHOOK_KEY_LONG_PRESS");
                intent.setPackage("com.bbk.VoiceAssistant");
                intent.setFlags(268435456);
                this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
                performHapticFeedback();
                if (!needWakeLock) {
                    return;
                }
            } catch (ActivityNotFoundException e) {
                VLog.w(TAG, "No activity... for search: " + e);
                if (!needWakeLock) {
                    return;
                }
            }
            this.mMediaEventWakeLock.release();
        } catch (Throwable th) {
            if (needWakeLock) {
                this.mMediaEventWakeLock.release();
            }
            throw th;
        }
    }

    private void startAgent(boolean needWakeLock) {
        if (needWakeLock) {
            this.mMediaEventWakeLock.acquire();
        }
        try {
            try {
                Intent intent = new Intent("vivo.intent.action.HEADSETHOOK_KEY_LONG_PRESS");
                intent.setPackage("com.vivo.agent");
                this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
                performHapticFeedback();
                if (!needWakeLock) {
                    return;
                }
            } catch (RuntimeException e) {
                VLog.w(TAG, "startServiceAsUser erro: " + e);
                if (!needWakeLock) {
                    return;
                }
            }
            this.mMediaEventWakeLock.release();
        } catch (Throwable th) {
            if (needWakeLock) {
                this.mMediaEventWakeLock.release();
            }
            throw th;
        }
    }

    private void startVivoiceAssitantRemind(boolean needWakeLock) {
        if (isSuperPowerSaverMode() || !isTelephonyIdle() || isSecureLocked() || !isVivoiceLaunchable()) {
            return;
        }
        if (needWakeLock) {
            this.mMediaEventWakeLock.acquire();
        }
        try {
            try {
                Intent intent = new Intent();
                intent.addFlags(268435456);
                intent.setClassName("com.bbk.VoiceAssistant", "com.bbk.VoiceAssistant.LauchTipActivity");
                this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
                performHapticFeedback();
                if (!needWakeLock) {
                    return;
                }
            } catch (ActivityNotFoundException e) {
                VLog.w(TAG, "No activity... for search: " + e);
                if (!needWakeLock) {
                    return;
                }
            }
            this.mMediaEventWakeLock.release();
        } catch (Throwable th) {
            if (needWakeLock) {
                this.mMediaEventWakeLock.release();
            }
            throw th;
        }
    }

    public void iskeepFrozenMediaSeesion(String sessionPackageName, int uid, KeyEvent keyEvent) {
        VivoFrozenPackageSupervisor instance;
        if (keyEvent != null) {
            int keyCode = keyEvent.getKeyCode();
            if (sessionPackageName != null) {
                if ((keyCode == 79 || keyCode == 126 || keyCode == 88 || keyCode == 87 || keyCode == 85) && (instance = VivoFrozenPackageSupervisor.getInstance()) != null) {
                    instance.isKeepFrozen(sessionPackageName, uid, null, -1, 6, true, "media KEYCODE");
                }
            }
        }
    }

    public boolean isPackRunning(String packageName) {
        ActivityManager activityManager = (ActivityManager) this.mContext.getSystemService(VivoFirewall.TYPE_ACTIVITY);
        if (activityManager == null || packageName == null || activityManager.getPackageImportance(packageName) == 1000) {
            return false;
        }
        return true;
    }
}