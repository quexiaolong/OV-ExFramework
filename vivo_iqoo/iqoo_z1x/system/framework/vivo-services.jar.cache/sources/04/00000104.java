package com.android.server.am.firewall;

import android.app.AlertDialog;
import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import com.android.server.am.PendingIntentRecord;
import com.android.server.policy.key.VivoOTGKeyHandler;
import com.vivo.services.rms.RmsInjectorImpl;
import com.vivo.services.security.server.VivoPermissionUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import vendor.pixelworks.hardware.display.V1_0.KernelConfig;
import vivo.util.VSlog;
import vivo.util.VivoThemeUtil;

/* loaded from: classes.dex */
public class VivoBringupController {
    private static final int COUNTER_MAX = 8;
    public static final HashSet<String> CTS_SHA1_SIGNATURE;
    private static final int MSG_NOTIFY_CLIENT = 1;
    private static final String TAG = "VivoFirewall";
    private static final int TIME_INTERVAL = 1000;
    private static volatile boolean mIsFgDialogShowing = false;
    private Handler mAppBringControllerHandler;
    private Context mContext;
    private VivoIqooSecureConnector mIqooSecureConnector;
    VivoFirewall mVivoFirewall;
    private Set<String> mSpecialCtsPkgs = new HashSet();
    private LinkedList<BringupItemRecord> mRecentRecords = new LinkedList<>();
    private Pattern mCtsPattern = Pattern.compile(".*android.*cts.*");
    private Pattern mVivoUnitTestPattern = Pattern.compile(".*android.*servicestests.*");

    static {
        HashSet<String> hashSet = new HashSet<>();
        CTS_SHA1_SIGNATURE = hashSet;
        hashSet.add("61:ED:37:7E:85:D3:86:A8:DF:EE:6B:86:4B:D8:5B:0B:FA:A5:AF:81:");
        CTS_SHA1_SIGNATURE.add("38:91:8A:45:3D:07:19:93:54:F8:B1:9A:F0:5E:C6:56:2C:ED:57:88:");
        CTS_SHA1_SIGNATURE.add("3B:8E:AA:F3:2B:13:A2:5A:01:13:27:3D:D9:B1:7B:D3:29:A7:D1:94:");
        CTS_SHA1_SIGNATURE.add("60:D8:F9:F4:71:82:0D:2D:58:93:7D:65:9C:0A:17:7F:01:FE:46:14:");
        CTS_SHA1_SIGNATURE.add("27:19:6E:38:6B:87:5E:76:AD:F7:00:E7:EA:84:E4:C6:EE:E3:3D:FA:");
        CTS_SHA1_SIGNATURE.add("58:E1:C4:13:3F:74:41:EC:3D:2C:27:02:70:A1:48:02:DA:47:BA:0E:");
        CTS_SHA1_SIGNATURE.add("D6:A0:60:39:C0:21:31:21:C6:31:BA:E6:19:F6:74:00:5F:29:D6:38:");
        CTS_SHA1_SIGNATURE.add("DF:E6:EE:1C:7C:C3:9D:70:D1:20:12:90:14:75:C8:7E:8A:46:6D:F9:");
        CTS_SHA1_SIGNATURE.add("A5:D4:EC:C0:6C:48:9C:AF:49:A6:62:CF:7B:E7:7B:E1:DE:D0:95:70:");
        CTS_SHA1_SIGNATURE.add("31:73:4F:D3:A6:04:D2:1A:2B:6C:BF:C8:BF:48:C9:FA:18:F2:7F:43:");
        CTS_SHA1_SIGNATURE.add("94:DC:45:99:5A:48:16:22:E1:B6:A8:D3:1A:7F:AA:16:E1:46:14:CE:");
        CTS_SHA1_SIGNATURE.add("20:A1:A0:9C:B0:0D:7D:F5:26:7E:C0:65:3C:EF:EE:F5:D3:41:3E:D8:");
        CTS_SHA1_SIGNATURE.add("8B:DF:54:13:4F:1E:2D:11:E7:C7:93:23:83:69:48:B1:7F:1F:2E:3B:");
        CTS_SHA1_SIGNATURE.add("6E:A0:51:F2:74:4D:70:93:13:3B:96:72:80:29:5B:A3:90:CD:9F:CE:");
        CTS_SHA1_SIGNATURE.add("9B:42:4C:2D:27:AD:51:A4:2A:33:7E:0B:B6:99:1C:76:EC:A4:44:61:");
        CTS_SHA1_SIGNATURE.add("5B:DC:B7:4A:71:46:3A:77:B1:CC:A6:B5:DD:73:73:33:67:EA:F3:F1:");
        CTS_SHA1_SIGNATURE.add("0A:C5:AB:2D:28:F9:BF:41:F4:89:8C:EF:00:3F:07:15:81:BE:69:C2:");
        CTS_SHA1_SIGNATURE.add("1F:38:7C:B2:5E:00:69:EF:CA:49:0A:DE:28:C0:60:E0:9D:37:DD:45:");
        CTS_SHA1_SIGNATURE.add("2D:5F:89:6E:35:90:0D:1C:44:45:2D:99:99:BB:B4:16:46:E2:EA:2C:");
        CTS_SHA1_SIGNATURE.add("4A:29:6F:25:28:41:C1:26:E8:26:61:22:86:33:93:E1:A1:07:60:0D:");
        CTS_SHA1_SIGNATURE.add("95:7D:8B:CA:48:A3:E9:A4:56:D3:A1:AE:FE:27:93:74:5A:9A:B9:01:");
        CTS_SHA1_SIGNATURE.add("29:27:27:21:17:82:74:50:F3:CF:BF:31:5B:AB:3A:E3:FB:3B:57:27:");
        CTS_SHA1_SIGNATURE.add("28:3D:60:DD:CD:20:C5:6E:A1:71:9C:E9:05:27:F1:23:5A:E8:0E:FA:");
        CTS_SHA1_SIGNATURE.add("4E:BD:D0:23:80:F1:FA:0B:67:41:49:1F:0A:F3:56:25:DB:A7:6E:9F:");
        CTS_SHA1_SIGNATURE.add("9C:A9:1F:9E:70:4D:63:0E:F6:7A:23:F5:2B:F1:57:7A:92:B9:CA:5D:");
    }

