package com.vivo.services.vgc;

import android.content.ContentValues;
import android.content.Context;
import android.icu.util.ULocale;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.FtFeature;
import com.android.server.ServiceThread;
import com.vivo.face.common.data.Constants;
import com.vivo.services.vgc.cbs.CbsUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import vivo.app.vgc.IVivoVgcService;
import vivo.app.vgc.VgcParcelable;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoVgcService extends IVivoVgcService.Stub {
    private static final boolean DEBUG = VgcUtils.DEBUG;
    private static final String TAG = "VGC";
    private static VivoVgcService sInstance;
    private ArrayList<String> mAllConfigDirArray;
    private ArrayList<String> mCarrierConfigDirArray;
    private Context mContext;
    private ServiceThread mCustThread;
    private ArrayList<String> mFileConfigDirArray;
    private MainHandler mHandler;
    private String[] mMapkeyList;
    private int[] mMapkeyStateList;
    private Map<String, ?> mSlot0Config;
    private Map<String, ?> mSlot1Config;
    private Map<String, ?> mVgcConfig;

    public static VivoVgcService getInstance(Context context) {
        if (sInstance == null) {
            synchronized (VivoVgcService.class) {
                if (sInstance == null) {
                    sInstance = new VivoVgcService(context);
                }
            }
        }
        return sInstance;
    }

    /* loaded from: classes.dex */
    final class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 1000) {
                VivoVgcService.this.loadConfig();
                Runnable finishRunnale = (Runnable) msg.obj;
                if (finishRunnale != null) {
                    VivoVgcService.this.mHandler.post(finishRunnale);
                }
            } else if (i == 2000) {
                Bundle data = msg.getData();
                int slotId = data.getInt("slotId");
                int state = data.getInt("state");
                String mapkey = data.getString("mapkey");
                Runnable updatedRunnale = (Runnable) msg.obj;
                VivoVgcService vivoVgcService = VivoVgcService.this;
                Map config = slotId == 0 ? vivoVgcService.mSlot0Config : vivoVgcService.mSlot1Config;
                VivoVgcService.this.mMapkeyStateList[slotId] = state;
                if (state == 0) {
                    config.clear();
                } else if (state == 1) {
                    VivoVgcService.this.mMapkeyList[slotId] = mapkey;
                    VivoVgcService.this.loadCarrierConfig(config, mapkey);
                }
                if (updatedRunnale != null) {
                    VivoVgcService.this.mHandler.post(updatedRunnale);
                }
            }
        }
    }

    public VivoVgcService(Context context) {
        this.mContext = context;
        ServiceThread serviceThread = new ServiceThread("VGC", -2, true);
        this.mCustThread = serviceThread;
        serviceThread.start();
        this.mVgcConfig = new ArrayMap();
        this.mSlot0Config = new ArrayMap();
        this.mSlot1Config = new ArrayMap();
        this.mAllConfigDirArray = new ArrayList<>();
        this.mFileConfigDirArray = new ArrayList<>();
        this.mCarrierConfigDirArray = new ArrayList<>();
        this.mMapkeyList = new String[TelephonyManager.getDefault().getSimCount()];
        this.mMapkeyStateList = new int[TelephonyManager.getDefault().getSimCount()];
        initConfigDirArray();
        this.mHandler = new MainHandler(this.mCustThread.getLooper());
        loadConfig();
    }

    private void initConfigDirArray() {
        File load_xml = new File(VgcUtils.VGC_LOAD_CONFIG_PATH_FILE);
        if (load_xml.exists()) {
            ArrayMap<String, ArrayList<String>> listMap = VgcXmlParserHelper.getStringListMapFromFile(load_xml.getPath());
            ArrayList<String> rootdirs = listMap.get("vgc_array");
            if (rootdirs == null) {
                VSlog.e("VGC", "vgc_array not found!");
                return;
            }
            Iterator<String> it = rootdirs.iterator();
            while (it.hasNext()) {
                String category = it.next();
                ArrayList<String> subConfigDirs = listMap.get(category);
                if (subConfigDirs == null) {
                    VSlog.w("VGC", "subConfigDirs=null, category=" + category);
                } else {
                    this.mAllConfigDirArray.addAll(subConfigDirs);
                }
            }
            if (listMap.containsKey("vgc_path_config")) {
                this.mFileConfigDirArray.addAll(listMap.get("vgc_path_config"));
            } else {
                this.mFileConfigDirArray.addAll(VgcUtils.defFileConfigDirArray);
            }
        } else {
            this.mAllConfigDirArray.addAll(VgcUtils.defConfigDirArray);
            this.mFileConfigDirArray.addAll(VgcUtils.defFileConfigDirArray);
        }
        Iterator<String> it2 = this.mAllConfigDirArray.iterator();
        while (it2.hasNext()) {
            String dir = it2.next();
            if (dir.contains("carrier")) {
                this.mCarrierConfigDirArray.add(dir);
            }
        }
        if (VgcUtils.DEBUG) {
            VSlog.e("VGC", "finish init: mCarrierConfigDirArray=" + this.mCarrierConfigDirArray);
            VSlog.e("VGC", "finish init: mAllConfigDirArray=" + this.mAllConfigDirArray);
            VSlog.e("VGC", "finish init: mFileConfigDirArray=" + this.mFileConfigDirArray);
        }
    }

    private String getFixedDir(String directory) {
        String CARRIER = SystemProperties.get("persist.product.carrier.name", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        StringBuffer sb = new StringBuffer(directory);
        String vgcTierBlacklistSupport = FtFeature.getFeatureAttribute("vivo.software.vgc", "support_tier_blacklist", "0");
        vgcTierBlacklistSupport.equals("1");
        if (directory.endsWith("region/")) {
            sb.append(VgcUtils.REGION);
            sb.append("/");
        } else if (VgcUtils.enableGoogle && directory.endsWith("google/")) {
            sb.append("tier");
            File tier_config = new File(sb.toString() + "_" + VgcUtils.REGION.toLowerCase());
            if (tier_config.exists()) {
                sb.append("_" + VgcUtils.REGION.toLowerCase());
            } else {
                sb.append(VgcUtils.TIERLEVEL);
            }
            sb.append("/");
        } else if (directory.equals(VgcUtils.CARRIER_DIR) || directory.equals(VgcUtils.COTA_CARRIER_DIR)) {
            sb.append(CARRIER);
            sb.append("/");
        } else if (directory.endsWith("project/")) {
            sb.append(VgcUtils.PROJECT);
            sb.append("/");
        }
        return sb.toString();
    }

    private Bundle parseAffinityFile(File affinityFile) {
        Bundle reply = new Bundle();
        try {
            FileInputStream inputStream = new FileInputStream(affinityFile);
            BufferedReader buffReader = new BufferedReader(new InputStreamReader(inputStream));
            while (true) {
                try {
                    String line = buffReader.readLine();
                    if (line == null) {
                        try {
                            break;
                        } catch (IOException e) {
                        }
                    } else if (line.startsWith("root=")) {
                        reply.putString("root", line.substring("root=".length()));
                    } else if (line.startsWith("child=")) {
                        reply.putString("child", line.substring("child=".length()));
                    }
                } catch (IOException e2) {
                    if (VgcUtils.DEBUG) {
                        VSlog.e("VGC", "parse fail of " + affinityFile.getPath() + ": e" + e2);
                    }
                    try {
                        buffReader.close();
                    } catch (IOException e3) {
                    }
                    try {
                        inputStream.close();
                    } catch (IOException e4) {
                    }
                }
            }
            buffReader.close();
            try {
                inputStream.close();
            } catch (IOException e5) {
            }
        } catch (IOException e6) {
            if (VgcUtils.DEBUG) {
                VSlog.e("VGC", "parse fail of " + affinityFile.getPath() + ": e" + e6);
            }
        }
        return reply;
    }

    private ArrayList<String> getAffinityDirs(String directory) {
        ArrayList<String> list = new ArrayList<>();
        File affinityFile = new File(directory, VgcUtils.AFFINITY_FILE);
        if (!directory.endsWith("/")) {
            directory = directory + "/";
        }
        if (affinityFile.exists()) {
            Bundle data = parseAffinityFile(affinityFile);
            String root = data.getString("root");
            String child = data.getString("child");
            if (!TextUtils.isEmpty(root)) {
                list.addAll(getAffinityDirs(root));
            }
            if (!list.contains(directory)) {
                list.add(directory);
                if (!TextUtils.isEmpty(child)) {
                    ArrayList<String> children = getAffinityDirs(child);
                    if (children.contains(directory)) {
                        if (VgcUtils.DEBUG) {
                            VSlog.e("VGC", "affinity.txt run into died loop children: " + children + ", currDir=" + directory);
                        }
                        return list;
                    }
                    Iterator<String> it = children.iterator();
                    while (it.hasNext()) {
                        String dir = it.next();
                        if (!list.contains(dir)) {
                            list.add(dir);
                        }
                    }
                }
            } else {
                if (VgcUtils.DEBUG) {
                    VSlog.e("VGC", "affinity.txt run into died loop roots: " + list + ", currDir=" + directory);
                }
                return list;
            }
        } else {
            list.add(directory);
        }
        return list;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void loadConfig() {
        boolean loadCotaConfig = SystemProperties.getBoolean("sys.cota_vgc.mount", false);
        Iterator<String> it = this.mAllConfigDirArray.iterator();
        while (it.hasNext()) {
            String dir = it.next();
            if (!dir.startsWith(VgcUtils.COTA_DIR) || loadCotaConfig) {
                String fixedDir = getFixedDir(dir);
                ArrayList<String> affinityDirs = getAffinityDirs(fixedDir);
                Iterator<String> it2 = affinityDirs.iterator();
                while (it2.hasNext()) {
                    String finalDir = it2.next();
                    ArrayMap map = VgcXmlParserHelper.getBaseConfigFromFile(finalDir + VgcUtils.VGC_CONFIG_FILE);
                    this.mVgcConfig.putAll(map);
                    ArrayMap map2 = VgcXmlParserHelper.getStringListMapFromFile(finalDir + VgcUtils.VGC_CONFIG_LIST_FILE);
                    this.mVgcConfig.putAll(map2);
                    ArrayMap map3 = VgcXmlParserHelper.getContentValuesMapFromFile(finalDir + VgcUtils.VGC_CONFIG_CUST_LIST_FILE);
                    this.mVgcConfig.putAll(map3);
                }
            }
        }
        Iterator<String> it3 = this.mFileConfigDirArray.iterator();
        while (it3.hasNext()) {
            ArrayMap map4 = VgcXmlParserHelper.getConfigPathFromFile(it3.next() + VgcUtils.VGC_PATH_CONFIG_PATH_FILE);
            this.mVgcConfig.putAll(map4);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void loadCarrierConfig(Map outMap, String mapkey) {
        Iterator<String> it = this.mCarrierConfigDirArray.iterator();
        while (it.hasNext()) {
            String dir = it.next();
            StringBuffer fullMapkeyDir = new StringBuffer(dir);
            fullMapkeyDir.append(mapkey);
            fullMapkeyDir.append("/");
            ArrayList<String> affinityDirs = getAffinityDirs(fullMapkeyDir.toString());
            Iterator<String> it2 = affinityDirs.iterator();
            while (it2.hasNext()) {
                String affinityDir = it2.next();
                ArrayMap map = VgcXmlParserHelper.getBaseConfigFromFile(affinityDir + VgcUtils.VGC_CONFIG_FILE);
                outMap.putAll(map);
                ArrayMap map2 = VgcXmlParserHelper.getStringListMapFromFile(affinityDir + VgcUtils.VGC_CONFIG_LIST_FILE);
                outMap.putAll(map2);
                ArrayMap map3 = VgcXmlParserHelper.getContentValuesMapFromFile(affinityDir + VgcUtils.VGC_CONFIG_CUST_LIST_FILE);
                outMap.putAll(map3);
            }
        }
    }

    public void updateVgcRes(Runnable runnable) {
        MainHandler mainHandler = this.mHandler;
        if (mainHandler != null) {
            Message loadMsg = mainHandler.obtainMessage(1000);
            if (runnable != null) {
                loadMsg.obj = runnable;
            }
            this.mHandler.sendMessage(loadMsg);
        }
    }

    public void updateVgcRes(int slotId, int state, String mapkey, Runnable runnable) {
        MainHandler mainHandler = this.mHandler;
        if (mainHandler != null) {
            Message updateMsg = mainHandler.obtainMessage(2000);
            Bundle data = new Bundle();
            data.putInt("slotId", slotId);
            data.putInt("state", state);
            data.putString("mapkey", mapkey);
            if (runnable != null) {
                updateMsg.obj = runnable;
            }
            updateMsg.setData(data);
            this.mHandler.sendMessage(updateMsg);
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:14:0x003d, code lost:
        if (r8.getClass().isInstance(r0) == false) goto L17;
     */
    /* JADX WARN: Multi-variable type inference failed */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private synchronized <T> T getValueFromMap(java.util.Map r6, java.lang.String r7, T r8, boolean r9) {
        /*
            r5 = this;
            monitor-enter(r5)
            boolean r0 = com.vivo.services.vgc.VgcUtils.DEBUG     // Catch: java.lang.Throwable -> Laa
            if (r0 == 0) goto L23
            java.lang.String r0 = "VGC"
            java.lang.StringBuilder r1 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> Laa
            r1.<init>()     // Catch: java.lang.Throwable -> Laa
            java.lang.String r2 = "getValueFromMap enter "
            r1.append(r2)     // Catch: java.lang.Throwable -> Laa
            r1.append(r7)     // Catch: java.lang.Throwable -> Laa
            java.lang.String r2 = "="
            r1.append(r2)     // Catch: java.lang.Throwable -> Laa
            r1.append(r8)     // Catch: java.lang.Throwable -> Laa
            java.lang.String r1 = r1.toString()     // Catch: java.lang.Throwable -> Laa
            vivo.util.VSlog.i(r0, r1)     // Catch: java.lang.Throwable -> Laa
        L23:
            r0 = r8
            if (r7 == 0) goto L7e
            boolean r1 = r6.containsKey(r7)     // Catch: java.lang.Throwable -> Laa
            if (r1 == 0) goto L7e
            java.lang.Object r1 = r6.get(r7)     // Catch: java.lang.Exception -> L41 java.lang.Throwable -> Laa
            r0 = r1
            if (r9 == 0) goto L40
            if (r8 == 0) goto L3f
            java.lang.Class r1 = r8.getClass()     // Catch: java.lang.Exception -> L41 java.lang.Throwable -> Laa
            boolean r1 = r1.isInstance(r0)     // Catch: java.lang.Exception -> L41 java.lang.Throwable -> Laa
            if (r1 != 0) goto L40
        L3f:
            r0 = r8
        L40:
            goto L7e
        L41:
            r1 = move-exception
            boolean r2 = com.vivo.services.vgc.VgcUtils.DEBUG     // Catch: java.lang.Throwable -> Laa
            if (r2 == 0) goto L7d
            java.lang.String r2 = "VGC"
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> Laa
            r3.<init>()     // Catch: java.lang.Throwable -> Laa
            r3.append(r7)     // Catch: java.lang.Throwable -> Laa
            java.lang.String r4 = "={"
            r3.append(r4)     // Catch: java.lang.Throwable -> Laa
            java.lang.Object r4 = r6.get(r7)     // Catch: java.lang.Throwable -> Laa
            r3.append(r4)     // Catch: java.lang.Throwable -> Laa
            java.lang.String r4 = "} cast to "
            r3.append(r4)     // Catch: java.lang.Throwable -> Laa
            if (r8 == 0) goto L6c
            java.lang.Class r4 = r8.getClass()     // Catch: java.lang.Throwable -> Laa
            java.lang.String r4 = r4.getSimpleName()     // Catch: java.lang.Throwable -> Laa
            goto L6e
        L6c:
            java.lang.String r4 = "null"
        L6e:
            r3.append(r4)     // Catch: java.lang.Throwable -> Laa
            java.lang.String r4 = " fail, using defaultValue"
            r3.append(r4)     // Catch: java.lang.Throwable -> Laa
            java.lang.String r3 = r3.toString()     // Catch: java.lang.Throwable -> Laa
            vivo.util.VSlog.e(r2, r3)     // Catch: java.lang.Throwable -> Laa
        L7d:
            r0 = r8
        L7e:
            boolean r1 = com.vivo.services.vgc.VgcUtils.DEBUG     // Catch: java.lang.Throwable -> Laa
            if (r1 == 0) goto La8
            java.lang.String r1 = "VGC"
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> Laa
            r2.<init>()     // Catch: java.lang.Throwable -> Laa
            java.lang.String r3 = "getValueFromMap return "
            r2.append(r3)     // Catch: java.lang.Throwable -> Laa
            r2.append(r7)     // Catch: java.lang.Throwable -> Laa
            java.lang.String r3 = "="
            r2.append(r3)     // Catch: java.lang.Throwable -> Laa
            r2.append(r0)     // Catch: java.lang.Throwable -> Laa
            java.lang.String r3 = " defaultValue="
            r2.append(r3)     // Catch: java.lang.Throwable -> Laa
            r2.append(r8)     // Catch: java.lang.Throwable -> Laa
            java.lang.String r2 = r2.toString()     // Catch: java.lang.Throwable -> Laa
            vivo.util.VSlog.i(r1, r2)     // Catch: java.lang.Throwable -> Laa
        La8:
            monitor-exit(r5)
            return r0
        Laa:
            r6 = move-exception
            monitor-exit(r5)
            throw r6
        */
        throw new UnsupportedOperationException("Method not decompiled: com.vivo.services.vgc.VivoVgcService.getValueFromMap(java.util.Map, java.lang.String, java.lang.Object, boolean):java.lang.Object");
    }

    public VgcParcelable getValueAidl(String name, VgcParcelable defValue) {
        Object defaultValue = defValue.getValue();
        try {
            return new VgcParcelable(getValueFromMap(this.mVgcConfig, name, defaultValue, true));
        } catch (Exception e) {
            VSlog.e("VGC", "VgcParcelable getValueFromMap fail, name=" + name + ", defValue=" + defValue + ". e=" + e);
            return defValue;
        }
    }

    public VgcParcelable getValueBySubIdAidl(String name, int subId, VgcParcelable defValue) {
        if (subId == -1) {
            return getValueAidl(name, defValue);
        }
        return getValueBySlotIdAidl(name, SubscriptionManager.getSlotIndex(subId), defValue);
    }

    public VgcParcelable getValueBySlotIdAidl(String name, int slotId, VgcParcelable defValue) {
        Map slotConfig = slotId == 0 ? this.mSlot0Config : this.mSlot1Config;
        Object defaultValue = defValue.getValue();
        try {
            return new VgcParcelable(getValueFromMap(slotConfig, name, getValueFromMap(this.mVgcConfig, name, defaultValue, true), true));
        } catch (Exception e) {
            VSlog.e("VGC", "VgcParcelable getValueFromMap fail, name=" + name + ", defValue=" + defValue + ". e=" + e);
            return defValue;
        }
    }

    public String getVgcVersion() {
        return VgcUtils.VGC_VERSION;
    }

    public boolean isVgcActivated() {
        return VgcUtils.VGC_SUPPORT;
    }

    public String getString(String name, String defaultStr) {
        String value = (String) getValueFromMap(this.mVgcConfig, name, defaultStr, false);
        return specialHandle(name, value instanceof String ? value : defaultStr);
    }

    public boolean getBool(String name, boolean defaultBool) {
        Object value = getValueFromMap(this.mVgcConfig, name, Boolean.valueOf(defaultBool), false);
        return value instanceof Boolean ? ((Boolean) value).booleanValue() : defaultBool;
    }

    public int getInt(String name, int defaultValue) {
        Object value = getValueFromMap(this.mVgcConfig, name, Integer.valueOf(defaultValue), false);
        return value instanceof Integer ? ((Integer) value).intValue() : defaultValue;
    }

    public List<String> getStringList(String name, List<String> defaultList) {
        Object value = getValueFromMap(this.mVgcConfig, name, defaultList, false);
        return value instanceof List ? (List) value : defaultList;
    }

    public ContentValues getContentValues(String name, ContentValues mContentValues) {
        Object value = getValueFromMap(this.mVgcConfig, name, mContentValues, false);
        return value instanceof ContentValues ? (ContentValues) value : mContentValues;
    }

    public String getFile(String name, String defaultStr) {
        String relativePath = (String) getValueFromMap(this.mVgcConfig, name, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, true);
        if (!TextUtils.isEmpty(relativePath)) {
            for (int i = this.mAllConfigDirArray.size() - 1; i >= 0; i--) {
                String fixedDir = getFixedDir(this.mAllConfigDirArray.get(i));
                ArrayList<String> affinityDirs = getAffinityDirs(fixedDir);
                for (int j = affinityDirs.size() - 1; j >= 0; j--) {
                    File file = new File(affinityDirs.get(j), relativePath);
                    if (DEBUG) {
                        VSlog.i("VGC", "getFile checking " + file.getPath());
                    }
                    if (file.exists()) {
                        return file.getPath();
                    }
                }
            }
        }
        return defaultStr;
    }

    public List<String> getFileList(String name, List<String> defaultList) {
        ArrayList<String> list = new ArrayList<>();
        String relativePath = (String) getValueFromMap(this.mVgcConfig, name, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, true);
        if (!TextUtils.isEmpty(relativePath)) {
            for (int i = this.mAllConfigDirArray.size() - 1; i >= 0; i--) {
                String fixedDir = getFixedDir(this.mAllConfigDirArray.get(i));
                ArrayList<String> affinityDirs = getAffinityDirs(fixedDir);
                for (int j = affinityDirs.size() - 1; j >= 0; j--) {
                    File file = new File(affinityDirs.get(j), relativePath);
                    if (DEBUG) {
                        VSlog.i("VGC", "getFileList checking " + file.getPath());
                    }
                    if (file.exists()) {
                        list.add(file.getPath());
                    }
                }
            }
        }
        if (list.isEmpty()) {
            return defaultList;
        }
        return list;
    }

    public String getFileBySlotId(String name, int slotId, String defFilePath) {
        if (!SubscriptionManager.isValidSlotIndex(slotId) || this.mMapkeyStateList[slotId] == 0) {
            return getFile(name, defFilePath);
        }
        String relativePath = (String) getValueFromMap(this.mVgcConfig, name, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, true);
        if (!TextUtils.isEmpty(relativePath)) {
            Iterator<String> it = this.mCarrierConfigDirArray.iterator();
            while (it.hasNext()) {
                String dir = it.next();
                String fixedDir = dir + "/" + this.mMapkeyList[slotId];
                ArrayList<String> affinityDirs = getAffinityDirs(fixedDir);
                for (int j = affinityDirs.size() - 1; j >= 0; j--) {
                    File file = new File(affinityDirs.get(j), relativePath);
                    if (DEBUG) {
                        VSlog.i("VGC", "getFileBySlotId mapkey checking " + file.getPath());
                    }
                    if (file.exists()) {
                        return file.getPath();
                    }
                }
            }
            Iterator<String> it2 = this.mAllConfigDirArray.iterator();
            while (it2.hasNext()) {
                String dir2 = it2.next();
                String fixedDir2 = getFixedDir(dir2);
                ArrayList<String> affinityDirs2 = getAffinityDirs(fixedDir2);
                for (int j2 = affinityDirs2.size() - 1; j2 >= 0; j2--) {
                    File file2 = new File(affinityDirs2.get(j2), relativePath);
                    if (DEBUG) {
                        VSlog.i("VGC", "getFileBySlotId checking " + file2.getPath());
                    }
                    if (file2.exists()) {
                        return file2.getPath();
                    }
                }
            }
        }
        return defFilePath;
    }

    public String getFileBySubId(String name, int subId, String defFilePath) {
        return getFileBySlotId(name, SubscriptionManager.getSlotIndex(subId), defFilePath);
    }

    public List<String> getFileListBySlotId(String name, int slotId, List<String> defaultList) {
        if (!SubscriptionManager.isValidSlotIndex(slotId) || this.mMapkeyStateList[slotId] == 0) {
            return getFileList(name, defaultList);
        }
        ArrayList<String> list = new ArrayList<>();
        String relativePath = (String) getValueFromMap(this.mVgcConfig, name, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, true);
        if (!TextUtils.isEmpty(relativePath)) {
            for (int i = this.mCarrierConfigDirArray.size() - 1; i >= 0; i--) {
                String fixedDir = this.mCarrierConfigDirArray.get(i) + "/" + this.mMapkeyList[slotId];
                ArrayList<String> affinityDirs = getAffinityDirs(fixedDir);
                for (int j = affinityDirs.size() - 1; j >= 0; j--) {
                    File file = new File(affinityDirs.get(j), relativePath);
                    if (DEBUG) {
                        VSlog.i("VGC", "getFileListBySlotId mapkey checking " + file.getPath());
                    }
                    if (file.exists()) {
                        list.add(file.getPath());
                    }
                }
            }
            for (int i2 = this.mAllConfigDirArray.size() - 1; i2 >= 0; i2--) {
                String fixedDir2 = getFixedDir(this.mAllConfigDirArray.get(i2));
                ArrayList<String> affinityDirs2 = getAffinityDirs(fixedDir2);
                for (int j2 = affinityDirs2.size() - 1; j2 >= 0; j2--) {
                    File file2 = new File(affinityDirs2.get(j2), relativePath);
                    if (DEBUG) {
                        VSlog.i("VGC", "getFileListBySlotId checking " + file2.getPath());
                    }
                    if (!file2.getPath().contains(this.mMapkeyList[slotId]) && file2.exists() && !list.contains(file2.getPath())) {
                        list.add(file2.getPath());
                    }
                }
            }
        }
        if (list.isEmpty()) {
            return defaultList;
        }
        return list;
    }

    public List<String> getFileListBySubId(String name, int subId, List<String> defaultList) {
        return getFileListBySlotId(name, SubscriptionManager.getSlotIndex(subId), defaultList);
    }

    private String specialHandle(String name, String defaultStr) {
        char c;
        String value;
        int hashCode = name.hashCode();
        if (hashCode != -2105574834) {
            if (hashCode == -1562644275 && name.equals("setupwizard_default_language")) {
                c = 0;
            }
            c = 65535;
        } else {
            if (name.equals("settings_default_region")) {
                c = 1;
            }
            c = 65535;
        }
        if (c == 0) {
            value = setupwizardDefaultLanguage(defaultStr);
        } else if (c == 1) {
            value = settingsDefaultRegion(defaultStr);
        } else {
            return defaultStr;
        }
        if (DEBUG) {
            VSlog.i("VGC", "specialHandle name=" + name + ", return=" + value + ", defaultStr=" + defaultStr);
        }
        return value;
    }

    private String setupwizardDefaultLanguage(String defLanguage) {
        String str;
        boolean userSetupCompleted = Settings.Secure.getInt(this.mContext.getContentResolver(), "user_setup_complete", 0) == 1;
        if (userSetupCompleted) {
            return defLanguage;
        }
        String isoLanguage = defLanguage;
        String iccid = CbsUtils.getDiscern();
        String isoCountry = SystemProperties.get("gsm.sim.operator.iso-country", SystemProperties.get("gsm.operator.iso-country", (String) null));
        if (isoCountry != null && isoCountry.length() >= 2 && getBool("VGC_enable_iso_country_language", false)) {
            int substringBeginIndex = isoCountry.startsWith(",") ? 1 : 0;
            isoCountry = isoCountry.substring(substringBeginIndex, substringBeginIndex + 2);
            ULocale likelyLocale = ULocale.addLikelySubtags(new ULocale("und", isoCountry));
            isoLanguage = likelyLocale.getLanguage() + "_" + isoCountry.toUpperCase();
        }
        String iccidLanguage = isoLanguage;
        ContentValues languageMap = getContentValues("VGC_iccid_language_map", null);
        if (languageMap != null && iccid != null && !iccid.trim().isEmpty() && iccid.length() >= 5) {
            String lang1 = languageMap.getAsString(iccid.substring(2, 3));
            String lang2 = languageMap.getAsString(iccid.substring(2, 4));
            String lang3 = languageMap.getAsString(iccid.substring(2, 5));
            if (lang2 != null) {
                str = lang2;
            } else {
                str = lang3 != null ? lang3 : lang1;
            }
            iccidLanguage = str;
        }
        if (DEBUG) {
            VSlog.i("VGC", "isoCountry=" + isoCountry + ", isoLanguage=" + isoLanguage + ", defaultStr=" + defLanguage);
        }
        return iccidLanguage != null ? iccidLanguage : isoLanguage;
    }

    private String settingsDefaultRegion(String defRegion) {
        String str;
        String strLocal;
        String iccidCountry;
        boolean userSetupCompleted = Settings.Secure.getInt(this.mContext.getContentResolver(), "user_setup_complete", 0) == 1;
        if (userSetupCompleted) {
            return defRegion;
        }
        String region = defRegion;
        if (getBool("VGC_enable_iso_country_region", false) && (region = SystemProperties.get("gsm.sim.operator.iso-country", SystemProperties.get("gsm.operator.iso-country", defRegion))) != null && !region.trim().isEmpty() && region.length() >= 2) {
            int substringBeginIndex = region.startsWith(",") ? 1 : 0;
            region = region.toUpperCase().substring(substringBeginIndex, substringBeginIndex + 2);
        }
        String iccid = CbsUtils.getDiscern();
        ContentValues languageMap = getContentValues("VGC_iccid_language_map", null);
        List<String> regionMap = getStringList("vgc_region_limit_list", null);
        ContentValues regionAlisMap = getContentValues("VGC_iso_country_alias_setupwizard_map", null);
        if (languageMap != null && iccid != null && !iccid.trim().isEmpty() && iccid.length() >= 5) {
            String region1 = languageMap.getAsString(iccid.substring(2, 3));
            String region2 = languageMap.getAsString(iccid.substring(2, 4));
            String region3 = languageMap.getAsString(iccid.substring(2, 5));
            if (region2 != null) {
                str = region2;
            } else {
                str = region3 != null ? region3 : region1;
            }
            String strLocal2 = str;
            if (!DEBUG) {
                strLocal = strLocal2;
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("strLocal=");
                strLocal = strLocal2;
                sb.append(strLocal);
                VSlog.i("VGC", sb.toString());
            }
            if (strLocal != null) {
                String[] temp = strLocal.split("_");
                if (temp.length >= 2) {
                    iccidCountry = temp[1];
                } else {
                    ULocale likelyLocale = ULocale.addLikelySubtags(new ULocale(strLocal, "und"));
                    iccidCountry = likelyLocale.getCountry().toUpperCase();
                }
                region = iccidCountry;
            }
        }
        String vivoCountryCode = region;
        if (regionAlisMap != null && regionAlisMap.containsKey(region)) {
            vivoCountryCode = regionAlisMap.getAsString(region);
        }
        if (DEBUG) {
            String isoCountry = SystemProperties.get("gsm.sim.operator.iso-country", SystemProperties.get("gsm.operator.iso-country", defRegion));
            VSlog.i("VGC", "isoCountry=" + isoCountry + ", region=" + region + ", defRegion=" + defRegion);
        }
        return (regionMap == null || !regionMap.contains(vivoCountryCode)) ? defRegion : vivoCountryCode;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:42:0x00d4, code lost:
        if (r6.equals("a") != false) goto L33;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    protected void dump(java.io.FileDescriptor r25, java.io.PrintWriter r26, java.lang.String[] r27) {
        /*
            Method dump skipped, instructions count: 1440
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.vivo.services.vgc.VivoVgcService.dump(java.io.FileDescriptor, java.io.PrintWriter, java.lang.String[]):void");
    }
}