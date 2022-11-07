package com.android.server.am.firewall;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ComponentInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Slog;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class VivoAppIsolationController {
    public static final String APP_ISOLATION_VERSION = "persist.sys.appisolationversion";
    private static final int FIELD_VALUE_APP_ISOLATION_BOX = 2;
    private static final int FIELD_VALUE_APP_SAFE_BOX = 3;
    private static final int FIELD_VALUE_RULES_TYPE_APP = 0;
    private static final String FIELD_VALUE_SWITCH_KEY = "switch";
    private static final int FIELD_VALUE_VIRUS_TYPE_APP = 0;
    private static final int FLAG_APP_ISOLATION_BOX = 1;
    private static final int FLAG_APP_SAFE_BOX = 2;
    private static final String IQOO_SECURE_AUTHORITY = "com.iqoo.secure.provider.secureprovider";
    private static final int MSG_NOTIFY_IQOO_SECURE = 3;
    private static final int MSG_UPDATE_CACHE_RULES_LIST = 0;
    private static final int MSG_UPDATE_CACHE_SWITCH = 2;
    private static final int MSG_UPDATE_CACHE_VIRUS_LIST = 1;
    public static final String NOTIFY_IQOO_SECURE_ACTION = "com.iqoo.secure.action.APPISOLATION_NOTIFY";
    public static final String NOTIFY_IQOO_SECURE_PACKAGE = "com.iqoo.secure";
    public static final String NOTIFY_KEY_CALLED_PACKAGE_NAME = "called_package_name";
    public static final String NOTIFY_KEY_CALLED_TYPE = "called_type";
    public static final String NOTIFY_KEY_CALLER_PACKAGE_NAME = "caller_package_name";
    private static final String PROJECTION_FIELD_ID = "_id";
    private static final String PROJECTION_RULES_APK_TYPE = "apk_type";
    private static final String PROJECTION_RULES_PACKAGE_NAME = "package_name";
    private static final String PROJECTION_RULES_POLICY_TYPE = "policy_type";
    private static final String PROJECTION_SWITCH_KEY = "key";
    private static final String PROJECTION_SWITCH_VALUE = "value";
    private static final String PROJECTION_VIRUS_APK_TYPE = "apktype";
    private static final String PROJECTION_VIRUS_PACKAGE_NAME = "packagename";
    private ContentObserver mContentObserver;
    private Context mContext;
    private Handler mHandler;
    private static final Uri CONTENT_URI_RULES_LIST = Uri.parse("content://com.iqoo.secure.provider.secureprovider/app_isolation_table");
    private static final Uri CONTENT_URI_VIRUS_LIST = Uri.parse("content://com.iqoo.secure.provider.secureprovider/scan_result_list");
    private static final Uri CONTENT_URI_IQOO_SWITCH = Uri.parse("content://com.iqoo.secure.provider.secureprovider/app_isolation_switch");
    private String TAG = VivoFirewall.TAG;
    private int mSwitch = 0;
    private List<String> mAppIsolationBoxList = new ArrayList();
    private List<String> mAppSafeBoxList = new ArrayList();
    private List<String> mVirusList = new ArrayList();
    BroadcastReceiver mAppIsolationReceiver = new BroadcastReceiver() { // from class: com.android.server.am.firewall.VivoAppIsolationController.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (intent != null && "android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
                VivoAppIsolationController.this.mHandler.obtainMessage(2).sendToTarget();
                VivoAppIsolationController.this.mHandler.obtainMessage(0).sendToTarget();
                VivoAppIsolationController.this.mHandler.obtainMessage(1).sendToTarget();
            }
        }
    };

    public VivoAppIsolationController(Context context, Looper looper) {
        this.mContext = context;
        this.mHandler = new AppIsolationHandler(looper);
        this.mContentObserver = new AppIsolationDBObserver(this.mHandler);
    }

    public void start() {
        try {
            SystemProperties.set(APP_ISOLATION_VERSION, "1");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void systemReady() {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        contentResolver.registerContentObserver(CONTENT_URI_RULES_LIST, true, this.mContentObserver);
        contentResolver.registerContentObserver(CONTENT_URI_VIRUS_LIST, false, this.mContentObserver);
        contentResolver.registerContentObserver(CONTENT_URI_IQOO_SWITCH, false, this.mContentObserver);
        registerBroadcast();
    }

    public boolean shouldPreventAppInteraction(String callerPackage, ComponentInfo bringupSide, String type, int callerPid, int callerUid) {
        if (TextUtils.isEmpty(callerPackage) || bringupSide == null || callerPackage.equals(bringupSide.applicationInfo.packageName)) {
            return false;
        }
        if ((this.mSwitch & 1) != 0 && (this.mAppIsolationBoxList.contains(bringupSide.applicationInfo.packageName) || this.mAppIsolationBoxList.contains(callerPackage))) {
            notifyIqooSecure(callerPackage, bringupSide.applicationInfo.packageName, type, callerUid);
            if (VivoFirewall.DEBUG) {
                String str = this.TAG;
                Slog.d(str, "shouldPreventAppInteraction>>callerPackage=" + callerPackage + ",bringupSide=" + bringupSide + ", type=" + type + ", callerPid=" + callerPid + ", callerUid=" + callerUid + ", mSwitch=" + this.mSwitch + ">>true");
            }
            return true;
        } else if ((this.mSwitch & 2) != 0 && this.mAppSafeBoxList.contains(bringupSide.applicationInfo.packageName) && this.mVirusList.contains(callerPackage)) {
            notifyIqooSecure(callerPackage, bringupSide.applicationInfo.packageName, type, callerUid);
            if (VivoFirewall.DEBUG) {
                String str2 = this.TAG;
                Slog.d(str2, "shouldPreventAppInteraction>>callerPackage=" + callerPackage + ",bringupSide=" + bringupSide + ", type=" + type + ", callerPid=" + callerPid + ", callerUid=" + callerUid + ", mSwitch=" + this.mSwitch + ">>true");
            }
            return true;
        } else if (VivoFirewall.DEBUG) {
            String str3 = this.TAG;
            Slog.d(str3, "shouldPreventAppInteraction>>callerPackage=" + callerPackage + ",bringupSide=" + bringupSide + ", type=" + type + ", callerPid=" + callerPid + ", callerUid=" + callerUid + ", mSwitch=" + this.mSwitch + ">>false");
            return false;
        } else {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCacheRulesList(long id) {
        List<String> appIsolationList;
        List<String> appSafeBoxList;
        if (VivoFirewall.DEBUG) {
            Slog.d(this.TAG, "VivoAppIsolationController>>updateCacheRulesList");
        }
        boolean totalUpdate = id == Long.MAX_VALUE;
        Cursor cursor = null;
        try {
            try {
                ContentResolver resolver = this.mContext.getContentResolver();
                cursor = totalUpdate ? resolver.query(CONTENT_URI_RULES_LIST, new String[]{PROJECTION_RULES_PACKAGE_NAME, PROJECTION_RULES_POLICY_TYPE}, "(policy_type =? OR policy_type =? ) AND apk_type = ?", new String[]{String.valueOf(2), String.valueOf(3), String.valueOf(0)}, null) : resolver.query(CONTENT_URI_RULES_LIST, new String[]{PROJECTION_RULES_PACKAGE_NAME, PROJECTION_RULES_POLICY_TYPE}, "_id = ? AND apk_type = ?", new String[]{String.valueOf(id), String.valueOf(0)}, null);
                if (cursor == null) {
                    if (cursor != null) {
                        cursor.close();
                        return;
                    }
                    return;
                }
                if (totalUpdate) {
                    appIsolationList = new ArrayList<>();
                    appSafeBoxList = new ArrayList<>();
                } else {
                    appIsolationList = new ArrayList<>(this.mAppIsolationBoxList);
                    appSafeBoxList = new ArrayList<>(this.mAppSafeBoxList);
                }
                while (cursor.moveToNext()) {
                    if (cursor.getInt(1) == 2) {
                        appIsolationList.add(cursor.getString(0));
                    } else if (cursor.getInt(1) == 3) {
                        appSafeBoxList.add(cursor.getString(0));
                    } else {
                        appIsolationList.remove(cursor.getString(0));
                        appSafeBoxList.remove(cursor.getString(0));
                    }
                }
                this.mAppIsolationBoxList = appIsolationList;
                this.mAppSafeBoxList = appSafeBoxList;
                if (VivoFirewall.DEBUG) {
                    String str = this.TAG;
                    Slog.d(str, "VivoAppIsolationController>>updateCacheRulesList mAppIsolationBoxList=" + this.mAppIsolationBoxList);
                    String str2 = this.TAG;
                    Slog.d(str2, "VivoAppIsolationController>>updateCacheRulesList mAppSafeBoxList=" + this.mAppSafeBoxList);
                }
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (cursor != null) {
                    cursor.close();
                }
            }
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCacheVirusList() {
        if (VivoFirewall.DEBUG) {
            Slog.d(this.TAG, "VivoAppIsolationController>>updateCacheVirusList");
        }
        Cursor cursor = null;
        try {
            try {
                ContentResolver resolver = this.mContext.getContentResolver();
                cursor = resolver.query(CONTENT_URI_VIRUS_LIST, new String[]{PROJECTION_VIRUS_PACKAGE_NAME, PROJECTION_VIRUS_APK_TYPE}, "apktype = 0", null, null);
                if (cursor == null) {
                    if (cursor != null) {
                        cursor.close();
                        return;
                    }
                    return;
                }
                List<String> virusList = new ArrayList<>();
                while (cursor.moveToNext()) {
                    virusList.add(cursor.getString(0));
                }
                this.mVirusList = virusList;
                if (VivoFirewall.DEBUG) {
                    String str = this.TAG;
                    Slog.d(str, "VivoAppIsolationController>>updateCacheVirusList mVirusList=" + this.mVirusList);
                }
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (cursor != null) {
                    cursor.close();
                }
            }
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateCacheSwitch() {
        if (VivoFirewall.DEBUG) {
            Slog.d(this.TAG, "VivoAppIsolationController>>updateCacheSwitch");
        }
        Cursor cursor = null;
        try {
            try {
                ContentResolver resolver = this.mContext.getContentResolver();
                cursor = resolver.query(CONTENT_URI_IQOO_SWITCH, new String[]{PROJECTION_SWITCH_VALUE}, "key = ?", new String[]{FIELD_VALUE_SWITCH_KEY}, null);
            } catch (Exception e) {
                e.printStackTrace();
                if (cursor == null) {
                    return;
                }
            }
            if (cursor == null) {
                if (cursor != null) {
                    cursor.close();
                    return;
                }
                return;
            }
            if (cursor.moveToFirst()) {
                int tmpSwitch = cursor.getInt(0);
                if (tmpSwitch != this.mSwitch) {
                    if (this.mSwitch == 0) {
                        this.mHandler.obtainMessage(0).sendToTarget();
                        this.mHandler.obtainMessage(1).sendToTarget();
                    }
                    this.mSwitch = tmpSwitch;
                }
                if (VivoFirewall.DEBUG) {
                    String str = this.TAG;
                    Slog.d(str, "VivoAppIsolationController>>updateCacheSwitch mSwitch=" + this.mSwitch);
                }
            }
            if (cursor == null) {
                return;
            }
            cursor.close();
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
            throw th;
        }
    }

    private void notifyIqooSecure(String callerPackage, String calledPackage, String calledType, int callerUid) {
        Bundle bundle = new Bundle();
        bundle.putString(NOTIFY_KEY_CALLER_PACKAGE_NAME, callerPackage);
        bundle.putString(NOTIFY_KEY_CALLED_PACKAGE_NAME, calledPackage);
        bundle.putString(NOTIFY_KEY_CALLED_TYPE, calledType);
        Message msg = this.mHandler.obtainMessage(3, bundle);
        msg.arg1 = callerUid;
        msg.sendToTarget();
    }

    private void registerBroadcast() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.BOOT_COMPLETED");
        this.mContext.registerReceiver(this.mAppIsolationReceiver, intentFilter, null, this.mHandler);
    }

    /* loaded from: classes.dex */
    class AppIsolationHandler extends Handler {
        public AppIsolationHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (VivoFirewall.DEBUG) {
                String str = VivoAppIsolationController.this.TAG;
                Slog.d(str, "AppIsolationHandler handleMessage msg=" + msg);
            }
            int i = msg.what;
            if (i == 0) {
                long id = Long.MAX_VALUE;
                if (msg.obj != null) {
                    id = ((Long) msg.obj).longValue();
                }
                VivoAppIsolationController.this.updateCacheRulesList(id);
            } else if (i == 1) {
                VivoAppIsolationController.this.updateCacheVirusList();
            } else if (i == 2) {
                VivoAppIsolationController.this.updateCacheSwitch();
            } else if (i == 3) {
                try {
                    Intent intent = new Intent(VivoAppIsolationController.NOTIFY_IQOO_SECURE_ACTION);
                    intent.setPackage(VivoAppIsolationController.NOTIFY_IQOO_SECURE_PACKAGE);
                    intent.putExtras((Bundle) msg.obj);
                    VivoAppIsolationController.this.mContext.sendBroadcastAsUser(intent, UserHandle.getUserHandleForUid(msg.arg1));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /* loaded from: classes.dex */
    class AppIsolationDBObserver extends ContentObserver {
        public AppIsolationDBObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (VivoFirewall.DEBUG) {
                String str = VivoAppIsolationController.this.TAG;
                Slog.d(str, "AppIsolationDBObserver onChange uri=" + uri + ",mSwitch=" + VivoAppIsolationController.this.mSwitch);
            }
            if (VivoAppIsolationController.CONTENT_URI_IQOO_SWITCH.equals(uri)) {
                VivoAppIsolationController.this.mHandler.obtainMessage(2).sendToTarget();
            } else if ((VivoAppIsolationController.this.mSwitch & 3) != 0) {
                int message = -1;
                if (!VivoAppIsolationController.CONTENT_URI_RULES_LIST.equals(uri)) {
                    if (VivoAppIsolationController.CONTENT_URI_VIRUS_LIST.equals(uri)) {
                        message = 1;
                    }
                } else {
                    message = 0;
                }
                Long id = null;
                try {
                    id = Long.valueOf(ContentUris.parseId(uri));
                } catch (Exception e) {
                }
                if (id == null) {
                    VivoAppIsolationController.this.mHandler.removeMessages(message);
                }
                Message msg = VivoAppIsolationController.this.mHandler.obtainMessage(message);
                msg.obj = id;
                VivoAppIsolationController.this.mHandler.sendMessageDelayed(msg, 500L);
            }
        }
    }
}