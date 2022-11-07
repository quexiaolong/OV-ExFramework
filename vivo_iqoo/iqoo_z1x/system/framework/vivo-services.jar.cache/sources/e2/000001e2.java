package com.android.server.display;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.SystemClock;
import com.android.server.wm.VivoWmsImpl;
import com.vivo.sensor.autobrightness.AutoBrightnessManagerImpl;
import com.vivo.sensor.autobrightness.utils.SElog;
import com.vivo.sensor.common.DriverNodeOperate;
import com.vivo.sensor.common.JsonObjectOperate;
import com.vivo.sensor.implement.SensorConfig;
import com.vivo.services.rms.ProcessList;
import java.io.File;
import java.util.ArrayList;
import java.util.Vector;
import org.json.JSONArray;
import org.json.JSONObject;
import vendor.pixelworks.hardware.display.V1_0.KernelConfig;
import vendor.pixelworks.hardware.display.V1_0.VendorConfig;

/* loaded from: classes.dex */
public class VivoRampAnimatorImpl implements IVivoRampAnimator {
    private static final int BRIGHTNESS_COUNT = 6;
    private static final String CONFIG_PATH = "/system/etc/SensorConfig/RampAnimatorParam.json";
    private static final int DIM_RATE = 600;
    private static final int DIM_TOTAL_TIME = 600;
    private static final String LCM_ID_PATH = "/sys/lcm/lcm_id";
    private static final int LCM_MAX_BRIGHTNESS_2047 = 2047;
    private static final int LCM_MAX_BRIGHTNESS_4095 = 4095;
    static final int PEM_BRIGHTNESS_RATE = 500;
    private static final int QUICKDOWN_COUNT = 4;
    private static final int QUICKUP_COUNT = 3;
    private static float QUICK_FIRST_TARGET = 0.0f;
    private static float QUICK_FOUR_TARGET = 0.0f;
    private static final int QUICK_RATE = 200;
    private static float QUICK_SECOND_TARGET = 0.0f;
    private static float QUICK_THIRD_TARGET = 0.0f;
    private static final String SENSOR_SUFFIX = ".json";
    private static final String TAG = "VRampAnimator";
    private AnimationCallback mAnimationCallback;
    private Context mContext;
    private int mGapMsparamter;
    public RampAnimator mRampAnimator;
    private float mRecordCurrentValue;
    private int mTotalTime;
    public static Vector<Integer> mRamperDownSectPoint = new Vector<>(0);
    public static Vector<Integer> mRamperDownSectMinStep = new Vector<>(0);
    public static Vector<Integer> mRamperUpSectPoint = new Vector<>(0);
    public static ArrayList<Vector> mRamperDownSection = new ArrayList<>();
    public static int DEFALUT_GAP = 50;
    public static float DEFALUT_STEP = 0.005f;
    public static Vector<Integer> mRamperQuickSectPoint = new Vector<>(0);
    public static double mUpStepNit = 0.5d;
    public static int mUpStep = 5;
    public static int mUpTotalCount = 20;
    public static long mUpTotalTs = 2800;
    public static boolean mIsChangeingUp = false;
    public static int mUpTime = 2800;
    public static int mUpGap = 100;
    public static long mCurrentChangeUpTs = 0;
    private static double mTargetUpLcmNit = 100.0d;
    private static int mUpLimmitCount = 25;
    private static int mUpRealCount = 25;
    private static int mCurrentDownSection = -1;
    private static long mChangeDownTotalTime = -1;
    private static int mDownTotalCount = 0;
    private static int mDownSetcionStep = 0;
    public static int mDownGap = 100;
    public static int mDownStep = 1;
    public static int mDownSectionTsBr = 500;
    public static int mDownSectionGapMinOne = 20;
    public static int mDownSectionGapMinTwo = 80;
    private float mTargetValuePriv = -1.0f;
    private int gap = DEFALUT_GAP;
    private int mTotalPemBrightnessTime = 10000;
    private int mPemBrightnessGap = 20;
    private int mPemBrightnessStep = 1;
    float mPemBrightnesssScale = 1.0f;
    private float mPemGoblalTarget = -1.0f;
    private boolean mRequestNeedStopAnimate = false;
    private Vector<Double> mPemSectionPoint = new Vector<>(0);
    private Vector<Integer> mPemSectionPointScale = new Vector<>(0);
    private int[] m_QuickupStep = new int[3];
    private int[] m_QuickupGap = new int[3];
    private int[] m_QuickdownStep = new int[4];
    private int[] m_QuickdownGap = new int[4];
    private float mQuickStep = 0.01f;
    private long mDownts = 0;
    private long mDownSectionTs = 0;
    private int mDownSectionBr = 0;
    private int mLastDownLcm = 0;
    private int[] mDownStepByStep = new int[4];
    private int[] mDownGapByStep = new int[4];
    private int mDownForLcmFlickIssue = 1;
    private boolean mIsUserDownStepConfig = false;
    private float mCurrentSettingValue = 1.0f;
    private int mSettingLcmStep = 1;
    private int mSettingLcmValue = LCM_MAX_BRIGHTNESS_2047;
    private int mSettingLcmDeltaValue = 1;
    private boolean mUseDefaultVaule = false;
    private int mDimGap = 20;
    private int mDimStep = 30;

