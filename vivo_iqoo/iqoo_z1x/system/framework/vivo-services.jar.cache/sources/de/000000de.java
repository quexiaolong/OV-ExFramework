package com.android.server.am;

import android.content.Intent;
import java.util.ArrayList;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoFcmGoldPassSupervisor {
    private final Object mFcmLock;
    private boolean mUseUnfreeze;
    private ArrayList<String> sFcmGoldPassActions;
    private ArrayList<String> sFcmGoldPassBackList;

    private VivoFcmGoldPassSupervisor() {
        this.sFcmGoldPassActions = new ArrayList<>();
        this.sFcmGoldPassBackList = new ArrayList<>();
        this.mFcmLock = new Object();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Instance {
        private static final VivoFcmGoldPassSupervisor INSTANCE = new VivoFcmGoldPassSupervisor();

        private Instance() {
        }
    }

    public static VivoFcmGoldPassSupervisor getInstance() {
        return Instance.INSTANCE;
    }

    public void updateFcmPolicy(boolean useUnfreeze, ArrayList<String> fcmActionLists, ArrayList<String> fcmBlackLists) {
        synchronized (this.mFcmLock) {
            this.sFcmGoldPassActions.clear();
            this.sFcmGoldPassBackList.clear();
            this.sFcmGoldPassActions = (ArrayList) fcmActionLists.clone();
            this.sFcmGoldPassBackList = (ArrayList) fcmBlackLists.clone();
            this.mUseUnfreeze = useUnfreeze;
            VSlog.i("RMS-FCM", "Fcm gold pass supervisor get fcm policy. useUnfreeze: " + this.mUseUnfreeze + ", sFcmGoldPassActions: " + this.sFcmGoldPassActions.toString() + ", sFcmGoldPassBackList: " + this.sFcmGoldPassBackList.toString());
        }
    }

    public boolean isUseUnfreeze() {
        return this.mUseUnfreeze;
    }

    public boolean allowDeliverFcmBroadcast(Intent intent, String targetPkgName) {
        boolean z;
        synchronized (this.mFcmLock) {
            if (intent != null) {
                try {
                    z = (this.sFcmGoldPassActions.contains(intent.getAction()) && !this.sFcmGoldPassBackList.contains(targetPkgName)) ? true : true;
                } finally {
                }
            }
            z = false;
        }
        return z;
    }

    public boolean needAddIncludeStoppedFlag(Intent intent, String targetPkgName) {
        return (intent.getFlags() & 32) == 0 && allowDeliverFcmBroadcast(intent, targetPkgName);
    }
}