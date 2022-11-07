package com.vivo.services.rms.display;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.ArrayMap;
import com.vivo.common.utils.VLog;
import com.vivo.services.rms.ProcessInfo;
import com.vivo.services.rms.display.scene.AnimationScene;
import com.vivo.services.rms.display.scene.AppRequestScene;
import com.vivo.services.rms.display.scene.BaseScene;
import com.vivo.services.rms.display.scene.DynamicScene;
import com.vivo.services.rms.display.scene.InteractionScene;
import com.vivo.services.rms.display.scene.PowerScene;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/* loaded from: classes.dex */
public class SceneManager implements Handler.Callback {
    public static final int ANIMATION_PRIORITY = 100000;
    public static final String ANIMATION_SCENE = "Animation";
    public static final int APP_REQUEST_PRIORITY = 80000;
    public static final String APP_REQUEST_SCENE = "AppRequest";
    public static final int EXTRA_CONFIG_BITS_SHIFT = 8;
    public static final int EXTRA_DFPS_SHIFT = 16;
    public static final int EXTRA_FLAG_CONFIG_BITS = 4;
    public static final int EXTRA_FLAG_MASK = 255;
    public static final int EXTRA_FLAG_REQ_DFPS = 2;
    public static final int EXTRA_FLAG_USE_PRIVATE_FPS = 1;
    public static final int FIX_RATE_PRIORITY = 105000;
    public static final String FIX_RATE_SCENE = "FixRate";
    public static final int INTERACTION_PRIORITY = 70000;
    public static final String INTERACTION_SCENE = "Interaction";
    public static final int MAX_AJUSTER_PRIORITY = 100;
    private static final int MAX_REQUEST_PER_PROCESS = 32;
    public static final int MIN_AJUSTER_PRIORITY = -100;
    public static final int POWER_PRIORITY = 90000;
    public static final String POWER_SCENE = "Power";
    private static final String TAG = "RefreshRateAdjuster";
    public static final int UPDATE_FLAG_FOR_BITS = 4;
    public static final int UPDATE_FLAG_FOR_BRIGHTNESS = 8;
    public static final int UPDATE_FLAG_FOR_FPS = 1;
    public static final int UPDATE_FLAG_FOR_RESOLUTION = 2;
    private Looper mLooper;
    private Handler mMainHandler;
    private final ArrayMap<String, BaseScene> mScenes = new ArrayMap<>();
    private HashMap<String, RefreshRateRequest> mRequests = new HashMap<>();
    private ArrayList<String> mTmpRemovedHandles = new ArrayList<>();
    private ArrayList<RefreshRateRequest> mTmpThermalRequests = new ArrayList<>();

    public SceneManager(Looper looper) {
        synchronized (this) {
            this.mLooper = looper;
            this.mMainHandler = new Handler(looper, this);
            this.mScenes.put(POWER_SCENE, new PowerScene(this));
            this.mScenes.put(ANIMATION_SCENE, new AnimationScene(this));
            this.mScenes.put(APP_REQUEST_SCENE, new AppRequestScene(this));
            this.mScenes.put(INTERACTION_SCENE, new InteractionScene(this));
            this.mScenes.put(FIX_RATE_SCENE, new DynamicScene(this, FIX_RATE_SCENE, FIX_RATE_PRIORITY, true));
        }
    }

