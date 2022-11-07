package com.vivo.services.rms;

import android.os.Bundle;
import com.android.server.am.VivoFcmGoldPassSupervisor;
import com.android.server.am.frozen.WorkingStateManager;
import com.vivo.services.proxy.ProxyConfigs;
import com.vivo.services.rms.appmng.namelist.OomPreloadList;
import com.vivo.services.rms.appmng.namelist.OomPreviousList;
import com.vivo.services.rms.appmng.namelist.OomProtectList;
import com.vivo.services.rms.appmng.namelist.OomStaticList;
import com.vivo.services.rms.cgrp.CgrpUtils;
import com.vivo.services.rms.display.RefreshRateAdjuster;
import com.vivo.services.rms.display.WindowRequestManager;
import com.vivo.services.rms.proxy.ProxyUtils;
import com.vivo.services.rms.proxy.VivoBinderProxy;
import java.io.File;
import java.util.ArrayList;

/* loaded from: classes.dex */
public class Config {
    private static final String APP_FPS_SETTINGS = "key_app_fps_settings";
    private static final String BG_HANGUP = "com.vivo.gamewatch.bg_hangup";
    private static final String CGROUP_CONFIGS = "key_cgroup_configs";
    private static final String GAME_SCENE = "com.vivo.gamewatch.game_scene";
    private static final String IS_IN_KERNEL_LMK = "is_in_kernel_lmk";
    private static final String IS_PRELOAD_SUPPORTED = "is_preload_supported_version2";
    private static final String IS_RMS_ENABLED = "com.vivo.rms.policy.PolicyManager.isRmsEnabled";
    private static final String LMK_PARA_MINFREE_PATH = "/sys/module/lowmemorykiller/parameters/minfree";
    private static final String OOM_PRELOAD_GET_LIST = "oom_preload_get_list";
    private static final String OOM_PRELOAD_LIST = "oom_preload_list";
    private static final String OOM_PREVIOUS_EXCLUDED_LIST = "previous_excluded_list";
    private static final String OOM_PREVIOUS_LIST = "oom_previous_list";
    private static final String OOM_STATIC_LIST = "oom_static_list";
    private static final String PREVIEW_CONFIGS = "key_preview_configs";
    private static final String PROTECT_LIST = "com.vivo.rms.policy.PolicyManager.protectlist";
    private static final String PROXY_BROADCAST_CONFIGS = "key_broadcast_configs";
    private static final String PROXY_CONFIGS = "key_bdproxy_configs";
    private static final String REFRESH_RATE_ADJUSTER_SETTINGS = "key_refresh_rate_adjuster_settings";
    private static final String RESTORE_OOM_LEVELS = "restore_oom_levels";
    private static final String SET_KEEP_QUIET_TYPE = "set_keep_quiet_type";
    private static final String UPDATE_FCM_POLICY = "update_fcm_policy";
    private static final String UPDATE_OOM_LEVELS = "update_oom_levels";
    private static final String VIVO_BINDER_PROXY = "vivo_binder_proxy";
    private static final String VSPA_FLAGS_LIST = "vspa_flags_list";

