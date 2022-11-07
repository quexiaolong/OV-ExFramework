package com.android.server.location;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import com.android.server.wm.VCD_FF_1;
import com.vivo.common.VivoCollectData;
import com.vivo.common.utils.VLog;
import com.vivo.vcodetransbase.EventTransfer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class VivoNetworkLocationData {
    private static final int MAX_RESULT = 10;
    private static final long MAX_TIME_LIMIT = 500;
    private static final int MSG_GPS_LOCATION_CHANGED = 2;
    private static final int MSG_NETWORK_LOCATION_CHANGED = 3;
    private static final int MSG_STARTNAVIGATING = 0;
    private static final int MSG_WRITE_DATA = 1;
    private static final String TAG = "VivoNetworkLocationData";
    private HandlerThread mCollectThread;
    private Context mContext;
    private MyHandler mDCHandler;
    private Location mGpsLocation;
    private LocationManager mLocationManager;
    private Location mNetworkLocation;
    private Object mVCD;
    public static boolean DEBUG = false;
    private static boolean hasData = false;
    private static boolean navigating = false;
    private int count = 0;
    private long gpsUpdateTime = 0;
    private long nlpUpdateTime = 0;
    private NlpJsonWrapper mNlpJsonWrapper = null;
    private SimpleDateFormat mSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private LocationListener mPassiveLocationListener = new LocationListener() { // from class: com.android.server.location.VivoNetworkLocationData.1
        @Override // android.location.LocationListener
        public void onLocationChanged(Location location) {
            if (VivoNetworkLocationData.this.count >= 10) {
                VivoNetworkLocationData.this.mLocationManager.removeUpdates(VivoNetworkLocationData.this.mPassiveLocationListener);
            } else if (location.getProvider().equals("gps")) {
                long now = System.currentTimeMillis();
                Bundle bundle = new Bundle();
                bundle.putParcelable("gpsloc", location);
                bundle.putLong("gpstime", now);
                Message msg = VivoNetworkLocationData.this.mDCHandler.obtainMessage(2);
                msg.setData(bundle);
                VivoNetworkLocationData.this.mDCHandler.sendMessage(msg);
            } else if (location.getProvider().equals("network")) {
                long now2 = System.currentTimeMillis();
                Bundle bundle2 = new Bundle();
                bundle2.putParcelable("nlploc", location);
                bundle2.putLong("nlptime", now2);
                Message msg2 = VivoNetworkLocationData.this.mDCHandler.obtainMessage(3);
                msg2.setData(bundle2);
                VivoNetworkLocationData.this.mDCHandler.sendMessage(msg2);
            }
        }

        @Override // android.location.LocationListener
        public void onStatusChanged(String provider, int status, Bundle extras) {
            if (VivoNetworkLocationData.DEBUG) {
                VLog.d(VivoNetworkLocationData.TAG, "onStatusChanged : provider" + provider + "status:" + status + "extras" + extras.toString());
            }
        }

        @Override // android.location.LocationListener
        public void onProviderEnabled(String provider) {
            if (VivoNetworkLocationData.DEBUG) {
                VLog.d(VivoNetworkLocationData.TAG, "onProviderEnabled : provider" + provider);
            }
        }

        @Override // android.location.LocationListener
        public void onProviderDisabled(String provider) {
            if (VivoNetworkLocationData.DEBUG) {
                VLog.d(VivoNetworkLocationData.TAG, "onProviderDisabled : provider" + provider);
            }
        }
    };

    static /* synthetic */ int access$108(VivoNetworkLocationData x0) {
        int i = x0.count;
        x0.count = i + 1;
        return i;
    }

    public VivoNetworkLocationData(Context context) {
        this.mContext = null;
        this.mVCD = null;
        this.mCollectThread = null;
        this.mDCHandler = null;
        this.mContext = context;
        this.mVCD = getVCD(context);
        this.mLocationManager = (LocationManager) context.getSystemService("location");
        HandlerThread handlerThread = new HandlerThread("nlp_data");
        this.mCollectThread = handlerThread;
        handlerThread.start();
        this.mDCHandler = new MyHandler(this.mCollectThread.getLooper());
    }

    private Object getVCD(Context context) {
        if (this.mVCD == null) {
            this.mVCD = new VivoCollectData(context);
        }
        return this.mVCD;
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
            int i = msg.what;
            if (i == 0) {
                boolean unused = VivoNetworkLocationData.hasData = false;
                VivoNetworkLocationData.this.count = 0;
                VivoNetworkLocationData.this.gpsUpdateTime = 0L;
                VivoNetworkLocationData.this.nlpUpdateTime = 0L;
                VivoNetworkLocationData vivoNetworkLocationData = VivoNetworkLocationData.this;
                vivoNetworkLocationData.mNlpJsonWrapper = new NlpJsonWrapper(System.currentTimeMillis());
                boolean unused2 = VivoNetworkLocationData.navigating = true;
            } else if (i == 1) {
                VivoNetworkLocationData.this.writeData();
                boolean unused3 = VivoNetworkLocationData.navigating = false;
            } else if (i != 2) {
                if (i != 3 || !VivoNetworkLocationData.navigating) {
                    return;
                }
                VivoNetworkLocationData.this.nlpUpdateTime = msg.getData().getLong("nlptime");
                VivoNetworkLocationData.this.mNetworkLocation = (Location) msg.getData().getParcelable("nlploc");
                if (VivoNetworkLocationData.this.gpsUpdateTime != 0 && VivoNetworkLocationData.this.nlpUpdateTime - VivoNetworkLocationData.this.gpsUpdateTime < VivoNetworkLocationData.MAX_TIME_LIMIT && VivoNetworkLocationData.this.nlpUpdateTime - VivoNetworkLocationData.this.gpsUpdateTime >= 0 && VivoNetworkLocationData.this.mNetworkLocation.distanceTo(VivoNetworkLocationData.this.mGpsLocation) > 0.01d && VivoNetworkLocationData.this.mNlpJsonWrapper != null) {
                    VivoNetworkLocationData.this.mNlpJsonWrapper.setLocation(VivoNetworkLocationData.this.mNetworkLocation, VivoNetworkLocationData.this.nlpUpdateTime, VivoNetworkLocationData.this.mGpsLocation, VivoNetworkLocationData.this.gpsUpdateTime);
                    VivoNetworkLocationData.this.gpsUpdateTime = 0L;
                    VivoNetworkLocationData.access$108(VivoNetworkLocationData.this);
                    boolean unused4 = VivoNetworkLocationData.hasData = true;
                }
            } else if (!VivoNetworkLocationData.navigating) {
            } else {
                VivoNetworkLocationData.this.gpsUpdateTime = msg.getData().getLong("gpstime");
                VivoNetworkLocationData.this.mGpsLocation = (Location) msg.getData().getParcelable("gpsloc");
                if (VivoNetworkLocationData.this.nlpUpdateTime != 0 && VivoNetworkLocationData.this.gpsUpdateTime - VivoNetworkLocationData.this.nlpUpdateTime < VivoNetworkLocationData.MAX_TIME_LIMIT && VivoNetworkLocationData.this.gpsUpdateTime - VivoNetworkLocationData.this.nlpUpdateTime >= 0 && VivoNetworkLocationData.this.mGpsLocation.distanceTo(VivoNetworkLocationData.this.mNetworkLocation) > 0.01d && VivoNetworkLocationData.this.mNlpJsonWrapper != null) {
                    VivoNetworkLocationData.this.mNlpJsonWrapper.setLocation(VivoNetworkLocationData.this.mNetworkLocation, VivoNetworkLocationData.this.nlpUpdateTime, VivoNetworkLocationData.this.mGpsLocation, VivoNetworkLocationData.this.gpsUpdateTime);
                    VivoNetworkLocationData.this.nlpUpdateTime = 0L;
                    VivoNetworkLocationData.access$108(VivoNetworkLocationData.this);
                    boolean unused5 = VivoNetworkLocationData.hasData = true;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class LocationDiff {
        private static final String KEY_DIFF = "diff";
        private static final String KEY_GPS = "gps";
        private static final String KEY_NLP = "nlp";
        private JSONArray arr = new JSONArray();

        public LocationDiff() {
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void addLocation(Location nlploc, long nlptime, Location gpsloc, long gpstime) {
            try {
                JSONObject o = new JSONObject();
                String nlp = nlploc.getAccuracy() + "," + VivoNetworkLocationData.this.mSdf.format(new Date(nlptime)) + "," + VivoNetworkLocationData.this.mSdf.format(new Date(nlploc.getTime()));
                String gps = gpsloc.getAccuracy() + "," + VivoNetworkLocationData.this.mSdf.format(new Date(gpstime)) + "," + VivoNetworkLocationData.this.mSdf.format(new Date(gpsloc.getTime()));
                String diff = nlploc.distanceTo(gpsloc) + "," + (nlptime - gpstime);
                if (VivoNetworkLocationData.DEBUG) {
                    VLog.d(VivoNetworkLocationData.TAG, "addLocation");
                }
                o.put(KEY_NLP, nlp);
                o.put(KEY_GPS, gps);
                o.put(KEY_DIFF, diff);
                this.arr.put(o);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public JSONArray getJsonArray() {
            return this.arr;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class NlpJsonWrapper {
        private static final String KEY_LOCATION_DIFF = "loc_diff";
        private static final String KEY_START = "start";
        private static final String KEY_STOP = "stop";
        private LocationDiff mLocationDiff;
        private long startTime;
        private long stopTime = 0;

        public NlpJsonWrapper(long startTime) {
            this.startTime = startTime;
            this.mLocationDiff = new LocationDiff();
        }

        public void setLocation(Location nlpLocation, long nlpTime, Location gpsLocation, long gpsTime) {
            this.mLocationDiff.addLocation(nlpLocation, nlpTime, gpsLocation, gpsTime);
        }

        public String toJsonString() {
            JSONObject obj = new JSONObject();
            try {
                obj.put(KEY_START, VivoNetworkLocationData.this.mSdf.format(Long.valueOf(this.startTime)));
                obj.put(KEY_LOCATION_DIFF, this.mLocationDiff.getJsonArray());
                obj.put(KEY_STOP, VivoNetworkLocationData.this.mSdf.format(Long.valueOf(this.stopTime)));
                return obj.toString();
            } catch (JSONException je) {
                VLog.d(VivoNetworkLocationData.TAG, "toJsonString got exception:" + je.getMessage());
                return "{}";
            }
        }

        public HashMap<String, String> getMapResult() {
            HashMap<String, String> mapResult = new HashMap<>();
            mapResult.put(VCD_FF_1.UUID_STR, UUID.randomUUID().toString());
            mapResult.put(KEY_START, VivoNetworkLocationData.this.mSdf.format(Long.valueOf(this.startTime)));
            mapResult.put(KEY_LOCATION_DIFF, this.mLocationDiff.getJsonArray().toString());
            mapResult.put(KEY_STOP, VivoNetworkLocationData.this.mSdf.format(Long.valueOf(this.stopTime)));
            return mapResult;
        }
    }

    public void startNavigating() {
        if (DEBUG) {
            VLog.d(TAG, "startNavigating");
        }
        this.mDCHandler.removeMessages(1);
        this.mLocationManager.requestLocationUpdates("passive", 0L, 0.0f, this.mPassiveLocationListener);
        this.mDCHandler.sendEmptyMessage(0);
    }

    public void stopNavigating() {
        if (DEBUG) {
            VLog.d(TAG, "stopNavigating");
        }
        this.mLocationManager.removeUpdates(this.mPassiveLocationListener);
        this.mDCHandler.sendEmptyMessage(1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void writeData() {
        Object obj = this.mVCD;
        if (obj != null) {
            boolean control = ((VivoCollectData) obj).getControlInfo("203");
            if (hasData && this.mNlpJsonWrapper != null && control) {
                HashMap<String, String> params = new HashMap<>(1);
                this.mNlpJsonWrapper.stopTime = System.currentTimeMillis();
                String content = this.mNlpJsonWrapper.toJsonString();
                params.put("info", content);
                ((VivoCollectData) this.mVCD).writeData("203", "2035", this.mNlpJsonWrapper.startTime, this.mNlpJsonWrapper.stopTime, this.mNlpJsonWrapper.stopTime - this.mNlpJsonWrapper.startTime, 1, params);
                HashMap<String, String> params1 = this.mNlpJsonWrapper.getMapResult();
                EventTransfer.getInstance().singleEvent("F500", "F500|10007", System.currentTimeMillis(), 0L, params1);
                if (DEBUG) {
                    VLog.d(TAG, "writeData content:" + content);
                }
            } else if (DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("writeData mNlpJsonWrapper=");
                sb.append(this.mNlpJsonWrapper == null ? "Null" : "NotNull");
                sb.append(" control=");
                sb.append(control);
                sb.append(" hasData=");
                sb.append(hasData);
                VLog.d(TAG, sb.toString());
            }
        }
        this.mNlpJsonWrapper = null;
    }
}