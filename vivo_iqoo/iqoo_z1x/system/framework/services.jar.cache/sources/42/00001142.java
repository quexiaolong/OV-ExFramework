package com.android.server.location.gnss;

import android.content.Context;
import android.hardware.tv.cec.V1_0.CecMessageType;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.util.FrameworkStatsLog;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import libcore.io.IoUtils;
import vivo.app.VivoFrameworkFactory;
import vivo.app.vgc.AbsVivoVgcManager;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class GnssConfiguration {
    private static final String CONFIG_A_GLONASS_POS_PROTOCOL_SELECT = "A_GLONASS_POS_PROTOCOL_SELECT";
    private static final String CONFIG_C2K_HOST = "C2K_HOST";
    private static final String CONFIG_C2K_PORT = "C2K_PORT";
    private static final String CONFIG_ES_EXTENSION_SEC = "ES_EXTENSION_SEC";
    private static final String CONFIG_GPS_LOCK = "GPS_LOCK";
    private static final String CONFIG_LPP_PROFILE = "LPP_PROFILE";
    public static final String CONFIG_NFW_PROXY_APPS = "NFW_PROXY_APPS";
    private static final String CONFIG_SUPL_ES = "SUPL_ES";
    private static final String CONFIG_SUPL_HOST = "SUPL_HOST";
    private static final String CONFIG_SUPL_MODE = "SUPL_MODE";
    private static final String CONFIG_SUPL_PORT = "SUPL_PORT";
    private static final String CONFIG_SUPL_VER = "SUPL_VER";
    private static final String CONFIG_USE_EMERGENCY_PDN_FOR_EMERGENCY_SUPL = "USE_EMERGENCY_PDN_FOR_EMERGENCY_SUPL";
    private static final String DEBUG_PROPERTIES_FILE = "/etc/gps_debug.conf";
    static final String LPP_PROFILE = "persist.sys.gps.lpp";
    private static final int MAX_EMERGENCY_MODE_EXTENSION_SECONDS = 300;
    private final Context mContext;
    private AbsVivoVgcManager mVivoVgcManager;
    private static final String TAG = "GnssConfiguration";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private int mEsExtensionSec = 0;
    private int mReloadTimes = 0;
    private Properties mProperties = new Properties();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface SetCarrierProperty {
        boolean set(int i);
    }

    private static native HalInterfaceVersion native_get_gnss_configuration_version();

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean native_set_emergency_supl_pdn(int i);

    private static native boolean native_set_es_extension_sec(int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean native_set_gnss_pos_protocol_select(int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean native_set_gps_lock(int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean native_set_lpp_profile(int i);

    private static native boolean native_set_satellite_blacklist(int[] iArr, int[] iArr2);

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean native_set_supl_es(int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean native_set_supl_mode(int i);

    /* JADX INFO: Access modifiers changed from: private */
    public static native boolean native_set_supl_version(int i);

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class HalInterfaceVersion {
        final int mMajor;
        final int mMinor;

        HalInterfaceVersion(int major, int minor) {
            this.mMajor = major;
            this.mMinor = minor;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public GnssConfiguration(Context context) {
        this.mVivoVgcManager = null;
        this.mContext = context;
        if (VivoFrameworkFactory.getFrameworkFactoryImpl() != null) {
            this.mVivoVgcManager = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoVgcManager();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Properties getProperties() {
        return this.mProperties;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getEsExtensionSec() {
        return this.mEsExtensionSec;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getSuplHost() {
        return this.mProperties.getProperty(CONFIG_SUPL_HOST);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getSuplPort(int defaultPort) {
        return getIntConfig(CONFIG_SUPL_PORT, defaultPort);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getC2KHost() {
        return this.mProperties.getProperty(CONFIG_C2K_HOST);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getC2KPort(int defaultPort) {
        return getIntConfig(CONFIG_C2K_PORT, defaultPort);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getSuplMode(int defaultMode) {
        return getIntConfig(CONFIG_SUPL_MODE, defaultMode);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getSuplEs(int defaulSuplEs) {
        return getIntConfig(CONFIG_SUPL_ES, defaulSuplEs);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getLppProfile() {
        return this.mProperties.getProperty(CONFIG_LPP_PROFILE);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<String> getProxyApps() {
        String proxyAppsStr = this.mProperties.getProperty(CONFIG_NFW_PROXY_APPS);
        if (TextUtils.isEmpty(proxyAppsStr)) {
            return Collections.EMPTY_LIST;
        }
        String[] proxyAppsArray = proxyAppsStr.trim().split("\\s+");
        if (proxyAppsArray.length == 0) {
            return Collections.EMPTY_LIST;
        }
        ArrayList proxyApps = new ArrayList(proxyAppsArray.length);
        for (String proxyApp : proxyAppsArray) {
            proxyApps.add(proxyApp);
        }
        return proxyApps;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setSatelliteBlacklist(int[] constellations, int[] svids) {
        native_set_satellite_blacklist(constellations, svids);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public HalInterfaceVersion getHalInterfaceVersion() {
        return native_get_gnss_configuration_version();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:45:0x0150  */
    /* JADX WARN: Removed duplicated region for block: B:65:0x01e3  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public void reloadGpsProperties() {
        /*
            Method dump skipped, instructions count: 495
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.location.gnss.GnssConfiguration.reloadGpsProperties():void");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.location.gnss.GnssConfiguration$3  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass3 extends HashMap<String, SetCarrierProperty> {
        final /* synthetic */ HalInterfaceVersion val$gnssConfigurationIfaceVersion;

        AnonymousClass3(HalInterfaceVersion halInterfaceVersion) {
            this.val$gnssConfigurationIfaceVersion = halInterfaceVersion;
            put(GnssConfiguration.CONFIG_SUPL_VER, $$Lambda$GnssConfiguration$3$lLwCkN9VuTe91diDEr_DrYYmo.INSTANCE);
            put(GnssConfiguration.CONFIG_SUPL_MODE, $$Lambda$GnssConfiguration$3$5KAZXMidKHgmCxmlFmT8sGpFEE.INSTANCE);
            if (GnssConfiguration.isConfigSuplEsSupported(this.val$gnssConfigurationIfaceVersion)) {
                put(GnssConfiguration.CONFIG_SUPL_ES, $$Lambda$GnssConfiguration$3$8rPsr1Qu3pQ4ZuFkOkp6tAmCg.INSTANCE);
            }
            put(GnssConfiguration.CONFIG_LPP_PROFILE, $$Lambda$GnssConfiguration$3$9oYv2aOkEOYfRc6t6z5q1C2SGo.INSTANCE);
            put(GnssConfiguration.CONFIG_A_GLONASS_POS_PROTOCOL_SELECT, $$Lambda$GnssConfiguration$3$asYDZWbJHUv1XJjQXTa7G2qgY.INSTANCE);
            put(GnssConfiguration.CONFIG_USE_EMERGENCY_PDN_FOR_EMERGENCY_SUPL, $$Lambda$GnssConfiguration$3$QHpnPofcCD4Q6pRu_IW9eaP0.INSTANCE);
            if (GnssConfiguration.isConfigGpsLockSupported(this.val$gnssConfigurationIfaceVersion)) {
                put(GnssConfiguration.CONFIG_GPS_LOCK, $$Lambda$GnssConfiguration$3$coaX28wgwR1rFNOMom2UbsZv5jM.INSTANCE);
            }
        }
    }

    private void logConfigurations() {
        FrameworkStatsLog.write((int) CecMessageType.REPORT_PHYSICAL_ADDRESS, getSuplHost(), getSuplPort(0), getC2KHost(), getC2KPort(0), getIntConfig(CONFIG_SUPL_VER, 0), getSuplMode(0), getSuplEs(0) == 1, getIntConfig(CONFIG_LPP_PROFILE, 0), getIntConfig(CONFIG_A_GLONASS_POS_PROTOCOL_SELECT, 0), getIntConfig(CONFIG_USE_EMERGENCY_PDN_FOR_EMERGENCY_SUPL, 0) == 1, getIntConfig(CONFIG_GPS_LOCK, 0), getEsExtensionSec(), this.mProperties.getProperty(CONFIG_NFW_PROXY_APPS));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void loadPropertiesFromCarrierConfig() {
        CarrierConfigManager configManager = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
        if (configManager == null) {
            return;
        }
        int ddSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        PersistableBundle configs = SubscriptionManager.isValidSubscriptionId(ddSubId) ? configManager.getConfigForSubId(ddSubId) : null;
        if (configs == null) {
            if (DEBUG) {
                Log.d(TAG, "SIM not ready, use default carrier config.");
            }
            configs = CarrierConfigManager.getDefaultConfig();
        }
        for (String configKey : configs.keySet()) {
            if (configKey.startsWith("gps.")) {
                String key = configKey.substring("gps.".length()).toUpperCase();
                Object value = configs.get(configKey);
                if (DEBUG) {
                    Log.d(TAG, "Gps config: " + key + " = " + value);
                }
                if (value instanceof String) {
                    this.mProperties.setProperty(key, (String) value);
                } else if (value != null) {
                    this.mProperties.setProperty(key, value.toString());
                }
            }
        }
    }

    private void loadPropertiesFromGpsDebugConfig(Properties properties) {
        try {
            File file = new File(DEBUG_PROPERTIES_FILE);
            FileInputStream stream = new FileInputStream(file);
            properties.load(stream);
            IoUtils.closeQuietly(stream);
        } catch (IOException e) {
            if (DEBUG) {
                Log.d(TAG, "Could not open GPS configuration file /etc/gps_debug.conf");
            }
        }
    }

    private int getRangeCheckedConfigEsExtensionSec() {
        int emergencyExtensionSeconds = getIntConfig(CONFIG_ES_EXTENSION_SEC, 0);
        if (emergencyExtensionSeconds > 300) {
            Log.w(TAG, "ES_EXTENSION_SEC: " + emergencyExtensionSeconds + " too high, reset to 300");
            return 300;
        } else if (emergencyExtensionSeconds < 0) {
            Log.w(TAG, "ES_EXTENSION_SEC: " + emergencyExtensionSeconds + " is negative, reset to zero.");
            return 0;
        } else {
            return emergencyExtensionSeconds;
        }
    }

    private int getIntConfig(String configParameter, int defaultValue) {
        String valueString = this.mProperties.getProperty(configParameter);
        if (TextUtils.isEmpty(valueString)) {
            return defaultValue;
        }
        try {
            return Integer.decode(valueString).intValue();
        } catch (NumberFormatException e) {
            Log.e(TAG, "Unable to parse config parameter " + configParameter + " value: " + valueString + ". Using default value: " + defaultValue);
            return defaultValue;
        }
    }

    private static boolean isConfigEsExtensionSecSupported(HalInterfaceVersion gnssConfiguartionIfaceVersion) {
        return gnssConfiguartionIfaceVersion.mMajor >= 2;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isConfigSuplEsSupported(HalInterfaceVersion gnssConfiguartionIfaceVersion) {
        return gnssConfiguartionIfaceVersion.mMajor < 2;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean isConfigGpsLockSupported(HalInterfaceVersion gnssConfiguartionIfaceVersion) {
        return gnssConfiguartionIfaceVersion.mMajor < 2;
    }
}