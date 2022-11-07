package com.vivo.services.rms.display.scene;

import com.vivo.services.rms.display.GlobalConfigs;
import com.vivo.services.rms.display.RefreshRateRequest;
import com.vivo.services.rms.display.SceneManager;

/* loaded from: classes.dex */
public class AppRequestScene extends BaseScene {
    public static final int PRIORITY_APP_REQUEST_FOR_BENCH_MARK = 5;
    public static final int PRIORITY_DEFAULT = 0;
    public static final int PRIORITY_FOR_APP_SET_FRAMERATE = 20;
    public static final int PRIORITY_FOR_GAME_SDK = 40;
    public static final int PRIORITY_FOR_VIDEO_PLAY = 30;
    public static final int PRIORITY_FOR_WINDOW_PREFERRED = 10;

    public AppRequestScene(SceneManager mng) {
        super(mng, SceneManager.APP_REQUEST_SCENE, SceneManager.APP_REQUEST_PRIORITY);
    }

    @Override // com.vivo.services.rms.display.scene.BaseScene
    protected int getRefreshRate(RefreshRateRequest request) {
        return request.priority <= this.mPriority + 5 ? request.reqFps : GlobalConfigs.getAppRequestRefreshRate(request.reqFps);
    }
}