package com.android.server.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;

/* loaded from: classes.dex */
public class VivoNlpPowerMonitorDebugPanel {
    private static boolean DBG = true;
    private static final String DO_NOT_ALERT_ANYMORE = "persist.vivo.nlpsave.notshowagain";
    private static final String TAG = "VivoNlpPowerMonitorDebugPanel";
    private Context mContext;
    private boolean mDebuging;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private BroadcastReceiver mVivoBroadcastReceiver;
    private IntentFilter mVivoIntentFilter;
    private VivoNlpPowerMonitor mVivoNlpPowerMonitor;
    private View mDebugPanelView = null;
    private TextView mTextView = null;
    public final Runnable mNlpSaveProfilingInfo = new Runnable() { // from class: com.android.server.location.VivoNlpPowerMonitorDebugPanel.1
        @Override // java.lang.Runnable
        public void run() {
            System.currentTimeMillis();
            VivoNlpPowerMonitorDebugPanel.this.mTextView.setTextColor(-1);
            String debugString = VivoNlpPowerMonitorDebugPanel.this.getDebugString();
            VivoNlpPowerMonitorDebugPanel.this.mTextView.setText(debugString);
            VivoNlpPowerMonitorDebugPanel.this.mHandler.postDelayed(VivoNlpPowerMonitorDebugPanel.this.mNlpSaveProfilingInfo, 1000L);
        }
    };

