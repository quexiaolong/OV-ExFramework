package com.vivo.services.security.server;

import android.app.AppOpsManager;
import android.app.AppOpsManagerInternal;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.database.sqlite.SQLiteException;
import android.hardware.graphics.common.V1_0.Dataspace;
import android.os.Binder;
import android.os.Handler;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManagerInternal;
import android.text.TextUtils;
import android.util.SparseArray;
import com.android.server.LocalServices;
import com.android.server.am.EmergencyBroadcastManager;
import com.vivo.framework.security.VivoPermissionManager;
import com.vivo.services.security.client.VivoPermissionInfo;
import com.vivo.services.security.client.VivoPermissionType;
import com.vivo.services.security.server.db.VivoPermissionDataBase;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* loaded from: classes.dex */
public class VivoPermissionConfig {
    public static final String ACCESS_WIFI_STATE_PERMISSION = "ACCESS_WIFI_STATE";
    private static final int DEBUG_CTS_TEST = SystemProperties.getInt("persist.debug.c_test", 0);
    private static final boolean DEBUG_CTS_TEST_23;
    private static final String FLAG_JUSTSY = "2";
    private static final String FLAG_TONGCHENG = "4";
    private static final String GN_SUPPORT;
    public static final boolean IS_25T30_LITE;
    private static final String PLATFORM_PACKAGE_NAME = "android";
    private static final String TAG = "VPS_VPC";
    private static SparseArray<Integer> mDataBaseStates;
    private static byte[] mVPILock;
    private AppOpsManager mAppopsManager;
    private Context mContext;
    private Handler mHandler;
    private PackageManager mPackageManager;
    private VivoPermissionService mVPS;
    private SparseArray<SparseArray<ArrayList<VivoPermissionInfo>>> mPermissionLists = new SparseArray<>();
    private SparseArray<List<VivoPermissionInfo>> mTrustedAppLists = new SparseArray<>();
    private SparseArray<List<VivoPermissionInfo>> mMonitorAppLists = new SparseArray<>();
    private SparseArray<HashMap<String, VivoPermissionInfo>> mPermissionMaps = new SparseArray<>();
    private SparseArray<VivoPermissionDataBase> mVPDBs = new SparseArray<>();
    private SparseArray<HashMap<String, String>> mBuiltInThirdPartMaps = new SparseArray<>();
    private SparseArray<ArrayList<VivoPermissionInfo>> mRemovedAppLists = new SparseArray<>();
    private final int REMOVED_APP_LIST_MAX_NUM = 10;
    private SparseArray<ArrayList<String>> mAppWhiteLists = new SparseArray<>();
    private boolean mIsConfigFinished = false;
    private UserManagerInternal mUserManagerInternal = null;
    private AppOpsManagerInternal mAppOpsManagerInternal = null;

    static {
        DEBUG_CTS_TEST_23 = "1".equals(SystemProperties.get("ro.build.g_test", "0")) || "1".equals(SystemProperties.get("ro.build.aia", "0"));
        GN_SUPPORT = SystemProperties.get("ro.build.gn.support", "0");
        mVPILock = new byte[0];
        mDataBaseStates = new SparseArray<>();
        IS_25T30_LITE = "Funtouch OS_3.0 Lite".equals(SystemProperties.get("ro.vivo.os.build.display.id", "0"));
    }

    public VivoPermissionConfig(VivoPermissionService vps, Context context, Handler handler) {
        int[] userIds;
        this.mVPS = null;
        this.mContext = null;
        this.mPackageManager = null;
        this.mAppopsManager = null;
        this.mVPS = vps;
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
        this.mAppopsManager = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
        this.mHandler = handler;
        for (int userId : getUserManagerInternal().getUserIds()) {
            doForUserInit(userId);
        }
    }

    private UserManagerInternal getUserManagerInternal() {
        if (this.mUserManagerInternal == null) {
            this.mUserManagerInternal = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        }
        return this.mUserManagerInternal;
    }

    private AppOpsManagerInternal getAppOpsManagerInternal() {
        if (this.mAppOpsManagerInternal == null) {
            this.mAppOpsManagerInternal = (AppOpsManagerInternal) LocalServices.getService(AppOpsManagerInternal.class);
        }
        return this.mAppOpsManagerInternal;
    }

    private boolean isValidUserId(int userId) {
        if (userId != -1 && userId < 0) {
            VivoPermissionService.printfError("Invalid userId:" + userId);
            return false;
        }
        return true;
    }

    public void doForUserInit(int userId) {
        if (isValidUserId(userId)) {
            VivoPermissionService.printfDebug("doForUserAdd userId = " + userId);
            for (int index = 0; index < 32; index++) {
                getPermissionList(userId).put(index, new ArrayList<>());
            }
            getAppWhiteList(userId).add("com.vivo.PCTools");
            if ("2".equals(GN_SUPPORT)) {
                VivoPermissionService.printfInfo("list add just mdm");
                getAppWhiteList(userId).add("com.justsy.mdm");
            } else if ("4".equals(GN_SUPPORT)) {
                VivoPermissionService.printfInfo("list add tongchen");
                getAppWhiteList(userId).add("com.tcshopapp");
                getAppWhiteList(userId).add("com.tongcheng.android");
            }
            startDefaultPermissionConfigAsync(userId);
        }
    }

    public void doForUserRemoved(int userId) {
        if (isValidUserId(userId)) {
            VivoPermissionService.printfDebug("doForUserRemoved userId = " + userId);
            this.mPermissionLists.delete(userId);
            this.mTrustedAppLists.delete(userId);
            this.mMonitorAppLists.delete(userId);
            this.mPermissionMaps.delete(userId);
            VivoPermissionDataBase vpdb = this.mVPDBs.get(userId);
            if (vpdb != null) {
                vpdb.removeDb();
            }
            this.mVPDBs.delete(userId);
            this.mBuiltInThirdPartMaps.delete(userId);
            this.mRemovedAppLists.delete(userId);
            this.mAppWhiteLists.delete(userId);
            mDataBaseStates.delete(userId);
        }
    }

