package com.vivo.services.autorecover;

import android.app.AppGlobals;
import android.app.IActivityController;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.graphics.common.V1_0.Dataspace;
import android.hardware.input.InputManagerInternal;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.DropBoxManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManagerInternal;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.FtFeature;
import android.view.Display;
import android.view.InputWindowHandle;
import android.view.KeyEvent;
import android.view.SurfaceControl;
import com.android.internal.policy.KeyInterceptionInfo;
import com.android.internal.util.DumpUtils;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.am.ActivityManagerService;
import com.android.server.policy.InputExceptionReport;
import com.android.server.policy.VivoPolicyUtil;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.WindowManagerInternal;
import com.android.server.wm.WindowState;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import com.vivo.framework.autorecover.SystemAutoRecoverManager;
import com.vivo.services.daemon.VivoDmServiceProxy;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import vivo.app.VivoFrameworkFactory;
import vivo.app.autorecover.ISystemAutoRecoverManager;
import vivo.app.autorecover.VivoParcelableException;
import vivo.app.configuration.AbsConfigurationManager;
import vivo.app.configuration.ConfigurationObserver;
import vivo.app.configuration.ContentValuesList;

/* loaded from: classes.dex */
public class SystemAutoRecoverService extends SystemService {
    private static final int DEFAULT_INVALID_SIZE_WINDOW_CHECK_DELAY = 3000;
    private static final String INVALID_WINDOW_POLICY_CONFIG_FILE = "/data/bbkcore/SystemAutoRecover_AutoRecoverPolicy_2.0.xml";
    private static final int MSG_CHECK_FROM_FOCUS_CHANGE = 5;
    private static final int MSG_CHECK_FROM_INPUT_TOUCH = 1;
    private static final int MSG_CHECK_NO_FOCUS_WINDOW = 4;
    private static final int MSG_FORCE_RECOVER = 7;
    private static final int MSG_INIT_POLICY_CONFIG = 2;
    private static final int MSG_REPORT_EXCEPTION = 6;
    private static final int MSG_UPDATE_POLICY_CONFIG = 3;
    private static final String POLICY_EXCEPTION_CATCHER = "policy_exception_catcher";
    private static final String SETTINGS_IS_GAME_MODE = "is_game_mode";
    public static final String TAG = "SystemAutoRecoverService";
    private static final String TAG_BLACK_LIST = "transparent_black_list";
    private static final String TAG_DEBUG = "debug";
    private static final String TAG_DETAIL_POLICY = "_policy";
    private static final String TAG_SWITCH_FOCUS_CHANGE_CHECK = "focus_change_check_switch";
    private static final String TAG_SWITCH_FORCE_STOP_FREEZING = "force_stop_freezing";
    private static final String TAG_SWITCH_INPUT_CHECK = "input_check_switch";
    private static final String TAG_SWITCH_LIST = "policy_switches";
    private static final String TAG_SWITCH_PUNISH = "punish_switch";
    private static final String TAG_SWITCH_STARTING_WINDOW_BACK_KEY_OPT = "starting_window_back_key_opt";
    private static final String TAG_WHITE_LIST = "transparent_white_list";
    private static final String VALUE_SWITCH_OFF = "off";
    private static final String VALUE_SWITCH_ON = "on";
    private boolean mActivityControllerIsMonkey;
    private ActivityManagerService mActivityManagerService;
    private AudioManager mAudioManager;
    private ContentValuesList mBlackList;
    private ContentValuesList mBlackWindowCheckFromNoFocusTimeDetailPolicy;
    private BroadcastReceiver mBroadcastReceiver;
    private AbsConfigurationManager mConfigurationManager;
    Context mContext;
    private ContentValuesList mDebugPolicy;
    private DropBoxManager mDropBoxManager;
    private final ArrayMap<String, ArrayList<WindowItem>> mDynamicConfigItemList;
    private final ArrayMap<String, ContentValuesList> mDynamicConfigMap;
    private SystemExceptionHandler mExceptionHandler;
    private boolean mFocusChangeCheckEnabled;
    private HashMap<Integer, ActivityRecord> mFocusedApps;
    private HashMap<Integer, WindowState> mFocusedWindows;
    private boolean mForceStopFreezingEnabled;
    private Handler mHandler;
    private boolean mHasActivityController;
    private boolean mInCallStateOffHook;
    private boolean mInputCheckEnabled;
    private InputManagerInternal mInputManagerInternal;
    private boolean mIsGameMode;
    private final Object mLock;
    private boolean mPasswordMode;
    private final ArraySet<InvalidWindowRecord> mPendingReportExceptions;
    private PowerManagerInternal mPowerManagerInternal;
    private boolean mPunishAll;
    private boolean mPunishEnabled;
    private boolean mShuttingDown;
    private boolean mStartingWindowBackKeyOptEnabled;
    private ContentValuesList mSwitchList;
    private final ArrayList<WindowItem> mTransparentWindowBlackList;
    private final ArrayList<WindowItem> mTransparentWindowWhiteList;
    private boolean mUserIsMonkey;
    private VivoDmServiceProxy mVivoDmSrvProxy;
    private ContentValuesList mWhiteList;
    private WindowManagerInternal mWindowManagerInternal;
    private static final boolean CHECK_APP_DRAW_BLACK = "true".equals(FtFeature.getFeatureAttribute("vivo.opt.atypicalhang", "check_black", "false"));
    private static boolean sReportImmediately = "true".equals(SystemProperties.get("persist.vivo.autorecover.report.immediately", "false"));
    private static boolean sIgnoreDebug = "true".equals(SystemProperties.get("persist.vivo.ars.ignoredebug", "false"));
    private static final String TAG_BLACK_POLICY = "_black";
    private static final String TAG_WHITE_POLICY = "_white";
    private static final String[] TAG_POLICY_TYPE_NAME = {TAG_BLACK_POLICY, TAG_WHITE_POLICY};
    private static final String TAG_BLACK_WINDOW_CHECK_FROM_INPUT = "black_window_check_from_input";
    private static final String TAG_BLACK_WINDOW_CHECK_FROM_NO_FOCUS_TIME_OUT = "black_window_check_from_no_focus_time_out";
    private static final String TAG_INVALID_WINDOW_SIZE_CHECK_FROM_FOCUS_CHANGE = "invalid_window_size_check_from_focus_change";
    private static final String TAG_INVALID_WINDOW_SIZE_CHECK_FROM_INPUT = "invalid_window_size_check_from_input";
    private static final String TAG_TRANSPARENT_ACTIVITY_CHECK_FROM_INPUT = "transparent_activity_check_from_input";
    private static final String TAG_TRANSPARENT_WINDOW_CHECK_FROM_INPUT = "transparent_window_check_from_input";
    private static final String[] TAG_DYNAMIC_POLICY_NAME = {TAG_BLACK_WINDOW_CHECK_FROM_INPUT, TAG_BLACK_WINDOW_CHECK_FROM_NO_FOCUS_TIME_OUT, TAG_INVALID_WINDOW_SIZE_CHECK_FROM_FOCUS_CHANGE, TAG_INVALID_WINDOW_SIZE_CHECK_FROM_INPUT, TAG_TRANSPARENT_ACTIVITY_CHECK_FROM_INPUT, TAG_TRANSPARENT_WINDOW_CHECK_FROM_INPUT};
    private static int DEFAULT_NO_FOCUSED_WINDOW_CHECK_DELAY = Math.max(SystemProperties.getInt("persist.vivo.nofocus.timeout", 6000), 6000);