    public boolean acquireRefreshRate(String handle, String sceneName, String reason, int fps, int priority, ProcessInfo caller, int duration, ProcessInfo client, int states, boolean dfps, int extra) {
        synchronized (this) {
            BaseScene scene = this.mScenes.get(sceneName);
            if (scene == null) {
                VLog.e("RefreshRateAdjuster", String.format("acquireRefreshRate scene=%s reason=%s fail for scene is not found", sceneName, reason));
                return false;
            } else if (caller != null && caller.sizeOfRefreshRateHandle() > 32) {
                VLog.e("RefreshRateAdjuster", String.format("acquireRefreshRate scene=%s reason=%s fail for %s pid=%d request too many handles", sceneName, reason, caller.mProcName, Integer.valueOf(caller.mPid)));
                return false;
            } else {
                RefreshRateRequest request = scene.createRequest(handle, reason, fps, priority, caller, duration, client, states, dfps, extra);
                if (GlobalConfigs.isDebug()) {
                    VLog.d("RefreshRateAdjuster", "acquireRefreshRate->" + request);
                }
                if (request != null) {
                    this.mRequests.put(handle, request);
                    request.caller.addRefreshRateHandle(handle);
                    if (request.client != null && request.caller != request.client) {
                        request.client.addRefreshRateHandle(handle);
                    }
                    if (request.duration > 0) {
                        this.mMainHandler.sendMessageDelayed(this.mMainHandler.obtainMessage(handle.hashCode(), request), request.duration);
                    }
                    RefreshRateAdjuster.getInstance().requestSetActiveMode(scene.getPriority() != 100000);
                    return true;
                }
                return false;
            }
        }
    }

    public boolean releaseRefreshRate(String handle) {
        if (TextUtils.isEmpty(handle)) {
            return false;
        }
        synchronized (this) {
            RefreshRateRequest request = this.mRequests.get(handle);
            if (request == null) {
                return false;
            }
            this.mRequests.remove(handle);
            request.caller.removeRefreshRateHandle(handle);
            if (request.client != null && request.caller != request.client) {
                request.client.removeRefreshRateHandle(handle);
            }
            if (!request.isTimeout()) {
                this.mMainHandler.removeMessages(handle.hashCode());
            }
            request.owner.releaseRequest(request);
            RefreshRateAdjuster.getInstance().requestSetActiveMode();
            if (GlobalConfigs.isDebug()) {
                VLog.d("RefreshRateAdjuster", "releaseRefreshRate->" + request);
            }
            return true;
        }
    }

    public boolean updateRequest(int flags, int value) {
        boolean update;
        synchronized (this) {
            update = false;
            for (RefreshRateRequest request : this.mRequests.values()) {
                update |= request.update(flags, value);
            }
            if (update) {
                RefreshRateAdjuster.getInstance().requestSetActiveMode();
            }
        }
        return update;
    }

    public boolean isEmpty() {
        boolean isEmpty;
        synchronized (this) {
            isEmpty = this.mRequests.isEmpty();
        }
        return isEmpty;
    }

    public boolean hasScene(String name) {
        boolean containsKey;
        synchronized (this) {
            containsKey = this.mScenes.containsKey(name);
        }
        return containsKey;
    }

    public boolean createScene(String name, int priority, boolean powerFirst) {
        synchronized (this) {
            BaseScene baseScene = this.mScenes.get(name);
            if (baseScene != null) {
                if (baseScene.getPriority() != priority) {
                    VLog.e("RefreshRateAdjuster", String.format("createScene %s fail, scene is already exist", name));
                    return false;
                }
                return true;
            }
            for (BaseScene scene : this.mScenes.values()) {
                if (intersect(scene.getPriority() - 100, scene.getPriority() + 100, priority - 100, priority + 100)) {
                    VLog.e("RefreshRateAdjuster", String.format("createScene %s fail, priority intersect with %s", name, scene.getName()));
                    return false;
                }
            }
            this.mScenes.put(name, new DynamicScene(this, name, priority, powerFirst));
            if (GlobalConfigs.isDebug()) {
                VLog.e("RefreshRateAdjuster", "createScene " + name);
            }
            return true;
        }
    }

