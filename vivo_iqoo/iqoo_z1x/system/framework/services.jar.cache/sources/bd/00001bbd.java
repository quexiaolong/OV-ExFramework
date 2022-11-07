package com.android.server.wallpaper;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.SystemProperties;
import android.util.Log;
import com.android.server.IVivoRmsInjector;

/* loaded from: classes2.dex */
class GLHelper {
    private static final String TAG = GLHelper.class.getSimpleName();
    private static final int sMaxTextureSize;

    GLHelper() {
    }

    static {
        int maxTextureSize = SystemProperties.getInt("sys.max_texture_size", 0);
        sMaxTextureSize = maxTextureSize > 0 ? maxTextureSize : retrieveTextureSizeFromGL();
    }

    private static int retrieveTextureSizeFromGL() {
        try {
            EGLDisplay eglDisplay = EGL14.eglGetDisplay(0);
            if (eglDisplay != null && eglDisplay != EGL14.EGL_NO_DISPLAY) {
                if (!EGL14.eglInitialize(eglDisplay, null, 0, null, 1)) {
                    String err = "eglInitialize failed: " + GLUtils.getEGLErrorString(EGL14.eglGetError());
                    throw new RuntimeException(err);
                }
                EGLConfig eglConfig = null;
                int[] configsCount = new int[1];
                EGLConfig[] configs = new EGLConfig[1];
                int[] configSpec = {12352, 4, 12324, 8, 12323, 8, 12322, 8, 12321, 0, 12325, 0, 12326, 0, 12327, 12344, 12344};
                if (!EGL14.eglChooseConfig(eglDisplay, configSpec, 0, configs, 0, 1, configsCount, 0)) {
                    String err2 = "eglChooseConfig failed: " + GLUtils.getEGLErrorString(EGL14.eglGetError());
                    throw new RuntimeException(err2);
                }
                if (configsCount[0] > 0) {
                    eglConfig = configs[0];
                }
                if (eglConfig != null) {
                    int[] attr_list = {12440, 2, 12344};
                    EGLContext eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, attr_list, 0);
                    if (eglContext == null || eglContext == EGL14.EGL_NO_CONTEXT) {
                        String err3 = "eglCreateContext failed: " + GLUtils.getEGLErrorString(EGL14.eglGetError());
                        throw new RuntimeException(err3);
                    }
                    int[] attrs = {12375, 1, 12374, 1, 12344};
                    EGLSurface eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, eglConfig, attrs, 0);
                    EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext);
                    int[] maxSize = new int[1];
                    GLES20.glGetIntegerv(3379, maxSize, 0);
                    EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
                    EGL14.eglDestroySurface(eglDisplay, eglSurface);
                    EGL14.eglDestroyContext(eglDisplay, eglContext);
                    EGL14.eglTerminate(eglDisplay);
                    return maxSize[0];
                }
                throw new RuntimeException("eglConfig not initialized!");
            }
            String err4 = "eglGetDisplay failed: " + GLUtils.getEGLErrorString(EGL14.eglGetError());
            throw new RuntimeException(err4);
        } catch (RuntimeException e) {
            Log.w(TAG, "Retrieve from GL failed", e);
            return IVivoRmsInjector.QUIET_TYPE_ALL;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int getMaxTextureSize() {
        return sMaxTextureSize;
    }
}