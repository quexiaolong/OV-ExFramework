package com.android.server.wm;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemProperties;
import android.provider.Settings;
import android.view.Display;
import com.android.server.wm.ActivityStack;
import com.android.server.wm.LaunchParamsController;
import com.vivo.services.superresolution.Constant;
import com.vivo.smartmultiwindow.IVivoSmartMultiWindowHelper;
import java.util.ArrayList;
import java.util.Map;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoFreeformActivityManager {
    static final String FREEFORM_MINIMIZE = "freeform_minimize";
    private static final Uri FREEFORM_MINIMIZE_URI = Uri.parse(Settings.System.CONTENT_URI + "/" + FREEFORM_MINIMIZE);
    static final String FREEFORM_RESIZE = "freeform_resize";
    private static final Uri FREEFORM_RESIZE_URI = Uri.parse(Settings.System.CONTENT_URI + "/" + FREEFORM_RESIZE);
    private static final String MULTI_WINDOW_WHITELIST_SWITCH_FREEFORM = "com.vivo.smartmultiwindow.whitelist.switch.freeform";
    private static final Uri MULTI_WINDOW_WHITELIST_URI_FREEFORM = Uri.parse(Settings.System.CONTENT_URI + "/" + MULTI_WINDOW_WHITELIST_SWITCH_FREEFORM);
    private static final String TAG = "VivoFreeformActivityManager";
    private boolean isFreeFormMax;
    private boolean isFreeFormMin;
    private boolean isVivoFreeformListValid;
    private ActivityTaskManagerService mActivityTaskManagerService;
    private boolean mCurrentRotationHasResized;
    private ArrayList<String> mForceFullScreenActivitylistFreeform;
    private ArrayList<String> mFreeFormEmergentActivity;
    private ArrayList<String> mFreeFormEnabledApp;
    private ArrayList<String> mFreeFormFullScreenApp;
    private int mFreeFormFullScreenAppSize;
    private ActivityRecord mFreeformKeepR;
    private String mFreeformPkg;
    private VivoFreeformMultiWindowConfig mFreeformSmartMultiWindowConfig;
    private ActivityManager.StackInfo mFreeformStackinfo;
    public final boolean mIsExport;
    private boolean mIsFirstTimeUnlock;
    private boolean mIsInDirectFreeformState;
    private boolean mIsStartingEmergent;
    private boolean mIsStartingPasswdOnHome;
    private boolean mIsStartingPassword;
    private boolean mIsStartingRecentOnHome;
    private boolean mIsUnlockingToFreeform;
    private boolean mIsVivoFreeformRuntimeEnable;
    private boolean mLastExitFromDirectFreeform;
    private VivoMultiWindowTransManager mMultiWindowWmsInstance;
    private Task mPrevVivoFreeformTask;
    private VivoFreeformMultiWindowObserver mVivoFreeformMultiWindowObserver;
    private VivoFreeformWindowManager mVivoFreeformWindowManager;
    private boolean resizeTaskFreeform;

    public boolean isCurrentRotationHasResized() {
        return this.mCurrentRotationHasResized;
    }

    public void setCurrentRotationHasResized(boolean currentRotationHasResized) {
        this.mCurrentRotationHasResized = currentRotationHasResized;
    }

    private VivoFreeformActivityManager() {
        this.mFreeformStackinfo = new ActivityManager.StackInfo();
        this.mFreeformKeepR = null;
        this.resizeTaskFreeform = false;
        this.isFreeFormMax = false;
        this.isFreeFormMin = false;
        this.mIsVivoFreeformRuntimeEnable = false;
        this.mIsInDirectFreeformState = false;
        this.mFreeFormEnabledApp = new ArrayList<>();
        this.mFreeFormEmergentActivity = new ArrayList<>();
        this.mFreeFormFullScreenApp = new ArrayList<>();
        this.mForceFullScreenActivitylistFreeform = new ArrayList<>();
        this.mFreeformSmartMultiWindowConfig = VivoFreeformMultiWindowConfig.getInstance();
        this.isVivoFreeformListValid = true;
        this.mIsExport = "yes".equals(SystemProperties.get("ro.vivo.product.overseas", "no"));
        this.mFreeFormFullScreenAppSize = 0;
        this.mMultiWindowWmsInstance = VivoMultiWindowTransManager.getInstance();
        this.mIsStartingPassword = false;
        this.mIsStartingPasswdOnHome = false;
        this.mIsStartingRecentOnHome = false;
        this.mLastExitFromDirectFreeform = false;
        this.mIsStartingEmergent = false;
        this.mIsFirstTimeUnlock = false;
        this.mFreeformPkg = null;
        this.mIsUnlockingToFreeform = false;
        this.mPrevVivoFreeformTask = null;
        this.mCurrentRotationHasResized = false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class VivoFreeformActivityManagerHolder {
        private static final VivoFreeformActivityManager sVivoFreeformActivityManager = new VivoFreeformActivityManager();

        private VivoFreeformActivityManagerHolder() {
        }
    }

    public static VivoFreeformActivityManager getInstance() {
        return VivoFreeformActivityManagerHolder.sVivoFreeformActivityManager;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void init(ActivityTaskManagerService atm) {
        this.mActivityTaskManagerService = atm;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityRecord getFreeformKeepR() {
        return this.mFreeformKeepR;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setFreeformKeepR(ActivityRecord freeformKeepR) {
        this.mFreeformKeepR = freeformKeepR;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isResizeTaskFreeform() {
        return this.resizeTaskFreeform;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setResizeTaskFreeform(boolean resizeTaskFreeform) {
        this.resizeTaskFreeform = resizeTaskFreeform;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isFreeFormMin() {
        return this.isFreeFormMin;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setFreeFormMin(boolean freeFormMin) {
        this.isFreeFormMin = freeFormMin;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArrayList<String> getFreeFormEnabledApp() {
        return this.mFreeFormEnabledApp;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArrayList<String> getFreeFormEmergentActivity() {
        return this.mFreeFormEmergentActivity;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArrayList<String> getFreeFormFullScreenApp() {
        return this.mFreeFormFullScreenApp;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArrayList<String> getForceFullScreenActivitylistFreeform() {
        return this.mForceFullScreenActivitylistFreeform;
    }

    VivoMultiWindowTransManager getMultiWindowWmsInstance() {
        return this.mMultiWindowWmsInstance;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isStartingPassword() {
        return this.mIsStartingPassword;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setIsStartingPassword(boolean isStartingPassword) {
        this.mIsStartingPassword = isStartingPassword;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isStartingPasswdOnHome() {
        return this.mIsStartingPasswdOnHome;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setIsStartingPasswdOnHome(boolean isStartingPasswdOnHome) {
        this.mIsStartingPasswdOnHome = isStartingPasswdOnHome;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isStartingRecentOnHome() {
        return this.mIsStartingRecentOnHome;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setIsStartingRecentOnHome(boolean isStartingRecentOnHome) {
        this.mIsStartingRecentOnHome = isStartingRecentOnHome;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isLastExitFromDirectFreeform() {
        return this.mLastExitFromDirectFreeform;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setLastExitFromDirectFreeform(boolean lastExitFromDirectFreeform) {
        this.mLastExitFromDirectFreeform = lastExitFromDirectFreeform;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isStartingEmergent() {
        return this.mIsStartingEmergent;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setIsStartingEmergent(boolean isStartingEmergent) {
        this.mIsStartingEmergent = isStartingEmergent;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isFirstTimeUnlock() {
        return this.mIsFirstTimeUnlock;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setIsFirstTimeUnlock(boolean isFirstTimeUnlock) {
        this.mIsFirstTimeUnlock = isFirstTimeUnlock;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isUnlockingToFreeform() {
        return this.mIsUnlockingToFreeform;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setIsUnlockingToFreeform(boolean isUnlockingToFreeform) {
        this.mIsUnlockingToFreeform = isUnlockingToFreeform;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void miniMizeWindowVivoFreeformMode(IBinder token, boolean mini) {
        ActivityStack fullscreenStack;
        Task topTask;
        ActivityStack freeformStack;
        ActivityRecord freeformStackTopActivity;
        if (token == null && ((freeformStack = this.mActivityTaskManagerService.mRootWindowContainer.getVivoFreeformStack()) == null || (freeformStackTopActivity = freeformStack.getTopMostActivity()) == null || !freeformStackTopActivity.nowVisible || (token = freeformStackTopActivity.appToken.asBinder()) == null)) {
            return;
        }
        ActivityRecord r = ActivityRecord.forTokenLocked(token);
        if (r == null) {
            throw new IllegalArgumentException("miniMizeWindowVivoFreeformMode: No activity record matching token=" + token);
        }
        ActivityStack stack = ActivityRecord.getStackLocked(token);
        if (stack == null || !stack.inFreeformWindowingMode() || stack.getDisplay() == null) {
            throw new IllegalStateException("miniMizeWindowVivoFreeformMode: You can only go fullscreen from freeform.");
        }
        DisplayContent freeformDisplay = stack.getDisplay();
        if (mini) {
            if (freeformDisplay.getFocusedStack() != stack && (topTask = stack.getTopMostTask()) != null) {
                this.mActivityTaskManagerService.setFocusedTask(topTask.mTaskId);
            }
            collectFreeformStackInfo(stack);
            this.isFreeFormMin = true;
            this.mActivityTaskManagerService.mWindowManager.setFreeFormMin(true);
            this.mActivityTaskManagerService.mWindowManager.setFreeformMiniStateChanged(true);
            if (!isInDirectFreeformState()) {
                fullscreenStack = freeformDisplay.getDefaultTaskDisplayArea().getStack(1, 1);
            } else {
                fullscreenStack = freeformDisplay.getDefaultTaskDisplayArea().getTopStackInWindowingMode(1);
            }
            ActivityRecord fullScreenAr = fullscreenStack != null ? fullscreenStack.topRunningActivityLocked() : null;
            stack.getDisplay().mDisplayContent.prepareAppTransition(11, false);
            stack.startPausingLocked(false, false, (ActivityRecord) null);
            if (fullScreenAr != null && fullScreenAr.moveFocusableActivityToTop("minimizeFreeForm")) {
                this.mActivityTaskManagerService.mRootWindowContainer.resumeFocusedStacksTopActivities();
                if (fullScreenAr.getTask() != null) {
                    this.mActivityTaskManagerService.mStackSupervisor.mRecentTasks.add(fullScreenAr.getTask());
                }
                stack.getDisplay().mDisplayContent.executeAppTransition();
            }
        } else {
            this.mActivityTaskManagerService.mWindowManager.setClosingFreeForm(true);
            VCD_FF_1.VCD_FF_4(this.mActivityTaskManagerService.mContext, isInDirectFreeformState(), r.packageName);
        }
        try {
            int currentUserId = this.mActivityTaskManagerService.mAmInternal.getCurrentUserId();
            Settings.System.putIntForUser(this.mActivityTaskManagerService.mContext.getContentResolver(), FREEFORM_MINIMIZE, mini ? 1 : 0, currentUserId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ActivityManager.StackInfo getVivoFreeformStackInfo() {
        VSlog.d(TAG, "getVivoFreeformStackInfo mFreeformStackinfo :" + this.mFreeformStackinfo);
        return this.mFreeformStackinfo;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void enterResizeVivoFreeformMode(IBinder token, boolean enter) {
        ActivityRecord r = ActivityRecord.forTokenLocked(token);
        if (r == null) {
            throw new IllegalArgumentException("enterResizeVivoFreeformMode: No activity record matching token=" + token);
        }
        ActivityStack stack = ActivityRecord.getStackLocked(token);
        if (stack == null || !stack.inFreeformWindowingMode() || stack.getDisplay() == null) {
            throw new IllegalStateException("enterResizeVivoFreeformMode: You can only enter resize mode from freeform.");
        }
        if (enter) {
            try {
                int currentUserId = this.mActivityTaskManagerService.mAmInternal.getCurrentUserId();
                Settings.System.putIntForUser(this.mActivityTaskManagerService.mContext.getContentResolver(), FREEFORM_RESIZE, 1, currentUserId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isInVivoFreeformMode(IBinder token) {
        ActivityRecord r = ActivityRecord.isInStackLocked(token);
        return r != null && isVivoFreeFormValid() && r.inFreeformWindowingMode();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void enableVivoFreeFormRuntime(boolean enable, boolean inDirectFreeformState) {
        this.mIsVivoFreeformRuntimeEnable = enable;
        this.mIsInDirectFreeformState = inDirectFreeformState;
        VSlog.d(TAG, "enableVivoFreeFormRuntime enable:" + enable + " inDirectFreeformState:" + inDirectFreeformState);
        this.mActivityTaskManagerService.mWindowManager.enableVivoFreeFormRuntime(enable, inDirectFreeformState);
        if (enable && !inDirectFreeformState) {
            this.mPrevVivoFreeformTask = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateVivoFreeFormConfig(Map configsMap) {
        if (configsMap == null) {
            VSlog.d(TAG, "updateVivoFreeFormConfig config is null!");
            return;
        }
        this.mFreeformSmartMultiWindowConfig.setMultiWindowConfig(configsMap);
        this.mFreeFormEnabledApp = this.mFreeformSmartMultiWindowConfig.getMultiWindowConfig(11);
        this.mFreeFormFullScreenApp = this.mFreeformSmartMultiWindowConfig.getMultiWindowConfig(10);
        this.mFreeFormEmergentActivity = this.mFreeformSmartMultiWindowConfig.getMultiWindowConfig(12);
        this.mForceFullScreenActivitylistFreeform = this.mFreeformSmartMultiWindowConfig.getMultiWindowConfig(14);
        this.mFreeFormFullScreenAppSize = this.mFreeFormFullScreenApp.size();
        this.mActivityTaskManagerService.mWindowManager.updateFreeformForceList(this.mForceFullScreenActivitylistFreeform);
        this.mVivoFreeformMultiWindowObserver.VivoFreeformListChanged(this.mActivityTaskManagerService.mContext);
    }

    public boolean isVivoFreeFormValid() {
        return VivoFreeformMultiWindowConfig.IS_VIVO_FREEFORM_SUPPORT && this.mIsVivoFreeformRuntimeEnable;
    }

    public boolean isInDirectFreeformState() {
        return this.mIsInDirectFreeformState;
    }

    public boolean isInVivoFreeform() {
        return this.mActivityTaskManagerService.mWindowManager.isInVivoFreeform();
    }

    public boolean isVivoFreeformListValid() {
        return this.isVivoFreeformListValid;
    }

    public boolean isFreeFormStackMax() {
        return this.isFreeFormMax;
    }

    public int getDisplayRotation(DisplayContent display) {
        synchronized (this) {
            long ident = Binder.clearCallingIdentity();
            if (display != null) {
                int rotation = display.getRotation();
                Binder.restoreCallingIdentity(ident);
                return rotation;
            }
            Binder.restoreCallingIdentity(ident);
            return 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateFreeformTaskUseGlobalConfig(Configuration tempGlobalConfig) {
        Configuration freeformTopTaskOverrideConfig;
        if (isVivoFreeFormValid() && isInVivoFreeform() && isFreeFormStackMax()) {
            ActivityStack freeformStack = this.mActivityTaskManagerService.mRootWindowContainer.getVivoFreeformStack();
            Task freeformTopTask = freeformStack != null ? freeformStack.getTopMostTask() : null;
            if (freeformTopTask != null && (freeformTopTaskOverrideConfig = freeformTopTask.getRequestedOverrideConfiguration()) != null && freeformTopTaskOverrideConfig.windowConfiguration.getAppBounds() != null && !freeformTopTaskOverrideConfig.windowConfiguration.getAppBounds().equals(tempGlobalConfig.windowConfiguration.getAppBounds()) && freeformTopTask.getRequestedOverrideBounds() != null && !freeformTopTask.getRequestedOverrideBounds().equals(tempGlobalConfig.windowConfiguration.getAppBounds())) {
                freeformTopTask.setBounds(tempGlobalConfig.windowConfiguration.getAppBounds());
            }
        }
    }

    public void notifyFreeFormStackMaxChanged(boolean fullScreen) {
        synchronized (this) {
            long ident = Binder.clearCallingIdentity();
            this.isFreeFormMax = fullScreen;
            this.mActivityTaskManagerService.mWindowManager.notifyFreeFormStackMaxChanged(fullScreen);
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class VivoFreeformMultiWindowObserver extends ContentObserver {
        VivoFreeformMultiWindowObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            ContentResolver cr;
            if (uri != null) {
                if (VivoFreeformActivityManager.MULTI_WINDOW_WHITELIST_URI_FREEFORM.equals(uri)) {
                    VivoFreeformListChanged(VivoFreeformActivityManager.this.mActivityTaskManagerService.mContext);
                }
                boolean z = true;
                if (VivoFreeformActivityManager.FREEFORM_MINIMIZE_URI.equals(uri)) {
                    ContentResolver cr2 = VivoFreeformActivityManager.this.mActivityTaskManagerService.mContext.getContentResolver();
                    if (cr2 != null) {
                        try {
                            int currentUserId = VivoFreeformActivityManager.this.mActivityTaskManagerService.mAmInternal.getCurrentUserId();
                            VivoFreeformActivityManager.this.isFreeFormMin = Settings.System.getIntForUser(cr2, VivoFreeformActivityManager.FREEFORM_MINIMIZE, 0, currentUserId) == 1;
                        } catch (Exception e) {
                            VSlog.e(VivoFreeformActivityManager.TAG, "VivoFreeformMultiWindowObserver FREEFORM_MINIMIZE_URI Exception in AMS");
                            return;
                        }
                    } else {
                        return;
                    }
                }
                if (VivoFreeformActivityManager.FREEFORM_RESIZE_URI.equals(uri) && (cr = VivoFreeformActivityManager.this.mActivityTaskManagerService.mContext.getContentResolver()) != null) {
                    try {
                        int currentUserId2 = VivoFreeformActivityManager.this.mActivityTaskManagerService.mAmInternal.getCurrentUserId();
                        WindowManagerService windowManagerService = VivoFreeformActivityManager.this.mActivityTaskManagerService.mWindowManager;
                        if (Settings.System.getIntForUser(cr, VivoFreeformActivityManager.FREEFORM_RESIZE, 0, currentUserId2) != 1) {
                            z = false;
                        }
                        windowManagerService.setFreeFormResizing(z);
                    } catch (Exception e2) {
                        VSlog.e(VivoFreeformActivityManager.TAG, "VivoFreeformMultiWindowObserver FREEFORM_RESIZE_URI Exception in AMS");
                    }
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void VivoFreeformListChanged(Context context) {
            ContentResolver cr;
            if (context != null && (cr = context.getContentResolver()) != null) {
                try {
                    int currentUserId = VivoFreeformActivityManager.this.mActivityTaskManagerService.mAmInternal.getCurrentUserId();
                    String value = Settings.System.getStringForUser(cr, VivoFreeformActivityManager.MULTI_WINDOW_WHITELIST_SWITCH_FREEFORM, currentUserId);
                    setVivoFreeformWhiteListSwitchValue(value);
                } catch (Exception e) {
                    VSlog.e(VivoFreeformActivityManager.TAG, "VivoFreeformMultiWindowObserver VivoFreeformListChanged Exception in AMS");
                    setVivoFreeformWhiteListSwitchValue("on");
                    e.printStackTrace();
                }
            }
        }

        private void setVivoFreeformWhiteListSwitchValue(String switchValue) {
            synchronized (this) {
                long ident = Binder.clearCallingIdentity();
                if ("on".equals(switchValue)) {
                    VivoFreeformActivityManager.this.mActivityTaskManagerService.mWindowManager.setVivoFreeformWhiteListSwitchValue(true);
                    VivoFreeformActivityManager.this.isVivoFreeformListValid = true;
                } else if ("off".equals(switchValue)) {
                    VivoFreeformActivityManager.this.mActivityTaskManagerService.mWindowManager.setVivoFreeformWhiteListSwitchValue(false);
                    VivoFreeformActivityManager.this.isVivoFreeformListValid = false;
                } else {
                    VivoFreeformActivityManager.this.mActivityTaskManagerService.mWindowManager.setVivoFreeformWhiteListSwitchValue(true);
                    VivoFreeformActivityManager.this.isVivoFreeformListValid = true;
                }
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerFreeformMultiWindowObserver() {
        this.mVivoFreeformMultiWindowObserver = new VivoFreeformMultiWindowObserver(this.mActivityTaskManagerService.mH);
        this.mActivityTaskManagerService.mContext.getContentResolver().registerContentObserver(MULTI_WINDOW_WHITELIST_URI_FREEFORM, false, this.mVivoFreeformMultiWindowObserver, -1);
        this.mActivityTaskManagerService.mContext.getContentResolver().registerContentObserver(FREEFORM_MINIMIZE_URI, false, this.mVivoFreeformMultiWindowObserver, -1);
        this.mActivityTaskManagerService.mContext.getContentResolver().registerContentObserver(FREEFORM_RESIZE_URI, false, this.mVivoFreeformMultiWindowObserver, -1);
    }

    boolean needFreeformRotation(ActivityRecord r) {
        if (r == null || r.appToken == null || r.getDisplay() == null || r.getDisplay().mDisplayContent == null || r.getDisplay().mDisplayContent.getRotation() == 0 || this.mActivityTaskManagerService.mWindowManager.getDefaultDisplayRotation() == 2) {
            return false;
        }
        int orientation = r.getOrientation();
        VSlog.d(TAG, "needFreeformRotation r = " + r + ", orientation = " + orientation);
        if (orientation != 1 && orientation != 9 && orientation != 7 && orientation != 5 && orientation != 12) {
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void getMultiWindowConnection() {
        try {
            IVivoSmartMultiWindowHelper.getInstance(this.mActivityTaskManagerService.mContext).bind();
        } catch (Exception e) {
            VSlog.e(TAG, "getMultiWindowConnection Exception in AMS");
            e.printStackTrace();
        }
    }

    private void collectFreeformStackInfo(ActivityStack stack) {
        String str;
        if (stack == null || !stack.inFreeformWindowingMode()) {
            return;
        }
        ActivityRecord freer = stack.topRunningActivityLocked();
        this.mFreeformStackinfo.stackId = stack.mTaskId;
        this.mFreeformStackinfo.stackToken = stack.mRemoteToken.toWindowContainerToken();
        this.mFreeformStackinfo.userId = freer != null ? freer.mUserId : 0;
        if (freer == null || !freer.isWhatsAppRegisterActivity()) {
            this.mFreeformStackinfo.topActivity = freer != null ? freer.mActivityComponent : null;
        } else {
            this.mFreeformStackinfo.topActivity = freer.intent.getComponent();
        }
        this.mFreeformStackinfo.displayId = stack.getDisplayId();
        this.mFreeformStackinfo.visible = true;
        this.mFreeformStackinfo.position = 0;
        Task task = freer != null ? freer.getTask() : null;
        int[] taskIds = new int[1];
        String[] taskNames = new String[1];
        Rect[] taskBounds = new Rect[1];
        int[] taskUserIds = new int[1];
        taskIds[0] = task != null ? task.mTaskId : 0;
        str = "unknown";
        if (task != null) {
            taskNames[0] = freer.mActivityComponent != null ? freer.mActivityComponent.flattenToString() : "unknown";
        } else {
            if (task.origActivity != null) {
                str = task.origActivity.flattenToString();
            } else if (task.realActivity != null) {
                str = task.realActivity.flattenToString();
            } else if (task.getTopActivity(false, false) != null) {
                str = task.getTopActivity(false, false).packageName;
            }
            taskNames[0] = str;
        }
        taskBounds[0] = new Rect();
        task.getBounds(taskBounds[0]);
        this.mActivityTaskManagerService.mWindowManager.scaleFreeformBack(taskBounds[0]);
        this.mFreeformStackinfo.bounds.set(taskBounds[0]);
        taskUserIds[0] = this.mFreeformStackinfo.userId;
        this.mFreeformStackinfo.taskIds = taskIds;
        this.mFreeformStackinfo.taskNames = taskNames;
        this.mFreeformStackinfo.taskBounds = taskBounds;
        this.mFreeformStackinfo.taskUserIds = taskUserIds;
    }

    public void setNormalFreezingAnimaiton(IBinder token) {
        VivoMultiWindowTransManager vivoMultiWindowTransManager = this.mMultiWindowWmsInstance;
        if (vivoMultiWindowTransManager != null) {
            vivoMultiWindowTransManager.prepareNormalTransFreezeWindowFrame();
            this.mMultiWindowWmsInstance.waitExitMultiWindowFreezeAnimation(token);
        }
    }

    public void setShortFreezingAnimaiton(IBinder token) {
        VivoMultiWindowTransManager vivoMultiWindowTransManager = this.mMultiWindowWmsInstance;
        if (vivoMultiWindowTransManager != null) {
            vivoMultiWindowTransManager.prepareShortTransFreezeWindowFrame();
            this.mMultiWindowWmsInstance.waitExitMultiWindowFreezeAnimation(token);
        }
    }

    public boolean moveTaskToBackWhenFinishActivityForWechat(ActivityRecord r, IBinder token) {
        if (isVivoFreeFormValid() && isInVivoFreeform() && this.mActivityTaskManagerService.mWindowManager.isClosingFreeForm()) {
            if ((Constant.APP_WEIXIN.equals(r.mActivityComponent.getPackageName()) || "com.tencent.qqlive".equals(r.mActivityComponent.getPackageName())) && !"com.tencent.mm/.ui.chatting.gallery.ImageGalleryUI".equals(r.mActivityComponent.flattenToShortString()) && !"com.tencent.mm/.plugin.fts.ui.FTSMainUI".equals(r.mActivityComponent.flattenToShortString()) && !isInDirectFreeformState()) {
                boolean res = this.mActivityTaskManagerService.moveActivityTaskToBack(token, true);
                if (!res) {
                    VSlog.d(TAG, "Move task to back for wechat failed");
                }
                return true;
            }
            return false;
        }
        return false;
    }

    public void ensureFocusForVivoFreeform(ActivityRecord r) {
        if (isVivoFreeFormValid() && isInVivoFreeform() && !isFreeFormStackMax() && this.mActivityTaskManagerService.mWindowManager != null && r.getDisplay() != null) {
            r.getDisplay().setFocusedApp(r, true);
            ActivityStack freeformStack = this.mActivityTaskManagerService.mRootWindowContainer.getVivoFreeformStack();
            if (freeformStack != null) {
                freeformStack.resetCheckFocusState();
            }
        }
    }

    public void moveFreeformWindowToTopWhenSetFocusTask(ActivityRecord r) {
        ActivityStack freeform;
        if (isVivoFreeFormValid() && isInVivoFreeform() && !isFreeFormMin() && r.getWindowingMode() == 1 && (freeform = this.mActivityTaskManagerService.mRootWindowContainer.getVivoFreeformStack()) != null) {
            freeform.moveFreeformWindowStateToTop();
        }
    }

    public boolean ignoreThrowExceptionWhenResizeTask(Task task) {
        VSlog.d(TAG, "resize task:" + task + " isVivoFreeFormValid:" + isVivoFreeFormValid() + " isInFreeForm:" + isInVivoFreeform() + " mIsRemovingFreeformStack:" + this.mActivityTaskManagerService.mWindowManager.isRemovingFreeformStack() + " exitingFreeForm:" + this.mActivityTaskManagerService.mWindowManager.isExitingFreeForm());
        if (isVivoFreeFormValid() || this.mActivityTaskManagerService.mWindowManager.isRemovingFreeformStack() || this.mActivityTaskManagerService.mWindowManager.isExitingFreeForm()) {
            return true;
        }
        return false;
    }

    public boolean toggleVivoFreeformWindowingMode(ActivityStack stack, ActivityRecord r) {
        ActivityStack curFullStack;
        ActivityStack fullstack;
        if (isVivoFreeFormValid()) {
            boolean isDirectFreeformExit = false;
            if (isVivoFreeFormValid() && isInVivoFreeform() && isInDirectFreeformState()) {
                isDirectFreeformExit = true;
            }
            DisplayContent currentDisplay = stack.getDisplay();
            if (currentDisplay != null) {
                curFullStack = currentDisplay.getStack(1, 1);
            } else {
                curFullStack = this.mActivityTaskManagerService.mRootWindowContainer.getDefaultDisplay().getStack(1, 1);
            }
            if (curFullStack != null && curFullStack.getResumedActivity() != null && curFullStack.topRunningActivityLocked() != null) {
                curFullStack.startPausingLocked(false, false, (ActivityRecord) null);
            }
            if (currentDisplay != null) {
                fullstack = currentDisplay.getDefaultTaskDisplayArea().createStack(1, 1, true);
            } else {
                fullstack = this.mActivityTaskManagerService.mRootWindowContainer.getDefaultDisplay().getDefaultTaskDisplayArea().createStack(1, 1, true);
            }
            setFreeformKeepR(fullstack != null ? fullstack.topRunningActivityLocked() : null);
            r.getTask().reparent(fullstack, true, 1, !needFreeformRotation(r), false, "exitFreeformMode");
            setFreeformKeepR(null);
            if (r.getState() == ActivityStack.ActivityState.RESUMED && r.app != null) {
                this.mActivityTaskManagerService.mAmInternal.sendProcessActivityChangeMessage(r.app.getPid(), r.info.applicationInfo.uid);
            }
            VCD_FF_1.VCD_FF_3(this.mActivityTaskManagerService.mContext, isDirectFreeformExit, r.packageName);
            VSlog.d(TAG, "toggleVivoFreeformWindowingMode end r:" + r);
            return true;
        }
        return false;
    }

    public void getFreeformPkg() {
        ActivityManager.StackInfo freeformStackInfo;
        if (isVivoFreeFormValid() && isInVivoFreeform() && (freeformStackInfo = this.mActivityTaskManagerService.getStackInfo(5, 1)) != null && freeformStackInfo.topActivity != null) {
            this.mFreeformPkg = freeformStackInfo.topActivity.getPackageName();
        }
    }

    public boolean updateFreeformAppConfig(WindowProcessController app, Configuration tempConfig) {
        String str = this.mFreeformPkg;
        if (str != null && str.equals(app.mName)) {
            this.mFreeformPkg = null;
            Configuration freeFormConfig = new Configuration(tempConfig);
            freeFormConfig.windowConfiguration.setWindowingMode(5);
            app.onConfigurationChanged(freeFormConfig);
            VSlog.d(TAG, "updateGlobalConfigurationLocked change freeform mode");
            return true;
        }
        return false;
    }

    public void exitFreeformWhenLockTask() {
        if (this.mActivityTaskManagerService.isVivoFreeFormValid()) {
            ActivityStack freeformStack = this.mActivityTaskManagerService.mRootWindowContainer.getVivoFreeformStack();
            if (this.mActivityTaskManagerService.isInVivoFreeform() && freeformStack != null && freeformStack.getDisplayArea() != null) {
                freeformStack.getDisplayArea().moveVivoFreeformTasksToFullscreenStackLocked(this.mActivityTaskManagerService.mRootWindowContainer.getTopDisplayFocusedStack() == freeformStack);
            }
        }
    }

    public void sendProcessChangeForGame() {
        DisplayContent topFocusDisplay = this.mActivityTaskManagerService.mRootWindowContainer.getTopFocusedDisplayContent();
        if (topFocusDisplay == null) {
            VSlog.d(TAG, "sendProcessChangeForGame focus display is null.");
            return;
        }
        ActivityStack fullscreenStack = topFocusDisplay.getStack(1, 1);
        ActivityRecord fullscreenTop = fullscreenStack != null ? fullscreenStack.topRunningActivityLocked() : null;
        if (fullscreenTop != null && fullscreenTop.nowVisible && fullscreenTop.app != null && !this.isFreeFormMin) {
            this.mActivityTaskManagerService.mAmInternal.sendProcessActivityChangeMessageOnce(fullscreenTop.app.getPid(), fullscreenTop.info.applicationInfo.uid);
        }
    }

    public void checkAndExitFreeformWhenSlideToHome(int targetActivityType, ActivityRecord launchedTargetActivity, int reorderMode) {
        DisplayContent defaultDisplay;
        ActivityRecord targetActivity;
        ActivityRecord topResume;
        if (isVivoFreeFormValid() && isInVivoFreeform()) {
            if ((isInDirectFreeformState() && ((topResume = this.mActivityTaskManagerService.mRootWindowContainer.getTopResumedActivity()) == null || !topResume.isPasswordActivity())) || (defaultDisplay = this.mActivityTaskManagerService.mRootWindowContainer.getDefaultDisplay()) == null) {
                return;
            }
            ActivityStack targetStack = defaultDisplay.getStack(0, targetActivityType);
            if (targetStack != null) {
                targetActivity = targetStack.isInTask(launchedTargetActivity);
            } else {
                targetActivity = null;
            }
            if (reorderMode == 1 && targetActivity != null && targetStack.isActivityTypeHome()) {
                this.mActivityTaskManagerService.mRootWindowContainer.getDefaultTaskDisplayArea().moveVivoFreeformTasksToFullscreenStackLocked(false);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isMultiDisplyPhone() {
        Display display = this.mActivityTaskManagerService.mRootWindowContainer.mDisplayManager.getDisplay(4096);
        return display != null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void moveFreeformTaskToSecondDisplay(IBinder token) {
        ActivityRecord r = ActivityRecord.forTokenLocked(token);
        if (r == null) {
            throw new IllegalArgumentException("moveFreeformTaskToSecondDisplay: No activity record matching token=" + token);
        }
        ActivityStack stack = r.getStack();
        if (stack == null || !stack.inFreeformWindowingMode() || stack.getDisplay() == null) {
            throw new IllegalStateException("moveFreeformTaskToSecondDisplay: You can only go fullscreen from freeform.");
        }
        if (ActivityTaskManagerDebugConfig.DEBUG_STACK) {
            VSlog.d(TAG, "moveFreeformTaskToSecondDisplay: " + r);
        }
        if (!isMultiDisplyPhone() || !isVivoFreeFormValid() || !isInVivoFreeform()) {
            return;
        }
        Task task = r.getTask();
        boolean isInDirectFreeform = this.mActivityTaskManagerService.isInDirectFreeformState();
        DisplayContent targetDisplay = this.mActivityTaskManagerService.mRootWindowContainer.getDisplayContent(4096);
        ActivityStack targetStack = targetDisplay != null ? targetDisplay.getDefaultTaskDisplayArea().createStack(1, 1, true) : null;
        if (task == null || targetStack == null) {
            return;
        }
        if (targetStack.getWindowingMode() == 5) {
            targetStack.setWindowingMode(1);
        }
        this.mActivityTaskManagerService.mWindowManager.updateFreeformTaskSnapshot(task);
        task.reparent(targetStack, true, 1, true, false, "moveFreeformTaskToSecondDisplay");
        VCD_FF_1.VCD_FF_6(this.mActivityTaskManagerService.mContext, isInDirectFreeform);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean ignoreAlwaysCreateStackForVivoFreeform(int windowingMode, int activityType) {
        return activityType == 1 && windowingMode == 5 && isVivoFreeFormValid() && isInVivoFreeform();
    }

    public Task getPrevVivoFreeformTask() {
        return this.mPrevVivoFreeformTask;
    }

    public void setPrevVivoFreeformTask(Task prevVivoFreeformTask) {
        this.mPrevVivoFreeformTask = prevVivoFreeformTask;
    }

    public void exitCurrentFreeformTaskForTransitToNext(ActivityRecord source, Task next, LaunchParamsController.LaunchParams launchParams) {
        if (isVivoFreeFormValid() && isInVivoFreeform() && isInDirectFreeformState() && source != null && source.inFreeformWindowingMode() && launchParams.mWindowingMode == 5) {
            if (next.isFreeFormEnabledApps()) {
                if (launchParams.mPreferredTaskDisplayArea != null && launchParams.mPreferredTaskDisplayArea == source.getDisplayArea()) {
                    if (source.getTask() != null) {
                        launchParams.mBounds.set(source.getTask().getBounds());
                    }
                    float freeformScale = this.mActivityTaskManagerService.mWindowManager.getFreeformScale();
                    launchParams.mPreferredTaskDisplayArea.moveVivoFreeformTasksToFullscreenStackLocked(false);
                    this.mActivityTaskManagerService.enableVivoFreeFormRuntime(true, true, freeformScale);
                    setPrevVivoFreeformTask(source.getTask());
                    Settings.System.putIntForUser(this.mActivityTaskManagerService.mContext.getContentResolver(), VivoFreeformUtils.VIVO_SETTINGS_IN_FREEFORM_TRANSIT, 1, this.mActivityTaskManagerService.mAmInternal.getCurrentUserId());
                    return;
                }
                return;
            }
            launchParams.mWindowingMode = 1;
        } else if (launchParams.mWindowingMode == 5) {
            launchParams.mWindowingMode = 1;
        }
    }

    public void setFreeformResizedInCurrentRotationIfNeed(Task task) {
        if (!this.mCurrentRotationHasResized && task != null && task.inFreeformWindowingMode()) {
            this.mCurrentRotationHasResized = true;
            int currentUserId = this.mActivityTaskManagerService.mAmInternal.getCurrentUserId();
            Settings.System.putIntForUser(this.mActivityTaskManagerService.mContext.getContentResolver(), VivoFreeformUtils.VIVO_SETTINGS_FREEFORM_RESIZED_IN_CURRENT_ROTATION, 1, currentUserId);
        }
    }
}