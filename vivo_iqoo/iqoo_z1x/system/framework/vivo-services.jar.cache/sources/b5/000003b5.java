package com.android.server.pm.dex;

import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.FtDeviceInfo;
import com.vivo.framework.configuration.ConfigurationManager;
import com.vivo.services.rms.Platform;
import com.vivo.services.superresolution.Constant;
import com.vivo.statistics.sdk.GatherManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import vivo.app.configuration.ConfigurationObserver;
import vivo.app.configuration.ContentValuesList;
import vivo.app.configuration.StringList;

/* loaded from: classes.dex */
public class RMPms {
    private static final String DEX2OAT64_ENABLE = "persist.vivo.dex2oat64.enable";
    private static final String DEX_INTERCEPT_FILE = "/data/bbkcore/InterceptUnknownDex.xml";
    private static final String DEX_INTERCEPT_LIST = "dex_intercept_list";
    private static final String DEX_TO_OAT_ENABLE = "dex_to_oat_enable";
    private static final String DEX_TO_OAT_FILE = "/data/bbkcore/Dex2Oat64.xml";
    private static final String DEX_TO_OAT_LIST = "dex_to_oat_list";
    private static final String IS_INSTALL_COMPILATION_PLAN_A = "is_install_compilation_plan_A";
    private static String TAG = "RMPms";
    private static final int TRON_COMPILATION_FILTER_EXTRACT = 3;
    private static final int TRON_COMPILATION_FILTER_FAKE_RUN_FROM_APK = 12;
    private static final int TRON_COMPILATION_FILTER_QUICKEN = 5;
    private static final int TRON_COMPILATION_FILTER_VERIFY = 4;
    private ConfigurationObserver dex2oat64Observer;
    private ConfigurationObserver dexInterceptObserver;
    private boolean mCompilePlanA;
    private ConfigurationManager mConfigurationManager;
    private boolean mDex2oat64Enable;
    private ContentValuesList mDex2oat64List;
    private StringList mDexBlackList;
    private final ArrayList<String> mDexInterceptList;
    private final HashMap<String, Integer> mDexoptFailMap;
    private boolean mInited;

    private RMPms() {
        this.mDex2oat64Enable = true;
        this.mInited = false;
        this.mCompilePlanA = Platform.isHighPerfDevice();
        this.mDexoptFailMap = new HashMap<>();
        this.mDexInterceptList = new ArrayList<String>() { // from class: com.android.server.pm.dex.RMPms.1
            {
                add(Constant.APP_DOUYIN);
                add(Constant.APP_TOUTIAO);
                add("com.smile.gifmaker");
            }
        };
        this.dexInterceptObserver = new ConfigurationObserver() { // from class: com.android.server.pm.dex.RMPms.2
            public void onConfigChange(String file, String name) {
                if (RMPms.this.mConfigurationManager != null) {
                    StringList list = RMPms.this.mConfigurationManager.getStringList(RMPms.DEX_INTERCEPT_FILE, RMPms.DEX_INTERCEPT_LIST);
                    RMPms.this.updateDexInterceptList(list);
                }
            }
        };
        this.dex2oat64Observer = new ConfigurationObserver() { // from class: com.android.server.pm.dex.RMPms.3
            public void onConfigChange(String file, String name) {
                if (RMPms.this.mConfigurationManager != null) {
                    ContentValuesList list = RMPms.this.mConfigurationManager.getContentValuesList(RMPms.DEX_TO_OAT_FILE, RMPms.DEX_TO_OAT_LIST);
                    RMPms.this.updateInstallStatus(list);
                }
            }
        };
    }

