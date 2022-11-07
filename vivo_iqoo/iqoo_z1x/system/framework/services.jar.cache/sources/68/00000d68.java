package com.android.server.display;

import java.io.PrintWriter;
import java.util.Arrays;

/* loaded from: classes.dex */
public class HysteresisLevels {
    private static final boolean DEBUG = false;
    private static final float DEFAULT_BRIGHTENING_HYSTERESIS = 0.1f;
    private static final float DEFAULT_DARKENING_HYSTERESIS = 0.2f;
    private static final String TAG = "HysteresisLevels";
    private final float[] mBrighteningThresholds;
    private final float[] mDarkeningThresholds;
    private final float[] mThresholdLevels;

    /* JADX INFO: Access modifiers changed from: package-private */
    public HysteresisLevels(int[] brighteningThresholds, int[] darkeningThresholds, int[] thresholdLevels) {
        if (brighteningThresholds.length != darkeningThresholds.length || darkeningThresholds.length != thresholdLevels.length + 1) {
            throw new IllegalArgumentException("Mismatch between hysteresis array lengths.");
        }
        this.mBrighteningThresholds = setArrayFormat(brighteningThresholds, 1000.0f);
        this.mDarkeningThresholds = setArrayFormat(darkeningThresholds, 1000.0f);
        this.mThresholdLevels = setArrayFormat(thresholdLevels, 1.0f);
    }

    public float getBrighteningThreshold(float value) {
        float brightConstant = getReferenceLevel(value, this.mBrighteningThresholds);
        float brightThreshold = (1.0f + brightConstant) * value;
        return brightThreshold;
    }

    public float getDarkeningThreshold(float value) {
        float darkConstant = getReferenceLevel(value, this.mDarkeningThresholds);
        float darkThreshold = (1.0f - darkConstant) * value;
        return darkThreshold;
    }

    private float getReferenceLevel(float value, float[] referenceLevels) {
        int index = 0;
        while (true) {
            float[] fArr = this.mThresholdLevels;
            if (fArr.length <= index || value < fArr[index]) {
                break;
            }
            index++;
        }
        return referenceLevels[index];
    }

    private float[] setArrayFormat(int[] configArray, float divideFactor) {
        float[] levelArray = new float[configArray.length];
        for (int index = 0; levelArray.length > index; index++) {
            levelArray[index] = configArray[index] / divideFactor;
        }
        return levelArray;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw) {
        pw.println(TAG);
        pw.println("  mBrighteningThresholds=" + Arrays.toString(this.mBrighteningThresholds));
        pw.println("  mDarkeningThresholds=" + Arrays.toString(this.mDarkeningThresholds));
        pw.println("  mThresholdLevels=" + Arrays.toString(this.mThresholdLevels));
    }
}