package com.vivo.services.rms.display;

import android.os.IBinder;
import android.os.Parcel;
import android.os.ServiceManager;
import com.vivo.common.utils.VLog;

/* loaded from: classes.dex */
public final class SfUtils {
    private static final int GET_VSYNC_PERIOD = 20008;
    private static final int ON_FROZEN_STATE_CHANGED = 20010;
    private static final int SET_DFPS = 9999;
    private static final String SURFACEFLINGER_DESCRIPTOR = "android.ui.ISurfaceComposer";
    private static final String SURFACEFLINGER_NAME = "SurfaceFlinger";
    private static final String TAG = "SfUtils";

    /* loaded from: classes.dex */
    private static class Service {
        private static final IBinder INSTANCE = ServiceManager.checkService(SfUtils.SURFACEFLINGER_NAME);

        private Service() {
        }
    }

    public static void setDfps(int fps) {
        if (Service.INSTANCE == null) {
            return;
        }
        Parcel data = Parcel.obtain();
        try {
            try {
                data.writeInterfaceToken(SURFACEFLINGER_DESCRIPTOR);
                data.writeInt(fps);
                Service.INSTANCE.transact(SET_DFPS, data, null, 0);
            } catch (Exception e) {
                VLog.e(TAG, String.format("setDfps %d error=%s", Integer.valueOf(fps), e.getMessage()));
            }
        } finally {
            data.recycle();
        }
    }

    public static int getVsyncPeriod() {
        if (Service.INSTANCE == null) {
            return 0;
        }
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            try {
                data.writeInterfaceToken(SURFACEFLINGER_DESCRIPTOR);
                if (Service.INSTANCE.transact(GET_VSYNC_PERIOD, data, reply, 0)) {
                    int period = reply.readInt();
                    return period;
                }
            } catch (Exception e) {
                VLog.e(TAG, String.format("getVsyncPeriod error=%s", e.getMessage()));
            }
            return 0;
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    public static void frozenUpdate(String reason, int pid, boolean frozen) {
        IBinder service = ServiceManager.checkService(SURFACEFLINGER_NAME);
        if (service == null) {
            return;
        }
        Parcel data = Parcel.obtain();
        try {
            try {
                data.writeInterfaceToken(SURFACEFLINGER_DESCRIPTOR);
                data.writeString(reason);
                data.writeIntArray(new int[]{pid});
                data.writeInt(frozen ? 1 : 0);
                service.transact(ON_FROZEN_STATE_CHANGED, data, null, 0);
            } catch (Exception e) {
                VLog.e(TAG, String.format("notifySF reason=%s pid=%d frozen=%s e=%s", reason, Integer.valueOf(pid), String.valueOf(frozen), e.getMessage()));
            }
        } finally {
            data.recycle();
        }
    }
}