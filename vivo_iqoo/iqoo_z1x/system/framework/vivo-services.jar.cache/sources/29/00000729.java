package com.vivo.services.rms.display.scene;

import com.vivo.services.rms.display.GlobalConfigs;
import com.vivo.services.rms.display.RefreshRateAdjuster;
import com.vivo.services.rms.display.RefreshRateRequest;
import com.vivo.services.rms.display.SceneManager;

/* loaded from: classes.dex */
public class AnimationScene extends BaseScene {
    private static final int ANIMATION_DURATION = 180000;
    public static final int ANIMATION_MAX_PRIORITY = 50;
    public static final String SCREEN_ROTATION_ANIMATION_NAME = "ScreenRotationAnimation";

    public AnimationScene(SceneManager mng) {
        super(mng, SceneManager.ANIMATION_SCENE, SceneManager.ANIMATION_PRIORITY);
    }

    @Override // com.vivo.services.rms.display.scene.BaseScene
    protected int getRefreshRate(RefreshRateRequest request) {
        RefreshRateRequest activeRequest;
        if (request.reqFps > 0) {
            return request.reqFps;
        }
        int fps = GlobalConfigs.getAnimationRefreshRate();
        if (request.priority == this.mPriority && (activeRequest = RefreshRateAdjuster.getInstance().getHighestPriorityRequest()) != null) {
            if (SCREEN_ROTATION_ANIMATION_NAME.equals(request.reason) || (activeRequest.priority >= 80000 && !GlobalConfigs.isAnimationHighRate())) {
                return Math.max(activeRequest.mode.fps, 60);
            }
            return fps;
        }
        return fps;
    }

    private boolean isAnimation(RefreshRateRequest request) {
        return request.priority <= this.mPriority + 50;
    }

    @Override // com.vivo.services.rms.display.scene.BaseScene
    protected int getDuration(RefreshRateRequest request, int duration) {
        if (isAnimation(request)) {
            return duration > 0 ? duration : ANIMATION_DURATION;
        }
        return duration;
    }
}