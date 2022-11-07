package com.android.server.pm.dex;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.os.RemoteException;
import android.os.UserManager;
import com.android.server.pm.PackageDexOptimizer;
import com.android.server.pm.dex.PackageDexUsage;
import com.vivo.services.security.server.VivoPermissionUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoDexManagerImpl implements IVivoDexManager {
    private static final String PCL_CONTEXT = "PCL[]";
    private static final String TAG = "VivoDexManager";
    private static final String TINKER_PATCH = "tinker_classN.apk";
    private static ArrayList<String> odexList = new ArrayList<>();
    private final Context mContext;
    private final DexManager mDexManager;
    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final PackageDexOptimizer mPackageDexOptimizer;
    private final PackageDexUsage mPackageDexUsage;
    private final IPackageManager mPackageManager;

    public VivoDexManagerImpl(DexManager dm, IPackageManager pms, PackageDexUsage pdu, PackageDexOptimizer pdo, Context context) {
        this.mDexManager = dm;
        this.mPackageManager = pms;
        this.mPackageDexUsage = pdu;
        this.mPackageDexOptimizer = pdo;
        this.mContext = context;
    }

    public boolean isTinkerPatch(Map<String, String> classLoaderContextMap) {
        String dexPath;
        return classLoaderContextMap != null && classLoaderContextMap.size() == 1 && (dexPath = (String) classLoaderContextMap.keySet().toArray()[0]) != null && dexPath.contains(TINKER_PATCH);
    }

    public void notifyTinkerLoad(final ApplicationInfo loadingAppInfo, Map<String, String> classLoaderContextMap, String loaderIsa, int loaderUserId) {
        Map<String, String> classLoaderContextMap2;
        Map<String, String> classLoaderContextMap3;
        try {
            final String tinkerPath = (String) classLoaderContextMap.keySet().toArray()[0];
            classLoaderContextMap2 = classLoaderContextMap;
            try {
                String loaderContext = classLoaderContextMap2.get(tinkerPath);
                PackageDexUsage.PackageUseInfo useInfo = this.mDexManager.getPackageUseInfoOrDefault(loadingAppInfo.packageName);
                boolean shouldDexopt = !useInfo.getDexUseInfoMap().containsKey(tinkerPath);
                VSlog.i(TAG, String.format("%s triggered dexopt tinker, path:%s, loaderContext:%s, shouldDexopt:%b", loadingAppInfo.packageName, tinkerPath, loaderContext, Boolean.valueOf(shouldDexopt)));
                if (!shouldDexopt && !odexList.contains(tinkerPath)) {
                    File file = new File(tinkerPath);
                    File temp64File = new File(file.getParent() + "/oat/arm64/" + file.getName().split("\\.")[0] + ".odex");
                    if (temp64File.exists() && temp64File.length() > 0) {
                        odexList.add(tinkerPath);
                    } else {
                        File tempFile = new File(file.getParent() + "/oat/arm/" + file.getName().split("\\.")[0] + ".odex");
                        if (tempFile.exists() && tempFile.length() > 0) {
                            odexList.add(tinkerPath);
                        }
                    }
                }
                if (!shouldDexopt && odexList.contains(tinkerPath)) {
                }
                if (isUserUnlocked(loaderUserId)) {
                    if (!"=UnsupportedClassLoaderContext=".equals(loaderContext)) {
                        classLoaderContextMap3 = classLoaderContextMap2;
                    } else {
                        classLoaderContextMap3 = new HashMap<>();
                        try {
                            classLoaderContextMap3.put(tinkerPath, PCL_CONTEXT);
                        } catch (Exception e) {
                            e = e;
                            classLoaderContextMap2 = classLoaderContextMap3;
                            VSlog.w(TAG, "Exception while notifying tinker patch load for package " + loadingAppInfo.packageName, e);
                        }
                    }
                    try {
                        this.mDexManager.notifyDexLoadInternal(loadingAppInfo, classLoaderContextMap3, loaderIsa, loaderUserId);
                        if (!odexList.contains(tinkerPath)) {
                            odexList.add(tinkerPath);
                        }
                        this.mExecutor.execute(new Runnable() { // from class: com.android.server.pm.dex.-$$Lambda$VivoDexManagerImpl$cjKrRRam7lLVpqykA520g0N-oUk
                            @Override // java.lang.Runnable
                            public final void run() {
                                VivoDexManagerImpl.this.lambda$notifyTinkerLoad$0$VivoDexManagerImpl(loadingAppInfo, tinkerPath);
                            }
                        });
                    } catch (Exception e2) {
                        e = e2;
                        classLoaderContextMap2 = classLoaderContextMap3;
                        VSlog.w(TAG, "Exception while notifying tinker patch load for package " + loadingAppInfo.packageName, e);
                    }
                }
            } catch (Exception e3) {
                e = e3;
                VSlog.w(TAG, "Exception while notifying tinker patch load for package " + loadingAppInfo.packageName, e);
            }
        } catch (Exception e4) {
            e = e4;
            classLoaderContextMap2 = classLoaderContextMap;
        }
    }

    public /* synthetic */ void lambda$notifyTinkerLoad$0$VivoDexManagerImpl(ApplicationInfo loadingAppInfo, String tinkerPath) {
        dexoptTinkerPatch(loadingAppInfo.packageName, tinkerPath);
    }

    private boolean dexoptTinkerPatch(String packageName, String tinkerPath) {
        VSlog.i(TAG, "Start to dexopt: " + tinkerPath);
        if (VivoPermissionUtils.OS_PKG.equals(packageName)) {
            VSlog.wtf(TAG, "System server jars should be optimized with dexoptSystemServer");
            return false;
        }
        DexoptOptions options = new DexoptOptions(packageName, 3, 525);
        PackageDexUsage.PackageUseInfo useInfo = this.mDexManager.getPackageUseInfoOrDefault(packageName);
        if (useInfo.getDexUseInfoMap().isEmpty()) {
            VSlog.i(TAG, "No secondary dex use for package:" + packageName);
            return true;
        }
        PackageDexUsage.DexUseInfo tinkerUseInfo = (PackageDexUsage.DexUseInfo) useInfo.getDexUseInfoMap().get(tinkerPath);
        if (tinkerUseInfo == null) {
            VSlog.w(TAG, String.format("tinker path:%s is provided, but dexuseinfo is empty.", tinkerPath));
            return true;
        }
        try {
            PackageInfo pkg = this.mPackageManager.getPackageInfo(packageName, 0, tinkerUseInfo.getOwnerUserId());
            if (pkg == null) {
                VSlog.i(TAG, "Could not find package when compiling secondary dex " + packageName + " for user " + tinkerUseInfo.getOwnerUserId());
                this.mPackageDexUsage.removeUserPackage(packageName, tinkerUseInfo.getOwnerUserId());
                return true;
            }
            int result = this.mPackageDexOptimizer.dexOptSecondaryDexPath(pkg.applicationInfo, tinkerPath, tinkerUseInfo, options);
            VSlog.i(TAG, String.format("End to dexopt: %s, result: %d", tinkerPath, Integer.valueOf(result)));
            return result != -1;
        } catch (RemoteException e) {
            throw new AssertionError(e);
        }
    }

    private boolean isUserUnlocked(int userId) {
        int[] profileIds;
        UserManager userManager = (UserManager) this.mContext.getSystemService("user");
        for (int id : userManager.getProfileIds(userId, true)) {
            if (!userManager.isUserUnlocked(id)) {
                return false;
            }
        }
        return true;
    }
}