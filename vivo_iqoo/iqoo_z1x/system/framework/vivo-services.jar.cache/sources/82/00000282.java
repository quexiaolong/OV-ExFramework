package com.android.server.location;

import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.SystemProperties;
import android.os.WorkSource;
import com.android.internal.location.ProviderRequest;
import com.android.server.location.VivoLocConf;
import com.android.server.location.gnss.GnssLocationProvider;
import com.vivo.common.utils.VLog;
import java.util.HashMap;
import vivo.app.configuration.ContentValuesList;

/* loaded from: classes.dex */
public class VivoCoreLocationManager {
    private static final String KEY_DATE = "date";
    private static final String KEY_FAKE_GPS = "fakeGps";
    private static final String KEY_FAKE_GPS_MAP_LIST = "mapList";
    private static final String KEY_FAKE_GPS_SUPPORT = "support";
    private static final String KEY_FUSED_GPS = "fusedGps";
    private static final String KEY_FUSED_GPS_MAP_LIST = "mapList";
    private static final String KEY_FUSED_GPS_SUPPORT = "support";
    private static final String KEY_VERSION = "version";
    private static final String TAG = "VivoCoreLocationManager";
    public static boolean isFusedLocationSupported = false;
    private static String mConfigDate = "unkown";
    private static double mVersion = 1.0d;
    private GnssLocationProvider mGnssLocationProvider;
    private HandlerThread mThread;
    private VivoFakeGpsProvider mVivoFakeGpsProvider;
    private VivoGpsStateMachine mVivoGpsStateMachine;
    private boolean DEBUG = false;
    private boolean isFakeGpsProviderStarted = false;
    private ProviderRequest mRequest = null;
    private WorkSource mSource = null;
    private FakeGpsConfig mFakeGpsConfig = null;
    private FusedGpsConfig mFusedGpsConfig = null;

    /* renamed from: lambda$5sCYC-048UthHO8Vv5iI_8VjD_8 */
    public static /* synthetic */ void m1lambda$5sCYC048UthHO8Vv5iI_8VjD_8(VivoCoreLocationManager vivoCoreLocationManager, ContentValuesList contentValuesList) {
        vivoCoreLocationManager.parseFusedGpsConfig(contentValuesList);
    }

    public static /* synthetic */ void lambda$YGqT9Jx80VCl7G9WaOFCNOprFVA(VivoCoreLocationManager vivoCoreLocationManager, ContentValuesList contentValuesList) {
        vivoCoreLocationManager.parseFakeGpsConfig(contentValuesList);
    }

    public VivoCoreLocationManager(Context context, GnssLocationProvider gnssProvider) {
        this.mThread = null;
        this.mVivoGpsStateMachine = null;
        this.mVivoFakeGpsProvider = null;
        this.mGnssLocationProvider = null;
        this.mGnssLocationProvider = gnssProvider;
        HandlerThread handlerThread = new HandlerThread(TAG);
        this.mThread = handlerThread;
        handlerThread.start();
        this.mVivoGpsStateMachine = new VivoGpsStateMachine(context, this, this.mThread.getLooper());
        this.mVivoFakeGpsProvider = new VivoFakeGpsProvider(context, this, this.mThread.getLooper());
        VivoLocConf config = VivoLocConf.getInstance();
        config.registerListener("fakeGps", new VivoLocConf.ContentValuesListChangedListener() { // from class: com.android.server.location.-$$Lambda$VivoCoreLocationManager$YGqT9Jx80VCl7G9WaOFCNOprFVA
            @Override // com.android.server.location.VivoLocConf.ContentValuesListChangedListener
            public final void onConfigChanged(ContentValuesList contentValuesList) {
                VivoCoreLocationManager.lambda$YGqT9Jx80VCl7G9WaOFCNOprFVA(VivoCoreLocationManager.this, contentValuesList);
            }
        });
        config.registerListener("fusedGps", new VivoLocConf.ContentValuesListChangedListener() { // from class: com.android.server.location.-$$Lambda$VivoCoreLocationManager$5sCYC-048UthHO8Vv5iI_8VjD_8
            @Override // com.android.server.location.VivoLocConf.ContentValuesListChangedListener
            public final void onConfigChanged(ContentValuesList contentValuesList) {
                VivoCoreLocationManager.m1lambda$5sCYC048UthHO8Vv5iI_8VjD_8(VivoCoreLocationManager.this, contentValuesList);
            }
        });
    }

