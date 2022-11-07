package com.vivo.services.timezone;

import android.content.Context;
import android.os.HandlerThread;
import android.os.SystemProperties;
import android.util.FtFeature;
import com.vivo.common.utils.VLog;
import com.vivo.framework.configuration.ConfigurationManager;
import java.util.List;
import vivo.app.configuration.ConfigurationObserver;
import vivo.app.configuration.StringList;
import vivo.app.timezone.ITZManager;

/* loaded from: classes.dex */
public class TZManagerService extends ITZManager.Stub {
    private static final String LIST_ONLINE_CONFIGURATION_DATA = "online_configuration_data";
    private static final String PUSH_FILE = "/data/bbkcore/combined_timezone_list_timezone_data_push_1.0.xml";
    private static final String TAG = "TZManagerService";
    private static ConfigurationManager configurationManager;
    private static StringList items;
    private static TZManagerService sInstance = null;
    private LocationUpdateHelper mLocationUpdateHelper;
    private long mNativeTimeZoneDB = 0;
    HandlerThread mHandlerThread = new HandlerThread("time-zone-moniter");
    private boolean mInit = false;
    private ConfigurationObserver configurationObserver = new ConfigurationObserver() { // from class: com.vivo.services.timezone.TZManagerService.2
        public void onConfigChange(String file, String name) {
            if (TZManagerService.configurationManager == null) {
                ConfigurationManager unused = TZManagerService.configurationManager = ConfigurationManager.getInstance();
            }
            StringList unused2 = TZManagerService.items = TZManagerService.configurationManager.getStringList(TZManagerService.PUSH_FILE, TZManagerService.LIST_ONLINE_CONFIGURATION_DATA);
        }
    };

    /* JADX INFO: Access modifiers changed from: private */
    public static native long initNativeTimeZoneDB();

    private static native String nativeLocationToTimeZone(long j, double d, double d2);

    private static native int nativeUpdateTimeZoneDB(long j);

    private TZManagerService(final Context context) {
        this.mHandlerThread.start();
        this.mHandlerThread.getThreadHandler().post(new Runnable() { // from class: com.vivo.services.timezone.TZManagerService.1
            @Override // java.lang.Runnable
            public void run() {
                VLog.d(TZManagerService.TAG, "start init TZManagerService");
                TZManagerService.this.mNativeTimeZoneDB = TZManagerService.initNativeTimeZoneDB();
                long interval = Integer.parseInt(SystemProperties.get("temp.timezone.check.interval", "45")) * 60 * 1000;
                Context context2 = context;
                TZManagerService tZManagerService = TZManagerService.this;
                LocationUpdateHelper.makeDefaultUpdater(context2, tZManagerService, tZManagerService.mHandlerThread.getLooper(), interval);
                TZManagerService.this.mLocationUpdateHelper = LocationUpdateHelper.getInstance();
                TZManagerService.this.mInit = true;
                VLog.d(TZManagerService.TAG, "end init TZManagerService");
            }
        });
        if (configurationManager == null) {
            configurationManager = ConfigurationManager.getInstance();
        }
        StringList stringList = configurationManager.getStringList(PUSH_FILE, LIST_ONLINE_CONFIGURATION_DATA);
        items = stringList;
        configurationManager.registerObserver(stringList, this.configurationObserver);
    }

    public static TZManagerService getInstance(Context context) {
        if (!FtFeature.isFeatureSupport("vivo.software.timezonecorrection")) {
            return null;
        }
        if (sInstance == null) {
            sInstance = new TZManagerService(context);
        }
        return sInstance;
    }

    public void plmnUpdateTimeZone(String timeZone) {
    }

    public String locationToTimeZone(double lat, double lon) {
        if (this.mInit) {
            long j = this.mNativeTimeZoneDB;
            if (j != 0) {
                return nativeLocationToTimeZone(j, lat, lon);
            }
        }
        VLog.d(TAG, "locationToTimeZone service is not ready,do nothing");
        return "TimeZoneMoniterService is not ready!";
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateTimeZoneDB() {
        if (this.mInit) {
            long j = this.mNativeTimeZoneDB;
            if (j != 0) {
                nativeUpdateTimeZoneDB(j);
                return;
            }
        }
        VLog.d(TAG, "updateTimeZoneDB  service is not ready,do nothing!");
    }

    public List<String> getOnlineConfigurationData() {
        try {
            if (items != null && items.getValues().size() != 0) {
                return items.getValues();
            }
            return null;
        } catch (Exception e) {
            VLog.e(TAG, "getOnlineConfigurationData list dead", e);
            e.printStackTrace();
            return null;
        }
    }

    public boolean shouldInterceptNITZ() {
        return this.mLocationUpdateHelper.shouldInterceptNITZ();
    }
}