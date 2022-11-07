package com.android.server.display.color;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import vivo.util.VSlog;

/* loaded from: classes.dex */
class ExynosDisplayFactory {
    private static final int LOOP_CNT = 40;
    private static final int MSG_SIGNAL_REFRESH = 1;
    private static final int MSG_SYSFS_WRITE_CGC17_ENC = 3;
    private static final int MSG_SYSFS_WRITE_CGC17_IDX = 2;
    private static final String TAG = "ExynosDisplayFactory";
    private int mColorModeSetting;
    private Context mContext;
    private int[] mCountDownTimerTable;
    private String mFactoryXMLMode;
    private String mFactoryXMLPath;
    private Handler mLocalHandler;
    private final boolean DEBUG = "eng".equals(Build.TYPE);
    private CountDownTimer mCountdownTimer = null;
    private int mTimeoutMs = 2400;
    private int mIntervalMs = 60;
    private int mCountDownTimerCount = 0;
    private String XML_SYSFS_PATH = "/sys/class/dqe/dqe/xml";
    private String CGC17_IDX_SYSFS_PATH = "/sys/class/dqe/dqe/cgc17_idx";
    private String CGC17_ENC_SYSFS_PATH = "/sys/class/dqe/dqe/cgc17_enc";
    private String CGC17_DEC_SYSFS_PATH = "/sys/class/dqe/dqe/cgc17_dec";
    private String CGC17_CON_SYSFS_PATH = "/sys/class/dqe/dqe/cgc17_con";
    private String DEGAMMA_SFR_SYSFS_PATH = "/sys/class/dqe/dqe/degamma";
    private String GAMMA_SFR_SYSFS_PATH = "/sys/class/dqe/dqe/gamma";
    private String GAMMA_MATRIX_SYSFS_PATH = "/sys/class/dqe/dqe/gamma_matrix";
    private String HSC48_IDX_SYSFS_PATH = "/sys/class/dqe/dqe/hsc48_idx";
    private String HSC48_LCG_SYSFS_PATH = "/sys/class/dqe/dqe/hsc48_lcg";
    private String HSC_SFR_SYSFS_PATH = "/sys/class/dqe/dqe/hsc";
    private String SCL_SFR_SYSFS_PATH = "/sys/class/dqe/dqe/scl";
    private ExynosDisplayATC mExynosDisplayATC = null;
    private String[] mColorModeSettingTable = {"mode1", "mode2", "mode3"};
    private Handler mHandler = new Handler() { // from class: com.android.server.display.color.ExynosDisplayFactory.2
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int i = msg.what;
            if (i == 1) {
                ExynosDisplayUtils.sendEmptyUpdate();
            } else if (i == 2) {
                ExynosDisplayFactory.this.sysfsWriteCGC17_IDX(msg.arg1);
            } else if (i == 3) {
                ExynosDisplayFactory.this.sysfsWriteCGC17_ENC(msg.obj.toString());
            }
        }
    };

    static /* synthetic */ int access$508(ExynosDisplayFactory x0) {
        int i = x0.mCountDownTimerCount;
        x0.mCountDownTimerCount = i + 1;
        return i;
    }

    public ExynosDisplayFactory(Context context) {
        this.mCountDownTimerTable = new int[]{0};
        this.mFactoryXMLPath = null;
        this.mFactoryXMLMode = null;
        this.mContext = context;
        this.mLocalHandler = new Handler(context.getMainLooper());
        this.mCountDownTimerTable = new int[40];
        for (int i = 0; i < 40; i++) {
            this.mCountDownTimerTable[i] = 0;
        }
        initCountDownTimer();
        this.mFactoryXMLPath = null;
        this.mFactoryXMLMode = "mode1";
        this.mLocalHandler.postDelayed(new Runnable() { // from class: com.android.server.display.color.ExynosDisplayFactory.1
            @Override // java.lang.Runnable
            public void run() {
                ExynosDisplayFactory exynosDisplayFactory = ExynosDisplayFactory.this;
                exynosDisplayFactory.startCountDownTimer(exynosDisplayFactory.mFactoryXMLPath, ExynosDisplayFactory.this.mFactoryXMLMode);
            }
        }, 0L);
    }

    public void setExynosDisplayATC(ExynosDisplayATC mATC) {
        this.mExynosDisplayATC = mATC;
    }

    private void setFactoryXMLColorMode() {
        ContentResolver resolver = this.mContext.getContentResolver();
        this.mColorModeSetting = Settings.System.getIntForUser(resolver, "display_color_mode", 0, -2);
        VSlog.d(TAG, "display_color_mode:" + this.mColorModeSetting);
        int i = this.mColorModeSetting;
        String[] strArr = this.mColorModeSettingTable;
        if (i >= strArr.length) {
            this.mColorModeSetting = strArr.length - 1;
        } else if (i < 0) {
            this.mColorModeSetting = 0;
        }
        this.mFactoryXMLMode = this.mColorModeSettingTable[this.mColorModeSetting];
        VSlog.d(TAG, "mColorModeSetting:" + this.mColorModeSetting + ", mFactoryXMLMode=" + this.mFactoryXMLMode);
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

    private void initCountDownTimer() {
        this.mCountdownTimer = new CountDownTimer(this.mTimeoutMs, this.mIntervalMs) { // from class: com.android.server.display.color.ExynosDisplayFactory.3
            @Override // android.os.CountDownTimer
            public void onTick(long millisUntilFinished) {
                int item_en = ExynosDisplayFactory.this.mCountDownTimerTable[ExynosDisplayFactory.this.mCountDownTimerCount];
                if (ExynosDisplayFactory.this.mCountDownTimerCount <= 0) {
                    if (ExynosDisplayFactory.this.mExynosDisplayATC != null && ExynosDisplayFactory.this.mFactoryXMLPath != null && item_en == 1) {
                        ExynosDisplayFactory.this.mExynosDisplayATC.parserATCXML(ExynosDisplayFactory.this.mFactoryXMLPath, ExynosDisplayFactory.this.mFactoryXMLMode);
                    }
                } else if (ExynosDisplayFactory.this.mCountDownTimerCount > 1) {
                    if (ExynosDisplayFactory.this.mCountDownTimerCount > 2) {
                        if (ExynosDisplayFactory.this.mCountDownTimerCount > 3) {
                            if (ExynosDisplayFactory.this.mCountDownTimerCount > 4) {
                                if (ExynosDisplayFactory.this.mCountDownTimerCount > 5) {
                                    if (ExynosDisplayFactory.this.mCountDownTimerCount > 6) {
                                        if (ExynosDisplayFactory.this.mCountDownTimerCount > 9) {
                                            if (ExynosDisplayFactory.this.mCountDownTimerCount > 10) {
                                                if (ExynosDisplayFactory.this.mCountDownTimerCount > 11) {
                                                    if (ExynosDisplayFactory.this.mCountDownTimerCount <= 12 && ExynosDisplayFactory.this.mHandler != null) {
                                                        ExynosDisplayFactory.this.mHandler.sendEmptyMessage(1);
                                                        ExynosDisplayFactory.this.mHandler.sendEmptyMessage(1);
                                                    }
                                                } else if (item_en == 1) {
                                                    ExynosDisplayFactory exynosDisplayFactory = ExynosDisplayFactory.this;
                                                    exynosDisplayFactory.setCalibrationCGC17_CON(exynosDisplayFactory.mFactoryXMLPath, ExynosDisplayFactory.this.mFactoryXMLMode);
                                                }
                                            } else if (item_en == 1) {
                                                ExynosDisplayFactory.this.setCalibrationCGC17_DEC();
                                            }
                                        } else if (item_en == 1) {
                                            ExynosDisplayFactory exynosDisplayFactory2 = ExynosDisplayFactory.this;
                                            exynosDisplayFactory2.setCalibrationCGC17_ENC(exynosDisplayFactory2.mFactoryXMLPath, ExynosDisplayFactory.this.mFactoryXMLMode, ExynosDisplayFactory.this.mCountDownTimerCount - 7);
                                        }
                                    } else if (item_en == 1) {
                                        ExynosDisplayFactory exynosDisplayFactory3 = ExynosDisplayFactory.this;
                                        exynosDisplayFactory3.setCalibrationSCL(exynosDisplayFactory3.mFactoryXMLPath, ExynosDisplayFactory.this.mFactoryXMLMode);
                                    }
                                } else if (item_en == 1) {
                                    ExynosDisplayFactory exynosDisplayFactory4 = ExynosDisplayFactory.this;
                                    exynosDisplayFactory4.setCalibrationHSC(exynosDisplayFactory4.mFactoryXMLPath, ExynosDisplayFactory.this.mFactoryXMLMode);
                                }
                            } else if (item_en == 1) {
                                ExynosDisplayFactory exynosDisplayFactory5 = ExynosDisplayFactory.this;
                                exynosDisplayFactory5.setCalibrationHSC48_LCG(exynosDisplayFactory5.mFactoryXMLPath, ExynosDisplayFactory.this.mFactoryXMLMode, ExynosDisplayFactory.this.mCountDownTimerCount - 4);
                            }
                        } else if (item_en == 1) {
                            ExynosDisplayFactory exynosDisplayFactory6 = ExynosDisplayFactory.this;
                            exynosDisplayFactory6.setCalibrationGAMMA_MATRIX(exynosDisplayFactory6.mFactoryXMLPath, ExynosDisplayFactory.this.mFactoryXMLMode);
                        }
                    } else if (item_en == 1) {
                        ExynosDisplayFactory exynosDisplayFactory7 = ExynosDisplayFactory.this;
                        exynosDisplayFactory7.setCalibrationGAMMA(exynosDisplayFactory7.mFactoryXMLPath, ExynosDisplayFactory.this.mFactoryXMLMode);
                    }
                } else if (item_en == 1) {
                    ExynosDisplayFactory exynosDisplayFactory8 = ExynosDisplayFactory.this;
                    exynosDisplayFactory8.setCalibrationDEGAMMA(exynosDisplayFactory8.mFactoryXMLPath, ExynosDisplayFactory.this.mFactoryXMLMode);
                }
                ExynosDisplayFactory.access$508(ExynosDisplayFactory.this);
            }

            @Override // android.os.CountDownTimer
            public void onFinish() {
                VSlog.d(ExynosDisplayFactory.TAG, "CountDownTimer finished = " + ExynosDisplayFactory.this.mCountDownTimerCount);
                ExynosDisplayFactory.this.mCountDownTimerCount = 0;
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

    /* JADX INFO: Access modifiers changed from: protected */
    public void startCountDownTimer(String xml_path, String mode_name) {
        String[] temp_array;
        if (xml_path == null) {
            xml_path = ExynosDisplayUtils.getStringFromFile(this.XML_SYSFS_PATH);
        }
        if (mode_name == null) {
            return;
        }
        this.mFactoryXMLPath = xml_path;
        this.mFactoryXMLMode = mode_name;
        if (!ExynosDisplayUtils.existFile(xml_path)) {
            return;
        }
        VSlog.d(TAG, "startCountDownTimer: xml_path=" + xml_path + ", mode_name=" + mode_name);
        for (int i = 0; i < 40; i++) {
            this.mCountDownTimerTable[i] = 0;
        }
        try {
            temp_array = ExynosDisplayUtils.parserXMLNodeText(xml_path, "version");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (temp_array != null && temp_array.length >= 1) {
            VSlog.d(TAG, "xml version: " + temp_array[0]);
            String[] temp_array2 = ExynosDisplayUtils.parserFactoryXMLAttribute(xml_path, mode_name, "atc", "al");
            if (temp_array2 != null && temp_array2.length >= 1) {
                this.mCountDownTimerTable[0] = 1;
            }
            VSlog.d(TAG, "atc: enable = " + this.mCountDownTimerTable[0]);
            int item_en = getItemEnable(xml_path, mode_name, "degamma");
            VSlog.d(TAG, "degamma: enable = " + item_en);
            if (item_en > 0) {
                this.mCountDownTimerTable[1] = 1;
            }
            int item_en2 = getItemEnable(xml_path, mode_name, "gamma");
            VSlog.d(TAG, "gamma: enable = " + item_en2);
            if (item_en2 > 0) {
                this.mCountDownTimerTable[2] = 1;
            }
            int item_en3 = getItemEnable(xml_path, mode_name, "gamma_matrix");
            VSlog.d(TAG, "gamma_matrix: enable = " + item_en3);
            if (item_en3 > 0) {
                this.mCountDownTimerTable[3] = 1;
            }
            int item_en4 = getItemEnable(xml_path, mode_name, "hsc");
            VSlog.d(TAG, "hsc: enable = " + item_en4);
            for (int i2 = 4; i2 <= 5; i2++) {
                if (item_en4 > 0) {
                    this.mCountDownTimerTable[i2] = 1;
                }
            }
            int item_en5 = getItemEnable(xml_path, mode_name, "scl");
            VSlog.d(TAG, "scl: enable = " + item_en5);
            if (item_en5 > 0) {
                this.mCountDownTimerTable[6] = 1;
            }
            int item_en6 = getItemEnable(xml_path, mode_name, "cgc17_con");
            VSlog.d(TAG, "cgc17_con: enable = " + item_en6);
            for (int i3 = 7; i3 <= 11; i3++) {
                if (item_en6 > 0) {
                    this.mCountDownTimerTable[i3] = 1;
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

    /* JADX INFO: Access modifiers changed from: protected */
    public int getCountDownTimerCount() {
        return this.mCountDownTimerCount;
    }
}