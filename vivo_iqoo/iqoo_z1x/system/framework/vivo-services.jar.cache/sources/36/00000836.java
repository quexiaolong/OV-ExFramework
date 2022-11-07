package com.vivo.services.sensorhub;

import android.content.Context;
import com.vivo.sensor.implement.VivoSensorImpl;

/* loaded from: classes.dex */
public class VivoQcomSensorHubController extends VivoSensorHubController {
    private static Context mContext;

    public VivoQcomSensorHubController(Context contxt) {
        mContext = contxt;
        this.mVivoSensorImpl = VivoSensorImpl.getInstance(contxt);
    }

    @Override // com.vivo.services.sensorhub.VivoSensorHubController
    public void handleSensorHubMessage(int sarMsg, long delay) {
        if (sarMsg == 0) {
            sendCaliDataToDriver(delay);
        }
    }
}