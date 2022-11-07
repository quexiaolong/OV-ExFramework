package com.vivo.services.rms;

import android.view.WindowManager;
import com.android.server.wm.VivoWmsImpl;
import com.android.server.wm.WindowManagerInternal;
import com.android.server.wm.WindowState;

/* loaded from: classes.dex */
public class RMWms {
    private VivoWmsImpl mVivoWms = null;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Instance {
        private static RMWms INSTANCE = new RMWms();

        private Instance() {
        }
    }

    public static RMWms getInstance() {
        return Instance.INSTANCE;
    }

    public void initialize(VivoWmsImpl vivoWms) {
        this.mVivoWms = vivoWms;
    }

    public int getPreferredModeId(WindowState w) {
        return this.mVivoWms.getPreferredModeId(w);
    }

    public void registerAppTransitionListener(WindowManagerInternal.AppTransitionListener listener) {
        this.mVivoWms.registerAppTransitionListener(listener);
    }

    public WindowManager.LayoutParams getAttrs(WindowState w) {
        return this.mVivoWms.getAttrs(w);
    }

    public int getOwnerPid(WindowState w) {
        return this.mVivoWms.getOwnerPid(w);
    }

    public int getWidth(WindowState w) {
        return this.mVivoWms.getWidth(w);
    }

    public int getHeight(WindowState w) {
        return this.mVivoWms.getHeight(w);
    }

    public boolean isAnimating(WindowState w) {
        return this.mVivoWms.isAnimating(w);
    }

    public int getLayer(WindowState w) {
        return this.mVivoWms.getLayer(w);
    }

    public int getRotation(WindowState w) {
        return this.mVivoWms.getRotation(w);
    }
}