package com.vivo.services.sensorhub;

import android.content.Context;
import android.os.SystemProperties;
import android.os.UEventObserver;
import com.vivo.common.utils.VLog;
import com.vivo.sensor.calibration.ProximityCaliConfigParser;
import vivo.app.sensorhub.IVivoSensorHub;

/* loaded from: classes.dex */
public class VivoSensorHubService extends IVivoSensorHub.Stub {
    protected static final int MSG_NOTIFY_ENG_MODE = 1;
    protected static final int MSG_UPDATE_SENSOR_CALI = 0;
    private static final int QCOM_DELAY = 15000;
    private static final String SENSOR_SUB_UEVENT_MATCH = "DEVPATH=/devices/virtual/sensorhub/monitor";
    private static final String TAG = "VivoSensorHubService";
    private static Context mContext;
    private static final String platform = SystemProperties.get("ro.vivo.product.solution", "unkown").toLowerCase();
    private static final String project = SystemProperties.get("ro.product.model.bbk", "unkown");
    private SensorHubRestart mHubRestart;
    private UEventObserver mSensorHubObserver;
    private VivoSensorHubController mVivoSensorHubController;

    public VivoSensorHubService() {
        this.mVivoSensorHubController = null;
        this.mHubRestart = null;
        this.mSensorHubObserver = new UEventObserver() { // from class: com.vivo.services.sensorhub.VivoSensorHubService.1
            public void onUEvent(UEventObserver.UEvent event) {
                try {
                    String state = event.get("SENSOR_HUB_STATE");
                    VLog.d(VivoSensorHubService.TAG, "onUEvent state = " + state);
                    if (state.equals("on") && VivoSensorHubService.this.mVivoSensorHubController != null) {
                        if (VivoSensorHubService.platform.equals("qcom")) {
                            VivoSensorHubService.this.mVivoSensorHubController.handleSensorHubMessage(0, 15000L);
                        } else {
                            VivoSensorHubService.this.mVivoSensorHubController.handleSensorHubMessage(0, ProximityCaliConfigParser.getSendNVDelay());
                        }
                        if (VivoSensorHubService.platform.equals("samsung") || VivoSensorHubService.platform.equals("mtk")) {
                            VivoSensorHubService.this.mVivoSensorHubController.handleSensorHubMessage(1, ProximityCaliConfigParser.getSendNVDelay());
                        }
                    }
                } catch (Exception e) {
                    VLog.e(VivoSensorHubService.TAG, "onUEvent e:" + e);
                }
            }
        };
    }

    public VivoSensorHubService(Context contxt) {
        this.mVivoSensorHubController = null;
        this.mHubRestart = null;
        this.mSensorHubObserver = new UEventObserver() { // from class: com.vivo.services.sensorhub.VivoSensorHubService.1
            public void onUEvent(UEventObserver.UEvent event) {
                try {
                    String state = event.get("SENSOR_HUB_STATE");
                    VLog.d(VivoSensorHubService.TAG, "onUEvent state = " + state);
                    if (state.equals("on") && VivoSensorHubService.this.mVivoSensorHubController != null) {
                        if (VivoSensorHubService.platform.equals("qcom")) {
                            VivoSensorHubService.this.mVivoSensorHubController.handleSensorHubMessage(0, 15000L);
                        } else {
                            VivoSensorHubService.this.mVivoSensorHubController.handleSensorHubMessage(0, ProximityCaliConfigParser.getSendNVDelay());
                        }
                        if (VivoSensorHubService.platform.equals("samsung") || VivoSensorHubService.platform.equals("mtk")) {
                            VivoSensorHubService.this.mVivoSensorHubController.handleSensorHubMessage(1, ProximityCaliConfigParser.getSendNVDelay());
                        }
                    }
                } catch (Exception e) {
                    VLog.e(VivoSensorHubService.TAG, "onUEvent e:" + e);
                }
            }
        };
        mContext = contxt;
        if (platform.equals("qcom")) {
            this.mVivoSensorHubController = new VivoQcomSensorHubController(contxt);
        } else if (platform.equals("samsung")) {
            this.mVivoSensorHubController = new VivoSamsungSensorHubController(contxt);
        } else if (platform.equals("mtk")) {
            this.mVivoSensorHubController = new VivoMtkSensorHubController(contxt);
        }
        if (this.mVivoSensorHubController == null) {
            VLog.e(TAG, "mVivoSensorHubController is null");
            return;
        }
        ProximityCaliConfigParser.parseSensorCaliConfig();
        this.mVivoSensorHubController.handleSensorHubMessage(0, ProximityCaliConfigParser.getSendNVDelay());
        this.mSensorHubObserver.startObserving(SENSOR_SUB_UEVENT_MATCH);
        NotifyTransitionToUnderLight transition = new NotifyTransitionToUnderLight(contxt);
        transition.registerAppTransitionListener();
        if (project.contains("PD1824")) {
            this.mHubRestart = new SensorHubRestart(mContext);
        }
    }
}