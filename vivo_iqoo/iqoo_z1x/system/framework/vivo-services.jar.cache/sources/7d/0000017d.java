package com.android.server.connectivity;

import android.content.pm.UserInfo;
import com.android.server.VivoDoubleInstanceServiceImpl;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoVpnImpl implements IVivoVpn {
    static final String TAG = "VivoVpnImpl";
    private VivoDoubleInstanceServiceImpl mVivoDoubleInstanceService;
    private Vpn mVpn;

    public VivoVpnImpl(Vpn vpn) {
        this.mVivoDoubleInstanceService = null;
        if (vpn == null) {
            VSlog.i(TAG, "container is " + vpn);
        }
        this.mVpn = vpn;
        this.mVivoDoubleInstanceService = VivoDoubleInstanceServiceImpl.getInstance();
    }

    public boolean isUserRestricted(UserInfo user) {
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl != null && vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable() && user.isDoubleAppUser()) {
            return true;
        }
        return false;
    }
}