    public RefreshRateRequest chooseHighestPriorityRequest() {
        synchronized (this) {
            int size = this.mRequests.size();
            if (size == 0) {
                return null;
            }
            long now = SystemClock.uptimeMillis();
            RefreshRateRequest highestPriorityRequest = null;
            for (RefreshRateRequest current : this.mRequests.values()) {
                BaseScene owner = current.owner;
                if (owner.needRemove(current)) {
                    this.mTmpRemovedHandles.add(current.handle);
                } else if (owner.isEnable() && owner.isValid(current) && !current.isTimeout(now)) {
                    if (current.isThermal()) {
                        this.mTmpThermalRequests.add(current);
                    } else {
                        if (highestPriorityRequest != null && highestPriorityRequest.priority >= current.priority) {
                            if (highestPriorityRequest.priority <= current.priority) {
                                if (highestPriorityRequest.priority == current.priority) {
                                    if (!highestPriorityRequest.hasFocus()) {
                                        if (current.hasFocus()) {
                                            highestPriorityRequest = current;
                                        } else if ((current.owner.isPowerFirst() || highestPriorityRequest.owner.isPowerFirst()) && current.fps < highestPriorityRequest.fps) {
                                            highestPriorityRequest = current;
                                        } else if (current.fps > highestPriorityRequest.fps) {
                                            highestPriorityRequest = current;
                                        }
                                    }
                                }
                            }
                        }
                        highestPriorityRequest = current;
                    }
                }
            }
            if (!this.mTmpThermalRequests.isEmpty()) {
                if (highestPriorityRequest == null || highestPriorityRequest.priority < PowerScene.thermalPriority()) {
                    RefreshRateRequest thermal = highestPriorityRequest;
                    Iterator<RefreshRateRequest> it = this.mTmpThermalRequests.iterator();
                    while (it.hasNext()) {
                        RefreshRateRequest current2 = it.next();
                        if (thermal == null || thermal.fps > current2.fps) {
                            thermal = current2;
                        }
                    }
                    highestPriorityRequest = thermal;
                }
                this.mTmpThermalRequests.clear();
            }
            if (!this.mTmpRemovedHandles.isEmpty()) {
                Iterator<String> it2 = this.mTmpRemovedHandles.iterator();
                while (it2.hasNext()) {
                    String handle = it2.next();
                    releaseRefreshRate(handle);
                }
                this.mTmpRemovedHandles.clear();
            }
            return highestPriorityRequest;
        }
    }

    public void setConfigs(Bundle bundle) {
        if (bundle == null) {
            return;
        }
        synchronized (this) {
            boolean changed = false;
            for (String sceneName : bundle.keySet()) {
                BaseScene scene = this.mScenes.get(sceneName);
                Bundle sceneConfig = bundle.getBundle(sceneName);
                if (scene != null && sceneConfig != null) {
                    boolean enable = sceneConfig.getBoolean(GlobalConfigs.KEY_SCENE_ENABLE, true);
                    if (enable != scene.isEnable()) {
                        scene.setEnable(enable);
                        changed = true;
                    }
                    scene.setDisabledReasons(sceneConfig.getStringArrayList(GlobalConfigs.KEY_SCENE_DISABLE_REASONS));
                }
            }
            if (changed) {
                RefreshRateAdjuster.getInstance().requestSetActiveMode();
            }
        }
    }

    public void dump(PrintWriter pw) {
        synchronized (this) {
            pw.println("Scenes:");
            for (BaseScene scene : this.mScenes.values()) {
                pw.print('\t');
                pw.println(scene.toString());
            }
            pw.println("Requests:");
            RefreshRateRequest activeRequest = RefreshRateAdjuster.getInstance().getHighestPriorityRequest();
            if (activeRequest != null) {
                pw.print('\t');
                pw.println(activeRequest);
            }
            for (String key : this.mRequests.keySet()) {
                RefreshRateRequest request = this.mRequests.get(key);
                if (request != activeRequest) {
                    pw.print('\t');
                    pw.println(this.mRequests.get(key));
                }
            }
        }
    }

    public Looper getLooper() {
        return this.mLooper;
    }

    public Handler getMainHandler() {
        return this.mMainHandler;
    }

    private boolean intersect(int min1, int max1, int min2, int max2) {
        if (max1 < min2 || min1 > max2) {
            return false;
        }
        return true;
    }

    @Override // android.os.Handler.Callback
    public boolean handleMessage(Message msg) {
        RefreshRateRequest request = (RefreshRateRequest) msg.obj;
        releaseRefreshRate(request.handle);
        return true;
    }
}