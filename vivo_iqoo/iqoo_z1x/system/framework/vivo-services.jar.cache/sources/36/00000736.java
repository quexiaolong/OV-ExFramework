package com.vivo.services.rms.sdk;

import android.util.SparseArray;
import com.vivo.face.common.data.Constants;

/* loaded from: classes.dex */
public class Consts {

    /* loaded from: classes.dex */
    public static class ProcessEvent {
        public static final int ADD = 5;
        public static final int ADD_DEPPKG = 8;
        public static final int ADD_PKG = 9;
        public static final int NOTIFY_SPECIAL = 12;
        public static final int REMOVE = 6;
        public static final int SET_ADJ = 0;
        public static final int SET_ADJTYPE = 3;
        public static final int SET_CONFIG = 10;
        public static final int SET_OOM = 4;
        public static final int SET_SCHEDGROUP = 2;
        public static final int SET_STATES = 1;
        public static final int START_ACTIVITY = 7;
        public static final int START_ACTIVITY_ON_VD = 11;
    }

    /* loaded from: classes.dex */
    public static class SystemEvent {
        public static final int SET_APP_LIST = 0;
        public static final int SET_BUNDLE = 1;
    }

    /* loaded from: classes.dex */
    public static class ProcessStates {
        public static final String FGACTIVITIES_NAME = "fgActivity";
        public static final int FGACTIVITY = 1;
        public static final int FGFORCE = 2;
        public static final String FGFORCE_NAME = "fgForce";
        public static final int FGSERVICE = 4;
        public static final String FGSERVICES_NAME = "fgService";
        public static final int FOCUS = 1024;
        public static final String FOCUS_NAME = "focus";
        public static final int HASACTIVITY = 64;
        public static final String HASACTIVITY_NAME = "hasActivity";
        public static final int HASNOTIFICATION = 256;
        public static final String HASNOTIFICATION_NAME = "hasNoti";
        public static final int HASSERVICE = 128;
        public static final String HASSERVICE_NAME = "hasService";
        public static final int HASSHOWNUI = 32;
        public static final String HASSHOWNUI_NAME = "hasShown";
        public static final int PAUSING = 512;
        public static final String PAUSING_NAME = "pausing";
        public static final SparseArray<String> STATES_NAMES;
        public static final int VIRTUAL_DISPLAY = 2048;
        public static final String VIRTUAL_DISPLAY_NAME = "virtual";
        public static final int VISIBLE = 8;
        public static final String VISIBLE_NAME = "visible";
        public static final int WORKING = 16;
        public static final String WORKING_NAME = "working";

        static {
            SparseArray<String> sparseArray = new SparseArray<>();
            STATES_NAMES = sparseArray;
            sparseArray.put(1, FGACTIVITIES_NAME);
            STATES_NAMES.put(2, FGFORCE_NAME);
            STATES_NAMES.put(4, FGSERVICES_NAME);
            STATES_NAMES.put(8, VISIBLE_NAME);
            STATES_NAMES.put(16, WORKING_NAME);
            STATES_NAMES.put(32, HASSHOWNUI_NAME);
            STATES_NAMES.put(256, HASNOTIFICATION_NAME);
            STATES_NAMES.put(64, HASACTIVITY_NAME);
            STATES_NAMES.put(128, HASSERVICE_NAME);
            STATES_NAMES.put(512, PAUSING_NAME);
            STATES_NAMES.put(FOCUS, FOCUS_NAME);
            STATES_NAMES.put(VIRTUAL_DISPLAY, VIRTUAL_DISPLAY_NAME);
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
}