    public void initialize() {
        this.mInited = true;
        ConfigurationManager configurationManager = ConfigurationManager.getInstance();
        this.mConfigurationManager = configurationManager;
        if (configurationManager != null) {
            this.mDexBlackList = configurationManager.getStringList(DEX_INTERCEPT_FILE, DEX_INTERCEPT_LIST);
            this.mDex2oat64List = this.mConfigurationManager.getContentValuesList(DEX_TO_OAT_FILE, DEX_TO_OAT_LIST);
            updateDexInterceptList(this.mDexBlackList);
            updateInstallStatus(this.mDex2oat64List);
            this.mConfigurationManager.registerObserver(this.mDexBlackList, this.dexInterceptObserver);
            this.mConfigurationManager.registerObserver(this.mDex2oat64List, this.dex2oat64Observer);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Instance {
        private static final RMPms INSTANCE = new RMPms();

        private Instance() {
        }
    }

    public static RMPms getInstance() {
        return Instance.INSTANCE;
    }

    public void updateDexInterceptList(StringList dexInterceptList) {
        if (!this.mInited) {
            return;
        }
        synchronized (this.mDexInterceptList) {
            if (dexInterceptList != null) {
                if (!dexInterceptList.isEmpty()) {
                    try {
                        ArrayList<String> list = (ArrayList) dexInterceptList.getValues();
                        if (list != null && !list.isEmpty()) {
                            this.mDexInterceptList.clear();
                            Iterator<String> it = list.iterator();
                            while (it.hasNext()) {
                                String packageName = it.next();
                                this.mDexInterceptList.add(packageName);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void updateInstallStatus(ContentValuesList list) {
        if (!this.mInited) {
            return;
        }
        if (list != null) {
            try {
                if (!list.isEmpty()) {
                    if (list.getValue(DEX_TO_OAT_ENABLE) != null) {
                        this.mDex2oat64Enable = Boolean.parseBoolean(list.getValue(DEX_TO_OAT_ENABLE));
                    }
                    String compilePlan = list.getValue(IS_INSTALL_COMPILATION_PLAN_A);
                    if (compilePlan != null && compilePlan.equals("true")) {
                        this.mCompilePlanA = true;
                    } else if (compilePlan != null && compilePlan.equals("false")) {
                        this.mCompilePlanA = false;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (this.mDex2oat64Enable) {
            SystemProperties.set(DEX2OAT64_ENABLE, "true");
        } else {
            SystemProperties.set(DEX2OAT64_ENABLE, "false");
        }
    }

    public boolean isNeedIntercept(String pkgName, int uid) {
        if (uid < 10000 || !this.mInited) {
            return false;
        }
        DexInterceptHelper dexInterceptHelper = new DexInterceptHelper();
        if (this.mDexInterceptList.contains(pkgName)) {
            dexInterceptHelper.send(pkgName, 1);
            return true;
        }
        dexInterceptHelper.send(pkgName, 0);
        return false;
    }

    public boolean isPlanA() {
        return this.mCompilePlanA;
    }

    public boolean isDexoptFailLessThreshold(String pkgName) {
        HashMap<String, Integer> hashMap;
        if (pkgName == null || (hashMap = this.mDexoptFailMap) == null) {
            return false;
        }
        synchronized (hashMap) {
            if (this.mDexoptFailMap.containsKey(pkgName)) {
                int times = this.mDexoptFailMap.get(pkgName).intValue();
                if (times >= 2) {
                    return false;
                }
                this.mDexoptFailMap.remove(pkgName);
                this.mDexoptFailMap.put(pkgName, Integer.valueOf(times + 1));
                return true;
            }
            this.mDexoptFailMap.put(pkgName, 1);
            return true;
        }
    }

    public void deleteDexoptFail(String pkgName) {
        HashMap<String, Integer> hashMap;
        if (pkgName == null || (hashMap = this.mDexoptFailMap) == null) {
            return;
        }
        synchronized (hashMap) {
            this.mDexoptFailMap.remove(pkgName);
        }
    }

    public void gatherDexOptInformation(String pkgName, int compilerFilter, boolean result, long startTime, int startTemperature) {
        String compilerLastStatus = getCompilerFilter(compilerFilter);
        String compilerNowStatus = result ? "speed-profile" : compilerLastStatus;
        int dexOptDur = (int) (SystemClock.uptimeMillis() - startTime);
        GatherManager.getInstance().gather("dexopt_supplement", new Object[]{pkgName, compilerLastStatus, compilerNowStatus, Integer.valueOf(dexOptDur), Integer.valueOf(startTemperature), Integer.valueOf(FtDeviceInfo.getBoardTempure())});
    }

    public String getCompilerFilter(int compilerFilter) {
        if (compilerFilter != 3) {
            if (compilerFilter != 4) {
                if (compilerFilter != 5) {
                    if (compilerFilter == 12) {
                        return "run-from-apk";
                    }
                    return "unknown";
                }
                return "quicken";
            }
            return "verify";
        }
        return "extract";
    }
}