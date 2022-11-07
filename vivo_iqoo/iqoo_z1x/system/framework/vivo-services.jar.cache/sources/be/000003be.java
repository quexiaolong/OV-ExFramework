package com.android.server.pm.permission;

import android.app.AppGlobals;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.PackageParser;
import android.os.Binder;
import android.os.Environment;
import android.os.FtBuild;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManagerInternal;
import android.os.storage.StorageManagerInternal;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Xml;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.XmlUtils;
import com.android.server.LocalServices;
import com.android.server.VivoDoubleInstanceServiceImpl;
import com.android.server.am.EmergencyBroadcastManager;
import com.android.server.pm.PackageManagerServiceUtils;
import com.android.server.pm.PackageSetting;
import com.android.server.pm.UserManagerService;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.permission.PermissionManagerServiceInternal;
import com.android.server.policy.SoftRestrictedPermissionPolicy;
import com.vivo.common.utils.VLog;
import com.vivo.services.rms.GameOptManager;
import com.vivo.services.security.server.VivoPermissionUtils;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import vivo.app.VivoFrameworkFactory;
import vivo.app.vgc.AbsVivoVgcManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoPermissionImpl implements IVivoPermission {
    private static final boolean DEBUG_GOOPER = "1".equals(SystemProperties.get("persist.debug.gooper1", "0"));
    private static final boolean DEBUG_GOOPER_LOG = "1".equals(SystemProperties.get("persist.debug.gooper2", "0"));
    public static final int DEFAULT_PACKAGE_INFO_QUERY_FLAGS = 536915968;
    static final String TAG = "VivoPermManagerImpl";
    public static ArrayList<String> mNotWhiteListPreGrantPkg;
    public static ArrayList<String> mSpecialDeletedSysPkgs;
    private static ArrayList<String> mSpecialInputMethodPkgs;
    private final Context mContext;
    private final PermissionManagerService mPermManagerService;
    private final PermissionSettings mSettings;
    private VivoDoubleInstanceServiceImpl mVivoDoubleInstanceService;
    private AbsVivoVgcManager vivoVgcManager;
    private ArrayMap<String, List<DefaultPermissionGrant>> mGrantPkgs = null;
    private Map<String, Set<String>> systemFixedPkgs = new ArrayMap();
    private final PackageManagerInternal mPackageManagerInt = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
    private final UserManagerInternal mUserManagerInt = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);

    static {
        ArrayList<String> arrayList = new ArrayList<>();
        mSpecialInputMethodPkgs = arrayList;
        arrayList.add("com.baidu.input_vivo");
        mSpecialInputMethodPkgs.add("com.sohu.inputmethod.sogou.vivo");
        mSpecialInputMethodPkgs.add("com.kikaoem.vivo.qisiemoji.inputmethod");
        mSpecialInputMethodPkgs.add("com.google.android.inputmethod.latin");
        mSpecialInputMethodPkgs.add("com.emoji.keyboard.touchpal.vivo");
        mSpecialInputMethodPkgs.add("com.baidu.input_bbk.service");
        ArrayList<String> arrayList2 = new ArrayList<>();
        mSpecialDeletedSysPkgs = arrayList2;
        arrayList2.add("com.vivo.game");
        ArrayList<String> arrayList3 = new ArrayList<>();
        mNotWhiteListPreGrantPkg = arrayList3;
        arrayList3.add("com.chaozh.iReader");
        mNotWhiteListPreGrantPkg.add("com.qualcomm.qti.simcontacts");
        mNotWhiteListPreGrantPkg.add("com.qti.confuridialer");
        mNotWhiteListPreGrantPkg.add("com.qualcomm.qti.callenhancement");
        mNotWhiteListPreGrantPkg.add("com.qualcomm.qti.callfeaturessetting");
        mNotWhiteListPreGrantPkg.add("com.android.vending");
        mNotWhiteListPreGrantPkg.add("com.google.android.marvin.talkback");
    }

    public VivoPermissionImpl(PermissionManagerService permManagerService, Context context, PermissionSettings settings) {
        this.mVivoDoubleInstanceService = null;
        this.vivoVgcManager = null;
        this.mPermManagerService = permManagerService;
        this.mContext = context;
        this.mSettings = settings;
        this.mVivoDoubleInstanceService = VivoDoubleInstanceServiceImpl.getInstance();
        if (VivoFrameworkFactory.getFrameworkFactoryImpl() != null) {
            this.vivoVgcManager = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoVgcManager();
        }
    }

    public void grantRtpermission(String packageName, String permName, int userId, boolean doGrant, PermissionManagerServiceInternal.PermissionCallback callback) {
        BasePermission bp;
        BasePermission bp2;
        if (!this.mUserManagerInt.exists(userId)) {
            VSlog.e(TAG, "No such user:" + userId);
            return;
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.GRANT_RUNTIME_PERMISSIONS", "grantRuntimePermission");
        if (doGrant) {
            AndroidPackage pkg = this.mPackageManagerInt.getPackage(packageName);
            PackageSetting ps = this.mPackageManagerInt.getPackageSetting(packageName);
            if (pkg == null || ps == null) {
                throw new IllegalArgumentException("Unknown package: " + packageName);
            }
            synchronized (this.mPermManagerService.mLock) {
                bp2 = this.mSettings.getPermissionLocked(permName);
            }
            if (bp2 == null) {
                throw new IllegalArgumentException("Unknown permission: " + permName);
            }
            bp2.enforceDeclaredUsedAndRuntimeOrDevelopment(pkg, ps);
            if (pkg.getTargetSdkVersion() < 23 && bp2.isRuntime()) {
                return;
            }
            int uid = UserHandle.getUid(userId, pkg.getUid());
            PermissionsState permissionsState = ps.getPermissionsState();
            int flags = permissionsState.getPermissionFlags(permName, userId);
            if ((flags & 16) != 0) {
                throw new SecurityException("Cannot grant system fixed permission " + permName + " for package " + packageName);
            } else if (pkg.getTargetSdkVersion() < 23) {
                VSlog.w(TAG, "Cannot grant runtime permission to a legacy app");
                return;
            } else if (bp2.isHardRestricted() && (flags & 14336) == 0) {
                VSlog.e(TAG, "Cannot grant hard restricted non-exempt permission " + permName + " for package " + packageName);
                return;
            } else if (bp2.isDevelopment()) {
                if (permissionsState.grantInstallPermission(bp2) != -1 && callback != null) {
                    callback.onInstallPermissionGranted();
                    return;
                }
                return;
            } else if (bp2.isSoftRestricted() && !SoftRestrictedPermissionPolicy.forPermission(this.mContext, pkg.toAppInfoWithoutState(), pkg, UserHandle.of(userId), permName).mayGrantPermission()) {
                VSlog.e(TAG, "Cannot grant soft restricted permission " + permName + " for package " + packageName);
                return;
            } else {
                int result = permissionsState.grantRuntimePermission(bp2, userId);
                if (result == -1) {
                    return;
                }
                if (result == 1 && callback != null) {
                    callback.onGidsChanged(UserHandle.getAppId(pkg.getUid()), userId);
                }
                if (callback != null) {
                    callback.onPermissionGranted(uid, userId);
                    return;
                }
                return;
            }
        }
        AndroidPackage pkg2 = this.mPackageManagerInt.getPackage(packageName);
        PackageSetting ps2 = this.mPackageManagerInt.getPackageSetting(packageName);
        if (pkg2 == null || ps2 == null) {
            throw new IllegalArgumentException("Unknown package: " + packageName);
        }
        synchronized (this.mPermManagerService.mLock) {
            bp = this.mSettings.getPermissionLocked(permName);
        }
        if (bp == null) {
            throw new IllegalArgumentException("Unknown permission: " + permName);
        }
        bp.enforceDeclaredUsedAndRuntimeOrDevelopment(pkg2, ps2);
        int uid2 = UserHandle.getUid(userId, pkg2.getUid());
        if ("android.permission.READ_EXTERNAL_STORAGE".equals(permName) || "android.permission.WRITE_EXTERNAL_STORAGE".equals(permName)) {
            long token = Binder.clearCallingIdentity();
            try {
                if (this.mUserManagerInt.isUserInitialized(userId)) {
                    StorageManagerInternal storageManagerInternal = (StorageManagerInternal) LocalServices.getService(StorageManagerInternal.class);
                    storageManagerInternal.onExternalStoragePolicyChanged(uid2, packageName);
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    public boolean grantSignaturePermissionForSpecialPackage(boolean allowed, AndroidPackage pkg, String perm) {
        if (!allowed && pkg.isSystem() && "com.vivo.space".equals(pkg.getPackageName())) {
            allowed = true;
        }
        if (!allowed && "android.permission.REQUEST_INSTALL_PACKAGES".equals(perm) && !FtBuild.isOverSeas() && isVivoPackage(pkg)) {
            allowed = true;
        }
        if (!allowed && "android.permission.MANAGE_ACTIVITY_STACKS".equals(perm) && isBuildInInputMethods(pkg)) {
            allowed = true;
        }
        if (!allowed && "android.permission.CAPTURE_AUDIO_OUTPUT".equals(perm)) {
            if (FtBuild.isOverSeas() || pkg == null) {
                return false;
            }
            if ("com.hpplay.happycast".equals(pkg.getPackageName())) {
                StringBuilder sb = new StringBuilder(1400);
                sb.append("3082027b308201e4a003020102020453247df7300d06092a864886f70d0101050500308181310f300d06035");
                sb.append("504060c06e4b8ade59bbd310f300d06035504080c06e5b9bfe4b89c310f300d06035504070c06e6b7b1e59c");
                sb.append("b3310f300d060355040a0c06e5ae9de5ae8931273025060355040b0c1ee6b7b1e59cb3e4b990e692ade7a79");
                sb.append("1e68a80e69c89e99990e585ace58fb83112301006035504030c09e99988e994a1e58d8e301e170d31343033");
                sb.append("31353136323131315a170d3434303330373136323131315a308181310f300d06035504060c06e4b8ade59bb");
                sb.append("d310f300d06035504080c06e5b9bfe4b89c310f300d06035504070c06e6b7b1e59cb3310f300d060355040a");
                sb.append("0c06e5ae9de5ae8931273025060355040b0c1ee6b7b1e59cb3e4b990e692ade7a791e68a80e69c89e99990e");
                sb.append("585ace58fb83112301006035504030c09e99988e994a1e58d8e30819f300d06092a864886f70d0101010500");
                sb.append("03818d0030818902818100897f7b7f693b583d17f208f6b69a8cd5b7f8eb44e4b8a4fcb1cea671e8d2af0ec");
                sb.append("fd49625f3326aa83f7b46ff27bb0dc46a988e98281c1788ec1356818dd7ac5ee84527d80175c012c42eea4c");
                sb.append("a55a67214ee8e3fe81a21096a04bb45e58e359ab0e9bc31a1bf90d7f3b31a9fee52606349ca4e2d8b6cbe63");
                sb.append("17e8c6e78954089990203010001300d06092a864886f70d0101050500038181007046333c519f87b92f5e4c");
                sb.append("40668e10eb08ef557eef22a366e90892a666462c09932a4e517e2ae09ac275a4318142c3689f68e564b40e6");
                sb.append("01e97cb07e75e3a3ba438f36802298d2b686b9475db4074858be17f4dbaf0d0f70c9098ad51dca99ec3a110");
                sb.append("4317c75a3fcc65a29be79b1d78f2c12d0f0c0dbe874631e95bb6e6575842");
                return TextUtils.equals(pkg.getSigningDetails().signatures[0].toCharsString(), sb.toString());
            }
        }
        if (!allowed && "android.permission.INTERNAL_SYSTEM_WINDOW".equals(perm) && pkg.isSystem() && "com.vivo.iotserver".equals(pkg.getPackageName())) {
            return true;
        }
        return allowed;
    }

    public boolean grantPermissionByDefault(AndroidPackage pkg, PermissionsState origPermissions, PermissionsState permissionsState, BasePermission bp, int userId) {
        if (pkg == null || origPermissions == null || permissionsState == null || bp == null || pkg == null || permissionsState == null || bp == null || permissionsState.hasRuntimePermission(bp.name, userId) || !isNeedGrantPermissionByDefault(pkg, userId) || !origPermissions.hasRequestedPermission(bp.name) || isFixedOrUserSet(origPermissions.getPermissionFlags(bp.name, userId))) {
            return false;
        }
        VSlog.v(TAG, "grant by default: pkg: " + pkg.getPackageName() + ", perm: " + bp.name + ", userId: " + userId);
        return permissionsState.grantRuntimePermission(bp, userId) != -1;
    }

    public boolean isNeedGrantPermissionByDefault(AndroidPackage pkg, int userId) {
        if (pkg == null || FtBuild.isOverSeas() || pkg == null || TextUtils.isEmpty(pkg.getPackageName()) || UserHandle.getAppId(pkg.getUid()) >= 10000) {
            return false;
        }
        return true;
    }

    public int updatePermissionFlags(boolean wasGrantedByDefault, boolean isSystemApp, BasePermission bp, int flags, int userId) {
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl;
        if ((isSystemApp || ((vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService) != null && userId == vivoDoubleInstanceServiceImpl.getDoubleAppUserId())) && bp.isHardOrSoftRestricted()) {
            if ((flags & 14336) == 0) {
                flags |= EmergencyBroadcastManager.FLAG_RECEIVER_KEYAPP;
            }
            if ((flags & 16384) != 0) {
                flags &= -16385;
            }
        }
        if (wasGrantedByDefault) {
            return flags | 32;
        }
        return flags;
    }

    private boolean isFixedOrUserSet(int flags) {
        return (flags & 23) != 0;
    }

    public boolean checkPermissionForDoubleInstance(String permName, int userId) {
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl != null && vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable() && this.mVivoDoubleInstanceService.getDoubleAppUserId() != -10000) {
            if (userId == 0 || 999 == userId) {
                if ("android.permission.INTERACT_ACROSS_USERS".equals(permName) || "android.permission.INTERACT_ACROSS_USERS_FULL".equals(permName)) {
                    return true;
                }
                return false;
            }
            return false;
        }
        return false;
    }

    public boolean checkUidPermissionForDoubleInstance(int uid, String permName, int userId) {
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl == null || !vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable() || this.mVivoDoubleInstanceService.getDoubleAppUserId() == -10000 || !((userId == 0 || 999 == userId) && ("android.permission.INTERACT_ACROSS_USERS".equals(permName) || "android.permission.INTERACT_ACROSS_USERS_FULL".equals(permName)))) {
            return false;
        }
        return (userId == 0 && "us.zoom.videomeetings".equals(this.mPackageManagerInt.getNameForUid(uid))) ? false : true;
    }

    public int handleUserIdByVivo(int uid, String pkg) {
        return uid;
    }

    public void dummy() {
    }

    public boolean grantRuntimePermissionWithoutNotify(String packageName, String permName, int userId, PermissionManagerServiceInternal.PermissionCallback callback) {
        BasePermission bp;
        if (!this.mUserManagerInt.exists(userId)) {
            VSlog.e(TAG, "No such user:" + userId);
            return false;
        } else if (Binder.getCallingUid() != 1000) {
            VSlog.e(TAG, "No system uid cannot call this method");
            return false;
        } else {
            AndroidPackage pkg = this.mPackageManagerInt.getPackage(packageName);
            PackageSetting ps = this.mPackageManagerInt.getPackageSetting(packageName);
            if (pkg == null || ps == null) {
                VSlog.e(TAG, "Unknown package: " + packageName);
                return false;
            }
            synchronized (this.mPermManagerService.mLock) {
                bp = this.mSettings.getPermissionLocked(permName);
            }
            if (bp == null) {
                VSlog.e(TAG, "Unknown permission: " + permName);
                return false;
            } else if (this.mPackageManagerInt.filterAppAccess(pkg, Binder.getCallingUid(), userId)) {
                VSlog.e(TAG, "Unknown package: " + packageName);
                return false;
            } else if (pkg.getTargetSdkVersion() >= 23 || !bp.isRuntime()) {
                int uid = UserHandle.getUid(userId, pkg.getUid());
                PermissionsState permissionsState = ps.getPermissionsState();
                int flags = permissionsState.getPermissionFlags(permName, userId);
                if ((flags & 16) != 0) {
                    VSlog.e(TAG, "Cannot grant system fixed permission " + permName + " for package " + packageName);
                    return false;
                } else if ((flags & 4) != 0) {
                    VSlog.e(TAG, "Cannot grant policy fixed permission " + permName + " for package " + packageName);
                    return false;
                } else if (bp.isHardRestricted() && (flags & 14336) == 0) {
                    VSlog.e(TAG, "Cannot grant hard restricted non-exempt permission " + permName + " for package " + packageName);
                    return false;
                } else if (bp.isSoftRestricted() && !SoftRestrictedPermissionPolicy.forPermission(this.mContext, pkg.toAppInfoWithoutState(), pkg, UserHandle.of(userId), permName).mayGrantPermission()) {
                    VSlog.e(TAG, "Cannot grant soft restricted permission " + permName + " for package " + packageName);
                    return false;
                } else if (bp.isDevelopment()) {
                    if (permissionsState.grantInstallPermission(bp) != -1 && callback != null) {
                        callback.onInstallPermissionGranted();
                    }
                    return false;
                } else if (ps.getInstantApp(userId) && !bp.isInstant()) {
                    VSlog.e(TAG, "Cannot grant non-ephemeral permission" + permName + " for package " + packageName);
                    return false;
                } else if (pkg.getTargetSdkVersion() < 23) {
                    VSlog.w(TAG, "Cannot grant runtime permission to a legacy app");
                    return false;
                } else {
                    int result = permissionsState.grantRuntimePermission(bp, userId);
                    if (result != -1) {
                        if (result == 1 && callback != null) {
                            callback.onGidsChanged(UserHandle.getAppId(pkg.getUid()), userId);
                        }
                        if (callback != null) {
                            callback.onPermissionGranted(uid, userId);
                        }
                        if ("android.permission.READ_EXTERNAL_STORAGE".equals(permName) || "android.permission.WRITE_EXTERNAL_STORAGE".equals(permName)) {
                            long token = Binder.clearCallingIdentity();
                            try {
                                if (this.mUserManagerInt.isUserInitialized(userId)) {
                                    StorageManagerInternal storageManagerInternal = (StorageManagerInternal) LocalServices.getService(StorageManagerInternal.class);
                                    storageManagerInternal.onExternalStoragePolicyChanged(uid, packageName);
                                }
                            } finally {
                                Binder.restoreCallingIdentity(token);
                            }
                        }
                        return true;
                    }
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    public boolean updatePermissionFlagsWithoutNotify(String permName, String packageName, int flagMask, int flagValues, int userId, PermissionManagerServiceInternal.PermissionCallback callback) {
        BasePermission bp;
        if (this.mUserManagerInt.exists(userId)) {
            if (Binder.getCallingUid() != 1000) {
                VSlog.e(TAG, "No system uid cannot call this method");
                return false;
            } else if ((flagMask & 4) != 0) {
                VSlog.e(TAG, "updatePermissionFlags requires android.permission.ADJUST_RUNTIME_PERMISSIONS_POLICY");
                return false;
            } else {
                AndroidPackage pkg = this.mPackageManagerInt.getPackage(packageName);
                PackageSetting ps = this.mPackageManagerInt.getPackageSetting(packageName);
                if (pkg == null || ps == null) {
                    VSlog.e(TAG, "Unknown package: " + packageName);
                    return false;
                } else if (this.mPackageManagerInt.filterAppAccess(pkg, 1000, userId)) {
                    VSlog.e(TAG, "Unknown package: " + packageName);
                    return false;
                } else {
                    synchronized (this.mPermManagerService.mLock) {
                        bp = this.mSettings.getPermissionLocked(permName);
                    }
                    if (bp == null) {
                        VSlog.e(TAG, "Unknown permission: " + permName);
                        return false;
                    }
                    PermissionsState permissionsState = ps.getPermissionsState();
                    boolean hadState = permissionsState.getRuntimePermissionState(permName, userId) != null;
                    boolean permissionUpdated = permissionsState.updatePermissionFlags(bp, userId, flagMask, flagValues);
                    if (permissionUpdated && callback != null) {
                        if (permissionsState.getInstallPermissionState(permName) != null) {
                            callback.onInstallPermissionUpdated();
                        } else if (permissionsState.getRuntimePermissionState(permName, userId) != null || hadState) {
                            callback.onPermissionUpdated(new int[]{userId}, false);
                        }
                    }
                    return permissionUpdated && bp.isRuntime();
                }
            }
        }
        return false;
    }

    public void grantTheRestPermissionsToApps(int userId, boolean onlySystem) {
        String[] strArr;
        int callingUid = Binder.getCallingUid();
        if (callingUid != 1000) {
            VSlog.e(TAG, "grantTheRestPermissions can only be run by the system");
            return;
        }
        VSlog.i(TAG, "Granting app permissions for user " + userId + " onlySystem " + onlySystem);
        List<PackageInfo> packages = this.mContext.getPackageManager().getInstalledPackagesAsUser(DEFAULT_PACKAGE_INFO_QUERY_FLAGS, 0);
        for (PackageInfo pkg : packages) {
            if (pkg != null) {
                if (!doesPackageSupportRuntimePermissions(pkg) || ArrayUtils.isEmpty(pkg.requestedPermissions) || isSysComponentOrPersistentPlatformSignedPrivApp(pkg) || (onlySystem && !pkg.applicationInfo.isSystemApp())) {
                    if (DEBUG_GOOPER_LOG) {
                        VLog.i(TAG, "Skip package " + pkg.packageName);
                    }
                } else {
                    Set<String> permissions = new ArraySet<>();
                    for (String permission : pkg.requestedPermissions) {
                        BasePermission bp = this.mPermManagerService.getPermission(permission);
                        if (bp != null && bp.isRuntime()) {
                            permissions.add(permission);
                        }
                    }
                    if (!permissions.isEmpty()) {
                        grantRuntimePermissionsLPwRest(pkg, permissions, false, false, true, userId);
                    }
                }
            }
        }
    }

    private void grantRuntimePermissionsLPwRest(PackageInfo pkg, Set<String> permissionsWithoutSplits, boolean systemFixed, boolean overrideUserChoice, boolean whitelistRestrictedPermissions, int userId) {
        boolean wasChanged = grantRuntimePermissions(pkg, permissionsWithoutSplits, systemFixed, overrideUserChoice, whitelistRestrictedPermissions, true, userId);
        if (wasChanged) {
            this.mPermManagerService.notifyRuntimePermissionStateChangedForSystemPkg(pkg.packageName, userId);
        }
    }

    public void grantThePermissionsToAppFromXml(int userId) {
        if (FtBuild.isOverSeas()) {
            return;
        }
        VSlog.i(TAG, "Granting app permissions for user " + userId + " from xml");
        if (this.mGrantPkgs == null) {
            this.mGrantPkgs = readDefaultPermissionGrantsLocked();
        }
        grantRuntimePermissionsToApp(this.mGrantPkgs, userId);
    }

    private void grantRuntimePermissionsToApp(ArrayMap<String, List<DefaultPermissionGrant>> grantPkgs, int userId) {
        Set<String> permissions;
        int j;
        int size;
        int pkgsCount = grantPkgs.size();
        Set<String> permissions2 = null;
        boolean isFirstBoot = isFirstBoot();
        for (int i = 0; i < pkgsCount; i++) {
            String packageName = grantPkgs.keyAt(i);
            PackageInfo pkg = getPackageInfo(packageName);
            if (pkg != null && !ArrayUtils.isEmpty(pkg.requestedPermissions) && (isFirstBoot || !mSpecialDeletedSysPkgs.contains(packageName))) {
                List<DefaultPermissionGrant> grantPerms = grantPkgs.valueAt(i);
                int size2 = grantPerms.size();
                boolean wasChange = false;
                int j2 = 0;
                while (j2 < size2) {
                    DefaultPermissionGrant permissionGrant = grantPerms.get(j2);
                    BasePermission bp = this.mPermManagerService.getPermission(permissionGrant.perm);
                    if (bp == null) {
                        j = j2;
                        size = size2;
                    } else {
                        if (permissions2 == null) {
                            Set<String> permissions3 = new ArraySet<>();
                            permissions = permissions3;
                        } else {
                            permissions2.clear();
                            permissions = permissions2;
                        }
                        if (!bp.isRuntime() || !ArrayUtils.contains(pkg.requestedPermissions, permissionGrant.perm)) {
                            j = j2;
                            size = size2;
                            permissions2 = permissions;
                        } else {
                            permissions.add(permissionGrant.perm);
                            j = j2;
                            size = size2;
                            wasChange |= grantRuntimePermissions(pkg, permissions, permissionGrant.fixed, permissionGrant.overrideUserChoice, true, false, userId);
                            permissions2 = permissions;
                        }
                    }
                    j2 = j + 1;
                    size2 = size;
                }
                if (wasChange) {
                    this.mPermManagerService.notifyRuntimePermissionStateChangedForSystemPkg(pkg.packageName, userId);
                }
            }
        }
    }

    private ArrayMap<String, List<DefaultPermissionGrant>> readDefaultPermissionGrantsLocked() {
        File dir = new File(Environment.getRootDirectory(), "etc/vivo-default-permissions/");
        File[] files = null;
        if (dir.isDirectory() && dir.canRead()) {
            files = dir.listFiles();
        }
        if (files == null) {
            return new ArrayMap<>(0);
        }
        ArrayMap<String, List<DefaultPermissionGrant>> grantPkgs = new ArrayMap<>();
        for (File file : files) {
            if (!file.getPath().endsWith(".xml")) {
                VSlog.i(TAG, "Non-xml file " + file + " in " + file.getParent() + " directory, ignoring");
            } else if (file.canRead()) {
                try {
                    InputStream str = new BufferedInputStream(new FileInputStream(file));
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setInput(str, null);
                    parse(parser, grantPkgs);
                    str.close();
                } catch (IOException | XmlPullParserException e) {
                    VSlog.w(TAG, "Error reading default permissions file " + file, e);
                }
            } else {
                VSlog.w(TAG, "Default permissions file " + file + " cannot be read");
            }
        }
        return grantPkgs;
    }

    private boolean isFirstBoot() {
        IPackageManager pm = AppGlobals.getPackageManager();
        boolean isFirstBoot = false;
        try {
            isFirstBoot = pm.isFirstBoot();
            VSlog.d(TAG, "isFirstBoot == " + isFirstBoot);
            return isFirstBoot;
        } catch (RemoteException e) {
            VSlog.e(TAG, e.getMessage(), e);
            return isFirstBoot;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class DefaultPermissionGrant {
        final boolean fixed;
        final boolean overrideUserChoice;
        final String perm;

        public DefaultPermissionGrant(String perm, boolean fixed, boolean overrideUserChoice) {
            this.perm = perm;
            this.fixed = fixed;
            this.overrideUserChoice = overrideUserChoice;
        }
    }

    private void parse(XmlPullParser parser, Map<String, List<DefaultPermissionGrant>> outGrantPkgs) throws IOException, XmlPullParserException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type != 1) {
                if (type != 3 || parser.getDepth() > outerDepth) {
                    if (type != 3 && type != 4) {
                        if (TextUtils.equals("grants", parser.getName())) {
                            parsegrants(parser, outGrantPkgs);
                        } else {
                            VSlog.e(TAG, "Unknown tag " + parser.getName());
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

    private void parsegrants(XmlPullParser parser, Map<String, List<DefaultPermissionGrant>> outGrantPkgs) throws IOException, XmlPullParserException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type != 1) {
                if (type != 3 || parser.getDepth() > outerDepth) {
                    if (type != 3 && type != 4) {
                        if (TextUtils.equals("grant", parser.getName())) {
                            String packageName = parser.getAttributeValue(null, "package");
                            List<DefaultPermissionGrant> packagePerms = outGrantPkgs.get(packageName);
                            if (packagePerms == null) {
                                PackageInfo packageInfo = getSystemPackageInfo(packageName);
                                if (packageInfo == null) {
                                    VSlog.w(TAG, "No such package:" + packageName);
                                    XmlUtils.skipCurrentTag(parser);
                                } else if (!doesPackageSupportRuntimePermissions(packageInfo)) {
                                    VSlog.w(TAG, "Skipping non supporting runtime permissions package:" + packageName);
                                    XmlUtils.skipCurrentTag(parser);
                                } else {
                                    packagePerms = new ArrayList();
                                    outGrantPkgs.put(packageName, packagePerms);
                                }
                            }
                            parsePermission(parser, packagePerms);
                        } else {
                            VSlog.e(TAG, "Unknown tag " + parser.getName() + "under <grants>");
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

    private void parsePermission(XmlPullParser parser, List<DefaultPermissionGrant> perms) throws IOException, XmlPullParserException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type != 1) {
                if (type != 3 || parser.getDepth() > outerDepth) {
                    if (type != 3 && type != 4) {
                        if (TextUtils.equals("permission", parser.getName())) {
                            String perm = parser.getAttributeValue(null, "name");
                            if (perm == null) {
                                VSlog.w(TAG, "Mandatory name attribute missing for permission tag");
                                XmlUtils.skipCurrentTag(parser);
                            } else {
                                boolean fixed = XmlUtils.readBooleanAttribute(parser, "fixed");
                                boolean overrideUserChoice = XmlUtils.readBooleanAttribute(parser, "overrideUserChoice");
                                DefaultPermissionGrant defaultPermissionGrant = new DefaultPermissionGrant(perm, fixed, overrideUserChoice);
                                perms.add(defaultPermissionGrant);
                            }
                        } else {
                            VSlog.e(TAG, "Unknown tag " + parser.getName() + "under <grant>");
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

    private PackageInfo getPackageInfo(String pkg) {
        return getPackageInfo(pkg, 0);
    }

    private PackageInfo getPackageInfo(String pkg, int flags) {
        if (pkg == null) {
            return null;
        }
        try {
            return this.mContext.getPackageManager().getPackageInfo(pkg, 536915968 | flags);
        } catch (PackageManager.NameNotFoundException e) {
            VSlog.e(TAG, "PackageNot found: " + pkg);
            return null;
        }
    }

    private static boolean doesPackageSupportRuntimePermissions(PackageInfo pkg) {
        return pkg.applicationInfo != null && pkg.applicationInfo.targetSdkVersion > 22;
    }

    /* JADX WARN: Removed duplicated region for block: B:49:0x0100  */
    /* JADX WARN: Removed duplicated region for block: B:56:0x0122  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private boolean grantRuntimePermissions(android.content.pm.PackageInfo r34, java.util.Set<java.lang.String> r35, boolean r36, boolean r37, boolean r38, boolean r39, int r40) {
        /*
            Method dump skipped, instructions count: 872
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.permission.VivoPermissionImpl.grantRuntimePermissions(android.content.pm.PackageInfo, java.util.Set, boolean, boolean, boolean, boolean, int):boolean");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ String[] lambda$grantRuntimePermissions$0(int x$0) {
        return new String[x$0];
    }

    private String getBackgroundPermission(String permission) {
        try {
            return this.mContext.getPackageManager().getPermissionInfo(permission, 0).backgroundPermission;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private boolean isPermissionRestricted(String name) {
        try {
            return this.mContext.getPackageManager().getPermissionInfo(name, 0).isRestricted();
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private PackageInfo getSystemPackageInfo(String pkg) {
        return getPackageInfo(pkg, 1048576);
    }

    public List<String> getWhiteListPkg() {
        if (this.mGrantPkgs == null) {
            this.mGrantPkgs = readDefaultPermissionGrantsLocked();
        }
        ArrayMap<String, List<DefaultPermissionGrant>> arrayMap = this.mGrantPkgs;
        if (arrayMap == null || arrayMap.size() == 0) {
            return new ArrayList(0);
        }
        List<String> whiteListPkg = new ArrayList<>();
        whiteListPkg.addAll(this.mGrantPkgs.keySet());
        return whiteListPkg;
    }

    private boolean isBuildInInputMethods(AndroidPackage pkg) {
        if (mSpecialInputMethodPkgs.contains(pkg.getPackageName()) && pkg.isPrivileged()) {
            return true;
        }
        return false;
    }

    public void dealWithInstallPermissionsFixed(PackageSetting ps, PackageParser.Package pkg) {
        if (ps != null && pkg != null && pkg.packageName != null && !ps.areInstallPermissionsFixed() && mSpecialDeletedSysPkgs.contains(pkg.packageName)) {
            ps.setInstallPermissionsFixed(true);
        }
    }

    private boolean isVivoPackage(AndroidPackage pkg) {
        if (pkg.isSystem() || pkg.isPrivileged()) {
            return true;
        }
        AndroidPackage basePkg = this.mPackageManagerInt.getPackage(VivoPermissionUtils.OS_PKG);
        if (compareSignatures(basePkg, pkg)) {
            return true;
        }
        AndroidPackage sharePkg = this.mPackageManagerInt.getPackage("com.android.providers.contacts");
        if (compareSignatures(sharePkg, pkg)) {
            return true;
        }
        AndroidPackage mediaPkg = this.mPackageManagerInt.getPackage("com.android.providers.media");
        return compareSignatures(mediaPkg, pkg);
    }

    private boolean compareSignatures(AndroidPackage pkg1, AndroidPackage pkg2) {
        if (pkg1 == null || pkg2 == null) {
            return false;
        }
        int match = PackageManagerServiceUtils.compareSignatures(pkg1.getSigningDetails().signatures, pkg2.getSigningDetails().signatures);
        return match == 0;
    }

    private boolean isSysComponentOrPersistentPlatformSignedPrivApp(PackageInfo pkg) {
        if (pkg.applicationInfo == null) {
            return false;
        }
        if (UserHandle.getAppId(pkg.applicationInfo.uid) < 10000) {
            return true;
        }
        if (pkg.applicationInfo.isPrivilegedApp()) {
            PackageInfo disabledPkg = getSystemPackageInfo(this.mPackageManagerInt.getDisabledSystemPackageName(pkg.applicationInfo.packageName));
            if (disabledPkg != null) {
                ApplicationInfo disabledPackageAppInfo = disabledPkg.applicationInfo;
                if (disabledPackageAppInfo != null && (disabledPackageAppInfo.flags & 8) == 0) {
                    return false;
                }
            } else if ((pkg.applicationInfo.flags & 8) == 0) {
                return false;
            }
            return this.mPackageManagerInt.isPlatformSigned(pkg.packageName);
        }
        return false;
    }

    public Map<String, Set<String>> getSystemFixedPkgs() {
        Map<String, Set<String>> systemFixedPkgsClone = new ArrayMap<>(this.systemFixedPkgs.size());
        synchronized (this.systemFixedPkgs) {
            systemFixedPkgsClone.putAll(this.systemFixedPkgs);
        }
        return systemFixedPkgsClone;
    }

    public void addSystemFixedPermToPkg(String pkg, String systemFixedPerm) {
        VSlog.v(TAG, "add systemfixed perm: " + systemFixedPerm + " to pkg: " + pkg);
        if (pkg == null || systemFixedPerm == null) {
            return;
        }
        synchronized (this.systemFixedPkgs) {
            Set<String> systemFixedPerms = this.systemFixedPkgs.get(pkg);
            if (systemFixedPerms == null) {
                systemFixedPerms = new ArraySet();
                this.systemFixedPkgs.put(pkg, systemFixedPerms);
            }
            systemFixedPerms.add(systemFixedPerm);
        }
    }

    public void removeSystemFixedPermtoPkg(String pkg, String perm) {
        VSlog.v(TAG, "remove systemfixed perm: " + perm + " to pkg: " + pkg);
        if (pkg == null || perm == null) {
            return;
        }
        synchronized (this.systemFixedPkgs) {
            Set<String> systemFixedPerms = this.systemFixedPkgs.get(pkg);
            if (systemFixedPerms == null) {
                return;
            }
            if (systemFixedPerms.contains(perm)) {
                systemFixedPerms.remove(perm);
            }
        }
    }

    public void grantRuntimePermissionsFromVgc(Handler handler) {
        if (handler == null) {
            return;
        }
        handler.post(new Runnable() { // from class: com.android.server.pm.permission.-$$Lambda$VivoPermissionImpl$aczGneBsVn4RU9IDCIj1XkX2HQU
            @Override // java.lang.Runnable
            public final void run() {
                VivoPermissionImpl.this.lambda$grantRuntimePermissionsFromVgc$1$VivoPermissionImpl();
            }
        });
    }

    public /* synthetic */ void lambda$grantRuntimePermissionsFromVgc$1$VivoPermissionImpl() {
        int[] userIds;
        try {
            for (int userId : UserManagerService.getInstance().getUserIds()) {
                grantRuntimePermissionsFromVgc(userId);
            }
        } catch (Exception e) {
            VSlog.e(TAG, "grant vgc permission throw exception", e);
        }
    }

    public void grantRuntimePermissionsFromVgc(int userId) {
        VSlog.i(TAG, "Granting app permissions for user " + userId + " from vgc");
        AbsVivoVgcManager absVivoVgcManager = this.vivoVgcManager;
        if (absVivoVgcManager == null || !absVivoVgcManager.isVgcActivated()) {
            return;
        }
        ArrayMap<String, List<DefaultPermissionGrant>> grantPkgs = readDefaultPermissionGrantsLockedFromVGC();
        grantRuntimePermissionsToApp(grantPkgs, userId);
    }

    private ArrayMap<String, List<DefaultPermissionGrant>> readDefaultPermissionGrantsLockedFromVGC() {
        AbsVivoVgcManager absVivoVgcManager = this.vivoVgcManager;
        if (absVivoVgcManager == null || !absVivoVgcManager.isVgcActivated()) {
            return null;
        }
        List<String> vgcRuntimePermConfigsPath = this.vivoVgcManager.getFileList("runtime_perm_path", (List) null);
        if (vgcRuntimePermConfigsPath == null || vgcRuntimePermConfigsPath.isEmpty()) {
            return new ArrayMap<>(0);
        }
        ArrayMap<String, List<DefaultPermissionGrant>> grantPkgs = new ArrayMap<>();
        for (String path : vgcRuntimePermConfigsPath) {
            if (path != null && path.endsWith(".xml")) {
                File file = new File(path);
                if (file.canRead()) {
                    try {
                        InputStream str = new BufferedInputStream(new FileInputStream(file));
                        XmlPullParser parser = Xml.newPullParser();
                        parser.setInput(str, null);
                        parse(parser, grantPkgs);
                        str.close();
                    } catch (IOException | XmlPullParserException e) {
                        VSlog.w(TAG, "Error reading default permissions file " + file, e);
                    }
                } else {
                    VSlog.w(TAG, "Default permissions file " + file + " cannot be read");
                }
            }
        }
        return grantPkgs;
    }

    public boolean isPermissionsReviewRequired(String pkgName, int userId) {
        return FtBuild.isOverSeas() || this.mPackageManagerInt.isTestApp(pkgName);
    }

    public boolean needGrantPermission(String permName, int uid, int callingUid) {
        return "android.permission.ACCESS_NETWORK_STATE".equals(permName) && GameOptManager.getGamingUid() == callingUid;
    }
}