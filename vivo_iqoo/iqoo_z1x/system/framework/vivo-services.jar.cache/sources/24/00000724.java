package com.vivo.services.rms.display;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Pair;
import android.view.WindowManager;
import com.android.server.wm.WindowManagerInternal;
import com.android.server.wm.WindowState;
import com.vivo.common.utils.VLog;
import com.vivo.services.rms.Platform;
import com.vivo.services.rms.ProcessInfo;
import com.vivo.services.rms.RMWms;
import com.vivo.services.rms.RmsInjectorImpl;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/* loaded from: classes.dex */
public class WindowRequestManager extends WindowManagerInternal.AppTransitionListener implements Handler.Callback {
    private static final String ANIMATION_TITLE = "_animation_";
    public static final int FLAG_ACTIVITY_SWITCH_HIGH_RATE = 4;
    public static final int FLAG_ANIMATION_HIGH_RATE = 2;
    public static final int FLAG_FOCUS_FIRST = 1;
    public static final int FLAG_LAYOUT_PARAM_ALWAYS = 8;
    public static final int FLAG_XML_SETTING_ALWAYS = 16;
    private static final String INPUT_METHOD = "InputMethod";
    private static final String KEY_BRIGHTNESS = "key_bringhtness";
    private static final String KEY_DFPS = "key_dfps";
    private static final String KEY_PENDING = "key_pending";
    private static final String KEY_REQ_CONFIG_BITS = "key_req_config_bits";
    private static final String KEY_REQ_FPS = "key_req_fps";
    private static final String KEY_REQ_PRIORITY = "key_req_priority";
    private static final String KEY_TOUCH_MAX = "key_touch_max";
    private static final String KEY_TOUCH_MIN = "key_touch_min";
    private static final String KEY_VERSION = "key_version";
    private static final String KEY_XML_SETTINGS = "key_xml_settings";
    private static final int MIN_WINDOW_SIZE = 200;
    private static final int MSG_APPLY_REQUSET = 1;
    private static final int MSG_RELEASE_ANIMATION = 2;
    private static final String REMOTE_ANIMATION_NAME = "RemoteAnimation";
    private static final Object SETTINGS_LOCK = new Object();
    private static final String TAG = "RefreshRateAdjuster";
    private static final String TOAST_NAME = "Toast";
    private static final int UPDATE_REQUEST_DELAYED_MILLIS = 20;
    private ArrayList<SettingItem> mAnimationSettings;
    private final HashMap<String, ArrayList<SettingItem>> mBrightnessXmlSettings;
    private ArrayList<SettingItem> mCommonSettings;
    private int mFlags;
    private WindowState mFocusWindow;
    private Handler mHander;
    private boolean mIsLandscape;
    private final HashMap<String, ArrayList<SettingItem>> mPackageFpsXmlSettings;
    private int mVersion;
    private final HashMap<String, ArrayList<SettingItem>> mWindowFpsXmlSettings;
    private WindowRequest mWindowRequest;
    private final HashMap<WindowState, ProcessInfo> mWindows;
    private Pair<SettingItem, String> mWindowsBrightness;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Instance {
        private static final WindowRequestManager INSTANCE = new WindowRequestManager();

        private Instance() {
        }
    }

    private WindowRequestManager() {
        this.mWindows = new HashMap<>();
        this.mPackageFpsXmlSettings = new HashMap<>();
        this.mWindowFpsXmlSettings = new HashMap<>();
        this.mBrightnessXmlSettings = new HashMap<>();
        this.mCommonSettings = new ArrayList<>();
        this.mAnimationSettings = new ArrayList<>();
        this.mVersion = -1;
        this.mFlags = 0;
        this.mHander = new Handler(RmsInjectorImpl.getInstance().getLooper(), this);
        if (Platform.isMiddlePerfDevice()) {
            this.mFlags = 7;
        } else {
            this.mFlags = 13;
        }
        RMWms.getInstance().registerAppTransitionListener(this);
    }

    public static WindowRequestManager getInstance() {
        return Instance.INSTANCE;
    }

    public void applyWindowRefreshRate(int displayId) {
        if (GlobalConfigs.isFeatureSupported() && DisplayConfigsManager.getInstance().isMultiMode() && displayId == 0 && !this.mHander.hasMessages(1)) {
            this.mHander.sendEmptyMessageDelayed(1, 20L);
        }
    }

