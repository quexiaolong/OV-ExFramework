package com.android.server.wallpaper;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import com.vivo.services.proxy.broadcast.BroadcastConfigs;
import vendor.pixelworks.hardware.display.V1_0.KernelConfig;

/* loaded from: classes.dex */
public class WallpaperColorToneJudger {
    static final boolean DEBUG = false;
    private static String VERSION = "1.0.0.3";
    public static int COLOR_TONE_UNKNOWN = -1;
    public static int COLOR_TONE_COLD = 0;
    public static int COLOR_TONE_WARM = 1;
    public static int COLOR_TONE_NEUTER = 2;
    private static int THRESHOLD_PIXEL_HUE_STRONGCOLD_LOWER_BOUND = KernelConfig.MEMC_OSD;
    private static int THRESHOLD_PIXEL_HUE_STRONGCOLD_UPER_BOUND = 225;
    private static int THRESHOLD_PIXEL_HUE_WEAKCOLD_LEFT_BOUND = 105;
    private static int THRESHOLD_PIXEL_HUE_WEAKCOLD_RIGHT_BOUND = 255;
    private static int THRESHOLD_PIXEL_HUE_STRONGWARM_LOWER_BOUND = 45;
    private static int THRESHOLD_PIXEL_HUE_STRONGWARM_UPER_BOUND = 315;
    private static int THRESHOLD_PIXEL_HUE_WEAKWARM_LEFT_BOUND = 75;
    private static int THRESHOLD_PIXEL_HUE_WEAKWARM_RIGHT_BOUND = 285;
    private static float THRESHOLD_PIXEL_SATURATION_STRONGCOLD_LOWER_BOUND = 0.2f;
    private static float THRESHOLD_PIXEL_SATURATION_STRONGCOLD_UPER_BOUND = 1.0f;
    private static float THRESHOLD_PIXEL_SATURATION_WEAKCOLD_LOWER_BOUND = 0.4f;
    private static float THRESHOLD_PIXEL_SATURATION_WEAKCOLD_UPER_BOUND = 1.0f;
    private static float THRESHOLD_PIXEL_SATURATION_STRONGWARM_LOWER_BOUND = 0.2f;
    private static float THRESHOLD_PIXEL_SATURATION_STRONGWARM_UPER_BOUND = 1.0f;
    private static float THRESHOLD_PIXEL_SATURATION_WEAKWARM_LOWER_BOUND = 0.2f;
    private static float THRESHOLD_PIXEL_SATURATION_WEAKWARM_UPER_BOUND = 1.0f;
    private static float THRESHOLD_PIXEL_BRIGHTNESS_STRONGCOLD_LOWER_BOUND = 0.0f;
    private static float THRESHOLD_PIXEL_BRIGHTNESS_STRONGCOLD_UPER_BOUND = 1.0f;
    private static float THRESHOLD_PIXEL_BRIGHTNESS_WEAKCOLD_LOWER_BOUND = 0.0f;
    private static float THRESHOLD_PIXEL_BRIGHTNESS_WEAKCOLD_UPER_BOUND = 0.6f;
    private static float THRESHOLD_PIXEL_BRIGHTNESS_STRONGWARM_LOWER_BOUND = 0.2f;
    private static float THRESHOLD_PIXEL_BRIGHTNESS_STRONGWARM_UPER_BOUND = 1.0f;
    private static float THRESHOLD_PIXEL_BRIGHTNESS_WEAKWARM_LOWER_BOUND = 0.2f;
    private static float THRESHOLD_PIXEL_BRIGHTNESS_WEAKWARM_UPER_BOUND = 0.8f;
    private static float THRESHOLD_PIXEL_LOW_SATURATION = 0.3f;
    private static float THRESHOLD_PIXEL_HIGH_SATURATION = 0.7f;
    private static double RESOLUTION_MAX = 153900.0d;
    private final String TAG = "WallpaperColorToneJudger";
    private int mTotalBlocksCount = 0;
    private int mTotalColdToneBlocksCount = 0;
    private int mTotalWarmToneBlocksCount = 0;
    private int mTotalNeuterToneBlocksCount = 0;
    private int mTotalUndefinedToneBlocksCount = 0;
    private int mLowSaturationBlocksCountColdTone = 0;
    private int mLowSaturationBlocksCountWarmTone = 0;
    private int mLowSaturationBlocksCountNeuterTone = 0;
    private int mHighSaturationBlocksCountColdTone = 0;
    private int mHighSaturationBlocksCountWarmTone = 0;
    private int mHighSaturationBlocksCountNeuterTone = 0;
    String mFileName = null;
    private int[] mPixels = null;

