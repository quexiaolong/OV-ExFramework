package com.vivo.services.popupcamera;

import java.text.SimpleDateFormat;
import java.util.Date;

/* loaded from: classes.dex */
public class CameraStatus {
    public static final int INVALID_CAMERA_ID = -1;
    private static final long STATUS_INVALID_TIME = -1;
    private static final String TAG = "CameraStatus";
    public int cameraId;
    public String currentStatusPackageName;
    public boolean isCameraOpened;
    public String lastStatusPackageName;
    public long currentStatusTimeMillis = STATUS_INVALID_TIME;
    public long lastCurrentStatusTimeMillis = STATUS_INVALID_TIME;

    public CameraStatus() {
        this.cameraId = -1;
        this.cameraId = -1;
    }

    public CameraStatus(int id) {
        this.cameraId = -1;
        this.cameraId = id;
    }

    public boolean isCameraOpened() {
        return this.isCameraOpened;
    }

    public boolean isCameraClosed() {
        return !this.isCameraOpened;
    }

    public boolean isCurrentStatusValid() {
        return this.currentStatusTimeMillis != STATUS_INVALID_TIME;
    }

    public boolean isLastStatusValid() {
        return this.lastCurrentStatusTimeMillis != STATUS_INVALID_TIME;
    }

    public String getCurrentStatusTimeString() {
        return timeMillisToString(this.currentStatusTimeMillis);
    }

    public String getLastStatusTimeString() {
        return timeMillisToString(this.lastCurrentStatusTimeMillis);
    }

    public int getCameraStatus() {
        if (this.isCameraOpened) {
            return 0;
        }
        return 1;
    }

    public String toString() {
        return "CameraStatus{cameraId=" + this.cameraId + " isCameraOpened=" + this.isCameraOpened + " currentStatusTimeMillis=" + getCurrentStatusTimeString() + " lastCurrentStatusTimeMillis=" + getLastStatusTimeString() + " currentStatusPackageName=" + this.currentStatusPackageName + " lastStatusPackageName=" + this.lastStatusPackageName + "}";
    }

    public static String timeMillisToString(long timeMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(timeMillis));
    }

    public static void updateCameraStatus(int id, boolean isCameraOpened, String packageName, CameraStatus outCameraStatus) {
        if (outCameraStatus != null) {
            outCameraStatus.cameraId = id;
            outCameraStatus.isCameraOpened = isCameraOpened;
            outCameraStatus.lastCurrentStatusTimeMillis = outCameraStatus.currentStatusTimeMillis;
            outCameraStatus.currentStatusTimeMillis = System.currentTimeMillis();
            outCameraStatus.lastStatusPackageName = outCameraStatus.currentStatusPackageName;
            outCameraStatus.currentStatusPackageName = packageName;
        }
    }
}