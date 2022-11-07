package com.android.server.display;

import android.content.Context;
import android.hardware.display.VivoDisplayModule;
import android.os.SystemProperties;
import android.view.Display;
import com.vivo.face.common.data.Constants;
import java.util.ArrayList;
import java.util.Random;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public final class VivoDisplayModuleController {
    private static final int ERROR_CODE = -1;
    private static final String PROP_VIVO_LOG_CTRL = "persist.sys.log.ctrl";
    private static final long STATE_BIT_UNKNOWN = -1;
    private static final int STATE_DISPLAY_OFF = 0;
    private static final String TAG = "VivoDisplayModuleController";
    private Context mContext;
    private long mDisplayContentVisible;
    private int mDisplayId;
    private VivoDisplayOverlayController mDisplayOverlayController;
    private String mDisplayStr;
    private int mModuleRegistered;
    private int mModuleVisibleNum;
    private final ArrayList<Pack> mPackList = new ArrayList<>();
    private int mStateDozePoll;
    private int mStateOnPoll;
    private static final Object LOCK = new Object();
    private static boolean DEBUG = SystemProperties.get("persist.sys.log.ctrl", "no").equals("yes");

    public VivoDisplayModuleController(Context context, int displayId) {
        this.mStateOnPoll = 0;
        this.mStateDozePoll = 0;
        this.mModuleVisibleNum = 0;
        this.mModuleRegistered = 0;
        this.mDisplayContentVisible = 0L;
        this.mStateOnPoll = 0;
        this.mStateDozePoll = 0;
        this.mModuleRegistered = 0;
        this.mModuleVisibleNum = 0;
        this.mDisplayId = displayId;
        this.mDisplayContentVisible = 0L;
        this.mDisplayStr = displayId == 0 ? "primary-display" : "secondary-display";
        this.mContext = context;
        this.mDisplayOverlayController = new VivoDisplayOverlayController(this.mContext, this.mDisplayId);
    }

    public int registerModule(VivoDisplayModule displayModule) {
        String module = displayModule.getName().toString();
        if (checkPermission(module)) {
            int index = findModuleIndex(module);
            if (index < 0) {
                int moduleId = generateModuleId();
                Pack pack = new Pack(moduleId, module, 1 << this.mModuleRegistered);
                pack.setDisplayModule(displayModule);
                pack.setRequestState(0);
                pack.setVisibleState(false);
                this.mPackList.add(pack);
                this.mModuleRegistered++;
                if (DEBUG) {
                    VSlog.d(TAG, module + " registered with module id " + moduleId + " in " + this.mDisplayStr + " total-registered " + this.mModuleRegistered);
                }
                return moduleId;
            }
            if (DEBUG) {
                VSlog.v(TAG, module + " is already registered, total module " + this.mModuleRegistered + " in " + this.mDisplayStr);
            }
            return -1;
        }
        return -1;
    }

    public int registerModule(String module) {
        if (checkPermission(module)) {
            int index = findModuleIndex(module);
            if (index < 0) {
                int moduleId = generateModuleId();
                Pack pack = new Pack(moduleId, module, 1 << this.mModuleRegistered);
                pack.setDisplayModule(null);
                pack.setRequestState(0);
                pack.setVisibleState(false);
                this.mPackList.add(pack);
                this.mModuleRegistered++;
                if (DEBUG) {
                    VSlog.d(TAG, module + " registered with module id " + moduleId + " in " + this.mDisplayStr + " total-registered " + this.mModuleRegistered);
                }
                return moduleId;
            }
            if (DEBUG) {
                VSlog.v(TAG, module + " is already registered, total module " + this.mModuleRegistered + " in " + this.mDisplayStr);
            }
            return -1;
        }
        return -1;
    }

    private boolean checkPermission(String module) {
        boolean support = false;
        int listSize = VivoDisplayModuleConfig.MODULE_LIST.size();
        int i = 0;
        while (true) {
            if (i >= listSize) {
                break;
            } else if (!VivoDisplayModuleConfig.MODULE_LIST.get(i).equals(module)) {
                i++;
            } else {
                support = true;
                break;
            }
        }
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append(module);
            sb.append(support ? " supported" : " unsupported");
            VSlog.d(TAG, sb.toString());
        }
        return support;
    }

    private int generateModuleId() {
        Random rand = new Random();
        int generatedId = rand.nextInt(Integer.MAX_VALUE);
        int numModules = this.mPackList.size();
        if (numModules < 1) {
            return generatedId;
        }
        int i = 0;
        do {
            if (this.mPackList.get(i).moduleId == generatedId) {
                generatedId = rand.nextInt(Integer.MAX_VALUE);
                continue;
            } else {
                i++;
                continue;
            }
        } while (i < numModules);
        return generatedId;
    }

    private int findModuleIndex(String module) {
        int numModules = this.mPackList.size();
        for (int i = 0; i < numModules; i++) {
            if (this.mPackList.get(i).typeStr.equals(module)) {
                return i;
            }
        }
        return -1;
    }

    private int findModuleIndex(int moduleId) {
        int numModules = this.mPackList.size();
        for (int i = 0; i < numModules; i++) {
            if (this.mPackList.get(i).moduleId == moduleId) {
                return i;
            }
        }
        return -1;
    }

    public boolean isModuleRegistered(String module) {
        int index = findModuleIndex(module);
        return index != -1;
    }

    public boolean isModuleRegistered(int moduleId) {
        int index = findModuleIndex(moduleId);
        return index != -1;
    }

    public String getModuleStr(int moduleId) {
        int index = findModuleIndex(moduleId);
        if (index != -1) {
            return this.mPackList.get(index).typeStr;
        }
        return "error";
    }

    public int getModuleId(String module) {
        int index = findModuleIndex(module);
        if (index == -1) {
            return -1;
        }
        return this.mPackList.get(index).moduleId;
    }

    private void updateStatePoll() {
        int numModules = this.mPackList.size();
        int tempStateDozePoll = 0;
        int tempStateOnPoll = 0;
        String globalPollMsg = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        for (int i = 0; i < numModules; i++) {
            globalPollMsg = globalPollMsg + this.mPackList.get(i).typeStr + ":" + Display.stateToString(this.mPackList.get(i).requestState) + " ";
            if (2 == this.mPackList.get(i).requestState) {
                tempStateOnPoll++;
            } else if (3 == this.mPackList.get(i).requestState) {
                tempStateDozePoll++;
            }
        }
        this.mStateOnPoll = tempStateOnPoll;
        this.mStateDozePoll = tempStateDozePoll;
        if (DEBUG) {
            VSlog.d(TAG, "updateStatePoll: " + this.mDisplayStr + " doze:(" + this.mStateDozePoll + ") on:(" + this.mStateOnPoll + ") total-module:(" + numModules + ") globalPollMsg:(" + globalPollMsg.trim() + ")");
        }
    }

    public void updateAllModuleRequestState(int requestState) {
        int numModules = this.mPackList.size();
        for (int i = 0; i < numModules; i++) {
            if (this.mPackList.get(i).requestState != requestState) {
                this.mPackList.get(i).setRequestState(requestState);
            }
        }
        updateStatePoll();
    }

    public void updateModuleRequestState(String module, int requestState) {
        int index = findModuleIndex(module);
        if (index == -1) {
            VSlog.w(TAG, "update module request state denied/unknown module in " + this.mDisplayStr);
            return;
        }
        int moduleRequestState = this.mPackList.get(index).requestState;
        if (moduleRequestState != requestState) {
            this.mPackList.get(index).setRequestState(requestState);
            updateStatePoll();
            return;
        }
        VSlog.d(TAG, module + " update request " + Display.stateToString(requestState) + " failed/reuqest state is not changed in " + this.mDisplayStr);
    }

    public int getModuleRequestState(String module) {
        int index = findModuleIndex(module);
        if (index == -1) {
            VSlog.w(TAG, "get module request state denied/unknown module in " + this.mDisplayStr);
            return 0;
        }
        return this.mPackList.get(index).requestState;
    }

    public boolean isStateDozePolled() {
        return this.mStateDozePoll > 0;
    }

    public boolean isStateOnPolled() {
        return this.mStateOnPoll > 0;
    }

    public ArrayList<String> getAllRegisteredModules() {
        ArrayList<String> modules = new ArrayList<>();
        int numModules = this.mPackList.size();
        for (int i = 0; i < numModules; i++) {
            modules.add(this.mPackList.get(i).typeStr);
        }
        return modules;
    }

    private void clearBit(long bit) {
        this.mDisplayContentVisible &= ~bit;
    }

    private void setBit(long bit) {
        this.mDisplayContentVisible |= bit;
    }

    public boolean displayContentVisible() {
        return this.mDisplayContentVisible != 0;
    }

    private long typeStrToBit(String module) {
        int index = findModuleIndex(module);
        if (index >= 0) {
            return this.mPackList.get(index).bit;
        }
        VSlog.w(TAG, module + " is not registered in " + this.mDisplayStr);
        return STATE_BIT_UNKNOWN;
    }

    public boolean displayModuleContentVisible(String module) {
        long bit = typeStrToBit(module);
        long moduleBit = this.mDisplayContentVisible & bit;
        return moduleBit == bit;
    }

    public void updateDisplayContentState(String module, boolean show) {
        synchronized (LOCK) {
            long bit = typeStrToBit(module);
            int index = findModuleIndex(module);
            long tempDisplayContentVisible = this.mDisplayContentVisible;
            if (STATE_BIT_UNKNOWN == bit) {
                VSlog.w(TAG, "updateDisplayContentState denied/invalid module in " + this.mDisplayStr);
            } else if (-1 == index) {
                VSlog.w(TAG, "updateDisplayContentState denied/unknown module in " + this.mDisplayStr);
            } else {
                updateVisibleModuleNum(index, show);
                if (show) {
                    setBit(bit);
                } else {
                    clearBit(bit);
                }
                if (tempDisplayContentVisible != this.mDisplayContentVisible) {
                    int numModules = this.mPackList.size();
                    String globalDisplayMsg = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                    for (int i = 0; i < numModules; i++) {
                        String moduleStr = this.mPackList.get(i).typeStr;
                        globalDisplayMsg = globalDisplayMsg + moduleStr + ":" + String.valueOf(displayModuleContentVisible(moduleStr)) + ":0x" + Long.toHexString(this.mPackList.get(i).bit) + " ";
                    }
                    VSlog.d(TAG, "updateDisplayContentState: " + this.mDisplayStr + " show: " + show + " type: " + bitToString(bit) + " bit: 0x" + Long.toHexString(bit) + " displayContentVisible: " + displayContentVisible() + " (0x" + Long.toHexString(tempDisplayContentVisible) + " to 0x" + Long.toHexString(this.mDisplayContentVisible) + ") globalDisplayMsg:(" + globalDisplayMsg.trim() + ")");
                }
            }
        }
    }

    private String bitToString(long bit) {
        int numModules = this.mPackList.size();
        for (int i = 0; i < numModules; i++) {
            if (this.mPackList.get(i).bit == bit) {
                return this.mPackList.get(i).typeStr;
            }
        }
        VSlog.w(TAG, "bitToString STATE_BIT_UNKNOWN bit " + bit);
        return "unknown(" + Long.toHexString(bit) + ")";
    }

    private void updateVisibleModuleNum(int index, boolean visible) {
        boolean moduleVisible = this.mPackList.get(index).visible;
        if (moduleVisible != visible) {
            this.mPackList.get(index).setVisibleState(visible);
            if (visible) {
                this.mModuleVisibleNum++;
            } else {
                this.mModuleVisibleNum--;
            }
        } else if (DEBUG) {
            VSlog.d(TAG, "module " + this.mPackList.get(index).typeStr + " visible state (" + moduleVisible + ") is not changed in " + this.mDisplayStr);
        }
    }

    public VivoDisplayModule getVivoDisplayModuleInfo(int moduleId) {
        int numModules = this.mPackList.size();
        Pack pack = null;
        int index = 0;
        while (true) {
            if (index >= numModules) {
                break;
            } else if (this.mPackList.get(index).moduleId != moduleId) {
                index++;
            } else {
                Pack pack2 = this.mPackList.get(index);
                pack = pack2;
                break;
            }
        }
        if (pack == null) {
            return null;
        }
        if (pack.displayModule != null) {
            return pack.displayModule;
        }
        VivoDisplayModule vivoDisplayModule = new VivoDisplayModule(this.mDisplayId, pack.moduleId, pack.typeStr);
        vivoDisplayModule.setVisibleState(pack.visible);
        return vivoDisplayModule;
    }

    public void enableDisableBlackOverlay(boolean enable) {
        VivoDisplayOverlayController vivoDisplayOverlayController = this.mDisplayOverlayController;
        if (vivoDisplayOverlayController != null) {
            vivoDisplayOverlayController.enableDisableBlackOverlay(enable);
        } else {
            VSlog.w(TAG, "enable disable black overlay denied/invalid vivo display overlay controller");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class Pack {
        public long bit;
        public VivoDisplayModule displayModule;
        public int moduleId;
        public int requestState;
        public String typeStr;
        public boolean visible;

        public Pack(int moduleId, String typeStr, long bit) {
            this.moduleId = moduleId;
            this.typeStr = typeStr;
            this.bit = bit;
        }

        public void setDisplayModule(VivoDisplayModule displayModule) {
            this.displayModule = displayModule;
        }

        public void setRequestState(int requestState) {
            this.requestState = requestState;
        }

        public void setVisibleState(boolean visible) {
            this.visible = visible;
            VivoDisplayModule vivoDisplayModule = this.displayModule;
            if (vivoDisplayModule != null) {
                vivoDisplayModule.setVisibleState(visible);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class VivoDisplayModuleConfig {
        public static final ArrayList<String> MODULE_LIST = new ArrayList<String>() { // from class: com.android.server.display.VivoDisplayModuleController.VivoDisplayModuleConfig.1
            {
                add(VivoDisplayModuleConfig.STR_MODULE_UDFINGERPRINT);
                add(VivoDisplayModuleConfig.STR_MODULE_NIGHTPEARL);
                add(VivoDisplayModuleConfig.STR_MODULE_FACEDETECT);
                add(VivoDisplayModuleConfig.STR_MODULE_CAMERA);
                add(VivoDisplayModuleConfig.STR_MODULE_LIGHT);
                add(VivoDisplayModuleConfig.STR_MODULE_SYSTEMUI_KEY);
                add(VivoDisplayModuleConfig.STR_MODULE_CAPACITY_KEY);
                add(VivoDisplayModuleConfig.STR_MODULE_GLOBAL_ANIMATION);
            }
        };
        public static final String STR_MODULE_CAMERA = "VivoCamera";
        public static final String STR_MODULE_CAPACITY_KEY = "capacity_key";
        public static final String STR_MODULE_FACEDETECT = "FaceDozeIcon";
        public static final String STR_MODULE_GLOBAL_ANIMATION = "global_animation";
        public static final String STR_MODULE_LIGHT = "light";
        public static final String STR_MODULE_NIGHTPEARL = "nightpearl";
        public static final String STR_MODULE_SYSTEMUI_KEY = "systemui_virtual_key";
        public static final String STR_MODULE_UDFINGERPRINT = "UDFingerprint";

        private VivoDisplayModuleConfig() {
        }
    }
}