package com.android.server.locksettings;

import android.os.SystemProperties;
import java.io.PrintWriter;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoLockSettingsShellCommandImpl implements IVivoLockSettingsShellCommand {
    private static final String TAG = "VivoLockSettingsShellCommandImpl";
    private static final String VIVO_CON_LEVEL_STRONGAUTH_TIMEOUT_PROPERTY = "persist.vivo.con.strongauth.timeout";
    private static final String VIVO_STRONGAUTH_TIMEOUT_PROPERTY = "persist.vivo.strongauth.timeout";

    public void runSetVivoTimeout(String timeout, PrintWriter pw) {
        VSlog.w(TAG, "isRootUid: " + LockSettingsService.sIsRootUid);
        if (LockSettingsService.sIsRootUid) {
            VivoLockSettingsStrongAuthImpl.sBBKStrongauthTimeout = Long.parseLong(timeout);
            if (VivoLockSettingsStrongAuthImpl.sBBKStrongauthTimeout <= 10000 && VivoLockSettingsStrongAuthImpl.sBBKStrongauthTimeout != -999) {
                VivoLockSettingsStrongAuthImpl.sBBKStrongauthTimeout = 10000L;
            }
            String value = "t:" + System.currentTimeMillis() + "_v:" + Long.toString(VivoLockSettingsStrongAuthImpl.sBBKStrongauthTimeout);
            SystemProperties.set(VIVO_STRONGAUTH_TIMEOUT_PROPERTY, value);
            pw.println("set vivotimeout to " + VivoLockSettingsStrongAuthImpl.sBBKStrongauthTimeout);
        }
    }

    public void runSetVivoTimeout(String timeout, PrintWriter pw, int userId) {
        VSlog.w(TAG, "isRootUid: " + LockSettingsService.sIsRootUid);
        if (LockSettingsService.sIsRootUid) {
            long tempTimeout = Long.parseLong(timeout);
            if (tempTimeout <= 10000 && tempTimeout != -999) {
                tempTimeout = 10000;
            }
            VivoLockSettingsStrongAuthImpl.sBBKStrongauthTimeoutArray.put(userId, tempTimeout);
            String value = "t:" + System.currentTimeMillis() + "_v:" + Long.toString(tempTimeout) + "_u:" + userId;
            SystemProperties.set(VIVO_STRONGAUTH_TIMEOUT_PROPERTY, value);
            pw.println("set vivotimeout to " + tempTimeout + ", userId: " + userId);
        }
    }

    public void runSetVivoNonStrongTimeout(String timeout, PrintWriter pw, int userId) {
        VSlog.w(TAG, "isRootUid: " + LockSettingsService.sIsRootUid);
        if (LockSettingsService.sIsRootUid) {
            long tempTimeout = Long.parseLong(timeout);
            if (tempTimeout <= 10000 && tempTimeout != -999) {
                tempTimeout = 10000;
            }
            VivoLockSettingsStrongAuthImpl.sBBKNonStrongNiometricTimeoutArray.put(userId, tempTimeout);
            pw.println("set vivo non strong timeout to " + tempTimeout + ", userId: " + userId);
        }
    }

    public void runSetVivoConvenienceLevelStrongauhTimeout(String timeout, PrintWriter pw, int userId) {
        VSlog.w(TAG, "isRootUid: " + LockSettingsService.sIsRootUid);
        if (LockSettingsService.sIsRootUid) {
            long tempTimeout = Long.parseLong(timeout);
            if (tempTimeout <= 10000 && tempTimeout != -999) {
                tempTimeout = 10000;
            }
            VivoLockSettingsStrongAuthImpl.sBBKConLevelStrongauthTimeoutArray.put(userId, tempTimeout);
            String value = "t:" + System.currentTimeMillis() + "_v:" + Long.toString(tempTimeout) + "_u:" + userId;
            SystemProperties.set(VIVO_CON_LEVEL_STRONGAUTH_TIMEOUT_PROPERTY, value);
            pw.println("set vivo convenience level strongauth timeout to " + tempTimeout + ", userId: " + userId);
        }
    }
}