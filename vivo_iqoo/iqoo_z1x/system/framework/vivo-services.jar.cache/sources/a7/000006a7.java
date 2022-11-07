package com.vivo.services.popupcamera;

import android.text.TextUtils;
import com.vivo.common.utils.VLog;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/* loaded from: classes.dex */
public class WindowUtils {
    private static final String TAG = "PopupCameraManagerService";
    private static final String WINDOW_MANAGER_SERVICE_CLASS = "com.android.server.wm.WindowManagerService";
    private static final String WINDOW_MANAGER_SERVICE_getFocusedWindow = "getFocusedWindow";
    private static final String WINDOW_MANAGER_SERVICE_getInstance = "getInstance";
    private static final String WINDOW_MANAGER_SERVICE_isPackageHasVisiblityWindow = "isPackageHasVisiblityWindow";
    private static final String WINDOW_STATE_CLASS = "com.android.server.wm.WindowState";
    private static final String WINDOW_STATE_getAttrs = "getAttrs";
    private static volatile boolean isWindowManagerServiceClassMethodInited = false;
    private static Class wmsClass = null;
    private static Method wms_getInstance = null;
    private static Method wms_isPackageHasVisiblityWindow = null;

    private static void initWindowManagerServiceClassAndMethod() {
        if (!isWindowManagerServiceClassMethodInited) {
            try {
                wmsClass = Class.forName(WINDOW_MANAGER_SERVICE_CLASS);
            } catch (ClassNotFoundException e) {
                VLog.d(TAG, "com.android.server.wm.WindowManagerServiceclass is not found!!!!");
                wmsClass = null;
            }
            Class cls = wmsClass;
            if (cls != null) {
                try {
                    Method declaredMethod = cls.getDeclaredMethod(WINDOW_MANAGER_SERVICE_getInstance, new Class[0]);
                    wms_getInstance = declaredMethod;
                    declaredMethod.setAccessible(true);
                } catch (NoSuchMethodException e2) {
                    VLog.d(TAG, "getInstance in com.android.server.wm.WindowManagerServiceclass is not found!!!!");
                    wms_getInstance = null;
                }
                try {
                    Method declaredMethod2 = wmsClass.getDeclaredMethod(WINDOW_MANAGER_SERVICE_isPackageHasVisiblityWindow, String.class);
                    wms_isPackageHasVisiblityWindow = declaredMethod2;
                    declaredMethod2.setAccessible(true);
                } catch (NoSuchMethodException e3) {
                    wms_isPackageHasVisiblityWindow = null;
                    VLog.d(TAG, "getFocusedWindow in com.android.server.wm.WindowManagerServiceclass is not found!!!!");
                }
            }
            isWindowManagerServiceClassMethodInited = true;
        }
    }

    public static boolean isPackageHasVisiblityWindow(String packageName) {
        Method method;
        Method method2;
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        initWindowManagerServiceClassAndMethod();
        Object wms = null;
        if (wmsClass != null && (method2 = wms_getInstance) != null) {
            try {
                wms = method2.invoke(null, null);
            } catch (IllegalAccessException e) {
                VLog.d(TAG, "invoke method getInstance failed!!!");
                return false;
            } catch (InvocationTargetException e2) {
                VLog.d(TAG, "invoke method getInstance failed!!!");
                return false;
            }
        }
        if (wms == null || (method = wms_isPackageHasVisiblityWindow) == null) {
            return false;
        }
        try {
            boolean ret = ((Boolean) method.invoke(wms, packageName)).booleanValue();
            return ret;
        } catch (IllegalAccessException e3) {
            VLog.d(TAG, "invoke method getFocusedWindow failed!!!");
            return false;
        } catch (InvocationTargetException e4) {
            VLog.d(TAG, "invoke method getFocusedWindow failed!!!");
            return false;
        }
    }
}