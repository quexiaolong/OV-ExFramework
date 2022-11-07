package com.vivo.services.configurationManager;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.text.TextUtils;
import com.android.internal.util.DumpUtils;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import vivo.app.configuration.BaseConfig;
import vivo.app.configuration.ContentValuesList;
import vivo.app.configuration.IConfigChangeCallback;
import vivo.app.configuration.IConfigurationManager;
import vivo.app.configuration.RawFileContent;
import vivo.app.configuration.StringList;
import vivo.app.configuration.Switch;

/* loaded from: classes.dex */
public class ConfigurationManagerImpl extends IConfigurationManager.Stub {
    private static final String CMS_VERSION_CODE = "2.0";
    private static final String CMS_VERSION_PROP_NAME = "ro.cms.version";
    private static final String DEFAULT_SYSTEM_CONFIG_DIR = "/data/bbkcore/";
    private static final int MSG_RE_OBSERVER_CONFIG_FILE = 1;
    static final String TAG = "ConfigurationManager";
    static final String TAGOFRAWFILECONTENT = "ConfigurationManagerOfRawFileContent";
    private static ConfigurationManagerImpl sInstance;
    private Context mContext;
    private Handler mMainHandler;
    private HandlerThread mMainHandlerThread;
    private final HashMap<String, ConfigFileRecord> mSwitchConfigFileMap = new HashMap<>();
    private final HashMap<String, ConfigFileRecord> mStringListConfigFileMap = new HashMap<>();
    private final HashMap<String, ConfigFileRecord> mContentValuesListConfigFileMap = new HashMap<>();
    private final HashMap<String, ConfigFileRecord> mRawFileContentConfigFileMap = new HashMap<>();

