package com.android.server.display.color.displayenhance;

import android.content.Context;
import android.os.Handler;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import com.android.server.FgThread;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import vendor.pixelworks.hardware.display.V1_0.IIris;
import vendor.pixelworks.hardware.display.V1_0.IIrisCallback;
import vivo.util.VSlog;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class DisplayEnhanceIrisConfig {
    public static final int DISPLAY_ENHANCE_HDR = 2;
    public static final int DISPLAY_ENHANCE_MEMC = 1;
    public static final int DISPLAY_ENHANCE_NONE = 0;
    public static final int DISPLAY_MODE_BYPASS = 1;
    private static final int DISPLAY_MODE_CMD = 56;
    public static final int DISPLAY_MODE_NORMAL = 0;
    private static final int MEMC_MODE_CMD = 258;
    private static final int MEMC_MODE_DUAL = 40;
    private static final int MEMC_MODE_NONE = 0;
    private static final int MEMC_MODE_SINGLE = 10;
    private static final int MEMC_WINDOW_CMD = 17;
    private static final int SDR2HDR_MODE_CMD = 267;
    public static final int SDR2HDR_MODE_GAME = 3;
    public static final int SDR2HDR_MODE_NONE = 0;
    public static final int SDR2HDR_MODE_PICTURE = 1;
    public static final int SDR2HDR_MODE_VIDEO = 2;
    private static final String TAG = "DisplayEnhanceIrisConfig";
    public static final String VIVO_DISPLAY_ENHANCE_STATUS = "vivo_display_enhance_state";
    private static DisplayEnhanceIrisConfig mIrisConfig = null;
    private Context mContext;
    private int mCookie;
    private int mDisplayEnhanceStatus = 0;
    private long mTimeStartMemc = 0;
    private long mTimeStartHdr = 0;
    private int mSdr2HdrMode = 0;
    private int mMemcMode = 0;
    private int mDisplayMode = 1;
    private IrisCallback mIrisCallback = new IrisCallback();
    private Handler mHandler = new Handler(FgThread.get().getLooper());

    private DisplayEnhanceIrisConfig(Context context) {
        this.mContext = context;
        registerCallback();
    }

    public static DisplayEnhanceIrisConfig getInstance(Context context) {
        if (mIrisConfig == null) {
            synchronized (DisplayEnhanceIrisConfig.class) {
                if (mIrisConfig == null) {
                    mIrisConfig = new DisplayEnhanceIrisConfig(context);
                }
            }
        }
        return mIrisConfig;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void putDisplayEnhanceStatusSetting(int value) {
        Settings.System.putIntForUser(this.mContext.getContentResolver(), VIVO_DISPLAY_ENHANCE_STATUS, value, -2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getDisplayEnhanceStatusSetting() {
        int value = Settings.System.getIntForUser(this.mContext.getContentResolver(), VIVO_DISPLAY_ENHANCE_STATUS, 0, -2);
        return value;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class IrisCallback extends IIrisCallback.Stub {
        IrisCallback() {
        }

        @Override // vendor.pixelworks.hardware.display.V1_0.IIrisCallback
        public void onFeatureChanged(int type, ArrayList<Integer> values) throws RemoteException {
            if (values.size() > 0) {
                VSlog.d(DisplayEnhanceIrisConfig.TAG, "onFeatureChanged: " + type + "=" + values.get(0));
                int value = DisplayEnhanceIrisConfig.this.getDisplayEnhanceStatusSetting();
                if (type == 13 && values.get(0).intValue() == 255) {
                    value |= 1;
                } else if (type == 13 && values.get(0).intValue() == 254) {
                    value &= -2;
                }
                DisplayEnhanceIrisConfig.this.putDisplayEnhanceStatusSetting(value);
                return;
            }
            VSlog.d(DisplayEnhanceIrisConfig.TAG, "onFeatureChanged: " + type);
        }
    }

    public void registerCallback() {
        this.mCookie = (Process.myPid() << 16) + Process.myTid();
        try {
            try {
                IIris iris = IIris.getService();
                if (iris == null) {
                    VSlog.e(TAG, "can't get IIris");
                } else {
                    iris.registerCallback2(this.mCookie, this.mIrisCallback);
                }
            } catch (NoSuchElementException e) {
                VSlog.e(TAG, "IIris service is not available");
            }
        } catch (RemoteException e2) {
            VSlog.e(TAG, "Access IIris failed", e2);
        }
    }

    public int irisConfigureSet(int type, int[] values, int count) {
        if (values == null) {
            VSlog.e(TAG, "Parameter exception, values=" + values);
            return -1;
        }
        try {
            if (values.length != count) {
                VSlog.e(TAG, "Parameter exception, values_len=" + values.length + ", count=" + count);
                return -1;
            }
            try {
                IIris iris = IIris.getService();
                if (iris == null) {
                    VSlog.e(TAG, "can't get IIris");
                    return -2;
                }
                ArrayList<Integer> v = new ArrayList<>(count);
                for (int i : values) {
                    v.add(Integer.valueOf(i));
                }
                return iris.irisConfigureSet(type, v);
            } catch (NoSuchElementException e) {
                VSlog.e(TAG, "IIris service is not available");
                return -1;
            }
        } catch (RemoteException e2) {
            VSlog.e(TAG, "Access IIris failed", e2);
            return -1;
        }
    }

    public int irisConfigureGet(int type, int[] values, int count) {
        if (values == null) {
            VSlog.e(TAG, "Parameter exception, values=" + values);
            return -1;
        } else if (values.length != count) {
            VSlog.e(TAG, "Parameter exception, values_len=" + values.length + ", count=" + count);
            return -1;
        } else {
            try {
                try {
                    IIris iris = IIris.getService();
                    if (iris == null) {
                        VSlog.e(TAG, "can't get IIris");
                        return -2;
                    }
                    ArrayList<Integer> v = new ArrayList<>(count);
                    for (int i : values) {
                        v.add(Integer.valueOf(i));
                    }
                    final C1ret ret = new C1ret();
                    iris.irisConfigureGet(type, v, new IIris.irisConfigureGetCallback() { // from class: com.android.server.display.color.displayenhance.DisplayEnhanceIrisConfig.1
                        @Override // vendor.pixelworks.hardware.display.V1_0.IIris.irisConfigureGetCallback
                        public void onValues(int result, ArrayList<Integer> values2) {
                            ret.rc = result;
                            ret.v = values2;
                        }
                    });
                    int len = Math.min(values.length, ret.v.size());
                    for (int i2 = 0; i2 < len; i2++) {
                        values[i2] = ret.v.get(i2).intValue();
                    }
                    return ret.rc;
                } catch (NoSuchElementException e) {
                    VSlog.e(TAG, "IIris service is not available");
                    return -1;
                }
            } catch (RemoteException e2) {
                VSlog.e(TAG, "Access IIris failed", e2);
                return -1;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.display.color.displayenhance.DisplayEnhanceIrisConfig$1ret  reason: invalid class name */
    /* loaded from: classes.dex */
    public class C1ret {
        ArrayList<Integer> v = null;
        int rc = -1;

        C1ret() {
        }
    }

    public void setDisplayMode(int mode) {
        if (this.mDisplayMode == mode) {
            return;
        }
        this.mDisplayMode = mode;
        VSlog.d(TAG, "setDisplayMode : " + mode);
        int[] displayMode = {mode};
        irisConfigureSet(56, displayMode, displayMode.length);
    }

    public int getDisplayMode() {
        int[] displayMode = new int[1];
        irisConfigureGet(56, displayMode, displayMode.length);
        VSlog.d(TAG, "getDisplayMode :" + displayMode[0]);
        return displayMode[0];
    }

    private void setMemcMode(int mode) {
        int i = this.mMemcMode;
        if (i == mode) {
            return;
        }
        if (i == 0 && mode != 0) {
            this.mTimeStartMemc = System.currentTimeMillis();
        } else if (this.mMemcMode != 0 && mode == 0) {
            long now = System.currentTimeMillis();
            long cost = now - this.mTimeStartMemc;
            long last = SystemProperties.getLong("persist.sys.hardware.memc.accu_time", 0L);
            long total = last + cost;
            SystemProperties.set("persist.sys.hardware.memc.accu_time", Long.toString(total));
            VSlog.d(TAG, "memc run duration: total=" + total + ", this=" + cost + ", last=" + last);
        }
        this.mMemcMode = mode;
        VSlog.d(TAG, "setMemcMode : " + mode);
        int[] memcMode = {mode};
        irisConfigureSet(258, memcMode, memcMode.length);
    }

    public int getMemcMode() {
        int[] memcMode = new int[1];
        irisConfigureGet(258, memcMode, memcMode.length);
        VSlog.d(TAG, "getMemcMode :" + memcMode[0]);
        if (memcMode[0] == 10) {
            return 1;
        }
        return memcMode[0] == 40 ? 2 : 0;
    }

    public void setMemcWindowEnable(boolean enable) {
        VSlog.d(TAG, "setMemcWindowEnable : " + enable);
        int[] memcWindow = new int[1];
        memcWindow[0] = enable ? 5 : 0;
        irisConfigureSet(17, memcWindow, memcWindow.length);
    }

    public void setSdr2HdrMode(int mode) {
        int i = this.mSdr2HdrMode;
        if (i == mode) {
            return;
        }
        if (i == 0 && mode == 2) {
            this.mTimeStartHdr = System.currentTimeMillis();
        } else if (this.mSdr2HdrMode == 2 && mode == 0) {
            long now = System.currentTimeMillis();
            long cost = now - this.mTimeStartHdr;
            long last = SystemProperties.getLong("persist.sys.hardware.sdr2hdr.accu_time", 0L);
            long total = cost + last;
            SystemProperties.set("persist.sys.hardware.sdr2hdr.accu_time", Long.toString(total));
            VSlog.d(TAG, "sdr2hdr run duration: total=" + total + ", this=" + cost + ", last=" + last);
        }
        this.mSdr2HdrMode = mode;
        VSlog.d(TAG, "setSdr2HdrMode : " + mode);
        int[] sdr2HdrMode = {3, mode};
        irisConfigureSet(267, sdr2HdrMode, sdr2HdrMode.length);
    }

    public int getSdr2HdrMode() {
        int[] sdr2HdrMode = new int[1];
        irisConfigureGet(267, sdr2HdrMode, sdr2HdrMode.length);
        VSlog.d(TAG, "getSdr2HdrMode :" + sdr2HdrMode[0]);
        return sdr2HdrMode[0];
    }

    public void setIrisStateMachine(int state, int value) {
        int memcMode = 0;
        int hdrMode = 0;
        VSlog.d(TAG, "setIrisStateMachine: state=" + state + ", value=" + value);
        if (state == 1) {
            if (value == 1) {
                memcMode = 10;
                this.mDisplayEnhanceStatus |= 1;
            } else if (value == 2) {
                memcMode = 40;
                this.mDisplayEnhanceStatus |= 1;
            } else {
                memcMode = 0;
                this.mDisplayEnhanceStatus &= -2;
            }
        } else if (state == 2) {
            if (value != 0) {
                this.mDisplayEnhanceStatus |= 2;
            } else {
                this.mDisplayEnhanceStatus &= -3;
            }
            hdrMode = value;
        } else {
            this.mDisplayEnhanceStatus = 0;
        }
        if (this.mDisplayEnhanceStatus != 0) {
            setDisplayMode(0);
        }
        if (state == 2) {
            setSdr2HdrMode(hdrMode);
        }
        if (state == 1) {
            setMemcMode(memcMode);
        }
        if (this.mDisplayEnhanceStatus == 0) {
            setDisplayMode(1);
        }
    }
}