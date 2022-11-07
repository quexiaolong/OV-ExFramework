package com.vivo.face.internal.wrapper;

import android.content.Context;
import android.os.IHwBinder;
import com.vivo.common.utils.VLog;
import com.vivo.framework.popupcamera.PopupCameraManager;
import vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider;

/* loaded from: classes.dex */
public final class CameraWrapper {
    private static final String TAG = "CameraWrapper";
    private IVivoCameraProvider mCameraProviderDaemon;
    private Context mContext;
    private IHwBinder.DeathRecipient mRecipient = new IHwBinder.DeathRecipient() { // from class: com.vivo.face.internal.wrapper.CameraWrapper.1
        public void serviceDied(long cookie) {
            VLog.e(CameraWrapper.TAG, "camera provider daemon died");
            CameraWrapper.this.mCameraProviderDaemon = null;
        }
    };

    public CameraWrapper(Context context) {
        this.mContext = context;
    }

    public void setCameraProviderDaemonParam(int param1, int param2) {
        getCameraProviderDaemon();
        IVivoCameraProvider iVivoCameraProvider = this.mCameraProviderDaemon;
        if (iVivoCameraProvider != null) {
            try {
                iVivoCameraProvider.setparam(param1, param2);
            } catch (Exception e) {
                VLog.e(TAG, "Failed to set camera provider daemon param: ", e);
            }
        }
    }

    private IVivoCameraProvider getCameraProviderDaemon() {
        if (this.mCameraProviderDaemon == null) {
            try {
                this.mCameraProviderDaemon = IVivoCameraProvider.getService();
            } catch (Exception e) {
                VLog.e(TAG, "Failed to get camera provider daemon: ", e);
            }
            IVivoCameraProvider iVivoCameraProvider = this.mCameraProviderDaemon;
            if (iVivoCameraProvider == null) {
                VLog.e(TAG, "invaild camera provider daemon");
                return null;
            }
            iVivoCameraProvider.asBinder().linkToDeath(this.mRecipient, 0L);
        }
        return this.mCameraProviderDaemon;
    }

    public void notifyPopupCameraStatus(int cameraId, int status, String packageName) {
        PopupCameraManager.getInstance().notifyCameraStatus(cameraId, status, packageName);
    }
}