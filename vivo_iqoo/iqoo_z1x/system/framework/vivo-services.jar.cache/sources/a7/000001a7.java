package com.android.server.display;

import android.content.Context;
import android.multidisplay.MultiDisplayManager;
import android.opengl.GLES20;
import android.os.BatteryManagerInternal;
import android.os.FtBuild;
import android.util.FtFeature;
import android.view.animation.PathInterpolator;
import com.android.server.LocalServices;
import com.android.server.wm.WindowManagerInternal;

/* loaded from: classes.dex */
public class VivoColorFadeImpl implements IVivoColorFade {
    private static final String TAG = "VivoColorFadeImpl";
    public static final boolean USE_SYSTEMUI_FADE;
    private int mAlphaLocation;
    private BatteryManagerInternal mBatteryManagerInternal;
    private ColorFade mColorFade;
    private int mDiffusionLocation;
    private int mFocusPosLocation1;
    private int mFocusPosLocation2;
    private boolean mIsPowered;
    private boolean mKeyguardHide;
    private boolean mKeyguardOccluded;
    private int mOvalSizeLocation;
    private int mProgressLoc;
    private int mRadiusLoc;
    private boolean mSupportRtblur;
    private WindowManagerInternal mWindowManagerInternal;
    private int rot;
    private PathInterpolator mInterpolatorScaleOn_h = new PathInterpolator(0.17f, 0.17f, 0.67f, 1.0f);
    private PathInterpolator mInterpolatorAlphaOn_h = new PathInterpolator(0.33f, 0.0f, 0.67f, 1.0f);
    private PathInterpolator mInterpolatorScaleOff_h = new PathInterpolator(0.48f, 0.15f, 0.67f, 1.0f);
    private PathInterpolator mInterpolatorAlphaOff_h = new PathInterpolator(0.17f, 0.17f, 0.36f, 1.0f);
    private PathInterpolator mInterpolatorScaleOn_v = new PathInterpolator(0.17f, 0.17f, 0.67f, 1.0f);
    private PathInterpolator mInterpolatorAlphaOn_v = new PathInterpolator(0.33f, 0.0f, 0.6f, 1.0f);
    private PathInterpolator mInterpolatorScaleOff_v = new PathInterpolator(0.33f, 0.0f, 0.67f, 1.0f);
    private PathInterpolator mInterpolatorAlphaOff_v = new PathInterpolator(0.25f, 0.1f, 0.25f, 1.0f);
    private int mColorFadeStyle = -1;
    private boolean mIsScreenOnAnimation = true;
    private boolean mOffBecauseOfWakeKey = false;
    private boolean mIsDynamicEffectsOn = true;

    public VivoColorFadeImpl(ColorFade colorFade) {
        this.mColorFade = colorFade;
        float[] texMatrix = {1.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f};
        colorFade.mTexMatrix = texMatrix;
        this.mSupportRtblur = FtFeature.isFeatureSupport("vivo.software.rtblur");
        this.mBatteryManagerInternal = (BatteryManagerInternal) LocalServices.getService(BatteryManagerInternal.class);
    }

    static {
        USE_SYSTEMUI_FADE = FtBuild.getRomVersion() >= 12.0f;
    }

    public void setColorFadeStyle(int colorFadeStyle) {
        this.mColorFadeStyle = colorFadeStyle;
    }

    public void setIsScreenOnAnimation(boolean isScreenOnAnimation) {
        this.mIsScreenOnAnimation = isScreenOnAnimation;
    }

    public void setOffReason(int offReason) {
        if (MultiDisplayManager.isMultiDisplay) {
            if (offReason == 4) {
                this.mOffBecauseOfWakeKey = true;
                this.mColorFade.mDisplayId = 0;
            } else if (offReason == 102) {
                this.mOffBecauseOfWakeKey = true;
                this.mColorFade.mDisplayId = 4096;
            } else {
                this.mOffBecauseOfWakeKey = false;
            }
        } else if (offReason == 4 || offReason == 2 || offReason == 0) {
            this.mOffBecauseOfWakeKey = true;
        } else {
            this.mOffBecauseOfWakeKey = false;
        }
    }

