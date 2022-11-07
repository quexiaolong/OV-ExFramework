package com.android.server.location;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.WorkSource;
import android.provider.Settings;
import com.android.internal.content.PackageMonitor;
import com.android.internal.location.ProviderRequest;
import com.vivo.common.utils.VLog;
import com.vivo.dr.IDRClient;
import com.vivo.dr.IDRService;
import com.vivo.face.common.data.Constants;
import com.vivo.services.backup.util.VivoBackupCommonUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

/* loaded from: classes.dex */
public class VivoDRManager {
    private static final int CUR_MODEL = 100;
    private static final int GNSS_REMOVE_UPDATE = 2;
    private static final int GNSS_REQUEST_UPDATE = 1;
    private static final int MAX_RESTART_COUNTS = 10;
    private static final long MIN_START_INTERVAL = 5000;
    private static final int MSG_CHECK_DR_STATUS = 12;
    private static final int MSG_CLEAR_REQUEST = 5;
    private static final int MSG_CONNECT_DR_SERVICE = 1;
    private static final int MSG_REQUEST_CHANGED = 9;
    private static final int MSG_SET_REQUEST = 2;
    private static final int MSG_START_NAVIGATING = 3;
    private static final int MSG_START_VPDR = 10;
    private static final int MSG_STOP_NAVIGATING = 4;
    private static final int MSG_STOP_VPDR = 11;
    private static final int MSG_UPDATE_LOCATION = 6;
    private static final int MSG_UPDATE_NMEA = 8;
    private static final int MSG_UPDATE_SATELLITE_STATUS = 7;
    private static final String TAG = "VivoDRManager";
    public static final boolean vivoDREnabled = true;
    private final Context mContext;
    private volatile IDRService mDRService;
    private VivoGnssLocationProviderExt mGnssLocationProvider;
    private Object mLock;
    private PackageMonitor mPackageMonitor;
    public static boolean DBG = false;
    private static ArrayList<String> mDefaultDRWhiteList = new ArrayList<>();
    private final String DR_PKG_NAME = "com.vivo.dr";
    private final String CLS_NAME = "com.vivo.dr.DRService";
    private ComponentName mComponentName = new ComponentName("com.vivo.dr", "com.vivo.dr.DRService");
    private int mRetryCount = 0;
    private final int MAX_RETRY_COUNT = 3;
    private long mLastStopTime = 0;
    private boolean mDRPkgInstalled = false;
    private HashSet<String> mRequestGpsSet = new HashSet<>();
    private boolean mDRStarted = false;
    private boolean mStartNavigating = false;
    private boolean mHasWhiteAppRequest = false;
    private ServiceConnection mVivoDRConnection = new ServiceConnection() { // from class: com.android.server.location.VivoDRManager.2
        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (VivoDRManager.DBG) {
                VLog.d(VivoDRManager.TAG, "onServiceConnected " + name);
            }
            if (service != null) {
                try {
                    VivoDRManager.this.mDRService = IDRService.Stub.asInterface(service);
                    if (VivoDRManager.this.mDRService != null) {
                        VivoDRManager.this.mVivoDRHandler.removeMessages(1);
                        VivoDRManager.this.mDRService.setListener(VivoDRManager.this.mDRClient);
                        VivoDRManager.this.mVivoDRHandler.removeMessages(10);
                        VivoDRManager.this.mVivoDRHandler.sendEmptyMessage(10);
                    }
                } catch (Exception e) {
                    VLog.e(VivoDRManager.TAG, e.toString());
                }
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            VLog.d(VivoDRManager.TAG, "onServiceDisconnected " + name);
            VivoDRManager.this.mLastStopTime = SystemClock.elapsedRealtime();
            VivoDRManager.this.mDRService = null;
            VivoDRManager.this.mDRStarted = false;
            if (VivoDRManager.access$608(VivoDRManager.this) < 10) {
                VivoDRManager.this.mVivoDRHandler.removeMessages(1);
                VivoDRManager.this.mVivoDRHandler.sendEmptyMessageDelayed(1, VivoDRManager.this.mRetryCount * 30 * 1000);
            }
        }
    };
    private Handler mVivoDRHandler = new Handler() { // from class: com.android.server.location.VivoDRManager.3
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (VivoDRManager.DBG) {
                StringBuilder sb = new StringBuilder();
                sb.append("handleMessage, ");
                sb.append(VivoDRManager.this.getMessageString(msg.what));
                sb.append(", mDRStarted:");
                sb.append(VivoDRManager.this.mDRStarted);
                sb.append(", mDRService:");
                sb.append(VivoDRManager.this.mDRService != null);
                VLog.d(VivoDRManager.TAG, sb.toString());
            }
            switch (msg.what) {
                case 1:
                    VivoDRManager vivoDRManager = VivoDRManager.this;
                    vivoDRManager.mDRPkgInstalled = vivoDRManager.checkPackageExists("com.vivo.dr");
                    int state = VivoDRManager.this.getServerVPDRState();
                    if (VivoDRManager.this.mDRPkgInstalled && state == 1 && VivoDRManager.this.mDRService == null) {
                        VivoDRManager.this.bindVivoDRService(false);
                        if (VivoDRManager.this.mDRService == null) {
                            VivoDRManager.this.mVivoDRHandler.removeMessages(1);
                            VivoDRManager.this.mVivoDRHandler.sendEmptyMessageDelayed(1, VivoBackupCommonUtil.TIMEOUT_MONITOR_INTERVAL);
                        }
                    }
                    if (VivoDRManager.DBG) {
                        VLog.d(VivoDRManager.TAG, "handleMessage, mDRPkgInstalled:" + VivoDRManager.this.mDRPkgInstalled + ", enable:" + state + ", mRetryCount:" + VivoDRManager.this.mRetryCount + ", mDRService:" + VivoDRManager.this.mDRService);
                        return;
                    }
                    return;
                case 2:
                    VivoDRManager.this.handleGpsRequest(msg);
                    return;
                case 3:
                    VivoDRManager.this.mRetryCount = 0;
                    VivoDRManager.this.mStartNavigating = true;
                    int state2 = VivoDRManager.this.getServerVPDRState();
                    if (state2 != 1 || !VivoDRManager.this.hasWhiteApp() || SystemClock.elapsedRealtime() - VivoDRManager.this.mLastStopTime <= 5000) {
                        VivoDRManager.this.mDRStarted = false;
                        VivoDRManager.this.unbindVivoDRService();
                    } else {
                        VivoDRManager.this.mVivoDRHandler.removeMessages(1);
                        VivoDRManager.this.mVivoDRHandler.sendEmptyMessage(1);
                    }
                    if (VivoDRManager.DBG) {
                        VLog.d(VivoDRManager.TAG, "MSG_START_NAVIGATING state:" + state2);
                        return;
                    }
                    return;
                case 4:
                    VivoDRManager.this.mStartNavigating = false;
                    VivoDRManager.this.mVivoDRHandler.removeMessages(11);
                    sendEmptyMessage(11);
                    return;
                case 5:
                    VivoDRManager.this.mRequestGpsSet.clear();
                    return;
                case 6:
                    Location location = (Location) msg.obj;
                    if (VivoDRManager.this.mDRStarted) {
                        try {
                            VivoDRManager.this.mDRService.updateLocationChanged(location);
                            return;
                        } catch (Exception e) {
                            VLog.e(VivoDRManager.TAG, "MSG_UPDATE_LOCATION " + e);
                            return;
                        }
                    }
                    return;
                case 7:
                    DRSvStatusInfo info = (DRSvStatusInfo) msg.obj;
                    if (VivoDRManager.this.mDRStarted) {
                        try {
                            VivoDRManager.this.mDRService.updateSatelliteStatusChanged(info.mSvCount, info.mSvidWithFlags, info.mCn0s, info.mSvElevations, info.mSvAzimuths, info.mSvCarrierFreqs);
                            return;
                        } catch (Exception e2) {
                            VLog.e(VivoDRManager.TAG, "MSG_UPDATE_SATELLITE_STATUS " + e2);
                            return;
                        }
                    }
                    return;
                case 8:
                    NmeaInfo i = (NmeaInfo) msg.obj;
                    if (VivoDRManager.this.mDRStarted) {
                        try {
                            VivoDRManager.this.mDRService.updateNmeaChanged(i.nmea, i.time);
                            return;
                        } catch (Exception e3) {
                            VLog.e(VivoDRManager.TAG, "MSG_UPDATE_NMEA " + e3);
                            return;
                        }
                    }
                    return;
                case 9:
                    int state3 = VivoDRManager.this.getServerVPDRState();
                    if (VivoDRManager.DBG) {
                        VLog.d(VivoDRManager.TAG, "MSG_REQUEST_CHANGED mLastServerVPDRState:" + state3 + ", mStartNavigating:" + VivoDRManager.this.mStartNavigating);
                    }
                    if (state3 == 1 && VivoDRManager.this.mStartNavigating) {
                        if (!VivoDRManager.this.hasWhiteApp()) {
                            if (VivoDRManager.this.mDRStarted) {
                                VivoDRManager.this.mVivoDRHandler.removeMessages(11);
                                sendEmptyMessage(11);
                                return;
                            }
                            return;
                        } else if (VivoDRManager.this.mDRService == null && SystemClock.elapsedRealtime() - VivoDRManager.this.mLastStopTime > 5000) {
                            VivoDRManager.this.mVivoDRHandler.removeMessages(1);
                            VivoDRManager.this.mVivoDRHandler.sendEmptyMessage(1);
                            return;
                        } else {
                            return;
                        }
                    }
                    return;
                case 10:
                    if (!VivoDRManager.this.mDRStarted) {
                        try {
                            if (VivoDRManager.DBG) {
                                VLog.d(VivoDRManager.TAG, "start VPDR");
                            }
                            VivoDRManager.this.mDRService.updateDRConfig_v2(VivoDRManager.this.getServerSubSwitchState(), VivoDRManager.this.getServerMaxVDRPredictTime(), VivoDRManager.this.getServerDCStatus());
                            VivoDRManager.this.mDRService.start();
                            VivoDRManager.this.mDRStarted = true;
                            return;
                        } catch (Exception e4) {
                            VLog.e(VivoDRManager.TAG, "start VPDR " + e4);
                            return;
                        }
                    }
                    return;
                case 11:
                    VivoDRManager.this.mVivoDRHandler.removeMessages(1);
                    if (VivoDRManager.this.mDRStarted) {
                        try {
                            if (VivoDRManager.DBG) {
                                VLog.d(VivoDRManager.TAG, "stop VPDR");
                            }
                            VivoDRManager.this.mDRService.stop();
                            VivoDRManager.this.mDRStarted = false;
                            VivoDRManager.this.unbindVivoDRService();
                            return;
                        } catch (Exception e5) {
                            VLog.e(VivoDRManager.TAG, "stop VPDR " + e5);
                            return;
                        }
                    }
                    return;
                case 12:
                    VivoDRManager.this.onCheckDRStatus();
                    return;
                default:
                    return;
            }
        }
    };
    private final IDRClient mDRClient = new DRClient() { // from class: com.android.server.location.VivoDRManager.4
        @Override // com.android.server.location.DRClient, com.vivo.dr.IDRClient
        public void onReportLocation(Location location, boolean trueGps) {
            if (VivoDRManager.DBG) {
                VLog.d(VivoDRManager.TAG, "onReportLocation trueGps:" + trueGps + ", " + location);
            }
            VivoDRManager.this.mGnssLocationProvider.reportDRLocation(location);
        }
    };
    private VivoLocationFeatureConfig mConfig = VivoLocationFeatureConfig.getInstance();

    /* loaded from: classes.dex */
    public interface VivoDRInterface {
        void reportDRLocation(Location location);
    }

    static /* synthetic */ int access$608(VivoDRManager x0) {
        int i = x0.mRetryCount;
        x0.mRetryCount = i + 1;
        return i;
    }

    public VivoDRManager(Context context, VivoGnssLocationProviderExt p) {
        this.mContext = context;
        this.mGnssLocationProvider = p;
        initWhiteList();
        new VivoDRDebugPanel(context, this);
    }

    private void initWhiteList() {
    }

    private void initPackageMonitor() {
        PackageMonitor packageMonitor = new PackageMonitor() { // from class: com.android.server.location.VivoDRManager.1
            public void onPackageRemoved(String packageName, int uid) {
                synchronized (VivoDRManager.this.mLock) {
                    if (packageName.equals("com.vivo.dr")) {
                        if (VivoDRManager.DBG) {
                            VLog.d(VivoDRManager.TAG, "onPackageRemoved " + packageName);
                        }
                        VivoDRManager.this.mVivoDRHandler.removeMessages(1);
                    }
                    super.onPackageRemoved(packageName, uid);
                }
            }

            public void onPackageAdded(String packageName, int uid) {
                synchronized (VivoDRManager.this.mLock) {
                    if (packageName.equals("com.vivo.dr")) {
                        if (VivoDRManager.DBG) {
                            VLog.d(VivoDRManager.TAG, "onPackageAdded " + packageName);
                        }
                        VivoDRManager.this.mVivoDRHandler.removeMessages(1);
                        VivoDRManager.this.mVivoDRHandler.sendEmptyMessageDelayed(1, 1000L);
                    }
                    super.onPackageAdded(packageName, uid);
                }
            }
        };
        this.mPackageMonitor = packageMonitor;
        packageMonitor.register(this.mContext, (Looper) null, UserHandle.ALL, true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Code restructure failed: missing block: B:12:0x0021, code lost:
        if (r3.isEmpty() == false) goto L15;
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
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r1 = "checkPackageExists "
            r0.append(r1)
            r0.append(r2)
            java.lang.String r0 = r0.toString()
            java.lang.String r1 = "VivoDRManager"
            com.vivo.common.utils.VLog.d(r1, r0)
            return r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.location.VivoDRManager.checkPackageExists(java.lang.String):boolean");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void bindVivoDRService(boolean force) {
        if (DBG) {
            VLog.d(TAG, "bindVivoDRService force:" + force + ", mDRPkgInstalled:" + this.mDRPkgInstalled + ", mDRService:" + this.mDRService);
        }
        try {
            if (this.mDRPkgInstalled) {
                if (this.mDRService == null || force) {
                    Intent intent = new Intent();
                    intent.setComponent(this.mComponentName);
                    this.mContext.bindService(intent, this.mVivoDRConnection, 1);
                }
            }
        } catch (Exception e) {
            VLog.e(TAG, e.toString());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unbindVivoDRService() {
        if (DBG) {
            VLog.d(TAG, "unbindVivoDRService");
        }
        try {
            if (this.mDRService != null) {
                this.mContext.unbindService(this.mVivoDRConnection);
                this.mLastStopTime = SystemClock.elapsedRealtime();
                this.mDRService = null;
            }
        } catch (Exception e) {
            VLog.e(TAG, e.toString());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getMessageString(int what) {
        switch (what) {
            case 1:
                return "MSG_CONNECT_DR_SERVICE";
            case 2:
                return "MSG_SET_REQUEST";
            case 3:
                return "MSG_START_NAVIGATING";
            case 4:
                return "MSG_STOP_NAVIGATING";
            case 5:
                return "MSG_CLEAR_REQUEST";
            case 6:
                return "MSG_UPDATE_LOCATION";
            case 7:
                return "MSG_UPDATE_SATELLITE_STATUS";
            case 8:
                return "MSG_UPDATE_NMEA";
            case 9:
                return "MSG_REQUEST_CHANGED";
            case 10:
                return "MSG_START_VPDR";
            case 11:
                return "MSG_STOP_VPDR";
            default:
                return "unknown";
        }
    }

    public static void setDebug(boolean debug) {
        DBG = debug;
        VLog.e(TAG, "enable DR logs: " + debug);
    }

    public void startVPDR() {
        this.mVivoDRHandler.sendEmptyMessage(3);
    }

    public void stopVPDR() {
        this.mVivoDRHandler.sendEmptyMessage(4);
    }

    public void updateLocationChanged(Location location) {
        Handler handler = this.mVivoDRHandler;
        handler.sendMessage(handler.obtainMessage(6, location));
    }

    public void updateSatelliteStatusChanged(int svCount, int[] prnWithFlags, float[] cn0s, float[] elevations, float[] azimuths, float[] carrierFreqs) {
        DRSvStatusInfo info = new DRSvStatusInfo(svCount, prnWithFlags, cn0s, elevations, azimuths, carrierFreqs);
        Handler handler = this.mVivoDRHandler;
        handler.sendMessage(handler.obtainMessage(7, info));
    }

    public void updateNmeaChanged(String nmea, long time) {
        NmeaInfo info = new NmeaInfo(nmea, time);
        Handler handler = this.mVivoDRHandler;
        handler.sendMessage(handler.obtainMessage(8, info));
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class NmeaInfo {
        public String nmea;
        public long time;

        public NmeaInfo(String n, long t) {
            this.nmea = n;
            this.time = t;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class DRSvStatusInfo {
        public float[] mCn0s;
        public float[] mSvAzimuths;
        public float[] mSvCarrierFreqs;
        public int mSvCount;
        public float[] mSvElevations;
        public int[] mSvidWithFlags;

        public DRSvStatusInfo(int svCount, int[] prnWithFlags, float[] cn0s, float[] elevations, float[] azimuths, float[] carrierFreqs) {
            this.mSvCount = svCount;
            this.mSvidWithFlags = prnWithFlags;
            this.mCn0s = cn0s;
            this.mSvElevations = elevations;
            this.mSvAzimuths = azimuths;
            this.mSvCarrierFreqs = carrierFreqs;
        }
    }

    public void onSetRequest(ProviderRequest request, WorkSource source) {
        if (DBG) {
            StringBuilder sb = new StringBuilder();
            sb.append("onSetRequest request=");
            sb.append(request == null ? "Null" : Boolean.valueOf(request.reportLocation));
            sb.append(" source=");
            sb.append(source != null ? Integer.valueOf(source.size()) : "Null");
            VLog.d(TAG, sb.toString());
        }
        if (request == null || source == null) {
            return;
        }
        Handler handler = this.mVivoDRHandler;
        handler.sendMessageDelayed(handler.obtainMessage(2, request.reportLocation ? 1 : 2, 0, source), 100L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleGpsRequest(Message msg) {
        boolean oldHasWhiteApp = this.mHasWhiteAppRequest;
        WorkSource source = (WorkSource) msg.obj;
        this.mRequestGpsSet.clear();
        if (msg.arg1 == 1) {
            for (int i = 0; i < source.size(); i++) {
                if (DBG) {
                    VLog.d(TAG, "handleGpsRequest add " + source.getName(i));
                }
                this.mRequestGpsSet.add(source.getName(i));
            }
        }
        boolean hasWhiteApp = hasWhiteApp();
        this.mHasWhiteAppRequest = hasWhiteApp;
        if (!oldHasWhiteApp && hasWhiteApp) {
            this.mVivoDRHandler.removeMessages(9);
            this.mVivoDRHandler.sendEmptyMessage(9);
        }
    }

    public boolean hasWhiteApp() {
        boolean has = false;
        String requestApp = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        String whiteAppListOnServer = this.mConfig.getWhiteListForVPDR();
        String defList = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        if (whiteAppListOnServer == null) {
            Iterator<String> reIt = this.mRequestGpsSet.iterator();
            ArrayList<String> arrayList = mDefaultDRWhiteList;
            if (arrayList != null && arrayList.size() >= 1) {
                while (reIt.hasNext()) {
                    String tempPkt = reIt.next();
                    requestApp = requestApp + tempPkt + ",";
                    if (tempPkt != null && !tempPkt.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK)) {
                        Iterator<String> it = mDefaultDRWhiteList.iterator();
                        while (true) {
                            if (it.hasNext()) {
                                String app = it.next();
                                defList = defList + app + ",";
                                if (app.equals(tempPkt)) {
                                    has = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } else if (!whiteAppListOnServer.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK)) {
            Iterator<String> reIt2 = this.mRequestGpsSet.iterator();
            while (true) {
                if (!reIt2.hasNext()) {
                    break;
                }
                String tempPkt2 = reIt2.next();
                requestApp = requestApp + tempPkt2 + ",";
                if (tempPkt2 != null && !tempPkt2.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK)) {
                    if (whiteAppListOnServer.contains(tempPkt2 + ",")) {
                        has = true;
                        break;
                    }
                }
            }
        }
        if (DBG) {
            VLog.d(TAG, "hasWhiteApp " + has + ", whiteAppListOnServer:" + whiteAppListOnServer + ", defList:" + mDefaultDRWhiteList + ", requestApp:" + requestApp);
        }
        return has;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onCheckDRStatus() {
        if (getServerVPDRState() > 0 && this.mStartNavigating && hasWhiteApp() && this.mDRService == null && SystemClock.elapsedRealtime() - this.mLastStopTime > 5000 && this.mRetryCount < 10 && !this.mVivoDRHandler.hasMessages(1)) {
            this.mVivoDRHandler.removeMessages(1);
            this.mVivoDRHandler.sendEmptyMessageDelayed(1, this.mRetryCount * 30 * 1000);
            this.mRetryCount++;
        }
    }

    public int getServerVPDRState() {
        int serverState = this.mConfig.getVPDRState();
        int localState = Settings.Global.getInt(this.mContext.getContentResolver(), "vivo_location_server_vpdr_enabled", -1);
        if (serverState == localState) {
            return serverState;
        }
        if (serverState != -1) {
            Settings.Global.putInt(this.mContext.getContentResolver(), "vivo_location_server_vpdr_enabled", serverState);
            return serverState;
        }
        return localState;
    }

    public int getServerSubSwitchState() {
        return this.mConfig.getSubSwitchStateForVPDR();
    }

    public int getServerMaxVDRPredictTime() {
        return this.mConfig.getMaxVDRPredictTimeForVPDR();
    }

    public boolean getServerDCStatus() {
        return this.mConfig.getDCStatusForVPDR();
    }

    public boolean isServerVPDREnabled() {
        int state = getServerVPDRState();
        return state != 0;
    }

    public boolean isDRStarted() {
        if (this.mDRStarted) {
            return true;
        }
        if (getServerVPDRState() > 0) {
            this.mVivoDRHandler.removeMessages(12);
            this.mVivoDRHandler.sendEmptyMessage(12);
            return false;
        }
        return false;
    }

    public String getDebugString() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append("mDRService [");
        sbuf.append(this.mDRService != null);
        sbuf.append("]");
        sbuf.append("\n");
        sbuf.append("mRetryCount[");
        sbuf.append(this.mRetryCount);
        sbuf.append("]");
        sbuf.append("\n");
        sbuf.append("mDRPkgInstalled[");
        sbuf.append(this.mDRPkgInstalled);
        sbuf.append("]");
        sbuf.append("\n");
        sbuf.append("mDRStarted[");
        sbuf.append(this.mDRStarted);
        sbuf.append("]");
        sbuf.append("\n");
        sbuf.append("mStartNavigating[");
        sbuf.append(this.mStartNavigating);
        sbuf.append("]");
        sbuf.append("\n");
        sbuf.append("mCurrentVPDRState[");
        sbuf.append(getServerVPDRState());
        sbuf.append("]");
        sbuf.append("\n");
        sbuf.append("mRequestGpsSet");
        sbuf.append(this.mRequestGpsSet);
        sbuf.append("\n");
        sbuf.append("\n");
        sbuf.append("mDefaultList");
        sbuf.append(mDefaultDRWhiteList.toString());
        sbuf.append("\n");
        sbuf.append("\n");
        String whiteAppListOnServer = this.mConfig.getWhiteListForVPDR();
        sbuf.append("ServerList[");
        sbuf.append(whiteAppListOnServer == null ? "null" : whiteAppListOnServer);
        sbuf.append("]");
        sbuf.append("\n");
        return sbuf.toString();
    }
}