package com.android.server.location;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.graphics.common.V1_0.Dataspace;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import com.android.server.UnifiedConfigThread;
import com.android.server.am.EmergencyBroadcastManager;
import com.android.server.location.VivoLocConf;
import com.android.server.policy.VivoPolicyConstant;
import com.android.server.wm.VCD_FF_1;
import com.vivo.common.VivoCollectData;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import com.vivo.vcodetransbase.EventTransfer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import vivo.app.configuration.ContentValuesList;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public final class VivoLocationManagerServiceUtils {
    private static final int MSG_INIT_DATA_ALARM = 1000;
    private static final int MSG_REFRESH_BACKGROUND_WHITE_LIST = 1003;
    private static final int MSG_REPORT_REQ_LOCATION_BD_COLLECT = 1001;
    private static final int MSG_SAVE_DATA_TO_DATABASE = 1002;
    public static final String NLP_OPTIMIZE_CLOUD_SWITCH_PROP = "persist.sys.nlp.cloud.swtich";
    private static final String NLP_OPTIMIZE_CLOUD_SWITCH_SETTING = "vivo.nlp.optimize.cloud.switch";
    private static final String NLP_OPTIMIZE_W_PKG_LIST_LOCAL_FILE_PATH = "/data/bbkcore/nlp_optimize_w_pkgs_local.xml";
    private static final String REPORT_REQ_LOCATION_BD_COLLECT = "com.vivo.REPORT_REQ_LOCATION_BD";
    private static final String SYS_APP_LIST = "sys_app_list";
    private static final String TAG = "VivoLocationUtils";
    private static final String T_PART_APP_LIST = "t_part_app_list";
    private Context mContext;
    VivoLocationManagerServiceExt mLocationService;
    private HandlerThread mLocationUtilsHThread;
    public LocationUtilsHandler mLocationUtilsHandler;
    private boolean mNLpOptimizeCloudSwitchIsOpen;
    private AlarmManager mSaveDataAlarmManager;
    PendingIntent mSaveDataAlarmPI;
    private Object mVCD;
    private static boolean DEBUG = false;
    private static int APP_REQUEST_LOCATION_MAX_REPORT = 20;
    static boolean NLP_LOCATION_REPORT_SWITCH = true;
    static boolean CONTROL_GPS_LISTENER = false;
    private ArrayList<String> mNlpOptimizeWPkgList = new ArrayList<>();
    private ArrayList<String> mBackgroudWhiteList = new ArrayList<>();
    private Handler mConfigHandler = UnifiedConfigThread.getHandler();
    private HashMap<String, RequestReport> mRequestReportMap = new HashMap<>();
    private ArrayList<String> mRequestReportToDBList = new ArrayList<>();
    private Object mReportLock = new Object();
    private long REPORT_TO_DB_INTERVAL = 604800000;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.location.VivoLocationManagerServiceUtils.2
        {
            VivoLocationManagerServiceUtils.this = this;
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            VLog.d(VivoLocationManagerServiceUtils.TAG, "receive broadcast intent, action: " + action);
            if (action == null) {
                return;
            }
            if (action.equals(VivoPolicyConstant.ACTION_VIVO_LOG_CHANGED)) {
                String adbStatus = intent.getStringExtra("adblog_status");
                if ("on".equals(adbStatus)) {
                    boolean unused = VivoLocationManagerServiceUtils.DEBUG = true;
                } else {
                    boolean unused2 = VivoLocationManagerServiceUtils.DEBUG = false;
                }
            } else if (action.equals(VivoLocationManagerServiceUtils.REPORT_REQ_LOCATION_BD_COLLECT)) {
                Message msg = VivoLocationManagerServiceUtils.this.mLocationUtilsHandler.obtainMessage(1002);
                VivoLocationManagerServiceUtils.this.mLocationUtilsHandler.sendMessage(msg);
            }
        }
    };

    /* renamed from: lambda$s8ZOv1w-NN_o8e32amc-OBlBWDw */
    public static /* synthetic */ void m3lambda$s8ZOv1wNN_o8e32amcOBlBWDw(VivoLocationManagerServiceUtils vivoLocationManagerServiceUtils, ContentValuesList contentValuesList) {
        vivoLocationManagerServiceUtils.parseNLPListConfigFromString(contentValuesList);
    }

    public VivoLocationManagerServiceUtils(final Context context, VivoLocationManagerServiceExt locationManagerService) {
        this.mNLpOptimizeCloudSwitchIsOpen = true;
        this.mContext = null;
        this.mVCD = null;
        this.mContext = context;
        this.mLocationService = locationManagerService;
        HandlerThread handlerThread = new HandlerThread("VivoLocationUtilsHandler");
        this.mLocationUtilsHThread = handlerThread;
        handlerThread.start();
        this.mLocationUtilsHandler = new LocationUtilsHandler(this.mLocationUtilsHThread.getLooper());
        String nlpOptimizeSwitch = SystemProperties.get(NLP_OPTIMIZE_CLOUD_SWITCH_PROP, "unknow");
        if ("unknow".equals(nlpOptimizeSwitch)) {
            VSlog.w(TAG, "## " + nlpOptimizeSwitch + " , the config may not download to local.");
        } else if ("true".equals(nlpOptimizeSwitch)) {
            this.mNLpOptimizeCloudSwitchIsOpen = true;
        } else if ("false".equals(nlpOptimizeSwitch)) {
            this.mNLpOptimizeCloudSwitchIsOpen = false;
        }
        this.mLocationUtilsHandler.post(new Runnable() { // from class: com.android.server.location.VivoLocationManagerServiceUtils.1
            {
                VivoLocationManagerServiceUtils.this = this;
            }

            @Override // java.lang.Runnable
            public void run() {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction("android.intent.action.BOOT_COMPLETED");
                intentFilter.addAction(VivoPolicyConstant.ACTION_VIVO_LOG_CHANGED);
                intentFilter.addAction(VivoLocationManagerServiceUtils.REPORT_REQ_LOCATION_BD_COLLECT);
                context.registerReceiver(VivoLocationManagerServiceUtils.this.mBroadcastReceiver, intentFilter, null, VivoLocationManagerServiceUtils.this.mLocationUtilsHandler);
            }
        });
        VSlog.i(TAG, "nlpOptimizeSwitch " + nlpOptimizeSwitch + " val:" + this.mNLpOptimizeCloudSwitchIsOpen);
        this.mVCD = getVCD(context);
        this.mSaveDataAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mSaveDataAlarmPI = null;
        VivoLocConf config = VivoLocConf.getInstance();
        config.registerListener(VivoLocConf.NETWORK_LOCATION_PROVIDER, new VivoLocConf.ContentValuesListChangedListener() { // from class: com.android.server.location.-$$Lambda$VivoLocationManagerServiceUtils$s8ZOv1w-NN_o8e32amc-OBlBWDw
            @Override // com.android.server.location.VivoLocConf.ContentValuesListChangedListener
            public final void onConfigChanged(ContentValuesList contentValuesList) {
                VivoLocationManagerServiceUtils.m3lambda$s8ZOv1wNN_o8e32amcOBlBWDw(VivoLocationManagerServiceUtils.this, contentValuesList);
            }
        });
    }

    private Object getVCD(Context context) {
        if (this.mVCD == null) {
            this.mVCD = new VivoCollectData(context);
        }
        return this.mVCD;
    }

    /* loaded from: classes.dex */
    public class LocationUtilsHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public LocationUtilsHandler(Looper looper) {
            super(looper);
            VivoLocationManagerServiceUtils.this = this$0;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            VSlog.i(VivoLocationManagerServiceUtils.TAG, "--handleMessage " + msg.what);
            switch (msg.what) {
                case 1000:
                    VivoLocationManagerServiceUtils.this.setSaveDataAlarmManager();
                    return;
                case 1001:
                    RequestReport requestReport = (RequestReport) msg.obj;
                    if (requestReport != null) {
                        synchronized (VivoLocationManagerServiceUtils.this.mReportLock) {
                            try {
                                VivoLocationManagerServiceUtils.this.reportAppRequestLocationLock(requestReport);
                            } catch (Exception e) {
                                VSlog.e(VivoLocationManagerServiceUtils.TAG, "report check " + e.toString());
                            }
                        }
                        return;
                    }
                    return;
                case 1002:
                    VivoLocationManagerServiceUtils.this.handleReportAppRequestLocationToBigDataDatabase();
                    return;
                case 1003:
                    VivoLocationManagerServiceUtils.this.handleRefreshBackgroudWhiteList();
                    return;
                default:
                    return;
            }
        }
    }

    public void setSaveDataAlarmManager() {
        Intent intent = new Intent(REPORT_REQ_LOCATION_BD_COLLECT);
        if (this.mSaveDataAlarmPI == null) {
            this.mSaveDataAlarmPI = PendingIntent.getBroadcast(this.mContext, 0, intent, Dataspace.RANGE_FULL);
        }
        this.mSaveDataAlarmManager.cancel(this.mSaveDataAlarmPI);
        long triggerAtMillis = this.REPORT_TO_DB_INTERVAL;
        this.mSaveDataAlarmManager.setRepeating(2, triggerAtMillis, triggerAtMillis, this.mSaveDataAlarmPI);
    }

    public ArrayList<String> getNLPOptimizeWPkgList() {
        return this.mNlpOptimizeWPkgList;
    }

    public boolean getNlpOptimizeCloudSwitchValue() {
        return this.mNLpOptimizeCloudSwitchIsOpen;
    }

    public boolean getControlGpsListeners() {
        boolean control = CONTROL_GPS_LISTENER;
        return control;
    }

    /* JADX WARN: Removed duplicated region for block: B:134:0x012d  */
    /* JADX WARN: Removed duplicated region for block: B:135:0x01b4  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public void parseNLPListConfigFromString(vivo.app.configuration.ContentValuesList r21) {
        /*
            Method dump skipped, instructions count: 473
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.location.VivoLocationManagerServiceUtils.parseNLPListConfigFromString(vivo.app.configuration.ContentValuesList):void");
    }

    /* loaded from: classes.dex */
    public static class RequestReport {
        public long reqFusedCount;
        public long reqGpsCount;
        public long reqNetworkCount;
        public long reqPassiveCount;
        public String requestCallerPkgName;
        public String requestProvider;

        public RequestReport(String pkgName, String providerName, int networkCount, int gpsCount, int passiveCount, int fussCount) {
            this.reqPassiveCount = 0L;
            this.reqFusedCount = 0L;
            this.requestCallerPkgName = pkgName;
            this.requestProvider = providerName;
            this.reqNetworkCount = networkCount;
            this.reqGpsCount = gpsCount;
            this.reqPassiveCount = passiveCount;
            this.reqFusedCount = fussCount;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("pk:" + this.requestCallerPkgName);
            sb.append(" n:" + this.reqNetworkCount);
            sb.append(" g:" + this.reqGpsCount);
            sb.append(" pa:" + this.reqPassiveCount);
            sb.append(" fu:" + this.reqFusedCount);
            return sb.toString();
        }
    }

    public void vivoPreReportAppRequestLocation(String requestPackageName, String providerName, int uid) {
        if (providerName == null) {
            VSlog.w(TAG, "provider name must not be null");
        } else if (!NLP_LOCATION_REPORT_SWITCH) {
            if (DEBUG) {
                VSlog.i(TAG, "Device is not allow collect info.");
            }
        } else {
            RequestReport requestReport = new RequestReport(requestPackageName, providerName, 0, 0, 0, 0);
            Message msg = this.mLocationUtilsHandler.obtainMessage(1001);
            msg.obj = requestReport;
            this.mLocationUtilsHandler.sendMessage(msg);
        }
    }

    protected void reportAppRequestLocationLock(RequestReport requestReport) {
        if (this.mRequestReportMap.size() > APP_REQUEST_LOCATION_MAX_REPORT * 10) {
            VSlog.w(TAG, "Record too much app request location.  Current recordSize:" + this.mRequestReportMap.size());
            return;
        }
        RequestReport rrTemp = this.mRequestReportMap.get(requestReport.requestCallerPkgName);
        if (rrTemp == null) {
            rrTemp = requestReport;
        }
        if (requestReport.requestProvider.equals("network")) {
            rrTemp.reqNetworkCount++;
        } else if (requestReport.requestProvider.equals("gps")) {
            rrTemp.reqGpsCount++;
        } else if (requestReport.requestProvider.equals("passive")) {
            rrTemp.reqPassiveCount++;
        } else if (requestReport.requestProvider.equals("fused")) {
            rrTemp.reqFusedCount++;
        } else {
            VSlog.w(TAG, "pkg " + rrTemp.requestCallerPkgName + " request unknow provider, " + rrTemp.requestProvider);
            return;
        }
        this.mRequestReportMap.put(rrTemp.requestCallerPkgName, rrTemp);
    }

    public void handleReportAppRequestLocationToBigDataDatabase() {
        if (DEBUG) {
            VSlog.d(TAG, "befor preCheck mapSize:" + this.mRequestReportMap.size() + " mRequestReportMap:" + this.mRequestReportMap);
        }
        final HashMap<String, RequestReport> reportMap = (HashMap) this.mRequestReportMap.clone();
        if (reportMap == null || reportMap.size() == 0) {
            VSlog.w(TAG, "No Need Report.");
            return;
        }
        final HashMap<String, ArrayList<RequestReport>> reportFilterMap = preCheckIsNeedWriteToDB(reportMap);
        if (reportFilterMap == null) {
            VSlog.i(TAG, "After pre Check, Donot report to db.");
        } else {
            new Thread(new Runnable() { // from class: com.android.server.location.VivoLocationManagerServiceUtils.3
                {
                    VivoLocationManagerServiceUtils.this = this;
                }

                @Override // java.lang.Runnable
                public void run() {
                    int writeSize;
                    try {
                        if (VivoLocationManagerServiceUtils.this.mVCD != null && ((VivoCollectData) VivoLocationManagerServiceUtils.this.mVCD).getControlInfo("203")) {
                            RequestReport rrt = null;
                            int writeSize2 = 0;
                            ArrayList<RequestReport> reportSysReqList = (ArrayList) reportFilterMap.get(VivoLocationManagerServiceUtils.SYS_APP_LIST);
                            if (reportSysReqList != null && reportSysReqList.size() > 0) {
                                int reportCount = reportSysReqList.size();
                                if (reportCount > VivoLocationManagerServiceUtils.APP_REQUEST_LOCATION_MAX_REPORT) {
                                    reportCount = VivoLocationManagerServiceUtils.APP_REQUEST_LOCATION_MAX_REPORT;
                                }
                                VSlog.i(VivoLocationManagerServiceUtils.TAG, "Begin write1 sysAppReqSize:" + reportSysReqList.size() + " allowSize:" + VivoLocationManagerServiceUtils.APP_REQUEST_LOCATION_MAX_REPORT);
                                writeSize2 = 0 + reportCount;
                                HashMap<String, String> writeMapInfo = new HashMap<>();
                                String result = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                                for (int index = 0; index < reportCount; index++) {
                                    rrt = reportSysReqList.get(index);
                                    result = result + "pk:" + rrt.requestCallerPkgName + " n:" + rrt.reqNetworkCount + " g:" + rrt.reqGpsCount + " pa:" + rrt.reqPassiveCount + " fu:" + rrt.reqFusedCount + ";";
                                }
                                writeMapInfo.put("sysApp", result);
                                HashMap<String, String> params = new HashMap<>(3);
                                params.put(VCD_FF_1.UUID_STR, UUID.randomUUID().toString());
                                params.put("apptype", "sysApp");
                                params.put("result", result);
                                EventTransfer.getInstance().singleEvent("F500", "F500|10005", System.currentTimeMillis(), 0L, params);
                                ((VivoCollectData) VivoLocationManagerServiceUtils.this.mVCD).writeData("203", "2100", System.currentTimeMillis(), System.currentTimeMillis(), 0L, 1, writeMapInfo, false);
                                ((VivoCollectData) VivoLocationManagerServiceUtils.this.mVCD).flush();
                            }
                            ArrayList<RequestReport> reportThreePartAppReqList = (ArrayList) reportFilterMap.get(VivoLocationManagerServiceUtils.T_PART_APP_LIST);
                            if (reportThreePartAppReqList != null && reportThreePartAppReqList.size() > 0) {
                                int reportCount2 = reportThreePartAppReqList.size();
                                if (reportCount2 > VivoLocationManagerServiceUtils.APP_REQUEST_LOCATION_MAX_REPORT) {
                                    reportCount2 = VivoLocationManagerServiceUtils.APP_REQUEST_LOCATION_MAX_REPORT;
                                }
                                VSlog.i(VivoLocationManagerServiceUtils.TAG, "Begin write2 3partAppReqSize:" + reportThreePartAppReqList.size() + " allowSize:" + VivoLocationManagerServiceUtils.APP_REQUEST_LOCATION_MAX_REPORT);
                                int writeSize3 = writeSize2 + reportCount2;
                                HashMap<String, String> writeMapInfo2 = new HashMap<>();
                                HashMap<String, String> writeMapInfo3 = writeMapInfo2;
                                String result2 = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                                for (int index2 = 0; index2 < reportCount2; index2++) {
                                    writeMapInfo3 = new HashMap<>();
                                    rrt = reportThreePartAppReqList.get(index2);
                                    result2 = result2 + "pk:" + rrt.requestCallerPkgName + " n:" + rrt.reqNetworkCount + " g:" + rrt.reqGpsCount + " pa:" + rrt.reqPassiveCount + " fu:" + rrt.reqFusedCount + ";";
                                }
                                writeMapInfo3.put("3App", result2);
                                HashMap<String, String> params2 = new HashMap<>(3);
                                params2.put(VCD_FF_1.UUID_STR, UUID.randomUUID().toString());
                                params2.put("apptype", "3App");
                                params2.put("result", result2);
                                EventTransfer.getInstance().singleEvent("F500", "F500|10005", System.currentTimeMillis(), 0L, params2);
                                ((VivoCollectData) VivoLocationManagerServiceUtils.this.mVCD).writeData("203", "2100", System.currentTimeMillis(), System.currentTimeMillis(), 0L, 1, writeMapInfo3, false);
                                ((VivoCollectData) VivoLocationManagerServiceUtils.this.mVCD).flush();
                                writeSize = writeSize3;
                            } else {
                                writeSize = writeSize2;
                            }
                            if (VivoLocationManagerServiceUtils.DEBUG) {
                                VSlog.d(VivoLocationManagerServiceUtils.TAG, "##write_report_end  recordSize:" + reportMap.size() + " writeSize:" + writeSize);
                            }
                            synchronized (VivoLocationManagerServiceUtils.this.mReportLock) {
                                VivoLocationManagerServiceUtils.this.mRequestReportMap.clear();
                            }
                            return;
                        }
                        VSlog.w(VivoLocationManagerServiceUtils.TAG, "V collect is not open for 203, Wait for Next time check.");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    private HashMap<String, ArrayList<RequestReport>> preCheckIsNeedWriteToDB(HashMap<String, RequestReport> reportMap) {
        HashMap<String, ArrayList<RequestReport>> pendingReportMap = new HashMap<>();
        System.currentTimeMillis();
        VSlog.i(TAG, "reportDInterV " + this.REPORT_TO_DB_INTERVAL + " size:" + reportMap.size());
        if (reportMap.size() > 0) {
            ArrayList<RequestReport> sysAppReqlist = new ArrayList<>();
            ArrayList<RequestReport> threePartAppReqList = new ArrayList<>();
            HashMap<String, ArrayList<String>> installPkgMap = getDeviceInstallPackageName();
            for (Map.Entry<String, RequestReport> entry : reportMap.entrySet()) {
                RequestReport requestReport = entry.getValue();
                if (DEBUG) {
                    VSlog.i(TAG, "check " + requestReport);
                }
                if (requestReport != null && !"unknow".equals(requestReport.requestCallerPkgName)) {
                    if (isSysApp(installPkgMap, requestReport.requestCallerPkgName)) {
                        sysAppReqlist.add(requestReport);
                    } else {
                        threePartAppReqList.add(requestReport);
                    }
                }
            }
            pendingReportMap.put(SYS_APP_LIST, sysAppReqlist);
            pendingReportMap.put(T_PART_APP_LIST, threePartAppReqList);
            return pendingReportMap;
        }
        return null;
    }

    private boolean isSysApp(HashMap<String, ArrayList<String>> installPkgMap, String reportPkg) {
        ArrayList<String> installSysApps;
        if (installPkgMap == null || installPkgMap.size() == 0 || (installSysApps = installPkgMap.get(SYS_APP_LIST)) == null || !installSysApps.contains(reportPkg)) {
            return false;
        }
        return true;
    }

    private HashMap<String, ArrayList<String>> getDeviceInstallPackageName() {
        HashMap<String, ArrayList<String>> installedPkgMap = new HashMap<>();
        try {
            PackageManager pm = this.mContext.getPackageManager();
            List<PackageInfo> installPkgInfos = pm.getInstalledPackages(EmergencyBroadcastManager.FLAG_RECEIVER_KEYAPP);
            if (installPkgInfos != null && installPkgInfos.size() > 0) {
                ArrayList<String> sysAppList = new ArrayList<>();
                ArrayList<String> threePartAppList = new ArrayList<>();
                for (PackageInfo pkgInfo : installPkgInfos) {
                    ApplicationInfo appInfo = pkgInfo.applicationInfo;
                    if (appInfo != null) {
                        if (!appInfo.isSystemApp() && !appInfo.isUpdatedSystemApp() && !appInfo.isPrivilegedApp()) {
                            threePartAppList.add(pkgInfo.packageName);
                        }
                        sysAppList.add(pkgInfo.packageName);
                    }
                }
                installedPkgMap.put(SYS_APP_LIST, sysAppList);
                installedPkgMap.put(T_PART_APP_LIST, threePartAppList);
                if (DEBUG) {
                    VSlog.w(TAG, "Device sysAppSize:" + sysAppList.size());
                    VSlog.w(TAG, "Device tpartAppSize:" + threePartAppList.size());
                }
            }
        } catch (Exception e) {
            VSlog.w(TAG, "#% " + e.toString());
        }
        return installedPkgMap;
    }

    public ArrayList<String> getBackgroundWhiteList() {
        ArrayList<String> arrayList;
        synchronized (this.mBackgroudWhiteList) {
            arrayList = this.mBackgroudWhiteList;
        }
        return arrayList;
    }

    public void handleRefreshBackgroudWhiteList() {
        this.mLocationService.vivoTriggerUpdateBgWhiteList();
    }
}