    private void startDefaultPermissionConfigAsync(final int userId) {
        this.mHandler.post(new Runnable() { // from class: com.vivo.services.security.server.VivoPermissionConfig.1
            @Override // java.lang.Runnable
            public void run() {
                synchronized (VivoPermissionConfig.mVPILock) {
                    VivoPermissionService.printfInfo("Start:startDefaultPermissionConfig");
                    VivoPermissionService.printfDebug("startDefaultPermissionConfig userId = " + userId);
                    VivoPermissionConfig.this.startDefaultPermissionConfig(userId);
                    VivoPermissionService.printfInfo("Finish:startDefaultPermissionConfig");
                    VivoPermissionService.printfInfo("mDataBaseState=" + VivoPermissionConfig.mDataBaseStates.get(userId));
                    VivoPermissionConfig.this.mIsConfigFinished = true;
                }
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startDefaultPermissionConfig(int userId) {
        List<VivoPermissionInfo> allVPIs = null;
        try {
            allVPIs = getVPDB(userId).findAllVPIs();
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        if (allVPIs == null || allVPIs.size() == 0) {
            buildDefaultPermissionsDB(userId);
            SystemProperties.set("persist.vivo.perm.init", "true");
            List<VivoPermissionInfo> allNewVPIs = null;
            try {
                allNewVPIs = getVPDB(userId).findAllVPIs();
            } catch (SQLiteException e2) {
                e2.printStackTrace();
            }
            configAllPermissions(allNewVPIs, userId);
            return;
        }
        String projectName = VivoPermissionManager.getInstance().getProjectName();
        if (projectName != null && projectName.equals("PD1421")) {
            if (true == checkIfAPKNoLongerNeedMonitor(allVPIs, userId)) {
                List<VivoPermissionInfo> allVPIs_temp = null;
                try {
                    allVPIs_temp = getVPDB(userId).findAllVPIs();
                } catch (SQLiteException e3) {
                    e3.printStackTrace();
                }
                configAllPermissions(allVPIs_temp, userId);
                return;
            }
            configAllPermissions(allVPIs, userId);
            return;
        }
        VivoPermissionService.printfInfo("not need checkIfMonitorAPKRemoved");
        configAllPermissions(allVPIs, userId);
    }

    private boolean checkIfAPKNoLongerNeedMonitor(List<VivoPermissionInfo> allVPIs, int userId) {
        VivoPermissionService.printfInfo("checkIfAPKNoLongerNeedMonitor begin");
        boolean hasApkRemove = false;
        if (allVPIs == null || allVPIs.size() == 0) {
            return false;
        }
        int allVPIsSize = allVPIs.size();
        for (int index = 0; index < allVPIsSize; index++) {
            VivoPermissionInfo vpi = allVPIs.get(index);
            if (vpi == null) {
                VivoPermissionService.printfInfo("checkIfAPKNoLongerNeedMonitor vpi is null index=" + index + " allVPIsSize=" + allVPIsSize);
            } else if (true == checkIfAPKNoLongerNeedMonitor(vpi.getPackageName(), userId)) {
                VivoPermissionService.printfInfo("checkIfAPKNoLongerNeedMonitor true app:" + vpi.getPackageName());
                hasApkRemove = true;
                removeVPIFromDB(vpi.getPackageName(), true, userId);
            }
        }
        VivoPermissionService.printfInfo("checkIfAPKNoLongerNeedMonitor end hasApkRemove=" + hasApkRemove);
        return hasApkRemove;
    }

    private boolean checkIfAPKNoLongerNeedMonitor(String packageName, int userId) {
        if (packageName == null) {
            return false;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            try {
                PackageInfo pi = this.mPackageManager.getPackageInfoAsUser(packageName, 4096, userId);
                Binder.restoreCallingIdentity(identity);
                boolean isMonitorSystemApp = VivoPermissionManager.getInstance().needMonitorSystemApp();
                boolean isSystemApp = !this.mVPS.needCheckPkg(pi);
                if (isSystemApp && !isMonitorSystemApp) {
                    VivoPermissionService.printfInfo("checkIfAPKNoLongerNeedMonitor apk is move to system app");
                    return true;
                } else if (getAppWhiteList(userId).contains(pi.packageName)) {
                    VivoPermissionService.printfInfo("checkIfAPKNoLongerNeedMonitor apk is in WhiteList");
                    return true;
                } else {
                    return false;
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                VivoPermissionService.printfInfo("checkIfAPKNoLongerNeedMonitor cannot find this apk:" + packageName);
                Binder.restoreCallingIdentity(identity);
                return false;
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
    }

    private void buildDefaultPermissionsDB(int userId) {
        List<PackageInfo> piList = this.mPackageManager.getInstalledPackagesAsUser(4096, userId);
        int size = piList.size();
        for (int index = 0; index < size; index++) {
            PackageInfo pi = piList.get(index);
            VivoPermissionInfo parseVPI = parseDefaultPackagePermission(pi, false, userId);
            if (parseVPI != null) {
                saveVPIToDB(parseVPI, false, userId);
            }
        }
    }

    private VivoPermissionInfo parseDefaultPackagePermission(String packageName, boolean grantPermissions, int userId) {
        long identity = Binder.clearCallingIdentity();
        try {
            try {
                PackageInfo pi = this.mPackageManager.getPackageInfoAsUser(packageName, 4096, userId);
                Binder.restoreCallingIdentity(identity);
                return parseDefaultPackagePermission(pi, grantPermissions, userId);
            } catch (PackageManager.NameNotFoundException e) {
                VivoPermissionService.printfError("getPackageInfoAsUser throw NameNotFoundException!, pkg: " + packageName + ", userId: " + userId);
                Binder.restoreCallingIdentity(identity);
                return null;
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
    }

    private VivoPermissionInfo parseDefaultPackagePermission(PackageInfo pi, boolean grantPermissions, int userId) {
        if (pi == null) {
            return null;
        }
        VivoPermissionInfo vpi = null;
        boolean isMonitorSystemApp = VivoPermissionManager.getInstance().needMonitorSystemApp();
        boolean isSystemApp = !this.mVPS.needCheckPkg(pi);
        boolean isVivoTest = this.mVPS.isTestApp(pi.packageName);
        if (isVivoTest || getAppWhiteList(userId).contains(pi.packageName)) {
            return null;
        }
        try {
            int packageUid = this.mPackageManager.getPackageUidAsUser(pi.packageName, userId);
            VivoPermissionService.printfInfo("parseDefaultPackagePermission packageName:" + pi.packageName + " packageUid:" + packageUid);
            if (pi.requestedPermissions == null) {
                VivoPermissionService.printfInfo("isSystemApp:" + isSystemApp + " " + pi.packageName + " requestedPermissions is null uid=" + packageUid);
                return null;
            }
            int i = 4;
            if (isSystemApp || 1000 == packageUid || 1001 == packageUid) {
                VivoPermissionService.printfInfo("parseDefaultPackagePermission -------->SystemApp " + pi.packageName + " uid:" + packageUid + " requestedPermissionsNull=false");
                if (isMonitorSystemApp) {
                    if (1000 == packageUid || 1001 == packageUid) {
                        vpi = new VivoPermissionInfo(pi.packageName);
                        for (int index = 0; index < 32; index++) {
                            VivoPermissionType type = VivoPermissionType.getVPType(index);
                            if (type.getVPCategory() == VivoPermissionType.VivoPermissionCategory.OTHERS) {
                                vpi.setPermissionResult(index, 1);
                            } else {
                                vpi.setPermissionResult(index, 3);
                                vpi.setDeniedMode(index, 48);
                                vpi.setDeniedDialogMode(index, 512);
                            }
                        }
                    } else {
                        vpi = new VivoPermissionInfo(pi.packageName);
                        int size = pi.requestedPermissions.length;
                        for (int index2 = 0; index2 < size; index2++) {
                            configDefaultPermissionResults(vpi, pi.requestedPermissions[index2]);
                        }
                        for (int index3 = 0; index3 < 32; index3++) {
                            int result = vpi.getPermissionResult(index3);
                            if (result == 4) {
                                vpi.setPermissionResult(index3, 3);
                            }
                            if (result != 0) {
                                vpi.setDeniedMode(index3, 48);
                                vpi.setDeniedDialogMode(index3, 512);
                            }
                        }
                    }
                } else if (TextUtils.equals("com.vivo.game", pi.packageName) || TextUtils.equals("com.chaozh.iReader", pi.packageName)) {
                    vpi = new VivoPermissionInfo(pi.packageName);
                    int size2 = pi.requestedPermissions.length;
                    for (int index4 = 0; index4 < size2; index4++) {
                        configDefaultPermissionResults(vpi, pi.requestedPermissions[index4]);
                    }
                    for (int index5 = 0; index5 < 32; index5++) {
                        int result2 = vpi.getPermissionResult(index5);
                        if (result2 == 4) {
                            vpi.setPermissionResult(index5, 1);
                        }
                        if (result2 != 0) {
                            vpi.setDeniedMode(index5, 32);
                            vpi.setDeniedDialogMode(index5, (int) VivoPermissionUtils.FLAGS_ALWAYS_USER_SENSITIVE);
                        }
                    }
                }
            } else {
                VivoPermissionService.printfInfo("parseDefaultPackagePermission -------->ThirdParty " + pi.packageName + " uid:" + packageUid + " requestedPermissionsNull=false");
                vpi = new VivoPermissionInfo(pi.packageName);
                int size3 = pi.requestedPermissions.length;
                for (int index6 = 0; index6 < size3; index6++) {
                    configDefaultPermissionResults(vpi, pi.requestedPermissions[index6]);
                }
                if (needDefaultTrustThirdPartApp(pi, userId)) {
                    vpi.grantAllPermissions();
                } else {
                    boolean isSpecialPkg = "jp.co.hit_point.tabikaeru.st".equals(pi.packageName);
                    int index7 = 0;
                    while (index7 < 32) {
                        if (vpi.getPermissionResult(index7) == i) {
                            if (grantPermissions) {
                                vpi.setPermissionResult(index7, 1);
                                vpi.setDeniedMode(index7, 32);
                                vpi.setDeniedDialogMode(index7, (int) VivoPermissionUtils.FLAGS_ALWAYS_USER_SENSITIVE);
                            } else {
                                vpi.setPermissionResult(index7, 3);
                            }
                        }
                        if (isSpecialPkg && index7 == 22) {
                            doForRuntimePermission(pi.packageName, index7, 1, userId);
                        }
                        index7++;
                        i = 4;
                    }
                    if (1 == DEBUG_CTS_TEST) {
                        vpi.grantAllPermissions();
                        VivoPermissionService.printfInfo("DEBUG_TCS_TEST so set all permission GRANTED");
                    }
                    if (VivoPermissionManager.getInstance().isOverSeas()) {
                        vpi.grantAllPermissions();
                    }
                }
            }
            return vpi;
        } catch (Exception e) {
            VivoPermissionService.printfError("fatal error:getPackageUid(" + pi.packageName + ") fail uid = -1");
            e.printStackTrace();
            return null;
        }
    }

    private boolean needDefaultTrustThirdPartApp(PackageInfo pi, int userId) {
        return !VivoPermissionManager.getInstance().needMonitorBuildInApps() && checkBuildInThirdPartApp(pi, userId);
    }

    private boolean checkBuildInThirdPartApp(PackageInfo pi, int userId) {
        boolean z = false;
        if (pi == null || pi.applicationInfo == null) {
            return false;
        }
        boolean result = false;
        try {
            String pkgPath = pi.applicationInfo.sourceDir;
            result = (pkgPath.startsWith("/system/vivo-apps") || pkgPath.startsWith("/data/vivo-apps")) ? true : true;
            VivoPermissionService.printfInfo("checkBuildInThirdPartApp-->pkgPath=" + pkgPath + ";result=" + result);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return result;
        }
    }

    private void configDefaultCustomPermissionResults(VivoPermissionInfo vpi, int typeId) {
        int customTypeId = -1;
        if (typeId == VivoPermissionType.CHANGE_NETWORK_STATE.getVPTypeId()) {
            customTypeId = VivoPermissionType.SEND_MMS.getVPTypeId();
        }
        if (customTypeId == -1) {
            return;
        }
        vpi.setPermissionResult(customTypeId, 4);
    }

    private void configDefaultPermissionResults(VivoPermissionInfo vpi, String permission) {
        VivoPermissionType type = VivoPermissionType.getVPType(permission);
        int typeId = type.getVPTypeId();
        if (VivoPermissionType.isValidTypeId(typeId)) {
            VivoPermissionService.printfInfo(vpi.getPackageName() + " has permission: " + permission);
            if (type.getVPCategory() == VivoPermissionType.VivoPermissionCategory.OTHERS) {
                vpi.setPermissionResult(typeId, 1);
            } else if (permission.contains(ACCESS_WIFI_STATE_PERMISSION)) {
                VivoPermissionService.printfInfo("configDefaultPermissionResults permission=ACCESS_WIFI_STATE just return");
                return;
            } else {
                vpi.setPermissionResult(typeId, 4);
            }
            configDefaultCustomPermissionResults(vpi, typeId);
        }
    }

    private void saveVPIToDB(final VivoPermissionInfo vpi, boolean isAsync, final int userId) {
        if (vpi == null) {
            return;
        }
        if (isAsync) {
            this.mHandler.post(new Runnable() { // from class: com.vivo.services.security.server.VivoPermissionConfig.2
                @Override // java.lang.Runnable
                public void run() {
                    VivoPermissionService.printfDebug("mVPDB is saving vpi :" + vpi.getPackageName());
                    try {
                        VivoPermissionConfig.this.getVPDB(userId).save(vpi);
                    } catch (SQLiteException e) {
                        e.printStackTrace();
                    }
                    VivoPermissionService.printfDebug("mVPDB saved vpi.");
                }
            });
            return;
        }
        try {
            getVPDB(userId).save(vpi);
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    private void removeVPIFromDB(final String packageName, boolean isAsync, final int userId) {
        if (isAsync) {
            this.mHandler.post(new Runnable() { // from class: com.vivo.services.security.server.VivoPermissionConfig.3
                @Override // java.lang.Runnable
                public void run() {
                    VivoPermissionService.printfDebug("mVPDB is deleting " + packageName);
                    try {
                        int result = VivoPermissionConfig.this.getVPDB(userId).delete(packageName);
                        VivoPermissionService.printfDebug("mVPDB deleted result=" + result);
                    } catch (SQLiteException e) {
                        e.printStackTrace();
                    }
                }
            });
            return;
        }
        try {
            getVPDB(userId).delete(packageName);
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    private void addVPIToSpecPermissionList(VivoPermissionInfo vpi, int type, int userId) {
        if (!VivoPermissionType.isValidTypeId(type)) {
            VivoPermissionService.printfError("addAPIToSpecifiedPermPkgList!!! Invalid VivoPermissionType ID!!!");
            return;
        }
        List<VivoPermissionInfo> vpiList = getPermissionList(userId).get(type);
        if (vpiList == null) {
            VivoPermissionService.printfError("addAPIToSpecifiedPermPkgList!!! vpiList null!!!, userid " + userId);
            return;
        }
        boolean hasAdded = false;
        int size = vpiList.size();
        int index = size - 1;
        while (true) {
            if (index < 0) {
                break;
            }
            VivoPermissionInfo currVPI = vpiList.get(index);
            if (!currVPI.getPackageName().equals(vpi.getPackageName())) {
                index--;
            } else {
                hasAdded = true;
                break;
            }
        }
        if (!hasAdded) {
            vpiList.add(vpi);
        }
    }

    private void addVPIToPermissionList(VivoPermissionInfo vpi, int userId) {
        if (vpi == null) {
            return;
        }
        for (int index = 0; index < 32; index++) {
            if (vpi.getPermissionResult(index) != 0) {
                addVPIToSpecPermissionList(vpi, index, userId);
            }
        }
    }

    private void addVPIToPermissionMap(VivoPermissionInfo vpi, int userId) {
        if (vpi != null && vpi.getPackageName() != null && !getPermissionMap(userId).containsKey(vpi.getPackageName())) {
            getPermissionMap(userId).put(vpi.getPackageName(), vpi);
        }
    }

    private void configAllPermissions(List<VivoPermissionInfo> allVPIs, int userId) {
        if (allVPIs == null || allVPIs.size() == 0) {
            return;
        }
        int allVPIsSize = allVPIs.size();
        for (int index = 0; index < allVPIsSize; index++) {
            VivoPermissionInfo vpi = allVPIs.get(index);
            if (isNeedDisplay(vpi)) {
                addVPIToPermissionList(vpi, userId);
                addVPIToPermissionMap(vpi, userId);
                if (vpi.isWhiteListApp()) {
                    getTrustedList(userId).add(vpi);
                } else {
                    getMonitorList(userId).add(vpi);
                }
            }
        }
    }

    protected boolean isNeedDisplay(VivoPermissionInfo vpi) {
        if (vpi == null) {
            return false;
        }
        for (int index = 0; index < 32; index++) {
            VivoPermissionType vpType = VivoPermissionType.getVPType(index);
            VivoPermissionType.VivoPermissionCategory vpc = vpType.getVPCategory();
            int result = vpi.getPermissionResult(index);
            if (result != 0 && vpc != VivoPermissionType.VivoPermissionCategory.OTHERS) {
                return true;
            }
        }
        return false;
    }

    public List<VivoPermissionInfo> getSpecifiedPermAppList(int vpTypeId, int userId) {
        ArrayList<VivoPermissionInfo> arrayList;
        if (!VivoPermissionType.isValidTypeId(vpTypeId)) {
            VivoPermissionService.printfError("getSpecifiedPermPkgList:failed!!! Invalid VivoPermissionType ID!!!");
            return null;
        }
        synchronized (mVPILock) {
            handleVpiList(getPermissionList(userId).get(vpTypeId), true, userId);
            arrayList = getPermissionList(userId).get(vpTypeId);
        }
        return arrayList;
    }

    public List<VivoPermissionInfo> getTrustedAppList(int userId) {
        List<VivoPermissionInfo> trustedList;
        synchronized (mVPILock) {
            trustedList = getTrustedList(userId);
        }
        return trustedList;
    }

    public List<VivoPermissionInfo> getMonitorAppList(int userId) {
        List<VivoPermissionInfo> monitorList;
        synchronized (mVPILock) {
            handleVpiList(getMonitorList(userId), false, userId);
            monitorList = getMonitorList(userId);
        }
        return monitorList;
    }

    public Map<String, VivoPermissionInfo> getPermissionMap() {
        if (!this.mIsConfigFinished) {
            VivoPermissionService.printfInfo("getPermissionMap mIsConfigFinished=false, return null!");
            return null;
        }
        int userId = UserHandle.getUserId(Binder.getCallingUid());
        return getPermissionMap(this.mVPS.verifyIncomingUserId(userId));
    }

    public void handleRuntimePermission(String packageName, boolean nedfixed, int userId) {
        boolean supportsRuntimePermissions;
        char c;
        int result;
        PackageInfo packageInfo = getPackageInfo(packageName, userId);
        if (packageInfo != null && packageInfo.applicationInfo != null && packageInfo.requestedPermissions != null) {
            char c2 = 22;
            boolean supportsRuntimePermissions2 = packageInfo.applicationInfo.targetSdkVersion > 22;
            VivoPermissionService.printfDebug("handleRuntimePermission supportsRuntimePermissions=" + supportsRuntimePermissions2);
            if (supportsRuntimePermissions2) {
                int permissionCount = packageInfo.requestedPermissions.length;
                boolean isLocationGrant = false;
                boolean isLocationGrantOneTime = false;
                boolean isStorageGrant = false;
                int i = 0;
                while (i < permissionCount) {
                    String requestedPermission = packageInfo.requestedPermissions[i];
                    VivoPermissionService.printfQuatitiesLog("handleRuntimePermission requestedPermission=" + requestedPermission);
                    if (!isRuntimePermission(requestedPermission)) {
                        VivoPermissionService.printfQuatitiesLog("handleRuntimePermission  isRuntimePermission = nonono");
                        supportsRuntimePermissions = supportsRuntimePermissions2;
                        c = c2;
                    } else {
                        int typeId = VivoPermissionType.getVPType(requestedPermission).getVPTypeId();
                        VivoPermissionService.printfQuatitiesLog("handleRuntimePermission  typeId=" + typeId);
                        if (!VivoPermissionType.isValidTypeId(typeId)) {
                            supportsRuntimePermissions = supportsRuntimePermissions2;
                            c = c2;
                        } else {
                            boolean granted = (packageInfo.requestedPermissionsFlags[i] & 2) != 0;
                            boolean isFixed = false;
                            boolean isGranted_fg = false;
                            int permissionFlags = this.mPackageManager.getPermissionFlags(requestedPermission, packageName, UserHandle.of(userId));
                            if (nedfixed) {
                                isFixed = (permissionFlags & 2) != 0;
                            }
                            boolean isGrantedOneTime = (permissionFlags & Dataspace.STANDARD_BT709) != 0;
                            if (typeId == 12) {
                                granted |= isLocationGrant;
                                isLocationGrant = granted;
                                supportsRuntimePermissions = supportsRuntimePermissions2;
                                isGranted_fg = this.mPackageManager.checkPermission("android.permission.ACCESS_BACKGROUND_LOCATION", packageName) != 0;
                                isGrantedOneTime |= isLocationGrantOneTime;
                                isLocationGrantOneTime = isGrantedOneTime;
                                c = 22;
                            } else {
                                supportsRuntimePermissions = supportsRuntimePermissions2;
                                c = 22;
                                if (typeId == 22) {
                                    granted |= isStorageGrant;
                                    isStorageGrant = granted;
                                }
                            }
                            if (isGrantedOneTime) {
                                result = 6;
                            } else {
                                result = granted ? isGranted_fg ? 5 : 1 : isFixed ? 2 : 3;
                            }
                            setVivoPermissionInfo(packageName, typeId, result, userId);
                        }
                    }
                    i++;
                    c2 = c;
                    supportsRuntimePermissions2 = supportsRuntimePermissions;
                }
                return;
            }
            int permissionCount2 = packageInfo.requestedPermissions.length;
            for (int i2 = 0; i2 < permissionCount2; i2++) {
                handleLegacyAppopsToVivoPermissionInfo(packageName, packageInfo, packageInfo.requestedPermissions[i2], userId);
            }
        }
    }

    private void handleLegacyAppopsToVivoPermissionInfo(String packageName, PackageInfo packageInfo, String requestedPermission, int userId) {
        if (packageName == null || packageInfo == null || requestedPermission == null || !isRuntimePermission(requestedPermission)) {
            return;
        }
        int typeId = VivoPermissionType.getVPType(requestedPermission).getVPTypeId();
        if (VivoPermissionType.isValidTypeId(typeId)) {
            String appOp = AppOpsManager.permissionToOp(requestedPermission);
            VivoPermissionService.printfQuatitiesLog("handleRuntimePermission checkOnePermission appOp=" + appOp);
            int permissionFlags = this.mPackageManager.getPermissionFlags(requestedPermission, packageName, UserHandle.of(userId));
            boolean reviewRequire = (permissionFlags & 64) != 0;
            if (reviewRequire) {
                setVivoPermissionInfo(packageName, typeId, 3, userId);
                return;
            }
            AppOpsManager appOpsManager = this.mAppopsManager;
            if (appOpsManager != null && appOp != null) {
                int appOpsMode = appOpsManager.unsafeCheckOpRaw(appOp, packageInfo.applicationInfo.uid, packageInfo.packageName);
                if (appOpsMode == 0) {
                    setVivoPermissionInfo(packageName, typeId, 1, userId);
                } else if (appOpsMode != 1) {
                    if (appOpsMode == 4) {
                        setVivoPermissionInfo(packageName, typeId, 5, userId);
                    }
                } else {
                    VivoPermissionInfo vpi = getPermissionMap(userId).get(packageName);
                    if (vpi != null && vpi.getPermissionResult(typeId) != 3 && vpi.getPermissionResult(typeId) != 2) {
                        setVivoPermissionInfo(packageName, typeId, 3, userId);
                    }
                }
            }
        }
    }

    public VivoPermissionInfo getAppPermission(String packageName, int userId) {
        VivoPermissionInfo vpi = null;
        synchronized (mVPILock) {
            if (!getPermissionMap(userId).containsKey(packageName)) {
                updateForPackageAdded_l(packageName, false, userId);
                if (getPermissionMap(userId).containsKey(packageName)) {
                    vpi = getPermissionMap(userId).get(packageName);
                } else {
                    VivoPermissionService.printfInfo("getAppPermission(" + packageName + ") is null !");
                }
            } else {
                vpi = getPermissionMap(userId).get(packageName);
            }
        }
        return vpi;
    }

    private void updateTrustedAndMonitorAppList_l(VivoPermissionInfo vpi, int userId) {
        if (vpi == null) {
            return;
        }
        if (vpi.isWhiteListApp()) {
            if (getMonitorList(userId).contains(vpi)) {
                getMonitorList(userId).remove(vpi);
            }
            if (!getTrustedList(userId).contains(vpi)) {
                getTrustedList(userId).add(0, vpi);
                return;
            }
            return;
        }
        if (getTrustedList(userId).contains(vpi)) {
            getTrustedList(userId).remove(vpi);
        }
        if (!getMonitorList(userId).contains(vpi)) {
            getMonitorList(userId).add(0, vpi);
        }
    }

    public void setWhiteListApp(String packageName, boolean enable, int userId) {
        synchronized (mVPILock) {
            VivoPermissionInfo vpi = getPermissionMap(userId).get(packageName);
            if (vpi == null) {
                return;
            }
            vpi.setWhiteListApp(enable);
            updateTrustedAndMonitorAppList_l(vpi, userId);
            VivoPermissionService.printfDebug("setWhiteListApp-->start saveVPIToDB");
            saveVPIToDB(vpi, true, userId);
        }
    }

    public void setBlackListApp(String packageName, boolean enable, int userId) {
        synchronized (mVPILock) {
            VivoPermissionInfo vpi = getPermissionMap(userId).get(packageName);
            if (vpi == null) {
                return;
            }
            vpi.setBlackListApp(enable);
            updateTrustedAndMonitorAppList_l(vpi, userId);
            VivoPermissionService.printfDebug("setBlackListApp-->start saveVPIToDB");
            saveVPIToDB(vpi, true, userId);
        }
    }

    public boolean isBuildInThirdPartApp(String packageName, int userId) {
        if (packageName == null) {
            return false;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            try {
                PackageInfo pi = this.mPackageManager.getPackageInfoAsUser(packageName, 4096, userId);
                Binder.restoreCallingIdentity(identity);
                return checkBuildInThirdPartApp(pi, userId);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                VivoPermissionService.printfInfo("isBuildInThirdPartApp cannot find this apk:" + packageName);
                Binder.restoreCallingIdentity(identity);
                return false;
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
    }

    public int getDataBaseState(int userId) {
        if (mDataBaseStates.get(userId) == null) {
            mDataBaseStates.put(userId, 1);
            return 1;
        }
        return mDataBaseStates.get(userId).intValue();
    }

    public static void setDataBaseState(int state, int userId) {
        mDataBaseStates.put(userId, Integer.valueOf(state));
    }

    private VivoPermissionInfo setVivoPermissionInfo(String packageName, int vpTypeId, int result, int userId) {
        synchronized (mVPILock) {
            VivoPermissionInfo vpi = getPermissionMap(userId).get(packageName);
            if (vpi == null) {
                return vpi;
            }
            vpi.setPermissionResult(vpTypeId, result);
            float osVer = VivoPermissionManager.getInstance().getOSVersion();
            if (osVer >= 3.0f && result != 1 && result != 5 && vpi.isWhiteListApp()) {
                vpi.setWhiteListApp(false);
            }
            updateTrustedAndMonitorAppList_l(vpi, userId);
            return vpi;
        }
    }

    public void saveAppPermission(String packageName, int vpTypeId, int result, int userId) {
        VivoPermissionInfo vpi = setVivoPermissionInfo(packageName, vpTypeId, result, userId);
        if (vpi != null) {
            saveVPIToDB(vpi, true, userId);
        }
    }

    public void saveAppPermission(VivoPermissionInfo paramVPI, int userId) {
        if (paramVPI == null || paramVPI.getPackageName() == null) {
            return;
        }
        synchronized (mVPILock) {
            VivoPermissionInfo vpi = getPermissionMap(userId).get(paramVPI.getPackageName());
            if (vpi == null) {
                return;
            }
            vpi.copyFrom(paramVPI);
            updateTrustedAndMonitorAppList_l(vpi, userId);
            VivoPermissionService.printfInfo("saveAppPermission all(" + vpi.getPackageName() + ")-->start saveVPIToDB");
            saveVPIToDB(vpi, true, userId);
        }
    }

    private void updatePackagePermission(VivoPermissionInfo oldVpi, VivoPermissionInfo newVpi) {
        String OldPkg;
        if (oldVpi == null || newVpi == null || (OldPkg = oldVpi.getPackageName()) == null || !OldPkg.equals(newVpi.getPackageName())) {
            return;
        }
        newVpi.updateFrom(oldVpi);
    }

    private void updateForPackageReplaced_l(String packageName, int userId) {
        if (packageName == null) {
            return;
        }
        int totalRemovedAppNum = getRemovedAppList(userId).size();
        int replacedIndex = -1;
        int index = 0;
        while (true) {
            if (index >= totalRemovedAppNum) {
                break;
            }
            VivoPermissionInfo removedVpi = getRemovedAppList(userId).get(index);
            if (!packageName.equals(removedVpi.getPackageName())) {
                index++;
            } else {
                replacedIndex = index;
                break;
            }
        }
        if (-1 == replacedIndex) {
            VivoPermissionService.printfInfo("updateForPackageReplaced_l but mRemovedAppList not have this package:" + packageName);
            return;
        }
        VivoPermissionInfo vpiReplaced = getRemovedAppList(userId).get(replacedIndex);
        getRemovedAppList(userId).remove(replacedIndex);
        VivoPermissionInfo vpi = getPermissionMap(userId).get(packageName);
        if (vpi == null) {
            return;
        }
        removePackageFromList(packageName, userId);
        updatePackagePermission(vpiReplaced, vpi);
        addPackageToList(vpi, userId);
        VivoPermissionService.printfInfo("updateForPackageReplaced_l end (" + packageName + ")-->start saveVPIToDB");
        saveVPIToDB(vpi, true, userId);
    }

    public void updateForPackageReplaced(String packageName, int userId) {
        synchronized (mVPILock) {
            updateForPackageReplaced_l(packageName, userId);
        }
    }

    private void addPackageToList(VivoPermissionInfo vpi, int userId) {
        if (vpi == null) {
            return;
        }
        addVPIToPermissionList(vpi, userId);
        addVPIToPermissionMap(vpi, userId);
        if (vpi.isWhiteListApp()) {
            getTrustedList(userId).add(0, vpi);
        } else {
            getMonitorList(userId).add(0, vpi);
        }
    }

    private void updateForPackageAdded_l(String packageName, boolean grantPermissions, int userId) {
        if (getAppWhiteList(userId).contains(packageName)) {
            VivoPermissionService.printfDebug("updateForPackageAdded(" + packageName + ")WhiteList");
        } else if (getPermissionMap(userId).containsKey(packageName)) {
            VivoPermissionService.printfDebug("updateForPackageAdded(" + packageName + ") is already in mPermissionMap,skip it");
        } else {
            VivoPermissionInfo vpi = parseDefaultPackagePermission(packageName, grantPermissions, userId);
            if (vpi == null) {
                VivoPermissionService.printfInfo("updateForPackageAdded_ls failed! " + packageName);
            } else if (!isNeedDisplay(vpi)) {
            } else {
                addPackageToList(vpi, userId);
                VivoPermissionService.printfInfo("updateForPackageAdded(" + packageName + ")-->start saveVPIToDB");
                saveVPIToDB(vpi, true, userId);
            }
        }
    }

    public void updateForPackageAdded(String packageName, boolean grantPermissions, int userId) {
        synchronized (mVPILock) {
            updateForPackageAdded_l(packageName, grantPermissions, userId);
        }
    }

    private void removePackageFromList(String packageName, int userId) {
        VivoPermissionInfo vpi;
        if (packageName == null || (vpi = getPermissionMap(userId).get(packageName)) == null) {
            return;
        }
        getPermissionMap(userId).remove(packageName);
        for (int index = 0; index < 32; index++) {
            List<VivoPermissionInfo> vpiList = getPermissionList(userId).get(index);
            if (vpiList != null && vpiList.contains(vpi)) {
                vpiList.remove(vpi);
            }
        }
        if (vpi.isWhiteListApp()) {
            getTrustedList(userId).remove(vpi);
        } else {
            getMonitorList(userId).remove(vpi);
        }
    }

    public void updateForPackageRemoved(String packageName, int userId) {
        synchronized (mVPILock) {
            VivoPermissionInfo vpi = getPermissionMap(userId).get(packageName);
            if (vpi == null) {
                return;
            }
            if (getRemovedAppList(userId).size() >= 10) {
                for (int index = 9; index >= 5; index--) {
                    getRemovedAppList(userId).remove(index);
                }
            }
            getRemovedAppList(userId).add(0, vpi);
            removePackageFromList(packageName, userId);
            VivoPermissionService.printfInfo("updateForPackageRemoved(" + packageName + ", userId: " + userId + ")-->start removeVPIFromDB");
            removeVPIFromDB(packageName, true, userId);
        }
    }

    public int checkConfigPermission(String packageName, String permName, int userId) {
        int result;
        VivoPermissionService.printfInfo("checkConfigPermission(" + packageName + "," + permName + ", userId: " + userId);
        if (!this.mIsConfigFinished) {
            VivoPermissionService.printfInfo("mIsConfigFinished=false, just GRANTED!");
            return 1;
        } else if (getAppWhiteList(userId).contains(packageName)) {
            return 1;
        } else {
            PackageInfo pkgInfo = getPackageInfo(packageName, userId);
            if (pkgInfo == null || pkgInfo.applicationInfo == null) {
                return 2;
            }
            int i = pkgInfo.applicationInfo.uid;
            AppOpsManager.permissionToOp(permName);
            long identity = Binder.clearCallingIdentity();
            try {
                handleLegacyAppopsToVivoPermissionInfo(packageName, pkgInfo, permName, userId);
                Binder.restoreCallingIdentity(identity);
                int typeId = VivoPermissionType.getVPType(permName).getVPTypeId();
                VivoPermissionInfo vpi = getAppPermission(packageName, userId);
                if (vpi != null) {
                    int result2 = vpi.getPermissionResult(typeId);
                    if (result2 == 2 && permName.contains(ACCESS_WIFI_STATE_PERMISSION) && packageName.toLowerCase().contains("wifi")) {
                        result = 1;
                    } else {
                        result = result2;
                    }
                    if (typeId == 19 || typeId == 18) {
                        if (DEBUG_CTS_TEST_23) {
                            return 1;
                        }
                        return 3;
                    }
                    return result;
                }
                int packageUid = -1;
                try {
                    packageUid = this.mPackageManager.getPackageUidAsUser(packageName, userId);
                } catch (Exception e) {
                    VivoPermissionService.printfInfo("getPackageUid(" + packageName + ") fail uid = -1");
                    e.printStackTrace();
                }
                VivoPermissionService.printfInfo("checkConfigPermission(" + packageName + "," + permName + ") is VivoPermissionInfo.UNKNOWN uid =" + packageUid);
                return 0;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
                throw th;
            }
        }
    }

    public int checkConfigDeniedMode(String packageName, String permName, int userId) {
        int typeId = VivoPermissionType.getVPType(permName).getVPTypeId();
        VivoPermissionInfo vpi = getAppPermission(packageName, userId);
        if (vpi != null) {
            int result = vpi.getDeniedMode(typeId);
            return result;
        }
        int packageUid = -1;
        try {
            packageUid = this.mPackageManager.getPackageUidAsUser(packageName, userId);
        } catch (Exception e) {
            VivoPermissionService.printfInfo("getPackageUid(" + packageName + ") fail uid = -1");
            e.printStackTrace();
        }
        VivoPermissionService.printfInfo("checkConfigDeniedMode(" + packageName + "," + permName + ") is VivoPermissionInfo.ZERO_TIMES uid =" + packageUid);
        return 32;
    }

    public void setConfigDeniedMode(String packageName, String permName, int deniedMode, int userId) {
        int typeId = VivoPermissionType.getVPType(permName).getVPTypeId();
        synchronized (mVPILock) {
            VivoPermissionInfo vpi = getPermissionMap(userId).get(packageName);
            if (vpi == null) {
                return;
            }
            vpi.setDeniedMode(typeId, deniedMode);
            saveVPIToDB(vpi, true, userId);
        }
    }

    public int checkConfigDeniedDialogMode(String packageName, String permName, int userId) {
        int typeId = VivoPermissionType.getVPType(permName).getVPTypeId();
        VivoPermissionInfo vpi = getAppPermission(packageName, userId);
        if (vpi != null) {
            int result = vpi.getDeniedDialogMode(typeId);
            return result;
        }
        int packageUid = -1;
        try {
            packageUid = this.mPackageManager.getPackageUidAsUser(packageName, userId);
        } catch (Exception e) {
            VivoPermissionService.printfInfo("getPackageUid(" + packageName + ") fail uid = -1");
            e.printStackTrace();
        }
        VivoPermissionService.printfInfo("checkConfigDeniedDialogMode(" + packageName + "," + permName + ") is DENIED_DIALOG_MODE_NO_COUNTDOWN_SETTING uid =" + packageUid);
        return 256;
    }

    public void doForUpdate(final int userId) {
        boolean needDo = "false".equals(SystemProperties.get("persist.vivo.perm.init", "false"));
        VivoPermissionService.printfDebug("do for update + " + needDo);
        if (needDo) {
            this.mHandler.post(new Runnable() { // from class: com.vivo.services.security.server.VivoPermissionConfig.4
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        List<VivoPermissionInfo> allVPIs = VivoPermissionConfig.this.getVPDB(userId).findAllVPIs();
                        if (allVPIs != null) {
                            for (VivoPermissionInfo vpi : allVPIs) {
                                VivoPermissionConfig.this.setNewAppPermission(vpi, userId);
                            }
                            SystemProperties.set("persist.vivo.perm.init", "true");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setNewAppPermission(VivoPermissionInfo vpi, int userId) {
        PackageInfo packageInfo;
        String pkgName = vpi.getPackageName();
        if (pkgName == null || (packageInfo = getPackageInfo(pkgName, userId)) == null || packageInfo.applicationInfo == null || packageInfo.requestedPermissions == null) {
            return;
        }
        boolean supportsRuntimePermissions = packageInfo.applicationInfo.targetSdkVersion > 22;
        if (this.mVPS.needCheckPkg(packageInfo)) {
            for (int index = 0; index < 32; index++) {
                VivoPermissionType type = VivoPermissionType.getVPType(index);
                if (type.getVPCategory() != VivoPermissionType.VivoPermissionCategory.OTHERS && type.getVPGroup() != VivoPermissionType.VivoPermissionGroup.CUSTOM) {
                    setNewAppPermission(packageInfo, index, vpi.getAllPermission(index), supportsRuntimePermissions);
                }
            }
        }
    }

    public boolean isRuntimePermission(String permission) {
        PermissionInfo info;
        return (permission == null || (info = getPermissionInfo(permission)) == null || info.packageName == null || !"android".equals(info.packageName) || (info.protectionLevel & 15) != 1) ? false : true;
    }

    public boolean isRuntimePermission(PermissionInfo info) {
        return info != null && info.packageName != null && "android".equals(info.packageName) && (info.protectionLevel & 15) == 1;
    }

    public PermissionInfo getPermissionInfo(String permission) {
        if (permission != null) {
            try {
                return this.mPackageManager.getPermissionInfo(permission, 0);
            } catch (PackageManager.NameNotFoundException e) {
                return null;
            }
        }
        return null;
    }

    public PackageInfo getPackageInfo(String packageName, int userId) {
        try {
            return this.mPackageManager.getPackageInfoAsUser(packageName, 4096, userId);
        } catch (PackageManager.NameNotFoundException e) {
            VivoPermissionService.printfError("No package: " + packageName + "  error = " + e);
            return null;
        }
    }

    public boolean setOnePermission(String packageName, String perm, int uid, boolean granted) {
        int userId = UserHandle.getUserId(uid);
        PackageInfo packageInfo = getPackageInfo(packageName, userId);
        if (packageInfo == null || packageInfo.applicationInfo == null || !isRuntimePermission(perm)) {
            return false;
        }
        boolean supportsRuntimePermissions = packageInfo.applicationInfo.targetSdkVersion > 22;
        if (!supportsRuntimePermissions) {
            int typeId = VivoPermissionType.getVPType(perm).getVPTypeId();
            String appOp = AppOpsManager.permissionToOp(perm);
            int uidMode = !granted ? 1 : 0;
            if (VivoPermissionType.isValidTypeId(typeId) && VivoPermissionType.getVPType(perm).getVPCategory() != VivoPermissionType.VivoPermissionCategory.OTHERS) {
                saveAppPermission(packageName, typeId, granted ? 1 : 3, userId);
                uidMode = 0;
            }
            AppOpsManager appOpsManager = this.mAppopsManager;
            if (appOpsManager == null || appOp == null) {
                return false;
            }
            appOpsManager.setUidMode(appOp, uid, uidMode);
        }
        return true;
    }

    public boolean setOnePermissionExt(String packageName, String perm, int uid, int result) {
        int userId = UserHandle.getUserId(uid);
        PackageInfo packageInfo = getPackageInfo(packageName, UserHandle.getUserId(uid));
        if (packageInfo == null || packageInfo.applicationInfo == null || !isRuntimePermission(perm)) {
            return false;
        }
        String appOp = AppOpsManager.permissionToOp(perm);
        int uidMode = getUidMode(result);
        boolean supportsRuntimePermissions = packageInfo.applicationInfo.targetSdkVersion > 22;
        if (!supportsRuntimePermissions) {
            int typeId = VivoPermissionType.getVPType(perm).getVPTypeId();
            if (VivoPermissionType.isValidTypeId(typeId) && VivoPermissionType.getVPType(perm).getVPCategory() != VivoPermissionType.VivoPermissionCategory.OTHERS) {
                saveAppPermission(packageName, typeId, result, userId);
            }
        }
        AppOpsManager appOpsManager = this.mAppopsManager;
        if (appOpsManager == null || appOp == null) {
            return false;
        }
        int currentMode = appOpsManager.unsafeCheckOpRaw(appOp, uid, packageName);
        if (currentMode == uidMode) {
            return false;
        }
        this.mAppopsManager.setUidMode(appOp, uid, uidMode);
        return true;
    }

    private int getUidMode(int result) {
        int i = result & 15;
        if (i == 2 || i == 3) {
            return 1;
        }
        if (i == 5) {
            return 4;
        }
        return 0;
    }

    public int checkOnePermission(String packageName, String perm, int uid) {
        boolean isFixed;
        VivoPermissionService.printfQuatitiesLog("checkOnePermission  packageName=" + packageName + " perm=" + perm + " uid=" + uid);
        PackageInfo packageInfo = getPackageInfo(packageName, UserHandle.getUserId(uid));
        if (packageInfo == null || packageInfo.applicationInfo == null || packageInfo.requestedPermissions == null || !isRuntimePermission(perm)) {
            return 1;
        }
        boolean supportsRuntimePermissions = packageInfo.applicationInfo.targetSdkVersion > 22;
        VivoPermissionService.printfQuatitiesLog("checkOnePermission  supportsRuntimePermissions=" + supportsRuntimePermissions);
        if (supportsRuntimePermissions) {
            int permissionCount = packageInfo.requestedPermissions.length;
            for (int i = 0; i < permissionCount; i++) {
                String requestedPermission = packageInfo.requestedPermissions[i];
                if (requestedPermission.equals(perm)) {
                    boolean granted = (packageInfo.requestedPermissionsFlags[i] & 2) != 0;
                    int permissionFlags = this.mPackageManager.getPermissionFlags(requestedPermission, packageName, UserHandle.getUserHandleForUid(uid));
                    isFixed = (permissionFlags & 2) != 0;
                    VivoPermissionService.printfQuatitiesLog("checkOnePermission  granted=" + granted + " permissionFlags=" + permissionFlags + " isFixed=" + isFixed);
                    if (granted) {
                        return 1;
                    }
                    return isFixed ? 2 : 3;
                }
            }
            return 1;
        }
        String appOp = AppOpsManager.permissionToOp(perm);
        boolean appOpAllowed = false;
        boolean appOpForgound = false;
        AppOpsManager appOpsManager = this.mAppopsManager;
        if (appOpsManager != null && appOp != null) {
            int appOpsMode = appOpsManager.unsafeCheckOpRaw(appOp, packageInfo.applicationInfo.uid, packageInfo.packageName);
            appOpAllowed = appOpsMode == 0;
            isFixed = appOpsMode == 4;
            appOpForgound = isFixed;
            VivoPermissionService.printfQuatitiesLog("checkOnePermission appOp=" + appOp + ", appOpsMode=" + appOpsMode);
        }
        if (appOpAllowed) {
            return 1;
        }
        return appOpForgound ? 5 : 3;
    }

    public boolean doForRuntimePermission(String packageName, int vpTypeId, int result, int userId) {
        PackageInfo packageInfo;
        if (packageName == null || (packageInfo = getPackageInfo(packageName, userId)) == null || packageInfo.applicationInfo == null || packageInfo.requestedPermissions == null) {
            return false;
        }
        boolean supportsRuntimePermissions = packageInfo.applicationInfo.targetSdkVersion > 22;
        VivoPermissionService.printfDebug("doForRuntimePermission packageName=" + packageInfo.packageName + " supportsRuntimePermissions=" + supportsRuntimePermissions);
        return setNewAppPermission(packageInfo, vpTypeId, result, supportsRuntimePermissions);
    }

    public void handleGroupPermission(String packageName, VivoPermissionType.VivoPermissionGroup vpg, int result, int userId) {
        if (packageName != null && vpg != VivoPermissionType.VivoPermissionGroup.CUSTOM) {
            PackageInfo packageInfo = getPackageInfo(packageName, userId);
            if (packageInfo != null && packageInfo.applicationInfo != null && packageInfo.requestedPermissions != null) {
                boolean supportsRuntimePermissions = packageInfo.applicationInfo.targetSdkVersion > 22;
                VivoPermissionService.printfDebug("handleGroupPermission packageName=" + packageInfo.packageName + " supportsRuntimePermissions=" + supportsRuntimePermissions);
                int permissionCount = packageInfo.requestedPermissions.length;
                for (int i = 0; i < permissionCount; i++) {
                    String requestedPermission = packageInfo.requestedPermissions[i];
                    if (VivoPermissionService.needHandleGroup(requestedPermission)) {
                        VivoPermissionService.printfQuatitiesLog("handleGroupPermission  requestedPermission =" + requestedPermission + " vpg=" + vpg);
                        if (requestedPermission != null && VivoPermissionUtils.getGroupOfPlatformPermission(requestedPermission) != null && vpg.getValue().equals(VivoPermissionUtils.getGroupOfPlatformPermission(requestedPermission))) {
                            VivoPermissionService.printfQuatitiesLog("handleGroupPermission group = " + VivoPermissionUtils.getGroupOfPlatformPermission(requestedPermission) + "  requestedPermission =" + requestedPermission);
                            if (supportsRuntimePermissions) {
                                setRuntimePermission(packageInfo, packageInfo.requestedPermissionsFlags[i], requestedPermission, result);
                            } else {
                                boolean granted = result == 1;
                                String appOp = AppOpsManager.permissionToOp(requestedPermission);
                                AppOpsManager appOpsManager = this.mAppopsManager;
                                if (appOpsManager != null && appOp != null) {
                                    appOpsManager.setUidMode(appOp, packageInfo.applicationInfo.uid, granted ? 0 : 1);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean setNewAppPermission(PackageInfo packageInfo, int vpTypeId, int result, boolean supportRT) {
        boolean isDone = false;
        VivoPermissionType.VivoPermissionGroup vpg = VivoPermissionType.getVPType(vpTypeId).getVPGroup();
        VivoPermissionService.printfDebug("setNewAppPermission packageName=" + packageInfo.packageName + " vpTypeId=" + vpTypeId + " vpg=" + vpg + " result=" + result);
        int permissionCount = packageInfo.requestedPermissions.length;
        for (int i = 0; i < permissionCount; i++) {
            String requestedPermission = packageInfo.requestedPermissions[i];
            PermissionInfo permInfo = getPermissionInfo(requestedPermission);
            VivoPermissionService.printfQuatitiesLog("setNewAppPermission requestedPermission=" + requestedPermission + " permInfo=" + permInfo);
            if (isRuntimePermission(permInfo)) {
                int typeId = VivoPermissionType.getVPType(requestedPermission).getVPTypeId();
                VivoPermissionService.printfQuatitiesLog("setNewAppPermission typeId = " + typeId + " permInfo.group=" + VivoPermissionUtils.getGroupOfPermission(permInfo) + " supportRT=" + supportRT);
                if (VivoPermissionService.needHandleGroup(requestedPermission) && vpg.getValue().equals(VivoPermissionUtils.getGroupOfPermission(permInfo))) {
                    if (supportRT) {
                        setRuntimePermission(packageInfo, packageInfo.requestedPermissionsFlags[i], requestedPermission, result);
                    } else {
                        setOldAppPermission(packageInfo, requestedPermission, result, true);
                    }
                } else if (vpTypeId == typeId) {
                    if (supportRT) {
                        setRuntimePermission(packageInfo, packageInfo.requestedPermissionsFlags[i], requestedPermission, result);
                        isDone = true;
                    } else {
                        setOldAppPermission(packageInfo, requestedPermission, result, true);
                    }
                } else if (vpTypeId == 12 && requestedPermission.equals("android.permission.ACCESS_BACKGROUND_LOCATION") && supportRT) {
                    setRuntimePermission(packageInfo, packageInfo.requestedPermissionsFlags[i], requestedPermission, (result & 15) != 1 ? 3 : 1);
                    isDone = true;
                }
            }
        }
        return isDone;
    }

    /* JADX WARN: Removed duplicated region for block: B:86:0x018f A[ADDED_TO_REGION] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private void setRuntimePermission(android.content.pm.PackageInfo r20, int r21, java.lang.String r22, int r23) {
        /*
            Method dump skipped, instructions count: 481
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.vivo.services.security.server.VivoPermissionConfig.setRuntimePermission(android.content.pm.PackageInfo, int, java.lang.String, int):void");
    }

    public void grantAllRuntimePermissions(String packageName, boolean enable, int userId) {
        if (packageName != null && enable) {
            PackageInfo packageInfo = getPackageInfo(packageName, userId);
            if (packageInfo != null && packageInfo.applicationInfo != null && packageInfo.requestedPermissions != null) {
                boolean supportsRuntimePermissions = packageInfo.applicationInfo.targetSdkVersion > 22;
                VivoPermissionService.printfDebug("grantAllRuntimePermissions packageName=" + packageInfo.packageName + " supportsRuntimePermissions=" + supportsRuntimePermissions);
                if (supportsRuntimePermissions) {
                    int permissionCount = packageInfo.requestedPermissions.length;
                    for (int i = 0; i < permissionCount; i++) {
                        String requestedPermission = packageInfo.requestedPermissions[i];
                        if (isRuntimePermission(requestedPermission)) {
                            if (isPermissionRestricted(requestedPermission)) {
                                this.mPackageManager.updatePermissionFlags(requestedPermission, packageName, EmergencyBroadcastManager.FLAG_RECEIVER_KEYAPP, EmergencyBroadcastManager.FLAG_RECEIVER_KEYAPP, UserHandle.of(0));
                            }
                            setRuntimePermission(packageInfo, packageInfo.requestedPermissionsFlags[i], requestedPermission, 1);
                        }
                    }
                    return;
                }
                int permissionCount2 = packageInfo.requestedPermissions.length;
                int uid = packageInfo.applicationInfo.uid;
                for (int i2 = 0; i2 < permissionCount2; i2++) {
                    String requestedPermission2 = packageInfo.requestedPermissions[i2];
                    if (isRuntimePermission(requestedPermission2)) {
                        String appOp = AppOpsManager.permissionToOp(requestedPermission2);
                        int permissionFlag = this.mPackageManager.getPermissionFlags(requestedPermission2, packageInfo.packageName, UserHandle.getUserHandleForUid(uid));
                        boolean reviewRequire = (permissionFlag & 64) != 0;
                        AppOpsManager appOpsManager = this.mAppopsManager;
                        if (appOpsManager != null && appOp != null) {
                            boolean appOpAllowed = appOpsManager.unsafeCheckOpNoThrow(appOp, uid, packageInfo.packageName) == 0;
                            if (!appOpAllowed) {
                                this.mAppopsManager.setUidMode(appOp, uid, 0);
                            }
                        }
                        if (reviewRequire) {
                            this.mPackageManager.updatePermissionFlags(requestedPermission2, packageInfo.packageName, 64, 0, UserHandle.getUserHandleForUid(uid));
                        }
                    }
                }
            }
        }
    }

    public void setOldAppPermission(PackageInfo packageInfo, String perm, int result, boolean nedGrant) {
        String appOp = AppOpsManager.permissionToOp(perm);
        VivoPermissionService.printfDebug("setOldAppPermission appOp=" + appOp + "result: " + result);
        int uid = packageInfo.applicationInfo.uid;
        int permissionFlag = this.mPackageManager.getPermissionFlags(perm, packageInfo.packageName, UserHandle.getUserHandleForUid(uid));
        boolean reviewRequire = (permissionFlag & 64) != 0;
        boolean shouldRevokeCompat = (permissionFlag & 8) != 0;
        boolean granted = (result & 15) == 1;
        boolean onlyForground = (result & 15) == 5;
        if (reviewRequire) {
            VivoPermissionService.printfDebug("setOldAppPermission updateflags perm: " + perm);
            this.mPackageManager.updatePermissionFlags(perm, packageInfo.packageName, 64, 0, UserHandle.getUserHandleForUid(uid));
        }
        AppOpsManager appOpsManager = this.mAppopsManager;
        if (appOpsManager != null && appOp != null) {
            boolean appOpAllowed = appOpsManager.checkOpNoThrow(appOp, uid, packageInfo.packageName) == 0;
            boolean appOpForground = this.mAppopsManager.unsafeCheckOpRaw(appOp, uid, packageInfo.packageName) == 4;
            if (granted) {
                if (!appOpAllowed) {
                    this.mAppopsManager.setUidMode(appOp, uid, 0);
                }
                if (shouldRevokeCompat) {
                    this.mPackageManager.updatePermissionFlags(perm, packageInfo.packageName, 8, 0, UserHandle.getUserHandleForUid(uid));
                }
            } else if (onlyForground) {
                int typeId = VivoPermissionType.getVPType(perm).getVPTypeId();
                if (typeId != 12) {
                    VivoPermissionService.printfDebug("GRANTED_FG is used for location permission, but not others");
                    return;
                }
                if (!appOpForground) {
                    VivoPermissionService.printfDebug("begin to set foreground, uid: " + uid + ", appops: " + appOp);
                    this.mAppopsManager.setUidMode(appOp, uid, 4);
                }
                if (shouldRevokeCompat) {
                    this.mPackageManager.updatePermissionFlags(perm, packageInfo.packageName, 8, 0, UserHandle.getUserHandleForUid(uid));
                }
            } else {
                if ((appOpAllowed || appOpForground) && nedGrant) {
                    this.mAppopsManager.setUidMode(appOp, uid, 1);
                }
                if (!shouldRevokeCompat) {
                    this.mPackageManager.updatePermissionFlags(perm, packageInfo.packageName, 8, 8, UserHandle.getUserHandleForUid(uid));
                }
            }
        } else if ("android.permission.ACCESS_BACKGROUND_LOCATION".equals(perm)) {
            if (granted) {
                if (shouldRevokeCompat) {
                    this.mPackageManager.updatePermissionFlags(perm, packageInfo.packageName, 8, 0, UserHandle.getUserHandleForUid(uid));
                }
            } else if (!shouldRevokeCompat) {
                this.mPackageManager.updatePermissionFlags(perm, packageInfo.packageName, 8, 8, UserHandle.getUserHandleForUid(uid));
            }
        }
    }

    private void handleVpiList(List<VivoPermissionInfo> vpis, boolean nedfixed, int userId) {
        if (vpis == null) {
            return;
        }
        for (VivoPermissionInfo vpi : vpis) {
            long identity = Binder.clearCallingIdentity();
            try {
                handleRuntimePermission(vpi.getPackageName(), nedfixed, userId);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    private boolean isPermissionRestricted(String name) {
        try {
            return this.mPackageManager.getPermissionInfo(name, 0).isRestricted();
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private SparseArray<ArrayList<VivoPermissionInfo>> getPermissionList(int userId) {
        synchronized (this.mPermissionLists) {
            if (this.mPermissionLists.get(userId) == null) {
                SparseArray<ArrayList<VivoPermissionInfo>> permissionList = new SparseArray<>(32);
                this.mPermissionLists.put(userId, permissionList);
                return permissionList;
            }
            return this.mPermissionLists.get(userId);
        }
    }

    private List<VivoPermissionInfo> getTrustedList(int userId) {
        synchronized (this.mTrustedAppLists) {
            if (this.mTrustedAppLists.get(userId) == null) {
                List<VivoPermissionInfo> list = new ArrayList<>();
                this.mTrustedAppLists.put(userId, list);
                return list;
            }
            return this.mTrustedAppLists.get(userId);
        }
    }

    private List<VivoPermissionInfo> getMonitorList(int userId) {
        synchronized (this.mMonitorAppLists) {
            if (this.mMonitorAppLists.get(userId) == null) {
                List<VivoPermissionInfo> list = new ArrayList<>();
                this.mMonitorAppLists.put(userId, list);
                return list;
            }
            return this.mMonitorAppLists.get(userId);
        }
    }

    private HashMap<String, VivoPermissionInfo> getPermissionMap(int userId) {
        synchronized (this.mPermissionMaps) {
            if (this.mPermissionMaps.get(userId) == null) {
                HashMap<String, VivoPermissionInfo> map = new HashMap<>();
                this.mPermissionMaps.put(userId, map);
                return map;
            }
            return this.mPermissionMaps.get(userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public VivoPermissionDataBase getVPDB(int userId) {
        synchronized (this.mVPDBs) {
            if (this.mVPDBs.get(userId) == null) {
                VivoPermissionDataBase vpdb = new VivoPermissionDataBase(this.mContext, userId);
                this.mVPDBs.put(userId, vpdb);
                return vpdb;
            }
            return this.mVPDBs.get(userId);
        }
    }

    private HashMap<String, String> getBuiltInThirdPartMap(int userId) {
        synchronized (this.mBuiltInThirdPartMaps) {
            if (this.mBuiltInThirdPartMaps.get(userId) == null) {
                HashMap<String, String> map = new HashMap<>();
                this.mBuiltInThirdPartMaps.put(userId, map);
                return map;
            }
            return this.mBuiltInThirdPartMaps.get(userId);
        }
    }

    private ArrayList<VivoPermissionInfo> getRemovedAppList(int userId) {
        synchronized (this.mRemovedAppLists) {
            if (this.mRemovedAppLists.get(userId) == null) {
                ArrayList<VivoPermissionInfo> list = new ArrayList<>();
                this.mRemovedAppLists.put(userId, list);
                return list;
            }
            return this.mRemovedAppLists.get(userId);
        }
    }

    private ArrayList<String> getAppWhiteList(int userId) {
        synchronized (this.mAppWhiteLists) {
            if (this.mAppWhiteLists.get(userId) == null) {
                ArrayList<String> list = new ArrayList<>();
                this.mAppWhiteLists.put(userId, list);
                return list;
            }
            return this.mAppWhiteLists.get(userId);
        }
    }
}