package com.vivo.services.popupcamera;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.content.ComponentName;
import android.os.RemoteException;
import com.vivo.services.superresolution.Constant;
import java.util.List;

/* loaded from: classes.dex */
public class ApplicationProcessStateHelper {
    private static String SYSTEM_GALLERY_ACTIVITY = Constant.ACTIVITY_GALLERY;

    /* loaded from: classes.dex */
    public static class ApplicationProcessStatus {
        public boolean isAppForeground = false;
        public boolean isInGalleryActivity = false;
    }

    public static ApplicationProcessStatus isApplicationProcessForeground(String packageName) {
        boolean retFromWms;
        ApplicationProcessStatus ret = new ApplicationProcessStatus();
        try {
            List<ActivityManager.RunningTaskInfo> tasks = ActivityManagerNative.getDefault().getTasks(1);
            if (tasks != null && !tasks.isEmpty() && tasks.get(0) != null) {
                ComponentName topActivity = tasks.get(0).topActivity;
                if (topActivity.getPackageName().equals(packageName)) {
                    ret.isAppForeground = true;
                }
                if (topActivity.getClassName().equals(SYSTEM_GALLERY_ACTIVITY)) {
                    ret.isInGalleryActivity = true;
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if (!ret.isAppForeground) {
            try {
                retFromWms = WindowUtils.isPackageHasVisiblityWindow(packageName);
            } catch (Exception e2) {
                retFromWms = false;
            }
            if (retFromWms) {
                ret.isAppForeground = true;
            }
        }
        return ret;
    }
}