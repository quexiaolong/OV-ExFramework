package com.android.server.display.color;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class ExynosDisplaySolution {
    private static final String DQE_TUNE_ENABLED = "dqe_tune_enabled";
    private static final String TAG = "ExynosDisplaySolution";
    private final boolean DEBUG;
    private boolean mBootCompleted;
    private final Context mContext;
    private ExynosDisplayColor mExynosDisplayColor;
    private ExynosDisplayPanel mExynosDisplayPanel;
    private ExynosDisplayTune mExynosDisplayTune;
    private SettingsObserver mSettingsObserver;
    private boolean mTuneEnableSetting;

    public ExynosDisplaySolution(Context context) {
        this.DEBUG = "eng".equals(Build.TYPE);
        this.mExynosDisplayPanel = null;
        this.mExynosDisplayColor = null;
        this.mExynosDisplayTune = null;
        this.mBootCompleted = false;
        this.mTuneEnableSetting = false;
        this.mContext = context;
        this.mExynosDisplayTune = new ExynosDisplayTune();
        this.mSettingsObserver = new SettingsObserver(context.getMainThreadHandler());
        ContentResolver resolver = this.mContext.getContentResolver();
        resolver.registerContentObserver(Settings.System.getUriFor(DQE_TUNE_ENABLED), false, this.mSettingsObserver, -1);
        settingChanged();
        VSlog.d(TAG, "ExynosDisplaySolution constructor");
    }

    public ExynosDisplaySolution(Context context, String name) {
        this.DEBUG = "eng".equals(Build.TYPE);
        this.mExynosDisplayPanel = null;
        this.mExynosDisplayColor = null;
        this.mExynosDisplayTune = null;
        this.mBootCompleted = false;
        this.mTuneEnableSetting = false;
        this.mContext = context;
        this.mExynosDisplayPanel = new ExynosDisplayPanel();
        this.mExynosDisplayColor = new ExynosDisplayColor();
    }

    public void setCABCMode(int value) {
        this.mExynosDisplayPanel.setCABCMode(value);
    }

    public void setColorMode(int value) {
        this.mExynosDisplayColor.setColorMode(value);
    }

    public void onScreenStatusChanged(Context context, Intent intent) {
        String action = intent.getAction();
        if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
            this.mBootCompleted = true;
            if (this.mTuneEnableSetting) {
                this.mExynosDisplayTune.enableTuneTimer(true);
            }
        } else if (!"android.intent.action.SCREEN_ON".equals(action) && !"android.intent.action.SCREEN_OFF".equals(action)) {
            "android.intent.action.USER_PRESENT".equals(action);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void settingChanged() {
        ContentResolver resolver = this.mContext.getContentResolver();
        boolean tuneEnableSetting = Settings.System.getIntForUser(resolver, DQE_TUNE_ENABLED, 0, -2) != 0;
        if (this.mTuneEnableSetting != tuneEnableSetting && this.mBootCompleted) {
            this.mExynosDisplayTune.enableTuneTimer(tuneEnableSetting);
        }
        this.mTuneEnableSetting = tuneEnableSetting;
    }

    /* loaded from: classes.dex */
    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            ExynosDisplaySolution.this.settingChanged();
        }
    }
}