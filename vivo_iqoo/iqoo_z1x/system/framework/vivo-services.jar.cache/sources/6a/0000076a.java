package com.vivo.services.rms.sp.config;

import android.content.Context;
import android.os.FileObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Xml;
import com.vivo.face.common.data.Constants;
import com.vivo.services.rms.sp.SpManagerImpl;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class ConfigManager {
    private static final String CONFIG_BBKCORE_FILE_PATH = "/data/bbkcore/super_process_config.new.xml";
    public static final int CONFIG_CHANGED_REASON_INIT = 1;
    public static final int CONFIG_CHANGED_REASON_PUSH = 2;
    private static final String CONFIG_DEFAULT_FILE_PATH = "/system/etc/super_process_config.new.xml";
    private static final String TAG = "SpManager";
    private static FileObserver mConfigFileObserver = null;
    private ConfigChangedCallback mConfigChangedCallback;
    private Handler mHandler;
    private boolean mIsEnabled;
    private final Object mLock;
    private final ArrayList<PackageConfig> mParsedConfigs;
    private Context mSystemContext;
    private final Runnable reloadRunnable;
    private final Runnable rewatchRunnable;

    /* loaded from: classes.dex */
    public interface ConfigChangedCallback {
        void callback(int i);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public interface IVersion {
        boolean isIn(long j);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class ConfigManagerHandler extends Handler {
        private ConfigManagerHandler(Looper looper) {
            super(looper);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class SingleVersion implements IVersion {
        private final long mInternalVersion;

        private SingleVersion(long version) {
            this.mInternalVersion = version;
        }

        @Override // com.vivo.services.rms.sp.config.ConfigManager.IVersion
        public boolean isIn(long version) {
            return this.mInternalVersion == version;
        }

        public String toString() {
            return "[" + this.mInternalVersion + "]";
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class VersionRange implements IVersion {
        private final long mMax;
        private final long mMin;

        private VersionRange(long min, long max) {
            this.mMin = min;
            this.mMax = max;
        }

        @Override // com.vivo.services.rms.sp.config.ConfigManager.IVersion
        public boolean isIn(long version) {
            return version <= this.mMax && version >= this.mMin;
        }

        public String toString() {
            if (this.mMax == Long.MAX_VALUE) {
                return "[>=" + this.mMin + "]";
            }
            return "[" + this.mMin + ", " + this.mMax + "]";
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class AttrList<T> {
        boolean allList;
        List<T> list;

        private AttrList(boolean all, List<T> list) {
            this.allList = all;
            this.list = list;
        }

        public String toString() {
            if (this.allList) {
                return "[ALL]";
            }
            List<T> list = this.list;
            if (list == null || list.size() <= 0) {
                return "[ERROR]";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < this.list.size() - 1; i++) {
                sb.append(this.list.get(i));
                sb.append("|");
            }
            List<T> list2 = this.list;
            sb.append(list2.get(list2.size() - 1));
            sb.append("]");
            return sb.toString();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class DeviceList extends AttrList<String> {
        private DeviceList(boolean all, List<String> list) {
            super(all, list);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean inList(String deviceName) {
            return this.allList || this.list.contains(deviceName);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class VersionList extends AttrList<IVersion> {
        private VersionList(boolean all, List<IVersion> list) {
            super(all, list);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean inList(long versionCode) {
            if (this.allList) {
                return true;
            }
            for (T v : this.list) {
                if (v.isIn(versionCode)) {
                    return true;
                }
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class PackageConfig {
        private DeviceList disableList;
        private DeviceList enableList;
        private VersionList fwkVersions;
        private String pkgName;
        private VersionList pkgVersions;

        private PackageConfig() {
        }

        public String toString() {
            Object[] objArr = new Object[5];
            objArr[0] = this.pkgName;
            VersionList versionList = this.pkgVersions;
            objArr[1] = versionList == null ? "NOT DEFINED" : versionList.toString();
            VersionList versionList2 = this.fwkVersions;
            objArr[2] = versionList2 == null ? "NOT DEFINED" : versionList2.toString();
            DeviceList deviceList = this.enableList;
            objArr[3] = deviceList == null ? "NOT DEFINED" : deviceList.toString();
            DeviceList deviceList2 = this.disableList;
            objArr[4] = deviceList2 != null ? deviceList2.toString() : "NOT DEFINED";
            return String.format("-- %s:\n\tpackage version:%s\n\tframework version:%s\n\tenable list:%s\n\tdisbale list:%s", objArr);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class XmlPath extends ArrayList<String> {
        private XmlPath() {
        }

        public void backward() {
            remove(size() - 1);
        }

        public void forward(String tagName) {
            add(tagName);
        }

        @Override // java.util.AbstractCollection
        public String toString() {
            String prefix = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            StringBuilder builder = new StringBuilder();
            Iterator<String> it = iterator();
            while (it.hasNext()) {
                String tag = it.next();
                builder.append(prefix);
                prefix = ".";
                builder.append(tag);
            }
            return builder.toString();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Instance {
        private static final ConfigManager INSTANCE = new ConfigManager();

        private Instance() {
        }
    }

    public static ConfigManager getInstance() {
        return Instance.INSTANCE;
    }

    private ConfigManager() {
        this.mLock = new Object();
        this.mParsedConfigs = new ArrayList<>();
        this.mIsEnabled = false;
        this.mConfigChangedCallback = null;
        this.reloadRunnable = new Runnable() { // from class: com.vivo.services.rms.sp.config.ConfigManager.1
            @Override // java.lang.Runnable
            public void run() {
                ConfigManager.this.loadList(2);
            }
        };
        this.rewatchRunnable = new Runnable() { // from class: com.vivo.services.rms.sp.config.ConfigManager.2
            @Override // java.lang.Runnable
            public void run() {
                ConfigManager.this.loadList(2);
                ConfigManager.this.startWatching();
            }
        };
        this.mSystemContext = null;
    }

    public void setConfigChangedCallback(ConfigChangedCallback callback) {
        this.mConfigChangedCallback = callback;
    }

    private void handleConfigChangedChange(int reason) {
        ConfigChangedCallback configChangedCallback = this.mConfigChangedCallback;
        if (configChangedCallback != null) {
            configChangedCallback.callback(reason);
        }
    }

    public void initialize(Context context) {
        this.mSystemContext = context;
        loadList(1);
        startWatching();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startLoadingThreadIfNeeded() {
        if (this.mHandler != null) {
            return;
        }
        synchronized (ConfigManager.class) {
            HandlerThread handlerThread = new HandlerThread("sp_config");
            handlerThread.start();
            this.mHandler = new ConfigManagerHandler(handlerThread.getLooper());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startWatching() {
        FileObserver fileObserver = mConfigFileObserver;
        if (fileObserver != null) {
            fileObserver.stopWatching();
        }
        File listFile = new File(CONFIG_BBKCORE_FILE_PATH);
        if (!listFile.exists()) {
            try {
                if (!listFile.createNewFile()) {
                    VSlog.e("SpManager", "create watching file failed.");
                    return;
                }
            } catch (Exception e) {
                VSlog.e("SpManager", "create watching file failed.", e);
                return;
            }
        }
        FileObserver fileObserver2 = new FileObserver(listFile, 1544) { // from class: com.vivo.services.rms.sp.config.ConfigManager.3
            @Override // android.os.FileObserver
            public void onEvent(int event, String path) {
                VSlog.d("SpManager", "event:" + event + " path:" + path);
                ConfigManager.this.startLoadingThreadIfNeeded();
                if (event == 8) {
                    ConfigManager.this.mHandler.removeCallbacks(ConfigManager.this.reloadRunnable);
                    ConfigManager.this.mHandler.removeCallbacks(ConfigManager.this.rewatchRunnable);
                    ConfigManager.this.mHandler.post(ConfigManager.this.reloadRunnable);
                } else if (event == 512 || event == 1024) {
                    ConfigManager.this.mHandler.removeCallbacks(ConfigManager.this.reloadRunnable);
                    ConfigManager.this.mHandler.removeCallbacks(ConfigManager.this.rewatchRunnable);
                    ConfigManager.this.mHandler.postDelayed(ConfigManager.this.rewatchRunnable, 5000L);
                }
            }
        };
        mConfigFileObserver = fileObserver2;
        fileObserver2.startWatching();
    }

    private static int parseVersion(String filename) {
        File listFile = new File(filename);
        if (listFile.exists()) {
            XmlPullParser parser = Xml.newPullParser();
            FileReader inFile = null;
            try {
                inFile = new FileReader(listFile);
                parser.setInput(inFile);
                while (parser.getEventType() != 1) {
                    try {
                        if (parser.getEventType() == 2 && "config".equals(parser.getName())) {
                            return Integer.parseInt(parser.getAttributeValue(null, "version"));
                        }
                        parser.next();
                    } catch (Exception e) {
                        VSlog.e("SpManager", "Parse config failed.", e);
                        return -1;
                    } finally {
                        IoUtils.closeQuietly(inFile);
                    }
                }
                return -1;
            } catch (Exception e2) {
                VSlog.e("SpManager", "Setup input stream failed.", e2);
                return -1;
            }
        }
        return -1;
    }

    private static VersionList parseVersionString(String versionString) {
        long end;
        if (versionString == null) {
            return null;
        }
        if (versionString.trim().equals("*")) {
            return new VersionList(true, null);
        }
        ArrayList<IVersion> list = new ArrayList<>();
        try {
            String[] split = versionString.split(",");
            int length = split.length;
            char c = 0;
            int i = 0;
            while (i < length) {
                String part = split[i];
                if (part.contains("-")) {
                    String[] startEnd = part.split("-");
                    long start = Long.parseLong(startEnd[c]);
                    if (startEnd.length < 2) {
                        end = Long.MAX_VALUE;
                    } else {
                        long end2 = Long.parseLong(startEnd[1]);
                        end = end2;
                    }
                    if (start <= end) {
                        list.add(new VersionRange(start, end));
                    }
                } else {
                    list.add(new SingleVersion(Long.parseLong(part)));
                }
                i++;
                c = 0;
            }
            return new VersionList(false, list);
        } catch (Exception e) {
            return null;
        }
    }

    private DeviceList parseDeviceList(String deviceString) {
        if (deviceString == null) {
            return null;
        }
        String valuated = deviceString.replaceAll("[^\\p{ASCII}]", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK).replace("\n", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK).replace("\t", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK).trim();
        if (valuated.equals("*")) {
            return new DeviceList(true, null);
        }
        String[] splited = valuated.split(",");
        List<String> devices = new ArrayList<>();
        for (String s : splited) {
            if (s != null && !TextUtils.isEmpty(s.trim())) {
                devices.add(s.trim());
            }
        }
        return new DeviceList(false, devices);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void loadList(int reason) {
        int bbkcoreVersion;
        int internalVersion;
        DeviceList disableList;
        int bbkcoreVersion2 = parseVersion(CONFIG_BBKCORE_FILE_PATH);
        int internalVersion2 = parseVersion(CONFIG_DEFAULT_FILE_PATH);
        if (bbkcoreVersion2 < 0 && internalVersion2 < 0) {
            return;
        }
        String fileToUse = bbkcoreVersion2 > internalVersion2 ? CONFIG_BBKCORE_FILE_PATH : CONFIG_DEFAULT_FILE_PATH;
        XmlPullParser parser = Xml.newPullParser();
        FileReader inFile = null;
        try {
            inFile = new FileReader(new File(fileToUse));
            parser.setInput(inFile);
            boolean mainSwitch = false;
            List<PackageConfig> pkgConfigs = new ArrayList<>();
            try {
                try {
                    String str = null;
                    XmlPath path = new XmlPath();
                    while (parser.getEventType() != 1) {
                        if (parser.getEventType() == 2) {
                            path.forward(parser.getName());
                            if ("config.sps".equals(path.toString())) {
                                try {
                                    mainSwitch = !"off".equals(parser.getAttributeValue(str, "switch"));
                                    bbkcoreVersion = bbkcoreVersion2;
                                    internalVersion = internalVersion2;
                                } catch (Exception e) {
                                    e = e;
                                    VSlog.e("SpManager", "Parse config failed.", e);
                                    IoUtils.closeQuietly(inFile);
                                    return;
                                } catch (Throwable th) {
                                    e = th;
                                    IoUtils.closeQuietly(inFile);
                                    throw e;
                                }
                            } else if ("config.packages.package".equals(path.toString())) {
                                String packageName = parser.getAttributeValue(str, "name");
                                String pkgVersionString = parser.getAttributeValue(str, "pkg-version");
                                VersionList pkgVersions = parseVersionString(pkgVersionString);
                                String fwkVersionString = parser.getAttributeValue(str, "fwk-version");
                                VersionList fwkVersions = parseVersionString(fwkVersionString);
                                String enableListString = parser.getAttributeValue(str, "enable-on");
                                DeviceList enableList = parseDeviceList(enableListString);
                                bbkcoreVersion = bbkcoreVersion2;
                                try {
                                    String disableListString = parser.getAttributeValue(null, "disable-on");
                                    disableList = parseDeviceList(disableListString);
                                    internalVersion = internalVersion2;
                                } catch (Exception e2) {
                                    e = e2;
                                    VSlog.e("SpManager", "Parse config failed.", e);
                                    IoUtils.closeQuietly(inFile);
                                    return;
                                } catch (Throwable th2) {
                                    e = th2;
                                    IoUtils.closeQuietly(inFile);
                                    throw e;
                                }
                                try {
                                    PackageConfig p = new PackageConfig();
                                    p.pkgName = packageName;
                                    p.pkgVersions = pkgVersions;
                                    p.fwkVersions = fwkVersions;
                                    p.enableList = enableList;
                                    p.disableList = disableList;
                                    pkgConfigs.add(p);
                                } catch (Exception e3) {
                                    e = e3;
                                    VSlog.e("SpManager", "Parse config failed.", e);
                                    IoUtils.closeQuietly(inFile);
                                    return;
                                }
                            } else {
                                bbkcoreVersion = bbkcoreVersion2;
                                internalVersion = internalVersion2;
                            }
                        } else {
                            bbkcoreVersion = bbkcoreVersion2;
                            internalVersion = internalVersion2;
                            if (parser.getEventType() == 3) {
                                path.backward();
                            }
                        }
                        parser.next();
                        bbkcoreVersion2 = bbkcoreVersion;
                        internalVersion2 = internalVersion;
                        str = null;
                    }
                    IoUtils.closeQuietly(inFile);
                    synchronized (this.mLock) {
                        this.mParsedConfigs.clear();
                        this.mParsedConfigs.addAll(pkgConfigs);
                        this.mIsEnabled = mainSwitch;
                    }
                    handleConfigChangedChange(reason);
                } catch (Exception e4) {
                    e = e4;
                } catch (Throwable th3) {
                    e = th3;
                }
            } catch (Throwable th4) {
                e = th4;
            }
        } catch (Exception e5) {
            VSlog.e("SpManager", "Setup input stream failed.", e5);
            IoUtils.closeQuietly(inFile);
        }
    }

    public boolean getIsEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mIsEnabled;
        }
        return z;
    }

    public boolean isPackageEnabled(String pkgName, long versionCode) {
        synchronized (this.mLock) {
            Iterator<PackageConfig> it = this.mParsedConfigs.iterator();
            while (it.hasNext()) {
                PackageConfig p = it.next();
                if (p.pkgName.equals(pkgName)) {
                    if (p.disableList == null || !p.disableList.inList(SpManagerImpl.DEVICE_NAME)) {
                        if (p.enableList == null || p.enableList.inList(SpManagerImpl.DEVICE_NAME)) {
                            if (p.fwkVersions == null || p.fwkVersions.inList(SpManagerImpl.SUPER_PROCESS_FRAMEWORK_VERSION)) {
                                return p.pkgVersions == null || p.pkgVersions.inList(versionCode);
                            }
                            return false;
                        }
                        return false;
                    }
                    return false;
                }
            }
            return false;
        }
    }

    public HashSet<String> copyPackageList() {
        HashSet<String> ret = new HashSet<>();
        synchronized (this.mLock) {
            Iterator<PackageConfig> it = this.mParsedConfigs.iterator();
            while (it.hasNext()) {
                PackageConfig p = it.next();
                ret.add(p.pkgName);
            }
        }
        return ret;
    }

    public void dump(PrintWriter pw) {
        pw.append("Current switch state: ").append((CharSequence) String.valueOf(getIsEnabled())).append("\n");
        pw.append("Current package configs:\n");
        synchronized (this.mLock) {
            Iterator<PackageConfig> it = this.mParsedConfigs.iterator();
            while (it.hasNext()) {
                PackageConfig row = it.next();
                pw.append((CharSequence) row.toString()).append("\n");
                pw.append((CharSequence) ("\tEnabled on this device:" + isPackageEnabled(row.pkgName, Helpers.getVersion(row.pkgName, this.mSystemContext)))).append("\n");
            }
        }
    }
}