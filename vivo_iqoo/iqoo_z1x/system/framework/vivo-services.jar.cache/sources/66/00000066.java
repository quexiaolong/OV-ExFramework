package com.android.server;

import android.media.AudioManager;
import android.os.SystemProperties;
import com.android.server.display.color.VivoColorManagerService;
import com.android.server.wm.VivoWmsImpl;
import com.vivo.services.rms.ProcessList;
import java.util.HashMap;
import java.util.Map;
import vendor.pixelworks.hardware.display.V1_0.Vendor2Config;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoVibratorSwr {
    private static final int HIGH_VIBRATOR_INTENSITY = 255;
    private static final int LOW_VIBRATOR_INTENSITY = 1;
    private static final int MID_VIBRATOR_INTENSITY = 145;
    private static final String TAG = "VibratorService_Swr";
    public static final String VIBRATION_INTENSITY_HIGH = "180";
    public static final String VIBRATION_INTENSITY_LOW = "100";
    public static final String VIBRATION_INTENSITY_MEDIUM = "140";
    boolean device1SwrStatus = getDeviceIdStatus(1);
    boolean device2SwrStatus = getDeviceIdStatus(2);
    private static final Map<String, Integer> SWRSTATUS_MAP = new HashMap<String, Integer>() { // from class: com.android.server.VivoVibratorSwr.1
        {
            put("PD2049", 2);
            put("PD2049F_EX", 2);
            put("PD2049F_EX_PDBD", 2);
            put("PD2056", 1);
            put("PD2056F_EX", 1);
            put("PD2056F_EX_PDBD", 1);
            put("TD2004", 1);
            put("PD2024", 0);
            put("PD2025", 0);
            put("PD1955", 0);
            put("PD1950", 0);
            put("PD1924", 0);
            put("PD1916", 0);
        }
    };
    private static final Map<Integer, String> PATH_MAP = new HashMap<Integer, String>() { // from class: com.android.server.VivoVibratorSwr.2
        {
            put(Integer.valueOf((int) VivoWmsImpl.NOTIFY_SPLIT_BAR_LAYOUT), "R_Array_Mbira.wav");
            put(302, "R_Blue_Meteor_Showers.wav");
            put(303, "R_Elec_Synth.wav");
            put(304, "R_Harp_Bell.wav");
            put(305, "R_High_Didi.wav");
            put(306, "R_Labyrinth.wav");
            put(307, "R_Limpid_Drop.wav");
            put(308, "R_Lovely_Xylophone.wav");
            put(309, "R_Marimba.wav");
            put(310, "R_Occarina.wav");
            put(311, "R_Resound.wav");
            put(312, "R_Rhythm.wav");
            put(313, "R_Ripples.wav");
            put(314, "R_Scene_Play.wav");
            put(315, "R_Set_Out.wav");
            put(316, "R_Spacious.wav");
            put(317, "R_Spring_Charm.wav");
            put(318, "R_Sunrise_View.wav");
            put(319, "R_Sunrise_View_Dubstep.wav");
            put(320, "R_Sunrise_View_Harp.wav");
            put(321, "R_Sunrise_View_Lyric.wav");
            put(322, "R_Sunrise_View_Piano.wav");
            put(323, "R_Sunrise_View_Relax.wav");
            put(324, "R_Tunk.wav");
            put(325, "R_Vintage_Ring.wav");
            put(326, "R_Xtreme_Tone.wav");
            put(327, "R_Xyl_Roll.wav");
            put(328, "R_Fantasy_Blue.wav");
            put(329, "R_Indomitable_Will.wav");
            put(330, "R_Jovi_Lifestyle.wav");
            put(331, "R_Jovi_Lifestyle_Full.wav");
            put(332, "R_Newborn.wav");
            put(401, "N_Andes.wav");
            put(402, "N_Bell.wav");
            put(403, "N_Bird.wav");
            put(404, "N_Bubble.wav");
            put(405, "N_Childlike.wav");
            put(406, "N_Circle.wav");
            put(407, "N_Cuckoo.wav");
            put(408, "N_Default.wav");
            put(409, "N_Dita.wav");
            put(410, "N_Dobe.wav");
            put(411, "N_Doda.wav");
            put(412, "N_DoReMi.wav");
            put(413, "N_Dust.wav");
            put(414, "N_Echo.wav");
            put(415, "N_Grain.wav");
            put(416, "N_Harmonic.wav");
            put(417, "N_Klock.wav");
            put(418, "N_Little.wav");
            put(419, "N_Money.wav");
            put(420, "N_MusicBox.wav");
            put(421, "N_Naughty.wav");
            put(422, "N_Peristalsis.wav");
            put(423, "N_Promote.wav");
            put(424, "N_Scale.wav");
            put(425, "N_Simple.wav");
            put(426, "N_Surprised.wav");
            put(427, "N_Theme.wav");
            put(428, "N_Twist.wav");
            put(429, "N_Unobtrusive.wav");
            put(Integer.valueOf((int) ProcessList.VERY_LASTEST_PREVIOUS_APP_ADJ), "N_Whisper.wav");
            put(431, "N_Whistle.wav");
            put(432, "N_Arrival.wav");
            put(433, "N_Emergence.wav");
            put(434, "N_Transformation.wav");
            put(435, "N_Happy.wav");
            put(436, "N_Arrangement.wav");
            put(437, "N_Encounter.wav");
            put(501, "A_Beautiful_Touching.wav");
            put(502, "A_Clock_Alert.wav");
            put(503, "A_Crisp_Ring.wav");
            put(504, "A_Cycle_Oscillation.wav");
            put(505, "A_Early_In_The_Morning.wav");
            put(506, "A_Fine_Day.wav");
            put(507, "A_Flush_Of_Dawn.wav");
            put(Integer.valueOf((int) VivoColorManagerService.VIVO_COLOR_MODE_AOD), "A_Get_Up_Action.wav");
            put(Integer.valueOf((int) VivoColorManagerService.VIVO_COLOR_MODE_FINGERPRINT), "A_Glassy_Lustre.wav");
            put(510, "A_Lights.wav");
            put(511, "A_Moonlight.wav");
            put(512, "A_Morning_Scene.wav");
            put(513, "A_Sound_Of_The_Sea.wav");
            put(Integer.valueOf((int) Vendor2Config.DISPLAY_BRIGHTNESS), "A_Thump.wav");
            put(Integer.valueOf((int) Vendor2Config.CM_COLOR_TEMP_MODE), "A_New_World.wav");
        }
    };

    private static long getSwrWavFileSizeBytes(String file_path) {
        long fileSize = VivoVibratorServiceImpl.vibratorProGetWavEffectSizeBytes(file_path);
        if (fileSize < 0) {
            VSlog.e(TAG, "Fail to geit swr file size, " + fileSize);
            return -1L;
        }
        return fileSize;
    }

    public boolean isSupportSwr(int effectID, int deviceID) {
        if (PATH_MAP.get(Integer.valueOf(effectID)) != null && getDeviceIdStatus(deviceID)) {
            return true;
        }
        return false;
    }

    private String transEffectIDtoString(int effectID) {
        String ret = PATH_MAP.get(Integer.valueOf(effectID));
        if (ret != null) {
            return ret;
        }
        return "error";
    }

    private String transVibrateStrengthtoVoltage(int vibrateStrength) {
        if (vibrateStrength != 1) {
            if (vibrateStrength != MID_VIBRATOR_INTENSITY) {
                if (vibrateStrength == 255) {
                    return VIBRATION_INTENSITY_HIGH;
                }
                return VIBRATION_INTENSITY_MEDIUM;
            }
            return VIBRATION_INTENSITY_MEDIUM;
        }
        return VIBRATION_INTENSITY_LOW;
    }

    public long setSwrParameters(int effectID) {
        AudioManager aM = new AudioManager();
        String filename = PATH_MAP.get(Integer.valueOf(effectID));
        if (filename == null) {
            return 0L;
        }
        String path1 = "vendor/firmware/" + filename;
        long fileLength = getSwrWavFileSizeBytes(path1);
        if (fileLength > 0) {
            long fileLength2 = (fileLength - 44) / 96;
            String path = "VivoVibrate=" + filename;
            aM.setParameters(path);
            return fileLength2;
        }
        VSlog.d(TAG, "file doesn't exist or is not a file");
        return 0L;
    }

    public long setSwrParameters(int effectID, int vibrateStrength) {
        AudioManager aM = new AudioManager();
        String vlotage = "haptics_intensity=" + transVibrateStrengthtoVoltage(vibrateStrength);
        VSlog.d(TAG, "String is:" + vlotage);
        aM.setParameters(vlotage);
        String filename = PATH_MAP.get(Integer.valueOf(effectID));
        if (filename == null) {
            return 0L;
        }
        String path1 = "vendor/firmware/" + filename;
        long fileLength = getSwrWavFileSizeBytes(path1);
        if (fileLength > 0) {
            long fileLength2 = (fileLength - 44) / 96;
            String path = "VivoVibrate=" + filename;
            VSlog.d(TAG, "String is:" + path + ";  File length is:" + fileLength2);
            aM.setParameters(path);
            return fileLength2;
        }
        VSlog.d(TAG, "file doesn't exist or is not a file");
        return 0L;
    }

    public void setSwrParameters(String parameters) {
        AudioManager aM = new AudioManager();
        String path = "VivoVibrate=" + parameters;
        aM.setParameters(path);
    }

    private boolean getDeviceIdStatus(int deviceID) {
        Integer SwrStatus;
        String projectName = SystemProperties.get("ro.product.model.bbk");
        if (projectName == null || (SwrStatus = SWRSTATUS_MAP.get(projectName)) == null) {
            return false;
        }
        int intValue = SwrStatus.intValue();
        if (intValue == 1) {
            if (deviceID == 1) {
                return true;
            }
        } else if (intValue == 2 && deviceID == 2) {
            return true;
        }
        return false;
    }
}