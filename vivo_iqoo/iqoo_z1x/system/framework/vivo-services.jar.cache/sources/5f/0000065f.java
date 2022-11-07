package com.vivo.services.nightmode;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.IProcessObserver;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.os.Binder;
import android.os.FtBuild;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import com.vivo.common.utils.VLog;
import com.vivo.services.backup.util.VivoBackupCommonUtil;
import com.vivo.services.nightmode.VivoNightModeService;
import com.vivo.services.superresolution.Constant;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import vivo.app.VivoFrameworkFactory;
import vivo.app.configuration.AbsConfigurationManager;
import vivo.app.configuration.ConfigurationObserver;
import vivo.app.configuration.ContentValuesList;
import vivo.app.nightmode.INightModeAppConfigChangeCallback;
import vivo.app.nightmode.IVivoNightModeManager;
import vivo.app.nightmode.NightModeController;
import vivo.app.nightmode.NightModeUtils;
import vivo.contentcatcher.IActivityObserver;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoNightModeService extends IVivoNightModeManager.Stub {
    private static final ArrayList<String> DISABLED_VNIGHT_MODE_PACKAGE_LIST;
    private static ArrayList<String> DONT_IN_SHOW_PACKAGE_LIST = null;
    private static final int NIGHT_MODE_GREY_VALUE_DONT_RECTIFIED = 2;
    private static final int NIGHT_MODE_GREY_VALUE_INIT = 0;
    private static final int NIGHT_MODE_GREY_VALUE_NEED_RECTIFIED = 1;
    private static final String NIGHT_MODE_LIST_PATH = "data/bbkcore/night_mode_list_2.0.xml";
    private static final int PANEL_GREY_LIMIT;
    private static final String SETTINGS_DISABLED_THIRD_APP = "vivo_nightmode_disabled_thirdapp";
    private static final String SETTINGS_KEY_GREY_VALUE_RECTIFIED = "vivo_nightmode_grey_rectified";
    private static final String STR_NIGHT_MODE_WEBVIEW_WHITELIST = "night_mode_webview_whitelists";
    private static final String STR_NIGHT_MODE_WHITELIST = "night_mode_whitelists";
    private static final int VERSION_EQUAL = 0;
    private static final int VERSION_LARGER = 1;
    private static final int VERSION_SMALLER = -1;
    private static boolean isRectifyGreyValueFinished;
    private static HashSet<String> mWebViewSets;
    private static int nightModeGreyRectifyLevel;
    private static float nightModeRectifiedOffsetLightest;
    private Context mContext;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private PackageManager mPm;
    private UiModeManager mUiModeManager;
    private final String TAG = "VivoNightModeService";
    private final boolean DEBUG = SystemProperties.getBoolean("persist.sys.debug.nightmode", false);
    private boolean mNightModeSwitchLocked = false;
    private boolean mPendingSwitchLocked = false;
    private int mPendingSwitchUserId = 0;
    private boolean mPendingModeToNight = false;
    private boolean firstChangeMode = true;
    private ArrayList<String> mDisablePackages = new ArrayList<>();
    private ArrayMap<String, VivoNightModeState> mNightModeStates = new ArrayMap<>();
    private int mVersion = 0;
    private String mResumedActivityName = null;
    private String FEATURE_OVER_SEAS = null;
    private ArrayMap<String, Boolean> mIsSystemPkgMap = new ArrayMap<>();
    private boolean isCustomModeWaitForScreenOff = false;
    private IProcessObserver mProcessObserver = new IProcessObserver.Stub() { // from class: com.vivo.services.nightmode.VivoNightModeService.1
        public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
            if (VivoNightModeService.this.mPendingSwitchLocked) {
                VLog.d("VivoNightModeService", "onForegroundActivitiesChanged pending switch, now switch to night " + VivoNightModeService.this.mPendingModeToNight);
                VivoNightModeService vivoNightModeService = VivoNightModeService.this;
                vivoNightModeService.switchNightModeWithoutPending(vivoNightModeService.mPendingModeToNight, VivoNightModeService.this.mPendingSwitchUserId, false);
            }
        }

        public void onProcessDied(int pid, int uid) {
        }

        public void onForegroundServicesChanged(int pid, int uid, int serviceTypes) {
        }
    };
    IActivityObserver mActivityObserver = new IActivityObserver.Stub() { // from class: com.vivo.services.nightmode.VivoNightModeService.2
        /* JADX WARN: Type inference failed for: r0v2, types: [com.vivo.services.nightmode.VivoNightModeService$2$1] */
        public void activityResumed(int pid, int uid, ComponentName componentName) throws RemoteException {
            if (componentName != null) {
                VivoNightModeService.this.mResumedActivityName = componentName.getClassName();
            }
            if (VivoNightModeService.this.mPendingSwitchLocked) {
                new Thread() { // from class: com.vivo.services.nightmode.VivoNightModeService.2.1
                    @Override // java.lang.Thread, java.lang.Runnable
                    public void run() {
                        VLog.d("VivoNightModeService", "activityResumed pending switch, now switch to night " + VivoNightModeService.this.mPendingModeToNight);
                        VivoNightModeService.this.switchNightModeWithoutPending(VivoNightModeService.this.mPendingModeToNight, VivoNightModeService.this.mPendingSwitchUserId, false);
                    }
                }.start();
            }
        }

        public void activityPaused(int pid, int uid, ComponentName componentName) throws RemoteException {
        }
    };
    private ArrayMap<String, NightModeFilter> mNightModeFilter = new ArrayMap<>();
    private ArraySet<ComponentName> mNightModeWhiteActivity = new ArraySet<>();
    private ArraySet<String> mNightModeForceAppEnabled = new ArraySet<>();
    private VivoFrameworkFactory mVivoFrameworkFactory = null;
    private AbsConfigurationManager mConfigurationManager = null;
    private ContentValuesList mNightModeWhiteLists = null;
    private ContentValuesList mNightModeWebViewWhiteLists = null;
    private ConfigurationObserver mNightModeWhiteListsObserver = new AnonymousClass4();
    private ConfigurationObserver mNightModeWebViewWhiteListsObserver = new AnonymousClass5();
    private Map<String, Boolean> mThirdAppMap = new ArrayMap();
    private ArraySet<String> mDisabledThirdAppSet = new ArraySet<>();
    private final RemoteCallbackList<INightModeAppConfigChangeCallback> mConfigChangeListener = new RemoteCallbackList<>();
    private String mDisabledThirdAppString = null;
    private int mCurSystemUid = 0;
    private boolean isVersionRom12Upper = false;
    private boolean isFetchLatestConfigFailed = false;

    public VivoNightModeService(Context context) {
        this.mUiModeManager = (UiModeManager) context.getSystemService("uimode");
        this.mContext = context;
        this.mPm = context.getPackageManager();
        HandlerThread handlerThread = new HandlerThread("VivoNightModeService", 10);
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        MyHandler myHandler = new MyHandler(this.mHandlerThread.getLooper());
        this.mHandler = myHandler;
        myHandler.post(new Runnable() { // from class: com.vivo.services.nightmode.-$$Lambda$VivoNightModeService$XkFf0MJaLfiGqK6SbnqXTeb5hCs
            @Override // java.lang.Runnable
            public final void run() {
                VivoNightModeService.this.lambda$new$0$VivoNightModeService();
            }
        });
        lambda$registerActivityObserver$1$VivoNightModeService(context);
        registerProcessObserver();
        shouldLimitGreyValue();
    }

    public /* synthetic */ void lambda$new$0$VivoNightModeService() {
        initVersionInfo();
        registerBroadcastReceivers();
        initUnifiedConfigs();
        readStateForUser();
        initThirdAppInfo();
    }

    public void registerProcessObserver() {
        try {
            ActivityManagerNative.getDefault().registerProcessObserver(this.mProcessObserver);
        } catch (Exception e) {
            VLog.e("VivoNightModeService", "error registerProcessObserver " + e);
        }
    }

    /* renamed from: registerActivityObserver */
    public void lambda$registerActivityObserver$1$VivoNightModeService(final Context mContext) {
        try {
            ActivityManager.getService().registerActivityObserver(this.mActivityObserver);
        } catch (Exception e) {
            VLog.d("VivoNightModeService", "Failure regiester observer", e);
            this.mHandler.postDelayed(new Runnable() { // from class: com.vivo.services.nightmode.-$$Lambda$VivoNightModeService$32l212pEzEewlesX7R39CN_Enk4
                @Override // java.lang.Runnable
                public final void run() {
                    VivoNightModeService.this.lambda$registerActivityObserver$1$VivoNightModeService(mContext);
                }
            }, VivoBackupCommonUtil.TIMEOUT_MONITOR_INTERVAL);
        }
    }

    public void switchNightMode(boolean night, boolean pending, int userId) {
        if (isOtherUserSwitch(night, userId)) {
            return;
        }
        if (pending) {
            if (this.isCustomModeWaitForScreenOff) {
                switchNightModeWithoutPending(night, userId, true);
                return;
            }
            this.mPendingSwitchLocked = true;
            this.mPendingSwitchUserId = userId;
            this.mPendingModeToNight = night;
            VLog.d("VivoNightModeService", "pending switch to night ?" + night);
            return;
        }
        switchNightModeWithoutPending(night, userId, false);
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        String opt;
        boolean dump_enable_package = false;
        boolean dump_disable_package = false;
        boolean enable_web = false;
        boolean disable_web = false;
        int opti = 0;
        while (opti < args.length && (opt = args[opti]) != null && opt.length() > 0 && opt.charAt(0) == '-') {
            opti++;
            if ("--disable".equals(opt)) {
                dump_disable_package = true;
            } else if ("--enable".equals(opt)) {
                dump_enable_package = true;
            } else if ("--enable-web".equals(opt)) {
                enable_web = true;
            } else if ("--disable-web".equals(opt)) {
                disable_web = true;
            }
        }
        if (enable_web && args.length == 2) {
            mWebViewSets.add(args[1]);
        }
        if (disable_web && args.length == 2) {
            mWebViewSets.remove(args[1]);
        }
        if (dump_disable_package) {
            if (args.length == 2) {
                disable(args[1]);
            } else if (this.mDisablePackages.size() > 0) {
                Iterator<String> it = this.mDisablePackages.iterator();
                while (it.hasNext()) {
                    String tmp = it.next();
                    pw.println(tmp);
                }
            }
        }
        if (dump_enable_package) {
            if (args.length == 2) {
                enable(args[1]);
                return;
            }
            Set<String> setKey = this.mNightModeStates.keySet();
            for (String key : setKey) {
                VivoNightModeState tmp2 = this.mNightModeStates.get(key);
                pw.println(tmp2.getPkgName() + "------disabled:" + tmp2.isDisabled());
            }
        }
    }

    private void disable(String pkg) {
        this.mDisablePackages.add(pkg);
        if (!this.mNightModeStates.containsKey(pkg)) {
            VivoNightModeState vns = new VivoNightModeState(pkg, true, 0);
            this.mNightModeStates.put(pkg, vns);
            return;
        }
        VivoNightModeState vns2 = this.mNightModeStates.get(pkg);
        vns2.setStates(true, 0);
    }

    private void enable(String pkg) {
        int i = 0;
        int length = this.mDisablePackages.size();
        while (i < length) {
            String item = this.mDisablePackages.get(i);
            if (item.equals(pkg)) {
                this.mDisablePackages.remove(item);
                length--;
                i--;
            }
            i++;
        }
        if (!this.mNightModeStates.containsKey(pkg)) {
            VivoNightModeState vns = new VivoNightModeState(pkg, false, 0);
            this.mNightModeStates.put(pkg, vns);
            return;
        }
        VivoNightModeState vns2 = this.mNightModeStates.get(pkg);
        vns2.setStates(false, 0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void switchNightModeWithoutPending(boolean night, int userId, boolean isCustomModeWaitForScreenOff) {
        long origId = Binder.clearCallingIdentity();
        if (this.mNightModeSwitchLocked || this.mUiModeManager == null) {
            VLog.w("VivoNightModeService", "switch mode in process " + this.mNightModeSwitchLocked);
            return;
        }
        try {
            try {
                this.firstChangeMode = false;
                this.mNightModeSwitchLocked = true;
                this.mPendingSwitchLocked = false;
                if (!isCustomModeWaitForScreenOff) {
                    setNightModeDatabase(night, userId);
                }
                this.mUiModeManager.setCustomModeWaitForScreenOff(isCustomModeWaitForScreenOff);
                this.mUiModeManager.getNightMode();
                if (!night) {
                    this.mUiModeManager.setNightMode(1);
                } else {
                    this.mUiModeManager.setNightMode(2);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } finally {
            this.mNightModeSwitchLocked = false;
            VLog.d("VivoNightModeService", "finished switch nightmode ...");
            Binder.restoreCallingIdentity(origId);
        }
    }

    private void setNightModeDatabase(boolean night, int userId) {
        if (userId == 999) {
            userId = 0;
        }
        try {
            Settings.System.putIntForUser(this.mContext.getContentResolver(), "vivo_nightmode_used", night ? 1 : -2, userId);
            VLog.d("VivoNightModeService", "switch night mode to " + night);
        } catch (Exception e) {
            VLog.w("VivoNightModeService", "setNightModeDatabase, " + e);
        }
    }

    private boolean isOtherUserSwitch(boolean night, int userId) {
        int curSystemUid = getCurrentSystemUser();
        if (userId != curSystemUid) {
            setNightModeDatabase(night, userId);
            return true;
        }
        return false;
    }

    public boolean neverChangedMode() {
        return this.firstChangeMode;
    }

    public boolean isNightModeSwitching() {
        return this.mNightModeSwitchLocked;
    }

    private void registerBroadcastReceivers() {
        IntentFilter pkgFilter = new IntentFilter();
        pkgFilter.addAction("android.intent.action.PACKAGE_ADDED");
        pkgFilter.addAction("android.intent.action.PACKAGE_REPLACED");
        pkgFilter.addDataScheme("package");
        this.mContext.registerReceiver(new BroadcastReceiver() { // from class: com.vivo.services.nightmode.VivoNightModeService.3
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    String action = intent.getAction();
                    if ("android.intent.action.PACKAGE_ADDED".equals(action) || "android.intent.action.PACKAGE_REPLACED".equals(action)) {
                        Uri uri = intent.getData();
                        String pkg = uri != null ? uri.getSchemeSpecificPart() : null;
                        VivoNightModeService.this.updateNightModeFilter(pkg);
                    }
                }
            }
        }, pkgFilter);
    }

    public boolean isNightModeWhiteApp(String pkg) {
        boolean result;
        if (TextUtils.isEmpty(pkg)) {
            return false;
        }
        synchronized (this.mNightModeStates) {
            result = getNightModeState(pkg).isDisabled() ? false : true;
        }
        if (this.DEBUG) {
            VSlog.d("VivoNightModeService", pkg + " isWhiteApp = " + result);
        }
        return result;
    }

    public boolean isDisableNightMode(String pkg) {
        boolean z;
        boolean whiteApp = isNightModeWhiteApp(pkg);
        boolean isSystemPkg = false;
        boolean z2 = true;
        try {
            if (this.mIsSystemPkgMap.containsKey(pkg)) {
                isSystemPkg = this.mIsSystemPkgMap.get(pkg).booleanValue();
            } else {
                ApplicationInfo info = this.mPm.getApplicationInfo(pkg, 0);
                if (info != null) {
                    if (!info.isSystemApp() && !info.isUpdatedSystemApp()) {
                        z = false;
                        isSystemPkg = z;
                        this.mIsSystemPkgMap.put(pkg, Boolean.valueOf(isSystemPkg));
                    }
                    z = true;
                    isSystemPkg = z;
                    this.mIsSystemPkgMap.put(pkg, Boolean.valueOf(isSystemPkg));
                } else {
                    VLog.d("VivoNightModeService", "isDisablePkg? " + pkg + "  get info null");
                }
            }
            whiteApp |= isSystemPkg;
        } catch (Exception e) {
            VLog.d("VivoNightModeService", "isDisablePkg? " + pkg + "  get info exception", e);
        }
        synchronized (this.mDisablePackages) {
            if (!this.mDisablePackages.contains(pkg) && whiteApp) {
                z2 = false;
            }
        }
        return z2;
    }

    public void disableNightMode(String pkg) {
        synchronized (this.mDisablePackages) {
            if (!this.mDisablePackages.contains(pkg)) {
                this.mDisablePackages.add(pkg);
            }
        }
    }

    static {
        HashSet<String> hashSet = new HashSet<>();
        mWebViewSets = hashSet;
        hashSet.add(Constant.APP_TOUTIAO);
        mWebViewSets.add("com.tmall.wireless");
        mWebViewSets.add("me.ele");
        mWebViewSets.add("com.dianping.v1");
        mWebViewSets.add("ctrip.android.view");
        mWebViewSets.add("com.taobao.trip");
        mWebViewSets.add("com.google.android.gms");
        mWebViewSets.add("com.Qunar");
        mWebViewSets.add(Constant.APP_WEIBO);
        mWebViewSets.add("com.netease.yanxuan");
        mWebViewSets.add("com.kaola");
        mWebViewSets.add("com.tencent.news");
        mWebViewSets.add("com.bbk.calendar");
        mWebViewSets.add("com.vivo.childrenmode");
        mWebViewSets.add("com.vivo.email");
        mWebViewSets.add("com.ss.android.article.video");
        PANEL_GREY_LIMIT = SystemProperties.getInt("persist.vivo.phone.panel_gray_limit", 0);
        nightModeGreyRectifyLevel = 99;
        nightModeRectifiedOffsetLightest = NightModeUtils.INVERT_MATRIX_OFFSET_LIGHTEST;
        isRectifyGreyValueFinished = false;
        DISABLED_VNIGHT_MODE_PACKAGE_LIST = new ArrayList<>();
        DONT_IN_SHOW_PACKAGE_LIST = new ArrayList<>();
        DISABLED_VNIGHT_MODE_PACKAGE_LIST.add(Constant.APP_WEIXIN);
        DISABLED_VNIGHT_MODE_PACKAGE_LIST.add(Constant.APP_DOUYIN);
        DISABLED_VNIGHT_MODE_PACKAGE_LIST.add("com.smile.gifmaker");
        DISABLED_VNIGHT_MODE_PACKAGE_LIST.add("com.jingdong.app.mall");
        DISABLED_VNIGHT_MODE_PACKAGE_LIST.add("com.benqu.wuta");
        DISABLED_VNIGHT_MODE_PACKAGE_LIST.add("com.achievo.vipshop");
        DISABLED_VNIGHT_MODE_PACKAGE_LIST.add("com.glodon.drawingexplorer");
    }

    public boolean needForWebView(String pkg) {
        return mWebViewSets.contains(pkg);
    }

    private VivoNightModeState getNightModeState(String pkg) {
        synchronized (this.mNightModeStates) {
            if (!this.mNightModeStates.containsKey(pkg)) {
                VivoNightModeState vns = new VivoNightModeState(pkg);
                this.mNightModeStates.put(pkg, vns);
                return vns;
            }
            return this.mNightModeStates.get(pkg);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void readStateForUser() {
        String str;
        boolean disabled;
        int flag;
        boolean forceAppEnabled;
        boolean dontInShowList;
        String str2 = "VivoNightModeService";
        try {
            if (this.mNightModeWhiteLists == null || this.mNightModeWhiteLists.isEmpty() || this.isFetchLatestConfigFailed) {
                VSlog.d("VivoNightModeService", "read state for user");
                XmlResourceParser xmlParser = this.mContext.getResources().getXml(isOverSeas() ? 51576835 : 51576834);
                while (true) {
                    int xmlEventType = xmlParser.next();
                    if (xmlEventType != 1) {
                        if (xmlEventType == 2 && "package".equals(xmlParser.getName())) {
                            String name = xmlParser.getAttributeValue(null, "name");
                            String disabledString = xmlParser.getAttributeValue(null, "disabled");
                            if (disabledString != null && !TextUtils.isEmpty(disabledString)) {
                                boolean disabled2 = Boolean.parseBoolean(disabledString);
                                disabled = disabled2;
                            } else {
                                disabled = false;
                            }
                            String flagString = xmlParser.getAttributeValue(null, "flag");
                            if (flagString != null && !TextUtils.isEmpty(flagString)) {
                                int flag2 = Integer.parseInt(flagString);
                                flag = flag2;
                            } else {
                                flag = 0;
                            }
                            String minDisabledVersionName = xmlParser.getAttributeValue(null, "minDisabledVersionName");
                            String maxDisabledVersionName = xmlParser.getAttributeValue(null, "maxDisabledVersionName");
                            String minDisabledVersionCode = xmlParser.getAttributeValue(null, "minDisabledVersionCode");
                            String maxDisabledVersionCode = xmlParser.getAttributeValue(null, "maxDisabledVersionCode");
                            String whiteActivity = xmlParser.getAttributeValue(null, "whiteActivity");
                            String forceAppEnabledString = xmlParser.getAttributeValue(null, "forceAppEnabled");
                            if (forceAppEnabledString != null && !TextUtils.isEmpty(forceAppEnabledString)) {
                                boolean forceAppEnabled2 = Boolean.parseBoolean(forceAppEnabledString);
                                forceAppEnabled = forceAppEnabled2;
                            } else {
                                forceAppEnabled = false;
                            }
                            String dontInShowListString = xmlParser.getAttributeValue(null, "dontInShowList");
                            if (dontInShowListString != null && !TextUtils.isEmpty(dontInShowListString)) {
                                boolean dontInShowList2 = Boolean.parseBoolean(dontInShowListString);
                                dontInShowList = dontInShowList2;
                            } else {
                                dontInShowList = false;
                            }
                            boolean dontInShowList3 = dontInShowList;
                            boolean dontInShowList4 = disabled;
                            boolean forceAppEnabled3 = forceAppEnabled;
                            XmlResourceParser xmlParser2 = xmlParser;
                            String str3 = str2;
                            int flag3 = flag;
                            boolean disabled3 = disabled;
                            try {
                                processXmlAttribute(name, dontInShowList4, flag, minDisabledVersionName, maxDisabledVersionName, minDisabledVersionCode, maxDisabledVersionCode, whiteActivity, forceAppEnabled3, dontInShowList3);
                                if (this.DEBUG) {
                                    str = str3;
                                    try {
                                        VSlog.d(str, "read state for user, name = " + name + ", disabled = " + disabled3 + ", flag = " + flag3 + ", minDisabledVersionName = " + minDisabledVersionName + ", maxDisabledVersionName = " + maxDisabledVersionName + ", minDisabledVersionCode = " + minDisabledVersionCode + ", maxDisabledVersionCode = " + maxDisabledVersionCode + ", whiteActivity = " + whiteActivity + ", forceAppEnabled = " + forceAppEnabled3 + ", dontInShowList = " + dontInShowList3);
                                    } catch (Exception e) {
                                        e = e;
                                        VSlog.e(str, "parser failed", e);
                                        return;
                                    }
                                } else {
                                    str = str3;
                                }
                                str2 = str;
                                xmlParser = xmlParser2;
                            } catch (Exception e2) {
                                e = e2;
                                str = str3;
                            }
                        }
                    } else {
                        return;
                    }
                }
            }
        } catch (Exception e3) {
            e = e3;
            str = str2;
        }
    }

    /* loaded from: classes.dex */
    private final class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
        }
    }

    public void enableNightMode(String pkg, boolean enable, boolean byUser) {
    }

    public String getResumedActivityName() {
        return this.mResumedActivityName;
    }

    public int getCurrentSystemUser() {
        try {
            return ActivityManager.getCurrentUser();
        } catch (Exception e) {
            VLog.e("VivoNightModeService", "VivoNightModeService::getCurrentSystemUser() failed, " + e);
            return 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class NightModeFilter {
        public String minDisabledVersionName = null;
        public String maxDisabledVersionName = null;
        public String minDisabledVersionCode = null;
        public String maxDisabledVersionCode = null;

        public NightModeFilter(String minDisabledVersionName, String maxDisabledVersionName, String minDisabledVersionCode, String maxDisabledVersionCode) {
            set(minDisabledVersionName, maxDisabledVersionName, minDisabledVersionCode, maxDisabledVersionCode);
        }

        public void set(String minDisabledVersionName, String maxDisabledVersionName, String minDisabledVersionCode, String maxDisabledVersionCode) {
            this.minDisabledVersionName = minDisabledVersionName;
            this.maxDisabledVersionName = maxDisabledVersionName;
            this.minDisabledVersionCode = minDisabledVersionCode;
            this.maxDisabledVersionCode = maxDisabledVersionCode;
        }
    }

    private void resetNightModeFilter() {
        synchronized (this.mNightModeFilter) {
            this.mNightModeFilter.clear();
        }
        synchronized (this.mNightModeWhiteActivity) {
            this.mNightModeWhiteActivity.clear();
        }
        synchronized (this.mNightModeForceAppEnabled) {
            this.mNightModeForceAppEnabled.clear();
        }
    }

    public boolean isInlineWhiteActivityList(ComponentName activity) {
        return this.mNightModeWhiteActivity.contains(activity);
    }

    public boolean isForceAppEnabled(String pkg) {
        return this.mNightModeForceAppEnabled.contains(pkg);
    }

    private void setNightModeFilter(String pkg, String minDisabledVersionName, String maxDisabledVersionName, String minDisabledVersionCode, String maxDisabledVersionCode, String whiteActivity, boolean forceAppEnabled) {
        if ((minDisabledVersionName != null && !TextUtils.isEmpty(minDisabledVersionName)) || ((maxDisabledVersionName != null && !TextUtils.isEmpty(maxDisabledVersionName)) || ((minDisabledVersionCode != null && !TextUtils.isEmpty(minDisabledVersionCode)) || (maxDisabledVersionCode != null && !TextUtils.isEmpty(maxDisabledVersionCode))))) {
            synchronized (this.mNightModeFilter) {
                if (!this.mNightModeFilter.containsKey(pkg)) {
                    NightModeFilter filter = new NightModeFilter(minDisabledVersionName, maxDisabledVersionName, minDisabledVersionCode, maxDisabledVersionCode);
                    this.mNightModeFilter.put(pkg, filter);
                } else {
                    NightModeFilter filter2 = this.mNightModeFilter.get(pkg);
                    filter2.set(minDisabledVersionName, maxDisabledVersionName, minDisabledVersionCode, maxDisabledVersionCode);
                }
            }
        }
        if (whiteActivity != null && !TextUtils.isEmpty(whiteActivity)) {
            synchronized (this.mNightModeWhiteActivity) {
                this.mNightModeWhiteActivity.add(new ComponentName(pkg, whiteActivity));
            }
        }
        if (forceAppEnabled) {
            synchronized (this.mNightModeForceAppEnabled) {
                this.mNightModeForceAppEnabled.add(pkg);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateNightModeFilter(String pkg) {
        if (pkg != null && !TextUtils.isEmpty(pkg) && this.mNightModeFilter.containsKey(pkg)) {
            if (inNightModeFilter(pkg)) {
                disable(pkg);
            } else {
                enable(pkg);
            }
        }
    }

    private boolean inNightModeFilter(String pkg) {
        PackageInfo info;
        try {
            if (this.mPm == null || !this.mNightModeFilter.containsKey(pkg) || (info = this.mPm.getPackageInfo(pkg, 0)) == null) {
                return false;
            }
            NightModeFilter filter = this.mNightModeFilter.get(pkg);
            String minDisabledVersionName = filter.minDisabledVersionName;
            String maxDisabledVersionName = filter.maxDisabledVersionName;
            String minDisabledVersionCode = filter.minDisabledVersionCode;
            String maxDisabledVersionCode = filter.maxDisabledVersionCode;
            String curVersionName = info.versionName;
            int curVersionCode = info.versionCode;
            boolean isMinDisabledVersionNameExists = false;
            boolean isVersionNameDisabled = false;
            boolean isMinDisabledVersionCodeExists = false;
            boolean isVersionCodeDisabled = false;
            if (minDisabledVersionName != null && !TextUtils.isEmpty(minDisabledVersionName)) {
                isMinDisabledVersionNameExists = true;
                isVersionNameDisabled = compareVersion(curVersionName, minDisabledVersionName) >= 0;
            }
            if (maxDisabledVersionName != null && !TextUtils.isEmpty(maxDisabledVersionName)) {
                int compare = compareVersion(curVersionName, maxDisabledVersionName);
                if (compare <= 0 && ((isMinDisabledVersionNameExists && isVersionNameDisabled) || !isMinDisabledVersionNameExists)) {
                    isVersionNameDisabled = true;
                } else {
                    isVersionNameDisabled = false;
                }
            }
            if (minDisabledVersionCode != null && !TextUtils.isEmpty(minDisabledVersionCode)) {
                isMinDisabledVersionCodeExists = true;
                isVersionCodeDisabled = curVersionCode >= Integer.parseInt(minDisabledVersionCode);
            }
            if (maxDisabledVersionCode != null && !TextUtils.isEmpty(maxDisabledVersionCode)) {
                if (curVersionCode <= Integer.parseInt(maxDisabledVersionCode) && ((isMinDisabledVersionCodeExists && isVersionCodeDisabled) || !isMinDisabledVersionCodeExists)) {
                    isVersionCodeDisabled = true;
                } else {
                    isVersionCodeDisabled = false;
                }
            }
            if (isVersionNameDisabled || isVersionCodeDisabled) {
                return true;
            }
            return false;
        } catch (Exception e) {
            VSlog.e("VivoNightModeService", "inNightModeFilter error " + e);
            return false;
        }
    }

    /* JADX WARN: Incorrect condition in loop: B:27:0x0056 */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private int compareVersion(java.lang.String r11, java.lang.String r12) {
        /*
            r10 = this;
            r0 = -1
            if (r11 == 0) goto L69
            if (r12 == 0) goto L69
            boolean r1 = android.text.TextUtils.isEmpty(r11)
            if (r1 != 0) goto L69
            boolean r1 = android.text.TextUtils.isEmpty(r12)
            if (r1 != 0) goto L69
            boolean r1 = r11.equals(r12)
            r2 = 0
            if (r1 == 0) goto L19
            return r2
        L19:
            java.lang.String r1 = "\\."
            java.lang.String[] r3 = r11.split(r1)
            java.lang.String[] r1 = r12.split(r1)
            int r4 = r3.length
            int r5 = r1.length
            int r4 = java.lang.Math.min(r4, r5)
            r5 = 0
            r6 = 0
        L2b:
            if (r5 >= r4) goto L40
            if (r6 != 0) goto L40
            r7 = r3[r5]
            int r7 = java.lang.Integer.parseInt(r7)
            r8 = r1[r5]
            int r8 = java.lang.Integer.parseInt(r8)
            int r6 = r7 - r8
            int r5 = r5 + 1
            goto L2b
        L40:
            r7 = 1
            if (r6 != 0) goto L65
            r8 = 0
            r8 = r5
        L45:
            int r9 = r3.length
            if (r8 >= r9) goto L54
            r9 = r3[r8]
            int r9 = java.lang.Integer.parseInt(r9)
            if (r9 <= 0) goto L51
            return r7
        L51:
            int r8 = r8 + 1
            goto L45
        L54:
            r7 = r5
        L55:
            int r8 = r1.length
            if (r7 >= r8) goto L64
            r8 = r1[r7]
            int r8 = java.lang.Integer.parseInt(r8)
            if (r8 <= 0) goto L61
            return r0
        L61:
            int r7 = r7 + 1
            goto L55
        L64:
            return r2
        L65:
            if (r6 <= 0) goto L68
            r0 = r7
        L68:
            return r0
        L69:
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.vivo.services.nightmode.VivoNightModeService.compareVersion(java.lang.String, java.lang.String):int");
    }

    private boolean isOverSeas() {
        String str = this.FEATURE_OVER_SEAS;
        if (str == null || TextUtils.isEmpty(str)) {
            this.FEATURE_OVER_SEAS = SystemProperties.get("ro.vivo.product.overseas", "no");
        }
        return "yes".equals(this.FEATURE_OVER_SEAS);
    }

    private void processXmlAttribute(String name, boolean disabled, int flag, String minDisabledVersionName, String maxDisabledVersionName, String minDisabledVersionCode, String maxDisabledVersionCode, String whiteActivity, boolean forceAppEnabled, boolean dontInShowList) {
        boolean disabled2;
        if (!TextUtils.isEmpty(name)) {
            setNightModeFilter(name, minDisabledVersionName, maxDisabledVersionName, minDisabledVersionCode, maxDisabledVersionCode, whiteActivity, forceAppEnabled);
            if (!inNightModeFilter(name)) {
                disabled2 = disabled;
            } else {
                disabled2 = true;
            }
            VivoNightModeState vns = new VivoNightModeState(name, disabled2, flag);
            synchronized (this.mNightModeStates) {
                if (getNightModeState(name).getFlag() != 1 || flag == 2) {
                    getNightModeState(name).copyFrom(vns);
                }
            }
            synchronized (this.mDisablePackages) {
                this.mDisablePackages.remove(name);
            }
            if (dontInShowList) {
                addNotInShowList(name);
            }
        }
    }

    private void initUnifiedConfigs() {
        try {
            VivoFrameworkFactory frameworkFactoryImpl = VivoFrameworkFactory.getFrameworkFactoryImpl();
            this.mVivoFrameworkFactory = frameworkFactoryImpl;
            if (frameworkFactoryImpl != null) {
                VSlog.d("VivoNightModeService", "init unified config");
                this.mConfigurationManager = this.mVivoFrameworkFactory.getConfigurationManager();
                initUnifiedLists();
                parseNightModeWhiteLists();
                parseNightModeWebViewWhiteLists();
            }
        } catch (Exception e) {
            VSlog.e("VivoNightModeService", "init unified configs error, " + e);
        }
    }

    private void initUnifiedLists() {
        if (this.mConfigurationManager != null) {
            VSlog.d("VivoNightModeService", "init unified list");
            ContentValuesList contentValuesList = this.mConfigurationManager.getContentValuesList(NIGHT_MODE_LIST_PATH, STR_NIGHT_MODE_WHITELIST);
            this.mNightModeWhiteLists = contentValuesList;
            this.mConfigurationManager.registerObserver(contentValuesList, this.mNightModeWhiteListsObserver);
            ContentValuesList contentValuesList2 = this.mConfigurationManager.getContentValuesList(NIGHT_MODE_LIST_PATH, STR_NIGHT_MODE_WEBVIEW_WHITELIST);
            this.mNightModeWebViewWhiteLists = contentValuesList2;
            this.mConfigurationManager.registerObserver(contentValuesList2, this.mNightModeWebViewWhiteListsObserver);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void parseNightModeWhiteLists() {
        Iterator<Map.Entry<String, ContentValues>> it;
        ContentValues cv;
        String name;
        boolean disabled;
        boolean dontInShowList;
        boolean disabled2 = false;
        int flag = 0;
        boolean forceAppEnabled = false;
        try {
            if (this.mNightModeWhiteLists != null) {
                VSlog.d("VivoNightModeService", "parse night mode white list");
                HashMap<String, ContentValues> mNightModeWhiteListMap = this.mNightModeWhiteLists.getValues();
                synchronized (this.mNightModeStates) {
                    this.mNightModeStates.clear();
                }
                resetNightModeFilter();
                resetFetchConfigFlag();
                resetNotInShowList();
                Iterator<Map.Entry<String, ContentValues>> it2 = mNightModeWhiteListMap.entrySet().iterator();
                while (it2.hasNext()) {
                    Map.Entry<String, ContentValues> entry = it2.next();
                    String name2 = entry.getKey();
                    ContentValues cv2 = entry.getValue();
                    if (cv2 == null) {
                        it = it2;
                        cv = cv2;
                        name = name2;
                    } else {
                        it = it2;
                        String tmpString = cv2.getAsString("disabled");
                        if (tmpString != null && !TextUtils.isEmpty(tmpString)) {
                            disabled = Boolean.parseBoolean(tmpString);
                        } else {
                            disabled = disabled2;
                        }
                        String tmpString2 = cv2.getAsString("flag");
                        if (tmpString2 != null && !TextUtils.isEmpty(tmpString2)) {
                            flag = Integer.parseInt(tmpString2);
                        }
                        String minDisabledVersionName = cv2.getAsString("minDisabledVersionName");
                        String maxDisabledVersionName = cv2.getAsString("maxDisabledVersionName");
                        String minDisabledVersionCode = cv2.getAsString("minDisabledVersionCode");
                        String maxDisabledVersionCode = cv2.getAsString("maxDisabledVersionCode");
                        String whiteActivity = cv2.getAsString("whiteActivity");
                        String tmpString3 = cv2.getAsString("forceAppEnabled");
                        if (tmpString3 != null && !TextUtils.isEmpty(tmpString3)) {
                            forceAppEnabled = Boolean.parseBoolean(tmpString3);
                        }
                        String tmpString4 = cv2.getAsString("dontInShowList");
                        if (tmpString4 != null && !TextUtils.isEmpty(tmpString4)) {
                            boolean dontInShowList2 = Boolean.parseBoolean(tmpString4);
                            this.isFetchLatestConfigFailed = false;
                            dontInShowList = dontInShowList2;
                        } else {
                            dontInShowList = false;
                        }
                        int flag2 = flag;
                        boolean forceAppEnabled2 = forceAppEnabled;
                        boolean dontInShowList3 = dontInShowList;
                        cv = cv2;
                        processXmlAttribute(name2, disabled, flag2, minDisabledVersionName, maxDisabledVersionName, minDisabledVersionCode, maxDisabledVersionCode, whiteActivity, forceAppEnabled2, dontInShowList3);
                        if (!this.DEBUG) {
                            name = name2;
                            flag = flag2;
                            forceAppEnabled = forceAppEnabled2;
                        } else {
                            StringBuilder sb = new StringBuilder();
                            sb.append("parse night mode lists, name = ");
                            name = name2;
                            sb.append(name);
                            sb.append(", disabled = ");
                            sb.append(disabled);
                            sb.append(", flag = ");
                            flag = flag2;
                            sb.append(flag);
                            sb.append(", minDisabledVersionName = ");
                            sb.append(minDisabledVersionName);
                            sb.append(", maxDisabledVersionName = ");
                            sb.append(maxDisabledVersionName);
                            sb.append(", minDisabledVersionCode = ");
                            sb.append(minDisabledVersionCode);
                            sb.append(", maxDisabledVersionCode = ");
                            sb.append(maxDisabledVersionCode);
                            sb.append(", whiteActivity = ");
                            sb.append(whiteActivity);
                            sb.append(", forceAppEnabled = ");
                            forceAppEnabled = forceAppEnabled2;
                            sb.append(forceAppEnabled);
                            sb.append(", dontInShowList = ");
                            sb.append(dontInShowList3);
                            VSlog.d("VivoNightModeService", sb.toString());
                        }
                        disabled2 = disabled;
                    }
                    it2 = it;
                }
            }
        } catch (Exception e) {
            VSlog.e("VivoNightModeService", "parse night mode lists error, " + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void parseNightModeWebViewWhiteLists() {
        try {
            if (this.mNightModeWebViewWhiteLists != null) {
                VSlog.d("VivoNightModeService", "parse night mode webview list");
                HashMap<String, ContentValues> mNightModeWebviewWhiteListMap = this.mNightModeWebViewWhiteLists.getValues();
                if (!mNightModeWebviewWhiteListMap.isEmpty()) {
                    mWebViewSets.clear();
                }
                for (Map.Entry<String, ContentValues> entry : mNightModeWebviewWhiteListMap.entrySet()) {
                    String name = entry.getKey();
                    mWebViewSets.add(name);
                    if (this.DEBUG) {
                        VSlog.d("VivoNightModeService", "parse night mode webview lists, name = " + name);
                    }
                }
            }
        } catch (Exception e) {
            VSlog.e("VivoNightModeService", "parse night mode webview lists error, " + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.vivo.services.nightmode.VivoNightModeService$4  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass4 extends ConfigurationObserver {
        AnonymousClass4() {
        }

        public void onConfigChange(String file, final String name) {
            VivoNightModeService.this.mHandler.post(new Runnable() { // from class: com.vivo.services.nightmode.-$$Lambda$VivoNightModeService$4$ogqrvB_0Wdf3w3Hc1aDcu8HaVTg
                @Override // java.lang.Runnable
                public final void run() {
                    VivoNightModeService.AnonymousClass4.this.lambda$onConfigChange$0$VivoNightModeService$4(name);
                }
            });
        }

        public /* synthetic */ void lambda$onConfigChange$0$VivoNightModeService$4(String name) {
            VSlog.d("VivoNightModeService", "white list changes, name = " + name);
            VivoNightModeService vivoNightModeService = VivoNightModeService.this;
            vivoNightModeService.mNightModeWhiteLists = vivoNightModeService.mConfigurationManager.getContentValuesList(VivoNightModeService.NIGHT_MODE_LIST_PATH, VivoNightModeService.STR_NIGHT_MODE_WHITELIST);
            VivoNightModeService.this.parseNightModeWhiteLists();
            VivoNightModeService.this.readStateForUser();
            VivoNightModeService.this.resetNightModeThridAppMap();
            VivoNightModeService.this.getNightModeThirdAppMap();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.vivo.services.nightmode.VivoNightModeService$5  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass5 extends ConfigurationObserver {
        AnonymousClass5() {
        }

        public void onConfigChange(String file, final String name) {
            VivoNightModeService.this.mHandler.post(new Runnable() { // from class: com.vivo.services.nightmode.-$$Lambda$VivoNightModeService$5$NW3__oZLDN9_vrTUIfzANEC6phs
                @Override // java.lang.Runnable
                public final void run() {
                    VivoNightModeService.AnonymousClass5.this.lambda$onConfigChange$0$VivoNightModeService$5(name);
                }
            });
        }

        public /* synthetic */ void lambda$onConfigChange$0$VivoNightModeService$5(String name) {
            VSlog.d("VivoNightModeService", "webview list changes, name = " + name);
            VivoNightModeService vivoNightModeService = VivoNightModeService.this;
            vivoNightModeService.mNightModeWebViewWhiteLists = vivoNightModeService.mConfigurationManager.getContentValuesList(VivoNightModeService.NIGHT_MODE_LIST_PATH, VivoNightModeService.STR_NIGHT_MODE_WEBVIEW_WHITELIST);
            VivoNightModeService.this.parseNightModeWebViewWhiteLists();
        }
    }

    private boolean shouldLimitGreyValue() {
        Context context;
        if (PANEL_GREY_LIMIT == 0 || (context = this.mContext) == null) {
            isRectifyGreyValueFinished = true;
            return false;
        }
        try {
            ContentResolver contentResolver = context.getContentResolver();
            if (contentResolver != null) {
                int userId = contentResolver.getUserId();
                if (userId == 999) {
                    userId = 0;
                }
                int nightModeLevel = Settings.System.getIntForUser(contentResolver, "vivo_nightmode_level", 0, userId);
                int isGreyValueRectified = Settings.System.getIntForUser(contentResolver, SETTINGS_KEY_GREY_VALUE_RECTIFIED, 0, userId);
                boolean isGreyValueFirstRectify = isGreyValueRectified == 0;
                int maxGreyRange = (int) (NightModeUtils.INVERT_MATRIX_OFFSET_LIGHTEST - 127.5f);
                int validGreyLimit = PANEL_GREY_LIMIT > maxGreyRange ? maxGreyRange : PANEL_GREY_LIMIT;
                int i = 100 - ((validGreyLimit * 100) / maxGreyRange);
                nightModeGreyRectifyLevel = i;
                if ((isGreyValueFirstRectify && nightModeLevel > i) || isGreyValueRectified == 1) {
                    nightModeRectifiedOffsetLightest = Math.min(validGreyLimit + 127.5f, NightModeUtils.INVERT_MATRIX_OFFSET_LIGHTEST);
                    if (isGreyValueFirstRectify) {
                        Settings.System.putIntForUser(contentResolver, SETTINGS_KEY_GREY_VALUE_RECTIFIED, 1, userId);
                    }
                    isRectifyGreyValueFinished = true;
                    VSlog.d("VivoNightModeService", "rectified offset is " + nightModeRectifiedOffsetLightest + ", limit = " + validGreyLimit);
                    return true;
                }
                if (isGreyValueFirstRectify) {
                    Settings.System.putIntForUser(contentResolver, SETTINGS_KEY_GREY_VALUE_RECTIFIED, 2, userId);
                }
                isRectifyGreyValueFinished = true;
                VSlog.d("VivoNightModeService", "don't rectify offset");
            }
        } catch (Exception e) {
            VLog.e("VivoNightModeService", "limit grey value error " + e);
        }
        return false;
    }

    public boolean isRectifyGreyValueFinished() {
        return isRectifyGreyValueFinished;
    }

    public float getLimitedGreyValue() {
        return nightModeRectifiedOffsetLightest;
    }

    private void initVersionInfo() {
        try {
            String osName = FtBuild.getOsName();
            String osVersion = FtBuild.getOsVersion();
            if (osName != null && osVersion != null) {
                boolean z = "Funtouch".equals(osName) && Float.valueOf(osVersion).floatValue() >= 12.0f;
                this.isCustomModeWaitForScreenOff = z;
                this.isVersionRom12Upper = z;
            }
        } catch (Exception e) {
            VLog.d("VivoNightModeService", "get version failed " + e);
        }
    }

    private void resetFetchConfigFlag() {
        this.isFetchLatestConfigFailed = this.isVersionRom12Upper;
    }

    private void initThirdAppInfo() {
        try {
            initSystemUid();
            readDisabledThirdApp();
            getDisabledThirdAppSetFromString();
            resetNightModeThridAppMap();
            getNightModeThirdAppMap();
        } catch (Exception e) {
            VLog.e("VivoNightModeService", "init third app info error " + e);
        }
    }

    private void initSystemUid() {
        this.mCurSystemUid = getCurrentSystemUser();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_SWITCHED");
        this.mContext.registerReceiver(new UserSwitchedReceiver(), filter);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class UserSwitchedReceiver extends BroadcastReceiver {
        private UserSwitchedReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            int currentId = intent.getIntExtra("android.intent.extra.user_handle", 0);
            VivoNightModeService.this.mCurSystemUid = currentId != 999 ? currentId : 0;
            VLog.d("VivoNightModeService", "user switched to " + VivoNightModeService.this.mCurSystemUid);
        }
    }

    public Map<String, Boolean> getNightModeThirdAppMap() {
        Map<String, Boolean> map;
        try {
            if (this.mNightModeStates != null && !this.mNightModeStates.isEmpty()) {
                synchronized (this.mThirdAppMap) {
                    if (this.mThirdAppMap != null && this.mThirdAppMap.isEmpty()) {
                        Set<String> keySet = this.mNightModeStates.keySet();
                        for (String key : keySet) {
                            VivoNightModeState valueState = this.mNightModeStates.get(key);
                            String pkg = valueState.getPkgName();
                            boolean isInDisabledThirdAppSet = isInDisabledThirdAppSet(pkg);
                            if ((!valueState.isDisabled() || isInDisabledThirdAppSet) && isFitThirdAppList(pkg)) {
                                if (isInDisabledThirdAppSet) {
                                    this.mThirdAppMap.put(pkg, false);
                                } else {
                                    this.mThirdAppMap.put(pkg, true);
                                }
                                if (this.DEBUG) {
                                    VSlog.d("VivoNightModeService", "get third app list, pkg = " + pkg);
                                }
                            }
                        }
                    }
                    map = this.mThirdAppMap;
                }
                return map;
            }
            return null;
        } catch (Exception e) {
            VLog.e("VivoNightModeService", "get third map error, " + e);
            return null;
        }
    }

    private boolean isFitThirdAppList(String pkg) {
        return (NightModeController.WHITE_PACKAGE_LIST.contains(pkg) || DISABLED_VNIGHT_MODE_PACKAGE_LIST.contains(pkg) || DONT_IN_SHOW_PACKAGE_LIST.contains(pkg)) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetNightModeThridAppMap() {
        synchronized (this.mThirdAppMap) {
            this.mThirdAppMap.clear();
        }
        if (this.DEBUG) {
            VSlog.d("VivoNightModeService", "reset night mode app map");
        }
    }

    private void resetNotInShowList() {
        synchronized (DONT_IN_SHOW_PACKAGE_LIST) {
            DONT_IN_SHOW_PACKAGE_LIST.clear();
        }
        if (this.DEBUG) {
            VSlog.d("VivoNightModeService", "reset not in show list");
        }
    }

    private void addNotInShowList(String pkg) {
        synchronized (DONT_IN_SHOW_PACKAGE_LIST) {
            DONT_IN_SHOW_PACKAGE_LIST.add(pkg);
        }
        if (this.DEBUG) {
            VSlog.d("VivoNightModeService", "add not in show list, pkg = " + pkg);
        }
    }

    private void readDisabledThirdApp() {
        this.mDisabledThirdAppString = Settings.System.getStringForUser(this.mContext.getContentResolver(), SETTINGS_DISABLED_THIRD_APP, this.mCurSystemUid);
        if (this.DEBUG) {
            VSlog.d("VivoNightModeService", "read disabled app, " + this.mDisabledThirdAppString);
        }
    }

    private void saveDisabledThirdApp() {
        Settings.System.putStringForUser(this.mContext.getContentResolver(), SETTINGS_DISABLED_THIRD_APP, this.mDisabledThirdAppString, this.mCurSystemUid);
        if (this.DEBUG) {
            VSlog.d("VivoNightModeService", "save disabled app, " + this.mDisabledThirdAppString + ", mCurSystemUid = " + this.mCurSystemUid);
        }
    }

    private void getDisabledThirdAppSetFromString() {
        this.mDisabledThirdAppSet.clear();
        String str = this.mDisabledThirdAppString;
        if (str != null) {
            String[] pkgSet = str.split(";");
            for (String pkg : pkgSet) {
                if (pkg != null && !TextUtils.isEmpty(pkg) && !this.mDisabledThirdAppSet.contains(pkg)) {
                    this.mDisabledThirdAppSet.add(pkg);
                    disable(pkg);
                }
            }
        }
        if (this.DEBUG) {
            VSlog.d("VivoNightModeService", "get disabled app set, " + this.mDisabledThirdAppSet.toString());
        }
    }

    private void setDisabledThirdAppSetToString() {
        StringBuilder strBuilder = new StringBuilder();
        ArraySet<String> arraySet = this.mDisabledThirdAppSet;
        if (arraySet != null) {
            Iterator<String> it = arraySet.iterator();
            while (it.hasNext()) {
                String pkg = it.next();
                strBuilder.append(pkg);
                strBuilder.append(";");
            }
        }
        this.mDisabledThirdAppString = strBuilder.toString();
        if (this.DEBUG) {
            VSlog.d("VivoNightModeService", "set disabled app str, " + this.mDisabledThirdAppString);
        }
    }

    private void updateThirdAppMap(String pkg, boolean state) {
        Map<String, Boolean> map = this.mThirdAppMap;
        if (map != null) {
            map.put(pkg, Boolean.valueOf(state));
            VSlog.d("VivoNightModeService", "updateThirdAppMap, pkg = " + pkg + ", state = " + state);
        }
    }

    private boolean isInDisabledThirdAppSet(String pkgName) {
        ArraySet<String> arraySet = this.mDisabledThirdAppSet;
        return arraySet != null && arraySet.contains(pkgName);
    }

    public boolean enableThirdAppNightMode(String pkgName) {
        ArraySet<String> arraySet;
        if (pkgName != null && !TextUtils.isEmpty(pkgName) && (arraySet = this.mDisabledThirdAppSet) != null && arraySet.contains(pkgName)) {
            this.mDisabledThirdAppSet.remove(pkgName);
            setDisabledThirdAppSetToString();
            saveDisabledThirdApp();
            updateThirdAppMap(pkgName, true);
            enable(pkgName);
            notifyNightModeAppStateChanged(pkgName, true);
            VSlog.d("VivoNightModeService", "enable app night mode " + pkgName);
            return true;
        }
        return false;
    }

    public boolean disableThirdAppNightMode(String pkgName) {
        ArraySet<String> arraySet;
        if (pkgName == null || TextUtils.isEmpty(pkgName) || (arraySet = this.mDisabledThirdAppSet) == null || arraySet.contains(pkgName)) {
            return false;
        }
        this.mDisabledThirdAppSet.add(pkgName);
        setDisabledThirdAppSetToString();
        saveDisabledThirdApp();
        updateThirdAppMap(pkgName, false);
        disable(pkgName);
        notifyNightModeAppStateChanged(pkgName, false);
        VSlog.d("VivoNightModeService", "disable app night mode " + pkgName);
        return true;
    }

    private boolean isCallbackRegistered(INightModeAppConfigChangeCallback callback) {
        synchronized (this) {
            int i = this.mConfigChangeListener.beginBroadcast();
            while (true) {
                int i2 = i - 1;
                if (i > 0) {
                    INightModeAppConfigChangeCallback listener = this.mConfigChangeListener.getBroadcastItem(i2);
                    if (callback.asBinder() == listener.asBinder()) {
                        this.mConfigChangeListener.finishBroadcast();
                        return true;
                    }
                    i = i2;
                } else {
                    this.mConfigChangeListener.finishBroadcast();
                    return false;
                }
            }
        }
    }

    public boolean registerNightModeAppConfigChangedListener(INightModeAppConfigChangeCallback callback) {
        boolean register;
        if (isCallbackRegistered(callback)) {
            return false;
        }
        synchronized (this) {
            register = this.mConfigChangeListener.register(callback);
        }
        return register;
    }

    public boolean unregisterNightModeAppConfigChangedListener(INightModeAppConfigChangeCallback callback) {
        boolean unregister;
        synchronized (this) {
            unregister = this.mConfigChangeListener.unregister(callback);
        }
        return unregister;
    }

    private void notifyNightModeAppStateChanged(String appName, boolean isNightModeOn) {
        synchronized (this) {
            int i = this.mConfigChangeListener.beginBroadcast();
            while (true) {
                int i2 = i - 1;
                if (i > 0) {
                    INightModeAppConfigChangeCallback callback = this.mConfigChangeListener.getBroadcastItem(i2);
                    if (callback != null) {
                        try {
                            callback.onNightModeAppStateChanged(appName, isNightModeOn);
                        } catch (Exception e) {
                            VSlog.e("VivoNightModeService", "notify webview evaluate changed error " + e);
                        }
                    }
                    i = i2;
                } else {
                    this.mConfigChangeListener.finishBroadcast();
                }
            }
        }
    }
}