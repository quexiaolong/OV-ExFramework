package com.android.server.pm.dex;

import android.content.pm.IPackageManager;
import android.os.RemoteException;
import android.util.Log;
import android.util.Slog;
import com.android.server.pm.PackageManagerService;
import dalvik.system.BaseDexClassLoader;
import dalvik.system.VMRuntime;
import java.util.Map;

/* loaded from: classes.dex */
public class SystemServerDexLoadReporter implements BaseDexClassLoader.Reporter {
    private final IPackageManager mPackageManager;
    private static final String TAG = "SystemServerDexLoadReporter";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);

    private SystemServerDexLoadReporter(IPackageManager pm) {
        this.mPackageManager = pm;
    }

    public void report(Map<String, String> classLoaderContextMap) {
        if (DEBUG) {
            Slog.i(TAG, "Reporting " + classLoaderContextMap);
        }
        if (classLoaderContextMap.isEmpty()) {
            Slog.wtf(TAG, "Bad call to DexLoadReporter: empty classLoaderContextMap");
            return;
        }
        try {
            this.mPackageManager.notifyDexLoad(PackageManagerService.PLATFORM_PACKAGE_NAME, classLoaderContextMap, VMRuntime.getRuntime().vmInstructionSet());
        } catch (RemoteException e) {
        }
    }

    public static void configureSystemServerDexReporter(IPackageManager pm) {
        Slog.i(TAG, "Configuring system server dex reporter");
        SystemServerDexLoadReporter reporter = new SystemServerDexLoadReporter(pm);
        BaseDexClassLoader.setReporter(reporter);
        ClassLoader currrentClassLoader = reporter.getClass().getClassLoader();
        if (currrentClassLoader instanceof BaseDexClassLoader) {
            ((BaseDexClassLoader) currrentClassLoader).reportClassLoaderChain();
            return;
        }
        Slog.wtf(TAG, "System server class loader is not a BaseDexClassLoader. type=" + currrentClassLoader.getClass().getName());
    }
}