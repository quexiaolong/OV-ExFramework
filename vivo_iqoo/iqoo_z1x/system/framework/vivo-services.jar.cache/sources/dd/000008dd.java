package com.vivo.services.vivolight;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;

/* loaded from: classes.dex */
public class SuperSaverPowerUtil {
    private static final String ACTION_SUPER_SAVER = "intent.action.super_power_save_send";
    private static final String SPS_ACTION_ENTER = "entered";
    private static final String SPS_ACTION_EXIT = "exited";
    private static final String SPS_KEY_ACTION = "sps_action";
    private final Context mContext;
    private VivoLightManagerService mService;
    private SuperSaverBroadcastReceiver mSuperSaverBroadcastReceiver = new SuperSaverBroadcastReceiver();

    public SuperSaverPowerUtil(Context context, VivoLightManagerService service) {
        this.mContext = context;
        this.mService = service;
    }

    public void register() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_SUPER_SAVER);
        this.mContext.registerReceiver(this.mSuperSaverBroadcastReceiver, filter);
    }

    private void unRegister() {
        this.mContext.unregisterReceiver(this.mSuperSaverBroadcastReceiver);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SuperSaverBroadcastReceiver extends BroadcastReceiver {
        private SuperSaverBroadcastReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            String action = intent.getAction();
            if (SuperSaverPowerUtil.ACTION_SUPER_SAVER.equals(action)) {
                String extra = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                try {
                    extra = intent.getStringExtra(SuperSaverPowerUtil.SPS_KEY_ACTION);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                VLog.d(VivoLightManagerService.TAG, "action = " + extra);
                if (SuperSaverPowerUtil.SPS_ACTION_ENTER.equals(extra)) {
                    VLog.d(VivoLightManagerService.TAG, "SPS_ACTION_ENTER");
                    SuperSaverPowerUtil.this.mService.setUltraSavePower(true);
                    SuperSaverPowerUtil.this.mService.notifyUpdateLight();
                } else if (SuperSaverPowerUtil.SPS_ACTION_EXIT.equals(extra)) {
                    VLog.d(VivoLightManagerService.TAG, "SPS_ACTION_EXIT");
                    SuperSaverPowerUtil.this.mService.setUltraSavePower(false);
                    SuperSaverPowerUtil.this.mService.notifyUpdateLight();
                }
            }
        }
    }
}