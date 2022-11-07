package com.vivo.services.rms.display;

import android.hardware.display.DisplayManagerGlobal;
import android.os.IBinder;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.SurfaceControl;
import com.vivo.common.utils.VLog;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/* loaded from: classes.dex */
public class DisplayConfigsManager {
    public static final int CURRENT_MAX_FPS = 1000;
    public static final int CURRENT_MIN_FPS = -1000;
    public static final String TAG = "RefreshRateAdjuster";
    private final SurfaceControl.DesiredDisplayConfigSpecs mActiveConfigSpecs;
    private int mActiveModeId;
    private IBinder mBuildInDisplayToken;
    private volatile int mConfigBits;
    private int mDFps;
    private DisplayMode mDefaultMode;
    private final ArrayList<SurfaceControl.DisplayConfig> mDisplayConfigs;
    private DisplayInfo mDisplayInfo;
    private int mLastModeId;
    private int mMaxRefreshRate;
    private int mMinRefreshRate;
    private boolean mMultiMode;
    private long mPhysicalDisplayId;
    private volatile int mResolution;
    private final HashMap<Integer, Integer> mSupportedConfigBits;
    private final ArrayList<DisplayMode> mSupportedModes;
    private final HashSet<Integer> mSupportedRefreshRates;
    private final HashSet<Integer> mSupportedResolutions;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Instance {
        private static final DisplayConfigsManager INSTANCE = new DisplayConfigsManager();

        private Instance() {
        }
    }

    public static DisplayConfigsManager getInstance() {
        return Instance.INSTANCE;
    }

    private DisplayConfigsManager() {
        this.mMinRefreshRate = -1;
        this.mMaxRefreshRate = -1;
        this.mActiveConfigSpecs = new SurfaceControl.DesiredDisplayConfigSpecs();
        this.mDisplayConfigs = new ArrayList<>();
        this.mSupportedModes = new ArrayList<>();
        this.mSupportedRefreshRates = new HashSet<>();
        this.mSupportedResolutions = new HashSet<>();
        this.mSupportedConfigBits = new HashMap<>();
        this.mMultiMode = false;
        this.mActiveModeId = -1;
        this.mLastModeId = -1;
        this.mResolution = 0;
        this.mConfigBits = 0;
        this.mDFps = 0;
    }

    public boolean initialize() {
        long[] physicalDisplayIds = SurfaceControl.getPhysicalDisplayIds();
        boolean z = false;
        if (physicalDisplayIds == null || physicalDisplayIds.length == 0) {
            return false;
        }
        long j = physicalDisplayIds[0];
        this.mPhysicalDisplayId = j;
        IBinder physicalDisplayToken = SurfaceControl.getPhysicalDisplayToken(j);
        this.mBuildInDisplayToken = physicalDisplayToken;
        if (physicalDisplayToken != null) {
            SurfaceControl.DisplayConfig[] configs = SurfaceControl.getDisplayConfigs(physicalDisplayToken);
            DisplayManagerGlobal dm = DisplayManagerGlobal.getInstance();
            DisplayInfo info = dm.getDisplayInfo(0);
            for (int i = 0; i < configs.length; i++) {
                this.mDisplayConfigs.add(configs[i]);
                DisplayMode mode = new DisplayMode(i, configs[i].configGroup, configs[i].refreshRate, configs[i].width, configs[i].height);
                this.mSupportedModes.add(mode);
                this.mSupportedRefreshRates.add(Integer.valueOf(mode.fps));
                this.mSupportedResolutions.add(Integer.valueOf(mode.resolution));
                if (mode.configBits != 0) {
                    this.mSupportedConfigBits.put(Integer.valueOf(mode.configBits), 0);
                }
            }
            this.mDisplayInfo = info;
            this.mMinRefreshRate = getMinRefreshRate(Integer.MIN_VALUE, Integer.MAX_VALUE);
            this.mMaxRefreshRate = getMaxRefreshRate(Integer.MIN_VALUE, Integer.MAX_VALUE);
            int activeConfig = SurfaceControl.getActiveConfig(this.mBuildInDisplayToken);
            this.mActiveModeId = activeConfig;
            DisplayMode displayMode = this.mSupportedModes.get(activeConfig);
            this.mDefaultMode = displayMode;
            this.mLastModeId = this.mActiveModeId;
            this.mResolution = displayMode.resolution;
            if (info.supportedModes.length > 1) {
                z = true;
            }
            this.mMultiMode = z;
            getInstance().setAllowedBrightness(2, DisplayMode.BIT_AUTO_MODE_1HZ_BRIGHTNESS);
            getInstance().setAllowedBrightness(1, 20);
        }
        return true;
    }

