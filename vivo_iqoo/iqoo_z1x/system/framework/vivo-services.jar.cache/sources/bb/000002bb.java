package com.android.server.location;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.VIVORsaForLocation;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemProperties;
import android.telephony.CellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import com.vivo.common.utils.VLog;
import com.vivo.dr.IDMClient;
import com.vivo.dr.IDMService;
import com.vivo.face.common.data.Constants;
import com.vivo.services.rms.sdk.Consts;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/* loaded from: classes.dex */
public class VivoLocationDiagnosticManager {
    private static final String DIAGNOSTIC_DEBUG_PROPERTIES = "persist.sys.LocDiagDbg";
    private static final int EVENT_TYPE_EXCEPTION = 2;
    private static final int EVENT_TYPE_FIX = 0;
    private static final int EVENT_TYPE_XTRA = 1;
    private static final int FLAG_ALL = 31;
    private static final int FLAG_REF_LOC = 3;
    private static final int FLAG_REF_TIME = 4;
    private static final int FLAG_SV_COUNT = 1;
    private static final int FLAG_SV_STRONG = 0;
    private static final int FLAG_XTRA_VALID = 2;
    private static final int MAX_CACHED_LEN = 10240;
    private static final int MAX_CACHED_SIZE = 100;
    private static final int MAX_MAP_CACHED = 2;
    private static final int MAX_MAP_SIZE = 20;
    private static final int MSG_ADD_KET_VALUE = 7;
    private static final int MSG_CHECK_CLIENT = 3;
    private static final int MSG_CHECK_PACKAGE = 9;
    private static final int MSG_FINISHED = 2;
    private static final int MSG_FLUSH = 1;
    private static final int MSG_LOG = 0;
    private static final int MSG_REPORT_EVENT = 5;
    private static final int MSG_SERVICE_CONNECTED = 8;
    private static final int MSG_UPDATE_CONFIG = 6;
    private static final int UNBIND_DELAY_TIME_MS = 180000;
    private VivoLocationFeatureConfig mConfig;
    private ConnectivityManager mConnMgr;
    private Context mContext;
    private IDMService mDMService;
    private Handler mHandler;
    private TelephonyManager mTelephonyManager;
    private WifiManager mWifiManager;
    private static final String TAG = "VivoLocationDiagnosticManager";
    public static boolean D = VLog.isLoggable(TAG, 3);
    private static VivoLocationDiagnosticManager sDiagnosticManager = null;
    private final String DR_PKG_NAME = "com.vivo.dr";
    private final String CLS_NAME = "com.vivo.dr.LocationDiagnosticService";
    private ComponentName mComponentName = new ComponentName("com.vivo.dr", "com.vivo.dr.LocationDiagnosticService");
    private boolean inited = false;
    private boolean mDMPkgInstalled = false;
    private Object mLock = new Object();
    private StringBuffer sb = null;
    private HashMap<String, String> mKeyMaps = new HashMap<>();
    private List<String> mLogCached = new ArrayList();
    private SimpleDateFormat formatter = new SimpleDateFormat("MM-dd HH:mm:ss.SSS ");
    private Date mDate = new Date(System.currentTimeMillis());
    private boolean mEnableByServer = false;
    private HashMap<String, String> mConfigs = new HashMap<>();
    private HashMap<String, String> mMaps = new HashMap<>();
    private int mEventType = -1;
    private String mEventStack = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    private boolean bConfigPendding = false;
    private boolean bLogPendding = false;
    private boolean bKeyPendding = false;
    private boolean bEventPendding = false;
    private boolean mIsSvStrong = false;
    private int mSvCount = 0;
    private boolean mXtraStatus = false;
    private long mXtraUpdateTime = 0;
    private Location mRefLoc = null;
    private long mRefLocUpdateTime = 0;
    private String mRefTimeType = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    private long mRefTimeUpdateTime = 0;
    private boolean mRefTimeValid = false;
    private boolean mAlreadyRecordEncryptedStr = false;
    private HashMap<String, Location> mLocations = new HashMap<>();
    private BroadcastReceiver mWifiScanReceiver = null;
    private boolean mListenWifiChanged = false;
    private long mLastRecordWifiTime = 0;
    private long mLastRecordWifiCount = 0;
    private long mLastScanTime = 0;
    private PhoneStateListener mCellListener = null;
    private boolean mListenCellChanged = false;
    private ServiceConnection mDiagClientConnection = new ServiceConnection() { // from class: com.android.server.location.VivoLocationDiagnosticManager.3
        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (VivoLocationDiagnosticManager.D) {
                VLog.d(VivoLocationDiagnosticManager.TAG, "onServiceConnected " + name);
            }
            if (service != null) {
                try {
                    VivoLocationDiagnosticManager.this.mDMService = IDMService.Stub.asInterface(service);
                    if (VivoLocationDiagnosticManager.this.mDMService != null) {
                        VivoLocationDiagnosticManager.this.mDMService.setCallback(VivoLocationDiagnosticManager.this.mDMClient);
                        VivoLocationDiagnosticManager.this.mHandler.sendEmptyMessage(8);
                    }
                } catch (Exception e) {
                    VLog.e(VivoLocationDiagnosticManager.TAG, e.toString());
                }
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            VLog.d(VivoLocationDiagnosticManager.TAG, "onServiceDisconnected " + name);
            VivoLocationDiagnosticManager.this.mDMService = null;
        }
    };
    private final IDMClient mDMClient = new IDMClient.Stub() { // from class: com.android.server.location.VivoLocationDiagnosticManager.4
        @Override // com.vivo.dr.IDMClient
        public void callbackFinished(boolean success) {
            if (VivoLocationDiagnosticManager.D) {
                VLog.d(VivoLocationDiagnosticManager.TAG, "callbackFinished success:" + success);
            }
            VivoLocationDiagnosticManager.this.mHandler.removeMessages(2);
            VivoLocationDiagnosticManager.this.mHandler.sendEmptyMessageDelayed(2, 180000L);
        }
    };

