package com.android.server.location;

import android.os.SystemProperties;
import com.android.server.location.VivoLocConf;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import java.util.HashMap;
import vivo.app.configuration.ContentValuesList;

/* loaded from: classes.dex */
public class VivoLocationFeatureConfig {
    private static final String TAG = "VivoLocationFeatureConfig";
    public static boolean DBG = false;
    private static VivoLocationFeatureConfig sInstance = null;
    private String mFakeLocSwitchState = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    private String mFakeLocWhiteList = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    private int mVPDRSwitchState = -1;
    private String mVPDRWhiteList = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    private int mVPDRSubSwitchState = -1;
    private int mVPDRVDRMaxTime = -1;
    private boolean mVPDRDCStatus = false;
    private boolean mAPIControlState = false;
    private HashMap<String, String> mAPIControlList = new HashMap<>();
    private HashMap<String, String> mDiagnosticConfigs = new HashMap<>();

    public static /* synthetic */ void lambda$3SCFU49ZSfEQyODNnMLmfHTCQuk(VivoLocationFeatureConfig vivoLocationFeatureConfig, ContentValuesList contentValuesList) {
        vivoLocationFeatureConfig.updateDiagnostic(contentValuesList);
    }

    public static /* synthetic */ void lambda$RZUPvqr3ed15hN3eHNdphI4ze8s(VivoLocationFeatureConfig vivoLocationFeatureConfig, ContentValuesList contentValuesList) {
        vivoLocationFeatureConfig.updateApiControl(contentValuesList);
    }

    public static /* synthetic */ void lambda$le5OahRdDdPDk9hU66T859JVZBY(VivoLocationFeatureConfig vivoLocationFeatureConfig, ContentValuesList contentValuesList) {
        vivoLocationFeatureConfig.updateFakeLocState(contentValuesList);
    }

    public static /* synthetic */ void lambda$oL30A7dCUSrgVM3V1giPNAQ2QUY(VivoLocationFeatureConfig vivoLocationFeatureConfig, ContentValuesList contentValuesList) {
        vivoLocationFeatureConfig.updateVPDRConfig(contentValuesList);
    }

    public static VivoLocationFeatureConfig getInstance() {
        if (sInstance == null) {
            sInstance = new VivoLocationFeatureConfig();
        }
        return sInstance;
    }

    private VivoLocationFeatureConfig() {
        VivoLocConf config = VivoLocConf.getInstance();
        config.registerListener(VivoLocConf.FAKE_LOC_STATE, new VivoLocConf.ContentValuesListChangedListener() { // from class: com.android.server.location.-$$Lambda$VivoLocationFeatureConfig$le5OahRdDdPDk9hU66T859JVZBY
            @Override // com.android.server.location.VivoLocConf.ContentValuesListChangedListener
            public final void onConfigChanged(ContentValuesList contentValuesList) {
                VivoLocationFeatureConfig.lambda$le5OahRdDdPDk9hU66T859JVZBY(VivoLocationFeatureConfig.this, contentValuesList);
            }
        });
        config.registerListener(VivoLocConf.VPDR_CONFIG, new VivoLocConf.ContentValuesListChangedListener() { // from class: com.android.server.location.-$$Lambda$VivoLocationFeatureConfig$oL30A7dCUSrgVM3V1giPNAQ2QUY
            @Override // com.android.server.location.VivoLocConf.ContentValuesListChangedListener
            public final void onConfigChanged(ContentValuesList contentValuesList) {
                VivoLocationFeatureConfig.lambda$oL30A7dCUSrgVM3V1giPNAQ2QUY(VivoLocationFeatureConfig.this, contentValuesList);
            }
        });
        config.registerListener(VivoLocConf.API_CONTROL, new VivoLocConf.ContentValuesListChangedListener() { // from class: com.android.server.location.-$$Lambda$VivoLocationFeatureConfig$RZUPvqr3ed15hN3eHNdphI4ze8s
            @Override // com.android.server.location.VivoLocConf.ContentValuesListChangedListener
            public final void onConfigChanged(ContentValuesList contentValuesList) {
                VivoLocationFeatureConfig.lambda$RZUPvqr3ed15hN3eHNdphI4ze8s(VivoLocationFeatureConfig.this, contentValuesList);
            }
        });
        config.registerListener(VivoLocConf.LOCATION_DIAGNOSTIC, new VivoLocConf.ContentValuesListChangedListener() { // from class: com.android.server.location.-$$Lambda$VivoLocationFeatureConfig$3SCFU49ZSfEQyODNnMLmfHTCQuk
            @Override // com.android.server.location.VivoLocConf.ContentValuesListChangedListener
            public final void onConfigChanged(ContentValuesList contentValuesList) {
                VivoLocationFeatureConfig.lambda$3SCFU49ZSfEQyODNnMLmfHTCQuk(VivoLocationFeatureConfig.this, contentValuesList);
            }
        });
        config.registerListener(VivoLocConf.OTHERS, new VivoLocConf.ContentValuesListChangedListener() { // from class: com.android.server.location.-$$Lambda$AGLglpb-PhUxjZKGwVlZx0kNx5M
            @Override // com.android.server.location.VivoLocConf.ContentValuesListChangedListener
            public final void onConfigChanged(ContentValuesList contentValuesList) {
                VivoLocationFeatureConfig.this.updateOthers(contentValuesList);
            }
        });
    }