    public long getPhysicalDisplayId() {
        return this.mPhysicalDisplayId;
    }

    public IBinder getBuildInDisplayToken() {
        return this.mBuildInDisplayToken;
    }

    public int getActiveModeId() {
        return this.mActiveModeId;
    }

    public int getLastActiveModeId() {
        return this.mLastModeId;
    }

    public int getLastFps() {
        return this.mSupportedModes.get(this.mLastModeId).fps;
    }

    public int getActiveFps() {
        return this.mSupportedModes.get(this.mActiveModeId).fps;
    }

    public int getConfigsBits() {
        return this.mConfigBits;
    }

    public int getResolution() {
        return this.mResolution;
    }

    public DisplayMode getActiveMode() {
        return this.mSupportedModes.get(this.mActiveModeId);
    }

    public DisplayMode getSfActiveMode() {
        int id = SurfaceControl.getActiveConfig(this.mBuildInDisplayToken);
        return this.mSupportedModes.get(id);
    }

    public DisplayMode getModeById(int idx) {
        return this.mSupportedModes.get(idx);
    }

    public int getModeFpsById(int idx) {
        return this.mSupportedModes.get(idx).fps;
    }

    public ArrayList<DisplayMode> getSupportedModes() {
        return this.mSupportedModes;
    }

    public boolean isMultiMode() {
        return this.mMultiMode;
    }

    public int getMaxRefreshRate(int min, int max) {
        int fps = 0;
        Iterator<DisplayMode> it = this.mSupportedModes.iterator();
        while (it.hasNext()) {
            DisplayMode mode = it.next();
            if (!mode.isPrivate && mode.fps >= min && mode.fps <= max && (fps == 0 || fps < mode.fps)) {
                fps = mode.fps;
            }
        }
        return fps;
    }

    public int getMinRefreshRate(int min, int max) {
        int fps = 0;
        Iterator<DisplayMode> it = this.mSupportedModes.iterator();
        while (it.hasNext()) {
            DisplayMode mode = it.next();
            if (!mode.isPrivate && mode.fps >= min && mode.fps <= max && (fps == 0 || fps > mode.fps)) {
                fps = mode.fps;
            }
        }
        return fps;
    }

    public int getMaxRefreshRate() {
        return this.mMaxRefreshRate;
    }

    public int getMinRefreshRate() {
        return this.mMinRefreshRate;
    }

    public DisplayMode findDisplayMode(int resolution, int fps, int configsBits, boolean includePrivateMode) {
        DisplayMode bestMode = null;
        float minDelta = 2.14748365E9f;
        Iterator<DisplayMode> it = this.mSupportedModes.iterator();
        while (it.hasNext()) {
            DisplayMode mode = it.next();
            if (resolution == 0 || mode.resolution == resolution) {
                if (!mode.isPrivate || includePrivateMode) {
                    if (mode.configBits == 0 || mode.configBits == configsBits) {
                        if (GlobalConfigs.isSameFps(mode.fps, fps)) {
                            if (minDelta > 0.0f || (0.0f == minDelta && mode.configBits == configsBits)) {
                                bestMode = mode;
                                minDelta = 0.0f;
                            }
                            if (mode.configBits == configsBits) {
                                break;
                            }
                        } else if (minDelta != 0.0f && mode.fps > fps) {
                            float delta = mode.fps - fps;
                            if (delta < minDelta || (delta == minDelta && mode.configBits == configsBits)) {
                                bestMode = mode;
                                minDelta = delta;
                            }
                        }
                    }
                }
            }
        }
        return bestMode;
    }

    public boolean isResolutionSupported(int resolution) {
        return this.mSupportedResolutions.contains(Integer.valueOf(resolution));
    }

    public boolean isConfigBitsSupported(int configBits) {
        return configBits == 0 || this.mSupportedConfigBits.containsKey(Integer.valueOf(configBits));
    }

