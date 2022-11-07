package com.vivo.services.sarpower;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import com.vivo.face.common.data.Constants;
import com.vivo.sensor.autobrightness.utils.SElog;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

/* loaded from: classes.dex */
public class ConfigList2Parser {
    private static final String ATTR_CONF_NAME = "confName";
    private static final String ATTR_COUNTRY_CODE = "countryCode";
    private static final String ATTR_COUNTRY_CODE_MATCH = "countryCodeMatch";
    private static final String ATTR_PROJECT = "project";
    private static final String ATTR_PROJECT_MATCH = "projectMatch";
    private static final String ATTR_PURPOSE = "purpose";
    private static final String ATTR_USE_EXIST_CONF = "useExistConf";
    private static final String ATTR_VERSION = "version";
    private static final int CONFIG_ATTR_COUNT = 5;
    private static final String CONFIG_LIST_VERSION_1 = "1";
    private static final String COUNTRY_CODE_SEPRATOR = ",";
    private static final String DEFAULT_COUNTRY_CODE = "N";
    private static final String TAG = "ConfigList2Parser";
    private static final String TAG_CMD = "CMD";
    private static final String TAG_CONFIG = "Config";
    private static final String TAG_CONFIG_LIST = "ConfigList";
    private static final String VAL_COUNTRY_ALL = "all";
    private static final String VAL_COUNTRY_EQUAL = "equal";
    private static final String VAL_COUNTRY_EXCLUDE = "exclude";
    private static final String VAL_PROJECT_EQUAL = "equal";
    private static final String VAL_PROJECT_START = "start";
    private static final String VAL_PURPOSE_ALL = "all";
    private static final String VAL_PURPOSE_CAMERA = "camera";
    private static final String VAL_PURPOSE_FACTORY = "factory";
    private static final String VAL_PURPOSE_NORMAL = "normal";
    private ArrayList<ConfigList2> mConfigArrayList = new ArrayList<>();
    private Handler mHandler;
    private ParseListener mListener;
    private final String mXmlPath;
    private static boolean DEBUG_BBK_LOG = "yes".equals(SystemProperties.get("persist.sys.log.ctrl", "no"));
    private static final String TAG_HEAD = "Head";
    private static final String TAG_BODY = "Body";
    private static final String TAG_C2K = "C2K";
    private static final String TAG_WHITE_HEAD = "WhiteHead";
    private static final String TAG_WHITE_BODY = "WhiteBody";
    private static final String TAG_WHITE_C2K = "WhiteC2K";
    private static final String TAG_RESET_GSM = "ResetGSM";
    private static final String TAG_RESET_C2K = "ResetC2K";
    private static final String[] COMMAND_TYPE_TAGS = {TAG_HEAD, TAG_BODY, TAG_C2K, TAG_WHITE_HEAD, TAG_WHITE_BODY, TAG_WHITE_C2K, TAG_RESET_GSM, TAG_RESET_C2K};

    /* loaded from: classes.dex */
    public interface ParseListener {
        void onParseFinished();
    }

    public ConfigList2Parser(String path, Looper looper, ParseListener listener) {
        this.mListener = null;
        this.mXmlPath = path;
        this.mListener = listener;
        this.mHandler = new Handler(looper);
    }

