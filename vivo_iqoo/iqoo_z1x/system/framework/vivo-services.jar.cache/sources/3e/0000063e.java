package com.vivo.services.engineerutile;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IPowerManager;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.vivo.common.utils.VLog;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;

/* loaded from: classes.dex */
public class BBKEngineerPowerSave {
    private static final String ACTION_SAVE_POWER = "android.intent.action.ALARM_SAVE_POWER";
    private static final long DELAY_TIME_MILLIS = 28800000;
    private static final String TAG = "BBKEngineerUtileService";
    private static final String USER_ACTION_CONTACTS_CHANGED = "contacts_changed";
    private static final String USER_ACTION_NETWORK_CONNECTED = "network_connected";
    private static final String USER_ACTION_NO_CHANGEDE = "no";
    private static final String USER_ACTION_PROPER = "persist.sys.user.action";
    private static final String USER_ACTION_SIM_STATE_CHANGED = "sim_ready";
    private static final String USER_ACTION_TIME_CHANGED = "time_changed";
    private static final String USER_ACTION_USB_CONNECT = "usb_connect";
    private Context mContext;
    public Handler mHandler = null;
    private AlertDialog mShutdownDialog = null;
    private int mShutdownSecond = 30;
    Runnable mCountdownRunnable = new Runnable() { // from class: com.vivo.services.engineerutile.BBKEngineerPowerSave.3
        @Override // java.lang.Runnable
        public void run() {
            if (BBKEngineerPowerSave.this.mShutdownDialog == null || !BBKEngineerPowerSave.this.mShutdownDialog.isShowing()) {
                Log.d(BBKEngineerPowerSave.TAG, "user cancel");
                return;
            }
            BBKEngineerPowerSave.access$510(BBKEngineerPowerSave.this);
            String content = String.format(BBKEngineerPowerSave.this.mContext.getString(51249892), Integer.valueOf(BBKEngineerPowerSave.this.mShutdownSecond));
            BBKEngineerPowerSave.this.mShutdownDialog.setMessage(content);
            if (BBKEngineerPowerSave.this.mShutdownSecond <= 0) {
                BBKEngineerPowerSave.this.shutdown();
            } else {
                BBKEngineerPowerSave.this.mHandler.postDelayed(BBKEngineerPowerSave.this.mCountdownRunnable, 1000L);
            }
        }
    };
    private BroadcastReceiver mConnecteReceiver = new BroadcastReceiver() { // from class: com.vivo.services.engineerutile.BBKEngineerPowerSave.4
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String act = intent.getAction();
            if (act.equals("android.intent.action.SIM_STATE_CHANGED")) {
                try {
                    TelephonyManager tm = (TelephonyManager) context.getSystemService("phone");
                    int state = tm.getSimState();
                    if (state == 5) {
                        Log.d(BBKEngineerPowerSave.TAG, "SIM_STATE_READY");
                        SystemProperties.set(BBKEngineerPowerSave.USER_ACTION_PROPER, BBKEngineerPowerSave.USER_ACTION_SIM_STATE_CHANGED);
                        BBKEngineerPowerSave.this.unRegisterNetworkEvent();
                        BBKEngineerPowerSave.this.cancelAlarmManager();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else if (act.equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                NetworkInfo info = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                if (info != null && NetworkInfo.State.CONNECTED == info.getState()) {
                    Log.d(BBKEngineerPowerSave.TAG, "info.getState():" + info.getState());
                    SystemProperties.set(BBKEngineerPowerSave.USER_ACTION_PROPER, BBKEngineerPowerSave.USER_ACTION_NETWORK_CONNECTED);
                    BBKEngineerPowerSave.this.unRegisterNetworkEvent();
                    BBKEngineerPowerSave.this.cancelAlarmManager();
                }
            } else if (act.equals("android.intent.action.SCREEN_ON") || act.equals("android.intent.action.SCREEN_OFF")) {
                BBKEngineerPowerSave.this.cancelAlarmManager();
                String user = SystemProperties.get(BBKEngineerPowerSave.USER_ACTION_PROPER, BBKEngineerPowerSave.USER_ACTION_NO_CHANGEDE);
                if (!BBKEngineerPowerSave.USER_ACTION_NO_CHANGEDE.equals(user)) {
                    BBKEngineerPowerSave.this.unRegisterNetworkEvent();
                } else {
                    BBKEngineerPowerSave.this.setAlarmManager();
                }
            } else if (act.equals(BBKEngineerPowerSave.ACTION_SAVE_POWER)) {
                Log.d(BBKEngineerPowerSave.TAG, "AlarmReceiver");
                if (BBKEngineerPowerSave.this.needStart()) {
                    BBKEngineerPowerSave.this.showShutdownDailog();
                    return;
                }
                BBKEngineerPowerSave.this.unRegisterNetworkEvent();
                BBKEngineerPowerSave.this.cancelAlarmManager();
            } else if (act.equals("android.intent.action.TIME_SET")) {
                Log.d(BBKEngineerPowerSave.TAG, "Intent.ACTION_TIME_CHANGED");
                if (BBKEngineerPowerSave.this.isSystemReady() && BBKEngineerPowerSave.this.isUserChageTime()) {
                    SystemProperties.set(BBKEngineerPowerSave.USER_ACTION_PROPER, BBKEngineerPowerSave.USER_ACTION_TIME_CHANGED);
                    BBKEngineerPowerSave.this.unRegisterNetworkEvent();
                    BBKEngineerPowerSave.this.cancelAlarmManager();
                    return;
                }
                Log.d(BBKEngineerPowerSave.TAG, "system selfChange!");
            } else if (act.equals("android.intent.action.BATTERY_CHANGED")) {
                int status = intent.getIntExtra("status", 1);
                Log.d(BBKEngineerPowerSave.TAG, "battery_status:" + status);
                if (status == 2) {
                    SystemProperties.set(BBKEngineerPowerSave.USER_ACTION_PROPER, BBKEngineerPowerSave.USER_ACTION_USB_CONNECT);
                    BBKEngineerPowerSave.this.unRegisterNetworkEvent();
                    BBKEngineerPowerSave.this.cancelAlarmManager();
                } else if (status == 5) {
                    BBKEngineerPowerSave.this.cancelAlarmManager();
                    BBKEngineerPowerSave.this.setAlarmManager();
                }
            }
        }
    };
    private ContentObserver mObserver = new ContentObserver(new Handler()) { // from class: com.vivo.services.engineerutile.BBKEngineerPowerSave.5
        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            Log.d(BBKEngineerPowerSave.TAG, "selfChange:" + selfChange);
            if (BBKEngineerPowerSave.this.simCardReady()) {
                BBKEngineerPowerSave.this.unRegisterNetworkEvent();
                BBKEngineerPowerSave.this.cancelAlarmManager();
            }
        }
    };

    static /* synthetic */ int access$510(BBKEngineerPowerSave x0) {
        int i = x0.mShutdownSecond;
        x0.mShutdownSecond = i - 1;
        return i;
    }

    public BBKEngineerPowerSave(Context context) {
        this.mContext = null;
        Log.d(TAG, "BBKEngineerPowerSave start");
        this.mContext = context;
        if (needStart()) {
            registerNetworkEvent();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showShutdownDailog() {
        unRegisterNetworkEvent();
        cancelAlarmManager();
        Log.d(TAG, "showAlertDialog");
        this.mShutdownSecond = 30;
        String content = String.format(this.mContext.getString(51249892), Integer.valueOf(this.mShutdownSecond));
        AlertDialog create = new AlertDialog.Builder(this.mContext).setIconAttribute(16843605).setTitle(this.mContext.getString(51249894)).setMessage(content).setPositiveButton(this.mContext.getString(51249893), new DialogInterface.OnClickListener() { // from class: com.vivo.services.engineerutile.BBKEngineerPowerSave.2
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialog, int which) {
                BBKEngineerPowerSave.this.shutdown();
            }
        }).setNegativeButton(this.mContext.getString(51249891), new DialogInterface.OnClickListener() { // from class: com.vivo.services.engineerutile.BBKEngineerPowerSave.1
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialog, int which) {
                Log.d(BBKEngineerPowerSave.TAG, "user cancel");
                dialog.dismiss();
                if (BBKEngineerPowerSave.this.needStart()) {
                    BBKEngineerPowerSave.this.registerNetworkEvent();
                    BBKEngineerPowerSave.this.setAlarmManager();
                }
            }
        }).create();
        this.mShutdownDialog = create;
        create.setCancelable(false);
        this.mShutdownDialog.getWindow().setType(2010);
        this.mShutdownDialog.getWindow().addFlags(2621568);
        this.mShutdownDialog.show();
        if (this.mHandler == null) {
            this.mHandler = new Handler();
        }
        this.mHandler.postDelayed(this.mCountdownRunnable, 1000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean needStart() {
        String factory = SystemProperties.get("persist.sys.factory.mode", USER_ACTION_NO_CHANGEDE);
        if (factory != null && factory.equals("yes")) {
            Log.d(TAG, "factory mode");
            return false;
        }
        String act = SystemProperties.get(USER_ACTION_PROPER, USER_ACTION_NO_CHANGEDE);
        if (!USER_ACTION_NO_CHANGEDE.equals(act)) {
            Log.d(TAG, "user mode:" + act);
            return false;
        }
        if (!isSystemReady()) {
            Log.i(TAG, "system not ready.");
        } else if (simCardReady()) {
            return false;
        } else {
            if (wifiConnected()) {
                SystemProperties.set(USER_ACTION_PROPER, USER_ACTION_NETWORK_CONNECTED);
                return false;
            }
        }
        String bspMode = SystemProperties.get("sys.bsptest.finish", "null");
        if (bspMode != null && !bspMode.equals("null")) {
            VLog.d(TAG, "bsptest mode:" + bspMode);
            return false;
        }
        String emTestMode = SystemProperties.get("sys.em_test.running", "null");
        if (emTestMode != null && emTestMode.equals("running")) {
            VLog.d(TAG, "emtest mode:" + emTestMode);
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerNetworkEvent() {
        Log.d(TAG, "registerNetworkEvent()");
        IntentFilter filter = new IntentFilter("android.intent.action.SIM_STATE_CHANGED");
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction(ACTION_SAVE_POWER);
        filter.addAction("android.intent.action.TIME_SET");
        filter.addAction("android.intent.action.BATTERY_CHANGED");
        this.mContext.registerReceiver(this.mConnecteReceiver, filter);
        this.mContext.getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, this.mObserver);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unRegisterNetworkEvent() {
        Log.d(TAG, "unRegisterNetworkEvent()");
        try {
            this.mContext.unregisterReceiver(this.mConnecteReceiver);
            this.mContext.getContentResolver().unregisterContentObserver(this.mObserver);
        } catch (Exception ex) {
            Log.d(TAG, "unRegisterNetworkEvent():" + ex.getMessage());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setAlarmManager() {
        Log.d(TAG, "setAlarmManager()");
        try {
            AlarmManager alarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
            long elapsedRealtime = SystemClock.elapsedRealtime() + DELAY_TIME_MILLIS;
            alarmManager.set(2, elapsedRealtime, getPendingIntent());
        } catch (Exception ex) {
            Log.d(TAG, "setAlarmManager():" + ex.getMessage());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelAlarmManager() {
        Log.d(TAG, "cancelAlarmManager()");
        AlarmManager alarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        alarmManager.cancel(getPendingIntent());
    }

    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(ACTION_SAVE_POWER);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.mContext, 0, intent, 0);
        return pendingIntent;
    }

    private boolean wifiConnectedBefore() {
        boolean network = false;
        try {
            File file = new File("/data/misc/wifi/wpa_supplicant.conf");
            FileReader reader = new FileReader(file);
            BufferedReader br = new BufferedReader(reader);
            while (true) {
                String line = br.readLine();
                if (line != null) {
                    if (line.startsWith("network={")) {
                        Log.d(TAG, "network={" + br.readLine() + "}");
                        network = true;
                        break;
                    }
                } else {
                    break;
                }
            }
            br.close();
            reader.close();
        } catch (Exception ex) {
            Log.e(TAG, "wifiConnectedBefore():" + ex.getMessage());
        }
        return network;
    }

    private boolean wifiConnected() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
            NetworkInfo wifiNetworkInfo = connectivityManager.getNetworkInfo(1);
            if (!wifiNetworkInfo.isConnected()) {
                return false;
            }
            return true;
        } catch (Exception ex) {
            Log.e(TAG, "wifiConnected():" + ex.getMessage());
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isSystemReady() {
        String bootanim = SystemProperties.get("init.svc.bootanim", "running");
        String boot_completed = SystemProperties.get("sys.boot_completed", "0");
        if (bootanim.equals("stopped") && boot_completed.equals("1")) {
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isUserChageTime() {
        int autoEnabled;
        try {
            autoEnabled = Settings.Global.getInt(this.mContext.getContentResolver(), "auto_time", 0);
        } catch (Exception ex) {
            Log.e(TAG, "isUserChageTime():" + ex.getMessage());
        }
        if (autoEnabled != 0) {
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean simCardReady() {
        try {
            Cursor cur = this.mContext.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
            int contacts = cur.getCount();
            if (contacts > 0) {
                SystemProperties.set(USER_ACTION_PROPER, USER_ACTION_CONTACTS_CHANGED);
                Log.d(TAG, "Contacts count:" + contacts);
                return true;
            }
            TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService("phone");
            if (tm.getSimState() == 5) {
                SystemProperties.set(USER_ACTION_PROPER, USER_ACTION_SIM_STATE_CHANGED);
                Log.d(TAG, "SIM_STATE_READY!");
                return true;
            }
            return false;
        } catch (Exception ex) {
            Log.e(TAG, "simCardReady():" + ex.getMessage());
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void shutdown() {
        try {
            Log.d(TAG, "shutdown by BBKEngineerPowerSave!");
            SystemProperties.set(USER_ACTION_PROPER, "shutdown");
            IPowerManager pms = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));
            Class<?> cls = Class.forName("android.os.IPowerManager");
            Method shutdwon = cls.getDeclaredMethod("shutdown", Boolean.TYPE, String.class, Boolean.TYPE);
            shutdwon.invoke(pms, false, "BBKEngineerPowerSave", false);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}