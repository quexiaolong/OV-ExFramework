package com.vivo.services.configurationManager;

import android.content.ContentValues;
import android.os.Handler;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import com.vivo.common.utils.VLog;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import vivo.app.configuration.ContentValuesList;
import vivo.app.configuration.IConfigChangeCallback;
import vivo.app.configuration.StringList;
import vivo.app.configuration.Switch;

/* loaded from: classes.dex */
public class ConfigFileRecord {
    public static final int MASK_CONTENTVALUESLIST = 3;
    public static final int MASK_RAWFILECONTENT = 0;
    public static final int MASK_STRINGLIST = 2;
    public static final int MASK_SWITCH = 1;
    private static final String TAG = "ConfigurationManager";
    private static final String TAGOFRAWFILECONTENT = "ConfigurationManagerOfRawFileContent";
    List<Object> configs;
    List<Object> lastConfigs;
    final RemoteCallbackList<IConfigChangeCallback> mConfigChangeObservers;
    private ConfigFileObserver mConfigFileObserver;
    private final Object mDispatchChangeLock;
    private String mFilePath;
    final Object mLock;
    private int type;

    public boolean isRawFileContentConfigFile() {
        return this.type == 0;
    }

    public boolean isSwitchConfigFile() {
        return this.type == 1;
    }

    public boolean isStringListConfigFile() {
        return this.type == 2;
    }

    public boolean isContentValuesListConfigFile() {
        return this.type == 3;
    }

    public ConfigFileRecord(String path, int type) {
        this(path, type, null);
    }

    public ConfigFileRecord(String path, int type, List<Object> configs) {
        this.mLock = new Object();
        this.mDispatchChangeLock = new Object();
        this.mConfigChangeObservers = new RemoteCallbackList<>();
        this.mFilePath = path;
        this.type = type;
        this.configs = configs;
    }

    public String getFilePath() {
        return this.mFilePath;
    }

    public void setConfigList(List<Object> configs) {
        synchronized (this.mLock) {
            this.lastConfigs = this.configs;
            this.configs = configs;
        }
    }

