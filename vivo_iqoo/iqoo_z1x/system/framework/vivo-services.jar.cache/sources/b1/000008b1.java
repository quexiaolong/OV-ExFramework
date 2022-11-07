package com.vivo.services.vgc.cbs;

import android.os.Environment;
import android.os.Handler;
import com.vivo.face.common.data.Constants;
import com.vivo.services.vgc.cbs.carrier.CarrierManagerFactory;
import com.vivo.services.vgc.cbs.carrier.CbsCarrierManager;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class CbsSettings {
    private static final String CONFIG_BACKUP_FILENAME = "cbs_config.xml.backup";
    private static final String CONFIG_FILENAME = "cbs_config.xml";
    public static final String KEY_APP_STATE = "app_state_";
    public static final String KEY_CURRENT_STATE = "current_state";
    public static final String KEY_MAP_KEY = "map_key";
    public static final String KEY_PREV_GID1 = "prev_gid1";
    public static final String KEY_PREV_GID2 = "prev_gid2";
    public static final String KEY_PREV_ICCID = "prev_iccid";
    public static final String KEY_PREV_IMSI = "prev_imsi";
    public static final String KEY_PREV_MCCMNC = "prev_mccmnc";
    public static final String KEY_PREV_SPN = "prev_spn";
    private static final String TAG = CbsUtils.TAG;
    private File mBackupFile;
    private CbsCarrierSimInfos mCarrierSimInfos;
    private CbsCarrierManager mCbsCarrierManager;
    private File mFile;
    private Handler mHandler;
    private Properties mProps;
    private ArrayList<String> mSimTriggerList;
    private File mUsersDir;
    private boolean mOnceRetry = true;
    private final Runnable mSaveRunnable = new Runnable() { // from class: com.vivo.services.vgc.cbs.CbsSettings.1
        @Override // java.lang.Runnable
        public void run() {
            CbsSettings.this.saveSettingsImpl();
        }
    };

    public CbsSettings(Handler handler) {
        this.mHandler = handler;
    }

    public void loadSettings(List<String> simInfoList) {
        if (CbsUtils.DEBUG) {
            VSlog.d(TAG, "CbsSettings  loadSettings");
        }
        this.mCarrierSimInfos = new CbsCarrierSimInfos();
        for (String file : simInfoList) {
            this.mCarrierSimInfos.loadCarrierSimInfo(new File(file));
        }
        this.mSimTriggerList = new ArrayList<>();
        List<CbsSimInfo> list = this.mCarrierSimInfos.getSimInfoFromFile(new File(CbsUtils.DEF_CARRIER_SIMINFO));
        for (CbsSimInfo info : list) {
            this.mSimTriggerList.add(info.getMapKey());
        }
        File file2 = new File(Environment.getDataDirectory(), "system");
        this.mUsersDir = file2;
        file2.mkdirs();
        this.mProps = new Properties();
        this.mBackupFile = new File(this.mUsersDir, CONFIG_BACKUP_FILENAME);
        File file3 = new File(this.mUsersDir, CONFIG_FILENAME);
        this.mFile = file3;
        if (file3.exists()) {
            loadPropertys();
        } else if (this.mBackupFile.exists()) {
            handleConfigFileBroken();
            loadPropertys();
            saveSettings();
        }
        this.mCbsCarrierManager = CarrierManagerFactory.getInstance().getCarrierManager(this.mCarrierSimInfos.getSimInfoList());
    }

    public ArrayList<String> getSimTriggerMapkeys() {
        return this.mSimTriggerList;
    }

    public CbsSimInfo getMapSimInfo(CbsSimInfo matchInfo) {
        return this.mCbsCarrierManager.getMapSimInfo(matchInfo);
    }

    public int getSimCardFlag() {
        String iccid = getPropValue(KEY_PREV_ICCID, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        String imsi = getPropValue(KEY_PREV_IMSI, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        String mccmnc = getPropValue(KEY_PREV_MCCMNC, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        String gid1 = getPropValue(KEY_PREV_GID1, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        String gid2 = getPropValue(KEY_PREV_GID2, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        String spn = getPropValue(KEY_PREV_SPN, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        CbsSimInfo simInfo = new CbsSimInfo(mccmnc, gid1, gid2, spn, iccid, imsi);
        return this.mCbsCarrierManager.getSimCardFlag(simInfo);
    }

    public int getCurrentState() {
        return getPropValue(KEY_CURRENT_STATE, 0);
    }

    public void setCurrentState(int state) {
        setPropValue(KEY_CURRENT_STATE, state);
    }

    public int getAppState(String moduleId) {
        return getPropValue(KEY_APP_STATE + moduleId, 0);
    }

    public void setAppState(String moduleId, Integer state) {
        setPropValue(KEY_APP_STATE + moduleId, Integer.toString(state.intValue()));
    }

    public String getPropValue(String key, String defValue) {
        return this.mProps.getProperty(key, defValue);
    }

    public int getPropValue(String key, int defValue) {
        String value = this.mProps.getProperty(key, String.valueOf(defValue));
        return CbsUtils.parseInt(value, defValue);
    }

    public void setPropValue(String key, String value) {
        if (key == null || value == null) {
            return;
        }
        synchronized (this.mProps) {
            this.mProps.put(key, value);
        }
        saveSettings();
    }

    public void setPropValue(String key, int value) {
        setPropValue(key, Integer.toString(value));
    }

    private void handleConfigFileBroken() {
        try {
            this.mBackupFile.renameTo(this.mFile);
        } catch (Exception e) {
            if (CbsUtils.DEBUG) {
                String str = TAG;
                VSlog.e(str, "handleConfigFileBroken Exception:" + e.getMessage());
            }
        }
        this.mFile = new File(this.mUsersDir, CONFIG_FILENAME);
    }

    private void loadPropertys() {
        String str;
        StringBuilder sb;
        this.mProps.clear();
        FileInputStream in = null;
        try {
            try {
                in = new FileInputStream(this.mFile);
                this.mProps.loadFromXML(in);
                try {
                    in.close();
                } catch (Exception e) {
                    e = e;
                    str = TAG;
                    sb = new StringBuilder();
                    sb.append("loadPropertys Exception:");
                    sb.append(e.getMessage());
                    VSlog.i(str, sb.toString());
                }
            } catch (Exception e2) {
                if (CbsUtils.DEBUG) {
                    String str2 = TAG;
                    VSlog.i(str2, "loadPropertys Exception:" + e2.getMessage());
                    e2.printStackTrace();
                }
                if (this.mOnceRetry && this.mBackupFile.exists()) {
                    this.mOnceRetry = false;
                    handleConfigFileBroken();
                    loadPropertys();
                    saveSettings();
                }
                if (in != null) {
                    try {
                        in.close();
                    } catch (Exception e3) {
                        e = e3;
                        str = TAG;
                        sb = new StringBuilder();
                        sb.append("loadPropertys Exception:");
                        sb.append(e.getMessage());
                        VSlog.i(str, sb.toString());
                    }
                }
            }
        } catch (Throwable th) {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e4) {
                    String str3 = TAG;
                    VSlog.i(str3, "loadPropertys Exception:" + e4.getMessage());
                }
            }
            throw th;
        }
    }

    public void saveImmediately() {
        saveSettingsImpl();
    }

    private void saveSettings() {
        this.mHandler.removeCallbacks(this.mSaveRunnable);
        this.mHandler.postDelayed(this.mSaveRunnable, 500L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Removed duplicated region for block: B:83:0x0077 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public void saveSettingsImpl() {
        /*
            Method dump skipped, instructions count: 285
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.vivo.services.vgc.cbs.CbsSettings.saveSettingsImpl():void");
    }

    public CbsCarrierSimInfos getCarrierSimInfos() {
        return this.mCarrierSimInfos;
    }
}