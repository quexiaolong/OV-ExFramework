package com.android.server.location;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.os.WorkSource;
import com.android.internal.location.ProviderRequest;
import com.android.server.location.VivoCoreLocationManager;
import com.android.server.wm.VCD_FF_1;
import com.vivo.common.VivoCollectData;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import com.vivo.vcodetransbase.EventTransfer;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class VivoFakeGpsProvider {
    private static final int ARG1_REMOVE_UPDATE = 2;
    private static final int ARG1_REQUEST_UPDATE = 1;
    private static final int FAKE_GPS_REPORT_INTERVAL = 200;
    private static final int LAST_LOCATION_VALID_TIME_LIMIT = 10000;
    private static final String LOCAL_SCAN_PKG_NAME = "com.vivo.fakegps";
    private static final int MAX_FAKE_GPS_REPORT = 10;
    private static final int MSG_RECEIVE_REAL_GPS = 4;
    private static final int MSG_REPORT_FAKE_GPS = 3;
    private static final int MSG_SCAN_WIFI = 6;
    private static final int MSG_SET_REQUEST = 5;
    private static final int MSG_START_GPS = 1;
    private static final int MSG_STOP_GPS = 2;
    private static final int WIFI_SCAN_INTERVAL = 250;
    private MyHandler mHandler;
    private LocationManager mLocationManager;
    private Object mVCD;
    private VivoCoreLocationManager mVivoCoreLocationManager;
    private WifiManager mWifiManager;
    private static final String PROP_FASTSCAN_TIME = "persist.vivo.fakegps.scantime";
    private static final int WIFI_SCAN_TIMES = SystemProperties.getInt(PROP_FASTSCAN_TIME, 1);
    private static final String TAG = "VivoFgProvider";
    private static boolean DEBUG = VLog.isLoggable(TAG, 3);
    private int mReportedFakeGpsCount = 0;
    private Location mLastFakeGpsLocation = null;
    private Location mLastRealGpsLocation = null;
    private boolean mNlpRegistered = false;
    private long mNlpFirstReportTimestamp = 0;
    private long mNlpRegisterTimestamp = 0;
    private long mNlpLastReportTimestamp = 0;
    private Location mLastPassiveLocation = null;
    private long mLastPassiveTimestamp = 0;
    private HashSet<String> mRequestGpsSet = new HashSet<>();
    private boolean mHasWhiteMapList = false;
    private boolean mSupportFakeGps = true;
    private ArrayList<String> mWhiteMapList = new ArrayList<>();
    private int mWifiScanedTimes = 0;
    private FgJsonWrapper mFgJsonWrapper = null;
    private LocationListener mNlpLocationListener = new LocationListener() { // from class: com.android.server.location.VivoFakeGpsProvider.3
        @Override // android.location.LocationListener
        public void onLocationChanged(Location location) {
            if (VivoFakeGpsProvider.this.mReportedFakeGpsCount == 0) {
                VivoFakeGpsProvider.this.mNlpFirstReportTimestamp = System.currentTimeMillis();
                if (VivoFakeGpsProvider.this.mFgJsonWrapper != null) {
                    VivoFakeGpsProvider.this.mFgJsonWrapper.setNlpLocation(location);
                    VivoFakeGpsProvider.this.mFgJsonWrapper.nlpRep = VivoFakeGpsProvider.this.mNlpFirstReportTimestamp;
                }
                VivoFakeGpsProvider vivoFakeGpsProvider = VivoFakeGpsProvider.this;
                vivoFakeGpsProvider.log("onLocationChanged mNlp register=" + VivoFakeGpsProvider.this.mNlpRegisterTimestamp + " now=" + VivoFakeGpsProvider.this.mNlpFirstReportTimestamp + " diff=" + (VivoFakeGpsProvider.this.mNlpFirstReportTimestamp - VivoFakeGpsProvider.this.mNlpRegisterTimestamp));
                VivoFakeGpsProvider vivoFakeGpsProvider2 = VivoFakeGpsProvider.this;
                StringBuilder sb = new StringBuilder();
                sb.append("onLocationChanged firstNlp:");
                sb.append(location);
                vivoFakeGpsProvider2.log(sb.toString());
            }
            Bundle bd = location.getExtras();
            bd.putCharSequence("src", "fk");
            location.setExtras(bd);
            VivoFakeGpsProvider.this.reportFakeGpsLocation(location);
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
    private LocationListener mPassiveLocationListener = new LocationListener() { // from class: com.android.server.location.VivoFakeGpsProvider.4
        @Override // android.location.LocationListener
        public void onLocationChanged(Location location) {
            VivoFakeGpsProvider.this.mLastPassiveTimestamp = System.currentTimeMillis();
            VivoFakeGpsProvider.this.mLastPassiveLocation = new Location(location);
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

    public VivoFakeGpsProvider(Context context, VivoCoreLocationManager manager, Looper looper) {
        this.mVivoCoreLocationManager = null;
        this.mHandler = null;
        this.mLocationManager = null;
        this.mWifiManager = null;
        this.mVCD = null;
        this.mVivoCoreLocationManager = manager;
        this.mHandler = new MyHandler(looper);
        this.mLocationManager = (LocationManager) context.getSystemService("location");
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        this.mHandler.post(new Runnable() { // from class: com.android.server.location.VivoFakeGpsProvider.1
            @Override // java.lang.Runnable
            public void run() {
                VivoFakeGpsProvider.this.mWhiteMapList.add("com.baidu.map.location");
                VivoFakeGpsProvider.this.mWhiteMapList.add("com.baidu.BaiduMap");
                VivoFakeGpsProvider.this.mWhiteMapList.add("com.autonavi.minimap");
                VivoFakeGpsProvider.this.mWhiteMapList.add("com.example.administrator.myapplication");
            }
        });
        this.mVCD = getVCD(context);
    }

    private Object getVCD(Context context) {
        if (this.mVCD == null) {
            this.mVCD = new VivoCollectData(context);
        }
        return this.mVCD;
    }

    public void onFakeGpsConfigChanged(final VivoCoreLocationManager.FakeGpsConfig config) {
        if (config == null) {
            VLog.e(TAG, "onFakeGpsConfigChanged config=null; return;");
            return;
        }
        this.mSupportFakeGps = config.support;
        this.mHandler.post(new Runnable() { // from class: com.android.server.location.VivoFakeGpsProvider.2
            @Override // java.lang.Runnable
            public void run() {
                String[] strArr;
                VivoFakeGpsProvider.this.mWhiteMapList.clear();
                for (String map : config.whiteList) {
                    VivoFakeGpsProvider.this.mWhiteMapList.add(map);
                }
                VivoFakeGpsProvider.this.log("onFakeGpsConfigChanged, list:" + VivoFakeGpsProvider.this.mWhiteMapList);
            }
        });
        log("onFakeGpsConfigChanged support:" + config.support + " list:" + config.whiteList);
    }

    public void setDebug(boolean debug) {
        DEBUG = debug;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isWhiteMap(String pkg) {
        if (this.mWhiteMapList.size() < 1) {
            log("isWhiteMap empty, all app support.");
            return true;
        }
        Iterator<String> it = this.mWhiteMapList.iterator();
        while (it.hasNext()) {
            String wm = it.next();
            if (wm.equals(pkg)) {
                return true;
            }
        }
        log("isWhiteMap can't find " + pkg);
        return false;
    }

    public void onSetRequest(ProviderRequest request, WorkSource source) {
        if (request != null && source != null) {
            if (source.size() >= 1) {
                String[] pkgList = new String[source.size()];
                for (int i = 0; i < source.size(); i++) {
                    pkgList[i] = source.getName(i);
                }
                this.mHandler.obtainMessage(5, request.reportLocation ? 1 : 2, 0, pkgList).sendToTarget();
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

    /* JADX INFO: Access modifiers changed from: private */
    public void scanWifi() {
        if (this.mWifiManager == null) {
            log("scanWifi mWifiManager=NULL, return;");
            return;
        }
        log("scanWifi started");
        try {
            Class<?> ownerClass = this.mWifiManager.getClass();
            Method localMethod = ownerClass.getDeclaredMethod("startFastScan", String.class);
            localMethod.setAccessible(true);
            localMethod.invoke(this.mWifiManager, LOCAL_SCAN_PKG_NAME);
        } catch (Exception e) {
            VLog.e(TAG, VLog.getStackTraceString(e));
        }
        this.mWifiScanedTimes++;
        log("scanWifi finished");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reportFakeGpsLocation(Location location) {
        String str;
        log("reportFakeGpsLocation provider : " + location.getProvider());
        if (!location.getProvider().equals("gps")) {
            log("reportFakeGpsLocation change provider from " + location.getProvider() + " to gps");
            location.setProvider("gps");
        }
        this.mLastFakeGpsLocation = new Location(location);
        this.mVivoCoreLocationManager.reportFusedLocation(location);
        this.mReportedFakeGpsCount++;
        this.mNlpLastReportTimestamp = System.currentTimeMillis();
        if (this.mReportedFakeGpsCount < 10) {
            this.mHandler.sendEmptyMessageDelayed(3, 200L);
        }
        log("reportFakeGpsLocation fake gps:" + this.mLastFakeGpsLocation.getLatitude() + "," + this.mLastFakeGpsLocation.getLongitude());
        StringBuilder sb = new StringBuilder();
        sb.append("reportFakeGpsLocation real gps:");
        if (this.mLastRealGpsLocation == null) {
            str = "NULL";
        } else {
            str = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + this.mLastRealGpsLocation.getLatitude() + "," + this.mLastRealGpsLocation.getLongitude();
        }
        sb.append(str);
        log(sb.toString());
        StringBuilder sb2 = new StringBuilder();
        sb2.append("reportFakeGpsLocation fake to real distance=");
        Location location2 = this.mLastRealGpsLocation;
        sb2.append(location2 == null ? "N/A" : Float.valueOf(this.mLastFakeGpsLocation.distanceTo(location2)));
        log(sb2.toString());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerNlpListner() {
        this.mReportedFakeGpsCount = 0;
        this.mNlpFirstReportTimestamp = 0L;
        this.mNlpRegisterTimestamp = System.currentTimeMillis();
        this.mLocationManager.requestLocationUpdates("network", 1000L, 0.0f, this.mNlpLocationListener, this.mHandler.getLooper());
        this.mLocationManager.removeUpdates(this.mPassiveLocationListener);
        this.mNlpRegistered = true;
        log("registerNlpListner");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unregisterNlpListner() {
        this.mReportedFakeGpsCount = 0;
        this.mNlpRegisterTimestamp = 0L;
        this.mLocationManager.removeUpdates(this.mNlpLocationListener);
        this.mLocationManager.requestLocationUpdates("passive", 1000L, 0.0f, this.mPassiveLocationListener, this.mHandler.getLooper());
        this.mHandler.removeMessages(3);
        this.mNlpRegistered = false;
        log("unregisterNlpListner");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void writeData() {
        Object obj = this.mVCD;
        if (obj != null) {
            boolean control = ((VivoCollectData) obj).getControlInfo("203");
            if (this.mNlpRegistered && this.mFgJsonWrapper != null && control) {
                HashMap<String, String> params = new HashMap<>(1);
                this.mFgJsonWrapper.stopTime = System.currentTimeMillis();
                String content = this.mFgJsonWrapper.toJsonString();
                params.put("info", content);
                ((VivoCollectData) this.mVCD).writeData("203", "2033", this.mFgJsonWrapper.startTime, this.mFgJsonWrapper.stopTime, this.mFgJsonWrapper.stopTime - this.mFgJsonWrapper.startTime, 1, params);
                log("writeData content:" + content);
                HashMap<String, String> params1 = this.mFgJsonWrapper.getMapResult();
                EventTransfer.getInstance().singleEvent("F500", "F500|10004", System.currentTimeMillis(), 0L, params1);
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("writeData mNlpRegistered=");
                sb.append(this.mNlpRegistered);
                sb.append(" mFgJsonWrapper=");
                sb.append(this.mFgJsonWrapper == null ? "Null" : "NotNull");
                sb.append(" control=");
                sb.append(control);
                log(sb.toString());
            }
        }
        this.mFgJsonWrapper = null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            String str;
            int i = 0;
            switch (msg.what) {
                case 1:
                    if (!VivoFakeGpsProvider.this.mHasWhiteMapList) {
                        VivoFakeGpsProvider.this.log("MSG_START_GPS NO white list map, return;");
                        VivoFakeGpsProvider.this.mFgJsonWrapper = null;
                        return;
                    }
                    VivoFakeGpsProvider vivoFakeGpsProvider = VivoFakeGpsProvider.this;
                    vivoFakeGpsProvider.mFgJsonWrapper = new FgJsonWrapper(vivoFakeGpsProvider.mRequestGpsSet, System.currentTimeMillis());
                    VivoFakeGpsProvider.this.log("MSG_START_GPS");
                    if (VivoFakeGpsProvider.this.mLastPassiveLocation != null && System.currentTimeMillis() - VivoFakeGpsProvider.this.mLastPassiveTimestamp < 10000) {
                        VivoFakeGpsProvider.this.log("MSG_START_GPS using last passive location.");
                        VivoFakeGpsProvider.this.mLastPassiveLocation.setTime(System.currentTimeMillis());
                        VivoFakeGpsProvider vivoFakeGpsProvider2 = VivoFakeGpsProvider.this;
                        vivoFakeGpsProvider2.reportFakeGpsLocation(vivoFakeGpsProvider2.mLastPassiveLocation);
                    }
                    VivoFakeGpsProvider.this.scanWifi();
                    VivoFakeGpsProvider.this.registerNlpListner();
                    VivoFakeGpsProvider.this.mFgJsonWrapper.nlpReg = VivoFakeGpsProvider.this.mNlpRegisterTimestamp;
                    Message m = obtainMessage(6);
                    sendMessageDelayed(m, 250L);
                    return;
                case 2:
                    VivoFakeGpsProvider.this.log("MSG_STOP_GPS");
                    if (VivoFakeGpsProvider.this.mNlpRegistered) {
                        VivoFakeGpsProvider.this.writeData();
                        VivoFakeGpsProvider.this.unregisterNlpListner();
                    } else {
                        VivoFakeGpsProvider.this.log("MSG_STOP_GPS mNlpRegistered=false");
                    }
                    removeMessages(6);
                    VivoFakeGpsProvider.this.mWifiScanedTimes = 0;
                    VivoFakeGpsProvider.this.mRequestGpsSet.clear();
                    return;
                case 3:
                    VivoFakeGpsProvider.this.log("MSG_REPORT_FAKE_GPS");
                    if (VivoFakeGpsProvider.this.mNlpRegistered) {
                        VivoFakeGpsProvider.this.mLastFakeGpsLocation.setTime(System.currentTimeMillis());
                        VivoFakeGpsProvider vivoFakeGpsProvider3 = VivoFakeGpsProvider.this;
                        vivoFakeGpsProvider3.reportFakeGpsLocation(vivoFakeGpsProvider3.mLastFakeGpsLocation);
                        return;
                    }
                    return;
                case 4:
                    VivoFakeGpsProvider.this.mLastRealGpsLocation = new Location((Location) msg.obj);
                    VivoFakeGpsProvider.this.mVivoCoreLocationManager.reportFusedLocation(VivoFakeGpsProvider.this.mLastRealGpsLocation, false);
                    if (!VivoFakeGpsProvider.this.mNlpRegistered) {
                        VivoFakeGpsProvider.this.log("onReportGpsLocation NLP not registtered, fake finished, return.");
                        return;
                    }
                    if (VivoFakeGpsProvider.this.mFgJsonWrapper != null) {
                        VivoFakeGpsProvider.this.mFgJsonWrapper.setGpsLocation(VivoFakeGpsProvider.this.mLastRealGpsLocation);
                    }
                    VivoFakeGpsProvider.this.writeData();
                    VivoFakeGpsProvider.this.unregisterNlpListner();
                    VivoFakeGpsProvider.this.log("MSG_RECEIVE_REAL_GPS real gps:" + VivoFakeGpsProvider.this.mLastRealGpsLocation.getLatitude() + "," + VivoFakeGpsProvider.this.mLastRealGpsLocation.getLongitude());
                    VivoFakeGpsProvider vivoFakeGpsProvider4 = VivoFakeGpsProvider.this;
                    StringBuilder sb = new StringBuilder();
                    sb.append("MSG_RECEIVE_REAL_GPS fake gps:");
                    if (VivoFakeGpsProvider.this.mLastFakeGpsLocation == null) {
                        str = "Null";
                    } else {
                        str = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + VivoFakeGpsProvider.this.mLastFakeGpsLocation.getLatitude() + "," + VivoFakeGpsProvider.this.mLastFakeGpsLocation.getLongitude();
                    }
                    sb.append(str);
                    vivoFakeGpsProvider4.log(sb.toString());
                    VivoFakeGpsProvider vivoFakeGpsProvider5 = VivoFakeGpsProvider.this;
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("MSG_RECEIVE_REAL_GPS real to fake distance=");
                    sb2.append(VivoFakeGpsProvider.this.mLastFakeGpsLocation == null ? "N/A" : Float.valueOf(VivoFakeGpsProvider.this.mLastRealGpsLocation.distanceTo(VivoFakeGpsProvider.this.mLastFakeGpsLocation)));
                    vivoFakeGpsProvider5.log(sb2.toString());
                    return;
                case 5:
                    String[] li = (String[]) msg.obj;
                    if (msg.arg1 == 1) {
                        int length = li.length;
                        while (i < length) {
                            String s = li[i];
                            VivoFakeGpsProvider.this.mRequestGpsSet.add(s);
                            i++;
                        }
                        VivoFakeGpsProvider.this.log("MSG_SET_REQUEST add:" + li.toString());
                    } else {
                        int length2 = li.length;
                        while (i < length2) {
                            String s2 = li[i];
                            VivoFakeGpsProvider.this.mRequestGpsSet.remove(s2);
                            i++;
                        }
                        VivoFakeGpsProvider.this.log("MSG_SET_REQUEST remove:" + li.toString());
                    }
                    Iterator<String> reIt = VivoFakeGpsProvider.this.mRequestGpsSet.iterator();
                    boolean has = false;
                    while (true) {
                        if (reIt.hasNext()) {
                            if (VivoFakeGpsProvider.this.isWhiteMap(reIt.next())) {
                                has = true;
                            }
                        }
                    }
                    VivoFakeGpsProvider.this.mHasWhiteMapList = has;
                    VivoFakeGpsProvider.this.log("MSG_SET_REQUEST has=" + VivoFakeGpsProvider.this.mHasWhiteMapList + " requestSet=" + VivoFakeGpsProvider.this.mRequestGpsSet.toString());
                    return;
                case 6:
                    if (VivoFakeGpsProvider.this.mWifiScanedTimes < VivoFakeGpsProvider.WIFI_SCAN_TIMES) {
                        VivoFakeGpsProvider.this.scanWifi();
                        Message msgScan = VivoFakeGpsProvider.this.mHandler.obtainMessage(6);
                        VivoFakeGpsProvider.this.mHandler.sendMessageDelayed(msgScan, 250L);
                        return;
                    }
                    VivoFakeGpsProvider.this.log("handle message WIFI_SCAN_TIMES=" + VivoFakeGpsProvider.WIFI_SCAN_TIMES + " mWifiScanedTimes=" + VivoFakeGpsProvider.this.mWifiScanedTimes);
                    return;
                default:
                    return;
            }
        }
    }

    public void onStartNavigating() {
        if (!this.mSupportFakeGps) {
            return;
        }
        log("onStartNavigating");
        this.mHandler.sendEmptyMessage(1);
    }

    public void onStopNavigating() {
        if (!this.mSupportFakeGps) {
            return;
        }
        log("onStopNavigating");
        this.mHandler.sendEmptyMessage(2);
    }

    public void onReportGpsLocation(Location location) {
        if (!this.mSupportFakeGps) {
            return;
        }
        long time = System.currentTimeMillis();
        log("onReportGpsLocation TIME_DIFF: net:" + this.mNlpFirstReportTimestamp + " gps:" + time + " passive:" + this.mLastPassiveTimestamp + " diff(nlp-gps):" + (this.mNlpFirstReportTimestamp - time));
        Location loc = new Location(location);
        this.mHandler.obtainMessage(4, loc).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void log(String msg) {
        if (DEBUG) {
            VLog.d(TAG, msg);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class FgJsonWrapper {
        private static final String KEY_ACCURACY = "acc";
        private static final String KEY_DIFF = "diff";
        private static final String KEY_DISTANCE = "distance";
        private static final String KEY_GPS = "gps";
        private static final String KEY_NLP = "nlp";
        private static final String KEY_NLP_REGISTER_TIME = "nlpReg";
        private static final String KEY_NLP_REPORT_TIME = "nlpRep";
        private static final String KEY_PKG = "pkg";
        private static final String KEY_START = "start";
        private static final String KEY_STOP = "stop";
        private static final String KEY_TIME = "time";
        private static final String KEY_TIME_DIFF = "timeDiff";
        public String[] pkgList;
        public long startTime;
        public long stopTime;
        public long nlpReg = -1;
        public long nlpRep = -1;
        public Location nlpLocation = null;
        public Location gpsLocation = null;

        public FgJsonWrapper(HashSet<String> pkgSet, long startTime) {
            this.pkgList = null;
            if (pkgSet != null) {
                this.pkgList = new String[pkgSet.size()];
                Iterator<String> it = pkgSet.iterator();
                while (it.hasNext()) {
                    this.pkgList[0] = it.next();
                }
            } else {
                this.pkgList = new String[0];
            }
            this.startTime = startTime;
        }

        public void setNlpLocation(Location location) {
            this.nlpLocation = new Location(location);
        }

        public void setGpsLocation(Location location) {
            this.gpsLocation = new Location(location);
        }

        public HashMap<String, String> getMapResult() {
            HashMap<String, String> mapResult = new HashMap<>();
            mapResult.put(VCD_FF_1.UUID_STR, UUID.randomUUID().toString());
            mapResult.put(KEY_START, String.valueOf(this.startTime));
            String[] strArr = this.pkgList;
            if (strArr != null) {
                String pkglist = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                for (String pkg : strArr) {
                    pkglist = pkglist + pkg + ",";
                }
                mapResult.put(KEY_PKG, pkglist);
            } else {
                mapResult.put(KEY_PKG, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
            }
            mapResult.put(KEY_NLP_REGISTER_TIME, String.valueOf(this.nlpReg));
            mapResult.put(KEY_NLP_REPORT_TIME, String.valueOf(this.nlpRep));
            if (this.nlpLocation != null) {
                String nlpLoc = this.nlpLocation.getAccuracy() + "," + this.nlpLocation.getTime();
                mapResult.put(KEY_NLP, nlpLoc);
            } else {
                mapResult.put(KEY_NLP, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
            }
            if (this.gpsLocation != null) {
                String gpsLoc = this.gpsLocation.getAccuracy() + "," + this.gpsLocation.getTime();
                mapResult.put(KEY_GPS, gpsLoc);
            } else {
                mapResult.put(KEY_GPS, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
            }
            Location location = this.nlpLocation;
            if (location != null && this.gpsLocation != null) {
                long timeDiff = location.getTime() - this.gpsLocation.getTime();
                String diffInfo = timeDiff + "," + this.nlpLocation.distanceTo(this.gpsLocation);
                mapResult.put(KEY_DIFF, diffInfo);
            } else {
                mapResult.put(KEY_DIFF, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
            }
            mapResult.put(KEY_STOP, String.valueOf(this.stopTime));
            return mapResult;
        }

        public String toJsonString() {
            String[] strArr;
            JSONObject obj = new JSONObject();
            try {
                obj.put(KEY_START, this.startTime);
                if (this.pkgList != null) {
                    JSONArray jPkgs = new JSONArray();
                    for (String pkg : this.pkgList) {
                        jPkgs.put(pkg);
                    }
                    obj.put(KEY_PKG, jPkgs);
                } else {
                    obj.put(KEY_PKG, (Object) null);
                }
                obj.put(KEY_NLP_REGISTER_TIME, this.nlpReg);
                obj.put(KEY_NLP_REPORT_TIME, this.nlpRep);
                if (this.nlpLocation != null) {
                    JSONObject nlpObj = new JSONObject();
                    nlpObj.put(KEY_ACCURACY, this.nlpLocation.getAccuracy());
                    nlpObj.put(KEY_TIME, this.nlpLocation.getTime());
                    obj.put(KEY_NLP, nlpObj);
                } else {
                    obj.put(KEY_NLP, (Object) null);
                }
                if (this.gpsLocation != null) {
                    JSONObject gpsObj = new JSONObject();
                    gpsObj.put(KEY_ACCURACY, this.gpsLocation.getAccuracy());
                    gpsObj.put(KEY_TIME, this.gpsLocation.getTime());
                    obj.put(KEY_GPS, gpsObj);
                } else {
                    obj.put(KEY_GPS, (Object) null);
                }
                if (this.nlpLocation != null && this.gpsLocation != null) {
                    JSONObject diffObj = new JSONObject();
                    long timeDiff = this.nlpLocation.getTime() - this.gpsLocation.getTime();
                    float distance = this.nlpLocation.distanceTo(this.gpsLocation);
                    diffObj.put(KEY_TIME_DIFF, timeDiff);
                    diffObj.put(KEY_DISTANCE, distance);
                    obj.put(KEY_DIFF, diffObj);
                } else {
                    obj.put(KEY_DIFF, (Object) null);
                }
                obj.put(KEY_STOP, this.stopTime);
                return obj.toString();
            } catch (JSONException je) {
                VLog.d(VivoFakeGpsProvider.TAG, "toJsonString got exception:" + je.getMessage());
                return "{}";
            }
        }
    }
}