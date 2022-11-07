package com.android.server.devicepolicy.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

/* loaded from: classes.dex */
public final class VivoEmmInfo {
    private String mEmmPackageName;
    private ArrayList<String> mEmmPermissions;
    private String mEmmShortName;
    private ArrayList<String> mRelatedPkgs;

    public void setVivoEmmInfo(String customShortName, String packageName, ArrayList<String> pkgs, ArrayList<String> permissions) {
        this.mEmmShortName = customShortName;
        this.mEmmPackageName = packageName;
        this.mRelatedPkgs = pkgs;
        this.mEmmPermissions = permissions;
    }

    public String getEmmPackageName() {
        return this.mEmmPackageName;
    }

    public String getEmmShortName() {
        return this.mEmmShortName;
    }

    public ArrayList<String> getRelatedPkgs() {
        return this.mRelatedPkgs;
    }

    public ArrayList<String> getEmmPermissions() {
        return this.mEmmPermissions;
    }

    public void writeInfoToXml(XmlSerializer out) throws IllegalArgumentException, IllegalStateException, IOException {
        out.startTag(null, "emm");
        String str = this.mEmmPackageName;
        if (str != null) {
            out.attribute(null, "packageName", str);
            out.attribute(null, "shortName", this.mEmmShortName);
            ArrayList<String> arrayList = this.mRelatedPkgs;
            if (arrayList != null && !arrayList.isEmpty()) {
                out.startTag(null, "emmRelatedPkgs");
                Iterator<String> it = this.mRelatedPkgs.iterator();
                while (it.hasNext()) {
                    String s = it.next();
                    out.startTag(null, "item");
                    out.attribute(null, "value", s);
                    out.endTag(null, "item");
                }
                out.endTag(null, "emmRelatedPkgs");
            }
            ArrayList<String> arrayList2 = this.mEmmPermissions;
            if (arrayList2 != null && !arrayList2.isEmpty()) {
                out.startTag(null, "emmPermissions");
                Iterator<String> it2 = this.mEmmPermissions.iterator();
                while (it2.hasNext()) {
                    String s2 = it2.next();
                    out.startTag(null, "item");
                    out.attribute(null, "value", s2);
                    out.endTag(null, "item");
                }
                out.endTag(null, "emmPermissions");
            }
        }
        out.endTag(null, "emm");
    }

    public void readInfoFromXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        this.mEmmPackageName = parser.getAttributeValue(null, "packageName");
        this.mEmmShortName = parser.getAttributeValue(null, "shortName");
        while (true) {
            int outerType = parser.next();
            if (outerType != 1) {
                if (outerType != 3 || parser.getDepth() > outerDepth) {
                    if (outerType != 3 && outerType != 4) {
                        String outerTag = parser.getName();
                        if ("emmRelatedPkgs".equals(outerTag)) {
                            this.mRelatedPkgs = readListFromXml(parser);
                        } else if ("emmPermissions".equals(outerTag)) {
                            this.mEmmPermissions = readListFromXml(parser);
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    ArrayList<String> readListFromXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        String str;
        ArrayList<String> result = new ArrayList<>();
        int outerDepth = parser.getDepth();
        while (true) {
            int outerType = parser.next();
            if (outerType == 1 || (outerType == 3 && parser.getDepth() <= outerDepth)) {
                break;
            } else if (outerType != 3 && outerType != 4) {
                String outerTag = parser.getName();
                if ("item".equals(outerTag) && (str = parser.getAttributeValue(null, "value")) != null) {
                    result.add(str);
                }
            }
        }
        return result;
    }
}