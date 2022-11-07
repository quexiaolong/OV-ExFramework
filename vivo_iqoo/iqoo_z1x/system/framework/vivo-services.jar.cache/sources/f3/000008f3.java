package vendor.pixelworks.hardware.display.V1_0;

import java.util.ArrayList;

/* loaded from: classes.dex */
public final class KernelConfig {
    public static final int AL_ENABLE = 44;
    public static final int ANALOG_BYPASS_MODE = 56;
    public static final int APP_FILTER = 111;
    public static final int AP_TE = 129;
    public static final int BLACK_BORDER = 22;
    public static final int BLACK_LIST_ADD = 24;
    public static final int BLACK_LIST_RST = 25;
    public static final int BLC_PWM_ENABLE = 68;
    public static final int BRIGHTNESS = 7;
    public static final int BRIGHTNESS_CHIP = 82;
    public static final int BYPASS_ENABLE = 30;
    public static final int CCF1_UPDATE = 71;
    public static final int CCF2_UPDATE = 72;
    public static final int CCT_VALUE = 35;
    public static final int CHIP_VERSION = 33;
    public static final int CINEMA_MODE = 23;
    public static final int CM_6AXES = 37;
    public static final int CM_BLUE_GAIN = 66;
    public static final int CM_COLOR_GAMUT = 40;
    public static final int CM_COLOR_GAMUT_PRE = 51;
    public static final int CM_COLOR_TEMP_MODE = 39;
    public static final int CM_CYAN_GAIN = 67;
    public static final int CM_FTC_ENABLE = 38;
    public static final int CM_GREEN_GAIN = 65;
    public static final int CM_MAGENTA_GAIN = 62;
    public static final int CM_RED_GAIN = 63;
    public static final int CM_SETTING = 32;
    public static final int CM_YELLOW_GAIN = 64;
    public static final int COLOR_ADJUST = 26;
    public static final int COLOR_TEMP_VALUE = 48;
    public static final int CONTRAST = 6;
    public static final int CONTRAST_DIMMING = 80;
    public static final int CSC_MATRIX = 75;
    public static final int DBC_CONFIG = 11;
    public static final int DBC_LCE_DATA_PATH = 53;
    public static final int DBC_LCE_POWER = 52;
    public static final int DBC_LED_GAIN = 69;
    public static final int DBC_LEVEL = 45;
    public static final int DBC_QUALITY = 9;
    public static final int DBG_KERNEL_LOG_LEVEL = 106;
    public static final int DBG_LOOP_BACK_MODE = 108;
    public static final int DBG_LOOP_BACK_MODE_RES = 109;
    public static final int DBG_SEND_PACKAGE = 107;
    public static final int DBG_TARGET_PI_REGADDR_SET = 102;
    public static final int DBG_TARGET_REGADDR_VALUE_GET = 103;
    public static final int DBG_TARGET_REGADDR_VALUE_SET = 105;
    public static final int DBG_TARGET_REGADDR_VALUE_SET2 = 112;
    public static final int DCE_LEVEL = 16;
    public static final int DEBUG_CAP = 113;
    public static final int DEMO_MODE = 46;
    public static final int DLV_SENSITIVITY = 10;
    public static final int DMA_LOAD = 55;
    public static final int DUAL2SINGLE_ST = 134;
    public static final int DYNAMIC_POWER_CTRL = 54;
    public static final int EXTERNAL_PWM = 8;
    public static final int FRC_LOW_LATENCY = 127;
    public static final int FW_UPDATE = 73;
    public static final int GAMMA = 4;
    public static final int GAMMA_TABLE_ENABLE = 58;
    public static final int GRAPHIC_DET_ENABLE = 43;
    public static final int HDR_COMPLETE = 91;
    public static final int HDR_MAXCLL = 49;
    public static final int HDR_PANEL_NITES_SET = 60;
    public static final int HDR_PREPARE = 90;
    public static final int HDR_SETTING = 50;
    public static final int HUE_SAT_ADJ = 74;
    public static final int LAYER_SIZE = 21;
    public static final int LCE_DEMO_WINDOW = 137;
    public static final int LCE_LEVEL = 42;
    public static final int LCE_MODE = 41;
    public static final int LCE_SETTING = 31;
    public static final int LUX_VALUE = 34;
    public static final int MCF_DATA = 92;
    public static final int MEMC_ACTIVE = 18;
    public static final int MEMC_DEMO = 2;
    public static final int MEMC_ENABLE = 13;
    public static final int MEMC_ENABLE_FOR_ASUS_CAMERA = 104;
    public static final int MEMC_LEVEL = 5;
    public static final int MEMC_LOWPOWER = 15;
    public static final int MEMC_OPTION = 14;
    public static final int MEMC_OSD = 135;
    public static final int MEMC_OSD_PROTECT = 136;
    public static final int MIPI2RX_PWRST = 133;
    public static final int MODE_SET = 120;
    public static final int N2M_ENABLE = 130;
    public static final int OSD_AUTOREFRESH = 124;
    public static final int OSD_ENABLE = 123;
    public static final int OSD_OVERFLOW_ST = 125;
    public static final int OSD_PATTERN_SHOW = 101;
    public static final int OUT_FRAME_RATE_SET = 122;
    public static final int PANEL_NITS = 99;
    public static final int PANEL_TE = 128;
    public static final int PANEL_TYPE = 57;
    public static final int PEAKING = 0;
    public static final int PEAKING_DEMO = 3;
    public static final int PEAKING_IDLE_CLK_ENABLE = 61;
    public static final int PQ_CONFIG = 12;
    public static final int PT_ENABLE = 29;
    public static final int READING_MODE = 36;
    public static final int SCALER_FILTER_LEVEL = 70;
    public static final int SCALER_PP_FILTER_LEVEL = 76;
    public static final int SDR2HDR = 47;
    public static final int SEND_FRAME = 100;
    public static final int SHARPNESS = 1;
    public static final int S_CURVE = 81;
    public static final int TRUE_CUT_CAP = 27;
    public static final int TRUE_CUT_DET = 28;
    public static final int TYPE_MAX = 138;
    public static final int USER_DEMO_WND = 17;
    public static final int VIDEO_FRAME_RATE_SET = 121;
    public static final int WAIT_VSYNC = 132;
    public static final int WHITE_LIST_ADD = 19;
    public static final int WHITE_LIST_RST = 20;
    public static final int WORK_MODE = 126;
    public static final int Y5P = 93;

