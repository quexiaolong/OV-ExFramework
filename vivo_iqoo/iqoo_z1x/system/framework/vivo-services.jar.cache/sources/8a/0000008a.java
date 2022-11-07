package com.android.server.am;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AtomicFile;
import android.util.Xml;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.policy.InputExceptionReport;
import com.bbk.appstore.frameworkassist.IFrameworkAssistAidlInterface;
import com.vivo.face.common.data.Constants;
import com.vivo.vcodetransbase.EventTransfer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;
import vivo.app.VivoFrameworkFactory;
import vivo.app.configuration.AbsConfigurationManager;
import vivo.app.configuration.ConfigurationObserver;
import vivo.app.configuration.StringList;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class AppCrashRescueUtil {
    private static final String ALLOW_ALL_NEKEYWORD_IDENTIFICATION = "all_ne_keyword";
    private static final int APPCRASH_DELETEDEX_DONE_PROCESSING = 2;
    private static final int APPCRASH_DELETEDEX_FAIL_PROCESSING = 3;
    private static final int APPCRASH_DELETEDEX_NO_PROCESSING = 0;
    private static final int APPCRASH_DELETEDEX_UNDER_PROCESSING = 1;
    private static final int APPCRASH_DOWNLOADAPK_DONE_PROCESSING = 6;
    private static final int APPCRASH_DOWNLOADAPK_FAIL_PROCESSING = 7;
    private static final int APPCRASH_DOWNLOADAPK_NO_PROCESSING = 4;
    private static final int APPCRASH_DOWNLOADAPK_UNDER_PROCESSING = 5;
    private static final int APPCRASH_INSTALLORIAPK_DONE_PROCESSING = 14;
    private static final int APPCRASH_INSTALLORIAPK_EXCEPTION_PROCESSING = 12;
    private static final int APPCRASH_INSTALLORIAPK_FAIL_PROCESSING = 15;
    private static final int APPCRASH_INSTALLORIAPK_NO_PROCESSING = 19;
    private static final int APPCRASH_INSTALLORIAPK_UNDER_PROCESSING = 13;
    private static final String APPCRASH_ODEX_DAMAGE_ERROR_TYPE_NAME = "odex_err_type";
    private static final int APPCRASH_REINSTALL_DONE_PROCESSING = 10;
    private static final int APPCRASH_REINSTALL_EXCEPTION_PROCESSING = 8;
    private static final int APPCRASH_REINSTALL_FAIL_PROCESSING = 11;
    private static final int APPCRASH_REINSTALL_NO_PROCESSING = 18;
    private static final int APPCRASH_REINSTALL_UNDER_PROCESSING = 9;
    private static final int APPCRASH_RESCANABI_DONE_PROCESSING = 16;
    private static final int APPCRASH_RESCANABI_FAIL_PROCESSING = 17;
    private static final String APPCRASH_RESCUE_3PARTY_LIST_NAME = "allow_thirdparty_app_list";
    private static final String APPCRASH_RESCUE_CONFIG_FILE = "/data/bbkcore/AppcrashRescue_config_1.0.xml";
    private static final String APPCRASH_RESCUE_JE_TYPE_NAME = "je_type";
    private static final String APPCRASH_RESCUE_NE_KEYWORD_NAME = "ne_keyword";
    private static final String APPCRASH_RESCUE_NE_TYPE_NAME = "ne_type";
    private static final String APPCRASH_RESCUE_SWITCH_NAME = "appcrash_switch";
    private static final String APPCRASH_RESCUE_UPDATE_TIMES_MAX_NAME = "update_times_max";
    private static AtomicFile APPCRASH_SAVING_FILE = null;
    private static ExecutorService APPCRASH_THREADPOLL = null;
    private static final String APPINFO_TAG = "name_times_downloadid";
    private static final String APPRESCUERECORD_TAG = "AppRescueRecord";
    private static final String CRASH_TAG = "AppCrashRescue";
    private static final String DOWNLOADFILE_IDENTIFICATION = "_rescue_";
    private static final String FILE_EXCEPTION_PROP = "debug.vivo.file.exception";
    private static final String IS_DEBUG_MODE_VALUE = "com.vivo.crashrescue.debug";
    private static final boolean IS_STRICT_PROJECT = false;
    private static final String NULL_CONFIGFILE_IDENTIFICATION = "dynamic_config_null";
    private static final String REQUEST_DOWNLOADURL_SUFFIX = "&params=downloadUrl";
    private static final String RESCUEAPPINFO_TAG = "RescueAppInfo";
    private static final int RESULT_RIGHT = 200;
    private static final int TIMEOUT_HTTP_SERVER_CONNECTTING = 5000;
    private static final int TIMEOUT_HTTP_SERVER_READ = 5000;
    private static final String UPDATETIMES_FILE = "appcrash.xml";
    private static final String VIVO_BBKLOG_ACTION = "android.vivo.bbklog.action.CHANGED";
    private static ConfigManagerUtil sConfigManagerUtil;
    private static ConfigurationObserver sConfiguration3partyListObserver;
    private static ConfigurationObserver sConfigurationJETypeListObserver;
    private static AbsConfigurationManager sConfigurationManager;
    private static ConfigurationObserver sConfigurationNEKeywordListObserver;
    private static ConfigurationObserver sConfigurationNETypeListObserver;
    private static ConfigurationObserver sConfigurationOdexErrTypeListObserver;
    private static ConfigurationObserver sConfigurationSwitchObserver;
    private static ConfigurationObserver sConfigurationUpdateTimesObserver;
    private static StringList sDexFileDamageErrorTypeStringList;
    private static AppCrashRescueUtil sInstance;
    private static StringList sRescue3partyAppStringList;
    private static StringList sRescueJETypeStringList;
    private static StringList sRescueNEKeywordStringList;
    private static StringList sRescueNETypeStringList;
    private static StringList sRescueSwitchStringList;
    private static StringList sRescueUpdateTimeStringList;
    private static IFrameworkAssistAidlInterface sVivoAppStoreProxy;
    private VivoAppStoreDeathRecipient VivoAppStoreDeathRecipient;
    private AppCrashBroadcastReceiver mAppCrashBroadcastReceiver;
    private Handler mAppErrorHandler;
    private HandlerThread mAppErrorThread;
    private final Context mContext;
    private DebugModeObserver mDebugModeObserver;
    private DownloadManager mDownloadManager;
    private final ActivityManagerService mService;
    private static boolean LOGDEBUG = false;
    private static ConcurrentHashMap<String, CrashAppInfo> crashAppInfoMap = new ConcurrentHashMap<>();
    private static String localDownloadDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
    private static final Object APPCRASHLOCK = new Object();
    private static String REQUEST_DOWNLOADURL_PRIFIX = null;
    private static final boolean IS_OVERSEA_TYPE = SystemProperties.get("ro.vivo.product.overseas", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK).equals("yes");
    private static boolean sIsDebugMode = false;
    private static List<String> sRescueSwitchList = new ArrayList<String>() { // from class: com.android.server.am.AppCrashRescueUtil.1
        {
            add("open");
        }
    };
    public static boolean sRescueSwitch = true;
    private static int sUpdateTimesMax = 3;
    private static int sDatalostCompareTimes = 2;
    private static List<String> sRescue3partyAppList = new ArrayList();
    private static List<String> sRescueNETypeList = new ArrayList<String>() { // from class: com.android.server.am.AppCrashRescueUtil.2
        {
            add("Aborted");
            add("Segmentation fault");
            add("Illegal instruction");
        }
    };
    private static List<String> sRescueJETypeList = new ArrayList<String>() { // from class: com.android.server.am.AppCrashRescueUtil.3
        {
            add("android.content.res.Resources$NotFoundException");
            add("java.lang.NoSuchMethodError");
            add("java.lang.ClassNotFoundException");
            add("java.lang.NoClassDefFoundError");
            add("java.lang.UnsatisfiedLinkError");
            add("java.lang.VerifyError");
            add("java.lang.NoSuchFieldError");
            add("java.lang.ExceptionInInitializerError");
        }
    };
    private static List<String> sRescueNEKeywordList = new ArrayList<String>() { // from class: com.android.server.am.AppCrashRescueUtil.4
        {
            add(AppCrashRescueUtil.ALLOW_ALL_NEKEYWORD_IDENTIFICATION);
        }
    };
    private static List<String> sRescueUpdateTimesMaxList = new ArrayList<String>() { // from class: com.android.server.am.AppCrashRescueUtil.5
        {
            add(InputExceptionReport.LEVEL_MEDIUM);
        }
    };
    private static List<String> sDexFileDamageErrorTypeList = new ArrayList<String>() { // from class: com.android.server.am.AppCrashRescueUtil.6
        {
            add("ClassStatus::kLast");
            add("DexFile::kDexNoIndex16");
            add("java.lang.VerifyError");
            add("VdexFile::GetQuickenedInfoOf");
            add("art::HInstructionBuilder");
            add("range0_.InSource");
            add("decompressed_size");
            add("accessor.InsnsSizeInCodeUnits");
        }
    };
    private static List<String> sRescueTypeNameList = new ArrayList<String>() { // from class: com.android.server.am.AppCrashRescueUtil.7
        {
            add(AppCrashRescueUtil.APPCRASH_RESCUE_SWITCH_NAME);
            add(AppCrashRescueUtil.APPCRASH_RESCUE_3PARTY_LIST_NAME);
            add(AppCrashRescueUtil.APPCRASH_RESCUE_NE_TYPE_NAME);
            add(AppCrashRescueUtil.APPCRASH_RESCUE_JE_TYPE_NAME);
            add(AppCrashRescueUtil.APPCRASH_RESCUE_NE_KEYWORD_NAME);
            add(AppCrashRescueUtil.APPCRASH_RESCUE_UPDATE_TIMES_MAX_NAME);
            add(AppCrashRescueUtil.APPCRASH_ODEX_DAMAGE_ERROR_TYPE_NAME);
        }
    };
    private static List<Long> sUndeletedFileDownloadid = new ArrayList();
    private boolean isInitReady = false;
    VivoAppStoreConnection mVivoAppStoreConnection = new VivoAppStoreConnection();

    AppCrashRescueUtil(Context context, ActivityManagerService service) {
        this.mContext = context;
        this.mService = service;
        HandlerThread handlerThread = new HandlerThread("appcrashRescueTrd");
        this.mAppErrorThread = handlerThread;
        handlerThread.start();
        this.mAppErrorHandler = new Handler(this.mAppErrorThread.getLooper());
    }

    public static synchronized AppCrashRescueUtil setInstance(Context context, ActivityManagerService service) {
        AppCrashRescueUtil appCrashRescueUtil;
        synchronized (AppCrashRescueUtil.class) {
            if (sInstance == null) {
                sInstance = new AppCrashRescueUtil(context, service);
            }
            appCrashRescueUtil = sInstance;
        }
        return appCrashRescueUtil;
    }

    public void init() {
        Handler handler = this.mAppErrorHandler;
        if (handler != null) {
            handler.post(new Runnable() { // from class: com.android.server.am.AppCrashRescueUtil.8
                @Override // java.lang.Runnable
                public void run() {
                    AppCrashRescueUtil.this.initializeAppcrashRescue();
                    AppCrashRescueUtil.this.registerAppCrashReceiver();
                    AppCrashRescueUtil.this.initializeRescueConfigFile();
                    AppCrashRescueUtil.this.updateRescueConfigFile();
                    AppCrashRescueUtil.this.isInitReady = true;
                }
            });
        }
    }

    public boolean initReady() {
        return this.isInitReady;
    }

    public static synchronized AppCrashRescueUtil getInstance() {
        AppCrashRescueUtil appCrashRescueUtil;
        synchronized (AppCrashRescueUtil.class) {
            appCrashRescueUtil = sInstance;
        }
        return appCrashRescueUtil;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initializeAppcrashRescue() {
        APPCRASH_THREADPOLL = new ThreadPoolExecutor(2, 20, 100L, TimeUnit.MILLISECONDS, new SynchronousQueue());
        this.mDownloadManager = (DownloadManager) this.mContext.getSystemService("download");
        File appcrashFile = new File(Environment.getDataSystemDirectory(), UPDATETIMES_FILE);
        if (!appcrashFile.exists()) {
            VSlog.i(CRASH_TAG, "saveAppcrashSettings: appcrashFile is not exist, create now. file path: " + appcrashFile.getAbsolutePath());
            boolean createResult = false;
            try {
                createResult = appcrashFile.createNewFile();
            } catch (IOException ioe) {
                VSlog.e(CRASH_TAG, "saveAppcrashSettings: ", ioe.fillInStackTrace());
            }
            if (createResult) {
                APPCRASH_SAVING_FILE = new AtomicFile(appcrashFile);
            } else {
                VSlog.d(CRASH_TAG, "create appcrashfile failed.");
            }
        } else {
            VSlog.i(CRASH_TAG, "appcrashFile exists, new atomicFile now.");
            APPCRASH_SAVING_FILE = new AtomicFile(appcrashFile);
            if (appcrashFile.length() != 0) {
                readAppCrashSettings();
            }
        }
        int value = Settings.Secure.getInt(this.mContext.getContentResolver(), IS_DEBUG_MODE_VALUE, 0);
        sIsDebugMode = value == 1;
        this.mDebugModeObserver = new DebugModeObserver();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initializeRescueConfigFile() {
        VivoFrameworkFactory vivoFrameworkFactory = VivoFrameworkFactory.getFrameworkFactoryImpl();
        if (vivoFrameworkFactory == null) {
            VSlog.e(CRASH_TAG, "vivoFrameworkFactory is null.");
            return;
        }
        try {
            AbsConfigurationManager configurationManager = vivoFrameworkFactory.getConfigurationManager();
            sConfigurationManager = configurationManager;
            if (configurationManager == null) {
                VSlog.e(CRASH_TAG, "sConfigurationManager is null.");
            } else {
                ConfigManagerUtil configManagerUtil = new ConfigManagerUtil(configurationManager);
                sConfigManagerUtil = configManagerUtil;
                configManagerUtil.getAllStringList();
                sConfigurationSwitchObserver = new ConfigurationObserver() { // from class: com.android.server.am.AppCrashRescueUtil.9
                    public void onConfigChange(String file, String name) {
                        AppCrashRescueUtil.this.mAppErrorHandler.post(new Runnable() { // from class: com.android.server.am.AppCrashRescueUtil.9.1
                            @Override // java.lang.Runnable
                            public void run() {
                                AppCrashRescueUtil.sConfigManagerUtil.updateRescueConfigFileList(AppCrashRescueUtil.APPCRASH_RESCUE_SWITCH_NAME);
                            }
                        });
                    }
                };
                sConfiguration3partyListObserver = new ConfigurationObserver() { // from class: com.android.server.am.AppCrashRescueUtil.10
                    public void onConfigChange(String file, String name) {
                        AppCrashRescueUtil.this.mAppErrorHandler.post(new Runnable() { // from class: com.android.server.am.AppCrashRescueUtil.10.1
                            @Override // java.lang.Runnable
                            public void run() {
                                AppCrashRescueUtil.sConfigManagerUtil.updateRescueConfigFileList(AppCrashRescueUtil.APPCRASH_RESCUE_3PARTY_LIST_NAME);
                            }
                        });
                    }
                };
                sConfigurationNETypeListObserver = new ConfigurationObserver() { // from class: com.android.server.am.AppCrashRescueUtil.11
                    public void onConfigChange(String file, String name) {
                        AppCrashRescueUtil.this.mAppErrorHandler.post(new Runnable() { // from class: com.android.server.am.AppCrashRescueUtil.11.1
                            @Override // java.lang.Runnable
                            public void run() {
                                AppCrashRescueUtil.sConfigManagerUtil.updateRescueConfigFileList(AppCrashRescueUtil.APPCRASH_RESCUE_NE_TYPE_NAME);
                            }
                        });
                    }
                };
                sConfigurationJETypeListObserver = new ConfigurationObserver() { // from class: com.android.server.am.AppCrashRescueUtil.12
                    public void onConfigChange(String file, String name) {
                        AppCrashRescueUtil.this.mAppErrorHandler.post(new Runnable() { // from class: com.android.server.am.AppCrashRescueUtil.12.1
                            @Override // java.lang.Runnable
                            public void run() {
                                AppCrashRescueUtil.sConfigManagerUtil.updateRescueConfigFileList(AppCrashRescueUtil.APPCRASH_RESCUE_JE_TYPE_NAME);
                            }
                        });
                    }
                };
                sConfigurationNEKeywordListObserver = new ConfigurationObserver() { // from class: com.android.server.am.AppCrashRescueUtil.13
                    public void onConfigChange(String file, String name) {
                        AppCrashRescueUtil.this.mAppErrorHandler.post(new Runnable() { // from class: com.android.server.am.AppCrashRescueUtil.13.1
                            @Override // java.lang.Runnable
                            public void run() {
                                AppCrashRescueUtil.sConfigManagerUtil.updateRescueConfigFileList(AppCrashRescueUtil.APPCRASH_RESCUE_NE_KEYWORD_NAME);
                            }
                        });
                    }
                };
                sConfigurationUpdateTimesObserver = new ConfigurationObserver() { // from class: com.android.server.am.AppCrashRescueUtil.14
                    public void onConfigChange(String file, String name) {
                        AppCrashRescueUtil.this.mAppErrorHandler.post(new Runnable() { // from class: com.android.server.am.AppCrashRescueUtil.14.1
                            @Override // java.lang.Runnable
                            public void run() {
                                AppCrashRescueUtil.sConfigManagerUtil.updateRescueConfigFileList(AppCrashRescueUtil.APPCRASH_RESCUE_UPDATE_TIMES_MAX_NAME);
                            }
                        });
                    }
                };
                sConfigurationOdexErrTypeListObserver = new ConfigurationObserver() { // from class: com.android.server.am.AppCrashRescueUtil.15
                    public void onConfigChange(String file, String name) {
                        AppCrashRescueUtil.this.mAppErrorHandler.post(new Runnable() { // from class: com.android.server.am.AppCrashRescueUtil.15.1
                            @Override // java.lang.Runnable
                            public void run() {
                                AppCrashRescueUtil.sConfigManagerUtil.updateRescueConfigFileList(AppCrashRescueUtil.APPCRASH_ODEX_DAMAGE_ERROR_TYPE_NAME);
                            }
                        });
                    }
                };
                sConfigManagerUtil.registerAllObserver();
            }
        } catch (Exception e) {
            VSlog.e(CRASH_TAG, "catch ex in initializeAppcrashRescue", e.fillInStackTrace());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateRescueConfigFile() {
        for (String name : sRescueTypeNameList) {
            sConfigManagerUtil.updateRescueConfigFileList(name);
        }
    }

    private boolean isNeedRescan(ProcessRecord app) {
        String primaryCpuAbi = app.info.primaryCpuAbi;
        String sourceDir = app.info.sourceDir;
        if (sourceDir == null) {
            return false;
        }
        String libPath = sourceDir.substring(0, sourceDir.lastIndexOf("/")) + "/lib";
        File libFile = new File(libPath);
        File libFile64 = new File(libPath + "/arm64");
        File libFile32 = new File(libPath + "/arm");
        VSlog.i(CRASH_TAG, "primaryCpuAbi is: " + primaryCpuAbi + ", libPath path is " + libPath);
        if (libFile.exists()) {
            if (libFile64.exists()) {
                if (!"arm64-v8a".equals(primaryCpuAbi)) {
                    VSlog.e(CRASH_TAG, "abi error:lib is arm64, current abi is " + primaryCpuAbi + ", need rescan.");
                    return true;
                }
            } else if (libFile32.exists() && !"armeabi-v7a".equals(primaryCpuAbi) && !"armeabi".equals(primaryCpuAbi)) {
                VSlog.e(CRASH_TAG, "abi error:lib is arm, current abi is " + primaryCpuAbi + ", need rescan.");
                return true;
            }
        }
        return false;
    }

    private boolean isSystemAppInstall(ProcessRecord app) {
        if (app == null) {
            VSlog.e(CRASH_TAG, "isSystemAppInstall: ProcessRecord is null.");
            return false;
        }
        return app.info.isSystemApp();
    }

    private void forceDeleteOatFile(ProcessRecord app, CrashAppInfo crashappInfo) {
        crashappInfo.currentRescueStatus = 1;
        String pkgName = app.info.packageName;
        try {
            this.mService.getPackageManagerInternalLocked().forceDeleteOatFile(pkgName);
            VSlog.i(CRASH_TAG, "forceDeleteOatFile successful. pkg name: " + pkgName);
            crashappInfo.currentRescueStatus = 2;
            reportBigData(2, crashappInfo, 1);
            installPkgOrUpdateCrashApp(app, crashappInfo);
        } catch (Exception e) {
            VSlog.e(CRASH_TAG, "catch ex in forceDeleteOatFile:", e.fillInStackTrace());
            crashappInfo.currentRescueStatus = 3;
            reportBigData(3, crashappInfo, 0);
            installPkgOrUpdateCrashApp(app, crashappInfo);
        }
    }

    private void installPkgOrUpdateCrashApp(ProcessRecord app, CrashAppInfo crashappInfo) {
        if (IS_OVERSEA_TYPE) {
            VSlog.w(CRASH_TAG, "oversea type, won't install, handle done.");
        } else if (isApkUnderDataPath(crashappInfo, app)) {
            installPkgByStore(crashappInfo, false, app);
        } else {
            updateCrashApp(app, crashappInfo);
        }
    }

    private void updateCrashApp(ProcessRecord app, CrashAppInfo appinfo) {
        fetchDownloadUrl(appinfo);
        if (appinfo.downloadApkUri == null) {
            VSlog.e(CRASH_TAG, "downloadApkUri is null, return.");
            return;
        }
        String downloadUrl = appinfo.downloadApkUri;
        String apkname = appinfo.packageName + DOWNLOADFILE_IDENTIFICATION + appinfo.updateAppTimes + ".apk";
        File downloadedFile = new File(localDownloadDirectory, apkname);
        Uri localUri = Uri.fromFile(downloadedFile);
        appinfo.apkAbsolutePath = localUri.getPath();
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
        VSlog.i(CRASH_TAG, "localUri is " + localUri.getPath());
        request.setDestinationUri(localUri);
        request.setTitle("DownloadManager");
        request.setDescription("downloading " + appinfo.packageName);
        request.setAllowedNetworkTypes(2);
        request.setVisibleInDownloadsUi(false);
        request.setShowRunningNotification(false);
        DownloadManager downloadManager = this.mDownloadManager;
        if (downloadManager != null) {
            appinfo.downloadId = downloadManager.enqueue(request);
            VSlog.i(CRASH_TAG, "download task enqueue, downloadId: " + appinfo.downloadId);
            appinfo.currentRescueStatus = 5;
            saveAppcrashSettings();
        }
    }

    private boolean isApkUnderDataPath(CrashAppInfo appinfo, ProcessRecord processRecord) {
        String filePath = processRecord.info.sourceDir;
        VSlog.i(CRASH_TAG, "filePath:" + filePath);
        String[] paths = filePath.split("/");
        if (paths != null && paths.length >= 3 && "data".equals(paths[1]) && "app".equals(paths[2])) {
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void bindVivoAppstore() {
        if (IS_OVERSEA_TYPE) {
            VSlog.w(CRASH_TAG, "oversea type, won't bind appstore.");
            return;
        }
        Intent intent = new Intent();
        intent.setAction("com.bbk.appstore.frameworkassist.IFrameworkAssistAidlInterface");
        intent.setPackage("com.bbk.appstore");
        VSlog.i(CRASH_TAG, "start to bind vivoAppstore.");
        int delayTime = SystemProperties.getInt("sys.crashRescue.delaytime", 1);
        if (delayTime != 0) {
            try {
                Thread.sleep(delayTime * 1000);
            } catch (Exception e) {
                VSlog.e(CRASH_TAG, "catch ex in bindVivoAppstore.");
            }
        }
        this.mContext.bindService(intent, this.mVivoAppStoreConnection, 1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class VivoAppStoreConnection implements ServiceConnection {
        private VivoAppStoreConnection() {
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            VSlog.i(AppCrashRescueUtil.CRASH_TAG, "appstore connected.");
            IFrameworkAssistAidlInterface unused = AppCrashRescueUtil.sVivoAppStoreProxy = IFrameworkAssistAidlInterface.Stub.asInterface(service);
            VivoAppStoreDeathRecipient vivoAppStoreDeathRecipient = new VivoAppStoreDeathRecipient(name);
            try {
                service.linkToDeath(vivoAppStoreDeathRecipient, 0);
            } catch (Exception e) {
                VSlog.e(AppCrashRescueUtil.CRASH_TAG, "catch ex in linkToDeath", e.fillInStackTrace());
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            VSlog.i(AppCrashRescueUtil.CRASH_TAG, "appstore disconnected.");
        }
    }

    /* loaded from: classes.dex */
    private class VivoAppStoreDeathRecipient implements IBinder.DeathRecipient {
        private final ComponentName mComponentName;

        VivoAppStoreDeathRecipient(ComponentName name) {
            this.mComponentName = name;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            VSlog.e(AppCrashRescueUtil.CRASH_TAG, "vivo appstore service died, componet name: " + this.mComponentName + ", retry to bind it.");
            if (AppCrashRescueUtil.sVivoAppStoreProxy != null) {
                AppCrashRescueUtil.sVivoAppStoreProxy.asBinder().unlinkToDeath(this, 0);
            }
            IFrameworkAssistAidlInterface unused = AppCrashRescueUtil.sVivoAppStoreProxy = null;
            AppCrashRescueUtil.this.bindVivoAppstore();
        }
    }

    private void installPkgByStore(CrashAppInfo appinfo, boolean isUpdateApp, ProcessRecord processRecord) {
        if (sVivoAppStoreProxy != null) {
            if (isUpdateApp) {
                appinfo.currentRescueStatus = 9;
            } else {
                if (processRecord != null) {
                    appinfo.apkAbsolutePath = processRecord.info.sourceDir;
                }
                appinfo.currentRescueStatus = 13;
            }
            VSlog.i(CRASH_TAG, "appstore install start, apk path is: " + appinfo.apkAbsolutePath + ", isUpdateApp: " + isUpdateApp);
            try {
                int installResult = sVivoAppStoreProxy.assist(appinfo.apkAbsolutePath, false);
                VSlog.i(CRASH_TAG, "appstore install result: " + installResult);
                if (isUpdateApp) {
                    appinfo.updateAppTimes++;
                    if (installResult == 1) {
                        appinfo.currentRescueStatus = 10;
                        reportBigData(10, appinfo, installResult);
                    } else {
                        appinfo.currentRescueStatus = 11;
                        reportBigData(11, appinfo, installResult);
                    }
                    this.mDownloadManager.remove(appinfo.downloadId);
                    appinfo.downloadId = 0L;
                    saveAppcrashSettings();
                    return;
                } else if (installResult == 1) {
                    appinfo.currentRescueStatus = 14;
                    reportBigData(14, appinfo, installResult);
                    return;
                } else {
                    appinfo.currentRescueStatus = 15;
                    reportBigData(15, appinfo, installResult);
                    return;
                }
            } catch (Exception e) {
                VSlog.e(CRASH_TAG, "catch ex in appstore.assit. ", e.fillInStackTrace());
                if (isUpdateApp) {
                    appinfo.currentRescueStatus = 8;
                    return;
                } else {
                    appinfo.currentRescueStatus = 12;
                    return;
                }
            }
        }
        VSlog.e(CRASH_TAG, "appstore proxy is null, can't install by it.");
        if (isUpdateApp) {
            appinfo.currentRescueStatus = 18;
        } else {
            appinfo.currentRescueStatus = 19;
        }
    }

    private void parseDownloadUrl(String json, CrashAppInfo appinfo) {
        if (TextUtils.isEmpty(json)) {
            VSlog.e(CRASH_TAG, "parseDownloadUrl but json is null, return.");
            return;
        }
        try {
            JSONObject mainJson = new JSONObject(json);
            try {
                JSONArray jsonArray = mainJson.getJSONArray("value");
                try {
                    JSONObject urlJson = jsonArray.getJSONObject(0);
                    if (urlJson != null) {
                        try {
                            String downloadUrl = urlJson.getString("downloadURL");
                            VSlog.i(CRASH_TAG, "parse downloadUrl is " + downloadUrl);
                            if (TextUtils.isEmpty(downloadUrl)) {
                                VSlog.v(CRASH_TAG, "parse downloadUrl failed.");
                                return;
                            } else {
                                appinfo.downloadApkUri = downloadUrl;
                                return;
                            }
                        } catch (Exception e) {
                            VSlog.v(CRASH_TAG, "parse downloadUrl err," + e.getMessage());
                            return;
                        }
                    }
                    VSlog.e(CRASH_TAG, "urlJson is null, return.");
                } catch (Exception e2) {
                    VSlog.e(CRASH_TAG, "parse urlJson err," + e2.getMessage());
                }
            } catch (Exception e3) {
                VSlog.e(CRASH_TAG, "parse jsonArray err," + e3.getMessage());
            }
        } catch (Exception e4) {
            VSlog.e(CRASH_TAG, "parse mainJson err," + e4.getMessage());
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:53:0x0114  */
    /* JADX WARN: Removed duplicated region for block: B:56:0x010b A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private void fetchDownloadUrl(com.android.server.am.AppCrashRescueUtil.CrashAppInfo r18) {
        /*
            Method dump skipped, instructions count: 280
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.AppCrashRescueUtil.fetchDownloadUrl(com.android.server.am.AppCrashRescueUtil$CrashAppInfo):void");
    }

    private void refreshCrashAppInfo(ProcessRecord app) {
        String pkgName = app.info.packageName;
        if (!crashAppInfoMap.containsKey(pkgName)) {
            createCrashAppInfo(app);
            return;
        }
        CrashAppInfo appinfo = crashAppInfoMap.get(pkgName);
        if (appinfo.processRecord == null) {
            appinfo.processRecord = app;
            appinfo.packageName = app.info.packageName;
            appinfo.isSystemApp = isSystemAppInstall(app);
            crashAppInfoMap.put(pkgName, appinfo);
            return;
        }
        updateCrashAppInfo(app);
    }

    private void getDatalostInfo(CrashAppInfo appinfo) {
        boolean damageTypeRepeat;
        String exType = appinfo.exceptionType;
        String exMsg = appinfo.exceptionMsg;
        String exStacktrace = appinfo.exceptionStacktrace;
        boolean alreayReportToEPM = false;
        if (appinfo.datalostInfo != null) {
            alreayReportToEPM = appinfo.datalostInfo.reportAlready;
        }
        if (TextUtils.isEmpty(exMsg) || TextUtils.isEmpty(exType) || TextUtils.isEmpty(exStacktrace) || sDexFileDamageErrorTypeList == null || alreayReportToEPM) {
            return;
        }
        if ("Native crash".equals(exType)) {
            if (appinfo.datalostInfo == null) {
                DatalostCollectInfo lostInfo = new DatalostCollectInfo();
                lostInfo.currentOdexDamageType = DatalostCollectInfo.getCurrentOdexDamageType(exStacktrace);
                appinfo.datalostInfo = lostInfo;
                return;
            }
            String lastDamageType = appinfo.datalostInfo.currentOdexDamageType;
            damageTypeRepeat = TextUtils.isEmpty(lastDamageType) ? false : exStacktrace.contains(lastDamageType);
            if (damageTypeRepeat) {
                appinfo.datalostInfo.reportAlready = true;
                SystemProperties.set(FILE_EXCEPTION_PROP, "odex");
                return;
            }
            DatalostCollectInfo datalostCollectInfo = appinfo.datalostInfo;
            DatalostCollectInfo datalostCollectInfo2 = appinfo.datalostInfo;
            datalostCollectInfo.currentOdexDamageType = DatalostCollectInfo.getCurrentOdexDamageType(exStacktrace);
            return;
        }
        DatalostCollectInfo datalostCollectInfo3 = appinfo.datalostInfo;
        String str = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        if (datalostCollectInfo3 == null) {
            DatalostCollectInfo lostInfo2 = new DatalostCollectInfo();
            if (sDexFileDamageErrorTypeList.contains(exType)) {
                str = exType;
            }
            lostInfo2.currentOdexDamageType = str;
            appinfo.datalostInfo = lostInfo2;
            return;
        }
        String lastDamageType2 = appinfo.datalostInfo.currentOdexDamageType;
        damageTypeRepeat = TextUtils.isEmpty(lastDamageType2) ? false : exType.equals(lastDamageType2);
        if (damageTypeRepeat) {
            appinfo.datalostInfo.reportAlready = true;
            return;
        }
        DatalostCollectInfo datalostCollectInfo4 = appinfo.datalostInfo;
        if (sDexFileDamageErrorTypeList.contains(exType)) {
            str = exType;
        }
        datalostCollectInfo4.currentOdexDamageType = str;
    }

    public void handleAppCrashTooMuch(final ProcessRecord app, final HashMap<String, String> map) {
        String pkgName = app.info.packageName;
        if (app.userId != 0) {
            VSlog.w(CRASH_TAG, "pkgname: " + pkgName + ", userid: " + app.userId + ", we won't handle appcrash for it.");
            return;
        }
        refreshCrashAppInfo(app);
        final CrashAppInfo currentCrashAppInfo = crashAppInfoMap.get(pkgName);
        if (APPCRASH_THREADPOLL != null) {
            Runnable appCrashRunnable = new Runnable() { // from class: com.android.server.am.AppCrashRescueUtil.16
                @Override // java.lang.Runnable
                public void run() {
                    AppCrashRescueUtil.this.handleAppCrashTooMuchInThreadPool(app, map, currentCrashAppInfo);
                }
            };
            APPCRASH_THREADPOLL.execute(appCrashRunnable);
            return;
        }
        VSlog.e(CRASH_TAG, "APPCRASH_THREADPOLL is null!");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleAppCrashTooMuchInThreadPool(ProcessRecord app, HashMap<String, String> map, CrashAppInfo currentCrashAppInfo) {
        if (sVivoAppStoreProxy == null) {
            bindVivoAppstore();
        }
        String pkgName = app.info.packageName;
        if (currentCrashAppInfo != null) {
            currentCrashAppInfo.appCrashCount++;
            long timeDiff = SystemClock.elapsedRealtime() - currentCrashAppInfo.appcrashTimeMills;
            String exType = map.get("shortMsg");
            String exMsg = map.get("longMsg");
            String exStackTrace = map.get("stackTrace");
            currentCrashAppInfo.exceptionType = exType;
            currentCrashAppInfo.exceptionMsg = exMsg;
            currentCrashAppInfo.exceptionStacktrace = exStackTrace;
            getDatalostInfo(currentCrashAppInfo);
            rescanAbiCheck(currentCrashAppInfo, app);
            currentCrashAppInfo.appcrashTimeMills = SystemClock.elapsedRealtime();
            if (currentCrashAppInfo.appCrashCount == 1) {
                VSlog.i(CRASH_TAG, "first crash, ex type is " + exType + ", ex msg is " + exMsg + ", just do checkAbiCheck.");
            } else if (shouldHandleRescue(pkgName, timeDiff, currentCrashAppInfo, exType, exMsg, exStackTrace)) {
                if (currentCrashAppInfo.currentRescueStatus >= 4 && currentCrashAppInfo.currentRescueStatus <= 6) {
                    downloadApkStatusCheck(currentCrashAppInfo.downloadId, false);
                }
                VSlog.w(CRASH_TAG, "handle appcrash now! current rescue status: " + currentCrashAppInfo.currentRescueStatus);
                int i = currentCrashAppInfo.currentRescueStatus;
                if (i == 0) {
                    forceDeleteOatFile(app, currentCrashAppInfo);
                } else if (i == 1) {
                    VSlog.i(CRASH_TAG, "wait for delete dex done.");
                } else if (i == 18) {
                    VSlog.i(CRASH_TAG, "reinstall frash apk met appstore err, try again!");
                    installPkgByStore(currentCrashAppInfo, true, null);
                } else if (i == 19) {
                    VSlog.i(CRASH_TAG, "install orignal apk met appstore err, try again!");
                    installPkgByStore(currentCrashAppInfo, false, app);
                } else {
                    switch (i) {
                        case 5:
                            VSlog.i(CRASH_TAG, "wait for downloading apk done.");
                            return;
                        case 6:
                            VSlog.i(CRASH_TAG, "download apk success!");
                            return;
                        case 7:
                            VSlog.i(CRASH_TAG, "download apk failed!");
                            return;
                        case 8:
                            VSlog.i(CRASH_TAG, "reinstall frash apk produces an ex, try again!");
                            installPkgByStore(currentCrashAppInfo, true, null);
                            return;
                        case 9:
                            VSlog.i(CRASH_TAG, "wait for reinstall fresh apk done.");
                            return;
                        case 10:
                            VSlog.i(CRASH_TAG, "Already handle done!");
                            return;
                        case 11:
                            VSlog.i(CRASH_TAG, "reinstall fresh apk failed!");
                            return;
                        case 12:
                            VSlog.i(CRASH_TAG, "install orignal apk produces an ex, try again!");
                            installPkgByStore(currentCrashAppInfo, false, app);
                            return;
                        case 13:
                            VSlog.i(CRASH_TAG, "wait for install orignal apk done.");
                            return;
                        case 14:
                            VSlog.i(CRASH_TAG, "install orignal apk done.Do update app next time.");
                            updateCrashApp(app, currentCrashAppInfo);
                            return;
                        case 15:
                            VSlog.i(CRASH_TAG, "install orignal apk failed.Do update app next time.");
                            updateCrashApp(app, currentCrashAppInfo);
                            return;
                        default:
                            return;
                    }
                }
            }
        }
    }

    private boolean shouldHandleRescue(String pkgName, long timeInterval, CrashAppInfo appinfo, String extype, String exmsg, String exStacktrace) {
        boolean result;
        boolean allowRescueApp = allowRescueCurApp(appinfo);
        boolean allowRescueCrashType = allowRescueCurCrashType(extype, exmsg, exStacktrace);
        int updateAppTimes = appinfo.updateAppTimes;
        if (sIsDebugMode) {
            result = true;
            VSlog.w(CRASH_TAG, "NOTICE:appcrash rescue debug mode open!");
            allowRescueApp = true;
            allowRescueCrashType = true;
        } else {
            result = timeInterval < 60000 && allowRescueApp && allowRescueCrashType && updateAppTimes <= sUpdateTimesMax;
        }
        VSlog.i(CRASH_TAG, "===>pkgName: " + pkgName + ", current status: " + appinfo.currentRescueStatus + ", isSystemApp: " + appinfo.isSystemApp + "\n allowRescueCurApp: " + allowRescueApp + ", allowRescueCurCrashType: " + allowRescueCrashType + ", ex type: " + extype + "\n ex msg is " + exmsg + ", crash count: " + appinfo.appCrashCount + ", updateAppTimes: " + updateAppTimes + ", timeDiff: " + timeInterval);
        return result;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class DebugModeObserver extends ContentObserver {
        private final Uri isDebugModeUri;

        public DebugModeObserver() {
            super(AppCrashRescueUtil.this.mAppErrorHandler);
            this.isDebugModeUri = Settings.Secure.getUriFor(AppCrashRescueUtil.IS_DEBUG_MODE_VALUE);
            ContentResolver resolver = AppCrashRescueUtil.this.mContext.getContentResolver();
            resolver.registerContentObserver(this.isDebugModeUri, false, this, 0);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri, int userId) {
            AppCrashRescueUtil.this.mAppErrorHandler.post(new Runnable() { // from class: com.android.server.am.AppCrashRescueUtil.DebugModeObserver.1
                @Override // java.lang.Runnable
                public void run() {
                    int value = Settings.Secure.getInt(AppCrashRescueUtil.this.mContext.getContentResolver(), AppCrashRescueUtil.IS_DEBUG_MODE_VALUE, 0);
                    VSlog.w(AppCrashRescueUtil.CRASH_TAG, "IS_DEBUG_MODE_VALUE:" + value);
                    if (value == 1) {
                        boolean unused = AppCrashRescueUtil.sIsDebugMode = true;
                    } else {
                        boolean unused2 = AppCrashRescueUtil.sIsDebugMode = false;
                    }
                }
            });
        }
    }

    private void saveAppcrashSettings() {
        synchronized (APPCRASHLOCK) {
            if (APPCRASH_SAVING_FILE != null) {
                FileOutputStream fos = null;
                try {
                    fos = APPCRASH_SAVING_FILE.startWrite();
                    XmlSerializer serializer = Xml.newSerializer();
                    serializer.setOutput(fos, StandardCharsets.UTF_8.name());
                    serializer.startDocument(null, true);
                    serializer.startTag(null, APPRESCUERECORD_TAG);
                    for (Map.Entry<String, CrashAppInfo> entry : crashAppInfoMap.entrySet()) {
                        serializer.startTag(null, RESCUEAPPINFO_TAG);
                        serializer.startTag(null, APPINFO_TAG);
                        String info = entry.getValue().packageName + "_" + entry.getValue().updateAppTimes + "_" + entry.getValue().downloadId;
                        VSlog.i(CRASH_TAG, "saveAppcrashSettings info is " + info);
                        serializer.text(info);
                        serializer.endTag(null, APPINFO_TAG);
                        serializer.endTag(null, RESCUEAPPINFO_TAG);
                    }
                    serializer.endTag(null, APPRESCUERECORD_TAG);
                    serializer.endDocument();
                    APPCRASH_SAVING_FILE.finishWrite(fos);
                } catch (IOException e) {
                    if (fos != null) {
                        APPCRASH_SAVING_FILE.failWrite(fos);
                    }
                    VSlog.e(CRASH_TAG, "saveAppcrashSettings ex", e.fillInStackTrace());
                }
            } else {
                VSlog.e(CRASH_TAG, "saveAppcrashSettings: APPCRASH_SAVING_FILE is null");
            }
        }
    }

    private void readAppCrashSettings() {
        File appcrashFile;
        FileInputStream fis = null;
        try {
            appcrashFile = new File(Environment.getDataSystemDirectory(), UPDATETIMES_FILE);
        } catch (Exception e) {
            VSlog.e(CRASH_TAG, "readAppCrashSettings: catch ex:", e.fillInStackTrace());
        }
        if (!appcrashFile.exists()) {
            VSlog.e(CRASH_TAG, "appcrashFile is not exist, we can't read it.");
            return;
        }
        fis = new FileInputStream(appcrashFile);
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(fis, StandardCharsets.UTF_8.name());
        for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
            String tagName = parser.getName();
            if (eventType == 2 && APPINFO_TAG.equals(tagName)) {
                String tempStr = parser.nextText();
                String[] tempStrArr = tempStr.split("_");
                if (tempStr != null && tempStrArr.length == 3) {
                    CrashAppInfo crashInfoTemp = new CrashAppInfo();
                    crashInfoTemp.updateAppTimes = Integer.parseInt(tempStrArr[1]);
                    crashInfoTemp.downloadId = Integer.parseInt(tempStrArr[2]);
                    crashAppInfoMap.put(tempStrArr[0], crashInfoTemp);
                    if (crashInfoTemp.downloadId != 0) {
                        sUndeletedFileDownloadid.add(Long.valueOf(crashInfoTemp.downloadId));
                    }
                }
            }
        }
        if (fis != null) {
            try {
                fis.close();
            } catch (IOException e2) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class ConfigManagerUtil {
        private AbsConfigurationManager mConfigManager;

        public ConfigManagerUtil(AbsConfigurationManager configManager) {
            this.mConfigManager = configManager;
        }

        public void getAllStringList() {
            int stringListLength = AppCrashRescueUtil.sRescueTypeNameList.size();
            for (int num = 0; num < stringListLength; num++) {
                String typeName = (String) AppCrashRescueUtil.sRescueTypeNameList.get(num);
                VSlog.i(AppCrashRescueUtil.CRASH_TAG, "getAllStringList: typeName: " + typeName);
                char c = 65535;
                switch (typeName.hashCode()) {
                    case -1726003266:
                        if (typeName.equals(AppCrashRescueUtil.APPCRASH_RESCUE_JE_TYPE_NAME)) {
                            c = 3;
                            break;
                        }
                        break;
                    case -762107039:
                        if (typeName.equals(AppCrashRescueUtil.APPCRASH_RESCUE_NE_KEYWORD_NAME)) {
                            c = 4;
                            break;
                        }
                        break;
                    case -416590603:
                        if (typeName.equals(AppCrashRescueUtil.APPCRASH_RESCUE_UPDATE_TIMES_MAX_NAME)) {
                            c = 5;
                            break;
                        }
                        break;
                    case 77070059:
                        if (typeName.equals(AppCrashRescueUtil.APPCRASH_ODEX_DAMAGE_ERROR_TYPE_NAME)) {
                            c = 6;
                            break;
                        }
                        break;
                    case 1216382829:
                        if (typeName.equals(AppCrashRescueUtil.APPCRASH_RESCUE_SWITCH_NAME)) {
                            c = 0;
                            break;
                        }
                        break;
                    case 1576225894:
                        if (typeName.equals(AppCrashRescueUtil.APPCRASH_RESCUE_3PARTY_LIST_NAME)) {
                            c = 1;
                            break;
                        }
                        break;
                    case 1824011458:
                        if (typeName.equals(AppCrashRescueUtil.APPCRASH_RESCUE_NE_TYPE_NAME)) {
                            c = 2;
                            break;
                        }
                        break;
                }
                switch (c) {
                    case 0:
                        StringList unused = AppCrashRescueUtil.sRescueSwitchStringList = this.mConfigManager.getStringList(AppCrashRescueUtil.APPCRASH_RESCUE_CONFIG_FILE, typeName);
                        break;
                    case 1:
                        StringList unused2 = AppCrashRescueUtil.sRescue3partyAppStringList = this.mConfigManager.getStringList(AppCrashRescueUtil.APPCRASH_RESCUE_CONFIG_FILE, typeName);
                        break;
                    case 2:
                        StringList unused3 = AppCrashRescueUtil.sRescueNETypeStringList = this.mConfigManager.getStringList(AppCrashRescueUtil.APPCRASH_RESCUE_CONFIG_FILE, typeName);
                        break;
                    case 3:
                        StringList unused4 = AppCrashRescueUtil.sRescueJETypeStringList = this.mConfigManager.getStringList(AppCrashRescueUtil.APPCRASH_RESCUE_CONFIG_FILE, typeName);
                        break;
                    case 4:
                        StringList unused5 = AppCrashRescueUtil.sRescueNEKeywordStringList = this.mConfigManager.getStringList(AppCrashRescueUtil.APPCRASH_RESCUE_CONFIG_FILE, typeName);
                        break;
                    case 5:
                        StringList unused6 = AppCrashRescueUtil.sRescueUpdateTimeStringList = this.mConfigManager.getStringList(AppCrashRescueUtil.APPCRASH_RESCUE_CONFIG_FILE, typeName);
                        break;
                    case 6:
                        StringList unused7 = AppCrashRescueUtil.sDexFileDamageErrorTypeStringList = this.mConfigManager.getStringList(AppCrashRescueUtil.APPCRASH_RESCUE_CONFIG_FILE, typeName);
                        break;
                }
            }
        }

        public void registerAllObserver() {
            int listLength = AppCrashRescueUtil.sRescueTypeNameList.size();
            for (int num = 0; num < listLength; num++) {
                String typeName = (String) AppCrashRescueUtil.sRescueTypeNameList.get(num);
                VSlog.i(AppCrashRescueUtil.CRASH_TAG, "registerAllObserver: typeName " + typeName);
                char c = 65535;
                switch (typeName.hashCode()) {
                    case -1726003266:
                        if (typeName.equals(AppCrashRescueUtil.APPCRASH_RESCUE_JE_TYPE_NAME)) {
                            c = 3;
                            break;
                        }
                        break;
                    case -762107039:
                        if (typeName.equals(AppCrashRescueUtil.APPCRASH_RESCUE_NE_KEYWORD_NAME)) {
                            c = 4;
                            break;
                        }
                        break;
                    case -416590603:
                        if (typeName.equals(AppCrashRescueUtil.APPCRASH_RESCUE_UPDATE_TIMES_MAX_NAME)) {
                            c = 5;
                            break;
                        }
                        break;
                    case 77070059:
                        if (typeName.equals(AppCrashRescueUtil.APPCRASH_ODEX_DAMAGE_ERROR_TYPE_NAME)) {
                            c = 6;
                            break;
                        }
                        break;
                    case 1216382829:
                        if (typeName.equals(AppCrashRescueUtil.APPCRASH_RESCUE_SWITCH_NAME)) {
                            c = 0;
                            break;
                        }
                        break;
                    case 1576225894:
                        if (typeName.equals(AppCrashRescueUtil.APPCRASH_RESCUE_3PARTY_LIST_NAME)) {
                            c = 1;
                            break;
                        }
                        break;
                    case 1824011458:
                        if (typeName.equals(AppCrashRescueUtil.APPCRASH_RESCUE_NE_TYPE_NAME)) {
                            c = 2;
                            break;
                        }
                        break;
                }
                switch (c) {
                    case 0:
                        this.mConfigManager.registerObserver(AppCrashRescueUtil.sRescueSwitchStringList, AppCrashRescueUtil.sConfigurationSwitchObserver);
                        break;
                    case 1:
                        this.mConfigManager.registerObserver(AppCrashRescueUtil.sRescue3partyAppStringList, AppCrashRescueUtil.sConfiguration3partyListObserver);
                        break;
                    case 2:
                        this.mConfigManager.registerObserver(AppCrashRescueUtil.sRescueNETypeStringList, AppCrashRescueUtil.sConfigurationNETypeListObserver);
                        break;
                    case 3:
                        this.mConfigManager.registerObserver(AppCrashRescueUtil.sRescueJETypeStringList, AppCrashRescueUtil.sConfigurationJETypeListObserver);
                        break;
                    case 4:
                        this.mConfigManager.registerObserver(AppCrashRescueUtil.sRescueNEKeywordStringList, AppCrashRescueUtil.sConfigurationNEKeywordListObserver);
                        break;
                    case 5:
                        this.mConfigManager.registerObserver(AppCrashRescueUtil.sRescueUpdateTimeStringList, AppCrashRescueUtil.sConfigurationUpdateTimesObserver);
                        break;
                    case 6:
                        this.mConfigManager.registerObserver(AppCrashRescueUtil.sDexFileDamageErrorTypeStringList, AppCrashRescueUtil.sConfigurationOdexErrTypeListObserver);
                        break;
                }
            }
        }

        public void updateRescueConfigFileList(String typeName) {
            char c = 65535;
            try {
                boolean z = true;
                switch (typeName.hashCode()) {
                    case -1726003266:
                        if (typeName.equals(AppCrashRescueUtil.APPCRASH_RESCUE_JE_TYPE_NAME)) {
                            c = 3;
                            break;
                        }
                        break;
                    case -762107039:
                        if (typeName.equals(AppCrashRescueUtil.APPCRASH_RESCUE_NE_KEYWORD_NAME)) {
                            c = 4;
                            break;
                        }
                        break;
                    case -416590603:
                        if (typeName.equals(AppCrashRescueUtil.APPCRASH_RESCUE_UPDATE_TIMES_MAX_NAME)) {
                            c = 5;
                            break;
                        }
                        break;
                    case 77070059:
                        if (typeName.equals(AppCrashRescueUtil.APPCRASH_ODEX_DAMAGE_ERROR_TYPE_NAME)) {
                            c = 6;
                            break;
                        }
                        break;
                    case 1216382829:
                        if (typeName.equals(AppCrashRescueUtil.APPCRASH_RESCUE_SWITCH_NAME)) {
                            c = 0;
                            break;
                        }
                        break;
                    case 1576225894:
                        if (typeName.equals(AppCrashRescueUtil.APPCRASH_RESCUE_3PARTY_LIST_NAME)) {
                            c = 1;
                            break;
                        }
                        break;
                    case 1824011458:
                        if (typeName.equals(AppCrashRescueUtil.APPCRASH_RESCUE_NE_TYPE_NAME)) {
                            c = 2;
                            break;
                        }
                        break;
                }
                switch (c) {
                    case 0:
                        if (AppCrashRescueUtil.sRescueSwitchList != null) {
                            StringList unused = AppCrashRescueUtil.sRescueSwitchStringList = this.mConfigManager.getStringList(AppCrashRescueUtil.APPCRASH_RESCUE_CONFIG_FILE, AppCrashRescueUtil.APPCRASH_RESCUE_SWITCH_NAME);
                            if (AppCrashRescueUtil.sRescueSwitchStringList != null) {
                                VSlog.i(AppCrashRescueUtil.CRASH_TAG, "sRescueSwitchList size: " + AppCrashRescueUtil.sRescueSwitchStringList.getValues().size());
                                List unused2 = AppCrashRescueUtil.sRescueSwitchList = AppCrashRescueUtil.sRescueSwitchStringList.getValues().size() == 1 ? AppCrashRescueUtil.sRescueSwitchStringList.getValues() : AppCrashRescueUtil.sRescueSwitchList;
                            }
                            if (!((String) AppCrashRescueUtil.sRescueSwitchList.get(0)).equals("open")) {
                                z = false;
                            }
                            AppCrashRescueUtil.sRescueSwitch = z;
                            VSlog.i(AppCrashRescueUtil.CRASH_TAG, "sRescueSwitch status: " + AppCrashRescueUtil.sRescueSwitch);
                            return;
                        }
                        return;
                    case 1:
                        if (AppCrashRescueUtil.sRescue3partyAppStringList != null) {
                            StringList unused3 = AppCrashRescueUtil.sRescue3partyAppStringList = this.mConfigManager.getStringList(AppCrashRescueUtil.APPCRASH_RESCUE_CONFIG_FILE, typeName);
                            VSlog.i(AppCrashRescueUtil.CRASH_TAG, "sRescue3partyAppStringList size: " + AppCrashRescueUtil.sRescue3partyAppStringList.getValues().size());
                            List unused4 = AppCrashRescueUtil.sRescue3partyAppList = AppCrashRescueUtil.sRescue3partyAppStringList.getValues().size() > 0 ? AppCrashRescueUtil.sRescue3partyAppStringList.getValues() : AppCrashRescueUtil.sRescue3partyAppList;
                        }
                        if (AppCrashRescueUtil.sRescue3partyAppList != null) {
                            for (String values : AppCrashRescueUtil.sRescue3partyAppList) {
                                VSlog.i(AppCrashRescueUtil.CRASH_TAG, "allow " + typeName + ": " + values);
                            }
                            return;
                        }
                        VSlog.i(AppCrashRescueUtil.CRASH_TAG, "allow " + typeName + ": null.");
                        return;
                    case 2:
                        if (AppCrashRescueUtil.sRescueNETypeStringList != null) {
                            StringList unused5 = AppCrashRescueUtil.sRescueNETypeStringList = this.mConfigManager.getStringList(AppCrashRescueUtil.APPCRASH_RESCUE_CONFIG_FILE, typeName);
                            VSlog.i(AppCrashRescueUtil.CRASH_TAG, "sRescueNETypeStringList size: " + AppCrashRescueUtil.sRescueNETypeStringList.getValues().size());
                            List unused6 = AppCrashRescueUtil.sRescueNETypeList = AppCrashRescueUtil.sRescueNETypeStringList.getValues().size() > 0 ? AppCrashRescueUtil.sRescueNETypeStringList.getValues() : AppCrashRescueUtil.sRescueNETypeList;
                        }
                        if (AppCrashRescueUtil.sRescueNETypeList != null) {
                            for (String values2 : AppCrashRescueUtil.sRescueNETypeList) {
                                VSlog.i(AppCrashRescueUtil.CRASH_TAG, "allow " + typeName + ": " + values2);
                            }
                            return;
                        }
                        VSlog.i(AppCrashRescueUtil.CRASH_TAG, "allow " + typeName + ": null.");
                        return;
                    case 3:
                        if (AppCrashRescueUtil.sRescueJETypeStringList != null) {
                            StringList unused7 = AppCrashRescueUtil.sRescueJETypeStringList = this.mConfigManager.getStringList(AppCrashRescueUtil.APPCRASH_RESCUE_CONFIG_FILE, typeName);
                            VSlog.i(AppCrashRescueUtil.CRASH_TAG, "sRescueJETypeStringList size: " + AppCrashRescueUtil.sRescueJETypeStringList.getValues().size());
                            List unused8 = AppCrashRescueUtil.sRescueJETypeList = AppCrashRescueUtil.sRescueJETypeStringList.getValues().size() > 0 ? AppCrashRescueUtil.sRescueJETypeStringList.getValues() : AppCrashRescueUtil.sRescueJETypeList;
                        }
                        if (AppCrashRescueUtil.sRescueJETypeList != null) {
                            for (String values3 : AppCrashRescueUtil.sRescueJETypeList) {
                                VSlog.i(AppCrashRescueUtil.CRASH_TAG, "allow " + typeName + ": " + values3);
                            }
                            return;
                        }
                        VSlog.i(AppCrashRescueUtil.CRASH_TAG, "allow " + typeName + ": null.");
                        return;
                    case 4:
                        if (AppCrashRescueUtil.sRescueNEKeywordStringList != null) {
                            StringList unused9 = AppCrashRescueUtil.sRescueNEKeywordStringList = this.mConfigManager.getStringList(AppCrashRescueUtil.APPCRASH_RESCUE_CONFIG_FILE, typeName);
                            VSlog.i(AppCrashRescueUtil.CRASH_TAG, "sRescueNEKeywordStringList size: " + AppCrashRescueUtil.sRescueNEKeywordStringList.getValues().size());
                            List unused10 = AppCrashRescueUtil.sRescueNEKeywordList = AppCrashRescueUtil.sRescueNEKeywordStringList.getValues().size() > 0 ? AppCrashRescueUtil.sRescueNEKeywordStringList.getValues() : AppCrashRescueUtil.sRescueNEKeywordList;
                        }
                        if (AppCrashRescueUtil.sRescueNEKeywordList != null) {
                            for (String values4 : AppCrashRescueUtil.sRescueNEKeywordList) {
                                VSlog.i(AppCrashRescueUtil.CRASH_TAG, "allow " + typeName + ": " + values4);
                            }
                            return;
                        }
                        VSlog.i(AppCrashRescueUtil.CRASH_TAG, "allow " + typeName + ": null.");
                        return;
                    case 5:
                        if (AppCrashRescueUtil.sRescueUpdateTimesMaxList != null) {
                            StringList unused11 = AppCrashRescueUtil.sRescueUpdateTimeStringList = this.mConfigManager.getStringList(AppCrashRescueUtil.APPCRASH_RESCUE_CONFIG_FILE, AppCrashRescueUtil.APPCRASH_RESCUE_UPDATE_TIMES_MAX_NAME);
                            if (AppCrashRescueUtil.sRescueUpdateTimeStringList != null) {
                                VSlog.i(AppCrashRescueUtil.CRASH_TAG, "sRescueUpdateTimeStringList size: " + AppCrashRescueUtil.sRescueUpdateTimeStringList.getValues().size());
                                List unused12 = AppCrashRescueUtil.sRescueUpdateTimesMaxList = AppCrashRescueUtil.sRescueUpdateTimeStringList.getValues().size() == 1 ? AppCrashRescueUtil.sRescueUpdateTimeStringList.getValues() : AppCrashRescueUtil.sRescueUpdateTimesMaxList;
                            }
                            String tempValue = (String) AppCrashRescueUtil.sRescueUpdateTimesMaxList.get(0);
                            int unused13 = AppCrashRescueUtil.sUpdateTimesMax = Integer.parseInt(tempValue);
                            VSlog.i(AppCrashRescueUtil.CRASH_TAG, "sUpdateTimesMax value: " + AppCrashRescueUtil.sUpdateTimesMax);
                            return;
                        }
                        return;
                    case 6:
                        if (AppCrashRescueUtil.sDexFileDamageErrorTypeStringList != null) {
                            StringList unused14 = AppCrashRescueUtil.sDexFileDamageErrorTypeStringList = this.mConfigManager.getStringList(AppCrashRescueUtil.APPCRASH_RESCUE_CONFIG_FILE, typeName);
                            VSlog.i(AppCrashRescueUtil.CRASH_TAG, "sDexFileDamageErrorTypeStringList size: " + AppCrashRescueUtil.sDexFileDamageErrorTypeStringList.getValues().size());
                            List unused15 = AppCrashRescueUtil.sDexFileDamageErrorTypeList = AppCrashRescueUtil.sDexFileDamageErrorTypeStringList.getValues().size() > 0 ? AppCrashRescueUtil.sDexFileDamageErrorTypeStringList.getValues() : AppCrashRescueUtil.sDexFileDamageErrorTypeList;
                        }
                        if (AppCrashRescueUtil.sDexFileDamageErrorTypeList != null) {
                            for (String values5 : AppCrashRescueUtil.sDexFileDamageErrorTypeList) {
                                VSlog.i(AppCrashRescueUtil.CRASH_TAG, "allow " + typeName + ": " + values5);
                            }
                            return;
                        }
                        VSlog.i(AppCrashRescueUtil.CRASH_TAG, "allow " + typeName + ": null.");
                        return;
                    default:
                        return;
                }
            } catch (Exception e) {
                VSlog.e(AppCrashRescueUtil.CRASH_TAG, "catch ex in updateRescueFile " + typeName, e.fillInStackTrace());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class CrashAppInfo {
        String apkAbsolutePath;
        int appCrashCount;
        long appcrashTimeMills;
        int currentRescueStatus;
        DatalostCollectInfo datalostInfo;
        String downloadApkUri;
        long downloadId;
        String exceptionMsg;
        String exceptionStacktrace;
        String exceptionType;
        boolean isSystemApp;
        String packageName;
        ProcessRecord processRecord;
        int updateAppTimes;

        CrashAppInfo() {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class DatalostCollectInfo {
        String currentOdexDamageType;
        boolean reportAlready;

        DatalostCollectInfo() {
        }

        public static String getConciseStackTrace(String originStacktrace) {
            String result = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            if (TextUtils.isEmpty(originStacktrace)) {
                return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            }
            try {
                String[] strTemp1 = originStacktrace.split("backtrace:");
                String[] strTemp2 = strTemp1[1].split("stack:");
                result = strTemp2[0].trim();
            } catch (Exception ex) {
                VSlog.e(AppCrashRescueUtil.CRASH_TAG, "catch ex in DatalostCollectInfo.getConciseStackTrace ", ex.fillInStackTrace());
            }
            VSlog.i(AppCrashRescueUtil.CRASH_TAG, "getConciseStackTrace: " + result);
            return result;
        }

        public static String getCurrentOdexDamageType(String originStacktrace) {
            if (AppCrashRescueUtil.sDexFileDamageErrorTypeList != null) {
                for (String value : AppCrashRescueUtil.sDexFileDamageErrorTypeList) {
                    if (originStacktrace.contains(value)) {
                        return value;
                    }
                }
                return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            }
            return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        }
    }

    private void createCrashAppInfo(ProcessRecord app) {
        CrashAppInfo appInfo = new CrashAppInfo();
        appInfo.processRecord = app;
        appInfo.packageName = app.info.packageName;
        appInfo.isSystemApp = isSystemAppInstall(app);
        crashAppInfoMap.put(appInfo.packageName, appInfo);
        VSlog.i(CRASH_TAG, "New package: " + app.info.packageName + " appCrashCount: 1, updateAppTimes: " + appInfo.updateAppTimes);
    }

    private void updateCrashAppInfo(ProcessRecord app) {
        String pkgName = app.info.packageName;
        CrashAppInfo currentCrashAppInfo = crashAppInfoMap.get(pkgName);
        currentCrashAppInfo.processRecord = app;
        currentCrashAppInfo.isSystemApp = isSystemAppInstall(app);
        crashAppInfoMap.put(pkgName, currentCrashAppInfo);
    }

    private boolean allowRescueCurApp(CrashAppInfo appinfo) {
        boolean allowRescue3PartyApp = false;
        List<String> list = sRescue3partyAppList;
        if (list != null && list.contains(appinfo.packageName)) {
            allowRescue3PartyApp = true;
        }
        return allowRescue3PartyApp || appinfo.isSystemApp;
    }

    private boolean allowRescueCurCrashType(String exType, String exMsg, String exStackTrace) {
        boolean result = false;
        if ("Native crash".equals(exType) && sDexFileDamageErrorTypeList != null && exStackTrace != null && allowRescueCurNEKeyword(exStackTrace)) {
            Iterator<String> it = sDexFileDamageErrorTypeList.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                String sigType = it.next();
                if (exStackTrace.contains(sigType)) {
                    result = true;
                    break;
                }
            }
        }
        List<String> list = sRescueJETypeList;
        if (list != null && list.contains(exType)) {
            return true;
        }
        return result;
    }

    private boolean allowRescueCurNEKeyword(String exStackTrace) {
        boolean result = false;
        List<String> list = sRescueNEKeywordList;
        if (list != null) {
            if (list.contains(ALLOW_ALL_NEKEYWORD_IDENTIFICATION)) {
                result = true;
            }
            for (String keyword : sRescueNEKeywordList) {
                if (exStackTrace.contains(keyword)) {
                    result = true;
                }
            }
        }
        return result;
    }

    private void rescanAbiCheck(CrashAppInfo appinfo, ProcessRecord app) {
        if (isNeedRescan(app)) {
            VSlog.w(CRASH_TAG, "rescanAbi for: " + appinfo.packageName);
            rescanAbi(appinfo, app);
        }
    }

    private void rescanAbi(CrashAppInfo appinfo, ProcessRecord app) {
        String sourceDir = app.info.sourceDir;
        String apkPath = sourceDir.substring(0, sourceDir.lastIndexOf("/"));
        if (apkPath == null) {
            return;
        }
        File installPkgFile = new File(apkPath);
        if (!installPkgFile.exists()) {
            return;
        }
        AndroidPackage newPkg = this.mService.getPackageManagerInternalLocked().scanPackageAbi(installPkgFile, app.info, 0L, UserHandle.SYSTEM);
        if (newPkg != null) {
            VSlog.i(CRASH_TAG, "rescanAbi successful!");
            reportBigData(16, appinfo, 1);
            return;
        }
        VSlog.e(CRASH_TAG, "rescanAbi failed.");
        reportBigData(17, appinfo, 0);
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r15v1 */
    /* JADX WARN: Type inference failed for: r15v10 */
    /* JADX WARN: Type inference failed for: r15v12 */
    /* JADX WARN: Type inference failed for: r15v14 */
    /* JADX WARN: Type inference failed for: r15v16 */
    /* JADX WARN: Type inference failed for: r15v18 */
    /* JADX WARN: Type inference failed for: r15v20 */
    /* JADX WARN: Type inference failed for: r15v21 */
    /* JADX WARN: Type inference failed for: r15v3 */
    /* JADX WARN: Type inference failed for: r15v4 */
    /* JADX WARN: Type inference failed for: r15v6 */
    /* JADX WARN: Type inference failed for: r15v8 */
    private void reportBigData(int currentStatus, CrashAppInfo appinfo, int exeResult) {
        String pkgName = appinfo.packageName;
        String isSystemApp = String.valueOf(appinfo.isSystemApp);
        String exType = appinfo.exceptionType;
        String exMsg = appinfo.exceptionMsg;
        String excResult = String.valueOf(exeResult);
        String updateTimes = String.valueOf(appinfo.updateAppTimes);
        String crashCount = String.valueOf(appinfo.appCrashCount);
        HashMap hashMap = new HashMap();
        hashMap.put("pkgName", pkgName);
        hashMap.put("isSystemApp", isSystemApp);
        hashMap.put("exType", exType);
        hashMap.put("exMsg", exMsg);
        hashMap.put("updateTimes", updateTimes);
        hashMap.put("crashCount", crashCount);
        try {
            try {
                if (currentStatus != 2) {
                    if (currentStatus != 3) {
                        if (currentStatus != 6) {
                            if (currentStatus != 7) {
                                if (currentStatus != 10) {
                                    if (currentStatus != 11) {
                                        switch (currentStatus) {
                                            case 14:
                                                hashMap.put("exeResult", excResult);
                                                hashMap = 0;
                                                EventTransfer.getInstance().singleEvent("F379", "F379|10003", System.currentTimeMillis(), 0L, hashMap);
                                                break;
                                            case 15:
                                                hashMap.put("exeResult", excResult);
                                                hashMap = 0;
                                                EventTransfer.getInstance().singleEvent("F379", "F379|10003", System.currentTimeMillis(), 0L, hashMap);
                                                break;
                                            case 16:
                                                hashMap.put("exeResult", "true");
                                                hashMap = 0;
                                                EventTransfer.getInstance().singleEvent("F379", "F379|10001", System.currentTimeMillis(), 0L, hashMap);
                                                break;
                                            case 17:
                                                hashMap.put("exeResult", "false");
                                                hashMap = 0;
                                                EventTransfer.getInstance().singleEvent("F379", "F379|10001", System.currentTimeMillis(), 0L, hashMap);
                                                break;
                                            default:
                                                return;
                                        }
                                    } else {
                                        hashMap.put("exeResult", excResult);
                                        hashMap = 0;
                                        EventTransfer.getInstance().singleEvent("F379", "F379|10004", System.currentTimeMillis(), 0L, hashMap);
                                    }
                                } else {
                                    hashMap.put("exeResult", excResult);
                                    hashMap = 0;
                                    EventTransfer.getInstance().singleEvent("F379", "F379|10004", System.currentTimeMillis(), 0L, hashMap);
                                }
                            } else {
                                hashMap.put("exeResult", "download_fail");
                                hashMap = 0;
                                EventTransfer.getInstance().singleEvent("F379", "F379|10004", System.currentTimeMillis(), 0L, hashMap);
                            }
                        } else {
                            hashMap.put("exeResult", "download_done");
                            hashMap = 0;
                            EventTransfer.getInstance().singleEvent("F379", "F379|10004", System.currentTimeMillis(), 0L, hashMap);
                        }
                    } else {
                        hashMap.put("exeResult", "false");
                        hashMap = 0;
                        EventTransfer.getInstance().singleEvent("F379", "F379|10002", System.currentTimeMillis(), 0L, hashMap);
                    }
                } else {
                    hashMap.put("exeResult", "true");
                    hashMap = 0;
                    EventTransfer.getInstance().singleEvent("F379", "F379|10002", System.currentTimeMillis(), 0L, hashMap);
                }
            } catch (Exception e) {
                e = e;
                VSlog.e(CRASH_TAG, "catch ex in reportBigData", e.fillInStackTrace());
            }
        } catch (Exception e2) {
            e = e2;
            VSlog.e(CRASH_TAG, "catch ex in reportBigData", e.fillInStackTrace());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class AppCrashBroadcastReceiver extends BroadcastReceiver {
        private AppCrashBroadcastReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            VSlog.i(AppCrashRescueUtil.CRASH_TAG, "onReceive action is " + action);
            if ("android.vivo.bbklog.action.CHANGED".equals(action)) {
                boolean status = "on".equals(intent.getStringExtra("adblog_status"));
                boolean unused = AppCrashRescueUtil.LOGDEBUG = status;
                VSlog.i(AppCrashRescueUtil.CRASH_TAG, "BBKLOG status: " + AppCrashRescueUtil.LOGDEBUG);
            } else if ("android.intent.action.DOWNLOAD_COMPLETE".equals(action)) {
                final long id = intent.getExtras().getLong("extra_download_id");
                VSlog.i(AppCrashRescueUtil.CRASH_TAG, "Received download id: " + id);
                AppCrashRescueUtil.this.mAppErrorHandler.post(new Runnable() { // from class: com.android.server.am.AppCrashRescueUtil.AppCrashBroadcastReceiver.1
                    @Override // java.lang.Runnable
                    public void run() {
                        AppCrashRescueUtil.this.downloadApkStatusCheck(id, true);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void downloadApkStatusCheck(long downloadId, boolean needInstall) {
        DownloadManager downloadManager;
        ArrayList<CrashAppInfo> valuesList = new ArrayList<>(crashAppInfoMap.values());
        if (sUndeletedFileDownloadid.contains(Long.valueOf(downloadId)) && (downloadManager = this.mDownloadManager) != null) {
            downloadManager.remove(downloadId);
            VSlog.i(CRASH_TAG, "download id: " + downloadId + " was added before reboot, remove it now.");
            return;
        }
        Iterator<CrashAppInfo> it = valuesList.iterator();
        while (it.hasNext()) {
            CrashAppInfo appinfo = it.next();
            if (downloadId == appinfo.downloadId) {
                Cursor cursor = this.mDownloadManager.query(new DownloadManager.Query().setFilterById(downloadId));
                try {
                    try {
                        if (cursor.moveToFirst()) {
                            int status = cursor.getInt(cursor.getColumnIndex("status"));
                            VSlog.i(CRASH_TAG, "download apk " + appinfo.packageName + " status: " + status);
                            if (status == 2) {
                                VSlog.i(CRASH_TAG, "downloding " + appinfo.packageName + " ...");
                            } else if (status == 4) {
                                VSlog.i(CRASH_TAG, "downloding " + appinfo.packageName + " pause.");
                            } else if (status == 8) {
                                appinfo.currentRescueStatus = 6;
                                reportBigData(6, appinfo, 1);
                                if (needInstall) {
                                    installPkgByStore(appinfo, true, null);
                                }
                            } else if (status == 16) {
                                VSlog.w(CRASH_TAG, "downloding " + appinfo.packageName + " failed!");
                                appinfo.currentRescueStatus = 7;
                                reportBigData(7, appinfo, 0);
                            }
                        } else {
                            VSlog.e(CRASH_TAG, "No status found for appcrash download!");
                        }
                    } catch (Exception e) {
                        VSlog.e(CRASH_TAG, "catch ex in downloadApkStatusCheck", e.fillInStackTrace());
                        if (cursor != null) {
                        }
                    }
                    if (cursor != null) {
                        cursor.close();
                    }
                } catch (Throwable th) {
                    if (cursor != null) {
                        cursor.close();
                    }
                    throw th;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerAppCrashReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.vivo.bbklog.action.CHANGED");
        filter.addAction("android.intent.action.DOWNLOAD_COMPLETE");
        AppCrashBroadcastReceiver appCrashBroadcastReceiver = new AppCrashBroadcastReceiver();
        this.mAppCrashBroadcastReceiver = appCrashBroadcastReceiver;
        this.mContext.registerReceiver(appCrashBroadcastReceiver, filter);
    }
}