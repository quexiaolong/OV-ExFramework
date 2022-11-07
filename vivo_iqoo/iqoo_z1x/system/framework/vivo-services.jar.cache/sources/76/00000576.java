package com.android.server.wm;

import android.app.AppGlobals;
import android.app.ContentProviderHolder;
import android.app.IActivityManager;
import android.app.IApplicationThread;
import android.app.IUserSwitchObserver;
import android.app.WallpaperInfo;
import android.app.servertransaction.ConfigurationChangeItem;
import android.bluetooth.IBluetoothManagerCallback;
import android.bluetooth.IBluetoothStateChangeCallback;
import android.content.ContentResolver;
import android.content.pm.ApplicationInfo;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.graphics.common.V1_0.Dataspace;
import android.location.LocationRequest;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.MergedConfiguration;
import android.util.SparseArray;
import android.view.DisplayInfo;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;
import com.android.internal.util.ArrayUtils;
import com.android.server.IVivoStats;
import com.android.server.am.ActiveServices;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.ProcessRecord;
import com.android.server.am.ServiceRecord;
import com.android.server.am.VivoFrozenPackageSupervisor;
import com.android.server.am.firewall.VivoFirewall;
import com.android.server.content.SyncOperation;
import com.android.server.display.DisplayManagerService;
import com.android.server.display.VirtualDisplayAdapter;
import com.android.server.location.LocationManagerService;
import com.android.server.power.PowerManagerService;
import com.android.server.wallpaper.WallpaperManagerService;
import com.android.server.wm.ActivityStack;
import com.vivo.common.utils.VLog;
import com.vivo.framework.pem.VivoStats;
import com.vivo.framework.pem.VivoStatsImpl;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/* loaded from: classes.dex */
public final class VivoStatsInServerImpl extends VivoStatsImpl implements IVivoStats {
    private static final int CUR_ENTER_SPLIT_MODE = 1;
    private static final int CUR_EXIT_SPLIT_MODE = 0;
    private static final int MSG_PIP = 45;
    private static final String SPLIT_PANEL_MODE_CHANGE_URI = "in_multi_window";
    private static VivoStatsInServerImpl _instanceExt;
    private final int MSG_ADD_WIN;
    private final int MSG_DOCKED;
    private final int MSG_REMOVE_WIN;
    private final int MSG_VPN;
    private final int MSG_WIN_HIDE;
    private final int MSG_WIN_SHOW;
    private final int WHICH_ACTIVITY;
    private final int WHICH_PID_DEL;
    private boolean hasRegistedSplitScreenModeObserver;
    private boolean isNoScaled;
    private ActivityManagerService mAms;
    private ContentResolver mContentResolver;
    private boolean mFirstTouch;
    private int mFlags;
    private final HashMap<Object, LocationManagerService.Receiver> mFrozenReceivers;
    private final ArrayList<PowerManagerService.WakeLock> mFrozenWakeLocks;
    private int mLastHashCore;
    private int mLastTouchAction;
    private int mNoteUid;
    private final HashMap<Integer, PidMsg> mScaledPids;
    private final HashMap<String, PidMsg> mScaledProcessNames;
    private VivoFrozenPackageSupervisor mVivoFrozenPackageSupervisor;
    private WindowManagerService mWms;

    private VivoStatsInServerImpl() {
        super(1);
        this.MSG_ADD_WIN = 31;
        this.MSG_REMOVE_WIN = 32;
        this.MSG_WIN_SHOW = 33;
        this.MSG_WIN_HIDE = 34;
        this.MSG_DOCKED = 40;
        this.WHICH_PID_DEL = 42;
        this.MSG_VPN = 46;
        this.WHICH_ACTIVITY = 48;
        this.mLastHashCore = 0;
        this.isNoScaled = false;
        this.mFirstTouch = false;
        this.mScaledPids = new HashMap<>();
        this.mScaledProcessNames = new HashMap<>();
        this.mNoteUid = -1;
        this.mLastTouchAction = -1;
        this.mFrozenWakeLocks = new ArrayList<>();
        this.mFrozenReceivers = new HashMap<>();
        this.mContentResolver = null;
        this.hasRegistedSplitScreenModeObserver = false;
        this.mVivoFrozenPackageSupervisor = VivoFrozenPackageSupervisor.getInstance();
    }

    public static VivoStatsInServerImpl getInstance() {
        if (_instanceExt == null) {
            _instanceExt = new VivoStatsInServerImpl();
        }
        return _instanceExt;
    }

    public void setWhichDocked(boolean exist) {
        if (VivoStats.mDocked != exist) {
            VivoStats.mDocked = exist;
            if (run[40]) {
                mHandler.post(new VivoStats.WhichMsgRunThreeIntTwo(40, exist ? 1 : 0, VivoStats.mStackId, VivoStats.mLastUid, VivoStats.mActivity_name, VivoStats.mPkgName));
            }
            if (DEBUG) {
                VLog.v("VivoStats", "noteWhichDockedState exist = " + exist + ", mStackId = " + VivoStats.mStackId + ", mLastUid = " + VivoStats.mLastUid + ", pkg = " + VivoStats.mPkgName + ", activity = " + VivoStats.mActivity_name);
            }
        }
    }

    public void noteHoldVpn(NetworkInfo.DetailedState detailedState, String reason, int ownerUID) {
        if (DEBUG) {
            VLog.v("VivoStats", "noteHoldVpn uid = " + ownerUID + ", detailedState = " + detailedState + ", reason = " + reason);
        }
        if (run[46]) {
            if (detailedState == NetworkInfo.DetailedState.CONNECTED) {
                mHandler.post(new VivoStats.WhichMsgRun(46, ownerUID, 1, reason));
            } else if (detailedState == NetworkInfo.DetailedState.DISCONNECTED) {
                mHandler.post(new VivoStats.WhichMsgRun(46, ownerUID, 0, reason));
            }
        }
    }

    public void removeOldProcessLocked(ProcessRecord app, int uid) {
        if (app != null) {
            if (run[42]) {
                mHandler.post(new VivoStats.WhichMsgRun(42, app.pid, uid, "startold"));
            }
            if (DEBUG) {
                VLog.v("VivoStats", "removeOldProcessLocked, pid = " + app.pid + ", uid = " + uid + ", reason = startold");
            }
            this.mVivoFrozenPackageSupervisor.noteProcessDied(app.pid, uid, app, "startold");
        }
    }

    public void noteProcessDied(int pid, int uid, String reason, ProcessRecord app) {
        noteWhich(42, pid, uid, reason);
        this.mVivoFrozenPackageSupervisor.noteProcessDied(pid, uid, app, reason);
    }

