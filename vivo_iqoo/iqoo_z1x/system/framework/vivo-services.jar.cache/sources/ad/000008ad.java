package com.vivo.services.vgc;

import android.content.ContentValues;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.Log;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

/* loaded from: classes.dex */
public class VgcXmlParserHelper {
    private static final String BOOL_TAG = "bool";
    private static final String CUST_LIST_TAG = "custlist";
    private static final String INT_TAG = "int";
    private static final String ITEM_TAG = "item";
    private static final String LIST_TAG = "list";
    private static final String NAME_TAG = "name";
    private static final String PATH_TAG = "path";
    private static final String ROOT_TAG = "config";
    private static final String STRING_TAG = "string";
    private static final String TAG = "VGC";
    private static final String VALUE_TAG = "value";
    private static final boolean DEBUG = VgcUtils.DEBUG;
    private static final String VIVO_PATH = VgcUtils.VGC_VIVO_ROOT;
    private static final String REGION_PATH = VgcUtils.VGC_REGION_ROOT;
    private static final String CARRIER_PATH = VgcUtils.VGC_CARRIER_ROOT;
    private static final ArrayMap<String, ContentValues> mCustListMap = new ArrayMap<>();
    private static final ArrayMap<String, ArrayList<String>> mListMap = new ArrayMap<>();
    private static final Map<String, String> mBoolMap = new HashMap();
    private static final Map<String, String> mStringMap = new HashMap();
    private static final Map<String, String> mIntMap = new HashMap();
    private static final Map<String, String> mFilePathMap = new HashMap();
    private static final Object sCustListMapLock = new Object();
    private static final Object sListMapLock = new Object();
    private static final Object sBoolMapLock = new Object();
    private static final Object sStringMapLock = new Object();
    private static final Object sIntMapLock = new Object();
    private static final Object sFilePathMapLock = new Object();

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

    private static boolean isSpecialTag(XmlPullParser parser, String[] tags, ContentValues cv) {
        for (String tag : tags) {
            if (!cv.containsKey(tag)) {
                VLog.d("VGC", "isSpecialTag tag: " + tag + " error ! ");
                return false;
            }
        }
        return true;
    }

    public static String getString(String name, String defaultStr) {
        synchronized (sStringMapLock) {
            if (mStringMap != null && name != null && mStringMap.containsKey(name)) {
                if (DEBUG) {
                    VLog.d("VGC", "getString name: " + name + " value: " + mStringMap.get(name));
                }
                return mStringMap.get(name);
            }
            if (DEBUG) {
                VLog.d("VGC", "getString name: " + name + " defaultStr: " + defaultStr);
            }
            return defaultStr;
        }
    }

    public static boolean getBool(String name, boolean defaultBool) {
        synchronized (sBoolMapLock) {
            if (mBoolMap != null && name != null && mBoolMap.containsKey(name)) {
                if (mBoolMap.get(name).equalsIgnoreCase("true")) {
                    if (DEBUG) {
                        VLog.d("VGC", "getBool name: " + name + " value: true");
                    }
                    return true;
                }
                if (DEBUG) {
                    VLog.d("VGC", "getBool name: " + name + " value:  false");
                }
                return false;
            }
            if (DEBUG) {
                VLog.d("VGC", "getBool name: " + name + " defaultBool: " + defaultBool);
            }
            return defaultBool;
        }
    }

