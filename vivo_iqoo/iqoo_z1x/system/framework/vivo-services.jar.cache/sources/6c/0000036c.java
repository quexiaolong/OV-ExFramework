package com.android.server.pm;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.android.internal.util.ArrayUtils;
import com.vivo.face.common.data.Constants;
import com.vivo.services.security.server.VivoPermissionUtils;
import java.io.File;
import java.io.FileFilter;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import vivo.util.VSlog;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class VivoPKMSCommonUtils {
    public static final String TAG = "PKMSCommonUtils";

    VivoPKMSCommonUtils() {
    }

    public static String getDeviceNetworkType(boolean chatty, Context context) {
        try {
            if (isWifiConnected(chatty, context)) {
                if (isVpnUsed()) {
                    return "wifi_vpn";
                }
                return "wifi";
            } else if (isMobileConnected(chatty, context)) {
                if (isVpnUsed()) {
                    return "mobile_vpn";
                }
                return "mobile";
            } else {
                return "none";
            }
        } catch (Exception e) {
            VSlog.w(TAG, "check net type, " + e.toString());
            return "unknow";
        }
    }

    public static boolean isWifiConnected(boolean chatty, Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        if (connectivityManager == null) {
            return false;
        }
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.getType() == 1) {
            VSlog.i(TAG, "wifi is working.");
            return true;
        }
        if (chatty) {
            VSlog.i(TAG, "wifi is not work.");
        }
        return false;
    }

    public static boolean isMobileConnected(boolean chatty, Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        if (connectivityManager == null) {
            return false;
        }
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.getType() == 0) {
            VSlog.i(TAG, "mobile is working.");
            return true;
        }
        if (chatty) {
            VSlog.i(TAG, "mobile is not work.");
        }
        return false;
    }

    public static boolean isVpnUsed() {
        try {
            Enumeration<NetworkInterface> niList = NetworkInterface.getNetworkInterfaces();
            if (niList != null) {
                Iterator it = Collections.list(niList).iterator();
                while (it.hasNext()) {
                    NetworkInterface intf = (NetworkInterface) it.next();
                    if (intf.isUp() && intf.getInterfaceAddresses().size() != 0 && ("tun0".equals(intf.getName()) || "ppp0".equals(intf.getName()))) {
                        return true;
                    }
                }
                return false;
            }
            return false;
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getOriginalInstallCallerPkgName(boolean chatty, String installerPackageName, int installerUid, int originatingUid, int installFlags, Context context) {
        String originInstallCallerPkgName = "unknow";
        if (chatty) {
            VSlog.w(TAG, installerPackageName + " " + installerUid + " " + originatingUid + " " + installFlags);
        }
        if (installerUid == 0 || installerUid == 2000) {
            if (installerUid == 0) {
                originInstallCallerPkgName = "root";
            } else if (installerUid == 2000) {
                originInstallCallerPkgName = "shell";
            }
            boolean fromVivo = (1073741824 & installFlags) != 0;
            if (fromVivo) {
                return "fromvivoapp";
            }
            return originInstallCallerPkgName;
        } else if ("com.android.packageinstaller".equals(installerPackageName)) {
            String originInstallCallerPkgName2 = getPkgNameFromUid(originatingUid, context);
            if (chatty) {
                VSlog.d(TAG, "com.android.packageinstaller callerPkg=" + originInstallCallerPkgName2);
                return originInstallCallerPkgName2;
            }
            return originInstallCallerPkgName2;
        } else if ("com.google.android.packageinstaller".equals(installerPackageName)) {
            String callerPkg = getPkgNameFromUid(originatingUid, context);
            if (chatty) {
                VSlog.d(TAG, "com.google.android.packageinstaller callerPkg=" + callerPkg);
            }
            return callerPkg;
        } else {
            return installerPackageName;
        }
    }

    public static String getPackageLabelName(boolean chatty, String packageName, Context context) {
        PackageManager packageManager;
        PackageInfo packageInfo;
        if (packageName == null || packageName.equals("unknow")) {
            return "unknow";
        }
        String pkgLabelName = packageName;
        if (packageName.equals("shell") || packageName.equals("root") || packageName.equals(VivoPermissionUtils.OS_PKG)) {
            VSlog.i(TAG, packageName + " labelName " + pkgLabelName);
            return pkgLabelName;
        }
        try {
            packageManager = context.getPackageManager();
            packageInfo = packageManager.getPackageInfo(packageName, 0);
        } catch (Exception e) {
            VSlog.w(TAG, "get " + packageName + " labelName," + e.toString());
        }
        if (packageInfo == null) {
            return pkgLabelName;
        }
        ApplicationInfo appInfo = packageInfo.applicationInfo;
        if (appInfo == null) {
            return pkgLabelName;
        }
        pkgLabelName = (String) packageManager.getApplicationLabel(appInfo);
        if (chatty) {
            VSlog.i(TAG, packageName + " labelName " + pkgLabelName);
        }
        return pkgLabelName;
    }

    public static String getPkgNameFromUid(int uid, Context context) {
        String pkgName = "unknow";
        PackageManager packageManager = context.getPackageManager();
        try {
            pkgName = packageManager.getNameForUid(uid);
        } catch (Exception e) {
            VSlog.w(TAG, "getNameFromUid " + e.toString());
        }
        if (pkgName == null) {
            String pkgName2 = "unknowPkg_" + uid;
            return pkgName2;
        }
        return pkgName;
    }

    public static String getPackageVersionName(String pkgName, Context context) {
        if (pkgName == null || pkgName.equals("unknow")) {
            return "unknow";
        }
        if (pkgName.equals("shell") || pkgName.equals("root") || pkgName.equals(VivoPermissionUtils.OS_PKG) || pkgName.equals("system_server") || pkgName.equals("system")) {
            VSlog.i(TAG, "special name -> " + pkgName);
            return "0.0";
        }
        PackageManager packageManager = context.getPackageManager();
        try {
            PackageInfo pkgInfo = packageManager.getPackageInfo(pkgName, 0);
            if (pkgInfo == null) {
                return "unknow";
            }
            String versionName = pkgInfo.versionName;
            return versionName;
        } catch (Exception e) {
            VSlog.w(TAG, "getPackageInfo " + e.toString());
            return "unknow";
        }
    }

    public static PackageParser.ApkLite parserApkNotCheckSignature(File stageFile) {
        if (stageFile == null) {
            return null;
        }
        VSlog.i(TAG, "begin parser " + stageFile);
        try {
            File[] addedFiles = stageFile.listFiles(new FileFilter() { // from class: com.android.server.pm.VivoPKMSCommonUtils.1
                @Override // java.io.FileFilter
                public boolean accept(File file) {
                    return (file.isDirectory() || file.getName().endsWith(".removed")) ? false : true;
                }
            });
            VSlog.i(TAG, "stageFile:" + stageFile + " addedFiles:" + addedFiles);
            if (ArrayUtils.isEmpty(addedFiles)) {
                return null;
            }
            if (addedFiles.length > 1) {
                VSlog.w(TAG, "mutil apk " + addedFiles.length);
                int length = addedFiles.length;
                for (int i = 0; i < length; i++) {
                    File file = addedFiles[i];
                    VSlog.i(TAG, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + file);
                }
            }
            try {
                PackageParser.ApkLite apkLite = PackageParser.parseApkLite(addedFiles[0], 0);
                return apkLite;
            } catch (PackageParser.PackageParserException e) {
                VSlog.w(TAG, "parser_apk_exp1 " + e.getMessage());
                return null;
            }
        } catch (Exception e2) {
            VSlog.w(TAG, "parser_apk_exp2 " + e2.getMessage());
            return null;
        }
    }

    public static int getPackageInstallState(String packageName, Context context) {
        PackageInfo pkgInfo;
        if (packageName == null || packageName.equals("unknow")) {
            return -1;
        }
        PackageManager packageManager = context.getPackageManager();
        try {
            pkgInfo = packageManager.getPackageInfo(packageName, 0);
        } catch (Exception e) {
            VSlog.w(TAG, "getPackageInfo " + packageName + " exp:" + e.toString());
        }
        if (pkgInfo == null) {
            return 0;
        }
        return 1;
    }
}