    public void updateRequest() {
        WindowRequest request = this.mWindowRequest;
        int mode = GlobalConfigs.getUserSettingRefreshRateMode();
        if (request != null && request.mode != mode) {
            applyWindowRefreshRate(0);
        }
    }

    public void updateFocus(int displayId, WindowState newFocus, WindowState oldFocus) {
        if (displayId != 0) {
            return;
        }
        synchronized (this) {
            this.mFocusWindow = newFocus;
            if (newFocus != null) {
                int rotation = RMWms.getInstance().getRotation(newFocus);
                boolean z = true;
                if (rotation != 3 && rotation != 1) {
                    z = false;
                }
                this.mIsLandscape = z;
            }
        }
    }

    public void addWindow(ProcessInfo pi, WindowState win) {
        synchronized (this) {
            if (!this.mWindows.containsKey(win)) {
                this.mWindows.put(win, pi);
            }
        }
    }

    public void removeWindow(ProcessInfo pi, WindowState win) {
        synchronized (this) {
            if (this.mWindows.containsKey(win)) {
                this.mWindows.remove(win);
            }
        }
    }

    private boolean filterWindows(HashMap<WindowState, ProcessInfo> windows) {
        Iterator<Map.Entry<WindowState, ProcessInfo>> it = windows.entrySet().iterator();
        boolean hasFocus = false;
        while (it.hasNext()) {
            Map.Entry<WindowState, ProcessInfo> entry = it.next();
            ProcessInfo pi = entry.getValue();
            if (pi == null || !pi.isAlive()) {
                it.remove();
            } else {
                WindowState win = entry.getKey();
                if (pi.hasFocus()) {
                    hasFocus = true;
                }
                int displayId = entry.getKey().getDisplayId();
                int width = RMWms.getInstance().getWidth(win);
                int height = RMWms.getInstance().getHeight(win);
                WindowManager.LayoutParams lp = RMWms.getInstance().getAttrs(win);
                String title = lp.getTitle().toString();
                if (lp == null || width < 200 || height < 200 || TOAST_NAME.equals(title) || INPUT_METHOD.equals(title) || displayId != 0) {
                    it.remove();
                }
            }
        }
        return hasFocus;
    }

    private WindowRequest chooseBestWindowsRequest(HashMap<WindowState, ProcessInfo> windows, WindowState focusWindow, int flags, boolean skipAnimation) {
        WindowRequest bestRequest = null;
        ProcessInfo bestPi = null;
        boolean focusFirst = (flags & 1) != 0;
        for (WindowState win : windows.keySet()) {
            ProcessInfo pi = windows.get(win);
            WindowRequest request = getWindowRequest(win, pi, flags);
            if (request != null) {
                if (bestRequest == null) {
                    bestRequest = request;
                    bestPi = pi;
                } else if (bestRequest.reqPriority <= request.reqPriority) {
                    if (bestRequest.reqPriority < request.reqPriority) {
                        bestRequest = request;
                        bestPi = pi;
                    } else {
                        if (focusFirst) {
                            if (!bestPi.hasFocus()) {
                                if (pi.hasFocus()) {
                                    bestRequest = request;
                                    bestPi = pi;
                                }
                            }
                        }
                        if (bestRequest.area < request.area) {
                            bestRequest = request;
                            bestPi = pi;
                        } else if (bestRequest.getFps() > request.getFps()) {
                            bestRequest = request;
                            bestPi = pi;
                        }
                    }
                }
            }
        }
        if (bestRequest != null && skipAnimation && RMWms.getInstance().isAnimating(bestRequest.owner)) {
            return null;
        }
        return bestRequest;
    }

    private Pair<SettingItem, String> chooseBestWindowsBringhtness(HashMap<WindowState, ProcessInfo> windows) {
        Pair<SettingItem, String> bestRequest = null;
        for (WindowState win : windows.keySet()) {
            ProcessInfo pi = windows.get(win);
            Pair<SettingItem, String> request = getWindowBringhtness(win, pi);
            if (request != null) {
                if (bestRequest == null) {
                    bestRequest = request;
                } else if (((SettingItem) bestRequest.first).brightness < ((SettingItem) request.first).brightness) {
                    bestRequest = request;
                }
            }
        }
        return bestRequest;
    }