    public void noteProcessAdd(int pid, int uid, String processName, String packageName, String reason) {
        noteWhich(41, pid, uid, processName, packageName);
        this.mVivoFrozenPackageSupervisor.noteProcessAdd(pid, uid, packageName, processName, reason);
    }

    public void noteProcessCreate(int pid, int uid, String processName, String packageName, String reason) {
        if (processName != null) {
            this.mVivoFrozenPackageSupervisor.noteProcessAdd(pid, uid, packageName, processName, reason);
        }
    }

    public boolean getFirstTouch() {
        return this.mFirstTouch;
    }

    public void setFirstTouch(boolean firstTouch) {
        this.mFirstTouch = firstTouch;
    }

    public void noteInput(int event, int uid) {
        if (this.mFirstTouch && event == 2) {
            this.mFirstTouch = false;
            note(17, -1, uid);
        }
    }

    public void noteInput(int keyCode, boolean flag, boolean down) {
        if (down) {
            note(17, keyCode, flag ? 1 : 0);
        }
    }

    public void onInputEvent(MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        if (action != 0) {
            if (action == 1) {
                note(23, 2, 0);
            } else if (action == 2 && this.mLastTouchAction != action) {
                note(23, 3, 0);
            }
        } else {
            note(23, 1, 0);
        }
        this.mLastTouchAction = action;
        this.mFlags = motionEvent.getFlags();
    }

    public void noteAddWindow(WindowManager.LayoutParams attrs, int pid, int uid) {
        if (run[31] && attrs != null && attrs.type >= 2000 && attrs.type <= 2999) {
            mHandler.post(new VivoStats.WhichMsgRunThreeInt(31, attrs.type, uid, pid, attrs.packageName));
            if (DEBUG) {
                VLog.v("VivoStats", "noteAddWindow, pid = " + pid + ", uid = " + uid + ", attrs.type = " + attrs.type + ", pkg = " + attrs.packageName);
            }
        }
    }

    public void removeWindow(WindowManager.LayoutParams attrs, int pid, int uid) {
        if (run[32] && attrs != null && attrs.type >= 2000 && attrs.type <= 2999) {
            mHandler.post(new VivoStats.WhichMsgRunThreeInt(32, attrs.type, uid, pid, attrs.packageName));
            if (DEBUG) {
                VLog.v("VivoStats", "removeWindow, pid = " + pid + ", uid = " + uid + ", attrs.type = " + attrs.type + ", pkg = " + attrs.packageName);
            }
        }
    }

    public void noteWindowShow(WindowManager.LayoutParams attrs, WindowState win, int pid, int uid, int W, int H) {
        if (run[33] && win.getNeedNote() && attrs.type >= 2000 && attrs.type <= 2999 && W >= 10 && H >= 10) {
            win.setNeedNote(false);
            mHandler.post(new VivoStats.WhichMsgRunThreeInt(33, attrs.type, uid, pid, attrs.packageName));
            if (DEBUG) {
                VLog.v("VivoStats", "noteWindowShow, pid = " + pid + ", uid = " + uid + ", attrs.type = " + attrs.type + ", pkg = " + attrs.packageName);
            }
        }
    }

    public void noteWindowHide(WindowManager.LayoutParams attrs, WindowState win, int pid, int uid, int W, int H) {
        if (run[34] && attrs.type >= 2000 && attrs.type <= 2999 && W >= 10 && H >= 10) {
            win.setNeedNote(true);
            mHandler.post(new VivoStats.WhichMsgRunThreeInt(34, attrs.type, uid, pid, attrs.packageName));
            if (DEBUG) {
                VLog.v("VivoStats", "noteWindowHide, pid = " + pid + ", uid = " + uid + ", attrs.type = " + attrs.type + ", pkg = " + attrs.packageName);
            }
        }
    }

    public void noteWallpaper(SparseArray<WallpaperManagerService.WallpaperData> wallpaperMap, int userId) {
        WallpaperInfo wallpaperInfo;
        int noteUid = 0;
        try {
            WallpaperManagerService.WallpaperData bwallpaper = wallpaperMap.get(userId);
            if (bwallpaper != null && bwallpaper.connection != null && (wallpaperInfo = bwallpaper.connection.mInfo) != null && wallpaperInfo.getServiceInfo() != null) {
                noteUid = wallpaperInfo.getServiceInfo().applicationInfo.uid;
            }
            if (this.mNoteUid != noteUid) {
                this.mNoteUid = noteUid;
                note(18, noteUid, 0);
            }
        } catch (Exception e) {
            VLog.e("VivoStats", "notePem:", e);
        }
    }

    public void noteWhichActivity(ActivityRecord next, int stackId) {
        int stackId2;
        String action;
        if (VivoStats.mSupportPemSystemStatus && next != null) {
            int curHashCode = next.hashCode();
            if (curHashCode != this.mLastHashCore && next.mActivityComponent != null && next.info != null && next.info.applicationInfo != null) {
                this.mLastHashCore = curHashCode;
                String pkgName = next.info.applicationInfo.packageName;
                if (next.intent == null) {
                    stackId2 = stackId;
                } else {
                    String action2 = next.intent.getAction();
                    if (action2 == null || !action2.startsWith("APAP-")) {
                        stackId2 = stackId;
                    } else {
                        String[] praseAction = action2.split("-");
                        if (praseAction.length <= 2) {
                            stackId2 = stackId;
                        } else {
                            stackId2 = stackId + 10000;
                            VLog.v("VivoStats", "for pem: action " + action2);
                            pkgName = praseAction[1] + "-pem-" + praseAction[2];
                        }
                    }
                    if (stackId2 < 10000 && (action = next.intent.getType()) != null) {
                        String[] praseType = action.split("/");
                        if (praseType.length > 1) {
                            stackId2 += 10000;
                            VLog.v("VivoStats", "for pem: type " + action);
                            pkgName = praseType[0] + "-pem-" + praseType[1];
                        }
                    }
                }
                VivoStats.mPkgName = pkgName;
                VivoStats.mStackId = stackId2;
                VivoStats.mLastUid = next.info.applicationInfo.uid;
                VivoStats.mActivity_name = next.mActivityComponent.getClassName();
                VivoStats.mCool = next.getState() == ActivityStack.ActivityState.INITIALIZING ? 1 : 0;
                if (run[48]) {
                    mHandler.post(new VivoStats.WhichMsgRunThreeIntTwo(48, VivoStats.mLastUid, stackId2, VivoStats.mCool, VivoStats.mActivity_name, pkgName));
                }
                if (DEBUG) {
                    VLog.v("VivoStats", "noteWhichActivity uid = " + VivoStats.mLastUid + ", stackId = " + stackId2 + ", activity = " + VivoStats.mActivity_name + ", pkgName = " + pkgName + ", cool = " + VivoStats.mCool);
                }
            }
        }
    }

