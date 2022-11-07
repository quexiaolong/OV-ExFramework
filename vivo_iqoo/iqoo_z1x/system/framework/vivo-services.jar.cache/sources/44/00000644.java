package com.vivo.services.engineerutile;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.graphics.common.V1_0.Dataspace;
import android.os.Handler;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Log;
import com.android.server.am.firewall.VivoFirewall;
import com.vivo.common.utils.VLog;
import java.util.List;
import vendor.vivoservices.x.vendor.factory.hardware.vivoem.V1_0.IEmCallback;
import vendor.vivoservices.x.vendor.factory.hardware.vivoem.V1_0.IVivoEm;
import vivo.app.engineerutile.IBBKEngineerUtileService;

/* loaded from: classes.dex */
public class BBKEngineerUtileService extends IBBKEngineerUtileService.Stub {
    private static final String TAG = "BBKEngineerUtileService";
    private Context mContext;
    private Handler mHandler = new Handler();

    public BBKEngineerUtileService(Context context) {
        this.mContext = null;
        Log.d(TAG, "BBKEngineerUtileService service start");
        this.mContext = context;
        new BBKEngineerPowerSave(context);
        startPcbaService();
        initIVivoEm();
    }

    public void sendBroadcastFromAtcid(final String name, final String action, final String extra) {
        this.mHandler.post(new Runnable() { // from class: com.vivo.services.engineerutile.BBKEngineerUtileService.1
            @Override // java.lang.Runnable
            public void run() {
                String[] strings;
                Intent intent = new Intent(action);
                intent.addFlags(Dataspace.TRANSFER_GAMMA2_2);
                intent.addFlags(67108864);
                String str = name;
                if (str != null && (strings = str.split("/")) != null && strings.length > 1 && strings[0] != null && strings[1] != null) {
                    if (!strings[1].contains(strings[0])) {
                        strings[1] = strings[0] + strings[1];
                    }
                    Log.d(BBKEngineerUtileService.TAG, "packageName: " + strings[0] + "/" + strings[1]);
                    intent.setClassName(strings[0], strings[1]);
                }
                String str2 = extra;
                if (str2 != null) {
                    String[] strings2 = str2.split(" ");
                    for (int i = 0; i + 2 < strings2.length; i += 3) {
                        if (strings2[i].equals("-e") || strings2[i].equals("--es")) {
                            intent.putExtra(strings2[i + 1], strings2[i + 2]);
                        } else if (strings2[i].equals("--ez")) {
                            intent.putExtra(strings2[i + 1], Boolean.parseBoolean(strings2[i + 2]));
                        } else if (strings2[i].equals("--ei")) {
                            intent.putExtra(strings2[i + 1], Integer.parseInt(strings2[i + 2]));
                        } else if (strings2[i].equals("--el")) {
                            intent.putExtra(strings2[i + 1], Long.parseLong(strings2[i + 2]));
                        }
                    }
                }
                Log.d(BBKEngineerUtileService.TAG, "sendBroadcastFromAtcid intent: " + intent.toString());
                BBKEngineerUtileService.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            }
        });
    }

    public void startActivityFromAtcid(String name, String action, String extra) {
        String[] strings;
        Intent intent = new Intent(action);
        if (name != null && (strings = name.split("/")) != null && strings.length > 1 && strings[0] != null && strings[1] != null) {
            if (!strings[1].contains(strings[0])) {
                strings[1] = strings[0] + strings[1];
            }
            Log.d(TAG, "packageName: " + strings[0] + "/" + strings[1]);
            intent.setClassName(strings[0], strings[1]);
        }
        int displayId = 0;
        if (extra != null) {
            String[] strings2 = extra.split(" ");
            for (int i = 0; i + 2 < strings2.length; i += 3) {
                if (strings2[i].equals("-e") || strings2[i].equals("--es")) {
                    intent.putExtra(strings2[i + 1], strings2[i + 2]);
                } else if (strings2[i].equals("--ez")) {
                    intent.putExtra(strings2[i + 1], Boolean.parseBoolean(strings2[i + 2]));
                } else if (strings2[i].equals("--ei")) {
                    intent.putExtra(strings2[i + 1], Integer.parseInt(strings2[i + 2]));
                    if ("display_id".equals(strings2[i + 1])) {
                        displayId = Integer.parseInt(strings2[i + 2]);
                    }
                } else if (strings2[i].equals("--el")) {
                    intent.putExtra(strings2[i + 1], Long.parseLong(strings2[i + 2]));
                }
            }
        }
        Log.d(TAG, "startActivityFromAtcid intent: " + intent.toString() + ", displayId: " + displayId);
        ActivityOptions options = ActivityOptions.makeBasic();
        options.setLaunchDisplayId(displayId);
        intent.setFlags(268435456);
        this.mContext.startActivity(intent, options.toBundle());
    }