    private List<Object> parseSystemConfigFile(String filePath, int type) {
        File file = new File(filePath);
        if (!file.exists()) {
            VLog.e(TAG, "system config file is not existed, we create the default system config file");
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ConfigFileRecord record = null;
        HashMap<String, ConfigFileRecord> tmp = getConfigMap(type);
        if (tmp != null) {
            ConfigFileRecord record2 = tmp.get(filePath);
            record = record2;
        }
        List<Object> list = null;
        long beginTime = System.currentTimeMillis();
        if (type == 1) {
            list = ListConvertHelper.convertSwitchList2ObjectList(XmlPullParserHelper.getSwitchListFromFile(filePath, this.mContext));
        } else if (type == 2) {
            list = ListConvertHelper.convertStringList2ObjectList(XmlPullParserHelper.getStringListFromFile(filePath, this.mContext));
        } else if (type == 3) {
            list = ListConvertHelper.convertContentValuesList2ObjectList(XmlPullParserHelper.getContentValuesListFromFile(filePath, this.mContext));
        }
        VLog.d(TAG, "parse config file " + filePath + " costs " + (System.currentTimeMillis() - beginTime) + "ms!!!");
        if (record == null) {
            record = new ConfigFileRecord(filePath, type, list);
            record.startObserverConfigFile(this.mMainHandler);
        } else {
            record.setConfigList(list);
        }
        if (tmp != null) {
            tmp.put(filePath, record);
        }
        return list;
    }

    private String parseSystemConfigRawFileContent(String filePath, int type) {
        File file = new File(filePath);
        if (!file.exists()) {
            VLog.e(TAG, "system config raw file is not existed, we create the default system config raw file");
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ConfigFileRecord record = null;
        HashMap<String, ConfigFileRecord> tmp = getConfigMap(type);
        if (tmp != null) {
            ConfigFileRecord record2 = tmp.get(filePath);
            record = record2;
        }
        long beginTime = System.currentTimeMillis();
        String filecontent = getRawFileContentFromFile(filePath, this.mContext);
        VLog.d(TAG, "parse config raw file " + filePath + " costs " + (System.currentTimeMillis() - beginTime) + "ms!!!");
        if (record == null) {
            record = new ConfigFileRecord(filePath, type);
            record.startObserverConfigFile(this.mMainHandler);
        } else {
            VLog.d(TAG, "parseSystemConfigRawFileContent record is already existed ");
        }
        if (tmp != null) {
            tmp.put(filePath, record);
        }
        return filecontent;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reparseSystemConfigFile(String filePath, int type) {
        VLog.d(TAG, "reparseSystemConfigFile filePath=" + filePath + " type=" + type);
        HashMap<String, ConfigFileRecord> tmp = getConfigMap(type);
        ConfigFileRecord record = null;
        if (tmp != null) {
            synchronized (tmp) {
                if (type != 0) {
                    parseSystemConfigFile(filePath, type);
                } else {
                    parseSystemConfigRawFileContent(filePath, type);
                    VLog.d(TAGOFRAWFILECONTENT, "reparseSystemConfigFile begin parse");
                }
                record = tmp.get(filePath);
            }
        }
        if (record != null) {
            if (type != 0) {
                record.dispatchConfigChanged();
                return;
            }
            VLog.d(TAGOFRAWFILECONTENT, "reparseSystemConfigFile begin dispatchConfigRawFileContentChanged");
            record.dispatchConfigRawFileContentChanged();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public HashMap<String, ConfigFileRecord> getConfigMap(int type) {
        if (type == 0) {
            HashMap<String, ConfigFileRecord> tmp = this.mRawFileContentConfigFileMap;
            return tmp;
        } else if (type == 1) {
            HashMap<String, ConfigFileRecord> tmp2 = this.mSwitchConfigFileMap;
            return tmp2;
        } else if (type == 2) {
            HashMap<String, ConfigFileRecord> tmp3 = this.mStringListConfigFileMap;
            return tmp3;
        } else if (type != 3) {
            return null;
        } else {
            HashMap<String, ConfigFileRecord> tmp4 = this.mContentValuesListConfigFileMap;
            return tmp4;
        }
    }

    private ConfigurationManagerImpl(Context context) {
        this.mContext = context;
        HandlerThread handlerThread = new HandlerThread(TAG);
        this.mMainHandlerThread = handlerThread;
        handlerThread.start();
        this.mMainHandler = new MainHandler(this.mMainHandlerThread.getLooper());
        if (TextUtils.isEmpty(SystemProperties.get(CMS_VERSION_PROP_NAME, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK))) {
            SystemProperties.set(CMS_VERSION_PROP_NAME, CMS_VERSION_CODE);
        }
        DecryptUtils.prepareDecryptV2KeyFile();
    }

    public static synchronized ConfigurationManagerImpl getInstance(Context context) {
        ConfigurationManagerImpl configurationManagerImpl;
        synchronized (ConfigurationManagerImpl.class) {
            if (sInstance == null) {
                sInstance = new ConfigurationManagerImpl(context);
            }
            configurationManagerImpl = sInstance;
        }
        return configurationManagerImpl;
    }

    private RawFileContent getRawfilecontentByName(String filepath) {
        String filecontent;
        synchronized (this.mRawFileContentConfigFileMap) {
            filecontent = parseSystemConfigRawFileContent(filepath, 0);
        }
        RawFileContent rawfilecontent = new RawFileContent(filepath, filecontent, filepath, false);
        return rawfilecontent;
    }

    public static String getRawFileContentFromFile(String path, Context context) {
        File file = new File(path);
        if (file.exists() && file.isFile() && file.canRead()) {
            String fileContent = null;
            if (file.length() > 512000) {
                VLog.e(TAG, "dec raw config file failed,more than 500kb");
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
                VLog.e(TAG, "dec raw config file failed");
            }
            if (TextUtils.isEmpty(fileContent)) {
                return null;
            }
            return fileContent;
        }
        return null;
    }

    private Switch getSwitchByName(String file, String name) {
        List<Switch> list;
        synchronized (this.mSwitchConfigFileMap) {
            ConfigFileRecord record = this.mSwitchConfigFileMap.get(file);
            if (record == null) {
                parseSystemConfigFile(file, 1);
                record = this.mSwitchConfigFileMap.get(file);
                if (record == null) {
                    return null;
                }
            }
            synchronized (record.mLock) {
                list = ListConvertHelper.convertObjectList2SwitchList(record.configs);
            }
            for (Switch w : list) {
                if (name.equals(w.getName())) {
                    return w;
                }
            }
            return new Switch(name, file, true);
        }
    }

    private StringList getStringListByName(String file, String name) {
        List<StringList> list;
        synchronized (this.mStringListConfigFileMap) {
            ConfigFileRecord record = this.mStringListConfigFileMap.get(file);
            if (record == null) {
                parseSystemConfigFile(file, 2);
                record = this.mStringListConfigFileMap.get(file);
                if (record == null) {
                    return null;
                }
            }
            synchronized (record.mLock) {
                list = ListConvertHelper.convertObjectList2StringList(record.configs);
            }
            for (StringList w : list) {
                if (name.equals(w.getName())) {
                    return w;
                }
            }
            return new StringList(name, file, true);
        }
    }

    private ContentValuesList getContentValuesListByName(String file, String name) {
        List<ContentValuesList> list;
        synchronized (this.mContentValuesListConfigFileMap) {
            ConfigFileRecord record = this.mContentValuesListConfigFileMap.get(file);
            if (record == null) {
                parseSystemConfigFile(file, 3);
                record = this.mContentValuesListConfigFileMap.get(file);
                if (record == null) {
                    return null;
                }
            }
            synchronized (record.mLock) {
                list = ListConvertHelper.convertObjectList2ContentValuesList(record.configs);
            }
            for (ContentValuesList w : list) {
                if (name.equals(w.getName())) {
                    return w;
                }
            }
            return new ContentValuesList(name, file, true);
        }
    }

    public RawFileContent getRawFileContent(String fileName) {
        if (TextUtils.isEmpty(fileName) || !isSafePathOrSystemApp(fileName).booleanValue()) {
            return null;
        }
        RawFileContent rawfilecontent = getRawfilecontentByName(fileName);
        return rawfilecontent;
    }

    public Switch getSwitchState(String switchName, String fileName) {
        if (TextUtils.isEmpty(fileName) || TextUtils.isEmpty(switchName) || !isSafePathOrSystemApp(fileName).booleanValue()) {
            return null;
        }
        return getSwitchByName(fileName, switchName);
    }

    public StringList getStandardList(String name, String fileName) {
        if (TextUtils.isEmpty(fileName) || TextUtils.isEmpty(name) || !isSafePathOrSystemApp(fileName).booleanValue()) {
            return null;
        }
        return getStringListByName(fileName, name);
    }

    public ContentValuesList getContentValuesList(String name, String fileName) {
        if (TextUtils.isEmpty(fileName) || TextUtils.isEmpty(name) || !isSafePathOrSystemApp(fileName).booleanValue()) {
            return null;
        }
        return getContentValuesListByName(fileName, name);
    }

    public Boolean isSafePathOrSystemApp(String path) {
        if (!path.startsWith(DEFAULT_SYSTEM_CONFIG_DIR) && !path.startsWith("data/bbkcore/")) {
            VLog.d(TAG, "decryptFile path is not right path,is not safe !!!!!");
            return false;
        }
        int uid = Binder.getCallingUid();
        PackageManager packageManager = this.mContext.getPackageManager();
        if (packageManager == null) {
            VLog.d(TAG, "decryptFile packageManager is null !!!!!");
            return false;
        } else if (packageManager.checkSignatures(uid, 1000) != 0) {
            VLog.d(TAG, "decryptFile package is not a systemapp !!!!!");
            VLog.d(TAG, "decryptFile getuid is " + uid);
            return false;
        } else {
            return true;
        }
    }

    public boolean registerCallback(BaseConfig config, int type, IConfigChangeCallback callback) {
        if (callback == null || config.isInvalid()) {
            return false;
        }
        ConfigFileRecord record = null;
        HashMap<String, ConfigFileRecord> tmp = getConfigMap(type);
        if (tmp != null) {
            synchronized (tmp) {
                record = tmp.get(config.getConfigFilePath());
            }
        }
        if (record == null) {
            return false;
        }
        return record.addConfigChangeCallback(callback, config.getName());
    }

    public void unregisterCallback(BaseConfig config, int type, IConfigChangeCallback callback) {
        if (callback != null && !config.isInvalid()) {
            ConfigFileRecord record = null;
            HashMap<String, ConfigFileRecord> tmp = getConfigMap(type);
            if (tmp != null) {
                synchronized (tmp) {
                    record = tmp.get(config.getConfigFilePath());
                }
            }
            if (record != null) {
                record.removeConfigChangeCallback(callback, config.getName());
            }
        }
    }

    /* loaded from: classes.dex */
    private class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            ConfigFileRecord record;
            int i = msg.what;
            if (i == 1) {
                String path = (String) msg.obj;
                int type2 = msg.arg1;
                HashMap<String, ConfigFileRecord> tmp = ConfigurationManagerImpl.this.getConfigMap(type2);
                if (tmp != null) {
                    synchronized (tmp) {
                        record = tmp.get(path);
                    }
                    if (record != null) {
                        record.startObserverConfigFile(this);
                    }
                }
            } else if (i == 1000) {
                String file = (String) msg.obj;
                Message reObserveMsg = Message.obtain(this, 1, file);
                reObserveMsg.arg1 = msg.arg1;
                sendMessage(reObserveMsg);
            } else if (i == 1001) {
                String file2 = (String) msg.obj;
                int type = msg.arg1;
                VLog.d(ConfigurationManagerImpl.TAGOFRAWFILECONTENT, "receiver file close write" + file2 + type);
                ConfigurationManagerImpl.this.reparseSystemConfigFile(file2, type);
            }
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw) && CommonUtils.DEBUG) {
            pw.println("Configuration Status:");
            pw.println("isAbeSupportDecryptV2=" + DecryptUtils.isAbeSupportDecryptV2());
            pw.println("Algorithm=" + CommonUtils.isCBC(CommonUtils.getCipherAlgorithm()));
            pw.println("switch configs:");
            boolean bRootDevDebug = CommonUtils.DEBUG;
            synchronized (this.mSwitchConfigFileMap) {
                for (String key : this.mSwitchConfigFileMap.keySet()) {
                    ConfigFileRecord r = this.mSwitchConfigFileMap.get(key);
                    r.dump(pw, bRootDevDebug);
                }
            }
            pw.println("********************************************************");
            pw.println("stringlist configs:");
            synchronized (this.mStringListConfigFileMap) {
                for (String key2 : this.mStringListConfigFileMap.keySet()) {
                    ConfigFileRecord r2 = this.mStringListConfigFileMap.get(key2);
                    r2.dump(pw, bRootDevDebug);
                }
            }
            pw.println("********************************************************");
            pw.println("contentvalues configs:");
            synchronized (this.mContentValuesListConfigFileMap) {
                for (String key3 : this.mContentValuesListConfigFileMap.keySet()) {
                    ConfigFileRecord r3 = this.mContentValuesListConfigFileMap.get(key3);
                    r3.dump(pw, bRootDevDebug);
                }
            }
            pw.println("********************************************************");
        }
    }
}