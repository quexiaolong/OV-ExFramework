package com.android.server.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
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
public class VivoDRDebugPanel {
    private static boolean DBG = false;
    private static final String TAG = "VivoDRDebugPanel";
    private Context mContext;
    private BroadcastReceiver mVivoBroadcastReceiver;
    private VivoDRManager mVivoDRManager;
    private IntentFilter mVivoIntentFilter;
    private View mDebugPanelView = null;
    private TextView mTextView = null;
    private Handler mHandler = new Handler() { // from class: com.android.server.location.VivoDRDebugPanel.1
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 0) {
                VivoDRDebugPanel.this.enableDebugProgiling(0);
            } else if (i == 1) {
                VivoDRDebugPanel.this.enableDebugProgiling(1);
            }
        }
    };
    public final Runnable mDRProfilingInfo = new Runnable() { // from class: com.android.server.location.VivoDRDebugPanel.2
        @Override // java.lang.Runnable
        public void run() {
            System.currentTimeMillis();
            VivoDRDebugPanel.this.mTextView.setTextColor(-1);
            String debugString = VivoDRDebugPanel.this.getDebugString();
            VivoDRDebugPanel.this.mTextView.setText(debugString);
            VivoDRDebugPanel.this.mHandler.postDelayed(VivoDRDebugPanel.this.mDRProfilingInfo, 1000L);
        }
    };

    public VivoDRDebugPanel(Context ctx, VivoDRManager manager) {
        this.mContext = null;
        this.mContext = ctx;
        this.mVivoDRManager = manager;
        if (SystemProperties.get("persist.sys.drpanel", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK).equals("true")) {
            DBG = true;
            this.mHandler.sendEmptyMessage(1);
        } else {
            DBG = false;
        }
        setupDRDebugReceiver();
    }

    public void showDebugPanel() {
        if (DBG) {
            VLog.d(TAG, "showDebugPanel:" + Thread.currentThread().getStackTrace()[2].getMethodName());
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
        layoutParams.alpha = 0.4f;
        WindowManager windowManager = (WindowManager) this.mContext.getSystemService("window");
        windowManager.addView(this.mDebugPanelView, layoutParams);
        this.mHandler.postDelayed(this.mDRProfilingInfo, 1000L);
    }

    public void hideDebugPanel() {
        try {
            if (DBG) {
                VLog.d(TAG, "hideDebugPanel:" + Thread.currentThread().getStackTrace()[2].getMethodName());
            }
            if (DBG) {
                VLog.d(TAG, "hideDebugPanel mDebugPanelView " + this.mDebugPanelView);
            }
            if (this.mDebugPanelView != null) {
                WindowManager windowManager = (WindowManager) this.mContext.getSystemService("window");
                windowManager.removeView(this.mDebugPanelView);
                this.mDebugPanelView = null;
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
                VLog.d(TAG, "enableDebugProgiling 0");
                this.mHandler.removeCallbacks(this.mDRProfilingInfo);
                hideDebugPanel();
            } else {
                this.mHandler.removeCallbacks(this.mDRProfilingInfo);
                this.mHandler.postDelayed(this.mDRProfilingInfo, 1000L);
                showDebugPanel();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getDebugString() {
        return this.mVivoDRManager.getDebugString();
    }

    private void setupDRDebugReceiver() {
        this.mVivoBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.location.VivoDRDebugPanel.3
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                try {
                    String action = intent.getAction();
                    if (action.equals("vivo.intent.action.DR_DEBUG_PANEL")) {
                        boolean enabled = intent.getBooleanExtra("show_panel", false);
                        if (enabled) {
                            SystemProperties.set("persist.sys.drpanel", "true");
                            VivoDRDebugPanel.this.mHandler.sendEmptyMessage(1);
                            boolean unused = VivoDRDebugPanel.DBG = true;
                        } else {
                            SystemProperties.set("persist.sys.drpanel", "false");
                            VivoDRDebugPanel.this.mHandler.sendEmptyMessage(0);
                            boolean unused2 = VivoDRDebugPanel.DBG = false;
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        this.mVivoIntentFilter = intentFilter;
        intentFilter.addAction("vivo.intent.action.SLA_DEBUG_PANEL");
        this.mContext.registerReceiver(this.mVivoBroadcastReceiver, this.mVivoIntentFilter);
    }
}