    public static void setWhichPIP(boolean exist) {
        if (VivoStats.mPIP != exist) {
            VivoStats.mPIP = exist;
            if (run[45]) {
                mHandler.post(new VivoStats.WhichMsgRunThreeIntTwo(45, exist ? 1 : 0, VivoStats.mStackId, VivoStats.mLastUid, VivoStats.mActivity_name, VivoStats.mPkgName));
            }
            VLog.v("VivoStats", "noteWhichPIPState exist = " + exist + ", mStackId = " + VivoStats.mStackId + ", mLastUid = " + VivoStats.mLastUid + ", pkg = " + VivoStats.mPkgName + ", activity = " + VivoStats.mActivity_name);
        }
    }

    public float getScale(ProcessRecord app, CompatibilityInfo cInfo, ApplicationInfo aInfo, int uid, Configuration newConfig) {
        String pkgName;
        VivoStats.UidCot u;
        if (mNoSupportPemDDC || this.isNoScaled) {
            return 1.0f;
        }
        boolean toScale = true;
        try {
            if (aInfo != null) {
                pkgName = aInfo.packageName;
                if (cInfo != null && (cInfo.isScalingRequired() || !cInfo.supportsScreen())) {
                    toScale = false;
                }
            } else {
                pkgName = app.processName;
            }
            if (toScale && (u = (VivoStats.UidCot) VivoStats.mResList.get(uid)) != null) {
                if (u.pkg != null) {
                    if (pkgName == null) {
                        return 1.0f;
                    }
                    int len = u.pkg.length - 1;
                    for (int i = 0; i <= len && !pkgName.equals(u.pkg[i]); i++) {
                        if (i == len) {
                            return 1.0f;
                        }
                    }
                }
                if (u.density != VivoStats.mCurDensity) {
                    PidMsg pmsg = new PidMsg();
                    pmsg.scale = u.density / VivoStats.mCurDensity;
                    pmsg.uidcot = u;
                    this.mScaledPids.put(Integer.valueOf(app.pid), pmsg);
                    this.mScaledProcessNames.put(app.processName, pmsg);
                    VLog.v("VivoStats", app.processName + " is will be scale to " + pmsg.scale + ", uid = " + uid + ", pid = " + app.pid + ", pkg = " + pkgName);
                    newConfig.densityDpi = (int) ((pmsg.scale * ((float) newConfig.densityDpi)) + 0.5f);
                    if (newConfig.windowConfiguration != null && newConfig.windowConfiguration.getAppBounds() != null) {
                        newConfig.windowConfiguration.getAppBounds().scale(pmsg.scale);
                    }
                    return pmsg.scale;
                }
            }
        } catch (Exception e) {
            VLog.e("VivoStats", "getScale error on " + app.processName + ", uid = " + uid + ", pid = " + app.pid, e);
        }
        return 1.0f;
    }

    public float getScaleFactor(WindowState win, float scale) {
        if (mSupportPemDDC && win.getScaleEnabled()) {
            return win.getScale();
        }
        return scale;
    }

    public void addWindowChangeAttrs(int pid, WindowManager.LayoutParams attrs) {
        if (attrs.type != 3 && this.mScaledPids.containsKey(Integer.valueOf(pid))) {
            attrs.privateFlags = 16777216 | attrs.privateFlags;
        }
    }

    public void changeWindowInsets(WindowState w) {
        if (mSupportPemDDC && w.getScaleEnabled()) {
            float scale = w.getScale();
            w.mGivenContentInsets.scale(1.0f / scale);
            w.mGivenVisibleInsets.scale(1.0f / scale);
            w.mGivenTouchableRegion.scale(1.0f / scale);
        }
    }

    public MergedConfiguration getWinMergedConfiguration(WindowState win, Object rooto, MergedConfiguration oldConfiguration) {
        if (mSupportPemDDC && win.getScaleEnabled()) {
            MergedConfiguration originMergedConfiguration = new MergedConfiguration();
            RootWindowContainer root = (RootWindowContainer) rooto;
            originMergedConfiguration.setConfiguration(root.getConfiguration(), win.getMergedOverrideConfiguration());
            return originMergedConfiguration;
        }
        return oldConfiguration;
    }

    public float overridePointer(WindowState win, float position) {
        if (mSupportPemDDC && win.getScaleEnabled()) {
            return position / win.getScale();
        }
        return position;
    }

    public Configuration getGlobalOverrideConfiguration(Configuration config, String processName) {
        PidMsg pmsg;
        if (mSupportPemDDC && processName != null && (pmsg = this.mScaledProcessNames.get(processName)) != null) {
            config.densityDpi = (int) ((pmsg.scale * config.densityDpi) + 0.5f);
            if (config.windowConfiguration != null && config.windowConfiguration.getAppBounds() != null) {
                config.windowConfiguration.getAppBounds().scale(pmsg.scale);
            }
        }
        return config;
    }

    public boolean intercept(Configuration config, String name, ActivityTaskManagerService atm, IApplicationThread thread, WindowProcessController mControl) throws RemoteException {
        PidMsg pmsg;
        if (mSupportPemDDC && name != null && (pmsg = this.mScaledProcessNames.get(name)) != null) {
            Configuration newConfig = new Configuration(config);
            newConfig.densityDpi = (int) ((pmsg.scale * newConfig.densityDpi) + 0.5f);
            if (newConfig.windowConfiguration != null && newConfig.windowConfiguration.getAppBounds() != null) {
                newConfig.windowConfiguration.getAppBounds().scale(pmsg.scale);
            }
            newConfig.seq = atm.increaseConfigurationSeqLocked();
            atm.getLifecycleManager().scheduleTransaction(thread, ConfigurationChangeItem.obtain(newConfig));
            mControl.setLastReportedConfiguration(newConfig);
            return true;
        }
        return false;
    }

    public void updateMatrix(Matrix tmpMatrix, WindowState win) {
        if (mSupportPemDDC && win.getScaleEnabled()) {
            float scale = 1.0f / win.getScale();
            tmpMatrix.postScale(scale, scale);
        }
    }

    public float getDsDxDy(WindowState win, float dsDx) {
        if (mSupportPemDDC && win.getScaleEnabled()) {
            return dsDx / win.getScale();
        }
        return dsDx;
    }

