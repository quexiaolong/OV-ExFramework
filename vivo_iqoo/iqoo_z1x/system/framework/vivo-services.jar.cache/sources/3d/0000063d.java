package com.vivo.services.configurationManager;

import android.content.ContentValues;
import android.content.Context;
import android.text.TextUtils;
import com.vivo.common.utils.VLog;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import vivo.app.configuration.ContentValuesList;
import vivo.app.configuration.StringList;
import vivo.app.configuration.Switch;

/* loaded from: classes.dex */
public class XmlPullParserHelper {
    private static final String TAG = "ConfigurationManager";

    private static ContentValues getTagAttributesAndValues(XmlPullParser parser) {
        ContentValues cv = new ContentValues();
        try {
            if (parser.getEventType() == 2) {
                int count = parser.getAttributeCount();
                for (int i = 0; i < count; i++) {
                    cv.put(parser.getAttributeName(i), parser.getAttributeValue(i));
                }
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }
        return cv;
    }

    private static ContentValues addItemForContentValuesList(XmlPullParser parser, ContentValuesList cl) {
        ContentValues cv = new ContentValues();
        String itemKey = null;
        String nameString = null;
        try {
            if (parser.getEventType() == 2) {
                int count = parser.getAttributeCount();
                for (int i = 0; i < count; i++) {
                    String key = parser.getAttributeName(i);
                    String value = parser.getAttributeValue(i);
                    if ("name".equals(key)) {
                        itemKey = value;
                        nameString = value;
                    } else if ("value".equals(key) && nameString != null) {
                        cv.put(nameString, value);
                    } else {
                        cv.put(key, value);
                    }
                }
                String nextString = parser.nextText();
                if (nextString != null && !TextUtils.isEmpty(nextString) && nameString != null) {
                    cv.put(nameString, nextString);
                }
                if (itemKey != null) {
                    cl.addItem(itemKey, cv);
                } else {
                    VLog.e(TAG, "Illegal item format: " + cv + ", name identifier must be included!");
                }
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return cv;
    }

    private static boolean isSpecialTag(XmlPullParser parser, String[] tags, ContentValues cv) {
        for (String tag : tags) {
            if (!cv.containsKey(tag)) {
                return false;
            }
        }
        return true;
    }

    public static List<Switch> getSwitchListFromFile(String path, Context context) {
        File file = new File(path);
        if (file.exists() && file.isFile() && file.canRead()) {
            String fileContent = null;
            if (file.length() > 512000) {
                VLog.e(TAG, "dec switch config file failed,more than 500kb");
                return null;
            }
            try {
                if (DecryptUtils.isAbeSupportDecryptV2()) {
                    fileContent = DecryptUtils.decryptFile(path, context);
                } else {
                    fileContent = Utils.decryptFile(path);
                }
            } catch (Exception e) {
                e.printStackTrace();
                VLog.e(TAG, "dec switch config file failed");
            }
            if (TextUtils.isEmpty(fileContent)) {
                return null;
            }
            try {
                ByteArrayInputStream fis = new ByteArrayInputStream(fileContent.getBytes());
                List<Switch> parseSwitchListFromInputStream = parseSwitchListFromInputStream(fis, path);
                fis.close();
                return parseSwitchListFromInputStream;
            } catch (FileNotFoundException e2) {
                e2.printStackTrace();
                return null;
            } catch (IOException e3) {
                e3.printStackTrace();
                return null;
            }
        }
        return null;
    }

    public static List<StringList> getStringListFromFile(String path, Context context) {
        File file = new File(path);
        if (file.exists() && file.isFile() && file.canRead()) {
            String fileContent = null;
            if (file.length() > 512000) {
                VLog.e(TAG, "dec stringlist config file failed,more than 500kb");
                return null;
            }
            try {
                if (DecryptUtils.isAbeSupportDecryptV2()) {
                    fileContent = DecryptUtils.decryptFile(path, context);
                } else {
                    fileContent = Utils.decryptFile(path);
                }
            } catch (Exception e) {
                e.printStackTrace();
                VLog.e(TAG, "dec stringlist config file failed");
            }
            if (TextUtils.isEmpty(fileContent)) {
                return null;
            }
            try {
                ByteArrayInputStream fis = new ByteArrayInputStream(fileContent.getBytes());
                List<StringList> parseStringListFromInputString = parseStringListFromInputString(fis, path);
                fis.close();
                return parseStringListFromInputString;
            } catch (FileNotFoundException e2) {
                e2.printStackTrace();
                return null;
            } catch (IOException e3) {
                e3.printStackTrace();
                return null;
            }
        }
        return null;
    }

    public static List<StringList> parseStringListFromInputString(InputStream is, String path) {
        List<StringList> list = new ArrayList<>();
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            InputStreamReader stream = null;
            try {
                try {
                    stream = new InputStreamReader(is);
                    parser.setInput(stream);
                    StringList sl = null;
                    boolean firstFoundRootTag = false;
                    while (parser.getEventType() != 1) {
                        if (parser.getEventType() == 2) {
                            if (!firstFoundRootTag) {
                                firstFoundRootTag = true;
                                if (!"lists".equalsIgnoreCase(parser.getName())) {
                                    VLog.e(TAG, "list " + path + "'s root tag is invalid");
                                    break;
                                }
                            }
                            if ("list".equalsIgnoreCase(parser.getName())) {
                                ContentValues cv = getTagAttributesAndValues(parser);
                                if (isSpecialTag(parser, new String[]{"name"}, cv)) {
                                    sl = new StringList(false);
                                    sl.setConfigFilePath(path);
                                    sl.setName(cv.getAsString("name"));
                                }
                            }
                            if ("item".equalsIgnoreCase(parser.getName())) {
                                ContentValues cv2 = getTagAttributesAndValues(parser);
                                if (sl != null && isSpecialTag(parser, new String[]{"value"}, cv2)) {
                                    String value = cv2.getAsString("value");
                                    sl.addItem(value);
                                }
                            }
                        }
                        if (parser.getEventType() == 3 && "list".equalsIgnoreCase(parser.getName())) {
                            if (ListConvertHelper.isStringListRepeated(list, sl)) {
                                VLog.e(TAG, "list " + sl.getName() + " is repeated in " + path + ", your list is ignored!!!!!!");
                            } else if (sl != null) {
                                list.add(sl);
                            }
                        }
                        parser.next();
                    }
                    try {
                        stream.close();
                    } catch (IOException e) {
                    }
                    return list;
                } catch (Exception e2) {
                    e2.printStackTrace();
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e3) {
                        }
                    }
                    return list;
                }
            } catch (Throwable th) {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e4) {
                    }
                }
                throw th;
            }
        } catch (Exception e5) {
            e5.printStackTrace();
            return null;
        }
    }

    public static List<ContentValuesList> getContentValuesListFromFile(String path, Context context) {
        File file = new File(path);
        if (file.exists() && file.isFile() && file.canRead()) {
            String fileContent = null;
            if (file.length() > 512000) {
                VLog.e(TAG, "dec contentvalue config file failed,more than 500kb");
                return null;
            }
            try {
                if (DecryptUtils.isAbeSupportDecryptV2()) {
                    fileContent = DecryptUtils.decryptFile(path, context);
                } else {
                    fileContent = Utils.decryptFile(path);
                }
            } catch (Exception e) {
                e.printStackTrace();
                VLog.e(TAG, "dec cvlist config file failed");
            }
            if (TextUtils.isEmpty(fileContent)) {
                return null;
            }
            try {
                ByteArrayInputStream fis = new ByteArrayInputStream(fileContent.getBytes());
                List<ContentValuesList> parseContentValuesListFromInputString = parseContentValuesListFromInputString(fis, path);
                fis.close();
                return parseContentValuesListFromInputString;
            } catch (FileNotFoundException e2) {
                e2.printStackTrace();
                return null;
            } catch (IOException e3) {
                e3.printStackTrace();
                return null;
            }
        }
        return null;
    }

    public static List<ContentValuesList> parseContentValuesListFromInputString(InputStream is, String path) {
        List<ContentValuesList> list = new ArrayList<>();
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            InputStreamReader stream = null;
            try {
                try {
                    stream = new InputStreamReader(is);
                    parser.setInput(stream);
                    ContentValuesList cl = null;
                    boolean firstFoundRootTag = false;
                    while (parser.getEventType() != 1) {
                        if (parser.getEventType() == 2) {
                            if (!firstFoundRootTag) {
                                firstFoundRootTag = true;
                                if (!"customlists".equalsIgnoreCase(parser.getName())) {
                                    VLog.e(TAG, "customlist " + path + "'s root tag is invalid");
                                    break;
                                }
                            }
                            if ("clist".equalsIgnoreCase(parser.getName())) {
                                ContentValues cv = getTagAttributesAndValues(parser);
                                if (isSpecialTag(parser, new String[]{"name"}, cv)) {
                                    cl = new ContentValuesList(false);
                                    cl.setConfigFilePath(path);
                                    cl.setName(cv.getAsString("name"));
                                }
                            }
                            if (cl != null && "item".equalsIgnoreCase(parser.getName())) {
                                addItemForContentValuesList(parser, cl);
                            }
                        }
                        if (parser.getEventType() == 3 && "clist".equalsIgnoreCase(parser.getName())) {
                            if (ListConvertHelper.isContentValuesListRepeated(list, cl)) {
                                VLog.e(TAG, "list " + cl.getName() + " is repeated in " + path + ", your list is ignored!!!!!!");
                            } else if (cl != null) {
                                list.add(cl);
                            }
                        }
                        parser.next();
                    }
                    try {
                        stream.close();
                    } catch (IOException e) {
                    }
                    return list;
                } catch (Exception e2) {
                    e2.printStackTrace();
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e3) {
                        }
                    }
                    return list;
                }
            } catch (Throwable th) {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e4) {
                    }
                }
                throw th;
            }
        } catch (Exception e5) {
            e5.printStackTrace();
            return list;
        }
    }

    public static List<Switch> parseSwitchListFromInputStream(InputStream is, String path) {
        List<Switch> list = new ArrayList<>();
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            InputStreamReader stream = null;
            try {
                try {
                    stream = new InputStreamReader(is);
                    parser.setInput(stream);
                    boolean firstFoundRootTag = false;
                    while (parser.getEventType() != 1) {
                        if (parser.getEventType() == 2) {
                            if (!firstFoundRootTag) {
                                firstFoundRootTag = true;
                                if (!"switches".equalsIgnoreCase(parser.getName())) {
                                    VLog.e(TAG, "switch config file " + path + "'s root tag is invalid");
                                    break;
                                }
                            }
                            if ("switch".equalsIgnoreCase(parser.getName())) {
                                ContentValues cv = getTagAttributesAndValues(parser);
                                if (isSpecialTag(parser, new String[]{"name", "value"}, cv)) {
                                    String name = cv.getAsString("name");
                                    String value = cv.getAsString("value");
                                    Switch sw = new Switch(name, "on".equalsIgnoreCase(value), path, false);
                                    if (ListConvertHelper.isSwitchRepeated(list, sw)) {
                                        VLog.e(TAG, "switch " + sw.getName() + " is repeated in " + path + ", your switch is ignored!!!!!!");
                                    } else {
                                        list.add(sw);
                                    }
                                }
                            }
                        }
                        parser.next();
                    }
                    try {
                        stream.close();
                    } catch (IOException e) {
                    }
                    return list;
                } catch (Exception e2) {
                    e2.printStackTrace();
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e3) {
                        }
                    }
                    return list;
                }
            } catch (Throwable th) {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e4) {
                    }
                }
                throw th;
            }
        } catch (Exception e5) {
            e5.printStackTrace();
            return list;
        }
    }

    public static String safeNextText(XmlPullParser parser) {
        String result = null;
        try {
            result = parser.nextText();
            if (parser.getEventType() != 3) {
                parser.nextTag();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}