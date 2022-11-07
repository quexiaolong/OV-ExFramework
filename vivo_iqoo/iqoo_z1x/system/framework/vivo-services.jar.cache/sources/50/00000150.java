package com.android.server.biometrics.face;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.os.NativeHandle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.text.TextUtils;
import com.android.internal.widget.LockPatternUtils;
import com.vivo.face.common.data.Constants;
import com.vivo.face.common.utils.SettingUtils;
import java.io.FileDescriptor;
import vendor.vivo.hardware.biometrics.face.V2_0.IBiometricsFace;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoFaceServiceImpl implements IVivoFaceService {
    private static final String ANDROID_VERSION = SystemProperties.get("ro.build.version.release", "9");
    private static final int COMPARE_FAIL = -35;
    private static final boolean DEBUG = true;
    private static final int FACE_SUCCESS = 0;
    private static final int LIVENESS_FAILURE = -34;
    private static final String PKG_ALIPAY = "com.eg.android.AlipayGphone";
    private static final String PKG_WECAHT = "com.tencent.mm";
    private static final String TAG = "VivoFaceServiceImpl";
    private Context mContext;
    private Handler mHandler;
    private boolean mIsNativeHandle = false;
    private LockPatternUtils mLockUtils;
    private FaceService mService;
    private FaceUIManagerServiceAdapter mServiceAdapter;

    public VivoFaceServiceImpl(Context context, FaceService service, Handler handler) {
        this.mContext = context;
        this.mService = service;
        this.mHandler = handler;
        this.mServiceAdapter = new FaceUIManagerServiceAdapter(context);
    }

    private IBiometricsFace getFaceDaemon() {
        return this.mService.getFaceDaemon();
    }

    private void checkPermission(String permission) {
        Context context = this.mContext;
        context.enforceCallingOrSelfPermission(permission, "Must have " + permission + " permission.");
    }

    public FileDescriptor getShareMemoryFd(int memorySize, String opPackageName) {
        checkPermission("android.permission.MANAGE_BIOMETRIC");
        FaceUIManagerServiceAdapter faceUIManagerServiceAdapter = this.mServiceAdapter;
        if (faceUIManagerServiceAdapter != null) {
            return faceUIManagerServiceAdapter.getShareMemoryFd(memorySize, opPackageName);
        }
        return null;
    }

    public void processAuthenticationOnStart(IBinder token, String opPackageName) {
        FaceUIManagerServiceAdapter faceUIManagerServiceAdapter = this.mServiceAdapter;
        if (faceUIManagerServiceAdapter != null) {
            faceUIManagerServiceAdapter.startAuthenticate(token, opPackageName);
        }
        if (TextUtils.equals("com.tencent.mm", opPackageName) || TextUtils.equals(PKG_ALIPAY, opPackageName)) {
            sendCommandInternal(Constants.CMD.CMD_HIDL_PAYMENT_AUTH, 0, "payment");
        }
    }

    public void processAuthenticationOnStop(IBinder token, String opPackageName) {
        FaceUIManagerServiceAdapter faceUIManagerServiceAdapter = this.mServiceAdapter;
        if (faceUIManagerServiceAdapter != null) {
            faceUIManagerServiceAdapter.cancelAuthenticate(token, opPackageName);
        }
    }

    public void processAuthenticationOnError(IBinder token, String opPackageName) {
        FaceUIManagerServiceAdapter faceUIManagerServiceAdapter = this.mServiceAdapter;
        if (faceUIManagerServiceAdapter != null) {
            faceUIManagerServiceAdapter.cancelAuthenticate(token, opPackageName);
        }
    }

    public void onAcquired(int acquiredInfo, int vendorCode) {
        FaceUIManagerServiceAdapter faceUIManagerServiceAdapter = this.mServiceAdapter;
        if (faceUIManagerServiceAdapter != null) {
            faceUIManagerServiceAdapter.onAcquired(acquiredInfo, vendorCode);
        }
    }

    public void onAuthenticationSucceeded(int faceId, int userId) {
        FaceUIManagerServiceAdapter faceUIManagerServiceAdapter = this.mServiceAdapter;
        if (faceUIManagerServiceAdapter != null) {
            faceUIManagerServiceAdapter.onAuthenticationSucceeded(faceId, userId);
        }
    }

    public void onAuthenticationFailed() {
        FaceUIManagerServiceAdapter faceUIManagerServiceAdapter = this.mServiceAdapter;
        if (faceUIManagerServiceAdapter != null) {
            faceUIManagerServiceAdapter.onAuthenticationFailed();
        }
    }

    public void onError(int error, int vendorCode) {
        FaceUIManagerServiceAdapter faceUIManagerServiceAdapter = this.mServiceAdapter;
        if (faceUIManagerServiceAdapter != null) {
            faceUIManagerServiceAdapter.onError(error, vendorCode);
        }
    }

    public void sendCommand(int command, int extra, String bundle) {
        checkPermission("android.permission.MANAGE_BIOMETRIC");
        sendCommandInternal(command, extra, bundle);
    }

    public void onFaceAlgorithmResult(int command, int result, int extras, String bundle) {
        FaceUIManagerServiceAdapter faceUIManagerServiceAdapter = this.mServiceAdapter;
        if (faceUIManagerServiceAdapter != null) {
            faceUIManagerServiceAdapter.onFaceAlgorithmResult(command, result, extras, bundle);
        }
    }

    public void doFaceAndroidUpdate() {
        int ardUpdateState = SystemProperties.getInt(Constants.ArdUpdate.PROP_ARD_UPDATE, 0);
        VSlog.i(TAG, "android update state " + ardUpdateState);
        if (1 != ardUpdateState) {
            return;
        }
        int switchState = SettingUtils.getSystemSettingInt(this.mContext, Constants.Setting.ARD9_FACE_UNLOCK_KEYGUARD_ENABLED, 0);
        VSlog.i(TAG, "face_unlock_keyguard_enabled: " + switchState);
        if (1 == switchState) {
            SettingUtils.putSecureSettingInt(this.mContext, Constants.Setting.FACE_UNLOCK_KEYGUARD_ENABLED, 1);
        }
        int switchState2 = SettingUtils.getSystemSettingInt(this.mContext, Constants.Setting.ARD9_FACE_UNLOCK_PRIVACY_ENABLED_1ST, 0);
        int switchState2nd = SettingUtils.getSystemSettingInt(this.mContext, Constants.Setting.ARD9_FACE_UNLOCK_PRIVACY_ENABLED_2ND, 0);
        VSlog.i(TAG, "face_unlock_privacy_enabled1: " + switchState2 + " face_unlock_privacy_enabled2: " + switchState2nd);
        if (1 == switchState2 && 1 == switchState2nd) {
            SettingUtils.putSecureSettingInt(this.mContext, Constants.Setting.FACE_UNLOCK_PRIVACY_ENABLED, 1);
        }
        int switchState3 = SettingUtils.getSystemSettingInt(this.mContext, "faceunlock_assisstant_enabled", 0);
        VSlog.i(TAG, "face_unlock_assistant_enabled: " + switchState3);
        if (1 == switchState3) {
            SettingUtils.putSecureSettingInt(this.mContext, "faceunlock_assisstant_enabled", 1);
        }
        int switchState4 = SettingUtils.getSystemSettingInt(this.mContext, "faceunlock_adjust_screen_brightness", 0);
        VSlog.i(TAG, "face_unlock_adjust_screen_brightness_enabled: " + switchState4);
        if (1 == switchState4) {
            SettingUtils.putSecureSettingInt(this.mContext, "faceunlock_adjust_screen_brightness", 1);
        }
        int switchState5 = SettingUtils.getSystemSettingInt(this.mContext, "faceunlock_keyguard_keep", 0);
        VSlog.i(TAG, "face_unlock_keyguard_keep_enabled: " + switchState5);
        if (1 == switchState5) {
            SettingUtils.putSecureSettingInt(this.mContext, "faceunlock_keyguard_keep", 1);
        }
        int switchState6 = SettingUtils.getSystemSettingInt(this.mContext, "finger_face_combine", 0);
        VSlog.i(TAG, "face_unlock_face_finger_enabled: " + switchState6);
        if (1 == switchState6) {
            SettingUtils.putSecureSettingInt(this.mContext, "finger_face_combine", 1);
        }
        int switchState7 = SettingUtils.getSystemSettingInt(this.mContext, "faceunlock_attention_focus", 0);
        VSlog.i(TAG, "face_unlock_attention_focus_enabled: " + switchState7);
        if (1 == switchState7) {
            SettingUtils.putSecureSettingInt(this.mContext, "faceunlock_attention_focus", 1);
        }
        int switchState8 = SettingUtils.getSystemSettingInt(this.mContext, Constants.Setting.ARD9_FACE_UNLOCK_SCREEN_OFF, 0);
        VSlog.i(TAG, "face_unlock_screen_off_enabled: " + switchState8);
        if (1 == switchState8) {
            SettingUtils.putSecureSettingInt(this.mContext, Constants.Setting.FACE_UNLOCK_SCREEN_OFF, 1);
        }
        int switchState9 = SettingUtils.getSystemSettingInt(this.mContext, "faceunlock_popup_camera_unlock_by", 0);
        VSlog.i(TAG, "face_unlock_popup_camera_start_by_power: " + switchState9);
        SettingUtils.putSecureSettingInt(this.mContext, "faceunlock_popup_camera_unlock_by", 1);
    }

    public boolean ardVersionUpdated(boolean updateAllowed) {
        int ardUpdateState = SystemProperties.getInt(Constants.ArdUpdate.PROP_ARD_UPDATE, 0);
        if (ardUpdateState != 0 || updateAllowed) {
            int preArdVersion = getPreUpdateArdVersion();
            if (9 != preArdVersion && 10 != preArdVersion) {
                VSlog.w(TAG, "preArdVersion " + preArdVersion + " illegal");
                return false;
            } else if (!ANDROID_VERSION.matches("[0-9]+")) {
                VSlog.e(TAG, "ardVersion contains letters of non-digit");
                return false;
            } else {
                int curArdVersion = Integer.parseInt(ANDROID_VERSION);
                if (preArdVersion == curArdVersion) {
                    VSlog.d(TAG, "android version has not been updated");
                    return false;
                }
                VSlog.d(TAG, "preArdVersion: " + preArdVersion + " curArdVersion: " + curArdVersion + " ardUpdateState: " + ardUpdateState);
                if (ardUpdateState == 0) {
                    SystemProperties.set(Constants.ArdUpdate.PROP_ARD_UPDATE, String.valueOf(1));
                    return isKeyguardSecure();
                } else if (1 == ardUpdateState) {
                    SystemProperties.set(Constants.ArdUpdate.PROP_ARD_UPDATE, String.valueOf(2));
                    return false;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    public IBinder getFaceUIBinder() {
        FaceUIManagerServiceAdapter faceUIManagerServiceAdapter = this.mServiceAdapter;
        if (faceUIManagerServiceAdapter != null) {
            return faceUIManagerServiceAdapter.asBinder();
        }
        return null;
    }

    private boolean isKeyguardSecure() {
        if (this.mLockUtils == null) {
            this.mLockUtils = new LockPatternUtils(this.mContext);
        }
        boolean lockSecure = false;
        LockPatternUtils lockPatternUtils = this.mLockUtils;
        if (lockPatternUtils != null) {
            lockSecure = lockPatternUtils.isSecure(ActivityManager.getCurrentUser());
        }
        VSlog.d(TAG, "lockSecure: " + lockSecure);
        return lockSecure;
    }

    public void onHidlServiceDied() {
        FaceUIManagerServiceAdapter faceUIManagerServiceAdapter = this.mServiceAdapter;
        if (faceUIManagerServiceAdapter != null) {
            faceUIManagerServiceAdapter.onHidlServiceDied();
        }
        this.mIsNativeHandle = false;
    }

    public void onRemoved() {
        FaceUIManagerServiceAdapter faceUIManagerServiceAdapter = this.mServiceAdapter;
        if (faceUIManagerServiceAdapter != null) {
            faceUIManagerServiceAdapter.onRemoved();
        }
    }

    public void onEnrollmentStart() {
        FaceUIManagerServiceAdapter faceUIManagerServiceAdapter = this.mServiceAdapter;
        if (faceUIManagerServiceAdapter != null) {
            faceUIManagerServiceAdapter.onEnrollmentStateChanged(true);
        }
    }

    public void onEnrollmentStop() {
        FaceUIManagerServiceAdapter faceUIManagerServiceAdapter = this.mServiceAdapter;
        if (faceUIManagerServiceAdapter != null) {
            faceUIManagerServiceAdapter.onEnrollmentStateChanged(false);
        }
    }

    public boolean getLockoutState() {
        FaceUIManagerServiceAdapter faceUIManagerServiceAdapter = this.mServiceAdapter;
        if (faceUIManagerServiceAdapter != null) {
            return faceUIManagerServiceAdapter.isLockout();
        }
        return false;
    }

    public void onSystemTime(long elapsedRealtime, int what) {
        FaceUIManagerServiceAdapter faceUIManagerServiceAdapter = this.mServiceAdapter;
        if (faceUIManagerServiceAdapter != null) {
            faceUIManagerServiceAdapter.onSystemTime(elapsedRealtime, what);
        }
    }

    public boolean getResultType(int result) {
        if (result == COMPARE_FAIL || result == LIVENESS_FAILURE || result == 0) {
            return true;
        }
        return false;
    }

    private void sendCommandInternal(int command, int extra, String bundle) {
        VSlog.i(TAG, "sendCommand: cmd: " + command + " extra: " + extra);
        if (isNativeCommand(command)) {
            sendNativeCommand(command, extra, 0, bundle);
        } else {
            sendCommonCommand(command, extra, bundle);
        }
    }

    private boolean isNativeCommand(int command) {
        if (command == 1007 || command == 1008 || command == 1010) {
            return false;
        }
        return true;
    }

    private void sendCommonCommand(int command, int extra, String bundle) {
        FaceUIManagerServiceAdapter faceUIManagerServiceAdapter = this.mServiceAdapter;
        if (faceUIManagerServiceAdapter != null) {
            faceUIManagerServiceAdapter.sendCommand(command, extra, bundle);
        }
    }

    private void sendNativeCommand(final int command, final int extra, final int param, final String bundle) {
        final IBiometricsFace daemon = getFaceDaemon();
        if (daemon != null) {
            try {
                if (command == 1002) {
                    if (!this.mIsNativeHandle) {
                        this.mIsNativeHandle = true;
                        NativeHandle windowId = this.mServiceAdapter.getNativeHandle();
                        if (windowId != null) {
                            daemon.sendhandle(windowId);
                        }
                    }
                    daemon.sendCommand(command, extra, param, "face");
                    return;
                }
                this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.face.-$$Lambda$VivoFaceServiceImpl$dhR-lyNNEmTDSIq2xeirdSwvClQ
                    @Override // java.lang.Runnable
                    public final void run() {
                        VivoFaceServiceImpl.lambda$sendNativeCommand$0(bundle, daemon, command, extra, param);
                    }
                });
            } catch (RemoteException e) {
                this.mIsNativeHandle = false;
                VSlog.e(TAG, "Unable to send command");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$sendNativeCommand$0(String bundle, IBiometricsFace daemon, int command, int extra, int param) {
        try {
            if (bundle == null) {
                daemon.sendCommand(command, extra, param, "face");
            } else {
                daemon.sendCommand(command, extra, param, bundle);
            }
        } catch (RemoteException e) {
            VSlog.e(TAG, "post Unable to send command");
        }
    }

    private int getPreUpdateArdVersion() {
        IBiometricsFace daemon = getFaceDaemon();
        if (daemon == null) {
            VSlog.e(TAG, "face HIDL not available");
            return -1;
        }
        try {
            return daemon.sendCommand((int) Constants.CMD.CMD_HIDL_GET_PRE_UPDATE_ARD_VERSION, 0, 0, "facedetect");
        } catch (RemoteException e) {
            VSlog.e(TAG, "Failed to get the android version of previous software");
            return -1;
        }
    }
}