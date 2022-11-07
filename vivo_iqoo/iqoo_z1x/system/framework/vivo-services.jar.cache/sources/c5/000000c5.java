package com.android.server.am;

import android.content.Intent;
import android.multidisplay.MultiDisplayManager;
import android.os.RemoteException;
import android.os.ShellCommand;
import android.os.UserHandle;
import com.vivo.face.common.data.Constants;
import java.io.PrintWriter;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoActivityManagerShellCommandImpl implements IVivoActivityManagerShellCommand {
    private long WAIT_TIME = 1000;

    public int runVcodeCommand(ShellCommand sc, PrintWriter pw) throws RemoteException {
        VcodeCommandImpl vcmd = new VcodeCommandImpl(sc);
        vcmd.runVcodeCommand(pw);
        return 0;
    }

    public boolean blockedActivityStarter(PrintWriter pw, final ActivityManagerService ams, Intent intent) {
        if (MultiDisplayManager.SUPPORT_CAR_NETWORKING) {
            String component = intent.getComponent() != null ? intent.getComponent().flattenToShortString() : Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            if (component == null || !component.equals("com.baidu.carlife/.CarlifeActivity")) {
                return false;
            }
            ams.mHandler.post(new Runnable() { // from class: com.android.server.am.VivoActivityManagerShellCommandImpl.1
                @Override // java.lang.Runnable
                public void run() {
                    VSlog.d("VCar", "start service for carnetworking");
                    Intent intent2 = new Intent();
                    intent2.setClassName("com.vivo.car.networking", "com.vivo.car.networking.base.monitor.CarlifeAdbService");
                    ams.mContext.startServiceAsUser(intent2, UserHandle.CURRENT);
                }
            });
            try {
                Thread.sleep(this.WAIT_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            pw.println("Warning: Activity not started, its current task has been brought to the front");
            pw.flush();
            return true;
        }
        return false;
    }
}