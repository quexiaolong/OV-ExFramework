package com.vivo.services.rms.display;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemProperties;
import android.view.InputEvent;
import com.android.server.ServiceThread;
import com.vivo.common.utils.VLog;
import com.vivo.services.rms.ProcessInfo;
import com.vivo.services.rms.appmng.AppManager;
import com.vivo.services.rms.display.DisplayConfigsManager;
import com.vivo.services.rms.display.scene.InteractionScene;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

/* loaded from: classes.dex */
public class RefreshRateAdjuster {
    private static final int MAX_VSYNC_VERIFY_COUNT = 10;
    private static final int MSG_INIT = 1;
    private static final int MSG_RELEASE_HANDLES = 2;
    private static final int MSG_SET_ACTIVE_MODE = 3;
    private static final int MSG_SET_DFPS = 6;
    private static final int MSG_USER_TOUCH = 4;
    private static final int MSG_USER_TOUCH_TIME_OUT = 5;
    private static final int MSG_VSYNC_VERIFY = 7;
    private static final int REQUEST_SET_ACTIVE_MODE_MILLIS = 16;
    private static final int SET_DFPS_DELAY_MILLIS = 100;
    public static final String TAG = "RefreshRateAdjuster";
    private static final int VSYNC_VERIFY_DELAY_MILLIS = 800;
    private AjusterHandler mAdjusterHandler;
    private Context mContext;
    private RefreshRateRequest mHighestPriorityRequest;
    private ProcessInfo mMyProc;
    private boolean mResetActiveModeRequested;
    private SceneManager mSceneManager;
    private int mShellDFps;
    private long mShellFpsHandle;
    private long mUserTouchHandle;
    private int mVersion;
    private boolean mVsyncVerify;
    private int mVsyncVerifyCount;
    private static final DisplayConfigsManager DISPLAY_CONFIGS = DisplayConfigsManager.getInstance();
    private static long sNextId = 0;