    private WindowRequest getWindowRequest(WindowState win, ProcessInfo pi, int flags) {
        Pair<SettingItem, String> xmlSettings;
        int preferredRefreshRate;
        WindowManager.LayoutParams lp = RMWms.getInstance().getAttrs(win);
        String title = getTitle(lp);
        boolean fgActivity = pi.isFgActivity();
        int mode = GlobalConfigs.getUserSettingRefreshRateMode();
        boolean focusFirst = (flags & 1) != 0 && pi.hasFocus();
        if (fgActivity || focusFirst || (flags & 16) != 0) {
            Pair<SettingItem, String> xmlSettings2 = getSettingItem(String.valueOf(mode), pi.mPkgName, title, false);
            xmlSettings = xmlSettings2;
        } else {
            xmlSettings = null;
        }
        if (fgActivity || focusFirst || (flags & 8) != 0) {
            int preferredRefreshRate2 = getPreferredRefreshRate(win, lp, pi.isSystemApp());
            preferredRefreshRate = preferredRefreshRate2;
        } else {
            preferredRefreshRate = 0;
        }
        if (xmlSettings == null && preferredRefreshRate == 0) {
            return null;
        }
        WindowRequest result = new WindowRequest(win, title, xmlSettings, pi, mode, preferredRefreshRate);
        return result;
    }

    private Pair<SettingItem, String> getWindowBringhtness(WindowState win, ProcessInfo pi) {
        Pair<SettingItem, String> result = getSettingItem(String.valueOf(GlobalConfigs.getUserSettingRefreshRateMode()), pi.mPkgName, getTitle(RMWms.getInstance().getAttrs(win)), true);
        if (result == null || ((SettingItem) result.first).brightness < 0) {
            return null;
        }
        return result;
    }

    private Pair<SettingItem, String> getSettingItem(String mode, String pkgName, String title, boolean brightness) {
        SettingItem item;
        SettingItem item2;
        ArrayList<SettingItem> matchedList;
        SettingItem item3;
        synchronized (SETTINGS_LOCK) {
            HashMap<String, ArrayList<SettingItem>> container = brightness ? this.mBrightnessXmlSettings : this.mPackageFpsXmlSettings;
            if (!brightness && (matchedList = this.mWindowFpsXmlSettings.get(title)) != null && (item3 = getItemLocked(matchedList, mode)) != null && item3.isValid(brightness)) {
                return Pair.create(item3, title);
            }
            ArrayList<SettingItem> matchedList2 = container.get(pkgName);
            if (matchedList2 != null) {
                SettingItem item4 = getItemLocked(matchedList2, mode);
                if (item4 != null && item4.isValid(brightness)) {
                    return Pair.create(item4, pkgName);
                }
            } else {
                Iterator<Map.Entry<String, ArrayList<SettingItem>>> iterator = container.entrySet().iterator();
                while (true) {
                    if (!iterator.hasNext()) {
                        break;
                    }
                    Map.Entry<String, ArrayList<SettingItem>> entry = iterator.next();
                    if (pkgName.startsWith(entry.getKey())) {
                        matchedList2 = entry.getValue();
                        break;
                    }
                }
                if (matchedList2 != null && (item = getItemLocked(matchedList2, mode)) != null && item.isValid(brightness)) {
                    return Pair.create(item, pkgName);
                }
            }
            if (this.mCommonSettings != null && (item2 = getItemLocked(this.mCommonSettings, mode)) != null && item2.isValid(brightness)) {
                return Pair.create(item2, pkgName);
            }
            return null;
        }
    }

    public int getPreferredRefreshRate(WindowState win, WindowManager.LayoutParams lp, boolean isSystemApp) {
        int preferredModeId = RMWms.getInstance().getPreferredModeId(win);
        int refreshRate = (int) lp.preferredRefreshRate;
        int fps = 0;
        if (preferredModeId != 0) {
            fps = DisplayConfigsManager.getInstance().getFpsByAndroidModeId(preferredModeId);
        }
        if (fps == 0 && refreshRate != 0 && DisplayConfigsManager.getInstance().isRefreshRateSupported(refreshRate)) {
            fps = refreshRate;
        }
        if (fps > 0 && fps < 60 && !isSystemApp && DisplayConfigsManager.getInstance().isRefreshRateSupported(fps)) {
            fps = 60;
        }
        if (fps != 0 && GlobalConfigs.isPrivateFps(fps)) {
            return DisplayConfigsManager.getInstance().adjustPrivateFps(fps);
        }
        return fps;
    }