    public void updateScaleEnableStatus(WindowManager.LayoutParams attrs, int pid, IVivoWindowState iWin) {
        PidMsg pmsg;
        if (iWin != null && (16777216 & attrs.privateFlags) != 0 && (pmsg = this.mScaledPids.get(Integer.valueOf(pid))) != null && pmsg.scale != 1.0f && pmsg.scale != 0.0f) {
            iWin.setScale(pmsg.scale);
            iWin.setScaleEnabled(true);
        }
    }

    public void updateWindowFrames(WindowFrames windowFrames, IVivoWindowState iWin) {
        if (iWin != null && iWin.getScaleEnabled()) {
            windowFrames.scaleInsets(iWin.getScale());
            windowFrames.mCompatFrame.scale(iWin.getScale());
        }
    }

    public void updateWindowFrames(WindowFrames windowFrames, IVivoWindowState iWin, boolean inSizeCompatMode) {
        if (!inSizeCompatMode && iWin != null && iWin.getScaleEnabled()) {
            windowFrames.mCompatFrame.scale(iWin.getScale());
        }
    }

    public boolean interceptGetMC(Configuration gConfig, Configuration oConfig, MergedConfiguration out, IVivoWindowState iWin) {
        if (iWin != null && iWin.getScaleEnabled()) {
            float scale = iWin.getScale();
            Configuration tempGlobalConfig = new Configuration(gConfig);
            Configuration tempOverrideConfig = new Configuration(oConfig);
            tempGlobalConfig.densityDpi = (int) ((tempGlobalConfig.densityDpi * scale) + 0.5f);
            tempOverrideConfig.densityDpi = (int) ((tempOverrideConfig.densityDpi * scale) + 0.5f);
            if (tempGlobalConfig.windowConfiguration != null && tempGlobalConfig.windowConfiguration.getAppBounds() != null) {
                tempGlobalConfig.windowConfiguration.getAppBounds().scale(scale);
            }
            if (tempOverrideConfig.windowConfiguration != null && tempOverrideConfig.windowConfiguration.getAppBounds() != null) {
                tempOverrideConfig.windowConfiguration.getAppBounds().scale(scale);
            }
            out.setConfiguration(tempGlobalConfig, tempOverrideConfig);
            return true;
        }
        return false;
    }

    public Configuration getConfig(Configuration config, IVivoWindowState iWin) {
        if (iWin != null && iWin.getScaleEnabled()) {
            Configuration tmpConfig = new Configuration(config);
            tmpConfig.densityDpi = (int) ((tmpConfig.densityDpi * iWin.getScale()) + 0.5f);
            if (tmpConfig.windowConfiguration != null && tmpConfig.windowConfiguration.getAppBounds() != null) {
                tmpConfig.windowConfiguration.getAppBounds().scale(iWin.getScale());
            }
            return tmpConfig;
        }
        return config;
    }

    public int update(int value, IVivoWindowState iWin) {
        if (iWin != null && iWin.getScaleEnabled()) {
            return (int) ((value / iWin.getScale()) + 0.5f);
        }
        return value;
    }

    public float update(float value, IVivoWindowState iWin) {
        if (iWin != null && iWin.getScaleEnabled()) {
            return value / iWin.getScale();
        }
        return value;
    }

    public float getWinXY(float winX, WindowState win) {
        if (mSupportPemDDC && win.getScaleEnabled()) {
            return win.getScale() * winX;
        }
        return winX;
    }

    public void updateSystemDecorRect(Rect systemDecorRect, IVivoWindowState iWin) {
        if (iWin != null && iWin.getScaleEnabled()) {
            float scale = iWin.getScale();
            systemDecorRect.left = (int) ((systemDecorRect.left * scale) - 0.5f);
            systemDecorRect.top = (int) ((systemDecorRect.top * scale) - 0.5f);
            systemDecorRect.right = (int) (((systemDecorRect.right + 1) * scale) - 0.5f);
            systemDecorRect.bottom = (int) (((systemDecorRect.bottom + 1) * scale) - 0.5f);
        }
    }

    public MergedConfiguration getMergedConfiguration(Configuration global, Configuration override, String name) {
        if (mNoSupportPemDDC || name == null) {
            return new MergedConfiguration(global, override);
        }
        PidMsg pmsg = this.mScaledProcessNames.get(name);
        if (pmsg != null && pmsg.scale != 1.0f) {
            Configuration tempGlobalConfig = new Configuration(global);
            Configuration tempMergedConfig = new Configuration(override);
            tempGlobalConfig.densityDpi = (int) ((pmsg.scale * tempGlobalConfig.densityDpi) + 0.5f);
            tempMergedConfig.densityDpi = (int) ((pmsg.scale * tempMergedConfig.densityDpi) + 0.5f);
            if (tempGlobalConfig.windowConfiguration != null && tempGlobalConfig.windowConfiguration.getAppBounds() != null) {
                tempGlobalConfig.windowConfiguration.getAppBounds().scale(pmsg.scale);
            }
            if (tempMergedConfig.windowConfiguration != null && tempMergedConfig.windowConfiguration.getAppBounds() != null) {
                tempMergedConfig.windowConfiguration.getAppBounds().scale(pmsg.scale);
            }
            return new MergedConfiguration(tempGlobalConfig, tempMergedConfig);
        }
        return new MergedConfiguration(global, override);
    }

    public Configuration obtainNewConfigIfScaled(Configuration config, String name) {
        if (mNoSupportPemDDC || name == null || config == null) {
            return config;
        }
        PidMsg pmsg = this.mScaledProcessNames.get(name);
        if (pmsg != null && pmsg.scale != 1.0f) {
            Configuration newConfig = new Configuration(config);
            newConfig.densityDpi = (int) ((pmsg.scale * newConfig.densityDpi) + 0.5f);
            if (newConfig.windowConfiguration != null && newConfig.windowConfiguration.getAppBounds() != null) {
                newConfig.windowConfiguration.getAppBounds().scale(pmsg.scale);
            }
            return newConfig;
        }
        return config;
    }