    static /* synthetic */ int access$1408(RefreshRateAdjuster x0) {
        int i = x0.mVsyncVerifyCount;
        x0.mVsyncVerifyCount = i + 1;
        return i;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Instance {
        private static final RefreshRateAdjuster INSTANCE = new RefreshRateAdjuster();

        private Instance() {
        }
    }

    public static RefreshRateAdjuster getInstance() {
        return Instance.INSTANCE;
    }

    private RefreshRateAdjuster() {
        this.mVersion = -1;
        this.mVsyncVerifyCount = 0;
        this.mVsyncVerify = false;
        this.mUserTouchHandle = -1L;
        this.mResetActiveModeRequested = true;
        this.mShellFpsHandle = -1L;
        this.mShellDFps = 0;
        ServiceThread adjusterThread = new ServiceThread("RefreshRateAdjuster", -10, false);
        adjusterThread.start();
        AjusterHandler ajusterHandler = new AjusterHandler(adjusterThread.getLooper());
        this.mAdjusterHandler = ajusterHandler;
        this.mSceneManager = new SceneManager(ajusterHandler.getLooper());
    }

    public void initialize(Context context) {
        this.mContext = context;
        DISPLAY_CONFIGS.initialize();
        this.mAdjusterHandler.sendEmptyMessageDelayed(1, 50L);
    }

    public RefreshRateRequest getHighestPriorityRequest() {
        return this.mHighestPriorityRequest;
    }

    public ProcessInfo getSelfProc() {
        return this.mMyProc;
    }

    public long nextHandle() {
        long j;
        synchronized (this) {
            j = sNextId + 1;
            sNextId = j;
        }
        return j;
    }

    public void requestSetActiveMode(boolean delayed) {
        if (GlobalConfigs.isFeatureSupported()) {
            boolean setActiveModeRequested = this.mAdjusterHandler.hasMessages(3);
            if (!delayed) {
                if (setActiveModeRequested) {
                    this.mAdjusterHandler.removeMessages(3);
                }
                this.mAdjusterHandler.sendEmptyMessage(3);
            } else if (!setActiveModeRequested) {
                this.mAdjusterHandler.sendEmptyMessageDelayed(3, 16L);
            }
        }
    }

    public void notifyFeatureSwitch(boolean enable) {
        if (enable) {
            reqeustResetActiveMode();
            requestSetActiveMode();
        }
        VLog.d("RefreshRateAdjuster", "notifyFeatureSwitch " + enable);
    }

    public void requestSetActiveMode() {
        requestSetActiveMode(true);
    }

    public void reqeustResetActiveMode() {
        this.mResetActiveModeRequested = true;
    }

    public void resetRequestTime() {
        if (GlobalConfigs.isFeatureSupported()) {
            boolean setActiveModeRequested = this.mAdjusterHandler.hasMessages(3);
            if (setActiveModeRequested) {
                this.mAdjusterHandler.removeMessages(3);
                this.mAdjusterHandler.sendEmptyMessageDelayed(3, 16L);
            }
        }
    }

    public void onDisplayActiveModeChanged(long physicalDisplayId, int configId) {
        if (!GlobalConfigs.isFeatureSupported() || physicalDisplayId != DISPLAY_CONFIGS.getPhysicalDisplayId()) {
            return;
        }
        requestVerifyActiveMode(false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void requestVerifyActiveMode(boolean fromVerify) {
        this.mAdjusterHandler.removeMessages(7);
        AjusterHandler ajusterHandler = this.mAdjusterHandler;
        ajusterHandler.sendMessageDelayed(ajusterHandler.obtainMessage(7, fromVerify ? 1 : 0, 0), 800L);
    }

    public float getActiveRefreshRate() {
        if (GlobalConfigs.isFeatureSupported()) {
            return DISPLAY_CONFIGS.getActiveFps();
        }
        return 0.0f;
    }

    public void onScreenStateChanged(boolean screenOn) {
        if (GlobalConfigs.isFeatureSupported()) {
            StringBuilder sb = new StringBuilder();
            sb.append("onScreenStateChanged screenOff = ");
            sb.append(!screenOn);
            VLog.d("RefreshRateAdjuster", sb.toString());
            reqeustResetActiveMode();
            requestSetActiveMode();
        }
    }

    public void clearDeathHandles(int caller, ArrayList<String> handles) {
        VLog.d("RefreshRateAdjuster", String.format("clearDeathHandles caller=%d handles=%s", Integer.valueOf(caller), handles));
        releaseHandles(handles);
    }

    public void releaseHandles(ArrayList<String> handles) {
        if (GlobalConfigs.isFeatureSupported() && handles != null && !handles.isEmpty()) {
            this.mAdjusterHandler.obtainMessage(2, 0, 0, handles).sendToTarget();
        }
    }

    public long acquireRefreshRate(long handle, String sceneName, String reason, int fps, int priority, int duration, int caller, int client, int states, boolean dfps, int extra) {
        ProcessInfo callerApp;
        if (GlobalConfigs.isFeatureSupported() && handle > 0 && (caller <= 0 || DISPLAY_CONFIGS.isMultiMode() || dfps)) {
            if (caller <= 0) {
                callerApp = this.mMyProc;
            } else {
                callerApp = AppManager.getInstance().getProcessInfo(caller);
            }
            if (callerApp == null) {
                VLog.e("RefreshRateAdjuster", "acquireRefreshRate callerApp not found " + caller);
                return -1L;
            }
            ProcessInfo clientApp = AppManager.getInstance().getProcessInfo(client);
            if (client <= 0 || clientApp != null) {
                if (this.mSceneManager.acquireRefreshRate(String.format("%d_%d", Integer.valueOf(callerApp.mPid), Long.valueOf(handle)), sceneName, reason, fps, priority, callerApp, duration, clientApp, states, dfps, extra)) {
                    return handle;
                }
                return -1L;
            }
            VLog.e("RefreshRateAdjuster", "acquireRefreshRate clientApp not found " + client);
            return -1L;
        }
        return -1L;
    }

    public boolean releaseRefreshRate(int caller, long handle) {
        if (GlobalConfigs.isFeatureSupported()) {
            if (caller <= 0) {
                caller = this.mMyProc.mPid;
            }
            return this.mSceneManager.releaseRefreshRate(String.format("%d_%d", Integer.valueOf(caller), Long.valueOf(handle)));
        }
        return false;
    }

    public long acquireRefreshRate(String sceneName, String reason, int reqFps, int reqDFps, int configBits, int priority, int duration, int caller, int client, int states, boolean dfps, int extra) {
        int extra2 = extra & 255;
        int reqDFps2 = reqDFps & 255;
        int configBits2 = configBits & 255;
        if (configBits2 > 0) {
            extra2 = extra2 | (configBits2 << 8) | 4;
        }
        if (reqDFps2 >= reqFps) {
            reqDFps2 = 0;
        }
        if (reqDFps2 > 0) {
            extra2 = extra2 | (reqDFps2 << 16) | 2;
        }
        return acquireRefreshRate(nextHandle(), sceneName, reason, reqFps, priority, duration, caller, client, states, dfps, extra2);
    }

    public long acquireRefreshRate(String sceneName, String reason, int fps, int priority, int duration, int caller, int client, int states, boolean dfps, int extra) {
        return acquireRefreshRate(nextHandle(), sceneName, reason, fps, priority, duration, caller, client, states, dfps, extra);
    }

    public long acquireRefreshRate(String sceneName, String reason, int fps, int priority, int duration, int caller, int client, int states, boolean dfps) {
        return acquireRefreshRate(nextHandle(), sceneName, reason, fps, priority, duration, caller, client, states, dfps, 0);
    }

    public boolean releaseRefreshRate(long handle) {
        return releaseRefreshRate(0, handle);
    }

    public boolean createScene(String name, int priority, boolean powerFirst) {
        if (GlobalConfigs.isFeatureSupported()) {
            return this.mSceneManager.createScene(name, priority, powerFirst);
        }
        return false;
    }

    public void setConfigBits(int caller, int configBits, int configMasks) {
        DISPLAY_CONFIGS.setConfigBits(caller, configBits, configMasks);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void setResolution(int width, int height) {
        DISPLAY_CONFIGS.setResolution(width, height);
    }

    public void setConfigs(Bundle bundle) {
        int version;
        if (bundle != null && (version = bundle.getInt(GlobalConfigs.KEY_VERSION, this.mVersion)) != this.mVersion) {
            this.mVersion = version;
            Bundle sceneConfigs = bundle.getBundle(GlobalConfigs.KEY_SCENE_CONFIGS);
            SceneManager sceneManager = this.mSceneManager;
            if (sceneManager != null && sceneConfigs != null) {
                sceneManager.setConfigs(sceneConfigs);
            }
            GlobalConfigs.setConfigs(bundle);
        }
    }

    public void updateRequest(int flags, int value) {
        if (!GlobalConfigs.isFeatureSupported()) {
            return;
        }
        this.mSceneManager.updateRequest(flags, value);
    }

    public void updateRequest() {
        updateRequest(1, 0);
    }

    public void notifyInputEvent(InputEvent event) {
        if (!GlobalConfigs.isFeatureSupported() || !DISPLAY_CONFIGS.isMultiMode()) {
            return;
        }
        if (this.mUserTouchHandle > 0) {
            this.mAdjusterHandler.removeMessages(5);
            this.mAdjusterHandler.sendEmptyMessageDelayed(5, GlobalConfigs.getTouchBoostDuration());
            return;
        }
        AjusterHandler ajusterHandler = this.mAdjusterHandler;
        ajusterHandler.sendMessageAtFrontOfQueue(ajusterHandler.obtainMessage(4, 0, 0));
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class AjusterHandler extends Handler {
        private AjusterHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    performInit();
                    return;
                case 2:
                    performReleaseHandles((ArrayList) msg.obj);
                    return;
                case 3:
                    performSetActiveConfig();
                    return;
                case 4:
                    performUserTouch(true);
                    return;
                case 5:
                    performUserTouch(false);
                    return;
                case 6:
                    performSetDFps(msg.arg1, false);
                    return;
                case 7:
                    performVsyncVerify(msg.arg1);
                    return;
                default:
                    return;
            }
        }

        private void performInit() {
            RefreshRateAdjuster.this.mMyProc = AppManager.getInstance().getProcessInfo(Process.myPid());
            boolean forceOpen = SystemProperties.getBoolean("persist.sys.vivo.refreshrate_force_open", false);
            if (RefreshRateAdjuster.DISPLAY_CONFIGS.getBuildInDisplayToken() != null) {
                if (RefreshRateAdjuster.DISPLAY_CONFIGS.isMultiMode() || forceOpen) {
                    GlobalConfigs.initialize(RefreshRateAdjuster.this.mContext, RefreshRateAdjuster.this.mAdjusterHandler.getLooper());
                    RefreshRateAdjuster.this.requestSetActiveMode(false);
                    RefreshRateAdjuster.this.reqeustResetActiveMode();
                }
            }
        }

        private void performReleaseHandles(ArrayList<String> handles) {
            Iterator<String> it = handles.iterator();
            while (it.hasNext()) {
                String handle = it.next();
                RefreshRateAdjuster.this.mSceneManager.releaseRefreshRate(handle);
            }
        }

        private void performUserTouch(boolean acquire) {
            if (acquire) {
                if (RefreshRateAdjuster.this.mUserTouchHandle > 0) {
                    return;
                }
                RefreshRateAdjuster refreshRateAdjuster = RefreshRateAdjuster.this;
                refreshRateAdjuster.mUserTouchHandle = refreshRateAdjuster.acquireRefreshRate(SceneManager.INTERACTION_SCENE, InteractionScene.INPUT_REASON, GlobalConfigs.getInteractionMaxRefreshRate(), 0, GlobalConfigs.getInteractionConfigBits(), 20, 0, 0, 0, 0, false, 1);
                RefreshRateAdjuster.this.mAdjusterHandler.removeMessages(5);
                RefreshRateAdjuster.this.mAdjusterHandler.sendEmptyMessageDelayed(5, GlobalConfigs.getTouchBoostDuration());
            } else if (RefreshRateAdjuster.this.mUserTouchHandle > 0) {
                RefreshRateAdjuster refreshRateAdjuster2 = RefreshRateAdjuster.this;
                refreshRateAdjuster2.releaseRefreshRate(refreshRateAdjuster2.mUserTouchHandle);
                RefreshRateAdjuster.this.mUserTouchHandle = -1L;
            }
        }

        private void performSetActiveConfig() {
            DisplayConfigsManager.DisplayMode mode;
            if (GlobalConfigs.isFeatureEnabled()) {
                if (RefreshRateAdjuster.this.mSceneManager.isEmpty()) {
                    return;
                }
                int dfps = 0;
                RefreshRateRequest request = null;
                boolean screenOff = !GlobalConfigs.isScreenOn();
                boolean requsetVerify = false;
                boolean requestUpdate = false;
                if (RefreshRateAdjuster.this.mResetActiveModeRequested) {
                    RefreshRateAdjuster.this.mResetActiveModeRequested = false;
                    requsetVerify = !screenOff;
                }
                DisplayConfigsManager.DisplayMode activeMode = RefreshRateAdjuster.DISPLAY_CONFIGS.getActiveMode();
                int modeId = activeMode.id;
                if (!screenOff) {
                    if (!RefreshRateAdjuster.this.mVsyncVerify) {
                        request = RefreshRateAdjuster.this.mSceneManager.chooseHighestPriorityRequest();
                        if (request == null) {
                            return;
                        }
                        modeId = request.mode.id;
                        if (GlobalConfigs.isDfpsEnable()) {
                            dfps = RefreshRateAdjuster.this.mShellDFps;
                            int realFps = request.mode.fps;
                            if (request.reqDFps > 0 && request.reqDFps < realFps) {
                                dfps = request.reqDFps;
                            } else if (request.dfps && request.fps > 0 && realFps > request.fps && !GlobalConfigs.isSameFps(realFps, request.fps)) {
                                dfps = request.fps;
                            }
                        }
                    } else {
                        DisplayConfigsManager.DisplayMode mode2 = RefreshRateAdjuster.DISPLAY_CONFIGS.getExactModeByFps(activeMode.resolution, GlobalConfigs.getVsyncRate(), activeMode.configBits);
                        RefreshRateAdjuster.this.mVsyncVerify = false;
                        requestUpdate = true;
                        if (mode2 != null) {
                            modeId = mode2.id;
                            VLog.e("RefreshRateAdjuster", "setActiveConfig error verify to " + mode2);
                        }
                    }
                } else {
                    performSetDFps(0, true);
                    if (activeMode != null && activeMode.isPrivate && (mode = RefreshRateAdjuster.DISPLAY_CONFIGS.findDisplayMode(activeMode.resolution, activeMode.fps, activeMode.configBits, false)) != null) {
                        modeId = mode.id;
                        VLog.e("RefreshRateAdjuster", "performSetActiveConfig reset privatefps=" + activeMode.fps);
                    }
                    if (modeId == activeMode.id) {
                        return;
                    }
                }
                if (modeId != activeMode.id) {
                    RefreshRateAdjuster.DISPLAY_CONFIGS.setActiveConfig(modeId);
                    if (GlobalConfigs.isDebug()) {
                        VLog.d("RefreshRateAdjuster", String.format("setActiveConfig -> mode from %d to %d, oldFps=%d newFps=%d screenOff=%s request=%s", Integer.valueOf(RefreshRateAdjuster.DISPLAY_CONFIGS.getLastActiveModeId()), Integer.valueOf(RefreshRateAdjuster.DISPLAY_CONFIGS.getActiveModeId()), Integer.valueOf(RefreshRateAdjuster.DISPLAY_CONFIGS.getLastFps()), Integer.valueOf(RefreshRateAdjuster.DISPLAY_CONFIGS.getActiveFps()), String.valueOf(screenOff), request));
                    } else {
                        VLog.d("RefreshRateAdjuster", String.format("setActiveConfig -> mode from %d to %d, oldFps=%d newFps=%d screenOff=%s", Integer.valueOf(RefreshRateAdjuster.DISPLAY_CONFIGS.getLastActiveModeId()), Integer.valueOf(RefreshRateAdjuster.DISPLAY_CONFIGS.getActiveModeId()), Integer.valueOf(RefreshRateAdjuster.DISPLAY_CONFIGS.getLastFps()), Integer.valueOf(RefreshRateAdjuster.DISPLAY_CONFIGS.getActiveFps()), String.valueOf(screenOff)));
                    }
                } else if (GlobalConfigs.isDebug()) {
                    Object[] objArr = new Object[4];
                    objArr[0] = Integer.valueOf(modeId);
                    objArr[1] = Integer.valueOf(RefreshRateAdjuster.DISPLAY_CONFIGS.getActiveFps());
                    objArr[2] = String.valueOf(screenOff);
                    objArr[3] = request == null ? "unknown" : request.toString();
                    VLog.d("RefreshRateAdjuster", String.format("setActiveConfig -> mode is not changed, mode=%d fps=%d screenOff=%s request=%s", objArr));
                }
                if (request != null && RefreshRateAdjuster.this.mHighestPriorityRequest != request) {
                    if (RefreshRateAdjuster.this.mHighestPriorityRequest != null) {
                        RefreshRateAdjuster.this.mHighestPriorityRequest.active = false;
                    }
                    RefreshRateAdjuster.this.mHighestPriorityRequest = request;
                    RefreshRateAdjuster.this.mHighestPriorityRequest.active = true;
                }
                if (RefreshRateAdjuster.DISPLAY_CONFIGS.getDFps() != dfps) {
                    RefreshRateAdjuster.this.mAdjusterHandler.removeMessages(6);
                    RefreshRateAdjuster.this.mAdjusterHandler.sendMessageDelayed(RefreshRateAdjuster.this.mAdjusterHandler.obtainMessage(6, dfps, 0), 100L);
                }
                if (requestUpdate) {
                    RefreshRateAdjuster.this.requestSetActiveMode(false);
                    RefreshRateAdjuster.this.mResetActiveModeRequested = requsetVerify;
                } else if (requsetVerify) {
                    RefreshRateAdjuster.this.requestVerifyActiveMode(true);
                }
                GlobalConfigs.updateBbkLogStatus();
                return;
            }
            performSetDFps(0, true);
            RefreshRateAdjuster.this.reqeustResetActiveMode();
            VLog.d("RefreshRateAdjuster", String.format("Feature disabled isFeatureSupported=%s isFeatureEnabled=%s", String.valueOf(GlobalConfigs.isFeatureSupported()), String.valueOf(GlobalConfigs.isFeatureEnabled())));
        }

        private void performVsyncVerify(int fromVerify) {
            RefreshRateAdjuster.access$1408(RefreshRateAdjuster.this);
            int i = 1;
            DisplayConfigsManager.DisplayMode reqMode = RefreshRateAdjuster.DISPLAY_CONFIGS.getActiveMode();
            DisplayConfigsManager.DisplayMode activeMode = RefreshRateAdjuster.DISPLAY_CONFIGS.getExactModeByFps(reqMode.resolution, GlobalConfigs.getVsyncRate(), reqMode.configBits);
            if (activeMode == null) {
                i = 0;
                activeMode = RefreshRateAdjuster.DISPLAY_CONFIGS.getSfActiveMode();
            }
            boolean screenOff = !GlobalConfigs.isScreenOn();
            VLog.d("RefreshRateAdjuster", String.format("performVsyncVerify -> mode=%d/%d fps=%d/%d brightness=%d/%d touch=%d/%d settings=%d screenOff=%s verify=[%d %d %d]", Integer.valueOf(reqMode.id), Integer.valueOf(activeMode.id), Integer.valueOf(reqMode.fps), Integer.valueOf(activeMode.fps), Integer.valueOf(GlobalConfigs.getScreenBrightness()), Integer.valueOf(GlobalConfigs.getLowBrightnessValue()), Integer.valueOf(GlobalConfigs.getInteractionMinRefreshRate()), Integer.valueOf(GlobalConfigs.getInteractionMaxRefreshRate()), Integer.valueOf(GlobalConfigs.getUserSettingRefreshRateMode()), String.valueOf(screenOff), Integer.valueOf(RefreshRateAdjuster.this.mVsyncVerifyCount), Integer.valueOf(i), Integer.valueOf(fromVerify)));
            if (activeMode.fps == reqMode.fps) {
                RefreshRateAdjuster.this.mVsyncVerifyCount = 0;
                return;
            }
            if (i != 0) {
                RefreshRateAdjuster.this.mVsyncVerify = true;
            } else {
                RefreshRateAdjuster.this.reqeustResetActiveMode();
            }
            if (RefreshRateAdjuster.this.mVsyncVerifyCount <= 10 && !screenOff) {
                RefreshRateAdjuster.this.requestSetActiveMode();
            }
        }

        private void performSetDFps(int dfps, boolean removeMessage) {
            if (dfps != RefreshRateAdjuster.DISPLAY_CONFIGS.getDFps()) {
                RefreshRateAdjuster.DISPLAY_CONFIGS.setDFps(dfps);
                SfUtils.setDfps(dfps);
                VLog.d("RefreshRateAdjuster", "performSetDFps=" + dfps);
                if (removeMessage) {
                    RefreshRateAdjuster.this.mAdjusterHandler.removeMessages(6);
                }
            }
        }
    }

    public void dump(PrintWriter pw, String[] args) {
        int fps;
        int extra;
        if (!GlobalConfigs.isAllowDump()) {
            return;
        }
        if (!GlobalConfigs.isFeatureSupported()) {
            pw.println("Feature not Supported.");
            pw.println("->SupportedModes:" + DISPLAY_CONFIGS.getSupportedModes());
            pw.println("->Prop:" + SystemProperties.getBoolean("persist.sys.vivo.refresh_rate_adjuster", true));
        } else if (args.length >= 2 && "--debug".equals(args[0])) {
            boolean debug = Boolean.valueOf(args[1]).booleanValue();
            GlobalConfigs.setDebug(debug);
            pw.println("DEBUG=" + debug);
        } else if (args.length >= 5 && "--acquire".equals(args[0])) {
            String sceneName = args[1];
            int fps2 = Integer.parseInt(args[2]);
            int priority = Integer.parseInt(args[3]);
            int duration = Integer.parseInt(args[4]);
            if (args.length < 6) {
                extra = 1;
            } else {
                int extra2 = Integer.parseInt(args[5]);
                extra = extra2;
            }
            pw.println("acquireRefreshRate:" + acquireRefreshRate(nextHandle(), sceneName, "adb-acquire", fps2, priority, duration, 0, 0, 0, true, extra));
        } else if (args.length >= 2 && "--release".equals(args[0])) {
            long handle = Integer.parseInt(args[1]);
            releaseRefreshRate(handle);
            pw.println("releaseRefreshRate:" + handle);
        } else if (args.length >= 1 && "--window-settings".equals(args[0])) {
            String value = null;
            if (args.length >= 2) {
                value = args[1];
            }
            WindowRequestManager.getInstance().dump(pw, value);
        } else if (args.length >= 2 && "--set-fps".equals(args[0])) {
            synchronized (this) {
                if (!this.mSceneManager.hasScene(SceneManager.FIX_RATE_SCENE)) {
                    this.mSceneManager.createScene(SceneManager.FIX_RATE_SCENE, SceneManager.FIX_RATE_PRIORITY, false);
                }
                if (this.mShellFpsHandle > 0) {
                    releaseRefreshRate(this.mShellFpsHandle);
                    this.mShellFpsHandle = -1L;
                }
                int fps3 = Integer.parseInt(args[1]);
                if (fps3 > 0) {
                    fps = fps3;
                    long handle2 = acquireRefreshRate(nextHandle(), SceneManager.FIX_RATE_SCENE, "adb-set-fps", fps3, 100, -1, 0, 0, 0, true, 1);
                    if (handle2 > 0) {
                        this.mShellFpsHandle = handle2;
                    }
                } else {
                    fps = fps3;
                }
                pw.println("setFps:" + fps);
            }
        } else if (args.length >= 2 && "--set-dfps".equals(args[0])) {
            synchronized (this) {
                int dfps = Integer.parseInt(args[1]);
                if (dfps > 0) {
                    this.mShellDFps = Math.min(dfps, DISPLAY_CONFIGS.getMaxRefreshRate());
                } else {
                    this.mShellDFps = 0;
                }
                requestSetActiveMode();
                pw.println("setDfps:" + this.mShellDFps);
            }
        } else if (args.length >= 3 && "--set-resolution".equals(args[0])) {
            int width = Integer.parseInt(args[1]);
            int height = Integer.parseInt(args[2]);
            pw.println(String.format("setResolution %dx%d", Integer.valueOf(width), Integer.valueOf(height)));
            setResolution(width, height);
        } else if (args.length >= 3 && "--set-config-bits".equals(args[0])) {
            int bits = GlobalConfigs.parseInteger(args[1]);
            int mask = GlobalConfigs.parseInteger(args[2]);
            pw.println(String.format("setConfigBits bits=0x%x,mask=0x%x", Integer.valueOf(bits), Integer.valueOf(mask)));
            setConfigBits(Process.myPid(), bits, mask);
        } else if (args.length >= 2 && "--set-flags".equals(args[0])) {
            int flags = GlobalConfigs.parseInteger(args[1]);
            pw.println(String.format("setFlags 0x%x", Integer.valueOf(flags)));
            WindowRequestManager.getInstance().setFlags(flags);
        } else {
            pw.println("Version:" + this.mVersion);
            DISPLAY_CONFIGS.dump(pw, args);
            this.mSceneManager.dump(pw);
        }
    }
}