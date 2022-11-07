package com.vivo.services.capacitykey;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.IProcessObserver;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.hardware.display.VivoDisplayModule;
import android.hardware.display.VivoDisplayStateManager;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.Display;
import java.util.List;
import vivo.app.capacitykey.ICapacityKey;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class CapacityKeyService extends ICapacityKey.Stub {
    private static final String BBK_LAUNCHER = "com.bbk.launcher2";
    public static final String KEY_GAME_MODE = "is_game_mode";
    public static final String KEY_GAME_SPACE = "is_game_space";
    public static final String PRESS_ABS = "screen_pressure_coordinate";
    private static final String TAG = "CapacityKeyService";
    private static IActivityManager mIActivityManager;
    private Handler handler;
    private Context mContext;
    private boolean mFactorySwitch;
    private DisplayMonitor mMainDisplayMonitor;
    private String PACKAGE_NAME = null;
    private String PACKAGE_NAME_BACK = null;
    private int package_num = 0;
    private boolean last_lcd_state = false;
    PhoneStateListener mPhoneStateListener = new PhoneStateListener() { // from class: com.vivo.services.capacitykey.CapacityKeyService.1
        @Override // android.telephony.PhoneStateListener
        public void onCallStateChanged(int state, String incomingNumber) {
            VSlog.d(CapacityKeyService.TAG, "onCallStateChanged state is " + state);
            if (state == 0) {
                CapacityKeyService.this.SetServiceState("CALL_NONE".getBytes());
            } else if (state == 1 || state == 2) {
                CapacityKeyService.this.SetServiceState("CALLING".getBytes());
            }
        }
    };
    private IProcessObserver mProcessObserver = new IProcessObserver.Stub() { // from class: com.vivo.services.capacitykey.CapacityKeyService.4
        public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
            VSlog.d(CapacityKeyService.TAG, "foregroundActivities detected uid=" + uid + " appName=" + CapacityKeyService.this.getAppNameFromUid(uid) + " state=" + foregroundActivities);
            try {
                if (foregroundActivities) {
                    CapacityKeyService.this.PACKAGE_NAME = CapacityKeyService.this.getAppNameFromUid(uid);
                    VSlog.d(CapacityKeyService.TAG, "foreground: " + CapacityKeyService.this.PACKAGE_NAME);
                    CapacityKeyService.access$408(CapacityKeyService.this);
                } else {
                    CapacityKeyService.this.PACKAGE_NAME_BACK = CapacityKeyService.this.getAppNameFromUid(uid);
                    VSlog.d(CapacityKeyService.TAG, "background: " + CapacityKeyService.this.PACKAGE_NAME_BACK);
                    CapacityKeyService.access$408(CapacityKeyService.this);
                }
                if (CapacityKeyService.this.package_num >= 2 && CapacityKeyService.this.PACKAGE_NAME != CapacityKeyService.this.PACKAGE_NAME_BACK) {
                    CapacityKeyService.this.package_num = 0;
                    if (CapacityKeyService.this.PACKAGE_NAME == CapacityKeyService.BBK_LAUNCHER) {
                        CapacityKeyService.this.SetServiceState("BBK_LAUNCHER".getBytes());
                    } else {
                        CapacityKeyService.this.SetServiceState("NOT_BBK_LAUNCHER".getBytes());
                    }
                }
            } catch (Exception e) {
                VSlog.d(CapacityKeyService.TAG, "Failed in TouchscreenSetAppCode");
            }
        }

        public void onProcessStateChanged(int pid, int uid, int importance) {
        }

        public void onProcessDied(int pid, int uid) {
        }

        public void onForegroundServicesChanged(int pid, int uid, int serviceTypes) {
        }
    };
    private final ContentObserver mGameModeObserver = new ContentObserver(new Handler()) { // from class: com.vivo.services.capacitykey.CapacityKeyService.5
        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (Settings.System.getUriFor("is_game_mode").equals(uri)) {
                if (CapacityKeyService.isInGameMode(CapacityKeyService.this.mContext)) {
                    VSlog.d(CapacityKeyService.TAG, "enter game mode");
                    CapacityKeyService.this.SetServiceState("GAME_IN".getBytes());
                } else {
                    VSlog.d(CapacityKeyService.TAG, "out game mode");
                    CapacityKeyService.this.SetServiceState("GAME_OUT".getBytes());
                }
            }
            if (Settings.Global.getUriFor("is_game_space").equals(uri)) {
                if (CapacityKeyService.isInGameSpace(CapacityKeyService.this.mContext)) {
                    VSlog.d(CapacityKeyService.TAG, "enter game space");
                    CapacityKeyService.this.SetServiceState("GAME_SPACE_IN".getBytes());
                } else {
                    VSlog.d(CapacityKeyService.TAG, "out game space");
                    CapacityKeyService.this.SetServiceState("GAME_SPACE_OUT".getBytes());
                }
            }
            if (Settings.Global.getUriFor(CapacityKeyService.PRESS_ABS).equals(uri)) {
                String pressAbs = CapacityKeyService.getPressCoordinate(CapacityKeyService.this.mContext);
                VSlog.d(CapacityKeyService.TAG, "get abs :" + pressAbs);
                CapacityKeyService capacityKeyService = CapacityKeyService.this;
                capacityKeyService.SetServiceState(("ABS" + pressAbs).getBytes());
            }
        }
    };

    /* JADX INFO: Access modifiers changed from: private */
    public static native int nativeCapacityKeySetSensitivity(int i, int i2);

    private static native void nativeInitCapacityKey();

    /* JADX INFO: Access modifiers changed from: private */
    public static native int nativeSetServiceState(byte[] bArr);

    static /* synthetic */ int access$408(CapacityKeyService x0) {
        int i = x0.package_num;
        x0.package_num = i + 1;
        return i;
    }

    /* loaded from: classes.dex */
    private class DisplayMonitor implements DisplayManager.DisplayListener, VivoDisplayStateManager.DisplayContentListener {
        private static final String DISPLAY_EVENT_ADD = "display added";
        private static final String DISPLAY_EVENT_CHANGED = "display changed";
        private static final String DISPLAY_EVENT_REMOVE = " display removed";
        private int mDisplayId;
        private DisplayManager mDisplayManager;
        private VivoDisplayStateManager mVivoDisplayStateManager;
        private int mDisplayRotation = -1;
        private int mBrightness = 1;

        private void updateDisplayState(Display display) {
            int rotation = display.getRotation();
            VSlog.d(CapacityKeyService.TAG, "mDisplayId =" + this.mDisplayId + " rotation = " + rotation + " mBrightness =" + this.mBrightness);
            if (this.mDisplayRotation != rotation) {
                if (rotation == 1) {
                    CapacityKeyService.this.SetServiceState("ROTATION:0".getBytes());
                } else if (rotation == 3) {
                    CapacityKeyService.this.SetServiceState("ROTATION:2".getBytes());
                } else {
                    CapacityKeyService.this.SetServiceState("ROTATION:1".getBytes());
                }
                this.mDisplayRotation = rotation;
            }
        }

        public void setBrightness(int brightness) {
            Display display = this.mDisplayManager.getDisplay(this.mDisplayId);
            this.mBrightness = brightness;
            if (display != null) {
                updateDisplayState(display);
            }
        }

        public DisplayMonitor(Context context, int displayId) {
            this.mDisplayManager = (DisplayManager) context.getSystemService("display");
            VivoDisplayStateManager vivoDisplayStateManager = (VivoDisplayStateManager) context.getSystemService("vivo_display_state");
            this.mVivoDisplayStateManager = vivoDisplayStateManager;
            this.mDisplayId = displayId;
            DisplayManager displayManager = this.mDisplayManager;
            if (displayManager == null || vivoDisplayStateManager == null) {
                return;
            }
            Display display = displayManager.getDisplay(displayId);
            if (display != null) {
                updateDisplayState(display);
            }
            HandlerThread ht = new HandlerThread("DisplayMonitor handler");
            ht.start();
            this.mDisplayManager.registerDisplayListener(this, new Handler(ht.getLooper()));
            this.mVivoDisplayStateManager.registerDisplayContentListener(this);
        }

        private void onProcessDisplayEvent(String event, int displayId) {
            Display display = this.mDisplayManager.getDisplay(displayId);
            if (display == null) {
                VSlog.d(CapacityKeyService.TAG, "display is null, event:" + event + " displayId:" + displayId + " mDisplayId:" + this.mDisplayId);
                return;
            }
            updateDisplayState(display);
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayRemoved(int displayId) {
            onProcessDisplayEvent(DISPLAY_EVENT_REMOVE, displayId);
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayAdded(int displayId) {
            onProcessDisplayEvent(DISPLAY_EVENT_ADD, displayId);
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayChanged(int displayId) {
            onProcessDisplayEvent(DISPLAY_EVENT_CHANGED, displayId);
        }

        public void onDisplayContentChanged(int displayId, boolean globalVisible, String module, boolean moduleVisible) {
        }

        public void onListenerRegistered(List<VivoDisplayModule> primaryDisplayContent, List<VivoDisplayModule> secondaryDisplayContent) {
        }
    }

    public void CapacityLcdBacklightStateSet(boolean isScreenOn) {
        if (isScreenOn) {
            if (!this.last_lcd_state) {
                SetServiceState("lcd_std:1".getBytes());
            }
        } else {
            SetServiceState("lcd_std:0".getBytes());
        }
        this.last_lcd_state = isScreenOn;
    }

    public CapacityKeyService(Context context) {
        this.mFactorySwitch = false;
        VSlog.d(TAG, "CapacityKeyService constructor");
        this.mContext = context;
        nativeInitCapacityKey();
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("is_game_mode"), false, this.mGameModeObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("is_game_space"), false, this.mGameModeObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor(PRESS_ABS), false, this.mGameModeObserver);
        ((TelephonyManager) context.getSystemService("phone")).listen(this.mPhoneStateListener, 32);
        this.mMainDisplayMonitor = new DisplayMonitor(context, 0);
        HandlerThread hThread = new HandlerThread("SensitivityThread");
        hThread.start();
        this.handler = new Handler(hThread.getLooper()) { // from class: com.vivo.services.capacitykey.CapacityKeyService.2
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                int index = msg.what;
                int sensitivity = msg.arg1;
                CapacityKeyService.nativeCapacityKeySetSensitivity(index, sensitivity);
            }
        };
        boolean equals = SystemProperties.get("persist.sys.factory.mode", "no").equals("yes");
        this.mFactorySwitch = equals;
        if (equals) {
            VSlog.d(TAG, "mFactorySwitch:" + this.mFactorySwitch);
            SetServiceState("FACTORY_MODE".getBytes());
        }
        String pressAbs = getPressCoordinate(this.mContext);
        VSlog.d(TAG, "get abs :" + pressAbs);
        SetServiceState(("ABS" + pressAbs).getBytes());
        mIActivityManager = ActivityManagerNative.getDefault();
        registerProcessObserver();
    }

    public int setSensitivity(int index, int sensitivity) {
        this.handler.obtainMessage(index, sensitivity, 0).sendToTarget();
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getAppNameFromUid(int uid) {
        VSlog.d(TAG, "pakage name is " + this.mContext.getPackageManager().getNameForUid(uid) + " with :" + uid);
        return this.mContext.getPackageManager().getNameForUid(uid);
    }

    public int SetServiceState(final byte[] appName) {
        Thread thread = new Thread(new Runnable() { // from class: com.vivo.services.capacitykey.CapacityKeyService.3
            @Override // java.lang.Runnable
            public void run() {
                VSlog.d(CapacityKeyService.TAG, "called ServiceState is " + appName);
                CapacityKeyService.nativeSetServiceState(appName);
            }
        });
        thread.start();
        return 0;
    }

    private void registerProcessObserver() {
        try {
            if (mIActivityManager != null) {
                mIActivityManager.registerProcessObserver(this.mProcessObserver);
            }
        } catch (RemoteException e) {
            VSlog.d(TAG, "registerProcessObserver failed.");
        }
    }

    public static boolean isInGameMode(Context context) {
        ContentResolver resolver = context.getContentResolver();
        int gameModeValue = Settings.System.getInt(resolver, "is_game_mode", 0);
        return 1 == gameModeValue;
    }

    public static boolean isInGameSpace(Context context) {
        ContentResolver resolver = context.getContentResolver();
        int gameSpaceValue = Settings.Global.getInt(resolver, "is_game_space", 0);
        return 1 == gameSpaceValue;
    }

    public static String getPressCoordinate(Context context) {
        ContentResolver resolver = context.getContentResolver();
        String press_coordinate = Settings.Global.getString(resolver, PRESS_ABS);
        return press_coordinate;
    }
}