    public void setRot(int rotation) {
        BatteryManagerInternal batteryManagerInternal = this.mBatteryManagerInternal;
        if (batteryManagerInternal != null) {
            this.mIsPowered = batteryManagerInternal.isPowered(7);
        }
        this.rot = rotation;
    }

    public void drawThreeFrames(int DEJANK_FRAMES) {
        if (this.mOffBecauseOfWakeKey) {
            for (int i = 0; i < DEJANK_FRAMES; i++) {
                this.mColorFade.draw(1.0f);
            }
            return;
        }
        for (int i2 = 0; i2 < DEJANK_FRAMES; i2++) {
            this.mColorFade.draw(0.0f);
        }
    }

    public int initGLfShader(Context context) {
        int i = this.mColorFadeStyle;
        if (i == -1) {
            int fshader = this.mColorFade.loadShader(context, 17825796, 35632);
            return fshader;
        }
        switch (i) {
            case VivoDisplayPowerControllerImpl.COLOR_FADE_ANIMATION_GLOBAL /* 4000 */:
                if (!USE_SYSTEMUI_FADE || this.mKeyguardHide || this.mKeyguardOccluded || ((!this.mSupportRtblur || !this.mIsDynamicEffectsOn) && this.mIsPowered)) {
                    int fshader2 = this.mColorFade.loadShader(context, 17825794, 35632);
                    return fshader2;
                }
                int fshader3 = this.mColorFade.loadShader(context, 17825796, 35632);
                return fshader3;
            case 4001:
                int fshader4 = this.mColorFade.loadShader(context, 17825795, 35632);
                return fshader4;
            case VivoDisplayPowerControllerImpl.COLOR_FADE_ANIMATION_VERTICAL /* 4002 */:
                int fshader5 = this.mColorFade.loadShader(context, 17825797, 35632);
                return fshader5;
            default:
                int fshader6 = this.mColorFade.loadShader(context, 17825794, 35632);
                return fshader6;
        }
    }

    public void initGLShadersParms(int program) {
        int i = this.mColorFadeStyle;
        if (i == 4001 || i == 4002) {
            this.mRadiusLoc = GLES20.glGetUniformLocation(program, "radius");
            this.mProgressLoc = GLES20.glGetUniformLocation(program, "progress");
            this.mFocusPosLocation1 = GLES20.glGetUniformLocation(program, "focusPoint1");
            this.mFocusPosLocation2 = GLES20.glGetUniformLocation(program, "focusPoint2");
            this.mOvalSizeLocation = GLES20.glGetUniformLocation(program, "ovalSize");
            this.mDiffusionLocation = GLES20.glGetUniformLocation(program, "diffusionRate");
            this.mAlphaLocation = GLES20.glGetUniformLocation(program, "alpha");
        } else if (!USE_SYSTEMUI_FADE || this.mKeyguardHide || this.mKeyguardOccluded || ((!this.mSupportRtblur || !this.mIsDynamicEffectsOn) && this.mIsPowered)) {
            this.mColorFade.mOpacityLoc = GLES20.glGetUniformLocation(program, "opacity");
        } else {
            this.mProgressLoc = GLES20.glGetUniformLocation(program, "level");
        }
        GLES20.glUseProgram(program);
    }

    public void initGLBuffers() {
    }

    public void dismissResources() {
    }

