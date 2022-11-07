package com.vivo.services.rms.display.scene;

import com.vivo.services.rms.display.SceneManager;

/* loaded from: classes.dex */
public class PowerScene extends BaseScene {
    public static final int PRIORITY_FOR_THERMAL = 50;

    public PowerScene(SceneManager mng) {
        super(mng, SceneManager.POWER_SCENE, SceneManager.POWER_PRIORITY, true);
    }

    public static int thermalPriority() {
        return 90050;
    }
}