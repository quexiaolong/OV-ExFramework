package com.android.server.stats.pull;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Slog;
import android.util.StatsEvent;
import com.android.server.notification.SnoozeHelper;
import com.android.service.nano.StringListParamProto;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes2.dex */
final class SettingsStatsUtil {
    private static final FlagsData[] GLOBAL_SETTINGS = {new FlagsData("GlobalFeature__boolean_whitelist", 1), new FlagsData("GlobalFeature__integer_whitelist", 2), new FlagsData("GlobalFeature__float_whitelist", 3), new FlagsData("GlobalFeature__string_whitelist", 4)};
    private static final FlagsData[] SECURE_SETTINGS = {new FlagsData("SecureFeature__boolean_whitelist", 1), new FlagsData("SecureFeature__integer_whitelist", 2), new FlagsData("SecureFeature__float_whitelist", 3), new FlagsData("SecureFeature__string_whitelist", 4)};
    private static final FlagsData[] SYSTEM_SETTINGS = {new FlagsData("SystemFeature__boolean_whitelist", 1), new FlagsData("SystemFeature__integer_whitelist", 2), new FlagsData("SystemFeature__float_whitelist", 3), new FlagsData("SystemFeature__string_whitelist", 4)};
    private static final String TAG = "SettingsStatsUtil";

    SettingsStatsUtil() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static List<StatsEvent> logGlobalSettings(Context context, int atomTag, int userId) {
        FlagsData[] flagsDataArr;
        String[] strArr;
        List<StatsEvent> output = new ArrayList<>();
        ContentResolver resolver = context.getContentResolver();
        for (FlagsData flagsData : GLOBAL_SETTINGS) {
            StringListParamProto proto = getList(flagsData.mFlagName);
            if (proto != null) {
                for (String key : proto.element) {
                    String value = Settings.Global.getStringForUser(resolver, key, userId);
                    output.add(createStatsEvent(atomTag, key, value, userId, flagsData.mDataType));
                }
            }
        }
        return output;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static List<StatsEvent> logSystemSettings(Context context, int atomTag, int userId) {
        FlagsData[] flagsDataArr;
        String[] strArr;
        List<StatsEvent> output = new ArrayList<>();
        ContentResolver resolver = context.getContentResolver();
        for (FlagsData flagsData : SYSTEM_SETTINGS) {
            StringListParamProto proto = getList(flagsData.mFlagName);
            if (proto != null) {
                for (String key : proto.element) {
                    String value = Settings.System.getStringForUser(resolver, key, userId);
                    output.add(createStatsEvent(atomTag, key, value, userId, flagsData.mDataType));
                }
            }
        }
        return output;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static List<StatsEvent> logSecureSettings(Context context, int atomTag, int userId) {
        FlagsData[] flagsDataArr;
        String[] strArr;
        List<StatsEvent> output = new ArrayList<>();
        ContentResolver resolver = context.getContentResolver();
        for (FlagsData flagsData : SECURE_SETTINGS) {
            StringListParamProto proto = getList(flagsData.mFlagName);
            if (proto != null) {
                for (String key : proto.element) {
                    String value = Settings.Secure.getStringForUser(resolver, key, userId);
                    output.add(createStatsEvent(atomTag, key, value, userId, flagsData.mDataType));
                }
            }
        }
        return output;
    }

    static StringListParamProto getList(String flag) {
        String base64 = DeviceConfig.getProperty("settings_stats", flag);
        if (TextUtils.isEmpty(base64)) {
            return null;
        }
        byte[] decode = Base64.decode(base64, 3);
        try {
            StringListParamProto list = StringListParamProto.parseFrom(decode);
            return list;
        } catch (Exception e) {
            Slog.e(TAG, "Error parsing string list proto", e);
            return null;
        }
    }

    private static StatsEvent createStatsEvent(int atomTag, String key, String value, int userId, int type) {
        StatsEvent.Builder builder = StatsEvent.newBuilder().setAtomId(atomTag).writeString(key);
        boolean booleanValue = false;
        int intValue = 0;
        float floatValue = 0.0f;
        String stringValue = "";
        if (TextUtils.isEmpty(value)) {
            builder.writeInt(0).writeBoolean(false).writeInt(0).writeFloat(0.0f).writeString("").writeInt(userId);
        } else {
            if (type == 1) {
                booleanValue = SnoozeHelper.XML_SNOOZED_NOTIFICATION_VERSION.equals(value);
            } else if (type == 2) {
                try {
                    intValue = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    Slog.w(TAG, "Can not parse value to float: " + value);
                }
            } else if (type == 3) {
                try {
                    floatValue = Float.parseFloat(value);
                } catch (NumberFormatException e2) {
                    Slog.w(TAG, "Can not parse value to float: " + value);
                }
            } else if (type == 4) {
                stringValue = value;
            } else {
                Slog.w(TAG, "Unexpected value type " + type);
            }
            builder.writeInt(type).writeBoolean(booleanValue).writeInt(intValue).writeFloat(floatValue).writeString(stringValue).writeInt(userId);
        }
        return builder.build();
    }

    /* loaded from: classes2.dex */
    static final class FlagsData {
        int mDataType;
        String mFlagName;

        FlagsData(String flagName, int dataType) {
            this.mFlagName = flagName;
            this.mDataType = dataType;
        }
    }
}