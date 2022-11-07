package com.android.server.power;

import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import com.android.internal.statusbar.IStatusBarService;

/* loaded from: classes2.dex */
public class InattentiveSleepWarningController {
    private static final String TAG = "InattentiveSleepWarning";
    private final Handler mHandler = new Handler();
    private boolean mIsShown;
    private IStatusBarService mStatusBarService;

    public static /* synthetic */ void lambda$EjYRIermunwb9Ll5LMj3chPJN6k(InattentiveSleepWarningController inattentiveSleepWarningController) {
        inattentiveSleepWarningController.showInternal();
    }

    public boolean isShown() {
        return this.mIsShown;
    }

    public void show() {
        if (isShown()) {
            return;
        }
        this.mHandler.post(new Runnable() { // from class: com.android.server.power.-$$Lambda$InattentiveSleepWarningController$EjYRIermunwb9Ll5LMj3chPJN6k
            @Override // java.lang.Runnable
            public final void run() {
                InattentiveSleepWarningController.lambda$EjYRIermunwb9Ll5LMj3chPJN6k(InattentiveSleepWarningController.this);
            }
        });
        this.mIsShown = true;
    }

    public void showInternal() {
        try {
            getStatusBar().showInattentiveSleepWarning();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to show inattentive sleep warning", e);
            this.mIsShown = false;
        }
    }

    public void dismiss(final boolean animated) {
        if (!isShown()) {
            return;
        }
        this.mHandler.post(new Runnable() { // from class: com.android.server.power.-$$Lambda$InattentiveSleepWarningController$fd5hIb5QJl3fTkCKcm9jEkrhnUU
            @Override // java.lang.Runnable
            public final void run() {
                InattentiveSleepWarningController.this.lambda$dismiss$0$InattentiveSleepWarningController(animated);
            }
        });
        this.mIsShown = false;
    }

    /* renamed from: dismissInternal */
    public void lambda$dismiss$0$InattentiveSleepWarningController(boolean animated) {
        try {
            getStatusBar().dismissInattentiveSleepWarning(animated);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to dismiss inattentive sleep warning", e);
        }
    }

    private IStatusBarService getStatusBar() {
        if (this.mStatusBarService == null) {
            this.mStatusBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
        }
        return this.mStatusBarService;
    }
}