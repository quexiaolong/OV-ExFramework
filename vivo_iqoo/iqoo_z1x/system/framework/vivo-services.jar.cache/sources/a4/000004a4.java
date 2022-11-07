package com.android.server.print;

import com.android.server.VivoDoubleInstanceServiceImpl;

/* loaded from: classes.dex */
public class VivoRemotePrintSpoolerImpl implements IVivoRemotePrintSpooler {
    static final String TAG = "VivoRemotePrintSpoolerImpl";
    private VivoDoubleInstanceServiceImpl mVivoDoubleInstanceService = VivoDoubleInstanceServiceImpl.getInstance();

    public boolean isDoubleInstanceUserid(int userid) {
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl != null && vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable() && 999 == userid) {
            return true;
        }
        return false;
    }
}