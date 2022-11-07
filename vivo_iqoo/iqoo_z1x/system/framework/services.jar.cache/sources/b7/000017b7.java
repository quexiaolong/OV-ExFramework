package com.android.server.power;

import android.content.Intent;
import android.os.IPowerManager;
import android.os.RemoteException;
import android.os.ShellCommand;
import java.io.PrintWriter;

/* loaded from: classes2.dex */
class PowerManagerShellCommand extends ShellCommand {
    private static final int LOW_POWER_MODE_ON = 1;
    final IPowerManager mInterface;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PowerManagerShellCommand(IPowerManager service) {
        this.mInterface = service;
    }

    /* JADX WARN: Removed duplicated region for block: B:24:0x0043  */
    /* JADX WARN: Removed duplicated region for block: B:32:0x0056 A[Catch: RemoteException -> 0x005b, TRY_LEAVE, TryCatch #0 {RemoteException -> 0x005b, blocks: (B:6:0x000c, B:26:0x0047, B:28:0x004c, B:30:0x0051, B:32:0x0056, B:13:0x0022, B:16:0x002c, B:19:0x0036), top: B:37:0x000c }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public int onCommand(java.lang.String r7) {
        /*
            r6 = this;
            if (r7 != 0) goto L7
            int r0 = r6.handleDefaultCommands(r7)
            return r0
        L7:
            java.io.PrintWriter r0 = r6.getOutPrintWriter()
            r1 = -1
            int r2 = r7.hashCode()     // Catch: android.os.RemoteException -> L5b
            r3 = -531688203(0xffffffffe04f14f5, float:-5.9687283E19)
            r4 = 2
            r5 = 1
            if (r2 == r3) goto L36
            r3 = 1032507032(0x3d8ace98, float:0.06777686)
            if (r2 == r3) goto L2c
            r3 = 1369181230(0x519c0c2e, float:8.3777405E10)
            if (r2 == r3) goto L22
        L21:
            goto L40
        L22:
            java.lang.String r2 = "set-mode"
            boolean r2 = r7.equals(r2)     // Catch: android.os.RemoteException -> L5b
            if (r2 == 0) goto L21
            r2 = r5
            goto L41
        L2c:
            java.lang.String r2 = "set-fixed-performance-mode-enabled"
            boolean r2 = r7.equals(r2)     // Catch: android.os.RemoteException -> L5b
            if (r2 == 0) goto L21
            r2 = r4
            goto L41
        L36:
            java.lang.String r2 = "set-adaptive-power-saver-enabled"
            boolean r2 = r7.equals(r2)     // Catch: android.os.RemoteException -> L5b
            if (r2 == 0) goto L21
            r2 = 0
            goto L41
        L40:
            r2 = r1
        L41:
            if (r2 == 0) goto L56
            if (r2 == r5) goto L51
            if (r2 == r4) goto L4c
            int r1 = r6.handleDefaultCommands(r7)     // Catch: android.os.RemoteException -> L5b
            return r1
        L4c:
            int r1 = r6.runSetFixedPerformanceModeEnabled()     // Catch: android.os.RemoteException -> L5b
            return r1
        L51:
            int r1 = r6.runSetMode()     // Catch: android.os.RemoteException -> L5b
            return r1
        L56:
            int r1 = r6.runSetAdaptiveEnabled()     // Catch: android.os.RemoteException -> L5b
            return r1
        L5b:
            r2 = move-exception
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "Remote exception: "
            r3.append(r4)
            r3.append(r2)
            java.lang.String r3 = r3.toString()
            r0.println(r3)
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.power.PowerManagerShellCommand.onCommand(java.lang.String):int");
    }

    private int runSetAdaptiveEnabled() throws RemoteException {
        this.mInterface.setAdaptivePowerSaveEnabled(Boolean.parseBoolean(getNextArgRequired()));
        return 0;
    }

    private int runSetMode() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        try {
            int mode = Integer.parseInt(getNextArgRequired());
            this.mInterface.setPowerSaveModeEnabled(mode == 1);
            return 0;
        } catch (RuntimeException ex) {
            pw.println("Error: " + ex.toString());
            return -1;
        }
    }

    private int runSetFixedPerformanceModeEnabled() throws RemoteException {
        boolean success = this.mInterface.setPowerModeChecked(3, Boolean.parseBoolean(getNextArgRequired()));
        if (!success) {
            PrintWriter ew = getErrPrintWriter();
            ew.println("Failed to set FIXED_PERFORMANCE mode");
            ew.println("This is likely because Power HAL AIDL is not implemented on this device");
        }
        return success ? 0 : -1;
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Power manager (power) commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("");
        pw.println("  set-adaptive-power-saver-enabled [true|false]");
        pw.println("    enables or disables adaptive power saver.");
        pw.println("  set-mode MODE");
        pw.println("    sets the power mode of the device to MODE.");
        pw.println("    1 turns low power mode on and 0 turns low power mode off.");
        pw.println("  set-fixed-performance-mode-enabled [true|false]");
        pw.println("    enables or disables fixed performance mode");
        pw.println("    note: this will affect system performance and should only be used");
        pw.println("          during development");
        pw.println();
        Intent.printIntentArgsHelp(pw, "");
    }
}