    public static boolean setBundle(String name, Bundle bundle) {
        if (bundle == null || name == null) {
            return false;
        }
        char c = 65535;
        switch (name.hashCode()) {
            case -2100752099:
                if (name.equals(OOM_STATIC_LIST)) {
                    c = 2;
                    break;
                }
                break;
            case -2032143858:
                if (name.equals(CGROUP_CONFIGS)) {
                    c = 14;
                    break;
                }
                break;
            case -1870713193:
                if (name.equals(IS_RMS_ENABLED)) {
                    c = 0;
                    break;
                }
                break;
            case -1488184910:
                if (name.equals(RESTORE_OOM_LEVELS)) {
                    c = 11;
                    break;
                }
                break;
            case -1459683252:
                if (name.equals(VIVO_BINDER_PROXY)) {
                    c = 19;
                    break;
                }
                break;
            case -1418736194:
                if (name.equals(PROXY_CONFIGS)) {
                    c = 15;
                    break;
                }
                break;
            case -1224353081:
                if (name.equals(VSPA_FLAGS_LIST)) {
                    c = 6;
                    break;
                }
                break;
            case -997250389:
                if (name.equals(OOM_PREVIOUS_EXCLUDED_LIST)) {
                    c = 4;
                    break;
                }
                break;
            case -993114453:
                if (name.equals(REFRESH_RATE_ADJUSTER_SETTINGS)) {
                    c = '\r';
                    break;
                }
                break;
            case -876906828:
                if (name.equals(OOM_PREVIOUS_LIST)) {
                    c = 3;
                    break;
                }
                break;
            case -775298682:
                if (name.equals(OOM_PRELOAD_LIST)) {
                    c = 5;
                    break;
                }
                break;
            case -764621783:
                if (name.equals(PROTECT_LIST)) {
                    c = 1;
                    break;
                }
                break;
            case -419581385:
                if (name.equals(UPDATE_FCM_POLICY)) {
                    c = '\b';
                    break;
                }
                break;
            case -295341549:
                if (name.equals(PROXY_BROADCAST_CONFIGS)) {
                    c = 16;
                    break;
                }
                break;
            case 123567642:
                if (name.equals(PREVIEW_CONFIGS)) {
                    c = 17;
                    break;
                }
                break;
            case 897675767:
                if (name.equals(APP_FPS_SETTINGS)) {
                    c = '\f';
                    break;
                }
                break;
            case 1768689815:
                if (name.equals(UPDATE_OOM_LEVELS)) {
                    c = '\n';
                    break;
                }
                break;
            case 1917954629:
                if (name.equals(GAME_SCENE)) {
                    c = 7;
                    break;
                }
                break;
            case 1945511298:
                if (name.equals(SET_KEEP_QUIET_TYPE)) {
                    c = '\t';
                    break;
                }
                break;
            case 1953743553:
                if (name.equals(BG_HANGUP)) {
                    c = 18;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                if (!bundle.containsKey("started")) {
                    return false;
                }
                boolean started = bundle.getBoolean("started");
                setRmsEnable(started);
                return true;
            case 1:
                OomProtectList.apply(bundle.getString("policy", "update"), bundle.getStringArrayList("PROTECT_ACTIVITY"), bundle.getStringArrayList("PROTECT_SERVICE"), bundle.getStringArrayList("PROTECT_GAME"));
                return true;
            case 2:
                OomStaticList.apply(bundle.getString("policy", "update"), bundle.getStringArrayList("proc"), bundle.getIntegerArrayList("adj"), bundle.getIntegerArrayList("state"), bundle.getIntegerArrayList("sched"));
                return true;
            case 3:
                OomPreviousList.apply(bundle.getIntegerArrayList("adj"), bundle.getIntegerArrayList("state"));
                return true;
            case 4:
                OomPreviousList.updateExcludedList(bundle.getStringArrayList("proc"));
                return true;
            case 5:
                OomPreloadList.apply(bundle.getString("policy", "update"), bundle.getStringArrayList("pkg"), bundle.getIntegerArrayList("adj"), bundle.getIntegerArrayList("state"), bundle.getIntegerArrayList("sched"));
                return true;
            case 6:
                VspaConfigs.setBundle(bundle);
                return true;
            case 7:
                String pkg = bundle.getString("pkg");
                int pid = bundle.getInt("pid");
                int scene = bundle.getInt("scene");
                if (scene == 1) {
                    GameOptManager.enterGame(pkg, pid);
                } else {
                    GameOptManager.exitGame(pkg, pid);
                }
                return true;
            case '\b':
                boolean useUnfreeze = bundle.getBoolean("isUseUnfreeze");
                ArrayList<String> fcmActionLists = bundle.getStringArrayList("fcmActionLists");
                ArrayList<String> fcmBlackLists = bundle.getStringArrayList("fcmBlackLists");
                VivoFcmGoldPassSupervisor.getInstance().updateFcmPolicy(useUnfreeze, fcmActionLists, fcmBlackLists);
                return true;
            case '\t':
                int flag = bundle.getInt("type");
                PreloadedAppRecordMgr.getInstance().setKeepQuietType(flag);
                return true;
            case '\n':
                int[] minFrees = bundle.getIntArray("minFrees");
                int[] oomAdjs = bundle.getIntArray("oomAdjs");
                RMAms.getInstance().updateOomLevels(minFrees, oomAdjs);
                return true;
            case 11:
                RMAms.getInstance().restoreOomLevels();
                return true;
            case '\f':
                WindowRequestManager.getInstance().setBundle(bundle);
                return true;
            case '\r':
                RefreshRateAdjuster.getInstance().setConfigs(bundle);
                return true;
            case 14:
                CgrpUtils.setConfigs(bundle);
                return true;
            case 15:
                ProxyUtils.setConfigs(bundle);
                return true;
            case 16:
                ProxyConfigs.setBundle(bundle);
                return true;
            case 17:
                AppPreviewAdjuster.getInstance().setBundle(bundle);
                return true;
            case 18:
                WorkingStateManager.getInstance().setState(16384, bundle);
                return true;
            case 19:
                boolean featureSupport = bundle.getBoolean("isFeatureSupport");
                ArrayList<String> binderProxyConfig = bundle.getStringArrayList("proxyWhiteList");
                VivoBinderProxy.getInstance().updateConfig(featureSupport, binderProxyConfig);
                return true;
            default:
                return false;
        }
    }

    public static Bundle getBundle(String name) {
        if (name == null || name.length() <= 0) {
            return null;
        }
        char c = 65535;
        int hashCode = name.hashCode();
        if (hashCode != 960438518) {
            if (hashCode != 1427274959) {
                if (hashCode == 1610700045 && name.equals(IS_IN_KERNEL_LMK)) {
                    c = 2;
                }
            } else if (name.equals(OOM_PRELOAD_GET_LIST)) {
                c = 1;
            }
        } else if (name.equals(IS_PRELOAD_SUPPORTED)) {
            c = 0;
        }
        if (c == 0) {
            Bundle b = new Bundle();
            b.putBoolean(IS_PRELOAD_SUPPORTED, true);
            return b;
        } else if (c != 1) {
            if (c != 2) {
                return null;
            }
            Bundle isInKernel = new Bundle();
            isInKernel.putBoolean("ret", isInKernelLmk());
            return isInKernel;
        } else {
            ArrayList<String> ret = OomPreloadList.getList();
            if (ret == null || ret.size() <= 0) {
                return null;
            }
            Bundle retB = new Bundle();
            retB.putStringArrayList(OOM_PRELOAD_GET_LIST, ret);
            return retB;
        }
    }

    public static void setRmsEnable(boolean enable) {
        RMAms.getInstance().setRmsEnable(enable);
        if (!enable) {
            SysFsModifier.restore();
            OomProtectList.restore();
            OomStaticList.restore();
            OomPreviousList.restore();
            OomPreviousList.restoreExcludedList();
            OomPreloadList.restore();
        }
    }

    private static boolean isInKernelLmk() {
        File file = new File(LMK_PARA_MINFREE_PATH);
        try {
            if (file.exists()) {
                return file.isFile();
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}