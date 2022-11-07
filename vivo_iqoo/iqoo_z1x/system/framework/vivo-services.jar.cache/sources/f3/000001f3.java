package com.android.server.display.color;

import android.content.Context;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import java.lang.reflect.Array;
import vivo.util.VSlog;

/* loaded from: classes.dex */
class ExynosDisplayFactoryHDR {
    private static final int LOOP_CNT = 150;
    private static final int MODE_CNT = 2;
    private static final int MSG_SIGNAL_REFRESH = 1;
    private static final int MSG_SYSFS_WRITE_CGC17_ENC = 3;
    private static final int MSG_SYSFS_WRITE_CGC17_IDX = 2;
    private static final String TAG = "ExynosDisplayFactoryHDR";
    private Context mContext;
    private int[][] mCountDownTimerTable;
    private String mFactoryXMLMode;
    private String mFactoryXMLPath;
    private Handler mLocalHandler;
    private final boolean DEBUG = "eng".equals(Build.TYPE);
    private CountDownTimer mCountdownTimer = null;
    private int mTimeoutMs = 6000;
    private int mIntervalMs = 40;
    private int mCountDownTimerCount = 0;
    private String XML_SYSFS_PATH = "/sys/class/dqe/dqe/xml";
    private String HDR_DQE_XML_PATH = "/vendor/etc/dqe/DQE_coef_data_default.xml";
    private String MODE_IDX_SYSFS_PATH = "/sys/class/dqe/hdr/mode_idx";
    private String CGC17_IDX_SYSFS_PATH = "/sys/class/dqe/hdr/cgc17_idx";
    private String CGC17_ENC_SYSFS_PATH = "/sys/class/dqe/hdr/cgc17_enc";
    private String CGC17_DEC_SYSFS_PATH = "/sys/class/dqe/hdr/cgc17_dec";
    private String CGC17_CON_SYSFS_PATH = "/sys/class/dqe/hdr/cgc17_con";
    private String DEGAMMA_SFR_SYSFS_PATH = "/sys/class/dqe/hdr/degamma";
    private String GAMMA_SFR_SYSFS_PATH = "/sys/class/dqe/hdr/gamma";
    private String GAMMA_MATRIX_SYSFS_PATH = "/sys/class/dqe/hdr/gamma_matrix";
    private String HSC48_IDX_SYSFS_PATH = "/sys/class/dqe/hdr/hsc48_idx";
    private String HSC48_LCG_SYSFS_PATH = "/sys/class/dqe/hdr/hsc48_lcg";
    private String HSC_SFR_SYSFS_PATH = "/sys/class/dqe/hdr/hsc";
    private String SCL_SFR_SYSFS_PATH = "/sys/class/dqe/hdr/scl";
    private String APS_SYSFS_PATH = "/sys/class/dqe/hdr/aps";
    private ExynosDisplayATC mExynosDisplayATC = null;
    private String[] mColorModeSettingTable = {"hdr10", "hdr10p"};
    private Handler mHandler = new Handler() { // from class: com.android.server.display.color.ExynosDisplayFactoryHDR.2
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int i = msg.what;
            if (i == 1) {
                ExynosDisplayUtils.sendEmptyUpdate();
            } else if (i == 2) {
                ExynosDisplayFactoryHDR.this.sysfsWriteCGC17_IDX(msg.arg1);
            } else if (i == 3) {
                ExynosDisplayFactoryHDR.this.sysfsWriteCGC17_ENC(msg.obj.toString());
            }
        }
    };

    static /* synthetic */ int access$408(ExynosDisplayFactoryHDR x0) {
        int i = x0.mCountDownTimerCount;
        x0.mCountDownTimerCount = i + 1;
        return i;
    }

    public ExynosDisplayFactoryHDR(Context context) {
        this.mCountDownTimerTable = new int[][]{new int[]{0}, new int[]{0}};
        this.mFactoryXMLPath = null;
        this.mFactoryXMLMode = null;
        this.mContext = context;
        this.mLocalHandler = new Handler(context.getMainLooper());
        this.mCountDownTimerTable = (int[][]) Array.newInstance(int.class, 2, LOOP_CNT);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < LOOP_CNT; j++) {
                this.mCountDownTimerTable[i][j] = 0;
            }
        }
        initCountDownTimer();
        this.mFactoryXMLPath = null;
        this.mFactoryXMLMode = "hdr10";
        this.mLocalHandler.postDelayed(new Runnable() { // from class: com.android.server.display.color.ExynosDisplayFactoryHDR.1
            @Override // java.lang.Runnable
            public void run() {
                ExynosDisplayFactoryHDR exynosDisplayFactoryHDR = ExynosDisplayFactoryHDR.this;
                exynosDisplayFactoryHDR.startCountDownTimer(exynosDisplayFactoryHDR.mFactoryXMLPath, ExynosDisplayFactoryHDR.this.mFactoryXMLMode);
            }
        }, 0L);
    }

    protected void setExynosDisplayATC(ExynosDisplayATC mATC) {
        this.mExynosDisplayATC = mATC;
    }

    private void sysfsWriteMODE_IDX(int idx) {
        String mode_idx = Integer.toString(idx);
        ExynosDisplayUtils.sysfsWriteSting(this.MODE_IDX_SYSFS_PATH, mode_idx);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sysfsWriteCGC17_IDX(int count) {
        int rgb = count / 17;
        int idx = count % 17;
        String cgc17_idx = Integer.toString(rgb) + " " + Integer.toString(idx);
        ExynosDisplayUtils.sysfsWriteSting(this.CGC17_IDX_SYSFS_PATH, cgc17_idx);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sysfsWriteCGC17_ENC(String enc) {
        ExynosDisplayUtils.sysfsWriteSting(this.CGC17_ENC_SYSFS_PATH, enc);
    }

    private void sysfsWriteCGC17_DEC(String dec) {
        ExynosDisplayUtils.sysfsWriteSting(this.CGC17_DEC_SYSFS_PATH, dec);
    }

    private void sysfsWriteCGC17_CON(String con) {
        String sysfs = ExynosDisplayUtils.getStringFromFile(this.CGC17_CON_SYSFS_PATH);
        if (sysfs != null && !sysfs.equals("0")) {
            ExynosDisplayUtils.sysfsWriteSting(this.CGC17_CON_SYSFS_PATH, con);
        }
    }

    private void sysfsWriteDEGAMMA(String degamma) {
        String sysfs = ExynosDisplayUtils.getStringFromFile(this.DEGAMMA_SFR_SYSFS_PATH);
        if (sysfs != null && !sysfs.equals("0")) {
            ExynosDisplayUtils.sysfsWriteSting(this.DEGAMMA_SFR_SYSFS_PATH, degamma);
        }
    }

    private void sysfsWriteGAMMA(String gamma) {
        String sysfs = ExynosDisplayUtils.getStringFromFile(this.GAMMA_SFR_SYSFS_PATH);
        if (sysfs != null && !sysfs.equals("0")) {
            ExynosDisplayUtils.sysfsWriteSting(this.GAMMA_SFR_SYSFS_PATH, gamma);
        }
    }

    private void sysfsWriteGAMMA_MATRIX(String gamma_matrix) {
        String sysfs = ExynosDisplayUtils.getStringFromFile(this.GAMMA_MATRIX_SYSFS_PATH);
        if (sysfs != null && !sysfs.equals("0")) {
            ExynosDisplayUtils.sysfsWriteSting(this.GAMMA_MATRIX_SYSFS_PATH, gamma_matrix);
        }
    }

    private void sysfsWriteHSC48_IDX(int idx) {
        String hsc48_idx = Integer.toString(idx);
        ExynosDisplayUtils.sysfsWriteSting(this.HSC48_IDX_SYSFS_PATH, hsc48_idx);
    }

    private void sysfsWriteHSC48_LCG(String lcg) {
        ExynosDisplayUtils.sysfsWriteSting(this.HSC48_LCG_SYSFS_PATH, lcg);
    }

    private void sysfsWriteHSC(String hsc) {
        String sysfs = ExynosDisplayUtils.getStringFromFile(this.HSC_SFR_SYSFS_PATH);
        if (sysfs != null && !sysfs.equals("0")) {
            ExynosDisplayUtils.sysfsWriteSting(this.HSC_SFR_SYSFS_PATH, hsc);
        }
    }

    private void sysfsWriteSCL(String scl) {
        String sysfs = ExynosDisplayUtils.getStringFromFile(this.SCL_SFR_SYSFS_PATH);
        if (sysfs != null && !sysfs.equals("0")) {
            ExynosDisplayUtils.sysfsWriteSting(this.SCL_SFR_SYSFS_PATH, scl);
        }
    }

    private void sysfsWriteAPS(String aps) {
        String sysfs = ExynosDisplayUtils.getStringFromFile(this.APS_SYSFS_PATH);
        if (sysfs != null && !sysfs.equals("0")) {
            ExynosDisplayUtils.sysfsWriteSting(this.APS_SYSFS_PATH, aps);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setCalibrationMODE_IDX(int idx) {
        try {
            sysfsWriteMODE_IDX(idx);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setCalibrationCGC17_ENC(String xml_path, String mode_name, int count) {
        VSlog.d(TAG, "setCalibrationCGC17_ENC + " + count);
        for (int idx = 0; idx < 17; idx++) {
            try {
                String[] temp_array = ExynosDisplayUtils.parserFactoryXMLText(xml_path, mode_name, "cgc17_enc", count, idx);
                if (temp_array != null && temp_array.length >= 1) {
                    String cgc17_enc = temp_array[0];
                    sysfsWriteCGC17_IDX((count * 17) + idx);
                    sysfsWriteCGC17_ENC(cgc17_enc);
                }
                VSlog.d(TAG, "xml cgc17_enc not found");
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        VSlog.d(TAG, "setCalibrationCGC17_ENC - " + count);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setCalibrationCGC17_DEC() {
        VSlog.d(TAG, "setCalibrationCGC17_DEC");
        sysfsWriteCGC17_DEC("7");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setCalibrationCGC17_CON(String xml_path, String mode_name) {
        VSlog.d(TAG, "setCalibrationCGC17_CON");
        try {
            String[] temp_array = ExynosDisplayUtils.parserFactoryXMLText(xml_path, mode_name, "cgc17_con", 0, 0);
            if (temp_array == null) {
                VSlog.d(TAG, "xml cgc17_con not found");
            } else if (temp_array.length < 1) {
                VSlog.d(TAG, "xml array size wrong: " + temp_array.length);
            } else {
                String cgc17_con = temp_array[0];
                sysfsWriteCGC17_CON(cgc17_con);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setCalibrationDEGAMMA(String xml_path, String mode_name) {
        VSlog.d(TAG, "setCalibrationDEGAMMA");
        try {
            String[] temp_array = ExynosDisplayUtils.parserFactoryXMLText(xml_path, mode_name, "degamma", 0, 0);
            if (temp_array == null) {
                VSlog.d(TAG, "xml degamma not found");
            } else if (temp_array.length < 1) {
                VSlog.d(TAG, "xml array size wrong: " + temp_array.length);
            } else {
                String degamma = temp_array[0];
                sysfsWriteDEGAMMA(degamma);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setCalibrationGAMMA(String xml_path, String mode_name) {
        VSlog.d(TAG, "setCalibrationGAMMA");
        try {
            String[] temp_array = ExynosDisplayUtils.parserFactoryXMLText(xml_path, mode_name, "gamma", 0, 0);
            if (temp_array == null) {
                VSlog.d(TAG, "xml gamma not found");
            } else if (temp_array.length < 1) {
                VSlog.d(TAG, "xml array size wrong: " + temp_array.length);
            } else {
                String gamma = temp_array[0];
                sysfsWriteGAMMA(gamma);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setCalibrationGAMMA_MATRIX(String xml_path, String mode_name) {
        VSlog.d(TAG, "setCalibrationGAMMA_MATRIX");
        try {
            String[] temp_array = ExynosDisplayUtils.parserFactoryXMLText(xml_path, mode_name, "gamma_matrix", 0, 0);
            if (temp_array == null) {
                VSlog.d(TAG, "xml gamma_matrix not found");
            } else if (temp_array.length < 1) {
                VSlog.d(TAG, "xml array size wrong: " + temp_array.length);
            } else {
                String gamma_matrix = temp_array[0];
                sysfsWriteGAMMA_MATRIX(gamma_matrix);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setCalibrationHSC48_LCG(String xml_path, String mode_name, int count) {
        VSlog.d(TAG, "setCalibrationHSC48_LCG: " + count);
        for (int idx = 0; idx < 3; idx++) {
            try {
                String[] temp_array = ExynosDisplayUtils.parserFactoryXMLText(xml_path, mode_name, "hsc48_lcg", idx, 0);
                if (temp_array != null && temp_array.length >= 1) {
                    String hsc48_lcg = temp_array[0];
                    sysfsWriteHSC48_IDX(idx);
                    sysfsWriteHSC48_LCG(hsc48_lcg);
                }
                VSlog.d(TAG, "xml hsc48_lcg not found");
                return;
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setCalibrationHSC(String xml_path, String mode_name) {
        VSlog.d(TAG, "setCalibrationHSC");
        try {
            String[] temp_array = ExynosDisplayUtils.parserFactoryXMLText(xml_path, mode_name, "hsc", 0, 0);
            if (temp_array == null) {
                VSlog.d(TAG, "xml hsc not found");
            } else if (temp_array.length < 1) {
                VSlog.d(TAG, "xml array size wrong: " + temp_array.length);
            } else {
                String hsc = temp_array[0];
                sysfsWriteHSC(hsc);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setCalibrationSCL(String xml_path, String mode_name) {
        VSlog.d(TAG, "setCalibrationSCL");
        try {
            String[] temp_array = ExynosDisplayUtils.parserFactoryXMLText(xml_path, mode_name, "scl", 0, 0);
            if (temp_array == null) {
                VSlog.d(TAG, "xml scl not found");
            } else if (temp_array.length < 1) {
                VSlog.d(TAG, "xml array size wrong: " + temp_array.length);
            } else {
                String scl = temp_array[0];
                sysfsWriteSCL(scl);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setCalibrationAPS(String xml_path, String mode_name) {
        VSlog.d(TAG, "setCalibrationAPS");
        try {
            String[] temp_array = ExynosDisplayUtils.parserFactoryXMLText(xml_path, mode_name, "aps", 0, 0);
            if (temp_array == null) {
                VSlog.d(TAG, "xml aps not found");
            } else if (temp_array.length < 1) {
                VSlog.d(TAG, "xml array size wrong: " + temp_array.length);
            } else {
                String aps = temp_array[0];
                sysfsWriteAPS(aps);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initCountDownTimer() {
        this.mCountdownTimer = new CountDownTimer(this.mTimeoutMs, this.mIntervalMs) { // from class: com.android.server.display.color.ExynosDisplayFactoryHDR.3
            @Override // android.os.CountDownTimer
            public void onTick(long millisUntilFinished) {
                if (ExynosDisplayFactoryHDR.this.mCountDownTimerCount > 0) {
                    if (ExynosDisplayFactoryHDR.this.mCountDownTimerCount > 1) {
                        if (ExynosDisplayFactoryHDR.this.mCountDownTimerCount > 2) {
                            if (ExynosDisplayFactoryHDR.this.mCountDownTimerCount > 3) {
                                if (ExynosDisplayFactoryHDR.this.mCountDownTimerCount > 4) {
                                    if (ExynosDisplayFactoryHDR.this.mCountDownTimerCount > 5) {
                                        if (ExynosDisplayFactoryHDR.this.mCountDownTimerCount > 6) {
                                            if (ExynosDisplayFactoryHDR.this.mCountDownTimerCount > 9) {
                                                if (ExynosDisplayFactoryHDR.this.mCountDownTimerCount > 10) {
                                                    if (ExynosDisplayFactoryHDR.this.mCountDownTimerCount <= 11) {
                                                        for (int i = 0; i < 2; i++) {
                                                            if (ExynosDisplayFactoryHDR.this.mCountDownTimerTable[i][ExynosDisplayFactoryHDR.this.mCountDownTimerCount] != 0) {
                                                                ExynosDisplayFactoryHDR.this.setCalibrationMODE_IDX(i);
                                                                ExynosDisplayFactoryHDR exynosDisplayFactoryHDR = ExynosDisplayFactoryHDR.this;
                                                                exynosDisplayFactoryHDR.setCalibrationCGC17_CON(exynosDisplayFactoryHDR.mFactoryXMLPath, ExynosDisplayFactoryHDR.this.mColorModeSettingTable[i]);
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    for (int i2 = 0; i2 < 2; i2++) {
                                                        if (ExynosDisplayFactoryHDR.this.mCountDownTimerTable[i2][ExynosDisplayFactoryHDR.this.mCountDownTimerCount] != 0) {
                                                            ExynosDisplayFactoryHDR.this.setCalibrationMODE_IDX(i2);
                                                            ExynosDisplayFactoryHDR.this.setCalibrationCGC17_DEC();
                                                        }
                                                    }
                                                }
                                            } else {
                                                for (int i3 = 0; i3 < 2; i3++) {
                                                    if (ExynosDisplayFactoryHDR.this.mCountDownTimerTable[i3][ExynosDisplayFactoryHDR.this.mCountDownTimerCount] != 0) {
                                                        ExynosDisplayFactoryHDR.this.setCalibrationMODE_IDX(i3);
                                                        ExynosDisplayFactoryHDR exynosDisplayFactoryHDR2 = ExynosDisplayFactoryHDR.this;
                                                        exynosDisplayFactoryHDR2.setCalibrationCGC17_ENC(exynosDisplayFactoryHDR2.mFactoryXMLPath, ExynosDisplayFactoryHDR.this.mColorModeSettingTable[i3], ExynosDisplayFactoryHDR.this.mCountDownTimerCount - 7);
                                                    }
                                                }
                                            }
                                        } else {
                                            for (int i4 = 0; i4 < 2; i4++) {
                                                if (ExynosDisplayFactoryHDR.this.mCountDownTimerTable[i4][ExynosDisplayFactoryHDR.this.mCountDownTimerCount] != 0) {
                                                    ExynosDisplayFactoryHDR.this.setCalibrationMODE_IDX(i4);
                                                    ExynosDisplayFactoryHDR exynosDisplayFactoryHDR3 = ExynosDisplayFactoryHDR.this;
                                                    exynosDisplayFactoryHDR3.setCalibrationSCL(exynosDisplayFactoryHDR3.mFactoryXMLPath, ExynosDisplayFactoryHDR.this.mColorModeSettingTable[i4]);
                                                }
                                            }
                                        }
                                    } else {
                                        for (int i5 = 0; i5 < 2; i5++) {
                                            if (ExynosDisplayFactoryHDR.this.mCountDownTimerTable[i5][ExynosDisplayFactoryHDR.this.mCountDownTimerCount] != 0) {
                                                ExynosDisplayFactoryHDR.this.setCalibrationMODE_IDX(i5);
                                                ExynosDisplayFactoryHDR exynosDisplayFactoryHDR4 = ExynosDisplayFactoryHDR.this;
                                                exynosDisplayFactoryHDR4.setCalibrationHSC(exynosDisplayFactoryHDR4.mFactoryXMLPath, ExynosDisplayFactoryHDR.this.mColorModeSettingTable[i5]);
                                            }
                                        }
                                    }
                                } else {
                                    for (int i6 = 0; i6 < 2; i6++) {
                                        if (ExynosDisplayFactoryHDR.this.mCountDownTimerTable[i6][ExynosDisplayFactoryHDR.this.mCountDownTimerCount] != 0) {
                                            ExynosDisplayFactoryHDR.this.setCalibrationMODE_IDX(i6);
                                            ExynosDisplayFactoryHDR exynosDisplayFactoryHDR5 = ExynosDisplayFactoryHDR.this;
                                            exynosDisplayFactoryHDR5.setCalibrationHSC48_LCG(exynosDisplayFactoryHDR5.mFactoryXMLPath, ExynosDisplayFactoryHDR.this.mColorModeSettingTable[i6], ExynosDisplayFactoryHDR.this.mCountDownTimerCount - 4);
                                        }
                                    }
                                }
                            } else {
                                for (int i7 = 0; i7 < 2; i7++) {
                                    if (ExynosDisplayFactoryHDR.this.mCountDownTimerTable[i7][ExynosDisplayFactoryHDR.this.mCountDownTimerCount] != 0) {
                                        ExynosDisplayFactoryHDR.this.setCalibrationMODE_IDX(i7);
                                        ExynosDisplayFactoryHDR exynosDisplayFactoryHDR6 = ExynosDisplayFactoryHDR.this;
                                        exynosDisplayFactoryHDR6.setCalibrationGAMMA_MATRIX(exynosDisplayFactoryHDR6.mFactoryXMLPath, ExynosDisplayFactoryHDR.this.mColorModeSettingTable[i7]);
                                    }
                                }
                            }
                        } else {
                            for (int i8 = 0; i8 < 2; i8++) {
                                if (ExynosDisplayFactoryHDR.this.mCountDownTimerTable[i8][ExynosDisplayFactoryHDR.this.mCountDownTimerCount] != 0) {
                                    ExynosDisplayFactoryHDR.this.setCalibrationMODE_IDX(i8);
                                    ExynosDisplayFactoryHDR exynosDisplayFactoryHDR7 = ExynosDisplayFactoryHDR.this;
                                    exynosDisplayFactoryHDR7.setCalibrationGAMMA(exynosDisplayFactoryHDR7.mFactoryXMLPath, ExynosDisplayFactoryHDR.this.mColorModeSettingTable[i8]);
                                }
                            }
                        }
                    } else {
                        for (int i9 = 0; i9 < 2; i9++) {
                            if (ExynosDisplayFactoryHDR.this.mCountDownTimerTable[i9][ExynosDisplayFactoryHDR.this.mCountDownTimerCount] != 0) {
                                ExynosDisplayFactoryHDR.this.setCalibrationMODE_IDX(i9);
                                ExynosDisplayFactoryHDR exynosDisplayFactoryHDR8 = ExynosDisplayFactoryHDR.this;
                                exynosDisplayFactoryHDR8.setCalibrationDEGAMMA(exynosDisplayFactoryHDR8.mFactoryXMLPath, ExynosDisplayFactoryHDR.this.mColorModeSettingTable[i9]);
                            }
                        }
                    }
                } else {
                    for (int i10 = 0; i10 < 2; i10++) {
                        if (ExynosDisplayFactoryHDR.this.mCountDownTimerTable[i10][ExynosDisplayFactoryHDR.this.mCountDownTimerCount] != 0) {
                            ExynosDisplayFactoryHDR.this.setCalibrationMODE_IDX(i10);
                            ExynosDisplayFactoryHDR exynosDisplayFactoryHDR9 = ExynosDisplayFactoryHDR.this;
                            exynosDisplayFactoryHDR9.setCalibrationAPS(exynosDisplayFactoryHDR9.mFactoryXMLPath, ExynosDisplayFactoryHDR.this.mColorModeSettingTable[i10]);
                        }
                    }
                }
                ExynosDisplayFactoryHDR.access$408(ExynosDisplayFactoryHDR.this);
            }

            @Override // android.os.CountDownTimer
            public void onFinish() {
                VSlog.d(ExynosDisplayFactoryHDR.TAG, "CountDownTimer finished = " + ExynosDisplayFactoryHDR.this.mCountDownTimerCount);
                ExynosDisplayFactoryHDR.this.mCountDownTimerCount = 0;
            }
        };
    }

    private int getItemEnable(String xml_path, String mode_name, String node) {
        try {
            String[] temp_array = ExynosDisplayUtils.parserFactoryXMLText(xml_path, mode_name, node, 0, 0);
            if (temp_array == null || temp_array.length < 1) {
                return 0;
            }
            String[] mItem = temp_array[0].split("\\s*,\\s*");
            int item_en = Integer.parseInt(mItem[0]);
            return item_en;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    protected void startCountDownTimer(String xml_path, String mode_name) {
        String xml_path2;
        String[] temp_array;
        if (xml_path == null) {
            xml_path = ExynosDisplayUtils.getStringFromFile(this.XML_SYSFS_PATH);
        }
        if (xml_path == null) {
            return;
        }
        if (ExynosDisplayUtils.existFile(this.HDR_DQE_XML_PATH)) {
            xml_path2 = this.HDR_DQE_XML_PATH;
        } else {
            VSlog.d(TAG, "xml_path=" + xml_path + ", lastIndexOf:" + xml_path.lastIndexOf("/") + " " + xml_path.lastIndexOf("."));
            String mDirPath = xml_path.substring(0, xml_path.lastIndexOf("/"));
            String mFilePath = xml_path.substring(xml_path.lastIndexOf("/") + 1, xml_path.lastIndexOf("."));
            xml_path2 = mDirPath + "/DQE_coef_data_" + mFilePath + ".xml";
        }
        if (mode_name == null) {
            return;
        }
        this.mFactoryXMLPath = xml_path2;
        this.mFactoryXMLMode = mode_name;
        if (!ExynosDisplayUtils.existFile(xml_path2)) {
            return;
        }
        VSlog.d(TAG, "startCountDownTimer: xml_path=" + xml_path2 + ", mode_name=" + mode_name);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < LOOP_CNT; j++) {
                this.mCountDownTimerTable[i][j] = 0;
            }
        }
        try {
            temp_array = ExynosDisplayUtils.parserXMLNodeText(xml_path2, "version");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (temp_array != null && temp_array.length >= 1) {
            VSlog.d(TAG, "xml version: " + temp_array[0]);
            for (int i2 = 0; i2 < 2; i2++) {
                String mColorMode = this.mColorModeSettingTable[i2];
                VSlog.d(TAG, "mode = " + mColorMode);
                int item_en = getItemEnable(xml_path2, mColorMode, "aps");
                VSlog.d(TAG, "aps: enable = " + item_en);
                if (item_en > 0) {
                    this.mCountDownTimerTable[i2][0] = 1;
                }
                int item_en2 = getItemEnable(xml_path2, mColorMode, "degamma");
                VSlog.d(TAG, "degamma: enable = " + item_en2);
                if (item_en2 > 0) {
                    this.mCountDownTimerTable[i2][1] = 1;
                }
                int item_en3 = getItemEnable(xml_path2, mColorMode, "gamma");
                VSlog.d(TAG, "gamma: enable = " + item_en3);
                if (item_en3 > 0) {
                    this.mCountDownTimerTable[i2][2] = 1;
                }
                int item_en4 = getItemEnable(xml_path2, mColorMode, "gamma_matrix");
                VSlog.d(TAG, "gamma_matrix: enable = " + item_en4);
                if (item_en4 > 0) {
                    this.mCountDownTimerTable[i2][3] = 1;
                }
                int item_en5 = getItemEnable(xml_path2, mColorMode, "hsc");
                VSlog.d(TAG, "hsc: enable = " + item_en5);
                for (int j2 = 4; j2 <= 5; j2++) {
                    if (item_en5 > 0) {
                        this.mCountDownTimerTable[i2][j2] = 1;
                    }
                }
                int item_en6 = getItemEnable(xml_path2, mColorMode, "scl");
                VSlog.d(TAG, "scl: enable = " + item_en6);
                if (item_en6 > 0) {
                    this.mCountDownTimerTable[i2][6] = 1;
                }
                int item_en7 = getItemEnable(xml_path2, mColorMode, "cgc17_con");
                VSlog.d(TAG, "cgc17_con: enable = " + item_en7);
                for (int j3 = 7; j3 <= 11; j3++) {
                    if (item_en7 > 0) {
                        this.mCountDownTimerTable[i2][j3] = 1;
                    }
                }
            }
            CountDownTimer countDownTimer = this.mCountdownTimer;
            if (countDownTimer != null) {
                this.mCountDownTimerCount = 0;
                countDownTimer.cancel();
                this.mCountdownTimer.start();
                return;
            }
            return;
        }
        VSlog.d(TAG, "xml version not found");
    }

    protected int getCountDownTimerCount() {
        return this.mCountDownTimerCount;
    }
}