    public static final String toString(int o) {
        if (o == 0) {
            return "PEAKING";
        }
        if (o == 1) {
            return "SHARPNESS";
        }
        if (o == 2) {
            return "MEMC_DEMO";
        }
        if (o == 3) {
            return "PEAKING_DEMO";
        }
        if (o == 4) {
            return "GAMMA";
        }
        if (o == 5) {
            return "MEMC_LEVEL";
        }
        if (o == 6) {
            return "CONTRAST";
        }
        if (o == 7) {
            return "BRIGHTNESS";
        }
        if (o == 8) {
            return "EXTERNAL_PWM";
        }
        if (o == 9) {
            return "DBC_QUALITY";
        }
        if (o == 10) {
            return "DLV_SENSITIVITY";
        }
        if (o == 11) {
            return "DBC_CONFIG";
        }
        if (o == 12) {
            return "PQ_CONFIG";
        }
        if (o == 13) {
            return "MEMC_ENABLE";
        }
        if (o == 14) {
            return "MEMC_OPTION";
        }
        if (o == 15) {
            return "MEMC_LOWPOWER";
        }
        if (o == 16) {
            return "DCE_LEVEL";
        }
        if (o == 17) {
            return "USER_DEMO_WND";
        }
        if (o == 18) {
            return "MEMC_ACTIVE";
        }
        if (o == 19) {
            return "WHITE_LIST_ADD";
        }
        if (o == 20) {
            return "WHITE_LIST_RST";
        }
        if (o == 21) {
            return "LAYER_SIZE";
        }
        if (o == 22) {
            return "BLACK_BORDER";
        }
        if (o == 23) {
            return "CINEMA_MODE";
        }
        if (o == 24) {
            return "BLACK_LIST_ADD";
        }
        if (o == 25) {
            return "BLACK_LIST_RST";
        }
        if (o == 26) {
            return "COLOR_ADJUST";
        }
        if (o == 27) {
            return "TRUE_CUT_CAP";
        }
        if (o == 28) {
            return "TRUE_CUT_DET";
        }
        if (o == 29) {
            return "PT_ENABLE";
        }
        if (o == 30) {
            return "BYPASS_ENABLE";
        }
        if (o == 31) {
            return "LCE_SETTING";
        }
        if (o == 32) {
            return "CM_SETTING";
        }
        if (o == 33) {
            return "CHIP_VERSION";
        }
        if (o == 34) {
            return "LUX_VALUE";
        }
        if (o == 35) {
            return "CCT_VALUE";
        }
        if (o == 36) {
            return "READING_MODE";
        }
        if (o == 37) {
            return "CM_6AXES";
        }
        if (o == 38) {
            return "CM_FTC_ENABLE";
        }
        if (o == 39) {
            return "CM_COLOR_TEMP_MODE";
        }
        if (o == 40) {
            return "CM_COLOR_GAMUT";
        }
        if (o == 41) {
            return "LCE_MODE";
        }
        if (o == 42) {
            return "LCE_LEVEL";
        }
        if (o == 43) {
            return "GRAPHIC_DET_ENABLE";
        }
        if (o == 44) {
            return "AL_ENABLE";
        }
        if (o == 45) {
            return "DBC_LEVEL";
        }
        if (o == 46) {
            return "DEMO_MODE";
        }
        if (o == 47) {
            return "SDR2HDR";
        }
        if (o == 48) {
            return "COLOR_TEMP_VALUE";
        }
        if (o == 49) {
            return "HDR_MAXCLL";
        }
        if (o == 50) {
            return "HDR_SETTING";
        }
        if (o == 51) {
            return "CM_COLOR_GAMUT_PRE";
        }
        if (o == 52) {
            return "DBC_LCE_POWER";
        }
        if (o == 53) {
            return "DBC_LCE_DATA_PATH";
        }
        if (o == 54) {
            return "DYNAMIC_POWER_CTRL";
        }
        if (o == 55) {
            return "DMA_LOAD";
        }
        if (o == 56) {
            return "ANALOG_BYPASS_MODE";
        }
        if (o == 57) {
            return "PANEL_TYPE";
        }
        if (o == 58) {
            return "GAMMA_TABLE_ENABLE";
        }
        if (o == 60) {
            return "HDR_PANEL_NITES_SET";
        }
        if (o == 61) {
            return "PEAKING_IDLE_CLK_ENABLE";
        }
        if (o == 62) {
            return "CM_MAGENTA_GAIN";
        }
        if (o == 63) {
            return "CM_RED_GAIN";
        }
        if (o == 64) {
            return "CM_YELLOW_GAIN";
        }
        if (o == 65) {
            return "CM_GREEN_GAIN";
        }
        if (o == 66) {
            return "CM_BLUE_GAIN";
        }
        if (o == 67) {
            return "CM_CYAN_GAIN";
        }
        if (o == 68) {
            return "BLC_PWM_ENABLE";
        }
        if (o == 69) {
            return "DBC_LED_GAIN";
        }
        if (o == 70) {
            return "SCALER_FILTER_LEVEL";
        }
        if (o == 71) {
            return "CCF1_UPDATE";
        }
        if (o == 72) {
            return "CCF2_UPDATE";
        }
        if (o == 73) {
            return "FW_UPDATE";
        }
        if (o == 74) {
            return "HUE_SAT_ADJ";
        }
        if (o == 75) {
            return "CSC_MATRIX";
        }
        if (o == 76) {
            return "SCALER_PP_FILTER_LEVEL";
        }
        if (o == 80) {
            return "CONTRAST_DIMMING";
        }
        if (o == 81) {
            return "S_CURVE";
        }
        if (o == 82) {
            return "BRIGHTNESS_CHIP";
        }
        if (o == 90) {
            return "HDR_PREPARE";
        }
        if (o == 91) {
            return "HDR_COMPLETE";
        }
        if (o == 92) {
            return "MCF_DATA";
        }
        if (o == 93) {
            return "Y5P";
        }
        if (o == 99) {
            return "PANEL_NITS";
        }
        if (o == 100) {
            return "SEND_FRAME";
        }
        if (o == 101) {
            return "OSD_PATTERN_SHOW";
        }
        if (o == 102) {
            return "DBG_TARGET_PI_REGADDR_SET";
        }
        if (o == 103) {
            return "DBG_TARGET_REGADDR_VALUE_GET";
        }
        if (o == 104) {
            return "MEMC_ENABLE_FOR_ASUS_CAMERA";
        }
        if (o == 105) {
            return "DBG_TARGET_REGADDR_VALUE_SET";
        }
        if (o == 106) {
            return "DBG_KERNEL_LOG_LEVEL";
        }
        if (o == 107) {
            return "DBG_SEND_PACKAGE";
        }
        if (o == 108) {
            return "DBG_LOOP_BACK_MODE";
        }
        if (o == 109) {
            return "DBG_LOOP_BACK_MODE_RES";
        }
        if (o == 111) {
            return "APP_FILTER";
        }
        if (o == 112) {
            return "DBG_TARGET_REGADDR_VALUE_SET2";
        }
        if (o == 113) {
            return "DEBUG_CAP";
        }
        if (o == 120) {
            return "MODE_SET";
        }
        if (o == 121) {
            return "VIDEO_FRAME_RATE_SET";
        }
        if (o == 122) {
            return "OUT_FRAME_RATE_SET";
        }
        if (o == 123) {
            return "OSD_ENABLE";
        }
        if (o == 124) {
            return "OSD_AUTOREFRESH";
        }
        if (o == 125) {
            return "OSD_OVERFLOW_ST";
        }
        if (o == 126) {
            return "WORK_MODE";
        }
        if (o == 127) {
            return "FRC_LOW_LATENCY";
        }
        if (o == 128) {
            return "PANEL_TE";
        }
        if (o == 129) {
            return "AP_TE";
        }
        if (o == 130) {
            return "N2M_ENABLE";
        }
        if (o == 132) {
            return "WAIT_VSYNC";
        }
        if (o == 133) {
            return "MIPI2RX_PWRST";
        }
        if (o == 134) {
            return "DUAL2SINGLE_ST";
        }
        if (o == 135) {
            return "MEMC_OSD";
        }
        if (o == 136) {
            return "MEMC_OSD_PROTECT";
        }
        if (o == 137) {
            return "LCE_DEMO_WINDOW";
        }
        if (o == 138) {
            return "TYPE_MAX";
        }
        return "0x" + Integer.toHexString(o);
    }

