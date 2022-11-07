package com.android.server.am.firewall;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoIqooSecureConnector {
    private static final String ALLOWED = "allowed";
    private static final String AUTHORITY = "com.vivo.appfilter.provider.secureprovider";
    private static final String BROADCAST_PRO = "persist.sys.appfilterbroadcast";
    private static final String CACHED_FILE_NAME = "/data/bring_up_apps.xml";
    private static final Uri CONTENT_URI = Uri.parse("content://com.vivo.appfilter.provider.secureprovider/bring_up_other_apps");
    private static final String FIELD_CALLER_PACKAGENAMES_DISABLED = "caller_package_names_disabled";
    private static final String FIELD_CALLER_PACKAGENAMES_ENABLED = "caller_package_names_enabled";
    private static final String FIELD_COMPONENT_LIST = "component_list";
    private static final String FIELD_DEFAULT_WHITE = "default_white";
    private static final String FIELD_ID = "_id";
    private static final String FIELD_PACKAGE_NAME = "package_name";
    private static final String FIELD_SYSTEM_TYPE = "system_type";
    private static final String FIELD_TYPE_LIMITS = "type_limits";
    private static final String IQOOSECURE_CALLBACK_SERVICE = "com.vivo.appfilter.service.VivoBringupManagerService";
    private static final String IQOOSECURE_PACKAGENAME = "com.vivo.appfilter";
    private static final int MSG_GET_DEFAULT_IME = 5;
    private static final int MSG_READ_FROM_FILE = 1;
    private static final int MSG_UPDATE_CACHE_LIST = 3;
    private static final int MSG_UPDATE_SYSTEM_LIST = 4;
    private static final int MSG_WRITE_TO_FILE = 2;
    private static final String TABLET_NAME = "/bring_up_other_apps";
    private ContentObserver mContentObserver;
    private final Context mContext;
    private String mCurMethodPackage;
    private Handler mHandler;
    private IntentFilter mIntentFilter;
    private ContentResolver mResolver;
    private SwtichFunctionReceiver mSwitchReceiver;
    private ContentObserver mSystemContentObserver;
    private String TAG = VivoFirewall.TAG;
    private boolean mEnable = false;
    private boolean mHaveRegisterOberver = false;
    private boolean mOnlyScreenOff = false;
    private boolean mScreenOff = false;
    private boolean mActivityScreenOffSwitch = false;
    private boolean mStartInstrumentSwitch = false;
    private boolean mAllProviderType = false;
    private boolean mAllActivityType = false;
    private boolean mCallerNullTypeSwitch = false;
    private int mBringupContinuousSwitch = 0;
    private int mMaxComboTimes = 8;
    private int mMaxComboDuration = 1000;
    private File mFileName = new File(CACHED_FILE_NAME);
    private HashMap<String, VivoAppRuleItem> mWhiteStartupMap = new HashMap<>();
    private HashMap<String, VivoSpecialRuleItem> mActivityComponentMap = new HashMap<>();
    private HashMap<String, VivoSpecialRuleItem> mFgActivityCtrlMap = new HashMap<>();
    private HashMap<String, VivoSpecialRuleItem> mFgActivityWhiteMap = new HashMap<>();
    private ArrayList<String> mSystemAppBlackList = new ArrayList<>();
    private ArrayList<String> mSpecailList = new ArrayList<>();
    private ArrayList<String> mSysTypeList = new ArrayList<>();
    private ArrayList<String> mSyncForbidTypeList = new ArrayList<>();
    private String BROADCAST_SWITCH = "com.vivo.appfilter.BringUpSwitch";
    private VivoCacheFileMgr mVivoCacheFileRW = new VivoCacheFileMgr();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class SwtichFunctionReceiver extends BroadcastReceiver {
        SwtichFunctionReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            if (VivoFirewall.DEBUG) {
                String str = VivoIqooSecureConnector.this.TAG;
                VSlog.d(str, "firewall receive " + intent.getAction());
            }
            if (!VivoIqooSecureConnector.this.BROADCAST_SWITCH.equals(intent.getAction())) {
                if ("android.intent.action.SCREEN_ON".equals(intent.getAction())) {
                    VivoIqooSecureConnector.this.mScreenOff = false;
                    return;
                } else if ("android.intent.action.SCREEN_OFF".equals(intent.getAction())) {
                    VivoIqooSecureConnector.this.mScreenOff = true;
                    return;
                } else if ("android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
                    VivoIqooSecureConnector.this.mHandler.obtainMessage(3).sendToTarget();
                    VivoIqooSecureConnector.this.mHandler.obtainMessage(5).sendToTarget();
                    return;
                } else {
                    return;
                }
            }
            boolean fromAppfilter = SystemProperties.getBoolean(VivoIqooSecureConnector.BROADCAST_PRO, false);
            String str2 = VivoIqooSecureConnector.this.TAG;
            VSlog.d(str2, "fromAppfilter:" + fromAppfilter);
            if (fromAppfilter) {
                try {
                    SystemProperties.set(VivoIqooSecureConnector.BROADCAST_PRO, "off");
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    VSlog.w(VivoIqooSecureConnector.this.TAG, "Throw runtime exception while setting prop");
                }
                boolean switchOn = intent.getBooleanExtra("switchon", false);
                boolean onlyScreenOff = intent.getBooleanExtra("onlyScreenOff", false);
                VivoIqooSecureConnector.this.mMaxComboTimes = intent.getIntExtra("maxComboTimes", 8);
                VivoIqooSecureConnector.this.mMaxComboDuration = intent.getIntExtra("maxComboDuration", 1000);
                VivoIqooSecureConnector.this.mOnlyScreenOff = onlyScreenOff;
                VivoIqooSecureConnector.this.mAllProviderType = intent.getBooleanExtra("allProviderType", false);
                VivoIqooSecureConnector.this.mAllActivityType = intent.getBooleanExtra("allActivityType", false);
                VivoIqooSecureConnector.this.mCallerNullTypeSwitch = intent.getBooleanExtra("callerNullTypeSwitch", false);
                VivoIqooSecureConnector.this.mBringupContinuousSwitch = intent.getIntExtra("bringupContinuousSwitch", 0);
                VivoIqooSecureConnector.this.mActivityScreenOffSwitch = intent.getBooleanExtra("activityScreenOffSwitch", false);
                VivoIqooSecureConnector.this.mStartInstrumentSwitch = intent.getBooleanExtra("startInstrumentSwitch", false);
                if (VivoIqooSecureConnector.this.mSysTypeList == null) {
                    VivoIqooSecureConnector.this.mSysTypeList = new ArrayList();
                }
                if (VivoIqooSecureConnector.this.mSysTypeList != null) {
                    VivoIqooSecureConnector.this.mSysTypeList.clear();
                }
                VivoIqooSecureConnector.this.mSysTypeList.addAll(intent.getStringArrayListExtra("sysTypeList"));
                if (VivoIqooSecureConnector.this.mSyncForbidTypeList == null) {
                    VivoIqooSecureConnector.this.mSyncForbidTypeList = new ArrayList();
                }
                if (VivoIqooSecureConnector.this.mSyncForbidTypeList != null) {
                    VivoIqooSecureConnector.this.mSyncForbidTypeList.clear();
                }
                VivoIqooSecureConnector.this.mSyncForbidTypeList.addAll(intent.getStringArrayListExtra("syncForbidTypeList"));
                if (VivoFirewall.DEBUG) {
                    String str3 = VivoIqooSecureConnector.this.TAG;
                    VSlog.d(str3, "firewall receive switch broadcast switchon = " + switchOn + ", onlyScreenOff = " + onlyScreenOff + ",mStartInstrumentSwitch=" + VivoIqooSecureConnector.this.mStartInstrumentSwitch + ",mMaxComboTimes=" + VivoIqooSecureConnector.this.mMaxComboTimes + ",mMaxComboDuration=" + VivoIqooSecureConnector.this.mMaxComboDuration + ",mSyncForbidTypeList=" + VivoIqooSecureConnector.this.mSyncForbidTypeList + ",mSysTypeList=" + VivoIqooSecureConnector.this.mSysTypeList);
                }
                if (switchOn) {
                    VivoIqooSecureConnector.this.setEnable(true);
                } else {
                    VivoIqooSecureConnector.this.setEnable(false);
                }
            }
        }
    }

    /* loaded from: classes.dex */
    class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            synchronized (VivoIqooSecureConnector.this.mWhiteStartupMap) {
                int i = msg.what;
                if (i == 1) {
                    boolean fromSetEnableFunction = msg.arg1 == 1;
                    boolean result = VivoIqooSecureConnector.this.mVivoCacheFileRW.readConfigXmlFile(VivoIqooSecureConnector.this.mFileName, VivoIqooSecureConnector.this.mWhiteStartupMap);
                    if (!fromSetEnableFunction) {
                        VivoIqooSecureConnector.this.mEnable = result;
                    }
                } else if (i == 2) {
                    VivoIqooSecureConnector.this.mVivoCacheFileRW.writeConfigXmlFile(VivoIqooSecureConnector.this.mWhiteStartupMap, VivoIqooSecureConnector.this.mFileName, VivoIqooSecureConnector.this.mEnable);
                } else if (i == 3) {
                    long id = Long.MAX_VALUE;
                    if (msg.obj != null) {
                        id = ((Long) msg.obj).longValue();
                    }
                    VivoIqooSecureConnector.this.updateCacheList(id);
                } else if (i == 5) {
                    String imeId = Settings.Secure.getStringForUser(VivoIqooSecureConnector.this.mResolver, "default_input_method", -2);
                    if (!TextUtils.isEmpty(imeId)) {
                        VivoIqooSecureConnector.this.mCurMethodPackage = imeId.split("/")[0];
                    }
                }
            }
        }
    }

    /* loaded from: classes.dex */
    class IqooSecureDBObserver extends ContentObserver {
        public IqooSecureDBObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            Uri defaultImeUri = Settings.Secure.getUriFor("default_input_method");
            if (defaultImeUri.equals(uri)) {
                if (VivoFirewall.DEBUG) {
                    String str = VivoIqooSecureConnector.this.TAG;
                    VSlog.d(str, "IMEObserver onChange uri=" + uri);
                }
                VivoIqooSecureConnector.this.mHandler.obtainMessage(5).sendToTarget();
                return;
            }
            Long id = null;
            try {
                id = Long.valueOf(ContentUris.parseId(uri));
            } catch (Exception e) {
            }
            if (id == null) {
                VivoIqooSecureConnector.this.mHandler.removeMessages(3);
            }
            if (VivoFirewall.DEBUG) {
                String str2 = VivoIqooSecureConnector.this.TAG;
                VSlog.d(str2, "IqooSecureDBListener onChange uri=" + uri + ",id=" + id);
            }
            Message msg = VivoIqooSecureConnector.this.mHandler.obtainMessage(3);
            msg.obj = id;
            VivoIqooSecureConnector.this.mHandler.sendMessageDelayed(msg, 500L);
        }
    }

    public VivoIqooSecureConnector(Context context, Looper lopper) {
        this.mHandler = null;
        this.mResolver = null;
        this.mContext = context;
        this.mHandler = new MyHandler(lopper);
        this.mResolver = this.mContext.getContentResolver();
        this.mContentObserver = new IqooSecureDBObserver(this.mHandler);
        IntentFilter intentFilter = new IntentFilter();
        this.mIntentFilter = intentFilter;
        intentFilter.addAction(this.BROADCAST_SWITCH);
        this.mIntentFilter.addAction("android.intent.action.SCREEN_OFF");
        this.mIntentFilter.addAction("android.intent.action.SCREEN_ON");
        this.mIntentFilter.addAction("android.intent.action.USER_SWITCHED");
        this.mSwitchReceiver = new SwtichFunctionReceiver();
    }

    public void registerBroadcast() {
        this.mContext.registerReceiver(this.mSwitchReceiver, this.mIntentFilter, null, this.mHandler);
    }

    public void startObserver() {
        if (this.mHaveRegisterOberver) {
            return;
        }
        ContentResolver resolver = this.mContext.getContentResolver();
        resolver.registerContentObserver(CONTENT_URI, true, this.mContentObserver, -1);
        resolver.registerContentObserver(Settings.Secure.getUriFor("default_input_method"), false, this.mContentObserver, -1);
        updateCacheList(Long.MAX_VALUE);
        this.mHandler.obtainMessage(5).sendToTarget();
        this.mHaveRegisterOberver = true;
    }

    public void stopObserver() {
        if (!this.mHaveRegisterOberver) {
            return;
        }
        ContentResolver resolver = this.mContext.getContentResolver();
        resolver.unregisterContentObserver(this.mContentObserver);
        this.mHaveRegisterOberver = false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Code restructure failed: missing block: B:145:0x02d4, code lost:
        if (r8 != null) goto L175;
     */
    /* JADX WARN: Code restructure failed: missing block: B:151:0x02e5, code lost:
        if (r8 == null) goto L173;
     */
    /* JADX WARN: Code restructure failed: missing block: B:152:0x02e7, code lost:
        r8.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:154:0x02eb, code lost:
        return;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public void updateCacheList(long r27) {
        /*
            Method dump skipped, instructions count: 758
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.firewall.VivoIqooSecureConnector.updateCacheList(long):void");
    }

    public HashMap<String, VivoAppRuleItem> getWhiteStartupData() {
        return this.mWhiteStartupMap;
    }

    public ArrayList<String> getSpecialStartupData() {
        return this.mSpecailList;
    }

    public ArrayList<String> getSytemAppBlackList() {
        return this.mSystemAppBlackList;
    }

    public HashMap<String, VivoSpecialRuleItem> getActivityComponentMap() {
        return this.mActivityComponentMap;
    }

    public HashMap<String, VivoSpecialRuleItem> getFgActivityCtrlMap() {
        return this.mFgActivityCtrlMap;
    }

    public HashMap<String, VivoSpecialRuleItem> getFgActivityWhiteMap() {
        return this.mFgActivityWhiteMap;
    }

    public int getMaxComboTimes() {
        return this.mMaxComboTimes;
    }

    public int getMaxComboDuration() {
        return this.mMaxComboDuration;
    }

    public boolean isEnable() {
        return this.mEnable;
    }

    public boolean isInRightScreenState() {
        return !this.mOnlyScreenOff || this.mScreenOff;
    }

    public boolean getActivityScreenSwitch() {
        return this.mActivityScreenOffSwitch;
    }

    public boolean getStartInstrumentSwitch() {
        return this.mStartInstrumentSwitch;
    }

    public boolean checkDefaultIMEPackage(String packageName) {
        if (TextUtils.isEmpty(this.mCurMethodPackage)) {
            this.mHandler.obtainMessage(5).sendToTarget();
        }
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        if (VivoFirewall.DEBUG) {
            String str = this.TAG;
            VSlog.d(str, "checkDefaultIMEPackage mCurMethodPackage=" + this.mCurMethodPackage);
        }
        return packageName.equals(this.mCurMethodPackage);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean setEnable(boolean enable) {
        synchronized (this.mWhiteStartupMap) {
            Message msg = Message.obtain();
            msg.arg1 = 1;
            if (enable) {
                startObserver();
            } else {
                stopObserver();
            }
            this.mEnable = enable;
        }
        return enable;
    }

    public void bringupIqooService(Message msg) {
        Intent intent = new Intent(IQOOSECURE_CALLBACK_SERVICE);
        intent.setPackage(IQOOSECURE_PACKAGENAME);
        intent.putExtras(msg.getData());
        try {
            if (VivoFirewall.DEBUG) {
                VSlog.d(this.TAG, "Firewall: START IQOOSECURE_CALLBACK_SERVICE");
            }
            this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
        } catch (Exception e) {
            if (VivoFirewall.DEBUG) {
                VSlog.d(this.TAG, "Firewall: START IQOOSECURE SERVICE Failed");
            }
        }
    }

    public boolean isSysTypeOn(String sysType) {
        ArrayList<String> arrayList;
        if (sysType == null || (arrayList = this.mSysTypeList) == null || arrayList.size() == 0) {
            return false;
        }
        return this.mSysTypeList.contains(sysType);
    }

    public boolean isForbiddenSyncType(String syncType) {
        ArrayList<String> arrayList;
        if (syncType == null || (arrayList = this.mSyncForbidTypeList) == null || arrayList.size() == 0) {
            return false;
        }
        if (this.mSyncForbidTypeList.contains(syncType)) {
            return true;
        }
        if (this.mSyncForbidTypeList.contains("vivoUid")) {
            try {
                int uid = Integer.parseInt(syncType);
                if (uid >= 10000 && uid <= 19999) {
                    return true;
                }
            } catch (Exception e) {
                VSlog.d(this.TAG, "sync reason is not uid");
            }
        }
        return false;
    }

    public boolean isSystemAppControled(String packageName) {
        if (this.mSystemAppBlackList.size() == 0 || TextUtils.isEmpty(packageName) || !this.mSystemAppBlackList.contains(packageName)) {
            return false;
        }
        String str = this.TAG;
        VSlog.d(str, packageName + " is under controled!");
        return true;
    }

    public boolean checkActivityComponentState(String callerPackage, String topComponent, String bingupComponent) {
        VivoSpecialRuleItem componentRuleItem = this.mActivityComponentMap.get(bingupComponent);
        if (componentRuleItem != null && componentRuleItem.checkActivityComponentState(callerPackage, topComponent)) {
            return true;
        }
        return false;
    }

    public boolean isAllProviderTypeOn() {
        return this.mAllProviderType;
    }

    public boolean isAllActivityTypeOn() {
        return this.mAllActivityType;
    }

    public boolean isCallerNullTypeOn() {
        return this.mCallerNullTypeSwitch;
    }

    public int getBringupContinuousSwitch() {
        return this.mBringupContinuousSwitch;
    }

    public void dumpCachedInfo(FileDescriptor fd, PrintWriter pw, String[] args, int opti) {
        if (args.length <= 1) {
            if (pw != null) {
                pw.println("Invalid argument!");
                return;
            }
            return;
        }
        String type = args[opti];
        if ("print".equals(type)) {
            dumpPrint(fd, pw, args, opti);
        } else {
            pw.println("Invalid argument!");
        }
    }

    private void dumpPrint(FileDescriptor fd, PrintWriter pw, String[] args, int opti) {
        pw.println("enable: " + this.mEnable);
        pw.println("providerType: " + this.mAllProviderType);
        pw.println("activityType: " + this.mAllActivityType);
        pw.println("activityScreenOffSwitch: " + this.mActivityScreenOffSwitch);
        pw.println("startInstrumentSwitch: " + this.mStartInstrumentSwitch);
        pw.println("callerNullType: " + this.mCallerNullTypeSwitch);
        pw.println("bringupContinuousSwitch: " + this.mBringupContinuousSwitch);
        HashMap<String, VivoAppRuleItem> hashMap = this.mWhiteStartupMap;
        if (hashMap != null) {
            for (Map.Entry<String, VivoAppRuleItem> entry : hashMap.entrySet()) {
                VivoAppRuleItem item = entry.getValue();
                pw.println("----------------------");
                item.dump(pw);
                pw.println("----------------------");
            }
        }
        ArrayList<String> arrayList = this.mSyncForbidTypeList;
        if (arrayList != null && arrayList.size() > 0) {
            Iterator<String> it = this.mSyncForbidTypeList.iterator();
            while (it.hasNext()) {
                String syncType = it.next();
                pw.println("Forbid sync type:" + syncType);
            }
        }
        ArrayList<String> arrayList2 = this.mSysTypeList;
        if (arrayList2 != null && arrayList2.size() > 0) {
            Iterator<String> it2 = this.mSysTypeList.iterator();
            while (it2.hasNext()) {
                String sysType = it2.next();
                pw.println("sys type:" + sysType);
            }
        }
    }

    private void dumpPolicy(FileDescriptor fd, PrintWriter pw, String[] args, int opti) {
        if (args != null && args.length == 3 && "policy".equals(args[1])) {
            if ("true".equals(args[2])) {
                pw.println("turn on bringup function.");
                setEnable(true);
                return;
            }
            pw.println("turn off bringup function.");
            setEnable(false);
        }
    }

    private void dumpRead(FileDescriptor fd, PrintWriter pw, String[] args, int opti) {
        this.mHandler.removeMessages(1);
        this.mHandler.sendEmptyMessage(1);
    }

    private void dumpWrite(FileDescriptor fd, PrintWriter pw, String[] args, int opti) {
        this.mHandler.removeMessages(2);
        this.mHandler.sendEmptyMessage(2);
    }

    private void dumpChange(FileDescriptor fd, PrintWriter pw, String[] args, int opti) {
        if (args.length <= 2) {
            if (pw != null) {
                pw.println("  Invalid argument!");
                return;
            }
            return;
        }
        if (args.length == 4 && !TextUtils.isEmpty(args[2]) && !TextUtils.isEmpty(args[3])) {
            if (this.mWhiteStartupMap.get(args[2]) != null) {
                if ("true".equals(args[3])) {
                    this.mWhiteStartupMap.get(args[2]).setAllowBringup(true);
                } else {
                    this.mWhiteStartupMap.get(args[2]).setAllowBringup(false);
                }
            } else {
                VivoAppRuleItem item = new VivoAppRuleItem(args[2]);
                if ("true".equals(args[3])) {
                    item.setAllowBringup(true);
                } else {
                    item.setAllowBringup(false);
                }
                this.mWhiteStartupMap.put(args[2], item);
            }
        }
        if (args.length == 5 && !TextUtils.isEmpty(args[2]) && !TextUtils.isEmpty(args[3]) && !TextUtils.isEmpty(args[4])) {
            if ("true".equals(args[4])) {
                if (this.mWhiteStartupMap.get(args[2]) != null) {
                    this.mWhiteStartupMap.get(args[2]).getBringupRule().put(args[3], true);
                    return;
                }
                VivoAppRuleItem item2 = new VivoAppRuleItem(args[2]);
                item2.getBringupRule().put(args[3], true);
                this.mWhiteStartupMap.put(args[2], item2);
            } else if (this.mWhiteStartupMap.get(args[2]) != null) {
                this.mWhiteStartupMap.get(args[2]).getBringupRule().put(args[3], false);
            } else {
                VivoAppRuleItem item3 = new VivoAppRuleItem(args[2]);
                item3.getBringupRule().put(args[3], false);
                this.mWhiteStartupMap.put(args[2], item3);
            }
        }
    }
}