package com.android.server.am;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.SystemProperties;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.slice.SliceClientPermissions;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

/* loaded from: classes.dex */
public class SettingsToPropertiesMapper {
    private static final String GLOBAL_SETTINGS_CATEGORY = "global_settings";
    private static final String RESET_PERFORMED_PROPERTY = "device_config.reset_performed";
    private static final String RESET_RECORD_FILE_PATH = "/data/server_configurable_flags/reset_flags";
    private static final String SYSTEM_PROPERTY_INVALID_SUBSTRING = "..";
    private static final int SYSTEM_PROPERTY_MAX_LENGTH = 92;
    private static final String SYSTEM_PROPERTY_PREFIX = "persist.device_config.";
    private static final String SYSTEM_PROPERTY_VALID_CHARACTERS_REGEX = "^[\\w\\.\\-@:]*$";
    private static final String TAG = "SettingsToPropertiesMapper";
    private final ContentResolver mContentResolver;
    private final String[] mDeviceConfigScopes;
    private final String[] mGlobalSettings;
    static final String[] sGlobalSettings = {"native_flags_health_check_enabled"};
    static final String[] sDeviceConfigScopes = {"activity_manager_native_boot", "configuration", "input_native_boot", "intelligence_content_suggestions", "media_native", "netd_native", "runtime_native", "runtime_native_boot", "storage_native_boot", "window_manager_native_boot"};

    protected SettingsToPropertiesMapper(ContentResolver contentResolver, String[] globalSettings, String[] deviceConfigScopes) {
        this.mContentResolver = contentResolver;
        this.mGlobalSettings = globalSettings;
        this.mDeviceConfigScopes = deviceConfigScopes;
    }

    void updatePropertiesFromSettings() {
        String[] strArr;
        String[] strArr2;
        for (final String globalSetting : this.mGlobalSettings) {
            Uri settingUri = Settings.Global.getUriFor(globalSetting);
            final String propName = makePropertyName(GLOBAL_SETTINGS_CATEGORY, globalSetting);
            if (settingUri == null) {
                log("setting uri is null for globalSetting " + globalSetting);
            } else if (propName == null) {
                log("invalid prop name for globalSetting " + globalSetting);
            } else {
                ContentObserver co = new ContentObserver(null) { // from class: com.android.server.am.SettingsToPropertiesMapper.1
                    @Override // android.database.ContentObserver
                    public void onChange(boolean selfChange) {
                        SettingsToPropertiesMapper.this.updatePropertyFromSetting(globalSetting, propName);
                    }
                };
                if (!isNativeFlagsResetPerformed()) {
                    updatePropertyFromSetting(globalSetting, propName);
                }
                this.mContentResolver.registerContentObserver(settingUri, false, co);
            }
        }
        for (String deviceConfigScope : this.mDeviceConfigScopes) {
            DeviceConfig.addOnPropertiesChangedListener(deviceConfigScope, AsyncTask.THREAD_POOL_EXECUTOR, new DeviceConfig.OnPropertiesChangedListener() { // from class: com.android.server.am.-$$Lambda$SettingsToPropertiesMapper$oP9A7vTPRZcZgLdy43KKEveF4zQ
                public final void onPropertiesChanged(DeviceConfig.Properties properties) {
                    SettingsToPropertiesMapper.this.lambda$updatePropertiesFromSettings$0$SettingsToPropertiesMapper(properties);
                }
            });
        }
    }

    public /* synthetic */ void lambda$updatePropertiesFromSettings$0$SettingsToPropertiesMapper(DeviceConfig.Properties properties) {
        String scope = properties.getNamespace();
        for (String key : properties.getKeyset()) {
            String propertyName = makePropertyName(scope, key);
            if (propertyName == null) {
                log("unable to construct system property for " + scope + SliceClientPermissions.SliceAuthority.DELIMITER + key);
                return;
            }
            setProperty(propertyName, properties.getString(key, (String) null));
        }
    }

    public static SettingsToPropertiesMapper start(ContentResolver contentResolver) {
        SettingsToPropertiesMapper mapper = new SettingsToPropertiesMapper(contentResolver, sGlobalSettings, sDeviceConfigScopes);
        mapper.updatePropertiesFromSettings();
        return mapper;
    }

    public static boolean isNativeFlagsResetPerformed() {
        String value = SystemProperties.get(RESET_PERFORMED_PROPERTY);
        return "true".equals(value);
    }

    public static String[] getResetNativeCategories() {
        if (!isNativeFlagsResetPerformed()) {
            return new String[0];
        }
        String content2 = getResetFlagsFileContent();
        if (TextUtils.isEmpty(content2)) {
            return new String[0];
        }
        String[] property_names = content2.split(";");
        HashSet<String> categories = new HashSet<>();
        for (String property_name : property_names) {
            String[] segments = property_name.split("\\.");
            if (segments.length < 3) {
                log("failed to extract category name from property " + property_name);
            } else {
                categories.add(segments[2]);
            }
        }
        return (String[]) categories.toArray(new String[0]);
    }

    static String makePropertyName(String categoryName, String flagName) {
        String propertyName = SYSTEM_PROPERTY_PREFIX + categoryName + "." + flagName;
        if (!propertyName.matches(SYSTEM_PROPERTY_VALID_CHARACTERS_REGEX) || propertyName.contains(SYSTEM_PROPERTY_INVALID_SUBSTRING)) {
            return null;
        }
        return propertyName;
    }

    private void setProperty(String key, String value) {
        if (value == null) {
            if (TextUtils.isEmpty(SystemProperties.get(key))) {
                return;
            }
            value = "";
        } else if (value.length() > SYSTEM_PROPERTY_MAX_LENGTH) {
            log(value + " exceeds system property max length.");
            return;
        }
        try {
            SystemProperties.set(key, value);
        } catch (Exception e) {
            log("Unable to set property " + key + " value '" + value + "'", e);
        }
    }

    private static void log(String msg, Exception e) {
        if (Build.IS_DEBUGGABLE) {
            Slog.wtf(TAG, msg, e);
        } else {
            Slog.e(TAG, msg, e);
        }
    }

    private static void log(String msg) {
        if (Build.IS_DEBUGGABLE) {
            Slog.wtf(TAG, msg);
        } else {
            Slog.e(TAG, msg);
        }
    }

    static String getResetFlagsFileContent() {
        String content2 = null;
        try {
            File reset_flag_file = new File(RESET_RECORD_FILE_PATH);
            BufferedReader br = new BufferedReader(new FileReader(reset_flag_file));
            content2 = br.readLine();
            br.close();
            return content2;
        } catch (IOException ioe) {
            log("failed to read file /data/server_configurable_flags/reset_flags", ioe);
            return content2;
        }
    }

    void updatePropertyFromSetting(String settingName, String propName) {
        String settingValue = Settings.Global.getString(this.mContentResolver, settingName);
        setProperty(propName, settingValue);
    }
}