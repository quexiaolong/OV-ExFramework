package com.android.server.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/* loaded from: classes.dex */
public class VivoNlpPowerMonitor {
    private static final int CUR_MODEL = 100;
    private static final int ENTRY_POWER_SAVE_DELAY = 10000;
    private static final int EXIT_POWER_SAVE_DELAY = 2000;
    public static final String FINGER_UNLOCK = "finger_unlock_open";
    private static final int HASH_MAP_INITIAL_CAPACITY_TO_TRACK_CONNECTED_NETWORKS = 5;
    public static final String MOVE_WAKE = "udfp_move_wake";
    private static final int MSG_EXIT_POWER_SAVE_MODE = 3;
    private static final int MSG_GET_LAST_LOCATION = 2;
    private static final int MSG_SENSOR_STATUS_CHANGED = 1;
    private static final int RGC_DISTANCE = 10;
    private static final int RGC_SAVE_COUNT = 50;
    private static final int SENSOR_MOTIONLESS = 1;
    private static final int SENSOR_MOVING = 2;
    private static final String TAG = "VivoNlpPowerMonitor";
    private static Context mContext;
    private ConnectivityManager mConnectivityManager;
    private VivoNlpPowerMonitorHandler mHandler;
    private final Object mLock;
    private Network mNetwork;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private NetworkCapabilities mNetworkCapabilities;
    private NetworkRequest mNetworkRequest;
    private NetworkRequest.Builder mNetworkRequestBuilder;
    protected List<String> mNlpSaveFilterList;
    private SensorManager mSensorManager;
    private Sensor mVivoMotionSensor;
    private VivoNlpPowerMonitorBroadcastReceiver mVivoNlpPowerMonitorBroadcastReceiver;
    protected LocationManager mlocationmanager;
    private static int TYPE_MOTION_DETECT = 91;
    private static VivoNlpPowerMonitorDebugPanel mVivoNlpPowerMonitorDebugPanel = null;
    protected boolean mIsProvisioned = true;
    protected int mSensorStatus = 2;
    protected boolean mIsRegistered = false;
    protected boolean mIsNetworkAvailable = true;
    protected Location mLastLocation = null;
    protected boolean bPowerSaveMode = false;
    protected boolean bStopNlpPowerSave = false;
    protected boolean bNlpPowerSaveTestMode = false;
    protected boolean bNlpPowerSaveStopMode = false;
    protected boolean bScreenOff = false;
    protected int mSaveProcess = 0;
    protected RgcNode[] mRgcSaves = new RgcNode[50];
    protected int flagForQueue = 0;
    private SensorEventListener mVivoMotionListener = new SensorEventListener() { // from class: com.android.server.location.VivoNlpPowerMonitor.1
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            try {
                float result = event.values[0];
                int status = 2;
                if (((int) result) == 1) {
                    status = 1;
                }
                VLog.d(VivoNlpPowerMonitor.TAG, "VivoMotionSensor lister get event " + VivoNlpPowerMonitor.eventToString(status));
                VivoNlpPowerMonitor.this.mHandler.sendMessage(VivoNlpPowerMonitor.this.mHandler.obtainMessage(1, status, 0));
            } catch (Exception ex) {
                VLog.e(VivoNlpPowerMonitor.TAG, Log.getStackTraceString(ex));
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private final LocationListener locationListener = new LocationListener() { // from class: com.android.server.location.VivoNlpPowerMonitor.2
        @Override // android.location.LocationListener
        public void onLocationChanged(Location location) {
            VivoNlpPowerMonitor.this.mLastLocation = location;
            VivoNlpPowerMonitor.this.gotLastLocation();
        }
    };

    public VivoNlpPowerMonitor(Context context, Looper looper, Object object) {
        this.mlocationmanager = null;
        mContext = context;
        ArrayList arrayList = new ArrayList();
        this.mNlpSaveFilterList = arrayList;
        arrayList.add("com.qualcomm.location");
        this.mLock = object;
        this.mHandler = new VivoNlpPowerMonitorHandler(looper);
        this.mVivoNlpPowerMonitorBroadcastReceiver = new VivoNlpPowerMonitorBroadcastReceiver();
        this.mSensorManager = (SensorManager) mContext.getSystemService("sensor");
        int sensorType = getSensorType();
        TYPE_MOTION_DETECT = sensorType;
        this.mVivoMotionSensor = this.mSensorManager.getDefaultSensor(sensorType);
        this.mConnectivityManager = (ConnectivityManager) mContext.getSystemService("connectivity");
        this.mlocationmanager = (LocationManager) mContext.getSystemService("location");
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        this.mNetworkRequestBuilder = builder;
        builder.addCapability(12);
        this.mNetworkRequestBuilder.addCapability(16);
        this.mNetworkRequestBuilder.removeCapability(15);
        this.mNetworkRequest = this.mNetworkRequestBuilder.build();
        ConnectivityManager.NetworkCallback createNetworkConnectivityCallback = createNetworkConnectivityCallback();
        this.mNetworkCallback = createNetworkConnectivityCallback;
        this.mConnectivityManager.registerNetworkCallback(this.mNetworkRequest, createNetworkConnectivityCallback, this.mHandler);
        if (mVivoNlpPowerMonitorDebugPanel == null) {
            mVivoNlpPowerMonitorDebugPanel = new VivoNlpPowerMonitorDebugPanel(mContext, this);
        }
    }

    public void startNlpPowerMonitor() {
        try {
            clearUp();
            if (!this.mIsRegistered && this.bScreenOff && isFingerUnlock(mContext) && this.mIsNetworkAvailable && !this.bNlpPowerSaveStopMode) {
                VLog.d(TAG, "register VivoMotionSensor listener");
                if (this.mVivoMotionSensor == null) {
                    VLog.d(TAG, "VivoMotionSensor is not available.");
                } else {
                    this.mSensorManager.registerListener(this.mVivoMotionListener, this.mVivoMotionSensor, 3);
                    this.mIsRegistered = true;
                }
            }
        } catch (Exception ex) {
            VLog.e(TAG, Log.getStackTraceString(ex));
        }
    }

    public boolean isRegistered() {
        return this.mIsRegistered;
    }

    public void stopNlpPowerMonitor() {
        try {
            if (this.mIsRegistered) {
                VLog.d(TAG, "unregister VivoMotionSensor listener");
                clearUp();
                if (this.mVivoMotionSensor != null) {
                    this.mSensorManager.unregisterListener(this.mVivoMotionListener, this.mVivoMotionSensor);
                }
                this.mIsRegistered = false;
            }
            if (this.mlocationmanager != null) {
                this.mlocationmanager.removeUpdates(this.locationListener);
            }
        } catch (Exception ex) {
            VLog.e(TAG, Log.getStackTraceString(ex));
        }
    }

    private void clearUp() {
        VLog.d(TAG, "clearUp");
        try {
            removeAllMessages();
            this.mSensorStatus = 2;
        } catch (Exception ex) {
            VLog.e(TAG, Log.getStackTraceString(ex));
        }
    }

    public static String eventToString(int event) {
        if (event != 1) {
            if (event == 2) {
                return "SENSOR_MOVING";
            }
            return "UNKNOWN";
        }
        return "SENSOR_MOTIONLESS";
    }

    private int getSensorType() {
        try {
            Class<?> sensorClass = Class.forName("android.hardware.Sensor");
            Field field = sensorClass.getDeclaredField("TYPE_VIVOMOTION_DETECT");
            return field.getInt(sensorClass);
        } catch (Exception e) {
            e.printStackTrace();
            return 91;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void getLastLocation() {
        if (this.mlocationmanager != null) {
            if (mContext.checkSelfPermission("android.permission.ACCESS_FINE_LOCATION") != 0 && mContext.checkSelfPermission("android.permission.ACCESS_COARSE_LOCATION") != 0) {
                VLog.d(TAG, "No location permission!");
                return;
            }
            this.mlocationmanager.requestLocationUpdates("passive", 0L, 0.0f, this.locationListener);
            this.mSaveProcess = 2;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void gotLastLocation() {
        Location location;
        VLog.d(TAG, "got the last location after MOTIONLESS, ready to save");
        this.mlocationmanager.removeUpdates(this.locationListener);
        if (this.mSensorStatus == 1 && (location = this.mLastLocation) != null) {
            if (this.bNlpPowerSaveTestMode) {
                location.setLatitude(39.907375d);
                this.mLastLocation.setLongitude(116.391349d);
            }
            this.bPowerSaveMode = true;
            Bundle extras = this.mLastLocation.getExtras();
            if (extras == null) {
                this.mLastLocation.setExtras(new Bundle());
                extras = this.mLastLocation.getExtras();
            }
            extras.putBoolean("NPS", true);
            VivoLocationManagerServiceExt.setPowerSaveStatus(true);
            VLog.d(TAG, "The last location " + this.mLastLocation);
            this.mSaveProcess = 3;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public List<Address> checkDistanceForGeocoder(double latitude, double longitude, Locale locale) {
        Location location = new Location("network");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        String mRgcLanguage = locale.getLanguage();
        int i = 0;
        while (true) {
            RgcNode[] rgcNodeArr = this.mRgcSaves;
            if (i < rgcNodeArr.length) {
                if (rgcNodeArr[i] != null) {
                    int distance = (int) rgcNodeArr[i].location.distanceTo(location);
                    String mCurrentLanguage = this.mRgcSaves[i].locale != null ? this.mRgcSaves[i].locale.getLanguage() : Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                    if (distance < 10 && this.mRgcSaves[i].result.size() > 0 && mRgcLanguage.equals(mCurrentLanguage)) {
                        VLog.d(TAG, "RGC distance " + distance + " to " + i + ": " + this.mRgcSaves[i].location.toString() + ", " + this.mRgcSaves[i].result + ", request " + locale.toString() + "(" + mRgcLanguage + "), check " + this.mRgcSaves[i].locale.toString() + "(" + mCurrentLanguage + ")");
                        return this.mRgcSaves[i].result;
                    }
                }
                i++;
            } else {
                return null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void updateRgcNode(Location loc, List<Address> res, Locale locale) {
        this.flagForQueue = (this.flagForQueue + 1) % 50;
        RgcNode rgcNode = new RgcNode(loc, res, locale);
        this.mRgcSaves[this.flagForQueue] = rgcNode;
        locale.getLanguage();
        VLog.d(TAG, "RGC update [" + this.flagForQueue + "]" + loc.toString() + ", " + res + ", " + locale.toString());
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean inNlpSaveWhiteList(String packageName) {
        return this.mNlpSaveFilterList.contains(packageName);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void exitPowerSaveMode() {
        this.mHandler.removeMessages(2);
        this.mlocationmanager.removeUpdates(this.locationListener);
        this.mLastLocation = null;
        this.bPowerSaveMode = false;
        VivoLocationManagerServiceExt.setPowerSaveStatus(false);
        VLog.d(TAG, "exit PowerSaveMode done");
        this.mSaveProcess = 0;
    }

    private static boolean isProvisioned(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), "device_provisioned", 0) == 1;
    }

    private static boolean isFingerUnlock(Context context) {
        boolean fingerUnlock = Settings.System.getInt(context.getContentResolver(), FINGER_UNLOCK, 0) > 0;
        boolean z = Settings.System.getInt(context.getContentResolver(), MOVE_WAKE, 0) > 0;
        return fingerUnlock;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class RgcNode {
        Locale locale;
        Location location;
        List<Address> result;

        RgcNode(Location loc, List<Address> res, Locale mlocale) {
            this.location = null;
            this.result = new ArrayList();
            this.locale = null;
            this.location = loc;
            this.result = res;
            this.locale = mlocale;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class VivoNlpPowerMonitorHandler extends Handler {
        public VivoNlpPowerMonitorHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            try {
                if (msg == null) {
                    VLog.e(VivoNlpPowerMonitor.TAG, "msg error");
                    return;
                }
                VLog.d(VivoNlpPowerMonitor.TAG, "handleMessage " + msgToString(msg.what));
                int i = msg.what;
                if (i != 1) {
                    if (i == 2) {
                        VivoNlpPowerMonitor.this.getLastLocation();
                        return;
                    } else if (i == 3) {
                        VivoNlpPowerMonitor.this.exitPowerSaveMode();
                        return;
                    } else {
                        return;
                    }
                }
                int status = msg.arg1;
                if (status != VivoNlpPowerMonitor.this.mSensorStatus) {
                    VLog.d(VivoNlpPowerMonitor.TAG, "MSG_SENSOR_STATUS_CHANGED new:" + VivoNlpPowerMonitor.eventToString(status) + ", old:" + VivoNlpPowerMonitor.eventToString(VivoNlpPowerMonitor.this.mSensorStatus));
                    VivoNlpPowerMonitor.this.mSensorStatus = status;
                    VivoNlpPowerMonitor.this.mHandler.removeMessages(3);
                    if (VivoNlpPowerMonitor.this.mSensorStatus == 1 && VivoNlpPowerMonitor.this.bScreenOff && VivoNlpPowerMonitor.this.mIsNetworkAvailable) {
                        VivoNlpPowerMonitor.this.mHandler.removeMessages(2);
                        VivoNlpPowerMonitor.this.mHandler.sendEmptyMessageDelayed(2, 10000L);
                        VivoNlpPowerMonitor.this.mSaveProcess = 1;
                    }
                }
                if (VivoNlpPowerMonitor.this.mSensorStatus == 2 && VivoNlpPowerMonitor.this.mIsNetworkAvailable) {
                    VivoNlpPowerMonitor.this.mHandler.sendEmptyMessageDelayed(3, 2000L);
                }
            } catch (Exception ex) {
                VLog.e(VivoNlpPowerMonitor.TAG, Log.getStackTraceString(ex));
            }
        }

        public String msgToString(int what) {
            if (what != 1) {
                if (what != 2) {
                    if (what == 3) {
                        return "MSG_EXIT_POWER_SAVE_MODE";
                    }
                    return "UNKNOWN";
                }
                return "MSG_GET_LAST_LOCATION";
            }
            return "MSG_SENSOR_STATUS_CHANGED";
        }
    }

    private void removeAllMessages() {
    }

    /* loaded from: classes.dex */
    class VivoNlpPowerMonitorBroadcastReceiver extends BroadcastReceiver {
        public VivoNlpPowerMonitorBroadcastReceiver() {
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.vivo.NLP_DEBUG");
            filter.addAction("android.intent.action.BOOT_COMPLETED");
            filter.addAction("android.intent.action.SCREEN_OFF");
            filter.addAction("android.intent.action.USER_PRESENT");
            filter.addAction("android.intent.action.LOCALE_CHANGED");
            VivoNlpPowerMonitor.mContext.registerReceiver(this, filter);
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            VLog.d(VivoNlpPowerMonitor.TAG, "receive broadcast intent, action: " + action);
            if (action == null) {
                return;
            }
            if (action.equals("android.intent.action.BOOT_COMPLETED")) {
                VivoNlpPowerMonitor vivoNlpPowerMonitor = VivoNlpPowerMonitor.this;
                vivoNlpPowerMonitor.mNetwork = vivoNlpPowerMonitor.mConnectivityManager.getActiveNetwork();
                if (VivoNlpPowerMonitor.this.mNetwork != null) {
                    VivoNlpPowerMonitor vivoNlpPowerMonitor2 = VivoNlpPowerMonitor.this;
                    vivoNlpPowerMonitor2.mNetworkCapabilities = vivoNlpPowerMonitor2.mConnectivityManager.getNetworkCapabilities(VivoNlpPowerMonitor.this.mNetwork);
                    if (VivoNlpPowerMonitor.this.mNetworkCapabilities != null) {
                        VivoNlpPowerMonitor vivoNlpPowerMonitor3 = VivoNlpPowerMonitor.this;
                        vivoNlpPowerMonitor3.mIsNetworkAvailable = vivoNlpPowerMonitor3.mNetworkCapabilities.hasCapability(16);
                    }
                } else {
                    VivoNlpPowerMonitor.this.mIsNetworkAvailable = false;
                }
                if (VivoNlpPowerMonitor.this.mIsNetworkAvailable) {
                    VivoLocationManagerServiceExt.setPowerSaveStatus(false);
                } else {
                    VivoLocationManagerServiceExt.setPowerSaveStatus(true);
                }
                VLog.d(VivoNlpPowerMonitor.TAG, "mIsNetworkAvailable: " + VivoNlpPowerMonitor.this.mIsNetworkAvailable);
            }
            if (action.equals("com.vivo.NLP_DEBUG")) {
                if (intent.getBooleanExtra("off", false)) {
                    VivoNlpPowerMonitor.this.mHandler.sendEmptyMessage(3);
                    VivoNlpPowerMonitor.this.stopNlpPowerMonitor();
                    VivoNlpPowerMonitor.this.bNlpPowerSaveStopMode = true;
                    VLog.d(VivoNlpPowerMonitor.TAG, "turn off the power save");
                }
                VivoNlpPowerMonitor.this.bStopNlpPowerSave = intent.getBooleanExtra("stop", false);
                if (VivoNlpPowerMonitor.this.bStopNlpPowerSave) {
                    VivoNlpPowerMonitor.this.mHandler.sendEmptyMessage(3);
                    VLog.d(VivoNlpPowerMonitor.TAG, "in test , ready to exit");
                }
                String tempWhiteList = intent.getStringExtra("white");
                if (tempWhiteList != null && tempWhiteList.length() > 0) {
                    VivoNlpPowerMonitor.this.mNlpSaveFilterList.add(tempWhiteList);
                    VLog.d(VivoNlpPowerMonitor.TAG, "in test , add white list: " + tempWhiteList);
                }
                VivoNlpPowerMonitor.this.bNlpPowerSaveTestMode = intent.getBooleanExtra("test", false);
                if (VivoNlpPowerMonitor.this.bNlpPowerSaveTestMode) {
                    VivoNlpPowerMonitor.this.bScreenOff = true;
                }
                VLog.d(VivoNlpPowerMonitor.TAG, "in test , ignore screen off status: " + VivoNlpPowerMonitor.this.bNlpPowerSaveTestMode);
            }
            if ("android.intent.action.USER_PRESENT".equals(intent.getAction())) {
                VivoNlpPowerMonitor.this.bScreenOff = false;
                VLog.d(VivoNlpPowerMonitor.TAG, "ScreenOff: " + VivoNlpPowerMonitor.this.bScreenOff + ", bNlpPowerSaveTestMode: " + VivoNlpPowerMonitor.this.bNlpPowerSaveTestMode);
                VivoNlpPowerMonitor vivoNlpPowerMonitor4 = VivoNlpPowerMonitor.this;
                vivoNlpPowerMonitor4.bScreenOff = vivoNlpPowerMonitor4.bScreenOff || VivoNlpPowerMonitor.this.bNlpPowerSaveTestMode;
                if (!VivoNlpPowerMonitor.this.bScreenOff) {
                    VivoNlpPowerMonitor.this.exitPowerSaveMode();
                    VivoNlpPowerMonitor.this.stopNlpPowerMonitor();
                }
            }
            if ("android.intent.action.SCREEN_OFF".equals(intent.getAction())) {
                VivoNlpPowerMonitor.this.bScreenOff = true;
                VivoNlpPowerMonitor.this.startNlpPowerMonitor();
                VLog.d(VivoNlpPowerMonitor.TAG, "ScreenOff: " + VivoNlpPowerMonitor.this.bScreenOff);
            }
            if ("android.intent.action.LOCALE_CHANGED".equals(intent.getAction())) {
                VLog.d(VivoNlpPowerMonitor.TAG, "language change, clear cache");
                synchronized (VivoNlpPowerMonitor.this.mLock) {
                    VivoNlpPowerMonitor.this.mRgcSaves = new RgcNode[50];
                    VivoNlpPowerMonitor.this.flagForQueue = 0;
                }
            }
        }
    }

    private ConnectivityManager.NetworkCallback createNetworkConnectivityCallback() {
        return new ConnectivityManager.NetworkCallback() { // from class: com.android.server.location.VivoNlpPowerMonitor.3
            private HashMap<Network, NetworkCapabilities> mAvailableNetworkCapabilities = new HashMap<>(5);

            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
                if (!VivoNlpPowerMonitor.hasCapabilitiesChanged(this.mAvailableNetworkCapabilities.get(network), capabilities)) {
                    Log.v(VivoNlpPowerMonitor.TAG, "Relevant network capabilities unchanged. Capabilities: " + capabilities);
                    return;
                }
                this.mAvailableNetworkCapabilities.put(network, capabilities);
                Log.d(VivoNlpPowerMonitor.TAG, "Network connected/capabilities updated. Available networks count: " + this.mAvailableNetworkCapabilities.size());
                VivoNlpPowerMonitor.this.mIsNetworkAvailable = true;
                VivoLocationManagerServiceExt.setPowerSaveStatus(false);
                VLog.d(VivoNlpPowerMonitor.TAG, "network " + network + " is onAvailable , ready to exit. Available networks count: " + this.mAvailableNetworkCapabilities.size());
            }

            @Override // android.net.ConnectivityManager.NetworkCallback
            public void onLost(Network network) {
                if (this.mAvailableNetworkCapabilities.remove(network) == null) {
                    Log.w(VivoNlpPowerMonitor.TAG, "Incorrectly received network callback onLost() before onCapabilitiesChanged() for network: " + network);
                    return;
                }
                Log.i(VivoNlpPowerMonitor.TAG, "Network connection lost. Available networks count: " + this.mAvailableNetworkCapabilities.size());
                if (this.mAvailableNetworkCapabilities.size() == 0) {
                    VivoNlpPowerMonitor.this.mIsNetworkAvailable = false;
                    VivoLocationManagerServiceExt.setPowerSaveStatus(true);
                    VLog.d(VivoNlpPowerMonitor.TAG, "network " + network + " is onLost , ready to entry");
                }
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean hasCapabilitiesChanged(NetworkCapabilities curCapabilities, NetworkCapabilities newCapabilities) {
        return curCapabilities == null || newCapabilities == null || hasCapabilityChanged(curCapabilities, newCapabilities, 18) || hasCapabilityChanged(curCapabilities, newCapabilities, 11);
    }

    private static boolean hasCapabilityChanged(NetworkCapabilities curCapabilities, NetworkCapabilities newCapabilities, int capability) {
        return curCapabilities.hasCapability(capability) != newCapabilities.hasCapability(capability);
    }
}