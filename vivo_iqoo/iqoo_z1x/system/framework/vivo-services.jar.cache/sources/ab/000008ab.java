package com.vivo.services.vgc;

import android.content.ContentValues;
import android.util.ArrayMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/* loaded from: classes.dex */
public class VgcConfigOverlayHelper {
    public static boolean isConfigRepeated(Map<String, String> map, String name) {
        if (map == null || name == null || !map.containsKey(name)) {
            return false;
        }
        return true;
    }

    public static boolean isPathRepeated(Map<String, String> map, String name) {
        if (map == null || name == null || !map.containsKey(name)) {
            return false;
        }
        return true;
    }

    public static boolean isStringListRepeated(ArrayMap<String, ArrayList<String>> list, String name) {
        if (list == null || name == null) {
            return false;
        }
        Set<Map.Entry<String, ArrayList<String>>> set = list.entrySet();
        for (Map.Entry<String, ArrayList<String>> entry : set) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isContentValuesListRepeated(ArrayMap<String, ContentValues> list, String name) {
        if (list == null || name == null) {
            return false;
        }
        Set<Map.Entry<String, ContentValues>> set = list.entrySet();
        for (Map.Entry<String, ContentValues> entry : set) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
}