    public void judgeColorToneType(String fileName, Bitmap bitmap) {
        int hue;
        int width;
        if (bitmap != null && !bitmap.isRecycled()) {
            this.mFileName = fileName;
            this.mTotalColdToneBlocksCount = 0;
            this.mTotalWarmToneBlocksCount = 0;
            this.mTotalNeuterToneBlocksCount = 0;
            this.mTotalUndefinedToneBlocksCount = 0;
            this.mLowSaturationBlocksCountColdTone = 0;
            this.mLowSaturationBlocksCountWarmTone = 0;
            this.mLowSaturationBlocksCountNeuterTone = 0;
            this.mHighSaturationBlocksCountColdTone = 0;
            this.mHighSaturationBlocksCountWarmTone = 0;
            this.mHighSaturationBlocksCountNeuterTone = 0;
            int width2 = bitmap.getWidth();
            int height = bitmap.getHeight();
            float scaleFactor = 1.0f;
            for (double resolution = width2 * height; resolution > RESOLUTION_MAX; resolution /= 4.0d) {
                scaleFactor /= 2.0f;
            }
            Bitmap scaledBitmap = scaleBitmap(bitmap, scaleFactor, scaleFactor);
            int scaledWidth = scaledBitmap.getWidth();
            int scaledHeight = scaledBitmap.getHeight();
            int dataLen = scaledWidth * scaledHeight;
            this.mTotalBlocksCount = dataLen;
            int[] iArr = new int[dataLen];
            this.mPixels = iArr;
            int dataLen2 = dataLen;
            scaledBitmap.getPixels(iArr, 0, scaledWidth, 0, 0, scaledWidth, scaledHeight);
            int i = 0;
            while (i < dataLen2) {
                int color = this.mPixels[i];
                Color.alpha(color);
                int colorR = Color.red(color);
                int colorG = Color.green(color);
                int colorB = Color.blue(color);
                int minComponent = Math.min(Math.min(colorR, colorG), colorB);
                int maxComponent = Math.max(Math.max(colorR, colorG), colorB);
                int dataLen3 = dataLen2;
                if (maxComponent == minComponent) {
                    hue = -100;
                } else if (maxComponent == colorR && colorG >= colorB) {
                    int hue2 = ((colorG - colorB) * 60) / (maxComponent - minComponent);
                    hue = hue2;
                } else if (maxComponent == colorR && colorG < colorB) {
                    hue = (((colorG - colorB) * 60) / (maxComponent - minComponent)) + 360;
                } else if (maxComponent == colorG) {
                    int hue3 = (((colorB - colorR) * 60) / (maxComponent - minComponent)) + KernelConfig.MODE_SET;
                    hue = hue3;
                } else if (maxComponent != colorB) {
                    hue = 0;
                } else {
                    hue = (((colorR - colorG) * 60) / (maxComponent - minComponent)) + BroadcastConfigs.PROXY_BR_ABNORMAL_SIZE;
                }
                float saturation = 0.0f;
                if (maxComponent == 0) {
                    width = width2;
                } else {
                    width = width2;
                    saturation = (maxComponent - minComponent) / maxComponent;
                }
                float bright = maxComponent / 255.0f;
                if (hue >= THRESHOLD_PIXEL_HUE_STRONGCOLD_LOWER_BOUND && hue <= THRESHOLD_PIXEL_HUE_STRONGCOLD_UPER_BOUND && saturation >= THRESHOLD_PIXEL_SATURATION_STRONGWARM_LOWER_BOUND && saturation <= THRESHOLD_PIXEL_SATURATION_STRONGWARM_UPER_BOUND && bright >= THRESHOLD_PIXEL_BRIGHTNESS_STRONGCOLD_LOWER_BOUND && bright <= THRESHOLD_PIXEL_BRIGHTNESS_STRONGCOLD_UPER_BOUND) {
                    this.mTotalColdToneBlocksCount++;
                } else if (((hue >= THRESHOLD_PIXEL_HUE_WEAKCOLD_LEFT_BOUND && hue <= THRESHOLD_PIXEL_HUE_STRONGCOLD_LOWER_BOUND) || (hue >= THRESHOLD_PIXEL_HUE_STRONGCOLD_UPER_BOUND && hue <= THRESHOLD_PIXEL_HUE_WEAKCOLD_RIGHT_BOUND)) && saturation >= THRESHOLD_PIXEL_SATURATION_WEAKCOLD_LOWER_BOUND && saturation <= THRESHOLD_PIXEL_SATURATION_WEAKCOLD_UPER_BOUND && bright >= THRESHOLD_PIXEL_BRIGHTNESS_WEAKCOLD_LOWER_BOUND && bright <= THRESHOLD_PIXEL_BRIGHTNESS_WEAKCOLD_UPER_BOUND) {
                    this.mTotalColdToneBlocksCount++;
                } else if (((hue >= 0 && hue < THRESHOLD_PIXEL_HUE_STRONGWARM_LOWER_BOUND) || (hue > THRESHOLD_PIXEL_HUE_STRONGWARM_UPER_BOUND && hue <= 360)) && saturation >= THRESHOLD_PIXEL_SATURATION_STRONGWARM_LOWER_BOUND && saturation <= THRESHOLD_PIXEL_SATURATION_STRONGWARM_UPER_BOUND && bright >= THRESHOLD_PIXEL_BRIGHTNESS_STRONGWARM_LOWER_BOUND && bright <= THRESHOLD_PIXEL_BRIGHTNESS_STRONGWARM_UPER_BOUND) {
                    this.mTotalWarmToneBlocksCount++;
                } else if (((hue >= THRESHOLD_PIXEL_HUE_STRONGWARM_LOWER_BOUND && hue < THRESHOLD_PIXEL_HUE_WEAKWARM_LEFT_BOUND) || (hue > THRESHOLD_PIXEL_HUE_WEAKWARM_RIGHT_BOUND && hue <= THRESHOLD_PIXEL_HUE_STRONGWARM_UPER_BOUND)) && saturation >= THRESHOLD_PIXEL_SATURATION_WEAKWARM_LOWER_BOUND && saturation <= THRESHOLD_PIXEL_SATURATION_WEAKWARM_UPER_BOUND && bright >= THRESHOLD_PIXEL_BRIGHTNESS_WEAKWARM_LOWER_BOUND && bright <= THRESHOLD_PIXEL_BRIGHTNESS_WEAKWARM_UPER_BOUND) {
                    this.mTotalWarmToneBlocksCount++;
                } else if ((hue < THRESHOLD_PIXEL_HUE_WEAKWARM_LEFT_BOUND || hue >= THRESHOLD_PIXEL_HUE_WEAKCOLD_LEFT_BOUND) && (hue <= THRESHOLD_PIXEL_HUE_WEAKCOLD_RIGHT_BOUND || hue > THRESHOLD_PIXEL_HUE_WEAKWARM_RIGHT_BOUND)) {
                    this.mTotalUndefinedToneBlocksCount++;
                } else {
                    this.mTotalNeuterToneBlocksCount++;
                }
                if (hue >= THRESHOLD_PIXEL_HUE_WEAKCOLD_LEFT_BOUND && hue <= THRESHOLD_PIXEL_HUE_WEAKCOLD_RIGHT_BOUND) {
                    if (saturation <= THRESHOLD_PIXEL_LOW_SATURATION) {
                        this.mLowSaturationBlocksCountColdTone++;
                    } else if (saturation >= THRESHOLD_PIXEL_HIGH_SATURATION) {
                        this.mHighSaturationBlocksCountColdTone++;
                    }
                } else if ((hue >= 0 && hue <= THRESHOLD_PIXEL_HUE_WEAKWARM_LEFT_BOUND) || (hue >= THRESHOLD_PIXEL_HUE_WEAKWARM_RIGHT_BOUND && hue <= 360)) {
                    if (saturation <= THRESHOLD_PIXEL_LOW_SATURATION) {
                        this.mLowSaturationBlocksCountWarmTone++;
                    } else if (saturation >= THRESHOLD_PIXEL_HIGH_SATURATION) {
                        this.mHighSaturationBlocksCountWarmTone++;
                    }
                } else if (saturation <= THRESHOLD_PIXEL_LOW_SATURATION) {
                    this.mLowSaturationBlocksCountNeuterTone++;
                } else if (saturation >= THRESHOLD_PIXEL_HIGH_SATURATION) {
                    this.mHighSaturationBlocksCountNeuterTone++;
                }
                i++;
                dataLen2 = dataLen3;
                width2 = width;
            }
        }
    }

