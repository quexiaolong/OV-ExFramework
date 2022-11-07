package com.vivo.services.vivo4dgamevibrator;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import android.view.WindowManager;
import com.vivo.framework.vivo4dgamevibrator.IVivo4DGameVibratorService;
import com.vivo.services.superresolution.Constant;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class Vivo4DGameVibratorService extends IVivo4DGameVibratorService.Stub {
    private static final int AR_556_VIB_RATE = 80;
    private static final int AR_762_VIB_RATE = 80;
    private static final String[] CAR_GAMES_FOR_AWINIC;
    private static final boolean DEBUG = false;
    private static final int DEVICE_NOT_SUPPORT_GAME_EFFECT = 0;
    private static final int DEVICE_SUPPORT_GAME_EFFECT_DRIVER = 1;
    private static final int DEVICE_SUPPORT_GAME_EFFECT_NO_DRIVER = 2;
    private static final int DMR_VIB_RATE = 500;
    public static final String[] EUROPEAN_REGION;
    private static final String EXCLUDE_AWI_SUPPORT_Gloft_PKG = "com.gameloft.android.ANMP.GloftA9HM";
    private static final String EXCLUDE_AWI_SUPPORT_KYBC_PKG = "com.aligames.kuang.kybc.vivo";
    private static final String EXCLUDE_AWI_SUPPORT_QQ_SPEED_PKG = "com.tencent.tmgp.speedmobile";
    private static final String EXCLUDE_AWI_SUPPORT_SGAME_PKG = "com.tencent.tmgp.sgame";
    private static final String EXCLUDE_PKG = "com.tencent.tmgp.cf";
    private static final String EXCLUDE_ROTOR_SUPPORT_CALLOFDUTY_PKG = "com.activision.callofduty.shooter";
    private static final String EXCLUDE_ROTOR_SUPPORT_IGLITE_PKG = "com.tencent.iglite";
    private static final String EXCLUDE_ROTOR_SUPPORT_ZJZ_PKG = "com.netease.zjz.vivo";
    private static final Map<Integer, int[]> GAME_EFFECT_MAP;
    private static final int GAME_VIB_MOD_AUDIO_AND_INTERFACE = 3;
    private static final int GAME_VIB_MOD_AUDIO_AND_INTERFACE_TYPE_ELEVEN = 11;
    private static final int GAME_VIB_MOD_AUDIO_AND_INTERFACE_TYPE_FIVE = 5;
    private static final int GAME_VIB_MOD_AUDIO_AND_INTERFACE_TYPE_FOUR = 4;
    private static final int GAME_VIB_MOD_AUDIO_ONLY = 1;
    private static final int GAME_VIB_MOD_INTERFACE_ONLY = 2;
    private static final int GAME_VIB_MOD_NOT_SUPPORT = 0;
    private static final Map<String, int[]> GUN_VIB_TIME;
    private static final String HURT_THREAD_NAME = "car_vibrator_thread";
    private static final String[] INCLUDE_PROJECT;
    private static final long MAX_VIB_TIME = 1000;
    private static final int MSG_CAR_KEY = 2;
    private static final int MSG_GUN_KEY = 3;
    private static final int PISTOL_VIB_RATE = 500;
    private static final String[] POWER_OPTIMIZATION_GAME;
    private static final String[] POWER_OPTIMIZATION_PROJECT;
    private static final String PROP_DEBUG_EFFECT_ID = "persist.vivo.gamevibeffect.debug.effectID";
    private static final String PROP_DEVICE_SUPPORT_GAME_EFFECT = "persist.vivo.support.lra";
    private static final String PROP_GAME_VIB_ENABLE_MOD = "persist.vivo.vivo4dgamevib.enable";
    private static final Map<String, int[]> ROTOR_GUN_VIB_TIME;
    private static final int SHOT_GUN_VIB_RATE = 500;
    private static final int SILENCER_ENERGY_THRESHOLD = 15000;
    private static final int SMG_VIB_RATE = 50;
    private static final int SNIPER_GUN_VIB_RATE = 500;
    public static final String[] SUPPORT_SKILL_VIBRATOR_GAMES;
    public static final String[] SUPPORT_SKILL_VIBRATOR_PROJECT;
    private static final String TAG = "gamevibrator";
    public static final int TYPE_4D_GAME_VIB_AUDIO = 1;
    public static final int TYPE_4D_GAME_VIB_AUDIO_NO_HAND = 4;
    public static final int TYPE_4D_GAME_VIB_INTERFACE = 2;
    public static final int TYPE_4D_GAME_VIB_INTERFACE_HAND = 3;
    public static final int TYPE_NOT_SUPPORT = 0;
    private static boolean carOn;
    private static int mGameEvent;
    private static int mMode;
    private static boolean sFunOn;
    private static int sGunType;
    private static int sSupportGameEffectType;
    private HurtHandler hurtHandler;
    private HandlerThread hurtThread;
    private AudioManager mAudioManager;
    private Context mContext;
    private Vibrator mVibrator;
    private Vivo4DGameVibratorMonitor mVivo4DGameVibratorMonitor;
    private String pkgName;
    private static final String[] SUPPORT_BOTH_INTERFACE_AUDIO_4D_GAME_VIB_PKG = new String[0];
    private static final HashMap<String, Integer> SUPPORT_4D_GAME_VIB_PKG_TYPE_MAP = new HashMap<>();
    private static final HashMap<String, String> SUPPORT_4D_GAME_VIB_PKG_NAME_MAP = new HashMap<>();
    private static final HashMap<String, String> SUPPORT_4D_GAME_VIB_PKG_NAME_MAP_F_EX = new HashMap<>();
    Map<Integer, int[]> GUN_EFFECTID_WITH_ENERGY_THRESHOLD = new HashMap<Integer, int[]>() { // from class: com.vivo.services.vivo4dgamevibrator.Vivo4DGameVibratorService.2
        {
            put(2, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9900, 9901, 606, 605, 500});
            put(6, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9900, 9901, 606, 605, 500});
            put(9, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9900, 9901, 606, 605, 500});
            put(37, new int[]{0, 9900, 9901, 606, 605, 500});
            put(1, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9902, 9908, 612, 611, 80});
            put(4, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9902, 9908, 612, 611, 80});
            put(7, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9902, 9908, 612, 611, 80});
            put(8, new int[]{0, 9902, 9908, 612, 611, 80});
            put(10, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9902, 9908, 612, 611, 80});
            put(22, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9902, 9908, 612, 611, 80});
            put(30, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9902, 9908, 612, 611, 80});
            put(0, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9903, 9908, 614, 613, 80});
            put(3, new int[]{0, 9903, 9908, 614, 613, 80});
            put(5, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9903, 9908, 614, 613, 80});
            put(11, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9903, 9908, 614, 613, 80});
            put(14, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9903, 9908, 614, 613, 80});
            put(12, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9904, 9908, 608, 607, 500});
            put(13, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9904, 9908, 608, 607, 500});
            put(21, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9904, 9908, 608, 607, 500});
            put(31, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9904, 9908, 608, 607, 500});
            put(32, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9904, 9908, 608, 607, 500});
            put(36, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9904, 9908, 608, 607, 500});
            put(15, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9905, 9909, 602, 601, 500});
            put(16, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9905, 9909, 602, 601, 500});
            put(17, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9905, 9909, 602, 601, 500});
            put(18, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9905, 9909, 602, 601, 500});
            put(19, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9905, 9909, 602, 601, 500});
            put(20, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9905, 9909, 602, 601, 500});
            put(23, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9905, 9909, 602, 601, 500});
            put(24, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9905, 9909, 602, 601, 500});
            put(25, new int[]{0, 9905, 9909, 602, 601, 500});
            put(26, new int[]{0, 9905, 9909, 602, 601, 500});
            put(42, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9905, 9909, 602, 601, 500});
            put(43, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9905, 9909, 602, 601, 500});
            put(45, new int[]{0, 9905, 9909, 602, 601, 500});
            put(46, new int[]{0, 9905, 9909, 602, 601, 500});
            put(33, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9906, 9909, Constant.REPORT_SPACE, 603, 50});
            put(35, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9906, 9909, Constant.REPORT_SPACE, 603, 50});
            put(34, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9906, 9909, Constant.REPORT_SPACE, 603, 50});
            put(39, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9906, 9909, Constant.REPORT_SPACE, 603, 50});
            put(44, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9906, 9909, Constant.REPORT_SPACE, 603, 50});
            put(47, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9906, 9909, Constant.REPORT_SPACE, 603, 50});
            put(27, new int[]{Vivo4DGameVibratorService.SILENCER_ENERGY_THRESHOLD, 9907, 9909, 610, 609, 500});
            put(29, new int[]{0, 9907, 9909, 610, 609, 500});
            put(28, new int[]{0, 9907, 9909, 610, 609, 500});
            put(40, new int[]{0, 9907, 9909, 610, 609, 500});
            put(41, new int[]{0, 9907, 9909, 610, 609, 500});
        }
    };
    Map<Integer, Integer> ROTOR_MOTOR_CLASSIFICATION_BY_GUN = new HashMap<Integer, Integer>() { // from class: com.vivo.services.vivo4dgamevibrator.Vivo4DGameVibratorService.4
        {
            put(2, 0);
            put(6, 0);
            put(27, 1);
            put(28, 1);
            put(29, 1);
            put(40, 1);
            put(41, 1);
            put(48, 1);
            put(37, 1);
            put(9, 1);
            put(7, 1);
            put(14, 1);
            put(13, 2);
            put(21, 2);
            put(31, 2);
            put(32, 2);
            put(12, 2);
            put(0, 3);
            put(1, 3);
            put(3, 3);
            put(4, 3);
            put(10, 3);
            put(33, 3);
            put(8, 3);
            put(30, 4);
            put(35, 4);
            put(39, 4);
            put(47, 4);
            put(36, 5);
            put(11, 5);
            put(5, 5);
            put(22, 5);
            put(42, 5);
            put(43, 5);
            put(44, 5);
            put(34, 5);
            put(15, 6);
            put(16, 6);
            put(17, 6);
            put(18, 6);
            put(19, 6);
            put(20, 6);
            put(23, 6);
            put(24, 6);
            put(25, 6);
            put(26, 6);
            put(45, 6);
            put(46, 6);
        }
    };
    private long mVibTime = 0;
    private long mVibGap = 0;
    private long mStartFireTime = 0;
    private long mEndFireTime = 0;
    private boolean mShieldVib = false;
    private int mVibCount = 0;
    private int modeSum = 0;
    private long motorOneTimeCache = 0;
    private long motorTwoTimeCache = 0;
    private long motorTimeCarCache = 0;

    static {
        SUPPORT_4D_GAME_VIB_PKG_TYPE_MAP.put(EXCLUDE_AWI_SUPPORT_SGAME_PKG, 3);
        SUPPORT_4D_GAME_VIB_PKG_TYPE_MAP.put(EXCLUDE_ROTOR_SUPPORT_ZJZ_PKG, 1);
        SUPPORT_4D_GAME_VIB_PKG_TYPE_MAP.put("com.tencent.tmgp.sgamece", 3);
        SUPPORT_4D_GAME_VIB_PKG_TYPE_MAP.put(EXCLUDE_PKG, 2);
        SUPPORT_4D_GAME_VIB_PKG_TYPE_MAP.put("com.tencent.tmgp.pubgmhd", 1);
        SUPPORT_4D_GAME_VIB_PKG_TYPE_MAP.put("com.netease.hyxd.vivo", 1);
        SUPPORT_4D_GAME_VIB_PKG_TYPE_MAP.put("com.tencent.ig", 1);
        SUPPORT_4D_GAME_VIB_PKG_TYPE_MAP.put(EXCLUDE_ROTOR_SUPPORT_IGLITE_PKG, 1);
        SUPPORT_4D_GAME_VIB_PKG_TYPE_MAP.put(EXCLUDE_ROTOR_SUPPORT_CALLOFDUTY_PKG, 4);
        SUPPORT_4D_GAME_VIB_PKG_TYPE_MAP.put("com.garena.game.codm", 4);
        SUPPORT_4D_GAME_VIB_PKG_TYPE_MAP.put(EXCLUDE_AWI_SUPPORT_QQ_SPEED_PKG, 4);
        SUPPORT_4D_GAME_VIB_PKG_TYPE_MAP.put(EXCLUDE_AWI_SUPPORT_KYBC_PKG, 4);
        SUPPORT_4D_GAME_VIB_PKG_TYPE_MAP.put(EXCLUDE_AWI_SUPPORT_Gloft_PKG, 4);
        SUPPORT_4D_GAME_VIB_PKG_NAME_MAP.put(EXCLUDE_AWI_SUPPORT_SGAME_PKG, "王者荣耀");
        SUPPORT_4D_GAME_VIB_PKG_NAME_MAP.put(EXCLUDE_ROTOR_SUPPORT_ZJZ_PKG, "终结战场");
        SUPPORT_4D_GAME_VIB_PKG_NAME_MAP.put(EXCLUDE_PKG, "穿越火线-枪战王者");
        SUPPORT_4D_GAME_VIB_PKG_NAME_MAP.put("com.tencent.tmgp.pubgmhd", "和平精英");
        SUPPORT_4D_GAME_VIB_PKG_NAME_MAP.put("com.netease.hyxd.vivo", "荒野行动");
        SUPPORT_4D_GAME_VIB_PKG_NAME_MAP.put(EXCLUDE_AWI_SUPPORT_QQ_SPEED_PKG, "QQ飞车");
        SUPPORT_4D_GAME_VIB_PKG_NAME_MAP.put(EXCLUDE_AWI_SUPPORT_KYBC_PKG, "狂野飙车9：竞速传奇");
        SUPPORT_4D_GAME_VIB_PKG_NAME_MAP_F_EX.put("com.tencent.ig", "PUBG Mobile");
        SUPPORT_4D_GAME_VIB_PKG_NAME_MAP_F_EX.put(EXCLUDE_ROTOR_SUPPORT_IGLITE_PKG, "PUBG Mobile Lite");
        SUPPORT_4D_GAME_VIB_PKG_NAME_MAP_F_EX.put(EXCLUDE_ROTOR_SUPPORT_CALLOFDUTY_PKG, "Call of Duty:Mobile");
        SUPPORT_4D_GAME_VIB_PKG_NAME_MAP_F_EX.put("com.garena.game.codm", "Call Of Duty");
        SUPPORT_4D_GAME_VIB_PKG_NAME_MAP_F_EX.put(EXCLUDE_AWI_SUPPORT_Gloft_PKG, "GloftA9");
        INCLUDE_PROJECT = new String[]{"PD1824", "PD1824B", "PD1824BA", "PD1824F_EX"};
        CAR_GAMES_FOR_AWINIC = new String[]{"PD1955F_EX", "PD1955", "PD1964F_EX"};
        POWER_OPTIMIZATION_PROJECT = new String[]{"PD1955F_EX", "PD1955"};
        POWER_OPTIMIZATION_GAME = new String[]{EXCLUDE_ROTOR_SUPPORT_CALLOFDUTY_PKG, EXCLUDE_ROTOR_SUPPORT_IGLITE_PKG};
        SUPPORT_SKILL_VIBRATOR_GAMES = new String[]{EXCLUDE_AWI_SUPPORT_SGAME_PKG};
        SUPPORT_SKILL_VIBRATOR_PROJECT = new String[]{"PD2024", "PD2025", "PD2049", "PD2049F_EX", "PD2055", "PD2056", "PD2056F_EX"};
        EUROPEAN_REGION = new String[]{"FR", "UK", "NL", "IT", "DE", "EEA"};
        sSupportGameEffectType = 0;
        GAME_EFFECT_MAP = new HashMap<Integer, int[]>() { // from class: com.vivo.services.vivo4dgamevibrator.Vivo4DGameVibratorService.1
            {
                put(1, new int[]{10011, Constant.REPORT_SPACE, 620});
                put(2, new int[]{10016, 602, 621});
                put(3, new int[]{10002, 608, 622});
                put(4, new int[]{10003, 610, 623});
                put(5, new int[]{10036, 606, 624});
            }
        };
        GUN_VIB_TIME = new HashMap<String, int[]>() { // from class: com.vivo.services.vivo4dgamevibrator.Vivo4DGameVibratorService.3
            {
                put("null", new int[]{45, 45, 45, 55, 55});
                put("PD1969F_EX", new int[]{50, 50, 50, 80, 80});
                put("PD1932", new int[]{55, 60, 65, 75, 95});
                put("PD1962", new int[]{40, 40, 40, 50, 50});
                put("PD1936", new int[]{40, 40, 40, 50, 50});
                put("PD1965", new int[]{50, 50, 50, 70, 70});
                put("PD1948F_EX", new int[]{50, 50, 50, 70, 70});
                put("PD1911", new int[]{40, 40, 40, 50, 50});
                put("PD1914", new int[]{40, 40, 40, 45, 45});
                put("PD1921", new int[]{55, 60, 65, 70, 75});
                put("PD1941", new int[]{45, 45, 45, 60, 60});
                put("PD1963", new int[]{45, 45, 45, 50, 50});
                put("PD1921F_EX", new int[]{55, 60, 65, 70, 75});
                put("PD1931F_EX", new int[]{45, 45, 45, 55, 55});
                put("PD2020", new int[]{45, 45, 45, 60, 60});
            }
        };
        ROTOR_GUN_VIB_TIME = new HashMap<String, int[]>() { // from class: com.vivo.services.vivo4dgamevibrator.Vivo4DGameVibratorService.5
            {
                put("PD1981", new int[]{85, 75, 75, 50, 50, 50, 45});
            }
        };
        sFunOn = false;
        mGameEvent = 0;
        carOn = false;
        sGunType = -1;
        mMode = 0;
    }

    private void startVib(int energy) {
        int[] timeArrays;
        if (sSupportGameEffectType != 0) {
            Integer effectID = matchEffect(energy);
            VSlog.d("gamevibrator", "get game effect id success, gunType: " + sGunType + " ,effect id: " + effectID);
            if (this.hurtThread == null) {
                HandlerThread handlerThread = new HandlerThread(HURT_THREAD_NAME);
                this.hurtThread = handlerThread;
                handlerThread.start();
                this.hurtHandler = new HurtHandler(this.hurtThread.getLooper());
            }
            Message message = this.hurtHandler.obtainMessage();
            message.what = 3;
            message.arg1 = effectID.intValue();
            this.hurtHandler.sendMessage(message);
            return;
        }
        String model = SystemProperties.get("ro.vivo.product.model", "null");
        VSlog.d("gamevibrator", "model:" + model);
        if (GUN_VIB_TIME.containsKey(model)) {
            timeArrays = GUN_VIB_TIME.get(model);
        } else {
            timeArrays = GUN_VIB_TIME.get("null");
        }
        int i = sGunType;
        if (i > -1 && this.ROTOR_MOTOR_CLASSIFICATION_BY_GUN.containsKey(Integer.valueOf(i)) && ROTOR_GUN_VIB_TIME.containsKey(model)) {
            int gunTypeNum = this.ROTOR_MOTOR_CLASSIFICATION_BY_GUN.get(Integer.valueOf(sGunType)).intValue();
            int[] rotorVibTimes = ROTOR_GUN_VIB_TIME.get(model);
            long rotorVibTime = rotorVibTimes[gunTypeNum];
            Slog.d("gamevibrator", "rotorVibTime:" + rotorVibTime);
            normalEffect(rotorVibTime);
            return;
        }
        int gunMod = convertToMod(energy);
        if (gunMod > 0 && gunMod <= 5) {
            long vibTime = timeArrays[gunMod - 1];
            Slog.d("gamevibrator", "vibTime:" + vibTime);
            normalEffect(vibTime);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void gameEffect(Integer effectID) {
        if (effectID != null) {
            Class clazz = this.mVibrator.getClass();
            try {
                Method method = clazz.getDeclaredMethod("gameVibrate", Integer.TYPE);
                if (method == null) {
                    VSlog.e("gamevibrator", "get game vibrate method failed.");
                } else {
                    long playMillis = ((Long) method.invoke(this.mVibrator, effectID)).longValue();
                    VSlog.i("gamevibrator", "effect will play millis: " + playMillis);
                }
                return;
            } catch (Exception e) {
                VSlog.e("gamevibrator", "call game vibrator exception", e);
                return;
            }
        }
        VSlog.d("gamevibrator", "get game effect id failed, gunType: " + sGunType);
    }

    private void startVibDual(float channel, int direction) {
        int angle = ((WindowManager) this.mContext.getSystemService("window")).getDefaultDisplay().getRotation();
        VSlog.d("gamevibrator", "hurt startVibDual channel: " + angle + " direction:" + direction);
        if (direction == 3) {
            gameEffectDual(626, 0);
        } else if (angle == 1) {
            VSlog.d("gamevibrator", "left right");
            if (direction == 1) {
                gameEffectDual(625, 1);
            } else if (direction == 2) {
                gameEffectDual(625, 2);
            }
        } else if (angle == 3) {
            VSlog.d("gamevibrator", "right left");
            if (direction == 1) {
                gameEffectDual(625, 1);
            } else if (direction == 2) {
                gameEffectDual(625, 2);
            }
        }
    }

    private void gameEffectDual(Integer effectID, int vibratorID) {
        VSlog.d("gamevibrator", "gameEffectDual: " + effectID + " vibratorID:" + vibratorID);
        long nowTime = SystemClock.elapsedRealtime();
        if (vibratorID == 1) {
            if (nowTime != 0 && nowTime - this.motorOneTimeCache < 110) {
                VSlog.e("gamevibrator", "one time:" + (nowTime - this.motorOneTimeCache));
                return;
            }
        } else if (nowTime != 0 && nowTime - this.motorTwoTimeCache < 90) {
            VSlog.e("gamevibrator", "two time:" + (nowTime - this.motorTwoTimeCache));
            return;
        }
        this.motorOneTimeCache = nowTime;
        this.motorTwoTimeCache = nowTime;
        if (effectID != null) {
            Class clazz = this.mVibrator.getClass();
            try {
                Method method = clazz.getDeclaredMethod("gameVibrate", Integer.TYPE, Integer.TYPE);
                if (method != null) {
                    long playMillis = ((Long) method.invoke(this.mVibrator, effectID, Integer.valueOf(vibratorID))).longValue();
                    VSlog.i("gamevibrator", "effect will play millis: " + playMillis);
                } else {
                    VSlog.e("gamevibrator", "get game vibrate dual method failed.");
                }
            } catch (Exception e) {
                VSlog.e("gamevibrator", "call game vibrator dual exception", e);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void gameEffectDualCar(Integer effectID, int vibratorID) {
        VSlog.d("gamevibrator", "gameEffectDualCar: " + effectID + " vibratorID:" + vibratorID);
        if (effectID != null) {
            Class clazz = this.mVibrator.getClass();
            try {
                Method method = clazz.getDeclaredMethod("gameVibrate", Integer.TYPE, Integer.TYPE);
                if (method != null) {
                    long playMillis = ((Long) method.invoke(this.mVibrator, effectID, Integer.valueOf(vibratorID))).longValue();
                    VSlog.i("gamevibrator", "effect will play millis: " + playMillis);
                } else {
                    VSlog.e("gamevibrator", "get game vibrate dual method failed.");
                }
            } catch (Exception e) {
                VSlog.e("gamevibrator", "call game vibrator dual exception", e);
            }
        }
    }

    private void normalEffect(long vibMillis) {
        VSlog.d("gamevibrator", "startVib time: " + SystemClock.elapsedRealtime());
        VibrationEffect effect = VibrationEffect.createOneShot(vibMillis, -1);
        this.mVibrator.vibrate(effect, (AudioAttributes) null);
    }

    public Vivo4DGameVibratorService(Context context) {
        this.mVibrator = null;
        this.mContext = context;
        int vibMod = SystemProperties.getInt(PROP_GAME_VIB_ENABLE_MOD, 0);
        VSlog.d("gamevibrator", "game vib enable mod: " + vibMod);
        if (vibMod != 0 && 2 != vibMod) {
            this.mVivo4DGameVibratorMonitor = new Vivo4DGameVibratorMonitor(context, this);
        } else {
            VSlog.d("gamevibrator", "device not support audio solution");
        }
        this.mVibrator = (Vibrator) context.getSystemService(Vibrator.class);
        sSupportGameEffectType = SystemProperties.getInt(PROP_DEVICE_SUPPORT_GAME_EFFECT, 0);
        VSlog.d("gamevibrator", "Vivo4DGameVibratorService constructor method called,className: " + Vivo4DGameVibratorService.class.getName() + " , is support game effect: " + sSupportGameEffectType);
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
    }

    public void vibrate(int mod, long callTimeMillis, long vibMillis) {
        VSlog.d("gamevibrator", "vibrate method called, mod: " + mod + ",callTimeMillis: " + callTimeMillis + ",call cost: " + (SystemClock.elapsedRealtime() - callTimeMillis) + "ms, , function is on: " + sFunOn + "Shield: " + this.mShieldVib);
        if (sFunOn) {
            startVib(mod);
        }
    }

    public void dualVibrate(float channel, int direction, long callTimeMillis, long vibMillis) {
        VSlog.d("gamevibrator", "dual vibrate method called, channel: " + channel + ",direction: " + direction + ",call cost: " + (SystemClock.elapsedRealtime() - callTimeMillis) + "ms");
        if (channel > 0.0f && direction > 0) {
            startVibDual(channel, direction);
        }
    }

    public void dualCarVibrate(int type, int channel, long callTimeMillis, long vibMillis) {
        int i = mGameEvent;
        if (i != 902 && i != 4 && i != 101) {
            if (i != 102) {
                if (this.hurtThread == null) {
                    HandlerThread handlerThread = new HandlerThread(HURT_THREAD_NAME);
                    this.hurtThread = handlerThread;
                    handlerThread.start();
                    this.hurtHandler = new HurtHandler(this.hurtThread.getLooper());
                }
                int mathChannel = Math.abs(channel);
                VSlog.d("gamevibrator", "dual car method called, type: " + type + ",channel: " + channel + ", mathChannel = " + mathChannel);
                int effectID = 631;
                if (mathChannel > 0 && mathChannel <= 1500) {
                    effectID = 631;
                } else if (mathChannel > 1500 && mathChannel <= 3000) {
                    effectID = 632;
                } else if (mathChannel > 3000 && mathChannel <= 5000) {
                    effectID = 633;
                } else if (mathChannel > 5000 && mathChannel <= 8000) {
                    effectID = 634;
                } else if (mathChannel > 8000 && mathChannel <= 12000) {
                    effectID = 635;
                } else if (mathChannel > 12000 && mathChannel <= 17000) {
                    effectID = 636;
                } else if (mathChannel > 17000 && mathChannel <= 32768) {
                    effectID = 637;
                }
                int index_large = channel - 32768;
                if (index_large > 0) {
                    if (index_large <= 1500) {
                        effectID = 631;
                    } else if (index_large <= 3000) {
                        effectID = 632;
                    } else if (index_large <= 5000) {
                        effectID = 633;
                    } else if (index_large <= 8000) {
                        effectID = 634;
                    } else if (index_large <= 12000) {
                        effectID = 635;
                    } else if (index_large <= 17000) {
                        effectID = 636;
                    } else if (index_large <= 32768) {
                        effectID = 637;
                    }
                }
                startVibCarDual(channel, effectID);
                return;
            }
        }
        VSlog.d("gamevibrator", "mGameEvent = " + mGameEvent + ", no vibrator return");
    }

    private void startVibCarDual(int channel, int effectId) {
        int angle = ((WindowManager) this.mContext.getSystemService("window")).getDefaultDisplay().getRotation();
        Message message = this.hurtHandler.obtainMessage();
        message.what = 2;
        message.arg1 = effectId;
        if (channel < 0) {
            if (angle == 1) {
                VSlog.d("gamevibrator", "right startVibDual: " + angle + " vibrator 2");
                message.arg2 = 2;
            } else if (angle == 3) {
                VSlog.d("gamevibrator", "right startVibDual: " + angle + " vibrator 1");
                message.arg2 = 1;
            }
        } else if (channel > 32768) {
            message.arg2 = 0;
        } else if (angle == 1) {
            VSlog.d("gamevibrator", "left startVibDual: " + angle + " vibrator 1");
            message.arg2 = 1;
        } else if (angle == 3) {
            VSlog.d("gamevibrator", "left startVibDual: " + angle + " vibrator 2");
            message.arg2 = 2;
        }
        this.hurtHandler.sendMessage(message);
    }

    public void funcOn(boolean on) {
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();
        VSlog.d("gamevibrator", "function on called, on: " + on + " , caller pid: " + pid + " , uid: " + uid);
        this.mVivo4DGameVibratorMonitor.setClickMonitorAppInfor(pid, uid);
        sFunOn = on;
    }

    public void funcOnWithGun(boolean on, int gunType) {
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();
        sGunType = gunType;
        if (on) {
            this.mShieldVib = false;
        } else {
            this.mVibCount = 0;
        }
        VSlog.d("gamevibrator", "function on with gun called, on: " + on + " , caller pid: " + pid + " , uid: " + uid + " , gunType:" + gunType);
        this.mVivo4DGameVibratorMonitor.setClickMonitorAppInfor(pid, uid);
        sFunOn = on;
    }

    public void gameEvent(String gamePkgs, int event) {
        VSlog.d("gamevibrator", "gameEvent, gamePkgs: " + gamePkgs + " , event: " + event);
        mGameEvent = event;
        if (event == 4) {
            Settings.System.putString(this.mContext.getContentResolver(), "injured_hidden_state", "com.tencent.tmgp.pubgmhd");
            Settings.System.putString(this.mContext.getContentResolver(), "transport_hidden_state", "com.tencent.tmgp.pubgmhd");
            return;
        }
        Settings.System.putString(this.mContext.getContentResolver(), "injured_hidden_state", null);
        Settings.System.putString(this.mContext.getContentResolver(), "transport_hidden_state", null);
    }

    public void funcOnWithCar(boolean on, int mode) {
        VSlog.d("gamevibrator", "function on with car, on: " + on + " , mode: " + mode + ", modeSum:" + this.modeSum);
        if (this.mAudioManager != null && (on != carOn || mMode != mode)) {
            VSlog.d("gamevibrator", "setParameters, on: " + on + " , mode: " + mode + ", modeSum:" + this.modeSum);
            if (on) {
                this.mAudioManager.setParameters("aw_haptic_mode=" + mode);
                this.modeSum = 0;
            } else {
                this.mAudioManager.setParameters("aw_haptic_mode=0");
                this.modeSum++;
            }
        }
        if (mode != 0) {
            mMode = mode;
        } else if (this.modeSum == 3) {
            this.modeSum = 0;
            mMode = mode;
        }
        carOn = on;
    }

    /* renamed from: get4DGameVibSupportPkgs */
    public HashMap<String, Integer> m5get4DGameVibSupportPkgs() {
        String[] strArr;
        String projectName = SystemProperties.get("ro.vivo.product.model");
        HashMap<String, Integer> result = new HashMap<>(SUPPORT_4D_GAME_VIB_PKG_TYPE_MAP.size() + SUPPORT_BOTH_INTERFACE_AUDIO_4D_GAME_VIB_PKG.length);
        String region = SystemProperties.get("ro.product.country.region");
        if (!TextUtils.isEmpty(region) && Arrays.asList(EUROPEAN_REGION).contains(region)) {
            return result;
        }
        int vibMod = SystemProperties.getInt(PROP_GAME_VIB_ENABLE_MOD, 0);
        if (vibMod == 0) {
            return result;
        }
        if (3 == vibMod || 4 == vibMod || 5 == vibMod) {
            result.putAll(SUPPORT_4D_GAME_VIB_PKG_TYPE_MAP);
            for (String tempPkg : SUPPORT_BOTH_INTERFACE_AUDIO_4D_GAME_VIB_PKG) {
                result.put(tempPkg, 2);
            }
            if (!Arrays.asList(INCLUDE_PROJECT).contains(projectName)) {
                result.remove(EXCLUDE_PKG);
            }
            if (projectName.equals("PD2024") || projectName.equals("PD2049")) {
                result.put(EXCLUDE_ROTOR_SUPPORT_ZJZ_PKG, 1);
            }
            if (4 == vibMod) {
                result.remove(EXCLUDE_AWI_SUPPORT_QQ_SPEED_PKG);
                result.remove(EXCLUDE_AWI_SUPPORT_KYBC_PKG);
                result.remove(EXCLUDE_AWI_SUPPORT_Gloft_PKG);
                result.remove(EXCLUDE_ROTOR_SUPPORT_IGLITE_PKG);
                result.remove(EXCLUDE_ROTOR_SUPPORT_CALLOFDUTY_PKG);
            }
            if (5 == vibMod) {
                result.remove(EXCLUDE_AWI_SUPPORT_QQ_SPEED_PKG);
                result.remove(EXCLUDE_AWI_SUPPORT_KYBC_PKG);
                result.remove(EXCLUDE_AWI_SUPPORT_Gloft_PKG);
            }
            return result;
        }
        int type = 0;
        for (Map.Entry<String, Integer> entry : SUPPORT_4D_GAME_VIB_PKG_TYPE_MAP.entrySet()) {
            String pkg = entry.getKey();
            type = entry.getValue().intValue();
            if ((1 == vibMod || 11 == vibMod) && 1 == type) {
                result.put(pkg, Integer.valueOf(type));
            } else if (2 == vibMod && 2 == type) {
                result.put(pkg, Integer.valueOf(type));
            }
        }
        result.remove(EXCLUDE_ROTOR_SUPPORT_ZJZ_PKG);
        if (!Arrays.asList(CAR_GAMES_FOR_AWINIC).contains(projectName)) {
            result.remove(EXCLUDE_AWI_SUPPORT_QQ_SPEED_PKG);
            result.remove(EXCLUDE_AWI_SUPPORT_KYBC_PKG);
            result.remove(EXCLUDE_AWI_SUPPORT_Gloft_PKG);
            result.remove(EXCLUDE_ROTOR_SUPPORT_IGLITE_PKG);
            result.remove(EXCLUDE_ROTOR_SUPPORT_CALLOFDUTY_PKG);
        }
        if (11 == vibMod) {
            result.put(EXCLUDE_AWI_SUPPORT_SGAME_PKG, Integer.valueOf(type));
        }
        return result;
    }

    public List<String> get4DGameSkillSupportPkgs() {
        String projectName = SystemProperties.get("ro.vivo.product.model");
        if (sSupportGameEffectType != 0 && Arrays.asList(SUPPORT_SKILL_VIBRATOR_PROJECT).contains(projectName)) {
            return Arrays.asList(SUPPORT_SKILL_VIBRATOR_GAMES);
        }
        return null;
    }

    /* renamed from: get4DGameVibSupportPkgNames */
    public HashMap<String, String> m4get4DGameVibSupportPkgNames() {
        String pkgName;
        String pkgName2;
        HashMap<String, String> pkgNamesMap = new HashMap<>();
        HashMap<String, Integer> pkgsMap = m5get4DGameVibSupportPkgs();
        String projectName = SystemProperties.get("ro.vivo.product.model");
        for (String key : pkgsMap.keySet()) {
            if (projectName != null && projectName.contains("F_EX")) {
                if (key != null && SUPPORT_4D_GAME_VIB_PKG_NAME_MAP_F_EX.containsKey(key) && (pkgName2 = SUPPORT_4D_GAME_VIB_PKG_NAME_MAP_F_EX.get(key)) != null && pkgName2.length() != 0) {
                    pkgNamesMap.put(key, pkgName2);
                }
            } else if (key != null && SUPPORT_4D_GAME_VIB_PKG_NAME_MAP.containsKey(key) && (pkgName = SUPPORT_4D_GAME_VIB_PKG_NAME_MAP.get(key)) != null && pkgName.length() != 0) {
                pkgNamesMap.put(key, pkgName);
            }
        }
        return pkgNamesMap;
    }

    private int convertToMod(int originEnergy) {
        if (originEnergy > 0 && originEnergy < 6) {
            return originEnergy;
        }
        if (originEnergy <= 0 || originEnergy > 20000) {
            if (originEnergy <= 20000 || originEnergy > 25000) {
                if (originEnergy <= 25000 || originEnergy > 30000) {
                    if (originEnergy > 30000 && originEnergy <= 36000) {
                        return 4;
                    }
                    if (originEnergy <= 36000) {
                        return 0;
                    }
                    return 5;
                }
                return 3;
            }
            return 2;
        }
        return 1;
    }

    private Integer matchEffect(int originEnergy) {
        if (originEnergy > 0 && originEnergy < 6) {
            if (1 != sSupportGameEffectType) {
                return Integer.valueOf(GAME_EFFECT_MAP.get(Integer.valueOf(originEnergy))[0]);
            }
            return Integer.valueOf(GAME_EFFECT_MAP.get(Integer.valueOf(originEnergy))[1]);
        }
        int i = sGunType;
        if (i > -1) {
            if (this.GUN_EFFECTID_WITH_ENERGY_THRESHOLD.containsKey(Integer.valueOf(i))) {
                int[] energy_threshold_with_effectId = this.GUN_EFFECTID_WITH_ENERGY_THRESHOLD.get(Integer.valueOf(sGunType));
                if (originEnergy < energy_threshold_with_effectId[0]) {
                    if (2 == sSupportGameEffectType) {
                        return Integer.valueOf(energy_threshold_with_effectId[2]);
                    }
                    return Integer.valueOf(energy_threshold_with_effectId[4]);
                } else if (2 == sSupportGameEffectType) {
                    return Integer.valueOf(energy_threshold_with_effectId[1]);
                } else {
                    return Integer.valueOf(energy_threshold_with_effectId[3]);
                }
            }
            return audioSolution(originEnergy);
        }
        return audioSolution(originEnergy);
    }

    public void setPkgName(String pkgName) {
        this.pkgName = pkgName;
    }

    private Integer audioSolution(int originEnergy) {
        int gunMod = convertToMod(originEnergy);
        int[] effect_array = GAME_EFFECT_MAP.get(Integer.valueOf(gunMod));
        if (2 != sSupportGameEffectType) {
            String projectName = SystemProperties.get("ro.vivo.product.model");
            VSlog.d("gamevibrator", "projectName:" + projectName + " , pkgName:" + this.pkgName);
            if (Arrays.asList(POWER_OPTIMIZATION_PROJECT).contains(projectName) && Arrays.asList(POWER_OPTIMIZATION_GAME).contains(this.pkgName)) {
                return Integer.valueOf(effect_array[2]);
            }
            return Integer.valueOf(effect_array[1]);
        }
        return Integer.valueOf(effect_array[0]);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class HurtHandler extends Handler {
        public HurtHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 2) {
                int effectID = msg.arg1;
                int deviceId = msg.arg2;
                Vivo4DGameVibratorService.this.gameEffectDualCar(Integer.valueOf(effectID), deviceId);
            } else if (i == 3) {
                int effectID2 = msg.arg1;
                Vivo4DGameVibratorService.this.gameEffect(Integer.valueOf(effectID2));
            }
        }
    }
}