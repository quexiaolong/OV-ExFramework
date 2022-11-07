package com.android.server.biometrics.fingerprint;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.biometrics.BiometricAuthenticator;
import android.hardware.biometrics.fingerprint.FingerprintKeyguardInternal;
import android.hardware.biometrics.fingerprint.FingerprintUIManagerInternal;
import android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.IFingerprintServiceReceiver;
import android.os.FtBuild;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;
import com.android.server.LocalServices;
import com.android.server.VivoDoubleInstanceServiceImpl;
import com.android.server.biometrics.AuthenticationClient;
import com.android.server.biometrics.BiometricServiceBase;
import com.android.server.biometrics.ClientMonitor;
import com.android.server.biometrics.Constants;
import com.android.server.biometrics.fingerprint.FingerprintService;
import com.vivo.face.common.data.Constants;
import com.vivo.fingerprint.FingerprintConfig;
import com.vivo.fingerprint.FingerprintConstants;
import com.vivo.fingerprint.analysis.AnalysisManager;
import com.vivo.services.security.server.VivoPermissionUtils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoFingerprintServiceImpl implements IVivoFingerprintService {
    private static final boolean DEBUG = true;
    private static final int ENROLL_TIMEOUT_SEC = 7200;
    private static final int ENROLL_TIMEOUT_VOS_SEC = 60;
    private static final String HAPTIC_FEEDBACK_ENABLED = "haptic_feedback_enabled";
    private static final String SETTING_FINGERPRINT_PRIVACY_ENCRYPTION = "finger_secure_open";
    private static final String SETTING_FINGERPRINT_UNLOCK = "finger_unlock_open";
    private static final String TAG = "VivoFingerprintServiceImpl";
    private String mAnalysisAcquiredInfo;
    private Context mContext;
    private Handler mHandler;
    private FingerprintService mService;
    private VivoDoubleInstanceServiceImpl mVivoDoubleInstanceService;
    private static final String LINEAR_VIBRATE = SystemProperties.get("persist.vivo.support.lra", "0");
    private static final String[] sThirdPartPackageWhiteList = new String[0];
    private static final String[] sVendorMessageWhiteList = {VivoPermissionUtils.OS_PKG, "com.vivo.fingerprintui", "com.vivo.fingerprint", "com.vivo.fingerprintengineer"};
    private boolean mForbidCancelCmd = false;
    private FingerprintUIManagerInternal mFingerprintUIManager = (FingerprintUIManagerInternal) LocalServices.getService(FingerprintUIManagerInternal.class);
    private AnalysisManager mAnalysisManager = AnalysisManager.get();

    public VivoFingerprintServiceImpl(Context context, FingerprintService service, Handler handler) {
        this.mVivoDoubleInstanceService = null;
        this.mContext = context;
        this.mHandler = handler;
        this.mService = service;
        this.mVivoDoubleInstanceService = VivoDoubleInstanceServiceImpl.getInstance();
    }

    public void authenticate(long operationId, int groupId) {
        if (this.mFingerprintUIManager != null) {
            ClientMonitor client = getCurrentClient();
            this.mFingerprintUIManager.showFingerprintDialog(client.getOwnerString(), 1, false);
        }
    }

    public void clientCancel() {
        FingerprintUIManagerInternal fingerprintUIManagerInternal = this.mFingerprintUIManager;
        if (fingerprintUIManagerInternal != null) {
            fingerprintUIManagerInternal.hideFingerprintDialog(-5);
        }
    }

    public int remove(int groupId, int biometricId) {
        return 0;
    }

    public int enumerate() {
        return 0;
    }

    public void enroll(byte[] cryptoToken, int groupId, int timeout, ArrayList<Integer> disabledFeatures) {
        if (this.mFingerprintUIManager != null) {
            ClientMonitor client = getCurrentClient();
            this.mFingerprintUIManager.showFingerprintDialog(client.getOwnerString(), 2, true);
        }
    }

    public void clientDestroy() {
        FingerprintUIManagerInternal fingerprintUIManagerInternal = this.mFingerprintUIManager;
        if (fingerprintUIManagerInternal != null) {
            fingerprintUIManagerInternal.hideFingerprintDialog(0);
        }
        this.mService.setPhysicalPowerKeyDisabled(false);
    }

    public void onEnrollResult(BiometricAuthenticator.Identifier identifier, int remaining) {
    }

    public void onAcquired(long deviceId, int acquiredInfo, int vendorCode) {
        setAuthAcquiredInfo(acquiredInfo, vendorCode);
    }

    public void onAuthenticationSucceeded(long deviceId, BiometricAuthenticator.Identifier biometric, int userId) {
        AnalysisManager.trace("fraAuth");
        FingerprintUIManagerInternal fingerprintUIManagerInternal = this.mFingerprintUIManager;
        if (fingerprintUIManagerInternal != null) {
            fingerprintUIManagerInternal.onFingerprintAuthenticated(true);
        }
        ClientMonitor currentClient = getCurrentClient();
        if (currentClient != null) {
            insertAnalysisData(currentClient.getOwnerString(), "success", "GOOD");
        }
    }

    public void onAuthenticationFailed(long deviceId) {
        FingerprintUIManagerInternal fingerprintUIManagerInternal = this.mFingerprintUIManager;
        if (fingerprintUIManagerInternal != null) {
            fingerprintUIManagerInternal.onFingerprintAuthenticated(false);
        }
        ClientMonitor currentClient = getCurrentClient();
        if (currentClient != null) {
            insertAnalysisData(currentClient.getOwnerString(), "fail", this.mAnalysisAcquiredInfo);
        }
    }

    public void onError(long deviceId, int error, int vendorCode, int cookie) {
        FingerprintUIManagerInternal fingerprintUIManagerInternal = this.mFingerprintUIManager;
        if (fingerprintUIManagerInternal != null) {
            fingerprintUIManagerInternal.onFingerprintError(FingerprintManager.getErrorString(this.mContext, error, vendorCode));
        }
    }

    public void onRemoved(BiometricAuthenticator.Identifier identifier, int remaining) {
        if (remaining == 0) {
            Fingerprint fp = (Fingerprint) identifier;
            ClientMonitor currentClient = getCurrentClient();
            if (currentClient != null) {
                notifyFingerprintUIFingerprintRemoved(fp.getGroupId(), currentClient.getTargetUserId());
            }
        }
    }

    public void onEnumerated(BiometricAuthenticator.Identifier identifier, int remaining) {
    }

    public int getFingerprintSensorType() {
        checkPermission("android.permission.USE_FINGERPRINT");
        if (FingerprintConfig.isOpticalFingerprint()) {
            return 2;
        }
        if (!FingerprintConfig.isSideFingerprint()) {
            return 1;
        }
        return 3;
    }

    public boolean shouldDispatchVendorMessage(boolean isBiometricPrompt, ClientMonitor client, int vendorCode) {
        boolean shoudDispatch = false;
        if (isBiometricPrompt || client == null) {
            return false;
        }
        if (client instanceof AuthenticationClient) {
            String owner = client.getOwnerString();
            int i = 0;
            while (true) {
                String[] strArr = sVendorMessageWhiteList;
                if (i < strArr.length) {
                    if (TextUtils.equals(owner, strArr[i])) {
                        shoudDispatch = true;
                    }
                    i++;
                } else {
                    return shoudDispatch;
                }
            }
        } else {
            return true;
        }
    }

    public boolean isDoubleAppSwitcherEnable(int userId) {
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl != null && vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable() && this.mVivoDoubleInstanceService.isDoubleAppUserExist() && userId == this.mVivoDoubleInstanceService.getDoubleAppUserId()) {
            return true;
        }
        return false;
    }

    private void notifyFingerState(int extras) {
        int vendorCode;
        if (extras == 0) {
            vendorCode = 1;
        } else {
            vendorCode = 2;
        }
        onAcquired(6, vendorCode);
    }

    private void notifyFingerprintUIFingerprintRemoved(int groupId, int userId) {
        VSlog.d(TAG, "notifyFingerprintUIFingerprintRemoved groupId: " + groupId + ", userId: " + userId);
        int remain = getEnrolledFingerprintSize(userId, 3);
        FingerprintUIManagerInternal fingerprintUIManagerInternal = this.mFingerprintUIManager;
        if (fingerprintUIManagerInternal != null) {
            fingerprintUIManagerInternal.onFingerprintRemoved(remain);
        }
        FingerprintKeyguardInternal fingerprintKeyguard = (FingerprintKeyguardInternal) LocalServices.getService(FingerprintKeyguardInternal.class);
        if (fingerprintKeyguard != null) {
            fingerprintKeyguard.onFingerprintRemoved(remain);
        }
    }

    private int getEnrolledFingerprintSize(int userId, int type) {
        List<Fingerprint> fingerprints;
        FingerprintManager fingerprintManager = (FingerprintManager) this.mContext.getSystemService("fingerprint");
        if (fingerprintManager == null || (fingerprints = fingerprintManager.getEnrolledFingerprints(userId, type)) == null) {
            return 0;
        }
        return fingerprints.size();
    }

    private void setAuthAcquiredInfo(int acquiredInfo, int vendorCode) {
        int tempInfo = vendorCode + 1000;
        if (tempInfo == 1001 || tempInfo == 1002) {
            return;
        }
        if (acquiredInfo == 6) {
            acquiredInfo = vendorCode + 1000;
        }
        if (this.mFingerprintUIManager != null && (acquiredInfo == 1028 || acquiredInfo == 1029)) {
            this.mFingerprintUIManager.onFingerprintAcquired(acquiredInfo);
            return;
        }
        FingerprintUIManagerInternal fingerprintUIManagerInternal = this.mFingerprintUIManager;
        if (fingerprintUIManagerInternal != null && acquiredInfo == 1011) {
            fingerprintUIManagerInternal.onFingerprintAcquired(acquiredInfo);
        }
        VSlog.i(TAG, " setAuthAcquiredInfo acquiredInfo is: " + acquiredInfo);
        this.mAnalysisAcquiredInfo = FingerprintConstants.toStringRequire(acquiredInfo);
    }

    private void insertAnalysisData(String pkgName, String authResult, String acquiceInfo) {
        if (TextUtils.isEmpty(pkgName) || TextUtils.isEmpty(authResult)) {
            return;
        }
        if (TextUtils.isEmpty(acquiceInfo)) {
            acquiceInfo = "GOOD";
        }
        if (this.mAnalysisManager == null) {
            this.mAnalysisManager = AnalysisManager.get();
        }
        AnalysisManager.stub("package", pkgName);
        AnalysisManager.stub("authResult", authResult);
        AnalysisManager.stub("acquice", acquiceInfo);
    }

    private void onAcquired(final int acquiredInfo, final int vendorCode) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.fingerprint.-$$Lambda$VivoFingerprintServiceImpl$18FHKu-pxfRIQbXJADuEioee1Zk
            @Override // java.lang.Runnable
            public final void run() {
                VivoFingerprintServiceImpl.this.lambda$onAcquired$0$VivoFingerprintServiceImpl(acquiredInfo, vendorCode);
            }
        });
    }

    public /* synthetic */ void lambda$onAcquired$0$VivoFingerprintServiceImpl(int acquiredInfo, int vendorCode) {
        this.mService.handleAcquiredForVivo(getHalDeviceId(), acquiredInfo, vendorCode);
    }

    private BiometricServiceBase.ServiceListener createServiceListenerImpl(IFingerprintServiceReceiver receiver) {
        return this.mService.createServiceListenerImpl(receiver);
    }

    private BiometricServiceBase.DaemonWrapper getDaemonWrapper() {
        return this.mService.getDaemonWrapperForVivo();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public IBiometricsFingerprint getFingerprintDaemon() {
        return this.mService.getFingerprintDaemon();
    }

    private long getHalDeviceId() {
        return this.mService.getHalDeviceIdForVivo();
    }

    private Constants getConstants() {
        return this.mService.getConstantsForVivo();
    }

    private int getCurrentUserId() {
        return this.mService.getCurrentUserId();
    }

    private ClientMonitor getCurrentClient() {
        return this.mService.getCurrentClientForVivo();
    }

    private void startClient(final ClientMonitor client, final boolean initiatedByClient) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.fingerprint.-$$Lambda$VivoFingerprintServiceImpl$fvBWhPxleQVktl3xYsKf33Y6RYQ
            @Override // java.lang.Runnable
            public final void run() {
                VivoFingerprintServiceImpl.this.lambda$startClient$1$VivoFingerprintServiceImpl(client, initiatedByClient);
            }
        });
    }

    public /* synthetic */ void lambda$startClient$1$VivoFingerprintServiceImpl(ClientMonitor client, boolean initiatedByClient) {
        this.mService.startClientForVivo(client, initiatedByClient);
    }

    private void removeClient(ClientMonitor client) {
        this.mService.removeClientForVivo(client);
    }

    private void checkPermission(String permission) {
        Context context = this.mContext;
        context.enforceCallingOrSelfPermission(permission, "Must have " + permission + " permission.");
    }

    private void vibratorPro(int effectId) {
        Vibrator vibrator = (Vibrator) this.mContext.getSystemService(Vibrator.class);
        try {
            Class clazz = vibrator.getClass();
            Method method = clazz.getDeclaredMethod("vibratorPro", Integer.TYPE, Long.TYPE, Integer.TYPE);
            if (method != null) {
                long playMillis = ((Long) method.invoke(vibrator, Integer.valueOf(effectId), -1, -1)).longValue();
                VSlog.i(TAG, "vibratorPro effect will play millis: " + playMillis);
            }
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            VSlog.i(TAG, "not support vibratorPro");
        }
    }

    private boolean isLinearVibrate() {
        return "1".equals(LINEAR_VIBRATE);
    }

    private boolean isNeedVibrate() {
        return isLinearVibrate() && Settings.System.getInt(this.mContext.getContentResolver(), HAPTIC_FEEDBACK_ENABLED, 0) != 0;
    }

    public boolean shouldVibrateSuccess() {
        ClientMonitor client = getCurrentClient();
        if (isNeedVibrate() && !isFingerprintUI(client.getOwnerString())) {
            vibratorPro(56);
            return false;
        }
        return !isOemOwner(client.getOwnerString());
    }

    public boolean shouldVibrateError() {
        ClientMonitor client = getCurrentClient();
        if (!isNeedVibrate() || isFingerprintUI(client.getOwnerString())) {
            return !isOemOwner(client.getOwnerString()) || (FingerprintConfig.isOpticalFingerprint() && !isFingerprintUI(client.getOwnerString()));
        }
        vibratorPro(54);
        return false;
    }

    public boolean isOemOwner(String owner) {
        if (isSystemOwner(owner)) {
            return true;
        }
        int i = 0;
        while (true) {
            String[] strArr = sThirdPartPackageWhiteList;
            if (i < strArr.length) {
                if (owner.equals(strArr[i])) {
                    return true;
                }
                i++;
            } else {
                return false;
            }
        }
    }

    public boolean isSystemOwner(String owner) {
        PackageManager pm = this.mContext.getPackageManager();
        try {
            PackageInfo pkgInfo = pm.getPackageInfo(owner, 1048576);
            if (pkgInfo != null) {
                return true;
            }
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            VSlog.d(TAG, "owner reject: " + owner);
            return false;
        }
    }

    protected boolean isFingerprintUI(String clientPackage) {
        return "com.vivo.fingerprintui".equals(clientPackage);
    }

    public BiometricServiceBase.EnrollClientImpl createEnrollClient(Context context, BiometricServiceBase.DaemonWrapper daemon, long halDeviceId, IBinder token, BiometricServiceBase.ServiceListener listener, int userId, int groupId, int flags, byte[] cryptoToken, boolean restricted, String owner, int[] disabledFeatures, int timeoutSec) {
        int enrollTimeout = TextUtils.equals(FtBuild.getOsName(), "vos") ? 60 : ENROLL_TIMEOUT_SEC;
        FingerprintService fingerprintService = this.mService;
        Objects.requireNonNull(fingerprintService);
        return new FingerprintService.EnrollClientImplOverride(fingerprintService, context, daemon, halDeviceId, token, listener, userId, groupId, flags, cryptoToken, restricted, owner, disabledFeatures, enrollTimeout) { // from class: com.android.server.biometrics.fingerprint.VivoFingerprintServiceImpl.1
            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
            {
                super(fingerprintService, context, daemon, halDeviceId, token, listener, userId, groupId, flags, cryptoToken, restricted, owner, disabledFeatures, enrollTimeout);
                Objects.requireNonNull(fingerprintService);
            }

            public boolean shouldVibrate() {
                return false;
            }

            protected int statsModality() {
                return VivoFingerprintServiceImpl.this.mService.statsModality();
            }

            public void destroy() {
                VivoFingerprintServiceImpl.this.clientDestroy();
                super.destroy();
            }

            public void onStart() {
                VivoFingerprintServiceImpl.this.mService.setPhysicalPowerKeyDisabled(true);
            }

            public void onStop() {
                VivoFingerprintServiceImpl.this.mService.setPhysicalPowerKeyDisabled(false);
            }

            public void binderDied() {
                VivoFingerprintServiceImpl.this.mService.setPhysicalPowerKeyDisabled(false);
                super.binderDied();
            }
        };
    }

    public BiometricServiceBase.AuthenticationClientImpl createFingerprintAuthClient(Context context, BiometricServiceBase.DaemonWrapper daemon, long halDeviceId, IBinder token, BiometricServiceBase.ServiceListener listener, int targetUserId, int groupId, int types, long opId, boolean restricted, String owner, int cookie, boolean requireConfirmation) {
        return new AuthenticationClientImplOverride(context, daemon, halDeviceId, token, listener, targetUserId, groupId, types, opId, restricted, owner, cookie, requireConfirmation);
    }

    /* loaded from: classes.dex */
    public class AuthenticationClientImplOverride extends FingerprintService.FingerprintAuthClient {
        int mType;
        List<Fingerprint> mVerifyList;

        /* JADX WARN: Illegal instructions before constructor call */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
            To view partially-correct code enable 'Show inconsistent code' option in preferences
        */
        public AuthenticationClientImplOverride(android.content.Context r18, com.android.server.biometrics.BiometricServiceBase.DaemonWrapper r19, long r20, android.os.IBinder r22, com.android.server.biometrics.BiometricServiceBase.ServiceListener r23, int r24, int r25, int r26, long r27, boolean r29, java.lang.String r30, int r31, boolean r32) {
            /*
                r16 = this;
                r15 = r16
                r14 = r17
                com.android.server.biometrics.fingerprint.VivoFingerprintServiceImpl.this = r14
                com.android.server.biometrics.fingerprint.FingerprintService r1 = com.android.server.biometrics.fingerprint.VivoFingerprintServiceImpl.access$000(r17)
                java.util.Objects.requireNonNull(r1)
                r0 = r16
                r2 = r18
                r3 = r19
                r4 = r20
                r6 = r22
                r7 = r23
                r8 = r24
                r9 = r25
                r10 = r27
                r12 = r29
                r13 = r30
                r14 = r31
                r15 = r32
                r0.<init>(r1, r2, r3, r4, r6, r7, r8, r9, r10, r12, r13, r14, r15)
                if (r26 == 0) goto L39
                r0 = r17
                r1 = r30
                boolean r2 = r0.isSystemOwner(r1)
                if (r2 == 0) goto L3d
                r2 = r26
                goto L3e
            L39:
                r0 = r17
                r1 = r30
            L3d:
                r2 = 1
            L3e:
                r3 = r16
                r3.mType = r2
                java.util.ArrayList r2 = new java.util.ArrayList
                r2.<init>()
                r3.mVerifyList = r2
                java.lang.String r2 = super.getLogTag()
                java.lang.StringBuilder r4 = new java.lang.StringBuilder
                r4.<init>()
                java.lang.String r5 = "authenticate type:0x"
                r4.append(r5)
                int r5 = r3.mType
                java.lang.String r5 = java.lang.Integer.toHexString(r5)
                r4.append(r5)
                java.lang.String r4 = r4.toString()
                vivo.util.VSlog.d(r2, r4)
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.server.biometrics.fingerprint.VivoFingerprintServiceImpl.AuthenticationClientImplOverride.<init>(com.android.server.biometrics.fingerprint.VivoFingerprintServiceImpl, android.content.Context, com.android.server.biometrics.BiometricServiceBase$DaemonWrapper, long, android.os.IBinder, com.android.server.biometrics.BiometricServiceBase$ServiceListener, int, int, int, long, boolean, java.lang.String, int, boolean):void");
        }

        public Fingerprint preHandleAuthentication(long deviceId, int fingerId, int groupId) {
            boolean isInVerifyList = false;
            int type = 1;
            StringBuilder builder = new StringBuilder();
            Iterator<Fingerprint> it = this.mVerifyList.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                Fingerprint fingerprint = it.next();
                if (fingerId == fingerprint.getBiometricId()) {
                    isInVerifyList = true;
                    type = fingerprint.getType();
                    break;
                }
                if (builder.length() > 0) {
                    builder.append(';');
                }
                builder.append(fingerprint.getBiometricId());
            }
            if (!isInVerifyList) {
                VSlog.d(super.getLogTag(), String.format("finger %d not matched in verify list %s, isInVerifyList: %b.", Integer.valueOf(fingerId), builder.toString(), Boolean.valueOf(isInVerifyList)));
                if (fingerId != 0) {
                    IBiometricsFingerprint daemon = VivoFingerprintServiceImpl.this.getFingerprintDaemon();
                    if (daemon == null) {
                        VSlog.w("BiometricStats", "restart authentication: no fingerprint HAL!");
                    } else {
                        try {
                            int resultInt = daemon.authenticate(getOpId(), groupId);
                            if (resultInt != 0) {
                                VSlog.w("BiometricStats", "restartAuthentication failed, result=" + resultInt);
                            }
                        } catch (RemoteException e) {
                            VSlog.e("BiometricStats", "Failed to restart authenticate:", e);
                        }
                    }
                }
                fingerId = 0;
            }
            Fingerprint fp = new Fingerprint(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, groupId, fingerId, deviceId, type);
            return fp;
        }

        public int start() {
            this.mVerifyList.clear();
            StringBuilder verifyTemplates = new StringBuilder();
            List<Fingerprint> fingerprints = FingerprintUtils.getInstance().getBiometricsForUser(getContext(), getTargetUserId(), -1);
            for (Fingerprint fingerprint : fingerprints) {
                int i = this.mType;
                if (i != -1 && (i & fingerprint.getType()) == 0) {
                    String logTag = super.getLogTag();
                    VSlog.d(logTag, "filter out fingerId:" + fingerprint.getBiometricId());
                } else {
                    this.mVerifyList.add(fingerprint);
                    if (verifyTemplates.length() > 0) {
                        verifyTemplates.append(';');
                    }
                    verifyTemplates.append(fingerprint.getBiometricId());
                }
            }
            String logTag2 = super.getLogTag();
            VSlog.d(logTag2, "verifyTemplates = " + verifyTemplates.toString());
            return super.start();
        }
    }
}