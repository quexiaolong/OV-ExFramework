package com.vivo.services.themeicon;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import com.vivo.face.common.data.Constants;
import java.io.File;
import java.util.List;
import vivo.app.VivoFrameworkFactory;
import vivo.app.configuration.AbsConfigurationManager;
import vivo.app.configuration.ConfigurationObserver;
import vivo.app.configuration.StringList;
import vivo.app.themeicon.IThemeIconManager;
import vivo.util.VSlog;
import vivo.util.VgcUtil;

/* loaded from: classes.dex */
public class ThemeIconService extends IThemeIconManager.Stub {
    private static final String CURRENT_DESKTOP_TYPE = "current_desktop_type";
    private static final int DEFAULT_DESKTOP_TYPE = 0;
    private static final int DEFAULT_EXPLORE_STYLE = 1;
    private static final int DEFAULT_STYLE = 1;
    private static final int EXPLORE_DESKTOP_TYPE = 1;
    private static final int MASK_TYPE_0 = 0;
    private static final int MASK_TYPE_1 = 1;
    private static final int MASK_TYPE_2 = 2;
    private static final int MASK_TYPE_UNKNOWN = -1;
    private static final String STR_LIST_MASK_TOAST = "toast_mask";
    private static final String STR_LIST_MASK_TYPE_0 = "theme_icon_mask_type_0";
    private static final String STR_LIST_MASK_TYPE_1 = "theme_icon_mask_type_1";
    private static final String STR_LIST_MASK_TYPE_2 = "theme_icon_mask_type_2";
    private static final String STR_THEME_ICON_FILE = "data/bbkcore/theme_icon_list_1.0.xml";
    private static final String STR_TOAST_FILE = "data/bbkcore/toast_list.xml";
    private static final String THEME_DIR_PATH = "theme_dir_path";
    private static final String THEME_ICONS_STYLE = "theme_icons_style";
    private static final String THEME_ICONS_STYLE_EXPLORE = "theme_icons_style_explore";
    private ContentResolver mContentResolver;
    private Context mContext;
    private Handler mHandler;
    private static String defaultThemePath = "/system/etc/theme/";
    private static String oemThemePath = "/oem/etc/theme/";
    private static String onlineThemePath = "/data/bbkcore/theme/";
    private static int mThemeStyle = -1;
    private static int mDesktopType = -1;
    private final String TAG = "ThemeIconService";
    private final boolean DEBUG = SystemProperties.getBoolean("persist.sys.debug.themeicon", false);
    private final boolean DEBUG_TOAST = SystemProperties.getBoolean("persist.sys.debug.toast", false);
    private HandlerThread mHandlerThread = new HandlerThread("ThemeIconService");
    private VivoFrameworkFactory mVivoFrameworkFactory = null;
    private AbsConfigurationManager mConfigurationManager = null;
    private StringList mStringListMaskType0 = null;
    private StringList mStringListMaskType1 = null;
    private StringList mStringListMaskType2 = null;
    private List<String> mMaskTypeList0 = null;
    private List<String> mMaskTypeList1 = null;
    private List<String> mMaskTypeList2 = null;
    private StringList mStringListMaskToast = null;
    private List<String> mMaskToastList = null;
    private String colorTheme = SystemProperties.get("persist.sys.theme.color", "null");
    private int mUserId = 0;
    private ConfigurationObserver MaskTypeObserver0 = new ConfigurationObserver() { // from class: com.vivo.services.themeicon.ThemeIconService.2
        public void onConfigChange(String file, String name) {
            if (ThemeIconService.this.mConfigurationManager != null) {
                ThemeIconService themeIconService = ThemeIconService.this;
                themeIconService.mStringListMaskType0 = themeIconService.mConfigurationManager.getStringList(ThemeIconService.STR_THEME_ICON_FILE, ThemeIconService.STR_LIST_MASK_TYPE_0);
                ThemeIconService themeIconService2 = ThemeIconService.this;
                themeIconService2.mMaskTypeList0 = themeIconService2.mStringListMaskType0.getValues();
                if (ThemeIconService.this.DEBUG) {
                    VSlog.i("ThemeIconService", "getMaskTypeStringList, mStringListMaskType0 = " + ThemeIconService.this.mStringListMaskType0);
                }
            }
        }
    };
    private ConfigurationObserver MaskTypeObserver1 = new ConfigurationObserver() { // from class: com.vivo.services.themeicon.ThemeIconService.3
        public void onConfigChange(String file, String name) {
            if (ThemeIconService.this.mConfigurationManager != null) {
                ThemeIconService themeIconService = ThemeIconService.this;
                themeIconService.mStringListMaskType1 = themeIconService.mConfigurationManager.getStringList(ThemeIconService.STR_THEME_ICON_FILE, ThemeIconService.STR_LIST_MASK_TYPE_1);
                ThemeIconService themeIconService2 = ThemeIconService.this;
                themeIconService2.mMaskTypeList1 = themeIconService2.mStringListMaskType1.getValues();
                if (ThemeIconService.this.DEBUG) {
                    VSlog.i("ThemeIconService", "getMaskTypeStringList, mStringListMaskType1 = " + ThemeIconService.this.mStringListMaskType1);
                }
            }
        }
    };
    private ConfigurationObserver MaskTypeObserver2 = new ConfigurationObserver() { // from class: com.vivo.services.themeicon.ThemeIconService.4
        public void onConfigChange(String file, String name) {
            if (ThemeIconService.this.mConfigurationManager != null) {
                ThemeIconService themeIconService = ThemeIconService.this;
                themeIconService.mStringListMaskType2 = themeIconService.mConfigurationManager.getStringList(ThemeIconService.STR_THEME_ICON_FILE, ThemeIconService.STR_LIST_MASK_TYPE_2);
                ThemeIconService themeIconService2 = ThemeIconService.this;
                themeIconService2.mMaskTypeList2 = themeIconService2.mStringListMaskType2.getValues();
                if (ThemeIconService.this.DEBUG) {
                    VSlog.i("ThemeIconService", "getMaskTypeStringList, mStringListMaskType2 = " + ThemeIconService.this.mStringListMaskType2);
                }
            }
        }
    };
    private ConfigurationObserver MaskToastObserver = new ConfigurationObserver() { // from class: com.vivo.services.themeicon.ThemeIconService.5
        public void onConfigChange(String file, String name) {
            if (ThemeIconService.this.mConfigurationManager != null) {
                ThemeIconService themeIconService = ThemeIconService.this;
                themeIconService.mStringListMaskToast = themeIconService.mConfigurationManager.getStringList(ThemeIconService.STR_TOAST_FILE, ThemeIconService.STR_LIST_MASK_TOAST);
                ThemeIconService themeIconService2 = ThemeIconService.this;
                themeIconService2.mMaskToastList = themeIconService2.mStringListMaskToast.getValues();
                if (ThemeIconService.this.DEBUG_TOAST) {
                    VSlog.i("ThemeIconService", "getMaskToastStringList, mMaskToastList = " + ThemeIconService.this.mMaskToastList);
                }
            }
        }
    };

