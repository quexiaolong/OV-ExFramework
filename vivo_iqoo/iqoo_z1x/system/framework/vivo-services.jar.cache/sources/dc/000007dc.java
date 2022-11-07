package com.vivo.services.sarpower;

import com.vivo.face.common.data.Constants;
import com.vivo.sensor.autobrightness.utils.SElog;
import java.util.ArrayList;
import java.util.Arrays;

/* loaded from: classes.dex */
public class ConfigList2 extends ConfigList {
    public static final int COUNTRY_MATCH_ALL = 3;
    public static final int COUNTRY_MATCH_EQUAL = 1;
    public static final int COUNTRY_MATCH_EXCLUDE = 2;
    public static final int PROJECT_MATCH_EQUAL = 1;
    public static final int PROJECT_MATCH_START_WITH = 2;
    public static final int PURPOSE_ALL = 7;
    public static final int PURPOSE_CAMERA = 4;
    public static final int PURPOSE_FACTORY = 2;
    public static final int PURPOSE_NORMAL = 1;
    private static final String TAG = "ConfigList2";
    public ArrayList<String> mBodyList;
    public ArrayList<String> mC2KList;
    public String mConfigName;
    public String[] mCountryCodeList;
    public int mCountryCodeMatch;
    public ArrayList<String> mHeadList;
    public int mProjectMatch;
    public int mPurpose;
    public ArrayList<String> mWhiteBodyList;
    public ArrayList<String> mWhiteC2KList;
    public ArrayList<String> mWhiteHeadList;

    public ConfigList2(String model) {
        super(model);
        this.mConfigName = null;
        this.mProjectMatch = 1;
        this.mCountryCodeMatch = 1;
        this.mPurpose = 0;
        this.mCountryCodeList = new String[0];
        this.mHeadList = new ArrayList<>();
        this.mBodyList = new ArrayList<>();
        this.mC2KList = new ArrayList<>();
        this.mWhiteHeadList = new ArrayList<>();
        this.mWhiteBodyList = new ArrayList<>();
        this.mWhiteC2KList = new ArrayList<>();
    }

    public void toArray() {
        if (this.mHeadList.size() > 0) {
            ArrayList<String> arrayList = this.mHeadList;
            this.commandsHead = (String[]) arrayList.toArray(new String[arrayList.size()]);
        } else {
            this.commandsHead = new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK};
        }
        if (this.mBodyList.size() > 0) {
            ArrayList<String> arrayList2 = this.mBodyList;
            this.commandsBody = (String[]) arrayList2.toArray(new String[arrayList2.size()]);
        } else {
            this.commandsBody = new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK};
        }
        if (this.mC2KList.size() > 0) {
            ArrayList<String> arrayList3 = this.mC2KList;
            this.commandsOnC2K = (String[]) arrayList3.toArray(new String[arrayList3.size()]);
        } else {
            this.commandsOnC2K = new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK};
        }
        if (this.mWhiteHeadList.size() > 0) {
            ArrayList<String> arrayList4 = this.mWhiteHeadList;
            this.wcommandsHead = (String[]) arrayList4.toArray(new String[arrayList4.size()]);
        } else {
            this.wcommandsHead = new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK};
        }
        if (this.mWhiteBodyList.size() > 0) {
            ArrayList<String> arrayList5 = this.mWhiteBodyList;
            this.wcommandsBody = (String[]) arrayList5.toArray(new String[arrayList5.size()]);
        } else {
            this.wcommandsBody = new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK};
        }
        if (this.mWhiteC2KList.size() > 0) {
            ArrayList<String> arrayList6 = this.mWhiteC2KList;
            this.wcommandsOnC2K = (String[]) arrayList6.toArray(new String[arrayList6.size()]);
            return;
        }
        this.wcommandsOnC2K = new String[]{Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK};
    }

    public void copyFrom(ConfigList2 other) {
        if (other == null) {
            SElog.d(TAG, "copyFrom other=null.");
            return;
        }
        this.mHeadList = new ArrayList<>(other.mHeadList);
        this.mBodyList = new ArrayList<>(other.mBodyList);
        this.mC2KList = new ArrayList<>(other.mC2KList);
        this.mWhiteHeadList = new ArrayList<>(other.mWhiteHeadList);
        this.mWhiteBodyList = new ArrayList<>(other.mWhiteBodyList);
        this.mWhiteC2KList = new ArrayList<>(other.mWhiteC2KList);
    }

    @Override // com.vivo.services.sarpower.ConfigList
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("model:" + this.model);
        sb.append(" projectMatch:" + this.mProjectMatch);
        sb.append(" purpose:" + this.mPurpose);
        sb.append(" country:" + Arrays.toString(this.mCountryCodeList));
        sb.append(" countryMatch:" + this.mCountryCodeMatch);
        sb.append("\nHead:" + Arrays.toString(this.commandsHead));
        sb.append("\nBody:" + Arrays.toString(this.commandsBody));
        sb.append("\nC2K:" + Arrays.toString(this.commandsOnC2K));
        sb.append("\nwHead:" + Arrays.toString(this.wcommandsHead));
        sb.append("\nwBody:" + Arrays.toString(this.wcommandsBody));
        sb.append("\nwC2K:" + Arrays.toString(this.wcommandsOnC2K));
        sb.append("\nwResetGSM:" + this.resetGSM);
        sb.append("\nwResetC2K:" + this.resetC2K);
        return sb.toString();
    }
}