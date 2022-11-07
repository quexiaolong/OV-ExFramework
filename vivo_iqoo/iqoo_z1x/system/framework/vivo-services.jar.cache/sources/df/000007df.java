package com.vivo.services.sarpower;

import android.content.Context;
import android.telephony.FtTelephony;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.vivo.sensor.autobrightness.config.AblConfig;
import com.vivo.sensor.autobrightness.utils.SElog;
import com.vivo.sensor.sarpower.VivoSarPowerStateController;
import java.util.List;

/* loaded from: classes.dex */
public class VivoGsmStateListener {
    private static final int GSM_CALLING_STATE = 34;
    protected static final int MSG_SAR_POWER_CHANGE = 0;
    private static final String TAG = "VivoGsmStateListener";
    private static Context mContext;
    private static FtTelephony mFtTelephony;
    private int mGsmChannelHigh;
    private int mGsmChannelLow;
    private int mPhoneCount;
    private PhoneStateListener[] mPhoneStateListener;
    private SubscriptionManager mSubscriptionManager;
    private TelephonyManager mTelephonyManagerService;
    private VivoSarPowerStateController mVivoSarPowerStateController;
    private boolean[] mIsPhoneInCall = {false, false};
    private boolean[] mIsGsm900 = {false, false};
    private boolean[] mIsSendSarState = {false, false};
    private boolean mIsCreateForSubscriptionId = false;
    private int[] mLastSimSlotIndex = {-1, -1};
    private SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangedListener = new SubscriptionManager.OnSubscriptionsChangedListener() { // from class: com.vivo.services.sarpower.VivoGsmStateListener.1
        int cacheSubLength = 0;

        @Override // android.telephony.SubscriptionManager.OnSubscriptionsChangedListener
        public void onSubscriptionsChanged() {
            PhoneStateListener[] phoneStateListenerArr;
            List<SubscriptionInfo> subList = VivoGsmStateListener.this.mSubscriptionManager.getActiveSubscriptionInfoList();
            if (subList != null && this.cacheSubLength != subList.size()) {
                int subListLength = subList.size();
                SElog.d(VivoGsmStateListener.TAG, "subList size = " + subListLength);
                for (int i = 0; i < subListLength; i++) {
                    try {
                        int subId = subList.get(i).getSubscriptionId();
                        int simSlotIndex = subList.get(i).getSimSlotIndex();
                        SElog.d(VivoGsmStateListener.TAG, "sSIndex = " + simSlotIndex);
                        if (VivoGsmStateListener.this.mPhoneStateListener[simSlotIndex] == null || VivoGsmStateListener.this.mLastSimSlotIndex[simSlotIndex] == simSlotIndex) {
                            SElog.d(VivoGsmStateListener.TAG, "Begin create For Id!");
                            VivoGsmStateListener.this.mTelephonyManagerService.createForSubscriptionId(subId).listen(VivoGsmStateListener.this.getPhoneStateListener(simSlotIndex), 33);
                            VivoGsmStateListener.this.mIsCreateForSubscriptionId = true;
                            if (VivoGsmStateListener.this.mLastSimSlotIndex[simSlotIndex] != simSlotIndex) {
                                VivoGsmStateListener.this.mLastSimSlotIndex[simSlotIndex] = simSlotIndex;
                            }
                            SElog.d(VivoGsmStateListener.TAG, "mLastSSIndex[" + simSlotIndex + "] = " + VivoGsmStateListener.this.mLastSimSlotIndex[simSlotIndex]);
                        }
                        SElog.e(VivoGsmStateListener.TAG, "subId = " + subId);
                    } catch (Exception e) {
                        SElog.e(VivoGsmStateListener.TAG, "onSubscriptionsChanged, e = " + e);
                    }
                }
                this.cacheSubLength = subListLength;
                SElog.d(VivoGsmStateListener.TAG, "cacheSubLength = " + this.cacheSubLength);
            } else if (subList == null && VivoGsmStateListener.this.mIsCreateForSubscriptionId) {
                for (PhoneStateListener listener : VivoGsmStateListener.this.mPhoneStateListener) {
                    SElog.e(VivoGsmStateListener.TAG, "set Listener to LISTEN_NONE!");
                    VivoGsmStateListener.this.mTelephonyManagerService.listen(listener, 0);
                    this.cacheSubLength = 0;
                }
                VivoGsmStateListener.this.mIsCreateForSubscriptionId = false;
            }
        }
    };