    public ThemeIconService(Context context) {
        this.mContext = null;
        this.mContentResolver = null;
        this.mHandler = null;
        this.mContext = context;
        if (context != null) {
            this.mContentResolver = context.getContentResolver();
        }
        this.mHandlerThread.start();
        ThemeIconHandler themeIconHandler = new ThemeIconHandler(this.mHandlerThread.getLooper());
        this.mHandler = themeIconHandler;
        themeIconHandler.post(new Runnable() { // from class: com.vivo.services.themeicon.ThemeIconService.1
            @Override // java.lang.Runnable
            public void run() {
                ThemeIconService.this.initMaskTypeList();
            }
        });
    }

    /* loaded from: classes.dex */
    private final class ThemeIconHandler extends Handler {
        public ThemeIconHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initMaskTypeList() {
        VivoFrameworkFactory frameworkFactoryImpl = VivoFrameworkFactory.getFrameworkFactoryImpl();
        this.mVivoFrameworkFactory = frameworkFactoryImpl;
        if (frameworkFactoryImpl != null) {
            this.mConfigurationManager = frameworkFactoryImpl.getConfigurationManager();
            getMaskTypeStringList();
            registerMaskTypeObserver();
            parseLocalConfigs();
            getMaskToastStringList();
            registerMaskToastObserver();
            parseLocalToastConfig();
            this.mUserId = getUserid();
        }
    }

    private void getMaskTypeStringList() {
        AbsConfigurationManager absConfigurationManager = this.mConfigurationManager;
        if (absConfigurationManager != null) {
            this.mStringListMaskType0 = absConfigurationManager.getStringList(STR_THEME_ICON_FILE, STR_LIST_MASK_TYPE_0);
            this.mStringListMaskType1 = this.mConfigurationManager.getStringList(STR_THEME_ICON_FILE, STR_LIST_MASK_TYPE_1);
            this.mStringListMaskType2 = this.mConfigurationManager.getStringList(STR_THEME_ICON_FILE, STR_LIST_MASK_TYPE_2);
            this.mMaskTypeList0 = this.mStringListMaskType0.getValues();
            this.mMaskTypeList1 = this.mStringListMaskType1.getValues();
            this.mMaskTypeList2 = this.mStringListMaskType2.getValues();
            if (this.DEBUG) {
                VSlog.i("ThemeIconService", "getMaskTypeStringList, mStringListMaskType0 = " + this.mStringListMaskType0 + ", mStringListMaskType1 = " + this.mStringListMaskType1 + ", mStringListMaskType2 = " + this.mStringListMaskType2);
            }
        }
    }

