package com.android.server.policy;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Process;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import com.vivo.common.utils.VLog;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/* loaded from: classes.dex */
public final class VivoPolicyUtil {
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.policy.VivoPolicyUtil.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (VivoPolicyConstant.ACTION_VIVO_LOG_CHANGED.equals(action)) {
                VivoPolicyUtil.IS_LOG_OPEN = SystemProperties.getBoolean("persist.sys.log.ctrl", false);
            } else if (VivoPolicyConstant.ACTION_MTK_LOG_CHANGED.equals(action)) {
                VivoPolicyUtil.IS_LOG_OPEN = SystemProperties.getBoolean("persist.sys.log.ctrl", false);
            }
        }
    };
    private Context mContext;
    public static boolean IS_LOG_OPEN = SystemProperties.get("persist.sys.log.ctrl", "no").equals("yes");
    public static boolean IS_INPUT_LOG_OPEN = SystemProperties.get(VivoPolicyConstant.KEY_VIVO_INPUT_LOG_CTRL, "no").equals("yes");
    private static volatile VivoPolicyUtil sVivoUtil = null;
    private static byte[] mLock = new byte[0];

    private VivoPolicyUtil(Context context) {
        this.mContext = null;
        this.mContext = context;
        registerReceiver();
    }

    public static VivoPolicyUtil createInstance(Context context) {
        if (sVivoUtil == null) {
            synchronized (mLock) {
                if (sVivoUtil == null) {
                    sVivoUtil = new VivoPolicyUtil(context);
                    sVivoUtil.printCallerInfo("create VivoPolicyUtil");
                }
            }
        }
        return sVivoUtil;
    }

    public static void destoryInstance() {
        if (sVivoUtil != null) {
            sVivoUtil.printCallerInfo("destory VivoPolicyUtil");
            sVivoUtil.unregisterReceiver();
            sVivoUtil = null;
        }
    }

    public static VivoPolicyUtil peekInstance() {
        return sVivoUtil;
    }

    public static void printf(String TAG, String msg) {
        if (IS_LOG_OPEN) {
            VLog.d(TAG, msg);
        }
    }

    public static boolean isSPSMode() {
        return SystemProperties.getBoolean("sys.super_power_save", false);
    }

    public static void launchSPS(Context context) {
        printf(VivoWMPHook.TAG, "launchSPS");
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setClassName("com.bbk.SuperPowerSave", "com.bbk.SuperPowerSave.SuperPowerSaveActivity");
        intent.addFlags(270532608);
        try {
            context.startActivityAsUser(intent, UserHandle.CURRENT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isDrivingMode() {
        return SystemProperties.getBoolean(VivoPolicyConstant.KEY_DRIVING_MODE, false);
    }

    public static void launchDrivingMode(Context context) {
        printf(VivoWMPHook.TAG, "launchDrivingMode");
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setClassName("com.bbk.DrivingMode", "com.bbk.DrivingMode.DrivingModeActivity");
        intent.addFlags(270532608);
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendToggleSPSBroadcast(Context context) {
        boolean isOn = isSPSMode();
        StringBuilder sb = new StringBuilder();
        sb.append("Toggle SPS to ");
        sb.append(!isOn);
        printf(VivoWMPHook.TAG, sb.toString());
        Intent i = new Intent("intent.action.super_power_save");
        i.putExtra("sps_action", isOn ? "stop" : "start");
        context.sendBroadcast(i);
    }

    public static boolean isMotorMode(Context context) {
        ContentResolver resolver = context.getContentResolver();
        return Settings.System.getInt(resolver, VivoPolicyConstant.KEY_MOTOR_MODE, 0) == 1;
    }

    public static void launchMotorMode(Context context) {
        printf(VivoWMPHook.TAG, "launchMotorMode");
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setClassName("com.vivo.motormode", "com.vivo.motormode.MotorModeHomeActivity");
        intent.addFlags(270532608);
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendPowerKeyScreenOffBroadcast(Context context) {
        printf(VivoWMPHook.TAG, "sendPowerKeyScreenOffBroadcast");
        Intent intent = new Intent(VivoPolicyConstant.ACTION_POWER_KEY_SCREEN_OFF);
        context.sendBroadcast(intent);
    }

    public static Class<?> getClass(String className) {
        try {
            Class<?> c = Class.forName(className);
            return c;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object invokeStaticMethodForResult(Class<?> c, String methodName, Class<?>[] parameterTypes, Object[] parameterValues) {
        try {
            Method m = c.getMethod(methodName, parameterTypes);
            try {
                Object result = m.invoke(c, parameterValues);
                return result;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } catch (NoSuchMethodException e2) {
            e2.printStackTrace();
            return null;
        }
    }

    public static void invokeStaticMethod(Class<?> c, String methodName, Class<?>[] parameterTypes, Object[] parameterValues) {
        try {
            Method m = c.getMethod(methodName, parameterTypes);
            try {
                m.invoke(c, parameterValues);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (NoSuchMethodException e2) {
            e2.printStackTrace();
        }
    }

    public static Object invokeMethodForResult(Object receiver, String methodName, Class<?>[] parameterTypes, Object[] parameterValues) {
        Class<?> c = receiver.getClass();
        try {
            Method m = c.getMethod(methodName, parameterTypes);
            try {
                Object result = m.invoke(receiver, parameterValues);
                return result;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } catch (NoSuchMethodException e2) {
            e2.printStackTrace();
            return null;
        }
    }

    public static void invokeMethod(Object receiver, String methodName, Class<?>[] parameterTypes, Object[] parameterValues) {
        Class<?> c = receiver.getClass();
        try {
            Method m = c.getMethod(methodName, parameterTypes);
            try {
                m.invoke(receiver, parameterValues);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (NoSuchMethodException e2) {
            e2.printStackTrace();
        }
    }

    public static Object createObject(String className, Class<?>[] parameterTypes, Object[] parameterValues) {
        Class<?> c = getClass(className);
        if (c == null) {
            return null;
        }
        try {
            Object result = c.getConstructor(parameterTypes).newInstance(parameterValues);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object getStaticFieldValue(Class<?> c, String filedName) {
        try {
            Field field = c.getDeclaredField(filedName);
            field.setAccessible(true);
            Object fildValue = field.get(c);
            return fildValue;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object getFieldValue(Object object, String filedName) {
        Class<?> classType = object.getClass();
        try {
            Field field = classType.getDeclaredField(filedName);
            field.setAccessible(true);
            Object fildValue = field.get(object);
            return fildValue;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void printCallerInfo(String msg) {
        int pid = Process.myPid();
        int uid = Process.myUid();
        VLog.i(VivoWMPHook.TAG, msg + " by PID=" + pid + "; UID=" + uid + "; " + Thread.currentThread().toString());
    }

    private void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(VivoPolicyConstant.ACTION_VIVO_LOG_CHANGED);
        intentFilter.addAction(VivoPolicyConstant.ACTION_MTK_LOG_CHANGED);
        this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter);
    }

    private void unregisterReceiver() {
        this.mContext.unregisterReceiver(this.mBroadcastReceiver);
    }

    private void verifyVivoLogState(Intent intent) {
        boolean isVivoLogOpen = SystemProperties.getInt("persist.sys.is_bbk_log", 0) != 0;
        if (isVivoLogOpen != IS_LOG_OPEN) {
            IS_LOG_OPEN = isVivoLogOpen;
            SystemProperties.set("persist.sys.log.ctrl", isVivoLogOpen ? "yes" : "no");
        }
    }

    private void verifyMtkLogState(Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            bundle.getInt("affected_log_type");
            int changedValue = bundle.getInt("log_new_state");
            boolean isMtkMobileLogOpen = (changedValue & 4) != 0;
            if (isMtkMobileLogOpen != IS_LOG_OPEN) {
                IS_LOG_OPEN = isMtkMobileLogOpen;
                SystemProperties.set("persist.sys.log.ctrl", isMtkMobileLogOpen ? "yes" : "no");
            }
        }
    }

    public static void launchFamilyCareRemindActivity(Context context) {
        printf(VivoWMPHook.TAG, "launchFamilyCareRemindActivity");
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setClassName("com.vivo.familycare.local", "com.vivo.familycare.local.view.RemindActivity");
        intent.addFlags(270532608);
        try {
            context.startActivityAsUser(intent, UserHandle.CURRENT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}