package com.android.server;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.IBluetoothManagerCallback;
import android.bluetooth.IBluetoothStateChangeCallback;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.os.Binder;
import android.os.FtBuild;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import com.android.server.am.VivoFrozenPackageSupervisor;
import com.android.vivo.tws.vivotws.IVivoFwTws;
import com.vivo.face.common.data.Constants;
import com.vivo.services.security.client.VivoPermissionManager;
import vivo.app.VivoFrameworkFactory;
import vivo.app.vgc.AbsVivoVgcManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoBluetoothManagerServiceImpl implements IVivoBluetoothManagerService {
    private static final String BLUETOOTH_ACTIVATION_POLICY = "bluetooth_activation_policy";
    private static final String CBS_BLUETOOTH_ACTIVATION_OVERRIDE = "bluetooth_activation_override";
    private static final String CBS_BLUETOOTH_SERVICE_MODULE_ID = "com.vivo.bluetooth_module_id";
    private static final String CBS_UPDATE_RES_ACTION = "vivo.intent.action.CBS_UPDATE_RES";
    private static final int MSG_CONNECT_TWS = 1;
    private static final String TAG = "VivoBluetoothManagerServiceImpl";
    public static final String USER_TURNON_OFF_BLUETOOTH = "user_turn_on_off_bluetooth";
    private static Context mContext;
    public static DevicePolicyManager mDevicePolicyManager = null;
    private ActivityManager mActivityManager;
    private IntentFilter mBluetoothCBSIntentFilter;
    private VivoBluetoothCBSReceiver mBluetoothCBSReceiver;
    private BluetoothManagerService mBluetoothManagerService;
    private ComponentName mComponentName;
    private volatile IVivoFwTws mVivoFwTwsService;
    private final String TWS_PKG_NAME = "com.android.vivo.tws.vivotws";
    private final String TWS_PKG_NAME_VOS = "com.vivo.tws.vivotws";
    private final String CLS_NAME = "com.android.vivo.tws.vivotws.service.VivoAdapterService";
    private int mRetryCount = 0;
    private final int MAX_RETRY_COUNT = 3;
    private int currentUser = 1;
    private AbsVivoVgcManager mVgcSdkManager = null;
    private ServiceConnection mTwsConnection = new ServiceConnection() { // from class: com.android.server.VivoBluetoothManagerServiceImpl.1
        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            VSlog.d(VivoBluetoothManagerServiceImpl.TAG, "onServiceConnected " + name);
            if (service != null) {
                try {
                    VivoBluetoothManagerServiceImpl.this.mVivoFwTwsService = IVivoFwTws.Stub.asInterface(service);
                    if (VivoBluetoothManagerServiceImpl.this.mVivoFwTwsService != null) {
                        VivoBluetoothManagerServiceImpl.this.mTwsHandler.removeMessages(1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            VivoBluetoothManagerServiceImpl.this.mRetryCount = 0;
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            VSlog.d(VivoBluetoothManagerServiceImpl.TAG, "onServiceDisconnected " + name);
            VivoBluetoothManagerServiceImpl.this.mVivoFwTwsService = null;
            VivoBluetoothManagerServiceImpl.this.mTwsHandler.removeMessages(1);
            VivoBluetoothManagerServiceImpl.this.mTwsHandler.sendEmptyMessage(1);
        }
    };
    private Handler mTwsHandler = new Handler() { // from class: com.android.server.VivoBluetoothManagerServiceImpl.2
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            VSlog.d(VivoBluetoothManagerServiceImpl.TAG, "handleMessage, msg.what:" + msg.what);
            if (msg.what == 1) {
                VSlog.d(VivoBluetoothManagerServiceImpl.TAG, "handleMessage MSG_CONNECT_TWS");
                try {
                    VivoBluetoothManagerServiceImpl.this.bindVivoFwTwsService(false);
                    if (VivoBluetoothManagerServiceImpl.this.mVivoFwTwsService == null) {
                        VivoBluetoothManagerServiceImpl.access$208(VivoBluetoothManagerServiceImpl.this);
                        VivoBluetoothManagerServiceImpl.this.mTwsHandler.sendEmptyMessageDelayed(1, VivoBluetoothManagerServiceImpl.this.mRetryCount * 20 * 1000);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    static /* synthetic */ int access$208(VivoBluetoothManagerServiceImpl x0) {
        int i = x0.mRetryCount;
        x0.mRetryCount = i + 1;
        return i;
    }

    public VivoBluetoothManagerServiceImpl(BluetoothManagerService bluetoothManagerService) {
        this.mBluetoothManagerService = bluetoothManagerService;
    }

    public void iskeepFrozen(Object cookie) {
        if (cookie == null) {
            return;
        }
        try {
            Integer callingUid = (Integer) cookie;
            if (callingUid.intValue() >= 10000) {
                String[] packageNames = AppGlobals.getPackageManager().getPackagesForUid(callingUid.intValue());
                if (ArrayUtils.isEmpty(packageNames)) {
                    return;
                }
                String packageName = packageNames[0];
                VivoFrozenPackageSupervisor instance = VivoFrozenPackageSupervisor.getInstance();
                if (instance != null && instance.isEnableFunction()) {
                    instance.isKeepFrozen(packageName, callingUid.intValue(), null, -1, 1, true, "bluetooth state change");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void expandMsg(Message msg) {
        if (msg != null) {
            msg.arg1 = Binder.getCallingUid();
        }
    }

    public void registerStateCallback(RemoteCallbackList<IBluetoothStateChangeCallback> stateChangeCallbacks, IBluetoothStateChangeCallback callback, Message msg) {
        if (stateChangeCallbacks == null || callback == null || msg == null) {
            return;
        }
        Integer callingUid = Integer.valueOf(msg.arg1);
        stateChangeCallbacks.register(callback, callingUid);
    }

    public void registerCallback(RemoteCallbackList<IBluetoothManagerCallback> mCallbacks, IBluetoothManagerCallback callback, Message msg) {
        if (mCallbacks == null || callback == null || msg == null) {
            return;
        }
        Integer callingUid = Integer.valueOf(msg.arg1);
        mCallbacks.register(callback, callingUid);
    }

    public boolean checkCallingVivoPermission() {
        return VivoPermissionManager.checkCallingVivoPermission("android.permission.BLUETOOTH");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void bindVivoFwTwsService(boolean force) {
        VSlog.d(TAG, "bindVivoFwTwsService force:" + force);
        try {
            if (this.mVivoFwTwsService == null || force) {
                Intent intent = new Intent();
                if ("vos".equals(FtBuild.getOsName())) {
                    this.mComponentName = new ComponentName("com.vivo.tws.vivotws", "com.android.vivo.tws.vivotws.service.VivoAdapterService");
                } else {
                    this.mComponentName = new ComponentName("com.android.vivo.tws.vivotws", "com.android.vivo.tws.vivotws.service.VivoAdapterService");
                }
                intent.setComponent(this.mComponentName);
                mContext.bindService(intent, this.mTwsConnection, 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean enableTwsApplication(Context context) {
        mContext = context;
        this.mTwsHandler.sendEmptyMessageDelayed(1, 5000L);
        return true;
    }

    public boolean isThailandComercials() {
        String comercial = SystemProperties.get("ro.vivo.phonelock.enabled", "NULL");
        SystemProperties.get("ro.product.country.region", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        boolean isThailandComercial = "1".equals(comercial);
        int phoneLock = 0;
        try {
            phoneLock = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoPhoneLockManager().isPhoneLockedEnable();
        } catch (Exception e) {
            VSlog.e(TAG, "isPhoneLockedEnable err: " + e.getMessage());
        }
        VSlog.d(TAG, "phoneLock is" + phoneLock);
        return isThailandComercial || phoneLock == 1;
    }

    public void registerObserverForGn(Context context) {
        mContext = context;
        ContentObserver mContentObserverForBluetooth = new ContentObserver(null) { // from class: com.android.server.VivoBluetoothManagerServiceImpl.3
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                VSlog.d(VivoBluetoothManagerServiceImpl.TAG, "debug for observer change!");
                if (VivoBluetoothManagerServiceImpl.this.isThailandComercials()) {
                    int currentUser = ActivityManager.getCurrentUser();
                    VivoBluetoothManagerServiceImpl.this.mBluetoothManagerService.mCtBluetoothCommand = Settings.Secure.getIntForUser(VivoBluetoothManagerServiceImpl.mContext.getContentResolver(), "ct_network_bluetooth", 1, currentUser);
                    VSlog.d(VivoBluetoothManagerServiceImpl.TAG, "CtBluetoothCommand is " + VivoBluetoothManagerServiceImpl.this.mBluetoothManagerService.mCtBluetoothCommand);
                    if (VivoBluetoothManagerServiceImpl.this.mBluetoothManagerService.mCtBluetoothCommand == 0) {
                        if (VivoBluetoothManagerServiceImpl.this.mBluetoothManagerService.mState == 12 || VivoBluetoothManagerServiceImpl.this.mBluetoothManagerService.mState == 11) {
                            try {
                                VivoBluetoothManagerServiceImpl.this.mBluetoothManagerService.disable(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, true);
                                VSlog.d(VivoBluetoothManagerServiceImpl.TAG, "debug for sendDisableMsg");
                            } catch (Exception e) {
                                VSlog.d(VivoBluetoothManagerServiceImpl.TAG, "registerObserverForGn: disable bluetooth failed!");
                            }
                        }
                    } else if (4 == VivoBluetoothManagerServiceImpl.this.mBluetoothManagerService.mCtBluetoothCommand) {
                        if (VivoBluetoothManagerServiceImpl.this.mBluetoothManagerService.mState == 10 || VivoBluetoothManagerServiceImpl.this.mBluetoothManagerService.mState == 13) {
                            try {
                                VivoBluetoothManagerServiceImpl.this.mBluetoothManagerService.enable(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                                VSlog.d(VivoBluetoothManagerServiceImpl.TAG, "debug for sendEnableMsg");
                            } catch (Exception e2) {
                                VSlog.d(VivoBluetoothManagerServiceImpl.TAG, "registerObserverForGn: enable bluetooth failed!");
                            }
                        }
                    }
                }
            }
        };
        mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("ct_network_bluetooth"), false, mContentObserverForBluetooth);
    }

    public boolean isCustomSDKRestrict() {
        if (mDevicePolicyManager == null) {
            mDevicePolicyManager = (DevicePolicyManager) mContext.getSystemService("device_policy");
        }
        if (mDevicePolicyManager != null) {
            long callingId = Binder.clearCallingIdentity();
            try {
                int type = mDevicePolicyManager.getCustomType();
                if (type > 0) {
                    int currentUser = ActivityManager.getCurrentUser();
                    this.currentUser = currentUser;
                    int state = mDevicePolicyManager.getRestrictionPolicy(null, 5, currentUser);
                    if (state == 2) {
                        return true;
                    }
                }
            } catch (Exception e) {
                return false;
            } finally {
                Binder.restoreCallingIdentity(callingId);
            }
        }
        return false;
    }

    public void registerObserverForCBS(Context context) {
        mContext = context;
        this.mBluetoothCBSIntentFilter = new IntentFilter();
        this.mBluetoothCBSReceiver = new VivoBluetoothCBSReceiver();
        this.mBluetoothCBSIntentFilter.addAction(CBS_UPDATE_RES_ACTION);
        this.mVgcSdkManager = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoVgcManager();
        mContext.registerReceiver(this.mBluetoothCBSReceiver, this.mBluetoothCBSIntentFilter);
    }

    /* loaded from: classes.dex */
    class VivoBluetoothCBSReceiver extends BroadcastReceiver {
        VivoBluetoothCBSReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            Slog.d(VivoBluetoothManagerServiceImpl.TAG, "VivoBluetoothCBSReceiver cbs update receiver!");
            String action = intent.getAction();
            if (VivoBluetoothManagerServiceImpl.CBS_UPDATE_RES_ACTION.equals(action)) {
                VSlog.d(VivoBluetoothManagerServiceImpl.TAG, "VivoBluetoothCBSReceiver action equal CBS_UPDATE_RES_ACTION");
                if (VivoBluetoothManagerServiceImpl.this.mVgcSdkManager == null) {
                    VivoBluetoothManagerServiceImpl.this.mVgcSdkManager = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoVgcManager();
                }
                if (VivoBluetoothManagerServiceImpl.this.mVgcSdkManager != null) {
                    VSlog.d(VivoBluetoothManagerServiceImpl.TAG, "VivoBluetoothCBSReceiver module change updateBluetoothStatePolicy!");
                    VivoBluetoothManagerServiceImpl.this.updateBluetoothStatePolicy();
                }
            }
        }
    }

    public void updateBluetoothStatePolicy() {
        VSlog.d(TAG, "updateBluetoothStatePolicy start!");
        int vgcBluetoothState = this.mVgcSdkManager.getInt(BLUETOOTH_ACTIVATION_POLICY, 10);
        boolean isBluetoothActivationOverride = this.mVgcSdkManager.getBool(CBS_BLUETOOTH_ACTIVATION_OVERRIDE, false);
        if (this.mBluetoothManagerService.mState != vgcBluetoothState) {
            if (getUserTurnOnOffBluetooth() || isBluetoothActivationOverride) {
                VSlog.d(TAG, "change default bluetooth state!");
                if (this.mBluetoothManagerService.mState == 12) {
                    this.mBluetoothManagerService.sendDisableMsg(9, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                } else {
                    this.mBluetoothManagerService.sendEnableMsg(false, 9, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                }
            }
        }
    }

    public void setUserTurnOnOffBluetooth() {
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            VSlog.d(TAG, "setUserTurnOnOffBluetooth start");
            Settings.Global.putInt(mContext.getContentResolver(), USER_TURNON_OFF_BLUETOOTH, 12);
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    public boolean getUserTurnOnOffBluetooth() {
        int bluetoothDefaultState = Settings.Global.getInt(mContext.getContentResolver(), USER_TURNON_OFF_BLUETOOTH, 10);
        boolean userTurnOffBluetooth = bluetoothDefaultState == 12;
        VSlog.d(TAG, "getUserTurnOnOffBluetooth userTurnOffBluetooth = " + userTurnOffBluetooth);
        return userTurnOffBluetooth;
    }
}