    public boolean hasConfigBits() {
        return !this.mSupportedConfigBits.isEmpty();
    }

    public boolean isConfigBitsAllowed(int configBits, int brightness) {
        if (configBits == 0) {
            return true;
        }
        if (hasConfigBits() && isConfigBitsSupported(configBits)) {
            for (Map.Entry<Integer, Integer> entry : this.mSupportedConfigBits.entrySet()) {
                int bits = entry.getKey().intValue();
                int threshold = entry.getValue().intValue();
                if (threshold > 0 && (configBits & bits) != 0 && brightness < threshold) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public boolean updateAllowdBrightness(int newBrightness, int oldBrightness) {
        if (hasConfigBits()) {
            for (Map.Entry<Integer, Integer> entry : this.mSupportedConfigBits.entrySet()) {
                int threshold = entry.getValue().intValue();
                if (threshold > 0) {
                    if ((newBrightness < threshold) != (oldBrightness < threshold)) {
                        return true;
                    }
                }
            }
            return false;
        }
        return false;
    }

    public int getAllowedBrightness(int configBits) {
        Integer value = this.mSupportedConfigBits.get(Integer.valueOf(configBits));
        if (value == null) {
            return 0;
        }
        return value.intValue();
    }

    public void setAllowedBrightness(int configBits, int brightness) {
        if (isConfigBitsSupported(configBits)) {
            this.mSupportedConfigBits.put(Integer.valueOf(configBits), Integer.valueOf(brightness));
        }
    }

    public boolean isRefreshRateSupported(int fps) {
        Iterator<Integer> it = this.mSupportedRefreshRates.iterator();
        while (it.hasNext()) {
            int refreshRate = it.next().intValue();
            if (GlobalConfigs.isSameFps(refreshRate, fps)) {
                return true;
            }
        }
        return false;
    }

    public int getFpsByAndroidModeId(int id) {
        Display.Mode[] modeArr;
        for (Display.Mode mode : this.mDisplayInfo.supportedModes) {
            if (mode.getModeId() == id) {
                return GlobalConfigs.convertRefreshRate(mode.getRefreshRate());
            }
        }
        return 0;
    }

    public int adjustPrivateFps(int fps) {
        DisplayMode mode = findDisplayMode(0, fps, 0, false);
        if (mode != null) {
            return mode.fps;
        }
        return 0;
    }

    public DisplayMode getExactModeByFps(int resolution, int fps, int reqBits) {
        DisplayMode mode;
        if (fps <= 0 || (mode = findDisplayMode(resolution, fps, reqBits, true)) == null || !GlobalConfigs.isSameFps(mode.fps, fps)) {
            return null;
        }
        return mode;
    }

    public int toRealFps(int fps) {
        if (fps == 1000) {
            fps = this.mMaxRefreshRate;
        }
        if (fps == -1000) {
            return this.mMinRefreshRate;
        }
        return fps;
    }

    public void setResolution(int width, int height) {
        int curResolution = this.mResolution;
        int resolution = DisplayMode.toResolution(width, height);
        if (curResolution != resolution && isResolutionSupported(resolution)) {
            VLog.d("RefreshRateAdjuster", String.format("setResolution from %dx%d to %dx%d", Integer.valueOf(DisplayMode.toWidth(curResolution)), Integer.valueOf(DisplayMode.toHeight(curResolution)), Integer.valueOf(width), Integer.valueOf(height)));
            this.mResolution = resolution;
            RefreshRateAdjuster.getInstance().updateRequest(2, resolution);
        }
    }

    public void setConfigBits(int caller, int bits, int mask) {
        int curConfigBits = this.mConfigBits;
        if ((curConfigBits & mask) != bits && isConfigBitsSupported(mask)) {
            int configBits = ((~mask) & curConfigBits) | (bits & mask);
            if (!isConfigBitsSupported(configBits)) {
                VLog.d("RefreshRateAdjuster", String.format("setConfigBits fail caller=%d for 0x%x is not supported, now=0x%x set=0x%x", Integer.valueOf(caller), Integer.valueOf(configBits), Integer.valueOf(curConfigBits), Integer.valueOf(bits)));
                return;
            }
            VLog.d("RefreshRateAdjuster", String.format("setConfigBits caller=%d set from 0x%x to 0x%x", Integer.valueOf(caller), Integer.valueOf(this.mConfigBits), Integer.valueOf(configBits)));
            this.mConfigBits = configBits;
            RefreshRateAdjuster.getInstance().updateRequest(4, configBits);
        }
    }

    public void setDFps(int dfps) {
        this.mDFps = dfps;
    }

    public int getDFps() {
        return this.mDFps;
    }

    public void setActiveConfig(int modeId) {
        if (modeId != this.mActiveModeId) {
            setDFps(0);
            DisplayMode lastMode = this.mSupportedModes.get(this.mActiveModeId);
            DisplayMode mode = this.mSupportedModes.get(modeId);
            this.mActiveConfigSpecs.defaultConfig = modeId;
            this.mActiveConfigSpecs.primaryRefreshRateMin = mode.fps;
            this.mActiveConfigSpecs.primaryRefreshRateMax = mode.fps;
            this.mActiveConfigSpecs.appRequestRefreshRateMin = mode.fps;
            this.mActiveConfigSpecs.appRequestRefreshRateMax = mode.fps;
            this.mLastModeId = this.mActiveModeId;
            this.mActiveModeId = modeId;
            if (lastMode.resolution != mode.resolution) {
                VLog.d("RefreshRateAdjuster", String.format("setDisplaySize from %dx%d to %dx%d", Integer.valueOf(lastMode.getWidth()), Integer.valueOf(lastMode.getHeight()), Integer.valueOf(mode.getWidth()), Integer.valueOf(mode.getHeight())));
                SurfaceControl.openTransaction();
                SurfaceControl.setDisplaySize(this.mBuildInDisplayToken, mode.getWidth(), mode.getHeight());
                SurfaceControl.closeTransaction();
            }
            SurfaceControl.setDesiredDisplayConfigSpecs(this.mBuildInDisplayToken, this.mActiveConfigSpecs);
        }
    }

    public void dump(PrintWriter pw, String[] args) {
        pw.println("FeatureEnabled:" + GlobalConfigs.isFeatureEnabled());
        pw.println("DisplayConfigs:");
        Iterator<SurfaceControl.DisplayConfig> it = this.mDisplayConfigs.iterator();
        while (it.hasNext()) {
            SurfaceControl.DisplayConfig config = it.next();
            pw.println("\t" + config.toString());
        }
        pw.println("SupportedModes:");
        Iterator<DisplayMode> it2 = this.mSupportedModes.iterator();
        while (it2.hasNext()) {
            DisplayMode mode = it2.next();
            pw.println("\t" + mode.toString());
        }
        pw.println("SupportedMinRefreshRate:" + this.mMinRefreshRate);
        pw.println("SupportedMaxRefreshRate:" + this.mMaxRefreshRate);
        pw.print("SupportedResolutions:[");
        Iterator<Integer> it1 = this.mSupportedResolutions.iterator();
        while (it1.hasNext()) {
            Integer value = it1.next();
            pw.print(String.format("%dx%d", Integer.valueOf(DisplayMode.toWidth(value.intValue())), Integer.valueOf(DisplayMode.toHeight(value.intValue()))));
            if (it1.hasNext()) {
                pw.print(",");
            }
        }
        pw.println("]");
        pw.print("SupportedConfigBits:[");
        Iterator<Map.Entry<Integer, Integer>> it22 = this.mSupportedConfigBits.entrySet().iterator();
        while (it22.hasNext()) {
            Map.Entry<Integer, Integer> entry = it22.next();
            pw.print(String.format("0x%x=%d", Integer.valueOf(entry.getKey().intValue()), Integer.valueOf(entry.getValue().intValue())));
            if (it22.hasNext()) {
                pw.print(",");
            }
        }
        pw.println("]");
        pw.println("DefaultMode:" + this.mDefaultMode);
        pw.println("LastMode:" + this.mSupportedModes.get(this.mLastModeId));
        pw.println("ActiveMode:" + this.mSupportedModes.get(this.mActiveModeId));
        pw.println(String.format("Resolution:%dx%d", Integer.valueOf(DisplayMode.toWidth(this.mResolution)), Integer.valueOf(DisplayMode.toHeight(this.mResolution))));
        pw.println(String.format("ConfigBits:0x%x", Integer.valueOf(this.mConfigBits)));
        pw.println("UserSettingMode:" + GlobalConfigs.getUserSettingRefreshRateMode());
        pw.println("TouchMinRefreshRate:" + GlobalConfigs.getInteractionMinRefreshRate());
        pw.println("TouchMaxRefreshRate:" + GlobalConfigs.getInteractionMaxRefreshRate());
        pw.println(String.format("TouchConfigBits:0x%x", Integer.valueOf(GlobalConfigs.getInteractionConfigBits())));
        pw.println("isOledPanel:" + GlobalConfigs.IS_OLED_PANEL);
        pw.println("UseBringhtness:" + GlobalConfigs.isUseBrightness());
        if (GlobalConfigs.isUseBrightness()) {
            pw.println("LowBrightnessThreshold:" + GlobalConfigs.getLowBrightnessValue());
        }
        pw.println("ScreenOn:" + GlobalConfigs.isScreenOn());
        pw.println("NightMode:" + GlobalConfigs.isNightMode());
        pw.println("Brightness:" + GlobalConfigs.getScreenBrightness());
        pw.println("DisplayState:" + Display.stateToString(GlobalConfigs.getDisplayState()));
        pw.println("LowPowerMode:" + GlobalConfigs.isLowPowerMode());
        pw.println("AutoTouchInLowPowerMode:" + GlobalConfigs.isAutoTouchInLowPowerMode());
        pw.println("AutoTouchMode:" + GlobalConfigs.isAutoTouchMode());
        pw.println("AnimationRefreshRate:" + GlobalConfigs.getAnimationRefreshRate());
        pw.println("AppRequestRefreshRateLimited:" + GlobalConfigs.isAppRequestRefreshRateLimited());
        pw.println("DfpsEnable:" + GlobalConfigs.isDfpsEnable());
        pw.println("VsyncRate:" + GlobalConfigs.getVsyncRate());
        pw.println("TouchMinFps:" + GlobalConfigs.getTouchMinDFps());
        pw.println("MinDfps:" + GlobalConfigs.getMinDFps());
        pw.println("DFPS:" + this.mDFps);
        pw.println();
    }

    /* loaded from: classes.dex */
    public static class DisplayMode {
        public static final int BIT_AUTO_MODE_10HZ = 1;
        public static final int BIT_AUTO_MODE_10HZ_BRIGHTNESS = 20;
        public static final int BIT_AUTO_MODE_1HZ = 2;
        public static final int BIT_AUTO_MODE_1HZ_BRIGHTNESS = 232;
        public static final int CONFIG_BITS_MASK = 255;
        public static final int CONFIG_BITS_SHIFT = 8;
        private static final int RESOLUTION_VALUE_MASK = 65535;
        private static final int RESOLUTION_VALUE_SHIFT = 16;
        public final int configBits;
        public final int configGroup;
        public final int fps;
        public final int id;
        public boolean isPrivate;
        public final int resolution;

        public DisplayMode(int idx, int groupId, float refreshRate, int w, int h) {
            groupId = groupId == -1 ? 0 : groupId;
            this.id = idx;
            this.configGroup = groupId & 255;
            this.configBits = groupId >> 8;
            this.resolution = toResolution(w, h);
            int convertRefreshRate = GlobalConfigs.convertRefreshRate(refreshRate);
            this.fps = convertRefreshRate;
            this.isPrivate = GlobalConfigs.isPrivateFps(convertRefreshRate);
        }

        public int getWidth() {
            return toWidth(this.resolution);
        }

        public int getHeight() {
            return toHeight(this.resolution);
        }

        public static int toWidth(int resolution) {
            return resolution >> 16;
        }

        public static int toHeight(int resolution) {
            return RESOLUTION_VALUE_MASK & resolution;
        }

        public static int toResolution(int w, int h) {
            return (w << 16) | h;
        }

        public String toString() {
            return "{id=" + this.id + ", group=" + this.configGroup + ", bits=" + String.format("0x%x", Integer.valueOf(this.configBits)) + ", fps=" + this.fps + ", width=" + getWidth() + ", height=" + getHeight() + ", isPrivate=" + this.isPrivate + "}";
        }
    }
}