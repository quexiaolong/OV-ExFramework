package com.android.server.location;

import android.app.ActivityManagerNative;
import android.app.IProcessObserver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.WorkSource;
import com.android.internal.location.ProviderRequest;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.location.VivoCoreLocationManager;
import com.android.server.wm.VCD_FF_1;
import com.vivo.common.VivoCollectData;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import com.vivo.services.backup.util.VivoBackupCommonUtil;
import com.vivo.vcodetransbase.EventTransfer;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/* loaded from: classes.dex */
public class VivoGpsStateMachine extends StateMachine {
    private static final int CMD_GPS_RESET = 8;
    private static final int CMD_GPS_RESTART = 9;
    private static final int CMD_RECEIVED_GPS_LOCATION = 3;
    private static final int CMD_RECEIVED_NETWORK_LOCATION = 4;
    private static final int CMD_REPORT_FAKE_GPS = 10;
    private static final int CMD_SET_REQUEST = 5;
    private static final int CMD_START_GPS = 1;
    private static final int CMD_STOP_FUSED_GPS = 6;
    private static final int CMD_STOP_GPS = 2;
    private static final int CMD_SV_STATUS = 7;
    private static final int FAKE_GPS_REPORT_INTERVAL = 200;
    private static final int GNSS_REMOVE_UPDATE = 2;
    private static final int GNSS_REQUEST_UPDATE = 1;
    private static final int HIGH_SPEED = 1;
    private static final int LARGE_DISTANCE = 1;
    private static final String LOCAL_SCAN_PKG_NAME = "com.vivo.fusedgps";
    private static final int MAX_FAKE_GPS_REPORT = 10;
    private static final int MSG_REPORT_FAKE_GPS = 104;
    private static final int MSG_REPORT_FUSED = 107;
    private static final int MSG_SAVE_FUSEDDATA = 101;
    private static final int MSG_START_FUSED = 105;
    private static final int MSG_START_GPS = 102;
    private static final int MSG_STOP_FUSED = 106;
    private static final int MSG_STOP_GPS = 103;
    private static final String NET_TYPE_CELL = "cl";
    private static final String NET_TYPE_WF = "wf";
    private static final int RECORD_MAX_LIMIT = 3;
    private static final int SHORT_DISTANCE = 2;
    private static final int SLOW_SPEED = 2;
    private static final int STRONG_SIGNAL = 2;
    private static final String TAG = "VivoGpsStateMachine";
    private static final int WEAK_SIGNAL = 1;
    private boolean dataSaved;
    private String fusedPackage;
    private boolean isBadGpsReset;
    private boolean isForegroundApp;
    private boolean isFusedLocation;
    private boolean isWhiteApp;
    private boolean isWifiConnected;
    private HandlerThread mCollectThread;
    private ConnectivityManager mConnManager;
    private Context mContext;
    private int mCurrentDistanceState;
    private int mCurrentSignalState;
    private int mCurrentSpeedState;
    private int mCurrentStep;
    private MyHandler mDCHandler;
    private FusedBD mFusedBD;
    private LinkedList<String> mFusedSuccList;
    private int mHighSpeed;
    private int mLargeDistance;
    private double mLastDistance;
    private Location mLastFinalLocation;
    private int mLastGpsScore;
    private Location mLastNetworkLocation;
    private long mLastNetworkLocationTime;
    private Location mLastRealGpsLocation;
    private long mLastWifiScanResultTime;
    private LocationRecord mLocationGpsRecord;
    private LocationManager mLocationManager;
    private LocationRecord mLocationNetworkRecord;
    private Looper mLooper;
    private LocationListener mNlpLocationListener;
    private boolean mNlpRegistered;
    private int mPreviousStepCount;
    private IProcessObserver mProcessObserver;
    private int mReportedFakeGpsCount;
    private HashSet<String> mRequestGpsSet;
    ScanBroadcastReceiver mScanBroadcastReceiver;
    private HashMap<String, Integer> mScoreParameterHashMap;
    SensorEventListener mSensorListener;
    private SensorManager mSensorManager;
    private int mShortDistance;
    private Sensor mStepCountSensor;
    private int mStrongSignal;
    private boolean mSupportFusedGps;
    private Object mVCD;
    private VivoCoreLocationManager mVivoCoreLocationManager;
    private VivoDefaultState mVivoDefaultState;
    private VivoFusedGpsUtil mVivoFusedGpsUtil;
    private VivoFusedState mVivoFusedState;
    private VivoGpsDisableState mVivoGpsDisableState;
    private VivoGpsEnableState mVivoGpsEnableState;
    private VivoGpsOnlyState mVivoGpsOnlyState;
    private VivoResetGpsState mVivoResetGpsState;
    private int mWeakSignal;
    private WifiManager mWifiManager;
    private int mlastL4WifiCount;
    private int mlastWifiScore;
    private static boolean DEBUG = false;
    private static boolean mLogMessages = false;
    private static ArrayList<String> mVivoGnssWhiteList = new ArrayList<>();
    private static String mForgroundAppName = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    private static String mLastForgroundAppName = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    private static final Object mGpsLock = new Object();

    static /* synthetic */ int access$3208(VivoGpsStateMachine x0) {
        int i = x0.mStrongSignal;
        x0.mStrongSignal = i + 1;
        return i;
    }

    static /* synthetic */ int access$3308(VivoGpsStateMachine x0) {
        int i = x0.mWeakSignal;
        x0.mWeakSignal = i + 1;
        return i;
    }

