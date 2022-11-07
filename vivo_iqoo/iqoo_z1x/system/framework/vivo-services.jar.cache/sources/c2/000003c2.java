package com.android.server.policy;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.KeyEvent;
import com.android.internal.policy.KeyInterceptionInfo;
import com.android.server.UnifiedConfigThread;
import com.android.server.policy.WindowManagerPolicy;
import com.vivo.common.utils.VLog;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

/* loaded from: classes.dex */
public class ForceBackManager {
    private static final String ATTR_BACK_COUNT = "backcount";
    private static final String ATTR_PACKAGE_NAME = "packagename";
    private static final String ATTR_TITLE = "title";
    private static final String CONFIG_IDENTIFIER_BACKSCREEN = "forceback";
    private static final String CONFIG_MODULE_NAME_BACK_SCREEN = "ForceBack";
    private static final String CONFIG_UPDATE_ACTION_BACK_SCREEN = "com.vivo.daemonService.unifiedconfig.update_finish_broadcast_ForceBack";
    private static final String CONFIG_URI = "content://com.vivo.abe.unifiedconfig.provider/configs";
    private static final String CONFIG_VERSION = "1.0";
    private static final int DEFAULT_BACK_COUNT = 3;
    private static final boolean DEFAULT_ENABLE = true;
    private static final int NAVIGATION_GESTURE = 1;
    private static final String NAVIGATION_GESTURE_ON_KEY = "navigation_gesture_on";
    private static final int NAVIGATION_KEYS = 0;
    private static final int RESULT_LOW_FREQUENCY = 0;
    private static final int RESULT_NOT_IN_LIST = -1;
    private static final int RESULT_TRIGGER = 1;
    private static final String TAG = "ForceBackManager";
    private static final String TAG_ENABLE = "enable";
    private static final String TAG_GESTURE_THRESHOLD = "gesturehreshold";
    private static final String TAG_IGNORE_REPORT_WINDOW = "ignore_report_window";
    private static final String TAG_KEY_THRESHOLD = "keythreshold";
    private static final String TAG_WINDOW = "window";
    private Context mContext;
    private long mFirstBackKeyTime;
    private Handler mHandler;
    private WindowManagerPolicy.WindowManagerFuncs mWindowManagerFuncs;
    public static boolean DEBUG = false;
    private static final long DEFAULT_BACK_KEY_TIME_OUT_THRESHOLD = SystemProperties.getLong("persist.vivo.backkey.threshold", 1000);
    private static final long DEFAULT_BACK_GESTURE_TIME_OUT_THRESHOLD = SystemProperties.getLong("persist.vivo.backgesture.threshold", 1000);
    private final Object mLock = new Object();
    private final ArrayList<ForceBackWindowInfo> mForceBackWindowList = new ArrayList<>();
    private final ArrayList<ForceBackWindowInfo> mIgnoreReportWindowList = new ArrayList<>();
    private KeyInterceptionInfo mLastAceeptedWindow = null;
    private boolean mEnable = true;
    private long mBackKeyTimeOut = DEFAULT_BACK_KEY_TIME_OUT_THRESHOLD;
    private long mBackGestureTimeOut = DEFAULT_BACK_GESTURE_TIME_OUT_THRESHOLD;
    private int mNavigationMode = 1;
    private int mContinuousBackKeyCount = 0;
    private boolean mInputMethodWindowShow = false;
    private Runnable mReadConfigRunnable = new Runnable() { // from class: com.android.server.policy.ForceBackManager.1
        @Override // java.lang.Runnable
        public void run() {
            ForceBackManager.this.readConfigFromDB("content://com.vivo.abe.unifiedconfig.provider/configs", ForceBackManager.CONFIG_MODULE_NAME_BACK_SCREEN, "1");
        }
    };
    private BroadcastReceiver mReceiver = new BroadcastReceiver() { // from class: com.android.server.policy.ForceBackManager.2
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (ForceBackManager.CONFIG_UPDATE_ACTION_BACK_SCREEN.equals(intent.getAction())) {
                if (VivoPolicyUtil.IS_LOG_OPEN) {
                    VLog.d(ForceBackManager.TAG, "receive com.vivo.daemonService.unifiedconfig.update_finish_broadcast_ForceBack");
                }
                ForceBackManager.this.mUnifiedConfigHandler.post(ForceBackManager.this.mReadConfigRunnable);
            }
        }
    };
    private Handler mUnifiedConfigHandler = UnifiedConfigThread.getHandler();

    public ForceBackManager(Context context, Handler handler, WindowManagerPolicy.WindowManagerFuncs windowManagerFuncs) {
        this.mContext = context;
        this.mHandler = handler;
        this.mWindowManagerFuncs = windowManagerFuncs;
        NavigationModeObserver navigationModeObserver = new NavigationModeObserver(this.mHandler);
        navigationModeObserver.observe();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CONFIG_UPDATE_ACTION_BACK_SCREEN);
        this.mContext.registerReceiver(this.mReceiver, intentFilter);
    }

    private static int safeInt(XmlPullParser parser, String att, int defValue) {
        String val = parser.getAttributeValue(null, att);
        return tryParseInt(val, defValue);
    }

    private static int tryParseInt(String value, int defValue) {
        if (TextUtils.isEmpty(value)) {
            return defValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defValue;
        }
    }

    private static long tryParseLong(String value, long defValue) {
        if (TextUtils.isEmpty(value)) {
            return defValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defValue;
        }
    }

    private static boolean safeBoolean(String val, boolean defValue) {
        return TextUtils.isEmpty(val) ? defValue : Boolean.parseBoolean(val);
    }

    public void systemReady() {
        this.mUnifiedConfigHandler.postDelayed(this.mReadConfigRunnable, 60000L);
    }

    public void countingBackEvent(KeyInterceptionInfo keyInterceptionInfo, KeyEvent event) {
        if (!this.mEnable) {
            VLog.d(TAG, "Force back feature disabled!");
            return;
        }
        int keyCode = event.getKeyCode();
        boolean down = event.getAction() == 0;
        long now = SystemClock.uptimeMillis();
        if (down) {
            WindowManagerPolicy.WindowState windowState = this.mWindowManagerFuncs.getInputMethodWindowLw();
            this.mInputMethodWindowShow = windowState != null && windowState.isVisibleLw();
        }
        if (this.mInputMethodWindowShow) {
            if (VivoPolicyUtil.IS_LOG_OPEN) {
                VLog.d(TAG, "Ignored when ime shown!");
            }
        } else if (keyCode == 4 && !down) {
            if (this.mLastAceeptedWindow != null && keyInterceptionInfo.mOwningPackage.equals(this.mLastAceeptedWindow.mOwningPackage) && keyInterceptionInfo.mLayoutTitle.equals(this.mLastAceeptedWindow.mLayoutTitle) && keyInterceptionInfo.hashCode == this.mLastAceeptedWindow.hashCode && now - this.mFirstBackKeyTime < getPostDelayResetCountTimeOut()) {
                this.mContinuousBackKeyCount++;
            } else {
                this.mContinuousBackKeyCount = 1;
            }
            if (this.mContinuousBackKeyCount == 1) {
                this.mFirstBackKeyTime = event.getEventTime();
            }
            this.mLastAceeptedWindow = keyInterceptionInfo;
            if (VivoPolicyUtil.IS_LOG_OPEN) {
                VLog.d(TAG, "mContinuousBackKeyCount = " + this.mContinuousBackKeyCount);
            }
            if (this.mContinuousBackKeyCount >= 2 && now - this.mFirstBackKeyTime < getPostDelayResetCountTimeOut()) {
                int checkForceBackResult = shouldForceBack(keyInterceptionInfo);
                if (1 == checkForceBackResult) {
                    this.mContinuousBackKeyCount = 0;
                    goHome();
                    InputExceptionReport.getInstance().reportForceBack(keyInterceptionInfo.mLayoutTitle, true);
                } else if (-1 == checkForceBackResult && this.mContinuousBackKeyCount == 3 && !shouldIgnoreReportToEPM(keyInterceptionInfo)) {
                    InputExceptionReport.getInstance().reportForceBack(keyInterceptionInfo.mLayoutTitle, false);
                }
            }
        }
    }

    private long getPostDelayResetCountTimeOut() {
        long delayTime = this.mBackKeyTimeOut;
        int i = this.mNavigationMode;
        if (i == 0) {
            long delayTime2 = this.mBackKeyTimeOut;
            return delayTime2;
        } else if (i == 1) {
            long delayTime3 = this.mBackGestureTimeOut;
            return delayTime3;
        } else {
            return delayTime;
        }
    }

    private int shouldForceBack(KeyInterceptionInfo keyInterceptionInfo) {
        synchronized (this.mLock) {
            Iterator<ForceBackWindowInfo> it = this.mForceBackWindowList.iterator();
            while (it.hasNext()) {
                ForceBackWindowInfo forceBackWindowInfo = it.next();
                if (keyInterceptionInfo.mOwningPackage.equals(forceBackWindowInfo.mPackageName) && keyInterceptionInfo.mLayoutTitle.contains(forceBackWindowInfo.mWindowTitle)) {
                    if (this.mContinuousBackKeyCount >= forceBackWindowInfo.mBackKeyCount) {
                        return 1;
                    }
                    return 0;
                }
            }
            return -1;
        }
    }

    private boolean shouldIgnoreReportToEPM(KeyInterceptionInfo keyInterceptionInfo) {
        synchronized (this.mLock) {
            Iterator<ForceBackWindowInfo> it = this.mIgnoreReportWindowList.iterator();
            while (it.hasNext()) {
                ForceBackWindowInfo forceBackWindowInfo = it.next();
                if (keyInterceptionInfo.mOwningPackage.equals(forceBackWindowInfo.mPackageName) && keyInterceptionInfo.mLayoutTitle.contains(forceBackWindowInfo.mWindowTitle)) {
                    return true;
                }
            }
            return false;
        }
    }

    private void goHome() {
        Intent intent = new Intent("android.intent.action.MAIN", (Uri) null);
        intent.addCategory("android.intent.category.HOME");
        intent.addFlags(270532608);
        if (isUserSetupComplete()) {
            VLog.d(TAG, "force go home!");
            this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
            return;
        }
        VLog.i(TAG, "Not starting activity because user setup is in progress: " + intent);
    }

    boolean isUserSetupComplete() {
        boolean isSetupComplete = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 0, -2) != 0;
        return isSetupComplete;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void readConfigFromDB(String uri, String moduleName, String type) {
        VLog.d(TAG, "readConfigFromDB");
        ContentResolver resolver = this.mContext.getContentResolver();
        String[] selectionArgs = {moduleName, type, "1.0", CONFIG_IDENTIFIER_BACKSCREEN};
        Cursor cursor = null;
        try {
            try {
                cursor = resolver.query(Uri.parse(uri), null, null, selectionArgs, null);
                if (cursor != null && cursor.moveToFirst()) {
                    while (!cursor.isAfterLast()) {
                        String fileId = cursor.getString(cursor.getColumnIndex("id"));
                        String targetIdentifier = cursor.getString(cursor.getColumnIndex("identifier"));
                        String fileVersion = cursor.getString(cursor.getColumnIndex("fileversion"));
                        byte[] fileContent = cursor.getBlob(cursor.getColumnIndex("filecontent"));
                        String contents = new String(fileContent, "UTF-8");
                        VLog.d(TAG, "init Config  fileId:" + fileId + " identified:" + targetIdentifier + " fileVersion:" + fileVersion + " \n" + contents);
                        if (CONFIG_IDENTIFIER_BACKSCREEN.equals(targetIdentifier) && !TextUtils.isEmpty(contents)) {
                            parseConfigFromDB(contents);
                        }
                        cursor.moveToNext();
                    }
                }
                if (cursor == null) {
                    return;
                }
            } catch (Exception e) {
                VLog.e(TAG, "OPEN DB!!! e=" + e);
                if (cursor == null) {
                    return;
                }
            }
            cursor.close();
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
            throw th;
        }
    }

    private void parseConfigFromDB(String contents) {
        if (TextUtils.isEmpty(contents)) {
            VLog.w(TAG, "parse config, content is empty. " + contents);
            return;
        }
        VLog.d(TAG, "parse config from str\n " + contents);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(contents.getBytes());
        try {
            try {
                try {
                    XmlPullParserFactory pullFactory = XmlPullParserFactory.newInstance();
                    XmlPullParser parser = pullFactory.newPullParser();
                    parser.setInput(inputStream, "utf-8");
                    ArrayList<ForceBackWindowInfo> forceBackWindowInfos = new ArrayList<>();
                    ArrayList<ForceBackWindowInfo> igonreReportWindowInfos = new ArrayList<>();
                    for (int eventCode = parser.getEventType(); eventCode != 1; eventCode = parser.next()) {
                        if (eventCode == 2) {
                            String name = parser.getName();
                            if (TAG_ENABLE.equals(name)) {
                                parser.next();
                                this.mEnable = safeBoolean(parser.getText(), true);
                            } else if (TAG_KEY_THRESHOLD.equals(name)) {
                                parser.next();
                                this.mBackKeyTimeOut = tryParseLong(parser.getText(), DEFAULT_BACK_KEY_TIME_OUT_THRESHOLD);
                            } else if (TAG_GESTURE_THRESHOLD.equals(name)) {
                                parser.next();
                                this.mBackGestureTimeOut = tryParseLong(parser.getText(), DEFAULT_BACK_GESTURE_TIME_OUT_THRESHOLD);
                            } else if (TAG_WINDOW.equals(name)) {
                                String packageName = parser.getAttributeValue(null, ATTR_PACKAGE_NAME);
                                String windowTitle = parser.getAttributeValue(null, ATTR_TITLE);
                                int count = safeInt(parser, ATTR_BACK_COUNT, 3);
                                ForceBackWindowInfo forceBackWindowInfo = new ForceBackWindowInfo(packageName, windowTitle, count);
                                if (VivoPolicyUtil.IS_LOG_OPEN) {
                                    VLog.d(TAG, "forceBackWindowInfo = " + forceBackWindowInfo);
                                }
                                forceBackWindowInfos.add(forceBackWindowInfo);
                            } else if (TAG_IGNORE_REPORT_WINDOW.equals(name)) {
                                String packageName2 = parser.getAttributeValue(null, ATTR_PACKAGE_NAME);
                                String windowTitle2 = parser.getAttributeValue(null, ATTR_TITLE);
                                ForceBackWindowInfo ignoreReportWindowInfo = new ForceBackWindowInfo(packageName2, windowTitle2, 0);
                                if (VivoPolicyUtil.IS_LOG_OPEN) {
                                    VLog.d(TAG, "forceBackWindowInfo = " + ignoreReportWindowInfo);
                                }
                                igonreReportWindowInfos.add(ignoreReportWindowInfo);
                            }
                        }
                    }
                    synchronized (this.mLock) {
                        this.mForceBackWindowList.clear();
                        this.mForceBackWindowList.addAll(forceBackWindowInfos);
                        this.mIgnoreReportWindowList.clear();
                        this.mIgnoreReportWindowList.addAll(igonreReportWindowInfos);
                    }
                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (IOException | XmlPullParserException e2) {
                e2.printStackTrace();
                inputStream.close();
            }
        } catch (Throwable th) {
            try {
                inputStream.close();
            } catch (Exception e3) {
                e3.printStackTrace();
            }
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateNavigationMode() {
        this.mNavigationMode = Settings.Secure.getInt(this.mContext.getContentResolver(), "navigation_gesture_on", 0);
    }

    public void dump(String prefix, PrintWriter pw) {
        if (!VivoPolicyUtil.IS_LOG_OPEN) {
            return;
        }
        pw.println(prefix + TAG);
        String prefix2 = prefix + "  ";
        pw.println(prefix2 + "mEnable = " + this.mEnable);
        pw.println(prefix2 + "mBackKeyTimeOut = " + this.mBackKeyTimeOut);
        pw.println(prefix2 + "mBackGestureTimeOut = " + this.mBackGestureTimeOut);
        synchronized (this.mLock) {
            pw.println(prefix2 + "ForceBackWindowList:");
            Iterator<ForceBackWindowInfo> it = this.mForceBackWindowList.iterator();
            while (it.hasNext()) {
                ForceBackWindowInfo forceBackWindowInfo = it.next();
                pw.println(prefix2 + "  " + forceBackWindowInfo);
            }
            pw.println(prefix2 + "IgnoreReportWindowList:");
            Iterator<ForceBackWindowInfo> it2 = this.mIgnoreReportWindowList.iterator();
            while (it2.hasNext()) {
                ForceBackWindowInfo forceBackWindowInfo2 = it2.next();
                pw.println(prefix2 + "  " + forceBackWindowInfo2);
            }
        }
    }

    /* loaded from: classes.dex */
    class NavigationModeObserver extends ContentObserver {
        public NavigationModeObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = ForceBackManager.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.Secure.getUriFor("navigation_gesture_on"), false, this, -1);
            ForceBackManager.this.updateNavigationMode();
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            ForceBackManager.this.updateNavigationMode();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class ForceBackWindowInfo {
        int mBackKeyCount;
        String mPackageName;
        String mWindowTitle;

        public ForceBackWindowInfo(String packageName, String windowTitle, int backKeyCount) {
            this.mPackageName = packageName;
            this.mWindowTitle = windowTitle;
            this.mBackKeyCount = backKeyCount;
        }

        public String toString() {
            return "ForceBackWindowInfo{ packageName = " + this.mPackageName + ",  title = " + this.mWindowTitle + ",  backKeyCount = " + this.mBackKeyCount + "}";
        }
    }
}