package com.android.server;

import android.app.ActivityThread;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ServiceManager;
import android.provider.Settings;
import android.text.TextUtils;
import com.android.server.wm.VivoEasyShareManager;
import com.vivo.services.autorecover.SystemAutoRecoverManagerInternal;
import java.io.BufferedReader;
import java.io.StringReader;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class SafeScreenUtil {
    static final String ACTION_UCS_UPDATE_SWITCH = "com.vivo.daemonService.unifiedconfig.update_finish_broadcast_SafeScreenSwitch";
    private static final String DISABLE = "0";
    private static final String ENABLE = "1";
    private static final String KEYWORD_DISABLE_SAFE_SCREEN = "disable_safe_screen";
    private static final String KEYWORD_ENABLE_SAFE_SCREEN = "enable_safe_screen";
    private static final String SAFE_SCREEN_FEATURE = "safe_screen_feature";
    private static final int TRANSACTION_SYNC_PWD_MODE = 20500;
    private static final String VIVO_PWD_PROTECT_SCREEN = "vivo_pwd_protect_screen";
    private Handler mHandler;
    private static final String TAG = SafeScreenUtil.class.getSimpleName();
    private static final Uri ONLINE_CONFIG_URI = Uri.parse("content://com.vivo.abe.unifiedconfig.provider/configs");
    private static final String[] SAFE_SCREEN_LIST_SELECTION = {"SafeScreenSwitch", "1", "1.0"};
    private static SafeScreenUtil sInstance = null;
    private boolean mIsPasswordMode = true;
    private boolean mPwdProtectStatus = true;
    BroadcastReceiver mSafeScreenReceiver = new BroadcastReceiver() { // from class: com.android.server.SafeScreenUtil.2
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            SafeScreenUtil safeScreenUtil = SafeScreenUtil.this;
            safeScreenUtil.log("DEBUG_SAFE_SCREEN:PMDBListener onReceive intent=" + intent);
            if (SafeScreenUtil.ACTION_UCS_UPDATE_SWITCH.equals(intent.getAction())) {
                SafeScreenUtil.this.mHandler.removeCallbacks(SafeScreenUtil.this.mReadConfigRunnable);
                SafeScreenUtil.this.mHandler.postDelayed(SafeScreenUtil.this.mReadConfigRunnable, 500L);
            }
        }
    };
    private Runnable mReadConfigRunnable = new Runnable() { // from class: com.android.server.SafeScreenUtil.3
        @Override // java.lang.Runnable
        public void run() {
            SafeScreenUtil.this.log("mReadConfigRunnable!");
            SafeScreenUtil.this.updateSafeScreenSwitchFromUCS(SafeScreenUtil.ONLINE_CONFIG_URI, SafeScreenUtil.SAFE_SCREEN_LIST_SELECTION);
        }
    };
    private Context mContext = ActivityThread.currentApplication();

    public static SafeScreenUtil getInstance() {
        if (sInstance == null) {
            synchronized (SafeScreenUtil.class) {
                if (sInstance == null) {
                    sInstance = new SafeScreenUtil();
                }
            }
        }
        return sInstance;
    }

    public void updatePasswordMode(boolean pwdMode) {
        if (this.mPwdProtectStatus && pwdMode != this.mIsPasswordMode) {
            syncPasswordMode(pwdMode);
        }
    }

    private SafeScreenUtil() {
        Handler handler = UnifiedConfigThread.getHandler();
        this.mHandler = handler;
        handler.post(new Runnable() { // from class: com.android.server.SafeScreenUtil.1
            @Override // java.lang.Runnable
            public void run() {
                SafeScreenUtil.this.registerObserver();
            }
        });
        long identity = Binder.clearCallingIdentity();
        try {
            initPwdProtectSetting();
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private void initPwdProtectSetting() {
        log("initPdProtectSetting");
        if (TextUtils.isEmpty(getSettings(SAFE_SCREEN_FEATURE))) {
            putSettings(SAFE_SCREEN_FEATURE, "1");
        }
        if (TextUtils.isEmpty(getSettings(VIVO_PWD_PROTECT_SCREEN))) {
            putSettings(VIVO_PWD_PROTECT_SCREEN, "1");
        }
        this.mPwdProtectStatus = "1".equals(getSettings(VIVO_PWD_PROTECT_SCREEN));
        this.mHandler.postDelayed(this.mReadConfigRunnable, 500L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void syncPasswordMode(boolean value) {
        VivoEasyShareManager.getInstance().notifyPwdMode(value);
        try {
            ((SystemAutoRecoverManagerInternal) LocalServices.getService(SystemAutoRecoverManagerInternal.class)).notifyPasswordMode(value);
        } catch (Exception e) {
            String str = TAG;
            VSlog.d(str, "syncPasswordMode cause exception: " + e);
        }
        try {
            IBinder surfaceClient = ServiceManager.getService("SurfaceFlinger");
            Parcel data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            data.writeInt(value ? 1 : 0);
            surfaceClient.transact(TRANSACTION_SYNC_PWD_MODE, data, null, 0);
            data.recycle();
            this.mIsPasswordMode = value;
            log("syncPdMode success, value: " + value);
        } catch (Exception e2) {
            e2.printStackTrace();
            log("ERROR sync pd mode e=" + e2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(VIVO_PWD_PROTECT_SCREEN), false, new SettingsDBObserver(null));
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_UCS_UPDATE_SWITCH);
        this.mContext.registerReceiver(this.mSafeScreenReceiver, filter);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getSettings(String key) {
        return Settings.Secure.getString(this.mContext.getContentResolver(), key);
    }

    private void putSettings(String key, String value) {
        Settings.Secure.putString(this.mContext.getContentResolver(), key, value);
    }

    private void onSafeScreenFeatureChange(String value) {
        if (TextUtils.equals(getSettings(SAFE_SCREEN_FEATURE), value)) {
            return;
        }
        log("safe screen changed to " + value);
        putSettings(SAFE_SCREEN_FEATURE, value);
        putSettings(VIVO_PWD_PROTECT_SCREEN, value);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class SettingsDBObserver extends ContentObserver {
        public SettingsDBObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            SafeScreenUtil safeScreenUtil = SafeScreenUtil.this;
            safeScreenUtil.log("pd protect status onChange " + selfChange + " uri:" + uri);
            SafeScreenUtil safeScreenUtil2 = SafeScreenUtil.this;
            safeScreenUtil2.mPwdProtectStatus = "1".equals(safeScreenUtil2.getSettings(SafeScreenUtil.VIVO_PWD_PROTECT_SCREEN));
            if (!SafeScreenUtil.this.mPwdProtectStatus && SafeScreenUtil.this.mIsPasswordMode) {
                SafeScreenUtil.this.syncPasswordMode(false);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateSafeScreenSwitchFromUCS(Uri uri, String[] selectionArgs) {
        log("start updateSafeScreenSwitchFromUCS");
        ContentResolver resolver = this.mContext.getContentResolver();
        try {
            Cursor cursor = resolver.query(uri, null, null, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                byte[] content = cursor.getBlob(cursor.getColumnIndex("filecontent"));
                String contents = new String(content, "UTF-8");
                log("getConfig " + contents);
                parseAndAddContent(new StringReader(contents));
            } else {
                log("cursor is null or empty!");
            }
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            log("ERROR OPEN DB!!! e=" + e);
        }
    }

    private void parseAndAddContent(StringReader reader) {
        log("parseAndAddContent start");
        try {
            BufferedReader bufferedReader = new BufferedReader(reader);
            while (true) {
                String line = bufferedReader.readLine();
                if (line == null) {
                    break;
                }
                log("parseAndAddContent line=" + line);
                String line2 = line.trim();
                if (KEYWORD_DISABLE_SAFE_SCREEN.equals(line2)) {
                    onSafeScreenFeatureChange("0");
                    log("disable safe screen feature!");
                    break;
                } else if (KEYWORD_ENABLE_SAFE_SCREEN.equals(line2)) {
                    onSafeScreenFeatureChange("1");
                    log("enable safe screen feature!");
                    break;
                }
            }
            bufferedReader.close();
        } catch (Exception e) {
            log("ERROR parse content e=" + e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void log(String content) {
        VSlog.d(TAG, content);
    }
}