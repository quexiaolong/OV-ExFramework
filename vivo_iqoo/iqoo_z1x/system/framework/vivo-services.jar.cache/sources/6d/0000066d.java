package com.vivo.services.physicalfling;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.FtBuild;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemProperties;
import android.util.FtFeature;
import com.vivo.face.common.data.Constants;
import com.vivo.services.security.server.VivoPermissionUtils;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import vivo.app.VivoFrameworkFactory;
import vivo.app.configuration.AbsConfigurationManager;
import vivo.app.configuration.ConfigurationObserver;
import vivo.app.configuration.StringList;
import vivo.app.physicalfling.IPhysicalFlingManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class PhysicalFlingService extends IPhysicalFlingManager.Stub {
    private static final String ATTR_LIST_TYPE = "list_type";
    private static final int BLACK_LIST_TYPE = 1;
    private static final int DEFAULT_TYPE = -1;
    private static final String FEATURE_SPRING_LIST = "vivo.software.springlist";
    private static final String STR_INLINE_BLACK_LIST_FILE = "data/bbkcore/spring_effect_blacklist_1.0.xml";
    private static final String STR_INLINE_WHITE_LIST_FILE = "data/bbkcore/spring_effect_whitelist_1.0.xml";
    private static final String STR_LOCAL_BLACK_LIST_FILE = "system/etc/spring_effect_blacklist_1.0.xml";
    private static final String STR_LOCAL_WHITE_LIST_FILE = "system/etc/spring_effect_whitelist_1.0.xml";
    private static final String STR_SPRING_EFFECT_BLACK_LIST = "spring_effect_blacklist";
    private static final String STR_SPRING_EFFECT_WHITE_LIST = "spring_effect_whitelist";
    private static final int WHITE_LIST_TYPE = 0;
    private Context mContext;
    private Handler mHandler;
    private static float mRomVersion = FtBuild.getRomVersion();
    private static String mRomName = FtBuild.getOsName();
    private static final ArrayList<String> DEFAULT_SYSTEM_PACKAGE_NAME_WHITE_LIST = new ArrayList<>(Arrays.asList("com.android.", "com.bbk.", "com.vivo.", "com.iqoo.", "com.mediatek.", "com.qualcomm.", VivoPermissionUtils.OS_PKG, "com.quicinc.", "com.baidu.input_bbk."));
    private final String TAG = "PhysicalFlingService";
    private final boolean DEBUG = SystemProperties.getBoolean("persist.sys.debug.physicalfling", false);
    private boolean mIsOverSea = FtBuild.isOverSeas();
    private boolean mFosSupport = "Funtouch".equals(mRomName);
    private boolean mVosSupport = "vos".equals(mRomName);
    private boolean mIsBlackList = true;
    private HandlerThread mHandlerThread = new HandlerThread("PhysicalFlingService");
    private VivoFrameworkFactory mVivoFrameworkFactory = null;
    private AbsConfigurationManager mConfigurationManager = null;
    private StringList mSpringEffectStringWhiteList = null;
    private List<String> mSpringEffectAppWhiteList = null;
    private StringList mSpringEffectStringBlackList = null;
    private List<String> mSpringEffectAppBlackList = null;
    private ConfigurationObserver springEffectListObserver = new ConfigurationObserver() { // from class: com.vivo.services.physicalfling.PhysicalFlingService.1
        public void onConfigChange(String file, String name) {
            if (PhysicalFlingService.this.DEBUG) {
                VSlog.d("PhysicalFlingService", "onConfigChange");
            }
            PhysicalFlingService.this.getPhysicalFlingList();
            PhysicalFlingService.this.parseLocalConfig();
        }
    };

    public PhysicalFlingService(Context context) {
        this.mContext = null;
        this.mHandler = null;
        this.mContext = context;
        this.mHandlerThread.start();
        Handler handler = new Handler(this.mHandlerThread.getLooper());
        this.mHandler = handler;
        handler.post(new Runnable() { // from class: com.vivo.services.physicalfling.-$$Lambda$PhysicalFlingService$XxwFWX3J5q2htC7rLelBkyTedpc
            @Override // java.lang.Runnable
            public final void run() {
                PhysicalFlingService.this.lambda$new$0$PhysicalFlingService();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: initPhysicalFlingList */
    public void lambda$new$0$PhysicalFlingService() {
        VivoFrameworkFactory frameworkFactoryImpl = VivoFrameworkFactory.getFrameworkFactoryImpl();
        this.mVivoFrameworkFactory = frameworkFactoryImpl;
        if (frameworkFactoryImpl != null) {
            this.mConfigurationManager = frameworkFactoryImpl.getConfigurationManager();
            int type = Integer.parseInt(FtFeature.getFeatureAttribute(FEATURE_SPRING_LIST, ATTR_LIST_TYPE, String.valueOf(-1)));
            checkListType(type);
            getPhysicalFlingList();
            registerPhysicalFlingObserver();
            parseLocalConfig();
        }
    }

    private void checkListType(int type) {
        if (this.DEBUG) {
            VSlog.d("PhysicalFlingService", "PhysicalFling TYPE=" + type);
        }
        if (type == -1) {
            if (!this.mIsOverSea) {
                if (this.DEBUG) {
                    VSlog.d("PhysicalFlingService", "Black list for domestic");
                }
                this.mIsBlackList = true;
                return;
            }
            if (this.DEBUG) {
                VSlog.d("PhysicalFlingService", "White list for oversea");
            }
            this.mIsBlackList = false;
        } else if (type == 1) {
            if (this.DEBUG) {
                VSlog.d("PhysicalFlingService", "Feature for black list");
            }
            this.mIsBlackList = true;
        } else {
            if (this.DEBUG) {
                VSlog.d("PhysicalFlingService", "Feature for white list");
            }
            this.mIsBlackList = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void getPhysicalFlingList() {
        AbsConfigurationManager absConfigurationManager = this.mConfigurationManager;
        if (absConfigurationManager != null) {
            if (this.mIsBlackList) {
                this.mSpringEffectStringBlackList = absConfigurationManager.getStringList(STR_INLINE_BLACK_LIST_FILE, STR_SPRING_EFFECT_BLACK_LIST);
            } else {
                this.mSpringEffectStringWhiteList = absConfigurationManager.getStringList(STR_INLINE_WHITE_LIST_FILE, STR_SPRING_EFFECT_WHITE_LIST);
                this.mSpringEffectStringBlackList = this.mConfigurationManager.getStringList(STR_INLINE_BLACK_LIST_FILE, STR_SPRING_EFFECT_BLACK_LIST);
            }
            StringList stringList = this.mSpringEffectStringWhiteList;
            if (stringList != null) {
                this.mSpringEffectAppWhiteList = stringList.getValues();
                if (this.DEBUG) {
                    VSlog.d("PhysicalFlingService", "get physical fling list, mSpringEffectAppWhiteList = " + this.mSpringEffectAppWhiteList);
                }
            }
            StringList stringList2 = this.mSpringEffectStringBlackList;
            if (stringList2 != null) {
                this.mSpringEffectAppBlackList = stringList2.getValues();
                if (this.DEBUG) {
                    VSlog.d("PhysicalFlingService", "get physical fling list, mSpringEffectAppBlackList = " + this.mSpringEffectAppBlackList);
                }
            }
        }
    }

    private void registerPhysicalFlingObserver() {
        AbsConfigurationManager absConfigurationManager = this.mConfigurationManager;
        if (absConfigurationManager != null) {
            absConfigurationManager.registerObserver(this.mSpringEffectStringWhiteList, this.springEffectListObserver);
            this.mConfigurationManager.registerObserver(this.mSpringEffectStringBlackList, this.springEffectListObserver);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void parseLocalConfig() {
        if (this.mIsBlackList) {
            parseLocalBlackConfig();
            return;
        }
        parseLocalBlackConfig();
        parseLocalWhiteConfig();
    }

    private void parseLocalWhiteConfig() {
        List<String> list = this.mSpringEffectAppWhiteList;
        if (list == null || list.isEmpty()) {
            String xmlStr = getSystemEtc(STR_LOCAL_WHITE_LIST_FILE);
            try {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                XmlPullParser xmlPullParser = factory.newPullParser();
                xmlPullParser.setInput(new StringReader(xmlStr));
                while (xmlPullParser.getEventType() != 1) {
                    int eventType = xmlPullParser.getEventType();
                    if (eventType == 2 && xmlPullParser.getName().startsWith("item")) {
                        int count = xmlPullParser.getAttributeCount();
                        for (int i = 0; i < count; i++) {
                            if ("value".equals(xmlPullParser.getAttributeName(i))) {
                                String pkg = new String(xmlPullParser.getAttributeValue(i));
                                this.mSpringEffectAppWhiteList.add(pkg);
                            }
                        }
                    }
                    xmlPullParser.next();
                }
            } catch (Exception e) {
                VSlog.e("PhysicalFlingService", "parse local list error " + e.getMessage());
            }
            if (this.DEBUG) {
                VSlog.d("PhysicalFlingService", "parse local config, mSpringEffectAppWhiteList = " + this.mSpringEffectAppWhiteList);
            }
        }
    }

    private void parseLocalBlackConfig() {
        List<String> list = this.mSpringEffectAppBlackList;
        if (list == null || list.isEmpty()) {
            String xmlStr = getSystemEtc(STR_LOCAL_BLACK_LIST_FILE);
            try {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                XmlPullParser xmlPullParser = factory.newPullParser();
                xmlPullParser.setInput(new StringReader(xmlStr));
                while (xmlPullParser.getEventType() != 1) {
                    int eventType = xmlPullParser.getEventType();
                    if (eventType == 2 && xmlPullParser.getName().startsWith("item")) {
                        int count = xmlPullParser.getAttributeCount();
                        for (int i = 0; i < count; i++) {
                            if ("value".equals(xmlPullParser.getAttributeName(i))) {
                                String pkg = new String(xmlPullParser.getAttributeValue(i));
                                this.mSpringEffectAppBlackList.add(pkg);
                            }
                        }
                    }
                    xmlPullParser.next();
                }
            } catch (Exception e) {
                VSlog.e("PhysicalFlingService", "parse local list error " + e.getMessage());
            }
            if (this.DEBUG) {
                VSlog.d("PhysicalFlingService", "parse local config, mSpringEffectAppBlackList = " + this.mSpringEffectAppBlackList);
            }
        }
    }

    private String getSystemEtc(String name) {
        StringBuilder sb;
        String result = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        File file = new File(name);
        if (file.exists()) {
            FileInputStream inputStream = null;
            ByteArrayOutputStream baos = null;
            try {
                try {
                    inputStream = new FileInputStream(name);
                    baos = new ByteArrayOutputStream();
                    while (true) {
                        int ch = inputStream.read();
                        if (ch == -1) {
                            break;
                        }
                        baos.write(ch);
                    }
                    byte[] buff = baos.toByteArray();
                    result = new String(buff, "UTF-8");
                    result = result.replaceAll("\\r\\n", "\n");
                } catch (Exception e) {
                    VSlog.e("PhysicalFlingService", "get etc fling list file FAILED!!!" + e.getMessage());
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (Exception e2) {
                            e = e2;
                            sb = new StringBuilder();
                            sb.append("get etc fling list Exception:");
                            sb.append(e.getMessage());
                            VSlog.e("PhysicalFlingService", sb.toString());
                            return result;
                        }
                    }
                    if (baos != null) {
                        baos.close();
                    }
                }
                try {
                    inputStream.close();
                    baos.close();
                } catch (Exception e3) {
                    e = e3;
                    sb = new StringBuilder();
                    sb.append("get etc fling list Exception:");
                    sb.append(e.getMessage());
                    VSlog.e("PhysicalFlingService", sb.toString());
                    return result;
                }
            } catch (Throwable th) {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Exception e4) {
                        VSlog.e("PhysicalFlingService", "get etc fling list Exception:" + e4.getMessage());
                        throw th;
                    }
                }
                if (baos != null) {
                    baos.close();
                }
                throw th;
            }
        } else {
            VSlog.e("PhysicalFlingService", "ect fling list not exists!");
        }
        return result;
    }

    public boolean isSupportPhysicalFling(String packageName) {
        if (this.mIsBlackList) {
            return isSupportFromBlackList(packageName);
        }
        return isSupportFromWhiteList(packageName);
    }

    private boolean isSupportFromWhiteList(String packageName) {
        boolean systemApp = isSystemApp(packageName);
        List<String> list = this.mSpringEffectAppWhiteList;
        boolean whitelistApp = list != null ? list.contains(packageName) : false;
        List<String> list2 = this.mSpringEffectAppBlackList;
        boolean blacklistApp = list2 != null ? list2.contains(packageName) : false;
        if (this.DEBUG) {
            VSlog.d("PhysicalFlingService", "packageName = " + packageName + " , systemApp = " + systemApp + " , whitelistApp = " + whitelistApp + " , blacklistApp = " + blacklistApp);
        }
        return (systemApp || whitelistApp) && !blacklistApp;
    }

    private boolean isSupportFromBlackList(String packageName) {
        List<String> list = this.mSpringEffectAppBlackList;
        boolean blacklistApp = list != null ? list.contains(packageName) : false;
        if (this.DEBUG) {
            VSlog.d("PhysicalFlingService", "packageName = " + packageName + " , blacklistApp = " + blacklistApp);
        }
        return !blacklistApp;
    }

    private Application getApplication() {
        return Application.getApplicationInstance();
    }

    private boolean isSystemApp(String packageName) {
        if (getApplication() != null) {
            try {
                PackageInfo pInfo = getApplication().getPackageManager().getPackageInfo(packageName, 0);
                boolean isSystemPackageName = false;
                Iterator<String> it = DEFAULT_SYSTEM_PACKAGE_NAME_WHITE_LIST.iterator();
                while (it.hasNext()) {
                    String name = it.next();
                    isSystemPackageName = packageName.contains(name);
                    if (isSystemPackageName) {
                        break;
                    }
                }
                if (this.DEBUG) {
                    VSlog.d("PhysicalFlingService", "packageName = " + packageName + " , isSystemPackageName = " + isSystemPackageName);
                }
                if ((pInfo.applicationInfo.flags & 1) == 0) {
                    if ((pInfo.applicationInfo.flags & 128) == 0) {
                        return false;
                    }
                }
                return isSystemPackageName;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
}