    public SystemAutoRecoverService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mActivityManagerService = null;
        this.mDynamicConfigMap = new ArrayMap<>();
        this.mTransparentWindowWhiteList = new ArrayList<>();
        this.mTransparentWindowBlackList = new ArrayList<>();
        this.mDynamicConfigItemList = new ArrayMap<>();
        this.mPendingReportExceptions = new ArraySet<>();
        this.mInputCheckEnabled = false;
        this.mFocusChangeCheckEnabled = false;
        this.mPunishEnabled = false;
        this.mForceStopFreezingEnabled = false;
        this.mStartingWindowBackKeyOptEnabled = false;
        this.mPunishAll = false;
        this.mUserIsMonkey = false;
        this.mHasActivityController = false;
        this.mActivityControllerIsMonkey = false;
        this.mShuttingDown = false;
        this.mPasswordMode = false;
        this.mInCallStateOffHook = false;
        this.mIsGameMode = false;
        this.mFocusedWindows = new HashMap<>();
        this.mFocusedApps = new HashMap<>();
        this.mBroadcastReceiver = new BroadcastReceiver() { // from class: com.vivo.services.autorecover.SystemAutoRecoverService.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (action != null && action.equals("android.intent.action.PHONE_STATE")) {
                    SystemAutoRecoverService.this.mInCallStateOffHook = TelephonyManager.EXTRA_STATE_OFFHOOK.equals(intent.getStringExtra("state"));
                }
            }
        };
        this.mContext = context;
    }

    public void onStart() {
        HandlerThread handlerThread = new HandlerThread("system_auto_recover", 10);
        handlerThread.start();
        this.mHandler = new InvalidWindowWatcherHandler(handlerThread.getLooper());
        SystemExceptionHandler systemExceptionHandler = new SystemExceptionHandler();
        this.mExceptionHandler = systemExceptionHandler;
        systemExceptionHandler.onStart();
        Looper.setIsSystemServerProcess(true);
        this.mConfigurationManager = VivoFrameworkFactory.getFrameworkFactoryImpl().getConfigurationManager();
        Handler handler = this.mHandler;
        handler.sendMessage(handler.obtainMessage(2));
        publishBinderService("system_auto_recover_service", new BinderService(), true);
        publishLocalService(SystemAutoRecoverManagerInternal.class, new LocalService());
        SystemAutoRecoverManager.getInstance();
        this.mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        this.mPowerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
        this.mInputManagerInternal = (InputManagerInternal) LocalServices.getService(InputManagerInternal.class);
        this.mAudioManager = (AudioManager) getContext().getSystemService("audio");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PHONE_STATE");
        this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, intentFilter, null, this.mHandler);
        GameModeObserver gameModeObserver = new GameModeObserver(this.mHandler);
        gameModeObserver.observe();
        this.mDropBoxManager = (DropBoxManager) getContext().getSystemService("dropbox");
        this.mVivoDmSrvProxy = VivoDmServiceProxy.asInterface(ServiceManager.getService("vivo_daemon.service"));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class AutoRecoverPolicyObserver extends ConfigurationObserver {
        AutoRecoverPolicyObserver() {
        }

        public void onConfigChange(String file, String name) {
            VLog.i(SystemAutoRecoverService.TAG, "onConfigChange file:" + file + ",name=" + name);
            if (SystemAutoRecoverService.this.mConfigurationManager != null) {
                SystemAutoRecoverService.this.updateInvalidWindowPolicyConfig();
            } else {
                VLog.i(SystemAutoRecoverService.TAG, "mConfigurationManager is null");
            }
        }
    }

    public void setAms(ActivityManagerService activityManagerService) {
        this.mActivityManagerService = activityManagerService;
    }

    /* loaded from: classes.dex */
    final class BinderService extends ISystemAutoRecoverManager.Stub {
        BinderService() {
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(SystemAutoRecoverService.this.mContext, SystemAutoRecoverService.TAG, pw)) {
                boolean dumpEnable = SystemProperties.getBoolean("persist.vivo.autorecoverservice.dump", false);
                if (!dumpEnable) {
                    return;
                }
                if (args != null && args.length > 0) {
                    if ("force_enable".equals(args[0])) {
                        SystemAutoRecoverService.this.forceEnable(true);
                        return;
                    } else if ("force_disable".equals(args[0])) {
                        SystemAutoRecoverService.this.forceEnable(false);
                        return;
                    } else {
                        return;
                    }
                }
                SystemAutoRecoverService.this.dump(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, pw);
            }
        }

        public boolean ignoreExceptionIfNeed(VivoParcelableException exception) {
            return SystemAutoRecoverService.this.mExceptionHandler.matchException(exception.getThrowable());
        }

        /* JADX WARN: Multi-variable type inference failed */
        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            new SystemAutoRecoverShellCommand(SystemAutoRecoverService.this).exec(this, in, out, err, args, callback, resultReceiver);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void forceEnable(boolean enabled) {
        this.mInputCheckEnabled = enabled;
        this.mFocusChangeCheckEnabled = enabled;
        this.mPunishEnabled = enabled;
        this.mForceStopFreezingEnabled = enabled;
        this.mStartingWindowBackKeyOptEnabled = enabled;
        this.mPunishAll = enabled;
        NoFocusWindowRecord.forceEnabled(enabled);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public void forceEnable(String tag, boolean enabled) {
        char c;
        switch (tag.hashCode()) {
            case -1409887931:
                if (tag.equals("input-check")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case -1378572017:
                if (tag.equals("starting-window-back-key-opt")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case -1261111424:
                if (tag.equals("focus-change-check")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case -1029400407:
                if (tag.equals("punish-all")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case -977068843:
                if (tag.equals("punish")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -293400624:
                if (tag.equals("nofocus-force-fg-recover")) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case 96673:
                if (tag.equals("all")) {
                    c = '\b';
                    break;
                }
                c = 65535;
                break;
            case 492653396:
                if (tag.equals("nofocus-force-bg-recover")) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case 1087139741:
                if (tag.equals("force-stop-freezing")) {
                    c = 3;
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
                this.mInputCheckEnabled = enabled;
                return;
            case 1:
                this.mFocusChangeCheckEnabled = enabled;
                return;
            case 2:
                this.mPunishEnabled = enabled;
                return;
            case 3:
                this.mForceStopFreezingEnabled = enabled;
                return;
            case 4:
                this.mStartingWindowBackKeyOptEnabled = enabled;
                return;
            case 5:
                this.mPunishAll = enabled;
                return;
            case 6:
            case 7:
                NoFocusWindowRecord.forceEnabled(tag, enabled);
                return;
            case '\b':
                forceEnable(enabled);
                return;
            default:
                return;
        }
    }

    public void setParam(String tag, String value) {
        char c;
        int hashCode = tag.hashCode();
        if (hashCode != -293400624) {
            if (hashCode == 492653396 && tag.equals("nofocus-force-bg-recover")) {
                c = 0;
            }
            c = 65535;
        } else {
            if (tag.equals("nofocus-force-fg-recover")) {
                c = 1;
            }
            c = 65535;
        }
        if (c == 0 || c == 1) {
            NoFocusWindowRecord.setParam(tag, value);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isInputCheckEnabled() {
        return this.mInputCheckEnabled;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isFocusChangeCheckEnabled() {
        return this.mFocusChangeCheckEnabled;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPunishEnabled() {
        return this.mPunishEnabled;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldCheckAppDrawBlack() {
        return CHECK_APP_DRAW_BLACK;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Display getDisplay(int displayId) {
        DisplayManager displayManager = (DisplayManager) this.mContext.getSystemService("display");
        return displayManager.getDisplay(displayId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void getStableInsets(int displayId, Rect outInsets) throws RemoteException {
        this.mActivityManagerService.mWindowManager.getStableInsets(displayId, outInsets);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void getStableInsetsWithoutCutout(int displayId, Rect outInsets) throws RemoteException {
        this.mActivityManagerService.mWindowManager.getStableInsetsWithoutCutout(displayId, outInsets);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isUserAMonkey() {
        return this.mUserIsMonkey || (this.mHasActivityController && this.mActivityControllerIsMonkey);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isPasswordMode() {
        return this.mPasswordMode;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isIncall() {
        int audioMode;
        return this.mInCallStateOffHook || (audioMode = this.mAudioManager.getMode()) == 2 || audioMode == 3;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isGameMode() {
        return this.mIsGameMode;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public float getSurfaceAlpha(InputWindowHandle inputWindowHandle, int displayId) {
        return this.mInputManagerInternal.getSurfaceAlpha(inputWindowHandle, displayId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void reportException(InvalidWindowRecord exception, int delay) {
        VLog.d(TAG, "reportException :" + exception + " delay = " + delay);
        this.mHandler.removeCallbacksAndMessages(exception);
        Message message = Message.obtain(this.mHandler, 6, exception);
        Bundle data = new Bundle();
        boolean shouldForceRecover = (delay == 0 || (exception.recoverWay() & 8) == 0) ? false : true;
        data.putBoolean("shouldForceRecover", shouldForceRecover);
        message.setData(data);
        this.mHandler.sendMessageDelayed(message, sReportImmediately ? 0L : delay);
        synchronized (this.mPendingReportExceptions) {
            this.mPendingReportExceptions.add(exception);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isFocused(WindowState win) {
        boolean z;
        synchronized (this.mLock) {
            z = win == this.mFocusedWindows.get(Integer.valueOf(win.getDisplayId()));
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:18:0x002f A[Catch: all -> 0x0070, TryCatch #0 {, blocks: (B:10:0x0016, B:12:0x001b, B:18:0x002f, B:21:0x004f, B:23:0x0051, B:24:0x006e), top: B:29:0x0016 }] */
    /* JADX WARN: Removed duplicated region for block: B:23:0x0051 A[Catch: all -> 0x0070, TryCatch #0 {, blocks: (B:10:0x0016, B:12:0x001b, B:18:0x002f, B:21:0x004f, B:23:0x0051, B:24:0x006e), top: B:29:0x0016 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public boolean shouldRecover(java.lang.String r9, java.lang.String r10, java.lang.String r11) {
        /*
            r8 = this;
            boolean r0 = r8.isCtsPackage(r10)
            r1 = 0
            if (r0 == 0) goto L8
            return r1
        L8:
            boolean r0 = r8.isDebugEnabled(r10)
            if (r0 == 0) goto Lf
            return r1
        Lf:
            java.lang.String r0 = r9.toLowerCase()
            android.util.ArrayMap<java.lang.String, java.util.ArrayList<com.vivo.services.autorecover.SystemAutoRecoverService$WindowItem>> r2 = r8.mDynamicConfigItemList
            monitor-enter(r2)
            boolean r3 = r8.mPunishAll     // Catch: java.lang.Throwable -> L70
            r4 = 1
            if (r3 != 0) goto L2c
            java.lang.String r3 = "on"
            vivo.app.configuration.ContentValuesList r5 = r8.mSwitchList     // Catch: java.lang.Throwable -> L70
            java.lang.String r5 = r5.getValue(r0)     // Catch: java.lang.Throwable -> L70
            boolean r3 = r3.equals(r5)     // Catch: java.lang.Throwable -> L70
            if (r3 == 0) goto L2a
            goto L2c
        L2a:
            r3 = r1
            goto L2d
        L2c:
            r3 = r4
        L2d:
            if (r3 == 0) goto L51
            android.util.ArrayMap<java.lang.String, java.util.ArrayList<com.vivo.services.autorecover.SystemAutoRecoverService$WindowItem>> r5 = r8.mDynamicConfigItemList     // Catch: java.lang.Throwable -> L70
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L70
            r6.<init>()     // Catch: java.lang.Throwable -> L70
            r6.append(r0)     // Catch: java.lang.Throwable -> L70
            java.lang.String r7 = "_white"
            r6.append(r7)     // Catch: java.lang.Throwable -> L70
            java.lang.String r6 = r6.toString()     // Catch: java.lang.Throwable -> L70
            java.lang.Object r5 = r5.get(r6)     // Catch: java.lang.Throwable -> L70
            java.util.ArrayList r5 = (java.util.ArrayList) r5     // Catch: java.lang.Throwable -> L70
            boolean r5 = r8.inPunishList(r5, r10, r11)     // Catch: java.lang.Throwable -> L70
            if (r5 != 0) goto L4f
            r1 = r4
        L4f:
            monitor-exit(r2)     // Catch: java.lang.Throwable -> L70
            return r1
        L51:
            android.util.ArrayMap<java.lang.String, java.util.ArrayList<com.vivo.services.autorecover.SystemAutoRecoverService$WindowItem>> r1 = r8.mDynamicConfigItemList     // Catch: java.lang.Throwable -> L70
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L70
            r4.<init>()     // Catch: java.lang.Throwable -> L70
            r4.append(r0)     // Catch: java.lang.Throwable -> L70
            java.lang.String r5 = "_black"
            r4.append(r5)     // Catch: java.lang.Throwable -> L70
            java.lang.String r4 = r4.toString()     // Catch: java.lang.Throwable -> L70
            java.lang.Object r1 = r1.get(r4)     // Catch: java.lang.Throwable -> L70
            java.util.ArrayList r1 = (java.util.ArrayList) r1     // Catch: java.lang.Throwable -> L70
            boolean r1 = r8.inPunishList(r1, r10, r11)     // Catch: java.lang.Throwable -> L70
            monitor-exit(r2)     // Catch: java.lang.Throwable -> L70
            return r1
        L70:
            r1 = move-exception
            monitor-exit(r2)     // Catch: java.lang.Throwable -> L70
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.vivo.services.autorecover.SystemAutoRecoverService.shouldRecover(java.lang.String, java.lang.String, java.lang.String):boolean");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SurfaceControl.ScreenshotGraphicBuffer captureLayers(SurfaceControl layer, Rect sourceCrop, float frameScale) {
        SurfaceControl.ScreenshotGraphicBuffer captureLayers;
        synchronized (this.mActivityManagerService.mActivityTaskManager.getGlobalLock()) {
            captureLayers = SurfaceControl.captureLayers(layer, sourceCrop, frameScale);
        }
        return captureLayers;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addToDropBox(String tag, String data) {
        this.mDropBoxManager.addText(tag, data);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void appendCurrentCpuState(StringBuilder sb) {
        this.mActivityManagerService.appendCurrentCpuState(sb);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void runShellCommand(String cmd) {
        try {
            this.mVivoDmSrvProxy.runShell(cmd);
        } catch (Exception e) {
            VLog.d(TAG, "runShellCommand cause exception: " + e);
        }
    }

    /* loaded from: classes.dex */
    private final class LocalService extends SystemAutoRecoverManagerInternal {
        private LocalService() {
        }

        @Override // com.vivo.services.autorecover.SystemAutoRecoverManagerInternal
        public void requestCheckForInputException(WindowState win, boolean isOpaque) {
            SystemAutoRecoverService.this.requestCheckForInputException(win, isOpaque);
        }

        @Override // com.vivo.services.autorecover.SystemAutoRecoverManagerInternal
        public void setFocusedWindow(int displayId, WindowState oldFocus, WindowState newFocus) {
            SystemAutoRecoverService.this.setFocusedWindow(displayId, oldFocus, newFocus);
        }

        @Override // com.vivo.services.autorecover.SystemAutoRecoverManagerInternal
        public void setFocusedApps(int displayId, ActivityRecord oldFocus, ActivityRecord newFocus) {
            SystemAutoRecoverService.this.setFocusedApps(displayId, oldFocus, newFocus);
        }

        @Override // com.vivo.services.autorecover.SystemAutoRecoverManagerInternal
        public boolean forceStopFreezingEnabled() {
            return SystemAutoRecoverService.this.mForceStopFreezingEnabled;
        }

        @Override // com.vivo.services.autorecover.SystemAutoRecoverManagerInternal
        public void reportBackKey(KeyEvent keyevent, KeyInterceptionInfo keyInterceptionInfo) {
            WindowState visibleStaringWindow;
            if (SystemAutoRecoverService.this.mStartingWindowBackKeyOptEnabled && keyInterceptionInfo == null && keyevent.getAction() == 0 && (visibleStaringWindow = SystemAutoRecoverService.this.mWindowManagerInternal.getVisibleStartingWindow()) != null) {
                SystemAutoRecoverService.this.goHome();
                ActivityRecord activityRecord = visibleStaringWindow.getActivityRecord();
                if (activityRecord != null && activityRecord.getActivityComponent() != null) {
                    InputExceptionReport.getInstance().reportEventToEpm(activityRecord.getActivityComponent().toShortString(), 10, activityRecord.getActivityComponent().getPackageName(), InvalidWindowRecord.reasonToString(5), "1", "1", String.valueOf(SystemAutoRecoverService.this.isUserAMonkey()));
                }
            }
        }

        @Override // com.vivo.services.autorecover.SystemAutoRecoverManagerInternal
        public void setUserIsMonkey(boolean userIsMonkey) {
            SystemAutoRecoverService.this.mUserIsMonkey = userIsMonkey;
        }

        @Override // com.vivo.services.autorecover.SystemAutoRecoverManagerInternal
        public void setActivityController(IActivityController controller, boolean imAMonkey) {
            SystemAutoRecoverService.this.mHasActivityController = controller != null;
            SystemAutoRecoverService.this.mActivityControllerIsMonkey = imAMonkey;
        }

        @Override // com.vivo.services.autorecover.SystemAutoRecoverManagerInternal
        public void notifyAppDied(int pid, int reason) {
            if (VivoPolicyUtil.IS_LOG_OPEN) {
                VLog.d(SystemAutoRecoverService.TAG, "notifyAppDied pid = " + pid + " reason = " + ExceptionInfo.diedReasonCodeToString(reason));
            }
            SystemAutoRecoverService.this.markExceptionRecoveredFromAppDied(pid, reason);
        }

        @Override // com.vivo.services.autorecover.SystemAutoRecoverManagerInternal
        public void notifyShutDown() {
            SystemAutoRecoverService.this.mShuttingDown = true;
            SystemAutoRecoverService.this.mHandler.removeMessages(4);
        }

        @Override // com.vivo.services.autorecover.SystemAutoRecoverManagerInternal
        public void notifyPasswordMode(boolean isPasswordMode) {
            if (VivoPolicyUtil.IS_LOG_OPEN) {
                VLog.d(SystemAutoRecoverService.TAG, "notifyPasswordMode password mode = " + isPasswordMode);
            }
            SystemAutoRecoverService.this.mPasswordMode = isPasswordMode;
        }

        @Override // com.vivo.services.autorecover.SystemAutoRecoverManagerInternal
        public void onScreenTurnOff() {
            if (VivoPolicyUtil.IS_LOG_OPEN) {
                VLog.d(SystemAutoRecoverService.TAG, "onScreenTurnOff");
            }
            SystemAutoRecoverService.this.markExceptionRecoveredFromScreenOff();
        }
    }

    /* loaded from: classes.dex */
    private class InvalidWindowWatcherHandler extends Handler {
        InvalidWindowWatcherHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    Bundle data = msg.getData();
                    WindowState win = (WindowState) msg.obj;
                    boolean isOpaque = data.getBoolean("isOpaque");
                    InvalidWindowRecord invalidSizeWindowRecord = new InvalidSizeWindowRecord(SystemAutoRecoverService.this, win, 1);
                    if (!invalidSizeWindowRecord.checkException()) {
                        InvalidWindowRecord invalidColorSpaeceWindowRecord = new InvalidColorSpaceWindowRecord(SystemAutoRecoverService.this, win, 1, isOpaque);
                        invalidColorSpaeceWindowRecord.checkException();
                        return;
                    }
                    return;
                case 2:
                    SystemAutoRecoverService.this.updateInvalidWindowPolicyConfig();
                    SystemAutoRecoverService.this.registerPolicyConfigCallback();
                    return;
                case 3:
                    SystemAutoRecoverService.this.updateInvalidWindowPolicyConfig();
                    return;
                case 4:
                    int displayId = msg.arg1;
                    SystemAutoRecoverService.this.punishForNoFocusedWindowTimeOut(displayId);
                    return;
                case 5:
                    InvalidWindowRecord invalidWindowRecord = (InvalidWindowRecord) msg.obj;
                    invalidWindowRecord.checkException();
                    return;
                case 6:
                    InvalidWindowRecord exception = (InvalidWindowRecord) msg.obj;
                    Bundle data2 = msg.getData();
                    boolean shouldForceRecover = data2.getBoolean("shouldForceRecover");
                    if (shouldForceRecover && exception.canRecover(true, false, 8, SystemClock.elapsedRealtime())) {
                        VLog.d(SystemAutoRecoverService.TAG, "Going to recover exception due to time out : " + exception);
                        exception.recover(false);
                    }
                    exception.report();
                    synchronized (SystemAutoRecoverService.this.mPendingReportExceptions) {
                        SystemAutoRecoverService.this.mPendingReportExceptions.remove(exception);
                    }
                    return;
                case 7:
                    InvalidWindowRecord exception2 = (InvalidWindowRecord) msg.obj;
                    Bundle data3 = msg.getData();
                    boolean background = data3.getBoolean("isBackground");
                    exception2.recover(background);
                    exception2.report();
                    synchronized (SystemAutoRecoverService.this.mPendingReportExceptions) {
                        SystemAutoRecoverService.this.mPendingReportExceptions.remove(exception2);
                    }
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerPolicyConfigCallback() {
        this.mConfigurationManager.registerObserver(this.mWhiteList, new AutoRecoverPolicyObserver());
        this.mConfigurationManager.registerObserver(this.mSwitchList, new AutoRecoverPolicyObserver());
        this.mConfigurationManager.registerObserver(this.mBlackList, new AutoRecoverPolicyObserver());
        this.mConfigurationManager.registerObserver(this.mExceptionHandler.getCurrentList(), new AutoRecoverPolicyObserver());
        this.mConfigurationManager.registerObserver(this.mBlackWindowCheckFromNoFocusTimeDetailPolicy, new AutoRecoverPolicyObserver());
        this.mConfigurationManager.registerObserver(this.mDebugPolicy, new AutoRecoverPolicyObserver());
        registerDynamicConfigCallback();
    }

    private void registerDynamicConfigCallback() {
        for (Map.Entry<String, ContentValuesList> entry : this.mDynamicConfigMap.entrySet()) {
            this.mConfigurationManager.registerObserver(entry.getValue(), new AutoRecoverPolicyObserver());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateInvalidWindowPolicyConfig() {
        this.mSwitchList = this.mConfigurationManager.getContentValuesList(INVALID_WINDOW_POLICY_CONFIG_FILE, TAG_SWITCH_LIST);
        this.mWhiteList = this.mConfigurationManager.getContentValuesList(INVALID_WINDOW_POLICY_CONFIG_FILE, TAG_WHITE_LIST);
        this.mBlackList = this.mConfigurationManager.getContentValuesList(INVALID_WINDOW_POLICY_CONFIG_FILE, TAG_BLACK_LIST);
        ContentValuesList contentValuesList = this.mConfigurationManager.getContentValuesList(INVALID_WINDOW_POLICY_CONFIG_FILE, "black_window_check_from_no_focus_time_out_policy");
        this.mBlackWindowCheckFromNoFocusTimeDetailPolicy = contentValuesList;
        NoFocusWindowRecord.setConfig(contentValuesList);
        ContentValuesList contentValuesList2 = this.mConfigurationManager.getContentValuesList(INVALID_WINDOW_POLICY_CONFIG_FILE, "debug_policy");
        this.mDebugPolicy = contentValuesList2;
        InvalidWindowRecord.setConfig(contentValuesList2);
        boolean z = true;
        this.mInputCheckEnabled = "true".equals(FtFeature.getFeatureAttribute("vivo.opt.atypicalhang", "check_input", "true")) && VALUE_SWITCH_ON.equals(this.mSwitchList.getValue(TAG_SWITCH_INPUT_CHECK));
        this.mPunishEnabled = VALUE_SWITCH_ON.equals(this.mSwitchList.getValue(TAG_SWITCH_PUNISH));
        this.mForceStopFreezingEnabled = FtFeature.isFeatureSupport("vivo.software.forcestopfreezing") && !VALUE_SWITCH_OFF.equals(this.mSwitchList.getValue(TAG_SWITCH_FORCE_STOP_FREEZING));
        this.mStartingWindowBackKeyOptEnabled = FtFeature.isFeatureSupport("vivo.software.startingwindowbackkeyopt") && !VALUE_SWITCH_OFF.equals(this.mSwitchList.getValue(TAG_SWITCH_STARTING_WINDOW_BACK_KEY_OPT));
        if (!"true".equals(FtFeature.getFeatureAttribute("vivo.opt.atypicalhang", "check_focus_change", "false")) || VALUE_SWITCH_OFF.equals(this.mSwitchList.getValue(TAG_SWITCH_FOCUS_CHANGE_CHECK))) {
            z = false;
        }
        this.mFocusChangeCheckEnabled = z;
        ArrayList<WindowItem> tempWhiteList = buildList(this.mWhiteList);
        if (tempWhiteList != null) {
            synchronized (this.mTransparentWindowWhiteList) {
                this.mTransparentWindowWhiteList.clear();
                this.mTransparentWindowWhiteList.addAll(tempWhiteList);
            }
        }
        ArrayList<WindowItem> tempBlackList = buildList(this.mBlackList);
        if (tempBlackList != null) {
            synchronized (this.mTransparentWindowBlackList) {
                this.mTransparentWindowBlackList.clear();
                this.mTransparentWindowBlackList.addAll(tempBlackList);
            }
        }
        updateDynamicConfig();
        this.mExceptionHandler.updateExceptionMatcher(this.mConfigurationManager.getContentValuesList(INVALID_WINDOW_POLICY_CONFIG_FILE, POLICY_EXCEPTION_CATCHER));
    }

    private void updateDynamicConfig() {
        String[] strArr;
        String[] strArr2;
        synchronized (this.mDynamicConfigItemList) {
            for (String policyName : TAG_DYNAMIC_POLICY_NAME) {
                for (String policyType : TAG_POLICY_TYPE_NAME) {
                    String name = policyName + policyType;
                    ContentValuesList contentValuesList = this.mConfigurationManager.getContentValuesList(INVALID_WINDOW_POLICY_CONFIG_FILE, name);
                    VLog.d(TAG, "getContentValuesList contentValuesList = " + contentValuesList);
                    this.mDynamicConfigMap.put(name, contentValuesList);
                    ArrayList<WindowItem> policyItemList = buildList(contentValuesList);
                    this.mDynamicConfigItemList.put(name, policyItemList);
                }
            }
        }
    }

    public void requestCheckForInputException(WindowState win, boolean isOpaque) {
        if (this.mInputCheckEnabled) {
            if (VivoPolicyUtil.IS_LOG_OPEN) {
                VLog.d(TAG, "requestCheckForInputException win = " + win + " isOpaque = " + isOpaque);
            }
            if (this.mHandler != null) {
                Bundle data = new Bundle();
                data.putBoolean("isOpaque", isOpaque);
                Message message = this.mHandler.obtainMessage(1);
                message.obj = win;
                message.setData(data);
                this.mHandler.sendMessage(message);
            }
        }
    }

    private void requestCheckForFocusChanged(WindowState win) {
        if (this.mFocusChangeCheckEnabled) {
            this.mHandler.removeMessages(5);
            if (win != null) {
                InvalidWindowRecord invalidWindowRecord = new InvalidSizeWindowRecord(this, win, 3);
                if (VivoPolicyUtil.IS_LOG_OPEN) {
                    VLog.d(TAG, "requestCheckForFocusChanged win = " + invalidWindowRecord.mWin + " isOpaque = " + invalidWindowRecord.mIsOpaque + " scene = " + invalidWindowRecord.mScene);
                }
                Handler handler = this.mHandler;
                if (handler != null) {
                    Message message = handler.obtainMessage(5);
                    message.obj = invalidWindowRecord;
                    this.mHandler.sendMessageDelayed(message, 3000L);
                }
            }
        }
    }

    private void checkNoFocusedWindow(int displayId, WindowState windowState) {
        PowerManagerInternal powerManagerInternal;
        this.mHandler.removeMessages(4);
        if (windowState == null && !this.mShuttingDown && (powerManagerInternal = this.mPowerManagerInternal) != null && powerManagerInternal.isInteractiveWithProximityState()) {
            Message message = this.mHandler.obtainMessage(4);
            message.arg1 = displayId;
            this.mHandler.sendMessageDelayed(message, DEFAULT_NO_FOCUSED_WINDOW_CHECK_DELAY);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void punishForNoFocusedWindowTimeOut(int displayId) {
        ActivityRecord focusedApp;
        PowerManagerInternal powerManagerInternal = this.mPowerManagerInternal;
        if (powerManagerInternal != null && !powerManagerInternal.isInteractiveWithProximityState()) {
            return;
        }
        synchronized (this.mLock) {
            focusedApp = this.mFocusedApps.get(Integer.valueOf(displayId));
        }
        if (focusedApp != null && focusedApp.getApp() != null) {
            InvalidWindowRecord invalidWindowRecord = new NoFocusWindowRecord(this, 4, true, focusedApp, displayId);
            invalidWindowRecord.checkException();
        }
    }

    public void setFocusedWindow(int displayId, WindowState oldFocus, WindowState newFocus) {
        synchronized (this.mLock) {
            this.mFocusedWindows.put(Integer.valueOf(displayId), newFocus);
        }
        requestCheckForFocusChanged(newFocus);
        checkNoFocusedWindow(displayId, newFocus);
        markExceptionRecoveredFromFocusChanged(oldFocus, newFocus);
    }

    public void setFocusedApps(int displayId, ActivityRecord oldFocus, ActivityRecord newFocus) {
        synchronized (this.mLock) {
            this.mFocusedApps.put(Integer.valueOf(displayId), newFocus);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void finishActivity(IBinder token, String reason) {
        boolean finished = this.mActivityManagerService.finishActivity(token, 0, (Intent) null, 0);
        StringBuilder sb = new StringBuilder();
        sb.append(finished ? "Success" : "Failed");
        sb.append(" to finish invalid activity: ");
        sb.append(token);
        sb.append(" because of ");
        sb.append(reason);
        VLog.d(TAG, sb.toString());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void forceStopPackage(String packageName, int userId, String reason) {
        VLog.d(TAG, "forceStopPackage: " + packageName + " userId: " + userId + " because of " + reason);
        this.mActivityManagerService.forceStopPackage(packageName, userId, true, reason);
    }

    void goHome() {
        Intent intent = new Intent("android.intent.action.MAIN", (Uri) null);
        intent.addCategory("android.intent.category.HOME");
        intent.addFlags(270532608);
        if (isUserSetupComplete()) {
            VLog.d(TAG, "Force go home due to back key trigger when starting window shown!");
            this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
            return;
        }
        VLog.i(TAG, "Not starting activity because user setup is in progress: " + intent);
    }

    private boolean isUserSetupComplete() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 0, -2) != 0;
    }

    private ArrayList<WindowItem> buildList(ContentValuesList list) {
        if (list != null) {
            ArrayList<WindowItem> tempList = new ArrayList<>();
            HashMap<String, ContentValues> values = list.getValues();
            values.keySet();
            for (String title : values.keySet()) {
                tempList.add(new WindowItem(title, list.getValue(title)));
            }
            return tempList;
        }
        return null;
    }

    private boolean inPunishList(ArrayList<WindowItem> list, String packageName, String componentName) {
        if (list == null) {
            return false;
        }
        WindowItem windowItem = new WindowItem(componentName, packageName);
        synchronized (this.mDynamicConfigItemList) {
            Iterator<WindowItem> it = list.iterator();
            while (it.hasNext()) {
                WindowItem item = it.next();
                if ((!windowItem.mPackageName.equals(item.mPackageName) && !WindowItem.COMMON_COMPONENT_TAG.equals(item.mPackageName)) || (!windowItem.mTitle.contains(item.mTitle) && (!TextUtils.isEmpty(windowItem.mTitle) || !WindowItem.ALL_WINDOW_TAG.equals(item.mTitle)))) {
                }
                return true;
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean inTransparentWhiteList(WindowState win) {
        String title = win.getAttrs().getTitle().toString();
        String packageName = win.getAttrs().packageName;
        synchronized (this.mTransparentWindowWhiteList) {
            Iterator<WindowItem> it = this.mTransparentWindowWhiteList.iterator();
            while (it.hasNext()) {
                WindowItem item = it.next();
                if ((!packageName.equals(item.mPackageName) && !WindowItem.COMMON_COMPONENT_TAG.equals(item.mPackageName)) || (!title.contains(item.mTitle) && (!TextUtils.isEmpty(title) || !WindowItem.ALL_WINDOW_TAG.equals(item.mTitle)))) {
                }
                VLog.d(TAG, "Ignore white list window: " + win);
                return true;
            }
            return false;
        }
    }

    private void markExceptionRecoveredFromFocusChanged(WindowState oldFocus, WindowState newFocus) {
        if (newFocus != null) {
            boolean isNewFocusHome = newFocus.isActivityTypeHome();
            long now = SystemClock.elapsedRealtime();
            synchronized (this.mPendingReportExceptions) {
                Iterator<InvalidWindowRecord> it = this.mPendingReportExceptions.iterator();
                while (it.hasNext()) {
                    InvalidWindowRecord exception = it.next();
                    if (isNewFocusHome && exception.canRecover(true, true, 1, now)) {
                        VLog.d(TAG, "Going to recover exception due to policy: " + exception);
                        forceRecoverPendingException(exception);
                    } else {
                        markExceptionRecovered(exception, 1, newFocus.getAttrs().getTitle().toString());
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void markExceptionRecoveredFromAppDied(int pid, int reason) {
        if (pid > 0) {
            synchronized (this.mPendingReportExceptions) {
                Iterator<InvalidWindowRecord> it = this.mPendingReportExceptions.iterator();
                while (it.hasNext()) {
                    InvalidWindowRecord exception = it.next();
                    if (exception.getExceptionInfo().getPid() == pid) {
                        markExceptionRecovered(exception, 2, ExceptionInfo.diedReasonCodeToString(reason));
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void markExceptionRecoveredFromScreenOff() {
        long now = SystemClock.elapsedRealtime();
        synchronized (this.mPendingReportExceptions) {
            Iterator<InvalidWindowRecord> it = this.mPendingReportExceptions.iterator();
            while (it.hasNext()) {
                InvalidWindowRecord exception = it.next();
                if (exception.canRecover(true, true, 16, now)) {
                    forceRecoverPendingException(exception);
                } else {
                    markExceptionRecovered(exception, 16, null);
                }
            }
        }
    }

    private void markExceptionRecovered(InvalidWindowRecord exception, int recoverWay, String recoverReason) {
        long now = SystemClock.elapsedRealtime();
        if (exception.canRecover(false, false, recoverWay, now) && this.mHandler.hasMessages(6, exception)) {
            String recoveredReason = InvalidWindowRecord.recoverWayToString(recoverWay, recoverReason);
            this.mHandler.removeCallbacksAndMessages(exception);
            exception.getExceptionInfo().setRecoveredTime(now);
            exception.getExceptionInfo().setRecoveredReason(recoveredReason);
            VLog.d(TAG, "markExceptionRecovered: exceptionInfo = " + exception);
            Message message = Message.obtain(this.mHandler, 6, exception);
            Bundle data = new Bundle();
            data.putBoolean("shouldForceRecover", false);
            message.setData(data);
            this.mHandler.sendMessage(message);
        }
    }

    private void forceRecoverPendingException(InvalidWindowRecord exception) {
        if (this.mHandler.hasMessages(6, exception)) {
            this.mHandler.removeCallbacksAndMessages(exception);
            Message message = Message.obtain(this.mHandler, 7, exception);
            Bundle data = new Bundle();
            data.putBoolean("isBackground", true);
            message.setData(data);
            this.mHandler.sendMessage(message);
        }
    }

    /* loaded from: classes.dex */
    class GameModeObserver extends ContentObserver {
        public GameModeObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = SystemAutoRecoverService.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor("is_game_mode"), false, this, -1);
            SystemAutoRecoverService.this.updateGameMode();
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            SystemAutoRecoverService.this.updateGameMode();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateGameMode() {
        this.mIsGameMode = Settings.System.getIntForUser(this.mContext.getContentResolver(), "is_game_mode", 0, -2) == 1;
        if (VivoPolicyUtil.IS_LOG_OPEN) {
            VLog.d(TAG, "updateGameMode mIsGameMode = " + this.mIsGameMode);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class WindowItem {
        public static final String ALL_WINDOW_TAG = "ALL";
        public static final String COMMON_COMPONENT_TAG = "COMMON_COMPONENT";
        String mPackageName;
        String mTitle;

        WindowItem(String title, String packageName) {
            this.mTitle = title;
            this.mPackageName = packageName;
        }

        public String toString() {
            return "WindowItem{mTitle='" + this.mTitle + "', mPackageName='" + this.mPackageName + "'}";
        }
    }

    private static void reloadConfig() {
        AlgorithmUtil.reloadThreshold();
        InvalidWindowRecord.reloadConfig();
        DEFAULT_NO_FOCUSED_WINDOW_CHECK_DELAY = Math.max(SystemProperties.getInt("persist.vivo.nofocus.timeout", 6000), 6000);
        sReportImmediately = "true".equals(SystemProperties.get("persist.vivo.autorecover.report.immediately", "false"));
        sIgnoreDebug = "true".equals(SystemProperties.get("persist.vivo.ars.ignoredebug", "false"));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isCtsPackage(String packageName) {
        return !TextUtils.isEmpty(packageName) && (packageName.contains(".cts") || packageName.contains(".gts") || packageName.contains(".test"));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isDebugEnabled(String packageName) {
        return "BOTH".equals(getDebugState(packageName));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getDebugState(String packageName) {
        if (sIgnoreDebug) {
            return "IGNORED";
        }
        boolean isAdbDebugEnabled = isAdbDebugEnabled();
        boolean isAppDebugEnabled = isAppDebugEnabled(packageName);
        if (isAdbDebugEnabled && isAppDebugEnabled) {
            return "BOTH";
        }
        if (isAdbDebugEnabled) {
            return "ADB_DEBUG_ENABLED";
        }
        if (isAppDebugEnabled) {
            return "APP_DEBUG_ENABLED";
        }
        return "DISABLED";
    }

    boolean isAppDebugEnabled(String packageName) {
        try {
            ApplicationInfo ai = AppGlobals.getPackageManager().getApplicationInfo(packageName, (int) Dataspace.STANDARD_BT601_625, 0);
            if (ai == null) {
                return false;
            }
            boolean isDebuggable = (ai.flags & 2) != 0;
            return isDebuggable;
        } catch (Exception e) {
            return false;
        }
    }

    boolean isAdbDebugEnabled() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "adb_enabled", 0) == 1;
    }

    public void dump(String prefix, PrintWriter pw) {
        reloadConfig();
        pw.println(prefix + TAG);
        String prefix2 = prefix + "    ";
        pw.println(prefix2 + "BLACK_SCALE_PIXEL = " + AlgorithmUtil.BLACK_SCALE_PIXEL);
        pw.println(prefix2 + "TRANSPARENT_SCALE_PIXEL = " + AlgorithmUtil.TRANSPARENT_SCALE_PIXEL);
        pw.println(prefix2 + "DEBUG_SCREEN_SHOT = " + AlgorithmUtil.DEBUG_SCREEN_SHOT);
        pw.println(prefix2 + "INVALID_WINDOW_SIZE_THRESHOLD = " + InvalidSizeWindowRecord.INVALID_WINDOW_SIZE_THRESHOLD);
        pw.println(prefix2 + "DEFAULT_NO_FOCUSED_WINDOW_CHECK_DELAY = " + DEFAULT_NO_FOCUSED_WINDOW_CHECK_DELAY);
        pw.println(prefix2 + "CHECK_APP_DRAW_BLACK = " + CHECK_APP_DRAW_BLACK);
        pw.println(prefix2 + "mInputCheckEnabled = " + this.mInputCheckEnabled);
        pw.println(prefix2 + "mFocusChangeCheckEnabled = " + this.mFocusChangeCheckEnabled);
        pw.println(prefix2 + "mPunishEnabled = " + this.mPunishEnabled);
        pw.println(prefix2 + "mForceStopFreezingEnabled = " + this.mForceStopFreezingEnabled);
        pw.println(prefix2 + "mStartingWindowBackKeyOptEnabled = " + this.mStartingWindowBackKeyOptEnabled);
        pw.println(prefix2 + "mPasswordMode = " + this.mPasswordMode);
        pw.println(prefix2 + "mPunishAll = " + this.mPunishAll);
        pw.println(prefix2 + "sIgnoreDebug = " + sIgnoreDebug);
        pw.println();
        NoFocusWindowRecord.dump(prefix2, pw);
        pw.println();
        InvalidWindowRecord.dump(prefix2, pw);
        pw.println();
        pw.println(prefix2 + "TransparentWhiteList:");
        synchronized (this.mTransparentWindowWhiteList) {
            Iterator<WindowItem> it = this.mTransparentWindowWhiteList.iterator();
            while (it.hasNext()) {
                WindowItem item = it.next();
                pw.println(prefix2 + prefix2 + item);
            }
        }
        pw.println();
        pw.println(prefix2 + "TransparentBlackList:");
        synchronized (this.mTransparentWindowBlackList) {
            Iterator<WindowItem> it2 = this.mTransparentWindowBlackList.iterator();
            while (it2.hasNext()) {
                WindowItem item2 = it2.next();
                pw.println(prefix2 + prefix2 + item2);
            }
        }
        pw.println();
        synchronized (this.mDynamicConfigItemList) {
            pw.println(prefix2 + "Switches:");
            pw.println(this.mSwitchList.toString());
            pw.println();
            for (Map.Entry<String, ArrayList<WindowItem>> entry : this.mDynamicConfigItemList.entrySet()) {
                pw.println(prefix2 + entry.getKey() + " List:");
                Iterator<WindowItem> it3 = entry.getValue().iterator();
                while (it3.hasNext()) {
                    WindowItem item3 = it3.next();
                    pw.println(prefix2 + prefix2 + item3);
                }
                pw.println();
            }
        }
        pw.println();
        pw.println(prefix2 + "PendingReportExceptions:");
        synchronized (this.mPendingReportExceptions) {
            Iterator<InvalidWindowRecord> it4 = this.mPendingReportExceptions.iterator();
            while (it4.hasNext()) {
                InvalidWindowRecord item4 = it4.next();
                pw.println(prefix2 + item4);
            }
        }
    }
}