    public void draw(float level) {
        int i = this.mColorFadeStyle;
        if (i == 4001) {
            float c = (float) (Math.sqrt(1150000.0d) / 2.0d);
            float focusY1 = (1200.0f - c) / 2400.0f;
            float focusY2 = (1200.0f + c) / 2400.0f;
            if (this.mIsScreenOnAnimation) {
                float scaleProgress = this.mInterpolatorScaleOn_h.getInterpolation(level);
                float alphaProgress = this.mInterpolatorAlphaOn_h.getInterpolation(level);
                float alpha = alphaProgress * 1.0f;
                float radius = (4.0f * level * scaleProgress * 2.7f) + 0.01f;
                drawFaded(radius, level, 1.4166666f, focusY1, 1.4166666f, focusY2, 900.0f, 1400.0f, alpha, 0.667f);
                return;
            }
            float scaleProgress2 = this.mInterpolatorScaleOff_h.getInterpolation(1.0f - level);
            float alphaProgress2 = this.mInterpolatorAlphaOff_h.getInterpolation(1.0f - level);
            float alpha2 = ((-1.0f) * alphaProgress2) + 1.0f;
            float radius2 = (4.0f * level * (((-2.7f) * scaleProgress2) + 2.7f)) + 0.01f;
            drawFaded(radius2, level, 1.4166666f, focusY1, 1.4166666f, focusY2, 900.0f, 1400.0f, alpha2, 0.667f);
        } else if (i == 4002) {
            float c2 = (float) (Math.sqrt(90000.0d) / 2.0d);
            if (this.mIsScreenOnAnimation) {
                float scaleProgress3 = this.mInterpolatorScaleOn_v.getInterpolation(level);
                float alphaProgress3 = this.mInterpolatorAlphaOn_v.getInterpolation(level);
                float scale = scaleProgress3 * 7.0f;
                float focusY12 = (((250.0f - c2) * scale) - 500.0f) / 2400.0f;
                float focusY22 = (((250.0f + c2) * scale) - 500.0f) / 2400.0f;
                float ovalSizeX = scale * 400.0f;
                float ovalSizeY = scale * 800.0f;
                float alpha3 = alphaProgress3 * 1.0f;
                float radius3 = scale + 0.01f;
                drawFaded(radius3, level, 0.5f, focusY12, 0.5f, focusY22, ovalSizeX, ovalSizeY, alpha3, 0.83f);
                return;
            }
            float scaleProgress4 = this.mInterpolatorScaleOff_v.getInterpolation(1.0f - level);
            float alphaProgress4 = this.mInterpolatorAlphaOff_v.getInterpolation(1.0f - level);
            float scale2 = ((-7.0f) * scaleProgress4) + 7.0f;
            float focusY13 = (((250.0f - c2) * scale2) - 500.0f) / 2400.0f;
            float focusY23 = (((250.0f + c2) * scale2) - 500.0f) / 2400.0f;
            float ovalSizeX2 = scale2 * 400.0f;
            float ovalSizeY2 = scale2 * 800.0f;
            float alpha4 = ((-1.0f) * alphaProgress4) + 1.0f;
            float radius4 = scale2 + 0.01f;
            drawFaded(radius4, level, 0.5f, focusY13, 0.5f, focusY23, ovalSizeX2, ovalSizeY2, alpha4, 0.83f);
        } else if (!USE_SYSTEMUI_FADE || this.mKeyguardHide || this.mKeyguardOccluded || ((!this.mSupportRtblur || !this.mIsDynamicEffectsOn) && this.mIsPowered)) {
            double one_minus_level = 1.0f - level;
            double cos = Math.cos(3.141592653589793d * one_minus_level);
            double sign = cos < 0.0d ? -1.0d : 1.0d;
            float opacity = ((float) (-Math.pow(one_minus_level, 2.0d))) + 1.0f;
            float gamma = (float) ((((sign * 0.5d * Math.pow(cos, 2.0d)) + 0.5d) * 0.9d) + 0.1d);
            this.mColorFade.drawFaded(opacity, 1.0f / gamma);
        } else {
            drawFaded(level);
        }
    }

