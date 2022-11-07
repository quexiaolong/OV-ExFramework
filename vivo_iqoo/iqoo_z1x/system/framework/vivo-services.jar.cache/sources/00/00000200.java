package com.android.server.display.color;

import android.os.Build;
import java.util.Timer;
import java.util.TimerTask;
import vivo.util.VSlog;

/* loaded from: classes.dex */
class ExynosDisplayTune {
    private static final String TAG = "ExynosDisplayTune";
    private Timer mTuneTimer;
    private final boolean DEBUG = "eng".equals(Build.TYPE);
    private String GAMMA_DIR_PATH = "/data/dqe/gamma/data/";
    private String GAMMA_RESET_PATH = "/data/dqe/gamma/data/reset";
    private String GAMMA_SYSFS_PATH = "/sys/class/dqe/dqe/gamma";
    private String CGC_DIR_PATH = "/data/dqe/cgc/data/";
    private String CGC_RESET_PATH = "/data/dqe/cgc/data/reset";
    private String CGC_SYSFS_PATH = "/sys/class/dqe/dqe/cgc";
    private String HSC_DIR_PATH = "/data/dqe/hsc/data/";
    private String HSC_RESET_PATH = "/data/dqe/hsc/data/reset";
    private String HSC_SYSFS_PATH = "/sys/class/dqe/dqe/hsc";
    private String ATC_DIR_PATH = "/data/dqe/aps/data/";
    private String ATC_RESET_PATH = "/data/dqe/aps/data/reset";
    private String ATC_ONOFF_SYSFS_PATH = "/sys/class/dqe/dqe/aps_onoff";
    private String ATC_SYSFS_PATH = "/sys/class/dqe/dqe/aps";
    private String GAMMA_ON_FILE_PATH = "/data/dqe/gamma_on";
    private String GAMMA_OFF_FILE_PATH = "/data/dqe/gamma_off";
    private String CGC_ON_FILE_PATH = "/data/dqe/cgc_on";
    private String CGC_OFF_FILE_PATH = "/data/dqe/cgc_off";
    private String HSC_ON_FILE_PATH = "/data/dqe/hsc_on";
    private String HSC_OFF_FILE_PATH = "/data/dqe/hsc_off";
    private String ATC_ON_FILE_PATH = "/data/dqe/aps_on";
    private String ATC_OFF_FILE_PATH = "/data/dqe/aps_off";
    private String MODE_NAME_FILE_PATH = "/data/dqe/mode_name.txt";
    private String MODE_NAME_VALUE = null;
    private String MODE_NAME_STREAM = null;
    private String MODE0_CGC_FILE_PATH = "/data/dqe/mode0_cgc";
    private String MODE0_GAMMA_FILE_PATH = "/data/dqe/mode0_gamma";
    private String MODE0_HSC_FILE_PATH = "/data/dqe/mode0_hsc";
    private String MODE1_CGC_FILE_PATH = "/data/dqe/mode1_cgc";
    private String MODE1_GAMMA_FILE_PATH = "/data/dqe/mode1_gamma";
    private String MODE1_HSC_FILE_PATH = "/data/dqe/mode1_hsc";
    private String MODE2_CGC_FILE_PATH = "/data/dqe/mode2_cgc";
    private String MODE2_GAMMA_FILE_PATH = "/data/dqe/mode2_gamma";
    private String MODE2_HSC_FILE_PATH = "/data/dqe/mode2_hsc";
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
    private String GAMMA_FILE_PATH = null;
    private String GAMMA_VALUE = null;
    private String GAMMA_STREAM = null;
    private String CGC_FILE_PATH = null;
    private String CGC_VALUE = null;
    private String CGC_STREAM = null;
    private String HSC_FILE_PATH = null;
    private String HSC_VALUE = null;
    private String HSC_STREAM = null;
    private String ATC_FILE_PATH = null;
    private String ATC_VALUE = null;
    private String ATC_ONOFF_VALUE = null;
    private String ATC_STREAM = null;
    private long mDelayMs = 1000;
    private long mPeriodMs = 1000;
    private String CALIB_DATA_XML_PATH = "/data/dqe/calib_data.xml";
    private String BYPASS_XML_FILE_PATH = "/vendor/etc/dqe/calib_data_bypass.xml";
    private String[] bypass_array = null;

