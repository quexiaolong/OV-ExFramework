package com.android.server.pm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageDeleteObserver2;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.FtFeature;
import com.android.server.pm.VivoPKMSUtils;
import com.vivo.face.common.data.Constants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import vivo.util.VSlog;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class VivoUninstallMgr {
    private static final String ACTION_ADB_UN_INSTALL_CANCEL_BC = "vivo.intent.action.ADB_UN_INSTALL_CANCEL_BC";
    private static final String ACTION_ADB_UN_INSTALL_VERIFY_FAIL = "vivo.intent.action.ADB_UN_INSTALL_VERIFY_FAIL";
    private static final String ACTION_ADB_UN_INSTALL_VERIFY_SUC = "vivo.intent.action.ADB_UN_INSTALL_VERIFY_SUC";
    private static final long ADB_UN_INSTALL_CALLBACK_TIMEOUT = 180000;
    private static final String ADB_UN_INSTALL_CANCEL_BR_PERMISSION = "android.permission.INSTALL_PACKAGES";
    static final long ADB_UN_INSTALL_MAX_VERIFY_INTERVAL = 1296000;
    private static final int ADB_UN_INSTALL_OBSERVER_CALLBACK_FROM_BC = 1;
    public static final int ADB_UN_INSTALL_OBSERVER_CALLBACK_FROM_PKMS = 3;
    private static final int ADB_UN_INSTALL_OBSERVER_CALLBACK_FROM_TIMEOUT = 2;
    private static final int MSG_FOR_ADB_UN_INSTALL_CALLBACK_FROM_PMS = 2000;
    private static final int MSG_FOR_ADB_UN_INSTALL_CALLBACK_TIMEOUT = 2001;
    private static final int MSG_FOR_ADB_UN_INSTALL_CANCEL_FROM_BC = 3000;
    private static final int MSG_FOR_ADB_UN_INSTALL_VERIFY = 1000;
    private static final int MSG_FOR_ADB_UN_INSTALL_VERIFY_FAIL = 1001;
    private static final int MSG_FOR_ADB_UN_INSTALL_VERIFY_SUC = 1002;
    public static final String TAG = "VivoUninstallMgr";
    private static volatile VivoUninstallMgr sVivoUninstallMgr;
    private static volatile WorkHandler sWorkHandler;
    private ArrayList<String> mBuiltInAppList;
    private Context mContext;
    private HashMap<String, String> mSystemCustomAppMap;
    private boolean DEBUG = false;
    private boolean DEBUG_FOR_ALL = false;
    private VivoAdbUnInstallVerifyBc mVivoAdbUnInstallVerifyBr = null;
    private ArrayList<VivoAdbUnInstallEntry> mAdbUnInstallVerifyList = new ArrayList<>();
    private boolean mSwitch = true;

    private VivoUninstallMgr() {
        VSlog.i(TAG, "UninstallMgr..");
    }

    public static final VivoUninstallMgr getInstance() {
        if (sVivoUninstallMgr == null) {
            synchronized (VivoUninstallMgr.class) {
                if (sVivoUninstallMgr == null) {
                    sVivoUninstallMgr = new VivoUninstallMgr();
                }
            }
        }
        return sVivoUninstallMgr;
    }

    public void init(Context context, Looper looper, ArrayList<String> builtInAppList, boolean debug, boolean debugAll) {
        if (sWorkHandler == null) {
            synchronized (VivoUninstallMgr.class) {
                if (sWorkHandler == null) {
                    sWorkHandler = new WorkHandler(looper);
                }
            }
        }
        this.mContext = context;
        this.DEBUG = debug;
        this.DEBUG_FOR_ALL = debugAll;
        this.mBuiltInAppList = builtInAppList;
        VSlog.i(TAG, "init " + debug + " " + debugAll);
    }

    public void systemReady(HashMap<String, String> sysCustomAppMap) {
        if (!isUnInstallVerifyFeatureSupport()) {
            return;
        }
        this.mSystemCustomAppMap = sysCustomAppMap;
        this.mVivoAdbUnInstallVerifyBr = new VivoAdbUnInstallVerifyBc();
        if (this.DEBUG_FOR_ALL) {
            VSlog.d(TAG, "map-> " + this.mSystemCustomAppMap + " \n list-> " + this.mBuiltInAppList);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean handleForAdbUnInstallVerify(boolean chatty, String packageName, String internalPackageName, IPackageDeleteObserver2 observer, PackageSetting ps, int flags, int callingUid, int callingPid, boolean isEng, boolean isOverseas, int userId) {
        this.DEBUG = chatty;
        if (this.mSwitch) {
            if (isOverseas) {
                VSlog.i(TAG, "Not verify uninstall " + packageName);
                return false;
            } else if (!isCallingUidShouldVerify(callingUid)) {
                VSlog.i(TAG, "Not verify uid " + callingUid + " " + callingPid);
                return false;
            } else if (isUnInstallVerifyFeatureSupport()) {
                VivoPKMSUtils.AdbVerifyConfig config = VivoPKMSUtils.getAdbUninstallVerifyConfig();
                if (!config.mFeatureOpen) {
                    VSlog.w(TAG, "feature is not open, not verify.");
                    return false;
                } else if (isDeviceCpuElapsedTimeEnough(config)) {
                    VSlog.i(TAG, "do not verify, onvertime.");
                    return false;
                } else {
                    boolean hasVerified = "1".equals(SystemProperties.get("persist.sys.uninstall.verified", "0"));
                    if (hasVerified) {
                        VSlog.w(TAG, "adb uninstall valided.");
                        return false;
                    } else if (!checkUninstallAPkIsShouldBeVerify(ps, packageName)) {
                        VSlog.i(TAG, "do not need verify, " + ps);
                        return false;
                    } else {
                        String currentTime = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + SystemClock.elapsedRealtime();
                        VivoAdbUnInstallVerifyInfo verifyInfo = new VivoAdbUnInstallVerifyInfo(flags, packageName, currentTime);
                        Message verifyInfoMsg = sWorkHandler.obtainMessage(1000);
                        verifyInfoMsg.obj = verifyInfo;
                        sWorkHandler.sendMessage(verifyInfoMsg);
                        if (this.DEBUG) {
                            VSlog.d(TAG, "Verify " + packageName + " requestTime:" + currentTime + " flags:" + flags + " callingUid:" + callingUid + " callingPid:" + callingPid);
                        }
                        VivoAdbUnInstallEntry unInstallEntry = new VivoAdbUnInstallEntry(packageName, ps, observer, currentTime);
                        synchronized (this.mAdbUnInstallVerifyList) {
                            this.mAdbUnInstallVerifyList.add(unInstallEntry);
                        }
                        Message msg = sWorkHandler.obtainMessage(MSG_FOR_ADB_UN_INSTALL_CALLBACK_TIMEOUT);
                        msg.obj = unInstallEntry;
                        sWorkHandler.sendMessageDelayed(msg, ADB_UN_INSTALL_CALLBACK_TIMEOUT);
                        return true;
                    }
                }
            } else {
                return false;
            }
        }
        return false;
    }

    private boolean checkUninstallAPkIsShouldBeVerify(PackageSetting ps, String packageName) {
        if (packageName == null || ps == null || ps.pkg == null) {
            return false;
        }
        String pkgCodePath = ps.pkg.getCodePath();
        VSlog.i(TAG, "codePath " + pkgCodePath + " packageName:" + packageName);
        if (pkgCodePath == null) {
            return false;
        }
        return isApkBuiltInApk(packageName) || isSpecialApk(pkgCodePath, packageName);
    }

    private boolean isApkBuiltInApk(String packageName) {
        boolean findBuiltInApk = false;
        ArrayList<String> arrayList = this.mBuiltInAppList;
        if (arrayList != null && arrayList.contains(packageName)) {
            findBuiltInApk = true;
        }
        if (findBuiltInApk) {
            ArrayList<String> delApkList = VivoPKMSUtils.getDeletedPkgListFromLocalFile("data/system/v_deleted_built_in_app.xml");
            if (this.DEBUG_FOR_ALL) {
                VSlog.d(TAG, "delList-> " + delApkList);
            }
            if (delApkList != null && delApkList.contains(packageName)) {
                VSlog.w(TAG, packageName + " has deled before.");
                return false;
            }
            return findBuiltInApk;
        }
        return findBuiltInApk;
    }

    private boolean isSpecialApk(String pkgCodePath, String packageName) {
        HashMap<String, String> hashMap = this.mSystemCustomAppMap;
        if (hashMap != null && hashMap.containsKey(packageName)) {
            return true;
        }
        return false;
    }

    private boolean isDeviceCpuElapsedTimeEnough(VivoPKMSUtils.AdbVerifyConfig config) {
        VivoPKMSUtils.DeviceBootInfo bootInfo = VivoPKMSUtils.parserDeviceBootInfoFromProp();
        if (bootInfo == null || bootInfo.bootCount < 0) {
            VSlog.w(TAG, "parser cpu elapse time error.");
            return true;
        }
        VSlog.i(TAG, "verify config " + config + "   elapseTime-> " + bootInfo.bootElapsedTime);
        if (bootInfo.bootElapsedTime >= config.mMaxVerifyInterval) {
            return true;
        }
        return false;
    }

    private boolean isCallingUidShouldVerify(int callingUid) {
        if (callingUid == 2000) {
            return true;
        }
        return false;
    }

    private boolean isUnInstallVerifyFeatureSupport() {
        if (FtFeature.isFeatureSupport("vivo.software.adb.un.install.verify")) {
            return true;
        }
        VSlog.i(TAG, "vivo.software.adb.un.install.verify not open. ");
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void scheduleAdbUnInstallObserver(String packageName, int resultCode, int callbackType) {
        if (!isUnInstallVerifyFeatureSupport()) {
            return;
        }
        Message msg = sWorkHandler.obtainMessage(2000);
        VivoAdbUnInstallEntry adbEntry = new VivoAdbUnInstallEntry(packageName, resultCode, callbackType);
        msg.obj = adbEntry;
        sWorkHandler.sendMessage(msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleForAdbUnInstallObserver(VivoAdbUnInstallEntry verifyEntry) {
        if (!isUnInstallVerifyFeatureSupport()) {
            return;
        }
        if (this.DEBUG) {
            VSlog.d(TAG, verifyEntry.toString());
        }
        if (verifyEntry == null) {
            return;
        }
        try {
            if (TextUtils.isEmpty(verifyEntry.mPackageName)) {
                return;
            }
            handleForAdbUnInstallObserverInner(verifyEntry.mPackageName, verifyEntry.mRequestTime, verifyEntry.mCallbackType, verifyEntry.mResultCode);
        } catch (Exception e) {
            VSlog.w(TAG, "handle observer, " + e.toString());
        }
    }

    private void handleForAdbUnInstallObserverInner(String packageName, String requestTime, int callbackType, int resultCode) {
        ArrayList<VivoAdbUnInstallEntry> findList = new ArrayList<>();
        synchronized (this.mAdbUnInstallVerifyList) {
            VSlog.i(TAG, "checkSize:" + this.mAdbUnInstallVerifyList.size());
            Iterator<VivoAdbUnInstallEntry> it = this.mAdbUnInstallVerifyList.iterator();
            while (it.hasNext()) {
                VivoAdbUnInstallEntry unInstallEntry = it.next();
                VSlog.i(TAG, "check.." + callbackType + " " + unInstallEntry.mCallbackType);
                if (this.DEBUG) {
                    VSlog.d(TAG, "docheck.  " + unInstallEntry);
                }
                if (packageName.equals(unInstallEntry.mPackageName)) {
                    if (callbackType == 1 || callbackType == 2) {
                        if (requestTime.equals(unInstallEntry.mRequestTime)) {
                            unInstallEntry.mResultCode = resultCode;
                            unInstallEntry.mCallbackType = callbackType;
                            findList.add(unInstallEntry);
                            if (this.DEBUG) {
                                VSlog.d(TAG, "Add to callBack list, " + unInstallEntry);
                            }
                        } else {
                            VSlog.i(TAG, packageName + " rt not equal.");
                        }
                    } else if (callbackType == 3) {
                        unInstallEntry.mResultCode = resultCode;
                        unInstallEntry.mCallbackType = callbackType;
                        findList.add(unInstallEntry);
                        VSlog.i(TAG, "remove msg " + unInstallEntry);
                        try {
                            sWorkHandler.removeMessages(MSG_FOR_ADB_UN_INSTALL_CALLBACK_TIMEOUT, unInstallEntry);
                        } catch (Exception e) {
                            VSlog.w(TAG, "remove msg catch exception " + e);
                        }
                        if (this.DEBUG) {
                            VSlog.d(TAG, "Add  " + unInstallEntry);
                        }
                    } else {
                        VSlog.w(TAG, "unknow call back type " + unInstallEntry);
                    }
                }
            }
            Iterator<VivoAdbUnInstallEntry> it2 = findList.iterator();
            while (it2.hasNext()) {
                VivoAdbUnInstallEntry entryTemp = it2.next();
                if (this.DEBUG) {
                    VSlog.d(TAG, "remove  " + entryTemp);
                }
                this.mAdbUnInstallVerifyList.remove(entryTemp);
            }
        }
        VSlog.i(TAG, "handle callbackType:" + callbackType + " packageName:" + packageName + " size:" + findList.size());
        Iterator<VivoAdbUnInstallEntry> it3 = findList.iterator();
        while (it3.hasNext()) {
            VivoAdbUnInstallEntry entryTemp2 = it3.next();
            if (entryTemp2.mVerfiyApkObserver != null) {
                if (this.DEBUG) {
                    VSlog.d(TAG, "Call observer  requestTime " + requestTime + " callbackType:" + callbackType + " resultCode:" + resultCode + " [" + entryTemp2 + "]");
                }
                try {
                    entryTemp2.mVerfiyApkObserver.onPackageDeleted(entryTemp2.mPackageName, resultCode, (String) null);
                } catch (RemoteException e2) {
                    VSlog.w(TAG, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + packageName + " remote died! ");
                } catch (Exception e3) {
                    VSlog.e(TAG, "call packge install observer catch exception " + e3.toString());
                }
            } else {
                VSlog.w(TAG, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + packageName + " not find observer ");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class WorkHandler extends Handler {
        public WorkHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            VSlog.i(VivoUninstallMgr.TAG, "--handleMessage " + msg.what);
            int i = msg.what;
            if (i == 2000) {
                VivoAdbUnInstallEntry pkmsCBEntry = (VivoAdbUnInstallEntry) msg.obj;
                VSlog.i(VivoUninstallMgr.TAG, "cb from pkms, " + pkmsCBEntry);
                VivoUninstallMgr.this.handleForAdbUnInstallObserver(pkmsCBEntry);
            } else if (i == VivoUninstallMgr.MSG_FOR_ADB_UN_INSTALL_CALLBACK_TIMEOUT) {
                VivoAdbUnInstallEntry verifyEntry = (VivoAdbUnInstallEntry) msg.obj;
                verifyEntry.mCallbackType = 2;
                verifyEntry.mResultCode = -1;
                VSlog.i(VivoUninstallMgr.TAG, "cb timeout, " + verifyEntry);
                VivoUninstallMgr.this.handleForAdbUnInstallObserver(verifyEntry);
            } else if (i != 3000) {
                switch (i) {
                    case 1000:
                        try {
                            VivoAdbUnInstallVerifyInfo info = (VivoAdbUnInstallVerifyInfo) msg.obj;
                            Uri uri = Uri.parse("package:" + info.pkgName);
                            Intent intent = new Intent("android.intent.action.DELETE", uri);
                            intent.setFlags(268435456);
                            intent.putExtra("un_install_app_sys", "adb");
                            intent.putExtra("requestTime", info.currentTime);
                            intent.putExtra("flags", info.flags);
                            if (VivoUninstallMgr.this.DEBUG_FOR_ALL) {
                                VSlog.d(VivoUninstallMgr.TAG, "start intent:" + intent);
                            }
                            VivoUninstallMgr.this.mContext.startActivity(intent);
                            return;
                        } catch (Exception e) {
                            VSlog.w(VivoUninstallMgr.TAG, "start uninstall pkg," + e.toString());
                            return;
                        }
                    case 1001:
                        return;
                    case 1002:
                        VivoUninstallMgr.this.writeVerifyValueToProp();
                        return;
                    default:
                        super.handleMessage(msg);
                        return;
                }
            } else {
                VivoAdbUnInstallEntry cancelEntry = (VivoAdbUnInstallEntry) msg.obj;
                if (VivoUninstallMgr.this.DEBUG) {
                    VSlog.d(VivoUninstallMgr.TAG, "cb from bc, " + cancelEntry);
                }
                VivoUninstallMgr.this.handleForAdbUnInstallObserver(cancelEntry);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void writeVerifyValueToProp() {
        SystemProperties.set("persist.sys.uninstall.verified", "1");
        VSlog.i(TAG, "set suc.");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class VivoAdbUnInstallVerifyBc extends BroadcastReceiver {
        public VivoAdbUnInstallVerifyBc() {
            IntentFilter filter = new IntentFilter();
            filter.addAction(VivoUninstallMgr.ACTION_ADB_UN_INSTALL_CANCEL_BC);
            filter.addAction(VivoUninstallMgr.ACTION_ADB_UN_INSTALL_VERIFY_FAIL);
            filter.addAction(VivoUninstallMgr.ACTION_ADB_UN_INSTALL_VERIFY_SUC);
            VivoUninstallMgr.this.mContext.registerReceiver(this, filter, VivoUninstallMgr.ADB_UN_INSTALL_CANCEL_BR_PERMISSION, null);
            VSlog.i(VivoUninstallMgr.TAG, "regist uninstall verify bc.");
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            VSlog.i(VivoUninstallMgr.TAG, "  intent " + intent);
            if (intent != null) {
                String action = intent.getAction();
                if (VivoUninstallMgr.ACTION_ADB_UN_INSTALL_CANCEL_BC.equals(action)) {
                    String requestTime = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + intent.getExtra("requestTime");
                    String packageName = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + intent.getExtra("packageName");
                    VSlog.i(VivoUninstallMgr.TAG, "rt:" + requestTime + " pkg:" + packageName);
                    requestTime = (requestTime == null || requestTime.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK)) ? "unknow" : "unknow";
                    Message msg = VivoUninstallMgr.sWorkHandler.obtainMessage(3000);
                    VivoAdbUnInstallEntry adbEntry = new VivoAdbUnInstallEntry(packageName, requestTime, -1, 1);
                    msg.obj = adbEntry;
                    VivoUninstallMgr.sWorkHandler.sendMessage(msg);
                } else if (VivoUninstallMgr.ACTION_ADB_UN_INSTALL_VERIFY_FAIL.equals(action)) {
                    Message msg2 = VivoUninstallMgr.sWorkHandler.obtainMessage(1001);
                    VivoUninstallMgr.sWorkHandler.sendMessage(msg2);
                } else if (VivoUninstallMgr.ACTION_ADB_UN_INSTALL_VERIFY_SUC.equals(action)) {
                    VSlog.i(VivoUninstallMgr.TAG, "verify_suc " + (Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + intent.getExtra("requestTime")) + " " + (Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + intent.getExtra("packageName")));
                    Message msg3 = VivoUninstallMgr.sWorkHandler.obtainMessage(1002);
                    VivoUninstallMgr.sWorkHandler.sendMessage(msg3);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class VivoAdbUnInstallVerifyInfo {
        public String currentTime;
        public int flags;
        public String pkgName;

        public VivoAdbUnInstallVerifyInfo(int flags, String pkgName, String currentTime) {
            this.flags = flags;
            this.pkgName = pkgName;
            this.currentTime = currentTime;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class VivoAdbUnInstallEntry {
        public int mCallbackType;
        public String mPackageName;
        public PackageSetting mPackageSetting;
        public String mRequestTime;
        public int mResultCode;
        public IPackageDeleteObserver2 mVerfiyApkObserver;

        public VivoAdbUnInstallEntry(String packageName, PackageSetting ps, IPackageDeleteObserver2 observer, String currentime) {
            this.mRequestTime = "unknow";
            this.mResultCode = -1;
            this.mCallbackType = -1;
            this.mPackageName = packageName;
            this.mPackageSetting = ps;
            this.mVerfiyApkObserver = observer;
            this.mRequestTime = currentime;
        }

        public VivoAdbUnInstallEntry(String packageName, String currentime, int result, int callbackType) {
            this.mRequestTime = "unknow";
            this.mResultCode = -1;
            this.mCallbackType = -1;
            this.mPackageName = packageName;
            this.mRequestTime = currentime;
            this.mResultCode = result;
            this.mCallbackType = callbackType;
        }

        public VivoAdbUnInstallEntry(String packageName, int result, int callbackType) {
            this.mRequestTime = "unknow";
            this.mResultCode = -1;
            this.mCallbackType = -1;
            this.mPackageName = packageName;
            this.mResultCode = result;
            this.mCallbackType = callbackType;
        }

        public String toString() {
            return "pN:" + this.mPackageName + " rt:" + this.mRequestTime + " rc:" + this.mResultCode + " cbt:" + this.mCallbackType;
        }
    }
}