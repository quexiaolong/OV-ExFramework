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
public class VivoCn0WeakDebugPanel {
    private static boolean DBG = true;
    private static final String DO_NOT_ALERT_ANYMORE = "persist.vivo.cn0weak.notshowagain";
    private static final String TAG = "VivoCn0WeakDebugPanel";
    private Context mContext;
    private boolean mDebuging;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private BroadcastReceiver mVivoBroadcastReceiver;
    private IntentFilter mVivoIntentFilter;
    private View mDebugPanelView = null;
    private TextView mTextView = null;
    public final Runnable mCn0WeakProfilingInfo = new Runnable() { // from class: com.android.server.location.VivoCn0WeakDebugPanel.1
        @Override // java.lang.Runnable
        public void run() {
            System.currentTimeMillis();
            VivoCn0WeakDebugPanel.this.mTextView.setTextColor(-1);
            String debugString = VivoCn0WeakDebugPanel.this.getDebugString();
            VivoCn0WeakDebugPanel.this.mTextView.setText(debugString);
            VivoCn0WeakDebugPanel.this.mHandler.postDelayed(VivoCn0WeakDebugPanel.this.mCn0WeakProfilingInfo, 1000L);
        }
    };

    /* loaded from: classes.dex */
    class VivoCn0PanelHandler extends Handler {
        public VivoCn0PanelHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 0) {
                VivoCn0WeakDebugPanel.this.enableDebugProgiling(0);
            } else if (i == 1) {
                VivoCn0WeakDebugPanel.this.enableDebugProgiling(1);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public VivoCn0WeakDebugPanel(Context ctx) {
        this.mContext = null;
        this.mHandler = null;
        this.mHandlerThread = null;
        this.mDebuging = false;
        this.mContext = ctx;
        HandlerThread handlerThread = new HandlerThread("vivo_cn0_debug_panel");
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        this.mDebuging = false;
        this.mHandler = new VivoCn0PanelHandler(this.mHandlerThread.getLooper());
        if (SystemProperties.get("persist.sys.cn0weakpanel", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK).equals("true")) {
            DBG = true;
            this.mHandler.sendEmptyMessage(1);
        } else {
            DBG = false;
        }
        setupCn0WeakDebugReceiver();
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
        this.mHandler.postDelayed(this.mCn0WeakProfilingInfo, 1000L);
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
                this.mHandler.removeCallbacks(this.mCn0WeakProfilingInfo);
                hideDebugPanel();
            } else {
                this.mHandler.removeCallbacks(this.mCn0WeakProfilingInfo);
                this.mHandler.postDelayed(this.mCn0WeakProfilingInfo, 1000L);
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
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.location.VivoCn0WeakDebugPanel.checkPackageExists(java.lang.String):boolean");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getDebugString() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append("DriveThreshold        [");
        sbuf.append(VivoCn0WeakManager.mCn0WeakDriveThreshold);
        sbuf.append("]");
        sbuf.append("\n");
        sbuf.append("WalkThreshold  [");
        sbuf.append(VivoCn0WeakManager.mCn0WeakWalkThreshold);
        sbuf.append("]");
        sbuf.append("\n");
        sbuf.append("IfPopAlert [");
        sbuf.append(VivoCn0WeakManager.mIfPopAlert);
        sbuf.append("]");
        sbuf.append("\n");
        sbuf.append("RequestTimeLimit [");
        sbuf.append(VivoCn0WeakManager.mRequestTimeLimit);
        sbuf.append("]");
        sbuf.append("\n");
        sbuf.append("TimesLimit [");
        sbuf.append(VivoCn0WeakManager.mTimesLimit);
        sbuf.append("]");
        sbuf.append("\n");
        sbuf.append("AlertLimitDay [");
        sbuf.append(VivoCn0WeakManager.mAlertLimitDay);
        sbuf.append("]");
        sbuf.append("\n");
        sbuf.append("DistanceLimit [");
        sbuf.append(VivoCn0WeakManager.mDistanceLimit);
        sbuf.append("]");
        sbuf.append("\n");
        sbuf.append("SpeedLimit [");
        sbuf.append(VivoCn0WeakManager.mSpeedLimit);
        sbuf.append("]");
        sbuf.append("\n");
        sbuf.append("MaxCn0 [");
        sbuf.append(VivoCn0WeakManager.mMaxCn0);
        sbuf.append("]");
        sbuf.append("\n");
        sbuf.append("MaxSpeed [");
        sbuf.append(VivoCn0WeakManager.mMaxSpeed);
        sbuf.append("]");
        sbuf.append("\n");
        sbuf.append("MoreThanDisLimit [");
        sbuf.append(VivoCn0WeakManager.mMoreThanDisLimit);
        sbuf.append(" " + VivoCn0WeakManager.mDistanceSinceFist);
        sbuf.append("]");
        sbuf.append("\n");
        sbuf.append("CountCn0Weak [");
        sbuf.append(VivoCn0WeakManager.mCountTime);
        sbuf.append("]");
        sbuf.append("\n");
        sbuf.append("Ready to pop out [");
        sbuf.append(VivoCn0WeakManager.mPopOut);
        sbuf.append("]");
        sbuf.append("\n");
        sbuf.append("DO_NOT_ALERT_ANYMORE [");
        sbuf.append(SystemProperties.get(DO_NOT_ALERT_ANYMORE, "no"));
        sbuf.append("]");
        sbuf.append("\n");
        return sbuf.toString();
    }

    private void setupCn0WeakDebugReceiver() {
        this.mVivoBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.location.VivoCn0WeakDebugPanel.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                try {
                    String action = intent.getAction();
                    if (action.equals("vivo.intent.action.CN0WEAK_DEBUG_PANEL")) {
                        boolean enabled = intent.getBooleanExtra("show_panel", false);
                        if (enabled) {
                            SystemProperties.set("persist.sys.cn0weakpanel", "true");
                            VivoCn0WeakDebugPanel.this.mHandler.sendEmptyMessage(1);
                            boolean unused = VivoCn0WeakDebugPanel.DBG = true;
                        } else {
                            SystemProperties.set("persist.sys.cn0weakpanel", "false");
                            VivoCn0WeakDebugPanel.this.mHandler.sendEmptyMessage(0);
                            boolean unused2 = VivoCn0WeakDebugPanel.DBG = false;
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        this.mVivoIntentFilter = intentFilter;
        intentFilter.addAction("vivo.intent.action.CN0WEAK_DEBUG_PANEL");
        this.mContext.registerReceiver(this.mVivoBroadcastReceiver, this.mVivoIntentFilter);
    }
}