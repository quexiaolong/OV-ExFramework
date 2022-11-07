package com.android.server.policy.key;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.UserSwitchObserver;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import com.android.server.am.firewall.VivoFirewall;
import com.android.server.policy.AVivoInterceptKeyCallback;
import com.android.server.policy.VivoWMPHook;
import com.vivo.common.utils.VLog;
import com.vivo.services.security.server.VivoPermissionUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/* loaded from: classes.dex */
public class VivoAIKeyHandler extends AVivoInterceptKeyCallback {
    static final String ACTION_KEY_GUIDE;
    public static final int BIND_MSG_DOUBLE_CLICK = 1003;
    public static final int BIND_MSG_DOWN = 1000;
    public static final int BIND_MSG_LONG_PRESS = 1002;
    public static final int BIND_MSG_UP = 1001;
    static final long DOUBLE_CLICK_DURATION = 200;
    public static final String KEY_ACTIVE = "vivo.aikey.active";
    static final long LONG_PRESS_DURATION = 500;
    private static final int MSG_DOUBLE_CLICK_TIMEOUT = 6;
    private static final int MSG_FORCE_UPDATE = 5;
    private static final int MSG_KEY_DOWN = 2;
    private static final int MSG_KEY_LONGPRESS_TIMEOUT = 4;
    private static final int MSG_KEY_MAKE_UPDATE = 3;
    private static final int MSG_KEY_UP = 1;
    private static final int MSG_KEY_VOICE_PREPOST_TIMEOUT = 8;
    private static final int MSG_UPDATE_SETTINGS = 7;
    private static final int MSG_UPDATE_SETTINGS_FOR_USER_SWITCH = 9;
    static final boolean SupportDoubleClick;
    static final boolean SupportLongPress;
    static final boolean SupportShortClick;
    static final String TAG = "VivoAIKeyHandler";
    static final long VOICE_PREPOST_DURATION = 300;
    private ContentObserver contentObserver;
    private Context mContext;
    private VivoAIKeyDoubleClick mDoubleHandler;
    private Handler mHandler;
    private boolean mHasMultiDisplay;
    private VivoAIKeyLongPress mLongHandler;
    private VivoAIKeyShortPress mShortHandler;
    private VivoAIKeyExtend mVivoAIKeyExtend;
    private VivoWMPHook mVivoWMPHook;
    private PowerManager.WakeLock mWakeLock;
    private static boolean mDebug = SystemProperties.getBoolean("per.debug.aikey", false);
    static final boolean OverSea = SystemProperties.getBoolean("ro.vivo.product.overseas", false);
    private boolean isDownPress = false;
    private long systemReadyTime = 0;
    private AtomicBoolean mNeedChange = new AtomicBoolean(true);
    private AtomicBoolean mIsFirstTime = new AtomicBoolean(true);
    private List<KeepRecord> mKeeps = new ArrayList(2);
    private final Set<Target> mNeedUpEvents = new HashSet();
    private boolean isSystemReady = false;
    private BroadcastReceiver mPackageReceiver = new BroadcastReceiver() { // from class: com.android.server.policy.key.VivoAIKeyHandler.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getData() != null) {
                intent.getData().getSchemeSpecificPart();
                String action = intent.getAction();
                if (TextUtils.equals("android.intent.action.PACKAGE_ADDED", action) || TextUtils.equals("android.intent.action.PACKAGE_REMOVED", action) || TextUtils.equals("android.intent.action.PACKAGE_CHANGED", action)) {
                    VivoAIKeyHandler.this.mNeedChange.set(true);
                }
            }
        }
    };
    UserSwitchObserver mUserSwitchListener = new UserSwitchObserver() { // from class: com.android.server.policy.key.VivoAIKeyHandler.2
        public void onUserSwitching(int newUserId) {
        }

        public void onUserSwitchComplete(int newUserId) {
            VLog.d(VivoAIKeyHandler.TAG, "switch Complete " + newUserId);
            Settings.System.putIntForUser(VivoAIKeyHandler.this.mContext.getContentResolver(), VivoAIKeyExtend.AIKEY_DISABLE, 0, -2);
            VivoAIKeyHandler.this.mHandler.sendEmptyMessage(9);
        }
    };
    private KeyCallback mCallback = new KeyCallback();

    static {
        SupportShortClick = SystemProperties.getInt("ro.vivo.aikey.shortpress", 1) == 1;
        SupportLongPress = SystemProperties.getInt("ro.vivo.aikey.longpress", 1) == 1;
        SupportDoubleClick = SystemProperties.getInt("ro.vivo.aikey.doubleclick", 1) == 1;
        ACTION_KEY_GUIDE = OverSea ? "vivo.intent.action.AIKEY_FIRST_PRESS" : "vivo.intent.action.INSIDE_AIKEY_FIRST_PRESS";
    }

    /* loaded from: classes.dex */
    private class KeyCallback implements Handler.Callback {
        private static final int KEY_TYPE_DOUBLE_CLICK = 3;
        private static final int KEY_TYPE_DOWN = 1;
        private static final int KEY_TYPE_LONG_PRESS = 2;
        private long mLastUp;
        private int status;
        private boolean waitDoubleClick;

        private KeyCallback() {
            this.status = 0;
            this.mLastUp = 0L;
            this.waitDoubleClick = false;
        }

        @Override // android.os.Handler.Callback
        public boolean handleMessage(Message msg) {
            if (!VivoAIKeyHandler.this.isSystemReady) {
                ActivityManager activityManager = (ActivityManager) VivoAIKeyHandler.this.mContext.getSystemService(VivoFirewall.TYPE_ACTIVITY);
                VivoAIKeyHandler.this.isSystemReady = ActivityManager.isSystemReady();
                if (!VivoAIKeyHandler.this.isSystemReady) {
                    return true;
                }
                VivoAIKeyHandler.this.mNeedChange.set(true);
                VLog.v(VivoAIKeyHandler.TAG, "***system ready");
                VivoAIKeyHandler.this.systemReadyTime = SystemClock.elapsedRealtime();
                VivoAIKeyHandler.this.mHandler.sendEmptyMessageDelayed(5, 180000L);
                if (VivoAIKeyHandler.this.mLongHandler != null) {
                    VivoAIKeyHandler.this.mLongHandler.systemReady();
                }
            }
            VivoAIKeyHandler.this.updateData();
            if (msg.what == 9 || !VivoAIKeyHandler.this.mVivoAIKeyExtend.isNeedDisableAIKey()) {
                int i = msg.what;
                if (i == 3) {
                    KeyguardManager keyguardManager = (KeyguardManager) VivoAIKeyHandler.this.mContext.getSystemService("keyguard");
                    if (keyguardManager.isKeyguardLocked() || !VivoAIKeyHandler.this.isSystemReady) {
                        VivoAIKeyHandler.this.mHandler.sendEmptyMessageDelayed(3, 60000L);
                    } else {
                        VivoAIKeyHandler.this.mHandler.sendEmptyMessage(7);
                    }
                } else if (i != 5) {
                    if (i != 7) {
                        if (i == 8) {
                            VivoAIKeyHandler.this.mLongHandler.sendVoicePreload(VivoAIKeyHandler.this.mContext);
                        } else if (i != 9) {
                            if (VivoAIKeyHandler.SupportDoubleClick) {
                                process2(msg);
                            } else {
                                process(msg);
                            }
                        }
                    }
                    VivoAIKeyHandler.this.updateSettings();
                    VivoAIKeyHandler.this.mNeedChange.set(true);
                    VivoAIKeyHandler.this.updateData();
                    VivoAIKeyHandler.this.mVivoAIKeyExtend.updateSettings();
                } else {
                    VLog.d(VivoAIKeyHandler.TAG, "force update component data after system ready, prevent PKMS does not return any data");
                    VivoAIKeyHandler.this.mNeedChange.set(true);
                    VivoAIKeyHandler.this.updateData();
                }
                return true;
            }
            return true;
        }

        void onKeyDown(KeyEvent event) {
            if (!VivoAIKeyHandler.this.isDownPress || (event != null && event.getRepeatCount() == 0)) {
                VivoAIKeyHandler.this.isDownPress = true;
                Message message = VivoAIKeyHandler.this.mHandler.obtainMessage(2);
                message.obj = event;
                VivoAIKeyHandler.this.mHandler.sendMessage(message);
                VivoAIKeyHandler.this.setLongPressTimeout();
            }
        }

        void onkeyUp(KeyEvent event) {
            if (VivoAIKeyHandler.this.mHandler.hasMessages(4)) {
                VivoAIKeyHandler.this.mHandler.removeMessages(4);
            }
            Message message = VivoAIKeyHandler.this.mHandler.obtainMessage(1);
            message.obj = event;
            VivoAIKeyHandler.this.mHandler.sendMessage(message);
        }

        private void process(Message msg) {
            int i = msg.what;
            if (i == 1) {
                int i2 = this.status;
                if (i2 == 1) {
                    VivoAIKeyHandler.this.mNeedUpEvents.addAll(VivoAIKeyHandler.this.mShortHandler.dispatchShortDown());
                    VivoAIKeyHandler.this.mShortHandler.disaptchUp(VivoAIKeyHandler.this.mNeedUpEvents);
                    if (VivoAIKeyHandler.this.mHandler.hasMessages(8)) {
                        VivoAIKeyHandler.this.mHandler.removeMessages(8);
                    }
                } else if (i2 == 2) {
                    VivoAIKeyHandler.this.mLongHandler.disaptchUp(VivoAIKeyHandler.this.mNeedUpEvents, VivoAIKeyHandler.this);
                    VLog.v(VivoAIKeyHandler.TAG, "*long press");
                }
                VivoAIKeyHandler.this.mNeedUpEvents.clear();
                if (VivoAIKeyHandler.this.mWakeLock != null && VivoAIKeyHandler.this.mWakeLock.isHeld()) {
                    VivoAIKeyHandler.this.mWakeLock.release();
                }
            } else if (i == 2) {
                if (msg.obj != null) {
                    if (!VivoAIKeyHandler.this.mWakeLock.isHeld()) {
                        VivoAIKeyHandler.this.mWakeLock.acquire(2000L);
                    }
                    this.status = 1;
                    if (VivoAIKeyHandler.this.mIsFirstTime.get()) {
                        VivoAIKeyHandler.this.mKeeps.clear();
                        return;
                    }
                    return;
                }
                VLog.e(VivoAIKeyHandler.TAG, "error! no down keyevent");
            } else if (i == 4) {
                if (VivoAIKeyHandler.mDebug) {
                    VLog.d(VivoAIKeyHandler.TAG, "long press " + VivoAIKeyHandler.this.isDownPress + " right side key trigger = " + VivoAIKeyHandler.this.mVivoWMPHook.mVivoPolicy.isRightSideKeyTriggered() + " isAiKeyHandled = " + VivoAIKeyHandler.this.mVivoWMPHook.mVivoPolicy.isAIKeyHandled());
                }
                if (VivoAIKeyHandler.this.isDownPress && !VivoAIKeyHandler.this.mVivoWMPHook.mVivoPolicy.isRightSideKeyTriggered() && !VivoAIKeyHandler.this.mVivoWMPHook.mVivoPolicy.isAIKeyHandled()) {
                    VivoAIKeyHandler.this.mNeedUpEvents.addAll(VivoAIKeyHandler.this.mLongHandler.dispatchLongPress(VivoAIKeyHandler.this));
                    this.status = 2;
                }
            }
        }

        void onKeyDown2(KeyEvent event) {
            if (!VivoAIKeyHandler.this.isDownPress || (event != null && event.getRepeatCount() == 0)) {
                VivoAIKeyHandler.this.isDownPress = true;
                Message message = VivoAIKeyHandler.this.mHandler.obtainMessage(2);
                message.obj = event;
                VivoAIKeyHandler.this.mHandler.sendMessage(message);
                VivoAIKeyHandler.this.setLongPressTimeout();
            }
        }

        void onKeyUp2(KeyEvent event) {
            if (VivoAIKeyHandler.this.mHandler.hasMessages(4)) {
                VivoAIKeyHandler.this.mHandler.removeMessages(4);
            }
            Message message = VivoAIKeyHandler.this.mHandler.obtainMessage(1);
            message.obj = event;
            VivoAIKeyHandler.this.mHandler.sendMessage(message);
        }

        private void process2(Message msg) {
            int i = msg.what;
            if (i == 1) {
                if (VivoAIKeyHandler.this.mHandler.hasMessages(6)) {
                    VivoAIKeyHandler.this.mHandler.removeMessages(6);
                }
                if (VivoAIKeyHandler.this.mHandler.hasMessages(4)) {
                    VivoAIKeyHandler.this.mHandler.removeMessages(4);
                }
                int i2 = this.status;
                if (i2 == 1) {
                    if (VivoAIKeyHandler.this.mHandler.hasMessages(8)) {
                        VivoAIKeyHandler.this.mHandler.removeMessages(8);
                    }
                    if (!this.waitDoubleClick) {
                        this.waitDoubleClick = true;
                        this.mLastUp = SystemClock.uptimeMillis();
                        VivoAIKeyHandler.this.mHandler.sendEmptyMessageDelayed(6, VivoAIKeyHandler.DOUBLE_CLICK_DURATION);
                        VLog.d(VivoAIKeyHandler.TAG, "up at " + this.mLastUp);
                    } else {
                        this.waitDoubleClick = false;
                        VivoAIKeyHandler.this.mDoubleHandler.disaptchUp();
                        VLog.v(VivoAIKeyHandler.TAG, "*double click");
                    }
                } else if (i2 == 2) {
                    VivoAIKeyHandler.this.mLongHandler.disaptchUp(VivoAIKeyHandler.this.mNeedUpEvents, VivoAIKeyHandler.this);
                    this.waitDoubleClick = false;
                    VLog.v(VivoAIKeyHandler.TAG, "*long press");
                }
                VivoAIKeyHandler.this.mNeedUpEvents.clear();
                if (VivoAIKeyHandler.this.mWakeLock != null && VivoAIKeyHandler.this.mWakeLock.isHeld()) {
                    VivoAIKeyHandler.this.mWakeLock.release();
                }
            } else if (i == 2) {
                if (msg.obj == null) {
                    VLog.e(VivoAIKeyHandler.TAG, "error! no down keyevent");
                    return;
                }
                KeyEvent keyEvent = (KeyEvent) msg.obj;
                Log.d(VivoAIKeyHandler.TAG, "down at " + keyEvent.getDownTime());
                if (!VivoAIKeyHandler.this.mWakeLock.isHeld()) {
                    VivoAIKeyHandler.this.mWakeLock.acquire(2000L);
                }
                this.status = 1;
                if (VivoAIKeyHandler.this.mIsFirstTime.get()) {
                    VivoAIKeyHandler.this.mKeeps.clear();
                }
                if (this.waitDoubleClick) {
                    if (VivoAIKeyHandler.this.mHandler.hasMessages(6)) {
                        VivoAIKeyHandler.this.mHandler.removeMessages(6);
                    }
                    if (keyEvent.getDownTime() - this.mLastUp <= VivoAIKeyHandler.DOUBLE_CLICK_DURATION) {
                        VivoAIKeyHandler.this.setLongPressTimeout();
                    }
                }
            } else if (i == 4) {
                if (VivoAIKeyHandler.mDebug) {
                    VLog.d(VivoAIKeyHandler.TAG, "long press " + VivoAIKeyHandler.this.isDownPress + " right side key trigger = " + VivoAIKeyHandler.this.mVivoWMPHook.mVivoPolicy.isRightSideKeyTriggered() + " isAiKeyHandled = " + VivoAIKeyHandler.this.mVivoWMPHook.mVivoPolicy.isAIKeyHandled());
                }
                if (VivoAIKeyHandler.this.isDownPress && !VivoAIKeyHandler.this.mVivoWMPHook.mVivoPolicy.isRightSideKeyTriggered() && !VivoAIKeyHandler.this.mVivoWMPHook.mVivoPolicy.isAIKeyHandled()) {
                    VivoAIKeyHandler.this.mNeedUpEvents.addAll(VivoAIKeyHandler.this.mLongHandler.dispatchLongPress(VivoAIKeyHandler.this));
                    this.status = 2;
                }
            } else if (i == 6 && this.waitDoubleClick) {
                this.waitDoubleClick = false;
                VivoAIKeyHandler.this.mNeedUpEvents.addAll(VivoAIKeyHandler.this.mShortHandler.dispatchShortDown());
                VivoAIKeyHandler.this.mShortHandler.disaptchUp(VivoAIKeyHandler.this.mNeedUpEvents);
                this.status = 0;
                VLog.v(VivoAIKeyHandler.TAG, "*click");
                if (VivoAIKeyHandler.this.mHandler.hasMessages(8)) {
                    VivoAIKeyHandler.this.mHandler.removeMessages(8);
                }
            }
        }
    }

    public VivoAIKeyHandler(VivoWMPHook vivoWMPHook, Context context) {
        this.contentObserver = new ContentObserver(this.mHandler) { // from class: com.android.server.policy.key.VivoAIKeyHandler.3
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                try {
                    if (VivoAIKeyHandler.this.mContext != null) {
                        ContentResolver contentResolver = VivoAIKeyHandler.this.mContext.getContentResolver();
                        VivoAIKeyHandler.this.mShortHandler.setShortChoose(Settings.System.getStringForUser(contentResolver, VivoAIKeyShortPress.USER_SELECT_SHORT_PRESS, -2));
                        VivoAIKeyHandler.this.mLongHandler.setLongPressChoose(Settings.System.getStringForUser(contentResolver, VivoAIKeyLongPress.USER_LONG_PRESS_SELECT, -2));
                        VivoAIKeyHandler.this.mDoubleHandler.setChoose(Settings.System.getStringForUser(contentResolver, VivoAIKeyDoubleClick.USER_SELECT, -2));
                        if (VivoAIKeyHandler.OverSea && VivoAIKeyHandler.this.mIsFirstTime.get()) {
                            boolean isFirst = Settings.Secure.getIntForUser(contentResolver, "jovi_btn_first_guide", 1, -2) == 1;
                            VivoAIKeyHandler.this.mIsFirstTime.set(isFirst);
                            VLog.d(VivoAIKeyHandler.TAG, "isFirst " + isFirst + ", size = " + VivoAIKeyHandler.this.mKeeps.size());
                            if (!VivoAIKeyHandler.this.mIsFirstTime.get() && VivoAIKeyHandler.this.mKeeps.size() > 0) {
                                try {
                                    Thread.sleep(1500L);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                for (KeepRecord r : VivoAIKeyHandler.this.mKeeps) {
                                    VivoAIKeyHandler.this.sendToTarget(r.componentName, r.intent, r.target, r.type);
                                }
                                VivoAIKeyHandler.this.mKeeps.clear();
                            }
                        }
                    }
                    VivoAIKeyHandler.this.mNeedChange.set(true);
                    boolean unused = VivoAIKeyHandler.mDebug = SystemProperties.getBoolean("per.debug.aikey", false);
                    VivoAIKeyHandler.this.updateData();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        };
        try {
            this.mVivoWMPHook = vivoWMPHook;
            this.mContext = context;
            this.mHasMultiDisplay = hasMultiDisplay();
            HandlerThread thread = new HandlerThread(TAG);
            thread.start();
            Handler handler = new Handler(thread.getLooper(), this.mCallback);
            this.mHandler = handler;
            this.mVivoAIKeyExtend = new VivoAIKeyExtend(context, handler);
            this.mShortHandler = new VivoAIKeyShortPress(this);
            this.mLongHandler = new VivoAIKeyLongPress(this, this.mContext);
            this.mDoubleHandler = new VivoAIKeyDoubleClick(this, this.mContext);
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
            intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
            intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
            intentFilter.addDataScheme("package");
            context.registerReceiver(this.mPackageReceiver, intentFilter, null, this.mHandler);
            PowerManager powerManager = (PowerManager) this.mContext.getSystemService("power");
            this.mWakeLock = powerManager.newWakeLock(1, "AIKey");
            ContentResolver contentResolver = context.getContentResolver();
            VLog.d(TAG, "aikey_version set 10");
            if (10 > Settings.Global.getInt(contentResolver, "aikey_version", 0)) {
                String shortKey = Settings.Global.getString(contentResolver, VivoAIKeyShortPress.USER_SELECT_SHORT_PRESS);
                String longKey = Settings.Global.getString(contentResolver, VivoAIKeyLongPress.USER_LONG_PRESS_SELECT);
                String doubleKey = Settings.Global.getString(contentResolver, VivoAIKeyDoubleClick.USER_SELECT);
                Settings.System.putStringForUser(contentResolver, VivoAIKeyShortPress.USER_SELECT_SHORT_PRESS, shortKey, -2);
                Settings.System.putStringForUser(contentResolver, VivoAIKeyLongPress.USER_LONG_PRESS_SELECT, longKey, -2);
                Settings.System.putStringForUser(contentResolver, VivoAIKeyDoubleClick.USER_SELECT, doubleKey, -2);
            }
            Settings.Global.putInt(contentResolver, "aikey_version", 10);
            Settings.System.putIntForUser(contentResolver, VivoAIKeyExtend.AIKEY_DISABLE, 0, -2);
            updateSettings();
            Uri uri = Uri.withAppendedPath(Settings.System.CONTENT_URI, VivoAIKeyShortPress.USER_SELECT_SHORT_PRESS);
            contentResolver.registerContentObserver(uri, true, this.contentObserver, -1);
            Uri uri2 = Uri.withAppendedPath(Settings.System.CONTENT_URI, VivoAIKeyLongPress.USER_LONG_PRESS_SELECT);
            this.mContext.getContentResolver().registerContentObserver(uri2, true, this.contentObserver, -1);
            Uri uri3 = Uri.withAppendedPath(Settings.System.CONTENT_URI, VivoAIKeyDoubleClick.USER_SELECT);
            context.getContentResolver().registerContentObserver(uri3, true, this.contentObserver, -1);
            Uri uri4 = Uri.withAppendedPath(Settings.Secure.CONTENT_URI, "jovi_btn_first_guide");
            this.mIsFirstTime.set(Settings.Secure.getIntForUser(contentResolver, "jovi_btn_first_guide", 1, -2) == 1);
            if (OverSea && this.mIsFirstTime.get()) {
                contentResolver.registerContentObserver(uri4, true, this.contentObserver, -1);
            }
            ActivityManager.getService().registerUserSwitchObserver(this.mUserSwitchListener, TAG);
            this.mHandler.sendEmptyMessageDelayed(3, 120000L);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateSettings() {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        this.mShortHandler.setShortChoose(Settings.System.getStringForUser(contentResolver, VivoAIKeyShortPress.USER_SELECT_SHORT_PRESS, -2));
        this.mLongHandler.setLongPressChoose(Settings.System.getStringForUser(contentResolver, VivoAIKeyLongPress.USER_LONG_PRESS_SELECT, -2));
        this.mDoubleHandler.setChoose(Settings.System.getStringForUser(contentResolver, VivoAIKeyDoubleClick.USER_SELECT, -2));
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public boolean onCheckForward(int keyCode, KeyEvent event) {
        return false;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public int onKeyDown(int keyCode, KeyEvent event) {
        if (this.mHasMultiDisplay) {
            VLog.d(TAG, "onKeyDown mHasMultiDisplay return");
            return -100;
        } else if (this.mVivoWMPHook.mVivoPolicy.isAIKeyHandled()) {
            VLog.d(TAG, "onKeyDown isAIKeyHandled return");
            return -100;
        } else {
            if (!this.mVivoAIKeyExtend.isAlarm() && this.mState == 0) {
                if (SupportDoubleClick) {
                    this.mCallback.onKeyDown2(event);
                } else {
                    this.mCallback.onKeyDown(event);
                }
            }
            return -100;
        }
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public int onKeyUp(int keyCode, KeyEvent event) {
        if (this.mHasMultiDisplay) {
            VLog.d(TAG, "onKeyUp mHasMultiDisplay return");
            return 0;
        } else if (this.mVivoWMPHook.mVivoPolicy.isAIKeyHandled()) {
            VLog.d(TAG, "onKeyUp isAIKeyHandled return");
            return 0;
        } else if (this.mVivoAIKeyExtend.isAlarm()) {
            this.mVivoAIKeyExtend.stopAlarm();
            return 0;
        } else {
            if (this.isDownPress) {
                this.isDownPress = false;
            }
            if (this.mState == 0) {
                if (SupportDoubleClick) {
                    this.mCallback.onKeyUp2(event);
                } else {
                    this.mCallback.onkeyUp(event);
                }
            }
            return 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateData() {
        int size;
        KeyguardManager keyguardManager;
        if (!this.mNeedChange.get() || this.mContext == null) {
            return;
        }
        VLog.d(TAG, "updateData");
        if (this.mIsFirstTime.get()) {
            Intent guideIntent = new Intent(ACTION_KEY_GUIDE);
            guideIntent.setPackage("com.vivo.aikeyguide");
            List<ResolveInfo> guideList = this.mContext.getPackageManager().queryIntentActivities(guideIntent, 786624);
            if (guideList == null || guideList.size() == 0) {
                VLog.d(TAG, "no guideapp");
                this.mIsFirstTime.set(false);
            } else {
                ResolveInfo rinfo = guideList.get(0);
                if (rinfo.getComponentInfo() == null || !rinfo.getComponentInfo().isEnabled()) {
                    VLog.d(TAG, "guideapp not enable");
                    this.mIsFirstTime.set(false);
                } else {
                    VLog.d(TAG, "guideapp exist");
                }
            }
        }
        try {
            try {
                size = this.mLongHandler.updateData(this.mContext) + this.mShortHandler.updateData(this.mContext) + this.mDoubleHandler.updateData(this.mContext);
                keyguardManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
            } catch (Exception e) {
                e.printStackTrace();
                KeyguardManager keyguardManager2 = (KeyguardManager) this.mContext.getSystemService("keyguard");
                if (!keyguardManager2.isKeyguardLocked()) {
                    if (0 < 1 && SystemClock.elapsedRealtime() - this.systemReadyTime < 120000) {
                        this.mNeedChange.set(true);
                        if (!mDebug) {
                            return;
                        }
                    }
                }
            }
            if (!keyguardManager.isKeyguardLocked()) {
                if (size < 1 && SystemClock.elapsedRealtime() - this.systemReadyTime < 120000) {
                    this.mNeedChange.set(true);
                    if (!mDebug) {
                        return;
                    }
                    VLog.v(TAG, "try to query components next time");
                    return;
                }
                this.mNeedChange.set(false);
                return;
            }
            this.mNeedChange.set(true);
        } catch (Throwable th) {
            KeyguardManager keyguardManager3 = (KeyguardManager) this.mContext.getSystemService("keyguard");
            if (keyguardManager3.isKeyguardLocked()) {
                this.mNeedChange.set(true);
            } else if (0 >= 1 || SystemClock.elapsedRealtime() - this.systemReadyTime >= 120000) {
                this.mNeedChange.set(false);
            } else {
                this.mNeedChange.set(true);
                if (mDebug) {
                    VLog.v(TAG, "try to query components next time");
                }
            }
            throw th;
        }
    }

    public void sendToTarget(ComponentName componentName, Intent intent, Target target, final int type) {
        if (componentName == null || intent == null || target == null) {
            VLog.w(TAG, "null data : " + componentName + ", " + intent + ", " + target);
            return;
        }
        if (this.mIsFirstTime.get()) {
            this.mKeeps.add(new KeepRecord(componentName, intent, target, type));
            Intent guideIntent = new Intent(ACTION_KEY_GUIDE);
            guideIntent.addFlags(268435456);
            guideIntent.setPackage("com.vivo.aikeyguide");
            try {
                try {
                    this.mContext.startActivityAsUser(guideIntent, UserHandle.CURRENT);
                    VLog.d(TAG, "send to guideapp");
                    if (OverSea) {
                        return;
                    }
                    this.mIsFirstTime.set(false);
                    ContentResolver contentResolver = this.mContext.getContentResolver();
                    Settings.Secure.putIntForUser(contentResolver, "jovi_btn_first_guide", 0, -2);
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                    if (this.isSystemReady) {
                        this.mIsFirstTime.set(false);
                    }
                    if (!OverSea) {
                        this.mIsFirstTime.set(false);
                        ContentResolver contentResolver2 = this.mContext.getContentResolver();
                        Settings.Secure.putIntForUser(contentResolver2, "jovi_btn_first_guide", 0, -2);
                    }
                }
            } catch (Throwable th) {
                if (!OverSea) {
                    this.mIsFirstTime.set(false);
                    ContentResolver contentResolver3 = this.mContext.getContentResolver();
                    Settings.Secure.putIntForUser(contentResolver3, "jovi_btn_first_guide", 0, -2);
                }
                throw th;
            }
        }
        intent.setComponent(componentName);
        intent.setPackage(target.pkgName);
        intent.putExtra("keyTime", SystemClock.elapsedRealtime());
        VLog.v(TAG, "#sendToTarget " + componentName + " type = " + target.type + ", isSystemUID =" + target.isSharedSystemUID);
        try {
            int i = target.type;
            if (i == 1) {
                intent.addFlags(268435456);
                this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
            } else if (i == 2) {
                if (target.isSharedSystemUID) {
                    this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
                } else {
                    this.mContext.bindServiceAsUser(intent, new ServiceConnection() { // from class: com.android.server.policy.key.VivoAIKeyHandler.4
                        /*  JADX ERROR: JadxRuntimeException in pass: RegionMakerVisitor
                            jadx.core.utils.exceptions.JadxRuntimeException: Can't find top splitter block for handler:B:11:0x0030
                            	at jadx.core.utils.BlockUtils.getTopSplitterForHandler(BlockUtils.java:1234)
                            	at jadx.core.dex.visitors.regions.RegionMaker.processTryCatchBlocks(RegionMaker.java:1018)
                            	at jadx.core.dex.visitors.regions.RegionMakerVisitor.visit(RegionMakerVisitor.java:55)
                            */
                        /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:11:0x0030 -> B:12:0x0032). Please submit an issue!!! */
                        @Override // android.content.ServiceConnection
                        public void onServiceConnected(android.content.ComponentName r4, android.os.IBinder r5) {
                            /*
                                r3 = this;
                                android.os.Messenger r0 = new android.os.Messenger     // Catch: java.lang.Throwable -> L20 android.os.RemoteException -> L22
                                r0.<init>(r5)     // Catch: java.lang.Throwable -> L20 android.os.RemoteException -> L22
                                com.android.server.policy.key.VivoAIKeyHandler r1 = com.android.server.policy.key.VivoAIKeyHandler.this     // Catch: java.lang.Throwable -> L20 android.os.RemoteException -> L22
                                android.os.Handler r1 = com.android.server.policy.key.VivoAIKeyHandler.access$200(r1)     // Catch: java.lang.Throwable -> L20 android.os.RemoteException -> L22
                                android.os.Message r1 = android.os.Message.obtain(r1)     // Catch: java.lang.Throwable -> L20 android.os.RemoteException -> L22
                                int r2 = r2     // Catch: java.lang.Throwable -> L20 android.os.RemoteException -> L22
                                r1.what = r2     // Catch: java.lang.Throwable -> L20 android.os.RemoteException -> L22
                                r0.send(r1)     // Catch: java.lang.Throwable -> L20 android.os.RemoteException -> L22
                                com.android.server.policy.key.VivoAIKeyHandler r0 = com.android.server.policy.key.VivoAIKeyHandler.this     // Catch: java.lang.Exception -> L30
                                android.content.Context r0 = com.android.server.policy.key.VivoAIKeyHandler.access$100(r0)     // Catch: java.lang.Exception -> L30
                                r0.unbindService(r3)     // Catch: java.lang.Exception -> L30
                                goto L2f
                            L20:
                                r0 = move-exception
                                goto L33
                            L22:
                                r0 = move-exception
                                r0.printStackTrace()     // Catch: java.lang.Throwable -> L20
                                com.android.server.policy.key.VivoAIKeyHandler r0 = com.android.server.policy.key.VivoAIKeyHandler.this     // Catch: java.lang.Exception -> L30
                                android.content.Context r0 = com.android.server.policy.key.VivoAIKeyHandler.access$100(r0)     // Catch: java.lang.Exception -> L30
                                r0.unbindService(r3)     // Catch: java.lang.Exception -> L30
                            L2f:
                                goto L32
                            L30:
                                r0 = move-exception
                            L32:
                                return
                            L33:
                                com.android.server.policy.key.VivoAIKeyHandler r1 = com.android.server.policy.key.VivoAIKeyHandler.this     // Catch: java.lang.Exception -> L3d
                                android.content.Context r1 = com.android.server.policy.key.VivoAIKeyHandler.access$100(r1)     // Catch: java.lang.Exception -> L3d
                                r1.unbindService(r3)     // Catch: java.lang.Exception -> L3d
                                goto L3e
                            L3d:
                                r1 = move-exception
                            L3e:
                                throw r0
                            */
                            throw new UnsupportedOperationException("Method not decompiled: com.android.server.policy.key.VivoAIKeyHandler.AnonymousClass4.onServiceConnected(android.content.ComponentName, android.os.IBinder):void");
                        }

                        @Override // android.content.ServiceConnection
                        public void onServiceDisconnected(ComponentName componentName2) {
                        }
                    }, 1, UserHandle.CURRENT);
                }
            }
        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isDebug() {
        return mDebug;
    }

    /* loaded from: classes.dex */
    public static class Target {
        public static final int TYPE_ACTIVITY = 1;
        public static final int TYPE_SERVICE = 2;
        final String action;
        final String componentName;
        final boolean isSharedSystemUID;
        final Bundle metaData;
        final int order;
        final String pkgName;
        final int type;

        public Target(String pkg, String cmpName, int order, int type, Bundle meta, boolean shareSysteUID, String action) {
            this.pkgName = pkg;
            this.componentName = cmpName;
            this.order = order;
            this.type = type;
            this.metaData = meta;
            this.isSharedSystemUID = shareSysteUID;
            this.action = action;
        }

        public static int getType(ResolveInfo info) {
            if (info == null) {
                return -1;
            }
            if (info.serviceInfo != null) {
                return 2;
            }
            if (info.activityInfo == null) {
                return -1;
            }
            return 1;
        }
    }

    public static void handleComponents(String tag, List<Target> targets, List<ResolveInfo> infos, HashMap<String, ResolveInfo> resolveInfoHashMap, PackageManager packageManager, String ORDER, String ACTION, String signature) {
        Iterator<ResolveInfo> it;
        HashMap<String, ResolveInfo> hashMap = resolveInfoHashMap;
        if (infos != null && targets != null && hashMap != null) {
            Iterator<ResolveInfo> it2 = infos.iterator();
            while (it2.hasNext()) {
                ResolveInfo info = it2.next();
                String name = null;
                String pkgName = null;
                Bundle metaData = null;
                int uid = -1;
                if (info.getComponentInfo() == null) {
                    hashMap = resolveInfoHashMap;
                } else if (info.getComponentInfo().isEnabled()) {
                    ApplicationInfo applicationInfo = null;
                    if (info.serviceInfo != null) {
                        pkgName = info.serviceInfo.packageName;
                        name = info.serviceInfo.name;
                        metaData = info.serviceInfo.metaData;
                        uid = info.serviceInfo.applicationInfo != null ? info.serviceInfo.applicationInfo.uid : -1;
                        applicationInfo = info.serviceInfo.applicationInfo;
                    } else if (info.activityInfo != null) {
                        pkgName = info.activityInfo.packageName;
                        name = info.activityInfo.name;
                        metaData = info.activityInfo.metaData;
                        uid = info.activityInfo.applicationInfo != null ? info.activityInfo.applicationInfo.uid : -1;
                        applicationInfo = info.activityInfo.applicationInfo;
                    }
                    if (TextUtils.isEmpty(name)) {
                        hashMap = resolveInfoHashMap;
                    } else if (metaData != null) {
                        int active = metaData.getInt(KEY_ACTIVE, 3);
                        Log.d(tag, name + ", active  " + active);
                        if (OverSea && (active == 2 || active == 3)) {
                            if (mDebug) {
                                VLog.d(tag, name + " oversea active");
                            }
                        } else if (OverSea || (active != 1 && active != 3)) {
                            Iterator<ResolveInfo> it3 = it2;
                            VLog.w(tag, name + ", not support this, active = " + active + ", " + OverSea);
                            hashMap = resolveInfoHashMap;
                            it2 = it3;
                        } else if (mDebug) {
                            VLog.d(tag, name + " cn active");
                        }
                        int signatureRes = packageManager.checkSignatures(signature, pkgName);
                        if (isDebug()) {
                            VLog.d(tag, pkgName + ",checkSignatures = " + signatureRes);
                        }
                        int signatureRes2 = ((uid > 0 && uid < 10000) || isVivoApp(packageManager, applicationInfo) || isWhiteList(pkgName)) ? 1 : signatureRes;
                        boolean contain = hashMap.containsKey(name);
                        if (contain || signatureRes2 < 0) {
                            it = it2;
                        } else {
                            hashMap.put(name, info);
                            int type = Target.getType(info);
                            if (type >= 0) {
                                int order = metaData.getInt(ORDER);
                                it = it2;
                                targets.add(new Target(pkgName, name, order, type, metaData, uid == 1000, ACTION));
                                VLog.d(tag, "add component " + info + ", order = " + order);
                            }
                        }
                        hashMap = resolveInfoHashMap;
                        it2 = it;
                    }
                }
            }
        }
        try {
            Collections.sort(targets, new Comparator<Target>() { // from class: com.android.server.policy.key.VivoAIKeyHandler.5
                @Override // java.util.Comparator
                public int compare(Target o1, Target o2) {
                    if (o1.order > o2.order) {
                        return -1;
                    }
                    if (o1.order == o2.order) {
                        return 0;
                    }
                    return 1;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String checkChooseData(String tag, List<Target> targets, String VALUES, String curChoose, String COMPONENT_SELECT, Context context) {
        String currentChoose = curChoose;
        int size = targets.size();
        if (size > 0 && currentChoose == null) {
            Target target = targets.get(size - 1);
            currentChoose = target.metaData.getString(VALUES);
            KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService("keyguard");
            if (!keyguardManager.isKeyguardLocked()) {
                Settings.System.putStringForUser(context.getContentResolver(), COMPONENT_SELECT, currentChoose, -2);
            }
            VLog.d(tag, "set mCurrentChoose " + currentChoose);
        }
        return currentChoose;
    }

    @Override // com.android.server.policy.AVivoInterceptKeyCallback, com.android.server.policy.IVivoKeyCallback
    public void cancelPendingKeyAction() {
        if (this.mHandler.hasMessages(4)) {
            this.mHandler.removeMessages(4);
        }
    }

    private boolean hasMultiDisplay() {
        DisplayManager displayManager = (DisplayManager) this.mContext.getSystemService("display");
        Display secondDisplay = displayManager.getDisplay(4096);
        return secondDisplay != null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setLongPressTimeout() {
        if (this.mHandler.hasMessages(4)) {
            this.mHandler.removeMessages(4);
        }
        this.mHandler.sendEmptyMessageDelayed(4, LONG_PRESS_DURATION);
        if (this.mHandler.hasMessages(8)) {
            this.mHandler.removeMessages(8);
        }
        VivoAIKeyLongPress vivoAIKeyLongPress = this.mLongHandler;
        if (vivoAIKeyLongPress != null && "voice".equals(vivoAIKeyLongPress.getLongPressChoose())) {
            this.mHandler.sendEmptyMessageDelayed(8, VOICE_PREPOST_DURATION);
        }
    }

    public static boolean isVivoApp(PackageManager packageManager, ApplicationInfo appInfo) {
        return (packageManager == null || appInfo == null || (packageManager.checkSignatures(VivoPermissionUtils.OS_PKG, appInfo.packageName) != 0 && packageManager.checkSignatures("com.android.providers.contacts", appInfo.packageName) != 0 && packageManager.checkSignatures("com.android.providers.media", appInfo.packageName) != 0 && (appInfo.flags & 1) == 0 && (appInfo.flags & 128) == 0)) ? false : true;
    }

    public static boolean isWhiteList(String pkgName) {
        String str = SystemProperties.get("ro.aikey.whitelist");
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        String[] pkgs = str.split(",");
        return Arrays.asList(pkgs).contains(pkgName);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class KeepRecord {
        ComponentName componentName;
        Intent intent;
        Target target;
        int type;

        KeepRecord(ComponentName c, Intent i, Target ta, int t) {
            this.componentName = c;
            this.intent = i;
            this.target = ta;
            this.type = t;
        }
    }
}