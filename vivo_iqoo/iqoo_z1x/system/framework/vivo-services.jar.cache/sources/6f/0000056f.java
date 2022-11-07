package com.android.server.wm;

import android.view.RemoteAnimationAdapter;
import android.view.RemoteAnimationTarget;
import com.android.server.wm.BarAnimController;
import java.util.ArrayList;
import java.util.function.Consumer;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoRemoteAnimationControllerImpl implements IVivoRemoteAnimationController {
    static final String TAG = "VivoRecentsAnimationControllerImpl";
    private final ArrayList<AnimationAdapter> mPendingBarsAnimations = new ArrayList<>();
    RemoteAnimationAdapter mRemoteAnimationAdapter;
    WindowManagerService mService;

    public VivoRemoteAnimationControllerImpl(RemoteAnimationAdapter adapter, WindowManagerService wms) {
        this.mRemoteAnimationAdapter = adapter;
        this.mService = wms;
    }

    public void dummy() {
        VSlog.i(TAG, "dummy, this=" + this);
    }

    public RemoteAnimationTarget[] startBarAnimations() {
        DisplayContent dc = this.mService.mRoot.getTopFocusedDisplayContent();
        if (dc != null) {
            return dc.startRemoteAnimationForBars(0L, 0L, new Consumer() { // from class: com.android.server.wm.-$$Lambda$VivoRemoteAnimationControllerImpl$ceIHyMMfvoJyTAyf0jEQLJrX5Y0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    VivoRemoteAnimationControllerImpl.this.lambda$startBarAnimations$0$VivoRemoteAnimationControllerImpl((AnimationAdapter) obj);
                }
            }, this.mPendingBarsAnimations);
        }
        return null;
    }

    public /* synthetic */ void lambda$startBarAnimations$0$VivoRemoteAnimationControllerImpl(AnimationAdapter adapter) {
        synchronized (this.mService.mGlobalLock) {
            this.mPendingBarsAnimations.remove(adapter);
        }
    }

    public void removeBarAnimations() {
        for (int i = this.mPendingBarsAnimations.size() - 1; i >= 0; i--) {
            BarAnimController.BarAnimationAdapter adapter = (BarAnimController.BarAnimationAdapter) this.mPendingBarsAnimations.get(i);
            removeAnimation(adapter);
        }
    }

    private void removeAnimation(BarAnimController.BarAnimationAdapter adapter) {
        if (adapter == null) {
            return;
        }
        VSlog.d(TAG, "removeBarsAnimation for type=" + adapter.getToken().windowType);
        adapter.getLeashFinishedCallback().onAnimationFinished(adapter.getLastAnimationType(), adapter);
        this.mPendingBarsAnimations.remove(adapter);
    }
}