    public void onStartNavigating() {
        if (isFusedLocationSupported) {
            this.mVivoGpsStateMachine.onStartNavigating();
            if (this.DEBUG) {
                VLog.d(TAG, "Fused:onStartNavigating");
                return;
            }
            return;
        }
        this.mVivoFakeGpsProvider.onStartNavigating();
        this.isFakeGpsProviderStarted = true;
        if (this.DEBUG) {
            VLog.d(TAG, "Fake:onStartNavigating");
        }
    }

    public void onStopNavigating() {
        if (isFusedLocationSupported) {
            this.mVivoGpsStateMachine.onStopNavigating();
            if (this.DEBUG) {
                VLog.d(TAG, "Fused:onStopNavigating");
                return;
            }
            return;
        }
        this.mVivoFakeGpsProvider.onStopNavigating();
        this.isFakeGpsProviderStarted = false;
        if (this.DEBUG) {
            VLog.d(TAG, "Fake:onStopNavigating");
        }
    }

    public void onReportGpsLocation(Location location) {
        if (isFusedLocationSupported) {
            this.mVivoGpsStateMachine.onReportGpsLocation(location);
            if (this.DEBUG) {
                VLog.d(TAG, "Fused:onReportGpsLocation");
                return;
            }
            return;
        }
        this.mVivoFakeGpsProvider.onReportGpsLocation(location);
        if (this.DEBUG) {
            VLog.d(TAG, "Fake:onReportGpsLocation");
        }
    }

    public void onSetRequest(ProviderRequest request, WorkSource source) {
        this.mRequest = request;
        this.mSource = source;
        if (isFusedLocationSupported) {
            this.mVivoGpsStateMachine.onSetRequest(request, source);
            if (this.DEBUG) {
                VLog.d(TAG, "Fused:onSetRequest");
                return;
            }
            return;
        }
        this.mVivoFakeGpsProvider.onSetRequest(request, source);
        if (this.DEBUG) {
            VLog.d(TAG, "Fake:onSetRequest");
        }
    }

    public void onReportSvStatus(int svCount, float[] mCn0s, float[] mSvElevations, float[] mSvAzimuths) {
        if (isFusedLocationSupported) {
            this.mVivoGpsStateMachine.onReportSvStatus(svCount, mCn0s, mSvElevations, mSvAzimuths);
            if (this.DEBUG) {
                VLog.d(TAG, "Fused:onReportSvStatus");
            }
        }
    }

    public void reportFusedLocation(Location location) {
        reportFusedLocation(location, true);
    }

    public void reportFusedLocation(Location location, boolean isFused) {
        if (!isFused) {
            this.mGnssLocationProvider.reportFusedLocation(location);
            return;
        }
        String dbProp = SystemProperties.get("persist.sys.FusedGpsDebug", "none");
        if ("dg".equals(dbProp)) {
            VLog.d(TAG, "Fused: fake dg location.");
            location.setLatitude(22.774855d);
            location.setLongitude(113.761268d);
        } else if ("sz".equals(dbProp)) {
            VLog.d(TAG, "Fused: fake sz location.");
            location.setLatitude(22.577355d);
            location.setLongitude(114.00654d);
        }
        if (isFused && !location.hasBearing()) {
            location.setBearing((float) Math.random());
        }
        if (isFused && !location.hasSpeed()) {
            location.setSpeed((float) Math.random());
        }
        Bundle bundle = location.getExtras();
        if (bundle == null) {
            bundle = new Bundle();
        }
        if (bundle.getInt("satellites") == 0) {
            bundle.putInt("satellites", 31);
        }
        if (bundle.getInt("meanCn0") == 0) {
            bundle.putInt("meanCn0", ((int) (Math.random() * 5.0d)) + 27);
        }
        if (bundle.getInt("maxCn0") == 0) {
            bundle.putInt("maxCn0", 41);
        }
        location.setExtras(bundle);
        this.mGnssLocationProvider.reportFusedLocation(location);
    }

    public void enableGps(boolean enable) {
        this.mGnssLocationProvider.enableGps(enable);
    }

    /* loaded from: classes.dex */
    public static class FakeGpsConfig {
        public boolean support;
        public String[] whiteList;

        public FakeGpsConfig(boolean support, String[] list) {
            this.support = false;
            this.whiteList = new String[0];
            this.support = support;
            if (list != null) {
                this.whiteList = list;
            }
        }
    }

