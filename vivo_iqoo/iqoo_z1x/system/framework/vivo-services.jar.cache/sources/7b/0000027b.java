package com.android.server.location;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.WorkSource;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import com.android.server.wm.VCD_FF_1;
import com.vivo.common.VivoCollectData;
import com.vivo.common.utils.VLog;
import com.vivo.vcodetransbase.EventTransfer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import vivo.app.configuration.ContentValuesList;

/* loaded from: classes.dex */
public class VivoCn0WeakManager {
    private static final String ACTION_CLICK_NOTIFICATION = "com.vivo.location.ACTION_CLICK_NOTIFICATION";
    private static final String ACTION_REMOVE_NOTIFICATION = "com.vivo.location.ACTION_REMOVE_NOTIFICATION";
    private static final String CN0_WEAK_LABEL = "190604";
    private static final String DO_NOT_ALERT_ANYMORE = "persist.vivo.cn0weak.notshowagain";
    private static final String LOCTION_ID = "1906";
    private static final int MSG_CLICK_NOTIFICATION = 11;
    private static final int MSG_COLLECT_BIGDATA = 6;
    private static final int MSG_NETWORK_LOCATION_REPORT = 5;
    private static final int MSG_NEVER_SHOW_AGAIN = 8;
    private static final int MSG_REFRESH_CONFIG = 9;
    private static final int MSG_REMOVE_NOTIFICATION = 12;
    private static final int MSG_REPORT_LOCATION = 4;
    private static final int MSG_REPORT_SVSTATUS = 2;
    private static final int MSG_SET_INTENT_FILTER = 13;
    private static final int MSG_START_NAVIGATING = 1;
    private static final int MSG_START_NOTIFICATION = 10;
    private static final int MSG_STOP_NAVIGATING = 3;
    private static final int MSG_VIEW_LOCATION_HELP = 7;
    private static final String NOTIFICATION_CHANNEL_ID = "vivo_cn0_weak_notify";
    private static final String TAG = "VivoCn0WeakManager";
    private boolean hasAppInList;
    private MyHandler mChargeHandler;
    private HandlerThread mChargeThread;
    private Context mContext;
    private LocationManager mLocationManager;
    private Notification.Builder mNotificationBuilder;
    private NotificationManager mNotificationManager;
    private Resources mResources;
    private Object mVCD;
    public static boolean DEBUG = false;
    protected static int mMaxCn0 = 0;
    protected static int mMaxSpeed = 0;
    protected static boolean mPopOut = false;
    protected static int mCountTime = 0;
    protected static boolean mMoreThanDisLimit = false;
    protected static int mDistanceSinceFist = 0;
    protected static int mCn0WeakDriveThreshold = 0;
    protected static int mCn0WeakWalkThreshold = 0;
    protected static boolean mIfPopAlert = false;
    protected static long mRequestTimeLimit = 0;
    protected static int mTimesLimit = 0;
    protected static int mAlertLimitDay = 0;
    protected static int mDistanceLimit = 0;
    protected static int mSpeedLimit = 0;
    private static ArrayList<String> mCn0WeakPackageList = null;
    private static VivoCn0WeakDebugPanel mVivoCn0WeakDebugPanel = null;
    private static boolean DBG = true;
    private static boolean mCn0WeakManagerOn = false;
    private long mStartNaviTime = 0;
    private boolean mFirst = false;
    private Location mFirstNetLoc = null;
    private Location mLastNetLoc = null;
    private long mLastPopTime = 0;
    private boolean mNeverShowAgain = false;
    private int mViewHelp = 0;
    private long mStartNotification = 0;
    private long mClickNotification = 0;
    private long mRemoveNotification = 0;
    private long mStartNavigation = 0;
    private IntentReceiver mIntentReceiver = null;
    private DialogInterface.OnClickListener dialogListen = new DialogInterface.OnClickListener() { // from class: com.android.server.location.VivoCn0WeakManager.3
        @Override // android.content.DialogInterface.OnClickListener
        public void onClick(DialogInterface dialog, int which) {
            if (which == -2) {
                VivoCn0WeakManager.this.setNotificationVisible();
            } else if (which != -1) {
                VivoCn0WeakManager.this.setNotificationVisible();
            } else {
                try {
                    VivoCn0WeakManager.this.mChargeHandler.sendEmptyMessage(7);
                    Intent intent = new Intent("com.vivo.settings.action.LOCATION_HELP");
                    intent.setFlags(268435456);
                    VivoCn0WeakManager.this.mContext.startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };
    private LocationListener mPassiveLocationListener = new LocationListener() { // from class: com.android.server.location.VivoCn0WeakManager.4
        @Override // android.location.LocationListener
        public void onLocationChanged(Location location) {
            if (location.getProvider().equals("network")) {
                VivoCn0WeakManager.this.mChargeHandler.obtainMessage(5, 0, 1, location).sendToTarget();
            }
        }

        @Override // android.location.LocationListener
        public void onStatusChanged(String provider, int status, Bundle extras) {
            if (VivoCn0WeakManager.DEBUG) {
                VLog.d(VivoCn0WeakManager.TAG, "onStatusChanged : provider" + provider + "status:" + status + "extras" + extras.toString());
            }
        }

        @Override // android.location.LocationListener
        public void onProviderEnabled(String provider) {
            if (VivoCn0WeakManager.DEBUG) {
                VLog.d(VivoCn0WeakManager.TAG, "onProviderEnabled : provider" + provider);
            }
        }

        @Override // android.location.LocationListener
        public void onProviderDisabled(String provider) {
            if (VivoCn0WeakManager.DEBUG) {
                VLog.d(VivoCn0WeakManager.TAG, "onProviderDisabled : provider" + provider);
            }
        }
    };

    static /* synthetic */ int access$612(VivoCn0WeakManager x0, int x1) {
        int i = x0.mViewHelp + x1;
        x0.mViewHelp = i;
        return i;
    }

    public VivoCn0WeakManager(Context context) {
        this.mContext = null;
        this.mChargeThread = null;
        this.mChargeHandler = null;
        this.mLocationManager = null;
        this.mVCD = null;
        this.hasAppInList = false;
        this.mContext = context;
        this.mLocationManager = (LocationManager) context.getSystemService("location");
        this.mVCD = getVCD(context);
        HandlerThread handlerThread = new HandlerThread("gps_cn0_weak");
        this.mChargeThread = handlerThread;
        handlerThread.start();
        this.mChargeHandler = new MyHandler(this.mChargeThread.getLooper());
        if (mVivoCn0WeakDebugPanel == null) {
            mVivoCn0WeakDebugPanel = new VivoCn0WeakDebugPanel(this.mContext);
        }
        this.hasAppInList = false;
        this.mChargeHandler.sendEmptyMessageDelayed(13, 0L);
        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        this.mResources = this.mContext.getResources();
    }

    private Object getVCD(Context context) {
        if (this.mVCD == null) {
            this.mVCD = new VivoCollectData(context);
        }
        return this.mVCD;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg == null) {
                return;
            }
            switch (msg.what) {
                case 1:
                    if (VivoCn0WeakManager.DEBUG) {
                        VLog.d(VivoCn0WeakManager.TAG, "MSG_START_NAVIGATING");
                    }
                    VivoCn0WeakManager.this.mStartNaviTime = ((Long) msg.obj).longValue();
                    return;
                case 2:
                    if (VivoCn0WeakManager.DEBUG) {
                        VLog.d(VivoCn0WeakManager.TAG, "MSG_REPORT_SVSTATUS " + ((Integer) msg.obj).intValue() + " Max:" + VivoCn0WeakManager.mMaxCn0);
                    }
                    if (((Integer) msg.obj).intValue() > VivoCn0WeakManager.mMaxCn0) {
                        VivoCn0WeakManager.mMaxCn0 = ((Integer) msg.obj).intValue();
                    }
                    if (VivoCn0WeakManager.mMaxCn0 > 32) {
                        VivoCn0WeakManager.mCountTime = 0;
                        return;
                    }
                    return;
                case 3:
                    VivoCn0WeakManager.this.mFirst = true;
                    if (VivoCn0WeakManager.DEBUG) {
                        VLog.d(VivoCn0WeakManager.TAG, "MSG_STOP_NAVIGATING " + VivoCn0WeakManager.this.mStartNaviTime + " " + ((Long) msg.obj).longValue() + " " + VivoCn0WeakManager.mRequestTimeLimit);
                    }
                    if (((Long) msg.obj).longValue() - VivoCn0WeakManager.this.mStartNaviTime > VivoCn0WeakManager.mRequestTimeLimit && VivoCn0WeakManager.mMoreThanDisLimit) {
                        VivoCn0WeakManager.this.chargeIfPopOut();
                    }
                    VivoCn0WeakManager.mMaxCn0 = 0;
                    VivoCn0WeakManager.mMaxSpeed = 0;
                    VivoCn0WeakManager.mMoreThanDisLimit = false;
                    VivoCn0WeakManager.mDistanceSinceFist = 0;
                    return;
                case 4:
                    if (((Integer) msg.obj).intValue() > VivoCn0WeakManager.mSpeedLimit) {
                        VivoCn0WeakManager.mMaxSpeed = ((Integer) msg.obj).intValue();
                        return;
                    }
                    return;
                case 5:
                    Location location = (Location) msg.obj;
                    if (VivoCn0WeakManager.this.mFirst) {
                        VivoCn0WeakManager.this.mFirstNetLoc = location;
                        VivoCn0WeakManager.this.mLastNetLoc = location;
                        VivoCn0WeakManager.this.mFirst = false;
                    } else {
                        VivoCn0WeakManager.this.mLastNetLoc = location;
                    }
                    if (!VivoCn0WeakManager.this.mFirst && VivoCn0WeakManager.this.mFirstNetLoc != null && VivoCn0WeakManager.this.mLastNetLoc != null && ((int) VivoCn0WeakManager.this.mFirstNetLoc.distanceTo(VivoCn0WeakManager.this.mLastNetLoc)) >= VivoCn0WeakManager.mDistanceLimit) {
                        VivoCn0WeakManager.mMoreThanDisLimit = true;
                        VivoCn0WeakManager.mDistanceSinceFist = (int) VivoCn0WeakManager.this.mFirstNetLoc.distanceTo(VivoCn0WeakManager.this.mLastNetLoc);
                        VLog.i(VivoCn0WeakManager.TAG, "MSG_NETWORK_LOCATION_REPORT" + VivoCn0WeakManager.mDistanceSinceFist);
                        return;
                    }
                    return;
                case 6:
                    String buf = VivoCn0WeakManager.mPopOut + "," + VivoCn0WeakManager.mCountTime + "," + VivoCn0WeakManager.this.mNeverShowAgain + "," + VivoCn0WeakManager.this.mViewHelp + "," + VivoCn0WeakManager.this.mStartNavigation + "," + VivoCn0WeakManager.this.mStartNotification + "," + VivoCn0WeakManager.this.mClickNotification + "," + VivoCn0WeakManager.this.mRemoveNotification + "|" + VivoCn0WeakManager.mCn0WeakDriveThreshold + "," + VivoCn0WeakManager.mCn0WeakWalkThreshold + "," + VivoCn0WeakManager.mIfPopAlert;
                    VivoCn0WeakManager.this.collectBD(buf);
                    HashMap<String, String> params = new HashMap<>();
                    params.put(VCD_FF_1.UUID_STR, UUID.randomUUID().toString());
                    params.put("pop", String.valueOf(VivoCn0WeakManager.mPopOut));
                    params.put("count", String.valueOf(VivoCn0WeakManager.mCountTime));
                    params.put("never", String.valueOf(VivoCn0WeakManager.this.mNeverShowAgain));
                    params.put("view", String.valueOf(VivoCn0WeakManager.this.mNeverShowAgain));
                    params.put("start", String.valueOf(VivoCn0WeakManager.this.mStartNavigation));
                    params.put("startn", String.valueOf(VivoCn0WeakManager.this.mStartNotification));
                    params.put("clickn", String.valueOf(VivoCn0WeakManager.this.mClickNotification));
                    params.put("removen", String.valueOf(VivoCn0WeakManager.this.mRemoveNotification));
                    params.put("drivet", String.valueOf(VivoCn0WeakManager.mCn0WeakDriveThreshold));
                    params.put("walkt", String.valueOf(VivoCn0WeakManager.mCn0WeakWalkThreshold));
                    params.put("ifpop", String.valueOf(VivoCn0WeakManager.mIfPopAlert));
                    EventTransfer.getInstance().singleEvent("F500", "F500|10009", System.currentTimeMillis(), 0L, params);
                    VivoCn0WeakManager.this.mStartNavigation = 0L;
                    VivoCn0WeakManager.this.mStartNotification = 0L;
                    VivoCn0WeakManager.this.mClickNotification = 0L;
                    VivoCn0WeakManager.this.mRemoveNotification = 0L;
                    VivoCn0WeakManager.mPopOut = false;
                    VivoCn0WeakManager.mCountTime = 0;
                    VivoCn0WeakManager.this.mViewHelp = 0;
                    return;
                case 7:
                    VivoCn0WeakManager.access$612(VivoCn0WeakManager.this, 1);
                    VivoCn0WeakManager.this.mChargeHandler.sendEmptyMessage(6);
                    return;
                case 8:
                    VivoCn0WeakManager.this.mNeverShowAgain = true;
                    return;
                case 9:
                default:
                    return;
                case 10:
                    VivoCn0WeakManager.this.mStartNotification = ((Long) msg.obj).longValue();
                    return;
                case 11:
                    VLog.d(VivoCn0WeakManager.TAG, "MSG_CLICK_NOTIFICATION");
                    VivoCn0WeakManager.this.mClickNotification = ((Long) msg.obj).longValue();
                    VivoCn0WeakManager.this.mChargeHandler.sendEmptyMessage(6);
                    return;
                case 12:
                    VLog.d(VivoCn0WeakManager.TAG, "MSG_REMOVE_NOTIFICATION");
                    VivoCn0WeakManager.this.mRemoveNotification = ((Long) msg.obj).longValue();
                    VivoCn0WeakManager.this.mChargeHandler.sendEmptyMessage(6);
                    return;
                case 13:
                    VivoCn0WeakManager.this.setupReceiver();
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setupReceiver() {
        this.mIntentReceiver = new IntentReceiver();
        IntentFilter f = new IntentFilter();
        f.addAction(ACTION_CLICK_NOTIFICATION);
        f.addAction(ACTION_REMOVE_NOTIFICATION);
        this.mContext.registerReceiver(this.mIntentReceiver, f);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class IntentReceiver extends BroadcastReceiver {
        private IntentReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                if (action == null) {
                    return;
                }
                VLog.d(VivoCn0WeakManager.TAG, "IntentReceiver " + action);
                if (action.equals(VivoCn0WeakManager.ACTION_CLICK_NOTIFICATION)) {
                    long now = System.currentTimeMillis();
                    VivoCn0WeakManager.this.mChargeHandler.obtainMessage(11, 0, 1, Long.valueOf(now)).sendToTarget();
                    Intent helpIntent = new Intent("com.vivo.settings.action.LOCATION_HELP");
                    helpIntent.setFlags(268435456);
                    VivoCn0WeakManager.this.mContext.startActivity(helpIntent);
                } else if (action.equals(VivoCn0WeakManager.ACTION_REMOVE_NOTIFICATION)) {
                    long now2 = System.currentTimeMillis();
                    VivoCn0WeakManager.this.mChargeHandler.obtainMessage(12, 0, 1, Long.valueOf(now2)).sendToTarget();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isBeyondDayLimit() {
        long now = System.currentTimeMillis();
        long countDay = (now - this.mLastPopTime) / 86400000;
        if (countDay >= mAlertLimitDay) {
            return true;
        }
        return false;
    }

    private boolean isHasListApp(WorkSource source) {
        boolean result = false;
        if (DEBUG) {
            VLog.d(TAG, "isHasListApp " + source.size());
        }
        for (int i = 0; i < source.size(); i++) {
            if (source.getName(i) != null && (result = isCallPkgInWhiteList(source.getName(i)))) {
                return result;
            }
        }
        return result;
    }

    private boolean isCallPkgInWhiteList(String packageName) {
        ArrayList<String> wPkgList = mCn0WeakPackageList;
        if (wPkgList != null && wPkgList.size() > 0) {
            if (DEBUG) {
                VLog.d(TAG, "All pkg " + wPkgList);
            }
            Iterator<String> it = wPkgList.iterator();
            while (it.hasNext()) {
                String pkgTemp = it.next();
                if (pkgTemp != null && packageName.contains(pkgTemp)) {
                    return true;
                }
            }
        }
        if (DEBUG) {
            VLog.d(TAG, "pkg " + packageName + " check Failed! Not in W_List");
            return false;
        }
        return false;
    }

    public void chargeIfPopOut() {
        int cn0Threhold;
        if (mMaxSpeed > mSpeedLimit) {
            cn0Threhold = mCn0WeakDriveThreshold;
        } else {
            cn0Threhold = mCn0WeakWalkThreshold;
        }
        if (mMaxCn0 < cn0Threhold) {
            mCountTime++;
            VLog.d(TAG, "chargeIfPopOut " + mCountTime);
        } else {
            mCountTime = 0;
        }
        if (mCountTime >= mTimesLimit) {
            VLog.d(TAG, "chargeIfPopOut popOut = true");
            mPopOut = true;
        }
        if (mPopOut && mIfPopAlert && !this.mNeverShowAgain && isBeyondDayLimit()) {
            VLog.d(TAG, "chargeIfPopOut popOutSuggestion");
            popOutSuggestion();
            this.mStartNavigation = this.mStartNaviTime;
        }
        if (mPopOut) {
            if (!mIfPopAlert || this.mNeverShowAgain) {
                this.mChargeHandler.sendEmptyMessage(6);
            }
        }
    }

    private void popOutSuggestion() {
        this.mLastPopTime = System.currentTimeMillis();
        LayoutInflater inflater = LayoutInflater.from(this.mContext);
        View view = inflater.inflate(50528431, (ViewGroup) null);
        CheckBox checkBox = (CheckBox) view.findViewById(51183694);
        AlertDialog tipsDialog = new AlertDialog.Builder(this.mContext, 51314692).setCancelable(false).setTitle(51249310).setView(view).setNegativeButton(51249304, this.dialogListen).setPositiveButton(51249307, this.dialogListen).setOnKeyListener(new DialogInterface.OnKeyListener() { // from class: com.android.server.location.VivoCn0WeakManager.1
            @Override // android.content.DialogInterface.OnKeyListener
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == 4 && event.getRepeatCount() == 0) {
                    return true;
                }
                return false;
            }
        }).create();
        tipsDialog.getWindow().setType(2003);
        tipsDialog.show();
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: com.android.server.location.VivoCn0WeakManager.2
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    SystemProperties.set(VivoCn0WeakManager.DO_NOT_ALERT_ANYMORE, "yes");
                    VivoCn0WeakManager.this.mChargeHandler.sendEmptyMessage(8);
                    return;
                }
                SystemProperties.set(VivoCn0WeakManager.DO_NOT_ALERT_ANYMORE, "no");
            }
        });
        checkBox.setChecked(false);
    }

    public void handleSetRequest(WorkSource source, long requestTime) {
        this.hasAppInList = isHasListApp(source);
    }

    public void startNavigating(long startTime) {
        if (this.hasAppInList) {
            this.mChargeHandler.obtainMessage(1, 0, 1, Long.valueOf(startTime)).sendToTarget();
            this.mLocationManager.requestLocationUpdates("passive", 0L, 0.0f, this.mPassiveLocationListener);
        }
    }

    public void stopNavigating(long stopTime) {
        if (this.hasAppInList) {
            this.mLocationManager.removeUpdates(this.mPassiveLocationListener);
            this.mChargeHandler.obtainMessage(3, 0, 1, Long.valueOf(stopTime)).sendToTarget();
        }
    }

    public void handleReportSvStatue(int topAverageCno) {
        if (this.hasAppInList) {
            this.mChargeHandler.obtainMessage(2, 0, 1, Integer.valueOf(topAverageCno)).sendToTarget();
        }
    }

    public void handleReportLocation(int speed) {
        if (this.hasAppInList) {
            this.mChargeHandler.obtainMessage(4, 0, 1, Integer.valueOf(speed)).sendToTarget();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void collectBD(String result) {
        try {
            if (this.mVCD != null) {
                VLog.d(TAG, result);
                if (((VivoCollectData) this.mVCD).getControlInfo(LOCTION_ID)) {
                    HashMap<String, String> params = new HashMap<>();
                    params.clear();
                    params.put("Cn0Weak", result);
                    ((VivoCollectData) this.mVCD).writeData(LOCTION_ID, CN0_WEAK_LABEL, System.currentTimeMillis(), System.currentTimeMillis(), 0L, 1, params);
                }
            }
        } catch (Exception e) {
            VLog.e(TAG, Log.getStackTraceString(e));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setNotificationVisible() {
        try {
            Bundle bundle = new Bundle();
            bundle.putInt("vivo.summaryIconRes", 50464099);
            bundle.putString("android.substName", this.mResources.getText(51249308).toString());
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "VivoGpsCn0WeakNotify", 2);
            this.mNotificationManager.createNotificationChannel(channel);
            CharSequence title = this.mResources.getText(51249310);
            CharSequence details = this.mResources.getString(51249309);
            Notification.Builder contentText = new Notification.Builder(this.mContext, NOTIFICATION_CHANNEL_ID).setGroupSummary(true).setGroup(NOTIFICATION_CHANNEL_ID).setSmallIcon(50464100).setExtras(bundle).setShowWhen(true).setWhen(System.currentTimeMillis()).setAutoCancel(true).setContentIntent(PendingIntent.getBroadcast(this.mContext, 0, new Intent(ACTION_CLICK_NOTIFICATION), 0)).setDeleteIntent(PendingIntent.getBroadcast(this.mContext, 0, new Intent(ACTION_REMOVE_NOTIFICATION), 0)).setColor(this.mResources.getColor(50856011)).setContentTitle(title).setContentText(details);
            this.mNotificationBuilder = contentText;
            this.mNotificationManager.notifyAsUser(null, 0, contentText.build(), UserHandle.ALL);
            long now = System.currentTimeMillis();
            this.mChargeHandler.obtainMessage(10, 0, 1, Long.valueOf(now)).sendToTarget();
        } catch (Exception e) {
            VLog.e(TAG, Log.getStackTraceString(e));
        }
    }

    public static void parseCn0WeakConfig(ContentValuesList list) {
        String[] projectList;
        String[] packages;
        if (DEBUG) {
            VLog.d(TAG, "parseCn0WeakConfig begin");
        }
        if (list == null) {
            return;
        }
        String device = Build.DEVICE;
        if (device != null) {
            device = device.toLowerCase();
        }
        String projects = list.getValue("project");
        if (projects != null) {
            projectList = projects.split(",");
        } else {
            projectList = new String[0];
        }
        if (device != null && projectList.length > 0) {
            int i = 0;
            while (true) {
                if (i >= projectList.length) {
                    break;
                }
                String temp = projectList[i];
                String[] tempSplit = temp.split("-");
                VLog.d(TAG, TAG + tempSplit[0] + " " + device);
                if (!device.equals(tempSplit[0])) {
                    i++;
                } else {
                    mCn0WeakManagerOn = true;
                    mCn0WeakWalkThreshold = Integer.parseInt(tempSplit[1]);
                    mCn0WeakDriveThreshold = Integer.parseInt(tempSplit[2]);
                    mIfPopAlert = Boolean.parseBoolean(tempSplit[3]);
                    break;
                }
            }
        }
        ArrayList<String> cn0WeakPackageList = new ArrayList<>();
        String packageList = list.getValue("packagesList");
        if (packageList != null) {
            packages = packageList.split(",");
        } else {
            packages = new String[0];
        }
        if (packageList != null && packages.length > 0) {
            for (String str : packages) {
                cn0WeakPackageList.add(str);
            }
        }
        mCn0WeakPackageList = cn0WeakPackageList;
        mTimesLimit = Integer.parseInt(list.getValue("timesLimit"));
        mRequestTimeLimit = Long.parseLong(list.getValue("requestTimeLimit"));
        mAlertLimitDay = Integer.parseInt(list.getValue("alertLimitDay"));
        mDistanceLimit = Integer.parseInt(list.getValue("distanceLimit"));
        mSpeedLimit = Integer.parseInt(list.getValue("speedLimit"));
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("projectList:" + projectList);
            sb.append(", packageList:" + cn0WeakPackageList);
            sb.append(", timesLimit:" + mTimesLimit);
            sb.append(", requestTimeLimit:" + mRequestTimeLimit);
            sb.append(", alertLimitDay:" + mAlertLimitDay);
            sb.append(", distanceLimit:" + mDistanceLimit);
            sb.append(", speedLimit:" + mSpeedLimit);
            VLog.d(TAG, sb.toString());
            VLog.d(TAG, "parseCn0WeakConfig end");
        }
    }

    public static boolean isCn0WeakManagerOn() {
        return mCn0WeakManagerOn;
    }
}