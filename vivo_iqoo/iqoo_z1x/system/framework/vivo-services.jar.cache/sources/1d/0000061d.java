package com.vivo.services.autorecover;

import android.os.ShellCommand;
import java.io.PrintWriter;

/* loaded from: classes.dex */
public class SystemAutoRecoverShellCommand extends ShellCommand {
    private final SystemAutoRecoverService mService;

    public SystemAutoRecoverShellCommand(SystemAutoRecoverService service) {
        this.mService = service;
    }

    /* JADX WARN: Removed duplicated region for block: B:26:0x004b  */
    /* JADX WARN: Removed duplicated region for block: B:32:0x006c A[Catch: Exception -> 0x0077, TRY_LEAVE, TryCatch #0 {Exception -> 0x0077, blocks: (B:9:0x0016, B:28:0x004f, B:30:0x0054, B:31:0x0062, B:32:0x006c, B:16:0x002c, B:19:0x0036, B:22:0x0040), top: B:37:0x0016 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public int onCommand(java.lang.String r9) {
        /*
            r8 = this;
            java.lang.String r0 = "persist.vivo.autorecoverservice.dump"
            r1 = 0
            boolean r0 = android.os.SystemProperties.getBoolean(r0, r1)
            if (r0 != 0) goto La
            return r1
        La:
            if (r9 != 0) goto L11
            int r1 = r8.handleDefaultCommands(r9)
            return r1
        L11:
            java.io.PrintWriter r2 = r8.getOutPrintWriter()
            r3 = -1
            int r4 = r9.hashCode()     // Catch: java.lang.Exception -> L77
            r5 = -1298848381(0xffffffffb2952583, float:-1.7362941E-8)
            r6 = 2
            r7 = 1
            if (r4 == r5) goto L40
            r5 = 1430702443(0x5546c96b, float:1.36605241E13)
            if (r4 == r5) goto L36
            r5 = 1671308008(0x639e22e8, float:5.8342016E21)
            if (r4 == r5) goto L2c
        L2b:
            goto L49
        L2c:
            java.lang.String r4 = "disable"
            boolean r4 = r9.equals(r4)     // Catch: java.lang.Exception -> L77
            if (r4 == 0) goto L2b
            r3 = r7
            goto L49
        L36:
            java.lang.String r4 = "setparam"
            boolean r4 = r9.equals(r4)     // Catch: java.lang.Exception -> L77
            if (r4 == 0) goto L2b
            r3 = r6
            goto L49
        L40:
            java.lang.String r4 = "enable"
            boolean r4 = r9.equals(r4)     // Catch: java.lang.Exception -> L77
            if (r4 == 0) goto L2b
            r3 = r1
        L49:
            if (r3 == 0) goto L6c
            if (r3 == r7) goto L62
            if (r3 == r6) goto L54
            int r1 = r8.handleDefaultCommands(r9)     // Catch: java.lang.Exception -> L77
            return r1
        L54:
            java.lang.String r3 = r8.getNextArgRequired()     // Catch: java.lang.Exception -> L77
            java.lang.String r4 = r8.getNextArgRequired()     // Catch: java.lang.Exception -> L77
            com.vivo.services.autorecover.SystemAutoRecoverService r5 = r8.mService     // Catch: java.lang.Exception -> L77
            r5.setParam(r3, r4)     // Catch: java.lang.Exception -> L77
            goto L76
        L62:
            java.lang.String r3 = r8.getNextArgRequired()     // Catch: java.lang.Exception -> L77
            com.vivo.services.autorecover.SystemAutoRecoverService r4 = r8.mService     // Catch: java.lang.Exception -> L77
            r4.forceEnable(r3, r1)     // Catch: java.lang.Exception -> L77
            goto L76
        L6c:
            java.lang.String r3 = r8.getNextArgRequired()     // Catch: java.lang.Exception -> L77
            com.vivo.services.autorecover.SystemAutoRecoverService r4 = r8.mService     // Catch: java.lang.Exception -> L77
            r4.forceEnable(r3, r7)     // Catch: java.lang.Exception -> L77
        L76:
            goto L8c
        L77:
            r3 = move-exception
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            java.lang.String r5 = "SystemAutoRecoverShellCommand cause exception: "
            r4.append(r5)
            r4.append(r3)
            java.lang.String r4 = r4.toString()
            r2.println(r4)
        L8c:
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.vivo.services.autorecover.SystemAutoRecoverShellCommand.onCommand(java.lang.String):int");
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("System auto recover service command options");
        pw.println("  help");
        pw.println("      Print this help text.");
        pw.println("  enable/disable");
        pw.println("      Enable or disable features.");
        pw.println("    input-check");
        pw.println("      Check transparent or black window from input.");
        pw.println("    focus-change-check");
        pw.println("      Check invalid window size or alpha from focus change.");
        pw.println("    punish");
        pw.println("      Punish exception source immediately.");
        pw.println("    force-stop-freezing");
        pw.println("      Force recover Display Forzen issue.");
        pw.println("    starting-window-back-key-opt");
        pw.println("      Go home when back key trigger with starting window shown.");
        pw.println("    punish-all");
        pw.println("      All exception source will be punished.");
        pw.println("    ignore-debug");
        pw.println("      Punish exception source even in debug mode");
        pw.println("    nofocus-force-bg-recover");
        pw.println("      Force recover no focus time out issue when background");
        pw.println("    nofocus-force-fg-recover");
        pw.println("      Force recover no focus time out issue when foreground");
        pw.println("  setParam");
        pw.println("      Set parameter of features.");
    }
}