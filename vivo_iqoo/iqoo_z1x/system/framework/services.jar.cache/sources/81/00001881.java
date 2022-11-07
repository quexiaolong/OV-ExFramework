package com.android.server.role;

import android.app.role.IRoleManager;
import android.os.Bundle;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.ShellCommand;
import android.os.UserHandle;
import android.util.Log;
import com.android.server.role.RoleManagerShellCommand;
import java.io.PrintWriter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class RoleManagerShellCommand extends ShellCommand {
    private final IRoleManager mRoleManager;

    /* JADX INFO: Access modifiers changed from: package-private */
    public RoleManagerShellCommand(IRoleManager roleManager) {
        this.mRoleManager = roleManager;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class CallbackFuture extends CompletableFuture<Void> {
        private CallbackFuture() {
        }

        public RemoteCallback createCallback() {
            return new RemoteCallback(new RemoteCallback.OnResultListener() { // from class: com.android.server.role.-$$Lambda$RoleManagerShellCommand$CallbackFuture$ya02agfKUbaiv_zXc0xWEop421Q
                public final void onResult(Bundle bundle) {
                    RoleManagerShellCommand.CallbackFuture.this.lambda$createCallback$0$RoleManagerShellCommand$CallbackFuture(bundle);
                }
            });
        }

        public /* synthetic */ void lambda$createCallback$0$RoleManagerShellCommand$CallbackFuture(Bundle result) {
            boolean successful = result != null;
            if (successful) {
                complete(null);
            } else {
                completeExceptionally(new RuntimeException("Failed"));
            }
        }

        public int waitForResult() {
            try {
                get(5L, TimeUnit.SECONDS);
                return 0;
            } catch (Exception e) {
                PrintWriter errPrintWriter = RoleManagerShellCommand.this.getErrPrintWriter();
                errPrintWriter.println("Error: see logcat for details.\n" + Log.getStackTraceString(e));
                return -1;
            }
        }
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
            r3 = -1831663689(0xffffffff92d307b7, float:-1.3317874E-27)
            r4 = 2
            r5 = 1
            if (r2 == r3) goto L36
            r3 = -1502066320(0xffffffffa6784970, float:-8.614181E-16)
            if (r2 == r3) goto L2c
            r3 = -1274754278(0xffffffffb404cb1a, float:-1.2367346E-7)
            if (r2 == r3) goto L22
        L21:
            goto L40
        L22:
            java.lang.String r2 = "remove-role-holder"
            boolean r2 = r7.equals(r2)     // Catch: android.os.RemoteException -> L5b
            if (r2 == 0) goto L21
            r2 = r5
            goto L41
        L2c:
            java.lang.String r2 = "clear-role-holders"
            boolean r2 = r7.equals(r2)     // Catch: android.os.RemoteException -> L5b
            if (r2 == 0) goto L21
            r2 = r4
            goto L41
        L36:
            java.lang.String r2 = "add-role-holder"
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
            int r1 = r6.runClearRoleHolders()     // Catch: android.os.RemoteException -> L5b
            return r1
        L51:
            int r1 = r6.runRemoveRoleHolder()     // Catch: android.os.RemoteException -> L5b
            return r1
        L56:
            int r1 = r6.runAddRoleHolder()     // Catch: android.os.RemoteException -> L5b
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
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.role.RoleManagerShellCommand.onCommand(java.lang.String):int");
    }

    private int getUserIdMaybe() {
        String option = getNextOption();
        if (option == null || !option.equals("--user")) {
            return 0;
        }
        int userId = UserHandle.parseUserArg(getNextArgRequired());
        return userId;
    }

    private int getFlagsMaybe() {
        String flags = getNextArg();
        if (flags == null) {
            return 0;
        }
        return Integer.parseInt(flags);
    }

    private int runAddRoleHolder() throws RemoteException {
        int userId = getUserIdMaybe();
        String roleName = getNextArgRequired();
        String packageName = getNextArgRequired();
        int flags = getFlagsMaybe();
        CallbackFuture future = new CallbackFuture();
        this.mRoleManager.addRoleHolderAsUser(roleName, packageName, flags, userId, future.createCallback());
        return future.waitForResult();
    }

    private int runRemoveRoleHolder() throws RemoteException {
        int userId = getUserIdMaybe();
        String roleName = getNextArgRequired();
        String packageName = getNextArgRequired();
        int flags = getFlagsMaybe();
        CallbackFuture future = new CallbackFuture();
        this.mRoleManager.removeRoleHolderAsUser(roleName, packageName, flags, userId, future.createCallback());
        return future.waitForResult();
    }

    private int runClearRoleHolders() throws RemoteException {
        int userId = getUserIdMaybe();
        String roleName = getNextArgRequired();
        int flags = getFlagsMaybe();
        CallbackFuture future = new CallbackFuture();
        this.mRoleManager.clearRoleHoldersAsUser(roleName, flags, userId, future.createCallback());
        return future.waitForResult();
    }

    public void onHelp() {
        PrintWriter pw = getOutPrintWriter();
        pw.println("Role manager (role) commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println();
        pw.println("  add-role-holder [--user USER_ID] ROLE PACKAGE [FLAGS]");
        pw.println("  remove-role-holder [--user USER_ID] ROLE PACKAGE [FLAGS]");
        pw.println("  clear-role-holders [--user USER_ID] ROLE [FLAGS]");
        pw.println();
    }
}