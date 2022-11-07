package com.vivo.services.vgc.cbs;

import android.util.Xml;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class CbsCarrierSimInfos {
    private static final String ATTR_CARDTYPE = "cardtype";
    private static final String ATTR_CARRIERID = "carrier_id";
    private static final String ATTR_CARRIER_NAME = "carrier_name";
    private static final String ATTR_GID1 = "gid1";
    private static final String ATTR_GID2 = "gid2";
    private static final String ATTR_ICCID = "iccid";
    private static final String ATTR_IMSI = "imsi";
    private static final String ATTR_MAP_KEY = "map_key";
    private static final String ATTR_MCCMNC = "mcc_mnc";
    private static final String ATTR_PROPERTY_NAME = "name";
    private static final String ATTR_RULE = "rule";
    private static final String ATTR_SPN = "spn";
    private static final String TAG = CbsUtils.TAG;
    private static final String TAG_ITEM = "siminfo";
    private static final String TAG_PROPERTY = "property";
    private List<CbsSimInfo> simInfoList = new ArrayList();

    public List<CbsSimInfo> getSimInfoList() {
        return this.simInfoList;
    }

    public boolean loadCarrierSimInfo(File carrier_siminfo) {
        ArrayList<CbsSimInfo> list = getSimInfoFromFile(carrier_siminfo);
        this.simInfoList.addAll(list);
        return !list.isEmpty();
    }

    public ArrayList<CbsSimInfo> getSimInfoFromFile(File carrier_siminfo) {
        FileInputStream str;
        XmlPullParser parser;
        int type;
        ArrayList<CbsSimInfo> replyList = new ArrayList<>();
        if (CbsUtils.DEBUG) {
            VSlog.d(TAG, "CbsSettings  loadCarrierSimInfo");
        }
        if (carrier_siminfo.exists()) {
            FileInputStream str2 = null;
            try {
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                try {
                    str = new FileInputStream(carrier_siminfo);
                    parser = Xml.newPullParser();
                    parser.setInput(str, StandardCharsets.UTF_8.name());
                    while (true) {
                        type = parser.next();
                        if (type == 2 || type == 1) {
                            break;
                        }
                    }
                } catch (Exception e2) {
                    VSlog.wtf(TAG, "Error reading cbs settings", e2);
                    str2.close();
                }
                if (type != 2) {
                    if (CbsUtils.DEBUG) {
                        VSlog.wtf(TAG, "No start tag found in scb config settings");
                    }
                    try {
                        str.close();
                    } catch (IOException e3) {
                        e3.printStackTrace();
                    }
                    return replyList;
                }
                boolean tagIntact = false;
                CbsSimInfo simInfo = new CbsSimInfo();
                for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                    if (eventType != 2) {
                        if (eventType == 3) {
                            if (TAG_ITEM.equals(parser.getName()) && tagIntact) {
                                if (CbsUtils.DEBUG) {
                                    String str3 = TAG;
                                    VSlog.i(str3, "loadCarrierSimInfo simInfo:" + simInfo);
                                }
                                CbsSimInfo tmp = new CbsSimInfo(simInfo);
                                replyList.add(tmp);
                            }
                            simInfo.deinit();
                            tagIntact = false;
                        }
                    } else if (!tagIntact && TAG_ITEM.equals(parser.getName())) {
                        tagIntact = true;
                        simInfo.setCardType(0);
                    } else if (tagIntact && TAG_PROPERTY.equals(parser.getName())) {
                        String name = parser.getAttributeValue(null, ATTR_PROPERTY_NAME);
                        if (name.equals(ATTR_MCCMNC)) {
                            simInfo.setMccMnc(parser.nextText());
                        } else if (name.equals(ATTR_GID1)) {
                            String gid1 = parser.nextText();
                            simInfo.setGid1(gid1);
                        } else if (name.equals(ATTR_GID2)) {
                            String gid2 = parser.nextText();
                            simInfo.setGid2(gid2);
                        } else if (name.equals(ATTR_SPN)) {
                            simInfo.setSpn(parser.nextText());
                        } else if (name.equals(ATTR_IMSI)) {
                            simInfo.setImsi(parser.nextText());
                        } else if (name.equals(ATTR_ICCID)) {
                            simInfo.setIccid(parser.nextText());
                        } else if (name.equals("map_key")) {
                            simInfo.setMapKey(parser.nextText());
                        } else if (name.equals(ATTR_CARDTYPE)) {
                            String cardTypeString = parser.nextText();
                            int cardType = CbsUtils.parseInt(cardTypeString, 0);
                            simInfo.setCardType(cardType);
                        } else if (name.equals(ATTR_RULE)) {
                            String ruleString = parser.nextText();
                            if (ruleString.length() > 2) {
                                Integer rule = Integer.valueOf(CbsUtils.parseInt(ruleString.substring(2), 16, 0));
                                simInfo.setRule(rule.intValue());
                            }
                        } else if (name.equals(ATTR_CARRIERID)) {
                            simInfo.setCarrierId(parser.nextText());
                        } else if (name.equals(ATTR_CARRIER_NAME)) {
                            simInfo.setCarrierName(parser.nextText());
                        }
                    }
                }
                str.close();
                return replyList;
            } catch (Throwable th) {
                try {
                    str2.close();
                } catch (IOException e4) {
                    e4.printStackTrace();
                }
                throw th;
            }
        }
        return replyList;
    }
}