    private Bitmap scaleBitmap(Bitmap bitmap, float widthScale, float heightScale) {
        if (bitmap == null || bitmap.isRecycled()) {
            return null;
        }
        Matrix matrix = new Matrix();
        matrix.postScale(widthScale, heightScale);
        Bitmap resizeBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return resizeBmp;
    }

    public int getColdBlocksCount() {
        return this.mTotalColdToneBlocksCount;
    }

    public int getWarmBlocksCount() {
        return this.mTotalWarmToneBlocksCount;
    }

    public int getNeuterBlocksCount() {
        return this.mTotalNeuterToneBlocksCount;
    }

    public int getUndefinedBlocksCount() {
        return this.mTotalUndefinedToneBlocksCount;
    }

    public int getTotalBlocksCount() {
        return this.mTotalBlocksCount;
    }

    public int getLowSaturationBlocksCountColdTone() {
        return this.mLowSaturationBlocksCountColdTone;
    }

    public int getLowSaturationBlocksCountWarmTone() {
        return this.mLowSaturationBlocksCountWarmTone;
    }

    public int getLowSaturationBlocksCountNeuterTone() {
        return this.mLowSaturationBlocksCountNeuterTone;
    }

    public int getHighSaturationBlocksCountColdTone() {
        return this.mHighSaturationBlocksCountColdTone;
    }

