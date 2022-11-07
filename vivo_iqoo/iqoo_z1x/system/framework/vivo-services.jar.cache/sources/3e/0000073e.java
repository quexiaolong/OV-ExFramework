package com.vivo.services.rms.sdk;

import android.app.ActivityManager;
import android.os.Bundle;
import android.os.Debug;
import android.os.IInterface;
import android.os.RemoteException;
import com.vivo.statistics.sdk.ArgPack;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public interface IRM extends IInterface {
    public static final int ACQUIRE_REFRESH_RATE = 23;
    public static final int CREATE_REFRESH_RATE_SCENE = 25;
    public static final String DESCRIPTOR = "com.vivo.rms.IRM";
    public static final int EXE_APP_CMD = 20;
    public static final int GET_ACTIVE_REFRESH_RATE = 26;
    public static final int GET_BUNDBLE = 7;
    public static final int GET_PROCESS_MEMORY_INFO = 21;
    public static final int GET_PSS = 4;
    public static final int GET_RUNNING_APP_PROCESSES = 18;
    public static final int IS_BROADCAST_REGISTERED = 19;
    public static final int KILL_PROCESS = 2;
    public static final int NEED_KEEP_QUIET_PKG = 14;
    public static final int NEED_KEEP_QUIET_UID = 13;
    public static final int NOTIFY_WORKING_STATE = 22;
    public static final int PROXY_APP = 17;
    public static final int READ_PATH = 11;
    public static final int READ_PROF_FILE = 9;
    public static final int RELEASE_REFRESH_RATE = 24;
    public static final int RESTORE_SYS_FS = 8;
    public static final int SET_APP_LIST = 1;
    public static final int SET_BUNDBLE = 6;
    public static final int SET_NEED_KEEP_QUIET_PKG = 16;
    public static final int SET_NEED_KEEP_QUIET_UID = 15;
    public static final int START_PROCESS = 12;
    public static final int STOP_PACKAGE = 3;
    public static final int WRITE_PATH = 10;
    public static final int WRITE_SYS_FS = 5;

    void acquireRefreshRate(long j, String str, String str2, int i, int i2, int i3, int i4, int i5, int i6, boolean z, int i7) throws RemoteException;

    boolean createRefreshRateScene(String str, int i, boolean z) throws RemoteException;

    ArgPack exeAppCmd(int i, int i2, ArgPack argPack) throws RemoteException;

    float getActiveRefreshRate() throws RemoteException;

    Bundle getBundle(String str) throws RemoteException;

    Debug.MemoryInfo[] getProcessMemoryInfo(int[] iArr, boolean z) throws RemoteException;

    int getPss(int i) throws RemoteException;

    ActivityManager.RunningAppProcessInfo getRunningAppProcesses(int i) throws RemoteException;

    boolean isBroadcastRegistered(String str, int i, String str2, int i2) throws RemoteException;

    void killProcess(int[] iArr, int[] iArr2, String str, boolean z) throws RemoteException;

    boolean needKeepQuiet(int i, int i2, int i3) throws RemoteException;

    boolean needKeepQuiet(String str, int i, int i2, int i3) throws RemoteException;

    void notifyWorkingState(String str, int i, int i2, boolean z) throws RemoteException;

    void proxyApp(List<String> list, int i, int i2, boolean z, String str) throws RemoteException;

    ArrayList<String> readPath(String str, int i) throws RemoteException;

    boolean readProcFile(String str, int[] iArr, String[] strArr, long[] jArr, float[] fArr) throws RemoteException;

    void releaseRefreshRate(int i, long j) throws RemoteException;

    boolean restoreSysFs(ArrayList<String> arrayList) throws RemoteException;

    void setAppList(String str, ArrayList<String> arrayList) throws RemoteException;

    boolean setBundle(String str, Bundle bundle) throws RemoteException;

    void setNeedKeepQuiet(int i, int i2, boolean z) throws RemoteException;

    void setNeedKeepQuiet(String str, int i, int i2, boolean z) throws RemoteException;

    int startProcess(int i, String str, String str2, String str3, boolean z, long j) throws RemoteException;

    void stopPackage(String str, int i, String str2) throws RemoteException;

    boolean writePath(String str, String str2) throws RemoteException;

    boolean writeSysFs(ArrayList<String> arrayList, ArrayList<String> arrayList2) throws RemoteException;
}