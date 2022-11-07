package com.vivo.services.backup.util;

import android.app.IBackupAgent;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import com.android.server.backup.BackupManagerService;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import vivo.app.backup.IVivoBackupManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoBackupReflectUtil {
    private static final String TAG = "VIVO_BACKUP_VivoBackupReflectUtil";
    private static Method mMtdBindToAgentSynchronousFromVivo;
    private static Method mMtdClearApplicationDataBeforeRestoreFromVivo;
    private static Method mMtdDoBackupByZip;
    private static Method mMtdDoRestoreAndroidPathByZip;
    private static Method mMtdDoRestoreByZip;
    private static Method mMtdTearDownAgentAndKillFromVivo;

    static {
        mMtdBindToAgentSynchronousFromVivo = null;
        mMtdTearDownAgentAndKillFromVivo = null;
        mMtdClearApplicationDataBeforeRestoreFromVivo = null;
        mMtdDoBackupByZip = null;
        mMtdDoRestoreByZip = null;
        mMtdDoRestoreAndroidPathByZip = null;
        Class<?> clz = null;
        try {
            clz = Class.forName("com.android.server.backup.BackupManagerService");
        } catch (ClassNotFoundException e) {
            VSlog.e(TAG, "can't find class com.android.server.backup.BackupManagerService");
        }
        if (clz != null) {
            try {
                mMtdBindToAgentSynchronousFromVivo = clz.getMethod("bindToAgentSynchronousFromVivo", String.class, Integer.TYPE);
            } catch (NoSuchMethodException e2) {
                VSlog.e(TAG, "cant reflect method bindToAgentSynchronousFromVivo");
                mMtdBindToAgentSynchronousFromVivo = null;
            }
        }
        if (clz != null) {
            try {
                mMtdTearDownAgentAndKillFromVivo = clz.getMethod("tearDownAgentAndKillFromVivo", String.class);
            } catch (NoSuchMethodException e3) {
                VSlog.e(TAG, "cant reflect method tearDownAgentAndKillFromVivo");
                mMtdTearDownAgentAndKillFromVivo = null;
            }
        }
        if (clz != null) {
            try {
                Method method = clz.getMethod("clearApplicationDataBeforeRestoreFromVivo", String.class);
                mMtdClearApplicationDataBeforeRestoreFromVivo = method;
                method.setAccessible(true);
            } catch (NoSuchMethodException e4) {
                VSlog.e(TAG, "can't reflect method clearApplicationDataBeforeRestoreFromVivo");
                mMtdClearApplicationDataBeforeRestoreFromVivo = null;
            }
        }
        Class<?> clz2 = null;
        try {
            clz2 = Class.forName("android.app.IBackupAgent");
        } catch (ClassNotFoundException e5) {
            VSlog.e(TAG, "can't find class android.app.backup.BackupAgent$BackupServiceBinder");
        }
        if (clz2 != null) {
            try {
                mMtdDoBackupByZip = clz2.getDeclaredMethod("doBackupByZip", Integer.TYPE, ParcelFileDescriptor.class, String[].class, Boolean.TYPE, IVivoBackupManager.class);
            } catch (NoSuchMethodException e6) {
                VSlog.e(TAG, "cant reflect method doBackupByZip");
                mMtdDoBackupByZip = null;
            }
        }
        if (clz2 != null) {
            try {
                mMtdDoRestoreByZip = clz2.getDeclaredMethod("doRestoreByZip", Integer.TYPE, ParcelFileDescriptor.class, IVivoBackupManager.class, String[].class);
            } catch (NoSuchMethodException e7) {
                VSlog.e(TAG, "cant reflect method doRestoreByZip");
                mMtdDoRestoreByZip = null;
            }
        }
        if (clz2 != null) {
            try {
                mMtdDoRestoreAndroidPathByZip = clz2.getDeclaredMethod("doRestoreAndroidPathByZip", Integer.TYPE, ParcelFileDescriptor.class, IVivoBackupManager.class, String[].class, List.class, List.class, Integer.TYPE);
            } catch (NoSuchMethodException e8) {
                VSlog.e(TAG, "cant reflect method doRestoreAndroidPathByZip");
                mMtdDoRestoreAndroidPathByZip = null;
            }
        }
    }

    public static IBackupAgent callBindToAgentSynchronousFromVivo(BackupManagerService service, String pkgName, int mode) {
        Method method = mMtdBindToAgentSynchronousFromVivo;
        if (method != null) {
            try {
                return (IBackupAgent) method.invoke(service, pkgName, Integer.valueOf(mode));
            } catch (IllegalAccessException e) {
                VSlog.e(TAG, "invoke bindToAgentSynchronousFromVivo Excecption", e);
                return null;
            } catch (InvocationTargetException e2) {
                VSlog.e(TAG, "invoke bindToAgentSynchronousFromVivo Exception", e2);
                return null;
            }
        }
        VSlog.e(TAG, "reflect bindToAgentSynchronousFromVivo failed");
        return null;
    }

    public static void callTearDownAgentAndKillFromVivo(BackupManagerService service, String pkgName) {
        Method method = mMtdTearDownAgentAndKillFromVivo;
        if (method == null) {
            VSlog.e(TAG, "reflect earDownAgentAndKillFromVivo failed");
            return;
        }
        try {
            method.invoke(service, pkgName);
        } catch (IllegalAccessException e) {
            VSlog.e(TAG, "invoke earDownAgentAndKillFromVivo Excecption", e);
        } catch (InvocationTargetException e2) {
            VSlog.e(TAG, "invoke earDownAgentAndKillFromVivo Exception", e2);
        }
    }

    public static void callClearApplicationDataBeforeRestoreFromVivo(BackupManagerService service, String packageName) {
        Method method = mMtdClearApplicationDataBeforeRestoreFromVivo;
        if (method == null) {
            VSlog.e(TAG, "reflect clearApplicationDataBeforeRestoreFromVivo failed");
            return;
        }
        try {
            method.invoke(service, packageName);
        } catch (IllegalAccessException e) {
            VSlog.e(TAG, "invoke clearApplicationDataBeforeRestoreFromVivo Excecption", e);
        } catch (InvocationTargetException e2) {
            VSlog.e(TAG, "invoke clearApplicationDataBeforeRestoreFromVivo Exception", e2);
        }
    }

    public static void callDoBackupByZip(IBackupAgent service, int fd, ParcelFileDescriptor data, String[] dirs, boolean asDual, IVivoBackupManager callbackBinder) throws RemoteException {
        Method method = mMtdDoBackupByZip;
        if (method == null) {
            VSlog.e(TAG, "reflect DoBackupByZip failed");
            return;
        }
        try {
            method.invoke(service, Integer.valueOf(fd), data, dirs, Boolean.valueOf(asDual), callbackBinder);
        } catch (IllegalAccessException e) {
            VSlog.e(TAG, "invoke DoBackupByZip Excecption", e);
        } catch (InvocationTargetException e2) {
            VSlog.e(TAG, "invoke DoBackupByZip Exception", e2);
        }
    }

    public static void callDoRestoreByZip(IBackupAgent service, int fd, ParcelFileDescriptor data, IVivoBackupManager callbackBinder, String[] cmdList) throws RemoteException {
        Method method = mMtdDoRestoreByZip;
        if (method == null) {
            VSlog.e(TAG, "reflect DoRestoreByZip failed");
            return;
        }
        try {
            method.invoke(service, Integer.valueOf(fd), data, callbackBinder, cmdList);
        } catch (IllegalAccessException e) {
            VSlog.e(TAG, "invoke DoRestoreByZip Excecption", e);
        } catch (InvocationTargetException e2) {
            VSlog.e(TAG, "invoke DoRestoreByZip Exception", e2);
        }
    }

    public static void callDoRestoreAndroidPathByZip(IBackupAgent service, int fd, ParcelFileDescriptor data, IVivoBackupManager callbackBinder, String[] cmdList, List<String> oldPath, List<String> newPath, int bufferSize) throws RemoteException {
        Method method = mMtdDoRestoreAndroidPathByZip;
        if (method != null) {
            try {
                method.invoke(service, Integer.valueOf(fd), data, callbackBinder, cmdList, oldPath, newPath, Integer.valueOf(bufferSize));
                return;
            } catch (IllegalAccessException e) {
                VSlog.e(TAG, "invoke DoRestoreAndroidPathByZip Excecption" + e);
                return;
            } catch (InvocationTargetException e2) {
                VSlog.e(TAG, "invoke DoRestoreAndroidPathByZip Exception" + e2);
                return;
            }
        }
        VSlog.e(TAG, "reflect DoRestoreAndroidPathByZip failed");
    }
}