package com.vivo.services.security.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import com.vivo.face.common.data.Constants;

/* loaded from: classes.dex */
public class VivoPermissionReceiver extends BroadcastReceiver {
    private VivoPermissionService mVPS;

    public VivoPermissionReceiver(VivoPermissionService vps) {
        this.mVPS = null;
        this.mVPS = vps;
    }

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String packageName = intent.getDataString();
        if (packageName == null) {
            return;
        }
        String packageName2 = packageName.replace("package:", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        if (this.mVPS.isHiddenApplication(packageName2)) {
            VivoPermissionService.printfInfo("isHiddenApplication=" + packageName2);
            return;
        }
        boolean grantPermissions = intent.getBooleanExtra("grantPermissions", false);
        Intent vIntent = new Intent();
        int userId = UserHandle.getUserId(intent.getIntExtra("android.intent.extra.UID", 0));
        VivoPermissionService.printfInfo("onReceive action=" + action + ", pkgName: " + packageName2 + ", userId: " + userId);
        if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
            if (intent.hasExtra("vhAction") && "true".equals(intent.getStringExtra("vhAction"))) {
                VivoPermissionService.printfInfo("It is hide app reset to normal, just return.");
                return;
            }
            this.mVPS.updateForPackageAdded(packageName2, grantPermissions, userId);
            vIntent.setAction("com.vivo.services.security.client.PACKAGE_PERMISSION_ADDED");
            if (intent.hasExtra("install_resource")) {
                String install_resource = intent.getStringExtra("install_resource");
                boolean IsInstallSilence = intent.getBooleanExtra("IsInstallSilence", false);
                vIntent.putExtra("install_resource", install_resource);
                vIntent.putExtra("IsInstallSilence", IsInstallSilence);
            }
            boolean replace = intent.getBooleanExtra("android.intent.extra.REPLACING", false);
            if (!replace) {
                sendBroadcast(context, vIntent, packageName2, userId);
            }
        } else if ("android.intent.action.PACKAGE_REMOVED".equals(action)) {
            this.mVPS.updateForPackageRemoved(packageName2, userId);
            vIntent.setAction("com.vivo.services.security.client.PACKAGE_PERMISSION_REMOVED");
            sendBroadcast(context, vIntent, packageName2, userId);
        } else if ("android.intent.action.PACKAGE_REPLACED".equals(action)) {
            this.mVPS.updateForPackageReplaced(packageName2, userId);
            vIntent.setAction("com.vivo.services.security.client.PACKAGE_PERMISSION_REPLACED");
            sendBroadcast(context, vIntent, packageName2, userId);
        }
    }

    public void sendBroadcast(Context context, Intent intent, String packageName, int userId) {
        intent.putExtra("package", packageName);
        intent.setPackage("com.vivo.permissionmanager");
        intent.addFlags(67108864);
        context.sendBroadcastAsUser(intent, UserHandle.of(userId));
        VivoPermissionService.printfInfo("sendBroadcast-->intent=" + intent + "; packageName=" + packageName);
    }
}