    private void parseRamperParams(String JsonData) {
        JSONArray section_downpoint;
        JSONArray section_downpoint2;
        SElog.d(TAG, "pareseRameperDownParams  br start");
        try {
            JSONObject jsonObj = new JSONObject(JsonData);
            if (!jsonObj.has("ramperbrightness")) {
                this.mUseDefaultVaule = true;
                return;
            }
            JSONObject algoMapLcmLevelObj = jsonObj.getJSONObject("ramperbrightness");
            if (algoMapLcmLevelObj == null) {
                this.mUseDefaultVaule = true;
            } else {
                if (algoMapLcmLevelObj.has("dimgap")) {
                    this.mDimGap = algoMapLcmLevelObj.getInt("dimgap");
                }
                if (algoMapLcmLevelObj.has("section_downpoint") && (section_downpoint2 = algoMapLcmLevelObj.getJSONArray("section_downpoint")) != null) {
                    int i = 0;
                    while (true) {
                        JSONObject jsonObj2 = jsonObj;
                        if (i >= section_downpoint2.length()) {
                            break;
                        }
                        mRamperDownSectPoint.add(Integer.valueOf(section_downpoint2.getInt(i)));
                        i++;
                        jsonObj = jsonObj2;
                        section_downpoint2 = section_downpoint2;
                    }
                }
                if (algoMapLcmLevelObj.has("sectiondown")) {
                    JSONArray section_temp = algoMapLcmLevelObj.getJSONArray("sectiondown");
                    int i2 = 0;
                    while (i2 < section_temp.length()) {
                        JSONArray section_temp2 = section_temp.getJSONArray(i2);
                        JSONArray section_temp3 = section_temp;
                        Vector<Double> algoMapLcmBrSec1Value = new Vector<>(0);
                        if (section_temp2 != null) {
                            for (int j = 0; j < section_temp2.length(); j++) {
                                algoMapLcmBrSec1Value.add(Double.valueOf(section_temp2.getDouble(j)));
                            }
                            int j2 = section_temp2.length();
                            if (j2 > 0) {
                                mRamperDownSection.add(algoMapLcmBrSec1Value);
                            }
                        }
                        i2++;
                        section_temp = section_temp3;
                    }
                }
                if (algoMapLcmLevelObj.has("section_down_min_step") && (section_downpoint = algoMapLcmLevelObj.getJSONArray("section_down_min_step")) != null) {
                    for (int i3 = 0; i3 < section_downpoint.length(); i3++) {
                        mRamperDownSectMinStep.add(Integer.valueOf(section_downpoint.getInt(i3)));
                    }
                }
                if (algoMapLcmLevelObj.has("downStepByStep")) {
                    JSONArray downstepBystep = algoMapLcmLevelObj.getJSONArray("downStepByStep");
                    for (int j3 = 0; j3 < downstepBystep.length(); j3++) {
                        this.mDownStepByStep[j3] = downstepBystep.getInt(j3);
                        SElog.d(TAG, "downstepbystep " + this.mDownStepByStep[j3]);
                    }
                }
                if (algoMapLcmLevelObj.has("downGapByStep")) {
                    JSONArray downGapByStep = algoMapLcmLevelObj.getJSONArray("downGapByStep");
                    for (int j4 = 0; j4 < downGapByStep.length(); j4++) {
                        this.mDownGapByStep[j4] = downGapByStep.getInt(j4);
                        SElog.d(TAG, "downGapbystep " + this.mDownGapByStep[j4]);
                    }
                }
                if (algoMapLcmLevelObj.has("isUseDownStepConfig")) {
                    this.mIsUserDownStepConfig = algoMapLcmLevelObj.getInt("isUseDownStepConfig") == 1;
                }
                if (algoMapLcmLevelObj.has("downlcmFlickValue")) {
                    this.mDownForLcmFlickIssue = algoMapLcmLevelObj.getInt("downlcmFlickValue");
                }
                if (algoMapLcmLevelObj.has("sectiondownkeybr")) {
                    mDownSectionTsBr = algoMapLcmLevelObj.getInt("sectiondownkeybr");
                }
                if (algoMapLcmLevelObj.has("sectiondowngapminone")) {
                    mDownSectionGapMinOne = algoMapLcmLevelObj.getInt("sectiondowngapminone");
                }
                if (algoMapLcmLevelObj.has("sectiondowngapmintwo")) {
                    mDownSectionGapMinTwo = algoMapLcmLevelObj.getInt("sectiondowngapmintwo");
                }
                if (!algoMapLcmLevelObj.has("section_uppoint")) {
                    this.mUseDefaultVaule = true;
                } else {
                    JSONArray section_uppoint = algoMapLcmLevelObj.getJSONArray("section_uppoint");
                    if (section_uppoint != null) {
                        for (int i4 = 0; i4 < section_uppoint.length(); i4++) {
                            mRamperUpSectPoint.add(Integer.valueOf(section_uppoint.getInt(i4)));
                        }
                    }
                }
                if (algoMapLcmLevelObj.has("uptotaltime")) {
                    mUpTime = algoMapLcmLevelObj.getInt("uptotaltime");
                }
                if (algoMapLcmLevelObj.has("uplimmitcount")) {
                    mUpLimmitCount = algoMapLcmLevelObj.getInt("uplimmitcount");
                }
                if (!algoMapLcmLevelObj.has("quick_sectionpoint")) {
                    this.mUseDefaultVaule = true;
                } else {
                    JSONArray quick_sectionpoint = algoMapLcmLevelObj.getJSONArray("quick_sectionpoint");
                    if (quick_sectionpoint != null) {
                        for (int i5 = 0; i5 < quick_sectionpoint.length(); i5++) {
                            mRamperQuickSectPoint.add(Integer.valueOf(quick_sectionpoint.getInt(i5)));
                            SElog.d(TAG, "quick_sectionpoint " + mRamperQuickSectPoint.get(i5));
                        }
                    }
                }
                if (algoMapLcmLevelObj.has("quickupstep")) {
                    JSONArray quickupstep = algoMapLcmLevelObj.getJSONArray("quickupstep");
                    for (int j5 = 0; j5 < quickupstep.length(); j5++) {
                        this.m_QuickupStep[j5] = quickupstep.getInt(j5);
                        SElog.d(TAG, "quickupstep " + this.m_QuickupStep[j5]);
                    }
                }
                if (algoMapLcmLevelObj.has("quickupgap")) {
                    JSONArray quickupgap = algoMapLcmLevelObj.getJSONArray("quickupgap");
                    for (int j6 = 0; j6 < quickupgap.length(); j6++) {
                        this.m_QuickupGap[j6] = Integer.parseInt(quickupgap.getString(j6));
                        SElog.d(TAG, "quickupgap " + this.m_QuickupGap[j6]);
                    }
                }
                if (algoMapLcmLevelObj.has("quickdownstep")) {
                    JSONArray quickdownstep = algoMapLcmLevelObj.getJSONArray("quickdownstep");
                    for (int j7 = 0; j7 < quickdownstep.length(); j7++) {
                        this.m_QuickdownStep[j7] = Integer.parseInt(quickdownstep.getString(j7));
                        SElog.d(TAG, "quickdownstep " + this.m_QuickdownStep[j7]);
                    }
                }
                if (algoMapLcmLevelObj.has("quickdowngap")) {
                    JSONArray quickdowngap = algoMapLcmLevelObj.getJSONArray("quickdowngap");
                    for (int j8 = 0; j8 < quickdowngap.length(); j8++) {
                        this.m_QuickdownGap[j8] = Integer.parseInt(quickdowngap.getString(j8));
                        SElog.d(TAG, "quickdowngap " + this.m_QuickdownGap[j8]);
                    }
                }
                if (algoMapLcmLevelObj.has("pemtotaltime")) {
                    this.mTotalPemBrightnessTime = algoMapLcmLevelObj.getInt("pemtotaltime");
                }
                if (algoMapLcmLevelObj.has("pemsectionnit")) {
                    JSONArray pemsectionnit = algoMapLcmLevelObj.getJSONArray("pemsectionnit");
                    for (int j9 = 0; j9 < pemsectionnit.length(); j9++) {
                        this.mPemSectionPoint.add(Double.valueOf(pemsectionnit.getDouble(j9)));
                        SElog.d(TAG, "mPemSectionPoint  " + this.mPemSectionPoint.get(j9));
                    }
                    if (algoMapLcmLevelObj.has("pemsectionscale")) {
                        JSONArray pemsectionscale = algoMapLcmLevelObj.getJSONArray("pemsectionscale");
                        for (int j10 = 0; j10 < pemsectionscale.length(); j10++) {
                            this.mPemSectionPointScale.add(Integer.valueOf(pemsectionscale.getInt(j10)));
                            SElog.d(TAG, "mPemSectionPointScale  " + this.mPemSectionPointScale.get(j10));
                        }
                    }
                } else {
                    this.mPemSectionPoint.clear();
                    this.mPemSectionPoint.add(Double.valueOf(20.0d));
                    this.mPemSectionPointScale.add(100);
                }
            }
        } catch (Exception e) {
            this.mUseDefaultVaule = true;
            e.printStackTrace();
        }
    }

    private void getQuickBrightSegmentPoint() {
        if (mRamperQuickSectPoint.size() >= 4) {
            QUICK_FOUR_TARGET = SensorConfig.lcmBrightness2FloatAfterDPC(mRamperQuickSectPoint.get(0).intValue());
            QUICK_THIRD_TARGET = SensorConfig.lcmBrightness2FloatAfterDPC(mRamperQuickSectPoint.get(1).intValue());
            QUICK_SECOND_TARGET = SensorConfig.lcmBrightness2FloatAfterDPC(mRamperQuickSectPoint.get(2).intValue());
            QUICK_FIRST_TARGET = SensorConfig.lcmBrightness2FloatAfterDPC(mRamperQuickSectPoint.get(3).intValue());
        }
    }

    public VivoRampAnimatorImpl(RampAnimator rampanimator, Context context) {
        this.mRampAnimator = rampanimator;
        this.mContext = context;
    }

    public Runnable getAnimationCallback() {
        parseGapAndStep();
        AnimationCallbackForNew animationCallbackForNew = new AnimationCallbackForNew();
        this.mAnimationCallback = animationCallbackForNew;
        return animationCallbackForNew;
    }

    private void parseGapAndStep() {
        SElog.d(TAG, "parseGapAndStep start: ");
        String path = getMultiSupplyJsonName(CONFIG_PATH);
        String JsonData = JsonObjectOperate.readJson(path);
        if (JsonData != null) {
            parseRamperParams(JsonData);
        } else {
            this.mUseDefaultVaule = true;
        }
        if (this.mUseDefaultVaule) {
            setDefaultValue();
        }
        getQuickBrightSegmentPoint();
    }

