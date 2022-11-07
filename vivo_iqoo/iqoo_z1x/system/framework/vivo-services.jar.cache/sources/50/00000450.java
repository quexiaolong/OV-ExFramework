package com.android.server.policy.key;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.text.TextUtils;
import com.android.server.policy.VivoPolicyUtil;
import com.vivo.common.utils.VLog;

/* loaded from: classes.dex */
public class VivoFlashlightController {
    private static final String TAG = "VivoFlashlightController";
    private String mCameraId;
    private CameraManager mCameraManager;
    private Context mContext;
    private Handler mHandler = new Handler();
    private boolean isFlashLightOn = false;
    private final CameraManager.TorchCallback mTorchCallback = new CameraManager.TorchCallback() { // from class: com.android.server.policy.key.VivoFlashlightController.1
        @Override // android.hardware.camera2.CameraManager.TorchCallback
        public void onTorchModeUnavailable(String cameraId) {
        }

        @Override // android.hardware.camera2.CameraManager.TorchCallback
        public void onTorchModeChanged(String cameraId, boolean enable) {
            if (VivoPolicyUtil.IS_LOG_OPEN) {
                VLog.d(VivoFlashlightController.TAG, "onTorchModeChange cameraId:" + cameraId + " mCameraId:" + VivoFlashlightController.this.mCameraId + " enable:" + enable);
            }
            if (TextUtils.equals(VivoFlashlightController.this.mCameraId, cameraId)) {
                VivoFlashlightController.this.isFlashLightOn = enable;
            }
        }
    };

    public VivoFlashlightController(Context context) {
        this.mContext = context;
        this.mCameraManager = (CameraManager) context.getSystemService("camera");
        reInitFlashlight();
        this.mCameraManager.registerTorchCallback(this.mTorchCallback, this.mHandler);
    }

    public boolean isFlashLightOn() {
        return this.isFlashLightOn;
    }

    public void setFlashLight(boolean enabled) {
        reInitFlashlight();
        setTorchMode(this.mCameraId, enabled);
    }

    private void setTorchMode(String cameraId, boolean enabled) {
        if (TextUtils.isEmpty(cameraId)) {
            VLog.d(TAG, "cameraId is empty");
            return;
        }
        try {
            VLog.d(TAG, "setFlashlight, enabled = " + enabled + ",mCameraId =" + this.mCameraId);
            this.mCameraManager.setTorchMode(cameraId, enabled);
        } catch (Exception e) {
            VLog.e(TAG, "Couldn't set torch mode", e);
        }
    }

    private void reInitFlashlight() {
        if (this.mCameraId == null) {
            try {
                this.mCameraId = getCameraId();
                VLog.i(TAG, "first get mCameraId = " + this.mCameraId);
            } catch (Throwable e) {
                this.mCameraId = null;
                VLog.e(TAG, "Couldn't initialize.", e);
            }
            VLog.i(TAG, "reInitFlashlight mCameraId = " + this.mCameraId);
        }
    }

    private String getCameraId() throws CameraAccessException {
        String[] ids = this.mCameraManager.getCameraIdList();
        for (String id : ids) {
            CameraCharacteristics c = this.mCameraManager.getCameraCharacteristics(id);
            Boolean flashAvailable = (Boolean) c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            Integer lensFacing = (Integer) c.get(CameraCharacteristics.LENS_FACING);
            if (flashAvailable != null && flashAvailable.booleanValue() && lensFacing != null && lensFacing.intValue() == 1) {
                return id;
            }
        }
        return null;
    }
}