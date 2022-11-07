package com.vivo.services.popupcamera;

import android.content.Context;
import android.provider.Settings;
import android.util.FtFeature;
import com.android.server.policy.InputExceptionReport;
import com.vivo.common.utils.VLog;
import com.vivo.framework.vivolight.VivoLightManager;
import vivo.app.vivolight.VivoLightRecord;

/* loaded from: classes.dex */
public class PopupCameraLightManager {
    private static final int DEFAULT_LIGHT_BRIGHTNESS = 3;
    private static final int DEFAULT_POPUP_CAMERA_LIGHT_TYPE = 10;
    private static final int DURATION = 0;
    private static final String LIGHT_PACKAGENAME = "android";
    private static final int LIGHT_PROPRITY = 1110;
    private static final int PUSH_CAMERA_LIGHT_TYPE = 20;
    private static final String TAG = "PopupCameraManagerService";
    private static VivoLightRecord popupLightRecord;
    private static VivoLightRecord pushLightRecord;
    private static Context sContext;
    private static volatile int popupLightID = -1;
    private static volatile int pushLightId = -1;
    private static boolean isLightRecordInited = false;
    static boolean isPupupCameraLightSupported = false;
    private static int sLightBrightness = 3;

    public static void initLightManager(Context context) {
        sContext = context;
        isPupupCameraLightSupported = "1".equals(FtFeature.getFeatureAttribute("vivo.hardware.popupcamera", "popup_camera_light", "0"));
        try {
            sLightBrightness = Integer.parseInt(FtFeature.getFeatureAttribute("vivo.hardware.popupcamera", "popup_camera_light_brightness", InputExceptionReport.LEVEL_MEDIUM));
        } catch (Exception e) {
            sLightBrightness = 3;
        }
    }

    private static VivoLightRecord getPopupVivoLightRecord() {
        int lightType = getPopupCameraLightType();
        VLog.d(TAG, "getPushVivoLightRecord lightType=" + lightType);
        VivoLightRecord tmp = new VivoLightRecord();
        tmp.setLightType(lightType);
        tmp.setLightBrightness(sLightBrightness);
        tmp.setPriority((int) LIGHT_PROPRITY);
        tmp.setPackageName("android");
        tmp.setDuration(0);
        tmp.setOffFlag(0);
        return tmp;
    }

    private static VivoLightRecord getPushVivoLightRecord() {
        int lightType = getPushCameraLightType();
        VLog.d(TAG, "getPushVivoLightRecord lightType=" + lightType);
        VivoLightRecord tmp = new VivoLightRecord();
        tmp.setLightType(lightType);
        tmp.setLightBrightness(sLightBrightness);
        tmp.setPriority((int) LIGHT_PROPRITY);
        tmp.setPackageName("android");
        tmp.setDuration(0);
        tmp.setOffFlag(0);
        return tmp;
    }

    public static synchronized void startCameraLightForPopup() {
        synchronized (PopupCameraLightManager.class) {
            if (isPupupCameraLightSupported) {
                stopCameraLightForPopup();
                if (isPopupCameraLightEnable()) {
                    VLog.d(TAG, "startCameraLightForPopup");
                    popupLightID = VivoLightManager.getInstance().startLight(getPopupVivoLightRecord());
                }
            }
        }
    }

    public static synchronized void stopCameraLightForPopup() {
        synchronized (PopupCameraLightManager.class) {
            if (isPupupCameraLightSupported) {
                if (popupLightID != -1) {
                    VLog.d(TAG, "stopCameraLightForPopup");
                    VivoLightManager.getInstance().stopLightById(popupLightID);
                    popupLightID = -1;
                }
            }
        }
    }

    public static synchronized void startCameraLightForPush() {
        synchronized (PopupCameraLightManager.class) {
            if (isPupupCameraLightSupported) {
                stopCameraLightForPush();
                if (isPopupCameraLightEnable()) {
                    VLog.d(TAG, "startCameraLightForPush");
                    pushLightId = VivoLightManager.getInstance().startLight(getPushVivoLightRecord());
                }
            }
        }
    }

    public static synchronized void stopCameraLightForPush() {
        synchronized (PopupCameraLightManager.class) {
            if (isPupupCameraLightSupported) {
                if (pushLightId != -1) {
                    VLog.d(TAG, "stopCameraLightForPush");
                    VivoLightManager.getInstance().stopLightById(pushLightId);
                    pushLightId = -1;
                }
            }
        }
    }

    private static boolean isPopupCameraLightEnable() {
        Context context = sContext;
        return context != null && 1 == Settings.Global.getInt(context.getContentResolver(), "popup_camera_light_enable", 1);
    }

    private static int getPopupCameraLightType() {
        Context context = sContext;
        if (context == null) {
            return 10;
        }
        return Settings.Global.getInt(context.getContentResolver(), "popup_camera_light_type_index", 10);
    }

    private static int getPushCameraLightType() {
        return getPopupCameraLightType() + 10;
    }
}