package com.android.server.policy.key;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.SQLException;
import android.net.Uri;
import android.os.FtBuild;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.view.KeyEvent;
import com.android.internal.telecom.ITelecomService;
import com.android.internal.telephony.VivoTelephonyApiParams;
import com.android.server.pm.VivoPKMSLocManager;
import com.android.server.policy.AVivoInterceptKeyCallback;
import com.android.server.policy.IVivoAdjustmentPolicy;
import com.android.server.policy.VivoWMPHook;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import com.vivo.fingerprint.FingerprintConfig;

/* loaded from: classes.dex */
public final class VivoPowerKeyHandler extends AVivoInterceptKeyCallback {
    private static final String KEY_FLASHLIGHT_ENABLE = "flashlight_enabled";
    private static final String KEY_FLASHLIGHT_STATE = "FlashState";
    private static final String KEY_VIVOFLASH_STATE = "vivoFlash_state";
    private static int POWER_INTERVAL_DELAY = 500;
    private static int POWER_INTERVAL_DELAY_FOR_SOS = VivoPKMSLocManager.MAX_LOCATION_WAIT_TIME;
    private static final String TAG = "VivoPowerKeyHandler";
    private boolean isVOS_1;
    private boolean isVos;
    private Context mContext;
    private boolean mPowerKeyConsumed;
    private final ContentResolver mResolver;
    private VivoFlashlightController mVivoFlashlightController;
    private IVivoAdjustmentPolicy mVivoPolicy;
    private int mEmgencyCount = 0;
    private int mSosCount = 0;
    private boolean mEmergencyEnable = false;
    private Handler mHandler = new Handler();
    private Runnable mTimeClickResetRunnable = new Runnable() { // from class: com.android.server.policy.key.VivoPowerKeyHandler.1
        @Override // java.lang.Runnable
        public void run() {
            VLog.d(VivoPowerKeyHandler.TAG, "PowerKey reset click times because overtime.");
            VivoPowerKeyHandler.this.mEmgencyCount = 0;
        }
    };
    private Runnable mResetSosCountRunnable = new Runnable() { // from class: com.android.server.policy.key.VivoPowerKeyHandler.2
        @Override // java.lang.Runnable
        public void run() {
            VLog.d(VivoPowerKeyHandler.TAG, "PowerKey reset sos click times because overtime.");
            VivoPowerKeyHandler.this.mSosCount = 0;
        }
    };

    public VivoPowerKeyHandler(Context context, IVivoAdjustmentPolicy vivoPolicy) {
        boolean z = false;
        this.isVos = false;
        this.isVOS_1 = false;
        this.mVivoFlashlightController = null;
        this.mContext = context;
        this.mVivoPolicy = vivoPolicy;
        this.mResolver = context.getContentResolver();
        registerCallObserver();
        POWER_INTERVAL_DELAY = SystemProperties.getInt("persist.vivo.emergency.interval", 500);
        this.isVos = "vos".equals(FtBuild.getOsName());
        if ("vos".equals(FtBuild.getOsName()) && "1.0".equals(FtBuild.getOsVersion())) {
            z = true;
        }
        this.isVOS_1 = z;
        if (this.isVos) {
            this.mVivoFlashlightController = new VivoFlashlightController(context);
        }
    }

