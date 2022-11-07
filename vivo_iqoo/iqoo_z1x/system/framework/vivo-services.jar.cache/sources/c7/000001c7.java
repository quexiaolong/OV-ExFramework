package com.android.server.display;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.hardware.display.IVivoDisplayStateService;
import android.hardware.display.IVivoDisplayStateServiceCallback;
import android.hardware.display.VivoDisplayModule;
import android.hardware.display.VivoDisplayStateInternal;
import android.multidisplay.MultiDisplayManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.SparseArray;
import android.view.Display;
import com.android.server.SystemService;
import com.vivo.services.rms.ProcessList;
import com.vivo.services.security.server.VivoPermissionUtils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoDisplayStateService extends SystemService {
    private static boolean DEBUG = SystemProperties.get("persist.sys.log.ctrl", "no").equals("yes");
    private static final String HLPM_MODE_NODE_VALUE_2NIT = "0";
    private static final String HLPM_MODE_NODE_VALUE_50NIT = "1";
    private static final int INVALID_MODULE_ID = -1;
    private static final String KEY_VIVO_LOG_CTRL = "persist.sys.log.ctrl";
    private static final String MODULE_NAME_CAMERA = "VivoCamera";
    private static final String MODULE_NAME_LIGHT = "light";
    private static final String PRIMARY_HLPM_MODE_NODE = "/sys/lcm/oled_hlpmmode";
    private static final String SECONDARY_HLPM_MODE_NODE = "/sys/lcm/oled_hlpmmode1";
    private static final String TAG = "VivoDisplayStateService";
    private boolean mAlwaysOnEnabled;
    private final SparseArray<CallbackRecord> mCallbacks;
    private Context mContext;
    private final SparseArray<DisplayController> mDisplayControllers;
    private DisplayManager mGlobalDisplayManager;
    private Handler mHandler;
    private final SparseArray<ModuleMonitor> mModuleMonitors;
    private int mPrimaryDisplayBacklight;
    private int mPrimaryDisplayPendingBacklight;
    private int mPrimaryDisplayPendingState;
    private int mPrimaryDisplayState;
    private int mSecondaryDisplayBacklight;
    private int mSecondaryDisplayPendingBacklight;
    private int mSecondaryDisplayPendingState;
    private int mSecondaryDisplayState;
    private SettingsObserver mSettingsObserver;
    private boolean mSupportMultiDisplay;
    private boolean mSuppressed;
    private boolean mSystemReady;
    private final ArrayList<CallbackRecord> mTempCallbacks;
    private HandlerThread mThread;

    public VivoDisplayStateService(Context context) {
        super(context);
        this.mCallbacks = new SparseArray<>();
        this.mTempCallbacks = new ArrayList<>();
        this.mModuleMonitors = new SparseArray<>();
        this.mDisplayControllers = new SparseArray<>();
        this.mContext = context;
        HandlerThread handlerThread = new HandlerThread(TAG);
        this.mThread = handlerThread;
        handlerThread.start();
        this.mHandler = new Handler(this.mThread.getLooper());
    }

    public void onStart() {
        publishBinderService("vivo_display_state", new VivoDisplayStateServiceWrapper());
        publishLocalService(VivoDisplayStateInternal.class, new LocalService());
        this.mSystemReady = false;
        this.mSupportMultiDisplay = false;
        this.mPrimaryDisplayState = 0;
        this.mPrimaryDisplayBacklight = -1;
        this.mSecondaryDisplayState = 0;
        this.mSecondaryDisplayBacklight = -1;
        this.mPrimaryDisplayPendingState = 0;
        this.mPrimaryDisplayPendingBacklight = -1;
        this.mSecondaryDisplayPendingState = 0;
        this.mSecondaryDisplayPendingBacklight = -1;
    }

    public void systemReady() {
        synchronized (this) {
            if (DEBUG) {
                VSlog.d(TAG, "system is ready: primary-display: " + Display.stateToString(this.mPrimaryDisplayState) + ":" + this.mPrimaryDisplayBacklight + " pending: " + Display.stateToString(this.mPrimaryDisplayPendingState) + ":" + this.mPrimaryDisplayPendingBacklight + " secondary-display: " + Display.stateToString(this.mSecondaryDisplayState) + ":" + this.mSecondaryDisplayBacklight + " pending: " + Display.stateToString(this.mSecondaryDisplayPendingState) + ":" + this.mSecondaryDisplayPendingBacklight);
            }
            if (supportMultiDisplay()) {
                this.mDisplayControllers.put(0, new DisplayController(0, this.mContext, this.mPrimaryDisplayState, this.mPrimaryDisplayBacklight, this.mPrimaryDisplayPendingState, this.mPrimaryDisplayPendingBacklight));
                this.mDisplayControllers.put(4096, new DisplayController(4096, this.mContext, this.mSecondaryDisplayState, this.mSecondaryDisplayBacklight, this.mSecondaryDisplayPendingState, this.mSecondaryDisplayPendingBacklight));
            } else {
                this.mDisplayControllers.put(0, new DisplayController(0, this.mContext, this.mPrimaryDisplayState, this.mPrimaryDisplayBacklight, this.mPrimaryDisplayPendingState, this.mPrimaryDisplayPendingBacklight));
            }
            this.mSystemReady = true;
        }
        ContentResolver resolver = this.mContext.getContentResolver();
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        resolver.registerContentObserver(Settings.Secure.getUriFor("doze_always_on"), false, this.mSettingsObserver, -1);
    }

    private boolean supportMultiDisplay() {
        if (this.mSystemReady) {
            return this.mSupportMultiDisplay;
        }
        if (this.mGlobalDisplayManager == null) {
            this.mGlobalDisplayManager = (DisplayManager) this.mContext.getSystemService("display");
        }
        DisplayManager displayManager = this.mGlobalDisplayManager;
        if (displayManager != null) {
            this.mSupportMultiDisplay = displayManager.getDisplay(4096) != null;
        }
        if (DEBUG) {
            VSlog.d(TAG, "supportMultiDisplay: " + this.mSupportMultiDisplay);
        }
        return this.mSupportMultiDisplay;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            VivoDisplayStateService.this.handleSettingsChanged();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleSettingsChanged() {
        this.mAlwaysOnEnabled = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "doze_always_on", 0, -2) != 0;
    }

    /* loaded from: classes.dex */
    private final class LocalService extends VivoDisplayStateInternal {
        private LocalService() {
        }

        public void suppressAmbientDisplay(final boolean isSuppressed) {
            VivoDisplayStateService.this.mHandler.post(new Runnable() { // from class: com.android.server.display.VivoDisplayStateService.LocalService.1
                @Override // java.lang.Runnable
                public void run() {
                    VivoDisplayStateService.this.suppressAmbientDisplayInternal(isSuppressed);
                }
            });
        }

        public void onBacklightStateChanged(final int displayId, final int state, final int brightness) {
            VivoDisplayStateService.this.mHandler.post(new Runnable() { // from class: com.android.server.display.VivoDisplayStateService.LocalService.2
                @Override // java.lang.Runnable
                public void run() {
                    VivoDisplayStateService.this.onBacklightStateChangedInternal(displayId, state, brightness);
                }
            });
        }

        public void requestOverlayDismiss(final int displayId, final int moduleId) {
            VivoDisplayStateService.this.mHandler.post(new Runnable() { // from class: com.android.server.display.VivoDisplayStateService.LocalService.3
                @Override // java.lang.Runnable
                public void run() {
                    VivoDisplayStateService.this.requestOverlayDismissInternal(displayId, moduleId);
                }
            });
        }

        public void onBacklightStateBeginChange(final int displayId, final int state, final int brightness) {
            VivoDisplayStateService.this.mHandler.post(new Runnable() { // from class: com.android.server.display.VivoDisplayStateService.LocalService.4
                @Override // java.lang.Runnable
                public void run() {
                    VivoDisplayStateService.this.onBacklightStateBeginChangeInternal(displayId, state, brightness);
                }
            });
        }

        public void onForceNoChangeVDSSChange(final int displayId, final boolean forceOnNoChangeVDSS) {
            VivoDisplayStateService.this.mHandler.post(new Runnable() { // from class: com.android.server.display.VivoDisplayStateService.LocalService.5
                @Override // java.lang.Runnable
                public void run() {
                    VivoDisplayStateService.this.onForceNoChangeVDSSChangeInternal(displayId, forceOnNoChangeVDSS);
                }
            });
        }

        public void requestDisplayState(final int displayId, final int moduleId, final int state, final int brightness) {
            VivoDisplayStateService.this.mHandler.post(new Runnable() { // from class: com.android.server.display.VivoDisplayStateService.LocalService.6
                @Override // java.lang.Runnable
                public void run() {
                    VivoDisplayStateService.this.requestDisplayStateInternal(displayId, moduleId, state, brightness, true);
                }
            });
        }
    }

    /* loaded from: classes.dex */
    private final class VivoDisplayStateServiceWrapper extends IVivoDisplayStateService.Stub {
        private VivoDisplayStateServiceWrapper() {
        }

        public int registerDisplayModule(IBinder token, int displayId, VivoDisplayModule displayModule) {
            return VivoDisplayStateService.this.registerDisplayModuleInternal(token, displayId, displayModule);
        }

        public int registerModule(IBinder token, int displayId, String module) {
            return VivoDisplayStateService.this.registerModuleInternal(token, displayId, module);
        }

        public void notifyContentState(final int displayId, final int moduleId, final boolean visible) {
            VivoDisplayStateService.this.mHandler.post(new Runnable() { // from class: com.android.server.display.VivoDisplayStateService.VivoDisplayStateServiceWrapper.1
                @Override // java.lang.Runnable
                public void run() {
                    VivoDisplayStateService.this.notifyContentStateInternal(displayId, moduleId, visible);
                }
            });
        }

        public void requestDisplayState(final int displayId, final int moduleId, final int state, final int brightness) {
            VivoDisplayStateService.this.mHandler.post(new Runnable() { // from class: com.android.server.display.VivoDisplayStateService.VivoDisplayStateServiceWrapper.2
                @Override // java.lang.Runnable
                public void run() {
                    VivoDisplayStateService.this.requestDisplayStateInternal(displayId, moduleId, state, brightness, false);
                }
            });
        }

        public void registerCallback(final IVivoDisplayStateServiceCallback callback) {
            final int callingPid = Binder.getCallingPid();
            if (callback != null) {
                if (VivoDisplayStateService.this.mCallbacks.get(callingPid) == null) {
                    VivoDisplayStateService.this.mHandler.post(new Runnable() { // from class: com.android.server.display.VivoDisplayStateService.VivoDisplayStateServiceWrapper.3
                        @Override // java.lang.Runnable
                        public void run() {
                            VivoDisplayStateService.this.registerCallbackInternal(callback, callingPid);
                        }
                    });
                    return;
                }
                throw new SecurityException("The calling process has already registered an IVivoDisplayStateServiceCallback.");
            }
            throw new IllegalArgumentException("listener must not be null");
        }

        public VivoDisplayModule getVivoDisplayModuleInfo(int displayId, int moduleId, String moduleStr) {
            return VivoDisplayStateService.this.getVivoDisplayModuleInfoInternal(displayId, moduleId, moduleStr);
        }

        public void requestOverlayState(final int displayId, final int moduleId, final boolean show) {
            VivoDisplayStateService.this.mHandler.post(new Runnable() { // from class: com.android.server.display.VivoDisplayStateService.VivoDisplayStateServiceWrapper.4
                @Override // java.lang.Runnable
                public void run() {
                    VivoDisplayStateService.this.requestOverlayStateInternal(displayId, moduleId, show);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void requestOverlayStateInternal(int displayId, int moduleId, boolean show) {
        if (!supportMultiDisplay() && displayId != 0) {
            VSlog.e(TAG, "invalid displayId " + displayId);
        } else if (supportMultiDisplay() && displayId != 0 && 4096 != displayId) {
            VSlog.e(TAG, "invalid displayId " + displayId);
        } else {
            DisplayController displayController = this.mDisplayControllers.get(displayId);
            if (displayController != null) {
                displayController.requestOverlayState(moduleId, show);
            } else {
                VSlog.w(TAG, "request overlay dismiss denied/invalid display controller");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int registerDisplayModuleInternal(IBinder binderToken, int displayId, VivoDisplayModule displayModule) {
        int moduleId;
        if (!supportMultiDisplay() && displayId != 0) {
            VSlog.e(TAG, "invalid displayId " + displayId);
            return -1;
        } else if (supportMultiDisplay() && displayId != 0 && 4096 != displayId) {
            VSlog.e(TAG, "invalid displayId " + displayId);
            return -1;
        } else {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (this) {
                    DisplayController displayController = this.mDisplayControllers.get(displayId);
                    if (displayController != null) {
                        moduleId = displayController.registerDisplayModule(displayModule);
                    } else {
                        moduleId = -1;
                        VSlog.w(TAG, "register module denied/invalid display controller");
                    }
                    registerModuleMonitor(binderToken, displayId, moduleId, displayModule.getName().toString());
                }
                return moduleId;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int registerModuleInternal(IBinder binderToken, int displayId, String module) {
        int moduleId;
        if (!supportMultiDisplay() && displayId != 0) {
            VSlog.e(TAG, "invalid displayId " + displayId);
            return -1;
        } else if (supportMultiDisplay() && displayId != 0 && 4096 != displayId) {
            VSlog.e(TAG, "invalid displayId " + displayId);
            return -1;
        } else {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (this) {
                    DisplayController displayController = this.mDisplayControllers.get(displayId);
                    if (displayController != null) {
                        moduleId = displayController.registerDisplayModule(module);
                    } else {
                        moduleId = -1;
                        VSlog.w(TAG, "register module denied/invalid display controller");
                    }
                    registerModuleMonitor(binderToken, displayId, moduleId, module);
                }
                return moduleId;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    private void registerModuleMonitor(IBinder token, int displayId, int moduleId, String module) {
        if (!supportMultiDisplay() && displayId != 0) {
            VSlog.e(TAG, "invalid displayId " + displayId);
        } else if (supportMultiDisplay() && displayId != 0 && 4096 != displayId) {
            VSlog.e(TAG, "invalid displayId " + displayId);
        } else if (-1 == moduleId) {
            VSlog.w(TAG, "registerModuleMonitor denied/invalid module id");
        } else if (this.mModuleMonitors.get(moduleId) != null) {
            VSlog.w(TAG, "registerModuleMonitor denied/module monitor is already registered");
        } else {
            ModuleMonitor moduleMonitor = new ModuleMonitor(displayId, moduleId, token, module);
            this.mModuleMonitors.put(moduleId, moduleMonitor);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyContentStateInternal(int displayId, int moduleId, boolean visible) {
        if (!supportMultiDisplay() && displayId != 0) {
            VSlog.e(TAG, "invalid displayId " + displayId);
        } else if (supportMultiDisplay() && displayId != 0 && 4096 != displayId) {
            VSlog.e(TAG, "invalid displayId " + displayId);
        } else {
            DisplayController displayController = this.mDisplayControllers.get(displayId);
            if (displayController != null) {
                displayController.updateDisplayContent(moduleId, visible);
            } else {
                VSlog.w(TAG, "notify content state denied/invalid display controller");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void requestDisplayStateInternal(int displayId, int moduleId, int state, int brightness, boolean internalCall) {
        if (!supportMultiDisplay() && displayId != 0) {
            VSlog.e(TAG, "invalid displayId " + displayId);
        } else if (supportMultiDisplay() && displayId != 0 && 4096 != displayId) {
            VSlog.e(TAG, "invalid displayId " + displayId);
        } else {
            DisplayController displayController = this.mDisplayControllers.get(displayId);
            if (displayController != null) {
                displayController.requestDisplayState(moduleId, state, brightness, !internalCall, internalCall);
            } else {
                VSlog.w(TAG, "request display state denied/invalid display controller");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onForceNoChangeVDSSChangeInternal(int displayId, boolean forceOnNoChangeVDSS) {
        DisplayController displayController = this.mDisplayControllers.get(displayId);
        if (displayController == null) {
            return;
        }
        displayController.onForceNoChangeVDSSChange(forceOnNoChangeVDSS);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void suppressAmbientDisplayInternal(boolean isSuppressed) {
        if (this.mSuppressed != isSuppressed) {
            VSlog.d(TAG, "suppressAmbientDisplay oldSuppressed = " + this.mSuppressed + ", newSuppressed = " + isSuppressed + ", alwaysOnEnabled = " + this.mAlwaysOnEnabled);
            this.mSuppressed = isSuppressed;
            if (this.mAlwaysOnEnabled) {
                updateAmbientDisplayState();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateAmbientDisplayState() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.display.VivoDisplayStateService.1
            @Override // java.lang.Runnable
            public void run() {
                DisplayController displayController = (DisplayController) VivoDisplayStateService.this.mDisplayControllers.get(0);
                if (!VivoDisplayStateService.this.mSuppressed) {
                    displayController.requestDisplayState(-1, 3, 0, false, true);
                } else {
                    displayController.requestDisplayState(-1, 1, 0, false, true);
                }
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onBacklightStateBeginChangeInternal(int displayId, int state, int brightness) {
        if (!supportMultiDisplay() && displayId != 0) {
            VSlog.e(TAG, "invalid displayId " + displayId);
        } else if (supportMultiDisplay() && displayId != 0 && 4096 != displayId) {
            VSlog.e(TAG, "invalid displayId " + displayId);
        } else {
            if (displayId == 0) {
                this.mPrimaryDisplayPendingState = state;
                this.mPrimaryDisplayPendingBacklight = brightness;
            } else if (4096 == displayId) {
                this.mSecondaryDisplayPendingState = state;
                this.mSecondaryDisplayPendingBacklight = brightness;
            }
            DisplayController displayController = this.mDisplayControllers.get(displayId);
            if (displayController != null) {
                displayController.onDisplayStateBeginChange(state, brightness);
            } else {
                VSlog.w(TAG, "backlight state begin change denied/invalid display controller");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onBacklightStateChangedInternal(int displayId, int state, int brightness) {
        if (!supportMultiDisplay() && displayId != 0) {
            VSlog.e(TAG, "invalid displayId " + displayId);
        } else if (supportMultiDisplay() && displayId != 0 && 4096 != displayId) {
            VSlog.e(TAG, "invalid displayId " + displayId);
        } else {
            if (displayId == 0) {
                this.mPrimaryDisplayState = state;
                this.mPrimaryDisplayBacklight = brightness;
            } else if (4096 == displayId) {
                this.mSecondaryDisplayState = state;
                this.mSecondaryDisplayBacklight = brightness;
            }
            DisplayController displayController = this.mDisplayControllers.get(displayId);
            if (displayController != null) {
                displayController.onDisplayStateChanged(state, brightness);
            } else {
                VSlog.w(TAG, "backlight state changed denied/invalid display controller");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void requestOverlayDismissInternal(int displayId, int moduleId) {
        if (!supportMultiDisplay() && displayId != 0) {
            VSlog.e(TAG, "invalid displayId " + displayId);
        } else if (supportMultiDisplay() && displayId != 0 && 4096 != displayId) {
            VSlog.e(TAG, "invalid displayId " + displayId);
        } else {
            DisplayController displayController = this.mDisplayControllers.get(displayId);
            if (displayController != null) {
                displayController.requestOverlayState(moduleId, false);
            } else {
                VSlog.w(TAG, "request overlay dismiss denied/invalid display controller");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public VivoDisplayModule getVivoDisplayModuleInfoInternal(int displayId, int moduleId, String moduleStr) {
        if (!supportMultiDisplay() && displayId != 0) {
            VSlog.e(TAG, "invalid displayId " + displayId);
            return null;
        } else if (supportMultiDisplay() && displayId != 0 && 4096 != displayId) {
            VSlog.e(TAG, "invalid displayId " + displayId);
            return null;
        } else {
            VivoDisplayModule displayModule = null;
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (this) {
                    DisplayController displayController = this.mDisplayControllers.get(displayId);
                    if (displayController != null) {
                        displayModule = displayController.getVivoDisplayModuleInfo(moduleId, moduleStr);
                    } else {
                        VSlog.w(TAG, "get vivo display module info denied/invalid display controller");
                    }
                }
                return displayModule;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerCallbackInternal(IVivoDisplayStateServiceCallback callback, int callingPid) {
        CallbackRecord record = new CallbackRecord(callingPid, callback);
        try {
            IBinder binder = callback.asBinder();
            binder.linkToDeath(record, 0);
            this.mCallbacks.put(callingPid, record);
            dispatchListenerRegisteredEvent(record);
        } catch (RemoteException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void dispatchListenerRegisteredEvent(CallbackRecord record) {
        if (record == null) {
            VSlog.w(TAG, "dispatch listener registered event denied/invalid listener");
            return;
        }
        DisplayController primaryDisplayController = this.mDisplayControllers.get(0);
        ArrayList<VivoDisplayModule> primaryDisplayContent = new ArrayList<>();
        if (primaryDisplayController != null) {
            primaryDisplayContent = primaryDisplayController.acquireDisplayContentDetail();
        }
        DisplayController secondaryDisplayController = this.mDisplayControllers.get(4096);
        ArrayList<VivoDisplayModule> secondaryDisplayContent = new ArrayList<>();
        if (secondaryDisplayController != null) {
            secondaryDisplayContent = secondaryDisplayController.acquireDisplayContentDetail();
        }
        record.notifyListenerRegisteredEventAsync(primaryDisplayContent, secondaryDisplayContent);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dispatchDisplayEvent(int displayId, boolean globalVisible, String module, boolean moduleVisible) {
        int numCallbacks;
        synchronized (this) {
            numCallbacks = this.mCallbacks.size();
            this.mTempCallbacks.clear();
            for (int i = 0; i < numCallbacks; i++) {
                this.mTempCallbacks.add(this.mCallbacks.valueAt(i));
            }
        }
        for (int i2 = 0; i2 < numCallbacks; i2++) {
            this.mTempCallbacks.get(i2).notifyDisplayContentEventAsync(displayId, globalVisible, module, moduleVisible);
        }
        this.mTempCallbacks.clear();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class ModuleMonitor implements IBinder.DeathRecipient {
        public final int mDisplayId;
        public final String mDisplayStr;
        public final String mModule;
        public final int mModuleId;
        private final IBinder mToken;

        public ModuleMonitor(int displayId, int moduleId, IBinder token, String module) {
            this.mDisplayId = displayId;
            this.mModuleId = moduleId;
            this.mToken = token;
            this.mModule = module;
            this.mDisplayStr = displayId == 0 ? "primary-display" : "secondary-display";
            if (VivoDisplayStateService.DEBUG) {
                VSlog.d(VivoDisplayStateService.TAG, "ModuleMonitor: " + this.mDisplayStr + " " + this.mModule + " with id " + this.mModuleId);
            }
            if (token != null) {
                try {
                    token.linkToDeath(this, 0);
                } catch (RemoteException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            VivoDisplayStateService.this.onMonitorDied(this);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onMonitorDied(final ModuleMonitor monitor) {
        synchronized (this) {
            VSlog.w(TAG, monitor.mDisplayStr + " " + monitor.mModule + " died");
            this.mModuleMonitors.remove(monitor.mModuleId);
            this.mHandler.post(new Runnable() { // from class: com.android.server.display.VivoDisplayStateService.2
                @Override // java.lang.Runnable
                public void run() {
                    VivoDisplayStateService.this.notifyContentStateInternal(monitor.mDisplayId, monitor.mModuleId, false);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class CallbackRecord implements IBinder.DeathRecipient {
        private final IVivoDisplayStateServiceCallback mCallback;
        public final int mPid;

        public CallbackRecord(int pid, IVivoDisplayStateServiceCallback callback) {
            this.mPid = pid;
            this.mCallback = callback;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            if (VivoDisplayStateService.DEBUG) {
                VSlog.d(VivoDisplayStateService.TAG, "Display state listener for pid " + this.mPid + " died.");
            }
            VivoDisplayStateService.this.onCallbackDied(this);
        }

        public void notifyDisplayContentEventAsync(int displayId, boolean globalVisible, String module, boolean moduleVisible) {
            IVivoDisplayStateServiceCallback iVivoDisplayStateServiceCallback = this.mCallback;
            if (iVivoDisplayStateServiceCallback == null) {
                VSlog.w(VivoDisplayStateService.TAG, "notifyDisplayContentEventAsync denied/invalid callback");
                return;
            }
            try {
                iVivoDisplayStateServiceCallback.onDisplayContentEvent(displayId, globalVisible, module, moduleVisible);
            } catch (RemoteException ex) {
                VSlog.w(VivoDisplayStateService.TAG, "Failed to notify process " + this.mPid + " display content event, assuming it died.", ex);
                binderDied();
            }
        }

        public void notifyListenerRegisteredEventAsync(ArrayList<VivoDisplayModule> primaryDisplayContent, ArrayList<VivoDisplayModule> secondaryDisplayContent) {
            IVivoDisplayStateServiceCallback iVivoDisplayStateServiceCallback = this.mCallback;
            if (iVivoDisplayStateServiceCallback == null) {
                VSlog.w(VivoDisplayStateService.TAG, "notifyListenerRegisteredEventAsync denied/invalid callback");
                return;
            }
            try {
                iVivoDisplayStateServiceCallback.onListenerRegistered(primaryDisplayContent, secondaryDisplayContent);
            } catch (RemoteException ex) {
                VSlog.w(VivoDisplayStateService.TAG, "Failed to notify process " + this.mPid + " listener registered event, assuming it died.", ex);
                binderDied();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onCallbackDied(CallbackRecord record) {
        synchronized (this) {
            this.mCallbacks.remove(record.mPid);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class DisplayController {
        private boolean mDismissOverlay;
        private int mDisplayBacklight;
        private boolean mDisplayContentVisible = false;
        private Context mDisplayContext;
        private int mDisplayId;
        private DisplayManager mDisplayManager;
        private VivoDisplayModuleController mDisplayModuleController;
        private int mDisplayPendingBacklight;
        private int mDisplayPendingState;
        private int mDisplayState;
        private String mDisplayStr;
        private PowerManager.WakeLock mDrawWakeLock;
        private boolean mForceOnNoChangeVDSS;
        private PowerManager mPowerManager;
        private boolean mShowOverlay;

        public DisplayController(int displayId, Context context, int state, int backlight, int pendingState, int pendingBacklight) {
            this.mDisplayId = displayId;
            this.mDisplayContext = context;
            this.mDisplayState = state;
            this.mDisplayBacklight = backlight;
            this.mDisplayPendingState = pendingState;
            this.mDisplayPendingBacklight = pendingBacklight;
            this.mDisplayModuleController = new VivoDisplayModuleController(this.mDisplayContext, this.mDisplayId);
            this.mDisplayStr = this.mDisplayId == 0 ? "primary-display" : "secondary-display";
            this.mPowerManager = (PowerManager) this.mDisplayContext.getSystemService("power");
            this.mDisplayManager = (DisplayManager) this.mDisplayContext.getSystemService("display");
            PowerManager powerManager = this.mPowerManager;
            this.mDrawWakeLock = powerManager.newWakeLock(128, "VivoDisplayStateService-" + this.mDisplayStr);
            setDrawWakeLockDisplayId();
        }

        public void onDisplayStateChanged(int displayState, int backlight) {
            VivoDisplayModuleController vivoDisplayModuleController;
            VivoDisplayModuleController vivoDisplayModuleController2;
            if (this.mDisplayState != displayState || ((this.mDisplayBacklight <= 0 && backlight > 0) || (this.mDisplayBacklight > 0 && backlight <= 0))) {
                VSlog.d(VivoDisplayStateService.TAG, this.mDisplayStr + " onDisplayStateChanged " + Display.stateToString(this.mDisplayState) + " to " + Display.stateToString(displayState) + " backlight: " + this.mDisplayBacklight + " to " + backlight);
            }
            if (this.mDisplayState != displayState && 1 == displayState) {
                this.mDismissOverlay = false;
            }
            if (VivoDisplayStateService.this.mAlwaysOnEnabled && 2 == this.mDisplayState && 1 == displayState) {
                VivoDisplayStateService.this.updateAmbientDisplayState();
            }
            if ((this.mDisplayBacklight > 0 && backlight <= 0) || (-1 == this.mDisplayBacklight && backlight == 0)) {
                VSlog.i(VivoDisplayStateService.TAG, "onDisplayStateChanged: " + this.mDisplayStr + " turns off, alwaysOnEnabled = " + VivoDisplayStateService.this.mAlwaysOnEnabled + ", suppressed = " + VivoDisplayStateService.this.mSuppressed);
                this.mDismissOverlay = false;
                VivoDisplayModuleController vivoDisplayModuleController3 = this.mDisplayModuleController;
                if (vivoDisplayModuleController3 != null) {
                    vivoDisplayModuleController3.updateAllModuleRequestState(1);
                }
                if (this.mDisplayContentVisible && (vivoDisplayModuleController2 = this.mDisplayModuleController) != null) {
                    vivoDisplayModuleController2.enableDisableBlackOverlay(true);
                }
            }
            if (2 == this.mDisplayState && 1 == displayState && this.mDisplayContentVisible && (vivoDisplayModuleController = this.mDisplayModuleController) != null) {
                this.mDismissOverlay = false;
                vivoDisplayModuleController.enableDisableBlackOverlay(true);
            }
            if (this.mDismissOverlay && 2 == displayState && this.mDisplayModuleController != null) {
                VSlog.i(VivoDisplayStateService.TAG, "onDisplayStateChanged: " + this.mDisplayStr + " dismiss overlay");
                this.mDisplayModuleController.enableDisableBlackOverlay(false);
            }
            if (this.mShowOverlay && this.mDisplayBacklight > 0 && backlight <= 0 && this.mDisplayModuleController != null) {
                VSlog.i(VivoDisplayStateService.TAG, "onDisplayStateChanged: " + this.mDisplayStr + " show overlay");
                this.mShowOverlay = false;
                this.mDisplayModuleController.enableDisableBlackOverlay(true);
            }
            if (1 == this.mDisplayState && ((3 == displayState || 4 == displayState) && !this.mDisplayContentVisible && this.mDisplayManager != null && (MultiDisplayManager.isMultiDisplay || (!this.mPowerManager.isInteractive() && (!VivoDisplayStateService.this.mAlwaysOnEnabled || VivoDisplayStateService.this.mSuppressed))))) {
                VSlog.i(VivoDisplayStateService.TAG, "onDisplayStateChanged: " + this.mDisplayStr + " reset STATE_OFF");
                this.mDisplayManager.setOverrideDisplayStateWrap(this.mDisplayId, 1, 1);
            }
            this.mDisplayState = displayState;
            this.mDisplayBacklight = backlight;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void onForceNoChangeVDSSChange(boolean forceOnNoChangeVDSS) {
            this.mForceOnNoChangeVDSS = forceOnNoChangeVDSS;
        }

        public void onDisplayStateBeginChange(int displayState, int backlight) {
            if ((this.mDisplayPendingState != displayState || ((this.mDisplayPendingBacklight <= 0 && backlight > 0) || (this.mDisplayPendingBacklight > 0 && backlight <= 0))) && VivoDisplayStateService.DEBUG) {
                VSlog.d(VivoDisplayStateService.TAG, this.mDisplayStr + " onDisplayStateBeginChange " + Display.stateToString(this.mDisplayPendingState) + " to " + Display.stateToString(displayState) + " backlight: " + this.mDisplayPendingBacklight + " to " + backlight);
            }
            if (this.mDisplayPendingBacklight <= 0 && backlight > 0) {
                VSlog.i(VivoDisplayStateService.TAG, "onDisplayStateBeginChange: " + this.mDisplayStr + " turns on");
                releaseDrawWakeLock();
                this.mDismissOverlay = false;
                VivoDisplayModuleController vivoDisplayModuleController = this.mDisplayModuleController;
                if (vivoDisplayModuleController != null) {
                    vivoDisplayModuleController.updateAllModuleRequestState(2);
                    this.mDisplayModuleController.enableDisableBlackOverlay(false);
                }
            }
            this.mDisplayPendingState = displayState;
            this.mDisplayPendingBacklight = backlight;
        }

        public void requestOverlayState(int moduleId, boolean show) {
            VivoDisplayModuleController vivoDisplayModuleController;
            VivoDisplayModuleController vivoDisplayModuleController2;
            VivoDisplayModuleController vivoDisplayModuleController3 = this.mDisplayModuleController;
            if (vivoDisplayModuleController3 == null) {
                VSlog.w(VivoDisplayStateService.TAG, this.mDisplayStr + " request overlay denied/invalid display state machine, show = " + show);
            } else if (!vivoDisplayModuleController3.isModuleRegistered(moduleId)) {
                VSlog.w(VivoDisplayStateService.TAG, this.mDisplayStr + " request overlay denied/invalid moduleId, show = " + show);
            } else {
                String moduleStr = this.mDisplayModuleController.getModuleStr(moduleId);
                VSlog.i(VivoDisplayStateService.TAG, this.mDisplayStr + ":" + moduleStr + " request overlay show = " + show);
                if (show) {
                    if (MultiDisplayManager.isMultiDisplay) {
                        this.mDismissOverlay = false;
                    }
                    this.mShowOverlay = true;
                    if (this.mDisplayBacklight <= 0 && (vivoDisplayModuleController2 = this.mDisplayModuleController) != null) {
                        vivoDisplayModuleController2.enableDisableBlackOverlay(true);
                        this.mShowOverlay = false;
                        return;
                    }
                    return;
                }
                if (MultiDisplayManager.isMultiDisplay) {
                    this.mDismissOverlay = true;
                }
                this.mShowOverlay = false;
                if (2 == this.mDisplayState && (vivoDisplayModuleController = this.mDisplayModuleController) != null) {
                    vivoDisplayModuleController.enableDisableBlackOverlay(false);
                }
            }
        }

        public int registerDisplayModule(VivoDisplayModule displayModule) {
            if (this.mDisplayModuleController == null) {
                VSlog.w(VivoDisplayStateService.TAG, this.mDisplayStr + " register module denied/invalid display state machine");
                return -1;
            }
            String module = displayModule.getName().toString();
            if (this.mDisplayModuleController.isModuleRegistered(module)) {
                VSlog.w(VivoDisplayStateService.TAG, this.mDisplayStr + " register module denied/" + module + " is already registered");
                return this.mDisplayModuleController.getModuleId(module);
            }
            return this.mDisplayModuleController.registerModule(displayModule);
        }

        public int registerDisplayModule(String module) {
            VivoDisplayModuleController vivoDisplayModuleController = this.mDisplayModuleController;
            if (vivoDisplayModuleController == null) {
                VSlog.w(VivoDisplayStateService.TAG, this.mDisplayStr + " register module denied/invalid display state machine");
                return -1;
            } else if (vivoDisplayModuleController.isModuleRegistered(module)) {
                VSlog.w(VivoDisplayStateService.TAG, this.mDisplayStr + " register module denied/" + module + " is already registered");
                return this.mDisplayModuleController.getModuleId(module);
            } else {
                return this.mDisplayModuleController.registerModule(module);
            }
        }

        public VivoDisplayModule getVivoDisplayModuleInfo(int moduleId, String moduleStr) {
            VivoDisplayModuleController vivoDisplayModuleController = this.mDisplayModuleController;
            if (vivoDisplayModuleController == null) {
                VSlog.w(VivoDisplayStateService.TAG, this.mDisplayStr + " get vivo display module info denied/invalid display state machine");
                return null;
            } else if (vivoDisplayModuleController.isModuleRegistered(moduleId)) {
                return this.mDisplayModuleController.getVivoDisplayModuleInfo(moduleId);
            } else {
                if (this.mDisplayModuleController.isModuleRegistered(moduleStr)) {
                    VivoDisplayModuleController vivoDisplayModuleController2 = this.mDisplayModuleController;
                    return vivoDisplayModuleController2.getVivoDisplayModuleInfo(vivoDisplayModuleController2.getModuleId(moduleStr));
                }
                return null;
            }
        }

        public void updateDisplayContent(final int moduleId, boolean visible) {
            boolean z;
            boolean displayModuleContentVisible;
            boolean displayModuleContentVisible2;
            VivoDisplayModuleController vivoDisplayModuleController;
            VivoDisplayModuleController vivoDisplayModuleController2 = this.mDisplayModuleController;
            if (vivoDisplayModuleController2 == null) {
                VSlog.w(VivoDisplayStateService.TAG, this.mDisplayStr + " update content denied/invalid display state machine");
            } else if (!vivoDisplayModuleController2.isModuleRegistered(moduleId)) {
                VSlog.e(VivoDisplayStateService.TAG, this.mDisplayStr + " update content denied/invalid moduleId");
            } else {
                final String module = this.mDisplayModuleController.getModuleStr(moduleId);
                boolean oldDisplayModuleContentVisible = this.mDisplayModuleController.displayModuleContentVisible(module);
                if (oldDisplayModuleContentVisible == visible) {
                    return;
                }
                boolean oldDisplayContentVisible = this.mDisplayContentVisible;
                this.mDisplayModuleController.updateDisplayContentState(module, visible);
                this.mDisplayContentVisible = this.mDisplayModuleController.displayContentVisible();
                boolean displayModuleContentVisible3 = this.mDisplayModuleController.displayModuleContentVisible(module);
                VSlog.i(VivoDisplayStateService.TAG, "display: " + this.mDisplayStr + " module: " + module + " oldState: " + oldDisplayModuleContentVisible + " newState: " + displayModuleContentVisible3 + " oldGlobalState: " + oldDisplayContentVisible + " newGlobalState: " + this.mDisplayContentVisible);
                if (!oldDisplayModuleContentVisible || displayModuleContentVisible3) {
                    z = true;
                    displayModuleContentVisible = displayModuleContentVisible3;
                } else {
                    int moduleRequestState = this.mDisplayModuleController.getModuleRequestState(module);
                    int i = 2;
                    final int pendingRequestState = this.mDisplayBacklight == 0 ? 1 : 2;
                    int pendingState = this.mDisplayContentVisible ? 4 : 1;
                    int pendingState2 = (this.mDisplayState != 2 || this.mForceOnNoChangeVDSS) ? this.mDisplayBacklight == 0 ? pendingState : 2 : 2;
                    if (this.mDisplayPendingState != 2 || this.mForceOnNoChangeVDSS) {
                        i = pendingState2;
                    }
                    final int pendingState3 = i;
                    int pendingState4 = this.mDisplayId;
                    int oppositDisplayPendingBacklight = pendingState4 == 0 ? VivoDisplayStateService.this.mSecondaryDisplayPendingBacklight : VivoDisplayStateService.this.mPrimaryDisplayPendingBacklight;
                    if (VivoDisplayStateService.DEBUG) {
                        VSlog.i(VivoDisplayStateService.TAG, "reset " + module + " request-state: " + Display.stateToString(moduleRequestState) + " pending-request-state: " + Display.stateToString(pendingRequestState) + " pending-set-state: " + Display.stateToString(pendingState3) + " opposit-pending-backlight: " + oppositDisplayPendingBacklight);
                    }
                    this.mDisplayModuleController.updateModuleRequestState(module, pendingRequestState);
                    boolean isInteractive = this.mPowerManager.isInteractive();
                    int i2 = ProcessList.HOME_APP_ADJ;
                    int delayTime1 = isInteractive ? 600 : 200;
                    if (!isInteractive) {
                        i2 = 0;
                    }
                    int delayTime2 = i2;
                    if (1 == pendingState3 && oppositDisplayPendingBacklight > 0) {
                        VivoDisplayStateService.this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.display.VivoDisplayStateService.DisplayController.1
                            @Override // java.lang.Runnable
                            public void run() {
                                DisplayController.this.requestDisplayState(moduleId, 1, 0, false, false);
                            }
                        }, delayTime1);
                        displayModuleContentVisible = displayModuleContentVisible3;
                        z = true;
                    } else if (delayTime2 <= 0) {
                        z = true;
                        displayModuleContentVisible = displayModuleContentVisible3;
                        requestDisplayState(moduleId, pendingState3, 0, false, false);
                        this.mDisplayModuleController.updateModuleRequestState(module, pendingRequestState);
                    } else {
                        VivoDisplayStateService.this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.display.VivoDisplayStateService.DisplayController.2
                            @Override // java.lang.Runnable
                            public void run() {
                                DisplayController.this.requestDisplayState(moduleId, pendingState3, 0, false, false);
                                DisplayController.this.mDisplayModuleController.updateModuleRequestState(module, pendingRequestState);
                            }
                        }, delayTime2);
                        displayModuleContentVisible = displayModuleContentVisible3;
                        z = true;
                    }
                }
                if (this.mDisplayContentVisible && this.mDisplayBacklight == 0 && this.mDisplayPendingBacklight == 0 && (vivoDisplayModuleController = this.mDisplayModuleController) != null && !this.mDismissOverlay) {
                    vivoDisplayModuleController.enableDisableBlackOverlay(z);
                }
                if (oldDisplayContentVisible == this.mDisplayContentVisible) {
                    displayModuleContentVisible2 = displayModuleContentVisible;
                    if (oldDisplayModuleContentVisible == displayModuleContentVisible2) {
                        return;
                    }
                } else {
                    displayModuleContentVisible2 = displayModuleContentVisible;
                }
                VivoDisplayStateService.this.dispatchDisplayEvent(this.mDisplayId, this.mDisplayContentVisible, module, displayModuleContentVisible2);
            }
        }

        public ArrayList<VivoDisplayModule> acquireDisplayContentDetail() {
            ArrayList<VivoDisplayModule> displayContent = new ArrayList<>();
            ArrayList<String> registeredModules = this.mDisplayModuleController.getAllRegisteredModules();
            int numModules = registeredModules.size();
            for (int i = 0; i < numModules; i++) {
                String moduleStr = registeredModules.get(i);
                boolean visible = this.mDisplayModuleController.displayModuleContentVisible(moduleStr);
                int moduleId = this.mDisplayModuleController.getModuleId(moduleStr);
                VivoDisplayModule displayModule = new VivoDisplayModule(this.mDisplayId, moduleId, moduleStr);
                displayModule.setVisibleState(visible);
                if (VivoDisplayStateService.DEBUG) {
                    VSlog.d(VivoDisplayStateService.TAG, "acquireDisplayContentDetail: " + this.mDisplayStr + " module: " + moduleStr + " visible: " + visible);
                }
                displayContent.add(displayModule);
            }
            return displayContent;
        }

        public void requestDisplayState(final int moduleId, int state, int brightness, boolean checkVisible, boolean internalCall) {
            boolean moduleContentVisible;
            if (!internalCall && !this.mDisplayModuleController.isModuleRegistered(moduleId)) {
                VSlog.e(VivoDisplayStateService.TAG, this.mDisplayStr + " request display state denied/invalid moduleId");
                return;
            }
            String module = internalCall ? VivoPermissionUtils.OS_PKG : this.mDisplayModuleController.getModuleStr(moduleId);
            int oldRequestDisplayState = internalCall ? 0 : this.mDisplayModuleController.getModuleRequestState(module);
            if (!internalCall) {
                moduleContentVisible = this.mDisplayModuleController.displayModuleContentVisible(module);
            } else {
                moduleContentVisible = true;
            }
            int oppositDisplayPendingState = this.mDisplayId == 0 ? VivoDisplayStateService.this.mSecondaryDisplayPendingState : VivoDisplayStateService.this.mPrimaryDisplayPendingState;
            int oppositDisplayPendingBacklight = this.mDisplayId == 0 ? VivoDisplayStateService.this.mSecondaryDisplayPendingBacklight : VivoDisplayStateService.this.mPrimaryDisplayPendingBacklight;
            if (!internalCall || VivoDisplayStateService.this.mAlwaysOnEnabled) {
                VSlog.i(VivoDisplayStateService.TAG, this.mDisplayStr + ":" + module + " request " + Display.stateToString(state) + "/" + Display.stateToString(oldRequestDisplayState));
            }
            if (!internalCall && VivoDisplayStateService.this.mAlwaysOnEnabled && VivoDisplayStateService.this.mSuppressed) {
                VSlog.e(VivoDisplayStateService.TAG, this.mDisplayStr + " request display state denied/alwaysOnEnabled and suppressed");
            } else if (2 == state) {
                VSlog.w(VivoDisplayStateService.TAG, this.mDisplayStr + ":" + module + " request STATE_ON denied/STATE_ON is not allowed to switch");
            } else if (!checkVisible || moduleContentVisible || 1 == state) {
                if (!internalCall && 1 == this.mDisplayState && 3 == state && oldRequestDisplayState != state && 2 == oppositDisplayPendingState && oppositDisplayPendingBacklight == 0) {
                    VSlog.w(VivoDisplayStateService.TAG, this.mDisplayStr + ":" + module + " request STATE_DOZE pending");
                    this.mDisplayModuleController.updateModuleRequestState(module, state);
                    VivoDisplayStateService.this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.display.VivoDisplayStateService.DisplayController.3
                        @Override // java.lang.Runnable
                        public void run() {
                            DisplayController.this.requestDisplayState(moduleId, 3, 0, true, false);
                        }
                    }, 400L);
                    return;
                }
                if (!internalCall) {
                    this.mDisplayModuleController.updateModuleRequestState(module, state);
                }
                if (1 == state) {
                    requestDisplayStateOFF(module, brightness);
                } else if (2 == state) {
                    requestDisplayStateON(module, brightness);
                } else if (3 == state) {
                    requestDisplayStateDOZE(module, brightness, internalCall);
                } else if (4 == state) {
                    requestDisplayStateSUSPEND(module, brightness, internalCall);
                } else {
                    VSlog.w(VivoDisplayStateService.TAG, this.mDisplayStr + ":" + module + " request display state denied/invalid display state");
                }
            } else {
                VSlog.w(VivoDisplayStateService.TAG, this.mDisplayStr + ":" + module + " request " + Display.stateToString(state) + " denied/module invisible");
            }
        }

        private void requestDisplayStateOFF(String module, int brightness) {
            if (this.mDisplayContentVisible && (!VivoDisplayStateService.this.mAlwaysOnEnabled || !VivoDisplayStateService.this.mSuppressed)) {
                VSlog.w(VivoDisplayStateService.TAG, this.mDisplayStr + ":" + module + " request STATE_OFF denied/content still display, alwaysOnEnabled = " + VivoDisplayStateService.this.mAlwaysOnEnabled + ", suppressed = " + VivoDisplayStateService.this.mSuppressed);
            } else if (this.mDisplayBacklight != 0) {
                VSlog.w(VivoDisplayStateService.TAG, this.mDisplayStr + ":" + module + " request STATE_OFF denied/backlight is lit up");
            } else {
                DisplayManager displayManager = this.mDisplayManager;
                if (displayManager == null) {
                    VSlog.w(VivoDisplayStateService.TAG, this.mDisplayStr + ":" + module + " request STATE_OFF denied/invalid display manager");
                    return;
                }
                displayManager.setOverrideDisplayStateWrap(this.mDisplayId, 1, 1);
                releaseDrawWakeLock();
            }
        }

        private void requestDisplayStateON(String module, int brightness) {
            if (!this.mDisplayContentVisible) {
                VSlog.w(VivoDisplayStateService.TAG, this.mDisplayStr + ":" + module + " request STATE_ON denied/content not display");
            } else if (this.mDisplayBacklight != 0) {
                VSlog.w(VivoDisplayStateService.TAG, this.mDisplayStr + ":" + module + " request STATE_ON denied/backlight is lit up");
            } else {
                DisplayManager displayManager = this.mDisplayManager;
                if (displayManager == null) {
                    VSlog.w(VivoDisplayStateService.TAG, this.mDisplayStr + ":" + module + " request STATE_ON denied/invalid display manager");
                    return;
                }
                displayManager.setOverrideDisplayStateWrap(this.mDisplayId, 2, 1);
                releaseDrawWakeLock();
            }
        }

        private void requestDisplayStateDOZE(String module, int brightness, boolean internalCall) {
            if (!this.mDisplayContentVisible && (!VivoDisplayStateService.this.mAlwaysOnEnabled || VivoDisplayStateService.this.mSuppressed)) {
                VSlog.w(VivoDisplayStateService.TAG, this.mDisplayStr + ":" + module + " request STATE_DOZE denied/content not display");
            } else if (this.mDisplayBacklight != 0) {
                VSlog.w(VivoDisplayStateService.TAG, this.mDisplayStr + ":" + module + " request STATE_DOZE denied/backlight is lit up");
            } else if (!internalCall && 2 == this.mDisplayState && this.mDisplayModuleController.isStateOnPolled()) {
                VSlog.w(VivoDisplayStateService.TAG, this.mDisplayStr + ":" + module + " request STATE_DOZE denied/ON is requested by other modules");
            } else {
                int oppositDisplayPendingBacklight = this.mDisplayId == 0 ? VivoDisplayStateService.this.mSecondaryDisplayPendingBacklight : VivoDisplayStateService.this.mPrimaryDisplayPendingBacklight;
                if (oppositDisplayPendingBacklight > 0 && !"VivoCamera".equals(module) && !"light".equals(module)) {
                    VSlog.w(VivoDisplayStateService.TAG, this.mDisplayStr + ":" + module + " request STATE_DOZE denied/opposit display is turning on");
                } else if (this.mDisplayManager == null) {
                    VSlog.w(VivoDisplayStateService.TAG, this.mDisplayStr + ":" + module + " request STATE_DOZE denied/invalid display manager");
                } else if (internalCall) {
                    if (!VivoDisplayStateService.this.mAlwaysOnEnabled) {
                        this.mDisplayManager.setOverrideDisplayStateWrap(this.mDisplayId, 3, 2);
                    } else {
                        this.mDisplayManager.setOverrideDisplayStateWrap(this.mDisplayId, 3, 3);
                    }
                } else {
                    acquireDrawWakeLock();
                    this.mDisplayManager.setOverrideDisplayStateWrap(this.mDisplayId, 3, 1);
                }
            }
        }

        private void requestDisplayStateSUSPEND(String module, int brightness, boolean internalCall) {
            if (!this.mDisplayContentVisible) {
                VSlog.w(VivoDisplayStateService.TAG, this.mDisplayStr + ":" + module + " request STATE_DOZE_SUSPEND denied/content not display");
            } else if (this.mDisplayBacklight != 0) {
                VSlog.w(VivoDisplayStateService.TAG, this.mDisplayStr + ":" + module + " request STATE_DOZE_SUSPEND denied/backlight is lit up");
            } else if (!internalCall && 2 == this.mDisplayState && this.mDisplayModuleController.isStateOnPolled()) {
                VSlog.w(VivoDisplayStateService.TAG, this.mDisplayStr + ":" + module + " request STATE_DOZE_SUSPEND denied/ON is requested by other modules");
            } else if (!internalCall && 3 == this.mDisplayState && this.mDisplayModuleController.isStateDozePolled()) {
                VSlog.w(VivoDisplayStateService.TAG, this.mDisplayStr + ":" + module + " request STATE_DOZE_SUSPEND denied/DOZE is requested by other modules");
            } else if (1 == this.mDisplayState) {
                VSlog.w(VivoDisplayStateService.TAG, this.mDisplayStr + ":" + module + " request STATE_DOZE_SUSPEND denied/current display state is OFF");
            } else {
                int oppositDisplayPendingBacklight = this.mDisplayId == 0 ? VivoDisplayStateService.this.mSecondaryDisplayPendingBacklight : VivoDisplayStateService.this.mPrimaryDisplayPendingBacklight;
                if (oppositDisplayPendingBacklight > 0 && !"VivoCamera".equals(module) && !"light".equals(module)) {
                    VSlog.w(VivoDisplayStateService.TAG, this.mDisplayStr + ":" + module + " request STATE_DOZE_SUSPEND denied/opposit display is turning on");
                    return;
                }
                DisplayManager displayManager = this.mDisplayManager;
                if (displayManager == null) {
                    VSlog.w(VivoDisplayStateService.TAG, this.mDisplayStr + ":" + module + " request STATE_DOZE_SUSPEND denied/invalid display manager");
                } else if (internalCall) {
                    displayManager.setOverrideDisplayStateWrap(this.mDisplayId, 4, 2);
                } else {
                    releaseDrawWakeLock();
                }
            }
        }

        private boolean setDrawWakeLockDisplayId() {
            PowerManager.WakeLock wakeLock = this.mDrawWakeLock;
            if (wakeLock == null) {
                VSlog.w(VivoDisplayStateService.TAG, "set draw wake lock denied/invalid draw wake lock");
                return false;
            }
            Class clazz = wakeLock.getClass();
            try {
                try {
                    Method method = clazz.getDeclaredMethod("setDisplayId", Integer.TYPE);
                    if (method == null) {
                        return true;
                    }
                    method.invoke(this.mDrawWakeLock, Integer.valueOf(this.mDisplayId));
                    return true;
                } catch (NoSuchMethodException e) {
                    VSlog.e(VivoDisplayStateService.TAG, "set draw wake lock denied/NoSuchMethodException");
                    return false;
                } catch (InvocationTargetException e3) {
                    VSlog.e(VivoDisplayStateService.TAG, "set draw wake lock denied/InvocationTargetException " + e3.getMessage());
                    return false;
                }
            } catch (IllegalAccessException e2) {
                VSlog.e(VivoDisplayStateService.TAG, "set draw wake lock denied/IllegalAccessException " + e2.getMessage());
                return false;
            }
        }

        private void acquireDrawWakeLock() {
            PowerManager.WakeLock wakeLock = this.mDrawWakeLock;
            if (wakeLock != null && !wakeLock.isHeld()) {
                this.mDrawWakeLock.acquire();
            } else {
                VSlog.w(VivoDisplayStateService.TAG, "draw wake lock is already acquired");
            }
        }

        private void releaseDrawWakeLock() {
            PowerManager.WakeLock wakeLock = this.mDrawWakeLock;
            if (wakeLock != null && wakeLock.isHeld()) {
                this.mDrawWakeLock.release();
            } else {
                VSlog.w(VivoDisplayStateService.TAG, "draw wake lock is already released");
            }
        }
    }
}