    public List<String> computeChangedConfigNames() {
        List<ContentValuesList> last;
        List<ContentValuesList> current;
        List<StringList> last2;
        List<StringList> current2;
        List<Switch> last3;
        List<Switch> current3;
        ArrayList<String> changedNames = new ArrayList<>();
        if (this.lastConfigs == null) {
            return changedNames;
        }
        int i = this.type;
        if (i == 1) {
            synchronized (this.mLock) {
                last3 = ListConvertHelper.convertObjectList2SwitchList(this.lastConfigs);
                current3 = ListConvertHelper.convertObjectList2SwitchList(this.configs);
            }
            for (Switch w : last3) {
                boolean found = false;
                boolean isChanged = false;
                String name = w.getName();
                boolean isOn = w.isOn();
                Iterator<Switch> it = current3.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    Switch k = it.next();
                    if (name.equals(k.getName())) {
                        found = true;
                        isChanged = isOn != k.isOn();
                    }
                }
                if (isChanged || !found) {
                    changedNames.add(name);
                }
            }
            for (Switch k2 : current3) {
                boolean found2 = false;
                String name2 = k2.getName();
                Iterator<Switch> it2 = last3.iterator();
                while (true) {
                    if (!it2.hasNext()) {
                        break;
                    }
                    if (name2.equals(it2.next().getName())) {
                        found2 = true;
                        break;
                    }
                }
                if (!found2) {
                    changedNames.add(name2);
                }
            }
        } else if (i == 2) {
            synchronized (this.mLock) {
                last2 = ListConvertHelper.convertObjectList2StringList(this.lastConfigs);
                current2 = ListConvertHelper.convertObjectList2StringList(this.configs);
            }
            for (StringList w2 : last2) {
                boolean found3 = false;
                boolean isChanged2 = false;
                String name3 = w2.getName();
                List<String> values = w2.getValues();
                Iterator<StringList> it3 = current2.iterator();
                while (true) {
                    if (!it3.hasNext()) {
                        break;
                    }
                    StringList k3 = it3.next();
                    if (name3.equals(k3.getName())) {
                        found3 = true;
                        isChanged2 = ListConvertHelper.compareStringList(values, k3.getValues());
                        break;
                    }
                }
                if (isChanged2 || !found3) {
                    changedNames.add(name3);
                }
            }
            for (StringList k4 : current2) {
                boolean found4 = false;
                String name4 = k4.getName();
                Iterator<StringList> it4 = last2.iterator();
                while (true) {
                    if (!it4.hasNext()) {
                        break;
                    }
                    if (name4.equals(it4.next().getName())) {
                        found4 = true;
                        break;
                    }
                }
                if (!found4) {
                    changedNames.add(name4);
                }
            }
        } else if (i == 3) {
            synchronized (this.mLock) {
                last = ListConvertHelper.convertObjectList2ContentValuesList(this.lastConfigs);
                current = ListConvertHelper.convertObjectList2ContentValuesList(this.configs);
            }
            for (ContentValuesList w3 : last) {
                boolean found5 = false;
                boolean isChanged3 = false;
                String name5 = w3.getName();
                HashMap<String, ContentValues> values2 = w3.getValues();
                Iterator<ContentValuesList> it5 = current.iterator();
                while (true) {
                    if (!it5.hasNext()) {
                        break;
                    }
                    ContentValuesList k5 = it5.next();
                    if (name5.equals(k5.getName())) {
                        found5 = true;
                        isChanged3 = ListConvertHelper.compareContentValuesList(values2, k5.getValues());
                        break;
                    }
                }
                if (isChanged3 || !found5) {
                    changedNames.add(name5);
                }
            }
            for (ContentValuesList k6 : current) {
                boolean found6 = false;
                String name6 = k6.getName();
                Iterator<ContentValuesList> it6 = last.iterator();
                while (true) {
                    if (it6.hasNext()) {
                        if (name6.equals(it6.next().getName())) {
                            found6 = true;
                            break;
                        }
                    } else {
                        break;
                    }
                }
                if (!found6) {
                    changedNames.add(name6);
                }
            }
        }
        return changedNames;
    }

    public void startObserverConfigFile(Handler handler) {
        synchronized (this) {
            VLog.d(TAG, "startObserverConfigFile " + this.mFilePath);
            if (this.mConfigFileObserver != null) {
                this.mConfigFileObserver.stopWatching();
            }
            File file = new File(this.mFilePath);
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            ConfigFileObserver configFileObserver = new ConfigFileObserver(this.mFilePath, this.type, 1544, handler);
            this.mConfigFileObserver = configFileObserver;
            configFileObserver.startWatching();
        }
    }

    public boolean isCallbackRegistered(IConfigChangeCallback callback) {
        synchronized (this) {
            int i = this.mConfigChangeObservers.beginBroadcast();
            while (i > 0) {
                i--;
                IConfigChangeCallback observer = this.mConfigChangeObservers.getBroadcastItem(i);
                if (callback.asBinder() == observer.asBinder()) {
                    VLog.d(TAG, "callback " + callback.asBinder() + " is registered");
                    this.mConfigChangeObservers.finishBroadcast();
                    return true;
                }
            }
            this.mConfigChangeObservers.finishBroadcast();
            return false;
        }
    }

    public boolean addConfigChangeCallback(IConfigChangeCallback callback, String name) {
        boolean register;
        if (isCallbackRegistered(callback)) {
            VLog.d(TAG, "callback " + callback + " has registered, one ConfigurationObserver only observe one config...");
            return false;
        }
        synchronized (this) {
            register = this.mConfigChangeObservers.register(callback, name);
        }
        return register;
    }

    public boolean removeConfigChangeCallback(IConfigChangeCallback callback, String name) {
        boolean unregister;
        synchronized (this) {
            unregister = this.mConfigChangeObservers.unregister(callback);
        }
        return unregister;
    }

    public void dispatchConfigChanged() {
        List<String> names = computeChangedConfigNames();
        VLog.d(TAG, "dispatchConfigChanged " + this.mFilePath + " changed names={" + names + "}");
        if (names == null || names.size() == 0) {
            CommonUtils.log("changed names is null or empty, not dispatchConfigChanged");
            return;
        }
        synchronized (this.mDispatchChangeLock) {
            int i = this.mConfigChangeObservers.beginBroadcast();
            while (i > 0) {
                i--;
                IConfigChangeCallback observer = this.mConfigChangeObservers.getBroadcastItem(i);
                String name = (String) this.mConfigChangeObservers.getBroadcastCookie(i);
                if (names.contains(name) && observer != null) {
                    try {
                        observer.onConfigChange(this.mFilePath, name);
                    } catch (RemoteException e) {
                    }
                }
            }
            this.mConfigChangeObservers.finishBroadcast();
        }
    }

    public void dispatchConfigRawFileContentChanged() {
        VLog.d(TAGOFRAWFILECONTENT, "dispatchConfigRawFileContentChanged begin");
        synchronized (this.mDispatchChangeLock) {
            int i = this.mConfigChangeObservers.beginBroadcast();
            while (i > 0) {
                i--;
                IConfigChangeCallback observer = this.mConfigChangeObservers.getBroadcastItem(i);
                if (observer != null) {
                    try {
                        VLog.d(TAGOFRAWFILECONTENT, "dispatchConfigRawFileContentChanged file is changed" + this.mFilePath);
                        observer.onConfigChange(this.mFilePath, (String) null);
                    } catch (RemoteException e) {
                    }
                }
            }
            this.mConfigChangeObservers.finishBroadcast();
        }
    }

    public void dump(PrintWriter pw) {
        dump(pw, false);
    }

    public void dump(PrintWriter pw, boolean bRootDevDebug) {
        pw.println(this.mFilePath + "{ ");
        if (bRootDevDebug) {
            synchronized (this.mLock) {
                if (this.configs != null) {
                    if (isSwitchConfigFile()) {
                        for (Object o : this.configs) {
                            String str = (Switch) o;
                            pw.println((Object) (str != null ? str.toString() : str));
                        }
                    } else if (isStringListConfigFile()) {
                        for (Object o2 : this.configs) {
                            String str2 = (StringList) o2;
                            pw.println((Object) (str2 != null ? str2.toString() : str2));
                        }
                    } else if (isContentValuesListConfigFile()) {
                        for (Object o3 : this.configs) {
                            String str3 = (ContentValuesList) o3;
                            pw.println((Object) (str3 != null ? str3.toString() : str3));
                        }
                    }
                } else {
                    pw.println("nothing");
                }
            }
        } else {
            pw.println("not root dev");
        }
        pw.println(" }");
        pw.println("{");
        int i = this.mConfigChangeObservers.beginBroadcast();
        while (i > 0) {
            i--;
            IConfigChangeCallback observer = this.mConfigChangeObservers.getBroadcastItem(i);
            pw.println(((String) this.mConfigChangeObservers.getBroadcastCookie(i)) + " callback-->" + observer.asBinder());
        }
        this.mConfigChangeObservers.finishBroadcast();
        pw.println("}");
    }
}