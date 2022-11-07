package com.android.server.am;

import android.app.IApplicationThread;
import android.app.admin.DevicePolicyManager;
import android.app.admin.VivoPolicyManagerInternal;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.multidisplay.MultiDisplayManager;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.VivoDoubleInstanceServiceImpl;
import com.android.server.policy.IVivoRatioControllerUtils;
import com.android.server.wm.ActivityTaskManagerDebugConfig;
import com.android.server.wm.VivoAppShareManager;
import com.vivo.appshare.AppShareConfig;
import com.vivo.face.common.data.Constants;
import com.vivo.face.common.state.FaceUIState;
import com.vivo.services.proxy.game.GameSceneProxyManager;
import com.vivo.services.rms.sp.SpManagerImpl;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import vendor.pixelworks.hardware.display.V1_0.KernelConfig;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoActiveServiceImpl implements IVivoActiveService {
    static final String TAG = "VivoActiveServiceImpl";
    private DevicePolicyManager dpm;
    private ActiveServices mActiveService;
    private AmsDataManager mAmsDataManager;
    private VivoDoubleInstanceServiceImpl mVivoDoubleInstanceService;
    private IVivoRatioControllerUtils mVivoRatioControllerUtils;
    boolean isCustomType = false;
    private List<String> mWhiteServiceList = new ArrayList();
    final ArrayList<String> BGSTART_ALLOWED_PACKAGE = new ArrayList<String>() { // from class: com.android.server.am.VivoActiveServiceImpl.1
        {
            add("com.vivo.abe");
            add("com.vivo.pushservice");
            add("com.LogiaGroup.LogiaDeck");
            add("com.vivo.sps");
        }
    };
    final ArrayList<String> BGSTART_ALLOWED_ACTION = new ArrayList<String>() { // from class: com.android.server.am.VivoActiveServiceImpl.2
        {
            add("com.vivo.pushservice.action.RECEIVE");
        }
    };

    public VivoActiveServiceImpl(ActiveServices activeService) {
        this.mVivoDoubleInstanceService = null;
        if (activeService == null) {
            Slog.i(TAG, "container is " + activeService);
        }
        this.mActiveService = activeService;
        this.mVivoDoubleInstanceService = VivoDoubleInstanceServiceImpl.getInstance();
        this.mAmsDataManager = AmsDataManager.getInstance();
        GameSceneProxyManager.initialize(this);
    }

    public void dummy() {
        Slog.i(TAG, "dummy, this=" + this);
    }

    public int changeUseridForDoubleInstance(int userId, int callingUid, Intent service) {
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl != null && service != null && vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable() && UserHandle.getUserId(callingUid) == 999) {
            if ("com.google.android.youtube.api.service.START".equals(service.getAction()) || "android.media.IMediaScannerService".equals(service.getAction()) || "vivo.content.catcher.touch.event".equals(service.getAction()) || "com.vivo.contentcatcher.action.TRIGGER".equals(service.getAction())) {
                return 0;
            }
            return userId;
        }
        return userId;
    }

    public boolean isBgStartAllowLocked(String callingPackage, IApplicationThread caller, ServiceRecord r) {
        List<String> list;
        ProcessRecord callerApp;
        boolean bgstart_allowed = false;
        if (callingPackage != null && this.BGSTART_ALLOWED_PACKAGE.contains(callingPackage) && (callerApp = this.mActiveService.mAm.getRecordForAppLocked(caller)) != null) {
            boolean isSystemApp = (callerApp.info.flags & KernelConfig.AP_TE) != 0;
            if (isSystemApp && r != null) {
                bgstart_allowed = true;
                r.bgStartAllowed = true;
            }
        }
        if (this.mActiveService.mAm.isBgStartAllowed(r.packageName) && r != null) {
            bgstart_allowed = true;
            r.bgStartAllowed = true;
        }
        if (this.isCustomType && (list = this.mWhiteServiceList) != null && list.contains(r.packageName) && r != null) {
            r.bgStartAllowed = true;
            return true;
        }
        return bgstart_allowed;
    }

    public void addServiceStartToHistory(ServiceRecord r, Intent service, ProcessRecord callerApp) {
        this.mAmsDataManager.addServiceStartToHistory(r, service, callerApp);
    }

    public void addServiceBindToHistory(ServiceRecord r, Intent service, ProcessRecord callerApp) {
        this.mAmsDataManager.addServiceBindToHistory(r, service, callerApp);
    }

    public void proxyServiceInGameSceneLocked(long now, ServiceRecord r) {
        if (r.app != null || r.restartDelay == 0 || r.restartDelay == 3600000 || GameSceneProxyManager.isServiceAllowRestartLocked(r.appInfo.packageName, r.processName, r.shortInstanceName)) {
            return;
        }
        r.restartDelay = 3600000L;
        r.nextRestartTime = r.restartDelay + now;
    }

    public void rescheduleServiceProxyedByGameScene() {
        ActiveServices activeServices = this.mActiveService;
        if (activeServices == null) {
            return;
        }
        synchronized (activeServices.mAm) {
            for (int i = this.mActiveService.mRestartingServices.size() - 1; i >= 0; i--) {
                ServiceRecord r = (ServiceRecord) this.mActiveService.mRestartingServices.get(i);
                if (r.restartDelay == 3600000) {
                    r.resetRestartCounter();
                    this.mActiveService.scheduleServiceRestartLocked(r, true);
                }
            }
        }
    }

    public void dumpServiceProxyedByGameScene(PrintWriter pw) {
        ActiveServices activeServices = this.mActiveService;
        if (activeServices == null) {
            return;
        }
        synchronized (activeServices.mAm) {
            pw.println("\tServiceDelayedByGameScene:");
            for (int i = this.mActiveService.mRestartingServices.size() - 1; i >= 0; i--) {
                ServiceRecord r = (ServiceRecord) this.mActiveService.mRestartingServices.get(i);
                if (r.restartDelay == 3600000) {
                    pw.print("\t\t");
                    pw.println(r);
                }
            }
        }
    }

    public boolean isImportantSystemUiService(ProcessRecord proc, ServiceRecord sr) {
        if (FaceUIState.PKG_SYSTEMUI.equals(proc.processName)) {
            Resources resources = this.mActiveService.mAm.mContext.getResources();
            ComponentName keyguardComponent = ComponentName.unflattenFromString(resources.getString(17039930));
            ComponentName wallpaperComponent = ComponentName.unflattenFromString(resources.getString(17040391));
            if (keyguardComponent.equals(sr.instanceName) || wallpaperComponent.equals(sr.instanceName)) {
                VSlog.d(TAG, "important systemui service that do not bring down = " + sr);
                return true;
            }
            return false;
        }
        return false;
    }

    public void restartServices(ProcessRecord app, final ServiceRecord sr) {
        if (SpManagerImpl.getInstance().isSuperSystemProcess(app.processName, app.uid) && !SpManagerImpl.getInstance().canStartOnSuperProcess(sr.packageName, sr.appInfo.uid)) {
            app.stopService(sr);
            app.updateBoundClientUids();
            if ("com.sp.sdk.PERSISTENT_SERVICE".equals(sr.intent.getIntent().getAction())) {
                for (int index = this.mActiveService.mAm.mApplicationInSps.size() - 1; index >= 0; index--) {
                    final ApplicationInfo appInfo = (ApplicationInfo) this.mActiveService.mAm.mApplicationInSps.get(index);
                    if (appInfo.packageName.equals(sr.packageName)) {
                        this.mActiveService.mAm.mApplicationInSps.remove(appInfo);
                        this.mActiveService.mAm.mHandler.post(new Runnable() { // from class: com.android.server.am.VivoActiveServiceImpl.3
                            @Override // java.lang.Runnable
                            public void run() {
                                VivoActiveServiceImpl.this.mActiveService.mAm.addAppLocked(appInfo, (String) null, false, (String) null, 2);
                            }
                        });
                        return;
                    }
                }
            }
            this.mActiveService.mAm.mHandler.post(new Runnable() { // from class: com.android.server.am.VivoActiveServiceImpl.4
                @Override // java.lang.Runnable
                public void run() {
                    Intent intent = (Intent) sr.intent.getIntent().clone();
                    try {
                        VivoActiveServiceImpl.this.mActiveService.mAm.mContext.startService(intent);
                    } catch (Exception ex) {
                        VSlog.w(VivoActiveServiceImpl.TAG, "restartServices sps exp intent:" + intent, ex);
                    }
                }
            });
        }
    }

    public boolean shouldBlockBindServiceForAppShareLocked(ProcessRecord callerApp, Intent service, String callingPackage) {
        if (AppShareConfig.SUPPROT_APPSHARE && this.mActiveService.mAm.isAppSharing()) {
            int callingUserId = UserHandle.getUserId(callerApp.info.uid);
            boolean isControlledByRemote = this.mActiveService.mAm.isControlledByRemote();
            String targetPackage = service != null ? service.getPackage() : Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            if (ActivityTaskManagerDebugConfig.DEBUG_APP_SHARE) {
                VSlog.i(TAG, "bindServiceLocked, callingPackage = " + callingPackage + ", uid = " + callerApp.info.uid + ", callingUserId = " + callingUserId + ", targetPackage = " + targetPackage + ", service = " + service);
            }
            if (this.mActiveService.mAm.isOnAppShareDisplay(callingPackage, callingUserId) && isControlledByRemote && !callingPackage.equals(targetPackage) && AppShareConfig.getInstance().isServiceIntentBlackList(service)) {
                VSlog.i(TAG, "block bind Service!");
                return true;
            }
            return false;
        }
        return false;
    }

    public void handleProcessStarted(ActivityManagerService ams, ServiceRecord r, ProcessRecord app) {
        if (AppShareConfig.SUPPROT_APPSHARE && this.mVivoRatioControllerUtils != null) {
            int displayId = ams.mActivityTaskManager.getFocusedDisplayId();
            if (this.mActiveService.mAm.isAppSharing() && this.mVivoRatioControllerUtils.isInputMethodPackageName(r.name) && (ams.mActivityTaskManager.isAppShareForeground() || MultiDisplayManager.isAppShareDisplayId(VivoAppShareManager.getInstance().getInputMethodDstDisplayId()))) {
                displayId = 10086;
            }
            this.mVivoRatioControllerUtils.handleProcessStarted(app.pid, displayId, displayId, r.name);
        }
    }

    public void systemServicesReady() {
        final DevicePolicyManager dpm = (DevicePolicyManager) this.mActiveService.mAm.mContext.getSystemService("device_policy");
        if (dpm != null && dpm.getCustomType() > 0) {
            this.isCustomType = true;
            this.mWhiteServiceList = dpm.getRestrictionInfoList(null, 1509);
            VivoPolicyManagerInternal mVivoPolicyManagerInternal = (VivoPolicyManagerInternal) LocalServices.getService(VivoPolicyManagerInternal.class);
            mVivoPolicyManagerInternal.setVivoPolicyListener(new VivoPolicyManagerInternal.VivoPolicyListener() { // from class: com.android.server.am.VivoActiveServiceImpl.5
                public void onVivoPolicyChanged(int poId) {
                    if (poId == 0 || poId == 1509) {
                        VivoActiveServiceImpl.this.mWhiteServiceList = dpm.getRestrictionInfoList(null, 1509);
                    }
                }
            });
        }
    }
}