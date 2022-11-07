package com.vivo.services.rms.display.scene;

import com.vivo.services.rms.display.GlobalConfigs;
import com.vivo.services.rms.display.RefreshRateRequest;
import com.vivo.services.rms.display.SceneManager;

/* loaded from: classes.dex */
public class InteractionScene extends BaseScene {
    public static final String INPUT_REASON = "touch";
    public static final int PRIORITY_FOR_TOUCH_MAX = 20;
    public static final int PRIORITY_FOR_TOUCH_MIN = 10;
    public static final int PRIORITY_FOR_USER_SETTING = 0;

    public InteractionScene(SceneManager mng) {
        super(mng, SceneManager.INTERACTION_SCENE, SceneManager.INTERACTION_PRIORITY);
    }

    @Override // com.vivo.services.rms.display.scene.BaseScene
    public boolean updateRequest(RefreshRateRequest request, int flags, int value) {
        int userSettingMinRefreshRate;
        if (isInputMax(request)) {
            request.reqFps = GlobalConfigs.clipFps(GlobalConfigs.getInteractionMaxRefreshRate());
            request.reqConfigBits = GlobalConfigs.getInteractionConfigBits();
        } else if (isUserSetting(request)) {
            request.reqConfigBits = GlobalConfigs.getInteractionConfigBits();
            if (GlobalConfigs.isIngoreInput() && GlobalConfigs.isUseMaxFpsWhenIgnoreInput()) {
                userSettingMinRefreshRate = GlobalConfigs.getInteractionMaxRefreshRate();
            } else {
                userSettingMinRefreshRate = GlobalConfigs.getUserSettingMinRefreshRate();
            }
            request.reqFps = GlobalConfigs.clipFps(userSettingMinRefreshRate);
        }
        return super.updateRequest(request, flags, value);
    }

    @Override // com.vivo.services.rms.display.scene.BaseScene
    public boolean isValid(RefreshRateRequest request) {
        if (super.isValid(request)) {
            return !isInput(request) || GlobalConfigs.isTouchValid();
        }
        return false;
    }

    private boolean isInput(RefreshRateRequest request) {
        return request.priority == this.mPriority + 20 || request.priority == this.mPriority + 10;
    }

    private boolean isInputMax(RefreshRateRequest request) {
        return request.priority == this.mPriority + 20;
    }

    private boolean isUserSetting(RefreshRateRequest request) {
        return request.priority == this.mPriority + 0;
    }
}