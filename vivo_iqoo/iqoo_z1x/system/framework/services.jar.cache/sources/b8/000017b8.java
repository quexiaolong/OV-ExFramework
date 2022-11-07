package com.android.server.power;

import android.content.Context;
import android.os.Environment;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import com.android.server.pm.Settings;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/* loaded from: classes2.dex */
final class PreRebootLogger {
    private static final String PREREBOOT_DIR = "prereboot";
    private static final String TAG = "PreRebootLogger";
    private static final String[] BUFFERS_TO_DUMP = {"system"};
    private static final String[] SERVICES_TO_DUMP = {"rollback", Settings.ATTR_PACKAGE};
    private static final Object sLock = new Object();
    private static final long MAX_DUMP_TIME = TimeUnit.SECONDS.toMillis(20);

    PreRebootLogger() {
    }

    public static void log(Context context) {
        log(context, getDumpDir());
    }

    static void log(Context context, File dumpDir) {
        if (needDump(context)) {
            dump(dumpDir, MAX_DUMP_TIME);
        } else {
            wipe(dumpDir);
        }
    }

    private static boolean needDump(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), "adb_enabled", 0) == 1 && !context.getPackageManager().getPackageInstaller().getActiveStagedSessions().isEmpty();
    }

    static void dump(final File dumpDir, long maxWaitTime) {
        Slog.d(TAG, "Dumping pre-reboot information...");
        final AtomicBoolean done = new AtomicBoolean(false);
        Thread t = new Thread(new Runnable() { // from class: com.android.server.power.-$$Lambda$PreRebootLogger$p8FewhqoJ8SIfUcoA5y2K00i1I4
            @Override // java.lang.Runnable
            public final void run() {
                PreRebootLogger.lambda$dump$0(dumpDir, done);
            }
        });
        t.start();
        try {
            t.join(maxWaitTime);
        } catch (InterruptedException e) {
            Slog.e(TAG, "Failed to dump pre-reboot information due to interrupted", e);
        }
        if (!done.get()) {
            Slog.w(TAG, "Failed to dump pre-reboot information due to timeout");
        }
    }

    public static /* synthetic */ void lambda$dump$0(File dumpDir, AtomicBoolean done) {
        String[] strArr;
        String[] strArr2;
        synchronized (sLock) {
            for (String buffer : BUFFERS_TO_DUMP) {
                dumpLogsLocked(dumpDir, buffer);
            }
            for (String service : SERVICES_TO_DUMP) {
                dumpServiceLocked(dumpDir, service);
            }
        }
        done.set(true);
    }

    private static void wipe(File dumpDir) {
        File[] listFiles;
        Slog.d(TAG, "Wiping pre-reboot information...");
        synchronized (sLock) {
            for (File file : dumpDir.listFiles()) {
                file.delete();
            }
        }
    }

    private static File getDumpDir() {
        File dumpDir = new File(Environment.getDataMiscDirectory(), PREREBOOT_DIR);
        if (!dumpDir.exists() || !dumpDir.isDirectory()) {
            throw new UnsupportedOperationException("Pre-reboot dump directory not found");
        }
        return dumpDir;
    }

    private static void dumpLogsLocked(File dumpDir, String buffer) {
        try {
            File dumpFile = new File(dumpDir, buffer);
            if (dumpFile.createNewFile()) {
                dumpFile.setWritable(true, true);
            } else {
                new FileWriter(dumpFile, false).flush();
            }
            String[] cmdline = {"logcat", "-d", "-b", buffer, "-f", dumpFile.getAbsolutePath()};
            Runtime.getRuntime().exec(cmdline).waitFor();
        } catch (IOException | InterruptedException e) {
            Slog.e(TAG, "Failed to dump system log buffer before reboot", e);
        }
    }

    private static void dumpServiceLocked(File dumpDir, String serviceName) {
        IBinder binder = ServiceManager.checkService(serviceName);
        if (binder == null) {
            return;
        }
        try {
            File dumpFile = new File(dumpDir, serviceName);
            ParcelFileDescriptor fd = ParcelFileDescriptor.open(dumpFile, 738197504);
            binder.dump(fd.getFileDescriptor(), (String[]) ArrayUtils.emptyArray(String.class));
        } catch (RemoteException | FileNotFoundException e) {
            Slog.e(TAG, String.format("Failed to dump %s service before reboot", serviceName), e);
        }
    }
}