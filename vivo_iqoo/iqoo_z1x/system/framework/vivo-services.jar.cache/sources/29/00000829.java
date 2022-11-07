package com.vivo.services.security.server;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.net.Uri;
import android.util.ArrayMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public final class VivoPermissionUtils {
    public static final float DEFAULT_MAX_LABEL_SIZE_PX = 500.0f;
    public static final int FLAGS_ALWAYS_USER_SENSITIVE = 768;
    public static final int FLAGS_PERMISSION_WHITELIST_ALL = 7;
    private static final Intent LAUNCHER_INTENT = new Intent("android.intent.action.MAIN", (Uri) null).addCategory("android.intent.category.LAUNCHER");
    private static final String LOG_TAG = "VivoPermissionUtils";
    public static final String OS_PKG = "android";
    private static final ArrayMap<String, String> PLATFORM_PERMISSIONS;
    private static final ArrayMap<String, ArrayList<String>> PLATFORM_PERMISSION_GROUPS;
    private static final String PROPERTY_LOCATION_ACCESS_CHECK_ENABLED = "location_access_check_enabled";

    static {
        ArrayMap<String, String> arrayMap = new ArrayMap<>();
        PLATFORM_PERMISSIONS = arrayMap;
        arrayMap.put("android.permission.READ_CONTACTS", "android.permission-group.CONTACTS");
        PLATFORM_PERMISSIONS.put("android.permission.WRITE_CONTACTS", "android.permission-group.CONTACTS");
        PLATFORM_PERMISSIONS.put("android.permission.GET_ACCOUNTS", "android.permission-group.CONTACTS");
        PLATFORM_PERMISSIONS.put("android.permission.READ_CALENDAR", "android.permission-group.CALENDAR");
        PLATFORM_PERMISSIONS.put("android.permission.WRITE_CALENDAR", "android.permission-group.CALENDAR");
        PLATFORM_PERMISSIONS.put("android.permission.SEND_SMS", "android.permission-group.SMS");
        PLATFORM_PERMISSIONS.put("android.permission.RECEIVE_SMS", "android.permission-group.SMS");
        PLATFORM_PERMISSIONS.put("android.permission.READ_SMS", "android.permission-group.SMS");
        PLATFORM_PERMISSIONS.put("android.permission.RECEIVE_MMS", "android.permission-group.SMS");
        PLATFORM_PERMISSIONS.put("android.permission.RECEIVE_WAP_PUSH", "android.permission-group.SMS");
        PLATFORM_PERMISSIONS.put("android.permission.READ_CELL_BROADCASTS", "android.permission-group.SMS");
        PLATFORM_PERMISSIONS.put("android.permission.READ_EXTERNAL_STORAGE", "android.permission-group.STORAGE");
        PLATFORM_PERMISSIONS.put("android.permission.WRITE_EXTERNAL_STORAGE", "android.permission-group.STORAGE");
        PLATFORM_PERMISSIONS.put("android.permission.ACCESS_MEDIA_LOCATION", "android.permission-group.STORAGE");
        PLATFORM_PERMISSIONS.put("android.permission.ACCESS_FINE_LOCATION", "android.permission-group.LOCATION");
        PLATFORM_PERMISSIONS.put("android.permission.ACCESS_COARSE_LOCATION", "android.permission-group.LOCATION");
        PLATFORM_PERMISSIONS.put("android.permission.ACCESS_BACKGROUND_LOCATION", "android.permission-group.LOCATION");
        PLATFORM_PERMISSIONS.put("android.permission.READ_CALL_LOG", "android.permission-group.CALL_LOG");
        PLATFORM_PERMISSIONS.put("android.permission.WRITE_CALL_LOG", "android.permission-group.CALL_LOG");
        PLATFORM_PERMISSIONS.put("android.permission.PROCESS_OUTGOING_CALLS", "android.permission-group.CALL_LOG");
        PLATFORM_PERMISSIONS.put("android.permission.READ_PHONE_STATE", "android.permission-group.PHONE");
        PLATFORM_PERMISSIONS.put("android.permission.READ_PHONE_NUMBERS", "android.permission-group.PHONE");
        PLATFORM_PERMISSIONS.put("android.permission.CALL_PHONE", "android.permission-group.PHONE");
        PLATFORM_PERMISSIONS.put("com.android.voicemail.permission.ADD_VOICEMAIL", "android.permission-group.PHONE");
        PLATFORM_PERMISSIONS.put("android.permission.USE_SIP", "android.permission-group.PHONE");
        PLATFORM_PERMISSIONS.put("android.permission.ANSWER_PHONE_CALLS", "android.permission-group.PHONE");
        PLATFORM_PERMISSIONS.put("android.permission.ACCEPT_HANDOVER", "android.permission-group.PHONE");
        PLATFORM_PERMISSIONS.put("android.permission.RECORD_AUDIO", "android.permission-group.MICROPHONE");
        PLATFORM_PERMISSIONS.put("android.permission.ACTIVITY_RECOGNITION", "android.permission-group.ACTIVITY_RECOGNITION");
        PLATFORM_PERMISSIONS.put("android.permission.CAMERA", "android.permission-group.CAMERA");
        PLATFORM_PERMISSIONS.put("android.permission.BODY_SENSORS", "android.permission-group.SENSORS");
        PLATFORM_PERMISSION_GROUPS = new ArrayMap<>();
        int numPlatformPermissions = PLATFORM_PERMISSIONS.size();
        for (int i = 0; i < numPlatformPermissions; i++) {
            String permission = PLATFORM_PERMISSIONS.keyAt(i);
            String permissionGroup = PLATFORM_PERMISSIONS.valueAt(i);
            ArrayList<String> permissionsOfThisGroup = PLATFORM_PERMISSION_GROUPS.get(permissionGroup);
            if (permissionsOfThisGroup == null) {
                permissionsOfThisGroup = new ArrayList<>();
                PLATFORM_PERMISSION_GROUPS.put(permissionGroup, permissionsOfThisGroup);
            }
            permissionsOfThisGroup.add(permission);
        }
    }

    private VivoPermissionUtils() {
    }

    public static String getGroupOfPlatformPermission(String permission) {
        return PLATFORM_PERMISSIONS.get(permission);
    }

    public static String getGroupOfPermission(PermissionInfo permission) {
        String groupName = getGroupOfPlatformPermission(permission.name);
        if (groupName == null) {
            return permission.group;
        }
        return groupName;
    }

    public static List<String> getPlatformPermissionNamesOfGroup(String group) {
        ArrayList<String> permissions = PLATFORM_PERMISSION_GROUPS.get(group);
        return permissions != null ? permissions : Collections.emptyList();
    }

    public static List<PermissionInfo> getPlatformPermissionsOfGroup(PackageManager pm, String group) {
        ArrayList<PermissionInfo> permInfos = new ArrayList<>();
        ArrayList<String> permissions = PLATFORM_PERMISSION_GROUPS.get(group);
        if (permissions == null) {
            return Collections.emptyList();
        }
        int numPermissions = permissions.size();
        for (int i = 0; i < numPermissions; i++) {
            String permName = permissions.get(i);
            try {
                PermissionInfo permInfo = pm.getPermissionInfo(permName, 0);
                permInfos.add(permInfo);
            } catch (PackageManager.NameNotFoundException e) {
                VSlog.e(LOG_TAG, permName + " not defined by platform", e);
            }
        }
        return permInfos;
    }

    public static boolean isBackgroundPermission(PackageManager pm, String perm) {
        String group;
        if (pm == null || perm == null || (group = getGroupOfPlatformPermission(perm)) == null) {
            return false;
        }
        try {
            List<PermissionInfo> permissionInfos = pm.queryPermissionsByGroup(group, 0);
            permissionInfos.addAll(getPlatformPermissionsOfGroup(pm, group));
            if (permissionInfos != null && permissionInfos.size() > 0) {
                for (PermissionInfo permInfo : permissionInfos) {
                    if (permInfo != null && permInfo.backgroundPermission != null && permInfo.backgroundPermission.equals(perm)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public static List<PermissionInfo> getPermissionInfosForGroup(PackageManager pm, String group) throws PackageManager.NameNotFoundException {
        List<PermissionInfo> permissions = pm.queryPermissionsByGroup(group, 0);
        permissions.addAll(getPlatformPermissionsOfGroup(pm, group));
        return permissions;
    }

    public static PackageItemInfo getGroupInfo(String groupName, Context context) {
        try {
            return context.getPackageManager().getPermissionGroupInfo(groupName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            try {
                return context.getPackageManager().getPermissionInfo(groupName, 0);
            } catch (PackageManager.NameNotFoundException e2) {
                return null;
            }
        }
    }

    public static List<PermissionInfo> getGroupPermissionInfos(String groupName, Context context) {
        try {
            return getPermissionInfosForGroup(context.getPackageManager(), groupName);
        } catch (PackageManager.NameNotFoundException e) {
            try {
                PermissionInfo permissionInfo = context.getPackageManager().getPermissionInfo(groupName, 0);
                List<PermissionInfo> permissions = new ArrayList<>();
                permissions.add(permissionInfo);
                return permissions;
            } catch (PackageManager.NameNotFoundException e2) {
                return null;
            }
        }
    }

    public static boolean isModernPermissionGroup(String name) {
        return PLATFORM_PERMISSION_GROUPS.containsKey(name);
    }

    public static List<String> getPlatformPermissionGroups() {
        return new ArrayList(PLATFORM_PERMISSION_GROUPS.keySet());
    }

    public static Set<String> getPlatformPermissions() {
        return PLATFORM_PERMISSIONS.keySet();
    }
}