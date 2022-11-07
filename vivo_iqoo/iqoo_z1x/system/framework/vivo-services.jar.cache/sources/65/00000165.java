package com.android.server.biometrics.fingerprint;

import android.os.RemoteException;
import android.os.ShellCommand;
import android.service.quicksettings.TileService;
import com.vivo.face.common.data.Constants;
import com.vivo.fingerprint.ui.IFingerprintUI;
import com.vivo.services.security.server.VivoPermissionUtils;
import java.io.PrintWriter;

/* loaded from: classes.dex */
public class FingerprintUIShellCommand extends ShellCommand {
    private final IFingerprintUI mInterface;

    public FingerprintUIShellCommand(IFingerprintUI service) {
        this.mInterface = service;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public int onCommand(String cmd) {
        boolean z;
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        try {
            switch (cmd.hashCode()) {
                case -1834189773:
                    if (cmd.equals("hide-dialog")) {
                        z = true;
                        break;
                    }
                    z = true;
                    break;
                case -1325619688:
                    if (cmd.equals("show-dialog")) {
                        z = true;
                        break;
                    }
                    z = true;
                    break;
                case 1612300298:
                    if (cmd.equals("check-support")) {
                        z = true;
                        break;
                    }
                    z = true;
                    break;
                case 1629310709:
                    if (cmd.equals("expand-notifications")) {
                        z = false;
                        break;
                    }
                    z = true;
                    break;
                default:
                    z = true;
                    break;
            }
            if (z) {
                if (z) {
                    PrintWriter pw = getOutPrintWriter();
                    pw.println(String.valueOf(TileService.isQuickSettingsSupported()));
                    return 0;
                } else if (!z) {
                    if (z) {
                        return runHideDialog();
                    }
                    return handleDefaultCommands(cmd);
                } else {
                    return runShowDialog();
                }
            }
            return runExpandNotifications();
        } catch (RemoteException e) {
            PrintWriter pw2 = getOutPrintWriter();
            pw2.println("Remote exception: " + e);
            return -1;
        }
    }

    private int runExpandNotifications() throws RemoteException {
        return 0;
    }

    private int runShowDialog() throws RemoteException {
        this.mInterface.showFingerprintDialog(VivoPermissionUtils.OS_PKG, 1, false);
        return 0;
    }

    private int runHideDialog() throws RemoteException {
        this.mInterface.hideFingerprintDialog(0);
        return 0;
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Status bar commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        pw.println("  expand-notifications");
        pw.println("    Open the notifications panel.");
        pw.println(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        pw.println("  check-support");
        pw.println("    Check if this device supports QS + APIs");
        pw.println(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        pw.println("  show-dialog");
        pw.println("    Show fingerprint dialog");
        pw.println(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        pw.println("  hide-dialog");
        pw.println("    Hide fingerprint dialog");
        pw.println(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
    }
}