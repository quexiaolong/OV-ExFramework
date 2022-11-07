package com.vivo.services.sarpower;

import android.content.Context;
import android.net.wifi.SoftApInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.HandlerExecutor;
import android.os.ServiceManager;
import android.telephony.CellIdentity;
import android.telephony.CellIdentityNr;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.ServiceState;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.VivoTelephonyApiParams;
import com.vivo.face.common.data.Constants;
import com.vivo.sensor.autobrightness.utils.SElog;
import com.vivo.sensor.sarpower.VivoSarPowerStateController;
import com.vivo.services.sarpower.VivoSarPowerStateService;
import org.json.JSONArray;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class VivoWifiFrequencyController {
    protected static final int MSG_SAR_POWER_CHANGE = 0;
    private static final String TAG = "VivoWifiFrequencyController";
    private static Context mContext;
    private VivoSarPowerStateService.VivoSarPowerHandler mVivoSarPowerHandler;
    private VivoSarPowerStateController mVivoSarPowerStateController;
    private WifiManager mWifiManager;
    private int mLastFrequencySA = 0;
    private int mLastFrequencyNSA = 0;
    private int mLastFrequencyWifi = 0;
    private int mLastFrequencySoftAp = 0;
    public WifiManager.SoftApCallback mSoftApCallback = new WifiManager.SoftApCallback() { // from class: com.vivo.services.sarpower.VivoWifiFrequencyController.1
        public void onStateChanged(int state, int failureReason) {
        }

        public void onInfoChanged(SoftApInfo softApInfo) {
            int frequency;
            if (softApInfo != null && (frequency = softApInfo.getFrequency()) != VivoWifiFrequencyController.this.mLastFrequencySoftAp) {
                SElog.d(VivoWifiFrequencyController.TAG, "frequencySofAp changed from " + VivoWifiFrequencyController.this.mLastFrequencySoftAp + " to " + frequency);
                VivoWifiFrequencyController.this.mLastFrequencySoftAp = frequency;
                VivoWifiFrequencyController.this.mVivoSarPowerStateController.notifySoftApFrequency(frequency);
                VivoWifiFrequencyController.this.mVivoSarPowerStateController.handleSarMessage(0, 0);
            }
        }
    };

    public VivoWifiFrequencyController(Context context, VivoSarPowerStateController vivoSarPowerStateController, VivoSarPowerStateService.VivoSarPowerHandler vivoSarPowerHandler) {
        this.mVivoSarPowerStateController = null;
        this.mWifiManager = null;
        mContext = context;
        this.mVivoSarPowerStateController = vivoSarPowerStateController;
        this.mVivoSarPowerHandler = vivoSarPowerHandler;
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        SElog.d(TAG, "init WifiFrequencyController");
    }

    public void getNetworkFrequencySA(ServiceState ss) {
        NetworkRegistrationInfo regInfo;
        CellIdentity cellIdentity;
        int nrArfcn;
        if (this.mVivoSarPowerStateController.isSupportWifiFrequency() && (regInfo = ss.getNetworkRegistrationInfo(2, 1)) != null && (cellIdentity = regInfo.getCellIdentity()) != null && (cellIdentity instanceof CellIdentityNr) && (nrArfcn = ((CellIdentityNr) cellIdentity).getNrarfcn()) != this.mLastFrequencySA) {
            SElog.d(TAG, "FrequencySA changed from  " + this.mLastFrequencyNSA + " to " + nrArfcn);
            this.mLastFrequencySA = nrArfcn;
            this.mVivoSarPowerStateController.notifyNetworkSANrarfcn(nrArfcn);
            this.mVivoSarPowerStateController.handleSarMessage(0, 0);
        }
    }

    public void getNetworkFrequencyNSA(int phoneCardId) {
        if (this.mVivoSarPowerStateController.isSupportWifiFrequency()) {
            new JSONObject();
            try {
                VivoTelephonyApiParams param = new VivoTelephonyApiParams("API_TAG_sendMiscInfo");
                ITelephony iTelephony = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
                param.put("phoneId", Integer.valueOf(phoneCardId));
                param.put("commandId", 2);
                param.put("buffer", "empty");
                VivoTelephonyApiParams ret = iTelephony.vivoTelephonyApi(param);
                if (ret != null) {
                    String result = (String) ret.getAsObject("response");
                    JSONArray jsonArray = new JSONArray(result);
                    int length = jsonArray.length();
                    if (length > 1) {
                        int nrIndex = -1;
                        for (int i = 0; i < length; i++) {
                            JSONObject tempObj = jsonArray.optJSONObject(i);
                            if (tempObj != null && tempObj.optString("RAT", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK).startsWith("NR5G")) {
                                nrIndex = i;
                            }
                        }
                        if (nrIndex != -1) {
                            JSONObject jsonObject = jsonArray.optJSONObject(nrIndex);
                            int arfcn = (int) jsonObject.optLong("EARFCN", 0L);
                            if (arfcn != this.mLastFrequencyNSA) {
                                SElog.d(TAG, "FrequencyNSA changed from  " + this.mLastFrequencyNSA + " to " + arfcn);
                                this.mLastFrequencyNSA = arfcn;
                                this.mVivoSarPowerStateController.notifyNetworkNSANrarfcn(arfcn);
                                this.mVivoSarPowerStateController.handleSarMessage(0, 0);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                SElog.e(TAG, "getNetworkFrequency failed", e);
            }
        }
    }

    public void getWifiFrequency() {
        WifiInfo wifiInfo = this.mWifiManager.getConnectionInfo();
        int frequency = wifiInfo.getFrequency();
        if (frequency != this.mLastFrequencyWifi) {
            SElog.d(TAG, "frequencyWifi changed from " + this.mLastFrequencyWifi + " to " + frequency);
            this.mLastFrequencyWifi = frequency;
            this.mVivoSarPowerStateController.notifyWifiFrequency(frequency);
            this.mVivoSarPowerStateController.handleSarMessage(0, 0);
        }
    }

    public void registerSoftAp() {
        WifiManager wifiManager = this.mWifiManager;
        if (wifiManager != null) {
            wifiManager.registerSoftApCallback(new HandlerExecutor(this.mVivoSarPowerHandler), this.mSoftApCallback);
        }
    }
}