    public void startParse() {
        this.mHandler.post(new Runnable() { // from class: com.vivo.services.sarpower.-$$Lambda$ConfigList2Parser$L8JhVZOZB-83KctVTJL9MBObT5M
            @Override // java.lang.Runnable
            public final void run() {
                ConfigList2Parser.this.lambda$startParse$0$ConfigList2Parser();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: handleParseXml */
    public void lambda$startParse$0$ConfigList2Parser() {
        parseXml();
        ParseListener parseListener = this.mListener;
        if (parseListener != null) {
            parseListener.onParseFinished();
        }
    }

    private void parseXml() {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            try {
                FileInputStream fis = new FileInputStream(this.mXmlPath);
                try {
                    try {
                        FileReader fr = new FileReader(new File(this.mXmlPath));
                        parser.setInput(fr);
                        int eventType = parser.getEventType();
                        if (eventType != 0) {
                            log("parseXml wrong doc type: first eventType:" + eventType);
                            throw new XmlPullParserException("wrong doc type: first eventType:" + eventType);
                        }
                        log("parseXml next() 1");
                        int outerDepth = parser.getDepth();
                        int next = parser.next();
                        while (true) {
                            int eventType2 = next;
                            if (eventType2 == 1 || parser.getDepth() <= outerDepth) {
                                break;
                            }
                            if (eventType2 == 2) {
                                String name = parser.getName();
                                if (TAG_CONFIG_LIST.equals(name)) {
                                    log("parseXml START_TAG parse:" + name);
                                    String version = parser.getAttributeValue(null, ATTR_VERSION);
                                    if ("1".equals(version)) {
                                        handleVersion1(parser);
                                    }
                                }
                            } else if (eventType2 != 3) {
                                log("parseXml unknown eventType:" + eventType2);
                            } else if (TAG_CONFIG_LIST.equals(parser.getName())) {
                                log("parseXml end of: ConfigList");
                            }
                            next = parser.next();
                        }
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        fis.close();
                    } catch (XmlPullParserException e2) {
                        log("parseXml perser exception:" + e2.getMessage());
                        fis.close();
                    }
                } catch (Exception e3) {
                }
            } catch (FileNotFoundException e4) {
                log("parseXml file exception: file not found.");
            } catch (SecurityException e5) {
                log("parseXml file exception: security.");
            }
        } catch (XmlPullParserException e6) {
            log("parseXml xml exception:" + e6.getMessage());
        }
    }

    private void handleVersion1(XmlPullParser parser) {
        log("handleVersion1");
        parser.getDepth();
        try {
            int eventType = parser.next();
            while (eventType != 1 && eventType != 3) {
                if (eventType == 2 && TAG_CONFIG.equals(parser.getName())) {
                    handleVersion1Config(parser);
                }
                eventType = parser.next();
            }
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }
    }

    private void handleVersion1Config(XmlPullParser parser) {
        Exception e;
        Exception e2;
        String[] strArr;
        boolean foundPrev;
        XmlPullParser xmlPullParser = parser;
        log("handleVersion1Config");
        int eventType = 2;
        String outerTag = null;
        String currentTag = null;
        boolean endOfConfig = false;
        ConfigList2 conf = null;
        boolean foundPrev2 = false;
        while (eventType != 1) {
            if (eventType == 2) {
                try {
                    currentTag = parser.getName();
                    try {
                        for (String type : COMMAND_TYPE_TAGS) {
                            if (type.equals(currentTag)) {
                                outerTag = currentTag;
                                log("handleVersion1Config found:" + currentTag);
                                break;
                            }
                        }
                        try {
                            if (TAG_CONFIG.equals(currentTag)) {
                                int attrCount = parser.getAttributeCount();
                                if (attrCount < 5) {
                                    endOfConfig = true;
                                    log("handleVersion1Config no enough attr for Config");
                                } else {
                                    String proj = xmlPullParser.getAttributeValue(null, ATTR_PROJECT);
                                    conf = new ConfigList2(proj);
                                    String configName = xmlPullParser.getAttributeValue(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, ATTR_CONF_NAME);
                                    String projectMatch = xmlPullParser.getAttributeValue(null, ATTR_PROJECT_MATCH);
                                    try {
                                        String purpose = xmlPullParser.getAttributeValue(null, ATTR_PURPOSE);
                                        String outerTag2 = outerTag;
                                        try {
                                            String countryCode = xmlPullParser.getAttributeValue(null, ATTR_COUNTRY_CODE);
                                            try {
                                                String countryCodeMatch = xmlPullParser.getAttributeValue(null, ATTR_COUNTRY_CODE_MATCH);
                                                boolean endOfConfig2 = endOfConfig;
                                                try {
                                                    String useExistConf = xmlPullParser.getAttributeValue(null, ATTR_USE_EXIST_CONF);
                                                    conf.mProjectMatch = "equal".equals(projectMatch) ? 1 : 2;
                                                    if (purpose != null) {
                                                        foundPrev = foundPrev2;
                                                        String[] pps = purpose.split(COUNTRY_CODE_SEPRATOR);
                                                        try {
                                                            int length = pps.length;
                                                            int attrCount2 = 0;
                                                            while (attrCount2 < length) {
                                                                String p = pps[attrCount2];
                                                                String[] pps2 = pps;
                                                                int i = length;
                                                                if (VAL_PURPOSE_FACTORY.equals(p)) {
                                                                    conf.mPurpose |= 2;
                                                                } else if (VAL_PURPOSE_CAMERA.equals(p)) {
                                                                    conf.mPurpose |= 4;
                                                                } else if (VAL_PURPOSE_NORMAL.equals(p)) {
                                                                    conf.mPurpose |= 1;
                                                                } else if ("all".equals(p)) {
                                                                    conf.mPurpose |= 7;
                                                                }
                                                                attrCount2++;
                                                                pps = pps2;
                                                                length = i;
                                                            }
                                                        } catch (IOException | XmlPullParserException e3) {
                                                            e2 = e3;
                                                            log("handleVersion1Config e 1:" + e2.getMessage());
                                                            return;
                                                        } catch (Exception e4) {
                                                            e = e4;
                                                            log("handleVersion1Config e 2:" + e.getMessage());
                                                            return;
                                                        }
                                                    } else {
                                                        foundPrev = foundPrev2;
                                                        conf.mPurpose = 1;
                                                    }
                                                    if (countryCode == null || Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK.equals(countryCode)) {
                                                        countryCode = DEFAULT_COUNTRY_CODE;
                                                    }
                                                    conf.mCountryCodeList = countryCode.split(COUNTRY_CODE_SEPRATOR);
                                                    if ("equal".equals(countryCodeMatch)) {
                                                        conf.mCountryCodeMatch = 1;
                                                    } else if (VAL_COUNTRY_EXCLUDE.equals(countryCodeMatch)) {
                                                        conf.mCountryCodeMatch = 2;
                                                    } else {
                                                        conf.mCountryCodeMatch = 3;
                                                    }
                                                    log("handleVersion1Config conf inited: model:" + conf.model + " projectMatch:" + conf.mProjectMatch + " purpose:" + conf.mPurpose + " country:" + Arrays.toString(conf.mCountryCodeList) + " coutryMatch:" + conf.mCountryCodeMatch);
                                                    if (configName != null) {
                                                        conf.mConfigName = configName;
                                                    } else if (useExistConf != null) {
                                                        Iterator<ConfigList2> it = this.mConfigArrayList.iterator();
                                                        while (true) {
                                                            if (!it.hasNext()) {
                                                                foundPrev2 = foundPrev;
                                                                break;
                                                            }
                                                            ConfigList2 prevConf = it.next();
                                                            if (prevConf.mConfigName.equals(useExistConf)) {
                                                                conf.copyFrom(prevConf);
                                                                foundPrev2 = true;
                                                                break;
                                                            }
                                                        }
                                                        try {
                                                            log("handleVersion1Config use exist:" + useExistConf + " found:" + foundPrev2);
                                                            outerTag = outerTag2;
                                                            currentTag = currentTag;
                                                            endOfConfig = endOfConfig2;
                                                        } catch (IOException | XmlPullParserException e5) {
                                                            e2 = e5;
                                                            log("handleVersion1Config e 1:" + e2.getMessage());
                                                            return;
                                                        } catch (Exception e6) {
                                                            e = e6;
                                                            log("handleVersion1Config e 2:" + e.getMessage());
                                                            return;
                                                        }
                                                    }
                                                    foundPrev2 = foundPrev;
                                                    outerTag = outerTag2;
                                                    currentTag = currentTag;
                                                    endOfConfig = endOfConfig2;
                                                } catch (IOException | XmlPullParserException e7) {
                                                    e2 = e7;
                                                } catch (Exception e8) {
                                                    e = e8;
                                                }
                                            } catch (IOException | XmlPullParserException e9) {
                                                e2 = e9;
                                            } catch (Exception e10) {
                                                e = e10;
                                            }
                                        } catch (IOException | XmlPullParserException e11) {
                                            e2 = e11;
                                        } catch (Exception e12) {
                                            e = e12;
                                        }
                                    } catch (IOException | XmlPullParserException e13) {
                                        e2 = e13;
                                    } catch (Exception e14) {
                                        e = e14;
                                    }
                                }
                            }
                        } catch (IOException | XmlPullParserException e15) {
                            e2 = e15;
                        } catch (Exception e16) {
                            e = e16;
                        }
                    } catch (IOException | XmlPullParserException e17) {
                        e2 = e17;
                    } catch (Exception e18) {
                        e = e18;
                    }
                } catch (IOException | XmlPullParserException e19) {
                    e2 = e19;
                } catch (Exception e20) {
                    e = e20;
                }
            } else if (eventType != 3) {
                if (eventType == 4) {
                    if (!foundPrev2) {
                        try {
                            if (TAG_CMD.equals(currentTag)) {
                                String cmd = parser.getText();
                                if (cmd != null && !Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK.equals(cmd) && !Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK.equals(cmd.trim())) {
                                    String cmd2 = cmd.trim();
                                    if (TAG_HEAD.equals(outerTag)) {
                                        conf.mHeadList.add(cmd2);
                                        log("handleVersion1Config add cmd for:Head\"" + cmd2 + "\"");
                                    } else if (TAG_BODY.equals(outerTag)) {
                                        conf.mBodyList.add(cmd2);
                                        log("handleVersion1Config add cmd for:Body\"" + cmd2 + "\"");
                                    } else if (TAG_C2K.equals(outerTag)) {
                                        conf.mC2KList.add(cmd2);
                                        log("handleVersion1Config add cmd for:C2K\"" + cmd2 + "\"");
                                    } else if (TAG_WHITE_HEAD.equals(outerTag)) {
                                        conf.mWhiteHeadList.add(cmd2);
                                        log("handleVersion1Config add cmd for:WhiteHead\"" + cmd2 + "\"");
                                    } else if (TAG_WHITE_BODY.equals(outerTag)) {
                                        conf.mWhiteBodyList.add(cmd2);
                                        log("handleVersion1Config add cmd for:WhiteBody\"" + cmd2 + "\"");
                                    } else if (TAG_WHITE_C2K.equals(outerTag)) {
                                        conf.mWhiteC2KList.add(cmd2);
                                        log("handleVersion1Config add cmd for:WhiteC2K\"" + cmd2 + "\"");
                                    }
                                }
                            }
                        } catch (IOException | XmlPullParserException e21) {
                            e2 = e21;
                            log("handleVersion1Config e 1:" + e2.getMessage());
                            return;
                        } catch (Exception e22) {
                            e = e22;
                            log("handleVersion1Config e 2:" + e.getMessage());
                            return;
                        }
                    }
                    if (!foundPrev2 && TAG_RESET_GSM.equals(currentTag)) {
                        String cmd3 = parser.getText();
                        if (cmd3 != null && !Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK.equals(cmd3.trim())) {
                            conf.resetGSM = cmd3;
                        }
                        log("handleVersion1Config TAG_RESET_GSM text=" + cmd3);
                    } else if (!foundPrev2 && TAG_RESET_C2K.equals(currentTag)) {
                        String cmd4 = parser.getText();
                        if (cmd4 != null && !Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK.equals(cmd4.trim())) {
                            conf.resetC2K = cmd4;
                        }
                        log("handleVersion1Config TAG_RESET_C2K text=" + cmd4);
                    }
                }
            } else if (TAG_CONFIG.equals(parser.getName())) {
                endOfConfig = true;
            }
            if (endOfConfig) {
                if (conf == null) {
                    log("handleVersion1Config conf null.");
                    return;
                }
                conf.toArray();
                this.mConfigArrayList.add(conf);
                log("handleVersion1Config add conf.");
                return;
            }
            try {
                eventType = parser.next();
                xmlPullParser = parser;
            } catch (IOException | XmlPullParserException e23) {
                e2 = e23;
                log("handleVersion1Config e 1:" + e2.getMessage());
                return;
            } catch (Exception e24) {
                e = e24;
                log("handleVersion1Config e 2:" + e.getMessage());
                return;
            }
        }
    }

    public void dump(PrintWriter pw) {
        Iterator<ConfigList2> it = this.mConfigArrayList.iterator();
        while (it.hasNext()) {
            ConfigList2 conf = it.next();
            pw.println(conf.toString());
        }
    }

    public ConfigList getConfigList(String model, String countryCode, int purpose) {
        if (model == null || Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK.equals(model)) {
            log("getConfigList invalid model:" + model);
            return null;
        }
        Iterator<ConfigList2> it = this.mConfigArrayList.iterator();
        while (it.hasNext()) {
            ConfigList2 conf = it.next();
            log("getConfigList search: model" + conf.model + " projMatch:" + conf.mProjectMatch + " purpose:" + conf.mPurpose + " coutry:" + Arrays.toString(conf.mCountryCodeList) + " countryMatch:" + conf.mCountryCodeMatch);
            if (purpose == 0 || (conf.mPurpose & purpose) != purpose) {
                log("getConfigList purpose not macth, continue");
            } else {
                boolean projectMatched = false;
                if (conf.mProjectMatch == 1 && conf.model.equalsIgnoreCase(model)) {
                    projectMatched = true;
                } else if (conf.mProjectMatch == 2 && model.toLowerCase().startsWith(conf.model.toLowerCase())) {
                    projectMatched = true;
                }
                if (!projectMatched) {
                    log("getConfigList project not macth, continue");
                } else {
                    boolean countryMatched = false;
                    if (conf.mCountryCodeMatch == 3) {
                        countryMatched = true;
                    } else {
                        int i = 0;
                        if (conf.mCountryCodeMatch == 1) {
                            String[] strArr = conf.mCountryCodeList;
                            int length = strArr.length;
                            while (i < length) {
                                String country = strArr[i];
                                if (country.equals(countryCode)) {
                                    countryMatched = true;
                                }
                                i++;
                            }
                        } else if (conf.mCountryCodeMatch == 2) {
                            boolean eq = false;
                            String[] strArr2 = conf.mCountryCodeList;
                            int length2 = strArr2.length;
                            while (i < length2) {
                                String country2 = strArr2[i];
                                eq = country2.equals(countryCode);
                                if (eq) {
                                    break;
                                }
                                i++;
                            }
                            if (!eq) {
                                countryMatched = true;
                            }
                        }
                    }
                    if (!countryMatched) {
                        log("getConfigList country not macth");
                    } else {
                        return conf;
                    }
                }
            }
        }
        log("getConfigList no macth after loop search.");
        return null;
    }

    private void log(String msg) {
        if (DEBUG_BBK_LOG) {
            SElog.d(TAG, msg);
        }
    }
}