package com.android.server.wm;

import android.content.IClipboard;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.view.RemoteAnimationTarget;
import java.util.ArrayList;
import java.util.function.Consumer;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoRecentsAnimationControllerImpl implements IVivoRecentsAnimationController {
    static final String TAG = "VivoRecentsAnimationControllerImpl";
    private RecentsAnimationController mController;
    private int mDisplayId;
    private final ArrayList<AnimationAdapter> mPendingBarsAnimations = new ArrayList<>();
    private WindowManagerService mService;

    public VivoRecentsAnimationControllerImpl(RecentsAnimationController controller, WindowManagerService wms, int displayId) {
        this.mController = controller;
        this.mService = wms;
        this.mDisplayId = displayId;
    }

    public void dummy() {
        VSlog.i(TAG, "dummy, this=" + this);
    }

    public RemoteAnimationTarget[] startBarAnimations() {
        DisplayContent dc = this.mService.mRoot.getDisplayContent(this.mDisplayId);
        if (dc != null) {
            return dc.startRemoteAnimationForBars(0L, 0L, new Consumer() { // from class: com.android.server.wm.-$$Lambda$VivoRecentsAnimationControllerImpl$BsTMCT_t8rkS5LBDGXBQ_8zWhAI
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    VivoRecentsAnimationControllerImpl.this.lambda$startBarAnimations$0$VivoRecentsAnimationControllerImpl((AnimationAdapter) obj);
                }
            }, this.mPendingBarsAnimations);
        }
        return null;
    }

    public /* synthetic */ void lambda$startBarAnimations$0$VivoRecentsAnimationControllerImpl(AnimationAdapter adapter) {
        synchronized (this.mService.mGlobalLock) {
            this.mPendingBarsAnimations.remove(adapter);
        }
    }

    public void hideCurrentClipboard() {
        try {
            IClipboard clipboard = IClipboard.Stub.asInterface(ServiceManager.getService("clipboard"));
            if (clipboard != null && clipboard.isClipboardDialogShowing()) {
                clipboard.hideClipboardDialog();
            }
        } catch (RemoteException e) {
        }
    }
}