package com.vivo.services.rms;

import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import com.vivo.services.security.server.VivoPermissionUtils;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

/* loaded from: classes.dex */
public class VspaConfigs {
    private static final String KEY_DISABLE_DO_FRAME_FRONT = "key_disable_do_frame_front";
    private static final String KEY_GAME_SDK_SERVER = "key_game_sdk_server";
    private static final String KEY_TEXT_VIEW_FILTER = "key_textview_filter";
    private static final String KEY_VERSION = "key_version";
    private static final String KEY_VGT_MTR = "key_vgt_mtr";
    private static final String KEY_VIEW_HARDWARE_ACC_BY_ACTIVITY = "key_view_hardware_acc_by_activity";
    private static final String KEY_VIEW_HARDWARE_ACC_BY_VIEW = "key_view_hardware_acc_by_view";
    private static final String KEY_WINDOW_HARDWARE_ACC = "key_window_hardware_acc";
    private static final ArrayList<String> DISABLE_DO_FRAME_FRONT_LIST = new ArrayList<>();
    private static final ArrayList<String> GAME_SDK_SERVER_LIST = new ArrayList<>();
    private static final ArrayMap<String, ArrayList<String>> WINDOW_HARDWARE_ACC_LIST = new ArrayMap<>();
    private static final ArrayMap<String, ArrayList<String>> VIEW_HARDWARE_ACC_BY_ACTIVITY_LIST = new ArrayMap<>();
    private static final ArrayMap<String, ArrayList<String>> VIEW_HARDWARE_ACC_BY_VIEW_LIST = new ArrayMap<>();
    private static final ArrayMap<String, ArrayList<String>> TEXT_VIEW_FILTER_LIST = new ArrayMap<>();
    private static final ArrayMap<String, ArrayList<String>> VGT_MTR_LIST = new ArrayMap<>();
    private static boolean FEATURE_ENABLED = SystemProperties.getBoolean("persist.sys.disable_vspa_configs", true);
    private static Object LOCK = new Object();
    private static int sVersion = -1;

    static {
        DISABLE_DO_FRAME_FRONT_LIST.add(VivoPermissionUtils.OS_PKG);
        DISABLE_DO_FRAME_FRONT_LIST.add("com.carrot.carrotfantasy");
        DISABLE_DO_FRAME_FRONT_LIST.add("com.ea.simcitymobile");
        DISABLE_DO_FRAME_FRONT_LIST.add("com.chinsoft.ChineseLunarCalendarLite");
        GAME_SDK_SERVER_LIST.add("com.tencent.tmgp.sgame");
        GAME_SDK_SERVER_LIST.add("com.tencent.tmgp.speedmobile");
        ArrayList<String> list = new ArrayList<>();
        list.clear();
        list.add("com.taobao.taobao/*");
        list.add("com.xueqiu.android/*");
        list.add("com.tencent.mm/*");
        list.add("com.youdao.dict/VerticalActivity");
        list.add("com.jingdong.app.mall/ChatImagePreviewActivity");
        initMapLocked(WINDOW_HARDWARE_ACC_LIST, list);
        list.clear();
        list.add("com.tencent.mm/SnsBrowseUI");
        list.add("com.tencent.mm/ImagePreviewUI");
        list.add("com.tencent.mm/SnsUploadBrowseUI");
        list.add("com.tencent.mobileqq/cooperation.qzone.QzoneFeedsPluginProxyActivity");
        initMapLocked(VIEW_HARDWARE_ACC_BY_ACTIVITY_LIST, list);
    }

