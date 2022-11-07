package com.android.server;

import android.content.pm.UserInfo;
import android.os.IBinder;
import android.os.IUserManager;
import android.os.ServiceManager;
import android.util.VivoDoubleInstanceImpl;
import com.android.server.pm.UserManagerService;
import com.vivo.services.rms.ProcessList;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoDoubleInstanceServiceImpl implements IVivoDoubleInstanceService {
    private static final int DOUBLE_APP_FIXED_USER_ID = 999;
    private static final int FLAG_DOUBLE_APP_PROFILE = 536870912;
    private static final String TAG = "VivoDoubleInstanceServiceImpl";
    private static boolean sEnabled = false;
    private static boolean sDebug = false;
    private static VivoDoubleInstanceServiceImpl sDoubleInstanceServiceImpl = null;
    private static IUserManager mUserManager = null;
    private int mDoubleAppUserId = ProcessList.INVALID_ADJ;
    private boolean isDoubleAppUserexist = false;

    private VivoDoubleInstanceServiceImpl() {
        sEnabled = VivoDoubleInstanceImpl.sEnabled;
        sDebug = VivoDoubleInstanceImpl.sDebug;
    }

    public static VivoDoubleInstanceServiceImpl getInstance() {
        if (sDoubleInstanceServiceImpl == null) {
            synchronized (VivoDoubleInstanceServiceImpl.class) {
                if (sDoubleInstanceServiceImpl == null) {
                    sDoubleInstanceServiceImpl = new VivoDoubleInstanceServiceImpl();
                }
            }
        }
        return sDoubleInstanceServiceImpl;
    }

    public int getDoubleAppFixedUserId() {
        return 999;
    }

    public boolean isDoubleInstanceEnable() {
        return sEnabled;
    }

    public boolean isDoubleInstanceDebugEnable() {
        return sDebug;
    }

    private void init() {
        if (mUserManager == null) {
            IBinder b = ServiceManager.getService("user");
            mUserManager = IUserManager.Stub.asInterface(b);
        }
        try {
            if (mUserManager != null) {
                this.mDoubleAppUserId = mUserManager.getDoubleAppUserId();
            } else {
                UserManagerService userManagerService = UserManagerService.getInstance();
                if (userManagerService != null) {
                    this.mDoubleAppUserId = userManagerService.getDoubleAppUserId();
                }
            }
            this.isDoubleAppUserexist = this.mDoubleAppUserId == 999;
        } catch (Exception e) {
            e.printStackTrace();
            VSlog.d(TAG, "ums getDoubleAppUserId error");
        }
    }

    public boolean isDoubleAppUserExist() {
        boolean z;
        synchronized (VivoDoubleInstanceServiceImpl.class) {
            init();
            z = this.isDoubleAppUserexist;
        }
        return z;
    }

    public int getDoubleAppUserId() {
        int i;
        synchronized (VivoDoubleInstanceServiceImpl.class) {
            init();
            i = this.mDoubleAppUserId;
        }
        return i;
    }

    public boolean isDoubleAppUser(UserInfo user) {
        return user != null && (user.flags & FLAG_DOUBLE_APP_PROFILE) == FLAG_DOUBLE_APP_PROFILE;
    }
}