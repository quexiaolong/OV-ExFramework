package com.android.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemProperties;
import android.provider.Settings;
import com.android.server.BatteryService;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoLedImpl implements IVivoLed {
    private static final int BATTERY_LOW_LEVEL_STATE = 0;
    private static boolean DEBUG = false;
    private static final String IPO_POWER_OFF = "android.intent.action.ACTION_SHUTDOWN_IPO";
    private static final String IPO_POWER_ON = "android.intent.action.ACTION_BOOT_IPO";
    private static final int SCREEN_ON_OR_OFF = 1;
    private static final String TAG = "VivoLedImpl";
    private BatteryService mBatteryService;
    private Context mContext;
    private BatteryService.Led mLed;
    private boolean LooperPrepareOk = false;
    private final int red = -65536;
    private final int green = -16711936;
    private boolean mNotificationChargeStates = true;
    private boolean mNotificationLowBatteryStates = false;
    private boolean mScreenOn = true;
    private int DEBUG_VIVOLEDS = SystemProperties.getInt("persist.sys.debug.bsleds", 1);
    private final String INDICATOR_CONFIG = SystemProperties.get("persist.vivo.phone.indicator", "No_indicator");
    private boolean mIPOShutdown = false;
    private boolean mIPOed = false;
    private boolean mIPOBoot = false;
    private boolean ipo_led_on = false;
    private boolean ipo_led_off = false;
    private boolean LowLevelFlag = false;
    private boolean mBootCompleted = false;
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() { // from class: com.android.server.VivoLedImpl.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.intent.action.SCREEN_ON")) {
                VivoLedImpl.this.printfLedInfoLevel3("ACTION_SCREEN_ON");
                VivoLedImpl.this.mScreenOn = true;
            } else if (action.equals("android.intent.action.SCREEN_OFF")) {
                VivoLedImpl.this.printfLedInfoLevel3("ACTION_SCREEN_OFF");
                VivoLedImpl.this.mScreenOn = false;
            }
            VivoLedImpl.this.updateLightsLocked(1);
        }
    };

    public VivoLedImpl(BatteryService.Led led, Context context, boolean debug) {
        DEBUG = debug;
        this.mLed = led;
        this.mContext = context;
    }

    public void Led(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        context.registerReceiver(this.mIntentReceiver, filter);
    }

    public void setBatteryService(BatteryService batteryService) {
        this.mBatteryService = batteryService;
    }

    public void getLightMenuSet() {
        int notification_light_on = 15;
        if ("No_indicator".equals(this.INDICATOR_CONFIG)) {
            this.mNotificationLowBatteryStates = false;
            this.mNotificationChargeStates = false;
        } else if ("Have_indicator_green".equals(this.INDICATOR_CONFIG)) {
            this.mNotificationLowBatteryStates = false;
            this.mNotificationChargeStates = true;
        } else {
            if (this.LooperPrepareOk) {
                notification_light_on = Settings.System.getInt(this.mContext.getContentResolver(), "notification_light", 14);
            } else {
                this.LooperPrepareOk = true;
            }
            if (((notification_light_on >> 2) & 1) == 1) {
                this.mNotificationLowBatteryStates = true;
            } else {
                this.mNotificationLowBatteryStates = false;
            }
            if (((notification_light_on >> 3) & 1) == 1) {
                this.mNotificationChargeStates = true;
            } else {
                this.mNotificationChargeStates = false;
            }
        }
    }

    public void printfLedInfoLevel1(String msg) {
        int i = this.DEBUG_VIVOLEDS;
        if (i == 1 || i > 5) {
            VSlog.i("VNOILBS", msg);
        }
    }

    public void printfLedInfoLevel2(String msg) {
        int i = this.DEBUG_VIVOLEDS;
        if (i == 2 || i > 5) {
            VSlog.i("VNOILBS", msg);
        }
    }

    public void printfLedInfoLevel3(String msg) {
        int i = this.DEBUG_VIVOLEDS;
        if (i == 3 || i > 7) {
            VSlog.i("VNOILBS", msg);
        }
    }

    public void updateLightsLocked(int from) {
        int level = this.mBatteryService.getHealthInfo().batteryLevel;
        int status = this.mBatteryService.getHealthInfo().batteryStatus;
        getLightMenuSet();
        printfLedInfoLevel1("VNOILBS mScreenOn= " + this.mScreenOn + " level = " + level + " mCriticalBatteryLevel = " + this.mBatteryService.getCriticalBatteryLevel() + " mNotificationLowBatteryStates = " + this.mNotificationLowBatteryStates + " mNotificationChargeStates = " + this.mNotificationChargeStates + " status = " + status);
        if (this.mScreenOn && this.mLed.mBatteryLight != null) {
            printfLedInfoLevel2("turnOff() 1");
            this.mLed.mBatteryLight.turnOff();
        } else if (level <= this.mBatteryService.getCriticalBatteryLevel() && this.mNotificationLowBatteryStates) {
            if (status == 2 && this.mNotificationChargeStates && this.mLed.mBatteryLight != null) {
                printfLedInfoLevel2("in lowBatteryStates but charging-setColor(green)");
                this.mLed.mBatteryLight.setColor(-16711936);
            } else if (this.mLed.mBatteryLight != null) {
                printfLedInfoLevel2("in lowBatteryStates no charging-setFlashing red");
                this.mLed.mBatteryLight.setFlashing(this.mLed.mBatteryLowARGB, 1, this.mLed.mBatteryLedOn, this.mLed.mBatteryLedOff);
            }
        } else if (status == 2 || status == 5) {
            if ((status == 5 || !this.mNotificationChargeStates) && this.mLed.mBatteryLight != null) {
                printfLedInfoLevel2("in battery full status mBatteryLight.turnOff() 2");
                this.mLed.mBatteryLight.turnOff();
            } else if (this.mNotificationChargeStates && from == 1 && this.mLed.mBatteryLight != null) {
                printfLedInfoLevel2("charging-setColor(green)");
                this.mLed.mBatteryLight.setColor(-16711936);
            }
        } else if (this.mLed.mBatteryLight != null) {
            printfLedInfoLevel2("turnOff() 3");
            this.mLed.mBatteryLight.turnOff();
        }
    }

    private void getIpoLedStatus() {
        if ("1".equals(SystemProperties.get("sys.ipo.ledon"))) {
            this.ipo_led_on = true;
        } else if ("0".equals(SystemProperties.get("sys.ipo.ledon"))) {
            this.ipo_led_off = true;
        }
        if (DEBUG) {
            VSlog.d(TAG, ">>>>>>>getIpoLedStatus ipo_led_on = " + this.ipo_led_on + ",  ipo_led_off = " + this.ipo_led_off + "<<<<<<<");
        }
    }

    private void updateLedStatus() {
        if (((this.ipo_led_off && this.mIPOBoot) || (this.LowLevelFlag && this.mIPOBoot)) && this.mLed.mBatteryLight != null) {
            this.mLed.mBatteryLight.turnOff();
        }
    }

    public void dummy() {
    }
}