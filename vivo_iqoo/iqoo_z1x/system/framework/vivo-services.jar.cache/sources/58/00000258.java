package com.android.server.input;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import com.android.server.UnifiedConfigThread;
import com.android.server.policy.VivoPolicyUtil;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.VivoMultiWindowConfig;
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
public class ThreeFingerConfigManager {
    private static final String ATTR_PACKAGE_NAME = "packagename";
    private static final String ATTR_TITLE = "title";
    private static final String CONFIG_IDENTIFIER_THREE_FINGER = "three_finger_config";
    private static final String CONFIG_MODULE_NAME_THREE_FINGER = "ThreeFingerGestureConfg";
    private static final String CONFIG_TYPE = "1";
    private static final String CONFIG_UPDATE_ACTION = "com.vivo.daemonService.unifiedconfig.update_finish_broadcast_ThreeFingerGestureConfg";
    private static final String CONFIG_URI = "content://com.vivo.abe.unifiedconfig.provider/configs";
    private static final String CONFIG_VERSION = "1.0";
    private static final String TAG = "ThreeFingerConfigManager";
    private static final String TAG_DISABLE_DURING_GAME = "disableduringgame";
    private static final String TAG_ITEM = "item";
    private static ThreeFingerConfigManager sInstance;
    private Context mContext;
    private final ArrayList<WindowInfo> mNotInterceptApps = new ArrayList<>();
    private boolean mDisableInterceptDuringGame = false;
    private boolean mIsGameMode = false;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() { // from class: com.android.server.input.ThreeFingerConfigManager.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (ThreeFingerConfigManager.CONFIG_UPDATE_ACTION.equals(intent.getAction())) {
                VLog.d(ThreeFingerConfigManager.TAG, "onReceive CONFIG_UPDATE_ACTION");
                ThreeFingerConfigManager.this.mHandler.post(ThreeFingerConfigManager.this.mReadConfigRunnable);
            }
        }
    };
    private Runnable mReadConfigRunnable = new Runnable() { // from class: com.android.server.input.ThreeFingerConfigManager.2
        @Override // java.lang.Runnable
        public void run() {
            ThreeFingerConfigManager.this.readConfig("content://com.vivo.abe.unifiedconfig.provider/configs", ThreeFingerConfigManager.CONFIG_MODULE_NAME_THREE_FINGER, "1");
        }
    };
    private Handler mHandler = UnifiedConfigThread.getHandler();

    public static ThreeFingerConfigManager getInstance(Context context) {
        ThreeFingerConfigManager threeFingerConfigManager;
        synchronized (ThreeFingerConfigManager.class) {
            if (sInstance == null) {
                sInstance = new ThreeFingerConfigManager(context);
            }
            threeFingerConfigManager = sInstance;
        }
        return threeFingerConfigManager;
    }

    public ThreeFingerConfigManager(Context context) {
        this.mContext = context;
    }

    public void systemReady() {
        GameModeObserver gameModeObserver = new GameModeObserver(this.mHandler);
        gameModeObserver.observe();
        registerBroadcast();
        this.mHandler.postDelayed(this.mReadConfigRunnable, 60000L);
    }

    public boolean shouldDisablePilferPointers(String callingPackage, WindowManagerPolicy.WindowState focusWindow) {
        if (VivoPolicyUtil.IS_LOG_OPEN) {
            VLog.d(TAG, "shouldDisablePilferPointers mIsGameMode = " + this.mIsGameMode + ", mDisableInterceptDuringGame = " + this.mDisableInterceptDuringGame + ", callingPackage = " + callingPackage + ", focusWindow = " + focusWindow);
        }
        if (VivoMultiWindowConfig.SMART_MULTIWINDOW_NAME.equals(callingPackage)) {
            return shouldDisablePilferPointers(focusWindow);
        }
        return false;
    }

    public boolean shouldDisablePilferPointers(WindowManagerPolicy.WindowState focusWindow) {
        if (focusWindow == null) {
            return false;
        }
        String focusPackage = focusWindow.getOwningPackage();
        String focusWindowTitle = focusWindow.getAttrs().getTitle().toString();
        if (this.mIsGameMode && this.mDisableInterceptDuringGame) {
            return true;
        }
        synchronized (this.mNotInterceptApps) {
            Iterator<WindowInfo> it = this.mNotInterceptApps.iterator();
            while (it.hasNext()) {
                WindowInfo windowInfo = it.next();
                if (windowInfo.mWindowTitle == null) {
                    if (windowInfo.mPackageName.equals(focusPackage)) {
                        return true;
                    }
                } else if (windowInfo.mPackageName.equals(focusPackage) && focusWindowTitle.contains(windowInfo.mWindowTitle)) {
                    return true;
                }
            }
            return false;
        }
    }

    private void registerBroadcast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(CONFIG_UPDATE_ACTION);
        this.mContext.registerReceiver(this.mReceiver, filter);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void readConfig(String uri, String moduleName, String type) {
        VLog.d(TAG, "readConfig");
        ContentResolver resolver = this.mContext.getContentResolver();
        String[] selectionArgs = {moduleName, type, "1.0"};
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
                        if (CONFIG_IDENTIFIER_THREE_FINGER.equals(targetIdentifier) && !TextUtils.isEmpty(contents)) {
                            parseConfig(contents);
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

    private void parseConfig(String contents) {
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
                    ArrayList<WindowInfo> notInterceptApps = new ArrayList<>();
                    for (int eventCode = parser.getEventType(); eventCode != 1; eventCode = parser.next()) {
                        if (eventCode == 2) {
                            String name = parser.getName();
                            if (TAG_ITEM.equals(name)) {
                                String packageName = parser.getAttributeValue(null, ATTR_PACKAGE_NAME);
                                String windowTitle = parser.getAttributeValue(null, ATTR_TITLE);
                                WindowInfo windowInfo = new WindowInfo(packageName, windowTitle);
                                VLog.d(TAG, "not intercept touch app = " + windowInfo);
                                notInterceptApps.add(windowInfo);
                            } else if (TAG_DISABLE_DURING_GAME.equals(name)) {
                                parser.next();
                                this.mDisableInterceptDuringGame = safeBoolean(parser.getText(), false);
                                VLog.d(TAG, "mDisableInterceptDuringGame = " + this.mDisableInterceptDuringGame);
                            }
                        }
                    }
                    synchronized (this.mNotInterceptApps) {
                        this.mNotInterceptApps.clear();
                        this.mNotInterceptApps.addAll(notInterceptApps);
                    }
                    inputStream.close();
                } catch (IOException | XmlPullParserException e) {
                    e.printStackTrace();
                    inputStream.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
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

    private static boolean safeBoolean(String val, boolean defValue) {
        return TextUtils.isEmpty(val) ? defValue : Boolean.parseBoolean(val);
    }

    /* loaded from: classes.dex */
    class GameModeObserver extends ContentObserver {
        public GameModeObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = ThreeFingerConfigManager.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor("is_game_mode"), false, this, -1);
            ThreeFingerConfigManager.this.updateGameMode();
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            ThreeFingerConfigManager.this.updateGameMode();
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
    public class WindowInfo {
        String mPackageName;
        String mWindowTitle;

        public WindowInfo(String packageName, String windowTitle) {
            this.mPackageName = packageName;
            this.mWindowTitle = windowTitle;
        }

        public String toString() {
            return "WindowInfo{ packageName = " + this.mPackageName + ",  title = " + this.mWindowTitle + "}";
        }
    }

    public void dump(String prefix, PrintWriter pw) {
        if (!VivoPolicyUtil.IS_LOG_OPEN) {
            return;
        }
        pw.println(prefix + TAG);
        String prefix2 = prefix + "  ";
        pw.println(prefix2 + "mIsGameMode = " + this.mIsGameMode);
        pw.println(prefix2 + "mDisableInterceptDuringGame = " + this.mDisableInterceptDuringGame);
        StringBuilder sb = new StringBuilder();
        sb.append(prefix2);
        sb.append("  ");
        String prefix3 = sb.toString();
        synchronized (this.mNotInterceptApps) {
            Iterator<WindowInfo> it = this.mNotInterceptApps.iterator();
            while (it.hasNext()) {
                WindowInfo windowInfo = it.next();
                pw.println(prefix3 + windowInfo);
            }
        }
    }
}