    public void setGammaValue(int value) {
        try {
            this.GAMMA_FILE_PATH = this.GAMMA_DIR_PATH.concat(Integer.toString(value));
            VSlog.d(TAG, "GAMMA_FILE_PATH = " + this.GAMMA_FILE_PATH);
            String stringFromFile = ExynosDisplayUtils.getStringFromFile(this.GAMMA_FILE_PATH);
            this.GAMMA_VALUE = stringFromFile;
            this.GAMMA_STREAM = stringFromFile.trim().replaceAll("\r\n", ",");
            VSlog.d(TAG, "GAMMA_STREAM = " + this.GAMMA_STREAM);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (this.GAMMA_STREAM != null) {
            VSlog.d(TAG, "setGammaValue()");
            ExynosDisplayUtils.sysfsWriteSting(this.GAMMA_SYSFS_PATH, this.GAMMA_STREAM);
        }
    }

    public void setGammaReset() {
        try {
            this.GAMMA_VALUE = ExynosDisplayUtils.getStringFromFile(this.GAMMA_RESET_PATH);
            VSlog.d(TAG, "GAMMA_RESET_PATH = " + this.GAMMA_RESET_PATH);
            this.GAMMA_STREAM = this.GAMMA_VALUE.trim().replaceAll("\r\n", ",");
            VSlog.d(TAG, "gammaStream = " + this.GAMMA_STREAM);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (this.GAMMA_STREAM != null) {
            VSlog.d(TAG, "setGammaReset()");
            ExynosDisplayUtils.sysfsWriteSting(this.GAMMA_SYSFS_PATH, this.GAMMA_STREAM);
        }
    }

    public void setGammaOn(int onoff) {
        try {
            if (onoff != 0) {
                this.GAMMA_VALUE = ExynosDisplayUtils.getStringFromFile(this.GAMMA_ON_FILE_PATH);
                VSlog.d(TAG, "GAMMA_ON_FILE_PATH = " + this.GAMMA_ON_FILE_PATH);
            } else {
                this.GAMMA_VALUE = ExynosDisplayUtils.getStringFromFile(this.GAMMA_OFF_FILE_PATH);
                VSlog.d(TAG, "GAMMA_OFF_FILE_PATH = " + this.GAMMA_OFF_FILE_PATH);
            }
            this.GAMMA_STREAM = this.GAMMA_VALUE.trim().replaceAll("\r\n", ",");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (this.GAMMA_STREAM != null) {
            VSlog.d(TAG, "setGammaOn()");
            ExynosDisplayUtils.sysfsWriteSting(this.GAMMA_SYSFS_PATH, this.GAMMA_STREAM);
        }
    }

    public void setCgcValue(int value) {
        try {
            this.CGC_FILE_PATH = this.CGC_DIR_PATH.concat(Integer.toString(value));
            VSlog.d(TAG, "CGC_FILE_PATH = " + this.CGC_FILE_PATH);
            String stringFromFile = ExynosDisplayUtils.getStringFromFile(this.CGC_FILE_PATH);
            this.CGC_VALUE = stringFromFile;
            this.CGC_STREAM = stringFromFile.trim().replaceAll("\r\n", ",");
            VSlog.d(TAG, "CGC_STREAM = " + this.CGC_STREAM);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (this.CGC_STREAM != null) {
            VSlog.d(TAG, "setCgcValue()");
            ExynosDisplayUtils.sysfsWriteSting(this.CGC_SYSFS_PATH, this.CGC_STREAM);
        }
    }

    public void setCgcReset() {
        try {
            this.CGC_VALUE = ExynosDisplayUtils.getStringFromFile(this.CGC_RESET_PATH);
            VSlog.d(TAG, "CGC_RESET_PATH = " + this.CGC_RESET_PATH);
            this.CGC_STREAM = this.CGC_VALUE.trim().replaceAll("\r\n", ",");
            VSlog.d(TAG, "cgcStream = " + this.CGC_STREAM);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (this.CGC_STREAM != null) {
            VSlog.d(TAG, "setCgcReset()");
            ExynosDisplayUtils.sysfsWriteSting(this.CGC_SYSFS_PATH, this.CGC_STREAM);
        }
    }

    public void setCgcOn(int onoff) {
        try {
            if (onoff != 0) {
                this.CGC_VALUE = ExynosDisplayUtils.getStringFromFile(this.CGC_ON_FILE_PATH);
                VSlog.d(TAG, "CGC_ON_FILE_PATH = " + this.CGC_ON_FILE_PATH);
            } else {
                this.CGC_VALUE = ExynosDisplayUtils.getStringFromFile(this.CGC_OFF_FILE_PATH);
                VSlog.d(TAG, "CGC_OFF_FILE_PATH = " + this.CGC_OFF_FILE_PATH);
            }
            this.CGC_STREAM = this.CGC_VALUE.trim().replaceAll("\r\n", ",");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (this.CGC_STREAM != null) {
            VSlog.d(TAG, "setCgcOn()");
            ExynosDisplayUtils.sysfsWriteSting(this.CGC_SYSFS_PATH, this.CGC_STREAM);
        }
    }

    public void setHscValue(int value) {
        try {
            this.HSC_FILE_PATH = this.HSC_DIR_PATH.concat(Integer.toString(value));
            VSlog.d(TAG, "HSC_FILE_PATH = " + this.HSC_FILE_PATH);
            String stringFromFile = ExynosDisplayUtils.getStringFromFile(this.HSC_FILE_PATH);
            this.HSC_VALUE = stringFromFile;
            this.HSC_STREAM = stringFromFile.trim().replaceAll("\r\n", ",");
            VSlog.d(TAG, "HSC_STREAM = " + this.HSC_STREAM);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (this.HSC_STREAM != null) {
            VSlog.d(TAG, "setHscValue()");
            ExynosDisplayUtils.sysfsWriteSting(this.HSC_SYSFS_PATH, this.HSC_STREAM);
        }
    }

    public void setHscReset() {
        try {
            this.HSC_VALUE = ExynosDisplayUtils.getStringFromFile(this.HSC_RESET_PATH);
            VSlog.d(TAG, "HSC_RESET_PATH = " + this.HSC_RESET_PATH);
            this.HSC_STREAM = this.HSC_VALUE.trim().replaceAll("\r\n", ",");
            VSlog.d(TAG, "hscStream = " + this.HSC_STREAM);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (this.HSC_STREAM != null) {
            VSlog.d(TAG, "setHscReset()");
            ExynosDisplayUtils.sysfsWriteSting(this.HSC_SYSFS_PATH, this.HSC_STREAM);
        }
    }

    public void setHscOn(int onoff) {
        try {
            if (onoff != 0) {
                this.HSC_VALUE = ExynosDisplayUtils.getStringFromFile(this.HSC_ON_FILE_PATH);
                VSlog.d(TAG, "HSC_ON_FILE_PATH = " + this.HSC_ON_FILE_PATH);
            } else {
                this.HSC_VALUE = ExynosDisplayUtils.getStringFromFile(this.HSC_OFF_FILE_PATH);
                VSlog.d(TAG, "HSC_OFF_FILE_PATH = " + this.HSC_OFF_FILE_PATH);
            }
            this.HSC_STREAM = this.HSC_VALUE.trim().replaceAll("\r\n", ",");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (this.HSC_STREAM != null) {
            VSlog.d(TAG, "setHscOn()");
            ExynosDisplayUtils.sysfsWriteSting(this.HSC_SYSFS_PATH, this.HSC_STREAM);
        }
    }

    public void setAtcValue(int value) {
        try {
            this.ATC_FILE_PATH = this.ATC_DIR_PATH.concat(Integer.toString(value));
            VSlog.d(TAG, "ATC_FILE_PATH = " + this.ATC_FILE_PATH);
            String stringFromFile = ExynosDisplayUtils.getStringFromFile(this.ATC_FILE_PATH);
            this.ATC_VALUE = stringFromFile;
            this.ATC_STREAM = stringFromFile.trim().replaceAll("\r\n", ",");
            VSlog.d(TAG, "ATC_STREAM = " + this.ATC_STREAM);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (this.ATC_STREAM != null) {
            VSlog.d(TAG, "setAtcValue()");
            ExynosDisplayUtils.sysfsWriteSting(this.ATC_SYSFS_PATH, this.ATC_STREAM);
        }
    }

    public void setAtcReset() {
        try {
            this.ATC_VALUE = ExynosDisplayUtils.getStringFromFile(this.ATC_RESET_PATH);
            VSlog.d(TAG, "ATC_RESET_PATH = " + this.ATC_RESET_PATH);
            this.ATC_STREAM = this.ATC_VALUE.trim().replaceAll("\r\n", ",");
            VSlog.d(TAG, "atcStream = " + this.ATC_STREAM);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (this.ATC_STREAM != null) {
            VSlog.d(TAG, "setAtcReset()");
            ExynosDisplayUtils.sysfsWriteSting(this.ATC_SYSFS_PATH, this.ATC_STREAM);
        }
    }

    public void setAtcOn(int onoff) {
        try {
            if (onoff != 0) {
                this.ATC_VALUE = ExynosDisplayUtils.getStringFromFile(this.ATC_ON_FILE_PATH);
                VSlog.d(TAG, "ATC_ON_FILE_PATH = " + this.ATC_ON_FILE_PATH);
            } else {
                this.ATC_VALUE = ExynosDisplayUtils.getStringFromFile(this.ATC_OFF_FILE_PATH);
                VSlog.d(TAG, "ATC_OFF_FILE_PATH = " + this.ATC_OFF_FILE_PATH);
            }
            this.ATC_STREAM = this.ATC_VALUE.trim().replaceAll("\r\n", ",");
            this.ATC_ONOFF_VALUE = ExynosDisplayUtils.getStringFromFile(this.ATC_ONOFF_SYSFS_PATH);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (this.ATC_STREAM != null) {
            VSlog.d(TAG, "setAtcOn()");
            ExynosDisplayUtils.sysfsWriteSting(this.ATC_SYSFS_PATH, this.ATC_STREAM);
        }
        String str = this.ATC_ONOFF_VALUE;
        if (str != null && !str.equals("0")) {
            VSlog.d(TAG, "setAtcOn(): onoff = " + onoff);
            ExynosDisplayUtils.sysfsWrite(this.ATC_ONOFF_SYSFS_PATH, onoff);
        }
    }

    public String getColorEnhancementMode() {
        try {
            String stringFromFile = ExynosDisplayUtils.getStringFromFile(this.MODE_NAME_FILE_PATH);
            this.MODE_NAME_VALUE = stringFromFile;
            if (stringFromFile != null) {
                this.MODE_NAME_STREAM = stringFromFile.trim().replaceAll("\r\n", ",");
            }
            return this.MODE_NAME_STREAM;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setColorEnhancement(int value) {
        String cgc_value = null;
        String gamma_value = null;
        String hsc_value = null;
        String cgc_stream = null;
        String gamma_stream = null;
        String hsc_stream = null;
        try {
            if (value == 0) {
                cgc_value = ExynosDisplayUtils.getStringFromFile(this.MODE0_CGC_FILE_PATH);
                gamma_value = ExynosDisplayUtils.getStringFromFile(this.MODE0_GAMMA_FILE_PATH);
                hsc_value = ExynosDisplayUtils.getStringFromFile(this.MODE0_HSC_FILE_PATH);
            } else if (value == 1) {
                cgc_value = ExynosDisplayUtils.getStringFromFile(this.MODE1_CGC_FILE_PATH);
                gamma_value = ExynosDisplayUtils.getStringFromFile(this.MODE1_GAMMA_FILE_PATH);
                hsc_value = ExynosDisplayUtils.getStringFromFile(this.MODE1_HSC_FILE_PATH);
            } else if (value == 2) {
                cgc_value = ExynosDisplayUtils.getStringFromFile(this.MODE2_CGC_FILE_PATH);
                gamma_value = ExynosDisplayUtils.getStringFromFile(this.MODE2_GAMMA_FILE_PATH);
                hsc_value = ExynosDisplayUtils.getStringFromFile(this.MODE2_HSC_FILE_PATH);
            }
            if (cgc_value != null) {
                cgc_stream = cgc_value.trim().replaceAll("\r\n", ",");
            }
            if (gamma_value != null) {
                gamma_stream = gamma_value.trim().replaceAll("\r\n", ",");
            }
            if (hsc_value != null) {
                hsc_stream = hsc_value.trim().replaceAll("\r\n", ",");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (cgc_stream != null) {
            ExynosDisplayUtils.sysfsWriteSting(this.CGC_SYSFS_PATH, cgc_stream);
        }
        if (gamma_stream != null) {
            ExynosDisplayUtils.sysfsWriteSting(this.GAMMA_SYSFS_PATH, gamma_stream);
        }
        if (hsc_stream != null) {
            ExynosDisplayUtils.sysfsWriteSting(this.HSC_SYSFS_PATH, hsc_stream);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setClibData() {
        if (!ExynosDisplayUtils.existFile(this.CALIB_DATA_XML_PATH) || getXMLVersion(this.CALIB_DATA_XML_PATH) != null) {
            return;
        }
        String[] temp_array = null;
        try {
            temp_array = ExynosDisplayUtils.parserTuneXML(this.CALIB_DATA_XML_PATH, "tune", "dqe");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (temp_array == null) {
            return;
        }
        if (temp_array.length < 6) {
            VSlog.d(TAG, "xml array size wrong: " + temp_array.length);
            return;
        }
        if (temp_array[3].equals("1")) {
            ExynosDisplayUtils.sysfsWriteSting(this.CGC_SYSFS_PATH, temp_array[0]);
        } else {
            ExynosDisplayUtils.sysfsWriteSting(this.CGC_SYSFS_PATH, this.bypass_array[0]);
        }
        if (temp_array[4].equals("1")) {
            ExynosDisplayUtils.sysfsWriteSting(this.GAMMA_SYSFS_PATH, temp_array[1]);
        } else {
            ExynosDisplayUtils.sysfsWriteSting(this.GAMMA_SYSFS_PATH, this.bypass_array[1]);
        }
        if (temp_array[5].equals("1")) {
            ExynosDisplayUtils.sysfsWriteSting(this.HSC_SYSFS_PATH, temp_array[2]);
        } else {
            ExynosDisplayUtils.sysfsWriteSting(this.HSC_SYSFS_PATH, this.bypass_array[2]);
        }
        ExynosDisplayUtils.sendEmptyUpdate();
    }

    private void startTuneTimer() {
        if (this.bypass_array == null) {
            this.bypass_array = ExynosDisplayUtils.parserXML(this.BYPASS_XML_FILE_PATH, "bypass", "dqe");
        }
        if (this.mTuneTimer == null) {
            Timer timer = new Timer();
            this.mTuneTimer = timer;
            timer.scheduleAtFixedRate(new TimerTask() { // from class: com.android.server.display.color.ExynosDisplayTune.1
                @Override // java.util.TimerTask, java.lang.Runnable
                public void run() {
                    ExynosDisplayTune.this.setClibData();
                }
            }, this.mDelayMs, this.mPeriodMs);
        }
    }

    private void stopTuneTimer() {
        Timer timer = this.mTuneTimer;
        if (timer != null) {
            timer.cancel();
            this.mTuneTimer = null;
        }
        this.bypass_array = null;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void enableTuneTimer(boolean enable) {
        if (enable) {
            startTuneTimer();
        } else {
            stopTuneTimer();
        }
        VSlog.d(TAG, "enableTuneTimer: enable=" + enable);
    }

    private String getXMLVersion(String xml_path) {
        try {
            String[] temp_array = ExynosDisplayUtils.parserXMLNodeText(xml_path, "version");
            if (temp_array != null && temp_array.length >= 1) {
                String version = temp_array[0];
                VSlog.d(TAG, "xml version: " + version);
                return version;
            }
            VSlog.d(TAG, "xml version not found");
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private int getItemEnable(String xml_path, String node) {
        try {
            String[] temp_array = ExynosDisplayUtils.parserXMLNodeText(xml_path, node);
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

    private void setCalibrationDQE() {
        int i;
        String str;
        char c;
        int i2;
        int i3;
        String str2 = "gamma_matrix";
        if (ExynosDisplayUtils.existFile(this.CALIB_DATA_XML_PATH) && getXMLVersion(this.CALIB_DATA_XML_PATH) != null) {
            VSlog.d(TAG, "setCalibrationDQE+");
            int[] mCalibrationDQETable = new int[90];
            int i4 = 0;
            while (true) {
                i = 0;
                if (i4 >= 90) {
                    break;
                }
                mCalibrationDQETable[i4] = 0;
                i4++;
            }
            try {
                int item_en = getItemEnable(this.CALIB_DATA_XML_PATH, "degamma");
                VSlog.d(TAG, "degamma: enable = " + item_en);
                int i5 = 1;
                if (item_en > 0) {
                    mCalibrationDQETable[1] = 1;
                }
                int item_en2 = getItemEnable(this.CALIB_DATA_XML_PATH, "gamma");
                VSlog.d(TAG, "gamma: enable = " + item_en2);
                if (item_en2 > 0) {
                    mCalibrationDQETable[2] = 1;
                }
                int item_en3 = getItemEnable(this.CALIB_DATA_XML_PATH, "gamma_matrix");
                VSlog.d(TAG, "gamma_matrix: enable = " + item_en3);
                if (item_en3 > 0) {
                    mCalibrationDQETable[3] = 1;
                }
                int item_en4 = getItemEnable(this.CALIB_DATA_XML_PATH, "hsc");
                VSlog.d(TAG, "hsc: enable = " + item_en4);
                for (int i6 = 4; i6 <= 7; i6++) {
                    if (item_en4 > 0) {
                        mCalibrationDQETable[i6] = 1;
                    }
                }
                int item_en5 = getItemEnable(this.CALIB_DATA_XML_PATH, "scl");
                VSlog.d(TAG, "scl: enable = " + item_en5);
                if (item_en5 > 0) {
                    mCalibrationDQETable[8] = 1;
                }
                int item_en6 = getItemEnable(this.CALIB_DATA_XML_PATH, "cgc17_con");
                VSlog.d(TAG, "cgc17_con: enable = " + item_en6);
                for (int i7 = 9; i7 <= 61; i7++) {
                    if (item_en6 > 0) {
                        mCalibrationDQETable[i7] = 1;
                    }
                }
                int i8 = 0;
                for (int i9 = 90; i8 < i9; i9 = 90) {
                    int item_en7 = mCalibrationDQETable[i8];
                    if (i8 <= 0) {
                        str = str2;
                    } else {
                        if (i8 <= i5) {
                            String[] temp_array = ExynosDisplayUtils.parserFactoryXMLText(this.CALIB_DATA_XML_PATH, "tune", "degamma", i, i);
                            if (temp_array != null && temp_array.length >= i5) {
                                String degamma = temp_array[i];
                                ExynosDisplayUtils.sysfsWriteSting(this.DEGAMMA_SFR_SYSFS_PATH, degamma);
                            }
                        } else if (i8 <= 2) {
                            String[] temp_array2 = ExynosDisplayUtils.parserFactoryXMLText(this.CALIB_DATA_XML_PATH, "tune", "gamma", i, i);
                            if (temp_array2 != null && temp_array2.length >= i5) {
                                String gamma = temp_array2[i];
                                ExynosDisplayUtils.sysfsWriteSting(this.GAMMA_SFR_SYSFS_PATH, gamma);
                            }
                        } else if (i8 <= 3) {
                            String[] temp_array3 = ExynosDisplayUtils.parserFactoryXMLText(this.CALIB_DATA_XML_PATH, "tune", str2, i, i);
                            if (temp_array3 != null && temp_array3.length >= i5) {
                                String gamma_matrix = temp_array3[i];
                                ExynosDisplayUtils.sysfsWriteSting(this.GAMMA_MATRIX_SYSFS_PATH, gamma_matrix);
                            }
                        } else if (i8 > 6) {
                            str = str2;
                            if (i8 <= 7) {
                                String[] temp_array4 = ExynosDisplayUtils.parserFactoryXMLText(this.CALIB_DATA_XML_PATH, "tune", "hsc", 0, 0);
                                if (temp_array4 != null && temp_array4.length >= 1) {
                                    String hsc = temp_array4[0];
                                    ExynosDisplayUtils.sysfsWriteSting(this.HSC_SFR_SYSFS_PATH, hsc);
                                }
                            } else if (i8 <= 8) {
                                String[] temp_array5 = ExynosDisplayUtils.parserFactoryXMLText(this.CALIB_DATA_XML_PATH, "tune", "scl", 0, 0);
                                if (temp_array5 != null && temp_array5.length >= 1) {
                                    String scl = temp_array5[0];
                                    ExynosDisplayUtils.sysfsWriteSting(this.SCL_SFR_SYSFS_PATH, scl);
                                }
                            } else if (i8 > 59) {
                                if (i8 > 60) {
                                    c = '=';
                                    if (i8 > 61) {
                                        break;
                                    }
                                    String[] temp_array6 = ExynosDisplayUtils.parserFactoryXMLText(this.CALIB_DATA_XML_PATH, "tune", "cgc17_con", 0, 0);
                                    if (temp_array6 != null) {
                                        i3 = 1;
                                        if (temp_array6.length >= 1) {
                                            i2 = 0;
                                            String cgc17_con = temp_array6[0];
                                            ExynosDisplayUtils.sysfsWriteSting(this.CGC17_CON_SYSFS_PATH, cgc17_con);
                                        } else {
                                            i2 = 0;
                                        }
                                    } else {
                                        i2 = 0;
                                        i3 = 1;
                                    }
                                    i8++;
                                    i5 = i3;
                                    str2 = str;
                                    i = i2;
                                } else if (item_en7 == 1) {
                                    ExynosDisplayUtils.sysfsWriteSting(this.CGC17_DEC_SYSFS_PATH, "7");
                                }
                            } else if (item_en7 == 1) {
                                int rgb = (i8 - 9) / 17;
                                int idx = (i8 - 9) % 17;
                                String[] temp_array7 = ExynosDisplayUtils.parserFactoryXMLText(this.CALIB_DATA_XML_PATH, "tune", "cgc17_enc", rgb, idx);
                                if (temp_array7 != null && temp_array7.length >= 1) {
                                    String cgc17_idx = Integer.toString(rgb) + " " + Integer.toString(idx);
                                    String cgc17_enc = temp_array7[0];
                                    ExynosDisplayUtils.sysfsWriteSting(this.CGC17_IDX_SYSFS_PATH, cgc17_idx);
                                    ExynosDisplayUtils.sysfsWriteSting(this.CGC17_ENC_SYSFS_PATH, cgc17_enc);
                                }
                                c = '=';
                                i2 = 0;
                                i3 = 1;
                                i8++;
                                i5 = i3;
                                str2 = str;
                                i = i2;
                            }
                        } else if (item_en7 == i5) {
                            int idx2 = i8 - 4;
                            str = str2;
                            String[] temp_array8 = ExynosDisplayUtils.parserFactoryXMLText(this.CALIB_DATA_XML_PATH, "tune", "hsc48_lcg", idx2, 0);
                            if (temp_array8 != null && temp_array8.length >= 1) {
                                String hsc48_idx = Integer.toString(idx2);
                                String hsc48_lcg = temp_array8[0];
                                ExynosDisplayUtils.sysfsWriteSting(this.HSC48_IDX_SYSFS_PATH, hsc48_idx);
                                ExynosDisplayUtils.sysfsWriteSting(this.HSC48_LCG_SYSFS_PATH, hsc48_lcg);
                            }
                            c = '=';
                            i2 = 0;
                            i3 = 1;
                            i8++;
                            i5 = i3;
                            str2 = str;
                            i = i2;
                        } else {
                            str = str2;
                        }
                        str = str2;
                        i2 = i;
                        i3 = i5;
                        c = '=';
                        i8++;
                        i5 = i3;
                        str2 = str;
                        i = i2;
                    }
                    c = '=';
                    i2 = 0;
                    i3 = 1;
                    i8++;
                    i5 = i3;
                    str2 = str;
                    i = i2;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            ExynosDisplayUtils.sendEmptyUpdate();
            ExynosDisplayUtils.sendEmptyUpdate();
            VSlog.d(TAG, "setCalibrationDQE-");
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void enableTuneDQE(boolean enable) {
        VSlog.d(TAG, "enableTuneDQE: enable=" + enable);
        if (enable) {
            setCalibrationDQE();
        }
    }
}