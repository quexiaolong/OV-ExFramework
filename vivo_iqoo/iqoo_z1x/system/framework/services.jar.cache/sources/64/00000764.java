package com.android.server.am;

import android.app.BroadcastOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.os.Binder;
import android.os.BugreportManager;
import android.os.BugreportParams;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Slog;
import com.android.server.SystemConfig;
import java.util.List;

/* loaded from: classes.dex */
public final class BugReportHandlerUtil {
    private static final String INTENT_BUGREPORT_REQUESTED = "com.android.internal.intent.action.BUGREPORT_REQUESTED";
    private static final String INTENT_GET_BUGREPORT_HANDLER_RESPONSE = "com.android.internal.intent.action.GET_BUGREPORT_HANDLER_RESPONSE";
    private static final String SHELL_APP_PACKAGE = "com.android.shell";
    private static final String TAG = "ActivityManager";

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isBugReportHandlerEnabled(Context context) {
        return context.getResources().getBoolean(17891385);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean launchBugReportHandlerApp(Context context) {
        String handlerApp;
        int handlerUser;
        String str;
        UserHandle of;
        BugreportHandlerResponseBroadcastReceiver bugreportHandlerResponseBroadcastReceiver;
        if (isBugReportHandlerEnabled(context)) {
            String handlerApp2 = getCustomBugReportHandlerApp(context);
            if (isShellApp(handlerApp2)) {
                return false;
            }
            int handlerUser2 = getCustomBugReportHandlerUser(context);
            if (!isValidBugReportHandlerApp(handlerApp2)) {
                handlerApp = getDefaultBugReportHandlerApp(context);
                handlerUser = 0;
            } else if (getBugReportHandlerAppReceivers(context, handlerApp2, handlerUser2).isEmpty()) {
                String handlerApp3 = getDefaultBugReportHandlerApp(context);
                resetCustomBugreportHandlerAppAndUser(context);
                handlerApp = handlerApp3;
                handlerUser = 0;
            } else {
                handlerApp = handlerApp2;
                handlerUser = handlerUser2;
            }
            if (isShellApp(handlerApp) || !isValidBugReportHandlerApp(handlerApp) || getBugReportHandlerAppReceivers(context, handlerApp, handlerUser).isEmpty()) {
                return false;
            }
            if (getBugReportHandlerAppResponseReceivers(context, handlerApp, handlerUser).isEmpty()) {
                launchBugReportHandlerApp(context, handlerApp, handlerUser);
                return true;
            }
            Slog.i(TAG, "Getting response from bug report handler app: " + handlerApp);
            Intent intent = new Intent(INTENT_GET_BUGREPORT_HANDLER_RESPONSE);
            intent.setPackage(handlerApp);
            intent.addFlags(AudioFormat.EVRC);
            intent.addFlags(16777216);
            long identity = Binder.clearCallingIdentity();
            try {
                of = UserHandle.of(handlerUser);
                bugreportHandlerResponseBroadcastReceiver = new BugreportHandlerResponseBroadcastReceiver(handlerApp, handlerUser);
                str = TAG;
            } catch (RuntimeException e) {
                e = e;
                str = TAG;
            } catch (Throwable th) {
                e = th;
                Binder.restoreCallingIdentity(identity);
                throw e;
            }
            try {
                try {
                    context.sendOrderedBroadcastAsUser(intent, of, "android.permission.DUMP", -1, null, bugreportHandlerResponseBroadcastReceiver, null, 0, null, null);
                    Binder.restoreCallingIdentity(identity);
                    return true;
                } catch (Throwable th2) {
                    e = th2;
                    Binder.restoreCallingIdentity(identity);
                    throw e;
                }
            } catch (RuntimeException e2) {
                e = e2;
                Slog.e(str, "Error while trying to get response from bug report handler app.", e);
                Binder.restoreCallingIdentity(identity);
                return false;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void launchBugReportHandlerApp(Context context, String handlerApp, int handlerUser) {
        Slog.i(TAG, "Launching bug report handler app: " + handlerApp);
        Intent intent = new Intent(INTENT_BUGREPORT_REQUESTED);
        intent.setPackage(handlerApp);
        intent.addFlags(AudioFormat.EVRC);
        intent.addFlags(16777216);
        BroadcastOptions options = BroadcastOptions.makeBasic();
        options.setBackgroundActivityStartsAllowed(true);
        long identity = Binder.clearCallingIdentity();
        try {
            try {
                context.sendBroadcastAsUser(intent, UserHandle.of(handlerUser), "android.permission.DUMP", options.toBundle());
            } catch (RuntimeException e) {
                Slog.e(TAG, "Error while trying to launch bugreport handler app.", e);
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private static String getCustomBugReportHandlerApp(Context context) {
        return Settings.Global.getString(context.getContentResolver(), "custom_bugreport_handler_app");
    }

    private static int getCustomBugReportHandlerUser(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), "custom_bugreport_handler_user", -10000);
    }

    private static boolean isShellApp(String app) {
        return SHELL_APP_PACKAGE.equals(app);
    }

    private static boolean isValidBugReportHandlerApp(String app) {
        return !TextUtils.isEmpty(app) && isBugreportWhitelistedApp(app);
    }

    private static boolean isBugreportWhitelistedApp(String app) {
        ArraySet<String> whitelistedApps = SystemConfig.getInstance().getBugreportWhitelistedPackages();
        return whitelistedApps.contains(app);
    }

    private static List<ResolveInfo> getBugReportHandlerAppReceivers(Context context, String handlerApp, int handlerUser) {
        Intent intent = new Intent(INTENT_BUGREPORT_REQUESTED);
        intent.setPackage(handlerApp);
        return context.getPackageManager().queryBroadcastReceiversAsUser(intent, 1048576, handlerUser);
    }

    private static List<ResolveInfo> getBugReportHandlerAppResponseReceivers(Context context, String handlerApp, int handlerUser) {
        Intent intent = new Intent(INTENT_GET_BUGREPORT_HANDLER_RESPONSE);
        intent.setPackage(handlerApp);
        return context.getPackageManager().queryBroadcastReceiversAsUser(intent, 1048576, handlerUser);
    }

    private static String getDefaultBugReportHandlerApp(Context context) {
        return context.getResources().getString(17039881);
    }

    private static void resetCustomBugreportHandlerAppAndUser(Context context) {
        long identity = Binder.clearCallingIdentity();
        try {
            Settings.Global.putString(context.getContentResolver(), "custom_bugreport_handler_app", getDefaultBugReportHandlerApp(context));
            Settings.Global.putInt(context.getContentResolver(), "custom_bugreport_handler_user", 0);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class BugreportHandlerResponseBroadcastReceiver extends BroadcastReceiver {
        private final String handlerApp;
        private final int handlerUser;

        BugreportHandlerResponseBroadcastReceiver(String handlerApp, int handlerUser) {
            this.handlerApp = handlerApp;
            this.handlerUser = handlerUser;
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (getResultCode() == -1) {
                BugReportHandlerUtil.launchBugReportHandlerApp(context, this.handlerApp, this.handlerUser);
                return;
            }
            Slog.w(BugReportHandlerUtil.TAG, "Request bug report because no response from handler app.");
            BugreportManager bugreportManager = (BugreportManager) context.getSystemService(BugreportManager.class);
            bugreportManager.requestBugreport(new BugreportParams(1), null, null);
        }
    }
}