    public DisplayInfo obtainNewDisplayInfoIfScaled(DisplayInfo info, int pid) {
        if (mNoSupportPemDDC || info == null) {
            return info;
        }
        PidMsg pmsg = this.mScaledPids.get(Integer.valueOf(pid));
        if (pmsg != null) {
            DisplayInfo newInfo = new DisplayInfo(info);
            newInfo.logicalDensityDpi = (int) ((pmsg.scale * newInfo.logicalDensityDpi) + 0.5f);
            newInfo.physicalXDpi = (int) ((pmsg.scale * newInfo.physicalXDpi) + 0.5f);
            newInfo.physicalYDpi = (int) ((pmsg.scale * newInfo.physicalYDpi) + 0.5f);
            newInfo.logicalHeight = (int) ((pmsg.scale * newInfo.logicalHeight) + 0.5f);
            newInfo.logicalWidth = (int) ((pmsg.scale * newInfo.logicalWidth) + 0.5f);
            newInfo.appHeight = (int) ((pmsg.scale * newInfo.appHeight) + 0.5f);
            newInfo.appWidth = (int) ((pmsg.scale * newInfo.appWidth) + 0.5f);
            newInfo.largestNominalAppHeight = (int) ((pmsg.scale * newInfo.largestNominalAppHeight) + 0.5f);
            newInfo.largestNominalAppWidth = (int) ((pmsg.scale * newInfo.largestNominalAppWidth) + 0.5f);
            newInfo.smallestNominalAppWidth = (int) ((pmsg.scale * newInfo.smallestNominalAppWidth) + 0.5f);
            newInfo.smallestNominalAppHeight = (int) ((pmsg.scale * newInfo.smallestNominalAppHeight) + 0.5f);
            return newInfo;
        }
        return info;
    }

    public void setDisableScaled(boolean disable) {
        this.isNoScaled = disable;
    }

    public void removeScaledProcess(String processName, int pid) {
        if (processName != null && this.mScaledProcessNames.containsKey(processName)) {
            try {
                this.mScaledPids.remove(Integer.valueOf(pid));
                this.mScaledProcessNames.remove(processName);
                VLog.v("VivoStats", "removeScaledPid pid = " + pid + ", name = " + processName);
            } catch (Exception e) {
                VLog.e("VivoStats", "removeScaledPid error on " + processName + ", pid = " + pid, e);
            }
        }
    }

    public void changeEventScale(InputEvent event, int pid) {
        PidMsg pmsg;
        if (!(event instanceof MotionEvent) || (pmsg = this.mScaledPids.get(Integer.valueOf(pid))) == null || pmsg.scale == 1.0f) {
            return;
        }
        ((MotionEvent) event).scale(1.0f / pmsg.scale);
    }

    /* loaded from: classes.dex */
    private final class PidMsg {
        public float scale = 1.0f;
        public VivoStats.UidCot uidcot = null;

        public PidMsg() {
        }
    }

    public boolean isFrozen(Object session) {
        if (session != null) {
            Session s = (Session) session;
            return this.mVivoFrozenPackageSupervisor.isFrozenPackage(s.mPackageName, s.mUid);
        }
        return false;
    }

    public boolean isKeepFrozen(ActivityRecord r, WindowProcessController callerApp, boolean forceUnfreeze, String reason) {
        if (r != null) {
            try {
                if (r.info != null && r.info.packageName != null && callerApp != null && callerApp.mInfo != null && callerApp.mInfo.packageName != null) {
                    int uid = (r.app == null || r.app.mInfo == null) ? -1 : r.app.mInfo.uid;
                    return this.mVivoFrozenPackageSupervisor.isKeepFrozen(r.info.packageName, uid, callerApp.mInfo.packageName, callerApp.mUid, 1, forceUnfreeze, reason);
                }
                return false;
            } catch (Exception e) {
                VLog.w("VivoStats", "Exception: " + e);
                return false;
            }
        }
        return false;
    }

    public boolean isFrozenProcess(WindowProcessController app, PrintWriter pw) {
        if (app != null && app.mInfo != null && app.mInfo.packageName != null && this.mVivoFrozenPackageSupervisor.isFrozenPackage(app.mInfo.packageName, app.mInfo.uid)) {
            if (pw != null) {
                pw.println("\n** this package: " + app.mInfo.packageName + " has been frozen **");
                return true;
            }
            return true;
        }
        return false;
    }

    public boolean isKeepFrozen(WindowProcessController app, boolean forceUnfreeze, String reason) {
        if (app == null || app.mInfo == null || app.mInfo.packageName == null) {
            return false;
        }
        return this.mVivoFrozenPackageSupervisor.isKeepFrozen(app.mInfo.packageName, app.mInfo.uid, null, -1, 1, forceUnfreeze, reason);
    }

    public boolean isFrozenPackage(WindowProcessController app) {
        if (app == null || app.mInfo == null || app.mInfo.packageName == null) {
            return false;
        }
        return this.mVivoFrozenPackageSupervisor.isFrozenPackage(app.mInfo.packageName, app.mInfo.uid);
    }

    public boolean isKeepFrozen(ActivityRecord r, boolean forceUnfreeze, String reason) {
        if (this.mWms.mPowerManager.isInteractive() && r != null) {
            int uid = (r.app == null || r.app.mInfo == null) ? -1 : r.app.mInfo.uid;
            return this.mVivoFrozenPackageSupervisor.isKeepFrozen(r.packageName, uid, null, -1, 1, forceUnfreeze, reason);
        }
        return false;
    }

    public boolean isKeepFrozenService(ServiceRecord r, boolean forceUnfreeze, String reason) {
        return this.mVivoFrozenPackageSupervisor.isKeepFrozenService(r, forceUnfreeze, reason);
    }

    public boolean isKeepFrozenService(ServiceRecord r, ProcessRecord callerApp, boolean forceUnfreeze, String reason) {
        return this.mVivoFrozenPackageSupervisor.isKeepFrozenService(r, callerApp, forceUnfreeze, reason);
    }

    public boolean isKeepFrozenService(ActiveServices activeService, ServiceRecord r, IApplicationThread caller, String callingPackage, int callingUid, boolean forceUnfreeze, String reason) {
        return this.mVivoFrozenPackageSupervisor.isKeepFrozenService(activeService, r, caller, callingPackage, callingUid, forceUnfreeze, reason);
    }

    public boolean isKeepFrozenBroadcastPrcocess(Object bro, ProcessRecord app, boolean forceUnfreeze, String reason) {
        return this.mVivoFrozenPackageSupervisor.isKeepFrozenBroadcastPrcocess(bro, app, forceUnfreeze, reason);
    }

    public boolean isKeepFrozen(String packageName, int uid, String callerPackage, int callerUid, int type, boolean forceUnfreeze, String reason) {
        return this.mVivoFrozenPackageSupervisor.isKeepFrozen(packageName, uid, callerPackage, callerUid, type, forceUnfreeze, reason);
    }

    public boolean isKeepFrozenProcess(ProcessRecord app, boolean forceUnfreeze, String reason) {
        return this.mVivoFrozenPackageSupervisor.isKeepFrozenProcess(app, forceUnfreeze, reason);
    }

    public void systemReady() {
        this.mVivoFrozenPackageSupervisor.systemReady();
        registerSplitScreenModeObserver();
    }

