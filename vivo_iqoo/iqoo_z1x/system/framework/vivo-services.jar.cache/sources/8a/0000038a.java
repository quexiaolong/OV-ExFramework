package com.android.server.pm;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageInstallObserver2;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.FtBuild;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManagerInternal;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Base64;
import android.util.FtDeviceInfo;
import android.util.FtFeature;
import android.util.LogPrinter;
import android.util.Slog;
import android.util.Xml;
import android.util.jar.StrictJarFile;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.XmlUtils;
import com.android.server.LocalServices;
import com.android.server.am.EmergencyBroadcastManager;
import com.android.server.devicepolicy.utils.VivoUtils;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.VivoPKMSUtils;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.parsing.pkg.ParsedPackage;
import com.android.server.policy.VivoPolicyConstant;
import com.vivo.common.VivoCollectData;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import com.vivo.services.proxy.broadcast.BroadcastConfigs;
import com.vivo.services.rms.ProcessList;
import com.vivo.services.security.server.VivoPermissionUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;
import vivo.util.VSlog;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class VivoPKMSUtils {
    private static final int AFTER_BOOT_REFRESH_INTERVAL = 5000;
    public static final String ANDROID_MANIFEST_FILENAME = "AndroidManifest.xml";
    private static final String BEFORE = "-";
    private static final String BIT32 = "32";
    private static final String BIT64 = "64";
    private static final String BLACKLIST_FILE_PATH = "/oem/blackapp.list";
    private static final String CONFIG_MODULE_NAME_ADB_SILENT_INSTALL = "adbSilentInstallConfig";
    private static final String CONFIG_MODULE_NAME_BASE_APK = "sys_base_apk_list";
    private static final String CONFIG_MODULE_NAME_COMMON_CONFIG = "pkmsCommonConfig";
    private static final String CONFIG_UPDATE_ACTION_ADB_SILENT_INSTALL = "com.vivo.daemonService.unifiedconfig.update_finish_broadcast_adbSilentInstallConfig";
    private static final String CONFIG_UPDATE_ACTION_BASE_APK = "com.vivo.daemonService.unifiedconfig.update_finish_broadcast_sys_base_apk_list";
    private static final String CONFIG_UPDATE_ACTION_COMMON_CONFIG = "com.vivo.daemonService.unifiedconfig.update_finish_broadcast_pkmsCommonConfig";
    private static final String CONFIG_URI = "content://com.vivo.abe.unifiedconfig.provider/configs";
    private static final String CONFIG_VERSION_BASE_APK_LIST = "1.0";
    private static final String CONFIG_VERSION_DEFAULT = "1.0";
    private static final String CT_EMM_CERT_TAG = "EmmCert";
    private static final boolean DEBUG_SYS_APP_DEL = true;
    private static int DEFAULT_MAX_RECORD_COUNT = 0;
    private static final int DELAY_INTERVAL = 1000;
    private static final String DEVICE_BOOT_INFO = "/data/bbkcore/framework_pkms/deviceBootInfo.xml";
    private static final String DEVICE_VERSION;
    private static final String EXCEPT = "!";
    private static final long FORBID_BRUSH_TOOLS_OVERTIME = 1296000;
    private static final long FORBID_DESKTOP_APP_OVERTIME = 604800;
    private static final String FUNTOUCH = "Funtouch";
    private static final String INSTALL_APK_INFO_PATH = "/data/bbkcore/framework_pkms/installApkInfo.xml";
    private static final String INSTALL_APK_SEQ_NUM_PATH = "/data/bbkcore/framework_pkms/installApkSeqNum.xml";
    private static final String LAST = "+";
    private static final String METAINF_EMMCER = "META-INF/VIVOEMM.CER";
    private static final String METAINF_MANIFEST = "META-INF/MANIFEST.MF";
    private static final String META_DATA_NOT_ADB_UNINSTALL = "vivo.uninstall.compatibility.not_adbUninstall";
    private static final String META_DATA_SUPPORT_ABI = "vivo.install.compatibility.abi";
    private static final String META_DATA_SUPPORT_ANDROID_VERSION = "vivo.install.compatibility.android_version";
    private static final String META_DATA_SUPPORT_DEVICETYPE = "vivo.install.compatibility.deviceType";
    private static final String META_DATA_SUPPORT_NOT_INSTALL = "vivo.install.compatibility.not_install";
    private static final String META_DATA_SUPPORT_OS_NAME = "vivo.install.compatibility.os_name";
    private static final String META_DATA_SUPPORT_OVERSEAS = "vivo.install.compatibility.overseas";
    private static final String META_DATA_SUPPORT_ROM_VERSION = "vivo.install.compatibility.%s.rom_version";
    private static final int MSG_CHECK_DEVICE_IS_BATCH_INSTALL_APK = 3000;
    private static final int MSG_DELAY_INIT_DEVICE_CODE = 2002;
    private static final int MSG_INIT_CONFIG = 1000;
    private static final int MSG_INIT_SEQ_NUM_FROM_LOCAL_FILE = 3001;
    private static final int MSG_REFRESH_BASE_APK_LIST_CONFIG = 1003;
    private static final int MSG_VCD_APK_INSTALL_LOG = 2000;
    private static final int MSG_VCD_APK_UN_INSTALL_LOG = 2001;
    private static final String NOT_INSTALL = "1";
    private static final String NOT_OVERSEAS = "0";
    private static final String OVERSEAS = "1";
    static final int PERMISSION_PRE_CHECK_ALLOW = 0;
    static final int PERMISSION_PRE_CHECK_NOT_ALLOW = -100;
    private static final String PUBLIC_KEY = "/system/etc/emm/emm_publickey.pem";
    private static final int RETRY_INTERVAL = 180000;
    private static final String SPLIT_FLAG = "\\|";
    private static final String SYSTEM_FIXED_PKG_INFO = "systemFixedPkgInfo.xml";
    protected static final String TAG = "VivoPKMSUtils";
    private static final String TAG_PACKAGE = "pkg";
    private static final String TAG_RUNTIME_PERMISSIONS = "runtime-permissions";
    private static final String UN_INSTALL_APK_INFO_PATH = "/data/bbkcore/framework_pkms/unInstallApkInfo.xml";
    private static final String UN_INSTALL_APK_SEQ_NUM_PATH = "/data/bbkcore/framework_pkms/unInstallApkSeqNum.xml";
    private static final String UPDATE_APK_INFO_PATH = "/data/bbkcore/framework_pkms/updateApkInfo.xml";
    private static final String UPDATE_APK_SEQ_NUM_PATH = "/data/bbkcore/framework_pkms/updateApkSeqNum.xml";
    private static final long VCD_INFO_FILE_TOTAL_SIZE = 5242880;
    private static boolean VCD_INFO_TO_LOCAL_OPEN = false;
    private static final String VOS = "vos";
    private static final int WIFI_LIST_SIZE = 5;
    private static final int WIFI_SCAN_GET_INTERVEL = 180000;
    private static byte[] base64DecodeChars;
    private static char[] base64EncodeChars;
    private static AdbVerifyConfig mAdbUninstallVerifyConfig;
    private static final String mCurrentOsName;
    private static final float mCurrentRomVersion;
    private static final boolean mIsBIT64Platform;
    private boolean DEBUG_FOR_ALL;
    private boolean DEBUG_PERMISSION;
    private boolean DEBUG_PREFERRED = PackageManagerService.DEBUG_PREFERRED;
    public ArrayList<EmmPackage> emmPackageCacheList;
    private ArrayList<String> mAdbUninstallBlackList;
    private ArrayList<String> mAppFilterPermissionList;
    ArrayList<String> mBuiltIn3PartApkNameList;
    private ArrayList<String> mBuiltIn3PartAppPathList;
    private Context mContext;
    private String mDeviceI;
    private String mDeviceID;
    private long mForbidBrushToolsOverTime;
    private HashMap<String, ForbidDeskTopAppInfo> mForbidDesktopAppBlackMap;
    private long mForbidDestTopAppOverTime;
    private Object mForbidObject;
    private ArrayList<String> mForbidUsbBrushAppBlackList;
    private ArrayList<String> mForbidUsbBrushAppSignatureList;
    private String mForbidUsbBrushAppWarningToast;
    private boolean mIFeatureOpen;
    private ArrayList<String> mIdleOptimizePackageList;
    private int mInstallApkSequenceNumber;
    private ArrayList<String> mInstallBlackList;
    private boolean mIsForbidBrushInstallShowToast;
    private boolean mIsForbidDesktopAppInstallByBlackMap;
    private boolean mIsForbidUsbBrushAppInstallByBlackList;
    private boolean mIsForbidUsbBrushAppInstallBySignature;
    private boolean mIsMonkeyTest;
    private boolean mIsSettingAllowAdbSimulateInput;
    private long mLastGetWifiElapsedTime;
    private String mLastWifiList;
    private Object mLock;
    private Object mLogInfoLocalLock;
    private final PackageManagerService mPKMService;
    VivoPKMSLocManager mPkmsLocMgr;
    private HandlerThread mPkmsUtilsHThread;
    public PkmsUtilsHandler mPkmsUtilsHandler;
    private final PackageManagerInternal mServiceInternal;
    private ArrayList<String> mSpecialIList;
    private int mUnInstallApkSequenceNumber;
    private int mUpdateApkSequenceNumber;
    private final UserManagerInternal mUserManagerInt;
    private ArrayList<VivoBlacklistApps> mVivoBlackAppList;
    VivoConfigReceiver mVivoConfigReceiver;
    VivoLogReceiver mVivoLogReceiver;
    VivoPKMSDatabaseUtils mVivoPKMSDatabaseUtils;
    public static boolean DEBUG = PackageManagerService.DEBUG;
    protected static boolean DEBUG_FOR_FB_APL = PackageManagerService.DEBUG_FOR_FB_APL;
    private static final boolean mIsOverseas = "yes".equals(SystemProperties.get("ro.vivo.product.overseas", "no"));
    static boolean mDeviceBootComplete = false;
    static final String[] CTS_APPS = {"com.drawelements.deqp", "com.replica.replicaisland", "android.abioverride.app", "foo.bar.baz", "android.admin.app", "android.alarmclock.service", "android.server.wm.alertwindowservice", "android.app.stubs", "com.android.app2", "android.app.usage.app", "android.app.usage.apptoo", "android.assist.testapp", "android.assist.service", "com.android.test.cantsavestate1", "com.android.test.cantsavestate2", "com.android.appsecurity.b71360999", "com.example.helloworld", "android.cpptools.app", "android.server.wm.alertwindowapp", "android.server.wm.alertwindowappsdk25", "android.server.am.debuggable", "android.server.am.deprecatedsdk", "android.server.am.displaysize", "com.android.compatibility.common.deviceinfo", "android.os.app", "android.server.am.prerelease", "android.server.am", "android.server.am.app27", "android.server.am.second", "android.server.am.third", "android.taskswitching.appa", "android.taskswitching.appb", "android.server.am.translucentapp", "android.server.am.translucentapp26", "android.server.wm.dndsourceapp", "android.server.wm.dndtargetapp", "android.server.wm.dndtargetappsdk23", "com.android.dynamiclinker", "android.externalservice.service", "android.rootlessgpudebug.DEBUG.app", "android.rootlessgpudebug.LAYERS.app", "android.rootlessgpudebug.RELEASE.app", "android.harmfulappwarning.sampleapp", "android.harmfulappwarning.testapp", "android.os.procfs", "test.instant.cookie", "android.backup.kvapp", "android.leanbackjank.app", "libcore.java.util.collectiontests", "android.libcore.runner", "android.media.app.media_session_test_helper", "android.nativemedia.aaudio", "android.perfetto.producer", "android.sample.app", "com.android.simpleperf", "org.skia.skqp", "android.os.lib.consumer1", "android.os.lib.consumer2", "android.os.lib.consumer3", "android.os.lib.provider", "android.os.lib.provider.recursive", "android.os.lib.consumer", "android.test.app", "com.android.gputest", "android.theme.app", "android.trustedvoice.app", "android.voiceinteraction.testapp", "android.voiceinteraction.service", "android.voicesettings.service", "android.backup.app", "android.accessibilityservice.delegate", "android.tests.devicesetup", "android.core.tests.runner", "android.core.vm-tests-tf", "android.accounts.test.shared", "com.android.ndkaudio", "android.server.wm.app", "android.server.wm.app27", "android.server.wm.second", "android.server.wm.third", "android.server.wm.deprecatedsdk", "android.server.wm.displaysize", "android.server.wm.prerelease", "android.server.wm.profileable", "android.server.wm.translucentapp", "android.server.wm.translucentapp26", "android.jdwptunnel.sampleapp", "com.android.angleIntegrationTest.driverTest", "com.android.angleIntegrationTest.driverTestSecondary", "com.android.app1", "com.android.app3", "com.android.tests.atomicinstall", "android.server.wm.backgroundactivity.appa", "android.server.wm.backgroundactivity.appb", "android.rootlessgpudebug.GLES_LAYERS.app", "android.os.powermanagertests", "android.backup.permission", "android.backup.permission22", "com.android.simpleperf.debuggable", "com.android.simpleperf.profileable", "android.testharness.app", "android.wifibroadcasts.app", "com.android.test.notificationdelegator", "com.android.tests.stagedinstall", "com.android.nn.benchmark.vts.v1_2", "com.android.test.storagedelegator", "com.android.tests.codepath.app", "android.appenumeration.editor.activity", "android.appenumeration.filters", "android.appenumeration.forcequeryable", "android.appenumeration.noapi", "android.appenumeration.queries.activity.action", "android.appenumeration.queries.nothing", "android.appenumeration.queries.nothing.haspermission", "android.appenumeration.queries.nothing.hasprovider", "android.appenumeration.queries.nothing.q", "android.appenumeration.queries.pkg", "android.appenumeration.queries.provider.action", "android.appenumeration.queries.provider.authority", "android.appenumeration.queries.service.action", "android.appenumeration.queries.activity.action.unexported", "android.appenumeration.queries.provider.action.unexported", "android.appenumeration.queries.provider.authority.unexported", "android.appenumeration.queries.service.action.unexported", "android.appenumeration.share.activity", "android.appenumeration.queries.nothing.shareduid", "android.appenumeration.noapi.shareduid", "android.appenumeration.web.activity", "android.appenumeration.queries.wildcard.action", "android.appenumeration.queries.wildcard.browsable", "android.appenumeration.queries.wildcard.editor", "android.appenumeration.queries.wildcard.share", "android.appenumeration.queries.wildcard.web", "com.android.bionic_app", "android.server.wm.shareuid.a", "android.server.wm.shareuid.b", "android.dynamicmime.helper", "android.dynamicmime.preferred", "android.dynamicmime.testapp", "android.dynamicmime.update", "android.rootlessgpudebug.INJECT.app", "android.graphics.gpuprofiling.app", "android.os.inattentivesleeptests", "android.matchflags.app.shared", "android.matchflags.app.uniqueandshared", "com.android.tests.securefrpinstall", "com.android.suspendapps.suspendtestapp", "com.android.suspendapps.suspendtestapp2", "com.android.suspendapps.testdeviceadmin", "android.server.wm.jetpack", "android.hdmicec.app", "android.incrementalinstall.incrementaltestapp", "android.incrementalinstall.inrementaltestappvalidation", "com.google.android.dialer.helper", "com.google.android.suspendapps.suspendtestapp2", "com.google.android.suspendapps.testdeviceadmin", "com.google.android.assist.voiceinteraction", "com.google.android.pseudolauncheractivity", "simpleperf.demo.java_api", "simpleperf.demo.cpp_api", "com.example.android.displayingbitmaps", "com.example.android.displayingbitmaps.test", "com.google.sample.tunnel", "com.android.simpleperf.profileable", "com.example.android.displayingbitmaps.tests", "com.google.android.assist.voiceinteraction", "com.google.android.dialer.helper", "com.google.android.suspendapps.suspendtestapp2", "com.google.android.pseudolauncheractivity", "com.google.android.appvisibility.testapp"};
    static final String[] GTS_APPS = {"com.google.android.ar.svc", "com.android.compatibility.common.deviceinfo", "android.largeapk.app", "com.android.notification.functional", "com.android.systemmetrics.functional", "android.app.usage.app", "com.google.android.suspendapps.permission.gts", "com.google.android.suspendapps.suspendtestapp", "com.appspot.safebrowsingtest.test1", "com.google.android.app.stubs", "com.google.android.app1", "com.google.android.app2", "com.google.android.app3", "com.google.android.assist.app1", "com.google.android.assist.app2", "com.android.preconditions.gts", "com.google.android.suspendapps.gts", "android.tradefed.contentprovider", "com.google.android.suspendapps.emptydeviceadmin", "com.android.bedstead.remotedpc.dpc"};
    static final String[] TF_APPS = {"com.android.tradefed.testapp", "com.android.tradefed.uitestapp", "com.android.tradefed.utils.wifi", "com.android.tradefed.utils", "android.accessibilityservice.delegate", "android.tests.devicesetup", "android.core.tests.runner", "android.core.vm-tests-tf", "android.accounts.test.shared", "com.android.ndkaudio", "com.example.com.gmsconfig", "io.appium.unlock"};
    static final String[] ARCORE_APPS = {"com.google.tango.cameratests", "com.google.ar.infrastructure.recorderapp", "com.google.tango.cameraimagequality", "com.google.tango.imucalibration", "com.google.tango.utility.rollingshutter", "com.google.tango.factorycal", "com.google.atap.tangohal.recorder"};
    static final String[] CANNOT_UNINSTALL_APPS = {"com.android.mms", "com.bbk.account"};
    static ArrayList<String> INTERCEPT_SHELL_PERMISSION_LIST = new ArrayList<>();
    private static boolean ADB_SHELL_SIMULATE_PROP = SystemProperties.getBoolean("persist.adb.simulate.input", false);
    private static final String OP_ENTRY = SystemProperties.get("ro.vivo.op.entry", "no");
    private static final boolean FEATURE_FOR_NET = "yes".equals(SystemProperties.get("ro.vivo.net.entry", "no"));

    static {
        boolean z = false;
        INTERCEPT_SHELL_PERMISSION_LIST.add("android.permission.INJECT_EVENTS");
        INTERCEPT_SHELL_PERMISSION_LIST.add("android.permission.WRITE_SECURE_SETTINGS");
        VCD_INFO_TO_LOCAL_OPEN = true;
        DEFAULT_MAX_RECORD_COUNT = 1000;
        DEVICE_VERSION = SystemProperties.get("ro.vivo.product.version", "unknow");
        mAdbUninstallVerifyConfig = new AdbVerifyConfig(true, FORBID_BRUSH_TOOLS_OVERTIME, 1);
        base64EncodeChars = new char[]{'Q', '8', 'v', 'N', '-', 'r', 'y', 'a', 'E', 'J', 'G', 'o', 'T', 'W', 'O', 't', 'K', '_', 'q', 'M', 'k', 'h', '5', 'R', 'Z', '6', 'L', 'x', 'c', 'U', 'A', '3', 'd', 'n', 'z', 'e', 'H', 'u', '2', 'X', 'j', 'S', 'b', 'V', 's', 'F', 'Y', 'w', 'f', 'P', 'D', '9', '4', 'C', '0', 'l', 'm', '1', 'I', 'p', '7', 'g', 'B', 'i'};
        base64DecodeChars = new byte[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 4, -1, -1, 54, 57, 38, 31, 52, 22, 25, 60, 1, 51, -1, -1, -1, -1, -1, -1, -1, 30, 62, 53, 50, 8, 45, 10, 36, 58, 9, 16, 26, 19, 3, 14, 49, 0, 23, 41, 12, 29, 43, 13, 39, 46, 24, -1, -1, -1, -1, 17, -1, 7, 42, 28, 32, 35, 48, 61, 21, 63, 40, 20, 55, 56, 33, 11, 59, 18, 5, 44, 15, 37, 2, 47, 27, 6, 34, -1, -1, -1, -1, -1};
        mCurrentOsName = getOsName();
        mCurrentRomVersion = FtBuild.getRomVersion();
        if (Build.SUPPORTED_64_BIT_ABIS.length > 0) {
            z = true;
        }
        mIsBIT64Platform = z;
    }

    /* loaded from: classes.dex */
    public class VivoBlacklistApps {
        public String mBlacklistAppDir;
        public String mInnerFileName;

        public VivoBlacklistApps(String blacklistAppDir, String innerFileName) {
            this.mBlacklistAppDir = blacklistAppDir;
            this.mInnerFileName = innerFileName;
        }
    }

    public VivoPKMSUtils(Context context, PackageManagerService pkms, VivoPKMSDatabaseUtils vivoPKMSDatabaseUtils) {
        this.DEBUG_PERMISSION = PackageManagerService.DEBUG_PERMISSIONS || DEBUG;
        this.DEBUG_FOR_ALL = PackageManagerService.DEBUG_FOR_ALL;
        this.mContext = null;
        this.mAppFilterPermissionList = new ArrayList<>();
        this.mLock = new Object();
        this.mBuiltIn3PartAppPathList = new ArrayList<>();
        this.mVivoBlackAppList = new ArrayList<>();
        this.mIsSettingAllowAdbSimulateInput = false;
        this.mIsMonkeyTest = false;
        this.mBuiltIn3PartApkNameList = new ArrayList<>();
        this.mIdleOptimizePackageList = new ArrayList<>();
        this.mIsForbidBrushInstallShowToast = false;
        this.mForbidUsbBrushAppWarningToast = null;
        this.mForbidUsbBrushAppBlackList = new ArrayList<>();
        this.mIsForbidUsbBrushAppInstallByBlackList = true;
        this.mForbidUsbBrushAppSignatureList = new ArrayList<>();
        this.mIsForbidUsbBrushAppInstallBySignature = false;
        this.mForbidDesktopAppBlackMap = new HashMap<>();
        this.mIsForbidDesktopAppInstallByBlackMap = true;
        this.mForbidBrushToolsOverTime = FORBID_BRUSH_TOOLS_OVERTIME;
        this.mForbidDestTopAppOverTime = FORBID_DESKTOP_APP_OVERTIME;
        this.mForbidObject = new Object();
        this.mInstallApkSequenceNumber = 0;
        this.mUnInstallApkSequenceNumber = 0;
        this.mUpdateApkSequenceNumber = 0;
        this.mDeviceID = "unknow";
        this.mDeviceI = "unknow";
        this.mLastGetWifiElapsedTime = 0L;
        this.mLastWifiList = null;
        this.mLogInfoLocalLock = new Object();
        this.mAdbUninstallBlackList = new ArrayList<>();
        this.mInstallBlackList = new ArrayList<>();
        this.mSpecialIList = new ArrayList<>();
        this.mIFeatureOpen = true;
        this.emmPackageCacheList = new ArrayList<>();
        this.mContext = context;
        this.mPKMService = pkms;
        this.mVivoPKMSDatabaseUtils = vivoPKMSDatabaseUtils;
        HandlerThread handlerThread = new HandlerThread("PkmsUtilsHandlerThread");
        this.mPkmsUtilsHThread = handlerThread;
        handlerThread.start();
        this.mPkmsUtilsHandler = new PkmsUtilsHandler(this.mPkmsUtilsHThread.getLooper());
        initConfig();
        InitBuiltIn3PartAppPathList();
        InitVivoBlacklistApps();
        initForbidList();
        initDefaultList();
        this.mPkmsLocMgr = VivoPKMSLocManager.init(this.mContext, this.mPkmsUtilsHThread.getLooper(), DEBUG, this.DEBUG_FOR_ALL);
        VivoUninstallMgr.getInstance().init(this.mContext, this.mPkmsUtilsHThread.getLooper(), this.mBuiltIn3PartApkNameList, DEBUG, this.DEBUG_FOR_ALL);
        VivoPKMSReportMgr.getInstance().init(this.mContext, this.mPkmsUtilsHThread.getLooper());
        this.mServiceInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        this.mUserManagerInt = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void systemReady() {
        this.mVivoLogReceiver = new VivoLogReceiver();
        this.mVivoConfigReceiver = new VivoConfigReceiver();
        registerSettingsObsever();
        VSlog.i(TAG, "pkms systemReady.");
        Message initSeqNumMsg = this.mPkmsUtilsHandler.obtainMessage(MSG_INIT_SEQ_NUM_FROM_LOCAL_FILE);
        this.mPkmsUtilsHandler.sendMessageDelayed(initSeqNumMsg, 3000L);
        initIdleOptimize();
        Message delayInitIEMsg = this.mPkmsUtilsHandler.obtainMessage(MSG_DELAY_INIT_DEVICE_CODE);
        this.mPkmsUtilsHandler.sendMessageDelayed(delayInitIEMsg, 15000L);
        this.mPkmsLocMgr.systemReady();
        installSubUserAppsAfterUpgrade();
        checkPermissionControllerStateForCloneUser();
    }

    private void installSubUserAppsAfterUpgrade() {
        int[] userIds = this.mPKMService.mUserManager.getUserIds();
        if (userIds != null && userIds.length > 1 && SystemProperties.get("persist.vivo.needInstallForSubUser", "yes").equals("yes")) {
            installSubUserAppsAfterUpgradeInternel(userIds);
        }
        SystemProperties.set("persist.vivo.needInstallForSubUser", "no");
    }

    private void installSubUserAppsAfterUpgradeInternel(int[] userIds) {
        int i;
        int i2;
        Collection<PackageSetting> packages = this.mPKMService.mSettings.mPackages.values();
        int packagesCount = packages.size();
        Iterator<PackageSetting> packagesIterator = packages.iterator();
        DevicePolicyManager dpm = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        String[] disallowedPackages_managedProfile = null;
        int j = 1;
        while (true) {
            int length = userIds.length;
            i = ProcessList.CACHED_APP_MAX_ADJ;
            i2 = 0;
            if (j >= length) {
                break;
            }
            if (this.mPKMService.mUserManager.getUserInfo(userIds[j]).isManagedProfile() && userIds[j] != 999 && dpm != null) {
                ComponentName deviceAdminComponentName = new ComponentName("abc", "abc");
                disallowedPackages_managedProfile = (String[]) dpm.getDisallowedSystemApps(deviceAdminComponentName, userIds[j], "android.app.action.PROVISION_MANAGED_USER").toArray(new String[0]);
            }
            j++;
        }
        int i3 = 0;
        while (i3 < packagesCount) {
            PackageSetting ps = packagesIterator.next();
            if (ps.pkg != null) {
                if (((!ps.isSystem() || ps.getPkgState().isHiddenUntilInstalled()) ? i2 : 1) != 0 && ps.getInstalled(i2)) {
                    int j2 = 1;
                    while (j2 < userIds.length) {
                        if (!ps.getInstalled(userIds[j2]) && userIds[j2] != i) {
                            if (this.mPKMService.mUserManager.getUserInfo(userIds[j2]).isManagedProfile()) {
                                if (disallowedPackages_managedProfile != null && !ArrayUtils.contains(disallowedPackages_managedProfile, ps.name)) {
                                    ps.setInstalled(true, userIds[j2]);
                                    VSlog.i(TAG, "installSubUserAppsAfterUpgradeInternel setInstalledManagedProfile " + userIds[j2] + " packageName:" + ps.pkg.getPackageName());
                                }
                            } else {
                                ps.setInstalled(true, userIds[j2]);
                                VSlog.i(TAG, "installSubUserAppsAfterUpgradeInternel setInstalled " + userIds[j2] + " packageName:" + ps.pkg.getPackageName());
                            }
                        }
                        j2++;
                        i = ProcessList.CACHED_APP_MAX_ADJ;
                    }
                }
            }
            i3++;
            i = ProcessList.CACHED_APP_MAX_ADJ;
            i2 = 0;
        }
    }

    private void checkPermissionControllerStateForCloneUser() {
        VSlog.i(TAG, "checkPermissionControllerStateForCloneUser");
        if (this.mPKMService.mUserManager.isDoubleAppUserExist()) {
            String packageName = mIsOverseas ? "com.google.android.permissioncontroller" : "com.android.permissioncontroller";
            PackageSetting pkgSetting = (PackageSetting) this.mPKMService.mSettings.mPackages.get(packageName);
            if (pkgSetting != null && !pkgSetting.getInstalled((int) ProcessList.CACHED_APP_MAX_ADJ)) {
                pkgSetting.setInstalled(true, (int) ProcessList.CACHED_APP_MAX_ADJ);
                VSlog.i(TAG, "PermissionControllerStateForCloneUser setInstalled packageName:" + packageName);
                return;
            }
            VSlog.i(TAG, "PermissionControllerStateForCloneUser already Installed packageName:" + packageName);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public ResolveInfo findSetPreferredActivityOrSystemActivity(Intent intent, String resolvedType, int flags, List<ResolveInfo> query, int userId) {
        List<PreferredActivity> prefs;
        ActivityInfo ai;
        if (intent != null && intent.getDataString() != null && "android.intent.action.VIEW".equals(intent.getAction()) && (intent.getDataString().contains("http://hybrid.vivo.com/app") || intent.getDataString().contains("https://hybrid.vivo.com/app") || intent.getDataString().contains("http://hapjs.org/app") || intent.getDataString().contains("https://hapjs.org/app"))) {
            intent.setPackage("com.vivo.hybrid");
            VSlog.v(TAG, "findSetPreferredActivityOrSystemActivity setPackage com.vivo.hybrid");
            return this.mPKMService.resolveIntent(intent, resolvedType, flags, userId);
        }
        PreferredIntentResolver pir = (PreferredIntentResolver) this.mPKMService.mSettings.mPreferredActivities.get(userId);
        int i = 0;
        if (pir != null) {
            prefs = pir.queryIntent(intent, resolvedType, (65536 & flags) != 0, userId);
        } else {
            prefs = null;
        }
        if (this.DEBUG_PREFERRED) {
            VSlog.v(TAG, "findSetPreferredActivityOrSystemActivity######  " + pir + " prefs:" + prefs);
        }
        int N = query.size();
        if (prefs != null && prefs.size() > 0) {
            int M = prefs.size();
            int i2 = 0;
            while (i2 < M) {
                PreferredActivity pa = prefs.get(i2);
                if (this.DEBUG_PREFERRED) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Checking PreferredActivity ds=");
                    sb.append(pa.countDataSchemes() > 0 ? pa.getDataScheme(i) : "<none>");
                    sb.append("\n  component=");
                    sb.append(pa.mPref.mComponent);
                    VSlog.v(TAG, sb.toString());
                    pa.dump(new LogPrinter(2, TAG, 3), "  ");
                }
                if (pa.mPref.mAlways && (ai = this.mPKMService.getActivityInfo(pa.mPref.mComponent, flags | 512, userId)) != null) {
                    if (this.DEBUG_PREFERRED) {
                        VSlog.v(TAG, "ai packageName ====" + ai.applicationInfo.packageName);
                        VSlog.v(TAG, "ai name ====" + ai.name);
                    }
                    for (int j = 0; j < N; j++) {
                        ResolveInfo ri = query.get(j);
                        if (this.DEBUG_PREFERRED) {
                            VSlog.v(TAG, "ri packageName ====" + ri.activityInfo.applicationInfo.packageName);
                            VSlog.v(TAG, "ri name ====" + ri.activityInfo.name);
                        }
                        if (ri.activityInfo.applicationInfo.packageName.equals(ai.applicationInfo.packageName) && ri.activityInfo.name.equals(ai.name)) {
                            if (this.DEBUG_PREFERRED) {
                                VSlog.v(TAG, "return ri name &&&&" + ri.activityInfo.name);
                            }
                            return ri;
                        }
                    }
                    continue;
                }
                i2++;
                i = 0;
            }
        } else {
            VSlog.i(TAG, "  prefs:" + prefs + " querySize:" + N);
        }
        int T = query.size();
        for (int i3 = 0; i3 < T; i3++) {
            ResolveInfo ri1 = query.get(i3);
            String packagename = ri1.activityInfo.packageName;
            if (this.DEBUG_PREFERRED) {
                VSlog.i(TAG, "\tpackagename:" + packagename);
            }
            if (isSystemApp(packagename)) {
                if (this.DEBUG_PREFERRED) {
                    VSlog.v(TAG, " return  packagename = " + packagename);
                    VSlog.v(TAG, " return -----------------------*********** ");
                }
                return ri1;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* JADX WARN: Code restructure failed: missing block: B:8:0x002b, code lost:
        if (r19.DEBUG_PREFERRED == false) goto L9;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public boolean isNeedIngnoreResolveActivity(android.content.Intent r20, java.lang.String r21) {
        /*
            Method dump skipped, instructions count: 636
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.VivoPKMSUtils.isNeedIngnoreResolveActivity(android.content.Intent, java.lang.String):boolean");
    }

    protected boolean isSystemApp(String packageName) {
        PackageManager packageManager = this.mContext.getPackageManager();
        try {
            ApplicationInfo info = packageManager.getApplicationInfo(packageName, 4096);
            if ((info.flags & 1) == 0) {
                if ((info.flags & 128) == 0) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    void initConfig() {
        Message msg = this.mPkmsUtilsHandler.obtainMessage(1000);
        this.mPkmsUtilsHandler.sendMessage(msg);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class PkmsUtilsHandler extends Handler {
        public PkmsUtilsHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (VivoPKMSUtils.DEBUG) {
                VSlog.d(VivoPKMSUtils.TAG, "--handleMessage " + msg.what);
            }
            int i = msg.what;
            if (i == 1000) {
                VSlog.i(VivoPKMSUtils.TAG, "msg:1000, do nothing.");
            } else if (i == VivoPKMSUtils.MSG_INIT_SEQ_NUM_FROM_LOCAL_FILE) {
                try {
                    VivoPKMSUtils.this.initSeqNumFromLocalFile();
                } catch (Exception e) {
                    VSlog.w(VivoPKMSUtils.TAG, "init seq catch exception, " + e.toString());
                }
            } else {
                switch (i) {
                    case 2000:
                        InstallApkBaseInfo installApkInfo = (InstallApkBaseInfo) msg.obj;
                        try {
                            VivoPKMSUtils.this.sendApkInstalledBroadcastInner(installApkInfo);
                            return;
                        } catch (Exception e2) {
                            VSlog.w(VivoPKMSUtils.TAG, "install info " + e2.toString());
                            return;
                        }
                    case VivoPKMSUtils.MSG_VCD_APK_UN_INSTALL_LOG /* 2001 */:
                        UnInstallApkBaseInfo unInstallApkInfo = (UnInstallApkBaseInfo) msg.obj;
                        try {
                            VivoPKMSUtils.this.sendApkUnInstalledBroadcastInner(unInstallApkInfo);
                            return;
                        } catch (Exception e3) {
                            VSlog.w(VivoPKMSUtils.TAG, "unInstall info " + e3.toString());
                            return;
                        }
                    case VivoPKMSUtils.MSG_DELAY_INIT_DEVICE_CODE /* 2002 */:
                        try {
                            VivoPKMSUtils.this.initDeviceI();
                            return;
                        } catch (Exception e4) {
                            return;
                        }
                    default:
                        return;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class VivoConfigReceiver extends BroadcastReceiver {
        public VivoConfigReceiver() {
            IntentFilter filter = new IntentFilter();
            filter.addAction(VivoPKMSUtils.CONFIG_UPDATE_ACTION_BASE_APK);
            filter.addAction("android.intent.action.BOOT_COMPLETED");
            filter.addAction(VivoPKMSUtils.CONFIG_UPDATE_ACTION_ADB_SILENT_INSTALL);
            filter.addAction("android.intent.action.USER_STARTED");
            VivoPKMSUtils.this.mContext.registerReceiver(this, filter);
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            VSlog.i(VivoPKMSUtils.TAG, "Receive " + intent);
            String action = intent.getAction();
            if (VivoPKMSUtils.CONFIG_UPDATE_ACTION_BASE_APK.equals(action)) {
                VivoPKMSUtils.this.refreshConfig(1000, "CONFIG_UPDATE_ACTION_BASE_APK", VivoPKMSUtils.CONFIG_MODULE_NAME_BASE_APK);
            } else if (VivoPKMSUtils.CONFIG_UPDATE_ACTION_ADB_SILENT_INSTALL.equals(action)) {
                VivoPKMSUtils.this.refreshConfig(1000, "CONFIG_UPDATE_ACTION_ADB_SILENT_INSTALL", VivoPKMSUtils.CONFIG_MODULE_NAME_ADB_SILENT_INSTALL);
            } else if (VivoPKMSUtils.CONFIG_UPDATE_ACTION_COMMON_CONFIG.equals(action)) {
                VivoPKMSUtils.this.refreshConfig(1000, "CONFIG_MODULE_NAME_COMMON_CONFIG", VivoPKMSUtils.CONFIG_MODULE_NAME_COMMON_CONFIG);
            } else if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
                VivoPKMSUtils.mDeviceBootComplete = true;
                VivoPKMSUtils.this.refreshConfig(6000, "BOOT_COMPLETED", VivoPKMSUtils.CONFIG_MODULE_NAME_BASE_APK);
                VivoPKMSUtils.this.refreshConfig(1000, "BOOT_COMPLETED", VivoPKMSUtils.CONFIG_MODULE_NAME_ADB_SILENT_INSTALL);
                VivoPKMSUtils.this.refreshConfig(1100, "BOOT_COMPLETED", VivoPKMSUtils.CONFIG_MODULE_NAME_COMMON_CONFIG);
                VivoPKMSUtils.this.refreshThirdLauncherPkgNames(1000);
            } else if ("android.intent.action.USER_STARTED".equals(action)) {
                final int userId = intent.getIntExtra("android.intent.extra.user_handle", ProcessList.INVALID_ADJ);
                VSlog.v(VivoPKMSUtils.TAG, "receive broadcast action: " + action + ", userId: " + userId);
                VivoPKMSUtils.this.mPkmsUtilsHandler.postDelayed(new Runnable() { // from class: com.android.server.pm.-$$Lambda$VivoPKMSUtils$VivoConfigReceiver$fjiE2hys1kwnhrNOLh-ypqpXf0Y
                    @Override // java.lang.Runnable
                    public final void run() {
                        VivoPKMSUtils.VivoConfigReceiver.this.lambda$onReceive$0$VivoPKMSUtils$VivoConfigReceiver(userId);
                    }
                }, 100L);
            }
        }

        public /* synthetic */ void lambda$onReceive$0$VivoPKMSUtils$VivoConfigReceiver(int userId) {
            VivoPKMSUtils.this.verifySystemFixedPermission(userId);
        }
    }

    void refreshConfig(final int delayTime, String info, final String moduleName) {
        if (this.DEBUG_PERMISSION || DEBUG_FOR_FB_APL) {
            VSlog.d(TAG, "refresh config  " + info + " delay " + delayTime + " moduleName:" + moduleName);
        }
        this.mPkmsUtilsHandler.postDelayed(new Runnable() { // from class: com.android.server.pm.VivoPKMSUtils.1
            @Override // java.lang.Runnable
            public void run() {
                boolean result = VivoPKMSUtils.this.readConfigFromDB("content://com.vivo.abe.unifiedconfig.provider/configs", moduleName, "1");
                VSlog.d(VivoPKMSUtils.TAG, "read " + moduleName + " config from DB-END  result:" + result + " delayTime " + delayTime);
            }
        }, delayTime);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Code restructure failed: missing block: B:30:0x00fe, code lost:
        if (0 == 0) goto L27;
     */
    /* JADX WARN: Code restructure failed: missing block: B:33:0x0103, code lost:
        return !r14;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public boolean readConfigFromDB(java.lang.String r17, java.lang.String r18, java.lang.String r19) {
        /*
            Method dump skipped, instructions count: 266
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.VivoPKMSUtils.readConfigFromDB(java.lang.String, java.lang.String, java.lang.String):boolean");
    }

    private void parseConfigFromDataBase(String contents, boolean writeToLocal, String info, String moduleName) {
        if (contents == null || contents.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK)) {
            VSlog.w(TAG, "parse config, content is empty. " + contents);
            return;
        }
        if (DEBUG_FOR_FB_APL || this.DEBUG_PERMISSION) {
            VSlog.d(TAG, "parse config from str");
        }
        try {
            ByteArrayInputStream byteStream = new ByteArrayInputStream(contents.getBytes());
            if (CONFIG_MODULE_NAME_BASE_APK.equals(moduleName)) {
                parseBaseApkListConfigFromStream(byteStream, writeToLocal, info, contents);
            } else if (CONFIG_MODULE_NAME_COMMON_CONFIG.equals(moduleName)) {
                parseCommonConfigFromStream(byteStream, info, contents);
            } else if (CONFIG_MODULE_NAME_ADB_SILENT_INSTALL.equals(moduleName)) {
                parseAdbSilentInstallConfigFromStream(byteStream, info, contents);
            }
        } catch (Exception e) {
            VSlog.w(TAG, "parse config," + e.toString());
        }
    }

    private void parseBaseApkListConfigFromStream(InputStream inputStream, boolean writeToLocal, String info, String sourceContents) {
        ArrayList<String> idleOptimizePackageListTemp;
        ArrayList<String> adbUninstallBlackListTemp;
        ArrayList<String> adbUninstallBlackListTemp2;
        int eventCode;
        ArrayList<String> installBlackListTemp;
        if (inputStream == null) {
            return;
        }
        if (DEBUG_FOR_FB_APL) {
            VSlog.d(TAG, "parse config  toLocal:" + writeToLocal + "  " + info + " sourceContents:" + sourceContents);
        }
        try {
            try {
                try {
                    try {
                        XmlPullParserFactory pullFactory = XmlPullParserFactory.newInstance();
                        XmlPullParser parser = pullFactory.newPullParser();
                        parser.setInput(inputStream, "utf-8");
                        int eventCode2 = parser.getEventType();
                        ArrayList<String> baseSysApkList = new ArrayList<>();
                        ArrayList<String> baseThrApkList = new ArrayList<>();
                        ArrayList<String> forbidUsbBrushAppBlackListTemp = new ArrayList<>();
                        ArrayList<String> forbidUsbBrushAppSignatureTemp = new ArrayList<>();
                        HashMap<String, ForbidDeskTopAppInfo> forbidDesktopAppBlackMapTemp = new HashMap<>();
                        ArrayList<String> idleOptimizePackageListTemp2 = new ArrayList<>();
                        ArrayList<String> adbUninstallBlackListTemp3 = new ArrayList<>();
                        ArrayList<String> installBlackListTemp2 = new ArrayList<>();
                        while (true) {
                            XmlPullParserFactory pullFactory2 = pullFactory;
                            if (eventCode2 == 1) {
                                ArrayList<String> idleOptimizePackageListTemp3 = idleOptimizePackageListTemp2;
                                ArrayList<String> adbUninstallBlackListTemp4 = adbUninstallBlackListTemp3;
                                if (DEBUG_FOR_FB_APL) {
                                    VSlog.d(TAG, "parser end print baseSysApkList: " + baseSysApkList);
                                    VSlog.d(TAG, "parser end print baseThrApkList: " + baseThrApkList);
                                }
                                if (baseSysApkList.size() > 0) {
                                    this.mVivoPKMSDatabaseUtils.generateBaseSysApkList(baseSysApkList);
                                }
                                if (baseThrApkList.size() > 0) {
                                    this.mVivoPKMSDatabaseUtils.generateBaseThrApkList(baseThrApkList);
                                }
                                applyParserValueToForbidList(forbidUsbBrushAppBlackListTemp, forbidUsbBrushAppSignatureTemp, forbidDesktopAppBlackMapTemp);
                                if (idleOptimizePackageListTemp3.size() > 0) {
                                    this.mIdleOptimizePackageList.clear();
                                    this.mIdleOptimizePackageList.addAll(idleOptimizePackageListTemp3);
                                }
                                if (adbUninstallBlackListTemp4.size() > 0) {
                                    this.mAdbUninstallBlackList.clear();
                                    this.mAdbUninstallBlackList.addAll(adbUninstallBlackListTemp4);
                                }
                                if (installBlackListTemp2.size() > 0) {
                                    this.mInstallBlackList.clear();
                                    this.mInstallBlackList.addAll(installBlackListTemp2);
                                }
                                if (inputStream != null) {
                                    inputStream.close();
                                    return;
                                }
                                return;
                            }
                            if (eventCode2 == 2) {
                                String name = parser.getName();
                                if ("package".equals(name)) {
                                    adbUninstallBlackListTemp2 = adbUninstallBlackListTemp3;
                                    installBlackListTemp = installBlackListTemp2;
                                    eventCode = eventCode2;
                                } else if ("sysPackage".equals(name)) {
                                    String value = parser.getAttributeValue(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, "name");
                                    if (DEBUG_FOR_FB_APL) {
                                        VSlog.i(TAG, " value:" + value);
                                    }
                                    if (!baseSysApkList.contains(value)) {
                                        baseSysApkList.add(value);
                                    }
                                    adbUninstallBlackListTemp2 = adbUninstallBlackListTemp3;
                                    installBlackListTemp = installBlackListTemp2;
                                    eventCode = eventCode2;
                                } else if ("thrPackage".equals(name)) {
                                    String value2 = parser.getAttributeValue(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, "name");
                                    if (DEBUG_FOR_FB_APL) {
                                        VSlog.i(TAG, " value:" + value2);
                                    }
                                    if (!baseThrApkList.contains(value2)) {
                                        baseThrApkList.add(value2);
                                    }
                                    adbUninstallBlackListTemp2 = adbUninstallBlackListTemp3;
                                    installBlackListTemp = installBlackListTemp2;
                                    eventCode = eventCode2;
                                } else if ("idleOptimizeList".equals(name)) {
                                    String pkgName = parser.getAttributeValue(null, "pkgName");
                                    if (DEBUG_FOR_FB_APL) {
                                        VSlog.i(TAG, " idleOptimizeMap pkgName:" + pkgName);
                                    }
                                    if (!idleOptimizePackageListTemp2.contains(pkgName)) {
                                        idleOptimizePackageListTemp2.add(pkgName);
                                    }
                                    if (DEBUG_FOR_FB_APL) {
                                        VSlog.i(TAG, "idleOptimizePackageListTemp:" + idleOptimizePackageListTemp2);
                                    }
                                    adbUninstallBlackListTemp2 = adbUninstallBlackListTemp3;
                                    installBlackListTemp = installBlackListTemp2;
                                    eventCode = eventCode2;
                                } else if ("adbUninstallBlackList".equals(name)) {
                                    String pkgName2 = parser.getAttributeValue(null, "name");
                                    if (DEBUG_FOR_FB_APL) {
                                        VSlog.i(TAG, " adbUninstallBlackList pkgName:" + pkgName2);
                                    }
                                    adbUninstallBlackListTemp2 = adbUninstallBlackListTemp3;
                                    if (!adbUninstallBlackListTemp2.contains(pkgName2)) {
                                        adbUninstallBlackListTemp2.add(pkgName2);
                                    }
                                    if (DEBUG_FOR_FB_APL) {
                                        VSlog.i(TAG, "adbUninstallBlackListTemp:" + adbUninstallBlackListTemp2);
                                    }
                                    eventCode = eventCode2;
                                    installBlackListTemp = installBlackListTemp2;
                                } else {
                                    adbUninstallBlackListTemp2 = adbUninstallBlackListTemp3;
                                    if ("installBlackList".equals(name)) {
                                        String pkgName3 = parser.getAttributeValue(null, "name");
                                        if (DEBUG_FOR_FB_APL) {
                                            StringBuilder sb = new StringBuilder();
                                            eventCode = eventCode2;
                                            sb.append(" installBlackList pkgName:");
                                            sb.append(pkgName3);
                                            VSlog.i(TAG, sb.toString());
                                        } else {
                                            eventCode = eventCode2;
                                        }
                                        installBlackListTemp = installBlackListTemp2;
                                        if (!installBlackListTemp.contains(pkgName3)) {
                                            installBlackListTemp.add(pkgName3);
                                        }
                                        if (DEBUG_FOR_FB_APL) {
                                            VSlog.i(TAG, "installBlackListTemp:" + installBlackListTemp);
                                        }
                                    } else {
                                        eventCode = eventCode2;
                                        installBlackListTemp = installBlackListTemp2;
                                    }
                                }
                                if ("isForbidUsbBrushAppInstallByBlackList".equals(name)) {
                                    String value3 = parser.getAttributeValue(null, "name");
                                    if (DEBUG_FOR_FB_APL) {
                                        VSlog.i(TAG, " forbidBlackApp?:" + value3);
                                    }
                                    if (value3 != null) {
                                        synchronized (this.mForbidObject) {
                                            this.mIsForbidUsbBrushAppInstallByBlackList = Boolean.parseBoolean(value3);
                                        }
                                    }
                                    adbUninstallBlackListTemp = adbUninstallBlackListTemp2;
                                    installBlackListTemp2 = installBlackListTemp;
                                    idleOptimizePackageListTemp = idleOptimizePackageListTemp2;
                                } else if ("isForbidUsbBrushAppInstallBySignature".equals(name)) {
                                    String value4 = parser.getAttributeValue(null, "name");
                                    if (DEBUG_FOR_FB_APL) {
                                        VSlog.i(TAG, " forbidSigInfo?:" + value4);
                                    }
                                    if (value4 != null) {
                                        synchronized (this.mForbidObject) {
                                            this.mIsForbidUsbBrushAppInstallBySignature = Boolean.parseBoolean(value4);
                                        }
                                    }
                                    adbUninstallBlackListTemp = adbUninstallBlackListTemp2;
                                    installBlackListTemp2 = installBlackListTemp;
                                    idleOptimizePackageListTemp = idleOptimizePackageListTemp2;
                                } else if ("isForbidDesktopAppInstallByBlackMap".equals(name)) {
                                    String value5 = parser.getAttributeValue(null, "name");
                                    if (DEBUG_FOR_FB_APL) {
                                        VSlog.i(TAG, " forbidDesktopApp?:" + value5);
                                    }
                                    if (value5 != null) {
                                        synchronized (this.mForbidObject) {
                                            this.mIsForbidDesktopAppInstallByBlackMap = Boolean.parseBoolean(value5);
                                        }
                                    }
                                    adbUninstallBlackListTemp = adbUninstallBlackListTemp2;
                                    installBlackListTemp2 = installBlackListTemp;
                                    idleOptimizePackageListTemp = idleOptimizePackageListTemp2;
                                } else if ("forbidUsbBrushAppBlackList".equals(name)) {
                                    String value6 = parser.getAttributeValue(null, "name");
                                    if (DEBUG_FOR_FB_APL) {
                                        VSlog.i(TAG, " blackBrushApp:" + value6);
                                    }
                                    if (!forbidUsbBrushAppBlackListTemp.contains(value6)) {
                                        forbidUsbBrushAppBlackListTemp.add(value6);
                                    }
                                    adbUninstallBlackListTemp = adbUninstallBlackListTemp2;
                                    installBlackListTemp2 = installBlackListTemp;
                                    idleOptimizePackageListTemp = idleOptimizePackageListTemp2;
                                } else if ("forbidUsbBrushAppSignature".equals(name)) {
                                    String value7 = parser.getAttributeValue(null, "publicKey");
                                    String pkgName4 = parser.getAttributeValue(null, "pkgName");
                                    if (DEBUG_FOR_FB_APL) {
                                        StringBuilder sb2 = new StringBuilder();
                                        installBlackListTemp2 = installBlackListTemp;
                                        sb2.append("pkgName:");
                                        sb2.append(pkgName4);
                                        sb2.append(" pubKey:");
                                        sb2.append(value7);
                                        VSlog.i(TAG, sb2.toString());
                                    } else {
                                        installBlackListTemp2 = installBlackListTemp;
                                    }
                                    if (value7 != null && value7.length() != 0 && !forbidUsbBrushAppSignatureTemp.contains(value7)) {
                                        forbidUsbBrushAppSignatureTemp.add(value7);
                                    }
                                    adbUninstallBlackListTemp = adbUninstallBlackListTemp2;
                                    idleOptimizePackageListTemp = idleOptimizePackageListTemp2;
                                } else {
                                    installBlackListTemp2 = installBlackListTemp;
                                    if ("forbidDesktopAppBlackMap".equals(name)) {
                                        String apkName = parser.getAttributeValue(null, "name");
                                        String cnName = parser.getAttributeValue(null, "cnName");
                                        String cnNOpen = parser.getAttributeValue(null, "cnNOpen");
                                        Boolean isOpen = Boolean.valueOf(Boolean.parseBoolean(cnNOpen));
                                        if (DEBUG_FOR_FB_APL) {
                                            adbUninstallBlackListTemp = adbUninstallBlackListTemp2;
                                            StringBuilder sb3 = new StringBuilder();
                                            idleOptimizePackageListTemp = idleOptimizePackageListTemp2;
                                            sb3.append(" deskTopApp:");
                                            sb3.append(apkName);
                                            sb3.append(" c_name:");
                                            sb3.append(cnName);
                                            sb3.append(" c_o:");
                                            sb3.append(cnNOpen);
                                            VSlog.i(TAG, sb3.toString());
                                        } else {
                                            adbUninstallBlackListTemp = adbUninstallBlackListTemp2;
                                            idleOptimizePackageListTemp = idleOptimizePackageListTemp2;
                                        }
                                        if (apkName == null) {
                                            pullFactory = pullFactory2;
                                            eventCode2 = eventCode;
                                            adbUninstallBlackListTemp3 = adbUninstallBlackListTemp;
                                            idleOptimizePackageListTemp2 = idleOptimizePackageListTemp;
                                        } else {
                                            ArrayList<String> deskTopAppCnNameList = new ArrayList<>();
                                            if (cnName != null) {
                                                try {
                                                    String[] strArrs = cnName.split(",");
                                                    if (strArrs != null) {
                                                        int length = strArrs.length;
                                                        int i = 0;
                                                        while (i < length) {
                                                            try {
                                                                String strTemp = strArrs[i];
                                                                String cnName2 = cnName;
                                                                try {
                                                                    deskTopAppCnNameList.add(strTemp);
                                                                    i++;
                                                                    cnName = cnName2;
                                                                } catch (Exception e) {
                                                                }
                                                            } catch (Exception e2) {
                                                            }
                                                        }
                                                    }
                                                } catch (Exception e3) {
                                                }
                                            }
                                            if (!forbidDesktopAppBlackMapTemp.containsKey(apkName)) {
                                                ForbidDeskTopAppInfo forbidDeskTopAppInfo = new ForbidDeskTopAppInfo(apkName, deskTopAppCnNameList, isOpen.booleanValue());
                                                forbidDesktopAppBlackMapTemp.put(apkName, forbidDeskTopAppInfo);
                                            }
                                        }
                                    } else {
                                        adbUninstallBlackListTemp = adbUninstallBlackListTemp2;
                                        idleOptimizePackageListTemp = idleOptimizePackageListTemp2;
                                        if ("isForbidBrushInstallShowToast".equals(name)) {
                                            String value8 = parser.getAttributeValue(null, "name");
                                            if (DEBUG_FOR_FB_APL) {
                                                VSlog.i(TAG, " is forbid install showToast?" + value8);
                                            }
                                            if (value8 != null) {
                                                synchronized (this.mForbidObject) {
                                                    this.mIsForbidBrushInstallShowToast = Boolean.parseBoolean(value8);
                                                }
                                            }
                                        } else if ("forbidUsbBrushAppWarningToast".equals(name)) {
                                            synchronized (this.mForbidObject) {
                                                this.mForbidUsbBrushAppWarningToast = parser.getAttributeValue(null, "name");
                                            }
                                        } else if ("forbidBrushToolsOverTime".equals(name)) {
                                            String value9 = parser.getAttributeValue(null, "name");
                                            if (this.DEBUG_FOR_ALL) {
                                                VSlog.i(TAG, " forbidBrushTools overtime " + value9);
                                            }
                                            if (value9 != null) {
                                                try {
                                                    synchronized (this.mForbidObject) {
                                                        this.mForbidBrushToolsOverTime = Long.parseLong(value9);
                                                    }
                                                } catch (Exception e4) {
                                                    VSlog.w(TAG, "exception1 " + e4.toString());
                                                }
                                            }
                                        } else if ("forbidDestTopAppOverTime".equals(name)) {
                                            String value10 = parser.getAttributeValue(null, "name");
                                            if (this.DEBUG_FOR_ALL) {
                                                VSlog.i(TAG, " forbidBrush DestTop app overtime " + value10);
                                            }
                                            if (value10 != null) {
                                                try {
                                                    synchronized (this.mForbidObject) {
                                                        this.mForbidDestTopAppOverTime = Long.parseLong(value10);
                                                    }
                                                } catch (Exception e5) {
                                                    VSlog.w(TAG, "exception2 " + e5.toString());
                                                }
                                            }
                                        } else if ("isCollectLogToLocalOpen".equals(name)) {
                                            String value11 = parser.getAttributeValue(null, "name");
                                            if (this.DEBUG_FOR_ALL) {
                                                VSlog.i(TAG, " vcd log is open, " + value11);
                                            }
                                            if (value11 != null) {
                                                try {
                                                    synchronized (this.mLogInfoLocalLock) {
                                                        VCD_INFO_TO_LOCAL_OPEN = Boolean.parseBoolean(value11);
                                                    }
                                                } catch (Exception e6) {
                                                    VSlog.w(TAG, "log open excp,  " + e6.toString());
                                                }
                                            }
                                        } else if ("collectLogMaxRecordCount".equals(name)) {
                                            String value12 = parser.getAttributeValue(null, "name");
                                            if (this.DEBUG_FOR_ALL) {
                                                VSlog.i(TAG, " max record count, " + value12);
                                            }
                                            if (value12 != null) {
                                                try {
                                                    synchronized (this.mLogInfoLocalLock) {
                                                        DEFAULT_MAX_RECORD_COUNT = Integer.parseInt(value12);
                                                    }
                                                } catch (Exception e7) {
                                                    VSlog.w(TAG, "max count excp, " + e7.toString());
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (eventCode2 != 3) {
                                idleOptimizePackageListTemp = idleOptimizePackageListTemp2;
                                adbUninstallBlackListTemp = adbUninstallBlackListTemp3;
                            } else {
                                parser.getName().equals("package");
                                idleOptimizePackageListTemp = idleOptimizePackageListTemp2;
                                adbUninstallBlackListTemp = adbUninstallBlackListTemp3;
                            }
                            eventCode2 = parser.next();
                            pullFactory = pullFactory2;
                            adbUninstallBlackListTemp3 = adbUninstallBlackListTemp;
                            idleOptimizePackageListTemp2 = idleOptimizePackageListTemp;
                        }
                    } catch (XmlPullParserException e8) {
                        e8.printStackTrace();
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    }
                } catch (Throwable th) {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (Exception e9) {
                            e9.printStackTrace();
                        }
                    }
                    throw th;
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        } catch (Exception e10) {
            e10.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class VivoLogReceiver extends BroadcastReceiver {
        public VivoLogReceiver() {
            IntentFilter filter = new IntentFilter();
            filter.addAction(VivoPolicyConstant.ACTION_VIVO_LOG_CHANGED);
            VivoPKMSUtils.this.mContext.registerReceiver(this, filter);
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            boolean logCtrl = SystemProperties.get("persist.sys.log.ctrl", "no").equals("yes");
            PackageManagerService.IS_LOG_CTRL_OPEN = logCtrl;
            PackageManagerService.DEBUG = logCtrl;
            PackageManagerService.DEBUG_INSTALL = logCtrl;
            PackageManagerService.DEBUG_REMOVE = logCtrl;
            PackageManagerService.DEBUG_UPGRADE = logCtrl;
            VivoPKMSUtils.DEBUG = logCtrl;
            VSlog.d(VivoPKMSUtils.TAG, "VivoLogReceiver  log.ctrl:" + logCtrl + " printLog " + PackageManagerService.IS_LOG_CTRL_OPEN);
        }
    }

    int preCheckUidPermission(String permName, int uid) {
        if (PackageManagerService.DEBUG_FOR_ALL) {
            VSlog.d(TAG, "preCheckUidPermission  " + permName + " uid:" + uid + " bootDone:" + mDeviceBootComplete + " AdbSimulate:" + this.mIsSettingAllowAdbSimulateInput + " adbShelPro:" + ADB_SHELL_SIMULATE_PROP + " isMonkey:" + this.mIsMonkeyTest);
        }
        if (mDeviceBootComplete && uid == 2000 && INTERCEPT_SHELL_PERMISSION_LIST.contains(permName)) {
            if (this.mIsSettingAllowAdbSimulateInput || this.mIsMonkeyTest || ADB_SHELL_SIMULATE_PROP) {
                if (DEBUG) {
                    VSlog.w(TAG, "preCheckUidPermission special Test will return allow   " + this.mIsSettingAllowAdbSimulateInput + " " + ADB_SHELL_SIMULATE_PROP);
                }
                return 0;
            } else if (checkDeviceIsRuWeiOrNetEntry()) {
                return 0;
            } else {
                if (DEBUG) {
                    VSlog.d(TAG, "Not allow " + uid + " to use " + permName + " " + ADB_SHELL_SIMULATE_PROP);
                    return -100;
                }
                return -100;
            }
        }
        return 0;
    }

    private boolean checkDeviceIsRuWeiOrNetEntry() {
        boolean isRW = false;
        isRW = (OP_ENTRY.contains("RW") || OP_ENTRY.equals("CMCC") || OP_ENTRY.equals("CTCC") || OP_ENTRY.equals("UNICOM")) ? true : true;
        if (isRW || FEATURE_FOR_NET || PackageManagerService.DEBUG_FOR_ALL) {
            StringBuilder sb = new StringBuilder();
            sb.append("Rw: ");
            sb.append(OP_ENTRY);
            sb.append(" netlock:");
            sb.append(FEATURE_FOR_NET);
            sb.append(" result :");
            sb.append(isRW || FEATURE_FOR_NET);
            VSlog.d(TAG, sb.toString());
        }
        return isRW || FEATURE_FOR_NET;
    }

    private void registerSettingsObsever() {
        String adbSimulateInit = SystemProperties.get("persist.sys.adb.simulate.ic", "unknow");
        VSlog.i(TAG, "registerSettingsObsever  adbSimulateInit " + adbSimulateInit);
        if (!"true".equals(adbSimulateInit)) {
            VSlog.i(TAG, "registerSettingsObsever will set prop  *.simulate.ic true");
            SystemProperties.set("persist.sys.adb.simulate.ic", "true");
        }
        Settings.Secure.putInt(this.mContext.getContentResolver(), "vivo_monkey_test", 0);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("vivo_monkey_test"), true, new ContentObserver(null) { // from class: com.android.server.pm.VivoPKMSUtils.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                VivoPKMSUtils vivoPKMSUtils = VivoPKMSUtils.this;
                vivoPKMSUtils.mIsMonkeyTest = Settings.Secure.getInt(vivoPKMSUtils.mContext.getContentResolver(), "vivo_monkey_test", 0) == 1;
                VSlog.i(VivoPKMSUtils.TAG, "Observer change, Is special test  ? " + VivoPKMSUtils.this.mIsMonkeyTest);
            }
        }, -1);
        this.mIsSettingAllowAdbSimulateInput = Settings.Secure.getInt(this.mContext.getContentResolver(), "vivo_adb_simulate_input", 0) == 1;
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("vivo_adb_simulate_input"), true, new ContentObserver(null) { // from class: com.android.server.pm.VivoPKMSUtils.3
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                VivoPKMSUtils vivoPKMSUtils = VivoPKMSUtils.this;
                vivoPKMSUtils.mIsSettingAllowAdbSimulateInput = Settings.Secure.getInt(vivoPKMSUtils.mContext.getContentResolver(), "vivo_adb_simulate_input", 0) == 1;
                VSlog.i(VivoPKMSUtils.TAG, "Observer change, Is allow adb simulate input  ? " + VivoPKMSUtils.this.mIsSettingAllowAdbSimulateInput);
            }
        }, -1);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean writeDeletedPkgToLocalFile(String filePath, List<String> pkgList, String delPkgName, boolean append) {
        if (DEBUG) {
            for (String pkgName : pkgList) {
                VSlog.d(TAG, "writeToFile pkgName:" + pkgName + " append:" + append);
            }
        }
        boolean result = false;
        File pkgFile = new File(filePath);
        if (!pkgFile.exists()) {
            try {
                pkgFile.createNewFile();
            } catch (IOException e) {
                VSlog.e(TAG, "create new file catche exception. " + e.toString());
            }
        }
        if (DEBUG) {
            VSlog.d(TAG, " filePath:" + filePath + " exist:" + pkgFile.exists());
        }
        if (pkgFile.exists()) {
            BufferedWriter bufferWriter = null;
            try {
                try {
                    try {
                        bufferWriter = new BufferedWriter(new FileWriter(pkgFile, append));
                        if (pkgList != null && pkgList.size() > 0) {
                            for (String packageName : pkgList) {
                                bufferWriter.write(packageName);
                                bufferWriter.newLine();
                            }
                        } else {
                            bufferWriter.write(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                        }
                        bufferWriter.flush();
                        result = true;
                        bufferWriter.close();
                    } catch (IOException e2) {
                        VSlog.e(TAG, "write catch exception " + e2.toString());
                        if (bufferWriter != null) {
                            bufferWriter.close();
                        }
                    }
                } catch (Throwable th) {
                    if (bufferWriter != null) {
                        try {
                            bufferWriter.close();
                        } catch (IOException e3) {
                        }
                    }
                    throw th;
                }
            } catch (IOException e4) {
            }
        }
        if (DEBUG) {
            VSlog.i(TAG, "Del " + delPkgName + " " + result);
        }
        return result;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public static ArrayList<String> getDeletedPkgListFromLocalFile(String filePath) {
        ArrayList<String> delPkgList = new ArrayList<>();
        File apkListFile = new File(filePath);
        if (apkListFile.exists() && 0 != apkListFile.length()) {
            BufferedReader reader = null;
            try {
                try {
                    try {
                        reader = new BufferedReader(new FileReader(apkListFile));
                        while (true) {
                            String pkgName = reader.readLine();
                            if (pkgName == null) {
                                break;
                            } else if (pkgName != null && pkgName.length() != 0) {
                                delPkgList.add(pkgName);
                            } else {
                                VSlog.d(TAG, " pkgName length is 0 pkgName:" + pkgName);
                            }
                        }
                        reader.close();
                    } catch (Throwable th) {
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (IOException e) {
                            }
                        }
                        throw th;
                    }
                } catch (IOException e2) {
                    e2.printStackTrace();
                    if (reader != null) {
                        reader.close();
                    }
                }
            } catch (IOException e3) {
            }
        } else {
            VSlog.w(TAG, filePath + " Not exist!");
        }
        return delPkgList;
    }

    boolean InitBuiltIn3PartAppPathFromDataVivoAppsIfNeed(String oemName, String path) {
        String defaultNewBuiltInApkPath = null;
        if (oemName != null && !oemName.equals("unknow")) {
            String appsOemList = "apps_" + oemName + ".list";
            VSlog.i(TAG, "oemName:" + oemName + " path:" + appsOemList);
            try {
                File appsOemListFile = new File(path + "/" + appsOemList);
                if (appsOemListFile.exists()) {
                    defaultNewBuiltInApkPath = appsOemListFile.getAbsolutePath();
                    VSlog.w(TAG, "apps list file path " + defaultNewBuiltInApkPath);
                }
            } catch (Exception e) {
                VSlog.e(TAG, "ee1 " + e.toString());
            }
            if (defaultNewBuiltInApkPath == null) {
                try {
                    defaultNewBuiltInApkPath = findBuiltInAppListAbsolutePath("/data/vivo-apps");
                } catch (Exception e2) {
                    VSlog.e(TAG, "ee1 " + e2.toString());
                }
            }
            if (defaultNewBuiltInApkPath != null) {
                backupBuiltInAppList(appsOemList);
            }
        }
        if (defaultNewBuiltInApkPath == null) {
            File defaultNewPath = new File(path + "/apps.list");
            VSlog.w(TAG, "oemApk list not exist, use default " + defaultNewPath);
            if (defaultNewPath.exists()) {
                defaultNewBuiltInApkPath = defaultNewPath.getAbsolutePath();
                backupBuiltInAppList("apps.list");
            }
        }
        VSlog.i(TAG, oemName + " " + path + " apkListAbsolutePath:" + defaultNewBuiltInApkPath);
        if (defaultNewBuiltInApkPath != null) {
            File defaultNewBuiltInApkFile = new File(defaultNewBuiltInApkPath);
            if (defaultNewBuiltInApkFile.exists()) {
                try {
                    InitBuiltIn3PartAppPathListInner(defaultNewBuiltInApkFile);
                } catch (Exception e3) {
                }
            } else {
                VSlog.i(TAG, "#### " + defaultNewBuiltInApkFile + " not exist.");
            }
        } else {
            VSlog.w(TAG, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + path + " is Not exist!");
        }
        if (oemName != null && !oemName.equals("unknow") && !is3PartAppListAvailable()) {
            this.mBuiltIn3PartAppPathList.clear();
            this.mBuiltIn3PartApkNameList.clear();
            InitBuiltIn3PartAppPathListInnerFromBackup(oemName);
        }
        if (this.mBuiltIn3PartAppPathList.isEmpty()) {
            this.mBuiltIn3PartApkNameList.clear();
            InitBuiltIn3PartAppPathListFiles();
        }
        if (this.mBuiltIn3PartAppPathList.size() > 0) {
            return true;
        }
        return false;
    }

    private void backupBuiltInAppList(String appsList) {
        File appsBackup = new File(VivoUtils.VIVO_POLICIES_PATH + appsList + ".bak");
        if (!appsBackup.exists()) {
            copyFile(new File("/data/vivo-apps/" + appsList), appsBackup);
            VSlog.w(TAG, "backup oemApk list.");
        }
    }

    private boolean is3PartAppListAvailable() {
        if (this.mBuiltIn3PartAppPathList.size() <= 0) {
            return false;
        }
        Iterator<String> it = this.mBuiltIn3PartAppPathList.iterator();
        while (it.hasNext()) {
            String app = it.next();
            if (!app.endsWith(".apk")) {
                return false;
            }
        }
        return true;
    }

    private void InitBuiltIn3PartAppPathListInnerFromBackup(String oemName) {
        String appsOemListBackup = "/data/system/apps_" + oemName + ".list.bak";
        File appsBackupFile = new File(appsOemListBackup);
        if (!appsBackupFile.exists()) {
            appsBackupFile = new File("/data/system/apps.list.bak");
        }
        if (appsBackupFile.exists()) {
            try {
                VSlog.w(TAG, "get oem apps from back file .");
                InitBuiltIn3PartAppPathListInner(appsBackupFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void InitBuiltIn3PartAppPathListFiles() {
        File[] fileList;
        File appListFile = new File("/data/vivo-apps");
        if (appListFile.exists() && appListFile.isDirectory() && (fileList = appListFile.listFiles()) != null) {
            for (File fileTemp : fileList) {
                VSlog.i(TAG, "#check# " + fileTemp);
                if (fileTemp != null && fileTemp.isDirectory()) {
                    String fileName = fileTemp.getName();
                    this.mBuiltIn3PartApkNameList.add(fileName);
                    VSlog.i(TAG, "find apkname:" + fileName);
                    File[] apkList = fileTemp.listFiles();
                    if (apkList != null) {
                        for (File apkFile : apkList) {
                            String fileName2 = apkFile.getName();
                            if (fileName2.endsWith(".apk")) {
                                this.mBuiltIn3PartAppPathList.add(fileName2);
                                VSlog.i(TAG, "find apk:" + fileName2);
                            }
                        }
                    }
                }
            }
        }
    }

    private void copyFile(File src, File dest) {
        BufferedWriter writer = null;
        BufferedReader reader = null;
        try {
            try {
                writer = new BufferedWriter(new FileWriter(dest));
                reader = new BufferedReader(new FileReader(src));
                while (true) {
                    String lineStr = reader.readLine();
                    if (lineStr == null) {
                        break;
                    }
                    writer.write(lineStr);
                    writer.newLine();
                    writer.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            IoUtils.closeQuietly(reader);
            IoUtils.closeQuietly(writer);
        }
    }

    private String findBuiltInAppListAbsolutePath(String findPath) {
        File[] fileList;
        String fileName;
        File appListFile = new File(findPath);
        if (appListFile.exists() && (fileList = appListFile.listFiles()) != null) {
            for (File fileTemp : fileList) {
                VSlog.i(TAG, "#check# " + fileTemp);
                if (fileTemp != null && !fileTemp.isDirectory() && (fileName = fileTemp.getName()) != null && fileName.endsWith(".list") && fileName.startsWith("apps") && !"apps.list".equals(fileName)) {
                    VSlog.i(TAG, "find " + fileName);
                    return fileTemp.getAbsolutePath();
                }
            }
        }
        VSlog.w(TAG, "Not found apps_*.list in /data/vivo-apps");
        return null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArrayList<String> getBuiltIn3PartAppPkgNameList() {
        return this.mBuiltIn3PartApkNameList;
    }

    /* JADX WARN: Removed duplicated region for block: B:52:0x01db  */
    /* JADX WARN: Removed duplicated region for block: B:66:0x01bf A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private void InitBuiltIn3PartAppPathList() {
        /*
            Method dump skipped, instructions count: 501
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.VivoPKMSUtils.InitBuiltIn3PartAppPathList():void");
    }

    private void InitBuiltIn3PartAppPathListInner(File file) {
        if (DEBUG) {
            VSlog.i(TAG, "init app_path " + file);
        }
        BufferedReader reader = null;
        try {
            try {
                try {
                    reader = new BufferedReader(new FileReader(file));
                    while (true) {
                        String apkPathTemp = reader.readLine();
                        if (apkPathTemp == null) {
                            break;
                        }
                        if (DEBUG) {
                            VSlog.d(TAG, "#0parser  " + apkPathTemp);
                        }
                        if (apkPathTemp != null && apkPathTemp.length() != 0) {
                            try {
                                parserApkPath(apkPathTemp);
                            } catch (Exception e) {
                            }
                        }
                    }
                    reader.close();
                } catch (Throwable th) {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (Exception e2) {
                        }
                    }
                    throw th;
                }
            } catch (Exception e3) {
                VSlog.e(TAG, "Init buildtIn app path catch exception " + e3.toString());
                if (reader != null) {
                    reader.close();
                }
            }
        } catch (Exception e4) {
        }
        if (DEBUG) {
            VSlog.d(TAG, "appPath:" + this.mBuiltIn3PartAppPathList + " \n appName:" + this.mBuiltIn3PartApkNameList);
        }
    }

    private void parserApkPath(String apkPath) {
        if (apkPath == null) {
            VSlog.w(TAG, "## path is null!");
            return;
        }
        String[] pathStr = apkPath.split(" ");
        if (pathStr != null && pathStr.length > 0) {
            String apkPathTemp = pathStr[0];
            if (DEBUG) {
                VSlog.d(TAG, "#1parser  apkPathTemp " + apkPathTemp);
            }
            if (apkPathTemp != null && apkPathTemp.contains("/")) {
                String[] apkInfoStr = apkPathTemp.split("\\/");
                if (apkInfoStr != null && apkInfoStr.length == 2) {
                    if (DEBUG) {
                        VSlog.d(TAG, "##1 apkPathTemp " + apkPathTemp + " pkgName :" + apkInfoStr[0] + ", Add to list apkName:" + apkInfoStr[1]);
                    }
                    this.mBuiltIn3PartAppPathList.add(apkInfoStr[1]);
                    this.mBuiltIn3PartApkNameList.add(apkInfoStr[0]);
                    return;
                }
                return;
            }
            if (DEBUG) {
                VSlog.d(TAG, "###1 apkPathTemp:" + apkPathTemp);
            }
            if (apkPathTemp != null && apkPathTemp.length() > 0) {
                this.mBuiltIn3PartAppPathList.add(apkPathTemp);
            }
        }
    }

    private void parseSpecialApkPath(String apkPath) {
        File[] appFilesLoop1;
        File[] appFilesLoop2;
        if (apkPath != null) {
            File specialAppFile = new File(apkPath);
            if (!specialAppFile.exists() || (appFilesLoop1 = specialAppFile.listFiles()) == null) {
                return;
            }
            for (File fileLoop1Temp : appFilesLoop1) {
                if (fileLoop1Temp != null && fileLoop1Temp.isDirectory() && (appFilesLoop2 = fileLoop1Temp.listFiles()) != null) {
                    for (File fileLoop2Temp : appFilesLoop2) {
                        if (fileLoop2Temp != null) {
                            String apkFilePath = fileLoop2Temp.getAbsolutePath();
                            if (apkFilePath.endsWith("apk")) {
                                String[] apkInfos = apkFilePath.split("\\/");
                                if (apkInfos != null && apkInfos.length > 1) {
                                    int max = apkInfos.length;
                                    this.mBuiltIn3PartAppPathList.add(apkInfos[max - 1]);
                                    if (DEBUG) {
                                        VSlog.d(TAG, "Add_special_apk " + apkFilePath);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return;
        }
        VSlog.w(TAG, "Special path " + apkPath + " not exist!");
    }

    ArrayList<String> getBuiltIn3PartAppPathList() {
        return this.mBuiltIn3PartAppPathList;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class VSysAppInstallParam {
        int installFlags;
        final IPackageInstallObserver2 observer;
        final String packageName;
        private int result;
        final PackageManagerService.VerificationInfo verificationInfo;

        public VSysAppInstallParam(String packageName, IPackageInstallObserver2 observer, int installFlags, int result, PackageManagerService.VerificationInfo verificationInfo) {
            this.packageName = packageName;
            this.observer = observer;
            this.installFlags = installFlags;
            this.result = result;
            this.verificationInfo = verificationInfo;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + this.packageName + " " + this.installFlags);
            if (this.verificationInfo.originatingUri != null) {
                sb.append(" oUir:" + this.verificationInfo.originatingUri);
            }
            sb.append(" oUid:" + this.verificationInfo.originatingUid + " installUid:" + this.verificationInfo.installerUid);
            return sb.toString();
        }
    }

    ArrayList<String> initCustomSysAppConfigFromLocal(String configFilePath) {
        if (configFilePath == null) {
            return new ArrayList<>();
        }
        ArrayList<String> customSysAppList = new ArrayList<>();
        File customPrivAppFile = new File(configFilePath, "priv-app");
        if (customPrivAppFile.exists()) {
            try {
                ArrayList<String> tmpList = scanCustomSysAppDir(customPrivAppFile);
                if (tmpList != null && tmpList.size() > 0) {
                    customSysAppList.addAll(tmpList);
                }
            } catch (Exception e) {
                VSlog.e(TAG, "scan " + customPrivAppFile + " catch exception " + e.toString());
            }
        }
        File customSysAppFile = new File(configFilePath, "app");
        if (customSysAppFile.exists()) {
            try {
                ArrayList<String> tmpList2 = scanCustomSysAppDir(customSysAppFile);
                if (tmpList2 != null && tmpList2.size() > 0) {
                    customSysAppList.addAll(tmpList2);
                }
            } catch (Exception e2) {
                VSlog.e(TAG, "scan " + customSysAppFile + " catch exception " + e2.toString());
            }
        }
        if (DEBUG) {
            VSlog.d(TAG, "init_custom_app " + customSysAppList);
        }
        return customSysAppList;
    }

    ArrayList<String> scanCustomSysAppDir(File appFileDir) {
        File[] files = appFileDir.listFiles();
        if (ArrayUtils.isEmpty(files)) {
            VLog.d(TAG, "No files in app dir " + appFileDir);
            return null;
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean writeDeletedSysPkgToLocalFile(String filePath, List<String> pkgList, String delPkgName, boolean append) {
        for (String pkgName : pkgList) {
            VSlog.d(TAG, "writeToFile pkgName:" + pkgName + " append:" + append);
        }
        boolean result = false;
        File pkgFile = new File(filePath);
        if (!pkgFile.exists()) {
            try {
                pkgFile.createNewFile();
            } catch (IOException e) {
                VSlog.e(TAG, "create new file catche exception. " + e.toString());
            }
        }
        VSlog.d(TAG, " filePath:" + filePath + " exist:" + pkgFile.exists());
        if (pkgFile.exists()) {
            BufferedWriter bufferWriter = null;
            try {
                try {
                    try {
                        bufferWriter = new BufferedWriter(new FileWriter(pkgFile, append));
                        if (pkgList != null && pkgList.size() > 0) {
                            for (String packageName : pkgList) {
                                bufferWriter.write(packageName);
                                bufferWriter.newLine();
                            }
                        } else {
                            bufferWriter.write(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                        }
                        bufferWriter.flush();
                        result = true;
                        bufferWriter.close();
                    } catch (IOException e2) {
                        VSlog.e(TAG, "write catch exception " + e2.toString());
                        if (bufferWriter != null) {
                            bufferWriter.close();
                        }
                    }
                } catch (IOException e3) {
                }
            } catch (Throwable th) {
                if (0 != 0) {
                    try {
                        bufferWriter.close();
                    } catch (IOException e4) {
                    }
                }
                throw th;
            }
        }
        VSlog.i(TAG, "Del " + delPkgName + " " + result);
        return result;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public ArrayList<String> getDeletedSysPkgListFromLocalFile() {
        ArrayList<String> delPkgList = new ArrayList<>();
        File sysApkListFile = new File("data/system/v_deleted_sys_app.xml");
        if (sysApkListFile.exists() && 0 != sysApkListFile.length()) {
            BufferedReader reader = null;
            try {
                try {
                    try {
                        reader = new BufferedReader(new FileReader(sysApkListFile));
                        while (true) {
                            String pkgName = reader.readLine();
                            if (pkgName == null) {
                                break;
                            } else if (pkgName != null && pkgName.length() != 0) {
                                delPkgList.add(pkgName);
                            } else {
                                VSlog.d(TAG, " pkgName length is 0 pkgName:" + pkgName);
                            }
                        }
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        if (reader != null) {
                            reader.close();
                        }
                    }
                } catch (IOException e2) {
                }
            } catch (Throwable th) {
                if (0 != 0) {
                    try {
                        reader.close();
                    } catch (IOException e3) {
                    }
                }
                throw th;
            }
        } else {
            VSlog.i(TAG, "Sys app All exist!");
        }
        Iterator<String> it = delPkgList.iterator();
        while (it.hasNext()) {
            VSlog.d(TAG, "LocalDel pkgname:" + it.next());
        }
        return delPkgList;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendApkInstalledBroadcast(boolean isInstallSilence, String installerPkg, String installerVersion, String pkgName, boolean isUpdate, String extra, DeviceBootInfo deviceBootInfo) {
        InstallApkBaseInfo installApkInfo = new InstallApkBaseInfo(isInstallSilence, installerPkg, installerVersion, pkgName, isUpdate, extra, deviceBootInfo);
        Message msg = this.mPkmsUtilsHandler.obtainMessage(2000);
        msg.obj = installApkInfo;
        VSlog.i(TAG, "isOVsea " + mIsOverseas + " deviceBC " + mDeviceBootComplete + " install, check something...");
        if (mIsOverseas || !mDeviceBootComplete) {
            this.mPkmsUtilsHandler.sendMessage(msg);
        } else if (this.mPkmsLocMgr.isLastLocationValid()) {
            VSlog.i(TAG, "last is valid...");
            this.mPkmsUtilsHandler.sendMessage(msg);
        } else {
            VSlog.i(TAG, "schedule request new ...");
            this.mPkmsLocMgr.scheduleNetworkLocation();
            this.mPkmsUtilsHandler.sendMessageDelayed(msg, 3000L);
        }
    }

    void sendApkInstalledBroadcastInner(InstallApkBaseInfo apkInfo) {
        long seqNum;
        String location;
        boolean hasLocation;
        String latitudeAndLongitude;
        String latitudeAndLongitude2;
        String rgcAddress;
        if (apkInfo.isUpdate) {
            long seqNum2 = getUpdateApkNextSeqNumber();
            seqNum = seqNum2;
        } else {
            long seqNum3 = getInstallApkNextSeqNumber();
            seqNum = seqNum3;
        }
        String extraTemp = buildApkInstallExtraInfo(apkInfo.isInstallSilence, apkInfo.extra, seqNum, apkInfo.isUpdate);
        String pkgCNName = getPackageCNName(apkInfo.pkgName);
        Bundle myExtras = new Bundle(1);
        myExtras.putLong("bootElapsedTime", apkInfo.deviceBootInfo.bootElapsedTime);
        myExtras.putInt("bootCount", apkInfo.deviceBootInfo.bootCount);
        myExtras.putString("installerPkg", apkInfo.installerPkg);
        myExtras.putString("installerVersion", apkInfo.installerVersion);
        myExtras.putString("pkgName", apkInfo.pkgName);
        myExtras.putString("pkgCNName", pkgCNName);
        myExtras.putInt("isUpdate", apkInfo.isUpdate ? 1 : 0);
        myExtras.putString("extra", extraTemp);
        Intent myIntent = new Intent("vivo.intent.action.PACKAGE_INSTALLED");
        myIntent.addFlags(67108864);
        myIntent.setPackage("com.vivo.assistant");
        String netType = VivoPKMSCommonUtils.getDeviceNetworkType(DEBUG, this.mContext);
        String deviceID = readDeviceID();
        String rgcAddress2 = null;
        if (!mIsOverseas && mDeviceBootComplete) {
            rgcAddress2 = this.mPkmsLocMgr.getLocationRgcAddress();
            String latitudeAndLongitude3 = this.mPkmsLocMgr.getLatitudeAndLongitude();
            if (!TextUtils.isEmpty(rgcAddress2)) {
                location = rgcAddress2;
                hasLocation = true;
                latitudeAndLongitude = latitudeAndLongitude3;
            } else if (!TextUtils.isEmpty(latitudeAndLongitude3)) {
                location = latitudeAndLongitude3;
                hasLocation = true;
                latitudeAndLongitude = latitudeAndLongitude3;
            } else {
                String location2 = getDeviceWifiScanList();
                location = location2;
                hasLocation = false;
                latitudeAndLongitude = latitudeAndLongitude3;
            }
        } else {
            location = null;
            hasLocation = false;
            latitudeAndLongitude = null;
        }
        Intent installIntent = new Intent("vivo.intent.action.BEHAVIOR_APPSTORE_PACKAGE_INSTALLED");
        installIntent.addFlags(67108864);
        installIntent.setPackage("com.bbk.appstore");
        String installerCNName = getPackageCNName(apkInfo.installerPkg);
        HashMap<String, String> pkgInfoMap = getPackageVersionInfo(apkInfo.pkgName);
        HashMap<String, String> installerPkgInfoMap = getPackageVersionInfo(apkInfo.installerPkg);
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("pTV", "v1");
        StringBuilder sb = new StringBuilder();
        sb.append(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        String latitudeAndLongitude4 = latitudeAndLongitude;
        String rgcAddress3 = rgcAddress2;
        sb.append(apkInfo.deviceBootInfo.bootElapsedTime);
        hashMap.put("bET", sb.toString());
        hashMap.put("bCt", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + apkInfo.deviceBootInfo.bootCount);
        hashMap.put("ctT", formatDate(System.currentTimeMillis()));
        hashMap.put("dID", deviceID);
        hashMap.put("ipN", apkInfo.installerPkg);
        hashMap.put("ipCN", installerCNName);
        hashMap.put("ipVN", apkInfo.installerVersion);
        hashMap.put("ipVC", installerPkgInfoMap.get("versionCode"));
        hashMap.put("update", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + (apkInfo.isUpdate ? 1 : 0));
        hashMap.put("seqN", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + seqNum);
        hashMap.put("pN", apkInfo.pkgName);
        hashMap.put("pCN", pkgCNName);
        hashMap.put("pVC", pkgInfoMap.get("versionCode"));
        hashMap.put("pVN", pkgInfoMap.get("versionName"));
        hashMap.put("si", apkInfo.isInstallSilence ? "1" : "0");
        hashMap.put("nt", netType);
        boolean asiModEnable = isAccessibilityEnable(this.mContext, apkInfo.installerPkg);
        hashMap.put("asiMod", asiModEnable ? "1" : "0");
        if (mIsOverseas) {
            latitudeAndLongitude2 = latitudeAndLongitude4;
            rgcAddress = rgcAddress3;
        } else {
            if (!hasLocation) {
                hashMap.put("wifiList", location);
            }
            rgcAddress = rgcAddress3;
            hashMap.put("rgc", rgcAddress);
            latitudeAndLongitude2 = latitudeAndLongitude4;
            hashMap.put("lat_lg", latitudeAndLongitude2);
        }
        myExtras.putSerializable("hashMap", hashMap);
        installIntent.putExtras(myExtras);
        VSlog.i(TAG, apkInfo.toString());
        this.mContext.sendBroadcastAsUser(installIntent, UserHandle.ALL, "android.permission.INSTALL_PACKAGES");
        this.mContext.sendBroadcastAsUser(myIntent, UserHandle.ALL, "android.permission.INSTALL_PACKAGES");
        boolean z = apkInfo.isInstallSilence;
        String str = apkInfo.installerPkg;
        String str2 = apkInfo.installerVersion;
        String latitudeAndLongitude5 = apkInfo.pkgName;
        writeInstallApkLog(z, str, str2, latitudeAndLongitude5, apkInfo.isUpdate, extraTemp, apkInfo.deviceBootInfo, seqNum, netType, deviceID, location, pkgCNName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void sendApkUnInstalledBroadcast(String uninstallerPkg, String pkgName, String pkgVersion, long firstInstallTime, long lastUpdateTime, int returnCode, String extra, DeviceBootInfo deviceBootInfo) {
        UnInstallApkBaseInfo unInstallApkInfo = new UnInstallApkBaseInfo(uninstallerPkg, pkgName, pkgVersion, firstInstallTime, lastUpdateTime, returnCode, extra, deviceBootInfo);
        Message msg = this.mPkmsUtilsHandler.obtainMessage(MSG_VCD_APK_UN_INSTALL_LOG);
        msg.obj = unInstallApkInfo;
        VSlog.i(TAG, "isOVsea " + mIsOverseas + " deviceBC:" + mDeviceBootComplete + " " + uninstallerPkg + "  uninstall, check something...  extra:" + extra);
        if (mIsOverseas || !mDeviceBootComplete) {
            this.mPkmsUtilsHandler.sendMessage(msg);
        } else if (this.mPkmsLocMgr.isLastLocationValid()) {
            VSlog.i(TAG, "last is valid...");
            this.mPkmsUtilsHandler.sendMessage(msg);
        } else {
            VSlog.i(TAG, "schedule request new ...");
            this.mPkmsLocMgr.scheduleNetworkLocation();
            this.mPkmsUtilsHandler.sendMessageDelayed(msg, 3000L);
        }
    }

    void sendApkUnInstalledBroadcastInner(UnInstallApkBaseInfo apkInfo) {
        String location;
        String rgcAddress;
        boolean hasLocation;
        String latitudeAndLongitude;
        String latitudeAndLongitude2;
        Intent myIntent;
        long unInstallSeq = getUnInstallApkNextSeqNumber();
        String extraTemp = buildApkUninstallExtraInfo(apkInfo.extra, unInstallSeq);
        Bundle myExtras = new Bundle(1);
        myExtras.putLong("bootElapsedTime", apkInfo.deviceBootInfo.bootElapsedTime);
        myExtras.putInt("bootCount", apkInfo.deviceBootInfo.bootCount);
        myExtras.putString("uninstallerPkg", apkInfo.uninstallerPkg);
        myExtras.putString("pkgName", apkInfo.pkgName);
        myExtras.putString("pkgVersion", apkInfo.pkgVersion);
        myExtras.putLong("firstInstallTime", apkInfo.firstInstallTime);
        myExtras.putLong("lastUpdateTime", apkInfo.lastUpdateTime);
        myExtras.putInt("returnCode", apkInfo.returnCode);
        myExtras.putString("extra", extraTemp);
        Intent myIntent2 = new Intent("vivo.intent.action.PACKAGE_UNINSTALLED");
        myIntent2.addFlags(67108864);
        myIntent2.setPackage("com.vivo.aiengine");
        String netType = VivoPKMSCommonUtils.getDeviceNetworkType(DEBUG, this.mContext);
        String deviceID = readDeviceID();
        if (!mIsOverseas && mDeviceBootComplete) {
            String rgcAddress2 = this.mPkmsLocMgr.getLocationRgcAddress();
            String latitudeAndLongitude3 = this.mPkmsLocMgr.getLatitudeAndLongitude();
            if (!TextUtils.isEmpty(rgcAddress2)) {
                location = rgcAddress2;
                rgcAddress = rgcAddress2;
                hasLocation = true;
                latitudeAndLongitude = latitudeAndLongitude3;
            } else if (!TextUtils.isEmpty(latitudeAndLongitude3)) {
                location = latitudeAndLongitude3;
                rgcAddress = rgcAddress2;
                hasLocation = true;
                latitudeAndLongitude = latitudeAndLongitude3;
            } else {
                String location2 = getDeviceWifiScanList();
                location = location2;
                rgcAddress = rgcAddress2;
                hasLocation = false;
                latitudeAndLongitude = latitudeAndLongitude3;
            }
        } else {
            location = null;
            rgcAddress = null;
            hasLocation = false;
            latitudeAndLongitude = null;
        }
        Intent unInstallIntent = new Intent("vivo.intent.action.BEHAVIOR_APPSTORE_PACKAGE_UNINSTALLED");
        unInstallIntent.addFlags(67108864);
        unInstallIntent.setPackage("com.bbk.appstore");
        String unInstallerCNName = getPackageCNName(apkInfo.uninstallerPkg);
        HashMap<String, String> unInstallerPkgInfoMap = getPackageVersionInfo(apkInfo.uninstallerPkg);
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("pTV", "v1");
        StringBuilder sb = new StringBuilder();
        sb.append(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        String latitudeAndLongitude4 = latitudeAndLongitude;
        sb.append(apkInfo.deviceBootInfo.bootElapsedTime);
        hashMap.put("bET", sb.toString());
        hashMap.put("bCt", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + apkInfo.deviceBootInfo.bootCount);
        hashMap.put("ctT", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + formatDate(System.currentTimeMillis()));
        hashMap.put("dID", deviceID);
        hashMap.put("upN", apkInfo.uninstallerPkg);
        hashMap.put("upCN", unInstallerCNName);
        hashMap.put("upVN", unInstallerPkgInfoMap.get("versionName"));
        hashMap.put("upVC", unInstallerPkgInfoMap.get("versionCode"));
        hashMap.put("seqN", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + unInstallSeq);
        hashMap.put("pN", apkInfo.pkgName);
        hashMap.put("pVN", apkInfo.pkgVersion);
        hashMap.put("nt", netType);
        hashMap.put("fit", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + apkInfo.firstInstallTime);
        hashMap.put("lut", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + apkInfo.lastUpdateTime);
        HashMap<String, String> extraHashMap = parserUninstallApkExtraInfo(apkInfo.extra);
        hashMap.put("bInApp", extraHashMap.get("bInApp"));
        hashMap.put("sysCApp", extraHashMap.get("sysCApp"));
        hashMap.put("who", "system");
        if (mIsOverseas) {
            latitudeAndLongitude2 = latitudeAndLongitude4;
        } else {
            if (!hasLocation) {
                hashMap.put("wifiList", location);
            }
            hashMap.put("rgc", rgcAddress);
            latitudeAndLongitude2 = latitudeAndLongitude4;
            hashMap.put("lat_lg", latitudeAndLongitude2);
        }
        myExtras.putSerializable("hashMap", hashMap);
        unInstallIntent.putExtras(myExtras);
        VSlog.i(TAG, "unInstall- hashmap->  " + hashMap);
        VSlog.i(TAG, apkInfo.toString());
        if (!"com.android.packageinstaller".equals(apkInfo.uninstallerPkg)) {
            this.mContext.sendBroadcastAsUser(unInstallIntent, UserHandle.ALL, "android.permission.DELETE_PACKAGES");
            myIntent = myIntent2;
            this.mContext.sendBroadcastAsUser(myIntent, UserHandle.ALL, "android.permission.DELETE_PACKAGES");
        } else {
            VSlog.i(TAG, "Do not send br.");
            myIntent = myIntent2;
        }
        writeUnInstallApkLTL(apkInfo.uninstallerPkg, apkInfo.pkgName, apkInfo.pkgVersion, apkInfo.firstInstallTime, apkInfo.lastUpdateTime, apkInfo.returnCode, extraTemp, apkInfo.deviceBootInfo, unInstallSeq, netType, deviceID, location);
    }

    private String buildApkInstallExtraInfo(boolean isInstallSilence, String extra, long seqNum, boolean isUpdate) {
        HashMap<String, String> aMap = new HashMap<>();
        aMap.put("adb_input", this.mIsSettingAllowAdbSimulateInput ? "1" : "0");
        aMap.put("silentI", isInstallSilence ? "1" : "0");
        if (isUpdate) {
            aMap.put("updateSeqNum", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + seqNum);
        } else {
            aMap.put("installSeqNum", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + seqNum);
        }
        aMap.put("extra", extra);
        if (PackageManagerService.DEBUG_FOR_ALL) {
            VSlog.d(TAG, "aMap:" + aMap);
        }
        return aMap.toString();
    }

    private String buildApkUninstallExtraInfo(String extra, long seqNum) {
        VSlog.i(TAG, "extra1:" + extra + " " + seqNum);
        String str = extra + "delSeqNum=" + seqNum + "\t";
        return str;
    }

    private String getPackageCNName(String pkgName) {
        PackageManager packageManager = this.mContext.getPackageManager();
        try {
            ApplicationInfo appInfo = packageManager.getApplicationInfo(pkgName, 4096);
            if (appInfo != null) {
                AssetManager am = new AssetManager();
                am.addAssetPath(appInfo.publicSourceDir);
                Resources res = new Resources(am, null, null);
                return res.getString(appInfo.labelRes);
            }
            return pkgName;
        } catch (Exception e) {
            VSlog.w(TAG, "check " + pkgName + " cnName," + e.toString());
            return pkgName;
        }
    }

    private HashMap<String, String> getPackageVersionInfo(String pkgName) {
        PackageInfo pkgInfo;
        HashMap<String, String> pkgInfoMap = new HashMap<>(4);
        pkgInfoMap.put("versionCode", "unknow");
        pkgInfoMap.put("versionName", "unknow");
        PackageManager packageManager = this.mContext.getPackageManager();
        try {
            pkgInfo = packageManager.getPackageInfo(pkgName, 0);
        } catch (Exception e) {
            VSlog.w(TAG, "check " + pkgName + " cnName," + e.toString());
        }
        if (pkgInfo == null) {
            return pkgInfoMap;
        }
        pkgInfoMap.put("versionCode", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + pkgInfo.versionCode);
        pkgInfoMap.put("versionName", pkgInfo.versionName);
        VSlog.i(TAG, "pkgInfoMap:" + pkgInfoMap);
        return pkgInfoMap;
    }

    private HashMap<String, String> parserUninstallApkExtraInfo(String extra) {
        String[] strArr1;
        String[] strArr2;
        HashMap<String, String> map = new HashMap<>(4);
        map.put("bInApp", "false");
        map.put("sysCApp", "false");
        if (TextUtils.isEmpty(extra)) {
            return map;
        }
        try {
            strArr1 = extra.split("\\t");
        } catch (Exception e) {
            VSlog.w(TAG, "parse extra," + e.toString());
        }
        if (strArr1 != null && strArr1.length >= 2) {
            for (String strTemp : strArr1) {
                if (!TextUtils.isEmpty(strTemp) && (strArr2 = strTemp.split("=")) != null && strArr2.length >= 2) {
                    if ("builtInApp".equals(strArr2[0])) {
                        map.put("bInApp", strArr2[1]);
                    } else if ("sysCustomApp".equals(strArr2[1])) {
                        map.put("sysCApp", strArr2[1]);
                    }
                }
            }
            return map;
        }
        return map;
    }

    private boolean isAccessibilityEnable(Context context, String installerPkg) {
        if (TextUtils.isEmpty(installerPkg)) {
            return false;
        }
        boolean find = false;
        boolean accessibilityEnabled = Settings.Secure.getInt(context.getContentResolver(), "accessibility_enabled", 0) == 1;
        if (accessibilityEnabled) {
            String enabledServicesStr = getEnabledServicesFromSettings(this.mContext, UserHandle.myUserId());
            if (TextUtils.isEmpty(enabledServicesStr)) {
                VSlog.i(TAG, installerPkg + " not open .");
                return false;
            }
            List<AccessibilityServiceInfo> serviceInfos = AccessibilityManager.getInstance(context).getInstalledAccessibilityServiceList();
            int serviceInfoCount = serviceInfos.size();
            VSlog.i(TAG, "serviceInfoCount:" + serviceInfoCount + " enabledServicesStr:" + enabledServicesStr);
            int i = 0;
            while (true) {
                if (i >= serviceInfoCount) {
                    break;
                }
                AccessibilityServiceInfo serviceInfo = serviceInfos.get(i);
                ResolveInfo resolveInfo = serviceInfo.getResolveInfo();
                VSlog.d(TAG, "resolveInfo:" + resolveInfo);
                if (resolveInfo != null && resolveInfo.serviceInfo != null) {
                    if (installerPkg.equals(resolveInfo.serviceInfo.packageName)) {
                        if (enabledServicesStr.contains(installerPkg)) {
                            find = true;
                        }
                    } else {
                        find = false;
                    }
                }
                i++;
            }
        }
        VSlog.i(TAG, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + installerPkg + " " + find + " " + accessibilityEnabled);
        return find;
    }

    private String getEnabledServicesFromSettings(Context context, int userId) {
        String enabledServicesSetting = Settings.Secure.getStringForUser(context.getContentResolver(), "enabled_accessibility_services", userId);
        return enabledServicesSetting;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static DeviceBootInfo parserDeviceBootInfoFromProp() {
        DeviceBootInfo deviceBootInfo = new DeviceBootInfo(-1, -1L);
        try {
            String restoredValue = SystemProperties.get("persist.sys.device.run.time", "0,0");
            int bootCount = Integer.parseInt(restoredValue.substring(0, restoredValue.indexOf(",")));
            long curRunningTime = SystemClock.elapsedRealtime() / 1000;
            long bootElapsedTime = Long.parseLong(restoredValue.substring(restoredValue.indexOf(",") + 1)) + curRunningTime;
            deviceBootInfo.bootCount = bootCount;
            deviceBootInfo.bootElapsedTime = bootElapsedTime;
            if (PackageManagerService.DEBUG_FOR_ALL) {
                VSlog.d(TAG, "bootCount:" + bootCount + " bootElapsedTime:" + bootElapsedTime + " subStr1:" + restoredValue.substring(0, restoredValue.indexOf(",")) + " subStr2:" + restoredValue.substring(restoredValue.indexOf(",") + 1));
            }
        } catch (Exception e) {
            VSlog.e(TAG, "parser prop catch exception, " + e.toString());
        }
        return deviceBootInfo;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class DeviceBootInfo {
        int bootCount;
        long bootElapsedTime;

        public DeviceBootInfo() {
            this.bootCount = -1;
            this.bootElapsedTime = -1L;
        }

        public DeviceBootInfo(int bootCount, long bootElapsedTime) {
            this.bootCount = -1;
            this.bootElapsedTime = -1L;
            this.bootCount = bootCount;
            this.bootElapsedTime = bootElapsedTime;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class InstallApkBaseInfo {
        DeviceBootInfo deviceBootInfo;
        String extra;
        String installerPkg;
        String installerVersion;
        boolean isInstallSilence;
        boolean isUpdate;
        String pkgName;

        public InstallApkBaseInfo() {
        }

        public InstallApkBaseInfo(boolean isInstallSilence, String installerPkg, String installerVersion, String pkgName, boolean isUpdate, String extra, DeviceBootInfo deviceBootInfo) {
            this.isInstallSilence = isInstallSilence;
            this.installerPkg = installerPkg;
            this.installerVersion = installerVersion;
            this.pkgName = pkgName;
            this.isUpdate = isUpdate;
            this.extra = extra;
            this.deviceBootInfo = deviceBootInfo;
        }

        public String toString() {
            return this.installerPkg + " install " + this.pkgName + " ";
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class UnInstallApkBaseInfo {
        DeviceBootInfo deviceBootInfo;
        String extra;
        long firstInstallTime;
        long lastUpdateTime;
        String pkgName;
        String pkgVersion;
        int returnCode;
        String uninstallerPkg;

        public UnInstallApkBaseInfo() {
        }

        public UnInstallApkBaseInfo(String uninstallerPkg, String pkgName, String pkgVersion, long firstInstallTime, long lastUpdateTime, int returnCode, String extra, DeviceBootInfo deviceBootInfo) {
            this.uninstallerPkg = uninstallerPkg;
            this.pkgName = pkgName;
            this.pkgVersion = pkgVersion;
            this.firstInstallTime = firstInstallTime;
            this.lastUpdateTime = lastUpdateTime;
            this.returnCode = returnCode;
            this.extra = extra;
            this.deviceBootInfo = deviceBootInfo;
        }

        public String toString() {
            return this.uninstallerPkg + " uninstall " + this.pkgName;
        }
    }

    void refreshThirdLauncherPkgNames(final int delayTime) {
        this.mPkmsUtilsHandler.postDelayed(new Runnable() { // from class: com.android.server.pm.VivoPKMSUtils.4
            @Override // java.lang.Runnable
            public void run() {
                VSlog.d(VivoPKMSUtils.TAG, "refreshThirdLauncherPkgNames delay " + delayTime);
            }
        }, delayTime);
    }

    void InitVivoBlacklistApps() {
        File blacklistfile = new File(BLACKLIST_FILE_PATH);
        readVivoBlackList(blacklistfile);
        File blacklistfile2 = new File("/data/vivo-apps/blackapp.list");
        readVivoBlackList(blacklistfile2);
        File blacklistfile3 = new File("/data/preload/blackapp.list");
        readVivoBlackList(blacklistfile3);
    }

    private void readVivoBlackList(File blacklistfile) {
        if (blacklistfile != null && blacklistfile.exists()) {
            BufferedReader reader = null;
            try {
                try {
                    try {
                        reader = new BufferedReader(new FileReader(blacklistfile));
                        while (true) {
                            String apkPathTemp = reader.readLine();
                            if (apkPathTemp != null) {
                                if (DEBUG) {
                                    VSlog.d(TAG, "#InitVivoBlacklistApps parser  " + apkPathTemp);
                                }
                                if (apkPathTemp != null && apkPathTemp.length() != 0) {
                                    try {
                                        parseBlacklistApkPath(apkPathTemp);
                                    } catch (Exception e) {
                                    }
                                }
                            } else {
                                reader.close();
                                return;
                            }
                        }
                    } catch (Throwable th) {
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (Exception e2) {
                            }
                        }
                        throw th;
                    }
                } catch (Exception e3) {
                    VSlog.e(TAG, "InitVivoBlacklistApps catch exception " + e3.toString());
                    if (reader != null) {
                        reader.close();
                    }
                }
            } catch (Exception e4) {
            }
        }
    }

    private void parseBlacklistApkPath(String apkPath) {
        if (apkPath == null) {
            VSlog.w(TAG, "## parseBlacklistApkPath path is null!");
            return;
        }
        String[] pathStr = apkPath.split("\\s+");
        if (pathStr != null && pathStr.length > 0) {
            if (DEBUG) {
                VSlog.d(TAG, "# parseBlacklistApkPath " + pathStr[0] + " " + pathStr[1]);
            }
            if (pathStr != null && pathStr.length == 2 && pathStr[0] != null && pathStr[1] != null) {
                String blacklistAppDir = pathStr[0];
                String innerFileName = pathStr[1];
                if (DEBUG) {
                    VSlog.d(TAG, "# parseBlacklistApkPath blacklistAppDir:" + blacklistAppDir + " innerFileName:" + innerFileName);
                }
                this.mVivoBlackAppList.add(new VivoBlacklistApps(blacklistAppDir, innerFileName));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArrayList<VivoBlacklistApps> getVivoBlackAppList() {
        return this.mVivoBlackAppList;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class ForbidDeskTopAppInfo {
        boolean mDeskTopAppCnNOpen;
        ArrayList<String> mDeskTopAppCnNameList;
        String mDeskTopAppName;

        public ForbidDeskTopAppInfo(String deskTopAppName, ArrayList<String> desktopAppCnNameList, boolean isOpen) {
            this.mDeskTopAppCnNOpen = false;
            this.mDeskTopAppName = deskTopAppName;
            this.mDeskTopAppCnNameList = desktopAppCnNameList;
            this.mDeskTopAppCnNOpen = isOpen;
        }

        public ForbidDeskTopAppInfo(String deskTopAppName, boolean isOpen) {
            this.mDeskTopAppCnNOpen = false;
            this.mDeskTopAppName = deskTopAppName;
            this.mDeskTopAppCnNOpen = isOpen;
        }

        public String toString() {
            return "apkName:" + this.mDeskTopAppName + " cnName:" + this.mDeskTopAppCnNameList + " open:" + this.mDeskTopAppCnNOpen;
        }
    }

    private void initForbidList() {
        this.mForbidUsbBrushAppWarningToast = String.format(this.mContext.getString(51249804), new Object[0]);
        this.mForbidUsbBrushAppBlackList.add("com.mycheering.onekeyinstall");
        this.mForbidUsbBrushAppBlackList.add("com.droidyuri.installerclient");
        this.mForbidUsbBrushAppBlackList.add("com.droidyuri.installerserver");
        this.mForbidUsbBrushAppBlackList.add("com.service.usbhelper");
        this.mForbidUsbBrushAppBlackList.add("com.shuame.sprite");
        this.mForbidUsbBrushAppBlackList.add("com.ui.usbhelper");
        this.mForbidUsbBrushAppBlackList.add("com.android.media.connect");
        this.mForbidUsbBrushAppBlackList.add("com.android.push.service");
        this.mForbidUsbBrushAppBlackList.add("com.droid.dispatcher");
        this.mForbidUsbBrushAppBlackList.add("com.qjkj.installtool");
        this.mForbidUsbBrushAppBlackList.add("com.oppo.filemanager");
        this.mForbidUsbBrushAppSignatureList.add(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        this.mForbidDesktopAppBlackMap.put("com.android.newline.spiritlauncher", new ForbidDeskTopAppInfo("com.android.newline.spiritlauncher", false));
        this.mForbidDesktopAppBlackMap.put("com.android.newline.funlauncher", new ForbidDeskTopAppInfo("com.android.newline.funlauncher", false));
        this.mForbidDesktopAppBlackMap.put("com.android.custom.launcher", new ForbidDeskTopAppInfo("com.android.custom.launcher", false));
        this.mForbidDesktopAppBlackMap.put("com.zlove.launcherconfig", new ForbidDeskTopAppInfo("com.zlove.launcherconfig", false));
        this.mForbidDesktopAppBlackMap.put("com.klauncher.vilauncher", new ForbidDeskTopAppInfo("com.klauncher.vilauncher", false));
        this.mForbidDesktopAppBlackMap.put("com.android.launcher8", new ForbidDeskTopAppInfo("com.android.launcher8", false));
        this.mForbidDesktopAppBlackMap.put("com.mycheering.launcher", new ForbidDeskTopAppInfo("com.mycheering.launcher", false));
        this.mForbidDesktopAppBlackMap.put("com.tylauncher.home", new ForbidDeskTopAppInfo("com.tylauncher.home", false));
        this.mForbidDesktopAppBlackMap.put("com.setup.launcher3", new ForbidDeskTopAppInfo("com.setup.launcher3", false));
        this.mForbidDesktopAppBlackMap.put("com.changmi.launcher", new ForbidDeskTopAppInfo("com.changmi.launcher", false));
        this.mForbidDesktopAppBlackMap.put("com.tylauncher.home.v", new ForbidDeskTopAppInfo("com.tylauncher.home.v", false));
        this.mForbidDesktopAppBlackMap.put("com.android.newline.smarthome", new ForbidDeskTopAppInfo("com.android.newline.smarthome", false));
        this.mForbidDesktopAppBlackMap.put("com.android.custom.desktop", new ForbidDeskTopAppInfo("com.android.custom.desktop", false));
        this.mForbidDesktopAppBlackMap.put("com.android.os.launcher", new ForbidDeskTopAppInfo("com.android.os.launcher", false));
        printAllForbidListInfo();
    }

    private void printAllForbidListInfo() {
        if (this.DEBUG_FOR_ALL) {
            VSlog.d(TAG, "isForbidUsbBrushAppByBlack?" + this.mIsForbidUsbBrushAppInstallByBlackList);
            VSlog.d(TAG, "isForbidUsbBrushAppInstallBySignature?" + this.mIsForbidUsbBrushAppInstallBySignature);
            VSlog.d(TAG, "isForbidDesktopAppInstallByBlackMap?" + this.mIsForbidDesktopAppInstallByBlackMap);
            VSlog.d(TAG, "blackApp:" + this.mForbidUsbBrushAppBlackList);
            VSlog.d(TAG, "forbidSigInfo:" + this.mForbidUsbBrushAppSignatureList);
            VSlog.d(TAG, "forbidDesktopApp:" + this.mForbidDesktopAppBlackMap);
            VSlog.d(TAG, "forbidWarningToast#:" + this.mForbidUsbBrushAppWarningToast);
        }
    }

    private void applyParserValueToForbidList(ArrayList<String> brushBlackList, ArrayList<String> brushSigList, HashMap<String, ForbidDeskTopAppInfo> forbidDesktopMap) {
        if (this.DEBUG_FOR_ALL) {
            VSlog.d(TAG, "print....");
            VSlog.d(TAG, "brushBlackList:" + brushBlackList);
            VSlog.d(TAG, "brushAppSigInfo:" + brushSigList);
            VSlog.d(TAG, "forbidDesktop:" + forbidDesktopMap);
        }
        if (brushBlackList.size() > 0) {
            synchronized (this.mForbidUsbBrushAppBlackList) {
                this.mForbidUsbBrushAppBlackList.clear();
                this.mForbidUsbBrushAppBlackList.addAll(brushBlackList);
            }
        }
        if (brushSigList.size() > 0) {
            synchronized (this.mForbidUsbBrushAppSignatureList) {
                this.mForbidUsbBrushAppSignatureList.clear();
                this.mForbidUsbBrushAppSignatureList.addAll(brushSigList);
            }
        }
        if (forbidDesktopMap.size() > 0) {
            synchronized (this.mForbidDesktopAppBlackMap) {
                this.mForbidDesktopAppBlackMap.clear();
                this.mForbidDesktopAppBlackMap.putAll(forbidDesktopMap);
            }
        }
        printAllForbidListInfo();
    }

    boolean isForbidBrushInstallShowToast() {
        boolean z;
        synchronized (this.mForbidObject) {
            z = this.mIsForbidBrushInstallShowToast;
        }
        return z;
    }

    boolean isForbidUsbBrushAppByBlack() {
        boolean z;
        synchronized (this.mForbidObject) {
            z = this.mIsForbidUsbBrushAppInstallByBlackList;
        }
        return z;
    }

    boolean isForbidUsbBrushAppInstallBySignature() {
        boolean z;
        synchronized (this.mForbidObject) {
            z = this.mIsForbidUsbBrushAppInstallBySignature;
        }
        return z;
    }

    boolean isForbidDesktopAppInstallByBlackMap() {
        boolean z;
        synchronized (this.mForbidObject) {
            z = this.mIsForbidDesktopAppInstallByBlackMap;
        }
        return z;
    }

    ArrayList<String> getForbidUsbBrushAppBlackList() {
        ArrayList<String> arrayList;
        synchronized (this.mForbidUsbBrushAppBlackList) {
            arrayList = this.mForbidUsbBrushAppBlackList;
        }
        return arrayList;
    }

    ArrayList<String> getForbidUsbBrushAppSignatureList() {
        ArrayList<String> arrayList;
        synchronized (this.mForbidUsbBrushAppSignatureList) {
            arrayList = this.mForbidUsbBrushAppSignatureList;
        }
        return arrayList;
    }

    HashMap<String, ForbidDeskTopAppInfo> getForbidDesktopAppBlackMap() {
        HashMap<String, ForbidDeskTopAppInfo> hashMap;
        synchronized (this.mForbidDesktopAppBlackMap) {
            hashMap = this.mForbidDesktopAppBlackMap;
        }
        return hashMap;
    }

    String getForbidBrushInstallWarningToast() {
        String str;
        synchronized (this.mForbidObject) {
            str = this.mForbidUsbBrushAppWarningToast;
        }
        return str;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean checkInstallingApkIsShouldBeForbid(String packageName, boolean systemApp, boolean replace, PackageManagerService.InstallArgs args, ParsedPackage pkg) {
        if (!FtFeature.isFeatureSupport("vivo.software.pkmsinstalllblacklist")) {
            if (this.DEBUG_FOR_ALL) {
                VSlog.d(TAG, "vivo.software.pkmsinstalllblacklist not open. ");
            }
            return false;
        } else if (systemApp) {
            return false;
        } else {
            if (replace) {
                if (this.DEBUG_FOR_ALL) {
                    VSlog.i(TAG, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + pkg.getPackageName() + " replace install.");
                }
                return false;
            } else if (args == null || args.verificationInfo == null) {
                return false;
            } else {
                VSlog.i(TAG, "check_pkg:" + pkg);
                if (pkg == null) {
                    return false;
                }
                VSlog.i(TAG, " systemApp:" + systemApp + " replace:" + replace);
                if (this.DEBUG_FOR_ALL) {
                    VSlog.d(TAG, "#1:" + this.mIsForbidUsbBrushAppInstallByBlackList + " #2" + this.mIsForbidUsbBrushAppInstallBySignature + " #3:" + this.mIsForbidDesktopAppInstallByBlackMap);
                }
                boolean isForbidInstall = checkInstallingApkIsShouldBeForbidInner(packageName, systemApp, replace, args, pkg);
                if (isForbidInstall) {
                    try {
                        writeVcdLog(packageName, args, pkg);
                    } catch (Exception e) {
                        VSlog.w(TAG, "log to logs catch exception, " + e.toString());
                    }
                } else {
                    VSlog.i(TAG, "#123# " + packageName);
                }
                return isForbidInstall;
            }
        }
    }

    private boolean checkInstallingApkIsShouldBeForbidInner(String packageName, boolean systemApp, boolean replace, PackageManagerService.InstallArgs args, ParsedPackage pkg) {
        ArrayList<String> forbidAppSignatureList;
        Signature[] pkgSigs;
        HashMap<String, ForbidDeskTopAppInfo> forbidDesktopAppMap;
        DeviceBootInfo deviceBootInfo = parserDeviceBootInfoFromProp();
        if (isForbidUsbBrushAppByBlack()) {
            ArrayList<String> usbBrushAppBlackList = getForbidUsbBrushAppBlackList();
            if (this.DEBUG_FOR_ALL) {
                VSlog.d(TAG, "ub_bl:" + usbBrushAppBlackList);
            }
            if (usbBrushAppBlackList != null && usbBrushAppBlackList.size() > 0 && usbBrushAppBlackList.contains(packageName)) {
                if (deviceBootInfo.bootElapsedTime >= this.mForbidBrushToolsOverTime) {
                    if (this.DEBUG_FOR_ALL) {
                        VSlog.d(TAG, "cpu elpsed overtime. " + deviceBootInfo.bootElapsedTime);
                    }
                    return false;
                }
                tryToShowApkInstallFailedToast(packageName);
                return true;
            }
        }
        if (isForbidDesktopAppInstallByBlackMap() && (forbidDesktopAppMap = getForbidDesktopAppBlackMap()) != null && forbidDesktopAppMap.size() > 0) {
            ForbidDeskTopAppInfo forbidDesktopAppInfo = forbidDesktopAppMap.get(packageName);
            if (this.DEBUG_FOR_ALL) {
                VSlog.d(TAG, "forbidDesktopAppInfo:" + forbidDesktopAppInfo);
            }
            if (forbidDesktopAppInfo != null) {
                if (deviceBootInfo.bootElapsedTime >= this.mForbidDestTopAppOverTime) {
                    if (this.DEBUG_FOR_ALL) {
                        VSlog.d(TAG, "cpu elpsed overtime " + deviceBootInfo.bootElapsedTime);
                    }
                    return false;
                } else if (forbidDesktopAppInfo.mDeskTopAppCnNOpen) {
                    ArrayList<String> deskTopAppCnNameList = forbidDesktopAppInfo.mDeskTopAppCnNameList;
                    if (deskTopAppCnNameList != null) {
                        String apkCnName = null;
                        try {
                            apkCnName = parserApkCnNameFromPackageParser(pkg);
                        } catch (Exception e) {
                        }
                        VSlog.i(TAG, "apkCnName:" + apkCnName + " packageName:" + packageName);
                        if (apkCnName != null && deskTopAppCnNameList.contains(apkCnName)) {
                            tryToShowApkInstallFailedToast(packageName);
                            return true;
                        }
                    }
                } else {
                    tryToShowApkInstallFailedToast(packageName);
                    return true;
                }
            }
        }
        if (isForbidUsbBrushAppInstallBySignature() && (forbidAppSignatureList = getForbidUsbBrushAppSignatureList()) != null && forbidAppSignatureList.size() > 0 && (pkgSigs = pkg.getSigningDetails().signatures) != null && pkgSigs.length > 0) {
            try {
                PublicKey publicKey = pkgSigs[0].getPublicKey();
                String pubKeyStr = publicKey.toString();
                if (forbidAppSignatureList.contains(pubKeyStr)) {
                    tryToShowApkInstallFailedToast(pkg.getPackageName());
                    return true;
                }
            } catch (Exception e2) {
                VSlog.e(TAG, "check " + pkg + " publickey catch exception, " + e2.toString());
                return false;
            }
        }
        return false;
    }

    private void tryToShowApkInstallFailedToast(String toInstallPkgName) {
        if (!isForbidBrushInstallShowToast()) {
            return;
        }
        try {
            String warningToast = getForbidBrushInstallWarningToast();
            showApkInstallFailedToastInner(warningToast);
        } catch (Exception e) {
            VSlog.w(TAG, "exception " + e.toString());
        }
    }

    private void showApkInstallFailedToastInner(final String toastInfo) {
        this.mPkmsUtilsHandler.postDelayed(new Runnable() { // from class: com.android.server.pm.VivoPKMSUtils.5
            @Override // java.lang.Runnable
            public void run() {
                Toast toast = Toast.makeText(VivoPKMSUtils.this.mContext, toastInfo, 1);
                toast.setGravity(17, 0, 0);
                toast.show();
            }
        }, 100L);
    }

    boolean checkInstallingApkisExist(String installingPkgName) {
        PackageManager packageManager = this.mContext.getPackageManager();
        try {
            ApplicationInfo info = packageManager.getApplicationInfo(installingPkgName, 4096);
            VSlog.i(TAG, "installingPkgName:" + installingPkgName + " info:" + info);
            if (info != null) {
                return true;
            }
            return false;
        } catch (Exception e) {
            VSlog.w(TAG, installingPkgName + " check exist ? exception " + e.toString());
            return false;
        }
    }

    private String parserApkCnNameFromPackageParser(ParsedPackage pkg) {
        if (pkg == null) {
            return null;
        }
        String appCnName = null;
        ApplicationInfo appInfo = null;
        if (0 == 0) {
            return null;
        }
        String str = appInfo.packageName;
        Resources res = this.mContext.getResources();
        AssetManager ass = new AssetManager();
        ass.addAssetPath(pkg.getBaseCodePath());
        Resources newRes = new Resources(ass, res.getDisplayMetrics(), res.getConfiguration());
        if (appInfo.labelRes != 0) {
            try {
                appCnName = newRes.getText(appInfo.labelRes).toString();
            } catch (Exception e) {
                VLog.e(TAG, "exception " + e);
            }
        }
        if (appCnName == null) {
            return appInfo.nonLocalizedLabel != null ? appInfo.nonLocalizedLabel.toString() : appInfo.packageName;
        }
        return appCnName;
    }

    private void writeVcdLog(String packageName, PackageManagerService.InstallArgs args, ParsedPackage pkg) {
        String install_resource;
        boolean IsInstallSilence;
        String install_resource2;
        String install_resource3;
        boolean IsInstallSilence2;
        String install_resource4;
        String install_resource5;
        boolean IsInstallSilence3;
        String install_resource6;
        int installerUid = args.verificationInfo.installerUid;
        int originatingUid = args.verificationInfo.originatingUid;
        String installerPackage = args.installSource.installerPackageName;
        int installFlags = args.installFlags;
        DeviceBootInfo deviceBootInfo = parserDeviceBootInfoFromProp();
        HashMap<String, String> vcdInfoMap = new HashMap<>();
        if ("com.android.packageinstaller".equals(installerPackage)) {
            install_resource3 = this.mPKMService.getNameForUid(originatingUid);
            IsInstallSilence2 = false;
        } else {
            if (installerUid == 0) {
                install_resource = null;
            } else if (installerUid == 2000) {
                install_resource = null;
            } else {
                if ("com.google.android.packageinstaller".equals(installerPackage)) {
                    String callerPkg = this.mPKMService.getNameForUid(originatingUid);
                    if (this.DEBUG_FOR_ALL) {
                        VSlog.d(TAG, "com.google.android.packageinstaller callerPkg=" + callerPkg);
                    }
                    install_resource2 = callerPkg;
                    IsInstallSilence = false;
                } else {
                    install_resource2 = installerPackage;
                    IsInstallSilence = true;
                }
                vcdInfoMap.put("p_r", install_resource2);
                vcdInfoMap.put("p_d", packageName + ".beForbidInstall");
                vcdInfoMap.put("p_w", "0");
                vcdInfoMap.put("p_s", String.valueOf(IsInstallSilence));
                vcdInfoMap.put("b_t", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + deviceBootInfo.bootElapsedTime);
                vcdInfoMap.put("b_c", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + deviceBootInfo.bootCount);
                vcdInfoMap.put("p_rc", "-110");
                vcdInfoMap.put("p_fInstall", "forbid_install");
                install_resource3 = install_resource2;
                IsInstallSilence2 = IsInstallSilence;
            }
            if (installerUid == 0) {
                vcdInfoMap.put("p_r", "0");
                install_resource4 = "root";
            } else if (installerUid != 2000) {
                install_resource4 = install_resource;
            } else {
                vcdInfoMap.put("p_r", "2000");
                install_resource4 = "shell";
            }
            boolean fromVivo = (1073741824 & installFlags) != 0;
            if (!this.DEBUG_FOR_ALL) {
                install_resource5 = install_resource4;
            } else {
                StringBuilder sb = new StringBuilder();
                install_resource5 = install_resource4;
                sb.append(" fromVivo:");
                sb.append(fromVivo);
                sb.append(" installFlags:");
                sb.append(installFlags);
                VSlog.d(TAG, sb.toString());
            }
            if (!fromVivo) {
                IsInstallSilence3 = false;
                install_resource6 = install_resource5;
            } else {
                vcdInfoMap.put("p_r", "com.vivo.PCTools");
                install_resource6 = "com.vivo.PCTools";
                IsInstallSilence3 = true;
            }
            vcdInfoMap.put("p_v", "-1");
            String str = install_resource6;
            IsInstallSilence = IsInstallSilence3;
            install_resource2 = str;
            vcdInfoMap.put("p_d", packageName + ".beForbidInstall");
            vcdInfoMap.put("p_w", "0");
            vcdInfoMap.put("p_s", String.valueOf(IsInstallSilence));
            vcdInfoMap.put("b_t", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + deviceBootInfo.bootElapsedTime);
            vcdInfoMap.put("b_c", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + deviceBootInfo.bootCount);
            vcdInfoMap.put("p_rc", "-110");
            vcdInfoMap.put("p_fInstall", "forbid_install");
            install_resource3 = install_resource2;
            IsInstallSilence2 = IsInstallSilence;
        }
        VivoCollectData VCD = VivoCollectData.getInstance(this.mContext);
        VCD.writeData("505", "5052", System.currentTimeMillis(), System.currentTimeMillis(), 0L, 1, vcdInfoMap);
        sendApkInstalledBroadcast(IsInstallSilence2, install_resource3, "forbid_install", packageName + ".beForbidInstall", false, "forbid_install", deviceBootInfo);
    }

    private int getInstallApkNextSeqNumber() {
        int i;
        synchronized (this.mLogInfoLocalLock) {
            i = this.mInstallApkSequenceNumber + 1;
            this.mInstallApkSequenceNumber = i;
        }
        return i;
    }

    private int getUnInstallApkNextSeqNumber() {
        int i;
        synchronized (this.mLogInfoLocalLock) {
            i = this.mUnInstallApkSequenceNumber + 1;
            this.mUnInstallApkSequenceNumber = i;
        }
        return i;
    }

    private int getUpdateApkNextSeqNumber() {
        int i;
        synchronized (this.mLogInfoLocalLock) {
            i = this.mUpdateApkSequenceNumber + 1;
            this.mUpdateApkSequenceNumber = i;
        }
        return i;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initSeqNumFromLocalFile() {
        VSlog.i(TAG, "begin init seq num from loc.");
        initSeqNumFromLocalFileInner(INSTALL_APK_SEQ_NUM_PATH);
        initSeqNumFromLocalFileInner(UN_INSTALL_APK_SEQ_NUM_PATH);
        initSeqNumFromLocalFileInner(UPDATE_APK_SEQ_NUM_PATH);
        reCheckSeqNum();
        VSlog.i(TAG, "after initSeq " + this.mInstallApkSequenceNumber + " " + this.mUnInstallApkSequenceNumber + " " + this.mUpdateApkSequenceNumber);
    }

    private void initSeqNumFromLocalFileInner(String seqNumPath) {
        int seqNum = -1;
        try {
            String seqStr = readOneLineFromFile(seqNumPath);
            if (seqStr != null) {
                seqNum = Integer.parseInt(seqStr);
            }
        } catch (Exception e) {
            VSlog.e(TAG, "init seq, " + e.toString());
        }
        if (DEBUG) {
            VSlog.i(TAG, "seqNum:" + seqNum + " path:" + seqNumPath);
        }
        if (seqNum > 0) {
            if (INSTALL_APK_SEQ_NUM_PATH.equals(seqNumPath)) {
                synchronized (this.mLogInfoLocalLock) {
                    this.mInstallApkSequenceNumber = seqNum;
                }
                return;
            } else if (UN_INSTALL_APK_SEQ_NUM_PATH.equals(seqNumPath)) {
                synchronized (this.mLogInfoLocalLock) {
                    this.mUnInstallApkSequenceNumber = seqNum;
                }
                return;
            } else if (UPDATE_APK_SEQ_NUM_PATH.equals(seqNumPath)) {
                synchronized (this.mLogInfoLocalLock) {
                    this.mUpdateApkSequenceNumber = seqNum;
                }
                return;
            } else {
                return;
            }
        }
        VSlog.w(TAG, "init seqNum " + seqNum);
    }

    private void writeCurrentSeqNumTL(String seqNumPath, long installSeq, long uninstallSeq) {
        long currentSeqNum = -1;
        if (INSTALL_APK_SEQ_NUM_PATH.equals(seqNumPath)) {
            currentSeqNum = installSeq;
        } else if (UN_INSTALL_APK_SEQ_NUM_PATH.equals(seqNumPath)) {
            currentSeqNum = uninstallSeq;
        } else if (UPDATE_APK_SEQ_NUM_PATH.equals(seqNumPath)) {
            currentSeqNum = installSeq;
        }
        File seqNumFile = checkFileExistAndCreate(seqNumPath);
        if (seqNumFile == null) {
            return;
        }
        if (this.DEBUG_FOR_ALL) {
            VSlog.i(TAG, "write seqNum:" + currentSeqNum + " " + seqNumFile);
        }
        ArrayList<String> infoList = new ArrayList<>();
        infoList.add(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + currentSeqNum);
        doWriteITF(seqNumFile, infoList, false);
    }

    private void writeInstallApkLog(boolean isInstallSilence, String installerPkg, String installerVersion, String pkgName, boolean isUpdate, String extra, DeviceBootInfo deviceBootInfo, long seqNum, String netType, String deviceID, String wifiList, String pkgCNName) {
        File infoFile;
        String seqNumPath;
        if (!FtFeature.isFeatureSupport("vivo.software.pkms_un_install_log")) {
            VSlog.i(TAG, "vivo.software.pkms_un_install_log not open. ");
        } else if (mIsOverseas) {
        } else {
            if (!VCD_INFO_TO_LOCAL_OPEN) {
                VSlog.w(TAG, "not open, not need.");
            } else if (seqNum > DEFAULT_MAX_RECORD_COUNT) {
                VSlog.w(TAG, seqNum + " > " + DEFAULT_MAX_RECORD_COUNT + " not need");
            } else if (deviceBootInfo.bootElapsedTime > 63072000) {
                VSlog.w(TAG, deviceBootInfo.bootElapsedTime + " to long, not need; skip it!");
            } else if (!checkVcdInfoSizeIsSafe()) {
                VSlog.w(TAG, "vcd info size is too big, do not write anymore.");
            } else {
                if (isUpdate) {
                    File infoFile2 = checkFileExistAndCreate(UPDATE_APK_INFO_PATH);
                    infoFile = infoFile2;
                    seqNumPath = UPDATE_APK_SEQ_NUM_PATH;
                } else {
                    File infoFile3 = checkFileExistAndCreate(INSTALL_APK_INFO_PATH);
                    infoFile = infoFile3;
                    seqNumPath = INSTALL_APK_SEQ_NUM_PATH;
                }
                if (infoFile == null) {
                    VSlog.w(TAG, " install infoFile not exist.");
                    return;
                }
                StringBuilder sBuiler = new StringBuilder(128);
                ArrayList<String> infoList = new ArrayList<>();
                String currentDate = formatDate(System.currentTimeMillis());
                sBuiler.append("v1\t");
                sBuiler.append(DEVICE_VERSION + "\t");
                sBuiler.append(currentDate + "\t");
                sBuiler.append(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + deviceBootInfo.bootCount + "\t");
                sBuiler.append(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + deviceBootInfo.bootElapsedTime + "\t");
                sBuiler.append(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + seqNum + "\t");
                StringBuilder sb = new StringBuilder();
                sb.append(installerPkg);
                sb.append("\t");
                sBuiler.append(sb.toString());
                sBuiler.append(pkgName + "\t");
                sBuiler.append(installerVersion + "\t");
                sBuiler.append(pkgCNName + "\t");
                sBuiler.append(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + isUpdate + "\t");
                sBuiler.append(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + isInstallSilence + "\t");
                StringBuilder sb2 = new StringBuilder();
                sb2.append(netType);
                sb2.append("\t");
                sBuiler.append(sb2.toString());
                sBuiler.append(deviceID + "\t");
                sBuiler.append(wifiList + "\t");
                sBuiler.append(extra + "\t");
                if (this.DEBUG_FOR_ALL) {
                    VSlog.d(TAG, "info:" + sBuiler.toString());
                }
                infoList.add(sBuiler.toString());
                doWriteITF(infoFile, infoList, true);
                writeCurrentSeqNumTL(seqNumPath, seqNum, -1L);
            }
        }
    }

    private void writeUnInstallApkLTL(String uninstallerPkg, String pkgName, String pkgVersion, long firstInstallTime, long lastUpdateTime, int returnCode, String extra, DeviceBootInfo deviceBootInfo, long unInstallSeq, String netType, String deviceID, String location) {
        if (!FtFeature.isFeatureSupport("vivo.software.pkms_un_install_log")) {
            VSlog.i(TAG, "vivo.software.pkms_un_install_log not open. ");
        } else if (mIsOverseas) {
        } else {
            if (!VCD_INFO_TO_LOCAL_OPEN) {
                VSlog.w(TAG, "not open, no need vcd info.");
            } else if (unInstallSeq > DEFAULT_MAX_RECORD_COUNT) {
                VSlog.w(TAG, unInstallSeq + " >" + DEFAULT_MAX_RECORD_COUNT + "  not need");
            } else if (deviceBootInfo.bootElapsedTime > 63072000) {
                VSlog.w(TAG, deviceBootInfo.bootElapsedTime + " to long, not need; skip it!");
            } else if (checkVcdInfoSizeIsSafe()) {
                File infoFile = checkFileExistAndCreate(UN_INSTALL_APK_INFO_PATH);
                if (infoFile == null) {
                    VSlog.w(TAG, "uninstal infoFile not exist.");
                    return;
                }
                StringBuilder sBuiler = new StringBuilder();
                ArrayList<String> infoList = new ArrayList<>();
                String pkgCNName = getPackageCNName(pkgName);
                sBuiler.append("v1\t");
                sBuiler.append(DEVICE_VERSION + "\t");
                sBuiler.append(formatDate(System.currentTimeMillis()) + "\t");
                sBuiler.append(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + deviceBootInfo.bootCount + "\t");
                sBuiler.append(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + deviceBootInfo.bootElapsedTime + "\t");
                sBuiler.append(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + unInstallSeq + "\t");
                StringBuilder sb = new StringBuilder();
                sb.append(uninstallerPkg);
                sb.append("\t");
                sBuiler.append(sb.toString());
                sBuiler.append(pkgName + "\t");
                sBuiler.append(pkgVersion + "\t");
                sBuiler.append(pkgCNName + "\t");
                sBuiler.append(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + formatDate(firstInstallTime) + "\t");
                sBuiler.append(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + formatDate(lastUpdateTime) + "\t");
                sBuiler.append(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + returnCode + "\t");
                StringBuilder sb2 = new StringBuilder();
                sb2.append(netType);
                sb2.append("\t");
                sBuiler.append(sb2.toString());
                sBuiler.append(deviceID + "\t");
                sBuiler.append(location + "\t");
                sBuiler.append(extra + "\t");
                if (this.DEBUG_FOR_ALL) {
                    VSlog.d(TAG, sBuiler.toString());
                }
                infoList.add(sBuiler.toString());
                doWriteITF(infoFile, infoList, true);
                writeCurrentSeqNumTL(UN_INSTALL_APK_SEQ_NUM_PATH, -1L, unInstallSeq);
            } else {
                VSlog.w(TAG, "vcd info size is too big, do not write anymore.");
            }
        }
    }

    private void doWriteITF(File file, ArrayList<String> infoList, boolean append) {
        if (DEBUG) {
            VSlog.d(TAG, "write  to " + file + " " + append);
        }
        if (file != null && file.exists()) {
            BufferedWriter bufferWriter = null;
            try {
                try {
                    try {
                        bufferWriter = new BufferedWriter(new FileWriter(file, append));
                        if (infoList != null && infoList.size() > 0) {
                            Iterator<String> it = infoList.iterator();
                            while (it.hasNext()) {
                                String strTemp = it.next();
                                bufferWriter.write(strTemp);
                                bufferWriter.newLine();
                            }
                        }
                        bufferWriter.flush();
                        bufferWriter.close();
                    } catch (Throwable th) {
                        if (bufferWriter != null) {
                            try {
                                bufferWriter.close();
                            } catch (IOException e) {
                            }
                        }
                        throw th;
                    }
                } catch (Exception e2) {
                    VSlog.e(TAG, "write catch exception " + e2.toString());
                    if (bufferWriter != null) {
                        bufferWriter.close();
                    }
                }
            } catch (IOException e3) {
            }
        }
    }

    private String readOneLineFromFile(String filePath) {
        String iOException;
        String readResult = "-1";
        try {
            File fileTemp = new File(filePath);
            if (fileTemp.exists() && 0 != fileTemp.length()) {
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new FileReader(fileTemp));
                    String readStr = reader.readLine();
                    if (this.DEBUG_FOR_ALL) {
                        VSlog.i(TAG, "readoneLineStr:" + readStr);
                    }
                    if (readStr != null) {
                        if (readStr.length() != 0) {
                            readResult = readStr;
                        }
                    }
                    try {
                        reader.close();
                    } catch (IOException e) {
                        iOException = e.toString();
                        VSlog.e(TAG, iOException);
                        return readResult;
                    }
                } catch (Exception e2) {
                    e2.printStackTrace();
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e3) {
                            iOException = e3.toString();
                            VSlog.e(TAG, iOException);
                            return readResult;
                        }
                    }
                }
            }
        } catch (Exception e4) {
            VSlog.w(TAG, "read oneLie from file, " + e4.toString());
        }
        return readResult;
    }

    private String readDeviceID() {
        if (!"unknow".equals(this.mDeviceID)) {
            return this.mDeviceID;
        }
        String deviceEmmcid = readOneLineFromFile("/sys/block/mmcblk0/device/cid");
        if (deviceEmmcid != null && !deviceEmmcid.equals("-1")) {
            this.mDeviceID = deviceEmmcid;
            return deviceEmmcid;
        }
        String ufsid = readOneLineFromFile("/sys/ufs/ufsid");
        if (ufsid != null && !ufsid.equals("-1")) {
            this.mDeviceID = ufsid;
        }
        String filePath1 = this.mDeviceID;
        return filePath1;
    }

    void initDeviceI() {
        TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService("phone");
        String deviceIe = null;
        if (tm != null) {
            deviceIe = tm.getImei(0);
        }
        if (deviceIe == null) {
            deviceIe = SystemProperties.get("persist.sys.updater.imei", "unknow");
        }
        if (!"unknow".equals(deviceIe)) {
            this.mDeviceI = deviceIe;
        }
    }

    private File checkFileExistAndCreate(String filePath) {
        File file;
        if (this.DEBUG_FOR_ALL) {
            VSlog.i(TAG, "checkPath " + filePath);
        }
        try {
            file = new File(filePath);
        } catch (Exception e) {
            VSlog.w(TAG, "crate " + filePath + " " + e.toString());
        }
        if (file.exists()) {
            return file;
        }
        if (!file.getParentFile().exists()) {
            VSlog.i(TAG, "file parent is not exist.");
            if (!file.getParentFile().mkdir()) {
                VSlog.i(TAG, "crate parent file false.");
                return null;
            }
            FileUtils.setPermissions(file.getParentFile(), 496, -1, -1);
        }
        if (file.createNewFile()) {
            VSlog.i(TAG, "crate " + filePath + " suc.");
            FileUtils.setPermissions(file, 496, -1, -1);
            return file;
        }
        return null;
    }

    private boolean checkVcdInfoSizeIsSafe() {
        File[] fileStr;
        long infoSize = 0;
        try {
            File file = new File("/data/bbkcore/framework_pkms");
            if (file.exists() && file.isDirectory() && (fileStr = file.listFiles()) != null) {
                for (File fileTemp : fileStr) {
                    if (this.DEBUG_FOR_ALL) {
                        VSlog.d(TAG, "fileTemp:" + fileTemp);
                    }
                    if (fileTemp != null && fileTemp.exists()) {
                        infoSize += fileTemp.length();
                    }
                }
            }
            VSlog.i(TAG, "logInfoSize:" + infoSize);
        } catch (Exception e) {
            VSlog.w(TAG, "infoSize " + e.toString());
        }
        if (infoSize >= VCD_INFO_FILE_TOTAL_SIZE) {
            return false;
        }
        return true;
    }

    private String getDeviceWifiScanList() {
        List<ScanResult> list;
        long curRunningTime = SystemClock.elapsedRealtime();
        String str = this.mLastWifiList;
        if (str != null && curRunningTime - this.mLastGetWifiElapsedTime < 180000) {
            return str;
        }
        this.mLastGetWifiElapsedTime = SystemClock.elapsedRealtime();
        StringBuilder sb = new StringBuilder();
        try {
            WifiManager wifiManager = (WifiManager) this.mContext.getSystemService("wifi");
            list = wifiManager.getScanResults();
        } catch (Exception e) {
            VSlog.w(TAG, "get wifi list, " + e.toString());
        }
        if (list != null && list.size() != 0) {
            if (list.size() > 1) {
                sortByLevel(list);
            }
            int index = 0;
            for (ScanResult sr : list) {
                if (sr.SSID != null && !Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK.equals(sr.SSID)) {
                    index++;
                    if (index > 5) {
                        break;
                    }
                    String bssidTemp = sr.BSSID;
                    if (bssidTemp != null) {
                        bssidTemp = bssidTemp.replace(":", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                    }
                    String ssidTemp = sr.SSID;
                    if (ssidTemp != null) {
                        ssidTemp = ssidTemp.replace(" ", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                    }
                    sb.append(bssidTemp + "#" + ssidTemp + " ");
                }
            }
            this.mLastWifiList = sb.toString();
            if (this.DEBUG_FOR_ALL) {
                VSlog.i(TAG, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + this.mLastWifiList);
            }
            return this.mLastWifiList;
        }
        return this.mLastWifiList;
    }

    private void sortByLevel(List<ScanResult> list) {
        Collections.sort(list, new Comparator<ScanResult>() { // from class: com.android.server.pm.VivoPKMSUtils.6
            @Override // java.util.Comparator
            public int compare(ScanResult lhs, ScanResult rhs) {
                return rhs.level - lhs.level;
            }
        });
    }

    private String formatDate(long timeMillis) {
        String formatResult = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + timeMillis;
        try {
            String formatResult2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date(timeMillis));
            return formatResult2;
        } catch (Exception e) {
            VSlog.e(TAG, "formate, " + e.toString());
            return formatResult;
        }
    }

    private boolean checkDeviceIsSelfUpgrade() {
        DeviceBootInfo deviceBootInfo = parserDeviceBootInfoFromProp();
        if (deviceBootInfo.bootCount > 1) {
            return true;
        }
        PackageManager packageManager = this.mContext.getPackageManager();
        try {
            List<ApplicationInfo> appInfoList = packageManager.getInstalledApplications(0);
            if (appInfoList != null) {
                for (ApplicationInfo appInfo : appInfoList) {
                    if (this.DEBUG_FOR_ALL) {
                        VSlog.i(TAG, "appInfo:" + appInfo + "sourceDir:" + appInfo.sourceDir);
                    }
                    if (appInfo != null && appInfo.sourceDir.contains("data/app")) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
        }
        return false;
    }

    private void reCheckSeqNum() {
        String bootInfo = readOneLineFromFile(DEVICE_BOOT_INFO);
        if (bootInfo != null && bootInfo.equals("DEVICE_BOOT_INFO")) {
            VSlog.i(TAG, "Do not need check seq Num.");
            return;
        }
        VSlog.i(TAG, "start recheck seqNum");
        if (checkDeviceIsSelfUpgrade()) {
            VSlog.i(TAG, "self upgrade, re init seq num");
            PackageManager packageManager = this.mContext.getPackageManager();
            int installedAppCount = 0;
            try {
                List<ApplicationInfo> appInfoList = packageManager.getInstalledApplications(0);
                if (appInfoList != null) {
                    for (ApplicationInfo appInfo : appInfoList) {
                        if (this.DEBUG_FOR_ALL) {
                            VSlog.d(TAG, "appInfo:" + appInfo + "sourceDir:" + appInfo.sourceDir);
                        }
                        if (appInfo != null && appInfo.sourceDir.contains("data/app")) {
                            installedAppCount++;
                        }
                    }
                }
            } catch (Exception e) {
            }
            VSlog.i(TAG, "re init to " + installedAppCount + " " + this.mInstallApkSequenceNumber);
            synchronized (this.mLogInfoLocalLock) {
                if (installedAppCount > this.mInstallApkSequenceNumber) {
                    this.mInstallApkSequenceNumber = installedAppCount;
                }
            }
            int i = this.mInstallApkSequenceNumber;
            if (i > 0) {
                writeCurrentSeqNumTL(INSTALL_APK_SEQ_NUM_PATH, i, -1L);
            }
        } else {
            VSlog.i(TAG, "not self upgrade.");
        }
        File seqNumFile = checkFileExistAndCreate(DEVICE_BOOT_INFO);
        ArrayList<String> infoList = new ArrayList<>();
        infoList.add("DEVICE_BOOT_INFO");
        doWriteITF(seqNumFile, infoList, false);
    }

    public void initIdleOptimize() {
        this.mIdleOptimizePackageList.add("com.facebook.katana");
        this.mIdleOptimizePackageList.add("com.picsart.studio");
    }

    public ArrayList<String> getIdleOptimizePackageList() {
        return this.mIdleOptimizePackageList;
    }

    public ArrayList<String> getServerInstallBlackList() {
        return this.mInstallBlackList;
    }

    private void initDefaultList() {
        this.mSpecialIList.add("865407010000009");
    }

    private void parseAdbSilentInstallConfigFromStream(InputStream inputStream, String info, String originContents) {
        StringBuilder sb;
        if (inputStream == null) {
            return;
        }
        if (this.DEBUG_FOR_ALL) {
            VSlog.d(TAG, "parse adb install config    " + info);
        }
        try {
            try {
                XmlPullParserFactory pullFactory = XmlPullParserFactory.newInstance();
                XmlPullParser parser = pullFactory.newPullParser();
                parser.setInput(inputStream, "utf-8");
                ArrayList<String> specialIeList = new ArrayList<>();
                for (int eventCode = parser.getEventType(); eventCode != 1; eventCode = parser.next()) {
                    if (eventCode == 2) {
                        String name = parser.getName();
                        if (!"specialIMEI".equals(name)) {
                            if ("imei".equals(name)) {
                                String value = parser.getAttributeValue(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, "value");
                                if (DEBUG) {
                                    VSlog.d(TAG, " value:" + value);
                                }
                                if (!specialIeList.contains(value)) {
                                    specialIeList.add(value);
                                }
                            } else if ("imeiFeatureOpen".equals(name)) {
                                String value2 = parser.getAttributeValue(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, "value");
                                if (DEBUG) {
                                    VSlog.d(TAG, " value " + value2);
                                    this.mIFeatureOpen = Boolean.parseBoolean(value2);
                                }
                            }
                        }
                    } else if (eventCode == 3) {
                        parser.getName().equals("specialIMEI");
                    }
                }
                if (specialIeList.size() > 0) {
                    this.mSpecialIList.clear();
                    this.mSpecialIList = (ArrayList) specialIeList.clone();
                }
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Exception e) {
                        e = e;
                        sb = new StringBuilder();
                        sb.append("is close,");
                        sb.append(e.toString());
                        VSlog.w(TAG, sb.toString());
                    }
                }
            } catch (XmlPullParserException e2) {
                VSlog.w(TAG, "xml parser, " + e2.toString());
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Exception e3) {
                        e = e3;
                        sb = new StringBuilder();
                        sb.append("is close,");
                        sb.append(e.toString());
                        VSlog.w(TAG, sb.toString());
                    }
                }
            } catch (Exception ioe) {
                VSlog.w(TAG, "exception " + ioe.toString());
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Exception e4) {
                        e = e4;
                        sb = new StringBuilder();
                        sb.append("is close,");
                        sb.append(e.toString());
                        VSlog.w(TAG, sb.toString());
                    }
                }
            }
        } catch (Throwable e5) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e6) {
                    VSlog.w(TAG, "is close," + e6.toString());
                }
            }
            throw e5;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean checkDeviceInstallShouldBeSilent(PackageManagerService.ActiveInstallSession activeInstallSession, boolean isEng, boolean isOverseas) {
        String packageName = activeInstallSession.getPackageName();
        PackageInstaller.SessionParams sessionParams = activeInstallSession.getSessionParams();
        activeInstallSession.getObserver();
        String installerPackageName = activeInstallSession.getInstallSource().installerPackageName;
        File stagedDir = activeInstallSession.getStagedDir();
        int installerUid = activeInstallSession.getInstallerUid();
        new PackageManagerService.VerificationInfo(sessionParams.originatingUri, sessionParams.referrerUri, sessionParams.originatingUid, installerUid);
        boolean fromVivo = (sessionParams.installFlags & 1073741824) != 0;
        boolean fromAdb = (sessionParams.installFlags & 32) != 0;
        boolean isGMSApk = isGMSApk(packageName);
        int callingUid = Binder.getCallingUid();
        VSlog.i(TAG, "-installStage - checkDeviceInstallShouldBeSilent packageName:" + packageName + " stagedDir:" + stagedDir + " installerPackageName:" + installerPackageName + " originatingUid:" + sessionParams.originatingUid + " installerUid:" + installerUid + " fromVivo:" + fromVivo + " fromAdb:" + fromAdb + " isGMSApk:" + isGMSApk + "  sessionParams.installFlags:" + sessionParams.installFlags);
        if (isOverseas || isGMSApk || fromVivo || callingUid < 10000) {
            return true;
        }
        if (("com.bbk.appstore".equals(installerPackageName) || "com.android.packageinstaller".equals(installerPackageName)) && !isOverseas) {
            return true;
        }
        if ("com.google.android.packageinstaller".equals(installerPackageName) && isOverseas) {
            return true;
        }
        if (installerPackageName == null || !this.mPKMService.isGgPackageExport(installerPackageName)) {
            if (!"com.android.vending".equals(installerPackageName) || Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK.equals(SystemProperties.get("ro.com.google.gmsversion", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK))) {
                int checkUidResult = this.mPKMService.checkUidSignatures(callingUid, 1000);
                if (this.DEBUG_FOR_ALL) {
                    VSlog.d(TAG, "check " + callingUid + " " + installerPackageName + " want silent install apk;  cmp " + checkUidResult);
                }
                if (checkUidResult == 0) {
                    return true;
                }
                if (!FtFeature.isFeatureSupport("vivo.software.pkms_adb_silent_install")) {
                    VSlog.i(TAG, "vivo.software.pkms_adb_silent_install not open. ");
                    return false;
                }
                if (DEBUG) {
                    VSlog.d(TAG, "[" + installerPackageName + " - " + installerUid + "] want install " + packageName + ", check.... " + fromAdb);
                }
                if (fromAdb && this.mIFeatureOpen) {
                    if (this.mSpecialIList.contains(this.mDeviceI)) {
                        VSlog.i(TAG, "find special imei");
                        return true;
                    }
                    boolean isAdbInstallDebug = "1".equals(SystemProperties.get("persist.sys.adb.install.debug", "0"));
                    if (isAdbInstallDebug) {
                        VSlog.w(TAG, "adb install debug ,silent");
                        return true;
                    }
                }
                boolean isAdbInstallDebug2 = DEBUG;
                if (isAdbInstallDebug2) {
                    VSlog.d(TAG, "check flase.");
                    return false;
                }
                return false;
            }
            return true;
        }
        return true;
    }

    private void parseCommonConfigFromStream(InputStream inputStream, String info, String originContents) {
        StringBuilder sb;
        if (inputStream == null) {
            return;
        }
        if (this.DEBUG_FOR_ALL) {
            VSlog.d(TAG, "parse adb install config    " + info);
        }
        try {
            try {
                try {
                    XmlPullParserFactory pullFactory = XmlPullParserFactory.newInstance();
                    XmlPullParser parser = pullFactory.newPullParser();
                    parser.setInput(inputStream, "utf-8");
                    AdbVerifyConfig adbUninstallVerifyConfig = new AdbVerifyConfig(1);
                    for (int eventCode = parser.getEventType(); eventCode != 1; eventCode = parser.next()) {
                        if (eventCode == 2) {
                            String name = parser.getName();
                            VSlog.i(TAG, " name:" + name);
                            if ("maxVerifyInterval".equals(name)) {
                                String value = parser.getAttributeValue(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, "value");
                                if (DEBUG) {
                                    VSlog.d(TAG, " value:" + value);
                                }
                                try {
                                    adbUninstallVerifyConfig.mMaxVerifyInterval = Long.parseLong(value);
                                } catch (Exception e) {
                                    VSlog.w(TAG, "verify interval, " + e.toString());
                                }
                            } else if ("unInstallFeatureOpen".equals(name)) {
                                String value2 = parser.getAttributeValue(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, "value");
                                if (DEBUG) {
                                    VSlog.d(TAG, " value " + value2);
                                    adbUninstallVerifyConfig.mFeatureOpen = Boolean.parseBoolean(value2);
                                }
                            }
                        } else if (eventCode == 3) {
                            parser.getName();
                        }
                    }
                    if (DEBUG) {
                        VSlog.d(TAG, "parser end print: " + adbUninstallVerifyConfig);
                    }
                    mAdbUninstallVerifyConfig = adbUninstallVerifyConfig;
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (Exception e2) {
                            e = e2;
                            sb = new StringBuilder();
                            sb.append("is close,");
                            sb.append(e.toString());
                            VSlog.w(TAG, sb.toString());
                        }
                    }
                } catch (Exception ioe) {
                    VSlog.w(TAG, "exception " + ioe.toString());
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (Exception e3) {
                            e = e3;
                            sb = new StringBuilder();
                            sb.append("is close,");
                            sb.append(e.toString());
                            VSlog.w(TAG, sb.toString());
                        }
                    }
                }
            } catch (XmlPullParserException e4) {
                VSlog.w(TAG, "xml parser, " + e4.toString());
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Exception e5) {
                        e = e5;
                        sb = new StringBuilder();
                        sb.append("is close,");
                        sb.append(e.toString());
                        VSlog.w(TAG, sb.toString());
                    }
                }
            }
        } catch (Throwable e6) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e7) {
                    VSlog.w(TAG, "is close," + e7.toString());
                }
            }
            throw e6;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class AdbVerifyConfig {
        boolean mFeatureOpen;
        long mMaxVerifyInterval;
        int mType;

        public AdbVerifyConfig(int type) {
            this.mType = type;
        }

        public AdbVerifyConfig(boolean featureOpen, long maxVerifyInterval, int type) {
            this.mFeatureOpen = featureOpen;
            this.mMaxVerifyInterval = maxVerifyInterval;
            this.mType = type;
        }

        public String toString() {
            return "open-> " + this.mFeatureOpen + "  interval-> " + this.mMaxVerifyInterval + "  type-> " + this.mType;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static AdbVerifyConfig getAdbUninstallVerifyConfig() {
        return mAdbUninstallVerifyConfig;
    }

    public boolean isGMSApk(String packageName) {
        if (packageName != null && ((packageName.contains("cts") || packageName.contains("gts") || packageName.contains("com.android.frameworks")) && packageName.contains(VivoPermissionUtils.OS_PKG))) {
            return true;
        }
        if ((packageName != null && packageName.startsWith("android.appenumeration")) || Arrays.asList(CTS_APPS).contains(packageName) || Arrays.asList(GTS_APPS).contains(packageName) || Arrays.asList(TF_APPS).contains(packageName) || Arrays.asList(ARCORE_APPS).contains(packageName)) {
            return true;
        }
        return false;
    }

    public boolean isNeedVerifyAdbInstall(String packageName, int installerUid, int installFlags) {
        if (mIsOverseas || installerUid != 2000 || (1073741824 & installFlags) != 0 || isGMSApk(packageName) || this.mPKMService.checkSignatures(packageName, "com.android.preconditions.cts") == 0 || this.mPKMService.checkSignatures(packageName, "com.android.compatibility.common.deviceinfo") == 0 || this.mPKMService.checkSignatures(packageName, "com.android.cts.encryptionapp") == 0) {
            return false;
        }
        if (!FtFeature.isFeatureSupport("vivo.software.pkms_adb_silent_install")) {
            VSlog.i(TAG, "vivo.software.pkms_adb_silent_install not open. ");
            return false;
        } else if (this.mIFeatureOpen && this.mSpecialIList.contains(this.mDeviceI)) {
            VSlog.i(TAG, "find special devices,silent");
            return false;
        } else if ("1".equals(SystemProperties.get("persist.sys.adb.install.debug", "0"))) {
            VSlog.w(TAG, "adb install debug ,silent");
            return false;
        } else if (DEBUG) {
            VSlog.d(TAG, "need verify adb install.");
            return true;
        } else {
            return true;
        }
    }

    public void writeSystemFixedPermissionSync(final int userId, final Map<String, Set<String>> pkgsPerm) {
        this.mPkmsUtilsHandler.postDelayed(new Runnable() { // from class: com.android.server.pm.-$$Lambda$VivoPKMSUtils$nXhT1pWYqm5C2WCcq05VG6GRkYE
            @Override // java.lang.Runnable
            public final void run() {
                VivoPKMSUtils.this.lambda$writeSystemFixedPermissionSync$0$VivoPKMSUtils(userId, pkgsPerm);
            }
        }, 100L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: writeSystemFixedPermission */
    public void lambda$writeSystemFixedPermissionSync$0$VivoPKMSUtils(int userId, Map<String, Set<String>> pkgsPerm) {
        VSlog.v(TAG, "begin to writeSystemFixedPermission, userId: " + userId);
        File systemFixedRuntimePermissionsFile = getSystemFixedRuntimePermissionsFile(userId);
        AtomicFile destination = new AtomicFile(systemFixedRuntimePermissionsFile, "package-perms-" + userId);
        FileOutputStream out = null;
        try {
            out = destination.startWrite();
            XmlSerializer serializer = Xml.newSerializer();
            serializer.setOutput(out, StandardCharsets.UTF_8.name());
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.startDocument(null, true);
            serializer.startTag(null, TAG_RUNTIME_PERMISSIONS);
            for (String packageName : pkgsPerm.keySet()) {
                try {
                    Set<String> systemFixedPerms = pkgsPerm.get(packageName);
                    if (systemFixedPerms != null && systemFixedPerms.size() > 0) {
                        serializer.startTag(null, TAG_PACKAGE);
                        serializer.attribute(null, "name", packageName);
                        for (String systemFixedPerm : systemFixedPerms) {
                            serializer.startTag(null, "item");
                            serializer.attribute(null, "name", systemFixedPerm);
                            serializer.endTag(null, "item");
                        }
                        serializer.endTag(null, TAG_PACKAGE);
                    }
                } catch (Throwable th) {
                    t = th;
                    try {
                        Slog.wtf(TAG, "Failed to write system fixed permissions", t);
                        destination.failWrite(out);
                    } finally {
                        IoUtils.closeQuietly(out);
                    }
                }
            }
            serializer.endTag(null, TAG_RUNTIME_PERMISSIONS);
            serializer.endDocument();
            destination.finishWrite(out);
        } catch (Throwable th2) {
            t = th2;
        }
    }

    private File getSystemFixedRuntimePermissionsFile(int userId) {
        File systemDir = new File(Environment.getDataDirectory(), "system");
        File userDir = new File(new File(systemDir, "users"), Integer.toString(userId));
        return new File(userDir, SYSTEM_FIXED_PKG_INFO);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void verifySystemFixedPermission(int userId) {
        XmlPullParser parser;
        String str;
        Map<String, Set<String>> map;
        String pkg;
        XmlPullParser parser2;
        Map<String, Set<String>> map2;
        XmlPullParser parser3;
        String str2;
        PackageInfo pkgInfo;
        Map<String, Set<String>> map3;
        String pkg2;
        VSlog.v(TAG, "begin verifySystemFixedPermission, userId = " + userId);
        File systemFixedPermissionsFile = getSystemFixedRuntimePermissionsFile(userId);
        if (!systemFixedPermissionsFile.exists()) {
            return;
        }
        try {
            FileInputStream in = new AtomicFile(systemFixedPermissionsFile).openRead();
            try {
                try {
                    XmlPullParser parser4 = Xml.newPullParser();
                    parser4.setInput(in, null);
                    Map<String, Set<String>> map4 = parseRuntimePermissionsLPr(parser4);
                    for (Map.Entry<String, Set<String>> entry : map4.entrySet()) {
                        String pkg3 = entry.getKey();
                        Set<String> perms = entry.getValue();
                        PackageInfo pkgInfo2 = this.mPKMService.getPackageInfo(pkg3, 4096, userId);
                        String str3 = "pkg: ";
                        if (pkgInfo2 == null) {
                            parser = parser4;
                            str = "pkg: ";
                            map = map4;
                            pkg = pkg3;
                        } else if (pkgInfo2.requestedPermissions == null) {
                            parser = parser4;
                            str = "pkg: ";
                            map = map4;
                            pkg = pkg3;
                        } else {
                            if (pkg3 == null || perms == null) {
                                parser2 = parser4;
                                map2 = map4;
                            } else {
                                for (String perm : perms) {
                                    if (ArrayUtils.contains(pkgInfo2.requestedPermissions, perm)) {
                                        if (perm == null || this.mPKMService.checkPermission(perm, pkg3, userId) == 0) {
                                            parser3 = parser4;
                                            str2 = str3;
                                            pkgInfo = pkgInfo2;
                                            map3 = map4;
                                            pkg2 = pkg3;
                                        } else {
                                            int flags = this.mPKMService.getPermissionFlags(perm, pkg3, userId);
                                            if ((flags & 4) == 0) {
                                                VSlog.v(TAG, "verifySystemFixedPermissionPwl, grant system fixed runtime permission: " + perm + " to pkg: " + pkg3);
                                                parser3 = parser4;
                                                str2 = str3;
                                                pkgInfo = pkgInfo2;
                                                map3 = map4;
                                                pkg2 = pkg3;
                                                this.mPKMService.updatePermissionFlags(perm, pkg3, flags, flags & (-17), true, userId);
                                                this.mPKMService.grantRuntimePermission(pkg2, perm, userId);
                                                this.mPKMService.updatePermissionFlags(perm, pkg2, flags | 16 | 32, flags | 16 | 32, true, userId);
                                            }
                                        }
                                        str3 = str2;
                                        pkg3 = pkg2;
                                        pkgInfo2 = pkgInfo;
                                        map4 = map3;
                                        parser4 = parser3;
                                    } else {
                                        VSlog.d(TAG, str3 + pkg3 + " do not request the perm: " + perm);
                                    }
                                }
                                parser2 = parser4;
                                map2 = map4;
                            }
                            map4 = map2;
                            parser4 = parser2;
                        }
                        VSlog.d(TAG, str + pkg + " is null, then continue");
                        map4 = map;
                        parser4 = parser;
                    }
                } catch (Exception e) {
                    VSlog.e(TAG, "Failed parsing permissions file:" + systemFixedPermissionsFile, e);
                }
            } finally {
                IoUtils.closeQuietly(in);
            }
        } catch (FileNotFoundException e2) {
            VSlog.i(TAG, "No  System fixed permissions state");
        }
    }

    private Map<String, Set<String>> parseRuntimePermissionsLPr(XmlPullParser parser) throws IOException, XmlPullParserException {
        HashMap<String, Set<String>> pkgPermMap = new HashMap<>();
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                break;
            } else if (type != 3 && type != 4) {
                String name = parser.getName();
                char c = 65535;
                if (name.hashCode() == 111052 && name.equals(TAG_PACKAGE)) {
                    c = 0;
                }
                if (c == 0) {
                    String packageName = parser.getAttributeValue(null, "name");
                    AndroidPackage pkg = this.mServiceInternal.getPackage(packageName);
                    if (pkg == null) {
                        Slog.w("PackageManager", "Unknown package:" + packageName);
                        XmlUtils.skipCurrentTag(parser);
                    } else {
                        Set<String> perms = parsePermissionsLPr(parser);
                        if (perms.size() > 0) {
                            pkgPermMap.put(packageName, perms);
                        }
                    }
                }
            }
        }
        return pkgPermMap;
    }

    private Set<String> parsePermissionsLPr(XmlPullParser parser) throws IOException, XmlPullParserException {
        ArraySet<String> arraySet = new ArraySet<>();
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                break;
            } else if (type != 3 && type != 4) {
                String name = parser.getName();
                char c = 65535;
                if (name.hashCode() == 3242771 && name.equals("item")) {
                    c = 0;
                }
                if (c == 0) {
                    String perm = parser.getAttributeValue(null, "name");
                    arraySet.add(perm);
                }
            }
        }
        return arraySet;
    }

    /* JADX WARN: Removed duplicated region for block: B:393:0x06d4 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:395:0x06dc A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public void collectEmmCertificates(com.android.server.pm.parsing.pkg.ParsedPackage r34, android.content.Context r35, boolean r36) throws android.content.pm.PackageParser.PackageParserException {
        /*
            Method dump skipped, instructions count: 1932
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.VivoPKMSUtils.collectEmmCertificates(com.android.server.pm.parsing.pkg.ParsedPackage, android.content.Context, boolean):void");
    }

    private static String[] convertStringsToArray(String inputStrings) {
        String[] outputStringArray = inputStrings.split(";");
        return outputStringArray;
    }

    private static String getPublicKey() {
        File pkgFile = new File(PUBLIC_KEY);
        if (pkgFile.exists() && 0 != pkgFile.length()) {
            BufferedReader reader = null;
            try {
                try {
                    reader = new BufferedReader(new FileReader(pkgFile));
                    StringBuilder sb = new StringBuilder();
                    while (true) {
                        String line = reader.readLine();
                        if (line == null) {
                            break;
                        } else if (!line.contains("---")) {
                            sb.append(line);
                        }
                    }
                    String sb2 = sb.toString();
                    try {
                        reader.close();
                    } catch (IOException e) {
                    }
                    return sb2;
                } catch (IOException e2) {
                    e2.printStackTrace();
                    if (reader != null) {
                        try {
                            reader.close();
                            return null;
                        } catch (IOException e3) {
                            return null;
                        }
                    }
                    return null;
                }
            } catch (Throwable th) {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e4) {
                    }
                }
                throw th;
            }
        }
        return null;
    }

    private static boolean verifySignature(String content, String encodedSign) {
        try {
            String publicKey = getPublicKey();
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            byte[] decodedKey = decode(publicKey);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKey);
            PublicKey pubKey = keyFactory.generatePublic(keySpec);
            java.security.Signature signature = java.security.Signature.getInstance("SHA256WithRSA");
            signature.initVerify(pubKey);
            signature.update(content.getBytes());
            boolean result = signature.verify(Base64.decode(encodedSign, 0));
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean compareDigest(String apkHash, byte[] content, String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            md.update(content);
            String result = bytes2Hex(md.digest());
            if (!result.equals(apkHash)) {
                return false;
            }
            return true;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean checkDateValid(String validPeriod) {
        if (TextUtils.isEmpty(validPeriod)) {
            return false;
        }
        String[] validPeriodArray = validPeriod.split("from | to ");
        try {
            Calendar calFrom = Calendar.getInstance();
            Calendar calTo = Calendar.getInstance();
            calFrom.setTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(validPeriodArray[1]));
            calTo.setTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(validPeriodArray[2]));
            long currentTime = System.currentTimeMillis();
            if (calFrom.getTimeInMillis() <= currentTime) {
                if (currentTime <= calTo.getTimeInMillis()) {
                    return true;
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static String getCertificateName(StrictJarFile jarFile) throws IOException {
        String path = null;
        Iterator<ZipEntry> i = jarFile.iterator();
        while (i.hasNext()) {
            ZipEntry entry = i.next();
            if (!entry.isDirectory()) {
                path = entry.getName();
                if (path.startsWith("META-INF/") && (path.endsWith(".DSA") || path.endsWith(".RSA") || path.endsWith(".EC"))) {
                    return path;
                }
            }
        }
        return path;
    }

    private static String bytes2Hex(byte[] bytes) {
        String result = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        for (byte b : bytes) {
            String tmp = Integer.toHexString(b & 255);
            if (tmp.length() == 1) {
                result = result + "0";
            }
            result = result + tmp;
        }
        return result;
    }

    public static void closeQuietly(StrictJarFile jarFile) {
        if (jarFile != null) {
            try {
                jarFile.close();
            } catch (Exception e) {
            }
        }
    }

    public static String encode(byte[] data) {
        StringBuffer sb = new StringBuffer();
        int len = data.length;
        int b1 = 0;
        while (true) {
            if (b1 >= len) {
                break;
            }
            int i = b1 + 1;
            int b12 = data[b1] & 255;
            if (i == len) {
                sb.append(base64EncodeChars[b12 >>> 2]);
                sb.append(base64EncodeChars[(b12 & 3) << 4]);
                break;
            }
            int i2 = i + 1;
            int b2 = data[i] & 255;
            if (i2 == len) {
                sb.append(base64EncodeChars[b12 >>> 2]);
                sb.append(base64EncodeChars[((b12 & 3) << 4) | ((b2 & BroadcastConfigs.PROXY_BR_ABNORMAL_SIZE) >>> 4)]);
                sb.append(base64EncodeChars[(b2 & 15) << 2]);
                break;
            }
            int i3 = i2 + 1;
            int b3 = data[i2] & 255;
            sb.append(base64EncodeChars[b12 >>> 2]);
            sb.append(base64EncodeChars[((b12 & 3) << 4) | ((b2 & BroadcastConfigs.PROXY_BR_ABNORMAL_SIZE) >>> 4)]);
            sb.append(base64EncodeChars[((b2 & 15) << 2) | ((b3 & 192) >>> 6)]);
            sb.append(base64EncodeChars[b3 & 63]);
            b1 = i3;
        }
        return sb.toString();
    }

    public static byte[] decode(String str) throws UnsupportedEncodingException {
        return decodePrivate(str);
    }

    private static byte[] decodePrivate(String str) throws UnsupportedEncodingException {
        StringBuffer sb = new StringBuffer();
        byte[] data = str.getBytes("US-ASCII");
        int len = data.length;
        int i = 0;
        while (len - i >= 4) {
            byte[] bArr = base64DecodeChars;
            int i2 = i + 1;
            byte b1 = bArr[data[i]];
            int i3 = i2 + 1;
            byte b2 = bArr[data[i2]];
            sb.append((char) ((b1 << 2) | ((b2 & 48) >>> 4)));
            int i4 = i3 + 1;
            byte b3 = base64DecodeChars[data[i3]];
            sb.append((char) (((b3 & 60) >>> 2) | ((b2 & 15) << 4)));
            int b4 = base64DecodeChars[data[i4]];
            sb.append((char) (((b3 & 3) << 6) | b4));
            i = i4 + 1;
        }
        int dis = len - i;
        if (1 == dis) {
            int i5 = i + 1;
            byte b12 = base64DecodeChars[data[i]];
            int temp = b12 << 2;
            if (temp > 0) {
                sb.append((char) temp);
            }
        } else if (2 == dis) {
            byte[] bArr2 = base64DecodeChars;
            int i6 = i + 1;
            byte b13 = bArr2[data[i]];
            int i7 = i6 + 1;
            byte b22 = bArr2[data[i6]];
            sb.append((char) ((b13 << 2) | ((b22 & 48) >>> 4)));
            int temp2 = (b22 & 15) << 4;
            if (temp2 > 0) {
                sb.append((char) temp2);
            }
        } else if (3 == dis) {
            byte[] bArr3 = base64DecodeChars;
            int i8 = i + 1;
            byte b14 = bArr3[data[i]];
            int i9 = i8 + 1;
            byte b23 = bArr3[data[i8]];
            sb.append((char) ((b14 << 2) | ((b23 & 48) >>> 4)));
            int i10 = i9 + 1;
            byte b32 = base64DecodeChars[data[i9]];
            sb.append((char) (((b32 & 60) >>> 2) | ((b23 & 15) << 4)));
            int temp3 = (b32 & 3) << 6;
            if (temp3 > 0) {
                sb.append((char) temp3);
            }
        }
        return sb.toString().getBytes("iso8859-1");
    }

    private static String getDeveloperKey(File apkfile) {
        String signature = null;
        JarFile jarFile = null;
        try {
            try {
                jarFile = new JarFile(apkfile);
                JarEntry je = jarFile.getJarEntry(ANDROID_MANIFEST_FILENAME);
                byte[] readBuffer = new byte[EmergencyBroadcastManager.FLAG_RECEIVER_KEYAPP];
                Certificate[] certs = loadCertificates(jarFile, je, readBuffer);
                if (certs != null && certs.length >= 1) {
                    signature = encodeBytes(certs[0].getEncoded());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return signature;
        } finally {
            IoUtils.closeQuietly(jarFile);
        }
    }

    private static Certificate[] loadCertificates(JarFile jarFile, JarEntry je, byte[] readBuffer) {
        InputStream is = null;
        try {
            is = jarFile.getInputStream(je);
            while (is.read(readBuffer, 0, readBuffer.length) != -1) {
            }
            return je != null ? je.getCertificates() : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            IoUtils.closeQuietly(is);
        }
    }

    private static String encodeBytes(byte[] bytes) {
        byte[] originalBytes = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(bytes);
            originalBytes = md5.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return encodeBytes2(originalBytes);
    }

    private static String encodeBytes2(byte[] bytes) {
        StringBuffer buf = new StringBuffer();
        for (int offset = 0; offset < bytes.length; offset++) {
            int i = bytes[offset];
            if (i < 0) {
                i += 256;
            }
            if (i < 16) {
                buf.append("0");
            }
            buf.append(Integer.toHexString(i));
        }
        return buf.toString();
    }

    /* loaded from: classes.dex */
    public class EmmPackage {
        public boolean add;
        public Bundle info;
        public String packageName;
        public int userHandle;

        public EmmPackage(String packageName, Bundle info, boolean add, int userHandle) {
            this.packageName = packageName;
            this.info = info;
            this.add = add;
            this.userHandle = userHandle;
        }
    }

    public void addEmmPackageCache(EmmPackage emmPackage) {
        this.emmPackageCacheList.add(emmPackage);
    }

    public static boolean isSupportCurrentPlatform(ParsedPackage packageToScan) {
        try {
            String packageName = packageToScan.getPackageName();
            Bundle metaData = packageToScan.getMetaData();
            if (metaData == null) {
                VSlog.e(TAG, " " + packageName + " do not set metaData, so do not intercept ");
                return true;
            } else if (isSupportOverseas(packageName, metaData) && isSupportRomVersion(packageName, metaData) && isSupportOsName(packageName, metaData) && isSupportAndroidVersion(packageName, metaData)) {
                if (isSupportAbi(packageToScan, metaData)) {
                    return isSupportInstall(packageName, metaData) && isSupportDeviceType(packageName, metaData);
                }
                VSlog.w(TAG, packageName + " can not be installed, because its supported abi is not right");
                return false;
            } else {
                return false;
            }
        } catch (Throwable e) {
            VSlog.e(TAG, e.toString());
            return true;
        }
    }

    public static boolean isSupportAbi(ParsedPackage packageToScan, Bundle metaData) {
        if (metaData == null) {
            VSlog.i(TAG, "isSupportAbi metaData is null");
            return true;
        }
        String metaDataAbi = metaData.getString(META_DATA_SUPPORT_ABI);
        if (TextUtils.isEmpty(metaDataAbi)) {
            return true;
        }
        String[] verArray = metaDataAbi.split(SPLIT_FLAG);
        String currentScanedAbi = packageToScan.getPrimaryCpuAbi();
        if (verArray == null || verArray.length == 0) {
            return true;
        }
        if (verArray.length == 1) {
            if (currentScanedAbi == null) {
                return false;
            }
            if (!"arm64-v8a".equals(currentScanedAbi) || BIT64.equals(metaDataAbi)) {
                return !("armeabi".equals(currentScanedAbi) || "armeabi-v7a".equals(currentScanedAbi)) || BIT32.equals(metaDataAbi);
            }
            return false;
        } else if (verArray.length != 2) {
            return true;
        } else {
            if ("arm64-v8a".equals(currentScanedAbi) && mIsBIT64Platform) {
                return true;
            }
            return ("armeabi".equals(currentScanedAbi) || "armeabi-v7a".equals(currentScanedAbi)) && !mIsBIT64Platform;
        }
    }

    private static boolean isSupportOverseas(String packageName, Bundle metaData) {
        try {
            if (metaData == null) {
                VSlog.i(TAG, "isSupportOverseas metaData is null");
                return true;
            }
            String overseas = metaData.getString(META_DATA_SUPPORT_OVERSEAS);
            if (TextUtils.isEmpty(overseas)) {
                return true;
            }
            if (!(mIsOverseas && overseas.contains("1")) && (mIsOverseas || !overseas.contains("0"))) {
                VSlog.w(TAG, packageName + " can not be installed, because its supported overseas is:" + overseas + " but current platform is: " + mIsOverseas);
                return false;
            }
            return true;
        } catch (Throwable e) {
            VSlog.e(TAG, e.toString());
            return true;
        }
    }

    public static boolean isSupportOsName(String packageName, Bundle metaData) {
        boolean support = false;
        try {
            if (metaData == null) {
                VSlog.i(TAG, "isSupportOsName metaData is null");
                return true;
            }
            String osName = metaData.getString(META_DATA_SUPPORT_OS_NAME);
            if (TextUtils.isEmpty(osName)) {
                return true;
            }
            String[] verArray = osName.split(SPLIT_FLAG);
            String currentOsName = getOsName();
            if (currentOsName != null && verArray != null) {
                if (!isValidConfig(verArray)) {
                    VSlog.w(TAG, packageName + " can not be installed, because META_DATA_SUPPORT_OS_NAME in your meta-data is wrong! Pls check your meta-data.");
                    return false;
                }
                int index = 0;
                while (true) {
                    if (index >= verArray.length) {
                        break;
                    } else if (!currentOsName.equals(verArray[index])) {
                        index++;
                    } else {
                        support = true;
                        break;
                    }
                }
                if (!support) {
                    VSlog.w(TAG, packageName + " can not be installed, because its supported osName is:" + osName + " but current platform is: " + currentOsName);
                }
                return support;
            }
            return true;
        } catch (Throwable th) {
            return true;
        }
    }

    private static boolean isSupportAndroidVersion(String packageName, Bundle metaData) {
        try {
            if (metaData == null) {
                VSlog.i(TAG, "isSupportAndroidVersion metaData is null");
                return true;
            }
            String androidVersion = metaData.getString(META_DATA_SUPPORT_ANDROID_VERSION);
            if (TextUtils.isEmpty(androidVersion)) {
                return true;
            }
            boolean androidVersionResult = processVersionResult(androidVersion, Build.VERSION.SDK_INT);
            if (!androidVersionResult) {
                VSlog.w(TAG, packageName + " can not be installed, because its supported androidVersion is:" + androidVersion + " but current platform is: " + androidVersionResult);
            }
            return androidVersionResult;
        } catch (Throwable e) {
            VSlog.e(TAG, e.toString());
            return true;
        }
    }

    private static boolean isSupportRomVersion(String packageName, Bundle metaData) {
        try {
            if (metaData == null) {
                VSlog.i(TAG, "isSupportAndroidVersion metaData is null");
                return true;
            }
            String configOsName = metaData.getString(META_DATA_SUPPORT_OS_NAME);
            if (TextUtils.isEmpty(configOsName)) {
                VSlog.d(TAG, "isSupportRomVersion configOsName is null");
                return true;
            }
            String currentOsName = getOsName();
            if (!configOsName.contains(currentOsName)) {
                VSlog.d(TAG, "isSupportRomVersion currentOsName = " + currentOsName + ",is error");
                return false;
            }
            String romVersion = metaData.getString(String.format(META_DATA_SUPPORT_ROM_VERSION, currentOsName));
            if (TextUtils.isEmpty(romVersion)) {
                VSlog.d(TAG, "isSupportRomVersion romVersion is null");
                return true;
            }
            boolean romVersionResult = processVersionResult(romVersion, mCurrentRomVersion);
            if (!romVersionResult) {
                VSlog.w(TAG, packageName + " can not be installed, because its supported romVersion is:" + romVersion + " but current platform is: " + mCurrentRomVersion);
            }
            return romVersionResult;
        } catch (Throwable e) {
            VSlog.e(TAG, e.toString());
            return true;
        }
    }

    private static boolean isSupportInstall(String packageName, Bundle metaData) {
        try {
            if (metaData == null) {
                VSlog.i(TAG, "isSupportInstall metaData is null");
                return true;
            }
            String not_install = metaData.getString(META_DATA_SUPPORT_NOT_INSTALL);
            if (!TextUtils.isEmpty(not_install) && "1".equals(not_install)) {
                VSlog.w(TAG, packageName + " can not be installed, because it do not support install.");
                return false;
            }
            return true;
        } catch (Throwable e) {
            VSlog.e(TAG, e.toString());
            return true;
        }
    }

    public static boolean isSupportDeviceType(String packageName, Bundle metaData) {
        if (metaData == null) {
            VSlog.i(TAG, "isSupportDeviceType metaData is null");
            return true;
        }
        String metaDeviceType = metaData.getString(META_DATA_SUPPORT_DEVICETYPE);
        if (TextUtils.isEmpty(metaDeviceType)) {
            return true;
        }
        String[] verArray = metaDeviceType.split(SPLIT_FLAG);
        String currentDeviceType = FtDeviceInfo.getDeviceType();
        if (verArray == null || verArray.length == 0 || ArrayUtils.contains(verArray, currentDeviceType)) {
            return true;
        }
        VSlog.w(TAG, packageName + " can not be installed, because metaDeviceType is " + Arrays.toString(verArray) + " but currentDeviceType is " + currentDeviceType);
        return false;
    }

    private static String getOsName() {
        try {
            String currentOsName = FtBuild.getOsName();
            return currentOsName;
        } catch (Throwable th) {
            return FUNTOUCH;
        }
    }

    private static boolean processVersionResult(String configVersion, float currentVersion) {
        List<PlatformVersionInfo> platformVersionInfos = fliterVersionInfo(configVersion);
        for (PlatformVersionInfo platformVersionInfo : platformVersionInfos) {
            boolean support = true;
            if (platformVersionInfo.getVersionType() == PlatformVersionInfo.NORMAL_TYPE) {
                if (platformVersionInfo.getVersion() != currentVersion) {
                    support = false;
                    continue;
                } else {
                    continue;
                }
            } else if (platformVersionInfo.getVersionType() == PlatformVersionInfo.BEFORE_TYPE) {
                if (platformVersionInfo.getVersion() < currentVersion) {
                    support = false;
                    continue;
                } else {
                    continue;
                }
            } else if (platformVersionInfo.getVersionType() == PlatformVersionInfo.LAST_TYPE) {
                if (platformVersionInfo.getVersion() > currentVersion) {
                    support = false;
                    continue;
                } else {
                    continue;
                }
            } else if (platformVersionInfo.getVersionType() == PlatformVersionInfo.EXCEPT_TYPE && platformVersionInfo.getVersion() == currentVersion) {
                support = false;
                continue;
            }
            if (support) {
                return true;
            }
        }
        return false;
    }

    private static List<PlatformVersionInfo> fliterVersionInfo(String configVersion) {
        List<PlatformVersionInfo> platformVersionInfos = new ArrayList<>();
        if (TextUtils.isEmpty(configVersion)) {
            return platformVersionInfos;
        }
        String[] verArray = configVersion.split(SPLIT_FLAG);
        if (verArray == null) {
            return platformVersionInfos;
        }
        for (int index = 0; index < verArray.length; index++) {
            PlatformVersionInfo platformVersionInfo = new PlatformVersionInfo();
            String version = Pattern.compile("[+!-]").matcher(verArray[index]).replaceAll(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK).trim();
            if (verArray[index].endsWith(BEFORE)) {
                platformVersionInfo.setVersionType(PlatformVersionInfo.BEFORE_TYPE);
            } else if (verArray[index].endsWith(LAST)) {
                platformVersionInfo.setVersionType(PlatformVersionInfo.LAST_TYPE);
            } else if (verArray[index].startsWith(EXCEPT)) {
                platformVersionInfo.setVersionType(PlatformVersionInfo.EXCEPT_TYPE);
            } else {
                version = verArray[index];
                platformVersionInfo.setVersionType(PlatformVersionInfo.NORMAL_TYPE);
            }
            if (!TextUtils.isEmpty(version)) {
                platformVersionInfo.setVersion(Float.valueOf(version).floatValue());
                platformVersionInfos.add(platformVersionInfo);
            }
        }
        return platformVersionInfos;
    }

    private static boolean isValidConfig(String[] configArray) {
        if (configArray == null) {
            return true;
        }
        for (int index = 0; index < configArray.length; index++) {
            if (TextUtils.isEmpty(configArray[index]) || "null".equals(configArray[index])) {
                return false;
            }
        }
        return true;
    }

    /* loaded from: classes.dex */
    public static class PlatformVersionInfo {
        private float version;
        private int versionType;
        public static int NORMAL_TYPE = 1;
        public static int BEFORE_TYPE = 2;
        public static int LAST_TYPE = 3;
        public static int EXCEPT_TYPE = 4;

        public int getVersionType() {
            return this.versionType;
        }

        public void setVersionType(int versionType) {
            this.versionType = versionType;
        }

        public float getVersion() {
            return this.version;
        }

        public void setVersion(float version) {
            this.version = version;
        }
    }

    public boolean isNotSupportAdbUninstall(String packageName) {
        Bundle metadata;
        if (this.mAdbUninstallBlackList.contains(packageName)) {
            VSlog.w(TAG, packageName + " can not be uninstall by shell. sblacklist");
            return true;
        } else if (this.mPKMService.getPackageInstallerPackageName().equals(packageName) || this.mPKMService.getPermissionControllerPackageName().equals(packageName)) {
            return true;
        } else {
            AndroidPackage pkg = (AndroidPackage) this.mPKMService.mPackages.get(packageName);
            if (pkg != null && (metadata = pkg.getMetaData()) != null && "1".equals(metadata.getString(META_DATA_NOT_ADB_UNINSTALL))) {
                VSlog.w(TAG, packageName + " can not be uninstall by shell. metadata");
                return true;
            }
            return false;
        }
    }
}