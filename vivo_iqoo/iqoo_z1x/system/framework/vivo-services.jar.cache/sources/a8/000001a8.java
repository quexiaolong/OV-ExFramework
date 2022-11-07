package com.android.server.display;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.biometrics.fingerprint.FingerprintUIManagerInternal;
import android.hardware.display.DisplayManager;
import android.hardware.display.IDisplayManagerCallback;
import android.multidisplay.MultiDisplayManager;
import android.multidisplay.MultiDisplayManagerInternal;
import android.os.Binder;
import android.os.Build;
import android.os.FtBuild;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.SparseArray;
import android.view.Display;
import android.view.SurfaceControl;
import com.android.server.LocalServices;
import com.android.server.display.DisplayManagerService;
import com.android.server.inputmethod.InputMethodManagerInternal;
import com.android.server.policy.VivoRatioControllerUtilsImpl;
import com.android.server.wm.VCD_FF_1;
import com.android.server.wm.VivoAppShareManager;
import com.vivo.appshare.AppShareConfig;
import com.vivo.common.VivoCollectData;
import com.vivo.sensor.autobrightness.config.AblConfig;
import com.vivo.sensor.autobrightness.config.BootBrightnessConfig;
import com.vivo.sensor.implement.SensorConfig;
import com.vivo.services.rms.RmsInjectorImpl;
import com.vivo.services.rms.display.RefreshRateAdjuster;
import com.vivo.services.rms.display.SceneManager;
import com.vivo.services.security.server.VivoPermissionUtils;
import com.vivo.vcodetransbase.EventTransfer;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoDisplayManagerServiceImpl implements IVivoDisplayManagerService {
    private static final String BBKLOG_ACTION = "android.vivo.bbklog.action.CHANGED";
    private static final String BBKLOG_STATUS = "adblog_status";
    private static boolean DEBUG_APP_SHARE = false;
    public static boolean DEBUG_CASTDISPLAY = false;
    private static final String FORCE_STATE_ON_REASON = "AppShare";
    private static final boolean IS_ENG;
    private static final boolean SMART_AOD_LCD;
    private static final String SURFACE_FLINGER = "SurfaceFlinger";
    private static final int SURFACE_FLINGER_TRANSACTION_SET_SIX_ZONE = 31016;
    static final String TAG = "VivoDisplayManagerServiceImpl";
    private static final String TAG_APPSHARED = "AppShare-DisplayManagerService";
    private static final long WAKE_LOCK_TIMEOUT = 500;
    private static boolean isEventEnable;
    private DisplayManagerService mDisplayManagerService;
    String mFocusAppName;
    private boolean mFpRequestDraw;
    private Handler mHandler;
    private boolean mIsOverseas;
    private MultiDisplayManagerInternal mMultiDisplayManagerInternal;
    private PowerManager mPowerManager;
    String mRecordAppName;
    long mRecordStartTime;
    VivoAppShareManager mVivoAppShareManager;
    private VivoRatioControllerUtilsImpl mVivoRatioControllerUtils;
    private PowerManager.WakeLock mWakeLock;
    private final Object mStateLock = new Object();
    protected int mOverridePrimaryState = 2;
    protected int mOverrideSecondState = 1;
    private int mOverridePrimaryBrightness = -1;
    private int mOverrideSecondBrightness = -1;
    private HashMap<String, Boolean> mForceStateOnMap = new HashMap<>();
    private float mTempBrightness = -1.0f;
    private String mAppSharePackageName = null;
    private int mAppShareUserId = -1;
    private PackageManager mPackageManager = null;
    private boolean mIsAppResumed = true;
    private DisplayManager mDisplayManager = null;
    private boolean mIsAppSharing = false;
    private int mState = 2;
    private boolean mForceOn = false;
    private Runnable mEmptyWork = new Runnable() { // from class: com.android.server.display.VivoDisplayManagerServiceImpl.2
        @Override // java.lang.Runnable
        public void run() {
        }
    };
    Runnable mSetOverrideWrapRunnable = new Runnable() { // from class: com.android.server.display.VivoDisplayManagerServiceImpl.5
        @Override // java.lang.Runnable
        public void run() {
            VivoDisplayManagerServiceImpl.this.mWakeLock.acquire(VivoDisplayManagerServiceImpl.WAKE_LOCK_TIMEOUT);
        }
    };
    private final ArrayList<ColorLock> mColorLocks = new ArrayList<>();
    private boolean mShouldSetColorMode = false;
    private final String EVENT_ID = "F351";
    private final String SUB_EVENT_ID = "F351|10001";
    private final String KEY_CAST_DISPLAY = "pkg_info";
    int virtualtype = 0;
    final ArrayMap<String, Integer> mRecordAppMap = new ArrayMap<>();
    int sVivoSpecVirtualDisplayId = SceneManager.APP_REQUEST_PRIORITY;
    private final Runnable mUpdateDisplayStateRunnable = new Runnable() { // from class: com.android.server.display.VivoDisplayManagerServiceImpl.6
        @Override // java.lang.Runnable
        public void run() {
            boolean z = true;
            if (VivoDisplayManagerServiceImpl.this.mState == 3 || VivoDisplayManagerServiceImpl.this.mState == 4) {
                if (VivoDisplayManagerServiceImpl.this.mIsAppSharing && VivoDisplayManagerServiceImpl.this.mIsAppResumed) {
                    VivoDisplayManagerServiceImpl.this.setForceDisplayStateOn(true, VivoDisplayManagerServiceImpl.FORCE_STATE_ON_REASON, false);
                    return;
                } else {
                    VivoDisplayManagerServiceImpl.this.setForceDisplayStateOn(false, VivoDisplayManagerServiceImpl.FORCE_STATE_ON_REASON, false);
                    return;
                }
            }
            VivoDisplayManagerServiceImpl vivoDisplayManagerServiceImpl = VivoDisplayManagerServiceImpl.this;
            if (vivoDisplayManagerServiceImpl.mState != 2 && VivoDisplayManagerServiceImpl.this.mState != 6) {
                z = false;
            }
            vivoDisplayManagerServiceImpl.setForceDisplayStateOn(false, VivoDisplayManagerServiceImpl.FORCE_STATE_ON_REASON, z);
        }
    };

    static {
        SMART_AOD_LCD = 1 == SystemProperties.getInt("persist.vivo.phone.smart_aod", 0);
        IS_ENG = "eng".equals(Build.TYPE);
        DEBUG_APP_SHARE = true;
        isEventEnable = false;
        DEBUG_CASTDISPLAY = "yes".equals(SystemProperties.get("persist.sys.log.ctrl", "no"));
    }

    public VivoDisplayManagerServiceImpl(DisplayManagerService displayMgrService) {
        this.mIsOverseas = false;
        this.mVivoAppShareManager = null;
        if (displayMgrService == null) {
            VSlog.i(TAG, "container is " + displayMgrService);
        }
        this.mDisplayManagerService = displayMgrService;
        this.mIsOverseas = FtBuild.isOverSeas();
        this.mVivoRatioControllerUtils = VivoRatioControllerUtilsImpl.getInstance();
        this.mVivoAppShareManager = VivoAppShareManager.getInstance();
    }

    public void dummy() {
    }

    public void systemReady() {
        this.mMultiDisplayManagerInternal = (MultiDisplayManagerInternal) LocalServices.getService(MultiDisplayManagerInternal.class);
        PowerManager powerManager = (PowerManager) this.mDisplayManagerService.getContext().getSystemService("power");
        this.mPowerManager = powerManager;
        boolean z = true;
        this.mWakeLock = powerManager.newWakeLock(1, "VivoBaseDisplayManagerService.mWakeLock");
        HandlerThread thread = new HandlerThread("VivoBaseDisplay");
        thread.start();
        this.mHandler = thread.getThreadHandler();
        initFingerprintDisplayState(this.mDisplayManagerService.mGlobalDisplayState, this.mTempBrightness);
        VivoCastDisplayUtil.getInstance(this.mDisplayManagerService.getContext(), this.mHandler, this.mDisplayManagerService.mUiHandler);
        RefreshRateAdjuster.getInstance().initialize(this.mDisplayManagerService.getContext());
        this.mDisplayManager = (DisplayManager) this.mDisplayManagerService.getContext().getSystemService(DisplayManager.class);
        if (AppShareConfig.SUPPROT_APPSHARE) {
            if (!IS_ENG && !"yes".equals(SystemProperties.get("persist.sys.log.ctrl", "no"))) {
                z = false;
            }
            DEBUG_APP_SHARE = z;
            IntentFilter bbklogFilter = new IntentFilter();
            bbklogFilter.addAction("android.vivo.bbklog.action.CHANGED");
            this.mDisplayManagerService.getContext().registerReceiver(new BroadcastReceiver() { // from class: com.android.server.display.VivoDisplayManagerServiceImpl.1
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context, Intent intent) {
                    boolean unused = VivoDisplayManagerServiceImpl.DEBUG_APP_SHARE = VivoDisplayManagerServiceImpl.IS_ENG || (intent != null && "on".equals(intent.getStringExtra(VivoDisplayManagerServiceImpl.BBKLOG_STATUS)));
                    VivoAppShareManager.setDebug(VivoDisplayManagerServiceImpl.DEBUG_APP_SHARE);
                }
            }, bbklogFilter, null, this.mHandler);
            this.mDisplayManager.registerDisplayListener(new AppShareDisplayListener(), this.mHandler);
        }
    }

    public Runnable updateDisplayState(DisplayDevice device) {
        LogicalDisplay logicalDisplay = this.mDisplayManagerService.findLogicalDisplayForDeviceLocked(device);
        if (logicalDisplay != null) {
            int displayId = logicalDisplay.getDisplayIdLocked();
            StringBuilder sb = new StringBuilder();
            sb.append("updateDisplayStateLocked : displayId = ");
            sb.append(displayId);
            sb.append(" ; mGlobalDisplayState = ");
            sb.append(this.mDisplayManagerService.mGlobalDisplayState);
            sb.append(" ; mGlobalDisplayBrightness = ");
            sb.append(this.mDisplayManagerService.mGlobalDisplayBrightness);
            sb.append(" ; mOverrideState = ");
            sb.append(displayId == 0 ? this.mOverridePrimaryState : this.mOverrideSecondState);
            sb.append(" ; mOverrideBrighetness = ");
            sb.append(displayId == 0 ? this.mOverridePrimaryBrightness : this.mOverrideSecondBrightness);
            VSlog.d("shuangping0705", sb.toString());
            if (displayId == 0) {
                this.mTempBrightness = this.mDisplayManagerService.mGlobalDisplayBrightness;
                if (SMART_AOD_LCD && this.mOverridePrimaryState == 4 && device.mForceStateFromTrueToFalse) {
                    VSlog.d(TAG, "fingerprint set forceStateOn from true to false, modify set state STATE_DOZE_SUSPEND to STATE_DOZE for SMART_AOD_LCD");
                    device.mForceStateFromTrueToFalse = false;
                    final int pendingPrimaryState = this.mOverridePrimaryState;
                    this.mOverridePrimaryState = 3;
                    Runnable work = updateDisplayStateInternal(device, 3, this.mOverridePrimaryBrightness);
                    this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.display.VivoDisplayManagerServiceImpl.3
                        @Override // java.lang.Runnable
                        public void run() {
                            VSlog.d(VivoDisplayManagerServiceImpl.TAG, "fingerprint reset pending STATE_DOZE_SUSPEND here");
                            VivoDisplayManagerServiceImpl.this.BinderService_setOverrideDisplayState(0, pendingPrimaryState);
                            VivoDisplayManagerServiceImpl.this.requestGlobalDisplayStateInternal();
                        }
                    }, WAKE_LOCK_TIMEOUT);
                    return work;
                }
                Runnable work2 = updateDisplayStateInternal(device, this.mOverridePrimaryState, this.mOverridePrimaryBrightness);
                return work2;
            } else if (displayId != 4096) {
                return null;
            } else {
                Runnable work3 = updateDisplayStateInternal(device, this.mOverrideSecondState, this.mOverrideSecondBrightness);
                return work3;
            }
        }
        VSlog.d(TAG, "updateDisplayState : logicalDisplay is null, return!");
        return null;
    }

    private Runnable updateDisplayStateInternal(DisplayDevice device, int overrideState, int overrideBrightness) {
        if (shouldApplyOverrideState(overrideState)) {
            float brightness = overrideBrightness >= 0 ? overrideBrightness : overrideState == 2 ? this.mDisplayManagerService.mGlobalDisplayBrightness : -1.0f;
            Runnable work = device.requestDisplayStateLocked(overrideState, brightness);
            if (work == null) {
                return this.mEmptyWork;
            }
            return work;
        }
        return this.mEmptyWork;
    }

    private boolean shouldApplyOverrideState(int overrideState) {
        if (this.mDisplayManagerService.mGlobalDisplayState == 1 && overrideState == 1) {
            return true;
        }
        return (this.mDisplayManagerService.mGlobalDisplayState == 2 && overrideState != 0) || overrideState == 3 || overrideState == 4;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void requestGlobalDisplayStateInternal() {
        synchronized (this.mDisplayManagerService.mTempDisplayStateWorkQueue) {
            synchronized (this.mDisplayManagerService.mSyncRoot) {
                this.mDisplayManagerService.applyGlobalDisplayStateLocked(this.mDisplayManagerService.mTempDisplayStateWorkQueue);
            }
            if (this.mDisplayManagerService.mGlobalDisplayState == 1 && (this.mOverridePrimaryState == 3 || this.mOverridePrimaryState == 4)) {
                this.mDisplayManagerService.mDisplayPowerController.updateBrightness();
            }
            for (int i = 0; i < this.mDisplayManagerService.mTempDisplayStateWorkQueue.size(); i++) {
                ((Runnable) this.mDisplayManagerService.mTempDisplayStateWorkQueue.get(i)).run();
            }
            this.mDisplayManagerService.mTempDisplayStateWorkQueue.clear();
        }
    }

    public void onGlobalDisplayStateChanged(final int state) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.display.VivoDisplayManagerServiceImpl.4
            @Override // java.lang.Runnable
            public void run() {
                if (state == 2) {
                    VivoDisplayManagerServiceImpl.this.mMultiDisplayManagerInternal.onWakefulnessChanged(1);
                }
                if (state == 1) {
                    VivoDisplayManagerServiceImpl.this.mMultiDisplayManagerInternal.onWakefulnessChanged(0);
                }
            }
        });
    }

    public void BinderService_setOverrideDisplayState(int displayId, int overrideState) {
        VSlog.d("shuangping0705", " setOverrideDisplayState : displayId = " + displayId + " ; overrideState = " + overrideState + " ; mOverridePrimaryState = " + this.mOverridePrimaryState + " ; mOverrideSecondState = " + this.mOverrideSecondState);
        boolean stateChanged = false;
        synchronized (this.mStateLock) {
            synchronized (this.mDisplayManagerService.mSyncRoot) {
                if (displayId == 0 && this.mOverridePrimaryState != overrideState) {
                    if (this.mOverridePrimaryState == 2 && (overrideState == 3 || overrideState == 4)) {
                        return;
                    }
                    stateChanged = true;
                    this.mOverridePrimaryState = overrideState;
                }
                if (displayId == 4096 && this.mOverrideSecondState != overrideState) {
                    if (this.mOverrideSecondState == 2 && (overrideState == 3 || overrideState == 4)) {
                        return;
                    }
                    stateChanged = true;
                    this.mOverrideSecondState = overrideState;
                }
                if (stateChanged) {
                    try {
                        Thread.sleep(1L);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    requestGlobalDisplayStateInternal();
                }
            }
        }
    }

    public void BinderService_setOverrideDisplayBrightness(int displayId, int brightness) {
        if (Binder.getCallingUid() != 1000) {
            VSlog.e("DisplayManagerService", "Only system user is allowed to call setOverrideDisplayBrightness. : CallingUid = " + Binder.getCallingUid() + ", CallingPid = " + Binder.getCallingPid());
            return;
        }
        boolean changed = false;
        synchronized (this.mDisplayManagerService.mSyncRoot) {
            if (displayId == 0) {
                try {
                    if (this.mOverridePrimaryBrightness != brightness) {
                        changed = true;
                        this.mOverridePrimaryBrightness = brightness;
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            if (displayId == 4096 && this.mOverrideSecondBrightness != brightness) {
                changed = true;
                this.mOverrideSecondBrightness = brightness;
            }
        }
        if (changed) {
            requestGlobalDisplayStateInternal();
        }
    }

    public void BinderService_setForceDisplayStateOn(int displayId, boolean on, String reason) {
        VSlog.d("shuangping0914", "displayId = " + displayId + "; on = " + on + "; reson = " + reason + "; " + Display.stateToString(this.mOverridePrimaryState));
        DisplayDevice device = null;
        if (this.mWakeLock != null) {
            this.mHandler.removeCallbacks(this.mSetOverrideWrapRunnable);
            this.mHandler.post(this.mSetOverrideWrapRunnable);
        }
        synchronized (this.mDisplayManagerService.mSyncRoot) {
            int count = this.mDisplayManagerService.mLogicalDisplays.size();
            for (int i = 0; i < count; i++) {
                int id = this.mDisplayManagerService.mLogicalDisplays.keyAt(i);
                if (id == displayId) {
                    device = ((LogicalDisplay) this.mDisplayManagerService.mLogicalDisplays.get(displayId)).getPrimaryDisplayDeviceLocked();
                }
            }
            if (device == null) {
                VSlog.d("shuangping0930", "The diplayId is invalid !");
                return;
            }
            if (FORCE_STATE_ON_REASON.equals(reason)) {
                setForceNoChangeVDSS(device, on);
            }
            boolean needOn = false;
            if (reason != null) {
                this.mForceStateOnMap.put(reason, Boolean.valueOf(on));
            }
            Iterator<Boolean> it = this.mForceStateOnMap.values().iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                Boolean forceOn = it.next();
                if (forceOn.booleanValue()) {
                    needOn = true;
                    break;
                }
            }
            if (device.mForceStateOn && !needOn) {
                device.mForceStateFromTrueToFalse = true;
            }
            device.mForceStateOn = needOn;
            requestGlobalDisplayStateInternal();
        }
    }

    public void BinderService_setOverrideDisplayStateWrap(int displayId, int displayState, int module) {
        boolean stateChanged = false;
        boolean z = true;
        if ((displayState == 1 || displayState == 4) && this.mWakeLock != null) {
            this.mHandler.removeCallbacks(this.mSetOverrideWrapRunnable);
            this.mHandler.post(this.mSetOverrideWrapRunnable);
        }
        synchronized (this.mStateLock) {
            int mOverrideState = displayId == 0 ? this.mOverridePrimaryState : this.mOverrideSecondState;
            String displayStr = displayId == 0 ? "primary-display" : "secondary-display";
            if (1 == module) {
                if (mOverrideState == 2 && displayState == 1) {
                    z = false;
                }
                stateChanged = z;
            } else if (2 == module) {
                if (mOverrideState != 3 && mOverrideState != 4) {
                    z = false;
                }
                stateChanged = z;
            } else if (3 == module) {
                if (mOverrideState == 2) {
                    z = false;
                }
                stateChanged = z;
            }
            if (stateChanged) {
                BinderService_setOverrideDisplayState(displayId, displayState);
            } else {
                VSlog.w("shuangping1102", displayStr + "(" + module + ") requires " + Display.stateToString(displayState) + " denied/current display state is " + Display.stateToString(mOverrideState));
            }
        }
    }

    public boolean isRefreshRateAdjusterSupported() {
        return RmsInjectorImpl.getInstance().isRefreshRateAdjusterSupported();
    }

    public void setForceNoChangeVDSS(DisplayDevice device, boolean on) {
        if (device != null) {
            device.mForceOnNoChangeVDSS = on;
        }
    }

    public boolean BinderService_doDump(PrintWriter pw, String[] args) {
        if (args != null && args.length > 0) {
            String type = args[0];
            if ("0".equals(type)) {
                this.mOverridePrimaryState = 2;
                this.mOverrideSecondState = 1;
                requestGlobalDisplayStateInternal();
                return true;
            } else if ("1".equals(type)) {
                this.mOverridePrimaryState = 1;
                this.mOverrideSecondState = 2;
                requestGlobalDisplayStateInternal();
                return true;
            } else if ("2".equals(type)) {
                this.mOverridePrimaryState = 2;
                this.mOverrideSecondState = 2;
                requestGlobalDisplayStateInternal();
                return true;
            }
        }
        return false;
    }

    public boolean hasVirtualDisplay(String packageName, VirtualDisplayAdapter virtualDisplayAdapter) {
        long token = Binder.clearCallingIdentity();
        try {
            return hasVirtualDisplayInternal(packageName, virtualDisplayAdapter);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public boolean hasVirtualDisplayInternal(String packageName, VirtualDisplayAdapter virtualDisplayAdapter) {
        synchronized (this.mDisplayManagerService.mSyncRoot) {
            if (virtualDisplayAdapter == null) {
                return false;
            }
            return virtualDisplayAdapter.hasVirtualDisplayLocked(packageName);
        }
    }

    public int modifyAssignDisplayId(boolean isDefault) {
        int id;
        if (isDefault) {
            id = 0;
        } else {
            DisplayManagerService displayManagerService = this.mDisplayManagerService;
            int i = displayManagerService.mNextNonDefaultDisplayId;
            displayManagerService.mNextNonDefaultDisplayId = i + 1;
            id = i;
        }
        if (id == 4096 || MultiDisplayManager.isAppShareDisplayId(id)) {
            DisplayManagerService displayManagerService2 = this.mDisplayManagerService;
            int i2 = displayManagerService2.mNextNonDefaultDisplayId;
            displayManagerService2.mNextNonDefaultDisplayId = i2 + 1;
            return i2;
        }
        return id;
    }

    public void initGlobalDisplayBrightness() {
        this.mDisplayManagerService.mGlobalDisplayBrightness = BootBrightnessConfig.mInitialBrightness;
        this.mTempBrightness = BootBrightnessConfig.mInitialBrightness;
    }

    public void setOverrideState(int state) {
        if (this.mDisplayManagerService.mGlobalDisplayState != state) {
            DisplayManager mDisplayManager = (DisplayManager) this.mDisplayManagerService.getContext().getSystemService("display");
            if (1 == state) {
                mDisplayManager.setOverrideDisplayState(0, 1);
            }
            if (2 == state) {
                mDisplayManager.setOverrideDisplayState(0, 2);
            }
        }
    }

    public boolean isDozeOrSuspendMode(int displayId) {
        int i;
        if (displayId == 0 && ((i = this.mOverridePrimaryState) == 3 || i == 4)) {
            return true;
        }
        if (displayId == 4096) {
            int i2 = this.mOverrideSecondState;
            return i2 == 3 || i2 == 4;
        }
        return false;
    }

    public void setDisplayLayerStackLocked(DisplayDevice device, DisplayDeviceInfo info) {
        LogicalDisplay display = this.mDisplayManagerService.findLogicalDisplayForDeviceLocked(device);
        if (display != null) {
            setDisplayLayerStackLockedInternal(device.getDisplayTokenLocked(), info.state == 1 ? -1 : display.mLayerStack, device);
        }
    }

    private void setDisplayLayerStackLockedInternal(IBinder displayToken, int layerStack, DisplayDevice device) {
        if (device.mCurrentLayerStack != layerStack) {
            VSlog.d(TAG, "setDisplayLayerStack for mTempDisplayInfo not changed ! layerStack = " + layerStack);
            device.mCurrentLayerStack = layerStack;
            SurfaceControl.openTransaction();
            SurfaceControl.setDisplayLayerStack(displayToken, layerStack);
            SurfaceControl.closeTransaction();
        }
    }

    private void initFingerprintDisplayState(int state, float brightness) {
        FingerprintUIManagerInternal fingerprintUIManager = (FingerprintUIManagerInternal) LocalServices.getService(FingerprintUIManagerInternal.class);
        if (fingerprintUIManager != null) {
            fingerprintUIManager.onDisplayStateChangeFinished(state, brightness);
        } else {
            VSlog.w(TAG, "initFingerprintDisplayState failed");
        }
    }

    public void configureDisplayLocked(DisplayDevice device, LogicalDisplay display, SurfaceControl.Transaction transaction, int state) {
        if (this.mFpRequestDraw) {
            if (state != 1) {
                this.mFpRequestDraw = false;
            }
            display.configureDisplayLocked(transaction, device, false);
            return;
        }
        display.configureDisplayLocked(transaction, device, state == 1);
    }

    public void requestDraw(boolean requestDraw) {
        this.mFpRequestDraw = requestDraw;
        if (!requestDraw) {
            return;
        }
        synchronized (this.mDisplayManagerService.mSyncRoot) {
            this.mDisplayManagerService.scheduleTraversalLocked(false);
        }
    }

    public float getLcmBrightness(float brightness) {
        return brightness;
    }

    public float getBootLcmBrightness() {
        return SensorConfig.getInitialBrightness();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class ColorLock implements IBinder.DeathRecipient {
        public final IBinder mLock;
        public final String mPackageName;
        public String mTag;

        private ColorLock(IBinder lock, String tag, String packageName) {
            this.mLock = lock;
            this.mTag = tag;
            this.mPackageName = packageName;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            VivoDisplayManagerServiceImpl.this.handleColorLockDeath(this);
        }
    }

    public void acquireColorLock(final IBinder lock, final String tag, final String packageName, final int uid, final int pid) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.display.-$$Lambda$VivoDisplayManagerServiceImpl$tZnZVI4lToHjUIJ7JGzKyJYjO0s
            @Override // java.lang.Runnable
            public final void run() {
                VivoDisplayManagerServiceImpl.this.lambda$acquireColorLock$0$VivoDisplayManagerServiceImpl(lock, tag, uid, pid, packageName);
            }
        });
    }

    public /* synthetic */ void lambda$acquireColorLock$0$VivoDisplayManagerServiceImpl(IBinder lock, String tag, int uid, int pid, String packageName) {
        VSlog.d(TAG, "acquireColorLockInternal: lock = " + Objects.hashCode(lock) + ", tag= " + tag + ", uid = " + uid + ", pid = " + pid);
        int index = findColorLockIndexLocked(lock);
        if (index < 0) {
            ColorLock colorLock = new ColorLock(lock, tag, packageName);
            try {
                lock.linkToDeath(colorLock, 0);
            } catch (RemoteException ex) {
                VSlog.d(TAG, "acquireColorLock cause exception: " + ex.fillInStackTrace());
            }
            this.mColorLocks.add(colorLock);
        } else {
            VSlog.d(TAG, "acquireColorLockInternal: lock = " + Objects.hashCode(lock) + " already here");
        }
        updataColorLock();
    }

    public void releaseColorLock(final IBinder lock, final String tag, final String packageName) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.display.-$$Lambda$VivoDisplayManagerServiceImpl$M9v3uskCBmrInrTkUPjONnqB6Jk
            @Override // java.lang.Runnable
            public final void run() {
                VivoDisplayManagerServiceImpl.this.lambda$releaseColorLock$1$VivoDisplayManagerServiceImpl(lock, tag, packageName);
            }
        });
    }

    public /* synthetic */ void lambda$releaseColorLock$1$VivoDisplayManagerServiceImpl(IBinder lock, String tag, String packageName) {
        int index = findColorLockIndexLocked(lock);
        if (index < 0) {
            VSlog.d(TAG, "releaseColorLockInternal: lock = " + Objects.hashCode(lock) + " tag = " + tag + ", packageName = " + packageName + " [not found] !");
            return;
        }
        ColorLock colorLock = this.mColorLocks.get(index);
        VSlog.d(TAG, "releaseColorLockInternal: lock= " + Objects.hashCode(lock) + " [" + colorLock.mTag + "], packageName = " + packageName);
        try {
            colorLock.mLock.unlinkToDeath(colorLock, 0);
        } catch (NoSuchElementException ex) {
            VSlog.d(TAG, "releaseColorLock cause exception lock= " + Objects.hashCode(lock) + " [" + colorLock.mTag + "], packageName = " + packageName + ", exception = " + ex);
        }
        this.mColorLocks.remove(index);
        updataColorLock();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleColorLockDeath(final ColorLock colorLock) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.display.-$$Lambda$VivoDisplayManagerServiceImpl$J7l6Bm0T5fr12fzGXXTZVpIK7Tg
            @Override // java.lang.Runnable
            public final void run() {
                VivoDisplayManagerServiceImpl.this.lambda$handleColorLockDeath$2$VivoDisplayManagerServiceImpl(colorLock);
            }
        });
    }

    public /* synthetic */ void lambda$handleColorLockDeath$2$VivoDisplayManagerServiceImpl(ColorLock colorLock) {
        VSlog.d(TAG, "handleColorLockDeath: lock= " + Objects.hashCode(colorLock.mLock) + " [" + colorLock.mTag + "] ");
        int index = this.mColorLocks.indexOf(colorLock);
        if (index < 0) {
            return;
        }
        colorLock.mLock.unlinkToDeath(colorLock, 0);
        this.mColorLocks.remove(index);
        updataColorLock();
    }

    private int findColorLockIndexLocked(IBinder lock) {
        int count = this.mColorLocks.size();
        for (int i = 0; i < count; i++) {
            if (this.mColorLocks.get(i).mLock == lock) {
                return i;
            }
        }
        return -1;
    }

    private void updataColorLock() {
        boolean shouldSetColorMode;
        if (!this.mColorLocks.isEmpty()) {
            VSlog.d(TAG, "shouldSetColorMode : true");
            for (int i = 0; i < this.mColorLocks.size(); i++) {
                ColorLock colorLock = this.mColorLocks.get(i);
                VSlog.d(TAG, "colorLock NO." + i + ": " + colorLock.mTag + " packagename = " + colorLock.mPackageName);
            }
            shouldSetColorMode = true;
        } else {
            VSlog.d(TAG, "shouldSetColorMode : false");
            shouldSetColorMode = false;
        }
        boolean shouldSetChanged = shouldSetColorMode != this.mShouldSetColorMode;
        if (shouldSetChanged) {
            this.mShouldSetColorMode = shouldSetColorMode;
            if (shouldSetColorMode) {
                setSixZone(1);
            } else {
                setSixZone(0);
            }
        }
    }

    private static void setSixZone(int state) {
        IBinder flinger = ServiceManager.getService(SURFACE_FLINGER);
        if (flinger != null) {
            Parcel data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            data.writeInt(state);
            try {
                try {
                    flinger.transact(SURFACE_FLINGER_TRANSACTION_SET_SIX_ZONE, data, null, 0);
                } catch (Exception ex) {
                    VSlog.e(TAG, "Failed to setSixZone transform" + ex.fillInStackTrace());
                }
            } finally {
                data.recycle();
            }
        }
    }

    public long getPhysicalDeviceId(int displayId) {
        synchronized (this.mDisplayManagerService.mSyncRoot) {
            ArrayList<DisplayDevice> displayDevices = this.mDisplayManagerService.mDisplayDevices;
            Iterator<DisplayDevice> it = displayDevices.iterator();
            while (it.hasNext()) {
                DisplayDevice device = it.next();
                if (device != null && device.sourceDisplayId == displayId) {
                    return device.getPhysicalId();
                }
            }
            return -1L;
        }
    }

    public void handleDisplayDeviceAddedLocked(DisplayDeviceInfo info) {
        if (info != null && this.virtualtype == 0) {
            if (info.type == 3) {
                this.virtualtype = 1;
            }
            if ("com.vivo.upnpserver".equals(info.ownerPackageName) && isEquals(info.name)) {
                this.virtualtype = 2;
            }
            int i = this.virtualtype;
            if (i == 1 || i == 2) {
                VivoCastDisplayUtil.setVirtualMode(true);
                if (this.mIsOverseas) {
                    if (DEBUG_CASTDISPLAY) {
                        VSlog.d(TAG, "handleDisplayDeviceAddedLocked virtualtype:" + this.virtualtype);
                        return;
                    }
                    return;
                }
                this.mRecordStartTime = System.currentTimeMillis();
                this.mRecordAppName = this.mFocusAppName;
                if (DEBUG_CASTDISPLAY) {
                    VSlog.d(TAG, "handleDisplayDeviceAddedLocked virtualtype:" + this.virtualtype + " mRecordAppName:" + this.mRecordAppName + " RecordStartTime:" + this.mRecordStartTime);
                }
            }
        }
    }

    private boolean isEquals(String diaplayDeviceInfoName) {
        VSlog.i(TAG, "isEquals, displayName=" + diaplayDeviceInfoName);
        DisplayManagerService displayManagerService = this.mDisplayManagerService;
        if (displayManagerService == null) {
            return false;
        }
        boolean flag = displayManagerService.isContainsSupportVirtualDisplayName(diaplayDeviceInfoName);
        return flag;
    }

    public void handleDisplayDeviceRemovedLocked(DisplayDeviceInfo info) {
        String str;
        if (info == null) {
            return;
        }
        if ((info.type == 3 && this.virtualtype == 1) || ("com.vivo.upnpserver".equals(info.ownerPackageName) && isEquals(info.name) && this.virtualtype == 2)) {
            long recordEndTime = System.currentTimeMillis();
            if (this.mIsOverseas || (str = this.mFocusAppName) == null) {
                if (DEBUG_CASTDISPLAY) {
                    VSlog.d(TAG, "handleDisplayDeviceRemovedLocked virtualtype:" + this.virtualtype);
                }
                VivoCastDisplayUtil.setVirtualMode(false);
                this.virtualtype = 0;
                return;
            }
            if (this.mRecordAppMap.get(str) == null) {
                int time = ((int) (recordEndTime - this.mRecordStartTime)) / 1000;
                if (time != 0) {
                    this.mRecordAppMap.put(this.mFocusAppName, Integer.valueOf(time));
                }
            } else {
                this.mRecordAppMap.put(this.mFocusAppName, Integer.valueOf((((int) (recordEndTime - this.mRecordStartTime)) / 1000) + this.mRecordAppMap.get(this.mFocusAppName).intValue()));
            }
            if (DEBUG_CASTDISPLAY) {
                VSlog.d(TAG, "handleDisplayDeviceRemovedLocked virtualtype:" + this.virtualtype + " RecordEndTime:" + recordEndTime);
            }
            VivoCastDisplayUtil.setVirtualMode(false);
            Handler handler = this.mHandler;
            if (handler != null) {
                handler.post(new Runnable() { // from class: com.android.server.display.-$$Lambda$VivoDisplayManagerServiceImpl$QV7K7ioIqM012eAA0p7Z0d1EO7E
                    @Override // java.lang.Runnable
                    public final void run() {
                        VivoDisplayManagerServiceImpl.this.lambda$handleDisplayDeviceRemovedLocked$3$VivoDisplayManagerServiceImpl();
                    }
                });
            }
        }
    }

    public /* synthetic */ void lambda$handleDisplayDeviceRemovedLocked$3$VivoDisplayManagerServiceImpl() {
        StringBuilder data = new StringBuilder();
        for (int i = this.mRecordAppMap.size() - 1; i >= 0; i--) {
            String temp_data = this.mRecordAppMap.keyAt(i) + '_' + this.mRecordAppMap.valueAt(i) + ',';
            data.append(temp_data);
        }
        this.virtualtype = 0;
        this.mRecordAppMap.clear();
        castDisplayEventProcess(data.toString());
    }

    public void setDefaultDisplayFocusAppName(String packageName) {
        if (this.mIsOverseas || packageName == null) {
            return;
        }
        if (this.mRecordAppName == null) {
            this.mRecordAppName = packageName;
        }
        this.mFocusAppName = packageName;
        int i = this.virtualtype;
        if ((i == 1 || i == 2) && !this.mFocusAppName.equals(this.mRecordAppName)) {
            long appRecordEndTime = System.currentTimeMillis();
            if (this.mRecordAppMap.get(this.mRecordAppName) == null) {
                int time = ((int) (appRecordEndTime - this.mRecordStartTime)) / 1000;
                if (time != 0) {
                    this.mRecordAppMap.put(this.mRecordAppName, Integer.valueOf(time));
                }
            } else {
                this.mRecordAppMap.put(this.mRecordAppName, Integer.valueOf((((int) (appRecordEndTime - this.mRecordStartTime)) / 1000) + this.mRecordAppMap.get(this.mRecordAppName).intValue()));
            }
            if (DEBUG_CASTDISPLAY) {
                VSlog.d(TAG, "setDefaultDisplayFocusAppName mRecordAppName:" + this.mRecordAppName + " mFocusAppName:" + this.mFocusAppName);
            }
            this.mRecordStartTime = appRecordEndTime;
            this.mRecordAppName = this.mFocusAppName;
        }
    }

    private void castDisplayEventProcess(String data) {
        try {
            VivoCollectData eventInstance = VivoCollectData.getInstance(this.mDisplayManagerService.getContext());
            isEventEnable = eventInstance.getControlInfo("F351");
            if (DEBUG_CASTDISPLAY) {
                VSlog.i(TAG, "castDisplayEventProcess isEventEnable: " + isEventEnable);
            }
            if (isEventEnable) {
                HashMap<String, String> params = new HashMap<>();
                params.put(VCD_FF_1.UUID_STR, UUID.randomUUID().toString());
                params.put("pkg_info", data);
                EventTransfer.getInstance().singleEvent("F351", "F351|10001", System.currentTimeMillis(), 0L, params);
                if (DEBUG_CASTDISPLAY) {
                    VSlog.d(TAG, "castDisplayEventProcess data: " + data);
                }
            }
        } catch (Exception exception) {
            VSlog.e(TAG, "castDisplayEventProcess Error e = " + exception);
            exception.printStackTrace();
        }
    }

    public void deliverDisplayEvent(int displayId, int event) {
        if (MultiDisplayManager.SUPPORT_CAR_NETWORKING && MultiDisplayManager.isVivoDisplay(displayId)) {
            if (event == 1) {
                MultiDisplayManager.markRunningDisplay(displayId, true);
            } else if (event == 3) {
                MultiDisplayManager.markRunningDisplay(displayId, false);
            }
            if (displayId == 90000) {
                if (event == 1) {
                    SystemProperties.set("sys.vivo.mirroring", "1");
                    SystemProperties.set("sys.vivo.bg.mirroring", "0");
                } else if (event == 3) {
                    SystemProperties.set("sys.vivo.mirroring", "0");
                    SystemProperties.set("sys.vivo.bg.mirroring", "0");
                }
            }
            if (MultiDisplayManager.DEBUG) {
                VSlog.d("VivoDisplay", "Delivering display " + displayId + "  ,running=" + MultiDisplayManager.isVirtualDisplayRunning(displayId) + " ,prop = " + SystemProperties.get("sys.vivo.mirroring", "-1") + " ,with event: " + event);
            }
        }
    }

    public int adjustDisplayIdForVirtualDisplay(int displayId, int callingUid, int callingPid) {
        if (MultiDisplayManager.isVCarDisplayRunning()) {
            if (!InputMethodManagerInternal.get().isUsedByCarNetworking(callingUid)) {
                return displayId;
            }
            return SceneManager.APP_REQUEST_PRIORITY;
        }
        int adjustDisplayId = adjustDisplayIdForAppLocked(this.mDisplayManagerService.mContext, displayId, callingUid, callingPid);
        return adjustDisplayId;
    }

    public int assignDisplayIdForVirtualDisplay(DisplayDevice device, boolean isDefault) {
        DisplayDeviceInfo deviceInfo = device.getDisplayDeviceInfoLocked();
        if (shouldAssignAppShareDisplayId(deviceInfo)) {
            return 10086;
        }
        if (!MultiDisplayManager.SUPPORT_CAR_NETWORKING) {
            return modifyAssignDisplayId(isDefault);
        }
        int displayId = -1;
        if (("com.vivo.car.networking".equals(deviceInfo.ownerPackageName) || "com.vivo.carlauncher".equals(deviceInfo.ownerPackageName)) && deviceInfo.name != null && deviceInfo.name.contains("vivo_car_screen")) {
            displayId = "vivo_car_screen".equals(deviceInfo.name) ? SceneManager.APP_REQUEST_PRIORITY : adjustSecondDisplayForCar(deviceInfo.name);
        } else if (("com.vivo.abe".equals(deviceInfo.ownerPackageName) || "com.vivo.sps".equals(deviceInfo.ownerPackageName)) && "vivo_rms_screen".equals(deviceInfo.name)) {
            displayId = 95555;
        } else if ("Vivo-Miracast".equals(deviceInfo.ownerPackageName) || "com.vivo.upnpserver".equals(deviceInfo.ownerPackageName)) {
            displayId = SceneManager.POWER_PRIORITY;
        } else if (deviceInfo.name != null && deviceInfo.name.startsWith("Carlife_")) {
            displayId = adjustPresentDisplayForCar();
        } else if (MultiDisplayManager.SUPPORT_BG_GAME && VivoPermissionUtils.OS_PKG.equals(deviceInfo.ownerPackageName) && "VivoGameMode".equals(deviceInfo.name) && !MultiDisplayManager.isGameDisplayRunning()) {
            displayId = 85000;
        }
        if (displayId == -1 || MultiDisplayManager.isVirtualDisplayRunning(displayId)) {
            displayId = modifyAssignDisplayId(isDefault);
            if (MultiDisplayManager.isVivoDisplay(displayId)) {
                DisplayManagerService displayManagerService = this.mDisplayManagerService;
                int i = displayManagerService.mNextNonDefaultDisplayId;
                displayManagerService.mNextNonDefaultDisplayId = i + 1;
                displayId = i;
            }
        }
        if (MultiDisplayManager.DEBUG) {
            VSlog.d("VivoDisplay", "Config " + deviceInfo.ownerPackageName + " of " + deviceInfo.name + " with display " + displayId);
        }
        return displayId;
    }

    private int adjustSecondDisplayForCar(String deviceName) {
        if (deviceName != null && deviceName.startsWith("vivo_car_screen_")) {
            try {
                String subStr = deviceName.substring("vivo_car_screen_".length());
                int id = Integer.parseInt(subStr);
                return SceneManager.APP_REQUEST_PRIORITY + id;
            } catch (NumberFormatException e) {
                VSlog.e(TAG, "adjustSecondDisplay Error e = " + e);
                return -1;
            }
        }
        return -1;
    }

    private int adjustPresentDisplayForCar() {
        for (int id = 81000; id <= 81005; id++) {
            synchronized (this.mDisplayManagerService.mSyncRoot) {
                if (this.mDisplayManagerService.mLogicalDisplays.get(id) == null) {
                    return id;
                }
            }
        }
        return -1;
    }

    public void resizeVirtualDisplayWithId(VirtualDisplayAdapter mVirtualDisplayAdapter, SparseArray<LogicalDisplay> mLogicalDisplays, ArrayList<DisplayDevice> mDisplayDevices, int displayId, IBinder appToken, int width, int height, int densityDpi) {
        long token = Binder.clearCallingIdentity();
        try {
            resizeVirtualDisplayInternal(mVirtualDisplayAdapter, mLogicalDisplays, mDisplayDevices, displayId, appToken, width, height, densityDpi);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void resizeVirtualDisplayInternal(VirtualDisplayAdapter mVirtualDisplayAdapter, SparseArray<LogicalDisplay> mLogicalDisplays, ArrayList<DisplayDevice> mDisplayDevices, int displayId, IBinder appToken, int width, int height, int densityDpi) {
        synchronized (this.mDisplayManagerService.mSyncRoot) {
            if (mVirtualDisplayAdapter == null) {
                return;
            }
            if (!mVirtualDisplayAdapter.needResizeLocked(appToken, width, height, densityDpi)) {
                VSlog.d(TAG_APPSHARED, "virtual display param is not changed. then do nothing. width : " + width + ", height : " + height);
                mVirtualDisplayAdapter.resizeDisplaySizeOny(appToken, width, height, densityDpi);
                return;
            }
            VSlog.d(TAG_APPSHARED, "resizeVirtualDisplayInternal displayId : " + displayId + ", width : " + width + ", height : " + height + ", token : " + appToken);
            mVirtualDisplayAdapter.resizeVirtualDisplayLocked(appToken, width, height, densityDpi);
            LogicalDisplay display = mLogicalDisplays.get(displayId);
            if (display != null) {
                display.updateLocked(mDisplayDevices);
                if (this.mVivoAppShareManager != null) {
                    this.mVivoAppShareManager.updateDisplayOrientation(displayId);
                }
            }
        }
    }

    public void setVirtualDisplayId(VirtualDisplayAdapter mVirtualDisplayAdapter, DisplayDevice devices, int displayId) {
        if (mVirtualDisplayAdapter == null) {
            return;
        }
        mVirtualDisplayAdapter.setVirtualDisplayId(devices, displayId);
    }

    public void resizeVirtualDisplayNoDenty(VirtualDisplayAdapter mVirtualDisplayAdapter, SparseArray<LogicalDisplay> mLogicalDisplays, ArrayList<DisplayDevice> mDisplayDevices, int displayId, int width, int height) {
        synchronized (this.mDisplayManagerService.mSyncRoot) {
            if (mVirtualDisplayAdapter == null) {
                return;
            }
            if (!mVirtualDisplayAdapter.needResizeLocked(displayId, width, height)) {
                VSlog.d(TAG, "virtual display param is not changed. then do nothing. width : " + width + ", height : " + height);
                return;
            }
            VSlog.d(TAG, "resizeVirtualDisplayNoDenty displayId : " + displayId + ", width : " + width + ", height : " + height);
            mVirtualDisplayAdapter.resizeVirtualDisplayLocked(displayId, width, height);
            LogicalDisplay display = mLogicalDisplays.get(displayId);
            if (display != null) {
                display.updateLocked(mDisplayDevices);
            }
        }
    }

    public int adjustDisplayIdForAppLocked(Context context, int oriDisplayId, int callingUid, int callingPid) {
        String[] pkgNames;
        if (!isAppShareDisplayExistLocked()) {
            return oriDisplayId;
        }
        if (this.mAppSharePackageName == null || this.mAppShareUserId == -1) {
            return oriDisplayId;
        }
        int userId = UserHandle.getUserId(callingUid);
        String pkgName = null;
        if (this.mPackageManager == null && this.mDisplayManagerService.getContext() != null) {
            this.mPackageManager = this.mDisplayManagerService.getContext().getPackageManager();
        }
        PackageManager packageManager = this.mPackageManager;
        if (packageManager != null && callingUid != 1000 && (pkgNames = packageManager.getPackagesForUid(callingUid)) != null && pkgNames.length > 0) {
            pkgName = pkgNames[0];
        }
        if (DEBUG_APP_SHARE) {
            VSlog.d(TAG, "adjustDisplayIdForAppLocked: callingUid = " + callingUid + ", callingPid = " + callingPid + ", userId = " + userId + ", pkgName = " + pkgName + ", oriDisplayId = " + oriDisplayId);
        }
        int candidateDisplayId = this.mVivoRatioControllerUtils.adjustDisplayIdForIME(context, (this.mAppSharePackageName.equals(pkgName) && this.mAppShareUserId == userId) ? 10086 : oriDisplayId, callingPid, callingUid);
        int imeEventDisplay = this.mVivoAppShareManager.getInputMethodShowEventDisplayId();
        if (imeEventDisplay >= 0 && this.mVivoRatioControllerUtils.isRunningInputMethodAll(callingPid)) {
            candidateDisplayId = imeEventDisplay;
        }
        if (DEBUG_APP_SHARE) {
            VSlog.d(TAG, "adjustDisplayIdForAppLocked: adjust displayId from : " + oriDisplayId + " to " + candidateDisplayId + ", pkgName: " + pkgName);
        }
        return candidateDisplayId;
    }

    public boolean createAppShareDisplayRepeatedly(String packageName, String displayName) {
        return AppShareConfig.APP_SHARE_PKG_NAME.equals(packageName) && isAppShareDisplayExistLocked() && "share_display_vivo_screen".equals(displayName);
    }

    public boolean shouldAssignAppShareDisplayId(DisplayDeviceInfo deviceInfo) {
        return AppShareConfig.SUPPROT_APPSHARE && AppShareConfig.APP_SHARE_PKG_NAME.equals(deviceInfo.ownerPackageName) && "share_display_vivo_screen".equals(deviceInfo.name);
    }

    public void notifyAppSharePackageChanged(String packageName, int userId) {
        if (!AppShareConfig.SUPPROT_APPSHARE) {
            return;
        }
        synchronized (this.mDisplayManagerService.mSyncRoot) {
            this.mAppSharePackageName = packageName;
            this.mAppShareUserId = userId;
            if (packageName != null && userId != -1) {
                updateAppShareState(true);
            } else if (this.mAppSharePackageName == null && this.mAppShareUserId == -1) {
                updateAppShareState(false);
            }
        }
    }

    public void notifyAppShareActivityStateChanged(boolean resume) {
        VSlog.i(TAG, "notifyAppShareActivityStateChanged: resume = " + resume);
        updateSharingAppState(resume);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateDisplayStateForAppShare() {
        this.mHandler.removeCallbacks(this.mUpdateDisplayStateRunnable);
        this.mHandler.post(this.mUpdateDisplayStateRunnable);
    }

    private void updateAppShareState(boolean isAppSharing) {
        this.mHandler.post(new UpdateAppShareState(isAppSharing));
    }

    private void updateSharingAppState(boolean resume) {
        this.mHandler.post(new UpdateSharingAppState(resume));
    }

    /* loaded from: classes.dex */
    private final class AppShareDisplayListener implements DisplayManager.DisplayListener {
        private AppShareDisplayListener() {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayAdded(int displayId) {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayChanged(int displayId) {
            Display display;
            int state;
            if (VivoDisplayManagerServiceImpl.DEBUG_APP_SHARE) {
                VSlog.i(VivoDisplayManagerServiceImpl.TAG, "onDisplayChanged: displayId = " + displayId + ", mIsAppSharing = " + VivoDisplayManagerServiceImpl.this.mIsAppSharing);
            }
            if (displayId == 0 && (display = VivoDisplayManagerServiceImpl.this.mDisplayManager.getDisplay(displayId)) != null && VivoDisplayManagerServiceImpl.this.mState != (state = display.getState())) {
                if (VivoDisplayManagerServiceImpl.DEBUG_APP_SHARE) {
                    VSlog.i(VivoDisplayManagerServiceImpl.TAG, "onDisplayChanged: from " + Display.stateToString(VivoDisplayManagerServiceImpl.this.mState) + " to " + Display.stateToString(state));
                }
                VivoDisplayManagerServiceImpl.this.mState = state;
                if (VivoDisplayManagerServiceImpl.this.mIsAppSharing) {
                    VivoDisplayManagerServiceImpl.this.updateDisplayStateForAppShare();
                }
            }
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayRemoved(int displayId) {
        }
    }

    public void setForceDisplayStateOn(boolean forceOn, String reason, boolean isOn) {
        VSlog.i(TAG, "setForceDisplayStateOn: forceOn = " + forceOn + ", mForceOn = " + this.mForceOn + ", reason = " + reason + ", isOn = " + isOn);
        if (this.mForceOn != forceOn || forceOn) {
            this.mForceOn = forceOn;
            if (!isOn) {
                this.mDisplayManager.setForceDisplayStateOn(0, forceOn, reason);
                if (forceOn) {
                    setBrightness(AblConfig.getMapping2048GrayScaleFrom256GrayScale(20));
                }
            }
        }
    }

    private void setBrightness(int brightness) {
        BufferedWriter writer = null;
        try {
            try {
                try {
                    writer = new BufferedWriter(new FileWriter("/sys/lcm/bl_level"));
                    writer.write(String.valueOf(brightness));
                    writer.flush();
                    VSlog.i(TAG, "setBrightness by app share: brightness = " + brightness + ", brightness(hex) = " + Integer.toHexString(brightness));
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    VSlog.e(TAG, "setBrightness: " + e);
                    if (writer != null) {
                        writer.close();
                    }
                }
            } catch (Throwable th) {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                }
                throw th;
            }
        } catch (IOException e3) {
            e3.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class UpdateAppShareState implements Runnable {
        private final boolean isAppSharing;

        public UpdateAppShareState(boolean isAppSharing) {
            this.isAppSharing = isAppSharing;
        }

        @Override // java.lang.Runnable
        public void run() {
            VivoDisplayManagerServiceImpl.this.mIsAppSharing = this.isAppSharing;
            VivoDisplayManagerServiceImpl.this.updateDisplayStateForAppShare();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class UpdateSharingAppState implements Runnable {
        private final boolean resume;

        public UpdateSharingAppState(boolean resume) {
            this.resume = resume;
        }

        @Override // java.lang.Runnable
        public void run() {
            VivoDisplayManagerServiceImpl.this.mIsAppResumed = this.resume;
            VivoDisplayManagerServiceImpl.this.updateDisplayStateForAppShare();
        }
    }

    public boolean isImeApplicationAndOnAppShareDisplayInternal() {
        boolean z = false;
        if (AppShareConfig.SUPPROT_APPSHARE) {
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                if (this.mVivoRatioControllerUtils.isImeApplication(this.mDisplayManagerService.getContext(), pid, uid)) {
                    if (MultiDisplayManager.isAppShareDisplayId(this.mVivoRatioControllerUtils.getCurrentInputMethodDisplayId())) {
                        z = true;
                    }
                }
                return z;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
        return false;
    }

    private boolean isAppShareDisplayExistLocked() {
        return AppShareConfig.SUPPROT_APPSHARE && this.mDisplayManagerService.mLogicalDisplays.get(10086) != null;
    }

    public void registerCallbackInternal(IDisplayManagerCallback callback, int callingPid, int callingUid) {
        if (this.mDisplayManagerService.mCallbacks.get(callingPid) != null) {
            IBinder.DeathRecipient deathRecipient = (DisplayManagerService.CallbackRecord) this.mDisplayManagerService.mCallbacks.get(callingPid);
            boolean isAlive = ((DisplayManagerService.CallbackRecord) deathRecipient).mCallback.asBinder().isBinderAlive();
            boolean uidChanged = ((DisplayManagerService.CallbackRecord) deathRecipient).mUid != callingUid;
            if (isAlive && !uidChanged) {
                throw new SecurityException("The calling process has already registered an IDisplayManagerCallback.");
            }
            ((DisplayManagerService.CallbackRecord) deathRecipient).mCallback.asBinder().unlinkToDeath(deathRecipient, 0);
            this.mDisplayManagerService.onCallbackDied(deathRecipient);
            VSlog.d(TAG, "registerCallback failed, pid = " + callingPid + ", newUid = " + callingUid + ", oldUid = " + ((DisplayManagerService.CallbackRecord) deathRecipient).mUid + ", isAlive = " + isAlive);
        }
        DisplayManagerService displayManagerService = this.mDisplayManagerService;
        Objects.requireNonNull(displayManagerService);
        IBinder.DeathRecipient callbackRecord = new DisplayManagerService.CallbackRecord(displayManagerService, callingPid, callingUid, callback);
        try {
            IBinder binder = callback.asBinder();
            binder.linkToDeath(callbackRecord, 0);
            this.mDisplayManagerService.mCallbacks.put(callingPid, callbackRecord);
        } catch (RemoteException ex) {
            throw new RuntimeException(ex);
        }
    }
}