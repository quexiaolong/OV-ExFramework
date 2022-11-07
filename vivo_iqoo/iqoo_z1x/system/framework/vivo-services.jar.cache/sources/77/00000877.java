package com.vivo.services.timezone;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.icu.util.TimeZone;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Xml;
import com.android.server.VivoAlarmMgrServiceImpl;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import libcore.timezone.CountryTimeZones;
import libcore.timezone.TimeZoneFinder;
import libcore.timezone.ZoneInfoDb;
import org.xmlpull.v1.XmlPullParser;

/* loaded from: classes.dex */
public class LocationUpdateHelper extends Handler {
    private static final String ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
    private static final String ACTION_COUNTRY_CHANGED = "vivo.intent.action.USER_COUNTRY_CHANGE";
    private static final String ACTION_NETWORK_SET_TIMEZONE = "android.intent.action.NETWORK_SET_TIMEZONE";
    private static final String ACTION_SCREEN_ON = "android.intent.action.SCREEN_ON";
    private static final String ACTION_SIGNAL_CHANGED = "android.intent.action.SERVICE_STATE";
    private static final String ACTION_TZDB_INTENT_CONFIG = "com.vivo.daemonService.unifiedconfig.update_finish_broadcast_TZDBConfig";
    private static final String ACTION_TZ_INTENT_CONFIG = "com.vivo.daemonService.unifiedconfig.update_finish_broadcast_TZConfig";
    private static final String ACTION_WEATHER_BROADCAST = "com.vivo.weather.ACTION_SEND_LOCAL_INFO";
    private static final String CONFIG_ENGINE_VERSION = "v2.0";
    private static final String CONFIG_TYPE = "1";
    private static final String CONFIG_URI = "content://com.vivo.abe.unifiedconfig.provider/configs";
    private static final String DEFAULT_ENUMLAT_COUNTRY = "false";
    private static final String DEFAULT_ENUMLAT_LOATION = "false";
    private static final String DEFAULT_ENUMLAT_PLMN = "false";
    private static final long DELAY_SWITCH_ON_TIME = 120000;
    private static final String INTENT_PREFIX = "com.vivo.daemonService.unifiedconfig.update_finish_broadcast_";
    private static final long JUST_BOOT_PERIOD = 300000;
    private static final long LOCATION_CACHE_VALID_TIME = 1800000000000L;
    private static final int LOCAT_GPS = 1;
    private static final int LOCAT_NET = 0;
    private static final int LOCAT_NO_LISTENER = -1;
    private static final int MSG_AUTO_TIME_SWITCH_CHANGE = 0;
    private static final int MSG_EMULATE_LOCATION = 4;
    private static final int MSG_GPS_LOCATION_RESULT_RET = 2;
    private static final int MSG_LOCATION_TIMEOUT = 3;
    private static final int MSG_NET_LOCATION_RESULT_RET = 1;
    private static final int MSG_START_CORRECTION_DELAY = 5;
    private static final String[] MSG_STRING = {"MSG_AUTO_TIME_SWITCH_CHANGE", "MSG_NET_LOCATION_RESULT_RET", "MSG_GPS_LOCATION_RESULT_RET", "MSG_LOCATION_TIMEOUT", "MSG_EMULATE_LOCATION", "MSG_START_CORRECTION_DELAY"};
    private static final String TAG = "LocationUpdateHelper";
    private static final String TZDB_CONFIG_MODULE = "TZDBConfig";
    private static final String TZ_CONFIG_IDENTIFY = "vivo_tz_configuration";
    private static final String TZ_CONFIG_MODULE = "TZConfig";
    private static LocationUpdateHelper mLocationUpdateHelper;
    private long DEFAULT_CORRECTION_INTERVAL;
    private ContentObserver mAutoTimeObserver;
    private ConnectivityManager mConnectivityManager;
    private Context mContext;
    private String mCurrentCountry;
    private int mCurrentLocatListener;
    private String mCurrentTimeZone;
    private boolean mEnableRetry;
    private boolean mEumlateCountry;
    private boolean mEumlateLocation;
    private boolean mEumlatePlmn;
    private final LocationListener mGPSLocationListener;
    private boolean mIsAutoTimeZone;
    private long mLastTimeScreenOnToCorrect;
    private LocationManager mLocationManager;
    private final LocationListener mNetworkLocationListener;
    private final BroadcastReceiver mReceiver;
    private boolean[] mSimStatus;
    private final BroadcastReceiver mStableReceiver;
    private TZManagerService mTZManagerService;
    private TelephonyManager mTelephonyManager;
    private HashMap<String, CountryInfo> mTimeZoneConfig;
    private boolean mTimeZoneOfflineEnable;
    private boolean mTimezoneCorrectionOn;
    private long mUpdateTime;
    private VivoAlarmMgrServiceImpl mVivoAlarmMgrService;

    public static void makeDefaultUpdater(Context context, TZManagerService service, Looper looper, long repeatTime) {
        if (mLocationUpdateHelper == null) {
            mLocationUpdateHelper = new LocationUpdateHelper(context, service, looper, repeatTime);
        }
    }

    public static LocationUpdateHelper getInstance() {
        return mLocationUpdateHelper;
    }

