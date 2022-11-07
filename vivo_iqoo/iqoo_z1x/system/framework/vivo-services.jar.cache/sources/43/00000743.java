package com.vivo.services.rms.sdk;

import android.app.ActivityManager;
import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import com.vivo.statistics.sdk.ArgPack;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class RMProxy implements IRM {
    private IBinder mRemote;

    public RMProxy(IBinder remote) {
        this.mRemote = remote;
    }

    @Override // android.os.IInterface
    public IBinder asBinder() {
        return this.mRemote;
    }

    public String getInterfaceDescriptor() {
        return IRM.DESCRIPTOR;
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public void setAppList(String typeName, ArrayList<String> appList) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeString(typeName);
        data.writeStringList(appList);
        try {
            this.mRemote.transact(1, data, reply, 0);
            reply.readException();
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public void killProcess(int[] pids, int[] curAdjs, String reason, boolean secure) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeIntArray(pids);
        data.writeIntArray(curAdjs);
        data.writeString(reason);
        data.writeInt(secure ? 1 : 0);
        try {
            this.mRemote.transact(2, data, reply, 0);
            reply.readException();
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public void stopPackage(String pkg, int userId, String reason) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeString(pkg);
        data.writeInt(userId);
        data.writeString(reason);
        try {
            this.mRemote.transact(3, data, reply, 0);
            reply.readException();
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public int getPss(int pid) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeInt(pid);
        try {
            this.mRemote.transact(4, data, reply, 0);
            int pss = reply.readInt();
            reply.readException();
            return pss;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public boolean writeSysFs(ArrayList<String> fileNames, ArrayList<String> values) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeStringList(fileNames);
        data.writeStringList(values);
        try {
            this.mRemote.transact(5, data, reply, 0);
            int result = reply.readInt();
            reply.readException();
            reply.recycle();
            data.recycle();
            return result == 1;
        } catch (Throwable th) {
            reply.recycle();
            data.recycle();
            throw th;
        }
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public boolean setBundle(String name, Bundle bundle) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeString(name);
        if (bundle == null) {
            bundle = new Bundle();
        }
        bundle.writeToParcel(data, 0);
        try {
            this.mRemote.transact(6, data, reply, 0);
            int result = reply.readInt();
            reply.readException();
            reply.recycle();
            data.recycle();
            if (result != 1) {
                return false;
            }
            return true;
        } catch (Throwable th) {
            reply.recycle();
            data.recycle();
            throw th;
        }
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public Bundle getBundle(String name) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeString(name);
        Bundle bundle = new Bundle();
        try {
            this.mRemote.transact(7, data, reply, 0);
            bundle.readFromParcel(reply);
            reply.readException();
            return bundle;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public boolean restoreSysFs(ArrayList<String> fileNames) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeStringList(fileNames);
        try {
            this.mRemote.transact(8, data, reply, 0);
            int result = reply.readInt();
            reply.readException();
            reply.recycle();
            data.recycle();
            return result == 1;
        } catch (Throwable th) {
            reply.recycle();
            data.recycle();
            throw th;
        }
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public boolean readProcFile(String file, int[] format, String[] outStrings, long[] outLongs, float[] outFloats) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeString(file);
        data.writeIntArray(format);
        data.writeStringArray(outStrings);
        data.writeLongArray(outLongs);
        data.writeFloatArray(outFloats);
        try {
            this.mRemote.transact(9, data, reply, 0);
            int result = reply.readInt();
            if (outStrings != null && outStrings.length > 0) {
                reply.readStringArray(outStrings);
            }
            if (outLongs != null && outLongs.length > 0) {
                reply.readLongArray(outLongs);
            }
            if (outFloats != null && outFloats.length > 0) {
                reply.readFloatArray(outFloats);
            }
            reply.readException();
            reply.recycle();
            data.recycle();
            return result == 1;
        } catch (Throwable th) {
            reply.recycle();
            data.recycle();
            throw th;
        }
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public boolean writePath(String path, String value) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeString(path);
        data.writeString(value);
        try {
            this.mRemote.transact(10, data, reply, 0);
            int result = reply.readInt();
            reply.readException();
            reply.recycle();
            data.recycle();
            return result == 1;
        } catch (Throwable th) {
            reply.recycle();
            data.recycle();
            throw th;
        }
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public ArrayList<String> readPath(String path, int bufferSize) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeString(path);
        data.writeInt(bufferSize);
        try {
            this.mRemote.transact(11, data, reply, 0);
            ArrayList<String> result = reply.createStringArrayList();
            reply.readException();
            return result;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public int startProcess(int userId, String pkg, String proc, String reason, boolean keepQuiet, long delay) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeInt(userId);
        data.writeString(pkg);
        data.writeString(proc);
        data.writeString(reason);
        data.writeInt(keepQuiet ? 1 : 0);
        data.writeLong(delay);
        try {
            this.mRemote.transact(12, data, reply, 0);
            int result = reply.readInt();
            reply.readException();
            return result;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public boolean needKeepQuiet(int pid, int uid, int flag) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeInt(pid);
        data.writeInt(uid);
        data.writeInt(flag);
        try {
            this.mRemote.transact(13, data, reply, 0);
            int result = reply.readInt();
            reply.readException();
            reply.recycle();
            data.recycle();
            return result == 1;
        } catch (Throwable th) {
            reply.recycle();
            data.recycle();
            throw th;
        }
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public boolean needKeepQuiet(String pkgName, int userId, int uid, int flag) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeString(pkgName);
        data.writeInt(userId);
        data.writeInt(uid);
        data.writeInt(flag);
        try {
            this.mRemote.transact(14, data, reply, 0);
            int result = reply.readInt();
            reply.readException();
            reply.recycle();
            data.recycle();
            return result == 1;
        } catch (Throwable th) {
            reply.recycle();
            data.recycle();
            throw th;
        }
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public void setNeedKeepQuiet(int pid, int uid, boolean keepQuiet) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeInt(pid);
        data.writeInt(uid);
        data.writeInt(keepQuiet ? 1 : 0);
        try {
            this.mRemote.transact(15, data, reply, 0);
            reply.readException();
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public void setNeedKeepQuiet(String pkgName, int userId, int uid, boolean keepQuiet) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeString(pkgName);
        data.writeInt(userId);
        data.writeInt(uid);
        data.writeInt(keepQuiet ? 1 : 0);
        try {
            this.mRemote.transact(16, data, reply, 0);
            reply.readException();
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public void proxyApp(List<String> pkgList, int userId, int mode, boolean proxy, String module) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeStringList(pkgList);
        data.writeInt(userId);
        data.writeInt(mode);
        data.writeInt(proxy ? 1 : 0);
        data.writeString(module);
        this.mRemote.transact(17, data, reply, 0);
        reply.readException();
        reply.recycle();
        data.recycle();
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public ActivityManager.RunningAppProcessInfo getRunningAppProcesses(int pid) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeInt(pid);
        this.mRemote.transact(18, data, reply, 0);
        ActivityManager.RunningAppProcessInfo app = new ActivityManager.RunningAppProcessInfo();
        try {
            app.readFromParcel(reply);
            reply.readException();
            return app;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public boolean isBroadcastRegistered(String pkgName, int userId, String action, int flags) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeString(pkgName);
        data.writeInt(userId);
        data.writeString(action);
        data.writeInt(flags);
        try {
            this.mRemote.transact(19, data, reply, 0);
            boolean registered = reply.readInt() != 0;
            reply.readException();
            return registered;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public ArgPack exeAppCmd(int pid, int cmd, ArgPack argPack) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInt(pid);
            data.writeInt(cmd);
            argPack.writeToParcel(data, 0);
            this.mRemote.transact(20, data, reply, 0);
            return ArgPack.createFromParcel(reply);
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public Debug.MemoryInfo[] getProcessMemoryInfo(int[] pids, boolean cached) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            try {
                data.writeIntArray(pids);
                data.writeInt(cached ? 1 : 0);
                this.mRemote.transact(21, data, reply, 0);
                reply.readException();
                return (Debug.MemoryInfo[]) reply.createTypedArray(Debug.MemoryInfo.CREATOR);
            } catch (RemoteException e) {
                e.printStackTrace();
                data.recycle();
                reply.recycle();
                return null;
            }
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public void notifyWorkingState(String pkgName, int uid, int mask, boolean state) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeString(pkgName);
            data.writeInt(uid);
            data.writeInt(mask);
            data.writeInt(state ? 1 : 0);
            this.mRemote.transact(22, data, reply, 1);
            reply.readException();
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public void acquireRefreshRate(long handle, String sceneName, String reason, int fps, int priority, int duration, int caller, int client, int states, boolean dfps, int extra) throws RemoteException {
        Parcel data = Parcel.obtain();
        try {
            data.writeLong(handle);
        } catch (Throwable th) {
            th = th;
        }
        try {
            data.writeString(sceneName);
            try {
                data.writeString(reason);
            } catch (Throwable th2) {
                th = th2;
                data.recycle();
                throw th;
            }
            try {
                data.writeInt(fps);
            } catch (Throwable th3) {
                th = th3;
                data.recycle();
                throw th;
            }
            try {
                data.writeInt(priority);
            } catch (Throwable th4) {
                th = th4;
                data.recycle();
                throw th;
            }
        } catch (Throwable th5) {
            th = th5;
            data.recycle();
            throw th;
        }
        try {
            data.writeInt(duration);
            try {
                data.writeInt(caller);
            } catch (Throwable th6) {
                th = th6;
                data.recycle();
                throw th;
            }
            try {
                data.writeInt(client);
            } catch (Throwable th7) {
                th = th7;
                data.recycle();
                throw th;
            }
            try {
                data.writeInt(states);
                data.writeInt(dfps ? 1 : 0);
                try {
                    data.writeInt(extra);
                    try {
                        this.mRemote.transact(23, data, null, 1);
                        data.recycle();
                    } catch (Throwable th8) {
                        th = th8;
                        data.recycle();
                        throw th;
                    }
                } catch (Throwable th9) {
                    th = th9;
                }
            } catch (Throwable th10) {
                th = th10;
                data.recycle();
                throw th;
            }
        } catch (Throwable th11) {
            th = th11;
            data.recycle();
            throw th;
        }
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public void releaseRefreshRate(int caller, long handle) throws RemoteException {
        Parcel data = Parcel.obtain();
        try {
            data.writeInt(caller);
            data.writeLong(handle);
            this.mRemote.transact(24, data, null, 1);
        } finally {
            data.recycle();
        }
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public boolean createRefreshRateScene(String name, int priority, boolean powerFirst) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeString(name);
            data.writeInt(priority);
            data.writeInt(powerFirst ? 1 : 0);
            this.mRemote.transact(25, data, reply, 0);
            reply.readException();
            return reply.readInt() == 1;
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    @Override // com.vivo.services.rms.sdk.IRM
    public float getActiveRefreshRate() throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            this.mRemote.transact(26, data, reply, 0);
            reply.readException();
            return reply.readFloat();
        } finally {
            data.recycle();
            reply.recycle();
        }
    }
}