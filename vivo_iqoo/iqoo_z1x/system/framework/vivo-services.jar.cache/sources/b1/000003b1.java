package com.android.server.pm;

import android.os.FileUtils;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.text.TextUtils;
import com.android.internal.util.JournaledFile;
import com.vivo.face.common.data.Constants;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import libcore.io.IoUtils;
import vivo.app.VivoFrameworkFactory;
import vivo.app.vgc.AbsVivoVgcManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoVGCPKMSUtils {
    public static final String COTA_DATA_PATH = "/data/cota/vgc/data";
    public static final String COTA_THIRD_APP_PATH = "/data/cota/vgc/app";
    public static final String COTA_THIRD_PRIV_APP_PATH = "/data/cota/vgc/priv-app";
    public static final String DATA_VGC_APP_PATH = "/data/vgc/app";
    public static final String DATA_VGC_DATA_PATH = "/data/vgc/data";
    public static final String DATA_VGC_PRIV_APP_PATH = "/data/vgc/priv-app";
    public static final String TAG = "VivoVGCPKMSUtils";
    private static final String VGC_DATA_APP_DELETED_LIST_PATH = "data/system/vgc_deleted_app.list";
    private static final int WARN_TIME_MS = 50;
    public static boolean DEBUG = PackageManagerService.DEBUG;
    private static final String VGC_ROOT_DIR = SystemProperties.get("ro.vgc.config.rootdir", "/vendor/vgc/");
    public static final String VGC_PRIV_APP_PATH = VGC_ROOT_DIR + "priv-app";
    public static final String VGC_APP_PATH = VGC_ROOT_DIR + "app";
    public static final String COTA_PRIV_APP_PATH = VGC_ROOT_DIR + "cota/priv-app";
    public static final String COTA_APP_PATH = VGC_ROOT_DIR + "cota/app";
    public static final String COTA_OVERLAY_PATH = VGC_ROOT_DIR + "cota/overlay";

    /* loaded from: classes.dex */
    public static class VgcBlackWhitelistApps {
        public String mAppDir;
        public String mInnerFileName;

        public VgcBlackWhitelistApps(String appDir, String innerFileName) {
            this.mAppDir = appDir;
            this.mInnerFileName = innerFileName;
        }
    }

    public static void getDeletedVgcAppsList(ArrayList<String> deletedList) {
        if (deletedList == null) {
            return;
        }
        BufferedReader reader = null;
        try {
            try {
                reader = new BufferedReader(new FileReader(VGC_DATA_APP_DELETED_LIST_PATH));
                while (true) {
                    String pkgName = reader.readLine();
                    if (pkgName == null) {
                        break;
                    }
                    deletedList.add(pkgName);
                }
            } catch (IOException e) {
                VSlog.e(TAG, "Failed to read vgc_deleted_app.list", e);
            }
        } finally {
            IoUtils.closeQuietly(reader);
        }
    }

    public static void writeDeletedVgcAppList(ArrayList<String> deletedList) {
        if (deletedList == null) {
            return;
        }
        File tempFile = new File("data/system/vgc_deleted_app.list.tmp");
        JournaledFile journal = new JournaledFile(new File(VGC_DATA_APP_DELETED_LIST_PATH), tempFile);
        File writeTarget = journal.chooseForWrite();
        FileOutputStream fstr = null;
        BufferedWriter writer = null;
        try {
            try {
                fstr = new FileOutputStream(writeTarget);
                writer = new BufferedWriter(new OutputStreamWriter(fstr, Charset.defaultCharset()));
                FileUtils.setPermissions(fstr.getFD(), 420, 1000, 1032);
                Iterator<String> it = deletedList.iterator();
                while (it.hasNext()) {
                    String pkg = it.next();
                    writer.append((CharSequence) pkg).append((CharSequence) "\n");
                }
                writer.flush();
                FileUtils.sync(fstr);
                journal.commit();
            } catch (Exception e) {
                VSlog.wtf(TAG, "Failed to write vgc_deleted_app.list", e);
                journal.rollback();
            }
        } finally {
            IoUtils.closeQuietly(writer);
            IoUtils.closeQuietly(fstr);
        }
    }

    public static boolean isCodePathInVGC(String codePath) {
        if (codePath != null) {
            if (codePath.startsWith(DATA_VGC_PRIV_APP_PATH) || codePath.startsWith(VGC_PRIV_APP_PATH) || codePath.startsWith(DATA_VGC_APP_PATH) || codePath.startsWith(VGC_APP_PATH) || codePath.startsWith(DATA_VGC_DATA_PATH)) {
                return true;
            }
            return false;
        }
        return false;
    }

    public static boolean isCodePathInCota(String codePath) {
        if (codePath != null) {
            if (codePath.startsWith(COTA_APP_PATH) || codePath.startsWith(COTA_DATA_PATH) || codePath.startsWith(COTA_PRIV_APP_PATH) || codePath.startsWith(COTA_OVERLAY_PATH) || codePath.startsWith(COTA_THIRD_APP_PATH) || codePath.startsWith(COTA_THIRD_PRIV_APP_PATH)) {
                return true;
            }
            return false;
        }
        return false;
    }

    private static String getFilePatch(String name) {
        AbsVivoVgcManager vivoVgcManager = null;
        if (VivoFrameworkFactory.getFrameworkFactoryImpl() != null) {
            vivoVgcManager = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoVgcManager();
        }
        if (vivoVgcManager != null) {
            String srcPath = vivoVgcManager.getFile(name, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
            return srcPath;
        }
        VSlog.w(TAG, "getVivoVgcManager fail, vivoVgcManager = " + vivoVgcManager);
        return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    }

    public static ArrayList<VgcBlackWhitelistApps> getVgcBlackListApps() {
        long start = SystemClock.elapsedRealtime();
        List<String> fileList = getFileList("app_install_black_path");
        ArrayList<VgcBlackWhitelistApps> blacklistApps = new ArrayList<>();
        if (fileList != null) {
            for (String path : fileList) {
                blacklistApps.addAll(readVgcBlackWhitelist(path));
            }
        }
        warnIfTooLong(SystemClock.elapsedRealtime() - start, "getVgcBlackListApps");
        return blacklistApps;
    }

    public static ArrayList<VgcBlackWhitelistApps> getVgcWhiteListApps() {
        long start = SystemClock.elapsedRealtime();
        List<String> fileList = getFileList("app_install_white_path");
        ArrayList<VgcBlackWhitelistApps> whitelistApps = new ArrayList<>();
        if (fileList != null) {
            for (String path : fileList) {
                whitelistApps.addAll(readVgcBlackWhitelist(path));
            }
        }
        warnIfTooLong(SystemClock.elapsedRealtime() - start, "getVgcWhiteListApps");
        return whitelistApps;
    }

    private static List<String> getFileList(String name) {
        AbsVivoVgcManager vivoVgcManager = null;
        if (VivoFrameworkFactory.getFrameworkFactoryImpl() != null) {
            vivoVgcManager = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoVgcManager();
        }
        if (vivoVgcManager != null) {
            List<String> srcPathList = vivoVgcManager.getFileList(name, (List) null);
            return srcPathList;
        }
        VSlog.w(TAG, "getVivoVgcManager fail, vivoVgcManager = " + vivoVgcManager);
        return null;
    }

    private static void warnIfTooLong(long duration, String operation) {
        if (duration > 50) {
            VSlog.w(TAG, operation + " took " + duration + " ms.");
        }
    }

    private static ArrayList<VgcBlackWhitelistApps> readVgcBlackWhitelist(String fileName) {
        ArrayList<VgcBlackWhitelistApps> apps = new ArrayList<>();
        if (TextUtils.isEmpty(fileName)) {
            return apps;
        }
        File file = new File(fileName);
        if (file.exists()) {
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
                                VSlog.d(TAG, "#readVgcBlackWhitelist parser  " + apkPathTemp);
                            }
                            if (apkPathTemp != null && apkPathTemp.length() != 0) {
                                try {
                                    VgcBlackWhitelistApps app = parseBlackWhitelistApkPath(apkPathTemp);
                                    if (app != null) {
                                        apps.add(app);
                                    }
                                } catch (Exception e) {
                                }
                            }
                        }
                        reader.close();
                    } catch (Exception e2) {
                        VSlog.e(TAG, "readVgcBlackWhitelist " + fileName + " catch exception " + e2.toString());
                        if (reader != null) {
                            reader.close();
                        }
                    }
                } catch (Exception e3) {
                }
            } catch (Throwable th) {
                if (0 != 0) {
                    try {
                        reader.close();
                    } catch (Exception e4) {
                    }
                }
                throw th;
            }
        }
        return apps;
    }

    private static VgcBlackWhitelistApps parseBlackWhitelistApkPath(String apkPath) {
        if (apkPath == null) {
            VSlog.w(TAG, "## parseBlackWhitelistApkPath path is null!");
            return null;
        }
        String[] pathStr = apkPath.split("\\s+");
        if (pathStr != null && pathStr.length > 0) {
            if (DEBUG) {
                VSlog.d(TAG, "# parseBlackWhitelistApkPath " + pathStr[0] + " " + pathStr[1]);
            }
            if (pathStr != null && pathStr.length == 2 && pathStr[0] != null && pathStr[1] != null) {
                String appDir = pathStr[0];
                String innerFileName = pathStr[1];
                if (DEBUG) {
                    VSlog.d(TAG, "# parseBlackWhitelistApkPath appDir:" + appDir + " innerFileName:" + innerFileName);
                }
                VgcBlackWhitelistApps blackWhitelistApps = new VgcBlackWhitelistApps(appDir, innerFileName);
                return blackWhitelistApps;
            }
        }
        return null;
    }

    public static List<String> getFakeSystemFlagList() {
        AbsVivoVgcManager vivoVgcManager = null;
        if (VivoFrameworkFactory.getFrameworkFactoryImpl() != null) {
            vivoVgcManager = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoVgcManager();
        }
        if (vivoVgcManager != null) {
            return vivoVgcManager.getStringList("pms_fake_system_flag_apps", (List) null);
        }
        return null;
    }

    public static List<String> getSysUninstallableList() {
        AbsVivoVgcManager vivoVgcManager = null;
        if (VivoFrameworkFactory.getFrameworkFactoryImpl() != null) {
            vivoVgcManager = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoVgcManager();
        }
        if (vivoVgcManager != null) {
            return vivoVgcManager.getStringList("launcher_sys_app_uninstall_list", (List) null);
        }
        return null;
    }

    public static List<String> getNotUninstallableList() {
        long start = SystemClock.elapsedRealtime();
        List<String> fileList = getFileList("pm_not_uninstallable_sys_apps");
        List<String> notUninstallaleList = new ArrayList<>();
        if (fileList != null && fileList.size() > 0) {
            for (String path : fileList) {
                notUninstallaleList.addAll(readPacakgeNameList(path));
            }
        }
        warnIfTooLong(SystemClock.elapsedRealtime() - start, "getNotUninstallableList");
        return notUninstallaleList;
    }

    private static ArrayList<String> readPacakgeNameList(String fileName) {
        BufferedReader reader = null;
        ArrayList<String> pkgNameList = new ArrayList<>();
        try {
            try {
                reader = new BufferedReader(new FileReader(fileName));
                while (true) {
                    String pkgName = reader.readLine();
                    if (pkgName == null) {
                        break;
                    }
                    pkgNameList.add(pkgName);
                }
            } catch (IOException e) {
                VSlog.e(TAG, "Failed to read :" + fileName, e);
            }
            return pkgNameList;
        } finally {
            IoUtils.closeQuietly(reader);
        }
    }
}