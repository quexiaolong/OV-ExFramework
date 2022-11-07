package com.android.server.gpu;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.gamedriver.GameDriverProto;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;
import com.android.framework.protobuf.InvalidProtocolBufferException;
import com.android.server.SystemService;
import com.android.server.pm.Settings;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class GpuService extends SystemService {
    private static final int BASE64_FLAGS = 3;
    public static final boolean DEBUG = false;
    private static final String DEV_DRIVER_PROPERTY = "ro.gfx.driver.1";
    private static final String GAME_DRIVER_WHITELIST_FILENAME = "whitelist.txt";
    private static final String PROD_DRIVER_PROPERTY = "ro.gfx.driver.0";
    public static final String TAG = "GpuService";
    private GameDriverProto.Blacklists mBlacklists;
    private ContentResolver mContentResolver;
    private final Context mContext;
    private final String mDevDriverPackageName;
    private DeviceConfigListener mDeviceConfigListener;
    private final Object mDeviceConfigLock;
    private long mGameDriverVersionCode;
    private final boolean mHasDevDriver;
    private final boolean mHasProdDriver;
    private final Object mLock;
    private final PackageManager mPackageManager;
    private final String mProdDriverPackageName;
    private SettingsObserver mSettingsObserver;

    private static native void nSetUpdatableDriverPath(String str);

    public GpuService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mDeviceConfigLock = new Object();
        this.mContext = context;
        this.mProdDriverPackageName = SystemProperties.get(PROD_DRIVER_PROPERTY);
        this.mGameDriverVersionCode = -1L;
        this.mDevDriverPackageName = SystemProperties.get(DEV_DRIVER_PROPERTY);
        this.mPackageManager = context.getPackageManager();
        this.mHasProdDriver = !TextUtils.isEmpty(this.mProdDriverPackageName);
        boolean z = !TextUtils.isEmpty(this.mDevDriverPackageName);
        this.mHasDevDriver = z;
        if (z || this.mHasProdDriver) {
            IntentFilter packageFilter = new IntentFilter();
            packageFilter.addAction("android.intent.action.PACKAGE_ADDED");
            packageFilter.addAction("android.intent.action.PACKAGE_CHANGED");
            packageFilter.addAction("android.intent.action.PACKAGE_REMOVED");
            packageFilter.addDataScheme(Settings.ATTR_PACKAGE);
            getContext().registerReceiverAsUser(new PackageReceiver(), UserHandle.ALL, packageFilter, null, null);
        }
    }

    @Override // com.android.server.SystemService
    public void onStart() {
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 1000) {
            this.mContentResolver = this.mContext.getContentResolver();
            if (!this.mHasProdDriver && !this.mHasDevDriver) {
                return;
            }
            this.mSettingsObserver = new SettingsObserver();
            this.mDeviceConfigListener = new DeviceConfigListener();
            fetchGameDriverPackageProperties();
            processBlacklists();
            setBlacklist();
            fetchDeveloperDriverPackageProperties();
        }
    }

    /* loaded from: classes.dex */
    private final class SettingsObserver extends ContentObserver {
        private final Uri mGameDriverBlackUri;

        SettingsObserver() {
            super(new Handler());
            this.mGameDriverBlackUri = Settings.Global.getUriFor("game_driver_blacklists");
            GpuService.this.mContentResolver.registerContentObserver(this.mGameDriverBlackUri, false, this, -1);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (uri != null && this.mGameDriverBlackUri.equals(uri)) {
                GpuService.this.processBlacklists();
                GpuService.this.setBlacklist();
            }
        }
    }

    /* loaded from: classes.dex */
    private final class DeviceConfigListener implements DeviceConfig.OnPropertiesChangedListener {
        DeviceConfigListener() {
            DeviceConfig.addOnPropertiesChangedListener("game_driver", GpuService.this.mContext.getMainExecutor(), this);
        }

        public void onPropertiesChanged(DeviceConfig.Properties properties) {
            synchronized (GpuService.this.mDeviceConfigLock) {
                if (properties.getKeyset().contains("game_driver_blacklists")) {
                    GpuService.this.parseBlacklists(properties.getString("game_driver_blacklists", ""));
                    GpuService.this.setBlacklist();
                }
            }
        }
    }

    /* loaded from: classes.dex */
    private final class PackageReceiver extends BroadcastReceiver {
        private PackageReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            Uri data = intent.getData();
            String packageName = data.getSchemeSpecificPart();
            boolean isProdDriver = packageName.equals(GpuService.this.mProdDriverPackageName);
            boolean isDevDriver = packageName.equals(GpuService.this.mDevDriverPackageName);
            if (!isProdDriver && !isDevDriver) {
                return;
            }
            String action = intent.getAction();
            char c = 65535;
            int hashCode = action.hashCode();
            if (hashCode != 172491798) {
                if (hashCode != 525384130) {
                    if (hashCode == 1544582882 && action.equals("android.intent.action.PACKAGE_ADDED")) {
                        c = 0;
                    }
                } else if (action.equals("android.intent.action.PACKAGE_REMOVED")) {
                    c = 2;
                }
            } else if (action.equals("android.intent.action.PACKAGE_CHANGED")) {
                c = 1;
            }
            if (c == 0 || c == 1 || c == 2) {
                if (isProdDriver) {
                    GpuService.this.fetchGameDriverPackageProperties();
                    GpuService.this.setBlacklist();
                } else if (isDevDriver) {
                    GpuService.this.fetchDeveloperDriverPackageProperties();
                }
            }
        }
    }

    private static void assetToSettingsGlobal(Context context, Context driverContext, String fileName, String settingsGlobal, CharSequence delimiter) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(driverContext.getAssets().open(fileName)));
            ArrayList<String> assetStrings = new ArrayList<>();
            while (true) {
                String assetString = reader.readLine();
                if (assetString != null) {
                    assetStrings.add(assetString);
                } else {
                    Settings.Global.putString(context.getContentResolver(), settingsGlobal, String.join(delimiter, assetStrings));
                    return;
                }
            }
        } catch (IOException e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void fetchGameDriverPackageProperties() {
        try {
            ApplicationInfo driverInfo = this.mPackageManager.getApplicationInfo(this.mProdDriverPackageName, 1048576);
            if (driverInfo.targetSdkVersion < 26) {
                return;
            }
            Settings.Global.putString(this.mContentResolver, "game_driver_whitelist", "");
            this.mGameDriverVersionCode = driverInfo.longVersionCode;
            try {
                Context driverContext = this.mContext.createPackageContext(this.mProdDriverPackageName, 4);
                assetToSettingsGlobal(this.mContext, driverContext, GAME_DRIVER_WHITELIST_FILENAME, "game_driver_whitelist", ",");
            } catch (PackageManager.NameNotFoundException e) {
            }
        } catch (PackageManager.NameNotFoundException e2) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void processBlacklists() {
        String base64String = DeviceConfig.getProperty("game_driver", "game_driver_blacklists");
        if (base64String == null) {
            base64String = Settings.Global.getString(this.mContentResolver, "game_driver_blacklists");
        }
        parseBlacklists(base64String != null ? base64String : "");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void parseBlacklists(String base64String) {
        synchronized (this.mLock) {
            this.mBlacklists = null;
            try {
                this.mBlacklists = GameDriverProto.Blacklists.parseFrom(Base64.decode(base64String, 3));
            } catch (IllegalArgumentException e) {
            } catch (InvalidProtocolBufferException e2) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setBlacklist() {
        Settings.Global.putString(this.mContentResolver, "game_driver_blacklist", "");
        synchronized (this.mLock) {
            if (this.mBlacklists == null) {
                return;
            }
            List<GameDriverProto.Blacklist> blacklists = this.mBlacklists.getBlacklistsList();
            for (GameDriverProto.Blacklist blacklist : blacklists) {
                if (blacklist.getVersionCode() == this.mGameDriverVersionCode) {
                    Settings.Global.putString(this.mContentResolver, "game_driver_blacklist", String.join(",", blacklist.getPackageNamesList()));
                    return;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void fetchDeveloperDriverPackageProperties() {
        try {
            ApplicationInfo driverInfo = this.mPackageManager.getApplicationInfo(this.mDevDriverPackageName, 1048576);
            if (driverInfo.targetSdkVersion < 26) {
                return;
            }
            setUpdatableDriverPath(driverInfo);
        } catch (PackageManager.NameNotFoundException e) {
        }
    }

    private void setUpdatableDriverPath(ApplicationInfo ai) {
        if (ai.primaryCpuAbi == null) {
            nSetUpdatableDriverPath("");
            return;
        }
        nSetUpdatableDriverPath(ai.sourceDir + "!/lib/");
    }
}