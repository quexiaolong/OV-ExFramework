package com.android.server.location;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/* loaded from: classes.dex */
public class SettingsHelper {
    private static final long DEFAULT_BACKGROUND_THROTTLE_INTERVAL_MS = 1800000;
    private static final long DEFAULT_BACKGROUND_THROTTLE_PROXIMITY_ALERT_INTERVAL_MS = 1800000;
    private static final float DEFAULT_COARSE_LOCATION_ACCURACY_M = 2000.0f;
    private static final String LOCATION_PACKAGE_BLACKLIST = "locationPackagePrefixBlacklist";
    private static final String LOCATION_PACKAGE_WHITELIST = "locationPackagePrefixWhitelist";
    private final LongGlobalSetting mBackgroundThrottleIntervalMs;
    private final StringSetCachedGlobalSetting mBackgroundThrottlePackageWhitelist;
    private final Context mContext;
    private final StringSetCachedGlobalSetting mIgnoreSettingsPackageWhitelist;
    private final IntegerSecureSetting mLocationMode;
    private final StringListCachedSecureSetting mLocationPackageBlacklist;
    private final StringListCachedSecureSetting mLocationPackageWhitelist;

    /* loaded from: classes.dex */
    public interface UserSettingChangedListener {
        void onSettingChanged(int i);
    }

    /* loaded from: classes.dex */
    public interface GlobalSettingChangedListener extends UserSettingChangedListener {
        void onSettingChanged();

        @Override // com.android.server.location.SettingsHelper.UserSettingChangedListener
        default void onSettingChanged(int userId) {
            onSettingChanged();
        }
    }

    public SettingsHelper(Context context, Handler handler) {
        this.mContext = context;
        this.mLocationMode = new IntegerSecureSetting(context, "location_mode", handler);
        this.mBackgroundThrottleIntervalMs = new LongGlobalSetting(context, "location_background_throttle_interval_ms", handler);
        this.mLocationPackageBlacklist = new StringListCachedSecureSetting(context, LOCATION_PACKAGE_BLACKLIST, handler);
        this.mLocationPackageWhitelist = new StringListCachedSecureSetting(context, LOCATION_PACKAGE_WHITELIST, handler);
        this.mBackgroundThrottlePackageWhitelist = new StringSetCachedGlobalSetting(context, "location_background_throttle_package_whitelist", $$Lambda$SettingsHelper$DVmNGa9ypltgL35WVwJuSTIxRS8.INSTANCE, handler);
        this.mIgnoreSettingsPackageWhitelist = new StringSetCachedGlobalSetting(context, "location_ignore_settings_package_whitelist", $$Lambda$SettingsHelper$Ez8giHaZAPYwS7zICeUtrlXPpBo.INSTANCE, handler);
    }

    public void onSystemReady() {
        this.mLocationMode.register();
        this.mBackgroundThrottleIntervalMs.register();
        this.mLocationPackageBlacklist.register();
        this.mLocationPackageWhitelist.register();
        this.mBackgroundThrottlePackageWhitelist.register();
        this.mIgnoreSettingsPackageWhitelist.register();
    }

    public boolean isLocationEnabled(int userId) {
        return this.mLocationMode.getValueForUser(0, userId) != 0;
    }

