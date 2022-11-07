package com.android.server.am.firewall;

import android.os.FileUtils;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import com.android.server.policy.InputExceptionReport;
import com.vivo.face.common.data.Constants;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoCacheFileMgr {
    private String TAG = VivoFirewall.TAG;

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                VSlog.d(this.TAG, "VivoCacheFileRW closeQuietly", e.fillInStackTrace());
            }
        }
    }

    public boolean readConfigXmlFile(File file, HashMap<String, VivoAppRuleItem> hashmap) {
        int type;
        int i;
        boolean result;
        String str;
        boolean result2;
        int packageDepth;
        int kind;
        boolean result3;
        String packageName;
        String str2;
        int bringupDepth;
        String str3;
        int bringupDepth2;
        String name;
        String str4 = "allowed";
        boolean result4 = true;
        if (file == null || !file.exists() || hashmap == null) {
            return false;
        }
        hashmap.clear();
        FileInputStream str5 = null;
        try {
            try {
                str5 = new FileInputStream(file);
                XmlPullParser parser = Xml.newPullParser();
                String str6 = null;
                parser.setInput(str5, null);
                while (true) {
                    type = parser.next();
                    i = 1;
                    if (type == 2 || type == 1) {
                        break;
                    }
                }
                if (type != 2) {
                    VSlog.w(this.TAG, "No start tag found in " + file.getName());
                    closeQuietly(str5);
                    return false;
                }
                int outerDepth = parser.getDepth();
                String attr = parser.getAttributeValue(null, "on");
                result4 = "true".equals(attr);
                while (true) {
                    try {
                        int type2 = parser.next();
                        if (type2 == i) {
                            result = result4;
                            break;
                        } else if (type2 == 3 && parser.getDepth() <= outerDepth) {
                            result = result4;
                            break;
                        } else {
                            if (type2 != 3 && type2 != 4) {
                                if ("package".equals(parser.getName())) {
                                    String tagName = parser.getAttributeValue(str6, "name");
                                    if (Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK.equals(tagName)) {
                                        XmlUtils.skipCurrentTag(parser);
                                    } else {
                                        VivoAppRuleItem appRuleItem = new VivoAppRuleItem(tagName);
                                        int packageDepth2 = parser.getDepth();
                                        while (true) {
                                            int type3 = parser.next();
                                            if (type3 == 1) {
                                                str = str4;
                                                result2 = result4;
                                                break;
                                            }
                                            if (type3 == 3) {
                                                packageDepth = packageDepth2;
                                                if (parser.getDepth() <= packageDepth) {
                                                    str = str4;
                                                    result2 = result4;
                                                    break;
                                                }
                                            } else {
                                                packageDepth = packageDepth2;
                                            }
                                            if (type3 == 3 || type3 == 4) {
                                                packageDepth2 = packageDepth;
                                                str4 = str4;
                                                result4 = result4;
                                                tagName = tagName;
                                            } else {
                                                String tagName2 = parser.getName();
                                                if ("bringup_pkg".equals(tagName2)) {
                                                    String tagName3 = tagName2;
                                                    String allowBringup = parser.getAttributeValue(null, str4);
                                                    try {
                                                        kind = Integer.parseInt(parser.getAttributeValue(null, "kind"));
                                                    } catch (NumberFormatException e) {
                                                        kind = -1;
                                                    }
                                                    appRuleItem.setRuleType(kind, true);
                                                    if (kind == 0) {
                                                        if ("true".equals(allowBringup)) {
                                                            appRuleItem.setAllowBringup(true);
                                                        } else {
                                                            appRuleItem.setAllowBringup(false);
                                                        }
                                                    }
                                                    int bringupDepth3 = parser.getDepth();
                                                    while (true) {
                                                        result3 = result4;
                                                        int type4 = parser.next();
                                                        packageName = tagName;
                                                        if (type4 == 1) {
                                                            str2 = str4;
                                                            break;
                                                        }
                                                        if (type4 == 3 && parser.getDepth() <= bringupDepth3) {
                                                            str2 = str4;
                                                            break;
                                                        }
                                                        if (type4 == 3) {
                                                            bringupDepth = bringupDepth3;
                                                            str3 = str4;
                                                        } else if (type4 == 4) {
                                                            bringupDepth = bringupDepth3;
                                                            str3 = str4;
                                                        } else {
                                                            String tagName4 = parser.getName();
                                                            if ("item".equals(tagName4)) {
                                                                String name2 = parser.getAttributeValue(null, "name");
                                                                String allowed = parser.getAttributeValue(null, str4);
                                                                if (kind != 0) {
                                                                    bringupDepth2 = bringupDepth3;
                                                                    name = str4;
                                                                    if (kind != -1) {
                                                                        appRuleItem.addToTypeList(kind, name2);
                                                                    }
                                                                } else if ("true".equals(allowed)) {
                                                                    bringupDepth2 = bringupDepth3;
                                                                    name = str4;
                                                                    appRuleItem.putBringupRule(name2, true);
                                                                } else {
                                                                    bringupDepth2 = bringupDepth3;
                                                                    name = str4;
                                                                    appRuleItem.putBringupRule(name2, false);
                                                                }
                                                            } else {
                                                                bringupDepth2 = bringupDepth3;
                                                                name = str4;
                                                            }
                                                            tagName3 = tagName4;
                                                            str4 = name;
                                                            result4 = result3;
                                                            tagName = packageName;
                                                            bringupDepth3 = bringupDepth2;
                                                        }
                                                        str4 = str3;
                                                        result4 = result3;
                                                        tagName = packageName;
                                                        bringupDepth3 = bringupDepth;
                                                    }
                                                    str4 = str2;
                                                    result4 = result3;
                                                    tagName = packageName;
                                                    packageDepth2 = packageDepth;
                                                } else {
                                                    packageDepth2 = packageDepth;
                                                }
                                            }
                                        }
                                        try {
                                            hashmap.put(appRuleItem.getPackageName(), appRuleItem);
                                        } catch (Exception e2) {
                                            e = e2;
                                            result4 = result2;
                                            e.printStackTrace();
                                            closeQuietly(str5);
                                            return result4;
                                        } catch (Throwable th) {
                                            th = th;
                                            closeQuietly(str5);
                                            throw th;
                                        }
                                    }
                                } else {
                                    str = str4;
                                    result2 = result4;
                                }
                                str4 = str;
                                result4 = result2;
                                str6 = null;
                                i = 1;
                            }
                            str4 = str4;
                            result4 = result4;
                            str6 = null;
                            i = 1;
                        }
                    } catch (Exception e3) {
                        e = e3;
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
                closeQuietly(str5);
                return result;
            } catch (Exception e4) {
                e = e4;
            }
        } catch (Throwable th3) {
            th = th3;
        }
    }

    public void writeConfigXmlFile(HashMap<String, VivoAppRuleItem> hashmap, File file, boolean on) {
        String str;
        if (file == null || hashmap == null) {
            return;
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        FileOutputStream fstr = null;
        BufferedOutputStream str2 = null;
        try {
            try {
                try {
                    fstr = new FileOutputStream(file);
                    str2 = new BufferedOutputStream(fstr);
                    FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                    fastXmlSerializer.setOutput(str2, "utf-8");
                    String str3 = null;
                    fastXmlSerializer.startDocument(null, true);
                    fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                    fastXmlSerializer.startTag(null, "app_rules");
                    if (on) {
                        fastXmlSerializer.attribute(null, "on", "true");
                    } else {
                        fastXmlSerializer.attribute(null, "on", "false");
                    }
                    Iterator<Map.Entry<String, VivoAppRuleItem>> iterator = hashmap.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<String, VivoAppRuleItem> entry = iterator.next();
                        String bringupPackage = entry.getKey();
                        VivoAppRuleItem value = entry.getValue();
                        if (value != null) {
                            fastXmlSerializer.startTag(str3, "package");
                            fastXmlSerializer.attribute(str3, "name", bringupPackage);
                            Iterator<Map.Entry<String, VivoAppRuleItem>> iterator2 = iterator;
                            if (value.hasType(0)) {
                                fastXmlSerializer.startTag(null, "bringup_pkg");
                                if (value.isAllowBringup()) {
                                    fastXmlSerializer.attribute(null, "allowed", "true");
                                } else {
                                    fastXmlSerializer.attribute(null, "allowed", "false");
                                }
                                fastXmlSerializer.attribute(null, "kind", "0");
                                HashMap<String, Boolean> subHashmap = value.getBringupRule();
                                for (Map.Entry<String, Boolean> subEntry : subHashmap.entrySet()) {
                                    String bringupPackage2 = subEntry.getKey();
                                    Boolean result = subEntry.getValue();
                                    HashMap<String, Boolean> subHashmap2 = subHashmap;
                                    fastXmlSerializer.startTag(null, "item");
                                    String key = bringupPackage;
                                    fastXmlSerializer.attribute(null, "name", bringupPackage2);
                                    if (result.booleanValue()) {
                                        fastXmlSerializer.attribute(null, "allowed", "true");
                                        str = null;
                                    } else {
                                        str = null;
                                        fastXmlSerializer.attribute(null, "allowed", "false");
                                    }
                                    fastXmlSerializer.endTag(str, "item");
                                    subHashmap = subHashmap2;
                                    bringupPackage = key;
                                }
                                fastXmlSerializer.endTag(null, "bringup_pkg");
                            } else if (value.hasType(1)) {
                                fastXmlSerializer.startTag(null, "bringup_pkg");
                                if (value.isAllowBringup()) {
                                    fastXmlSerializer.attribute(null, "allowed", "true");
                                } else {
                                    fastXmlSerializer.attribute(null, "allowed", "false");
                                }
                                fastXmlSerializer.attribute(null, "kind", "1");
                                ArrayList<String> list = value.getTypeList(1);
                                if (list != null) {
                                    Iterator<String> it = list.iterator();
                                    while (it.hasNext()) {
                                        String item = it.next();
                                        fastXmlSerializer.startTag(null, "item");
                                        fastXmlSerializer.attribute(null, "name", item);
                                        fastXmlSerializer.attribute(null, "allowed", "false");
                                        fastXmlSerializer.endTag(null, "item");
                                        list = list;
                                    }
                                }
                                fastXmlSerializer.endTag(null, "bringup_pkg");
                            } else if (value.hasType(2)) {
                                fastXmlSerializer.startTag(null, "bringup_pkg");
                                if (value.isAllowBringup()) {
                                    fastXmlSerializer.attribute(null, "allowed", "true");
                                } else {
                                    fastXmlSerializer.attribute(null, "allowed", "false");
                                }
                                fastXmlSerializer.attribute(null, "kind", "2");
                                ArrayList<String> list2 = value.getTypeList(2);
                                if (list2 != null) {
                                    Iterator<String> it2 = list2.iterator();
                                    while (it2.hasNext()) {
                                        String item2 = it2.next();
                                        fastXmlSerializer.startTag(null, "item");
                                        fastXmlSerializer.attribute(null, "name", item2);
                                        fastXmlSerializer.attribute(null, "allowed", "true");
                                        fastXmlSerializer.endTag(null, "item");
                                        list2 = list2;
                                    }
                                }
                                fastXmlSerializer.endTag(null, "bringup_pkg");
                            }
                            if (value.hasType(3)) {
                                fastXmlSerializer.startTag(null, "bringup_pkg");
                                if (value.isAllowBringup()) {
                                    fastXmlSerializer.attribute(null, "allowed", "true");
                                } else {
                                    fastXmlSerializer.attribute(null, "allowed", "false");
                                }
                                fastXmlSerializer.attribute(null, "kind", InputExceptionReport.LEVEL_MEDIUM);
                                ArrayList<String> list3 = value.getTypeList(3);
                                if (list3 != null) {
                                    Iterator<String> it3 = list3.iterator();
                                    while (it3.hasNext()) {
                                        String item3 = it3.next();
                                        fastXmlSerializer.startTag(null, "item");
                                        fastXmlSerializer.attribute(null, "name", item3);
                                        fastXmlSerializer.attribute(null, "allowed", "false");
                                        fastXmlSerializer.endTag(null, "item");
                                    }
                                }
                                fastXmlSerializer.endTag(null, "bringup_pkg");
                            } else if (value.hasType(4)) {
                                fastXmlSerializer.startTag(null, "bringup_pkg");
                                if (value.isAllowBringup()) {
                                    fastXmlSerializer.attribute(null, "allowed", "true");
                                } else {
                                    fastXmlSerializer.attribute(null, "allowed", "false");
                                }
                                fastXmlSerializer.attribute(null, "kind", "4");
                                ArrayList<String> list4 = value.getTypeList(4);
                                if (list4 != null) {
                                    Iterator<String> it4 = list4.iterator();
                                    while (it4.hasNext()) {
                                        String item4 = it4.next();
                                        fastXmlSerializer.startTag(null, "item");
                                        fastXmlSerializer.attribute(null, "name", item4);
                                        fastXmlSerializer.attribute(null, "allowed", "true");
                                        fastXmlSerializer.endTag(null, "item");
                                    }
                                }
                                fastXmlSerializer.endTag(null, "bringup_pkg");
                            }
                            fastXmlSerializer.endTag(null, "package");
                            iterator = iterator2;
                            str3 = null;
                        }
                    }
                    fastXmlSerializer.endTag(null, "app_rules");
                    fastXmlSerializer.endDocument();
                    str2.flush();
                    FileUtils.sync(fstr);
                    str2.close();
                } catch (Throwable th) {
                    if (str2 != null) {
                        try {
                            str2.flush();
                            if (fstr != null) {
                                FileUtils.sync(fstr);
                            }
                            str2.close();
                        } catch (IOException e2) {
                            e2.printStackTrace();
                        }
                    }
                    throw th;
                }
            } catch (Exception e3) {
                e3.printStackTrace();
                if (str2 != null) {
                    str2.flush();
                    if (fstr != null) {
                        FileUtils.sync(fstr);
                    }
                    str2.close();
                }
            }
        } catch (IOException e4) {
            e4.printStackTrace();
        }
    }
}