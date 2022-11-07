package com.android.server.biometrics.fingerprint;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.biometrics.fingerprint.FingerprintKeyguardInternal;
import android.hardware.biometrics.fingerprint.FingerprintUIManagerInternal;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.view.Display;
import com.android.internal.util.DumpUtils;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.biometrics.fingerprint.FingerprintUIManagerService;
import com.android.server.wm.SnapshotWindow;
import com.vivo.fingerprint.WindowStatus;
import com.vivo.fingerprint.ui.IFingerprintUI;
import com.vivo.fingerprint.ui.IFingerprintUIManagerService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class FingerprintUIManagerService extends SystemService implements IBinder.DeathRecipient {
    static final boolean DEBUG;
    static final String TAG = "FingerprintUIManagerService";
    private List<WindowStatus> mAllWindows;
    private Binder mBinder;
    private IFingerprintUI mCallback;
    private final Context mContext;
    private FingerprintUIState mFingerprintUIState;
    private Handler mHandler;
    private int mImeBackDisposition;
    private IBinder mImeToken;
    private int mImeWindowVis;
    private final FingerprintUIManagerInternal mInternalService;
    private PowerManager mPowerManager;
    private boolean mShowImeSwitcher;
    private SnapshotWindow mSnapshotWin;

    static {
        DEBUG = SystemProperties.getBoolean("persist.sys.log.ctrl", Build.IS_DEBUGGABLE) || Build.IS_DEBUGGABLE;
    }

    public FingerprintUIManagerService(Context context) {
        super(context);
        this.mHandler = new Handler();
        this.mImeWindowVis = 0;
        this.mBinder = new IFingerprintUIManagerService.Stub() { // from class: com.android.server.biometrics.fingerprint.FingerprintUIManagerService.1
            public void registerFingerprintUI(IFingerprintUI callback) {
                FingerprintUIManagerService.this.checkPermission();
                FingerprintUIManagerService.this.mCallback = callback;
                try {
                    FingerprintUIManagerService.this.mCallback.asBinder().linkToDeath(FingerprintUIManagerService.this, 0);
                    FingerprintUIState tempState = new FingerprintUIState();
                    synchronized (FingerprintUIManagerService.this.mFingerprintUIState) {
                        FingerprintUIManagerService.this.mFingerprintUIState.copyTo(tempState);
                    }
                    Bundle extras = new Bundle();
                    extras.putInt("crashCount", tempState.crashCount);
                    FingerprintUIManagerService.this.mCallback.onResume(extras);
                    FingerprintUIManagerService.this.mCallback.onDisplayStateChangeStarted(tempState.displayState, tempState.displayBrightness);
                    FingerprintUIManagerService.this.mCallback.onDisplayStateChangeFinished(tempState.displayActuallyState, tempState.displayActuallyBrightness);
                    DialogInfo dialogInfo = tempState.dialogInfo;
                    if (dialogInfo != null) {
                        FingerprintUIManagerService.this.mCallback.showFingerprintDialog(dialogInfo.owner, dialogInfo.type, dialogInfo.inWhiteList);
                    }
                } catch (RemoteException ex) {
                    FingerprintUIManagerService.warning("Remote Exception", ex);
                }
            }

            public float getCurrentBrightness() {
                return FingerprintUIManagerService.this.mFingerprintUIState.displayBrightness;
            }

            public List<WindowStatus> getWindowList() {
                return FingerprintUIManagerService.this.mAllWindows;
            }

            protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
                if (DumpUtils.checkDumpPermission(FingerprintUIManagerService.this.mContext, FingerprintUIManagerService.TAG, pw)) {
                    long ident = Binder.clearCallingIdentity();
                    try {
                        FingerprintUIManagerService.this.dumpInternal(fd, pw, args);
                    } finally {
                        Binder.restoreCallingIdentity(ident);
                    }
                }
            }

            /* JADX WARN: Multi-variable type inference failed */
            public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
                new FingerprintUIShellCommand(FingerprintUIManagerService.this.mCallback).exec(this, in, out, err, args, callback, resultReceiver);
            }
        };
        this.mInternalService = new AnonymousClass2();
        this.mAllWindows = new ArrayList();
        this.mContext = context;
        this.mFingerprintUIState = new FingerprintUIState();
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        LocalServices.addService(FingerprintUIManagerInternal.class, this.mInternalService);
    }

    public void onStart() {
        publishBinderService("fingerprintui", this.mBinder);
    }

    @Override // android.os.IBinder.DeathRecipient
    public void binderDied() {
        this.mCallback = null;
        synchronized (this.mFingerprintUIState) {
            this.mFingerprintUIState.crashCount++;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.STATUS_BAR", TAG);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.biometrics.fingerprint.FingerprintUIManagerService$2  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass2 implements FingerprintUIManagerInternal {
        AnonymousClass2() {
        }

        public void showFingerprintDialog(String owner, int type, boolean inWhiteList) {
            FingerprintUIManagerService.debug("showFingerprintDialog()");
            try {
                if (FingerprintUIManagerService.this.mCallback != null) {
                    FingerprintUIManagerService.this.mCallback.showFingerprintDialog(owner, type, inWhiteList);
                }
            } catch (RemoteException ex) {
                FingerprintUIManagerService.warning("Remote Exception", ex);
            }
            synchronized (FingerprintUIManagerService.this.mFingerprintUIState) {
                FingerprintUIManagerService.this.mFingerprintUIState.dialogInfo = new DialogInfo(owner, type, inWhiteList);
            }
        }

        public void onFingerprintAuthenticated(boolean authenticated) {
            FingerprintUIManagerService.debug("onFingerprintAuthenticated(" + authenticated + ")");
            try {
                if (FingerprintUIManagerService.this.mCallback != null) {
                    FingerprintUIManagerService.this.mCallback.onFingerprintAuthenticated(authenticated);
                }
            } catch (RemoteException ex) {
                FingerprintUIManagerService.warning("Remote Exception", ex);
            }
        }

        public void onFaceAuthenticated(int status) {
            FingerprintUIManagerService.debug("onFaceAuthenticated(" + status + ")");
            try {
                if (FingerprintUIManagerService.this.mCallback != null) {
                    FingerprintUIManagerService.this.mCallback.onFaceAuthenticated(status);
                }
                FingerprintKeyguardInternal fingerprintKeyguard = (FingerprintKeyguardInternal) LocalServices.getService(FingerprintKeyguardInternal.class);
                if (fingerprintKeyguard != null) {
                    fingerprintKeyguard.onFaceAuthenticated(status);
                }
            } catch (RemoteException ex) {
                FingerprintUIManagerService.warning("Remote Exception", ex);
            }
        }

        public void onFingerprintHelp(String message) {
            FingerprintUIManagerService.debug("onFingerprintHelp(" + message + ")");
            try {
                if (FingerprintUIManagerService.this.mCallback != null) {
                    FingerprintUIManagerService.this.mCallback.onFingerprintHelp(message);
                }
            } catch (RemoteException ex) {
                FingerprintUIManagerService.warning("Remote Exception", ex);
            }
        }

        public void onFingerprintAcquired(int acquiredInfo) {
            FingerprintUIManagerService.debug("onFingerprintAcquired(" + acquiredInfo + ")");
            try {
                if (FingerprintUIManagerService.this.mCallback != null) {
                    FingerprintUIManagerService.this.mCallback.onFingerprintAcquired(acquiredInfo);
                }
            } catch (RemoteException ex) {
                FingerprintUIManagerService.warning("Remote Exception", ex);
            }
        }

        public void onFingerprintError(String error) {
            FingerprintUIManagerService.debug("onFingerprintError(" + error + ")");
            try {
                if (FingerprintUIManagerService.this.mCallback != null) {
                    FingerprintUIManagerService.this.mCallback.onFingerprintError(error);
                }
            } catch (RemoteException ex) {
                FingerprintUIManagerService.warning("Remote Exception", ex);
            }
        }

        public void hideFingerprintDialog(int reason) {
            FingerprintUIManagerService.debug("hideFingerprintDialog(" + reason + ")");
            try {
                if (FingerprintUIManagerService.this.mCallback != null) {
                    FingerprintUIManagerService.this.mCallback.hideFingerprintDialog(reason);
                }
            } catch (RemoteException ex) {
                FingerprintUIManagerService.warning("Remote Exception", ex);
            }
            synchronized (FingerprintUIManagerService.this.mFingerprintUIState) {
                FingerprintUIManagerService.this.mFingerprintUIState.dialogInfo = null;
            }
        }

        public void setImeWindowStatus(final IBinder token, final int vis, final int backDisposition, final boolean showImeSwitcher) {
            FingerprintUIManagerService.debug("setImeWindowStatus(vis=" + vis + ", backDisposition=" + backDisposition + ")");
            synchronized (FingerprintUIManagerService.this.mFingerprintUIState) {
                FingerprintUIManagerService.this.mImeWindowVis = vis;
                FingerprintUIManagerService.this.mImeBackDisposition = backDisposition;
                FingerprintUIManagerService.this.mImeToken = token;
                FingerprintUIManagerService.this.mShowImeSwitcher = showImeSwitcher;
                FingerprintUIManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.fingerprint.-$$Lambda$FingerprintUIManagerService$2$L59gy1p_4nARupBfl5Ln1zprdcY
                    @Override // java.lang.Runnable
                    public final void run() {
                        FingerprintUIManagerService.AnonymousClass2.this.lambda$setImeWindowStatus$0$FingerprintUIManagerService$2(token, vis, backDisposition, showImeSwitcher);
                    }
                });
            }
        }

        public /* synthetic */ void lambda$setImeWindowStatus$0$FingerprintUIManagerService$2(IBinder token, int vis, int backDisposition, boolean showImeSwitcher) {
            try {
                if (FingerprintUIManagerService.this.mCallback != null) {
                    FingerprintUIManagerService.this.mCallback.setImeWindowStatus(token, vis, backDisposition, showImeSwitcher);
                }
            } catch (RemoteException ex) {
                FingerprintUIManagerService.warning("Remote Exception", ex);
            }
        }

        public void onDisplayStateChangeStarted(int state, float brightness) {
            int oldDisplayState;
            float oldDisplayBrightness;
            synchronized (FingerprintUIManagerService.this.mFingerprintUIState) {
                if (FingerprintUIManagerService.this.mFingerprintUIState.displayState != state || FingerprintUIManagerService.this.mFingerprintUIState.displayBrightness * brightness <= 0.0f) {
                    FingerprintUIManagerService.debug("onDisplayStateChangeStarted(" + Display.stateToString(state) + ", " + brightness + ")");
                }
                oldDisplayState = FingerprintUIManagerService.this.mFingerprintUIState.displayState;
                FingerprintUIManagerService.this.mFingerprintUIState.displayState = state;
                oldDisplayBrightness = FingerprintUIManagerService.this.mFingerprintUIState.displayBrightness;
                FingerprintUIManagerService.this.mFingerprintUIState.displayBrightness = brightness;
            }
            try {
                if (FingerprintUIManagerService.this.mCallback != null) {
                    if (oldDisplayState != state || oldDisplayBrightness * brightness <= 0.0f) {
                        FingerprintUIManagerService.this.mCallback.onDisplayStateChangeStarted(state, brightness);
                    }
                }
            } catch (RemoteException ex) {
                FingerprintUIManagerService.warning("Remote Exception", ex);
            }
        }

        public void onDisplayStateChangeFinished(int state, float brightness) {
            synchronized (FingerprintUIManagerService.this.mFingerprintUIState) {
                if (FingerprintUIManagerService.this.mFingerprintUIState.displayActuallyState != state || FingerprintUIManagerService.this.mFingerprintUIState.displayActuallyBrightness * brightness <= 0.0f) {
                    FingerprintUIManagerService.debug("onDisplayStateChangeFinished(" + Display.stateToString(state) + ", " + brightness + ")");
                }
                FingerprintUIManagerService.this.mFingerprintUIState.displayActuallyState = state;
                FingerprintUIManagerService.this.mFingerprintUIState.displayActuallyBrightness = brightness;
            }
            try {
                if (FingerprintUIManagerService.this.mCallback != null) {
                    FingerprintUIManagerService.this.mCallback.onDisplayStateChangeFinished(state, brightness);
                }
            } catch (RemoteException ex) {
                FingerprintUIManagerService.warning("Remote Exception", ex);
            }
            if (FingerprintUIManagerService.this.mSnapshotWin == null) {
                FingerprintUIManagerService fingerprintUIManagerService = FingerprintUIManagerService.this;
                fingerprintUIManagerService.mSnapshotWin = SnapshotWindow.getInstance(fingerprintUIManagerService.mContext);
            }
            if (FingerprintUIManagerService.this.mSnapshotWin != null) {
                FingerprintUIManagerService.this.mSnapshotWin.onDisplayStateChangeFinished(brightness);
            }
        }

        public void onFingerprintRemoved(int remaining) {
            try {
                if (FingerprintUIManagerService.this.mCallback != null) {
                    FingerprintUIManagerService.this.mCallback.onFingerprintRemoved(remaining);
                }
            } catch (RemoteException ex) {
                FingerprintUIManagerService.warning("Remote Exception", ex);
            }
        }

        public void setWindowStatus(final String token, final String owner, final String title, final int type, final int layer, final Rect rect, final boolean shown, final int format, final int flags, final boolean ownerCanAddInternalSystemWindow) {
            FingerprintUIManagerService.debug(String.format("setWindowStatus(%s, %s, %d, %d, %s, %b, %d, %d, %b)", owner, title, Integer.valueOf(type), Integer.valueOf(layer), rect.toShortString(), Boolean.valueOf(shown), Integer.valueOf(format), Integer.valueOf(flags), Boolean.valueOf(ownerCanAddInternalSystemWindow)));
            FingerprintUIManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.fingerprint.-$$Lambda$FingerprintUIManagerService$2$NGeningE-VYsbrKjgZvoSgRILsQ
                @Override // java.lang.Runnable
                public final void run() {
                    FingerprintUIManagerService.AnonymousClass2.this.lambda$setWindowStatus$1$FingerprintUIManagerService$2(shown, owner, title, type);
                }
            });
            synchronized (FingerprintUIManagerService.this.mFingerprintUIState) {
                if (FingerprintUIManagerService.this.mFingerprintUIState.dialogInfo == null) {
                    return;
                }
                FingerprintUIManagerService.this.mHandler.post(new Runnable() { // from class: com.android.server.biometrics.fingerprint.-$$Lambda$FingerprintUIManagerService$2$5HX_my-gR6dOHM52OwBlk_WQlBg
                    @Override // java.lang.Runnable
                    public final void run() {
                        FingerprintUIManagerService.AnonymousClass2.this.lambda$setWindowStatus$2$FingerprintUIManagerService$2(token, owner, title, type, layer, rect, shown, format, flags, ownerCanAddInternalSystemWindow);
                    }
                });
            }
        }

        public /* synthetic */ void lambda$setWindowStatus$1$FingerprintUIManagerService$2(boolean shown, String owner, String title, int type) {
            if (shown) {
                FingerprintUIManagerService.this.mAllWindows.add(new WindowStatus(owner, title, type));
                return;
            }
            for (int i = FingerprintUIManagerService.this.mAllWindows.size() - 1; i >= 0; i--) {
                if (TextUtils.equals(((WindowStatus) FingerprintUIManagerService.this.mAllWindows.get(i)).owner, owner) && TextUtils.equals(((WindowStatus) FingerprintUIManagerService.this.mAllWindows.get(i)).title, title) && ((WindowStatus) FingerprintUIManagerService.this.mAllWindows.get(i)).type == type) {
                    FingerprintUIManagerService.this.mAllWindows.remove(i);
                }
            }
        }

        public /* synthetic */ void lambda$setWindowStatus$2$FingerprintUIManagerService$2(String token, String owner, String title, int type, int layer, Rect rect, boolean shown, int format, int flags, boolean ownerCanAddInternalSystemWindow) {
            try {
                if (FingerprintUIManagerService.this.mCallback != null) {
                    FingerprintUIManagerService.this.mCallback.setWindowStatus(token, owner, title, type, layer, rect, shown, format, flags, ownerCanAddInternalSystemWindow);
                }
            } catch (RemoteException ex) {
                FingerprintUIManagerService.warning("Remote Exception", ex);
            }
        }

        public void onAutoBrightness(float brightness, boolean useAutoBrightness) {
            FingerprintUIManagerService.debug(String.format("onAutoBrightness(%f, %b)", Float.valueOf(brightness), Boolean.valueOf(useAutoBrightness)));
            try {
                if (FingerprintUIManagerService.this.mCallback != null) {
                    FingerprintUIManagerService.this.mCallback.onAutoBrightness(brightness, useAutoBrightness);
                }
            } catch (RemoteException ex) {
                FingerprintUIManagerService.warning("Remote Exception", ex);
            }
        }
    }

    private void enforceFingerprintUIOrShell() {
        if (Binder.getCallingUid() == 2000) {
            return;
        }
        enforceFingerprintUI();
    }

    private void enforceFingerprintUI() {
        this.mContext.enforceCallingOrSelfPermission("com.vivo.fingerprint.permission.USE_FINGERPRINTUI_MANAGER", TAG);
    }

    protected void dumpInternal(FileDescriptor fd, PrintWriter pw, String[] args) {
        synchronized (this.mFingerprintUIState) {
            pw.println("  mImeWindowVis=" + this.mImeWindowVis);
            pw.println("  mImeBackDisposition=" + this.mImeBackDisposition);
            pw.println("  mShowImeSwitcher=" + this.mShowImeSwitcher);
            pw.println("  mFingerprintUIState.crashCount=" + this.mFingerprintUIState.crashCount);
            pw.println("  mFingerprintUIState.displayState=" + Display.stateToString(this.mFingerprintUIState.displayState));
            pw.println("  mFingerprintUIState.displayBrightness=" + this.mFingerprintUIState.displayBrightness);
            pw.println();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void debug(String msg) {
        if (DEBUG) {
            VSlog.d(TAG, msg);
        }
    }

    private static void info(String msg) {
        VSlog.i(TAG, msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void warning(String msg, Throwable tr) {
        VSlog.w(TAG, msg, tr);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class FingerprintUIState {
        int crashCount;
        DialogInfo dialogInfo;
        float displayActuallyBrightness;
        int displayActuallyState;
        float displayBrightness;
        int displayState;

        private FingerprintUIState() {
        }

        void copyTo(FingerprintUIState state) {
            state.displayState = this.displayState;
            state.displayBrightness = this.displayBrightness;
            state.displayActuallyState = this.displayActuallyState;
            state.displayActuallyBrightness = this.displayActuallyBrightness;
            state.crashCount = this.crashCount;
            DialogInfo dialogInfo = this.dialogInfo;
            if (dialogInfo != null) {
                state.dialogInfo = new DialogInfo(dialogInfo);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class DialogInfo {
        boolean inWhiteList;
        String owner;
        int type;

        DialogInfo(String o, int t, boolean i) {
            this.owner = o;
            this.type = t;
            this.inWhiteList = i;
        }

        DialogInfo(DialogInfo from) {
            copyFrom(from);
        }

        void copyFrom(DialogInfo from) {
            this.owner = from.owner;
            this.type = from.type;
            this.inWhiteList = from.inWhiteList;
        }
    }
}