    public void updateFakeLocState(ContentValuesList list) {
        this.mFakeLocSwitchState = list.getValue("switchState");
        this.mFakeLocWhiteList = list.getValue("whiteList");
        SystemProperties.set("sys.location.fakestate", this.mFakeLocSwitchState + Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        if (DBG) {
            VLog.d(TAG, "updateFakeLocState, mFakeLocSwitchState:" + this.mFakeLocSwitchState + " mFakeLocWhiteList:" + this.mFakeLocWhiteList);
        }
    }

    public void updateVPDRConfig(ContentValuesList list) {
        String blModel = list.getValue("blacklistModel");
        String currentModel = SystemProperties.get("ro.boot.vivo.hardware.pcb.detect", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        if (DBG) {
            VLog.d(TAG, "currentModel: " + currentModel);
        }
        if (blModel != null && !blModel.isEmpty()) {
            String[] blModelList = blModel.split(",");
            if (currentModel != null && blModelList != null && blModelList.length > 0) {
                for (int i = 0; i < blModelList.length; i++) {
                    if (!blModelList[i].isEmpty() && currentModel.contains(blModelList[i])) {
                        if (DBG) {
                            VLog.d(TAG, "This project do not use vivo dr.");
                        }
                        this.mVPDRSwitchState = 0;
                        return;
                    }
                }
            }
        }
        String blPlatform = list.getValue("blacklistPlatform");
        String currentPlatform = SystemProperties.get("ro.vivo.product.platform", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        if (DBG) {
            VLog.d(TAG, "currentPlatform: " + currentPlatform);
        }
        if (blPlatform != null && !blPlatform.isEmpty()) {
            String[] blPlatformList = blPlatform.split(",");
            if (currentPlatform != null && blPlatformList != null && blPlatformList.length > 0) {
                for (int i2 = 0; i2 < blPlatformList.length; i2++) {
                    if (!blPlatformList[i2].isEmpty() && currentPlatform.equals(blPlatformList[i2])) {
                        if (DBG) {
                            VLog.d(TAG, "This platform do not use vivo dr.");
                        }
                        this.mVPDRSwitchState = 0;
                        return;
                    }
                }
            }
        }
        String switchState = list.getValue("switchState");
        if ("off".equals(switchState)) {
            this.mVPDRSwitchState = 0;
        } else if ("on".equals(switchState)) {
            this.mVPDRSwitchState = 1;
        } else {
            this.mVPDRSwitchState = -1;
        }
        this.mVPDRWhiteList = list.getValue("whiteList");
        this.mVPDRSubSwitchState = Integer.parseInt(list.getValue("subSwitchState"));
        this.mVPDRVDRMaxTime = Integer.parseInt(list.getValue("VDRMaxTime"));
        String dcStatus = list.getValue("dc_status");
        if (dcStatus != null && "on".equals(dcStatus)) {
            this.mVPDRDCStatus = true;
        } else {
            this.mVPDRDCStatus = false;
        }
        if (DBG) {
            VLog.d(TAG, "updateVPDRConfig mVPDRSwitchState:" + this.mVPDRSwitchState + " mVPDRWhiteList:" + this.mVPDRWhiteList + " mVPDRSubSwitchState:" + this.mVPDRSubSwitchState + " mVPDRVDRMaxTime:" + this.mVPDRVDRMaxTime + " mVPDRDCStatus:" + this.mVPDRDCStatus);
        }
    }

    public void updateApiControl(ContentValuesList list) {
        String packageName;
        String state = list.getValue("switchState");
        if (state != null && "on".equals(state)) {
            this.mAPIControlState = true;
        } else {
            this.mAPIControlState = false;
        }
        int index = 1;
        HashMap<String, String> aPIControlList = new HashMap<>();
        do {
            packageName = list.getValue("pkg" + index);
            String fobiddenList = list.getAttr("pkg" + index, "forbid");
            if (packageName != null && fobiddenList != null) {
                aPIControlList.put(packageName, fobiddenList);
            }
            index++;
        } while (packageName != null);
        this.mAPIControlList = aPIControlList;
        if (DBG) {
            VLog.d(TAG, "updateApiControl mAPIControlState:" + this.mAPIControlState + " mAPIControlList:" + this.mAPIControlList);
        }
    }

    public void updateDiagnostic(ContentValuesList list) {
        HashMap<String, String> diagnosticConfigs = new HashMap<>();
        String value = list.getValue("switchState");
        if (value != null && !value.isEmpty()) {
            diagnosticConfigs.put("switchState", value);
        }
        String value2 = list.getValue("ReportEvents");
        if (value2 != null && !value2.isEmpty()) {
            diagnosticConfigs.put("ReportEvents", value2);
        }
        String value3 = list.getValue("ReportIntervals");
        if (value3 != null && !value3.isEmpty()) {
            diagnosticConfigs.put("ReportIntervals", value3);
        }
        String value4 = list.getValue("ReportContents");
        if (value4 != null && !value4.isEmpty()) {
            diagnosticConfigs.put("ReportContents", value4);
        }
        String value5 = list.getValue("ServerTrigger");
        if (value5 != null && !value5.isEmpty()) {
            diagnosticConfigs.put("ServerTrigger", value5);
        }
        String value6 = list.getValue("ServerTriggerType");
        if (value6 != null && !value6.isEmpty()) {
            diagnosticConfigs.put("ServerTriggerType", value6);
        }
        String value7 = list.getValue("ServerTriggerDelay");
        if (value7 != null && !value7.isEmpty()) {
            diagnosticConfigs.put("ServerTriggerDelay", value7);
        }
        this.mDiagnosticConfigs = diagnosticConfigs;
        VivoLocationDiagnosticManager diagnoster = VivoLocationDiagnosticManager.getInstance();
        diagnoster.onConfigChange(this.mDiagnosticConfigs);
    }

    public void updateOthers(ContentValuesList list) {
        try {
            String serverLogLevel = list.getValue("qcomLogProp");
            String restartConfig = list.getValue("restartLauncherProp");
            String currentLogLevel = SystemProperties.get("persist.vivo.gnss.log.level", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
            if (serverLogLevel != null && !serverLogLevel.isEmpty() && !serverLogLevel.equals(currentLogLevel)) {
                if (DBG) {
                    VLog.d(TAG, "set qcomLogProp:" + serverLogLevel + " current:" + currentLogLevel);
                }
                SystemProperties.set("persist.vivo.gnss.log.level", serverLogLevel);
                if (restartConfig != null && !restartConfig.isEmpty()) {
                    if (DBG) {
                        VLog.d(TAG, "restartLauncherProp:" + restartConfig);
                    }
                    SystemProperties.set("persist.vivo.gnss.restart_launcher", restartConfig);
                }
            }
        } catch (Exception e) {
            VLog.e(TAG, e.toString());
        }
    }

    public String getWhiteListAppForFakeLocState() {
        String str = this.mFakeLocSwitchState;
        if (str != null && "on".equals(str)) {
            return this.mFakeLocWhiteList;
        }
        return null;
    }

    public String getWhiteListForVPDR() {
        return this.mVPDRWhiteList;
    }

    public int getVPDRState() {
        return this.mVPDRSwitchState;
    }

    public int getSubSwitchStateForVPDR() {
        return this.mVPDRSubSwitchState;
    }

    public int getMaxVDRPredictTimeForVPDR() {
        return this.mVPDRVDRMaxTime;
    }

    public boolean getDCStatusForVPDR() {
        return this.mVPDRDCStatus;
    }

    public boolean getApiControlStatus() {
        return this.mAPIControlState;
    }

    public boolean getApiAllowedState(String api, String packageName) {
        String blackList = this.mAPIControlList.get(packageName);
        if (blackList == null || !blackList.contains(api)) {
            return true;
        }
        return false;
    }
}