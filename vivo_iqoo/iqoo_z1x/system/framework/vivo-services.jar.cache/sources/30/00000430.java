package com.android.server.policy;

import android.content.Context;
import android.util.Log;
import android.view.IWindowManager;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.policy.key.VivoAIKeyHandler;
import com.android.server.policy.key.VivoCameraDoubleClickKeyHandler;
import com.android.server.policy.key.VivoCameraKeyHandler;
import com.android.server.policy.key.VivoCustomKeyHandler;
import com.android.server.policy.key.VivoGamepadKeyHandler;
import com.android.server.policy.key.VivoIMusicCollectKeyHandler;
import com.android.server.policy.key.VivoOTGKeyHandler;
import com.android.server.policy.key.VivoPowerKeyHandler;
import com.android.server.policy.key.VivoScreenClockKeyHandler;
import com.android.server.policy.key.VivoSmartwakeKeyHandler;
import com.android.server.policy.key.VivoTaskKeyHandler;
import com.android.server.policy.key.VivoVolumeKeyHandler;
import com.android.server.policy.motion.VivoMSPointerEventListener;

/* loaded from: classes.dex */
public final class VivoWMPHookCreator {
    private static VivoWMPHook mInstance = null;
    private static IVivoKeyFallbackListener mKeyFallbackListener = null;

    public static VivoWMPHook createInstance(Context context, IWindowManager windowManager, WindowManagerPolicy.WindowManagerFuncs windowManagerFuncs, IVivoAdjustmentPolicy vivoPolicy) {
        VivoWMPHook vivoWMPHook = mInstance;
        if (vivoWMPHook != null) {
            return vivoWMPHook;
        }
        VivoWMPHook vivoWMPHook2 = new VivoWMPHook(context, windowManager, windowManagerFuncs, vivoPolicy);
        mInstance = vivoWMPHook2;
        return vivoWMPHook2;
    }

    public static VivoWMPHook peekInstance() {
        return mInstance;
    }

    public static void createPointerEventListener(VivoWMPHook vivoWMPHook) {
        VivoMSPointerEventListener msListener = new VivoMSPointerEventListener(vivoWMPHook.mContext);
        vivoWMPHook.mWindowManagerFuncs.registerPointerEventListener(msListener, 0);
    }