    public static final String dumpBitfield(int o) {
        ArrayList<String> list = new ArrayList<>();
        int flipped = 0;
        list.add("PEAKING");
        if ((o & 1) == 1) {
            list.add("SHARPNESS");
            flipped = 0 | 1;
        }
        if ((o & 2) == 2) {
            list.add("MEMC_DEMO");
            flipped |= 2;
        }
        if ((o & 3) == 3) {
            list.add("PEAKING_DEMO");
            flipped |= 3;
        }
        if ((o & 4) == 4) {
            list.add("GAMMA");
            flipped |= 4;
        }
        if ((o & 5) == 5) {
            list.add("MEMC_LEVEL");
            flipped |= 5;
        }
        if ((o & 6) == 6) {
            list.add("CONTRAST");
            flipped |= 6;
        }
        if ((o & 7) == 7) {
            list.add("BRIGHTNESS");
            flipped |= 7;
        }
        if ((o & 8) == 8) {
            list.add("EXTERNAL_PWM");
            flipped |= 8;
        }
        if ((o & 9) == 9) {
            list.add("DBC_QUALITY");
            flipped |= 9;
        }
        if ((o & 10) == 10) {
            list.add("DLV_SENSITIVITY");
            flipped |= 10;
        }
        if ((o & 11) == 11) {
            list.add("DBC_CONFIG");
            flipped |= 11;
        }
        if ((o & 12) == 12) {
            list.add("PQ_CONFIG");
            flipped |= 12;
        }
        if ((o & 13) == 13) {
            list.add("MEMC_ENABLE");
            flipped |= 13;
        }
        if ((o & 14) == 14) {
            list.add("MEMC_OPTION");
            flipped |= 14;
        }
        if ((o & 15) == 15) {
            list.add("MEMC_LOWPOWER");
            flipped |= 15;
        }
        if ((o & 16) == 16) {
            list.add("DCE_LEVEL");
            flipped |= 16;
        }
        if ((o & 17) == 17) {
            list.add("USER_DEMO_WND");
            flipped |= 17;
        }
        if ((o & 18) == 18) {
            list.add("MEMC_ACTIVE");
            flipped |= 18;
        }
        if ((o & 19) == 19) {
            list.add("WHITE_LIST_ADD");
            flipped |= 19;
        }
        if ((o & 20) == 20) {
            list.add("WHITE_LIST_RST");
            flipped |= 20;
        }
        if ((o & 21) == 21) {
            list.add("LAYER_SIZE");
            flipped |= 21;
        }
        if ((o & 22) == 22) {
            list.add("BLACK_BORDER");
            flipped |= 22;
        }
        if ((o & 23) == 23) {
            list.add("CINEMA_MODE");
            flipped |= 23;
        }
        if ((o & 24) == 24) {
            list.add("BLACK_LIST_ADD");
            flipped |= 24;
        }
        if ((o & 25) == 25) {
            list.add("BLACK_LIST_RST");
            flipped |= 25;
        }
        if ((o & 26) == 26) {
            list.add("COLOR_ADJUST");
            flipped |= 26;
        }
        if ((o & 27) == 27) {
            list.add("TRUE_CUT_CAP");
            flipped |= 27;
        }
        if ((o & 28) == 28) {
            list.add("TRUE_CUT_DET");
            flipped |= 28;
        }
        if ((o & 29) == 29) {
            list.add("PT_ENABLE");
            flipped |= 29;
        }
        if ((o & 30) == 30) {
            list.add("BYPASS_ENABLE");
            flipped |= 30;
        }
        if ((o & 31) == 31) {
            list.add("LCE_SETTING");
            flipped |= 31;
        }
        if ((o & 32) == 32) {
            list.add("CM_SETTING");
            flipped |= 32;
        }
        if ((o & 33) == 33) {
            list.add("CHIP_VERSION");
            flipped |= 33;
        }
        if ((o & 34) == 34) {
            list.add("LUX_VALUE");
            flipped |= 34;
        }
        if ((o & 35) == 35) {
            list.add("CCT_VALUE");
            flipped |= 35;
        }
        if ((o & 36) == 36) {
            list.add("READING_MODE");
            flipped |= 36;
        }
        if ((o & 37) == 37) {
            list.add("CM_6AXES");
            flipped |= 37;
        }
        if ((o & 38) == 38) {
            list.add("CM_FTC_ENABLE");
            flipped |= 38;
        }
        if ((o & 39) == 39) {
            list.add("CM_COLOR_TEMP_MODE");
            flipped |= 39;
        }
        if ((o & 40) == 40) {
            list.add("CM_COLOR_GAMUT");
            flipped |= 40;
        }
        if ((o & 41) == 41) {
            list.add("LCE_MODE");
            flipped |= 41;
        }
        if ((o & 42) == 42) {
            list.add("LCE_LEVEL");
            flipped |= 42;
        }
        if ((o & 43) == 43) {
            list.add("GRAPHIC_DET_ENABLE");
            flipped |= 43;
        }
        if ((o & 44) == 44) {
            list.add("AL_ENABLE");
            flipped |= 44;
        }
        if ((o & 45) == 45) {
            list.add("DBC_LEVEL");
            flipped |= 45;
        }
        if ((o & 46) == 46) {
            list.add("DEMO_MODE");
            flipped |= 46;
        }
        if ((o & 47) == 47) {
            list.add("SDR2HDR");
            flipped |= 47;
        }
        if ((o & 48) == 48) {
            list.add("COLOR_TEMP_VALUE");
            flipped |= 48;
        }
        if ((o & 49) == 49) {
            list.add("HDR_MAXCLL");
            flipped |= 49;
        }
        if ((o & 50) == 50) {
            list.add("HDR_SETTING");
            flipped |= 50;
        }
        if ((o & 51) == 51) {
            list.add("CM_COLOR_GAMUT_PRE");
            flipped |= 51;
        }
        if ((o & 52) == 52) {
            list.add("DBC_LCE_POWER");
            flipped |= 52;
        }
        if ((o & 53) == 53) {
            list.add("DBC_LCE_DATA_PATH");
            flipped |= 53;
        }
        if ((o & 54) == 54) {
            list.add("DYNAMIC_POWER_CTRL");
            flipped |= 54;
        }
        if ((o & 55) == 55) {
            list.add("DMA_LOAD");
            flipped |= 55;
        }
        if ((o & 56) == 56) {
            list.add("ANALOG_BYPASS_MODE");
            flipped |= 56;
        }
        if ((o & 57) == 57) {
            list.add("PANEL_TYPE");
            flipped |= 57;
        }
        if ((o & 58) == 58) {
            list.add("GAMMA_TABLE_ENABLE");
            flipped |= 58;
        }
        if ((o & 60) == 60) {
            list.add("HDR_PANEL_NITES_SET");
            flipped |= 60;
        }
        if ((o & 61) == 61) {
            list.add("PEAKING_IDLE_CLK_ENABLE");
            flipped |= 61;
        }
        if ((o & 62) == 62) {
            list.add("CM_MAGENTA_GAIN");
            flipped |= 62;
        }
        if ((o & 63) == 63) {
            list.add("CM_RED_GAIN");
            flipped |= 63;
        }
        if ((o & 64) == 64) {
            list.add("CM_YELLOW_GAIN");
            flipped |= 64;
        }
        if ((o & 65) == 65) {
            list.add("CM_GREEN_GAIN");
            flipped |= 65;
        }
        if ((o & 66) == 66) {
            list.add("CM_BLUE_GAIN");
            flipped |= 66;
        }
        if ((o & 67) == 67) {
            list.add("CM_CYAN_GAIN");
            flipped |= 67;
        }
        if ((o & 68) == 68) {
            list.add("BLC_PWM_ENABLE");
            flipped |= 68;
        }
        if ((o & 69) == 69) {
            list.add("DBC_LED_GAIN");
            flipped |= 69;
        }
        if ((o & 70) == 70) {
            list.add("SCALER_FILTER_LEVEL");
            flipped |= 70;
        }
        if ((o & 71) == 71) {
            list.add("CCF1_UPDATE");
            flipped |= 71;
        }
        if ((o & 72) == 72) {
            list.add("CCF2_UPDATE");
            flipped |= 72;
        }
        if ((o & 73) == 73) {
            list.add("FW_UPDATE");
            flipped |= 73;
        }
        if ((o & 74) == 74) {
            list.add("HUE_SAT_ADJ");
            flipped |= 74;
        }
        if ((o & 75) == 75) {
            list.add("CSC_MATRIX");
            flipped |= 75;
        }
        if ((o & 76) == 76) {
            list.add("SCALER_PP_FILTER_LEVEL");
            flipped |= 76;
        }
        if ((o & 80) == 80) {
            list.add("CONTRAST_DIMMING");
            flipped |= 80;
        }
        if ((o & 81) == 81) {
            list.add("S_CURVE");
            flipped |= 81;
        }
        if ((o & 82) == 82) {
            list.add("BRIGHTNESS_CHIP");
            flipped |= 82;
        }
        if ((o & 90) == 90) {
            list.add("HDR_PREPARE");
            flipped |= 90;
        }
        if ((o & 91) == 91) {
            list.add("HDR_COMPLETE");
            flipped |= 91;
        }
        if ((o & 92) == 92) {
            list.add("MCF_DATA");
            flipped |= 92;
        }
        if ((o & 93) == 93) {
            list.add("Y5P");
            flipped |= 93;
        }
        if ((o & 99) == 99) {
            list.add("PANEL_NITS");
            flipped |= 99;
        }
        if ((o & 100) == 100) {
            list.add("SEND_FRAME");
            flipped |= 100;
        }
        if ((o & 101) == 101) {
            list.add("OSD_PATTERN_SHOW");
            flipped |= 101;
        }
        if ((o & 102) == 102) {
            list.add("DBG_TARGET_PI_REGADDR_SET");
            flipped |= 102;
        }
        if ((o & DBG_TARGET_REGADDR_VALUE_GET) == 103) {
            list.add("DBG_TARGET_REGADDR_VALUE_GET");
            flipped |= DBG_TARGET_REGADDR_VALUE_GET;
        }
        if ((o & 104) == 104) {
            list.add("MEMC_ENABLE_FOR_ASUS_CAMERA");
            flipped |= 104;
        }
        if ((o & 105) == 105) {
            list.add("DBG_TARGET_REGADDR_VALUE_SET");
            flipped |= 105;
        }
        if ((o & 106) == 106) {
            list.add("DBG_KERNEL_LOG_LEVEL");
            flipped |= 106;
        }
        if ((o & DBG_SEND_PACKAGE) == 107) {
            list.add("DBG_SEND_PACKAGE");
            flipped |= DBG_SEND_PACKAGE;
        }
        if ((o & DBG_LOOP_BACK_MODE) == 108) {
            list.add("DBG_LOOP_BACK_MODE");
            flipped |= DBG_LOOP_BACK_MODE;
        }
        if ((o & DBG_LOOP_BACK_MODE_RES) == 109) {
            list.add("DBG_LOOP_BACK_MODE_RES");
            flipped |= DBG_LOOP_BACK_MODE_RES;
        }
        if ((o & APP_FILTER) == 111) {
            list.add("APP_FILTER");
            flipped |= APP_FILTER;
        }
        if ((o & DBG_TARGET_REGADDR_VALUE_SET2) == 112) {
            list.add("DBG_TARGET_REGADDR_VALUE_SET2");
            flipped |= DBG_TARGET_REGADDR_VALUE_SET2;
        }
        if ((o & DEBUG_CAP) == 113) {
            list.add("DEBUG_CAP");
            flipped |= DEBUG_CAP;
        }
        if ((o & MODE_SET) == 120) {
            list.add("MODE_SET");
            flipped |= MODE_SET;
        }
        if ((o & VIDEO_FRAME_RATE_SET) == 121) {
            list.add("VIDEO_FRAME_RATE_SET");
            flipped |= VIDEO_FRAME_RATE_SET;
        }
        if ((o & OUT_FRAME_RATE_SET) == 122) {
            list.add("OUT_FRAME_RATE_SET");
            flipped |= OUT_FRAME_RATE_SET;
        }
        if ((o & OSD_ENABLE) == 123) {
            list.add("OSD_ENABLE");
            flipped |= OSD_ENABLE;
        }
        if ((o & OSD_AUTOREFRESH) == 124) {
            list.add("OSD_AUTOREFRESH");
            flipped |= OSD_AUTOREFRESH;
        }
        if ((o & OSD_OVERFLOW_ST) == 125) {
            list.add("OSD_OVERFLOW_ST");
            flipped |= OSD_OVERFLOW_ST;
        }
        if ((o & WORK_MODE) == 126) {
            list.add("WORK_MODE");
            flipped |= WORK_MODE;
        }
        if ((o & FRC_LOW_LATENCY) == 127) {
            list.add("FRC_LOW_LATENCY");
            flipped |= FRC_LOW_LATENCY;
        }
        if ((o & 128) == 128) {
            list.add("PANEL_TE");
            flipped |= 128;
        }
        if ((o & AP_TE) == 129) {
            list.add("AP_TE");
            flipped |= AP_TE;
        }
        if ((o & N2M_ENABLE) == 130) {
            list.add("N2M_ENABLE");
            flipped |= N2M_ENABLE;
        }
        if ((o & WAIT_VSYNC) == 132) {
            list.add("WAIT_VSYNC");
            flipped |= WAIT_VSYNC;
        }
        if ((o & MIPI2RX_PWRST) == 133) {
            list.add("MIPI2RX_PWRST");
            flipped |= MIPI2RX_PWRST;
        }
        if ((o & DUAL2SINGLE_ST) == 134) {
            list.add("DUAL2SINGLE_ST");
            flipped |= DUAL2SINGLE_ST;
        }
        if ((o & MEMC_OSD) == 135) {
            list.add("MEMC_OSD");
            flipped |= MEMC_OSD;
        }
        if ((o & MEMC_OSD_PROTECT) == 136) {
            list.add("MEMC_OSD_PROTECT");
            flipped |= MEMC_OSD_PROTECT;
        }
        if ((o & LCE_DEMO_WINDOW) == 137) {
            list.add("LCE_DEMO_WINDOW");
            flipped |= LCE_DEMO_WINDOW;
        }
        if ((o & TYPE_MAX) == 138) {
            list.add("TYPE_MAX");
            flipped |= TYPE_MAX;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}