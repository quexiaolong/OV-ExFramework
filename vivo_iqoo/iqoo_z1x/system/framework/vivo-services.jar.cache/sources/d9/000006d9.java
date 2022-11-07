package com.vivo.services.rms;

import com.vivo.common.utils.VLog;
import com.vivo.services.rms.appmng.AppManager;
import java.util.List;

/* loaded from: classes.dex */
public class GameOptManager {
    public static final String GAME_WATCH_PKGNAME = "com.vivo.gamewatch";
    public static final String TAG = "GameOpt";
    private static String sName;
    private static int sPid;
    private static boolean sPlaying = false;
    private static int sUid;

    public static boolean isGamePlaying() {
        return sPlaying;
    }

    public static int getGamingUid() {
        return sUid;
    }

    public static void onProcDeath(List<String> pkgNames) {
        if (pkgNames != null && pkgNames.contains("com.vivo.gamewatch")) {
            exitGame(sName, sPid);
        }
    }

    public static void enterGame(String pkg, int pid) {
        if (!sPlaying) {
            ProcessInfo info = AppManager.getInstance().getProcessInfo(pid);
            if (info != null && info.mPkgName.equals(pkg)) {
                sUid = info.mUid;
            }
            sPid = pid;
            sName = pkg;
            sPlaying = true;
            RmsInjectorImpl.getInstance().updateGameScene(pkg, pid, true);
            VLog.i(TAG, String.format("enterGame pkg=%s pid=%d", pkg, Integer.valueOf(pid)));
        }
    }

    public static void exitGame(String pkg, int pid) {
        if (sPlaying) {
            sPlaying = false;
            sName = null;
            sPid = -1;
            sUid = -1;
            RmsInjectorImpl.getInstance().updateGameScene(pkg, pid, false);
            VLog.i(TAG, String.format("exitGame pkg=%s pid=%d", pkg, Integer.valueOf(pid)));
        }
    }

    public static String getGameName() {
        return sName;
    }
}