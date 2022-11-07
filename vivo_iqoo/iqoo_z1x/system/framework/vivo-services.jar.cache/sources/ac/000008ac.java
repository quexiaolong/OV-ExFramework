package com.vivo.services.vgc;

import android.os.SystemProperties;
import com.vivo.face.common.data.Constants;
import java.util.ArrayList;

/* loaded from: classes.dex */
public final class VgcUtils {
    public static final String AFFINITY_FILE = "affinity.txt";
    public static final String COTA_CARRIER_DIR;
    public static final String COTA_DIR;
    public static final int MSG_LOAD_CONFIG_INFO = 1000;
    public static final int MSG_UPDATE_CONFIG_INFO = 2000;
    public static final String TAG = "VGC";
    public static final String VGC_COMMON_ROOT_DIR;
    public static final String VGC_CONFIG_CUST_LIST_FILE = "vgc_custList.xml";
    public static final String VGC_CONFIG_FILE = "vgc_config.xml";
    public static final String VGC_CONFIG_LIST_FILE = "vgc_stringList.xml";
    public static final String VGC_CONFIG_PATH_FILE;
    public static final String VGC_LOAD_CONFIG_PATH_FILE;
    public static final String VGC_PATH_CONFIG_PATH_FILE = "vgc_path_config.xml";
    public static final String VGC_PROJECT_ROOT_DIR;
    public static ArrayList defConfigDirArray;
    public static ArrayList defFileConfigDirArray;
    public static final boolean VGC_SUPPORT = SystemProperties.getBoolean("ro.vivo.vgc.activate", false);
    public static final boolean DEBUG = SystemProperties.get("persist.sys.log.ctrl", "no").equals("yes");
    public static final boolean CHECK_UP = SystemProperties.get("persist.vgc_checkup", "yes").equals("yes");
    public static final boolean enableGoogle = SystemProperties.get("ro.vivo.os.name", "NA").equalsIgnoreCase("vos");
    public static final boolean enablePDPB = SystemProperties.get("ro.vgc.project.type", "NA").equalsIgnoreCase("PDPB");
    public static final String VGC_VERSION = SystemProperties.get("ro.vgc.software.version", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
    public static final String CARRIER = SystemProperties.get("persist.product.carrier.name", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
    public static final String REGION = SystemProperties.get("ro.product.country.region", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
    public static final String VGC_DIR = SystemProperties.get("ro.vgc.config.rootdir", "/vgc/");
    public static final String VIVO_DIR = SystemProperties.get("ro.vgc.config.vivodir", "/vgc/cust/vivo/");
    public static final String CARRIER_DIR = SystemProperties.get("ro.vgc.config.carrierdir", "/vgc/cust/carrier/");
    public static final String REGION_DIR = SystemProperties.get("ro.vgc.config.regiondir", "/vgc/cust/region/");
    public static final String TIERLEVEL = SystemProperties.get("ro.system.vivo.tierlevel", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
    public static final String PROJECT = SystemProperties.get("ro.vivo.oem.model", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
    public static final String VGC_VIVO_ROOT = VIVO_DIR;
    public static final String VGC_CARRIER_ROOT = CARRIER_DIR + CARRIER + "/";
    public static final String VGC_REGION_ROOT = REGION_DIR + REGION + "/";

    static {
        StringBuilder sb = new StringBuilder();
        sb.append(VGC_DIR);
        sb.append("cota/");
        COTA_DIR = SystemProperties.get("ro.vgc.cota.dir", sb.toString());
        COTA_CARRIER_DIR = COTA_DIR + "cust/carrier/";
        VGC_COMMON_ROOT_DIR = SystemProperties.get("ro.vgc.common.dir", "/system/vgc-common/");
        VGC_PROJECT_ROOT_DIR = SystemProperties.get("ro.vgc.project.dir", "/system/vgc-project/");
        VGC_CONFIG_PATH_FILE = VGC_DIR + "cust/vgc_path_config.xml";
        VGC_LOAD_CONFIG_PATH_FILE = VGC_DIR + "cust/vgc_load.xml";
        defConfigDirArray = new ArrayList();
        defFileConfigDirArray = new ArrayList();
        defConfigDirArray.add(VGC_COMMON_ROOT_DIR + "cust/vivo/");
        defConfigDirArray.add(VGC_COMMON_ROOT_DIR + "cust/google/");
        defConfigDirArray.add(VGC_COMMON_ROOT_DIR + "cust/google/region/");
        defConfigDirArray.add(VGC_COMMON_ROOT_DIR + "cust/region/");
        defConfigDirArray.add(VGC_COMMON_ROOT_DIR + "cust/carrier/");
        defConfigDirArray.add(VGC_PROJECT_ROOT_DIR + "cust/vivo/");
        defConfigDirArray.add(VGC_PROJECT_ROOT_DIR + "cust/project/");
        defConfigDirArray.add(VGC_PROJECT_ROOT_DIR + "cust/google/");
        defConfigDirArray.add(VGC_PROJECT_ROOT_DIR + "cust/region/");
        defConfigDirArray.add(VGC_PROJECT_ROOT_DIR + "cust/carrier/");
        defConfigDirArray.add(VIVO_DIR);
        defConfigDirArray.add(REGION_DIR);
        defConfigDirArray.add(CARRIER_DIR);
        defConfigDirArray.add(COTA_DIR + "cust/vivo/");
        defConfigDirArray.add(COTA_DIR + "cust/region/");
        defConfigDirArray.add(COTA_DIR + "cust/carrier/");
        defFileConfigDirArray.add(VGC_COMMON_ROOT_DIR + "cust/");
        defFileConfigDirArray.add(VGC_PROJECT_ROOT_DIR + "cust/");
        defFileConfigDirArray.add(VGC_DIR + "cust/");
        defFileConfigDirArray.add(COTA_DIR + "cust/");
    }
}