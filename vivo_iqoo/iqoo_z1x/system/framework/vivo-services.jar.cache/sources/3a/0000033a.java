package com.android.server.notification;

import android.app.ActivityManagerInternal;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.FtFeature;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.server.LocalServices;
import com.android.server.UnifiedConfigThread;
import com.android.server.notification.ManagedServices;
import com.vivo.services.security.server.VivoPermissionUtils;
import com.vivo.services.superresolution.Constant;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import vivo.app.VivoFrameworkFactory;
import vivo.app.configuration.AbsConfigurationManager;
import vivo.app.configuration.ConfigurationObserver;
import vivo.app.configuration.ContentValuesList;
import vivo.util.AESUtils;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class NotificationWhiteListManager {
    private static final String ALLOW_MORE_NOTIFICATIONS_MANUAL_LIST = "/data/bbkcore/allow_more_notifications_manual_list.xml";
    private static final String ATTR_FORCE_DEFAULT_ALLOW = "forceDefaultAllow";
    private static final String ATTR_ITEM_ALLOW = "allow";
    private static final String ATTR_ITEM_ALLOW_MORE = "allowMore";
    private static final String ATTR_ITEM_FORCE_TAKE_EFFECT = "forceTakeEffect";
    private static final String ATTR_ITEM_JOIN_VIVO_PUSH = "joinVivoPush";
    private static final String ATTR_ITEM_NAME = "name";
    private static final String ATTR_JOIN_VPUSH_SDK = "joinVPushSDk";
    private static final String ATTR_MANUAL_ALLOW_MORE = "manualAllowMore";
    private static final String ATTR_MANUAL_SETTING = "manualSetting";
    private static final String ATTR_USER_HANDLE = "userHandle";
    private static final String DEFAULT_SYSTEM_OFF = "/system/etc/shield_list_system_off_bk.txt";
    private static final String DEFAULT_WHITE_LIST_FILE;
    private static final boolean IS_TARGET_VERSION;
    private static final String JOIN_VIVO_PUSH_LIST = "/data/bbkcore/join_vivo_push_list.xml";
    private static final String[] LOCAL_WHITE_LIST;
    private static final String LOCAL_WHITE_LIST_FILE = "/data/bbkcore/shield_list_server_bk_2.xml";
    private static final String MANUAL_SETTING_LIST_FILE = "/data/bbkcore/shield_list_manual_1.xml";
    private static final int MSG_STORE_MANUAL_SETTINGS = 100;
    private static final long OVERRIDE_MANUAL_LIST_DELAY = 3000;
    private static final String PUSH_VERSION = "4";
    private static final String PUSH_VERSION_KEY = "persist.vivo.push.version";
    private static final String SERVER_WHITE_LIST = "data/bbkcore/NotificationService_notification3_1.0.xml";
    private static final String TAG = "NotificationWhiteListManager";
    private static final String TAG_DEFAULT_ALLOW = "mDefaultAllow";
    private static final String TAG_DEFAULT_ALLOW_EX = "defaultAllowEx";
    private static final String TAG_ITEM = "item";
    private static final String TAG_ITEM_TEXT = "text";
    private static final String TAG_NOTIFICATION_MANUAL_POLICY = "notificationManual-policy";
    private static final String TAG_NOTIFICATION_OP_POLICY = "notificationOp-policy";
    private static final String TAG_NOTIFICATION_SYSTEM_POLICY = "notificationSystem-policy";
    private static final String TAG_PACKAGE_LIST = "packageList";
    private AbsConfigurationManager mAbsConfigurationManager;
    private boolean mContainsForceTakeEffect;
    private final Context mContext;
    private int mCurrentUserId;
    private boolean mDefaultAllow;
    private boolean mDefaultAllowForceTakeEffect;
    private boolean mHasFinishUserSetup;
    private final boolean mIsOverseasProduct;
    private final LoadXmlCallback mLoadDefaultWhiteListCallback;
    private final LoadXmlCallback mLoadManualCallback;
    private final LoadXmlCallback mLoadSystemDefaultOffCallback;
    private final StoreXmlCallback mManualStoreCallback;
    private boolean mNeedResetAll;
    private final Handler mNotificationIoHandler;
    private final NotificationManagerService mNotificationManagerService;
    private boolean mPolicyInited;
    private final PreferencesHelper mPreferencesHelper;
    private final ReentrantReadWriteLock.ReadLock mReadLock;
    private boolean mServerDefaultOffFirstInit;
    private boolean mServerFileFirstInit;
    private boolean mServerFileNeverInit;
    private final boolean mSystemFirstBoot;
    private final int mTypeJoinVPushSDK;
    private final int mTypeSwitchAllowMoreManual;
    private final int mTypeSwitchManual;
    private final ReentrantReadWriteLock.WriteLock mWriteLock;
    private final ArrayMap<String, NotificationServerRule> mServerWhiteList = new ArrayMap<>();
    private final ArrayMap<Integer, ArrayMap<String, NotificationLocalState>> mLocalManualState = new ArrayMap<>();
    private ArrayMap<String, NotificationLocalState> mLocalManualStatesByUser = new ArrayMap<>();
    private final ArrayMap<String, NotificationServerRule> mSystemDefaultOffList = new ArrayMap<>();

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public interface LoadXmlCallback {
        void onLoadError(Exception exc);

        void onLoadFinished();

        void onTagLoad(String str, ContentValues contentValues);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public interface StoreXmlCallback {
        void onStoreError(Exception exc);

        void onStoreFinish();
    }

    static {
        boolean isFeatureSupport = FtFeature.isFeatureSupport("vivo.software.new.notification.whitelist");
        IS_TARGET_VERSION = isFeatureSupport;
        DEFAULT_WHITE_LIST_FILE = isFeatureSupport ? "/system/etc/shield_list_server_bk_3.txt" : "/system/etc/shield_list_server_bk_2.xml";
        LOCAL_WHITE_LIST = new String[]{"com.tencent.mobileqq", Constant.APP_WEIXIN, VivoPermissionUtils.OS_PKG};
    }

    public NotificationWhiteListManager(Context context, NotificationManagerService notificationManagerService, PreferencesHelper preferencesHelper) {
        boolean equals = SystemProperties.get("ro.vivo.product.overseas", "no").equals("yes");
        this.mIsOverseasProduct = equals;
        this.mContainsForceTakeEffect = false;
        this.mNeedResetAll = true;
        this.mDefaultAllowForceTakeEffect = false;
        this.mDefaultAllow = equals;
        this.mPolicyInited = false;
        this.mServerFileFirstInit = false;
        this.mServerDefaultOffFirstInit = false;
        this.mCurrentUserId = UserHandle.myUserId();
        this.mServerFileNeverInit = false;
        this.mLoadManualCallback = new LoadXmlCallback() { // from class: com.android.server.notification.NotificationWhiteListManager.3
            @Override // com.android.server.notification.NotificationWhiteListManager.LoadXmlCallback
            public void onTagLoad(String tagName, ContentValues attributes) {
                if (!TextUtils.isEmpty(tagName) && NotificationWhiteListManager.TAG_ITEM.equals(tagName)) {
                    String pkg = attributes.getAsString(NotificationWhiteListManager.ATTR_ITEM_NAME);
                    boolean manualSetting = NotificationWhiteListManager.safeBoolean(attributes, NotificationWhiteListManager.ATTR_MANUAL_SETTING, false);
                    boolean manualAllowMore = NotificationWhiteListManager.safeBoolean(attributes, NotificationWhiteListManager.ATTR_MANUAL_ALLOW_MORE, false);
                    boolean joinVPushSDK = NotificationWhiteListManager.safeBoolean(attributes, NotificationWhiteListManager.ATTR_JOIN_VPUSH_SDK, false);
                    Integer userHandle = attributes.getAsInteger(NotificationWhiteListManager.ATTR_USER_HANDLE);
                    int realUserId = NotificationWhiteListManager.this.mCurrentUserId;
                    if (userHandle != null) {
                        realUserId = userHandle.intValue();
                    }
                    ArrayMap<String, NotificationLocalState> statesForUser = (ArrayMap) NotificationWhiteListManager.this.mLocalManualState.get(Integer.valueOf(realUserId));
                    if (statesForUser == null) {
                        statesForUser = new ArrayMap<>();
                        NotificationWhiteListManager.this.mLocalManualState.put(Integer.valueOf(realUserId), statesForUser);
                    }
                    if (!TextUtils.isEmpty(pkg)) {
                        NotificationLocalState state = new NotificationLocalState();
                        state.switchedManual = manualSetting;
                        state.mJoinVPushSDK = joinVPushSDK;
                        state.switchedAllowMoreManual = manualAllowMore;
                        statesForUser.put(pkg, state);
                    }
                }
            }

            @Override // com.android.server.notification.NotificationWhiteListManager.LoadXmlCallback
            public void onLoadFinished() {
                if (!NotificationWhiteListManager.this.mLocalManualState.isEmpty()) {
                    NotificationWhiteListManager.this.checkoutManualList();
                }
            }

            @Override // com.android.server.notification.NotificationWhiteListManager.LoadXmlCallback
            public void onLoadError(Exception ex) {
                VSlog.d(NotificationWhiteListManager.TAG, "loading manual state error", ex);
            }
        };
        this.mManualStoreCallback = new StoreXmlCallback() { // from class: com.android.server.notification.NotificationWhiteListManager.4
            @Override // com.android.server.notification.NotificationWhiteListManager.StoreXmlCallback
            public void onStoreFinish() {
            }

            @Override // com.android.server.notification.NotificationWhiteListManager.StoreXmlCallback
            public void onStoreError(Exception ex) {
                VSlog.e(NotificationWhiteListManager.TAG, "storeManualSetting failed try again later");
            }
        };
        this.mLoadDefaultWhiteListCallback = new LoadXmlCallback() { // from class: com.android.server.notification.NotificationWhiteListManager.5
            @Override // com.android.server.notification.NotificationWhiteListManager.LoadXmlCallback
            public void onTagLoad(String tagName, ContentValues attributes) {
                if (!tagName.equals(NotificationWhiteListManager.TAG_DEFAULT_ALLOW) || NotificationWhiteListManager.this.mIsOverseasProduct) {
                    if (tagName.equals(NotificationWhiteListManager.TAG_DEFAULT_ALLOW_EX) && NotificationWhiteListManager.this.mIsOverseasProduct) {
                        NotificationWhiteListManager.this.mDefaultAllowForceTakeEffect = NotificationWhiteListManager.safeBoolean(attributes, NotificationWhiteListManager.ATTR_FORCE_DEFAULT_ALLOW, false);
                        boolean newDefault = NotificationWhiteListManager.safeBoolean(attributes, NotificationWhiteListManager.TAG_ITEM_TEXT, NotificationWhiteListManager.this.mDefaultAllow);
                        if (newDefault != NotificationWhiteListManager.this.mDefaultAllow) {
                            NotificationWhiteListManager.this.mDefaultAllow = newDefault;
                            NotificationWhiteListManager.this.mNeedResetAll = true;
                            return;
                        }
                        return;
                    } else if (tagName.equals(NotificationWhiteListManager.TAG_ITEM)) {
                        String pkgName = attributes.getAsString(NotificationWhiteListManager.ATTR_ITEM_NAME);
                        if (!TextUtils.isEmpty(pkgName)) {
                            NotificationServerRule rule = new NotificationServerRule();
                            rule.mAllow = NotificationWhiteListManager.safeBoolean(attributes, NotificationWhiteListManager.ATTR_ITEM_ALLOW, false);
                            rule.mJoinVPush = NotificationWhiteListManager.safeBoolean(attributes, NotificationWhiteListManager.ATTR_ITEM_JOIN_VIVO_PUSH, false);
                            rule.mAllowMore = NotificationWhiteListManager.safeBoolean(attributes, NotificationWhiteListManager.ATTR_ITEM_ALLOW_MORE, false);
                            rule.mForceTakeEffect = NotificationWhiteListManager.safeBoolean(attributes, NotificationWhiteListManager.ATTR_ITEM_FORCE_TAKE_EFFECT, false);
                            NotificationWhiteListManager.this.mServerWhiteList.put(pkgName, rule);
                            return;
                        }
                        return;
                    } else {
                        return;
                    }
                }
                NotificationWhiteListManager.this.mDefaultAllowForceTakeEffect = NotificationWhiteListManager.safeBoolean(attributes, NotificationWhiteListManager.ATTR_FORCE_DEFAULT_ALLOW, false);
                boolean newDefault2 = NotificationWhiteListManager.safeBoolean(attributes, NotificationWhiteListManager.TAG_ITEM_TEXT, NotificationWhiteListManager.this.mDefaultAllow);
                if (newDefault2 != NotificationWhiteListManager.this.mDefaultAllow) {
                    NotificationWhiteListManager.this.mDefaultAllow = newDefault2;
                    NotificationWhiteListManager.this.mNeedResetAll = true;
                }
            }

            @Override // com.android.server.notification.NotificationWhiteListManager.LoadXmlCallback
            public void onLoadFinished() {
            }

            @Override // com.android.server.notification.NotificationWhiteListManager.LoadXmlCallback
            public void onLoadError(Exception ex) {
                VSlog.e(NotificationWhiteListManager.TAG, "load default data failed", ex);
            }
        };
        this.mLoadSystemDefaultOffCallback = new LoadXmlCallback() { // from class: com.android.server.notification.NotificationWhiteListManager.6
            @Override // com.android.server.notification.NotificationWhiteListManager.LoadXmlCallback
            public void onTagLoad(String tagName, ContentValues attributes) {
                if (tagName.equals(NotificationWhiteListManager.TAG_ITEM)) {
                    String pkgName = attributes.getAsString(NotificationWhiteListManager.ATTR_ITEM_NAME);
                    if (!TextUtils.isEmpty(pkgName)) {
                        NotificationServerRule rule = new NotificationServerRule();
                        rule.mAllow = NotificationWhiteListManager.safeBoolean(attributes, NotificationWhiteListManager.ATTR_ITEM_ALLOW, false);
                        rule.mForceTakeEffect = NotificationWhiteListManager.safeBoolean(attributes, NotificationWhiteListManager.ATTR_ITEM_FORCE_TAKE_EFFECT, false);
                        NotificationWhiteListManager.this.mSystemDefaultOffList.put(pkgName, rule);
                    }
                }
            }

            @Override // com.android.server.notification.NotificationWhiteListManager.LoadXmlCallback
            public void onLoadFinished() {
            }

            @Override // com.android.server.notification.NotificationWhiteListManager.LoadXmlCallback
            public void onLoadError(Exception ex) {
                VSlog.e(NotificationWhiteListManager.TAG, "load default system off failed", ex);
            }
        };
        this.mTypeSwitchManual = 0;
        this.mTypeSwitchAllowMoreManual = 1;
        this.mTypeJoinVPushSDK = 2;
        this.mContext = context;
        this.mNotificationManagerService = notificationManagerService;
        this.mPreferencesHelper = preferencesHelper;
        this.mNotificationIoHandler = new Handler(UnifiedConfigThread.getHandler().getLooper());
        ReentrantReadWriteLock mLock = new ReentrantReadWriteLock();
        this.mReadLock = mLock.readLock();
        this.mWriteLock = mLock.writeLock();
        this.mNotificationIoHandler.postDelayed(new Runnable() { // from class: com.android.server.notification.-$$Lambda$NotificationWhiteListManager$BUKNdwwWUnV2-94dIY6f55El_DE
            @Override // java.lang.Runnable
            public final void run() {
                NotificationWhiteListManager.this.lambda$new$0$NotificationWhiteListManager();
            }
        }, 5000L);
        boolean z = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 0, -2) != 0;
        this.mHasFinishUserSetup = z;
        this.mSystemFirstBoot = !z;
        if (VivoFrameworkFactory.getFrameworkFactoryImpl() != null) {
            this.mAbsConfigurationManager = VivoFrameworkFactory.getFrameworkFactoryImpl().getConfigurationManager();
        }
    }

    public /* synthetic */ void lambda$new$0$NotificationWhiteListManager() {
        if (NotificationManagerService.DBG) {
            VSlog.d(TAG, "Post initWhiteList!");
        }
        initWhiteList();
    }

    public void onStart() {
        SystemProperties.set(PUSH_VERSION_KEY, "4");
        this.mNotificationIoHandler.post(new Runnable() { // from class: com.android.server.notification.-$$Lambda$NotificationWhiteListManager$l_ECnAzhmZTxMin-xvSL6VW0E8A
            @Override // java.lang.Runnable
            public final void run() {
                NotificationWhiteListManager.this.lambda$onStart$1$NotificationWhiteListManager();
            }
        });
        if (IS_TARGET_VERSION) {
            Settings.System.putInt(this.mNotificationManagerService.getContext().getContentResolver(), "vivo_notification_permission_request", 1);
        }
    }

    public /* synthetic */ void lambda$onStart$1$NotificationWhiteListManager() {
        AbsConfigurationManager absConfigurationManager = this.mAbsConfigurationManager;
        if (absConfigurationManager != null) {
            absConfigurationManager.registerObserver(absConfigurationManager.getContentValuesList(SERVER_WHITE_LIST, TAG_NOTIFICATION_OP_POLICY), new ConfigurationObserver() { // from class: com.android.server.notification.NotificationWhiteListManager.1
                public void onConfigChange(String file, String name) {
                    if (NotificationManagerService.DBG) {
                        VSlog.d(NotificationWhiteListManager.TAG, " onConfigChange");
                    }
                    NotificationWhiteListManager.this.loadUnifiedConfigAsync();
                }
            });
            if (IS_TARGET_VERSION) {
                AbsConfigurationManager absConfigurationManager2 = this.mAbsConfigurationManager;
                absConfigurationManager2.registerObserver(absConfigurationManager2.getContentValuesList(SERVER_WHITE_LIST, TAG_NOTIFICATION_SYSTEM_POLICY), new ConfigurationObserver() { // from class: com.android.server.notification.NotificationWhiteListManager.2
                    public void onConfigChange(String file, String name) {
                        if (NotificationManagerService.DBG) {
                            VSlog.d(NotificationWhiteListManager.TAG, " onConfigChange system off");
                        }
                        NotificationWhiteListManager.this.loadUnifiedConfigAsync();
                    }
                });
            }
        }
    }

    public void saveSwitchedManualItem(String pkg, int uid) {
        int userHandle = UserHandle.getUserId(uid);
        this.mWriteLock.lock();
        ArrayMap<String, NotificationLocalState> statesForUser = this.mLocalManualState.get(Integer.valueOf(userHandle));
        if (statesForUser == null) {
            statesForUser = new ArrayMap<>();
            this.mLocalManualState.put(Integer.valueOf(userHandle), statesForUser);
        }
        NotificationLocalState state = statesForUser.get(pkg);
        if (this.mPolicyInited && (state == null || !state.switchedManual)) {
            if (state == null) {
                state = new NotificationLocalState();
            }
            state.switchedManual = true;
            statesForUser.put(pkg, state);
            storeLocalManualState();
        }
        this.mWriteLock.unlock();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void saveAllowMoreManualItem(String pkg, int uid) {
        int userHandle = UserHandle.getUserId(uid);
        this.mWriteLock.lock();
        ArrayMap<String, NotificationLocalState> statesForUser = this.mLocalManualState.get(Integer.valueOf(userHandle));
        if (statesForUser == null) {
            statesForUser = new ArrayMap<>();
            this.mLocalManualState.put(Integer.valueOf(userHandle), statesForUser);
        }
        NotificationLocalState state = statesForUser.get(pkg);
        if (this.mPolicyInited && (state == null || !state.switchedAllowMoreManual)) {
            if (state == null) {
                state = new NotificationLocalState();
            }
            state.switchedAllowMoreManual = true;
            statesForUser.put(pkg, state);
            storeLocalManualState();
        }
        this.mWriteLock.unlock();
    }

    private void storeLocalManualState() {
        this.mNotificationIoHandler.removeMessages(100);
        Message msg = this.mNotificationIoHandler.obtainMessage(100);
        msg.setCallback(new Runnable() { // from class: com.android.server.notification.-$$Lambda$NotificationWhiteListManager$siyid21Hp9SY8kx0SuFjhRww4Wk
            @Override // java.lang.Runnable
            public final void run() {
                NotificationWhiteListManager.this.lambda$storeLocalManualState$2$NotificationWhiteListManager();
            }
        });
        this.mNotificationIoHandler.sendMessageDelayed(msg, OVERRIDE_MANUAL_LIST_DELAY);
    }

    public /* synthetic */ void lambda$storeLocalManualState$2$NotificationWhiteListManager() {
        this.mReadLock.lock();
        if (NotificationManagerService.DBG) {
            VSlog.d(TAG, "store manual state");
        }
        List<ContentValues> arrayList = new ArrayList<>(this.mLocalManualState.size());
        for (Map.Entry<Integer, ArrayMap<String, NotificationLocalState>> statesForUsers : this.mLocalManualState.entrySet()) {
            int userHandle = statesForUsers.getKey().intValue();
            ArrayMap<String, NotificationLocalState> stateForUser = statesForUsers.getValue();
            if (stateForUser != null) {
                for (Map.Entry<String, NotificationLocalState> stateEntry : statesForUsers.getValue().entrySet()) {
                    ContentValues stateValue = new ContentValues();
                    NotificationLocalState state = stateEntry.getValue();
                    if (!TextUtils.isEmpty(stateEntry.getKey()) && state != null) {
                        stateValue.put(ATTR_ITEM_NAME, stateEntry.getKey());
                        stateValue.put(ATTR_JOIN_VPUSH_SDK, Boolean.valueOf(state.mJoinVPushSDK));
                        stateValue.put(ATTR_MANUAL_SETTING, Boolean.valueOf(state.switchedManual));
                        stateValue.put(ATTR_MANUAL_ALLOW_MORE, Boolean.valueOf(state.switchedAllowMoreManual));
                        stateValue.put(ATTR_USER_HANDLE, Integer.valueOf(userHandle));
                        arrayList.add(stateValue);
                    }
                }
            }
        }
        Map<String, List<ContentValues>> attrItems = new HashMap<>(1);
        attrItems.put(TAG_ITEM, arrayList);
        XmlLSHelper.storeXml(MANUAL_SETTING_LIST_FILE, TAG_NOTIFICATION_MANUAL_POLICY, attrItems, this.mManualStoreCallback);
        this.mReadLock.unlock();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkoutManualList() {
        this.mWriteLock.lock();
        PackageManager pm = this.mContext.getPackageManager();
        for (Map.Entry<Integer, ArrayMap<String, NotificationLocalState>> statesForUsers : this.mLocalManualState.entrySet()) {
            int userHandle = statesForUsers.getKey().intValue();
            ArrayMap<String, NotificationLocalState> stateForUser = statesForUsers.getValue();
            if (stateForUser != null) {
                int size = stateForUser.size();
                for (int index = size - 1; index >= 0; index--) {
                    String pkg = stateForUser.keyAt(index);
                    try {
                        pm.getApplicationInfoAsUser(pkg, 256, userHandle);
                    } catch (PackageManager.NameNotFoundException e) {
                        if (NotificationManagerService.DBG) {
                            VSlog.d(TAG, "checkout " + pkg + " NameNotFoundException");
                        }
                        stateForUser.remove(pkg);
                    }
                }
            }
        }
        this.mWriteLock.unlock();
    }

    void initWhiteList() {
        ActivityManagerInternal activityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        if (activityManagerInternal != null) {
            this.mCurrentUserId = activityManagerInternal.getCurrentUserId();
        }
        this.mWriteLock.lock();
        this.mLocalManualState.put(Integer.valueOf(this.mCurrentUserId), this.mLocalManualStatesByUser);
        if (NotificationManagerService.DBG) {
            VSlog.d(TAG, "initWhiteList");
        }
        XmlLSHelper.loadXml(MANUAL_SETTING_LIST_FILE, this.mLoadManualCallback);
        if (this.mLocalManualStatesByUser.isEmpty() && this.mLocalManualState.size() == 1) {
            initLocalManualFromOldFile();
        }
        loadUnifiedConfig(false);
        if (this.mServerWhiteList.isEmpty()) {
            if (NotificationManagerService.DBG) {
                VSlog.d(TAG, "initWhiteList , server white list empty");
            }
            if (!IS_TARGET_VERSION) {
                XmlLSHelper.loadXml(LOCAL_WHITE_LIST_FILE, this.mLoadDefaultWhiteListCallback);
            }
            if (this.mServerWhiteList.isEmpty()) {
                this.mServerFileNeverInit = true;
                XmlLSHelper.loadXml(DEFAULT_WHITE_LIST_FILE, this.mLoadDefaultWhiteListCallback);
            }
        }
        if (IS_TARGET_VERSION && this.mSystemDefaultOffList.isEmpty()) {
            XmlLSHelper.loadXml(DEFAULT_SYSTEM_OFF, this.mLoadSystemDefaultOffCallback);
        }
        if (!this.mServerWhiteList.isEmpty()) {
            if (NotificationManagerService.DBG) {
                VSlog.d(TAG, "initWhiteList , backup exists.");
            }
            applyLatestServerWhiteList();
        }
        this.mPolicyInited = true;
        this.mWriteLock.unlock();
    }

    private boolean isSystemApp(PackageInfo pkgInfo) {
        if ((IS_TARGET_VERSION && pkgInfo.applicationInfo != null && this.mSystemDefaultOffList.containsKey(pkgInfo.applicationInfo.packageName)) || pkgInfo == null || pkgInfo.applicationInfo == null) {
            return false;
        }
        if ((pkgInfo.applicationInfo.flags & 1) == 0 && (pkgInfo.applicationInfo.flags & 128) == 0) {
            return false;
        }
        return true;
    }

    private boolean isLocalWhiteList(String packageName) {
        String[] strArr;
        for (String pkg : LOCAL_WHITE_LIST) {
            if (pkg.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCtsPackage(String packageName) {
        return !TextUtils.isEmpty(packageName) && (packageName.contains(".cts") || packageName.contains(".gts") || packageName.equals("com.android.test.notificationdelegator"));
    }

    private void applyDefaultLocked() {
        if (this.mNeedResetAll) {
            if (!this.mSystemFirstBoot && !this.mServerFileFirstInit && !this.mDefaultAllowForceTakeEffect) {
                VSlog.d(TAG, "do not apply default");
                return;
            }
            PackageManager pm = this.mContext.getPackageManager();
            List<ApplicationInfo> installedList = pm.getInstalledApplicationsAsUser(0, this.mCurrentUserId);
            if (installedList == null || installedList.size() == 0) {
                if (NotificationManagerService.DBG) {
                    VSlog.d(TAG, "applyDefault cant GET list!");
                    return;
                }
                return;
            }
            this.mNeedResetAll = false;
            for (ApplicationInfo appInfo : installedList) {
                if (appInfo != null && (this.mLocalManualStatesByUser.get(appInfo.packageName) == null || !this.mLocalManualStatesByUser.get(appInfo.packageName).switchedManual)) {
                    if (this.mLocalManualStatesByUser.get(appInfo.packageName) == null || !this.mLocalManualStatesByUser.get(appInfo.packageName).mJoinVPushSDK) {
                        if (!isLocalWhiteList(appInfo.packageName) && !isCtsPackage(appInfo.packageName) && (IS_TARGET_VERSION || ((appInfo.flags & 1) == 0 && (appInfo.flags & 128) == 0))) {
                            if (!IS_TARGET_VERSION || (((appInfo.flags & 1) == 0 && (appInfo.flags & 128) == 0) || (this.mSystemDefaultOffList.get(appInfo.packageName) != null && !this.mSystemDefaultOffList.get(appInfo.packageName).mAllow))) {
                                setNotificationsEnabledForPackageVivo(appInfo.packageName, appInfo.uid, this.mDefaultAllow);
                            }
                        }
                    }
                }
            }
        }
    }

    private void loadUnifiedConfig(boolean update) {
        this.mWriteLock.lock();
        ContentValuesList rulesFromServer = loadDataByConfigurationManager(SERVER_WHITE_LIST, TAG_NOTIFICATION_OP_POLICY);
        if (rulesFromServer != null && !rulesFromServer.isEmpty()) {
            this.mServerWhiteList.clear();
            this.mContainsForceTakeEffect = false;
            for (Map.Entry<String, ContentValues> item : rulesFromServer.getValues().entrySet()) {
                String pkg = item.getKey();
                ContentValues contents = item.getValue();
                if (!TextUtils.isEmpty(item.getKey()) && contents != null) {
                    if (TAG_DEFAULT_ALLOW.equals(pkg) && !this.mIsOverseasProduct) {
                        this.mDefaultAllow = safeBoolean(contents, TAG_DEFAULT_ALLOW, this.mDefaultAllow);
                        this.mDefaultAllowForceTakeEffect = safeBoolean(contents, ATTR_FORCE_DEFAULT_ALLOW, this.mDefaultAllowForceTakeEffect);
                    } else if (TAG_DEFAULT_ALLOW_EX.equals(pkg) && this.mIsOverseasProduct) {
                        this.mDefaultAllow = safeBoolean(contents, TAG_DEFAULT_ALLOW_EX, this.mDefaultAllow);
                        this.mDefaultAllowForceTakeEffect = safeBoolean(contents, ATTR_FORCE_DEFAULT_ALLOW, this.mDefaultAllowForceTakeEffect);
                    } else {
                        NotificationServerRule newRule = new NotificationServerRule();
                        newRule.mAllow = safeBoolean(contents, pkg, this.mDefaultAllow);
                        newRule.mAllowMore = safeBoolean(contents, ATTR_ITEM_ALLOW_MORE, this.mDefaultAllow);
                        newRule.mJoinVPush = safeBoolean(contents, ATTR_ITEM_JOIN_VIVO_PUSH, false);
                        newRule.mForceTakeEffect = safeBoolean(contents, ATTR_ITEM_FORCE_TAKE_EFFECT, false);
                        this.mServerWhiteList.put(pkg, newRule);
                        if (newRule.mForceTakeEffect) {
                            this.mContainsForceTakeEffect = true;
                        }
                    }
                }
            }
        }
        if (IS_TARGET_VERSION) {
            loadSystemOffPkg(SERVER_WHITE_LIST);
        }
        this.mWriteLock.unlock();
        if (update) {
            this.mServerFileFirstInit = this.mServerFileNeverInit;
            applyLatestServerWhiteList();
            this.mServerFileFirstInit = false;
            this.mServerFileNeverInit = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void loadUnifiedConfigAsync() {
        this.mNotificationIoHandler.post(new Runnable() { // from class: com.android.server.notification.-$$Lambda$NotificationWhiteListManager$IsG-Ub-EtC2mZtWAWH9HC8iYV38
            @Override // java.lang.Runnable
            public final void run() {
                NotificationWhiteListManager.this.lambda$loadUnifiedConfigAsync$3$NotificationWhiteListManager();
            }
        });
    }

    public /* synthetic */ void lambda$loadUnifiedConfigAsync$3$NotificationWhiteListManager() {
        loadUnifiedConfig(true);
    }

    public void onUserSwitched(int userId) {
        if (this.mPolicyInited) {
            this.mWriteLock.lock();
            this.mCurrentUserId = userId;
            if (this.mLocalManualState.get(Integer.valueOf(userId)) == null) {
                this.mLocalManualStatesByUser = new ArrayMap<>();
                this.mLocalManualState.put(Integer.valueOf(this.mCurrentUserId), this.mLocalManualStatesByUser);
                this.mNeedResetAll = true;
                applyLatestServerWhiteList();
            } else {
                this.mLocalManualStatesByUser = this.mLocalManualState.get(Integer.valueOf(userId));
            }
            this.mWriteLock.unlock();
        }
    }

    public void onUserChanged(int userId, boolean added) {
        if (this.mPolicyInited && !added) {
            this.mWriteLock.lock();
            ArrayMap<String, NotificationLocalState> trulyMoved = this.mLocalManualState.remove(Integer.valueOf(userId));
            if (trulyMoved != null) {
                storeLocalManualState();
            }
            this.mWriteLock.unlock();
        }
    }

    private void loadSystemOffPkg(String fileName) {
        this.mWriteLock.lock();
        ContentValuesList defaultOffRules = loadDataByConfigurationManager(fileName, TAG_NOTIFICATION_SYSTEM_POLICY);
        if (defaultOffRules != null && !defaultOffRules.isEmpty()) {
            this.mSystemDefaultOffList.clear();
            for (Map.Entry<String, ContentValues> item : defaultOffRules.getValues().entrySet()) {
                String pkg = item.getKey();
                ContentValues contents = item.getValue();
                if (!TextUtils.isEmpty(pkg) && contents != null && !contents.isEmpty()) {
                    NotificationServerRule rule = new NotificationServerRule();
                    rule.mAllow = safeBoolean(contents, ATTR_ITEM_ALLOW, this.mDefaultAllow);
                    rule.mForceTakeEffect = safeBoolean(contents, ATTR_ITEM_FORCE_TAKE_EFFECT, false);
                    if (rule.mForceTakeEffect) {
                        boolean userSwitched = this.mLocalManualStatesByUser.get(pkg) != null && this.mLocalManualStatesByUser.get(pkg).switchedManual;
                        if (!userSwitched) {
                            PackageManager pm = this.mContext.getPackageManager();
                            try {
                                ApplicationInfo info = pm.getApplicationInfoAsUser(pkg, 0, this.mCurrentUserId);
                                if (info != null) {
                                    setNotificationsEnabledForPackageVivo(pkg, info.uid, rule.mAllow);
                                }
                            } catch (PackageManager.NameNotFoundException e) {
                            }
                        }
                    }
                    this.mSystemDefaultOffList.put(pkg, rule);
                }
            }
        }
        this.mWriteLock.unlock();
        if (this.mServerFileNeverInit) {
            this.mNeedResetAll = true;
        }
    }

    private ContentValuesList loadDataByConfigurationManager(String fileName, String tag) {
        AbsConfigurationManager absConfigurationManager = this.mAbsConfigurationManager;
        if (absConfigurationManager != null) {
            return absConfigurationManager.getContentValuesList(fileName, tag);
        }
        return null;
    }

    private void applyLatestServerWhiteList() {
        this.mReadLock.lock();
        applyDefaultLocked();
        if (!this.mServerWhiteList.isEmpty()) {
            PackageManager pm = this.mContext.getPackageManager();
            if ((this.mSystemFirstBoot && !this.mHasFinishUserSetup) || this.mServerFileFirstInit || this.mContainsForceTakeEffect) {
                for (Map.Entry<String, NotificationServerRule> entry : this.mServerWhiteList.entrySet()) {
                    String pkg = entry.getKey();
                    NotificationServerRule rule = entry.getValue();
                    NotificationLocalState state = this.mLocalManualStatesByUser.get(pkg);
                    if (!TextUtils.isEmpty(pkg) && rule != null && (!containsForceButNotInitial() || rule.mForceTakeEffect)) {
                        boolean applyAllow = true;
                        boolean applyAllowMore = true;
                        if (state != null && (state.mJoinVPushSDK || state.switchedManual)) {
                            applyAllow = false;
                        }
                        if (state != null && state.switchedAllowMoreManual) {
                            applyAllowMore = false;
                        }
                        try {
                            PackageInfo info = pm.getPackageInfoAsUser(pkg, 0, this.mCurrentUserId);
                            ApplicationInfo ai = null;
                            if (info != null) {
                                ai = info.applicationInfo;
                            }
                            if (ai != null) {
                                if (applyAllow) {
                                    setNotificationsEnabledForPackageVivo(pkg, ai.uid, rule.mAllow);
                                }
                                if (applyAllowMore) {
                                    boolean effectChannel = rule.mJoinVPush;
                                    boolean allowMore = rule.mAllowMore;
                                    this.mPreferencesHelper.setMoreNotificationEnabled(pkg, ai.uid, allowMore, effectChannel);
                                }
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                        }
                    }
                }
            }
        }
        this.mReadLock.unlock();
    }

    private boolean containsForceButNotInitial() {
        boolean firstBoot = this.mSystemFirstBoot && !this.mHasFinishUserSetup;
        return (firstBoot || this.mServerFileFirstInit || !this.mContainsForceTakeEffect) ? false : true;
    }

    private void setNotificationsEnabledForPackageVivo(String pkg, int uid, boolean enabled) {
        this.mNotificationManagerService.checkCallerIsSystem();
        if (NotificationManagerService.DBG) {
            StringBuilder sb = new StringBuilder();
            sb.append(enabled ? "en" : "dis");
            sb.append("abling notifications for ");
            sb.append(pkg);
            VSlog.v(TAG, sb.toString());
        }
        this.mPreferencesHelper.setEnabled(pkg, uid, enabled);
        if (!enabled) {
            this.mNotificationManagerService.cancelAllNotificationsInt(Process.myUid(), Process.myPid(), pkg, (String) null, 0, 0, true, UserHandle.getUserId(uid), 7, (ManagedServices.ManagedServiceInfo) null);
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:13:0x003a, code lost:
        if (isCtsPackage(r5.packageName) != false) goto L38;
     */
    /* JADX WARN: Removed duplicated region for block: B:24:0x0065  */
    /* JADX WARN: Removed duplicated region for block: B:37:0x00b9  */
    /* JADX WARN: Removed duplicated region for block: B:38:0x00bb  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public void disableNotificationIfNeed(java.lang.String r10, int r11) {
        /*
            r9 = this;
            java.lang.String r0 = "NotificationWhiteListManager"
            java.util.concurrent.locks.ReentrantReadWriteLock$ReadLock r1 = r9.mReadLock
            r1.lock()
            android.util.ArrayMap<java.lang.String, com.android.server.notification.NotificationWhiteListManager$NotificationLocalState> r1 = r9.mLocalManualStatesByUser
            java.lang.Object r1 = r1.get(r10)
            com.android.server.notification.NotificationWhiteListManager$NotificationLocalState r1 = (com.android.server.notification.NotificationWhiteListManager.NotificationLocalState) r1
            if (r1 == 0) goto L15
            boolean r2 = r1.switchedManual
            if (r2 != 0) goto Lc0
        L15:
            r2 = 0
            r3 = 0
            android.content.Context r4 = r9.mContext
            android.content.pm.PackageManager r4 = r4.getPackageManager()
            r5 = 0
            r6 = 0
            android.content.pm.PackageInfo r6 = r4.getPackageInfoAsUser(r10, r6, r11)     // Catch: android.content.pm.PackageManager.NameNotFoundException -> L3f
            r5 = r6
            boolean r6 = r9.isSystemApp(r5)     // Catch: android.content.pm.PackageManager.NameNotFoundException -> L3f
            if (r6 != 0) goto L3c
            if (r5 == 0) goto L3e
            java.lang.String r6 = r5.packageName     // Catch: android.content.pm.PackageManager.NameNotFoundException -> L3f
            boolean r6 = r9.isLocalWhiteList(r6)     // Catch: android.content.pm.PackageManager.NameNotFoundException -> L3f
            if (r6 != 0) goto L3c
            java.lang.String r6 = r5.packageName     // Catch: android.content.pm.PackageManager.NameNotFoundException -> L3f
            boolean r6 = r9.isCtsPackage(r6)     // Catch: android.content.pm.PackageManager.NameNotFoundException -> L3f
            if (r6 == 0) goto L3e
        L3c:
            r2 = 1
            r3 = 1
        L3e:
            goto L5d
        L3f:
            r6 = move-exception
            boolean r7 = com.android.server.notification.NotificationManagerService.DBG
            if (r7 == 0) goto L5d
            java.lang.StringBuilder r7 = new java.lang.StringBuilder
            r7.<init>()
            java.lang.String r8 = "checkWhetherNeedDisable "
            r7.append(r8)
            r7.append(r10)
            java.lang.String r8 = " NameNotFound"
            r7.append(r8)
            java.lang.String r7 = r7.toString()
            vivo.util.VSlog.e(r0, r7)
        L5d:
            if (r5 == 0) goto Lc0
            android.content.pm.ApplicationInfo r6 = r5.applicationInfo
            if (r6 == 0) goto Lc0
            if (r2 != 0) goto Lb3
            android.util.ArrayMap<java.lang.String, com.android.server.notification.NotificationWhiteListManager$NotificationServerRule> r6 = r9.mServerWhiteList
            java.lang.Object r6 = r6.get(r10)
            com.android.server.notification.NotificationWhiteListManager$NotificationServerRule r6 = (com.android.server.notification.NotificationWhiteListManager.NotificationServerRule) r6
            java.lang.String r7 = "checkWhetherNeedDisable pkg="
            if (r6 == 0) goto L90
            boolean r2 = r6.mAllow
            r3 = 1
            boolean r8 = com.android.server.notification.NotificationManagerService.DBG
            if (r8 == 0) goto Lb3
            java.lang.StringBuilder r8 = new java.lang.StringBuilder
            r8.<init>()
            r8.append(r7)
            r8.append(r10)
            java.lang.String r7 = "  contains in mLocalWhiteList"
            r8.append(r7)
            java.lang.String r7 = r8.toString()
            vivo.util.VSlog.d(r0, r7)
            goto Lb3
        L90:
            if (r1 == 0) goto Lb3
            boolean r8 = r1.mJoinVPushSDK
            if (r8 == 0) goto Lb3
            r2 = 1
            r3 = 1
            boolean r8 = com.android.server.notification.NotificationManagerService.DBG
            if (r8 == 0) goto Lb3
            java.lang.StringBuilder r8 = new java.lang.StringBuilder
            r8.<init>()
            r8.append(r7)
            r8.append(r10)
            java.lang.String r7 = "  contains in joinVPushFromSdk"
            r8.append(r7)
            java.lang.String r7 = r8.toString()
            vivo.util.VSlog.d(r0, r7)
        Lb3:
            android.content.pm.ApplicationInfo r0 = r5.applicationInfo
            int r0 = r0.uid
            if (r3 == 0) goto Lbb
            r6 = r2
            goto Lbd
        Lbb:
            boolean r6 = r9.mDefaultAllow
        Lbd:
            r9.setNotificationsEnabledForPackageVivo(r10, r0, r6)
        Lc0:
            java.util.concurrent.locks.ReentrantReadWriteLock$ReadLock r0 = r9.mReadLock
            r0.unlock()
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.notification.NotificationWhiteListManager.disableNotificationIfNeed(java.lang.String, int):void");
    }

    private void disableAllowMoreIfNeed(String pkg, int changeUserId) {
        this.mReadLock.lock();
        NotificationLocalState state = this.mLocalManualStatesByUser.get(pkg);
        if (state == null || !state.switchedAllowMoreManual) {
            PackageManager pm = this.mContext.getPackageManager();
            PackageInfo info = null;
            try {
                info = pm.getPackageInfoAsUser(pkg, 0, changeUserId);
            } catch (PackageManager.NameNotFoundException e) {
            }
            NotificationServerRule serverRule = this.mServerWhiteList.get(pkg);
            if (info != null && serverRule != null && serverRule.mJoinVPush) {
                VSlog.d(TAG, "checkWhetherNeedDisableAllowMore setMoreNotificationEnabled pkg = " + pkg + " allowMore = " + serverRule.mAllowMore);
                this.mPreferencesHelper.setMoreNotificationEnabled(pkg, info.applicationInfo.uid, serverRule.mAllowMore, false);
            }
        }
        this.mReadLock.unlock();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void checkWhenAppInstalled(Intent intent, int changeUserId) {
        Uri uri;
        String pkgName;
        String action = intent.getAction();
        if (!"android.intent.action.PACKAGE_ADDED".equals(action) || (uri = intent.getData()) == null || (pkgName = uri.getSchemeSpecificPart()) == null) {
            return;
        }
        disableNotificationIfNeed(pkgName, changeUserId);
        disableAllowMoreIfNeed(pkgName, changeUserId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void checkWhenAppUninstalled(Intent intent) {
        Uri uri = intent.getData();
        if (uri != null) {
            String pkgName = uri.getSchemeSpecificPart();
            if (!TextUtils.isEmpty(pkgName)) {
                this.mWriteLock.lock();
                NotificationLocalState state = this.mLocalManualStatesByUser.remove(pkgName);
                this.mWriteLock.unlock();
                if (state != null) {
                    storeLocalManualState();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasJoinVivoPush(String pkg) {
        this.mReadLock.lock();
        boolean hasJoin = false;
        if (!TextUtils.isEmpty(pkg)) {
            NotificationServerRule rule = this.mServerWhiteList.get(pkg);
            NotificationLocalState state = this.mLocalManualStatesByUser.get(pkg);
            hasJoin = (rule != null && rule.mJoinVPush) || (state != null && state.mJoinVPushSDK);
        }
        this.mReadLock.unlock();
        return hasJoin;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void joinVivoPushInternal(String pkg) {
        this.mWriteLock.lock();
        NotificationLocalState state = this.mLocalManualStatesByUser.get(pkg);
        if (state == null || !state.mJoinVPushSDK) {
            if (state == null) {
                state = new NotificationLocalState();
            }
            state.mJoinVPushSDK = true;
            this.mLocalManualStatesByUser.put(pkg, state);
            storeLocalManualState();
            PackageManager pm = this.mContext.getPackageManager();
            PackageInfo info = null;
            boolean enabled = false;
            try {
                info = pm.getPackageInfoAsUser(pkg, 0, this.mCurrentUserId);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            if (info != null && info.applicationInfo != null) {
                if (!state.switchedManual) {
                    setNotificationsEnabledForPackageVivo(pkg, info.applicationInfo.uid, true);
                    NotificationServerRule rule = this.mServerWhiteList.get(pkg);
                    PreferencesHelper preferencesHelper = this.mPreferencesHelper;
                    int i = info.applicationInfo.uid;
                    if (rule != null && (!rule.mJoinVPush || rule.mAllowMore)) {
                        enabled = true;
                    }
                    preferencesHelper.setMoreNotificationEnabled(pkg, i, enabled, true);
                } else {
                    if (this.mPreferencesHelper.getImportance(pkg, info.applicationInfo.uid) != 0) {
                        enabled = true;
                    }
                    this.mPreferencesHelper.setMoreNotificationEnabled(pkg, info.applicationInfo.uid, enabled, true);
                }
            }
        }
        this.mWriteLock.unlock();
    }

    private void initLocalManualFromOldFile() {
        if (NotificationManagerService.DBG) {
            VSlog.d(TAG, "initLocalManualFromOldFile");
        }
        ArrayList<String> joinVivoPushAppsFromSdk = new ArrayList<>();
        ArrayList<String> allowMoreAppsByManual = new ArrayList<>();
        ArrayList<String> manualSettingList = new ArrayList<>();
        getWhiteListFromTextFile(MANUAL_SETTING_LIST_FILE, manualSettingList, true);
        getWhiteListFromTextFile(ALLOW_MORE_NOTIFICATIONS_MANUAL_LIST, allowMoreAppsByManual, true);
        getWhiteListFromTextFile(JOIN_VIVO_PUSH_LIST, joinVivoPushAppsFromSdk, true);
        loadManualStateFromOldList(joinVivoPushAppsFromSdk, 2);
        loadManualStateFromOldList(allowMoreAppsByManual, 1);
        loadManualStateFromOldList(manualSettingList, 0);
        checkoutManualList();
        storeLocalManualState();
    }

    @Deprecated
    private void loadManualStateFromOldList(ArrayList<String> originList, int type) {
        Iterator<String> it = originList.iterator();
        while (it.hasNext()) {
            String pkg = it.next();
            if (!TextUtils.isEmpty(pkg)) {
                NotificationLocalState state = this.mLocalManualStatesByUser.get(pkg);
                if (state == null) {
                    state = new NotificationLocalState();
                }
                if (type != 0) {
                    if (type == 1) {
                        state.switchedAllowMoreManual = true;
                    } else if (type == 2) {
                        state.mJoinVPushSDK = true;
                    }
                } else {
                    state.switchedManual = true;
                }
                this.mLocalManualStatesByUser.put(pkg, state);
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:51:0x00f4  */
    /* JADX WARN: Removed duplicated region for block: B:82:? A[RETURN, SYNTHETIC] */
    @java.lang.Deprecated
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private boolean getWhiteListFromTextFile(java.lang.String r12, java.util.ArrayList<java.lang.String> r13, boolean r14) {
        /*
            Method dump skipped, instructions count: 349
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.notification.NotificationWhiteListManager.getWhiteListFromTextFile(java.lang.String, java.util.ArrayList, boolean):boolean");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw) {
        this.mReadLock.lock();
        int N = this.mServerWhiteList.size();
        if (N > 0) {
            pw.println("  Join vivo push apps:");
            for (Map.Entry<String, NotificationServerRule> entry : this.mServerWhiteList.entrySet()) {
                if (entry.getValue() != null && entry.getValue().mJoinVPush) {
                    pw.println("    " + entry.getKey());
                }
            }
            pw.println("  ");
        }
        int N2 = this.mLocalManualState.size();
        if (N2 > 0) {
            pw.println("  local manual state:");
            for (Map.Entry<String, NotificationLocalState> entry2 : this.mLocalManualStatesByUser.entrySet()) {
                NotificationLocalState state = entry2.getValue();
                if (state != null) {
                    pw.println(String.format("    %s joinVPushSDK:%s shieldListManual:%s allowMoreAppsByManual:%s", entry2.getKey(), Boolean.valueOf(state.mJoinVPushSDK), Boolean.valueOf(state.switchedManual), Boolean.valueOf(state.switchedAllowMoreManual)));
                }
            }
            pw.println("  ");
        }
        this.mReadLock.unlock();
    }

    private static boolean safeBoolean(XmlPullParser parser, String att, boolean defValue) {
        String val = parser.getAttributeValue(null, att);
        return TextUtils.isEmpty(val) ? defValue : Boolean.parseBoolean(val);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean safeBoolean(ContentValues contentValues, String key, boolean defValue) {
        Boolean res = contentValues.getAsBoolean(key);
        return res != null ? res.booleanValue() : defValue;
    }

    public void updateUserSetupState(int setupState) {
        this.mHasFinishUserSetup = setupState != 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class NotificationServerRule {
        boolean mAllow;
        boolean mAllowMore;
        boolean mForceTakeEffect;
        boolean mJoinVPush;

        private NotificationServerRule() {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class NotificationLocalState {
        boolean mJoinVPushSDK;
        boolean switchedAllowMoreManual;
        boolean switchedManual;

        private NotificationLocalState() {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class XmlLSHelper {
        private XmlLSHelper() {
        }

        static void loadXml(String filePath, LoadXmlCallback callback) {
            String xmlString = readByBufferedReader(filePath);
            if (!TextUtils.isEmpty(xmlString)) {
                try {
                    ByteArrayInputStream inputStream = new ByteArrayInputStream(xmlString.getBytes());
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setInput(inputStream, null);
                    parser.getName();
                    ContentValues cv = new ContentValues();
                    for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                        String tag = parser.getName();
                        if (eventType == 2) {
                            int attrCount = parser.getAttributeCount();
                            cv = new ContentValues(attrCount);
                            for (int i = 0; i < attrCount; i++) {
                                cv.put(parser.getAttributeName(i), parser.getAttributeValue(i));
                            }
                        } else if (eventType == 4) {
                            cv.put(NotificationWhiteListManager.TAG_ITEM_TEXT, parser.getText());
                        } else if (eventType == 3) {
                            callback.onTagLoad(tag, cv);
                        }
                    }
                    callback.onLoadFinished();
                    inputStream.close();
                } catch (IOException | XmlPullParserException e) {
                    callback.onLoadError(e);
                }
            }
        }

        private static String readByBufferedReader(String filePath) {
            File file = new File(filePath);
            if (!file.exists()) {
                return null;
            }
            StringBuffer buffer = null;
            boolean errorHappened = false;
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                while (true) {
                    String line = bufferedReader.readLine();
                    if (line == null) {
                        break;
                    }
                    if (buffer == null) {
                        buffer = new StringBuffer();
                    }
                    buffer.append(line);
                    buffer.append("\n");
                }
                bufferedReader.close();
            } catch (Exception e) {
                VSlog.e(NotificationWhiteListManager.TAG, "Buffered Reader failed! " + e.getMessage());
                errorHappened = true;
            }
            if (errorHappened || buffer == null || TextUtils.isEmpty(buffer.toString())) {
                return null;
            }
            String result = buffer.toString();
            String decryptedResult = AESUtils.aesDecryptForP(result);
            if (!TextUtils.isEmpty(decryptedResult)) {
                return decryptedResult;
            }
            return result;
        }

        static void storeXml(String filePath, String rootTag, Map<String, List<ContentValues>> xmlItems, StoreXmlCallback callback) {
            Iterator<Map.Entry<String, List<ContentValues>>> it;
            if (TextUtils.isEmpty(rootTag) || xmlItems == null || xmlItems.isEmpty()) {
                VSlog.d(NotificationWhiteListManager.TAG, "store data with empty tag or empty values");
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            File dstFile = new File(filePath);
            if (!dstFile.exists()) {
                try {
                    dstFile.createNewFile();
                } catch (IOException e) {
                    if (NotificationManagerService.DBG) {
                        VSlog.d(NotificationWhiteListManager.TAG, "createNewFile failed " + filePath);
                    }
                }
            }
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            try {
                fastXmlSerializer.setOutput(baos, StandardCharsets.UTF_8.name());
                String str = null;
                fastXmlSerializer.startDocument(null, true);
                fastXmlSerializer.startTag(null, rootTag);
                Iterator<Map.Entry<String, List<ContentValues>>> it2 = xmlItems.entrySet().iterator();
                while (it2.hasNext()) {
                    Map.Entry<String, List<ContentValues>> entry = it2.next();
                    if (entry.getValue() == null) {
                        it = it2;
                    } else {
                        List<ContentValues> attributes = entry.getValue();
                        String itemTag = entry.getKey();
                        int itemCount = attributes.size();
                        int i = 0;
                        while (i < itemCount) {
                            ContentValues itemAttr = attributes.get(i);
                            fastXmlSerializer.startTag(str, itemTag);
                            for (String key : itemAttr.keySet()) {
                                fastXmlSerializer.attribute(null, key, itemAttr.getAsString(key));
                                it2 = it2;
                                entry = entry;
                            }
                            fastXmlSerializer.endTag(null, itemTag);
                            i++;
                            it2 = it2;
                            entry = entry;
                            str = null;
                        }
                        it = it2;
                    }
                    it2 = it;
                    str = null;
                }
                fastXmlSerializer.endTag(null, rootTag);
                fastXmlSerializer.endDocument();
            } catch (IOException e2) {
                callback.onStoreError(e2);
            }
            try {
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(dstFile));
                if (NotificationManagerService.DBG) {
                    VSlog.d(NotificationWhiteListManager.TAG, "createNewFile " + filePath);
                }
                baos.toString();
                String content = AESUtils.aesEncryptForP(baos.toString());
                bufferedWriter.write(content);
                callback.onStoreFinish();
                bufferedWriter.close();
            } catch (Exception e3) {
                VSlog.d(NotificationWhiteListManager.TAG, "saveWhiteListToXmlFile cause exception: " + e3.fillInStackTrace());
                callback.onStoreError(e3);
            }
        }
    }
}