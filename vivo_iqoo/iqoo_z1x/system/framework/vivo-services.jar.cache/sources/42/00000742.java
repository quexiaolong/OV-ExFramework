package com.vivo.services.rms.sdk;

import android.app.ActivityManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import com.vivo.statistics.sdk.ArgPack;
import java.util.ArrayList;

/* loaded from: classes.dex */
public abstract class RMNative extends Binder implements IRM {
    public static final String SERVICE_NAME = "rms";
    public static final String VERSION = "1.0";

    public RMNative() {
        attachInterface(this, IRM.DESCRIPTOR);
    }

    public static IRM asInterface(IBinder obj) {
        if (obj == null) {
            return null;
        }
        IRM in = (IRM) obj.queryLocalInterface(IRM.DESCRIPTOR);
        if (in != null) {
            return in;
        }
        return new RMProxy(obj);
    }

    @Override // android.os.IInterface
    public IBinder asBinder() {
        return this;
    }

    @Override // android.os.Binder
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        boolean powerFirst;
        switch (code) {
            case 1:
                String typeName = data.readString();
                ArrayList<String> appList = data.createStringArrayList();
                setAppList(typeName, appList);
                reply.writeNoException();
                return true;
            case 2:
                int[] pids = data.createIntArray();
                int[] curAdjs = data.createIntArray();
                String reason = data.readString();
                powerFirst = data.readInt() == 1;
                killProcess(pids, curAdjs, reason, powerFirst);
                reply.writeNoException();
                return true;
            case 3:
                String pkg = data.readString();
                int userId = data.readInt();
                String reason2 = data.readString();
                stopPackage(pkg, userId, reason2);
                reply.writeNoException();
                return true;
            case 4:
                int pid = data.readInt();
                int pss = getPss(pid);
                reply.writeInt(pss);
                reply.writeNoException();
                return true;
            case 5:
                ArrayList<String> fileNames = data.createStringArrayList();
                ArrayList<String> values = data.createStringArrayList();
                reply.writeInt(writeSysFs(fileNames, values) ? 1 : 0);
                reply.writeNoException();
                return true;
            case 6:
                String name = data.readString();
                Bundle bundle = new Bundle();
                bundle.readFromParcel(data);
                reply.writeInt(setBundle(name, bundle) ? 1 : 0);
                reply.writeNoException();
                return true;
            case 7:
                String name2 = data.readString();
                Bundle bundle2 = getBundle(name2);
                if (bundle2 == null) {
                    bundle2 = new Bundle();
                }
                bundle2.writeToParcel(reply, 0);
                reply.writeNoException();
                return true;
            case 8:
                ArrayList<String> fileNames2 = data.createStringArrayList();
                reply.writeInt(restoreSysFs(fileNames2) ? 1 : 0);
                reply.writeNoException();
                return true;
            case 9:
                String file = data.readString();
                int[] format = data.createIntArray();
                String[] outStrings = data.createStringArray();
                long[] outLongs = data.createLongArray();
                float[] outFloats = data.createFloatArray();
                reply.writeInt(readProcFile(file, format, outStrings, outLongs, outFloats) ? 1 : 0);
                if (outStrings != null && outStrings.length > 0) {
                    reply.writeStringArray(outStrings);
                }
                if (outLongs != null && outLongs.length > 0) {
                    reply.writeLongArray(outLongs);
                }
                if (outFloats != null && outFloats.length > 0) {
                    reply.writeFloatArray(outFloats);
                }
                reply.writeNoException();
                return true;
            case 10:
                String path = data.readString();
                String value = data.readString();
                reply.writeInt(writePath(path, value) ? 1 : 0);
                reply.writeNoException();
                return true;
            case 11:
                String path2 = data.readString();
                int bufferSize = data.readInt();
                ArrayList<String> result = readPath(path2, bufferSize);
                reply.writeStringList(result);
                reply.writeNoException();
                return true;
            case 12:
                int userId2 = data.readInt();
                String pkg2 = data.readString();
                String proc = data.readString();
                String reason3 = data.readString();
                boolean keepQuiet = data.readInt() == 1;
                long delay = data.readLong();
                int result2 = startProcess(userId2, pkg2, proc, reason3, keepQuiet, delay);
                reply.writeInt(result2);
                reply.writeNoException();
                return true;
            case 13:
                int pid2 = data.readInt();
                int uid = data.readInt();
                int flag = data.readInt();
                reply.writeInt(needKeepQuiet(pid2, uid, flag) ? 1 : 0);
                reply.writeNoException();
                return true;
            case 14:
                String pkg3 = data.readString();
                int userId3 = data.readInt();
                int uid2 = data.readInt();
                int flag2 = data.readInt();
                reply.writeInt(needKeepQuiet(pkg3, userId3, uid2, flag2) ? 1 : 0);
                reply.writeNoException();
                return true;
            case 15:
                int pid3 = data.readInt();
                int uid3 = data.readInt();
                powerFirst = data.readInt() == 1;
                setNeedKeepQuiet(pid3, uid3, powerFirst);
                reply.writeNoException();
                return true;
            case 16:
                String pkgName = data.readString();
                int userId4 = data.readInt();
                int uid4 = data.readInt();
                powerFirst = data.readInt() == 1;
                setNeedKeepQuiet(pkgName, userId4, uid4, powerFirst);
                reply.writeNoException();
                return true;
            case 17:
                ArrayList<String> pkgList = data.createStringArrayList();
                int userId5 = data.readInt();
                int mode = data.readInt();
                boolean proxy = data.readInt() == 1;
                String module = data.readString();
                proxyApp(pkgList, userId5, mode, proxy, module);
                return true;
            case 18:
                int pid4 = data.readInt();
                ActivityManager.RunningAppProcessInfo app = getRunningAppProcesses(pid4);
                app.writeToParcel(reply, 0);
                reply.writeNoException();
                return true;
            case 19:
                String pkgName2 = data.readString();
                int userId6 = data.readInt();
                String action = data.readString();
                int state = data.readInt();
                reply.writeInt(isBroadcastRegistered(pkgName2, userId6, action, state) ? 1 : 0);
                reply.writeNoException();
                return true;
            case 20:
                int pid5 = data.readInt();
                int cmd = data.readInt();
                ArgPack result3 = exeAppCmd(pid5, cmd, (ArgPack) ArgPack.CREATOR.createFromParcel(data));
                if (result3 == null) {
                    result3 = new ArgPack(new Object[0]);
                }
                result3.writeToParcel(reply, 0);
                return true;
            case 21:
                int[] pids2 = data.createIntArray();
                powerFirst = data.readInt() == 1;
                Debug.MemoryInfo[] mi = getProcessMemoryInfo(pids2, powerFirst);
                reply.writeNoException();
                reply.writeTypedArray(mi, 1);
                return true;
            case 22:
                String pkgName3 = data.readString();
                int uid5 = data.readInt();
                int mask = data.readInt();
                powerFirst = data.readInt() == 1;
                notifyWorkingState(pkgName3, uid5, mask, powerFirst);
                reply.writeNoException();
                return true;
            case 23:
                long handle = data.readLong();
                String sceneName = data.readString();
                String reason4 = data.readString();
                int fps = data.readInt();
                int priority = data.readInt();
                int duration = data.readInt();
                int caller = data.readInt();
                int client = data.readInt();
                int states = data.readInt();
                boolean dfps = data.readInt() == 1;
                int extra = data.readInt();
                acquireRefreshRate(handle, sceneName, reason4, fps, priority, duration, caller, client, states, dfps, extra);
                return true;
            case 24:
                int caller2 = data.readInt();
                long handle2 = data.readLong();
                releaseRefreshRate(caller2, handle2);
                return true;
            case 25:
                String name3 = data.readString();
                int priority2 = data.readInt();
                powerFirst = data.readInt() == 1;
                boolean createRefreshRateScene = createRefreshRateScene(name3, priority2, powerFirst);
                reply.writeNoException();
                reply.writeInt(createRefreshRateScene ? 1 : 0);
                return true;
            case 26:
                float result4 = getActiveRefreshRate();
                reply.writeNoException();
                reply.writeFloat(result4);
                return true;
            default:
                return super.onTransact(code, data, reply, flags);
        }
    }
}