    public void registerSplitScreenModeObserver() {
        if (!this.hasRegistedSplitScreenModeObserver) {
            WindowManagerService windowManagerService = this.mWms;
            if (windowManagerService == null) {
                VLog.e("VivoStats", "need to wait ams init finished.");
            } else {
                ContentResolver contentResolver = windowManagerService.mContext.getContentResolver();
                this.mContentResolver = contentResolver;
                if (contentResolver != null) {
                    SplitScreenModeObserver splitScreenModeObserver = new SplitScreenModeObserver(mHandler);
                    this.mContentResolver.registerContentObserver(Settings.System.getUriFor(SPLIT_PANEL_MODE_CHANGE_URI), true, splitScreenModeObserver);
                    this.hasRegistedSplitScreenModeObserver = true;
                    VLog.i("VivoStats", "register split screen mode observer successed.");
                    return;
                }
                VLog.e("VivoStats", "ContentResolver is null.");
            }
            mHandler.postDelayed(new Runnable() { // from class: com.android.server.wm.VivoStatsInServerImpl.1
                @Override // java.lang.Runnable
                public void run() {
                    VivoStatsInServerImpl.this.registerSplitScreenModeObserver();
                }
            }, 1000L);
        }
        VLog.i("VivoStats", "register split screen mode observer:" + this.hasRegistedSplitScreenModeObserver);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SplitScreenModeObserver extends ContentObserver {
        public SplitScreenModeObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            try {
                boolean isSplitMode = Settings.System.getInt(VivoStatsInServerImpl.this.mContentResolver, VivoStatsInServerImpl.SPLIT_PANEL_MODE_CHANGE_URI, 0) == 1;
                VLog.d("VivoStats", "cur is in split mode:" + isSplitMode);
                VivoStatsInServerImpl.this.setWhichDocked(isSplitMode);
            } catch (Exception e) {
                VLog.e("VivoStats", "Split Mode Observer error cause:" + e);
            }
        }
    }

    public void initForWms(WindowManagerService wms) {
        this.mWms = wms;
    }

    public void initForAms(ActivityManagerService ams) {
        this.mAms = ams;
    }

    public void requestFrozen(String packageName, boolean isFrozen) {
        this.mVivoFrozenPackageSupervisor.requestFrozen(packageName, isFrozen, -1);
    }

    public void requestFrozen(String packageName, boolean isFrozen, int flag) {
        this.mVivoFrozenPackageSupervisor.requestFrozenWithFlag(packageName, isFrozen, flag, -1);
    }

    public void requestFrozen(String packageName, boolean isFrozen, int flag, int uid) {
        this.mVivoFrozenPackageSupervisor.requestFrozenWithUid(packageName, isFrozen, flag, uid, -1);
    }

    public void setFrozenEnable(boolean frozenEnable) {
        this.mVivoFrozenPackageSupervisor.setFrozenEnable(frozenEnable);
    }

    public void setVmrEnable(boolean enable) {
        this.mVivoFrozenPackageSupervisor.setVmrEnable(enable);
    }

    public boolean isFrozenEnable() {
        return this.mVivoFrozenPackageSupervisor.isEnableFunction();
    }

    public boolean isKeepFrozen(IApplicationThread caller, String packageName, int userId, int type, String reason) {
        int uid = -1;
        if (userId == 0) {
            uid = 10000;
        } else {
            long token = Binder.clearCallingIdentity();
            try {
            } catch (Exception e) {
                e = e;
            } catch (Throwable th) {
                th = th;
                Binder.restoreCallingIdentity(token);
                throw th;
            }
            try {
                try {
                    uid = AppGlobals.getPackageManager().getPackageUid(packageName, (int) Dataspace.STANDARD_BT601_625, userId);
                } catch (Throwable th2) {
                    th = th2;
                    Binder.restoreCallingIdentity(token);
                    throw th;
                }
            } catch (Exception e2) {
                e = e2;
                e.printStackTrace();
                Binder.restoreCallingIdentity(token);
                return this.mVivoFrozenPackageSupervisor.isKeepFrozen(packageName, uid, null, -1, type, false, reason);
            }
            Binder.restoreCallingIdentity(token);
        }
        return this.mVivoFrozenPackageSupervisor.isKeepFrozen(packageName, uid, null, -1, type, false, reason);
    }

    public boolean isFrozenPackage(String packageName, int uid) {
        return this.mVivoFrozenPackageSupervisor.isFrozenPackage(packageName, uid);
    }

    public boolean setCallerWhiteList(List<String> pkgNames) {
        return this.mVivoFrozenPackageSupervisor.setCallerWhiteList(pkgNames);
    }

    public boolean setBeCalledWhiteList(List<String> pkgNames) {
        return this.mVivoFrozenPackageSupervisor.setBeCalledWhiteList(pkgNames);
    }

    public boolean setInterfaceWhiteList(List<String> pkgNames) {
        return this.mVivoFrozenPackageSupervisor.setInterfaceWhiteList(pkgNames);
    }

    public boolean setFrozenPkgBlacklist(List<String> pkgNames, int len, int add, int fromWhich) {
        return this.mVivoFrozenPackageSupervisor.setFrozenPkgBlacklist(pkgNames, len, add, fromWhich);
    }

    public boolean isFrozenProcess(ProcessRecord app) {
        return this.mVivoFrozenPackageSupervisor.isFrozenProcess(app);
    }

    public boolean isFrozenProcess(int uid) {
        return this.mVivoFrozenPackageSupervisor.isFrozenPackage(uid);
    }

    public boolean isKeepFrozenProvider(Object cpro, ProcessRecord r) {
        return this.mVivoFrozenPackageSupervisor.isKeepFrozenProvider(cpro, r);
    }

    public boolean isKeepFrozenProvider(ContentProviderHolder holder, int uid) {
        if (holder != null && holder.info != null && holder.info.packageName != null && this.mVivoFrozenPackageSupervisor.isKeepFrozen(holder.info.packageName, -1, null, uid, 4, false, "get provider type")) {
            VLog.d("VivoStats", "get content provider type is intercepted by vivo frozen");
            return true;
        }
        return false;
    }

    public boolean shutDown(int timeout) {
        if (timeout == 0) {
            this.mVivoFrozenPackageSupervisor.unfrozenAllPackages();
            return true;
        }
        return false;
    }

    public void dumpFrozenPackageLocked(PrintWriter pw, String[] args, int opti) {
        ActivityManagerService activityManagerService = this.mAms;
        if (activityManagerService == null) {
            return;
        }
        synchronized (activityManagerService) {
            this.mVivoFrozenPackageSupervisor.dumpFrozenPackageLocked(pw);
        }
    }

    public boolean isFrozenProcess(ProcessRecord app, PrintWriter pw) {
        return this.mVivoFrozenPackageSupervisor.isFrozenProcess(app, pw);
    }

