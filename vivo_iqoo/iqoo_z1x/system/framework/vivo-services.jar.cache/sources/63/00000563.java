package com.android.server.wm;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.view.SurfaceControl;
import java.util.ArrayList;
import vendor.pixelworks.hardware.display.V1_0.KernelConfig;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoLayerRecorderManager {
    static final String TAG = "LayerRecorderManager";
    final Context mContext;
    final Object mLock = new Object();
    private ArrayList<DeathWatcher> mDeathWatcher = new ArrayList<>();

    public VivoLayerRecorderManager(Context context) {
        this.mContext = context;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class DeathWatcher implements IBinder.DeathRecipient {
        final IBinder mDeathToken;
        final IBinder mDisplayToken;

        DeathWatcher(IBinder displayToken, IBinder deathToken) {
            this.mDisplayToken = displayToken;
            this.mDeathToken = deathToken;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (VivoLayerRecorderManager.this.mLock) {
                SurfaceControl.destroyLayerRecorder(this.mDisplayToken);
                this.mDeathToken.unlinkToDeath(this, 0);
                VivoLayerRecorderManager.this.mDeathWatcher.remove(this);
                VSlog.v(VivoLayerRecorderManager.TAG, "binder died, remove LayerRecorder. token: " + this.mDisplayToken);
            }
        }
    }

    public IBinder createLayerRecorder(String pkg, String name, IBinder deathToken) {
        IBinder displayToken;
        if (!isCallerSystemApp()) {
            VSlog.e(TAG, "abort createLayerRecorder");
            return null;
        } else if (deathToken == null) {
            VSlog.e(TAG, "deathToken is null to abort createLayerRecorder");
            return null;
        } else {
            long origId = Binder.clearCallingIdentity();
            synchronized (this.mLock) {
                displayToken = SurfaceControl.createLayerRecorder(name, null);
                if (displayToken != null) {
                    DeathWatcher deathWatcher = new DeathWatcher(displayToken, deathToken);
                    try {
                        deathToken.linkToDeath(deathWatcher, 0);
                        this.mDeathWatcher.add(deathWatcher);
                    } catch (RemoteException e) {
                        VSlog.e(TAG, "createLayerRecorder failed for remote exception");
                        SurfaceControl.destroyLayerRecorder(displayToken);
                        return null;
                    }
                }
            }
            Binder.restoreCallingIdentity(origId);
            VSlog.v(TAG, "createLayerRecorder done, token: " + displayToken);
            return displayToken;
        }
    }

    public void destroyLayerRecorder(String pkg, IBinder deathToken) {
        if (!isCallerSystemApp()) {
            VSlog.e(TAG, "abort destroyLayerRecorder");
        } else if (deathToken == null) {
            VSlog.e(TAG, "deathToken is null to abort destroyLayerRecorder");
        } else {
            long origId = Binder.clearCallingIdentity();
            synchronized (this.mLock) {
                int N = this.mDeathWatcher.size();
                DeathWatcher deathWatcher = null;
                int i = 0;
                while (true) {
                    if (i >= N) {
                        break;
                    } else if (this.mDeathWatcher.get(i).mDeathToken != deathToken) {
                        i++;
                    } else {
                        deathWatcher = this.mDeathWatcher.get(i);
                        break;
                    }
                }
                if (deathWatcher != null) {
                    SurfaceControl.destroyLayerRecorder(deathWatcher.mDisplayToken);
                    deathToken.unlinkToDeath(deathWatcher, 0);
                    this.mDeathWatcher.remove(deathWatcher);
                    VSlog.v(TAG, "destroyLayerRecorder done, token: " + deathWatcher.mDisplayToken);
                }
            }
            Binder.restoreCallingIdentity(origId);
        }
    }

    private boolean isCallerSystemApp() {
        String callingApp;
        ApplicationInfo info;
        try {
            int uid = Binder.getCallingUid();
            String[] packageNames = this.mContext.getPackageManager().getPackagesForUid(uid);
            if (packageNames == null || packageNames.length == 0 || (info = this.mContext.getPackageManager().getApplicationInfoAsUser((callingApp = packageNames[0]), 0, UserHandle.getCallingUserId())) == null) {
                return false;
            }
            if ("com.example.blurdemo".equals(callingApp)) {
                return true;
            }
            return (info.flags & KernelConfig.AP_TE) != 0;
        } catch (Exception ex) {
            VSlog.e(TAG, "Exception ex: " + ex);
            return false;
        }
    }
}