package com.android.server.devicepolicy.utils;

import android.content.Context;
import android.util.Log;
import com.vivo.face.common.data.Constants;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/* loaded from: classes.dex */
public final class VivoUtils {
    public static final int MAX_LIST_SIZE = 10000;
    public static final String VIVO_CUSTOM_JSON = "emm_list.json";
    public static final String VIVO_CUSTOM_PATH = "/data/custom/";
    public static final String VIVO_CUSTOM_XML = "custom_config.xml";
    public static final String VIVO_EMM_INFO_XML = "vivo_emm_info.xml";
    public static final String VIVO_POLICIES_PATH = "/data/system/";
    public static final String VIVO_POLICIES_XML = "vivo_policies.xml";
    private static Context mContext = null;

    public static boolean init(Context context) {
        if (context == null) {
            Log.e("VivoUtils", "init context is null");
            return false;
        }
        mContext = context;
        return true;
    }

    public static boolean isOverLimit(List<String> list, List<String> list2) {
        if (list == null || list2 == null || list.size() + list2.size() <= 10000) {
            return false;
        }
        Log.e("VivoUtils", "The data is over limit: 10000");
        return true;
    }

    public static boolean addItemsFromList(List<String> list, List<String> list2, boolean isAdd) {
        if (list == null || list2 == null) {
            return false;
        }
        if (isAdd) {
            if (isOverLimit(list, list2)) {
                return false;
            }
            for (String string : list2) {
                if (!list.contains(string) && string != null && !string.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK) && !string.trim().isEmpty()) {
                    list.add(string);
                }
            }
            return true;
        }
        HashSet<String> hashSet = new HashSet<>(list2);
        List<String> list22 = new ArrayList<>();
        for (String string2 : list) {
            if (!hashSet.contains(string2)) {
                list22.add(string2);
            }
        }
        list.clear();
        list.addAll(list22);
        return true;
    }

    public static boolean addItemsFromPhoneNumList(List<String> list, List<String> list2, boolean isAdd) {
        if (list == null || list2 == null) {
            return false;
        }
        if (isAdd) {
            if (isOverLimit(list, list2)) {
                return false;
            }
            if (list.size() != 0) {
                List<String> temp = new ArrayList<>(list);
                for (String s2 : list2) {
                    if (!list.contains(s2) && s2 != null && !s2.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK) && !s2.trim().isEmpty()) {
                        try {
                            String[] info2 = s2.split(":");
                            for (String s : temp) {
                                String[] info = s.split(":");
                                if (info[0].equals(info2[0]) && (info[1].equals(info2[1]) || (3 == Integer.parseInt(info2[1]) && (info[2].equals(info2[2]) || 48 == Integer.parseInt(info2[2]))))) {
                                    list.remove(s);
                                }
                            }
                            list.add(s2);
                        } catch (Exception e) {
                            Log.e("VivoUtils", "addItemsFromPhoneNumList error item " + s2);
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                for (String s22 : list2) {
                    if (!list.contains(s22) && s22 != null && !s22.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK) && !s22.trim().isEmpty()) {
                        list.add(s22);
                    }
                }
            }
        } else {
            HashSet<String> hashSet = new HashSet<>(list2);
            List<String> list22 = new ArrayList<>();
            for (String string : list) {
                if (!hashSet.contains(string)) {
                    list22.add(string);
                }
            }
            list.clear();
            list.addAll(list22);
        }
        return true;
    }
}