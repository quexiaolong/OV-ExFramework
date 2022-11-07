package com.android.server.display.color;

import android.os.Build;
import java.io.File;
import vivo.util.VSlog;

/* loaded from: classes.dex */
class ExynosDisplayColor {
    private static final String TAG = "ExynosDisplayColor";
    private final boolean DEBUG = "eng".equals(Build.TYPE);
    private String GAMMA_SYSFS_PATH = "/sys/class/dqe/dqe/gamma";
    private String CGC_SYSFS_PATH = "/sys/class/dqe/dqe/cgc";
    private String HSC_SYSFS_PATH = "/sys/class/dqe/dqe/hsc";
    private String XML_SYSFS_PATH = "/sys/class/dqe/dqe/xml";
    private String TUNE_MODE1_SYSFS_PATH = "/sys/class/dqe/dqe/tune_mode1";
    private String TUNE_MODE2_SYSFS_PATH = "/sys/class/dqe/dqe/tune_mode2";
    private String TUNE_MODE3_SYSFS_PATH = "/sys/class/dqe/dqe/tune_mode3";
    private String TUNE_ONOFF_SYSFS_PATH = "/sys/class/dqe/dqe/tune_onoff";
    private String COLOR_MODE_SYSFS_PATH = "/sys/class/dqe/dqe/aosp_colors";
    private String HW_VER_SYSFS_PATH = "/sys/class/dqe/dqe/dqe_ver";
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
    private String COLORTEMP_XML_FILE_PATH = "/vendor/etc/dqe/calib_data_colortemp.xml";
    private String EYETEMP_XML_FILE_PATH = "/vendor/etc/dqe/calib_data_eyetemp.xml";
    private String BYPASS_XML_FILE_PATH = "/vendor/etc/dqe/calib_data_bypass.xml";
    private String RGBGAIN_XML_FILE_PATH = "/vendor/etc/dqe/calib_data_rgbgain.xml";
    private String SKINCOLOR_XML_FILE_PATH = "/vendor/etc/dqe/calib_data_skincolor.xml";
    private String CE_XML_FILE_PATH = "/vendor/etc/dqe/calib_data_ce.xml";
    private String WHITEPOINT_XML_FILE_PATH = "/vendor/etc/dqe/calib_data_whitepoint.xml";
    private String[] colortemp_array = null;
    private String[] eyetemp_array = null;
    private String[] gamma_bypass_array = null;
    private String[] rgain_array = null;
    private String[] ggain_array = null;
    private String[] bgain_array = null;
    private String[] skincolor_array = null;
    private String[] whitepoint_array = null;
    private String[] hsc_bypass_array = null;
    private float[] rgb_gain = {1.0f, 1.0f, 1.0f};
    private String hw_ver = null;
    private int[] mItemEnableTable = null;
    private ExynosDisplayATC mExynosDisplayATC = null;

    public ExynosDisplayColor() {
        checkHWVersion();
    }

    protected void setExynosDisplayATC(ExynosDisplayATC mATC) {
        this.mExynosDisplayATC = mATC;
    }

    private boolean existFile(String file_path) {
        File file = new File(file_path);
        return file.exists() && file.isFile();
    }

    private void checkHWVersion() {
        this.hw_ver = null;
        if (!existFile(this.HW_VER_SYSFS_PATH)) {
            return;
        }
        this.hw_ver = ExynosDisplayUtils.getStringFromFile(this.HW_VER_SYSFS_PATH);
        VSlog.d(TAG, "hw_ver: " + this.hw_ver);
    }