    private void registerMaskTypeObserver() {
        AbsConfigurationManager absConfigurationManager = this.mConfigurationManager;
        if (absConfigurationManager != null) {
            absConfigurationManager.registerObserver(this.mStringListMaskType0, this.MaskTypeObserver0);
            this.mConfigurationManager.registerObserver(this.mStringListMaskType1, this.MaskTypeObserver1);
            this.mConfigurationManager.registerObserver(this.mStringListMaskType2, this.MaskTypeObserver2);
        }
    }

    private int getMaskTypeByDictionary(String packageName) {
        int maskType = -1;
        if (packageName != null) {
            char head = packageName.charAt(packageName.length() - 1);
            if ((head >= 'a' && head <= 'm') || (head >= 'A' && head <= 'M')) {
                maskType = 1;
            } else {
                maskType = 2;
            }
            if (this.DEBUG) {
                VSlog.i("ThemeIconService", "getMaskTypeByDictionary, packageName = " + packageName + ", maskType = " + maskType);
            }
        }
        return maskType;
    }

    private int getMaskTypeFromStringList(String packageName) {
        int maskType = -1;
        List<String> list = this.mMaskTypeList0;
        if (list != null && list.contains(packageName)) {
            maskType = 1;
        } else {
            List<String> list2 = this.mMaskTypeList1;
            if (list2 != null && list2.contains(packageName)) {
                maskType = 1;
            } else {
                List<String> list3 = this.mMaskTypeList2;
                if (list3 != null && list3.contains(packageName)) {
                    maskType = 2;
                }
            }
        }
        if (this.DEBUG) {
            VSlog.i("ThemeIconService", "getMaskTypeFromStringList, packageName = " + packageName + ", maskType = " + maskType);
        }
        return maskType;
    }

    public int getMaskType(String packageName) {
        int maskType = -1;
        if (packageName != null && !TextUtils.isEmpty(packageName)) {
            int maskTypeFromList = getMaskTypeFromStringList(packageName);
            if (maskTypeFromList != -1) {
                maskType = maskTypeFromList;
            } else {
                maskType = getMaskTypeByDictionary(packageName);
            }
            if (this.DEBUG) {
                VSlog.i("ThemeIconService", "getMaskType, packageName = " + packageName + ", maskType = " + maskType);
            }
        }
        return maskType;
    }

