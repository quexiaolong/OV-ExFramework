package com.android.server.webkit;

import android.os.RemoteException;
import android.os.ShellCommand;
import android.webkit.IWebViewUpdateService;
import java.io.PrintWriter;

/* loaded from: classes2.dex */
class WebViewUpdateServiceShellCommand extends ShellCommand {
    final IWebViewUpdateService mInterface;

    /* JADX INFO: Access modifiers changed from: package-private */
    public WebViewUpdateServiceShellCommand(IWebViewUpdateService service) {
        this.mInterface = service;
    }

    /* JADX WARN: Removed duplicated region for block: B:24:0x0044  */
    /* JADX WARN: Removed duplicated region for block: B:32:0x0057 A[Catch: RemoteException -> 0x005c, TRY_LEAVE, TryCatch #0 {RemoteException -> 0x005c, blocks: (B:6:0x000c, B:26:0x0048, B:28:0x004d, B:30:0x0052, B:32:0x0057, B:13:0x0023, B:16:0x002d, B:19:0x0037), top: B:37:0x000c }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public int onCommand(java.lang.String r8) {
        /*
            r7 = this;
            if (r8 != 0) goto L7
            int r0 = r7.handleDefaultCommands(r8)
            return r0
        L7:
            java.io.PrintWriter r0 = r7.getOutPrintWriter()
            r1 = -1
            int r2 = r8.hashCode()     // Catch: android.os.RemoteException -> L5c
            r3 = -1857752288(0xffffffff9144f320, float:-1.5536592E-28)
            r4 = 0
            r5 = 2
            r6 = 1
            if (r2 == r3) goto L37
            r3 = -1381305903(0xffffffffadaaf1d1, float:-1.943415E-11)
            if (r2 == r3) goto L2d
            r3 = 436183515(0x19ffa1db, float:2.6431755E-23)
            if (r2 == r3) goto L23
        L22:
            goto L41
        L23:
            java.lang.String r2 = "disable-multiprocess"
            boolean r2 = r8.equals(r2)     // Catch: android.os.RemoteException -> L5c
            if (r2 == 0) goto L22
            r2 = r5
            goto L42
        L2d:
            java.lang.String r2 = "set-webview-implementation"
            boolean r2 = r8.equals(r2)     // Catch: android.os.RemoteException -> L5c
            if (r2 == 0) goto L22
            r2 = r4
            goto L42
        L37:
            java.lang.String r2 = "enable-multiprocess"
            boolean r2 = r8.equals(r2)     // Catch: android.os.RemoteException -> L5c
            if (r2 == 0) goto L22
            r2 = r6
            goto L42
        L41:
            r2 = r1
        L42:
            if (r2 == 0) goto L57
            if (r2 == r6) goto L52
            if (r2 == r5) goto L4d
            int r1 = r7.handleDefaultCommands(r8)     // Catch: android.os.RemoteException -> L5c
            return r1
        L4d:
            int r1 = r7.enableMultiProcess(r4)     // Catch: android.os.RemoteException -> L5c
            return r1
        L52:
            int r1 = r7.enableMultiProcess(r6)     // Catch: android.os.RemoteException -> L5c
            return r1
        L57:
            int r1 = r7.setWebViewImplementation()     // Catch: android.os.RemoteException -> L5c
            return r1
        L5c:
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
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.webkit.WebViewUpdateServiceShellCommand.onCommand(java.lang.String):int");
    }

    private int setWebViewImplementation() throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        String shellChosenPackage = getNextArg();
        if (shellChosenPackage == null) {
            pw.println("Failed to switch, no PACKAGE provided.");
            pw.println("");
            helpSetWebViewImplementation();
            return 1;
        }
        String newPackage = this.mInterface.changeProviderAndSetting(shellChosenPackage);
        if (!shellChosenPackage.equals(newPackage)) {
            pw.println(String.format("Failed to switch to %s, the WebView implementation is now provided by %s.", shellChosenPackage, newPackage));
            return 1;
        }
        pw.println("Success");
        return 0;
    }

    private int enableMultiProcess(boolean enable) throws RemoteException {
        PrintWriter pw = getOutPrintWriter();
        this.mInterface.enableMultiProcess(enable);
        pw.println("Success");
        return 0;
    }

    public void helpSetWebViewImplementation() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("  set-webview-implementation PACKAGE");
        pw.println("    Set the WebView implementation to the specified package.");
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("WebView updater commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("");
        helpSetWebViewImplementation();
        pw.println("  enable-multiprocess");
        pw.println("    Enable multi-process mode for WebView");
        pw.println("  disable-multiprocess");
        pw.println("    Disable multi-process mode for WebView");
        pw.println();
    }
}