    public VivoGsmStateListener(Context context, VivoSarPowerStateController vivoSarPowerStateController) {
        this.mVivoSarPowerStateController = null;
        this.mTelephonyManagerService = null;
        this.mGsmChannelLow = -1;
        this.mGsmChannelHigh = -1;
        mContext = context;
        this.mGsmChannelLow = AblConfig.getGsmChannelLow();
        this.mGsmChannelHigh = AblConfig.getGsmChannelHigh();
        this.mVivoSarPowerStateController = vivoSarPowerStateController;
        this.mTelephonyManagerService = (TelephonyManager) mContext.getSystemService("phone");
        this.mSubscriptionManager = (SubscriptionManager) mContext.getSystemService(SubscriptionManager.class);
        this.mPhoneCount = this.mTelephonyManagerService.getPhoneCount();
        SElog.d(TAG, "Get mPhone Count = " + this.mPhoneCount + " mGsmChannelLow = " + this.mGsmChannelLow + " mGsmChannelHigh = " + this.mGsmChannelHigh);
        this.mPhoneStateListener = new PhoneStateListener[this.mPhoneCount];
        SubscriptionManager subscriptionManager = this.mSubscriptionManager;
        if (subscriptionManager != null) {
            subscriptionManager.addOnSubscriptionsChangedListener(this.mOnSubscriptionsChangedListener);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public PhoneStateListener getPhoneStateListener(final int phoneId) {
        this.mPhoneStateListener[phoneId] = new PhoneStateListener() { // from class: com.vivo.services.sarpower.VivoGsmStateListener.2
            @Override // android.telephony.PhoneStateListener
            public void onCallStateChanged(int state, String incomingNumber) {
                SElog.d(VivoGsmStateListener.TAG, "onCallStateChanged,state = " + state);
                if (state != 2) {
                    VivoGsmStateListener.this.mIsPhoneInCall[phoneId] = false;
                } else {
                    VivoGsmStateListener.this.mIsPhoneInCall[phoneId] = true;
                }
                if (!VivoGsmStateListener.this.mIsPhoneInCall[phoneId] || !VivoGsmStateListener.this.mIsGsm900[phoneId] || VivoGsmStateListener.this.mIsSendSarState[phoneId]) {
                    if (VivoGsmStateListener.this.mIsSendSarState[phoneId] && !VivoGsmStateListener.this.mIsPhoneInCall[phoneId]) {
                        VivoGsmStateListener.this.NotifySarStateChanged(0, 34);
                        VivoGsmStateListener.this.mIsSendSarState[phoneId] = false;
                        return;
                    }
                    return;
                }
                VivoGsmStateListener.this.NotifySarStateChanged(1, 34);
                VivoGsmStateListener.this.mIsSendSarState[phoneId] = true;
            }

            @Override // android.telephony.PhoneStateListener
            public void onServiceStateChanged(ServiceState serviceState) {
                int rat = serviceState.getVoiceNetworkType();
                int channel = serviceState.getChannelNumber();
                SElog.d(VivoGsmStateListener.TAG, "onServiceStateChanged,rat = " + rat + ",channel = " + channel);
                if (VivoGsmStateListener.this.GsmChannelConfig()) {
                    if (rat == 16 && channel > VivoGsmStateListener.this.mGsmChannelLow && channel < VivoGsmStateListener.this.mGsmChannelHigh) {
                        VivoGsmStateListener.this.mIsGsm900[phoneId] = true;
                    }
                } else if (rat == 16) {
                    VivoGsmStateListener.this.mIsGsm900[phoneId] = true;
                } else {
                    VivoGsmStateListener.this.mIsGsm900[phoneId] = false;
                }
                if (!VivoGsmStateListener.this.mIsPhoneInCall[phoneId] || !VivoGsmStateListener.this.mIsGsm900[phoneId] || VivoGsmStateListener.this.mIsSendSarState[phoneId]) {
                    if (VivoGsmStateListener.this.mIsSendSarState[phoneId] && !VivoGsmStateListener.this.mIsGsm900[phoneId]) {
                        VivoGsmStateListener.this.NotifySarStateChanged(0, 34);
                        VivoGsmStateListener.this.mIsSendSarState[phoneId] = false;
                        return;
                    }
                    return;
                }
                VivoGsmStateListener.this.NotifySarStateChanged(1, 34);
                VivoGsmStateListener.this.mIsSendSarState[phoneId] = true;
            }
        };
        return this.mPhoneStateListener[phoneId];
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean GsmChannelConfig() {
        int i = this.mGsmChannelLow;
        return i >= 0 && this.mGsmChannelHigh > i;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void NotifySarStateChanged(int state, int aim) {
        this.mVivoSarPowerStateController.notifyStateChange(state, aim);
        this.mVivoSarPowerStateController.handleSarMessage(0, 0);
    }
}