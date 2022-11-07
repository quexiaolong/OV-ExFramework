package com.vivo.services.vgc.cbs;

import com.vivo.face.common.data.Constants;
import java.util.Objects;
import java.util.regex.Pattern;

/* loaded from: classes.dex */
public class CbsSimInfo {
    public static final int SIM_BIT_FLAG_GID1 = 16;
    public static final int SIM_BIT_FLAG_GID2 = 32;
    public static final int SIM_BIT_FLAG_ICCID = 8;
    public static final int SIM_BIT_FLAG_IMSI = 4;
    public static final int SIM_BIT_FLAG_MCCMNC = 1;
    public static final int SIM_BIT_FLAG_SPN = 2;
    private int mCardType;
    private String mCarrierId;
    private String mCarrierName;
    private int mCheckRule;
    private String mGid1;
    private String mGid2;
    private String mIccid;
    private String mImsi;
    private String mMapKey;
    private String mMccMnc;
    private int mRule;
    private String mSpn;
    public static int CARDTYPE_NO_CARD = -1;
    public static int CARDTYPE_OPENMARKET = 0;
    public static int CARDTYPE_MAIN_OP = 1;
    public static int CARDTYPE_SUB_OP = 2;

    public CbsSimInfo(String mMccMnc, String mGid1, String mSpn) {
        this(mMccMnc, mGid1, null, mSpn, null, null);
    }

    public CbsSimInfo() {
        this.mMccMnc = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mGid1 = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mGid2 = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mSpn = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mIccid = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mImsi = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mCarrierId = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mCarrierName = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mMapKey = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mCardType = CARDTYPE_NO_CARD;
        this.mRule = 0;
        this.mCheckRule = 0;
    }

    public CbsSimInfo(CbsSimInfo srcInfo) {
        this.mMccMnc = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mGid1 = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mGid2 = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mSpn = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mIccid = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mImsi = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mCarrierId = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mCarrierName = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mMapKey = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mCardType = CARDTYPE_NO_CARD;
        this.mRule = 0;
        this.mCheckRule = 0;
        this.mMccMnc = srcInfo.mMccMnc;
        this.mGid1 = srcInfo.mGid1;
        this.mGid2 = srcInfo.mGid2;
        this.mSpn = srcInfo.mSpn;
        this.mIccid = srcInfo.mIccid;
        this.mImsi = srcInfo.mImsi;
        this.mMapKey = srcInfo.mMapKey;
        this.mCardType = srcInfo.mCardType;
        this.mCarrierId = srcInfo.mCarrierId;
        this.mCarrierName = srcInfo.mCarrierName;
        this.mCheckRule = srcInfo.mCheckRule;
        this.mRule = srcInfo.mRule;
    }

    public CbsSimInfo(String mMccMnc, String mGid1, String gid2, String mSpn, String mIccid, String mImsi) {
        this.mMccMnc = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mGid1 = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mGid2 = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mSpn = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mIccid = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mImsi = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mCarrierId = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mCarrierName = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mMapKey = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mCardType = CARDTYPE_NO_CARD;
        this.mRule = 0;
        this.mCheckRule = 0;
        this.mMccMnc = mMccMnc;
        this.mGid1 = mGid1;
        this.mGid2 = gid2;
        this.mSpn = mSpn;
        this.mIccid = mIccid;
        this.mImsi = mImsi;
    }

    public String getMccMnc() {
        return this.mMccMnc;
    }

    public void setMccMnc(String mMccMnc) {
        this.mCheckRule |= 1;
        this.mMccMnc = mMccMnc;
    }

    public String getGid1() {
        return this.mGid1;
    }

    public void setGid1(String mGid1) {
        this.mCheckRule |= 16;
        this.mGid1 = mGid1;
    }

    public String getGid2() {
        return this.mGid2;
    }

    public void setGid2(String gid2) {
        this.mCheckRule |= 32;
        this.mGid2 = gid2;
    }

    public String getSpn() {
        return this.mSpn;
    }

    public void setSpn(String mSpn) {
        this.mCheckRule |= 2;
        this.mSpn = mSpn;
    }

    public String getIccid() {
        return this.mIccid;
    }