    private void setDefault2047() {
        mRamperDownSection.clear();
        mRamperDownSectPoint.clear();
        mRamperUpSectPoint.clear();
        int[] upsection = {500, 1800};
        for (int i : upsection) {
            mRamperUpSectPoint.add(Integer.valueOf(i));
        }
        int[] downsectionpoint = {1087, 858, 729, 612, 511, 454, 424, 395, 386, 375, 355, 296, 282, VendorConfig.SET_MEMC_SETTING, 254, 247, 193, 152, 139, KernelConfig.DUAL2SINGLE_ST, KernelConfig.OUT_FRAME_RATE_SET, KernelConfig.APP_FILTER, 89, 78, 64, 57, 45, 38, 18, 2};
        for (int i2 : downsectionpoint) {
            mRamperDownSectPoint.add(Integer.valueOf(i2));
        }
        double[][] downsection = {new double[]{-2.293005671d, 6081.782609d}, new double[]{-3.489559165d, 7898.150812d}, new double[]{-4.799126638d, 9321.650655d}, new double[]{-6.217054264d, 10538.23256d}, new double[]{-9.393162393d, 12853.61538d}, new double[]{-12.9009901d, 15000.40594d}, new double[]{-24.38596491d, 20869.22807d}, new double[]{-65.46153846d, 39517.53846d}, new double[]{-70.27272727d, 41576.72727d}, new double[]{-109.1111111d, 56917.88889d}, new double[]{-73.0d, 42979.0d}, new double[]{-74.85d, 43672.75d}, new double[]{-123.8135593d, 61054.81356d}, new double[]{-178.7142857d, 77305.42857d}, new double[]{-143.6875d, 67427.875d}, new double[]{-208.1666667d, 84579.33333d}, new double[]{-157.2857143d, 71655.57143d}, new double[]{-107.4259259d, 59340.2037d}, new double[]{-131.4634146d, 63979.43902d}, new double[]{-162.1538462d, 68644.38462d}, new double[]{-102.0d, 60283.0d}, new double[]{-166.0833333d, 68870.16667d}, new double[]{-245.1818182d, 78520.18182d}, new double[]{-68.5d, 58908.5d}, new double[]{-116.9090909d, 63216.90909d}, new double[]{-35.78571429d, 56889.28571d}, new double[]{-157.4285714d, 64674.42857d}, new double[]{-42.41666667d, 58118.75d}, new double[]{-127.1428571d, 61931.42857d}, new double[]{-90.0d, 60520.0d}, new double[]{-100.4d, 60707.2d}};
        for (int i3 = 0; i3 < downsection.length; i3++) {
            Vector<Double> tempValue = new Vector<>(0);
            tempValue.add(Double.valueOf(downsection[i3][0]));
            tempValue.add(Double.valueOf(downsection[i3][1]));
            mRamperDownSection.add(tempValue);
        }
        mRamperDownSectMinStep.clear();
        int[] downsectionminstep = {22, 15, 11, 9, 6, 4, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
        for (int i4 : downsectionminstep) {
            mRamperDownSectMinStep.add(Integer.valueOf(i4));
        }
        mRamperQuickSectPoint.clear();
        int[] quicksectionpoint = {1000, 570, 351, 2};
        for (int i5 = 0; i5 < quicksectionpoint.length; i5++) {
            mRamperQuickSectPoint.add(Integer.valueOf(i5));
        }
        int[] iArr = this.m_QuickupStep;
        iArr[0] = 40;
        iArr[1] = 50;
        iArr[2] = 100;
        int[] iArr2 = this.m_QuickupGap;
        iArr2[0] = 20;
        iArr2[1] = 0;
        iArr2[2] = 0;
        int[] iArr3 = this.m_QuickdownStep;
        iArr3[0] = 5;
        iArr3[1] = 7;
        iArr3[2] = 12;
        iArr3[3] = 60;
        int[] iArr4 = this.m_QuickdownGap;
        iArr4[0] = 40;
        iArr4[1] = 40;
        iArr4[2] = 30;
        iArr4[3] = 20;
        mDownSectionTsBr = ProcessList.HEAVY_WEIGHT_APP_ADJ;
    }

    private void setDefault4095() {
        mRamperDownSection.clear();
        mRamperDownSectPoint.clear();
        mRamperUpSectPoint.clear();
        int[] upsection = {764, 2390};
        for (int i : upsection) {
            mRamperUpSectPoint.add(Integer.valueOf(i));
        }
        int[] downsectionpoint = {3301, 2509, 1518, 988, 848, 739, 511, ProcessList.PROTECT_SERVICE_ADJ, 413, 295, VivoWmsImpl.UPDATA_SYSTEMUI_GESTURE_STYLE, 152, KernelConfig.OSD_ENABLE, 98, 69, 47, 30, 17, 2};
        for (int i2 : downsectionpoint) {
            mRamperDownSectPoint.add(Integer.valueOf(i2));
        }
        double[][] downsection = {new double[]{-1.77581864d, 9050.97733d}, new double[]{-3.111111111d, 13458.77778d}, new double[]{-5.24419778d, 18810.69223d}, new double[]{-8.305660377d, 23457.99245d}, new double[]{-9.821428571d, 24955.57143d}, new double[]{-11.32110092d, 26227.29358d}, new double[]{-15.02631579d, 28965.44737d}, new double[]{-19.29508197d, 31146.78689d}, new double[]{-22.16216216d, 32436.97297d}, new double[]{-26.77118644d, 34340.5d}, new double[]{-37.52272727d, 37512.20455d}, new double[]{-52.92727273d, 40700.94545d}, new double[]{-66.27586207d, 42729.93103d}, new double[]{-87.72d, 45367.56d}, new double[]{-109.0d, 47453.0d}, new double[]{-168.3636364d, 51549.09091d}, new double[]{-242.0d, 55010.0d}, new double[]{-402.1538462d, 59814.61538d}, new double[]{-591.25d, 63029.25d}};
        for (int i3 = 0; i3 < downsection.length; i3++) {
            Vector<Double> tempValue = new Vector<>(0);
            tempValue.add(Double.valueOf(downsection[i3][0]));
            tempValue.add(Double.valueOf(downsection[i3][1]));
            mRamperDownSection.add(tempValue);
        }
        mRamperDownSectMinStep.clear();
        int[] downsectionminstep = {29, 17, 10, 7, 6, 5, 4, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2};
        for (int i4 : downsectionminstep) {
            mRamperDownSectMinStep.add(Integer.valueOf(i4));
        }
        mRamperQuickSectPoint.clear();
        int[] quicksectionpoint = {1803, 1000, 465, 2};
        for (int i5 = 0; i5 < quicksectionpoint.length; i5++) {
            mRamperQuickSectPoint.add(Integer.valueOf(i5));
        }
        int[] iArr = this.m_QuickupStep;
        iArr[0] = 55;
        iArr[1] = 125;
        iArr[2] = 218;
        int[] iArr2 = this.m_QuickupGap;
        iArr2[0] = 20;
        iArr2[1] = 0;
        iArr2[2] = 0;
        int[] iArr3 = this.m_QuickdownStep;
        iArr3[0] = 7;
        iArr3[1] = 22;
        iArr3[2] = 24;
        iArr3[3] = 131;
        int[] iArr4 = this.m_QuickdownGap;
        iArr4[0] = 40;
        iArr4[1] = 40;
        iArr4[2] = 30;
        iArr4[3] = 20;
        mDownSectionTsBr = 739;
    }

    private void setDefaultValue() {
        if (SensorConfig.float2LcmBrightnessAfterDPC(1.0f) == LCM_MAX_BRIGHTNESS_2047) {
            setDefault2047();
        } else if (SensorConfig.float2LcmBrightnessAfterDPC(1.0f) == LCM_MAX_BRIGHTNESS_4095) {
            setDefault4095();
        }
    }

    private static String modifyFilePath(String originPath, String LCM_ID_PATH2) {
        String modifiedFilePath = originPath;
        String driverDisplayId = getDriverLcmId(LCM_ID_PATH2);
        if (driverDisplayId == null) {
            SElog.e(TAG, "Failed to get Display ID!");
        } else {
            String driverDisplayIdToLower = driverDisplayId.toLowerCase().trim();
            if (driverDisplayIdToLower.length() > 0) {
                String[] strs = originPath.split("\\.");
                if (strs.length > 0 && strs[0] != null) {
                    modifiedFilePath = strs[0] + "_" + driverDisplayIdToLower + SENSOR_SUFFIX;
                }
                File file = new File(modifiedFilePath);
                if (!file.exists()) {
                    modifiedFilePath = originPath;
                }
                SElog.e(TAG, "modifiedFilePath: " + originPath + " -> " + modifiedFilePath);
            }
        }
        String JsonData = JsonObjectOperate.readJson(modifiedFilePath);
        if (JsonData == null) {
            return originPath;
        }
        return modifiedFilePath;
    }

    public static String getDriverLcmId(String driverLcmPath) {
        String driverLcmId = DriverNodeOperate.getString(driverLcmPath);
        if (driverLcmId == null) {
            SElog.e(TAG, "read driver ic failed");
            return null;
        }
        return driverLcmId.trim();
    }

    private static String getMultiSupplyJsonName(String jsonPath) {
        return modifyFilePath(jsonPath, LCM_ID_PATH);
    }

    private long calcDownBrightnessTs(int brightness, Vector<Double> v) {
        long ts = 0;
        if (v == null) {
            ts = 0;
        }
        if (v.size() > 0) {
            int count = v.size() - 1;
            for (int i = 0; i < v.size(); i++) {
                ts += (long) (v.get(i).doubleValue() * Math.pow(brightness, count));
                count--;
            }
        }
        return ts;
    }

    private int getDownStepAndGap(int brightness) {
        int down_gap;
        long j = 4572414629676717179L;
        int down_gap2 = 20;
        int i = 0;
        while (true) {
            if (i >= mRamperDownSection.size()) {
                break;
            } else if (i >= mRamperDownSectPoint.size() || brightness < mRamperDownSectPoint.get(i).intValue()) {
                i++;
                j = j;
            } else {
                int i2 = mCurrentDownSection;
                if (i != i2 || i2 == -1) {
                    this.mDownts = calcDownBrightnessTs(brightness, mRamperDownSection.get(i));
                    this.mDownSectionTs = calcDownBrightnessTs(mRamperDownSectPoint.get(i).intValue(), mRamperDownSection.get(i));
                    int intValue = mRamperDownSectPoint.get(i).intValue();
                    this.mDownSectionBr = intValue;
                    mDownSetcionStep = Math.abs(brightness - intValue);
                    long delta_up_ts = Math.abs(this.mDownSectionTs - this.mDownts);
                    mChangeDownTotalTime = SystemClock.elapsedRealtime() + delta_up_ts;
                    mDownTotalCount = mDownSetcionStep;
                    mCurrentDownSection = i;
                    if (i < mRamperDownSectMinStep.size()) {
                        mDownStep = mRamperDownSectMinStep.get(i).intValue();
                    }
                }
            }
        }
        if (mDownSetcionStep > 0) {
            int delta_br = Math.abs(brightness - this.mDownSectionBr);
            int i3 = mDownStep;
            if (i3 > 0) {
                mDownTotalCount = delta_br / i3;
            }
            if (mDownTotalCount > 0) {
                int down_gap3 = ((int) (mChangeDownTotalTime - SystemClock.elapsedRealtime())) / mDownTotalCount;
                if (brightness > mDownSectionTsBr) {
                    down_gap2 = Math.max(mDownSectionGapMinOne, down_gap3);
                } else {
                    down_gap2 = Math.max(mDownSectionGapMinTwo, down_gap3);
                }
            }
            mDownGap = down_gap2;
        } else {
            mDownStep = 2;
        }
        if (mChangeDownTotalTime - SystemClock.elapsedRealtime() <= 0 || mDownSetcionStep <= 0) {
            if (brightness > mDownSectionTsBr) {
                down_gap = Math.max(mDownSectionGapMinOne, down_gap2);
            } else {
                down_gap = Math.max(mDownSectionGapMinTwo, down_gap2);
            }
            mDownGap = down_gap;
        }
        SElog.d(TAG, "getCurrentDown   br" + brightness + " step=" + mDownStep + " gap=" + mDownGap + " mDownts=" + this.mDownts + " seciont_ts=" + this.mDownSectionTs + " total_time=" + (mChangeDownTotalTime - SystemClock.elapsedRealtime()));
        int animate_lcm = brightness - mDownStep;
        return animate_lcm;
    }

    private float getUpStepAndGap(float brightness) {
        float animate_float;
        int current_lcm_brightness = SensorConfig.float2LcmBrightnessAfterDPC(this.mRampAnimator.mCurrentValue);
        double current_lcm_nit = SensorConfig.getAutoBrightnessLcmToNit(current_lcm_brightness);
        double animate_nit = mUpStepNit + current_lcm_nit;
        int i = current_lcm_brightness + 1;
        if (!SensorConfig.isSupportNitConvertLcm()) {
            current_lcm_nit = current_lcm_brightness;
            animate_nit = current_lcm_nit + mUpStepNit;
        }
        long elapsedRealtime = SystemClock.elapsedRealtime();
        mCurrentChangeUpTs = elapsedRealtime;
        long j = mUpTotalTs;
        if (j - elapsedRealtime > 0) {
            long delta_ts = j - elapsedRealtime;
            if (mUpStepNit > 0.0d) {
                int abs = (int) (Math.abs(mTargetUpLcmNit - current_lcm_nit) / mUpStepNit);
                mUpRealCount = abs;
                if (abs > 0) {
                    int i2 = (int) (delta_ts / abs);
                    mUpGap = i2;
                    mUpGap = Math.max(50, Math.min(mUpTime / mUpLimmitCount, i2));
                }
            }
        }
        if (!SensorConfig.isSupportNitConvertLcm()) {
            animate_float = SensorConfig.lcmBrightness2FloatAfterDPC((int) animate_nit);
        } else {
            int animate_lcm = SensorConfig.getAutoBrightnessNitToLcmBr(animate_nit);
            animate_float = SensorConfig.lcmBrightness2FloatAfterDPC(animate_lcm);
        }
        SElog.d(TAG, "getUpStepAndGap count=" + mUpTotalCount + " current gap=" + mUpGap + " mUpTime=" + mUpTime + " mUpStepNit=" + mUpStepNit + " animate_float=" + animate_float);
        return animate_float;
    }

    private void calcUpTotalTimeAndStep(float target) {
        int target_lcm_brightness = SensorConfig.float2LcmBrightnessAfterDPC(target);
        int current_lcm_brightness = SensorConfig.float2LcmBrightnessAfterDPC(this.mRampAnimator.mCurrentValue);
        mTargetUpLcmNit = SensorConfig.getAutoBrightnessLcmToNit(target_lcm_brightness);
        double current_lcm_brightness_nit = SensorConfig.getAutoBrightnessLcmToNit(current_lcm_brightness);
        if (!SensorConfig.isSupportNitConvertLcm()) {
            mTargetUpLcmNit = target_lcm_brightness;
            current_lcm_brightness_nit = current_lcm_brightness;
            mUpLimmitCount = 15;
        }
        if (target > this.mRampAnimator.mAnimatedValue) {
            if (!mIsChangeingUp) {
                mUpTotalTs = SystemClock.elapsedRealtime() + mUpTime;
                mCurrentChangeUpTs = SystemClock.elapsedRealtime();
            }
            long delta_up_ts = Math.max(mUpTotalTs - mCurrentChangeUpTs, 1000L);
            if (mRamperUpSectPoint.size() > 1) {
                if (target_lcm_brightness > mRamperUpSectPoint.get(0).intValue()) {
                    int max = Math.max(10, Math.min(mUpLimmitCount, ((int) Math.abs(mTargetUpLcmNit - current_lcm_brightness_nit)) / 15));
                    mUpTotalCount = max;
                    int i = (int) (delta_up_ts / max);
                    mUpGap = i;
                    mUpGap = Math.max(20, Math.min(mUpTime / mUpLimmitCount, i));
                } else if (target_lcm_brightness > mRamperUpSectPoint.get(1).intValue()) {
                    int max2 = Math.max(10, Math.min(mUpLimmitCount, ((int) Math.abs(mTargetUpLcmNit - current_lcm_brightness_nit)) / 20));
                    mUpTotalCount = max2;
                    int i2 = (int) (delta_up_ts / max2);
                    mUpGap = i2;
                    mUpGap = Math.max(20, Math.min(mUpTime / mUpLimmitCount, i2));
                } else {
                    int max3 = Math.max(10, Math.min(mUpLimmitCount, ((int) Math.abs(mTargetUpLcmNit - current_lcm_brightness_nit)) / 3));
                    mUpTotalCount = max3;
                    int i3 = (int) (delta_up_ts / max3);
                    mUpGap = i3;
                    mUpGap = Math.max(50, Math.min(mUpTime / mUpLimmitCount, i3));
                }
            }
            mUpStepNit = (mTargetUpLcmNit - current_lcm_brightness_nit) / mUpTotalCount;
            SElog.e(TAG, "calc uptotal mUpStepNit=" + mUpStepNit + " mUpGap=" + mUpGap + " target=" + target + " mAnimatedValue=" + this.mRampAnimator.mCurrentValue + " target_lcm=" + target_lcm_brightness + " current_lcm=" + current_lcm_brightness + " delta_up_ts=" + delta_up_ts + " mUpTotalCount=" + mUpTotalCount);
        }
    }

    private void calcDimStepAndGap(float target) {
        int deltaLcm = Math.abs(SensorConfig.float2LcmBrightnessAfterDPC(target) - SensorConfig.float2LcmBrightnessAfterDPC(this.mRampAnimator.mCurrentValue));
        int count = ProcessList.HOME_APP_ADJ / this.mDimGap;
        this.mDimStep = deltaLcm / Math.max(2, count);
        SElog.e(TAG, "calcDimStepAndGap gap= " + this.mDimGap + " step= " + this.mDimStep + " delta_lcm= " + deltaLcm);
    }

    public void setChangeTime(float target) {
        if (this.mTargetValuePriv != target) {
            SElog.e(TAG, "new target=" + target);
            if (this.mRampAnimator.mRate == 500.0f) {
                calcPemBrightnessStepAndGap(target);
            } else if (this.mRampAnimator.mRate == 600.0f) {
                calcDimStepAndGap(target);
            } else if (target > this.mTargetValuePriv) {
                calcUpTotalTimeAndStep(target);
            }
            mCurrentDownSection = -1;
            this.mTargetValuePriv = target;
        }
    }

    public void syncBrightnessSettings(float brightness) {
        this.mCurrentSettingValue = brightness;
        AutoBrightnessManagerImpl.getInstance(this.mContext).setFameworkBackLight(brightness);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isPemAnimationRunning() {
        if (this.mRampAnimator.mAnimating && ((int) this.mRampAnimator.mLastRate) == 500 && SensorConfig.floatEquals(this.mPemGoblalTarget, this.mRampAnimator.mGoblalTarget)) {
            return true;
        }
        return false;
    }

    public void setPemBrightnessScale(float scale) {
        SElog.d(TAG, "setPemBrightnessScale  scale=" + scale + "last_scale" + this.mPemBrightnesssScale);
        if (!SensorConfig.floatEquals(this.mPemBrightnesssScale, scale)) {
            this.mPemBrightnesssScale = scale;
            this.mRequestNeedStopAnimate = false;
            if (!this.mRampAnimator.mAnimating) {
                this.mPemGoblalTarget = this.mRampAnimator.mGoblalTarget;
                RampAnimator rampAnimator = this.mRampAnimator;
                rampAnimator.animateTo(rampAnimator.mGoblalTarget, 500.0f);
                return;
            }
            RampAnimator rampAnimator2 = this.mRampAnimator;
            rampAnimator2.animateTo(rampAnimator2.mGoblalTarget, this.mRampAnimator.mRate);
        }
    }

    public void needStopRampAnimator(boolean need) {
        boolean z = this.mRequestNeedStopAnimate;
        if (need != z) {
            if (!need && z && !this.mRampAnimator.mAnimating) {
                RampAnimator rampAnimator = this.mRampAnimator;
                rampAnimator.animateTo(rampAnimator.mGoblalTarget, this.mRampAnimator.mRate);
            }
            this.mRequestNeedStopAnimate = need;
            SElog.d(TAG, "needStopRampAnimator=" + this.mRequestNeedStopAnimate);
        }
    }

    public float getPemBrightness(float target) {
        if (target <= 0.0f) {
            return target;
        }
        if (SensorConfig.floatEquals(this.mPemBrightnesssScale, 1.0f)) {
            return target;
        }
        int temp_lcm = SensorConfig.float2LcmBrightnessAfterDPC(target);
        double nit = SensorConfig.getAutoBrightnessLcmToNit(temp_lcm);
        if (!SensorConfig.isSupportNitConvertLcm()) {
            return target * this.mPemBrightnesssScale;
        }
        double temp_scale = this.mPemBrightnesssScale;
        if (this.mPemSectionPoint.size() > 0 && nit <= this.mPemSectionPoint.get(0).doubleValue()) {
            temp_scale = 1.0d;
            SElog.d(TAG, "getPemBrightness fix scale=  1.0 pending scale= " + this.mPemBrightnesssScale);
        }
        double temp_nit = nit * temp_scale;
        double nit2 = temp_nit;
        int i = 0;
        while (true) {
            if (i < this.mPemSectionPoint.size()) {
                if (nit >= this.mPemSectionPoint.get(i).doubleValue() && temp_nit < this.mPemSectionPoint.get(i).doubleValue() && i < this.mPemSectionPointScale.size()) {
                    nit2 = Math.max((this.mPemSectionPoint.get(i).doubleValue() * this.mPemSectionPointScale.get(i).intValue()) / 100.0d, temp_nit);
                    SElog.d(TAG, "getPemBrightness fix nit " + nit2 + " locate_nit " + nit + " temp_nit " + temp_nit);
                    break;
                }
                i++;
                nit2 = nit2;
            } else {
                break;
            }
        }
        int temp_lcm2 = SensorConfig.getAutoBrightnessNitToLcmBr(nit2);
        float brightness = SensorConfig.lcmBrightness2FloatAfterDPC(temp_lcm2);
        if (brightness < 5.0E-5f) {
            brightness = 5.0E-5f;
        }
        SElog.d(TAG, "getPemBrightness  pem real nit= " + nit2 + " real lcm_br=" + brightness + "  frame_float_br= " + target + " frame_br= " + SensorConfig.float2LcmBrightnessAfterDPC(target));
        return brightness;
    }

    public void calcPemBrightnessStepAndGap(float target) {
        int pemBrightnessCount;
        if (this.mRampAnimator.mRate == 500.0f && (pemBrightnessCount = Math.abs(SensorConfig.float2LcmBrightnessAfterDPC(target) - SensorConfig.float2LcmBrightnessAfterDPC(this.mRampAnimator.mAnimatedValue))) > 0) {
            int i = this.mTotalPemBrightnessTime / pemBrightnessCount;
            this.mPemBrightnessGap = i;
            int max = Math.max(20, i);
            this.mPemBrightnessGap = max;
            int min = Math.min(500, max);
            this.mPemBrightnessGap = min;
            if (min > 0) {
                pemBrightnessCount = this.mTotalPemBrightnessTime / min;
            }
            if (pemBrightnessCount > 0) {
                this.mPemBrightnessStep = Math.abs(SensorConfig.float2LcmBrightnessAfterDPC(target) - SensorConfig.float2LcmBrightnessAfterDPC(this.mRampAnimator.mAnimatedValue)) / pemBrightnessCount;
            }
            this.mPemBrightnessStep = Math.max(1, this.mPemBrightnessStep);
        }
    }

    public void postVivoAnimationCallback(float target) {
        this.mTargetValuePriv = target;
        this.mRecordCurrentValue = this.mRampAnimator.mCurrentValue;
        postAnimationCallbackDelayed(0);
    }

    public void notifyGlobalTargetChanged(float target) {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void postAnimationCallbackDelayed(int period) {
        this.mRampAnimator.mChoreographer.postCallbackDelayed(1, this.mAnimationCallback, null, period);
    }

    public void setAnimating(boolean animating) {
        SensorConfig.setBackLightAnimating(animating);
    }

    /* loaded from: classes.dex */
    public abstract class AnimationCallback implements Runnable {
        public AnimationCallback() {
        }
    }

    private float calcSettingForRate200() {
        int setting_lcm = SensorConfig.float2LcmBrightness(this.mCurrentSettingValue);
        int setting_target = SensorConfig.float2LcmBrightness(this.mRampAnimator.mGoblalTarget);
        this.mSettingLcmDeltaValue = Math.abs(Math.abs(setting_lcm - setting_target) - SensorConfig.quickStepToLcmValue(this.mQuickStep));
        float setting_float = this.mCurrentSettingValue;
        int delta_quick = Math.max(1, Math.abs(SensorConfig.float2LcmBrightnessAfterDPC(this.mRampAnimator.mCurrentValue) - SensorConfig.float2LcmBrightnessAfterDPC(this.mRampAnimator.mTargetValue)));
        float scale = this.mSettingLcmDeltaValue / delta_quick;
        float f = this.mQuickStep;
        if (f > 0.0f) {
            this.mSettingLcmStep = Math.max(1, (int) (SensorConfig.quickStepToLcmValue(f) * scale));
        }
        if (this.mRampAnimator.mCurrentValue > this.mRampAnimator.mTargetValue) {
            setting_float = SensorConfig.lcmBrightness2Float(setting_lcm - this.mSettingLcmStep);
            if (setting_float >= this.mCurrentSettingValue) {
                setting_float -= 0.001f;
            }
        } else if (this.mRampAnimator.mCurrentValue < this.mRampAnimator.mTargetValue) {
            setting_float = SensorConfig.lcmBrightness2Float(this.mSettingLcmStep + setting_lcm);
            if (setting_float <= this.mCurrentSettingValue) {
                setting_float += 0.001f;
            }
        }
        float setting_float2 = Math.min(1.0f, setting_float);
        SElog.d(TAG, "calcSettingForRate200  mSettingLcmStep= " + this.mSettingLcmStep + " setting_float= " + setting_float2 + " mSettingLcmDeltaValue=" + this.mSettingLcmDeltaValue + " detal_quick= " + delta_quick + " scale=" + scale);
        return setting_float2;
    }

    private float calcSettingForAutoRate() {
        int setting_lcm = SensorConfig.float2LcmBrightness(this.mCurrentSettingValue);
        int setting_target = SensorConfig.float2LcmBrightness(this.mRampAnimator.mGoblalTarget);
        float setting_float = this.mCurrentSettingValue;
        int delta_auto = Math.max(1, Math.abs(SensorConfig.float2LcmBrightnessAfterDPC(this.mRampAnimator.mCurrentValue) - SensorConfig.float2LcmBrightnessAfterDPC(this.mRampAnimator.mTargetValue)));
        if (this.mRampAnimator.mCurrentValue > this.mRampAnimator.mTargetValue) {
            int abs = Math.abs(Math.abs(setting_lcm - setting_target) - mDownStep);
            this.mSettingLcmDeltaValue = abs;
            float scale = abs / delta_auto;
            int i = (int) (mDownStep * scale);
            this.mSettingLcmStep = i;
            setting_float = SensorConfig.lcmBrightness2Float(setting_lcm - i);
            if (setting_float >= this.mCurrentSettingValue) {
                setting_float -= 0.001f;
            }
        } else if (this.mRampAnimator.mCurrentValue < this.mRampAnimator.mTargetValue) {
            int abs2 = Math.abs(Math.abs(setting_lcm - setting_target) - mDownStep);
            this.mSettingLcmDeltaValue = abs2;
            int i2 = mUpRealCount;
            if (i2 > 0) {
                this.mSettingLcmStep = (int) (abs2 / i2);
            }
            setting_float = SensorConfig.lcmBrightness2Float(this.mSettingLcmStep + setting_lcm);
            if (setting_float <= this.mCurrentSettingValue) {
                setting_float += 0.001f;
            }
        }
        SElog.d(TAG, "calcSettingForRateAuto  mSettingLcmStep=" + this.mSettingLcmStep + " setting_float= " + setting_float + " mSettingLcmDeltaValue= " + this.mSettingLcmDeltaValue + " delta_auto=" + delta_auto);
        return setting_float;
    }

    private float calcSettingForRateDim() {
        int setting_lcm = SensorConfig.float2LcmBrightness(this.mCurrentSettingValue);
        int setting_target = SensorConfig.float2LcmBrightness(this.mRampAnimator.mGoblalTarget);
        float f = this.mCurrentSettingValue;
        this.mSettingLcmDeltaValue = Math.abs(Math.abs(setting_lcm - setting_target) - this.mDimStep);
        int delta_dim = Math.abs(SensorConfig.float2LcmBrightnessAfterDPC(this.mRampAnimator.mCurrentValue) - SensorConfig.float2LcmBrightnessAfterDPC(this.mRampAnimator.mTargetValue));
        float scale = this.mSettingLcmDeltaValue / delta_dim;
        int i = this.mDimStep;
        if (i > 0) {
            this.mSettingLcmStep = Math.max(1, (int) (i * scale));
        } else {
            this.mSettingLcmStep = 10;
        }
        float setting_float = SensorConfig.lcmBrightness2Float(setting_lcm - this.mSettingLcmStep);
        if (setting_float >= this.mCurrentSettingValue) {
            return setting_float - 0.001f;
        }
        return setting_float;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public float calcSettingStep() {
        float setting_float = this.mCurrentSettingValue;
        if (this.mRampAnimator.mRate != 500.0f) {
            if (this.mRampAnimator.mRate == 200.0f) {
                float setting_float2 = calcSettingForRate200();
                return setting_float2;
            } else if (this.mRampAnimator.mRate == 600.0f) {
                float setting_float3 = calcSettingForRateDim();
                return setting_float3;
            } else {
                float setting_float4 = calcSettingForAutoRate();
                return setting_float4;
            }
        }
        return setting_float;
    }

    private void handleQuickUpAnimate() {
        if (this.mRampAnimator.mAnimatedValue >= QUICK_FOUR_TARGET) {
            this.mQuickStep = SensorConfig.lcmBrightness2FloatAfterDPC(this.m_QuickupStep[2]);
            RampAnimator rampAnimator = this.mRampAnimator;
            rampAnimator.mAnimatedValue = Math.min(rampAnimator.mAnimatedValue + this.mQuickStep, this.mRampAnimator.mTargetValue);
            this.gap = this.m_QuickupGap[2];
        } else if (this.mRampAnimator.mAnimatedValue >= QUICK_SECOND_TARGET) {
            this.mQuickStep = SensorConfig.lcmBrightness2FloatAfterDPC(this.m_QuickupStep[1]);
            RampAnimator rampAnimator2 = this.mRampAnimator;
            rampAnimator2.mAnimatedValue = Math.min(rampAnimator2.mAnimatedValue + this.mQuickStep, this.mRampAnimator.mTargetValue);
            this.gap = this.m_QuickupGap[1];
        } else if (this.mRampAnimator.mAnimatedValue >= QUICK_FIRST_TARGET) {
            this.mQuickStep = SensorConfig.lcmBrightness2FloatAfterDPC(this.m_QuickupStep[0]);
            RampAnimator rampAnimator3 = this.mRampAnimator;
            rampAnimator3.mAnimatedValue = Math.min(rampAnimator3.mAnimatedValue + this.mQuickStep, this.mRampAnimator.mTargetValue);
            this.gap = this.m_QuickupGap[0];
        } else {
            this.mQuickStep = 0.01f;
            RampAnimator rampAnimator4 = this.mRampAnimator;
            rampAnimator4.mAnimatedValue = Math.min(rampAnimator4.mAnimatedValue + 0.1f, this.mRampAnimator.mTargetValue);
        }
    }

    private void handleQuickDownAnimate() {
        if (this.mRampAnimator.mAnimatedValue > QUICK_FOUR_TARGET) {
            this.mQuickStep = SensorConfig.lcmBrightness2FloatAfterDPC(this.m_QuickdownStep[3]);
            this.gap = this.m_QuickdownGap[3];
            SElog.d(TAG, "quick 1 " + this.mRampAnimator.mAnimatedValue + " gap=" + this.gap + " step=" + this.mQuickStep + " mTargetValue=" + this.mRampAnimator.mTargetValue);
        } else if (this.mRampAnimator.mAnimatedValue >= QUICK_THIRD_TARGET) {
            this.mQuickStep = SensorConfig.lcmBrightness2FloatAfterDPC(this.m_QuickdownStep[2]);
            this.gap = this.m_QuickdownGap[2];
            SElog.d(TAG, "quick 2 " + this.mRampAnimator.mAnimatedValue + " gap=" + this.gap + " step=" + this.mQuickStep + " mTargetValue=" + this.mRampAnimator.mTargetValue);
        } else if (this.mRampAnimator.mAnimatedValue >= QUICK_SECOND_TARGET) {
            this.mQuickStep = SensorConfig.lcmBrightness2FloatAfterDPC(this.m_QuickdownStep[1]);
            this.gap = this.m_QuickdownGap[1];
            SElog.d(TAG, "quick 3 " + this.mRampAnimator.mAnimatedValue + " gap=" + this.gap + " step=" + this.mQuickStep + " mTargetValue=" + this.mRampAnimator.mTargetValue);
        } else if (this.mRampAnimator.mAnimatedValue >= QUICK_FIRST_TARGET) {
            this.mQuickStep = SensorConfig.lcmBrightness2FloatAfterDPC(this.m_QuickdownStep[0]);
            this.gap = this.m_QuickdownGap[0];
            SElog.d(TAG, "quick 4 " + this.mRampAnimator.mAnimatedValue + " gap=" + this.gap + " step=" + this.mQuickStep + " mTargetValue=" + this.mRampAnimator.mTargetValue);
        } else {
            this.mQuickStep = 0.001f;
            this.gap = 20;
            SElog.d(TAG, "quick 5 " + this.mRampAnimator.mAnimatedValue + " gap=" + this.gap + " step=" + this.mQuickStep + " mTargetValue= " + this.mRampAnimator.mTargetValue);
        }
        int delta_lcm = Math.abs(SensorConfig.float2LcmBrightnessAfterDPC(this.mRampAnimator.mAnimatedValue) - SensorConfig.float2LcmBrightnessAfterDPC(this.mRampAnimator.mTargetValue));
        if (this.mRampAnimator.mTargetValue >= QUICK_SECOND_TARGET) {
            if (delta_lcm <= 20 || delta_lcm <= SensorConfig.quickStepToLcmValue(this.mQuickStep)) {
                float f = this.mQuickStep;
                this.mQuickStep = Math.min(f, SensorConfig.lcmBrightness2FloatAfterDPC(Math.max(1, Math.round(SensorConfig.quickStepToLcmValue(f) / 20))));
                this.gap = 40;
                SElog.d(TAG, "quick 6 " + this.mRampAnimator.mAnimatedValue + " gap= " + this.gap + " step=" + this.mQuickStep + " mTargetValue " + this.mRampAnimator.mTargetValue + "delta_lcm=ll" + delta_lcm);
            }
        } else if (delta_lcm <= 5 || delta_lcm <= SensorConfig.quickStepToLcmValue(this.mQuickStep)) {
            float f2 = this.mQuickStep;
            this.mQuickStep = Math.min(f2, SensorConfig.lcmBrightness2FloatAfterDPC(Math.max(1, Math.round(SensorConfig.quickStepToLcmValue(f2) / 5))));
            this.gap = 40;
            SElog.d(TAG, "quick 7 " + this.mRampAnimator.mAnimatedValue + " gap=" + this.gap + " step=" + this.mQuickStep + " mTargetValue= " + this.mRampAnimator.mTargetValue + " delta_lcm=" + delta_lcm);
        }
        RampAnimator rampAnimator = this.mRampAnimator;
        rampAnimator.mAnimatedValue = Math.max(rampAnimator.mAnimatedValue - this.mQuickStep, this.mRampAnimator.mTargetValue);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleQuickAnimate() {
        this.gap = 20;
        if (this.mRampAnimator.mTargetValue > this.mRampAnimator.mCurrentValue) {
            handleQuickUpAnimate();
        } else if (this.mRampAnimator.mTargetValue < this.mRampAnimator.mCurrentValue) {
            handleQuickDownAnimate();
        }
    }

    private void handleAutoUpAnimate() {
        float temp_value = getUpStepAndGap(this.mRampAnimator.mCurrentValue);
        this.gap = mUpGap;
        mCurrentDownSection = -1;
        mIsChangeingUp = true;
        if (this.mRampAnimator.mAnimatedValue >= temp_value) {
            RampAnimator rampAnimator = this.mRampAnimator;
            rampAnimator.mAnimatedValue = Math.min(rampAnimator.mAnimatedValue + 0.001f, this.mRampAnimator.mTargetValue);
        } else {
            RampAnimator rampAnimator2 = this.mRampAnimator;
            rampAnimator2.mAnimatedValue = Math.min(temp_value, rampAnimator2.mTargetValue);
        }
        SElog.d(TAG, "up " + mUpGap + "step " + mUpStepNit + " mAnimatedValue " + this.mRampAnimator.mAnimatedValue + "mTargetValue" + this.mRampAnimator.mTargetValue);
    }

    private void handleAutoDownAnimate() {
        if (this.mIsUserDownStepConfig) {
            handleAutoDownStepAnimate();
        } else {
            handleAutoDownCurveAnimate();
        }
    }

    private void handleAutoDownStepAnimate() {
        float step;
        if (this.mRampAnimator.mAnimatedValue > QUICK_FOUR_TARGET) {
            step = SensorConfig.lcmBrightness2FloatAfterDPC(this.mDownStepByStep[3]);
            this.gap = this.mDownGapByStep[3];
            mDownStep = this.mDownStepByStep[3];
        } else if (this.mRampAnimator.mAnimatedValue >= QUICK_THIRD_TARGET) {
            step = SensorConfig.lcmBrightness2FloatAfterDPC(this.mDownStepByStep[2]);
            this.gap = this.mDownGapByStep[2];
            mDownStep = this.mDownStepByStep[2];
        } else if (this.mRampAnimator.mAnimatedValue >= QUICK_SECOND_TARGET) {
            step = SensorConfig.lcmBrightness2FloatAfterDPC(this.mDownStepByStep[1]);
            this.gap = this.mDownGapByStep[1];
            mDownStep = this.mDownStepByStep[1];
        } else if (this.mRampAnimator.mAnimatedValue >= QUICK_FIRST_TARGET) {
            step = SensorConfig.lcmBrightness2FloatAfterDPC(this.mDownStepByStep[0]);
            this.gap = this.mDownGapByStep[0];
            mDownStep = this.mDownStepByStep[0];
        } else {
            step = 0.001f;
            this.gap = 20;
            mDownStep = 2;
        }
        RampAnimator rampAnimator = this.mRampAnimator;
        rampAnimator.mAnimatedValue = Math.max(rampAnimator.mAnimatedValue - step, this.mRampAnimator.mTargetValue);
        if (this.mRampAnimator.mAnimatedValue <= SensorConfig.lcmBrightness2FloatAfterDPC(this.mDownForLcmFlickIssue)) {
            RampAnimator rampAnimator2 = this.mRampAnimator;
            rampAnimator2.mAnimatedValue = rampAnimator2.mTargetValue;
        }
        SElog.d(TAG, "downByStep animate" + this.mRampAnimator.mAnimatedValue + "mDownStep" + mDownStep + " gap " + this.gap + " mTargetValue" + this.mRampAnimator.mTargetValue);
    }

    private void handleAutoDownCurveAnimate() {
        int temp_lcm = SensorConfig.float2LcmBrightnessAfterDPC(this.mRampAnimator.mCurrentValue);
        int animate_lcm = getDownStepAndGap(temp_lcm);
        mIsChangeingUp = false;
        this.gap = mDownGap;
        if (this.mRampAnimator.mAnimatedValue <= SensorConfig.lcmBrightness2FloatAfterDPC(animate_lcm)) {
            RampAnimator rampAnimator = this.mRampAnimator;
            rampAnimator.mAnimatedValue = Math.max(rampAnimator.mAnimatedValue - 0.001f, this.mRampAnimator.mTargetValue);
        } else {
            this.mRampAnimator.mAnimatedValue = Math.max(SensorConfig.lcmBrightness2FloatAfterDPC(animate_lcm), this.mRampAnimator.mTargetValue);
        }
        if (this.mRampAnimator.mAnimatedValue <= SensorConfig.lcmBrightness2FloatAfterDPC(this.mDownForLcmFlickIssue)) {
            RampAnimator rampAnimator2 = this.mRampAnimator;
            rampAnimator2.mAnimatedValue = rampAnimator2.mTargetValue;
        }
        SElog.d(TAG, "down" + this.mRampAnimator.mAnimatedValue + "animate_lcm" + animate_lcm + " gap " + this.gap + " mTargetValue" + this.mRampAnimator.mTargetValue);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleDimAnimate() {
        if (this.mDimStep <= 0) {
            RampAnimator rampAnimator = this.mRampAnimator;
            rampAnimator.mAnimatedValue = rampAnimator.mTargetValue;
            return;
        }
        float temp = this.mRampAnimator.mAnimatedValue - SensorConfig.lcmBrightness2FloatAfterDPC(this.mDimStep);
        this.gap = this.mDimGap;
        if (SensorConfig.floatEquals(this.mRampAnimator.mAnimatedValue, temp)) {
            RampAnimator rampAnimator2 = this.mRampAnimator;
            rampAnimator2.mAnimatedValue = Math.max(rampAnimator2.mAnimatedValue + 0.001f, this.mRampAnimator.mTargetValue);
        } else {
            RampAnimator rampAnimator3 = this.mRampAnimator;
            rampAnimator3.mAnimatedValue = Math.max(temp, rampAnimator3.mTargetValue);
        }
        SElog.d(TAG, "dim" + this.mRampAnimator.mAnimatedValue + " gap " + this.gap + " step" + this.mDimStep);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleAutoAnimate() {
        if (this.mRampAnimator.mTargetValue > this.mRampAnimator.mCurrentValue) {
            handleAutoUpAnimate();
        } else if (this.mRampAnimator.mTargetValue < this.mRampAnimator.mCurrentValue) {
            handleAutoDownAnimate();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handlePemAnimate() {
        mCurrentDownSection = -1;
        mIsChangeingUp = false;
        if (this.mRampAnimator.mTargetValue > this.mRampAnimator.mCurrentValue) {
            this.gap = this.mPemBrightnessGap;
            int lcm_brightness = SensorConfig.float2LcmBrightnessAfterDPC(this.mRampAnimator.mCurrentValue);
            int animatedValue = this.mPemBrightnessStep + lcm_brightness;
            float temp = SensorConfig.lcmBrightness2FloatAfterDPC(animatedValue);
            if (SensorConfig.floatEquals(this.mRampAnimator.mAnimatedValue, temp)) {
                RampAnimator rampAnimator = this.mRampAnimator;
                rampAnimator.mAnimatedValue = Math.min(rampAnimator.mAnimatedValue + 0.001f, this.mRampAnimator.mTargetValue);
            } else {
                RampAnimator rampAnimator2 = this.mRampAnimator;
                rampAnimator2.mAnimatedValue = Math.min(temp, rampAnimator2.mTargetValue);
            }
            SElog.d(TAG, "pem up" + this.mRampAnimator.mAnimatedValue + " gap " + this.gap + " mPemBrightnessStep " + this.mPemBrightnessStep + " animatedValue " + animatedValue + "mTargetValue " + this.mRampAnimator.mTargetValue);
        } else if (this.mRampAnimator.mTargetValue < this.mRampAnimator.mCurrentValue) {
            this.gap = this.mPemBrightnessGap;
            int lcm_brightness2 = SensorConfig.float2LcmBrightnessAfterDPC(this.mRampAnimator.mCurrentValue);
            int animatedValue2 = lcm_brightness2 - this.mPemBrightnessStep;
            float temp2 = SensorConfig.lcmBrightness2FloatAfterDPC(animatedValue2);
            if (SensorConfig.floatEquals(this.mRampAnimator.mAnimatedValue, temp2)) {
                RampAnimator rampAnimator3 = this.mRampAnimator;
                rampAnimator3.mAnimatedValue = Math.max(rampAnimator3.mAnimatedValue - 0.001f, this.mRampAnimator.mTargetValue);
            } else {
                RampAnimator rampAnimator4 = this.mRampAnimator;
                rampAnimator4.mAnimatedValue = Math.max(temp2, rampAnimator4.mTargetValue);
            }
            SElog.d(TAG, "pem down" + this.mRampAnimator.mAnimatedValue + " gap " + this.gap + " mPemBrightnessStep " + this.mPemBrightnessStep + " animatedValue " + animatedValue2 + " mTargetValue " + this.mRampAnimator.mTargetValue);
        }
        if (this.mRequestNeedStopAnimate) {
            RampAnimator rampAnimator5 = this.mRampAnimator;
            rampAnimator5.mTargetValue = rampAnimator5.mAnimatedValue;
            RampAnimator rampAnimator6 = this.mRampAnimator;
            rampAnimator6.mCurrentValue = rampAnimator6.mAnimatedValue;
            SElog.d(TAG, "setting issue " + this.mRampAnimator.mAnimatedValue + "mPemBrightnessStepmTargetValue " + this.mRampAnimator.mTargetValue + "mRequestNeedStopAnimate" + this.mRequestNeedStopAnimate);
        }
    }

    /* loaded from: classes.dex */
    private class AnimationCallbackForNew extends AnimationCallback {
        private AnimationCallbackForNew() {
            super();
        }

        @Override // java.lang.Runnable
        public void run() {
            float scale = ValueAnimator.getDurationScale();
            if (scale == 0.0f) {
                VivoRampAnimatorImpl.this.mRampAnimator.mAnimatedValue = VivoRampAnimatorImpl.this.mRampAnimator.mTargetValue;
            } else if (VivoRampAnimatorImpl.this.mRampAnimator.mRate == 500.0f || VivoRampAnimatorImpl.this.isPemAnimationRunning()) {
                VivoRampAnimatorImpl.this.handlePemAnimate();
            } else if (VivoRampAnimatorImpl.this.mRampAnimator.mRate == 200.0f) {
                VivoRampAnimatorImpl.this.handleQuickAnimate();
            } else if (VivoRampAnimatorImpl.this.mRampAnimator.mRate == 600.0f) {
                VivoRampAnimatorImpl.this.handleDimAnimate();
            } else {
                VivoRampAnimatorImpl.this.handleAutoAnimate();
            }
            float oldCurrentValue = VivoRampAnimatorImpl.this.mRampAnimator.mCurrentValue;
            if (VivoRampAnimatorImpl.this.mRampAnimator.mAnimatedValue > 1.0f) {
                VivoRampAnimatorImpl.this.mRampAnimator.mAnimatedValue = 1.0f;
            }
            VivoRampAnimatorImpl.this.mRampAnimator.mCurrentValue = VivoRampAnimatorImpl.this.mRampAnimator.mAnimatedValue;
            if (!SensorConfig.floatEquals(oldCurrentValue, VivoRampAnimatorImpl.this.mRampAnimator.mCurrentValue)) {
                VivoRampAnimatorImpl.this.mRampAnimator.mProperty.setValue(VivoRampAnimatorImpl.this.mRampAnimator.mObject, VivoRampAnimatorImpl.this.mRampAnimator.mCurrentValue);
                if (VivoRampAnimatorImpl.this.mRampAnimator.mRate != 500.0f) {
                    if (VivoRampAnimatorImpl.this.mRampAnimator.mTargetValue > VivoRampAnimatorImpl.this.mRampAnimator.mCurrentValue) {
                        float setting_float = Math.min(VivoRampAnimatorImpl.this.calcSettingStep(), VivoRampAnimatorImpl.this.mRampAnimator.mGoblalTarget);
                        VivoRampAnimatorImpl.this.syncBrightnessSettings(setting_float);
                    } else if (VivoRampAnimatorImpl.this.mRampAnimator.mTargetValue < VivoRampAnimatorImpl.this.mRampAnimator.mCurrentValue) {
                        float setting_float2 = Math.max(VivoRampAnimatorImpl.this.calcSettingStep(), VivoRampAnimatorImpl.this.mRampAnimator.mGoblalTarget);
                        VivoRampAnimatorImpl.this.syncBrightnessSettings(setting_float2);
                    }
                }
            }
            if (!SensorConfig.floatEquals(VivoRampAnimatorImpl.this.mRampAnimator.mTargetValue, VivoRampAnimatorImpl.this.mRampAnimator.mCurrentValue)) {
                VivoRampAnimatorImpl vivoRampAnimatorImpl = VivoRampAnimatorImpl.this;
                vivoRampAnimatorImpl.postAnimationCallbackDelayed(vivoRampAnimatorImpl.gap);
                return;
            }
            VivoRampAnimatorImpl vivoRampAnimatorImpl2 = VivoRampAnimatorImpl.this;
            vivoRampAnimatorImpl2.syncBrightnessSettings(vivoRampAnimatorImpl2.mRampAnimator.mGoblalTarget);
            VivoRampAnimatorImpl.this.mRampAnimator.mAnimating = false;
            VivoRampAnimatorImpl.mIsChangeingUp = false;
            if ((!VivoRampAnimatorImpl.this.mRequestNeedStopAnimate || VivoRampAnimatorImpl.this.mRampAnimator.mRate != 500.0f) && VivoRampAnimatorImpl.this.mRampAnimator.mListener != null) {
                VivoRampAnimatorImpl.this.mRampAnimator.mListener.onAnimationEnd();
            }
        }
    }
}