    public VivoGpsStateMachine(Context context, VivoCoreLocationManager manager, Looper looper) {
        super(TAG, looper);
        this.mContext = null;
        this.mVivoCoreLocationManager = null;
        this.mLooper = null;
        this.mVivoDefaultState = new VivoDefaultState();
        this.mVivoGpsEnableState = new VivoGpsEnableState();
        this.mVivoGpsDisableState = new VivoGpsDisableState();
        this.mVivoGpsOnlyState = new VivoGpsOnlyState();
        this.mVivoFusedState = new VivoFusedState();
        this.mVivoResetGpsState = new VivoResetGpsState();
        this.mLocationManager = null;
        this.mNlpRegistered = false;
        this.mVivoFusedGpsUtil = null;
        this.mWifiManager = null;
        this.mConnManager = null;
        this.mScanBroadcastReceiver = null;
        this.isWifiConnected = false;
        this.mLastRealGpsLocation = null;
        this.mLastNetworkLocation = null;
        this.mLastFinalLocation = null;
        this.mLastNetworkLocationTime = -1L;
        this.mLastWifiScanResultTime = -1L;
        this.mLocationGpsRecord = null;
        this.mLocationNetworkRecord = null;
        this.mLastGpsScore = 0;
        this.mlastWifiScore = 0;
        this.mlastL4WifiCount = 0;
        this.mCurrentSignalState = 1;
        this.mWeakSignal = 0;
        this.mStrongSignal = 0;
        this.mCurrentSpeedState = 2;
        this.mHighSpeed = 0;
        this.mLastDistance = 0.0d;
        this.mCurrentDistanceState = 2;
        this.mLargeDistance = 0;
        this.mShortDistance = 0;
        this.mSupportFusedGps = false;
        this.mRequestGpsSet = new HashSet<>();
        this.isWhiteApp = false;
        this.isForegroundApp = false;
        this.mScoreParameterHashMap = new HashMap<>();
        this.mSensorManager = null;
        this.mStepCountSensor = null;
        this.mCurrentStep = 0;
        this.mPreviousStepCount = -1;
        this.isFusedLocation = false;
        this.isBadGpsReset = false;
        this.mReportedFakeGpsCount = 0;
        this.fusedPackage = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.dataSaved = false;
        this.mFusedBD = null;
        this.mVCD = null;
        this.mFusedSuccList = null;
        this.mCollectThread = null;
        this.mDCHandler = null;
        this.mProcessObserver = new IProcessObserver.Stub() { // from class: com.android.server.location.VivoGpsStateMachine.2
            public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
                VivoGpsStateMachine vivoGpsStateMachine = VivoGpsStateMachine.this;
                vivoGpsStateMachine.log("onForegroundActivitiesChanged: pid=" + pid + ", uid=" + uid + ", foregroundActivities=" + foregroundActivities);
                if (foregroundActivities) {
                    String unused = VivoGpsStateMachine.mForgroundAppName = VivoGpsStateMachine.this.getAppName(pid, uid);
                    VivoGpsStateMachine vivoGpsStateMachine2 = VivoGpsStateMachine.this;
                    vivoGpsStateMachine2.log("mForgroundAppName=" + VivoGpsStateMachine.mForgroundAppName + ", mLastForgroundAppName=" + VivoGpsStateMachine.mLastForgroundAppName);
                    String unused2 = VivoGpsStateMachine.mLastForgroundAppName = VivoGpsStateMachine.mForgroundAppName;
                }
            }

            public void onForegroundServicesChanged(int pid, int uid, int serviceTypes) {
            }

            public void onProcessDied(int pid, int uid) {
            }
        };
        this.mNlpLocationListener = new LocationListener() { // from class: com.android.server.location.VivoGpsStateMachine.3
            @Override // android.location.LocationListener
            public void onLocationChanged(Location location) {
                Location loc = new Location(location);
                VivoGpsStateMachine.this.sendMessage(4, loc);
                VivoGpsStateMachine.this.log("nlp onLocationChanged");
            }

            @Override // android.location.LocationListener
            public void onProviderDisabled(String s) {
            }

            @Override // android.location.LocationListener
            public void onProviderEnabled(String s) {
            }

            @Override // android.location.LocationListener
            public void onStatusChanged(String s, int n, Bundle bundle) {
            }
        };
        this.mSensorListener = new SensorEventListener() { // from class: com.android.server.location.VivoGpsStateMachine.4
            @Override // android.hardware.SensorEventListener
            public void onSensorChanged(SensorEvent event) {
                int tempStep = (int) event.values[0];
                if (tempStep >= 0) {
                    if (VivoGpsStateMachine.this.mPreviousStepCount < 0) {
                        VivoGpsStateMachine.this.mPreviousStepCount = tempStep;
                    } else {
                        VivoGpsStateMachine vivoGpsStateMachine = VivoGpsStateMachine.this;
                        vivoGpsStateMachine.mCurrentStep = tempStep - vivoGpsStateMachine.mPreviousStepCount;
                    }
                }
                VivoGpsStateMachine vivoGpsStateMachine2 = VivoGpsStateMachine.this;
                vivoGpsStateMachine2.log("currentStep: " + VivoGpsStateMachine.this.mCurrentStep + " tempStep: " + tempStep);
                if (VivoGpsStateMachine.this.mCurrentStep > 60 && VivoGpsStateMachine.this.mCurrentSignalState == 2) {
                    Message msg = VivoGpsStateMachine.this.obtainMessage(6);
                    msg.obj = 9;
                    VivoGpsStateMachine.this.sendMessageDelayed(msg, 1000L);
                }
            }

            @Override // android.hardware.SensorEventListener
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        this.mContext = context;
        this.mVivoCoreLocationManager = manager;
        this.mLooper = looper;
        this.mFusedSuccList = new LinkedList<>();
        addState(this.mVivoDefaultState);
        addState(this.mVivoGpsEnableState);
        addState(this.mVivoGpsOnlyState);
        addState(this.mVivoFusedState);
        addState(this.mVivoResetGpsState);
        addState(this.mVivoGpsDisableState);
        setInitialState(this.mVivoGpsDisableState);
        this.mLocationManager = (LocationManager) context.getSystemService("location");
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        SensorManager sensorManager = (SensorManager) context.getSystemService("sensor");
        this.mSensorManager = sensorManager;
        if (sensorManager != null) {
            this.mStepCountSensor = sensorManager.getDefaultSensor(19);
        }
        this.mScanBroadcastReceiver = new ScanBroadcastReceiver();
        this.mVivoFusedGpsUtil = new VivoFusedGpsUtil();
        registerProcessObserver();
        this.mLocationGpsRecord = new LocationRecord();
        this.mLocationNetworkRecord = new LocationRecord();
        this.mVCD = getVCD(context);
        HandlerThread handlerThread = new HandlerThread("fused_location_data");
        this.mCollectThread = handlerThread;
        handlerThread.start();
        this.mDCHandler = new MyHandler(this.mCollectThread.getLooper());
        this.mConnManager = (ConnectivityManager) context.getSystemService("connectivity");
        synchronized (mGpsLock) {
            initParameter();
        }
        start();
    }

    private Object getVCD(Context context) {
        if (this.mVCD == null) {
            this.mVCD = new VivoCollectData(context);
        }
        return this.mVCD;
    }

    /* loaded from: classes.dex */
    class FusedBD {
        private int startReason = 0;
        private int stopReason = 0;
        private boolean fakeGps = false;
        private String pkg = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        private int wifiScore = 0;
        private int gpsScore = 0;
        private double distance = 0.0d;
        private double gpsSpeed = 0.0d;

        FusedBD() {
        }

        public void reportFused(String pkg, int wifiScore, int gpsScore, double distance, double gpsSpeed) {
            this.pkg = pkg;
            this.wifiScore = wifiScore;
            this.gpsScore = gpsScore;
            this.distance = distance;
            this.gpsSpeed = gpsSpeed;
        }

        public void setStart(int type) {
            if (this.startReason == 0) {
                this.startReason = type;
            }
        }

        public void setStop(int type) {
            if (this.stopReason == 0) {
                this.stopReason = type;
            }
        }

        public void setFakeGps() {
            this.fakeGps = true;
        }

