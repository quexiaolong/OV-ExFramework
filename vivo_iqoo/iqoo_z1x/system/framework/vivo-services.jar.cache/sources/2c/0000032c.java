package com.android.server.notification;

import android.app.admin.DevicePolicyManager;
import android.app.admin.VivoPolicyManagerInternal;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.text.TextUtils;
import android.util.Xml;
import com.android.server.LocalServices;
import com.android.server.notification.ManagedServices;
import com.android.server.policy.InputExceptionReport;
import com.vivo.services.security.server.VivoPermissionUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class NotificationBlackListManager {
    private static final String ACTION_UNIFIED_CONFIG_UPDATE = "com.vivo.daemonService.unifiedconfig.update_finish_broadcast_NotificationIntercept";
    public static final String NOTIFICATION_URI = "content://com.vivo.abe.unifiedconfig.provider/configs";
    private static final String TAG = "NotificationBlackListManager";
    private Context mContext;
    DevicePolicyManager mDpm;
    private Handler mNotificationHandler;
    private NotificationManagerService mNotificationManagerService;
    VivoPolicyManagerInternal mVivoPolicyManagerInternal;
    private static final int MY_UID = Process.myUid();
    private static final int MY_PID = Process.myPid();
    private static String tagType = "blacklist";
    private static String tagPkg = "package";
    private final HandlerThread mNotificationThread = new HandlerThread("notification_intercept");
    private final ArrayList<String> mNotificationInterceptApps = new ArrayList<>();
    List<String> mPolicyList = new ArrayList();
    private boolean mIsInCustomMode = false;
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() { // from class: com.android.server.notification.NotificationBlackListManager.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (NotificationBlackListManager.ACTION_UNIFIED_CONFIG_UPDATE.equals(action)) {
                NotificationBlackListManager.this.mNotificationHandler.removeCallbacks(NotificationBlackListManager.this.mGetUnifiedConfigRunnable);
                NotificationBlackListManager.this.mNotificationHandler.postDelayed(NotificationBlackListManager.this.mGetUnifiedConfigRunnable, 500L);
            }
        }
    };
    private final Runnable mGetUnifiedConfigRunnable = new Runnable() { // from class: com.android.server.notification.NotificationBlackListManager.2
        @Override // java.lang.Runnable
        public void run() {
            if (NotificationManagerService.DBG) {
                VSlog.d(NotificationBlackListManager.TAG, "mGetUnifiedConfigRunnable!");
            }
            NotificationBlackListManager.this.getUnifiedConfig("NotificationIntercept", InputExceptionReport.LEVEL_MEDIUM, "1.0", "notification_intercept");
            NotificationBlackListManager.this.updateNotification();
        }
    };

    public NotificationBlackListManager(Context context, NotificationManagerService notificationManagerService) {
        this.mContext = context;
        this.mNotificationManagerService = notificationManagerService;
        this.mNotificationThread.start();
        this.mNotificationHandler = new Handler(this.mNotificationThread.getLooper());
        this.mDpm = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        this.mNotificationHandler.postDelayed(new Runnable() { // from class: com.android.server.notification.NotificationBlackListManager.3
            @Override // java.lang.Runnable
            public void run() {
                NotificationBlackListManager.this.init();
            }
        }, 6000L);
    }

    public boolean isIntercept(String pkg) {
        boolean res;
        synchronized (this.mNotificationInterceptApps) {
            boolean z = true;
            res = !TextUtils.isEmpty(pkg) && this.mNotificationInterceptApps.contains(pkg);
            if (!res && this.mIsInCustomMode) {
                if (TextUtils.isEmpty(pkg) || ((this.mPolicyList != null && this.mPolicyList.contains(pkg)) || pkg.equals(VivoPermissionUtils.OS_PKG))) {
                    z = false;
                }
                res = z;
            }
        }
        return res;
    }

    public void onStart() {
        registerBroadcastReceiver();
    }

    private void registerBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_UNIFIED_CONFIG_UPDATE);
        this.mContext.registerReceiver(this.mIntentReceiver, intentFilter);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void init() {
        this.mNotificationHandler.removeCallbacks(this.mGetUnifiedConfigRunnable);
        this.mNotificationHandler.postDelayed(this.mGetUnifiedConfigRunnable, 80000L);
        if (this.mDpm.getCustomType() > 0) {
            if (this.mDpm.getRestrictionPolicy(null, 326) == 1) {
                this.mIsInCustomMode = true;
                this.mPolicyList = this.mDpm.getRestrictionInfoList(null, 1509);
            }
            VivoPolicyManagerInternal vivoPolicyManagerInternal = (VivoPolicyManagerInternal) LocalServices.getService(VivoPolicyManagerInternal.class);
            this.mVivoPolicyManagerInternal = vivoPolicyManagerInternal;
            vivoPolicyManagerInternal.setVivoPolicyListener(new VivoPolicyManagerInternal.VivoPolicyListener() { // from class: com.android.server.notification.-$$Lambda$NotificationBlackListManager$arjBS7qhKt04KfKPfGGWH6wXwhA
                public final void onVivoPolicyChanged(int i) {
                    NotificationBlackListManager.this.lambda$init$0$NotificationBlackListManager(i);
                }
            });
        }
    }

    public /* synthetic */ void lambda$init$0$NotificationBlackListManager(int poId) {
        synchronized (this.mNotificationInterceptApps) {
            if (poId == 0 || poId == 326 || poId == 1509) {
                int policy = this.mDpm.getRestrictionPolicy(null, 326);
                if (policy == 1) {
                    this.mIsInCustomMode = true;
                    this.mPolicyList = this.mDpm.getRestrictionInfoList(null, 1509);
                } else {
                    this.mIsInCustomMode = false;
                    this.mPolicyList = null;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateNotification() {
        synchronized (this.mNotificationManagerService.mNotificationLock) {
            synchronized (this.mNotificationInterceptApps) {
                cancelAllNotificationsByBlackList(this.mNotificationInterceptApps);
            }
        }
    }

    void cancelAllNotificationsByBlackList(ArrayList<String> blacklist) {
        ArrayList<String> toDeletePkg = new ArrayList<>();
        int N = this.mNotificationManagerService.mNotificationList.size();
        for (int i = 0; i < N; i++) {
            NotificationRecord tmpRecord = (NotificationRecord) this.mNotificationManagerService.mNotificationList.get(i);
            String tmpPkg = tmpRecord.getSbn().getPackageName();
            if (blacklist.contains(tmpPkg) && !toDeletePkg.contains(tmpPkg)) {
                toDeletePkg.add(tmpPkg);
            }
        }
        Iterator<String> it = toDeletePkg.iterator();
        while (it.hasNext()) {
            String pkg = it.next();
            this.mNotificationManagerService.cancelAllNotificationsInt(MY_UID, MY_PID, pkg, (String) null, 0, 0, true, -1, 20, (ManagedServices.ManagedServiceInfo) null);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean getUnifiedConfig(String moduleName, String type, String version, String identifier) {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        String[] selectionArgs = {moduleName, type, version, identifier};
        Cursor cursor = null;
        byte[] fileContent = null;
        try {
            try {
                try {
                    Cursor cursor2 = contentResolver.query(Uri.parse("content://com.vivo.abe.unifiedconfig.provider/configs"), null, null, selectionArgs, null);
                    if (cursor2 != null) {
                        cursor2.moveToFirst();
                        if (cursor2.getCount() > 0) {
                            while (!cursor2.isAfterLast()) {
                                fileContent = cursor2.getBlob(cursor2.getColumnIndex("filecontent"));
                                cursor2.moveToNext();
                            }
                        } else if (NotificationManagerService.DBG) {
                            VSlog.d(TAG, "no data!");
                        }
                    }
                    if (cursor2 != null) {
                        cursor2.close();
                    }
                } catch (Throwable th) {
                    if (0 != 0) {
                        try {
                            cursor.close();
                        } catch (Exception e) {
                        }
                    }
                    throw th;
                }
            } catch (Exception e2) {
                VSlog.e(TAG, "open database error! " + e2.fillInStackTrace());
                if (0 != 0) {
                    cursor.close();
                }
            }
        } catch (Exception e3) {
        }
        if (fileContent != null) {
            String tmpResult = new String(fileContent);
            parseXmlFormatData(new ByteArrayInputStream(tmpResult.getBytes()));
            return true;
        }
        return false;
    }

    private void parseXmlFormatData(InputStream inputStream) {
        synchronized (this.mNotificationInterceptApps) {
            try {
                this.mNotificationInterceptApps.clear();
                try {
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setInput(inputStream, null);
                    parser.getName();
                    for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                        String tag = parser.getName();
                        if (eventType == 2 && tagPkg.equals(tag)) {
                            parser.next();
                            String pkg = parser.getText();
                            if (pkg != null && !TextUtils.isEmpty(pkg)) {
                                this.mNotificationInterceptApps.add(pkg);
                            }
                        }
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                    if (inputStream != null) {
                        inputStream.close();
                    }
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception e2) {
            }
        }
    }
}