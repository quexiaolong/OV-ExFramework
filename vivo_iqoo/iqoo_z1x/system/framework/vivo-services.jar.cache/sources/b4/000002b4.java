package com.android.server.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import com.vivo.common.utils.VLog;
import com.vivo.framework.configuration.ConfigurationManager;
import java.util.ArrayList;
import java.util.Iterator;
import vivo.app.configuration.ConfigurationObserver;
import vivo.app.configuration.ContentValuesList;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class VivoLocConf {
    public static final String API_CONTROL = "ApiControl";
    public static final String APP_FILTER = "APPFilter";
    public static final String CN0_STRENGTH = "cn0Strength";
    public static final String CN0_WEAK = "cn0Weak";
    public static final String FAKE_GPS = "fakeGps";
    public static final String FAKE_LOC_STATE = "FakeLocState";
    public static final String FUSED_GPS = "fusedGps";
    private static final String GPS_CONFIGURATION_FILE = "/data/bbkcore/GpsConfiguration_vivo_gps_configuration_4.0.xml";
    private static final String INTENT_DUMP_INFO = "com.android.server.VivoLocConf.debug.dump";
    private static final String INTENT_GET_CONFIG = "com.android.server.VivoLocConf.debug.update";
    private static final String LOCATION_APP_FILTER = "/data/bbkcore/VivoLocationAppFilter_VivoLocationAppFilter_v2.0.xml";
    public static final String LOCATION_DIAGNOSTIC = "VivoLocationDiagnostic";
    private static final String LOCATION_DIAGNOSTIC_FILE = "/data/bbkcore/VivoLocationFeatureConfig_VivoLocationDiagnostic_v2.0.xml";
    private static final String LOCATION_FEATURE_CONFIG_FILE = "/data/bbkcore/VivoLocationFeatureConfig_VivoLocationFeatureConfig_v2.0.xml";
    public static final String LOCATION_NOTIFY = "VivoLocationNotify";
    private static final String LOCATION_NOTIFY_FILE = "/data/bbkcore/VivoLocationNotify_vivoLocationNotify_v2.0.xml";
    public static final String MOCK_LOCATION_RECOVERY_NOTIFY = "VivoMockLocationRecoveryNotify";
    private static final String MOCK_LOCATION_RECOVERY_NOTIFY_FILE = "/data/bbkcore/VivoMockLocationRecoveryNotify_VivoMockLocationRecoveryNotify_v2.0.xml";
    public static final String NETWORK_LOCATION_PROVIDER = "NetworkLocationProvider";
    private static final String NETWORK_LOCATION_PROVIDER_FILE = "/data/bbkcore/NetworkLocationProvider_NetworkLocationProvider_2.0.xml";
    public static final String OTHERS = "Others";
    private static final String TAG = "VivoLocConf";
    public static final String VPDR_CONFIG = "VPDRConfig";
    private static ConfigurationManager mConfigurationManager;
    public static boolean D = false;
    private static VivoLocConf sVivoLocConf = null;
    private ArrayList<MyConfig> mConfigList = new ArrayList<>();
    private Context mContext = null;
    private Object mLock = new Object();
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.location.VivoLocConf.2
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            VLog.d(VivoLocConf.TAG, "receive broadcast intent, action: " + action);
            if (action == null) {
                return;
            }
            if (action.equals(VivoLocConf.INTENT_DUMP_INFO)) {
                VivoLocConf.this.dumpAll();
            } else if (action.equals(VivoLocConf.INTENT_GET_CONFIG)) {
                VivoLocConf.this.freshAllConfig();
            }
        }
    };

    /* loaded from: classes.dex */
    public interface ContentValuesListChangedListener {
        void onConfigChanged(ContentValuesList contentValuesList);
    }

    public static VivoLocConf getInstance() {
        if (sVivoLocConf == null) {
            sVivoLocConf = new VivoLocConf();
        }
        return sVivoLocConf;
    }

    private VivoLocConf() {
    }

    public void init(Context context, Handler handler) {
        this.mContext = context;
        if (mConfigurationManager == null) {
            mConfigurationManager = ConfigurationManager.getInstance();
        }
        this.mConfigList.clear();
        this.mConfigList.add(new MyConfig(GPS_CONFIGURATION_FILE, FAKE_GPS));
        this.mConfigList.add(new MyConfig(GPS_CONFIGURATION_FILE, FUSED_GPS));
        this.mConfigList.add(new MyConfig(GPS_CONFIGURATION_FILE, CN0_WEAK));
        this.mConfigList.add(new MyConfig(GPS_CONFIGURATION_FILE, CN0_STRENGTH));
        this.mConfigList.add(new MyConfig(NETWORK_LOCATION_PROVIDER_FILE, NETWORK_LOCATION_PROVIDER));
        this.mConfigList.add(new MyConfig(LOCATION_NOTIFY_FILE, LOCATION_NOTIFY));
        this.mConfigList.add(new MyConfig(MOCK_LOCATION_RECOVERY_NOTIFY_FILE, MOCK_LOCATION_RECOVERY_NOTIFY));
        this.mConfigList.add(new MyConfig(LOCATION_FEATURE_CONFIG_FILE, FAKE_LOC_STATE));
        this.mConfigList.add(new MyConfig(LOCATION_FEATURE_CONFIG_FILE, VPDR_CONFIG));
        this.mConfigList.add(new MyConfig(LOCATION_FEATURE_CONFIG_FILE, API_CONTROL));
        this.mConfigList.add(new MyConfig(LOCATION_FEATURE_CONFIG_FILE, OTHERS));
        this.mConfigList.add(new MyConfig(LOCATION_APP_FILTER, APP_FILTER));
        this.mConfigList.add(new MyConfig(LOCATION_DIAGNOSTIC_FILE, LOCATION_DIAGNOSTIC));
        Iterator<MyConfig> it = this.mConfigList.iterator();
        while (it.hasNext()) {
            MyConfig config = it.next();
            config.register();
        }
        if (handler != null) {
            handler.post(new Runnable() { // from class: com.android.server.location.VivoLocConf.1
                @Override // java.lang.Runnable
                public void run() {
                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(VivoLocConf.INTENT_DUMP_INFO);
                    intentFilter.addAction(VivoLocConf.INTENT_GET_CONFIG);
                    VivoLocConf.this.mContext.registerReceiver(VivoLocConf.this.mBroadcastReceiver, intentFilter);
                }
            });
        } else {
            VLog.e(TAG, "no thread for dbg operation!!!");
        }
    }

    public void registerListener(String name, ContentValuesListChangedListener listener) {
        synchronized (this.mLock) {
            if (name != null) {
                if (!name.isEmpty() && listener != null) {
                    Iterator<MyConfig> it = this.mConfigList.iterator();
                    while (it.hasNext()) {
                        MyConfig config = it.next();
                        if (name.equals(config.getName())) {
                            config.addListener(listener);
                        }
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpAll() {
        VLog.d(TAG, "******************************* Dump VivoLocConf begin *************************\n");
        Iterator<MyConfig> it = this.mConfigList.iterator();
        while (it.hasNext()) {
            MyConfig config = it.next();
            config.dump();
        }
        VLog.d(TAG, "******************************* Dump VivoLocConf end ***************************\n");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void freshAllConfig() {
        Iterator<MyConfig> it = this.mConfigList.iterator();
        while (it.hasNext()) {
            MyConfig config = it.next();
            config.updateList();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MyConfig extends ConfigurationObserver {
        private String mFile;
        private ContentValuesList mList;
        private ArrayList<ContentValuesListChangedListener> mListener = new ArrayList<>();
        private String mName;

        public MyConfig(String file, String name) {
            this.mFile = file;
            this.mName = name;
        }

        public void register() {
            this.mList = VivoLocConf.mConfigurationManager.getContentValuesList(this.mFile, this.mName);
            VivoLocConf.mConfigurationManager.registerObserver(this.mList, this);
        }

        public void onConfigChange(String file, String name) {
            VLog.d(VivoLocConf.TAG, "config update,module:" + this.mName + " file:" + file + " name:" + name);
            updateList();
        }

        public void updateList() {
            ContentValuesList contentValuesList = VivoLocConf.mConfigurationManager.getContentValuesList(this.mFile, this.mName);
            this.mList = contentValuesList;
            if (contentValuesList != null) {
                Iterator<ContentValuesListChangedListener> it = this.mListener.iterator();
                while (it.hasNext()) {
                    ContentValuesListChangedListener listener = it.next();
                    inform(listener);
                }
            }
        }

        public void addListener(ContentValuesListChangedListener listener) {
            VLog.d(VivoLocConf.TAG, "addListener " + this.mName);
            this.mListener.add(listener);
            inform(listener);
        }

        private void inform(ContentValuesListChangedListener listener) {
            ContentValuesList contentValuesList = this.mList;
            if (contentValuesList != null && contentValuesList.getValues() != null && this.mList.getValues().size() != 0) {
                synchronized (VivoLocConf.this.mLock) {
                    if (VivoLocConf.D) {
                        VLog.d(VivoLocConf.TAG, "notify Listener " + this.mName + " changed, content:" + this.mList);
                    }
                    try {
                        listener.onConfigChanged(this.mList);
                    } catch (Exception e) {
                        VLog.e(VivoLocConf.TAG, e.toString());
                    }
                }
                return;
            }
            VLog.d(VivoLocConf.TAG, "Config is empty, name:" + this.mName);
        }

        public String getName() {
            return this.mName;
        }

        public ContentValuesList getList() {
            return this.mList;
        }

        public void dump() {
            VLog.d(VivoLocConf.TAG, "-------" + this.mName + "-----------" + this.mFile + "----------");
            ContentValuesList contentValuesList = this.mList;
            VLog.d(VivoLocConf.TAG, contentValuesList == null ? "Empty" : contentValuesList.toString());
        }
    }
}