package com.android.server.location;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.graphics.common.V1_0.Dataspace;
import android.location.Location;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.WorkSource;
import android.util.Log;
import com.android.internal.location.ProviderRequest;
import com.android.server.location.VivoDRManager;
import com.android.server.location.VivoLocConf;
import com.android.server.location.gnss.GnssLocationProvider;
import com.android.server.policy.VivoPolicyConstant;
import com.android.server.wm.VCD_FF_1;
import com.vivo.common.VivoCollectData;
import com.vivo.common.VivoCollectFile;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import com.vivo.server.adapter.ServiceAdapterFactory;
import com.vivo.server.adapter.location.AbsIZatDCControllerAdapter;
import com.vivo.server.adapter.location.Diagnoster;
import com.vivo.vcodetransbase.EventTransfer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import vivo.app.configuration.ContentValuesList;

/* loaded from: classes.dex */
public class VivoGnssLocationProviderExt implements IVivoGnssLocationProvider, VivoDRManager.VivoDRInterface {
    private static final int CUR_MODEL = 100;
    private static final int FIX_AVAILABLE = 1;
    private static final int FIX_UNAVAILABLE = 0;
    private static final String GNSS_BLOCK_INTENT = "com.vivo.GNSS_BLOCK";
    private static final String GNSS_DEBUG_INTENT = "com.vivo.GNSS_DEBUG";
    private static final String GNSS_TRIGGER_EXCEPTION = "com.vivo.GNSS_EXCEPTION";
    private static final String LOCATION_ID = "203";
    private static final String MAX_CN0_DATA_INTENT = "com.vivo.MAX_CN0_DATA";
    private static final int MSG_CN0_LOST_SUDDENLY = 109;
    private static final int MSG_INIT_FOR_MAX_CN0_DATA = 107;
    private static final int MSG_NO_SV_OR_FIX = 101;
    private static final int MSG_RESTART_NAVIGATION = 104;
    private static final int MSG_SAVE_MAX_CN0_PER_DAY = 108;
    private static final int MSG_START_NAVIGATION = 100;
    private static final int MSG_STOP_GPS_REQUEST = 103;
    private static final int MSG_STOP_NAVIGATION = 105;
    private static final int MSG_STRONG_CN0_CONTINUE_BEGIN = 110;
    private static final int MSG_STRONG_CN0_CONTINUE_END = 111;
    private static final int MSG_SV_REPORT = 102;
    private static final long RECENT_FIX_TIMEOUT = 10000;
    private static final String RESTART_GPS_LABEL = "2042";
    public static final String TAG = "IVivoGnssLocationProvider";
    private static final int TIMES_OF_INTERVAL = 10;
    private AlarmManager mAlarmManager;
    private Handler mCloudDiagnoseHandler;
    private int mCn0JumpToZero;
    private int mCn0LostSuddenly;
    private Handler mHandler;
    private boolean mIsDrive;
    private long mLastFixTime;
    private long mLastSvReportTime;
    private int mLastTopCn0;
    private int mMaxCn0;
    private int mMaxCn0PerTime;
    private ArrayList<Integer> mMaxCn0Record;
    private int mMaxDriveCn0;
    private int mMaxDriveCn0PerTime;
    private ArrayList<Integer> mMaxDriveCn0Record;
    private int mStartNaviCount;
    private VivoCn0WeakManager mVivoCn0WeakManager;
    private VivoCoreLocationManager mVivoCoreLocationManager;
    private VivoGpsPowerMonitor mVivoGpsPowerMonitor;
    private VivoNetworkLocationData mVivomNetworkLocationData;
    private static boolean mGpsEnabled = false;
    private static boolean mGnssTestMode = false;
    private static boolean mBlockTestMode = false;
    static long ALARM_TRIGGER_INTERVAL = 86400000;
    private static String LOCATION_ID_FOR_SMALL_DATA = "1906";
    private static String MAX_CN0_LABEL = "190602";
    private static int[] mContinueLevel = null;
    static final String[] PACKAGE_FILTER_LIST = {"com.baidu.BaiduMap", "com.autonavi.minimap"};
    private static boolean hasWhiteListApp = false;
    private AbsIZatDCControllerAdapter mIZatAdapter = null;
    private boolean mDCStart = false;
    private int mCurrentInterval = -1;
    private int mCurrentRestartStatus = 0;
    private List<Integer> mTopSvsList = new ArrayList();
    private int mStrongContinueCount = 0;
    GnssLocationProvider mGnssLocationProvider = null;
    private boolean mScreenState = true;
    private long mLastRestartTime = 0;
    private int mRestartCount = 0;
    private String mRestartType = null;
    private Object mVCD = null;
    private Object mVCF = null;
    PendingIntent mCn0ReportAlarmPI = null;
    private long mNaviStartTime = 0;
    private int mSvReportTime = 0;
    private int mCn0StrongCount = 0;
    private boolean mContinueState = false;
    private long mContinueBegin = 0;
    private int mSvCount = 10;
    private int mCn0StrengthLevel = 35;
    private int mNaviTimeLimit = 300000;
    private String mCn0StrengthRecord = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    private VivoDRManager mVivoDRManager = null;
    private int mFixStatus = 0;
    private boolean mIsNavigating = false;
    private VivoLocationDiagnosticManager mDiagnoster = VivoLocationDiagnosticManager.getInstance();
    private Context mContext = null;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.location.VivoGnssLocationProviderExt.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            VLog.d(VivoGnssLocationProviderExt.TAG, "receive broadcast intent, action: " + action);
            if (action == null) {
                return;
            }
            if (action.equals(VivoGnssLocationProviderExt.GNSS_DEBUG_INTENT)) {
                boolean unused = VivoGnssLocationProviderExt.mGnssTestMode = intent.getBooleanExtra("Test", false);
            } else if (action.equals(VivoGnssLocationProviderExt.MAX_CN0_DATA_INTENT)) {
                if (VivoGnssLocationProviderExt.this.mHandler != null) {
                    VivoGnssLocationProviderExt.this.mHandler.obtainMessage(108, 0, 1, null).sendToTarget();
                }
            } else if (action.equals(VivoGnssLocationProviderExt.GNSS_BLOCK_INTENT)) {
                boolean unused2 = VivoGnssLocationProviderExt.mBlockTestMode = true;
            } else if (action.equals(VivoGnssLocationProviderExt.GNSS_TRIGGER_EXCEPTION)) {
                VivoGnssLocationProviderExt.this.mDiagnoster.reportEvent(0, VLog.getStackTraceString(new Throwable()));
            }
            if (action.equals(VivoPolicyConstant.ACTION_VIVO_LOG_CHANGED)) {
                String adbStatus = intent.getStringExtra("adblog_status");
                VLog.d(VivoGnssLocationProviderExt.TAG, "mBroadcastReceiver adb:" + adbStatus);
                if ("on".equals(adbStatus)) {
                    GnssLocationProvider.DEBUG = true;
                    GnssLocationProvider.VERBOSE = true;
                    VivoGnssLocationProviderExt.this.setDebug(true);
                    if (VivoGnssLocationProviderExt.this.mIZatAdapter != null) {
                        VivoGnssLocationProviderExt.this.mIZatAdapter.setDebug(true);
                    }
                    VivoDRManager.setDebug(true);
                } else {
                    GnssLocationProvider.DEBUG = VLog.isLoggable(VivoGnssLocationProviderExt.TAG, 3);
                    GnssLocationProvider.VERBOSE = VLog.isLoggable(VivoGnssLocationProviderExt.TAG, 2);
                    VivoGnssLocationProviderExt.this.setDebug(false);
                    if (VivoGnssLocationProviderExt.this.mIZatAdapter != null) {
                        VivoGnssLocationProviderExt.this.mIZatAdapter.setDebug(false);
                    }
                    VivoDRManager.setDebug(false);
                }
            }
            if (action.equals("android.intent.action.SCREEN_OFF")) {
                VivoGnssLocationProviderExt.this.mScreenState = false;
            } else if (action.equals("android.intent.action.SCREEN_ON")) {
                VivoGnssLocationProviderExt.this.mScreenState = true;
            }
        }
    };
    private Runnable mCloudDiagnoseRunnable = new Runnable() { // from class: com.android.server.location.VivoGnssLocationProviderExt.2
        @Override // java.lang.Runnable
        public void run() {
            VLog.d(VivoGnssLocationProviderExt.TAG, "mCloudDiagnoseRunnable called.");
            if (VivoGnssLocationProviderExt.this.mVCF != null) {
                VivoCollectFile vivoCollectFile = (VivoCollectFile) VivoGnssLocationProviderExt.this.mVCF;
                VivoCollectFile.writeData("1402", "1402_5", "Subtype:3;cause:1;firmware_ver:unknow", true, (String) null);
            }
        }
    };

    static /* synthetic */ int access$1308(VivoGnssLocationProviderExt x0) {
        int i = x0.mRestartCount;
        x0.mRestartCount = i + 1;
        return i;
    }

    static /* synthetic */ int access$1408(VivoGnssLocationProviderExt x0) {
        int i = x0.mStartNaviCount;
        x0.mStartNaviCount = i + 1;
        return i;
    }

    static /* synthetic */ String access$2484(VivoGnssLocationProviderExt x0, Object x1) {
        String str = x0.mCn0StrengthRecord + x1;
        x0.mCn0StrengthRecord = str;
        return str;
    }

    static /* synthetic */ int access$3208(VivoGnssLocationProviderExt x0) {
        int i = x0.mCn0JumpToZero;
        x0.mCn0JumpToZero = i + 1;
        return i;
    }

    static /* synthetic */ int access$3708(VivoGnssLocationProviderExt x0) {
        int i = x0.mCn0LostSuddenly;
        x0.mCn0LostSuddenly = i + 1;
        return i;
    }

    static /* synthetic */ int access$912(VivoGnssLocationProviderExt x0, int x1) {
        int i = x0.mStrongContinueCount + x1;
        x0.mStrongContinueCount = i;
        return i;
    }

    static /* synthetic */ int access$920(VivoGnssLocationProviderExt x0, int x1) {
        int i = x0.mStrongContinueCount - x1;
        x0.mStrongContinueCount = i;
        return i;
    }

    public Executor getVivoExecutor() {
        if (this.mHandler == null) {
            this.mHandler = new VivoGnssLocationProviderExtHandler(VivoLocThread.getInstance().getGnssProviderLooper());
        }
        return new HandlerExecutor(this.mHandler);
    }

    public Looper getVivoLooper() {
        return VivoLocThread.getInstance().getGnssProviderLooper();
    }

    public void initialize(final Context context, GnssLocationProvider gnssLocationProvider) {
        this.mGnssLocationProvider = gnssLocationProvider;
        this.mContext = context;
        this.mVivoGpsPowerMonitor = new VivoGpsPowerMonitor(context, gnssLocationProvider, VivoLocThread.getInstance().getGnssProviderLooper());
        this.mHandler.post(new Runnable() { // from class: com.android.server.location.VivoGnssLocationProviderExt.3
            @Override // java.lang.Runnable
            public void run() {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(VivoGnssLocationProviderExt.GNSS_DEBUG_INTENT);
                intentFilter.addAction(VivoPolicyConstant.ACTION_VIVO_LOG_CHANGED);
                intentFilter.addAction("android.intent.action.SCREEN_OFF");
                intentFilter.addAction("android.intent.action.SCREEN_ON");
                intentFilter.addAction(VivoGnssLocationProviderExt.MAX_CN0_DATA_INTENT);
                intentFilter.addAction(VivoGnssLocationProviderExt.GNSS_BLOCK_INTENT);
                intentFilter.addAction(VivoGnssLocationProviderExt.GNSS_TRIGGER_EXCEPTION);
                context.registerReceiverAsUser(VivoGnssLocationProviderExt.this.mBroadcastReceiver, UserHandle.ALL, intentFilter, null, VivoGnssLocationProviderExt.this.mHandler);
            }
        });
        this.mStartNaviCount = 0;
        this.mLastTopCn0 = 0;
        this.mLastSvReportTime = 0L;
        this.mCn0LostSuddenly = 0;
        this.mCn0JumpToZero = 0;
        this.mVivomNetworkLocationData = new VivoNetworkLocationData(context);
        this.mVivoCoreLocationManager = new VivoCoreLocationManager(context, this.mGnssLocationProvider);
        ServiceAdapterFactory serviceAdapterFactory = ServiceAdapterFactory.getServiceAdapterFactory();
        if (serviceAdapterFactory == null) {
            VLog.e(TAG, "null = serviceAdapterFactory");
        } else {
            AbsIZatDCControllerAdapter iZatDCControllerAdapter = ServiceAdapterFactory.getServiceAdapterFactory().getIZatDCControllerAdapter();
            this.mIZatAdapter = iZatDCControllerAdapter;
            if (iZatDCControllerAdapter == null) {
                VLog.e(TAG, "mIZatAdapter = null");
            } else {
                if (!this.mDCStart) {
                    this.mDCStart = iZatDCControllerAdapter.initDC(this.mContext);
                    VLog.w(TAG, "initDC");
                }
                this.mIZatAdapter.setDiagnosticCallback(new Diagnoster() { // from class: com.android.server.location.VivoGnssLocationProviderExt.4
                    public void Log(String log) {
                        VivoGnssLocationProviderExt.this.mDiagnoster.Log(log);
                    }

                    public void addKeyValue(String key, String value) {
                        VivoGnssLocationProviderExt.this.mDiagnoster.addKeyValue(key, value);
                    }

                    public void reportEvent(int type, String stack) {
                        VivoGnssLocationProviderExt.this.mDiagnoster.reportEvent(type, stack);
                    }

                    public void updateXtraStatus(boolean valid, long updateTime) {
                        VivoGnssLocationProviderExt.this.mDiagnoster.updateXtraStatus(valid, updateTime);
                    }

                    public void updateRefLocStatus(Location refLoc, long updateTime) {
                        VivoGnssLocationProviderExt.this.mDiagnoster.updateRefLocStatus(refLoc, updateTime);
                    }

                    public void updateRefTimeStatus(boolean valid, String type, long updateTime) {
                        VivoGnssLocationProviderExt.this.mDiagnoster.updateRefTimeStatus(valid, type, updateTime);
                    }
                });
                VLog.w(TAG, "set diag callback to adapter");
            }
        }
        this.mVCD = getVCD(context);
        this.mVCF = getVCF();
        this.mCloudDiagnoseHandler = VivoLocThread.getInstance().getDiagnosticThreadHandler();
        if (VivoCn0WeakManager.isCn0WeakManagerOn()) {
            this.mVivoCn0WeakManager = new VivoCn0WeakManager(this.mContext);
        }
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mMaxCn0Record = new ArrayList<>();
        this.mMaxDriveCn0Record = new ArrayList<>();
        this.mCn0ReportAlarmPI = null;
        this.mMaxCn0 = 0;
        this.mMaxDriveCn0 = 0;
        this.mIsDrive = false;
        Handler handler = this.mHandler;
        if (handler != null) {
            handler.sendEmptyMessage(107);
        }
        this.mCn0StrengthRecord = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mNaviStartTime = 0L;
        this.mSvReportTime = 0;
        this.mCn0StrongCount = 0;
        this.mContinueState = false;
        this.mContinueBegin = 0L;
        mContinueLevel = new int[6];
        this.mNaviStartTime = SystemClock.elapsedRealtime();
        VivoLocConf config = VivoLocConf.getInstance();
        config.registerListener(VivoLocConf.CN0_STRENGTH, new VivoLocConf.ContentValuesListChangedListener() { // from class: com.android.server.location.-$$Lambda$VivoGnssLocationProviderExt$fT5VKe-r1F_wFqlHKbX-K_SwOSg
            @Override // com.android.server.location.VivoLocConf.ContentValuesListChangedListener
            public final void onConfigChanged(ContentValuesList contentValuesList) {
                VivoGnssLocationProviderExt.this.parseCn0StrengthConfig(contentValuesList);
            }
        });
        config.registerListener(VivoLocConf.CN0_WEAK, $$Lambda$Y0z0HAc6zIaC1P8ByquHV7RZ5HM.INSTANCE);
        this.mVivoDRManager = new VivoDRManager(this.mContext, this);
    }

    public void updateEnabled(boolean enable) {
        mGpsEnabled = enable;
        VivoLocationDiagnosticManager vivoLocationDiagnosticManager = this.mDiagnoster;
        vivoLocationDiagnosticManager.Log("updateEnabled:" + enable);
    }

    public boolean isInFactoryMode() {
        if ("yes".equals(SystemProperties.get("persist.sys.factory.mode", "no")) && "0".equals(SystemProperties.get("sys.bsptest.gps", "0")) && VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_FACTORY_MODE)) {
            return true;
        }
        return false;
    }

    public void onStartNavigating(int interval) {
        this.mDiagnoster.startNavigation(interval);
        if (!mGnssTestMode && VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_POWER_MONITOR)) {
            this.mVivoGpsPowerMonitor.startGpsPowerMonitor();
        }
        if (VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_FUSED_LOCATION)) {
            this.mVivoCoreLocationManager.onStartNavigating();
        }
        long now = System.currentTimeMillis();
        if (VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_BIG_DATA)) {
            this.mVivomNetworkLocationData.startNavigating();
            AbsIZatDCControllerAdapter absIZatDCControllerAdapter = this.mIZatAdapter;
            if (absIZatDCControllerAdapter != null) {
                if (!this.mDCStart) {
                    this.mDCStart = absIZatDCControllerAdapter.initDC(this.mContext);
                }
                if (this.mDCStart) {
                    this.mIZatAdapter.startNavigating(now, hasWhiteListApp);
                }
            }
        }
        if (this.mVivoCn0WeakManager != null && VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_CN0_WEAK)) {
            this.mVivoCn0WeakManager.startNavigating(now);
        }
        this.mLastFixTime = 0L;
        this.mFixStatus = 0;
        this.mIsDrive = false;
        this.mHandler.obtainMessage(100, interval, 1, null).sendToTarget();
        if (this.mVivoDRManager != null && VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_DR_LOCATION)) {
            this.mVivoDRManager.startVPDR();
        }
        try {
            this.mIsNavigating = true;
            SystemProperties.set("persist.sys.gnss.start", "1");
        } catch (Exception e) {
            VLog.e(TAG, "Exception: " + e);
        }
    }

    public void onStopNavigating() {
        this.mDiagnoster.Log("onStopNavigating");
        if (VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_FUSED_LOCATION)) {
            this.mVivoCoreLocationManager.onStopNavigating();
        }
        if (VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_BIG_DATA)) {
            this.mVivomNetworkLocationData.stopNavigating();
        }
        long now = System.currentTimeMillis();
        if (this.mVivoCn0WeakManager != null && VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_CN0_WEAK)) {
            this.mVivoCn0WeakManager.stopNavigating(now);
        }
        if (this.mIZatAdapter != null && this.mDCStart && VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_BIG_DATA)) {
            this.mIZatAdapter.stopNavigating(now, hasWhiteListApp);
            hasWhiteListApp = false;
        }
        this.mFixStatus = 0;
        if (this.mVivoDRManager != null && VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_DR_LOCATION)) {
            this.mVivoDRManager.stopVPDR();
        }
        this.mLastFixTime = 0L;
        this.mHandler.obtainMessage(105, 0, 1, null).sendToTarget();
    }

    public void stopNavigating() {
        this.mDiagnoster.Log("stopNavigating");
        if (VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_POWER_MONITOR)) {
            this.mVivoGpsPowerMonitor.stopGpsPowerMonitor();
        }
        if (VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_CN0_WEAK)) {
            this.mHandler.removeMessages(109);
        }
        this.mHandler.obtainMessage(103, 0, 1, null).sendToTarget();
        try {
            this.mIsNavigating = false;
            SystemProperties.set("persist.sys.gnss.start", "0");
        } catch (Exception e) {
            VLog.e(TAG, "Exception: " + e);
        }
    }

    public void onSetRequest(ProviderRequest request) {
        WorkSource source = request.workSource;
        VivoLocationDiagnosticManager vivoLocationDiagnosticManager = this.mDiagnoster;
        vivoLocationDiagnosticManager.Log("onSetRequest:" + request + " worksource:" + source);
        if (VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_FUSED_LOCATION)) {
            this.mVivoCoreLocationManager.onSetRequest(request, source);
        }
        if (request != null && request.reportLocation && source != null && mGpsEnabled && this.mHandler != null && VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_BIG_DATA) && this.mCloudDiagnoseHandler != null) {
            VLog.d(TAG, "diagnose delay runable posted.");
            this.mCloudDiagnoseHandler.postDelayed(this.mCloudDiagnoseRunnable, 2000L);
        }
        if (VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_CN0_WEAK)) {
            if (VivoCn0WeakManager.isCn0WeakManagerOn() && this.mVivoCn0WeakManager == null) {
                VLog.d(TAG, "VivoCn0WeakManager + isCn0WeakManagerOn");
                this.mVivoCn0WeakManager = new VivoCn0WeakManager(this.mContext);
            }
            if (this.mVivoCn0WeakManager != null && request != null && request.reportLocation && source != null) {
                long requestTime = System.currentTimeMillis();
                this.mVivoCn0WeakManager.handleSetRequest(source, requestTime);
            }
        }
        if (this.mVivoDRManager != null && VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_DR_LOCATION)) {
            this.mVivoDRManager.onSetRequest(request, source);
        }
    }

    public void handleSetRequest(ProviderRequest request, WorkSource source) {
        VivoLocationDiagnosticManager vivoLocationDiagnosticManager = this.mDiagnoster;
        vivoLocationDiagnosticManager.Log("handleSetRequest, " + request + ", " + source);
        if (VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_BIG_DATA)) {
            if (this.mHandler != null && VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_BIG_DATA)) {
                if (mBlockTestMode) {
                    VLog.d(TAG, "sleep begin");
                    try {
                        Thread.sleep(3000L);
                    } catch (InterruptedException e) {
                    }
                    VLog.d(TAG, "sleep end");
                }
                Handler handler = this.mCloudDiagnoseHandler;
                if (handler != null) {
                    handler.removeCallbacks(this.mCloudDiagnoseRunnable);
                }
            }
            if (request != null && source != null && source.size() > 0) {
                if (isHasWhiteListApp(source)) {
                    AbsIZatDCControllerAdapter absIZatDCControllerAdapter = this.mIZatAdapter;
                    if (absIZatDCControllerAdapter != null && this.mDCStart) {
                        absIZatDCControllerAdapter.onSetRequest(request.toString(), source.toString());
                        if (request.reportLocation) {
                            hasWhiteListApp = true;
                            return;
                        }
                        return;
                    }
                    return;
                }
                hasWhiteListApp = false;
            }
        }
    }

    public void onReportGpsLocation(boolean started, Location location) {
        if (started && this.mFixStatus != 1) {
            this.mFixStatus = 1;
        }
        onReportGpsLocation(location);
    }

    public void onReportSvStatus(int svCount, float[] cn0s, float[] svElevations, float[] svAzimuths, int[] svidWithFlags, float[] svCarrierFreqs, float[] basebandCn0s) {
        if (this.mFixStatus == 1 && this.mLastFixTime > 0 && SystemClock.elapsedRealtime() - this.mLastFixTime > 10000 && VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_BIG_DATA)) {
            onLostLocation();
        }
        if (VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_POWER_MONITOR)) {
            this.mVivoGpsPowerMonitor.reportSvStatus(svCount, cn0s);
        }
        int strongCn0Count = 0;
        for (int i = 0; i < svCount; i++) {
            if (cn0s[i] > 0.0f) {
                this.mTopSvsList.add(Integer.valueOf((int) cn0s[i]));
            }
            if (cn0s[i] > this.mCn0StrengthLevel) {
                strongCn0Count++;
            }
        }
        this.mSvReportTime++;
        if (strongCn0Count > this.mSvCount) {
            this.mCn0StrongCount++;
            if (!this.mContinueState) {
                this.mHandler.sendEmptyMessage(MSG_STRONG_CN0_CONTINUE_BEGIN);
            }
        } else if (this.mContinueState) {
            this.mHandler.sendEmptyMessage(111);
        }
        try {
            this.mTopSvsList.sort(new Comparator<Integer>() { // from class: com.android.server.location.VivoGnssLocationProviderExt.5
                @Override // java.util.Comparator
                public int compare(Integer o1, Integer o2) {
                    return o2.compareTo(o1);
                }
            });
        } catch (Exception e) {
            VLog.e(TAG, "sv sort failed", e);
        }
        int topCn0 = 0;
        if (this.mTopSvsList.size() >= 6) {
            int i2 = 0;
            for (Integer num : this.mTopSvsList) {
                float cn0 = num.intValue();
                if (i2 == 6) {
                    break;
                }
                topCn0 = (int) (topCn0 + cn0);
                i2++;
            }
        }
        int topCn02 = topCn0 / 6;
        if (VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_CN0_WEAK)) {
            this.mHandler.removeMessages(109);
            this.mHandler.sendEmptyMessageDelayed(109, 3000L);
        }
        this.mHandler.obtainMessage(102, this.mTopSvsList.size(), 1, Integer.valueOf(topCn02 > 1 ? topCn02 : 0)).sendToTarget();
        this.mTopSvsList.clear();
        this.mDiagnoster.updateSvStatus(svCount, topCn02);
        if (this.mIZatAdapter != null && this.mDCStart && hasWhiteListApp && VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_BIG_DATA)) {
            this.mIZatAdapter.updateSvStatus(svCount, svidWithFlags, cn0s);
        }
        if (this.mVivoCn0WeakManager != null && VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_CN0_WEAK)) {
            this.mVivoCn0WeakManager.handleReportSvStatue(topCn02);
        }
        if (VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_FUSED_LOCATION)) {
            this.mVivoCoreLocationManager.onReportSvStatus(svCount, cn0s, svElevations, svAzimuths);
        }
        if (this.mVivoDRManager != null && VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_DR_LOCATION)) {
            this.mVivoDRManager.updateSatelliteStatusChanged(svCount, svidWithFlags, cn0s, svElevations, svAzimuths, svCarrierFreqs);
        }
    }

    public void onNlpPassiveLocation(Location location) {
        if (this.mIZatAdapter != null && this.mDCStart && hasWhiteListApp && VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_CN0_WEAK)) {
            this.mIZatAdapter.onNlpPassiveLocation(location.getTime(), location.getLongitude(), location.getLatitude());
        }
        VivoLocationDiagnosticManager vivoLocationDiagnosticManager = this.mDiagnoster;
        vivoLocationDiagnosticManager.Log("onNlpPassiveLocation:" + location);
    }

    public void onNetworkChange(boolean data) {
        if (this.mIZatAdapter != null && this.mDCStart && hasWhiteListApp && VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_CN0_WEAK)) {
            this.mIZatAdapter.setNetwork(data);
        }
        VivoLocationDiagnosticManager vivoLocationDiagnosticManager = this.mDiagnoster;
        vivoLocationDiagnosticManager.Log("onNetworkChange,data:" + data);
    }

    public void onReportNmea(String nmea, long timestamp) {
        if (this.mVivoDRManager != null && VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_DR_LOCATION)) {
            this.mVivoDRManager.updateNmeaChanged(nmea, timestamp);
        }
    }

    public boolean isGpsUsing() {
        VLog.d(TAG, "enter powersavemode and screen off, isGpsUsing:" + this.mIsNavigating);
        return this.mIsNavigating;
    }

    /* loaded from: classes.dex */
    class VivoGnssLocationProviderExtHandler extends Handler {
        public VivoGnssLocationProviderExtHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            VLog.d(VivoGnssLocationProviderExt.TAG, "handleMessage " + msg.what);
            try {
                switch (msg.what) {
                    case 100:
                        VivoGnssLocationProviderExt.this.mCurrentInterval = msg.arg1;
                        if (VivoGnssLocationProviderExt.this.mCurrentInterval >= 0 && VivoGnssLocationProviderExt.this.mCurrentInterval < 1000) {
                            VivoGnssLocationProviderExt.this.mCurrentInterval = 1000;
                        }
                        VivoGnssLocationProviderExt.this.mStrongContinueCount = 0;
                        VivoGnssLocationProviderExt.this.mLastTopCn0 = 0;
                        VivoGnssLocationProviderExt.this.mLastSvReportTime = 0L;
                        VLog.d(VivoGnssLocationProviderExt.TAG, "currentInterval: " + VivoGnssLocationProviderExt.this.mCurrentInterval + " mCurrentRestartStatus: " + VivoGnssLocationProviderExt.this.mCurrentRestartStatus);
                        VivoGnssLocationProviderExt.this.mHandler.removeMessages(101);
                        VivoGnssLocationProviderExt.this.mHandler.removeMessages(104);
                        if (VivoGnssLocationProviderExt.this.mCurrentInterval > 0 && VivoGnssLocationProviderExt.this.mCurrentInterval <= 10000 && VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_NO_SV_FIX_RECOVERY)) {
                            if (!VivoGnssLocationProviderExt.mGnssTestMode && VivoGnssLocationProviderExt.this.mRestartCount < 5) {
                                VivoGnssLocationProviderExt.this.mCurrentRestartStatus = 1;
                                VivoGnssLocationProviderExt.this.mHandler.sendMessageDelayed(Message.obtain(VivoGnssLocationProviderExt.this.mHandler, 101, 1, 0), VivoGnssLocationProviderExt.this.mCurrentInterval * 10);
                            } else {
                                VLog.d(VivoGnssLocationProviderExt.TAG, "in Gnss Test Mode , skip GPS restart ");
                            }
                        }
                        VivoGnssLocationProviderExt.access$1408(VivoGnssLocationProviderExt.this);
                        VivoGnssLocationProviderExt.this.mNaviStartTime = 0L;
                        VivoGnssLocationProviderExt.this.mSvReportTime = 0;
                        VivoGnssLocationProviderExt.this.mCn0StrongCount = 0;
                        VivoGnssLocationProviderExt.this.mContinueState = false;
                        VivoGnssLocationProviderExt.this.mContinueBegin = 0L;
                        int[] unused = VivoGnssLocationProviderExt.mContinueLevel = new int[6];
                        VivoGnssLocationProviderExt.this.mNaviStartTime = SystemClock.elapsedRealtime();
                        VivoGnssLocationProviderExt.this.mMaxCn0PerTime = 0;
                        VivoGnssLocationProviderExt.this.mMaxDriveCn0PerTime = 0;
                        return;
                    case 101:
                        VivoGnssLocationProviderExt.this.mHandler.removeMessages(101);
                        if (VivoGnssLocationProviderExt.this.mCurrentRestartStatus == 1 && VivoGnssLocationProviderExt.this.mScreenState) {
                            int type = msg.arg1;
                            if (type == 0) {
                                VivoGnssLocationProviderExt.this.mRestartType = "no_fix";
                                VivoGnssLocationProviderExt.this.mDiagnoster.reportEvent(0, VLog.getStackTraceString(new Throwable()));
                            } else if (type == 1) {
                                VivoGnssLocationProviderExt.this.mRestartType = "no_sv";
                            }
                            VivoGnssLocationProviderExt.this.mGnssLocationProvider.enableGps(false);
                            VivoGnssLocationProviderExt.this.mHandler.sendEmptyMessageDelayed(104, 1000L);
                            return;
                        }
                        return;
                    case 102:
                        int topCn0 = ((Integer) msg.obj).intValue();
                        if (topCn0 > VivoGnssLocationProviderExt.this.mMaxCn0) {
                            VivoGnssLocationProviderExt.this.mMaxCn0 = topCn0;
                        }
                        if (topCn0 > VivoGnssLocationProviderExt.this.mMaxCn0PerTime) {
                            VivoGnssLocationProviderExt.this.mMaxCn0PerTime = topCn0;
                        }
                        if (VivoGnssLocationProviderExt.this.mIsDrive && topCn0 > VivoGnssLocationProviderExt.this.mMaxDriveCn0) {
                            VivoGnssLocationProviderExt.this.mMaxDriveCn0 = topCn0;
                        }
                        if (VivoGnssLocationProviderExt.this.mIsDrive && topCn0 > VivoGnssLocationProviderExt.this.mMaxDriveCn0PerTime) {
                            VivoGnssLocationProviderExt.this.mMaxDriveCn0PerTime = topCn0;
                        }
                        long now = SystemClock.elapsedRealtime();
                        if (topCn0 == 0 && VivoGnssLocationProviderExt.this.mLastTopCn0 > 25 && now - VivoGnssLocationProviderExt.this.mLastSvReportTime < 2000) {
                            VivoGnssLocationProviderExt.access$3208(VivoGnssLocationProviderExt.this);
                            VivoGnssLocationProviderExt.this.mDiagnoster.Log("CN0 jump to 0.");
                        }
                        VivoGnssLocationProviderExt.this.mLastTopCn0 = topCn0;
                        VivoGnssLocationProviderExt.this.mLastSvReportTime = now;
                        VivoGnssLocationProviderExt.this.mHandler.removeMessages(101);
                        try {
                            if (VivoGnssLocationProviderExt.this.mCurrentInterval > 0 && VivoGnssLocationProviderExt.this.mCurrentInterval <= 10000 && VivoGnssLocationProviderExt.this.mCurrentRestartStatus == 1 && VivoGnssLocationProviderExt.this.mRestartCount < 5 && SystemClock.elapsedRealtime() - VivoGnssLocationProviderExt.this.mLastRestartTime > 180000) {
                                if (msg.arg1 >= 6 && ((Integer) msg.obj).intValue() >= 30) {
                                    VivoGnssLocationProviderExt.access$912(VivoGnssLocationProviderExt.this, 1);
                                    if (VivoGnssLocationProviderExt.this.mStrongContinueCount > 8) {
                                        VivoGnssLocationProviderExt.this.mStrongContinueCount = 8;
                                    }
                                } else {
                                    VivoGnssLocationProviderExt.access$920(VivoGnssLocationProviderExt.this, 1);
                                    if (VivoGnssLocationProviderExt.this.mStrongContinueCount < 0) {
                                        VivoGnssLocationProviderExt.this.mStrongContinueCount = 0;
                                    }
                                }
                                if (VivoGnssLocationProviderExt.this.mLastFixTime < 0 || SystemClock.elapsedRealtime() - VivoGnssLocationProviderExt.this.mLastFixTime < VivoGnssLocationProviderExt.this.mCurrentInterval * 5 || VivoGnssLocationProviderExt.this.mStrongContinueCount < 5) {
                                    VivoGnssLocationProviderExt.this.mHandler.sendMessageDelayed(Message.obtain(VivoGnssLocationProviderExt.this.mHandler, 101, 1, 0), VivoGnssLocationProviderExt.this.mCurrentInterval * 10);
                                    return;
                                } else {
                                    VivoGnssLocationProviderExt.this.mHandler.sendMessage(Message.obtain(VivoGnssLocationProviderExt.this.mHandler, 101, 0, 0));
                                    return;
                                }
                            }
                            return;
                        } catch (Exception e) {
                            e.printStackTrace();
                            return;
                        }
                    case 103:
                        VivoGnssLocationProviderExt.this.mHandler.removeMessages(101);
                        VivoGnssLocationProviderExt.this.mHandler.removeMessages(104);
                        VivoGnssLocationProviderExt.this.mCurrentInterval = -1;
                        VivoGnssLocationProviderExt.this.mCurrentRestartStatus = 0;
                        VivoGnssLocationProviderExt.this.mStrongContinueCount = 0;
                        VivoGnssLocationProviderExt.this.mLastRestartTime = 0L;
                        VivoGnssLocationProviderExt.this.mRestartCount = 0;
                        return;
                    case 104:
                        VLog.d(VivoGnssLocationProviderExt.TAG, "restart navigation after 10 times interval ");
                        VivoGnssLocationProviderExt.this.mCurrentRestartStatus = 2;
                        VivoGnssLocationProviderExt.this.mLastRestartTime = SystemClock.elapsedRealtime();
                        VivoGnssLocationProviderExt.access$1308(VivoGnssLocationProviderExt.this);
                        VivoGnssLocationProviderExt.this.mGnssLocationProvider.enableGps(true);
                        String buf = "RestartType:" + VivoGnssLocationProviderExt.this.mRestartType + ",count:" + VivoGnssLocationProviderExt.this.mRestartCount;
                        VLog.i(VivoGnssLocationProviderExt.TAG, "MSG_RESTART_NAVIGATION," + buf);
                        return;
                    case 105:
                        VivoGnssLocationProviderExt.this.mHandler.removeMessages(101);
                        VivoGnssLocationProviderExt.this.mCurrentInterval = -1;
                        VivoGnssLocationProviderExt.this.mCurrentRestartStatus = 0;
                        VivoGnssLocationProviderExt.this.mStrongContinueCount = 0;
                        long naviTime = SystemClock.elapsedRealtime() - VivoGnssLocationProviderExt.this.mNaviStartTime;
                        if (naviTime > VivoGnssLocationProviderExt.this.mNaviTimeLimit && VivoGnssLocationProviderExt.this.mCn0StrengthRecord.length() < 500) {
                            String cn0Continue = VivoGnssLocationProviderExt.mContinueLevel[0] + "," + VivoGnssLocationProviderExt.mContinueLevel[1] + "," + VivoGnssLocationProviderExt.mContinueLevel[3] + "," + VivoGnssLocationProviderExt.mContinueLevel[4] + "," + VivoGnssLocationProviderExt.mContinueLevel[5] + "," + VivoGnssLocationProviderExt.this.mMaxCn0PerTime + "," + VivoGnssLocationProviderExt.this.mMaxDriveCn0PerTime + "|" + VivoGnssLocationProviderExt.this.mSvCount + "," + VivoGnssLocationProviderExt.this.mCn0StrengthLevel + "," + VivoGnssLocationProviderExt.this.mNaviTimeLimit;
                            VivoGnssLocationProviderExt.access$2484(VivoGnssLocationProviderExt.this, cn0Continue + ";");
                            return;
                        }
                        return;
                    case 106:
                    default:
                        return;
                    case 107:
                        VivoGnssLocationProviderExt.this.setAlarmManagerForMaxCn0();
                        return;
                    case 108:
                        VivoGnssLocationProviderExt.this.mMaxCn0Record.add(Integer.valueOf(VivoGnssLocationProviderExt.this.mMaxCn0));
                        VivoGnssLocationProviderExt.this.mMaxDriveCn0Record.add(Integer.valueOf(VivoGnssLocationProviderExt.this.mMaxDriveCn0));
                        VivoGnssLocationProviderExt.this.mMaxCn0 = 0;
                        VivoGnssLocationProviderExt.this.mMaxDriveCn0 = 0;
                        if (VivoGnssLocationProviderExt.this.mMaxCn0Record.size() == 7) {
                            int tempMax = 0;
                            Iterator it = VivoGnssLocationProviderExt.this.mMaxCn0Record.iterator();
                            while (it.hasNext()) {
                                Integer cn0 = (Integer) it.next();
                                if (cn0.intValue() > tempMax) {
                                    tempMax = cn0.intValue();
                                }
                            }
                            int tempDriveMax = 0;
                            Iterator it2 = VivoGnssLocationProviderExt.this.mMaxDriveCn0Record.iterator();
                            while (it2.hasNext()) {
                                Integer cn02 = (Integer) it2.next();
                                if (cn02.intValue() > tempDriveMax) {
                                    tempDriveMax = cn02.intValue();
                                }
                            }
                            String cn0Record = VivoGnssLocationProviderExt.this.mCn0StrengthRecord;
                            VivoGnssLocationProviderExt.this.saveToDataFile(tempMax, tempDriveMax, VivoGnssLocationProviderExt.this.mStartNaviCount, VivoGnssLocationProviderExt.this.mCn0LostSuddenly, VivoGnssLocationProviderExt.this.mCn0JumpToZero, cn0Record);
                            VivoGnssLocationProviderExt.this.mMaxCn0Record = new ArrayList();
                            VivoGnssLocationProviderExt.this.mMaxDriveCn0Record = new ArrayList();
                            VivoGnssLocationProviderExt.this.mStartNaviCount = 0;
                            VivoGnssLocationProviderExt.this.mCn0LostSuddenly = 0;
                            VivoGnssLocationProviderExt.this.mCn0JumpToZero = 0;
                            VivoGnssLocationProviderExt.this.mCn0StrengthRecord = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                            return;
                        }
                        return;
                    case 109:
                        VLog.d(VivoGnssLocationProviderExt.TAG, "Lost suddenly");
                        VivoGnssLocationProviderExt.access$3708(VivoGnssLocationProviderExt.this);
                        return;
                    case VivoGnssLocationProviderExt.MSG_STRONG_CN0_CONTINUE_BEGIN /* 110 */:
                        VivoGnssLocationProviderExt.this.mContinueState = true;
                        VivoGnssLocationProviderExt.this.mContinueBegin = SystemClock.elapsedRealtime();
                        VLog.d(VivoGnssLocationProviderExt.TAG, "Cn0 Strength CN0_CONTINUE_BEGIN");
                        return;
                    case 111:
                        VivoGnssLocationProviderExt.this.mContinueState = false;
                        int continueLast = (int) (SystemClock.elapsedRealtime() - VivoGnssLocationProviderExt.this.mContinueBegin);
                        if (continueLast > 0) {
                            if (continueLast > 100000) {
                                int[] iArr = VivoGnssLocationProviderExt.mContinueLevel;
                                iArr[5] = iArr[5] + 1;
                            } else {
                                int[] iArr2 = VivoGnssLocationProviderExt.mContinueLevel;
                                int i = continueLast / 20000;
                                iArr2[i] = iArr2[i] + 1;
                            }
                        }
                        VLog.d(VivoGnssLocationProviderExt.TAG, "Cn0 Strength CN0_CONTINUE_END" + continueLast);
                        return;
                }
            } catch (Exception e2) {
                VLog.e(VivoGnssLocationProviderExt.TAG, VLog.getStackTraceString(e2));
            }
        }
    }

    private Object getVCD(Context context) {
        if (this.mVCD == null) {
            this.mVCD = new VivoCollectData(context);
        }
        return this.mVCD;
    }

    private Object getVCF() {
        if (this.mVCF == null) {
            this.mVCF = new VivoCollectFile();
        }
        return this.mVCF;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setAlarmManagerForMaxCn0() {
        Intent intent = new Intent(MAX_CN0_DATA_INTENT);
        if (this.mCn0ReportAlarmPI == null) {
            this.mCn0ReportAlarmPI = PendingIntent.getBroadcast(this.mContext, 0, intent, Dataspace.RANGE_FULL);
        }
        this.mAlarmManager.cancel(this.mCn0ReportAlarmPI);
        long triggerAtMillis = ALARM_TRIGGER_INTERVAL;
        this.mAlarmManager.setRepeating(2, triggerAtMillis, triggerAtMillis, this.mCn0ReportAlarmPI);
    }

    public void onLostLocation() {
        AbsIZatDCControllerAdapter absIZatDCControllerAdapter = this.mIZatAdapter;
        if (absIZatDCControllerAdapter != null && this.mDCStart && hasWhiteListApp) {
            absIZatDCControllerAdapter.lostLocation();
        }
        this.mDiagnoster.Log("onLostLocation");
    }

    private void onReportGpsLocation(Location location) {
        VivoLocationDiagnosticManager vivoLocationDiagnosticManager = this.mDiagnoster;
        vivoLocationDiagnosticManager.Log("GPS:" + location);
        this.mLastFixTime = SystemClock.elapsedRealtime();
        if (!this.mIsDrive && ((int) location.getSpeed()) > 10) {
            this.mIsDrive = true;
        }
        VivoDRManager vivoDRManager = this.mVivoDRManager;
        if (vivoDRManager != null && vivoDRManager.isDRStarted() && VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_BIG_DATA)) {
            this.mVivoDRManager.updateLocationChanged(location);
        } else if (VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_FUSED_LOCATION)) {
            this.mVivoCoreLocationManager.onReportGpsLocation(location);
        } else {
            this.mGnssLocationProvider.reportFusedLocation(location);
        }
        if (this.mVivoCn0WeakManager != null && VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_CN0_WEAK)) {
            this.mVivoCn0WeakManager.handleReportLocation((int) location.getSpeed());
        }
        if (this.mIZatAdapter != null && this.mDCStart && hasWhiteListApp && VivoLocationFeature.isFeatureSupport(VivoLocationFeature.FEATURE_BIG_DATA)) {
            this.mIZatAdapter.onReportLocation(location);
        }
    }

    @Override // com.android.server.location.VivoDRManager.VivoDRInterface
    public void reportDRLocation(Location location) {
        VivoLocationDiagnosticManager vivoLocationDiagnosticManager = this.mDiagnoster;
        vivoLocationDiagnosticManager.Log("DR:" + location);
        VLog.w("IVivoGnssLocationProviderVPDR", "reportDRLocation " + location);
        this.mLastFixTime = SystemClock.elapsedRealtime();
        this.mVivoCoreLocationManager.onReportGpsLocation(location);
    }

    private boolean isHasWhiteListApp(WorkSource source) {
        VLog.d(TAG, "IzatDC " + source.size());
        ArrayList<String> wPkgList = new ArrayList<>(VivoGpsStateMachine.getGnssWhiteList());
        int i = 0;
        while (true) {
            if (i >= source.size()) {
                return false;
            }
            String packageName = source.getName(i);
            if (packageName != null) {
                if (wPkgList.size() > 0) {
                    Iterator<String> it = wPkgList.iterator();
                    while (it.hasNext()) {
                        String pkgTemp = it.next();
                        if (pkgTemp != null && packageName.contains(pkgTemp)) {
                            VLog.d(TAG, "in wPkg list");
                            return true;
                        }
                    }
                } else {
                    String[] strArr = PACKAGE_FILTER_LIST;
                    if (strArr != null) {
                        for (String pkg : strArr) {
                            if (pkg.equals(packageName)) {
                                VLog.d(TAG, packageName + " in  V_W_Inner_Config List");
                                return true;
                            }
                        }
                    }
                }
                VLog.d(TAG, "pkg " + packageName + " check Failed! Not in W_List");
            }
            i++;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void saveToDataFile(final int maxCn0, final int maxDriveCn0, final int startNaviCount, final int cn0LostSuddenly, final int cn0JumpToZero, final String cn0StrengthRecord) {
        new Thread(new Runnable() { // from class: com.android.server.location.VivoGnssLocationProviderExt.6
            @Override // java.lang.Runnable
            public void run() {
                try {
                    String cn0Ab = startNaviCount + "," + cn0LostSuddenly + "," + cn0JumpToZero;
                    if (VivoGnssLocationProviderExt.this.mVCD != null && ((VivoCollectData) VivoGnssLocationProviderExt.this.mVCD).getControlInfo(VivoGnssLocationProviderExt.LOCATION_ID_FOR_SMALL_DATA)) {
                        HashMap<String, String> params = new HashMap<>();
                        params.clear();
                        params.put("MC", String.valueOf(maxCn0));
                        params.put("MDCW", String.valueOf(maxDriveCn0));
                        params.put("CA", cn0Ab);
                        params.put("CR", cn0StrengthRecord);
                        VLog.i(VivoGnssLocationProviderExt.TAG, "Cn0 data collect: " + params.toString());
                        HashMap<String, String> params1 = new HashMap<>(3);
                        params1.put(VCD_FF_1.UUID_STR, UUID.randomUUID().toString());
                        params1.put("MC", String.valueOf(maxCn0));
                        params1.put("MDCW", String.valueOf(maxDriveCn0));
                        params1.put("CA", cn0Ab);
                        params1.put("CR", cn0StrengthRecord);
                        EventTransfer.getInstance().singleEvent("F500", "F500|10010", System.currentTimeMillis(), 0L, params1);
                        ((VivoCollectData) VivoGnssLocationProviderExt.this.mVCD).writeData(VivoGnssLocationProviderExt.LOCATION_ID_FOR_SMALL_DATA, VivoGnssLocationProviderExt.MAX_CN0_LABEL, System.currentTimeMillis(), System.currentTimeMillis(), 0L, 1, params);
                    }
                } catch (Exception e) {
                    VLog.e(VivoGnssLocationProviderExt.TAG, Log.getStackTraceString(e));
                }
            }
        }).start();
    }

    private void onRestartGpsData(String buf) {
        try {
            if (this.mVCD != null && ((VivoCollectData) this.mVCD).getControlInfo(LOCATION_ID)) {
                HashMap<String, String> params = new HashMap<>();
                params.clear();
                params.put("vivoRestartGps", buf);
                ((VivoCollectData) this.mVCD).writeData(LOCATION_ID, RESTART_GPS_LABEL, System.currentTimeMillis(), System.currentTimeMillis(), 0L, 1, params);
                EventTransfer.getInstance().singleEvent("F500", "F500|10011", System.currentTimeMillis(), 0L, params);
            }
        } catch (Exception e) {
            VLog.e(TAG, "Exception: " + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void parseCn0StrengthConfig(ContentValuesList list) {
        this.mSvCount = Integer.parseInt(list.getValue("SvCount"));
        this.mCn0StrengthLevel = Integer.parseInt(list.getValue("Cn0StrengthLevel"));
        this.mNaviTimeLimit = Integer.parseInt(list.getValue("NaviTimeLimit"));
    }

    public void setDebug(boolean debug) {
        this.mVivoCoreLocationManager.setDebug(debug);
    }

    private String messageIdAsString(int message) {
        switch (message) {
            case 100:
                return "MSG_START_NAVIGATION";
            case 101:
                return "MSG_NO_SV_OR_FIX";
            case 102:
                return "MSG_SV_REPORT";
            case 103:
                return "MSG_STOP_GPS_REQUEST";
            case 104:
                return "MSG_RESTART_NAVIGATION";
            case 105:
                return "MSG_STOP_NAVIGATION";
            default:
                return "<Unknown>";
        }
    }
}