    public int getPreferredRefreshRate(WindowState win, boolean isSystemApp) {
        return getPreferredRefreshRate(win, RMWms.getInstance().getAttrs(win), isSystemApp);
    }

    public long acquireRefreshRate(String sceneName, String reason, int fps, int priority, int duration, int forWho) {
        if (DisplayConfigsManager.getInstance().isMultiMode()) {
            if (this.mIsLandscape && REMOTE_ANIMATION_NAME.equals(reason)) {
                return -1L;
            }
            return RefreshRateAdjuster.getInstance().acquireRefreshRate(sceneName, reason, fps, priority, duration, 0, forWho, 0, false);
        }
        return -1L;
    }

    public void releaseRefreshRate(long handle) {
        if (handle <= 0) {
            return;
        }
        int delayed = GlobalConfigs.animationReleaseDelayedTime();
        if (delayed <= 0) {
            RefreshRateAdjuster.getInstance().releaseRefreshRate(handle);
            return;
        }
        Handler handler = this.mHander;
        handler.sendMessageDelayed(handler.obtainMessage(2, 0, 0, Long.valueOf(handle)), delayed);
    }

    public int onAppTransitionStartingLocked(int transit, long duration, long statusBarAnimationStartTime, long statusBarAnimationDuration) {
        String reason;
        if (duration > 0 && isActivitySwitchHightRate() && GlobalConfigs.isFeatureSupported() && DisplayConfigsManager.getInstance().isMultiMode() && (reason = transitName(transit)) != null) {
            RefreshRateAdjuster.getInstance().acquireRefreshRate(SceneManager.ANIMATION_SCENE, reason, 0, 0, (int) (GlobalConfigs.animationReleaseDelayedTime() + duration), 0, 0, 0, false);
            if (GlobalConfigs.isDebug()) {
                VLog.d("RefreshRateAdjuster", "onAppTransition transit=" + reason + " duration = " + duration);
            }
        }
        return 0;
    }

    private String transitName(int transit) {
        if (transit != 18) {
            if (transit != 24) {
                if (transit != 25) {
                    switch (transit) {
                        case 6:
                            return "activity_open";
                        case 7:
                            return "activity_close";
                        case 8:
                            return "task_open";
                        case 9:
                            return "task_close";
                        case 10:
                            return "task_to_front";
                        case 11:
                            return "task_to_back";
                        case 12:
                            return "wallpaper_close";
                        case 13:
                            return "wallpaper_open";
                        case 14:
                            return "wallpaper_intra_open";
                        case 15:
                            return "wallpaper_intra_close";
                        case 16:
                            return "task_open_behind";
                        default:
                            return null;
                    }
                }
                return "translucent_activity_close";
            }
            return "translucent_activity_open";
        }
        return "activity_relaunch";
    }

    public void applyRequest() {
        HashMap<WindowState, ProcessInfo> windows;
        WindowState focusWindow;
        int flags;
        WindowRequest request;
        synchronized (this) {
            windows = new HashMap<>(this.mWindows);
            focusWindow = this.mFocusWindow;
            flags = this.mFlags;
        }
        if (focusWindow == null) {
            return;
        }
        WindowRequest currentRequest = this.mWindowRequest;
        boolean skipFps = false;
        int mode = GlobalConfigs.getUserSettingRefreshRateMode();
        if (currentRequest != null && currentRequest.owner == focusWindow && currentRequest.mode == mode) {
            int curPreferredFps = getPreferredRefreshRate(currentRequest.owner, currentRequest.client.isSystemApp());
            int lastPreferredFps = currentRequest.reqPriority == 10 ? currentRequest.reqFps : 0;
            if (curPreferredFps == lastPreferredFps) {
                skipFps = true;
            }
        }
        if (skipFps && !GlobalConfigs.isUseBrightness()) {
            return;
        }
        boolean hasFocus = filterWindows(windows);
        if (!skipFps) {
            if (hasFocus) {
                request = chooseBestWindowsRequest(windows, focusWindow, flags, currentRequest == null || currentRequest.reqFps <= 0);
            } else {
                request = null;
            }
            if (!Objects.equals(currentRequest, request)) {
                RefreshRateAdjuster.getInstance().resetRequestTime();
                this.mWindowRequest = request;
                if (currentRequest != null) {
                    currentRequest.cancel();
                }
                if (request != null) {
                    request.apply();
                }
                RefreshRateAdjuster.getInstance().requestSetActiveMode();
            }
        }
        if (GlobalConfigs.isUseBrightness()) {
            Pair<SettingItem, String> bringhtness = chooseBestWindowsBringhtness(windows);
            if (!Objects.equals(this.mWindowsBrightness, bringhtness)) {
                this.mWindowsBrightness = bringhtness;
                GlobalConfigs.setWindowBrightnessValue(bringhtness != null ? ((SettingItem) bringhtness.first).brightness : 0);
            }
        }
    }