    public static int getInt(String name, int defaultValue) {
        synchronized (sIntMapLock) {
            if (mIntMap != null && name != null && mIntMap.containsKey(name)) {
                int tempValue = -1000;
                try {
                    tempValue = Integer.parseInt(mIntMap.get(name));
                    if (DEBUG) {
                        VLog.d("VGC", "getInt name: " + name + " value: " + tempValue);
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                return tempValue;
            }
            if (DEBUG) {
                VLog.d("VGC", "getInt name: " + name + " defaultValue: " + defaultValue);
            }
            return defaultValue;
        }
    }

    public static List<String> getStringList(String name, List<String> defaultList) {
        synchronized (sListMapLock) {
            if (!mListMap.isEmpty() && name != null) {
                Set<Map.Entry<String, ArrayList<String>>> set = mListMap.entrySet();
                for (Map.Entry<String, ArrayList<String>> entry : set) {
                    if (name.equalsIgnoreCase(entry.getKey())) {
                        if (DEBUG) {
                            VLog.d("VGC", "getStringList name: " + name + " List: " + entry.getValue());
                        }
                        return entry.getValue();
                    }
                }
            }
            if (DEBUG) {
                VLog.d("VGC", "getStringList name: " + name + " defaultList: " + defaultList);
            }
            return defaultList;
        }
    }

    public static ContentValues getContentValues(String name, ContentValues mContentValues) {
        synchronized (sCustListMapLock) {
            if (!mCustListMap.isEmpty() && name != null) {
                Set<Map.Entry<String, ContentValues>> set = mCustListMap.entrySet();
                for (Map.Entry<String, ContentValues> entry : set) {
                    if (name.equalsIgnoreCase(entry.getKey())) {
                        if (DEBUG) {
                            VLog.d("VGC", "getContentValues exit name: " + name);
                        }
                        return entry.getValue();
                    }
                }
            }
            if (DEBUG) {
                VLog.d("VGC", "getContentValues name: " + name);
            }
            return mContentValues;
        }
    }

    public static String getFile(String name, String defaultStr) {
        synchronized (sFilePathMapLock) {
            if (mFilePathMap != null && name != null && mFilePathMap.containsKey(name)) {
                String vivoPath = VIVO_PATH + mFilePathMap.get(name);
                String regionPath = REGION_PATH + mFilePathMap.get(name);
                String CARRIER_DIR = SystemProperties.get("ro.vgc.config.carrierdir", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                String CARRIER = SystemProperties.get("persist.product.carrier.name", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                String VGC_CARRIER_ROOT = CARRIER_DIR + CARRIER + "/";
                String carrierPath = VGC_CARRIER_ROOT + mFilePathMap.get(name);
                File vivoFile = new File(vivoPath);
                File regionFile = new File(regionPath);
                File carrierFile = new File(carrierPath);
                if (carrierFile.exists()) {
                    if (DEBUG) {
                        Log.d("VGC", "getVgcFilePath name: " + name + " path: " + carrierPath);
                    }
                    return carrierPath;
                } else if (regionFile.exists()) {
                    if (DEBUG) {
                        Log.d("VGC", "getVgcFilePath name: " + name + " path: " + regionPath);
                    }
                    return regionPath;
                } else if (vivoFile.exists()) {
                    if (DEBUG) {
                        Log.d("VGC", "getVgcFilePath name: " + name + " path: " + vivoPath);
                    }
                    return vivoPath;
                }
            }
            if (DEBUG) {
                Log.d("VGC", "getVgcFilePath name: " + name + " defaultStr path: " + defaultStr);
            }
            return defaultStr;
        }
    }

    public static void parseStringListOverlay(String configName, String carrier_path) {
        parseStringListFromFile(VIVO_PATH + configName);
        parseStringListFromFile(REGION_PATH + configName);
        if (carrier_path == null || carrier_path.isEmpty()) {
            parseStringListFromFile(CARRIER_PATH + configName);
            return;
        }
        parseStringListFromFile(carrier_path + configName);
    }

    public static boolean parseStringListFromFile(String path) {
        File configFile = new File(path);
        if (!configFile.exists() || 0 == configFile.length()) {
            VLog.e("VGC", "StringList file " + path + "  not exit!! ");
            return false;
        }
        if (DEBUG) {
            VLog.d("VGC", "parseStringListFromFile:   " + path);
        }
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            FileReader reader = null;
            try {
                try {
                    reader = new FileReader(path);
                    parser.setInput(reader);
                    String listName = null;
                    ArrayList<String> ItemList = null;
                    boolean firstFoundRootTag = false;
                    while (parser.getEventType() != 1) {
                        if (parser.getEventType() == 2) {
                            if (!firstFoundRootTag) {
                                firstFoundRootTag = true;
                                if (!ROOT_TAG.equalsIgnoreCase(parser.getName())) {
                                    VLog.e("VGC", "list " + path + "'s root tag is invalid: " + parser.getName());
                                    break;
                                }
                            }
                            if (LIST_TAG.equalsIgnoreCase(parser.getName())) {
                                ContentValues cv = getTagAttributesAndValues(parser);
                                if (isSpecialTag(parser, new String[]{NAME_TAG}, cv)) {
                                    ItemList = new ArrayList<>();
                                    listName = parser.getAttributeValue(null, NAME_TAG);
                                    if (DEBUG) {
                                        VLog.d("VGC", "list name:  " + listName);
                                    }
                                }
                            }
                            if (ITEM_TAG.equalsIgnoreCase(parser.getName())) {
                                ContentValues cv2 = getTagAttributesAndValues(parser);
                                if (ItemList != null && isSpecialTag(parser, new String[]{VALUE_TAG}, cv2)) {
                                    String value = parser.getAttributeValue(null, VALUE_TAG);
                                    ItemList.add(value);
                                    if (DEBUG) {
                                        VLog.d("VGC", "item value:  " + value);
                                    }
                                }
                            }
                        }
                        if (parser.getEventType() == 3 && LIST_TAG.equalsIgnoreCase(parser.getName())) {
                            if (VgcConfigOverlayHelper.isStringListRepeated(mListMap, listName)) {
                                mListMap.put(listName, ItemList);
                                if (DEBUG) {
                                    VLog.d("VGC", "overlay StringList " + listName + " ItemList :" + ItemList + "in " + path);
                                }
                            } else if (ItemList != null && listName != null) {
                                mListMap.put(listName, ItemList);
                                if (DEBUG) {
                                    VLog.d("VGC", "add StringList:  " + listName + " ItemList :" + ItemList);
                                }
                            }
                        }
                        parser.next();
                    }
                    try {
                        reader.close();
                    } catch (IOException e) {
                    }
                    if (DEBUG) {
                        VLog.d("VGC", "parseStringListFromFile end ");
                    }
                    return true;
                } catch (Throwable th) {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e2) {
                        }
                    }
                    throw th;
                }
            } catch (Exception e3) {
                e3.printStackTrace();
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e4) {
                    }
                }
                return false;
            }
        } catch (Exception e5) {
            e5.printStackTrace();
            return false;
        }
    }

    public static ArrayMap getStringListMapFromFile(String path) {
        ArrayMap map = new ArrayMap();
        File configFile = new File(path);
        if (!configFile.exists() || 0 == configFile.length()) {
            VLog.e("VGC", "StringList file " + path + "  not exit!! ");
            return map;
        }
        if (DEBUG) {
            VLog.d("VGC", "getStringListFromFile:   " + path);
        }
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            FileReader reader = null;
            try {
                try {
                    reader = new FileReader(path);
                    parser.setInput(reader);
                    String listName = null;
                    ArrayList<String> ItemList = null;
                    boolean firstFoundRootTag = false;
                    while (parser.getEventType() != 1) {
                        if (parser.getEventType() == 2) {
                            if (!firstFoundRootTag) {
                                firstFoundRootTag = true;
                                if (!ROOT_TAG.equalsIgnoreCase(parser.getName())) {
                                    VLog.e("VGC", "list " + path + "'s root tag is invalid: " + parser.getName());
                                    break;
                                }
                            }
                            if (LIST_TAG.equalsIgnoreCase(parser.getName())) {
                                ContentValues cv = getTagAttributesAndValues(parser);
                                if (isSpecialTag(parser, new String[]{NAME_TAG}, cv)) {
                                    ItemList = new ArrayList<>();
                                    listName = parser.getAttributeValue(null, NAME_TAG);
                                    if (DEBUG) {
                                        VLog.d("VGC", "list name:  " + listName);
                                    }
                                }
                            }
                            if (ITEM_TAG.equalsIgnoreCase(parser.getName())) {
                                ContentValues cv2 = getTagAttributesAndValues(parser);
                                if (ItemList != null && isSpecialTag(parser, new String[]{VALUE_TAG}, cv2)) {
                                    String value = parser.getAttributeValue(null, VALUE_TAG);
                                    ItemList.add(value);
                                    if (DEBUG) {
                                        VLog.d("VGC", "item value:  " + value);
                                    }
                                }
                            }
                        }
                        if (parser.getEventType() == 3 && LIST_TAG.equalsIgnoreCase(parser.getName())) {
                            if (VgcConfigOverlayHelper.isStringListRepeated(map, listName)) {
                                map.put(listName, ItemList);
                            } else if (ItemList != null && listName != null) {
                                map.put(listName, ItemList);
                                if (DEBUG) {
                                    VLog.d("VGC", "getStringListFromFile add:  " + listName + " ItemList :" + ItemList);
                                }
                            }
                        }
                        parser.next();
                    }
                    try {
                        reader.close();
                    } catch (IOException e) {
                    }
                    if (DEBUG) {
                        VLog.d("VGC", "parseStringListFromFile end ");
                    }
                    return map;
                } catch (Exception e2) {
                    e2.printStackTrace();
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e3) {
                        }
                    }
                    return map;
                }
            } catch (Throwable th) {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e4) {
                    }
                }
                throw th;
            }
        } catch (Exception e5) {
            e5.printStackTrace();
            return map;
        }
    }

    public static void parseContentValuesListOverlay(String configName, String carrier_path) {
        parseContentValuesListFromFile(VIVO_PATH + configName);
        parseContentValuesListFromFile(REGION_PATH + configName);
        if (carrier_path == null || carrier_path.isEmpty()) {
            parseContentValuesListFromFile(CARRIER_PATH + configName);
            return;
        }
        parseContentValuesListFromFile(carrier_path + configName);
    }

    public static boolean parseContentValuesListFromFile(String path) {
        File configFile = new File(path);
        if (!configFile.exists() || 0 == configFile.length()) {
            VLog.e("VGC", "cust list file " + path + "  not exit!! ");
            return false;
        }
        if (DEBUG) {
            VLog.d("VGC", "parseContentValuesListFromFile:   " + path);
        }
        new ArrayList();
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            FileReader reader = null;
            try {
                try {
                    reader = new FileReader(path);
                    parser.setInput(reader);
                    ContentValues mContentValues = null;
                    String custListName = null;
                    boolean firstFoundRootTag = false;
                    while (parser.getEventType() != 1) {
                        if (parser.getEventType() == 2) {
                            if (!firstFoundRootTag) {
                                firstFoundRootTag = true;
                                if (!ROOT_TAG.equalsIgnoreCase(parser.getName())) {
                                    VLog.e("VGC", "customlist " + path + "'s root tag is invalid: " + parser.getName());
                                    break;
                                }
                            }
                            if (CUST_LIST_TAG.equalsIgnoreCase(parser.getName())) {
                                ContentValues cv = getTagAttributesAndValues(parser);
                                if (isSpecialTag(parser, new String[]{NAME_TAG}, cv)) {
                                    mContentValues = new ContentValues();
                                    custListName = parser.getAttributeValue(null, NAME_TAG);
                                    if (DEBUG) {
                                        VLog.d("VGC", "custList name:  " + custListName);
                                    }
                                }
                            }
                            if (mContentValues != null && ITEM_TAG.equalsIgnoreCase(parser.getName())) {
                                ContentValues cv2 = getTagAttributesAndValues(parser);
                                if (isSpecialTag(parser, new String[]{NAME_TAG, VALUE_TAG}, cv2)) {
                                    String name = parser.getAttributeValue(null, NAME_TAG);
                                    String value = parser.getAttributeValue(null, VALUE_TAG);
                                    mContentValues.put(name, value);
                                    if (DEBUG) {
                                        VLog.d("VGC", "Item name :" + name + " value:  " + value);
                                    }
                                }
                            }
                        }
                        if (parser.getEventType() == 3 && CUST_LIST_TAG.equalsIgnoreCase(parser.getName())) {
                            if (VgcConfigOverlayHelper.isContentValuesListRepeated(mCustListMap, custListName)) {
                                mCustListMap.put(custListName, mContentValues);
                                if (DEBUG) {
                                    VLog.d("VGC", "overlay custList " + custListName + " in " + path);
                                }
                            } else if (mContentValues != null && custListName != null) {
                                mCustListMap.put(custListName, mContentValues);
                                if (DEBUG) {
                                    VLog.d("VGC", "add custList:  " + custListName);
                                }
                            }
                        }
                        parser.next();
                    }
                    try {
                        reader.close();
                    } catch (IOException e) {
                    }
                    if (DEBUG) {
                        VLog.d("VGC", "parseContentValuesListFromFile end ");
                    }
                    return true;
                } catch (Exception e2) {
                    e2.printStackTrace();
                    if (reader == null) {
                        return false;
                    }
                    try {
                        reader.close();
                        return false;
                    } catch (IOException e3) {
                        return false;
                    }
                }
            } catch (Throwable th) {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e4) {
                    }
                }
                throw th;
            }
        } catch (Exception e5) {
            e5.printStackTrace();
            return false;
        }
    }

    public static ArrayMap getContentValuesMapFromFile(String path) {
        String str;
        String str2 = VALUE_TAG;
        ArrayMap map = new ArrayMap();
        File configFile = new File(path);
        if (!configFile.exists() || 0 == configFile.length()) {
            VLog.e("VGC", "cust list file " + path + "  not exit!! ");
            return map;
        }
        if (DEBUG) {
            VLog.d("VGC", "getContentValuesMapFromFile:   " + path);
        }
        new ArrayList();
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            FileReader reader = null;
            try {
                try {
                    reader = new FileReader(path);
                    parser.setInput(reader);
                    ContentValues mContentValues = null;
                    String custListName = null;
                    boolean firstFoundRootTag = false;
                    while (parser.getEventType() != 1) {
                        if (parser.getEventType() != 2) {
                            str = str2;
                        } else {
                            if (!firstFoundRootTag) {
                                firstFoundRootTag = true;
                                if (!ROOT_TAG.equalsIgnoreCase(parser.getName())) {
                                    VLog.e("VGC", "customlist " + path + "'s root tag is invalid: " + parser.getName());
                                    break;
                                }
                            }
                            if (CUST_LIST_TAG.equalsIgnoreCase(parser.getName())) {
                                ContentValues cv = getTagAttributesAndValues(parser);
                                if (isSpecialTag(parser, new String[]{NAME_TAG}, cv)) {
                                    mContentValues = new ContentValues();
                                    custListName = parser.getAttributeValue(null, NAME_TAG);
                                    if (DEBUG) {
                                        VLog.d("VGC", "custList name:  " + custListName);
                                    }
                                }
                            }
                            if (mContentValues == null || !ITEM_TAG.equalsIgnoreCase(parser.getName())) {
                                str = str2;
                            } else {
                                ContentValues cv2 = getTagAttributesAndValues(parser);
                                if (!isSpecialTag(parser, new String[]{NAME_TAG, str2}, cv2)) {
                                    str = str2;
                                } else {
                                    String name = parser.getAttributeValue(null, NAME_TAG);
                                    String value = parser.getAttributeValue(null, str2);
                                    mContentValues.put(name, value);
                                    if (DEBUG) {
                                        StringBuilder sb = new StringBuilder();
                                        str = str2;
                                        sb.append("Item name :");
                                        sb.append(name);
                                        sb.append(" value:  ");
                                        sb.append(value);
                                        VLog.d("VGC", sb.toString());
                                    } else {
                                        str = str2;
                                    }
                                }
                            }
                        }
                        if (parser.getEventType() == 3 && CUST_LIST_TAG.equalsIgnoreCase(parser.getName())) {
                            if (VgcConfigOverlayHelper.isContentValuesListRepeated(map, custListName)) {
                                map.put(custListName, mContentValues);
                                if (DEBUG) {
                                    VLog.d("VGC", "overlay custList " + custListName + " in " + path);
                                }
                            } else if (mContentValues != null && custListName != null) {
                                map.put(custListName, mContentValues);
                                if (DEBUG) {
                                    VLog.d("VGC", "add custList:  " + custListName);
                                }
                            }
                        }
                        parser.next();
                        str2 = str;
                    }
                    try {
                        reader.close();
                    } catch (IOException e) {
                    }
                    if (DEBUG) {
                        VLog.d("VGC", "parseContentValuesListFromFile end ");
                    }
                    return map;
                } catch (Exception e2) {
                    e2.printStackTrace();
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e3) {
                        }
                    }
                    return map;
                }
            } catch (Throwable th) {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e4) {
                    }
                }
                throw th;
            }
        } catch (Exception e5) {
            e5.printStackTrace();
            return map;
        }
    }

    public static void parseConfigOverlay(String configName, String carrier_path) {
        parseConfigFromFile(VIVO_PATH + configName);
        parseConfigFromFile(REGION_PATH + configName);
        if (carrier_path == null || carrier_path.isEmpty()) {
            parseConfigFromFile(CARRIER_PATH + configName);
            return;
        }
        parseConfigFromFile(carrier_path + configName);
    }

    public static boolean parseConfigFromFile(String path) {
        File configFile = new File(path);
        if (!configFile.exists() || 0 == configFile.length()) {
            VLog.e("VGC", "config file " + path + "  not exit!! ");
            return false;
        }
        if (DEBUG) {
            VLog.d("VGC", "parseConfigFromFile:   " + path);
        }
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            FileReader reader = null;
            try {
                try {
                    reader = new FileReader(path);
                    parser.setInput(reader);
                    boolean firstFoundRootTag = false;
                    while (parser.getEventType() != 1) {
                        if (parser.getEventType() == 2) {
                            if (!firstFoundRootTag) {
                                firstFoundRootTag = true;
                                if (!ROOT_TAG.equalsIgnoreCase(parser.getName())) {
                                    VLog.e("VGC", "Config config file " + path + "'s root tag is invalid: " + parser.getName());
                                    break;
                                }
                            }
                            if (BOOL_TAG.equalsIgnoreCase(parser.getName())) {
                                ContentValues cv = getTagAttributesAndValues(parser);
                                if (isSpecialTag(parser, new String[]{NAME_TAG}, cv)) {
                                    String name = parser.getAttributeValue(null, NAME_TAG);
                                    String value = parser.nextText();
                                    if (VgcConfigOverlayHelper.isConfigRepeated(mBoolMap, name)) {
                                        mBoolMap.put(name, value);
                                        if (DEBUG) {
                                            VLog.d("VGC", "overlay bool " + name + " in " + path);
                                        }
                                    } else if (name != null && value != null) {
                                        mBoolMap.put(name, value);
                                        if (DEBUG) {
                                            VLog.d("VGC", "Add bool name:  " + name + "  value: " + value);
                                        }
                                    }
                                }
                            } else if (STRING_TAG.equalsIgnoreCase(parser.getName())) {
                                ContentValues cv2 = getTagAttributesAndValues(parser);
                                if (isSpecialTag(parser, new String[]{NAME_TAG}, cv2)) {
                                    String name2 = parser.getAttributeValue(null, NAME_TAG);
                                    String value2 = parser.nextText();
                                    if (VgcConfigOverlayHelper.isConfigRepeated(mStringMap, name2)) {
                                        mStringMap.put(name2, value2);
                                        if (DEBUG) {
                                            VLog.d("VGC", "overlay string " + name2 + " in " + path);
                                        }
                                    } else if (name2 != null && value2 != null) {
                                        mStringMap.put(name2, value2);
                                        if (DEBUG) {
                                            VLog.d("VGC", "Add string name:  " + name2 + "  value: " + value2);
                                        }
                                    }
                                }
                            } else if (INT_TAG.equalsIgnoreCase(parser.getName())) {
                                ContentValues cv3 = getTagAttributesAndValues(parser);
                                if (isSpecialTag(parser, new String[]{NAME_TAG}, cv3)) {
                                    String name3 = parser.getAttributeValue(null, NAME_TAG);
                                    String value3 = parser.nextText();
                                    if (VgcConfigOverlayHelper.isConfigRepeated(mIntMap, name3)) {
                                        mIntMap.put(name3, value3);
                                        if (DEBUG) {
                                            VLog.d("VGC", "overlay int " + name3 + " in " + path);
                                        }
                                    } else if (name3 != null && value3 != null) {
                                        mIntMap.put(name3, value3);
                                        if (DEBUG) {
                                            VLog.d("VGC", "Add int name:  " + name3 + "  value: " + value3);
                                        }
                                    }
                                }
                            }
                        }
                        parser.next();
                    }
                    try {
                        reader.close();
                    } catch (IOException e) {
                    }
                    if (DEBUG) {
                        VLog.d("VGC", "parseConfigFromFile end ");
                    }
                    return true;
                } catch (Exception e2) {
                    e2.printStackTrace();
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e3) {
                        }
                    }
                    return false;
                }
            } catch (Throwable th) {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e4) {
                    }
                }
                throw th;
            }
        } catch (Exception e5) {
            e5.printStackTrace();
            return false;
        }
    }

    /* JADX WARN: Can't wrap try/catch for region: R(6:33|(3:53|54|(1:56))(2:(1:52)(2:36|(1:51))|45)|38|39|(1:41)|42) */
    /* JADX WARN: Code restructure failed: missing block: B:70:0x01fb, code lost:
        if (com.vivo.services.vgc.VgcXmlParserHelper.DEBUG != false) goto L50;
     */
    /* JADX WARN: Code restructure failed: missing block: B:71:0x01fd, code lost:
        com.vivo.common.utils.VLog.d("VGC", "parseInt name: " + r9 + " value: " + r10 + " fail, drop this config");
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public static android.util.ArrayMap getBaseConfigFromFile(java.lang.String r14) {
        /*
            Method dump skipped, instructions count: 619
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.vivo.services.vgc.VgcXmlParserHelper.getBaseConfigFromFile(java.lang.String):android.util.ArrayMap");
    }

    public static boolean parseConfigPathFromFile(String path) {
        File configFile = new File(path);
        if (!configFile.exists() || 0 == configFile.length()) {
            VLog.e("VGC", "config path file " + path + "  not exit!! ");
            return false;
        }
        if (DEBUG) {
            VLog.d("VGC", "parsePathFromFile:   " + path);
        }
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            FileReader reader = null;
            try {
                try {
                    FileReader reader2 = new FileReader(path);
                    parser.setInput(reader2);
                    boolean firstFoundRootTag = false;
                    while (parser.getEventType() != 1) {
                        if (parser.getEventType() == 2) {
                            if (!firstFoundRootTag) {
                                firstFoundRootTag = true;
                                if (!ROOT_TAG.equalsIgnoreCase(parser.getName())) {
                                    VLog.e("VGC", "Config config file " + path + "'s root tag is invalid: " + parser.getName());
                                    break;
                                }
                            }
                            if (PATH_TAG.equalsIgnoreCase(parser.getName())) {
                                ContentValues cv = getTagAttributesAndValues(parser);
                                if (isSpecialTag(parser, new String[]{NAME_TAG}, cv)) {
                                    String name = parser.getAttributeValue(null, NAME_TAG);
                                    String value = parser.nextText();
                                    if (name != null && value != null) {
                                        synchronized (sFilePathMapLock) {
                                            if (VgcConfigOverlayHelper.isPathRepeated(mFilePathMap, name)) {
                                                if (DEBUG) {
                                                    VLog.d("VGC", "overlay path " + name + " in " + path);
                                                }
                                            } else if (DEBUG) {
                                                VLog.d("VGC", "Add path name:  " + name + "  path: " + value);
                                            }
                                            mFilePathMap.put(name, value);
                                        }
                                    }
                                } else {
                                    continue;
                                }
                            } else {
                                continue;
                            }
                        }
                        parser.next();
                    }
                    try {
                        reader2.close();
                    } catch (IOException e) {
                    }
                    if (DEBUG) {
                        VLog.d("VGC", "parseConfigPathFromFile end ");
                    }
                    return true;
                } catch (Exception e2) {
                    e2.printStackTrace();
                    if (0 != 0) {
                        try {
                            reader.close();
                        } catch (IOException e3) {
                        }
                    }
                    return false;
                }
            } catch (Throwable th) {
                if (0 != 0) {
                    try {
                        reader.close();
                    } catch (IOException e4) {
                    }
                }
                throw th;
            }
        } catch (Exception e5) {
            e5.printStackTrace();
            return false;
        }
    }

    public static ArrayMap getConfigPathFromFile(String path) {
        ArrayMap map = new ArrayMap();
        File configFile = new File(path);
        if (!configFile.exists() || 0 == configFile.length()) {
            VLog.e("VGC", "config path file " + path + "  not exit!! ");
            return map;
        }
        if (DEBUG) {
            VLog.d("VGC", "parsePathFromFile:   " + path);
        }
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            FileReader reader = null;
            try {
                try {
                    reader = new FileReader(path);
                    parser.setInput(reader);
                    boolean firstFoundRootTag = false;
                    while (parser.getEventType() != 1) {
                        if (parser.getEventType() == 2) {
                            if (!firstFoundRootTag) {
                                firstFoundRootTag = true;
                                if (!ROOT_TAG.equalsIgnoreCase(parser.getName())) {
                                    VLog.e("VGC", "Config config file " + path + "'s root tag is invalid: " + parser.getName());
                                    break;
                                }
                            }
                            if (PATH_TAG.equalsIgnoreCase(parser.getName())) {
                                ContentValues cv = getTagAttributesAndValues(parser);
                                if (isSpecialTag(parser, new String[]{NAME_TAG}, cv)) {
                                    String name = parser.getAttributeValue(null, NAME_TAG);
                                    String value = parser.nextText();
                                    if (name != null && value != null) {
                                        if (VgcConfigOverlayHelper.isPathRepeated(map, name)) {
                                            if (DEBUG) {
                                                VLog.d("VGC", "overlay path " + name + " in " + path);
                                            }
                                        } else if (DEBUG) {
                                            VLog.d("VGC", "Add path name:  " + name + "  path: " + value);
                                        }
                                        map.put(name, value);
                                    }
                                }
                            }
                        }
                        parser.next();
                    }
                    try {
                        reader.close();
                    } catch (IOException e) {
                    }
                    if (DEBUG) {
                        VLog.d("VGC", "parseConfigPathFromFile end ");
                    }
                    return map;
                } catch (Exception e2) {
                    e2.printStackTrace();
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e3) {
                        }
                    }
                    return map;
                }
            } catch (Throwable th) {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e4) {
                    }
                }
                throw th;
            }
        } catch (Exception e5) {
            e5.printStackTrace();
            return map;
        }
    }
}