    public static void setBundle(Bundle bundle) {
        int version = bundle.getInt("key_version", sVersion);
        if (sVersion >= version) {
            return;
        }
        sVersion = version;
        synchronized (LOCK) {
            initListLocked(DISABLE_DO_FRAME_FRONT_LIST, bundle.getStringArrayList(KEY_DISABLE_DO_FRAME_FRONT));
            initListLocked(GAME_SDK_SERVER_LIST, bundle.getStringArrayList(KEY_GAME_SDK_SERVER));
            initMapLocked(WINDOW_HARDWARE_ACC_LIST, bundle.getStringArrayList(KEY_WINDOW_HARDWARE_ACC));
            initMapLocked(VIEW_HARDWARE_ACC_BY_ACTIVITY_LIST, bundle.getStringArrayList(KEY_VIEW_HARDWARE_ACC_BY_ACTIVITY));
            initMapLocked(VIEW_HARDWARE_ACC_BY_VIEW_LIST, bundle.getStringArrayList(KEY_VIEW_HARDWARE_ACC_BY_VIEW));
            initMapLocked(TEXT_VIEW_FILTER_LIST, bundle.getStringArrayList(KEY_TEXT_VIEW_FILTER));
            initMapLocked(VGT_MTR_LIST, bundle.getStringArrayList(KEY_VGT_MTR));
        }
    }

    public static void dump(PrintWriter pw, String[] args) {
        if (!isAllowDump()) {
            return;
        }
        if (args.length >= 1 && "disable".equals(args[0])) {
            FEATURE_ENABLED = false;
        } else if (args.length >= 1 && "enable".equals(args[0])) {
            FEATURE_ENABLED = true;
        }
        pw.println("FEATURE_ENABLED = " + FEATURE_ENABLED);
        pw.println("VERSION = " + sVersion);
        synchronized (LOCK) {
            dumpListLocked(pw, DISABLE_DO_FRAME_FRONT_LIST, "DISABLE_DO_FRAME_FRONT_LIST");
            dumpListLocked(pw, GAME_SDK_SERVER_LIST, "GAME_SDK_SERVER_LIST");
            dumpMapLocked(pw, WINDOW_HARDWARE_ACC_LIST, "WINDOW_HARDWARE_ACC_LIST");
            dumpMapLocked(pw, VIEW_HARDWARE_ACC_BY_ACTIVITY_LIST, "VIEW_HARDWARE_ACC_BY_ACTIVITY_LIST");
            dumpMapLocked(pw, VIEW_HARDWARE_ACC_BY_VIEW_LIST, "VIEW_HARDWARE_ACC_BY_VIEW_LIST");
            dumpMapLocked(pw, TEXT_VIEW_FILTER_LIST, "TEXT_VIEW_FILTER_LIST");
            dumpMapLocked(pw, VGT_MTR_LIST, "VGT_MTR_LIST");
        }
    }

    public static void initAppCoreSettings(Bundle coreSettings, ApplicationInfo info, String procName) {
        if (!FEATURE_ENABLED || coreSettings == null || info == null || info.packageName == null || procName == null) {
            return;
        }
        synchronized (LOCK) {
            addFlagsLocked(coreSettings, info, procName);
            addListsLocked(coreSettings, procName);
        }
    }

    private static void addFlagsLocked(Bundle coreSettings, ApplicationInfo info, String procName) {
        int flags = 0;
        if (isDoframeFront(info, procName)) {
            flags = 0 | 1;
        }
        if (isGameSdkServer(info, procName)) {
            flags |= 2;
        }
        if (flags != 0) {
            coreSettings.putInt("__key_vspa_flags", flags);
        }
    }

