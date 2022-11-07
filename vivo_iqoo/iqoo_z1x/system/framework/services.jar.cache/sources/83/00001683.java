package com.android.server.pm.dex;

import android.os.SystemClock;
import com.vivo.statistics.sdk.GatherManager;
import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class ArtDataHelper {
    public static final int FAIL_REASON_LOCK = 1;
    public static final int FAIL_REASON_NO_PROFILE = 0;
    public static final int FAIL_REASON_NO_PROFILE_LOCK = 2;
    public static final int FAIL_REASON_UNKNOWN = -1;
    public static final int FAIL_REASON_USER_INSTALL = 3;
    public static final int INSTALL_REASON_SELF_UPDATE = 3;
    public static final int INSTALL_REASON_SILENCE = 0;
    public static final int INSTALL_REASON_UPDATE = 1;
    public static final int INSTALL_REASON_USER = 2;
    public static final int MODE_QUICKEN = 1;
    public static final int MODE_SPEED_PROFILE = 0;
    private static final String TAG = "art_pp";
    private int mCompilerMode;
    private int mFailReason;
    private int mInstallDur;
    private int mInstallReason;
    private int mInstallSuccess;
    private String mPkgName;
    private int mProfileSize;
    private long mStartInstallTime = 0;

    public ArtDataHelper(String pkgName) {
        this.mPkgName = pkgName;
    }

    public static boolean isOpen() {
        return GatherManager.getInstance().getService() != null;
    }

    public void setStartInstallTime(long startInstallTime) {
        this.mStartInstallTime = startInstallTime;
    }

    public void end(int installReason, int installSuccess, int compilerMode, int failReason, String installResource) {
        this.mInstallReason = installReason;
        this.mInstallSuccess = installSuccess;
        this.mCompilerMode = compilerMode;
        this.mFailReason = failReason;
        this.mInstallDur = (int) (SystemClock.uptimeMillis() - this.mStartInstallTime);
        if (installResource != null) {
            if (installResource.equals("com.bbk.appstore")) {
                installReason += 10;
            } else if (installResource.equals("com.vivo.easyshare")) {
                installReason += 20;
            } else if (installResource.equals("root")) {
                installReason += 30;
            } else {
                installReason += 40;
            }
        }
        String profilePath = String.format(Locale.US, "/data/misc/usable-profiles/%s.prof", this.mPkgName);
        long tmpSize = new File(profilePath).length() / 1024;
        if (tmpSize >= 2147483647L) {
            VSlog.e(TAG, "Profile size too big");
            return;
        }
        this.mProfileSize = (int) tmpSize;
        GatherManager.getInstance().gather(TAG, new Object[]{this.mPkgName, Integer.valueOf(installSuccess), Integer.valueOf(compilerMode), Integer.valueOf(failReason), Integer.valueOf(installReason), Integer.valueOf(this.mInstallDur), Integer.valueOf(this.mProfileSize)});
    }

    public static String getSourceInstallation(String installerPackage, int installerUid, String callerPkg, int installFlags) {
        String install_resource = null;
        if ("com.android.packageinstaller".equals(installerPackage)) {
            return callerPkg;
        }
        if (installerUid == 0 || installerUid == 2000) {
            if (installerUid == 0) {
                install_resource = "root";
            } else if (installerUid == 2000) {
                install_resource = "shell";
            }
            boolean fromVivo = (1073741824 & installFlags) != 0;
            if (fromVivo) {
                return "com.vivo.PCTools";
            }
            return install_resource;
        } else if ("com.google.android.packageinstaller".equals(installerPackage)) {
            return callerPkg;
        } else {
            return installerPackage;
        }
    }

    public String toString() {
        HashMap<String, String> outVal = new HashMap<>();
        outVal.put("pkg", this.mPkgName);
        outVal.put("installSuccess", String.valueOf(this.mInstallSuccess));
        outVal.put("mode", String.valueOf(this.mCompilerMode));
        outVal.put("failReason", String.valueOf(this.mFailReason));
        outVal.put("installReason", String.valueOf(this.mInstallReason));
        outVal.put("installDur", String.valueOf(this.mInstallDur));
        outVal.put("profileSize", String.valueOf(this.mProfileSize));
        return outVal.toString();
    }
}