    @Override // android.os.Handler.Callback
    public boolean handleMessage(Message msg) {
        if (msg.what == 1) {
            applyRequest();
        } else if (msg.what == 2 && msg.obj != null) {
            RefreshRateAdjuster.getInstance().releaseRefreshRate(((Long) msg.obj).longValue());
        }
        return true;
    }

    public void setBundle(Bundle bundle) {
        if (bundle == null) {
            return;
        }
        setXmlSettings(bundle);
    }

    public void setFlags(int flags) {
        this.mFlags = flags;
    }

    /* JADX WARN: Type inference failed for: r5v4, types: [int, boolean] */
    /* JADX WARN: Type inference failed for: r5v6 */
    /* JADX WARN: Type inference failed for: r5v7 */
    private void setXmlSettings(Bundle bundle) {
        Bundle settings = bundle.getBundle(KEY_XML_SETTINGS);
        if (settings == null) {
            return;
        }
        boolean z = false;
        boolean isPending = bundle.getBoolean(KEY_PENDING, false);
        int version = bundle.getInt("key_version", 0);
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter('|');
        synchronized (SETTINGS_LOCK) {
            if (this.mVersion <= version && (isPending || this.mVersion != version)) {
                if (!isPending) {
                    this.mPackageFpsXmlSettings.clear();
                    this.mCommonSettings.clear();
                    this.mAnimationSettings.clear();
                }
                this.mVersion = version;
                try {
                    for (String titles : settings.keySet()) {
                        Bundle modes = settings.getBundle(titles);
                        if (modes != null) {
                            ?? r5 = z;
                            for (String mode : modes.keySet()) {
                                Bundle item = modes.getBundle(mode);
                                if (item != null && mode != null && GlobalConfigs.isModeSupported(mode)) {
                                    SettingItem si = new SettingItem(mode, item.getInt(KEY_REQ_FPS, -1), item.getInt(KEY_REQ_PRIORITY, r5), item.getInt(KEY_TOUCH_MIN, -1), item.getInt(KEY_TOUCH_MAX, -1), item.getBoolean(KEY_DFPS, r5), item.getInt(KEY_BRIGHTNESS, -1), item.getInt(KEY_REQ_CONFIG_BITS, r5));
                                    splitter.setString(titles);
                                    Iterator<String> it = splitter.iterator();
                                    while (it.hasNext()) {
                                        String title = it.next();
                                        addSettingItemLocked(title.trim(), si);
                                    }
                                }
                                r5 = 0;
                            }
                            z = false;
                        }
                    }
                } catch (NumberFormatException e) {
                    VLog.e("RefreshRateAdjuster", "Parse fail" + e.toString());
                }
            }
        }
    }

