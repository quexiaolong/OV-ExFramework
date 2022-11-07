package com.android.server.display.color;

import android.content.Context;
import android.os.FtBuild;
import android.util.FtFeature;
import com.vivo.server.adapter.ServiceAdapterFactory;
import com.vivo.server.adapter.lcm.AbsVivoDisplayLcmControlImpl;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoLtmController {
    private static final int LTM_ON_LIGHT_LUX_THRESHOLD = 30000;
    public static final int LTM_USER_DEFAULT = 0;
    public static final int LTM_USER_GAME = 1;
    public static final int LTM_USER_VIDEO = 2;
    static final String TAG = "VivoLtmController";
    private VivoLcmEventTransferUtils mEtUtil;
    private long mLtmStartTime;
    private AbsVivoDisplayLcmControlImpl mVivoDisplayLcmControlImpl;
    private ServiceAdapterFactory serviceAdapter;
    private static VivoLtmController mVivoLtmController = null;
    private static final boolean mIsLtmAvailable = FtFeature.isFeatureSupport("vivo.hardware.LTM");
    private static ExynosDisplayATC mExynosDisplayATC = null;
    private boolean mMustOn = false;
    private boolean mLightLuxOn = false;
    private boolean mLtmOn = false;
    private int mLtmUserId = 0;
    private final Object mLock = new Object();
    private boolean mTest = false;

    public VivoLtmController(Context context) {
        this.mVivoDisplayLcmControlImpl = null;
        this.mEtUtil = null;
        if (mIsLtmAvailable) {
            if (FtBuild.isQCOMPlatform()) {
                ServiceAdapterFactory serviceAdapterFactory = ServiceAdapterFactory.getServiceAdapterFactory();
                this.serviceAdapter = serviceAdapterFactory;
                if (serviceAdapterFactory != null) {
                    AbsVivoDisplayLcmControlImpl vivoDisplayLcmControlImpl = serviceAdapterFactory.getVivoDisplayLcmControlImpl();
                    this.mVivoDisplayLcmControlImpl = vivoDisplayLcmControlImpl;
                    if (vivoDisplayLcmControlImpl == null) {
                        VSlog.e(TAG, "LTM:mVivoDisplayLcmControlImpl is null");
                    }
                }
            } else if (FtBuild.isSamsungPlatform()) {
                ExynosDisplayATC exynosDisplayATC = ExynosDisplayATC.getInstance(context);
                mExynosDisplayATC = exynosDisplayATC;
                if (exynosDisplayATC == null) {
                    VSlog.e(TAG, "mExynosDisplayATC is null");
                }
            }
        }
        this.mLtmStartTime = System.currentTimeMillis();
        this.mEtUtil = VivoLcmEventTransferUtils.getInstance();
    }

    public static synchronized VivoLtmController getInstance(Context context) {
        VivoLtmController vivoLtmController;
        synchronized (VivoLtmController.class) {
            if (mVivoLtmController == null) {
                mVivoLtmController = new VivoLtmController(context);
            }
            vivoLtmController = mVivoLtmController;
        }
        return vivoLtmController;
    }

    public boolean isAvailable() {
        return mIsLtmAvailable;
    }

    public void setLtmOn(boolean on, int userId) {
        VSlog.d(TAG, "setLtmOn: on=" + on + " userId=" + userId);
        synchronized (this.mLock) {
            this.mLtmUserId = userId;
            this.mMustOn = on;
            setLtmEnable(on);
        }
    }

    public void setLuxOn(boolean on) {
        VSlog.d(TAG, "setLuxOn: on=" + on);
        synchronized (this.mLock) {
            this.mLightLuxOn = on;
            setLtmEnable(on);
        }
    }

    public void setLuxOnTest(boolean on) {
        this.mTest = true;
        setLuxOn(on);
    }

    private void setLtmEnable(boolean enable) {
        VSlog.d(TAG, "setLtmEnable: enable=" + enable + ", ltmConditions=" + ltmConditions());
        if (ltmConditions() && enable) {
            enableLtm();
        } else if (!ltmConditions() && !enable) {
            disableLtm();
        }
    }

    private boolean ltmConditions() {
        if (this.mMustOn || this.mLightLuxOn) {
            return true;
        }
        return false;
    }

    private void enableLtm() {
        if (!this.mLtmOn) {
            VSlog.d(TAG, "enableLtm");
            this.mLtmOn = true;
            AbsVivoDisplayLcmControlImpl absVivoDisplayLcmControlImpl = this.mVivoDisplayLcmControlImpl;
            if (absVivoDisplayLcmControlImpl != null) {
                absVivoDisplayLcmControlImpl.setLTMOn();
            } else {
                ExynosDisplayATC exynosDisplayATC = mExynosDisplayATC;
                if (exynosDisplayATC != null) {
                    exynosDisplayATC.setLTMOn(true, this.mLtmUserId);
                }
            }
            this.mLtmStartTime = System.currentTimeMillis();
        } else if (this.mTest) {
            this.mLtmStartTime = System.currentTimeMillis();
            this.mTest = false;
        }
    }

    private void disableLtm() {
        VivoLcmEventTransferUtils vivoLcmEventTransferUtils;
        if (this.mLtmOn) {
            VSlog.d(TAG, "disableLtm");
            this.mLtmOn = false;
            AbsVivoDisplayLcmControlImpl absVivoDisplayLcmControlImpl = this.mVivoDisplayLcmControlImpl;
            if (absVivoDisplayLcmControlImpl != null) {
                absVivoDisplayLcmControlImpl.setLTMOff();
            } else {
                ExynosDisplayATC exynosDisplayATC = mExynosDisplayATC;
                if (exynosDisplayATC != null) {
                    exynosDisplayATC.setLTMOn(false, this.mLtmUserId);
                }
            }
            VivoLcmEventTransferUtils vivoLcmEventTransferUtils2 = this.mEtUtil;
            if (vivoLcmEventTransferUtils2 != null) {
                vivoLcmEventTransferUtils2.send(6, this.mLtmStartTime, System.currentTimeMillis() - this.mLtmStartTime);
            }
        } else if (this.mTest && (vivoLcmEventTransferUtils = this.mEtUtil) != null) {
            vivoLcmEventTransferUtils.send(6, this.mLtmStartTime, System.currentTimeMillis() - this.mLtmStartTime);
            this.mTest = false;
        }
    }
}