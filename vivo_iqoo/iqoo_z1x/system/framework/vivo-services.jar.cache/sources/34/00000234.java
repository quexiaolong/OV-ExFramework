package com.android.server.display.color.displayenhance;

import android.content.Context;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import vivo.util.VSlog;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public abstract class DisplayEnhanceController {
    static final String TAG = "DisplayEnhanceController";
    private Context mContext;
    public boolean isMainSwitchOpen = false;
    public boolean isUserMainSwitchOpen = false;
    public boolean isHighTemp = false;
    public List<String> mAppPackageList = new ArrayList();
    public Map<String, Integer> mAppSwitch = new HashMap();
    public final Object mAppSwitchLock = new Object();
    public final Object mMainSwitchLock = new Object();

    public abstract int getDisplayEnhanceState();

    public DisplayEnhanceController(Context context) {
        this.mContext = context;
    }

    public boolean isMainSwitchOpen() {
        return this.isMainSwitchOpen;
    }

    public void setDisplayEnhanceState(int state) {
        this.isUserMainSwitchOpen = state == 1;
        this.isMainSwitchOpen = state == 1;
        this.isHighTemp = false;
    }

    public boolean checkLowPowerStatus(int lowPowerLevel) {
        VSlog.d(TAG, "checkLowPowerStatus: lowPowerLevel=" + lowPowerLevel + "  isUserMainSwitchOpen = " + this.isUserMainSwitchOpen + " isMainSwitchOpen = " + this.isMainSwitchOpen);
        if (lowPowerLevel == 2 && this.isUserMainSwitchOpen) {
            this.isMainSwitchOpen = false;
            return true;
        } else if (lowPowerLevel == 2 || !this.isUserMainSwitchOpen || this.isMainSwitchOpen) {
            return false;
        } else {
            this.isMainSwitchOpen = true;
            return true;
        }
    }

    public boolean checkTemperature(int temperature) {
        if (!this.isMainSwitchOpen && !this.isHighTemp) {
            return false;
        }
        if (temperature >= VivoDisplayEnhanceManagerService.TEMPERATURE_MAX && !this.isHighTemp && this.isUserMainSwitchOpen) {
            VSlog.d(TAG, "checkTemperature: temperature is to high");
            this.isMainSwitchOpen = false;
            this.isHighTemp = true;
            return true;
        } else if (temperature >= VivoDisplayEnhanceManagerService.TEMPERATURE_MAX - 1 || !this.isHighTemp || !this.isUserMainSwitchOpen || this.isMainSwitchOpen) {
            return false;
        } else {
            VSlog.d(TAG, "checkTemperature: temperature is down");
            this.isMainSwitchOpen = true;
            this.isHighTemp = false;
            return true;
        }
    }

    public void initAppSwitch(String appSwitch) {
        if (appSwitch != null) {
            try {
                JSONObject object = new JSONObject(appSwitch);
                synchronized (this.mAppSwitchLock) {
                    for (String name : this.mAppSwitch.keySet()) {
                        if (object.has(name)) {
                            int oldState = ((Integer) object.get(name)).intValue();
                            if (this.mAppSwitch.get(name) != null && this.mAppSwitch.get(name).intValue() != oldState) {
                                this.mAppSwitch.put(name, Integer.valueOf(oldState));
                                VSlog.d(TAG, "set AppSwitch: app=" + name + "  value=" + oldState);
                            }
                        }
                    }
                }
            } catch (JSONException e) {
                VSlog.e(TAG, "initAppSwitch: json parse exception " + e);
            }
        }
    }

    public List<String> getSupportAppList() {
        return this.mAppPackageList;
    }

    public void setPackageSettingState(String name, int state) {
        if (name == null) {
            return;
        }
        synchronized (this.mAppSwitchLock) {
            Integer oldStateInteger = this.mAppSwitch.get(name);
            int oldState = oldStateInteger != null ? oldStateInteger.intValue() : 0;
            VSlog.d(TAG, "setPackageSettingState: name=" + name + ", state=" + state + ", oldState=" + oldState);
            if (oldState == state) {
                return;
            }
            this.mAppSwitch.put(name, Integer.valueOf(state));
        }
    }

    public int getPackageSettingState(String name) {
        synchronized (this.mAppSwitchLock) {
            if (!this.mAppSwitch.isEmpty() && name != null) {
                Integer state = this.mAppSwitch.get(name);
                if (state != null) {
                    return state.intValue();
                }
                return 0;
            }
            return 0;
        }
    }
}