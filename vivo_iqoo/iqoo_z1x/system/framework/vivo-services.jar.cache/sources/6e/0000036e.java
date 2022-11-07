package com.android.server.pm;

import android.app.ActivityManager;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.ParceledListSlice;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.FtBuild;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Slog;
import com.android.server.am.firewall.VivoFirewall;
import com.vivo.face.common.data.Constants;
import com.vivo.services.superresolution.Constant;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public final class VivoPKMSDatabaseUtils {
    private static final String AUTHORITY = "com.iqoo.secure.provider.secureprovider";
    private static final String BuiltInThirdPartDirPath = "/apps";
    private static final String BuiltInThirdPartVivoDirPath = "/system/vivo-apps";
    public static final int GET_INSTALL_APP_LIST_ALLOW = 0;
    public static final int GET_INSTALL_APP_LIST_ALWAY_ASK = 2;
    public static final int GET_INSTALL_APP_LIST_FORBID = 1;
    private static final int INSTALL_APP_CONFIG_DB_CHANGE_QUERY_TOKEN = 102;
    private static final int INSTALL_APP_CONFIG_INSERT_ALL_TOKEN = 201;
    private static final int INSTALL_APP_CONFIG_UPDATE_IS_FORCE_TOKEN = 200;
    private static final int INSTALL_APP_DEFAULT_CONFIG_QUERY_TOKEN = 100;
    public static final int INTERCEPT_MODE_ALLOW = 1;
    public static final int INTERCEPT_MODE_ASKING = 3;
    public static final int INTERCEPT_MODE_FORBID = 2;
    public static final int INTERCEPT_MODE_NOT_INTERCEPT = 0;
    private static final String LOCK_SYSTEMHOME_ENABLED = "desktop_usage_rights_enabled";
    private static final int MSG_CHECK_TO_QUERY_APP_CONFIG = 1001;
    private static final int MSG_DELETE_FORBID_SHOW_TOAST_FROM_CACHE_LIST = 1003;
    private static final int MSG_DELETE_INSTALL_APP_CONFIG_FROM_LOCAL = 1005;
    private static final int MSG_INIT_DATA = 1006;
    private static final int MSG_INSERT_INSTALL_APP_CONFIG_COMPLETE = 1004;
    private static final int MSG_QUERY_INSTALL_APP_CONFIG_COMPLETE = 1000;
    private static final int MSG_UPDATE_INSTALL_APP_CONFIG_TO_DB = 1007;
    private static final String NEW_AUTHORITY = "com.vivo.permissionmanager.provider.permission";
    public static final int SECURITY_LEVEL_HIGH = 0;
    public static final int SECURITY_LEVEL_LOW = 2;
    public static final int SECURITY_LEVEL_MIDDLE = 1;
    private static final String TAG = "VivoPKMSDatabaseUtils";
    private static final String TAG_HOME_RESTRICTION = "HomeRestriction";
    private static final String THREE_PART_PKG_FORBID_SHOW_TOAST_FILE = "/data/bbkcore/three_part_pkg_forbid_show_toast.xml";
    private boolean isDeviceBootComplete;
    private ActivityManager mActivityManager;
    private Context mContext;
    private HomeRestrictionObserver mHomeRestrictionOb;
    private Handler mMainHandler;
    private HandlerThread mPkmsDBHThread;
    public PkmsDBHandler mPkmsDBHandler;
    private ContentResolver mResolver;
    private static boolean DEBUG = PackageManagerService.DEBUG;
    protected static boolean DEBUG_FOR_FB_APL = PackageManagerService.DEBUG_FOR_FB_APL;
    private static String Algorithm = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    private static String mPasswordInstalled = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    private static final String INSTALL_APPS_ID = "_id";
    private static final String PKGNAME = "pkgname";
    private static final String ISFORCE = "is_force";
    private static final String LAST_SERVER_STATUS = "last_server_status";
    private static final String STATUS = "status";
    private static final String[] QUERY_SELECTION_ARGS = {INSTALL_APPS_ID, PKGNAME, ISFORCE, LAST_SERVER_STATUS, STATUS};
    static final String[] BASE_SYS_APK_ARRS = {"com.android.mms", "com.android.dialer", "com.android.contacts", "com.android.camera", "com.android.VideoPlayer", "com.android.filemanager", "com.bbk.calendar", "com.google.android.webview"};
    static final String[] BASE_THR_APK_ARRS = {"com.tencent.mobileqq", Constant.APP_WEIXIN, "com.eg.android.AlipayGphone", Constant.APP_WEIBO, "com.facebook.katana", "com.twitter.android", "com.alipay.android.app", "com.tencent.WBlog", "jp.naver.line.android", "com.qzone", "com.autonavi.minimap", "com.baidu.BaiduMap", "com.xiaomi.gamecenter.sdk.service", "com.google.android.gm", "com.google.android.apps.maps", "com.sdu.didi.psnger", "com.didi.echo", "com.viber.voip", "com.whatsapp"};
    static final String[] FILTER_LIST = {"com.vivo.easyshare", "com.vivo.PCTools", "com.bbk.iqoo.feedback", "com.vivo.childrenmode", "com.android.bbk.lockscreen3", "com.android.cts.applicationvisibility"};
    private static boolean mPermissionManagerAppExist = false;
    private static boolean mRomVersionIsLower3_0 = false;
    private static PackageManagerService mPkmService = null;
    private Uri CONTENT_URI = null;
    private QueryHandler mQueryHandler = null;
    private DataBaseObserver mObserver = null;
    private final HashMap<String, VivoADBInstallWarningDialog> mWarningDlgMap = new HashMap<>();
    private Object mObjectLock = new Object();
    private HashMap<String, Integer> mResult = new HashMap<>();
    protected List<String> mForbidShowToastAppListCache = new ArrayList();
    private ArrayList<InstallAppConfig> mInstallAppConfigList = new ArrayList<>();
    HashMap<String, InstallAppConfig> mLocalInsertAppConfigMap = new HashMap<>();
    ArrayList<String> mBaseThrApkList = new ArrayList<>();
    ArrayList<String> mBaseSysApkList = new ArrayList<>();
    Object mBaseApkLock = new Object();
    private HashMap<String, String> mBuiltInThirdPartMap = new HashMap<>();
    private boolean mHomeRestrictionEnabled = true;
    private BroadcastReceiver mForbidThreePartPkgBR = new BroadcastReceiver() { // from class: com.android.server.pm.VivoPKMSDatabaseUtils.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (VivoPKMSDatabaseUtils.DEBUG_FOR_FB_APL) {
                VSlog.d(VivoPKMSDatabaseUtils.TAG, "device boot complete. intent:" + intent + " " + VivoPKMSDatabaseUtils.this.CONTENT_URI);
            }
            VivoPKMSDatabaseUtils.this.mQueryHandler.startQuery(100, null, VivoPKMSDatabaseUtils.this.CONTENT_URI, VivoPKMSDatabaseUtils.QUERY_SELECTION_ARGS, null, null, null);
            VivoPKMSDatabaseUtils.this.isDeviceBootComplete = true;
        }
    };

    /* JADX INFO: Access modifiers changed from: package-private */
    public VivoPKMSDatabaseUtils(PackageManagerService pkms, Context context) {
        this.mContext = null;
        this.mResolver = null;
        mPkmService = pkms;
        this.mContext = context;
        this.mResolver = context.getContentResolver();
        this.mActivityManager = (ActivityManager) this.mContext.getSystemService(VivoFirewall.TYPE_ACTIVITY);
        this.mMainHandler = new Handler(Looper.getMainLooper());
        SystemProperties.set("persist.sys.name.ec.enable", "2");
        HandlerThread handlerThread = new HandlerThread("PkmsDBHandlerThread");
        this.mPkmsDBHThread = handlerThread;
        handlerThread.start();
        this.mPkmsDBHandler = new PkmsDBHandler(this.mPkmsDBHThread.getLooper());
    }

    public void systemReady() {
        queryAndObserveDatabase();
        startHomeRestrictionOb();
    }

    void queryAndObserveDatabase() {
        initData(this.mContext);
        if (this.mQueryHandler == null) {
            this.mQueryHandler = new QueryHandler(this.mResolver);
        }
        if (this.mObserver == null) {
            this.mObserver = new DataBaseObserver(null);
        }
        this.mObserver.observe();
        this.mQueryHandler.startQuery(100, null, this.CONTENT_URI, QUERY_SELECTION_ARGS, null, null, null);
        if (DEBUG) {
            VSlog.d(TAG, "queryAndObserveDatabase  begin.");
        }
        IntentFilter intentFilter = new IntentFilter("android.intent.action.BOOT_COMPLETED");
        this.mContext.registerReceiver(this.mForbidThreePartPkgBR, intentFilter);
        scheduleInitBuildInAppData();
    }

    HashMap<String, Integer> getResult() {
        return this.mResult;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        /* JADX WARN: Code restructure failed: missing block: B:21:0x00cc, code lost:
            if (r21 == null) goto L18;
         */
        /* JADX WARN: Code restructure failed: missing block: B:23:0x00cf, code lost:
            r0 = r18.this$0.mPkmsDBHandler.obtainMessage(1000);
            r0.obj = r0;
            r18.this$0.mPkmsDBHandler.sendMessage(r0);
         */
        /* JADX WARN: Code restructure failed: missing block: B:31:?, code lost:
            return;
         */
        @Override // android.content.AsyncQueryHandler
        /*
            Code decompiled incorrectly, please refer to instructions dump.
            To view partially-correct code enable 'Show inconsistent code' option in preferences
        */
        protected void onQueryComplete(int r19, java.lang.Object r20, android.database.Cursor r21) {
            /*
                Method dump skipped, instructions count: 234
                To view this dump change 'Code comments level' option to 'DEBUG'
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.VivoPKMSDatabaseUtils.QueryHandler.onQueryComplete(int, java.lang.Object, android.database.Cursor):void");
        }

        @Override // android.content.AsyncQueryHandler
        protected void onInsertComplete(int token, Object cookie, Uri uri) {
            if (VivoPKMSDatabaseUtils.DEBUG_FOR_FB_APL) {
                Slog.d(VivoPKMSDatabaseUtils.TAG, "onInsertComplete " + token + " uri:" + uri);
            }
            VivoPKMSDatabaseUtils.this.mPkmsDBHandler.removeMessages(1001);
        }

        @Override // android.content.AsyncQueryHandler
        protected void onUpdateComplete(int token, Object cookie, int result) {
            if (VivoPKMSDatabaseUtils.DEBUG_FOR_FB_APL) {
                Slog.d(VivoPKMSDatabaseUtils.TAG, "onUpdateComplete  token:" + token + " result:" + result);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class DataBaseObserver extends ContentObserver {
        public DataBaseObserver(Handler handler) {
            super(handler);
        }

        public void observe() {
            VivoPKMSDatabaseUtils.this.mResolver.registerContentObserver(VivoPKMSDatabaseUtils.this.CONTENT_URI, false, this);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if (VivoPKMSDatabaseUtils.DEBUG_FOR_FB_APL) {
                Slog.d(VivoPKMSDatabaseUtils.TAG, "db onchange selfChange:" + selfChange);
            }
            VivoPKMSDatabaseUtils.this.mQueryHandler.startQuery(102, null, VivoPKMSDatabaseUtils.this.CONTENT_URI, VivoPKMSDatabaseUtils.QUERY_SELECTION_ARGS, null, null, null);
        }
    }

    public boolean queryPackageIsForeGroundProcess(String packageName) {
        boolean result = false;
        if (this.mActivityManager == null) {
            Slog.w(TAG, "!am is null.");
            return false;
        }
        long origId = Binder.clearCallingIdentity();
        try {
            try {
                ComponentName cn = this.mActivityManager.getRunningTasks(1).get(0).topActivity;
                Binder.restoreCallingIdentity(origId);
                String currentPackageName = cn.getPackageName();
                if (!TextUtils.isEmpty(currentPackageName) && currentPackageName.equals(packageName)) {
                    result = true;
                }
                if (DEBUG_FOR_FB_APL) {
                    VSlog.d(TAG, " checkPKG:" + packageName + " is fore ground process  " + result);
                }
                return result;
            } catch (SecurityException e) {
                e.printStackTrace();
                Binder.restoreCallingIdentity(origId);
                return true;
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(origId);
            throw th;
        }
    }

    public boolean writeThreePartPackageToForbidShowToastFile(List<String> pkgList, boolean append) {
        if (DEBUG_FOR_FB_APL) {
            for (String pkgName : pkgList) {
                VSlog.d(TAG, "writeToFile pkgName:" + pkgName + " append:" + append);
            }
        }
        boolean result = false;
        File pkgFile = new File(THREE_PART_PKG_FORBID_SHOW_TOAST_FILE);
        if (!pkgFile.exists()) {
            try {
                pkgFile.createNewFile();
            } catch (IOException e) {
                VSlog.e(TAG, "create new file catche exception. " + e.toString());
            }
        }
        if (DEBUG_FOR_FB_APL) {
            VSlog.d(TAG, "writeThreePartPackageToForbidShowToastFile pkgFile:" + pkgFile + " exist:" + pkgFile.exists());
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
        return result;
    }

    public List<String> queryForbidShowToastPakcageList() {
        List<String> pkgList = new ArrayList<>();
        File pkgFile = new File(THREE_PART_PKG_FORBID_SHOW_TOAST_FILE);
        if (pkgFile.exists() && 0 != pkgFile.length()) {
            BufferedReader reader = null;
            try {
                try {
                    try {
                        reader = new BufferedReader(new FileReader(pkgFile));
                        while (true) {
                            String pkgName = reader.readLine();
                            if (pkgName == null) {
                                break;
                            } else if (pkgName != null && pkgName.length() != 0) {
                                pkgList.add(pkgName);
                            } else {
                                VSlog.d(TAG, "queryForbidShowToastPakcageList pkgName length is 0 pkgName:" + pkgName);
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
        }
        if (DEBUG_FOR_FB_APL) {
            Slog.d(TAG, " queryForbidShowToastPakcageList print all..");
            Iterator<String> it = pkgList.iterator();
            while (it.hasNext()) {
                VSlog.d(TAG, " pkgname:" + it.next());
            }
        }
        return pkgList;
    }

    public boolean deleteThreePartPackageFromForbidShowToastFile(String packageName) {
        File pkgFile = new File(THREE_PART_PKG_FORBID_SHOW_TOAST_FILE);
        if (pkgFile.exists() && pkgFile.length() > 0) {
            List<String> pkgList = queryForbidShowToastPakcageList();
            List<String> pkgListNew = new ArrayList<>();
            if (pkgList == null || pkgList.size() <= 0) {
                return false;
            }
            for (String pkgName : pkgList) {
                if (!packageName.equals(pkgName)) {
                    if (DEBUG_FOR_FB_APL) {
                        Slog.d(TAG, "add " + pkgName + " to list.");
                    }
                    pkgListNew.add(pkgName);
                }
            }
            boolean result = writeThreePartPackageToForbidShowToastFile(pkgListNew, false);
            return result;
        } else if (!DEBUG_FOR_FB_APL) {
            return false;
        } else {
            Slog.d(TAG, "deleteThreePartPackageFromForbidShowToastFile " + packageName + " pkg file not exist? " + pkgFile);
            return false;
        }
    }

    void deleteForbidedShowToastAppFromCacheListIfNeed(String packageName) {
        Message msg = this.mPkmsDBHandler.obtainMessage(1003);
        msg.obj = packageName;
        this.mPkmsDBHandler.sendMessage(msg);
    }

    boolean deleteForbideShowToastAppFromCacheList(String packageName) {
        boolean delResult = false;
        if (packageName != null) {
            synchronized (this.mObjectLock) {
                if (this.mForbidShowToastAppListCache.contains(packageName) && (delResult = deleteThreePartPackageFromForbidShowToastFile(packageName))) {
                    this.mForbidShowToastAppListCache.remove(packageName);
                }
            }
        }
        if (DEBUG_FOR_FB_APL) {
            Slog.d(TAG, packageName + " gone. so,remove it from 3partForbid map. delResult:" + delResult + "  " + this.mForbidShowToastAppListCache);
        }
        return delResult;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void convertPackageName(ArrayList<InstallAppConfig> appConfigList) {
        if (DEBUG_FOR_FB_APL) {
            Slog.d(TAG, "-convertPackageName  localList :" + this.mInstallAppConfigList + " newAppConfigList:" + appConfigList);
        }
        ArrayList<InstallAppConfig> listTemp = new ArrayList<>();
        Iterator<InstallAppConfig> it = appConfigList.iterator();
        while (it.hasNext()) {
            InstallAppConfig appConfig = it.next();
            if (DEBUG_FOR_FB_APL) {
                Slog.d(TAG, "convert package name:" + appConfig.packageName + " isForce:" + appConfig.isForce + "  status:" + appConfig.status);
            }
            String deenPackageName = null;
            try {
                deenPackageName = deencrpt(appConfig.packageName);
            } catch (Exception e) {
                Slog.e(TAG, "deecrpt catch excetpion " + e.toString());
            }
            if (DEBUG_FOR_FB_APL) {
                Slog.d(TAG, "after packagename:" + deenPackageName);
            }
            if (deenPackageName != null) {
                appConfig.packageName = deenPackageName;
            }
            listTemp.add(appConfig);
        }
        synchronized (this.mObjectLock) {
            if (DEBUG_FOR_FB_APL) {
                Slog.d(TAG, "convert end. oldSize:" + this.mInstallAppConfigList.size() + " newSize:" + listTemp.size());
                StringBuilder sb = new StringBuilder();
                sb.append("old config list :");
                sb.append(this.mInstallAppConfigList);
                Slog.d(TAG, sb.toString());
                Slog.d(TAG, "new config list :" + listTemp);
            }
            this.mInstallAppConfigList.clear();
            this.mInstallAppConfigList.addAll(listTemp);
            updateLocalAppConfigInLock();
            listTemp.clear();
        }
        if (DEBUG_FOR_FB_APL) {
            localPrint("after deencrpt ", this.mInstallAppConfigList);
        }
    }

    private void localPrint(String msg, ArrayList<InstallAppConfig> appConfigList) {
        ArrayList<InstallAppConfig> configTemp;
        new ArrayList();
        synchronized (this.mObjectLock) {
            configTemp = (ArrayList) appConfigList.clone();
        }
        if (configTemp != null) {
            Iterator<InstallAppConfig> it = configTemp.iterator();
            while (it.hasNext()) {
                InstallAppConfig temp = it.next();
                Slog.d(TAG, "print  " + temp);
            }
        }
    }

    private String encrpt(String resource) {
        if (resource == null || resource.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK)) {
            return null;
        }
        byte[] secretArr = encryptMode(resource.getBytes());
        byte[] secret = Base64.encode(secretArr, 0);
        String secretString = new String(secret);
        return secretString;
    }

    private byte[] encryptMode(byte[] src) {
        try {
            SecretKey deskey = new SecretKeySpec(build3Deskey(mPasswordInstalled), Algorithm);
            Cipher cipher = Cipher.getInstance(Algorithm);
            cipher.init(1, deskey);
            return cipher.doFinal(src);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String deencrpt(String encrptedStr) {
        if (encrptedStr == null || encrptedStr.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK)) {
            return null;
        }
        byte[] decryptFrom = Base64.decode(encrptedStr, 0);
        byte[] secreArr2 = decryptMode(decryptFrom);
        if (secreArr2 == null) {
            return null;
        }
        return new String(secreArr2);
    }

    private byte[] decryptMode(byte[] src) {
        try {
            SecretKey deskey = new SecretKeySpec(build3Deskey(mPasswordInstalled), Algorithm);
            Cipher cipher = Cipher.getInstance(Algorithm);
            cipher.init(2, deskey);
            return cipher.doFinal(src);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class PkmsDBHandler extends Handler {
        public PkmsDBHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            VSlog.d(VivoPKMSDatabaseUtils.TAG, "--handleMessage " + msg.what);
            switch (msg.what) {
                case 1000:
                    try {
                        VivoPKMSDatabaseUtils.this.convertPackageName((ArrayList) msg.obj);
                        return;
                    } catch (Exception e) {
                        VSlog.e(VivoPKMSDatabaseUtils.TAG, "convert package catch exception " + e.toString());
                        e.printStackTrace();
                        return;
                    }
                case 1001:
                    VivoPKMSDatabaseUtils.this.mQueryHandler.startQuery(100, null, VivoPKMSDatabaseUtils.this.CONTENT_URI, VivoPKMSDatabaseUtils.QUERY_SELECTION_ARGS, null, null, null);
                    return;
                case 1002:
                case 1004:
                default:
                    return;
                case 1003:
                    VivoPKMSDatabaseUtils.this.deleteForbideShowToastAppFromCacheList((String) msg.obj);
                    return;
                case 1005:
                    VivoPKMSDatabaseUtils.this.deleteAppConfigFromLocalCache((String) msg.obj);
                    return;
                case 1006:
                    VivoPKMSDatabaseUtils.this.handlerInitBuildInAppData();
                    return;
                case 1007:
                    InstallAppConfig appConfig = (InstallAppConfig) msg.obj;
                    ContentValues contentValue = new ContentValues();
                    contentValue.put(VivoPKMSDatabaseUtils.ISFORCE, Integer.valueOf(appConfig.isForce));
                    String selection = "_id=" + appConfig._id;
                    if (VivoPKMSDatabaseUtils.DEBUG_FOR_FB_APL) {
                        Slog.d(VivoPKMSDatabaseUtils.TAG, "will update " + appConfig + " to db.   " + selection);
                    }
                    VivoPKMSDatabaseUtils.this.mQueryHandler.startUpdate(200, null, VivoPKMSDatabaseUtils.this.CONTENT_URI, contentValue, selection, null);
                    return;
            }
        }
    }

    public ArrayList<InstallAppConfig> getLocalInstallAppConfigList() {
        ArrayList<InstallAppConfig> arrayList;
        synchronized (this.mObjectLock) {
            arrayList = this.mInstallAppConfigList;
        }
        return arrayList;
    }

    protected boolean updateLocalInstallAppConfigs(InstallAppConfig appConfig) {
        synchronized (this.mObjectLock) {
            Iterator<InstallAppConfig> it = this.mInstallAppConfigList.iterator();
            while (it.hasNext()) {
                InstallAppConfig appConfigTemp = it.next();
                if (appConfigTemp.packageName == appConfig.packageName) {
                    appConfigTemp.isForce = appConfig.isForce;
                    if (DEBUG_FOR_FB_APL) {
                        Slog.d(TAG, "Find, update local ,  " + appConfig);
                    }
                    return true;
                }
            }
            this.mInstallAppConfigList.add(appConfig);
            if (DEBUG_FOR_FB_APL) {
                Slog.d(TAG, "Not find, add call app list to local list. " + appConfig);
            }
            return false;
        }
    }

    private void initData(Context context) {
        if (isPermissionManagerAppExisted(context)) {
            mPermissionManagerAppExist = true;
            this.CONTENT_URI = Uri.parse("content://com.vivo.permissionmanager.provider.permission" + File.separator + "read_installed_apps");
        } else {
            this.CONTENT_URI = Uri.parse("content://com.iqoo.secure.provider.secureprovider/read_installed_apps");
        }
        mRomVersionIsLower3_0 = FtBuild.getRomVersion() < 3.0f;
        if (DEBUG_FOR_FB_APL) {
            VSlog.d(TAG, "initData CONTENT_URI:" + this.CONTENT_URI);
        }
        try {
            Algorithm = "AES";
            mPasswordInstalled = new String(Base64.decode(context.getString(51249215), 0));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isPermissionManagerAppExisted(Context context) {
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo applicationInfo = null;
        try {
            applicationInfo = packageManager.getApplicationInfo("com.vivo.permissionmanager", 0);
        } catch (PackageManager.NameNotFoundException e) {
            VSlog.w(TAG, "permissionmanager not found " + e);
        }
        if (DEBUG_FOR_FB_APL) {
            VSlog.d(TAG, " isPermissionManagerAppExisted " + applicationInfo);
        }
        return applicationInfo != null;
    }

    public static boolean isPermissionManagerAppSpliteFromIqoo() {
        return mPermissionManagerAppExist;
    }

    public static boolean isRomVersionIsLow_30() {
        return mRomVersionIsLower3_0;
    }

    public boolean isDeviceBootCompleted() {
        return this.isDeviceBootComplete;
    }

    public ForbidResult checkCallerPackageIsForbidThreePartApp(String msg) {
        ForbidResult forbidResult = new ForbidResult();
        int callingUid = Binder.getCallingUid();
        forbidResult.result = 1;
        forbidResult.securityLevel = 0;
        forbidResult.callingUid = callingUid;
        if (DEBUG_FOR_FB_APL) {
            VSlog.d(TAG, "Check begin:  callingUid -> " + callingUid + " -> " + msg);
        }
        if (callingUid < 10000) {
            forbidResult.result = 0;
            forbidResult.securityLevel = 2;
            forbidResult.packageName = "this is system app";
            return forbidResult;
        }
        PackageInfo packageInfo = getPackageInfoFromCallingUid(callingUid, UserHandle.getCallingUserId());
        if (packageInfo == null) {
            forbidResult.result = 1;
            forbidResult.securityLevel = 0;
            forbidResult.packageName = "unknow!";
            return forbidResult;
        }
        String callPkgName = packageInfo.packageName;
        forbidResult.packageName = callPkgName;
        ApplicationInfo applicationInfo = packageInfo.applicationInfo;
        if (checkCallerIsSystemApp(applicationInfo)) {
            forbidResult.result = 0;
            forbidResult.securityLevel = 2;
            if (DEBUG_FOR_FB_APL) {
                Slog.d(TAG, "caller pkg " + callingUid + " is system app, so donot intercept!");
            }
            return forbidResult;
        } else if (isCallerAppInFilterList(callPkgName)) {
            forbidResult.result = 0;
            forbidResult.securityLevel = 2;
            if (DEBUG_FOR_FB_APL) {
                Slog.d(TAG, "caller pkg " + callingUid + " is in Filter List, so donot intercept!");
            }
            return forbidResult;
        } else {
            InstallAppConfig callerAppConfig = getCallerPkgConfig(callPkgName);
            if (DEBUG_FOR_FB_APL) {
                VSlog.d(TAG, "Checking  appConfig -> " + callerAppConfig);
            }
            if (forbidResult.result == 0) {
                forbidResult.securityLevel = 2;
                return forbidResult;
            } else if (callerAppConfig == null) {
                forbidResult.result = 1;
                forbidResult.securityLevel = 2;
                return forbidResult;
            } else {
                if (DEBUG_FOR_FB_APL) {
                    Slog.d(TAG, "caller pkg " + forbidResult.packageName + " isForce:" + callerAppConfig.isForce + " mLocalInsertAppConfigMap:" + this.mLocalInsertAppConfigMap);
                }
                int i = callerAppConfig.isForce;
                if (i != 0) {
                    if (i == 1) {
                        forbidResult.result = 2;
                        forbidResult.securityLevel = 0;
                    } else if (i == 2) {
                        waitForShowWarnDialog(forbidResult, callerAppConfig, callingUid, UserHandle.getCallingUserId());
                    }
                } else {
                    forbidResult.result = 1;
                    int state = callerAppConfig.status;
                    if (state == 0) {
                        forbidResult.securityLevel = 0;
                    } else if (state == 1) {
                        forbidResult.securityLevel = 1;
                    } else if (state == 2) {
                        forbidResult.securityLevel = 2;
                    }
                }
                if (!callerAppConfig.exist) {
                    synchronized (this.mObjectLock) {
                        InstallAppConfig configTemp = this.mLocalInsertAppConfigMap.get(callerAppConfig.packageName);
                        if (configTemp == null) {
                            this.mLocalInsertAppConfigMap.put(callerAppConfig.packageName, callerAppConfig);
                            insertAppInstallConfigIfNeed(callerAppConfig, true);
                        } else if (DEBUG_FOR_FB_APL) {
                            Slog.d(TAG, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + callerAppConfig.packageName + " exist, donnot insert!");
                        }
                    }
                }
                if (DEBUG_FOR_FB_APL) {
                    Slog.d(TAG, "Check end:  **&&** " + forbidResult + "  " + callerAppConfig + " mLocalInsertAppConfigMap:" + this.mLocalInsertAppConfigMap);
                }
                return forbidResult;
            }
        }
    }

    private InstallAppConfig getCallerPkgConfig(String callPkgName) {
        InstallAppConfig installAppConfig = null;
        synchronized (this.mObjectLock) {
            if (this.mInstallAppConfigList != null && this.mInstallAppConfigList.size() > 0) {
                Iterator<InstallAppConfig> it = this.mInstallAppConfigList.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    InstallAppConfig appConfigTemp = it.next();
                    if (DEBUG_FOR_FB_APL) {
                        Slog.d(TAG, "\t check appConfigTemp:" + appConfigTemp);
                    }
                    if (appConfigTemp.packageName.equals(callPkgName)) {
                        installAppConfig = new InstallAppConfig();
                        installAppConfig._id = appConfigTemp._id;
                        installAppConfig.packageName = appConfigTemp.packageName;
                        installAppConfig.isForce = appConfigTemp.isForce;
                        installAppConfig.last_server_status = appConfigTemp.last_server_status;
                        installAppConfig.status = appConfigTemp.status;
                        installAppConfig.exist = true;
                        if (DEBUG_FOR_FB_APL) {
                            Slog.d(TAG, "Find caller pkg config  " + callPkgName + " is force:" + appConfigTemp.isForce + " status:" + appConfigTemp.status);
                        }
                    }
                }
            } else {
                Message msg = this.mPkmsDBHandler.obtainMessage(1001);
                this.mPkmsDBHandler.sendMessageDelayed(msg, 10000L);
                VSlog.w(TAG, "current app config list is null, check it!");
            }
            if (installAppConfig == null) {
                installAppConfig = new InstallAppConfig();
                installAppConfig.exist = false;
                installAppConfig.packageName = callPkgName;
                installAppConfig.isForce = 0;
                if (DEBUG_FOR_FB_APL) {
                    VSlog.d(TAG, "Not find caller " + callPkgName + " in local configList, will add to List. localList:" + this.mInstallAppConfigList);
                }
                this.mInstallAppConfigList.add(installAppConfig);
            }
        }
        return installAppConfig;
    }

    protected PackageInfo getPackageInfoFromCallingUid(int callingUid, int userId) {
        String[] packagesArray = mPkmService.getPackagesForUid(callingUid);
        if (packagesArray == null) {
            Slog.w(TAG, " callingUid " + callingUid + " getPackge is null.");
            return null;
        }
        if (DEBUG_FOR_FB_APL) {
            Slog.d(TAG, "Get_pks  packageS:" + Arrays.toString(packagesArray) + " callingUid:" + callingUid + "  appConfigList:" + this.mInstallAppConfigList + " " + userId);
        }
        PackageInfo appInfo = null;
        for (String pkgTemp : packagesArray) {
            appInfo = mPkmService.getPackageInfo(pkgTemp, 64, userId);
            if (DEBUG_FOR_FB_APL) {
                Slog.d(TAG, "Find calling package -> " + appInfo);
            }
            if (appInfo != null) {
                break;
            }
        }
        if (appInfo == null) {
            Slog.w(TAG, "Can not get " + callingUid + " package info!");
        }
        return appInfo;
    }

    private boolean isSystemApp(ApplicationInfo info) {
        return (info.flags & 1) != 0;
    }

    private boolean isUpdatedSystemApp(ApplicationInfo info) {
        return (info.flags & 128) != 0;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isSystemPreBuiltApp(String packageName) {
        if (this.mBuiltInThirdPartMap.size() > 0) {
            String value = this.mBuiltInThirdPartMap.get(packageName);
            if (DEBUG_FOR_FB_APL) {
                Slog.d(TAG, packageName + " checking,  builtIn app map value:" + value);
            }
            if (value != null) {
                if (DEBUG_FOR_FB_APL) {
                    Slog.d(TAG, "find  " + packageName + " in system prebulid list.");
                }
                return true;
            }
        } else {
            Slog.w(TAG, "system builtIn app map is NUll.");
        }
        ArrayList<String> arrayList = this.mBaseThrApkList;
        if (arrayList != null && arrayList.size() > 0) {
            Iterator<String> it = this.mBaseThrApkList.iterator();
            while (it.hasNext()) {
                String pkgName = it.next();
                if (DEBUG_FOR_FB_APL) {
                    VSlog.d(TAG, "base three apk " + pkgName + "  check " + packageName);
                }
                if (packageName.equals(pkgName)) {
                    if (DEBUG_FOR_FB_APL) {
                        VSlog.d(TAG, packageName + " is base three part app.");
                    }
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void generateBaseThrApkList(ArrayList<String> baseThrApkList) {
        if (baseThrApkList != null) {
            synchronized (this.mBaseApkLock) {
                this.mBaseThrApkList.clear();
                this.mBaseThrApkList = (ArrayList) baseThrApkList.clone();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void generateBaseSysApkList(ArrayList<String> baseSysApkList) {
        if (baseSysApkList != null) {
            synchronized (this.mBaseApkLock) {
                this.mBaseSysApkList.clear();
                this.mBaseSysApkList = (ArrayList) baseSysApkList.clone();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isBaseThrApk(ArrayList<String> apkList, String packageName) {
        String[] strArr;
        if (packageName == null || packageName.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK)) {
            return false;
        }
        if (apkList == null || apkList.size() <= 0) {
            for (String pkgName : BASE_THR_APK_ARRS) {
                if (packageName.equals(pkgName)) {
                    return true;
                }
            }
        } else {
            Iterator<String> it = apkList.iterator();
            while (it.hasNext()) {
                String pkgName2 = it.next();
                if (packageName.equals(pkgName2)) {
                    return true;
                }
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public ArrayList<String> getAllBaseThrApkList() {
        ArrayList<String> baseThrApkList;
        synchronized (this.mBaseApkLock) {
            baseThrApkList = new ArrayList<>();
            if (this.mBaseThrApkList != null && this.mBaseThrApkList.size() > 0) {
                baseThrApkList = (ArrayList) this.mBaseThrApkList.clone();
            }
        }
        return baseThrApkList;
    }

    protected ArrayList<String> getAllBaseSysApkList() {
        ArrayList<String> baseSysApkList;
        synchronized (this.mBaseApkLock) {
            baseSysApkList = new ArrayList<>();
            if (this.mBaseSysApkList != null && this.mBaseSysApkList.size() > 0) {
                baseSysApkList = (ArrayList) this.mBaseSysApkList.clone();
            }
        }
        return baseSysApkList;
    }

    boolean checkCallerIsSystemApp(ApplicationInfo info) {
        if (info != null) {
            if (isSystemApp(info) || isUpdatedSystemApp(info)) {
                return true;
            }
            return false;
        }
        return false;
    }

    boolean isCallerAppInFilterList(String pkgName) {
        boolean result = false;
        String[] strArr = FILTER_LIST;
        if (strArr != null && strArr.length > 0) {
            int length = strArr.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                String strTemp = strArr[i];
                if (!strTemp.equals(pkgName)) {
                    i++;
                } else {
                    result = true;
                    break;
                }
            }
        }
        if (DEBUG_FOR_FB_APL) {
            Slog.d(TAG, pkgName + " is In Filter list ? " + result);
        }
        return result;
    }

    protected boolean isInCallState() {
        TelephonyManager telephonymanager = (TelephonyManager) this.mContext.getSystemService("phone");
        boolean result = telephonymanager.getCallState() == 2;
        if (DEBUG) {
            Slog.d(TAG, "check current device is in calling state.   " + result);
        }
        return result;
    }

    private void updateLocalAppConfigInLock() {
        ArrayList<InstallAppConfig> arrayList = this.mInstallAppConfigList;
        if (arrayList != null && arrayList.size() > 0) {
            if (DEBUG_FOR_FB_APL) {
                Slog.d(TAG, "update local app config map befor:" + this.mLocalInsertAppConfigMap);
            }
            this.mLocalInsertAppConfigMap.clear();
            ArrayList<InstallAppConfig> installAppConfigTemp = (ArrayList) this.mInstallAppConfigList.clone();
            Iterator<InstallAppConfig> it = installAppConfigTemp.iterator();
            while (it.hasNext()) {
                InstallAppConfig configTemp = it.next();
                this.mLocalInsertAppConfigMap.put(configTemp.packageName, configTemp);
            }
            return;
        }
        this.mLocalInsertAppConfigMap.clear();
        if (DEBUG_FOR_FB_APL) {
            Slog.d(TAG, "updateLocalAppConfigInLock  local config list is NULL, clear first!");
        }
    }

    private void waitForShowWarnDialog(ForbidResult forbidResult, InstallAppConfig appConfig, int callingUid, int userId) {
        if (DEBUG_FOR_FB_APL) {
            VSlog.d(TAG, "waiting For Show WarnDialog  " + appConfig);
        }
        boolean isNeedUpdate = false;
        if (!this.isDeviceBootComplete) {
            VSlog.i(TAG, "Current device is not boot complete, not show app alert dialog; Just FB.  pkgName:" + appConfig.packageName);
            forbidResult.result = 2;
            forbidResult.securityLevel = 0;
            return;
        }
        boolean isCalling = isInCallState();
        if (isCalling) {
            VSlog.i(TAG, "Current is calling, not show any alert dialog; Just FB.");
            forbidResult.result = 2;
            forbidResult.securityLevel = 0;
            return;
        }
        boolean isForeGround = queryPackageIsForeGroundProcess(appConfig.packageName);
        if (!isForeGround) {
            if (DEBUG_FOR_FB_APL) {
                VSlog.d(TAG, appConfig.packageName + " is backgroud process. Dont show alert dialog!");
            }
            forbidResult.result = 2;
            forbidResult.securityLevel = 0;
            return;
        }
        DialogResult warnDialogResult = showGetInstallPackageWarnDialog(appConfig.packageName, userId, Binder.getCallingPid());
        if (DEBUG_FOR_FB_APL) {
            VSlog.d(TAG, "After wait " + appConfig.packageName + " dialog result " + warnDialogResult);
        }
        int i = appConfig.isForce;
        appConfig.oldForce = appConfig.isForce;
        if (warnDialogResult.result == 2) {
            appConfig.isForce = 0;
            isNeedUpdate = true;
            forbidResult.result = 1;
            int state = appConfig.status;
            if (state == 0) {
                forbidResult.securityLevel = 0;
            } else if (state == 1) {
                forbidResult.securityLevel = 1;
            } else if (state == 2) {
                forbidResult.securityLevel = 2;
            }
            if (DEBUG_FOR_FB_APL) {
                VSlog.d(TAG, "User click allow, " + appConfig.packageName + "  ");
            }
        } else if (warnDialogResult.result == 1) {
            appConfig.isForce = 1;
            isNeedUpdate = true;
            forbidResult.result = 2;
        } else if (warnDialogResult.result == 3) {
            isNeedUpdate = false;
            forbidResult.result = 2;
        }
        if (DEBUG_FOR_FB_APL) {
            VSlog.d(TAG, "End wait for dialog result. AC:" + appConfig + " FR:" + forbidResult + " WDR:" + warnDialogResult);
        }
        if (!warnDialogResult.isUserChoose) {
            isNeedUpdate = false;
        }
        if (DEBUG_FOR_FB_APL) {
            VSlog.d(TAG, "isNeedUpdate:" + isNeedUpdate + " " + appConfig);
        }
        if (!appConfig.exist) {
            synchronized (this.mObjectLock) {
                InstallAppConfig configTemp = this.mLocalInsertAppConfigMap.get(appConfig.packageName);
                if (configTemp == null) {
                    appConfig.exist = true;
                    this.mLocalInsertAppConfigMap.put(appConfig.packageName, appConfig);
                    if (!warnDialogResult.isUserChoose) {
                        insertAppInstallConfigIfNeed(appConfig, false);
                    } else {
                        insertAppInstallConfigIfNeed(appConfig, true);
                    }
                    isNeedUpdate = false;
                    updateLocalInstallAppConfigs(appConfig);
                } else if (DEBUG_FOR_FB_APL) {
                    VSlog.d(TAG, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + appConfig.packageName + " exist, donnot insert!");
                }
            }
        }
        if (isNeedUpdate) {
            updateLocalInstallAppConfigs(appConfig);
            updateAppInstallConfigIsForceValueToDB(appConfig);
        }
    }

    private DialogResult showGetInstallPackageWarnDialog(String callerPackageName, int userId, int callingPid) {
        final VivoADBInstallWarningDialog warnDialog;
        DialogResult dialogResult;
        ApplicationInfo appInfo = mPkmService.getApplicationInfo(callerPackageName, 0, userId);
        String applicationName = (String) this.mContext.getPackageManager().getApplicationLabel(appInfo);
        if (DEBUG_FOR_FB_APL) {
            VSlog.d(TAG, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + callerPackageName + " " + applicationName + " showGetInstallPackageWarnDialog");
        }
        synchronized (this.mWarningDlgMap) {
            try {
                VivoADBInstallWarningDialog warnDialog2 = this.mWarningDlgMap.get(callerPackageName);
                try {
                    if (warnDialog2 == null) {
                        try {
                            warnDialog = new VivoADBInstallWarningDialog(mPkmService, this.mContext, this.mMainHandler, callerPackageName, applicationName, 268435456, callingPid);
                            this.mWarningDlgMap.put(callerPackageName, warnDialog);
                            if (DEBUG_FOR_FB_APL) {
                                VSlog.d(TAG, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + callerPackageName + " will show alert dialog, warnDialog:" + warnDialog);
                            }
                            this.mMainHandler.post(new Runnable() { // from class: com.android.server.pm.VivoPKMSDatabaseUtils.2
                                @Override // java.lang.Runnable
                                public void run() {
                                    warnDialog.showGetInstallPackageWarnDialog();
                                }
                            });
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    } else {
                        if (DEBUG_FOR_FB_APL) {
                            VSlog.d(TAG, callerPackageName + " warn dialog already in showing.  Donot show new!!  warnDialog:" + warnDialog2);
                        }
                        warnDialog = warnDialog2;
                    }
                    synchronized (warnDialog) {
                        try {
                            warnDialog.wait(11000L);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        synchronized (this.mWarningDlgMap) {
                            if (this.mWarningDlgMap.containsKey(callerPackageName)) {
                                this.mWarningDlgMap.remove(callerPackageName);
                            }
                        }
                        int result = warnDialog.getWarningConfirmResult();
                        if (result == 3) {
                            dialogResult = new DialogResult(result, false);
                        } else if (result == 2) {
                            dialogResult = new DialogResult(result, true);
                        } else {
                            dialogResult = new DialogResult(result, warnDialog.mRememberChoice | warnDialog.mDialogIsChecked);
                        }
                        if (DEBUG_FOR_FB_APL) {
                            VSlog.d(TAG, callerPackageName + " warn dialog result " + result + " isTimeout:" + warnDialog.mWarnDialogIsTimeOut + " " + dialogResult);
                        }
                    }
                    return dialogResult;
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    protected void updateAppInstallConfigIsForceValueToDB(InstallAppConfig appConfig) {
        boolean isAlreadyQueue = this.mPkmsDBHandler.hasMessages(1007);
        if (DEBUG_FOR_FB_APL) {
            VSlog.d(TAG, "updateAppInstallConfigIsForceValueToDB appConfig:" + appConfig + " isAlreadyQueue:" + isAlreadyQueue);
        }
        Message msg = this.mPkmsDBHandler.obtainMessage(1007);
        msg.obj = appConfig;
        this.mPkmsDBHandler.sendMessageDelayed(msg, 0L);
    }

    protected void scheduleDeleteAppConfigFromLocalCache(String packageName) {
        Message msg = this.mPkmsDBHandler.obtainMessage(1005);
        msg.obj = packageName;
        this.mPkmsDBHandler.sendMessage(msg);
    }

    protected void deleteAppConfigFromLocalCache(String packageName) {
        synchronized (this.mObjectLock) {
            this.mLocalInsertAppConfigMap.remove(packageName);
        }
    }

    protected void insertAppInstallConfigIfNeed(InstallAppConfig callerAppConfig, boolean isForceChange) {
        if (DEBUG_FOR_FB_APL) {
            VSlog.d(TAG, "insertAppInstallConfigIfNeed " + callerAppConfig + " isForceChange:" + isForceChange);
        }
        try {
            insertCallerAppConfigToDB(callerAppConfig, isForceChange);
        } catch (Exception e) {
            VSlog.w(TAG, "insert catche exceptin " + e);
        }
    }

    private void insertCallerAppConfigToDB(InstallAppConfig callerAppConfig, boolean isForceChange) {
        String encrPakcageName = null;
        if (callerAppConfig.packageName != null) {
            try {
                encrPakcageName = encrpt(callerAppConfig.packageName);
            } catch (Exception e) {
                this.mLocalInsertAppConfigMap.remove(callerAppConfig.packageName);
                Slog.w(TAG, "insert caller package to local db catch exception.");
            }
        }
        if (DEBUG_FOR_FB_APL) {
            Slog.d(TAG, "insertCallerAppConfigToDB  oldName " + callerAppConfig.packageName + " newName " + encrPakcageName);
        }
        if (encrPakcageName == null) {
            encrPakcageName = callerAppConfig.packageName;
        }
        ContentValues contentValue = new ContentValues();
        contentValue.put(PKGNAME, encrPakcageName);
        if (isForceChange) {
            contentValue.put(ISFORCE, Integer.valueOf(callerAppConfig.isForce));
        } else {
            contentValue.put(ISFORCE, Integer.valueOf(callerAppConfig.oldForce));
        }
        contentValue.put(LAST_SERVER_STATUS, (Integer) (-1));
        contentValue.put(STATUS, Integer.valueOf(callerAppConfig.status));
        this.mQueryHandler.startInsert(201, null, this.CONTENT_URI, contentValue);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<ApplicationInfo> buildSysBaseApplicationInfoList(String packageName, int callingUid, int userId) {
        ApplicationInfo applicationInfo;
        String[] strArr;
        String[] strArr2;
        ArrayList<ApplicationInfo> list = new ArrayList<>();
        long ident = Binder.clearCallingIdentity();
        if (packageName != null) {
            try {
                PackageInfo pkgInfo = mPkmService.getPackageInfo(packageName, 64, userId);
                if (pkgInfo != null && (applicationInfo = pkgInfo.applicationInfo) != null) {
                    list.add(applicationInfo);
                }
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
                throw th;
            }
        }
        ArrayList<String> baseSysApkList = getAllBaseSysApkList();
        if (baseSysApkList == null || baseSysApkList.size() <= 0) {
            for (String basePkgName : BASE_SYS_APK_ARRS) {
                buildSysBaseApplicationInfoParceledListInner(basePkgName, userId, list);
            }
        } else {
            Iterator<String> it = baseSysApkList.iterator();
            while (it.hasNext()) {
                String basePkgName2 = it.next();
                buildSysBaseApplicationInfoParceledListInner(basePkgName2, userId, list);
            }
            baseSysApkList.clear();
        }
        ArrayList<String> baseThrApkList = getAllBaseThrApkList();
        if (baseThrApkList != null && baseThrApkList.size() > 0) {
            Iterator<String> it2 = baseThrApkList.iterator();
            while (it2.hasNext()) {
                String basePkgName3 = it2.next();
                buildSysBaseApplicationInfoParceledListInner(basePkgName3, userId, list);
            }
            baseThrApkList.clear();
        } else {
            for (String basePkgName4 : BASE_THR_APK_ARRS) {
                buildSysBaseApplicationInfoParceledListInner(basePkgName4, userId, list);
            }
        }
        Binder.restoreCallingIdentity(ident);
        if (DEBUG_FOR_FB_APL) {
            Slog.d(TAG, "create application list, caller pakcage name: " + packageName + " " + list.size());
        }
        if (DEBUG_FOR_FB_APL) {
            Iterator<ApplicationInfo> it3 = list.iterator();
            while (it3.hasNext()) {
                ApplicationInfo applicationInof = it3.next();
                VSlog.d(TAG, "\tbuild pkginfo list ->  " + applicationInof);
            }
        }
        return list;
    }

    void buildSysBaseApplicationInfoParceledListInner(String basePkgName, int userId, ArrayList<ApplicationInfo> list) {
        ApplicationInfo applicationInfo;
        PackageInfo pkgInfo = mPkmService.getPackageInfo(basePkgName, 64, userId);
        if (pkgInfo != null && (applicationInfo = pkgInfo.applicationInfo) != null) {
            list.add(applicationInfo);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ParceledListSlice<PackageInfo> buildSysBasePackageInfoParceledList(String packageName, int callingUid, int userId) {
        String[] strArr;
        String[] strArr2;
        ArrayList<PackageInfo> list = new ArrayList<>();
        long ident = Binder.clearCallingIdentity();
        if (packageName != null) {
            try {
                PackageInfo pkgInfo = mPkmService.getPackageInfo(packageName, 64, userId);
                if (pkgInfo == null) {
                    VSlog.w(TAG, callingUid + " " + packageName + " getPackageInfo is NULL!");
                } else {
                    list.add(pkgInfo);
                }
                VSlog.d(TAG, "build sys base apk: " + packageName + " " + pkgInfo);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
                throw th;
            }
        }
        ArrayList<String> baseSysApkList = getAllBaseSysApkList();
        if (baseSysApkList == null || baseSysApkList.size() <= 0) {
            for (String basePkgName : BASE_SYS_APK_ARRS) {
                buildSysBasePackageInfoParceledListInner(basePkgName, userId, list);
            }
        } else {
            Iterator<String> it = baseSysApkList.iterator();
            while (it.hasNext()) {
                String basePkgName2 = it.next();
                buildSysBasePackageInfoParceledListInner(basePkgName2, userId, list);
            }
            baseSysApkList.clear();
        }
        ArrayList<String> baseThrApkList = getAllBaseThrApkList();
        if (baseThrApkList != null && baseThrApkList.size() > 0) {
            Iterator<String> it2 = baseThrApkList.iterator();
            while (it2.hasNext()) {
                String basePkgName3 = it2.next();
                buildSysBasePackageInfoParceledListInner(basePkgName3, userId, list);
            }
            baseThrApkList.clear();
        } else {
            for (String basePkgName4 : BASE_THR_APK_ARRS) {
                buildSysBasePackageInfoParceledListInner(basePkgName4, userId, list);
            }
        }
        Binder.restoreCallingIdentity(ident);
        if (DEBUG_FOR_FB_APL) {
            Slog.d(TAG, "create PackageInfo list, caller pakcage name:" + packageName + "  returnListSize:" + list.size());
        }
        if (DEBUG_FOR_FB_APL) {
            Iterator<PackageInfo> it3 = list.iterator();
            while (it3.hasNext()) {
                PackageInfo pakcageInfo = it3.next();
                Slog.d(TAG, "\tbuild pkginfo list ->  " + pakcageInfo);
            }
        }
        return new ParceledListSlice<>(list);
    }

    void buildSysBasePackageInfoParceledListInner(String packageName, int userId, ArrayList<PackageInfo> list) {
        PackageInfo pkgInfo = mPkmService.getPackageInfo(packageName, 64, userId);
        if (pkgInfo != null) {
            list.add(pkgInfo);
        }
    }

    void scheduleInitBuildInAppData() {
        Message msg = this.mPkmsDBHandler.obtainMessage(1006);
        this.mPkmsDBHandler.sendMessage(msg);
    }

    void handlerInitBuildInAppData() {
        String str;
        String str2;
        File builtInThirdPartAppFile;
        String[] appFilePathList;
        int i;
        String[] appFilePathList2;
        String appPath;
        String str3 = BuiltInThirdPartVivoDirPath;
        String str4 = BuiltInThirdPartDirPath;
        try {
            File builtInThirdPartAppFile2 = new File(BuiltInThirdPartDirPath);
            if (builtInThirdPartAppFile2.exists()) {
                String[] appFilePathList3 = builtInThirdPartAppFile2.list();
                if (DEBUG_FOR_FB_APL) {
                    VSlog.d(TAG, "begin sacn buildIn app  /apps exist.");
                }
                if (appFilePathList3 != null) {
                    int length = appFilePathList3.length;
                    int i2 = 0;
                    while (i2 < length) {
                        String filePath = appFilePathList3[i2];
                        File apkFile = new File(str4, filePath);
                        if (!apkFile.isDirectory()) {
                            str2 = str4;
                            builtInThirdPartAppFile = builtInThirdPartAppFile2;
                            appFilePathList = appFilePathList3;
                        } else {
                            File[] apkFileList = apkFile.listFiles();
                            if (apkFileList == null) {
                                str2 = str4;
                                builtInThirdPartAppFile = builtInThirdPartAppFile2;
                                appFilePathList = appFilePathList3;
                            } else {
                                str2 = str4;
                                int i3 = 0;
                                for (int length2 = apkFileList.length; i3 < length2; length2 = i) {
                                    File apkFileTemp = apkFileList[i3];
                                    File builtInThirdPartAppFile3 = builtInThirdPartAppFile2;
                                    if (apkFileTemp != null) {
                                        String appPath2 = apkFileTemp.getAbsolutePath();
                                        if (!DEBUG) {
                                            i = length2;
                                            appPath = appPath2;
                                            appFilePathList2 = appFilePathList3;
                                        } else {
                                            StringBuilder sb = new StringBuilder();
                                            i = length2;
                                            sb.append("scan_buildin app data, path:");
                                            appPath = appPath2;
                                            sb.append(appPath);
                                            appFilePathList2 = appFilePathList3;
                                            sb.append(" apkFile:");
                                            sb.append(apkFileTemp);
                                            VSlog.i(TAG, sb.toString());
                                        }
                                        if (appPath.endsWith("apk")) {
                                            try {
                                                PackageParser.PackageLite pkg = PackageParser.parsePackageLite(apkFileTemp, 0);
                                                try {
                                                    try {
                                                        this.mBuiltInThirdPartMap.put(pkg.packageName, appPath);
                                                        if (DEBUG_FOR_FB_APL) {
                                                            VSlog.d(TAG, "\tAdd_BuiltIn app to map.  pkgName:" + pkg.packageName + " apkPath:" + appPath);
                                                        }
                                                    } catch (Exception e) {
                                                        e = e;
                                                        e.printStackTrace();
                                                        VSlog.d(TAG, "create sys pre builtIn app catch exception. " + e.toString());
                                                        i3++;
                                                        appFilePathList3 = appFilePathList2;
                                                        builtInThirdPartAppFile2 = builtInThirdPartAppFile3;
                                                    }
                                                } catch (Exception e2) {
                                                    e = e2;
                                                }
                                            } catch (Exception e3) {
                                                e = e3;
                                            }
                                        }
                                    } else {
                                        i = length2;
                                        appFilePathList2 = appFilePathList3;
                                    }
                                    i3++;
                                    appFilePathList3 = appFilePathList2;
                                    builtInThirdPartAppFile2 = builtInThirdPartAppFile3;
                                }
                                builtInThirdPartAppFile = builtInThirdPartAppFile2;
                                appFilePathList = appFilePathList3;
                            }
                        }
                        i2++;
                        str4 = str2;
                        appFilePathList3 = appFilePathList;
                        builtInThirdPartAppFile2 = builtInThirdPartAppFile;
                    }
                } else {
                    VSlog.w(TAG, " /apps app file is null!");
                }
            } else if (DEBUG_FOR_FB_APL) {
                VSlog.d(TAG, "/apps path  in not exist, ");
            }
            File builtInThirdPartVivoAppFile = new File(BuiltInThirdPartVivoDirPath);
            if (builtInThirdPartVivoAppFile.exists()) {
                String[] appFilePathList4 = builtInThirdPartVivoAppFile.list();
                if (DEBUG_FOR_FB_APL) {
                    VSlog.d(TAG, "begin sacn buildIn app  /system/vivo-apps exist appFilePathList:" + appFilePathList4);
                }
                if (appFilePathList4 != null) {
                    int length3 = appFilePathList4.length;
                    int i4 = 0;
                    while (i4 < length3) {
                        File apkFile2 = new File(str3, appFilePathList4[i4]);
                        String apkFilePath = apkFile2.getAbsolutePath();
                        if (!apkFilePath.endsWith("apk")) {
                            str = str3;
                        } else {
                            try {
                                PackageParser.PackageLite pkg2 = PackageParser.parsePackageLite(apkFile2, 0);
                                this.mBuiltInThirdPartMap.put(pkg2.packageName, apkFilePath);
                                if (DEBUG_FOR_FB_APL) {
                                    VSlog.d(TAG, "\tAdd_BuiltIn app to map.  pkgName:" + pkg2.packageName + " path:" + apkFilePath);
                                }
                                str = str3;
                            } catch (Exception e4) {
                                e4.printStackTrace();
                                StringBuilder sb2 = new StringBuilder();
                                sb2.append("create sys pre builtIn app catch exception. ");
                                str = str3;
                                sb2.append(e4.toString());
                                VSlog.d(TAG, sb2.toString());
                            }
                        }
                        i4++;
                        str3 = str;
                    }
                } else {
                    VSlog.w(TAG, " /system/vivo-apps app file is null!");
                }
            } else if (DEBUG) {
                VSlog.d(TAG, "/system/vivo-apps is not exist, ");
            }
        } catch (Exception e5) {
            VSlog.e(TAG, "init data catch exception " + e5.toString());
        }
        if (DEBUG_FOR_FB_APL) {
            VSlog.d(TAG, "init BuiltInThirdPartMap end   builtInApp size:" + this.mBuiltInThirdPartMap.size() + "\n " + this.mBuiltInThirdPartMap);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class ForbidResult {
        int callingUid;
        String packageName;
        int result = 1;
        int securityLevel = 0;

        public ForbidResult() {
        }

        public ForbidResult(String packageName, int callingUid) {
            this.packageName = packageName;
            this.callingUid = callingUid;
        }

        public String toString() {
            return "packageName " + this.packageName + " callingUid:" + this.callingUid + "  result:" + this.result + " securityLevel:" + this.securityLevel;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class InstallAppConfig {
        int _id;
        boolean exist;
        int isForce;
        int last_server_status;
        boolean notUpdateToDB;
        int oldForce;
        String packageName;
        int status;

        public InstallAppConfig() {
            this.isForce = 0;
            this.last_server_status = -1;
            this.status = 2;
            this.exist = true;
            this.notUpdateToDB = false;
            this.oldForce = -1;
        }

        public InstallAppConfig(int _id, String packageName, int isForce, int last_server_status, int status) {
            this.isForce = 0;
            this.last_server_status = -1;
            this.status = 2;
            this.exist = true;
            this.notUpdateToDB = false;
            this.oldForce = -1;
            this._id = _id;
            this.packageName = packageName;
            this.isForce = isForce;
            this.last_server_status = last_server_status;
            this.status = status;
        }

        public String toString() {
            return "PKG_name " + this.packageName + " _id:" + this._id + " isForce:" + this.isForce + " last_server_status:" + this.last_server_status + " status:" + this.status + " exist:" + this.exist + " notUpdateToDB:" + this.notUpdateToDB + " oldForce:" + this.oldForce;
        }
    }

    /* loaded from: classes.dex */
    class AppRequestCache {
        int callingPid;
        int callingUid;
        int isForce;
        String packageName;
        long requestTime;

        public AppRequestCache(int callingPid, int callingUid, int isForce, String packageName, long requestTime) {
            this.callingPid = callingPid;
            this.callingUid = callingUid;
            this.isForce = isForce;
            this.packageName = packageName;
            this.requestTime = requestTime;
        }

        public String toString() {
            return " packageName:" + this.packageName + " isForce:" + this.isForce + " callingPid:" + this.callingPid + " callingUid:" + this.callingUid + " requestTime:" + this.requestTime;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class DialogResult {
        boolean isUserChoose;
        int result;

        public DialogResult() {
        }

        public DialogResult(int result, boolean isUserChoose) {
            this.result = result;
            this.isUserChoose = isUserChoose;
        }

        public String toString() {
            return "DialogResult " + this.result + " isUserChoose:" + this.isUserChoose;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class HomeRestrictionObserver extends ContentObserver {
        HomeRestrictionObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver contentResolver = VivoPKMSDatabaseUtils.this.mResolver;
            contentResolver.registerContentObserver(Uri.parse(Settings.Secure.CONTENT_URI + "/" + VivoPKMSDatabaseUtils.LOCK_SYSTEMHOME_ENABLED), false, this, -1);
            VivoPKMSDatabaseUtils vivoPKMSDatabaseUtils = VivoPKMSDatabaseUtils.this;
            vivoPKMSDatabaseUtils.updatemLockSystemHomeState(vivoPKMSDatabaseUtils.mContext);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            VivoPKMSDatabaseUtils vivoPKMSDatabaseUtils = VivoPKMSDatabaseUtils.this;
            vivoPKMSDatabaseUtils.updatemLockSystemHomeState(vivoPKMSDatabaseUtils.mContext);
        }
    }

    public void updatemLockSystemHomeState(Context context) {
        if (context == null) {
            return;
        }
        String mode = Settings.Secure.getStringForUser(context.getContentResolver(), LOCK_SYSTEMHOME_ENABLED, ActivityManager.getCurrentUser());
        if (DEBUG) {
            VSlog.d(TAG_HOME_RESTRICTION, "updatemLockSystemHomeState mode:" + mode);
        }
        if (mode != null) {
            this.mHomeRestrictionEnabled = "0".equals(mode);
        }
        SystemProperties.set("persist.home.restr.enable", this.mHomeRestrictionEnabled ? "true" : "false");
    }

    void startHomeRestrictionOb() {
        if (this.mHomeRestrictionOb == null) {
            this.mHomeRestrictionOb = new HomeRestrictionObserver(null);
        }
        this.mHomeRestrictionOb.observe();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean getHomeRestrictionEnabled() {
        return this.mHomeRestrictionEnabled;
    }
}