package com.android.server.wm;

import android.multidisplay.MultiDisplayManager;
import android.os.SystemProperties;
import com.vivo.services.rms.RmsInjectorImpl;
import com.vivo.services.rms.display.SceneManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoTaskLaunchParamsModifierImpl implements IVivoTaskLaunchParamsModifier {
    public static final boolean DEBUG_VIVO = SystemProperties.getBoolean("persist.vivo.display.debug", true);
    static final String TAG = "VivoTaskLaunchParamsModifier";

    public TaskDisplayArea getPreferredDisplayArea(TaskDisplayArea taskDisplayArea, ActivityRecord activity, ActivityStackSupervisor supervisor) {
        DisplayContent dc;
        ActivityStack top;
        ActivityStack top2;
        DisplayContent dc2;
        ActivityStack top3;
        if (!MultiDisplayManager.SUPPORT_CAR_NETWORKING) {
            return taskDisplayArea;
        }
        if (taskDisplayArea == null && activity != null && activity.launchedFromPackage != null) {
            if (MultiDisplayManager.isVCarDisplayRunning() && (dc2 = supervisor.mRootWindowContainer.getDisplayContent((int) SceneManager.APP_REQUEST_PRIORITY)) != null && (top3 = dc2.getTopStack()) != null && top3.isPresentWithPackage(activity.launchedFromPackage)) {
                TaskDisplayArea taskDisplayArea2 = dc2.getDefaultTaskDisplayArea();
                if (DEBUG_VIVO) {
                    VSlog.d("VivoCar", "display-from-vivo=" + taskDisplayArea2);
                }
                return taskDisplayArea2;
            }
            if (MultiDisplayManager.isUIDisplayRunning()) {
                DisplayContent dc3 = supervisor.mRootWindowContainer.getDisplayContent(95555);
                boolean isPreload = (activity.info == null || activity.info.applicationInfo == null || !RmsInjectorImpl.getInstance().isRmsPreload(activity.packageName, activity.info.applicationInfo.uid)) ? false : true;
                if (dc3 != null && isPreload && (top2 = dc3.getTopStack()) != null && top2.isPresentWithPackage(activity.launchedFromPackage)) {
                    TaskDisplayArea taskDisplayArea3 = dc3.getDefaultTaskDisplayArea();
                    if (DEBUG_VIVO) {
                        VSlog.d("VivoCar", "display-from-vivo=" + taskDisplayArea3);
                    }
                    return taskDisplayArea3;
                }
            }
            if (MultiDisplayManager.isVCarSecondDisplayRunning()) {
                for (int i = 1; i <= 5; i++) {
                    if (MultiDisplayManager.isVirtualDisplayRunning(i + SceneManager.APP_REQUEST_PRIORITY) && (dc = supervisor.mRootWindowContainer.getDisplayContent(i + SceneManager.APP_REQUEST_PRIORITY)) != null && (top = dc.getTopStack()) != null && top.isPresentWithPackage(activity.launchedFromPackage)) {
                        TaskDisplayArea taskDisplayArea4 = dc.getDefaultTaskDisplayArea();
                        if (DEBUG_VIVO) {
                            VSlog.d("VivoCar", "display-from-vivo=" + taskDisplayArea4);
                        }
                        return taskDisplayArea4;
                    }
                }
            }
        }
        return taskDisplayArea;
    }
}