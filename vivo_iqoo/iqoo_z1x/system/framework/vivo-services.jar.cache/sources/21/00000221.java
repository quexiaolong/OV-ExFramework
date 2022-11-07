package com.android.server.display.color;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.opengl.Matrix;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.FtFeature;
import com.vivo.dcdiming.DcDiming;
import com.vivo.face.common.data.Constants;
import com.vivo.sensor.implement.SensorConfig;
import com.vivo.services.rms.ProcessList;
import java.util.Arrays;
import java.util.Vector;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoLightColorMatrixControl {
    private static final int ENTER_HDR_TRANSACTION = 1000;
    private static final int EXIT_HDR_TRANSACTION = 1001;
    private static final int POWER_STATE_FINISH_SLEEP = 4;
    private static final int POWER_STATE_FINISH_WAKE_UP = 2;
    private static final int POWER_STATE_START_SLEEP = 3;
    private static final int POWER_STATE_START_UNKNOW = 0;
    private static final int POWER_STATE_START_WAKE_UP = 1;
    private static final String SURFACE_FLINGER = "SurfaceFlinger";
    private static final int SURFACE_FLINGER_TRANSACTION_SET_HDR_BINDER = 31019;
    static final String TAG = "VivoLightColorMatrixControl";
    private static final String VIVO_DARK_COLOR = "vivo_nightmode_used";
    private static final String VIVO_DC_DIMMING_ACTIVE = "vivo_dc_dimming_active";
    private static final String VIVO_DC_DIMMING_SET = "vivo_dc_dimming_enabled";
    private static final String VIVO_DISPLAY_INVERSION = "accessibility_display_inversion_enabled";
    private static final String VIVO_LIGHT_MATRIX_ENABLED = "vivo_light_matrix_enabled";
    private static final String VIVO_SET_LIGHT_MATRIX = "vivo_set_light_matrix";
    private final String VIVO_PRODUCT_MODEL;
    private ColorDisplayService mColorDisplayService;
    private ContentObserver mContentObserver;
    private final Context mContext;
    private int mCurBrightness;
    private int mCurrentUser;
    private int mDcEnable;
    private final boolean mDcHdrLimit;
    private LightTintController mDcTint;
    private DcDiming mDimControl;
    private boolean mDoubleScreen;
    private boolean mFirstTime;
    private Binder mHdrBinder;
    private final IBinder.DeathRecipient mHdrBinderDeathRecipient;
    private int mLastBrightnessBeforeSleep;
    private boolean mLmDebug;
    private LightTintController mLmTint;
    private float mScreenBrightnessSettingDefault;
    private final boolean mSupportDc;
    private final boolean mSupportDcLayer;
    private final boolean mSupportDcMatrix;
    private final boolean mSupportDcNightModeExclusion;
    private final boolean mSupportLm = FtFeature.isFeatureSupport("vivo.software.lightmatrix");
    private VivoColorManagerService mVivoColorManager;
    private static VivoLightColorMatrixControl mLmControl = null;
    private static boolean mPowerScreenOn = true;
    private static int mPowerState = 0;
    private static Object mPowerScreenLock = new Object();

    /* loaded from: classes.dex */
    public interface IEndAniCallBack {
        void run(int i);
    }

    private VivoLightColorMatrixControl(VivoColorManagerService colorManager, ColorDisplayService colorDisplayService, Context context) {
        boolean isFeatureSupport = FtFeature.isFeatureSupport("vivo.hardware.dcdiming");
        this.mSupportDc = isFeatureSupport;
        this.mSupportDcMatrix = isFeatureSupport && "v2.0".equals(FtFeature.getFeatureAttribute("vivo.hardware.dcdiming", "version", "v2.0"));
        this.mSupportDcLayer = this.mSupportDc && "v1.0".equals(FtFeature.getFeatureAttribute("vivo.hardware.dcdiming", "version", "v2.0"));
        this.mDcHdrLimit = "true".equals(FtFeature.getFeatureAttribute("vivo.hardware.dcdiming", "hdr_limit", "false"));
        this.mSupportDcNightModeExclusion = FtFeature.isFeatureSupport("vivo.hardware.dcnight");
        this.VIVO_PRODUCT_MODEL = SystemProperties.get("ro.vivo.product.model", "unkown").toLowerCase();
        this.mCurrentUser = ProcessList.INVALID_ADJ;
        this.mDcEnable = 0;
        this.mDoubleScreen = false;
        this.mContentObserver = null;
        this.mLmDebug = false;
        this.mFirstTime = true;
        this.mDimControl = DcDiming.getDcInstance();
        this.mCurBrightness = -1;
        this.mLastBrightnessBeforeSleep = -1;
        this.mScreenBrightnessSettingDefault = 0.39763778f;
        this.mHdrBinder = null;
        this.mVivoColorManager = null;
        this.mHdrBinderDeathRecipient = new IBinder.DeathRecipient() { // from class: com.android.server.display.color.VivoLightColorMatrixControl.2
            @Override // android.os.IBinder.DeathRecipient
            public void binderDied() {
                VivoLightColorMatrixControl.this.mHdrBinder.unlinkToDeath(this, 0);
                VivoLightColorMatrixControl.this.mHdrBinder = null;
                if (VivoLightColorMatrixControl.this.mSupportDcLayer) {
                    VivoLightColorMatrixControl.this.onDcStateChanged();
                } else if (VivoLightColorMatrixControl.this.mSupportDcMatrix) {
                    VivoLightColorMatrixControl.this.onDcChanged();
                }
            }
        };
        this.mVivoColorManager = colorManager;
        this.mContext = context;
        this.mColorDisplayService = colorDisplayService;
        if (this.VIVO_PRODUCT_MODEL.startsWith("pd1821")) {
            this.mDoubleScreen = true;
        }
        if (this.mSupportDcMatrix) {
            this.mDcTint = new LightTintController(401);
            VSlog.d(TAG, "Support dc matrix");
        }
        if (this.mSupportDcLayer) {
            VSlog.d(TAG, "Support dc layer");
        }
        if (this.mSupportLm) {
            this.mLmTint = new LightTintController(ProcessList.HEAVY_WEIGHT_APP_ADJ);
            VSlog.d(TAG, "Support light matrix");
        }
        PowerManager pm = (PowerManager) this.mContext.getSystemService("power");
        if (pm != null) {
            this.mScreenBrightnessSettingDefault = pm.getBrightnessConstraint(2);
            VSlog.d(TAG, "mScreenBrightnessSettingDefault=" + this.mScreenBrightnessSettingDefault);
        }
        if (this.mDcHdrLimit) {
            HdrBinder hdrBinder = new HdrBinder();
            this.mHdrBinder = hdrBinder;
            if (hdrBinder != null) {
                hdrBinder.linkToDeath(this.mHdrBinderDeathRecipient, 0);
                setHdrBinderForSf();
            }
        }
    }

    public static synchronized VivoLightColorMatrixControl getInstance(VivoColorManagerService colorManager, ColorDisplayService colorDisplayService, Context context) {
        VivoLightColorMatrixControl vivoLightColorMatrixControl;
        synchronized (VivoLightColorMatrixControl.class) {
            if (mLmControl == null) {
                mLmControl = new VivoLightColorMatrixControl(colorManager, colorDisplayService, context);
            }
            vivoLightColorMatrixControl = mLmControl;
        }
        return vivoLightColorMatrixControl;
    }

    public static synchronized VivoLightColorMatrixControl getExistInstance() {
        VivoLightColorMatrixControl vivoLightColorMatrixControl;
        synchronized (VivoLightColorMatrixControl.class) {
            vivoLightColorMatrixControl = mLmControl;
        }
        return vivoLightColorMatrixControl;
    }

    private void initDefaultStatus() {
        this.mDcEnable = 0;
        this.mLastBrightnessBeforeSleep = -1;
        LightTintController lightTintController = this.mDcTint;
        if (lightTintController != null) {
            lightTintController.onSetUser(this.mCurrentUser);
        }
        LightTintController lightTintController2 = this.mLmTint;
        if (lightTintController2 != null) {
            lightTintController2.onSetUser(this.mCurrentUser);
        }
        LightTintController lightTintController3 = this.mDcTint;
        if (lightTintController3 != null) {
            lightTintController3.setUp(this.mContext, false);
        }
        LightTintController lightTintController4 = this.mLmTint;
        if (lightTintController4 != null) {
            lightTintController4.setUp(this.mContext, false);
        }
        if (this.mSupportDcLayer) {
            this.mDcEnable = Settings.System.getIntForUser(this.mContext.getContentResolver(), VIVO_DC_DIMMING_SET, 0, this.mCurrentUser);
            onDcStateChanged(false);
        }
        if (this.mDcTint != null) {
            onDcChanged();
        }
        if (this.mLmTint != null) {
            onLmChanged();
        }
    }

    public void setUp(int userHandle) {
        this.mCurrentUser = userHandle;
        VSlog.d(TAG, "setUp userHandle=" + userHandle + " mSupportDcMatrix=" + this.mSupportDcMatrix + " mSupportLm=" + this.mSupportLm);
        if (this.mFirstTime) {
            this.mFirstTime = false;
        } else {
            initDefaultStatus();
        }
        if (this.mContentObserver == null) {
            this.mContentObserver = new ContentObserver(this.mColorDisplayService.mHandler) { // from class: com.android.server.display.color.VivoLightColorMatrixControl.1
                @Override // android.database.ContentObserver
                public void onChange(boolean selfChange, Uri uri) {
                    super.onChange(selfChange, uri);
                    String setting = uri == null ? null : uri.getLastPathSegment();
                    VSlog.d(VivoLightColorMatrixControl.TAG, "onChange: setting=" + setting);
                    if (setting != null) {
                        char c = 65535;
                        switch (setting.hashCode()) {
                            case -1291154694:
                                if (setting.equals(VivoLightColorMatrixControl.VIVO_SET_LIGHT_MATRIX)) {
                                    c = 3;
                                    break;
                                }
                                break;
                            case 252330130:
                                if (setting.equals(VivoLightColorMatrixControl.VIVO_DC_DIMMING_SET)) {
                                    c = 0;
                                    break;
                                }
                                break;
                            case 807527615:
                                if (setting.equals(VivoLightColorMatrixControl.VIVO_LIGHT_MATRIX_ENABLED)) {
                                    c = 2;
                                    break;
                                }
                                break;
                            case 1888121780:
                                if (setting.equals(VivoLightColorMatrixControl.VIVO_DARK_COLOR)) {
                                    c = 1;
                                    break;
                                }
                                break;
                        }
                        if (c == 0 || c == 1) {
                            if (VivoLightColorMatrixControl.this.mDcTint != null) {
                                VivoLightColorMatrixControl.this.onDcChanged();
                            } else if (VivoLightColorMatrixControl.this.mSupportDcLayer) {
                                VivoLightColorMatrixControl.this.onDcStateChanged(false);
                            }
                        } else if (c == 2) {
                            VivoLightColorMatrixControl.this.onLmChanged();
                        } else if (c == 3) {
                            VivoLightColorMatrixControl.this.onSetLmChanged();
                        }
                    }
                }
            };
        }
        ContentResolver cr = this.mContext.getContentResolver();
        if (this.mSupportDcMatrix || this.mSupportDcLayer) {
            cr.registerContentObserver(Settings.System.getUriFor(VIVO_DC_DIMMING_SET), false, this.mContentObserver, this.mCurrentUser);
            if (this.mSupportDcNightModeExclusion) {
                cr.registerContentObserver(Settings.System.getUriFor(VIVO_DARK_COLOR), false, this.mContentObserver, this.mCurrentUser);
            }
        }
        if (this.mSupportLm) {
            cr.registerContentObserver(Settings.System.getUriFor(VIVO_LIGHT_MATRIX_ENABLED), false, this.mContentObserver, this.mCurrentUser);
        }
        if (this.mSupportDcMatrix || this.mSupportLm) {
            cr.registerContentObserver(Settings.System.getUriFor(VIVO_SET_LIGHT_MATRIX), false, this.mContentObserver, this.mCurrentUser);
        }
    }

    public void tearDown() {
        VSlog.d(TAG, "tearDown: currentUser=" + this.mCurrentUser);
        if (this.mContentObserver != null) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mContentObserver);
        }
        LightTintController lightTintController = this.mDcTint;
        if (lightTintController != null) {
            lightTintController.tearDown();
        }
        LightTintController lightTintController2 = this.mLmTint;
        if (lightTintController2 != null) {
            lightTintController2.tearDown();
        }
    }

    public void onStartUser(int userHandle) {
        if (this.mFirstTime) {
            this.mCurrentUser = userHandle;
            initDefaultStatus();
        }
    }

    public void onPowerOn() {
        LightTintController lightTintController = this.mDcTint;
        if (lightTintController != null) {
            lightTintController.onPowerOn();
        }
        LightTintController lightTintController2 = this.mLmTint;
        if (lightTintController2 != null) {
            lightTintController2.onPowerOn();
        }
    }

    public void onPowerOff() {
        LightTintController lightTintController = this.mDcTint;
        if (lightTintController != null) {
            lightTintController.onPowerOff();
        }
        LightTintController lightTintController2 = this.mLmTint;
        if (lightTintController2 != null) {
            lightTintController2.onPowerOff();
        }
    }

    public static void onStartedWakingUp(int why) {
        VSlog.d(TAG, "onStartedWakingUp: why=" + why);
        synchronized (mPowerScreenLock) {
            mPowerState = 1;
            mPowerScreenOn = true;
        }
    }

    public static void onFinishedWakingUp() {
        VSlog.d(TAG, "onFinishedWakingUp");
        synchronized (mPowerScreenLock) {
            mPowerState = 2;
        }
    }

    public static void onStartedGoingToSleep(int why) {
        VSlog.d(TAG, "onStartedGoingToSleep: why=" + why);
        synchronized (mPowerScreenLock) {
            mPowerState = 3;
            mPowerScreenOn = false;
        }
    }

    public static void onFinishedGoingToSleep(int why) {
        VSlog.d(TAG, "onFinishedGoingToSleep: why=" + why);
        synchronized (mPowerScreenLock) {
            mPowerState = 4;
        }
    }

    public LightTintController getLightTintController(int level) {
        if (level == 400) {
            return this.mLmTint;
        }
        if (level == 401) {
            return this.mDcTint;
        }
        VSlog.e(TAG, "leve is invalid=" + level);
        return null;
    }

    public void onAutoBrightness(float brightness, boolean useAutoBrightness) {
        VSlog.d(TAG, "onAutoBrightness brightness=" + brightness + " useAutoBrightness=" + useAutoBrightness);
        int lcmbacklight = SensorConfig.float2LcmBrightnessAfterDPC(brightness);
        if (useAutoBrightness && brightness != 0.0f) {
            VSlog.d(TAG, "get auto brightness=" + lcmbacklight + " ahead.");
            onBrightnessChanged(0, lcmbacklight);
        }
    }

    public void notifyUdSnapshot() {
        int i;
        VSlog.d(TAG, "notifyUdSnapshot mCurBrightness=" + this.mCurBrightness + " mLastBrightnessBeforeSleep=" + this.mLastBrightnessBeforeSleep);
        if (this.mCurBrightness == 0 && (i = this.mLastBrightnessBeforeSleep) > 0) {
            onBrightnessChanged(0, i);
        }
    }

    public void onBrightnessChanged(int display, int brightness) {
        if (brightness == 0) {
            this.mLastBrightnessBeforeSleep = this.mCurBrightness;
        }
        this.mCurBrightness = brightness;
        LightTintController lightTintController = this.mDcTint;
        if (lightTintController != null) {
            lightTintController.onBrightnessChanged(brightness);
        }
        LightTintController lightTintController2 = this.mLmTint;
        if (lightTintController2 != null) {
            lightTintController2.onBrightnessChanged(brightness);
        }
        if (this.mSupportDcLayer) {
            this.mDimControl.setBrightnessV2(display, brightness);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean onDcStateChanged() {
        return onDcStateChanged(false, false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean onDcStateChanged(boolean bInversion) {
        return onDcStateChanged(bInversion, false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized boolean onDcStateChanged(boolean bInversion, boolean isHdr) {
        boolean bUpdateInvertColor;
        int final_dc_state;
        boolean dc_diming_enable_state = isAccessiblityDcEnabled();
        boolean display_inversion_state = isAccessiblityInversionEnabled();
        boolean nightmode_state = isAccessiblityDarkColorEnabled();
        int original_dc_sate = this.mDimControl.getDcDimingState();
        boolean bUpdateDc = false;
        bUpdateInvertColor = false;
        if (this.mSupportDcNightModeExclusion) {
            if (nightmode_state) {
                if (dc_diming_enable_state) {
                    Settings.System.putIntForUser(this.mContext.getContentResolver(), VIVO_DC_DIMMING_SET, 0, this.mCurrentUser);
                    dc_diming_enable_state = false;
                    Settings.System.putIntForUser(this.mContext.getContentResolver(), VIVO_DC_DIMMING_ACTIVE, 1, this.mCurrentUser);
                }
            } else {
                int dimming_active_state = Settings.System.getIntForUser(this.mContext.getContentResolver(), VIVO_DC_DIMMING_ACTIVE, 0, this.mCurrentUser);
                if (dimming_active_state == 1 && !dc_diming_enable_state) {
                    Settings.System.putIntForUser(this.mContext.getContentResolver(), VIVO_DC_DIMMING_SET, 1, this.mCurrentUser);
                    dc_diming_enable_state = true;
                    Settings.System.putIntForUser(this.mContext.getContentResolver(), VIVO_DC_DIMMING_ACTIVE, 0, this.mCurrentUser);
                }
            }
        }
        VSlog.d(TAG, "Setting dcdiming original state=" + original_dc_sate + " dc_diming_enable_state=" + dc_diming_enable_state + " display_inversion_state=" + display_inversion_state + " mSupportDcNightModeExclusion=" + this.mSupportDcNightModeExclusion + " nightmode_state=" + nightmode_state);
        if (dc_diming_enable_state && !display_inversion_state && ((!this.mSupportDcNightModeExclusion || !nightmode_state) && !isHdr)) {
            final_dc_state = 1;
        } else {
            final_dc_state = 0;
        }
        VSlog.d(TAG, "Setting dcdiming state from " + original_dc_sate + " to " + final_dc_state);
        if (original_dc_sate != final_dc_state) {
            VSlog.d(TAG, "dc change mDcEnable=" + this.mDcEnable);
            if (this.mLmTint != null) {
                VSlog.d(TAG, " mLightMartrixEnable=" + this.mLmTint.isActivated());
            }
            this.mDcEnable = final_dc_state;
            boolean bLmEnable = isAccessiblityLightMatrixEnabled();
            if (this.mLmTint != null && bLmEnable && this.mLmTint.isAvailableBrightness() && !display_inversion_state && (!this.mSupportDcNightModeExclusion || !nightmode_state)) {
                if (final_dc_state == 0 && !this.mLmTint.isActivated()) {
                    this.mLmTint.endLmAnimator();
                    this.mLmTint.setActivated(true);
                    this.mLmTint.updateDtmColorMatrix();
                    float[] matrix = this.mLmTint.getRawMatrix();
                    int lightBound = this.mLmTint.getLightBound();
                    VSlog.d(TAG, "dc disable brightnessMatrix R=" + matrix[0] + " G=" + matrix[5] + " B=" + matrix[10]);
                    this.mDimControl.setDcDimingStateV3(0, final_dc_state, lightBound, matrix);
                    if (this.mDoubleScreen) {
                        this.mDimControl.setDcDimingStateV3(1, final_dc_state, lightBound, matrix);
                    }
                    bUpdateDc = true;
                } else if (final_dc_state != 1 || !this.mLmTint.isActivated()) {
                    bUpdateDc = false;
                } else {
                    this.mLmTint.endLmAnimator();
                    float[] matrix2 = this.mLmTint.getRawMatrix();
                    VSlog.d(TAG, "dc enable brightnessMatrix R=" + matrix2[0] + " G=" + matrix2[5] + " B=" + matrix2[10]);
                    this.mDimControl.setDcDimingStateV3(0, final_dc_state, this.mCurBrightness, matrix2);
                    if (this.mDoubleScreen) {
                        this.mDimControl.setDcDimingStateV3(1, final_dc_state, this.mCurBrightness, matrix2);
                    }
                    this.mLmTint.setActivated(false);
                    this.mLmTint.updateDtmColorMatrix();
                    bUpdateDc = true;
                }
            }
            if (this.mLmTint != null && !bUpdateDc && bInversion && this.mCurBrightness > 0 && this.mCurBrightness <= this.mDimControl.getMaxDcBrightness(0)) {
                if (final_dc_state == 1 && !display_inversion_state) {
                    float[] matrix3 = this.mLmTint.getRawMatrix();
                    this.mDimControl.setDcDimingStateV3(0, 4, this.mCurBrightness, matrix3);
                    if (this.mDoubleScreen) {
                        this.mDimControl.setDcDimingStateV3(1, 4, this.mCurBrightness, matrix3);
                    }
                    updateInvertColorMatrix();
                    bUpdateInvertColor = true;
                    bUpdateDc = true;
                } else if (final_dc_state == 0 && display_inversion_state) {
                    float[] matrix4 = this.mLmTint.getRawMatrix();
                    this.mDimControl.setDcDimingStateV3(0, 3, this.mCurBrightness, matrix4);
                    if (this.mDoubleScreen) {
                        this.mDimControl.setDcDimingStateV3(1, 3, this.mCurBrightness, matrix4);
                    }
                    updateInvertColorMatrix();
                    bUpdateInvertColor = true;
                    bUpdateDc = true;
                } else {
                    bUpdateInvertColor = false;
                    bUpdateDc = false;
                }
            }
            if (!bUpdateDc) {
                this.mDimControl.setDcDimingStateV2(0, final_dc_state);
                if (this.mDoubleScreen) {
                    this.mDimControl.setDcDimingStateV2(1, final_dc_state);
                }
            }
        }
        return bUpdateInvertColor;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean onDcChanged() {
        return onDcChanged(false, false);
    }

    private boolean onDcChanged(boolean bInversion) {
        return onDcChanged(bInversion, false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized boolean onDcChanged(boolean bInversion, boolean isHdr) {
        boolean bUpdateInvertColor;
        boolean final_dc_state;
        boolean dc_diming_enable_state = isAccessiblityDcEnabled();
        boolean display_inversion_state = isAccessiblityInversionEnabled();
        boolean nightmode_state = isAccessiblityDarkColorEnabled();
        bUpdateInvertColor = false;
        boolean original_dc_sate = this.mDcTint.isFeatureEnable();
        if (this.mSupportDcNightModeExclusion) {
            if (nightmode_state) {
                if (dc_diming_enable_state) {
                    Settings.System.putIntForUser(this.mContext.getContentResolver(), VIVO_DC_DIMMING_SET, 0, this.mCurrentUser);
                    dc_diming_enable_state = false;
                    Settings.System.putIntForUser(this.mContext.getContentResolver(), VIVO_DC_DIMMING_ACTIVE, 1, this.mCurrentUser);
                }
            } else {
                int dimming_active_state = Settings.System.getIntForUser(this.mContext.getContentResolver(), VIVO_DC_DIMMING_ACTIVE, 0, this.mCurrentUser);
                if (dimming_active_state == 1 && !dc_diming_enable_state) {
                    Settings.System.putIntForUser(this.mContext.getContentResolver(), VIVO_DC_DIMMING_SET, 1, this.mCurrentUser);
                    dc_diming_enable_state = true;
                    Settings.System.putIntForUser(this.mContext.getContentResolver(), VIVO_DC_DIMMING_ACTIVE, 0, this.mCurrentUser);
                }
            }
        }
        VSlog.d(TAG, "Setting dcdiming original state=" + original_dc_sate + " dc_diming_enable_state=" + dc_diming_enable_state + " display_inversion_state=" + display_inversion_state + " mSupportDcNightModeExclusion=" + this.mSupportDcNightModeExclusion + " nightmode_state=" + nightmode_state);
        if (dc_diming_enable_state && !display_inversion_state && ((!this.mSupportDcNightModeExclusion || !nightmode_state) && !isHdr && !this.mLmDebug)) {
            final_dc_state = true;
        } else {
            final_dc_state = false;
        }
        VSlog.d(TAG, "Setting dcdiming state from " + original_dc_sate + " to " + final_dc_state);
        if (this.mLmTint != null) {
            VSlog.d(TAG, "isAvailableBrightness = " + this.mLmTint.isAvailableBrightness());
        }
        boolean bLmEnable = isAccessiblityLightMatrixEnabled();
        if (original_dc_sate != final_dc_state) {
            if (this.mLmTint == null || !this.mLmTint.isAvailableBrightness() || display_inversion_state || !bLmEnable) {
                if (bInversion && this.mDcTint.isAvailableBrightness()) {
                    if (final_dc_state && !display_inversion_state) {
                        this.mDcEnable = 1;
                        this.mDcTint.onDisableInversionColor(null);
                        updateInvertColorMatrix();
                        bUpdateInvertColor = true;
                    } else if (!final_dc_state && display_inversion_state) {
                        this.mDcEnable = 0;
                        this.mDcTint.onEnableInversionColor();
                        updateInvertColorMatrix();
                        bUpdateInvertColor = true;
                    } else {
                        bUpdateInvertColor = false;
                    }
                } else if (final_dc_state) {
                    this.mDcEnable = 1;
                    this.mDcTint.onEnable(null);
                    if (this.mLmTint != null) {
                        this.mLmTint.setFeatureEnable(false);
                    }
                } else {
                    this.mDcEnable = 0;
                    this.mDcTint.onDisable(true);
                    if (this.mLmTint != null) {
                        this.mLmTint.setFeatureEnable(true);
                    }
                }
            } else if (final_dc_state) {
                VSlog.d(TAG, "enable dc");
                this.mDcEnable = 1;
                float[] revertMatrix = this.mLmTint.onDisable(false);
                this.mDcTint.onEnable(revertMatrix);
            } else {
                VSlog.d(TAG, "disable dc");
                this.mDcEnable = 0;
                float[] revertMatrix2 = this.mDcTint.onDisable(false);
                this.mLmTint.onEnable(revertMatrix2);
            }
        }
        return bUpdateInvertColor;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean onLmChanged() {
        return onLmChanged(false);
    }

    private synchronized boolean onLmChanged(boolean bInversion) {
        boolean bLmEnable;
        boolean bLmEnable2 = isAccessiblityLightMatrixEnabled();
        boolean bLmFeatureEnable = this.mLmTint.isFeatureEnable();
        boolean display_inversion_state = isAccessiblityInversionEnabled();
        boolean bUpdateInvertColor = false;
        VSlog.d(TAG, "onLmChanged bLmFeatureEnable=" + bLmFeatureEnable + " bLmEnable=" + bLmEnable2 + " mDcEnable=" + this.mDcEnable);
        if (bLmEnable2 && !display_inversion_state && !this.mLmDebug) {
            bLmEnable = true;
        } else {
            bLmEnable = false;
        }
        if (bLmEnable != bLmFeatureEnable) {
            if (this.mDcEnable != 1 && this.mLmTint != null) {
                if (bLmEnable) {
                    if (!bInversion || !display_inversion_state || !this.mLmTint.isAvailableBrightness()) {
                        this.mLmTint.onEnable(null);
                    } else {
                        this.mLmTint.onEnable(null);
                        updateInvertColorMatrix();
                        bUpdateInvertColor = true;
                    }
                } else if (bInversion && !display_inversion_state && this.mLmTint.isAvailableBrightness()) {
                    updateInvertColorMatrix();
                    this.mLmTint.onDisable(true);
                    bUpdateInvertColor = true;
                } else {
                    this.mLmTint.onDisable(true);
                }
            }
            return false;
        }
        return bUpdateInvertColor;
    }

    public boolean onInversionChanged() {
        VSlog.d(TAG, "onInversionChanged");
        boolean bDcUpdateInvertColor = false;
        if (!this.mSupportDcMatrix && !this.mSupportLm && !this.mSupportDcLayer) {
            return false;
        }
        if (this.mSupportDcMatrix) {
            bDcUpdateInvertColor = onDcChanged(true);
        } else if (this.mSupportDcLayer) {
            bDcUpdateInvertColor = onDcStateChanged(true);
        }
        boolean bLmUpdateInvertColor = this.mSupportLm ? onLmChanged(true) : false;
        if (!bDcUpdateInvertColor && !bLmUpdateInvertColor) {
            setInvertColorMatrix();
        }
        return true;
    }

    /* loaded from: classes.dex */
    class HdrBinder extends Binder {
        HdrBinder() {
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == 1000) {
                VSlog.d(VivoLightColorMatrixControl.TAG, "ENTER_HDR_TRANSACTION mPowerScreenOn=" + VivoLightColorMatrixControl.mPowerScreenOn);
                synchronized (VivoLightColorMatrixControl.mPowerScreenLock) {
                    if (VivoLightColorMatrixControl.this.mCurBrightness > 0 && VivoLightColorMatrixControl.mPowerScreenOn) {
                        if (VivoLightColorMatrixControl.this.mSupportDcLayer) {
                            VivoLightColorMatrixControl.this.onDcStateChanged(false, true);
                        } else if (VivoLightColorMatrixControl.this.mSupportDcMatrix) {
                            VivoLightColorMatrixControl.this.onDcChanged(false, true);
                        }
                    }
                }
            } else if (code == 1001) {
                VSlog.d(VivoLightColorMatrixControl.TAG, "EXIT_HDR_TRANSACTION mPowerScreenOn=" + VivoLightColorMatrixControl.mPowerScreenOn);
                synchronized (VivoLightColorMatrixControl.mPowerScreenLock) {
                    if (VivoLightColorMatrixControl.this.mCurBrightness > 0 && VivoLightColorMatrixControl.mPowerScreenOn) {
                        if (VivoLightColorMatrixControl.this.mSupportDcLayer) {
                            VivoLightColorMatrixControl.this.onDcStateChanged();
                        } else if (VivoLightColorMatrixControl.this.mSupportDcMatrix) {
                            VivoLightColorMatrixControl.this.onDcChanged();
                        }
                    }
                }
            } else {
                VSlog.d(VivoLightColorMatrixControl.TAG, "error code=" + code);
            }
            return super.onTransact(code, data, reply, flags);
        }
    }

    private void setHdrBinderForSf() {
        VSlog.d(TAG, "setHdrBinderForSf");
        IBinder flinger = ServiceManager.getService(SURFACE_FLINGER);
        if (flinger != null) {
            Parcel data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            data.writeStrongBinder(this.mHdrBinder);
            try {
                try {
                    flinger.transact(SURFACE_FLINGER_TRANSACTION_SET_HDR_BINDER, data, null, 0);
                } catch (Exception e) {
                    VSlog.e(TAG, "Failed to set setHdrBinderForSf");
                }
            } finally {
                data.recycle();
            }
        }
    }

    private void updateInvertColorMatrix() {
        VSlog.d(TAG, "updateInvertColorMatrix");
        this.mColorDisplayService.mHandler.sendEmptyMessage(12);
    }

    private void setInvertColorMatrix() {
        VSlog.d(TAG, "setInvertColorMatrix");
        this.mColorDisplayService.mHandler.sendEmptyMessage(13);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onSetLmChanged() {
        String factor_str = Settings.System.getStringForUser(this.mContext.getContentResolver(), VIVO_SET_LIGHT_MATRIX, this.mCurrentUser);
        int factor = Integer.parseInt(factor_str);
        VSlog.d(TAG, "onSetLmChanged factor=" + factor);
        if (!this.mSupportLm && !this.mSupportDcMatrix) {
            VSlog.e(TAG, "onSetLmChanged failed: not enable dc/lm");
        } else if (factor >= 0) {
            this.mLmDebug = true;
            VSlog.d(TAG, "onSetLmChanged disable lm");
            if (this.mDcTint != null) {
                onDcChanged();
                this.mDcTint.setFactorMatrix(factor);
                this.mColorDisplayService.mHandler.sendEmptyMessage(9);
            } else if (this.mLmTint != null) {
                onLmChanged();
                this.mLmTint.setFactorMatrix(factor);
                this.mColorDisplayService.mHandler.sendEmptyMessage(6);
            } else {
                VSlog.e(TAG, "onSetLmChanged failed");
            }
        } else {
            this.mLmDebug = false;
            if (this.mDcTint != null) {
                onDcChanged();
            } else if (this.mLmTint != null) {
                onLmChanged();
            } else {
                VSlog.e(TAG, "onSetLmChanged failed");
            }
            VSlog.d(TAG, "onSetLmChanged enable lm");
        }
    }

    private boolean isAccessiblityInversionEnabled() {
        if (this.mCurrentUser == -10000) {
            this.mCurrentUser = -2;
        }
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), VIVO_DISPLAY_INVERSION, 0, this.mCurrentUser) != 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isAccessiblityDcEnabled() {
        if (this.mCurrentUser == -10000) {
            this.mCurrentUser = -2;
        }
        return Settings.System.getIntForUser(this.mContext.getContentResolver(), VIVO_DC_DIMMING_SET, 0, this.mCurrentUser) != 0;
    }

    private boolean isAccessiblityDarkColorEnabled() {
        if (this.mCurrentUser == -10000) {
            this.mCurrentUser = -2;
        }
        return Settings.System.getIntForUser(this.mContext.getContentResolver(), VIVO_DARK_COLOR, 0, this.mCurrentUser) == 1;
    }

    private boolean isAccessiblityLightMatrixEnabled() {
        if (this.mCurrentUser == -10000) {
            this.mCurrentUser = -2;
        }
        return Settings.System.getIntForUser(this.mContext.getContentResolver(), VIVO_LIGHT_MATRIX_ENABLED, 1, this.mCurrentUser) != 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getCurBrightnessFromSettings() {
        if (this.mCurrentUser == -10000) {
            this.mCurrentUser = -2;
        }
        float brightnessFloat = Settings.System.getFloatForUser(this.mContext.getContentResolver(), "screen_brightness_float", this.mScreenBrightnessSettingDefault, this.mCurrentUser);
        return SensorConfig.float2LcmBrightness(brightnessFloat);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class LightTintController extends TintController {
        private static final String DC_DIMMING_PATH = "/sys/lcm/dc_dimming";
        private static final String LCM_CONFIG_PATH = "/system/etc/LcmConfig/LcmConfig.json";
        private static final int LM_DISABLE_FOR_INVERT = 3;
        private static final int LM_ENABLE_FOR_INVERT = 4;
        private static final int LM_ENABLE_FOR_UD = 5;
        private static final int LM_INVALID = 0;
        private static final int LM_UPDATE = 1;
        private static final int NOT_SET = -1;
        private static final String OLED_HBM_PATH = "/sys/lcm/oled_hbm";
        private static final int SURFACE_FLINGER_TRANSACTION_GET_UD_HBM_STATUS = 31017;
        private static final int SURFACE_FLINGER_TRANSACTION_SET_LIGHT_BOUND = 31018;
        private static final int SURFACE_FLINGER_TRANSACTION_SET_LIGHT_COLOR_MATRIX = 31014;
        private static final int UPDATE_LIGHT = 2;
        private static final String VIVO_DISPLAY_BRIGHTNESS_MATRIX_ACTIVATED = "vivo_display_brightness_matrix_activated";
        private String TAG;
        private int mLevel;
        private final String mProductModel = SystemProperties.get("ro.vivo.product.model", "unkown");
        private final String mLowerProductModel = SystemProperties.get("ro.vivo.product.model", "unkown").toLowerCase();
        private int VIVO_MARTRIX_BOUND_LIGHT = 0;
        private final float[] mMatrix = new float[16];
        private final float[] MATRIX_IDENTITY = new float[16];
        private boolean mPowerChange = false;
        private Object mLock = new Object();
        private Vector<Double> mColorMatrixAlphaMap = new Vector<>(0);
        private Boolean mIsFeatureEnable = false;
        private float[] mTempMatrix = new float[16];
        private int mCurBrightness = -1;
        private boolean mDisableLmAgain = false;

        public LightTintController(int level) {
            this.TAG = "VivoLightTintController";
            this.mLevel = 0;
            this.mLevel = level;
            if (level == 401) {
                this.TAG = "VivoLightTintController_DC";
            } else {
                this.TAG = "VivoLightTintController_LM";
            }
            initConfigAlpha();
        }

        private void initConfigAlpha() {
            String JsonData = readJson(LCM_CONFIG_PATH);
            boolean bMatch = parseConfigAlphaJson(JsonData, true);
            if (!bMatch) {
                bMatch = parseConfigAlphaJson(JsonData, false);
            }
            if (!bMatch) {
                useDefaultColorMatrixAlphaMap();
            }
            setLightBoundForSf();
        }

        public void setUp(Context context, boolean needsLinear) {
            Matrix.setIdentityM(this.MATRIX_IDENTITY, 0);
            Matrix.setIdentityM(this.mMatrix, 0);
            Matrix.setIdentityM(this.mTempMatrix, 0);
            setActivated(null, 0);
            this.mIsFeatureEnable = false;
            this.mPowerChange = false;
            this.mCurBrightness = VivoLightColorMatrixControl.this.getCurBrightnessFromSettings();
            String str = this.TAG;
            VSlog.d(str, "mCurBrightness=" + this.mCurBrightness);
        }

        private float getFactorOfBrightness(int brightness) {
            if (brightness > this.VIVO_MARTRIX_BOUND_LIGHT || brightness <= 0) {
                return 1.0f;
            }
            double factor = this.mColorMatrixAlphaMap.get(brightness).doubleValue();
            return (float) factor;
        }

        public void setMatrix(int cct) {
            float[] fArr = this.mMatrix;
            if (fArr.length != 16 || cct > this.VIVO_MARTRIX_BOUND_LIGHT) {
                VSlog.d(this.TAG, "The display transformation matrix must be 4x4");
                return;
            }
            Matrix.setIdentityM(fArr, 0);
            double factor = 1.0d;
            if (cct != 0) {
                factor = this.mColorMatrixAlphaMap.get(cct).doubleValue();
            }
            float[] fArr2 = this.mMatrix;
            fArr2[0] = (float) factor;
            fArr2[5] = (float) factor;
            fArr2[10] = (float) factor;
            String str = this.TAG;
            VSlog.d(str, "setMatrix brightnessMatrix R=" + this.mMatrix[0] + " G=" + this.mMatrix[5] + " B=" + this.mMatrix[10]);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setFactorMatrix(int factor) {
            float[] fArr = this.mMatrix;
            if (fArr.length != 16 || factor <= 0 || factor > 1000) {
                VSlog.d(this.TAG, "The display transformation matrix must be 4x4");
                return;
            }
            Matrix.setIdentityM(fArr, 0);
            double newFactor = factor / 1000.0d;
            float[] fArr2 = this.mMatrix;
            fArr2[0] = (float) newFactor;
            fArr2[5] = (float) newFactor;
            fArr2[10] = (float) newFactor;
            String str = this.TAG;
            VSlog.d(str, "setMatrix brightnessMatrix R=" + this.mMatrix[0] + " G=" + this.mMatrix[5] + " B=" + this.mMatrix[10]);
        }

        public float[] getMatrix() {
            String str = this.TAG;
            VSlog.d(str, "getMatrix isActivated=" + isActivated() + " brightnessMatrix R=" + this.mMatrix[0] + " G=" + this.mMatrix[5] + " B=" + this.mMatrix[10]);
            return (isActivated() || VivoLightColorMatrixControl.this.mLmDebug) ? this.mMatrix : this.MATRIX_IDENTITY;
        }

        public float[] getRawMatrix() {
            return this.mMatrix;
        }

        public int getLightBound() {
            return this.VIVO_MARTRIX_BOUND_LIGHT;
        }

        public synchronized void setActivated(Boolean activated) {
            if (activated == null) {
                super.setActivated((Boolean) null);
                return;
            }
            boolean activationStateChanged = activated.booleanValue() != isActivated();
            int brightness = getBrightnessSetting();
            if (isActivatedStateNotSet() || activationStateChanged) {
                super.setActivated(activated);
                VSlog.d(this.TAG, activated.booleanValue() ? "Turning on light martrix " : "Turning off light martrix ");
                if (activated.booleanValue()) {
                    setMatrix(brightness);
                } else {
                    Matrix.setIdentityM(this.mMatrix, 0);
                }
                String str = this.TAG;
                VSlog.d(str, "setActivated brightnessMatrix R=" + this.mMatrix[0] + " G=" + this.mMatrix[5] + " B=" + this.mMatrix[10]);
            }
        }

        public synchronized void setActivated(Boolean activated, int brightness) {
            if (activated == null) {
                super.setActivated((Boolean) null);
                return;
            }
            boolean activationStateChanged = activated.booleanValue() != isActivated();
            if (isActivatedStateNotSet() || activationStateChanged) {
                super.setActivated(activated);
                VSlog.d(this.TAG, activated.booleanValue() ? "Turning on light martrix " : "Turning off light martrix ");
                if (activated.booleanValue()) {
                    setMatrix(brightness);
                } else {
                    Matrix.setIdentityM(this.mMatrix, 0);
                }
                String str = this.TAG;
                VSlog.d(str, "setActivated brightnessMatrix R=" + this.mMatrix[0] + " G=" + this.mMatrix[5] + " B=" + this.mMatrix[10]);
            }
        }

        public void setFeatureEnable(Boolean isFeatureEnable) {
            this.mIsFeatureEnable = isFeatureEnable;
        }

        public boolean isFeatureEnable() {
            Boolean bool = this.mIsFeatureEnable;
            return bool != null && bool.booleanValue();
        }

        public int getLevel() {
            return this.mLevel;
        }

        public boolean isAvailable(Context context) {
            return true;
        }

        public void tearDown() {
            String str = this.TAG;
            VSlog.d(str, "tearDown: currentUser=" + VivoLightColorMatrixControl.this.mCurrentUser);
            onDisable(true);
        }

        public void onSetUser(int userHandle) {
            String str = this.TAG;
            VSlog.d(str, "onSetUser: userHandle=" + userHandle);
            VivoLightColorMatrixControl.this.mCurrentUser = userHandle;
        }

        public void onPowerOn() {
            synchronized (this.mLock) {
                VSlog.d(this.TAG, "onPowerOn");
                this.mPowerChange = true;
            }
        }

        public void onPowerOff() {
            synchronized (this.mLock) {
                VSlog.d(this.TAG, "onPowerOff");
                this.mPowerChange = true;
            }
        }

        public void onDisableInversionColor(float[] revertMatrix) {
            if (!isActivated() && getBrightnessSetting() > 0 && getBrightnessSetting() <= this.VIVO_MARTRIX_BOUND_LIGHT) {
                String str = this.TAG;
                VSlog.d(str, "lm enable mCurBrightness=" + getBrightnessSetting());
                endLmAnimator();
                setActivated(true);
                updateDtmColorMatrix();
                System.arraycopy(this.mMatrix, 0, this.mTempMatrix, 0, 16);
                if (revertMatrix != null) {
                    float[] invertMatrix = new float[16];
                    Matrix.invertM(invertMatrix, 0, revertMatrix, 0);
                    Matrix.multiplyMM(this.mTempMatrix, 0, invertMatrix, 0, this.mMatrix, 0);
                }
                String str2 = this.TAG;
                VSlog.d(str2, "onEnable mMatrix R=" + this.mMatrix[0] + " G=" + this.mMatrix[5] + " B=" + this.mMatrix[10]);
                String str3 = this.TAG;
                VSlog.d(str3, "onEnable mTempMatrix R=" + this.mTempMatrix[0] + " G=" + this.mTempMatrix[5] + " B=" + this.mTempMatrix[10]);
                setColorMatrixInternal(1, this.mCurBrightness, 4, this.mTempMatrix);
            }
            setFeatureEnable(true);
        }

        public float[] onEnableInversionColor() {
            System.arraycopy(this.mMatrix, 0, this.mTempMatrix, 0, 16);
            if (isActivated()) {
                String str = this.TAG;
                VSlog.d(str, "lm disbale mCurBrightness=" + getBrightnessSetting());
                endLmAnimator();
                setColorMatrixInternal(0, this.mCurBrightness, 3, this.mMatrix);
                setActivated(false);
                updateDtmColorMatrix();
            }
            setFeatureEnable(false);
            return this.mTempMatrix;
        }

        public void onEnable(float[] revertMatrix) {
            if (!isActivated() && getBrightnessSetting() > 0 && getBrightnessSetting() <= this.VIVO_MARTRIX_BOUND_LIGHT) {
                String str = this.TAG;
                VSlog.d(str, "lm enable mCurBrightness=" + getBrightnessSetting());
                endLmAnimator();
                setActivated(true);
                updateDtmColorMatrix();
                System.arraycopy(this.mMatrix, 0, this.mTempMatrix, 0, 16);
                if (revertMatrix != null) {
                    float[] invertMatrix = new float[16];
                    Matrix.invertM(invertMatrix, 0, revertMatrix, 0);
                    Matrix.multiplyMM(this.mTempMatrix, 0, invertMatrix, 0, this.mMatrix, 0);
                }
                String str2 = this.TAG;
                VSlog.d(str2, "onEnable mMatrix R=" + this.mMatrix[0] + " G=" + this.mMatrix[5] + " B=" + this.mMatrix[10]);
                String str3 = this.TAG;
                VSlog.d(str3, "onEnable mTempMatrix R=" + this.mTempMatrix[0] + " G=" + this.mTempMatrix[5] + " B=" + this.mTempMatrix[10]);
                setColorMatrixInternal(1, this.mCurBrightness, 5, this.mMatrix);
            }
            setFeatureEnable(true);
        }

        public float[] onDisable(boolean bUpdateSf) {
            System.arraycopy(this.mMatrix, 0, this.mTempMatrix, 0, 16);
            if (isActivated()) {
                String str = this.TAG;
                VSlog.d(str, "lm disbale mCurBrightness=" + getBrightnessSetting());
                endLmAnimator();
                if (bUpdateSf) {
                    setColorMatrixInternal(0, this.mCurBrightness, 1, this.mMatrix);
                }
                setActivated(false);
                updateDtmColorMatrix();
            }
            setFeatureEnable(false);
            String str2 = this.TAG;
            VSlog.d(str2, "onDisable mMatrix R=" + this.mMatrix[0] + " G=" + this.mMatrix[5] + " B=" + this.mMatrix[10]);
            String str3 = this.TAG;
            VSlog.d(str3, "onDisable mTempMatrix R=" + this.mTempMatrix[0] + " G=" + this.mTempMatrix[5] + " B=" + this.mTempMatrix[10]);
            return this.mTempMatrix;
        }

        public boolean isAvailableBrightness() {
            return getBrightnessSetting() >= 0 && getBrightnessSetting() <= this.VIVO_MARTRIX_BOUND_LIGHT;
        }

        private void updateColorMatrix(int brightness) {
            setMatrix(brightness);
        }

        private void applyColorMatrix(boolean bAnimated) {
            int i;
            int i2;
            String str = this.TAG;
            VSlog.d(str, "applyColorMatrix bAnimated=" + bAnimated);
            setData(this.mCurBrightness);
            if (this.mLevel == 401) {
                Handler handler = VivoLightColorMatrixControl.this.mColorDisplayService.mHandler;
                if (bAnimated) {
                    ColorDisplayService unused = VivoLightColorMatrixControl.this.mColorDisplayService;
                    i2 = 10;
                } else {
                    ColorDisplayService unused2 = VivoLightColorMatrixControl.this.mColorDisplayService;
                    i2 = 9;
                }
                handler.sendEmptyMessage(i2);
                return;
            }
            Handler handler2 = VivoLightColorMatrixControl.this.mColorDisplayService.mHandler;
            if (bAnimated) {
                ColorDisplayService unused3 = VivoLightColorMatrixControl.this.mColorDisplayService;
                i = 7;
            } else {
                ColorDisplayService unused4 = VivoLightColorMatrixControl.this.mColorDisplayService;
                i = 6;
            }
            handler2.sendEmptyMessage(i);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void updateDtmColorMatrix() {
            VSlog.d(this.TAG, "updateDtmColorMatrix");
            if (VivoLightColorMatrixControl.this.mVivoColorManager != null && VivoLightColorMatrixControl.this.mVivoColorManager.mDtm != null) {
                VivoLightColorMatrixControl.this.mVivoColorManager.mDtm.updateColorMatrix(this.mLevel, getMatrix());
            } else if (this.mLevel == 401) {
                Handler handler = VivoLightColorMatrixControl.this.mColorDisplayService.mHandler;
                ColorDisplayService unused = VivoLightColorMatrixControl.this.mColorDisplayService;
                handler.sendEmptyMessage(11);
            } else {
                Handler handler2 = VivoLightColorMatrixControl.this.mColorDisplayService.mHandler;
                ColorDisplayService unused2 = VivoLightColorMatrixControl.this.mColorDisplayService;
                handler2.sendEmptyMessage(8);
            }
        }

        private boolean isNeedDimming(int lastBrightness, int brightness) {
            if (lastBrightness == 0 && brightness > 0) {
                String str = this.TAG;
                VSlog.d(str, "isNeedDimming from " + lastBrightness + " to " + brightness);
                return false;
            } else if (lastBrightness > 0 && brightness == 0) {
                String str2 = this.TAG;
                VSlog.d(str2, "isNeedDimming from " + lastBrightness + " to " + brightness);
                return false;
            } else if (brightness != lastBrightness) {
                if (VivoLightColorMatrixControl.this.mSupportLm || VivoLightColorMatrixControl.this.mSupportDcMatrix) {
                    boolean bUdHbmStatus = getUdHbmStatus();
                    boolean bHbmNode = getUdHbmNode();
                    String str3 = this.TAG;
                    VSlog.d(str3, "onBrightnessChanged bUdHbmStatus=" + bUdHbmStatus + " bHbmNode=" + bHbmNode);
                    return (bUdHbmStatus || bHbmNode) ? false : true;
                }
                return true;
            } else {
                String str4 = this.TAG;
                VSlog.d(str4, "isNeedDimming from " + lastBrightness + " to " + brightness);
                return false;
            }
        }

        public void onBrightnessChanged(int brightness) {
            VivoLightColorMatrixControl.this.isAccessiblityDcEnabled();
            int lastBrightness = this.mCurBrightness;
            if (this.mCurBrightness == brightness) {
                String str = this.TAG;
                VSlog.d(str, "onBrightnessChanged ignore same brightness " + this.mCurBrightness);
                return;
            }
            this.mCurBrightness = brightness;
            String str2 = this.TAG;
            VSlog.d(str2, "onBrightnessChanged brightness=" + brightness + " mPowerChange=" + this.mPowerChange);
            if (brightness >= 0) {
                if (!VivoLightColorMatrixControl.this.mSupportDcMatrix || this.mIsFeatureEnable.booleanValue()) {
                    if (VivoLightColorMatrixControl.this.mSupportDcLayer && VivoLightColorMatrixControl.this.mDcEnable == 1) {
                        return;
                    }
                    boolean bAnimated = isNeedDimming(lastBrightness, brightness);
                    String str3 = this.TAG;
                    VSlog.d(str3, "onBrightnessChanged bAnimated=" + bAnimated);
                    float factor = getFactorOfBrightness(brightness);
                    if (brightness >= this.VIVO_MARTRIX_BOUND_LIGHT || brightness == 0 || factor == 1.0d) {
                        if (brightness == 0) {
                            if (isActivated()) {
                                clearLightBound();
                                this.mDisableLmAgain = true;
                            }
                            endLmAnimator(new EndAniPowerOff(), brightness);
                            return;
                        }
                        if (isActivated() || (this.mDisableLmAgain && hasLightBound())) {
                            setActivated(false);
                            setColorMatrixInternal(0, brightness, 2, this.mMatrix);
                            applyColorMatrix(bAnimated);
                            clearLightBound();
                        }
                        if (this.mDisableLmAgain) {
                            this.mDisableLmAgain = false;
                        }
                    } else if (lastBrightness == 0 && brightness > 0) {
                        endLmAnimator(new EndAniPowerOn(), brightness);
                    } else {
                        if (!isActivated()) {
                            setActivated(true);
                            setLightBound();
                        } else {
                            updateColorMatrix(brightness);
                        }
                        setColorMatrixInternal(1, brightness, 2, this.mMatrix);
                        applyColorMatrix(bAnimated);
                    }
                }
            }
        }

        public int getBrightnessSetting() {
            return this.mCurBrightness;
        }

        private boolean getUdHbmStatus() {
            IBinder flinger = ServiceManager.getService(VivoLightColorMatrixControl.SURFACE_FLINGER);
            if (flinger != null) {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                try {
                    flinger.transact(SURFACE_FLINGER_TRANSACTION_GET_UD_HBM_STATUS, data, reply, 0);
                    return reply.readBoolean();
                } catch (Exception e) {
                    VSlog.e(this.TAG, "Failed to set eye color transform");
                } finally {
                    data.recycle();
                    reply.recycle();
                }
            }
            return false;
        }

        private boolean getUdHbmNode() {
            int oled_hbm;
            String oled_hbm_str = VivoLcmUtils.readKernelNode(OLED_HBM_PATH);
            int iLen = 0;
            if (oled_hbm_str != null) {
                oled_hbm_str = oled_hbm_str.replace("\n", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                iLen = oled_hbm_str.length() <= 4 ? oled_hbm_str.length() : 4;
            }
            if (oled_hbm_str != null) {
                try {
                    oled_hbm = Integer.parseInt(oled_hbm_str.substring(0, iLen), 16);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    return false;
                }
            } else {
                oled_hbm = 0;
            }
            if (oled_hbm == 0 || oled_hbm == 5 || oled_hbm == 7 || oled_hbm == 8) {
                return false;
            }
            return true;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setColorMatrixInternal(int enable, int brightness, int flag, float[] m) {
            IBinder flinger = ServiceManager.getService(VivoLightColorMatrixControl.SURFACE_FLINGER);
            if (flinger != null) {
                Parcel data = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                if (m != null) {
                    data.writeInt(0);
                    data.writeInt(this.mLevel);
                    data.writeInt(-1);
                    data.writeInt(enable);
                    data.writeInt(brightness);
                    data.writeInt(flag);
                    for (int i = 0; i < 16; i++) {
                        data.writeFloat(m[i]);
                    }
                } else {
                    data.writeInt(0);
                }
                try {
                    try {
                        flinger.transact(SURFACE_FLINGER_TRANSACTION_SET_LIGHT_COLOR_MATRIX, data, null, 0);
                    } catch (Exception e) {
                        VSlog.e(this.TAG, "Failed to set eye color transform");
                    }
                } finally {
                    data.recycle();
                }
            }
        }

        /* loaded from: classes.dex */
        public class EndAniPowerOff implements IEndAniCallBack {
            public EndAniPowerOff() {
            }

            @Override // com.android.server.display.color.VivoLightColorMatrixControl.IEndAniCallBack
            public void run(int brightness) {
                if (LightTintController.this.isActivated()) {
                    VSlog.d(LightTintController.this.TAG, "force disable lm");
                    LightTintController lightTintController = LightTintController.this;
                    lightTintController.setColorMatrixInternal(0, brightness, 1, lightTintController.mMatrix);
                    LightTintController.this.setActivated(false, brightness);
                    LightTintController.this.updateDtmColorMatrix();
                }
            }
        }

        /* loaded from: classes.dex */
        public class EndAniPowerOn implements IEndAniCallBack {
            public EndAniPowerOn() {
            }

            @Override // com.android.server.display.color.VivoLightColorMatrixControl.IEndAniCallBack
            public void run(int brightness) {
                if (brightness == 0) {
                    VSlog.e(LightTintController.this.TAG, "EndAniPowerOn: brightness 0 is error");
                } else if (!LightTintController.this.isActivated()) {
                    VSlog.d(LightTintController.this.TAG, "force enable lm");
                    LightTintController.this.setActivated(true, brightness);
                    LightTintController.this.updateDtmColorMatrix();
                    LightTintController lightTintController = LightTintController.this;
                    lightTintController.setColorMatrixInternal(1, brightness, 5, lightTintController.mMatrix);
                }
            }
        }

        private void endLmAnimator(final IEndAniCallBack endAniCallBack, final int brightness) {
            if (Looper.myLooper() != VivoLightColorMatrixControl.this.mColorDisplayService.mHandler.getLooper()) {
                VivoLightColorMatrixControl.this.mColorDisplayService.mHandler.post(new Runnable() { // from class: com.android.server.display.color.VivoLightColorMatrixControl.LightTintController.1
                    @Override // java.lang.Runnable
                    public void run() {
                        String str = LightTintController.this.TAG;
                        VSlog.d(str, "endAnimator in other thread brightness=" + brightness);
                        LightTintController.this.endAnimator();
                        IEndAniCallBack iEndAniCallBack = endAniCallBack;
                        if (iEndAniCallBack != null) {
                            iEndAniCallBack.run(brightness);
                        }
                    }
                });
                return;
            }
            String str = this.TAG;
            VSlog.d(str, "endAnimator in display service thread brightness=" + brightness);
            endAnimator();
            if (endAniCallBack != null) {
                endAniCallBack.run(brightness);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void endLmAnimator() {
            if (Looper.myLooper() != VivoLightColorMatrixControl.this.mColorDisplayService.mHandler.getLooper()) {
                VivoLightColorMatrixControl.this.mColorDisplayService.mHandler.post(new Runnable() { // from class: com.android.server.display.color.VivoLightColorMatrixControl.LightTintController.2
                    @Override // java.lang.Runnable
                    public void run() {
                        VSlog.d(LightTintController.this.TAG, "endAnimator in other thread");
                        LightTintController.this.endAnimator();
                    }
                });
                return;
            }
            VSlog.d(this.TAG, "endAnimator in display service thread");
            endAnimator();
        }

        private void setLightBound() {
            String dc_dimming_str = VivoLcmUtils.readKernelNode(DC_DIMMING_PATH);
            int iLen = 0;
            if (dc_dimming_str != null) {
                dc_dimming_str = dc_dimming_str.replace("\n", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                iLen = dc_dimming_str.length() <= 4 ? dc_dimming_str.length() : 4;
            }
            String str = this.TAG;
            VSlog.d(str, "setLightBound: real brightness=" + getBrightnessSetting() + " dc_dimming_str=" + dc_dimming_str);
            int dc_diming = 0;
            if (dc_dimming_str != null) {
                try {
                    dc_diming = Integer.parseInt(dc_dimming_str.substring(0, iLen), 16);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    return;
                }
            }
            if (dc_diming != this.VIVO_MARTRIX_BOUND_LIGHT && getBrightnessSetting() <= this.VIVO_MARTRIX_BOUND_LIGHT) {
                VivoLcmUtils.writeKernelNode(DC_DIMMING_PATH, Integer.toString(this.VIVO_MARTRIX_BOUND_LIGHT));
                String str2 = this.TAG;
                VSlog.d(str2, "set dcdiming=" + this.VIVO_MARTRIX_BOUND_LIGHT);
            }
        }

        private void clearLightBound() {
            int dc_diming;
            String dc_dimming_str = VivoLcmUtils.readKernelNode(DC_DIMMING_PATH);
            int iLen = 0;
            if (dc_dimming_str != null) {
                dc_dimming_str = dc_dimming_str.replace("\n", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                iLen = dc_dimming_str.length() <= 4 ? dc_dimming_str.length() : 4;
            }
            VSlog.d(this.TAG, "clearLightBound: real brightness=" + getBrightnessSetting() + " dc_dimming_str=" + dc_dimming_str);
            if (dc_dimming_str != null) {
                try {
                    dc_diming = Integer.parseInt(dc_dimming_str.substring(0, iLen), 16);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    return;
                }
            } else {
                dc_diming = 0;
            }
            if (dc_diming != 0) {
                VivoLcmUtils.writeKernelNode(DC_DIMMING_PATH, Integer.toString(0));
                VSlog.d(this.TAG, "set dcdiming =0");
            }
        }

        private boolean hasLightBound() {
            int dc_diming;
            String dc_dimming_str = VivoLcmUtils.readKernelNode(DC_DIMMING_PATH);
            int iLen = 0;
            if (dc_dimming_str != null) {
                dc_dimming_str = dc_dimming_str.replace("\n", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                iLen = dc_dimming_str.length() <= 4 ? dc_dimming_str.length() : 4;
            }
            VSlog.d(this.TAG, "hasLightBound: dc_dimming_str=" + dc_dimming_str);
            if (dc_dimming_str != null) {
                try {
                    dc_diming = Integer.parseInt(dc_dimming_str.substring(0, iLen), 16);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            } else {
                dc_diming = 0;
            }
            if (dc_diming == 0) {
                return false;
            }
            return true;
        }

        private void setLightBoundForSf() {
            IBinder flinger = ServiceManager.getService(VivoLightColorMatrixControl.SURFACE_FLINGER);
            if (flinger != null) {
                Parcel data = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                data.writeInt(this.mLevel);
                data.writeInt(this.VIVO_MARTRIX_BOUND_LIGHT);
                try {
                    try {
                        flinger.transact(SURFACE_FLINGER_TRANSACTION_SET_LIGHT_BOUND, data, null, 0);
                    } catch (Exception e) {
                        VSlog.e(this.TAG, "Failed to set setLightBoundForSf");
                    }
                } finally {
                    data.recycle();
                }
            }
        }

        /* JADX WARN: Removed duplicated region for block: B:28:0x0099 A[RETURN] */
        /* JADX WARN: Removed duplicated region for block: B:29:0x009a  */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
            To view partially-correct code enable 'Show inconsistent code' option in preferences
        */
        private java.lang.String readJson(java.lang.String r11) {
            /*
                r10 = this;
                java.lang.String r0 = "Close conf file got exception: "
                java.io.File r1 = new java.io.File
                r1.<init>(r11)
                boolean r2 = r1.exists()
                r3 = 0
                if (r2 != 0) goto L25
                java.lang.String r0 = r10.TAG
                java.lang.StringBuilder r2 = new java.lang.StringBuilder
                r2.<init>()
                java.lang.String r4 = "Can't find file:"
                r2.append(r4)
                r2.append(r11)
                java.lang.String r2 = r2.toString()
                vivo.util.VSlog.e(r0, r2)
                return r3
            L25:
                long r4 = r1.length()
                int r2 = (int) r4
                r4 = 1
                if (r2 >= r4) goto L44
                java.lang.String r0 = r10.TAG
                java.lang.StringBuilder r4 = new java.lang.StringBuilder
                r4.<init>()
                java.lang.String r5 = "Empty file:"
                r4.append(r5)
                r4.append(r11)
                java.lang.String r4 = r4.toString()
                vivo.util.VSlog.e(r0, r4)
                return r3
            L44:
                r4 = 0
                r5 = 0
                java.io.FileInputStream r6 = new java.io.FileInputStream     // Catch: java.lang.Throwable -> L6d java.lang.Exception -> L6f
                r6.<init>(r1)     // Catch: java.lang.Throwable -> L6d java.lang.Exception -> L6f
                r4 = r6
                byte[] r6 = new byte[r2]     // Catch: java.lang.Throwable -> L6d java.lang.Exception -> L6f
                r5 = r6
                r4.read(r5)     // Catch: java.lang.Throwable -> L6d java.lang.Exception -> L6f
                r4.close()     // Catch: java.lang.Exception -> L57
            L56:
                goto L97
            L57:
                r6 = move-exception
                java.lang.String r7 = r10.TAG
                java.lang.StringBuilder r8 = new java.lang.StringBuilder
                r8.<init>()
            L5f:
                r8.append(r0)
                r8.append(r6)
                java.lang.String r0 = r8.toString()
                vivo.util.VSlog.e(r7, r0)
                goto L56
            L6d:
                r3 = move-exception
                goto La1
            L6f:
                r6 = move-exception
                r5 = 0
                java.lang.String r7 = r10.TAG     // Catch: java.lang.Throwable -> L6d
                java.lang.StringBuilder r8 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L6d
                r8.<init>()     // Catch: java.lang.Throwable -> L6d
                java.lang.String r9 = "New/Read conf file got exception:"
                r8.append(r9)     // Catch: java.lang.Throwable -> L6d
                r8.append(r6)     // Catch: java.lang.Throwable -> L6d
                java.lang.String r8 = r8.toString()     // Catch: java.lang.Throwable -> L6d
                vivo.util.VSlog.e(r7, r8)     // Catch: java.lang.Throwable -> L6d
                if (r4 == 0) goto L97
                r4.close()     // Catch: java.lang.Exception -> L8e
                goto L56
            L8e:
                r6 = move-exception
                java.lang.String r7 = r10.TAG
                java.lang.StringBuilder r8 = new java.lang.StringBuilder
                r8.<init>()
                goto L5f
            L97:
                if (r5 != 0) goto L9a
                return r3
            L9a:
                java.lang.String r0 = new java.lang.String
                r0.<init>(r5)
                r3 = 0
                return r0
            La1:
                if (r4 == 0) goto Lbc
                r4.close()     // Catch: java.lang.Exception -> La7
                goto Lbc
            La7:
                r6 = move-exception
                java.lang.String r7 = r10.TAG
                java.lang.StringBuilder r8 = new java.lang.StringBuilder
                r8.<init>()
                r8.append(r0)
                r8.append(r6)
                java.lang.String r0 = r8.toString()
                vivo.util.VSlog.e(r7, r0)
            Lbc:
                throw r3
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.server.display.color.VivoLightColorMatrixControl.LightTintController.readJson(java.lang.String):java.lang.String");
        }

        private boolean parseConfigAlphaJson(String JsonData, boolean exactMatch) {
            JSONArray acColorMatrix;
            String str = this.TAG;
            VSlog.d(str, "parseConfigAlphaJson exactMatch=" + exactMatch);
            if (JsonData == null) {
                VSlog.e(this.TAG, "parseConf conf null, use default hard code conf.");
                return false;
            }
            try {
                JSONArray confList = new JSONArray(JsonData);
                String str2 = this.TAG;
                VSlog.d(str2, "configlist length = " + confList.length());
                for (int i = 0; i < confList.length() - 1; i++) {
                    JSONObject obj = confList.getJSONObject(i);
                    if (obj == null) {
                        String str3 = this.TAG;
                        VSlog.e(str3, "parseConf found empty object index=" + i);
                    } else if (!obj.has("project")) {
                        VSlog.e(this.TAG, "parseConf missing project");
                    } else {
                        JSONArray objProjs = obj.getJSONArray("project");
                        if (objProjs != null && objProjs.length() >= 1) {
                            for (int j = 0; j < objProjs.length(); j++) {
                                if ((exactMatch && this.mProductModel.equals(objProjs.getString(j))) || (!exactMatch && this.mLowerProductModel.startsWith(objProjs.getString(j)))) {
                                    if (this.mLevel == 401) {
                                        acColorMatrix = obj.getJSONArray("DCDimmingAlphaMap");
                                    } else {
                                        acColorMatrix = obj.getJSONArray("ColorMatrixAlphaMap");
                                    }
                                    if (acColorMatrix != null && acColorMatrix.length() >= 1) {
                                        this.VIVO_MARTRIX_BOUND_LIGHT = acColorMatrix.length() - 1;
                                        for (int k = 0; k < acColorMatrix.length(); k++) {
                                            this.mColorMatrixAlphaMap.add(Double.valueOf(acColorMatrix.getString(k)));
                                        }
                                        int k2 = this.mLevel;
                                        if (k2 == 401) {
                                            String str4 = this.TAG;
                                            VSlog.d(str4, "Use LcmConfig DCDimmingAlphaMap, VIVO_MARTRIX_BOUND_LIGHT=" + this.VIVO_MARTRIX_BOUND_LIGHT);
                                        } else {
                                            String str5 = this.TAG;
                                            VSlog.d(str5, "Use LcmConfig ColorMatrixAlphaMap, VIVO_MARTRIX_BOUND_LIGHT=" + this.VIVO_MARTRIX_BOUND_LIGHT);
                                        }
                                        return true;
                                    }
                                    VSlog.e(this.TAG, "parse ColorMatrixAlphaMap failed");
                                }
                            }
                            continue;
                        }
                        VSlog.e(this.TAG, "parseConf found null/empty project");
                    }
                }
                return false;
            } catch (JSONException e) {
                String str6 = this.TAG;
                VSlog.e(str6, "Failed to parse conf file,got exception:" + e);
                return false;
            }
        }

        private void useDefaultColorMatrixAlphaMap() {
            Double valueOf = Double.valueOf(0.35d);
            Double valueOf2 = Double.valueOf(0.85d);
            Double valueOf3 = Double.valueOf(0.925d);
            Double valueOf4 = Double.valueOf(1.0d);
            Double[] acColorMatrixAlphaMap = {Double.valueOf(0.0d), valueOf, valueOf, valueOf, valueOf, valueOf, valueOf, valueOf, valueOf, valueOf, valueOf, Double.valueOf(0.354d), Double.valueOf(0.359d), Double.valueOf(0.364d), Double.valueOf(0.369d), Double.valueOf(0.374d), Double.valueOf(0.378d), Double.valueOf(0.381d), Double.valueOf(0.385d), Double.valueOf(0.386d), Double.valueOf(0.387d), Double.valueOf(0.388d), Double.valueOf(0.389d), Double.valueOf(0.39d), Double.valueOf(0.391d), Double.valueOf(0.392d), Double.valueOf(0.393d), Double.valueOf(0.394d), Double.valueOf(0.395d), Double.valueOf(0.396d), Double.valueOf(0.397d), Double.valueOf(0.398d), Double.valueOf(0.399d), Double.valueOf(0.4d), Double.valueOf(0.401d), Double.valueOf(0.403d), Double.valueOf(0.405d), Double.valueOf(0.407d), Double.valueOf(0.409d), Double.valueOf(0.411d), Double.valueOf(0.413d), Double.valueOf(0.415d), Double.valueOf(0.417d), Double.valueOf(0.419d), Double.valueOf(0.421d), Double.valueOf(0.423d), Double.valueOf(0.425d), Double.valueOf(0.427d), Double.valueOf(0.429d), Double.valueOf(0.431d), Double.valueOf(0.433d), Double.valueOf(0.435d), Double.valueOf(0.437d), Double.valueOf(0.439d), Double.valueOf(0.441d), Double.valueOf(0.443d), Double.valueOf(0.445d), Double.valueOf(0.447d), Double.valueOf(0.449d), Double.valueOf(0.451d), Double.valueOf(0.453d), Double.valueOf(0.455d), Double.valueOf(0.457d), Double.valueOf(0.459d), Double.valueOf(0.461d), Double.valueOf(0.463d), Double.valueOf(0.465d), Double.valueOf(0.467d), Double.valueOf(0.469d), Double.valueOf(0.471d), Double.valueOf(0.473d), Double.valueOf(0.475d), Double.valueOf(0.477d), Double.valueOf(0.479d), Double.valueOf(0.481d), Double.valueOf(0.483d), Double.valueOf(0.485d), Double.valueOf(0.487d), Double.valueOf(0.489d), Double.valueOf(0.491d), Double.valueOf(0.493d), Double.valueOf(0.495d), Double.valueOf(0.497d), Double.valueOf(0.499d), Double.valueOf(0.501d), Double.valueOf(0.503d), Double.valueOf(0.505d), Double.valueOf(0.507d), Double.valueOf(0.509d), Double.valueOf(0.511d), Double.valueOf(0.513d), Double.valueOf(0.515d), Double.valueOf(0.517d), Double.valueOf(0.519d), Double.valueOf(0.521d), Double.valueOf(0.523d), Double.valueOf(0.525d), Double.valueOf(0.526d), Double.valueOf(0.527d), Double.valueOf(0.528d), Double.valueOf(0.529d), Double.valueOf(0.53d), Double.valueOf(0.532d), Double.valueOf(0.537d), Double.valueOf(0.542d), Double.valueOf(0.547d), Double.valueOf(0.552d), Double.valueOf(0.557d), Double.valueOf(0.562d), Double.valueOf(0.567d), Double.valueOf(0.572d), Double.valueOf(0.577d), Double.valueOf(0.582d), Double.valueOf(0.587d), Double.valueOf(0.592d), Double.valueOf(0.597d), Double.valueOf(0.602d), Double.valueOf(0.607d), Double.valueOf(0.612d), Double.valueOf(0.617d), Double.valueOf(0.622d), Double.valueOf(0.627d), Double.valueOf(0.632d), Double.valueOf(0.637d), Double.valueOf(0.641d), Double.valueOf(0.645d), Double.valueOf(0.649d), Double.valueOf(0.653d), Double.valueOf(0.657d), Double.valueOf(0.661d), Double.valueOf(0.665d), Double.valueOf(0.669d), Double.valueOf(0.673d), Double.valueOf(0.677d), Double.valueOf(0.681d), Double.valueOf(0.685d), Double.valueOf(0.689d), Double.valueOf(0.693d), Double.valueOf(0.697d), Double.valueOf(0.701d), Double.valueOf(0.703d), Double.valueOf(0.705d), Double.valueOf(0.707d), Double.valueOf(0.709d), Double.valueOf(0.711d), Double.valueOf(0.713d), Double.valueOf(0.718d), Double.valueOf(0.723d), Double.valueOf(0.728d), Double.valueOf(0.733d), Double.valueOf(0.738d), Double.valueOf(0.743d), Double.valueOf(0.748d), Double.valueOf(0.753d), Double.valueOf(0.758d), Double.valueOf(0.763d), Double.valueOf(0.768d), Double.valueOf(0.773d), Double.valueOf(0.778d), Double.valueOf(0.784d), Double.valueOf(0.79d), Double.valueOf(0.796d), Double.valueOf(0.802d), Double.valueOf(0.808d), Double.valueOf(0.814d), Double.valueOf(0.82d), Double.valueOf(0.826d), Double.valueOf(0.832d), Double.valueOf(0.838d), Double.valueOf(0.844d), valueOf2, valueOf2, Double.valueOf(0.855d), Double.valueOf(0.86d), Double.valueOf(0.865d), Double.valueOf(0.87d), Double.valueOf(0.875d), Double.valueOf(0.88d), Double.valueOf(0.885d), Double.valueOf(0.89d), Double.valueOf(0.895d), Double.valueOf(0.9d), Double.valueOf(0.905d), Double.valueOf(0.91d), Double.valueOf(0.915d), Double.valueOf(0.92d), valueOf3, valueOf3, Double.valueOf(0.93d), Double.valueOf(0.935d), Double.valueOf(0.94d), Double.valueOf(0.945d), Double.valueOf(0.95d), Double.valueOf(0.955d), Double.valueOf(0.96d), Double.valueOf(0.965d), Double.valueOf(0.97d), Double.valueOf(0.975d), Double.valueOf(0.98d), Double.valueOf(0.985d), Double.valueOf(0.99d), Double.valueOf(0.995d), valueOf4, valueOf4, valueOf4, valueOf4, valueOf4, valueOf4, valueOf4};
            this.mColorMatrixAlphaMap = new Vector<>(Arrays.asList(acColorMatrixAlphaMap));
            this.VIVO_MARTRIX_BOUND_LIGHT = 208;
            String str = this.TAG;
            VSlog.d(str, "Use default ColorMatrixAlphaMap, VIVO_MARTRIX_BOUND_LIGHT=" + this.VIVO_MARTRIX_BOUND_LIGHT);
        }
    }
}