package com.vivo.services.sarpower;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import com.vivo.face.common.data.Constants;
import com.vivo.sensor.autobrightness.utils.SElog;
import com.vivo.services.sarpower.ConfigList2Parser;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.HashMap;

/* loaded from: classes.dex */
public class CommandConfig {
    private static final String BOARD_VERSION = "/sys/devs_list/board_version";
    private static final String SAR_CONFIG_PATH = "/system/etc/SarConfig.xml";
    private static final String TAG = "SarCommandConfig";
    private static final String VIVO_MTK_SAR_CONFIG_PARSED = "com.vivo.services.sarpower";
    private static Context mContext;
    private ConfigList2Parser mConfigList2Parser;
    private Handler mStateChangeHandler;
    private static final String model = SystemProperties.get("ro.vivo.product.model").toLowerCase();
    private static final String mCountryCode = SystemProperties.get("ro.product.customize.bbk", "N");
    private static final boolean isOverseas = SystemProperties.get("ro.vivo.product.overseas", "no").equals("yes");
    private static ConfigList mCameraConfigs = new ConfigList("1813_camera", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.1
        {
            put("head", new String[]{"AT+ERFTX=10,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7,7,7,0,0,0,0,0,24,24,24,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0", "AT+ERFTX=10,2,24,24,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,23,0", "AT+ERFTX=10,3,3,24,0", "AT+ERFTX=10,3,7,4,0", "AT+ERFTX=10,3,39,12,0", "AT+ERFTX=10,3,40,4,0"});
            put("body", new String[]{"AT+ERFTX=10,1,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,0,0,0,0,0,0,0,0,0,24,24,24,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0", "AT+ERFTX=10,2,20,20,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,22,0", "AT+ERFTX=10,3,3,19,0", "AT+ERFTX=10,3,39,10,0"});
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.2
        {
            put("head", new String[]{"AT+ERFTX=10,1,16,16,16,16,0,0,0,0,24,24,24,24,0,0,0,0,50,50,50,50,0,0,0,0,76,76,76,76,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0", "AT+ERFTX=10,2,65,65,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,68,0", "AT+ERFTX=10,3,3,72,0", "AT+ERFTX=10,3,5,4,0", "AT+ERFTX=10,3,7,64,0", "AT+ERFTX=10,3,8,8,0", "AT+ERFTX=10,3,38,44,0", "AT+ERFTX=10,3,39,55,0", "AT+ERFTX=10,3,40,48,0", "AT+ERFTX=10,3,41,48,0"});
            put("body", new String[]{"AT+ERFTX=10,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,32,32,32,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0", "AT+ERFTX=10,2,24,24,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,26,0", "AT+ERFTX=10,3,3,23,0", "AT+ERFTX=10,3,39,14,0"});
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK});
    private static final ConfigList[] mConfigs = {new ConfigList("td1702", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.3
        {
            put("head", new String[]{"AT+ERFTX=9,8,8,8,16,16,16", "AT+ERFTX=13,7,8,16"});
            put("body", new String[0]);
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.4
        {
            put("head", new String[]{"AT+ERFTX=9,16,16,16,32,32,32", "AT+ERFTX=13,7,16,32"});
            put("body", new String[0]);
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1718", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.5
        {
            put("head", new String[]{"AT+ERFTX=10,2,4,4,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,4,0", "AT+ERFTX=10,3,3,12,0", "AT+ERFTX=10,3,7,12,0"});
            put("body", new String[0]);
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.6
        {
            put("head", new String[]{"AT+ERFTX=10,1,0,0,0,0,0,0,0,0,16,16,16,16,16,16,16,16,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0", "AT+ERFTX=10,2,8,8,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,8,0", "AT+ERFTX=10,3,3,16,0", "AT+ERFTX=10,3,7,16,0"});
            put("body", new String[0]);
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("td1702f_ex", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.7
        {
            put("head", new String[]{"AT+ERFTX=10,2,4,4,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,4,0"});
            put("body", new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK});
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.8
        {
            put("head", new String[]{"AT+ERFTX=10,1,0,0,0,0,0,0,0,0,24,24,24,24,24,24,24,24,28,28,28,28,28,28,28,28,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0", "AT+ERFTX=10,2,52,52,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,52,0", "AT+ERFTX=10,3,3,36,0", "AT+ERFTX=10,3,40,16,0", "AT+ERFTX=10,3,41,16,0"});
            put("body", new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK});
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("td1705", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.9
        {
            put("head", new String[]{"AT+ERFTX=10,1,0,0,0,0,0,0,0,0,24,24,24,24,24,24,24,24,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,24,24,24,24,24,24,24,24,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0", "AT+ERFTX=10,2,24,24,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,24,24,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,7,8,24"});
            put("body", new String[]{"AT+ERFTX=10,1,0,0,0,0,0,0,0,0,48,48,48,48,48,48,48,48,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,48,48,48,48,48,48,48,48,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0", "AT+ERFTX=10,2,48,48,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,48,48,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,7,16,48"});
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.10
        {
            put("head", new String[]{"AT+ERFTX=10,1,0,0,0,0,0,0,0,0,24,24,24,24,24,24,24,24,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,24,24,24,24,24,24,24,24,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0", "AT+ERFTX=10,2,24,24,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,24,24,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,7,8,24"});
            put("body", new String[]{"AT+ERFTX=10,1,0,0,0,0,0,0,0,0,48,48,48,48,48,48,48,48,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,48,48,48,48,48,48,48,48,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0", "AT+ERFTX=10,2,48,48,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,48,48,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,7,16,48"});
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1803", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.11
        {
            put("head", new String[]{"AT+ERFTX=10,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,24,24,24,24,24,24,24,24", "AT+ERFTX=10,2,0,0,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,32,32,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,0,32", "AT+ERFTX=10,3,3,0,8", "AT+ERFTX=10,3,7,0,16", "AT+ERFTX=10,3,34,0,24", "AT+ERFTX=10,3,39,0,12"});
            put("body", new String[0]);
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.12
        {
            put("head", new String[]{"AT+ERFTX=10,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,32,32,32,32,32,32,32,32,40,40,40,40,40,40,40,40", "AT+ERFTX=10,2,0,0,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,48,48,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,0,48", "AT+ERFTX=10,3,3,0,24", "AT+ERFTX=10,3,7,0,24", "AT+ERFTX=10,3,34,0,32", "AT+ERFTX=10,3,39,0,32", "AT+ERFTX=10,3,38,0,16", "AT+ERFTX=10,3,41,0,16"});
            put("body", new String[0]);
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1803f_ex", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.13
        {
            put("head", new String[]{"AT+ERFTX=10,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,24,24,24,24,24,24,24,24", "AT+ERFTX=10,2,0,0,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,32,32,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,0,32", "AT+ERFTX=10,3,3,0,8", "AT+ERFTX=10,3,7,0,16"});
            put("body", new String[]{"AT+ERFTX=10,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,32,32,32,32,32,32,32,32", "AT+ERFTX=10,2,0,0,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,28,28,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,0,28", "AT+ERFTX=10,3,7,0,12"});
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.14
        {
            put("head", new String[]{"AT+ERFTX=10,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,48,48,48,48,48,48,48,48,64,64,64,64,64,64,64,64", "AT+ERFTX=10,2,0,0,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,72,72,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,0,72", "AT+ERFTX=10,3,3,0,64", "AT+ERFTX=10,3,7,0,56", "AT+ERFTX=10,3,38,0,48", "AT+ERFTX=10,3,40,0,24", "AT+ERFTX=10,3,41,0,48"});
            put("body", new String[]{"AT+ERFTX=10,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,40,40,40,40,40,40,40,40", "AT+ERFTX=10,2,0,0,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,36,36,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,0,36", "AT+ERFTX=10,3,3,0,32", "AT+ERFTX=10,3,7,0,32"});
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1803bf_ex", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.15
        {
            put("head", new String[]{"AT+ERFTX=10,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,24,24,24,24,24,24,24,24", "AT+ERFTX=10,2,0,0,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,32,32,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,0,32", "AT+ERFTX=10,3,3,0,8", "AT+ERFTX=10,3,7,0,16"});
            put("body", new String[]{"AT+ERFTX=10,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,32,32,32,32,32,32,32,32", "AT+ERFTX=10,2,0,0,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,28,28,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,0,28", "AT+ERFTX=10,3,7,0,12"});
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.16
        {
            put("head", new String[]{"AT+ERFTX=10,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,48,48,48,48,48,48,48,48,64,64,64,64,64,64,64,64", "AT+ERFTX=10,2,0,0,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,72,72,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,0,72", "AT+ERFTX=10,3,3,0,64", "AT+ERFTX=10,3,7,0,56", "AT+ERFTX=10,3,38,0,48", "AT+ERFTX=10,3,40,0,24", "AT+ERFTX=10,3,41,0,48"});
            put("body", new String[]{"AT+ERFTX=10,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,40,40,40,40,40,40,40,40", "AT+ERFTX=10,2,0,0,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,36,36,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,0,36", "AT+ERFTX=10,3,3,0,32", "AT+ERFTX=10,3,7,0,32"});
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1801", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.17
        {
            put("head", new String[]{"AT+ERFTX=9,0,0,0,0,0,0"});
            put("body", new String[]{"AT+ERFTX=9,0,0,0,0,0,0"});
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.18
        {
            put("head", new String[]{"AT+ERFTX=10,2,0,0,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,14,14,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,0,12", "AT+ERFTX=10,3,3,0,4", "AT+ERFTX=10,3,7,0,12"});
            put("body", new String[]{"AT+ERFTX=9,0,0,0,0,0,0"});
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1732", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.19
        {
            put("head", new String[]{"AT+ERFTX=5,2,28,28,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,28", "AT+ERFTX=5,3,3,20", "AT+ERFTX=5,3,7,12"});
            put("body", new String[]{"AT+ERFTX=9,0,0,0,0,0,0"});
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.20
        {
            put("head", new String[]{"AT+ERFTX=5,2,36,36,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,36", "AT+ERFTX=5,3,3,32", "AT+ERFTX=5,3,7,16"});
            put("body", new String[]{"AT+ERFTX=9,0,0,0,0,0,0"});
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1732f_ex", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.21
        {
            put("head", new String[]{"AT+ERFTX=5,2,20,20,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,20", "AT+ERFTX=5,3,3,16", "AT+ERFTX=5,3,7,24"});
            put("body", new String[]{"AT+ERFTX=5,2,20,20,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,28", "AT+ERFTX=5,3,3,12", "AT+ERFTX=5,3,7,28", "AT+ERFTX=5,3,40,8"});
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.22
        {
            put("head", new String[]{"AT+ERFTX=5,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,80,80,80,80,80,80,80,80,104,104,104,104,104,104,104,104", "AT+ERFTX=5,2,72,72,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,72", "AT+ERFTX=5,3,3,56", "AT+ERFTX=5,3,5,16", "AT+ERFTX=5,3,7,64", "AT+ERFTX=5,3,38,48", "AT+ERFTX=5,3,40,72", "AT+ERFTX=5,3,41,48"});
            put("body", new String[]{"AT+ERFTX=5,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,80,80,80,80,80,80,80,80,104,104,104,104,104,104,104,104", "AT+ERFTX=5,2,72,72,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,72", "AT+ERFTX=5,3,3,56", "AT+ERFTX=5,3,5,16", "AT+ERFTX=5,3,7,64", "AT+ERFTX=5,3,38,48", "AT+ERFTX=5,3,40,72", "AT+ERFTX=5,3,41,48"});
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1732f_ex_RU", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.23
        {
            put("head", new String[]{"AT+ERFTX=5,2,8,8,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,8", "AT+ERFTX=5,3,3,16", "AT+ERFTX=5,3,7,24"});
            put("body", new String[]{"AT+ERFTX=5,2,16,16,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,16", "AT+ERFTX=5,3,3,16", "AT+ERFTX=5,3,7,28", "AT+ERFTX=5,3,40,8"});
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.24
        {
            put("head", new String[]{"AT+ERFTX=5,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,80,80,80,80,80,80,80,80,104,104,104,104,104,104,104,104", "AT+ERFTX=5,2,72,72,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,72", "AT+ERFTX=5,3,3,56", "AT+ERFTX=5,3,7,64", "AT+ERFTX=5,3,38,48", "AT+ERFTX=5,3,40,72", "AT+ERFTX=5,3,41,48"});
            put("body", new String[]{"AT+ERFTX=5,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,80,80,80,80,80,80,80,80,104,104,104,104,104,104,104,104", "AT+ERFTX=5,2,72,72,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,72", "AT+ERFTX=5,3,3,56", "AT+ERFTX=5,3,7,64", "AT+ERFTX=5,3,38,48", "AT+ERFTX=5,3,40,72", "AT+ERFTX=5,3,41,48"});
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1732f_ex_PH", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.25
        {
            put("head", new String[]{"AT+ERFTX=5,3,3,24", "AT+ERFTX=5,3,7,24"});
            put("body", new String[]{"AT+ERFTX=5,2,8,8,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,8", "AT+ERFTX=5,3,3,24", "AT+ERFTX=5,3,7,28", "AT+ERFTX=5,3,40,8"});
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.26
        {
            put("head", new String[]{"AT+ERFTX=5,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,80,80,80,80,80,80,80,80,104,104,104,104,104,104,104,104", "AT+ERFTX=5,2,72,72,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,72", "AT+ERFTX=5,3,3,56", "AT+ERFTX=5,3,5,16", "AT+ERFTX=5,3,7,64", "AT+ERFTX=5,3,38,48", "AT+ERFTX=5,3,40,72", "AT+ERFTX=5,3,41,48"});
            put("body", new String[]{"AT+ERFTX=5,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,80,80,80,80,80,80,80,80,104,104,104,104,104,104,104,104", "AT+ERFTX=5,2,72,72,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,72", "AT+ERFTX=5,3,3,56", "AT+ERFTX=5,3,5,16", "AT+ERFTX=5,3,7,64", "AT+ERFTX=5,3,38,48", "AT+ERFTX=5,3,40,72", "AT+ERFTX=5,3,41,48"});
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1732b", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.27
        {
            put("head", new String[]{"AT+ERFTX=5,2,20,20,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,20", "AT+ERFTX=5,3,3,16", "AT+ERFTX=5,3,7,24"});
            put("body", new String[]{"AT+ERFTX=9,0,0,0,0,0,0"});
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.28
        {
            put("head", new String[]{"AT+ERFTX=5,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,80,80,80,80,80,80,80,80,104,104,104,104,104,104,104,104", "AT+ERFTX=5,2,72,72,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,72", "AT+ERFTX=5,3,3,56", "AT+ERFTX=5,3,5,16", "AT+ERFTX=5,3,7,64", "AT+ERFTX=5,3,38,48", "AT+ERFTX=5,3,40,72", "AT+ERFTX=5,3,41,48"});
            put("body", new String[]{"AT+ERFTX=9,0,0,0,0,0,0"});
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1732bf_ex", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.29
        {
            put("head", new String[]{"AT+ERFTX=5,2,20,20,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,20", "AT+ERFTX=5,3,3,16", "AT+ERFTX=5,3,7,24"});
            put("body", new String[]{"AT+ERFTX=5,2,20,20,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,28", "AT+ERFTX=5,3,3,12", "AT+ERFTX=5,3,7,28", "AT+ERFTX=5,3,40,8"});
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.30
        {
            put("head", new String[]{"AT+ERFTX=5,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,80,80,80,80,80,80,80,80,104,104,104,104,104,104,104,104", "AT+ERFTX=5,2,72,72,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,72", "AT+ERFTX=5,3,3,56", "AT+ERFTX=5,3,5,16", "AT+ERFTX=5,3,7,64", "AT+ERFTX=5,3,38,48", "AT+ERFTX=5,3,40,72", "AT+ERFTX=5,3,41,48"});
            put("body", new String[]{"AT+ERFTX=5,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,80,80,80,80,80,80,80,80,104,104,104,104,104,104,104,104", "AT+ERFTX=5,2,72,72,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,72", "AT+ERFTX=5,3,3,56", "AT+ERFTX=5,3,5,16", "AT+ERFTX=5,3,7,64", "AT+ERFTX=5,3,38,48", "AT+ERFTX=5,3,40,72", "AT+ERFTX=5,3,41,48"});
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1732bf_ex_RU", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.31
        {
            put("head", new String[]{"AT+ERFTX=5,2,8,8,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,8", "AT+ERFTX=5,3,3,16", "AT+ERFTX=5,3,7,24"});
            put("body", new String[]{"AT+ERFTX=5,2,16,16,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,16", "AT+ERFTX=5,3,3,16", "AT+ERFTX=5,3,7,28", "AT+ERFTX=5,3,40,8"});
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.32
        {
            put("head", new String[]{"AT+ERFTX=5,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,80,80,80,80,80,80,80,80,104,104,104,104,104,104,104,104", "AT+ERFTX=5,2,72,72,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,72", "AT+ERFTX=5,3,3,56", "AT+ERFTX=5,3,7,64", "AT+ERFTX=5,3,38,48", "AT+ERFTX=5,3,40,72", "AT+ERFTX=5,3,41,48"});
            put("body", new String[]{"AT+ERFTX=5,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,80,80,80,80,80,80,80,80,104,104,104,104,104,104,104,104", "AT+ERFTX=5,2,72,72,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,72", "AT+ERFTX=5,3,3,56", "AT+ERFTX=5,3,7,64", "AT+ERFTX=5,3,38,48", "AT+ERFTX=5,3,40,72", "AT+ERFTX=5,3,41,48"});
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1732bf_ex_PH", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.33
        {
            put("head", new String[]{"AT+ERFTX=5,3,3,24", "AT+ERFTX=5,3,7,24"});
            put("body", new String[]{"AT+ERFTX=5,2,8,8,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,8", "AT+ERFTX=5,3,3,24", "AT+ERFTX=5,3,7,28", "AT+ERFTX=5,3,40,8"});
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.34
        {
            put("head", new String[]{"AT+ERFTX=5,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,80,80,80,80,80,80,80,80,104,104,104,104,104,104,104,104", "AT+ERFTX=5,2,72,72,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,72", "AT+ERFTX=5,3,3,56", "AT+ERFTX=5,3,5,16", "AT+ERFTX=5,3,7,64", "AT+ERFTX=5,3,38,48", "AT+ERFTX=5,3,40,72", "AT+ERFTX=5,3,41,48"});
            put("body", new String[]{"AT+ERFTX=5,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,80,80,80,80,80,80,80,80,104,104,104,104,104,104,104,104", "AT+ERFTX=5,2,72,72,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,72", "AT+ERFTX=5,3,3,56", "AT+ERFTX=5,3,5,16", "AT+ERFTX=5,3,7,64", "AT+ERFTX=5,3,38,48", "AT+ERFTX=5,3,40,72", "AT+ERFTX=5,3,41,48"});
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1732bf_ex_TW", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.35
        {
            put("head", new String[]{"AT+ERFTX=5,3,3,24", "AT+ERFTX=5,3,7,24"});
            put("body", new String[]{"AT+ERFTX=5,2,8,8,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,8", "AT+ERFTX=5,3,3,24", "AT+ERFTX=5,3,7,28", "AT+ERFTX=5,3,40,8"});
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.36
        {
            put("head", new String[]{"AT+ERFTX=5,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,80,80,80,80,80,80,80,80,104,104,104,104,104,104,104,104", "AT+ERFTX=5,2,72,72,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,72", "AT+ERFTX=5,3,3,56", "AT+ERFTX=5,3,5,16", "AT+ERFTX=5,3,7,64", "AT+ERFTX=5,3,38,48", "AT+ERFTX=5,3,40,72", "AT+ERFTX=5,3,41,48"});
            put("body", new String[]{"AT+ERFTX=5,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,80,80,80,80,80,80,80,80,104,104,104,104,104,104,104,104", "AT+ERFTX=5,2,72,72,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,72", "AT+ERFTX=5,3,3,56", "AT+ERFTX=5,3,5,16", "AT+ERFTX=5,3,7,64", "AT+ERFTX=5,3,38,48", "AT+ERFTX=5,3,40,72", "AT+ERFTX=5,3,41,48"});
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1732c", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.37
        {
            put("head", new String[]{"AT+ERFTX=5,2,20,20,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,20", "AT+ERFTX=5,3,3,16", "AT+ERFTX=5,3,7,24"});
            put("body", new String[]{"AT+ERFTX=9,0,0,0,0,0,0"});
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.38
        {
            put("head", new String[]{"AT+ERFTX=5,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,80,80,80,80,80,80,80,80,104,104,104,104,104,104,104,104", "AT+ERFTX=5,2,72,72,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,72", "AT+ERFTX=5,3,3,56", "AT+ERFTX=5,3,5,16", "AT+ERFTX=5,3,7,64", "AT+ERFTX=5,3,38,48", "AT+ERFTX=5,3,40,72", "AT+ERFTX=5,3,41,48"});
            put("body", new String[]{"AT+ERFTX=9,0,0,0,0,0,0"});
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1732cf_ex", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.39
        {
            put("head", new String[]{"AT+ERFTX=5,2,20,20,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,20", "AT+ERFTX=5,3,3,16", "AT+ERFTX=5,3,7,24"});
            put("body", new String[]{"AT+ERFTX=5,2,20,20,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,28", "AT+ERFTX=5,3,3,12", "AT+ERFTX=5,3,7,28", "AT+ERFTX=5,3,40,8"});
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.40
        {
            put("head", new String[]{"AT+ERFTX=5,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,80,80,80,80,80,80,80,80,104,104,104,104,104,104,104,104", "AT+ERFTX=5,2,72,72,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,72", "AT+ERFTX=5,3,3,56", "AT+ERFTX=5,3,5,16", "AT+ERFTX=5,3,7,64", "AT+ERFTX=5,3,38,48", "AT+ERFTX=5,3,40,72", "AT+ERFTX=5,3,41,48"});
            put("body", new String[]{"AT+ERFTX=5,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,80,80,80,80,80,80,80,80,104,104,104,104,104,104,104,104", "AT+ERFTX=5,2,72,72,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,72", "AT+ERFTX=5,3,3,56", "AT+ERFTX=5,3,5,16", "AT+ERFTX=5,3,7,64", "AT+ERFTX=5,3,38,48", "AT+ERFTX=5,3,40,72", "AT+ERFTX=5,3,41,48"});
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1732cf_ex_PH", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.41
        {
            put("head", new String[]{"AT+ERFTX=5,3,3,16", "AT+ERFTX=5,3,7,24"});
            put("body", new String[]{"AT+ERFTX=5,2,8,8,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,8", "AT+ERFTX=5,3,3,16", "AT+ERFTX=5,3,7,28", "AT+ERFTX=5,3,40,8"});
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.42
        {
            put("head", new String[]{"AT+ERFTX=3,1,0,0,0,0,0,0,0,0,0,24,24,24,0,24,24,24,80,80,80,80,80,80,80,80,104,104,104,104,104,104,104,104", "AT+ERFTX=5,2,72,72,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,72", "AT+ERFTX=5,3,3,56", "AT+ERFTX=5,3,5,16", "AT+ERFTX=5,3,7,64", "AT+ERFTX=5,3,38,48", "AT+ERFTX=5,3,40,72", "AT+ERFTX=5,3,41,48"});
            put("body", new String[]{"AT+ERFTX=3,1,0,0,0,0,0,0,0,0,0,24,24,24,0,24,24,24,80,80,80,80,80,80,80,80,104,104,104,104,104,104,104,104", "AT+ERFTX=5,2,72,72,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,72", "AT+ERFTX=5,3,3,56", "AT+ERFTX=5,3,5,16", "AT+ERFTX=5,3,7,64", "AT+ERFTX=5,3,38,48", "AT+ERFTX=5,3,40,72", "AT+ERFTX=5,3,41,48"});
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1732d", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.43
        {
            put("head", new String[]{"AT+ERFTX=5,2,20,20,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,20", "AT+ERFTX=5,3,3,16", "AT+ERFTX=5,3,7,24"});
            put("body", new String[]{"AT+ERFTX=9,0,0,0,0,0,0"});
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.44
        {
            put("head", new String[]{"AT+ERFTX=5,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,80,80,80,80,80,80,80,80,104,104,104,104,104,104,104,104", "AT+ERFTX=5,2,72,72,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,72", "AT+ERFTX=5,3,3,56", "AT+ERFTX=5,3,5,16", "AT+ERFTX=5,3,7,64", "AT+ERFTX=5,3,38,48", "AT+ERFTX=5,3,40,72", "AT+ERFTX=5,3,41,48"});
            put("body", new String[]{"AT+ERFTX=9,0,0,0,0,0,0"});
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1732df_ex", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.45
        {
            put("head", new String[]{"AT+ERFTX=5,2,20,20,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,20", "AT+ERFTX=5,3,3,16", "AT+ERFTX=5,3,7,24"});
            put("body", new String[]{"AT+ERFTX=5,2,20,20,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,28", "AT+ERFTX=5,3,3,12", "AT+ERFTX=5,3,7,28", "AT+ERFTX=5,3,40,8"});
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.46
        {
            put("head", new String[]{"AT+ERFTX=3,1,0,0,0,0,0,0,0,0,0,24,24,24,0,24,24,24,80,80,80,80,80,80,80,80,104,104,104,104,104,104,104,104", "AT+ERFTX=5,2,72,72,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,72", "AT+ERFTX=5,3,3,56", "AT+ERFTX=5,3,5,16", "AT+ERFTX=5,3,7,64", "AT+ERFTX=5,3,38,48", "AT+ERFTX=5,3,40,72", "AT+ERFTX=5,3,41,48"});
            put("body", new String[]{"AT+ERFTX=3,1,0,0,0,0,0,0,0,0,0,24,24,24,0,24,24,24,80,80,80,80,80,80,80,80,104,104,104,104,104,104,104,104", "AT+ERFTX=5,2,72,72,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,72", "AT+ERFTX=5,3,3,56", "AT+ERFTX=5,3,5,16", "AT+ERFTX=5,3,7,64", "AT+ERFTX=5,3,38,48", "AT+ERFTX=5,3,40,72", "AT+ERFTX=5,3,41,48"});
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1732df_ex_PH", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.47
        {
            put("head", new String[]{"AT+ERFTX=5,3,3,16", "AT+ERFTX=5,3,7,24"});
            put("body", new String[]{"AT+ERFTX=5,2,8,8,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,8", "AT+ERFTX=5,3,3,16", "AT+ERFTX=5,3,7,28", "AT+ERFTX=5,3,40,8"});
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.48
        {
            put("head", new String[]{"AT+ERFTX=3,1,0,0,0,0,0,0,0,0,0,24,24,24,0,24,24,24,80,80,80,80,80,80,80,80,104,104,104,104,104,104,104,104", "AT+ERFTX=5,2,72,72,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,72", "AT+ERFTX=5,3,3,56", "AT+ERFTX=5,3,5,16", "AT+ERFTX=5,3,7,64", "AT+ERFTX=5,3,38,48", "AT+ERFTX=5,3,40,72", "AT+ERFTX=5,3,41,48"});
            put("body", new String[]{"AT+ERFTX=3,1,0,0,0,0,0,0,0,0,0,24,24,24,0,24,24,24,80,80,80,80,80,80,80,80,104,104,104,104,104,104,104,104", "AT+ERFTX=5,2,72,72,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,72", "AT+ERFTX=5,3,3,56", "AT+ERFTX=5,3,5,16", "AT+ERFTX=5,3,7,64", "AT+ERFTX=5,3,38,48", "AT+ERFTX=5,3,40,72", "AT+ERFTX=5,3,41,48"});
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1813", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.49
        {
            put("head", new String[]{"AT+ERFTX=10,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7,7,7,0,0,0,0,0,16,16,16,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0", "AT+ERFTX=10,2,24,24,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,23,0", "AT+ERFTX=10,3,3,24,0", "AT+ERFTX=10,3,4,16,0", "AT+ERFTX=10,3,7,4,0", "AT+ERFTX=10,3,39,12,0", "AT+ERFTX=10,3,40,4,0"});
            put("body", new String[0]);
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.50
        {
            put("head", new String[]{"AT+ERFTX=10,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,15,15,15,0,0,0,0,0,24,24,24,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0", "AT+ERFTX=10,2,32,32,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,31,0", "AT+ERFTX=10,3,3,32,0", "AT+ERFTX=10,3,4,20,0", "AT+ERFTX=10,3,7,12,0", "AT+ERFTX=10,3,39,20,0", "AT+ERFTX=10,3,40,12,0"});
            put("body", new String[0]);
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1813e", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.51
        {
            put("head", new String[]{"AT+ERFTX=10,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7,7,7,0,0,0,0,0,16,16,16,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0", "AT+ERFTX=10,2,24,24,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,23,0", "AT+ERFTX=10,3,3,24,0", "AT+ERFTX=10,3,4,16,0", "AT+ERFTX=10,3,7,4,0", "AT+ERFTX=10,3,39,12,0", "AT+ERFTX=10,3,40,4,0"});
            put("body", new String[0]);
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.52
        {
            put("head", new String[]{"AT+ERFTX=10,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,15,15,15,0,0,0,0,0,24,24,24,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0", "AT+ERFTX=10,2,32,32,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,31,0", "AT+ERFTX=10,3,3,32,0", "AT+ERFTX=10,3,4,20,0", "AT+ERFTX=10,3,7,12,0", "AT+ERFTX=10,3,39,20,0", "AT+ERFTX=10,3,40,12,0"});
            put("body", new String[0]);
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1813f_ex", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.53
        {
            put("head", new String[]{"AT+ERFTX=10,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7,7,7,0,0,0,0,0,24,24,24,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0", "AT+ERFTX=10,2,24,24,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,23,0", "AT+ERFTX=10,3,3,24,0", "AT+ERFTX=10,3,7,4,0", "AT+ERFTX=10,3,39,12,0", "AT+ERFTX=10,3,40,4,0"});
            put("body", new String[]{"AT+ERFTX=10,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,24,24,24,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0", "AT+ERFTX=10,2,20,20,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,22,0", "AT+ERFTX=10,3,3,19,0", "AT+ERFTX=10,3,39,10,0"});
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.54
        {
            put("head", new String[]{"AT+ERFTX=10,1,16,16,16,16,0,0,0,0,24,24,24,24,0,0,0,0,50,50,50,50,0,0,0,0,76,76,76,76,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0", "AT+ERFTX=10,2,65,65,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,68,0", "AT+ERFTX=10,3,3,72,0", "AT+ERFTX=10,3,5,4,0", "AT+ERFTX=10,3,7,64,0", "AT+ERFTX=10,3,8,8,0", "AT+ERFTX=10,3,38,44,0", "AT+ERFTX=10,3,39,55,0", "AT+ERFTX=10,3,40,48,0", "AT+ERFTX=10,3,41,48,0"});
            put("body", new String[]{"AT+ERFTX=10,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,32,32,32,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0", "AT+ERFTX=10,2,24,24,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,26,0", "AT+ERFTX=10,3,3,23,0", "AT+ERFTX=10,3,39,14,0"});
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1813bf_ex", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.55
        {
            put("head", new String[]{"AT+ERFTX=10,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7,7,7,0,0,0,0,0,24,24,24,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0", "AT+ERFTX=10,2,24,24,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,23,0", "AT+ERFTX=10,3,3,24,0", "AT+ERFTX=10,3,7,4,0", "AT+ERFTX=10,3,39,12,0", "AT+ERFTX=10,3,40,4,0"});
            put("body", new String[]{"AT+ERFTX=10,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,24,24,24,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0", "AT+ERFTX=10,2,20,20,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,22,0", "AT+ERFTX=10,3,3,19,0", "AT+ERFTX=10,3,39,10,0"});
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.56
        {
            put("head", new String[]{"AT+ERFTX=10,1,16,16,16,16,0,0,0,0,24,24,24,24,0,0,0,0,50,50,50,50,0,0,0,0,76,76,76,76,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0", "AT+ERFTX=10,2,65,65,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,68,0", "AT+ERFTX=10,3,3,72,0", "AT+ERFTX=10,3,5,4,0", "AT+ERFTX=10,3,7,64,0", "AT+ERFTX=10,3,8,8,0", "AT+ERFTX=10,3,38,44,0", "AT+ERFTX=10,3,39,55,0", "AT+ERFTX=10,3,40,48,0", "AT+ERFTX=10,3,41,48,0"});
            put("body", new String[]{"AT+ERFTX=10,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,32,32,32,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0", "AT+ERFTX=10,2,24,24,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,26,0", "AT+ERFTX=10,3,3,23,0", "AT+ERFTX=10,3,39,14,0"});
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1831f_ex", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.57
        {
            put("head", new String[]{"AT+ERFTX=10,1,0,8,8,8,0,8,8,8,0,4,4,4,0,4,4,4,0,4,4,4,0,4,4,4,0,4,4,4,0,4,4,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0", "AT+ERFTX=10,2,12,12,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,12,0", "AT+ERFTX=10,3,7,12,0"});
            put("body", new String[]{"AT+ERFTX=10,1,0,8,8,8,0,8,8,8,0,4,4,4,0,4,4,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,00,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0"});
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.58
        {
            put("head", new String[]{"AT+ERFTX=10,1,48,48,48,48,48,48,48,48,40,40,40,40,40,40,40,40,32,32,32,32,32,32,32,32,36,36,36,36,36,36,36,36,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0", "AT+ERFTX=10,2,56,56,0,0,,,,,24,24,,,,,24,24,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,52,0", "AT+ERFTX=10,3,3,40,0", "AT+ERFTX=10,3,5,28,0", "AT+ERFTX=10,3,7,52,0", "AT+ERFTX=10,3,8,30,0", "AT+ERFTX=10,3,20,12,0", "AT+ERFTX=10,3,38,31,0", "AT+ERFTX=10,3,40,38,0", "AT+ERFTX=10,3,41,30,0"});
            put("body", new String[]{"AT+ERFTX=10,1,12,12,12,12,12,12,12,12,8,8,8,8,8,8,8,8,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0", "AT+ERFTX=10,3,7,8,0"});
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1818c", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.59
        {
            put("head", new String[]{"AT+ERFTX=5,3,7,20"});
            put("body", new String[0]);
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.60
        {
            put("head", new String[]{"AT+ERFTX=5,3,7,24"});
            put("body", new String[0]);
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1818b", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.61
        {
            put("head", new String[]{"AT+ERFTX=5,3,7,20"});
            put("body", new String[0]);
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.62
        {
            put("head", new String[]{"AT+ERFTX=5,3,7,24"});
            put("body", new String[0]);
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1818cf_ex", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.63
        {
            put("head", new String[]{"AT+ERFTX=5,3,7,12"});
            put("body", new String[]{"AT+ERFTX=5,3,7,12"});
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.64
        {
            put("head", new String[]{"AT+ERFTX=3,1,64,64,64,64,64,64,64,64,40,40,40,40,40,40,40,40,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88", "AT+ERFTX=5,2,48,48,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,48", "AT+ERFTX=5,3,3,48", "AT+ERFTX=5,3,7,48", "AT+ERFTX=5,3,38,56", "AT+ERFTX=5,3,40,56", "AT+ERFTX=5,3,41,56"});
            put("body", new String[]{"AT+ERFTX=5,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,8,8,8,8,8,8,8,8", "AT+ERFTX=5,3,7,16"});
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1818df_ex", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.65
        {
            put("head", new String[]{"AT+ERFTX=5,3,7,12"});
            put("body", new String[]{"AT+ERFTX=5,3,7,12"});
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.66
        {
            put("head", new String[]{"AT+ERFTX=3,1,64,64,64,64,64,64,64,64,40,40,40,40,40,40,40,40,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88", "AT+ERFTX=5,2,48,48,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,48", "AT+ERFTX=5,3,3,48", "AT+ERFTX=5,3,7,48", "AT+ERFTX=5,3,38,56", "AT+ERFTX=5,3,40,56", "AT+ERFTX=5,3,41,56"});
            put("body", new String[]{"AT+ERFTX=5,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,8,8,8,8,8,8,8,8", "AT+ERFTX=5,3,7,16"});
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1818gf_ex", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.67
        {
            put("head", new String[]{"AT+ERFTX=5,3,7,12"});
            put("body", new String[]{"AT+ERFTX=5,3,7,12"});
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.68
        {
            put("head", new String[]{"AT+ERFTX=3,1,64,64,64,64,64,64,64,64,40,40,40,40,40,40,40,40,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88", "AT+ERFTX=5,2,48,48,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,48", "AT+ERFTX=5,3,3,48", "AT+ERFTX=5,3,7,48", "AT+ERFTX=5,3,38,56", "AT+ERFTX=5,3,40,56", "AT+ERFTX=5,3,41,56"});
            put("body", new String[]{"AT+ERFTX=5,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,8,8,8,8,8,8,8,8", "AT+ERFTX=5,3,7,16"});
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1818hf_ex", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.69
        {
            put("head", new String[]{"AT+ERFTX=5,3,7,12"});
            put("body", new String[]{"AT+ERFTX=5,3,7,12"});
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.70
        {
            put("head", new String[]{"AT+ERFTX=3,1,64,64,64,64,64,64,64,64,40,40,40,40,40,40,40,40,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88", "AT+ERFTX=5,2,48,48,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,", "AT+ERFTX=5,3,1,48", "AT+ERFTX=5,3,3,48", "AT+ERFTX=5,3,7,48", "AT+ERFTX=5,3,38,56", "AT+ERFTX=5,3,40,56", "AT+ERFTX=5,3,41,56"});
            put("body", new String[]{"AT+ERFTX=5,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,8,8,8,8,8,8,8,8", "AT+ERFTX=5,3,7,16"});
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1818e", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.71
        {
            put("head", new String[]{"AT+ERFTX=5,3,7,20"});
            put("body", new String[0]);
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.72
        {
            put("head", new String[]{"AT+ERFTX=5,3,7,24"});
            put("body", new String[0]);
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1831", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.73
        {
            put("head", new String[]{"AT+ERFTX=10,1,0,4,4,4,0,4,4,4,0,4,4,4,0,4,4,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0", "AT+ERFTX=10,2,12,12,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,12,0", "AT+ERFTX=10,3,7,12,0"});
            put("body", new String[0]);
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.74
        {
            put("head", new String[]{"AT+ERFTX=10,1,12,12,12,12,12,12,12,12,12,12,12,12,12,12,12,12,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0", "AT+ERFTX=10,2,16,16,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,16,0", "AT+ERFTX=10,3,3,8,0", "AT+ERFTX=10,3,7,16,0", "AT+ERFTX=10,3,38,8,0", "AT+ERFTX=10,3,40,8,0", "AT+ERFTX=10,3,41,8,0"});
            put("body", new String[0]);
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1913", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.75
        {
            put("head", new String[]{"AT+ERFTX=10,3,40,12,0"});
            put("body", new String[]{"AT+ERFTX=10,3,40,8,0"});
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.76
        {
            put("head", new String[]{"AT+ERFTX=10,1,48,48,48,48,48,48,48,48,48,48,48,48,48,48,48,48,32,32,32,32,32,32,32,32,52,52,52,52,52,52,52,52,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0", "AT+ERFTX=10,2,32,32,0,0,,,,,24,24,,,,,28,28,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,40,0", "AT+ERFTX=10,3,3,44,0", "AT+ERFTX=10,3,5,28,0", "AT+ERFTX=10,3,7,48,0", "AT+ERFTX=10,3,8,32,0", "AT+ERFTX=10,3,20,24,0", "AT+ERFTX=10,3,38,32,0", "AT+ERFTX=10,3,39,40,0", "AT+ERFTX=10,3,40,56,0", "AT+ERFTX=10,3,41,32,0"});
            put("body", new String[]{"AT+ERFTX=10,1,16,16,16,16,16,16,16,16,24,24,24,24,24,24,24,24,16,16,16,16,16,16,16,16,24,24,24,24,24,24,24,24,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0", "AT+ERFTX=10,2,24,24,0,0,,,,,16,16,,,,,24,24,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,24,0", "AT+ERFTX=10,3,3,24,0", "AT+ERFTX=10,3,5,8,0", "AT+ERFTX=10,3,7,24,0", "AT+ERFTX=10,3,8,16,0", "AT+ERFTX=10,3,38,16,0", "AT+ERFTX=10,3,39,24,0", "AT+ERFTX=10,3,40,24,0", "AT+ERFTX=10,3,41,16,0"});
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1913f_ex_AU", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.77
        {
            put("head", new String[]{"AT+ERFTX=10,2,8,8,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,8,0", "AT+ERFTX=10,3,7,32,0", "AT+ERFTX=10,3,40,16,0"});
            put("body", new String[]{"AT+ERFTX=10,2,8,8,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,8,8,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,8,8", "AT+ERFTX=10,3,7,32,0"});
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.78
        {
            put("head", new String[]{"AT+ERFTX=10,2,8,8,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,8,0", "AT+ERFTX=10,3,7,32,0", "AT+ERFTX=10,3,40,16,0"});
            put("body", new String[]{"AT+ERFTX=10,2,8,8,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,8,8,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=10,3,1,8,8", "AT+ERFTX=10,3,7,32,0"});
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1901f_ex_IN", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.79
        {
            put("head", new String[]{"AT+ERFTX=5,2,8,8,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=5,3,1,8", "AT+ERFTX=5,3,7,24"});
            put("body", new String[]{"AT+ERFTX=5,2,8,8,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=5,3,1,8", "AT+ERFTX=5,3,7,16"});
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.80
        {
            put("head", new String[]{"AT+ERFTX=3,1,40,40,40,40,40,40,40,40,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88", "AT+ERFTX=5,2,72,72,40,40,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=5,3,1,72", "AT+ERFTX=5,3,3,40", "AT+ERFTX=5,3,7,72", "AT+ERFTX=5,3,8,24", "AT+ERFTX=5,3,34,40", "AT+ERFTX=5,3,38,40", "AT+ERFTX=5,3,39,40", "AT+ERFTX=5,3,40,72", "AT+ERFTX=5,3,41,40"});
            put("body", new String[]{"AT+ERFTX=3,1,0,0,0,0,0,0,0,0,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,32,0,0,0,0,0,0,0,0", "AT+ERFTX=5,2,24,24,0,0,0,0,0,0,24,24,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=5,3,1,24", "AT+ERFTX=5,3,3,8", "AT+ERFTX=5,3,7,24"});
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1901f_ex_PH", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.81
        {
            put("head", new String[]{"AT+ERFTX=3,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,16,16,16,16,16,16,16,16,0,0,0,0,0,0,0,0", "AT+ERFTX=5,2,8,8,,,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=5,3,1,8", "AT+ERFTX=5,3,7,16"});
            put("body", new String[]{"AT+ERFTX=3,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,12,12,12,12,12,12,12,12,0,0,0,0,0,0,0,0", "AT+ERFTX=5,2,4,4,,,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=5,3,1,4", "AT+ERFTX=5,3,7,8"});
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.82
        {
            put("head", new String[]{"AT+ERFTX=3,1,40,40,40,40,40,40,40,40,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88", "AT+ERFTX=5,2,72,72,40,40,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=5,3,1,72", "AT+ERFTX=5,3,3,40", "AT+ERFTX=5,3,7,72", "AT+ERFTX=5,3,8,24", "AT+ERFTX=5,3,34,40", "AT+ERFTX=5,3,38,40", "AT+ERFTX=5,3,39,40", "AT+ERFTX=5,3,40,72", "AT+ERFTX=5,3,41,40"});
            put("body", new String[]{"AT+ERFTX=3,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,32,32,32,32,32,32,32,32,40,40,40,40,40,40,40,40", "AT+ERFTX=5,2,40,40,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=5,3,1,40", "AT+ERFTX=5,3,3,16", "AT+ERFTX=5,3,7,32", "AT+ERFTX=5,3,38,16", "AT+ERFTX=5,3,41,16"});
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}), new ConfigList("pd1901df_ex_HK", new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.83
        {
            put("head", new String[]{"AT+ERFTX=3,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,8,8,8,8,8,8,8,8,0,0,0,0,0,0,0,0", "AT+ERFTX=5,2,4,4,,,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=5,3,1,8", "AT+ERFTX=5,3,3,8", "AT+ERFTX=5,3,7,4"});
            put("body", new String[0]);
        }
    }, new HashMap<String, String[]>() { // from class: com.vivo.services.sarpower.CommandConfig.84
        {
            put("head", new String[]{"AT+ERFTX=3,1,40,40,40,40,40,40,40,40,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88,88", "AT+ERFTX=5,2,72,72,40,40,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=5,3,1,72", "AT+ERFTX=5,3,3,40", "AT+ERFTX=5,3,7,72", "AT+ERFTX=5,3,8,24", "AT+ERFTX=5,3,34,40", "AT+ERFTX=5,3,38,40", "AT+ERFTX=5,3,39,40", "AT+ERFTX=5,3,40,72", "AT+ERFTX=5,3,41,40"});
            put("body", new String[]{"AT+ERFTX=3,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,32,32,32,32,32,32,32,32,40,40,40,40,40,40,40,40", "AT+ERFTX=5,2,40,40,0,0,,,,,0,0,,,,,0,0,,,,,,,,,,,,,,,,,,,,,,,,,0,0,0,0,,,,,0,0,,,,,0,0", "AT+ERFTX=5,3,1,40", "AT+ERFTX=5,3,3,16", "AT+ERFTX=5,3,7,32", "AT+ERFTX=5,3,38,16", "AT+ERFTX=5,3,41,16"});
        }
    }, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK}, new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK})};
    public String[] mSarCommandsHead = {"AT+ERFTX=9,16,16,16,32,32,32"};
    public String[] mSarCommandsBody = {"AT+ERFTX=9,16,16,16,32,32,32"};
    public String[] mSarCommandsWhiteHead = {"AT+ERFTX=9,16,16,16,32,32,32"};
    public String[] mSarCommandsWhiteBody = {"AT+ERFTX=9,16,16,16,32,32,32"};
    public String[] mSarCommandsOnC2K = {"AT+ERFTX=4,8,0,8,16"};
    public String[] mSarCommandsOnC2KWhite = {"AT+ERFTX=4,8,0,8,16"};
    public String[] mCameraSarCommandsHead = {"AT+ERFTX=9,16,16,16,32,32,32"};
    public String[] mCameraSarCommandsBody = {"AT+ERFTX=9,16,16,16,32,32,32"};
    public String[] mCameraSarCommandsWhiteHead = {"AT+ERFTX=9,16,16,16,32,32,32"};
    public String[] mCameraSarCommandsWhiteBody = {"AT+ERFTX=9,16,16,16,32,32,32"};
    public String[] mCameraSarCommandsOnC2K = {"AT+ERFTX=4,8,0,8,16"};
    public String[] mCameraSarCommandsOnC2KWhite = {"AT+ERFTX=4,8,0,8,16"};
    public String mResetGSM = null;
    public String mResetC2K = null;
    private boolean mParseFinished = false;
    private boolean mBootCompleted = false;
    private boolean mUpdateParameterCommandSent = false;
    private final IntentFilter mBootCompleteFilter = new IntentFilter("android.intent.action.BOOT_COMPLETED");
    private Runnable mRegisterReceiverRunnable = new Runnable() { // from class: com.vivo.services.sarpower.CommandConfig.86
        @Override // java.lang.Runnable
        public void run() {
            CommandConfig.mContext.registerReceiver(CommandConfig.this.mPhoneStateReceiver, CommandConfig.this.mBootCompleteFilter);
        }
    };
    private final BroadcastReceiver mPhoneStateReceiver = new BroadcastReceiver() { // from class: com.vivo.services.sarpower.CommandConfig.87
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            String action = intent.getAction();
            SElog.d(CommandConfig.TAG, "mPsReceiver action:" + action);
            if (action.equals("android.intent.action.BOOT_COMPLETED")) {
                CommandConfig.this.mBootCompleted = true;
                if (CommandConfig.this.mParseFinished && !CommandConfig.this.mUpdateParameterCommandSent) {
                    CommandConfig.this.sendIntentLocked();
                    CommandConfig.this.mUpdateParameterCommandSent = true;
                }
            }
        }
    };

    private static String getModelWithCountryCode(String model2) {
        String modelWithCountryCode = model2;
        if (!isOverseas || mCountryCode.equals("N")) {
            return modelWithCountryCode;
        }
        if (model2.equals("pd1732f_ex") && (mCountryCode.equals("PH") || mCountryCode.equals("RU"))) {
            modelWithCountryCode = modelWithCountryCode + "_" + mCountryCode;
        }
        if (model2.equals("pd1732bf_ex") && (mCountryCode.equals("PH") || mCountryCode.equals("RU") || mCountryCode.equals("TW"))) {
            modelWithCountryCode = modelWithCountryCode + "_" + mCountryCode;
        }
        if (model2.equals("pd1732cf_ex") && mCountryCode.equals("PH")) {
            modelWithCountryCode = modelWithCountryCode + "_" + mCountryCode;
        }
        if (model2.equals("pd1732df_ex") && mCountryCode.equals("PH")) {
            return modelWithCountryCode + "_" + mCountryCode;
        }
        return modelWithCountryCode;
    }

    private boolean isUnderFactoryMode() {
        return SystemProperties.get("persist.sys.factory.mode", "no").equals("yes");
    }

    private static boolean isPD1732D() {
        try {
            FileInputStream mInputStream = new FileInputStream(BOARD_VERSION);
            byte[] buf = new byte[100];
            int len = mInputStream.read(buf);
            String board_version = new String(buf, 0, len);
            SElog.e(TAG, "borad version: " + board_version + " len: " + len);
            char[] temp = board_version.toCharArray();
            mInputStream.close();
            if (temp[2] != '0') {
                return false;
            }
            return true;
        } catch (Exception e) {
            System.out.println(e);
            return false;
        }
    }

    private static String getBoardVersionChanged(String model2) {
        String tempModel = model2;
        if (model2.equals("pd1732") && isPD1732D()) {
            tempModel = "pd1732c";
        }
        if (model2.equals("pd1732f_ex") && isPD1732D()) {
            tempModel = "pd1732cf_ex";
        }
        if (model2.startsWith("pd1901")) {
            tempModel = "pd1901f_ex_IN";
            if (model2.equals("pd1901f_ex")) {
                if (mCountryCode.equals("PH") || mCountryCode.equals("EG") || mCountryCode.equals("MA") || mCountryCode.equals("RU")) {
                    tempModel = "pd1901f_ex_PH";
                }
            } else if (model2.equals("pd1901bf_ex")) {
                if (mCountryCode.equals("TW") || mCountryCode.equals("RU")) {
                    tempModel = "pd1901f_ex_PH";
                }
            } else if (model2.equals("pd1901df_ex")) {
                if (mCountryCode.equals("HK")) {
                    tempModel = "pd1901df_ex_HK";
                }
            } else {
                tempModel = "pd1901f_ex_IN";
            }
        }
        if (model2.equals("pd1913")) {
            return "pd1913";
        }
        if (model2.startsWith("pd1913")) {
            if (!mCountryCode.equals("IN") && !mCountryCode.equals("MM") && !mCountryCode.equals("RU") && !mCountryCode.equals("PH") && !mCountryCode.equals("ID") && !mCountryCode.equals("UA") && !mCountryCode.equals("TW") && !mCountryCode.equals("F_PH")) {
                return "pd1913f_ex_AU";
            }
            return "pd1913";
        }
        return tempModel;
    }

    private Object[] parseCommandForCamera() {
        String[] strArr = {"AT+ERFTX=9,16,16,16,32,32,32"};
        String[] strArr2 = {"AT+ERFTX=9,16,16,16,32,32,32"};
        String[] strArr3 = {"AT+ERFTX=9,16,16,16,32,32,32"};
        String[] strArr4 = {"AT+ERFTX=9,16,16,16,32,32,32"};
        String[] strArr5 = {"AT+ERFTX=4,8,0,8,16"};
        String[] strArr6 = {"AT+ERFTX=4,8,0,8,16"};
        String[] specificConfigHead = mCameraConfigs.commandsHead;
        String[] specificConfigBody = mCameraConfigs.commandsBody;
        String[] wspecificConfigHead = mCameraConfigs.wcommandsHead;
        String[] wspecificConfigBody = mCameraConfigs.wcommandsBody;
        String[] specificConfigOnC2K = mCameraConfigs.commandsOnC2K;
        String[] wspecificConfigOnC2K = mCameraConfigs.wcommandsOnC2K;
        SElog.e(TAG, "get it, return project  's specific command config : " + specificConfigHead.toString());
        return new Object[]{specificConfigHead, specificConfigBody, wspecificConfigHead, wspecificConfigBody, specificConfigOnC2K, wspecificConfigOnC2K};
    }

    private Object[] parseCommandByProject() {
        String[] specificConfigHead = {"AT+ERFTX=9,16,16,16,32,32,32"};
        String[] specificConfigBody = {"AT+ERFTX=9,16,16,16,32,32,32"};
        String[] wspecificConfigHead = {"AT+ERFTX=9,16,16,16,32,32,32"};
        String[] wspecificConfigBody = {"AT+ERFTX=9,16,16,16,32,32,32"};
        String[] specificConfigOnC2K = {"AT+ERFTX=4,8,0,8,16"};
        String[] wspecificConfigOnC2K = {"AT+ERFTX=4,8,0,8,16"};
        String mModel = model;
        SElog.e(TAG, "isSeas= " + isOverseas + ", mCC = " + mCountryCode + ", model = " + mModel);
        if (!isUnderFactoryMode()) {
            mModel = getModelWithCountryCode(model);
        }
        SElog.e(TAG, " model with cc = " + mModel);
        String mModel2 = getBoardVersionChanged(mModel);
        SElog.e(TAG, "after modify  model = " + mModel2);
        ConfigList[] configListArr = mConfigs;
        if (configListArr != null) {
            if (configListArr.length > 0) {
                int i = 0;
                while (true) {
                    ConfigList[] configListArr2 = mConfigs;
                    if (i >= configListArr2.length) {
                        break;
                    } else if (!mModel2.equals(configListArr2[i].model)) {
                        i++;
                    } else {
                        String[] specificConfigHead2 = mConfigs[i].commandsHead;
                        String[] specificConfigBody2 = mConfigs[i].commandsBody;
                        String[] wspecificConfigHead2 = mConfigs[i].wcommandsHead;
                        String[] wspecificConfigBody2 = mConfigs[i].wcommandsBody;
                        String[] specificConfigOnC2K2 = mConfigs[i].commandsOnC2K;
                        String[] wspecificConfigOnC2K2 = mConfigs[i].wcommandsOnC2K;
                        SElog.e(TAG, "get it, return project " + mModel2 + " 's specific command config : " + specificConfigHead2.toString());
                        return new Object[]{specificConfigHead2, specificConfigBody2, wspecificConfigHead2, wspecificConfigBody2, specificConfigOnC2K2, wspecificConfigOnC2K2};
                    }
                }
            } else {
                SElog.e(TAG, "mConfigs is empty, return default command config : " + specificConfigHead.toString());
            }
        } else {
            SElog.e(TAG, "mConfigs is null! return default command config : " + specificConfigHead.toString());
        }
        return new Object[]{specificConfigHead, specificConfigBody, wspecificConfigHead, wspecificConfigBody, specificConfigOnC2K, wspecificConfigOnC2K};
    }

    public CommandConfig(Looper looper, Context contxt) {
        mContext = contxt;
        Handler handler = new Handler(looper);
        this.mStateChangeHandler = handler;
        handler.post(this.mRegisterReceiverRunnable);
        ConfigList2Parser configList2Parser = new ConfigList2Parser(SAR_CONFIG_PATH, looper, new ConfigList2Parser.ParseListener() { // from class: com.vivo.services.sarpower.CommandConfig.85
            @Override // com.vivo.services.sarpower.ConfigList2Parser.ParseListener
            public void onParseFinished() {
                CommandConfig.this.mParseFinished = true;
                SElog.d(CommandConfig.TAG, "onParseFinished");
                CommandConfig.this.updateSarCommands();
                CommandConfig.this.updateSarCommands4Camera();
                if (CommandConfig.this.mBootCompleted && !CommandConfig.this.mUpdateParameterCommandSent) {
                    CommandConfig.this.sendIntentLocked();
                    CommandConfig.this.mUpdateParameterCommandSent = true;
                }
            }
        });
        this.mConfigList2Parser = configList2Parser;
        configList2Parser.startParse();
        updateSarCommands();
        updateSarCommands4Camera();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendIntentLocked() {
        Intent intent = new Intent(VIVO_MTK_SAR_CONFIG_PARSED);
        mContext.sendBroadcast(intent);
    }

    public void updateSarCommands() {
        ConfigList conf = null;
        if (this.mParseFinished) {
            conf = this.mConfigList2Parser.getConfigList(model, mCountryCode, isUnderFactoryMode() ? 2 : 1);
        }
        if (conf != null) {
            this.mSarCommandsHead = conf.commandsHead;
            this.mSarCommandsBody = conf.commandsBody;
            this.mSarCommandsWhiteHead = conf.wcommandsHead;
            this.mSarCommandsWhiteBody = conf.wcommandsBody;
            this.mSarCommandsOnC2K = conf.commandsOnC2K;
            this.mSarCommandsOnC2KWhite = conf.wcommandsOnC2K;
            this.mResetGSM = conf.resetGSM;
            this.mResetC2K = conf.resetC2K;
            SElog.d(TAG, "updateSarCommands found conf by xml");
            return;
        }
        Object[] commandset = parseCommandByProject();
        this.mSarCommandsHead = (String[]) commandset[0];
        this.mSarCommandsBody = (String[]) commandset[1];
        this.mSarCommandsWhiteHead = (String[]) commandset[2];
        this.mSarCommandsWhiteBody = (String[]) commandset[3];
        this.mSarCommandsOnC2K = (String[]) commandset[4];
        this.mSarCommandsOnC2KWhite = (String[]) commandset[5];
        this.mResetGSM = null;
        this.mResetC2K = null;
        SElog.d(TAG, "updateSarCommands4Camera found conf");
    }

    public void updateSarCommands4Camera() {
        ConfigList conf = null;
        if (this.mParseFinished) {
            conf = this.mConfigList2Parser.getConfigList(model, mCountryCode, 4);
        }
        if (conf != null) {
            this.mCameraSarCommandsHead = conf.commandsHead;
            this.mCameraSarCommandsBody = conf.commandsBody;
            this.mCameraSarCommandsWhiteHead = conf.wcommandsHead;
            this.mCameraSarCommandsWhiteBody = conf.wcommandsBody;
            this.mCameraSarCommandsOnC2K = conf.commandsOnC2K;
            this.mCameraSarCommandsOnC2KWhite = conf.wcommandsOnC2K;
            SElog.d(TAG, "updateSarCommands4Camera found conf by xml");
            return;
        }
        Object[] commandset = parseCommandForCamera();
        this.mCameraSarCommandsHead = (String[]) commandset[0];
        this.mCameraSarCommandsBody = (String[]) commandset[1];
        this.mCameraSarCommandsWhiteHead = (String[]) commandset[2];
        this.mCameraSarCommandsWhiteBody = (String[]) commandset[3];
        this.mCameraSarCommandsOnC2K = (String[]) commandset[4];
        this.mCameraSarCommandsOnC2KWhite = (String[]) commandset[5];
        SElog.d(TAG, "updateSarCommands4Camera found conf");
    }

    public void dump(PrintWriter pw) {
        pw.println("---- ConfigList2 ----");
        this.mConfigList2Parser.dump(pw);
        pw.println("---- ConfigList2 end ----");
        pw.println();
    }
}