    public int getHighSaturationBlocksCountWarmTone() {
        return this.mHighSaturationBlocksCountWarmTone;
    }

    public int getHighSaturationBlocksCountNeuterTone() {
        return this.mHighSaturationBlocksCountNeuterTone;
    }

    private boolean isColdTone() {
        int i;
        int i2 = this.mTotalColdToneBlocksCount;
        if (i2 <= 0 || (i = this.mTotalBlocksCount) <= 0 || i2 <= this.mTotalWarmToneBlocksCount || i2 <= this.mTotalNeuterToneBlocksCount) {
            return false;
        }
        int i3 = this.mLowSaturationBlocksCountColdTone;
        int i4 = this.mLowSaturationBlocksCountNeuterTone;
        if ((i3 + i4) / i >= 0.3f || ((i3 + this.mLowSaturationBlocksCountWarmTone) + i4) / i >= 0.65f) {
            int i5 = this.mTotalBlocksCount;
            if ((this.mTotalWarmToneBlocksCount / i5 >= 0.007f && this.mTotalNeuterToneBlocksCount / i5 >= 0.015f) || this.mHighSaturationBlocksCountColdTone / this.mTotalBlocksCount <= 0.3f) {
                return false;
            }
        }
        int i6 = this.mTotalWarmToneBlocksCount;
        if (i6 == 0) {
            if (this.mTotalColdToneBlocksCount / this.mTotalBlocksCount >= 0.1f) {
                return true;
            }
        } else {
            int i7 = this.mTotalColdToneBlocksCount;
            if (i7 > i6) {
                if (i7 / i6 >= 5.0f && i7 / this.mTotalBlocksCount >= 0.4f) {
                    return true;
                }
                int i8 = this.mTotalWarmToneBlocksCount;
                int i9 = this.mTotalNeuterToneBlocksCount;
                int i10 = this.mTotalBlocksCount;
                if ((i8 + i9) / i10 <= 0.1f && this.mTotalColdToneBlocksCount / (i8 + i9) >= 3.0f && this.mTotalUndefinedToneBlocksCount / i10 <= 0.98f) {
                    return true;
                }
            }
        }
        if (this.mTotalColdToneBlocksCount / this.mTotalBlocksCount < 0.67f) {
            return false;
        }
        return true;
    }

    private boolean isWarmTone() {
        int i;
        int i2;
        int i3 = this.mTotalWarmToneBlocksCount;
        if (i3 <= 0 || (i = this.mTotalBlocksCount) <= 0 || i3 <= (i2 = this.mTotalColdToneBlocksCount) || i3 <= this.mTotalNeuterToneBlocksCount) {
            return false;
        }
        int i4 = this.mLowSaturationBlocksCountWarmTone;
        int i5 = this.mLowSaturationBlocksCountNeuterTone;
        if ((i4 + i5) / i >= 0.3f || ((this.mLowSaturationBlocksCountColdTone + i4) + i5) / i >= 0.65f) {
            return false;
        }
        if (i2 == 0) {
            if (i3 / i >= 0.1f) {
                return true;
            }
        } else if (i3 > i2) {
            if (i3 / i2 >= 5.0f && i3 / i >= 0.4f) {
                return true;
            }
            int i6 = this.mTotalColdToneBlocksCount;
            int i7 = this.mTotalNeuterToneBlocksCount;
            int i8 = this.mTotalBlocksCount;
            if ((i6 + i7) / i8 <= 0.1f && this.mTotalWarmToneBlocksCount / (i6 + i7) >= 3.0f && this.mTotalUndefinedToneBlocksCount / i8 <= 0.98f) {
                return true;
            }
        }
        if (this.mTotalWarmToneBlocksCount / this.mTotalBlocksCount < 0.67f) {
            return false;
        }
        return true;
    }

    public int getColorToneType() {
        int i = COLOR_TONE_UNKNOWN;
        if (isColdTone()) {
            int colorTone = COLOR_TONE_COLD;
            return colorTone;
        } else if (isWarmTone()) {
            int colorTone2 = COLOR_TONE_WARM;
            return colorTone2;
        } else {
            int colorTone3 = COLOR_TONE_NEUTER;
            return colorTone3;
        }
    }

    public void onPause() {
    }

    public void release() {
    }
}