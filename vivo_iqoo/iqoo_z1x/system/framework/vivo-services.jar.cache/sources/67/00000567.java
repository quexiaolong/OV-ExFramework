package com.android.server.wm;

import android.content.Context;
import android.os.FtBuild;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.view.DisplayInfo;
import com.android.server.notification.VivoNotificationManagerServiceImpl;
import com.vivo.common.VivoCollectData;
import com.vivo.face.common.data.Constants;
import com.vivo.services.superresolution.Constant;
import com.vivo.vcodetransbase.EventTransfer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoMultiWindowConfig extends VivoMultiWindowFeature implements IVivoMultiWindowConfig {
    private static final String ALLOW_ALLTYPE_DRAG_IND_APP = "AllowAllTypDragIndApp";
    private static final String ALLOW_SPLIT_APPS_PACKAGENAME_lIST = "AllowSplitAppsList";
    private static final String ALLOW_SPLIT_INPUT_METHODS = "InputMethodApp";
    public static boolean DEBUG = false;
    public static boolean DEBUG_LOG_CTRL = false;
    private static final String DOUBLE_RESUME_APP = "DoubleResumeApp";
    private static final String EVENT_WRITE_TAG = "split_info:";
    private static final String FORCE_FULL_SCREEN_ACTIVITIES_LIST = "ForceFullscreenActivity";
    private static final String FORCE_MIN_MAX_ACTIVITIES_LIST = "ForceMinMaxActivity";
    private static final String FORCE_RESIZABLE_ACTIVITIES_LIST = "ForceResizableActivity";
    private static final String IGNORE_RELAUNCH_ACTIVITY = "IgnoreRelaunchActivity";
    private static final String IGNORE_RELAUNCH_ACTIVITY_AFTER_SPLIT = "IgnoreRelaunchActivityAlreadySplit";
    private static final String IGNORE_RELAUNCH_APP = "IgnoreRelaunchApp";
    private static final String IGNORE_RELAUNCH_ORIENTATION_ACTIVITY = "IgnoreRelaunchOrientationActivity";
    private static final String KEY_MULT_DRAG = "stride";
    private static final String KEY_MULT_DRAG_APP = "app1";
    private static final String KEY_MULT_DROP_APP = "app2";
    public static final String MODIFY_TOP_TENCENT_VERSION = "ModifyTopInTencentVersion";
    private static final String NEED_HIDE_SOFT_BEFORE_SPLIT_ACTIVITY = "NeedHideSoftBeforeSplitActivity";
    private static final String NEED_RELAUNCH_ACTIVITY = "NeedRelaunchActivity";
    private static final String NEED_RELAUNCH_APP = "NeedRelaunchApp";
    private static final String NEED_RELAUNCH_ORIENTATION_ACTIVITY = "NeedRelaunchOrientationActivity";
    private static final String NOT_ALLOW_SPLIT_APPS_PACKAGENAME_lIST = "NotAllowSplitAppsList";
    private static final String ORIGINAL_OS_NAME = "vos";
    public static final String SMART_MULTIWINDOW_NAME = "com.vivo.smartmultiwindow";
    private static final String SUB_EVENT_ID_DRAG = "107293";
    private static final String TAG = "VivoMultiWindowConfig";
    public static final int TYPE_ALLOW_ALLTYPE_DRAG_IND_MASK_APP = 54;
    public static final int TYPE_ALLOW_SPLIT_APPS_PACKAGENAME_LIST = 1;
    public static final int TYPE_ALLOW_SPLIT_INPUT_METHOD = 53;
    public static final int TYPE_DOUBLE_RESUME_APP = 32;
    public static final int TYPE_FORCE_FULL_SCREEN_ACTIVITIES_LIST = 3;
    public static final int TYPE_FORCE_MIN_MAX_ACTIVITIES_LIST = 4;
    public static final int TYPE_FORCE_RESIZABLE_ACTIVITIES_LIST = 5;
    public static final int TYPE_IGNORE_RELAUNCH_ACTIVITY = 19;
    public static final int TYPE_IGNORE_RELAUNCH_ACTIVITY_AFTER_SPLIT = 52;
    public static final int TYPE_IGNORE_RELAUNCH_APP = 18;
    public static final int TYPE_IGNORE_RELAUNCH_ORIENTATION_ACTIVITY = 21;
    public static final int TYPE_NEED_HIDE_SOFT_BEFORE_SPLIT_ACTIVITY = 66;
    public static final int TYPE_NEED_RELAUNCH_ACTIVITY = 17;
    public static final int TYPE_NEED_RELAUNCH_APP = 16;
    public static final int TYPE_NEED_RELAUNCH_ORIENTATION_ACTIVITY = 20;
    public static final int TYPE_NOT_ALLOW_SPLIT_APPS_PACKAGENAME_LIST = 2;
    public static final int TYPE_RESIZING_BG_ACTIVITY = 48;
    public static final int TYPE_RESIZING_BG_COLOR = 49;
    public static final int TYPE_RESIZING_BG_FALLBACK_ACTIVITY = 50;
    public static final int TYPE_RESIZING_BG_FALLBACK_COLOR = 51;
    private static VivoMultiWindowConfig sVivoMultiWindowConfig;
    private ArrayList<String> mAllowSplitAppslist;
    private ArrayList<String> mAllowSplitInputMethods;
    private ArrayList<String> mAvoidMoveFocusToPrimaryAcitvity;
    private ArrayList<String> mDoubleResumeApp;
    private ArrayList<String> mForceCutoutAppList;
    private ArrayList<String> mForceFullScreenActivitylist;
    private ArrayList<String> mForceHideStatusJustAfterExitSplitList;
    private ArrayList<String> mForceIgnoreRelaunchAfterBackActivityList;
    private ArrayList<String> mForceMinMaxActivitylist;
    private ArrayList<String> mForceResizableActivitylist;
    private Handler mHandler;
    private ArrayList<String> mIgnoreRelaunchActivity;
    private ArrayList<String> mIgnoreRelaunchActivityAlreadySplit;
    private ArrayList<String> mIgnoreRelaunchApp;
    private ArrayList<String> mIgnoreRelaunchOrientationActivity;
    private ArrayList<String> mIgnoreUpdateMultiModeWithInvisibleList;
    private boolean mIsOverseas;
    private ArrayList<String> mLayoutIncludeNavApp;
    private ArrayList<String> mModifyTopInTencentVersion;
    private ArrayList<String> mNeedHideSoftBeforeSplitActivity;
    private ArrayList<String> mNeedRelaunchActivity;
    private ArrayList<String> mNeedRelaunchApp;
    private ArrayList<String> mNeedRelaunchOrientationActivity;
    private ArrayList<String> mNotAllowSplitAppslist;
    private ArrayList<String> mSpecialDockApp;
    private ArrayList<String> mSpecialFreezingActivityList;
    private ArrayList<String> mSpecialSplashActivity;
    private int mTierLevel;
    private ArrayList<String> mVideoAppList;
    boolean mVigourOverSeaVos2;
    private ArrayList<String> mVivoAllowAllTypeDragIndMaskApp;
    boolean mVivoMultiWindowSupport;
    boolean mVivoVosMultiWindowSupport;
    private static boolean isSplitScreenEventEnable = false;
    private static final boolean ifCheckLockDebug = SystemProperties.getBoolean("persist.vivo.split_checkthreadlock", false);
    public static boolean SYS_LOG_CTRL = "yes".equals(SystemProperties.get("persist.sys.log.ctrl", "no"));
    private final String EVENT_ID = "1072";
    private final String SUB_EVENT_ID_EXIT_SPLIT = "107254";
    private final String KEY_MULT_SCREEN_QUIT = "quit";
    private final String KEY_MULT_SCREEN_QUIT_WAY = "way";
    private final String VAL_QUIT_TYPE_4 = "4";
    private boolean mMultiWindowConfigInitalization = false;
    boolean mIsVivoVosVersion = ORIGINAL_OS_NAME.equals(SystemProperties.get("ro.vivo.os.name", "unknown"));

    static {
        boolean z = false;
        boolean equals = "yes".equals(SystemProperties.get("persist.multiwindowlog.debug", "no"));
        DEBUG_LOG_CTRL = equals;
        DEBUG = (SYS_LOG_CTRL || equals) ? true : true;
    }

    public static VivoMultiWindowConfig getInstance() {
        if (sVivoMultiWindowConfig == null) {
            synchronized (VivoMultiWindowConfig.class) {
                if (sVivoMultiWindowConfig == null) {
                    sVivoMultiWindowConfig = new VivoMultiWindowConfig();
                }
            }
        }
        return sVivoMultiWindowConfig;
    }

    private void addDefaultValueToList() {
        ArrayList<String> arrayList = this.mNotAllowSplitAppslist;
        if (arrayList != null) {
            arrayList.add("com.bbk.SuperPowerSave");
        }
        ArrayList<String> arrayList2 = this.mIgnoreRelaunchActivity;
        if (arrayList2 != null) {
            arrayList2.add("com.bbk.SuperPowerSave/.SuperPowerSaveActivity");
        } else {
            VSlog.wtf(TAG, "ignore relaunch activity list is null");
        }
    }

    private VivoMultiWindowConfig() {
        this.mHandler = null;
        boolean z = false;
        this.mTierLevel = 0;
        this.mIsOverseas = false;
        this.mVivoMultiWindowSupport = IS_VIVO_SUPPORT_MULTIWINDOW_PROPERTY && !this.mIsVivoVosVersion;
        boolean z2 = IS_VIVO_SUPPORT_MULTIWINDOW_PROPERTY && this.mIsVivoVosVersion;
        this.mVivoVosMultiWindowSupport = z2;
        if (z2 && FtBuild.getRomVersion() > 2.0f) {
            z = true;
        }
        this.mVigourOverSeaVos2 = z;
        this.mVideoAppList = null;
        this.mIgnoreUpdateMultiModeWithInvisibleList = null;
        this.mSpecialSplashActivity = null;
        this.mSpecialDockApp = null;
        this.mLayoutIncludeNavApp = null;
        this.mSpecialFreezingActivityList = null;
        this.mForceHideStatusJustAfterExitSplitList = null;
        this.mForceCutoutAppList = null;
        this.mForceIgnoreRelaunchAfterBackActivityList = null;
        this.mAvoidMoveFocusToPrimaryAcitvity = null;
        this.mAllowSplitAppslist = new ArrayList<>();
        this.mNotAllowSplitAppslist = new ArrayList<>();
        this.mForceFullScreenActivitylist = new ArrayList<>();
        this.mForceMinMaxActivitylist = new ArrayList<>();
        this.mForceResizableActivitylist = new ArrayList<>();
        this.mNeedRelaunchApp = new ArrayList<>();
        this.mNeedRelaunchActivity = new ArrayList<>();
        this.mNeedHideSoftBeforeSplitActivity = new ArrayList<>();
        this.mIgnoreRelaunchApp = new ArrayList<>();
        this.mIgnoreRelaunchActivity = new ArrayList<>();
        this.mIgnoreRelaunchActivityAlreadySplit = new ArrayList<>();
        this.mNeedRelaunchOrientationActivity = new ArrayList<>();
        this.mIgnoreRelaunchOrientationActivity = new ArrayList<>();
        this.mDoubleResumeApp = new ArrayList<>();
        this.mAllowSplitInputMethods = new ArrayList<>();
        this.mModifyTopInTencentVersion = new ArrayList<>();
        this.mVivoAllowAllTypeDragIndMaskApp = new ArrayList<>();
        if (Looper.myLooper() != null) {
            this.mHandler = new Handler();
        }
        addDefaultValueToList();
        initVideoAppList();
        initSpecialDockAppList();
        initLayoutIncludeNavApp();
        initSpecialFreezingActviityList();
        initForceCutoutAppList();
        initForceIgnoreRelaunchAfterBackActivityList();
        initAvoidMoveFocusToPrimaryAcitvity();
        initForceHideStatusbarJustAfterExitSplitList();
        initIgnoreUpdateMultiModeWithInvisibleList();
        this.mTierLevel = FtBuild.getTierLevel();
        this.mIsOverseas = FtBuild.isOverSeas();
        dumpCurrentConfig();
    }

    void dumpArrayList(String info, ArrayList<String> list) {
        if (list != null) {
            VSlog.v(TAG, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + info + ":" + Arrays.toString(list.toArray()));
            return;
        }
        VSlog.v(TAG, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + info + ":<null>");
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public void setMultiWindowConfig(Map<String, ArrayList<String>> map) {
        synchronized (this) {
            Iterator<Map.Entry<String, ArrayList<String>>> it = map.entrySet().iterator();
            while (true) {
                char c = 1;
                if (it.hasNext()) {
                    Map.Entry<String, ArrayList<String>> entry = it.next();
                    String key = entry.getKey();
                    switch (key.hashCode()) {
                        case -2106698235:
                            if (key.equals(NEED_RELAUNCH_APP)) {
                                c = 5;
                                break;
                            }
                            c = 65535;
                            break;
                        case -1653293529:
                            if (key.equals(IGNORE_RELAUNCH_ORIENTATION_ACTIVITY)) {
                                c = 11;
                                break;
                            }
                            c = 65535;
                            break;
                        case -1642677620:
                            if (key.equals(FORCE_MIN_MAX_ACTIVITIES_LIST)) {
                                c = 4;
                                break;
                            }
                            c = 65535;
                            break;
                        case -1352475929:
                            if (key.equals(IGNORE_RELAUNCH_ACTIVITY)) {
                                c = '\b';
                                break;
                            }
                            c = 65535;
                            break;
                        case -1279074636:
                            if (key.equals(NOT_ALLOW_SPLIT_APPS_PACKAGENAME_lIST)) {
                                break;
                            }
                            c = 65535;
                            break;
                        case -121576021:
                            if (key.equals(NEED_RELAUNCH_ACTIVITY)) {
                                c = 6;
                                break;
                            }
                            c = 65535;
                            break;
                        case -48127447:
                            if (key.equals(ALLOW_ALLTYPE_DRAG_IND_APP)) {
                                c = 14;
                                break;
                            }
                            c = 65535;
                            break;
                        case -36715069:
                            if (key.equals(DOUBLE_RESUME_APP)) {
                                c = '\f';
                                break;
                            }
                            c = 65535;
                            break;
                        case 357831503:
                            if (key.equals(FORCE_RESIZABLE_ACTIVITIES_LIST)) {
                                c = 3;
                                break;
                            }
                            c = 65535;
                            break;
                        case 927154709:
                            if (key.equals(FORCE_FULL_SCREEN_ACTIVITIES_LIST)) {
                                c = 2;
                                break;
                            }
                            c = 65535;
                            break;
                        case 1019728577:
                            if (key.equals(ALLOW_SPLIT_APPS_PACKAGENAME_lIST)) {
                                c = 0;
                                break;
                            }
                            c = 65535;
                            break;
                        case 1104089315:
                            if (key.equals(NEED_RELAUNCH_ORIENTATION_ACTIVITY)) {
                                c = '\n';
                                break;
                            }
                            c = 65535;
                            break;
                        case 1150431478:
                            if (key.equals(ALLOW_SPLIT_INPUT_METHODS)) {
                                c = '\r';
                                break;
                            }
                            c = 65535;
                            break;
                        case 1510808211:
                            if (key.equals(MODIFY_TOP_TENCENT_VERSION)) {
                                c = 16;
                                break;
                            }
                            c = 65535;
                            break;
                        case 1576205865:
                            if (key.equals(IGNORE_RELAUNCH_ACTIVITY_AFTER_SPLIT)) {
                                c = '\t';
                                break;
                            }
                            c = 65535;
                            break;
                        case 1996426057:
                            if (key.equals(IGNORE_RELAUNCH_APP)) {
                                c = 7;
                                break;
                            }
                            c = 65535;
                            break;
                        case 2022795336:
                            if (key.equals(NEED_HIDE_SOFT_BEFORE_SPLIT_ACTIVITY)) {
                                c = 15;
                                break;
                            }
                            c = 65535;
                            break;
                        default:
                            c = 65535;
                            break;
                    }
                    switch (c) {
                        case 0:
                            this.mAllowSplitAppslist.clear();
                            this.mAllowSplitAppslist.addAll(entry.getValue());
                            break;
                        case 1:
                            this.mNotAllowSplitAppslist.clear();
                            this.mNotAllowSplitAppslist.addAll(entry.getValue());
                            break;
                        case 2:
                            this.mForceFullScreenActivitylist.clear();
                            this.mForceFullScreenActivitylist.addAll(entry.getValue());
                            break;
                        case 3:
                            this.mForceResizableActivitylist.clear();
                            this.mForceResizableActivitylist.addAll(entry.getValue());
                            break;
                        case 4:
                            this.mForceMinMaxActivitylist.clear();
                            this.mForceMinMaxActivitylist.addAll(entry.getValue());
                            break;
                        case 5:
                            this.mNeedRelaunchApp.clear();
                            this.mNeedRelaunchApp.addAll(entry.getValue());
                            break;
                        case 6:
                            this.mNeedRelaunchActivity.clear();
                            this.mNeedRelaunchActivity.addAll(entry.getValue());
                            break;
                        case 7:
                            this.mIgnoreRelaunchApp.clear();
                            this.mIgnoreRelaunchApp.addAll(entry.getValue());
                            break;
                        case '\b':
                            this.mIgnoreRelaunchActivity.clear();
                            this.mIgnoreRelaunchActivity.addAll(entry.getValue());
                            break;
                        case '\t':
                            this.mIgnoreRelaunchActivityAlreadySplit.clear();
                            this.mIgnoreRelaunchActivityAlreadySplit.addAll(entry.getValue());
                            break;
                        case '\n':
                            this.mNeedRelaunchOrientationActivity.clear();
                            this.mNeedRelaunchOrientationActivity.addAll(entry.getValue());
                            break;
                        case 11:
                            this.mIgnoreRelaunchOrientationActivity.clear();
                            this.mIgnoreRelaunchOrientationActivity.addAll(entry.getValue());
                            break;
                        case '\f':
                            this.mDoubleResumeApp.clear();
                            this.mDoubleResumeApp.addAll(entry.getValue());
                            break;
                        case '\r':
                            this.mAllowSplitInputMethods.clear();
                            this.mAllowSplitInputMethods.addAll(entry.getValue());
                            break;
                        case 14:
                            this.mVivoAllowAllTypeDragIndMaskApp.clear();
                            this.mVivoAllowAllTypeDragIndMaskApp.addAll(entry.getValue());
                            break;
                        case 15:
                            this.mNeedHideSoftBeforeSplitActivity.clear();
                            this.mNeedHideSoftBeforeSplitActivity.addAll(entry.getValue());
                            break;
                        case 16:
                            this.mModifyTopInTencentVersion.clear();
                            this.mModifyTopInTencentVersion.addAll(entry.getValue());
                            break;
                        default:
                            VSlog.v(TAG, "setMultiWindowConfig:unknown key " + entry.getKey());
                            break;
                    }
                    addDefaultValueToList();
                } else {
                    this.mMultiWindowConfigInitalization = true;
                    if (DEBUG_ALL_SPLIT_PRIV_LOG) {
                        VSlog.v(TAG, "setMultiWindowConfig:dump config");
                        dumpArrayList("ALLOW_SPLIT_APPS_PACKAGENAME_lIST", this.mAllowSplitAppslist);
                        dumpArrayList("NOT_ALLOW_SPLIT_APPS_PACKAGENAME_lIST", this.mNotAllowSplitAppslist);
                        dumpArrayList("FORCE_FULL_SCREEN_ACTIVITIES_LIST", this.mForceFullScreenActivitylist);
                        dumpArrayList("FORCE_RESIZABLE_ACTIVITIES_LIST", this.mForceResizableActivitylist);
                        dumpArrayList("FORCE_MIN_MAX_ACTIVITIES_LIST", this.mForceMinMaxActivitylist);
                        dumpArrayList("NEED_RELAUNCH_APP", this.mNeedRelaunchApp);
                        dumpArrayList("NEED_RELAUNCH_ACTIVITY", this.mNeedRelaunchActivity);
                        dumpArrayList("IGNORE_RELAUNCH_APP", this.mIgnoreRelaunchApp);
                        dumpArrayList("IGNORE_RELAUNCH_ACTIVITY", this.mIgnoreRelaunchActivity);
                        dumpArrayList("IGNORE_RELAUNCH_ACTIVITY_AFTER_SPLIT", this.mIgnoreRelaunchActivityAlreadySplit);
                        dumpArrayList("NEED_RELAUNCH_ORIENTATION_ACTIVITY", this.mNeedRelaunchOrientationActivity);
                        dumpArrayList("IGNORE_RELAUNCH_ORIENTATION_ACTIVITY", this.mIgnoreRelaunchOrientationActivity);
                        dumpArrayList("DOUBLE_RESUME_APP", this.mDoubleResumeApp);
                        dumpArrayList("ALLOW_SPLIT_INPUT_METHODS", this.mAllowSplitInputMethods);
                        dumpArrayList("MODIFY_TOP_TENCENT_VERSION", this.mModifyTopInTencentVersion);
                        dumpArrayList("ALLOW_DRAG_ALLTYPE_IND_MASK_APP", this.mVivoAllowAllTypeDragIndMaskApp);
                        dumpArrayList("NEED_HIDE_SOFT_BEFORE_SPLIT_ACTIVITY", this.mNeedHideSoftBeforeSplitActivity);
                    }
                }
            }
        }
    }

    private ArrayList<String> getMultiWindowConfig(int type) {
        if (type != 1) {
            if (type != 2) {
                if (type != 3) {
                    if (type != 4) {
                        if (type != 5) {
                            if (type != 32) {
                                if (type != 66) {
                                    switch (type) {
                                        case 16:
                                            return this.mNeedRelaunchApp;
                                        case 17:
                                            return this.mNeedRelaunchActivity;
                                        case 18:
                                            return this.mIgnoreRelaunchApp;
                                        case 19:
                                            return this.mIgnoreRelaunchActivity;
                                        case 20:
                                            return this.mNeedRelaunchOrientationActivity;
                                        case 21:
                                            return this.mIgnoreRelaunchOrientationActivity;
                                        default:
                                            switch (type) {
                                                case 52:
                                                    return this.mIgnoreRelaunchActivityAlreadySplit;
                                                case 53:
                                                    return this.mAllowSplitInputMethods;
                                                case 54:
                                                    return this.mVivoAllowAllTypeDragIndMaskApp;
                                                default:
                                                    VSlog.v(TAG, "getMultiWindowConfig:unknown type " + type);
                                                    return null;
                                            }
                                    }
                                }
                                return this.mNeedHideSoftBeforeSplitActivity;
                            }
                            return this.mDoubleResumeApp;
                        }
                        return this.mForceResizableActivitylist;
                    }
                    return this.mForceMinMaxActivitylist;
                }
                return this.mForceFullScreenActivitylist;
            }
            return this.mNotAllowSplitAppslist;
        }
        return this.mAllowSplitAppslist;
    }

    private boolean isInListByType(int type, String str) {
        ArrayList<String> configlist = getMultiWindowConfig(type);
        if (configlist == null || str == null || str == null) {
            return false;
        }
        boolean isInList = configlist.contains(str);
        return isInList;
    }

    public boolean isAllowSplitApp(String packageName) {
        boolean isInListByType;
        synchronized (this) {
            isInListByType = isInListByType(1, packageName);
        }
        return isInListByType;
    }

    public boolean isNotAllowSplitApp(String packageName) {
        boolean isInListByType;
        synchronized (this) {
            isInListByType = isInListByType(2, packageName);
        }
        return isInListByType;
    }

    public boolean isForceFullscreenActivity(String fullActivityName) {
        boolean isInListByType;
        synchronized (this) {
            isInListByType = isInListByType(3, fullActivityName);
        }
        return isInListByType;
    }

    public boolean isForceResizableActivity(String fullActivityName) {
        boolean isInListByType;
        synchronized (this) {
            isInListByType = isInListByType(5, fullActivityName);
        }
        return isInListByType;
    }

    public boolean isForceMinMaxActivity(String fullActivityName) {
        boolean isInListByType;
        synchronized (this) {
            isInListByType = isInListByType(4, fullActivityName);
        }
        return isInListByType;
    }

    public boolean isNeedRelaunchApp(String packageName) {
        boolean isInListByType;
        synchronized (this) {
            isInListByType = isInListByType(16, packageName);
        }
        return isInListByType;
    }

    public boolean isNeededHideSoftBeforeSplitActivity(String fullActivityName) {
        boolean isInListByType;
        synchronized (this) {
            isInListByType = isInListByType(66, fullActivityName);
        }
        return isInListByType;
    }

    public boolean isNeedRelaunchActivity(String fullActivityName) {
        boolean isInListByType;
        synchronized (this) {
            isInListByType = isInListByType(17, fullActivityName);
        }
        return isInListByType;
    }

    public boolean isIgnoreRelaunchApp(String packageName) {
        boolean isInListByType;
        synchronized (this) {
            isInListByType = isInListByType(18, packageName);
        }
        return isInListByType;
    }

    public boolean isIgnoreRelaunchActivity(String fullActivityName) {
        boolean isInListByType;
        synchronized (this) {
            isInListByType = isInListByType(19, fullActivityName);
        }
        return isInListByType;
    }

    public boolean isIgnoreRelaunchActivityAlreadySplit(String fullActivityName) {
        boolean isInListByType;
        synchronized (this) {
            isInListByType = isInListByType(52, fullActivityName);
        }
        return isInListByType;
    }

    public boolean isNeedRelaunchOrientationActivity(String fullActivityName) {
        boolean isInListByType;
        synchronized (this) {
            isInListByType = isInListByType(20, fullActivityName);
        }
        return isInListByType;
    }

    public boolean isIgnoreRelaunchOrientationActivity(String fullActivityName) {
        boolean isInListByType;
        synchronized (this) {
            isInListByType = isInListByType(21, fullActivityName);
        }
        return isInListByType;
    }

    public boolean isDoubleResumeApp(String packageName) {
        boolean isInListByType;
        synchronized (this) {
            isInListByType = isInListByType(32, packageName);
        }
        return isInListByType;
    }

    public boolean isModifyTopTencentVersion(String version) {
        ArrayList<String> arrayList;
        if (this.mVivoMultiWindowSupport && version != null && (arrayList = this.mModifyTopInTencentVersion) != null) {
            int count = arrayList.size();
            for (int i = 0; i < count; i++) {
                String item = this.mModifyTopInTencentVersion.get(i);
                if (item != null && version.equals(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isMultiWindowConfigInitalization() {
        return this.mMultiWindowConfigInitalization;
    }

    public boolean isVivoAllowSplitInputMethod(String packageName) {
        boolean isInListByType;
        synchronized (this) {
            isInListByType = isInListByType(53, packageName);
        }
        return isInListByType;
    }

    public boolean isVivoAllowAllTypeDragIndMaskApp(String packageName) {
        boolean z = false;
        if (packageName == null) {
            return false;
        }
        synchronized (this) {
            if (isInListByType(54, packageName) || (packageName != null && packageName.contains(Constant.APP_WEIXIN))) {
                z = true;
            }
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isVivoMultiWindowSupport() {
        return this.mVivoMultiWindowSupport;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isVivoVosMultiWindowSupport() {
        return this.mVivoVosMultiWindowSupport;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isVivoOverVos2MultiWindowSupport() {
        return this.mVigourOverSeaVos2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isMultiWindowSupport() {
        return IS_VIVO_SUPPORT_MULTIWINDOW_PROPERTY;
    }

    void initVideoAppList() {
        if (this.mVideoAppList == null) {
            this.mVideoAppList = new ArrayList<>();
        }
        ArrayList<String> arrayList = this.mVideoAppList;
        if (arrayList != null) {
            arrayList.add("com.youku.phone/com.youku.ui.activity.DetailActivity");
            this.mVideoAppList.add("com.sohu.sohuvideo/.mvp.ui.activity.MediaVideoDetailActivity");
            this.mVideoAppList.add("com.sohu.sohuvideo/.mvp.ui.activity.VideoDetailActivity");
            this.mVideoAppList.add("com.qiyi.video");
            this.mVideoAppList.add("com.tencent.qqlive/.ona.activity.VideoDetailActivity");
            this.mVideoAppList.add("tv.danmaku.bili/com.bilibili.bililive.videoliveplayer.ui.live.roomv2.LiveNewRoomActivity");
            this.mVideoAppList.add("cn.cntv/.ui.activity.evening.SoireeActivity");
            this.mVideoAppList.add("com.pplive.androidphone/.ui.detail.ChannelDetailActivity");
            this.mVideoAppList.add("com.letv.android.client/.album.AlbumPlayActivity");
            this.mVideoAppList.add("com.android.VideoPlayer/.MovieViewActivity");
            this.mVideoAppList.add("com.android.VideoPlayer/com.vivo.video.local.localplayer.LocalInnerPlayerActivity");
            this.mVideoAppList.add("com.chaozh.iReader/com.zhangyue.iReader.read.ui.Activity_BookBrowser_TXT");
            this.mVideoAppList.add("com.android.settings/com.vivo.settings.secret.PasswordActivity");
            this.mVideoAppList.add("com.playit.videoplayer/com.quantum.player.ui.activities.MainActivity");
            this.mVideoAppList.add("com.google.android.youtube/com.google.android.apps.youtube.app.WatchWhileActivity");
            this.mVideoAppList.add("com.iqiyi.i18n/org.iqiyi.video.activity.PlayerActivity");
            this.mVideoAppList.add("com.android.VideoPlayer/com.android.VideoPlayer.MovieViewPublicActivity");
        }
    }

    public boolean isVideoAppRunning(String primaryApp, String secondaryApp) {
        ArrayList<String> arrayList;
        if (!IS_VIVO_SHOW_DIVIDER_BG_FORCE && primaryApp != null && secondaryApp != null && (arrayList = this.mVideoAppList) != null) {
            int count = arrayList.size();
            for (int i = 0; i < count; i++) {
                String item = this.mVideoAppList.get(i);
                if (item != null && (primaryApp.contains(item) || secondaryApp.contains(item))) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    void initIgnoreUpdateMultiModeWithInvisibleList() {
        if (this.mIgnoreUpdateMultiModeWithInvisibleList == null) {
            this.mIgnoreUpdateMultiModeWithInvisibleList = new ArrayList<>();
        }
        ArrayList<String> arrayList = this.mIgnoreUpdateMultiModeWithInvisibleList;
        if (arrayList != null) {
            arrayList.add("com.tencent.qqlive.ona.activity.VideoDetailActivity");
        }
    }

    public boolean isIgnoreUpdateMultiModeWithInvisibleActivity(String record) {
        ArrayList<String> arrayList = this.mIgnoreUpdateMultiModeWithInvisibleList;
        if (arrayList != null && !arrayList.isEmpty() && record != null) {
            int count = this.mIgnoreUpdateMultiModeWithInvisibleList.size();
            for (int i = 0; i < count; i++) {
                String item = this.mIgnoreUpdateMultiModeWithInvisibleList.get(i);
                if (item != null && record.contains(item)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    void initSpecialDockAppList() {
        if (this.mSpecialDockApp == null) {
            this.mSpecialDockApp = new ArrayList<>();
        }
        if (this.mSpecialSplashActivity == null) {
            this.mSpecialSplashActivity = new ArrayList<>();
        }
        ArrayList<String> arrayList = this.mSpecialDockApp;
        if (arrayList != null) {
            arrayList.add("cn.cntv/.");
        }
        ArrayList<String> arrayList2 = this.mSpecialSplashActivity;
        if (arrayList2 != null) {
            arrayList2.add("cn.cntv/.ui.activity.SplashActivity");
            this.mSpecialSplashActivity.add("com.android.settings/com.vivo.settings.secret.PasswordActivity");
            this.mSpecialSplashActivity.add("com.android.settings/com.vivo.settings.secret.PasswordActivityUD");
        }
    }

    public boolean isSpecialSplashActivity(String runningApp) {
        ArrayList<String> arrayList;
        if (runningApp != null && (arrayList = this.mSpecialSplashActivity) != null) {
            int count = arrayList.size();
            for (int i = 0; i < count; i++) {
                String item = this.mSpecialSplashActivity.get(i);
                if (item != null && runningApp.contains(item)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public boolean isSpecialDockApp(String runningApp) {
        ArrayList<String> arrayList;
        if (runningApp != null && (arrayList = this.mSpecialDockApp) != null) {
            int count = arrayList.size();
            for (int i = 0; i < count; i++) {
                String item = this.mSpecialDockApp.get(i);
                if (item != null && runningApp.contains(item)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    void initLayoutIncludeNavApp() {
        if (this.mLayoutIncludeNavApp == null) {
            this.mLayoutIncludeNavApp = new ArrayList<>();
        }
        ArrayList<String> arrayList = this.mLayoutIncludeNavApp;
        if (arrayList != null) {
            arrayList.add("com.vivo.upslide");
        }
    }

    public boolean isLayoutIncludeNavApp(String runningApp) {
        ArrayList<String> arrayList;
        if (runningApp != null && (arrayList = this.mLayoutIncludeNavApp) != null) {
            int count = arrayList.size();
            for (int i = 0; i < count; i++) {
                String item = this.mLayoutIncludeNavApp.get(i);
                if (item != null && runningApp.contains(item)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    void initSpecialFreezingActviityList() {
        if (this.mSpecialFreezingActivityList == null) {
            this.mSpecialFreezingActivityList = new ArrayList<>();
        }
        ArrayList<String> arrayList = this.mSpecialFreezingActivityList;
        if (arrayList != null) {
            arrayList.add("com.tencent.mm/.ui.transmit.SelectConversationUI");
        }
    }

    public boolean isSpecialFreezingActivity(String record) {
        ArrayList<String> arrayList = this.mSpecialFreezingActivityList;
        if (arrayList != null && !arrayList.isEmpty() && record != null) {
            int count = this.mSpecialFreezingActivityList.size();
            for (int i = 0; i < count; i++) {
                String item = this.mSpecialFreezingActivityList.get(i);
                if (item != null && record.contains(item)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    void initForceHideStatusbarJustAfterExitSplitList() {
        if (this.mForceHideStatusJustAfterExitSplitList == null) {
            this.mForceHideStatusJustAfterExitSplitList = new ArrayList<>();
        }
        ArrayList<String> arrayList = this.mForceHideStatusJustAfterExitSplitList;
        if (arrayList != null) {
            arrayList.add("com.tencent.qqlive.ona.activity.VideoDetailActivity");
        }
    }

    public boolean isForceHideStatusbarJustAfterExitSplit(String inputWinStateStr) {
        ArrayList<String> arrayList = this.mForceHideStatusJustAfterExitSplitList;
        if (arrayList != null && !arrayList.isEmpty() && inputWinStateStr != null) {
            int count = this.mForceHideStatusJustAfterExitSplitList.size();
            for (int i = 0; i < count; i++) {
                String item = this.mForceHideStatusJustAfterExitSplitList.get(i);
                if (item != null && inputWinStateStr.contains(item)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public boolean isNotIgnoreRelaunchActivityWhenFloatMsg(String activity) {
        if (activity != null && activity.equals("com.tencent.mobileqq/.activity.SplashActivity")) {
            return true;
        }
        return false;
    }

    public boolean isUseMiddleTargetAfterRotationPackage(String packageName) {
        if (packageName != null) {
            if (packageName.equals(SMART_MULTIWINDOW_NAME) || packageName.equals("com.vivo.upslide")) {
                return true;
            }
            return false;
        }
        return false;
    }

    public static boolean isMatchDockNavPos(int dockSide, DisplayInfo displayInfo, int navPos, int activityType, int windowingmode) {
        if (activityType == 2 && windowingmode == 1) {
            if (DEBUG) {
                VSlog.i(TAG, "split-screen-navcolor isMatchDockNavPos this is third home and full screen show");
            }
            return true;
        } else if (displayInfo == null) {
            return false;
        } else {
            if (displayInfo.logicalWidth > displayInfo.logicalHeight) {
                if (1 == dockSide && navPos == 1) {
                    return true;
                }
                if (3 == dockSide && navPos == 2) {
                    return true;
                }
            } else if (4 == dockSide && navPos == 4) {
                return true;
            }
            return false;
        }
    }

    public boolean adjustStartFlagsPackageIfNeededInSplit(String packageName) {
        if (packageName != null && packageName.equals("cn.wps.moffice_eng")) {
            return true;
        }
        return false;
    }

    public boolean skipPipIfNeededInSplit(String name) {
        if (name != null) {
            if (name.contains("com.android.chrome/com.google.android.apps.chrome.Main") || name.contains("com.google.android.apps.maps/com.google.android.maps.MapsActivity") || name.contains("com.playit.videoplayer/com.quantum.player.ui.activities.MainActivity") || name.contains("com.google.android.youtube/com.google.android.apps.youtube.app.WatchWhileActivity") || name.contains("com.whatsapp/.voipcalling.VoipActivityV2")) {
                return true;
            }
            return false;
        }
        return false;
    }

    public void splitBackEventThreadRun(final Context context) {
        Handler handler;
        if (!this.mIsOverseas && (handler = this.mHandler) != null) {
            handler.post(new Runnable() { // from class: com.android.server.wm.-$$Lambda$VivoMultiWindowConfig$d-A_4YPk221mCAGIZknoERw2VwA
                @Override // java.lang.Runnable
                public final void run() {
                    VivoMultiWindowConfig.this.lambda$splitBackEventThreadRun$0$VivoMultiWindowConfig(context);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: splitBackExiEventProcess */
    public void lambda$splitBackEventThreadRun$0$VivoMultiWindowConfig(Context context) {
        try {
            VivoCollectData eventInstance = VivoCollectData.getInstance(context);
            isSplitScreenEventEnable = eventInstance.getControlInfo("1072");
            if (DEBUG) {
                VSlog.i(TAG, "attachStack isSplitScreenEventEnable = " + isSplitScreenEventEnable);
            }
            if (isSplitScreenEventEnable) {
                HashMap<String, String> params = new HashMap<>();
                params.put(VCD_FF_1.UUID_STR, UUID.randomUUID().toString());
                params.put("quit", "1");
                params.put("way", "4");
                EventTransfer.getInstance().singleEvent("1072", "107254", System.currentTimeMillis(), 0L, params);
                if (DEBUG) {
                    VSlog.d(TAG, "split_info:107254 -- exit docked stack 4 : params = " + params);
                }
            }
        } catch (Exception exception) {
            VSlog.e(TAG, "splitBackExit event processing Error e = " + exception);
            exception.printStackTrace();
        }
    }

    public void splitDragEventThreadRun(final Context context, final String dragPackage, final String dropPackage) {
        Handler handler;
        if (!this.mIsOverseas && (handler = this.mHandler) != null) {
            handler.post(new Runnable() { // from class: com.android.server.wm.-$$Lambda$VivoMultiWindowConfig$f0YHTR7HgWwBogYGdfMPAuR2MF4
                @Override // java.lang.Runnable
                public final void run() {
                    VivoMultiWindowConfig.this.lambda$splitDragEventThreadRun$1$VivoMultiWindowConfig(context, dragPackage, dropPackage);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: splitDragEventProcess */
    public void lambda$splitDragEventThreadRun$1$VivoMultiWindowConfig(Context context, String drapPackage, String dropPackage) {
        try {
            VivoCollectData eventInstance = VivoCollectData.getInstance(context);
            isSplitScreenEventEnable = eventInstance.getControlInfo("1072");
            if (DEBUG) {
                VSlog.i(TAG, "attachStack isSplitScreenEventEnable = " + isSplitScreenEventEnable);
            }
            if (isSplitScreenEventEnable) {
                HashMap<String, String> params = new HashMap<>();
                params.put(VCD_FF_1.UUID_STR, UUID.randomUUID().toString());
                params.put(KEY_MULT_DRAG, "1");
                params.put(KEY_MULT_DRAG_APP, drapPackage);
                params.put(KEY_MULT_DROP_APP, dropPackage);
                EventTransfer.getInstance().singleEvent("1072", SUB_EVENT_ID_DRAG, System.currentTimeMillis(), 0L, params);
                if (DEBUG) {
                    VSlog.d(TAG, "split_info:107293 -- split drag event : params = " + params);
                }
            }
        } catch (Exception exception) {
            VSlog.e(TAG, "splitdrag devent processing Error e = " + exception);
            exception.printStackTrace();
        }
    }

    void initForceCutoutAppList() {
        if (this.mForceCutoutAppList == null) {
            this.mForceCutoutAppList = new ArrayList<>();
        }
        ArrayList<String> arrayList = this.mForceCutoutAppList;
        if (arrayList != null) {
            arrayList.add(Constant.APP_WEIXIN);
            this.mForceCutoutAppList.add("com.google.android.youtube");
        }
    }

    public boolean isForceCutoutApp(String app) {
        ArrayList<String> arrayList;
        if (IS_VIVO_SPLIT_FORCE_LAND_CUTOUT && app != null && (arrayList = this.mForceCutoutAppList) != null) {
            int count = arrayList.size();
            for (int i = 0; i < count; i++) {
                String item = this.mForceCutoutAppList.get(i);
                if (item != null && item.equals(app)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    void initForceIgnoreRelaunchAfterBackActivityList() {
        if (this.mForceIgnoreRelaunchAfterBackActivityList == null) {
            this.mForceIgnoreRelaunchAfterBackActivityList = new ArrayList<>();
        }
        ArrayList<String> arrayList = this.mForceIgnoreRelaunchAfterBackActivityList;
        if (arrayList != null) {
            arrayList.add("com.tencent.mm/.plugin.sns.ui.SnsTimeLineUI");
        }
    }

    public boolean isForceIgnoreRelaunchAfterBackActivity(String activity) {
        ArrayList<String> arrayList;
        if (IS_VIVO_SPLIT_FORCE_IGNORE_AFTER_BACK && activity != null && (arrayList = this.mForceIgnoreRelaunchAfterBackActivityList) != null) {
            int count = arrayList.size();
            for (int i = 0; i < count; i++) {
                String item = this.mForceIgnoreRelaunchAfterBackActivityList.get(i);
                if (item != null && item.equals(activity)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    void initAvoidMoveFocusToPrimaryAcitvity() {
        if (this.mAvoidMoveFocusToPrimaryAcitvity == null) {
            this.mAvoidMoveFocusToPrimaryAcitvity = new ArrayList<>();
        }
        ArrayList<String> arrayList = this.mAvoidMoveFocusToPrimaryAcitvity;
        if (arrayList != null) {
            arrayList.add("com.android.settings/com.vivo.settings.secret.PasswordActivity");
        }
    }

    public boolean isAvoidMoveFocusToPrimaryAcitvity(String activity) {
        ArrayList<String> arrayList;
        if (activity != null && (arrayList = this.mAvoidMoveFocusToPrimaryAcitvity) != null) {
            int count = arrayList.size();
            for (int i = 0; i < count; i++) {
                String item = this.mAvoidMoveFocusToPrimaryAcitvity.get(i);
                if (item != null && activity.contains(item)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public boolean reparentChildrenWhenImmediateDestroy(String sc) {
        if (sc != null) {
            if (sc.contains("com.tencent.qqlive") || sc.contains("com.qiyi.video") || sc.contains("com.youku.phone") || sc.contains("com.iqiyi.i18n") || sc.contains("com.android.VideoPlayer") || sc.contains(Constant.APP_DOUYU) || sc.contains("com.autonavi.minimap") || sc.contains("com.chaozh.iReader") || sc.contains("com.google.android.youtube")) {
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean skipSetVisableWhenFloatMessage(String packageName) {
        if (packageName != null) {
            if (packageName.equals(SMART_MULTIWINDOW_NAME) || packageName.equals("com.vivo.upslide") || packageName.equals("com.bbk.launcher2") || packageName.equals(VivoNotificationManagerServiceImpl.PKG_LAUNCHER)) {
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean isForceNotStartHomeAndBackApps(String packageName) {
        if (packageName != null && packageName.equals(Constant.APP_WEIXIN)) {
            return true;
        }
        return false;
    }

    public static void checkSplitThreadLockIfEnabled(String funcTag, Object lockObj) {
        if (ifCheckLockDebug) {
            VSlog.i(TAG, funcTag + " checkSplitThreadLock " + Thread.holdsLock(lockObj) + " thread name =" + Thread.currentThread().getName() + " thread id = " + Thread.currentThread().getId());
        }
    }

    void dumpCurrentConfig() {
        VSlog.v(TAG, "MultiWindowConfig:");
        VSlog.v(TAG, "        IS_VIVO_SUPPORT_MULTIWINDOW_PROPERTY:" + IS_VIVO_SUPPORT_MULTIWINDOW_PROPERTY);
        VSlog.v(TAG, "        IS_VIVO_SUPPORT_MULTIWINDOW_DEBUG_PROPERTY:" + IS_VIVO_SUPPORT_MULTIWINDOW_DEBUG_PROPERTY);
        VSlog.v(TAG, "        IS_VIVO_AM_RESIZABLE_PROPERTY:" + IS_VIVO_AM_RESIZABLE_PROPERTY);
        VSlog.v(TAG, "        IS_VIVO_AM_RELAUNCH_PROPERTY:" + IS_VIVO_AM_RELAUNCH_PROPERTY);
        VSlog.v(TAG, "        IS_VIVO_DOUBLE_RESUME_PROPERTY:" + IS_VIVO_DOUBLE_RESUME_PROPERTY);
        VSlog.v(TAG, "        IS_VIVO_ROTATE_FREE:" + IS_VIVO_ROTATE_FREE);
        VSlog.v(TAG, "        IS_VIVO_DEFER_ROTATE:" + IS_VIVO_DEFER_ROTATE);
        VSlog.v(TAG, "        IS_VIVO_SPLIT_NAV_COLOR:" + IS_VIVO_SPLIT_NAV_COLOR);
        VSlog.v(TAG, "        IS_VIVO_SPLIT_BACK_PROCESS:" + IS_VIVO_SPLIT_BACK_PROCESS);
        VSlog.v(TAG, "        TIERLEVEL:" + this.mTierLevel);
        VSlog.v(TAG, "        ISOVERSEAS:" + this.mIsOverseas);
    }

    public static void updateDebugConfigs(boolean logOpen) {
        SYS_LOG_CTRL = logOpen;
        boolean equals = "yes".equals(SystemProperties.get("persist.multiwindowlog.debug", "no"));
        DEBUG_LOG_CTRL = equals;
        DEBUG = SYS_LOG_CTRL || equals;
    }

    public static boolean isDebugAllPrivateInfo() {
        return DEBUG_ALL_SPLIT_PRIV_LOG;
    }
}