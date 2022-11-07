package com.android.server.pm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageInstallObserver2;
import android.content.pm.PackageParser;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.DisplayMetrics;
import android.util.FtFeature;
import com.vivo.face.common.data.Constants;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import vivo.util.VSlog;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class VivoADBVerifyInstallManager {
    private static final int ADB_INSTALL_CALLBACK_TIMEOUT = 180000;
    private static final String ADB_INSTALL_CANCEL_BR_PERMISSION = "android.permission.INSTALL_PACKAGES";
    public static final int ADB_INSTALL_OBSERVER_CALLBACK_FROM_BC = 1;
    public static final int ADB_INSTALL_OBSERVER_CALLBACK_FROM_PKMS = 3;
    public static final int ADB_INSTALL_OBSERVER_CALLBACK_FROM_TIMEOUT = 2;
    private static final int MSG_HANDLER_FOR_ADB_INSTALL_CALLBACK_OBSERVER = 2000;
    private static final int MSG_HANDLER_FOR_ADB_INSTALL_CALLBACK_TIMEOUT = 3000;
    private static final int MSG_HANDLER_FOR_ADB_INSTALL_CANCEL_FROM_BC = 1000;
    private static final int MSG_HANDLER_FOR_ADB_INSTALL_VERIFY = 4000;
    public static final String TAG = "VivoADBVerifyInstallUtils";
    private static VivoADBVerifyInstallManager sVivoADBVerifyInstallManager;
    private HandlerThread mAdbInstallManagerHThread;
    public AdbInstallVerifyManagerHandler mAdbInstallMangerHandler;
    Context mContext;
    private boolean DEBUG = false;
    private VivoAdbInstallCancelReceiver mVivoAdbInstallCancelBR = null;
    private ArrayList<VivoAdbInstallerEntry> mAdbInstallerVerifyList = new ArrayList<>();
    private boolean mSwitch = true;

    public VivoADBVerifyInstallManager() {
        HandlerThread handlerThread = new HandlerThread("AdbInstallVerifyManagerHandler");
        this.mAdbInstallManagerHThread = handlerThread;
        handlerThread.start();
        this.mAdbInstallMangerHandler = new AdbInstallVerifyManagerHandler(this.mAdbInstallManagerHThread.getLooper());
    }

    public static final VivoADBVerifyInstallManager getInstance() {
        if (sVivoADBVerifyInstallManager == null) {
            synchronized (VivoADBVerifyInstallManager.class) {
                if (sVivoADBVerifyInstallManager == null) {
                    sVivoADBVerifyInstallManager = new VivoADBVerifyInstallManager();
                }
            }
        }
        return sVivoADBVerifyInstallManager;
    }

    public void init(Context context, boolean debugLog) {
        this.mContext = context;
        this.DEBUG = debugLog;
    }

    public void systemReady() {
        this.mVivoAdbInstallCancelBR = new VivoAdbInstallCancelReceiver();
    }

    boolean handleForAdbInstallVerify(boolean debugLog, String packageName, Uri originatingUri, String originPath, IPackageInstallObserver2 observer, int installFlags, String installerPackageName, int callingUid, boolean isEng, boolean isCts, boolean isOverseas, int installerUid) {
        File installApkFile;
        int binderCallUid = Binder.getCallingUid();
        VSlog.i(TAG, "handleForAdbInstallVerify  binderCallUid:" + binderCallUid + " packageName:" + packageName + " callingUid:" + callingUid);
        if (checkCallerUidPermision(binderCallUid)) {
            this.DEBUG = debugLog;
            if (this.mSwitch) {
                if (isEng || isCts || isOverseas) {
                    VSlog.i(TAG, "Not verify uid " + installerUid + " install " + packageName);
                    return false;
                } else if (installerUid == 0) {
                    VSlog.i(TAG, "Not verify uid " + installerUid + " install " + packageName);
                    return false;
                } else if (!FtFeature.isFeatureSupport("vivo.software.adbinstallverify")) {
                    if (this.DEBUG) {
                        VSlog.d(TAG, "vivo.software.adbinstallverify not open. ");
                    }
                    return false;
                } else {
                    boolean isAdbInstallDebug = "1".equals(SystemProperties.get("persist.adb.install.debug", "0"));
                    if (isAdbInstallDebug) {
                        VSlog.w(TAG, "adb install debug ,Not verify");
                        return false;
                    }
                    File originFile = new File(originPath);
                    if (originFile.exists()) {
                        File installApkFile2 = originFile;
                        if (!originFile.isDirectory()) {
                            installApkFile = installApkFile2;
                        } else {
                            try {
                                installApkFile2 = findApkFilePathFromDataAppVmdTemp(originFile);
                                VSlog.i(TAG, "installApkFile:" + installApkFile2);
                                installApkFile = installApkFile2;
                            } catch (Exception e) {
                                VSlog.e(TAG, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + originPath + " " + e.toString());
                                installApkFile = installApkFile2;
                            }
                        }
                        if (installApkFile != null && installApkFile.exists()) {
                            String currentTime = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + SystemClock.elapsedRealtime();
                            VivoAdbInstallerVerifyInfo adbInstallVerifyInfo = new VivoAdbInstallerVerifyInfo(installFlags, installApkFile, currentTime);
                            Message verifyInfoMsg = this.mAdbInstallMangerHandler.obtainMessage(4000);
                            verifyInfoMsg.obj = adbInstallVerifyInfo;
                            this.mAdbInstallMangerHandler.sendMessage(verifyInfoMsg);
                            if (this.DEBUG) {
                                VSlog.d(TAG, "Verify " + installApkFile + " requestTime:" + currentTime + " installFlags:" + installFlags + " callingUid:" + callingUid + " callingPid:" + Binder.getCallingPid());
                            }
                            VivoAdbInstallerEntry adbInstallerVerifyEntry = new VivoAdbInstallerEntry(packageName, originatingUri, originPath, observer, currentTime);
                            synchronized (this.mAdbInstallerVerifyList) {
                                this.mAdbInstallerVerifyList.add(adbInstallerVerifyEntry);
                                if (PackageManagerService.DEBUG_FOR_FB_APL) {
                                    VSlog.d(TAG, "Add verify entry " + originPath);
                                }
                            }
                            Message msg = this.mAdbInstallMangerHandler.obtainMessage(3000);
                            msg.obj = adbInstallerVerifyEntry;
                            this.mAdbInstallMangerHandler.sendMessageDelayed(msg, 180000L);
                            return true;
                        }
                        VSlog.w(TAG, "Not exist," + originPath + " cannot verify account!");
                        return false;
                    }
                    return false;
                }
            }
            return false;
        }
        return false;
    }

    private File findApkFilePathFromDataAppVmdTemp(File originFile) {
        File[] fileLists = originFile.listFiles();
        VSlog.i(TAG, "find# filePath:" + originFile);
        if (fileLists != null) {
            for (File fileTemp : fileLists) {
                if (this.DEBUG) {
                    VSlog.d(TAG, "find#  fileTemp:" + fileTemp + " getAbsolutePath:" + fileTemp.getAbsolutePath());
                }
                if (fileTemp != null && fileTemp.getAbsolutePath().endsWith(".apk")) {
                    return fileTemp;
                }
            }
            return null;
        }
        return null;
    }

    void handleForAdbInstallObserver(String packageName, Uri originatingUri, String originPath, String requestTime, int callbackType, int resultCode) {
        try {
            handleForAdbInstallObserverInner(packageName, originatingUri, originPath, requestTime, callbackType, resultCode);
        } catch (Exception e) {
            VSlog.w(TAG, "handleForAdbInstallObserver " + e.toString());
        }
    }

    private void handleForAdbInstallObserverInner(String packageName, Uri originatingUri, String originPath, String requestTime, int callbackType, int resultCode) {
        if (this.DEBUG) {
            VSlog.d(TAG, "pn:" + packageName + " ou:" + originatingUri + " op:" + originPath + " rt:" + requestTime + " cbt:" + callbackType + " rc:" + resultCode);
        }
        if (packageName == null) {
            packageName = parserApkPkgNameFromOriginPath(originPath);
        }
        ArrayList<VivoAdbInstallerEntry> findList = new ArrayList<>();
        synchronized (this.mAdbInstallerVerifyList) {
            boolean findObserverCallBackFromPKMS = false;
            Iterator<VivoAdbInstallerEntry> it = this.mAdbInstallerVerifyList.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                VivoAdbInstallerEntry adbInstallEntry = it.next();
                VSlog.i(TAG, "check  " + adbInstallEntry);
                if (findObserverCallBackFromPKMS && callbackType == 3) {
                    VSlog.i(TAG, " break.");
                    break;
                } else if (checkInstallPathIsValid(packageName, originatingUri, originPath, adbInstallEntry)) {
                    if (this.DEBUG) {
                        VSlog.d(TAG, "Find ct " + callbackType + " " + adbInstallEntry);
                    }
                    if (callbackType != 1 && callbackType != 2) {
                        if (callbackType == 3) {
                            adbInstallEntry.mResultCode = resultCode;
                            findList.add(adbInstallEntry);
                            findObserverCallBackFromPKMS = true;
                            VSlog.i(TAG, "remove msg " + adbInstallEntry);
                            try {
                                this.mAdbInstallMangerHandler.removeMessages(3000, adbInstallEntry);
                            } catch (Exception e) {
                                VSlog.w(TAG, "remove msg catch exception " + e);
                            }
                            if (this.DEBUG) {
                                VSlog.d(TAG, "Add  " + adbInstallEntry);
                            }
                        } else {
                            VSlog.w(TAG, "unknow call back type " + adbInstallEntry);
                        }
                    } else if (requestTime.equals(adbInstallEntry.mRequestTime)) {
                        adbInstallEntry.mResultCode = resultCode;
                        findList.add(adbInstallEntry);
                        if (this.DEBUG) {
                            VSlog.d(TAG, "Add to callBack list, " + adbInstallEntry);
                        }
                    }
                }
            }
            Iterator<VivoAdbInstallerEntry> it2 = findList.iterator();
            while (it2.hasNext()) {
                VivoAdbInstallerEntry entryTemp = it2.next();
                if (this.DEBUG) {
                    VSlog.d(TAG, "remove  " + entryTemp);
                }
                this.mAdbInstallerVerifyList.remove(entryTemp);
            }
        }
        VSlog.i(TAG, "handle " + originPath + " callbackType:" + callbackType + " packageName:" + packageName + " size:" + findList.size());
        Iterator<VivoAdbInstallerEntry> it3 = findList.iterator();
        while (it3.hasNext()) {
            VivoAdbInstallerEntry entryTemp2 = it3.next();
            if (entryTemp2.mVerfiyApkObserver != null) {
                if (this.DEBUG) {
                    VSlog.d(TAG, "Call observer  requestTime " + requestTime + " callbackType:" + callbackType + " resultCode:" + resultCode + " [" + entryTemp2 + "]");
                }
                try {
                    entryTemp2.mVerfiyApkObserver.onPackageInstalled(entryTemp2.mVerifyApkPath, resultCode, (String) null, (Bundle) null);
                } catch (RemoteException e2) {
                    VSlog.w(TAG, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + originPath + " remote died! ");
                } catch (Exception e3) {
                    VSlog.e(TAG, "call packge install observer catch exception " + e3.toString());
                }
            } else {
                VSlog.w(TAG, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + originPath + " not find observer ");
            }
        }
    }

    private String parserApkPkgNameFromOriginPath(String originPath) {
        if (originPath != null) {
            File originFile = new File(originPath);
            File apkFile = findApkFilePathFromDataAppVmdTemp(originFile);
            PackageParser packageParser = new PackageParser();
            DisplayMetrics metrics = new DisplayMetrics();
            packageParser.setDisplayMetrics(metrics);
            try {
                PackageParser.Package pkg = packageParser.parsePackage(apkFile, 0);
                if (pkg != null) {
                    return pkg.packageName;
                }
                return null;
            } catch (Exception e) {
                VSlog.w(TAG, "PackageParserException: " + e.toString());
                return null;
            }
        }
        return null;
    }

    private boolean checkInstallPathIsValid(String packageName, Uri originatingUri, String originPath, VivoAdbInstallerEntry adbInstallEntry) {
        if (originPath != null && originPath.equals(adbInstallEntry.mVerifyApkPath)) {
            return true;
        }
        if (packageName != null && packageName.equals(adbInstallEntry.mPackageName)) {
            return true;
        }
        if (originatingUri != null && adbInstallEntry.mOriginatingUri != null) {
            String originUriPath1 = originatingUri.getPath();
            String originUriPath2 = adbInstallEntry.mOriginatingUri.getPath();
            if (originUriPath1 != null && originUriPath2 != null && originUriPath1.equals(originUriPath2)) {
                return true;
            }
        }
        if (this.DEBUG) {
            VSlog.d(TAG, "valid fail, " + packageName + " " + originPath + " ");
            return false;
        }
        return false;
    }

    private String parserApkPathFromUri(Uri originatingUri) {
        if (originatingUri == null) {
            return null;
        }
        String apkPath = originatingUri.getPath();
        VSlog.i(TAG, "originatingUri:" + originatingUri + " apkPath:" + apkPath);
        return apkPath;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class AdbInstallVerifyManagerHandler extends Handler {
        public AdbInstallVerifyManagerHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (VivoADBVerifyInstallManager.this.DEBUG) {
                VSlog.d(VivoADBVerifyInstallManager.TAG, "--handleMessage " + msg.what);
            }
            int i = msg.what;
            if (i == 1000) {
                VivoAdbInstallerEntry adbInstallEntry = (VivoAdbInstallerEntry) msg.obj;
                if (VivoADBVerifyInstallManager.this.DEBUG) {
                    VSlog.d(VivoADBVerifyInstallManager.TAG, "INSTALL_CANCEL_FROM_BC " + adbInstallEntry);
                }
                VivoADBVerifyInstallManager.this.handleForAdbInstallObserver(adbInstallEntry.mPackageName, adbInstallEntry.mOriginatingUri, adbInstallEntry.mVerifyApkPath, adbInstallEntry.mRequestTime, adbInstallEntry.mCallbackType, adbInstallEntry.mResultCode);
            } else if (i == 3000) {
                VivoAdbInstallerEntry adbInstallVerityTimeOutEntry = (VivoAdbInstallerEntry) msg.obj;
                adbInstallVerityTimeOutEntry.mCallbackType = 2;
                VSlog.d(VivoADBVerifyInstallManager.TAG, "INSTALL_CALLBACK_TIMEOUT " + adbInstallVerityTimeOutEntry);
                VivoADBVerifyInstallManager.this.handleForAdbInstallObserver(adbInstallVerityTimeOutEntry.mPackageName, adbInstallVerityTimeOutEntry.mOriginatingUri, adbInstallVerityTimeOutEntry.mVerifyApkPath, adbInstallVerityTimeOutEntry.mRequestTime, 2, VivoPmsImpl.UNKNOWN_UID);
            } else if (i == 4000) {
                VivoAdbInstallerVerifyInfo vivoAdbInstallerVerifyInfo = (VivoAdbInstallerVerifyInfo) msg.obj;
                Intent intent = new Intent();
                intent.setFlags(268435456);
                intent.putExtra("install_app_from_adb", "adb");
                intent.putExtra("requestTime", vivoAdbInstallerVerifyInfo.currentTime);
                intent.putExtra("installFlags", vivoAdbInstallerVerifyInfo.installFlags);
                intent.setDataAndType(Uri.fromFile(vivoAdbInstallerVerifyInfo.installApkFile), "application/vnd.android.package-archive");
                if (PackageManagerService.DEBUG_FOR_ALL) {
                    VSlog.d(VivoADBVerifyInstallManager.TAG, "intent:" + intent);
                }
                VivoADBVerifyInstallManager.this.mContext.startActivity(intent);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class VivoAdbInstallCancelReceiver extends BroadcastReceiver {
        public VivoAdbInstallCancelReceiver() {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.CANCLE_INSTALL_APP_FROM_ADB");
            VivoADBVerifyInstallManager.this.mContext.registerReceiver(this, filter, VivoADBVerifyInstallManager.ADB_INSTALL_CANCEL_BR_PERMISSION, null);
            VSlog.i(VivoADBVerifyInstallManager.TAG, "regist for CANCLE_INSTALL_APP_FROM_ADB");
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            VSlog.i(VivoADBVerifyInstallManager.TAG, "CANCLE_INSTALL_APP_FROM_ADB  intent " + intent);
            if (intent != null && "android.intent.action.CANCLE_INSTALL_APP_FROM_ADB".equals(intent.getAction())) {
                String apkPath = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + intent.getExtra("apkPath");
                String requestTime = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + intent.getExtra("requestTime");
                String packageName = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + intent.getExtra("packageName");
                VSlog.d(VivoADBVerifyInstallManager.TAG, "ap:" + apkPath + " rt:" + requestTime + " pkg:" + packageName);
                requestTime = (requestTime == null || requestTime.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK)) ? "unknow" : "unknow";
                Message msg = VivoADBVerifyInstallManager.this.mAdbInstallMangerHandler.obtainMessage(1000);
                VivoAdbInstallerEntry adbEntry = new VivoAdbInstallerEntry(packageName, apkPath, null, requestTime, VivoPmsImpl.UNKNOWN_UID, 1);
                msg.obj = adbEntry;
                VivoADBVerifyInstallManager.this.mAdbInstallMangerHandler.sendMessage(msg);
            }
        }
    }

    private boolean checkCallerUidPermision(int callingUid) {
        if (callingUid == 0 || callingUid == 1000) {
            return true;
        }
        return false;
    }

    /* loaded from: classes.dex */
    static class VivoAdbInstallerVerifyInfo {
        public String currentTime;
        public File installApkFile;
        public int installFlags;

        public VivoAdbInstallerVerifyInfo(int installFlags, File installApkFile, String currentTime) {
            this.installFlags = installFlags;
            this.installApkFile = installApkFile;
            this.currentTime = currentTime;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class VivoAdbInstallerEntry {
        public int mCallbackType;
        public Uri mOriginatingUri;
        public String mPackageName;
        public String mRequestTime;
        public int mResultCode;
        public IPackageInstallObserver2 mVerfiyApkObserver;
        public String mVerifyApkPath;

        public VivoAdbInstallerEntry(String packageName, Uri originatingUri, String verifyApkPath, IPackageInstallObserver2 observer, String currentime) {
            this.mResultCode = VivoPmsImpl.UNKNOWN_UID;
            this.mCallbackType = -1;
            this.mPackageName = packageName;
            this.mOriginatingUri = originatingUri;
            this.mVerifyApkPath = verifyApkPath;
            this.mVerfiyApkObserver = observer;
            this.mRequestTime = currentime;
        }

        public VivoAdbInstallerEntry(String verifyApkPath, IPackageInstallObserver2 observer, String currentime) {
            this.mResultCode = VivoPmsImpl.UNKNOWN_UID;
            this.mCallbackType = -1;
            this.mVerifyApkPath = verifyApkPath;
            this.mVerfiyApkObserver = observer;
            this.mRequestTime = currentime;
        }

        public VivoAdbInstallerEntry(String packageName, String verifyApkPath, IPackageInstallObserver2 observer, String currentime, int result, int callbackType) {
            this.mResultCode = VivoPmsImpl.UNKNOWN_UID;
            this.mCallbackType = -1;
            this.mPackageName = packageName;
            this.mVerifyApkPath = verifyApkPath;
            this.mVerfiyApkObserver = observer;
            this.mRequestTime = currentime;
            this.mResultCode = result;
            this.mCallbackType = callbackType;
        }

        public String toString() {
            return "pn:" + this.mPackageName + " op:" + this.mVerifyApkPath + " rt:" + this.mRequestTime + " rc:" + this.mResultCode + " cbt:" + this.mCallbackType;
        }
    }

    boolean checkCalingHasInstallFromVivoAdbPermission(int callingUid) {
        int callingPid = Binder.getCallingPid();
        long ident = Binder.clearCallingIdentity();
        try {
            try {
            } catch (Exception e) {
                VSlog.w(TAG, "check  uid:" + callingUid + " pid:" + callingPid + " catch exception " + e.toString());
            }
            if (checkCallingPidIsFromPm(callingPid, callingUid)) {
                VSlog.i(TAG, "find calling   pid:" + callingPid + " uid:" + callingUid + " from system pm.");
                return true;
            }
            VSlog.w(TAG, "Check pid  " + callingPid + " uid " + callingUid + " fail!");
            return false;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private boolean checkCallingPidIsFromPm(int callingPid, int callingUid) {
        try {
            String callingPidFilePath = "/proc/" + callingPid + "/cmdline";
            ArrayList<String> pidCmdInfoList = readPidInfoFromProc(callingPidFilePath);
            boolean findPm = false;
            if (pidCmdInfoList != null) {
                Iterator<String> it = pidCmdInfoList.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    String pidCmdInfo = it.next();
                    if (pidCmdInfo.contains("com.android.commands.pm")) {
                        VSlog.i(TAG, "calling pid " + callingPid + " is from pm, should check Valid...");
                        findPm = true;
                        break;
                    }
                }
            } else {
                VSlog.w(TAG, "Odd " + callingPidFilePath + " is empty!");
            }
            VSlog.i(TAG, "read " + callingPidFilePath + " find? " + findPm);
            if (findPm) {
                return checkCallingPidOriginFileIsValid(callingPid);
            }
        } catch (Exception e) {
            VSlog.e(TAG, "read file catch exception " + e.toString());
        }
        VSlog.i(TAG, "check callingPid " + callingPid + " fail!");
        return false;
    }

    private boolean checkCallingPidOriginFileIsValid(int callingPid) {
        String pidCmdFilePath = "/proc/" + callingPid + "/maps";
        ArrayList<String> pidFileList = readPidInfoFromProc(pidCmdFilePath);
        if (pidFileList != null) {
            Iterator<String> it = pidFileList.iterator();
            while (it.hasNext()) {
                String recordInfo = it.next();
                if (recordInfo != null && (recordInfo.contains("/system/framework/oat/arm/pm.odex") || recordInfo.contains("/system/framework/oat/arm64/pm.odex") || recordInfo.contains("/system/framework/arm/pm.odex") || recordInfo.contains("/system/framework/arm64/pm.odex") || recordInfo.contains("/system/framework/pm.jar") || recordInfo.contains("/system/framework/pm.odex") || recordInfo.contains("/data/dalvik-cache/arm/system@framework@pm.jar@classes.dex") || recordInfo.contains("/data/dalvik-cache/arm64/system@framework@pm.jar@classes.dex"))) {
                    if (this.DEBUG) {
                        VSlog.d(TAG, "##Find " + pidCmdFilePath + " " + recordInfo);
                        return true;
                    }
                    return true;
                }
            }
        } else {
            VSlog.w(TAG, "Odd " + pidCmdFilePath + " is Empty!");
        }
        VSlog.w(TAG, "check " + callingPid + " origin file not valid!  path " + pidCmdFilePath);
        return false;
    }

    private ArrayList<String> readPidInfoFromProc(String pidCmdFilePath) {
        File pidCmdFile = new File(pidCmdFilePath);
        if (pidCmdFile.exists()) {
            BufferedReader reader = null;
            try {
                try {
                    reader = new BufferedReader(new FileReader(pidCmdFile));
                    ArrayList<String> pidFdList = new ArrayList<>();
                    while (true) {
                        String readStr = reader.readLine();
                        if (readStr == null) {
                            try {
                                break;
                            } catch (IOException e) {
                            }
                        } else if (readStr != null && readStr.length() > 0) {
                            pidFdList.add(readStr);
                        }
                    }
                    reader.close();
                    return pidFdList;
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
        VSlog.w(TAG, " pidCmdFile:" + pidCmdFile);
        return null;
    }
}