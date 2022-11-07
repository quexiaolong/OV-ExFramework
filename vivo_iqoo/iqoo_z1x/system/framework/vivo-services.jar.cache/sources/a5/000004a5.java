package com.android.server.statusbar;

import android.os.Bundle;
import android.os.RemoteException;

/* loaded from: classes.dex */
public class VivoStatusBarManagerServiceImpl implements IVivoStatusBarManagerService {
    static final String TAG = "VivoWallpaperManagerServiceImpl";
    private StatusBarManagerService mStatusBarManagerService;

    public VivoStatusBarManagerServiceImpl(StatusBarManagerService statusbarMgrService) {
        this.mStatusBarManagerService = statusbarMgrService;
    }

    public void dummy() {
    }

    public void onClearAllNotificationsIgnoreFlags() {
        this.mStatusBarManagerService.enforceStatusBarService();
        this.mStatusBarManagerService.mNotificationDelegate.onClearAllIgnoreFlags();
    }

    public void onClearNotificationsIgnoreFlags(String pkg) {
        this.mStatusBarManagerService.enforceStatusBarService();
        this.mStatusBarManagerService.mNotificationDelegate.onClearIgnoreFlags(pkg);
    }

    public void notifyInfo(int reason, Bundle infos) {
        if (this.mStatusBarManagerService.mBar != null) {
            try {
                this.mStatusBarManagerService.mBar.notifyInfo(reason, infos);
            } catch (RemoteException e) {
            }
        }
    }

    public void setStatusBarIconColor(boolean whiteStyle) {
        try {
            if (this.mStatusBarManagerService.mBar != null) {
                this.mStatusBarManagerService.mBar.setStatusBarIconColor(whiteStyle);
            }
        } catch (RemoteException e) {
        }
    }

    public void changeUpslideState(boolean down, boolean canceled) {
        try {
            if (this.mStatusBarManagerService.mBar != null) {
                this.mStatusBarManagerService.mBar.changeUpslideState(down, canceled);
            }
        } catch (RemoteException e) {
        }
    }

    public void setNetworkSpeed(String speed) {
        try {
            if (this.mStatusBarManagerService.mBar != null) {
                this.mStatusBarManagerService.mBar.setNetworkSpeed(speed);
            }
        } catch (RemoteException e) {
        }
    }

    public void setSimcardFlow(String flow1, String flow2) {
        try {
            if (this.mStatusBarManagerService.mBar != null) {
                this.mStatusBarManagerService.mBar.setSimcardFlow(flow1, flow2);
            }
        } catch (RemoteException e) {
        }
    }

    public void setSimcardFlowExtension(Bundle flowBundle) {
        try {
            if (this.mStatusBarManagerService.mBar != null) {
                this.mStatusBarManagerService.mBar.setSimcardFlowExtension(flowBundle);
            }
        } catch (RemoteException e) {
        }
    }
}