    public void dumpBinderProxy() {
        VLog.d("VivoStats", "force unfrozenAllPackages for dumpBinderProxy");
        this.mVivoFrozenPackageSupervisor.unfrozenAllPackages();
    }

    public boolean isFrozenPackageObject(Object cookie) {
        if (cookie == null) {
            return false;
        }
        try {
            Integer callingUid = (Integer) cookie;
            if (callingUid.intValue() >= 10000) {
                if (this.mVivoFrozenPackageSupervisor.isFrozenPackage(callingUid.intValue())) {
                    return true;
                }
            }
        } catch (Exception e) {
        }
        return false;
    }

    public void registerObserver(RemoteCallbackList<IUserSwitchObserver> userSwitchObservers, IUserSwitchObserver observer, int uid) {
        if (userSwitchObservers == null || observer == null) {
            return;
        }
        userSwitchObservers.register(observer, Integer.valueOf(uid));
    }

    private int removeHoldWakeLockUid(PowerManagerService power, int uid) {
        ArrayList<PowerManagerService.WakeLock> needReleaseWakeLocks = new ArrayList<>();
        synchronized (power.mLock) {
            int numWkLocks = power.mWakeLocks.size();
            if (numWkLocks <= 0) {
                return 0;
            }
            Iterator it = power.mWakeLocks.iterator();
            while (it.hasNext()) {
                PowerManagerService.WakeLock wl = (PowerManagerService.WakeLock) it.next();
                if (wl != null) {
                    if (wl.mOwnerUid == uid) {
                        needReleaseWakeLocks.add(wl);
                    } else if (wl.mWorkSource != null && wl.mWorkSource.get(0) == uid) {
                        needReleaseWakeLocks.add(wl);
                    }
                }
            }
            Iterator<PowerManagerService.WakeLock> it2 = needReleaseWakeLocks.iterator();
            while (it2.hasNext()) {
                PowerManagerService.WakeLock next = it2.next();
                VLog.d("VivoStats", "removeWakeLockByFrozen: lock=" + Objects.hashCode(next.mLock) + " [" + next.mTag + "], flags=0x" + Integer.toHexString(next.mFlags));
                int index = power.findWakeLockIndexLocked(next.mLock);
                if (index >= 0) {
                    next.mLock.unlinkToDeath(next, 0);
                    power.removeWakeLockLocked(next, index);
                } else {
                    VLog.d("VivoStats", "removeWakeLockByFrozen: lock=" + Objects.hashCode(next.mLock) + " [not found], flags=0x" + Integer.toHexString(next.mFlags));
                }
            }
            this.mFrozenWakeLocks.addAll(needReleaseWakeLocks);
            return needReleaseWakeLocks.size();
        }
    }

    public boolean removeFrozenWakeLock(PowerManagerService power, IBinder lock, boolean needRemove) {
        synchronized (power.mLock) {
            int find = -1;
            int count = this.mFrozenWakeLocks.size();
            int i = 0;
            while (true) {
                if (i >= count) {
                    break;
                }
                PowerManagerService.WakeLock wl = this.mFrozenWakeLocks.get(i);
                if (wl.mLock != lock) {
                    i++;
                } else {
                    find = i;
                    VLog.d("VivoStats", "lock=" + Objects.hashCode(wl.mLock) + " [" + wl.mTag + "], flags=0x" + Integer.toHexString(wl.mFlags) + " has been remove.");
                    break;
                }
            }
            if (find > -1) {
                if (needRemove) {
                    this.mFrozenWakeLocks.remove(find);
                }
                return true;
            }
            return false;
        }
    }

