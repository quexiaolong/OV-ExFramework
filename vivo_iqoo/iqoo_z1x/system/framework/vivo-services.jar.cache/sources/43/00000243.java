package com.android.server.display.color.displayenhance;

import android.content.Context;
import android.content.res.Resources;
import android.os.FtBuild;
import android.os.SystemProperties;
import android.util.Xml;
import com.android.server.am.firewall.VivoFirewall;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import vivo.app.VivoFrameworkFactory;
import vivo.app.configuration.AbsConfigurationManager;
import vivo.app.configuration.ConfigurationObserver;
import vivo.app.configuration.RawFileContent;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class MemcHdrWhiteList {
    static final String TAG = "MemcHdrWhiteList";
    public static final String WHITE_LIST_FILE_PATH = "/data/bbkcore/displayenhance_white_list_2.0.xml";
    private static volatile MemcHdrWhiteList sMemcHdrWhiteList = null;
    private Context mContext;
    private boolean DBG = SystemProperties.getBoolean("persist.vivo.display.enhance.debug", false);
    private HashMap<String, HashMap<String, String>> mPackageWhiteListMap = new HashMap<>();
    private ArrayList<WhiteListUpdataeListener> mWhiteListUpdataeListeners = new ArrayList<>();
    private AbsConfigurationManager mCMSmanager = null;
    private RawFileContent mRawFileContent = null;
    private String mFileContent = null;
    private boolean mAppInfoPasered = false;

    /* loaded from: classes.dex */
    public interface WhiteListUpdataeListener {
        void updateWhiteList(HashMap<String, HashMap<String, String>> hashMap);
    }

    private MemcHdrWhiteList(Context context) {
        this.mContext = context;
        initCMSmanager();
        getFileContent();
    }

    private void initCMSmanager() {
        AbsConfigurationManager configurationManager = VivoFrameworkFactory.getFrameworkFactoryImpl().getConfigurationManager();
        this.mCMSmanager = configurationManager;
        if (configurationManager != null) {
            RawFileContent rawFileContent = configurationManager.getRawFileContent(WHITE_LIST_FILE_PATH);
            this.mRawFileContent = rawFileContent;
            this.mCMSmanager.registerObserver(rawFileContent, new ConfigurationObserver() { // from class: com.android.server.display.color.displayenhance.MemcHdrWhiteList.1
                public void onConfigChange(String file, String name) {
                    MemcHdrWhiteList.this.mAppInfoPasered = false;
                    MemcHdrWhiteList.this.getFileContent();
                    MemcHdrWhiteList.this.mPackageWhiteListMap = new HashMap();
                    MemcHdrWhiteList.this.parserAppInfo();
                    Iterator it = MemcHdrWhiteList.this.mWhiteListUpdataeListeners.iterator();
                    while (it.hasNext()) {
                        WhiteListUpdataeListener listener = (WhiteListUpdataeListener) it.next();
                        listener.updateWhiteList(MemcHdrWhiteList.this.mPackageWhiteListMap);
                    }
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void getFileContent() {
        AbsConfigurationManager absConfigurationManager = this.mCMSmanager;
        if (absConfigurationManager != null) {
            try {
                RawFileContent rawFileContent = absConfigurationManager.getRawFileContent(WHITE_LIST_FILE_PATH);
                this.mRawFileContent = rawFileContent;
                if (rawFileContent != null) {
                    this.mFileContent = rawFileContent.getFileContent();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static MemcHdrWhiteList getInstance(Context context) {
        if (sMemcHdrWhiteList == null) {
            synchronized (MemcHdrWhiteList.class) {
                if (sMemcHdrWhiteList == null) {
                    sMemcHdrWhiteList = new MemcHdrWhiteList(context);
                }
            }
        }
        return sMemcHdrWhiteList;
    }

    public synchronized HashMap<String, HashMap<String, String>> parserAppInfo() {
        if (this.mAppInfoPasered) {
            return this.mPackageWhiteListMap;
        }
        try {
            XmlPullParser xmlParser = getWhiteList();
            if (xmlParser != null) {
                try {
                    HashMap<String, String> activityWhiteListMap = null;
                    if (this.DBG) {
                        VSlog.d(TAG, "loadFrcCtrlInfo start");
                    }
                    for (int event = xmlParser.getEventType(); event != 1; event = xmlParser.next()) {
                        if (event == 2) {
                            if ("package".equals(xmlParser.getName())) {
                                String pkg = xmlParser.getAttributeValue(0);
                                if (this.DBG) {
                                    VSlog.i(TAG, "<<<== package : " + pkg + " ==>>>");
                                }
                                activityWhiteListMap = new HashMap<>();
                                this.mPackageWhiteListMap.put(pkg, activityWhiteListMap);
                            } else if (VivoFirewall.TYPE_ACTIVITY.equals(xmlParser.getName())) {
                                String activity = xmlParser.getAttributeValue(0);
                                String memcChannel = xmlParser.nextText();
                                if (this.DBG) {
                                    VSlog.i(TAG, "activity : " + activity + ", memcChannel: " + memcChannel);
                                }
                                if (activityWhiteListMap != null) {
                                    activityWhiteListMap.put(activity, memcChannel);
                                }
                            }
                        }
                    }
                    if (this.DBG) {
                        VSlog.d(TAG, "loadFrcCtrlInfo end");
                    }
                    this.mAppInfoPasered = true;
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                }
            }
        } catch (Resources.NotFoundException e2) {
            e2.printStackTrace();
        } catch (IOException e3) {
            e3.printStackTrace();
        }
        return this.mPackageWhiteListMap;
    }

    private XmlPullParser getWhiteList() {
        XmlPullParser xmlParser = null;
        if (this.mFileContent != null) {
            InputStream targetStream = new ByteArrayInputStream(this.mFileContent.getBytes());
            xmlParser = Xml.newPullParser();
            try {
                xmlParser.setInput(targetStream, "utf-8");
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }
        }
        if (xmlParser == null) {
            VSlog.d(TAG, "getWhiteList from CMS failed");
            Resources res = this.mContext.getResources();
            if (res != null) {
                if ("vos".equals(FtBuild.getOsName())) {
                    XmlPullParser xmlParser2 = res.getXml(51576840);
                    return xmlParser2;
                }
                XmlPullParser xmlParser3 = res.getXml(51576832);
                return xmlParser3;
            }
            return xmlParser;
        }
        return xmlParser;
    }

    public void setWhiteListUpdataeListener(WhiteListUpdataeListener listener) {
        ArrayList<WhiteListUpdataeListener> arrayList = this.mWhiteListUpdataeListeners;
        if (arrayList != null) {
            arrayList.add(listener);
        }
    }
}