    /* loaded from: classes.dex */
    public static class FusedGpsConfig {
        public HashMap<String, Integer> parameterHashmap;
        public boolean support;
        public String[] whiteList;

        public FusedGpsConfig(boolean support, String[] list, HashMap<String, Integer> hashmap) {
            this.support = false;
            this.whiteList = new String[0];
            this.parameterHashmap = new HashMap<>();
            this.support = support;
            if (list != null) {
                this.whiteList = list;
            }
            if (hashmap != null) {
                this.parameterHashmap = hashmap;
            }
        }
    }

    public void parseFakeGpsConfig(ContentValuesList list) {
        String[] maps;
        if (list == null) {
            return;
        }
        boolean support = Boolean.parseBoolean(list.getValue("support"));
        String mapList = list.getValue("mapList");
        if (mapList != null) {
            maps = mapList.split(",");
        } else {
            maps = new String[0];
        }
        FakeGpsConfig fakeGpsConfig = new FakeGpsConfig(support, maps);
        this.mFakeGpsConfig = fakeGpsConfig;
        VivoFakeGpsProvider vivoFakeGpsProvider = this.mVivoFakeGpsProvider;
        if (vivoFakeGpsProvider != null) {
            vivoFakeGpsProvider.onFakeGpsConfigChanged(fakeGpsConfig);
        } else if (this.DEBUG) {
            VLog.d(TAG, "parseFakeGpsConfig mVivoFakeGpsProvider is null.");
        }
    }

    public void parseFusedGpsConfig(ContentValuesList list) {
        String[] projectList;
        String[] maps;
        WorkSource workSource;
        if (list == null) {
            return;
        }
        String device = Build.DEVICE;
        if (device != null) {
            device = device.toLowerCase();
        }
        String projects = list.getValue("project");
        if (projects != null) {
            projectList = projects.split(",");
        } else {
            projectList = new String[0];
        }
        if (device != null && projectList != null && projectList.length > 0) {
            for (String temp : projectList) {
                if (device.equals(temp)) {
                    VLog.d(TAG, "VivoFusedLocation support on " + device);
                    boolean support = Boolean.parseBoolean(list.getValue("support"));
                    String mapList = list.getValue("mapList");
                    if (mapList != null) {
                        maps = mapList.split(",");
                    } else {
                        maps = new String[0];
                    }
                    HashMap<String, Integer> hashmaps = new HashMap<>();
                    hashmaps.put("GpsScoreThreshold", Integer.valueOf(Integer.parseInt(list.getValue("GpsScoreThreshold"))));
                    hashmaps.put("L4WifiCount", Integer.valueOf(Integer.parseInt(list.getValue("L4WifiCount"))));
                    hashmaps.put("L3WifiCount", Integer.valueOf(Integer.parseInt(list.getValue("L3WifiCount"))));
                    hashmaps.put("L4WifiScore", Integer.valueOf(Integer.parseInt(list.getValue("L4WifiScore"))));
                    hashmaps.put("L3WifiScore", Integer.valueOf(Integer.parseInt(list.getValue("L3WifiScore"))));
                    FusedGpsConfig fusedGpsConfig = new FusedGpsConfig(support, maps, hashmaps);
                    this.mFusedGpsConfig = fusedGpsConfig;
                    VivoGpsStateMachine vivoGpsStateMachine = this.mVivoGpsStateMachine;
                    if (vivoGpsStateMachine != null) {
                        vivoGpsStateMachine.onFusedGpsConfigChanged(fusedGpsConfig);
                        isFusedLocationSupported = true;
                        ProviderRequest providerRequest = this.mRequest;
                        if (providerRequest != null && (workSource = this.mSource) != null) {
                            this.mVivoGpsStateMachine.onSetRequest(providerRequest, workSource);
                        }
                        if (this.isFakeGpsProviderStarted) {
                            this.mVivoFakeGpsProvider.onStopNavigating();
                            this.mVivoGpsStateMachine.onStartNavigating();
                            this.isFakeGpsProviderStarted = false;
                        }
                    } else if (this.DEBUG) {
                        VLog.d(TAG, "parseFusedGpsConfig mVivoGpsStateMachine is null.");
                    }
                }
            }
        }
    }

    public void setDebug(boolean debug) {
        this.DEBUG = debug;
        this.mVivoGpsStateMachine.setDebug(debug);
        this.mVivoFakeGpsProvider.setDebug(debug);
        if (this.DEBUG) {
            VLog.d(TAG, "setDebug");
        }
    }
}