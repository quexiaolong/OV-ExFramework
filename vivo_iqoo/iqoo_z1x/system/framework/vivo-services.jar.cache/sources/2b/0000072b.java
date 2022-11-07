package com.vivo.services.rms.display.scene;

import android.os.SystemClock;
import com.vivo.common.utils.VLog;
import com.vivo.services.rms.ProcessInfo;
import com.vivo.services.rms.appmng.AppManager;
import com.vivo.services.rms.display.DisplayConfigsManager;
import com.vivo.services.rms.display.GlobalConfigs;
import com.vivo.services.rms.display.RefreshRateRequest;
import com.vivo.services.rms.display.SceneManager;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

/* loaded from: classes.dex */
public abstract class BaseScene {
    static final DisplayConfigsManager DISPLAY_CONFIGS = DisplayConfigsManager.getInstance();
    private static final int FGACTIVITY_STATE_EPSILON_TIME = 3000;
    public static final String TAG = "RefreshRateAdjuster";
    protected final HashSet<String> mDisabledReasons;
    protected boolean mEnable;
    protected String mName;
    protected boolean mPowerFirst;
    protected int mPriority;
    protected final SceneManager mSceneManager;

    public BaseScene(SceneManager mng, String name, int priority) {
        this(mng, name, priority, false);
    }

    public BaseScene(SceneManager mng, String name, int priority, boolean powerFirst) {
        this.mEnable = true;
        this.mPowerFirst = false;
        this.mDisabledReasons = new HashSet<>();
        this.mSceneManager = mng;
        this.mName = name;
        this.mPriority = priority;
        this.mPowerFirst = powerFirst;
    }

    public String getName() {
        return this.mName;
    }

    public int getPriority() {
        return this.mPriority;
    }

    public boolean isPowerFirst() {
        return this.mPowerFirst;
    }

    public void setDisabledReasons(Collection<? extends String> c) {
        this.mDisabledReasons.clear();
        if (c != null) {
            this.mDisabledReasons.addAll(c);
        }
    }

    public boolean isEnable() {
        return this.mEnable;
    }

    public void setEnable(boolean enable) {
        this.mEnable = enable;
    }

    public SceneManager getSceneManager() {
        return this.mSceneManager;
    }

    public RefreshRateRequest createRequest(String handle, String reason, int reqFps, int priority, ProcessInfo caller, int duration, ProcessInfo client, int states, boolean dfps, int extra) {
        if (isAllowed(reason)) {
            RefreshRateRequest request = new RefreshRateRequest();
            request.handle = handle;
            request.reason = reason;
            request.owner = this;
            request.caller = caller;
            request.dfps = dfps;
            request.client = client;
            request.reqStates = states & 65535;
            request.extra = extra;
            request.usePrivateFps = (extra & 1) != 0;
            request.priority = getPriority(priority);
            request.duration = getDuration(request, duration);
            request.reqFps = GlobalConfigs.clipFps(reqFps);
            request.fps = getRefreshRate(request);
            request.createTime = SystemClock.uptimeMillis();
            if ((extra & 4) != 0) {
                request.reqConfigBits = (extra >> 8) & 255;
            }
            if ((extra & 2) != 0) {
                request.reqDFps = (extra >> 16) & 255;
            }
            request.resolution = DISPLAY_CONFIGS.getResolution();
            request.configBits = getConfigBits(request, DISPLAY_CONFIGS.getConfigsBits());
            request.mode = DISPLAY_CONFIGS.findDisplayMode(request.resolution, request.fps, request.configBits, request.usePrivateFps);
            if (request.mode != null && client != null && request.reqStates != 0) {
                request.registerStateChangeListener();
            }
            if (request.mode != null) {
                return request;
            }
            return null;
        }
        return null;
    }

    public int getConfigBits(RefreshRateRequest request, int globalBits) {
        int reqBits = request.reqConfigBits;
        if (!GlobalConfigs.isConfigBitsAllowed(request.reqConfigBits)) {
            reqBits = 0;
        }
        int configsBits = globalBits | reqBits;
        if (!GlobalConfigs.isConfigBitsAllowed(configsBits)) {
            return reqBits;
        }
        return configsBits;
    }

    public void releaseRequest(RefreshRateRequest request) {
        if (request.client != null) {
            request.unregisterStateChangeListener();
        }
    }

    private boolean isAllowed(String reason) {
        if (reason == null) {
            return false;
        }
        if (this.mDisabledReasons.isEmpty()) {
            return true;
        }
        Iterator<String> it = this.mDisabledReasons.iterator();
        while (it.hasNext()) {
            String key = it.next();
            if (reason.startsWith(key)) {
                return false;
            }
        }
        return true;
    }

    public boolean updateRequest(RefreshRateRequest request, int flags, int value) {
        int configBits;
        int resolution = request.resolution;
        int fps = getRefreshRate(request);
        int i = request.configBits;
        if ((flags & 2) != 0) {
            resolution = value;
        }
        if ((flags & 4) != 0) {
            configBits = getConfigBits(request, value);
        } else {
            configBits = getConfigBits(request, DISPLAY_CONFIGS.getConfigsBits());
        }
        if (resolution != request.resolution || fps != request.fps || configBits != request.configBits) {
            DisplayConfigsManager.DisplayMode mode = DISPLAY_CONFIGS.findDisplayMode(resolution, fps, configBits, request.usePrivateFps);
            if (mode == null) {
                VLog.e("RefreshRateAdjuster", String.format("Can't find mode, scene=%s reason=%s resolution=%dx%d fps=%d configBits=0x%x", this.mName, request.reason, Integer.valueOf(DisplayConfigsManager.DisplayMode.toWidth(resolution)), Integer.valueOf(DisplayConfigsManager.DisplayMode.toHeight(resolution)), Integer.valueOf(fps), Integer.valueOf(configBits)));
                return false;
            }
            request.resolution = resolution;
            request.fps = fps;
            request.configBits = configBits;
            if (request.mode != mode) {
                request.mode = mode;
                return true;
            }
        }
        return false;
    }

    public boolean needRemove(RefreshRateRequest request) {
        if (request.caller == null || request.caller.isAlive()) {
            return (request.client == null || request.client.isAlive()) ? false : true;
        }
        return true;
    }

    public boolean isValid(RefreshRateRequest request) {
        if (request.client == null || request.reqStates == 0) {
            return true;
        }
        if (request.client.hasStates(request.reqStates)) {
            return ((request.reqStates & 1) == 0 || request.client.getInvisibleTime() < 3000) && AppManager.getInstance().inBuildInDisplay(request.client);
        }
        return false;
    }

    protected boolean update(RefreshRateRequest request) {
        return false;
    }

    protected int getDuration(RefreshRateRequest request, int duration) {
        return duration;
    }

    protected int getRefreshRate(RefreshRateRequest request) {
        return request.reqFps;
    }

    protected int getPriority(int priority) {
        if (priority > 100) {
            priority = 100;
        } else if (priority < -100) {
            priority = -100;
        }
        return this.mPriority + priority;
    }

    public String toString() {
        return String.format("%s priority=%d powerFirst=%s enable=%s disabledReasons=%s", this.mName, Integer.valueOf(this.mPriority), String.valueOf(this.mPowerFirst), String.valueOf(this.mEnable), this.mDisabledReasons);
    }
}