package com.android.server.policy;

import android.media.AudioSystem;
import android.os.FtBuild;
import android.text.TextUtils;
import com.vivo.common.utils.VLog;
import com.vivo.framework.configuration.ConfigurationManager;
import java.util.ArrayList;
import vivo.app.configuration.AbsConfigurationManager;
import vivo.app.configuration.ConfigurationObserver;
import vivo.app.configuration.StringList;

/* loaded from: classes.dex */
public class SmartClickManager {
    private static final String FORCE_START_LIST_NAME = "force_start_when_music_active";
    private static final String POLICY_CONFIG_FILE = "/data/bbkcore/SmartClick_smart_click_force_start_2.0.xml";
    private static final String TAG = "SmartClickManager";
    private AbsConfigurationManager mConfigurationManager;
    private ArrayList<String> mForceStartWhiteList = new ArrayList<>();
    private StringList mStringList;

    public void onSystemReady() {
        if (FtBuild.isOverSeas()) {
            return;
        }
        this.mConfigurationManager = ConfigurationManager.getInstance();
        updateConfig();
        registerConfigCallback();
    }

    public boolean isStartWhenInMusic(boolean isMusicActive) {
        if (!isMusicActive) {
            return true;
        }
        if (FtBuild.isOverSeas()) {
            return false;
        }
        boolean result = false;
        boolean hasPkgNotInList = false;
        String pkgsInMusic = AudioSystem.getParameters("check_play_active_pkg");
        if (!TextUtils.isEmpty(pkgsInMusic)) {
            String[] pkgsArray = pkgsInMusic.split(";");
            if (pkgsArray.length == 0) {
                hasPkgNotInList = true;
            }
            synchronized (this.mForceStartWhiteList) {
                int i = 0;
                while (true) {
                    if (i < pkgsArray.length) {
                        if (TextUtils.isEmpty(pkgsArray[i]) || this.mForceStartWhiteList.contains(pkgsArray[i])) {
                            i++;
                        } else {
                            hasPkgNotInList = true;
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }
            if (!hasPkgNotInList) {
                result = true;
            }
            VLog.d(TAG, "play active packages:" + pkgsInMusic + " isStarted = " + result);
            return result;
        }
        VLog.i(TAG, "pkg list is null or empty");
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class SmartClickConfigObserver extends ConfigurationObserver {
        SmartClickConfigObserver() {
        }

        public void onConfigChange(String file, String name) {
            VLog.d(SmartClickManager.TAG, "onConfigChange file:" + file + " name=" + name);
            if (SmartClickManager.this.mConfigurationManager != null) {
                SmartClickManager.this.updateConfig();
            } else {
                VLog.i(SmartClickManager.TAG, "mConfigurationManager is null");
            }
        }
    }

    private void registerConfigCallback() {
        this.mConfigurationManager.registerObserver(this.mStringList, new SmartClickConfigObserver());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateConfig() {
        StringList stringList = this.mConfigurationManager.getStringList(POLICY_CONFIG_FILE, FORCE_START_LIST_NAME);
        this.mStringList = stringList;
        ArrayList<String> tempList = stringList == null ? null : (ArrayList) stringList.getValues();
        if (tempList != null) {
            if (tempList.size() == 0) {
                VLog.d(TAG, "force start list name is empty");
            }
            synchronized (this.mForceStartWhiteList) {
                this.mForceStartWhiteList.clear();
                this.mForceStartWhiteList.addAll(tempList);
            }
        }
        VLog.d(TAG, "updateConfig: mForceStartWhiteList=" + tempList);
    }
}