        public String toString() {
            String result = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + this.startReason + "," + this.stopReason + "," + this.fakeGps + "," + this.pkg + "," + this.wifiScore + "," + this.gpsScore + "," + this.distance + "," + this.gpsSpeed;
            return result;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class LocationRecord {
        private Location[] mLocationArray = new Location[3];
        private int size;

        public LocationRecord() {
            this.size = 0;
            this.size = 0;
            for (int i = 0; i < 3; i++) {
                this.mLocationArray[i] = null;
            }
        }

        public void addLocation(Location location) {
            int i = this.size;
            if (i >= 3) {
                for (int i2 = 0; i2 < 2; i2++) {
                    Location[] locationArr = this.mLocationArray;
                    Location location2 = locationArr[i2];
                    locationArr[i2] = locationArr[i2 + 1];
                }
                this.mLocationArray[2] = location;
            } else {
                this.mLocationArray[i] = location;
            }
            this.size++;
        }

        public void clear() {
            this.size = 0;
            for (int i = 0; i < 3; i++) {
                this.mLocationArray[i] = null;
            }
        }

        public int getSize() {
            return this.size;
        }

        public Location getFirstLocation() {
            return this.mLocationArray[0];
        }

        public Location getSecondLocation() {
            return this.mLocationArray[1];
        }

        public Location getLastLocation() {
            int i = this.size;
            if (i >= 3) {
                return this.mLocationArray[2];
            }
            if (i == 0) {
                return null;
            }
            return this.mLocationArray[i - 1];
        }
    }

    private void initParameter() {
        mVivoGnssWhiteList.add("com.gps.fusedgps");
        mVivoGnssWhiteList.add("com.baidu.map.location");
        mVivoGnssWhiteList.add("com.baidu.BaiduMap");
        mVivoGnssWhiteList.add("com.autonavi.minimap");
        this.mScoreParameterHashMap.put("GpsScoreThreshold", 80);
        this.mScoreParameterHashMap.put("L4WifiCount", 7);
        this.mScoreParameterHashMap.put("L3WifiCount", 6);
        this.mScoreParameterHashMap.put("L4WifiScore", 100);
        this.mScoreParameterHashMap.put("L3WifiScore", 0);
        this.mVivoFusedGpsUtil.setScoreParameterHashMap(this.mScoreParameterHashMap);
        this.mDCHandler.post(new Runnable() { // from class: com.android.server.location.VivoGpsStateMachine.1
            @Override // java.lang.Runnable
            public void run() {
                IntentFilter mIntentFilter = new IntentFilter();
                mIntentFilter.addAction("android.net.wifi.SCAN_RESULTS");
                mIntentFilter.addAction("android.intent.action.SCREEN_ON");
                mIntentFilter.addAction("android.intent.action.SCREEN_OFF");
                VivoGpsStateMachine.this.mContext.registerReceiver(VivoGpsStateMachine.this.mScanBroadcastReceiver, mIntentFilter);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reset() {
        this.mCurrentStep = 0;
        this.mPreviousStepCount = -1;
        this.mReportedFakeGpsCount = 0;
        this.mLastNetworkLocation = null;
        this.mLastNetworkLocationTime = -1L;
        this.mLastWifiScanResultTime = -1L;
        this.mLastFinalLocation = null;
        this.mLastRealGpsLocation = null;
        this.mLastGpsScore = 0;
        synchronized (mGpsLock) {
            this.mRequestGpsSet.clear();
        }
        this.mCurrentDistanceState = 2;
        this.mLargeDistance = 0;
        this.mShortDistance = 0;
        this.mCurrentSignalState = 1;
        this.mWeakSignal = 0;
        this.mStrongSignal = 0;
        this.mCurrentSpeedState = 2;
        this.mHighSpeed = 0;
        this.isForegroundApp = false;
        this.isWhiteApp = false;
        this.isWifiConnected = false;
        LocationRecord locationRecord = this.mLocationGpsRecord;
        if (locationRecord != null) {
            locationRecord.clear();
        }
        LocationRecord locationRecord2 = this.mLocationNetworkRecord;
        if (locationRecord2 != null) {
            locationRecord2.clear();
        }
        VivoFusedGpsUtil vivoFusedGpsUtil = this.mVivoFusedGpsUtil;
        if (vivoFusedGpsUtil != null) {
            vivoFusedGpsUtil.clear();
        }
    }

    /* loaded from: classes.dex */
    class VivoDefaultState extends State {
        VivoDefaultState() {
        }

        public void enter() {
            VivoGpsStateMachine.this.log(getName());
        }

        public boolean processMessage(Message msg) {
            int i = msg.what;
            return true;
        }

        public void exit() {
        }
    }

    /* loaded from: classes.dex */
    class VivoGpsEnableState extends State {
        VivoGpsEnableState() {
        }

        public void enter() {
            VivoGpsStateMachine.this.log(getName());
        }

        public boolean processMessage(Message msg) {
            int i = msg.what;
            return true;
        }

        public void exit() {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleGpsRequest(Message msg) {
        WorkSource source = (WorkSource) msg.obj;
        if (msg.arg1 == 1) {
            synchronized (mGpsLock) {
                for (int i = 0; i < source.size(); i++) {
                    log("add " + source.getName(i));
                    this.mRequestGpsSet.add(source.getName(i));
                }
            }
        } else {
            synchronized (mGpsLock) {
                for (int i2 = 0; i2 < source.size(); i2++) {
                    log("remove " + source.getName(i2));
                    this.mRequestGpsSet.remove(source.getName(i2));
                }
            }
        }
        synchronized (mGpsLock) {
            Iterator<String> reIt = this.mRequestGpsSet.iterator();
            boolean has = false;
            boolean isForegroundTemp = false;
            while (reIt.hasNext()) {
                String tempPkt = reIt.next();
                if (!has && mVivoGnssWhiteList != null && mVivoGnssWhiteList.size() >= 1) {
                    Iterator<String> it = mVivoGnssWhiteList.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        String whiteApp = it.next();
                        if (whiteApp.equals(tempPkt)) {
                            has = true;
                            this.fusedPackage = tempPkt;
                            break;
                        }
                    }
                }
                if (tempPkt.equals(mForgroundAppName)) {
                    isForegroundTemp = true;
                }
                if (has && isForegroundTemp) {
                    break;
                }
            }
            this.isWhiteApp = has;
            this.isForegroundApp = isForegroundTemp;
            log("isWhiteApp" + this.isWhiteApp + "isForegroundApp" + this.isForegroundApp);
        }
    }

    /* loaded from: classes.dex */
    class VivoGpsDisableState extends State {
        VivoGpsDisableState() {
        }

        public void enter() {
            VivoGpsStateMachine vivoGpsStateMachine = VivoGpsStateMachine.this;
            vivoGpsStateMachine.log(getName() + " enter");
            VivoGpsStateMachine.this.reset();
        }

        public boolean processMessage(Message msg) {
            int i = msg.what;
            if (i == 1) {
                VivoGpsStateMachine vivoGpsStateMachine = VivoGpsStateMachine.this;
                vivoGpsStateMachine.transitionTo(vivoGpsStateMachine.mVivoGpsOnlyState);
            } else if (i == 5) {
                VivoGpsStateMachine.this.handleGpsRequest(msg);
                if (VivoGpsStateMachine.this.mSupportFusedGps && VivoGpsStateMachine.this.isWhiteApp && VivoGpsStateMachine.this.isForegroundApp) {
                    Message msgf = VivoGpsStateMachine.this.mDCHandler.obtainMessage(105);
                    msgf.obj = 3;
                    VivoGpsStateMachine.this.mDCHandler.sendMessage(msgf);
                    VivoGpsStateMachine vivoGpsStateMachine2 = VivoGpsStateMachine.this;
                    vivoGpsStateMachine2.transitionTo(vivoGpsStateMachine2.mVivoFusedState);
                } else {
                    VivoGpsStateMachine vivoGpsStateMachine3 = VivoGpsStateMachine.this;
                    vivoGpsStateMachine3.transitionTo(vivoGpsStateMachine3.mVivoGpsOnlyState);
                }
                VivoGpsStateMachine vivoGpsStateMachine4 = VivoGpsStateMachine.this;
                vivoGpsStateMachine4.log("CMD_SET_REQUEST has=" + VivoGpsStateMachine.this.isWhiteApp + " requestSet= " + VivoGpsStateMachine.this.mRequestGpsSet.toString() + " reportLocation:" + msg.arg1);
            } else {
                return false;
            }
            return true;
        }

        public void exit() {
            VivoGpsStateMachine vivoGpsStateMachine = VivoGpsStateMachine.this;
            vivoGpsStateMachine.log(getName() + " exit");
        }
    }

    /* loaded from: classes.dex */
    class VivoGpsOnlyState extends State {
        VivoGpsOnlyState() {
        }

        public void enter() {
            VivoGpsStateMachine.this.log(getName());
        }

        public boolean processMessage(Message msg) {
            int i = msg.what;
            if (i != 1) {
                if (i == 2) {
                    VivoGpsStateMachine vivoGpsStateMachine = VivoGpsStateMachine.this;
                    vivoGpsStateMachine.transitionTo(vivoGpsStateMachine.mVivoGpsDisableState);
                } else if (i == 3) {
                    Location mGpsLocation = new Location((Location) msg.obj);
                    VivoGpsStateMachine.this.mVivoCoreLocationManager.reportFusedLocation(mGpsLocation, false);
                    VivoGpsStateMachine vivoGpsStateMachine2 = VivoGpsStateMachine.this;
                    vivoGpsStateMachine2.log("reportGpsLocation directly gps:" + mGpsLocation.getLatitude() + "," + mGpsLocation.getLongitude());
                } else if (i != 5) {
                    return false;
                } else {
                    VivoGpsStateMachine.this.handleGpsRequest(msg);
                    if (VivoGpsStateMachine.this.mSupportFusedGps && VivoGpsStateMachine.this.isWhiteApp && VivoGpsStateMachine.this.isForegroundApp) {
                        Message msgf = VivoGpsStateMachine.this.mDCHandler.obtainMessage(105);
                        msgf.obj = 1;
                        VivoGpsStateMachine.this.mDCHandler.sendMessage(msgf);
                        VivoGpsStateMachine vivoGpsStateMachine3 = VivoGpsStateMachine.this;
                        vivoGpsStateMachine3.transitionTo(vivoGpsStateMachine3.mVivoFusedState);
                    }
                    VivoGpsStateMachine vivoGpsStateMachine4 = VivoGpsStateMachine.this;
                    vivoGpsStateMachine4.log("CMD_SET_REQUEST has=" + VivoGpsStateMachine.this.isWhiteApp + " requestSet= " + VivoGpsStateMachine.this.mRequestGpsSet.toString() + " reportLocation:" + msg.arg1);
                }
            }
            return true;
        }

        public void exit() {
        }
    }

    /* loaded from: classes.dex */
    class VivoFusedState extends State {
        Bundle bundle = null;

        VivoFusedState() {
        }

        public void enter() {
            VivoGpsStateMachine.this.log(getName());
            if (!VivoGpsStateMachine.this.mNlpRegistered) {
                VivoGpsStateMachine.this.scanWifi();
                VivoGpsStateMachine.this.registerListner();
                NetworkInfo mWifiInfo = VivoGpsStateMachine.this.mConnManager.getNetworkInfo(1);
                VivoGpsStateMachine.this.isWifiConnected = mWifiInfo.isConnected();
            }
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (!VivoGpsStateMachine.this.mNlpRegistered) {
                        VivoGpsStateMachine.this.scanWifi();
                        VivoGpsStateMachine.this.registerListner();
                        break;
                    }
                    break;
                case 2:
                    if (VivoGpsStateMachine.this.mNlpRegistered) {
                        VivoGpsStateMachine.this.unregisterListner();
                    }
                    synchronized (VivoGpsStateMachine.mGpsLock) {
                        VivoGpsStateMachine.this.mRequestGpsSet.clear();
                    }
                    VivoGpsStateMachine vivoGpsStateMachine = VivoGpsStateMachine.this;
                    vivoGpsStateMachine.transitionTo(vivoGpsStateMachine.mVivoGpsDisableState);
                    break;
                case 3:
                    VivoGpsStateMachine.this.removeMessages(10);
                    Location mGpsLocation = new Location((Location) msg.obj);
                    VivoGpsStateMachine.this.mLastRealGpsLocation = mGpsLocation;
                    VivoGpsStateMachine.this.reportFusedGpsLocation(mGpsLocation, true);
                    VivoGpsStateMachine.this.mLocationGpsRecord.addLocation(mGpsLocation);
                    if (VivoGpsStateMachine.this.mLastNetworkLocationTime >= 0 && SystemClock.elapsedRealtime() - VivoGpsStateMachine.this.mLastNetworkLocationTime > VivoBackupCommonUtil.TIMEOUT_MONITOR_INTERVAL && VivoGpsStateMachine.this.mNlpRegistered) {
                        VivoGpsStateMachine vivoGpsStateMachine2 = VivoGpsStateMachine.this;
                        vivoGpsStateMachine2.log(SystemClock.elapsedRealtime() + " CMD_STOP_FUSED_GPS");
                        Message msgf = VivoGpsStateMachine.this.obtainMessage(6);
                        msgf.obj = 1;
                        VivoGpsStateMachine.this.sendMessage(msgf);
                    }
                    VivoGpsStateMachine vivoGpsStateMachine3 = VivoGpsStateMachine.this;
                    vivoGpsStateMachine3.log("CMD_RECEIVED_GPS_LOCATION location:" + mGpsLocation.getLatitude() + "," + mGpsLocation.getLongitude() + " LastNetworkLocationTime:" + VivoGpsStateMachine.this.mLastNetworkLocationTime);
                    break;
                case 4:
                    VivoGpsStateMachine.this.removeMessages(10);
                    Location mNetworkLocation = new Location((Location) msg.obj);
                    Bundle extras = mNetworkLocation.getExtras();
                    this.bundle = extras;
                    if (extras == null) {
                        VivoGpsStateMachine.this.log("CMD_RECEIVED_NETWORK_LOCATION extras is null");
                        break;
                    } else {
                        String netType = extras.getString("netType", null);
                        if (netType != null && netType.equals(VivoGpsStateMachine.NET_TYPE_WF) && (VivoGpsStateMachine.this.isWifiConnected || (!VivoGpsStateMachine.this.isWifiConnected && VivoGpsStateMachine.this.mLastWifiScanResultTime >= 0 && SystemClock.elapsedRealtime() - VivoGpsStateMachine.this.mLastWifiScanResultTime < 15000))) {
                            VivoGpsStateMachine.this.mLastNetworkLocation = mNetworkLocation;
                            VivoGpsStateMachine.this.mLocationNetworkRecord.addLocation(mNetworkLocation);
                            VivoGpsStateMachine.this.mLastNetworkLocationTime = SystemClock.elapsedRealtime();
                        }
                        if (VivoGpsStateMachine.this.mLastRealGpsLocation == null) {
                            VivoGpsStateMachine.this.mLastNetworkLocation = mNetworkLocation;
                            VivoGpsStateMachine.this.reportFusedGpsLocation(mNetworkLocation);
                            VivoGpsStateMachine.this.mLastNetworkLocationTime = SystemClock.elapsedRealtime();
                        }
                        VivoGpsStateMachine vivoGpsStateMachine4 = VivoGpsStateMachine.this;
                        vivoGpsStateMachine4.log("CMD_RECEIVED_NETWORK_LOCATION location: " + mNetworkLocation.getLatitude() + "," + mNetworkLocation.getLongitude() + ", extras: " + mNetworkLocation.getExtras() + ",netType:" + netType + " mLastWifiScanResultTime:" + VivoGpsStateMachine.this.mLastWifiScanResultTime + " mLastNetworkLocation: " + VivoGpsStateMachine.this.mLastNetworkLocation);
                        break;
                    }
                    break;
                case 5:
                    VivoGpsStateMachine.this.handleGpsRequest(msg);
                    if (!VivoGpsStateMachine.this.mSupportFusedGps || !VivoGpsStateMachine.this.isWhiteApp || !VivoGpsStateMachine.this.isForegroundApp) {
                        Message msgf2 = VivoGpsStateMachine.this.obtainMessage(6);
                        msgf2.obj = 9;
                        VivoGpsStateMachine.this.sendMessage(msgf2);
                    }
                    VivoGpsStateMachine vivoGpsStateMachine5 = VivoGpsStateMachine.this;
                    vivoGpsStateMachine5.log(getName() + " CMD_SET_REQUEST has=" + VivoGpsStateMachine.this.isWhiteApp + " requestSet= " + VivoGpsStateMachine.this.mRequestGpsSet.toString() + " reportLocation:" + msg.arg1);
                    break;
                case 6:
                    if (VivoGpsStateMachine.this.mNlpRegistered) {
                        VivoGpsStateMachine.this.unregisterListner();
                    }
                    int stopFusedType = ((Integer) msg.obj).intValue();
                    VivoGpsStateMachine.this.sendStopFusedMsg(stopFusedType);
                    VivoGpsStateMachine vivoGpsStateMachine6 = VivoGpsStateMachine.this;
                    vivoGpsStateMachine6.transitionTo(vivoGpsStateMachine6.mVivoGpsOnlyState);
                    break;
                case 7:
                    Bundle bundle = (Bundle) msg.obj;
                    this.bundle = bundle;
                    if (bundle != null) {
                        int svCount = bundle.getInt("svCount");
                        float[] mCn0s = this.bundle.getFloatArray("cn0s");
                        float[] mSvElevations = this.bundle.getFloatArray("SvElevations");
                        float[] mSvAzimuths = this.bundle.getFloatArray("SvAzimuths");
                        VivoGpsStateMachine vivoGpsStateMachine7 = VivoGpsStateMachine.this;
                        vivoGpsStateMachine7.mLastGpsScore = vivoGpsStateMachine7.mVivoFusedGpsUtil.calculateGpsScore(svCount, mCn0s, mSvElevations, mSvAzimuths);
                        int gpsScoreThreshold = 80;
                        synchronized (VivoGpsStateMachine.mGpsLock) {
                            if (VivoGpsStateMachine.this.mScoreParameterHashMap.containsKey("gpsScoreThreshold") && ((Integer) VivoGpsStateMachine.this.mScoreParameterHashMap.get("gpsScoreThreshold")).intValue() > 0) {
                                gpsScoreThreshold = ((Integer) VivoGpsStateMachine.this.mScoreParameterHashMap.get("gpsScoreThreshold")).intValue();
                            }
                        }
                        if (VivoGpsStateMachine.this.mLastGpsScore > gpsScoreThreshold || (VivoGpsStateMachine.this.mLastGpsScore > gpsScoreThreshold - 5 && VivoGpsStateMachine.this.mLastGpsScore < gpsScoreThreshold && VivoGpsStateMachine.this.mVivoFusedGpsUtil.getRateOfCn0AndOthers() > 0.55d)) {
                            VivoGpsStateMachine.access$3208(VivoGpsStateMachine.this);
                            if (VivoGpsStateMachine.this.mStrongSignal >= 3) {
                                VivoGpsStateMachine.this.mWeakSignal = 0;
                                if (VivoGpsStateMachine.this.mStrongSignal >= 5) {
                                    VivoGpsStateMachine.this.mCurrentSignalState = 2;
                                }
                            }
                        } else {
                            VivoGpsStateMachine.access$3308(VivoGpsStateMachine.this);
                            if (VivoGpsStateMachine.this.mWeakSignal >= 3) {
                                VivoGpsStateMachine.this.mStrongSignal = 0;
                                if (VivoGpsStateMachine.this.mWeakSignal >= 5) {
                                    VivoGpsStateMachine.this.mCurrentSignalState = 1;
                                }
                            }
                        }
                        VivoGpsStateMachine vivoGpsStateMachine8 = VivoGpsStateMachine.this;
                        vivoGpsStateMachine8.log("onReportSvStatus svcount: " + svCount + " GpsScore: " + VivoGpsStateMachine.this.mLastGpsScore + " weakSignal: " + VivoGpsStateMachine.this.mWeakSignal + " strongSignal: " + VivoGpsStateMachine.this.mStrongSignal + " currentSignalState: " + VivoGpsStateMachine.this.mCurrentSignalState);
                        break;
                    }
                    break;
                case 8:
                    VivoGpsStateMachine vivoGpsStateMachine9 = VivoGpsStateMachine.this;
                    vivoGpsStateMachine9.transitionTo(vivoGpsStateMachine9.mVivoResetGpsState);
                    break;
                case 10:
                    if (VivoGpsStateMachine.this.mLastNetworkLocation != null && VivoGpsStateMachine.this.mLastRealGpsLocation == null) {
                        VivoGpsStateMachine vivoGpsStateMachine10 = VivoGpsStateMachine.this;
                        vivoGpsStateMachine10.reportFusedGpsLocation(vivoGpsStateMachine10.mLastNetworkLocation);
                        break;
                    }
                    break;
            }
            return true;
        }

        public void exit() {
            if (!VivoGpsStateMachine.this.mNlpRegistered) {
                VivoGpsStateMachine.this.reset();
                VivoGpsStateMachine.this.removeMessages(10);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg == null) {
                return;
            }
            switch (msg.what) {
                case 101:
                    String content = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                    for (int i = 0; i < 3; i++) {
                        try {
                            content = content + ((String) VivoGpsStateMachine.this.mFusedSuccList.removeFirst()) + ";";
                        } catch (Exception e) {
                            e.printStackTrace();
                            return;
                        }
                    }
                    if (VivoGpsStateMachine.this.mVCD != null && ((VivoCollectData) VivoGpsStateMachine.this.mVCD).getControlInfo("203")) {
                        HashMap<String, String> params = new HashMap<>(1);
                        VivoGpsStateMachine.this.log("writeData" + content);
                        params.put("info", content);
                        ((VivoCollectData) VivoGpsStateMachine.this.mVCD).writeData("203", "2038", 0L, 0L, System.currentTimeMillis(), 1, params);
                        ((VivoCollectData) VivoGpsStateMachine.this.mVCD).flush();
                        return;
                    }
                    VivoGpsStateMachine.this.log("Vcd is not open for 203, Wait for Next time check.");
                    return;
                case 102:
                    VivoGpsStateMachine vivoGpsStateMachine = VivoGpsStateMachine.this;
                    vivoGpsStateMachine.mFusedBD = new FusedBD();
                    return;
                case 103:
                    if (VivoGpsStateMachine.this.mFusedBD != null) {
                        VivoGpsStateMachine.this.log("MSG_STOP_GPS" + VivoGpsStateMachine.this.mFusedBD.toString());
                        VivoGpsStateMachine.this.mFusedSuccList.add(VivoGpsStateMachine.this.mFusedBD.toString());
                        HashMap<String, String> params2 = new HashMap<>(3);
                        params2.put(VCD_FF_1.UUID_STR, UUID.randomUUID().toString());
                        params2.put("start", String.valueOf(VivoGpsStateMachine.this.mFusedBD.startReason));
                        params2.put("stop", String.valueOf(VivoGpsStateMachine.this.mFusedBD.startReason));
                        params2.put("fake", String.valueOf(VivoGpsStateMachine.this.mFusedBD.fakeGps));
                        params2.put("pkg", VivoGpsStateMachine.this.mFusedBD.pkg);
                        params2.put("wifiscore", String.valueOf(VivoGpsStateMachine.this.mFusedBD.wifiScore));
                        params2.put("gpsscore", String.valueOf(VivoGpsStateMachine.this.mFusedBD.gpsScore));
                        params2.put("distance", String.valueOf(VivoGpsStateMachine.this.mFusedBD.distance));
                        params2.put("speed", String.valueOf(VivoGpsStateMachine.this.mFusedBD.gpsSpeed));
                        EventTransfer.getInstance().singleEvent("F500", "F500|10008", System.currentTimeMillis(), 0L, params2);
                        if (VivoGpsStateMachine.this.mFusedSuccList.size() >= 3) {
                            Message msgf = VivoGpsStateMachine.this.mDCHandler.obtainMessage(101);
                            VivoGpsStateMachine.this.mDCHandler.sendMessage(msgf);
                        }
                        VivoGpsStateMachine.this.mFusedBD = null;
                        return;
                    }
                    return;
                case 104:
                    if (VivoGpsStateMachine.this.mFusedBD != null) {
                        VivoGpsStateMachine.this.mFusedBD.setFakeGps();
                        return;
                    }
                    return;
                case 105:
                    if (VivoGpsStateMachine.this.mFusedBD != null) {
                        VivoGpsStateMachine.this.log("MSG_START_FUSED" + ((Integer) msg.obj).intValue());
                        VivoGpsStateMachine.this.mFusedBD.setStart(((Integer) msg.obj).intValue());
                        return;
                    }
                    return;
                case 106:
                    if (VivoGpsStateMachine.this.mFusedBD != null) {
                        VivoGpsStateMachine.this.log("MSG_STOP_FUSED" + ((Integer) msg.obj).intValue());
                        VivoGpsStateMachine.this.mFusedBD.setStop(((Integer) msg.obj).intValue());
                        return;
                    }
                    return;
                case 107:
                    if (VivoGpsStateMachine.this.mFusedBD != null) {
                        VivoGpsStateMachine.this.mFusedBD.reportFused(msg.getData().getString("pkg"), msg.getData().getInt("wifiScore"), msg.getData().getInt("gpsScore"), msg.getData().getDouble("distance"), msg.getData().getDouble("speed"));
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    /* loaded from: classes.dex */
    class VivoResetGpsState extends State {
        boolean isResetGps = false;

        VivoResetGpsState() {
        }

        public void enter() {
            VivoGpsStateMachine.this.log(getName());
            VivoGpsStateMachine.this.mVivoCoreLocationManager.enableGps(false);
            VivoGpsStateMachine.this.removeMessages(9);
            VivoGpsStateMachine.this.removeMessages(6);
            VivoGpsStateMachine.this.sendMessageDelayed(9, 3000L);
            Message msg = VivoGpsStateMachine.this.obtainMessage(6);
            msg.obj = 2;
            VivoGpsStateMachine.this.sendMessageDelayed(msg, 10000L);
            this.isResetGps = true;
            if (VivoGpsStateMachine.this.mLocationGpsRecord != null && VivoGpsStateMachine.this.mLocationGpsRecord.getSize() <= 3 && VivoGpsStateMachine.this.mLastRealGpsLocation != null) {
                if ((VivoGpsStateMachine.this.mLastRealGpsLocation.hasSpeed() ? VivoGpsStateMachine.this.mLastRealGpsLocation.getSpeed() : -1.0f) < 3.0f && VivoGpsStateMachine.this.mVivoFusedGpsUtil.getRateOfCn0AndOthers() < 0.3d) {
                    VivoGpsStateMachine.this.isBadGpsReset = true;
                }
            }
        }

        public boolean processMessage(Message msg) {
            Location lastNetworkLocation;
            int i = msg.what;
            if (i != 2) {
                if (i == 3) {
                    VivoGpsStateMachine vivoGpsStateMachine = VivoGpsStateMachine.this;
                    vivoGpsStateMachine.log("CMD_RECEIVED_GPS_LOCATION LocationNetworkRecord: " + VivoGpsStateMachine.this.mLocationNetworkRecord + " size:" + VivoGpsStateMachine.this.mLocationNetworkRecord.getSize() + " lastNetworkLocation:" + VivoGpsStateMachine.this.mLocationNetworkRecord.getLastLocation());
                    Location mGpsLocation = new Location((Location) msg.obj);
                    if (VivoGpsStateMachine.this.mLocationNetworkRecord != null && VivoGpsStateMachine.this.mLocationNetworkRecord.getSize() > 0 && (lastNetworkLocation = VivoGpsStateMachine.this.mLocationNetworkRecord.getLastLocation()) != null) {
                        double distance = VivoGpsStateMachine.this.mVivoFusedGpsUtil.calculateDistance(mGpsLocation, lastNetworkLocation);
                        if (distance <= 200.0d || VivoGpsStateMachine.this.mVivoFusedGpsUtil.getRateOfCn0AndOthers() < 0.3d) {
                            if ((mGpsLocation.hasSpeed() ? mGpsLocation.getSpeed() : -1.0f) <= 3.0f) {
                                if (distance <= 200.0d || VivoGpsStateMachine.this.mVivoFusedGpsUtil.getRateOfCn0AndOthers() >= 0.3d) {
                                    VivoGpsStateMachine.this.removeMessages(6);
                                    Message msgf = VivoGpsStateMachine.this.mDCHandler.obtainMessage(105);
                                    msgf.obj = 2;
                                    VivoGpsStateMachine.this.mDCHandler.sendMessage(msgf);
                                    VivoGpsStateMachine vivoGpsStateMachine2 = VivoGpsStateMachine.this;
                                    vivoGpsStateMachine2.transitionTo(vivoGpsStateMachine2.mVivoFusedState);
                                    VivoGpsStateMachine.this.sendMessageDelayed(3, mGpsLocation, 500L);
                                    VivoGpsStateMachine.this.isBadGpsReset = false;
                                } else {
                                    VivoGpsStateMachine.this.isBadGpsReset = true;
                                }
                            }
                        }
                        Message msgf2 = VivoGpsStateMachine.this.obtainMessage(6);
                        msgf2.obj = 4;
                        VivoGpsStateMachine.this.sendMessage(msgf2);
                        VivoGpsStateMachine.this.isBadGpsReset = false;
                    }
                } else if (i == 4) {
                    Location mNetworkLocation = new Location((Location) msg.obj);
                    Bundle bundle = mNetworkLocation.getExtras();
                    if (bundle == null) {
                        VivoGpsStateMachine.this.log("CMD_RECEIVED_NETWORK_LOCATION extras is null");
                    } else {
                        String netType = bundle.getString("netType", null);
                        if (netType != null && netType.equals(VivoGpsStateMachine.NET_TYPE_WF)) {
                            if (VivoGpsStateMachine.this.isWifiConnected || (!VivoGpsStateMachine.this.isWifiConnected && VivoGpsStateMachine.this.mLastWifiScanResultTime >= 0 && SystemClock.elapsedRealtime() - VivoGpsStateMachine.this.mLastWifiScanResultTime < 15000)) {
                                VivoGpsStateMachine.this.mLastNetworkLocation = mNetworkLocation;
                                VivoGpsStateMachine.this.mLocationNetworkRecord.addLocation(mNetworkLocation);
                                VivoGpsStateMachine.this.mLastNetworkLocationTime = SystemClock.elapsedRealtime();
                            }
                            if (VivoGpsStateMachine.this.isBadGpsReset) {
                                VivoGpsStateMachine.this.mLastNetworkLocation = mNetworkLocation;
                                VivoGpsStateMachine.this.reportFusedGpsLocation(mNetworkLocation);
                                VivoGpsStateMachine.this.mLastNetworkLocationTime = SystemClock.elapsedRealtime();
                                VivoGpsStateMachine.this.removeMessages(6);
                                Message msgf3 = VivoGpsStateMachine.this.obtainMessage(6);
                                msgf3.obj = 5;
                                VivoGpsStateMachine.this.sendMessageDelayed(msgf3, 11000L);
                            }
                        }
                        VivoGpsStateMachine vivoGpsStateMachine3 = VivoGpsStateMachine.this;
                        vivoGpsStateMachine3.log("CMD_RECEIVED_NETWORK_LOCATION location: " + mNetworkLocation.getLatitude() + "," + mNetworkLocation.getLongitude() + ", extras: " + mNetworkLocation.getExtras() + ",netType:" + netType + " mLastWifiScanResultTime:" + VivoGpsStateMachine.this.mLastWifiScanResultTime + " mLastNetworkLocation: " + VivoGpsStateMachine.this.mLastNetworkLocation);
                    }
                } else if (i == 5) {
                    VivoGpsStateMachine.this.handleGpsRequest(msg);
                    if (!VivoGpsStateMachine.this.mSupportFusedGps || !VivoGpsStateMachine.this.isWhiteApp || !VivoGpsStateMachine.this.isForegroundApp) {
                        Message msgf4 = VivoGpsStateMachine.this.obtainMessage(6);
                        msgf4.obj = 3;
                        VivoGpsStateMachine.this.sendMessage(msgf4);
                    }
                    VivoGpsStateMachine vivoGpsStateMachine4 = VivoGpsStateMachine.this;
                    vivoGpsStateMachine4.log(getName() + " CMD_SET_REQUEST has=" + VivoGpsStateMachine.this.isWhiteApp + " requestSet= " + VivoGpsStateMachine.this.mRequestGpsSet.toString() + " reportLocation:" + msg.arg1);
                } else if (i == 6) {
                    if (VivoGpsStateMachine.this.mNlpRegistered) {
                        VivoGpsStateMachine.this.unregisterListner();
                    }
                    int stopFusedType = ((Integer) msg.obj).intValue();
                    VivoGpsStateMachine.this.sendStopFusedMsg(stopFusedType);
                    VivoGpsStateMachine vivoGpsStateMachine5 = VivoGpsStateMachine.this;
                    vivoGpsStateMachine5.transitionTo(vivoGpsStateMachine5.mVivoGpsOnlyState);
                } else if (i == 9) {
                    VivoGpsStateMachine.this.mVivoCoreLocationManager.enableGps(true);
                    this.isResetGps = false;
                }
            } else if (!this.isResetGps) {
                if (VivoGpsStateMachine.this.mNlpRegistered) {
                    VivoGpsStateMachine.this.unregisterListner();
                }
                synchronized (VivoGpsStateMachine.mGpsLock) {
                    VivoGpsStateMachine.this.mRequestGpsSet.clear();
                }
                VivoGpsStateMachine vivoGpsStateMachine6 = VivoGpsStateMachine.this;
                vivoGpsStateMachine6.transitionTo(vivoGpsStateMachine6.mVivoGpsDisableState);
            }
            return true;
        }

        public void exit() {
            if (!VivoGpsStateMachine.this.mNlpRegistered) {
                VivoGpsStateMachine.this.reset();
            }
            VivoGpsStateMachine.this.isBadGpsReset = false;
        }
    }

    public void onSetRequest(ProviderRequest request, WorkSource source) {
        log("onSetRequest");
        if (request != null && source != null) {
            if (source.size() >= 1) {
                sendMessageDelayed(5, request.reportLocation ? 1 : 2, 0, source, 100L);
                return;
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("onSetRequest request=");
        sb.append(request == null ? "Null" : Boolean.valueOf(request.reportLocation));
        sb.append(" source=");
        sb.append(source != null ? Integer.valueOf(source.size()) : "Null");
        log(sb.toString());
    }

    public void onStartNavigating() {
        log("onStartNavigating");
        this.dataSaved = false;
        sendMessageDelayed(1, 100L);
        Message msgf = this.mDCHandler.obtainMessage(102);
        this.mDCHandler.sendMessage(msgf);
    }

    public void onStopNavigating() {
        log("onStopNavigating");
        this.dataSaved = false;
        sendMessage(2);
        Message msgf = this.mDCHandler.obtainMessage(103);
        this.mDCHandler.sendMessage(msgf);
    }

    public void onReportGpsLocation(Location location) {
        System.currentTimeMillis();
        Location loc = new Location(location);
        sendMessage(3, loc);
    }

    public void onReportSvStatus(int svCount, float[] mCn0s, float[] mSvElevations, float[] mSvAzimuths) {
        Bundle bundle = new Bundle();
        bundle.putInt("svCount", svCount);
        bundle.putFloatArray("cn0s", mCn0s);
        bundle.putFloatArray("SvElevations", mSvElevations);
        bundle.putFloatArray("SvAzimuths", mSvAzimuths);
        sendMessage(7, bundle);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerListner() {
        log("registerListner");
        this.mLocationManager.requestLocationUpdates("network", 1000L, 0.0f, this.mNlpLocationListener, this.mLooper);
        Sensor sensor = this.mStepCountSensor;
        if (sensor != null) {
            this.mSensorManager.registerListener(this.mSensorListener, sensor, 3);
        }
        this.mLastNetworkLocationTime = SystemClock.elapsedRealtime();
        this.mLastWifiScanResultTime = SystemClock.elapsedRealtime();
        this.mNlpRegistered = true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unregisterListner() {
        log("unregisterListner");
        if (this.mNlpRegistered) {
            this.mLocationManager.removeUpdates(this.mNlpLocationListener);
            Sensor sensor = this.mStepCountSensor;
            if (sensor != null) {
                this.mSensorManager.unregisterListener(this.mSensorListener, sensor);
            }
        }
        this.mCurrentStep = 0;
        this.mPreviousStepCount = -1;
        this.mNlpRegistered = false;
        this.mLastNetworkLocation = null;
        this.mLastNetworkLocationTime = -1L;
        this.mLastWifiScanResultTime = -1L;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scanWifi() {
        WifiManager wifiManager = this.mWifiManager;
        if (wifiManager == null) {
            log("scanWifi mWifiManager=NULL, return;");
            return;
        }
        try {
            Class<?> ownerClass = wifiManager.getClass();
            Method localMethod = ownerClass.getDeclaredMethod("startFastScan", String.class);
            localMethod.setAccessible(true);
            localMethod.invoke(this.mWifiManager, LOCAL_SCAN_PKG_NAME);
        } catch (Exception e) {
            VLog.e(TAG, VLog.getStackTraceString(e));
        }
        log("scanWifi finished");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendStopFusedMsg(int type) {
        Message msg = this.mDCHandler.obtainMessage(106);
        msg.obj = Integer.valueOf(type);
        this.mDCHandler.sendMessage(msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportFusedGpsLocation(Location location) {
        reportFusedGpsLocation(location, false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportFusedGpsLocation(Location location, boolean isGps) {
        LocationRecord locationRecord;
        int i;
        int i2;
        if (location != null) {
            this.mLastFinalLocation = new Location(location);
            StringBuilder sb = new StringBuilder();
            sb.append("reportFusedGpsLocation provider : ");
            sb.append(this.mLastFinalLocation.getProvider());
            sb.append(" speed:");
            sb.append(this.mLastFinalLocation.hasSpeed() ? this.mLastFinalLocation.getSpeed() : -1.0f);
            log(sb.toString());
            boolean z = false;
            if (this.mLastRealGpsLocation == null) {
                if (!this.mLastFinalLocation.getProvider().equals("gps")) {
                    this.mLastFinalLocation.setProvider("gps");
                }
                this.isFusedLocation = false;
                if (this.mReportedFakeGpsCount == 0) {
                    Message msgf = this.mDCHandler.obtainMessage(104);
                    this.mDCHandler.sendMessage(msgf);
                }
                int i3 = this.mReportedFakeGpsCount + 1;
                this.mReportedFakeGpsCount = i3;
                if (i3 < 10) {
                    removeMessages(10);
                    sendMessageDelayed(10, 200L);
                }
            } else {
                log("gpsScore: " + this.mLastGpsScore + " wifiScore: " + this.mlastWifiScore + " currentSignalState: " + this.mCurrentSignalState);
                if (this.mLastNetworkLocation != null && this.mLastGpsScore > 0 && this.mlastWifiScore > 0 && this.mLastFinalLocation.hasSpeed() && this.mCurrentSignalState == 1) {
                    double distance = this.mVivoFusedGpsUtil.calculateDistance(this.mLastRealGpsLocation, this.mLastNetworkLocation);
                    double rateOfGps = (this.mLastGpsScore * 1.0d) / (i + this.mlastWifiScore);
                    log("distance between gpslocation and networklocation: " + distance + " rateOfGps: " + rateOfGps);
                    if (this.mLastFinalLocation.getSpeed() < 3.0f && this.mlastWifiScore >= 80 && distance > 0.0d && distance <= 200.0d) {
                        this.mVivoFusedGpsUtil.calculateLocation(this.mLastFinalLocation, this.mLastRealGpsLocation, this.mLastNetworkLocation, rateOfGps);
                        this.isFusedLocation = true;
                    } else if (this.mLastFinalLocation.getSpeed() < 2.0f && (i2 = this.mlastWifiScore) <= 80 && i2 >= 30 && this.mLastGpsScore <= 60 && distance > 0.0d && distance <= 100.0d) {
                        this.mVivoFusedGpsUtil.calculateLocation(this.mLastFinalLocation, this.mLastRealGpsLocation, this.mLastNetworkLocation, rateOfGps);
                        this.isFusedLocation = true;
                    } else if (this.mLastFinalLocation.getSpeed() < 2.0f && this.mlastWifiScore <= 30 && this.mlastL4WifiCount >= 2 && this.mLastGpsScore <= 30 && distance > 0.0d && distance <= 100.0d) {
                        this.mVivoFusedGpsUtil.calculateLocation(this.mLastFinalLocation, this.mLastRealGpsLocation, this.mLastNetworkLocation, rateOfGps);
                        this.isFusedLocation = true;
                    } else {
                        this.isFusedLocation = false;
                    }
                    if (distance > 200.0d && this.mlastWifiScore >= 80) {
                        LocationRecord locationRecord2 = this.mLocationGpsRecord;
                        if (locationRecord2 != null && locationRecord2.getSize() <= 3 && this.mLastFinalLocation.getSpeed() < 3.0f) {
                            sendMessage(8);
                            return;
                        }
                        LocationRecord locationRecord3 = this.mLocationGpsRecord;
                        if (locationRecord3 != null && locationRecord3.getSize() > 3 && this.mLastFinalLocation.getSpeed() < 3.0f) {
                            Location mNetworkSecondLocation = this.mLocationNetworkRecord.getSecondLocation();
                            Location mNetworkLastLocation = this.mLocationNetworkRecord.getLastLocation();
                            if (mNetworkSecondLocation != null && mNetworkLastLocation != null) {
                                if (this.mVivoFusedGpsUtil.calculateDistance(this.mLastRealGpsLocation, mNetworkLastLocation) <= 200.0d || this.mVivoFusedGpsUtil.calculateDistance(this.mLastRealGpsLocation, mNetworkSecondLocation) > 200.0d) {
                                    if (this.mVivoFusedGpsUtil.calculateDistance(this.mLastRealGpsLocation, mNetworkLastLocation) > 200.0d && this.mVivoFusedGpsUtil.calculateDistance(this.mLastRealGpsLocation, mNetworkSecondLocation) > 200.0d) {
                                        sendMessage(8);
                                        return;
                                    }
                                } else {
                                    log("CMD_STOP_FUSED_GPS 1");
                                    Message msg = obtainMessage(6);
                                    msg.obj = 6;
                                    sendMessage(msg);
                                }
                            } else {
                                sendMessage(8);
                                return;
                            }
                        }
                    }
                    if (this.isFusedLocation) {
                        log("[\"gps\"," + this.mLastRealGpsLocation.getLatitude() + "," + this.mLastRealGpsLocation.getLongitude() + ",\"network\"," + this.mLastNetworkLocation.getLatitude() + "," + this.mLastNetworkLocation.getLongitude() + ",\"fused\"," + this.mLastFinalLocation.getLatitude() + "," + this.mLastFinalLocation.getLongitude() + ",\"" + new SimpleDateFormat("yyyy-MM-dd").format(new Date(System.currentTimeMillis())) + "\",\"" + new SimpleDateFormat("HH:mm:ss").format(new Date(System.currentTimeMillis())) + "\"]");
                        if (!this.dataSaved) {
                            Bundle bundle = new Bundle();
                            bundle.putString("pkg", this.fusedPackage);
                            bundle.putInt("wifiScore", this.mlastWifiScore);
                            bundle.putInt("gpsScore", this.mLastGpsScore);
                            bundle.putDouble("distance", distance);
                            bundle.putDouble("speed", this.mLastRealGpsLocation.getSpeed());
                            Message msg2 = this.mDCHandler.obtainMessage(107);
                            msg2.setData(bundle);
                            this.mDCHandler.sendMessage(msg2);
                            this.dataSaved = true;
                        }
                    }
                    this.mLastDistance = distance;
                } else {
                    this.isFusedLocation = false;
                }
                if (this.mLastFinalLocation.hasSpeed() && this.mLastFinalLocation.getSpeed() > 3.0f) {
                    int i4 = this.mHighSpeed + 1;
                    this.mHighSpeed = i4;
                    if (i4 >= 3) {
                        this.mCurrentSpeedState = 1;
                    }
                }
                if (this.mStrongSignal > 20 || this.mCurrentSpeedState == 1 || ((locationRecord = this.mLocationGpsRecord) != null && locationRecord.getSize() > 3 && this.mlastWifiScore < 30 && this.mLastGpsScore >= 50)) {
                    log("CMD_STOP_FUSED_GPS 2");
                    Message msg3 = obtainMessage(6);
                    msg3.obj = 7;
                    sendMessage(msg3);
                }
            }
            log("reportFusedGpsLocation fused gps:" + this.mLastFinalLocation.getLatitude() + "," + this.mLastFinalLocation.getLongitude());
            VivoCoreLocationManager vivoCoreLocationManager = this.mVivoCoreLocationManager;
            Location location2 = this.mLastFinalLocation;
            if (!isGps || this.isFusedLocation) {
                z = true;
            }
            vivoCoreLocationManager.reportFusedLocation(location2, z);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleScreenChange(boolean screenOn) {
        log("handleScreenChange ScreenOn:" + screenOn);
        if (screenOn) {
            removeMessages(6);
            return;
        }
        removeMessages(6);
        Message msg = obtainMessage(6);
        msg.obj = 8;
        sendMessageDelayed(msg, 5000L);
    }

    public void registerProcessObserver() {
        try {
            log("registerProcessObserver");
            ActivityManagerNative.getDefault().registerProcessObserver(this.mProcessObserver);
        } catch (Exception e) {
            log("error registerProcessObserver " + e);
        }
    }

    public String getAppName(int pid, int uid) {
        String packageName = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        try {
            packageName = this.mContext.getPackageManager().getNameForUid(uid);
        } catch (Exception e) {
            VLog.e(TAG, VLog.getStackTraceString(e));
        }
        VLog.d(TAG, "getAppNameFromUid " + packageName);
        return packageName;
    }

    /* loaded from: classes.dex */
    private class ScanBroadcastReceiver extends BroadcastReceiver {
        private ScanBroadcastReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            if (action.equals("android.net.wifi.SCAN_RESULTS")) {
                if (VivoGpsStateMachine.this.mWifiManager != null && VivoGpsStateMachine.this.mSupportFusedGps) {
                    synchronized (VivoGpsStateMachine.mGpsLock) {
                        List<ScanResult> mScanResultLists = VivoGpsStateMachine.this.mWifiManager.getScanResults();
                        if (mScanResultLists != null) {
                            VivoGpsStateMachine vivoGpsStateMachine = VivoGpsStateMachine.this;
                            vivoGpsStateMachine.log("scan results count: " + mScanResultLists.size());
                            VivoGpsStateMachine.this.mlastWifiScore = VivoGpsStateMachine.this.mVivoFusedGpsUtil.calculateWifiScore(mScanResultLists);
                            VivoGpsStateMachine.this.mlastL4WifiCount = VivoGpsStateMachine.this.mVivoFusedGpsUtil.getLastL4WifiCount();
                            VivoGpsStateMachine.this.mLastWifiScanResultTime = SystemClock.elapsedRealtime();
                        }
                    }
                }
            } else if (!action.equals("android.net.wifi.STATE_CHANGE")) {
                if (action.equals("android.intent.action.SCREEN_ON")) {
                    VivoGpsStateMachine.this.handleScreenChange(true);
                } else if (action.equals("android.intent.action.SCREEN_OFF")) {
                    VivoGpsStateMachine.this.handleScreenChange(false);
                }
            } else {
                NetworkInfo info = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                if (info.getState().equals(NetworkInfo.State.DISCONNECTED)) {
                    VivoGpsStateMachine.this.isWifiConnected = false;
                } else if (info.getState().equals(NetworkInfo.State.CONNECTED)) {
                    VivoGpsStateMachine.this.isWifiConnected = true;
                }
            }
        }
    }

    public void onFusedGpsConfigChanged(VivoCoreLocationManager.FusedGpsConfig config) {
        String[] strArr;
        if (config == null) {
            VLog.e(TAG, "onFusedGpsConfigChanged config=null; return;");
            return;
        }
        synchronized (mGpsLock) {
            try {
                this.mSupportFusedGps = config.support;
                mVivoGnssWhiteList.clear();
                for (String map : config.whiteList) {
                    mVivoGnssWhiteList.add(map);
                }
                this.mScoreParameterHashMap.put("GpsScoreThreshold", config.parameterHashmap.get("GpsScoreThreshold"));
                this.mScoreParameterHashMap.put("L4WifiCount", config.parameterHashmap.get("L4WifiCount"));
                this.mScoreParameterHashMap.put("L3WifiCount", config.parameterHashmap.get("L3WifiCount"));
                this.mScoreParameterHashMap.put("L4WifiScore", config.parameterHashmap.get("L4WifiScore"));
                this.mScoreParameterHashMap.put("L3WifiScore", config.parameterHashmap.get("L3WifiScore"));
                this.mVivoFusedGpsUtil.setScoreParameterHashMap(this.mScoreParameterHashMap);
                log("onFusedGpsConfigChanged support:" + config.support + " list:" + config.whiteList + " scoreParameter: " + this.mScoreParameterHashMap.toString());
            } catch (Exception e) {
                initParameter();
                log("onFusedGpsConfigChanged exception:" + e.getMessage());
            }
        }
    }

    protected void log(String msg) {
        if (DEBUG) {
            VLog.d(TAG, msg);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public static ArrayList<String> getGnssWhiteList() {
        synchronized (mGpsLock) {
            if (mVivoGnssWhiteList != null) {
                return mVivoGnssWhiteList;
            }
            return new ArrayList<>();
        }
    }

    public void setDebug(boolean debug) {
        DEBUG = debug;
        this.mVivoFusedGpsUtil.setDebug(debug);
    }
}