    private SettingItem getItemLocked(ArrayList<SettingItem> container, String mode) {
        if (container == null) {
            return null;
        }
        SettingItem first = null;
        SettingItem second = null;
        Iterator<SettingItem> it = container.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            SettingItem r = it.next();
            if (r.mode.equals(mode)) {
                first = r;
                break;
            } else if (r.mode.equals("*")) {
                second = r;
            }
        }
        return first != null ? first : second;
    }

    private void addItemLocked(ArrayList<SettingItem> container, SettingItem item) {
        Iterator<SettingItem> it = container.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            } else if (it.next().mode.equals(item.mode)) {
                it.remove();
                break;
            }
        }
        container.add(item);
    }

    private void addSettingItemLocked(String title, SettingItem item) {
        ArrayList<SettingItem> container;
        if (title.isEmpty()) {
            return;
        }
        if (title.equals("*")) {
            container = this.mCommonSettings;
        } else if (title.equals(ANIMATION_TITLE)) {
            container = this.mAnimationSettings;
        } else if (title.indexOf("/") != -1) {
            container = this.mWindowFpsXmlSettings.get(title);
            if (container == null) {
                container = new ArrayList<>();
                this.mWindowFpsXmlSettings.put(title, container);
            }
        } else {
            container = this.mPackageFpsXmlSettings.get(title);
            if (container == null) {
                container = new ArrayList<>();
                this.mPackageFpsXmlSettings.put(title, container);
            }
        }
        if (item.brightness > 0) {
            ArrayList<SettingItem> list = this.mBrightnessXmlSettings.get(title);
            if (list == null) {
                list = new ArrayList<>();
                this.mBrightnessXmlSettings.put(title, container);
            }
            addItemLocked(list, item);
        }
        addItemLocked(container, item);
    }

    public boolean isAnimationHighRate() {
        return (this.mFlags & 2) != 0;
    }

    public boolean isActivitySwitchHightRate() {
        return (this.mFlags & 4) != 0;
    }

    public int getAnimationRefreshRate() {
        int fps = GlobalConfigs.getUserSettingMaxRefreshRate();
        SettingItem item = null;
        synchronized (SETTINGS_LOCK) {
            if (!this.mAnimationSettings.isEmpty()) {
                item = getItemLocked(this.mAnimationSettings, String.valueOf(GlobalConfigs.getUserSettingRefreshRateMode()));
            }
        }
        if (item != null && item.reqFps > 0) {
            fps = item.reqFps;
        }
        return Math.max(fps, 60);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class SettingItem {
        public int brightness;
        public int configBits;
        public boolean dfps;
        public String mode;
        public int reqFps;
        public int reqPriority;
        public int touchMaxFps;
        public int touchMinFps;

        public SettingItem(String inMode, int inReqFps, int inReqPriority, int inTouchMinFps, int inTouchMaxFps, boolean inDfps, int InBrightness, int bits) {
            this.reqFps = -1;
            this.reqPriority = 0;
            this.touchMinFps = -1;
            this.touchMaxFps = -1;
            this.dfps = false;
            this.brightness = -1;
            this.configBits = 0;
            this.mode = inMode;
            this.reqFps = DisplayConfigsManager.getInstance().toRealFps(inReqFps);
            this.reqPriority = inReqPriority;
            this.touchMinFps = DisplayConfigsManager.getInstance().toRealFps(inTouchMinFps);
            this.touchMaxFps = DisplayConfigsManager.getInstance().toRealFps(inTouchMaxFps);
            this.dfps = inDfps;
            this.brightness = InBrightness;
            this.configBits = bits;
        }

        public boolean isValid(boolean isBrightness) {
            return !isBrightness || this.brightness >= 0;
        }

        public String toString() {
            return String.format("%s->reqFps=%d touchMinFps=%d touchMaxFps=%d  priority=%d configBits=0x%x dfps=%s brightness=%d", this.mode, Integer.valueOf(this.reqFps), Integer.valueOf(this.touchMinFps), Integer.valueOf(this.touchMaxFps), Integer.valueOf(this.reqPriority), Integer.valueOf(this.configBits), String.valueOf(this.dfps), Integer.valueOf(this.brightness));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class WindowRequest {
        public int area;
        public ProcessInfo client;
        public int configBits;
        public boolean dfps;
        public int mode;
        public WindowState owner;
        public int reqFps;
        public int reqPriority;
        public String reqReason;
        public int touchMaxFps;
        public int touchMinFps;
        public String touchReason;
        public String winTitle;
        private long reqFpsHandle = -1;
        private long touchMinHandle = -1;

        public WindowRequest(WindowState win, String inWinTitle, Pair<SettingItem, String> xmlSettings, ProcessInfo pi, int inMode, int fps) {
            this.reqFps = -1;
            this.reqPriority = -1;
            this.configBits = 0;
            this.touchMinFps = -1;
            this.touchMaxFps = -1;
            this.dfps = false;
            this.area = 0;
            this.owner = win;
            this.winTitle = inWinTitle;
            this.client = pi;
            this.mode = inMode;
            this.area = RMWms.getInstance().getWidth(win) * RMWms.getInstance().getWidth(win);
            boolean fpsInited = false;
            if (fps > 0) {
                this.reqFps = fps;
                this.reqPriority = 10;
                this.reqReason = "preferred:" + this.winTitle;
                fpsInited = true;
            }
            if (xmlSettings != null) {
                SettingItem xmlSetting = (SettingItem) xmlSettings.first;
                String xmlKey = (String) xmlSettings.second;
                if (!fpsInited || (xmlSetting.reqFps > 0 && xmlSetting.reqPriority >= this.reqPriority)) {
                    this.reqFps = xmlSetting.reqFps;
                    this.reqPriority = xmlSetting.reqPriority;
                    this.reqReason = "reqFps:" + xmlKey;
                }
                if (xmlSetting.touchMinFps > 0) {
                    this.touchMinFps = xmlSetting.touchMinFps;
                    this.touchReason = "touchMin:" + xmlKey;
                }
                this.configBits = xmlSetting.configBits;
                this.dfps = xmlSetting.dfps;
                this.touchMaxFps = xmlSetting.touchMaxFps;
            }
        }

        public int getFps() {
            int i = this.reqFps;
            if (i > 0) {
                return i;
            }
            int i2 = this.touchMaxFps;
            return i2 > 0 ? i2 : GlobalConfigs.getUserSettingMaxRefreshRate();
        }

        public void apply() {
            if (!this.client.isAlive() || this.client.mPid != RMWms.getInstance().getOwnerPid(this.owner)) {
                return;
            }
            int extra = 1;
            int i = this.configBits & 255;
            this.configBits = i;
            if (i != 0) {
                int extra2 = 1 | 4;
                extra = extra2 | (i << 8);
            }
            if (this.reqFpsHandle <= 0 && this.reqFps > 0) {
                this.reqFpsHandle = RefreshRateAdjuster.getInstance().acquireRefreshRate(SceneManager.APP_REQUEST_SCENE, this.reqReason, this.reqFps, this.reqPriority, 0, 0, this.client.mPid, 0, this.dfps, extra);
                return;
            }
            if (this.touchMinHandle <= 0 && this.touchMinFps > 0) {
                this.touchMinHandle = RefreshRateAdjuster.getInstance().acquireRefreshRate(SceneManager.INTERACTION_SCENE, this.touchReason, this.touchMinFps, 10, 0, 0, this.client.mPid, 0, this.dfps, extra);
            }
            if (this.touchMinFps > 0 || this.touchMaxFps > 0 || this.configBits > 0) {
                GlobalConfigs.setWindowInteractionFps(this.touchMinFps, this.touchMaxFps, this.configBits);
            }
        }

        public void cancel() {
            if (this.reqFpsHandle > 0) {
                RefreshRateAdjuster.getInstance().releaseRefreshRate(this.reqFpsHandle);
                this.reqFpsHandle = -1L;
            }
            if (this.touchMinHandle > 0) {
                RefreshRateAdjuster.getInstance().releaseRefreshRate(this.touchMinHandle);
                this.touchMinHandle = -1L;
            }
            GlobalConfigs.setWindowInteractionFps(-1, -1, 0);
        }

        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof WindowRequest)) {
                return false;
            }
            WindowRequest that = (WindowRequest) obj;
            return that.mode == this.mode && that.owner == this.owner && that.reqFps == this.reqFps && that.reqPriority == this.reqPriority && that.touchMinFps == this.touchMinFps && that.touchMaxFps == this.touchMaxFps && that.dfps == this.dfps && that.configBits == this.configBits;
        }

        public String toString() {
            return String.format("%s->%s mode=%d pid=%d reqFps=%d touchMinFps=%d touchMaxFps=%d priority=%d configBits=0x%x dfps=%s", Integer.toHexString(System.identityHashCode(this.owner)), this.winTitle, Integer.valueOf(this.mode), Integer.valueOf(this.client.mPid), Integer.valueOf(this.reqFps), Integer.valueOf(this.touchMinFps), Integer.valueOf(this.touchMaxFps), Integer.valueOf(this.reqPriority), Integer.valueOf(this.configBits), String.valueOf(this.dfps));
        }
    }

    private String getTitle(WindowManager.LayoutParams lp) {
        String title = lp.getTitle().toString();
        if (TextUtils.isEmpty(title)) {
            title = "null";
        }
        if (title.indexOf("/") == -1) {
            return lp.packageName + "/" + title;
        }
        return title;
    }

    private String toString(WindowState win) {
        WindowManager.LayoutParams lp = RMWms.getInstance().getAttrs(win);
        int mode = GlobalConfigs.getUserSettingRefreshRateMode();
        String title = getTitle(lp);
        Pair<SettingItem, String> xmlSettings = getSettingItem(String.valueOf(mode), win.getOwningPackage(), title, false);
        StringBuilder builder = new StringBuilder(128);
        builder.append(String.format("%s->title=%s pid=%d width=%d height=%d modeId=%d fps=%.2f animating=%s", Integer.toHexString(System.identityHashCode(win)), title, Integer.valueOf(RMWms.getInstance().getOwnerPid(win)), Integer.valueOf(RMWms.getInstance().getWidth(win)), Integer.valueOf(RMWms.getInstance().getHeight(win)), Integer.valueOf(RMWms.getInstance().getPreferredModeId(win)), Float.valueOf(lp.preferredRefreshRate), String.valueOf(RMWms.getInstance().isAnimating(win))));
        if (xmlSettings != null) {
            builder.append(" xml={");
            builder.append(((SettingItem) xmlSettings.first).toString());
            builder.append("}");
        }
        return builder.toString();
    }

    private String flagsToName(int flags) {
        if (flags != 1) {
            if (flags != 2) {
                if (flags != 4) {
                    if (flags != 8) {
                        if (flags == 16) {
                            return "xml_setting_always";
                        }
                        return "unknow";
                    }
                    return "layout_param_always";
                }
                return "activity_switch_high_rate";
            }
            return "animation_high_rate";
        }
        return "focus_first";
    }

    private void dumpSettings(PrintWriter pw, HashMap<String, ArrayList<SettingItem>> settings) {
        if (!settings.isEmpty()) {
            for (String title : settings.keySet()) {
                pw.print('\t');
                pw.print(title);
                pw.println(":");
                Iterator<SettingItem> it = settings.get(title).iterator();
                while (it.hasNext()) {
                    SettingItem item = it.next();
                    pw.print('\t');
                    pw.print('\t');
                    pw.println(item.toString());
                }
            }
        }
    }

    private void dumpFlags(PrintWriter pw, int flags) {
        int i = 0;
        int shiftFlags = flags;
        pw.print(String.format("Flags:0x%X->[", Integer.valueOf(flags)));
        while (shiftFlags != 0) {
            int flag = 1 << i;
            shiftFlags >>= 1;
            i++;
            if ((flags & flag) != 0) {
                pw.print(flagsToName(flag));
                if (shiftFlags != 0) {
                    pw.append('|');
                }
            }
        }
        pw.println(']');
    }

    public void dump(PrintWriter pw, String value) {
        if (!GlobalConfigs.isAllowDump()) {
            return;
        }
        if (value == null || value.equals("0")) {
            pw.print("Xml Settings");
            pw.print(this.mVersion);
            pw.println(":");
            dumpSettings(pw, this.mWindowFpsXmlSettings);
            dumpSettings(pw, this.mPackageFpsXmlSettings);
            if (!this.mCommonSettings.isEmpty()) {
                pw.println("Common:");
                Iterator<SettingItem> it = this.mCommonSettings.iterator();
                while (it.hasNext()) {
                    SettingItem item = it.next();
                    pw.print('\t');
                    pw.println(item.toString());
                }
            }
            if (!this.mAnimationSettings.isEmpty()) {
                pw.println("Animation:");
                Iterator<SettingItem> it2 = this.mAnimationSettings.iterator();
                while (it2.hasNext()) {
                    SettingItem item2 = it2.next();
                    pw.print('\t');
                    pw.println(item2.toString());
                }
            }
        }
        synchronized (this) {
            pw.println("Window List:");
            for (WindowState win : this.mWindows.keySet()) {
                pw.print('\t');
                pw.println(toString(win));
            }
        }
        WindowState focusWindow = this.mFocusWindow;
        if (focusWindow != null) {
            pw.print("FocusWindow:");
            pw.println(Integer.toHexString(System.identityHashCode(focusWindow)));
        }
        WindowRequest request = this.mWindowRequest;
        if (request != null) {
            pw.println("WindowRequest:");
            pw.print('\t');
            pw.println(request.toString());
        }
        Pair<SettingItem, String> brightness = this.mWindowsBrightness;
        if (brightness != null) {
            pw.println("WindowsBrightness:");
            pw.print('\t');
            pw.println(brightness.toString());
        }
        dumpFlags(pw, this.mFlags);
    }
}