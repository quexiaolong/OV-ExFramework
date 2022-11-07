package com.android.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManagerInternal;
import android.os.PowerManager;
import android.util.Slog;
import com.android.internal.os.CachedDeviceState;

/* loaded from: classes.dex */
public class CachedDeviceStateService extends SystemService {
    private static final String TAG = "CachedDeviceStateService";
    private final BroadcastReceiver mBroadcastReceiver;
    private final CachedDeviceState mDeviceState;

    public CachedDeviceStateService(Context context) {
        super(context);
        this.mDeviceState = new CachedDeviceState();
        this.mBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.CachedDeviceStateService.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                char c;
                String action = intent.getAction();
                int hashCode = action.hashCode();
                if (hashCode == -2128145023) {
                    if (action.equals("android.intent.action.SCREEN_OFF")) {
                        c = 2;
                    }
                    c = 65535;
                } else if (hashCode != -1538406691) {
                    if (hashCode == -1454123155 && action.equals("android.intent.action.SCREEN_ON")) {
                        c = 1;
                    }
                    c = 65535;
                } else {
                    if (action.equals("android.intent.action.BATTERY_CHANGED")) {
                        c = 0;
                    }
                    c = 65535;
                }
                if (c == 0) {
                    CachedDeviceStateService.this.mDeviceState.setCharging(intent.getIntExtra("plugged", 0) != 0);
                } else if (c == 1) {
                    CachedDeviceStateService.this.mDeviceState.setScreenInteractive(true);
                } else if (c == 2) {
                    CachedDeviceStateService.this.mDeviceState.setScreenInteractive(false);
                }
            }
        };
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishLocalService(CachedDeviceState.Readonly.class, this.mDeviceState.getReadonlyClient());
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (500 == phase) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.BATTERY_CHANGED");
            filter.addAction("android.intent.action.SCREEN_ON");
            filter.addAction("android.intent.action.SCREEN_OFF");
            filter.setPriority(1000);
            getContext().registerReceiver(this.mBroadcastReceiver, filter);
            this.mDeviceState.setCharging(queryIsCharging());
            this.mDeviceState.setScreenInteractive(queryScreenInteractive(getContext()));
        }
    }

    private boolean queryIsCharging() {
        BatteryManagerInternal batteryManager = (BatteryManagerInternal) LocalServices.getService(BatteryManagerInternal.class);
        if (batteryManager != null) {
            return batteryManager.getPlugType() != 0;
        }
        Slog.wtf(TAG, "BatteryManager null while starting CachedDeviceStateService");
        return true;
    }

    private boolean queryScreenInteractive(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(PowerManager.class);
        if (powerManager == null) {
            Slog.wtf(TAG, "PowerManager null while starting CachedDeviceStateService");
            return false;
        }
        return powerManager.isInteractive();
    }
}