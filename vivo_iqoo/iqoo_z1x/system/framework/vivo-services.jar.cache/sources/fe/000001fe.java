package com.android.server.display.color;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.util.Log;
import vivo.common.IExynosDisplaySolutionManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class ExynosDisplaySolutionManagerService extends IExynosDisplaySolutionManager.Stub {
    private static final String ATC_MODE_ENABLED = "atc_mode_enabled";
    private static final String TAG = "ExynosDisplaySolutionManagerService";
    public static final String serviceName = "exynos_display";
    private String mColorModeName;
    private final Context mContext;
    private ExynosDisplaySolution mExynosDisplay;
    private ExynosDisplayATC mExynosDisplayATC;
    private ExynosDisplayColor mExynosDisplayColor;
    private ExynosDisplayFactoryHDR mExynosDisplayFactoryHDR;
    private ExynosDisplayPanel mExynosDisplayPanel;
    private ExynosDisplayTune mExynosDisplayTune;
    private HandlerThread mHandlerThread;
    private SettingsObserver mSettingsObserver;
    private final boolean DEBUG = "eng".equals(Build.TYPE);
    private final Object mLock = new Object();
    private ExynosDisplayFactory mExynosDisplayFactory = null;
    private ExynosDisplayFactoryModeSet mExynosDisplayFactoryModeSet = null;
    private boolean mBootCompleted = false;
    private boolean mAtcEnableSetting = false;

    public ExynosDisplaySolutionManagerService(Context context) {
        this.mExynosDisplay = null;
        this.mExynosDisplayTune = null;
        this.mExynosDisplayPanel = null;
        this.mExynosDisplayColor = null;
        this.mExynosDisplayATC = null;
        this.mExynosDisplayFactoryHDR = null;
        this.mContext = context;
        this.mExynosDisplay = new ExynosDisplaySolution(context);
        this.mExynosDisplayTune = new ExynosDisplayTune();
        this.mExynosDisplayPanel = new ExynosDisplayPanel();
        this.mExynosDisplayColor = new ExynosDisplayColor();
        this.mExynosDisplayATC = ExynosDisplayATC.getInstance(context);
        this.mExynosDisplayFactoryHDR = new ExynosDisplayFactoryHDR(context);
        this.mSettingsObserver = new SettingsObserver(context.getMainThreadHandler());
        ContentResolver resolver = this.mContext.getContentResolver();
        resolver.registerContentObserver(Settings.System.getUriFor(ATC_MODE_ENABLED), false, this.mSettingsObserver, -1);
        settingChanged();
    }

    public void setDisplayFeature(String arg0, int arg1, int arg2, String arg3) {
        synchronized (this.mLock) {
            VSlog.d(TAG, "setDisplayFeature(): " + arg0 + "  " + arg1 + "  " + arg2 + "  " + arg3);
            if (arg0.equals("setDisplayColorFeature")) {
                int timer_count = this.mExynosDisplayFactory.getCountDownTimerCount();
                if (this.mBootCompleted && timer_count <= 0) {
                    this.mExynosDisplayColor.setDisplayColorFeature(arg1, arg2, arg3);
                    return;
                }
                Log.e(TAG, "setDisplayColorFeature is not ready: mBootCompleted=" + this.mBootCompleted + ", timer_count=" + timer_count);
                return;
            }
            boolean onoff = false;
            if (arg0.equals("dqe_tune")) {
                if (arg1 == 0) {
                    if (arg2 != 0) {
                        onoff = true;
                    }
                    this.mExynosDisplayTune.enableTuneDQE(onoff);
                }
            } else if (arg0.equals("atc_user")) {
                if (arg1 == 0) {
                    if (arg2 != 0) {
                        onoff = true;
                    }
                    boolean onoff2 = onoff;
                    this.mExynosDisplayATC.enableATC(onoff2);
                    this.mExynosDisplayATC.enableLightSensor(onoff2);
                }
            } else if (arg0.equals("atc_tune")) {
                if (arg1 == 0) {
                    boolean onoff3 = arg2 != 0;
                    this.mExynosDisplayATC.enableATCTuneMode(onoff3);
                    this.mExynosDisplayATC.enableATC(onoff3);
                    this.mExynosDisplayATC.enableLightSensor(onoff3);
                }
                if (arg1 == 8) {
                    if (arg2 != 0) {
                        onoff = true;
                    }
                    this.mExynosDisplayATC.enableATCTuneMode(onoff);
                }
                if (arg1 == 9) {
                    this.mExynosDisplayATC.enableATCTuneMode(true);
                    this.mExynosDisplayATC.setLastLuminance(arg2);
                }
            } else if (arg0.equals("atc_timer")) {
                this.mExynosDisplayATC.enableATCTuneMode(true);
                this.mExynosDisplayATC.setCountDownTimer(arg1, arg2);
            } else if (arg0.equals("factory_timer")) {
                this.mExynosDisplayFactory.startCountDownTimer(null, "mode1");
            }
        }
    }

    public void onScreenStatusChanged(Context context, Intent intent) {
        String action = intent.getAction();
        if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
            this.mBootCompleted = true;
            boolean z = this.mAtcEnableSetting;
            if (z) {
                this.mExynosDisplayATC.enableATC(z);
                this.mExynosDisplayATC.enableLightSensor(true);
            }
        } else if ("android.intent.action.SCREEN_ON".equals(action)) {
            this.mExynosDisplayATC.enableScreenAction(true);
        } else if ("android.intent.action.SCREEN_OFF".equals(action)) {
            this.mExynosDisplayATC.enableScreenAction(false);
        } else {
            "android.intent.action.USER_PRESENT".equals(action);
        }
        this.mExynosDisplay.onScreenStatusChanged(context, intent);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void settingChanged() {
        ContentResolver resolver = this.mContext.getContentResolver();
        boolean atcEnableSetting = Settings.System.getIntForUser(resolver, ATC_MODE_ENABLED, 0, -2) != 0;
        if (this.mAtcEnableSetting != atcEnableSetting && this.mBootCompleted) {
            this.mExynosDisplayATC.enableATC(atcEnableSetting);
            this.mExynosDisplayATC.enableLightSensor(atcEnableSetting);
        }
        this.mAtcEnableSetting = atcEnableSetting;
    }

    /* loaded from: classes.dex */
    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            ExynosDisplaySolutionManagerService.this.settingChanged();
        }
    }
}