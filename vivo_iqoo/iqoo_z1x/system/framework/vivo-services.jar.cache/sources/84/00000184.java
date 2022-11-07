package com.android.server.devicepolicy;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.AlarmManager;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.INotificationManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.WallpaperManager;
import android.app.admin.DevicePolicyEventLogger;
import android.app.admin.IVivoCallStateCallback;
import android.app.admin.IVivoPolicyManagerCallback;
import android.app.admin.VivoPolicyManagerInternal;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.SuspendDialogInfo;
import android.content.pm.VersionedPackage;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.display.ColorDisplayManager;
import android.hardware.graphics.common.V1_0.Dataspace;
import android.net.ConnectivityManager;
import android.net.IConnectivityManager;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.NetworkStatsHistory;
import android.net.NetworkTemplate;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.nfc.INfcAdapter;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.FtBuild;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteCallback;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StatFs;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManagerInternal;
import android.os.storage.IStorageManager;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.permission.PermissionControllerManager;
import android.provider.Settings;
import android.security.KeyStore;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.data.ApnSetting;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.JsonReader;
import android.util.Log;
import android.util.SparseArray;
import android.util.Xml;
import android.view.Display;
import android.view.SurfaceControl;
import android.view.WindowManager;
import android.view.accessibility.IAccessibilityManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import com.android.internal.app.IAppOpsCallback;
import com.android.internal.app.IAppOpsService;
import com.android.internal.net.VpnProfile;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.SmsApplication;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.JournaledFile;
import com.android.internal.util.Preconditions;
import com.android.server.LocalServices;
import com.android.server.am.firewall.VivoAppIsolationController;
import com.android.server.am.firewall.VivoFirewall;
import com.android.server.audio.VivoAudioServiceImpl;
import com.android.server.devicepolicy.utils.VcodeUtils;
import com.android.server.devicepolicy.utils.VivoAdminInfo;
import com.android.server.devicepolicy.utils.VivoEmmInfo;
import com.android.server.devicepolicy.utils.VivoUtils;
import com.android.server.display.VivoDisplayPowerControllerImpl;
import com.android.server.pm.PackageManagerService;
import com.android.server.policy.key.VivoOTGKeyHandler;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.android.server.wm.VivoMultiWindowConfig;
import com.android.server.wm.VivoWmsImpl;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import com.vivo.face.common.state.FaceUIState;
import com.vivo.services.rms.sdk.Consts;
import com.vivo.services.security.client.VivoPermissionManager;
import com.vivo.services.security.client.VivoPermissionType;
import com.vivo.services.security.server.VivoPermissionUtils;
import com.vivo.services.superresolution.Constant;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;
import vendor.pixelworks.hardware.display.V1_0.KernelConfig;

/* loaded from: classes.dex */
public class VivoCustomDpmsImpl implements IVivoCustomDpms {
    public static final String ACTION_EMM_VCODE = "vivo.app.action.VIVO_EMM_VCODE";
    private static final int ADD_PACKAGE_TO_PRELOAD = 1;
    public static List<String> ALLOW_DISABLED_COMPONENT_SYSTEM_APP = null;
    public static List<String> ALLOW_DISABLED_LAUNCH_SYSTEM_APP = null;
    public static List<String> ALLOW_DISABLED_SYSTEM_APP = null;
    private static final String ATTR_VALUE = "value";
    private static final String AUTHORITY = "com.vivo.browser";
    private static final Uri AUTHORITY_URI;
    private static final Uri CONTENT_URI;
    private static final String CUSTOM_SHORT_NAME_DEFAULT = "common";
    private static final String CUSTOM_TOOL_NAME = "com.vivo.customtool";
    private static final int CUSTOM_TYPE_DEBUG = 1;
    private static final int CUSTOM_TYPE_DEFAULT = 0;
    private static final boolean DEBUG = true;
    public static final long INTERVAL_VCODE = 21600000;
    private static final String PATH_OTG = "/sys/kernel/audio-max20328/unuseirq";
    private static final String PERMISSION_MANAGER_CURRENT_STATE = "currentstate";
    private static final String PERMISSION_MANAGER_ISFORCE = "is_force";
    private static final String PERMISSION_MANAGER_PASSWORD_CRYPT_KEY = "iqoo11-14";
    private static final String PERMISSION_MANAGER_PKGNAME = "pkgname";
    private static final int PERMISSION_MANAGER_STATE_ALLOWED = 0;
    private static final int PERMISSION_MANAGER_STATE_FORBIDDEN = 1;
    private static final String SDK_VERSION = "V2.0";
    private static final String SPEED_UP_ACTION_NAME = "com.android.KILL_BG_APPS_SERVICE";
    private static final String SPEED_UP_PACKAGE_NAME = "com.vivo.upslide";
    private static final String TAG_ADMIN_PASSWORD_TOKEN = "admin-password-token";
    private static final boolean TEST_FLAG = true;
    private static final int TRAFF_DIRECT_BOTH = 0;
    private static final int TRAFF_DIRECT_DOWNLOAD = 1;
    private static final int TRAFF_DIRECT_UPLOAD = 2;
    private static final int TRAFF_MODE_BOTH = 0;
    private static final int TRAFF_MODE_MOBILE = 1;
    private static final int TRAFF_MODE_WLAN = 2;
    private static final String VIVO_EMM_NAME = "com.vivo.emmTest";
    private static final String VIVO_LOG_TAG = "VDPMS";
    private static final int VIVO_STATUS_BAR_DISABLE2_MASK = 1;
    private static final int VIVO_STATUS_BAR_DISABLE_MASK = 34013184;
    public static final SparseArray<VivoPolicyData> mVivoUserData;
    private static final String[] sEmergencyNumsCTCC;
    private final int UPDATE_DEFAULT_APP;
    private final int UPDATE_DEFAULT_APP_FINISHED;
    private final int UPDATE_PERMISSION_OP;
    private final int UPDATE_PERMISSION_OP_FINISHED;
    private final String VIVO_CUSTOM_VERSION;
    private ActivityManager mActivityManager;
    private AlarmManager mAlarmManager;
    private IAppOpsCallback mAppOpsCallback;
    private AppOpsManager mAppOpsManager;
    public List<String> mAutoStartPkgs;
    private RemoteCallbackList<IVivoCallStateCallback> mCallStateCallbacks;
    private RemoteCallbackList<IVivoPolicyManagerCallback> mCallbacks;
    private ContentResolver mContentResolver;
    private Context mContext;
    private int mCustomMultiUser;
    private String mCustomShortName;
    private int mCustomType;
    public ComponentName mDefaultBrowser;
    public ComponentName mDefaultLauncher;
    private DeviceInfo mDeviceInfo;
    public ComponentName mDeviceOwner;
    public List<String> mDisableListPkgs;
    private String mEmmFromCota;
    private String mEmmShortName;
    final Handler mHandler;
    private IAccessibilityManager mIAccessibilityManager;
    private IPackageManager mIPackageManager;
    private ITelephony mITelephony;
    public List<String> mInstallWlistPkgs;
    private boolean mIsBootCompleted;
    private boolean mIsDoingUnmount;
    private boolean mIsDpmDefaultAppSet;
    private boolean mIsDpmPermissionOpSet;
    private boolean mIsEmmAPI;
    private boolean mIsOverseas;
    private boolean mIsRemoving;
    private int mLastCallState;
    private LocalService mLocalService;
    private NotificationManager mNotificationManager;
    public List<String> mPermissionListPkgs;
    public List<String> mPersistentListPkgs;
    private String mRoEmmShortName;
    private String mRoShortName;
    private DevicePolicyManagerService mService;
    private INetworkStatsService mStatsService;
    private IBinder mStatusbarToken;
    StorageEventListener mStorageListener;
    private TelephonyManager mTelephonyManager;
    public List<String> mUninstallBlistPkgs;
    private UserManagerInternal mUserManagerInternal;
    final BroadcastReceiver mVcodeReceiver;
    final BroadcastReceiver mVivoCustomMonitor;
    private VivoPermissionManager mVivoPermissionManager;
    private VivoPolicyManagerInternal.VivoPolicyListener mVivoPolicyListener;
    private boolean needAutoStart;
    private static final Uri BG_START_UP_URI = Uri.parse("content://com.vivo.permissionmanager.provider.permission/bg_start_up_apps");
    private static final Uri CONTROL_LOCK_SCRENN_URI = Uri.parse("content://com.vivo.permissionmanager.provider.permission/control_locked_screen_action");
    private static final Uri BG_ACTIVITY_URI = Uri.parse("content://com.vivo.permissionmanager.provider.permission/start_bg_activity");
    private static final Uri READ_INSTALLED_URI = Uri.parse("content://com.vivo.permissionmanager.provider.permission/read_installed_apps");
    private static final boolean isSupportTF = "1".equals(SystemProperties.get("vold.decrypt.sd_card_support", "0"));
    private final List<String> MASK_EXEMPT_APP = new ArrayList(Arrays.asList("com.android.dialer", "com.android.contacts", "com.android.mms", "com.android.incallui", "com.vivo.sim.contacts", "com.android.providers.telephony", "com.android.server.telecom"));
    private final HashMap<String, ComponentName> SETTINGS_MENU_COMPONENT_NAMES = new HashMap<String, ComponentName>() { // from class: com.android.server.devicepolicy.VivoCustomDpmsImpl.1
        {
            put("suggestion", new ComponentName(VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS, "com.android.settings.Settings$SuggestionSettings"));
            put("vivo_account", new ComponentName(VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS, "com.android.settings.Settings$BbkAccountSettings"));
            put("device_info", new ComponentName(VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS, "com.android.settings.Settings$MyDeviceInfoSettingsActivity"));
            put("airplane_mode", new ComponentName(VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS, "com.android.settings.Settings$AirplaneModeSettings"));
            put("wifi", new ComponentName(VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS, "com.android.wifisettings.Settings$WifiSettingsActivity"));
            put("mobile_network", new ComponentName("com.android.phone", "com.android.phone.MobileNetworkSettings"));
            put("extra_network_connection", new ComponentName(VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS, "com.android.settings.Settings$ExtraNetworkConnectionActivity"));
            put("notifications", new ComponentName(FaceUIState.PKG_SYSTEMUI, "com.vivo.systemui.statusbar.notification.settings.StatusbarSettingActivity"));
            put("display_brightness", new ComponentName(VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS, "com.android.settings.Settings$DisplaySettingsActivity"));
            put("theme", new ComponentName(VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS, "com.android.settings.Settings$VivoThemeSettingsActivity"));
            put("dynamic_effect", new ComponentName(VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS, "com.android.settings.Settings$DynamicEffectSettingsActivity"));
            put("sounds_vibration", new ComponentName(VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS, "com.android.settings.Settings$SoundSettingsActivity"));
            put("jovi", new ComponentName(VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS, "com.android.settings.Settings$JoviSettingsActivity"));
            put("navigation", new ComponentName(VivoCustomDpmsImpl.SPEED_UP_PACKAGE_NAME, "com.vivo.upslide.navigation.settings.settings.NavigationSettingsActivity"));
            put("fingerpint_face_password", new ComponentName(VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS, "com.android.settings.Settings$FingerpintAndFaceSettingsActivity"));
            put("game_cube", new ComponentName("com.vivo.gamecube", "com.vivo.gamecube.GameCubeMainActivity"));
            put("shortcuts_accessibility", new ComponentName(VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS, "com.android.settings.Settings$ShortcutsAccessibilityActivity"));
            put("system_management", new ComponentName(VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS, "com.android.settings.Settings$SystemManagementActivity"));
            put("security_privacy", new ComponentName(VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS, "com.android.settings.Settings$SecurityAndPrivacySettingsActivity"));
            put("screen_time", new ComponentName(VivoAppIsolationController.NOTIFY_IQOO_SECURE_PACKAGE, "com.iqoo.secure.timemanager.view.TimeManagerForSettingActivity"));
            put("storage", new ComponentName(VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS, "com.android.settings.Settings$StorageSettingsActivity"));
            put("battery", new ComponentName("com.iqoo.powersaving", "com.iqoo.powersaving.PowerSavingManagerActivity"));
            put("application_permission", new ComponentName(VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS, "com.android.settings.Settings$ApplicationPermissionActivity"));
            put("google", new ComponentName("com.google.android.gms", "com.google.android.gms.app.settings.GoogleSettingsLink"));
            put("account_sync", new ComponentName(VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS, "com.android.settings.Settings$AccountDashboardActivity"));
        }
    };
    private IConnectivityManager mConnectivityManager = null;
    private ConnectivityManager mCM = null;
    private IStatusBarService mStatusBarService = null;
    private IStorageManager iStorageManager = null;
    private StorageManager mStorageManager = null;
    private int mVgcCustomType = SystemProperties.getInt("ro.vgc.cust.id", 0);
    private String mVgcShortName = SystemProperties.get("ro.vgc.cust.name", CUSTOM_SHORT_NAME_DEFAULT);
    private int mRoCustomType = SystemProperties.getInt("ro.build.gn.support", 0);

    static {
        Uri parse = Uri.parse("content://com.vivo.browser");
        AUTHORITY_URI = parse;
        CONTENT_URI = Uri.withAppendedPath(parse, "history");
        sEmergencyNumsCTCC = new String[]{"110", "112", "911", "120", "119", "999", "122", "000", "08", "118"};
        ALLOW_DISABLED_SYSTEM_APP = new ArrayList(Arrays.asList("com.android.bbkmusic", "com.bbk.calendar", "com.android.BBKClock", "com.android.bbksoundrecorder", "com.vivo.Tips", "com.android.bbkcalculator", "com.android.notes", "com.vivo.compass", "com.vivo.FMRadio", "com.vivo.email", "com.android.VideoPlayer", "com.vivo.weather", "com.bbk.cloud", "com.vivo.game", "com.vivo.space", "com.chaozh.iReader", "com.vivo.childrenmode", "com.bbk.VoiceAssistant", "com.vivo.agent", "com.vivo.magazine", "com.vivo.doubleinstance", "com.vivo.floatingball", "com.android.BBKCrontab", "com.vivo.hiboard", VivoMultiWindowConfig.SMART_MULTIWINDOW_NAME, "com.vivo.easyshare", "com.android.bbk.lockscreen3", "com.bbk.iqoo.feedback", "com.vivo.gamecube", "com.vivo.hybrid", "com.vivo.assistant", "com.vivo.globalsearch", "com.vivo.simplelauncher", "com.vivo.notes", "com.vivo.translator", "com.vivo.wallet"));
        ALLOW_DISABLED_COMPONENT_SYSTEM_APP = new ArrayList(Arrays.asList("com.android.bbkmusic", "com.bbk.calendar", "com.android.BBKClock", "com.android.bbksoundrecorder", "com.vivo.Tips", "com.android.bbkcalculator", "com.android.notes", "com.vivo.compass", "com.vivo.FMRadio", "com.vivo.email", "com.android.VideoPlayer", "com.vivo.weather", "com.bbk.cloud", "com.vivo.game", "com.vivo.space", "com.chaozh.iReader", "com.vivo.childrenmode", "com.bbk.VoiceAssistant", "com.vivo.agent", "com.vivo.magazine", "com.vivo.doubleinstance", "com.vivo.floatingball", "com.android.BBKCrontab", "com.vivo.hiboard", VivoMultiWindowConfig.SMART_MULTIWINDOW_NAME, "com.vivo.easyshare", "com.android.bbk.lockscreen3", "com.bbk.iqoo.feedback", VivoAppIsolationController.NOTIFY_IQOO_SECURE_PACKAGE, VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS, "com.vivo.gamecube", "com.vivo.hybrid", "com.vivo.globalsearch", "com.vivo.simplelauncher", "com.vivo.notes", "com.vivo.translator", "com.vivo.wallet"));
        ALLOW_DISABLED_LAUNCH_SYSTEM_APP = new ArrayList(Arrays.asList("com.bbk.appstore", "com.android.bbkmusic", "com.bbk.calendar", "com.android.BBKClock", "com.android.bbksoundrecorder", "com.vivo.Tips", "com.android.bbkcalculator", "com.android.notes", "com.vivo.compass", "com.vivo.FMRadio", AUTHORITY, "com.vivo.email", "com.android.VideoPlayer", "com.vivo.weather", "com.bbk.cloud", "com.vivo.game", "com.vivo.space", "com.chaozh.iReader", "com.vivo.childrenmode", "com.bbk.VoiceAssistant", "com.vivo.agent", "com.vivo.magazine", "com.vivo.floatingball", VivoAppIsolationController.NOTIFY_IQOO_SECURE_PACKAGE, VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS, "com.vivo.easyshare", "com.android.bbk.lockscreen3", "com.bbk.iqoo.feedback", "com.android.filemanager", "com.android.dialer", "com.android.mms", "com.android.contacts", Constant.APP_GALLERY, "com.bbk.theme", "com.android.camera", "com.vivo.gamecube", "com.vivo.hybrid", "com.vivo.globalsearch", "com.vivo.simplelauncher", "com.vivo.notes", "com.vivo.translator", "com.vivo.wallet"));
        mVivoUserData = new SparseArray<>();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class VivoPolicyData {
        int mUserHandle;
        VivoActiveAdmin mVivoActiveAdmin = null;
        final ArrayMap<String, VivoEmmInfo> mEmmInfoMap = new ArrayMap<>();
        List<String> mEmmBlackList = new ArrayList();
        List<String> mEmmDisabledList = new ArrayList();
        List<String> mPowerExceptionInfolist = new ArrayList();
        List<String> mAlarmExceptionInfolist = new ArrayList();
        List<String> mLocationExceptionInfolist = new ArrayList();
        List<String> mCrashExceptionInfolist = new ArrayList();
        List<String> mWakeLockExceptionInfolist = new ArrayList();
        long mPasswordToken = 0;

        public VivoPolicyData(int userHandle) {
            this.mUserHandle = userHandle;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class VivoActiveAdmin {
        private static final String TAG_ADMIN_ACCESSIBILITY_LIST = "admin-accessibility-list";
        private static final String TAG_ADMIN_DEVICE_OWNER_POLICY = "admin-device-owner-policy";
        private static final String TAG_ADMIN_PROFILE_OWNER_POLICY = "admin-profile-owner-policy";
        private static final String TAG_AIRPLANE_POLICY = "tag-airplane-policy";
        private static final String TAG_APN_DISABLE_RECOVERY_LIST = "apn-disable-recovery-list";
        private static final String TAG_APP_ALARM_W_LIST = "app-alarm-w-list";
        private static final String TAG_APP_COMPONENT_DISABLE_LIST = "app-component-disabled-list";
        private static final String TAG_APP_DISABLED_APP_LIST = "app-disabled-app-list";
        private static final String TAG_APP_DISALLOWED_CLEAR_DATA_LIST = "app-disabled-clear-data-list";
        private static final String TAG_APP_DISALLOWED_LAUNCH_LIST = "app-disallowed-launch-list";
        private static final String TAG_APP_DISALLOWED_LAUNCH_POLICY = "app-disallowed-launch-policy";
        private static final String TAG_APP_INSTALL_BW_POLICY = "app-install-bw-policy";
        private static final String TAG_APP_INSTALL_B_LIST = "app-install-b-list";
        private static final String TAG_APP_INSTALL_W_LIST = "app-install-w-list";
        private static final String TAG_APP_METERED_DATA_BW_POLICY = "app-metered-data-bw-policy";
        private static final String TAG_APP_METERED_DATA_B_LIST = "app-metered-data-b-list";
        private static final String TAG_APP_METERED_DATA_W_LIST = "app-metered-data-w-list";
        private static final String TAG_APP_NOTIFICATION_LIST = "app-notification-list";
        private static final String TAG_APP_PERMISSION_W_LIST = "app-permission-w-list";
        private static final String TAG_APP_PERSISTENT_APP_LIST = "app-persistent-app-list";
        private static final String TAG_APP_TRUSTED_SOURCE_LIST = "app-trusted-source-list";
        private static final String TAG_APP_TRUSTED_SOURCE_POLICY = "app-trusted-source-policy";
        private static final String TAG_APP_UNINSTALL_BW_POLICY = "app-uninstall-bw-policy";
        private static final String TAG_APP_UNINSTALL_B_LIST = "app-uninstall-b-list";
        private static final String TAG_APP_UNINSTALL_W_LIST = "app-uninstall-w-list";
        private static final String TAG_APP_WLAN_DATA_BW_POLICY = "app-wlan-data-bw-policy";
        private static final String TAG_APP_WLAN_DATA_B_LIST = "app-wlan-data-b-list";
        private static final String TAG_APP_WLAN_DATA_W_LIST = "app-wlan-data-w-list";
        private static final String TAG_BLUETOOTH_AP_POLICY = "bluetooth-ap-policy";
        private static final String TAG_BLUETOOTH_BW_POLICY = "bluetooth-bw-policy";
        private static final String TAG_BLUETOOTH_B_LIST = "bluetooth-b-list";
        private static final String TAG_BLUETOOTH_POLICY = "bluetooth-policy";
        private static final String TAG_BLUETOOTH_W_LIST = "bluetooth-w-list";
        private static final String TAG_CALL_FORWARDING_POLICY = "call-forwarding-policy";
        private static final String TAG_CALL_IN_COUNT_POLICY = "tag-call-in-count-policy";
        private static final String TAG_CALL_OUT_COUNT_POLICY = "tag-call-out-count-policy";
        private static final String TAG_DEFAULT_APP_SET_POLICY = "default-app-set-policy";
        private static final String TAG_FLASH_POLICY = "flashlight-policy";
        private static final String TAG_LIST_ITEM = "item";
        private static final String TAG_LOCATION_POLICY = "location-policy";
        private static final String TAG_MIC_POLICY = "mic-policy";
        private static final String TAG_NETWORK_APN_POLICY = "network-apn-policy";
        private static final String TAG_NETWORK_DOMAIN_BW_POLICY = "network-domain-bw-policy";
        private static final String TAG_NETWORK_DOMAIN_B_LIST = "network-domain-b-list";
        private static final String TAG_NETWORK_DOMAIN_W_LIST = "network-domain-w-list";
        private static final String TAG_NETWORK_IP_ADDR_BW_POLICY = "network-ip-addr-bw-policy";
        private static final String TAG_NETWORK_IP_ADDR_B_LIST = "network-ip-addr-b-list";
        private static final String TAG_NETWORK_IP_ADDR_W_LIST = "network-ip-addr-w-list";
        private static final String TAG_NETWORK_MOBILE_DATA_POLICY = "network-mobile-data-policy";
        private static final String TAG_NETWORK_MOBILE_DATA_SLOT_POLICY = "network-mobile-data-slot-policy";
        private static final String TAG_NETWORK_VPN_LIST = "network-vpn-list";
        private static final String TAG_NFC_ALL_POLICY = "nfc-all-policy";
        private static final String TAG_OPERATION_AI_KEY_POLICY = "operation-ai-key-policy";
        private static final String TAG_OPERATION_APP_NOTIFICATION_POLICY = "tag-operation-app-notification-policy";
        private static final String TAG_OPERATION_BACKUP_POLICY = "operation-backup-policy";
        private static final String TAG_OPERATION_BACK_KEY_POLICY = "operation-back-key-policy";
        private static final String TAG_OPERATION_CALL_RECORD_POLICY = "operation-call-record-policy";
        private static final String TAG_OPERATION_CLIPBOARD_POLICY = "operation-clipboard-policy";
        private static final String TAG_OPERATION_HARD_FACTORY_POLICY = "operation-hard-factory-policy";
        private static final String TAG_OPERATION_HOME_KEY_POLICY = "operation-home-key-policy";
        private static final String TAG_OPERATION_MENU_KEY_POLICY = "operation-menu-key-policy";
        private static final String TAG_OPERATION_MOCK_LOCATION_POLICY = "operation-mock-location-policy";
        private static final String TAG_OPERATION_POWER_PANEL_KEY_POLICY = "operation-power-panel-key-policy";
        private static final String TAG_OPERATION_POWER_SAVING_POLICY = "operation-power-saving-policy";
        private static final String TAG_OPERATION_RECENT_TASK_KEY_POLICY = "operation-recent-task-key-policy";
        private static final String TAG_OPERATION_STATUS_BAR_DISABLE2_ALL_POLICY = "operation-status-bar2-all-policy";
        private static final String TAG_OPERATION_STATUS_BAR_DISABLE_ALL_POLICY = "operation-status-bar-all-policy";
        private static final String TAG_OPERATION_STATUS_BAR_POLICY = "operation-status-bar-policy";
        private static final String TAG_OPERATION_SYS_UPGRADE_POLICY = "operation-sys-upgrade-policy";
        private static final String TAG_OPERATION_VOLUME_LONGPRESS_POLICY = "operation-volume-longpress-policy";
        private static final String TAG_PERIPHERAL_WLAN_DIRECT_POLICY = "peripheral-wlan-direct-policy";
        private static final String TAG_PERIPHERAL_WLAN_SCAN_ALWAYS_POLICY = "peripheral-wlan-scan-always-policy";
        private static final String TAG_PHONE_CALL1_POLICY = "phone-call1-policy";
        private static final String TAG_PHONE_CALL2_POLICY = "phone-call2-policy";
        private static final String TAG_PHONE_CALL_BW_POLICY = "phone-call-bw-policy";
        private static final String TAG_PHONE_CALL_B_LIST = "phone-call-b-list";
        private static final String TAG_PHONE_CALL_POLICY = "phone-call-policy";
        private static final String TAG_PHONE_CALL_W_LIST = "phone-call-w-list";
        private static final String TAG_PHONE_MASK_PERMISSION_B_LIST = "phone-mask-permission-b-list";
        private static final String TAG_PHONE_MASK_PERMISSION_W_LIST = "phone-mask-permission-w-list";
        private static final String TAG_PHONE_MASK_POLICY = "phone-mask-policy";
        private static final String TAG_PHONE_MMS1_POLICY = "phone-mms1-policy";
        private static final String TAG_PHONE_MMS2_POLICY = "phone-mms2-policy";
        private static final String TAG_PHONE_MMS_POLICY = "phone-mms-policy";
        private static final String TAG_PHONE_MUTLI_CALL_POLICY = "phone-mutli-call-policy";
        private static final String TAG_PHONE_NETWORK_SMS_POLICY = "phone-network-sms-policy";
        private static final String TAG_PHONE_SIM_SLOT_POLICY = "phone-sim-slot-policy";
        private static final String TAG_PHONE_SMS1_POLICY = "phone-sms1-policy";
        private static final String TAG_PHONE_SMS2_POLICY = "phone-sms2-policy";
        private static final String TAG_PHONE_SMS_BW_POLICY = "phone-sms-bw-policy";
        private static final String TAG_PHONE_SMS_B_LIST = "phone-sms-b-list";
        private static final String TAG_PHONE_SMS_POLICY = "phone-sms-policy";
        private static final String TAG_PHONE_SMS_W_LIST = "phone-sms-w-list";
        private static final String TAG_POLICIES = "policies";
        private static final String TAG_SECURITY_FORGOT_PASSWORD_POLICY = "security-forgot-password-policy";
        private static final String TAG_SETTINGS_MENU_LIST = "tag-settings-menu-list";
        private static final String TAG_SMS_IN_POLICY = "sms-in-policy";
        private static final String TAG_SMS_OUT_POLICY = "sms-out-policy";
        private static final String TAG_SPEAKER_POLICY = "speaker-policy";
        private static final String TAG_TELECOM_MASK_PERMISSION_BWLIST = "tag-telecom-mask-permission-bwlist";
        private static final String TAG_TELECOM_PIN_LOCK_POLICY = "telecom-pin-lock-policy";
        private static final String TAG_USB_AP_POLICY = "usb-ap-policy";
        private static final String TAG_USB_OTG_POLICY = "usb-otg-policy";
        private static final String TAG_USB_TRANSFER_POLICY = "usb-transfer-policy";
        private static final String TAG_USERMENU_SUPPORT = "user-menu-support";
        private static final String TAG_WLAN_AP_BW_POLICY = "wlan-ap-bw-policy";
        private static final String TAG_WLAN_AP_B_LIST = "wlan-ap-b-list";
        private static final String TAG_WLAN_AP_POLICY = "wlan-ap-policy";
        private static final String TAG_WLAN_AP_W_LIST = "wlan-ap-w-list";
        private static final String TAG_WLAN_BW_POLICY = "wlan-bw-policy";
        private static final String TAG_WLAN_B_LIST = "wlan-b-list";
        private static final String TAG_WLAN_POLICY = "wlan-policy";
        private static final String TAG_WLAN_W_LIST = "wlan-w-list";
        VivoAdminInfo mInfo;
        final int DEF_POLICY = 0;
        final ArrayMap<Integer, VivoDefaultAppInfo> mDefaultAppInfoMap = new ArrayMap<>();
        int wlanPolicy = 0;
        int wlanBWPolicy = 0;
        int wlanApPolicy = 0;
        int wlanApBWPolicy = 0;
        int bluetoothPolicy = 0;
        int bluetoothBWPolicy = 0;
        int bluetoothApPolicy = 0;
        int usbTransferPolicy = 0;
        int usbApPolicy = 0;
        int usbOtgPolicy = 0;
        int speakerPolicy = 0;
        int flashLightPolicy = 0;
        int locationPolicy = 0;
        int micPolicy = 0;
        int nfcAllPolicy = 0;
        int phoneCallPolicy = 0;
        int phoneCallBWPolicy = 0;
        int phoneSmsPolicy = 0;
        int phoneSmsBWPolicy = 0;
        int phoneMmsPolicy = 0;
        int phoneSimSlotPolicy = 0;
        int phoneMutliCallPolicy = 0;
        int appInstallBWPolicy = 0;
        int appUnInstallBWPolicy = 0;
        int appTrustedSourcePolicy = 0;
        int appDisallowedLaunchPolicy = 0;
        int appMeteredDataBWPolicy = 0;
        int appWlanDataBWPolicy = 0;
        int networkApnPolicy = 0;
        int networkMobileDataPolicy = 0;
        int networkMobileDataSlotPolicy = 0;
        int networkDomainBWPolicy = 0;
        int networkIpAddrBWPolicy = 0;
        int operationStatusBarDiablePolicy = 0;
        int operationStatusBarDiable2Policy = 0;
        int operationHomeKeyPolicy = 0;
        int operationMenuKeyPolicy = 0;
        int operationBackKeyPolicy = 0;
        int operationStatusbarPolicy = 0;
        int operationRecentTaskKeyPolicy = 0;
        int operationPowerPanelKeyPolicy = 0;
        int operationClipboardPolicy = 0;
        int operationBackupPolicy = 0;
        int operationPowerSavingPolicy = 0;
        int operationHardFactoryResetPolicy = 0;
        int operationCallRecordPolicy = 0;
        int adminDeviceOwnerPolicy = 0;
        int adminProfileOwnerPolicy = 0;
        int securityForgotPasswordPolicy = 0;
        int phoneCall1Policy = 0;
        int phoneCall2Policy = 0;
        int phoneSms1Policy = 0;
        int phoneSms2Policy = 0;
        int phoneMms1Policy = 0;
        int phoneMms2Policy = 0;
        int phoneMaskPolicy = 0;
        int userMenuSupport = 0;
        int callForwardingPolicy = 0;
        int phoneNetworkSMSPolicy = 0;
        int telecomPinLockPolicy = 0;
        int operationMockLocationPolicy = 0;
        int peripheralWlanDirectPolicy = 0;
        int peripheralWlanScanAlwaysPolicy = 0;
        int operationSysUpgradePolicy = 0;
        int telecomMaskPermissionBwlist = 0;
        int operationAppNotification = 0;
        int airplanePolicy = 0;
        int callInCountPolicy = -1;
        int callOutCountPolicy = -1;
        int SMSInCountPolicy = -1;
        int SMSOutCountPolicy = -1;
        String operationAiKeyPolicy = null;
        String operationVolumeLongPressPolicy = null;
        List<String> wlanBlist = null;
        List<String> wlanWlist = null;
        List<String> wlanApBlist = null;
        List<String> wlanApWlist = null;
        List<String> bluetoothBlist = null;
        List<String> bluetoothWlist = null;
        List<String> phoneCallBlist = null;
        List<String> phoneCallWlist = null;
        List<String> phoneSmsBlist = null;
        List<String> phoneSmsWlist = null;
        List<String> appInstallBlist = null;
        List<String> appInstallWlist = null;
        List<String> appUnInstallBlist = null;
        List<String> appUnInstallWlist = null;
        List<String> appTrustedSourcelist = null;
        List<String> appDisallowedLaunchlist = null;
        List<String> appDisabledApplist = null;
        List<String> appPersistentApplist = null;
        List<String> appDisallowedClearDatalist = null;
        List<String> appMeteredDataBlist = null;
        List<String> appMeteredDataWlist = null;
        List<String> appWlanDataBlist = null;
        List<String> appWlanDataWlist = null;
        List<String> networkDomainBlist = null;
        List<String> networkDomainWlist = null;
        List<String> networkIpAddrBlist = null;
        List<String> networkIpAddrWlist = null;
        List<String> appPermissionWlist = null;
        List<String> appAlarmWlist = null;
        List<String> appComponentDisablelist = null;
        List<String> adminActiveAccessibilitylist = null;
        List<String> appNotificationListenerlist = null;
        List<String> networkVpnlist = null;
        List<String> phoneMaskPermissionBlist = null;
        List<String> phoneMaskPermissionWlist = null;
        List<String> settingsMenulist = null;
        List<String> apnDisableRecoveryList = null;

        /* JADX INFO: Access modifiers changed from: package-private */
        /* loaded from: classes.dex */
        public static class VivoDefaultAppInfo {
            ComponentName componentName;
            boolean disableModify;

            VivoDefaultAppInfo(ComponentName name, boolean disable) {
                this.componentName = null;
                this.disableModify = true;
                this.componentName = name;
                this.disableModify = disable;
            }
        }

        VivoActiveAdmin(VivoAdminInfo info) {
            this.mInfo = info;
        }

        int getUid() {
            return this.mInfo.getActivityInfo().applicationInfo.uid;
        }

        public UserHandle getUserHandle() {
            return UserHandle.of(UserHandle.getUserId(this.mInfo.getActivityInfo().applicationInfo.uid));
        }

        void writeListToXml(XmlSerializer out, String tag, List<String> list) throws IllegalArgumentException, IllegalStateException, IOException {
            if (list == null || list.isEmpty()) {
                return;
            }
            out.startTag(null, tag);
            for (String s : list) {
                if (s != null) {
                    out.startTag(null, TAG_LIST_ITEM);
                    out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, s);
                    out.endTag(null, TAG_LIST_ITEM);
                }
            }
            out.endTag(null, tag);
        }

        List<String> readListFromXml(XmlPullParser parser) throws XmlPullParserException, IOException {
            String str;
            List<String> result = new ArrayList<>();
            int outerDepth = parser.getDepth();
            while (true) {
                int outerType = parser.next();
                if (outerType == 1 || (outerType == 3 && parser.getDepth() <= outerDepth)) {
                    break;
                } else if (outerType != 3 && outerType != 4) {
                    String outerTag = parser.getName();
                    if (TAG_LIST_ITEM.equals(outerTag) && (str = parser.getAttributeValue(null, VivoCustomDpmsImpl.ATTR_VALUE)) != null) {
                        result.add(str);
                    }
                }
            }
            return result;
        }

        void writeToXml(XmlSerializer out) throws IllegalArgumentException, IllegalStateException, IOException {
            if (this.wlanPolicy != 0) {
                out.startTag(null, TAG_WLAN_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.wlanPolicy));
                out.endTag(null, TAG_WLAN_POLICY);
            }
            if (this.wlanApPolicy != 0) {
                out.startTag(null, TAG_WLAN_AP_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.wlanApPolicy));
                out.endTag(null, TAG_WLAN_AP_POLICY);
            }
            if (this.wlanBWPolicy != 0) {
                out.startTag(null, TAG_WLAN_BW_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.wlanBWPolicy));
                out.endTag(null, TAG_WLAN_BW_POLICY);
            }
            if (this.wlanApBWPolicy != 0) {
                out.startTag(null, TAG_WLAN_AP_BW_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.wlanApBWPolicy));
                out.endTag(null, TAG_WLAN_AP_BW_POLICY);
            }
            if (this.bluetoothPolicy != 0) {
                out.startTag(null, TAG_BLUETOOTH_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.bluetoothPolicy));
                out.endTag(null, TAG_BLUETOOTH_POLICY);
            }
            if (this.airplanePolicy != 0) {
                out.startTag(null, TAG_AIRPLANE_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.airplanePolicy));
                out.endTag(null, TAG_AIRPLANE_POLICY);
            }
            if (this.bluetoothApPolicy != 0) {
                out.startTag(null, TAG_BLUETOOTH_AP_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.bluetoothApPolicy));
                out.endTag(null, TAG_BLUETOOTH_AP_POLICY);
            }
            if (this.bluetoothBWPolicy != 0) {
                out.startTag(null, TAG_BLUETOOTH_BW_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.bluetoothBWPolicy));
                out.endTag(null, TAG_BLUETOOTH_BW_POLICY);
            }
            if (this.usbTransferPolicy != 0) {
                out.startTag(null, TAG_USB_TRANSFER_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.usbTransferPolicy));
                out.endTag(null, TAG_USB_TRANSFER_POLICY);
            }
            if (this.usbApPolicy != 0) {
                out.startTag(null, TAG_USB_AP_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.usbApPolicy));
                out.endTag(null, TAG_USB_AP_POLICY);
            }
            if (this.usbOtgPolicy != 0) {
                out.startTag(null, TAG_USB_OTG_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.usbOtgPolicy));
                out.endTag(null, TAG_USB_OTG_POLICY);
            }
            if (this.speakerPolicy != 0) {
                out.startTag(null, TAG_SPEAKER_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.speakerPolicy));
                out.endTag(null, TAG_SPEAKER_POLICY);
            }
            if (this.flashLightPolicy != 0) {
                out.startTag(null, TAG_FLASH_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.flashLightPolicy));
                out.endTag(null, TAG_FLASH_POLICY);
            }
            if (this.locationPolicy != 0) {
                out.startTag(null, TAG_LOCATION_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.locationPolicy));
                out.endTag(null, TAG_LOCATION_POLICY);
            }
            if (this.micPolicy != 0) {
                out.startTag(null, TAG_MIC_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.micPolicy));
                out.endTag(null, TAG_MIC_POLICY);
            }
            if (this.nfcAllPolicy != 0) {
                out.startTag(null, TAG_NFC_ALL_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.nfcAllPolicy));
                out.endTag(null, TAG_NFC_ALL_POLICY);
            }
            if (this.phoneCallPolicy != 0) {
                out.startTag(null, TAG_PHONE_CALL_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.phoneCallPolicy));
                out.endTag(null, TAG_PHONE_CALL_POLICY);
            }
            if (this.phoneCallBWPolicy != 0) {
                out.startTag(null, TAG_PHONE_CALL_BW_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.phoneCallBWPolicy));
                out.endTag(null, TAG_PHONE_CALL_BW_POLICY);
            }
            if (this.phoneSmsPolicy != 0) {
                out.startTag(null, TAG_PHONE_SMS_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.phoneSmsPolicy));
                out.endTag(null, TAG_PHONE_SMS_POLICY);
            }
            if (this.phoneSmsBWPolicy != 0) {
                out.startTag(null, TAG_PHONE_SMS_BW_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.phoneSmsBWPolicy));
                out.endTag(null, TAG_PHONE_SMS_BW_POLICY);
            }
            if (this.phoneMmsPolicy != 0) {
                out.startTag(null, TAG_PHONE_MMS_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.phoneMmsPolicy));
                out.endTag(null, TAG_PHONE_MMS_POLICY);
            }
            if (this.phoneSimSlotPolicy != 0) {
                out.startTag(null, TAG_PHONE_SIM_SLOT_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.phoneSimSlotPolicy));
                out.endTag(null, TAG_PHONE_SIM_SLOT_POLICY);
            }
            if (this.phoneMutliCallPolicy != 0) {
                out.startTag(null, TAG_PHONE_MUTLI_CALL_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.phoneMutliCallPolicy));
                out.endTag(null, TAG_PHONE_MUTLI_CALL_POLICY);
            }
            if (this.appInstallBWPolicy != 0) {
                out.startTag(null, TAG_APP_INSTALL_BW_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.appInstallBWPolicy));
                out.endTag(null, TAG_APP_INSTALL_BW_POLICY);
            }
            if (this.appUnInstallBWPolicy != 0) {
                out.startTag(null, TAG_APP_UNINSTALL_BW_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.appUnInstallBWPolicy));
                out.endTag(null, TAG_APP_UNINSTALL_BW_POLICY);
            }
            if (this.appTrustedSourcePolicy != 0) {
                out.startTag(null, TAG_APP_TRUSTED_SOURCE_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.appTrustedSourcePolicy));
                out.endTag(null, TAG_APP_TRUSTED_SOURCE_POLICY);
            }
            if (this.appDisallowedLaunchPolicy != 0) {
                out.startTag(null, TAG_APP_DISALLOWED_LAUNCH_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.appDisallowedLaunchPolicy));
                out.endTag(null, TAG_APP_DISALLOWED_LAUNCH_POLICY);
            }
            if (this.appMeteredDataBWPolicy != 0) {
                out.startTag(null, TAG_APP_METERED_DATA_BW_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.appMeteredDataBWPolicy));
                out.endTag(null, TAG_APP_METERED_DATA_BW_POLICY);
            }
            if (this.appWlanDataBWPolicy != 0) {
                out.startTag(null, TAG_APP_WLAN_DATA_BW_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.appWlanDataBWPolicy));
                out.endTag(null, TAG_APP_WLAN_DATA_BW_POLICY);
            }
            if (this.networkApnPolicy != 0) {
                out.startTag(null, TAG_NETWORK_APN_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.networkApnPolicy));
                out.endTag(null, TAG_NETWORK_APN_POLICY);
            }
            if (this.networkMobileDataPolicy != 0) {
                out.startTag(null, TAG_NETWORK_MOBILE_DATA_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.networkMobileDataPolicy));
                out.endTag(null, TAG_NETWORK_MOBILE_DATA_POLICY);
            }
            if (this.networkMobileDataSlotPolicy != 0) {
                out.startTag(null, TAG_NETWORK_MOBILE_DATA_SLOT_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.networkMobileDataSlotPolicy));
                out.endTag(null, TAG_NETWORK_MOBILE_DATA_SLOT_POLICY);
            }
            if (this.networkDomainBWPolicy != 0) {
                out.startTag(null, TAG_NETWORK_DOMAIN_BW_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.networkDomainBWPolicy));
                out.endTag(null, TAG_NETWORK_DOMAIN_BW_POLICY);
            }
            if (this.networkIpAddrBWPolicy != 0) {
                out.startTag(null, TAG_NETWORK_IP_ADDR_BW_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.networkIpAddrBWPolicy));
                out.endTag(null, TAG_NETWORK_IP_ADDR_BW_POLICY);
            }
            if (this.operationHomeKeyPolicy != 0) {
                out.startTag(null, TAG_OPERATION_HOME_KEY_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.operationHomeKeyPolicy));
                out.endTag(null, TAG_OPERATION_HOME_KEY_POLICY);
            }
            if (this.operationMenuKeyPolicy != 0) {
                out.startTag(null, TAG_OPERATION_MENU_KEY_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.operationMenuKeyPolicy));
                out.endTag(null, TAG_OPERATION_MENU_KEY_POLICY);
            }
            if (this.operationBackKeyPolicy != 0) {
                out.startTag(null, TAG_OPERATION_BACK_KEY_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.operationBackKeyPolicy));
                out.endTag(null, TAG_OPERATION_BACK_KEY_POLICY);
            }
            if (this.operationRecentTaskKeyPolicy != 0) {
                out.startTag(null, TAG_OPERATION_RECENT_TASK_KEY_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.operationRecentTaskKeyPolicy));
                out.endTag(null, TAG_OPERATION_RECENT_TASK_KEY_POLICY);
            }
            if (this.operationPowerPanelKeyPolicy != 0) {
                out.startTag(null, TAG_OPERATION_POWER_PANEL_KEY_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.operationPowerPanelKeyPolicy));
                out.endTag(null, TAG_OPERATION_POWER_PANEL_KEY_POLICY);
            }
            if (this.operationStatusbarPolicy != 0) {
                out.startTag(null, TAG_OPERATION_STATUS_BAR_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.operationStatusbarPolicy));
                out.endTag(null, TAG_OPERATION_STATUS_BAR_POLICY);
            }
            if (this.operationStatusBarDiablePolicy != 0) {
                out.startTag(null, TAG_OPERATION_STATUS_BAR_DISABLE_ALL_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.operationStatusBarDiablePolicy));
                out.endTag(null, TAG_OPERATION_STATUS_BAR_DISABLE_ALL_POLICY);
            }
            if (this.operationStatusBarDiable2Policy != 0) {
                out.startTag(null, TAG_OPERATION_STATUS_BAR_DISABLE2_ALL_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.operationStatusBarDiable2Policy));
                out.endTag(null, TAG_OPERATION_STATUS_BAR_DISABLE2_ALL_POLICY);
            }
            if (this.operationClipboardPolicy != 0) {
                out.startTag(null, TAG_OPERATION_CLIPBOARD_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.operationClipboardPolicy));
                out.endTag(null, TAG_OPERATION_CLIPBOARD_POLICY);
            }
            if (this.operationBackupPolicy != 0) {
                out.startTag(null, TAG_OPERATION_BACKUP_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.operationBackupPolicy));
                out.endTag(null, TAG_OPERATION_BACKUP_POLICY);
            }
            if (this.operationPowerSavingPolicy != 0) {
                out.startTag(null, TAG_OPERATION_POWER_SAVING_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.operationPowerSavingPolicy));
                out.endTag(null, TAG_OPERATION_POWER_SAVING_POLICY);
            }
            if (this.operationHardFactoryResetPolicy != 0) {
                out.startTag(null, TAG_OPERATION_HARD_FACTORY_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.operationHardFactoryResetPolicy));
                out.endTag(null, TAG_OPERATION_HARD_FACTORY_POLICY);
            }
            if (this.adminDeviceOwnerPolicy != 0) {
                out.startTag(null, TAG_ADMIN_DEVICE_OWNER_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.adminDeviceOwnerPolicy));
                out.endTag(null, TAG_ADMIN_DEVICE_OWNER_POLICY);
            }
            if (this.adminProfileOwnerPolicy != 0) {
                out.startTag(null, TAG_ADMIN_PROFILE_OWNER_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.adminProfileOwnerPolicy));
                out.endTag(null, TAG_ADMIN_PROFILE_OWNER_POLICY);
            }
            if (!this.mDefaultAppInfoMap.isEmpty()) {
                Set<Map.Entry<Integer, VivoDefaultAppInfo>> set = this.mDefaultAppInfoMap.entrySet();
                for (Map.Entry<Integer, VivoDefaultAppInfo> entry : set) {
                    VivoDefaultAppInfo info = entry.getValue();
                    out.startTag(null, TAG_DEFAULT_APP_SET_POLICY);
                    out.attribute(null, "id", Integer.toString(entry.getKey().intValue()));
                    if (info.componentName != null) {
                        out.attribute(null, "component", info.componentName.flattenToString());
                        out.attribute(null, "disable-modify", Boolean.toString(info.disableModify));
                    }
                    out.endTag(null, TAG_DEFAULT_APP_SET_POLICY);
                }
            }
            if (this.securityForgotPasswordPolicy != 0) {
                out.startTag(null, TAG_SECURITY_FORGOT_PASSWORD_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.securityForgotPasswordPolicy));
                out.endTag(null, TAG_SECURITY_FORGOT_PASSWORD_POLICY);
            }
            if (this.phoneCall1Policy != 0) {
                out.startTag(null, TAG_PHONE_CALL1_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.phoneCall1Policy));
                out.endTag(null, TAG_PHONE_CALL1_POLICY);
            }
            if (this.phoneCall2Policy != 0) {
                out.startTag(null, TAG_PHONE_CALL2_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.phoneCall2Policy));
                out.endTag(null, TAG_PHONE_CALL2_POLICY);
            }
            if (this.phoneSms1Policy != 0) {
                out.startTag(null, TAG_PHONE_SMS1_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.phoneSms1Policy));
                out.endTag(null, TAG_PHONE_SMS1_POLICY);
            }
            if (this.phoneSms2Policy != 0) {
                out.startTag(null, TAG_PHONE_SMS2_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.phoneSms2Policy));
                out.endTag(null, TAG_PHONE_SMS2_POLICY);
            }
            if (this.phoneMms1Policy != 0) {
                out.startTag(null, TAG_PHONE_MMS1_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.phoneMms1Policy));
                out.endTag(null, TAG_PHONE_MMS1_POLICY);
            }
            if (this.phoneMms2Policy != 0) {
                out.startTag(null, TAG_PHONE_MMS2_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.phoneMms2Policy));
                out.endTag(null, TAG_PHONE_MMS2_POLICY);
            }
            if (this.phoneMaskPolicy != 0) {
                out.startTag(null, TAG_PHONE_MASK_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.phoneMaskPolicy));
                out.endTag(null, TAG_PHONE_MASK_POLICY);
            }
            if (this.operationVolumeLongPressPolicy != null) {
                out.startTag(null, TAG_OPERATION_VOLUME_LONGPRESS_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, this.operationVolumeLongPressPolicy);
                out.endTag(null, TAG_OPERATION_VOLUME_LONGPRESS_POLICY);
            }
            if (this.operationCallRecordPolicy != 0) {
                out.startTag(null, TAG_OPERATION_CALL_RECORD_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.operationCallRecordPolicy));
                out.endTag(null, TAG_OPERATION_CALL_RECORD_POLICY);
            }
            if (this.userMenuSupport != 0) {
                out.startTag(null, TAG_USERMENU_SUPPORT);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.userMenuSupport));
                out.endTag(null, TAG_USERMENU_SUPPORT);
            }
            if (this.callForwardingPolicy != 0) {
                out.startTag(null, TAG_CALL_FORWARDING_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.callForwardingPolicy));
                out.endTag(null, TAG_CALL_FORWARDING_POLICY);
            }
            if (this.phoneNetworkSMSPolicy != 0) {
                out.startTag(null, TAG_PHONE_NETWORK_SMS_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.phoneNetworkSMSPolicy));
                out.endTag(null, TAG_PHONE_NETWORK_SMS_POLICY);
            }
            if (this.telecomPinLockPolicy != 0) {
                out.startTag(null, TAG_TELECOM_PIN_LOCK_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.telecomPinLockPolicy));
                out.endTag(null, TAG_TELECOM_PIN_LOCK_POLICY);
            }
            if (this.operationMockLocationPolicy != 0) {
                out.startTag(null, TAG_OPERATION_MOCK_LOCATION_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.operationMockLocationPolicy));
                out.endTag(null, TAG_OPERATION_MOCK_LOCATION_POLICY);
            }
            if (this.peripheralWlanDirectPolicy != 0) {
                out.startTag(null, TAG_PERIPHERAL_WLAN_DIRECT_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.peripheralWlanDirectPolicy));
                out.endTag(null, TAG_PERIPHERAL_WLAN_DIRECT_POLICY);
            }
            if (this.peripheralWlanScanAlwaysPolicy != 0) {
                out.startTag(null, TAG_PERIPHERAL_WLAN_SCAN_ALWAYS_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.peripheralWlanScanAlwaysPolicy));
                out.endTag(null, TAG_PERIPHERAL_WLAN_SCAN_ALWAYS_POLICY);
            }
            if (this.operationSysUpgradePolicy != 0) {
                out.startTag(null, TAG_OPERATION_SYS_UPGRADE_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.operationSysUpgradePolicy));
                out.endTag(null, TAG_OPERATION_SYS_UPGRADE_POLICY);
            }
            if (this.telecomMaskPermissionBwlist != 0) {
                out.startTag(null, TAG_TELECOM_MASK_PERMISSION_BWLIST);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.telecomMaskPermissionBwlist));
                out.endTag(null, TAG_TELECOM_MASK_PERMISSION_BWLIST);
            }
            if (this.operationAppNotification != 0) {
                out.startTag(null, TAG_OPERATION_APP_NOTIFICATION_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.operationAppNotification));
                out.endTag(null, TAG_OPERATION_APP_NOTIFICATION_POLICY);
            }
            if (this.callInCountPolicy != -1) {
                out.startTag(null, TAG_CALL_IN_COUNT_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.callInCountPolicy));
                out.endTag(null, TAG_CALL_IN_COUNT_POLICY);
            }
            if (this.callOutCountPolicy != -1) {
                out.startTag(null, TAG_CALL_OUT_COUNT_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.callOutCountPolicy));
                out.endTag(null, TAG_CALL_OUT_COUNT_POLICY);
            }
            if (this.SMSInCountPolicy != -1) {
                out.startTag(null, TAG_SMS_IN_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.SMSInCountPolicy));
                out.endTag(null, TAG_SMS_IN_POLICY);
            }
            if (this.SMSOutCountPolicy != -1) {
                out.startTag(null, TAG_SMS_OUT_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, Integer.toString(this.SMSOutCountPolicy));
                out.endTag(null, TAG_SMS_OUT_POLICY);
            }
            if (this.operationAiKeyPolicy != null) {
                out.startTag(null, TAG_OPERATION_AI_KEY_POLICY);
                out.attribute(null, VivoCustomDpmsImpl.ATTR_VALUE, this.operationAiKeyPolicy);
                out.endTag(null, TAG_OPERATION_AI_KEY_POLICY);
            }
            writeListToXml(out, TAG_WLAN_B_LIST, this.wlanBlist);
            writeListToXml(out, TAG_WLAN_W_LIST, this.wlanWlist);
            writeListToXml(out, TAG_WLAN_AP_B_LIST, this.wlanApBlist);
            writeListToXml(out, TAG_WLAN_AP_W_LIST, this.wlanApWlist);
            writeListToXml(out, TAG_BLUETOOTH_B_LIST, this.bluetoothBlist);
            writeListToXml(out, TAG_BLUETOOTH_W_LIST, this.bluetoothWlist);
            writeListToXml(out, TAG_PHONE_CALL_B_LIST, this.phoneCallBlist);
            writeListToXml(out, TAG_PHONE_CALL_W_LIST, this.phoneCallWlist);
            writeListToXml(out, TAG_PHONE_SMS_B_LIST, this.phoneSmsBlist);
            writeListToXml(out, TAG_PHONE_SMS_W_LIST, this.phoneSmsWlist);
            writeListToXml(out, TAG_APP_INSTALL_B_LIST, this.appInstallBlist);
            writeListToXml(out, TAG_APP_INSTALL_W_LIST, this.appInstallWlist);
            writeListToXml(out, TAG_APP_UNINSTALL_B_LIST, this.appUnInstallBlist);
            writeListToXml(out, TAG_APP_UNINSTALL_W_LIST, this.appUnInstallWlist);
            writeListToXml(out, TAG_APP_TRUSTED_SOURCE_LIST, this.appTrustedSourcelist);
            writeListToXml(out, TAG_APP_DISALLOWED_LAUNCH_LIST, this.appDisallowedLaunchlist);
            writeListToXml(out, TAG_APP_DISABLED_APP_LIST, this.appDisabledApplist);
            writeListToXml(out, TAG_APP_PERSISTENT_APP_LIST, this.appPersistentApplist);
            writeListToXml(out, TAG_APP_DISALLOWED_CLEAR_DATA_LIST, this.appDisallowedClearDatalist);
            writeListToXml(out, TAG_APP_METERED_DATA_B_LIST, this.appMeteredDataBlist);
            writeListToXml(out, TAG_APP_METERED_DATA_W_LIST, this.appMeteredDataWlist);
            writeListToXml(out, TAG_APP_WLAN_DATA_B_LIST, this.appWlanDataBlist);
            writeListToXml(out, TAG_APP_WLAN_DATA_W_LIST, this.appWlanDataWlist);
            writeListToXml(out, TAG_NETWORK_DOMAIN_B_LIST, this.networkDomainBlist);
            writeListToXml(out, TAG_NETWORK_DOMAIN_W_LIST, this.networkDomainWlist);
            writeListToXml(out, TAG_NETWORK_IP_ADDR_B_LIST, this.networkIpAddrBlist);
            writeListToXml(out, TAG_NETWORK_IP_ADDR_W_LIST, this.networkIpAddrWlist);
            writeListToXml(out, TAG_APP_PERMISSION_W_LIST, this.appPermissionWlist);
            writeListToXml(out, TAG_APP_ALARM_W_LIST, this.appAlarmWlist);
            writeListToXml(out, TAG_APP_COMPONENT_DISABLE_LIST, this.appComponentDisablelist);
            writeListToXml(out, TAG_ADMIN_ACCESSIBILITY_LIST, this.adminActiveAccessibilitylist);
            writeListToXml(out, TAG_APP_NOTIFICATION_LIST, this.appNotificationListenerlist);
            writeListToXml(out, TAG_NETWORK_VPN_LIST, this.networkVpnlist);
            writeListToXml(out, TAG_PHONE_MASK_PERMISSION_B_LIST, this.phoneMaskPermissionBlist);
            writeListToXml(out, TAG_PHONE_MASK_PERMISSION_W_LIST, this.phoneMaskPermissionWlist);
            writeListToXml(out, TAG_SETTINGS_MENU_LIST, this.settingsMenulist);
            writeListToXml(out, TAG_APN_DISABLE_RECOVERY_LIST, this.apnDisableRecoveryList);
        }

        /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
            jadx.core.utils.exceptions.JadxRuntimeException: CFG modification limit reached, blocks count: 549
            	at jadx.core.dex.visitors.blocks.BlockProcessor.processBlocksTree(BlockProcessor.java:59)
            	at jadx.core.dex.visitors.blocks.BlockProcessor.visit(BlockProcessor.java:39)
            */
        void readFromXml(org.xmlpull.v1.XmlPullParser r10) throws org.xmlpull.v1.XmlPullParserException, java.io.IOException {
            /*
                Method dump skipped, instructions count: 2198
                To view this dump change 'Code comments level' option to 'DEBUG'
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.server.devicepolicy.VivoCustomDpmsImpl.VivoActiveAdmin.readFromXml(org.xmlpull.v1.XmlPullParser):void");
        }
    }

    public VivoCustomDpmsImpl(Context context, DevicePolicyManagerService service) {
        String str = SystemProperties.get("ro.build.gn.custom", CUSTOM_SHORT_NAME_DEFAULT);
        this.mRoShortName = str;
        String str2 = SystemProperties.get("ro.build.gn.emm.custom", str);
        this.mRoEmmShortName = str2;
        this.mIsDoingUnmount = false;
        this.mCustomShortName = this.mRoShortName;
        this.mEmmShortName = SystemProperties.get("persist.emm.custom.shortname", str2);
        this.mCustomType = SystemProperties.getInt("persist.emm.custom.type", this.mRoCustomType);
        this.mCustomMultiUser = SystemProperties.getInt("persist.vivo.custom.multiuser", 0);
        this.mStatusbarToken = new Binder();
        this.mCallbacks = new RemoteCallbackList<>();
        this.mCallStateCallbacks = new RemoteCallbackList<>();
        this.VIVO_CUSTOM_VERSION = SystemProperties.get("ro.build.version.bbk", "0");
        this.mEmmFromCota = null;
        this.needAutoStart = false;
        this.mIsBootCompleted = false;
        this.mIsDpmDefaultAppSet = false;
        this.mIsDpmPermissionOpSet = false;
        this.UPDATE_DEFAULT_APP = 10;
        this.UPDATE_DEFAULT_APP_FINISHED = 11;
        this.UPDATE_PERMISSION_OP = 12;
        this.UPDATE_PERMISSION_OP_FINISHED = 13;
        this.mIsOverseas = false;
        this.mUninstallBlistPkgs = null;
        this.mPersistentListPkgs = null;
        this.mPermissionListPkgs = null;
        this.mAutoStartPkgs = null;
        this.mInstallWlistPkgs = null;
        this.mDisableListPkgs = null;
        this.mDeviceOwner = null;
        this.mDefaultBrowser = null;
        this.mDefaultLauncher = null;
        this.mStorageListener = new StorageEventListener() { // from class: com.android.server.devicepolicy.VivoCustomDpmsImpl.2
            /* JADX WARN: Type inference failed for: r2v6, types: [com.android.server.devicepolicy.VivoCustomDpmsImpl$2$1] */
            public void onStorageStateChanged(String path, String oldState, String newState) {
                VLog.i(VivoCustomDpmsImpl.VIVO_LOG_TAG, "Received storage state changed notification that " + path + " changed state from " + oldState + " to " + newState);
                VolumeInfo exVolume = VivoCustomDpmsImpl.this.getExternalVolume();
                if (exVolume == null) {
                    VLog.w(VivoCustomDpmsImpl.VIVO_LOG_TAG, "onStorageStateChanged:formatSDCard failed because the ex sdcard is not exist ");
                    return;
                }
                String sdPath = exVolume.path;
                if (sdPath != null && sdPath.equals(path) && "unmounted".equals(newState) && VivoCustomDpmsImpl.this.mIsDoingUnmount) {
                    VivoCustomDpmsImpl.this.mIsDoingUnmount = false;
                    new Thread() { // from class: com.android.server.devicepolicy.VivoCustomDpmsImpl.2.1
                        @Override // java.lang.Thread, java.lang.Runnable
                        public void run() {
                            VolumeInfo exVolume2 = VivoCustomDpmsImpl.this.getExternalVolume();
                            if (exVolume2 == null) {
                                VLog.w(VivoCustomDpmsImpl.VIVO_LOG_TAG, "onStorageStateChanged:formatSDCard failed because the ex sdcard is not exist ");
                                return;
                            }
                            String sdId = exVolume2.id;
                            try {
                                VLog.d(VivoCustomDpmsImpl.VIVO_LOG_TAG, "formatInternalStorage sdId = " + sdId + ", format begin!");
                                VivoCustomDpmsImpl.this.iStorageManager.format(sdId);
                                VLog.d(VivoCustomDpmsImpl.VIVO_LOG_TAG, "formatInternalStorage sdId = " + sdId + ", format end!");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            try {
                                VLog.d(VivoCustomDpmsImpl.VIVO_LOG_TAG, "formatInternalStorage sdId = " + sdId + ", mount begin!");
                                VivoCustomDpmsImpl.this.iStorageManager.mount(sdId);
                                VLog.d(VivoCustomDpmsImpl.VIVO_LOG_TAG, "formatInternalStorage sdId = " + sdId + ", mount end!");
                            } catch (Exception e2) {
                                e2.printStackTrace();
                            }
                        }
                    }.start();
                }
            }
        };
        this.mHandler = new Handler() { // from class: com.android.server.devicepolicy.VivoCustomDpmsImpl.3
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                long callingId;
                super.handleMessage(msg);
                VLog.i(VivoCustomDpmsImpl.VIVO_LOG_TAG, "mHandler msg = " + msg.what);
                if (msg.what != 10 || VivoCustomDpmsImpl.this.mIsDpmDefaultAppSet) {
                    if (msg.what == 11) {
                        VivoCustomDpmsImpl.this.mIsDpmDefaultAppSet = false;
                        return;
                    } else if (msg.what != 12 || VivoCustomDpmsImpl.this.mIsDpmPermissionOpSet) {
                        if (msg.what == 13) {
                            VivoCustomDpmsImpl.this.mIsDpmPermissionOpSet = false;
                            return;
                        }
                        return;
                    } else {
                        VivoCustomDpmsImpl.this.mIsDpmPermissionOpSet = true;
                        int userHandle = msg.arg1;
                        VivoActiveAdmin ap = VivoCustomDpmsImpl.this.getVivoUserData(userHandle).mVivoActiveAdmin;
                        if (ap != null && ap.appPermissionWlist != null && !ap.appPermissionWlist.isEmpty()) {
                            callingId = Binder.clearCallingIdentity();
                            try {
                                try {
                                    for (String packageName : ap.appPermissionWlist) {
                                        PackageInfo packageInfo = VivoCustomDpmsImpl.this.mContext.getPackageManager().getPackageInfoAsUser(packageName, 4096, userHandle);
                                        if (packageInfo != null) {
                                            VLog.w(VivoCustomDpmsImpl.VIVO_LOG_TAG, "UPDATE_PERMISSION_OP packageName = " + packageName);
                                            if (packageInfo.requestedPermissions != null && Arrays.asList(packageInfo.requestedPermissions).contains("android.permission.WRITE_SETTINGS")) {
                                                VivoCustomDpmsImpl.this.mAppOpsManager.setMode(23, packageInfo.applicationInfo.uid, packageName, 0);
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    VLog.e(VivoCustomDpmsImpl.VIVO_LOG_TAG, "UPDATE_PERMISSION_OP failed");
                                    e.printStackTrace();
                                }
                            } finally {
                            }
                        }
                        VivoCustomDpmsImpl.this.mHandler.sendEmptyMessageDelayed(13, 2000L);
                        return;
                    }
                }
                VivoCustomDpmsImpl.this.mIsDpmDefaultAppSet = true;
                int userHandle2 = msg.arg1;
                VivoActiveAdmin ap2 = VivoCustomDpmsImpl.this.getVivoUserData(userHandle2).mVivoActiveAdmin;
                if (ap2 != null && ap2.mDefaultAppInfoMap != null && !ap2.mDefaultAppInfoMap.isEmpty()) {
                    callingId = Binder.clearCallingIdentity();
                    try {
                        VivoActiveAdmin.VivoDefaultAppInfo defaultApp = ap2.mDefaultAppInfoMap.get(3009);
                        if (defaultApp != null && defaultApp.componentName != null && VivoCustomDpmsImpl.this.isPkgExist(defaultApp.componentName.getPackageName(), userHandle2) && defaultApp.disableModify) {
                            VivoCustomDpmsImpl.this.sendChangedNotification(3009, userHandle2);
                        }
                        VivoActiveAdmin.VivoDefaultAppInfo defaultApp2 = ap2.mDefaultAppInfoMap.get(3012);
                        if (defaultApp2 != null && defaultApp2.componentName != null && VivoCustomDpmsImpl.this.isPkgExist(defaultApp2.componentName.getPackageName(), userHandle2) && defaultApp2.disableModify) {
                            try {
                                String dpmSet = defaultApp2.componentName.getPackageName() + "/" + defaultApp2.componentName.getShortClassName();
                                String curName = Settings.Secure.getStringForUser(VivoCustomDpmsImpl.this.mContentResolver, "default_input_method", userHandle2);
                                if (curName != null && !curName.equals(dpmSet)) {
                                    VLog.i(VivoCustomDpmsImpl.VIVO_LOG_TAG, "UPDATE_DEFAULT_APP resetDefaultInputMethod ri = " + curName + ", defaultApp = " + dpmSet);
                                    Settings.Secure.putStringForUser(VivoCustomDpmsImpl.this.mContentResolver, "enabled_input_methods", dpmSet, userHandle2);
                                    Settings.Secure.putStringForUser(VivoCustomDpmsImpl.this.mContentResolver, "default_input_method", dpmSet, userHandle2);
                                }
                            } catch (Exception e2) {
                                VLog.e(VivoCustomDpmsImpl.VIVO_LOG_TAG, "Exception UPDATE_DEFAULT_APP set Default InputMethod = " + defaultApp2.componentName);
                                e2.printStackTrace();
                            }
                            VivoCustomDpmsImpl.this.sendChangedNotification(3012, userHandle2);
                        }
                        VivoActiveAdmin.VivoDefaultAppInfo defaultApp3 = ap2.mDefaultAppInfoMap.get(3008);
                        if (defaultApp3 != null && defaultApp3.componentName != null && VivoCustomDpmsImpl.this.isPkgExist(defaultApp3.componentName.getPackageName(), userHandle2) && defaultApp3.disableModify) {
                            VivoCustomDpmsImpl.this.sendChangedNotification(3008, userHandle2);
                        }
                        VivoActiveAdmin.VivoDefaultAppInfo defaultApp4 = ap2.mDefaultAppInfoMap.get(3011);
                        if (defaultApp4 != null && defaultApp4.componentName != null && VivoCustomDpmsImpl.this.isPkgExist(defaultApp4.componentName.getPackageName(), userHandle2) && defaultApp4.disableModify) {
                            VivoCustomDpmsImpl.this.sendChangedNotification(3011, userHandle2);
                        }
                        VivoActiveAdmin.VivoDefaultAppInfo defaultApp5 = ap2.mDefaultAppInfoMap.get(3010);
                        if (defaultApp5 != null && defaultApp5.componentName != null && VivoCustomDpmsImpl.this.isPkgExist(defaultApp5.componentName.getPackageName(), userHandle2) && defaultApp5.disableModify) {
                            VivoCustomDpmsImpl.this.sendChangedNotification(3010, userHandle2);
                        }
                    } catch (Exception e3) {
                        VLog.e(VivoCustomDpmsImpl.VIVO_LOG_TAG, "UPDATE_DEFAULT_APP failed");
                        e3.printStackTrace();
                    } finally {
                    }
                }
                VivoCustomDpmsImpl.this.mHandler.sendEmptyMessageDelayed(11, 10000L);
            }
        };
        this.mVivoCustomMonitor = new BroadcastReceiver() { // from class: com.android.server.devicepolicy.VivoCustomDpmsImpl.4
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                int userHandle = intent.getIntExtra("android.intent.extra.user_handle", 0);
                try {
                    if ("android.intent.action.ACTION_PREFERRED_ACTIVITY_CHANGED".equals(action)) {
                        VLog.v(VivoCustomDpmsImpl.VIVO_LOG_TAG, "mVivoCustomMonitor ACTION_PREFERRED_ACTIVITY_CHANGED");
                        Message msg = new Message();
                        msg.what = 10;
                        msg.arg1 = userHandle;
                        if (!VivoCustomDpmsImpl.this.mHandler.hasMessages(10)) {
                            VivoCustomDpmsImpl.this.mHandler.sendMessageDelayed(msg, 3000L);
                        }
                    }
                } catch (Exception e) {
                    VLog.v(VivoCustomDpmsImpl.VIVO_LOG_TAG, "Exception in mVivoCustomMonitor");
                    e.printStackTrace();
                }
            }
        };
        this.mAppOpsCallback = new IAppOpsCallback.Stub() { // from class: com.android.server.devicepolicy.VivoCustomDpmsImpl.8
            public void opChanged(int op, int uid, String packageName) {
                int userHandle = UserHandle.getUserId(uid);
                if (op == 23) {
                    try {
                        VivoActiveAdmin ap = VivoCustomDpmsImpl.this.getVivoUserData(userHandle).mVivoActiveAdmin;
                        if (ap != null && ap.appPermissionWlist != null && ap.appPermissionWlist.contains(packageName)) {
                            VLog.v(VivoCustomDpmsImpl.VIVO_LOG_TAG, "mAppOpsCallback");
                            Message msg = new Message();
                            msg.what = 12;
                            msg.arg1 = userHandle;
                            if (!VivoCustomDpmsImpl.this.mHandler.hasMessages(12)) {
                                VivoCustomDpmsImpl.this.mHandler.sendMessageDelayed(msg, 2000L);
                            }
                        }
                    } catch (Exception e) {
                        VLog.v(VivoCustomDpmsImpl.VIVO_LOG_TAG, "Exception in mVivoCustomMonitor");
                        e.printStackTrace();
                    }
                }
            }
        };
        this.mVcodeReceiver = new BroadcastReceiver() { // from class: com.android.server.devicepolicy.VivoCustomDpmsImpl.9
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                VLog.v(VivoCustomDpmsImpl.VIVO_LOG_TAG, "mVcodeReceiver");
                int userHandle = context2.getUserId();
                VivoCustomDpmsImpl.this.handleExceptionInfo(userHandle);
            }
        };
        this.mService = service;
        this.mContext = context;
        initVivoBaseDpms();
    }

    protected void initVivoBaseDpms() {
        this.mIPackageManager = AppGlobals.getPackageManager();
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        this.mITelephony = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        this.mActivityManager = (ActivityManager) this.mContext.getSystemService(VivoFirewall.TYPE_ACTIVITY);
        this.iStorageManager = IStorageManager.Stub.asInterface(ServiceManager.getService("mount"));
        VivoUtils.init(this.mContext);
        getVivoUserData(0);
        updateCustomType(false);
        updateCustomInfo();
        LocalService localService = new LocalService();
        this.mLocalService = localService;
        LocalServices.addService(VivoPolicyManagerInternal.class, localService);
        this.mContentResolver = this.mContext.getContentResolver();
        this.mAppOpsManager = (AppOpsManager) this.mContext.getSystemService("appops");
        this.mDeviceInfo = new DeviceInfo();
        this.mIsEmmAPI = false;
        this.mIsRemoving = false;
        initOthers();
    }

    public void onBootCompleted() {
        if (SystemProperties.get("persist.sys.factory.mode", "no").equals("yes")) {
            VLog.v(VIVO_LOG_TAG, "onBootCompleted init fail, is factory mode");
        } else if (getCustomType() <= 0) {
        } else {
            int callingUid = Binder.getCallingUid();
            int userId = UserHandle.getUserId(callingUid);
            resetStatusBarDisabled(userId);
            initVcode(userId);
            preload(userId);
            addAutoStart(userId);
            this.mIsBootCompleted = true;
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.ACTION_PREFERRED_ACTIVITY_CHANGED");
            this.mContext.registerReceiver(this.mVivoCustomMonitor, intentFilter);
            registerPermissionMonitor(userId);
        }
    }

    public void addAutoStart(int userId) {
        String str;
        VLog.v(VIVO_LOG_TAG, "addAutoStart");
        if (this.mPermissionListPkgs != null) {
            try {
                AppsPermisionsThread localThread = new AppsPermisionsThread(this.mPermissionListPkgs, userId, true);
                localThread.start();
            } catch (Exception e) {
                VLog.e(VIVO_LOG_TAG, "addAutoStart setAppPermisions failed");
                e.printStackTrace();
            }
        }
        List<String> persistentApp = getAppPersistentAppList(null, userId);
        if (persistentApp != null && persistentApp.size() > 0 && (str = this.mEmmFromCota) != null && persistentApp.contains(str)) {
            VLog.v(VIVO_LOG_TAG, this.mEmmFromCota + " is already persistent app,return");
            return;
        }
        if (this.mAutoStartPkgs != null) {
            AutoStartThread localThread2 = new AutoStartThread(this.mAutoStartPkgs, userId);
            localThread2.start();
        }
        if (this.needAutoStart && this.mEmmFromCota != null) {
            try {
                VivoPolicyData policy = getVivoUserData(userId);
                if (policy.mEmmBlackList != null && policy.mEmmBlackList.size() > 0 && policy.mEmmBlackList.contains(this.mEmmFromCota)) {
                    VLog.e(VIVO_LOG_TAG, "addAutoStart failed, " + this.mEmmFromCota + " in BlackList");
                } else if (policy.mEmmDisabledList != null && policy.mEmmDisabledList.size() > 0 && policy.mEmmDisabledList.contains(this.mEmmFromCota)) {
                    VLog.e(VIVO_LOG_TAG, "addAutoStart failed, " + this.mEmmFromCota + " in DisabledList");
                } else {
                    PackageInfo packageInfo = this.mIPackageManager.getPackageInfo(this.mEmmFromCota, 0, userId);
                    if (packageInfo == null) {
                        VLog.e(VIVO_LOG_TAG, "addAutoStart mEmmFromCota = " + this.mEmmFromCota + " is not exist! ");
                        return;
                    }
                    List<String> list = new ArrayList<>();
                    list.add(this.mEmmFromCota);
                    AutoStartThread localThread3 = new AutoStartThread(list, userId);
                    localThread3.start();
                }
            } catch (Exception e2) {
                VLog.e(VIVO_LOG_TAG, "addAutoStart failed, " + e2);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class AutoStartThread extends Thread {
        private List<String> pkgs;
        private int userId;

        public AutoStartThread(List<String> pkgs, int userId) {
            this.pkgs = null;
            this.pkgs = pkgs;
            this.userId = userId;
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            synchronized (AutoStartThread.class) {
                if (this.pkgs != null && !this.pkgs.isEmpty()) {
                    PackageManagerService pms = ServiceManager.getService("package");
                    for (String pkg : this.pkgs) {
                        try {
                            pms.setPackageStoppedState(pkg, false, this.userId);
                            VivoCustomDpmsImpl.this.updateAllBgStartUpDB(pkg, 0);
                            VivoCustomDpmsImpl.this.updateAllBgActivityDB(pkg, 0);
                            VLog.v(VivoCustomDpmsImpl.VIVO_LOG_TAG, "addAutoStart succeeded PkgName = " + pkg);
                        } catch (Exception e) {
                            VLog.e(VivoCustomDpmsImpl.VIVO_LOG_TAG, "run AutoStartThread failed, " + e);
                        }
                    }
                }
            }
        }
    }

    public void initOthers() {
        VLog.v(VIVO_LOG_TAG, "initOthers");
        if (SystemProperties.get("persist.sys.factory.mode", "no").equals("yes")) {
            VLog.v(VIVO_LOG_TAG, "initOthers fail, is factory mode");
            return;
        }
        this.mIsOverseas = "yes".equals(SystemProperties.get("ro.vivo.product.overseas", "no"));
        this.mUserManagerInternal = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        if (getCustomType() <= 0) {
            return;
        }
        if (this.mUserManagerInternal != null && getVivoUserData(0).mVivoActiveAdmin != null) {
            this.mUserManagerInternal.setDeviceManaged(true);
        }
        String uninstallBlistPkgs = getDefaultProp("ro.vivo.cust.uninstall.blist.pkgs");
        String persistentListPkgs = getDefaultProp("ro.vivo.cust.persistent.pkgs");
        String permissionListPkgs = getDefaultProp("ro.vivo.cust.permission.pkgs");
        String autoStartPkgs = getDefaultProp("ro.vivo.cust.autostart.pkgs");
        String installWlistPkgs = getDefaultProp("ro.vivo.cust.install.wlist.pkgs");
        String disableListPkgs = getDefaultProp("ro.vivo.cust.disable.pkgs");
        String deviceowner = getDefaultProp("ro.vivo.cust.device.owner");
        String defaultBrowser = getDefaultProp("ro.vivo.cust.default.browser");
        String defaultLauncher = getDefaultProp("ro.vivo.cust.default.launcher");
        if (uninstallBlistPkgs != null) {
            try {
                if (uninstallBlistPkgs.length() > 0) {
                    this.mUninstallBlistPkgs = Arrays.asList(uninstallBlistPkgs.split("\\s+"));
                }
            } catch (Exception e) {
                VLog.v(VIVO_LOG_TAG, "initOthers failed");
                e.printStackTrace();
                return;
            }
        }
        if (persistentListPkgs != null && persistentListPkgs.length() > 0) {
            this.mPersistentListPkgs = Arrays.asList(persistentListPkgs.split("\\s+"));
        }
        if (permissionListPkgs != null && permissionListPkgs.length() > 0) {
            this.mPermissionListPkgs = Arrays.asList(permissionListPkgs.split("\\s+"));
        }
        if (autoStartPkgs != null && autoStartPkgs.length() > 0) {
            this.mAutoStartPkgs = Arrays.asList(autoStartPkgs.split("\\s+"));
        }
        if (installWlistPkgs != null && installWlistPkgs.length() > 0) {
            this.mInstallWlistPkgs = Arrays.asList(installWlistPkgs.split("\\s+"));
        }
        if (disableListPkgs != null && disableListPkgs.length() > 0) {
            this.mDisableListPkgs = Arrays.asList(disableListPkgs.split("\\s+"));
        }
        if (deviceowner != null && deviceowner.length() > 0) {
            this.mDeviceOwner = ComponentName.unflattenFromString(deviceowner);
        }
        if (defaultBrowser != null && defaultBrowser.length() > 0) {
            this.mDefaultBrowser = ComponentName.unflattenFromString(defaultBrowser);
        }
        if (defaultLauncher != null && defaultLauncher.length() > 0) {
            this.mDefaultLauncher = ComponentName.unflattenFromString(defaultLauncher);
        }
    }

    private String getDefaultProp(String propName) {
        if (propName == null || propName.isEmpty()) {
            VLog.v(VIVO_LOG_TAG, "getDefaultProp prop name is null");
            return null;
        }
        String propValue = SystemProperties.get(propName, (String) null);
        for (int i = 1; i < 10; i++) {
            String newPropName = propName + Integer.toString(i);
            String tmpPropValue = SystemProperties.get(newPropName, (String) null);
            VLog.v(VIVO_LOG_TAG, "getDefaultProp prop name : " + newPropName + " value : " + propValue);
            if (tmpPropValue == null || tmpPropValue.length() <= 0) {
                break;
            }
            propValue = propValue + " " + tmpPropValue;
        }
        return propValue;
    }

    private void preload(int userHandle) {
        VLog.v(VIVO_LOG_TAG, "preload");
        synchronized (getLockObject()) {
            if (this.mDisableListPkgs != null && !this.mDisableListPkgs.isEmpty()) {
                for (String pkg : this.mDisableListPkgs) {
                    try {
                        PackageInfo packageInfo = this.mIPackageManager.getPackageInfo(pkg, 0, userHandle);
                        if (packageInfo != null) {
                            this.mIPackageManager.setApplicationEnabledSetting(pkg, 2, 0, userHandle, this.mContext.getPackageName());
                        }
                    } catch (Exception e) {
                        VLog.e(VIVO_LOG_TAG, "preload Disable Pkgs packageNames = " + pkg + " failed! " + e);
                    }
                }
            }
            if (this.mDeviceOwner != null && getVivoAdminUncheckedLocked(userHandle) == null) {
                this.mIsEmmAPI = true;
                try {
                    this.mService.setActiveAdmin(this.mDeviceOwner, false, userHandle);
                    this.mService.setDeviceOwner(this.mDeviceOwner, "owner", userHandle);
                } catch (Exception e2) {
                    VLog.e(VIVO_LOG_TAG, "preload set device owner = " + this.mDeviceOwner);
                    e2.printStackTrace();
                }
                this.mIsEmmAPI = false;
            }
            if (this.mDefaultBrowser != null && getVivoAdminUncheckedLocked(userHandle) == null) {
                try {
                    ResolveInfo ri = this.mIPackageManager.findPersistentPreferredActivity(getBrowserIntent(), userHandle);
                    if (ri != null && ri.getComponentInfo() != null && !this.mDefaultBrowser.getClassName().equals(ri.getComponentInfo().name)) {
                        this.mIPackageManager.clearPackagePersistentPreferredActivities(ri.getComponentInfo().packageName, userHandle);
                    }
                    this.mIPackageManager.addPersistentPreferredActivity(getBrowserFilter(), this.mDefaultBrowser, userHandle);
                    this.mIPackageManager.flushPackageRestrictionsAsUser(userHandle);
                    sendChangedNotification(3010, userHandle);
                } catch (Exception e3) {
                    VLog.e(VIVO_LOG_TAG, "preload set Default Browser = " + this.mDefaultBrowser);
                    e3.printStackTrace();
                }
            }
            if (this.mDefaultLauncher != null && getVivoAdminUncheckedLocked(userHandle) == null) {
                try {
                    writeAllowReplaceDesktopState();
                    ResolveInfo ri2 = this.mIPackageManager.findPersistentPreferredActivity(getHomeIntent(), userHandle);
                    if (ri2 != null && ri2.getComponentInfo() != null && !this.mDefaultLauncher.getClassName().equals(ri2.getComponentInfo().name)) {
                        this.mIPackageManager.clearPackagePersistentPreferredActivities(ri2.getComponentInfo().packageName, userHandle);
                    }
                    this.mIPackageManager.addPersistentPreferredActivity(getHomeFilter(), this.mDefaultLauncher, userHandle);
                    this.mIPackageManager.flushPackageRestrictionsAsUser(userHandle);
                    sendChangedNotification(3009, userHandle);
                } catch (Exception e4) {
                    VLog.e(VIVO_LOG_TAG, "preload set Default Launcher = " + this.mDefaultLauncher);
                    e4.printStackTrace();
                }
            }
            VivoActiveAdmin ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            if (ap != null && ap.usbOtgPolicy == 1) {
                setOtgEnable(false);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isPkgExist(String pkg, int userHandle) {
        PackageInfo packageInfo;
        try {
            packageInfo = this.mIPackageManager.getPackageInfo(pkg, 0, userHandle);
        } catch (Exception e) {
            VLog.e(VIVO_LOG_TAG, pkg + " is not exist!");
        }
        if (packageInfo == null) {
            return false;
        }
        return true;
    }

    public void onBroadcastReceive(Context context, Intent intent) {
        Uri data;
        String pkgName;
        String pkgName2;
        String action = intent.getAction();
        int userHandle = intent.getIntExtra("android.intent.extra.user_handle", 0);
        if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
            Uri data2 = intent.getData();
            if (data2 == null || (pkgName2 = data2.getEncodedSchemeSpecificPart()) == null) {
                return;
            }
            VLog.v(VIVO_LOG_TAG, "Package added pkgName = " + pkgName2);
            checkAppState(pkgName2, userHandle, true);
            setInstalledPkgTelecomMaskPermission(userHandle, pkgName2);
        } else if (!"android.intent.action.PACKAGE_REMOVED".equals(action)) {
            if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
                if (this.mCustomType == 1 && getVivoAdmin(userHandle) != null) {
                    sendDebugWarningNotification(userHandle);
                }
                handleExceptionInfo(userHandle);
            } else if (!"android.intent.action.PACKAGE_CHANGED".equals(action) || (data = intent.getData()) == null || (pkgName = data.getEncodedSchemeSpecificPart()) == null) {
            } else {
                disabledAppState(pkgName, userHandle);
            }
        } else {
            Uri data3 = intent.getData();
            boolean removing = intent.getBooleanExtra("android.intent.extra.DATA_REMOVED", false);
            if (data3 != null) {
                String pkgName3 = data3.getEncodedSchemeSpecificPart();
                VLog.v(VIVO_LOG_TAG, "Package removed pkgName = " + pkgName3 + ", removing = " + removing);
                if (pkgName3 == null || !removing) {
                    return;
                }
                removeAppPolicy(pkgName3, context.getUserId());
                removeEmmInfo(pkgName3, context.getUserId());
            }
        }
    }

    private void setInstalledPkgTelecomMaskPermission(int userHandle, String pkgName) {
        RemoteCallback mRemoteCallback;
        ComponentName vivoAdmin = getVivoAdminUncheckedLocked(userHandle);
        VivoActiveAdmin ap = getVivoActiveAdminUncheckedLocked(vivoAdmin, userHandle);
        if (ap != null && ap.telecomMaskPermissionBwlist == 4) {
            if (ap.phoneMaskPermissionWlist == null || (ap.phoneMaskPermissionWlist != null && !ap.phoneMaskPermissionWlist.contains(pkgName))) {
                List<String> appPermissionWhiteList = getAppPermissionWhiteListUncheckedLocked(ap);
                if (this.MASK_EXEMPT_APP.contains(pkgName)) {
                    return;
                }
                if (appPermissionWhiteList == null || !appPermissionWhiteList.contains(pkgName)) {
                    try {
                        final CompletableFuture<Boolean> result = new CompletableFuture<>();
                        mRemoteCallback = new RemoteCallback(new RemoteCallback.OnResultListener() { // from class: com.android.server.devicepolicy.-$$Lambda$VivoCustomDpmsImpl$GJZYKHqQB7TAlW5RoaJMKbC7izg
                            public final void onResult(Bundle bundle) {
                                result.complete(Boolean.valueOf(b != null));
                            }
                        });
                        setPermissionGrantState(vivoAdmin, this.mContext.getPackageName(), pkgName, "android.permission.READ_CALL_LOG", 2, mRemoteCallback);
                    } catch (RemoteException e) {
                        e = e;
                    }
                    try {
                        setPermissionGrantState(vivoAdmin, this.mContext.getPackageName(), pkgName, "android.permission.READ_SMS", 2, mRemoteCallback);
                        setPermissionGrantState(vivoAdmin, this.mContext.getPackageName(), pkgName, "android.permission.RECEIVE_SMS", 2, mRemoteCallback);
                        setPermissionGrantState(vivoAdmin, this.mContext.getPackageName(), pkgName, "android.permission.READ_CONTACTS", 2, mRemoteCallback);
                    } catch (RemoteException e2) {
                        e = e2;
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    Object getLockObject() {
        return this.mService.getLockObject();
    }

    public void resetStatusBarDisabled(int userHandle) {
        synchronized (getLockObject()) {
            VLog.v(VIVO_LOG_TAG, "resetStatusBarDisabled");
            ComponentName admin = getVivoAdminUncheckedLocked(userHandle);
            if (admin == null) {
                return;
            }
            VivoActiveAdmin ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            if (ap != null) {
                try {
                    IStatusBarService asInterface = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
                    this.mStatusBarService = asInterface;
                    if (asInterface == null) {
                        return;
                    }
                    if (ap.operationStatusBarDiablePolicy != 0) {
                        this.mStatusBarService.disableForUser(ap.operationStatusBarDiablePolicy, this.mStatusbarToken, ap.mInfo.getPackageName(), userHandle);
                    }
                    if (ap.operationStatusBarDiable2Policy != 0) {
                        this.mStatusBarService.disable2ForUser(ap.operationStatusBarDiable2Policy, this.mStatusbarToken, ap.mInfo.getPackageName(), userHandle);
                    }
                    VLog.v(VIVO_LOG_TAG, "resetStatusBarDisabled completed");
                } catch (Exception e) {
                    VLog.e(VIVO_LOG_TAG, "resetStatusBarDisabled fail! " + e);
                }
            }
        }
    }

    private void checkAppState(String packageName, int userHandle, boolean isAdd) {
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            if (ap == null) {
                return;
            }
            if (isAdd && packageName != null && ap.appDisallowedLaunchlist != null && ap.appDisallowedLaunchlist.contains(packageName)) {
                List<String> keys = new ArrayList<>();
                keys.add(packageName);
                try {
                    this.mIPackageManager.setPackagesSuspendedAsUser((String[]) keys.toArray(new String[keys.size()]), isAdd, (PersistableBundle) null, (PersistableBundle) null, (SuspendDialogInfo) null, VivoPermissionUtils.OS_PKG, userHandle);
                } catch (Exception e) {
                }
            }
        }
    }

    private void disabledAppState(String packageName, int userHandle) {
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            if (ap == null) {
                if (this.mDisableListPkgs != null && !this.mDisableListPkgs.isEmpty() && packageName != null && this.mDisableListPkgs.contains(packageName)) {
                    try {
                        this.mIPackageManager.setApplicationEnabledSetting(packageName, 2, 0, userHandle, this.mContext.getPackageName());
                    } catch (Exception e) {
                        VLog.e(VIVO_LOG_TAG, "disabledAppState failed! " + e);
                    }
                }
                return;
            }
            VLog.v(VIVO_LOG_TAG, "disabledAppState packageName = " + packageName);
            if (packageName != null && ap.appDisabledApplist != null && ap.appDisabledApplist.contains(packageName)) {
                try {
                    this.mIPackageManager.setApplicationEnabledSetting(packageName, 2, 0, userHandle, this.mContext.getPackageName());
                } catch (Exception e2) {
                    VLog.e(VIVO_LOG_TAG, "disabledAppState failed! " + e2);
                }
            }
            return;
        }
    }

    private void removeEmmInfo(String packageName, int userHandle) {
        synchronized (getLockObject()) {
            VivoPolicyData policy = getVivoUserData(userHandle);
            if (packageName != null && policy.mEmmInfoMap.containsKey(packageName)) {
                setEmmPackage(packageName, null, false, userHandle);
            }
        }
    }

    private void removeAppPolicy(String packageName, int userHandle) {
        synchronized (getLockObject()) {
            VivoPolicyData policy = getVivoUserData(userHandle);
            VivoActiveAdmin ap = policy.mVivoActiveAdmin;
            if (packageName != null && ap != null && ap.appPermissionWlist != null && ap.appPermissionWlist.size() > 0 && ap.appPermissionWlist.contains(packageName)) {
                ap.appPermissionWlist.remove(packageName);
                saveVivoSettingsLocked(1509, userHandle);
            }
        }
    }

    public void registerPolicyCallback(IVivoPolicyManagerCallback callback) {
        synchronized (getLockObject()) {
            if (callback != null) {
                VLog.v(VIVO_LOG_TAG, "registerPolicyCallback");
                this.mCallbacks.register(callback);
            }
        }
    }

    public void unregisterPolicyCallback(IVivoPolicyManagerCallback callback) {
        synchronized (getLockObject()) {
            if (callback != null) {
                VLog.v(VIVO_LOG_TAG, "unregisterPolicyCallback");
                this.mCallbacks.unregister(callback);
            }
        }
    }

    public void notifyVivoPolicyCallback(int poId) {
        int i = this.mCallbacks.beginBroadcast();
        while (i > 0) {
            i--;
            try {
                this.mCallbacks.getBroadcastItem(i).onVivoPolicyChanged(poId);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        this.mCallbacks.finishBroadcast();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class LocalService extends VivoPolicyManagerInternal {
        private List<VivoPolicyManagerInternal.VivoPolicyListener> mVivoPolicyListener;

        LocalService() {
        }

        public void setVivoPolicyListener(VivoPolicyManagerInternal.VivoPolicyListener listener) {
            synchronized (this) {
                if (this.mVivoPolicyListener == null) {
                    this.mVivoPolicyListener = new ArrayList();
                }
                if (!this.mVivoPolicyListener.contains(listener)) {
                    this.mVivoPolicyListener.add(listener);
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void notifyVivoPolicyChanged(int poId) {
            List<VivoPolicyManagerInternal.VivoPolicyListener> listeners;
            VivoCustomDpmsImpl.this.notifyVivoPolicyCallback(poId);
            List<VivoPolicyManagerInternal.VivoPolicyListener> list = this.mVivoPolicyListener;
            if (list == null || list.isEmpty()) {
                VLog.v(VivoCustomDpmsImpl.VIVO_LOG_TAG, "notifyVivoPolicyChanged mVivoPolicyListener is null");
                return;
            }
            synchronized (this) {
                listeners = new ArrayList<>(this.mVivoPolicyListener);
            }
            int listenerCount = listeners.size();
            for (int i = 0; i < listenerCount; i++) {
                listeners.get(i).onVivoPolicyChanged(poId);
            }
            VLog.v(VivoCustomDpmsImpl.VIVO_LOG_TAG, "notifyVivoPolicyChanged poId= " + poId);
        }
    }

    public void updateProjectInfo() {
        checkCallingFromVivoApp();
        this.mRoCustomType = SystemProperties.getInt("ro.build.gn.support", 0);
        VLog.i(VIVO_LOG_TAG, "mRoCustomType = " + this.mRoCustomType + ", mCustomType = " + this.mCustomType);
        if (this.mRoCustomType == 0 && this.mCustomType == 1) {
            VLog.i(VIVO_LOG_TAG, "There is no need to update project info.");
        } else {
            this.mCustomType = SystemProperties.getInt("persist.emm.custom.type", this.mRoCustomType);
            customTypeChanged();
        }
        String str = SystemProperties.get("ro.build.gn.custom", CUSTOM_SHORT_NAME_DEFAULT);
        this.mRoShortName = str;
        String str2 = SystemProperties.get("ro.build.gn.emm.custom", str);
        this.mRoEmmShortName = str2;
        this.mCustomShortName = this.mRoShortName;
        this.mEmmShortName = SystemProperties.get("persist.emm.custom.shortname", str2);
    }

    public int getCustomType() {
        return this.mCustomType;
    }

    private boolean updateCustomType(boolean shouldSend) {
        boolean hasDebugEmm = false;
        boolean isUpdate = false;
        try {
            VivoPolicyData policy = getVivoUserData(0);
            if (policy.mEmmInfoMap != null && policy.mEmmInfoMap.size() > 0) {
                Iterator<VivoEmmInfo> it = policy.mEmmInfoMap.values().iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    VivoEmmInfo info = it.next();
                    String shortName = info.getEmmShortName();
                    if ("debug".equals(shortName)) {
                        hasDebugEmm = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            VLog.e(VIVO_LOG_TAG, "updateCustomType exception " + e);
        }
        if (hasDebugEmm && this.mCustomType == 0) {
            this.mCustomType = 1;
            isUpdate = true;
        } else if (!hasDebugEmm && this.mCustomType == 1) {
            this.mCustomType = 0;
            isUpdate = true;
        }
        VLog.i(VIVO_LOG_TAG, "updateCustomType mCustomType = " + this.mCustomType + ", isUpdate = " + isUpdate + ", shouldSend = " + shouldSend);
        if (isUpdate && shouldSend) {
            customTypeChanged();
        }
        return isUpdate;
    }

    private void updateCustomInfo() {
        XmlPullParser parser;
        String tag;
        VLog.v(VIVO_LOG_TAG, "updateCustomInfo");
        String base = new File(new File(VivoUtils.VIVO_CUSTOM_PATH), VivoUtils.VIVO_CUSTOM_XML).getAbsolutePath();
        File file = new File(base);
        JournaledFile journal = new JournaledFile(file, new File(base + ".tmp"));
        FileInputStream stream = null;
        File file2 = journal.chooseForRead();
        try {
            stream = new FileInputStream(file2);
            parser = Xml.newPullParser();
            parser.setInput(stream, StandardCharsets.UTF_8.name());
            while (true) {
                int type = parser.next();
                if (type == 1 || type == 2) {
                    break;
                }
            }
            tag = parser.getName();
        } catch (FileNotFoundException e) {
        } catch (Exception e2) {
            VLog.w(VIVO_LOG_TAG, "failed parsing " + file2, e2);
        }
        if (!"config".equals(tag)) {
            throw new XmlPullParserException("Settings do not start with config tag: found " + tag);
        }
        parser.next();
        int outerDepth = parser.getDepth();
        while (true) {
            int type2 = parser.next();
            if (type2 == 1 || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                break;
            } else if (type2 != 3 && type2 != 4 && "projectinfos".equals(parser.getName())) {
                String shortName = parser.getAttributeValue(null, "ShortName");
                if (shortName != null && shortName.length() > 1) {
                    this.mEmmShortName = shortName;
                    VLog.v(VIVO_LOG_TAG, "readCustomInfoFromXml mEmmShortName = " + this.mEmmShortName);
                }
                String customType = parser.getAttributeValue(null, "ProjectId");
                if (customType != null && customType.length() > 0) {
                    this.mCustomType = Integer.parseInt(customType);
                    VLog.v(VIVO_LOG_TAG, "readCustomInfoFromXml mCustomType = " + this.mCustomType);
                }
                readEmmFromXml(parser);
            }
        }
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e3) {
            }
        }
    }

    private void readEmmFromXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        ComponentName component;
        VLog.v(VIVO_LOG_TAG, "updateCustomInfo readEmmFromXml");
        int outerDepth = parser.getDepth();
        while (true) {
            int outerType = parser.next();
            if (outerType != 1) {
                if (outerType != 3 || parser.getDepth() > outerDepth) {
                    if (outerType != 3 && outerType != 4) {
                        String outerTag = parser.getName();
                        if ("admincomponent".equals(outerTag)) {
                            String auto = parser.getAttributeValue(null, "AutoStart");
                            if (auto != null) {
                                this.needAutoStart = Boolean.parseBoolean(auto);
                                VLog.v(VIVO_LOG_TAG, "readCustomInfoFromXml needAutoStart = " + this.needAutoStart);
                            }
                            String name = parser.nextText();
                            if (!TextUtils.isEmpty(name) && (component = ComponentName.unflattenFromString(name)) != null) {
                                this.mEmmFromCota = component.getPackageName();
                            }
                            VLog.v(VIVO_LOG_TAG, "readCustomInfoFromXml mEmmFromCota = " + this.mEmmFromCota);
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private void customTypeChanged() {
        Intent intent = new Intent("vivo.app.action.VIVO_EMM_CUSTOM_TYPE_CHANGED");
        intent.setFlags(1073741824);
        long token = Binder.clearCallingIdentity();
        try {
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void sendDebugWarningNotification(int userHandle) {
        long callingId = Binder.clearCallingIdentity();
        try {
            String title = this.mContext.getResources().getString(51249792);
            String message = this.mContext.getResources().getString(51249791);
            if (this.mNotificationManager == null) {
                this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
            }
            Bundle bundle = new Bundle();
            bundle.putInt("vivo.summaryIconRes", 50464101);
            Notification notification = new Notification.Builder(this.mContext, SystemNotificationChannels.DEVELOPER).setSmallIcon(17301543).setWhen(0L).setOngoing(true).setTicker(title).setDefaults(0).setColor(this.mContext.getColor(17170460)).setContentTitle(title).setContentText(message).setContentIntent(null).setVisibility(1).setFlag(32, true).setExtras(bundle).setStyle(new Notification.BigTextStyle().bigText(message)).build();
            this.mNotificationManager.notifyAsUser(null, 891216, notification, new UserHandle(userHandle));
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    private void cancelDebugWarningNotification(int userHandle) {
        long callingId = Binder.clearCallingIdentity();
        try {
            if (this.mNotificationManager == null) {
                this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
            }
            this.mNotificationManager.cancelAsUser(null, 891216, new UserHandle(userHandle));
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    private void sendTestVersionNotification(int userHandle) {
        long callingId = Binder.clearCallingIdentity();
        try {
            String title = this.mContext.getResources().getString(51249796);
            String message = this.mContext.getResources().getString(51249795);
            if (this.mNotificationManager == null) {
                this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
            }
            Bundle bundle = new Bundle();
            bundle.putInt("vivo.summaryIconRes", 50464101);
            Notification notification = new Notification.Builder(this.mContext, SystemNotificationChannels.DEVELOPER).setSmallIcon(17301543).setWhen(0L).setOngoing(true).setTicker(title).setDefaults(0).setColor(this.mContext.getColor(17170460)).setContentTitle(title).setContentText(message).setContentIntent(null).setVisibility(1).setFlag(32, true).setExtras(bundle).build();
            this.mNotificationManager.notifyAsUser(null, 891217, notification, new UserHandle(userHandle));
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    public String getCustomShortName() {
        checkGetInfoPermission();
        return this.mCustomShortName;
    }

    public String getEmmShortName() {
        checkGetInfoPermission();
        return this.mEmmShortName;
    }

    public String getEmmFromCota() {
        return this.mEmmFromCota;
    }

    private boolean isSystemApp(ApplicationInfo appInfo) {
        return ((appInfo.flags & 1) == 0 && (appInfo.flags & 128) == 0 && appInfo.uid != 1000) ? false : true;
    }

    private boolean isVivoApp(String packageName) {
        try {
            if (this.mIPackageManager.checkSignatures(VivoPermissionUtils.OS_PKG, packageName) != 0 && this.mIPackageManager.checkSignatures("com.android.providers.contacts", packageName) != 0) {
                if (this.mIPackageManager.checkSignatures("com.android.providers.media", packageName) != 0) {
                    return false;
                }
            }
            return true;
        } catch (RemoteException e) {
            VLog.e(VIVO_LOG_TAG, "isVivoApp failed", e);
            return false;
        }
    }

    private boolean isVivoEmmUid(int uid) {
        int userId = UserHandle.getUserId(uid);
        VivoPolicyData policy = getVivoUserData(userId);
        if (policy.mEmmInfoMap != null && policy.mEmmInfoMap.size() > 0) {
            for (String pkg : policy.mEmmInfoMap.keySet()) {
                if (!TextUtils.isEmpty(pkg)) {
                    try {
                        if (uid == this.mContext.getPackageManager().getPackageUidAsUser(pkg, userId)) {
                            if (policy.mEmmBlackList != null && policy.mEmmBlackList.size() > 0 && policy.mEmmBlackList.contains(pkg)) {
                                VLog.e(VIVO_LOG_TAG, "isVivoEmmUid failed, " + pkg + " in BlackList");
                                return false;
                            } else if (policy.mEmmDisabledList != null && policy.mEmmDisabledList.size() > 0 && policy.mEmmDisabledList.contains(pkg)) {
                                VLog.e(VIVO_LOG_TAG, "isVivoEmmUid failed, " + pkg + " in DisabledList");
                                return false;
                            } else {
                                return true;
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }
        return false;
    }

    private boolean isCallingFromSystem() {
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        return callingUid == 1000 || callingUid == 0 || callingPid == Process.myPid();
    }

    private boolean isSystemUid(int callingUid) {
        if (callingUid > 1000 && callingUid < 9999) {
            VLog.d(VIVO_LOG_TAG, "isSystemUid uid=" + callingUid);
            return true;
        }
        return false;
    }

    private boolean checkGetInfoPermission() {
        int callingUid = Binder.getCallingUid();
        int userId = UserHandle.getUserId(callingUid);
        try {
        } catch (RemoteException e) {
            VLog.e(VIVO_LOG_TAG, "checkGetInfoPermission failed", e);
        }
        if (isCallingFromSystem() || isVivoEmmUid(callingUid) || isSystemUid(callingUid)) {
            return true;
        }
        String[] pkgs = this.mIPackageManager.getPackagesForUid(callingUid);
        if (pkgs == null || pkgs.length == 0) {
            VLog.e(VIVO_LOG_TAG, "checkGetInfoPermission failed: pkg not exist, callingUid=" + callingUid);
            throw new SecurityException("checkGetInfoPermission denied: should be available!");
        }
        for (int i = 0; i < pkgs.length; i++) {
            ApplicationInfo info = this.mIPackageManager.getApplicationInfo(pkgs[i], 0, userId);
            if ((info != null && isSystemApp(info)) || isVivoApp(pkgs[i])) {
                return true;
            }
        }
        throw new SecurityException("checkGetInfoPermission permission denied: call from uid=" + callingUid);
    }

    private boolean checkCallingFromVivoApp() {
        String[] pkgs;
        int callingUid = Binder.getCallingUid();
        UserHandle.getUserId(callingUid);
        try {
            pkgs = this.mIPackageManager.getPackagesForUid(callingUid);
        } catch (RemoteException e) {
            VLog.e(VIVO_LOG_TAG, "checkGetInfoPermission failed", e);
        }
        if (pkgs == null || pkgs.length == 0) {
            VLog.e(VIVO_LOG_TAG, "checkCallingFromVivoApp failed: pkg not exist, callingUid=" + callingUid);
            throw new SecurityException("checkCallingFromVivoApp denied: should be available!");
        }
        for (String str : pkgs) {
            if (isVivoApp(str)) {
                return true;
            }
        }
        throw new SecurityException("checkCallingFromVivoApp permission denied: call from uid=" + callingUid);
    }

    public boolean isEmmAPI() {
        return this.mIsEmmAPI;
    }

    public void setEmmBlackList(List<String> pkgs, boolean add, int userHandle) {
        checkCallingFromVivoApp();
        if (pkgs == null || pkgs.isEmpty()) {
            throw new IllegalArgumentException("Unknown EmmInfo");
        }
        synchronized (getLockObject()) {
            long callingId = Binder.clearCallingIdentity();
            try {
                VivoPolicyData policy = getVivoUserData(userHandle);
                new ArrayList();
                ComponentName vivoadmin = getVivoAdminUncheckedLocked(userHandle);
                for (String packageName : pkgs) {
                    if (add) {
                        if (!policy.mEmmBlackList.contains(packageName)) {
                            policy.mEmmBlackList.add(packageName);
                        }
                        if (vivoadmin != null && packageName != null && packageName.equals(vivoadmin.getPackageName()) && add) {
                            setVivoAdmin(vivoadmin, false, userHandle);
                        }
                    } else {
                        policy.mEmmBlackList.remove(packageName);
                    }
                }
                saveEmmInfo(policy, userHandle);
                Binder.restoreCallingIdentity(callingId);
            } catch (Exception e) {
                VLog.e(VIVO_LOG_TAG, "setEmmBlackList exception " + e);
                Binder.restoreCallingIdentity(callingId);
            }
        }
    }

    public List<String> getEmmBlackList(int userHandle) {
        List<String> pkgList;
        checkGetInfoPermission();
        synchronized (getLockObject()) {
            VivoPolicyData policy = getVivoUserData(userHandle);
            pkgList = policy.mEmmBlackList;
        }
        return pkgList;
    }

    public boolean setEmmDisabledList(List<String> pkgs, boolean isAdd, int userHandle) {
        boolean result;
        List<String> pkgAdded;
        String packageName;
        PackageInfo packageInfo;
        checkCallingFromVivoApp();
        if (pkgs == null || pkgs.isEmpty()) {
            throw new IllegalArgumentException("setEmmDisabledList : Unknown EmmInfo");
        }
        boolean result2 = false;
        synchronized (getLockObject()) {
            try {
                long callingId = Binder.clearCallingIdentity();
                try {
                    try {
                        VivoPolicyData policy = getVivoUserData(userHandle);
                        List<String> pkgAdded2 = new ArrayList<>();
                        ComponentName vivoadmin = getVivoAdminUncheckedLocked(userHandle);
                        result = false;
                        for (String packageName2 : pkgs) {
                            try {
                                try {
                                    try {
                                        packageInfo = this.mIPackageManager.getPackageInfo(packageName2, 0, userHandle);
                                    } catch (Exception e) {
                                        e = e;
                                        pkgAdded = pkgAdded2;
                                        packageName = packageName2;
                                    }
                                    if (packageInfo == null) {
                                        try {
                                            VLog.i(VIVO_LOG_TAG, "setEmmDisabledList packageName" + packageName2 + " is not exist");
                                            pkgAdded = pkgAdded2;
                                        } catch (Exception e2) {
                                            e = e2;
                                            pkgAdded = pkgAdded2;
                                            packageName = packageName2;
                                        }
                                        pkgAdded2 = pkgAdded;
                                    } else {
                                        if (vivoadmin != null && packageName2 != null && packageName2.equals(vivoadmin.getPackageName()) && isAdd) {
                                            setVivoAdmin(vivoadmin, false, userHandle);
                                        }
                                        pkgAdded = pkgAdded2;
                                        packageName = packageName2;
                                        try {
                                            this.mIPackageManager.setApplicationEnabledSetting(packageName2, isAdd ? 2 : 1, 0, userHandle, this.mContext.getPackageName());
                                            if (!isAdd) {
                                                policy.mEmmDisabledList.remove(packageName);
                                            } else if (!policy.mEmmDisabledList.contains(packageName)) {
                                                policy.mEmmDisabledList.add(packageName);
                                            }
                                            result = true;
                                            pkgAdded2 = pkgAdded;
                                        } catch (Exception e3) {
                                            e = e3;
                                        }
                                    }
                                    VLog.e(VIVO_LOG_TAG, "setEmmDisabledList packageNames = " + packageName + " failed! " + e);
                                    pkgAdded2 = pkgAdded;
                                } catch (Throwable th) {
                                    th = th;
                                    Binder.restoreCallingIdentity(callingId);
                                    throw th;
                                }
                            } catch (Exception e4) {
                                e = e4;
                                result2 = result;
                                VLog.e(VIVO_LOG_TAG, "setEmmDisabledList exception " + e);
                                Binder.restoreCallingIdentity(callingId);
                                return result2;
                            }
                        }
                        saveEmmInfo(policy, userHandle);
                    } catch (Exception e5) {
                        e = e5;
                    }
                    try {
                        Binder.restoreCallingIdentity(callingId);
                        result2 = result;
                        return result2;
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                }
            } catch (Throwable th4) {
                th = th4;
            }
        }
    }

    public List<String> getEmmDisabledList(int userHandle) {
        List<String> pkgList;
        checkGetInfoPermission();
        synchronized (getLockObject()) {
            VivoPolicyData policy = getVivoUserData(userHandle);
            pkgList = policy.mEmmDisabledList;
        }
        return pkgList;
    }

    public void setEmmPackage(String packageName, Bundle info, boolean add, int userHandle) {
        if (!isCallingFromSystem()) {
            throw new SecurityException("setEmmPackage permission denied: call from uid=" + Binder.getCallingUid());
        } else if (packageName == null) {
            throw new IllegalArgumentException("Unknown EmmInfo");
        } else {
            synchronized (getLockObject()) {
                long callingId = Binder.clearCallingIdentity();
                try {
                    VivoPolicyData policy = getVivoUserData(userHandle);
                    if (policy.mEmmInfoMap.containsKey(packageName)) {
                        policy.mEmmInfoMap.remove(packageName);
                    }
                    if (add) {
                        if (info != null) {
                            String shortName = info.getString("emm_custom_short_name_string");
                            ArrayList<String> relatedPkgs = info.getStringArrayList("emm_related_pkg_list");
                            ArrayList<String> emmPermissions = info.getStringArrayList("emm_permission_list");
                            VivoEmmInfo emmInfo = new VivoEmmInfo();
                            emmInfo.setVivoEmmInfo(shortName, packageName, relatedPkgs, emmPermissions);
                            policy.mEmmInfoMap.put(packageName, emmInfo);
                            VLog.i(VIVO_LOG_TAG, "setEmmPackage packageName = " + packageName + ", shortName = " + shortName + ", relatedPkgs = " + relatedPkgs + ", emmPermissions = " + emmPermissions);
                        }
                    } else if (hasVivoActiveAdmin(userHandle) && packageName.equals(getVivoAdmin(userHandle).getPackageName())) {
                        setVivoAdmin(getVivoAdmin(userHandle), false, userHandle);
                    }
                    saveEmmInfo(policy, userHandle);
                    updateCustomType(true);
                } catch (Exception e) {
                    VLog.e(VIVO_LOG_TAG, "setEmmPackage exception " + e);
                }
                Binder.restoreCallingIdentity(callingId);
            }
        }
    }

    public List<String> getEmmPackage(int userHandle) {
        checkGetInfoPermission();
        List<String> pkgList = new ArrayList<>();
        synchronized (getLockObject()) {
            long callingId = Binder.clearCallingIdentity();
            try {
                VivoPolicyData policy = getVivoUserData(userHandle);
                if (policy.mEmmInfoMap != null && policy.mEmmInfoMap.size() > 0) {
                    pkgList.addAll(policy.mEmmInfoMap.keySet());
                }
                Binder.restoreCallingIdentity(callingId);
            } catch (Exception e) {
                VLog.e(VIVO_LOG_TAG, "getEmmPackage exception " + e);
                Binder.restoreCallingIdentity(callingId);
                return null;
            }
        }
        return pkgList;
    }

    private void loadEmmInfo(VivoPolicyData policy, int userHandle) {
        XmlPullParser parser;
        String tag;
        VLog.v(VIVO_LOG_TAG, "loadEmmInfo");
        String base = new File(getVivoPolicyFileDirectory(userHandle), VivoUtils.VIVO_EMM_INFO_XML).getAbsolutePath();
        File file = new File(base);
        JournaledFile journal = new JournaledFile(file, new File(base + ".tmp"));
        FileInputStream stream = null;
        File file2 = journal.chooseForRead();
        try {
            stream = new FileInputStream(file2);
            parser = Xml.newPullParser();
            parser.setInput(stream, StandardCharsets.UTF_8.name());
            while (true) {
                int type = parser.next();
                if (type == 1 || type == 2) {
                    break;
                }
            }
            tag = parser.getName();
        } catch (FileNotFoundException e) {
            VLog.v(VIVO_LOG_TAG, "vivo_emm_info.xml not found, Let's check emm_list.json");
            try {
                stream = new FileInputStream("/data/custom/emm_list.json");
                JsonReader reader = new JsonReader(new InputStreamReader(stream));
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (name.equals("blacklist")) {
                        reader.beginArray();
                        while (reader.hasNext()) {
                            policy.mEmmBlackList.add(reader.nextString());
                        }
                        reader.endArray();
                    } else if (name.equals("disablelist")) {
                        reader.beginArray();
                        while (reader.hasNext()) {
                            policy.mEmmDisabledList.add(reader.nextString());
                        }
                        reader.endArray();
                    }
                }
                reader.endObject();
                if (policy.mEmmBlackList.size() > 0 || policy.mEmmDisabledList.size() > 0) {
                    saveEmmInfo(policy, userHandle);
                }
            } catch (Exception ex) {
                VLog.v(VIVO_LOG_TAG, "emm_list.json open fail! " + ex);
            }
        } catch (Exception e2) {
            VLog.e(VIVO_LOG_TAG, "failed parsing ", e2);
        }
        if (!"emmInfos".equals(tag)) {
            throw new XmlPullParserException("Settings do not start with emmInfos tag: found " + tag);
        }
        parser.next();
        int outerDepth = parser.getDepth();
        policy.mEmmInfoMap.clear();
        policy.mEmmBlackList.clear();
        policy.mEmmDisabledList.clear();
        policy.mPowerExceptionInfolist.clear();
        policy.mAlarmExceptionInfolist.clear();
        policy.mLocationExceptionInfolist.clear();
        policy.mCrashExceptionInfolist.clear();
        policy.mWakeLockExceptionInfolist.clear();
        while (true) {
            int type2 = parser.next();
            if (type2 == 1 || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                break;
            } else if (type2 != 3 && type2 != 4) {
                String tag2 = parser.getName();
                if ("emm".equals(tag2)) {
                    VivoEmmInfo info = new VivoEmmInfo();
                    info.readInfoFromXml(parser);
                    policy.mEmmInfoMap.put(info.getEmmPackageName(), info);
                } else if ("emm-b-app".equals(tag2)) {
                    policy.mEmmBlackList.add(parser.getAttributeValue(null, ATTR_VALUE));
                } else if ("emm-disabled-app".equals(tag2)) {
                    policy.mEmmDisabledList.add(parser.getAttributeValue(null, ATTR_VALUE));
                } else if ("power-exception-info-list".equals(tag2)) {
                    policy.mPowerExceptionInfolist.add(parser.getAttributeValue(null, ATTR_VALUE));
                } else if ("alarm-exception-info-list".equals(tag2)) {
                    policy.mAlarmExceptionInfolist.add(parser.getAttributeValue(null, ATTR_VALUE));
                } else if ("location-exception-info-list".equals(tag2)) {
                    policy.mLocationExceptionInfolist.add(parser.getAttributeValue(null, ATTR_VALUE));
                } else if ("crash-exception-info-list".equals(tag2)) {
                    policy.mCrashExceptionInfolist.add(parser.getAttributeValue(null, ATTR_VALUE));
                } else if ("wakelock-exception-info-list".equals(tag2)) {
                    policy.mWakeLockExceptionInfolist.add(parser.getAttributeValue(null, ATTR_VALUE));
                }
            }
        }
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e3) {
            }
        }
    }

    private void saveEmmInfo(VivoPolicyData policy, int userHandle) {
        VLog.v(VIVO_LOG_TAG, "saveEmmInfo");
        String base = new File(getVivoPolicyFileDirectory(userHandle), VivoUtils.VIVO_EMM_INFO_XML).getAbsolutePath();
        File file = new File(base);
        JournaledFile journal = new JournaledFile(file, new File(base + ".tmp"));
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(journal.chooseForWrite(), false);
            XmlSerializer out = new FastXmlSerializer();
            out.setOutput(stream, StandardCharsets.UTF_8.name());
            out.startDocument(null, true);
            out.startTag(null, "emmInfos");
            if (policy.mEmmInfoMap != null && policy.mEmmInfoMap.size() > 0) {
                for (VivoEmmInfo info : policy.mEmmInfoMap.values()) {
                    if (info != null) {
                        info.writeInfoToXml(out);
                    }
                }
            }
            if (policy.mEmmBlackList != null) {
                try {
                    if (policy.mEmmBlackList.size() > 0) {
                        for (Iterator<String> it = policy.mEmmBlackList.iterator(); it.hasNext(); it = it) {
                            String s = it.next();
                            out.startTag(null, "emm-b-app");
                            out.attribute(null, ATTR_VALUE, s);
                            out.endTag(null, "emm-b-app");
                        }
                    }
                } catch (IOException e) {
                    e = e;
                    VLog.w(VIVO_LOG_TAG, "failed writing file", e);
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e2) {
                        }
                    }
                    journal.rollback();
                    return;
                }
            }
            if (policy.mEmmDisabledList != null && policy.mEmmDisabledList.size() > 0) {
                for (String s2 : policy.mEmmDisabledList) {
                    out.startTag(null, "emm-disabled-app");
                    out.attribute(null, ATTR_VALUE, s2);
                    out.endTag(null, "emm-disabled-app");
                }
            }
            if (policy.mPowerExceptionInfolist != null && policy.mPowerExceptionInfolist.size() > 0) {
                for (String s3 : policy.mPowerExceptionInfolist) {
                    out.startTag(null, "power-exception-info-list");
                    out.attribute(null, ATTR_VALUE, s3);
                    out.endTag(null, "power-exception-info-list");
                }
            }
            if (policy.mAlarmExceptionInfolist != null && policy.mAlarmExceptionInfolist.size() > 0) {
                for (String s4 : policy.mAlarmExceptionInfolist) {
                    out.startTag(null, "alarm-exception-info-list");
                    out.attribute(null, ATTR_VALUE, s4);
                    out.endTag(null, "alarm-exception-info-list");
                }
            }
            if (policy.mLocationExceptionInfolist != null && policy.mLocationExceptionInfolist.size() > 0) {
                for (String s5 : policy.mLocationExceptionInfolist) {
                    out.startTag(null, "location-exception-info-list");
                    out.attribute(null, ATTR_VALUE, s5);
                    out.endTag(null, "location-exception-info-list");
                }
            }
            if (policy.mCrashExceptionInfolist != null && policy.mCrashExceptionInfolist.size() > 0) {
                for (String s6 : policy.mCrashExceptionInfolist) {
                    out.startTag(null, "crash-exception-info-list");
                    out.attribute(null, ATTR_VALUE, s6);
                    out.endTag(null, "crash-exception-info-list");
                }
            }
            if (policy.mWakeLockExceptionInfolist != null && policy.mWakeLockExceptionInfolist.size() > 0) {
                for (String s7 : policy.mWakeLockExceptionInfolist) {
                    out.startTag(null, "wakelock-exception-info-list");
                    out.attribute(null, ATTR_VALUE, s7);
                    out.endTag(null, "wakelock-exception-info-list");
                }
            }
            out.endTag(null, "emmInfos");
            out.endDocument();
            stream.flush();
            FileUtils.sync(stream);
            stream.close();
            journal.commit();
            sendChangedNotification(-1, userHandle);
        } catch (IOException e3) {
            e = e3;
        }
    }

    private List<String> getEmmRelatedPkgs(String packageName, int userHandle) {
        List<String> relatedList;
        List<String> pkgList = new ArrayList<>();
        synchronized (getLockObject()) {
            try {
                try {
                    VivoPolicyData policy = getVivoUserData(userHandle);
                    if (policy.mEmmInfoMap != null && policy.mEmmInfoMap.size() > 0 && (relatedList = policy.mEmmInfoMap.get(packageName).getRelatedPkgs()) != null && relatedList.size() > 0) {
                        pkgList.addAll(relatedList);
                    }
                } catch (Exception e) {
                    VLog.e(VIVO_LOG_TAG, "getEmmRelatedPkgs exception " + e);
                    return null;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return pkgList;
    }

    public List<String> getCustomPkgs() {
        checkGetInfoPermission();
        List<String> pkgList = new ArrayList<>();
        int callingUid = Binder.getCallingUid();
        int userId = UserHandle.getUserId(callingUid);
        try {
            VivoPolicyData policy = getVivoUserData(userId);
            if (policy.mEmmInfoMap != null && policy.mEmmInfoMap.size() > 0) {
                for (String pkg : policy.mEmmInfoMap.keySet()) {
                    if (!TextUtils.isEmpty(pkg)) {
                        pkgList.add(pkg);
                        List<String> relatedList = policy.mEmmInfoMap.get(pkg).getRelatedPkgs();
                        if (relatedList != null && relatedList.size() > 0) {
                            pkgList.addAll(relatedList);
                        }
                    }
                }
            } else if (this.mPersistentListPkgs != null) {
                pkgList.addAll(this.mPersistentListPkgs);
            }
            return pkgList;
        } catch (Exception e) {
            VLog.e(VIVO_LOG_TAG, "getCustomPkgs exception " + e);
            return null;
        }
    }

    private List<String> getEmmPermissions(String packageName, int userHandle) {
        List<String> permList = new ArrayList<>();
        synchronized (getLockObject()) {
            try {
                try {
                    VivoPolicyData policy = getVivoUserData(userHandle);
                    if (policy.mEmmInfoMap != null && policy.mEmmInfoMap.size() > 0) {
                        permList.addAll(policy.mEmmInfoMap.get(packageName).getEmmPermissions());
                    }
                } catch (Exception e) {
                    VLog.e(VIVO_LOG_TAG, "getEmmPermissions exception " + e);
                    return null;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return permList;
    }

    private boolean isUidInEmmBlackAndDisableList(int uid) {
        int i;
        try {
            String[] pkgs = this.mIPackageManager.getPackagesForUid(uid);
            if (pkgs == null || pkgs.length == 0) {
                VLog.e(VIVO_LOG_TAG, "isUidInEmmBlackAndDisableList failed: pkg not exist, uid=" + uid);
                throw new SecurityException("isUidInEmmBlackAndDisableList denied: should be available!");
            }
            VivoPolicyData policy = getVivoUserData(0);
            while (i < pkgs.length) {
                i = (policy.mEmmBlackList.contains(pkgs[i]) || policy.mEmmDisabledList.contains(pkgs[i])) ? 0 : i + 1;
                return true;
            }
            return false;
        } catch (RemoteException e) {
            VLog.e(VIVO_LOG_TAG, "checkGetInfoPermission failed", e);
            return false;
        }
    }

    public boolean setVivoAdmin(ComponentName adminReceiver, boolean enable, int userHandle) {
        long callingId;
        if (SystemProperties.get("persist.sys.factory.mode", "no").equals("yes")) {
            VLog.v(VIVO_LOG_TAG, "setVivoAdmin fail, is factory mode");
            return false;
        }
        synchronized (getLockObject()) {
            try {
                int callingUid = Binder.getCallingUid();
                if (!isVivoEmmUid(callingUid) && !isCallingFromSystem()) {
                    try {
                        ApplicationInfo info = this.mIPackageManager.getApplicationInfo(CUSTOM_TOOL_NAME, 0, userHandle);
                        if (info == null || callingUid != this.mContext.getPackageManager().getPackageUidAsUser(CUSTOM_TOOL_NAME, userHandle) || !isSystemApp(info)) {
                            throw new SecurityException("setVivoAdmin permission denied: call from uid=" + callingUid);
                        }
                    } catch (Exception e) {
                        throw new SecurityException("setVivoAdmin permission denied: call from uid=" + callingUid);
                    }
                }
                if (enable) {
                    if (!this.mIsRemoving) {
                        if (isUidInEmmBlackAndDisableList(callingUid)) {
                            VLog.v(VIVO_LOG_TAG, "setVivoAdmin fail, cause it is in blacklist");
                            return false;
                        } else if (hasVivoActiveAdmin(userHandle)) {
                            VLog.v(VIVO_LOG_TAG, "user " + userHandle + " already has Vadmin");
                            return false;
                        } else {
                            callingId = Binder.clearCallingIdentity();
                            try {
                                this.mService.setActiveAdmin(adminReceiver, true, userHandle);
                                Binder.restoreCallingIdentity(callingId);
                                setVivoAdmin(adminReceiver, userHandle);
                                if (this.mCustomType == 1) {
                                    sendDebugWarningNotification(userHandle);
                                }
                                return true;
                            } finally {
                            }
                        }
                    }
                } else if (!this.mIsRemoving) {
                    this.mIsRemoving = true;
                    this.mIsEmmAPI = true;
                    try {
                        if (this.mService.isDeviceOwner(adminReceiver, userHandle)) {
                            this.mService.clearDeviceOwner(adminReceiver.getPackageName());
                        } else if (this.mService.isProfileOwner(adminReceiver, userHandle)) {
                            this.mService.clearProfileOwner(adminReceiver);
                        } else {
                            removeVivoAdmin(adminReceiver, userHandle);
                            callingId = Binder.clearCallingIdentity();
                            try {
                                this.mService.clearVivoAdminData(adminReceiver, userHandle);
                                this.mService.removeActiveAdmin(adminReceiver, userHandle);
                            } finally {
                            }
                        }
                        if (this.mCustomType == 1) {
                            cancelDebugWarningNotification(userHandle);
                        }
                        return true;
                    } catch (Exception e2) {
                        VLog.e(VIVO_LOG_TAG, "setVivoAdmin " + e2);
                        return false;
                    } finally {
                        this.mIsEmmAPI = false;
                        this.mIsRemoving = false;
                    }
                }
            } catch (Exception e3) {
                VLog.e(VIVO_LOG_TAG, "setVivoAdmin failed! " + e3);
                e3.printStackTrace();
            }
            return false;
        }
    }

    public ComponentName getVivoAdmin(int userHandle) {
        checkGetInfoPermission();
        return getVivoAdminUncheckedLocked(userHandle);
    }

    public ComponentName getVivoAdminUncheckedLocked(int userHandle) {
        synchronized (getLockObject()) {
            VivoPolicyData policy = getVivoUserData(userHandle);
            if (policy.mVivoActiveAdmin != null) {
                return policy.mVivoActiveAdmin.mInfo.getComponent();
            }
            return null;
        }
    }

    public void setVivoAdmin(ComponentName adminReceiver, int userHandle) {
        synchronized (getLockObject()) {
            long callingId = Binder.clearCallingIdentity();
            VLog.v(VIVO_LOG_TAG, "setAdmin adminReceiver = " + adminReceiver + ", user " + userHandle);
            if (getVivoActiveAdminUncheckedLocked(adminReceiver, userHandle) != null) {
                VLog.v(VIVO_LOG_TAG, "setAdmin adminReceiver is already admin");
                Binder.restoreCallingIdentity(callingId);
                return;
            }
            VivoAdminInfo info = findVivoAdmin(adminReceiver, userHandle);
            VivoPolicyData policy = getVivoUserData(userHandle);
            policy.mVivoActiveAdmin = new VivoActiveAdmin(info);
            saveVivoSettingsLocked(0, userHandle);
            this.mUserManagerInternal.setDeviceManaged(true);
            Binder.restoreCallingIdentity(callingId);
        }
    }

    public void removeVivoAdmin(ComponentName adminReceiver, int userHandle) {
        VivoPolicyData policy = getVivoUserData(userHandle);
        VivoActiveAdmin admin = getVivoActiveAdminUncheckedLocked(adminReceiver, userHandle);
        if (admin == null) {
            return;
        }
        VLog.v(VIVO_LOG_TAG, "removeAdmin adminReceiver = " + adminReceiver + ", user " + userHandle);
        removeOtherPolicy(adminReceiver, userHandle);
        synchronized (getLockObject()) {
            long callingId = Binder.clearCallingIdentity();
            policy.mVivoActiveAdmin = null;
            saveVivoSettingsLocked(0, userHandle);
            this.mUserManagerInternal.setDeviceManaged(false);
            Binder.restoreCallingIdentity(callingId);
        }
    }

    /* JADX WARN: Can't wrap try/catch for region: R(15:5|(1:7)|8|9|10|11|(24:(3:221|222|(34:224|(9:227|228|229|230|(3:232|233|(2:263|251))(1:270)|238|(3:260|261|262)(9:240|241|242|243|244|246|(1:248)|249|250)|251|225)|274|275|14|15|16|17|18|(1:20)|26|27|(3:150|151|(28:153|154|155|156|157|(4:160|(8:162|163|164|165|(7:168|169|170|171|(2:173|174)(1:176)|175|166)|181|182|(2:184|185)(1:187))(2:191|192)|186|158)|193|194|30|31|(3:35|(7:38|39|40|41|43|44|36)|48)|49|(1:51)|52|(4:54|(1:56)|57|(1:59))|60|(3:64|(4:67|(4:71|(1:73)|74|75)|76|65)|79)|80|(3:84|(4:87|(2:97|(2:104|(3:106|107|109)(1:113))(3:114|115|116))|110|85)|127)|134|135|136|137|(1:139)|140|(1:142)|132|133))|29|30|31|(4:33|35|(1:36)|48)|49|(0)|52|(0)|60|(4:62|64|(1:65)|79)|80|(4:82|84|(1:85)|127)|134|135|136|137|(0)|140|(0)|132|133))|27|(0)|29|30|31|(0)|49|(0)|52|(0)|60|(0)|80|(0)|134|135|136|137|(0)|140|(0)|132|133)|13|14|15|16|17|18|(0)|26) */
    /* JADX WARN: Can't wrap try/catch for region: R(24:(3:221|222|(34:224|(9:227|228|229|230|(3:232|233|(2:263|251))(1:270)|238|(3:260|261|262)(9:240|241|242|243|244|246|(1:248)|249|250)|251|225)|274|275|14|15|16|17|18|(1:20)|26|27|(3:150|151|(28:153|154|155|156|157|(4:160|(8:162|163|164|165|(7:168|169|170|171|(2:173|174)(1:176)|175|166)|181|182|(2:184|185)(1:187))(2:191|192)|186|158)|193|194|30|31|(3:35|(7:38|39|40|41|43|44|36)|48)|49|(1:51)|52|(4:54|(1:56)|57|(1:59))|60|(3:64|(4:67|(4:71|(1:73)|74|75)|76|65)|79)|80|(3:84|(4:87|(2:97|(2:104|(3:106|107|109)(1:113))(3:114|115|116))|110|85)|127)|134|135|136|137|(1:139)|140|(1:142)|132|133))|29|30|31|(4:33|35|(1:36)|48)|49|(0)|52|(0)|60|(4:62|64|(1:65)|79)|80|(4:82|84|(1:85)|127)|134|135|136|137|(0)|140|(0)|132|133))|27|(0)|29|30|31|(0)|49|(0)|52|(0)|60|(0)|80|(0)|134|135|136|137|(0)|140|(0)|132|133) */
    /* JADX WARN: Can't wrap try/catch for region: R(38:5|(1:7)|8|9|10|11|(3:221|222|(34:224|(9:227|228|229|230|(3:232|233|(2:263|251))(1:270)|238|(3:260|261|262)(9:240|241|242|243|244|246|(1:248)|249|250)|251|225)|274|275|14|15|16|17|18|(1:20)|26|27|(3:150|151|(28:153|154|155|156|157|(4:160|(8:162|163|164|165|(7:168|169|170|171|(2:173|174)(1:176)|175|166)|181|182|(2:184|185)(1:187))(2:191|192)|186|158)|193|194|30|31|(3:35|(7:38|39|40|41|43|44|36)|48)|49|(1:51)|52|(4:54|(1:56)|57|(1:59))|60|(3:64|(4:67|(4:71|(1:73)|74|75)|76|65)|79)|80|(3:84|(4:87|(2:97|(2:104|(3:106|107|109)(1:113))(3:114|115|116))|110|85)|127)|134|135|136|137|(1:139)|140|(1:142)|132|133))|29|30|31|(4:33|35|(1:36)|48)|49|(0)|52|(0)|60|(4:62|64|(1:65)|79)|80|(4:82|84|(1:85)|127)|134|135|136|137|(0)|140|(0)|132|133))|13|14|15|16|17|18|(0)|26|27|(0)|29|30|31|(0)|49|(0)|52|(0)|60|(0)|80|(0)|134|135|136|137|(0)|140|(0)|132|133) */
    /* JADX WARN: Code restructure failed: missing block: B:197:0x03c8, code lost:
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:199:0x03ca, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:201:0x03ce, code lost:
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:203:0x03d2, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:205:0x03d7, code lost:
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:59:0x0128, code lost:
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:63:0x0130, code lost:
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:64:0x0131, code lost:
        r9 = false;
     */
    /* JADX WARN: Code restructure failed: missing block: B:65:0x0132, code lost:
        com.vivo.common.utils.VLog.e(com.android.server.devicepolicy.VivoCustomDpmsImpl.VIVO_LOG_TAG, "removeOtherPolicy setApplicationEnabledSetting doubleinstance failed! " + r0);
     */
    /* JADX WARN: Removed duplicated region for block: B:114:0x022d A[Catch: all -> 0x0269, Exception -> 0x026e, TRY_ENTER, TryCatch #1 {Exception -> 0x026e, blocks: (B:105:0x0206, B:114:0x022d, B:116:0x0235, B:117:0x023b, B:119:0x0241, B:124:0x0254, B:132:0x0277, B:135:0x0287, B:137:0x028b, B:138:0x0298, B:140:0x029c, B:143:0x02ad, B:145:0x02b5, B:146:0x02c3, B:148:0x02c9, B:150:0x02d1, B:152:0x02dc, B:154:0x02e7, B:155:0x02f3, B:156:0x02f8, B:160:0x0317, B:162:0x031f, B:163:0x032a, B:165:0x0330, B:168:0x0348, B:170:0x034c, B:172:0x0356, B:175:0x035f, B:177:0x0363, B:179:0x036d, B:182:0x0376), top: B:219:0x0206 }] */
    /* JADX WARN: Removed duplicated region for block: B:119:0x0241 A[Catch: all -> 0x0269, Exception -> 0x026e, TRY_LEAVE, TryCatch #1 {Exception -> 0x026e, blocks: (B:105:0x0206, B:114:0x022d, B:116:0x0235, B:117:0x023b, B:119:0x0241, B:124:0x0254, B:132:0x0277, B:135:0x0287, B:137:0x028b, B:138:0x0298, B:140:0x029c, B:143:0x02ad, B:145:0x02b5, B:146:0x02c3, B:148:0x02c9, B:150:0x02d1, B:152:0x02dc, B:154:0x02e7, B:155:0x02f3, B:156:0x02f8, B:160:0x0317, B:162:0x031f, B:163:0x032a, B:165:0x0330, B:168:0x0348, B:170:0x034c, B:172:0x0356, B:175:0x035f, B:177:0x0363, B:179:0x036d, B:182:0x0376), top: B:219:0x0206 }] */
    /* JADX WARN: Removed duplicated region for block: B:132:0x0277 A[Catch: all -> 0x0269, Exception -> 0x026e, TRY_ENTER, TRY_LEAVE, TryCatch #1 {Exception -> 0x026e, blocks: (B:105:0x0206, B:114:0x022d, B:116:0x0235, B:117:0x023b, B:119:0x0241, B:124:0x0254, B:132:0x0277, B:135:0x0287, B:137:0x028b, B:138:0x0298, B:140:0x029c, B:143:0x02ad, B:145:0x02b5, B:146:0x02c3, B:148:0x02c9, B:150:0x02d1, B:152:0x02dc, B:154:0x02e7, B:155:0x02f3, B:156:0x02f8, B:160:0x0317, B:162:0x031f, B:163:0x032a, B:165:0x0330, B:168:0x0348, B:170:0x034c, B:172:0x0356, B:175:0x035f, B:177:0x0363, B:179:0x036d, B:182:0x0376), top: B:219:0x0206 }] */
    /* JADX WARN: Removed duplicated region for block: B:135:0x0287 A[Catch: all -> 0x0269, Exception -> 0x026e, TRY_ENTER, TryCatch #1 {Exception -> 0x026e, blocks: (B:105:0x0206, B:114:0x022d, B:116:0x0235, B:117:0x023b, B:119:0x0241, B:124:0x0254, B:132:0x0277, B:135:0x0287, B:137:0x028b, B:138:0x0298, B:140:0x029c, B:143:0x02ad, B:145:0x02b5, B:146:0x02c3, B:148:0x02c9, B:150:0x02d1, B:152:0x02dc, B:154:0x02e7, B:155:0x02f3, B:156:0x02f8, B:160:0x0317, B:162:0x031f, B:163:0x032a, B:165:0x0330, B:168:0x0348, B:170:0x034c, B:172:0x0356, B:175:0x035f, B:177:0x0363, B:179:0x036d, B:182:0x0376), top: B:219:0x0206 }] */
    /* JADX WARN: Removed duplicated region for block: B:143:0x02ad A[Catch: all -> 0x0269, Exception -> 0x026e, TRY_ENTER, TryCatch #1 {Exception -> 0x026e, blocks: (B:105:0x0206, B:114:0x022d, B:116:0x0235, B:117:0x023b, B:119:0x0241, B:124:0x0254, B:132:0x0277, B:135:0x0287, B:137:0x028b, B:138:0x0298, B:140:0x029c, B:143:0x02ad, B:145:0x02b5, B:146:0x02c3, B:148:0x02c9, B:150:0x02d1, B:152:0x02dc, B:154:0x02e7, B:155:0x02f3, B:156:0x02f8, B:160:0x0317, B:162:0x031f, B:163:0x032a, B:165:0x0330, B:168:0x0348, B:170:0x034c, B:172:0x0356, B:175:0x035f, B:177:0x0363, B:179:0x036d, B:182:0x0376), top: B:219:0x0206 }] */
    /* JADX WARN: Removed duplicated region for block: B:148:0x02c9 A[Catch: all -> 0x0269, Exception -> 0x026e, TryCatch #1 {Exception -> 0x026e, blocks: (B:105:0x0206, B:114:0x022d, B:116:0x0235, B:117:0x023b, B:119:0x0241, B:124:0x0254, B:132:0x0277, B:135:0x0287, B:137:0x028b, B:138:0x0298, B:140:0x029c, B:143:0x02ad, B:145:0x02b5, B:146:0x02c3, B:148:0x02c9, B:150:0x02d1, B:152:0x02dc, B:154:0x02e7, B:155:0x02f3, B:156:0x02f8, B:160:0x0317, B:162:0x031f, B:163:0x032a, B:165:0x0330, B:168:0x0348, B:170:0x034c, B:172:0x0356, B:175:0x035f, B:177:0x0363, B:179:0x036d, B:182:0x0376), top: B:219:0x0206 }] */
    /* JADX WARN: Removed duplicated region for block: B:160:0x0317 A[Catch: all -> 0x0269, Exception -> 0x026e, TRY_ENTER, TryCatch #1 {Exception -> 0x026e, blocks: (B:105:0x0206, B:114:0x022d, B:116:0x0235, B:117:0x023b, B:119:0x0241, B:124:0x0254, B:132:0x0277, B:135:0x0287, B:137:0x028b, B:138:0x0298, B:140:0x029c, B:143:0x02ad, B:145:0x02b5, B:146:0x02c3, B:148:0x02c9, B:150:0x02d1, B:152:0x02dc, B:154:0x02e7, B:155:0x02f3, B:156:0x02f8, B:160:0x0317, B:162:0x031f, B:163:0x032a, B:165:0x0330, B:168:0x0348, B:170:0x034c, B:172:0x0356, B:175:0x035f, B:177:0x0363, B:179:0x036d, B:182:0x0376), top: B:219:0x0206 }] */
    /* JADX WARN: Removed duplicated region for block: B:165:0x0330 A[Catch: all -> 0x0269, Exception -> 0x026e, TryCatch #1 {Exception -> 0x026e, blocks: (B:105:0x0206, B:114:0x022d, B:116:0x0235, B:117:0x023b, B:119:0x0241, B:124:0x0254, B:132:0x0277, B:135:0x0287, B:137:0x028b, B:138:0x0298, B:140:0x029c, B:143:0x02ad, B:145:0x02b5, B:146:0x02c3, B:148:0x02c9, B:150:0x02d1, B:152:0x02dc, B:154:0x02e7, B:155:0x02f3, B:156:0x02f8, B:160:0x0317, B:162:0x031f, B:163:0x032a, B:165:0x0330, B:168:0x0348, B:170:0x034c, B:172:0x0356, B:175:0x035f, B:177:0x0363, B:179:0x036d, B:182:0x0376), top: B:219:0x0206 }] */
    /* JADX WARN: Removed duplicated region for block: B:192:0x03b6 A[Catch: Exception -> 0x03c8, all -> 0x03ff, TryCatch #14 {all -> 0x03ff, blocks: (B:211:0x03e3, B:190:0x03a8, B:192:0x03b6, B:193:0x03bf, B:195:0x03c4), top: B:235:0x002e }] */
    /* JADX WARN: Removed duplicated region for block: B:195:0x03c4 A[Catch: Exception -> 0x03c8, all -> 0x03ff, TRY_LEAVE, TryCatch #14 {all -> 0x03ff, blocks: (B:211:0x03e3, B:190:0x03a8, B:192:0x03b6, B:193:0x03bf, B:195:0x03c4), top: B:235:0x002e }] */
    /* JADX WARN: Removed duplicated region for block: B:233:0x014a A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:57:0x0116 A[Catch: Exception -> 0x0128, all -> 0x012a, TRY_LEAVE, TryCatch #3 {all -> 0x012a, blocks: (B:35:0x009b, B:37:0x00a8, B:43:0x00ce, B:53:0x010b, B:55:0x0110, B:57:0x0116), top: B:222:0x009b }] */
    /* JADX WARN: Type inference failed for: r15v17 */
    /* JADX WARN: Type inference failed for: r15v24, types: [int, boolean] */
    /* JADX WARN: Type inference failed for: r15v25 */
    /* JADX WARN: Type inference failed for: r15v27 */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private void removeOtherPolicy(android.content.ComponentName r30, int r31) {
        /*
            Method dump skipped, instructions count: 1028
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.devicepolicy.VivoCustomDpmsImpl.removeOtherPolicy(android.content.ComponentName, int):void");
    }

    public boolean isVivoActiveAdmin(ComponentName adminReceiver, int userHandle) {
        boolean z;
        synchronized (getLockObject()) {
            z = getVivoActiveAdminUncheckedLocked(adminReceiver, userHandle) != null;
        }
        return z;
    }

    public boolean hasVivoActiveAdmin(int userHandle) {
        boolean z;
        synchronized (getLockObject()) {
            z = getVivoUserData(userHandle).mVivoActiveAdmin != null;
        }
        return z;
    }

    private VivoActiveAdmin getVivoActiveAdminUncheckedLocked(ComponentName adminReceiver, int userHandle) {
        VivoActiveAdmin vivoadmin;
        if (adminReceiver == null || (vivoadmin = getVivoUserData(userHandle).mVivoActiveAdmin) == null || !adminReceiver.getPackageName().equals(vivoadmin.mInfo.getActivityInfo().packageName) || !adminReceiver.getClassName().equals(vivoadmin.mInfo.getActivityInfo().name)) {
            return null;
        }
        return vivoadmin;
    }

    private VivoActiveAdmin getVivoActiveAdmin(ComponentName adminReceiver, int userHandle) {
        VivoActiveAdmin vivoadmin = getVivoActiveAdminUncheckedLocked(adminReceiver, userHandle);
        if (vivoadmin != null) {
            if (vivoadmin.getUid() != Binder.getCallingUid()) {
                throw new SecurityException("VivoAdmin " + adminReceiver + " is not owned by uid " + Binder.getCallingUid());
            }
            return vivoadmin;
        }
        throw new SecurityException("No active vivo admin owned by uid " + Binder.getCallingUid() + ", ComponentName:" + adminReceiver);
    }

    private VivoAdminInfo findVivoAdmin(ComponentName adminName, int userHandle) {
        ActivityInfo ai = null;
        try {
            ai = this.mIPackageManager.getReceiverInfo(adminName, 819328, userHandle);
        } catch (RemoteException e) {
        }
        if (ai == null) {
            throw new IllegalArgumentException("Unknown admin: " + adminName);
        }
        return new VivoAdminInfo(this.mContext, ai);
    }

    public VivoPolicyData getVivoUserData(int userHandle) {
        VivoPolicyData policy;
        synchronized (getLockObject()) {
            policy = mVivoUserData.get(userHandle);
            if (policy == null) {
                policy = new VivoPolicyData(userHandle);
                mVivoUserData.append(userHandle, policy);
                loadEmmInfo(policy, userHandle);
                loadVivoSettingsLocked(policy, userHandle);
            }
        }
        return policy;
    }

    private JournaledFile makeVivoJournaledFile(int userId) {
        String base = new File(getVivoPolicyFileDirectory(userId), VivoUtils.VIVO_POLICIES_XML).getAbsolutePath();
        VLog.v(VIVO_LOG_TAG, "Opening " + base);
        File file = new File(base);
        return new JournaledFile(file, new File(base + ".tmp"));
    }

    private void saveVivoSettingsLocked(int poId, int userHandle) {
        VLog.v(VIVO_LOG_TAG, "saveSettingsLocked");
        VivoPolicyData policy = getVivoUserData(userHandle);
        JournaledFile journal = makeVivoJournaledFile(userHandle);
        FileOutputStream stream = null;
        long token = Binder.clearCallingIdentity();
        try {
            try {
                stream = new FileOutputStream(journal.chooseForWrite(), false);
                XmlSerializer out = new FastXmlSerializer();
                out.setOutput(stream, StandardCharsets.UTF_8.name());
                out.startDocument(null, true);
                out.startTag(null, "policies");
                if (policy.mPasswordToken != 0) {
                    out.startTag(null, TAG_ADMIN_PASSWORD_TOKEN);
                    out.attribute(null, ATTR_VALUE, Long.toString(policy.mPasswordToken));
                    out.endTag(null, TAG_ADMIN_PASSWORD_TOKEN);
                }
                VivoActiveAdmin ap = policy.mVivoActiveAdmin;
                if (ap != null) {
                    out.startTag(null, "admin");
                    out.attribute(null, "name", ap.mInfo.getComponent().flattenToString());
                    ap.writeToXml(out);
                    out.endTag(null, "admin");
                }
                out.endTag(null, "policies");
                out.endDocument();
                stream.flush();
                FileUtils.sync(stream);
                stream.close();
                journal.commit();
                sendChangedNotification(poId, userHandle);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        } catch (IOException e) {
            VLog.w(VIVO_LOG_TAG, "failed writing file", e);
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e2) {
                }
            }
            journal.rollback();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendChangedNotification(int poId, int userHandle) {
        Intent intent = new Intent("vivo.app.action.POLICY_MANAGER_STATE_CHANGED");
        intent.putExtra("poId", poId);
        intent.setFlags(1073741824);
        long token = Binder.clearCallingIdentity();
        try {
            this.mContext.sendBroadcastAsUser(intent, new UserHandle(userHandle));
            Binder.restoreCallingIdentity(token);
            this.mLocalService.notifyVivoPolicyChanged(poId);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
            throw th;
        }
    }

    private void loadVivoSettingsLocked(VivoPolicyData policy, int userHandle) {
        FileInputStream stream;
        XmlPullParser parser;
        int i;
        String tag;
        int outerDepth;
        int outerDepth2;
        JournaledFile journal = makeVivoJournaledFile(userHandle);
        FileInputStream stream2 = null;
        File file = journal.chooseForRead();
        try {
            try {
                try {
                    try {
                        stream = new FileInputStream(file);
                        parser = Xml.newPullParser();
                        parser.setInput(stream, StandardCharsets.UTF_8.name());
                        while (true) {
                            int type = parser.next();
                            i = 1;
                            if (type == 1 || type == 2) {
                                break;
                            }
                        }
                        tag = parser.getName();
                    } catch (Throwable th) {
                        if (0 != 0) {
                            try {
                                stream2.close();
                            } catch (IOException e) {
                            }
                        }
                        throw th;
                    }
                } catch (IOException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException e2) {
                    VLog.w(VIVO_LOG_TAG, "failed parsing " + file, e2);
                    if (0 != 0) {
                        stream2.close();
                    }
                }
            } catch (FileNotFoundException e3) {
                if (0 != 0) {
                    stream2.close();
                }
            }
        } catch (IOException e4) {
        }
        if (!"policies".equals(tag)) {
            throw new XmlPullParserException("Settings do not start with policies tag: found " + tag);
        }
        parser.next();
        int outerDepth3 = parser.getDepth();
        String str = null;
        policy.mVivoActiveAdmin = null;
        while (true) {
            int type2 = parser.next();
            if (type2 == i || (type2 == 3 && parser.getDepth() <= outerDepth3)) {
                break;
            }
            if (type2 == 3) {
                outerDepth = outerDepth3;
            } else if (type2 == 4) {
                outerDepth = outerDepth3;
            } else {
                String tag2 = parser.getName();
                if (TAG_ADMIN_PASSWORD_TOKEN.equals(tag2)) {
                    outerDepth2 = outerDepth3;
                    policy.mPasswordToken = Long.parseLong(parser.getAttributeValue(str, ATTR_VALUE));
                } else {
                    outerDepth2 = outerDepth3;
                    if ("admin".equals(tag2)) {
                        String name = parser.getAttributeValue(str, "name");
                        try {
                            VivoAdminInfo dai = findVivoAdmin(ComponentName.unflattenFromString(name), userHandle);
                            if (UserHandle.getUserId(dai.getActivityInfo().applicationInfo.uid) != userHandle) {
                                VLog.w(VIVO_LOG_TAG, "findAdmin returned an incorrect uid " + dai.getActivityInfo().applicationInfo.uid + " for user " + userHandle);
                            }
                            if (dai != null) {
                                VivoActiveAdmin ap = new VivoActiveAdmin(dai);
                                ap.readFromXml(parser);
                                policy.mVivoActiveAdmin = ap;
                            }
                        } catch (RuntimeException e5) {
                            VLog.w(VIVO_LOG_TAG, "Failed loading admin " + name, e5);
                        }
                    }
                }
                outerDepth3 = outerDepth2;
                i = 1;
                str = null;
            }
            outerDepth3 = outerDepth;
            i = 1;
            str = null;
        }
        stream.close();
        if (0 != 0) {
            saveVivoSettingsLocked(0, userHandle);
        }
    }

    private File getVivoPolicyFileDirectory(int userId) {
        if (userId == 0) {
            return new File(VivoUtils.VIVO_POLICIES_PATH);
        }
        return Environment.getUserSystemDirectory(userId);
    }

    public void checkCallingEmmPermission(ComponentName admin, String permission) {
        int callingUid = Binder.getCallingUid();
        int userId = UserHandle.getUserId(callingUid);
        checkCallingEmmPermission(admin, permission, userId);
    }

    private boolean isVivoCustomTool(int callingUid, int userHandle) {
        try {
            if (callingUid == this.mContext.getPackageManager().getPackageUidAsUser(CUSTOM_TOOL_NAME, userHandle)) {
                if (this.mIPackageManager.checkSignatures(VivoPermissionUtils.OS_PKG, CUSTOM_TOOL_NAME) == 0) {
                    return true;
                }
                return false;
            }
            return false;
        } catch (Exception e) {
            VLog.e(VIVO_LOG_TAG, "checkGetInfoPermission failed", e);
            return false;
        }
    }

    private void checkCallingEmmPermission(ComponentName admin, String permission, int userHandle) {
        int callingUid = Binder.getCallingUid();
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null && permission != null) {
                List<String> permList = getEmmPermissions(ap.mInfo.getPackageName(), userHandle);
                if (permList.contains(permission)) {
                    VLog.i(VIVO_LOG_TAG, "Admin " + admin + " have permission " + permission);
                    return;
                }
            }
            if (ap != null && isVivoCustomTool(callingUid, userHandle)) {
                VLog.i(VIVO_LOG_TAG, "custom tool have permission");
                return;
            }
            throw new SecurityException("Admin " + admin + " or the caller don't have permission " + permission);
        }
    }

    public boolean setRestrictionPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        Preconditions.checkNotNull(admin, "ComponentName is null");
        boolean success = false;
        try {
            reportVcodeInvoke(admin, polId);
            if (polId == 112) {
                success = setAppControlPolicy(admin, polId, policy, userHandle);
            } else if (polId == 502) {
                success = setForgotPasswordPolicy(admin, polId, policy, userHandle);
            } else if (polId == 701) {
                success = setUserMenuPolicy(admin, polId, policy, userHandle);
            } else if (polId == 601) {
                success = setAdminDeviceOwnerPolicy(admin, polId, policy, userHandle);
            } else if (polId != 602) {
                switch (polId) {
                    case 1:
                        success = setWlanPolicy(admin, polId, policy, userHandle);
                        break;
                    case 2:
                        success = setWlanBWPolicy(admin, polId, policy, userHandle);
                        break;
                    case 3:
                        success = setWlanApPolicy(admin, polId, policy, userHandle);
                        break;
                    case 4:
                        success = setWlanApBWPolicy(admin, polId, policy, userHandle);
                        break;
                    case 5:
                        success = setBluetoothPolicy(admin, polId, policy, userHandle);
                        break;
                    case 6:
                        success = setBluetoothBWPolicy(admin, polId, policy, userHandle);
                        break;
                    case 7:
                        success = setBluetoothApPolicy(admin, polId, policy, userHandle);
                        break;
                    default:
                        switch (polId) {
                            case 9:
                                success = setUsbTransferPolicy(admin, polId, policy, userHandle);
                                break;
                            case 10:
                                success = setUsbDebugPolicy(admin, polId, policy, userHandle);
                                break;
                            case 11:
                                success = setUsbApPolicy(admin, polId, policy, userHandle);
                                break;
                            case 12:
                                success = setUsbOtgPolicy(admin, polId, policy, userHandle);
                                break;
                            case 13:
                                success = setSpeakerPolicy(admin, polId, policy, userHandle);
                                break;
                            case 14:
                                success = setFlashLightPolicy(admin, polId, policy, userHandle);
                                break;
                            case 15:
                                success = setMicPolicy(admin, polId, policy, userHandle);
                                break;
                            case 16:
                                success = setWlanConfigPolicy(admin, polId, policy, userHandle);
                                break;
                            case 17:
                                success = setBluetoothConfigPolicy(admin, polId, policy, userHandle);
                                break;
                            case 18:
                                success = setBluetoothSharingPolicy(admin, polId, policy, userHandle);
                                break;
                            case 19:
                                success = setAppLocationPolicy(admin, polId, policy, userHandle);
                                break;
                            case 20:
                                success = setCameraPolicy(admin, polId, policy, userHandle);
                                break;
                            case 21:
                                success = setExternalStoragePolicy(admin, polId, policy, userHandle);
                                break;
                            case 22:
                                success = setNfcSharingPolicy(admin, polId, policy, userHandle);
                                break;
                            case 23:
                                success = setNfcAllPolicy(admin, polId, policy, userHandle);
                                break;
                            case 24:
                                success = setPeripheralWlanDirectPolicy(admin, polId, policy, userHandle);
                                break;
                            case 25:
                                success = setPeripheralWlanScanAlwaysPolicy(admin, polId, policy, userHandle);
                                break;
                            default:
                                switch (polId) {
                                    case 101:
                                        success = setAppInstallPolicy(admin, polId, policy, userHandle);
                                        break;
                                    case 102:
                                        success = setAppInstallBWPolicy(admin, polId, policy, userHandle);
                                        break;
                                    case KernelConfig.DBG_TARGET_REGADDR_VALUE_GET /* 103 */:
                                        success = setAppUnInstallPolicy(admin, polId, policy, userHandle);
                                        break;
                                    case 104:
                                        success = setAppUnInstallBWPolicy(admin, polId, policy, userHandle);
                                        break;
                                    case 105:
                                        success = setAppTrustedSourcePolicy(admin, polId, policy, userHandle);
                                        break;
                                    case 106:
                                        success = setAppDisallowedLaunchPolicy(admin, polId, policy, userHandle);
                                        break;
                                    case KernelConfig.DBG_SEND_PACKAGE /* 107 */:
                                        success = setAppMeteredDataBWPolicy(admin, polId, policy, userHandle);
                                        break;
                                    case KernelConfig.DBG_LOOP_BACK_MODE /* 108 */:
                                        success = setAppWlanDataBWPolicy(admin, polId, policy, userHandle);
                                        break;
                                    case KernelConfig.DBG_LOOP_BACK_MODE_RES /* 109 */:
                                        success = setAppPermissionPolicy(admin, polId, policy, userHandle);
                                        break;
                                    case 110:
                                        success = setInstallUnknownSourcePolicy(admin, polId, policy, userHandle);
                                        break;
                                    default:
                                        switch (polId) {
                                            case 201:
                                                success = setNetworkApnPolicy(admin, polId, policy, userHandle);
                                                break;
                                            case 202:
                                                success = setNetworkMobileDataPolicy(admin, polId, policy, userHandle);
                                                break;
                                            case 203:
                                                success = setNetworkDomainBWPolicy(admin, polId, policy, userHandle);
                                                break;
                                            case VivoWmsImpl.UPDATA_GAME_MODE /* 204 */:
                                                success = setNetworkIpAddrBWPolicy(admin, polId, policy, userHandle);
                                                break;
                                            case VivoWmsImpl.SPLIT_CLEAR_TEMP_COLOR_STATE /* 205 */:
                                                success = setOverrideApnPolicy(admin, polId, policy, userHandle);
                                                break;
                                            case VivoWmsImpl.SPLIT_MINI_LAUNCHER_ANIM_CHANGE_TIMEOUT /* 206 */:
                                                success = setVpnPolicy(admin, polId, policy, userHandle);
                                                break;
                                            case VivoWmsImpl.UPDATA_SYSTEMUI_GESTURE_STYLE /* 207 */:
                                                success = setDataRoamingPolicy(admin, polId, policy, userHandle);
                                                break;
                                            case 208:
                                                success = setTetheringPolicy(admin, polId, policy, userHandle);
                                                break;
                                            case VivoAudioServiceImpl.MSG_CHECK_RECORDING_EVENT /* 209 */:
                                                success = setConfigNetworkPolicy(admin, polId, policy, userHandle);
                                                break;
                                            case 210:
                                                success = setNetworkMobileDataSlotPolicy(admin, polId, policy, userHandle);
                                                break;
                                            default:
                                                switch (polId) {
                                                    case VivoWmsImpl.NOTIFY_SPLIT_BAR_LAYOUT /* 301 */:
                                                        success = setOperationHomeKeyPolicy(admin, polId, policy, userHandle);
                                                        break;
                                                    case 302:
                                                        success = setOperationBackKeyPolicy(admin, polId, policy, userHandle);
                                                        break;
                                                    case 303:
                                                        success = setOperationMenuKeyPolicy(admin, polId, policy, userHandle);
                                                        break;
                                                    case 304:
                                                        success = setOperationRecentTaskKeyPolicy(admin, polId, policy, userHandle);
                                                        break;
                                                    case 305:
                                                        success = setOperationStatusbarPolicy(admin, polId, policy, userHandle);
                                                        break;
                                                    case 306:
                                                        success = setOperationClipboardPolicy(admin, polId, policy, userHandle);
                                                        break;
                                                    case 307:
                                                        success = setOperationBackupPolicy(admin, polId, policy, userHandle);
                                                        break;
                                                    default:
                                                        switch (polId) {
                                                            case 309:
                                                                success = setOperationPowerSavingPolicy(admin, polId, policy, userHandle);
                                                                break;
                                                            case 310:
                                                                success = setOperationPowerPanelKeyPolicy(admin, polId, policy, userHandle);
                                                                break;
                                                            case 311:
                                                                success = setOperationHardFactoryResetPolicy(admin, polId, policy, userHandle);
                                                                break;
                                                            case 312:
                                                                success = setSafeModePolicy(admin, polId, policy, userHandle);
                                                                break;
                                                            case 313:
                                                                success = setVolumePolicy(admin, polId, policy, userHandle);
                                                                break;
                                                            case 314:
                                                                success = setScreenCapturePolicy(admin, polId, policy, userHandle);
                                                                break;
                                                            case 315:
                                                                success = setLocalePolicy(admin, polId, policy, userHandle);
                                                                break;
                                                            case 316:
                                                                success = setAccountModifyPolicy(admin, polId, policy, userHandle);
                                                                break;
                                                            case 317:
                                                                success = setDataTimePolicy(admin, polId, policy, userHandle);
                                                                break;
                                                            case 318:
                                                                success = setAutoTimeOffPolicy(admin, polId, policy, userHandle);
                                                                break;
                                                            case 319:
                                                                success = setFactoryResetPolicy(admin, polId, policy, userHandle);
                                                                break;
                                                            case 320:
                                                                success = setFunGamePolicy(admin, polId, policy, userHandle);
                                                                break;
                                                            case 321:
                                                                success = setBrightnessPolicy(admin, polId, policy, userHandle);
                                                                break;
                                                            case 322:
                                                                success = setScreenTimeoutPolicy(admin, polId, policy, userHandle);
                                                                break;
                                                            case 323:
                                                                success = setConfigWallpaperPolicy(admin, polId, policy, userHandle);
                                                                break;
                                                            case 324:
                                                                success = setCallRecordPolicy(admin, polId, policy, userHandle);
                                                                break;
                                                            case 325:
                                                                success = setMockLocationPolicy(admin, polId, policy, userHandle);
                                                                break;
                                                            case 326:
                                                                success = setOperationAppNotificationPolicy(admin, polId, policy, userHandle);
                                                                break;
                                                            case 327:
                                                                success = setOperationSysUpgradePolicy(admin, polId, policy, userHandle);
                                                                break;
                                                            case 328:
                                                                success = setEyeProtectionPolicy(admin, polId, policy, userHandle);
                                                                break;
                                                            case 329:
                                                                success = setMagazineLockPolicy(admin, polId, policy, userHandle);
                                                                break;
                                                            default:
                                                                switch (polId) {
                                                                    case 401:
                                                                        success = setPhoneCallPolicy(admin, polId, policy, userHandle);
                                                                        break;
                                                                    case 402:
                                                                        success = setPhoneCallBWPolicy(admin, polId, policy, userHandle);
                                                                        break;
                                                                    case 403:
                                                                        success = setPhoneSmsPolicy(admin, polId, policy, userHandle);
                                                                        break;
                                                                    case 404:
                                                                        success = setPhoneSmsBWPolicy(admin, polId, policy, userHandle);
                                                                        break;
                                                                    case 405:
                                                                        success = setPhoneMmsPolicy(admin, polId, policy, userHandle);
                                                                        break;
                                                                    case 406:
                                                                        success = setPhoneSimSlotPolicy(admin, polId, policy, userHandle);
                                                                        break;
                                                                    case 407:
                                                                        success = setAirplaneModePolicy(admin, polId, policy, userHandle);
                                                                        break;
                                                                    case 408:
                                                                        success = setPhoneCall1Policy(admin, polId, policy, userHandle);
                                                                        break;
                                                                    case 409:
                                                                        success = setPhoneCall2Policy(admin, polId, policy, userHandle);
                                                                        break;
                                                                    case 410:
                                                                        success = setPhoneSms1Policy(admin, polId, policy, userHandle);
                                                                        break;
                                                                    case 411:
                                                                        success = setPhoneSms2Policy(admin, polId, policy, userHandle);
                                                                        break;
                                                                    case 412:
                                                                        success = setPhoneMms1Policy(admin, polId, policy, userHandle);
                                                                        break;
                                                                    case 413:
                                                                        success = setPhoneMms2Policy(admin, polId, policy, userHandle);
                                                                        break;
                                                                    case 414:
                                                                        success = setPhoneMaskPolicy(admin, polId, policy, userHandle);
                                                                        break;
                                                                    case 415:
                                                                        success = setPhoneMutliCallPolicy(admin, polId, policy, userHandle);
                                                                        break;
                                                                    case 416:
                                                                        success = setPhoneCallForwardingPolicy(admin, polId, policy, userHandle);
                                                                        break;
                                                                    case 417:
                                                                        success = setPhoneNetworkSMSPolicy(admin, polId, policy, userHandle);
                                                                        break;
                                                                    case 418:
                                                                        success = setTelecomMaskPermissionBwlistPolicy(admin, polId, policy, userHandle);
                                                                        break;
                                                                    default:
                                                                        VLog.w(VIVO_LOG_TAG, "Bad device polId");
                                                                        break;
                                                                }
                                                        }
                                                }
                                        }
                                }
                        }
                }
            } else {
                success = setAdminProfileOwnerPolicy(admin, polId, policy, userHandle);
            }
        } catch (Exception e) {
            VLog.e(VIVO_LOG_TAG, "setRestrictionPolicy failed! " + e);
            e.printStackTrace();
        }
        VLog.d(VIVO_LOG_TAG, "setRestrictionPolicy polId= " + polId + ",policy = " + policy + ",success = " + success + ",userHandle = " + userHandle);
        return success;
    }

    public int getRestrictionPolicy(ComponentName admin, int polId, int userHandle) {
        int policy = 0;
        if (14 != polId && 306 != polId) {
            try {
                checkGetInfoPermission();
            } catch (Exception e) {
                VLog.e(VIVO_LOG_TAG, "getRestrictionPolicy failed! " + e);
                e.printStackTrace();
            }
        }
        if (polId == 112) {
            policy = getAppControlPolicy(admin, userHandle);
        } else if (polId == 502) {
            policy = getForgotPasswordPolicy(admin, userHandle);
        } else if (polId == 701) {
            policy = getUserMenuPolicy(admin, userHandle);
        } else if (polId == 601) {
            policy = getAdminDeviceOwnerPolicy(admin, userHandle);
        } else if (polId != 602) {
            switch (polId) {
                case 1:
                    policy = getWlanPolicy(admin, userHandle);
                    break;
                case 2:
                    policy = getWlanBWPolicy(admin, userHandle);
                    break;
                case 3:
                    policy = getWlanApPolicy(admin, userHandle);
                    break;
                case 4:
                    policy = getWlanApBWPolicy(admin, userHandle);
                    break;
                case 5:
                    policy = getBluetoothPolicy(admin, userHandle);
                    break;
                case 6:
                    policy = getBluetoothBWPolicy(admin, userHandle);
                    break;
                case 7:
                    policy = getBluetoothApPolicy(admin, userHandle);
                    break;
                default:
                    switch (polId) {
                        case 9:
                            policy = getUsbTransferPolicy(admin, userHandle);
                            break;
                        case 10:
                            policy = getUsbDebugPolicy(admin, userHandle);
                            break;
                        case 11:
                            policy = getUsbApPolicy(admin, userHandle);
                            break;
                        case 12:
                            policy = getUsbOtgPolicy(admin, userHandle);
                            break;
                        case 13:
                            policy = getSpeakerPolicy(admin, userHandle);
                            break;
                        case 14:
                            policy = getFlashLightPolicy(admin, userHandle);
                            break;
                        case 15:
                            policy = getMicPolicy(admin, userHandle);
                            break;
                        case 16:
                            policy = getWlanConfigPolicy(admin, userHandle);
                            break;
                        case 17:
                            policy = getBluetoothConfigPolicy(admin, userHandle);
                            break;
                        case 18:
                            policy = getBluetoothSharingPolicy(admin, userHandle);
                            break;
                        case 19:
                            policy = getAppLocationPolicy(admin, userHandle);
                            break;
                        case 20:
                            policy = getCameraPolicy(admin, userHandle);
                            break;
                        case 21:
                            policy = getExternalStoragePolicy(admin, userHandle);
                            break;
                        case 22:
                            policy = getNfcSharingPolicy(admin, userHandle);
                            break;
                        case 23:
                            policy = getNfcAllPolicy(admin, userHandle);
                            break;
                        case 24:
                            policy = getPeripheralWlanDirectPolicy(admin, userHandle);
                            break;
                        case 25:
                            policy = getPeripheralWlanScanAlwaysPolicy(admin, userHandle);
                            break;
                        default:
                            switch (polId) {
                                case 101:
                                    policy = getAppInstallPolicy(admin, userHandle);
                                    break;
                                case 102:
                                    policy = getAppInstallBWPolicy(admin, userHandle);
                                    break;
                                case KernelConfig.DBG_TARGET_REGADDR_VALUE_GET /* 103 */:
                                    policy = getAppUnInstallPolicy(admin, userHandle);
                                    break;
                                case 104:
                                    policy = getAppUnInstallBWPolicy(admin, userHandle);
                                    break;
                                case 105:
                                    policy = getAppTrustedSourcePolicy(admin, userHandle);
                                    break;
                                case 106:
                                    policy = getAppDisallowedLaunchPolicy(admin, userHandle);
                                    break;
                                case KernelConfig.DBG_SEND_PACKAGE /* 107 */:
                                    policy = getAppMeteredDataBWPolicy(admin, userHandle);
                                    break;
                                case KernelConfig.DBG_LOOP_BACK_MODE /* 108 */:
                                    policy = getAppWlanDataBWPolicy(admin, userHandle);
                                    break;
                                case KernelConfig.DBG_LOOP_BACK_MODE_RES /* 109 */:
                                    policy = getAppPermissionPolicy(admin, userHandle);
                                    break;
                                case 110:
                                    policy = getInstallUnknownSourcePolicy(admin, userHandle);
                                    break;
                                default:
                                    switch (polId) {
                                        case 201:
                                            policy = getNetworkApnPolicy(admin, userHandle);
                                            break;
                                        case 202:
                                            policy = getNetworkMobileDataPolicy(admin, userHandle);
                                            break;
                                        case 203:
                                            policy = getNetworkDomainBWPolicy(admin, userHandle);
                                            break;
                                        case VivoWmsImpl.UPDATA_GAME_MODE /* 204 */:
                                            policy = getNetworkIpAddrBWPolicy(admin, userHandle);
                                            break;
                                        case VivoWmsImpl.SPLIT_CLEAR_TEMP_COLOR_STATE /* 205 */:
                                            policy = getOverrideApnPolicy(admin, userHandle);
                                            break;
                                        case VivoWmsImpl.SPLIT_MINI_LAUNCHER_ANIM_CHANGE_TIMEOUT /* 206 */:
                                            policy = getVpnPolicy(admin, userHandle);
                                            break;
                                        case VivoWmsImpl.UPDATA_SYSTEMUI_GESTURE_STYLE /* 207 */:
                                            policy = getDataRoamingPolicy(admin, userHandle);
                                            break;
                                        case 208:
                                            policy = getTetheringPolicy(admin, userHandle);
                                            break;
                                        case VivoAudioServiceImpl.MSG_CHECK_RECORDING_EVENT /* 209 */:
                                            policy = getConfigNetworkPolicy(admin, userHandle);
                                            break;
                                        case 210:
                                            policy = getNetworkMobileDataSlotPolicy(admin, userHandle);
                                            break;
                                        default:
                                            switch (polId) {
                                                case VivoWmsImpl.NOTIFY_SPLIT_BAR_LAYOUT /* 301 */:
                                                    policy = getOperationHomeKeyPolicy(admin, userHandle);
                                                    break;
                                                case 302:
                                                    policy = getOperationBackKeyPolicy(admin, userHandle);
                                                    break;
                                                case 303:
                                                    policy = getOperationMenuKeyPolicy(admin, userHandle);
                                                    break;
                                                case 304:
                                                    policy = getOperationRecentTaskKeyPolicy(admin, userHandle);
                                                    break;
                                                case 305:
                                                    policy = getOperationStatusbarPolicy(admin, userHandle);
                                                    break;
                                                case 306:
                                                    policy = getOperationClipboardPolicy(admin, userHandle);
                                                    break;
                                                case 307:
                                                    policy = getOperationBackupPolicy(admin, userHandle);
                                                    break;
                                                default:
                                                    switch (polId) {
                                                        case 309:
                                                            policy = getOperationPowerSavingPolicy(admin, userHandle);
                                                            break;
                                                        case 310:
                                                            policy = getOperationPowerPanelKeyPolicy(admin, userHandle);
                                                            break;
                                                        case 311:
                                                            policy = getOperationHardFactoryResetPolicy(admin, userHandle);
                                                            break;
                                                        case 312:
                                                            policy = getSafeModePolicy(admin, userHandle);
                                                            break;
                                                        case 313:
                                                            policy = getVolumePolicy(admin, userHandle);
                                                            break;
                                                        case 314:
                                                            policy = getScreenCapturePolicy(admin, userHandle);
                                                            break;
                                                        case 315:
                                                            policy = getLocalePolicy(admin, userHandle);
                                                            break;
                                                        case 316:
                                                            policy = getAccountModifyPolicy(admin, userHandle);
                                                            break;
                                                        case 317:
                                                            policy = getDataTimePolicy(admin, userHandle);
                                                            break;
                                                        case 318:
                                                            policy = getAutoTimeOffPolicy(admin, userHandle);
                                                            break;
                                                        case 319:
                                                            policy = getFactoryResetPolicy(admin, userHandle);
                                                            break;
                                                        case 320:
                                                            policy = getFunGamePolicy(admin, userHandle);
                                                            break;
                                                        case 321:
                                                            policy = getBrightnessPolicy(admin, userHandle);
                                                            break;
                                                        case 322:
                                                            policy = getScreenTimeoutPolicy(admin, userHandle);
                                                            break;
                                                        case 323:
                                                            policy = getConfigWallpaperPolicy(admin, userHandle);
                                                            break;
                                                        case 324:
                                                            policy = getCallRecordPolicy(admin, userHandle);
                                                            break;
                                                        case 325:
                                                            policy = getMockLocationPolicy(admin, userHandle);
                                                            break;
                                                        case 326:
                                                            policy = getOperationAppNotificationPolicy(admin, userHandle);
                                                            break;
                                                        case 327:
                                                            policy = getOperationSysUpgradePolicy(admin, userHandle);
                                                            break;
                                                        case 328:
                                                            policy = getEyeProtectionPolicy(admin, userHandle);
                                                            break;
                                                        case 329:
                                                            policy = getMagazineLockPolicy(admin, userHandle);
                                                            break;
                                                        default:
                                                            switch (polId) {
                                                                case 401:
                                                                    policy = getPhoneCallPolicy(admin, userHandle);
                                                                    break;
                                                                case 402:
                                                                    policy = getPhoneCallBWPolicy(admin, userHandle);
                                                                    break;
                                                                case 403:
                                                                    policy = getPhoneSmsPolicy(admin, userHandle);
                                                                    break;
                                                                case 404:
                                                                    policy = getPhoneSmsBWPolicy(admin, userHandle);
                                                                    break;
                                                                case 405:
                                                                    policy = getPhoneMmsPolicy(admin, userHandle);
                                                                    break;
                                                                case 406:
                                                                    policy = getPhoneSimSlotPolicy(admin, userHandle);
                                                                    break;
                                                                case 407:
                                                                    policy = getAirplaneModePolicy(admin, userHandle);
                                                                    break;
                                                                case 408:
                                                                    policy = getPhoneCall1Policy(admin, userHandle);
                                                                    break;
                                                                case 409:
                                                                    policy = getPhoneCall2Policy(admin, userHandle);
                                                                    break;
                                                                case 410:
                                                                    policy = getPhoneSms1Policy(admin, userHandle);
                                                                    break;
                                                                case 411:
                                                                    policy = getPhoneSms2Policy(admin, userHandle);
                                                                    break;
                                                                case 412:
                                                                    policy = getPhoneMms1Policy(admin, userHandle);
                                                                    break;
                                                                case 413:
                                                                    policy = getPhoneMms2Policy(admin, userHandle);
                                                                    break;
                                                                case 414:
                                                                    policy = getPhoneMaskPolicy(admin, userHandle);
                                                                    break;
                                                                case 415:
                                                                    policy = getPhoneMutliCallPolicy(admin, userHandle);
                                                                    break;
                                                                case 416:
                                                                    policy = getPhoneCallForwardingPolicy(admin, userHandle);
                                                                    break;
                                                                case 417:
                                                                    policy = getPhoneNetworkSMSPolicy(admin, userHandle);
                                                                    break;
                                                                case 418:
                                                                    policy = getTelecomMaskPermissionBwlistPolicy(admin, userHandle);
                                                                    break;
                                                                case 419:
                                                                    policy = getTelecomPINLockPolicy(admin, userHandle);
                                                                    break;
                                                                default:
                                                                    VLog.w(VIVO_LOG_TAG, "Bad device polId");
                                                                    break;
                                                            }
                                                    }
                                            }
                                    }
                            }
                    }
            }
        } else {
            policy = getAdminProfileOwnerPolicy(admin, userHandle);
        }
        VLog.d(VIVO_LOG_TAG, "getRestrictionPolicy polId= " + polId + ",policy = " + policy + ",userHandle = " + userHandle);
        return policy;
    }

    public boolean setRestrictionInfoList(ComponentName admin, int listId, List<String> keys, boolean isAdd, int userHandle) {
        Preconditions.checkNotNull(admin, "ComponentName is null");
        boolean success = false;
        try {
            reportVcodeInvoke(admin, listId);
            switch (listId) {
                case 1001:
                case 1002:
                    success = setWlanBWList(admin, listId, keys, isAdd, userHandle);
                    break;
                case 1003:
                case Constants.CMD.CMD_HIDL_GET_PRE_UPDATE_ARD_VERSION /* 1004 */:
                    success = setWlanApBWList(admin, listId, keys, isAdd, userHandle);
                    break;
                case Constants.CMD.CMD_HIDL_FACE_AUTO_TEST /* 1005 */:
                case Constants.CMD.CMD_HIDL_INITIALIZATION /* 1006 */:
                    success = setBluetoothBWList(admin, listId, keys, isAdd, userHandle);
                    break;
                default:
                    switch (listId) {
                        case 1101:
                        case 1102:
                            success = setAppInstallBWList(admin, listId, keys, isAdd, userHandle);
                            break;
                        case 1103:
                        case 1104:
                            success = setAppUnInstallBWList(admin, listId, keys, isAdd, userHandle);
                            break;
                        default:
                            switch (listId) {
                                case 1106:
                                case 1107:
                                    success = setAppMeteredDataBWList(admin, listId, keys, isAdd, userHandle);
                                    break;
                                case 1108:
                                case 1109:
                                    success = setAppWlanDataBWList(admin, listId, keys, isAdd, userHandle);
                                    break;
                                default:
                                    switch (listId) {
                                        case 1201:
                                        case 1202:
                                            success = setNetworkDomainBWList(admin, listId, keys, isAdd, userHandle);
                                            break;
                                        case 1203:
                                        case 1204:
                                            success = setNetworkIpAddrBWList(admin, listId, keys, isAdd, userHandle);
                                            break;
                                        default:
                                            switch (listId) {
                                                case 1301:
                                                case 1302:
                                                    success = setPhoneCallBWList(admin, listId, keys, isAdd, userHandle);
                                                    break;
                                                case 1303:
                                                case 1304:
                                                    success = setPhoneSmsBWList(admin, listId, keys, isAdd, userHandle);
                                                    break;
                                                case 1305:
                                                case 1306:
                                                    success = setPhoneMaskPermissionBWList(admin, listId, keys, isAdd, userHandle);
                                                    break;
                                                default:
                                                    switch (listId) {
                                                        case 1501:
                                                            success = setAppDisabledAppList(admin, listId, keys, isAdd, userHandle);
                                                            break;
                                                        case 1502:
                                                            success = setAppDisallowedLaunchList(admin, listId, keys, isAdd, userHandle);
                                                            break;
                                                        case 1503:
                                                            success = setAppPersistentAppList(admin, listId, keys, isAdd, userHandle);
                                                            break;
                                                        case 1504:
                                                            success = setAppDisallowedClearDataList(admin, listId, keys, isAdd, userHandle);
                                                            break;
                                                        default:
                                                            switch (listId) {
                                                                case 1508:
                                                                    success = setAppTrustedSourceList(admin, listId, keys, isAdd, userHandle);
                                                                    break;
                                                                case 1509:
                                                                    success = setAppPermissionWhiteList(admin, listId, keys, isAdd, userHandle);
                                                                    break;
                                                                case 1510:
                                                                    success = setAppAlarmWhiteList(admin, listId, keys, isAdd, userHandle);
                                                                    break;
                                                                case 1511:
                                                                    success = setAppNotificationListenerList(admin, listId, keys, isAdd, userHandle);
                                                                    break;
                                                                case 1512:
                                                                    success = setApnDisableRecoveryList(admin, listId, keys, isAdd, userHandle);
                                                                    break;
                                                                default:
                                                                    VLog.w(VIVO_LOG_TAG, "Bad device listId");
                                                                    break;
                                                            }
                                                    }
                                            }
                                    }
                            }
                    }
            }
        } catch (Exception e) {
            VLog.e(VIVO_LOG_TAG, "setRestrictionInfoList failed! " + e);
            e.printStackTrace();
        }
        VLog.d(VIVO_LOG_TAG, "setRestrictionInfoList listId= " + listId + ",isAdd = " + isAdd + ",success = " + success + ",userHandle = " + userHandle);
        return success;
    }

    public List<String> getRestrictionInfoList(ComponentName admin, int listId, int userHandle) {
        List<String> result = null;
        try {
            checkGetInfoPermission();
            if (listId != 3215) {
                switch (listId) {
                    case 1001:
                    case 1002:
                        result = getWlanBWList(admin, listId, userHandle);
                        break;
                    case 1003:
                    case Constants.CMD.CMD_HIDL_GET_PRE_UPDATE_ARD_VERSION /* 1004 */:
                        result = getWlanApBWList(admin, listId, userHandle);
                        break;
                    case Constants.CMD.CMD_HIDL_FACE_AUTO_TEST /* 1005 */:
                    case Constants.CMD.CMD_HIDL_INITIALIZATION /* 1006 */:
                        result = getBluetoothBWList(admin, listId, userHandle);
                        break;
                    default:
                        switch (listId) {
                            case 1101:
                            case 1102:
                                result = getAppInstallBWList(admin, listId, userHandle);
                                break;
                            case 1103:
                            case 1104:
                                result = getAppUnInstallBWList(admin, listId, userHandle);
                                break;
                            default:
                                switch (listId) {
                                    case 1106:
                                    case 1107:
                                        result = getAppMeteredDataBWList(admin, listId, userHandle);
                                        break;
                                    case 1108:
                                    case 1109:
                                        result = getAppWlanDataBWList(admin, listId, userHandle);
                                        break;
                                    default:
                                        switch (listId) {
                                            case 1201:
                                            case 1202:
                                                result = getNetworkDomainBWList(admin, listId, userHandle);
                                                break;
                                            case 1203:
                                            case 1204:
                                                result = getNetworkIpAddrBWList(admin, listId, userHandle);
                                                break;
                                            default:
                                                switch (listId) {
                                                    case 1301:
                                                    case 1302:
                                                        result = getPhoneCallBWList(admin, listId, userHandle);
                                                        break;
                                                    case 1303:
                                                    case 1304:
                                                        result = getPhoneSmsBWList(admin, listId, userHandle);
                                                        break;
                                                    case 1305:
                                                    case 1306:
                                                        result = getPhoneMaskPermissionBWList(admin, listId, userHandle);
                                                        break;
                                                    default:
                                                        switch (listId) {
                                                            case 1501:
                                                                result = getAppDisabledAppList(admin, userHandle);
                                                                break;
                                                            case 1502:
                                                                result = getAppDisallowedLaunchList(admin, userHandle);
                                                                break;
                                                            case 1503:
                                                                result = getAppPersistentAppList(admin, userHandle);
                                                                break;
                                                            case 1504:
                                                                result = getAppDisallowedClearDataList(admin, userHandle);
                                                                break;
                                                            case 1505:
                                                                result = getDeviceInfo2(admin, userHandle);
                                                                break;
                                                            case 1506:
                                                                result = getDeviceInfo3(admin, userHandle);
                                                                break;
                                                            case 1507:
                                                                result = getDeviceInfo1(admin, userHandle);
                                                                break;
                                                            case 1508:
                                                                result = getAppTrustedSourceList(admin, userHandle);
                                                                break;
                                                            case 1509:
                                                                result = getAppPermissionWhiteList(admin, userHandle);
                                                                break;
                                                            case 1510:
                                                                result = getAppAlarmWhiteList(admin, userHandle);
                                                                break;
                                                            case 1511:
                                                                result = getAppNotificationListenerList(admin, userHandle);
                                                                break;
                                                            case 1512:
                                                                result = getApnDisableRecoveryList(admin, userHandle);
                                                                break;
                                                            default:
                                                                VLog.w(VIVO_LOG_TAG, "Bad device listId");
                                                                break;
                                                        }
                                                }
                                        }
                                }
                        }
                }
            } else {
                result = getCustomSettingsMenu(admin, userHandle);
            }
            StringBuilder sb = new StringBuilder();
            sb.append("getRestrictionInfoList listId= ");
            sb.append(listId);
            sb.append(",result = ");
            sb.append((result == null || result.isEmpty()) ? Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK : String.join(",", result));
            sb.append(",userHandle = ");
            sb.append(userHandle);
            VLog.d(VIVO_LOG_TAG, sb.toString());
        } catch (Exception e) {
            VLog.e(VIVO_LOG_TAG, "getRestrictionInfoList failed! " + e);
            e.printStackTrace();
        }
        return result;
    }

    public boolean invokeDeviceTransaction(ComponentName admin, int transId, Bundle data, int userHandle) {
        boolean success = false;
        try {
            reportVcodeInvoke(admin, transId);
            if (transId == 3101) {
                success = addVpnProfile(admin, data, userHandle);
            } else if (transId == 3102) {
                success = delVpnProfile(admin, data, userHandle);
            } else if (transId == 3108) {
                success = removeOverrideApn(admin, data, userHandle);
            } else if (transId == 3109) {
                success = setAlwaysOnVpnPackage(admin, data, userHandle);
            } else if (transId != 3301 && transId != 3302) {
                if (transId == 3304 || transId == 3305) {
                    success = setProfileOwner(admin, transId, data, userHandle);
                } else {
                    switch (transId) {
                        case KernelConfig.APP_FILTER /* 111 */:
                            success = clearAppData(admin, data, userHandle);
                            break;
                        case 419:
                            success = setTelecomPINLock(admin, transId, data, userHandle);
                            break;
                        case 2001:
                            success = setWlanConfigurations(admin, data, userHandle);
                            break;
                        case 2006:
                            success = delWlanConfigurations(admin, data, userHandle);
                            break;
                        case 3006:
                            success = setSystemPermission(admin, transId, data, userHandle);
                            break;
                        case 3008:
                        case 3009:
                        case 3010:
                        case 3011:
                        case 3012:
                        case 3013:
                            success = setDefaultApp(admin, transId, data, userHandle);
                            break;
                        case 3106:
                            success = updateOverrideApn(admin, data, userHandle);
                            break;
                        case 3201:
                            success = shutDown(admin, userHandle);
                            break;
                        case 3215:
                            success = setCustomSettingsMenu(admin, transId, data, userHandle);
                            break;
                        case 3312:
                            success = setOrganizationName(admin, data, userHandle);
                            break;
                        case 3313:
                            success = setPermittedAccessibilityServices(admin, data, userHandle);
                            break;
                        case 3400:
                            success = setTelecomEndCall(admin, transId, data, userHandle);
                            break;
                        case 3401:
                            success = setTelecomAnswerCall(admin, transId, data, userHandle);
                            break;
                        case 3402:
                            success = setTelecomCallNum(admin, transId, data, userHandle);
                            break;
                        case 3403:
                            success = setTelecomSMSNum(admin, transId, data, userHandle);
                            break;
                        case 3602:
                            success = lockNow(admin, data, userHandle);
                            break;
                        case 3603:
                            success = setResetPasswordToken(admin, transId, data, userHandle);
                            break;
                        case 3604:
                            success = clearResetPasswordToken(admin, transId, data, userHandle);
                            break;
                        case 3606:
                            success = resetPasswordWithToken(admin, data, userHandle);
                            break;
                        case 3607:
                            success = setMaximumTimeToLock(admin, data, userHandle);
                            break;
                        case 3609:
                            success = setRequiredStrongAuthTimeout(admin, data, userHandle);
                            break;
                        case 3611:
                            success = setPasswordExpirationTimeout(admin, data, userHandle);
                            break;
                        case 3613:
                            success = setPasswordQuality(admin, data, userHandle);
                            break;
                        case 3615:
                            success = setKeyguardDisabledFeatures(admin, data, userHandle);
                            break;
                        default:
                            switch (transId) {
                                case 3001:
                                    success = silentInstallPackage(admin, data, userHandle);
                                    break;
                                case 3002:
                                    success = silentUnInstallPackage(admin, data, userHandle);
                                    break;
                                case 3003:
                                    success = killProcess(admin, data, userHandle);
                                    break;
                                case 3004:
                                    success = forceStopPackage(admin, data, userHandle);
                                    break;
                                default:
                                    switch (transId) {
                                        case 3020:
                                            success = setComponentEnabledSetting(admin, transId, data, userHandle);
                                            break;
                                        case 3021:
                                            success = clearBackgroundApps(admin, data, userHandle);
                                            break;
                                        case 3022:
                                            success = setLockTaskFeatures(admin, data, userHandle);
                                            break;
                                        case 3023:
                                            success = setLockTaskPackages(admin, data, userHandle);
                                            break;
                                        case 3024:
                                            success = isLockTaskPermitted(admin, data, userHandle);
                                            break;
                                        case 3025:
                                            success = startLockApp(admin, transId, data, userHandle);
                                            break;
                                        default:
                                            switch (transId) {
                                                case 3203:
                                                    success = setOperationHardResetPassword(admin, transId, data, userHandle);
                                                    break;
                                                case 3204:
                                                    success = operationFormatSDCard(admin, transId, data, userHandle);
                                                    break;
                                                case 3205:
                                                    success = reboot(admin, userHandle);
                                                    break;
                                                case 3206:
                                                    success = wipeData(admin, data, userHandle);
                                                    break;
                                                case 3207:
                                                    success = setVolumeLongPressPolicy(admin, transId, data, userHandle);
                                                    break;
                                                default:
                                                    switch (transId) {
                                                        case 3209:
                                                            success = setAppNotification(admin, transId, data, userHandle);
                                                            break;
                                                        case 3210:
                                                            success = setOperationAiKeyPolicy(admin, transId, data, userHandle);
                                                            break;
                                                        case 3211:
                                                            success = setSysTime(admin, transId, data, userHandle);
                                                            break;
                                                        case 3212:
                                                            success = setDesktopWallpaper(admin, transId, data, userHandle);
                                                            break;
                                                        case 3213:
                                                            success = setLockWallpaper(admin, transId, data, userHandle);
                                                            break;
                                                        default:
                                                            switch (transId) {
                                                                case 3308:
                                                                    success = setAdminAccessibilityService(admin, transId, data, userHandle);
                                                                    break;
                                                                case 3309:
                                                                    success = setAdminDeviceAdmin(admin, transId, data, userHandle);
                                                                    break;
                                                                case 3310:
                                                                    success = isAccessibilityServicePermitted(admin, transId, data, userHandle);
                                                                    break;
                                                                default:
                                                                    VLog.w(VIVO_LOG_TAG, "Bad device transId");
                                                                    break;
                                                            }
                                                    }
                                            }
                                    }
                            }
                    }
                }
            } else {
                success = setDeviceOwner(admin, transId, data, userHandle);
            }
        } catch (Exception e) {
            VLog.e(VIVO_LOG_TAG, "invokeDeviceTransaction failed! " + e);
            e.printStackTrace();
        }
        VLog.d(VIVO_LOG_TAG, "invokeDeviceTransaction transId= " + transId + ",success = " + success + ",userHandle = " + userHandle);
        return success;
    }

    public Bundle getInfoDeviceTransaction(ComponentName admin, int transId, Bundle data, int userHandle) {
        VLog.d(VIVO_LOG_TAG, "getInfoDeviceTransaction transId= " + transId + ",userHandle = " + userHandle);
        if (3315 != transId) {
            try {
                checkGetInfoPermission();
            } catch (Exception e) {
                VLog.e(VIVO_LOG_TAG, "getInfoDeviceTransaction failed! " + e);
                e.printStackTrace();
                return null;
            }
        }
        if (transId != 3022) {
            if (transId != 3023) {
                if (transId != 3402) {
                    if (transId != 3403) {
                        switch (transId) {
                            case 308:
                                return captureScreen(admin, userHandle);
                            case 2002:
                                return getWlanConfigurations(admin, userHandle);
                            case 2005:
                                return getWifiMacAddress(admin, userHandle);
                            case 3005:
                                return getRunningAppProcesses(admin, userHandle);
                            case 3107:
                                return getOverrideApns(admin, userHandle);
                            case 3110:
                                return getAlwaysOnVpnPackage(admin, userHandle);
                            case 3203:
                                return getOperationHardResetPassword(admin, transId, userHandle);
                            case 3214:
                                return getTopAppPackage(admin, userHandle);
                            case 3303:
                                return getDeviceOwnerInfo(userHandle);
                            case 3311:
                                return getPermittedAccessibilityServicesForUser(admin, transId, data, userHandle);
                            case 3501:
                                return getRomVersionInfo(admin, userHandle);
                            case 3601:
                                return isDeviceRoot(admin, userHandle);
                            case 3605:
                                return isResetPasswordTokenActive(admin, userHandle);
                            case 3608:
                                return getMaximumTimeToLock(admin, userHandle);
                            case 3610:
                                return getRequiredStrongAuthTimeout(admin, userHandle);
                            case 3612:
                                return getPasswordExpirationTimeout(admin, userHandle);
                            case 3614:
                                return getPasswordQuality(admin, userHandle);
                            case 3616:
                                return getKeyguardDisabledFeatures(admin, userHandle);
                            default:
                                switch (transId) {
                                    case 3008:
                                    case 3009:
                                    case 3010:
                                    case 3011:
                                    case 3012:
                                    case 3013:
                                        return getDefaultApp(admin, transId, userHandle);
                                    default:
                                        switch (transId) {
                                            case 3103:
                                                return getVpnProfile(admin, userHandle);
                                            case 3104:
                                                return getNetworkAppTrafficBytes(admin, transId, data, userHandle);
                                            case 3105:
                                                return addOverrideApn(admin, data, userHandle);
                                            default:
                                                switch (transId) {
                                                    case 3207:
                                                        return getVolumeLongPressPolicy(admin, userHandle);
                                                    case 3208:
                                                        return getBrowserHistory(admin, userHandle);
                                                    case 3209:
                                                        return getAppNotification(admin, data, userHandle);
                                                    case 3210:
                                                        return getOperationAiKeyPolicy(admin, userHandle);
                                                    default:
                                                        switch (transId) {
                                                            case 3306:
                                                                return getProfileOwnerInfo(userHandle);
                                                            case 3307:
                                                                return getOrganizationName();
                                                            case 3308:
                                                                return getAdminAccessibilityService(admin, transId, data, userHandle);
                                                            default:
                                                                switch (transId) {
                                                                    case 3314:
                                                                        return getPermittedAccessibilityServices(admin, userHandle);
                                                                    case 3315:
                                                                        return getSdkInfo(admin, userHandle);
                                                                    case 3316:
                                                                        return getDeviceInfo(admin, userHandle);
                                                                    default:
                                                                        VLog.w(VIVO_LOG_TAG, "Bad device transId");
                                                                        return null;
                                                                }
                                                        }
                                                }
                                        }
                                }
                        }
                    }
                    return getTelecomSMSNum(admin, data, userHandle);
                }
                return getTelecomCallNum(admin, data, userHandle);
            }
            return getLockTaskPackages(admin, userHandle);
        }
        return getLockTaskFeatures(admin, userHandle);
    }

    public void reportExceptionInfo(int infoId, Bundle data, int userHandle) {
        String pkgName;
        List<String> pkgs;
        Exception e;
        Exception e2;
        int i = userHandle;
        VLog.w(VIVO_LOG_TAG, "reportExceptionInfo id = " + infoId);
        if (getCustomType() > 0 && !this.mIsOverseas) {
            checkGetInfoPermission();
            if (data != null && (pkgName = data.getString("package_name")) != null && (pkgs = getAppPersistentAppList(null, i)) != null && pkgs.contains(pkgName)) {
                VivoPolicyData policy = getVivoUserData(i);
                synchronized (getLockObject()) {
                    int i2 = 1;
                    try {
                        try {
                            try {
                                if (infoId != 1503) {
                                    switch (infoId) {
                                        case VivoDisplayPowerControllerImpl.COLOR_FADE_ANIMATION_GLOBAL /* 4000 */:
                                            break;
                                        case 4001:
                                            long time = data.getLong("state_long");
                                            VLog.w(VIVO_LOG_TAG, "reportExceptionInfo wakelock pkgName = " + pkgName + ", time = " + time);
                                            if (policy != null) {
                                                if (policy.mWakeLockExceptionInfolist == null) {
                                                    policy.mWakeLockExceptionInfolist = new ArrayList();
                                                }
                                                if (policy.mWakeLockExceptionInfolist.size() == 0) {
                                                    policy.mWakeLockExceptionInfolist.add(pkgName + ":" + String.valueOf(time));
                                                } else {
                                                    int i3 = 0;
                                                    while (true) {
                                                        if (i3 < policy.mWakeLockExceptionInfolist.size()) {
                                                            String[] oldInfos = policy.mWakeLockExceptionInfolist.get(i3).split(":");
                                                            if (pkgName.equals(oldInfos[0])) {
                                                                long time_old = Long.parseLong(oldInfos[i2]);
                                                                String info_new = pkgName + ":" + String.valueOf(time_old + time);
                                                                policy.mWakeLockExceptionInfolist.set(i3, info_new);
                                                            } else {
                                                                if (i3 == policy.mWakeLockExceptionInfolist.size() - i2) {
                                                                    policy.mWakeLockExceptionInfolist.add(pkgName + ":" + String.valueOf(time));
                                                                }
                                                                i3++;
                                                                i2 = 1;
                                                            }
                                                        }
                                                    }
                                                }
                                                saveEmmInfo(policy, i);
                                            }
                                            return;
                                        case VivoDisplayPowerControllerImpl.COLOR_FADE_ANIMATION_VERTICAL /* 4002 */:
                                            VLog.w(VIVO_LOG_TAG, "reportExceptionInfo location pkgName = " + pkgName);
                                            if (policy != null) {
                                                if (policy.mLocationExceptionInfolist == null) {
                                                    policy.mLocationExceptionInfolist = new ArrayList();
                                                }
                                                if (policy.mLocationExceptionInfolist.size() == 0) {
                                                    policy.mLocationExceptionInfolist.add(pkgName + ":1");
                                                } else {
                                                    int i4 = 0;
                                                    while (true) {
                                                        if (i4 < policy.mLocationExceptionInfolist.size()) {
                                                            String[] oldInfos2 = policy.mLocationExceptionInfolist.get(i4).split(":");
                                                            if (pkgName.equals(oldInfos2[0])) {
                                                                int count_old = Integer.parseInt(oldInfos2[1]);
                                                                String info_new2 = pkgName + ":" + String.valueOf(count_old + 1);
                                                                policy.mLocationExceptionInfolist.set(i4, info_new2);
                                                            } else {
                                                                if (i4 == policy.mLocationExceptionInfolist.size() - 1) {
                                                                    policy.mLocationExceptionInfolist.add(pkgName + ":1");
                                                                }
                                                                i4++;
                                                            }
                                                        }
                                                    }
                                                }
                                                saveEmmInfo(policy, i);
                                            }
                                            return;
                                        case 4003:
                                            VLog.w(VIVO_LOG_TAG, "reportExceptionInfo alarm pkgName = " + pkgName);
                                            if (policy != null) {
                                                if (policy.mAlarmExceptionInfolist == null) {
                                                    policy.mAlarmExceptionInfolist = new ArrayList();
                                                }
                                                if (policy.mAlarmExceptionInfolist.size() == 0) {
                                                    policy.mAlarmExceptionInfolist.add(pkgName + ":1");
                                                } else {
                                                    int i5 = 0;
                                                    while (true) {
                                                        if (i5 < policy.mAlarmExceptionInfolist.size()) {
                                                            String[] oldInfos3 = policy.mAlarmExceptionInfolist.get(i5).split(":");
                                                            if (pkgName.equals(oldInfos3[0])) {
                                                                int count_old2 = Integer.parseInt(oldInfos3[1]);
                                                                String info_new3 = pkgName + ":" + String.valueOf(count_old2 + 1);
                                                                policy.mAlarmExceptionInfolist.set(i5, info_new3);
                                                            } else {
                                                                if (i5 == policy.mAlarmExceptionInfolist.size() - 1) {
                                                                    policy.mAlarmExceptionInfolist.add(pkgName + ":1");
                                                                }
                                                                i5++;
                                                            }
                                                        }
                                                    }
                                                }
                                                saveEmmInfo(policy, i);
                                            }
                                            return;
                                        case 4004:
                                            VLog.w(VIVO_LOG_TAG, "reportExceptionInfo crash pkgName = " + pkgName);
                                            int type = data.getInt("state_int");
                                            reportVcodeException(pkgName, type);
                                            return;
                                        default:
                                            return;
                                    }
                                }
                                double num = data.getDouble("state_double");
                                boolean isKilled = data.getBoolean("state_boolean");
                                VLog.w(VIVO_LOG_TAG, "reportExceptionInfo power pkgName = " + pkgName + ", num = " + num + ", isKilled = " + isKilled);
                                if (policy != null) {
                                    if (policy.mPowerExceptionInfolist == null) {
                                        policy.mPowerExceptionInfolist = new ArrayList();
                                    }
                                    StringBuilder sb = new StringBuilder();
                                    sb.append(pkgName);
                                    sb.append(":");
                                    sb.append(String.valueOf(num));
                                    sb.append(":");
                                    sb.append(isKilled ? "1" : "0");
                                    String info = sb.toString();
                                    if (policy.mPowerExceptionInfolist.size() == 0) {
                                        policy.mPowerExceptionInfolist.add(info + ":1");
                                    } else {
                                        int i6 = 0;
                                        while (true) {
                                            if (i6 < policy.mPowerExceptionInfolist.size()) {
                                                try {
                                                    String[] oldInfos4 = policy.mPowerExceptionInfolist.get(i6).split(":");
                                                    if (pkgName.equals(oldInfos4[0])) {
                                                        double average_old = Double.parseDouble(oldInfos4[1]);
                                                        int count_old3 = Integer.parseInt(oldInfos4[3]);
                                                        double average_new = ((count_old3 * average_old) + num) / (count_old3 + 1);
                                                        String isKilled_new = isKilled ? "1" : oldInfos4[2];
                                                        String info_new4 = pkgName + ":" + String.valueOf(average_new) + ":" + isKilled_new + ":" + String.valueOf(count_old3 + 1);
                                                        policy.mPowerExceptionInfolist.set(i6, info_new4);
                                                    } else {
                                                        try {
                                                            List<String> pkgs2 = pkgs;
                                                            double num2 = num;
                                                            if (i6 == policy.mPowerExceptionInfolist.size() - 1) {
                                                                policy.mPowerExceptionInfolist.add(info + ":1");
                                                            }
                                                            i6++;
                                                            i = userHandle;
                                                            pkgs = pkgs2;
                                                            num = num2;
                                                        } catch (Exception e3) {
                                                            e = e3;
                                                            e2 = e;
                                                            e2.printStackTrace();
                                                            return;
                                                        } catch (Throwable th) {
                                                            th = th;
                                                            e = th;
                                                            throw e;
                                                        }
                                                    }
                                                } catch (Exception e4) {
                                                    e = e4;
                                                    e2 = e;
                                                    e2.printStackTrace();
                                                    return;
                                                } catch (Throwable th2) {
                                                    th = th2;
                                                    e = th;
                                                    throw e;
                                                }
                                            }
                                        }
                                    }
                                    try {
                                        saveEmmInfo(policy, userHandle);
                                    } catch (Exception e5) {
                                        e = e5;
                                        e2 = e;
                                        e2.printStackTrace();
                                        return;
                                    }
                                }
                                return;
                            } catch (Exception e6) {
                                e = e6;
                            } catch (Throwable th3) {
                                th = th3;
                            }
                        } catch (Exception e7) {
                            e2 = e7;
                        } catch (Throwable th4) {
                            e = th4;
                        }
                    } catch (Throwable th5) {
                        th = th5;
                    }
                }
            }
            return;
        }
        VLog.e(VIVO_LOG_TAG, "reportExceptionInfo CustomType:" + getCustomType() + ", isOvs:" + this.mIsOverseas);
    }

    public List<String> getExceptionInfo(int userHandle) {
        List<String> infoList = new ArrayList<>();
        synchronized (getLockObject()) {
            VivoPolicyData policy = getVivoUserData(userHandle);
            if (policy.mPowerExceptionInfolist != null && policy.mPowerExceptionInfolist.size() > 0) {
                for (String s : policy.mPowerExceptionInfolist) {
                    String info = "power:" + s;
                    infoList.add(info);
                }
            }
            if (policy.mAlarmExceptionInfolist != null && policy.mAlarmExceptionInfolist.size() > 0) {
                for (String s2 : policy.mAlarmExceptionInfolist) {
                    String info2 = "alarm:" + s2;
                    infoList.add(info2);
                }
            }
            if (policy.mLocationExceptionInfolist != null && policy.mLocationExceptionInfolist.size() > 0) {
                for (String s3 : policy.mLocationExceptionInfolist) {
                    String info3 = "location:" + s3;
                    infoList.add(info3);
                }
            }
            if (policy.mWakeLockExceptionInfolist != null && policy.mWakeLockExceptionInfolist.size() > 0) {
                for (String s4 : policy.mWakeLockExceptionInfolist) {
                    String info4 = "wakelock:" + s4;
                    infoList.add(info4);
                }
            }
        }
        return infoList;
    }

    private boolean setWlanPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_PERI_WLAN", userHandle);
        if (policy == 0 || policy == 1 || policy == 2 || policy == 6 || policy == 5) {
            synchronized (getLockObject()) {
                VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
                if (ap != null) {
                    if (ap.wlanPolicy != policy) {
                        long callingId = Binder.clearCallingIdentity();
                        try {
                            WifiManager mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
                            if (policy == 5 && (ap.wlanPolicy == 0 || ap.wlanPolicy == 6)) {
                                if (!mWifiManager.isWifiEnabled()) {
                                    mWifiManager.setWifiEnabled(true);
                                }
                            } else if (policy == 6 && (ap.wlanPolicy == 0 || ap.wlanPolicy == 5)) {
                                if (mWifiManager.isWifiEnabled()) {
                                    mWifiManager.setWifiEnabled(false);
                                }
                            } else if (policy == 1) {
                                if (mWifiManager.isWifiEnabled()) {
                                    mWifiManager.setWifiEnabled(false);
                                }
                            } else if (policy == 2) {
                                if (!mWifiManager.isWifiEnabled()) {
                                    mWifiManager.setWifiEnabled(true);
                                }
                            } else if (policy != 0) {
                                Binder.restoreCallingIdentity(callingId);
                                return false;
                            }
                            Binder.restoreCallingIdentity(callingId);
                            ap.wlanPolicy = policy;
                            saveVivoSettingsLocked(polId, userHandle);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Binder.restoreCallingIdentity(callingId);
                            return false;
                        }
                    }
                    return true;
                }
                return false;
            }
        }
        throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
    }

    private int getWlanPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        WifiManager mWifiManager;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            if (ap != null) {
                if ((ap.wlanPolicy == 5 || ap.wlanPolicy == 6) && this.mIsBootCompleted && (mWifiManager = (WifiManager) this.mContext.getSystemService("wifi")) != null) {
                    return mWifiManager.isWifiEnabled() ? 5 : 6;
                }
            }
            return ap != null ? ap.wlanPolicy : 0;
        }
    }

    private boolean setWlanApPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_PERI_WLAN", userHandle);
        if (policy < 0 || policy > 2) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.wlanApPolicy != policy) {
                    ap.wlanApPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getWlanApPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.wlanApPolicy : 0;
        }
        return i;
    }

    private boolean setWlanBWPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_PERI_WLAN", userHandle);
        if (policy != 0 && policy != 3 && policy != 4) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.wlanBWPolicy != policy) {
                    ap.wlanBWPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getWlanBWPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.wlanBWPolicy : 0;
        }
        return i;
    }

    private boolean setWlanApBWPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_PERI_WLAN", userHandle);
        if (policy != 0 && policy != 3 && policy != 4) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.wlanApBWPolicy != policy) {
                    ap.wlanApBWPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getWlanApBWPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.wlanApBWPolicy : 0;
        }
        return i;
    }

    private boolean setWlanBWList(ComponentName admin, int listId, List<String> keys, boolean isAdd, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_PERI_WLAN", userHandle);
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        boolean success = false;
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (listId == 1001) {
                    if (ap.wlanBlist == null) {
                        ap.wlanBlist = new ArrayList();
                    }
                    success = VivoUtils.addItemsFromList(ap.wlanBlist, keys, isAdd);
                } else if (listId == 1002) {
                    if (ap.wlanWlist == null) {
                        ap.wlanWlist = new ArrayList();
                    }
                    success = VivoUtils.addItemsFromList(ap.wlanWlist, keys, isAdd);
                }
                saveVivoSettingsLocked(listId, userHandle);
            }
        }
        return success;
    }

    private List<String> getWlanBWList(ComponentName admin, int listId, int userHandle) {
        VivoActiveAdmin ap;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            if (ap != null) {
                if (listId == 1001) {
                    return ap.wlanBlist;
                } else if (listId == 1002) {
                    return ap.wlanWlist;
                }
            }
            return null;
        }
    }

    private boolean setWlanApBWList(ComponentName admin, int listId, List<String> keys, boolean isAdd, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_PERI_WLAN", userHandle);
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        boolean success = false;
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (listId == 1003) {
                    if (ap.wlanApBlist == null) {
                        ap.wlanApBlist = new ArrayList();
                    }
                    success = VivoUtils.addItemsFromList(ap.wlanApBlist, keys, isAdd);
                } else if (listId == 1004) {
                    if (ap.wlanApWlist == null) {
                        ap.wlanApWlist = new ArrayList();
                    }
                    success = VivoUtils.addItemsFromList(ap.wlanApWlist, keys, isAdd);
                }
                saveVivoSettingsLocked(listId, userHandle);
            }
        }
        return success;
    }

    private List<String> getWlanApBWList(ComponentName admin, int listId, int userHandle) {
        VivoActiveAdmin ap;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            if (ap != null) {
                if (listId == 1003) {
                    return ap.wlanApBlist;
                } else if (listId == 1004) {
                    return ap.wlanApWlist;
                }
            }
            return null;
        }
    }

    private boolean setWlanConfigurations(ComponentName admin, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_PERI_WLAN", userHandle);
        if (data == null || data.getSize() == 0) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap == null) {
                return false;
            }
            long callingId = Binder.clearCallingIdentity();
            ArrayList<WifiConfiguration> arrayList = data.getParcelableArrayList("wificonfig");
            if (arrayList.isEmpty()) {
                throw new IllegalArgumentException("IllegalArgumentException:setWlanConfigurations is illegal!");
            }
            WifiManager mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
            Iterator<WifiConfiguration> it = arrayList.iterator();
            while (it.hasNext()) {
                WifiConfiguration conf = it.next();
                int networkId = mWifiManager.addNetwork(conf);
                if (networkId != -1) {
                    mWifiManager.enableNetwork(networkId, true);
                } else {
                    Binder.restoreCallingIdentity(callingId);
                    return false;
                }
            }
            Binder.restoreCallingIdentity(callingId);
            return true;
        }
    }

    private Bundle getWlanConfigurations(ComponentName admin, int userHandle) {
        Bundle bundle;
        synchronized (getLockObject()) {
            bundle = new Bundle();
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                long callingId = Binder.clearCallingIdentity();
                WifiManager mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
                ArrayList<WifiConfiguration> arrayList = (ArrayList) mWifiManager.getConfiguredNetworks();
                bundle.putParcelableArrayList("wificonfig", arrayList);
                Binder.restoreCallingIdentity(callingId);
            }
        }
        return bundle;
    }

    private boolean delWlanConfigurations(ComponentName admin, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_PERI_WLAN", userHandle);
        if (data == null || data.getSize() == 0) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                long callingId = Binder.clearCallingIdentity();
                int networkId = data.getInt("state_int");
                WifiManager mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
                if (networkId != -1) {
                    mWifiManager.disableNetwork(networkId);
                    boolean removeNetwork = mWifiManager.removeNetwork(networkId);
                    Binder.restoreCallingIdentity(callingId);
                    return removeNetwork;
                }
                Binder.restoreCallingIdentity(callingId);
            }
            return false;
        }
    }

    private boolean setWlanConfigPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        boolean z;
        checkCallingEmmPermission(admin, "EMM_PERI_WLAN", userHandle);
        if (policy != 0 && policy != 1) {
            throw new IllegalArgumentException("IllegalArgumentException: setWlanConfigPolicy input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap == null) {
                return false;
            }
            DevicePolicyManagerService devicePolicyManagerService = this.mService;
            if (policy == 1) {
                z = true;
            } else {
                z = false;
            }
            devicePolicyManagerService.setUserRestriction(admin, "no_config_wifi", z, false);
            return true;
        }
    }

    private int getWlanConfigPolicy(ComponentName admin, int userHandle) {
        synchronized (getLockObject()) {
            Bundle restrictions = this.mService.getUserRestrictions(admin, false);
            if (restrictions == null) {
                return 0;
            }
            boolean isRestrict = restrictions.getBoolean("no_config_wifi", false);
            return isRestrict ? 1 : 0;
        }
    }

    private Bundle getWifiMacAddress(ComponentName admin, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_PERI_WLAN", userHandle);
        synchronized (getLockObject()) {
            Bundle bundle = new Bundle();
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                String addr = this.mService.getWifiMacAddress(admin);
                bundle.putString("state_string", addr);
                return bundle;
            }
            return null;
        }
    }

    private boolean setBluetoothPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_PERI_BLUETOOTH", userHandle);
        if (policy == 0 || policy == 1 || policy == 2 || policy == 6 || policy == 5) {
            synchronized (getLockObject()) {
                VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
                if (ap != null) {
                    if (ap.bluetoothPolicy != policy) {
                        this.mService.setUserRestriction(admin, "no_bluetooth", policy == 1, false);
                        long callingId = Binder.clearCallingIdentity();
                        try {
                            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                            if (policy == 5 && (ap.bluetoothPolicy == 0 || ap.bluetoothPolicy == 6)) {
                                if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
                                    mBluetoothAdapter.enable();
                                }
                            } else if (policy == 6 && (ap.bluetoothPolicy == 0 || ap.bluetoothPolicy == 5)) {
                                if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
                                    mBluetoothAdapter.disable();
                                }
                            } else if (policy == 1) {
                                if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
                                    mBluetoothAdapter.disable();
                                }
                            } else if (policy == 2) {
                                if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
                                    mBluetoothAdapter.enable();
                                }
                            } else if (policy != 0) {
                                Binder.restoreCallingIdentity(callingId);
                                return false;
                            }
                            Binder.restoreCallingIdentity(callingId);
                            ap.bluetoothPolicy = policy;
                            saveVivoSettingsLocked(polId, userHandle);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Binder.restoreCallingIdentity(callingId);
                            return false;
                        }
                    }
                    return true;
                }
                return false;
            }
        }
        throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
    }

    private int getBluetoothPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        BluetoothAdapter mBluetoothAdapter;
        synchronized (getLockObject()) {
            if (admin == null) {
                admin = getVivoAdminUncheckedLocked(userHandle);
                if (admin == null) {
                    return 0;
                }
                this.mIsEmmAPI = true;
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            } else {
                ap = getVivoActiveAdminUncheckedLocked(admin, userHandle);
            }
            Bundle restrictions = this.mService.getUserRestrictions(admin, false);
            this.mIsEmmAPI = false;
            boolean isRestrict = false;
            if (restrictions != null) {
                isRestrict = restrictions.getBoolean("no_bluetooth", false);
            }
            if (isRestrict) {
                return 1;
            }
            if (ap != null) {
                if ((ap.bluetoothPolicy == 5 || ap.bluetoothPolicy == 6) && this.mIsBootCompleted && (mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()) != null) {
                    return mBluetoothAdapter.isEnabled() ? 5 : 6;
                }
            }
            return ap != null ? ap.bluetoothPolicy : 0;
        }
    }

    private boolean setBluetoothApPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_PERI_BLUETOOTH", userHandle);
        if (policy < 0 || policy > 2) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.bluetoothApPolicy != policy) {
                    ap.bluetoothApPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getBluetoothApPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.bluetoothApPolicy : 0;
        }
        return i;
    }

    private boolean setBluetoothBWPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_PERI_BLUETOOTH", userHandle);
        if (policy != 0 && policy != 3 && policy != 4) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.bluetoothBWPolicy != policy) {
                    ap.bluetoothBWPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getBluetoothBWPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.bluetoothBWPolicy : 0;
        }
        return i;
    }

    private boolean setBluetoothBWList(ComponentName admin, int listId, List<String> keys, boolean isAdd, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_PERI_BLUETOOTH", userHandle);
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        boolean success = false;
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (listId == 1005) {
                    if (ap.bluetoothBlist == null) {
                        ap.bluetoothBlist = new ArrayList();
                    }
                    success = VivoUtils.addItemsFromList(ap.bluetoothBlist, keys, isAdd);
                } else if (listId == 1006) {
                    if (ap.bluetoothWlist == null) {
                        ap.bluetoothWlist = new ArrayList();
                    }
                    success = VivoUtils.addItemsFromList(ap.bluetoothWlist, keys, isAdd);
                }
                saveVivoSettingsLocked(listId, userHandle);
            }
        }
        return success;
    }

    private List<String> getBluetoothBWList(ComponentName admin, int listId, int userHandle) {
        VivoActiveAdmin ap;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            if (ap != null) {
                if (listId == 1005) {
                    return ap.bluetoothBlist;
                } else if (listId == 1006) {
                    return ap.bluetoothWlist;
                }
            }
            return null;
        }
    }

    private boolean setBluetoothConfigPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        boolean z;
        checkCallingEmmPermission(admin, "EMM_PERI_BLUETOOTH", userHandle);
        if (policy != 0 && policy != 1) {
            throw new IllegalArgumentException("IllegalArgumentException: setBluetoothConfigPolicy input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap == null) {
                return false;
            }
            DevicePolicyManagerService devicePolicyManagerService = this.mService;
            if (policy == 1) {
                z = true;
            } else {
                z = false;
            }
            devicePolicyManagerService.setUserRestriction(admin, "no_config_bluetooth", z, false);
            return true;
        }
    }

    private int getBluetoothConfigPolicy(ComponentName admin, int userHandle) {
        synchronized (getLockObject()) {
            Bundle restrictions = this.mService.getUserRestrictions(admin, false);
            if (restrictions == null) {
                return 0;
            }
            boolean isRestrict = restrictions.getBoolean("no_config_bluetooth", false);
            return isRestrict ? 1 : 0;
        }
    }

    private boolean setBluetoothSharingPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        boolean z;
        checkCallingEmmPermission(admin, "EMM_PERI_BLUETOOTH", userHandle);
        if (policy != 0 && policy != 1) {
            throw new IllegalArgumentException("IllegalArgumentException: setBluetoothSharingPolicy input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap == null) {
                return false;
            }
            DevicePolicyManagerService devicePolicyManagerService = this.mService;
            if (policy == 1) {
                z = true;
            } else {
                z = false;
            }
            devicePolicyManagerService.setUserRestriction(admin, "no_bluetooth_sharing", z, false);
            return true;
        }
    }

    private int getBluetoothSharingPolicy(ComponentName admin, int userHandle) {
        synchronized (getLockObject()) {
            Bundle restrictions = this.mService.getUserRestrictions(admin, false);
            if (restrictions == null) {
                return 0;
            }
            boolean isRestrict = restrictions.getBoolean("no_bluetooth_sharing", false);
            return isRestrict ? 1 : 0;
        }
    }

    private boolean setUsbTransferPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_PERI_USB", userHandle);
        if (policy != 0 && policy != 1 && policy != 10 && policy != 11) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                long callingId = Binder.clearCallingIdentity();
                if (ap.usbTransferPolicy != policy) {
                    ap.usbTransferPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                Binder.restoreCallingIdentity(callingId);
                return true;
            }
            return false;
        }
    }

    private int getUsbTransferPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.usbTransferPolicy : 0;
        }
        return i;
    }

    private boolean setUsbApPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_PERI_USB", userHandle);
        if (policy < 0 || policy > 2) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap == null) {
                return false;
            }
            if (ap.usbApPolicy != policy) {
                ap.usbApPolicy = policy;
                if (policy == 1) {
                    long callingId = Binder.clearCallingIdentity();
                    try {
                        if (this.mCM == null) {
                            this.mCM = (ConnectivityManager) this.mContext.getSystemService("connectivity");
                        }
                        this.mCM.setUsbTethering(false);
                        Binder.restoreCallingIdentity(callingId);
                    } catch (Exception e) {
                        VLog.e(VIVO_LOG_TAG, "setUsbApPolicy failed");
                        e.printStackTrace();
                        Binder.restoreCallingIdentity(callingId);
                    }
                }
                saveVivoSettingsLocked(polId, userHandle);
            }
            return true;
        }
    }

    private int getUsbApPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.usbApPolicy : 0;
        }
        return i;
    }

    private boolean setUsbOtgPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_PERI_USB", userHandle);
        if (policy < 0 || policy > 2) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap == null) {
                return false;
            }
            if (ap.usbOtgPolicy != policy) {
                ap.usbOtgPolicy = policy;
                long callingId = Binder.clearCallingIdentity();
                try {
                } catch (Exception e) {
                    VLog.e(VIVO_LOG_TAG, "setUsbOtgPolicy failed");
                    e.printStackTrace();
                    Binder.restoreCallingIdentity(callingId);
                }
                if (policy == 1) {
                    setOtgEnable(false);
                } else {
                    if (policy == 0) {
                        setOtgEnable(true);
                    }
                    Binder.restoreCallingIdentity(callingId);
                    saveVivoSettingsLocked(polId, userHandle);
                }
                Binder.restoreCallingIdentity(callingId);
                saveVivoSettingsLocked(polId, userHandle);
            }
            return true;
        }
    }

    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:15:0x0037 -> B:30:0x0047). Please submit an issue!!! */
    private void setOtgEnable(boolean enable) {
        File file = new File(PATH_OTG);
        if (!file.exists()) {
            VLog.e(VIVO_LOG_TAG, "path:/sys/kernel/audio-max20328/unuseirq is not exist");
            return;
        }
        BufferedWriter bw = null;
        try {
            try {
                try {
                    bw = new BufferedWriter(new FileWriter(file));
                    bw.write(enable ? "0" : "1");
                    bw.flush();
                    bw.close();
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    if (bw != null) {
                        bw.close();
                    }
                }
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        } catch (Throwable th) {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e3) {
                    e3.printStackTrace();
                }
            }
            throw th;
        }
    }

    private int getUsbOtgPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.usbOtgPolicy : 0;
        }
        return i;
    }

    private boolean setUsbDebugPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        boolean z;
        checkCallingEmmPermission(admin, "EMM_PERI_USB", userHandle);
        if (policy != 0 && policy != 1) {
            throw new IllegalArgumentException("IllegalArgumentException: setUsbDebugPolicy input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap == null) {
                return false;
            }
            DevicePolicyManagerService devicePolicyManagerService = this.mService;
            if (policy == 1) {
                z = true;
            } else {
                z = false;
            }
            devicePolicyManagerService.setUserRestriction(admin, "no_debugging_features", z, false);
            return true;
        }
    }

    private int getUsbDebugPolicy(ComponentName admin, int userHandle) {
        synchronized (getLockObject()) {
            Bundle restrictions = this.mService.getUserRestrictions(admin, false);
            if (restrictions == null) {
                return 0;
            }
            boolean isRestrict = restrictions.getBoolean("no_debugging_features", false);
            return isRestrict ? 1 : 0;
        }
    }

    private boolean setMicPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_PERI_MICROPHONE", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.micPolicy != policy) {
                    ap.micPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getMicPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.micPolicy : 0;
        }
        return i;
    }

    private boolean setSpeakerPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_PERI_SPEAKER", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.speakerPolicy != policy) {
                    ap.speakerPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getSpeakerPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.speakerPolicy : 0;
        }
        return i;
    }

    private boolean setFlashLightPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_PERI_CAMERA", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.flashLightPolicy != policy) {
                    ap.flashLightPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getFlashLightPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.flashLightPolicy : 0;
        }
        return i;
    }

    private boolean setAppLocationPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_PERI_LOCATION", userHandle);
        if (policy == 0 || policy == 1 || policy == 2 || policy == 6 || policy == 5) {
            synchronized (getLockObject()) {
                VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
                if (ap != null) {
                    if (ap.locationPolicy != policy) {
                        this.mService.setUserRestriction(admin, "no_share_location", policy == 1, false);
                        long callingId = Binder.clearCallingIdentity();
                        if (policy == 5) {
                            try {
                                if (ap.locationPolicy == 0 || ap.locationPolicy == 6) {
                                    Settings.Secure.putIntForUser(this.mContentResolver, "location_mode", 3, userHandle);
                                    Binder.restoreCallingIdentity(callingId);
                                    ap.locationPolicy = policy;
                                    saveVivoSettingsLocked(polId, userHandle);
                                }
                            } catch (Exception e) {
                                VLog.e(VIVO_LOG_TAG, "setAppLocationPolicy exception " + e);
                                Binder.restoreCallingIdentity(callingId);
                                return false;
                            }
                        }
                        if (policy == 6 && (ap.locationPolicy == 0 || ap.locationPolicy == 5)) {
                            Settings.Secure.putIntForUser(this.mContentResolver, "location_mode", 0, userHandle);
                        } else if (policy != 1) {
                            if (policy == 2) {
                                Settings.Secure.putIntForUser(this.mContentResolver, "location_mode", 3, userHandle);
                            } else if (policy != 0) {
                                Binder.restoreCallingIdentity(callingId);
                                return false;
                            }
                        }
                        Binder.restoreCallingIdentity(callingId);
                        ap.locationPolicy = policy;
                        saveVivoSettingsLocked(polId, userHandle);
                    }
                    return true;
                }
                return false;
            }
        }
        throw new IllegalArgumentException("IllegalArgumentException: setAppLocationPolicy input is illegal!");
    }

    private int getAppLocationPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        if (admin == null) {
            admin = getVivoAdminUncheckedLocked(userHandle);
            if (admin == null) {
                return 0;
            }
            this.mIsEmmAPI = true;
        }
        Bundle restrictions = this.mService.getUserRestrictions(admin, false);
        this.mIsEmmAPI = false;
        boolean isRestrict = false;
        if (restrictions != null) {
            isRestrict = restrictions.getBoolean("no_share_location", false);
        }
        if (isRestrict) {
            return 1;
        }
        if (admin != null) {
            ap = getVivoActiveAdminUncheckedLocked(admin, userHandle);
        } else {
            ap = getVivoUserData(userHandle).mVivoActiveAdmin;
        }
        if (ap != null && (ap.locationPolicy == 5 || ap.locationPolicy == 6)) {
            boolean onOff = 3 == Settings.Secure.getIntForUser(this.mContentResolver, "location_mode", 0, userHandle);
            return onOff ? 5 : 6;
        } else if (ap != null) {
            return ap.locationPolicy;
        } else {
            return 0;
        }
    }

    private boolean setCameraPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        boolean z;
        checkCallingEmmPermission(admin, "EMM_PERI_CAMERA", userHandle);
        if (policy != 0 && policy != 1) {
            throw new IllegalArgumentException("IllegalArgumentException: setCameraPolicy input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap == null) {
                return false;
            }
            DevicePolicyManagerService devicePolicyManagerService = this.mService;
            if (policy == 1) {
                z = true;
            } else {
                z = false;
            }
            devicePolicyManagerService.setCameraDisabled(admin, z, false);
            return true;
        }
    }

    private int getCameraPolicy(ComponentName admin, int userHandle) {
        int i;
        synchronized (getLockObject()) {
            boolean result = this.mService.getCameraDisabled(admin, userHandle, false);
            i = result ? 1 : 0;
        }
        return i;
    }

    private boolean setExternalStoragePolicy(ComponentName admin, int polId, int policy, int userHandle) {
        boolean z;
        checkCallingEmmPermission(admin, "EMM_PERI_EXTERNAL_STORAGE", userHandle);
        if (policy != 0 && policy != 1) {
            throw new IllegalArgumentException("IllegalArgumentException: setExternalStoragePolicy input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap == null) {
                return false;
            }
            DevicePolicyManagerService devicePolicyManagerService = this.mService;
            if (policy == 1) {
                z = true;
            } else {
                z = false;
            }
            devicePolicyManagerService.setUserRestriction(admin, "no_physical_media", z, false);
            return true;
        }
    }

    private int getExternalStoragePolicy(ComponentName admin, int userHandle) {
        synchronized (getLockObject()) {
            Bundle restrictions = this.mService.getUserRestrictions(admin, false);
            if (restrictions == null) {
                return 0;
            }
            boolean isRestrict = restrictions.getBoolean("no_physical_media", false);
            return isRestrict ? 1 : 0;
        }
    }

    private boolean setNfcSharingPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        boolean z;
        checkCallingEmmPermission(admin, "EMM_PERI_NFC", userHandle);
        if (policy != 0 && policy != 1) {
            throw new IllegalArgumentException("IllegalArgumentException: setNfcSharingPolicy input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap == null) {
                return false;
            }
            DevicePolicyManagerService devicePolicyManagerService = this.mService;
            if (policy == 1) {
                z = true;
            } else {
                z = false;
            }
            devicePolicyManagerService.setUserRestriction(admin, "no_outgoing_beam", z, false);
            return true;
        }
    }

    private int getNfcSharingPolicy(ComponentName admin, int userHandle) {
        synchronized (getLockObject()) {
            Bundle restrictions = this.mService.getUserRestrictions(admin, false);
            if (restrictions == null) {
                return 0;
            }
            boolean isRestrict = restrictions.getBoolean("no_outgoing_beam", false);
            return isRestrict ? 1 : 0;
        }
    }

    private boolean setNfcAllPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_PERI_NFC", userHandle);
        if (policy < 0 || policy > 2) {
            throw new IllegalArgumentException("IllegalArgumentException: setNfcAllPolicy is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.nfcAllPolicy != policy) {
                    ap.nfcAllPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                    long token = Binder.clearCallingIdentity();
                    try {
                        IBinder nfcServiceBinder = ServiceManager.getService("nfc");
                        if (nfcServiceBinder == null) {
                            VLog.w(VIVO_LOG_TAG, "Could not connect to NFC service to setNfcAllPolicy forbidden");
                            Binder.restoreCallingIdentity(token);
                            return false;
                        }
                        INfcAdapter nfcAdapterRaw = INfcAdapter.Stub.asInterface(nfcServiceBinder);
                        if (policy == 1) {
                            nfcAdapterRaw.disable(true);
                        } else if (policy == 2) {
                            nfcAdapterRaw.enable();
                            nfcAdapterRaw.enableNdefPush();
                        }
                        Binder.restoreCallingIdentity(token);
                    } catch (Exception e) {
                        VLog.w(VIVO_LOG_TAG, "Could not notify NFC service, remote exception: " + e);
                        Binder.restoreCallingIdentity(token);
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
    }

    private int getNfcAllPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.nfcAllPolicy : 0;
        }
        return i;
    }

    private boolean setPhoneCallPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_TELE_CALL", userHandle);
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.phoneCallPolicy != policy) {
                    ap.phoneCallPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getPhoneCallPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.phoneCallPolicy : 0;
        }
        return i;
    }

    private boolean setPhoneSmsPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_TELE_MMS", userHandle);
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.phoneSmsPolicy != policy) {
                    ap.phoneSmsPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getPhoneSmsPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.phoneSmsPolicy : 0;
        }
        return i;
    }

    private boolean setPhoneMmsPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_TELE_MMS", userHandle);
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.phoneMmsPolicy != policy) {
                    ap.phoneMmsPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getPhoneMmsPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.phoneMmsPolicy : 0;
        }
        return i;
    }

    private boolean setPhoneSimSlotPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_TELECOM", userHandle);
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.phoneSimSlotPolicy != policy) {
                    ap.phoneSimSlotPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getPhoneSimSlotPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.phoneSimSlotPolicy : 0;
        }
        return i;
    }

    private boolean setPhoneCallBWPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_TELE_CALL", userHandle);
        if (policy != 0 && policy != 3 && policy != 4) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.phoneCallBWPolicy != policy) {
                    ap.phoneCallBWPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getPhoneCallBWPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.phoneCallBWPolicy : 0;
        }
        return i;
    }

    private boolean setPhoneSmsBWPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_TELE_MMS", userHandle);
        if (policy != 0 && policy != 3 && policy != 4) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.phoneSmsBWPolicy != policy) {
                    ap.phoneSmsBWPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getPhoneSmsBWPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.phoneSmsBWPolicy : 0;
        }
        return i;
    }

    private boolean setPhoneCallBWList(ComponentName admin, int listId, List<String> keys, boolean isAdd, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_TELE_CALL", userHandle);
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        boolean success = false;
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (listId == 1301) {
                    if (ap.phoneCallBlist == null) {
                        ap.phoneCallBlist = new ArrayList();
                    }
                    success = VivoUtils.addItemsFromPhoneNumList(ap.phoneCallBlist, keys, isAdd);
                } else if (listId == 1302) {
                    if (ap.phoneCallWlist == null) {
                        ap.phoneCallWlist = new ArrayList();
                    }
                    success = VivoUtils.addItemsFromPhoneNumList(ap.phoneCallWlist, keys, isAdd);
                }
                saveVivoSettingsLocked(listId, userHandle);
            }
        }
        return success;
    }

    private List<String> getPhoneCallBWList(ComponentName admin, int listId, int userHandle) {
        VivoActiveAdmin ap;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            if (ap != null) {
                if (listId == 1301) {
                    return ap.phoneCallBlist;
                } else if (listId == 1302) {
                    return ap.phoneCallWlist;
                }
            }
            return null;
        }
    }

    private boolean setPhoneSmsBWList(ComponentName admin, int listId, List<String> keys, boolean isAdd, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_TELE_MMS", userHandle);
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        boolean success = false;
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (listId == 1303) {
                    if (ap.phoneSmsBlist == null) {
                        ap.phoneSmsBlist = new ArrayList();
                    }
                    success = VivoUtils.addItemsFromPhoneNumList(ap.phoneSmsBlist, keys, isAdd);
                } else if (listId == 1304) {
                    if (ap.phoneSmsWlist == null) {
                        ap.phoneSmsWlist = new ArrayList();
                    }
                    success = VivoUtils.addItemsFromPhoneNumList(ap.phoneSmsWlist, keys, isAdd);
                }
                saveVivoSettingsLocked(listId, userHandle);
            }
        }
        return success;
    }

    private List<String> getPhoneSmsBWList(ComponentName admin, int listId, int userHandle) {
        VivoActiveAdmin ap;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            if (ap != null) {
                if (listId == 1303) {
                    return ap.phoneSmsBlist;
                } else if (listId == 1304) {
                    return ap.phoneSmsWlist;
                }
            }
            return null;
        }
    }

    private boolean setTelecomEndCall(ComponentName admin, int transId, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_TELE_CALL", userHandle);
        synchronized (getLockObject()) {
            long callingId = Binder.clearCallingIdentity();
            try {
                TelecomManager mTelecomManager = (TelecomManager) this.mContext.getSystemService("telecom");
                if (mTelecomManager == null) {
                    Binder.restoreCallingIdentity(callingId);
                    return true;
                }
                boolean endCall = mTelecomManager.endCall();
                Binder.restoreCallingIdentity(callingId);
                return endCall;
            } catch (Exception e) {
                VLog.e(VIVO_LOG_TAG, "setTelecomEndCall fail! " + e);
                Binder.restoreCallingIdentity(callingId);
                return false;
            }
        }
    }

    private boolean setTelecomAnswerCall(ComponentName admin, int transId, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_TELE_CALL", userHandle);
        synchronized (getLockObject()) {
            long callingId = Binder.clearCallingIdentity();
            try {
                TelecomManager mTelecomManager = (TelecomManager) this.mContext.getSystemService("telecom");
                if (mTelecomManager != null) {
                    mTelecomManager.acceptRingingCall();
                }
                Binder.restoreCallingIdentity(callingId);
            } catch (Exception e) {
                VLog.e(VIVO_LOG_TAG, "setTelecomAnswerCall fail! " + e);
                Binder.restoreCallingIdentity(callingId);
                return false;
            }
        }
        return true;
    }

    private List<String> getDeviceInfo2(ComponentName admin, int userHandle) {
        String[] arr = new String[2];
        return Arrays.asList(arr);
    }

    private List<String> getDeviceInfo3(ComponentName admin, int userHandle) {
        String[] arr = new String[2];
        return Arrays.asList(arr);
    }

    private boolean setAirplaneModePolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_TELECOM", userHandle);
        if (policy == 0 || policy == 1 || policy == 5 || policy == 6) {
            synchronized (getLockObject()) {
                VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
                if (ap != null) {
                    if (ap.airplanePolicy != policy) {
                        this.mService.setUserRestriction(admin, "no_airplane_mode", policy == 1, false);
                        long callingId = Binder.clearCallingIdentity();
                        try {
                            if (policy == 5) {
                                Settings.Global.putString(this.mContentResolver, "airplane_mode_on", "1");
                            } else if (policy == 6) {
                                Settings.Global.putString(this.mContentResolver, "airplane_mode_on", "0");
                            }
                            Intent intent = new Intent("android.intent.action.AIRPLANE_MODE");
                            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
                            Binder.restoreCallingIdentity(callingId);
                            ap.airplanePolicy = policy;
                            saveVivoSettingsLocked(polId, userHandle);
                        } catch (Exception e) {
                            VLog.e(VIVO_LOG_TAG, "setAirplaneModePolicy fail! " + e);
                            Binder.restoreCallingIdentity(callingId);
                            return false;
                        }
                    }
                    return true;
                }
                return false;
            }
        }
        throw new IllegalArgumentException("IllegalArgumentException: setAirplaneModePolicy input is illegal!");
    }

    private int getAirplaneModePolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            Bundle restrictions = this.mService.getUserRestrictions(admin, false);
            if (restrictions == null) {
                return 0;
            }
            boolean isRestrict = restrictions.getBoolean("no_airplane_mode", false);
            if (isRestrict) {
                return 1;
            }
            if (ap != null) {
                if (ap.airplanePolicy == 5 || ap.airplanePolicy == 6) {
                    return Settings.Global.getString(this.mContentResolver, "airplane_mode_on") == "1" ? 5 : 6;
                }
            }
            return ap.airplanePolicy;
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:17:0x002a, code lost:
        if (r7 == 0) goto L19;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private boolean setPhoneCall1Policy(android.content.ComponentName r5, int r6, int r7, int r8) {
        /*
            r4 = this;
            java.lang.String r0 = "EMM_TELE_CALL"
            r4.checkCallingEmmPermission(r5, r0, r8)
            java.lang.Object r0 = r4.getLockObject()
            monitor-enter(r0)
            com.android.server.devicepolicy.VivoCustomDpmsImpl$VivoActiveAdmin r1 = r4.getVivoActiveAdmin(r5, r8)     // Catch: java.lang.Throwable -> L3e
            if (r1 == 0) goto L3b
            int r2 = r1.phoneCall1Policy     // Catch: java.lang.Throwable -> L3e
            if (r2 == r7) goto L38
            r2 = 32
            r3 = -1
            if (r7 != r2) goto L1c
            r1.callInCountPolicy = r3     // Catch: java.lang.Throwable -> L3e
            goto L30
        L1c:
            r2 = 16
            if (r7 != r2) goto L23
            r1.callOutCountPolicy = r3     // Catch: java.lang.Throwable -> L3e
            goto L30
        L23:
            r2 = 48
            if (r7 == r2) goto L2c
            java.util.Objects.requireNonNull(r1)     // Catch: java.lang.Throwable -> L3e
            if (r7 != 0) goto L30
        L2c:
            r1.callInCountPolicy = r3     // Catch: java.lang.Throwable -> L3e
            r1.callOutCountPolicy = r3     // Catch: java.lang.Throwable -> L3e
        L30:
            r1.phoneCall1Policy = r7     // Catch: java.lang.Throwable -> L3e
            r4.updateMixedPhoneCallOutPolicy(r1)     // Catch: java.lang.Throwable -> L3e
            r4.saveVivoSettingsLocked(r6, r8)     // Catch: java.lang.Throwable -> L3e
        L38:
            r2 = 1
            monitor-exit(r0)     // Catch: java.lang.Throwable -> L3e
            return r2
        L3b:
            monitor-exit(r0)     // Catch: java.lang.Throwable -> L3e
            r0 = 0
            return r0
        L3e:
            r1 = move-exception
            monitor-exit(r0)     // Catch: java.lang.Throwable -> L3e
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.devicepolicy.VivoCustomDpmsImpl.setPhoneCall1Policy(android.content.ComponentName, int, int, int):boolean");
    }

    private int getPhoneCall1Policy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.phoneCall1Policy : 0;
        }
        return i;
    }

    /* JADX WARN: Code restructure failed: missing block: B:17:0x002a, code lost:
        if (r7 == 0) goto L19;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private boolean setPhoneCall2Policy(android.content.ComponentName r5, int r6, int r7, int r8) {
        /*
            r4 = this;
            java.lang.String r0 = "EMM_TELE_CALL"
            r4.checkCallingEmmPermission(r5, r0, r8)
            java.lang.Object r0 = r4.getLockObject()
            monitor-enter(r0)
            com.android.server.devicepolicy.VivoCustomDpmsImpl$VivoActiveAdmin r1 = r4.getVivoActiveAdmin(r5, r8)     // Catch: java.lang.Throwable -> L3e
            if (r1 == 0) goto L3b
            int r2 = r1.phoneCall2Policy     // Catch: java.lang.Throwable -> L3e
            if (r2 == r7) goto L38
            r2 = 32
            r3 = -1
            if (r7 != r2) goto L1c
            r1.callInCountPolicy = r3     // Catch: java.lang.Throwable -> L3e
            goto L30
        L1c:
            r2 = 16
            if (r7 != r2) goto L23
            r1.callOutCountPolicy = r3     // Catch: java.lang.Throwable -> L3e
            goto L30
        L23:
            r2 = 48
            if (r7 == r2) goto L2c
            java.util.Objects.requireNonNull(r1)     // Catch: java.lang.Throwable -> L3e
            if (r7 != 0) goto L30
        L2c:
            r1.callInCountPolicy = r3     // Catch: java.lang.Throwable -> L3e
            r1.callOutCountPolicy = r3     // Catch: java.lang.Throwable -> L3e
        L30:
            r1.phoneCall2Policy = r7     // Catch: java.lang.Throwable -> L3e
            r4.updateMixedPhoneCallOutPolicy(r1)     // Catch: java.lang.Throwable -> L3e
            r4.saveVivoSettingsLocked(r6, r8)     // Catch: java.lang.Throwable -> L3e
        L38:
            r2 = 1
            monitor-exit(r0)     // Catch: java.lang.Throwable -> L3e
            return r2
        L3b:
            monitor-exit(r0)     // Catch: java.lang.Throwable -> L3e
            r0 = 0
            return r0
        L3e:
            r1 = move-exception
            monitor-exit(r0)     // Catch: java.lang.Throwable -> L3e
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.devicepolicy.VivoCustomDpmsImpl.setPhoneCall2Policy(android.content.ComponentName, int, int, int):boolean");
    }

    private int getPhoneCall2Policy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.phoneCall2Policy : 0;
        }
        return i;
    }

    private void updateMixedPhoneCallOutPolicy(VivoActiveAdmin ap) {
        boolean OutBlock1;
        synchronized (getLockObject()) {
            if (ap != null) {
                boolean z = true;
                if (ap.phoneCall1Policy != 16 && ap.phoneCall1Policy != 48) {
                    OutBlock1 = false;
                    if (ap.phoneCall2Policy != 16 && ap.phoneCall2Policy != 48) {
                        z = false;
                    }
                    boolean OutBlock2 = z;
                    if (!OutBlock1 && !OutBlock2) {
                        ap.phoneCallPolicy = 17;
                    } else if (OutBlock1 && OutBlock2) {
                        ap.phoneCallPolicy = 18;
                    } else if (!OutBlock1 && OutBlock2) {
                        ap.phoneCallPolicy = 19;
                    } else {
                        ap.phoneCallPolicy = 0;
                    }
                }
                OutBlock1 = true;
                if (ap.phoneCall2Policy != 16) {
                    z = false;
                }
                boolean OutBlock22 = z;
                if (!OutBlock1) {
                }
                if (OutBlock1) {
                }
                if (!OutBlock1) {
                }
                ap.phoneCallPolicy = 0;
            }
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:17:0x002a, code lost:
        if (r7 == 0) goto L19;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private boolean setPhoneSms1Policy(android.content.ComponentName r5, int r6, int r7, int r8) {
        /*
            r4 = this;
            java.lang.String r0 = "EMM_TELE_MMS"
            r4.checkCallingEmmPermission(r5, r0, r8)
            java.lang.Object r0 = r4.getLockObject()
            monitor-enter(r0)
            com.android.server.devicepolicy.VivoCustomDpmsImpl$VivoActiveAdmin r1 = r4.getVivoActiveAdmin(r5, r8)     // Catch: java.lang.Throwable -> L3e
            if (r1 == 0) goto L3b
            int r2 = r1.phoneSms1Policy     // Catch: java.lang.Throwable -> L3e
            if (r2 == r7) goto L38
            r2 = 32
            r3 = -1
            if (r7 != r2) goto L1c
            r1.SMSInCountPolicy = r3     // Catch: java.lang.Throwable -> L3e
            goto L30
        L1c:
            r2 = 16
            if (r7 != r2) goto L23
            r1.SMSOutCountPolicy = r3     // Catch: java.lang.Throwable -> L3e
            goto L30
        L23:
            r2 = 48
            if (r7 == r2) goto L2c
            java.util.Objects.requireNonNull(r1)     // Catch: java.lang.Throwable -> L3e
            if (r7 != 0) goto L30
        L2c:
            r1.SMSOutCountPolicy = r3     // Catch: java.lang.Throwable -> L3e
            r1.SMSInCountPolicy = r3     // Catch: java.lang.Throwable -> L3e
        L30:
            r1.phoneSms1Policy = r7     // Catch: java.lang.Throwable -> L3e
            r4.updateMixedPhoneSmsOutPolicy(r1)     // Catch: java.lang.Throwable -> L3e
            r4.saveVivoSettingsLocked(r6, r8)     // Catch: java.lang.Throwable -> L3e
        L38:
            r2 = 1
            monitor-exit(r0)     // Catch: java.lang.Throwable -> L3e
            return r2
        L3b:
            monitor-exit(r0)     // Catch: java.lang.Throwable -> L3e
            r0 = 0
            return r0
        L3e:
            r1 = move-exception
            monitor-exit(r0)     // Catch: java.lang.Throwable -> L3e
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.devicepolicy.VivoCustomDpmsImpl.setPhoneSms1Policy(android.content.ComponentName, int, int, int):boolean");
    }

    private int getPhoneSms1Policy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.phoneSms1Policy : 0;
        }
        return i;
    }

    /* JADX WARN: Code restructure failed: missing block: B:17:0x002a, code lost:
        if (r7 == 0) goto L19;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private boolean setPhoneSms2Policy(android.content.ComponentName r5, int r6, int r7, int r8) {
        /*
            r4 = this;
            java.lang.String r0 = "EMM_TELE_MMS"
            r4.checkCallingEmmPermission(r5, r0, r8)
            java.lang.Object r0 = r4.getLockObject()
            monitor-enter(r0)
            com.android.server.devicepolicy.VivoCustomDpmsImpl$VivoActiveAdmin r1 = r4.getVivoActiveAdmin(r5, r8)     // Catch: java.lang.Throwable -> L3e
            if (r1 == 0) goto L3b
            int r2 = r1.phoneSms2Policy     // Catch: java.lang.Throwable -> L3e
            if (r2 == r7) goto L38
            r2 = 32
            r3 = -1
            if (r7 != r2) goto L1c
            r1.SMSInCountPolicy = r3     // Catch: java.lang.Throwable -> L3e
            goto L30
        L1c:
            r2 = 16
            if (r7 != r2) goto L23
            r1.SMSOutCountPolicy = r3     // Catch: java.lang.Throwable -> L3e
            goto L30
        L23:
            r2 = 48
            if (r7 == r2) goto L2c
            java.util.Objects.requireNonNull(r1)     // Catch: java.lang.Throwable -> L3e
            if (r7 != 0) goto L30
        L2c:
            r1.SMSOutCountPolicy = r3     // Catch: java.lang.Throwable -> L3e
            r1.SMSInCountPolicy = r3     // Catch: java.lang.Throwable -> L3e
        L30:
            r1.phoneSms2Policy = r7     // Catch: java.lang.Throwable -> L3e
            r4.updateMixedPhoneSmsOutPolicy(r1)     // Catch: java.lang.Throwable -> L3e
            r4.saveVivoSettingsLocked(r6, r8)     // Catch: java.lang.Throwable -> L3e
        L38:
            r2 = 1
            monitor-exit(r0)     // Catch: java.lang.Throwable -> L3e
            return r2
        L3b:
            monitor-exit(r0)     // Catch: java.lang.Throwable -> L3e
            r0 = 0
            return r0
        L3e:
            r1 = move-exception
            monitor-exit(r0)     // Catch: java.lang.Throwable -> L3e
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.devicepolicy.VivoCustomDpmsImpl.setPhoneSms2Policy(android.content.ComponentName, int, int, int):boolean");
    }

    private int getPhoneSms2Policy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.phoneSms2Policy : 0;
        }
        return i;
    }

    private void updateMixedPhoneSmsOutPolicy(VivoActiveAdmin ap) {
        boolean OutBlock1;
        synchronized (getLockObject()) {
            if (ap != null) {
                boolean z = true;
                if (ap.phoneSms1Policy != 16 && ap.phoneSms1Policy != 48) {
                    OutBlock1 = false;
                    if (ap.phoneSms2Policy != 16 && ap.phoneSms2Policy != 48) {
                        z = false;
                    }
                    boolean OutBlock2 = z;
                    if (!OutBlock1 && !OutBlock2) {
                        ap.phoneSmsPolicy = 17;
                    } else if (OutBlock1 && OutBlock2) {
                        ap.phoneSmsPolicy = 18;
                    } else if (!OutBlock1 && OutBlock2) {
                        ap.phoneSmsPolicy = 19;
                    } else {
                        ap.phoneSmsPolicy = 0;
                    }
                }
                OutBlock1 = true;
                if (ap.phoneSms2Policy != 16) {
                    z = false;
                }
                boolean OutBlock22 = z;
                if (!OutBlock1) {
                }
                if (OutBlock1) {
                }
                if (!OutBlock1) {
                }
                ap.phoneSmsPolicy = 0;
            }
        }
    }

    private boolean setPhoneMms1Policy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_TELE_MMS", userHandle);
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.phoneMms1Policy != policy) {
                    ap.phoneMms1Policy = policy;
                    updateMixedPhoneMmsOutPolicy(ap);
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getPhoneMms1Policy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.phoneMms1Policy : 0;
        }
        return i;
    }

    private boolean setPhoneMms2Policy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_TELE_MMS", userHandle);
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.phoneMms2Policy != policy) {
                    ap.phoneMms2Policy = policy;
                    updateMixedPhoneMmsOutPolicy(ap);
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getPhoneMms2Policy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.phoneMms2Policy : 0;
        }
        return i;
    }

    private void updateMixedPhoneMmsOutPolicy(VivoActiveAdmin ap) {
        boolean OutBlock1;
        synchronized (getLockObject()) {
            if (ap != null) {
                boolean z = true;
                if (ap.phoneMms1Policy != 16 && ap.phoneMms1Policy != 48) {
                    OutBlock1 = false;
                    if (ap.phoneMms2Policy != 16 && ap.phoneMms2Policy != 48) {
                        z = false;
                    }
                    boolean OutBlock2 = z;
                    if (!OutBlock1 && !OutBlock2) {
                        ap.phoneMmsPolicy = 17;
                    } else if (OutBlock1 && OutBlock2) {
                        ap.phoneMmsPolicy = 18;
                    } else if (!OutBlock1 && OutBlock2) {
                        ap.phoneMmsPolicy = 19;
                    } else {
                        ap.phoneMmsPolicy = 0;
                    }
                }
                OutBlock1 = true;
                if (ap.phoneMms2Policy != 16) {
                    z = false;
                }
                boolean OutBlock22 = z;
                if (!OutBlock1) {
                }
                if (OutBlock1) {
                }
                if (!OutBlock1) {
                }
                ap.phoneMmsPolicy = 0;
            }
        }
    }

    private boolean setPhoneMaskPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_TELECOM", userHandle);
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.phoneMaskPolicy != policy) {
                    ap.phoneMaskPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getPhoneMaskPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.phoneMaskPolicy : 0;
        }
        return i;
    }

    private boolean setPhoneMaskPermissionBWList(ComponentName admin, int listId, List<String> keys, boolean isAdd, int userHandle) {
        int i;
        int i2;
        VivoActiveAdmin ap;
        boolean z;
        CompletableFuture<Boolean> result;
        VivoActiveAdmin ap2;
        int i3;
        boolean z2;
        List<String> list;
        int i4;
        String info;
        int i5 = listId;
        List<String> list2 = keys;
        boolean z3 = isAdd;
        int i6 = userHandle;
        checkCallingEmmPermission(admin, "EMM_TELECOM", i6);
        if (list2 == null || keys.isEmpty()) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        boolean success = false;
        synchronized (getLockObject()) {
            try {
            } catch (Throwable th) {
                th = th;
            }
            try {
                VivoActiveAdmin ap3 = getVivoActiveAdmin(admin, i6);
                if (ap3 != null) {
                    if (i5 == 1305) {
                        i = i6;
                        i2 = i5;
                        if (ap3.phoneMaskPermissionBlist == null) {
                            ap3.phoneMaskPermissionBlist = new ArrayList();
                        }
                        success = VivoUtils.addItemsFromPhoneNumList(ap3.phoneMaskPermissionBlist, list2, z3);
                    } else if (i5 != 1306) {
                        i = i6;
                        i2 = i5;
                    } else {
                        try {
                            List<String> successList = new ArrayList<>();
                            i2 = 4;
                            try {
                                if (ap3.telecomMaskPermissionBwlist == 4) {
                                    final CompletableFuture<Boolean> result2 = new CompletableFuture<>();
                                    RemoteCallback mRemoteCallback = new RemoteCallback(new RemoteCallback.OnResultListener() { // from class: com.android.server.devicepolicy.-$$Lambda$VivoCustomDpmsImpl$AI2i1r-OvAb06JK8okSt0J1wYHM
                                        public final void onResult(Bundle bundle) {
                                            result2.complete(Boolean.valueOf(b != null));
                                        }
                                    });
                                    for (String info2 : keys) {
                                        VLog.i(VIVO_LOG_TAG, "setPhoneMaskPermissionBWList info = " + info2);
                                        List<String> appPermissionWhiteList = getAppPermissionWhiteListUncheckedLocked(ap3);
                                        if (this.MASK_EXEMPT_APP.contains(info2)) {
                                            result = result2;
                                            ap2 = ap3;
                                            i3 = i6;
                                            z2 = z3;
                                            list = list2;
                                            i4 = i5;
                                        } else {
                                            if (appPermissionWhiteList != null) {
                                                try {
                                                    if (appPermissionWhiteList.contains(info2)) {
                                                        result = result2;
                                                        ap2 = ap3;
                                                        i3 = i6;
                                                        z2 = z3;
                                                        list = list2;
                                                        i4 = i5;
                                                    }
                                                } catch (Exception e) {
                                                    e = e;
                                                    i = i6;
                                                    i2 = i5;
                                                    VLog.w(VIVO_LOG_TAG, "failed setPermissionGrantState ", e);
                                                    saveVivoSettingsLocked(i2, i);
                                                    return success;
                                                }
                                            }
                                            if (!z3) {
                                                info = info2;
                                                result = result2;
                                                ap2 = ap3;
                                                i3 = i6;
                                                z2 = z3;
                                                list = list2;
                                                i4 = i5;
                                                setPermissionGrantState(admin, this.mContext.getPackageName(), info, "android.permission.READ_CALL_LOG", 2, mRemoteCallback);
                                                setPermissionGrantState(admin, this.mContext.getPackageName(), info, "android.permission.READ_SMS", 2, mRemoteCallback);
                                                setPermissionGrantState(admin, this.mContext.getPackageName(), info, "android.permission.RECEIVE_SMS", 2, mRemoteCallback);
                                                setPermissionGrantState(admin, this.mContext.getPackageName(), info, "android.permission.READ_CONTACTS", 2, mRemoteCallback);
                                            } else {
                                                info = info2;
                                                setPermissionGrantState(admin, this.mContext.getPackageName(), info2, "android.permission.READ_CALL_LOG", 1, mRemoteCallback);
                                                ap2 = ap3;
                                                i3 = i6;
                                                z2 = z3;
                                                result = result2;
                                                list = list2;
                                                i4 = i5;
                                                setPermissionGrantState(admin, this.mContext.getPackageName(), info, "android.permission.READ_SMS", 1, mRemoteCallback);
                                                setPermissionGrantState(admin, this.mContext.getPackageName(), info, "android.permission.RECEIVE_SMS", 1, mRemoteCallback);
                                                setPermissionGrantState(admin, this.mContext.getPackageName(), info, "android.permission.READ_CONTACTS", 1, mRemoteCallback);
                                            }
                                            successList.add(info);
                                        }
                                        z3 = z2;
                                        i5 = i4;
                                        list2 = list;
                                        ap3 = ap2;
                                        i6 = i3;
                                        result2 = result;
                                    }
                                    ap = ap3;
                                    i = i6;
                                    z = z3;
                                    i2 = i5;
                                } else {
                                    ap = ap3;
                                    i = i6;
                                    z = z3;
                                    i2 = i5;
                                    successList.addAll(list2);
                                }
                                if (ap.phoneMaskPermissionWlist == null) {
                                    ap.phoneMaskPermissionWlist = new ArrayList();
                                }
                                success = VivoUtils.addItemsFromPhoneNumList(ap.phoneMaskPermissionWlist, successList, z);
                            } catch (Exception e2) {
                                e = e2;
                            }
                        } catch (Exception e3) {
                            e = e3;
                            i = i6;
                            i2 = i5;
                        }
                    }
                    saveVivoSettingsLocked(i2, i);
                }
                return success;
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    private List<String> getAppPermissionWhiteListUncheckedLocked(VivoActiveAdmin ap) {
        if (ap != null) {
            if (this.mPersistentListPkgs != null) {
                List<String> all = new ArrayList<>(this.mPersistentListPkgs);
                if (ap.appPermissionWlist != null && ap.appPermissionWlist.size() > 0) {
                    all.addAll(ap.appPermissionWlist);
                }
                return all;
            }
            return ap.appPermissionWlist;
        }
        return this.mPersistentListPkgs;
    }

    private List<String> getPhoneMaskPermissionBWList(ComponentName admin, int listId, int userHandle) {
        VivoActiveAdmin ap;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            if (ap != null) {
                if (listId == 1305) {
                    return ap.phoneMaskPermissionBlist;
                } else if (listId == 1306) {
                    return ap.phoneMaskPermissionWlist;
                }
            }
            return null;
        }
    }

    private boolean setCallRecordPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_TELE_CALL", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.operationCallRecordPolicy != policy) {
                    ap.operationCallRecordPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    public int getCallRecordPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.operationCallRecordPolicy : 0;
        }
        return i;
    }

    public void registerCallStateCallback(IVivoCallStateCallback callback) {
        int callingUid = Binder.getCallingUid();
        if (!isVivoEmmUid(callingUid)) {
            VLog.w(VIVO_LOG_TAG, "registerCallStateCallback failed!");
            return;
        }
        synchronized (getLockObject()) {
            if (callback != null) {
                VLog.v(VIVO_LOG_TAG, "registerCallStateCallback");
                this.mCallStateCallbacks.register(callback);
            }
        }
    }

    public void unregisterCallStateCallback(IVivoCallStateCallback callback) {
        synchronized (getLockObject()) {
            if (callback != null) {
                VLog.v(VIVO_LOG_TAG, "unregisterCallStateCallback");
                this.mCallStateCallbacks.unregister(callback);
            }
        }
    }

    private void notifyCallStateCallback(Bundle state) {
        int i = this.mCallStateCallbacks.beginBroadcast();
        while (i > 0) {
            i--;
            try {
                this.mCallStateCallbacks.getBroadcastItem(i).onVivoCallStateChanged(state);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        this.mCallStateCallbacks.finishBroadcast();
    }

    public void reportInfo(int infoId, Bundle data, int userHandle) {
        VLog.w(VIVO_LOG_TAG, "reportInfo id = " + infoId);
        checkGetInfoPermission();
        if (data == null) {
            VLog.w(VIVO_LOG_TAG, "reportInfo data is null!");
            return;
        }
        try {
            if (infoId == 5000) {
                notifyCallStateCallback(data);
                handleTelecomCallNum(data, userHandle);
            } else if (infoId == 5001) {
                handleTelecomSMSNum(data, userHandle);
            }
        } catch (Exception e) {
            VLog.w(VIVO_LOG_TAG, "reportInfo exception!");
            e.printStackTrace();
        }
    }

    private void handleTelecomCallNum(Bundle data, int userHandle) {
        ComponentName vivoAdmin = getVivoAdminUncheckedLocked(userHandle);
        VivoActiveAdmin ap = getVivoActiveAdminUncheckedLocked(vivoAdmin, userHandle);
        if (ap != null) {
            int callState = data.getInt("callState");
            if (callState != 1) {
                this.mLastCallState = callState;
                return;
            }
            String number = data.getString("callHandle");
            VLog.v(VIVO_LOG_TAG, "handleTelecomCallNum, mLastCallState = " + this.mLastCallState + "  ap.callInCountPolicy = " + ap.callInCountPolicy + "  ap.callOutCountPolicy" + ap.callOutCountPolicy);
            if (!isEmergencyNumberForDisplay(number)) {
                int i = this.mLastCallState;
                if (5 == i) {
                    if (ap.callInCountPolicy != -1) {
                        if (ap.callInCountPolicy != 1) {
                            if (ap.callInCountPolicy > 1) {
                                ap.callInCountPolicy--;
                                saveVivoSettingsLocked(3402, userHandle);
                                return;
                            }
                            return;
                        }
                        ap.callInCountPolicy = 0;
                        if (ap.phoneCall1Policy == 16) {
                            ap.phoneCall1Policy = 48;
                        } else {
                            ap.phoneCall1Policy = 32;
                        }
                        sendChangedNotification(408, userHandle);
                        if (ap.phoneCall2Policy == 16) {
                            ap.phoneCall2Policy = 48;
                        } else {
                            ap.phoneCall2Policy = 32;
                        }
                        updateMixedPhoneCallOutPolicy(ap);
                        saveVivoSettingsLocked(409, userHandle);
                    }
                } else if (3 == i && ap.callOutCountPolicy != -1) {
                    if (ap.callOutCountPolicy != 1) {
                        if (ap.callOutCountPolicy > 1) {
                            ap.callOutCountPolicy--;
                            saveVivoSettingsLocked(3402, userHandle);
                            return;
                        }
                        return;
                    }
                    ap.callOutCountPolicy = 0;
                    if (ap.phoneCall1Policy == 32) {
                        ap.phoneCall1Policy = 48;
                    } else {
                        ap.phoneCall1Policy = 16;
                    }
                    sendChangedNotification(408, userHandle);
                    if (ap.phoneCall2Policy == 32) {
                        ap.phoneCall2Policy = 48;
                    } else {
                        ap.phoneCall2Policy = 16;
                    }
                    updateMixedPhoneCallOutPolicy(ap);
                    saveVivoSettingsLocked(409, userHandle);
                }
            }
        }
    }

    private boolean isEmergencyNumberForDisplay(String number) {
        String[] strArr;
        if (TextUtils.isEmpty(number)) {
            return false;
        }
        for (String emergency : sEmergencyNumsCTCC) {
            if (emergency.equals(number)) {
                return true;
            }
        }
        return false;
    }

    private void handleTelecomSMSNum(Bundle data, int userHandle) {
        ComponentName vivoAdmin = getVivoAdminUncheckedLocked(userHandle);
        VivoActiveAdmin ap = getVivoActiveAdminUncheckedLocked(vivoAdmin, userHandle);
        if (ap != null) {
            int inOrOut = data.getInt("state_int");
            VLog.v(VIVO_LOG_TAG, "handleTelecomSMSNum, inOrOut = " + inOrOut + "  ap.SMSInCountPolicy = " + ap.SMSInCountPolicy);
            if (32 == inOrOut) {
                if (ap.SMSInCountPolicy == -1) {
                    return;
                }
                if (ap.SMSInCountPolicy == 1) {
                    ap.SMSInCountPolicy = 0;
                    if (ap.phoneSms1Policy == 16) {
                        ap.phoneSms1Policy = 48;
                    } else {
                        ap.phoneSms1Policy = 32;
                    }
                    sendChangedNotification(410, userHandle);
                    if (ap.phoneSms2Policy == 16) {
                        ap.phoneSms2Policy = 48;
                    } else {
                        ap.phoneSms2Policy = 32;
                    }
                    updateMixedPhoneSmsOutPolicy(ap);
                    saveVivoSettingsLocked(411, userHandle);
                } else if (ap.SMSInCountPolicy > 1) {
                    ap.SMSInCountPolicy--;
                    saveVivoSettingsLocked(3403, userHandle);
                }
            } else if (16 != inOrOut || ap.SMSOutCountPolicy == -1) {
            } else {
                if (ap.SMSOutCountPolicy == 1) {
                    ap.SMSOutCountPolicy = 0;
                    if (ap.phoneSms1Policy == 32) {
                        ap.phoneSms1Policy = 48;
                    } else {
                        ap.phoneSms1Policy = 16;
                    }
                    sendChangedNotification(410, userHandle);
                    if (ap.phoneSms2Policy == 32) {
                        ap.phoneSms2Policy = 48;
                    } else {
                        ap.phoneSms2Policy = 16;
                    }
                    updateMixedPhoneSmsOutPolicy(ap);
                    saveVivoSettingsLocked(411, userHandle);
                } else if (ap.SMSOutCountPolicy > 1) {
                    ap.SMSOutCountPolicy--;
                    saveVivoSettingsLocked(3403, userHandle);
                }
            }
        }
    }

    private boolean setPhoneMutliCallPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_TELE_CALL", userHandle);
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.phoneMutliCallPolicy != policy) {
                    ap.phoneMutliCallPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getPhoneMutliCallPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.phoneMutliCallPolicy : 0;
        }
        return i;
    }

    private boolean setPhoneCallForwardingPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_TELE_CALL", userHandle);
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.callForwardingPolicy != policy) {
                    ap.callForwardingPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getPhoneCallForwardingPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.callForwardingPolicy : 0;
        }
        return i;
    }

    private boolean setPhoneNetworkSMSPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_TELE_MMS", userHandle);
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.phoneNetworkSMSPolicy != policy) {
                    ap.phoneNetworkSMSPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getPhoneNetworkSMSPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.phoneNetworkSMSPolicy : 0;
        }
        return i;
    }

    private boolean setTelecomCallNum(ComponentName admin, int polId, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_TELE_CALL", userHandle);
        if (data == null) {
            throw new IllegalArgumentException("IllegalArgumentException: setTelecomCallNum para is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap == null) {
                return false;
            }
            int inOrOut = data.getInt("state_int");
            int count = data.getInt("state_int2");
            if (count != 0 && count >= -1) {
                if (32 == inOrOut) {
                    if (ap.callInCountPolicy != count) {
                        if (ap.phoneCall1Policy == 32) {
                            Objects.requireNonNull(ap);
                            ap.phoneCall1Policy = 0;
                            sendChangedNotification(408, userHandle);
                        } else if (ap.phoneCall1Policy == 48) {
                            ap.phoneCall1Policy = 16;
                            sendChangedNotification(408, userHandle);
                        }
                        if (ap.phoneCall2Policy == 32) {
                            Objects.requireNonNull(ap);
                            ap.phoneCall2Policy = 0;
                            sendChangedNotification(409, userHandle);
                        } else if (ap.phoneCall2Policy == 48) {
                            ap.phoneCall2Policy = 16;
                            sendChangedNotification(409, userHandle);
                        }
                        ap.callInCountPolicy = count;
                        updateMixedPhoneCallOutPolicy(ap);
                        saveVivoSettingsLocked(polId, userHandle);
                    }
                } else if (16 == inOrOut) {
                    if (ap.callOutCountPolicy != count) {
                        if (ap.phoneCall1Policy == 16) {
                            Objects.requireNonNull(ap);
                            ap.phoneCall1Policy = 0;
                            sendChangedNotification(408, userHandle);
                        } else if (ap.phoneCall1Policy == 48) {
                            ap.phoneCall1Policy = 32;
                            sendChangedNotification(408, userHandle);
                        }
                        if (ap.phoneCall2Policy == 16) {
                            Objects.requireNonNull(ap);
                            ap.phoneCall2Policy = 0;
                            sendChangedNotification(409, userHandle);
                        } else if (ap.phoneCall2Policy == 48) {
                            ap.phoneCall2Policy = 32;
                            sendChangedNotification(409, userHandle);
                        }
                        ap.callOutCountPolicy = count;
                        updateMixedPhoneCallOutPolicy(ap);
                        saveVivoSettingsLocked(polId, userHandle);
                    }
                } else {
                    throw new IllegalArgumentException("IllegalArgumentException: setTelecomCallNum para is illegal!");
                }
                return true;
            }
            return false;
        }
    }

    private Bundle getTelecomCallNum(ComponentName admin, Bundle data, int userHandle) {
        VivoActiveAdmin ap;
        if (data == null) {
            throw new IllegalArgumentException("IllegalArgumentException: getTelecomCallNum para is illegal!");
        }
        synchronized (getLockObject()) {
            Bundle bundle = new Bundle();
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            if (ap != null) {
                int inOrOut = data.getInt("state_int");
                if (32 == inOrOut) {
                    bundle.putInt("state_int", ap.callInCountPolicy);
                } else if (16 == inOrOut) {
                    bundle.putInt("state_int", ap.callOutCountPolicy);
                }
                VLog.v(VIVO_LOG_TAG, "getTelecomCallNum callInCountPolicy:" + ap.callInCountPolicy + ", callOutCountPolicy:" + ap.callOutCountPolicy);
                return bundle;
            }
            return null;
        }
    }

    private boolean setTelecomSMSNum(ComponentName admin, int polId, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_TELE_MMS", userHandle);
        if (data == null) {
            throw new IllegalArgumentException("IllegalArgumentException: setTelecomSMSNum para is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap == null) {
                return false;
            }
            int inOrOut = data.getInt("state_int");
            int count = data.getInt("state_int2");
            if (count != 0 && count >= -1) {
                if (32 == inOrOut) {
                    if (ap.SMSInCountPolicy != count) {
                        if (ap.phoneSms1Policy == 32) {
                            Objects.requireNonNull(ap);
                            ap.phoneSms1Policy = 0;
                            sendChangedNotification(410, userHandle);
                        } else if (ap.phoneSms1Policy == 48) {
                            ap.phoneSms1Policy = 16;
                            sendChangedNotification(410, userHandle);
                        }
                        if (ap.phoneSms2Policy == 32) {
                            Objects.requireNonNull(ap);
                            ap.phoneSms2Policy = 0;
                            sendChangedNotification(411, userHandle);
                        } else if (ap.phoneSms2Policy == 48) {
                            ap.phoneSms2Policy = 16;
                            sendChangedNotification(411, userHandle);
                        }
                        ap.SMSInCountPolicy = count;
                        updateMixedPhoneSmsOutPolicy(ap);
                        saveVivoSettingsLocked(polId, userHandle);
                    }
                } else if (16 == inOrOut) {
                    if (ap.SMSOutCountPolicy != count) {
                        if (ap.phoneSms1Policy == 16) {
                            Objects.requireNonNull(ap);
                            ap.phoneSms1Policy = 0;
                            sendChangedNotification(410, userHandle);
                        } else if (ap.phoneSms1Policy == 48) {
                            ap.phoneSms1Policy = 32;
                            sendChangedNotification(410, userHandle);
                        }
                        if (ap.phoneSms2Policy == 16) {
                            Objects.requireNonNull(ap);
                            ap.phoneSms2Policy = 0;
                            sendChangedNotification(411, userHandle);
                        } else if (ap.phoneSms2Policy == 48) {
                            ap.phoneSms2Policy = 32;
                            sendChangedNotification(411, userHandle);
                        }
                        ap.SMSOutCountPolicy = count;
                        updateMixedPhoneSmsOutPolicy(ap);
                        saveVivoSettingsLocked(polId, userHandle);
                    }
                } else {
                    throw new IllegalArgumentException("IllegalArgumentException: setTelecomSMSNum para is illegal!");
                }
                return true;
            }
            return false;
        }
    }

    private Bundle getTelecomSMSNum(ComponentName admin, Bundle data, int userHandle) {
        VivoActiveAdmin ap;
        if (data == null) {
            throw new IllegalArgumentException("IllegalArgumentException: getTelecomSMSNum para is illegal!");
        }
        synchronized (getLockObject()) {
            Bundle bundle = new Bundle();
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            if (ap != null) {
                int inOrOut = data.getInt("state_int");
                if (32 == inOrOut) {
                    bundle.putInt("state_int", ap.SMSInCountPolicy);
                } else if (16 == inOrOut) {
                    bundle.putInt("state_int", ap.SMSOutCountPolicy);
                }
                VLog.v(VIVO_LOG_TAG, "getTelecomCallNum SMSInCountPolicy:" + ap.SMSInCountPolicy + ", SMSOutCountPolicy:" + ap.SMSOutCountPolicy);
                return bundle;
            }
            return null;
        }
    }

    private boolean setTelecomPINLock(ComponentName admin, int polId, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_TELECOM", userHandle);
        if (data != null) {
            String password = data.getString("state_string");
            String password2 = data.getString("state_string2");
            int policy = data.getInt("state_int");
            if (policy == 0 || policy == 1) {
                synchronized (getLockObject()) {
                    try {
                    } catch (Throwable th) {
                        th = th;
                    }
                    try {
                        VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
                        if (ap != null) {
                            long callingId = Binder.clearCallingIdentity();
                            try {
                                if (policy == 1) {
                                    try {
                                        if (this.mITelephony == null) {
                                            this.mITelephony = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
                                        }
                                        int[] subIds = SubscriptionManager.getSubId(0);
                                        if (subIds != null && subIds.length > 0 && this.mITelephony.isIccLockEnabled(subIds[0]) && (password == null || this.mITelephony.setIccLockEnabled(subIds[0], false, password) != Integer.MAX_VALUE)) {
                                            VLog.e(VIVO_LOG_TAG, "setTelecomPINLock fail! psd invalid");
                                            Binder.restoreCallingIdentity(callingId);
                                            return false;
                                        }
                                        int[] subIds2 = SubscriptionManager.getSubId(1);
                                        if (subIds2 != null && subIds2.length > 0 && this.mITelephony.isIccLockEnabled(subIds2[0]) && (password2 == null || this.mITelephony.setIccLockEnabled(subIds2[0], false, password2) != Integer.MAX_VALUE)) {
                                            VLog.e(VIVO_LOG_TAG, "setTelecomPINLock fail! psd2 invalid");
                                            Binder.restoreCallingIdentity(callingId);
                                            return false;
                                        }
                                    } catch (Exception e) {
                                        e = e;
                                        VLog.e(VIVO_LOG_TAG, "setTelecomPINLock fail! " + e);
                                        Binder.restoreCallingIdentity(callingId);
                                        return false;
                                    } catch (Throwable th2) {
                                        th = th2;
                                        Binder.restoreCallingIdentity(callingId);
                                        throw th;
                                    }
                                }
                                if (ap.telecomPinLockPolicy != policy) {
                                    ap.telecomPinLockPolicy = policy;
                                    try {
                                        saveVivoSettingsLocked(polId, userHandle);
                                    } catch (Exception e2) {
                                        e = e2;
                                        VLog.e(VIVO_LOG_TAG, "setTelecomPINLock fail! " + e);
                                        Binder.restoreCallingIdentity(callingId);
                                        return false;
                                    }
                                }
                                Binder.restoreCallingIdentity(callingId);
                                return true;
                            } catch (Throwable th3) {
                                th = th3;
                            }
                        }
                        return false;
                    } catch (Throwable th4) {
                        th = th4;
                        throw th;
                    }
                }
            }
            throw new IllegalArgumentException("IllegalArgumentException: setTelecomPINLock input is illegal!");
        }
        throw new IllegalArgumentException("IllegalArgumentException: setTelecomPINLock para is illegal!");
    }

    private int getTelecomPINLockPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.telecomPinLockPolicy : 0;
        }
        return i;
    }

    private boolean setAppInstallBWPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_APP_PACKAGE", userHandle);
        if (policy != 0 && policy != 3 && policy != 4) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.appInstallBWPolicy != policy) {
                    ap.appInstallBWPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getAppInstallBWPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            if (this.mInstallWlistPkgs != null) {
                return 4;
            }
            return ap != null ? ap.appInstallBWPolicy : 0;
        }
    }

    private boolean setAppUnInstallBWPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_APP_PACKAGE", userHandle);
        if (policy != 0 && policy != 3 && policy != 4) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.appUnInstallBWPolicy != policy) {
                    ap.appUnInstallBWPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getAppUnInstallBWPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            if (this.mUninstallBlistPkgs != null) {
                return 3;
            }
            return ap != null ? ap.appUnInstallBWPolicy : 0;
        }
    }

    private boolean setAppTrustedSourcePolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_APP_PACKAGE", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.appTrustedSourcePolicy != policy) {
                    ap.appTrustedSourcePolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getAppTrustedSourcePolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.appTrustedSourcePolicy : 0;
        }
        return i;
    }

    private boolean setAppDisallowedLaunchPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_APP_RUNNING", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.appDisallowedLaunchPolicy != policy) {
                    ap.appDisallowedLaunchPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getAppDisallowedLaunchPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.appDisallowedLaunchPolicy : 0;
        }
        return i;
    }

    private boolean setAppMeteredDataBWPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_APP_PERMISSION", userHandle);
        if (policy != 0 && policy != 3 && policy != 4) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.appMeteredDataBWPolicy != policy) {
                    ap.appMeteredDataBWPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getAppMeteredDataBWPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.appMeteredDataBWPolicy : 0;
        }
        return i;
    }

    private boolean setAppWlanDataBWPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_APP_PERMISSION", userHandle);
        if (policy != 0 && policy != 3 && policy != 4) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.appWlanDataBWPolicy != policy) {
                    ap.appWlanDataBWPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getAppWlanDataBWPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.appWlanDataBWPolicy : 0;
        }
        return i;
    }

    private boolean setAppPermissionPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_APP_PERMISSION", userHandle);
        if (policy != 0 && policy != 1 && policy != 2) {
            throw new IllegalArgumentException("IllegalArgumentException: setAppPermissionPolicy input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                try {
                    this.mService.setPermissionPolicy(admin, admin.getPackageName(), policy);
                    return true;
                } catch (Exception e) {
                    VLog.e(VIVO_LOG_TAG, "setAppPermissionPolicy failed", e);
                    e.printStackTrace();
                }
            }
            return false;
        }
    }

    private int getAppPermissionPolicy(ComponentName admin, int userHandle) {
        int permissionPolicy;
        synchronized (getLockObject()) {
            try {
                try {
                    permissionPolicy = this.mService.getPermissionPolicy(admin);
                } catch (RemoteException e) {
                    throw new IllegalStateException("getAppPermissionPolicy failed.", e);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return permissionPolicy;
    }

    private boolean setAppInstallBWList(ComponentName admin, int listId, List<String> keys, boolean isAdd, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_APP_PACKAGE", userHandle);
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        boolean success = false;
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (listId == 1101) {
                    if (ap.appInstallBlist == null) {
                        ap.appInstallBlist = new ArrayList();
                    }
                    success = VivoUtils.addItemsFromList(ap.appInstallBlist, keys, isAdd);
                } else if (listId == 1102) {
                    if (ap.appInstallWlist == null) {
                        ap.appInstallWlist = new ArrayList();
                    }
                    success = VivoUtils.addItemsFromList(ap.appInstallWlist, keys, isAdd);
                }
                saveVivoSettingsLocked(listId, userHandle);
            }
        }
        return success;
    }

    private List<String> getAppInstallBWList(ComponentName admin, int listId, int userHandle) {
        VivoActiveAdmin ap;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            if (ap != null) {
                if (listId != 1101) {
                    if (listId == 1102) {
                        if (this.mInstallWlistPkgs != null) {
                            List<String> all = new ArrayList<>(this.mInstallWlistPkgs);
                            if (ap.appInstallWlist != null && ap.appInstallWlist.size() > 0) {
                                all.addAll(ap.appInstallWlist);
                            }
                            return all;
                        }
                        return ap.appInstallWlist;
                    }
                } else {
                    return ap.appInstallBlist;
                }
            } else if (this.mInstallWlistPkgs != null && listId == 1102) {
                return this.mInstallWlistPkgs;
            }
            return null;
        }
    }

    private boolean setAppUnInstallBWList(ComponentName admin, int listId, List<String> keys, boolean isAdd, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_APP_PACKAGE", userHandle);
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        boolean success = false;
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (listId == 1103) {
                    if (ap.appUnInstallBlist == null) {
                        ap.appUnInstallBlist = new ArrayList();
                    }
                    success = VivoUtils.addItemsFromList(ap.appUnInstallBlist, keys, isAdd);
                } else if (listId == 1104) {
                    if (ap.appUnInstallWlist == null) {
                        ap.appUnInstallWlist = new ArrayList();
                    }
                    success = VivoUtils.addItemsFromList(ap.appUnInstallWlist, keys, isAdd);
                }
                saveVivoSettingsLocked(listId, userHandle);
            }
        }
        return success;
    }

    private List<String> getAppUnInstallBWList(ComponentName admin, int listId, int userHandle) {
        VivoActiveAdmin ap;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            if (ap != null) {
                if (listId == 1103) {
                    if (this.mUninstallBlistPkgs != null) {
                        List<String> all = new ArrayList<>(this.mUninstallBlistPkgs);
                        if (ap.appUnInstallBlist != null && ap.appUnInstallBlist.size() > 0) {
                            all.addAll(ap.appUnInstallBlist);
                        }
                        return all;
                    }
                    return ap.appUnInstallBlist;
                } else if (listId == 1104) {
                    return ap.appUnInstallWlist;
                }
            } else if (this.mUninstallBlistPkgs != null && listId == 1103) {
                return this.mUninstallBlistPkgs;
            }
            return null;
        }
    }

    private boolean setAppTrustedSourceList(ComponentName admin, int listId, List<String> keys, boolean isAdd, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_APP_PACKAGE", userHandle);
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        boolean success = false;
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.appTrustedSourcelist == null) {
                    ap.appTrustedSourcelist = new ArrayList();
                }
                success = VivoUtils.addItemsFromList(ap.appTrustedSourcelist, keys, isAdd);
                saveVivoSettingsLocked(listId, userHandle);
            }
        }
        return success;
    }

    private List<String> getAppTrustedSourceList(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        List<String> list;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            list = ap != null ? ap.appTrustedSourcelist : null;
        }
        return list;
    }

    /* JADX WARN: Removed duplicated region for block: B:61:0x0148 A[Catch: all -> 0x0162, TRY_LEAVE, TryCatch #2 {all -> 0x0162, blocks: (B:8:0x001c, B:10:0x0024, B:12:0x0028, B:14:0x0031, B:15:0x003a, B:17:0x0040, B:19:0x004c, B:20:0x0068, B:21:0x006d, B:22:0x0073, B:49:0x011c, B:59:0x0142, B:61:0x0148, B:58:0x013e), top: B:82:0x001c }] */
    /* JADX WARN: Removed duplicated region for block: B:66:0x0155  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private boolean setAppDisallowedLaunchList(android.content.ComponentName r24, int r25, java.util.List<java.lang.String> r26, boolean r27, int r28) {
        /*
            Method dump skipped, instructions count: 371
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.devicepolicy.VivoCustomDpmsImpl.setAppDisallowedLaunchList(android.content.ComponentName, int, java.util.List, boolean, int):boolean");
    }

    private List<String> getAppDisallowedLaunchList(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        List<String> list;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            list = ap != null ? ap.appDisallowedLaunchlist : null;
        }
        return list;
    }

    private boolean isAllowDisabled(PackageInfo pkgInfo) {
        return (pkgInfo.applicationInfo.flags & 1) == 0 || ALLOW_DISABLED_SYSTEM_APP.contains(pkgInfo.packageName);
    }

    private boolean isAllowDisableComponent(String pkg, int userHandle) {
        try {
            PackageInfo pkgInfo = this.mIPackageManager.getPackageInfo(pkg, 0, userHandle);
            if (pkgInfo != null) {
                if ((pkgInfo.applicationInfo.flags & 1) != 0) {
                    if (!ALLOW_DISABLED_COMPONENT_SYSTEM_APP.contains(pkg)) {
                    }
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            VLog.e(VIVO_LOG_TAG, "isAllowDisableComponent packageNames = " + pkg + " check failed! " + e);
            return false;
        }
    }

    private boolean isAllowDisableLaunch(String pkg, int userHandle) {
        try {
            PackageInfo pkgInfo = this.mIPackageManager.getPackageInfo(pkg, 0, userHandle);
            if (pkgInfo != null) {
                if ((pkgInfo.applicationInfo.flags & 1) != 0) {
                    if (!ALLOW_DISABLED_LAUNCH_SYSTEM_APP.contains(pkg)) {
                    }
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            VLog.e(VIVO_LOG_TAG, "isAllowDisableLaunch packageNames = " + pkg + " check failed! " + e);
            return false;
        }
    }

    /* JADX WARN: Can't wrap try/catch for region: R(11:17|18|(2:19|20)|(13:(3:23|24|(2:26|27)(1:38))(1:62)|39|(1:41)(1:58)|42|43|44|(1:47)|48|(3:50|(1:52)(1:54)|53)|55|56|57|34)(2:63|64)|28|29|31|32|33|34|15) */
    /* JADX WARN: Code restructure failed: missing block: B:49:0x00f2, code lost:
        r0 = e;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private boolean setAppDisabledAppList(android.content.ComponentName r22, int r23, java.util.List<java.lang.String> r24, boolean r25, int r26) {
        /*
            Method dump skipped, instructions count: 325
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.devicepolicy.VivoCustomDpmsImpl.setAppDisabledAppList(android.content.ComponentName, int, java.util.List, boolean, int):boolean");
    }

    private List<String> getAppDisabledAppList(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            if (this.mDisableListPkgs != null && ap != null) {
                List<String> all = new ArrayList<>(this.mDisableListPkgs);
                if (ap.appDisabledApplist != null && ap.appDisabledApplist.size() > 0) {
                    all.addAll(ap.appDisabledApplist);
                }
                return all;
            }
            return ap != null ? ap.appDisabledApplist : this.mDisableListPkgs;
        }
    }

    private boolean setAppPersistentAppList(ComponentName admin, int listId, List<String> keys, boolean isAdd, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_APP_RUNNING", userHandle);
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            try {
                VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
                if (ap != null) {
                    if (ap.appPersistentApplist == null) {
                        ap.appPersistentApplist = new ArrayList();
                    }
                    List<String> relatedPkgs = getEmmRelatedPkgs(admin.getPackageName(), userHandle);
                    List<String> list = new ArrayList<>();
                    long callingId = Binder.clearCallingIdentity();
                    try {
                        try {
                            for (String pkg : keys) {
                                if ((relatedPkgs != null && relatedPkgs.contains(pkg)) || pkg.equals(admin.getPackageName())) {
                                    list.add(pkg);
                                    VLog.d(VIVO_LOG_TAG, "set persistent app: " + pkg + ", add = " + isAdd);
                                }
                            }
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    } catch (Exception e) {
                        VLog.e(VIVO_LOG_TAG, "setAppPersistentAppList exception " + e);
                    }
                    Binder.restoreCallingIdentity(callingId);
                    if (list.size() > 0) {
                        VivoUtils.addItemsFromList(ap.appPersistentApplist, list, isAdd);
                        saveVivoSettingsLocked(listId, userHandle);
                        return true;
                    }
                }
                return false;
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    private List<String> getAppPersistentAppList(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            if (this.mPersistentListPkgs != null && ap != null) {
                List<String> all = new ArrayList<>(this.mPersistentListPkgs);
                if (ap.appPersistentApplist != null && ap.appPersistentApplist.size() > 0) {
                    all.addAll(ap.appPersistentApplist);
                }
                return all;
            }
            return ap != null ? ap.appPersistentApplist : this.mPersistentListPkgs;
        }
    }

    private boolean setAppDisallowedClearDataList(ComponentName admin, int listId, List<String> keys, boolean isAdd, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_APP_PACKAGE", userHandle);
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.appDisallowedClearDatalist == null) {
                    ap.appDisallowedClearDatalist = new ArrayList();
                }
                VivoUtils.addItemsFromList(ap.appDisallowedClearDatalist, keys, isAdd);
                saveVivoSettingsLocked(listId, userHandle);
                return true;
            }
            return false;
        }
    }

    private List<String> getAppDisallowedClearDataList(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        List<String> list;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            list = ap != null ? ap.appDisallowedClearDatalist : null;
        }
        return list;
    }

    private List<String> getThirdPartyAppList(List<String> pkgs, List<String> others, int userHandle) {
        List<String> thirdList = new ArrayList<>();
        for (String pkg : pkgs) {
            try {
                PackageInfo pkgInfo = this.mIPackageManager.getPackageInfo(pkg, 0, userHandle);
                if (pkgInfo != null && pkgInfo.applicationInfo != null && (!isSystemApp(pkgInfo.applicationInfo) || (others != null && others.contains(pkg)))) {
                    thirdList.add(pkg);
                }
            } catch (Exception e) {
                VLog.w(VIVO_LOG_TAG, "getThirdPartyAppList packageNames = " + pkg + " failed! " + e);
            }
        }
        return thirdList;
    }

    private boolean setAppMeteredDataBWList(ComponentName admin, int listId, List<String> keys, boolean isAdd, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_APP_PERMISSION", userHandle);
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        List<String> others = new ArrayList<>(Arrays.asList("com.android.bbkmusic", AUTHORITY, "com.bbk.appstore", "com.vivo.game", "com.vivo.wallet", "com.vivo.space", "com.android.VideoPlayer", "com.chaozh.iReader", "com.vivo.email"));
        List<String> thirdList = getThirdPartyAppList(keys, others, userHandle);
        if (thirdList == null || thirdList.isEmpty()) {
            VLog.w(VIVO_LOG_TAG, "setAppMeteredDataBWList failed, para not allowed!");
            return false;
        }
        boolean success = false;
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (listId == 1106) {
                    if (ap.appMeteredDataBlist == null) {
                        ap.appMeteredDataBlist = new ArrayList();
                    }
                    success = VivoUtils.addItemsFromList(ap.appMeteredDataBlist, thirdList, isAdd);
                } else if (listId == 1107) {
                    if (ap.appMeteredDataWlist == null) {
                        ap.appMeteredDataWlist = new ArrayList();
                    }
                    success = VivoUtils.addItemsFromList(ap.appMeteredDataWlist, thirdList, isAdd);
                }
                saveVivoSettingsLocked(listId, userHandle);
            }
        }
        return success;
    }

    private List<String> getAppMeteredDataBWList(ComponentName admin, int listId, int userHandle) {
        VivoActiveAdmin ap;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            if (ap != null) {
                if (listId == 1106) {
                    return ap.appMeteredDataBlist;
                } else if (listId == 1107) {
                    return ap.appMeteredDataWlist;
                }
            }
            return null;
        }
    }

    private boolean setAppWlanDataBWList(ComponentName admin, int listId, List<String> keys, boolean isAdd, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_APP_PERMISSION", userHandle);
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        List<String> others = new ArrayList<>(Arrays.asList("com.android.bbkmusic", "com.android.VideoPlayer"));
        List<String> thirdList = getThirdPartyAppList(keys, others, userHandle);
        if (thirdList == null || thirdList.isEmpty()) {
            VLog.w(VIVO_LOG_TAG, "setAppMeteredDataBWList failed, para not allowed!");
            return false;
        }
        boolean success = false;
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (listId == 1108) {
                    if (ap.appWlanDataBlist == null) {
                        ap.appWlanDataBlist = new ArrayList();
                    }
                    success = VivoUtils.addItemsFromList(ap.appWlanDataBlist, thirdList, isAdd);
                } else if (listId == 1109) {
                    if (ap.appWlanDataWlist == null) {
                        ap.appWlanDataWlist = new ArrayList();
                    }
                    success = VivoUtils.addItemsFromList(ap.appWlanDataWlist, thirdList, isAdd);
                }
                saveVivoSettingsLocked(listId, userHandle);
            }
        }
        return success;
    }

    private List<String> getAppWlanDataBWList(ComponentName admin, int listId, int userHandle) {
        VivoActiveAdmin ap;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            if (ap != null) {
                if (listId == 1108) {
                    return ap.appWlanDataBlist;
                } else if (listId == 1109) {
                    return ap.appWlanDataWlist;
                }
            }
            return null;
        }
    }

    private boolean silentInstallPackage(ComponentName admin, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_APP_PACKAGE", userHandle);
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                long callingId = Binder.clearCallingIdentity();
                try {
                    String packagePath = data.getString("package_path");
                    int flags = data.getInt("args_flag_int");
                    if (TextUtils.isEmpty(packagePath)) {
                        throw new IllegalArgumentException("IllegalArgumentException:installPackage path is illegal!");
                    }
                    PackageInfo info = this.mContext.getPackageManager().getPackageArchiveInfo(packagePath, 1);
                    String packageName = info != null ? info.applicationInfo.packageName : null;
                    ServiceManager.getService("package");
                    VLog.d(VIVO_LOG_TAG, "silentInstallPackage packagePath:" + packagePath + " flags:" + flags + " packageName:" + packageName);
                    Binder.restoreCallingIdentity(callingId);
                    return true;
                } catch (Exception e) {
                    VLog.e(VIVO_LOG_TAG, "silentInstallPackage " + e);
                    Binder.restoreCallingIdentity(callingId);
                    return false;
                }
            }
            return false;
        }
    }

    private boolean silentUnInstallPackage(ComponentName admin, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_APP_PACKAGE", userHandle);
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                long callingId = Binder.clearCallingIdentity();
                try {
                    String packageName = data.getString("package_name");
                    int flags = data.getInt("args_flag_int");
                    if (TextUtils.isEmpty(packageName)) {
                        Binder.restoreCallingIdentity(callingId);
                        return false;
                    }
                    VLog.d(VIVO_LOG_TAG, "silentUnInstallPackage packageName:" + packageName + " flags:" + flags);
                    PackageInfo info = this.mContext.getPackageManager().getPackageInfo(packageName, 0);
                    if (info != null) {
                        VersionedPackage vp = new VersionedPackage(packageName, info.versionCode);
                        this.mIPackageManager.deletePackageVersioned(vp, new PackageManager.LegacyPackageDeleteObserver((IPackageDeleteObserver) null).getBinder(), userHandle, flags);
                    }
                    Binder.restoreCallingIdentity(callingId);
                    return true;
                } catch (PackageManager.NameNotFoundException | RemoteException e) {
                    VLog.e(VIVO_LOG_TAG, "silentUnInstallPackage " + e);
                    Binder.restoreCallingIdentity(callingId);
                    return false;
                }
            }
            return false;
        }
    }

    public boolean isAllowSlientInstall(int uid) {
        boolean isHavePermission = false;
        int userId = UserHandle.getUserId(uid);
        ComponentName component = getVivoAdminUncheckedLocked(userId);
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdminUncheckedLocked(component, userId);
            if (ap != null) {
                List<String> permList = getEmmPermissions(ap.mInfo.getPackageName(), userId);
                if (permList.contains("EMM_APP_PACKAGE")) {
                    VLog.i(VIVO_LOG_TAG, "Admin " + component + " have permission emm app package");
                    isHavePermission = true;
                }
            }
        }
        return isVivoEmmUid(uid) && isHavePermission;
    }

    private boolean killProcess(ComponentName admin, Bundle data, int userHandle) {
        ArrayList<String> processList;
        checkCallingEmmPermission(admin, "EMM_APP_RUNNING", userHandle);
        synchronized (getLockObject()) {
            try {
            } catch (Throwable th) {
                th = th;
            }
            try {
                VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
                if (ap != null) {
                    long callingId = Binder.clearCallingIdentity();
                    try {
                        processList = new ArrayList<>();
                    } catch (Exception e) {
                        e = e;
                    } catch (Throwable th2) {
                        e = th2;
                        Binder.restoreCallingIdentity(callingId);
                        throw e;
                    }
                    try {
                        try {
                            processList.addAll(data.getStringArrayList("process_name_list"));
                            if (processList.isEmpty()) {
                                Binder.restoreCallingIdentity(callingId);
                                return false;
                            }
                            List<ActivityManager.RunningAppProcessInfo> processes = this.mActivityManager.getRunningAppProcesses();
                            for (ActivityManager.RunningAppProcessInfo info : processes) {
                                for (int i = 0; i < processList.size(); i++) {
                                    if (processList.get(i).equals(info.processName)) {
                                        Process.killProcess(info.pid);
                                        VLog.d(VIVO_LOG_TAG, "kill " + info.processName + " pid = " + info.pid);
                                    }
                                }
                            }
                            Binder.restoreCallingIdentity(callingId);
                            return true;
                        } catch (Throwable th3) {
                            e = th3;
                            Binder.restoreCallingIdentity(callingId);
                            throw e;
                        }
                    } catch (Exception e2) {
                        e = e2;
                        VLog.e(VIVO_LOG_TAG, "killProcess " + e);
                        Binder.restoreCallingIdentity(callingId);
                        return false;
                    }
                }
                return false;
            } catch (Throwable th4) {
                th = th4;
                throw th;
            }
        }
    }

    private boolean forceStopPackage(ComponentName admin, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_APP_RUNNING", userHandle);
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                long callingId = Binder.clearCallingIdentity();
                try {
                    ArrayList<String> packageList = new ArrayList<>();
                    packageList.addAll(data.getStringArrayList("package_name_list"));
                    if (packageList.isEmpty()) {
                        Binder.restoreCallingIdentity(callingId);
                        return false;
                    }
                    for (int i = 0; i < packageList.size(); i++) {
                        this.mActivityManager.forceStopPackage(packageList.get(i));
                    }
                    Binder.restoreCallingIdentity(callingId);
                    return true;
                } catch (Exception e) {
                    VLog.e(VIVO_LOG_TAG, "forceStopPackage " + e);
                    Binder.restoreCallingIdentity(callingId);
                    return false;
                }
            }
            return false;
        }
    }

    private boolean clearBackgroundApps(ComponentName admin, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_APP_RUNNING", userHandle);
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                long callingId = Binder.clearCallingIdentity();
                boolean includeWhite = data.getBoolean("state_boolean");
                String packageName = ap.mInfo.getPackageName();
                ArrayList<String> reservePkgLsit = new ArrayList<>();
                Intent intent = new Intent();
                intent.setPackage(SPEED_UP_PACKAGE_NAME);
                intent.setAction(SPEED_UP_ACTION_NAME);
                intent.putExtra("PKGNAME", packageName);
                intent.putExtra("INCLUDEWHITE", includeWhite);
                reservePkgLsit.add(packageName);
                intent.putStringArrayListExtra("reserve_pkgs", reservePkgLsit);
                this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
                Binder.restoreCallingIdentity(callingId);
                return true;
            }
            return false;
        }
    }

    private Bundle getRunningAppProcesses(ComponentName admin, int userHandle) {
        Bundle bundle;
        ArrayList<ActivityManager.RunningAppProcessInfo> processes;
        int i;
        checkCallingEmmPermission(admin, "EMM_APP_RUNNING", userHandle);
        synchronized (getLockObject()) {
            bundle = new Bundle();
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                long callingId = Binder.clearCallingIdentity();
                try {
                    ArrayList<ActivityManager.RunningAppProcessInfo> processes2 = (ArrayList) this.mActivityManager.getRunningAppProcesses();
                    ArrayList<ActivityManager.RunningAppProcessInfo> list = new ArrayList<>();
                    Intent mainIntent = new Intent("android.intent.action.MAIN", (Uri) null);
                    mainIntent.addCategory("android.intent.category.LAUNCHER");
                    List<ResolveInfo> apps = this.mContext.getPackageManager().queryIntentActivities(mainIntent, 0);
                    Iterator<ActivityManager.RunningAppProcessInfo> it = processes2.iterator();
                    while (it.hasNext()) {
                        ActivityManager.RunningAppProcessInfo appProcess = it.next();
                        int importance = appProcess.importance;
                        if (importance <= 200) {
                            processes = processes2;
                        } else {
                            String[] pkgList = appProcess.pkgList;
                            processes = processes2;
                            int length = pkgList.length;
                            int i2 = 0;
                            while (i2 < length) {
                                String packageName = pkgList[i2];
                                Iterator<ResolveInfo> it2 = apps.iterator();
                                while (true) {
                                    if (!it2.hasNext()) {
                                        i = length;
                                        break;
                                    }
                                    ResolveInfo appInfo = it2.next();
                                    i = length;
                                    String packageName2 = packageName;
                                    if (!TextUtils.equals(appInfo.activityInfo.packageName, packageName2)) {
                                        packageName = packageName2;
                                        length = i;
                                    } else if (!list.contains(appProcess)) {
                                        list.add(appProcess);
                                    }
                                }
                                i2++;
                                length = i;
                            }
                        }
                        processes2 = processes;
                    }
                    bundle.putParcelableArrayList("running_app_list", list);
                    Binder.restoreCallingIdentity(callingId);
                } catch (Exception e) {
                    VLog.e(VIVO_LOG_TAG, "getRunningAppProcesses " + e);
                    Binder.restoreCallingIdentity(callingId);
                }
            }
        }
        return bundle;
    }

    private boolean setComponentEnabledSetting(ComponentName admin, int transId, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_APP_RUNNING", userHandle);
        if (data == null) {
            return false;
        }
        synchronized (getLockObject()) {
            try {
                try {
                    VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
                    if (ap != null) {
                        long callingId = Binder.clearCallingIdentity();
                        List<String> temp = new ArrayList<>();
                        List<ComponentName> comList = data.getParcelableArrayList("component_name_list");
                        int policy = data.getInt("state_int");
                        int flags = data.getInt("args_flag_int");
                        try {
                            VLog.e(VIVO_LOG_TAG, "setComponentEnabledSetting comList = " + comList + ", policy = " + policy + ", flags = " + flags);
                        } catch (Exception e) {
                            VLog.e(VIVO_LOG_TAG, "setComponentEnabledSetting " + e);
                        }
                        if (comList != null && !comList.isEmpty()) {
                            if (ap.appComponentDisablelist == null) {
                                ap.appComponentDisablelist = new ArrayList();
                            }
                            for (ComponentName component : comList) {
                                if (isAllowDisableComponent(component.getPackageName(), userHandle)) {
                                    try {
                                        this.mIPackageManager.setComponentEnabledSetting(component, policy, flags, userHandle);
                                        temp.add(component.flattenToString());
                                    } catch (RemoteException e2) {
                                        VLog.e(VIVO_LOG_TAG, "setComponentEnabledSetting fail " + e2);
                                    }
                                } else {
                                    VLog.i(VIVO_LOG_TAG, "setComponentEnabledSetting pkg" + component.getPackageName() + " is not exist or not allowed to disable");
                                }
                            }
                            Binder.restoreCallingIdentity(callingId);
                            VivoUtils.addItemsFromList(ap.appComponentDisablelist, temp, policy != 0);
                            saveVivoSettingsLocked(transId, userHandle);
                            return true;
                        }
                        Binder.restoreCallingIdentity(callingId);
                        return false;
                    }
                    return false;
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    private boolean setDefaultApp(ComponentName admin, int transId, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_APPLICATION", userHandle);
        if (data != null) {
            ComponentName comp = (ComponentName) data.getParcelable("component_name");
            if (comp != null) {
                boolean disModi = data.getBoolean("state_boolean");
                synchronized (getLockObject()) {
                    VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
                    if (ap != null) {
                        long callingId = Binder.clearCallingIdentity();
                        try {
                            Integer id = new Integer(transId);
                            ap.mDefaultAppInfoMap.put(id, new VivoActiveAdmin.VivoDefaultAppInfo(comp, disModi));
                            VLog.i(VIVO_LOG_TAG, "setDefaultApp comp = " + comp);
                            if (transId == 3009) {
                                writeAllowReplaceDesktopState();
                                try {
                                    ResolveInfo ri = this.mIPackageManager.findPersistentPreferredActivity(getHomeIntent(), userHandle);
                                    if (ri != null && ri.getComponentInfo() != null) {
                                        this.mIPackageManager.clearPackagePersistentPreferredActivities(ri.getComponentInfo().packageName, userHandle);
                                    }
                                    if (disModi) {
                                        this.mIPackageManager.addPersistentPreferredActivity(getHomeFilter(), comp, userHandle);
                                    }
                                    this.mIPackageManager.flushPackageRestrictionsAsUser(userHandle);
                                } catch (RemoteException e) {
                                }
                            } else if (transId == 3012) {
                                String name = comp.getPackageName() + "/" + comp.getShortClassName();
                                VLog.i(VIVO_LOG_TAG, "setDefaultInputMethod name = " + name);
                                Settings.Secure.putStringForUser(this.mContentResolver, "enabled_input_methods", name, userHandle);
                                Settings.Secure.putStringForUser(this.mContentResolver, "default_input_method", name, userHandle);
                            } else if (transId == 3008) {
                                SmsApplication.setDefaultApplication(comp.getPackageName(), this.mContext);
                                try {
                                    ResolveInfo ri2 = this.mIPackageManager.findPersistentPreferredActivity(getSmsIntent(), userHandle);
                                    if (ri2 != null && ri2.getComponentInfo() != null) {
                                        VLog.i(VIVO_LOG_TAG, "clearDefaultSMS name = " + ri2.getComponentInfo());
                                        this.mIPackageManager.clearPackagePersistentPreferredActivities(ri2.getComponentInfo().packageName, userHandle);
                                    }
                                    if (disModi) {
                                        this.mIPackageManager.addPersistentPreferredActivity(getSmsFilter(), comp, userHandle);
                                    }
                                    this.mIPackageManager.flushPackageRestrictionsAsUser(userHandle);
                                } catch (RemoteException e2) {
                                }
                            } else if (transId == 3011) {
                                try {
                                    ResolveInfo ri3 = this.mIPackageManager.findPersistentPreferredActivity(getEmailIntent(), userHandle);
                                    if (ri3 != null && ri3.getComponentInfo() != null) {
                                        VLog.i(VIVO_LOG_TAG, "clearDefaultEMAIL name = " + ri3.getComponentInfo());
                                        this.mIPackageManager.clearPackagePersistentPreferredActivities(ri3.getComponentInfo().packageName, userHandle);
                                    }
                                    if (disModi) {
                                        this.mIPackageManager.addPersistentPreferredActivity(getEmailFilter(), comp, userHandle);
                                    }
                                    this.mIPackageManager.flushPackageRestrictionsAsUser(userHandle);
                                } catch (RemoteException e3) {
                                }
                            } else if (transId == 3010) {
                                try {
                                    ResolveInfo ri4 = this.mIPackageManager.findPersistentPreferredActivity(getBrowserIntent(), userHandle);
                                    if (ri4 != null && ri4.getComponentInfo() != null) {
                                        VLog.i(VIVO_LOG_TAG, "clearDefaultBROWSER name = " + ri4.getComponentInfo());
                                        this.mIPackageManager.clearPackagePersistentPreferredActivities(ri4.getComponentInfo().packageName, userHandle);
                                    }
                                    if (disModi) {
                                        this.mIPackageManager.addPersistentPreferredActivity(getBrowserFilter(), comp, userHandle);
                                    }
                                    this.mIPackageManager.flushPackageRestrictionsAsUser(userHandle);
                                } catch (RemoteException e4) {
                                }
                            }
                            Binder.restoreCallingIdentity(callingId);
                            saveVivoSettingsLocked(transId, userHandle);
                            return true;
                        } catch (Exception e5) {
                            VLog.e(VIVO_LOG_TAG, "setDefaultApp failed");
                            e5.printStackTrace();
                            Binder.restoreCallingIdentity(callingId);
                            return false;
                        }
                    }
                    return false;
                }
            }
            throw new IllegalArgumentException("IllegalArgumentException: setDefaultApp para is illegal!");
        }
        throw new IllegalArgumentException("IllegalArgumentException: setDefaultApp para is illegal!");
    }

    private void writeAllowReplaceDesktopState() {
        Settings.Secure.putInt(this.mContentResolver, "desktop_usage_rights_enabled", 1);
    }

    private Intent getHomeIntent() {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        intent.addCategory("android.intent.category.DEFAULT");
        return intent;
    }

    private IntentFilter getHomeFilter() {
        IntentFilter filter = new IntentFilter("android.intent.action.MAIN");
        filter.addCategory("android.intent.category.HOME");
        filter.addCategory("android.intent.category.DEFAULT");
        return filter;
    }

    private Intent getBrowserIntent() {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.BROWSABLE");
        intent.setData(Uri.parse("https:"));
        intent.addCategory("android.intent.category.DEFAULT");
        return intent;
    }

    private IntentFilter getBrowserFilter() {
        IntentFilter filter = new IntentFilter("android.intent.action.VIEW");
        filter.addCategory("android.intent.category.BROWSABLE");
        filter.addDataScheme("https");
        filter.addCategory("android.intent.category.DEFAULT");
        return filter;
    }

    private Intent getEmailIntent() {
        Intent intent = new Intent("android.intent.action.SENDTO");
        intent.setData(Uri.parse("mailto:"));
        intent.addCategory("android.intent.category.DEFAULT");
        return intent;
    }

    private IntentFilter getEmailFilter() {
        IntentFilter filter = new IntentFilter("android.intent.action.SENDTO");
        filter.addDataScheme("mailto");
        filter.addCategory("android.intent.category.DEFAULT");
        return filter;
    }

    private Intent getSmsIntent() {
        Intent intent = new Intent("android.intent.action.SENDTO");
        intent.setData(Uri.parse("smsto:"));
        intent.addCategory("android.intent.category.DEFAULT");
        return intent;
    }

    private IntentFilter getSmsFilter() {
        IntentFilter filter = new IntentFilter("android.intent.action.SENDTO");
        filter.addDataScheme("smsto");
        filter.addCategory("android.intent.category.DEFAULT");
        return filter;
    }

    private Bundle getDefaultApp(ComponentName admin, int transId, int userHandle) {
        VivoActiveAdmin ap;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            Bundle bundle = new Bundle();
            if (ap != null) {
                Integer id = new Integer(transId);
                VivoActiveAdmin.VivoDefaultAppInfo defaultApp = ap.mDefaultAppInfoMap.get(id);
                if (defaultApp == null) {
                    if (transId == 3010 && this.mDefaultBrowser != null) {
                        if (isPkgExist(this.mDefaultBrowser.getPackageName(), userHandle)) {
                            bundle.putParcelable("component_name", this.mDefaultBrowser);
                            bundle.putBoolean("state_boolean", true);
                        }
                    } else if (transId == 3009 && this.mDefaultLauncher != null && isPkgExist(this.mDefaultLauncher.getPackageName(), userHandle)) {
                        bundle.putParcelable("component_name", this.mDefaultLauncher);
                        bundle.putBoolean("state_boolean", true);
                    }
                } else if (defaultApp.componentName != null && isPkgExist(defaultApp.componentName.getPackageName(), userHandle)) {
                    bundle.putParcelable("component_name", defaultApp.componentName);
                    bundle.putBoolean("state_boolean", defaultApp.disableModify);
                    VLog.i(VIVO_LOG_TAG, "getDefaultApp transId = " + transId + ", componentName =" + defaultApp.componentName.flattenToString() + ", disableModify = " + defaultApp.disableModify);
                }
                return bundle;
            } else if (transId == 3010 && this.mDefaultBrowser != null) {
                if (isPkgExist(this.mDefaultBrowser.getPackageName(), userHandle)) {
                    bundle.putParcelable("component_name", this.mDefaultBrowser);
                    bundle.putBoolean("state_boolean", true);
                }
                return bundle;
            } else if (transId == 3009 && this.mDefaultLauncher != null) {
                if (isPkgExist(this.mDefaultLauncher.getPackageName(), userHandle)) {
                    bundle.putParcelable("component_name", this.mDefaultLauncher);
                    bundle.putBoolean("state_boolean", true);
                }
                return bundle;
            } else {
                return null;
            }
        }
    }

    private boolean setLockTaskFeatures(ComponentName admin, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_APP_RUNNING", userHandle);
        if (data == null) {
            throw new IllegalArgumentException("IllegalArgumentException: setLockTaskFeatures para is illegal!");
        }
        int flags = data.getInt("state_int");
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                this.mService.setLockTaskFeatures(admin, flags);
                return true;
            }
            return false;
        }
    }

    private Bundle getLockTaskFeatures(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            if (ap != null) {
                int flags = this.mService.getLockTaskFeatures(admin);
                VLog.i(VIVO_LOG_TAG, "getLockTaskFeatures flags: " + flags);
                Bundle bundle = new Bundle();
                bundle.putInt("state_int", flags);
                return bundle;
            }
            return null;
        }
    }

    private boolean setLockTaskPackages(ComponentName admin, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_APP_RUNNING", userHandle);
        if (data == null) {
            throw new IllegalArgumentException("IllegalArgumentException: setLockTaskPackages para is illegal!");
        }
        String[] packages = data.getStringArray("state_string");
        if (packages == null || packages.length == 0) {
            throw new IllegalArgumentException("IllegalArgumentException: setLockTaskPackages para is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                this.mService.setLockTaskPackages(admin, packages);
                return true;
            }
            return false;
        }
    }

    private Bundle getLockTaskPackages(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            if (ap != null) {
                String[] packages = this.mService.getLockTaskPackages(admin);
                if (packages != null) {
                    VLog.i(VIVO_LOG_TAG, "getLockTaskPackages packages: " + String.join(",", packages));
                }
                Bundle bundle = new Bundle();
                bundle.putStringArray("state_string", packages);
                return bundle;
            }
            return null;
        }
    }

    private boolean isLockTaskPermitted(ComponentName admin, Bundle data, int userHandle) {
        if (data == null) {
            throw new IllegalArgumentException("IllegalArgumentException: isLockTaskPermitted para is illegal!");
        }
        String pkg = data.getString("state_string");
        if (pkg == null) {
            throw new IllegalArgumentException("IllegalArgumentException: isLockTaskPermitted para is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(getVivoAdminUncheckedLocked(userHandle), userHandle);
            if (ap != null) {
                return this.mService.isLockTaskPermitted(pkg);
            }
            return false;
        }
    }

    private boolean setAppInstallPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        boolean z;
        checkCallingEmmPermission(admin, "EMM_APP_PACKAGE", userHandle);
        if (policy != 0 && policy != 1) {
            throw new IllegalArgumentException("IllegalArgumentException: setAppInstallPolicy input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap == null) {
                return false;
            }
            DevicePolicyManagerService devicePolicyManagerService = this.mService;
            if (policy == 1) {
                z = true;
            } else {
                z = false;
            }
            devicePolicyManagerService.setUserRestriction(admin, "no_install_apps", z, false);
            return true;
        }
    }

    private int getAppInstallPolicy(ComponentName admin, int userHandle) {
        synchronized (getLockObject()) {
            Bundle restrictions = this.mService.getUserRestrictions(admin, false);
            if (restrictions == null) {
                return 0;
            }
            boolean isRestrict = restrictions.getBoolean("no_install_apps", false);
            return isRestrict ? 1 : 0;
        }
    }

    private boolean setAppUnInstallPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        boolean z;
        checkCallingEmmPermission(admin, "EMM_APP_PACKAGE", userHandle);
        if (policy != 0 && policy != 1) {
            throw new IllegalArgumentException("IllegalArgumentException: setAppUnInstallPolicy input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap == null) {
                return false;
            }
            DevicePolicyManagerService devicePolicyManagerService = this.mService;
            if (policy == 1) {
                z = true;
            } else {
                z = false;
            }
            devicePolicyManagerService.setUserRestriction(admin, "no_uninstall_apps", z, false);
            return true;
        }
    }

    private int getAppUnInstallPolicy(ComponentName admin, int userHandle) {
        synchronized (getLockObject()) {
            Bundle restrictions = this.mService.getUserRestrictions(admin, false);
            if (restrictions == null) {
                return 0;
            }
            boolean isRestrict = restrictions.getBoolean("no_uninstall_apps", false);
            return isRestrict ? 1 : 0;
        }
    }

    private boolean setInstallUnknownSourcePolicy(ComponentName admin, int polId, int policy, int userHandle) {
        boolean z;
        checkCallingEmmPermission(admin, "EMM_APP_PACKAGE", userHandle);
        if (policy != 0 && policy != 1) {
            throw new IllegalArgumentException("IllegalArgumentException: setInstallUnknownSourcePolicy input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap == null) {
                return false;
            }
            DevicePolicyManagerService devicePolicyManagerService = this.mService;
            if (policy == 1) {
                z = true;
            } else {
                z = false;
            }
            devicePolicyManagerService.setUserRestriction(admin, "no_install_unknown_sources", z, false);
            return true;
        }
    }

    private int getInstallUnknownSourcePolicy(ComponentName admin, int userHandle) {
        synchronized (getLockObject()) {
            Bundle restrictions = this.mService.getUserRestrictions(admin, false);
            if (restrictions == null) {
                return 0;
            }
            boolean isRestrict = restrictions.getBoolean("no_install_unknown_sources", false);
            return isRestrict ? 1 : 0;
        }
    }

    private boolean setAppControlPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        boolean z;
        checkCallingEmmPermission(admin, "EMM_APP_PACKAGE", userHandle);
        if (policy != 0 && policy != 1) {
            throw new IllegalArgumentException("IllegalArgumentException: setAppControlPolicy input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap == null) {
                return false;
            }
            DevicePolicyManagerService devicePolicyManagerService = this.mService;
            if (policy == 1) {
                z = true;
            } else {
                z = false;
            }
            devicePolicyManagerService.setUserRestriction(admin, "no_control_apps", z, false);
            return true;
        }
    }

    private int getAppControlPolicy(ComponentName admin, int userHandle) {
        synchronized (getLockObject()) {
            Bundle restrictions = this.mService.getUserRestrictions(admin, false);
            if (restrictions == null) {
                return 0;
            }
            boolean isRestrict = restrictions.getBoolean("no_control_apps", false);
            return isRestrict ? 1 : 0;
        }
    }

    private boolean clearAppData(ComponentName admin, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_APP_PACKAGE", userHandle);
        if (data == null) {
            throw new IllegalArgumentException("IllegalArgumentException: clearAppData para is illegal!");
        }
        String packageName = data.getString("package_name");
        synchronized (getLockObject()) {
            this.mService.clearApplicationUserData(admin, packageName, new IPackageDataObserver.Stub() { // from class: com.android.server.devicepolicy.VivoCustomDpmsImpl.5
                public void onRemoveCompleted(String pkg, boolean succeeded) {
                }
            });
        }
        return true;
    }

    private boolean setAppNotification(ComponentName admin, int polId, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPERATION", userHandle);
        if (data != null) {
            int policy = data.getInt("state_int");
            String packageName = data.getString("package_name");
            if (policy == 5 || policy == 6) {
                synchronized (getLockObject()) {
                    VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
                    if (ap != null && packageName != null) {
                        long callingId = -1;
                        try {
                            NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
                            INotificationManager mNotificationService = NotificationManager.getService();
                            VLog.w(VIVO_LOG_TAG, "setAppNotification pkg: " + packageName + ", policy: " + policy);
                            int uid = this.mContext.getPackageManager().getPackageUidAsUser(packageName, userHandle);
                            callingId = Binder.clearCallingIdentity();
                            mNotificationService.setNotificationsEnabledForPackage(packageName, uid, policy == 5);
                            if (callingId != -1) {
                                Binder.restoreCallingIdentity(callingId);
                            }
                            return true;
                        } catch (Exception e) {
                            e.printStackTrace();
                            if (callingId != -1) {
                                Binder.restoreCallingIdentity(callingId);
                            }
                        }
                    }
                    return false;
                }
            }
            throw new IllegalArgumentException("IllegalArgumentException: setAppNotification input is illegal!");
        }
        throw new IllegalArgumentException("IllegalArgumentException: setAppNotification para is illegal!");
    }

    private Bundle getAppNotification(ComponentName admin, Bundle data, int userHandle) {
        Bundle bundle;
        if (data == null) {
            throw new IllegalArgumentException("IllegalArgumentException: getAppNotification para is illegal!");
        }
        synchronized (getLockObject()) {
            String packageName = data.getString("package_name");
            bundle = new Bundle();
            if (admin != null) {
                VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
                if (ap != null && packageName != null) {
                    try {
                        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
                        INotificationManager mNotificationService = NotificationManager.getService();
                        int uid = this.mContext.getPackageManager().getPackageUidAsUser(packageName, userHandle);
                        boolean enable = mNotificationService.areNotificationsEnabledForPackage(packageName, uid);
                        VLog.w(VIVO_LOG_TAG, "getAppNotification pkg: " + packageName + ", enable: " + enable);
                        bundle.putInt("state_int", enable ? 5 : 6);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return bundle;
    }

    private boolean setNetworkApnPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_NET_APN", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.networkApnPolicy != policy) {
                    ap.networkApnPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getNetworkApnPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.networkApnPolicy : 0;
        }
        return i;
    }

    private boolean setNetworkMobileDataPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_NETWORK", userHandle);
        if (policy < 0 || policy > 2) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.networkMobileDataPolicy != policy) {
                    ap.networkMobileDataPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getNetworkMobileDataPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.networkMobileDataPolicy : 0;
        }
        return i;
    }

    private boolean setNetworkDomainBWPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_NET_ADDRESS", userHandle);
        if (policy != 0 && policy != 3 && policy != 4) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.networkDomainBWPolicy != policy) {
                    ap.networkDomainBWPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getNetworkDomainBWPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.networkDomainBWPolicy : 0;
        }
        return i;
    }

    private boolean setNetworkIpAddrBWPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_NET_ADDRESS", userHandle);
        if (policy != 0 && policy != 3 && policy != 4) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.networkIpAddrBWPolicy != policy) {
                    ap.networkIpAddrBWPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getNetworkIpAddrBWPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.networkIpAddrBWPolicy : 0;
        }
        return i;
    }

    private boolean setNetworkDomainBWList(ComponentName admin, int listId, List<String> keys, boolean isAdd, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_NET_ADDRESS", userHandle);
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        boolean success = false;
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (listId == 1201) {
                    if (ap.networkDomainBlist == null) {
                        ap.networkDomainBlist = new ArrayList();
                    }
                    success = VivoUtils.addItemsFromList(ap.networkDomainBlist, keys, isAdd);
                } else if (listId == 1202) {
                    if (ap.networkDomainWlist == null) {
                        ap.networkDomainWlist = new ArrayList();
                    }
                    success = VivoUtils.addItemsFromList(ap.networkDomainWlist, keys, isAdd);
                }
                saveVivoSettingsLocked(listId, userHandle);
            }
        }
        return success;
    }

    private List<String> getNetworkDomainBWList(ComponentName admin, int listId, int userHandle) {
        VivoActiveAdmin ap;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            if (ap != null) {
                if (listId == 1201) {
                    return ap.networkDomainBlist;
                } else if (listId == 1202) {
                    return ap.networkDomainWlist;
                }
            }
            return null;
        }
    }

    private boolean setNetworkIpAddrBWList(ComponentName admin, int listId, List<String> keys, boolean isAdd, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_NET_ADDRESS", userHandle);
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        boolean success = false;
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (listId == 1203) {
                    if (ap.networkIpAddrBlist == null) {
                        ap.networkIpAddrBlist = new ArrayList();
                    }
                    success = VivoUtils.addItemsFromList(ap.networkIpAddrBlist, keys, isAdd);
                } else if (listId == 1204) {
                    if (ap.networkIpAddrWlist == null) {
                        ap.networkIpAddrWlist = new ArrayList();
                    }
                    success = VivoUtils.addItemsFromList(ap.networkIpAddrWlist, keys, isAdd);
                }
                saveVivoSettingsLocked(listId, userHandle);
            }
        }
        return success;
    }

    private List<String> getNetworkIpAddrBWList(ComponentName admin, int listId, int userHandle) {
        VivoActiveAdmin ap;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            if (ap != null) {
                if (listId == 1203) {
                    return ap.networkIpAddrBlist;
                } else if (listId == 1204) {
                    return ap.networkIpAddrWlist;
                }
            }
            return null;
        }
    }

    private boolean addVpnProfile(ComponentName admin, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_NET_VPN", userHandle);
        long callingId = -1;
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                try {
                    VpnProfile vpnProfile = data.getParcelable("vpn_profile");
                    boolean connect = data.getBoolean("state_boolean");
                    boolean alwaysOn = data.getBoolean("state_boolean2");
                    if (vpnProfile == null) {
                        if (-1 != -1) {
                            Binder.restoreCallingIdentity(-1L);
                        }
                        return false;
                    }
                    VLog.w(VIVO_LOG_TAG, "the vpnProfile key is: " + vpnProfile.key);
                    KeyStore keyStore = KeyStore.getInstance();
                    keyStore.put("VPN_" + vpnProfile.key, vpnProfile.encode(), -1, 0);
                    String str = vpnProfile.key;
                    KeyStore keyStore2 = KeyStore.getInstance();
                    if (VpnProfile.decode(str, keyStore2.get("VPN_" + vpnProfile.key)) == null) {
                        if (-1 != -1) {
                            Binder.restoreCallingIdentity(-1L);
                        }
                        return false;
                    }
                    if (this.mConnectivityManager == null) {
                        this.mConnectivityManager = IConnectivityManager.Stub.asInterface(ServiceManager.getService("connectivity"));
                    }
                    if (alwaysOn) {
                        callingId = Binder.clearCallingIdentity();
                        if (!KeyStore.getInstance().put("LOCKDOWN_VPN", vpnProfile.key.getBytes(), -1, 0)) {
                            KeyStore keyStore3 = KeyStore.getInstance();
                            keyStore3.delete("VPN_" + vpnProfile.key, -1);
                            if (callingId != -1) {
                                Binder.restoreCallingIdentity(callingId);
                            }
                            return false;
                        }
                    } else if (connect) {
                        this.mConnectivityManager.startLegacyVpn(vpnProfile);
                    }
                    if (ap.networkVpnlist == null) {
                        ap.networkVpnlist = new ArrayList();
                    }
                    List<String> keys = new ArrayList<>();
                    keys.add(vpnProfile.key);
                    VivoUtils.addItemsFromList(ap.networkVpnlist, keys, true);
                    saveVivoSettingsLocked(3101, userHandle);
                    if (callingId != -1) {
                        Binder.restoreCallingIdentity(callingId);
                    }
                    try {
                        if (alwaysOn) {
                            try {
                                callingId = Binder.clearCallingIdentity();
                                this.mConnectivityManager.updateLockdownVpn();
                            } catch (Exception e) {
                                VLog.e(VIVO_LOG_TAG, "updateLockdownVpn fail! " + e);
                                if (callingId != -1) {
                                    Binder.restoreCallingIdentity(callingId);
                                }
                                return false;
                            }
                        }
                        if (callingId != -1) {
                            Binder.restoreCallingIdentity(callingId);
                        }
                        return true;
                    } catch (Throwable th) {
                        if (callingId != -1) {
                            Binder.restoreCallingIdentity(callingId);
                        }
                        throw th;
                    }
                } catch (Exception e2) {
                    VLog.e(VIVO_LOG_TAG, "addVpnProfile fail! " + e2);
                    if (-1 != -1) {
                        Binder.restoreCallingIdentity(-1L);
                    }
                    return false;
                }
            }
            return false;
        }
    }

    private Bundle getVpnProfile(ComponentName admin, int userHandle) {
        Bundle bundle;
        String[] list;
        synchronized (getLockObject()) {
            bundle = new Bundle();
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                try {
                    ArrayList<? extends Parcelable> arrayList = new ArrayList<>();
                    KeyStore keyStore = KeyStore.getInstance();
                    for (String key : keyStore.list("VPN_")) {
                        VpnProfile profile = VpnProfile.decode(key, keyStore.get("VPN_" + key));
                        arrayList.add(profile);
                        VLog.i(VIVO_LOG_TAG, "getVpnProfile key: " + profile.key + ", name:" + profile.name);
                    }
                    bundle.putParcelableArrayList("vpn_profile_list", arrayList);
                } catch (Exception e) {
                    VLog.e(VIVO_LOG_TAG, "getVpnProfile fail! " + e);
                }
            }
        }
        return bundle;
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Unreachable block: B:44:0x00e2
        	at jadx.core.dex.visitors.blocks.BlockProcessor.checkForUnreachableBlocks(BlockProcessor.java:81)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.processBlocksTree(BlockProcessor.java:47)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.visit(BlockProcessor.java:39)
        */
    private boolean delVpnProfile(android.content.ComponentName r17, android.os.Bundle r18, int r19) {
        /*
            Method dump skipped, instructions count: 292
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.devicepolicy.VivoCustomDpmsImpl.delVpnProfile(android.content.ComponentName, android.os.Bundle, int):boolean");
    }

    /* JADX WARN: Not initialized variable reg: 31, insn: 0x02d4: MOVE  (r8 I:??[long, double]) = (r31 I:??[long, double] A[D('trafficBytes' long)]), block:B:117:0x02d4 */
    private Bundle getNetworkAppTrafficBytes(ComponentName admin, int transId, Bundle data, int userHandle) {
        long callingId;
        long wifi_bytes_rx;
        long wifi_bytes_tx;
        long sim_data_bytes_rx;
        long sim_data_bytes_tx;
        long currentTime;
        long trafficBytes;
        long trafficBytes2;
        Class<?> pm;
        int uid;
        int[] subIds;
        int[] subIds2;
        checkCallingEmmPermission(admin, "EMM_NETWORK", userHandle);
        if (data != null) {
            int mode = data.getInt("state_int");
            int direct = data.getInt("state_int2");
            String packageName = data.getString("package_name");
            synchronized (getLockObject()) {
                try {
                    try {
                        callingId = Binder.clearCallingIdentity();
                        wifi_bytes_rx = 0;
                        wifi_bytes_tx = 0;
                        sim_data_bytes_rx = 0;
                        sim_data_bytes_tx = 0;
                        currentTime = System.currentTimeMillis();
                    } catch (Throwable th) {
                        th = th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
                try {
                    try {
                        Class<?> pm2 = Class.forName("android.content.pm.IPackageManager");
                        int uid2 = Build.VERSION.SDK_INT;
                        if (uid2 <= 23) {
                            try {
                                pm = pm2;
                                Method getPackageUid = pm.getMethod("getPackageUid", String.class, Integer.TYPE);
                                trafficBytes = -1;
                                try {
                                    uid = ((Integer) getPackageUid.invoke(this.mIPackageManager, packageName, Integer.valueOf(userHandle))).intValue();
                                } catch (Exception e) {
                                    e = e;
                                    VLog.e(VIVO_LOG_TAG, "getNetworkAppTrafficBytes fail! " + e);
                                    Binder.restoreCallingIdentity(callingId);
                                    trafficBytes2 = trafficBytes;
                                    Bundle bundle = new Bundle();
                                    bundle.putLong("state_long", trafficBytes2);
                                    return bundle;
                                } catch (Throwable th3) {
                                    th = th3;
                                    Binder.restoreCallingIdentity(callingId);
                                    throw th;
                                }
                            } catch (Exception e2) {
                                e = e2;
                                trafficBytes = -1;
                                VLog.e(VIVO_LOG_TAG, "getNetworkAppTrafficBytes fail! " + e);
                                Binder.restoreCallingIdentity(callingId);
                                trafficBytes2 = trafficBytes;
                                Bundle bundle2 = new Bundle();
                                bundle2.putLong("state_long", trafficBytes2);
                                return bundle2;
                            } catch (Throwable th4) {
                                th = th4;
                                Binder.restoreCallingIdentity(callingId);
                                throw th;
                            }
                        } else {
                            trafficBytes = -1;
                            pm = pm2;
                            try {
                                Method getPackageUid2 = pm.getMethod("getPackageUid", String.class, Integer.TYPE, Integer.TYPE);
                                uid = ((Integer) getPackageUid2.invoke(this.mIPackageManager, packageName, 0, Integer.valueOf(userHandle))).intValue();
                            } catch (Exception e3) {
                                e = e3;
                                VLog.e(VIVO_LOG_TAG, "getNetworkAppTrafficBytes fail! " + e);
                                Binder.restoreCallingIdentity(callingId);
                                trafficBytes2 = trafficBytes;
                                Bundle bundle22 = new Bundle();
                                bundle22.putLong("state_long", trafficBytes2);
                                return bundle22;
                            } catch (Throwable th5) {
                                th = th5;
                                Binder.restoreCallingIdentity(callingId);
                                throw th;
                            }
                        }
                        VLog.d(VIVO_LOG_TAG, "getTrafficBytes packageName:" + packageName + " uid:" + uid);
                        if (this.mStatsService == null) {
                            this.mStatsService = INetworkStatsService.Stub.asInterface(ServiceManager.getService("netstats"));
                        }
                        if (this.mStatsService != null) {
                            INetworkStatsSession mSession = this.mStatsService.openSession();
                            NetworkTemplate networkTemplate = NetworkTemplate.buildTemplateWifi();
                            this.mStatsService.forceUpdate();
                            NetworkStatsHistory networkStatsHistory = mSession.getHistoryForUid(networkTemplate, uid, -1, 0, 10);
                            NetworkStatsHistory.Entry entry = networkStatsHistory.getValues(0L, Long.MAX_VALUE, currentTime, (NetworkStatsHistory.Entry) null);
                            StringBuilder sb = new StringBuilder();
                            sb.append("getTrafficBytes wifi entry.rxBytes:");
                            try {
                                long wifi_bytes_rx2 = entry.rxBytes;
                                sb.append(wifi_bytes_rx2);
                                sb.append(" entry.txBytes:");
                                sb.append(entry.txBytes);
                                VLog.d(VIVO_LOG_TAG, sb.toString());
                            } catch (Exception e4) {
                                e = e4;
                            } catch (Throwable th6) {
                                th = th6;
                            }
                            try {
                                if (direct != 0 && direct != 1) {
                                    wifi_bytes_rx = 0;
                                    if (direct != 0 || direct == 2) {
                                        wifi_bytes_tx = entry.txBytes;
                                    }
                                    subIds = SubscriptionManager.getSubId(0);
                                    if (subIds == null && subIds.length > 0) {
                                        String subscriberId = this.mTelephonyManager.getSubscriberId(subIds[0]);
                                        NetworkTemplate networkTemplate2 = NetworkTemplate.buildTemplateMobileAll(subscriberId);
                                        this.mStatsService.forceUpdate();
                                        NetworkStatsHistory networkStatsHistory2 = mSession.getHistoryForUid(networkTemplate2, uid, -1, 0, 10);
                                        NetworkStatsHistory.Entry entry2 = networkStatsHistory2.getValues(0L, Long.MAX_VALUE, currentTime, (NetworkStatsHistory.Entry) null);
                                        StringBuilder sb2 = new StringBuilder();
                                        sb2.append("getTrafficBytes CustomUtils.SIM1 entry.rxBytes:");
                                        try {
                                            try {
                                                sb2.append(entry2.rxBytes);
                                                sb2.append(" entry.txBytes:");
                                                sb2.append(entry2.txBytes);
                                                VLog.d(VIVO_LOG_TAG, sb2.toString());
                                                if (direct == 0 || direct == 1) {
                                                    sim_data_bytes_rx = 0 + entry2.rxBytes;
                                                }
                                                if (direct != 0) {
                                                    if (direct == 2) {
                                                    }
                                                }
                                                sim_data_bytes_tx = 0 + entry2.txBytes;
                                            } catch (Throwable th7) {
                                                th = th7;
                                                Binder.restoreCallingIdentity(callingId);
                                                throw th;
                                            }
                                        } catch (Exception e5) {
                                            e = e5;
                                            VLog.e(VIVO_LOG_TAG, "getNetworkAppTrafficBytes fail! " + e);
                                            Binder.restoreCallingIdentity(callingId);
                                            trafficBytes2 = trafficBytes;
                                            Bundle bundle222 = new Bundle();
                                            bundle222.putLong("state_long", trafficBytes2);
                                            return bundle222;
                                        }
                                    }
                                    subIds2 = SubscriptionManager.getSubId(1);
                                    if (subIds2 == null && subIds2.length > 0) {
                                        String subscriberId2 = this.mTelephonyManager.getSubscriberId(subIds2[0]);
                                        NetworkTemplate networkTemplate3 = NetworkTemplate.buildTemplateMobileAll(subscriberId2);
                                        this.mStatsService.forceUpdate();
                                        NetworkStatsHistory networkStatsHistory3 = mSession.getHistoryForUid(networkTemplate3, uid, -1, 0, 10);
                                        NetworkStatsHistory.Entry entry3 = networkStatsHistory3.getValues(0L, Long.MAX_VALUE, currentTime, (NetworkStatsHistory.Entry) null);
                                        VLog.d(VIVO_LOG_TAG, "getTrafficBytes CustomUtils.SIM2 entry.rxBytes:" + entry3.rxBytes + " entry.txBytes:" + entry3.txBytes);
                                        if (direct == 0 || direct == 1) {
                                            sim_data_bytes_rx += entry3.rxBytes;
                                        }
                                        if (direct == 0 || direct == 2) {
                                            sim_data_bytes_tx += entry3.txBytes;
                                        }
                                    }
                                }
                                wifi_bytes_tx = entry.txBytes;
                                subIds = SubscriptionManager.getSubId(0);
                                if (subIds == null) {
                                }
                                subIds2 = SubscriptionManager.getSubId(1);
                                if (subIds2 == null) {
                                }
                            } catch (Exception e6) {
                                e = e6;
                                VLog.e(VIVO_LOG_TAG, "getNetworkAppTrafficBytes fail! " + e);
                                Binder.restoreCallingIdentity(callingId);
                                trafficBytes2 = trafficBytes;
                                Bundle bundle2222 = new Bundle();
                                bundle2222.putLong("state_long", trafficBytes2);
                                return bundle2222;
                            } catch (Throwable th8) {
                                th = th8;
                                Binder.restoreCallingIdentity(callingId);
                                throw th;
                            }
                            wifi_bytes_rx = entry.rxBytes;
                            if (direct != 0) {
                            }
                        }
                        if (mode == 0) {
                            long trafficBytes3 = wifi_bytes_rx + wifi_bytes_tx;
                            trafficBytes2 = trafficBytes3 + sim_data_bytes_rx + sim_data_bytes_tx;
                        } else if (mode != 1) {
                            trafficBytes2 = mode != 2 ? trafficBytes : wifi_bytes_rx + wifi_bytes_tx;
                        } else {
                            long trafficBytes4 = sim_data_bytes_rx + sim_data_bytes_tx;
                            trafficBytes2 = trafficBytes4;
                        }
                        Binder.restoreCallingIdentity(callingId);
                    } catch (Exception e7) {
                        e = e7;
                        trafficBytes = -1;
                    } catch (Throwable th9) {
                        th = th9;
                    }
                    Bundle bundle22222 = new Bundle();
                    bundle22222.putLong("state_long", trafficBytes2);
                    return bundle22222;
                } catch (Throwable th10) {
                    th = th10;
                    throw th;
                }
            }
        }
        throw new IllegalArgumentException("IllegalArgumentException: setAdminDeviceAdmin para is illegal!");
    }

    private boolean setOverrideApnPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_NET_APN", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: setOverrideApnPolicy input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                try {
                    this.mService.setOverrideApnsEnabled(admin, policy == 0);
                    return true;
                } catch (Exception e) {
                    VLog.e(VIVO_LOG_TAG, "getNetworkAppTrafficBytes fail! " + e);
                }
            }
            return false;
        }
    }

    private int getOverrideApnPolicy(ComponentName admin, int userHandle) {
        int i;
        synchronized (getLockObject()) {
            i = this.mService.isOverrideApnEnabled(admin) ? 0 : 1;
        }
        return i;
    }

    private Bundle addOverrideApn(ComponentName admin, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_NET_APN", userHandle);
        if (data == null) {
            throw new IllegalArgumentException("IllegalArgumentException: addOverrideApn para is illegal!");
        }
        ApnSetting apnSetting = (ApnSetting) data.getParcelable("apn_info");
        if (apnSetting == null) {
            throw new IllegalArgumentException("IllegalArgumentException: addOverrideApn para is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                Bundle bundle = new Bundle();
                int apnId = this.mService.addOverrideApn(admin, apnSetting);
                VLog.v(VIVO_LOG_TAG, "addOverrideApn apnId:" + apnId);
                bundle.putInt("state_int", apnId);
                return bundle;
            }
            return null;
        }
    }

    private boolean updateOverrideApn(ComponentName admin, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_NET_APN", userHandle);
        if (data == null) {
            throw new IllegalArgumentException("IllegalArgumentException: updateOverrideApn para is illegal!");
        }
        int apnId = data.getInt("state_int");
        ApnSetting apnSetting = (ApnSetting) data.getParcelable("apn_info");
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                return this.mService.updateOverrideApn(admin, apnId, apnSetting);
            }
            return false;
        }
    }

    private boolean removeOverrideApn(ComponentName admin, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_NET_APN", userHandle);
        if (data == null) {
            throw new IllegalArgumentException("IllegalArgumentException: removeOverrideApn para is illegal!");
        }
        int apnId = data.getInt("state_int");
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                return this.mService.removeOverrideApn(admin, apnId);
            }
            return false;
        }
    }

    private Bundle getOverrideApns(ComponentName admin, int userHandle) {
        synchronized (getLockObject()) {
            Bundle bundle = new Bundle();
            List<ApnSetting> apnList = this.mService.getOverrideApns(admin);
            if (apnList != null) {
                bundle.putParcelableArrayList("apn_info", (ArrayList) apnList);
                return bundle;
            }
            return null;
        }
    }

    private boolean setVpnPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_NET_VPN", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: setVpnPolicy input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                this.mService.setUserRestriction(admin, "no_config_vpn", policy == 1, false);
                return true;
            }
            return false;
        }
    }

    private int getVpnPolicy(ComponentName admin, int userHandle) {
        synchronized (getLockObject()) {
            Bundle restrictions = this.mService.getUserRestrictions(admin, false);
            if (restrictions == null) {
                return 0;
            }
            boolean isRestrict = restrictions.getBoolean("no_config_vpn", false);
            return isRestrict ? 1 : 0;
        }
    }

    private boolean setAlwaysOnVpnPackage(ComponentName admin, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_NET_VPN", userHandle);
        if (data == null) {
            throw new IllegalArgumentException("IllegalArgumentException: setAlwaysOnVpnPackage para is illegal!");
        }
        String vpnPackage = data.getString("package_name");
        boolean lockdown = data.getBoolean("state_boolean");
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                return this.mService.setAlwaysOnVpnPackage(admin, vpnPackage, lockdown, (List) null);
            }
            return false;
        }
    }

    private Bundle getAlwaysOnVpnPackage(ComponentName admin, int userHandle) {
        Bundle bundle;
        synchronized (getLockObject()) {
            bundle = new Bundle();
            String vpnPackage = this.mService.getAlwaysOnVpnPackage(admin);
            VLog.v(VIVO_LOG_TAG, "getAlwaysOnVpnPackage vpnPackage:" + vpnPackage);
            bundle.putString("package_name", vpnPackage);
        }
        return bundle;
    }

    private boolean setDataRoamingPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_NETWORK", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: setDataRoamingPolicy input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                this.mService.setUserRestriction(admin, "no_data_roaming", policy == 1, false);
                return true;
            }
            return false;
        }
    }

    private int getDataRoamingPolicy(ComponentName admin, int userHandle) {
        synchronized (getLockObject()) {
            Bundle restrictions = this.mService.getUserRestrictions(admin, false);
            if (restrictions == null) {
                return 0;
            }
            boolean isRestrict = restrictions.getBoolean("no_data_roaming", false);
            return isRestrict ? 1 : 0;
        }
    }

    private boolean setTetheringPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_NETWORK", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: setTetheringPolicy input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                this.mService.setUserRestriction(admin, "no_config_tethering", policy == 1, false);
                return true;
            }
            return false;
        }
    }

    private int getTetheringPolicy(ComponentName admin, int userHandle) {
        synchronized (getLockObject()) {
            Bundle restrictions = this.mService.getUserRestrictions(admin, false);
            if (restrictions == null) {
                return 0;
            }
            boolean isRestrict = restrictions.getBoolean("no_config_tethering", false);
            return isRestrict ? 1 : 0;
        }
    }

    private boolean setConfigNetworkPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_NETWORK", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: setConfigNetworkPolicy input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                this.mService.setUserRestriction(admin, "no_config_mobile_networks", policy == 1, false);
                return true;
            }
            return false;
        }
    }

    private int getConfigNetworkPolicy(ComponentName admin, int userHandle) {
        synchronized (getLockObject()) {
            Bundle restrictions = this.mService.getUserRestrictions(admin, false);
            if (restrictions == null) {
                return 0;
            }
            boolean isRestrict = restrictions.getBoolean("no_config_mobile_networks", false);
            return isRestrict ? 1 : 0;
        }
    }

    private boolean setNetworkMobileDataSlotPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_NETWORK", userHandle);
        if (policy < 0 || policy > 2) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.networkMobileDataSlotPolicy != policy) {
                    ap.networkMobileDataSlotPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getNetworkMobileDataSlotPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.networkMobileDataSlotPolicy : 0;
        }
        return i;
    }

    private boolean setPeripheralWlanDirectPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_PERI_WLAN", userHandle);
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.peripheralWlanDirectPolicy != policy) {
                    ap.peripheralWlanDirectPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getPeripheralWlanDirectPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.peripheralWlanDirectPolicy : 0;
        }
        return i;
    }

    private boolean setPeripheralWlanScanAlwaysPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_PERI_WLAN", userHandle);
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.peripheralWlanScanAlwaysPolicy != policy) {
                    ap.peripheralWlanScanAlwaysPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getPeripheralWlanScanAlwaysPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.peripheralWlanScanAlwaysPolicy : 0;
        }
        return i;
    }

    private boolean setApnDisableRecoveryList(ComponentName admin, int listId, List<String> keys, boolean isAdd, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_NET_APN", userHandle);
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        boolean success = false;
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.apnDisableRecoveryList == null) {
                    ap.apnDisableRecoveryList = new ArrayList();
                }
                success = VivoUtils.addItemsFromList(ap.apnDisableRecoveryList, keys, isAdd);
                saveVivoSettingsLocked(listId, userHandle);
            }
        }
        return success;
    }

    private List<String> getApnDisableRecoveryList(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            if (ap != null) {
                return ap.apnDisableRecoveryList;
            }
            return null;
        }
    }

    private boolean setOperationHomeKeyPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPER_KEY_EVENT", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.operationHomeKeyPolicy != policy) {
                    long callingId = Binder.clearCallingIdentity();
                    try {
                        int newFlag = policy == 1 ? ap.operationStatusBarDiablePolicy | 2097152 : ap.operationStatusBarDiablePolicy & (-2097153);
                        if (this.mStatusBarService == null) {
                            this.mStatusBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
                        }
                        this.mStatusBarService.disableForUser(newFlag, this.mStatusbarToken, ap.mInfo.getPackageName(), userHandle);
                        ap.operationHomeKeyPolicy = policy;
                        ap.operationStatusBarDiablePolicy = newFlag;
                        saveVivoSettingsLocked(polId, userHandle);
                        Binder.restoreCallingIdentity(callingId);
                    } catch (Exception e) {
                        VLog.e(VIVO_LOG_TAG, "setOperationHomeKeyPolicy fail! " + e);
                        Binder.restoreCallingIdentity(callingId);
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
    }

    private int getOperationHomeKeyPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.operationHomeKeyPolicy : 0;
        }
        return i;
    }

    private boolean setOperationMenuKeyPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPER_KEY_EVENT", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.operationMenuKeyPolicy != policy) {
                    long callingId = Binder.clearCallingIdentity();
                    try {
                        int newFlag = policy == 1 ? ap.operationStatusBarDiablePolicy | Dataspace.TRANSFER_GAMMA2_2 : ap.operationStatusBarDiablePolicy & (-16777217);
                        int newFlag2 = policy == 1 ? ap.operationStatusBarDiable2Policy | 1 : ap.operationStatusBarDiable2Policy & (-2);
                        if (this.mStatusBarService == null) {
                            this.mStatusBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
                        }
                        this.mStatusBarService.disableForUser(newFlag, this.mStatusbarToken, ap.mInfo.getPackageName(), userHandle);
                        this.mStatusBarService.disable2ForUser(newFlag2, this.mStatusbarToken, ap.mInfo.getPackageName(), userHandle);
                        ap.operationMenuKeyPolicy = policy;
                        ap.operationStatusBarDiablePolicy = newFlag;
                        ap.operationStatusBarDiable2Policy = newFlag2;
                        saveVivoSettingsLocked(polId, userHandle);
                        Binder.restoreCallingIdentity(callingId);
                    } catch (Exception e) {
                        VLog.e(VIVO_LOG_TAG, "setOperationMenuKeyPolicy fail! " + e);
                        Binder.restoreCallingIdentity(callingId);
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
    }

    private int getOperationMenuKeyPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.operationMenuKeyPolicy : 0;
        }
        return i;
    }

    private boolean setOperationBackKeyPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPER_KEY_EVENT", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.operationBackKeyPolicy != policy) {
                    long callingId = Binder.clearCallingIdentity();
                    try {
                        int newFlag = policy == 1 ? ap.operationStatusBarDiablePolicy | Dataspace.TRANSFER_LINEAR : ap.operationStatusBarDiablePolicy & (-4194305);
                        if (this.mStatusBarService == null) {
                            this.mStatusBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
                        }
                        this.mStatusBarService.disableForUser(newFlag, this.mStatusbarToken, ap.mInfo.getPackageName(), userHandle);
                        ap.operationBackKeyPolicy = policy;
                        ap.operationStatusBarDiablePolicy = newFlag;
                        saveVivoSettingsLocked(polId, userHandle);
                        Binder.restoreCallingIdentity(callingId);
                    } catch (Exception e) {
                        VLog.e(VIVO_LOG_TAG, "setOperationBackKeyPolicy fail! " + e);
                        Binder.restoreCallingIdentity(callingId);
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
    }

    private int getOperationBackKeyPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.operationBackKeyPolicy : 0;
        }
        return i;
    }

    private boolean setOperationRecentTaskKeyPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPER_KEY_EVENT", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.operationRecentTaskKeyPolicy != policy) {
                    long callingId = Binder.clearCallingIdentity();
                    try {
                        int newFlag = policy == 1 ? ap.operationStatusBarDiablePolicy | Dataspace.TRANSFER_GAMMA2_2 : ap.operationStatusBarDiablePolicy & (-16777217);
                        int newFlag2 = policy == 1 ? ap.operationStatusBarDiable2Policy | 1 : ap.operationStatusBarDiable2Policy & (-2);
                        if (this.mStatusBarService == null) {
                            this.mStatusBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
                        }
                        this.mStatusBarService.disableForUser(newFlag, this.mStatusbarToken, ap.mInfo.getPackageName(), userHandle);
                        this.mStatusBarService.disable2ForUser(newFlag2, this.mStatusbarToken, ap.mInfo.getPackageName(), userHandle);
                        ap.operationRecentTaskKeyPolicy = policy;
                        ap.operationStatusBarDiablePolicy = newFlag;
                        ap.operationStatusBarDiable2Policy = newFlag2;
                        saveVivoSettingsLocked(polId, userHandle);
                        Binder.restoreCallingIdentity(callingId);
                    } catch (Exception e) {
                        VLog.e(VIVO_LOG_TAG, "setOperationRecentTaskKeyPolicy fail! " + e);
                        Binder.restoreCallingIdentity(callingId);
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
    }

    private int getOperationRecentTaskKeyPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.operationRecentTaskKeyPolicy : 0;
        }
        return i;
    }

    private boolean setOperationPowerPanelKeyPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPER_POWER_EVENT", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.operationPowerPanelKeyPolicy != policy) {
                    long callingId = Binder.clearCallingIdentity();
                    try {
                        int newFlag = policy == 1 ? ap.operationStatusBarDiable2Policy | 8 : ap.operationStatusBarDiable2Policy & (-9);
                        if (this.mStatusBarService == null) {
                            this.mStatusBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
                        }
                        this.mStatusBarService.disable2ForUser(newFlag, this.mStatusbarToken, ap.mInfo.getPackageName(), userHandle);
                        ap.operationPowerPanelKeyPolicy = policy;
                        ap.operationStatusBarDiable2Policy = newFlag;
                        saveVivoSettingsLocked(polId, userHandle);
                        Binder.restoreCallingIdentity(callingId);
                    } catch (Exception e) {
                        VLog.e(VIVO_LOG_TAG, "setOperationPowerPanelKeyPolicy fail! " + e);
                        Binder.restoreCallingIdentity(callingId);
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
    }

    private int getOperationPowerPanelKeyPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.operationPowerPanelKeyPolicy : 0;
        }
        return i;
    }

    private boolean setOperationClipboardPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPER_INPUT", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.operationClipboardPolicy != policy) {
                    ap.operationClipboardPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getOperationClipboardPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.operationClipboardPolicy : 0;
        }
        return i;
    }

    private boolean setOperationBackupPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPER_BACKUP_RESET", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.operationBackupPolicy != policy) {
                    ap.operationBackupPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getOperationBackupPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.operationBackupPolicy : 0;
        }
        return i;
    }

    private boolean shutDown(ComponentName admin, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPERATION", userHandle);
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            long callingId = Binder.clearCallingIdentity();
            if (ap != null) {
                ((PowerManager) this.mContext.getSystemService(PowerManager.class)).shutdown(false, null, false);
                Binder.restoreCallingIdentity(callingId);
                return true;
            }
            Binder.restoreCallingIdentity(callingId);
            return false;
        }
    }

    private boolean reboot(ComponentName admin, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPERATION", userHandle);
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                this.mService.reboot(admin);
                return true;
            }
            return false;
        }
    }

    private Bundle captureScreen(ComponentName admin, int userHandle) {
        Bundle mBundle;
        checkCallingEmmPermission(admin, "EMM_OPER_CAPTRUE", userHandle);
        synchronized (getLockObject()) {
            mBundle = new Bundle();
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                long callingId = Binder.clearCallingIdentity();
                WindowManager windowManager = (WindowManager) this.mContext.getSystemService("window");
                Display display = windowManager.getDefaultDisplay();
                DisplayMetrics displayMetrics = new DisplayMetrics();
                display.getRealMetrics(displayMetrics);
                Rect crop = new Rect(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);
                Bitmap screenBitmap = SurfaceControl.screenshot(crop, crop.width(), crop.height(), display.getRotation());
                VLog.v(VIVO_LOG_TAG, "bitmap's size is:" + screenBitmap.getAllocationByteCount());
                mBundle.putParcelable("screen_capture_bitmap", screenBitmap);
                Binder.restoreCallingIdentity(callingId);
            }
        }
        return mBundle;
    }

    private boolean setOperationStatusbarPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPER_KEY_EVENT", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.operationStatusbarPolicy != policy) {
                    long callingId = Binder.clearCallingIdentity();
                    try {
                        int newFlag = policy == 1 ? ap.operationStatusBarDiablePolicy | VIVO_STATUS_BAR_DISABLE_MASK : ap.operationStatusBarDiablePolicy & (-34013185);
                        int newFlag2 = policy == 1 ? ap.operationStatusBarDiable2Policy | 1 : ap.operationStatusBarDiable2Policy & (-2);
                        if (this.mStatusBarService == null) {
                            this.mStatusBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
                        }
                        this.mStatusBarService.disableForUser(newFlag, this.mStatusbarToken, ap.mInfo.getPackageName(), userHandle);
                        this.mStatusBarService.disable2ForUser(newFlag2, this.mStatusbarToken, ap.mInfo.getPackageName(), userHandle);
                        ap.operationStatusbarPolicy = policy;
                        ap.operationStatusBarDiablePolicy = newFlag;
                        ap.operationStatusBarDiable2Policy = newFlag2;
                        saveVivoSettingsLocked(polId, userHandle);
                        Binder.restoreCallingIdentity(callingId);
                    } catch (Exception e) {
                        VLog.e(VIVO_LOG_TAG, "setOperationStatusbarPolicy fail! " + e);
                        Binder.restoreCallingIdentity(callingId);
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
    }

    private int getOperationStatusbarPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.operationStatusbarPolicy : 0;
        }
        return i;
    }

    private boolean setOperationPowerSavingPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPERATION", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.operationPowerSavingPolicy != policy) {
                    ap.operationPowerSavingPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getOperationPowerSavingPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.operationPowerSavingPolicy : 0;
        }
        return i;
    }

    private boolean setOperationHardFactoryResetPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPER_HARD_RESET", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.operationHardFactoryResetPolicy != policy) {
                    ap.operationHardFactoryResetPolicy = policy;
                    SystemProperties.set("persist.vivo.emm.hard_set", Integer.toString(policy));
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getOperationHardFactoryResetPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.operationHardFactoryResetPolicy : 0;
        }
        return i;
    }

    private String encryptReset(String source) {
        int[] arrInt = new int[100];
        char[] cArr = new char[100];
        char[] byteChars = source.toCharArray();
        for (int i = 0; i < source.length(); i++) {
            char c = byteChars[i];
            arrInt[i] = c + i + 1;
            byteChars[i] = (char) arrInt[i];
            VLog.i(VIVO_LOG_TAG, "encryptReset, i=" + i + ",byteChars[i]=" + byteChars[i] + ",a=" + ((int) c) + ",arrInt[i]=" + arrInt[i]);
        }
        String str1 = new String(byteChars);
        VLog.i(VIVO_LOG_TAG, "encryptReset, source = " + source + ", str1 = " + str1);
        return str1;
    }

    private String decryptReset(String source) {
        int[] arrInt = new int[100];
        char[] cArr = new char[100];
        char[] byteChars = source.toCharArray();
        for (int i = 0; i < source.length(); i++) {
            char c = byteChars[i];
            arrInt[i] = (c - i) - 1;
            byteChars[i] = (char) arrInt[i];
            VLog.i(VIVO_LOG_TAG, "decryptReset, i=" + i + ",byteChars[i]=" + byteChars[i] + ",a=" + ((int) c) + ",arrInt[i]=" + arrInt[i]);
        }
        String str1 = new String(byteChars);
        VLog.i(VIVO_LOG_TAG, "decryptReset, source = " + source + ", str1 = " + str1);
        return str1;
    }

    private boolean setOperationHardResetPassword(ComponentName admin, int transId, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPER_BACKUP_RESET", userHandle);
        if (data != null) {
            String pass = data.getString("state_string");
            synchronized (getLockObject()) {
                long callingId = Binder.clearCallingIdentity();
                try {
                    String encryptStr = "0".equals(pass) ? "0" : encryptReset(pass);
                    SystemProperties.set("persist.vivo.emm.hard_reset_code", encryptStr);
                    Binder.restoreCallingIdentity(callingId);
                } catch (Exception e) {
                    VLog.e(VIVO_LOG_TAG, "setOperationHardResetPassword fail! " + e);
                    Binder.restoreCallingIdentity(callingId);
                }
            }
            return true;
        }
        throw new IllegalArgumentException("IllegalArgumentException: setOperationHardResetPassword para is illegal!");
    }

    private Bundle getOperationHardResetPassword(ComponentName admin, int transId, int userHandle) {
        Bundle bundle;
        synchronized (getLockObject()) {
            bundle = new Bundle();
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                long callingId = Binder.clearCallingIdentity();
                try {
                    String pass = SystemProperties.get("persist.vivo.emm.hard_reset_code", "0");
                    String decryptStr = "0";
                    if (!"0".equals(pass)) {
                        decryptStr = decryptReset(pass);
                    }
                    bundle.putString("state_string", decryptStr);
                    Binder.restoreCallingIdentity(callingId);
                } catch (Exception e) {
                    VLog.e(VIVO_LOG_TAG, "getVpnProfile fail! " + e);
                    Binder.restoreCallingIdentity(callingId);
                }
            }
        }
        return bundle;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public VolumeInfo getExternalVolume() {
        if (!isSupportTF) {
            return null;
        }
        if (this.mStorageManager == null) {
            StorageManager from = StorageManager.from(this.mContext);
            this.mStorageManager = from;
            from.registerListener(this.mStorageListener);
        }
        List<VolumeInfo> volumes = this.mStorageManager.getVolumes();
        VolumeInfo exVolume = null;
        for (VolumeInfo vol : volumes) {
            if (vol != null && vol.getDisk() != null && vol.getDisk().isSd()) {
                exVolume = vol;
            }
        }
        if (exVolume != null) {
            VLog.d(VIVO_LOG_TAG, "getExternalVolume , path = " + exVolume.path);
        }
        return exVolume;
    }

    /* JADX WARN: Type inference failed for: r6v3, types: [com.android.server.devicepolicy.VivoCustomDpmsImpl$6] */
    private boolean operationFormatSDCard(ComponentName admin, int transId, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPER_BACKUP_RESET", userHandle);
        synchronized (getLockObject()) {
            if (!isSupportTF) {
                VLog.d(VIVO_LOG_TAG, "formatSDCard fail for device is not support TF card");
                return false;
            }
            long callingId = Binder.clearCallingIdentity();
            VLog.d(VIVO_LOG_TAG, "formatSDCard from callingId = " + callingId);
            try {
                VolumeInfo exVolume = getExternalVolume();
                if (exVolume == null) {
                    VLog.w(VIVO_LOG_TAG, "formatSDCard failed because the sdcard is not exist!");
                    Binder.restoreCallingIdentity(callingId);
                    return false;
                }
                int state = exVolume.state;
                if (2 == state || state == 0 || 6 == state || 1 == state) {
                    new Thread() { // from class: com.android.server.devicepolicy.VivoCustomDpmsImpl.6
                        @Override // java.lang.Thread, java.lang.Runnable
                        public void run() {
                            try {
                                VolumeInfo exVolume2 = VivoCustomDpmsImpl.this.getExternalVolume();
                                if (exVolume2 != null) {
                                    VivoCustomDpmsImpl.this.iStorageManager.unmount(exVolume2.id);
                                    VivoCustomDpmsImpl.this.mIsDoingUnmount = true;
                                    VLog.d(VivoCustomDpmsImpl.VIVO_LOG_TAG, "formatSDCard unmountVolume end!");
                                    return;
                                }
                                VLog.w(VivoCustomDpmsImpl.VIVO_LOG_TAG, "run formatSDCard failed because the sdcard is not exist!");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();
                    VLog.d(VIVO_LOG_TAG, "formatSDCard end");
                    Binder.restoreCallingIdentity(callingId);
                    return true;
                }
                VLog.w(VIVO_LOG_TAG, "formatSDCard failed because the sdcard's state is " + state);
                Binder.restoreCallingIdentity(callingId);
                return false;
            } catch (Exception e) {
                VLog.e(VIVO_LOG_TAG, "Failed to get state from storage manager", e);
                Binder.restoreCallingIdentity(callingId);
                return false;
            }
        }
    }

    private boolean setSafeModePolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPER_KEY_EVENT", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: setSafeModePolicy input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                this.mService.setUserRestriction(admin, "no_safe_boot", policy == 1, false);
                return true;
            }
            return false;
        }
    }

    private int getSafeModePolicy(ComponentName admin, int userHandle) {
        synchronized (getLockObject()) {
            Bundle restrictions = this.mService.getUserRestrictions(admin, false);
            if (restrictions == null) {
                return 0;
            }
            boolean isRestrict = restrictions.getBoolean("no_safe_boot", false);
            return isRestrict ? 1 : 0;
        }
    }

    private boolean setVolumePolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPER_KEY_EVENT", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: setVolumePolicy input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                this.mService.setUserRestriction(admin, "no_adjust_volume", policy == 1, false);
                return true;
            }
            return false;
        }
    }

    private int getVolumePolicy(ComponentName admin, int userHandle) {
        synchronized (getLockObject()) {
            Bundle restrictions = this.mService.getUserRestrictions(admin, false);
            if (restrictions == null) {
                return 0;
            }
            boolean isRestrict = restrictions.getBoolean("no_adjust_volume", false);
            return isRestrict ? 1 : 0;
        }
    }

    private boolean setScreenCapturePolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPER_CAPTRUE", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: setScreenCapturePolicy input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                this.mService.setScreenCaptureDisabled(admin, policy == 1, false);
                return true;
            }
            return false;
        }
    }

    private int getScreenCapturePolicy(ComponentName admin, int userHandle) {
        int i;
        synchronized (getLockObject()) {
            boolean result = this.mService.getScreenCaptureDisabled(admin, userHandle, false);
            i = result ? 1 : 0;
        }
        return i;
    }

    private boolean setLocalePolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPER_LOCALE", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: setLocalePolicy input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                this.mService.setUserRestriction(admin, "no_config_locale", policy == 1, false);
                return true;
            }
            return false;
        }
    }

    private int getLocalePolicy(ComponentName admin, int userHandle) {
        synchronized (getLockObject()) {
            Bundle restrictions = this.mService.getUserRestrictions(admin, false);
            if (restrictions == null) {
                return 0;
            }
            boolean isRestrict = restrictions.getBoolean("no_config_locale", false);
            return isRestrict ? 1 : 0;
        }
    }

    private boolean setAccountModifyPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPER_ACCOUNT", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: setAccountModifyPolicy input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                this.mService.setUserRestriction(admin, "no_modify_accounts", policy == 1, false);
                return true;
            }
            return false;
        }
    }

    private int getAccountModifyPolicy(ComponentName admin, int userHandle) {
        synchronized (getLockObject()) {
            Bundle restrictions = this.mService.getUserRestrictions(admin, false);
            if (restrictions == null) {
                return 0;
            }
            boolean isRestrict = restrictions.getBoolean("no_modify_accounts", false);
            return isRestrict ? 1 : 0;
        }
    }

    private boolean setDataTimePolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPER_DATA_TIME", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: setDataTimePolicy input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                this.mService.setUserRestriction(admin, "no_config_date_time", policy == 1, false);
                return true;
            }
            return false;
        }
    }

    private int getDataTimePolicy(ComponentName admin, int userHandle) {
        synchronized (getLockObject()) {
            Bundle restrictions = this.mService.getUserRestrictions(admin, false);
            if (restrictions == null) {
                return 0;
            }
            boolean isRestrict = restrictions.getBoolean("no_config_date_time", false);
            return isRestrict ? 1 : 0;
        }
    }

    private boolean setAutoTimeOffPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPER_DATA_TIME", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: setAutoTimeOffPolicy input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap == null) {
                return false;
            }
            this.mService.setAutoTimeRequired(admin, policy == 1);
            return true;
        }
    }

    private int getAutoTimeOffPolicy(ComponentName admin, int userHandle) {
        int i;
        synchronized (getLockObject()) {
            boolean result = this.mService.getAutoTimeRequired();
            i = result ? 1 : 0;
        }
        return i;
    }

    private boolean wipeData(ComponentName admin, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPER_BACKUP_RESET", userHandle);
        if (data == null) {
            throw new IllegalArgumentException("IllegalArgumentException: wipeData para is illegal!");
        }
        int flags = data.getInt("state_int");
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                this.mService.wipeDataWithReason(flags, "vivo_enterprise", false);
                return true;
            }
            return false;
        }
    }

    private boolean setFactoryResetPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPER_BACKUP_RESET", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: setFactoryResetPolicy input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                this.mService.setUserRestriction(admin, "no_factory_reset", policy == 1, false);
                return true;
            }
            return false;
        }
    }

    private int getFactoryResetPolicy(ComponentName admin, int userHandle) {
        synchronized (getLockObject()) {
            Bundle restrictions = this.mService.getUserRestrictions(admin, false);
            if (restrictions == null) {
                return 0;
            }
            boolean isRestrict = restrictions.getBoolean("no_factory_reset", false);
            return isRestrict ? 1 : 0;
        }
    }

    private boolean setFunGamePolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPERATION", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: setFunGamePolicy input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                this.mService.setUserRestriction(admin, "no_fun", policy == 1, false);
                return true;
            }
            return false;
        }
    }

    private int getFunGamePolicy(ComponentName admin, int userHandle) {
        synchronized (getLockObject()) {
            Bundle restrictions = this.mService.getUserRestrictions(admin, false);
            if (restrictions == null) {
                return 0;
            }
            boolean isRestrict = restrictions.getBoolean("no_fun", false);
            return isRestrict ? 1 : 0;
        }
    }

    private boolean setBrightnessPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPER_DISPALY_BRIGHTNESS", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: setBrightnessPolicy input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                this.mService.setUserRestriction(admin, "no_config_brightness", policy == 1, false);
                return true;
            }
            return false;
        }
    }

    private int getBrightnessPolicy(ComponentName admin, int userHandle) {
        synchronized (getLockObject()) {
            Bundle restrictions = this.mService.getUserRestrictions(admin, false);
            if (restrictions == null) {
                return 0;
            }
            boolean isRestrict = restrictions.getBoolean("no_config_brightness", false);
            return isRestrict ? 1 : 0;
        }
    }

    private boolean setScreenTimeoutPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPER_DISPALY_BRIGHTNESS", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: setScreenTimeoutPolicy input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                this.mService.setUserRestriction(admin, "no_config_screen_timeout", policy == 1, false);
                return true;
            }
            return false;
        }
    }

    private int getScreenTimeoutPolicy(ComponentName admin, int userHandle) {
        synchronized (getLockObject()) {
            Bundle restrictions = this.mService.getUserRestrictions(admin, false);
            if (restrictions == null) {
                return 0;
            }
            boolean isRestrict = restrictions.getBoolean("no_config_screen_timeout", false);
            return isRestrict ? 1 : 0;
        }
    }

    private boolean setConfigWallpaperPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPERATION", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: setConfigWallpaperPolicy input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                this.mService.setUserRestriction(admin, "no_set_wallpaper", policy == 1, false);
                sendChangedNotification(polId, userHandle);
                return true;
            }
            return false;
        }
    }

    private int getConfigWallpaperPolicy(ComponentName admin, int userHandle) {
        synchronized (getLockObject()) {
            int i = 1;
            if (admin == null) {
                admin = getVivoAdminUncheckedLocked(userHandle);
                if (admin == null) {
                    return 0;
                }
                this.mIsEmmAPI = true;
            }
            Bundle restrictions = this.mService.getUserRestrictions(admin, false);
            this.mIsEmmAPI = false;
            if (restrictions == null) {
                return 0;
            }
            boolean isRestrict = restrictions.getBoolean("no_set_wallpaper", false);
            if (!isRestrict) {
                i = 0;
            }
            return i;
        }
    }

    private boolean setVolumeLongPressPolicy(ComponentName admin, int transId, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPER_KEY_EVENT", userHandle);
        if (data == null) {
            throw new IllegalArgumentException("IllegalArgumentException: setVolumeLongPressPolicy para is illegal!");
        }
        String packageName = data.getString("package_name");
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                ap.operationVolumeLongPressPolicy = packageName;
                saveVivoSettingsLocked(transId, userHandle);
                return true;
            }
            return false;
        }
    }

    private Bundle getVolumeLongPressPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        synchronized (getLockObject()) {
            Bundle bundle = new Bundle();
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            if (ap != null) {
                bundle.putString("package_name", ap.operationVolumeLongPressPolicy);
                VLog.v(VIVO_LOG_TAG, "getVolumeLongPressPolicy package:" + ap.operationVolumeLongPressPolicy);
                return bundle;
            }
            return null;
        }
    }

    private boolean setMockLocationPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        char c;
        checkCallingEmmPermission(admin, "EMM_OPERATION", userHandle);
        synchronized (getLockObject()) {
            try {
            } catch (Throwable th) {
                th = th;
            }
            try {
                VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
                int i = 0;
                if (ap != null) {
                    long callingId = Binder.clearCallingIdentity();
                    try {
                        if (policy == 1) {
                            try {
                                char c2 = ':';
                                List<AppOpsManager.PackageOps> packageOps = this.mAppOpsManager.getPackagesForOps(new int[]{58});
                                if (packageOps != null && packageOps.size() > 0) {
                                    for (AppOpsManager.PackageOps packageOp : packageOps) {
                                        if (((AppOpsManager.OpEntry) packageOp.getOps().get(i)).getMode() != 2) {
                                            ApplicationInfo ai = this.mContext.getPackageManager().getApplicationInfo(packageOp.getPackageName(), 512);
                                            c = ':';
                                            this.mAppOpsManager.setMode(58, ai.uid, packageOp.getPackageName(), 2);
                                        } else {
                                            c = c2;
                                        }
                                        c2 = c;
                                        i = 0;
                                    }
                                }
                            } catch (Exception e) {
                                e = e;
                                VLog.e(VIVO_LOG_TAG, "setMockLocationPolicy fail! " + e.getMessage());
                                Binder.restoreCallingIdentity(callingId);
                                return false;
                            } catch (Throwable th2) {
                                th = th2;
                                Binder.restoreCallingIdentity(callingId);
                                throw th;
                            }
                        }
                        if (ap.operationMockLocationPolicy != policy) {
                            ap.operationMockLocationPolicy = policy;
                            try {
                                saveVivoSettingsLocked(polId, userHandle);
                            } catch (Exception e2) {
                                e = e2;
                                VLog.e(VIVO_LOG_TAG, "setMockLocationPolicy fail! " + e.getMessage());
                                Binder.restoreCallingIdentity(callingId);
                                return false;
                            }
                        }
                        Binder.restoreCallingIdentity(callingId);
                        return true;
                    } catch (Throwable th3) {
                        th = th3;
                    }
                }
                return false;
            } catch (Throwable th4) {
                th = th4;
                throw th;
            }
        }
    }

    private int getMockLocationPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.operationMockLocationPolicy : 0;
        }
        return i;
    }

    private boolean setOperationSysUpgradePolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_UPGRADE", userHandle);
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.operationSysUpgradePolicy != policy) {
                    ap.operationSysUpgradePolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getOperationSysUpgradePolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.operationSysUpgradePolicy : 0;
        }
        return i;
    }

    private boolean setOperationAiKeyPolicy(ComponentName admin, int polId, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPER_KEY_EVENT", userHandle);
        if (data == null) {
            throw new IllegalArgumentException("IllegalArgumentException: setOperationAiKeyPolicy para is illegal!");
        }
        String packageName = data.getString("package_name");
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                ap.operationAiKeyPolicy = packageName;
                saveVivoSettingsLocked(polId, userHandle);
                return true;
            }
            return false;
        }
    }

    private Bundle getOperationAiKeyPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        synchronized (getLockObject()) {
            Bundle bundle = new Bundle();
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            if (ap != null) {
                bundle.putString("package_name", ap.operationAiKeyPolicy);
                VLog.v(VIVO_LOG_TAG, "getOperationAiKeyPolicy operationAiKeyPolicy:" + ap.operationAiKeyPolicy);
                return bundle;
            }
            return null;
        }
    }

    private boolean setEyeProtectionPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPERATION", userHandle);
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            long callingId = Binder.clearCallingIdentity();
            if (ap != null) {
                try {
                    ColorDisplayManager mColorDisplayManager = (ColorDisplayManager) this.mContext.getSystemService(ColorDisplayManager.class);
                    if (policy == 5) {
                        boolean nightDisplayActivated = mColorDisplayManager.setNightDisplayActivated(true);
                        Binder.restoreCallingIdentity(callingId);
                        return nightDisplayActivated;
                    } else if (policy == 6) {
                        boolean nightDisplayActivated2 = mColorDisplayManager.setNightDisplayActivated(false);
                        Binder.restoreCallingIdentity(callingId);
                        return nightDisplayActivated2;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Binder.restoreCallingIdentity(callingId);
                }
            }
            Binder.restoreCallingIdentity(callingId);
            return false;
        }
    }

    private int getEyeProtectionPolicy(ComponentName admin, int userHandle) {
        int isDisplay;
        synchronized (getLockObject()) {
            isDisplay = 6;
            try {
                ColorDisplayManager mColorDisplayManager = (ColorDisplayManager) this.mContext.getSystemService(ColorDisplayManager.class);
                if (mColorDisplayManager.isNightDisplayActivated()) {
                    isDisplay = 5;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return isDisplay;
    }

    private boolean setSysTime(ComponentName admin, int polId, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPERATION", userHandle);
        if (data == null) {
            throw new IllegalArgumentException("IllegalArgumentException: setOperationAiKeyPolicy para is illegal!");
        }
        boolean success = false;
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (this.mService.getAutoTimeRequired()) {
                    return false;
                }
                this.mService.setAutoTimeEnabled(admin, false);
                this.mService.setAutoTimeZoneEnabled(admin, false);
                long millis = data.getLong("state_long");
                String timeZone = data.getString("state_string");
                if (!TextUtils.isEmpty(timeZone)) {
                    if (!this.mService.setTimeZone(admin, timeZone)) {
                        return false;
                    }
                    success = true;
                }
                if (millis != 0) {
                    if (!this.mService.setTime(admin, millis)) {
                        return false;
                    }
                    success = true;
                }
            }
            return success;
        }
    }

    private boolean setDesktopWallpaper(ComponentName admin, int polId, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPERATION", userHandle);
        if (data == null) {
            throw new IllegalArgumentException("IllegalArgumentException: setOperationAiKeyPolicy para is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                long callingId = Binder.clearCallingIdentity();
                try {
                    Bitmap bitmap = (Bitmap) data.getParcelable("screen_capture_bitmap");
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                    InputStream is = new ByteArrayInputStream(baos.toByteArray());
                    WallpaperManager.getInstance(this.mContext).setStream(is, null, true, 1);
                    Binder.restoreCallingIdentity(callingId);
                } catch (IOException e) {
                    e.printStackTrace();
                    Binder.restoreCallingIdentity(callingId);
                }
                return true;
            }
            return false;
        }
    }

    private boolean setLockWallpaper(ComponentName admin, int polId, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPERATION", userHandle);
        if (data == null) {
            throw new IllegalArgumentException("IllegalArgumentException: setOperationAiKeyPolicy para is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                long callingId = Binder.clearCallingIdentity();
                try {
                    Bitmap bitmap = (Bitmap) data.getParcelable("screen_capture_bitmap");
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                    InputStream is = new ByteArrayInputStream(baos.toByteArray());
                    WallpaperManager.getInstance(this.mContext).setStream(is, null, true, 2);
                    Binder.restoreCallingIdentity(callingId);
                } catch (IOException e) {
                    e.printStackTrace();
                    Binder.restoreCallingIdentity(callingId);
                }
                return true;
            }
            return false;
        }
    }

    private boolean setMagazineLockPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPERATION", userHandle);
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                long callingId = Binder.clearCallingIdentity();
                try {
                    Settings.Global.putInt(this.mContext.getContentResolver(), "custom_magazine_enable", -1);
                    if (policy == 5) {
                        Settings.Global.putInt(this.mContext.getContentResolver(), "custom_magazine_enable", 1);
                    } else if (policy != 6) {
                        throw new IllegalArgumentException("IllegalArgumentException: setMagazineLockPolicy para is illegal!");
                    } else {
                        Settings.Global.putInt(this.mContext.getContentResolver(), "custom_magazine_enable", 0);
                    }
                    Binder.restoreCallingIdentity(callingId);
                } catch (Exception e) {
                    e.printStackTrace();
                    Binder.restoreCallingIdentity(callingId);
                }
                return true;
            }
            return false;
        }
    }

    private int getMagazineLockPolicy(ComponentName admin, int userHandle) {
        return 0;
    }

    private boolean startLockApp(ComponentName admin, int polId, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_APP_RUNNING", userHandle);
        if (data != null) {
            synchronized (getLockObject()) {
                VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
                if (ap != null) {
                    boolean inLockTaskPackages = false;
                    String[] packages = this.mService.getLockTaskPackages(admin);
                    if (packages != null && packages.length != 0) {
                        long callingId = Binder.clearCallingIdentity();
                        try {
                            for (String pkg : packages) {
                                if (pkg.equals(this.mActivityManager.getRunningTasks(1).get(0).topActivity.getPackageName())) {
                                    inLockTaskPackages = true;
                                }
                            }
                            if (!inLockTaskPackages) {
                                Binder.restoreCallingIdentity(callingId);
                                return false;
                            }
                            boolean isAdd = data.getBoolean("state_boolean");
                            List<IBinder> topVisibleActivities = ((ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class)).getTopVisibleActivities();
                            if (isAdd) {
                                ActivityTaskManager.getService().startLockTaskModeByToken(topVisibleActivities.get(0));
                            } else {
                                ActivityTaskManager.getService().stopLockTaskModeByToken(topVisibleActivities.get(0));
                            }
                            Binder.restoreCallingIdentity(callingId);
                            return true;
                        } catch (Exception e) {
                            e.printStackTrace();
                            Binder.restoreCallingIdentity(callingId);
                        }
                    }
                    return false;
                }
                return false;
            }
        }
        throw new IllegalArgumentException("IllegalArgumentException: setOperationAiKeyPolicy para is illegal!");
    }

    private boolean setCustomSettingsMenu(ComponentName admin, int listId, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_APP_RUNNING", userHandle);
        if (data == null) {
            throw new IllegalArgumentException("IllegalArgumentException: setCustomSettingsMenu para is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                ap.settingsMenulist = null;
                ArrayList<String> list = data.getStringArrayList("component_name_list");
                if (list != null && list.size() > 0) {
                    ap.settingsMenulist = list;
                }
                saveVivoSettingsLocked(listId, userHandle);
                return true;
            }
            return false;
        }
    }

    private List<String> getCustomSettingsMenu(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            if (ap != null && ap.settingsMenulist != null) {
                return ap.settingsMenulist;
            }
            return new ArrayList();
        }
    }

    /* JADX WARN: Type inference failed for: r2v2, types: [com.android.server.devicepolicy.VivoCustomDpmsImpl$7] */
    private boolean setTelecomMaskPermissionBwlistPolicy(final ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_TELECOM", userHandle);
        synchronized (getLockObject()) {
            final VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.telecomMaskPermissionBwlist != policy) {
                    ap.telecomMaskPermissionBwlist = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                    new Thread() { // from class: com.android.server.devicepolicy.VivoCustomDpmsImpl.7
                        @Override // java.lang.Thread, java.lang.Runnable
                        public void run() {
                            try {
                                VivoCustomDpmsImpl.this.setTelecomMaskPermissionBwlist(admin, ap, ap.telecomMaskPermissionBwlist == 4);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();
                }
                return true;
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setTelecomMaskPermissionBwlist(ComponentName admin, VivoActiveAdmin ap, boolean isOn) throws RemoteException {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.LAUNCHER");
        List<ResolveInfo> resolveInfos = this.mContext.getPackageManager().queryIntentActivities(intent, 0);
        for (ResolveInfo appInfo : resolveInfos) {
            if (appInfo.system) {
                setPermissionGrant(admin, ap, appInfo.activityInfo.packageName, isOn);
            }
        }
        InputMethodManager imm = (InputMethodManager) this.mContext.getSystemService("input_method");
        List<InputMethodInfo> methodList = imm.getInputMethodList();
        for (InputMethodInfo mi : methodList) {
            setPermissionGrant(admin, ap, mi.getPackageName(), isOn);
        }
        List<PackageInfo> packages = this.mContext.getPackageManager().getInstalledPackages(0);
        for (PackageInfo info : packages) {
            if ((info.applicationInfo.flags & 1) == 0) {
                setPermissionGrant(admin, ap, info.packageName, isOn);
            }
        }
    }

    private void setPermissionGrant(ComponentName admin, VivoActiveAdmin ap, String packageName, boolean isOn) throws RemoteException {
        final CompletableFuture<Boolean> result = new CompletableFuture<>();
        RemoteCallback mRemoteCallback = new RemoteCallback(new RemoteCallback.OnResultListener() { // from class: com.android.server.devicepolicy.-$$Lambda$VivoCustomDpmsImpl$WPYlfk0h1iTydtBaeVSi0fqFPlI
            public final void onResult(Bundle bundle) {
                result.complete(Boolean.valueOf(b != null));
            }
        });
        List<String> appPermissionWhiteList = getAppPermissionWhiteListUncheckedLocked(ap);
        if (this.MASK_EXEMPT_APP.contains(packageName)) {
            return;
        }
        if (appPermissionWhiteList == null || !appPermissionWhiteList.contains(packageName)) {
            if (!isOn) {
                setPermissionGrantState(admin, this.mContext.getPackageName(), packageName, "android.permission.READ_CALL_LOG", 0, mRemoteCallback);
                setPermissionGrantState(admin, this.mContext.getPackageName(), packageName, "android.permission.READ_SMS", 0, mRemoteCallback);
                setPermissionGrantState(admin, this.mContext.getPackageName(), packageName, "android.permission.RECEIVE_SMS", 0, mRemoteCallback);
                setPermissionGrantState(admin, this.mContext.getPackageName(), packageName, "android.permission.READ_CONTACTS", 0, mRemoteCallback);
                VLog.i("VivoUtils", "setPermissionGrant DEFAULT, packageName = " + packageName);
            } else if (ap.phoneMaskPermissionWlist != null && ap.phoneMaskPermissionWlist.contains(packageName)) {
                VLog.i("VivoUtils", "setPermissionGrant GRANTED, packageName = " + packageName);
                setPermissionGrantState(admin, this.mContext.getPackageName(), packageName, "android.permission.READ_CALL_LOG", 1, mRemoteCallback);
                setPermissionGrantState(admin, this.mContext.getPackageName(), packageName, "android.permission.READ_SMS", 1, mRemoteCallback);
                setPermissionGrantState(admin, this.mContext.getPackageName(), packageName, "android.permission.RECEIVE_SMS", 1, mRemoteCallback);
                setPermissionGrantState(admin, this.mContext.getPackageName(), packageName, "android.permission.READ_CONTACTS", 1, mRemoteCallback);
            } else {
                setPermissionGrantState(admin, this.mContext.getPackageName(), packageName, "android.permission.READ_CALL_LOG", 2, mRemoteCallback);
                setPermissionGrantState(admin, this.mContext.getPackageName(), packageName, "android.permission.READ_SMS", 2, mRemoteCallback);
                setPermissionGrantState(admin, this.mContext.getPackageName(), packageName, "android.permission.RECEIVE_SMS", 2, mRemoteCallback);
                setPermissionGrantState(admin, this.mContext.getPackageName(), packageName, "android.permission.READ_CONTACTS", 2, mRemoteCallback);
                VLog.i("VivoUtils", "setPermissionGrant DENIED, packageName = " + packageName);
            }
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r1v0, types: [com.android.server.devicepolicy.VivoCustomDpmsImpl] */
    /* JADX WARN: Type inference failed for: r1v1 */
    private void setPermissionGrantState(final ComponentName admin, final String callerPackage, String packageName, final String permission, final int grantState, final RemoteCallback callback) throws RemoteException {
        final boolean isPostQAdmin;
        Bundle bundle = this;
        Objects.requireNonNull(callback);
        UserHandle user = Binder.getCallingUserHandle();
        synchronized (getLockObject()) {
            long ident = Binder.clearCallingIdentity();
            try {
                try {
                    isPostQAdmin = bundle.getTargetSdk(callerPackage, user.getIdentifier()) >= 29;
                    if (!isPostQAdmin) {
                        try {
                            if (bundle.getTargetSdk(packageName, user.getIdentifier()) < 23) {
                                callback.sendResult((Bundle) null);
                                Binder.restoreCallingIdentity(ident);
                                return;
                            }
                        } catch (SecurityException e) {
                            e = e;
                            bundle = 0;
                            VLog.e(VIVO_LOG_TAG, "Could not set permission grant state", e);
                            callback.sendResult(bundle);
                            Binder.restoreCallingIdentity(ident);
                        } catch (Throwable th) {
                            th = th;
                            Binder.restoreCallingIdentity(ident);
                            throw th;
                        }
                    }
                } catch (SecurityException e2) {
                    e = e2;
                }
                try {
                    try {
                    } catch (PackageManager.NameNotFoundException e3) {
                        throw new RemoteException("Cannot check if " + permission + "is a runtime permission", e3, false, true);
                    }
                } catch (SecurityException e4) {
                    e = e4;
                    VLog.e(VIVO_LOG_TAG, "Could not set permission grant state", e);
                    callback.sendResult(bundle);
                    Binder.restoreCallingIdentity(ident);
                }
                if (!bundle.isRuntimePermission(permission)) {
                    callback.sendResult((Bundle) null);
                    Binder.restoreCallingIdentity(ident);
                    return;
                }
                if (grantState == 1 || grantState == 2 || grantState == 0) {
                    bundle.getPermissionControllerManager(user).setRuntimePermissionGrantStateByDeviceAdmin(callerPackage, packageName, permission, grantState, bundle.mContext.getMainExecutor(), new Consumer() { // from class: com.android.server.devicepolicy.-$$Lambda$VivoCustomDpmsImpl$LjrFRVvluUD2_n95HaccrRACEd4
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            VivoCustomDpmsImpl.lambda$setPermissionGrantState$3(isPostQAdmin, callback, admin, callerPackage, permission, grantState, (Boolean) obj);
                        }
                    });
                }
                Binder.restoreCallingIdentity(ident);
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$setPermissionGrantState$3(boolean isPostQAdmin, RemoteCallback callback, ComponentName admin, String callerPackage, String permission, int grantState, Boolean permissionWasSet) {
        if (isPostQAdmin && !permissionWasSet.booleanValue()) {
            callback.sendResult((Bundle) null);
            return;
        }
        boolean isDelegate = admin == null;
        DevicePolicyEventLogger.createEvent(19).setAdmin(callerPackage).setStrings(new String[]{permission}).setInt(grantState).setBoolean(isDelegate).write();
        callback.sendResult(Bundle.EMPTY);
    }

    private int getTargetSdk(String packageName, int userId) {
        try {
            ApplicationInfo ai = this.mIPackageManager.getApplicationInfo(packageName, 0, userId);
            if (ai == null) {
                return 0;
            }
            return ai.targetSdkVersion;
        } catch (RemoteException e) {
            return 0;
        }
    }

    private PermissionControllerManager getPermissionControllerManager(UserHandle user) {
        if (user.equals(this.mContext.getUser())) {
            return (PermissionControllerManager) this.mContext.getSystemService(PermissionControllerManager.class);
        }
        try {
            return (PermissionControllerManager) this.mContext.createPackageContextAsUser(this.mContext.getPackageName(), 0, user).getSystemService(PermissionControllerManager.class);
        } catch (PackageManager.NameNotFoundException notPossible) {
            throw new IllegalStateException(notPossible);
        }
    }

    private boolean isRuntimePermission(String permissionName) throws PackageManager.NameNotFoundException {
        PackageManager packageManager = this.mContext.getPackageManager();
        PermissionInfo permissionInfo = packageManager.getPermissionInfo(permissionName, 0);
        return (permissionInfo.protectionLevel & 15) == 1;
    }

    private int getTelecomMaskPermissionBwlistPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.telecomMaskPermissionBwlist : 0;
        }
        return i;
    }

    private boolean setOperationAppNotificationPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_OPERATION", userHandle);
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.operationAppNotification != policy) {
                    ap.operationAppNotification = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getOperationAppNotificationPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.operationAppNotification : 0;
        }
        return i;
    }

    private boolean setAdminDeviceOwnerPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_ADMIN_OWNER", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.adminDeviceOwnerPolicy != policy) {
                    ap.adminDeviceOwnerPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    public int getAdminDeviceOwnerPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.adminDeviceOwnerPolicy : 0;
        }
        return i;
    }

    private boolean setAdminProfileOwnerPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_ADMIN_OWNER", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.adminProfileOwnerPolicy != policy) {
                    ap.adminProfileOwnerPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    public int getAdminProfileOwnerPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.adminProfileOwnerPolicy : 0;
        }
        return i;
    }

    private boolean setDeviceOwner(ComponentName admin, int transId, Bundle data, int userHandle) {
        boolean z;
        int callingUid = Binder.getCallingUid();
        if (data == null) {
            throw new IllegalArgumentException("IllegalArgumentException: setDeviceOwner para is illegal!");
        }
        String name = data.getString("package_name");
        if (name == null) {
            throw new IllegalArgumentException("IllegalArgumentException: setDeviceOwner name is null!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(getVivoAdminUncheckedLocked(userHandle), userHandle);
            if (ap != null && callingUid == ap.getUid()) {
                checkCallingEmmPermission(getVivoAdminUncheckedLocked(userHandle), "EMM_ADMIN_OWNER", userHandle);
                boolean success = true;
                if (transId == 3301) {
                    long callingId = Binder.clearCallingIdentity();
                    try {
                        if (this.mService.getActiveAdminUncheckedLocked(admin, userHandle) == null) {
                            this.mService.setActiveAdmin(admin, true, userHandle);
                        }
                        this.mIsEmmAPI = true;
                        success = this.mService.setDeviceOwner(admin, name, userHandle);
                        if (!success) {
                            z = false;
                        } else if (ap.appDisabledApplist == null || ap.appDisabledApplist.isEmpty() || !ap.appDisabledApplist.contains("com.vivo.doubleinstance")) {
                            try {
                                PackageInfo packageInfo = this.mIPackageManager.getPackageInfo("com.vivo.doubleinstance", 0, userHandle);
                                if (packageInfo != null) {
                                    z = false;
                                    try {
                                        this.mIPackageManager.setApplicationEnabledSetting("com.vivo.doubleinstance", 2, 0, userHandle, this.mContext.getPackageName());
                                    } catch (Exception e) {
                                    } catch (Throwable th) {
                                        th = th;
                                        this.mIsEmmAPI = z;
                                        Binder.restoreCallingIdentity(callingId);
                                        throw th;
                                    }
                                } else {
                                    z = false;
                                }
                            } catch (Exception e2) {
                                z = false;
                            }
                        } else {
                            z = false;
                        }
                        this.mIsEmmAPI = z;
                        Binder.restoreCallingIdentity(callingId);
                    } catch (Throwable th2) {
                        th = th2;
                        z = false;
                    }
                } else if (transId == 3302) {
                    this.mIsEmmAPI = true;
                    this.mService.clearDeviceOwner(name);
                    if (ap.appDisabledApplist == null || ap.appDisabledApplist.isEmpty() || !ap.appDisabledApplist.contains("com.vivo.doubleinstance")) {
                        long callingId2 = Binder.clearCallingIdentity();
                        try {
                            PackageInfo packageInfo2 = this.mIPackageManager.getPackageInfo("com.vivo.doubleinstance", 0, userHandle);
                            if (packageInfo2 != null) {
                                this.mIPackageManager.setApplicationEnabledSetting("com.vivo.doubleinstance", 1, 0, userHandle, this.mContext.getPackageName());
                            }
                        } catch (Exception e3) {
                        }
                        Binder.restoreCallingIdentity(callingId2);
                    }
                    this.mIsEmmAPI = false;
                    return success;
                }
                return success;
            }
            return false;
        }
    }

    private boolean setProfileOwner(ComponentName admin, int transId, Bundle data, int userHandle) {
        int callingUid = Binder.getCallingUid();
        if (data == null) {
            throw new IllegalArgumentException("IllegalArgumentException: setProfileOwner para is illegal!");
        }
        String name = data.getString("package_name");
        ComponentName component = (ComponentName) data.getParcelable("component_name");
        if (name == null && component == null) {
            throw new IllegalArgumentException("IllegalArgumentException: setProfileOwner name is null!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(getVivoAdminUncheckedLocked(userHandle), userHandle);
            if (ap == null || callingUid != ap.getUid()) {
                return false;
            }
            checkCallingEmmPermission(getVivoAdminUncheckedLocked(userHandle), "EMM_ADMIN_OWNER", userHandle);
            boolean success = true;
            if (transId == 3304) {
                long callingId = Binder.clearCallingIdentity();
                if (this.mService.getActiveAdminUncheckedLocked(admin, userHandle) == null) {
                    this.mService.setActiveAdmin(admin, true, userHandle);
                }
                success = this.mService.setProfileOwner(admin, name, userHandle);
                Binder.restoreCallingIdentity(callingId);
            } else if (transId == 3305) {
                this.mIsEmmAPI = true;
                this.mService.clearProfileOwner(component);
                this.mIsEmmAPI = false;
                return success;
            }
            return success;
        }
    }

    private Bundle getDeviceOwnerInfo(int userHandle) {
        Bundle bundle;
        synchronized (getLockObject()) {
            long callingId = Binder.clearCallingIdentity();
            this.mIsEmmAPI = true;
            ComponentName component = this.mService.getDeviceOwnerComponent(true);
            String name = this.mService.getDeviceOwnerName();
            int userid = this.mService.getDeviceOwnerUserId();
            this.mIsEmmAPI = false;
            Binder.restoreCallingIdentity(callingId);
            bundle = new Bundle();
            bundle.putParcelable("component_name", component);
            bundle.putString("package_name", name);
            bundle.putInt("args_flag_int", userid);
            if (component != null) {
                VLog.i(VIVO_LOG_TAG, "getDeviceOwnerInfo componentName =" + component.flattenToString() + ", userid = " + userid);
            }
        }
        return bundle;
    }

    private Bundle getProfileOwnerInfo(int userHandle) {
        Bundle bundle;
        synchronized (getLockObject()) {
            long callingId = Binder.clearCallingIdentity();
            this.mIsEmmAPI = true;
            ComponentName component = this.mService.getProfileOwner(userHandle);
            String name = this.mService.getProfileOwnerName(userHandle);
            this.mIsEmmAPI = false;
            Binder.restoreCallingIdentity(callingId);
            bundle = new Bundle();
            bundle.putParcelable("component_name", component);
            bundle.putString("package_name", name);
            if (component != null) {
                VLog.i(VIVO_LOG_TAG, "getProfileOwnerInfo componentName =" + component.flattenToString());
            }
        }
        return bundle;
    }

    private boolean setOrganizationName(ComponentName admin, Bundle data, int userHandle) {
        if (data == null) {
            throw new IllegalArgumentException("IllegalArgumentException: setOrganizationName para is illegal!");
        }
        VivoActiveAdmin ap = getVivoActiveAdmin(getVivoAdminUncheckedLocked(userHandle), userHandle);
        if (ap != null) {
            CharSequence name = data.getCharSequence("state_char");
            this.mService.setOrganizationName(admin, name);
            return true;
        }
        return false;
    }

    private Bundle getOrganizationName() {
        Bundle bundle;
        synchronized (getLockObject()) {
            long callingId = Binder.clearCallingIdentity();
            CharSequence organizationName = this.mService.getDeviceOwnerOrganizationName();
            Binder.restoreCallingIdentity(callingId);
            bundle = new Bundle();
            bundle.putString("state_string", organizationName != null ? organizationName.toString() : null);
        }
        return bundle;
    }

    /* JADX WARN: Removed duplicated region for block: B:66:0x01e6 A[Catch: all -> 0x0250, Exception -> 0x0254, TryCatch #6 {Exception -> 0x0254, all -> 0x0250, blocks: (B:11:0x0030, B:13:0x0034, B:14:0x0040, B:16:0x0064, B:17:0x0068, B:19:0x006e, B:21:0x007a, B:23:0x007e, B:26:0x00b7, B:28:0x00bb, B:30:0x00c9, B:32:0x00cf, B:34:0x00dd, B:39:0x0107, B:41:0x0137, B:43:0x013d, B:64:0x01c2, B:66:0x01e6, B:68:0x01f5, B:70:0x01fe, B:72:0x020a, B:74:0x0216, B:76:0x0222, B:44:0x0146, B:46:0x014c, B:47:0x0168, B:49:0x016e, B:50:0x0176, B:52:0x017c, B:53:0x01a0, B:55:0x01a3, B:61:0x01ba, B:59:0x01b0, B:60:0x01b5), top: B:119:0x0030 }] */
    /* JADX WARN: Removed duplicated region for block: B:79:0x0231  */
    /* JADX WARN: Removed duplicated region for block: B:81:0x0235 A[Catch: Exception -> 0x0248, all -> 0x0273, TRY_LEAVE, TryCatch #2 {all -> 0x0273, blocks: (B:78:0x022d, B:81:0x0235, B:92:0x0257), top: B:115:0x002c }] */
    /* JADX WARN: Removed duplicated region for block: B:85:0x024a  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private boolean setAdminAccessibilityService(android.content.ComponentName r18, int r19, android.os.Bundle r20, int r21) {
        /*
            Method dump skipped, instructions count: 665
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.devicepolicy.VivoCustomDpmsImpl.setAdminAccessibilityService(android.content.ComponentName, int, android.os.Bundle, int):boolean");
    }

    private Bundle getAdminAccessibilityService(ComponentName admin, int transId, Bundle data, int userHandle) {
        VivoActiveAdmin ap;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            if (ap != null && ap.adminActiveAccessibilitylist != null && !ap.adminActiveAccessibilitylist.isEmpty()) {
                Bundle bundle = new Bundle();
                ArrayList<ComponentName> arrayList = new ArrayList<>();
                for (String component : ap.adminActiveAccessibilitylist) {
                    arrayList.add(ComponentName.unflattenFromString(component));
                    VLog.d(VIVO_LOG_TAG, "getAdminAccessibilityService component = " + component);
                }
                bundle.putParcelableArrayList("component_name_list", arrayList);
                return bundle;
            }
            return null;
        }
    }

    private boolean setPermittedAccessibilityServices(ComponentName admin, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_ADMIN_ACCESSIBILITY", userHandle);
        if (data == null) {
            throw new IllegalArgumentException("IllegalArgumentException: setPermittedAccessibilityServices para is illegal!");
        }
        VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
        if (ap != null) {
            ArrayList<String> packageList = data.getStringArrayList("package_name_list");
            return this.mService.setPermittedAccessibilityServices(admin, packageList);
        }
        return false;
    }

    private Bundle getPermittedAccessibilityServices(ComponentName admin, int userHandle) {
        ArrayList<String> permittedList = new ArrayList<>();
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                List<String> list = this.mService.getPermittedAccessibilityServices(admin);
                if (list != null) {
                    permittedList.addAll(list);
                    VLog.d(VIVO_LOG_TAG, "getPermittedAccessibilityServices list = " + String.join(",", list));
                } else {
                    permittedList = null;
                }
                Bundle bundle = new Bundle();
                bundle.putStringArrayList("package_name_list", permittedList);
                return bundle;
            }
            return null;
        }
    }

    private boolean setAdminDeviceAdmin(ComponentName admin, int transId, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_ADMIN_OWNER", userHandle);
        if (data != null) {
            ComponentName comp = (ComponentName) data.getParcelable("component_name");
            if (comp != null) {
                boolean isActive = data.getBoolean("state_boolean");
                synchronized (getLockObject()) {
                    long callingId = Binder.clearCallingIdentity();
                    try {
                        if (isActive) {
                            this.mService.setActiveAdmin(comp, false, userHandle);
                            if (this.mService.getActiveAdminUncheckedLocked(comp, userHandle) == null) {
                                Binder.restoreCallingIdentity(callingId);
                                return false;
                            }
                        } else if (this.mService.getActiveAdminUncheckedLocked(comp, userHandle) == null) {
                            Binder.restoreCallingIdentity(callingId);
                            return false;
                        } else {
                            this.mService.removeActiveAdmin(comp, userHandle);
                            if (!this.mService.isRemovingAdmin(comp, userHandle) && this.mService.getActiveAdminUncheckedLocked(comp, userHandle) != null) {
                                Binder.restoreCallingIdentity(callingId);
                                return false;
                            }
                        }
                        Binder.restoreCallingIdentity(callingId);
                        return true;
                    } catch (Exception e) {
                        VLog.e(VIVO_LOG_TAG, "setAdminDeviceAdmin fail! " + e);
                        Binder.restoreCallingIdentity(callingId);
                        return false;
                    }
                }
            }
            throw new IllegalArgumentException("IllegalArgumentException: setAdminDeviceAdmin para is illegal!");
        }
        throw new IllegalArgumentException("IllegalArgumentException: setAdminDeviceAdmin para is illegal!");
    }

    private boolean isAccessibilityServicePermitted(ComponentName admin, int transId, Bundle data, int userHandle) {
        if (data != null) {
            String packageName = data.getString("package_name");
            if (packageName != null) {
                synchronized (getLockObject()) {
                    VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
                    if (ap != null) {
                        long callingId = Binder.clearCallingIdentity();
                        try {
                            boolean isAccessibilityServicePermittedByAdmin = this.mService.isAccessibilityServicePermittedByAdmin(admin, packageName, userHandle);
                            Binder.restoreCallingIdentity(callingId);
                            return isAccessibilityServicePermittedByAdmin;
                        } catch (Exception e) {
                            VLog.e(VIVO_LOG_TAG, "isAccessibilityServicePermitted fail! " + e);
                            Binder.restoreCallingIdentity(callingId);
                            return false;
                        }
                    }
                    return false;
                }
            }
            throw new IllegalArgumentException("IllegalArgumentException: isAccessibilityServicePermitted para is illegal!");
        }
        throw new IllegalArgumentException("IllegalArgumentException: isAccessibilityServicePermitted para is illegal!");
    }

    private Bundle getPermittedAccessibilityServicesForUser(ComponentName admin, int transId, Bundle data, int userHandle) {
        ArrayList<String> permittedList = new ArrayList<>();
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap == null) {
                return null;
            }
            long callingId = Binder.clearCallingIdentity();
            List<String> list = this.mService.getPermittedAccessibilityServicesForUser(userHandle);
            if (list != null) {
                permittedList.addAll(list);
                VLog.d(VIVO_LOG_TAG, "getPermittedAccessibilityServicesForUser list = " + String.join(",", list));
                Binder.restoreCallingIdentity(callingId);
                Bundle bundle = new Bundle();
                bundle.putStringArrayList("package_name_list", permittedList);
                return bundle;
            }
            Binder.restoreCallingIdentity(callingId);
            return null;
        }
    }

    private List<String> getDeviceInfo1(ComponentName admin, int userHandle) {
        List<String> info = new ArrayList<>();
        return info;
    }

    private boolean setForgotPasswordPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_SEC_LOCK_PASSWORD", userHandle);
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (ap.securityForgotPasswordPolicy != policy) {
                    ap.securityForgotPasswordPolicy = policy;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                return true;
            }
            return false;
        }
    }

    private int getForgotPasswordPolicy(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        int i;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            i = ap != null ? ap.securityForgotPasswordPolicy : 0;
        }
        return i;
    }

    private Bundle isDeviceRoot(ComponentName admin, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_SECURITY", userHandle);
        synchronized (getLockObject()) {
            Bundle bundle = new Bundle();
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                int root = SystemProperties.getInt("persist.sys.is_root", 0);
                bundle.putInt("state_int", root);
                return bundle;
            }
            return null;
        }
    }

    private boolean lockNow(ComponentName admin, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_SEC_LOCK_PASSWORD", userHandle);
        synchronized (getLockObject()) {
            new Bundle();
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                this.mService.lockNow(0, false);
                return true;
            }
            return false;
        }
    }

    private boolean setResetPasswordToken(ComponentName admin, int transId, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_SEC_LOCK_PASSWORD", userHandle);
        if (data != null) {
            byte[] token = data.getByteArray("state_byte");
            synchronized (getLockObject()) {
                VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
                if (ap != null) {
                    long callingId = Binder.clearCallingIdentity();
                    KeyguardManager km = (KeyguardManager) this.mContext.getSystemService("keyguard");
                    try {
                        km.createConfirmDeviceCredentialIntent(null, null, userHandle);
                        Binder.restoreCallingIdentity(callingId);
                    } catch (Exception e) {
                        VLog.e(VIVO_LOG_TAG, "setResetPasswordToken fail! " + e);
                        e.printStackTrace();
                        Binder.restoreCallingIdentity(callingId);
                    }
                    try {
                        if (this.mService.setResetPasswordToken(admin, token)) {
                            VivoPolicyData policy = getVivoUserData(userHandle);
                            long handle = this.mService.getUserData(userHandle).mPasswordTokenHandle;
                            if (policy.mPasswordToken != handle) {
                                policy.mPasswordToken = handle;
                                saveVivoSettingsLocked(transId, userHandle);
                            }
                            return true;
                        }
                    } catch (Exception e2) {
                        VLog.e(VIVO_LOG_TAG, "setResetPasswordToken fail! " + e2);
                        e2.printStackTrace();
                    }
                }
                return false;
            }
        }
        throw new IllegalArgumentException("IllegalArgumentException: setResetPasswordToken para is illegal!");
    }

    private boolean clearResetPasswordToken(ComponentName admin, int transId, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_SEC_LOCK_PASSWORD", userHandle);
        synchronized (getLockObject()) {
            new Bundle();
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                try {
                    boolean success = this.mService.clearResetPasswordToken(admin);
                    VivoPolicyData policy = getVivoUserData(userHandle);
                    policy.mPasswordToken = 0L;
                    saveVivoSettingsLocked(transId, userHandle);
                    return success;
                } catch (Exception e) {
                    VLog.e(VIVO_LOG_TAG, "clearResetPasswordToken fail! " + e);
                    e.printStackTrace();
                }
            }
            return false;
        }
    }

    private Bundle isResetPasswordTokenActive(ComponentName admin, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_SEC_LOCK_PASSWORD", userHandle);
        synchronized (getLockObject()) {
            Bundle bundle = new Bundle();
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                boolean active = this.mService.isResetPasswordTokenActive(admin);
                VLog.v(VIVO_LOG_TAG, "isResetPasswordTokenActive active:" + active);
                bundle.putBoolean("state_boolean", active);
                return bundle;
            }
            return null;
        }
    }

    private boolean resetPasswordWithToken(ComponentName admin, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_SEC_LOCK_PASSWORD", userHandle);
        if (data == null) {
            throw new IllegalArgumentException("IllegalArgumentException: resetPasswordWithToken para is illegal!");
        }
        byte[] token = data.getByteArray("state_byte");
        String password = data.getString("state_string");
        int flags = data.getInt("state_int");
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap == null) {
                return false;
            }
            boolean success = this.mService.resetPasswordWithToken(admin, password, token, flags);
            if ((password == null || Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK.equals(password)) && success) {
                Intent intent = new Intent("com.vivo.secure.keyguard.enable.or.disable");
                intent.putExtra("enable", false);
                intent.setPackage("com.vivo.fingerprint");
                this.mContext.sendBroadcast(intent);
                intent.setPackage("com.vivo.faceunlock");
                this.mContext.sendBroadcast(intent);
            }
            return success;
        }
    }

    private boolean setMaximumTimeToLock(ComponentName admin, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_SEC_LOCK_PASSWORD", userHandle);
        if (data == null) {
            throw new IllegalArgumentException("IllegalArgumentException: setMaximumTimeToLock para is illegal!");
        }
        long timeMs = data.getLong("state_long");
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                this.mService.setMaximumTimeToLock(admin, timeMs, false);
                return true;
            }
            return false;
        }
    }

    private Bundle getMaximumTimeToLock(ComponentName admin, int userHandle) {
        synchronized (getLockObject()) {
            Bundle bundle = new Bundle();
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                long timeMs = this.mService.getMaximumTimeToLock(admin, userHandle, false);
                bundle.putLong("state_long", timeMs);
                return bundle;
            }
            return null;
        }
    }

    private boolean setRequiredStrongAuthTimeout(ComponentName admin, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_SEC_LOCK_PASSWORD", userHandle);
        if (data == null) {
            throw new IllegalArgumentException("IllegalArgumentException: setRequiredStrongAuthTimeout para is illegal!");
        }
        long timeMs = data.getLong("state_long");
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                this.mService.setRequiredStrongAuthTimeout(admin, timeMs, false);
                return true;
            }
            return false;
        }
    }

    private Bundle getRequiredStrongAuthTimeout(ComponentName admin, int userHandle) {
        synchronized (getLockObject()) {
            Bundle bundle = new Bundle();
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                long timeMs = this.mService.getRequiredStrongAuthTimeout(admin, userHandle, false);
                bundle.putLong("state_long", timeMs);
                return bundle;
            }
            return null;
        }
    }

    private boolean setPasswordExpirationTimeout(ComponentName admin, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_SEC_LOCK_PASSWORD", userHandle);
        if (data == null) {
            throw new IllegalArgumentException("IllegalArgumentException: setPasswordExpirationTimeout para is illegal!");
        }
        long timeMs = data.getLong("state_long");
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                this.mService.setPasswordExpirationTimeout(admin, timeMs, false);
                return true;
            }
            return false;
        }
    }

    private Bundle getPasswordExpirationTimeout(ComponentName admin, int userHandle) {
        synchronized (getLockObject()) {
            Bundle bundle = new Bundle();
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                long timeMs = this.mService.getPasswordExpirationTimeout(admin, userHandle, false);
                bundle.putLong("state_long", timeMs);
                return bundle;
            }
            return null;
        }
    }

    private boolean setPasswordQuality(ComponentName admin, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_SEC_LOCK_PASSWORD", userHandle);
        if (data == null) {
            throw new IllegalArgumentException("IllegalArgumentException: setPasswordQuality para is illegal!");
        }
        int quality = data.getInt("state_int");
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                this.mService.setPasswordQuality(admin, quality, false);
                return true;
            }
            return false;
        }
    }

    private Bundle getPasswordQuality(ComponentName admin, int userHandle) {
        synchronized (getLockObject()) {
            Bundle bundle = new Bundle();
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                int quality = this.mService.getPasswordQuality(admin, userHandle, false);
                VLog.v(VIVO_LOG_TAG, "getPasswordQuality quality:" + quality);
                bundle.putInt("state_int", quality);
                return bundle;
            }
            return null;
        }
    }

    private boolean setKeyguardDisabledFeatures(ComponentName admin, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_SEC_LOCK_PASSWORD", userHandle);
        if (data == null) {
            throw new IllegalArgumentException("IllegalArgumentException: setKeyguardDisabledFeatures para is illegal!");
        }
        int flag = data.getInt("state_int");
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                this.mService.setKeyguardDisabledFeatures(admin, flag, false);
                return true;
            }
            return false;
        }
    }

    private Bundle getKeyguardDisabledFeatures(ComponentName admin, int userHandle) {
        synchronized (getLockObject()) {
            Bundle bundle = new Bundle();
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                int flag = this.mService.getKeyguardDisabledFeatures(admin, userHandle, false);
                VLog.v(VIVO_LOG_TAG, "getKeyguardDisabledFeatures flag:" + flag);
                bundle.putInt("state_int", flag);
                return bundle;
            }
            return null;
        }
    }

    private boolean setSystemPermission(ComponentName admin, int transId, Bundle data, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_APP_SYSTEM_PERMISSION", userHandle);
        if (data == null) {
            throw new IllegalArgumentException("IllegalArgumentException: setSystemPermission para is illegal!");
        }
        String packageName = data.getString("package_name");
        String permission = data.getString("permission_string");
        int grantState = data.getInt("state_int");
        List<String> relatedPkgs = getEmmRelatedPkgs(admin.getPackageName(), userHandle);
        if (packageName == null || (((relatedPkgs == null || !relatedPkgs.contains(packageName)) && !getEmmPackage(userHandle).contains(packageName)) || permission == null || grantState < 1 || grantState > 4)) {
            throw new IllegalArgumentException("IllegalArgumentException: setSystemPermission para is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                long callingId = Binder.clearCallingIdentity();
                VivoPermissionManager.getVPM(this.mContext).setAppPermission(packageName, VivoPermissionType.getVPType(permission).getVPTypeId(), grantState);
                Binder.restoreCallingIdentity(callingId);
                return true;
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int updateAllBgStartUpDB(String pkgName, int curState) {
        ContentValues values = new ContentValues();
        values.put(PERMISSION_MANAGER_PKGNAME, pkgName);
        values.put(PERMISSION_MANAGER_CURRENT_STATE, Integer.valueOf(curState));
        try {
            int updateRet = this.mContentResolver.update(BG_START_UP_URI, values, "pkgname=?", new String[]{pkgName});
            return updateRet;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    private int updateAllControlLockedScreenDB(String pkgName, int curState) {
        ContentValues values = new ContentValues();
        values.put(PERMISSION_MANAGER_PKGNAME, pkgName);
        values.put(PERMISSION_MANAGER_CURRENT_STATE, Integer.valueOf(curState));
        try {
            int updateRet = this.mContentResolver.update(CONTROL_LOCK_SCRENN_URI, values, "pkgname=?", new String[]{pkgName});
            return updateRet;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int updateAllBgActivityDB(String pkgName, int curState) {
        ContentValues values = new ContentValues();
        values.put(PERMISSION_MANAGER_PKGNAME, pkgName);
        values.put(PERMISSION_MANAGER_CURRENT_STATE, Integer.valueOf(curState));
        try {
            int updateRet = this.mContentResolver.update(BG_ACTIVITY_URI, values, "pkgname=?", new String[]{pkgName});
            return updateRet;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    private String encrypt(String resource) {
        if (resource == null || resource.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK)) {
            return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        }
        try {
            byte[] secretArr = encryptMode(resource.getBytes("UTF-8"));
            byte[] secret = Base64.encode(secretArr, 0);
            String secretString = new String(secret);
            return secretString;
        } catch (Exception e) {
            e.printStackTrace();
            return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        }
    }

    private byte[] build3Deskey(String keyStr) throws Exception {
        byte[] key = new byte[24];
        byte[] temp = keyStr.getBytes();
        if (key.length > temp.length) {
            System.arraycopy(temp, 0, key, 0, temp.length);
        } else {
            System.arraycopy(temp, 0, key, 0, key.length);
        }
        return key;
    }

    private byte[] encryptMode(byte[] src) {
        try {
            SecretKey deskey = new SecretKeySpec(build3Deskey(PERMISSION_MANAGER_PASSWORD_CRYPT_KEY), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(1, deskey);
            return cipher.doFinal(src);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean isFwSupportDes() {
        String support = SystemProperties.get("persist.sys.name.ec.enable", "0");
        if ("0".equals(support)) {
            return false;
        }
        return true;
    }

    private void updateReadInstalledAppStatus(String pkgName, boolean forbid) {
        int isForce;
        String encrypted = pkgName;
        if (isFwSupportDes()) {
            encrypted = encrypt(pkgName);
        }
        if (forbid) {
            isForce = 2;
        } else {
            isForce = 0;
        }
        try {
            ContentValues values = new ContentValues();
            values.put(PERMISSION_MANAGER_ISFORCE, Integer.valueOf(isForce));
            values.put("status", (Integer) 2);
            this.mContentResolver.update(READ_INSTALLED_URI, values, "pkgname=?", new String[]{encrypted});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class AppsPermisionsThread extends Thread {
        private boolean isAdd;
        private List<String> pkgs;
        private int userId;

        public AppsPermisionsThread(List<String> pkgs, int userId, boolean isAdd) {
            this.pkgs = null;
            this.pkgs = pkgs;
            this.userId = userId;
            this.isAdd = isAdd;
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            VLog.w(VivoCustomDpmsImpl.VIVO_LOG_TAG, "AppsPermisionsThread run  pkgs = " + this.pkgs);
            synchronized (AppsPermisionsThread.class) {
                VivoCustomDpmsImpl.this.grantAppsPermisions(this.pkgs, this.userId, this.isAdd);
            }
        }
    }

    private void grantNotificationListener(String packageName, int userId) {
        new ArrayList();
        List<ResolveInfo> installedServices = this.mContext.getPackageManager().queryIntentServicesAsUser(new Intent("android.service.notification.NotificationListenerService"), KernelConfig.WAIT_VSYNC, userId);
        if (this.mNotificationManager == null) {
            this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        }
        for (ResolveInfo resolveInfo : installedServices) {
            ServiceInfo info = resolveInfo.serviceInfo;
            if ("android.permission.BIND_NOTIFICATION_LISTENER_SERVICE".equals(info.permission) && info.packageName.equals(packageName)) {
                ComponentName comp = new ComponentName(info.packageName, info.name);
                VLog.w(VIVO_LOG_TAG, "grantNotificationListener comp = " + comp);
                this.mNotificationManager.setNotificationListenerAccessGranted(comp, true);
            }
        }
    }

    private void registerPermissionMonitor(int userHandle) {
        VivoActiveAdmin ap = getVivoUserData(userHandle).mVivoActiveAdmin;
        if (ap != null && ap.appPermissionWlist != null && !ap.appPermissionWlist.isEmpty()) {
            long callingId = Binder.clearCallingIdentity();
            try {
                try {
                    IAppOpsService iAppOps = IAppOpsService.Stub.asInterface(ServiceManager.getService("appops"));
                    for (String packageName : ap.appPermissionWlist) {
                        PackageInfo packageInfo = this.mContext.getPackageManager().getPackageInfoAsUser(packageName, 4096, userHandle);
                        if (packageInfo != null) {
                            VLog.w(VIVO_LOG_TAG, "registerPermissionMonitor packageName = " + packageName);
                            if (packageInfo.requestedPermissions != null && Arrays.asList(packageInfo.requestedPermissions).contains("android.permission.WRITE_SETTINGS")) {
                                iAppOps.startWatchingMode(23, packageName, this.mAppOpsCallback);
                            }
                        }
                    }
                } catch (Exception e) {
                    VLog.e(VIVO_LOG_TAG, "registerPermissionMonitor failed");
                    e.printStackTrace();
                }
            } finally {
                Binder.restoreCallingIdentity(callingId);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void grantAppsPermisions(List<String> pkgs, int userId, boolean isAdd) {
        VLog.w(VIVO_LOG_TAG, "grantAppsPermisions pkgs = " + pkgs);
        if (pkgs != null && !pkgs.isEmpty()) {
            IAppOpsService iAppOps = IAppOpsService.Stub.asInterface(ServiceManager.getService("appops"));
            for (String packageName : pkgs) {
                try {
                    PackageInfo packageInfo = this.mContext.getPackageManager().getPackageInfoAsUser(packageName, 4096, userId);
                    VLog.w(VIVO_LOG_TAG, "grantAppsPermisions packageName = " + packageName + ", packageInfo = " + packageInfo);
                    if (packageInfo != null) {
                        if (packageInfo.requestedPermissions != null && Arrays.asList(packageInfo.requestedPermissions).contains("android.permission.WRITE_SETTINGS")) {
                            this.mAppOpsManager.setMode(23, packageInfo.applicationInfo.uid, packageName, 0);
                            iAppOps.startWatchingMode(23, packageName, this.mAppOpsCallback);
                        }
                        this.mAppOpsManager.setMode(67, packageInfo.applicationInfo.uid, packageName, 0);
                        this.mAppOpsManager.setMode(24, packageInfo.applicationInfo.uid, packageName, 0);
                        this.mAppOpsManager.setMode(43, packageInfo.applicationInfo.uid, packageName, 0);
                        this.mAppOpsManager.setMode(25, packageInfo.applicationInfo.uid, packageName, 0);
                        updateAllBgStartUpDB(packageName, 0);
                        updateAllControlLockedScreenDB(packageName, 0);
                        updateAllBgActivityDB(packageName, 0);
                        if (this.mVivoPermissionManager == null) {
                            this.mVivoPermissionManager = new VivoPermissionManager();
                        }
                        PackageManagerService pms = ServiceManager.getService("package");
                        if (isAdd) {
                            this.mVivoPermissionManager.setAppPermission(packageName, VivoPermissionType.getVPType("android.permission.SET_WALLPAPER").getVPTypeId(), 1);
                            pms.updatePermissionFlags("android.permission.SET_WALLPAPER", packageName, 48, 48, true, userId);
                            this.mVivoPermissionManager.setAppPermission(packageName, VivoPermissionType.getVPType("android.permission.CALL_PHONE").getVPTypeId(), 1);
                            pms.updatePermissionFlags("android.permission.CALL_PHONE", packageName, 48, 48, true, userId);
                            this.mVivoPermissionManager.setAppPermission(packageName, VivoPermissionType.getVPType("android.permission.READ_PHONE_STATE").getVPTypeId(), 1);
                            pms.updatePermissionFlags("android.permission.READ_PHONE_STATE", packageName, 48, 48, true, userId);
                            this.mVivoPermissionManager.setAppPermission(packageName, VivoPermissionType.getVPType("android.permission.READ_CALL_LOG").getVPTypeId(), 1);
                            pms.updatePermissionFlags("android.permission.READ_CALL_LOG", packageName, 48, 48, true, userId);
                            this.mVivoPermissionManager.setAppPermission(packageName, VivoPermissionType.getVPType("android.permission.WRITE_CALL_LOG").getVPTypeId(), 1);
                            pms.updatePermissionFlags("android.permission.WRITE_CALL_LOG", packageName, 48, 48, true, userId);
                            this.mVivoPermissionManager.setAppPermission(packageName, VivoPermissionType.getVPType("android.permission.SEND_SMS").getVPTypeId(), 1);
                            pms.updatePermissionFlags("android.permission.SEND_SMS", packageName, 48, 48, true, userId);
                            this.mVivoPermissionManager.setAppPermission(packageName, VivoPermissionType.getVPType("android.permission.READ_SMS").getVPTypeId(), 1);
                            pms.updatePermissionFlags("android.permission.READ_SMS", packageName, 48, 48, true, userId);
                            this.mVivoPermissionManager.setAppPermission(packageName, VivoPermissionType.getVPType("android.permission.SEND_MMS").getVPTypeId(), 1);
                            this.mVivoPermissionManager.setAppPermission(packageName, VivoPermissionType.getVPType("android.permission.WRITE_SMS").getVPTypeId(), 1);
                            pms.updatePermissionFlags("android.permission.WRITE_SMS", packageName, 48, 48, true, userId);
                            this.mVivoPermissionManager.setAppPermission(packageName, VivoPermissionType.getVPType("android.permission.RECEIVE_SMS").getVPTypeId(), 1);
                            pms.updatePermissionFlags("android.permission.RECEIVE_SMS", packageName, 48, 48, true, userId);
                            this.mVivoPermissionManager.setAppPermission(packageName, VivoPermissionType.getVPType("android.permission.READ_CONTACTS").getVPTypeId(), 1);
                            pms.updatePermissionFlags("android.permission.READ_CONTACTS", packageName, 48, 48, true, userId);
                            this.mVivoPermissionManager.setAppPermission(packageName, VivoPermissionType.getVPType("android.permission.WRITE_CONTACTS").getVPTypeId(), 1);
                            pms.updatePermissionFlags("android.permission.WRITE_CONTACTS", packageName, 48, 48, true, userId);
                            this.mVivoPermissionManager.setAppPermission(packageName, VivoPermissionType.getVPType("android.permission.ACCESS_FINE_LOCATION").getVPTypeId(), 1);
                            pms.updatePermissionFlags("android.permission.ACCESS_FINE_LOCATION", packageName, 48, 48, true, userId);
                            this.mVivoPermissionManager.setAppPermission(packageName, VivoPermissionType.getVPType("android.permission.ACCESS_COARSE_LOCATION").getVPTypeId(), 1);
                            pms.updatePermissionFlags("android.permission.ACCESS_COARSE_LOCATION", packageName, 48, 48, true, userId);
                            this.mVivoPermissionManager.setAppPermission(packageName, VivoPermissionType.getVPType("android.permission.READ_CALENDAR").getVPTypeId(), 1);
                            pms.updatePermissionFlags("android.permission.READ_CALENDAR", packageName, 48, 48, true, userId);
                            this.mVivoPermissionManager.setAppPermission(packageName, VivoPermissionType.getVPType("android.permission.WRITE_CALENDAR").getVPTypeId(), 1);
                            pms.updatePermissionFlags("android.permission.WRITE_CALENDAR", packageName, 48, 48, true, userId);
                            this.mVivoPermissionManager.setAppPermission(packageName, VivoPermissionType.getVPType("android.permission.CAMERA").getVPTypeId(), 1);
                            pms.updatePermissionFlags("android.permission.CAMERA", packageName, 48, 48, true, userId);
                            this.mVivoPermissionManager.setAppPermission(packageName, VivoPermissionType.getVPType("android.permission.RECORD_AUDIO").getVPTypeId(), 1);
                            pms.updatePermissionFlags("android.permission.RECORD_AUDIO", packageName, 48, 48, true, userId);
                            this.mVivoPermissionManager.setAppPermission(packageName, VivoPermissionType.getVPType("android.permission.WRITE_EXTERNAL_STORAGE").getVPTypeId(), 1);
                            pms.updatePermissionFlags("android.permission.WRITE_EXTERNAL_STORAGE", packageName, 48, 48, true, userId);
                            this.mVivoPermissionManager.setAppPermission(packageName, VivoPermissionType.getVPType("android.permission.READ_EXTERNAL_STORAGE").getVPTypeId(), 1);
                            pms.updatePermissionFlags("android.permission.READ_EXTERNAL_STORAGE", packageName, 48, 48, true, userId);
                        } else {
                            pms.updatePermissionFlags("android.permission.SET_WALLPAPER", packageName, 48, 0, true, userId);
                            pms.updatePermissionFlags("android.permission.CALL_PHONE", packageName, 48, 0, true, userId);
                            pms.updatePermissionFlags("android.permission.READ_PHONE_STATE", packageName, 48, 0, true, userId);
                            pms.updatePermissionFlags("android.permission.READ_CALL_LOG", packageName, 48, 0, true, userId);
                            pms.updatePermissionFlags("android.permission.WRITE_CALL_LOG", packageName, 48, 0, true, userId);
                            pms.updatePermissionFlags("android.permission.SEND_SMS", packageName, 48, 0, true, userId);
                            pms.updatePermissionFlags("android.permission.READ_SMS", packageName, 48, 0, true, userId);
                            pms.updatePermissionFlags("android.permission.WRITE_SMS", packageName, 48, 0, true, userId);
                            pms.updatePermissionFlags("android.permission.RECEIVE_SMS", packageName, 48, 0, true, userId);
                            pms.updatePermissionFlags("android.permission.READ_CONTACTS", packageName, 48, 0, true, userId);
                            pms.updatePermissionFlags("android.permission.WRITE_CONTACTS", packageName, 48, 0, true, userId);
                            pms.updatePermissionFlags("android.permission.ACCESS_FINE_LOCATION", packageName, 48, 0, true, userId);
                            pms.updatePermissionFlags("android.permission.ACCESS_COARSE_LOCATION", packageName, 48, 0, true, userId);
                            pms.updatePermissionFlags("android.permission.READ_CALENDAR", packageName, 48, 0, true, userId);
                            pms.updatePermissionFlags("android.permission.WRITE_CALENDAR", packageName, 48, 0, true, userId);
                            pms.updatePermissionFlags("android.permission.CAMERA", packageName, 48, 0, true, userId);
                            pms.updatePermissionFlags("android.permission.RECORD_AUDIO", packageName, 48, 0, true, userId);
                            pms.updatePermissionFlags("android.permission.WRITE_EXTERNAL_STORAGE", packageName, 48, 0, true, userId);
                            pms.updatePermissionFlags("android.permission.READ_EXTERNAL_STORAGE", packageName, 48, 0, true, userId);
                        }
                        updateReadInstalledAppStatus(packageName, false);
                        grantNotificationListener(packageName, userId);
                    }
                } catch (Exception e) {
                    VLog.w(VIVO_LOG_TAG, "failed grantAppsPermisions packageName = " + packageName, e);
                }
            }
        }
    }

    private boolean setAppPermissionWhiteList(ComponentName admin, int listId, List<String> keys, boolean isAdd, int userHandle) {
        PackageInfo packageInfo;
        checkCallingEmmPermission(admin, "EMM_APP_PERMISSION", userHandle);
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("IllegalArgumentException: setAppPermissionWhiteList para is illegal!");
        }
        VLog.w(VIVO_LOG_TAG, "setAppPermissionWhiteList admin = " + admin + ", keys = " + keys + ", isAdd = " + isAdd);
        synchronized (getLockObject()) {
            try {
            } catch (Throwable th) {
                th = th;
            }
            try {
                VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
                int i = 0;
                if (ap != null) {
                    long callingId = Binder.clearCallingIdentity();
                    try {
                        List<String> successPkgs = new ArrayList<>();
                        AppsPermisionsThread localThread = new AppsPermisionsThread(keys, userHandle, isAdd);
                        localThread.start();
                        for (String packageName : keys) {
                            if (packageName != null) {
                                if (isAdd) {
                                    try {
                                        packageInfo = this.mContext.getPackageManager().getPackageInfoAsUser(packageName, i, userHandle);
                                        VLog.w(VIVO_LOG_TAG, "setAppPermissionWhiteList admin = " + admin + ", packageName = " + packageName + ", packageInfo = " + packageInfo);
                                    } catch (PackageManager.NameNotFoundException e) {
                                        VLog.w(VIVO_LOG_TAG, "failed setAppPermissionWhiteList packageName = " + packageName, e);
                                    }
                                    if (packageInfo == null) {
                                        i = 0;
                                    }
                                }
                                successPkgs.add(packageName);
                            }
                            i = 0;
                        }
                        if (!successPkgs.isEmpty()) {
                            if (ap.appPermissionWlist == null) {
                                ap.appPermissionWlist = new ArrayList();
                            }
                            VLog.w(VIVO_LOG_TAG, "setAppPermissionWhiteList admin = " + admin + ", successPkgs = " + successPkgs + ", isAdd = " + isAdd);
                            VivoUtils.addItemsFromList(ap.appPermissionWlist, successPkgs, isAdd);
                            try {
                                try {
                                    saveVivoSettingsLocked(listId, userHandle);
                                    Binder.restoreCallingIdentity(callingId);
                                    return true;
                                } catch (Throwable th2) {
                                    e = th2;
                                    Binder.restoreCallingIdentity(callingId);
                                    throw e;
                                }
                            } catch (Exception e2) {
                                e = e2;
                                VLog.w(VIVO_LOG_TAG, "failed setAppPermissionWhiteList ", e);
                                Binder.restoreCallingIdentity(callingId);
                                return false;
                            }
                        }
                        Binder.restoreCallingIdentity(callingId);
                    } catch (Exception e3) {
                        e = e3;
                    } catch (Throwable th3) {
                        e = th3;
                        Binder.restoreCallingIdentity(callingId);
                        throw e;
                    }
                }
                return false;
            } catch (Throwable th4) {
                th = th4;
                throw th;
            }
        }
    }

    private List<String> getAppPermissionWhiteList(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            if (ap != null) {
                if (this.mPermissionListPkgs != null) {
                    List<String> all = new ArrayList<>(this.mPermissionListPkgs);
                    if (ap.appPermissionWlist != null && ap.appPermissionWlist.size() > 0) {
                        all.addAll(ap.appPermissionWlist);
                    }
                    return all;
                }
                return ap.appPermissionWlist;
            }
            return this.mPermissionListPkgs;
        }
    }

    private boolean setAppAlarmWhiteList(ComponentName admin, int listId, List<String> keys, boolean isAdd, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_APP_ALARM_PERMISSION", userHandle);
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("IllegalArgumentException: setAppAlarmWhiteList para is illegal!");
        }
        synchronized (getLockObject()) {
            try {
            } catch (Throwable th) {
                th = th;
            }
            try {
                VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
                if (ap != null) {
                    List<String> relatedPkgs = getEmmRelatedPkgs(admin.getPackageName(), userHandle);
                    List<String> list = new ArrayList<>();
                    long callingId = Binder.clearCallingIdentity();
                    try {
                        for (String pkg : keys) {
                            if ((relatedPkgs == null || !relatedPkgs.contains(pkg)) && !pkg.equals(admin.getPackageName())) {
                                Binder.restoreCallingIdentity(callingId);
                                return false;
                            }
                            list.add(pkg);
                            VLog.d(VIVO_LOG_TAG, "set alarm white app: " + pkg + ", add = " + isAdd);
                        }
                        if (ap.appAlarmWlist == null) {
                            ap.appAlarmWlist = new ArrayList();
                        }
                        if (list.size() > 0) {
                            VivoUtils.addItemsFromList(ap.appAlarmWlist, list, isAdd);
                            try {
                                try {
                                    saveVivoSettingsLocked(listId, userHandle);
                                    Binder.restoreCallingIdentity(callingId);
                                    return true;
                                } catch (Throwable th2) {
                                    e = th2;
                                    Binder.restoreCallingIdentity(callingId);
                                    throw e;
                                }
                            } catch (Exception e) {
                                e = e;
                                VLog.w(VIVO_LOG_TAG, "failed setAppAlarmWhiteList ", e);
                                Binder.restoreCallingIdentity(callingId);
                                return false;
                            }
                        }
                        Binder.restoreCallingIdentity(callingId);
                    } catch (Exception e2) {
                        e = e2;
                    } catch (Throwable th3) {
                        e = th3;
                        Binder.restoreCallingIdentity(callingId);
                        throw e;
                    }
                }
                return false;
            } catch (Throwable th4) {
                th = th4;
                throw th;
            }
        }
    }

    private List<String> getAppAlarmWhiteList(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            if (ap != null) {
                return ap.appAlarmWlist;
            }
            return null;
        }
    }

    private boolean setAppNotificationListenerList(ComponentName admin, int listId, List<String> keys, boolean isAdd, int userHandle) {
        checkCallingEmmPermission(admin, "EMM_APP_PERMISSION", userHandle);
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("IllegalArgumentException: setAppNotificationListenerList para is illegal!");
        }
        synchronized (getLockObject()) {
            try {
            } catch (Throwable th) {
                th = th;
            }
            try {
                VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
                if (ap != null) {
                    long callingId = Binder.clearCallingIdentity();
                    try {
                        List<String> successList = new ArrayList<>();
                        if (isAdd) {
                            for (String info : keys) {
                                ComponentName comp = ComponentName.unflattenFromString(info);
                                ServiceInfo service = this.mIPackageManager.getServiceInfo(comp, (int) KernelConfig.WAIT_VSYNC, userHandle);
                                VLog.i(VIVO_LOG_TAG, "setAppNotificationListenerList info = " + info + ", service = " + service);
                                if (service != null && "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE".equals(service.permission)) {
                                    if (this.mNotificationManager == null) {
                                        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
                                    }
                                    this.mNotificationManager.setNotificationListenerAccessGranted(comp, true);
                                    successList.add(info);
                                }
                            }
                        } else {
                            successList.addAll(keys);
                        }
                        if (ap.appNotificationListenerlist == null) {
                            ap.appNotificationListenerlist = new ArrayList();
                        }
                        VivoUtils.addItemsFromList(ap.appNotificationListenerlist, successList, isAdd);
                    } catch (Exception e) {
                        e = e;
                    } catch (Throwable th2) {
                        e = th2;
                        Binder.restoreCallingIdentity(callingId);
                        throw e;
                    }
                    try {
                        try {
                            saveVivoSettingsLocked(listId, userHandle);
                            Binder.restoreCallingIdentity(callingId);
                            return true;
                        } catch (Throwable th3) {
                            e = th3;
                            Binder.restoreCallingIdentity(callingId);
                            throw e;
                        }
                    } catch (Exception e2) {
                        e = e2;
                        VLog.w(VIVO_LOG_TAG, "failed setAppNotificationListenerList ", e);
                        Binder.restoreCallingIdentity(callingId);
                        return false;
                    }
                }
                return false;
            } catch (Throwable th4) {
                th = th4;
                throw th;
            }
        }
    }

    private List<String> getAppNotificationListenerList(ComponentName admin, int userHandle) {
        VivoActiveAdmin ap;
        synchronized (getLockObject()) {
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            if (ap != null) {
                return ap.appNotificationListenerlist;
            }
            return null;
        }
    }

    private Bundle getRomVersionInfo(ComponentName admin, int userHandle) {
        synchronized (getLockObject()) {
            Bundle bundle = new Bundle();
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                String romVersion = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + FtBuild.getProductVersion();
                bundle.putString("state_string", romVersion);
                return bundle;
            }
            return null;
        }
    }

    private Bundle getBrowserHistory(ComponentName admin, int userHandle) {
        Bundle bundle;
        VivoActiveAdmin ap;
        checkCallingEmmPermission(admin, "EMM_OPERATION", userHandle);
        synchronized (getLockObject()) {
            bundle = new Bundle();
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            if (ap != null) {
                long callingId = Binder.clearCallingIdentity();
                try {
                    bundle.putStringArrayList("state_string", getVisitedHistory(this.mContentResolver));
                    Binder.restoreCallingIdentity(callingId);
                } catch (Exception e) {
                    e.printStackTrace();
                    Binder.restoreCallingIdentity(callingId);
                }
            }
        }
        return bundle;
    }

    /* JADX WARN: Code restructure failed: missing block: B:14:0x0033, code lost:
        if (r0 != null) goto L17;
     */
    /* JADX WARN: Code restructure failed: missing block: B:15:0x0035, code lost:
        r0.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:20:0x0044, code lost:
        if (r0 == null) goto L18;
     */
    /* JADX WARN: Code restructure failed: missing block: B:22:0x0047, code lost:
        return r1;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private static java.util.ArrayList<java.lang.String> getVisitedHistory(android.content.ContentResolver r9) {
        /*
            r0 = 0
            java.util.ArrayList r1 = new java.util.ArrayList
            r1.<init>()
            java.lang.String r2 = "url"
            java.lang.String[] r5 = new java.lang.String[]{r2}     // Catch: java.lang.Throwable -> L39 java.lang.Exception -> L3b
            android.net.Uri r4 = com.android.server.devicepolicy.VivoCustomDpmsImpl.CONTENT_URI     // Catch: java.lang.Throwable -> L39 java.lang.Exception -> L3b
            java.lang.String r6 = "visits > 0"
            r7 = 0
            r8 = 0
            r3 = r9
            android.database.Cursor r2 = r3.query(r4, r5, r6, r7, r8)     // Catch: java.lang.Throwable -> L39 java.lang.Exception -> L3b
            r0 = r2
            if (r0 != 0) goto L21
        L1b:
            if (r0 == 0) goto L20
            r0.close()
        L20:
            return r1
        L21:
            r2 = 0
        L22:
            boolean r3 = r0.moveToNext()     // Catch: java.lang.Throwable -> L39 java.lang.Exception -> L3b
            if (r3 == 0) goto L33
            r3 = 0
            java.lang.String r3 = r0.getString(r3)     // Catch: java.lang.Throwable -> L39 java.lang.Exception -> L3b
            r1.add(r3)     // Catch: java.lang.Throwable -> L39 java.lang.Exception -> L3b
            int r2 = r2 + 1
            goto L22
        L33:
            if (r0 == 0) goto L47
        L35:
            r0.close()
            goto L47
        L39:
            r2 = move-exception
            goto L48
        L3b:
            r2 = move-exception
            java.lang.String r3 = "VDPMS"
            java.lang.String r4 = "getVisitedHistory"
            android.util.Log.e(r3, r4, r2)     // Catch: java.lang.Throwable -> L39
            if (r0 == 0) goto L47
            goto L35
        L47:
            return r1
        L48:
            if (r0 == 0) goto L4d
            r0.close()
        L4d:
            throw r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.devicepolicy.VivoCustomDpmsImpl.getVisitedHistory(android.content.ContentResolver):java.util.ArrayList");
    }

    private Bundle getSdkInfo(ComponentName admin, int userHandle) {
        Bundle bundle;
        synchronized (getLockObject()) {
            bundle = new Bundle();
            boolean available = false;
            String customname = "UnKnown";
            int callingUid = Binder.getCallingUid();
            if (isVivoEmmUid(callingUid)) {
                available = true;
            }
            if (!CUSTOM_SHORT_NAME_DEFAULT.equals(this.mEmmShortName)) {
                customname = this.mEmmShortName;
            } else if (this.mCustomType == 1) {
                customname = "debug";
            }
            bundle.putString("state_string", "sdkersion:V2.0 customname:" + customname + " available:" + available);
        }
        return bundle;
    }

    private Bundle getDeviceInfo(ComponentName admin, int userHandle) {
        synchronized (getLockObject()) {
            try {
            } catch (Throwable th) {
                th = th;
            }
            try {
                Bundle bundle = new Bundle();
                VivoActiveAdmin ap = admin != null ? getVivoActiveAdmin(admin, userHandle) : getVivoUserData(userHandle).mVivoActiveAdmin;
                if (ap != null) {
                    long callingId = Binder.clearCallingIdentity();
                    try {
                        String ram = this.mDeviceInfo.getRamSize() + "GB";
                        String rom = this.mDeviceInfo.getRomSize() + "GB";
                        String screen = this.mDeviceInfo.getScreenWidth() + "*" + this.mDeviceInfo.getScreenHeigh() + " ";
                        String manufa = this.mDeviceInfo.getManufacturer() + " ";
                        String model = this.mDeviceInfo.getModel() + " ";
                        String kernel = this.mDeviceInfo.getKernelNum() + " ";
                        String software = this.mDeviceInfo.getSoftwareNum() + " ";
                        bundle.putString("state_string", "ram:" + ram + " rom:" + rom + " screen:" + screen + " manufa:" + manufa + " model:" + model + " kernel:" + kernel + " software:" + software);
                        Binder.restoreCallingIdentity(callingId);
                        return bundle;
                    } catch (Exception e) {
                        e.printStackTrace();
                        Binder.restoreCallingIdentity(callingId);
                    }
                }
                return null;
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    private Bundle getTopAppPackage(ComponentName admin, int userHandle) {
        Bundle bundle;
        VivoActiveAdmin ap;
        synchronized (getLockObject()) {
            bundle = new Bundle();
            if (admin != null) {
                ap = getVivoActiveAdmin(admin, userHandle);
            } else {
                ap = getVivoUserData(userHandle).mVivoActiveAdmin;
            }
            if (ap != null) {
                long callingId = Binder.clearCallingIdentity();
                try {
                    String topPkg = this.mActivityManager.getRunningTasks(1).get(0).topActivity.getClassName();
                    bundle.putString("package_name", topPkg);
                    Binder.restoreCallingIdentity(callingId);
                } catch (Exception e) {
                    e.printStackTrace();
                    Binder.restoreCallingIdentity(callingId);
                }
            }
        }
        return bundle;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class DeviceInfo {
        private WindowManager windowManager;
        private long ramSize = 0;
        private long romSize = 0;
        private int romSizetoGB = 0;
        private int returnRomSize = 0;
        private int screenWidth = 0;
        private int screenHeigh = 0;
        private String manufacturer = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        private String model = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        private String kernelNum = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        private String softwareNum = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        private DisplayMetrics dm = new DisplayMetrics();

        public DeviceInfo() {
            this.windowManager = (WindowManager) VivoCustomDpmsImpl.this.mContext.getSystemService("window");
            initDeviceInfo();
        }

        void initDeviceInfo() {
            this.manufacturer = SystemProperties.get("ro.product.vendor.manufacturer", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
            this.model = SystemProperties.get("ro.product.model", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
            this.kernelNum = getKernelNum();
            this.softwareNum = getBuildNumber();
        }

        private String getBuildNumber() {
            String version = SystemProperties.get("ro.build.version.bbk", Build.DISPLAY);
            String customize_bbk = SystemProperties.get("ro.product.customize.bbk", "N");
            if (version.indexOf("_") >= 0) {
                if (customize_bbk.equals("CN-YD")) {
                    return version.replaceFirst("_", "-YD_");
                }
                if (customize_bbk.equals("CN-DX")) {
                    return version.replaceFirst("_", "-DX_");
                }
                if (customize_bbk.equals("CN-YD-A")) {
                    return version.replaceFirst("_", "-YD-A_");
                }
                if (customize_bbk.equals("CN-YD-B")) {
                    return version.replaceFirst("_", "-YD-B_");
                }
                return version;
            }
            return version;
        }

        public int getRamSize() {
            try {
                ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                VivoCustomDpmsImpl.this.mActivityManager.getMemoryInfo(memoryInfo);
                long j = memoryInfo.totalMem;
                this.ramSize = j;
                if (0 != j) {
                    return (int) Math.ceil(Long.valueOf(j).longValue() / 1.073741824E9d);
                }
                return 0;
            } catch (Exception e) {
                Log.d(VivoCustomDpmsImpl.VIVO_LOG_TAG, "getRamSize fail:" + e.toString());
                return 0;
            }
        }

        public int getRomSize() {
            try {
                StatFs fs = new StatFs(Environment.getDataDirectory().getPath());
                long blockCountLong = fs.getBlockCountLong() * fs.getBlockSizeLong();
                this.romSize = blockCountLong;
                if (0 != blockCountLong) {
                    int ceil = (int) Math.ceil(Long.valueOf(blockCountLong).longValue() / 1.073741824E9d);
                    this.romSizetoGB = ceil;
                    if (ceil > 512) {
                        this.returnRomSize = Consts.ProcessStates.FOCUS;
                    } else if (ceil > 256) {
                        this.returnRomSize = 512;
                    } else if (ceil > 128) {
                        this.returnRomSize = 256;
                    } else if (ceil > 64) {
                        this.returnRomSize = 128;
                    } else if (ceil > 32) {
                        this.returnRomSize = 64;
                    } else if (ceil > 16) {
                        this.returnRomSize = 32;
                    } else if (ceil > 8) {
                        this.returnRomSize = 16;
                    } else if (ceil > 4) {
                        this.returnRomSize = 8;
                    } else if (ceil > 2) {
                        this.returnRomSize = 4;
                    } else if (ceil > 1) {
                        this.returnRomSize = 2;
                    } else {
                        this.returnRomSize = 1;
                    }
                    return this.returnRomSize;
                }
                return 0;
            } catch (Exception e) {
                Log.d(VivoCustomDpmsImpl.VIVO_LOG_TAG, "getRomSize fail:" + e.toString());
                return 0;
            }
        }

        public int getScreenWidth() {
            try {
                this.windowManager.getDefaultDisplay().getMetrics(this.dm);
                this.screenWidth = this.dm.widthPixels;
            } catch (Exception e) {
                Log.d(VivoCustomDpmsImpl.VIVO_LOG_TAG, "getScreenWidth fail:" + e.toString());
            }
            return this.screenWidth;
        }

        public int getScreenHeigh() {
            try {
                this.windowManager.getDefaultDisplay().getMetrics(this.dm);
                this.screenHeigh = this.dm.heightPixels;
            } catch (Exception e) {
                Log.d(VivoCustomDpmsImpl.VIVO_LOG_TAG, "getScreenHeigh fail:" + e.toString());
            }
            return this.screenHeigh;
        }

        public String getAndroidVersion() {
            return Build.VERSION.RELEASE;
        }

        public String getManufacturer() {
            return this.manufacturer;
        }

        public String getModel() {
            return this.model;
        }

        public String getKernelNum() {
            try {
                BufferedReader reader = new BufferedReader(new FileReader("/proc/version"), 256);
                String procVersionStr = reader.readLine();
                reader.close();
                Pattern p = Pattern.compile("\\w+\\s+\\w+\\s+([^\\s]+)\\s+\\(([^\\s@]+(?:@[^\\s.]+)?)[^)]*\\)\\s+\\((?:[^(]*\\([^)]*\\))?[^)]*\\)\\s+([^\\s]+)\\s+(?:PREEMPT\\s+)?(.+)");
                Matcher m = p.matcher(procVersionStr);
                if (!m.matches()) {
                    Log.e(VivoCustomDpmsImpl.VIVO_LOG_TAG, "Regex did not match on /proc/version: " + procVersionStr);
                    return "Unavailable";
                } else if (m.groupCount() < 4) {
                    Log.e(VivoCustomDpmsImpl.VIVO_LOG_TAG, "Regex match on /proc/version only returned " + m.groupCount() + " groups");
                    return "Unavailable";
                } else {
                    return new StringBuilder(m.group(1)).toString();
                }
            } catch (IOException e) {
                Log.e(VivoCustomDpmsImpl.VIVO_LOG_TAG, "IO Exception when getting kernel version for Device Info screen", e);
                return "Unavailable";
            }
        }

        public String getSoftwareNum() {
            return this.softwareNum;
        }
    }

    private boolean setUserMenuPolicy(ComponentName admin, int polId, int policy, int userHandle) {
        if (policy < 0 || policy > 1) {
            throw new IllegalArgumentException("IllegalArgumentException: input is illegal!");
        }
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap != null) {
                if (1 == this.mCustomMultiUser && ap.userMenuSupport == 0) {
                    ap.userMenuSupport = 1;
                    saveVivoSettingsLocked(polId, userHandle);
                }
                VLog.i(VIVO_LOG_TAG, "setUserMenuPolicy support = " + ap.userMenuSupport);
                if (1 == ap.userMenuSupport) {
                    if (policy == 0) {
                        SystemProperties.set("persist.vivo.multiuser", "1");
                        SystemProperties.set("persist.vivo.custom.multiuser", "1");
                    } else {
                        SystemProperties.set("persist.vivo.multiuser", "0");
                        SystemProperties.set("persist.vivo.custom.multiuser", "0");
                    }
                }
                return true;
            }
            return false;
        }
    }

    private int getUserMenuPolicy(ComponentName admin, int userHandle) {
        synchronized (getLockObject()) {
            VivoActiveAdmin ap = getVivoActiveAdmin(admin, userHandle);
            if (ap == null || 1 != ap.userMenuSupport) {
                return 1;
            }
            int multiuser = SystemProperties.getInt("persist.vivo.multiuser", 0);
            int customMultiuser = SystemProperties.getInt("persist.vivo.custom.multiuser", 0);
            return (multiuser == 0 && customMultiuser == 0) ? 1 : 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleExceptionInfo(int userHandle) {
        if (getCustomType() <= 0 || this.mIsOverseas) {
            return;
        }
        VLog.v(VIVO_LOG_TAG, "handleExceptionInfo");
        synchronized (getLockObject()) {
            VivoPolicyData policy = getVivoUserData(userHandle);
            if (policy != null) {
                try {
                    if (policy.mAlarmExceptionInfolist != null && policy.mAlarmExceptionInfolist.size() > 0) {
                        for (String s : policy.mAlarmExceptionInfolist) {
                            String[] info = s.split(":");
                            reportVcodeAlarm(info[0], info[1]);
                        }
                        policy.mAlarmExceptionInfolist.clear();
                    }
                    if (policy.mLocationExceptionInfolist != null && policy.mLocationExceptionInfolist.size() > 0) {
                        for (String s2 : policy.mLocationExceptionInfolist) {
                            String[] info2 = s2.split(":");
                            reportVcodeLocation(info2[0], info2[1]);
                        }
                        policy.mLocationExceptionInfolist.clear();
                    }
                    if (policy.mPowerExceptionInfolist != null && policy.mPowerExceptionInfolist.size() > 0) {
                        for (String s3 : policy.mPowerExceptionInfolist) {
                            String[] info3 = s3.split(":");
                            double value = Double.parseDouble(info3[1]);
                            reportVcodePower(info3[0], String.format("%.6f", Double.valueOf(value)), info3[2], info3[3]);
                        }
                        policy.mPowerExceptionInfolist.clear();
                    }
                    if (policy.mWakeLockExceptionInfolist != null && policy.mWakeLockExceptionInfolist.size() > 0) {
                        for (String s4 : policy.mWakeLockExceptionInfolist) {
                            String[] info4 = s4.split(":");
                            if (Long.parseLong(info4[1]) > 1000) {
                                double time = Long.parseLong(info4[1]) / 1000.0d;
                                reportVcodeWakeLock(info4[0], String.format("%.2f", Double.valueOf(time)));
                            }
                        }
                        policy.mWakeLockExceptionInfolist.clear();
                    }
                    saveEmmInfo(policy, userHandle);
                } catch (Exception e) {
                    VLog.e(VIVO_LOG_TAG, "handleExceptionInfo failed: " + e.getMessage());
                }
            }
        }
        scheduleAlarms(userHandle);
    }

    private void initVcode(int userId) {
        if (getCustomType() <= 0) {
            return;
        }
        VLog.v(VIVO_LOG_TAG, "initVcode");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_EMM_VCODE);
        this.mContext.registerReceiver(this.mVcodeReceiver, intentFilter);
    }

    private void scheduleAlarms(int userId) {
        VLog.v(VIVO_LOG_TAG, "scheduleAlarms");
        try {
            if (this.mAlarmManager == null) {
                this.mAlarmManager = (AlarmManager) this.mContext.getSystemService(AlarmManager.class);
            }
            Intent i = new Intent(ACTION_EMM_VCODE);
            PendingIntent pi = PendingIntent.getBroadcastAsUser(this.mContext, 0, i, 0, UserHandle.of(userId));
            this.mAlarmManager.set(2, SystemClock.elapsedRealtime() + 86400000, pi);
        } catch (Exception e) {
            VLog.e(VIVO_LOG_TAG, "scheduleAlarms failed: " + e.getMessage());
        }
    }

    private void reportVcodeInvoke(ComponentName admin, int poId) {
        if (this.mIsOverseas) {
            return;
        }
        synchronized (getLockObject()) {
            try {
            } catch (Exception e) {
                VLog.e(VIVO_LOG_TAG, "reportVcodeInvoke failed: " + e.getMessage());
            }
            if (admin == null) {
                VLog.w(VIVO_LOG_TAG, "reportVcodeInvoke admin is null, return");
                return;
            }
            String pkg = admin.getPackageName();
            HashMap<String, String> data = new HashMap<>(2);
            data.put("c_id", Integer.toString(this.mCustomType));
            data.put("fuc_id", Integer.toString(poId));
            data.put("package", pkg);
            VcodeUtils.report(VcodeUtils.TYPE_INVOKE, data);
        }
    }

    private void reportVcodeException(String pkg, int type) {
        if (this.mIsOverseas) {
            return;
        }
        synchronized (getLockObject()) {
            try {
                HashMap<String, String> data = new HashMap<>(2);
                data.put("c_id", Integer.toString(this.mCustomType));
                data.put("package", pkg);
                data.put("err_type", Integer.toString(type));
                VcodeUtils.report(VcodeUtils.TYPE_EXCEPTION, data);
            } catch (Exception e) {
                VLog.e(VIVO_LOG_TAG, "reportVcodeException failed: " + e.getMessage());
            }
        }
    }

    private void reportVcodeAlarm(String pkg, String count) {
        if (this.mIsOverseas) {
            return;
        }
        synchronized (getLockObject()) {
            try {
                HashMap<String, String> data = new HashMap<>(2);
                data.put("c_id", Integer.toString(this.mCustomType));
                data.put("package", pkg);
                data.put("count", count);
                VcodeUtils.report(VcodeUtils.TYPE_ALARM, data);
            } catch (Exception e) {
                VLog.e(VIVO_LOG_TAG, "reportVcodeAlarm failed: " + e.getMessage());
            }
        }
    }

    private void reportVcodeLocation(String pkg, String count) {
        if (this.mIsOverseas) {
            return;
        }
        synchronized (getLockObject()) {
            try {
                HashMap<String, String> data = new HashMap<>(2);
                data.put("c_id", Integer.toString(this.mCustomType));
                data.put("package", pkg);
                data.put("count", count);
                VcodeUtils.report(VcodeUtils.TYPE_LOCATION, data);
            } catch (Exception e) {
                VLog.e(VIVO_LOG_TAG, "reportVcodeLocation failed: " + e.getMessage());
            }
        }
    }

    private void reportVcodePower(String pkg, String value, String isKill, String count) {
        if (this.mIsOverseas) {
            return;
        }
        synchronized (getLockObject()) {
            try {
                HashMap<String, String> data = new HashMap<>(2);
                data.put("c_id", Integer.toString(this.mCustomType));
                data.put("package", pkg);
                data.put("count", count);
                data.put("is_kill", isKill);
                data.put("cap_avg", value);
                VcodeUtils.report(VcodeUtils.TYPE_POWER, data);
            } catch (Exception e) {
                VLog.e(VIVO_LOG_TAG, "reportVcodePower failed: " + e.getMessage());
            }
        }
    }

    private void reportVcodeWakeLock(String pkg, String time) {
        if (this.mIsOverseas) {
            return;
        }
        synchronized (getLockObject()) {
            try {
                HashMap<String, String> data = new HashMap<>(2);
                data.put("c_id", Integer.toString(this.mCustomType));
                data.put("package", pkg);
                data.put("locktime_sum", time);
                VcodeUtils.report(VcodeUtils.TYPE_WAKELOCK, data);
            } catch (Exception e) {
                VLog.e(VIVO_LOG_TAG, "reportVcodeWakeLock failed: " + e.getMessage());
            }
        }
    }
}