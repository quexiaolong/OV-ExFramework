package com.android.server.location;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.IProcessObserver;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.drawable.Icon;
import android.hardware.graphics.common.V1_0.Dataspace;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import com.android.internal.content.PackageMonitor;
import com.android.server.am.firewall.VivoFirewall;
import com.android.server.location.VivoLocConf;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import com.vivo.services.security.server.VivoPermissionUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vivo.app.configuration.ContentValuesList;

/* loaded from: classes.dex */
public class VivoLocationNotify {
    private static final String ACTION_DO_NOT_POP_AGAIN = "com.vivo.location.ACTION_DO_NOT_POP_AGAIN";
    private static final String ACTION_ENABLE_GPS = "com.vivo.location.ACTION_ENABLE_GPS";
    private static final int ID_START = 51249828;
    public static final int MSG_DO_NOT_POP_AGAIN = 3;
    public static final int MSG_ENABLE_GPS = 4;
    public static final int MSG_PKG_REMOVED = 6;
    public static final int MSG_PROCESS_DIED = 1;
    public static final int MSG_READ_XML = 5;
    public static final int MSG_REQUEST_LOCATION = 0;
    public static final int MSG_UPDATE_LOCATION_STATE = 2;
    private static final String NOTIFICATION_CHANNEL_ID = "vivo_location_notify";
    private static final int NOTIFY_DISABLE = 1;
    private static final int NOTIFY_ENABLE = 0;
    private static final int NOTIFY_TYPE_ENABLE_GPS = 1;
    private static final int NOTIFY_TYPE_ENABLE_LOCATION = 0;
    private static final String SETTINGS_LOCATION_NOTIFY = "location_notify_";
    private static final String TAG = "VivoLocationNotify";
    private static Context mContext = null;
    public static final boolean vivoLocationNotifyEnabled = true;
    private LocationManager mLocationManager;
    private LocationNofityHandler mLocationNofityHandler;
    private Notification.Builder mNotificationBuilder;
    private NotificationManager mNotificationManager;
    private Resources mResources;
    private BroadcastReceiver mVivoBroadcastReceiver;
    private IntentFilter mVivoIntentFilter;
    private HandlerThread vivoThread;
    private static boolean DBG = true;
    public static HashMap<String, NotifySettings> mNotifyAppMap = new HashMap<>();
    boolean isUpdating = false;
    private int mLocationMode = 3;
    private IProcessObserver mProcessObserver = new IProcessObserver.Stub() { // from class: com.android.server.location.VivoLocationNotify.5
        {
            VivoLocationNotify.this = this;
        }

        public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
        }

        public void onForegroundServicesChanged(int pid, int uid, int serviceTypes) {
        }