    public int onFronzenPackage(PowerManagerService power, String packageName) {
        if (packageName == null) {
            return 0;
        }
        try {
            int uid = AppGlobals.getPackageManager().getPackageUid(packageName, (int) Dataspace.STANDARD_BT601_625, 0);
            if (uid < 10000) {
                return 0;
            }
            return removeHoldWakeLockUid(power, uid);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public void runPre() {
        VLog.i("VivoStats", "Shutting down unfrozen apps...");
        IActivityManager activityManager = IActivityManager.Stub.asInterface(ServiceManager.checkService(VivoFirewall.TYPE_ACTIVITY));
        if (activityManager != null) {
            try {
                activityManager.shutdown(0);
            } catch (RemoteException e) {
            }
        }
    }

    public void removeFrozenReceiverLocked(LocationManagerService.Receiver receiver) {
        this.mFrozenReceivers.remove(receiver.mKey);
    }

    public int onFrozenPackage(String packageName, boolean isFrozen, Object lock, HashMap<Object, LocationManagerService.Receiver> receivers, LocationManagerService service) {
        if (packageName == null) {
            VLog.w("VivoStats", "Frozen GPS fail,reason: packageName invalid!");
            return 0;
        }
        int count = 0;
        synchronized (lock) {
            if (isFrozen) {
                ArrayList<LocationManagerService.Receiver> deadReceivers = null;
                for (Object key : receivers.keySet()) {
                    LocationManagerService.Receiver receiver = receivers.get(key);
                    if (receiver.mCallerIdentity.packageName.equals(packageName)) {
                        if (deadReceivers == null) {
                            deadReceivers = new ArrayList<>();
                        }
                        deadReceivers.add(receiver);
                        this.mFrozenReceivers.put(key, receiver);
                        count++;
                    }
                }
                if (deadReceivers != null) {
                    Iterator<LocationManagerService.Receiver> it = deadReceivers.iterator();
                    while (it.hasNext()) {
                        LocationManagerService.Receiver receiver2 = it.next();
                        VLog.i("VivoStats", "Frozen GPS remove " + receiver2.mCallerIdentity.packageName + " - " + Integer.toHexString(System.identityHashCode(receiver2)));
                        service.removeUpdatesLocked(receiver2);
                    }
                }
            } else {
                HashMap<Object, LocationManagerService.Receiver> addReceivers = new HashMap<>();
                for (Object key2 : this.mFrozenReceivers.keySet()) {
                    LocationManagerService.Receiver receiver3 = this.mFrozenReceivers.get(key2);
                    if (receiver3.mCallerIdentity.packageName.equals(packageName)) {
                        addReceivers.put(key2, receiver3);
                    }
                }
                for (Object key3 : addReceivers.keySet()) {
                    this.mFrozenReceivers.remove(key3);
                    LocationManagerService.Receiver receiver4 = addReceivers.get(key3);
                    if (receiver4.isListener()) {
                        try {
                            receiver4.getListener().asBinder().linkToDeath(receiver4, 0);
                        } catch (RemoteException e) {
                            VLog.e("VivoStats", "linkToDeath failed:", e);
                        }
                    }
                    VLog.i("VivoStats", "Frozen GPS add " + receiver4.mCallerIdentity.packageName + " - " + Integer.toHexString(System.identityHashCode(receiver4)));
                    receivers.put(key3, receiver4);
                    Iterator it2 = receiver4.mUpdateRecords.values().iterator();
                    while (true) {
                        if (!it2.hasNext()) {
                            break;
                        }
                        LocationManagerService.UpdateRecord updateRecord = (LocationManagerService.UpdateRecord) it2.next();
                        if (updateRecord.mProvider != null) {
                            LocationRequest sanitizedRequest = updateRecord.mRealRequest;
                            VLog.i("VivoStats", "Frozen requestLocationUpdatesLocked " + receiver4.mCallerIdentity.packageName + " - " + sanitizedRequest);
                            service.requestLocationUpdatesLocked(sanitizedRequest, receiver4);
                            break;
                        }
                    }
                    count++;
                }
            }
        }
        return count;
    }

    public boolean iskeepFrozen(LocationManagerService.Receiver receiver) {
        if (receiver != null && receiver.mCallerIdentity != null && receiver.mCallerIdentity.packageName != null && this.mVivoFrozenPackageSupervisor.isFrozenPackage(receiver.mCallerIdentity.packageName, receiver.mCallerIdentity.uid)) {
            VLog.d("VivoStats", "skipping loc update for Frozen app: " + receiver.mCallerIdentity.packageName);
            return true;
        }
        return false;
    }

    public boolean isFrozenPackageSync(SyncOperation op) {
        if (op != null && op.owningPackage != null) {
            return this.mVivoFrozenPackageSupervisor.isFrozenPackage(op.owningPackage, op.owningUid);
        }
        return false;
    }

    public void iskeepFrozenMediaSeesion(String sessionPackageName, int uid, KeyEvent keyEvent) {
        if (keyEvent != null) {
            int keyCode = keyEvent.getKeyCode();
            if (sessionPackageName != null) {
                if (keyCode == 79 || keyCode == 126 || keyCode == 88 || keyCode == 87 || keyCode == 91 || keyCode == 164 || keyCode == 85) {
                    this.mVivoFrozenPackageSupervisor.isKeepFrozen(sessionPackageName, uid, null, -1, 6, true, "media KEYCODE");
                }
            }
        }
    }

    public void iskeepFrozenObject(Object cookie) {
        if (cookie == null) {
            return;
        }
        try {
            Integer callingUid = (Integer) cookie;
            if (callingUid.intValue() >= 10000) {
                String[] packageNames = AppGlobals.getPackageManager().getPackagesForUid(callingUid.intValue());
                if (!ArrayUtils.isEmpty(packageNames) && this.mVivoFrozenPackageSupervisor.isEnableFunction()) {
                    this.mVivoFrozenPackageSupervisor.isKeepFrozen(packageNames[0], callingUid.intValue(), null, -1, 1, true, "bluetooth state change");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void expandMsg(Message msg) {
        if (msg != null) {
            msg.arg1 = Binder.getCallingUid();
        }
    }

    public void registerStateCallback(RemoteCallbackList<IBluetoothStateChangeCallback> stateChangeCallbacks, IBluetoothStateChangeCallback callback, Message msg) {
        if (stateChangeCallbacks == null || callback == null || msg == null) {
            return;
        }
        Integer callingUid = Integer.valueOf(msg.arg1);
        stateChangeCallbacks.register(callback, callingUid);
    }

    public void registerCallback(RemoteCallbackList<IBluetoothManagerCallback> mCallbacks, IBluetoothManagerCallback callback, Message msg) {
        if (mCallbacks == null || callback == null || msg == null) {
            return;
        }
        Integer callingUid = Integer.valueOf(msg.arg1);
        mCallbacks.register(callback, callingUid);
    }

    public boolean hasVirtualDisplay(String packageName, VirtualDisplayAdapter virtualDisplayAdapter, DisplayManagerService.SyncRoot syncRoot) {
        long token = Binder.clearCallingIdentity();
        try {
            synchronized (syncRoot) {
                if (virtualDisplayAdapter == null) {
                    return false;
                }
                return this.mVivoFrozenPackageSupervisor.hasVirtualDisplayLocked(virtualDisplayAdapter, packageName);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public int getFlagsOfLastMotionEvent() {
        VLog.d("VivoStats", "getFlagsOfLastMotionEvent: flags=" + this.mFlags);
        return this.mFlags;
    }

    public Map<Integer, String> getForegroundApp() {
        Map<Integer, String> apps = new HashMap<>();
        synchronized (this.mWms.mGlobalLock) {
            TaskDisplayArea taskDisplayArea = this.mWms.mRoot.getDefaultTaskDisplayArea();
            if (taskDisplayArea != null) {
                boolean inSplitScreen = taskDisplayArea.isSplitScreenModeActivated();
                ArrayList<ActivityStack> stacks = new ArrayList<>();
                if (inSplitScreen) {
                    ActivityStack primaryStack = taskDisplayArea.getTopStackInWindowingMode(3);
                    if (primaryStack != null) {
                        stacks.add(primaryStack);
                    }
                    ActivityStack secondaryStack = taskDisplayArea.getTopStackInWindowingMode(4);
                    if (secondaryStack != null) {
                        stacks.add(secondaryStack);
                    }
                } else {
                    ActivityStack topFullScreenStack = taskDisplayArea.getTopStackInWindowingMode(1);
                    if (topFullScreenStack != null) {
                        stacks.add(topFullScreenStack);
                    }
                }
                final List<ActivityRecord> activities = new ArrayList<>();
                Iterator<ActivityStack> it = stacks.iterator();
                while (it.hasNext()) {
                    ActivityStack stack = it.next();
                    Task topTask = stack.getTopMostTask();
                    if (topTask != null) {
                        Objects.requireNonNull(activities);
                        topTask.forAllActivities(new Consumer() { // from class: com.android.server.wm.-$$Lambda$MN0LOs_GLQM6InrhD7RC3D3Ymms
                            @Override // java.util.function.Consumer
                            public final void accept(Object obj) {
                                activities.add((ActivityRecord) obj);
                            }
                        });
                    }
                }
                for (int index = 0; index < activities.size(); index++) {
                    ActivityRecord r = activities.get(index);
                    if (r != null && r.info != null && r.info.applicationInfo != null) {
                        int uid = r.info.applicationInfo.uid;
                        String packageName = r.info.applicationInfo.packageName;
                        if (!apps.containsKey(Integer.valueOf(uid))) {
                            apps.put(Integer.valueOf(uid), packageName);
                        }
                    }
                }
            }
        }
        return apps;
    }
}