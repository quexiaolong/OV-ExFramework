package com.android.server.wm;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Path;
import android.graphics.PointF;
import android.media.AudioManager;
import android.net.Uri;
import android.os.FileObserver;
import android.os.FtBuild;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.FtFeature;
import android.util.Xml;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.server.UnifiedConfigThread;
import com.android.server.policy.VivoRatioControllerUtilsImpl;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import com.vivo.services.superresolution.Constant;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class ThirdPartyIncomingManager {
    private static final String ACTION_UNIFIED_CONFIG_UPDATE = "com.vivo.daemonService.unifiedconfig.update_finish_broadcast_ThirdPartyIncomingPolicy";
    private static final String ATTR_HOLD_POLICY = "HOLD";
    private static final String ATTR_IGNORE_POLICY = "IGNORE";
    private static final String ATTR_ITEM_ACT = "act";
    private static final String ATTR_ITEM_CLS = "cls";
    private static final String ATTR_ITEM_PKG = "pkg";
    private static final String ATTR_ITEM_THIRD_PARTY_INCOMING_POLICY = "policy";
    private static final String ATTR_NOTICE_POLICY = "NOTICE";
    private static final String BACKUP_THIRD_PARTY_INCOMING_POLICY_FILE_PATH = "/data/bbkcore/third_party_incoming_policy.xml";
    private static final String DEFAULT_WINDOW_POLICY_FILE_PATH = "/system/etc/third_party_incoming_policy.xml";
    private static final String DEFAULT_WINDOW_POLICY_FILE_PATH_DOMESTIC = "/system/etc/third_party_incoming_policy_domestic.xml";
    private static final long DUPLICATE_INCOMING_TIME = 3000;
    private static final int REMOVE_FLOAT_WINDOW = 2;
    private static final int SHOW_FLOAT_WINDOW = 1;
    public static final String TAG = "ThirdPartyIncoming";
    private static final String TAG_ITEM = "item";
    private static final String TAG_ITEM_EX = "item_ex";
    private static final int UPDATE_FLOAT_WINDOW = 3;
    private static final String URI = "content://com.vivo.abe.unifiedconfig.provider/configs";
    private FloatWindow floatWindow;
    private ActivityTaskManagerService mActivityTaskManagerService;
    private Context mContext;
    private FileObserver mFileObserver;
    private ThirdPartyIncomingHandler mHandler;
    private boolean mIsProductSupport;
    private long mLastShowWindowTime;
    private int mPendingStartUserId;
    private ThirdPartyIncomingObserver mThirdPartyIncomingObserver;
    private Timer timer;
    private TimerTask timerTask;
    private static ArrayList<ComponentName> sNoticeList = new ArrayList<>();
    private static ArrayList<String> sNoticeActList = new ArrayList<>();
    private static ArrayList<ComponentName> sIgnoreList = new ArrayList<>();
    private static ArrayList<ComponentName> sHoldList = new ArrayList<>();
    private final boolean DBG = SystemProperties.getBoolean("persist.vivopolicy.debug", false);
    private final String WEIXIN = Constant.APP_WEIXIN;
    private final String QQ = "com.tencent.mobileqq";
    private final String LINE = "jp.naver.line.android";
    private final String WHATSAPP = "com.whatsapp";
    private final String MESSENGER = "com.facebook.orca";
    private Handler mIoHandler = UnifiedConfigThread.getHandler();
    private ArrayList<Intent> mPendingStartIntents = new ArrayList<>();
    private boolean mIsOverseasProduct = SystemProperties.get("ro.vivo.product.overseas", "no").equals("yes");
    private boolean mIsInGame = false;
    private boolean mIsFullScreen = false;
    private boolean mIsInDriveGuide = false;
    private boolean mIsInDriveMode = false;
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() { // from class: com.android.server.wm.ThirdPartyIncomingManager.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.CONFIGURATION_CHANGED".equals(action)) {
                ThirdPartyIncomingManager.this.updateFloatWindow();
            } else if (ThirdPartyIncomingManager.ACTION_UNIFIED_CONFIG_UPDATE.equals(action)) {
                ThirdPartyIncomingManager.this.postRetriveFile();
            }
        }
    };
    private Runnable retriveFileRunnable = new Runnable() { // from class: com.android.server.wm.ThirdPartyIncomingManager.2
        @Override // java.lang.Runnable
        public void run() {
            if (ThirdPartyIncomingManager.this.DBG) {
                VSlog.d(ThirdPartyIncomingManager.TAG, "DEBUG_THIRD_PARTY_INCOMING_POLICY:retriveFileRunnable!");
            }
            File serverFile = new File(ThirdPartyIncomingManager.BACKUP_THIRD_PARTY_INCOMING_POLICY_FILE_PATH);
            ThirdPartyIncomingManager.this.readXmlFileFromUnifiedConfig("ThirdPartyIncomingPolicy", "1", "1.0", "ThirdPartyIncomingPolicy", serverFile);
        }
    };
    private Runnable reObserverListRunnable = new Runnable() { // from class: com.android.server.wm.ThirdPartyIncomingManager.3
        @Override // java.lang.Runnable
        public void run() {
            if (ThirdPartyIncomingManager.this.DBG) {
                VSlog.d(ThirdPartyIncomingManager.TAG, "DEBUG_THIRD_PARTY_INCOMING_POLICY:reObserverListRunnable ");
            }
            ThirdPartyIncomingManager.this.readList(ThirdPartyIncomingManager.BACKUP_THIRD_PARTY_INCOMING_POLICY_FILE_PATH);
            ThirdPartyIncomingManager.this.observeFile();
        }
    };

    /* loaded from: classes.dex */
    public enum SwipeDirection {
        NULL,
        X,
        Y
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class ThirdPartyIncomingObserver extends ContentObserver {
        private final Uri isFullscreenUri;
        private final Uri isGameStateUri;
        private final Uri isInDriveGuideUri;
        private final Uri isInDriveModeUri;

        public ThirdPartyIncomingObserver() {
            super(ThirdPartyIncomingManager.this.mHandler);
            this.isGameStateUri = Settings.System.getUriFor("is_game_mode");
            this.isFullscreenUri = Settings.System.getUriFor("vivo_fullscreen_flag");
            this.isInDriveGuideUri = Settings.System.getUriFor("drive_mode_ringing_intercepted");
            this.isInDriveModeUri = Settings.System.getUriFor("drive_mode_enabled");
            ContentResolver resolver = ThirdPartyIncomingManager.this.mContext.getContentResolver();
            resolver.registerContentObserver(this.isGameStateUri, false, this, -1);
            resolver.registerContentObserver(this.isFullscreenUri, false, this, -1);
            resolver.registerContentObserver(this.isInDriveGuideUri, false, this, -1);
            resolver.registerContentObserver(this.isInDriveModeUri, false, this, -1);
            ThirdPartyIncomingManager.this.mIsInGame = "1".equals(Settings.System.getString(ThirdPartyIncomingManager.this.mContext.getContentResolver(), "is_game_mode"));
            ThirdPartyIncomingManager.this.mIsFullScreen = Settings.System.getInt(ThirdPartyIncomingManager.this.mContext.getContentResolver(), "vivo_fullscreen_flag", 0) == 1;
            ThirdPartyIncomingManager.this.mIsInDriveGuide = "1".equals(Settings.System.getString(ThirdPartyIncomingManager.this.mContext.getContentResolver(), "drive_mode_ringing_intercepted"));
            ThirdPartyIncomingManager.this.mIsInDriveMode = "1".equals(Settings.System.getString(ThirdPartyIncomingManager.this.mContext.getContentResolver(), "drive_mode_enabled"));
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri, int userId) {
            if (this.isGameStateUri.equals(uri)) {
                ThirdPartyIncomingManager thirdPartyIncomingManager = ThirdPartyIncomingManager.this;
                thirdPartyIncomingManager.mIsInGame = "1".equals(Settings.System.getString(thirdPartyIncomingManager.mContext.getContentResolver(), "is_game_mode"));
            } else if (this.isFullscreenUri.equals(uri)) {
                ThirdPartyIncomingManager thirdPartyIncomingManager2 = ThirdPartyIncomingManager.this;
                thirdPartyIncomingManager2.mIsFullScreen = Settings.System.getInt(thirdPartyIncomingManager2.mContext.getContentResolver(), "vivo_fullscreen_flag", 0) == 1;
                ThirdPartyIncomingManager.this.updateFloatWindow();
            } else if (this.isInDriveGuideUri.equals(uri)) {
                ThirdPartyIncomingManager thirdPartyIncomingManager3 = ThirdPartyIncomingManager.this;
                thirdPartyIncomingManager3.mIsInDriveGuide = "1".equals(Settings.System.getString(thirdPartyIncomingManager3.mContext.getContentResolver(), "drive_mode_ringing_intercepted"));
            } else if (this.isInDriveModeUri.equals(uri)) {
                ThirdPartyIncomingManager thirdPartyIncomingManager4 = ThirdPartyIncomingManager.this;
                thirdPartyIncomingManager4.mIsInDriveMode = "1".equals(Settings.System.getString(thirdPartyIncomingManager4.mContext.getContentResolver(), "drive_mode_enabled"));
            }
        }
    }

    public ThirdPartyIncomingManager(ActivityTaskManagerService atms) {
        boolean z = false;
        this.mActivityTaskManagerService = atms;
        this.mContext = atms.mContext;
        if (FtFeature.isFeatureSupport("vivo.software.tpincomingfloat") && !"vos".equals(FtBuild.getOsName())) {
            z = true;
        }
        this.mIsProductSupport = z;
        VSlog.d(TAG, "DEBUG_THIRD_PARTY_INCOMING_POLICY: support = " + this.mIsProductSupport);
    }

    private void init() {
        VSlog.d(TAG, "DEBUG_THIRD_PARTY_INCOMING_POLICY:init");
        sNoticeList.clear();
        sNoticeActList.clear();
        sIgnoreList.clear();
        sHoldList.clear();
        boolean backupRes = readList(BACKUP_THIRD_PARTY_INCOMING_POLICY_FILE_PATH);
        if (!backupRes) {
            if (this.mIsOverseasProduct) {
                readList(DEFAULT_WINDOW_POLICY_FILE_PATH);
            } else {
                readList(DEFAULT_WINDOW_POLICY_FILE_PATH_DOMESTIC);
            }
        }
        if (this.DBG) {
            VSlog.d(TAG, "DEBUG_THIRD_PARTY_INCOMING_POLICY: backupRes=" + backupRes);
        }
        observeFile();
    }

    public void systemReady(Looper looper) {
        if (!this.mIsProductSupport) {
            return;
        }
        VSlog.d(TAG, "DEBUG_THIRD_PARTY_INCOMING_POLICY: systemReady");
        this.mHandler = new ThirdPartyIncomingHandler(looper);
        this.floatWindow = new FloatWindow();
        init();
        postRetriveFile();
        this.mThirdPartyIncomingObserver = new ThirdPartyIncomingObserver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        filter.addAction(ACTION_UNIFIED_CONFIG_UPDATE);
        this.mContext.registerReceiver(this.mIntentReceiver, filter);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void postRetriveFile() {
        if (this.DBG) {
            VSlog.d(TAG, "DEBUG_THIRD_PARTY_INCOMING_POLICY:postRetriveFile");
        }
        this.mIoHandler.removeCallbacks(this.retriveFileRunnable);
        this.mIoHandler.postDelayed(this.retriveFileRunnable, 10000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean readList(String srcPath) {
        boolean res = false;
        synchronized (sNoticeList) {
            if (this.DBG) {
                VSlog.d(TAG, "DEBUG_THIRD_PARTY_INCOMING_POLICY: start readList  from " + srcPath + " before sNoticeList=" + sNoticeList + " sNoticeActList=" + sNoticeActList + " sIgnoreList=" + sIgnoreList + " sHoldList=" + sHoldList);
            }
            try {
                File file = new File(srcPath);
                String result = readByBufferedReader(file);
                if (result != null) {
                    res = readXmlFile(new ByteArrayInputStream(result.getBytes()));
                }
            } catch (Exception e) {
                VSlog.e(TAG, "read list error! " + e.fillInStackTrace());
            }
            if (this.DBG) {
                VSlog.d(TAG, "DEBUG_THIRD_PARTY_INCOMING_POLICY: after readList sNoticeList=" + sNoticeList + " sNoticeActList=" + sNoticeActList + " sIgnoreList=" + sIgnoreList + " sHoldList=" + sHoldList);
            }
        }
        return res;
    }

    private String readByBufferedReader(File file) {
        if (file == null || !file.exists()) {
            return null;
        }
        BufferedReader bReader = null;
        StringBuffer buffer = null;
        try {
            try {
                try {
                    bReader = new BufferedReader(new FileReader(file));
                    while (true) {
                        String line = bReader.readLine();
                        if (line == null) {
                            break;
                        }
                        if (buffer == null) {
                            buffer = new StringBuffer();
                        }
                        if (buffer != null) {
                            buffer.append(line);
                            buffer.append("\n");
                        }
                    }
                    bReader.close();
                } catch (Throwable th) {
                    if (bReader != null) {
                        try {
                            bReader.close();
                        } catch (Exception e) {
                        }
                    }
                    throw th;
                }
            } catch (Exception e2) {
            }
        } catch (Exception e3) {
            VSlog.e(TAG, "Buffered Reader failed! " + e3.fillInStackTrace());
            if (bReader != null) {
                bReader.close();
            }
        }
        if (buffer != null) {
            return buffer.toString();
        }
        return null;
    }

    private boolean readXmlFile(InputStream inputStream) {
        if (this.DBG) {
            VSlog.d(TAG, "DEBUG_THIRD_PARTY_INCOMING_POLICY:readXmlFile FROM " + inputStream);
        }
        boolean result = true;
        sNoticeList.clear();
        sNoticeActList.clear();
        sIgnoreList.clear();
        sHoldList.clear();
        try {
            try {
                try {
                    try {
                        XmlPullParser parser = Xml.newPullParser();
                        parser.setInput(inputStream, null);
                        parser.getName();
                        for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                            String tag = parser.getName();
                            if (eventType == 2 && ((!this.mIsOverseasProduct && tag.equals(TAG_ITEM)) || (this.mIsOverseasProduct && tag.equals(TAG_ITEM_EX)))) {
                                String itemPkg = parser.getAttributeValue(null, ATTR_ITEM_PKG);
                                String itemCls = parser.getAttributeValue(null, ATTR_ITEM_CLS);
                                String itemAct = parser.getAttributeValue(null, ATTR_ITEM_ACT);
                                String policy = parser.getAttributeValue(null, ATTR_ITEM_THIRD_PARTY_INCOMING_POLICY);
                                if (this.DBG) {
                                    VSlog.d(TAG, "DEBUG_THIRD_PARTY_INCOMING_POLICY:readXmlFile applySettings itemPkg =" + itemPkg + " itemCls=" + itemCls + " policy=" + policy);
                                }
                                if (itemPkg != null && !itemPkg.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK) && itemCls != null && !itemCls.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK) && policy != null && !policy.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK)) {
                                    if (policy.equals(ATTR_NOTICE_POLICY)) {
                                        sNoticeList.add(new ComponentName(itemPkg, itemCls));
                                    } else if (policy.equals(ATTR_IGNORE_POLICY)) {
                                        sIgnoreList.add(new ComponentName(itemPkg, itemCls));
                                    } else if (policy.equals(ATTR_HOLD_POLICY)) {
                                        sHoldList.add(new ComponentName(itemPkg, itemCls));
                                    }
                                }
                                if (itemPkg != null && !itemPkg.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK) && itemAct != null && !itemAct.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK) && policy != null && !policy.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK) && policy.equals(ATTR_NOTICE_POLICY)) {
                                    sNoticeActList.add(itemAct);
                                }
                            }
                        }
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    }
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                    result = false;
                    if (inputStream != null) {
                        inputStream.close();
                    }
                }
            } catch (Exception e2) {
            }
            return result;
        } catch (Throwable th) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e3) {
                }
            }
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void readXmlFileFromUnifiedConfig(String module, String type, String version, String identifier, File file) {
        Cursor cursor;
        ContentResolver resolver = this.mContext.getContentResolver();
        String[] selectionArgs = {module, type, version, identifier};
        Cursor cursor2 = null;
        byte[] filecontent = null;
        try {
            try {
                try {
                    cursor = resolver.query(Uri.parse("content://com.vivo.abe.unifiedconfig.provider/configs"), null, null, selectionArgs, null);
                    if (cursor != null) {
                        cursor.moveToFirst();
                        if (cursor.getCount() > 0) {
                            while (!cursor.isAfterLast()) {
                                filecontent = cursor.getBlob(cursor.getColumnIndex("filecontent"));
                                cursor.moveToNext();
                                if (this.DBG) {
                                    VSlog.d(TAG, "DEBUG_THIRD_PARTY_INCOMING_POLICY:content = " + new String(filecontent));
                                }
                            }
                        } else if (this.DBG) {
                            VSlog.d(TAG, "DEBUG_THIRD_PARTY_INCOMING_POLICY:no data!");
                        }
                    }
                } catch (Exception e) {
                }
            } catch (Exception e2) {
                VSlog.e(TAG, "DEBUG_THIRD_PARTY_INCOMING_POLICY:open database error! " + e2.fillInStackTrace());
                if (0 != 0) {
                    cursor2.close();
                }
            }
            if (cursor != null) {
                cursor.close();
            }
            if (filecontent != null) {
                String result = new String(filecontent);
                writeByBufferedWriter(result, file);
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    cursor2.close();
                } catch (Exception e3) {
                }
            }
            throw th;
        }
    }

    private void writeByBufferedWriter(String string, File desFile) {
        if (string == null) {
            return;
        }
        synchronized (sNoticeList) {
            BufferedWriter bWriter = null;
            try {
                try {
                    if (desFile.exists() && desFile.isFile()) {
                        desFile.delete();
                    }
                    if (this.DBG) {
                        VSlog.d(TAG, "DEBUG_THIRD_PARTY_INCOMING_POLICY:writeByBufferedWriter createNewFile");
                    }
                    desFile.createNewFile();
                    bWriter = new BufferedWriter(new FileWriter(desFile));
                    bWriter.write(string);
                    if (this.DBG) {
                        VSlog.d(TAG, "DEBUG_THIRD_PARTY_INCOMING_POLICY:writeByBufferedWriter: " + string);
                    }
                    bWriter.close();
                } catch (Exception e) {
                    if (this.DBG) {
                        VSlog.e(TAG, "DEBUG_THIRD_PARTY_INCOMING_POLICY:Buffered write failed! " + e.fillInStackTrace());
                    }
                    if (bWriter != null) {
                        bWriter.close();
                    }
                }
            } catch (Exception e2) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void observeFile() {
        if (this.DBG) {
            VSlog.d(TAG, "DEBUG_THIRD_PARTY_INCOMING_POLICY:observeShieldListServer set");
        }
        FileObserver fileObserver = this.mFileObserver;
        if (fileObserver != null) {
            fileObserver.stopWatching();
        }
        if (this.DBG) {
            VSlog.d(TAG, "DEBUG_THIRD_PARTY_INCOMING_POLICY:observeShieldListServer set fileToObserve:/data/bbkcore/third_party_incoming_policy.xml");
        }
        File file = new File(BACKUP_THIRD_PARTY_INCOMING_POLICY_FILE_PATH);
        try {
            if (!file.exists()) {
                if (this.DBG) {
                    VSlog.d(TAG, "DEBUG_THIRD_PARTY_INCOMING_POLICY:observeShieldListServer file not exist ,create new one");
                }
                file.createNewFile();
            }
        } catch (Exception e) {
            if (this.DBG) {
                VSlog.e(TAG, "DEBUG_THIRD_PARTY_INCOMING_POLICY:create file error");
            }
        }
        FileObserver fileObserver2 = new FileObserver(BACKUP_THIRD_PARTY_INCOMING_POLICY_FILE_PATH) { // from class: com.android.server.wm.ThirdPartyIncomingManager.4
            @Override // android.os.FileObserver
            public void onEvent(int event, String path) {
                if (ThirdPartyIncomingManager.this.DBG) {
                    VSlog.d(ThirdPartyIncomingManager.TAG, "DEBUG_THIRD_PARTY_INCOMING_POLICY:observeShieldListServer fired event=" + event + " path=" + path);
                }
                if (8 == event) {
                    ThirdPartyIncomingManager.this.readList(ThirdPartyIncomingManager.BACKUP_THIRD_PARTY_INCOMING_POLICY_FILE_PATH);
                }
                if (event == 1024 || event == 512) {
                    if (ThirdPartyIncomingManager.this.DBG) {
                        VSlog.d(ThirdPartyIncomingManager.TAG, "DEBUG_THIRD_PARTY_INCOMING_POLICY:observeShieldListServer file deleted");
                    }
                    ThirdPartyIncomingManager.this.mIoHandler.removeCallbacks(ThirdPartyIncomingManager.this.reObserverListRunnable);
                    ThirdPartyIncomingManager.this.mIoHandler.postDelayed(ThirdPartyIncomingManager.this.reObserverListRunnable, 2000L);
                }
            }
        };
        this.mFileObserver = fileObserver2;
        fileObserver2.startWatching();
    }

    private boolean isKeyGuardLocked() {
        KeyguardManager mKeyguardManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
        if (mKeyguardManager == null) {
            return false;
        }
        boolean isKeyGuardLocked = mKeyguardManager.isKeyguardLocked();
        return isKeyGuardLocked;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isSilentOrAntiDisturbMode() {
        AudioManager mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        NotificationManager mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        try {
            boolean isSilent = mAudioManager.getRingerMode() == 0;
            boolean isAntiDisturbMode = mNotificationManager.getZenMode() != 0;
            return isSilent || isAntiDisturbMode;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isCallWhenVisible(String pkgName) {
        DisplayContent display = this.mActivityTaskManagerService.mWindowManager.mRoot.getDisplayContent(this.mActivityTaskManagerService.getFocusedDisplayId());
        if (display == null) {
            return false;
        }
        DisplayAreaPolicy displayAreaPolicy = display.mDisplayAreaPolicy;
        for (int i = displayAreaPolicy.getTaskDisplayAreaCount() - 1; i >= 0; i--) {
            TaskDisplayArea taskDisplayArea = displayAreaPolicy.getTaskDisplayAreaAt(i);
            for (int j = taskDisplayArea.getStackCount() - 1; j >= 0; j--) {
                ActivityStack activityStack = taskDisplayArea.getStackAt(j);
                ActivityRecord top = activityStack.getTopVisibleActivity();
                if (top != null && pkgName != null && pkgName.equals(top.packageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean shouldStartIncoming(Intent intent, int userId, ActivityRecord sourceRecord) {
        if (this.mIsProductSupport && intent.getComponent() != null) {
            if (intent.getIsForceStart()) {
                VSlog.d(TAG, "shouldStartIncoming, force start : " + intent);
                return true;
            }
            boolean isSpecial = false;
            boolean shouldIgnore = false;
            boolean shouldHold = false;
            int i = 0;
            while (true) {
                if (i >= sNoticeList.size()) {
                    break;
                } else if (!sNoticeList.get(i).equals(intent.getComponent())) {
                    i++;
                } else {
                    isSpecial = true;
                    break;
                }
            }
            if (!isSpecial) {
                int i2 = 0;
                while (true) {
                    if (i2 >= sNoticeActList.size()) {
                        break;
                    } else if (!sNoticeActList.get(i2).equals(intent.getAction())) {
                        i2++;
                    } else {
                        isSpecial = true;
                        break;
                    }
                }
            }
            if (!isSpecial) {
                int i3 = 0;
                while (true) {
                    if (i3 >= sIgnoreList.size()) {
                        break;
                    } else if (!sIgnoreList.get(i3).equals(intent.getComponent())) {
                        i3++;
                    } else {
                        isSpecial = true;
                        shouldIgnore = true;
                        break;
                    }
                }
            }
            if (!isSpecial) {
                int i4 = 0;
                while (true) {
                    if (i4 >= sHoldList.size()) {
                        break;
                    } else if (!sHoldList.get(i4).equals(intent.getComponent())) {
                        i4++;
                    } else {
                        isSpecial = true;
                        shouldHold = true;
                        break;
                    }
                }
            }
            if (isSpecial) {
                VSlog.d(TAG, "shouldStartIncoming, special intent : " + intent);
                if (isCallWhenVisible(intent.getComponent().getPackageName())) {
                    VSlog.d(TAG, "start as caller is visible");
                    return true;
                }
                if ((this.mActivityTaskManagerService.isGameModeOpen() && this.mIsInGame) || (this.mIsInDriveMode && this.mIsInDriveGuide)) {
                    if (!isKeyGuardLocked()) {
                        if (shouldIgnore) {
                            VSlog.d(TAG, "ignore one: " + intent);
                            return false;
                        } else if (shouldHold) {
                            this.mPendingStartIntents.add(intent);
                            VSlog.d(TAG, "add hold activity in pendingSatrt " + intent);
                            return false;
                        } else {
                            boolean shouldUpdateWindow = true;
                            long curIncomingTime = SystemClock.currentNetworkTimeMillis();
                            VSlog.d(TAG, "curIncomingTime = " + curIncomingTime);
                            if (!this.mPendingStartIntents.isEmpty() && curIncomingTime - this.mLastShowWindowTime <= DUPLICATE_INCOMING_TIME) {
                                Intent oldIntent = this.mPendingStartIntents.get(0);
                                if (intent.getComponent().equals(oldIntent.getComponent())) {
                                    VSlog.d(TAG, "should not update float window");
                                    shouldUpdateWindow = false;
                                }
                            }
                            this.mPendingStartIntents.clear();
                            this.mPendingStartIntents.add(intent);
                            this.mPendingStartUserId = userId;
                            if (shouldUpdateWindow) {
                                this.mLastShowWindowTime = curIncomingTime;
                                this.mHandler.removeMessages(1);
                                ThirdPartyIncomingHandler thirdPartyIncomingHandler = this.mHandler;
                                thirdPartyIncomingHandler.sendMessage(thirdPartyIncomingHandler.obtainMessage(1, new Intent(intent)));
                                VSlog.d(TAG, "show float window for third-party incoming: " + intent);
                            }
                            return false;
                        }
                    }
                }
                return true;
            }
            return true;
        }
        return true;
    }

    void startPendingStartActivity() {
        VSlog.d(TAG, "startPendingStartActivity list " + this.mPendingStartIntents.toString());
        for (int i = 0; i < this.mPendingStartIntents.size(); i++) {
            Intent ti = this.mPendingStartIntents.get(i);
            if (ti == null) {
                VSlog.w(TAG, "pending start Intent is null");
            } else {
                VSlog.d(TAG, "startPendingStartActivity " + ti + " userId=" + this.mPendingStartUserId);
                ti.setForceStart(true);
                this.mContext.startActivityAsUser(ti, new UserHandle(this.mPendingStartUserId));
            }
        }
        this.mPendingStartIntents.clear();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateFloatWindow() {
        this.mHandler.removeMessages(3);
        this.mHandler.sendEmptyMessage(3);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class ThirdPartyIncomingHandler extends Handler {
        public ThirdPartyIncomingHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            int i = message.what;
            if (i == 1) {
                Intent intent = (Intent) message.obj;
                ThirdPartyIncomingManager.this.floatWindow.addView(intent);
            } else if (i == 2) {
                ThirdPartyIncomingManager.this.floatWindow.removeView(false, false);
            } else if (i != 3) {
            } else {
                ThirdPartyIncomingManager.this.floatWindow.updateWindowParams();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class FloatWindow implements View.OnClickListener, View.OnTouchListener {
        private static final float ADD_ANIM_FLOAT_HEIGHT_VLAUE = 0.15686f;
        private static final int FLOAT_WINDOW_SHOW_TIME = 60000;
        private static final float MIN_SWIP_DISTANCE = 10.0f;
        private static final float SWIPE_PROGRESS_FADE_END = 0.5f;
        private static final int TOP_OFFSET = 20;
        private static final int WIDTH_OFFSET = 100;
        private Intent addAfterRemoveIntent;
        private boolean addAfterRemoveView;
        ImageView answer;
        TextView callName;
        ImageView cancel;
        DisplayMetrics dm;
        WindowManager.LayoutParams layoutParams;
        private ValueAnimator mBackAnimator;
        private float mDownPosX;
        private float mDownPosY;
        private AnimatorSet mEnterAnimator;
        private ValueAnimator mExitAnimator;
        int mHoleRadius;
        int mHoleX;
        int mHoleY;
        boolean mIsHoleScreen;
        private boolean mIsWindowAdding;
        private boolean mIsWindowBacking;
        private boolean mIsWindowRemoving;
        WindowManager mWM;
        private int mWindowHeight;
        TextView notice;
        View rootView;
        Vibrator vibrator;
        private float mVelocity = -1.0f;
        private int MAX_DISMISS_VELOCITY = 200;
        private float SWIPE_ESCAPE_VELOCITY = 3.0f;
        private int DEFAULT_ESCAPE_ANIMATION_DURATION = 150;
        private SwipeDirection mSwipeDirection = SwipeDirection.NULL;
        private int mWidthPixels = -1;
        private int mHeightPixels = -1;
        private int mScreenWidth = -1;
        private boolean mIsAddToWindow = false;
        long[] pattern = {1000, 1000, 1000, 1000};
        int mStatusBarHeight = -1;
        int mEarHeight = -1;
        boolean mHasInitHoleConfig = false;
        private VelocityTracker mVelocityTracker = VelocityTracker.obtain();
        private VivoRatioControllerUtilsImpl mVivoRatioControllerUtils = VivoRatioControllerUtilsImpl.getInstance();

        FloatWindow() {
        }

        private Vibrator getVibrator() {
            return (Vibrator) ThirdPartyIncomingManager.this.mContext.getSystemService("vibrator");
        }

        private WindowManager getmWM() {
            return (WindowManager) ThirdPartyIncomingManager.this.mContext.getSystemService("window");
        }

        private boolean isScreenOrientationPortrait() {
            return ThirdPartyIncomingManager.this.mContext.getResources().getConfiguration().orientation == 1;
        }

        private int getEarWithStatusBarHeight() {
            int top;
            if (this.mStatusBarHeight == -1) {
                try {
                    this.mStatusBarHeight = ThirdPartyIncomingManager.this.mContext.getResources().getDimensionPixelSize(17105488);
                } catch (Exception e) {
                    VLog.e(ThirdPartyIncomingManager.TAG, "get status bar height error");
                    this.mStatusBarHeight = -1;
                }
            }
            if (this.mEarHeight == -1) {
                if (FtFeature.isFeatureSupport(32)) {
                    this.mEarHeight = this.mVivoRatioControllerUtils.alienScreenCoverInsetTop;
                } else {
                    this.mEarHeight = 0;
                }
            }
            if (!this.mHasInitHoleConfig) {
                this.mIsHoleScreen = FtFeature.isFeatureSupport("vivo.hardware.holescreen");
                this.mHoleX = Integer.parseInt(FtFeature.getFeatureAttribute("vivo.hardware.holescreen", "hole_x", "0"));
                this.mHoleY = Integer.parseInt(FtFeature.getFeatureAttribute("vivo.hardware.holescreen", "hole_y", "0"));
                this.mHoleRadius = Integer.parseInt(FtFeature.getFeatureAttribute("vivo.hardware.holescreen", "hole_radius", "0"));
                this.mHasInitHoleConfig = true;
            }
            boolean isFullScreen = ThirdPartyIncomingManager.this.mIsFullScreen;
            boolean isPortrait = isScreenOrientationPortrait();
            if (ThirdPartyIncomingManager.this.DBG) {
                VLog.i(ThirdPartyIncomingManager.TAG, "getEarWithStatusBarHeight mStatusBarHeight = " + this.mStatusBarHeight + " mEarHeight =" + this.mEarHeight + " mHoleX = " + this.mHoleX + " mHoleY = " + this.mHoleY + " mHoleRadius = " + this.mHoleRadius + " isFullScreen = " + isFullScreen + " isProtrait =" + isPortrait);
            }
            if (isPortrait) {
                top = this.mStatusBarHeight;
            } else {
                top = isFullScreen ? 0 : this.mStatusBarHeight;
            }
            return top + 20;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void updateWindowParams() {
            if (this.rootView != null && this.layoutParams != null) {
                int newHeight = getEarWithStatusBarHeight();
                if (ThirdPartyIncomingManager.this.DBG) {
                    VLog.i(ThirdPartyIncomingManager.TAG, "updateWindowParams get y = " + newHeight);
                }
                if (this.layoutParams.y != newHeight) {
                    this.layoutParams.y = newHeight;
                    this.mWM.updateViewLayout(this.rootView, this.layoutParams);
                }
            }
        }

        private void updateView(Intent intent) {
            if (this.rootView == null) {
                View inflate = LayoutInflater.from(ThirdPartyIncomingManager.this.mContext).inflate(50528424, (ViewGroup) null);
                this.rootView = inflate;
                this.cancel = (ImageView) inflate.findViewById(51183746);
                this.answer = (ImageView) this.rootView.findViewById(51183744);
                this.callName = (TextView) this.rootView.findViewById(51183656);
                this.notice = (TextView) this.rootView.findViewById(51183745);
                this.cancel.setOnClickListener(this);
                this.answer.setOnClickListener(this);
                this.rootView.setOnTouchListener(this);
            }
            if (intent.getComponent().getPackageName().equals("com.tencent.mobileqq")) {
                this.callName.setText(51249624);
                this.notice.setText(51249629);
            } else if (intent.getComponent().getPackageName().equals(Constant.APP_WEIXIN)) {
                this.callName.setText(51249625);
                this.notice.setText(51249630);
            } else if (intent.getComponent().getPackageName().equals("jp.naver.line.android")) {
                this.callName.setText(51249622);
                this.notice.setText(51249627);
            } else if (intent.getComponent().getPackageName().equals("com.whatsapp")) {
                this.callName.setText(51249626);
                this.notice.setText(51249631);
            } else if (intent.getComponent().getPackageName().equals("com.facebook.orca")) {
                this.callName.setText(51249623);
                this.notice.setText(51249628);
            }
        }

        private void initWindowParams() {
            Display display;
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            this.layoutParams = layoutParams;
            layoutParams.type = 2003;
            this.layoutParams.format = 1;
            this.layoutParams.flags = 8389416;
            this.layoutParams.privateFlags |= 16;
            this.mWindowHeight = ThirdPartyIncomingManager.this.mContext.getResources().getDimensionPixelSize(51118476);
            if (this.dm == null && (display = this.mWM.getDefaultDisplay()) != null) {
                DisplayMetrics displayMetrics = new DisplayMetrics();
                this.dm = displayMetrics;
                display.getMetrics(displayMetrics);
            }
            this.mWidthPixels = this.dm.widthPixels;
            int i = this.dm.heightPixels;
            this.mHeightPixels = i;
            int i2 = this.mWidthPixels;
            if (i > i2) {
                i = i2;
            }
            this.mScreenWidth = i;
            this.layoutParams.y = getEarWithStatusBarHeight();
            this.layoutParams.width = this.mScreenWidth - 100;
            this.layoutParams.height = this.mWindowHeight;
            this.layoutParams.gravity = 49;
        }

        void addView(Intent intent) {
            if (this.rootView != null) {
                this.addAfterRemoveView = true;
                this.addAfterRemoveIntent = intent;
                removeView(false, false);
                return;
            }
            if (this.mWM == null) {
                this.mWM = getmWM();
            }
            if (this.mWM != null) {
                updateView(intent);
                initWindowParams();
                if (this.layoutParams.width == 0 || this.layoutParams.height == 0) {
                    VLog.w(ThirdPartyIncomingManager.TAG, "Get wrong pixels from default display, give up adding window");
                    return;
                }
                this.mWM.addView(this.rootView, this.layoutParams);
                doAddAnimation();
                doVibrate();
                this.mIsAddToWindow = true;
                startTimer();
            }
        }

        void removeView(boolean isAnswer, boolean isAnimate) {
            if (this.rootView == null) {
                return;
            }
            stopTimer();
            if (this.vibrator == null) {
                this.vibrator = getVibrator();
            }
            Vibrator vibrator = this.vibrator;
            if (vibrator != null) {
                vibrator.cancel();
            }
            this.mIsAddToWindow = false;
            if (this.mIsWindowBacking) {
                this.mBackAnimator.cancel();
            }
            if (isAnimate) {
                doDispearAnimation(isAnswer);
            } else {
                tearDown(isAnswer);
            }
        }

        private void startTimer() {
            ThirdPartyIncomingManager.this.timer = new Timer();
            ThirdPartyIncomingManager.this.timerTask = new TimerTask() { // from class: com.android.server.wm.ThirdPartyIncomingManager.FloatWindow.1
                @Override // java.util.TimerTask, java.lang.Runnable
                public void run() {
                    ThirdPartyIncomingManager.this.mHandler.sendEmptyMessage(2);
                }
            };
            ThirdPartyIncomingManager.this.timer.schedule(ThirdPartyIncomingManager.this.timerTask, 60000L);
        }

        private void stopTimer() {
            if (ThirdPartyIncomingManager.this.timer != null) {
                ThirdPartyIncomingManager.this.timer.cancel();
                ThirdPartyIncomingManager.this.timer = null;
            }
            if (ThirdPartyIncomingManager.this.timerTask != null) {
                ThirdPartyIncomingManager.this.timerTask.cancel();
                ThirdPartyIncomingManager.this.timerTask = null;
            }
        }

        private boolean isSwipFarOrFastEnough() {
            boolean swipedFastEnough;
            float maxVelocity = this.MAX_DISMISS_VELOCITY * this.dm.densityDpi;
            float escapeVelocity = this.SWIPE_ESCAPE_VELOCITY * this.dm.densityDpi;
            this.mVelocityTracker.computeCurrentVelocity(1000, maxVelocity);
            this.mVelocity = getVelocity(this.mVelocityTracker);
            float perpendicularVelocity = getPerpendicularVelocity(this.mVelocityTracker);
            VLog.i(ThirdPartyIncomingManager.TAG, "mVelocity = " + this.mVelocity + "escapeVelocity:" + escapeVelocity);
            boolean swipedFarEnough = ((double) Math.abs(getWindowTranslation(this.layoutParams))) > ((double) getWindowSize(this.layoutParams)) * 0.4d;
            if (Math.abs(this.mVelocity) > escapeVelocity && Math.abs(this.mVelocity) > Math.abs(perpendicularVelocity)) {
                if ((this.mVelocity > 0.0f) == (getWindowTranslation(this.layoutParams) > 0.0f)) {
                    swipedFastEnough = true;
                    return swipedFastEnough || swipedFarEnough;
                }
            }
            swipedFastEnough = false;
            if (swipedFastEnough) {
                return true;
            }
        }

        private void doAddAnimation() {
            int from = -this.layoutParams.height;
            int middle = (int) (this.layoutParams.height * ADD_ANIM_FLOAT_HEIGHT_VLAUE);
            this.layoutParams.height += middle;
            this.mWM.updateViewLayout(this.rootView, this.layoutParams);
            ValueAnimator ani1 = ValueAnimator.ofInt(from, middle);
            Path bPath1 = VivoBezierUtil.buildPath(new PointF(0.1f, 0.0f), new PointF(0.1f, 1.0f));
            PathInterpolator interPolator1 = new PathInterpolator(bPath1);
            ani1.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.wm.ThirdPartyIncomingManager.FloatWindow.2
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public void onAnimationUpdate(ValueAnimator animation) {
                    if (FloatWindow.this.mIsAddToWindow) {
                        FloatWindow.this.rootView.setTranslationY(((Integer) animation.getAnimatedValue()).intValue());
                    }
                }
            });
            ani1.setInterpolator(interPolator1);
            ani1.setDuration(150L);
            ValueAnimator ani2 = ValueAnimator.ofInt(middle, 0);
            Path bPath2 = VivoBezierUtil.buildPath(new PointF(0.3f, 0.0f), new PointF(0.3f, 1.0f));
            PathInterpolator interPolator2 = new PathInterpolator(bPath2);
            ani2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.wm.ThirdPartyIncomingManager.FloatWindow.3
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public void onAnimationUpdate(ValueAnimator animation) {
                    if (FloatWindow.this.mIsAddToWindow) {
                        FloatWindow.this.rootView.setTranslationY(((Integer) animation.getAnimatedValue()).intValue());
                    }
                }
            });
            ani2.addListener(new AnimatorListenerAdapter() { // from class: com.android.server.wm.ThirdPartyIncomingManager.FloatWindow.4
                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animation) {
                    FloatWindow.this.mIsWindowAdding = false;
                }
            });
            ani2.setInterpolator(interPolator2);
            ani2.setDuration(250L);
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playSequentially(ani1, ani2);
            animatorSet.start();
            this.mEnterAnimator = animatorSet;
            this.mIsWindowAdding = true;
        }

        private void doDispearAnimation(boolean isAnswer) {
            final float startPos = getWindowTranslation(this.layoutParams);
            ValueAnimator ani = ValueAnimator.ofFloat(0.0f, 1.0f);
            Path bPath = isAnswer ? VivoBezierUtil.buildPath(new PointF(0.15f, 0.0f), new PointF(0.3f, 0.07f), new PointF(0.4f, 0.14f), new PointF(0.7f, 0.6f), new PointF(0.82f, 1.0f)) : VivoBezierUtil.buildPath(new PointF(0.2f, 0.0f), new PointF(0.33f, 0.04f), new PointF(0.47f, 0.14f), new PointF(0.8f, 0.67f), new PointF(0.87f, 1.0f));
            long duration = this.DEFAULT_ESCAPE_ANIMATION_DURATION;
            if (this.mVelocity != 0.0f) {
                duration = Math.min(duration, (int) ((Math.abs(getWindowSize(this.layoutParams) - getWindowTranslation(this.layoutParams)) * 1000.0f) / Math.abs(this.mVelocity)));
            }
            this.rootView.setLayerType(2, null);
            ani.addListener(new AnimatorListenerAdapter() { // from class: com.android.server.wm.ThirdPartyIncomingManager.FloatWindow.5
                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animation) {
                    if (!FloatWindow.this.mIsAddToWindow) {
                        FloatWindow.this.rootView.setVisibility(8);
                        FloatWindow.this.rootView.setLayerType(0, null);
                        FloatWindow.this.tearDown();
                    }
                }
            });
            ani.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.wm.ThirdPartyIncomingManager.FloatWindow.6
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public void onAnimationUpdate(ValueAnimator animation) {
                    if (!FloatWindow.this.mIsAddToWindow) {
                        float f = ((Float) animation.getAnimatedValue()).floatValue();
                        if (FloatWindow.this.mSwipeDirection != SwipeDirection.X) {
                            if (FloatWindow.this.mSwipeDirection == SwipeDirection.Y) {
                                View view = FloatWindow.this.rootView;
                                FloatWindow floatWindow = FloatWindow.this;
                                view.setTranslationY((-floatWindow.getViewSize(floatWindow.rootView)) * f);
                                return;
                            }
                            return;
                        }
                        FloatWindow floatWindow2 = FloatWindow.this;
                        if (floatWindow2.getWindowTranslation(floatWindow2.layoutParams) > 0.0f) {
                            WindowManager.LayoutParams layoutParams = FloatWindow.this.layoutParams;
                            float f2 = startPos;
                            layoutParams.x = (int) (((FloatWindow.this.layoutParams.width - f2) * f) + f2);
                        } else {
                            WindowManager.LayoutParams layoutParams2 = FloatWindow.this.layoutParams;
                            float f3 = startPos;
                            layoutParams2.x = (int) ((((-FloatWindow.this.layoutParams.width) - f3) * f) + f3);
                        }
                        View view2 = FloatWindow.this.rootView;
                        FloatWindow floatWindow3 = FloatWindow.this;
                        float windowFadeSize = floatWindow3.getWindowFadeSize(floatWindow3.layoutParams);
                        FloatWindow floatWindow4 = FloatWindow.this;
                        float abs = windowFadeSize - Math.abs(floatWindow4.getWindowTranslation(floatWindow4.layoutParams));
                        FloatWindow floatWindow5 = FloatWindow.this;
                        view2.setAlpha(abs / floatWindow5.getWindowFadeSize(floatWindow5.layoutParams));
                        FloatWindow.this.mWM.updateViewLayout(FloatWindow.this.rootView, FloatWindow.this.layoutParams);
                    }
                }
            });
            PathInterpolator interPolator = new PathInterpolator(bPath);
            ani.setInterpolator(interPolator);
            ani.setDuration(duration);
            ani.start();
            this.mExitAnimator = ani;
            this.mIsWindowRemoving = true;
        }

        private void doBackAnimation() {
            float[] fArr = new float[2];
            fArr[0] = this.mSwipeDirection == SwipeDirection.X ? this.layoutParams.x : 0.0f;
            fArr[1] = 0.0f;
            ValueAnimator ani = ValueAnimator.ofFloat(fArr);
            ani.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.wm.ThirdPartyIncomingManager.FloatWindow.7
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public void onAnimationUpdate(ValueAnimator animation) {
                    if (FloatWindow.this.mIsAddToWindow) {
                        float f = ((Float) animation.getAnimatedValue()).floatValue();
                        View view = FloatWindow.this.rootView;
                        FloatWindow floatWindow = FloatWindow.this;
                        float windowFadeSize = floatWindow.getWindowFadeSize(floatWindow.layoutParams) - Math.abs(f);
                        FloatWindow floatWindow2 = FloatWindow.this;
                        view.setAlpha(windowFadeSize / floatWindow2.getWindowFadeSize(floatWindow2.layoutParams));
                        FloatWindow.this.layoutParams.x = (int) f;
                        FloatWindow.this.mWM.updateViewLayout(FloatWindow.this.rootView, FloatWindow.this.layoutParams);
                    }
                }
            });
            ani.addListener(new AnimatorListenerAdapter() { // from class: com.android.server.wm.ThirdPartyIncomingManager.FloatWindow.8
                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animation) {
                    FloatWindow.this.mSwipeDirection = SwipeDirection.NULL;
                    FloatWindow.this.mIsWindowBacking = false;
                }
            });
            ani.setInterpolator(new DecelerateInterpolator());
            ani.setDuration(150L);
            ani.start();
            this.mBackAnimator = ani;
            this.mIsWindowBacking = true;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void tearDown() {
            tearDown(false);
        }

        private void tearDown(boolean isAnswer) {
            VLog.d(ThirdPartyIncomingManager.TAG, "tearDown");
            this.rootView.animate().setListener(null);
            try {
                this.mWM.removeView(this.rootView);
            } catch (Exception e) {
            }
            if (isAnswer) {
                this.rootView.post(new Runnable() { // from class: com.android.server.wm.ThirdPartyIncomingManager.FloatWindow.9
                    @Override // java.lang.Runnable
                    public void run() {
                        ThirdPartyIncomingManager.this.startPendingStartActivity();
                    }
                });
            }
            removeAnimations();
            this.mWM = null;
            this.rootView = null;
            this.mSwipeDirection = SwipeDirection.NULL;
            this.mIsWindowRemoving = false;
            if (this.addAfterRemoveView) {
                addView(this.addAfterRemoveIntent);
                this.addAfterRemoveView = false;
            }
        }

        private void removeAnimations() {
            AnimatorSet animatorSet = this.mEnterAnimator;
            if (animatorSet != null) {
                animatorSet.cancel();
            }
            ValueAnimator valueAnimator = this.mExitAnimator;
            if (valueAnimator != null) {
                valueAnimator.cancel();
            }
            ValueAnimator valueAnimator2 = this.mBackAnimator;
            if (valueAnimator2 != null) {
                valueAnimator2.cancel();
            }
        }

        private void doVibrate() {
            if (!ThirdPartyIncomingManager.this.isSilentOrAntiDisturbMode()) {
                if (this.vibrator == null) {
                    this.vibrator = getVibrator();
                }
                Vibrator vibrator = this.vibrator;
                if (vibrator != null) {
                    vibrator.vibrate(this.pattern, -1);
                }
            }
        }

        private float getPerpendicularVelocity(VelocityTracker vt) {
            return this.mSwipeDirection == SwipeDirection.X ? vt.getYVelocity() : vt.getXVelocity();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public float getWindowTranslation(WindowManager.LayoutParams wmParams) {
            return this.mSwipeDirection == SwipeDirection.X ? wmParams.x : wmParams.y;
        }

        private float getVelocity(VelocityTracker vt) {
            return this.mSwipeDirection == SwipeDirection.X ? vt.getXVelocity() : vt.getYVelocity();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public float getWindowFadeSize(WindowManager.LayoutParams wmParams) {
            return getWindowSize(wmParams) * SWIPE_PROGRESS_FADE_END;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public float getViewSize(View v) {
            return this.mSwipeDirection == SwipeDirection.X ? v.getMeasuredWidth() : v.getMeasuredHeight();
        }

        private float getWindowSize(WindowManager.LayoutParams wmParams) {
            return this.mSwipeDirection == SwipeDirection.X ? wmParams.width : wmParams.height;
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View v) {
            if (this.mIsWindowAdding || this.mIsWindowRemoving || this.mIsWindowBacking) {
                return;
            }
            int id = v.getId();
            if (id == 51183744) {
                removeView(true, false);
            } else if (id == 51183746) {
                removeView(false, false);
            }
        }

        /* JADX WARN: Code restructure failed: missing block: B:19:0x0027, code lost:
            if (r0 != 3) goto L19;
         */
        @Override // android.view.View.OnTouchListener
        /*
            Code decompiled incorrectly, please refer to instructions dump.
            To view partially-correct code enable 'Show inconsistent code' option in preferences
        */
        public boolean onTouch(android.view.View r13, android.view.MotionEvent r14) {
            /*
                Method dump skipped, instructions count: 249
                To view this dump change 'Code comments level' option to 'DEBUG'
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.ThirdPartyIncomingManager.FloatWindow.onTouch(android.view.View, android.view.MotionEvent):boolean");
        }
    }
}