    public LocationUpdateHelper(Context context, TZManagerService service, Looper looper, long repeatTime) {
        super(looper);
        this.mCurrentLocatListener = -1;
        this.DEFAULT_CORRECTION_INTERVAL = 2700000L;
        this.mEumlateLocation = false;
        this.mEumlatePlmn = false;
        this.mEumlateCountry = false;
        this.mTimezoneCorrectionOn = false;
        this.mIsAutoTimeZone = false;
        this.mTimeZoneOfflineEnable = true;
        this.mEnableRetry = true;
        this.mSimStatus = new boolean[2];
        this.mUpdateTime = 0L;
        this.mLastTimeScreenOnToCorrect = 0L;
        this.mCurrentTimeZone = "invalid";
        this.mTimeZoneConfig = new HashMap<>();
        this.mTelephonyManager = null;
        this.mAutoTimeObserver = new ContentObserver(this) { // from class: com.vivo.services.timezone.LocationUpdateHelper.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                VLog.d(LocationUpdateHelper.TAG, "Auto TimeZone state change");
                Message msg = new Message();
                msg.what = 0;
                msg.arg1 = LocationUpdateHelper.this.isAutoTimeZone() ? 1 : 0;
                LocationUpdateHelper.this.sendMessage(msg);
            }
        };
        this.mNetworkLocationListener = new LocationListener() { // from class: com.vivo.services.timezone.LocationUpdateHelper.2
            @Override // android.location.LocationListener
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override // android.location.LocationListener
            public void onProviderEnabled(String provider) {
            }

            @Override // android.location.LocationListener
            public void onProviderDisabled(String provider) {
            }

            @Override // android.location.LocationListener
            public void onLocationChanged(Location location) {
                VLog.d(LocationUpdateHelper.TAG, "Net onChanged ");
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                LocationUpdateHelper.this.removeMessages(3);
                Message msg = new Message();
                msg.what = 1;
                msg.obj = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + latitude + "/" + longitude;
                LocationUpdateHelper.this.sendMessage(msg);
            }
        };
        this.mGPSLocationListener = new LocationListener() { // from class: com.vivo.services.timezone.LocationUpdateHelper.3
            @Override // android.location.LocationListener
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override // android.location.LocationListener
            public void onProviderEnabled(String provider) {
            }

            @Override // android.location.LocationListener
            public void onProviderDisabled(String provider) {
            }

            @Override // android.location.LocationListener
            public void onLocationChanged(Location location) {
                VLog.d(LocationUpdateHelper.TAG, "G onChanged ");
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                LocationUpdateHelper.this.removeMessages(3);
                Message msg = new Message();
                msg.what = 2;
                msg.obj = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + latitude + "/" + longitude;
                LocationUpdateHelper.this.sendMessage(msg);
            }
        };
        this.mStableReceiver = new BroadcastReceiver() { // from class: com.vivo.services.timezone.LocationUpdateHelper.4
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                VLog.d(LocationUpdateHelper.TAG, "mStableReceiver:" + intent.getAction());
                if (LocationUpdateHelper.ACTION_COUNTRY_CHANGED.equals(intent.getAction())) {
                    String country = LocationUpdateHelper.this.getCurrentCountry();
                    if (!LocationUpdateHelper.this.mCurrentCountry.equalsIgnoreCase(country)) {
                        if (LocationUpdateHelper.this.mIsAutoTimeZone) {
                            LocationUpdateHelper.this.updateCountry("received the ACTION_COUNTRY_CHANGED", country);
                            return;
                        }
                        LocationUpdateHelper.this.mCurrentCountry = country;
                        VLog.d(LocationUpdateHelper.TAG, "AutoTimeZone is closed! ignore this ACTION_COUNTRY_CHANGED.");
                        return;
                    }
                    VLog.d(LocationUpdateHelper.TAG, "country is not changed");
                } else if (LocationUpdateHelper.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                    LocationUpdateHelper.this.loadTimeZoneConfig();
                    if (LocationUpdateHelper.this.mIsAutoTimeZone) {
                        LocationUpdateHelper locationUpdateHelper = LocationUpdateHelper.this;
                        if (!locationUpdateHelper.isSingleTimeZoneCountry(locationUpdateHelper.mCurrentCountry)) {
                            LocationUpdateHelper.this.onTimeZoneConfigChange();
                        }
                    }
                } else if (LocationUpdateHelper.ACTION_TZ_INTENT_CONFIG.equals(intent.getAction())) {
                    LocationUpdateHelper.this.loadTimeZoneConfig();
                    if (LocationUpdateHelper.this.mIsAutoTimeZone) {
                        LocationUpdateHelper locationUpdateHelper2 = LocationUpdateHelper.this;
                        if (!locationUpdateHelper2.isSingleTimeZoneCountry(locationUpdateHelper2.mCurrentCountry)) {
                            LocationUpdateHelper.this.onTimeZoneConfigChange();
                        }
                    }
                } else if (LocationUpdateHelper.ACTION_TZDB_INTENT_CONFIG.equals(intent.getAction())) {
                    if (LocationUpdateHelper.this.mTZManagerService != null) {
                        LocationUpdateHelper.this.mTZManagerService.updateTimeZoneDB();
                    }
                } else if (!LocationUpdateHelper.ACTION_SIGNAL_CHANGED.equals(intent.getAction())) {
                    if (LocationUpdateHelper.this.isEmulateCountry() && LocationUpdateHelper.ACTION_SCREEN_ON.equals(intent.getAction()) && !LocationUpdateHelper.this.getCurrentCountry().equals(LocationUpdateHelper.this.mCurrentCountry)) {
                        LocationUpdateHelper locationUpdateHelper3 = LocationUpdateHelper.this;
                        locationUpdateHelper3.updateCountry("in emulate testing, find emulate country code changed", locationUpdateHelper3.getCurrentCountry());
                        LocationUpdateHelper.this.trigerUpdateTimeZone("updateCountry");
                    }
                } else {
                    Bundle bundle = intent.getExtras();
                    int voiceRegState = bundle.getInt("voiceRegState");
                    boolean signal = voiceRegState == 0;
                    int num = intent.getIntExtra("slot", -1);
                    VLog.d(LocationUpdateHelper.TAG, "Num:" + num + "--signal:" + signal);
                    LocationUpdateHelper locationUpdateHelper4 = LocationUpdateHelper.this;
                    if (!locationUpdateHelper4.isSingleTimeZoneCountry(locationUpdateHelper4.mCurrentCountry)) {
                        LocationUpdateHelper locationUpdateHelper5 = LocationUpdateHelper.this;
                        if (locationUpdateHelper5.isAllCountryDispatchNitz(locationUpdateHelper5.mCurrentCountry)) {
                            LocationUpdateHelper.this.handleSignalChanged(signal, num);
                            return;
                        }
                    }
                    LocationUpdateHelper.this.recordSignal(signal, num);
                }
            }
        };
        this.mReceiver = new BroadcastReceiver() { // from class: com.vivo.services.timezone.LocationUpdateHelper.5
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                VLog.d(LocationUpdateHelper.TAG, "mReceiver:" + action);
                if (action != null) {
                    if (LocationUpdateHelper.ACTION_WEATHER_BROADCAST.equals(action)) {
                        if (!LocationUpdateHelper.this.isEmulateLocation()) {
                            try {
                                LocationUpdateHelper.this.processWeatherBroadcast(intent);
                            } catch (Exception e) {
                                VLog.d(LocationUpdateHelper.TAG, "processWeatherBroadcast error:" + e.toString());
                            }
                        }
                    } else if (LocationUpdateHelper.ACTION_SCREEN_ON.equals(action)) {
                        LocationUpdateHelper.this.processScreenOnEvent();
                    }
                }
            }
        };
        this.mContext = context;
        this.DEFAULT_CORRECTION_INTERVAL = repeatTime;
        this.mTZManagerService = service;
        this.mEumlateLocation = "true".equals(SystemProperties.get("temp.timezone.check.emulatelocation", "false"));
        this.mEumlatePlmn = "true".equals(SystemProperties.get("temp.timezone.check.emulateplmn", "false"));
        this.mEumlateCountry = "true".equals(SystemProperties.get("temp.timezone.check.emulatecountry", "false"));
        this.mConnectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        this.mLocationManager = (LocationManager) this.mContext.getSystemService("location");
        this.mVivoAlarmMgrService = VivoAlarmMgrServiceImpl.getInstance();
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("auto_time_zone"), true, this.mAutoTimeObserver);
        this.mIsAutoTimeZone = isAutoTimeZone();
        this.mCurrentCountry = getCurrentCountry();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_COUNTRY_CHANGED);
        filter.addAction(ACTION_BOOT_COMPLETED);
        filter.addAction(ACTION_TZ_INTENT_CONFIG);
        filter.addAction(ACTION_TZDB_INTENT_CONFIG);
        filter.addAction(ACTION_SIGNAL_CHANGED);
        if (isEmulateCountry() && isEmulateLocation()) {
            filter.addAction(ACTION_SCREEN_ON);
        }
        context.registerReceiver(this.mStableReceiver, filter, null, this);
        initDefaultTimeZoneConfig();
        boolean[] zArr = this.mSimStatus;
        zArr[0] = false;
        zArr[1] = false;
        this.mLastTimeScreenOnToCorrect = SystemClock.elapsedRealtime();
    }

    @Override // android.os.Handler
    public void handleMessage(Message msg) {
        if (msg != null) {
            VLog.d(TAG, "handleMessage what = " + getMessageAction(msg.what));
            int i = msg.what;
            if (i == 0) {
                processAutoTimeSwitchEvent(msg.arg1 == 1);
            } else if (i == 1) {
                handleLocationResult((String) msg.obj, 0);
            } else if (i == 2) {
                handleLocationResult((String) msg.obj, 1);
            } else if (i != 3) {
                if (i != 4) {
                    if (i == 5) {
                        switchOnTimezoneCorrection(this.mCurrentCountry);
                        this.mEnableRetry = true;
                        trigerUpdateTimeZone("lost signal");
                        return;
                    }
                    return;
                }
                Location location = getEmulateLocation(msg.arg1);
                if (location == null) {
                    return;
                }
                if (msg.arg1 == 1) {
                    this.mGPSLocationListener.onLocationChanged(location);
                } else {
                    this.mNetworkLocationListener.onLocationChanged(location);
                }
            } else if (msg.arg1 == 1) {
                VLog.d(TAG, "Timeout!  remove G linstner mEnableRetry:" + this.mEnableRetry);
                if (!isEmulateLocation()) {
                    this.mLocationManager.removeUpdates(this.mGPSLocationListener);
                    this.mCurrentLocatListener = -1;
                }
                if (isNetworkConnected() && this.mEnableRetry) {
                    trigerUpdateTimeZone("gps location failed,force net");
                    this.mEnableRetry = false;
                }
            } else {
                VLog.d(TAG, "Timeout!  remove NET linstner mEnableRetry:" + this.mEnableRetry);
                if (!isEmulateLocation()) {
                    this.mLocationManager.removeUpdates(this.mNetworkLocationListener);
                    this.mCurrentLocatListener = -1;
                }
                if (this.mEnableRetry) {
                    trigerUpdateTimeZone("net location failed,force gps");
                    this.mEnableRetry = false;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void processScreenOnEvent() {
        long thisTimeScreenOn = SystemClock.elapsedRealtime();
        if (thisTimeScreenOn - this.mLastTimeScreenOnToCorrect > this.DEFAULT_CORRECTION_INTERVAL) {
            this.mEnableRetry = true;
            trigerUpdateTimeZone("screen on leads correction");
            this.mLastTimeScreenOnToCorrect = thisTimeScreenOn;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void trigerUpdateTimeZone(String reason) {
        VLog.d(TAG, "trigerUpdateTimeZone ---" + reason);
        if (!isEmulateCountry() && !isEmulateLocation()) {
            String plmn = getPlmnByCardNum(0);
            if (plmn != null && isSupportNitz(plmn)) {
                if (this.mSimStatus[0]) {
                    VLog.d(TAG, "1st has signal and support nitz, do nothing");
                    return;
                }
                VLog.d(TAG, "1st support NITZ but now it can not get the signal");
            }
            String plmn2 = getPlmnByCardNum(1);
            if (plmn2 != null && isSupportNitz(plmn2)) {
                if (this.mSimStatus[1]) {
                    VLog.d(TAG, "2nd has signal and support nitz, do nothing");
                    return;
                }
                VLog.d(TAG, "2nd support NITZ but now it can not get the signal");
            }
        }
        if (reason.indexOf("force gps") != -1) {
            startLocation(1);
        } else if (reason.indexOf("force net") != -1) {
            startLocation(0);
        } else if ("updateCountry".equals(reason)) {
            LaunchNewRoundLocate();
        } else {
            Location location = getLastLocation();
            if (location != null) {
                long v = SystemClock.elapsedRealtimeNanos() - location.getElapsedRealtimeNanos();
                VLog.d(TAG, "diff:" + v);
                if (Math.abs(v) < LOCATION_CACHE_VALID_TIME) {
                    VLog.d(TAG, "use last info");
                    updateTimeZone(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + location.getLatitude(), Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + location.getLongitude());
                    return;
                }
                LaunchNewRoundLocate();
                return;
            }
            LaunchNewRoundLocate();
        }
    }

    private void LaunchNewRoundLocate() {
        this.mEnableRetry = true;
        if (isNetworkConnected()) {
            startLocation(0);
        } else {
            startLocation(1);
        }
    }

    private void updateTimeZone(String latitude, String longitude) {
        String timeZone = getTimeZone(latitude, longitude);
        if ("America/SitkaAmerica/Metlakatla".equals(timeZone)) {
            timeZone = "America/Metlakatla";
        }
        updateTimeZone(timeZone);
    }

    private void updateTimeZone(String timeZone) {
        VLog.d(TAG, "updateTimeZone  zone:" + timeZone + " old timezone is=" + this.mCurrentTimeZone);
        if (timeZone == null) {
            VLog.d(TAG, "Invalid timezone id " + timeZone);
        } else if (timeZone.equals(this.mCurrentTimeZone)) {
            VLog.d(TAG, "no need update timezone,no change!");
        } else if (TextUtils.isEmpty(timeZone) || !checkTimeZoneIdAvailable(timeZone)) {
            VLog.d(TAG, "Invalid timezone id " + timeZone);
        } else {
            this.mVivoAlarmMgrService.setTimeZoneImpl(timeZone);
            this.mCurrentTimeZone = timeZone;
            this.mUpdateTime = SystemClock.elapsedRealtime();
            if (timeZone != null && timeZone.length() <= 91) {
                SystemProperties.set("persist.radio.vivo.zone", timeZone);
            }
            Intent intent = new Intent(ACTION_NETWORK_SET_TIMEZONE);
            intent.addFlags(536870912);
            intent.putExtra("time-zone", timeZone);
            this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    private void startLocation(int type) {
        if (hasMessages(3)) {
            VLog.d(TAG, "... ,cancle this !");
            return;
        }
        if (isEmulateLocation()) {
            Message msg = new Message();
            msg.arg1 = type;
            msg.what = 4;
            sendMessage(msg);
        } else if (type == 0) {
            this.mLocationManager.requestLocationUpdates("network", 0L, 0.0f, this.mNetworkLocationListener);
            this.mCurrentLocatListener = 0;
        } else {
            this.mLocationManager.requestLocationUpdates("gps", 0L, 0.0f, this.mGPSLocationListener);
            this.mCurrentLocatListener = 1;
        }
        VLog.d(TAG, "start with type=" + type);
        setLocationTimeoutCheck(type);
    }

    private void handleLocationResult(String location, int locatType) {
        VLog.d(TAG, "handleResult Type=" + locatType);
        String[] l = location.split("/");
        updateTimeZone(l[0], l[1]);
        if (locatType == 0) {
            this.mLocationManager.removeUpdates(this.mNetworkLocationListener);
            this.mCurrentLocatListener = -1;
            removeMessages(1);
            return;
        }
        this.mLocationManager.removeUpdates(this.mGPSLocationListener);
        this.mCurrentLocatListener = -1;
        removeMessages(2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void processWeatherBroadcast(Intent intent) {
        HashMap<String, String> info = (HashMap) intent.getSerializableExtra("local_info");
        if (info == null) {
            return;
        }
        String latitude = info.get("latitude");
        String longitude = info.get("longitude");
        info.get("locality");
        info.get("countryCode");
        info.get("province");
        String plmn = getPlmnByCardNum(0);
        if (plmn != null && isSupportNitz(plmn)) {
            if (this.mSimStatus[0]) {
                VLog.d(TAG, "1st has signal and support nitz, do nothing");
                return;
            }
            VLog.d(TAG, "processWeatherBroadcast 1st support NITZ but now it can not get the signal");
        }
        String plmn2 = getPlmnByCardNum(1);
        if (plmn2 != null && isSupportNitz(plmn2)) {
            if (this.mSimStatus[1]) {
                VLog.d(TAG, "2nd has signal and support nitz, do nothing");
                return;
            }
            VLog.d(TAG, "processWeatherBroadcast 2nd support NITZ but now it can not get the signal");
        }
        if (SystemClock.elapsedRealtime() - this.mUpdateTime <= this.DEFAULT_CORRECTION_INTERVAL) {
            VLog.d(TAG, "processWeatherBroadcast not match the condition, ignore update timezone");
        } else {
            updateTimeZone(latitude, longitude);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCountry(String reason, String country) {
        VLog.d(TAG, "updateCountry:" + country + ",reason:" + reason);
        this.mCurrentCountry = country;
        if (isSingleTimeZoneCountry(country)) {
            if (isTimezoneCorrectionOn()) {
                switchOffTimezoneCorrection("now in single timezone country, switch off timezone correction");
            }
        } else if (isAllCountryDispatchNitz(this.mCurrentCountry)) {
            if (isDeviceHasSignal() && isTimezoneCorrectionOn()) {
                switchOffTimezoneCorrection("current country support nitz and the device has signal");
            } else if (!isDeviceHasSignal() && !isTimezoneCorrectionOn() && !hasMessages(5)) {
                VLog.d(TAG, "the country support nitz but now there is no signal");
                switchOnTimezoneCorrection(this.mCurrentCountry);
                this.mEnableRetry = true;
                trigerUpdateTimeZone("updateCountry");
            }
        } else {
            VLog.d(TAG, "current country does not support nitz");
            if (!isTimezoneCorrectionOn()) {
                switchOnTimezoneCorrection(this.mCurrentCountry);
                this.mEnableRetry = true;
                trigerUpdateTimeZone("updateCountry");
            }
        }
    }

    private void processAutoTimeSwitchEvent(boolean isAutoTimeZone) {
        VLog.d(TAG, "processAutoTimeSwitchEvent " + isAutoTimeZone);
        this.mIsAutoTimeZone = isAutoTimeZone;
        if (isAutoTimeZone) {
            updateCountry("auto time zone start", getCurrentCountry());
            return;
        }
        if (hasMessages(5)) {
            VLog.d(TAG, "cancel autotime zone when delay period");
            removeMessages(5);
        }
        if (isTimezoneCorrectionOn()) {
            switchOffTimezoneCorrection("auto time zone closed");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void recordSignal(boolean signal, int num) {
        if (num != 0 && num != 1) {
            return;
        }
        this.mSimStatus[num] = signal;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleSignalChanged(boolean signal, int num) {
        VLog.d(TAG, "handleSignalChanged --signal=" + signal + " --Num=" + num);
        if (num != 0 && num != 1) {
            return;
        }
        this.mSimStatus[num] = signal;
        if (signal) {
            if (hasMessages(5)) {
                removeMessages(5);
            } else if (isTimezoneCorrectionOn()) {
                switchOffTimezoneCorrection("the current country --" + this.mCurrentCountry + " support NITZ and the signal recovery");
            }
        } else if (this.mIsAutoTimeZone && !isDeviceHasSignal() && !isTimezoneCorrectionOn() && !hasMessages(5)) {
            maybeSwitchOnCorrection();
        }
    }

    private void maybeSwitchOnCorrection() {
        VLog.d(TAG, "send the MSG_START_CORRECTION_DELAY msg in a NITZable country");
        delaySwitchOnCorrection();
        if (SystemClock.elapsedRealtime() < JUST_BOOT_PERIOD) {
            this.mEnableRetry = true;
            trigerUpdateTimeZone("the device need to check its timezone once after boot in us-like country");
        }
    }

    private void delaySwitchOnCorrection() {
        if (hasMessages(5)) {
            VLog.d(TAG, "the delay starting msg already exists in the queue");
            return;
        }
        Message msg = new Message();
        msg.what = 5;
        sendMessageDelayed(msg, DELAY_SWITCH_ON_TIME);
    }

    private void switchOnTimezoneCorrection(String country) {
        VLog.d(TAG, "switchOnTimezoneCorrection in:" + country);
        if (isTimezoneCorrectionOn()) {
            VLog.d(TAG, "correction already switch on");
            return;
        }
        clearMessage();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_WEATHER_BROADCAST);
        filter.addAction(ACTION_SCREEN_ON);
        this.mContext.registerReceiver(this.mReceiver, filter, null, this);
        setTimezoneCorrectionStatus(true);
    }

    private void switchOffTimezoneCorrection(String reason) {
        VLog.d(TAG, "switchOffTimezoneCorrection reason:" + reason + " isTimezoneCorrectionOn:" + isTimezoneCorrectionOn());
        if (!isTimezoneCorrectionOn()) {
            return;
        }
        clearMessage();
        this.mContext.unregisterReceiver(this.mReceiver);
        setTimezoneCorrectionStatus(false);
        this.mCurrentTimeZone = "invalid";
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isSingleTimeZoneCountry(String countryCode) {
        if (countryCode == null || Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK.equals(countryCode)) {
            return false;
        }
        if ("cn".equalsIgnoreCase(countryCode)) {
            return true;
        }
        CountryTimeZones countryTimeZones = TimeZoneFinder.getInstance().lookupCountryTimeZones(countryCode);
        if (countryTimeZones != null) {
            List<CountryTimeZones.TimeZoneMapping> timeZoneMappings = countryTimeZones.getTimeZoneMappings();
            TimeZone defaultTimeZone = countryTimeZones.getDefaultTimeZone();
            return true ^ countryUsesDifferentOffsets(timeZoneMappings, defaultTimeZone);
        }
        VLog.d(TAG, "current country " + countryCode + " is invalid, as default, it is not a single tz country");
        return false;
    }

    private boolean countryUsesDifferentOffsets(List<CountryTimeZones.TimeZoneMapping> timezoneMappings, TimeZone defaultTimeZone) {
        if (timezoneMappings == null || timezoneMappings.isEmpty() || defaultTimeZone == null) {
            VLog.d(TAG, "should not happened, no timezone data for this country");
            return false;
        } else if (timezoneMappings.size() == 1) {
            return false;
        } else {
            long whenMillis = System.currentTimeMillis();
            String countryDefaultTZID = defaultTimeZone.getID();
            int countryDefaultOffset = defaultTimeZone.getOffset(whenMillis);
            for (CountryTimeZones.TimeZoneMapping timezoneMapping : timezoneMappings) {
                if (!timezoneMapping.getTimeZoneId().equals(countryDefaultTZID)) {
                    TimeZone timezone = timezoneMapping.getTimeZone();
                    int candidateOffset = timezone.getOffset(whenMillis);
                    if (countryDefaultOffset != candidateOffset) {
                        VLog.d(TAG, "in multiple timezone country");
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isAllCountryDispatchNitz(String country) {
        if (country == null || Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK.equals(country)) {
            return false;
        }
        CountryInfo countryInfo = this.mTimeZoneConfig.get(country);
        if (countryInfo != null && "true".equals(countryInfo.mEnableNITZ)) {
            return true;
        }
        VLog.d(TAG, "the current country " + country + " is invalid, as default, all country not dispatch nitz");
        return false;
    }

    private boolean isSupportNitz(String plmn) {
        if (plmn == null || Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK.equals(plmn)) {
            return false;
        }
        if (isAllCountryDispatchNitz(this.mCurrentCountry)) {
            return true;
        }
        CountryInfo countryInfo = this.mTimeZoneConfig.get(this.mCurrentCountry);
        if (countryInfo == null) {
            return false;
        }
        VLog.d(TAG, "config nitz data!");
        Boolean v = countryInfo.mPlmnInfo.get(plmn);
        if (v != null) {
            return v.booleanValue();
        }
        return countryInfo.mDefaultPlmnNITZ;
    }

    private boolean isNetworkConnected() {
        NetworkInfo activeInfo = this.mConnectivityManager.getActiveNetworkInfo();
        if (activeInfo == null || !activeInfo.isConnected()) {
            VLog.d(TAG, "isNeworkConnected false");
            return false;
        }
        VLog.d(TAG, "isNeworkConnected true");
        return true;
    }

    private boolean isTimezoneCorrectionOn() {
        return this.mTimezoneCorrectionOn;
    }

    private boolean isDeviceHasSignal() {
        boolean[] zArr = this.mSimStatus;
        return zArr[0] || zArr[1];
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isAutoTimeZone() {
        try {
            return Settings.Global.getInt(this.mContext.getContentResolver(), "auto_time_zone") > 0;
        } catch (Exception e) {
            VLog.v(TAG, e.toString());
            return false;
        }
    }

    public boolean shouldInterceptNITZ() {
        if (isEmulateCountry() && isEmulateLocation()) {
            return true;
        }
        return (!isTimezoneCorrectionOn() || "invalid".equals(this.mCurrentTimeZone) || Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK.equals(this.mCurrentTimeZone) || this.mCurrentTimeZone == null) ? false : true;
    }

    Location getLastLocation() {
        if (isEmulateLocation()) {
            return null;
        }
        Location loc1 = this.mLocationManager.getLastKnownLocation("network");
        Location loc2 = this.mLocationManager.getLastKnownLocation("gps");
        if (loc1 == null && loc2 == null) {
            return null;
        }
        if (loc1 == null || loc2 == null) {
            return loc1 == null ? loc2 : loc1;
        } else if (loc1.getElapsedRealtimeNanos() - loc2.getElapsedRealtimeNanos() > 0) {
            return loc1;
        } else {
            return loc2;
        }
    }

    private String getTimeZone(String latitude, String longitude) {
        String timeZone = null;
        if (this.mTimeZoneOfflineEnable && (((timeZone = queryTimeZoneOffline(latitude, longitude)) == null || Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK.equals(timeZone) || timeZone.contains("error")) && isNetworkConnected())) {
            VLog.d(TAG, "queryTimeZoneOffline failed:");
        }
        return timeZone;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getCurrentCountry() {
        if (isEmulateCountry()) {
            VLog.d(TAG, "use EmulateCountry");
            return SystemProperties.get("temp.timezone.check.emulatecountrycode", "us");
        }
        String mcc = SystemProperties.get("persist.radio.vivo.mcc", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        VLog.d(TAG, "mcc=" + mcc);
        if (!Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK.equals(mcc)) {
            try {
                return MccTable.countryCodeForMcc(Integer.parseInt(mcc));
            } catch (Exception e) {
                VLog.d(TAG, "mcc convert countrycode failed! mcc=" + mcc + " exception:" + e.toString());
            }
        }
        VLog.d(TAG, "can not get a valid country");
        return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    }

    private String getPlmn() {
        if (isEmulatePlmn()) {
            String plmn = getEmulatePlmn();
            return plmn;
        }
        String plmn2 = getRealPlmn();
        return plmn2;
    }

    private String getRealPlmn() {
        String plmn = getPhonePlmn(0);
        if (plmn == null) {
            return getPhonePlmn(1);
        }
        return plmn;
    }

    private String getPhonePlmn(int index) {
        if (this.mTelephonyManager == null) {
            this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        }
        if (this.mTelephonyManager == null) {
            VLog.d(TAG, "Can't get TelephonyManager service,may be the service is not init!");
            return null;
        }
        int subId = getSubId(0);
        if (subId == -1) {
            VLog.d(TAG, "Can't get 0 subID");
            subId = getSubId(1);
            if (subId == -1) {
                VLog.d(TAG, "Can't get 1st subID");
                return null;
            }
        }
        return this.mTelephonyManager.getNetworkOperator(subId);
    }

    private int getSubId(int num) {
        int[] subIds = SubscriptionManager.getSubId(num);
        if (subIds == null || subIds.length < 1) {
            return -1;
        }
        int subId = subIds[0];
        return subId;
    }

    private String getPlmnByCardNum(int num) {
        if (isEmulatePlmn()) {
            return getEmulatePlmn();
        }
        if (this.mTelephonyManager == null) {
            this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        }
        if (this.mTelephonyManager == null) {
            VLog.d(TAG, "Can't get TelephonyManager service,may be the service is not init!");
            return null;
        }
        int subId = getSubId(num);
        if (subId == -1) {
            return null;
        }
        return this.mTelephonyManager.getNetworkOperator(subId);
    }

    String getMessageAction(int action) {
        String[] strArr = MSG_STRING;
        if (action < strArr.length) {
            return strArr[action];
        }
        return String.valueOf(action);
    }

    private void setLocationTimeoutCheck(int type) {
        Message msg = new Message();
        msg.what = 3;
        msg.arg1 = type;
        sendMessageDelayed(msg, 10000L);
    }

    private void setTimezoneCorrectionStatus(boolean value) {
        this.mTimezoneCorrectionOn = value;
    }

    private String queryTimeZoneOffline(String lat, String lon) {
        String timeZone = null;
        try {
            timeZone = this.mTZManagerService.locationToTimeZone(Double.parseDouble(lat), Double.parseDouble(lon));
        } catch (Exception e) {
            VLog.d(TAG, e.toString());
            e.printStackTrace();
        }
        if (timeZone != null) {
            return timeZone.replace("\"", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        }
        return timeZone;
    }

    private void clearMessage() {
        if (hasMessages(3)) {
            int i = this.mCurrentLocatListener;
            if (i == 0) {
                this.mLocationManager.removeUpdates(this.mNetworkLocationListener);
            } else if (i == 1) {
                this.mLocationManager.removeUpdates(this.mGPSLocationListener);
            }
            removeMessages(3);
        }
        removeMessages(1);
        removeMessages(2);
        removeMessages(4);
        removeMessages(5);
    }

    private boolean checkTimeZoneIdAvailable(String zoneId) {
        if (TextUtils.isEmpty(zoneId)) {
            return false;
        }
        try {
            return ZoneInfoDb.getInstance().hasTimeZone(zoneId);
        } catch (IOException e) {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isEmulateCountry() {
        return this.mEumlateCountry;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isEmulateLocation() {
        return this.mEumlateLocation;
    }

    private boolean isEmulatePlmn() {
        return this.mEumlatePlmn;
    }

    private Location getEmulateLocation(int locatType) {
        VLog.d(TAG, "use EmulateLocation");
        Location location = new Location(locatType == 0 ? "network" : "gps");
        location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        String value = SystemProperties.get("temp.timezone.check.emulatepos", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        String[] ls = value.split(",");
        if (ls.length != 2) {
            VLog.d(TAG, "can't get emulate");
            return null;
        }
        try {
            location.setLatitude(Double.valueOf(ls[0]).doubleValue());
            location.setLongitude(Double.valueOf(ls[1]).doubleValue());
            return location;
        } catch (Exception e) {
            VLog.d(TAG, "invalid");
            return null;
        }
    }

    private String getEmulatePlmn() {
        return SystemProperties.get("temp.timezone.check.emulateplmnid", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class CountryInfo {
        public String mCountryCode;
        public boolean mDefaultPlmnNITZ;
        public String mEnableNITZ;
        public HashMap<String, Boolean> mPlmnInfo = new HashMap<>();

        CountryInfo() {
        }
    }

    private void initDefaultTimeZoneConfig() {
        String[] enableNITZCountries = {"cn", "us", "id"};
        String[] disableNITZCountries = {"aq", "au", "br", "ca", "cd", "cl", "ec", "es", "fm", "gl", "ki", "kz", "mn", "mx", "nz", "pf", "pg", "pt", "ru", "ua", "um"};
        for (String countryCode : enableNITZCountries) {
            CountryInfo ci = new CountryInfo();
            ci.mEnableNITZ = "true";
            ci.mCountryCode = countryCode;
            this.mTimeZoneConfig.put(countryCode, ci);
        }
        for (String countryCode2 : disableNITZCountries) {
            CountryInfo ci2 = new CountryInfo();
            ci2.mEnableNITZ = "false";
            ci2.mCountryCode = countryCode2;
            this.mTimeZoneConfig.put(countryCode2, ci2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void loadTimeZoneConfig() {
        VLog.d(TAG, "loadTimeZoneConfig");
        try {
            readConfig("content://com.vivo.abe.unifiedconfig.provider/configs", TZ_CONFIG_MODULE, "1", CONFIG_ENGINE_VERSION, TZ_CONFIG_IDENTIFY);
        } catch (Exception e) {
            VLog.d(TAG, "loadTimeZoneConfig:" + e.toString());
        }
        dumpTimeZoneConfig();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onTimeZoneConfigChange() {
        VLog.d(TAG, "onTimeZoneConfigChange current country is " + this.mCurrentCountry);
        if (isSingleTimeZoneCountry(this.mCurrentCountry)) {
            if (isTimezoneCorrectionOn()) {
                switchOffTimezoneCorrection("now in single timezone country, switch off timezone correction");
            }
        } else if (isAllCountryDispatchNitz(this.mCurrentCountry)) {
            if (isDeviceHasSignal() && isTimezoneCorrectionOn()) {
                switchOffTimezoneCorrection("current country support nitz and the device has signal");
            } else if (!isDeviceHasSignal() && !isTimezoneCorrectionOn() && !hasMessages(5)) {
                VLog.d(TAG, "the country support nitz but now there is no signal");
                switchOnTimezoneCorrection(this.mCurrentCountry);
                this.mEnableRetry = true;
                trigerUpdateTimeZone("the country support nitz but now there is no signal");
            }
        } else {
            VLog.d(TAG, "current country does not support nitz");
            if (!isTimezoneCorrectionOn()) {
                switchOnTimezoneCorrection(this.mCurrentCountry);
                this.mEnableRetry = true;
                trigerUpdateTimeZone("the country does not support nitz");
            }
        }
    }

    private void readConfig(String uri, String moduleName, String type, String version, String identifier) {
        ContentResolver resolver = this.mContext.getContentResolver();
        String[] selectionArgs = {moduleName, type, version, identifier};
        Cursor cursor = null;
        try {
            try {
                cursor = resolver.query(Uri.parse(uri), null, null, selectionArgs, null);
                if (cursor != null) {
                    cursor.moveToFirst();
                    if (cursor.getCount() > 0) {
                        while (!cursor.isAfterLast()) {
                            cursor.getString(cursor.getColumnIndex("id"));
                            cursor.getString(cursor.getColumnIndex("identifier"));
                            cursor.getString(cursor.getColumnIndex("fileversion"));
                            byte[] filecontent = cursor.getBlob(cursor.getColumnIndex("filecontent"));
                            String applists = new String(filecontent, "UTF-8");
                            VLog.d(TAG, "getConfig Notify.xml:\n  " + applists);
                            StringReader reader = new StringReader(applists);
                            parseTZConfig(reader);
                            cursor.moveToNext();
                        }
                    } else {
                        VLog.e(TAG, "no TimeZone config data");
                    }
                }
                if (cursor == null) {
                    return;
                }
            } catch (Exception e) {
                VLog.e(TAG, "read TimeZone Config error:" + e);
                if (0 == 0) {
                    return;
                }
            }
            cursor.close();
        } catch (Throwable th) {
            if (0 != 0) {
                cursor.close();
            }
            throw th;
        }
    }

    private void parseTZConfig(StringReader reader) {
        String v;
        HashMap<String, CountryInfo> info = new HashMap<>();
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(reader);
            for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                if (eventType == 2) {
                    String name = parser.getName();
                    VLog.d(TAG, "parseTZConfig tag:" + name);
                    if ("country".equals(name)) {
                        CountryInfo countryInfo = parseCountry(parser);
                        if (countryInfo == null) {
                            throw new Exception("parse country info error");
                        }
                        info.put(countryInfo.mCountryCode, countryInfo);
                    } else if ("offlineswitch".equals(name) && (v = parser.getText()) != null) {
                        this.mTimeZoneOfflineEnable = "on".equals(v);
                    }
                }
            }
            this.mTimeZoneConfig = info;
        } catch (Exception e) {
            VLog.d(TAG, "----error:parseTZConfig " + e.toString());
        }
    }

    private CountryInfo parseCountry(XmlPullParser parser) {
        CountryInfo countryInfo = new CountryInfo();
        countryInfo.mDefaultPlmnNITZ = false;
        int l = parser.getAttributeCount();
        for (int i = 0; i < l; i++) {
            String attrName = parser.getAttributeName(i);
            VLog.d(TAG, "country-----attr:" + attrName + " value:" + parser.getAttributeValue(i));
            if ("countryCode".equals(attrName)) {
                countryInfo.mCountryCode = parser.getAttributeValue(i);
            } else if ("enableNITZ".equals(attrName)) {
                countryInfo.mEnableNITZ = parser.getAttributeValue(i);
            } else if ("defaultPlmnNITZ".equals(attrName)) {
                countryInfo.mDefaultPlmnNITZ = "true".equals(parser.getAttributeValue(i));
            }
        }
        boolean exit = false;
        try {
            int eventType = parser.next();
            while (eventType != 1) {
                if (eventType == 2) {
                    VLog.d(TAG, "parseCountry tag:" + parser.getName());
                    if ("plmn".equals(parser.getName()) && !parsePlmn(parser, countryInfo.mPlmnInfo)) {
                        throw new Exception("parse plmn error!");
                    }
                } else if (eventType == 3 && "country".equals(parser.getName())) {
                    exit = true;
                }
                eventType = parser.next();
            }
            return countryInfo;
        } catch (Exception e) {
            VLog.d(TAG, "-----error:parseCountry " + e.toString());
            return null;
        }
    }

    private boolean parsePlmn(XmlPullParser parser, HashMap<String, Boolean> plmnInfo) {
        if (parser.getAttributeCount() == 2) {
            String n = parser.getAttributeName(0);
            if ("plmnID".equals(n)) {
                String plmnID = parser.getAttributeValue(0);
                String n2 = parser.getAttributeName(1);
                if ("enableNITZ".equals(n2)) {
                    String enableNITZ = parser.getAttributeValue(1);
                    VLog.d(TAG, "enableNITZ:" + enableNITZ);
                    plmnInfo.put(plmnID, Boolean.valueOf("true".equals(enableNITZ)));
                    return true;
                }
            }
        }
        return false;
    }

    private void dumpTimeZoneConfig() {
        Set<String> keys = this.mTimeZoneConfig.keySet();
        for (String key : keys) {
            CountryInfo countryInfo = this.mTimeZoneConfig.get(key);
            VLog.d(TAG, "------------country:" + countryInfo.mCountryCode + " enableNITZ:" + countryInfo.mEnableNITZ);
            for (String plmnKey : countryInfo.mPlmnInfo.keySet()) {
                VLog.d(TAG, "enableNITZ:" + countryInfo.mPlmnInfo.get(plmnKey));
            }
        }
        VLog.d(TAG, "------------mTimeZoneOfflineEnable:" + this.mTimeZoneOfflineEnable);
    }
}