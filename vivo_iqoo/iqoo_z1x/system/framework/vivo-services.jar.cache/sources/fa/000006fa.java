package com.vivo.services.rms.appmng;

import android.util.SparseArray;
import com.android.server.am.frozen.WorkingStateManager;
import com.vivo.face.common.data.Constants;

/* loaded from: classes.dex */
public class WorkingState {
    public static final int AUDIO = 2;
    public static final String AUDIO_NAME = "audio";
    public static final int DOWNLOAD = 4;
    public static final String DOWNLOAD_NAME = "download";
    public static final int NAVIGATION = 16;
    public static final String NAVIGATION_NAME = "navigation";
    public static final int NONE = 0;
    public static final String NONE_NAME = "none";
    public static final int RECORD = 1;
    public static final String RECORD_NAME = "record";
    public static final SparseArray<String> STATES_NAMES;

    static {
        SparseArray<String> sparseArray = new SparseArray<>();
        STATES_NAMES = sparseArray;
        sparseArray.put(1, "record");
        STATES_NAMES.put(2, "audio");
        STATES_NAMES.put(4, "download");
        STATES_NAMES.put(16, "navigation");
    }

    public static void notifyWorkingState(String pkgName, int uid, int mask, boolean state) {
        AppManager.getInstance().updateWorkingState(pkgName, uid, mask, state);
        WorkingStateManager.getInstance().updateWorkingStateByRms(pkgName, uid, mask, state);
    }

    public static void resetWorkingState() {
        AppManager.getInstance().resetWorkingState();
    }

    public static String getName(int state) {
        if (state == 0) {
            return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        }
        StringBuilder builder = new StringBuilder(24);
        for (int i = 0; i < STATES_NAMES.size(); i++) {
            if ((STATES_NAMES.keyAt(i) & state) != 0) {
                builder.append(STATES_NAMES.valueAt(i));
                builder.append(" ");
            }
        }
        return builder.substring(0, builder.length() - 1);
    }
}