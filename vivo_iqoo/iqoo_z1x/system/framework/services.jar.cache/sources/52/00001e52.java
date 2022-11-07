package com.android.server.wm;

import android.content.res.Resources;
import android.provider.DeviceConfig;
import android.util.ArraySet;
import com.android.internal.os.BackgroundThread;
import com.android.server.wm.utils.DeviceConfigInterface;
import java.io.PrintWriter;
import java.util.Iterator;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class HighRefreshRateBlacklist {
    private final String[] mDefaultBlacklist;
    private DeviceConfigInterface mDeviceConfig;
    private final ArraySet<String> mBlacklistedPackages = new ArraySet<>();
    private final Object mLock = new Object();
    private OnPropertiesChangedListener mListener = new OnPropertiesChangedListener();

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HighRefreshRateBlacklist create(Resources r) {
        return new HighRefreshRateBlacklist(r, DeviceConfigInterface.REAL);
    }

    HighRefreshRateBlacklist(Resources r, DeviceConfigInterface deviceConfig) {
        this.mDefaultBlacklist = r.getStringArray(17236042);
        this.mDeviceConfig = deviceConfig;
        deviceConfig.addOnPropertiesChangedListener("display_manager", BackgroundThread.getExecutor(), this.mListener);
        String property = this.mDeviceConfig.getProperty("display_manager", "high_refresh_rate_blacklist");
        updateBlacklist(property);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateBlacklist(String property) {
        synchronized (this.mLock) {
            this.mBlacklistedPackages.clear();
            int i = 0;
            if (property != null) {
                String[] packages = property.split(",");
                int length = packages.length;
                while (i < length) {
                    String pkg = packages[i];
                    String pkgName = pkg.trim();
                    if (!pkgName.isEmpty()) {
                        this.mBlacklistedPackages.add(pkgName);
                    }
                    i++;
                }
            } else {
                String[] strArr = this.mDefaultBlacklist;
                int length2 = strArr.length;
                while (i < length2) {
                    String pkg2 = strArr[i];
                    this.mBlacklistedPackages.add(pkg2);
                    i++;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isBlacklisted(String packageName) {
        boolean contains;
        synchronized (this.mLock) {
            contains = this.mBlacklistedPackages.contains(packageName);
        }
        return contains;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw) {
        pw.println("High Refresh Rate Blacklist");
        pw.println("  Packages:");
        synchronized (this.mLock) {
            Iterator<String> it = this.mBlacklistedPackages.iterator();
            while (it.hasNext()) {
                String pkg = it.next();
                pw.println("    " + pkg);
            }
        }
    }

    void dispose() {
        this.mDeviceConfig.removeOnPropertiesChangedListener(this.mListener);
        this.mDeviceConfig = null;
        this.mBlacklistedPackages.clear();
    }

    /* loaded from: classes2.dex */
    private class OnPropertiesChangedListener implements DeviceConfig.OnPropertiesChangedListener {
        private OnPropertiesChangedListener() {
        }

        public void onPropertiesChanged(DeviceConfig.Properties properties) {
            if (properties.getKeyset().contains("high_refresh_rate_blacklist")) {
                HighRefreshRateBlacklist.this.updateBlacklist(properties.getString("high_refresh_rate_blacklist", (String) null));
            }
        }
    }
}