    public void setLocationEnabled(boolean enabled, int userId) {
        int i;
        long identity = Binder.clearCallingIdentity();
        try {
            ContentResolver contentResolver = this.mContext.getContentResolver();
            if (enabled) {
                i = 3;
            } else {
                i = 0;
            }
            Settings.Secure.putIntForUser(contentResolver, "location_mode", i, userId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void addOnLocationEnabledChangedListener(UserSettingChangedListener listener) {
        this.mLocationMode.addListener(listener);
    }

    public void removeOnLocationEnabledChangedListener(UserSettingChangedListener listener) {
        this.mLocationMode.removeListener(listener);
    }

    public long getBackgroundThrottleIntervalMs() {
        return this.mBackgroundThrottleIntervalMs.getValue(1800000L);
    }

    public void addOnBackgroundThrottleIntervalChangedListener(GlobalSettingChangedListener listener) {
        this.mBackgroundThrottleIntervalMs.addListener(listener);
    }

    public void removeOnBackgroundThrottleIntervalChangedListener(GlobalSettingChangedListener listener) {
        this.mBackgroundThrottleIntervalMs.removeListener(listener);
    }

    public boolean isLocationPackageBlacklisted(int userId, String packageName) {
        List<String> locationPackageBlacklist = this.mLocationPackageBlacklist.getValueForUser(userId);
        if (locationPackageBlacklist.isEmpty()) {
            return false;
        }
        List<String> locationPackageWhitelist = this.mLocationPackageWhitelist.getValueForUser(userId);
        for (String locationWhitelistPackage : locationPackageWhitelist) {
            if (packageName.startsWith(locationWhitelistPackage)) {
                return false;
            }
        }
        for (String locationBlacklistPackage : locationPackageBlacklist) {
            if (packageName.startsWith(locationBlacklistPackage)) {
                return true;
            }
        }
        return false;
    }

    public Set<String> getBackgroundThrottlePackageWhitelist() {
        return this.mBackgroundThrottlePackageWhitelist.getValue();
    }

    public void addOnBackgroundThrottlePackageWhitelistChangedListener(GlobalSettingChangedListener listener) {
        this.mBackgroundThrottlePackageWhitelist.addListener(listener);
    }

    public void removeOnBackgroundThrottlePackageWhitelistChangedListener(GlobalSettingChangedListener listener) {
        this.mBackgroundThrottlePackageWhitelist.removeListener(listener);
    }

    public Set<String> getIgnoreSettingsPackageWhitelist() {
        return this.mIgnoreSettingsPackageWhitelist.getValue();
    }

    public void addOnIgnoreSettingsPackageWhitelistChangedListener(GlobalSettingChangedListener listener) {
        this.mIgnoreSettingsPackageWhitelist.addListener(listener);
    }

    public void removeOnIgnoreSettingsPackageWhitelistChangedListener(GlobalSettingChangedListener listener) {
        this.mIgnoreSettingsPackageWhitelist.removeListener(listener);
    }

    public long getBackgroundThrottleProximityAlertIntervalMs() {
        long identity = Binder.clearCallingIdentity();
        try {
            return Settings.Global.getLong(this.mContext.getContentResolver(), "location_background_throttle_proximity_alert_interval_ms", 1800000L);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public float getCoarseLocationAccuracyM() {
        long identity = Binder.clearCallingIdentity();
        try {
            return Settings.Secure.getFloat(this.mContext.getContentResolver(), "locationCoarseAccuracy", DEFAULT_COARSE_LOCATION_ACCURACY_M);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void setLocationProviderAllowed(String provider, boolean enabled, int userId) {
        if ("fused".equals(provider) || "passive".equals(provider)) {
            return;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            ContentResolver contentResolver = this.mContext.getContentResolver();
            StringBuilder sb = new StringBuilder();
            sb.append(enabled ? "+" : "-");
            sb.append(provider);
            Settings.Secure.putStringForUser(contentResolver, "location_providers_allowed", sb.toString(), userId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ");
        int userId = ActivityManager.getCurrentUser();
        ipw.print("Location Enabled: ");
        ipw.println(isLocationEnabled(userId));
        List<String> locationPackageBlacklist = this.mLocationPackageBlacklist.getValueForUser(userId);
        if (!locationPackageBlacklist.isEmpty()) {
            ipw.println("Location Blacklisted Packages:");
            ipw.increaseIndent();
            for (String packageName : locationPackageBlacklist) {
                ipw.println(packageName);
            }
            ipw.decreaseIndent();
            List<String> locationPackageWhitelist = this.mLocationPackageWhitelist.getValueForUser(userId);
            if (!locationPackageWhitelist.isEmpty()) {
                ipw.println("Location Whitelisted Packages:");
                ipw.increaseIndent();
                for (String packageName2 : locationPackageWhitelist) {
                    ipw.println(packageName2);
                }
                ipw.decreaseIndent();
            }
        }
        Set<String> backgroundThrottlePackageWhitelist = this.mBackgroundThrottlePackageWhitelist.getValue();
        if (!backgroundThrottlePackageWhitelist.isEmpty()) {
            ipw.println("Throttling Whitelisted Packages:");
            ipw.increaseIndent();
            for (String packageName3 : backgroundThrottlePackageWhitelist) {
                ipw.println(packageName3);
            }
            ipw.decreaseIndent();
        }
        Set<String> ignoreSettingsPackageWhitelist = this.mIgnoreSettingsPackageWhitelist.getValue();
        if (!ignoreSettingsPackageWhitelist.isEmpty()) {
            ipw.println("Bypass Whitelisted Packages:");
            ipw.increaseIndent();
            for (String packageName4 : ignoreSettingsPackageWhitelist) {
                ipw.println(packageName4);
            }
            ipw.decreaseIndent();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static abstract class ObservingSetting extends ContentObserver {
        private final CopyOnWriteArrayList<UserSettingChangedListener> mListeners;
        private boolean mRegistered;

        private ObservingSetting(Handler handler) {
            super(handler);
            this.mListeners = new CopyOnWriteArrayList<>();
        }

        protected synchronized boolean isRegistered() {
            return this.mRegistered;
        }

        protected synchronized void register(Context context, Uri uri) {
            if (this.mRegistered) {
                return;
            }
            context.getContentResolver().registerContentObserver(uri, false, this, -1);
            this.mRegistered = true;
        }

        public void addListener(UserSettingChangedListener listener) {
            this.mListeners.add(listener);
        }

        public void removeListener(UserSettingChangedListener listener) {
            this.mListeners.remove(listener);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri, int userId) {
            if (LocationManagerService.D) {
                Log.d(LocationManagerService.TAG, "location setting changed [u" + userId + "]: " + uri);
            }
            Iterator<UserSettingChangedListener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                UserSettingChangedListener listener = it.next();
                listener.onSettingChanged(userId);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class IntegerSecureSetting extends ObservingSetting {
        private final Context mContext;
        private final String mSettingName;

        private IntegerSecureSetting(Context context, String settingName, Handler handler) {
            super(handler);
            this.mContext = context;
            this.mSettingName = settingName;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void register() {
            register(this.mContext, Settings.Secure.getUriFor(this.mSettingName));
        }

        public int getValueForUser(int defaultValue, int userId) {
            long identity = Binder.clearCallingIdentity();
            try {
                return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), this.mSettingName, defaultValue, userId);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class StringListCachedSecureSetting extends ObservingSetting {
        private int mCachedUserId;
        private List<String> mCachedValue;
        private final Context mContext;
        private final String mSettingName;

        private StringListCachedSecureSetting(Context context, String settingName, Handler handler) {
            super(handler);
            this.mContext = context;
            this.mSettingName = settingName;
            this.mCachedUserId = -10000;
        }

        public void register() {
            register(this.mContext, Settings.Secure.getUriFor(this.mSettingName));
        }

        public synchronized List<String> getValueForUser(int userId) {
            List<String> value;
            Preconditions.checkArgument(userId != -10000);
            value = this.mCachedValue;
            if (userId != this.mCachedUserId) {
                long identity = Binder.clearCallingIdentity();
                try {
                    String setting = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), this.mSettingName, userId);
                    if (TextUtils.isEmpty(setting)) {
                        try {
                            value = Collections.emptyList();
                        } catch (Throwable th) {
                            th = th;
                            Binder.restoreCallingIdentity(identity);
                            throw th;
                        }
                    } else {
                        value = Arrays.asList(setting.split(","));
                    }
                    Binder.restoreCallingIdentity(identity);
                    if (isRegistered()) {
                        this.mCachedUserId = userId;
                        this.mCachedValue = value;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
            return value;
        }

        public synchronized void invalidateForUser(int userId) {
            if (this.mCachedUserId == userId) {
                this.mCachedUserId = -10000;
                this.mCachedValue = null;
            }
        }

        @Override // com.android.server.location.SettingsHelper.ObservingSetting, android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri, int userId) {
            invalidateForUser(userId);
            super.onChange(selfChange, uri, userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class LongGlobalSetting extends ObservingSetting {
        private final Context mContext;
        private final String mSettingName;

        private LongGlobalSetting(Context context, String settingName, Handler handler) {
            super(handler);
            this.mContext = context;
            this.mSettingName = settingName;
        }

        public void register() {
            register(this.mContext, Settings.Global.getUriFor(this.mSettingName));
        }

        public long getValue(long defaultValue) {
            long identity = Binder.clearCallingIdentity();
            try {
                return Settings.Global.getLong(this.mContext.getContentResolver(), this.mSettingName, defaultValue);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class StringSetCachedGlobalSetting extends ObservingSetting {
        private final Supplier<ArraySet<String>> mBaseValuesSupplier;
        private ArraySet<String> mCachedValue;
        private final Context mContext;
        private final String mSettingName;
        private boolean mValid;

        private StringSetCachedGlobalSetting(Context context, String settingName, Supplier<ArraySet<String>> baseValuesSupplier, Handler handler) {
            super(handler);
            this.mContext = context;
            this.mSettingName = settingName;
            this.mBaseValuesSupplier = baseValuesSupplier;
            this.mValid = false;
        }

        public void register() {
            register(this.mContext, Settings.Global.getUriFor(this.mSettingName));
        }

        public synchronized Set<String> getValue() {
            ArraySet<String> value;
            value = this.mCachedValue;
            if (!this.mValid) {
                long identity = Binder.clearCallingIdentity();
                try {
                    value = new ArraySet<>(this.mBaseValuesSupplier.get());
                    String setting = Settings.Global.getString(this.mContext.getContentResolver(), this.mSettingName);
                    if (!TextUtils.isEmpty(setting)) {
                        try {
                            value.addAll(Arrays.asList(setting.split(",")));
                        } catch (Throwable th) {
                            th = th;
                            Binder.restoreCallingIdentity(identity);
                            throw th;
                        }
                    }
                    Binder.restoreCallingIdentity(identity);
                    if (isRegistered()) {
                        this.mValid = true;
                        this.mCachedValue = value;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
            return value;
        }

        public synchronized void invalidate() {
            this.mValid = false;
            this.mCachedValue = null;
        }

        @Override // com.android.server.location.SettingsHelper.ObservingSetting, android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri, int userId) {
            invalidate();
            super.onChange(selfChange, uri, userId);
        }
    }
}