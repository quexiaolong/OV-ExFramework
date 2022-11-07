package com.android.server.pm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import com.vivo.face.common.data.Constants;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public final class VivoPKMSLocManager {
    private static final long LOCATION_MAX_VALID_TIME = 600000;
    private static final int MAX_FAIL_COUNT = 5;
    public static final int MAX_LOCATION_WAIT_TIME = 3000;
    private static final int MSG_NETWORK_LOCATION_ABANDON = 1003;
    private static final int MSG_NETWORK_LOCATION_FINISH = 1002;
    private static final int MSG_NETWORK_LOCATION_TIMEOUT = 1001;
    private static final int MSG_REQUEST_GEOCODER = 2000;
    private static final int MSG_REQUEST_NETWORK_LOCATION = 1000;
    public static final int NETWORK_LOCAITON_TIMEOUT = 10000;
    private static final long REQ_LOC_MAX_FAIL_TIME = 180000;
    public static final String TAG = "VivoPKMSLocManager";
    private Context mContext;
    TestHandler mHandler;
    private MyLocBroadcast mLocBc;
    private LocationManager mLocationManager;
    private LocationListener mNetWorkLocationListener;
    private WorkHandler mWorkHandler;
    private static boolean DEBUG = false;
    private static boolean DEBUG_FOR_ALL = false;
    private static volatile VivoPKMSLocManager sInstance = null;
    private boolean mIsLocationing = false;
    private boolean mIsLocationValid = false;
    private String mLatitudeAndLongitude = null;
    private long mLastLocationTime = 0;
    private boolean mIsRGCing = false;
    private boolean mRGCValid = false;
    private String mLastAddress = null;
    private Object mLocingLock = new Object();
    private Object mLocValidLock = new Object();
    private Object mNetworkTypeLock = new Object();
    private String mNetworkType = "1";
    private int mLocReqFailCount = 0;
    private long mLastReqLocTime = 0;
    private final ExecutorService mExecutorService = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue());

    public static VivoPKMSLocManager init(Context context, Looper looper, boolean debug, boolean debugAll) {
        if (sInstance == null) {
            synchronized (VivoPKMSLocManager.class) {
                if (sInstance == null) {
                    sInstance = new VivoPKMSLocManager(context, looper, debug, debugAll);
                }
            }
        }
        return sInstance;
    }

    private VivoPKMSLocManager(Context context, Looper looper, boolean debug, boolean debugAll) {
        this.mContext = null;
        this.mWorkHandler = null;
        this.mContext = context;
        this.mWorkHandler = new WorkHandler(looper);
        DEBUG = debug;
        DEBUG_FOR_ALL = debugAll;
        logI(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + debug + " " + debugAll);
    }

    public void systemReady() {
        this.mLocBc = new MyLocBroadcast();
        if (DEBUG_FOR_ALL) {
            new VivoTestBr();
        }
    }

    protected boolean isWifiNetwork() {
        synchronized (this.mNetworkTypeLock) {
            logD("nettype-> " + this.mNetworkType);
            if (this.mNetworkType.equals("1")) {
                return true;
            }
            return false;
        }
    }

    protected boolean isCanReqNetworkLoc() {
        synchronized (this.mLocValidLock) {
            logD("current fail count," + this.mLocReqFailCount);
            if (this.mLocReqFailCount < 5) {
                return true;
            }
            long elapsedRealtime = SystemClock.elapsedRealtime();
            boolean isLastReqLocFailOverTime = elapsedRealtime - this.mLastReqLocTime > REQ_LOC_MAX_FAIL_TIME;
            if (isLastReqLocFailOverTime) {
                this.mLocReqFailCount = 0;
                return true;
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void scheduleNetworkLocation() {
        if (!this.mWorkHandler.hasMessages(1000)) {
            Message reqLocationMsg = this.mWorkHandler.obtainMessage(1000);
            this.mWorkHandler.sendMessage(reqLocationMsg);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void requestNetworkLocation() {
        if (!isWifiNetwork()) {
            logD("not wifi, give up..");
        } else if (!isCanReqNetworkLoc()) {
        } else {
            synchronized (this.mLocingLock) {
                initLocationManager(this.mContext);
                if (this.mIsLocationing) {
                    return;
                }
                this.mIsLocationing = true;
                this.mExecutorService.execute(new Runnable() { // from class: com.android.server.pm.VivoPKMSLocManager.1
                    @Override // java.lang.Runnable
                    public void run() {
                        VivoPKMSLocManager.this.logD("req sending to lms...");
                        boolean isWifiConnected = VivoPKMSCommonUtils.isWifiConnected(VivoPKMSLocManager.DEBUG, VivoPKMSLocManager.this.mContext);
                        if (isWifiConnected) {
                            try {
                                VivoPKMSLocManager.this.mLastReqLocTime = SystemClock.elapsedRealtime();
                                VivoPKMSLocManager.this.mLocationManager.requestLocationUpdates("network", 1111L, 0.0f, VivoPKMSLocManager.this.mNetWorkLocationListener, VivoPKMSLocManager.this.mWorkHandler.getLooper());
                                VivoPKMSLocManager.this.logD("req send to lms done.");
                                Message msg = VivoPKMSLocManager.this.mWorkHandler.obtainMessage(1001);
                                VivoPKMSLocManager.this.mWorkHandler.sendMessageDelayed(msg, 10000L);
                                return;
                            } catch (Exception e) {
                                VivoPKMSLocManager vivoPKMSLocManager = VivoPKMSLocManager.this;
                                vivoPKMSLocManager.logI("request loc," + e.toString());
                                return;
                            }
                        }
                        VivoPKMSLocManager.this.logD("no connect wifi, not need req.");
                        synchronized (VivoPKMSLocManager.this.mLocingLock) {
                            VivoPKMSLocManager.this.mIsLocationing = false;
                        }
                        synchronized (VivoPKMSLocManager.this.mNetworkTypeLock) {
                            VivoPKMSLocManager.this.mNetworkType = "0";
                        }
                    }
                });
            }
        }
    }

    private void stopNetworkLocation(String reason) {
        try {
            synchronized (this.mLocingLock) {
                this.mIsLocationing = false;
            }
            this.mExecutorService.execute(new Runnable() { // from class: com.android.server.pm.VivoPKMSLocManager.2
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        if (VivoPKMSLocManager.this.mNetWorkLocationListener != null) {
                            VivoPKMSLocManager.this.mLocationManager.removeUpdates(VivoPKMSLocManager.this.mNetWorkLocationListener);
                        }
                    } catch (Exception e) {
                        VivoPKMSLocManager vivoPKMSLocManager = VivoPKMSLocManager.this;
                        vivoPKMSLocManager.logI("remove listener," + e.toString());
                    }
                }
            });
        } catch (Exception e) {
            logI("stop newtwork," + e.toString());
        }
    }

    private void initLocationManager(Context context) {
        if (this.mLocationManager == null) {
            this.mLocationManager = (LocationManager) context.getSystemService("location");
        }
        if (this.mNetWorkLocationListener == null) {
            this.mNetWorkLocationListener = new NetWorkLocationListener();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isLastLocationValid() {
        synchronized (this.mLocValidLock) {
            if (this.mIsLocationValid) {
                long elapsedRealtime = SystemClock.elapsedRealtime();
                boolean isLastLocOverTime = elapsedRealtime - this.mLastLocationTime > LOCATION_MAX_VALID_TIME;
                if (isLastLocOverTime) {
                    this.mIsLocationValid = false;
                    this.mRGCValid = false;
                    return false;
                }
                return true;
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public String getLatitudeAndLongitude() {
        synchronized (this.mLocValidLock) {
            if (this.mIsLocationValid) {
                return this.mLatitudeAndLongitude;
            }
            this.mLatitudeAndLongitude = null;
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public String getLocationRgcAddress() {
        synchronized (this.mLocValidLock) {
            if (this.mRGCValid) {
                return this.mLastAddress;
            }
            this.mLastAddress = null;
            return null;
        }
    }

    private void scheduleLocationGeocoder(Location location) {
        if (!this.mWorkHandler.hasMessages(2000)) {
            logD("start req rgc..");
            Message rgcMsg = this.mWorkHandler.obtainMessage(2000);
            rgcMsg.obj = location;
            this.mWorkHandler.sendMessage(rgcMsg);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void requestGeocoder(final Location location) {
        if (location == null) {
            resetRGCValue();
            resetRgcState();
            return;
        }
        synchronized (this.mLocingLock) {
            if (this.mIsRGCing) {
                logD("rgcing..., not start new!");
                return;
            }
            logI("start rgc ...");
            this.mIsRGCing = true;
            final Geocoder geocoder = new Geocoder(this.mContext);
            try {
                this.mExecutorService.execute(new Runnable() { // from class: com.android.server.pm.VivoPKMSLocManager.3
                    @Override // java.lang.Runnable
                    public void run() {
                        VivoPKMSLocManager.this.logD("begin rgc..");
                        try {
                            try {
                                List<Address> addList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                if (addList != null && addList.size() > 0) {
                                    Address add = addList.get(0);
                                    String countryName = add.getCountryName();
                                    String adminArea = add.getAdminArea();
                                    String locality = add.getLocality();
                                    String subLocality = add.getSubLocality();
                                    String thoroughfare = add.getThoroughfare();
                                    String subThoroughfare = add.getSubThoroughfare();
                                    StringBuilder sb = new StringBuilder();
                                    sb.append(countryName + "#");
                                    sb.append(adminArea + "#");
                                    sb.append(locality + "#");
                                    sb.append(subLocality + "#");
                                    sb.append(thoroughfare + "#");
                                    sb.append(subThoroughfare);
                                    synchronized (VivoPKMSLocManager.this.mLocValidLock) {
                                        VivoPKMSLocManager.this.mLastAddress = sb.toString();
                                        VivoPKMSLocManager.this.mRGCValid = true;
                                    }
                                }
                                VivoPKMSLocManager.this.logI("rgc end");
                            } catch (Exception e) {
                                VivoPKMSLocManager.this.resetRGCValue();
                                VivoPKMSLocManager vivoPKMSLocManager = VivoPKMSLocManager.this;
                                vivoPKMSLocManager.logI("gecoder1 catch ex, " + e.toString());
                            }
                        } finally {
                            VivoPKMSLocManager.this.resetRgcState();
                        }
                    }
                });
            } catch (Exception e) {
                resetRGCValue();
                resetRgcState();
                logI("gecoder2 catch ex, " + e.toString());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetRGCValue() {
        synchronized (this.mLocValidLock) {
            this.mRGCValid = false;
            this.mLastAddress = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetRgcState() {
        synchronized (this.mLocingLock) {
            this.mIsRGCing = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleNetworkLocTimeout() {
        synchronized (this.mLocValidLock) {
            this.mIsLocationValid = false;
            this.mLastLocationTime = 0L;
            this.mRGCValid = false;
            this.mLocReqFailCount++;
        }
        stopNetworkLocation("TIMEOUT");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleNetworkLocFinish(Location location) {
        try {
            if (location != null) {
                synchronized (this.mLocValidLock) {
                    this.mLatitudeAndLongitude = location.getLatitude() + "_" + location.getLongitude();
                    this.mIsLocationValid = true;
                    this.mLastLocationTime = SystemClock.elapsedRealtime();
                    this.mLocReqFailCount = 0;
                }
                scheduleLocationGeocoder(location);
            } else {
                synchronized (this.mLocValidLock) {
                    this.mIsLocationValid = false;
                    this.mLastLocationTime = 0L;
                }
            }
        } finally {
            stopNetworkLocation("FINISH");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleNetworkLocAbandon() {
        synchronized (this.mLocValidLock) {
            this.mIsLocationValid = false;
            this.mLocReqFailCount++;
        }
        stopNetworkLocation("ABANDON");
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class WorkHandler extends Handler {
        public WorkHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            VivoPKMSLocManager vivoPKMSLocManager = VivoPKMSLocManager.this;
            vivoPKMSLocManager.logI("what " + msg.what);
            int i = msg.what;
            if (i != 2000) {
                switch (i) {
                    case 1000:
                        try {
                            VivoPKMSLocManager.this.requestNetworkLocation();
                            return;
                        } catch (Exception e) {
                            VivoPKMSLocManager vivoPKMSLocManager2 = VivoPKMSLocManager.this;
                            vivoPKMSLocManager2.logI("start network request," + e.toString());
                            return;
                        }
                    case 1001:
                        VivoPKMSLocManager.this.handleNetworkLocTimeout();
                        return;
                    case 1002:
                        VivoPKMSLocManager.this.handleNetworkLocFinish((Location) msg.obj);
                        return;
                    case 1003:
                        VivoPKMSLocManager.this.handleNetworkLocAbandon();
                        return;
                    default:
                        super.handleMessage(msg);
                        return;
                }
            }
            VivoPKMSLocManager.this.requestGeocoder((Location) msg.obj);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class NetWorkLocationListener implements LocationListener {
        NetWorkLocationListener() {
        }

        @Override // android.location.LocationListener
        public void onLocationChanged(Location location) {
            if (location != null) {
                VivoPKMSLocManager.this.mWorkHandler.removeMessages(1001);
                if (!VivoPKMSLocManager.this.mWorkHandler.hasMessages(1002)) {
                    Message msg = new Message();
                    msg.what = 1002;
                    msg.obj = location;
                    VivoPKMSLocManager.this.mWorkHandler.sendMessage(msg);
                }
            }
        }

        @Override // android.location.LocationListener
        public void onProviderDisabled(String provider) {
            if ("network".equals(provider)) {
                VivoPKMSLocManager.this.mWorkHandler.removeMessages(1003);
                Message msg = new Message();
                msg.what = 1003;
                VivoPKMSLocManager.this.mWorkHandler.sendMessage(msg);
            }
        }

        @Override // android.location.LocationListener
        public void onProviderEnabled(String provider) {
            "network".equals(provider);
        }

        @Override // android.location.LocationListener
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }

    /* loaded from: classes.dex */
    class MyLocBroadcast extends BroadcastReceiver {
        public MyLocBroadcast() {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            VivoPKMSLocManager.this.mContext.registerReceiver(this, filter);
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            String action = intent.getAction();
            if ("android.net.conn.CONNECTIVITY_CHANGE".equals(action)) {
                synchronized (VivoPKMSLocManager.this.mNetworkTypeLock) {
                    NetworkInfo info = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                    if (info == null) {
                        VivoPKMSLocManager.this.mNetworkType = "0";
                    } else {
                        int netType = info.getType();
                        NetworkInfo.State state = info.getState();
                        if (!NetworkInfo.State.CONNECTED.equals(state)) {
                            VivoPKMSLocManager.this.mNetworkType = "0";
                        } else if (1 == netType) {
                            VivoPKMSLocManager.this.mNetworkType = "1";
                            VivoPKMSLocManager.this.mLocReqFailCount = 0;
                        } else {
                            VivoPKMSLocManager.this.mNetworkType = "2";
                        }
                    }
                }
            }
        }
    }

    void logI(String info) {
        VSlog.i(TAG, info);
    }

    void logD(String info) {
        if (DEBUG || VivoPKMSUtils.DEBUG) {
            VSlog.d(TAG, info);
        }
    }

    void logAll(String info) {
        if (DEBUG_FOR_ALL) {
            VSlog.d(TAG, info);
        }
    }

    /* loaded from: classes.dex */
    class VivoTestBr extends BroadcastReceiver {
        public VivoTestBr() {
            IntentFilter filter = new IntentFilter();
            filter.addAction("vivo.action.pkms.loc.test");
            HandlerThread hThread = new HandlerThread("pkmsLocHThread");
            hThread.start();
            VivoPKMSLocManager.this.mHandler = new TestHandler(hThread.getLooper());
            VivoPKMSLocManager.this.mContext.registerReceiver(this, filter, "android.permission.INSTALL_PACKAGES", VivoPKMSLocManager.this.mHandler);
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            intent.getAction();
            VivoPKMSLocManager vivoPKMSLocManager = VivoPKMSLocManager.this;
            vivoPKMSLocManager.logI("intent:" + intent);
            boolean isValid = VivoPKMSLocManager.this.isLastLocationValid();
            VivoPKMSLocManager vivoPKMSLocManager2 = VivoPKMSLocManager.this;
            vivoPKMSLocManager2.logI("isValid " + isValid);
            VivoPKMSLocManager.this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.pm.VivoPKMSLocManager.VivoTestBr.1
                @Override // java.lang.Runnable
                public void run() {
                    VivoPKMSLocManager.this.testLoc();
                }
            }, 0L);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void testLoc() {
        scheduleNetworkLocation();
    }

    /* loaded from: classes.dex */
    static class TestHandler extends Handler {
        public TestHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }
}