    private static void addListsLocked(Bundle coreSettings, String procName) {
        Bundle data = new Bundle();
        ArrayList<String> list = null;
        for (String key : WINDOW_HARDWARE_ACC_LIST.keySet()) {
            if (procName.startsWith(key)) {
                ArrayList<String> list2 = WINDOW_HARDWARE_ACC_LIST.get(key);
                list = list2;
            }
        }
        if (list == null || list.isEmpty()) {
            ArrayList<String> list3 = WINDOW_HARDWARE_ACC_LIST.get("*");
            list = list3;
        }
        if (list != null && !list.isEmpty()) {
            data.putStringArrayList("__key_window_hardware_acc", new ArrayList<>(list));
        }
        ArrayList<String> list4 = null;
        for (String key2 : VIEW_HARDWARE_ACC_BY_ACTIVITY_LIST.keySet()) {
            if (procName.startsWith(key2)) {
                ArrayList<String> list5 = VIEW_HARDWARE_ACC_BY_ACTIVITY_LIST.get(key2);
                list4 = list5;
            }
        }
        if (list4 == null || list4.isEmpty()) {
            ArrayList<String> list6 = VIEW_HARDWARE_ACC_BY_ACTIVITY_LIST.get("*");
            list4 = list6;
        }
        if (list4 != null && !list4.isEmpty()) {
            data.putStringArrayList("__key_view_hardware_acc_by_activity", new ArrayList<>(list4));
        }
        ArrayList<String> list7 = null;
        for (String key3 : VIEW_HARDWARE_ACC_BY_VIEW_LIST.keySet()) {
            if (procName.startsWith(key3)) {
                ArrayList<String> list8 = VIEW_HARDWARE_ACC_BY_VIEW_LIST.get(key3);
                list7 = list8;
            }
        }
        if (list7 == null || list7.isEmpty()) {
            ArrayList<String> list9 = VIEW_HARDWARE_ACC_BY_VIEW_LIST.get("*");
            list7 = list9;
        }
        if (list7 != null && !list7.isEmpty()) {
            data.putStringArrayList("__key_view_hardware_acc_by_view", new ArrayList<>(list7));
        }
        ArrayList<String> list10 = null;
        for (String key4 : TEXT_VIEW_FILTER_LIST.keySet()) {
            if (procName.startsWith(key4)) {
                ArrayList<String> list11 = TEXT_VIEW_FILTER_LIST.get(key4);
                list10 = list11;
            }
        }
        if (list10 == null || list10.isEmpty()) {
            ArrayList<String> list12 = TEXT_VIEW_FILTER_LIST.get("*");
            list10 = list12;
        }
        if (list10 != null && !list10.isEmpty()) {
            data.putStringArrayList("__key_text_view_filter", new ArrayList<>(list10));
        }
        ArrayList<String> list13 = VGT_MTR_LIST.get(procName);
        ArrayList<String> list14 = list13;
        if (list14 != null) {
            data.putStringArrayList("__key_vgt_mtr_values", new ArrayList<>(list14));
        }
        if (!data.isEmpty()) {
            coreSettings.putBundle("__key_vspa_lists", data);
        }
    }

    private static void initListLocked(ArrayList<String> targetList, ArrayList<String> list) {
        if (list == null) {
            return;
        }
        targetList.clear();
        targetList.addAll(list);
    }

    private static void dumpListLocked(PrintWriter pw, ArrayList<String> list, String tag) {
        if (list == null) {
            return;
        }
        pw.print(tag);
        pw.print("=");
        pw.println(list);
    }

    private static void initMapLocked(ArrayMap<String, ArrayList<String>> map, ArrayList<String> list) {
        if (list == null) {
            return;
        }
        map.clear();
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            String item = it.next();
            int index = item.indexOf(47);
            if (index > 0 && index != item.length() - 1) {
                String key = item.substring(0, index);
                String value = item.substring(index + 1);
                if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
                    ArrayList<String> targetList = map.get(key);
                    if (targetList == null) {
                        targetList = new ArrayList<>();
                        map.put(key, targetList);
                    }
                    targetList.add(value);
                }
            }
        }
    }

    private static void dumpMapLocked(PrintWriter pw, ArrayMap<String, ArrayList<String>> map, String tag) {
        if (map == null) {
            return;
        }
        pw.print(tag);
        pw.print("=");
        pw.println(map);
    }

    private static boolean isDoframeFront(ApplicationInfo info, String procName) {
        if ((info.flags & 8) != 0 || info.category == 0) {
            return false;
        }
        Iterator<String> it = DISABLE_DO_FRAME_FRONT_LIST.iterator();
        while (it.hasNext()) {
            String prefix = it.next();
            if (procName.startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isGameSdkServer(ApplicationInfo info, String procName) {
        if (info.isSystemApp() || !info.packageName.equals(procName) || UserHandle.getAppId(info.uid) < 10000) {
            return false;
        }
        Iterator<String> it = GAME_SDK_SERVER_LIST.iterator();
        while (it.hasNext()) {
            String prefix = it.next();
            if (procName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAllowDump() {
        return SystemProperties.getBoolean("persist.rms.allow_dump", false);
    }
}