package com.android.server.rollback;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.VersionedPackage;
import android.content.rollback.PackageRollbackInfo;
import android.content.rollback.RollbackInfo;
import android.hardware.tv.cec.V1_0.CecMessageType;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.util.FrameworkStatsLog;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/* loaded from: classes2.dex */
public final class WatchdogRollbackLogger {
    private static final String LOGGING_PARENT_KEY = "android.content.pm.LOGGING_PARENT";
    private static final String TAG = "WatchdogRollbackLogger";

    private WatchdogRollbackLogger() {
    }

    private static String getLoggingParentName(Context context, String packageName) {
        PackageManager packageManager = context.getPackageManager();
        try {
            ApplicationInfo ai = packageManager.getPackageInfo(packageName, 1073741952).applicationInfo;
            if (ai.metaData == null) {
                return null;
            }
            return ai.metaData.getString(LOGGING_PARENT_KEY);
        } catch (Exception e) {
            Slog.w(TAG, "Unable to discover logging parent package: " + packageName, e);
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static VersionedPackage getLogPackage(Context context, VersionedPackage failingPackage) {
        String logPackageName = getLoggingParentName(context, failingPackage.getPackageName());
        if (logPackageName == null) {
            return null;
        }
        try {
            VersionedPackage loggingParent = new VersionedPackage(logPackageName, context.getPackageManager().getPackageInfo(logPackageName, 0).getLongVersionCode());
            return loggingParent;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private static Set<VersionedPackage> getLogPackages(Context context, List<String> failedPackageNames) {
        Set<VersionedPackage> parentPackages = new ArraySet<>();
        for (String failedPackageName : failedPackageNames) {
            parentPackages.add(getLogPackage(context, new VersionedPackage(failedPackageName, 0)));
        }
        return parentPackages;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void logRollbackStatusOnBoot(Context context, int rollbackId, String logPackageName, List<RollbackInfo> recentlyCommittedRollbacks) {
        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        RollbackInfo rollback = null;
        Iterator<RollbackInfo> it = recentlyCommittedRollbacks.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            RollbackInfo info = it.next();
            if (rollbackId == info.getRollbackId()) {
                rollback = info;
                break;
            }
        }
        if (rollback == null) {
            Slog.e(TAG, "rollback info not found for last staged rollback: " + rollbackId);
            return;
        }
        VersionedPackage oldLoggingPackage = null;
        if (!TextUtils.isEmpty(logPackageName)) {
            Iterator it2 = rollback.getPackages().iterator();
            while (true) {
                if (!it2.hasNext()) {
                    break;
                }
                PackageRollbackInfo packageRollback = (PackageRollbackInfo) it2.next();
                if (logPackageName.equals(packageRollback.getPackageName())) {
                    oldLoggingPackage = packageRollback.getVersionRolledBackFrom();
                    break;
                }
            }
        }
        int sessionId = rollback.getCommittedSessionId();
        PackageInstaller.SessionInfo sessionInfo = packageInstaller.getSessionInfo(sessionId);
        if (sessionInfo == null) {
            Slog.e(TAG, "On boot completed, could not load session id " + sessionId);
        } else if (sessionInfo.isStagedSessionApplied()) {
            logEvent(oldLoggingPackage, 2, 0, "");
        } else if (sessionInfo.isStagedSessionFailed()) {
            logEvent(oldLoggingPackage, 3, 0, "");
        }
    }

    public static void logApexdRevert(Context context, List<String> failedPackageNames, String failingNativeProcess) {
        Set<VersionedPackage> logPackages = getLogPackages(context, failedPackageNames);
        for (VersionedPackage logPackage : logPackages) {
            logEvent(logPackage, 2, 5, failingNativeProcess);
        }
    }

    public static void logEvent(VersionedPackage logPackage, int type, int rollbackReason, String failingPackageName) {
        Slog.i(TAG, "Watchdog event occurred with type: " + rollbackTypeToString(type) + " logPackage: " + logPackage + " rollbackReason: " + rollbackReasonToString(rollbackReason) + " failedPackageName: " + failingPackageName);
        if (logPackage != null) {
            FrameworkStatsLog.write((int) CecMessageType.SELECT_DIGITAL_SERVICE, type, logPackage.getPackageName(), logPackage.getVersionCode(), rollbackReason, failingPackageName, new byte[0]);
        } else {
            FrameworkStatsLog.write((int) CecMessageType.SELECT_DIGITAL_SERVICE, type, "", 0, rollbackReason, failingPackageName, new byte[0]);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int mapFailureReasonToMetric(int failureReason) {
        if (failureReason != 1) {
            if (failureReason != 2) {
                if (failureReason != 3) {
                    return failureReason != 4 ? 0 : 4;
                }
                return 3;
            }
            return 2;
        }
        return 1;
    }

    private static String rollbackTypeToString(int type) {
        if (type != 1) {
            if (type != 2) {
                if (type != 3) {
                    if (type == 4) {
                        return "ROLLBACK_BOOT_TRIGGERED";
                    }
                    return "UNKNOWN";
                }
                return "ROLLBACK_FAILURE";
            }
            return "ROLLBACK_SUCCESS";
        }
        return "ROLLBACK_INITIATE";
    }

    private static String rollbackReasonToString(int reason) {
        if (reason != 1) {
            if (reason != 2) {
                if (reason != 3) {
                    if (reason != 4) {
                        if (reason == 5) {
                            return "REASON_NATIVE_CRASH_DURING_BOOT";
                        }
                        return "UNKNOWN";
                    }
                    return "REASON_APP_NOT_RESPONDING";
                }
                return "REASON_APP_CRASH";
            }
            return "REASON_EXPLICIT_HEALTH_CHECK";
        }
        return "REASON_NATIVE_CRASH";
    }
}