    private void drawFaded(float radius, float progress, float focusX1, float focusY1, float focusX2, float focusY2, float ovalSizeX, float ovalSizeY, float alpha, float diffusionRate) {
        GLES20.glUseProgram(this.mColorFade.mProgram);
        GLES20.glUniformMatrix4fv(this.mColorFade.mProjMatrixLoc, 1, false, this.mColorFade.mProjMatrix, 0);
        GLES20.glUniformMatrix4fv(this.mColorFade.mTexMatrixLoc, 1, false, this.mColorFade.mTexMatrix, 0);
        GLES20.glUniform1f(this.mRadiusLoc, radius);
        GLES20.glUniform1f(this.mProgressLoc, progress);
        GLES20.glUniform1f(this.mAlphaLocation, alpha);
        GLES20.glUniform1f(this.mDiffusionLocation, diffusionRate);
        GLES20.glUniform2f(this.mFocusPosLocation1, focusX1, focusY1);
        GLES20.glUniform2f(this.mFocusPosLocation2, focusX2, focusY2);
        GLES20.glUniform2f(this.mOvalSizeLocation, ovalSizeX, ovalSizeY);
        GLES20.glBindBuffer(34962, this.mColorFade.mGLBuffers[0]);
        GLES20.glEnableVertexAttribArray(this.mColorFade.mVertexLoc);
        GLES20.glVertexAttribPointer(this.mColorFade.mVertexLoc, 2, 5126, false, 0, 0);
        GLES20.glBindBuffer(34962, this.mColorFade.mGLBuffers[1]);
        GLES20.glEnableVertexAttribArray(this.mColorFade.mTexCoordLoc);
        GLES20.glVertexAttribPointer(this.mColorFade.mTexCoordLoc, 2, 5126, false, 0, 0);
        GLES20.glDrawArrays(6, 0, 4);
        GLES20.glBindBuffer(34962, 0);
    }

    private void drawFaded(float level) {
        GLES20.glUseProgram(this.mColorFade.mProgram);
        GLES20.glUniformMatrix4fv(this.mColorFade.mProjMatrixLoc, 1, false, this.mColorFade.mProjMatrix, 0);
        GLES20.glUniformMatrix4fv(this.mColorFade.mTexMatrixLoc, 1, false, this.mColorFade.mTexMatrix, 0);
        GLES20.glUniform1f(this.mProgressLoc, level);
        GLES20.glBindBuffer(34962, this.mColorFade.mGLBuffers[0]);
        GLES20.glEnableVertexAttribArray(this.mColorFade.mVertexLoc);
        GLES20.glVertexAttribPointer(this.mColorFade.mVertexLoc, 2, 5126, false, 0, 0);
        GLES20.glBindBuffer(34962, this.mColorFade.mGLBuffers[1]);
        GLES20.glEnableVertexAttribArray(this.mColorFade.mTexCoordLoc);
        GLES20.glVertexAttribPointer(this.mColorFade.mTexCoordLoc, 2, 5126, false, 0, 0);
        GLES20.glDrawArrays(6, 0, 4);
        GLES20.glBindBuffer(34962, 0);
    }

    public void drawFadedMod1() {
    }

    public void drawFadedMod2() {
    }

    public void captureScreenshotTextureAndSetViewportMod() {
    }

    public void destroyScreenshotTextureMod() {
    }

    public void setWakeReason(String reason) {
        if (reason.equals("WakeKey")) {
            this.mColorFade.mDisplayId = 0;
        } else if (reason.equals("AiKey")) {
            this.mColorFade.mDisplayId = 4096;
        }
    }

    public void setOccluded(boolean occluded) {
        if (this.mKeyguardOccluded != occluded) {
            this.mKeyguardOccluded = occluded;
        }
    }

    public void onKeyguardLockChanged() {
        if (this.mWindowManagerInternal == null) {
            this.mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        }
        WindowManagerInternal windowManagerInternal = this.mWindowManagerInternal;
        if (windowManagerInternal != null && !windowManagerInternal.isKeyguardShowingAndNotOccluded()) {
            this.mKeyguardHide = true;
        }
    }

    public void setKeyguardHide(boolean keyguardHide) {
        if (this.mKeyguardHide != keyguardHide) {
            this.mKeyguardHide = keyguardHide;
        }
    }

    public void setDynamicEffectsOn(boolean dynamicEffectsOn) {
        if (this.mIsDynamicEffectsOn != dynamicEffectsOn) {
            this.mIsDynamicEffectsOn = dynamicEffectsOn;
        }
    }

    public void dummy() {
    }
}