    /* loaded from: classes.dex */
    class VivoNlpSavePanelHandler extends Handler {
        public VivoNlpSavePanelHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 0) {
                VivoNlpPowerMonitorDebugPanel.this.enableDebugProgiling(0);
            } else if (i == 1) {
                VivoNlpPowerMonitorDebugPanel.this.enableDebugProgiling(1);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public VivoNlpPowerMonitorDebugPanel(Context ctx, VivoNlpPowerMonitor mVivoNlpPowerMonitor) {
        this.mContext = null;
        this.mHandler = null;
        this.mHandlerThread = null;
        this.mDebuging = false;
        this.mVivoNlpPowerMonitor = null;
        this.mContext = ctx;
        this.mVivoNlpPowerMonitor = mVivoNlpPowerMonitor;
        HandlerThread handlerThread = new HandlerThread(TAG);
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        this.mDebuging = false;
        this.mHandler = new VivoNlpSavePanelHandler(this.mHandlerThread.getLooper());
        if (SystemProperties.get("persist.sys.nlpsavepanel", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK).equals("true")) {
            DBG = true;
            this.mHandler.sendEmptyMessage(1);
        } else {
            DBG = false;
        }
        setupNlpSaveDebugReceiver();
    }

    public void showDebugPanel() {
        if (DBG) {
            VLog.d(TAG, "showDebugPanel:" + Thread.currentThread().getStackTrace()[2].getMethodName());
        }
        if (this.mDebuging) {
            return;
        }
        LayoutInflater adbInflater = LayoutInflater.from(this.mContext);
        this.mDebugPanelView = adbInflater.inflate(50528422, (ViewGroup) null);
        if (DBG) {
            VLog.d(TAG, "showDebugPanel mDebugPanelView " + this.mDebugPanelView);
        }
        View view = this.mDebugPanelView;
        if (view != null) {
            this.mTextView = (TextView) view.findViewById(51183647);
        }
        this.mTextView.setTextColor(-1);
        String debugString = getDebugString();
        this.mTextView.setText(debugString);
        if (DBG) {
            VLog.d(TAG, "showDebugPanel " + debugString);
        }
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.type = 2003;
        layoutParams.flags = 24;
        layoutParams.width = -2;
        layoutParams.height = -2;
        layoutParams.gravity = 83;
        layoutParams.alpha = 0.7f;
        WindowManager windowManager = (WindowManager) this.mContext.getSystemService("window");
        windowManager.addView(this.mDebugPanelView, layoutParams);
        this.mHandler.postDelayed(this.mNlpSaveProfilingInfo, 1000L);
        this.mDebuging = true;
    }

    public void hideDebugPanel() {
        try {
            VLog.d(TAG, "hideDebugPanel:" + Thread.currentThread().getStackTrace()[2].getMethodName());
            VLog.d(TAG, "hideDebugPanel mDebugPanelView " + this.mDebugPanelView);
            if (this.mDebugPanelView != null) {
                WindowManager windowManager = (WindowManager) this.mContext.getSystemService("window");
                windowManager.removeView(this.mDebugPanelView);
                this.mDebugPanelView = null;
                this.mDebuging = false;
            }
            this.mTextView = null;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void enableDebugProgiling(int verbose) {
        try {
            VLog.d(TAG, "enableDebugProgiling " + verbose);
            if (verbose <= 0) {
                VLog.d(TAG, "enableDebugProgiling 000");
                this.mHandler.removeCallbacks(this.mNlpSaveProfilingInfo);
                hideDebugPanel();
            } else {
                this.mHandler.removeCallbacks(this.mNlpSaveProfilingInfo);
                this.mHandler.postDelayed(this.mNlpSaveProfilingInfo, 1000L);
                showDebugPanel();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:12:0x0021, code lost:
        if (r3.isEmpty() == false) goto L12;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private boolean checkPackageExists(java.lang.String r6) {
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
            return r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.location.VivoNlpPowerMonitorDebugPanel.checkPackageExists(java.lang.String):boolean");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getDebugString() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append("SvaeMode[");
        sbuf.append(this.mVivoNlpPowerMonitor.mIsRegistered ? "Sensor" : "Network");
        sbuf.append("]");
        sbuf.append("\n");
        sbuf.append("TestMode[");
        sbuf.append(this.mVivoNlpPowerMonitor.bNlpPowerSaveTestMode);
        sbuf.append("]");
        sbuf.append("\n");
        sbuf.append("SensorStatus[");
        sbuf.append(this.mVivoNlpPowerMonitor.mSensorStatus == 1 ? "Static" : "Moving");
        sbuf.append("]");
        sbuf.append("\n");
        sbuf.append("NetworkStatus[");
        sbuf.append(this.mVivoNlpPowerMonitor.mIsNetworkAvailable);
        sbuf.append("]");
        sbuf.append("\n");
        sbuf.append("ScreenStatus[");
        sbuf.append(this.mVivoNlpPowerMonitor.bScreenOff);
        sbuf.append("]");
        sbuf.append("\n");
        sbuf.append("Waiting[");
        sbuf.append(this.mVivoNlpPowerMonitor.mSaveProcess);
        sbuf.append("]");
        sbuf.append("\n");
        sbuf.append("Ready[");
        sbuf.append(this.mVivoNlpPowerMonitor.bPowerSaveMode);
        sbuf.append("]");
        sbuf.append("\n");
        sbuf.append("Location[");
        sbuf.append(this.mVivoNlpPowerMonitor.mLastLocation == null ? Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK : this.mVivoNlpPowerMonitor.mLastLocation.getLatitude() + "," + this.mVivoNlpPowerMonitor.mLastLocation.getLongitude());
        sbuf.append("]");
        sbuf.append("\n");
        sbuf.append("RGCCount[");
        sbuf.append(this.mVivoNlpPowerMonitor.flagForQueue);
        sbuf.append("]");
        sbuf.append("\n");
        StringBuilder sb = new StringBuilder();
        for (String s : this.mVivoNlpPowerMonitor.mNlpSaveFilterList) {
            sb.append(s + ",\n");
        }
        sbuf.append("WhiteList[");
        sbuf.append(sb.toString());
        sbuf.append("]");
        sbuf.append("\n");
        VLog.d(TAG, "getDebugString\n" + ((Object) sbuf));
        return sbuf.toString();
    }

    private void setupNlpSaveDebugReceiver() {
        this.mVivoBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.location.VivoNlpPowerMonitorDebugPanel.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                try {
                    String action = intent.getAction();
                    if (action.equals("vivo.intent.action.NLP_SVAE_DEBUG_PANEL")) {
                        boolean enabled = intent.getBooleanExtra("show_panel", false);
                        if (enabled) {
                            SystemProperties.set("persist.sys.nlpsavepanel", "true");
                            VivoNlpPowerMonitorDebugPanel.this.mHandler.sendEmptyMessage(1);
                            boolean unused = VivoNlpPowerMonitorDebugPanel.DBG = true;
                        } else {
                            SystemProperties.set("persist.sys.nlpsavepanel", "false");
                            VivoNlpPowerMonitorDebugPanel.this.mHandler.sendEmptyMessage(0);
                            boolean unused2 = VivoNlpPowerMonitorDebugPanel.DBG = false;
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        this.mVivoIntentFilter = intentFilter;
        intentFilter.addAction("vivo.intent.action.NLP_SVAE_DEBUG_PANEL");
        this.mContext.registerReceiver(this.mVivoBroadcastReceiver, this.mVivoIntentFilter);
    }
}