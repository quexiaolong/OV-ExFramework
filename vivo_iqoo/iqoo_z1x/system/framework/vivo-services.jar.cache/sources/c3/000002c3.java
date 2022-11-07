package com.android.server.location;

import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.ILocationListener;
import android.location.Location;
import android.location.LocationRequest;
import android.location.VIVORsaForLocation;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.WorkSource;
import android.provider.Settings;
import com.android.internal.location.ProviderRequest;
import com.android.internal.util.ArrayUtils;
import com.android.server.am.VivoFrozenPackageSupervisor;
import com.android.server.display.VivoDisplayPowerControllerImpl;
import com.android.server.location.LocationManagerService;
import com.android.server.location.VivoMockLocationRecoveryNotify;
import com.android.server.policy.VivoPolicyConstant;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import com.vivo.services.security.client.VivoPermissionManager;
import com.vivo.services.security.server.VivoPermissionUtils;
import com.vivo.services.superresolution.Constant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoLocationManagerServiceExt implements IVivoLocationManagerService {
    private static final String ANDROID_LOCATION_CTS = "android.location.cts";
    private static final String COM_BAIDU_MAP = "com.baidu.BaiduMap";
    private static final String COM_GAODE_MAP = "com.autonavi.minimap";
    private static final int CUR_MODEL = 100;
    private static final long LOG_CACHE_INTERVAL = 30000;
    private static final int MAX_GPS_LISTENER = 10;
    private static final int MSG_NLP_ALWAY_OPEN_OPTIMIZE_INIT = 1000;
    private static final int MSG_NLP_ALWAY_OPEN_OPTIMIZE_NETWORK_DISABLE_CALL_3THRPART_APP = 1001;
    private static final int MSG_START_GNSS_LOG = 101;
    private static final int MSG_STOP_GNSS_LOG = 102;
    private static final int PKG_TYPE_IS_WHITE_PKG = 2;
    private static final int PKG_TYPE_UNKNOW = -1;
    private static final String TAG = "VivoLocationManagerServiceExt";
    private Context mContext;
    private DevicePolicyManager mDevicePolicyManager;
    private LocationManagerService mLocationManagerService;
    private final Object mLock;
    private PackageManager mPackageManager;
    private HashMap<LocationManagerService.Receiver, Integer> mPkgBelongToWho;
    private HashMap<Object, LocationManagerService.Receiver> mReceivers;
    private VivoLocationAppFilter mVivoBlacklist;
    private VivoLocConf mVivoConfig;
    private VivoLocationFeatureConfig mVivoLocationFeatureConfig;
    private VivoLocationNotify mVivoLocationNotify;
    VivoLocationManagerServiceUtils mVivoLocationUtils;
    private VivoMockLocationRecoveryNotify mVivoMockLocationRecoveryNotify;
    private VivoNlpPowerMonitor mVivoNlpPowerMonitor;
    private VivoRequestRecordData mVivoRequestData;
    public static boolean D = false;
    private static final boolean ISOverseas = "yes".equals(SystemProperties.get("ro.vivo.product.overseas", "no"));
    static final String[] PACKAGE_FILTER_LIST = {"com.vivo.dynamic.location.auto", "com.vivo.easyshare", "com.sie.mp", "com.vivo.weather", "com.vivo.weather.provider", "com.bbk.VoiceAssistant", "com.vivo.daemonService", "com.qualcomm.location", "com.vivo.compass", "com.android.camera", "com.vivo.abe", "com.vivo.browser", VivoPermissionUtils.OS_PKG, "com.vivo.assistant", "com.tencent.android.location", "com.amap.android.location", "com.baidu.map.location"};
    public static HashMap<String, ProviderRequest> mProviderRequests = new HashMap<>();
    private static boolean bReadyForSave = false;
    private static boolean shouldSave = false;
    private final HashMap<Object, LocationManagerService.Receiver> mFrozenReceivers = new HashMap<>();
    private final HashMap<String, ArrayList<LocationManagerService.Receiver>> mGpsRequestMap = new HashMap<>();
    private boolean mNLPSwitchISEnable = true;
    private ArrayList<String> mMapRequestList = new ArrayList<>();
    ArrayList<String> mCustomPkgs = new ArrayList<>();
    private VivoLocationDiagnosticManager mDiagnoster = VivoLocationDiagnosticManager.getInstance();
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.location.VivoLocationManagerServiceExt.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            VLog.d(VivoLocationManagerServiceExt.TAG, "receive broadcast intent, action: " + action);
            if (action != null && action.equals(VivoPolicyConstant.ACTION_VIVO_LOG_CHANGED)) {
                String adbStatus = intent.getStringExtra("adblog_status");
                VLog.d(VivoLocationManagerServiceExt.TAG, "mBroadcastReceiver adb:" + adbStatus);
                if ("on".equals(adbStatus)) {
                    LocationManagerService.D = true;
                    VivoLocationManagerServiceExt.D = true;
                    VivoNetworkLocationData.DEBUG = true;
                    VivoRequestRecordData.DEBUG = true;
                    VivoLocConf.D = true;
                    VivoCn0WeakManager.DEBUG = true;
                    VivoLocationFeatureConfig.DBG = true;
                    VivoLocationDiagnosticManager.D = true;
                    return;
                }
                VivoLocationManagerServiceExt.D = VLog.isLoggable(VivoLocationManagerServiceExt.TAG, 3);
                LocationManagerService.D = VLog.isLoggable(VivoLocationManagerServiceExt.TAG, 3);
                VivoNetworkLocationData.DEBUG = VLog.isLoggable(VivoLocationManagerServiceExt.TAG, 3);
                VivoRequestRecordData.DEBUG = VLog.isLoggable(VivoLocationManagerServiceExt.TAG, 3);
                VivoLocConf.D = VLog.isLoggable(VivoLocationManagerServiceExt.TAG, 3);
                VivoCn0WeakManager.DEBUG = VLog.isLoggable(VivoLocationManagerServiceExt.TAG, 3);
                VivoLocationFeatureConfig.DBG = VLog.isLoggable(VivoLocationManagerServiceExt.TAG, 3);
                VivoLocationDiagnosticManager.D = VLog.isLoggable(VivoLocationManagerServiceExt.TAG, 3);
            }
        }
    };
    private boolean bReadyForOff = false;
    private ProviderRequest.Builder providerRequestOff = new ProviderRequest.Builder();
    private boolean mIsWeixinRequestGps = false;
    private LocationWorkerHandler mLocationHandler = new LocationWorkerHandler(VivoLocThread.getInstance().getLocationServiceLooper());

    public VivoLocationManagerServiceExt(final Context context, boolean D2, LocationManagerService locationManagerService, Object lock, HashMap<Object, LocationManagerService.Receiver> receivers) {
        this.mReceivers = new HashMap<>();
        this.mContext = null;
        this.mDevicePolicyManager = null;
        this.mVivoLocationFeatureConfig = null;
        this.mVivoBlacklist = null;
        this.mVivoConfig = null;
        this.mLocationManagerService = locationManagerService;
        this.mReceivers = receivers;
        this.mLock = lock;
        this.mContext = context;
        VivoLocConf vivoLocConf = VivoLocConf.getInstance();
        this.mVivoConfig = vivoLocConf;
        vivoLocConf.init(this.mContext, this.mLocationHandler);
        this.mDiagnoster.init(this.mContext);
        this.mVivoLocationNotify = new VivoLocationNotify(context, D2);
        this.mVivoMockLocationRecoveryNotify = new VivoMockLocationRecoveryNotify(context, new LocationManagerServiceExtCallbackImpl(), D2);
        this.mVivoLocationFeatureConfig = VivoLocationFeatureConfig.getInstance();
        this.mVivoBlacklist = new VivoLocationAppFilter();
        this.mVivoLocationUtils = new VivoLocationManagerServiceUtils(context, this);
        this.mLocationHandler.post(new Runnable() { // from class: com.android.server.location.VivoLocationManagerServiceExt.2
            @Override // java.lang.Runnable
            public void run() {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(VivoPolicyConstant.ACTION_VIVO_LOG_CHANGED);
                context.registerReceiverAsUser(VivoLocationManagerServiceExt.this.mBroadcastReceiver, UserHandle.ALL, intentFilter, null, VivoLocationManagerServiceExt.this.mLocationHandler);
            }
        });
        this.mPackageManager = this.mContext.getPackageManager();
        this.mVivoRequestData = new VivoRequestRecordData(context);
        this.mDevicePolicyManager = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        VIVORsaForLocation vivoRsaForLocation = VIVORsaForLocation.getInstance();
        vivoRsaForLocation.setEnvironment(this.mLocationHandler, this.mContext);
    }

    public boolean applyRequirementsLocked(String provider, ArrayList<LocationManagerService.UpdateRecord> records, WorkSource worksource, boolean reportlocation) {
        VivoNlpPowerMonitor vivoNlpPowerMonitor;
        if (D) {
            StringBuilder sb = new StringBuilder("Active Record for " + provider + " provider:\n");
            if (records != null) {
                Iterator<LocationManagerService.UpdateRecord> it = records.iterator();
                while (it.hasNext()) {
                    LocationManagerService.UpdateRecord record = it.next();
                    sb.append("UpdateRecord[");
                    sb.append(record.mProvider);
                    sb.append(" ");
                    sb.append(record.mReceiver.mCallerIdentity);
                    sb.append(" ");
                    if (!record.mIsForegroundUid) {
                        sb.append("(background) ");
                    }
                    sb.append(record.mRealRequest);
                    sb.append(" ");
                    sb.append(record.mReceiver.mWorkSource);
                    sb.append("]\n");
                }
            }
            VLog.d(TAG, sb.toString());
        }
        if (provider.equals("gps") && VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_WEIXIN_OPTIMIZE)) {
            updateWeixinRequestGpsStatus(provider, records);
        }
        if (provider.equals("network") && (vivoNlpPowerMonitor = this.mVivoNlpPowerMonitor) != null && reportlocation) {
            if (!vivoNlpPowerMonitor.mIsNetworkAvailable) {
                VLog.d(TAG, "VNPM: is no net mode , skip request: ");
                return false;
            }
            boolean inNlpSaveWhiteList = false;
            String packageName = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            for (int i = 0; i < worksource.size() && !inNlpSaveWhiteList; i++) {
                packageName = worksource.getPackageName(i);
                inNlpSaveWhiteList = this.mVivoNlpPowerMonitor.inNlpSaveWhiteList(packageName);
            }
            VLog.d(TAG, "VNPM: in NlpSaveWhiteList: " + inNlpSaveWhiteList + ", package name: " + packageName);
            if (this.mVivoNlpPowerMonitor.bPowerSaveMode && this.mVivoNlpPowerMonitor.mLastLocation != null) {
                if (!inNlpSaveWhiteList) {
                    VLog.d(TAG, "VNPM: is running , skip request and report directly");
                    this.mVivoNlpPowerMonitor.mLastLocation.setTime(System.currentTimeMillis());
                    this.mLocationManagerService.getLocationProviderManager("network").onReportLocation(this.mVivoNlpPowerMonitor.mLastLocation);
                    shouldSave = true;
                } else {
                    shouldSave = false;
                }
            }
            VLog.d(TAG, "VNPM: Ready For Off: " + this.bReadyForOff + ", Should Save: " + shouldSave + ", Ready For Save: " + bReadyForSave);
            if (!this.bReadyForOff && shouldSave && bReadyForSave) {
                vivoSetRequestOffForSaveLock();
                this.bReadyForOff = true;
                return false;
            } else if (this.bReadyForOff && shouldSave && bReadyForSave) {
                return false;
            } else {
                this.bReadyForOff = false;
            }
        }
        return true;
    }

    public List<Address> getFromLocationCheck(double latitude, double longitude, Locale locale) {
        List<Address> temp;
        VivoNlpPowerMonitor vivoNlpPowerMonitor = this.mVivoNlpPowerMonitor;
        if (vivoNlpPowerMonitor == null || (temp = vivoNlpPowerMonitor.checkDistanceForGeocoder(latitude, longitude, locale)) == null || temp.size() <= 0) {
            return null;
        }
        VLog.d(TAG, "VNPM: report RGC");
        return temp;
    }

    public void getFromLocationUpdate(double latitude, double longitude, List<Address> addrs, Locale locale) {
        if (this.mVivoNlpPowerMonitor != null && addrs != null && addrs.size() > 0) {
            Location loc = new Location("network");
            loc.setLatitude(latitude);
            loc.setLongitude(longitude);
            this.mVivoNlpPowerMonitor.updateRgcNode(loc, addrs, locale);
        }
    }

    public void notifyLocationOff(String packageName, String name) {
        VivoLocationDiagnosticManager vivoLocationDiagnosticManager = this.mDiagnoster;
        vivoLocationDiagnosticManager.Log("notifyLocationOff, package:" + packageName + " provider:" + name);
        if (name.equals("gps") && VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_LOCATIION_NOTIFY)) {
            this.mVivoLocationNotify.requestLocation(packageName);
        }
    }

    public void removeFrozenReceiverLocked(LocationManagerService.Receiver receiver) {
        if (VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_PEM_FROZEN)) {
            this.mFrozenReceivers.remove(receiver.mKey);
        }
    }

    public int onFrozenPackage(String packageName, int uid, boolean isFrozen) {
        if (VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_PEM_FROZEN)) {
            if (packageName == null || uid < 10000) {
                VSlog.w(TAG, "Frozen GPS fail,reason: packageName or uid is invalid!");
                return 0;
            }
            int count = 0;
            synchronized (this.mLock) {
                if (isFrozen) {
                    ArrayList<LocationManagerService.Receiver> deadReceivers = null;
                    for (Object key : this.mReceivers.keySet()) {
                        LocationManagerService.Receiver receiver = this.mReceivers.get(key);
                        if (isFrozenPackage(receiver, packageName, uid) || isWorkSourceFrozenPackage(receiver, packageName, uid)) {
                            if (deadReceivers == null) {
                                deadReceivers = new ArrayList<>();
                            }
                            deadReceivers.add(receiver);
                            if (isFrozenPackage(receiver, packageName, uid)) {
                                this.mFrozenReceivers.put(key, receiver);
                            }
                            count++;
                        }
                    }
                    if (deadReceivers != null) {
                        Iterator<LocationManagerService.Receiver> it = deadReceivers.iterator();
                        while (it.hasNext()) {
                            LocationManagerService.Receiver receiver2 = it.next();
                            if (D) {
                                VLog.i(TAG, "Frozen GPS remove " + receiver2.mCallerIdentity.packageName + " - " + Integer.toHexString(System.identityHashCode(receiver2)));
                            }
                            this.mLocationManagerService.removeUpdatesLocked(receiver2);
                        }
                    }
                } else {
                    HashMap<Object, LocationManagerService.Receiver> addReceivers = new HashMap<>();
                    for (Object key2 : this.mFrozenReceivers.keySet()) {
                        LocationManagerService.Receiver receiver3 = this.mFrozenReceivers.get(key2);
                        if (isFrozenPackage(receiver3, packageName, uid)) {
                            addReceivers.put(key2, receiver3);
                        }
                    }
                    for (Object key3 : addReceivers.keySet()) {
                        this.mFrozenReceivers.remove(key3);
                        LocationManagerService.Receiver receiver4 = addReceivers.get(key3);
                        if (receiver4.isListener()) {
                            try {
                                receiver4.getListener().asBinder().linkToDeath(receiver4, 0);
                            } catch (RemoteException e) {
                                VSlog.e(TAG, "linkToDeath failed:", e);
                            }
                        }
                        if (D) {
                            VLog.i(TAG, "Frozen GPS add " + receiver4.mCallerIdentity.packageName + " - " + Integer.toHexString(System.identityHashCode(receiver4)));
                        }
                        this.mReceivers.put(key3, receiver4);
                        Iterator it2 = receiver4.mUpdateRecords.values().iterator();
                        while (true) {
                            if (!it2.hasNext()) {
                                break;
                            }
                            LocationManagerService.UpdateRecord updateRecord = (LocationManagerService.UpdateRecord) it2.next();
                            if (updateRecord.mProvider != null) {
                                LocationRequest sanitizedRequest = updateRecord.mRealRequest;
                                if (D) {
                                    VLog.i(TAG, "Frozen requestLocationUpdatesLocked " + receiver4.mCallerIdentity.packageName + " - " + sanitizedRequest);
                                }
                                this.mLocationManagerService.requestLocationUpdatesLocked(sanitizedRequest, receiver4);
                            }
                        }
                        count++;
                    }
                }
            }
            return count;
        }
        return 0;
    }

    private boolean isFrozenPackage(LocationManagerService.Receiver receiver, String packageName, int uid) {
        return packageName != null && packageName.equals(receiver.mCallerIdentity.packageName) && uid == receiver.mCallerIdentity.uid;
    }

    private boolean isWorkSourceFrozenPackage(LocationManagerService.Receiver receiver, String packageName, int uid) {
        return "com.google.android.gms".equals(receiver.mCallerIdentity.packageName) && packageName != null && receiver.mWorkSource != null && receiver.mWorkSource.size() == 1 && (receiver.mWorkSource.getUid(0) == uid || packageName.equals(receiver.mWorkSource.getPackageName(0)));
    }

    public boolean iskeepFrozen(LocationManagerService.Receiver receiver) {
        if (receiver != null && receiver.mCallerIdentity != null && receiver.mCallerIdentity.packageName != null) {
            VivoFrozenPackageSupervisor vfps = VivoFrozenPackageSupervisor.getInstance();
            if (vfps.isFrozenPackage(receiver.mCallerIdentity.packageName, receiver.mCallerIdentity.uid)) {
                VSlog.d(TAG, "skipping loc update for Frozen app: " + receiver.mCallerIdentity.packageName);
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean shouldSkipLocationReport(Location location, LocationManagerService.Receiver receiver, String providerName) {
        if (isTestAppRunning(location, receiver, providerName) && VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_TEST_APP_CHECK)) {
            return true;
        }
        if (iskeepFrozen(receiver) && VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_PEM_FROZEN)) {
            return true;
        }
        this.mDiagnoster.reportLocation(providerName, location);
        return false;
    }

    public void addRequest(LocationRequest request, LocationManagerService.Receiver recevier, String packageName, int uid) {
        if (this.mVivoLocationUtils.getControlGpsListeners() && VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_LISTENER_CONTROL) && "gps".equals(request.getProvider())) {
            int gpsListener = checkGpsRequestPermission(packageName, uid);
            String packageAndUid = packageName + uid;
            if (gpsListener >= 10) {
                removeFirstRequest(packageAndUid);
            }
            addGpsRequest(packageAndUid, recevier);
        }
        this.mDiagnoster.Log("addRequest:" + request + " Receiver:" + recevier + " packageName:" + packageName);
    }

    public boolean isBlakcListedAppCalling(String packageName, String providerName) {
        if ("gps".equals(providerName) && VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_BLACKLIST_APP) && this.mVivoBlacklist.isInBlacklist(packageName)) {
            if (D) {
                VLog.d(TAG, "BlackListApp :" + packageName + " wake gps failed");
                return true;
            }
            return true;
        }
        return false;
    }

    public Handler getVivoThreadHandler() {
        if (VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_INDEPENDENT_THREAD)) {
            return this.mLocationHandler;
        }
        return null;
    }

    public boolean checkVivoLocationPermission() {
        if (VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_VIVO_PERMISSION)) {
            return VivoPermissionManager.checkCallingVivoPermission("android.permission.ACCESS_FINE_LOCATION");
        }
        return true;
    }

    public void vivoPreReportAppRequestLocation(String packageName, String provider, int uid) {
        if (VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_BIG_DATA)) {
            this.mVivoLocationUtils.vivoPreReportAppRequestLocation(packageName, provider, uid);
        }
    }

    public void initializeLocked() {
        if (VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_NLP_SWITCH)) {
            Message msg = this.mLocationHandler.obtainMessage(1000);
            this.mLocationHandler.sendMessage(msg);
        }
        this.mVivoNlpPowerMonitor = new VivoNlpPowerMonitor(this.mContext, VivoLocThread.getInstance().getLocationServiceLooper(), this.mLock);
    }

    public void updateCustomMachineUseGpsFrequency(String name, String packageName) {
        DevicePolicyManager devicePolicyManager = this.mDevicePolicyManager;
        if (devicePolicyManager != null && devicePolicyManager.getCustomType() > 0) {
            ArrayList<String> arrayList = (ArrayList) this.mDevicePolicyManager.getCustomPkgs();
            this.mCustomPkgs = arrayList;
            if (arrayList != null && packageName != null && arrayList.contains(packageName)) {
                if (D) {
                    VLog.d(TAG, "size: " + this.mCustomPkgs.size() + " reportExceptionInfo packageName: " + packageName);
                }
                Bundle data = new Bundle();
                data.putString("package_name", packageName);
                this.mDevicePolicyManager.reportExceptionInfo(VivoDisplayPowerControllerImpl.COLOR_FADE_ANIMATION_VERTICAL, data);
            }
        } else if (D) {
            VLog.d(TAG, "DevicePolicyManager == null || mDevicePolicyManager.getCustomType <= 0 ");
        }
    }

    public boolean inBackgroundThrottlePackageWhitelist(String packageName) {
        if (VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_LOCATION_CONFIG)) {
            return this.mVivoLocationUtils.getBackgroundWhiteList().contains(packageName);
        }
        return false;
    }

    public boolean requestLocationUpdates(String packageName, LocationRequest request, ILocationListener listener, PendingIntent intent, String providerName, int uid) {
        if (isBlakcListedAppCalling(packageName, providerName)) {
            return false;
        }
        if (VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_BLACKLIST_APP)) {
            return isDisabledAndWhiteListAppCalling(packageName, request, listener, intent, providerName, uid);
        }
        return true;
    }

    public void requestLocationUpdatesLocked(LocationRequest request, LocationManagerService.Receiver receiver, LocationManagerService.UpdateRecord record) {
        String packageName = receiver.mCallerIdentity.packageName;
        int uid = receiver.mCallerIdentity.uid;
        String providerName = request.getProvider();
        if (D) {
            StringBuilder sb = new StringBuilder();
            sb.append("request ");
            sb.append(Integer.toHexString(System.identityHashCode(receiver)));
            sb.append(" ");
            sb.append(providerName);
            sb.append(" ");
            sb.append(request);
            sb.append(" from ");
            sb.append(packageName);
            sb.append("(");
            sb.append(uid);
            sb.append(" ");
            sb.append(record.mIsForegroundUid ? "foreground" : "background");
            sb.append(this.mLocationManagerService.isThrottlingExempt(receiver.mCallerIdentity) ? " [whitelisted]" : Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
            sb.append(")");
            VLog.d(TAG, sb.toString());
        }
        if (ISOverseas) {
            return;
        }
        if (isNLPOptimizeCloudSwitchOpen() && receiver != null && !isSpecialProject() && VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_BLACKLIST_APP) && providerName != null && providerName.equals("network")) {
            boolean isCallingPkgInWhileList = isCallPkgInWhiteList(packageName, providerName, uid);
            if (isCallingPkgInWhileList) {
                receiver.mPkgBelongToWho = 2;
            } else {
                receiver.mPkgBelongToWho = -1;
            }
        }
        if ((packageName.equals(COM_BAIDU_MAP) || packageName.equals(COM_GAODE_MAP)) && providerName.equals("gps") && VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_DYNAMIC_LOG)) {
            this.mLocationHandler.sendEmptyMessage(101);
            synchronized (this.mLock) {
                this.mMapRequestList.add(Integer.toHexString(System.identityHashCode(receiver)));
            }
            VLog.d(TAG, "dynamic log add receiver : " + Integer.toHexString(System.identityHashCode(receiver)));
            VLog.d(TAG, "package: " + packageName + " request GPS location and we start collecting Gnss dynamic debug log");
        }
        ArrayList<LocationManagerService.UpdateRecord> networkRecord = (ArrayList) this.mLocationManagerService.mRecordsByProvider.get("network");
        if (VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_BIG_DATA) && providerName.equals("network") && networkRecord != null && networkRecord.size() == 1) {
            this.mVivoRequestData.requestLocationUpdatesLocked(packageName);
        }
        VivoLocationDiagnosticManager vivoLocationDiagnosticManager = this.mDiagnoster;
        vivoLocationDiagnosticManager.Log("requestLocationUpdatesLocked:" + receiver + " provider:" + providerName + " package:" + packageName);
    }

    public boolean handleLocationChangedLocked(String provider, LocationManagerService.Receiver receiver) {
        int pkgType;
        if (provider.equals("network") && VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_BIG_DATA)) {
            this.mVivoRequestData.handleLocationChangedLocked();
        }
        boolean isNlpOptimizeCloudOpen = isNLPOptimizeCloudSwitchOpen();
        if (!isNlpOptimizeCloudOpen || isSpecialProject() || this.mNLPSwitchISEnable || !provider.equals("network") || !VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_NLP_SWITCH) || (pkgType = receiver.mPkgBelongToWho) == 2 || pkgType != -1) {
            return true;
        }
        return false;
    }

    public boolean onUseableChangedLocked(boolean useableIgnoringAllowed, boolean mAllowed) {
        boolean isEnabled = mAllowed;
        if (isNLPOptimizeCloudSwitchOpen() && !isSpecialProject() && VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_NLP_SWITCH)) {
            if (!this.mNLPSwitchISEnable && isEnabled && useableIgnoringAllowed) {
                isEnabled = false;
            }
            if (isEnabled && !useableIgnoringAllowed) {
                this.mNLPSwitchISEnable = false;
                if (D) {
                    VLog.w(TAG, "## Do not XX ");
                }
                this.mLocationHandler.removeMessages(1001);
                Message msg = this.mLocationHandler.obtainMessage(1001);
                this.mLocationHandler.sendMessage(msg);
                return false;
            } else if (!isEnabled && useableIgnoringAllowed) {
                this.mNLPSwitchISEnable = true;
                if (D) {
                    VLog.i(TAG, "##### sys Enabel ");
                }
                return true;
            }
        }
        return false;
    }

    public boolean isProviderEnabledForUser(int uid) {
        if (isNLPOptimizeCloudSwitchOpen() && !isSpecialProject() && !this.mNLPSwitchISEnable && VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_NLP_SWITCH)) {
            try {
                String[] pkgList = this.mPackageManager.getPackagesForUid(uid);
                if (pkgList != null && pkgList.length > 0) {
                    for (String pkgtemp : pkgList) {
                        if (isCallPkgInWhiteList(pkgtemp, "network", uid)) {
                            if (D) {
                                VLog.d(TAG, "isProviderEnabled callingUid:" + uid + "  " + pkgtemp);
                                return true;
                            } else {
                                return true;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (D) {
            VLog.d(TAG, "isProviderEnabled callingUid:" + uid + " false");
        }
        return false;
    }

    public boolean isSameRequest(String name, ProviderRequest request) {
        if (VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_FILT_SAME_REQUEST)) {
            if (D) {
                VLog.d(TAG, "new request:" + request + " old request:" + mProviderRequests.get(name));
            }
            if (request == mProviderRequests.get(name)) {
                return true;
            }
            mProviderRequests.put(name, request);
        }
        if ("network".equals(name) && request != null) {
            this.mDiagnoster.startNetworkLocating(request.reportLocation);
            return false;
        }
        return false;
    }

    public boolean isDoubleInstanceEnable() {
        return false;
    }

    public void removeUpdatesLocked(LocationManagerService.Receiver receiver) {
        String receiverCode = Integer.toHexString(System.identityHashCode(receiver));
        if (this.mMapRequestList.contains(receiverCode) && VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_DYNAMIC_LOG)) {
            VLog.d(TAG, "dynamic log list size = " + this.mMapRequestList.size() + " delete receiver :" + receiverCode);
            this.mMapRequestList.remove(receiverCode);
            StringBuilder sb = new StringBuilder();
            sb.append("dynamic log list size = ");
            sb.append(this.mMapRequestList.size());
            VLog.d(TAG, sb.toString());
            if (this.mMapRequestList.isEmpty()) {
                this.mLocationHandler.sendEmptyMessageDelayed(102, 30000L);
                VLog.d(TAG, "reveiver: " + receiverCode + " removed and we stop collecting Gnss debug log after 30s");
            }
        }
        if (this.mVivoLocationUtils.getControlGpsListeners() && VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_LISTENER_CONTROL)) {
            removeRequest(receiver);
        }
        VivoLocationDiagnosticManager vivoLocationDiagnosticManager = this.mDiagnoster;
        vivoLocationDiagnosticManager.Log("removeUpdatesLocked:" + receiver);
    }

    public void getFromLocation(String packageName, boolean succ) {
        if (VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_BIG_DATA)) {
            this.mVivoRequestData.getFromLocation(packageName, succ);
        }
    }

    public void getFromLocationName(String packageName, boolean succ, String location) {
        if (VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_BIG_DATA)) {
            this.mVivoRequestData.getFromLocationName(packageName, succ, location);
        }
    }

    public boolean checkApiAllowed(String api, String packageName) {
        if (VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_API_CONTROL) && this.mVivoLocationFeatureConfig.getApiControlStatus()) {
            if (packageName == null || packageName.isEmpty()) {
                packageName = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
            }
            boolean allow = this.mVivoLocationFeatureConfig.getApiAllowedState(api, packageName);
            boolean isCts = isCtsApp();
            VLog.d(TAG, "LocApi " + api + " called by " + packageName + ", allow:" + allow + " isCts:" + isCts);
            return allow || isCts;
        }
        return true;
    }

    public void loopLog(String log, boolean logcat) {
        if (logcat) {
            VLog.d(TAG, log);
        }
        this.mDiagnoster.Log(log);
    }

    private void vivoSetRequestOffForSaveLock() {
        synchronized (this.mLock) {
            LocationManagerService.LocationProviderManager p = this.mLocationManagerService.getLocationProviderManager("network");
            if (p == null) {
                return;
            }
            ProviderRequest pr = this.providerRequestOff.build();
            p.setRequest(pr);
            VLog.d(TAG, "VNPM: Set Request Off For Save");
        }
    }

    public static void setPowerSaveStatus(boolean status) {
        bReadyForSave = status;
        if (!status) {
            shouldSave = false;
        }
        VLog.d(TAG, "VNPM: Should Save: " + shouldSave + ", Ready For Save: " + bReadyForSave);
    }

    /* loaded from: classes.dex */
    private class ProviderRequestRecord {
        ProviderRequest mRequest;
        WorkSource mWorkSource;

        public ProviderRequestRecord(ProviderRequest request, WorkSource workSource) {
            this.mRequest = request;
            this.mWorkSource = workSource;
        }

        public boolean equals(ProviderRequestRecord record) {
            if (record != null && this.mRequest.reportLocation == record.mRequest.reportLocation && this.mRequest.interval == record.mRequest.interval && this.mRequest.lowPowerMode == record.mRequest.lowPowerMode && this.mRequest.locationSettingsIgnored == record.mRequest.locationSettingsIgnored && this.mWorkSource.equals(record.mWorkSource)) {
                return true;
            }
            return false;
        }
    }

    /* loaded from: classes.dex */
    private class LocationWorkerHandler extends Handler {
        public LocationWorkerHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (VivoLocationManagerServiceExt.D) {
                VLog.i(VivoLocationManagerServiceExt.TAG, "--handleMessage " + msg.what);
            }
            int i = msg.what;
            if (i == 101) {
                VivoLocationManagerServiceExt.this.mLocationHandler.removeMessages(102);
                Intent gnssStartIntent = new Intent("android.vivo.bbklog.action.GnssLogChanged");
                gnssStartIntent.putExtra("flag", 1);
                gnssStartIntent.setPackage("com.android.bbklog");
                VivoLocationManagerServiceExt.this.mContext.sendBroadcast(gnssStartIntent);
            } else if (i == 102) {
                Intent gnssStopIntent = new Intent("android.vivo.bbklog.action.GnssLogChanged");
                gnssStopIntent.putExtra("flag", 0);
                gnssStopIntent.setPackage("com.android.bbklog");
                VivoLocationManagerServiceExt.this.mContext.sendBroadcast(gnssStopIntent);
                VLog.d(VivoLocationManagerServiceExt.TAG, "dynamic log :After 30s and Gnss log will be closed now");
            } else if (i != 1000) {
                if (i == 1001) {
                    try {
                        synchronized (VivoLocationManagerServiceExt.this.mLock) {
                            VivoLocationManagerServiceExt.this.tellAppSysNetworkProviderIsDisableLocked("network", false);
                        }
                    } catch (Exception e) {
                        if (VivoLocationManagerServiceExt.D) {
                            VLog.e(VivoLocationManagerServiceExt.TAG, "tell app network provider disable catch exception!");
                        }
                    }
                }
            } else {
                String nlpStateStr = Settings.Secure.getString(VivoLocationManagerServiceExt.this.mContext.getContentResolver(), "location_providers_allowed");
                synchronized (VivoLocationManagerServiceExt.this.mLock) {
                    if (nlpStateStr == null) {
                        VivoLocationManagerServiceExt.this.mNLPSwitchISEnable = false;
                    } else if (nlpStateStr.contains("network")) {
                        VivoLocationManagerServiceExt.this.mNLPSwitchISEnable = true;
                    } else {
                        VivoLocationManagerServiceExt.this.mNLPSwitchISEnable = false;
                    }
                }
                if (VivoLocationManagerServiceExt.D) {
                    VLog.d(VivoLocationManagerServiceExt.TAG, "MSG_SettingProvider nlpStateStr:" + nlpStateStr + " mNLPSwitchISEnable:" + VivoLocationManagerServiceExt.this.mNLPSwitchISEnable);
                }
            }
        }
    }

    public boolean isTestAppRunning(Location location, LocationManagerService.Receiver receiver, String providerName) {
        String mMockCheckString = " " + location;
        if (receiver.mCallerIdentity.packageName.equals(ANDROID_LOCATION_CTS) && providerName.equals("gps") && !mMockCheckString.contains("mock")) {
            if (location.hasAltitude() && location.hasSpeed() && location.hasVerticalAccuracy() && location.hasSpeedAccuracy()) {
                return false;
            }
            if (D) {
                VLog.d(TAG, "ctsTest: failed: " + location);
                return true;
            }
            return true;
        }
        return false;
    }

    private int checkGpsRequestPermission(String packageName, int uid) {
        String packageAndUid = packageName + uid;
        if (D) {
            VLog.d(TAG, "checkGpsRequestPermission" + this.mGpsRequestMap.toString());
        }
        ArrayList<LocationManagerService.Receiver> listenerList = this.mGpsRequestMap.get(packageAndUid);
        if (listenerList != null) {
            return this.mGpsRequestMap.get(packageAndUid).size();
        }
        return 0;
    }

    private void removeFirstRequest(String packageAndUid) {
        ArrayList<LocationManagerService.Receiver> receiverList = this.mGpsRequestMap.get(packageAndUid);
        if (receiverList != null && receiverList.size() > 0) {
            LocationManagerService.Receiver receiver = receiverList.get(0);
            Iterator<LocationManagerService.Receiver> iter = this.mGpsRequestMap.get(packageAndUid).iterator();
            while (iter.hasNext()) {
                if (iter.next().equals(receiver)) {
                    iter.remove();
                }
            }
            this.mLocationManagerService.removeUpdatesLocked(receiver);
        }
    }

    private void addGpsRequest(String packageAndUid, LocationManagerService.Receiver receiver) {
        ArrayList<LocationManagerService.Receiver> receiverList = this.mGpsRequestMap.get(packageAndUid);
        if (receiverList == null) {
            receiverList = new ArrayList<>();
        }
        if (!receiverList.contains(receiver)) {
            receiverList.add(receiver);
            this.mGpsRequestMap.put(packageAndUid, receiverList);
        }
    }

    private void removeRequest(LocationManagerService.Receiver receiver) {
        String packageAndUid = receiver.mCallerIdentity.packageName + receiver.mCallerIdentity.uid;
        if (this.mGpsRequestMap.get(packageAndUid) == null) {
            return;
        }
        Iterator<LocationManagerService.Receiver> iter = this.mGpsRequestMap.get(packageAndUid).iterator();
        while (iter.hasNext()) {
            if (iter.next().equals(receiver)) {
                iter.remove();
            }
        }
    }

    public void updateWeixinRequestGpsStatus(String provider, ArrayList<LocationManagerService.UpdateRecord> records) {
        if (provider == null || !provider.equals("gps")) {
            return;
        }
        boolean isWeixinRequestGps = false;
        try {
            Iterator<LocationManagerService.UpdateRecord> it = records.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                LocationManagerService.UpdateRecord record = it.next();
                LocationManagerService.Receiver receiver = record.mReceiver;
                if (receiver.mCallerIdentity.packageName != null && receiver.mCallerIdentity.packageName.equals(Constant.APP_WEIXIN)) {
                    LocationRequest locationRequest = record.mRealRequest;
                    long interval = locationRequest.getInterval();
                    if (interval < Long.MAX_VALUE) {
                        isWeixinRequestGps = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
        }
        long token = Binder.clearCallingIdentity();
        try {
            try {
                String whiteList = this.mVivoLocationFeatureConfig.getWhiteListAppForFakeLocState();
                if (whiteList != null) {
                    if (this.mIsWeixinRequestGps == isWeixinRequestGps) {
                        return;
                    }
                    this.mIsWeixinRequestGps = isWeixinRequestGps;
                    if (isWeixinRequestGps) {
                        Settings.Global.putInt(this.mContext.getContentResolver(), "vivo_requesting_gps", 1);
                        if (D) {
                            VLog.d(TAG, "isRequestingGps true");
                        }
                    } else {
                        Settings.Global.putInt(this.mContext.getContentResolver(), "vivo_requesting_gps", 0);
                        if (D) {
                            VLog.d(TAG, "isRequestingGps false");
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        } catch (Exception e2) {
            VLog.d(TAG, "Write settings  error : " + e2);
        }
    }

    private boolean isDisabledAndWhiteListAppCalling(String packageName, LocationRequest request, ILocationListener listener, PendingIntent intent, String providerName, int uid) {
        boolean getNlpOptimizeCloudSwitchValue = this.mVivoLocationUtils.getNlpOptimizeCloudSwitchValue();
        if (D) {
            VLog.d(TAG, "Request_pre_check provider " + providerName + " OC:" + ISOverseas + " locSwitch:" + this.mNLPSwitchISEnable + " uid:" + uid + " cloud:" + getNlpOptimizeCloudSwitchValue + " from " + packageName);
        }
        if (isNLPOptimizeCloudSwitchOpen() && !isSpecialProject() && !this.mNLPSwitchISEnable && providerName != null && providerName.equals("network") && !isCallPkgInWhiteList(packageName, providerName, uid)) {
            if (D) {
                VLog.w(TAG, "Current Device " + providerName + " is not enable, Not allow " + packageName + " to request " + providerName + "; " + this.mNLPSwitchISEnable);
            }
            vivoCallProviderDisable(request, listener, intent, packageName, providerName);
            return false;
        }
        return true;
    }

    private static boolean isSpecialProject() {
        if (ISOverseas) {
            return true;
        }
        return false;
    }

    private boolean isNLPOptimizeCloudSwitchOpen() {
        return this.mVivoLocationUtils.getNlpOptimizeCloudSwitchValue();
    }

    private void vivoCallProviderDisable(LocationRequest request, ILocationListener listener, PendingIntent intent, String packageName, String providerName) {
        try {
            if (listener != null) {
                listener.onProviderDisabled(providerName);
            } else if (intent != null) {
                Intent providerIntent = new Intent();
                providerIntent.putExtra("providerEnabled", false);
                intent.send(this.mContext, 0, providerIntent);
            }
            if (D) {
                VLog.d(TAG, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + packageName + " request " + providerName + ", but current is disable; tell Client end");
            }
        } catch (Exception e) {
            if (D) {
                VLog.w(TAG, "CallProviderDisable  " + e.toString());
            }
        }
    }

    private boolean isCallPkgInWhiteList(String packageName, String providerName, int callingUid) {
        ArrayList<String> wPkgList = this.mVivoLocationUtils.getNLPOptimizeWPkgList();
        if (wPkgList != null && wPkgList.size() > 0) {
            if (D) {
                VLog.d(TAG, "All pkg " + wPkgList);
            }
            Iterator<String> it = wPkgList.iterator();
            while (it.hasNext()) {
                String pkgTemp = it.next();
                if (packageName.equals(pkgTemp)) {
                    if (D) {
                        VLog.d(TAG, callingUid + " " + packageName + " in  V_W_Config List");
                    }
                    return true;
                }
            }
        } else {
            String[] strArr = PACKAGE_FILTER_LIST;
            if (strArr != null) {
                for (String pkg : strArr) {
                    if (pkg.equals(packageName)) {
                        return true;
                    }
                }
            }
        }
        if (D) {
            VLog.d(TAG, " callingUid " + callingUid + " pkg " + packageName + " check Failed! Not in W_List");
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void vivoTriggerUpdateBgWhiteList() {
        if (Binder.getCallingUid() != 1000) {
            return;
        }
        synchronized (this.mLock) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void tellAppSysNetworkProviderIsDisableLocked(String provider, boolean enabled) {
        ArrayList<LocationManagerService.UpdateRecord> records;
        LocationManagerService.LocationProviderManager p = this.mLocationManagerService.getLocationProviderManager(provider);
        if (p != null && (records = (ArrayList) this.mLocationManagerService.mRecordsByProvider.get(provider)) != null) {
            int N = records.size();
            if (D) {
                VLog.d(TAG, "provider " + provider + "  enable " + enabled + ", recordSize: " + N + "; Try Tell app!");
            }
            ArrayList<LocationManagerService.Receiver> needRemoveReceiverList = new ArrayList<>();
            for (int i = 0; i < N; i++) {
                LocationManagerService.UpdateRecord record = records.get(i);
                if (D) {
                    VLog.d(TAG, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + record.mReceiver);
                }
                if (this.mLocationManagerService.mUserInfoHelper.isCurrentUserId(UserHandle.getUserId(record.mReceiver.mCallerIdentity.uid))) {
                    record.mReceiver.callProviderEnabledLocked(provider, enabled);
                    if (record.mReceiver.mPkgBelongToWho == 2) {
                        needRemoveReceiverList.add(record.mReceiver);
                    }
                }
            }
            int i2 = needRemoveReceiverList.size();
            if (i2 > 0) {
                Iterator<LocationManagerService.Receiver> it = needRemoveReceiverList.iterator();
                while (it.hasNext()) {
                    LocationManagerService.Receiver receiveTemp = it.next();
                    if (D) {
                        VLog.d(TAG, "## Will remove No systemApp network instance, " + receiveTemp);
                    }
                    this.mLocationManagerService.removeUpdatesLocked(receiveTemp);
                }
            }
        }
    }

    /* loaded from: classes.dex */
    public class LocationManagerServiceExtCallbackImpl implements VivoMockLocationRecoveryNotify.LocationManagerServiceExtCallback {
        public LocationManagerServiceExtCallbackImpl() {
        }

        @Override // com.android.server.location.VivoMockLocationRecoveryNotify.LocationManagerServiceExtCallback
        public void removeTestProvider(String provider, String tag) {
            try {
                VivoLocationManagerServiceExt.this.mLocationManagerService.removeTestProvider(provider, VivoPermissionUtils.OS_PKG, "0");
                VLog.d(tag, "removeTestProvider packageName: " + VivoLocationManagerServiceExt.this.mContext.getOpPackageName() + "   uid: " + Binder.getCallingUid() + "  provider:" + provider + " successfully");
            } catch (SecurityException e) {
                VLog.d(tag, "removeTestProvider packageName: " + VivoLocationManagerServiceExt.this.mContext.getOpPackageName() + "   uid: " + Binder.getCallingUid() + "  provider:" + provider + " failed");
            }
        }

        @Override // com.android.server.location.VivoMockLocationRecoveryNotify.LocationManagerServiceExtCallback
        public boolean isMock(String name) {
            LocationManagerService.LocationProviderManager providerManager = VivoLocationManagerServiceExt.this.mLocationManagerService.getLocationProviderManager(name);
            if (providerManager == null) {
                return false;
            }
            return providerManager.isMock();
        }

        @Override // com.android.server.location.VivoMockLocationRecoveryNotify.LocationManagerServiceExtCallback
        public List<String> getProviders() {
            return VivoLocationManagerServiceExt.this.mLocationManagerService.getProviders((Criteria) null, true);
        }
    }

    public void addTestProviderNotify(String pk, String provider) {
        if (this.mVivoMockLocationRecoveryNotify.isEnableVivoMockLocationRecoveryNotify()) {
            this.mVivoMockLocationRecoveryNotify.addTestProviderNotify(pk, provider);
        }
    }

    public void removeTestProviderNotify(String pk, String provider) {
        if (this.mVivoMockLocationRecoveryNotify.isEnableVivoMockLocationRecoveryNotify()) {
            this.mVivoMockLocationRecoveryNotify.removeTestProviderNotify(pk, provider);
        }
    }

    public void setTestProviderNotify() {
        if (this.mVivoMockLocationRecoveryNotify.isEnableVivoMockLocationRecoveryNotify()) {
            this.mVivoMockLocationRecoveryNotify.setTestProviderNotify();
        }
    }

    private boolean isCtsApp() {
        int uid = Binder.getCallingUid();
        if (ArrayUtils.contains(this.mContext.getPackageManager().getPackagesForUid(uid), ANDROID_LOCATION_CTS)) {
            return true;
        }
        return false;
    }
}