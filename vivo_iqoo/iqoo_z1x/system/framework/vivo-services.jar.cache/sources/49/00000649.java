package com.vivo.services.engineerutile;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.hardware.graphics.common.V1_0.Dataspace;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import com.android.server.am.firewall.VivoFirewall;
import com.vivo.services.security.server.VivoPermissionUtils;
import java.util.List;

/* loaded from: classes.dex */
public class PCBAFloatView {
    private static final int MSG_HILE = 0;
    private static final int MSG_SHOW = 1;
    private static final String TAG = "PCBAFloatView";
    private Context mContext;
    private Handler mHandler;
    private TextView mTextView = null;
    private WindowManager.LayoutParams wmParams = null;
    private WindowManager mWindowManager = null;
    private boolean mShow = false;

    public PCBAFloatView(Context context) {
        this.mContext = null;
        this.mHandler = null;
        Log.d(TAG, TAG);
        this.mContext = context;
        createFloatView();
        registerLocaleChange();
        this.mHandler = new ViewHandler();
        new ViewThread().start();
    }

    private void createFloatView() {
        this.mTextView = new TextView(this.mContext);
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        DisplayMetrics metric = new DisplayMetrics();
        this.mWindowManager.getDefaultDisplay().getRealMetrics(metric);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-2, -2, 2002, 40, -2);
        this.wmParams = layoutParams;
        layoutParams.gravity = 51;
        this.wmParams.x = metric.widthPixels;
        this.wmParams.y = getStatusBarHeight();
        String mLock = SystemProperties.get("ro.pcba.control", "1");
        if ("0".equals(mLock)) {
            this.mTextView.setText(51249867);
        } else if ("2".equals(mLock)) {
            this.mTextView.setText(51249868);
        }
        this.mTextView.setTextColor(-65536);
        this.mTextView.setTypeface(Typeface.defaultFromStyle(1));
        this.mWindowManager.addView(this.mTextView, this.wmParams);
        this.mShow = true;
    }

    private int getStatusBarHeight() {
        int resourceId = this.mContext.getResources().getIdentifier("status_bar_height", "dimen", VivoPermissionUtils.OS_PKG);
        if (resourceId <= 0) {
            return 50;
        }
        int statusBarHeight = this.mContext.getResources().getDimensionPixelSize(resourceId);
        return statusBarHeight;
    }

    private void registerLocaleChange() {
        IntentFilter filter = new IntentFilter("android.intent.action.LOCALE_CHANGED");
        this.mContext.registerReceiver(new LocaleChangeReceiver(), filter);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isHome(Context mContext) {
        ActivityManager mActivityManager;
        try {
            mActivityManager = (ActivityManager) mContext.getSystemService(VivoFirewall.TYPE_ACTIVITY);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (mActivityManager == null) {
            Log.e(TAG, "ACTIVITY_SERVICE is invalid");
            return false;
        }
        List<ActivityManager.RunningTaskInfo> rti = mActivityManager.getRunningTasks(1);
        if (rti == null) {
            Log.e(TAG, "getRunningTasks is invalid");
            return false;
        } else if (rti.size() < 1) {
            Log.e(TAG, "no task is running");
            return true;
        } else {
            PackageManager packageManager = mContext.getPackageManager();
            if (packageManager == null) {
                Log.e(TAG, "packageManager is invalid");
                return false;
            }
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.addCategory("android.intent.category.HOME");
            List<ResolveInfo> resolveInfo = packageManager.queryIntentActivities(intent, Dataspace.STANDARD_BT709);
            if (resolveInfo != null && resolveInfo.size() != 0) {
                for (ResolveInfo ri : resolveInfo) {
                    if (ri.activityInfo.packageName.equals(rti.get(0).topActivity.getPackageName())) {
                        return true;
                    }
                }
                return false;
            }
            Log.e(TAG, "queryIntentActivities is invalid");
            return false;
        }
    }

    /* loaded from: classes.dex */
    private class ViewThread extends Thread {
        private ViewThread() {
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            while (true) {
                try {
                    Thread.sleep(1000L);
                    boolean home = PCBAFloatView.this.isHome(PCBAFloatView.this.mContext);
                    PCBAFloatView.this.mHandler.obtainMessage(home ? 1 : 0).sendToTarget();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class LocaleChangeReceiver extends BroadcastReceiver {
        private LocaleChangeReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null || PCBAFloatView.this.mTextView == null || !intent.getAction().equals("android.intent.action.LOCALE_CHANGED")) {
                return;
            }
            String mLock = SystemProperties.get("ro.pcba.control", "1");
            if ("0".equals(mLock)) {
                PCBAFloatView.this.mTextView.setText(51249867);
            } else if ("2".equals(mLock)) {
                PCBAFloatView.this.mTextView.setText(51249868);
            }
        }
    }

    /* loaded from: classes.dex */
    private class ViewHandler extends Handler {
        private ViewHandler() {
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                if (PCBAFloatView.this.mShow) {
                    PCBAFloatView.this.mShow = false;
                    PCBAFloatView.this.mWindowManager.removeView(PCBAFloatView.this.mTextView);
                }
            } else if (msg.what == 1 && !PCBAFloatView.this.mShow) {
                PCBAFloatView.this.mShow = true;
                PCBAFloatView.this.mWindowManager.addView(PCBAFloatView.this.mTextView, PCBAFloatView.this.wmParams);
            }
        }
    }
}