    private void registerCallObserver() {
        boolean isIndia = "IN".equals(SystemProperties.get("ro.product.customize.bbk", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK));
        if (isIndia) {
            try {
                boolean z = true;
                if (Settings.Global.getInt(this.mResolver, "sos_call_toggle", 1) != 1) {
                    z = false;
                }
                this.mEmergencyEnable = z;
                VLog.d(TAG, "registerCallObserver EmergencyEnable = " + this.mEmergencyEnable);
                this.mResolver.registerContentObserver(Settings.Global.getUriFor("sos_call_toggle"), false, new CallSettingsObserver(this.mHandler));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private ITelecomService getITelecom() {
        return ITelecomService.Stub.asInterface(ServiceManager.getService("telecom"));
    }

    private void dial() {
        if (!isUserSetupComplete()) {
            VLog.d(TAG, "Ignoring because device not setup.");
            return;
        }
        VLog.d(TAG, "notify powerkey tree.");
        try {
            VivoTelephonyApiParams args = new VivoTelephonyApiParams("API_TAG_placeCall");
            args.put("number", "112");
            args.put("slot", 0);
            args.put("isMotionCall", false);
            args.put("isVideo", false);
            args.put("isPowerKeyDialer", true);
            getITelecom().vivoTelephonyApi(args);
        } catch (RemoteException e) {
            VLog.w(TAG, "call vivoTelephonyApi failed " + e);
        }
    }

    private void sosCall() {
        if (!isUserSetupComplete()) {
            VLog.d(TAG, "Ignoring because device not setup.");
            return;
        }
        VLog.d(TAG, "notify sos call");
        try {
            VivoTelephonyApiParams args = new VivoTelephonyApiParams("API_TAG_SosModePlaceCall");
            getITelecom().vivoTelephonyApi(args);
        } catch (RemoteException e) {
            VLog.w(TAG, "call vivoTelephonyApi failed " + e);
        }
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public boolean onCheckForward(int keyCode, KeyEvent event) {
        int i = this.mState;
        return false;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public int onKeyDown(int keyCode, KeyEvent event) {
        int result = -100;
        if (this.mState == 0) {
            if (!this.isVOS_1 && !this.mIsScreenOn && isFlashlightOn()) {
                turnOffFlashLight();
                this.mPowerKeyConsumed = true;
                result = 0;
            }
            if (FingerprintConfig.isSideFingerprint() && FingerprintConfig.isPowerKeyDisabledByFingerprint(this.mContext)) {
                VLog.d(TAG, "handle powerkey down result by fingerprint");
                return result;
            }
            if (this.mEmgencyCount == 0) {
                this.mHandler.removeCallbacks(this.mTimeClickResetRunnable);
                this.mHandler.postDelayed(this.mTimeClickResetRunnable, POWER_INTERVAL_DELAY);
            }
            if (this.mVivoPolicy.getLastKeyCode() != 26) {
                this.mSosCount = 0;
                VLog.d(TAG, "recet sos count because other key trigger");
            }
            if (this.mSosCount == 0) {
                this.mHandler.removeCallbacks(this.mResetSosCountRunnable);
                this.mHandler.postDelayed(this.mResetSosCountRunnable, POWER_INTERVAL_DELAY_FOR_SOS);
            }
            int i = this.mEmgencyCount + 1;
            this.mEmgencyCount = i;
            this.mSosCount++;
            if (i == 3 && this.mEmergencyEnable) {
                dial();
                this.mPowerKeyConsumed = true;
                result = 0;
            }
            if (this.mSosCount == 5) {
                sosCall();
            }
        }
        return result;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public int onKeyUp(int keyCode, KeyEvent event) {
        if (this.mState != 0 || !this.mPowerKeyConsumed) {
            return -100;
        }
        this.mPowerKeyConsumed = false;
        return 0;
    }

    private boolean isFlashlightOn() {
        if (this.isVos) {
            boolean isFlashLightOn = this.mVivoFlashlightController.isFlashLightOn();
            printf("light = " + isFlashLightOn);
            return isFlashLightOn;
        }
        int flash = Settings.System.getIntForUser(this.mResolver, KEY_VIVOFLASH_STATE, 0, -2);
        int light = Settings.System.getIntForUser(this.mResolver, KEY_FLASHLIGHT_STATE, 0, -2);
        printf("flash = " + flash + " light = " + light);
        return flash == 0 && light == 1;
    }

    private void turnOffFlashLight() {
        if (this.isVos) {
            this.mVivoFlashlightController.setFlashLight(false);
            return;
        }
        printf("turnOffFlashLight");
        Settings.System.putIntForUser(this.mResolver, KEY_FLASHLIGHT_STATE, 0, -2);
    }

    private void printf(String msg) {
        VivoWMPHook.printf(msg);
    }

    private boolean isUserSetupComplete() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 0, -2) != 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class CallSettingsObserver extends ContentObserver {
        public CallSettingsObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            try {
                VivoPowerKeyHandler vivoPowerKeyHandler = VivoPowerKeyHandler.this;
                boolean z = true;
                if (Settings.Global.getInt(VivoPowerKeyHandler.this.mResolver, "sos_call_toggle", 1) != 1) {
                    z = false;
                }
                vivoPowerKeyHandler.mEmergencyEnable = z;
                VLog.d(VivoPowerKeyHandler.TAG, "onChange EmergencyEnable = " + VivoPowerKeyHandler.this.mEmergencyEnable);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}