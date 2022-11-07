package com.vivo.services.sensorhub;

import android.content.Context;
import com.vivo.sensor.implement.VivoSensorImpl;

/* loaded from: classes.dex */
public class VivoSamsungSensorHubController extends VivoSensorHubController {
    private static Context mContext;

    public VivoSamsungSensorHubController(Context contxt) {
        mContext = contxt;
        this.mVivoSensorImpl = VivoSensorImpl.getInstance(contxt);
        handleSensorHubMessage(1, 0L);
    }

    @Override // com.vivo.services.sensorhub.VivoSensorHubController
    public void handleSensorHubMessage(int sarMsg, long delay) {
        if (sarMsg == 0) {
            sendCaliDataToDriver(delay);
        } else if (sarMsg == 1) {
            sendFactoryModeToDriver(delay);
        }
    }
}