    public static VivoLocationDiagnosticManager getInstance() {
        if (sDiagnosticManager == null) {
            sDiagnosticManager = new VivoLocationDiagnosticManager();
        }
        return sDiagnosticManager;
    }

    private VivoLocationDiagnosticManager() {
    }

    public void init(Context context) {
        if (context == null) {
            return;
        }
        this.mContext = context;
        this.sb = new StringBuffer((int) MAX_CACHED_LEN);
        MyHandler myHandler = new MyHandler(VivoLocThread.getInstance().getDiagnosticThreadHandler().getLooper());
        this.mHandler = myHandler;
        myHandler.sendEmptyMessageDelayed(9, 3000L);
        this.mConnMgr = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        if (this.mWifiScanReceiver == null) {
            this.mWifiScanReceiver = new BroadcastReceiver() { // from class: com.android.server.location.VivoLocationDiagnosticManager.1
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context c, Intent intent) {
                    boolean success = intent.getBooleanExtra("resultsUpdated", false);
                    VivoLocationDiagnosticManager.this.mLastRecordWifiTime = System.currentTimeMillis();
                    long scanTime = VivoLocationDiagnosticManager.this.mLastRecordWifiTime - VivoLocationDiagnosticManager.this.mLastScanTime;
                    VivoLocationDiagnosticManager vivoLocationDiagnosticManager = VivoLocationDiagnosticManager.this;
                    vivoLocationDiagnosticManager.mLastRecordWifiCount = vivoLocationDiagnosticManager.mWifiManager.getScanResults().size();
                    if (VivoLocationDiagnosticManager.this.mLastScanTime != 0) {
                        VivoLocationDiagnosticManager vivoLocationDiagnosticManager2 = VivoLocationDiagnosticManager.this;
                        vivoLocationDiagnosticManager2.Log("scan_succ:" + success + " wifi_count:" + VivoLocationDiagnosticManager.this.mLastRecordWifiCount + " scan_time:" + scanTime);
                    }
                    if (VivoLocationDiagnosticManager.D) {
                        VLog.d(VivoLocationDiagnosticManager.TAG, "scan_succ:" + success + " wifi_count:" + VivoLocationDiagnosticManager.this.mLastRecordWifiCount + " scan_time:" + scanTime);
                    }
                    VivoLocationDiagnosticManager.this.mLastScanTime = 0L;
                }
            };
        }
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        if (this.mCellListener == null) {
            this.mCellListener = new PhoneStateListener() { // from class: com.android.server.location.VivoLocationDiagnosticManager.2
                @Override // android.telephony.PhoneStateListener
                public void onCellInfoChanged(List<CellInfo> cellInfo) {
                    if (cellInfo == null) {
                        if (VivoLocationDiagnosticManager.D) {
                            VLog.d(VivoLocationDiagnosticManager.TAG, "update cellinfo null");
                        }
                        VivoLocationDiagnosticManager.this.Log("updateCellInfo:0");
                        return;
                    }
                    if (VivoLocationDiagnosticManager.D) {
                        VLog.d(VivoLocationDiagnosticManager.TAG, "update cellinfo, count:" + cellInfo.size());
                    }
                    VivoLocationDiagnosticManager vivoLocationDiagnosticManager = VivoLocationDiagnosticManager.this;
                    vivoLocationDiagnosticManager.Log("updateCellInfo:" + cellInfo.toString());
                }
            };
        }
        this.mConfig = VivoLocationFeatureConfig.getInstance();
        this.inited = true;
    }

    public void startNetworkLocating(boolean start) {
        long token;
        boolean isApEnabled;
        boolean data;
        boolean wifi;
        long wifiAge;
        StringBuilder sb;
        if (!start) {
            Log("onStopNetworkLocation.");
            return;
        }
        long token2 = Binder.clearCallingIdentity();
        try {
            try {
                isApEnabled = this.mWifiManager.isWifiApEnabled();
                NetworkInfo wifiInfo = this.mConnMgr.getNetworkInfo(1);
                data = false;
                NetworkInfo mobileInfo = this.mConnMgr.getNetworkInfo(0);
                wifi = wifiInfo == null ? false : wifiInfo.isConnected();
                if (mobileInfo != null) {
                    data = mobileInfo.isConnected();
                }
                wifiAge = this.mLastRecordWifiTime == 0 ? 0L : System.currentTimeMillis() - this.mLastRecordWifiTime;
                sb = new StringBuilder();
                sb.append("onStartNetworkLocation, wifi_conn:");
                sb.append(wifi);
                sb.append(" data_conn:");
                sb.append(data);
                sb.append(" ap_enabled:");
                sb.append(isApEnabled);
                sb.append(" wifi_count:");
                token = token2;
            } catch (Exception e) {
                e = e;
                token = token2;
            } catch (Throwable th) {
                th = th;
                token = token2;
                Binder.restoreCallingIdentity(token);
                throw th;
            }
            try {
                sb.append(this.mLastRecordWifiCount);
                sb.append(" wifi_age:");
                sb.append(wifiAge);
                Log(sb.toString());
                if (D) {
                    VLog.d(TAG, "onStartNetworkLocation, wifi_conn:" + wifi + " data_conn:" + data + " ap_enabled:" + isApEnabled + " wifi_count:" + this.mLastRecordWifiCount + " wifi_age:" + wifiAge);
                }
            } catch (Exception e2) {
                e = e2;
                VLog.d(TAG, "get network status error : " + e);
                Binder.restoreCallingIdentity(token);
            }
            Binder.restoreCallingIdentity(token);
        } catch (Throwable th2) {
            th = th2;
            Binder.restoreCallingIdentity(token);
            throw th;
        }
    }

    public void startScan(String packageName) {
        if (this.mLastScanTime == 0) {
            this.mLastScanTime = System.currentTimeMillis();
            Log("startScan:" + packageName);
        }
        if (D) {
            VLog.d(TAG, "startScan:" + packageName + " Time:" + this.mLastScanTime);
        }
    }

    public void startNavigation(int interval) {
        if (this.inited) {
            Log("onStartNavigating, interval:" + interval);
            this.mIsSvStrong = false;
            this.mSvCount = 0;
            this.mXtraStatus = false;
            this.mRefLoc = null;
            this.mRefTimeValid = false;
        }
    }

    public void Log(String log) {
        if (this.inited) {
            StringBuilder formatLog = new StringBuilder();
            formatLog.append(String.valueOf(System.currentTimeMillis()));
            formatLog.append(" ");
            formatLog.append(Process.myPid());
            formatLog.append(" ");
            formatLog.append(Process.myTid());
            formatLog.append(" ");
            formatLog.append(log);
            formatLog.append("\n");
            this.sb.append((CharSequence) formatLog);
            if (this.sb.length() > MAX_CACHED_LEN) {
                flushLog();
            }
        }
    }

    private void flushLog() {
        this.mHandler.obtainMessage(1, this.sb.toString()).sendToTarget();
        StringBuffer stringBuffer = this.sb;
        stringBuffer.delete(0, stringBuffer.length());
    }

    public void addKeyValue(String key, String value) {
        if (D) {
            VLog.d(TAG, "key:" + key + ", value:" + value);
        }
        this.mKeyMaps.put(key, value);
        if (this.mKeyMaps.size() > 20) {
            if (this.inited) {
                this.mHandler.obtainMessage(7, new HashMap(this.mKeyMaps)).sendToTarget();
            }
            this.mKeyMaps.clear();
        }
    }

    public void reportEvent(int type, String stack) {
        if (this.inited) {
            if (D) {
                VLog.d(TAG, "report event:" + type + " stack:" + stack);
            }
            this.mHandler.obtainMessage(5, type, 0, stack).sendToTarget();
        }
    }

    public void reportLocation(String provider, Location location) {
        if (this.inited) {
            if (!this.mAlreadyRecordEncryptedStr) {
                addKeyValue("encrypted", VIVORsaForLocation.getInstance().getEncodedStr());
                Log("encrypted:" + VIVORsaForLocation.getInstance().getEncodedStr());
                this.mAlreadyRecordEncryptedStr = true;
            }
            if (this.mLocations.get(provider) != location && !"passive".equals(provider)) {
                this.mLocations.put(provider, location);
                Log("incoming " + provider + " " + location.toString());
                if (D) {
                    VLog.d(TAG, "incoming " + provider);
                }
            }
        }
    }

    public void updateSvStatus(int count, int topCn0) {
        if (this.inited) {
            Log("report sv, counts:" + count + " top6 cn0:" + topCn0);
            if (topCn0 > 30) {
                this.mIsSvStrong = true;
            } else {
                this.mIsSvStrong = false;
            }
            this.mSvCount = count;
        }
    }

    public void updateXtraStatus(boolean valid, long updateTime) {
        Log("update xtra:" + valid + " updateTime:" + updateTime);
        this.mXtraStatus = valid;
        this.mXtraUpdateTime = updateTime;
    }

    public void updateRefLocStatus(Location refLoc, long updateTime) {
        Log("update ref loc:" + refLoc + " updateTime:" + updateTime);
        this.mRefLoc = refLoc;
        this.mRefLocUpdateTime = updateTime;
    }

    public void updateRefTimeStatus(boolean valid, String type, long updateTime) {
        Log("update ref time, valid:" + valid + " type:" + type + " updateTime:" + updateTime);
        this.mRefTimeValid = valid;
        this.mRefTimeType = type;
        this.mRefTimeUpdateTime = updateTime;
    }

    public void onConfigChange(HashMap<String, String> maps) {
        if (D) {
            VLog.d(TAG, "Diagnoster config changed, config:" + maps.toString());
        }
        this.mConfigs = maps;
        boolean equals = maps.get("switchState") == null ? false : this.mConfigs.get("switchState").equals("on");
        this.mEnableByServer = equals;
        if (!equals) {
            this.mHandler.sendEmptyMessageDelayed(2, 180000L);
            if (this.mListenWifiChanged) {
                VLog.d(TAG, "remove wifi listener.");
                this.mContext.unregisterReceiver(this.mWifiScanReceiver);
                this.mListenWifiChanged = false;
            }
            if (this.mListenCellChanged) {
                VLog.d(TAG, "remove cell listener.");
                this.mTelephonyManager.listen(this.mCellListener, 0);
                this.mListenCellChanged = false;
            }
        } else {
            if (!this.mListenWifiChanged) {
                VLog.d(TAG, "add wifi listener.");
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction("android.net.wifi.SCAN_RESULTS");
                this.mContext.registerReceiver(this.mWifiScanReceiver, intentFilter);
                this.mListenWifiChanged = true;
            }
            if (!this.mListenCellChanged) {
                VLog.d(TAG, "add cell listener.");
                this.mTelephonyManager.listen(this.mCellListener, Consts.ProcessStates.FOCUS);
                this.mListenCellChanged = true;
            }
        }
        if (this.inited) {
            flushLog();
        }
        this.mHandler.obtainMessage(6).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class MyHandler extends Handler {
        MyHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (VivoLocationDiagnosticManager.D) {
                VLog.d(VivoLocationDiagnosticManager.TAG, "handleMessage " + msg.what);
            }
            try {
                switch (msg.what) {
                    case 1:
                        VivoLocationDiagnosticManager.this.flush((String) msg.obj);
                        return;
                    case 2:
                        VivoLocationDiagnosticManager.this.releaseClient();
                        return;
                    case 3:
                        VivoLocationDiagnosticManager.this.serverReady();
                        return;
                    case 4:
                    default:
                        return;
                    case 5:
                        VivoLocationDiagnosticManager.this.checkAndReportEvent(msg.arg1, (String) msg.obj);
                        return;
                    case 6:
                        VivoLocationDiagnosticManager.this.sendConfig();
                        return;
                    case 7:
                        VivoLocationDiagnosticManager.this.sendMaps((HashMap) msg.obj);
                        return;
                    case 8:
                        VivoLocationDiagnosticManager.this.dealWithService();
                        return;
                    case 9:
                        VivoLocationDiagnosticManager.this.mDMPkgInstalled = VivoLocationDiagnosticManager.this.checkPackageExists("com.vivo.dr");
                        return;
                }
            } catch (Exception e) {
                VLog.e(VivoLocationDiagnosticManager.TAG, e.toString());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void flush(String log) {
        if (!this.mEnableByServer) {
            return;
        }
        if (log != null && !log.isEmpty()) {
            this.mLogCached.add(log);
        }
        try {
            if (this.mLogCached.size() >= 80.0d) {
                if (!serverReady()) {
                    VLog.w(TAG, "not flush log because current not bind DMService.");
                    this.bLogPendding = true;
                } else {
                    if (D) {
                        VLog.d(TAG, "start flushing log to DMService.");
                    }
                    for (String l : this.mLogCached) {
                        this.mDMService.recordLoopLog(l);
                    }
                    this.mLogCached.clear();
                    this.bLogPendding = false;
                    this.mAlreadyRecordEncryptedStr = false;
                }
            }
        } catch (Exception e) {
            VLog.e(TAG, e.toString());
        }
        while (this.mLogCached.size() > 100) {
            this.mLogCached.remove(0);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Code restructure failed: missing block: B:16:0x005e, code lost:
        if (r1 <= 2) goto L17;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public void sendMaps(java.util.HashMap<java.lang.String, java.lang.String> r8) {
        /*
            r7 = this;
            boolean r0 = r7.mEnableByServer
            if (r0 != 0) goto L5
            return
        L5:
            java.lang.String r0 = "VivoLocationDiagnosticManager"
            if (r8 == 0) goto L62
            java.util.HashMap<java.lang.String, java.lang.String> r1 = r7.mMaps
            r1.putAll(r8)
            java.util.HashMap<java.lang.String, java.lang.String> r1 = r7.mMaps
            int r1 = r1.size()
            r2 = 2
            if (r1 <= r2) goto L62
            java.util.HashMap<java.lang.String, java.lang.String> r3 = r7.mMaps
            java.util.Set r3 = r3.entrySet()
            java.util.Iterator r3 = r3.iterator()
        L21:
            boolean r4 = r3.hasNext()
            if (r4 == 0) goto L62
            java.lang.Object r4 = r3.next()
            java.util.Map$Entry r4 = (java.util.Map.Entry) r4
            boolean r5 = com.android.server.location.VivoLocationDiagnosticManager.D
            if (r5 == 0) goto L59
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            java.lang.String r6 = "not save key:"
            r5.append(r6)
            java.lang.Object r6 = r4.getKey()
            java.lang.String r6 = (java.lang.String) r6
            r5.append(r6)
            java.lang.String r6 = ", value:"
            r5.append(r6)
            java.lang.Object r6 = r4.getValue()
            java.lang.String r6 = (java.lang.String) r6
            r5.append(r6)
            java.lang.String r5 = r5.toString()
            com.vivo.common.utils.VLog.d(r0, r5)
        L59:
            r3.remove()
            int r1 = r1 + (-1)
            if (r1 > r2) goto L61
            goto L62
        L61:
            goto L21
        L62:
            boolean r1 = r7.serverReady()     // Catch: java.lang.Exception -> L92
            if (r1 == 0) goto L89
            boolean r1 = com.android.server.location.VivoLocationDiagnosticManager.D     // Catch: java.lang.Exception -> L92
            if (r1 == 0) goto L71
            java.lang.String r1 = "start send keymaps to DMService."
            com.vivo.common.utils.VLog.d(r0, r1)     // Catch: java.lang.Exception -> L92
        L71:
            java.util.HashMap<java.lang.String, java.lang.String> r1 = r7.mMaps     // Catch: java.lang.Exception -> L92
            int r1 = r1.size()     // Catch: java.lang.Exception -> L92
            if (r1 <= 0) goto L80
            com.vivo.dr.IDMService r1 = r7.mDMService     // Catch: java.lang.Exception -> L92
            java.util.HashMap<java.lang.String, java.lang.String> r2 = r7.mMaps     // Catch: java.lang.Exception -> L92
            r1.recordKeyInfo(r2)     // Catch: java.lang.Exception -> L92
        L80:
            java.util.HashMap<java.lang.String, java.lang.String> r1 = r7.mMaps     // Catch: java.lang.Exception -> L92
            r1.clear()     // Catch: java.lang.Exception -> L92
            r1 = 0
            r7.bKeyPendding = r1     // Catch: java.lang.Exception -> L92
            goto L91
        L89:
            java.lang.String r1 = "not send keymaps because current not bind DMService."
            com.vivo.common.utils.VLog.w(r0, r1)     // Catch: java.lang.Exception -> L92
            r1 = 1
            r7.bKeyPendding = r1     // Catch: java.lang.Exception -> L92
        L91:
            goto L9a
        L92:
            r1 = move-exception
            java.lang.String r2 = r1.toString()
            com.vivo.common.utils.VLog.e(r0, r2)
        L9a:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.location.VivoLocationDiagnosticManager.sendMaps(java.util.HashMap):void");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkAndReportEvent(int type, String stack) {
        int checkFlag = 65535;
        if (!this.mIsSvStrong) {
            Log("sv signal is weak, maybe can't get fix is normal case.");
            checkFlag = 65535 & (-2);
        }
        if (this.mSvCount < 5) {
            Log("too few SVs:" + this.mSvCount + ", maybe can't get fix is normal case.");
            checkFlag &= -3;
        }
        if (!this.mXtraStatus) {
            Log("lack of xtra data, maybe can't get fix is normal case.");
            checkFlag &= -5;
        }
        if (this.mRefLoc == null) {
            Log("lack of ref loc, maybe can't get fix is normal case.");
            checkFlag &= -9;
        }
        if (!this.mRefTimeValid) {
            Log("lack of ref time, maybe can't get fix is normal case.");
            checkFlag &= -17;
        }
        if (SystemProperties.getInt(DIAGNOSTIC_DEBUG_PROPERTIES, 0) != 0) {
            int checkFlag2 = checkFlag | SystemProperties.getInt(DIAGNOSTIC_DEBUG_PROPERTIES, 0);
            VLog.d(TAG, "location diagnostic set debug flag:" + checkFlag2);
        }
        triggerCloudDiagnostic(type, stack);
    }

    private void triggerCloudDiagnostic(int type, String stack) {
        this.mEventType = type;
        this.mEventStack = stack;
        try {
            if (serverReady()) {
                flush(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                sendMaps(null);
                if (D) {
                    VLog.d(TAG, "reportLocationEvent");
                }
                this.mDMService.reportLocationEvent(this.mEventType, this.mEventStack);
                this.bEventPendding = false;
                return;
            }
            VLog.w(TAG, "report fail because current not bind DMService, event:" + type);
            this.bEventPendding = true;
        } catch (Exception e) {
            VLog.e(TAG, e.toString());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendConfig() {
        try {
            if (!serverReady()) {
                VLog.w(TAG, "sendConfig fail because current not bind DMService");
                this.bConfigPendding = true;
                return;
            }
            if (D) {
                VLog.d(TAG, "updateConfig");
            }
            this.mDMService.updateConfig(this.mConfigs);
            this.bConfigPendding = false;
        } catch (Exception e) {
            VLog.e(TAG, e.toString());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dealWithService() {
        if (this.bConfigPendding) {
            sendConfig();
        }
        if (this.bLogPendding) {
            flush(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        }
        if (this.bKeyPendding) {
            sendMaps(null);
        }
        if (this.bEventPendding) {
            triggerCloudDiagnostic(this.mEventType, this.mEventStack);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean serverReady() {
        if (!this.mEnableByServer) {
            if (D) {
                VLog.d(TAG, "location diagnostic disabled.");
            }
            return false;
        }
        this.mHandler.removeMessages(2);
        if (this.mDMService == null) {
            if (this.mDMPkgInstalled) {
                bindVivoDMService(false);
            }
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void releaseClient() {
        if (this.mDMService != null) {
            unbindVivoDMService();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Code restructure failed: missing block: B:12:0x0021, code lost:
        if (r3.isEmpty() == false) goto L17;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public boolean checkPackageExists(java.lang.String r6) {
        /*
            r5 = this;
            r0 = 1
            r1 = 0
            if (r6 == 0) goto Lc
            boolean r2 = r6.isEmpty()
            if (r2 != 0) goto Lc
            r2 = r0
            goto Ld
        Lc:
            r2 = r1
        Ld:
            if (r2 == 0) goto L29
            android.content.Context r3 = r5.mContext     // Catch: java.lang.Exception -> L27
            android.content.pm.PackageManager r3 = r3.getPackageManager()     // Catch: java.lang.Exception -> L27
            android.content.pm.PackageInfo r3 = r3.getPackageInfo(r6, r1)     // Catch: java.lang.Exception -> L27
            java.lang.String r3 = r3.versionName     // Catch: java.lang.Exception -> L27
            if (r3 == 0) goto L24
            boolean r4 = r3.isEmpty()     // Catch: java.lang.Exception -> L27
            if (r4 != 0) goto L24
            goto L25
        L24:
            r0 = r1
        L25:
            r2 = r0
            goto L29
        L27:
            r0 = move-exception
            r2 = 0
        L29:
            boolean r0 = com.android.server.location.VivoLocationDiagnosticManager.D
            if (r0 == 0) goto L43
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r1 = "checkPackageExists "
            r0.append(r1)
            r0.append(r2)
            java.lang.String r0 = r0.toString()
            java.lang.String r1 = "VivoLocationDiagnosticManager"
            com.vivo.common.utils.VLog.d(r1, r0)
        L43:
            return r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.location.VivoLocationDiagnosticManager.checkPackageExists(java.lang.String):boolean");
    }

    private void bindVivoDMService(boolean force) {
        if (D) {
            VLog.d(TAG, "bindVivoDMService force:" + force + ", mDMPkgInstalled:" + this.mDMPkgInstalled + ", mDMService:" + this.mDMService);
        }
        try {
            if (this.mDMPkgInstalled) {
                if (this.mDMService == null || force) {
                    Intent intent = new Intent();
                    intent.setComponent(this.mComponentName);
                    this.mContext.bindService(intent, this.mDiagClientConnection, 1);
                }
            }
        } catch (Exception e) {
            VLog.e(TAG, e.toString());
        }
    }

    private void unbindVivoDMService() {
        if (D) {
            VLog.d(TAG, "unbindVivoDMService");
        }
        try {
            if (this.mDMService != null) {
                this.mContext.unbindService(this.mDiagClientConnection);
                this.mDMService = null;
            }
        } catch (Exception e) {
            VLog.e(TAG, e.toString());
        }
    }
}