    public static void createInterceptKeyHandler(VivoWMPHook vivoWMPHook) {
        VivoInterceptKeyRegister interceptKeyHandler = new VivoInterceptKeyRegister(vivoWMPHook.mContext);
        Log.d("VivoWMPHookCreator", "register taskkey.");
        VivoTaskKeyHandler taskKeyHandler = new VivoTaskKeyHandler(vivoWMPHook.mContext, vivoWMPHook.mVivoPolicy);
        interceptKeyHandler.registerInterceptKeyListener(82, taskKeyHandler);
        VivoPowerKeyHandler powerKeyHandler = new VivoPowerKeyHandler(vivoWMPHook.mContext, vivoWMPHook.mVivoPolicy);
        interceptKeyHandler.registerInterceptKeyListener(26, powerKeyHandler);
        VivoCameraKeyHandler cameraKeyHandler = new VivoCameraKeyHandler(vivoWMPHook.mContext);
        interceptKeyHandler.registerInterceptKeyListener(27, cameraKeyHandler);
        VivoVolumeKeyHandler multiMediaKeyHandler = new VivoVolumeKeyHandler(vivoWMPHook.mContext, vivoWMPHook.mVivoPolicy);
        interceptKeyHandler.registerInterceptKeyListener(25, multiMediaKeyHandler);
        interceptKeyHandler.registerInterceptKeyListener(24, multiMediaKeyHandler);
        VivoOTGKeyHandler otgKeyHandler = new VivoOTGKeyHandler(vivoWMPHook.mContext, vivoWMPHook.mVivoPolicy);
        interceptKeyHandler.registerInterceptKeyListener(117, otgKeyHandler);
        interceptKeyHandler.registerInterceptKeyListener(118, otgKeyHandler);
        VivoCustomKeyHandler customKeyHandler = new VivoCustomKeyHandler(vivoWMPHook.mContext, vivoWMPHook.mVivoPolicy);
        interceptKeyHandler.registerInterceptKeyListener(VivoPolicyConstant.KEYCODE_TS_LARGE_SUPPRESSION, customKeyHandler);
        VivoSmartwakeKeyHandler smartwakeKeyHandler = new VivoSmartwakeKeyHandler(vivoWMPHook.mContext, vivoWMPHook.mVivoPolicy);
        interceptKeyHandler.registerInterceptKeyListener(VivoPolicyConstant.KEYCODE_WAKEUP, smartwakeKeyHandler);
        interceptKeyHandler.registerInterceptKeyListener(VivoPolicyConstant.KEYCODE_WAKEUP_SWIPE, smartwakeKeyHandler);
        interceptKeyHandler.registerInterceptKeyListener(21, smartwakeKeyHandler);
        interceptKeyHandler.registerInterceptKeyListener(22, smartwakeKeyHandler);
        interceptKeyHandler.registerInterceptKeyListener(41, smartwakeKeyHandler);
        interceptKeyHandler.registerInterceptKeyListener(19, smartwakeKeyHandler);
        interceptKeyHandler.registerInterceptKeyListener(31, smartwakeKeyHandler);
        interceptKeyHandler.registerInterceptKeyListener(33, smartwakeKeyHandler);
        interceptKeyHandler.registerInterceptKeyListener(43, smartwakeKeyHandler);
        interceptKeyHandler.registerInterceptKeyListener(51, smartwakeKeyHandler);
        interceptKeyHandler.registerInterceptKeyListener(34, smartwakeKeyHandler);
        interceptKeyHandler.registerInterceptKeyListener(29, smartwakeKeyHandler);
        interceptKeyHandler.registerInterceptKeyListener(303, smartwakeKeyHandler);
        interceptKeyHandler.registerInterceptKeyListener(50, smartwakeKeyHandler);
        interceptKeyHandler.registerInterceptKeyListener(36, smartwakeKeyHandler);
        vivoWMPHook.registerWaitingForDrawnWindowListener(smartwakeKeyHandler);
        if (VivoPolicyConstant.OBJECT_AIKEY != null) {
            VivoAIKeyHandler aikeyHandler = new VivoAIKeyHandler(vivoWMPHook, vivoWMPHook.mContext);
            interceptKeyHandler.registerInterceptKeyListener(VivoPolicyConstant.KEYCODE_AI, aikeyHandler);
        }
        if (VivoPolicyConstant.OBJECT_GAME_PAD_LEFT != null && VivoPolicyConstant.OBJECT_GAME_PAD_RIGHT != null) {
            VivoGamepadKeyHandler gamepadKeyHandler = new VivoGamepadKeyHandler(vivoWMPHook.mContext, vivoWMPHook.mVivoPolicy, vivoWMPHook.mWindowManager, vivoWMPHook.mWindowManagerFuncs);
            interceptKeyHandler.registerInterceptKeyListener(VivoPolicyConstant.KEYCODE_GAME_PAD_LEFT, gamepadKeyHandler);
            interceptKeyHandler.registerInterceptKeyListener(VivoPolicyConstant.KEYCODE_GAME_PAD_RIGHT, gamepadKeyHandler);
        }
        if (VivoPolicyConstant.OBJECT_SCREEN_CLOCK_WAKE_UP_KEY != null) {
            VivoScreenClockKeyHandler screenClockKeyHandler = new VivoScreenClockKeyHandler(vivoWMPHook.mContext);
            interceptKeyHandler.registerInterceptKeyListener(VivoPolicyConstant.KEYCODE_SCREEN_CLOCK_WAKE_UP, screenClockKeyHandler);
        }
        if (VivoPolicyConstant.OBJECT_IMUSIC_COLLECT_KEY != null) {
            VivoIMusicCollectKeyHandler iMusicCollectKeyHandler = new VivoIMusicCollectKeyHandler(vivoWMPHook.mContext);
            interceptKeyHandler.registerInterceptKeyListener(VivoPolicyConstant.KEYCODE_IMUSIC_COLLECT, iMusicCollectKeyHandler);
        }
        if (VivoPolicyConstant.OBJECT_CAMERA_DOUBLE_CLICK_KEY != null) {
            VivoCameraDoubleClickKeyHandler cameraDoubleClickKeyHandler = new VivoCameraDoubleClickKeyHandler(vivoWMPHook.mContext, vivoWMPHook.mWindowManager);
            interceptKeyHandler.registerInterceptKeyListener(VivoPolicyConstant.KEYCODE_CAMERA_DOUBLE_CLICK, cameraDoubleClickKeyHandler);
        }
        interceptKeyHandler.hookupListeners(vivoWMPHook);
    }

    public static IVivoKeyFallbackListener getKeyFallbackListener(Context context) {
        IVivoKeyFallbackListener iVivoKeyFallbackListener = mKeyFallbackListener;
        if (iVivoKeyFallbackListener != null) {
            return iVivoKeyFallbackListener;
        }
        if (context.getApplicationContext() != null) {
            context = context.getApplicationContext();
        }
        VivoFallbackKeyRegister fallbackKeyHandler = new VivoFallbackKeyRegister(context);
        mKeyFallbackListener = fallbackKeyHandler;
        VivoCameraKeyHandler cameraKeyHandler = new VivoCameraKeyHandler(context);
        fallbackKeyHandler.registerInterceptKeyListener(27, cameraKeyHandler);
        return mKeyFallbackListener;
    }
}