    private void parseLocalConfigs() {
        if (this.mMaskTypeList0.isEmpty() && this.mMaskTypeList1.isEmpty() && this.mMaskTypeList2.isEmpty()) {
            try {
                XmlResourceParser xmlParser = this.mContext.getResources().getXml(51576838);
                List<String> maskTypeList = null;
                while (true) {
                    int xmlEventType = xmlParser.next();
                    if (xmlEventType == 1) {
                        break;
                    } else if (xmlEventType == 2) {
                        if ("list".equals(xmlParser.getName())) {
                            String list = xmlParser.getAttributeValue(0);
                            if (STR_LIST_MASK_TYPE_0.equals(list)) {
                                maskTypeList = this.mMaskTypeList0;
                            } else if (STR_LIST_MASK_TYPE_1.equals(list)) {
                                maskTypeList = this.mMaskTypeList1;
                            } else if (STR_LIST_MASK_TYPE_2.equals(list)) {
                                maskTypeList = this.mMaskTypeList2;
                            }
                        } else if ("item".equals(xmlParser.getName())) {
                            String item = xmlParser.getAttributeValue(0);
                            if (maskTypeList != null) {
                                maskTypeList.add(item);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                VSlog.e("ThemeIconService", "parseLocalConfigs " + e);
            }
            if (this.DEBUG) {
                VSlog.i("ThemeIconService", "parseLocalConfigs, mMaskTypeList0 = " + this.mMaskTypeList0 + ", mMaskTypeList1 = " + this.mMaskTypeList1 + ", mMaskTypeList2 = " + this.mMaskTypeList2);
            }
        }
    }

    private void getMaskToastStringList() {
        AbsConfigurationManager absConfigurationManager = this.mConfigurationManager;
        if (absConfigurationManager != null) {
            StringList stringList = absConfigurationManager.getStringList(STR_TOAST_FILE, STR_LIST_MASK_TOAST);
            this.mStringListMaskToast = stringList;
            this.mMaskToastList = stringList.getValues();
            if (this.DEBUG_TOAST) {
                VSlog.i("ThemeIconService", "getMaskToastStringList, mMaskToastList = " + this.mMaskToastList);
            }
        }
    }

    private void registerMaskToastObserver() {
        AbsConfigurationManager absConfigurationManager = this.mConfigurationManager;
        if (absConfigurationManager != null) {
            absConfigurationManager.registerObserver(this.mStringListMaskToast, this.MaskToastObserver);
        }
    }

    private void parseLocalToastConfig() {
        if (this.mMaskToastList.isEmpty()) {
            try {
                XmlResourceParser xmlParser = this.mContext.getResources().getXml(51576839);
                while (true) {
                    int xmlEventType = xmlParser.next();
                    if (xmlEventType == 1) {
                        break;
                    } else if (xmlEventType == 2 && "item".equals(xmlParser.getName())) {
                        String item = xmlParser.getAttributeValue(0);
                        if (this.mMaskToastList != null) {
                            this.mMaskToastList.add(item);
                        }
                    }
                }
            } catch (Exception e) {
                VSlog.e("ThemeIconService", "parseLocalToastConfig " + e);
            }
            if (this.DEBUG_TOAST) {
                VSlog.i("ThemeIconService", "parseLocalToastConfig, mMaskToastList = " + this.mMaskToastList);
            }
        }
    }

    public boolean isMaskToastString(String packageName) {
        boolean isMaskToastString = (this.mMaskToastList == null || packageName == null || TextUtils.isEmpty(packageName) || !this.mMaskToastList.contains(packageName)) ? false : true;
        if (this.DEBUG_TOAST) {
            VSlog.i("ThemeIconService", "isMaskToastString " + isMaskToastString + "  mMaskToastList:" + this.mMaskToastList);
        }
        return isMaskToastString;
    }

    private String calculateThemePath() {
        String onlineThemePathUser = onlineThemePath;
        if (this.mUserId != 0) {
            onlineThemePathUser = onlineThemePath + this.mUserId + "/";
        }
        String path = onlineThemePathUser + "style/" + mThemeStyle + "/";
        if (new File(path + "launcher/icon_mask.png").exists()) {
            return path;
        }
        String path2 = onlineThemePathUser;
        if (new File(path2 + "launcher/icon_mask.png").exists()) {
            return path2;
        }
        try {
            path2 = VgcUtil.getFile(THEME_DIR_PATH, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        } catch (Exception e) {
            VSlog.w("ThemeIconService", "get theme_dir_path error");
        }
        if (this.DEBUG) {
            VSlog.d("ThemeIconService", "vgcThemePath = " + path2);
        }
        if (new File(path2 + "/launcher/icon_mask.png").exists()) {
            return path2 + "/";
        }
        String path3 = oemThemePath;
        if (new File(path3 + "launcher/icon_mask.png").exists()) {
            return path3;
        }
        if (this.colorTheme != null) {
            String path4 = path3 + "custom/" + this.colorTheme + "/";
            if (new File(path4 + "launcher/icon_mask.png").exists()) {
                return path4;
            }
        }
        String path5 = defaultThemePath + "style/" + mThemeStyle + "/";
        if (new File(path5 + "launcher/icon_mask.png").exists()) {
            return path5;
        }
        String path6 = defaultThemePath;
        if (this.colorTheme != null) {
            String path7 = path6 + "custom/" + this.colorTheme + "/";
            if (new File(path7 + "launcher/icon_mask.png").exists()) {
                return path7;
            }
        }
        return defaultThemePath;
    }

    public String getThemePath() {
        int userid = getUserid();
        this.mUserId = userid;
        int intForUser = Settings.System.getIntForUser(this.mContentResolver, CURRENT_DESKTOP_TYPE, 0, userid);
        mDesktopType = intForUser;
        mThemeStyle = 1 == intForUser ? Settings.System.getIntForUser(this.mContentResolver, THEME_ICONS_STYLE_EXPLORE, 1, this.mUserId) : Settings.System.getIntForUser(this.mContentResolver, THEME_ICONS_STYLE, 1, this.mUserId);
        if (this.DEBUG) {
            VSlog.d("ThemeIconService", "mDesktopType = " + mDesktopType + ", mThemeStyle = " + mThemeStyle);
        }
        return calculateThemePath();
    }

    private int getUserid() {
        long token = Binder.clearCallingIdentity();
        try {
            int userid = ActivityManager.getCurrentUser();
            if (this.DEBUG) {
                VSlog.d("ThemeIconService", "ThemeIconService getUserid userid = " + userid);
            }
            return userid;
        } catch (Exception e) {
            VSlog.e("ThemeIconService", "ThemeIconService getUserid error e = " + e);
            return 0;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }
}