    public void setColorMode(int value) {
        String[] temp_array;
        String xml_path = ExynosDisplayUtils.getStringFromFile(this.XML_SYSFS_PATH);
        if (xml_path == null || !ExynosDisplayUtils.existFile(xml_path)) {
            return;
        }
        String tune_mode1 = null;
        String tune_mode2 = null;
        String tune_mode3 = null;
        String tune_onoff = null;
        String color_mode = null;
        try {
            temp_array = ExynosDisplayUtils.parserXMLNodeText(xml_path, "version");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (temp_array != null && temp_array.length > 0) {
            VSlog.d(TAG, "xml verson: " + temp_array[0]);
            return;
        }
        String[] temp_array2 = ExynosDisplayUtils.parserXML(xml_path, "mode1", "dqe");
        if (temp_array2 != null && temp_array2.length == 3) {
            tune_mode1 = temp_array2[0] + "," + temp_array2[1] + "," + temp_array2[2] + ",";
        }
        String[] temp_array3 = ExynosDisplayUtils.parserXML(xml_path, "mode2", "dqe");
        if (temp_array3 != null && temp_array3.length == 3) {
            tune_mode2 = temp_array3[0] + "," + temp_array3[1] + "," + temp_array3[2] + ",";
        }
        String[] temp_array4 = ExynosDisplayUtils.parserXML(xml_path, "mode3", "dqe");
        if (temp_array4 != null && temp_array4.length == 3) {
            tune_mode3 = temp_array4[0] + "," + temp_array4[1] + "," + temp_array4[2] + ",";
        }
        String[] temp_array5 = ExynosDisplayUtils.parserXML(xml_path, "onoff", "dqe");
        if (temp_array5 != null && temp_array5.length == 3) {
            tune_onoff = temp_array5[0] + "," + temp_array5[1] + "," + temp_array5[2] + ",";
        }
        if (tune_mode1 != null) {
            ExynosDisplayUtils.sysfsWriteSting(this.TUNE_MODE1_SYSFS_PATH, tune_mode1);
        }
        if (tune_mode2 != null) {
            ExynosDisplayUtils.sysfsWriteSting(this.TUNE_MODE2_SYSFS_PATH, tune_mode2);
        }
        if (tune_mode3 != null) {
            ExynosDisplayUtils.sysfsWriteSting(this.TUNE_MODE3_SYSFS_PATH, tune_mode3);
        }
        if (tune_onoff != null) {
            ExynosDisplayUtils.sysfsWriteSting(this.TUNE_ONOFF_SYSFS_PATH, tune_onoff);
        }
        color_mode = ExynosDisplayUtils.getStringFromFile(this.COLOR_MODE_SYSFS_PATH);
        if (color_mode != null && !color_mode.equals("0")) {
            VSlog.d(TAG, "setColorMode(): value = " + value);
            ExynosDisplayUtils.sysfsWrite(this.COLOR_MODE_SYSFS_PATH, value);
        }
    }

    void setColorTempValue(int value) {
        String stream = null;
        try {
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (this.colortemp_array == null || this.colortemp_array.length == 0 || value >= this.colortemp_array.length) {
            return;
        }
        stream = this.colortemp_array[value];
        if (stream != null) {
            VSlog.d(TAG, "setColorTempValue()");
            ExynosDisplayUtils.sysfsWriteSting(this.GAMMA_SYSFS_PATH, stream);
        }
    }

    void setColorTempOn(int onoff) {
        String stream = null;
        try {
            if (onoff != 0) {
                this.colortemp_array = ExynosDisplayUtils.parserXML(this.COLORTEMP_XML_FILE_PATH, "colortemp", "gamma");
            } else {
                this.colortemp_array = null;
            }
            if (this.gamma_bypass_array == null) {
                this.gamma_bypass_array = ExynosDisplayUtils.parserXML(this.BYPASS_XML_FILE_PATH, "bypass", "gamma");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (this.gamma_bypass_array != null && this.gamma_bypass_array.length != 0) {
            stream = this.gamma_bypass_array[0];
            if (stream != null) {
                VSlog.d(TAG, "setColorTempOn()");
                ExynosDisplayUtils.sysfsWriteSting(this.GAMMA_SYSFS_PATH, stream);
            }
        }
    }

    void setEyeTempValue(int value) {
        String stream = null;
        try {
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (this.eyetemp_array == null || this.eyetemp_array.length == 0 || value >= this.eyetemp_array.length) {
            return;
        }
        stream = this.eyetemp_array[value];
        if (stream != null) {
            VSlog.d(TAG, "setEyeTempValue()");
            ExynosDisplayUtils.sysfsWriteSting(this.GAMMA_SYSFS_PATH, stream);
        }
    }

    void setEyeTempOn(int onoff) {
        String stream = null;
        try {
            if (onoff != 0) {
                this.eyetemp_array = ExynosDisplayUtils.parserXML(this.EYETEMP_XML_FILE_PATH, "eyetemp", "gamma");
            } else {
                this.eyetemp_array = null;
            }
            if (this.gamma_bypass_array == null) {
                this.gamma_bypass_array = ExynosDisplayUtils.parserXML(this.BYPASS_XML_FILE_PATH, "bypass", "gamma");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (this.gamma_bypass_array != null && this.gamma_bypass_array.length != 0) {
            stream = this.gamma_bypass_array[0];
            if (stream != null) {
                VSlog.d(TAG, "setEyeTempOn()");
                ExynosDisplayUtils.sysfsWriteSting(this.GAMMA_SYSFS_PATH, stream);
            }
        }
    }

    void setRgbGainValue(int r, int g, int b) {
        String stream = null;
        try {
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (this.rgain_array == null || this.rgain_array.length == 0 || this.ggain_array == null || this.ggain_array.length == 0 || this.bgain_array == null || this.bgain_array.length == 0 || r >= this.rgain_array.length || g >= this.ggain_array.length || b >= this.bgain_array.length) {
            return;
        }
        stream = this.rgain_array[r] + "," + this.ggain_array[g] + "," + this.bgain_array[b];
        if (stream != null) {
            VSlog.d(TAG, "setRgbGainValue()");
            ExynosDisplayUtils.sysfsWriteSting(this.GAMMA_SYSFS_PATH, stream);
        }
    }

    void setRgbGainOn(int onoff) {
        String stream = null;
        try {
            if (onoff != 0) {
                this.rgain_array = ExynosDisplayUtils.parserXML(this.RGBGAIN_XML_FILE_PATH, "rgbgain", "red");
                this.ggain_array = ExynosDisplayUtils.parserXML(this.RGBGAIN_XML_FILE_PATH, "rgbgain", "green");
                this.bgain_array = ExynosDisplayUtils.parserXML(this.RGBGAIN_XML_FILE_PATH, "rgbgain", "blue");
            } else {
                this.rgain_array = null;
                this.ggain_array = null;
                this.bgain_array = null;
            }
            if (this.gamma_bypass_array == null) {
                this.gamma_bypass_array = ExynosDisplayUtils.parserXML(this.BYPASS_XML_FILE_PATH, "bypass", "gamma");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (this.gamma_bypass_array != null && this.gamma_bypass_array.length != 0) {
            stream = this.gamma_bypass_array[0];
            if (stream != null) {
                VSlog.d(TAG, "setRgbGainOn()");
                ExynosDisplayUtils.sysfsWriteSting(this.GAMMA_SYSFS_PATH, stream);
            }
        }
    }

    void setSkinColorOn(int onoff) {
        String stream = null;
        try {
            if (onoff != 0) {
                this.skincolor_array = ExynosDisplayUtils.parserXML(this.SKINCOLOR_XML_FILE_PATH, "skincolor", "hsc");
            } else {
                this.skincolor_array = ExynosDisplayUtils.parserXML(this.BYPASS_XML_FILE_PATH, "bypass", "hsc");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (this.skincolor_array != null && this.skincolor_array.length != 0) {
            stream = this.skincolor_array[0];
            if (stream != null) {
                VSlog.d(TAG, "setSkinColorOn()");
                ExynosDisplayUtils.sysfsWriteSting(this.HSC_SYSFS_PATH, stream);
            }
        }
    }

    void setHsvGainValue(int h, int s, int v) {
        String stream = null;
        StringBuilder stringBuilder = new StringBuilder();
        try {
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (this.hsc_bypass_array != null && this.hsc_bypass_array.length != 0) {
            String[] hsc_lut = this.hsc_bypass_array[0].split(",");
            if (this.hw_ver == null) {
                hsc_lut[9] = Integer.toString(1);
                hsc_lut[10] = Integer.toString(1);
                hsc_lut[11] = Integer.toString(1);
                hsc_lut[12] = Integer.toString(s - 127);
                hsc_lut[13] = Integer.toString(h - 127);
                hsc_lut[14] = Integer.toString(v - 127);
                hsc_lut[146] = Integer.toString(255);
                hsc_lut[147] = Integer.toString(255);
                hsc_lut[148] = Integer.toString(255);
                hsc_lut[149] = Integer.toString(255);
                hsc_lut[150] = Integer.toString(255);
                hsc_lut[151] = Integer.toString(255);
                hsc_lut[152] = Integer.toString(255);
                hsc_lut[153] = Integer.toString(255);
            } else {
                hsc_lut[8] = Integer.toString(1);
                hsc_lut[9] = Integer.toString(h - 127);
                hsc_lut[10] = Integer.toString(1);
                hsc_lut[11] = Integer.toString(s - 127);
                hsc_lut[12] = Integer.toString(1);
                hsc_lut[13] = Integer.toString(v - 127);
                for (int i = 57; i <= 74; i++) {
                    hsc_lut[i] = Integer.toString(255);
                }
                hsc_lut[57] = Integer.toString(0);
                hsc_lut[66] = Integer.toString(0);
            }
            for (int i2 = 0; i2 < hsc_lut.length; i2++) {
                String temp_hsc = i2 < hsc_lut.length - 1 ? hsc_lut[i2] + "," : hsc_lut[i2];
                stringBuilder.append(temp_hsc);
            }
            if (stringBuilder.length() > 0) {
                stream = stringBuilder.toString();
            }
            if (stream != null) {
                VSlog.d(TAG, "setHsvGainValue()");
                ExynosDisplayUtils.sysfsWriteSting(this.HSC_SYSFS_PATH, stream);
            }
        }
    }

    void setHsvGainValueOldVer(int h, int s, int v) {
        String stream = null;
        StringBuilder stringBuilder = new StringBuilder();
        try {
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (this.hsc_bypass_array != null && this.hsc_bypass_array.length != 0) {
            String[] hsc_lut = this.hsc_bypass_array[0].split(",");
            hsc_lut[7] = Integer.toString(0);
            hsc_lut[9] = Integer.toString(1);
            hsc_lut[10] = Integer.toString(s - 127);
            hsc_lut[12] = Integer.toString(1);
            hsc_lut[13] = Integer.toString(h - 127);
            hsc_lut[14] = Integer.toString(h - 127);
            hsc_lut[15] = Integer.toString(h - 127);
            hsc_lut[16] = Integer.toString(h - 127);
            hsc_lut[17] = Integer.toString(h - 127);
            hsc_lut[18] = Integer.toString(h - 127);
            hsc_lut[22] = Integer.toString(255);
            hsc_lut[23] = Integer.toString(255);
            hsc_lut[24] = Integer.toString(255);
            hsc_lut[25] = Integer.toString(255);
            hsc_lut[26] = Integer.toString(255);
            hsc_lut[27] = Integer.toString(255);
            hsc_lut[28] = Integer.toString(255);
            hsc_lut[29] = Integer.toString(255);
            hsc_lut[30] = Integer.toString(0);
            for (int i = 0; i < hsc_lut.length; i++) {
                String temp_hsc = i < hsc_lut.length - 1 ? hsc_lut[i] + "," : hsc_lut[i];
                stringBuilder.append(temp_hsc);
            }
            if (stringBuilder.length() > 0) {
                stream = stringBuilder.toString();
            }
            if (stream != null) {
                VSlog.d(TAG, "setHsvGainValue()");
                ExynosDisplayUtils.sysfsWriteSting(this.HSC_SYSFS_PATH, stream);
            }
        }
    }

    void setHsvGainOn(int onoff) {
        String stream = null;
        try {
            if (this.hsc_bypass_array == null) {
                this.hsc_bypass_array = ExynosDisplayUtils.parserXML(this.BYPASS_XML_FILE_PATH, "bypass", "hsc");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (this.hsc_bypass_array != null && this.hsc_bypass_array.length != 0) {
            stream = this.hsc_bypass_array[0];
            if (stream != null) {
                VSlog.d(TAG, "setHsvGainOn()");
                ExynosDisplayUtils.sysfsWriteSting(this.HSC_SYSFS_PATH, stream);
            }
        }
    }

    String getColorEnhancementMode() {
        return "Off,UI,Gallery,Browser,Camera,Video";
    }

    void setColorEnhancement(int value) {
        if (!ExynosDisplayUtils.existFile(this.CE_XML_FILE_PATH)) {
            return;
        }
        String cgc_stream = null;
        String gamma_stream = null;
        String hsc_stream = null;
        String[] temp_array = null;
        try {
            if (value == 0) {
                temp_array = ExynosDisplayUtils.parserXML(this.CE_XML_FILE_PATH, "ce_off", "dqe");
            } else if (value == 1) {
                temp_array = ExynosDisplayUtils.parserXML(this.CE_XML_FILE_PATH, "ce_ui", "dqe");
            } else if (value != 2) {
                if (value == 3) {
                    temp_array = ExynosDisplayUtils.parserXML(this.CE_XML_FILE_PATH, "ce_browser", "dqe");
                } else if (value == 4) {
                    temp_array = ExynosDisplayUtils.parserXML(this.CE_XML_FILE_PATH, "ce_camera", "dqe");
                } else if (value == 5) {
                    temp_array = ExynosDisplayUtils.parserXML(this.CE_XML_FILE_PATH, "ce_video", "dqe");
                }
            } else {
                temp_array = ExynosDisplayUtils.parserXML(this.CE_XML_FILE_PATH, "ce_gallery", "dqe");
            }
            if (temp_array != null && temp_array.length == 3) {
                cgc_stream = temp_array[0];
                gamma_stream = temp_array[1];
                hsc_stream = temp_array[2];
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

    private void setWhitePointColorOnCGC17(int onoff) {
        String xml_path;
        String mode_name;
        if (onoff != 0) {
            xml_path = this.WHITEPOINT_XML_FILE_PATH;
            mode_name = "whitepoint";
        } else {
            xml_path = this.BYPASS_XML_FILE_PATH;
            mode_name = "bypass";
        }
        for (int rgb = 0; rgb < 3; rgb++) {
            for (int idx = 0; idx < 17; idx++) {
                try {
                    String[] temp_array = ExynosDisplayUtils.parserFactoryXMLText(xml_path, mode_name, "cgc17_enc", rgb, idx);
                    if (temp_array != null && temp_array.length >= 1) {
                        String cgc17_enc = temp_array[0];
                        String cgc17_idx = Integer.toString(rgb) + " " + Integer.toString(idx);
                        ExynosDisplayUtils.sysfsWriteSting(this.CGC17_IDX_SYSFS_PATH, cgc17_idx);
                        ExynosDisplayUtils.sysfsWriteSting(this.CGC17_ENC_SYSFS_PATH, cgc17_enc);
                    }
                    VSlog.d(TAG, "xml cgc17_enc not found");
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
        ExynosDisplayUtils.sysfsWriteSting(this.CGC17_DEC_SYSFS_PATH, "7");
        String[] temp_array2 = ExynosDisplayUtils.parserFactoryXMLText(xml_path, mode_name, "cgc17_con", 0, 0);
        if (temp_array2 != null && temp_array2.length >= 1) {
            String cgc17_con = temp_array2[0];
            ExynosDisplayUtils.sysfsWriteSting(this.CGC17_CON_SYSFS_PATH, cgc17_con);
            return;
        }
        VSlog.d(TAG, "xml cgc17_con not found");
    }

    private void setWhitePointColorOnCGC(int onoff) {
        String stream = null;
        try {
            if (onoff != 0) {
                this.whitepoint_array = ExynosDisplayUtils.parserXML(this.WHITEPOINT_XML_FILE_PATH, "whitepoint", "cgc");
            } else {
                this.whitepoint_array = ExynosDisplayUtils.parserXML(this.BYPASS_XML_FILE_PATH, "bypass", "cgc");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (this.whitepoint_array != null && this.whitepoint_array.length != 0) {
            stream = this.whitepoint_array[0];
            if (stream != null) {
                VSlog.d(TAG, "setWhitePointColorOn()");
                ExynosDisplayUtils.sysfsWriteSting(this.CGC_SYSFS_PATH, stream);
            }
        }
    }

    void setWhitePointColorOn(int onoff) {
        long mStartTimeMillis = System.currentTimeMillis();
        if (this.hw_ver == null) {
            setWhitePointColorOnCGC(onoff);
        } else {
            setWhitePointColorOnCGC17(onoff);
        }
        VSlog.d(TAG, "elaspedTime: " + (System.currentTimeMillis() - mStartTimeMillis));
    }

    /* JADX WARN: Removed duplicated region for block: B:49:0x00dd  */
    /* JADX WARN: Removed duplicated region for block: B:64:? A[RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    void setRgbGain(float r16, float r17, float r18) {
        /*
            Method dump skipped, instructions count: 271
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.display.color.ExynosDisplayColor.setRgbGain(float, float, float):void");
    }

    float[] getRgbGain() {
        return this.rgb_gain;
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

    private void setXMLColorModesImpl(String xml_path, String mode_name) {
        int i;
        String[] temp_array;
        ExynosDisplayATC exynosDisplayATC = this.mExynosDisplayATC;
        if (exynosDisplayATC != null) {
            exynosDisplayATC.parserATCXML(xml_path, mode_name);
        }
        this.mItemEnableTable = new int[90];
        for (int i2 = 0; i2 < 90; i2++) {
            this.mItemEnableTable[i2] = 0;
        }
        int item_en = getItemEnable(xml_path, mode_name, "degamma");
        VSlog.d(TAG, "degamma: enable = " + item_en);
        int i3 = 1;
        if (item_en > 0) {
            this.mItemEnableTable[1] = 1;
        }
        int item_en2 = getItemEnable(xml_path, mode_name, "gamma");
        VSlog.d(TAG, "gamma: enable = " + item_en2);
        if (item_en2 > 0) {
            this.mItemEnableTable[2] = 1;
        }
        int item_en3 = getItemEnable(xml_path, mode_name, "gamma_matrix");
        VSlog.d(TAG, "gamma_matrix: enable = " + item_en3);
        if (item_en3 > 0) {
            this.mItemEnableTable[3] = 1;
        }
        int item_en4 = getItemEnable(xml_path, mode_name, "hsc");
        VSlog.d(TAG, "hsc: enable = " + item_en4);
        for (int i4 = 4; i4 <= 7; i4++) {
            if (item_en4 > 0) {
                this.mItemEnableTable[i4] = 1;
            }
        }
        int item_en5 = getItemEnable(xml_path, mode_name, "scl");
        VSlog.d(TAG, "scl: enable = " + item_en5);
        if (item_en5 > 0) {
            this.mItemEnableTable[8] = 1;
        }
        int item_en6 = getItemEnable(xml_path, mode_name, "cgc17_con");
        VSlog.d(TAG, "cgc17_con: enable = " + item_en6);
        for (int i5 = 9; i5 <= 61; i5++) {
            if (item_en6 > 0) {
                this.mItemEnableTable[i5] = 1;
            }
        }
        int i6 = 0;
        for (int i7 = 90; i6 < i7; i7 = 90) {
            try {
                int item_en7 = this.mItemEnableTable[i6];
                if (i6 > 0) {
                    if (i6 <= i3) {
                        String[] temp_array2 = ExynosDisplayUtils.parserFactoryXMLText(xml_path, mode_name, "degamma", 0, 0);
                        if (temp_array2 != null && temp_array2.length >= i3) {
                            String degamma = temp_array2[0];
                            ExynosDisplayUtils.sysfsWriteSting(this.DEGAMMA_SFR_SYSFS_PATH, degamma);
                        }
                    } else if (i6 <= 2) {
                        String[] temp_array3 = ExynosDisplayUtils.parserFactoryXMLText(xml_path, mode_name, "gamma", 0, 0);
                        if (temp_array3 != null && temp_array3.length >= i3) {
                            String gamma = temp_array3[0];
                            ExynosDisplayUtils.sysfsWriteSting(this.GAMMA_SFR_SYSFS_PATH, gamma);
                        }
                    } else if (i6 <= 3) {
                        String[] temp_array4 = ExynosDisplayUtils.parserFactoryXMLText(xml_path, mode_name, "gamma_matrix", 0, 0);
                        if (temp_array4 != null && temp_array4.length >= i3) {
                            String gamma_matrix = temp_array4[0];
                            ExynosDisplayUtils.sysfsWriteSting(this.GAMMA_MATRIX_SYSFS_PATH, gamma_matrix);
                        }
                    } else if (i6 > 6) {
                        if (i6 <= 7) {
                            String[] temp_array5 = ExynosDisplayUtils.parserFactoryXMLText(xml_path, mode_name, "hsc", 0, 0);
                            if (temp_array5 != null && temp_array5.length >= 1) {
                                String hsc = temp_array5[0];
                                ExynosDisplayUtils.sysfsWriteSting(this.HSC_SFR_SYSFS_PATH, hsc);
                            }
                        } else if (i6 <= 8) {
                            String[] temp_array6 = ExynosDisplayUtils.parserFactoryXMLText(xml_path, mode_name, "scl", 0, 0);
                            if (temp_array6 != null && temp_array6.length >= 1) {
                                String scl = temp_array6[0];
                                ExynosDisplayUtils.sysfsWriteSting(this.SCL_SFR_SYSFS_PATH, scl);
                            }
                        } else if (i6 > 59) {
                            if (i6 > 60) {
                                if (i6 > 61) {
                                    break;
                                }
                                String[] temp_array7 = ExynosDisplayUtils.parserFactoryXMLText(xml_path, mode_name, "cgc17_con", 0, 0);
                                if (temp_array7 != null) {
                                    int length = temp_array7.length;
                                    i = 1;
                                    if (length >= 1) {
                                        String cgc17_con = temp_array7[0];
                                        ExynosDisplayUtils.sysfsWriteSting(this.CGC17_CON_SYSFS_PATH, cgc17_con);
                                    }
                                } else {
                                    i = 1;
                                }
                                i6++;
                                i3 = i;
                            } else if (item_en7 == 1) {
                                ExynosDisplayUtils.sysfsWriteSting(this.CGC17_DEC_SYSFS_PATH, "7");
                            }
                        } else if (item_en7 == 1) {
                            int rgb = (i6 - 9) / 17;
                            int idx = (i6 - 9) % 17;
                            String[] temp_array8 = ExynosDisplayUtils.parserFactoryXMLText(xml_path, mode_name, "cgc17_enc", rgb, idx);
                            if (temp_array8 != null) {
                                try {
                                    if (temp_array8.length >= 1) {
                                        String cgc17_idx = Integer.toString(rgb) + " " + Integer.toString(idx);
                                        String cgc17_enc = temp_array8[0];
                                        temp_array = temp_array8;
                                        try {
                                            ExynosDisplayUtils.sysfsWriteSting(this.CGC17_IDX_SYSFS_PATH, cgc17_idx);
                                            ExynosDisplayUtils.sysfsWriteSting(this.CGC17_ENC_SYSFS_PATH, cgc17_enc);
                                            i = 1;
                                            i6++;
                                            i3 = i;
                                        } catch (Exception e) {
                                            e = e;
                                            e.printStackTrace();
                                            return;
                                        }
                                    }
                                } catch (Exception e2) {
                                    e = e2;
                                }
                            }
                            temp_array = temp_array8;
                            i = 1;
                            i6++;
                            i3 = i;
                        }
                    } else if (item_en7 == i3) {
                        int idx2 = i6 - 4;
                        String[] temp_array9 = ExynosDisplayUtils.parserFactoryXMLText(xml_path, mode_name, "hsc48_lcg", idx2, 0);
                        if (temp_array9 != null && temp_array9.length >= i3) {
                            String hsc48_idx = Integer.toString(idx2);
                            String hsc48_lcg = temp_array9[0];
                            ExynosDisplayUtils.sysfsWriteSting(this.HSC48_IDX_SYSFS_PATH, hsc48_idx);
                            ExynosDisplayUtils.sysfsWriteSting(this.HSC48_LCG_SYSFS_PATH, hsc48_lcg);
                        }
                        i = 1;
                        i6++;
                        i3 = i;
                    }
                    i = i3;
                    i6++;
                    i3 = i;
                }
                i = 1;
                i6++;
                i3 = i;
            } catch (Exception e3) {
                e = e3;
            }
        }
        ExynosDisplayUtils.sendEmptyUpdate();
        ExynosDisplayUtils.sendEmptyUpdate();
    }

    private void setProductXMLColorModes(String mode_name) {
        String xml_path = ExynosDisplayUtils.getStringFromFile(this.XML_SYSFS_PATH);
        if (!ExynosDisplayUtils.existFile(xml_path)) {
            return;
        }
        VSlog.d(TAG, "setProductXMLColorModes: xml_path=" + xml_path + ", mode_name=" + mode_name);
        if (getXMLVersion(xml_path) == null) {
            return;
        }
        setXMLColorModesImpl(xml_path, mode_name);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDisplayColorFeature(int arg1, int arg2, String arg3) {
        VSlog.d(TAG, "setDisplayColorFeature(): " + arg1 + "  " + arg2 + "  " + arg3);
        if (arg1 == 0 && arg2 == 0 && arg3 != null) {
            setProductXMLColorModes(arg3);
        }
    }
}