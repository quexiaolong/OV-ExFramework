package com.android.server.wm;

import android.content.ClipData;
import android.view.IWindow;
import vivo.app.vivoscreenshot.IVivoScreenshotManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoSessionImpl implements IVivoSession {
    static final String TAG = "VivoSessionImpl";
    private Session mSession;
    IVivoScreenshotManager mVivoScreenshotManager = null;
    private VivoEasyShareManager mVivoEasyShareManager = VivoEasyShareManager.getInstance();

    public void dummy() {
        VSlog.i(TAG, "dummy, this=" + this);
    }

    public void setVivoScreenshotManager(IVivoScreenshotManager manager) {
        this.mVivoScreenshotManager = manager;
    }

    public IVivoScreenshotManager getVivoScreenshotManager() {
        return this.mVivoScreenshotManager;
    }

    public void setSession(Session session) {
        this.mSession = session;
    }

    public void updateHasSurfaceView(IWindow window, boolean visible) {
        Session session = this.mSession;
        if (session != null && session.mService != null) {
            this.mSession.mService.updateHasSurfaceView(this.mSession, window, visible);
        }
    }

    public void notifyDragStartedFromPC(int x, int y, ClipData data) {
        this.mVivoEasyShareManager.notifyDragStartedFromPC(x, y, data);
    }
}