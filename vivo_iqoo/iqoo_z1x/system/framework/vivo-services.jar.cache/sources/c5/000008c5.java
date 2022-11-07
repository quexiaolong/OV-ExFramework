package com.vivo.services.vgc.cbs.carrier;

import android.app.ActivityThread;
import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.vivo.face.common.data.Constants;
import com.vivo.framework.vgc.VivoCbsManager;
import com.vivo.services.vgc.cbs.CbsSimInfo;
import java.util.List;

/* loaded from: classes.dex */
public class GeneralManager extends CbsCarrierManager {
    public GeneralManager(List<CbsSimInfo> simInfoList) {
        super(simInfoList);
    }

    @Override // com.vivo.services.vgc.cbs.carrier.CbsCarrierManager
    public CbsSimInfo getMapSimInfo(CbsSimInfo matchInfo) {
        return findMatchPattern(matchInfo);
    }

    @Override // com.vivo.services.vgc.cbs.carrier.CbsCarrierManager
    public int getSimCardFlag(CbsSimInfo matchInfo) {
        Context context;
        int flag = 0;
        CbsSimInfo pattern = findMatchPattern(matchInfo);
        try {
            Context context2 = ActivityThread.currentApplication();
            if (context2 != null) {
                SubscriptionManager subscriptionManager = (SubscriptionManager) context2.getSystemService("telephony_subscription_service");
                TelephonyManager telephonyManager = (TelephonyManager) context2.getSystemService("phone");
                List<SubscriptionInfo> subscriptionInfos = subscriptionManager.getActiveSubscriptionInfoList();
                if (subscriptionInfos != null) {
                    for (SubscriptionInfo info : subscriptionInfos) {
                        int subId = info.getSubscriptionId();
                        int phoneId = SubscriptionManager.getPhoneId(subId);
                        int cardIdx = phoneId + 1;
                        String iccid = telephonyManager.getSimSerialNumber(subId);
                        String imsi = telephonyManager.getSubscriberId(subId);
                        String mccmnc = VivoCbsManager.getMccMncbyCard(cardIdx);
                        String gid1 = VivoCbsManager.getGid1byCard(cardIdx);
                        String spn = VivoCbsManager.getSpnbyCard(cardIdx);
                        CbsSimInfo simInfo = new CbsSimInfo(mccmnc, gid1, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, spn, iccid, imsi);
                        SubscriptionManager subscriptionManager2 = subscriptionManager;
                        CbsSimInfo currSimCardPattern = findMatchPattern(simInfo);
                        if (TextUtils.isEmpty(currSimCardPattern.getMapKey())) {
                            context = context2;
                        } else {
                            context = context2;
                            if (currSimCardPattern.getMapKey().equals(pattern.getMapKey())) {
                                if (cardIdx == 1) {
                                    flag |= 1;
                                } else if (cardIdx == 2) {
                                    flag |= 16;
                                }
                            }
                        }
                        subscriptionManager = subscriptionManager2;
                        context2 = context;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    private CbsSimInfo findMatchPattern(CbsSimInfo dst) {
        CbsSimInfo result = new CbsSimInfo();
        if (dst == null) {
            return result;
        }
        if (this.mSimInfoList.size() == 0) {
            return result;
        }
        for (int i = 0; i < this.mSimInfoList.size(); i++) {
            if (CbsSimInfo.match(this.mSimInfoList.get(i), dst) || dst.equals(this.mSimInfoList.get(i))) {
                return this.mSimInfoList.get(i);
            }
        }
        return result;
    }
}