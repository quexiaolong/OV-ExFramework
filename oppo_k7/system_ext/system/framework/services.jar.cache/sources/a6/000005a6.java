package com.android.server;

import android.common.IOplusCommonFactory;
import android.content.Context;
import android.os.Handler;
import android.util.Slog;
import com.android.server.am.ActivityManagerService;
import com.android.server.appop.AppOpsService;
import com.android.server.appop.OplusAppOpsService;
import com.android.server.audio.AudioService;
import com.android.server.wm.ActivityTaskManagerService;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/* loaded from: classes.dex */
public abstract class OplusCommonServiceFactory implements IOplusCommonFactory {
    private static final String AMS_CLASSNAME = "com.android.server.am.OplusActivityManagerService";
    private static final String AS_CLASSNAME = "com.android.server.audio.OplusAudioService";
    private static final String ATMS_CLASSNAME = "com.android.server.wm.OplusActivityTaskManagerService";
    private static final String MY_TAG = "OplusCommonServiceFactory";
    private static final String WMS_CLASSNAME = "com.android.server.wm.OplusWindowManagerService";
    private final String TAG = getClass().getSimpleName();

    public static final AudioService getOplusAudioService(Context context) {
        return createOplusAudioService(context);
    }

    public static final AppOpsService getAppOpsService(File file, Handler handler, Context context) {
        Slog.i(MY_TAG, "getOplusAppOpsService");
        return new OplusAppOpsService(file, handler, context);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Object newInstance(String className) throws Exception {
        Class<?> clazz = Class.forName(className);
        Constructor constructor = clazz.getConstructor(new Class[0]);
        return constructor.newInstance(new Object[0]);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void warn(String methodName) {
        Slog.w(this.TAG, methodName);
    }

    private static ActivityManagerService createActivityManagerService(Context context, ActivityTaskManagerService atms) {
        Slog.i(MY_TAG, "createActivityManagerService reflect");
        try {
            Class<?> clazz = Class.forName(AMS_CLASSNAME);
            Constructor constructor = clazz.getDeclaredConstructor(Context.class, ActivityTaskManagerService.class);
            return (ActivityManagerService) constructor.newInstance(context, atms);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IllegalAccessException e2) {
            e2.printStackTrace();
            return null;
        } catch (InstantiationException e3) {
            e3.printStackTrace();
            return null;
        } catch (NoSuchMethodException e4) {
            e4.printStackTrace();
            return null;
        } catch (InvocationTargetException e5) {
            e5.printStackTrace();
            return null;
        }
    }

    private static AudioService createOplusAudioService(Context context) {
        Slog.i(MY_TAG, "createOplusAudioService reflect");
        try {
            Class<?> clazz = Class.forName(AS_CLASSNAME);
            Constructor constructor = clazz.getDeclaredConstructor(Context.class);
            return (AudioService) constructor.newInstance(context);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IllegalAccessException e2) {
            e2.printStackTrace();
            return null;
        } catch (InstantiationException e3) {
            e3.printStackTrace();
            return null;
        } catch (NoSuchMethodException e4) {
            e4.printStackTrace();
            return null;
        } catch (InvocationTargetException e5) {
            e5.printStackTrace();
            return null;
        }
    }
}