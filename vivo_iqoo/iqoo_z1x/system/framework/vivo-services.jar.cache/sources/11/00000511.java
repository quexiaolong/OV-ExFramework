package com.android.server.wm;

import android.app.ActivityOptions;
import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.Context;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Rect;
import android.hardware.graphics.common.V1_0.Dataspace;
import android.multidisplay.MultiDisplayManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;
import android.view.DisplayInfo;
import com.android.server.VCarConfigManager;
import com.android.server.VivoDoubleInstanceServiceImpl;
import com.android.server.am.AmsConfigManager;
import com.android.server.am.AmsDataManager;
import com.android.server.am.VivoAmsUtils;
import com.android.server.am.firewall.VivoAppIsolationController;
import com.android.server.am.firewall.VivoFirewall;
import com.android.server.display.VivoDisplayPowerControllerImpl;
import com.android.server.policy.key.VivoOTGKeyHandler;
import com.android.server.uri.NeededUriGrants;
import com.android.server.wm.ActivityStack;
import com.android.server.wm.LaunchParamsController;
import com.vivo.common.doubleinstance.DoubleInstanceConfig;
import com.vivo.common.utils.ChildrenModeHelper;
import com.vivo.common.utils.MotorModeHelper;
import com.vivo.face.common.state.FaceUIState;
import com.vivo.services.rms.RmsInjectorImpl;
import com.vivo.services.rms.display.SceneManager;
import com.vivo.services.security.server.VivoPermissionUtils;
import com.vivo.services.superresolution.Constant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import vendor.pixelworks.hardware.display.V1_0.KernelConfig;
import vivo.app.VivoFrameworkFactory;
import vivo.app.vperf.AbsVivoPerfManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoActivityStarterImpl implements IVivoActivityStarter {
    private static final ArrayList<String> BGSTART_WHITELIST;
    public static final int START_CHILDMODE_DENIED = 4001;
    static final String TAG = "VivoActivityStarterImpl";
    private final ActivityStarter mActivityStarter;
    private final Context mContext;
    public AbsVivoPerfManager mPerf;
    VivoAppShareManager mVivoAppShareManager;
    private VivoDoubleInstanceServiceImpl mVivoDoubleInstanceService;
    private final VivoFirewall mVivoFirewall;
    private final ActivityTaskManagerService mVivoService;
    private final VivoSoftwareLock mVivoSoftwareLock;
    private final ActivityStackSupervisor mVivoSupervisor;
    ActivityStack preTopFullscreenStack = null;
    private static boolean mIsPasswordAndRotated_90 = false;
    private static boolean mIsPasswordAndRotated_270 = false;

    static {
        ArrayList<String> arrayList = new ArrayList<>();
        BGSTART_WHITELIST = arrayList;
        arrayList.add(VivoPermissionUtils.OS_PKG);
        BGSTART_WHITELIST.add("com.android");
        BGSTART_WHITELIST.add("com.google");
        BGSTART_WHITELIST.add("com.vivo.easyshare");
        BGSTART_WHITELIST.add("com.iqoo.engineermode");
        BGSTART_WHITELIST.add("com.vivo.wallet");
    }

    public VivoActivityStarterImpl(ActivityStarter activityStarter, ActivityTaskManagerService service, ActivityStackSupervisor supervisor, Context ctx) {
        this.mVivoDoubleInstanceService = null;
        this.mPerf = null;
        if (activityStarter == null) {
            Slog.i(TAG, "container is " + activityStarter);
        }
        this.mContext = ctx;
        this.mActivityStarter = activityStarter;
        this.mVivoService = service;
        this.mVivoSupervisor = supervisor;
        this.mVivoFirewall = VivoFirewall.getInstance(ctx);
        this.mVivoDoubleInstanceService = VivoDoubleInstanceServiceImpl.getInstance();
        this.mPerf = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoPerfManager((Context) null);
        this.mVivoSoftwareLock = new VivoSoftwareLock();
        this.mVivoAppShareManager = VivoAppShareManager.getInstance();
    }

    public void dummy() {
        Slog.i(TAG, "dummy, this=" + this);
    }

    public int startActivityCheck(WindowProcessController callerProcessController, ActivityInfo activityInfo, Intent intent, String lastStartReason, int realCallingUid, String callingPackage, int callingUid, int callingPid, int err, int userId, IBinder token, String resultWho, int requestCode, String featureId, String resolvedType) {
        int errResult = shouldPreventStartActivity(callerProcessController, activityInfo, intent, lastStartReason, realCallingUid, callingPackage, callingUid, callingPid, err, userId);
        if (activityInfo != null && errResult == 0) {
            noteImportantEvent(3, activityInfo.packageName);
            thirdLifeControlActivity(callerProcessController, activityInfo, realCallingUid);
        } else if (errResult == 402) {
            IIntentSender positiveSender = this.mVivoService.getIntentSenderLocked(2, callingPackage, featureId, callingUid, userId, token, resultWho, requestCode, new Intent[]{intent}, new String[]{resolvedType}, 1342177280, (Bundle) null);
            IIntentSender negativeSender = this.mVivoService.getIntentSenderLocked(3, callingPackage, featureId, callingUid, userId, token, resultWho, requestCode, new Intent[]{intent}, new String[]{resolvedType}, 1342177280, (Bundle) null);
            this.mVivoFirewall.showFgActivityDialog(callerProcessController.mInfo.packageName, activityInfo.packageName, positiveSender, negativeSender, userId, token, resultWho, requestCode, resolvedType);
        }
        return errResult;
    }

    private int shouldPreventStartActivity(WindowProcessController callerProcessController, ActivityInfo activityInfo, Intent intent, String lastStartReason, int realCallingUid, String callingPackage, int callingUid, int callingPid, int err, int userId) {
        String str;
        String str2;
        String str3;
        String str4;
        long beginTime;
        int err2;
        long beginTime2 = SystemClock.uptimeMillis();
        if (err != 0 || intent == null || callerProcessController == null || activityInfo == null) {
            str = ": user ";
            str2 = ": XXXX";
            str3 = VivoFirewall.TAG;
            str4 = " for activity ";
            beginTime = beginTime2;
        } else if ((callerProcessController.mInfo.flags & KernelConfig.AP_TE) != 0 && !this.mVivoFirewall.isSystemAppControlled(callerProcessController.mInfo.packageName)) {
            str = ": user ";
            str2 = ": XXXX";
            str3 = VivoFirewall.TAG;
            str4 = " for activity ";
            beginTime = beginTime2;
        } else if ((activityInfo.applicationInfo.flags & KernelConfig.AP_TE) != 0 && !this.mVivoFirewall.isSystemAppControlled(activityInfo.packageName)) {
            str = ": user ";
            str2 = ": XXXX";
            str3 = VivoFirewall.TAG;
            str4 = " for activity ";
            beginTime = beginTime2;
        } else {
            beginTime = beginTime2;
            if (!this.mVivoFirewall.shouldPreventStartProcess(callingPackage, activityInfo, VivoFirewall.TYPE_ACTIVITY, callingPid, callingUid)) {
                int err3 = this.mVivoFirewall.checkActivityStart(hasLaunchingActivity(callerProcessController), callingPackage, activityInfo, VivoFirewall.TYPE_ACTIVITY, callingPid, callingUid);
                if (err3 != 0) {
                    if (err3 == 4002) {
                        VSlog.w(VivoFirewall.TAG, "==/==> " + callingPackage + "/" + callingUid + " for background activity " + intent.toShortString(true, true, true, false) + ": user " + userId + ": XXXX");
                    } else if (err3 == 402) {
                        VSlog.w(VivoFirewall.TAG, "==/==> " + callingPackage + "/" + callingUid + " for foreground ask activity " + intent.toShortString(true, true, true, false) + ": user " + userId + ": XXXX");
                    } else if (err3 == 401) {
                        VSlog.w(VivoFirewall.TAG, "==/==> " + callingPackage + "/" + callingUid + " for foreground activity " + intent.toShortString(true, true, true, false) + ": user " + userId + ": XXXX");
                    }
                } else if (this.mVivoFirewall.shouldPreventAppInteraction(callingPackage, activityInfo, VivoFirewall.TYPE_ACTIVITY, callingPid, callingUid)) {
                    Slog.w(TAG, "==/==> " + callingPackage + "/" + callingUid + " for app isolation activity " + intent.toShortString(true, true, true, false) + ": user " + userId + ": XXXX");
                    err2 = 401;
                } else if (RmsInjectorImpl.getInstance().needKeepQuiet(callingPackage, UserHandle.getUserId(callingUid), callingUid, 4) && !hasLaunchingActivity(callerProcessController)) {
                    err2 = 4002;
                }
                err2 = err3;
            } else {
                VSlog.w(VivoFirewall.TAG, "==/==> " + callingPackage + "/" + callingUid + " for activity " + intent.toShortString(true, true, true, false) + ": user " + userId + ": XXXX");
                err2 = 401;
            }
            VivoFirewall.checkTime(beginTime, "activity shouldPreventStartProcess");
            return err2;
        }
        if (err == 0 && intent != null && !intent.getIsVivoWidget() && callerProcessController == null) {
            String str5 = str3;
            if (activityInfo != null && (activityInfo.applicationInfo.flags & KernelConfig.AP_TE) == 0 && !"startActivityFromRecents".equals(lastStartReason)) {
                String str6 = str;
                String str7 = str4;
                if (this.mVivoFirewall.shouldPreventStartProcess(null, activityInfo, VivoFirewall.TYPE_ACTIVITY, callingPid, callingUid, realCallingUid)) {
                    VSlog.w(str5, "==/==> " + callingPackage + "/" + callingUid + str7 + intent.toShortString(true, true, true, false) + str6 + userId + str2);
                    err2 = 401;
                } else if (UserHandle.getAppId(callingUid) >= 10000) {
                    boolean fromShortcut = "startActivityInPackage".equals(lastStartReason);
                    if (!fromShortcut && this.mVivoFirewall.shouldPreventActivityStart(false, callingPackage, activityInfo, VivoFirewall.TYPE_ACTIVITY, callingPid, callingUid)) {
                        VSlog.w(str5, "==/==> " + callingPackage + "/" + callingUid + " for background alarm activity " + intent.toShortString(true, true, true, false) + str6 + userId + str2);
                        err2 = VivoDisplayPowerControllerImpl.COLOR_FADE_ANIMATION_VERTICAL;
                    } else if (!fromShortcut && RmsInjectorImpl.getInstance().needKeepQuiet(callingPackage, UserHandle.getUserId(callingUid), callingUid, 4)) {
                        err2 = VivoDisplayPowerControllerImpl.COLOR_FADE_ANIMATION_VERTICAL;
                    }
                }
                VivoFirewall.checkTime(beginTime, "activity shouldPreventStartProcess");
                return err2;
            }
        }
        err2 = err;
        VivoFirewall.checkTime(beginTime, "activity shouldPreventStartProcess");
        return err2;
    }

    public int startActivityCheckForCM(WindowProcessController callerProcessController, ActivityInfo activityInfo, Intent intent, String lastStartReason, int realCallingUid, String callingPackage, int callingUid, int callingPid, int err, int userId, String featureId, String resolvedType) {
        if (intent == null || shouldIgnoreChildrenModeByAppShareLocked(callingPackage, realCallingUid, intent)) {
            return 0;
        }
        ChildrenModeHelper helper = ChildrenModeHelper.getInstance(this.mContext, 0);
        MotorModeHelper motorModeHelper = MotorModeHelper.getInstance(this.mContext, 0);
        ComponentName comp = intent.getComponent();
        if (comp == null && intent.getSelector() != null) {
            comp = intent.getSelector().getComponent();
        }
        if (comp != null) {
            if (helper.filter(comp.getPackageName(), comp.getClassName(), callingPackage) && motorModeHelper.filter(comp.getPackageName(), comp.getClassName(), callingPackage)) {
                return 0;
            }
            VSlog.d(TAG, "children mode forbid start : " + comp);
            return 4001;
        }
        return 0;
    }

    public boolean checkBgActivityInWhiteList(ActivityInfo activityInfo, String callingPackage, int callingUid) {
        return this.mVivoFirewall.checkBgActivityInWhiteList(activityInfo, callingPackage, callingUid);
    }

    private void thirdLifeControlActivity(WindowProcessController callerProcessRecord, ActivityInfo activityInfo, int realCallingUid) {
        String callerPkg = null;
        int callerUid = realCallingUid;
        if (callerProcessRecord != null && callerProcessRecord.mInfo != null) {
            callerPkg = callerProcessRecord.mInfo.packageName;
            callerUid = callerProcessRecord.mInfo.uid;
        }
        if (activityInfo != null) {
            String calledPkg = activityInfo.packageName;
            String calledClassName = activityInfo.name;
            int calledUid = activityInfo.applicationInfo.uid;
            this.mVivoFirewall.sendThirdLifeControlIntent(callerPkg, callerUid, calledPkg, calledUid, calledClassName, VivoFirewall.TYPE_ACTIVITY);
        }
    }

    private void noteImportantEvent(int eventType, String packageName) {
        this.mVivoFirewall.noteImportantEvent(eventType, packageName);
    }

    private boolean hasLaunchingActivity(WindowProcessController callerApp) {
        if (callerApp != null) {
            return callerApp.isInterestingToUserLockedForVivo();
        }
        return false;
    }

    private int getSameActivityCountInTask(ActivityRecord startingActivity) {
        int count = 0;
        if (startingActivity == null || startingActivity.mActivityComponent == null || startingActivity.getTask() == null) {
            return 1;
        }
        for (int i = 0; i < startingActivity.getTask().getChildCount(); i++) {
            ActivityRecord activityRecord = startingActivity.getTask().getChildAt(i).asActivityRecord();
            if (activityRecord != null && activityRecord.mActivityComponent.equals(startingActivity.mActivityComponent)) {
                count++;
            }
        }
        return count;
    }

    private int getSameFinishingActivityCountInTask(ActivityRecord startingActivity) {
        int count = 0;
        if (startingActivity == null || startingActivity.mActivityComponent == null || startingActivity.getTask() == null) {
            if (startingActivity == null) {
                VSlog.d(TAG, "getSameFinishingActivityCountInTask startingActivity is null");
                return 1;
            } else {
                VSlog.d(TAG, "mActivityComponent : " + startingActivity.mActivityComponent + " task:" + startingActivity.getTask());
                return 1;
            }
        }
        for (int i = 0; i < startingActivity.getTask().getChildCount(); i++) {
            ActivityRecord activityRecord = startingActivity.getTask().getChildAt(i).asActivityRecord();
            if (activityRecord != null && activityRecord.finishing && activityRecord.mActivityComponent.equals(startingActivity.mActivityComponent)) {
                count++;
            }
        }
        return count;
    }

    private boolean packageInWhiteList(ActivityRecord startingActivity) {
        if (Constant.APP_WEIXIN.equals(startingActivity.packageName)) {
            return true;
        }
        return false;
    }

    public int shouldAbortWhenTooManyActivitiesInTask(ActivityRecord startingActivity, ActivityOptions activityOption) {
        if (!this.mVivoService.mAmInternal.isInActivityNumControl()) {
            VSlog.d(TAG, "isInActivityNumControl false ");
            return 0;
        } else if (packageInWhiteList(startingActivity)) {
            return 0;
        } else {
            Task taskRecord = startingActivity.getTask();
            if (taskRecord != null && taskRecord.getChildCount() >= 20) {
                VSlog.d(TAG, "too many activities in task: " + taskRecord);
            }
            if (getSameFinishingActivityCountInTask(startingActivity) >= this.mVivoService.mAmInternal.getMaxSameActivitiesInTask()) {
                VSlog.d(TAG, "abort start " + startingActivity);
                ActivityOptions.abort(activityOption);
                return 102;
            } else if (taskRecord == null || taskRecord.getChildCount() < this.mVivoService.mAmInternal.getMaxActiviesInTask() || getSameActivityCountInTask(startingActivity) < this.mVivoService.mAmInternal.getMaxSameActivitiesInTask()) {
                return 0;
            } else {
                VSlog.d(TAG, "too many activities in one task, clear task");
                taskRecord.performClearTask("too many activities in one task");
                ActivityOptions.abort(activityOption);
                return 102;
            }
        }
    }

    public void setVivoActivityControllerTimeout() {
        VivoAmsUtils.setVivoActivityControllerTimeout();
    }

    public void cancelVivoActivityControllerTimeout() {
        VivoAmsUtils.cancelVivoActivityControllerTimeout();
    }

    private boolean isDoubleAppPackageExist(String packageName) {
        IPackageManager iPackageManager = AppGlobals.getPackageManager();
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl != null && vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable() && this.mVivoDoubleInstanceService.isDoubleAppUserExist()) {
            int doubleAppUserId = this.mVivoDoubleInstanceService.getDoubleAppUserId();
            long callingUid = Binder.clearCallingIdentity();
            try {
                isExist = iPackageManager.isPackageAvailable(packageName, doubleAppUserId);
                Binder.restoreCallingIdentity(callingUid);
            } catch (RemoteException e) {
                if (this.mVivoDoubleInstanceService.isDoubleInstanceDebugEnable()) {
                    VSlog.d(TAG, "RemoteException when invoke isPackageAvailable");
                }
                Binder.restoreCallingIdentity(callingUid);
                e.printStackTrace();
            }
            if (this.mVivoDoubleInstanceService.isDoubleInstanceDebugEnable()) {
                VSlog.d(TAG, "is package:" + packageName + " double app exist:" + isExist + ", mDoubleAppUserId: " + doubleAppUserId);
            }
        }
        return isExist;
    }

    private List<String> getLauncherPkgs() {
        List<String> names = new ArrayList<>();
        IPackageManager iPackageManager = AppGlobals.getPackageManager();
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        long callingUid = Binder.clearCallingIdentity();
        try {
            List<ResolveInfo> resolveInfo = iPackageManager.queryIntentActivities(intent, intent.resolveTypeIfNeeded(this.mVivoService.mContext.getContentResolver()), (int) Dataspace.STANDARD_BT709, 0).getList();
            Binder.restoreCallingIdentity(callingUid);
            for (ResolveInfo ri : resolveInfo) {
                names.add(ri.activityInfo.packageName);
            }
        } catch (Exception e) {
            VSlog.d(TAG, "RemoteException when invoke queryIntentActivities");
            Binder.restoreCallingIdentity(callingUid);
            e.printStackTrace();
        }
        return names;
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Unreachable block: B:122:0x0208
        	at jadx.core.dex.visitors.blocks.BlockProcessor.checkForUnreachableBlocks(BlockProcessor.java:81)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.processBlocksTree(BlockProcessor.java:47)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.visit(BlockProcessor.java:39)
        */
    public android.content.pm.ActivityInfo checkActivityInfo(android.content.Intent r21, android.content.pm.ActivityInfo r22, int r23, java.lang.String r24, java.lang.String r25) {
        /*
            Method dump skipped, instructions count: 612
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.VivoActivityStarterImpl.checkActivityInfo(android.content.Intent, android.content.pm.ActivityInfo, int, java.lang.String, java.lang.String):android.content.pm.ActivityInfo");
    }

    public boolean checkDoubleAppResolverActivity(ActivityInfo aInfo, int userId, int callingUid) {
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl == null || !vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable() || !this.mVivoDoubleInstanceService.isDoubleAppUserExist() || aInfo == null) {
            return false;
        }
        if (999 != userId && userId != 0) {
            return false;
        }
        if (this.mVivoDoubleInstanceService.isDoubleInstanceDebugEnable()) {
            VSlog.d(TAG, "check ActivityInfo");
            VSlog.d(TAG, "callingUid:" + callingUid + ", called userId:" + userId);
        }
        DoubleInstanceConfig config = DoubleInstanceConfig.getInstance();
        ArrayList<String> doubleAppPkgNamesWhiteList = config.getSupportedAppPackageName();
        boolean isNeedCheck = doubleAppPkgNamesWhiteList.contains(aInfo.packageName);
        return isNeedCheck;
    }

    public boolean isCallBackActivity(ActivityInfo aInfo, String callingPkgName, String callingActivityName) {
        if (this.mVivoDoubleInstanceService.isDoubleInstanceDebugEnable()) {
            VSlog.d(TAG, "callingPkgName:" + callingPkgName + ",callingActivityName:" + callingActivityName);
            VSlog.d(TAG, "calledPkgName:" + aInfo.packageName + ",calledActivityName:" + aInfo.name);
        }
        ArrayList<String> callBackActivityNames = DoubleInstanceConfig.getInstance().getActivityWithoutChooser();
        synchronized (DoubleInstanceConfig.getInstance()) {
            Iterator<String> it = callBackActivityNames.iterator();
            while (it.hasNext()) {
                String callBackActivityName = it.next();
                if (callBackActivityName.equals(aInfo.name) && aInfo.packageName != null && !aInfo.packageName.equals(callingPkgName)) {
                    VSlog.d(TAG, "just return");
                    return true;
                }
            }
            return false;
        }
    }

    public int startActivityMayWaitForDoubleInstance(Intent intent, String callingPackage, int userId, int realCallingUid) {
        int userIdTmp = userId;
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl != null && vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable() && 999 == userId) {
            if (intent.getComponent() != null || intent.getPackage() != null) {
                ArrayList<String> doubleAppPkgNamesWhiteList = DoubleInstanceConfig.getInstance().getSupportedAppPackageName();
                ArrayList<String> systemAppInDoubleUser = DoubleInstanceConfig.getInstance().getSystemAppInDoubleUser();
                boolean isDoubleAppCalling = false;
                boolean isSelfCalling = true;
                boolean isSystemAppInDoubleUserCalled = false;
                synchronized (DoubleInstanceConfig.getInstance()) {
                    Iterator<String> it = doubleAppPkgNamesWhiteList.iterator();
                    while (it.hasNext()) {
                        String doubleAppPkgName = it.next();
                        if (doubleAppPkgName.equals(callingPackage)) {
                            isDoubleAppCalling = true;
                        }
                    }
                    Iterator<String> it2 = systemAppInDoubleUser.iterator();
                    while (it2.hasNext()) {
                        String pkgName = it2.next();
                        if ((intent.getComponent() != null && intent.getComponent().getPackageName().equals(pkgName)) || (intent.getPackage() != null && intent.getPackage().equals(pkgName))) {
                            isSystemAppInDoubleUserCalled = true;
                            break;
                        }
                    }
                }
                if ((intent.getComponent() != null && !intent.getComponent().getPackageName().equals(callingPackage)) || (intent.getPackage() != null && !intent.getPackage().equals(callingPackage))) {
                    isSelfCalling = false;
                }
                if ("com.whatsapp".equals(intent.getPackage()) && "com.instagram.android".equals(callingPackage)) {
                    isSelfCalling = true;
                    intent.fixUris(userId);
                }
                if ("jp.naver.line.android".equals(callingPackage) && "com.google.android.gms".equals(intent.getPackage())) {
                    isSystemAppInDoubleUserCalled = true;
                }
                if ("android.intent.action.VIEW".equals(intent.getAction()) && "com.google.android.packageinstaller".equals(intent.getPackage())) {
                    isSystemAppInDoubleUserCalled = false;
                }
                if (isDoubleAppCalling && !isSelfCalling && !isSystemAppInDoubleUserCalled) {
                    if (this.mVivoDoubleInstanceService.isDoubleInstanceDebugEnable()) {
                        VSlog.d(TAG, "dobule app calling nonself activity");
                    }
                    userIdTmp = 0;
                }
                if (intent.getComponent() != null && "com.android.systemui.chooser.ChooserActivity".equals(intent.getComponent().getClassName())) {
                    return 0;
                }
                return userIdTmp;
            } else if ("android.intent.action.VIEW".equals(intent.getAction()) && "market".equals(intent.getScheme())) {
                return 0;
            } else {
                if ("android.intent.action.CHOOSER".equals(intent.getAction())) {
                    Intent extraIntent = (Intent) intent.getParcelableExtra("android.intent.extra.INTENT");
                    if (extraIntent != null && "com.whatsapp.action.WHATSAPP_RECORDING".equals(extraIntent.getAction())) {
                        extraIntent.putExtra("userId", userId);
                        return 0;
                    }
                    return userIdTmp;
                } else if ("android.intent.action.INSERT_OR_EDIT".equals(intent.getAction()) && "vnd.android.cursor.item/contact".equals(intent.getType())) {
                    return 0;
                } else {
                    if ("android.intent.action.INSERT".equals(intent.getAction()) && "vnd.android.cursor.dir/raw_contact".equals(intent.getType())) {
                        return 0;
                    }
                    if (("android.settings.MANAGE_APPLICATIONS_SETTINGS".equals(intent.getAction()) || "android.settings.APPLICATION_DETAILS_SETTINGS".equals(intent.getAction()) || "android.settings.action.MANAGE_WRITE_SETTINGS".equals(intent.getAction()) || "android.settings.NFC_SETTINGS".equals(intent.getAction())) && "no".equals(SystemProperties.get("ro.vivo.product.overseas", "no"))) {
                        return 0;
                    }
                    if ("android.settings.action.MANAGE_OVERLAY_PERMISSION".equals(intent.getAction()) && "no".equals(SystemProperties.get("ro.vivo.product.overseas", "no"))) {
                        return 0;
                    }
                    if ("android.settings.MANAGE_UNKNOWN_APP_SOURCES".equals(intent.getAction()) && "no".equals(SystemProperties.get("ro.vivo.product.overseas", "no"))) {
                        return 0;
                    }
                    if (("android.settings.CHANNEL_NOTIFICATION_SETTINGS".equals(intent.getAction()) || "android.settings.APP_NOTIFICATION_SETTINGS".equals(intent.getAction())) && "no".equals(SystemProperties.get("ro.vivo.product.overseas", "no"))) {
                        intent.putExtra("extra_double_app_uid", realCallingUid);
                        return 0;
                    } else if (Constant.APP_WEIXIN.equals(callingPackage) && "android.intent.action.MAIN".equals(intent.getAction()) && intent.getCategories() != null && intent.getCategories().contains("android.intent.category.HOME")) {
                        return 0;
                    } else {
                        if ("com.viber.voip".equals(callingPackage) && "android.intent.action.CREATE_DOCUMENT".equals(intent.getAction()) && intent.getCategories() != null && intent.getCategories().contains("android.intent.category.OPENABLE")) {
                            return 0;
                        }
                        return userIdTmp;
                    }
                }
            }
        } else if ("com.whatsapp.action.WHATSAPP_RECORDING".equals(intent.getAction())) {
            return intent.getIntExtra("userId", 0);
        } else {
            return userIdTmp;
        }
    }

    public void startActivityForDoubleInstance(Intent intent, String callingPackage, int callingUid) {
        Uri newUri;
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl != null && vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable() && UserHandle.getUserId(callingUid) == 999) {
            if (intent != null && intent.getAction() != null && intent.getAction().equals("android.intent.action.VIEW") && intent.getData() != null && intent.getData().getAuthority() != null && (intent.getData().getAuthority().equals("com.whatsapp.provider.media") || intent.getData().getAuthority().equals("com.bbm.fileprovider"))) {
                intent.setDataAndType(ContentProvider.maybeAddUserId(intent.getData(), UserHandle.getUserId(callingUid)), intent.getType());
            } else if (intent != null && intent.getAction() != null && intent.getAction().equals("android.intent.action.SEND") && intent.getExtras() != null && !"jp.naver.line.android".equals(callingPackage) && intent.getExtras().containsKey("android.intent.extra.STREAM")) {
                Bundle newBundle = intent.getExtras();
                Uri newUri2 = (Uri) newBundle.getParcelable("android.intent.extra.STREAM");
                if (newUri2 != null && newUri2.getAuthority() != null && newUri2.getAuthority().equals("com.whatsapp.provider.media")) {
                    intent.putExtra("android.intent.extra.STREAM", ContentProvider.maybeAddUserId(newUri2, UserHandle.getUserId(callingUid)));
                }
            } else if (intent != null && (("android.intent.action.VIEW".equals(intent.getAction()) || "com.tencent.QQBrowser.action.sdk.document".equals(intent.getAction())) && intent.getData() != null)) {
                Uri newUri3 = intent.getData();
                if (newUri3 != null) {
                    if ("com.tencent.mm.external.fileprovider".equals(newUri3.getAuthority()) || "org.telegram.messenger.provider".equals(newUri3.getAuthority()) || "com.tencent.mobileqq.fileprovider".equals(newUri3.getAuthority())) {
                        intent.fixUris(UserHandle.getUserId(callingUid));
                    }
                }
            } else if (intent != null && "com.android.camera.action.CROP".equals(intent.getAction()) && intent.getData() != null && (newUri = intent.getData()) != null && "com.xunmeng.pinduoduo.pdd.fileProvider".equals(newUri.getAuthority())) {
                intent.fixUris(UserHandle.getUserId(callingUid));
            }
        }
    }

    public boolean isDoubleInstanceEnable() {
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl != null) {
            return vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable();
        }
        return false;
    }

    public boolean setWillCallContactsFromFreefromMms(Intent intent, String callingPackage) {
        if (this.mVivoService.isVivoFreeFormValid() && this.mVivoService.isInVivoFreeform() && this.mVivoService.isInDirectFreeformState()) {
            this.mVivoSupervisor.setWillCallContactsFromFreefromMms(willCallContactsFromFreefromMms(intent, callingPackage));
            return true;
        }
        return false;
    }

    private boolean willCallContactsFromFreefromMms(Intent intent, String callingPackage) {
        ActivityRecord freeformTop;
        if (this.mVivoSupervisor.isWillCallContactsFromFreefromMms()) {
            return true;
        }
        if (intent == null || callingPackage == null || !callingPackage.equals("com.android.mms") || (freeformTop = this.mVivoSupervisor.getFreeformTopActivity()) == null) {
            return false;
        }
        return freeformTop.packageName.equals(callingPackage) && intent.toString().contains("com.vivo.contacts");
    }

    public boolean setWillCallContactsFromPasswd(Intent intent, String callingPackage) {
        if (this.mVivoService.isVivoFreeFormValid() && this.mVivoService.isInVivoFreeform() && this.mVivoService.isInDirectFreeformState()) {
            this.mVivoSupervisor.setWillCallContactsFromPasswd(willCallContactsFromPasswd(intent, callingPackage));
            return true;
        }
        return false;
    }

    private boolean willCallContactsFromPasswd(Intent intent, String callingPackage) {
        if (this.mVivoSupervisor.isWillCallContactsFromPasswd()) {
            return true;
        }
        if (intent == null || callingPackage == null || !callingPackage.equals(VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS)) {
            return false;
        }
        return intent.toString().contains("com.vivo.contacts");
    }

    public boolean ignoreUseActivityControlerToIntercept(ActivityOptions checkedOptions, Intent intent) {
        if (this.mVivoService.isVivoFreeFormValid() && checkedOptions != null && 5 == checkedOptions.getLaunchWindowingMode()) {
            if (intent != null && intent.toString().contains("SOFTWARE_LOCK_PASSWORD")) {
                checkedOptions.setLaunchWindowingMode(0);
                checkedOptions.setLaunchBounds(new Rect(0, 0, 0, 0));
                return true;
            }
            return true;
        }
        return false;
    }

    public void setLaunchModeForPasswdActivityInFreeform(Intent intent, ResolveInfo rInfo, ActivityInfo aInfo) {
        if (this.mVivoService.isVivoFreeFormValid() && intent != null && intent.toString().contains("com.android.settings/com.vivo.settings.secret.PasswordActivity") && rInfo != null && rInfo.activityInfo != null && aInfo != null && rInfo.activityInfo.launchMode == 0) {
            rInfo.activityInfo.launchMode = 2;
            aInfo.launchMode = 2;
        }
    }

    public boolean toggleToFreeformIfNeed(ActivityOptions options, ActivityRecord r) {
        ActivityRecord top;
        if (this.mVivoService.isVivoFreeFormValid() && this.mVivoService.isInDirectFreeformState() && options != null && 5 == options.getLaunchWindowingMode() && (top = this.mVivoService.mRootWindowContainer.getTopResumedActivity()) != null && top.mActivityComponent.equals(r.mActivityComponent) && top.mUserId == r.mUserId && !top.inFreeformWindowingMode()) {
            int rotation = top.getDisplay().getDisplayInfo().rotation;
            if (rotation == 0 || !"com.android.bbkcalculator".equals(top.packageName)) {
                top.getTask().mLastNonFullscreenBounds = options.getLaunchBounds();
                IBinder token = top.appToken.asBinder();
                this.mVivoService.toggleFreeformWindowingMode(token);
                return true;
            }
            return false;
        }
        return false;
    }

    public void verifyBoundsAndSetPasswdRotateState(ActivityOptions options, ActivityRecord r) {
        DisplayContent displayContent = this.mVivoService.mRootWindowContainer.getTopFocusedDisplayContent();
        if (r != null && r.isPasswordActivity() && displayContent != null) {
            int orientation = this.mVivoService.getDisplayRotation(displayContent);
            if (orientation == 3) {
                mIsPasswordAndRotated_270 = true;
                mIsPasswordAndRotated_90 = false;
            } else if (orientation == 1) {
                mIsPasswordAndRotated_270 = false;
                mIsPasswordAndRotated_90 = true;
            } else {
                mIsPasswordAndRotated_270 = false;
                mIsPasswordAndRotated_90 = false;
            }
        } else if (this.mVivoService.isVivoFreeFormValid() && options != null && 5 == options.getLaunchWindowingMode()) {
            if (r != null && r.isForceFullScreenForFreeForm()) {
                options.setLaunchBounds(null);
            }
            if (mIsPasswordAndRotated_270 || mIsPasswordAndRotated_90) {
                verifyFreeFormBounds(options);
            }
        } else {
            mIsPasswordAndRotated_270 = false;
            mIsPasswordAndRotated_90 = false;
        }
    }

    private boolean isStatusBarVisible(Context ctx) {
        return Settings.System.getInt(ctx.getContentResolver(), "vivo_fullscreen_flag", 0) == 0;
    }

    private void verifyFreeFormBounds(ActivityOptions opt) {
        Rect targetFreeFormStackBounds;
        int statusBarHeight;
        if (opt != null && (targetFreeFormStackBounds = opt.getLaunchBounds()) != null) {
            DisplayInfo displayInfo = this.mVivoService.mWindowManager.getDefaultDisplayContentLocked().getDisplayInfo();
            Rect curFreeformSize = new Rect(targetFreeFormStackBounds);
            this.mVivoService.mWindowManager.scaleFreeformBack(curFreeformSize);
            int left = curFreeformSize.left;
            int top = curFreeformSize.top;
            int middleX = (curFreeformSize.width() / 2) + left;
            int middleY = (curFreeformSize.height() / 2) + top;
            int rotatedMiddleX = 0;
            int rotatedMiddleY = 0;
            if (mIsPasswordAndRotated_270) {
                rotatedMiddleX = middleY;
                rotatedMiddleY = displayInfo.logicalHeight - middleX;
                mIsPasswordAndRotated_270 = false;
            } else if (mIsPasswordAndRotated_90) {
                rotatedMiddleX = displayInfo.logicalWidth - middleY;
                rotatedMiddleY = middleX;
                mIsPasswordAndRotated_90 = false;
            }
            if (rotatedMiddleX == 0 && rotatedMiddleY == 0) {
                return;
            }
            int rotatedLeft = rotatedMiddleX - (curFreeformSize.width() / 2);
            int rotatedTop = rotatedMiddleY - (curFreeformSize.height() / 2);
            if (rotatedTop < 0) {
                rotatedTop = 0;
            }
            if (isStatusBarVisible(this.mContext) && rotatedTop < (statusBarHeight = this.mContext.getResources().getDimensionPixelSize(17105488))) {
                rotatedTop = statusBarHeight;
            }
            if (rotatedLeft < 0) {
                rotatedLeft = 0;
            }
            if (curFreeformSize.width() + rotatedLeft > displayInfo.logicalWidth) {
                rotatedLeft = displayInfo.logicalWidth - curFreeformSize.width();
            }
            curFreeformSize.set(rotatedLeft, rotatedTop, targetFreeFormStackBounds.width() + rotatedLeft, targetFreeFormStackBounds.height() + rotatedTop);
            opt.setLaunchBounds(curFreeformSize);
        }
    }

    public boolean preventStartHomeForVivoFreeform(ActivityRecord r, ActivityRecord sourceRecord) {
        Task sourceTask;
        if (this.mVivoService.mWindowManager.isVivoFreeformFeatureSupport() && this.mVivoService.mWindowManager.isExitingFreeForm() && r != null && r.isActivityTypeHome() && sourceRecord != null && (sourceTask = sourceRecord.getTask()) != null && sourceTask.isFromVivoFreeform() && Constant.APP_WEIXIN.equals(sourceRecord.packageName)) {
            return true;
        }
        return false;
    }

    public void startActivityUncheckedInVivoFreeform(ActivityRecord sourceRecord, ActivityRecord startActivity, Intent intent) {
        ActivityRecord freeformTopActivity;
        boolean inVivoFreeform = this.mVivoService.isVivoFreeFormValid() && this.mVivoService.isInVivoFreeform();
        if (!inVivoFreeform || startActivity == null) {
            return;
        }
        ActivityStack freeformStack = this.mVivoService.mRootWindowContainer.getVivoFreeformStack();
        TaskDisplayArea taskDisplayArea = freeformStack != null ? freeformStack.getDisplayArea() : null;
        if (freeformStack == null || taskDisplayArea == null) {
            return;
        }
        if ((this.mVivoService.isKeyguardLocked() || ("com.android.camera".equals(startActivity.packageName) && FaceUIState.PKG_SYSTEMUI.equals(startActivity.launchedFromPackage))) && ((freeformTopActivity = this.mVivoSupervisor.getFreeformTopActivity()) == null || !freeformTopActivity.packageName.equals(startActivity.packageName))) {
            taskDisplayArea.moveVivoFreeformTasksToFullscreenStackLocked(false);
        }
        if (intent.hasCategory("android.intent.category.HOME") && (!this.mVivoService.isMultiDisplyPhone() || this.mVivoService.getFocusedDisplayId() == 0)) {
            if (this.mVivoService.isInDirectFreeformState()) {
                ActivityStack fullScreenStack = taskDisplayArea.getStack(1, 1);
                ActivityRecord resumeActivity = fullScreenStack != null ? fullScreenStack.getResumedActivity() : null;
                if (resumeActivity != null && (resumeActivity.isPasswordActivity() || resumeActivity.isPasswordAuthenActivity())) {
                    taskDisplayArea.moveVivoFreeformTasksToFullscreenStackLocked(false);
                }
            } else {
                taskDisplayArea.moveVivoFreeformTasksToFullscreenStackLocked(false);
            }
        }
        ActivityRecord homeActivity = taskDisplayArea.getHomeActivity();
        ActivityRecord freeformTop = freeformStack.topRunningActivityLocked();
        this.mVivoService.setIsStartingRecentOnHome(false);
        if (this.mVivoService.isInDirectFreeformState() && homeActivity != null && homeActivity.nowVisible && freeformTop != null && freeformTop.nowVisible && startActivity.isActivityTypeRecents()) {
            this.mVivoService.setIsStartingRecentOnHome(true);
        }
        if (freeformTop != null && freeformTop.nowVisible && startActivity.isActivityTypeRecents()) {
            this.mVivoService.mWindowManager.setStartingRecent(true);
        }
        if (intent.hasCategory("android.intent.category.HOME")) {
            this.mVivoService.mWindowManager.setStartingRecentBreakAdjust(false);
        }
        if (startActivity.isEmergentActivity()) {
            this.mVivoService.setIsStartingEmergent(true);
        }
    }

    public void setStartPasswdOnHomeIfNeed(ActivityRecord startActivity) {
        if (this.mVivoService.isVivoFreeFormValid() && this.mVivoService.isInDirectFreeformState() && startActivity.isPasswordActivity()) {
            TaskDisplayArea taskDisplayArea = this.mVivoSupervisor.mRootWindowContainer.getDefaultTaskDisplayArea();
            ActivityRecord homeActivity = taskDisplayArea != null ? taskDisplayArea.getHomeActivity() : null;
            if (homeActivity != null && homeActivity.nowVisible) {
                this.mVivoService.setIsStartingPasswdOnHome(true);
            }
        }
    }

    public void getTopFullscreenStack() {
        ActivityStack freeformStack;
        this.preTopFullscreenStack = null;
        if (this.mVivoService.isVivoFreeFormValid() && this.mVivoService.isInVivoFreeform() && (freeformStack = this.mVivoService.mRootWindowContainer.getVivoFreeformStack()) != null && freeformStack.getDisplayArea() != null) {
            this.preTopFullscreenStack = freeformStack.getDisplayArea().getStack(1, 1);
        }
    }

    public boolean ignoreResetTaskIntentFlagInFreeform(ActivityRecord startActivity) {
        if (this.mVivoService.isVivoFreeFormValid() && this.mVivoService.isInVivoFreeform() && startActivity.getWindowingMode() == 5) {
            VSlog.d(TAG, "ignoreResetTaskIntentFlagInFreeform startActivity = " + startActivity);
            return true;
        }
        return false;
    }

    public void setLaunchFlagForPasswdInFreeform(ActivityRecord startActivity) {
        if (this.mVivoService.isVivoFreeFormValid() && startActivity.isPasswordActivity() && (this.mActivityStarter.mLaunchFlags & 67108864) != 0) {
            this.mActivityStarter.mLaunchFlags &= -67108865;
        }
    }

    public void updateLastNonFullscreenBoundsForFreeform(Task targetTask, int preferredWindowingMode, ActivityOptions options) {
        if (options != null && options.getLaunchWindowingMode() == 5 && targetTask != null) {
            Rect restBounds = options.getLaunchBounds();
            ActivityRecord top = targetTask.topRunningActivityLocked();
            if (top != null && top.isForceFullScreenForFreeForm()) {
                targetTask.mLastNonFullscreenBounds.setEmpty();
                int oldRotation = -1;
                if (top.getDisplay() != null) {
                    oldRotation = this.mVivoService.getDisplayRotation(top.getDisplay());
                }
                if (oldRotation == 1 || oldRotation == 3) {
                    top.updateRequestOverrideConfigUseBounds(targetTask.getBounds());
                    this.mVivoService.mStackSupervisor.setRestoreOverideConfigWhenEnterToMax(true);
                }
                this.mVivoService.mStackSupervisor.setbackLastBoundsWhenEnterToMax(restBounds);
            } else if (restBounds != null) {
                targetTask.mLastNonFullscreenBounds = new Rect(restBounds);
            }
        }
    }

    public void checkAndExitFreeformModeIfNeeded(ActivityStack targetStack, ActivityRecord startActivity) {
        ActivityStack activityStack;
        boolean inVivoFreeform = this.mVivoService.isVivoFreeFormValid() && this.mVivoService.isInVivoFreeform();
        if (inVivoFreeform && targetStack != null && startActivity != null && targetStack.getWindowingMode() == 1 && targetStack != (activityStack = this.preTopFullscreenStack) && activityStack != null && !startActivity.isEmergentActivity()) {
            ActivityStack freeformStack = this.mVivoSupervisor.mRootWindowContainer.getVivoFreeformStack();
            DisplayContent displayContent = targetStack.getDisplay();
            boolean targetStackInSecondDisplay = false;
            if (this.mVivoService.isMultiDisplyPhone() && displayContent != null && displayContent.mDisplayId != 0) {
                targetStackInSecondDisplay = true;
            }
            if (freeformStack != null && freeformStack.getDisplayArea() != null && freeformStack.getDisplayArea() == targetStack.getDisplayArea() && !targetStackInSecondDisplay && !this.mVivoService.isInDirectFreeformState()) {
                freeformStack.getDisplayArea().moveVivoFreeformTasksToFullscreenStackLocked(false);
            }
        }
    }

    public int processSpecialActivitiesInFreeform(ActivityRecord r, ActivityRecord top, ActivityRecord startActivity) {
        boolean inVivoFreeform = this.mVivoService.isVivoFreeFormValid() && this.mVivoService.isInVivoFreeform();
        if (!inVivoFreeform) {
            return -1;
        }
        String startActivityName = null;
        String topActivityName = (top == null || top.mActivityComponent == null) ? null : top.mActivityComponent.flattenToShortString();
        if (r != null && r.mActivityComponent != null) {
            startActivityName = startActivity.mActivityComponent.flattenToShortString();
        }
        if ("com.tencent.mobileqq/.activity.QQLSActivity".equals(startActivityName) && ("com.android.settings/com.vivo.settings.secret.PasswordActivity".equals(topActivityName) || "com.android.settings/com.vivo.settings.secret.PasswordActivityUD".equals(topActivityName))) {
            return 2;
        }
        if ("com.tencent.mobileqq/.activity.RegisterGuideActivity".equals(startActivityName)) {
            this.mActivityStarter.mLaunchFlags &= -67108865;
        }
        if (this.mVivoService.isInDirectFreeformState() && (startActivity.isPasswordActivity() || startActivity.isPasswordAuthenActivity())) {
            this.mVivoService.setIsStartingPassword(true);
        }
        if ((startActivity.isEmergentActivity() || startActivity.isPasswordAuthenActivity()) && !this.mVivoService.isStartingEmergent()) {
            this.mVivoService.setIsStartingEmergent(true);
        }
        return -1;
    }

    public boolean isSameActivityShowInFreeform(ActivityRecord startActivity) {
        String freeformTopName;
        if (!this.mVivoService.isVivoFreeFormValid() || !this.mVivoService.isInVivoFreeform()) {
            return false;
        }
        ActivityStack freeFormStack = this.mVivoSupervisor.mRootWindowContainer.getVivoFreeformStack();
        String startActivityName = null;
        ActivityRecord freeformTop = freeFormStack != null ? freeFormStack.getTopNonFinishingActivity() : null;
        if (freeformTop == null || freeformTop.mActivityComponent == null) {
            freeformTopName = null;
        } else {
            freeformTopName = freeformTop.mActivityComponent.flattenToShortString();
        }
        if (startActivity.mActivityComponent != null) {
            startActivityName = startActivity.mActivityComponent.flattenToShortString();
        }
        if (startActivityName == null || !startActivityName.equals(freeformTopName) || startActivity.mUserId != freeformTop.mUserId) {
            return false;
        }
        return true;
    }

    public boolean bringFoundTaskToFrontInFreeform(ActivityStack launchStack, ActivityRecord startActivity, Task intentTask, boolean noAnimation, ActivityOptions options) {
        if (this.mVivoService.isVivoFreeFormValid() && launchStack.inFreeformWindowingMode()) {
            if (launchStack.hasChild(intentTask)) {
                launchStack.moveTaskToFront(intentTask, noAnimation, options, startActivity.appTimeTracker, true, "bringingFoundTaskToFront");
            } else {
                intentTask.reparent(launchStack, true, 0, true, true, "reparentToTargetStack");
            }
            this.mActivityStarter.mMovedToFront = true;
            return true;
        }
        return false;
    }

    public boolean removeUnuseStack(ActivityStack launchStack, ActivityRecord intentActivity) {
        if (this.mVivoService.isVivoFreeFormValid() && intentActivity.getStack() != null && intentActivity.getStack().inFreeformWindowingMode() && launchStack != intentActivity.getStack()) {
            launchStack.removeIfPossible();
            return true;
        }
        return false;
    }

    public void exitFreeformWhenLockTask() {
        ActivityStack freeformStack;
        if (this.mVivoService.isVivoFreeFormValid() && this.mVivoService.isInVivoFreeform() && this.mVivoService.getLockTaskController().getLockTaskModeState() == 2 && (freeformStack = this.mVivoService.mRootWindowContainer.getVivoFreeformStack()) != null && freeformStack.getDisplayArea() != null) {
            freeformStack.getDisplayArea().moveVivoFreeformTasksToFullscreenStackLocked(false);
        }
    }

    public ActivityStack getLaunchStackForVivoFreeform(ActivityRecord r, ActivityOptions aOptions) {
        ActivityStack freeFormStack;
        boolean inVivoFreeform = this.mVivoService.isVivoFreeFormValid() && this.mVivoService.isInVivoFreeform();
        if (!inVivoFreeform || r.isActivityTypeRecents() || r.isActivityTypeHome() || (freeFormStack = this.mVivoSupervisor.mRootWindowContainer.getVivoFreeformStack()) == null) {
            return null;
        }
        if (r.getTask() != null) {
            if (r.getTask() == freeFormStack.getTopMostTask()) {
                return freeFormStack;
            }
        } else {
            String className = r.mActivityComponent != null ? r.mActivityComponent.flattenToShortString() : null;
            if ("com.tencent.mobileqq/com.tencent.av.ui.VChatActivity".equals(className) || "com.tencent.mobileqq/cooperation.qzone.QzoneFeedsPluginProxyActivity".equals(className)) {
                String pkgName = r.mActivityComponent.getPackageName();
                String freeFormPkgName = null;
                Task freeformTopTask = freeFormStack.getTopMostTask();
                if (freeformTopTask != null && freeformTopTask.realActivity != null) {
                    freeFormPkgName = freeformTopTask.realActivity.getPackageName();
                }
                if (pkgName != null && pkgName.equals(freeFormPkgName) && r.mUserId == freeformTopTask.mUserId) {
                    return freeFormStack;
                }
            } else if (this.mVivoService.isInDirectFreeformState() && className != null && className.contains("com.vivo.contacts") && this.mVivoSupervisor.isWillCallContactsFromFreefromMms() && this.mVivoSupervisor.isWillCallContactsFromPasswd()) {
                this.mVivoSupervisor.setWillCallContactsFromPasswd(false);
                return freeFormStack;
            }
        }
        if (!r.isPasswordActivity()) {
            return null;
        }
        return this.mVivoSupervisor.mRootWindowContainer.getLaunchStack(r, aOptions, r.getTask(), true);
    }

    public void changeTaskWindowModeIfNeed(ActivityRecord source, Task intentTask, LaunchParamsController.LaunchParams launchParams) {
        if (this.mVivoService.isVivoFreeFormValid() && source != null && source.inFreeformWindowingMode() && intentTask != null && intentTask.getTopNonFinishingActivity() != null && intentTask.getDisplayArea() != null && launchParams.hasWindowingMode() && launchParams.mWindowingMode != intentTask.getStack().getWindowingMode() && launchParams.mWindowingMode == 5) {
            int activityType = this.mActivityStarter.mStartActivity != null ? this.mActivityStarter.mStartActivity.getActivityType() : intentTask.getActivityType();
            this.mVivoService.exitCurrentFreeformTaskForTransitToNext(source, intentTask, launchParams);
            if (launchParams.mWindowingMode == 5) {
                intentTask.getStack().setWindowingMode(intentTask.getDisplayArea().validateWindowingMode(launchParams.mWindowingMode, this.mActivityStarter.mStartActivity, intentTask, activityType));
                if (intentTask.getStack().inFreeformWindowingMode()) {
                    intentTask.setBounds(launchParams.mBounds);
                }
            }
        }
    }

    public int isStartTaskInPrimaryStackWithHome(ActivityRecord intentActivity) {
        ActivityStack miniLauncherTask;
        int bStartTaskInPrimaryStackWithHome = 1;
        if (!this.mVivoService.isMultiWindowSupport() || !this.mVivoService.isInMultiWindowDefaultDisplay() || !this.mVivoService.isSplittingScreenByVivo()) {
            return 1;
        }
        ActivityStack topSecondaryStack = this.mVivoService.mRootWindowContainer.getDefaultTaskDisplayArea().getTopStackInWindowingMode(4);
        ActivityStack topFullscreenStack = this.mVivoService.mRootWindowContainer.getDefaultTaskDisplayArea().getTopStackInWindowingMode(1);
        if ((topSecondaryStack == null || !topSecondaryStack.isActivityTypeHome()) && (topFullscreenStack == null || !topFullscreenStack.isActivityTypeHome())) {
            return 1;
        }
        ActivityStack topPrimaryStack = this.mVivoService.mRootWindowContainer.getDefaultTaskDisplayArea().getTopStackInWindowingMode(3);
        Task topPrimaryTask = topPrimaryStack != null ? topPrimaryStack.getTopMostTask() : null;
        Task task = intentActivity != null ? intentActivity.getTask() : null;
        ActivityStack rootTask = intentActivity != null ? intentActivity.getRootTask() : null;
        if (topPrimaryStack == rootTask && topPrimaryTask != task && task != null) {
            Task topSecondaryTask = topSecondaryStack != null ? topSecondaryStack.getTopMostTask() : null;
            Task topFullscreenTask = topFullscreenStack != null ? topFullscreenStack.getTopMostTask() : null;
            if ((topSecondaryTask == null || !topSecondaryTask.isActivityTypeHome()) && (topFullscreenTask == null || !topFullscreenTask.isActivityTypeHome())) {
                return 1;
            }
            VSlog.d(TAG, "isStartTaskInPrimaryStackWithHome topPrimaryTask:" + topPrimaryTask + " task:" + task + " bStartTaskInPrimaryStackWithHome:2");
            return 2;
        } else if (topPrimaryStack != rootTask || topPrimaryTask != task || task == null) {
            return 1;
        } else {
            if (this.mVivoService.isVivoVosMultiWindowSupport() && !isSpecialActivity(intentActivity)) {
                this.mVivoService.mWindowManager.showRecentApps();
                bStartTaskInPrimaryStackWithHome = 3;
                VSlog.d(TAG, "isStartTaskInPrimaryStackWithHome topPrimaryTask:" + topPrimaryTask + " task:" + task + " bStartTaskInPrimaryStackWithHome:3");
            }
            if (this.mVivoService.isVivoMultiWindowSupport() && (miniLauncherTask = this.mVivoService.mRootWindowContainer.getDefaultTaskDisplayArea().getMiniLauncherTask()) != null && (miniLauncherTask instanceof ActivityStack)) {
                this.mVivoService.mStackSupervisor.specialFreezingMultiWindow("specifyTime", 100);
                miniLauncherTask.moveToFront("StartTaskInPrimaryStackWithHome");
                VSlog.d(TAG, "isStartTaskInPrimaryStackWithHome miniLauncherTask:" + miniLauncherTask + " task:" + task + " bStartTaskInPrimaryStackWithHome:3");
                return 3;
            }
            return bStartTaskInPrimaryStackWithHome;
        }
    }

    private boolean isSpecialActivity(ActivityRecord activity) {
        if (activity == null || activity.mActivityComponent == null || activity.mActivityComponent.getClassName() == null) {
            return false;
        }
        return activity.mActivityComponent.getClassName().equals("com.google.android.finsky.unauthenticated.UnauthenticatedMainActivity");
    }

    public void setVivoFloatPackageColdStartingIfNeed(String packageName) {
        if (!this.mVivoService.isMultiWindowSupport()) {
            return;
        }
        if (this.mVivoService.getVivoFloatMessageFlag() && packageName != null && packageName.equals(this.mVivoService.getVivoPendingPackageInSplit())) {
            this.mVivoService.setVivoFloatPackageColdStarting(true);
        }
        this.mVivoService.setVivoCurrentPackageInColdStarting(packageName);
    }

    public boolean checkVivoController(Intent intent, int userId, String packageName, ActivityOptions checkedOptions, ActivityRecord resultRecord, IBinder resultTo, int requestCode, int callingUid, boolean abort) {
        Intent watchIntent;
        if (this.mVivoService.mVivoController == null) {
            return abort;
        }
        try {
            if (this.mVivoService.isSecureControllerForMultiWindow) {
                watchIntent = (Intent) intent.clone();
            } else {
                watchIntent = intent.cloneFilter();
            }
            if (isDoubleInstanceEnable()) {
                watchIntent.setTargetUserId(userId);
            }
            setVivoActivityControllerTimeout();
            abort |= !this.mVivoService.mVivoController.activityStarting(watchIntent, packageName, checkedOptions == null ? null : checkedOptions.toBundle());
            cancelVivoActivityControllerTimeout();
            if (abort) {
                setInterceptedResultValue(resultRecord, resultTo, requestCode);
                setInterceptedCallingUid(callingUid);
            }
        } catch (RemoteException e) {
            this.mVivoService.mVivoController = null;
        }
        return abort;
    }

    public boolean needLaunchInHomeStackForSplitMode(ActivityRecord r, ActivityRecord source) {
        if (r != null && r.intent != null && this.mVivoService.isInMultiWindowFocusedDisplay() && r.getTask() == null && r.intent.getCategories() != null && r.intent.getCategories().contains("android.intent.category.HOME")) {
            StringBuilder sb = new StringBuilder();
            sb.append("needLaunchInHomeStackForSplitMode target pkg: ");
            sb.append(r.packageName);
            sb.append(", source pkg: ");
            sb.append(source == null ? null : source.packageName);
            VSlog.d(TAG, sb.toString());
            return true;
        }
        return false;
    }

    public int activityStartFisrtLauncheBoost(String packageName) {
        AbsVivoPerfManager absVivoPerfManager = this.mPerf;
        if (absVivoPerfManager != null) {
            return absVivoPerfManager.perfHint(4225, packageName, -1, 1);
        }
        return -1;
    }

    public ActivityRecord getInterceptedSourceRecord() {
        return this.mVivoSoftwareLock.getInterceptedSourceRecord();
    }

    public IBinder getInterceptedResultTo() {
        return this.mVivoSoftwareLock.getInterceptedResultTo();
    }

    public int getInterceptedRequestCode() {
        return this.mVivoSoftwareLock.getInterceptedRequestCode();
    }

    public int getInterceptedCallingUid() {
        return this.mVivoSoftwareLock.getInterceptedCallingUid();
    }

    public NeededUriGrants getInterceptedNeededUriGrants() {
        return this.mVivoSoftwareLock.getInterceptedNeededUriGrants();
    }

    public void setInterceptedCallingUid(int uid) {
        this.mVivoSoftwareLock.setInterceptedCallingUid(uid);
    }

    public void setInterceptedNeededUriGrants(NeededUriGrants neededUriGrants) {
        this.mVivoSoftwareLock.setInterceptedNeededUriGrants(neededUriGrants);
    }

    public String getInterceptedCallingPackage() {
        return this.mVivoSoftwareLock.getInterceptedCallingPackage();
    }

    public void setInterceptedCallingPackage(String callingPackage) {
        this.mVivoSoftwareLock.setInterceptedCallingPackage(callingPackage);
    }

    public void setInterceptedResultValue(ActivityRecord resultRecord, IBinder resultTo, int requestcode) {
        if (VivoSoftwareLock.isSecureController && resultRecord != null && resultTo != null) {
            this.mVivoSoftwareLock.setInterceptedResultValue(resultRecord, resultTo, requestcode);
        }
    }

    public boolean checkStartFormSoftwareLock(Intent intent, ActivityInfo aInfo) {
        if ((intent.getFlags() & Integer.MIN_VALUE) != 0) {
            return true;
        }
        if (aInfo != null) {
            if ((!VivoSoftwareLock.isInDaemon && !aInfo.applicationInfo.packageName.startsWith(VivoAppIsolationController.NOTIFY_IQOO_SECURE_PACKAGE)) || (VivoSoftwareLock.isInDaemon && !aInfo.applicationInfo.packageName.startsWith("com.vivo.daemonService"))) {
                this.mVivoSoftwareLock.setInterceptedResultValue(null, null, -1);
                return false;
            }
            return false;
        }
        return false;
    }

    public Intent getWatchIntent(Intent intent) {
        if (VivoSoftwareLock.isSecureController) {
            Intent watchIntent = (Intent) intent.clone();
            return watchIntent;
        }
        Intent watchIntent2 = intent.cloneFilter();
        return watchIntent2;
    }

    public boolean blockByUserSetup(ActivityRecord r) {
        return this.mVivoService.checkActivityInfo(r, 0);
    }

    public void reportBootingActivityStartInfo(int callingUid, String callingPackage, ActivityRecord r) {
        if (!this.mVivoService.mIsOverseas && r != null && !r.isActivityTypeHome()) {
            String callerPackage = callingPackage;
            String callerStackTrace = null;
            if ((callingUid == Process.myUid() || callingUid == 0) && callerPackage == null) {
                callerPackage = "system_server";
                callerStackTrace = Debug.getCallers(5);
            }
            AmsDataManager.getInstance().reportBootingActivityStartInfo(callerPackage, r.shortComponentName, "activity start before booted", callerStackTrace);
        }
    }

    public boolean startHomeForVirtualDisplay(Intent intent, ActivityRecord sourceRecord) {
        if (MultiDisplayManager.SUPPORT_CAR_NETWORKING) {
            boolean isHomeIntent = intent != null && "android.intent.action.MAIN".equals(intent.getAction()) && (intent.hasCategory("android.intent.category.HOME") || intent.hasCategory("android.intent.category.SECONDARY_HOME"));
            VSlog.d("VivoCar", "startHomeForVirtualDisplay intent=" + intent + " ,isHomeIntent=" + isHomeIntent);
            if (!isHomeIntent || sourceRecord == null || !MultiDisplayManager.isVivoDisplay(sourceRecord.getDisplayId())) {
                return isHomeIntent;
            }
            this.mActivityStarter.mService.mH.post(new Runnable() { // from class: com.android.server.wm.VivoActivityStarterImpl.1
                @Override // java.lang.Runnable
                public void run() {
                    Intent homeCarNetworking = new Intent("vivo.intent.carnetworking.category.HOME");
                    homeCarNetworking.setPackage("com.vivo.car.networking");
                    VSlog.d("VivoCar", "send carnetworking broadcast: " + homeCarNetworking);
                    VivoActivityStarterImpl.this.mContext.sendBroadcastAsUser(homeCarNetworking, UserHandle.ALL);
                }
            });
            return true;
        }
        return false;
    }

    public boolean startActivityForVirtualDisplay(ActivityRecord sourceRecord, ActivityRecord reusedActivity, Intent intent) {
        if (MultiDisplayManager.SUPPORT_CAR_NETWORKING) {
            VSlog.d("VivoCar", "==>startActivityForVirtualDisplay intent=" + intent);
            if ((sourceRecord != null && MultiDisplayManager.isVivoDisplay(sourceRecord.getDisplayId())) || (reusedActivity != null && MultiDisplayManager.isVivoDisplay(reusedActivity.getDisplayId()))) {
                String component = intent.getComponent() != null ? intent.getComponent().flattenToShortString() : "None";
                ArrayList<String> whitelist = VCarConfigManager.getInstance().get(VivoFirewall.TYPE_ACTIVITY);
                if (whitelist != null && whitelist.size() > 0 && whitelist.contains(component)) {
                    if (MultiDisplayManager.DEBUG) {
                        VSlog.d("VivoStack", "Force moveToDefaultDisplay ,r: " + component);
                    }
                    this.mActivityStarter.mPreferredTaskDisplayArea = this.mVivoService.mRootWindowContainer.getDefaultTaskDisplayArea();
                    this.mActivityStarter.mLaunchParams.mPreferredTaskDisplayArea = this.mVivoService.mRootWindowContainer.getDefaultTaskDisplayArea();
                }
            }
            int preferredDisplayId = this.mActivityStarter.mPreferredTaskDisplayArea != null ? this.mActivityStarter.mPreferredTaskDisplayArea.getDisplayId() : -1;
            VSlog.d("VivoCar", "preferredDisplayId=" + preferredDisplayId + " ,sourceRecord=" + sourceRecord + " ,lastStartActivityRecord=" + reusedActivity);
            if (reusedActivity == null || preferredDisplayId == -1 || reusedActivity.getDisplayId() == -1 || preferredDisplayId == reusedActivity.getDisplayId() || !((preferredDisplayId == 0 || MultiDisplayManager.isVivoDisplay(preferredDisplayId)) && (reusedActivity.getDisplayId() == 0 || MultiDisplayManager.isVivoDisplay(reusedActivity.getDisplayId())))) {
                return false;
            }
            if (preferredDisplayId == 0 && reusedActivity.getDisplayId() == 90000) {
                return this.mVivoService.moveToDisplayForVirtualDisplay(preferredDisplayId, reusedActivity.getDisplayId(), true);
            }
            ActivityStack startedActivityStack = reusedActivity.getStack();
            if (startedActivityStack == null) {
                if (MultiDisplayManager.DEBUG) {
                    VSlog.w("VivoStack", "skip because stack is null!");
                }
                return false;
            }
            TaskDisplayArea taskDisplayArea = null;
            DisplayContent displayContent = this.mVivoService.mRootWindowContainer.getDisplayContent(preferredDisplayId);
            if (displayContent != null) {
                taskDisplayArea = displayContent.getDefaultTaskDisplayArea();
            }
            StringBuilder sb = new StringBuilder();
            sb.append("reparent to display ");
            sb.append(taskDisplayArea != null ? taskDisplayArea.getDisplayId() : -1);
            sb.append(" ,stack: ");
            sb.append(startedActivityStack);
            VSlog.d("VivoCar", sb.toString());
            if (taskDisplayArea != null) {
                boolean fromResume = reusedActivity.isState(ActivityStack.ActivityState.RESUMED);
                boolean updateRecentTask = reusedActivity.getDisplayId() == 95555;
                ActivityStack samePackageStack = getSamePackageStackIfNeed(startedActivityStack);
                startedActivityStack.reparent(taskDisplayArea, true);
                this.mActivityStarter.mMovedToFront = true;
                if (updateRecentTask && reusedActivity.getTask() != null) {
                    this.mVivoSupervisor.mRecentTasks.add(reusedActivity.getTask());
                    VSlog.d("VivoCar", "updateRecent task: " + reusedActivity.getTask());
                }
                if (samePackageStack != null) {
                    samePackageStack.reparent(taskDisplayArea, false);
                    VSlog.d("VivoCar", "reparent " + samePackageStack + " for same package");
                }
                if (!fromResume && preferredDisplayId == 0) {
                    reusedActivity.getDisplayContent().prepareAppTransition(10, false, 0, false);
                }
            }
            return false;
        }
        return false;
    }

    private ActivityStack getSamePackageStackIfNeed(ActivityStack startedActivityStack) {
        String startedPkg;
        TaskDisplayArea taskDisplayArea = startedActivityStack.getDisplayArea();
        if (taskDisplayArea == null || taskDisplayArea.getStackCount() <= 2) {
            return null;
        }
        boolean skip = false;
        for (int sNdx = taskDisplayArea.getStackCount() - 1; sNdx >= 0; sNdx--) {
            if (sNdx < taskDisplayArea.getStackCount()) {
                ActivityStack stack = taskDisplayArea.getStackAt(sNdx);
                if (stack != startedActivityStack && stack.realActivity != null && startedActivityStack.realActivity != null && (startedPkg = startedActivityStack.realActivity.getPackageName()) != null && startedPkg.equals(stack.realActivity.getPackageName())) {
                    return stack;
                }
                if (skip) {
                    break;
                } else if (stack == startedActivityStack) {
                    skip = true;
                }
            }
        }
        return null;
    }

    public boolean canSkipSoftwareLock(ActivityOptions checkedOptions, String callingPackage, int callingUid) {
        if (!MultiDisplayManager.SUPPORT_CAR_NETWORKING) {
            return false;
        }
        boolean preloadSkip = false;
        int preferredDisplayId = checkedOptions != null ? checkedOptions.getLaunchDisplayId() : -1;
        if (preferredDisplayId == 95555 && ("com.vivo.abe".equals(callingPackage) || "com.vivo.sps".equals(callingPackage))) {
            preloadSkip = true;
        } else if (this.mActivityStarter.mRmsInjector != null && this.mActivityStarter.mRmsInjector.isRmsPreload(callingPackage, callingUid)) {
            preloadSkip = true;
        }
        if (preloadSkip && MultiDisplayManager.DEBUG) {
            VSlog.i("VivoStack", "ActivityStarting skip to notify because of prelaod :" + callingPackage);
        }
        return preloadSkip;
    }

    public boolean shouldSkipForVirtualDisplay(ActivityOptions vivoOptions, Intent intent, String callingPackage) {
        ActivityStack top;
        ActivityStack top2;
        if (MultiDisplayManager.SUPPORT_CAR_NETWORKING && MultiDisplayManager.isVCarDisplayRunning()) {
            int preferredDisplayId = vivoOptions != null ? vivoOptions.getLaunchDisplayId() : -1;
            if (MultiDisplayManager.isResCarDisplay(preferredDisplayId)) {
                VSlog.d("VivoCar", "skip check task: " + intent);
                return true;
            }
            DisplayContent displayContent = this.mVivoSupervisor.mRootWindowContainer.getDisplayContent((int) SceneManager.APP_REQUEST_PRIORITY);
            if (displayContent != null && (top2 = displayContent.getTopStack()) != null && top2.isPresentWithPackage(callingPackage)) {
                VSlog.d("VivoCar", "skip check task2: " + intent);
                return true;
            }
            DisplayContent miniDisplayContent = this.mVivoSupervisor.mRootWindowContainer.getDisplayContent(80003);
            if (miniDisplayContent == null || (top = miniDisplayContent.getTopStack()) == null || !top.isPresentWithPackage(callingPackage)) {
                return false;
            }
            VSlog.d("VivoCar", "skip vivoFirewall check " + intent);
            return true;
        }
        return false;
    }

    public boolean shouldBlockedByAppShareLocked(ActivityRecord sourceRecord, String callingPackage, int realCallingPid, int realCallingUid, ActivityInfo info, Intent intent) {
        return this.mVivoAppShareManager.shouldBlockedByAppShareLocked(sourceRecord, callingPackage, realCallingPid, realCallingUid, info, intent);
    }

    public int startAppShareOrBlockStart(ActivityRecord sourceRecord, ActivityRecord r, ActivityRecord reusedActivity, String lastStartReason) {
        return this.mVivoAppShareManager.startAppShareOrBlockStartLocked(sourceRecord, r, reusedActivity, lastStartReason);
    }

    public boolean shouldIgnoreChildrenModeByAppShareLocked(String callingPackage, int realCallingUid, Intent intent) {
        return this.mVivoAppShareManager.shouldIgnoreChildrenModeByAppShareLocked(callingPackage, realCallingUid, intent);
    }

    public void resetAppShareCallingPackage() {
        this.mVivoAppShareManager.resetAppShareCallingPackage();
    }

    public ActivityOptions setLaunchDisplayIdForAppShareIfNeededLocked(ActivityInfo aInfo, Intent intent, ActivityRecord r, ActivityRecord sourceRecord, String callingPackage, int realCallingUid, ActivityOptions checkedOptions) {
        return this.mVivoAppShareManager.setLaunchDisplayIdForAppShareIfNeededLocked(aInfo, intent, r, sourceRecord, callingPackage, realCallingUid, checkedOptions);
    }

    public boolean createNewTaskForAppShareLocked(ActivityRecord r) {
        return this.mVivoAppShareManager.createNewTaskForAppShareLocked(r);
    }

    public boolean shouldAbortBgstart(ActivityRecord sourceRecord) {
        if (AmsConfigManager.getInstance().isActivityBgstartAllowed() || sourceRecord == null || sourceRecord.isState(ActivityStack.ActivityState.RESUMED) || (sourceRecord.info.applicationInfo.flags & KernelConfig.AP_TE) == 0) {
            return false;
        }
        Iterator<String> it = BGSTART_WHITELIST.iterator();
        while (it.hasNext()) {
            String prefix = it.next();
            if (sourceRecord.packageName.startsWith(prefix)) {
                return false;
            }
        }
        return !AmsConfigManager.getInstance().getBgStartAllowedActivityList().contains(sourceRecord.packageName);
    }

    public int getDupStartStateInSplit(TaskDisplayArea taskDisplayArea, ActivityRecord intentActivity) {
        int dupStartState = 0;
        try {
            if (!taskDisplayArea.isSplitScreenModeActivated() || intentActivity == null || intentActivity.mActivityComponent == null) {
                return 0;
            }
            ActivityStack primarySplitTask = taskDisplayArea.getRootSplitScreenPrimaryTask();
            ActivityRecord primayTopAr = primarySplitTask.topRunningActivity();
            int numActivities = 1;
            if (primayTopAr != null && primayTopAr.getTask() != null) {
                numActivities = primayTopAr.getTask().mReuseActivitiesReport.numActivities;
            }
            if (numActivities == 1 && intentActivity == primayTopAr) {
                dupStartState = 1;
                VSlog.e(TAG, "DUP_START_STATE_PRIMARY  intentActivity=" + intentActivity);
                return 1;
            }
            return 0;
        } catch (Exception e) {
            VSlog.e(TAG, "check double start in split failed-" + e);
            return dupStartState;
        }
    }
}