        public void onProcessDied(int pid, int uid) {
            if (VivoLocationNotify.DBG) {
                VLog.d("VivoLocationNotify", "onProcessDied: pid=" + pid + ", uid=" + uid);
            }
            Message m = VivoLocationNotify.this.mLocationNofityHandler.obtainMessage(1);
            m.arg1 = pid;
            m.arg2 = uid;
            VivoLocationNotify.this.mLocationNofityHandler.sendMessage(m);
        }
    };
    private final PackageMonitor mPackageMonitor = new PackageMonitor() { // from class: com.android.server.location.VivoLocationNotify.6
        {
            VivoLocationNotify.this = this;
        }

        public void onPackageUpdateFinished(String packageName, int uid) {
        }

        public void onPackageAdded(String packageName, int uid) {
        }

        public void onPackageRemoved(String packageName, int uid) {
            if (VivoLocationNotify.DBG) {
                VLog.d("VivoLocationNotify", "onPackageRemoved " + packageName);
            }
            try {
                Message msg = VivoLocationNotify.this.mLocationNofityHandler.obtainMessage(6, packageName);
                VivoLocationNotify.this.mLocationNofityHandler.sendMessage(msg);
            } catch (Exception e) {
                VLog.e("VivoLocationNotify", Log.getStackTraceString(e));
            }
        }

        public boolean onPackageChanged(String packageName, int uid, String[] components) {
            return super.onPackageChanged(packageName, uid, components);
        }
    };

    public static /* synthetic */ void lambda$tu4HSwUQu_GGIxAFqLBbDTozMiU(VivoLocationNotify vivoLocationNotify, ContentValuesList contentValuesList) {
        vivoLocationNotify.updateVivoLocationNotifyApp(contentValuesList);
    }

    public VivoLocationNotify(Context context, boolean dbg) {
        mContext = context;
        VivoLocConf conf = VivoLocConf.getInstance();
        conf.registerListener("VivoLocationNotify", new VivoLocConf.ContentValuesListChangedListener() { // from class: com.android.server.location.-$$Lambda$VivoLocationNotify$tu4HSwUQu_GGIxAFqLBbDTozMiU
            @Override // com.android.server.location.VivoLocConf.ContentValuesListChangedListener
            public final void onConfigChanged(ContentValuesList contentValuesList) {
                VivoLocationNotify.lambda$tu4HSwUQu_GGIxAFqLBbDTozMiU(VivoLocationNotify.this, contentValuesList);
            }
        });
        HandlerThread handlerThread = new HandlerThread("VivoLocationNotify");
        this.vivoThread = handlerThread;
        handlerThread.start();
        this.mNotificationManager = (NotificationManager) mContext.getSystemService("notification");
        this.mResources = mContext.getResources();
        this.mLocationNofityHandler = new LocationNofityHandler(this.vivoThread.getLooper());
        setupVivoReceiver();
        registerProcessObserver();
        registerForLocationToggle();
        this.mLocationNofityHandler.sendEmptyMessageDelayed(2, 5000L);
        this.mPackageMonitor.register(mContext, (Looper) null, UserHandle.ALL, true);
    }

    private void setupVivoReceiver() {
        this.mVivoBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.location.VivoLocationNotify.1
            {
                VivoLocationNotify.this = this;
            }

            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                try {
                    String action = intent.getAction();
                    if (VivoLocationNotify.DBG) {
                        VLog.d("VivoLocationNotify", "setupVivoReceiver action:" + action);
                    }
                    if (action.equals(VivoLocationNotify.ACTION_DO_NOT_POP_AGAIN)) {
                        String pk = (String) intent.getExtra("location_notify_package_name", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                        Message msg = VivoLocationNotify.this.mLocationNofityHandler.obtainMessage(3, pk);
                        VivoLocationNotify.this.mLocationNofityHandler.sendMessage(msg);
                    } else if (action.equals(VivoLocationNotify.ACTION_ENABLE_GPS)) {
                        VivoLocationNotify.this.mLocationNofityHandler.sendEmptyMessage(4);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        this.mLocationNofityHandler.post(new Runnable() { // from class: com.android.server.location.VivoLocationNotify.2
            {
                VivoLocationNotify.this = this;
            }

            @Override // java.lang.Runnable
            public void run() {
                VivoLocationNotify.this.mVivoIntentFilter = new IntentFilter();
                VivoLocationNotify.this.mVivoIntentFilter.addAction(VivoLocationNotify.ACTION_DO_NOT_POP_AGAIN);
                VivoLocationNotify.this.mVivoIntentFilter.addAction(VivoLocationNotify.ACTION_ENABLE_GPS);
                VivoLocationNotify.mContext.registerReceiver(VivoLocationNotify.this.mVivoBroadcastReceiver, VivoLocationNotify.this.mVivoIntentFilter);
            }
        });
    }

    public void updateVivoLocationNotifyApp(ContentValuesList list) {
        if (DBG) {
            VLog.d("VivoLocationNotify", "updateVivoLocationNotifyApp start");
        }
        if (list == null) {
            return;
        }
        int id = ID_START;
        int index = 1;
        HashMap<String, NotifySettings> notifyAppMap = new HashMap<>();
        while (true) {
            StringBuilder sb = new StringBuilder();
            sb.append("pkg");
            int index2 = index + 1;
            sb.append(index);
            String packageName = list.getValue(sb.toString());
            if (packageName != null) {
                NotifySettings settings = new NotifySettings();
                settings.setPackageName(packageName);
                settings.setNotifyId(id);
                notifyAppMap.put(packageName, settings);
                id++;
            }
            if (packageName == null) {
                break;
            }
            index = index2;
        }
        mNotifyAppMap = notifyAppMap;
        if (DBG && notifyAppMap != null) {
            dumpState();
        }
        if (DBG) {
            VLog.d("VivoLocationNotify", "updateVivoLocationNotifyApp end");
        }
    }

    private void dumpState() {
        try {
            VLog.d("VivoLocationNotify", "-------------------------dumpState begin-------------------------");
            for (Map.Entry<String, NotifySettings> entry : mNotifyAppMap.entrySet()) {
                entry.getValue().dumpSettings();
            }
            VLog.d("VivoLocationNotify", "-------------------------dumpState end---------------------------");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void enableVerboseLogging(int verbose) {
        if (verbose > 0) {
            DBG = true;
        } else {
            DBG = false;
        }
    }

    /* loaded from: classes.dex */
    public class NotifySettings {
        private boolean notifyEnabled;
        private int notifyID;
        private String packageName;

        private NotifySettings() {
            VivoLocationNotify.this = r1;
            this.packageName = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            this.notifyEnabled = true;
        }

        public void NotifySettings() {
            this.packageName = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            this.notifyEnabled = true;
            this.notifyID = VivoLocationNotify.ID_START;
        }

        public void NotifySettings(NotifySettings settings) {
            this.packageName = settings.packageName;
            this.notifyEnabled = settings.notifyEnabled;
        }

        public void setPackageName(String name) {
            this.packageName = name;
        }

        public void setNotifyEnabled(boolean enable) {
            this.notifyEnabled = enable;
        }

        public void setNotifyId(int id) {
            this.notifyID = id;
        }

        public void dumpSettings() {
            VLog.d("VivoLocationNotify", this.packageName + " " + this.notifyEnabled);
        }

        public String toString() {
            VLog.d("VivoLocationNotify", this.packageName + " " + this.notifyEnabled);
            StringBuffer sb = new StringBuffer();
            sb.append("packageName: ");
            sb.append(this.packageName);
            sb.append(", notifyEnabled: ");
            sb.append(this.notifyEnabled);
            return sb.toString();
        }
    }

    private void registerForLocationToggle() {
        final ContentObserver contentObserver = new ContentObserver(null) { // from class: com.android.server.location.VivoLocationNotify.3
            {
                VivoLocationNotify.this = this;
            }

            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                if (VivoLocationNotify.DBG) {
                    VLog.d("VivoLocationNotify", "registerForLocationToggle onChange");
                }
                VivoLocationNotify.this.mLocationNofityHandler.sendEmptyMessage(2);
            }
        };
        this.mLocationNofityHandler.post(new Runnable() { // from class: com.android.server.location.VivoLocationNotify.4
            {
                VivoLocationNotify.this = this;
            }

            @Override // java.lang.Runnable
            public void run() {
                VivoLocationNotify.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("location_providers_allowed"), false, contentObserver);
            }
        });
    }

    public void registerProcessObserver() {
        try {
            if (DBG) {
                VLog.d("VivoLocationNotify", "registerProcessObserver");
            }
            ActivityManagerNative.getDefault().registerProcessObserver(this.mProcessObserver);
        } catch (Exception e) {
            VLog.e("VivoLocationNotify", "error registerProcessObserver " + e);
        }
    }

    /* loaded from: classes.dex */
    public class LocationNofityHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public LocationNofityHandler(Looper looper) {
            super(looper);
            VivoLocationNotify.this = this$0;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            String appName;
            NotifySettings settings;
            super.handleMessage(msg);
            if (VivoLocationNotify.DBG) {
                VLog.d("VivoLocationNotify", "handleMessage " + msg.what);
            }
            try {
                int i = msg.what;
                if (i == 0) {
                    if (VivoLocationNotify.DBG) {
                        VLog.d("VivoLocationNotify", "mLocationMode is " + VivoLocationNotify.this.mLocationMode);
                    }
                    if (VivoLocationNotify.this.mLocationMode != 3 && VivoLocationNotify.this.mLocationMode != 1 && (appName = (String) msg.obj) != null && !appName.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK)) {
                        NotifySettings settings2 = VivoLocationNotify.mNotifyAppMap.get(appName);
                        int notifyType = 0;
                        if (settings2 != null && settings2.notifyEnabled) {
                            ContentResolver contentResolver = VivoLocationNotify.mContext.getContentResolver();
                            int enable = Settings.Global.getInt(contentResolver, VivoLocationNotify.SETTINGS_LOCATION_NOTIFY + appName, 0);
                            if (enable == 1) {
                                if (VivoLocationNotify.DBG) {
                                    VLog.d("VivoLocationNotify", "notify is disabled");
                                    return;
                                }
                                return;
                            }
                            if (VivoLocationNotify.this.mLocationMode == 2) {
                                notifyType = 1;
                            }
                            VivoLocationNotify.this.setNotificationVisible(true, notifyType, settings2);
                            settings2.notifyEnabled = false;
                        }
                    }
                } else if (i == 1) {
                    String appName2 = VivoLocationNotify.getAppName(msg.arg1, msg.arg2);
                    if (VivoLocationNotify.DBG) {
                        VLog.d("VivoLocationNotify", "appName:" + appName2);
                    }
                    if (appName2 != null && !appName2.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK) && (settings = VivoLocationNotify.mNotifyAppMap.get(appName2)) != null && VivoLocationNotify.this.isAppDied(appName2)) {
                        if (!settings.notifyEnabled) {
                            VivoLocationNotify.this.cancelNotification(settings);
                        }
                        settings.notifyEnabled = true;
                        if (VivoLocationNotify.DBG) {
                            VLog.d("VivoLocationNotify", "set appName notifyEnabled " + settings.notifyEnabled);
                        }
                    }
                } else if (i == 2) {
                    VivoLocationNotify.this.mLocationMode = Settings.Secure.getInt(VivoLocationNotify.mContext.getContentResolver(), "location_mode", 3);
                    if (VivoLocationNotify.DBG) {
                        VLog.d("VivoLocationNotify", "set mLocationMode:" + VivoLocationNotify.this.mLocationMode);
                    }
                    VivoLocationNotify.this.cancelAllNotification();
                } else if (i == 3) {
                    String appName3 = (String) msg.obj;
                    if (appName3 != null && !appName3.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK)) {
                        ContentResolver contentResolver2 = VivoLocationNotify.mContext.getContentResolver();
                        Settings.Global.putInt(contentResolver2, VivoLocationNotify.SETTINGS_LOCATION_NOTIFY + appName3, 1);
                        NotifySettings settings3 = VivoLocationNotify.mNotifyAppMap.get(appName3);
                        if (settings3 != null) {
                            VivoLocationNotify.this.cancelNotification(settings3);
                        }
                    }
                } else if (i == 4) {
                    Settings.Secure.putInt(VivoLocationNotify.mContext.getContentResolver(), "location_mode", 3);
                    VivoLocationNotify.this.cancelAllNotification();
                } else if (i == 6) {
                    String appName4 = (String) msg.obj;
                    NotifySettings settings4 = VivoLocationNotify.mNotifyAppMap.get(appName4);
                    if (settings4 != null) {
                        if (VivoLocationNotify.DBG) {
                            VLog.d("VivoLocationNotify", "settings :" + settings4);
                        }
                        VivoLocationNotify.this.cancelNotification(settings4);
                        settings4.notifyEnabled = true;
                        ContentResolver contentResolver3 = VivoLocationNotify.mContext.getContentResolver();
                        Settings.Global.putInt(contentResolver3, VivoLocationNotify.SETTINGS_LOCATION_NOTIFY + appName4, 0);
                        if (VivoLocationNotify.DBG) {
                            VLog.d("VivoLocationNotify", "MSG_PKG_REMOVED " + appName4);
                        }
                    } else if (VivoLocationNotify.DBG) {
                        VLog.d("VivoLocationNotify", "settings is null");
                    }
                }
            } catch (Exception e) {
                VLog.e("VivoLocationNotify", Log.getStackTraceString(e));
            }
        }
    }

    public void requestLocation(String pk) {
        if (DBG) {
            VLog.d("VivoLocationNotify", "requestLocation " + pk);
        }
        Message msg = this.mLocationNofityHandler.obtainMessage(0, pk);
        this.mLocationNofityHandler.sendMessage(msg);
    }

    public static String getProgramNameByPackageName(String packageName) {
        PackageManager pm = mContext.getPackageManager();
        try {
            String name = pm.getApplicationLabel(pm.getApplicationInfo(packageName, 128)).toString();
            return name;
        } catch (Exception e) {
            VLog.e("VivoLocationNotify", Log.getStackTraceString(e));
            return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        }
    }

    public static String getAppName(int pid, int uid) {
        String pk = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        try {
            pk = mContext.getPackageManager().getNameForUid(uid);
        } catch (Exception e) {
            VLog.e("VivoLocationNotify", Log.getStackTraceString(e));
        }
        if (DBG) {
            VLog.d("VivoLocationNotify", "getAppName " + pk);
        }
        return pk;
    }

    public boolean isAppDied(String pk) {
        boolean died = true;
        try {
            ApplicationInfo ai = mContext.getPackageManager().getApplicationInfo(pk, 1);
            ActivityManager mActivityManager = (ActivityManager) mContext.getSystemService(VivoFirewall.TYPE_ACTIVITY);
            List<ActivityManager.RunningAppProcessInfo> runTasks = mActivityManager.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo runAppInfo : runTasks) {
                if (ai.uid == runAppInfo.uid) {
                    died = false;
                }
            }
        } catch (Exception e) {
            VLog.e("VivoLocationNotify", Log.getStackTraceString(e));
        }
        if (DBG) {
            VLog.d("VivoLocationNotify", "isAppDied " + pk + ", died :" + died);
        }
        return died;
    }

    private boolean isLocationEnabled() {
        boolean enabled = Settings.Secure.getInt(mContext.getContentResolver(), "location_mode", 3) != 0;
        if (DBG) {
            VLog.d("VivoLocationNotify", "isLocationEnabled " + enabled);
        }
        return enabled;
    }

    public void setNotificationVisible(boolean visible, int type, NotifySettings settings) {
        Notification.Action enable;
        CharSequence title;
        CharSequence details;
        if (DBG) {
            VLog.d("VivoLocationNotify", "setNotificationVisible " + visible + ", type:" + type + ", " + settings);
        }
        if (settings == null || !settings.notifyEnabled) {
            return;
        }
        try {
            if (!visible) {
                this.mNotificationManager.cancelAsUser(null, settings.notifyID, UserHandle.ALL);
                return;
            }
            Bundle bundle = new Bundle();
            bundle.putInt("vivo.summaryIconRes", 50464099);
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "VivoLocationNotify", 4);
            this.mNotificationManager.createNotificationChannel(channel);
            this.mNotificationBuilder = new Notification.Builder(mContext, NOTIFICATION_CHANNEL_ID).setWhen(0L).setSmallIcon(50464100).setExtras(bundle).setAutoCancel(true).setContentIntent(TaskStackBuilder.create(mContext).addNextIntentWithParentStack(new Intent("android.settings.LOCATION_SOURCE_SETTINGS")).getPendingIntent(0, 0, null, UserHandle.CURRENT)).setColor(this.mResources.getColor(50856011));
            Notification.Action donotPopAgain = new Notification.Action.Builder((Icon) null, this.mResources.getText(51249830), getPrivateBroadcast(ACTION_DO_NOT_POP_AGAIN, settings)).build();
            if (type == 1) {
                enable = new Notification.Action.Builder((Icon) null, this.mResources.getText(51249832), getPrivateBroadcast(ACTION_ENABLE_GPS, settings)).build();
            } else {
                enable = new Notification.Action.Builder((Icon) null, this.mResources.getText(51249831), getPrivateBroadcast(ACTION_ENABLE_GPS, settings)).build();
            }
            this.mNotificationBuilder.addAction(donotPopAgain).addAction(enable);
            if (type == 1) {
                title = this.mResources.getText(51249807);
            } else {
                title = this.mResources.getText(51249829);
            }
            if (type == 1) {
                details = this.mResources.getString(51249806, getProgramNameByPackageName(settings.packageName));
            } else {
                details = this.mResources.getString(ID_START, getProgramNameByPackageName(settings.packageName));
            }
            this.mNotificationBuilder.setTicker(title);
            this.mNotificationBuilder.setContentTitle(title);
            this.mNotificationBuilder.setContentText(details);
            this.mNotificationManager.notifyAsUser(null, settings.notifyID, this.mNotificationBuilder.build(), UserHandle.ALL);
        } catch (Exception e) {
            VLog.e("VivoLocationNotify", Log.getStackTraceString(e));
        }
    }

    public void cancelNotification(NotifySettings settings) {
        if (DBG) {
            VLog.d("VivoLocationNotify", "cancelNotification " + settings.packageName);
        }
        this.mNotificationManager.cancelAsUser(null, settings.notifyID, UserHandle.ALL);
    }

    public void cancelAllNotification() {
        if (DBG) {
            VLog.d("VivoLocationNotify", "cancelAllNotification");
        }
        for (Map.Entry<String, NotifySettings> entry : mNotifyAppMap.entrySet()) {
            NotifySettings settings = entry.getValue();
            this.mNotificationManager.cancelAsUser(null, settings.notifyID, UserHandle.ALL);
        }
    }

    private PendingIntent getBroadcast(Context context, int requestCode, Intent intent, int flags) {
        return PendingIntent.getBroadcast(context, requestCode, intent, flags);
    }

    private PendingIntent getPrivateBroadcast(String action, NotifySettings settings) {
        Intent intent = new Intent(action).setPackage(VivoPermissionUtils.OS_PKG);
        intent.putExtra("location_notify_package_name", settings.packageName);
        return getBroadcast(mContext, 0, intent, Dataspace.RANGE_FULL);
    }
}