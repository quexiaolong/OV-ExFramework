package com.android.server.usb;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.debug.AdbManagerInternal;
import android.hardware.graphics.common.V1_0.Dataspace;
import android.hardware.usb.UsbManager;
import android.multidisplay.MultiDisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.RemoteViews;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.server.LocalServices;
import com.android.server.policy.VivoPolicyConstant;
import com.android.server.policy.key.VivoOTGKeyHandler;
import com.android.server.usb.UsbDeviceManager;
import com.vivo.face.common.data.Constants;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;
import vivo.app.VivoFrameworkFactory;
import vivo.app.phonelock.AbsVivoPhoneLockManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoUsbHandlerImpl implements IVivoUsbHandler {
    private static final String ACTION_USB_NOTIFICATION = "com.vivo.usb.notifications.intent.action";
    static final String ADB_NOTIFICATION_CHANNEL_ID_TV = "usbdevicemanager.adb.tv";
    private static final String BBKLOG_STATUS = "adblog_status";
    static boolean DEBUG = false;
    private static final int FLAG_PHONE_LOCKED = 1;
    private static final String INTENT_BUTTONID_TAG = "UsbButtonId";
    private static final boolean ISOverseas;
    private static final boolean IS_DEVICE_RW;
    private static final boolean IS_ENG;
    private static final boolean IS_LOG_CTRL_OPEN;
    private static final boolean IS_NET_ENTRY;
    private static final String KEY_VIVO_LOG_CTRL = "persist.sys.log.ctrl";
    private static final int MSG_SEND_USB_HELP_BROADCAST = 1000;
    private static final int MSG_SET_CURRENT_FUNCTIONS = 2;
    private static final int MSG_SET_SCREEN_UNLOCKED_FUNCTIONS = 12;
    private static final String MTK_DEBUG_PORT = "mtp,adb,acm";
    private static final String OP_ENTRY;
    private static final String QCOM_DEBUG_PORT = "diag,serial_cdev,serial_tty,rmnet_ipa,mass_storage,adb";
    private static final String QCOM_DEBUG_PORT9053 = "diag,diag_mdm,serial_cdev,serial_cdev_mdm,nmea,rmnet,mass_storage,adb";
    private static final String SAMSUNG_DEBUG_PORT = "dm,acm,adb";
    private static final String SAMSUNG_DEBUG_PORT9815 = "lsi-dm,acm,adb";
    private static String TAG = "VivoUsbDeviceManager";
    private static final int TAG_USB_CHARGING_MODE_ID = 4;
    private static final int TAG_USB_DEBUG_MANUAL_ID = 6;
    private static final int TAG_USB_DEBUG_TIPS_ID = 5;
    private static final int TAG_USB_HELP_ID = 1;
    private static final int TAG_USB_MANAGER_FILE_ID = 3;
    private static final int TAG_USB_TRANSFER_PHOTO_ID = 2;
    private static final String USB_FUNCTION_CHARGING_ONLY = "charging";
    private static final String USB_NOTIFICATION_HELP_MANUAL_ACTION = "com.android.bbk_phoneInstructions_jump";
    private static final String USB_NOTIFICATION_HELP_MANUAL_NEW_ACTION = "com.vivo.space.phonemanual.ManualDetailActivity";
    static final String USB_PERSISTENT_CONFIG_PROPERTY = "persist.sys.usb.config";
    private static final String USB_PREFS_XML = "UsbDeviceManagerPrefs.xml";
    static final String USB_STATE_PROPERTY = "sys.usb.state";
    static final int mStaticUsbAdbDebugTipsNotificationId = 110110;
    static final int mStaticUsbNotificationId = 10011;
    private ContentResolver mContentResolver;
    private Context mContext;
    protected boolean mIsDebugPort;
    private boolean mIsUsbGadgetExist;
    public NotificationManager mNotificationManager;
    private SharedPreferences mSettings;
    private String mSocId;
    private StatusBarManager mStatusBarManager;
    private boolean mUsbAdbDebugIsOpen;
    private boolean mUsbAdbDebugTipsIsClickByUser;
    private UsbDeviceManager.UsbHandler mUsbHandler;
    private IVivoUsbHandlerLegacy mUsbHandlerLegacy;
    private boolean mVivoAdbNotificationIsShown;
    private VivoUsbCustomization mVivoUsbCustomization;
    private String mPlatform = SystemProperties.get("ro.board.platform", "def");
    private int mLastId = 0;

    static {
        boolean z = false;
        IS_ENG = Build.TYPE.equals("eng") || Build.TYPE.equals("branddebug");
        boolean equals = "yes".equals(SystemProperties.get("persist.sys.log.ctrl", "no"));
        IS_LOG_CTRL_OPEN = equals;
        if (equals || IS_ENG) {
            z = true;
        }
        DEBUG = z;
        OP_ENTRY = SystemProperties.get("ro.vivo.op.entry", "no");
        IS_NET_ENTRY = isNetEntry();
        IS_DEVICE_RW = isDeviceRW();
        ISOverseas = "yes".equals(SystemProperties.get("ro.vivo.product.overseas", "no"));
    }

    public VivoUsbHandlerImpl(UsbDeviceManager.UsbHandler usbHandler, Context context) {
        this.mSocId = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mIsUsbGadgetExist = false;
        this.mUsbHandler = usbHandler;
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        this.mSettings = getPinnedSharedPrefs(this.mContext);
        this.mStatusBarManager = (StatusBarManager) this.mContext.getSystemService("statusbar");
        this.mUsbAdbDebugTipsIsClickByUser = "yes".equals(Settings.System.getString(this.mContext.getContentResolver(), "usb.adb.debug.noti.clicked"));
        this.mUsbAdbDebugIsOpen = Settings.Global.getInt(this.mContext.getContentResolver(), "vivo_development_show", 0) == 1;
        this.mVivoUsbCustomization = new VivoUsbCustomization(this.mContentResolver, this, context);
        UsbBroadcastReceiver usbNotificationReceiver = new UsbBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_USB_NOTIFICATION);
        this.mContext.registerReceiver(usbNotificationReceiver, intentFilter);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("vivo_development_show"), true, new ContentObserver(this.mUsbHandler) { // from class: com.android.server.usb.VivoUsbHandlerImpl.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                VivoUsbHandlerImpl vivoUsbHandlerImpl = VivoUsbHandlerImpl.this;
                vivoUsbHandlerImpl.mUsbAdbDebugIsOpen = 1 == Settings.Global.getInt(vivoUsbHandlerImpl.mContext.getContentResolver(), "vivo_development_show", 0);
                VivoUsbHandlerImpl vivoUsbHandlerImpl2 = VivoUsbHandlerImpl.this;
                vivoUsbHandlerImpl2.mUsbAdbDebugTipsIsClickByUser = "yes".equals(Settings.System.getString(vivoUsbHandlerImpl2.mContext.getContentResolver(), "usb.adb.debug.noti.clicked"));
                String str = VivoUsbHandlerImpl.TAG;
                VSlog.d(str, "Setting change  selfChange=" + selfChange + " mUsbAdbDebugIsOpen=" + VivoUsbHandlerImpl.this.mUsbAdbDebugIsOpen + " ,mUsbAdbDebugTipsHide=" + VivoUsbHandlerImpl.this.mUsbAdbDebugTipsIsClickByUser);
                VivoUsbHandlerImpl.this.mUsbHandler.updateAdbNotification(false);
            }
        });
        if ("msm8937".equals(this.mPlatform) || "msm8917".equals(this.mPlatform)) {
            this.mSocId = getSocId();
        }
        File usbGadgetFile = new File("/config/usb_gadget");
        if (usbGadgetFile.exists() && usbGadgetFile.isDirectory()) {
            this.mIsUsbGadgetExist = true;
        }
        registerLogBroadcast(this.mContext);
    }

    private void registerLogBroadcast(Context mContext) {
        IntentFilter bbklogFilter = new IntentFilter();
        bbklogFilter.addAction(VivoPolicyConstant.ACTION_VIVO_LOG_CHANGED);
        mContext.registerReceiver(new BroadcastReceiver() { // from class: com.android.server.usb.VivoUsbHandlerImpl.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                boolean status = "on".equals(intent.getStringExtra(VivoUsbHandlerImpl.BBKLOG_STATUS));
                String str = VivoUsbHandlerImpl.TAG;
                VSlog.w(str, "*****SWITCH LOG TO " + status);
                VivoUsbHandlerImpl.DEBUG = status;
                UsbDeviceManager.DEBUG = status;
            }
        }, bbklogFilter, null, this.mUsbHandler);
    }

    private String getSocId() {
        File file = new File("/sys/devices/soc0/soc_id");
        String socId = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        try {
            if (file.exists()) {
                socId = readFileFirstLine("/sys/devices/soc0/soc_id");
            } else {
                socId = readFileFirstLine("/sys/devices/system/soc/soc0/id");
            }
        } catch (Exception e) {
        }
        String str = TAG;
        VSlog.d(str, "getSocId socId:" + socId);
        return socId;
    }

    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:11:0x002a -> B:26:0x003a). Please submit an issue!!! */
    private String readFileFirstLine(String path) {
        File file = new File(path);
        if (!file.exists()) {
            return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        }
        String line = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        BufferedReader br = null;
        try {
            try {
                try {
                    br = new BufferedReader(new FileReader(file));
                    line = br.readLine();
                    br.close();
                    br.close();
                } catch (Throwable th) {
                    if (br != null) {
                        try {
                            br.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    throw th;
                }
            } catch (IOException e2) {
                e2.printStackTrace();
                if (br != null) {
                    br.close();
                }
            }
        } catch (IOException e3) {
            e3.printStackTrace();
        }
        return line;
    }

    private boolean isVivoCtccTestMode() {
        boolean isCtccTest = false;
        String vivoCtcc = SystemProperties.get("persist.sys.usb.ctcc.test", "0");
        isCtccTest = ("1".equals(vivoCtcc) || "2".equals(vivoCtcc)) ? true : true;
        String str = TAG;
        VSlog.d(str, "persist.sys.usb.ctcc.test is :" + vivoCtcc + " ,isCtccTest=" + isCtccTest);
        return isCtccTest;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void exitVivoCtccTestMode() {
        SystemProperties.set("persist.sys.usb.ctcc.test", "0");
        VSlog.d(TAG, "exit ctcc test mode.");
    }

    private boolean isFactoryMode() {
        if ("yes".equals(SystemProperties.get("persist.sys.factory.mode", "no"))) {
            String function = SystemProperties.get(USB_STATE_PROPERTY);
            return isDebugPortConfig(function);
        }
        return false;
    }

    private long changeCurrentFunctionsInt(String functions, long currentFunctions) {
        long currentFunctions2;
        if (isCharingFunction(functions)) {
            if (isDebugPortConfig(functions) || isCtccFunction(functions)) {
                if (currentFunctions != 32 && currentFunctions != 2 && currentFunctions != 64 && currentFunctions != 8) {
                    this.mUsbHandlerLegacy.setToCharging();
                    if (!functions.contains("rndis")) {
                        return 0L;
                    }
                    return 32L;
                }
                return currentFunctions;
            }
            return 0L;
        }
        try {
            return UsbManager.usbFunctionsFromString(functions) & (-2);
        } catch (IllegalArgumentException e) {
            if (!TextUtils.isEmpty(functions) && functions.contains("mtp")) {
                currentFunctions2 = 4;
            } else if (!TextUtils.isEmpty(functions) && functions.contains("rndis")) {
                currentFunctions2 = 32;
            } else {
                currentFunctions2 = 0;
            }
            VSlog.e(TAG, "IllegalArgumentException change mCurrentFunctions to none");
            return currentFunctions2;
        }
    }

    private String handleCheckVivoUsbFunction(String functions) {
        String str = TAG;
        VSlog.d(str, "handleCheckVivoUsbFunction functions:" + functions);
        String vivoCtcc = SystemProperties.get("persist.sys.usb.ctcc.test", "0");
        if (TextUtils.isEmpty(functions) && !"2".equals(vivoCtcc)) {
            return functions;
        }
        if (isVivoCtccTestMode()) {
            String str2 = TAG;
            VSlog.d(str2, "handleCheckVivoUsbFunction ,vivoCtcc=" + vivoCtcc + " ,mPlatform=" + this.mPlatform + " ,mSocId=" + this.mSocId + " ,mIsUsbGadgetExist=" + this.mIsUsbGadgetExist);
            if ("1".equals(vivoCtcc)) {
                if ("msm8996".equals(this.mPlatform)) {
                    functions = "rndis,serial_cdev,diag,adb";
                } else if ("msm8917".equals(this.mPlatform) || "sdm450".equals(this.mPlatform)) {
                    functions = "rndis,serial_smd,diag,adb";
                } else if ("msm8953".equals(this.mPlatform) || "msm8937".equals(this.mPlatform)) {
                    if (this.mIsUsbGadgetExist) {
                        functions = "rndis,serial_cdev,diag,adb";
                    } else {
                        functions = "rndis,serial_smd,diag,adb";
                    }
                } else if ("sdm660".equals(this.mPlatform)) {
                    functions = "rndis,serial_cdev,diag,adb";
                } else if ("sdm670".equals(this.mPlatform)) {
                    functions = "rndis,serial_cdev,diag,adb";
                } else if ("sdm845".equals(this.mPlatform) || "sdm710".equals(this.mPlatform)) {
                    functions = "rndis,serial_cdev,diag,adb";
                } else {
                    functions = "rndis,serial_cdev,diag,adb";
                }
            } else if ("2".equals(vivoCtcc)) {
                if ("msm8996".equals(this.mPlatform)) {
                    functions = QCOM_DEBUG_PORT;
                } else if ("msm8953".equals(this.mPlatform)) {
                    if (this.mIsUsbGadgetExist) {
                        functions = "diag,serial_cdev,rmnet,adb";
                    } else {
                        functions = "diag,serial_smd,rmnet_ipa,adb";
                    }
                } else if ("sdm450".equals(this.mPlatform)) {
                    functions = "diag,serial_smd,rmnet_ipa,adb";
                } else if ("sdm660".equals(this.mPlatform)) {
                    functions = "diag,serial_cdev,rmnet,adb";
                } else if ("sdm670".equals(this.mPlatform)) {
                    functions = "diag,serial_cdev,rmnet,adb";
                } else if ("sdm845".equals(this.mPlatform) || "sdm710".equals(this.mPlatform)) {
                    functions = "diag,serial_cdev,rmnet,adb";
                } else if ("msm8937".equals(this.mPlatform)) {
                    if (this.mIsUsbGadgetExist) {
                        functions = "diag,serial_cdev,rmnet,adb";
                    } else if ("313".equals(this.mSocId) || "320".equals(this.mSocId)) {
                        functions = "diag,serial_smd,rmnet_ipa,adb";
                    } else {
                        functions = "diag,serial_smd,rmnet_qti_bam,adb";
                    }
                } else if ("msm8917".equals(this.mPlatform)) {
                    if ("313".equals(this.mSocId) || "320".equals(this.mSocId)) {
                        functions = "diag,serial_smd,rmnet_ipa,adb";
                    } else {
                        functions = "diag,serial_smd,rmnet_qti_bam,adb";
                    }
                } else {
                    functions = "diag,serial_cdev,rmnet,adb";
                }
            } else if (isFactoryMode()) {
                VSlog.d(TAG, "setEnabledFunctions failed, is factory mode.");
                return "none";
            }
        }
        String str3 = TAG;
        VSlog.d(str3, "handleCheckVivoUsbFunction functions:" + functions);
        return functions;
    }

    private boolean isDebugPortConfig(String function) {
        if (TextUtils.isEmpty(function)) {
            return false;
        }
        return QCOM_DEBUG_PORT9053.equals(function) || SAMSUNG_DEBUG_PORT9815.equals(function) || QCOM_DEBUG_PORT.equals(function) || MTK_DEBUG_PORT.equals(function) || SAMSUNG_DEBUG_PORT.equals(function);
    }

    private String getVivoChargingFunctions() {
        if (isAdbEnabled()) {
            return "charging,adb";
        }
        return USB_FUNCTION_CHARGING_ONLY;
    }

    private boolean isAdbEnabled() {
        return ((AdbManagerInternal) LocalServices.getService(AdbManagerInternal.class)).isAdbEnabled((byte) 0);
    }

    private boolean isCharingFunction(String function) {
        return TextUtils.isEmpty(function) || "none".equals(function) || "charging,adb".equals(function) || USB_FUNCTION_CHARGING_ONLY.equals(function) || isDebugPortConfig(function) || isCtccFunction(function);
    }

    private boolean isCtccFunction(String function) {
        if (isVivoCtccTestMode()) {
            if (function.equals(QCOM_DEBUG_PORT) || function.equals("diag,serial_cdev,rmnet,adb") || function.equals("diag,serial_smd,rmnet_ipa,adb") || function.equals("diag,serial_smd,rmnet_qti_bam,adb") || function.equals("rndis,serial_cdev,diag,adb") || function.equals("rndis,serial_smd,diag,adb")) {
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean isUsbTransferAllowedVivo() {
        return !isPhoneLocked();
    }

    public boolean isPhoneLocked() {
        try {
            AbsVivoPhoneLockManager absVivoPhoneLockManager = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoPhoneLockManager();
            if (absVivoPhoneLockManager != null && absVivoPhoneLockManager.isPhoneLocked() == 1) {
                VSlog.d(TAG, "isPhoneLocked:true");
                return true;
            }
        } catch (Exception e) {
            VSlog.e(TAG, "Exception getPhoneLockFlag failed");
        }
        VSlog.d(TAG, "isPhoneLocked:false");
        return false;
    }

    public boolean useVivoUsbNotification(int id, int titleRes, boolean connected, boolean force, long currentFunctions, boolean sourcePower) {
        String str = TAG;
        VSlog.d(str, "useVivoUsbNotification id=" + id + " ,titleRes=" + titleRes + " ,connected=" + connected + " ,force=" + force + " ,currentFunctions=" + currentFunctions + " ,mLastId=" + this.mLastId);
        if (IS_NET_ENTRY || IS_DEVICE_RW || hasUserRestrictionOnUsb()) {
            String str2 = TAG;
            VSlog.d(str2, "useVivoUsbNotification false, IS_NET_ENTRY=" + IS_NET_ENTRY + " ,IS_DEVICE_RW=" + IS_DEVICE_RW + " ,hasUserRestrictionOnUsb()=" + hasUserRestrictionOnUsb());
            return false;
        } else if (connected) {
            if (sourcePower) {
                cancelVivoUsbNotification();
                String str3 = TAG;
                VSlog.d(str3, "useVivoUsbNotification true, sourcePower = " + sourcePower);
                return true;
            } else if (id == 27 || id == 28 || id == 47 || id == 29 || id == 30 || (id == 32 && titleRes == 17041930)) {
                if (id == 47) {
                    titleRes = 17041930;
                }
                if (id == 32 && this.mContext.getPackageManager().hasSystemFeature("android.hardware.type.automotive")) {
                    cancelVivoUsbNotification();
                    VSlog.d(TAG, "useVivoUsbNotification true, not relevant for automotive");
                    return true;
                }
                if (force) {
                    cancelVivoUsbNotification();
                }
                if (id != this.mLastId || force) {
                    showVivoUsbNotification(titleRes, currentFunctions);
                    this.mLastId = id;
                    VSlog.d(TAG, "useVivoUsbNotification true");
                    return true;
                }
                VSlog.d(TAG, "useVivoUsbNotification true, not change");
                return true;
            } else {
                cancelVivoUsbNotification();
                VSlog.d(TAG, "useVivoUsbNotification true, function not supported");
                return true;
            }
        } else {
            cancelVivoUsbNotification();
            VSlog.d(TAG, "useVivoUsbNotification true, not connected");
            return true;
        }
    }

    private void showVivoUsbNotification(int titleRes, long currentFunctions) {
        VSlog.d(TAG, "Show vivo expand usb notification begin.");
        RemoteViews remoteViews = initNotifyRemoteViewsData(currentFunctions);
        Intent usbNotificationIntent = new Intent(ACTION_USB_NOTIFICATION);
        if (currentFunctions == 4) {
            usbNotificationIntent.putExtra(INTENT_BUTTONID_TAG, 1);
            PendingIntent pi_usb_help = PendingIntent.getBroadcast(this.mContext, 1, usbNotificationIntent, Dataspace.RANGE_FULL);
            remoteViews.setOnClickPendingIntent(51183886, pi_usb_help);
        }
        if (currentFunctions == 4 || currentFunctions == 16) {
            usbNotificationIntent.putExtra(INTENT_BUTTONID_TAG, 4);
            PendingIntent usb_info_click_pi = PendingIntent.getBroadcast(this.mContext, 4, usbNotificationIntent, Dataspace.RANGE_FULL);
            remoteViews.setOnClickPendingIntent(51183881, usb_info_click_pi);
            PendingIntent usb_info_click_pi2 = PendingIntent.getBroadcast(this.mContext, 4, usbNotificationIntent, Dataspace.RANGE_FULL);
            remoteViews.setOnClickPendingIntent(51183662, usb_info_click_pi2);
        }
        usbNotificationIntent.putExtra(INTENT_BUTTONID_TAG, 3);
        PendingIntent pi_usb_manager_file = PendingIntent.getBroadcast(this.mContext, 3, usbNotificationIntent, Dataspace.RANGE_FULL);
        remoteViews.setOnClickPendingIntent(51183762, pi_usb_manager_file);
        usbNotificationIntent.putExtra(INTENT_BUTTONID_TAG, 2);
        PendingIntent pi_transfer_photo = PendingIntent.getBroadcast(this.mContext, 2, usbNotificationIntent, Dataspace.RANGE_FULL);
        remoteViews.setOnClickPendingIntent(51183763, pi_transfer_photo);
        Resources r = this.mContext.getResources();
        CharSequence message = r.getText(17041944);
        CharSequence title = r.getText(titleRes);
        Bundle bundle = new Bundle();
        bundle.putInt("vivo.summaryIconRes", 50463853);
        Notification.Builder builder = new Notification.Builder(this.mContext, SystemNotificationChannels.USB).setSmallIcon(50463852).setWhen(0L).setOngoing(true).setTicker(title).setDefaults(0).setVisibility(1).setExtras(bundle).setContentTitle(title).setContentText(message).setCustomBigContentView(remoteViews);
        Notification notification = builder.build();
        String str = TAG;
        VSlog.d(str, "showUsbNotificationVivo notifyAsUser =10011 ,notifications=" + notification);
        this.mUsbHandler.mUsbNotificationId = mStaticUsbNotificationId;
        this.mNotificationManager.notifyAsUser(null, mStaticUsbNotificationId, notification, UserHandle.ALL);
        VSlog.d(TAG, "Show vivo expand usb notification end.");
    }

    public void cancelVivoUsbNotification() {
        if (this.mLastId != 0) {
            String str = TAG;
            VSlog.d(str, "cancelVivoUsbNotification, mLastId =" + this.mLastId);
            this.mNotificationManager.cancelAsUser(null, mStaticUsbNotificationId, UserHandle.ALL);
            this.mLastId = 0;
        }
    }

    private RemoteViews initNotifyRemoteViewsData(long currentFunctions) {
        String str = TAG;
        VSlog.d(str, "initNotifyRemoteViewsData mCurrentFunctions=" + UsbManager.usbFunctionsToString(currentFunctions));
        RemoteViews remoteViews = new RemoteViews(this.mContext.getPackageName(), 50528310);
        remoteViews.setViewVisibility(51183886, 4);
        remoteViews.setTextColor(51183888, this.mContext.getResources().getColor(50856109));
        remoteViews.setTextColor(51183891, this.mContext.getResources().getColor(50856109));
        remoteViews.setTextViewText(51183882, this.mContext.getResources().getString(51249679));
        remoteViews.setImageViewResource(51183890, 50464310);
        remoteViews.setImageViewResource(51183887, 50464310);
        if (currentFunctions == 4 || currentFunctions == 16) {
            if (currentFunctions == 4) {
                remoteViews.setViewVisibility(51183886, 0);
                remoteViews.setTextColor(51183888, this.mContext.getResources().getColor(50856111));
                remoteViews.setTextColor(51183886, this.mContext.getResources().getColor(50856230));
                remoteViews.setTextColor(51183891, this.mContext.getResources().getColor(50856109));
                remoteViews.setImageViewResource(51183890, 50464310);
                remoteViews.setImageViewResource(51183887, 50464308);
            } else {
                remoteViews.setViewVisibility(51183886, 4);
                remoteViews.setTextColor(51183888, this.mContext.getResources().getColor(50856109));
                remoteViews.setTextColor(51183891, this.mContext.getResources().getColor(50856111));
                remoteViews.setImageViewResource(51183890, 50464308);
                remoteViews.setImageViewResource(51183887, 50464310);
            }
            remoteViews.setTextViewText(51183882, this.mContext.getResources().getString(51249681));
        }
        return remoteViews;
    }

    public void useVivoAdbNotification(boolean force, boolean connected, boolean adbEnabled) {
        String str = TAG;
        VSlog.d(str, "useVivoAdbNotification mUsbAdbDebugIsOpen=" + this.mUsbAdbDebugIsOpen + ", connected=" + connected + ", adbEnabled=" + adbEnabled);
        if (connected) {
            if (!this.mUsbAdbDebugIsOpen && !adbEnabled) {
                if (!this.mVivoAdbNotificationIsShown || force) {
                    if (force) {
                        cancelVivoAdbNotification();
                    }
                    showVivoAdbNotification();
                    return;
                }
                VSlog.d(TAG, "useVivoAdbNotification, not change");
                return;
            }
            cancelVivoAdbNotification();
            String str2 = TAG;
            VSlog.d(str2, "useVivoAdbNotification, mUsbAdbDebugIsOpen=" + this.mUsbAdbDebugIsOpen + ", adbEnabled=" + adbEnabled);
            return;
        }
        cancelVivoAdbNotification();
        VSlog.d(TAG, "useVivoAdbNotification, not connected");
    }

    private void showVivoAdbNotification() {
        String str = TAG;
        VSlog.d(str, "showUsbDebugTipsNotification mUsbAdbDebugTipsIsClickByUser=" + this.mUsbAdbDebugTipsIsClickByUser);
        if (this.mUsbAdbDebugTipsIsClickByUser) {
            VSlog.d(TAG, "Adb debug notification tips is close by user, so not show this notification again.");
            return;
        }
        VSlog.d(TAG, "Usb Debug Tips Notification will show.");
        RemoteViews adbDebugRemoteViews = new RemoteViews(this.mContext.getPackageName(), 50528430);
        adbDebugRemoteViews.setTextViewText(51183883, this.mContext.getResources().getText(51249675));
        adbDebugRemoteViews.setTextViewText(51183884, this.mContext.getResources().getText(51249674));
        adbDebugRemoteViews.setTextViewText(51183885, this.mContext.getResources().getText(51249676));
        Intent usbNotificationIntent = new Intent(ACTION_USB_NOTIFICATION);
        usbNotificationIntent.putExtra(INTENT_BUTTONID_TAG, 5);
        PendingIntent usb_debug_tips_pi = PendingIntent.getBroadcast(this.mContext, 5, usbNotificationIntent, Dataspace.RANGE_FULL);
        adbDebugRemoteViews.setOnClickPendingIntent(51183885, usb_debug_tips_pi);
        adbDebugRemoteViews.setTextColor(51183885, this.mContext.getResources().getColor(50856108));
        usbNotificationIntent.putExtra(INTENT_BUTTONID_TAG, 6);
        PendingIntent usb_debug_manual_pi = PendingIntent.getBroadcast(this.mContext, 6, usbNotificationIntent, Dataspace.RANGE_FULL);
        new Notification();
        Resources r = this.mContext.getResources();
        CharSequence title = r.getText(51249675);
        CharSequence message = r.getText(51249674);
        Bundle bundle = new Bundle();
        bundle.putInt("vivo.summaryIconRes", 50463160);
        Notification notifications = new Notification.Builder(this.mContext, SystemNotificationChannels.DEVELOPER).setSmallIcon(50463159).setWhen(0L).setExtras(bundle).setOngoing(true).setTicker(title).setDefaults(0).setPriority(0).setContentTitle(title).setContentText(message).setContent(adbDebugRemoteViews).setContentIntent(usb_debug_manual_pi).setVisibility(1).build();
        notifications.bigContentView = adbDebugRemoteViews;
        this.mNotificationManager.notifyAsUser(null, mStaticUsbAdbDebugTipsNotificationId, notifications, UserHandle.ALL);
        this.mVivoAdbNotificationIsShown = true;
        VSlog.d(TAG, "Usb Debug Tips Notification show end");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelVivoAdbNotification() {
        if (this.mVivoAdbNotificationIsShown) {
            this.mVivoAdbNotificationIsShown = false;
            VSlog.d(TAG, "cancelAsUser, mStaticUsbAdbDebugTipsNotificationId=110110");
            this.mNotificationManager.cancelAsUser(null, mStaticUsbAdbDebugTipsNotificationId, UserHandle.ALL);
        }
    }

    private boolean hasUserRestrictionOnUsb() {
        UserManager userManager = (UserManager) this.mContext.getSystemService("user");
        return userManager.hasUserRestriction("no_usb_file_transfer");
    }

    private static boolean isNetEntry() {
        return "yes".equals(SystemProperties.get("ro.vivo.net.entry", "no"));
    }

    private static boolean isDeviceRW() {
        boolean isRW = false;
        if (OP_ENTRY.contains("RW") || OP_ENTRY.equals("CMCC") || OP_ENTRY.equals("CTCC") || OP_ENTRY.equals("UNICOM")) {
            if (OP_ENTRY.contains("CTCC_RW")) {
                isRW = false;
            } else {
                isRW = true;
            }
        }
        if (DEBUG) {
            String str = TAG;
            VSlog.d(str, "isDeviceRW ro.vivo.op.entry=" + OP_ENTRY + " ,isRW=" + isRW);
        }
        return isRW;
    }

    /* loaded from: classes.dex */
    private class UsbBroadcastReceiver extends BroadcastReceiver {
        private UsbBroadcastReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (ActivityManager.isUserAMonkey()) {
                String str = VivoUsbHandlerImpl.TAG;
                VSlog.d(str, "Current click is monkey test intent=" + intent);
                return;
            }
            String action = intent.getAction();
            if (action.equals(VivoUsbHandlerImpl.ACTION_USB_NOTIFICATION)) {
                int buttonId = intent.getIntExtra(VivoUsbHandlerImpl.INTENT_BUTTONID_TAG, 0);
                String str2 = VivoUsbHandlerImpl.TAG;
                VSlog.d(str2, "onReceive clickId:" + buttonId + " mCurrentFunctions:" + VivoUsbHandlerImpl.this.mUsbHandler.mCurrentFunctions);
                switch (buttonId) {
                    case 1:
                        if (VivoUsbHandlerImpl.ISOverseas) {
                            VivoUsbHandlerImpl.this.sendMessage(1000, "usb_help");
                            return;
                        } else {
                            VivoUsbHandlerImpl.this.sendMessage(1000, "usbhelp");
                            return;
                        }
                    case 2:
                        if (VivoUsbHandlerImpl.this.allowUsbTransfer()) {
                            if (VivoUsbHandlerImpl.this.mUsbHandler.mCurrentFunctions == 16) {
                                String str3 = VivoUsbHandlerImpl.TAG;
                                VSlog.d(str3, "CurrentFunctions is " + VivoUsbHandlerImpl.this.mUsbHandler.mCurrentFunctions + " don't change.");
                                return;
                            }
                            VivoUsbHandlerImpl.this.sendMessage(12, (Object) 16L);
                            VivoUsbHandlerImpl.this.mUsbHandler.mCurrentFunctions = 16L;
                            VivoUsbHandlerImpl.this.mUsbHandler.updateUsbNotification(false);
                            VivoUsbHandlerImpl.this.exitVivoCtccTestMode();
                            return;
                        }
                        return;
                    case 3:
                        if (VivoUsbHandlerImpl.this.allowUsbTransfer()) {
                            if (VivoUsbHandlerImpl.this.mUsbHandler.mCurrentFunctions == 4) {
                                String str4 = VivoUsbHandlerImpl.TAG;
                                VSlog.d(str4, "mCurrentFunctions is " + VivoUsbHandlerImpl.this.mUsbHandler.mCurrentFunctions + " donot change.");
                                return;
                            }
                            if (!VivoUsbHandlerImpl.QCOM_DEBUG_PORT.equals(VivoUsbHandlerImpl.this.getSystemProperty(VivoUsbHandlerImpl.USB_STATE_PROPERTY, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK)) && !VivoUsbHandlerImpl.QCOM_DEBUG_PORT9053.equals(VivoUsbHandlerImpl.this.getSystemProperty(VivoUsbHandlerImpl.USB_STATE_PROPERTY, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK)) && !VivoUsbHandlerImpl.SAMSUNG_DEBUG_PORT.equals(VivoUsbHandlerImpl.this.getSystemProperty(VivoUsbHandlerImpl.USB_STATE_PROPERTY, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK)) && !VivoUsbHandlerImpl.SAMSUNG_DEBUG_PORT9815.equals(VivoUsbHandlerImpl.this.getSystemProperty(VivoUsbHandlerImpl.USB_STATE_PROPERTY, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK))) {
                                VivoUsbHandlerImpl.this.mUsbHandler.mCurrentFunctions = 4L;
                                VivoUsbHandlerImpl.this.mUsbHandler.updateUsbNotification(false);
                            } else {
                                VivoUsbHandlerImpl.this.mIsDebugPort = false;
                                VivoUsbHandlerImpl.this.mUsbHandler.mCurrentFunctions = 4L;
                                VivoUsbHandlerImpl.this.mUsbHandler.updateUsbNotification(false);
                            }
                            VivoUsbHandlerImpl.this.sendMessage(12, (Object) 4L);
                            VivoUsbHandlerImpl.this.exitVivoCtccTestMode();
                            return;
                        }
                        return;
                    case 4:
                        if (VivoUsbHandlerImpl.this.allowUsbTransfer()) {
                            VivoUsbHandlerImpl.this.clearLockFunctions();
                            if (VivoUsbHandlerImpl.this.mUsbHandler.mCurrentFunctions == 0) {
                                String str5 = VivoUsbHandlerImpl.TAG;
                                VSlog.d(str5, "CurrentFunctions is " + VivoUsbHandlerImpl.this.mUsbHandler.mCurrentFunctions + " don't change.");
                                return;
                            }
                            VivoUsbHandlerImpl.this.sendMessage(2, (Object) 0L);
                            VivoUsbHandlerImpl.this.mUsbHandler.updateUsbNotification(false);
                            return;
                        }
                        return;
                    case 5:
                        VivoUsbHandlerImpl.this.cancelVivoAdbNotification();
                        VivoUsbHandlerImpl.this.mUsbAdbDebugTipsIsClickByUser = true;
                        Settings.System.putString(VivoUsbHandlerImpl.this.mContext.getContentResolver(), "usb.adb.debug.noti.clicked", "yes");
                        VivoUsbHandlerImpl.this.mStatusBarManager.collapsePanels();
                        VSlog.d(VivoUsbHandlerImpl.TAG, "user click the usb debug tip button");
                        return;
                    case 6:
                        if (VivoUsbHandlerImpl.ISOverseas) {
                            VivoUsbHandlerImpl.this.sendMessage(1000, "usb_debug");
                            return;
                        } else {
                            VivoUsbHandlerImpl.this.sendMessage(1000, "usbdebug");
                            return;
                        }
                    default:
                        return;
                }
            }
        }
    }

    private SharedPreferences getPinnedSharedPrefs(Context context) {
        File prefsFile = new File(Environment.getDataSystemDeDirectory(0), USB_PREFS_XML);
        return context.createDeviceProtectedStorageContext().getSharedPreferences(prefsFile, 0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void clearLockFunctions() {
        if (this.mSettings != null) {
            this.mUsbHandler.mScreenUnlockedFunctions = 0L;
            SharedPreferences.Editor editor = this.mSettings.edit();
            editor.putString(String.format(Locale.ENGLISH, "usb-screen-unlocked-config-%d", Integer.valueOf(this.mUsbHandler.mCurrentUser)), "none");
            editor.commit();
        }
    }

    private boolean isVivoEngineerMode() {
        boolean result = false;
        String bbkem = SystemProperties.get("persist.sys.bbkem", "0");
        result = ("1".equals(bbkem) || "2".equals(bbkem)) ? true : true;
        String str = TAG;
        VSlog.d(str, "persist.sys.bbkem  bbkem=" + bbkem + " ,isVivoEM=" + result);
        return result;
    }

    private void sendMessage(int what, boolean arg) {
        this.mUsbHandler.removeMessages(what);
        Message m = Message.obtain((Handler) this.mUsbHandler, what);
        m.arg1 = arg ? 1 : 0;
        this.mUsbHandler.sendMessage(m);
    }

    public void sendMessage(int what, Object arg) {
        this.mUsbHandler.removeMessages(what);
        Message m = Message.obtain((Handler) this.mUsbHandler, what);
        m.obj = arg;
        this.mUsbHandler.sendMessage(m);
    }

    private void sendMessage(int what, Object arg, boolean arg1) {
        this.mUsbHandler.removeMessages(what);
        Message m = Message.obtain((Handler) this.mUsbHandler, what);
        m.obj = arg;
        m.arg1 = arg1 ? 1 : 0;
        this.mUsbHandler.sendMessage(m);
    }

    private void sendMessage(int what, boolean arg1, boolean arg2) {
        this.mUsbHandler.removeMessages(what);
        Message m = Message.obtain((Handler) this.mUsbHandler, what);
        m.arg1 = arg1 ? 1 : 0;
        m.arg2 = arg2 ? 1 : 0;
        this.mUsbHandler.sendMessage(m);
    }

    private void sendMessageDelayed(int what, boolean arg, long delayMillis) {
        this.mUsbHandler.removeMessages(what);
        Message m = Message.obtain((Handler) this.mUsbHandler, what);
        m.arg1 = arg ? 1 : 0;
        this.mUsbHandler.sendMessageDelayed(m, delayMillis);
    }

    protected void setSystemProperty(String prop, String val) {
        SystemProperties.set(prop, val);
    }

    protected String getSystemProperty(String prop, String def) {
        return SystemProperties.get(prop, def);
    }

    protected void putGlobalSettings(ContentResolver contentResolver, String setting, int val) {
        Settings.Global.putInt(contentResolver, setting, val);
    }

    private void printMessage(int msg) {
        String messageStr = "MSG_UNKNOWN";
        boolean shouldPrint = true;
        if (msg != 1000) {
            switch (msg) {
                case 0:
                    messageStr = "MSG_UPDATE_STATE";
                    break;
                case 1:
                    messageStr = "MSG_ENABLE_ADB";
                    break;
                case 2:
                    messageStr = "MSG_SET_CURRENT_FUNCTIONS";
                    break;
                case 3:
                    messageStr = "MSG_SYSTEM_READY";
                    break;
                case 4:
                    messageStr = "MSG_BOOT_COMPLETED";
                    break;
                case 5:
                    messageStr = "MSG_USER_SWITCHED";
                    break;
                case 6:
                    messageStr = "MSG_UPDATE_USER_RESTRICTIONS";
                    break;
                case 7:
                    messageStr = "MSG_UPDATE_PORT_STATE";
                    break;
                case 8:
                    messageStr = "MSG_ACCESSORY_MODE_ENTER_TIMEOUT";
                    break;
                case 9:
                    messageStr = "MSG_UPDATE_CHARGING_STATE";
                    shouldPrint = false;
                    break;
                case 10:
                    messageStr = "MSG_UPDATE_HOST_STATE";
                    break;
                case 11:
                    messageStr = "MSG_LOCALE_CHANGED";
                    break;
                case 12:
                    messageStr = "MSG_SET_SCREEN_UNLOCKED_FUNCTIONS";
                    break;
                case 13:
                    messageStr = "MSG_UPDATE_SCREEN_LOCK";
                    break;
                case 14:
                    messageStr = "MSG_SET_CHARGING_FUNCTIONS";
                    break;
                case 15:
                    messageStr = "MSG_SET_FUNCTIONS_TIMEOUT";
                    break;
                case 16:
                    messageStr = "MSG_GET_CURRENT_USB_FUNCTIONS";
                    break;
                case 17:
                    messageStr = "MSG_FUNCTION_SWITCH_TIMEOUT";
                    break;
            }
        } else {
            messageStr = "MSG_SEND_USB_HELP_BROADCAST";
        }
        if (shouldPrint) {
            String str = TAG;
            VSlog.d(str, "handleMessage " + messageStr + " ,message.what=" + msg);
        }
    }

    public boolean initCurrentFunctions(IVivoUsbHandlerLegacy usbHandlerLegacy, boolean currentFunctionsApplied, long currentFunctions) {
        this.mUsbHandlerLegacy = usbHandlerLegacy;
        String persistFunc = getSystemProperty(USB_PERSISTENT_CONFIG_PROPERTY, "none");
        String configFunc = getSystemProperty("sys.usb.config", "none");
        String stateFunc = getSystemProperty(USB_STATE_PROPERTY, "none");
        String str = TAG;
        VSlog.d(str, "initCurrentFunctions currentFunctions=" + currentFunctions + " ,currentFunctionsApplied=" + currentFunctionsApplied);
        UsbDeviceManager.UsbHandler usbHandler = this.mUsbHandler;
        usbHandler.mCurrentFunctions = changeCurrentFunctionsInt(stateFunc, usbHandler.mCurrentFunctions);
        boolean applied = configFunc.equals(stateFunc);
        if (isDebugPortConfig(stateFunc)) {
            this.mIsDebugPort = true;
        }
        String str2 = TAG;
        VSlog.d(str2, "initCurrentFunctions currentFunctions=" + currentFunctions + " ,currentFunctionsApplied=" + applied);
        String str3 = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("persist.sys.usb.config=");
        sb.append(persistFunc);
        VSlog.d(str3, sb.toString());
        String str4 = TAG;
        VSlog.d(str4, "sys.usb.config=" + configFunc);
        String str5 = TAG;
        VSlog.d(str5, "sys.usb.state=" + stateFunc);
        return applied;
    }

    public String applyAdbFunctionVivo(String functions) {
        if ("yes".equals(SystemProperties.get("persist.sys.factory.mode", "no")) || "yes".equals(SystemProperties.get("persist.vivo.cts.adb.enable", "no"))) {
            if (USB_FUNCTION_CHARGING_ONLY.equals(functions) || "mtp".equals(functions) || "ptp".equals(functions)) {
                functions = functions + ",vusbd";
            }
            if (functions.contains("adb")) {
                functions = functions.replace("adb", "vusbd");
            }
        }
        if (isVivoCustomized()) {
            functions = this.mVivoUsbCustomization.applyAdbFunctionVivo(functions);
        }
        VSlog.d(TAG, "applyAdbFunctionVivo, functions:=" + functions);
        return functions;
    }

    public String trySetEnabledFunctionsVivo(long usbFunctions, boolean forceRestart) {
        String functions;
        if (usbFunctions != 0 && !isPhoneLocked()) {
            functions = UsbManager.usbFunctionsToString(usbFunctions);
        } else {
            functions = getVivoChargingFunctions();
            String str = TAG;
            VSlog.d(str, "getVivoChargingFunctions=" + functions);
        }
        String functions2 = handleCheckVivoUsbFunction(functions);
        UsbDeviceManager.UsbHandler usbHandler = this.mUsbHandler;
        usbHandler.mCurrentFunctions = changeCurrentFunctionsInt(functions2, usbHandler.mCurrentFunctions);
        return functions2;
    }

    public boolean stopSetCurrentFunctions(long usbFunctions, boolean forceRestart, String functions) {
        String persistFunctions = getSystemProperty(USB_STATE_PROPERTY, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        if (isDebugPortConfig(persistFunctions) && isFactoryMode() && this.mIsDebugPort) {
            String str = TAG;
            VSlog.w(str, "trySetEnabledFunctions return,  ,mIsDebugPort=" + this.mIsDebugPort);
            return true;
        }
        return false;
    }

    public void handleMessage(Message msg) {
        printMessage(msg.what);
        if (msg.what == 1000) {
            if (ISOverseas) {
                Intent usbIntent = new Intent(USB_NOTIFICATION_HELP_MANUAL_ACTION);
                usbIntent.putExtra("main_title", "更多应用");
                usbIntent.putExtra("sub_title", "USB连接说明");
                String usbStr = (String) msg.obj;
                usbIntent.putExtra("app_name", usbStr);
                usbIntent.setComponent(new ComponentName("com.android.BBKPhoneInstructions", "com.android.BBKPhoneInstructions.SkipReceiver"));
                String str = TAG;
                VSlog.d(str, "send bc to show usb_debug_manual, usb click usbStr=" + usbStr);
                this.mContext.sendBroadcast(usbIntent);
            } else {
                Intent usbIntent2 = new Intent(USB_NOTIFICATION_HELP_MANUAL_NEW_ACTION);
                String usbStr2 = (String) msg.obj;
                if ("usbdebug".equals(usbStr2)) {
                    usbIntent2.putExtra("com.vivo.space.ikey.UNITED_ENTER_EXTRA_CATEGORY", 1);
                } else {
                    usbIntent2.putExtra("com.vivo.space.ikey.UNITED_ENTER_EXTRA_CATEGORY", 0);
                }
                usbIntent2.putExtra("com.vivo.space.ikey.UNITED_ENTER_EXTRA_STR", usbStr2);
                usbIntent2.setFlags(874512384);
                String str2 = TAG;
                VSlog.d(str2, "send bc to show usb_debug_manual, usb click usbStr=" + usbStr2);
                try {
                    this.mContext.startActivity(usbIntent2);
                } catch (Exception e) {
                    VSlog.d(TAG, "can not start activity usbdebug USB_NOTIFICATION_HELP_MANUAL_NEW_ACTION");
                }
            }
            this.mStatusBarManager.collapsePanels();
        }
    }

    public boolean handleMessageUpdateState(boolean connected, boolean configured, boolean bootCompleted, boolean screenLocked, boolean isAdbEnabled, long screenUnlockedFunctions, long currentFunctions, boolean currentFunctionsApplied) {
        boolean applied = currentFunctionsApplied;
        String str = TAG;
        VSlog.d(str, "MSG_UPDATE_STATE mConnected=" + connected + " ,mConfigured=" + configured + " ,mBootCompleted=" + bootCompleted + " ,mScreenLocked=" + screenLocked + " ,mScreenUnlockedFunctions=" + screenUnlockedFunctions + " ,isAdbEnabled()=" + isAdbEnabled());
        String str2 = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("MSG_UPDATE_STATE mCurrentFunctions=");
        sb.append(currentFunctions);
        sb.append(" (");
        sb.append(UsbManager.usbFunctionsToString(currentFunctions));
        sb.append(")");
        VSlog.d(str2, sb.toString());
        if (connected && configured) {
            applied = initCurrentFunctions(this.mUsbHandlerLegacy, applied, currentFunctions);
            if (bootCompleted && MTK_DEBUG_PORT.equals(getSystemProperty(USB_STATE_PROPERTY, "none"))) {
                this.mUsbHandler.mCurrentFunctions = 4L;
            }
        }
        return applied;
    }

    public void finishBoot(boolean bootCompleted, boolean currentUsbFunctionsReceived, boolean systemReady, long currentFunctions, boolean screenLocked, boolean isAdbEnabled, long screenUnlockedFunctions) {
        if (systemReady) {
            this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        }
        String str = TAG;
        VSlog.d(str, "finishBoot mBootCompleted=" + bootCompleted + " ,mCurrentUsbFunctionsReceived=" + currentUsbFunctionsReceived + " ,mSystemReady=" + systemReady);
        if (bootCompleted && currentUsbFunctionsReceived && systemReady) {
            String str2 = TAG;
            VSlog.d(str2, "finishBoot mCurrentFunctions=" + currentFunctions + " ,mScreenLocked=" + screenLocked + " ,isAdbEnabled()=" + isAdbEnabled + " ,mScreenUnlockedFunctions=" + screenUnlockedFunctions);
        }
    }

    public Notification.Builder generateUsbNotificationBuilder(String channel, CharSequence title, CharSequence message, PendingIntent pi) {
        Bundle bundle = new Bundle();
        bundle.putInt("vivo.summaryIconRes", 50463853);
        Notification.Builder builder = new Notification.Builder(this.mContext, channel).setSmallIcon(50463852).setExtras(bundle).setWhen(0L).setOngoing(true).setTicker(title).setDefaults(0).setColor(this.mContext.getColor(17170460)).setContentTitle(title).setContentText(message).setContentIntent(pi).setVisibility(1);
        return builder;
    }

    public Intent generateNotificationIntent() {
        if ("yes".equals(SystemProperties.get("ro.vivo.product.overseas", "no"))) {
            Intent intent = Intent.makeRestartActivityTask(new ComponentName(VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS, "com.android.settings.Settings$UsbDetailsActivity"));
            return intent;
        }
        Intent intent2 = Intent.makeRestartActivityTask(new ComponentName(VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS, "com.android.settings.deviceinfo.UsbSettings"));
        return intent2;
    }

    public long getChargingFunctions() {
        return 0L;
    }

    public void setDebugPort(boolean debugPort) {
        this.mIsDebugPort = debugPort;
    }

    public boolean notifyAccessoryDetached() {
        if (MultiDisplayManager.SUPPORT_CAR_NETWORKING && this.mUsbHandler.mUsbDeviceManager.getCurrentSettings() != null && this.mUsbHandler.mUsbDeviceManager.getCurrentSettings().accessoryMatches4CarNetworking(this.mContext, this.mUsbHandler.mCurrentAccessory)) {
            VSlog.d("VivoAccessory", "accessoryDetached : " + this.mUsbHandler.mCurrentAccessory + " skip broadcast.");
            return true;
        }
        return false;
    }

    public void dummy() {
    }

    public boolean allowUsbTransfer() {
        if (isVivoCustomized()) {
            boolean result = this.mVivoUsbCustomization.allowUsbTransfer();
            String str = TAG;
            VSlog.w(str, "It is VivoCustomized, allowUsbTransfer:" + result);
            return result;
        }
        return true;
    }

    public boolean allowSetRndis(long functions) {
        if (isVivoCustomized()) {
            boolean result = this.mVivoUsbCustomization.allowSetRndis(functions);
            String str = TAG;
            VSlog.w(str, "It is VivoCustomized, allowSetRndis:" + result);
            return result;
        }
        return true;
    }

    String getVivoUsbCusFunction(String functions) {
        return this.mVivoUsbCustomization.getVivoUsbCusFunction(functions);
    }

    public boolean isVivoCustomized() {
        return this.mVivoUsbCustomization.isVivoCustomized();
    }
}