    public void stopActivityFromAtcid(final String name, String action, String extra) {
        this.mHandler.post(new Runnable() { // from class: com.vivo.services.engineerutile.BBKEngineerUtileService.2
            @Override // java.lang.Runnable
            public void run() {
                ActivityManager am = (ActivityManager) BBKEngineerUtileService.this.mContext.getSystemService(VivoFirewall.TYPE_ACTIVITY);
                am.forceStopPackage(name);
            }
        });
    }

    public void startServiceFromAtcid(String name, String action, String extra) {
        String[] strings;
        Intent intent = new Intent(action);
        String packageName = null;
        if (name != null && (strings = name.split("/")) != null && strings.length > 1 && strings[0] != null && strings[1] != null) {
            if (!strings[1].contains(strings[0])) {
                strings[1] = strings[0] + strings[1];
            }
            Log.d(TAG, "setClassName: " + strings[0] + "/" + strings[1]);
            packageName = strings[0];
            intent.setClassName(strings[0], strings[1]);
        }
        if (extra != null) {
            String[] strings2 = extra.split(" ");
            for (int i = 0; i + 2 < strings2.length; i += 3) {
                if (strings2[i].equals("-e") || strings2[i].equals("--es")) {
                    intent.putExtra(strings2[i + 1], strings2[i + 2]);
                } else if (strings2[i].equals("--ez")) {
                    intent.putExtra(strings2[i + 1], Boolean.parseBoolean(strings2[i + 2]));
                } else if (strings2[i].equals("--ei")) {
                    intent.putExtra(strings2[i + 1], Integer.parseInt(strings2[i + 2]));
                } else if (strings2[i].equals("--el")) {
                    intent.putExtra(strings2[i + 1], Long.parseLong(strings2[i + 2]));
                }
            }
        }
        if (packageName != null) {
            try {
                PackageManager pm = this.mContext.getPackageManager();
                ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 1);
                Log.d(TAG, "startServiceFromAtcid, uid: " + appInfo.uid + ", intent: " + intent.toString());
                if (1000 == appInfo.uid) {
                    this.mContext.startService(intent);
                } else {
                    this.mContext.startForegroundService(intent);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void stopServiceFromAtcid(final String name, final String action, final String extra) {
        this.mHandler.post(new Runnable() { // from class: com.vivo.services.engineerutile.BBKEngineerUtileService.3
            @Override // java.lang.Runnable
            public void run() {
                String[] strings;
                Intent intent = new Intent(action);
                String str = name;
                if (str != null && (strings = str.split("/")) != null && strings.length > 1 && strings[0] != null && strings[1] != null) {
                    if (!strings[1].contains(strings[0])) {
                        strings[1] = strings[0] + strings[1];
                    }
                    Log.d(BBKEngineerUtileService.TAG, "packageName: " + strings[0] + "/" + strings[1]);
                    intent.setClassName(strings[0], strings[1]);
                }
                String str2 = extra;
                if (str2 != null) {
                    String[] strings2 = str2.split(" ");
                    for (int i = 0; i + 2 < strings2.length; i += 3) {
                        if (strings2[i].equals("-e") || strings2[i].equals("--es")) {
                            intent.putExtra(strings2[i + 1], strings2[i + 2]);
                        } else if (strings2[i].equals("--ez")) {
                            intent.putExtra(strings2[i + 1], Boolean.parseBoolean(strings2[i + 2]));
                        } else if (strings2[i].equals("--ei")) {
                            intent.putExtra(strings2[i + 1], Integer.parseInt(strings2[i + 2]));
                        } else if (strings2[i].equals("--el")) {
                            intent.putExtra(strings2[i + 1], Long.parseLong(strings2[i + 2]));
                        }
                    }
                }
                Log.d(BBKEngineerUtileService.TAG, "stopServiceFromAtcid intent: " + intent.toString());
                BBKEngineerUtileService.this.mContext.stopService(intent);
            }
        });
    }

    public int checkCameraDeviceConnect() {
        VLog.v(TAG, "not support!");
        return 0;
    }

    public int isSetupwizardDisabled() {
        VLog.v(TAG, "not support!");
        return 0;
    }

    public int isServiceWork(String serviceName) {
        ActivityManager mAM = (ActivityManager) this.mContext.getSystemService(VivoFirewall.TYPE_ACTIVITY);
        List<ActivityManager.RunningServiceInfo> mList = mAM.getRunningServices(200);
        if (mList == null) {
            Log.v(TAG, "service list null");
            return 0;
        }
        Log.v(TAG, "List.size():" + mList.size());
        for (int i = mList.size() - 1; i >= 0; i--) {
            if (serviceName.equals(mList.get(i).service.getClassName())) {
                Log.v(TAG, "position:" + i);
                return 1;
            }
        }
        return 0;
    }

    private void startPcbaService() {
        String mLock = SystemProperties.get("ro.pcba.control", "1");
        if ("1".equals(mLock)) {
            return;
        }
        Log.v(TAG, "start PCBAFloatView");
        new PCBAFloatView(this.mContext);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class EmCallback extends IEmCallback.Stub {
        EmCallback() {
        }

        @Override // vendor.vivoservices.x.vendor.factory.hardware.vivoem.V1_0.IEmCallback
        public boolean callbackToClient(String data, String name, String action, String extra) {
            if (data == null) {
                return false;
            }
            try {
                Log.d(BBKEngineerUtileService.TAG, "callbackToClient:" + data);
                if ("sendBroadcastFromAtcid".equals(data)) {
                    BBKEngineerUtileService.this.sendBroadcastFromAtcid(name, action, extra);
                } else if ("startActivityFromAtcid".equals(data)) {
                    BBKEngineerUtileService.this.startActivityFromAtcid(name, action, extra);
                } else if ("stopActivityFromAtcid".equals(data)) {
                    BBKEngineerUtileService.this.stopActivityFromAtcid(name, action, extra);
                } else if ("startServiceFromAtcid".equals(data)) {
                    BBKEngineerUtileService.this.startServiceFromAtcid(name, action, extra);
                } else if ("stopServiceFromAtcid".equals(data)) {
                    BBKEngineerUtileService.this.stopServiceFromAtcid(name, action, extra);
                } else if ("isServiceWork".equals(data)) {
                    int work = BBKEngineerUtileService.this.isServiceWork(name);
                    return work == 1;
                } else {
                    return false;
                }
                return true;
            } catch (Exception ex) {
                Log.e(BBKEngineerUtileService.TAG, "callbackToClient Exception", ex);
                return false;
            }
        }

        @Override // vendor.vivoservices.x.vendor.factory.hardware.vivoem.V1_0.IEmCallback
        public String callbackEngineerMode(String data) {
            if (data == null) {
                return null;
            }
            Log.d(BBKEngineerUtileService.TAG, "callbackEngineerMode:" + data);
            return data;
        }
    }

    private void initIVivoEm() {
        try {
            IVivoEm mIVivoEm = IVivoEm.getService();
            if (mIVivoEm == null) {
                Log.e(TAG, "IVivoEm is null");
            } else {
                EmCallback mIEmCallback = new EmCallback();
                mIVivoEm.setServerCallback(mIEmCallback, TAG);
            }
        } catch (Exception e) {
            Log.e(TAG, "initIVivoEm Exception", e);
        }
    }
}