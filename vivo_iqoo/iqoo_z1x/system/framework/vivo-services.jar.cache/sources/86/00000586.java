package com.android.server.wm;

import android.text.TextUtils;
import com.android.internal.os.TransferPipe;
import java.io.PrintWriter;
import java.util.function.Consumer;

/* loaded from: classes.dex */
public class VivoWindowManagerShellCommandImpl implements IVivoWindowManagerShellCommand {
    private final WindowManagerService mInternal;
    private final WindowManagerShellCommand mShell;

    public VivoWindowManagerShellCommandImpl(WindowManagerShellCommand shell, WindowManagerService service) {
        this.mShell = shell;
        this.mInternal = service;
    }

    public int runDumpWindowViewHierarchy(final PrintWriter pw) {
        if (!this.mInternal.checkCallingPermission("android.permission.DUMP", "runDumpVisibleWindowViews()")) {
            throw new SecurityException("Requires DUMP permission");
        }
        final String title = this.mShell.getNextArgRequired();
        if (TextUtils.isEmpty(title)) {
            return 0;
        }
        try {
            synchronized (this.mInternal.mGlobalLock) {
                this.mInternal.mRoot.forAllWindows(new Consumer() { // from class: com.android.server.wm.-$$Lambda$VivoWindowManagerShellCommandImpl$XuTipi1sMTXOkhuGIGdouKmxDko
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        VivoWindowManagerShellCommandImpl.this.lambda$runDumpWindowViewHierarchy$0$VivoWindowManagerShellCommandImpl(title, pw, (WindowState) obj);
                    }
                }, false);
            }
        } catch (Exception e) {
            pw.println("Error fetching dump " + e.getMessage());
        }
        return 0;
    }

    public /* synthetic */ void lambda$runDumpWindowViewHierarchy$0$VivoWindowManagerShellCommandImpl(String title, PrintWriter pw, WindowState w) {
        String windowName = w.getName();
        if (!TextUtils.isEmpty(windowName)) {
            String windowNameToCompare = windowName.replace(" ", "-");
            if (w.isVisible() && title.equals(windowNameToCompare)) {
                try {
                    TransferPipe pipe = new TransferPipe();
                    w.mClient.executeCommand("dump-view-hierarchy", (String) null, pipe.getWriteFd());
                    pipe.go(this.mShell.getOutFileDescriptor(), 2000L);
                    pipe.kill();
                } catch (Exception e) {
                    pw.println("Error fetching dump " + e.getMessage());
                }
            }
        }
    }
}