    public void setIccid(String mIccid) {
        this.mCheckRule |= 8;
        this.mIccid = mIccid;
    }

    public String getImsi() {
        return this.mImsi;
    }

    public void setImsi(String mImsi) {
        this.mCheckRule |= 4;
        this.mImsi = mImsi;
    }

    public String getMapKey() {
        return this.mMapKey;
    }

    public void setMapKey(String mMapKey) {
        this.mMapKey = mMapKey;
    }

    public int getCardType() {
        return this.mCardType;
    }

    public void setCardType(int cardType) {
        this.mCardType = cardType;
    }

    public int getRule() {
        return this.mRule;
    }

    public void setRule(int mRule) {
        this.mRule = mRule;
    }

    public String getCarrierId() {
        return this.mCarrierId;
    }

    public void setCarrierId(String carrier_id) {
        this.mCarrierId = carrier_id;
    }

    public String getCarrierName() {
        return this.mCarrierName;
    }

    public void setCarrierName(String carrier_name) {
        this.mCarrierName = carrier_name;
    }

    public int getCheckRule() {
        return this.mCheckRule;
    }

    public void deinit() {
        this.mMccMnc = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mGid1 = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mGid2 = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mSpn = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mIccid = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mImsi = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mMapKey = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mCardType = -1;
        this.mCarrierId = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mCarrierName = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        this.mCheckRule = 0;
        this.mRule = 0;
    }

    public String toString() {
        return "CbsSimInfo{mMccMnc='" + this.mMccMnc + "', mGid1='" + this.mGid1 + "', mGid2='" + this.mGid2 + "', mSpn='" + this.mSpn + "', mMapKey='" + this.mMapKey + "', mCardType='" + this.mCardType + "', mCarrierId=" + this.mCarrierId + ", mCarrierName=" + this.mCarrierName + ", mRule=0x" + Integer.toHexString(this.mRule) + ", mCheckRule=0x" + Integer.toHexString(this.mCheckRule) + '}';
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CbsSimInfo simInfo = (CbsSimInfo) o;
        if (infoEquals(this.mMccMnc, simInfo.mMccMnc) && infoEquals(this.mGid1, simInfo.mGid1) && infoEquals(this.mGid2, simInfo.mGid2) && infoEquals(this.mSpn, simInfo.mSpn) && infoEquals(this.mIccid, simInfo.mIccid) && infoEquals(this.mImsi, simInfo.mImsi)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(this.mMccMnc, this.mGid1, this.mGid2, this.mSpn, this.mIccid, this.mImsi);
    }

    private static boolean infoEquals(String key1, String key2) {
        if (key1 == null && key2 == null) {
            return true;
        }
        if (key1 == null || key2 == null) {
            return false;
        }
        return key1.equals(key2);
    }

    public static boolean match(CbsSimInfo patternObj, CbsSimInfo info) {
        if (patternObj == null || info == null) {
            return false;
        }
        int patternRule = patternObj.getRule();
        int matchRule = 0;
        if (patternRule == 0) {
            return false;
        }
        for (int i = 0; i < 32; i++) {
            int i2 = (1 << i) & patternRule;
            if (i2 != 1) {
                if (i2 != 2) {
                    if (i2 != 4) {
                        if (i2 != 8) {
                            if (i2 == 16) {
                                if (Pattern.matches(patternObj.getGid1(), info.getGid1())) {
                                    matchRule |= 16;
                                }
                            } else if (i2 == 32 && Pattern.matches(patternObj.getGid2(), info.getGid2())) {
                                matchRule |= 32;
                            }
                        } else if (Pattern.matches(patternObj.getIccid(), info.getIccid())) {
                            matchRule |= 8;
                        }
                    } else if (Pattern.matches(patternObj.getImsi(), info.getImsi())) {
                        matchRule |= 4;
                    }
                } else if (Pattern.matches(patternObj.getSpn(), info.getSpn())) {
                    matchRule |= 2;
                }
            } else if (Pattern.matches(patternObj.getMccMnc(), info.getMccMnc())) {
                matchRule |= 1;
            }
        }
        return matchRule == patternRule;
    }
}