    /* loaded from: classes.dex */
    class AppBringControllerHandler extends Handler {
        public AppBringControllerHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                VivoBringupController.this.mIqooSecureConnector.bringupIqooService(msg);
            }
        }
    }

    public VivoBringupController(Context context, VivoFirewall firewall, Looper lopper) {
        this.mContext = context;
        this.mVivoFirewall = firewall;
        this.mIqooSecureConnector = new VivoIqooSecureConnector(context, lopper);
        this.mAppBringControllerHandler = new AppBringControllerHandler(lopper);
        loadSpecialCtsPackages();
    }

    public void start() {
        this.mIqooSecureConnector.registerBroadcast();
    }

    public void systemReady() {
        Message msg = this.mAppBringControllerHandler.obtainMessage(1);
        this.mAppBringControllerHandler.sendMessage(msg);
    }

    public boolean shouldPreventStartProcess(String callerPackage, ComponentInfo bringupSide, String type, int callerPid, int callerUid) {
        return shouldPreventStartProcess(callerPackage, bringupSide, type, callerPid, callerUid, -1);
    }

    public boolean shouldPreventStartProcess(String callerPackage, ComponentInfo bringupSide, String type, int callerPid, int callerUid, int realCallerUid) {
        String callerPackage2;
        int callerUid2;
        String callerPackage3 = callerPackage;
        int callerUid3 = callerUid;
        if (bringupSide != null && this.mIqooSecureConnector.isEnable()) {
            log("callerPackage=" + callerPackage3 + ",bringupSide=" + bringupSide + ", type=" + type + ", callerPid=" + callerPid + ", callerUid=" + callerUid3 + ", realCallerUid=" + realCallerUid);
            if ((VivoFirewall.TYPE_SYSTEM_JOB.equals(type) || VivoFirewall.TYPE_SYSTEM_SYNC.equals(type) || VivoFirewall.TYPE_SYSTEM_ACCO.equals(type) || VivoFirewall.TYPE_SYSTEM_NOTI.equals(type)) && !this.mIqooSecureConnector.isSysTypeOn(type)) {
                log("Sys type " + type + " not enable,allow to bringup");
                return false;
            } else if (callerPackage3 == null && VivoFirewall.TYPE_PROVIDER.equals(type) && !this.mIqooSecureConnector.isAllProviderTypeOn()) {
                log("allow bringup provider not all type on");
                return false;
            } else if (this.mIqooSecureConnector.isInRightScreenState()) {
                if (VivoFirewall.TYPE_INSTRUMENT.equals(type) && !this.mIqooSecureConnector.getStartInstrumentSwitch()) {
                    log("start instrument not enable, allow to bring up");
                    return false;
                }
                if (callerUid3 == -1) {
                    callerUid3 = bringupSide.applicationInfo.uid;
                }
                if (isSystemInternalWhiteList(bringupSide, callerPid, callerUid3, callerPackage3)) {
                    return false;
                }
                long beginTime = SystemClock.uptimeMillis();
                if (this.mVivoFirewall.isUidRunning(bringupSide.applicationInfo.uid)) {
                    log("package " + bringupSide.applicationInfo.packageName + " running,allow to start.");
                    VivoFirewall.checkTime(beginTime, "isUidRunning");
                    return false;
                }
                VivoFirewall.checkTime(beginTime, "isUidRunning");
                if (VivoFirewall.TYPE_ACTIVITY.equals(type)) {
                    if (callerPackage3 == null) {
                        String nonSystemPackageName = getNonSystemPackageName(callerUid3);
                        callerPackage3 = nonSystemPackageName;
                        if (nonSystemPackageName == null) {
                            return false;
                        }
                    }
                    if (!this.mIqooSecureConnector.isAllActivityTypeOn()) {
                        log(type + " type not enable,allow to bringup");
                        return false;
                    }
                    ComponentName topAppComponentName = this.mVivoFirewall.getTopAppComponentName();
                    if (topAppComponentName == null) {
                        log("topAppComponentName is null.");
                        return false;
                    }
                    String topPackageName = topAppComponentName.getPackageName();
                    if (!TextUtils.isEmpty(topPackageName) && !isInActivityScreenState() && (topPackageName.equals(callerPackage3) || "com.android.systemui/com.android.systemui.chooser.ChooserActivity".equals(topAppComponentName.flattenToString()) || (realCallerUid == 1000 && topPackageName.equals(VivoPermissionUtils.OS_PKG)))) {
                        VivoIqooSecureConnector vivoIqooSecureConnector = this.mIqooSecureConnector;
                        String flattenToString = topAppComponentName.flattenToString();
                        if (vivoIqooSecureConnector.checkActivityComponentState(callerPackage3, flattenToString, bringupSide.packageName + "/" + bringupSide.name)) {
                            log("callerPackage " + callerPackage3 + " is top but " + bringupSide.packageName + "/" + bringupSide.name + " ctled.");
                            return true;
                        }
                        log("callerPackage " + callerPackage3 + " is top.");
                        return false;
                    }
                }
                if (callerUid3 == bringupSide.applicationInfo.uid) {
                    if (callerPackage3 == null) {
                        HashMap<String, VivoAppRuleItem> map = this.mIqooSecureConnector.getWhiteStartupData();
                        VivoAppRuleItem item = map.get(bringupSide.applicationInfo.packageName);
                        if (this.mIqooSecureConnector.isCallerNullTypeOn() && ((item == null || !item.isAllowToBeBringedupBySystem()) && !this.mIqooSecureConnector.checkDefaultIMEPackage(bringupSide.applicationInfo.packageName))) {
                            log("callerNullType on:" + bringupSide.applicationInfo.packageName + ": XXXX SS");
                            return true;
                        } else if (this.mIqooSecureConnector.getSpecialStartupData().contains(bringupSide.applicationInfo.packageName)) {
                            log("S List:" + bringupSide.applicationInfo.packageName + ": XXXX SS");
                            return true;
                        }
                    }
                    if (!VivoFirewall.TYPE_SYSTEM_JOB.equals(type)) {
                        log("allow self start:" + bringupSide.applicationInfo.packageName + ",uid:" + callerUid3);
                        return false;
                    }
                }
                if (callerPackage3 != null) {
                    callerPackage2 = callerPackage3;
                } else {
                    String callerPackage4 = getNonSystemPackageName(callerUid3);
                    if (callerPackage4 != null) {
                        callerPackage2 = callerPackage4;
                    } else {
                        return false;
                    }
                }
                if (!VivoFirewall.TYPE_SYSTEM_JOB.equals(type)) {
                    callerUid2 = callerUid3;
                } else {
                    log("callerUid " + callerUid3 + " is start " + bringupSide.applicationInfo.uid + "'s job");
                    callerUid2 = 1000;
                }
                if (shouldBringupApp(callerPackage2, bringupSide, type, callerPid, callerUid2)) {
                    return false;
                }
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    /* JADX WARN: Removed duplicated region for block: B:151:0x02b4 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private boolean shouldBringupApp(java.lang.String r21, android.content.pm.ComponentInfo r22, java.lang.String r23, int r24, int r25) {
        /*
            Method dump skipped, instructions count: 872
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.firewall.VivoBringupController.shouldBringupApp(java.lang.String, android.content.pm.ComponentInfo, java.lang.String, int, int):boolean");
    }

    public boolean shouldValidateSyncType(String syncType) {
        return this.mIqooSecureConnector.isForbiddenSyncType(syncType);
    }

    public boolean isSystemAppControled(String packageName) {
        return this.mIqooSecureConnector.isSystemAppControled(packageName);
    }

    public int getBringupContinuousSwitch() {
        return this.mIqooSecureConnector.getBringupContinuousSwitch();
    }

    public int checkFgActivityCtrlState(String callerPackage, int callerUid, ComponentInfo bringupComponent) {
        if (TextUtils.isEmpty(callerPackage)) {
            callerPackage = getNonSystemPackageName(callerUid);
            if (TextUtils.isEmpty(callerPackage)) {
                log(" callerPackage is null, callerUid:" + callerUid);
                return 0;
            }
        }
        if (callerPackage.equals(bringupComponent.packageName)) {
            return 0;
        }
        VivoSpecialRuleItem callerRuleItem = this.mIqooSecureConnector.getFgActivityCtrlMap().get(callerPackage);
        VivoSpecialRuleItem calledRuleItem = this.mIqooSecureConnector.getFgActivityWhiteMap().get(bringupComponent.packageName);
        if ((calledRuleItem == null || (calledRuleItem != null && ((calledRuleItem.isWhiteListEmpty() || (!calledRuleItem.isWhiteListEmpty() && !calledRuleItem.isWhiteListContains(callerPackage))) && calledRuleItem.isBlackListContains(callerPackage)))) && callerRuleItem != null) {
            if (callerRuleItem.isBlackListContains(bringupComponent.packageName)) {
                return 401;
            }
            if (callerRuleItem.isWhiteListContains(bringupComponent.packageName)) {
                return 402;
            }
        }
        return 0;
    }

    public void showFgActivityDialog(String callerPackage, String calledPackage, IIntentSender positiveSender, IIntentSender negativeSender, Handler uiHandler, int userId, IBinder token, String resultWho, int requestCode, String resolvedType) {
        if (!mIsFgDialogShowing) {
            mIsFgDialogShowing = true;
            uiHandler.post(new ShowFgDialogRunnable(this.mContext, callerPackage, calledPackage, positiveSender, negativeSender, userId, token, resultWho, requestCode, resolvedType));
        }
    }

    private boolean isInActivityScreenState() {
        if (this.mIqooSecureConnector.getActivityScreenSwitch() && this.mVivoFirewall.isScreenOff()) {
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class ShowFgDialogRunnable implements Runnable, DialogInterface.OnClickListener {
        private String calledPackage;
        private String callerPackage;
        private Context context;
        private IIntentSender negativeSender;
        private IIntentSender positiveSender;
        private int requestCode;
        private String resolvedType;
        private String resultWho;
        private IBinder token;
        private int userId;

        ShowFgDialogRunnable(Context context, String callerPackage, String calledPackage, IIntentSender positiveSender, IIntentSender negativeSender, int userId, IBinder token, String resultWho, int requestCode, String resolvedType) {
            this.context = new ContextThemeWrapper(context, VivoThemeUtil.getSystemThemeStyle(VivoThemeUtil.ThemeType.DIALOG_ALERT));
            this.callerPackage = callerPackage;
            this.calledPackage = calledPackage;
            this.positiveSender = positiveSender;
            this.negativeSender = negativeSender;
            this.userId = userId;
            this.token = token;
            this.resultWho = resultWho;
            this.requestCode = requestCode;
            this.resolvedType = resolvedType;
        }

        @Override // java.lang.Runnable
        public void run() {
            String contentStr = String.format(this.context.getString(51249797), VivoBringupController.getAppName(this.context, this.callerPackage, this.userId), VivoBringupController.getAppName(this.context, this.calledPackage, this.userId));
            AlertDialog alertDialog = new AlertDialog.Builder(this.context, VivoThemeUtil.getSystemThemeStyle(VivoThemeUtil.ThemeType.DIALOG_ALERT)).setMessage(contentStr).setPositiveButton(51249392, this).setNegativeButton(17039360, this).create();
            alertDialog.getWindow().setType(2003);
            alertDialog.getWindow().getAttributes().privateFlags |= 536870928;
            alertDialog.setCancelable(false);
            alertDialog.show();
        }

        @Override // android.content.DialogInterface.OnClickListener
        public void onClick(DialogInterface dialogInterface, int i) {
            PendingIntentRecord pir;
            VivoBringupController.log("ShowFgDialogRunnable onClick:" + i);
            if (-1 == i) {
                PendingIntentRecord pir2 = (PendingIntentRecord) this.positiveSender;
                pir = pir2;
            } else if (-2 != i) {
                pir = null;
            } else {
                PendingIntentRecord pir3 = this.negativeSender;
                pir = pir3;
            }
            if (pir != null) {
                try {
                    pir.sendInner(0, (Intent) null, this.resolvedType, (IBinder) null, (IIntentReceiver) null, (String) null, this.token, this.resultWho, this.requestCode, 0, 0, (Bundle) null);
                } catch (Exception e) {
                    VSlog.e("VivoFirewall", "Unable to launch supplied IntentSender for FG Activity.");
                }
            }
            boolean unused = VivoBringupController.mIsFgDialogShowing = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class BringupItemRecord {
        private String mCallerPackageName;
        private String mPackageName;
        private long mTime;

        BringupItemRecord(String packageName, String callerPackageName, long time) {
            this.mPackageName = packageName;
            this.mCallerPackageName = callerPackageName;
            this.mTime = time;
        }
    }

    private boolean isSystemInternalWhiteList(ComponentInfo componentInfo, int callerPid, int callerUid, String callerPackage) {
        if ((callerUid < 10000 && callerUid != 1000) || componentInfo.applicationInfo.uid < 10000 || isCtsPackage(componentInfo.applicationInfo.packageName)) {
            return true;
        }
        return false;
    }

    private boolean isCtsPackage(String packageName) {
        if (this.mSpecialCtsPkgs.contains(packageName)) {
            return true;
        }
        if (this.mCtsPattern.matcher(packageName).matches() || this.mVivoUnitTestPattern.matcher(packageName).matches()) {
            String signature = getApkSha1Signature(this.mContext, packageName);
            if (TextUtils.isEmpty(signature) || CTS_SHA1_SIGNATURE.contains(signature)) {
                log("allow cpkg:" + packageName + ", sig:" + signature);
                return true;
            }
            log("cpkg but sig not match, pkg:" + packageName + ", sig:" + signature);
            return false;
        }
        return false;
    }

    private String getNonSystemPackageName(int uid) {
        ApplicationInfo apinfo;
        try {
            IPackageManager pm = AppGlobals.getPackageManager();
            String[] packageNames = pm.getPackagesForUid(uid);
            if (packageNames != null && (apinfo = pm.getApplicationInfo(packageNames[0], 0, 0)) != null && (apinfo.flags & KernelConfig.AP_TE) == 0) {
                log("getNonSystemPackageName=" + packageNames[0]);
                return packageNames[0];
            }
            log("getNonSystemPackageName null");
            return null;
        } catch (RemoteException e) {
            log("fail to get package");
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String getAppName(Context context, String packageName, int userId) {
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfoAsUser(packageName, 64, userId);
            return pi.applicationInfo.loadLabel(context.getPackageManager()).toString();
        } catch (PackageManager.NameNotFoundException e) {
            log("Can't get calling app package info");
            return packageName;
        }
    }

    private boolean isSettingsRunningTop() {
        ComponentName topcomp = this.mVivoFirewall.getTopAppComponentName();
        log("top component:" + topcomp);
        if (topcomp == null) {
            return false;
        }
        String packageName = topcomp.getPackageName();
        if (!VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS.equals(packageName)) {
            return false;
        }
        return true;
    }

    private boolean isUidRunning(int uid) {
        long origId = Binder.clearCallingIdentity();
        boolean isRunning = this.mVivoFirewall.isUidProcessAliveUnsafe(uid, VivoPermissionUtils.OS_PKG);
        Binder.restoreCallingIdentity(origId);
        return isRunning;
    }

    private void loadSpecialCtsPackages() {
        this.mSpecialCtsPkgs.add("android.tests.devicesetup");
        this.mSpecialCtsPkgs.add("android.voicesettings");
        this.mSpecialCtsPkgs.add("android.voiceinteraction");
    }

    private void sendInterceptInfo(String callerPackageName, ComponentInfo bringupSide, String type, boolean resule, int callerPid, int callerUid, boolean overshoot) {
        if (!resule && RmsInjectorImpl.getInstance().needKeepQuiet(callerPackageName, UserHandle.getUserId(callerUid), callerUid, 32)) {
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putString("PACKAGE_NAME", bringupSide.packageName);
        bundle.putString("COMPONENT_NAME", bringupSide.name);
        bundle.putString("COMPONENT_TYPE", type);
        bundle.putBoolean("RESULT", resule);
        bundle.putBoolean("OVERSHOOT", overshoot);
        bundle.putLong("TIME", System.currentTimeMillis());
        bundle.putInt("CALLER_PID", callerPid);
        bundle.putInt("CALLER_UID", callerUid);
        bundle.putString("CALLER_PACKAGE_NAME", callerPackageName);
        log("Firewall: " + callerPackageName + "(callerPid:" + callerPid + " ,callerUid:" + callerUid + ") bring up " + bringupSide.packageName + "/" + bringupSide.name + " is allowed? " + resule);
        Message msg = this.mAppBringControllerHandler.obtainMessage(1);
        msg.setData(bundle);
        this.mAppBringControllerHandler.sendMessage(msg);
    }

    public void dumpCachedInfo(FileDescriptor fd, PrintWriter pw, String[] args, int opti) {
        this.mIqooSecureConnector.dumpCachedInfo(fd, pw, args, opti);
        Iterator<BringupItemRecord> it = this.mRecentRecords.iterator();
        while (it.hasNext()) {
            BringupItemRecord item = it.next();
            pw.println("time:" + item.mTime + ",callee:" + item.mPackageName + "|caller:" + item.mCallerPackageName);
        }
        pw.println("Special List:");
        Iterator<String> it2 = this.mIqooSecureConnector.getSpecialStartupData().iterator();
        while (it2.hasNext()) {
            String item2 = it2.next();
            pw.println("---item:" + item2);
        }
        pw.println("System List:");
        Iterator<String> it3 = this.mIqooSecureConnector.getSytemAppBlackList().iterator();
        while (it3.hasNext()) {
            String item3 = it3.next();
            pw.println("---item:" + item3);
        }
        pw.println("Other List:");
        for (String key : this.mIqooSecureConnector.getActivityComponentMap().keySet()) {
            pw.println("---" + this.mIqooSecureConnector.getActivityComponentMap().get(key));
        }
        for (String key2 : this.mIqooSecureConnector.getFgActivityCtrlMap().keySet()) {
            pw.println("---" + this.mIqooSecureConnector.getFgActivityCtrlMap().get(key2));
        }
        for (String key3 : this.mIqooSecureConnector.getFgActivityWhiteMap().keySet()) {
            pw.println("---" + this.mIqooSecureConnector.getFgActivityWhiteMap().get(key3));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void log(String msg) {
        if (VivoFirewall.DEBUG) {
            VSlog.d("VivoFirewall", msg);
        }
    }

    public static String getApkSha1Signature(Context context, String pkgName) {
        StringBuilder hexString = new StringBuilder();
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(pkgName, 64);
            byte[] cert = info.signatures[0].toByteArray();
            byte[] publicKey = MessageDigest.getInstance("SHA1").digest(cert);
            for (byte b : publicKey) {
                String appendString = Integer.toHexString(b & 255).toUpperCase(Locale.US);
                if (appendString.length() == 1) {
                    hexString.append("0");
                }
                hexString.append(appendString);
                hexString.append(":");
            }
        } catch (Exception e) {
            log("getApkSignature e is: " + e);
        }
        return hexString.toString();
    }
}