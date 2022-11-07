package com.vivo.services.sensorhub;

import android.os.SystemProperties;
import com.vivo.common.utils.VLog;
import com.vivo.sensor.calibration.ProximityCaliConfigParser;
import com.vivo.sensor.implement.VivoSensorImpl;
import com.vivo.sensor.sensoroperate.SensorTestResult;
import com.vivo.sensor.sensoroperate.SensorTestResultCallback;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public abstract class VivoSensorHubController {
    private static final String ACC_CALI_OFFSET_FLAG = "persist.sys.gs_cal_flag";
    private static final int ACC_SET_OFFSET = 1536;
    private static final String BASE_THRESHOLD_SENSOR = "persist.sys.base_threshold_prox";
    private static final String BASE_THRESHOLD_SENSOR_B = "persist.sys.base_threshold_prox_b";
    private static final String BASE_THRESHOLD_SENSOR_LOW = "persist.sys.base_threshold_prox_low";
    private static final String BASE_THRESHOLD_SENSOR_SECOND = "persist.sys.ps_cali_data_short";
    private static final String BASE_THRESHOLD_SENSOR_SECOND_B = "persist.sys.ps_cali_data_short_b";
    private static final String CCT_CALI_FACTOR_B = "persist.sys.cali.factor_b";
    private static final String CCT_CALI_FACTOR_C = "persist.sys.cali.factor_c";
    private static final String CCT_CALI_FACTOR_G = "persist.sys.cali.factor_g";
    private static final String CCT_CALI_FACTOR_R = "persist.sys.cali.factor_r";
    private static final String CCT_CALI_FACTOR_WB = "persist.sys.cali.factor_w";
    private static final String CCT_CALI_OFFSET_FLAG = "persist.sys.cct_cali_flag";
    private static final String GYRO_CALI_OFFSET_FLAG = "persist.sys.gyroscope_cal_flag";
    private static final int GYRO_SET_OFFSET = 1537;
    protected static final int MSG_NOTIFY_ENG_MODE = 1;
    protected static final int MSG_UPDATE_SENSOR_CALI = 0;
    private static final int POST_CCT_SET_OFFSET = 1539;
    private static final int PRE_CCT_SET_OFFSET = 1538;
    private static final String PROX_BASE_VALUE = "persist.vendor.ps_base_value_new";
    private static final int PROX_B_SET_CALI_OFFSET_DATA = 837;
    private static final String PS_CALI_FLAG = "persist.sys.ps_cali_flag";
    private static final String PS_CALI_FLAG_B = "persist.sys.ps_cali_flag_b";
    private static final String PS_CALI_FLAG_LOW = "persist.sys.ps_cali_flag_low";
    private static final String PS_CALI_OFFSET_DATA_H = "persist.sys.ps_cali_offset_data0";
    private static final String PS_CALI_OFFSET_FLAG = "persist.sys.ps_offset";
    private static final int PS_DRIVER_TEMP_CALI_BACK = 839;
    private static final int PS_NOTIFY_ENG_MODE = 530;
    private static final String PS_SECOND_TEMP_CALI_DATA = "persist.vendor.ps_second_temp_cali_data";
    private static final String PS_SECOND_TEMP_CALI_OFFSET = "persist.vendor.ps_second_temp_cali_offset";
    private static final int PS_SET_CALI_OFFSET_DATA = 524;
    private static final int PS_SET_ENG_CALI_DATA = 520;
    private static final int PS_SET_ENG_CALI_DATA_BACK = 834;
    private static final int PS_SET_ENG_CALI_DATA_FRONT_LOW = 529;
    private static final int SENSOR_COMMAND_PROXIMITY_ENG_MODE = 2063;
    private static final int SENSOR_COMMAND_SET_PS_CALI_DATA = 22;
    private static final int SENSOR_COMMAND_SET_PS_CALI_OFFSET_DATA = 24;
    protected static final String TAG = "VivoSensorHubService";
    private static final String Under_BASE_THRESHOLD_SENSOR = "persist.sys.base_threshold_prox_under";
    private static final String Under_BASE_THRESHOLD_SENSOR_SHORT = "persist.sys.ps_cali_data_short_under";
    private static final String Under_PS_CALI_FLAG = "persist.sys.ps_cali_flag_under";
    private static final String platform = SystemProperties.get("ro.vivo.product.solution", "unkown").toLowerCase();
    private static final String factoryMode = SystemProperties.get("persist.sys.factory.mode", "no");
    protected VivoSensorImpl mVivoSensorImpl = null;
    private int mPsOffset = -1;
    public SensorTestResultCallback mSensorTestResultCallback = new SensorTestResultCallback() { // from class: com.vivo.services.sensorhub.VivoSensorHubController.1
        public void operateResult(SensorTestResult result) {
            VLog.d(VivoSensorHubController.TAG, "operateResult Enter mPsOffset " + VivoSensorHubController.this.mPsOffset + " ,result " + result.mTestVal[0]);
            if (VivoSensorHubController.this.mPsOffset != 1 && VivoSensorHubController.this.mPsOffset != ((int) result.mTestVal[0]) && ProximityCaliConfigParser.isSectionCaliWriteBackToNv()) {
                int psoffset = (int) result.mTestVal[0];
                NVItemSocketClient socketClient = new NVItemSocketClient();
                String updateMsg = "ps_cali_offset_nv " + psoffset;
                VLog.d(VivoSensorHubController.TAG, "operateResult offset " + psoffset);
                try {
                    socketClient.sendMessage(updateMsg);
                } catch (Exception e) {
                    VLog.e(VivoSensorHubController.TAG, "Fail to write back psCalioffset ");
                }
            }
        }
    };

    public abstract void handleSensorHubMessage(int i, long j);

    /* JADX INFO: Access modifiers changed from: protected */
    public void sendCaliDataToDriver(long delay) {
        if (this.mVivoSensorImpl == null) {
            VLog.e(TAG, "mVivoSensorImpl is null.");
            return;
        }
        sendProxiCaliData(delay);
        sendAccGyroCaliData(delay);
        if (ProximityCaliConfigParser.isUseCCTCali()) {
            sendCctCaliData(delay);
        }
        if (ProximityCaliConfigParser.isUseMagCali()) {
            sendMagCaliData(delay);
        }
    }

    private void sendProxiCaliData(long delay) {
        sendOpticsCaliData(delay);
        sendSectionCaliData(delay);
        if (ProximityCaliConfigParser.isDoubleUnderProx()) {
            sendDoubleUnderProxCaliData(delay);
        } else if (ProximityCaliConfigParser.isSingleUnderProx()) {
            sendSingleUnderProxCaliData(delay);
        } else if (ProximityCaliConfigParser.isDoubleScreenUnderProx()) {
            sendDoubleScreenUnderProxCaliData(delay);
        } else {
            sendMicroProxCaliData(delay);
        }
    }

    private void sendOpticsCaliData(long delay) {
        if (ProximityCaliConfigParser.isUseOpticsCali()) {
            SensorTestResult tempRes = new SensorTestResult();
            int[] tempTestArg = {24, 24, -1};
            int psDoubleCaliFlag = SystemProperties.getInt(PS_CALI_FLAG_B, 0);
            int psSingleCaliFlag = SystemProperties.getInt(Under_PS_CALI_FLAG, 0);
            if (ProximityCaliConfigParser.isDoubleUnderProx() && psDoubleCaliFlag == 1) {
                tempTestArg[2] = SystemProperties.getInt(PS_CALI_OFFSET_FLAG, 0);
                this.mVivoSensorImpl.vivoSensorTest((int) PROX_B_SET_CALI_OFFSET_DATA, tempRes, tempTestArg, delay);
            } else if (ProximityCaliConfigParser.isSingleUnderProx() && psSingleCaliFlag == 1) {
                if (platform.equals("qcom")) {
                    tempTestArg[2] = SystemProperties.getInt(PS_CALI_OFFSET_DATA_H, 0);
                } else {
                    int offsetValue = SystemProperties.getInt(PS_CALI_OFFSET_FLAG, 0);
                    int newOffset = SystemProperties.getInt(PS_SECOND_TEMP_CALI_OFFSET, 0);
                    if (newOffset != 0) {
                        VLog.d(TAG, "use new cali_off_data: " + newOffset + ", old data: " + offsetValue);
                        offsetValue = newOffset;
                    }
                    tempTestArg[2] = offsetValue;
                }
                this.mVivoSensorImpl.vivoSensorTest((int) PS_SET_CALI_OFFSET_DATA, tempRes, tempTestArg, delay);
            }
            VLog.d(TAG, "proximity optics offsetValue " + tempTestArg[2]);
        }
    }

    private void sendSectionCaliData(long delay) {
        int psCaliFlag = SystemProperties.getInt(PS_CALI_FLAG, 0);
        if (ProximityCaliConfigParser.isUseSectionCali() && psCaliFlag == 1) {
            SensorTestResult tempRes = new SensorTestResult();
            int[] tempTestArg = {24, 24, SystemProperties.getInt(PS_CALI_OFFSET_FLAG, 0)};
            this.mPsOffset = tempTestArg[2];
            this.mVivoSensorImpl.vivoSensorTest((int) PS_SET_CALI_OFFSET_DATA, tempRes, tempTestArg, delay, this.mSensorTestResultCallback);
            VLog.d(TAG, "proximity section offsetValue " + tempTestArg[2]);
        }
    }

    private void sendDoubleUnderProxCaliData(long delay) {
        int i;
        int psCaliFlag_low = SystemProperties.getInt(PS_CALI_FLAG_LOW, 0);
        int psCaliFlag = SystemProperties.getInt(PS_CALI_FLAG_B, 0);
        int longBaseValue_Low = SystemProperties.getInt(BASE_THRESHOLD_SENSOR_LOW, ProximityCaliConfigParser.getDefaultBaseLowValue());
        int longBaseValueFront = SystemProperties.getInt(Under_BASE_THRESHOLD_SENSOR, ProximityCaliConfigParser.getDefaultLongBaseValueFrontValue());
        int longBaseValueBack = SystemProperties.getInt(BASE_THRESHOLD_SENSOR_B, ProximityCaliConfigParser.getDefaultLongBaseValueBackValue());
        int shortBaseValueFront = SystemProperties.getInt(Under_BASE_THRESHOLD_SENSOR_SHORT, ProximityCaliConfigParser.getDefaultShortBaseValueFrontValue());
        int shortBaseValueBack = SystemProperties.getInt(BASE_THRESHOLD_SENSOR_SECOND_B, ProximityCaliConfigParser.getDefaultShortBaseValueBackValue());
        if (psCaliFlag != 1) {
            i = 1;
        } else {
            SensorTestResult tempResFront = new SensorTestResult();
            int[] tempTestArgFront = {22, longBaseValueFront, shortBaseValueFront};
            this.mVivoSensorImpl.vivoSensorTest(520, tempResFront, tempTestArgFront, delay);
            VLog.d(TAG, "proximity cali_data front: data[0]" + tempTestArgFront[0] + " long " + tempTestArgFront[1] + " short " + tempTestArgFront[2]);
            SensorTestResult tempResBack = new SensorTestResult();
            int[] tempTestArgBack = {22, longBaseValueBack, shortBaseValueBack};
            this.mVivoSensorImpl.vivoSensorTest((int) PS_SET_ENG_CALI_DATA_BACK, tempResBack, tempTestArgBack, delay);
            StringBuilder sb = new StringBuilder();
            sb.append("proximity cali_data back: data[0]");
            sb.append(tempTestArgBack[0]);
            sb.append(" long ");
            i = 1;
            sb.append(tempTestArgBack[1]);
            sb.append(" short ");
            sb.append(tempTestArgBack[2]);
            VLog.d(TAG, sb.toString());
        }
        if (psCaliFlag_low == i) {
            int[] tempTestArgLow = new int[3];
            SensorTestResult tempResLow = new SensorTestResult();
            tempTestArgLow[0] = 22;
            tempTestArgLow[i] = longBaseValue_Low;
            tempTestArgLow[2] = 0;
            this.mVivoSensorImpl.vivoSensorTest((int) PS_SET_ENG_CALI_DATA_FRONT_LOW, tempResLow, tempTestArgLow, delay);
            VLog.d(TAG, "proximity cali_data front low : data[0]" + tempTestArgLow[0] + " long " + tempTestArgLow[1]);
        }
    }

    private void sendSingleUnderProxCaliData(long delay) {
        int psCaliFlag = SystemProperties.getInt(Under_PS_CALI_FLAG, 0);
        int psCaliFlag_low = SystemProperties.getInt(PS_CALI_FLAG_LOW, 0);
        int longBaseValue = SystemProperties.getInt(Under_BASE_THRESHOLD_SENSOR, ProximityCaliConfigParser.getDefaultLongBaseValueFrontValue());
        int shortBaseValue = SystemProperties.getInt(Under_BASE_THRESHOLD_SENSOR_SHORT, ProximityCaliConfigParser.getDefaultShortBaseValueFrontValue());
        int longBaseValue_Low = SystemProperties.getInt(BASE_THRESHOLD_SENSOR_LOW, ProximityCaliConfigParser.getDefaultBaseLowValue());
        if (psCaliFlag == 1) {
            SensorTestResult tempRes = new SensorTestResult();
            int[] tempTestArg = {22, longBaseValue, shortBaseValue};
            if (!platform.equals("qcom")) {
                int newCaliData = SystemProperties.getInt(PS_SECOND_TEMP_CALI_DATA, 0);
                if (newCaliData != 0) {
                    VLog.d(TAG, "use new cali_data: " + newCaliData);
                    tempTestArg[1] = newCaliData;
                }
                int baseValue = SystemProperties.getInt(PROX_BASE_VALUE, 0);
                if (baseValue != 0) {
                    VLog.d(TAG, "baseValue: " + baseValue);
                    tempTestArg[2] = baseValue;
                }
            }
            this.mVivoSensorImpl.vivoSensorTest(520, tempRes, tempTestArg, delay);
            VLog.d(TAG, "proximity cali_data front: data[0]" + tempTestArg[0] + " long " + tempTestArg[1] + " short " + tempTestArg[2]);
        }
        if (psCaliFlag_low == 1) {
            SensorTestResult tempResLow = new SensorTestResult();
            int[] tempTestArgLow = {22, longBaseValue_Low, 0};
            this.mVivoSensorImpl.vivoSensorTest((int) PS_SET_ENG_CALI_DATA_FRONT_LOW, tempResLow, tempTestArgLow, delay);
            VLog.d(TAG, "proximity cali_data front low : data[0]" + tempTestArgLow[0] + " long " + tempTestArgLow[1]);
        }
    }

    private void sendDoubleScreenUnderProxCaliData(long delay) {
        int psCaliFlag_Front = SystemProperties.getInt(Under_PS_CALI_FLAG, 0);
        int psCaliFlag_Back = SystemProperties.getInt(PS_CALI_FLAG_B, 0);
        int longBaseValueFront = SystemProperties.getInt(Under_BASE_THRESHOLD_SENSOR, ProximityCaliConfigParser.getDefaultLongBaseValueFrontValue());
        int longBaseValueBack = SystemProperties.getInt(BASE_THRESHOLD_SENSOR_B, ProximityCaliConfigParser.getDefaultLongBaseValueBackValue());
        int shortBaseValueFront = SystemProperties.getInt(Under_BASE_THRESHOLD_SENSOR_SHORT, ProximityCaliConfigParser.getDefaultShortBaseValueFrontValue());
        int shortBaseValueBack = SystemProperties.getInt(BASE_THRESHOLD_SENSOR_SECOND_B, ProximityCaliConfigParser.getDefaultShortBaseValueBackValue());
        if (psCaliFlag_Front == 1) {
            SensorTestResult tempResFront = new SensorTestResult();
            int[] tempTestArgFront = {22, longBaseValueFront, shortBaseValueFront};
            this.mVivoSensorImpl.vivoSensorTest(520, tempResFront, tempTestArgFront, delay);
            VLog.d(TAG, "proximity cali_data front: data[0]" + tempTestArgFront[0] + " long " + tempTestArgFront[1] + " short " + tempTestArgFront[2]);
        }
        if (psCaliFlag_Back == 1) {
            SensorTestResult tempResBack = new SensorTestResult();
            int[] tempTestArgBack = {22, longBaseValueBack, shortBaseValueBack};
            this.mVivoSensorImpl.vivoSensorTest((int) PS_SET_ENG_CALI_DATA_BACK, tempResBack, tempTestArgBack, delay);
            VLog.d(TAG, "proximity cali_data back: data[0]" + tempTestArgBack[0] + " long " + tempTestArgBack[1] + " short " + tempTestArgBack[2]);
        }
    }

    private void sendMicroProxCaliData(long delay) {
        int psCaliFlag = SystemProperties.getInt(PS_CALI_FLAG, 0);
        if (psCaliFlag == 1) {
            int[] tempTestArg = new int[3];
            SensorTestResult tempRes = new SensorTestResult();
            if (ProximityCaliConfigParser.isSendPsOffset()) {
                int[] tempTestArgFlag = new int[3];
                SensorTestResult tempResFlag = new SensorTestResult();
                int offsetFlag = SystemProperties.getInt(PS_CALI_OFFSET_FLAG, 0);
                if (offsetFlag == 1 || offsetFlag == ProximityCaliConfigParser.getExpectPsOffsetValue()) {
                    tempTestArgFlag[0] = 24;
                    tempTestArgFlag[1] = offsetFlag;
                    tempTestArgFlag[2] = 0;
                    this.mVivoSensorImpl.vivoSensorTest((int) PS_SET_CALI_OFFSET_DATA, tempResFlag, tempTestArgFlag, delay);
                    VLog.d(TAG, "proximity cali_off_data: data[0]" + tempTestArgFlag[0] + " offsetFlag " + tempTestArgFlag[1]);
                } else {
                    VLog.d(TAG, "proximity no need to send cali type to driver");
                }
            }
            int BaseValue = SystemProperties.getInt(BASE_THRESHOLD_SENSOR, ProximityCaliConfigParser.getDefaultBaseValue());
            tempTestArg[0] = 22;
            tempTestArg[1] = BaseValue;
            tempTestArg[2] = 0;
            this.mVivoSensorImpl.vivoSensorTest(520, tempRes, tempTestArg, delay);
            VLog.d(TAG, "proximity cali_data: data[0]" + tempTestArg[0] + " data[1] " + tempTestArg[1] + " data[2] " + tempTestArg[2]);
        }
    }

    private void sendAccGyroCaliData(long delay) {
        int offsetX;
        int offsetY;
        String str;
        String str2;
        String str3;
        int offsetZ;
        String str4;
        String str5;
        int offsetX2;
        int offsetY2;
        int offsetX3;
        int offsetY3;
        int offsetZ2 = 0;
        int[] tempTestArgAcc = new int[3];
        SensorTestResult tempResAcc = new SensorTestResult();
        int[] tempTestArgGyro = new int[3];
        SensorTestResult tempResGyro = new SensorTestResult();
        int offsetFlag = SystemProperties.getInt(ACC_CALI_OFFSET_FLAG, 0);
        if (offsetFlag != 1) {
            offsetX = 0;
            offsetY = 0;
            str = "unknown";
            str2 = TAG;
            str3 = " Y ";
            offsetZ = 0;
            str4 = ",";
            str5 = " Z ";
        } else {
            String caliOffsetDataStr = SystemProperties.get("persist.sys.gsensor_cal_xyz", "unknown");
            if (caliOffsetDataStr.equals("unknown")) {
                offsetX2 = 0;
                offsetY2 = 0;
            } else {
                String[] strs = caliOffsetDataStr.split(",");
                offsetX2 = 0;
                int offsetX4 = strs.length;
                offsetY2 = 0;
                if (offsetX4 == 3) {
                    offsetX3 = Integer.parseInt(strs[0].trim());
                    offsetY3 = Integer.parseInt(strs[1].trim());
                    offsetZ2 = Integer.parseInt(strs[2].trim());
                    tempTestArgAcc[0] = offsetX3;
                    tempTestArgAcc[1] = offsetY3;
                    tempTestArgAcc[2] = offsetZ2;
                    StringBuilder sb = new StringBuilder();
                    offsetX = offsetX3;
                    sb.append("Acc cali_off_data: X");
                    offsetY = offsetY3;
                    int offsetY4 = tempTestArgAcc[0];
                    sb.append(offsetY4);
                    sb.append(" Y ");
                    sb.append(tempTestArgAcc[1]);
                    sb.append(" Z ");
                    sb.append(tempTestArgAcc[2]);
                    VLog.d(TAG, sb.toString());
                    VivoSensorImpl vivoSensorImpl = this.mVivoSensorImpl;
                    str = "unknown";
                    str2 = TAG;
                    str3 = " Y ";
                    int offsetZ3 = offsetZ2;
                    str4 = ",";
                    str5 = " Z ";
                    vivoSensorImpl.vivoSensorTest((int) ACC_SET_OFFSET, tempResAcc, tempTestArgAcc, delay);
                    offsetZ = offsetZ3;
                }
            }
            offsetX3 = offsetX2;
            offsetY3 = offsetY2;
            tempTestArgAcc[0] = offsetX3;
            tempTestArgAcc[1] = offsetY3;
            tempTestArgAcc[2] = offsetZ2;
            StringBuilder sb2 = new StringBuilder();
            offsetX = offsetX3;
            sb2.append("Acc cali_off_data: X");
            offsetY = offsetY3;
            int offsetY42 = tempTestArgAcc[0];
            sb2.append(offsetY42);
            sb2.append(" Y ");
            sb2.append(tempTestArgAcc[1]);
            sb2.append(" Z ");
            sb2.append(tempTestArgAcc[2]);
            VLog.d(TAG, sb2.toString());
            VivoSensorImpl vivoSensorImpl2 = this.mVivoSensorImpl;
            str = "unknown";
            str2 = TAG;
            str3 = " Y ";
            int offsetZ32 = offsetZ2;
            str4 = ",";
            str5 = " Z ";
            vivoSensorImpl2.vivoSensorTest((int) ACC_SET_OFFSET, tempResAcc, tempTestArgAcc, delay);
            offsetZ = offsetZ32;
        }
        int offsetFlag2 = SystemProperties.getInt(GYRO_CALI_OFFSET_FLAG, 0);
        if (offsetFlag2 == 1) {
            String caliOffsetDataStr2 = SystemProperties.get("persist.sys.gyroscope_cal_xyz", str);
            if (!caliOffsetDataStr2.equals(str)) {
                String[] strs2 = caliOffsetDataStr2.split(str4);
                if (strs2.length == 3) {
                    int offsetX5 = Integer.parseInt(strs2[0].trim());
                    int offsetY5 = Integer.parseInt(strs2[1].trim());
                    offsetZ = Integer.parseInt(strs2[2].trim());
                    offsetX = offsetX5;
                    offsetY = offsetY5;
                }
            }
            tempTestArgGyro[0] = offsetX;
            tempTestArgGyro[1] = offsetY;
            tempTestArgGyro[2] = offsetZ;
            VLog.d(str2, "Gyro cali_off_data: X" + tempTestArgGyro[0] + str3 + tempTestArgGyro[1] + str5 + tempTestArgGyro[2]);
            this.mVivoSensorImpl.vivoSensorTest((int) GYRO_SET_OFFSET, tempResGyro, tempTestArgGyro, delay);
        }
    }

    private void sendCctCaliData(long delay) {
        int[] tempTestArgPre = new int[3];
        SensorTestResult tempResPre = new SensorTestResult();
        int[] tempTestArgPost = new int[3];
        SensorTestResult tempResPost = new SensorTestResult();
        String rcali = SystemProperties.get(CCT_CALI_FACTOR_R, "unknown");
        String gcali = SystemProperties.get(CCT_CALI_FACTOR_G, "unknown");
        String bcali = SystemProperties.get(CCT_CALI_FACTOR_B, "unknown");
        String ccali = SystemProperties.get(CCT_CALI_FACTOR_C, "unknown");
        String wbcali = SystemProperties.get(CCT_CALI_FACTOR_WB, "unknown");
        if (!rcali.equals("unknown") && !gcali.equals("unknown") && !bcali.equals("unknown")) {
            if (!ccali.equals("unknown") && !wbcali.equals("unknown")) {
                try {
                    tempTestArgPre[0] = (int) (Float.parseFloat(ccali.trim()) * 1000.0f);
                    tempTestArgPre[1] = (int) (Float.parseFloat(rcali.trim()) * 1000.0f);
                    tempTestArgPre[2] = (int) (Float.parseFloat(gcali.trim()) * 1000.0f);
                    VLog.d(TAG, "Cct cali_off_data: c " + tempTestArgPre[0] + " r " + tempTestArgPre[1] + " g " + tempTestArgPre[2]);
                    this.mVivoSensorImpl.vivoSensorTest((int) PRE_CCT_SET_OFFSET, tempResPre, tempTestArgPre, delay);
                    try {
                        tempTestArgPost[0] = (int) (Float.parseFloat(bcali.trim()) * 1000.0f);
                        tempTestArgPost[1] = (int) (Float.parseFloat(wbcali.trim()) * 1000.0f);
                        tempTestArgPost[2] = 0;
                        VLog.d(TAG, "Cct cali_off_data: b " + tempTestArgPost[0] + " wb " + tempTestArgPost[1]);
                        this.mVivoSensorImpl.vivoSensorTest((int) POST_CCT_SET_OFFSET, tempResPost, tempTestArgPost, delay);
                    } catch (Exception e) {
                        VLog.e(TAG, " NumberFormatException ");
                    }
                } catch (Exception e2) {
                    VLog.e(TAG, " NumberFormatException ");
                }
            }
        }
    }

    private void sendMagCaliData(long delay) {
        String mag_param = SystemProperties.get("persist.vendor.mag_calilib_param", "unknown").trim();
        parseMagCali(mag_param, 72, delay);
        String mag_offset = SystemProperties.get("persist.vendor.mag_calilib_offset", "unknown").trim();
        parseMagCali(mag_offset, 69, delay);
    }

    private void parseMagCali(String value, int type, long delay) {
        int[] tempTestArg = new int[3];
        SensorTestResult tempRes = new SensorTestResult();
        try {
            if (!value.equals("unknown")) {
                String[] cali = value.split(",");
                if (cali.length == 3) {
                    tempTestArg[0] = Integer.parseInt(cali[0]);
                    tempTestArg[1] = Integer.parseInt(cali[1]);
                    tempTestArg[2] = Integer.parseInt(cali[2]);
                    VLog.d(TAG, "mag cali_data: X " + tempTestArg[0] + " Y " + tempTestArg[1] + " Z " + tempTestArg[2]);
                    if (tempTestArg[0] != 0 || tempTestArg[1] != 0 || tempTestArg[2] != 0) {
                        this.mVivoSensorImpl.vivoSensorTest(type, tempRes, tempTestArg, delay);
                    }
                }
            }
        } catch (Exception e) {
            VLog.e(TAG, " get mag_cali_data failed.");
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void sendFactoryModeToDriver(long delay) {
        if (this.mVivoSensorImpl == null) {
            VLog.e(TAG, "mVivoSensorImpl is null.");
            return;
        }
        SensorTestResult tempRes = new SensorTestResult();
        int[] tempTestArg = {SENSOR_COMMAND_PROXIMITY_ENG_MODE, 0, 0};
        if (factoryMode.equals("yes")) {
            tempTestArg[1] = 1;
        }
        this.mVivoSensorImpl.vivoSensorTest((int) PS_NOTIFY_ENG_MODE, tempRes, tempTestArg, delay);
        VLog.d(TAG, "factory mode: " + tempTestArg[1]);
    }
}