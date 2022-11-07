package com.android.server;

import android.app.AlarmManager;
import android.app.IAlarmListener;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.app.admin.VivoPolicyManagerInternal;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.WorkSource;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.AlarmManagerService;
import com.android.server.VivoAlarmMgrServiceImpl;
import com.android.server.am.VivoAmsUtils;
import com.android.server.policy.VivoPolicyConstant;
import com.android.server.policy.key.VivoOTGKeyHandler;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import com.vivo.framework.pem.VivoStats;
import com.vivo.services.proxy.ProxyConfigs;
import com.vivo.services.rms.ProcessList;
import com.vivo.services.security.server.VivoPermissionUtils;
import com.vivo.services.superresolution.Constant;
import com.vivo.services.timezone.TZManagerService;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoAlarmMgrServiceImpl implements IVivoAlarmMgrService {
    public static boolean DEBUG_DUMP = false;
    public static final boolean IS_ENG;
    public static boolean IS_LOG_CTRL_OPEN = false;
    public static final int MAX_ALL_ALARM_COUNT = 2000;
    public static final int MAX_ALL_BATCH_COUNT = 1000;
    public static final int MAX_PER_BATCH_ALARM_COUNT = 100;
    public static final int MAX_PER_PKG_ALARM_COUNT = 800;
    public static final int MSG_CHECK_TIME_TICK_IS_TIMEOUT = 1000;
    public static final int MSG_DO_SOME_INIT = 1002;
    public static final String TAG = "AlarmManager";
    public static final String TAG_MORE_LOG = "AlarmManager_Log";
    static int mInterceptGMSalarm;
    private static VivoAlarmMgrServiceImpl mVivoAlarmMgrServiceImpl;
    static boolean sALARM_ALIGN_RUNING;
    final AlarmAlign mAlarmAlign;
    private Context mContext;
    DevicePolicyManager mDpm;
    private AlarmMainHandler mMainHandler;
    private AlarmManagerService mService;
    VivoPolicyManagerInternal mVivoPolicyManagerInternal;
    private long mLastTickTime = 0;
    private boolean misfromTickTimeTimeout = false;
    private SimpleDateFormat mSdformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
    public Map<String, ArrayList<AlarmManagerService.Alarm>> mAlarmBatchesFrozen = new HashMap();
    final ArrayList<String> ALARM_WHITELIST = new ArrayList<String>() { // from class: com.android.server.VivoAlarmMgrServiceImpl.1
        {
            add("com.android.BBKClock");
            add("com.android.BBKCrontab");
            add("com.vivo.space");
            add("com.android.mms");
            add(Constant.APP_GALLERY);
            add("com.vivo.widget.gallery");
            add("com.android.providers.calendar");
            add(VivoOTGKeyHandler.KEY_PACKAGE_SETTINGS);
            add("com.vivo.widget.calendar");
            add("com.android.deskclock");
            add("com.vivo.crontab");
            add("com.bbk.updater");
            add("com.vivo.hybrid");
            add("com.vivo.notes");
            add("com.vivo.cota");
            add("com.android.notes");
            add("com.vivo.nightpearl");
            add("com.vivo.findphone");
            add("com.vivo.timerwidget");
        }
    };
    private long mNextBootTimeByBBKClock = 0;
    private long mNextBootTimeByBBKCrontab = 0;
    private long mNextBootTimeBybsptest = 0;
    boolean mIsCustom = false;
    ArrayList<String> mCustomPkgs = new ArrayList<>();

    static {
        boolean z = true;
        boolean z2 = Build.TYPE.equals("eng") || Build.TYPE.equals("branddebug");
        IS_ENG = z2;
        if (!z2 && !SystemProperties.get("persist.sys.log.ctrl", "no").equals("yes")) {
            z = false;
        }
        IS_LOG_CTRL_OPEN = z;
        DEBUG_DUMP = SystemProperties.getBoolean("persist.debug.alarm.enable", false);
        sALARM_ALIGN_RUNING = false;
        mInterceptGMSalarm = -1;
    }

    public VivoAlarmMgrServiceImpl(AlarmManagerService service) {
        this.mService = service;
        AlarmAlign alarmAlign = new AlarmAlign();
        this.mAlarmAlign = alarmAlign;
        alarmAlign.init();
        logDebugInit();
        VSlog.i(TAG_MORE_LOG, "vivo alarm service impl.");
        mVivoAlarmMgrServiceImpl = this;
    }

    public static VivoAlarmMgrServiceImpl getInstance() {
        return mVivoAlarmMgrServiceImpl;
    }

    public void dummy() {
        VSlog.w(TAG_MORE_LOG, "alarm dummy test");
    }

    public void onStart(Context context, Looper looper) {
        VSlog.i(TAG, "alarm imp onStart..");
        this.mContext = context;
        AlarmMainHandler alarmMainHandler = new AlarmMainHandler(looper);
        this.mMainHandler = alarmMainHandler;
        Message msg = alarmMainHandler.obtainMessage(1002);
        this.mMainHandler.sendMessage(msg);
    }

    public int onSet(int type, long triggerAtTime, long windowLength, long interval, int flags, PendingIntent operation, int callingUid) {
        String morelog;
        if (getInterceptGMS() > 0 && checkAllowInterceptGmsAlarms(operation, callingUid)) {
            if (IS_ENG) {
                morelog = "operation:" + operation;
            } else {
                morelog = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            }
            if (AlarmManagerService.localLOGV) {
                VSlog.d(TAG, "not allow ExtraFeature to deliver alarms when cut net " + morelog);
                return -1;
            }
            return -1;
        }
        if (IS_LOG_CTRL_OPEN) {
            int callingPid = Binder.getCallingPid();
            if (callingPid == Process.myPid()) {
                VSlog.d(TAG_MORE_LOG, "beginSet type:" + type + " triggerAtTime:" + triggerAtTime + ",flags:" + flags + ",operation:" + operation + " windowLength:" + windowLength);
            } else {
                VSlog.d(TAG_MORE_LOG, "beginSet type:" + type + " triggerAtTime:" + triggerAtTime + ",flags:" + flags + ",operation:" + operation + " windowLength:" + windowLength + " PID:" + callingPid + " UID:" + callingUid);
            }
        }
        if (IS_LOG_CTRL_OPEN) {
            VSlog.d(TAG_MORE_LOG, triggerAtTime + " " + type);
        }
        return type;
    }

    public boolean onSetTime(long millis) {
        VSlog.i(TAG, "--setTime_setKernelTime millis:" + millis + " PID:" + Binder.getCallingPid() + " UID:" + Binder.getCallingUid());
        return true;
    }

    public void onSetTimeZone(String tz) {
        VSlog.i(TAG, "--setTimeZone " + tz + " PID:" + Binder.getCallingPid() + " UID:" + Binder.getCallingUid());
    }

    public boolean isSpecialApp(Intent intent, String pkg) {
        if (pkg == null || !this.ALARM_WHITELIST.contains(pkg)) {
            return false;
        }
        VSlog.i(TAG, " Donot_remove " + pkg + " alarm!");
        return true;
    }

    public boolean isAlarmPendingIntentCanceled(AlarmManagerService.Alarm alarm) {
        boolean cancel = VivoAmsUtils.isPendingIntentCanceled(alarm.operation);
        if (cancel) {
            VSlog.w(TAG_MORE_LOG, alarm + " pendingIntentLeak.");
        }
        return cancel;
    }

    public boolean isAlarmSuitableForCurrentBatch(AlarmManagerService.Alarm newAlarm, ArrayList<AlarmManagerService.Alarm> alarms) {
        if (newAlarm.operation != null) {
            String targetPackageName = newAlarm.operation.getTargetPackage();
            int targetPackgeAlarmCount = 0;
            if (targetPackageName != null && !targetPackageName.equals(VivoPermissionUtils.OS_PKG)) {
                Iterator<AlarmManagerService.Alarm> it = alarms.iterator();
                while (it.hasNext()) {
                    AlarmManagerService.Alarm alarm = it.next();
                    if (alarm.operation != null && targetPackageName.equals(alarm.operation.getTargetPackage()) && (targetPackgeAlarmCount = targetPackgeAlarmCount + 1) > 100) {
                        return false;
                    }
                }
                return true;
            }
            return true;
        }
        return true;
    }

    /* JADX WARN: Removed duplicated region for block: B:14:0x001e A[Catch: Exception -> 0x0098, TryCatch #0 {Exception -> 0x0098, blocks: (B:3:0x000a, B:14:0x001e, B:16:0x0063, B:15:0x004b), top: B:21:0x000a }] */
    /* JADX WARN: Removed duplicated region for block: B:15:0x004b A[Catch: Exception -> 0x0098, TryCatch #0 {Exception -> 0x0098, blocks: (B:3:0x000a, B:14:0x001e, B:16:0x0063, B:15:0x004b), top: B:21:0x000a }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private void printAlarmTriggerTime(int r17, long r18, long r20, android.app.PendingIntent r22) {
        /*
            r16 = this;
            r1 = r16
            r2 = r17
            r3 = r18
            r5 = r20
            java.lang.String r7 = "AlarmManager_Log"
            boolean r0 = com.android.server.VivoAlarmMgrServiceImpl.IS_LOG_CTRL_OPEN     // Catch: java.lang.Exception -> L98
            if (r0 == 0) goto L97
            r0 = 3
            if (r2 == r0) goto L17
            r0 = 2
            if (r2 != r0) goto L15
            goto L17
        L15:
            r0 = 0
            goto L18
        L17:
            r0 = 1
        L18:
            java.lang.String r8 = "unknow"
            java.lang.String r9 = ""
            if (r0 == 0) goto L4b
            java.text.SimpleDateFormat r10 = r1.mSdformat     // Catch: java.lang.Exception -> L98
            java.util.Date r11 = new java.util.Date     // Catch: java.lang.Exception -> L98
            long r12 = java.lang.System.currentTimeMillis()     // Catch: java.lang.Exception -> L98
            long r12 = r12 + r3
            long r14 = android.os.SystemClock.elapsedRealtime()     // Catch: java.lang.Exception -> L98
            long r12 = r12 - r14
            r11.<init>(r12)     // Catch: java.lang.Exception -> L98
            java.lang.String r10 = r10.format(r11)     // Catch: java.lang.Exception -> L98
            r8 = r10
            java.text.SimpleDateFormat r10 = r1.mSdformat     // Catch: java.lang.Exception -> L98
            java.util.Date r11 = new java.util.Date     // Catch: java.lang.Exception -> L98
            long r12 = java.lang.System.currentTimeMillis()     // Catch: java.lang.Exception -> L98
            long r12 = r12 + r5
            long r14 = android.os.SystemClock.elapsedRealtime()     // Catch: java.lang.Exception -> L98
            long r12 = r12 - r14
            r11.<init>(r12)     // Catch: java.lang.Exception -> L98
            java.lang.String r10 = r10.format(r11)     // Catch: java.lang.Exception -> L98
            r9 = r10
            goto L63
        L4b:
            java.text.SimpleDateFormat r10 = r1.mSdformat     // Catch: java.lang.Exception -> L98
            java.util.Date r11 = new java.util.Date     // Catch: java.lang.Exception -> L98
            r11.<init>(r3)     // Catch: java.lang.Exception -> L98
            java.lang.String r10 = r10.format(r11)     // Catch: java.lang.Exception -> L98
            r8 = r10
            java.text.SimpleDateFormat r10 = r1.mSdformat     // Catch: java.lang.Exception -> L98
            java.util.Date r11 = new java.util.Date     // Catch: java.lang.Exception -> L98
            r11.<init>(r5)     // Catch: java.lang.Exception -> L98
            java.lang.String r10 = r10.format(r11)     // Catch: java.lang.Exception -> L98
            r9 = r10
        L63:
            java.lang.StringBuilder r10 = new java.lang.StringBuilder     // Catch: java.lang.Exception -> L98
            r10.<init>()     // Catch: java.lang.Exception -> L98
            java.lang.String r11 = "triggerAtTime:"
            r10.append(r11)     // Catch: java.lang.Exception -> L98
            r10.append(r3)     // Catch: java.lang.Exception -> L98
            java.lang.String r11 = " triggerTimeToData:"
            r10.append(r11)     // Catch: java.lang.Exception -> L98
            r10.append(r8)     // Catch: java.lang.Exception -> L98
            java.lang.String r11 = " triggerElapsed:"
            r10.append(r11)     // Catch: java.lang.Exception -> L98
            r10.append(r5)     // Catch: java.lang.Exception -> L98
            java.lang.String r11 = " elapsedToData:"
            r10.append(r11)     // Catch: java.lang.Exception -> L98
            r10.append(r9)     // Catch: java.lang.Exception -> L98
            java.lang.String r11 = " type = "
            r10.append(r11)     // Catch: java.lang.Exception -> L98
            r10.append(r2)     // Catch: java.lang.Exception -> L98
            java.lang.String r10 = r10.toString()     // Catch: java.lang.Exception -> L98
            vivo.util.VSlog.d(r7, r10)     // Catch: java.lang.Exception -> L98
        L97:
            goto Lad
        L98:
            r0 = move-exception
            java.lang.StringBuilder r8 = new java.lang.StringBuilder
            r8.<init>()
            java.lang.String r9 = "format alarm time catch exception "
            r8.append(r9)
            r8.append(r0)
            java.lang.String r8 = r8.toString()
            vivo.util.VSlog.e(r7, r8)
        Lad:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.VivoAlarmMgrServiceImpl.printAlarmTriggerTime(int, long, long, android.app.PendingIntent):void");
    }

    public boolean isPackageCanAddMoreAlarm(PendingIntent operation, ArrayList<AlarmManagerService.Batch> alarmBatches, int type, long triggerAtTime, long triggerElapsed) {
        printAlarmTriggerTime(type, triggerAtTime, triggerElapsed, operation);
        int targetPackageAlarmCount = 0;
        if (operation != null) {
            String targetPackageName = operation.getTargetPackage();
            int currentAlarmCount = 0;
            if (targetPackageName != null && !targetPackageName.equals(VivoPermissionUtils.OS_PKG)) {
                Iterator<AlarmManagerService.Batch> it = alarmBatches.iterator();
                while (it.hasNext()) {
                    AlarmManagerService.Batch batch = it.next();
                    Iterator it2 = batch.alarms.iterator();
                    while (it2.hasNext()) {
                        AlarmManagerService.Alarm alarm = (AlarmManagerService.Alarm) it2.next();
                        currentAlarmCount++;
                        if (alarm.operation != null && targetPackageName.equals(alarm.operation.getTargetPackage())) {
                            targetPackageAlarmCount++;
                        }
                    }
                }
                if (IS_LOG_CTRL_OPEN || currentAlarmCount > 2000) {
                    VSlog.w(TAG_MORE_LOG, "currentAlarmCount :" + currentAlarmCount + " BatchSize:" + alarmBatches.size());
                }
            }
            if (targetPackageAlarmCount > 800) {
                VSlog.e(TAG_MORE_LOG, "!!! Package " + targetPackageName + " registered  too much Alarm. " + operation + " , Ingore it.\n");
                return false;
            }
            return true;
        }
        return true;
    }

    public boolean isNeedRebatchAllAlarmsLocked(ArrayList<AlarmManagerService.Batch> alarmBatches) {
        if (alarmBatches == null) {
            return true;
        }
        int currentAlarmCounts = getAllBatchsAlarmCountLocked(alarmBatches);
        if (currentAlarmCounts > 2000) {
            VSlog.e(TAG_MORE_LOG, "alarm numbers too much , do not rebatch. currentAlarmCounts:" + currentAlarmCounts + " mAlarmBatches.size():" + alarmBatches.size());
            return false;
        } else if (alarmBatches.size() <= 1000) {
            return true;
        } else {
            VSlog.e(TAG_MORE_LOG, "alarmBatch too much , do not rebatch. currentAlarmCounts:" + currentAlarmCounts + " mAlarmBatches.size():" + alarmBatches.size());
            return false;
        }
    }

    public int getAllBatchsAlarmCountLocked(ArrayList<AlarmManagerService.Batch> alarmBatches) {
        if (alarmBatches == null) {
            return 0;
        }
        int alarmCount = 0;
        Iterator<AlarmManagerService.Batch> it = alarmBatches.iterator();
        while (it.hasNext()) {
            AlarmManagerService.Batch batch = it.next();
            alarmCount += batch.size();
        }
        if (IS_LOG_CTRL_OPEN || alarmCount > 5000) {
            VSlog.d(TAG_MORE_LOG, "--currentAlarmCount :" + alarmCount + " BatchSize:" + alarmBatches.size());
            if (alarmCount > 2000 || alarmBatches.size() > 1000) {
                try {
                    dumpAllAlarmInfoLocked(alarmBatches);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return alarmCount;
    }

    public void dumpAllAlarmInfoLocked(ArrayList<AlarmManagerService.Batch> alarmBatches) {
        if (alarmBatches == null) {
            return;
        }
        HashMap<String, ArrayList<AlarmManagerService.Alarm>> alarmMap = new HashMap<>();
        Iterator<AlarmManagerService.Batch> it = alarmBatches.iterator();
        while (it.hasNext()) {
            AlarmManagerService.Batch batch = it.next();
            for (int i = 0; i < batch.size(); i++) {
                AlarmManagerService.Alarm alarm = batch.get(i);
                if (alarm != null && alarm.packageName != null) {
                    String packageName = alarm.packageName;
                    if (alarmMap.containsKey(packageName)) {
                        alarmMap.get(packageName).add(batch.get(i));
                    } else {
                        ArrayList<AlarmManagerService.Alarm> alarmList = new ArrayList<>();
                        alarmList.add(batch.get(i));
                        alarmMap.put(packageName, alarmList);
                    }
                }
            }
        }
        if (IS_LOG_CTRL_OPEN) {
            VSlog.w(TAG_MORE_LOG, "--beging dumpAlarmInfoForMoreLog. This will dump App, who set Alarm num exceed 100  alarmMapSize:" + alarmMap.size());
        }
        for (Map.Entry<String, ArrayList<AlarmManagerService.Alarm>> entry : alarmMap.entrySet()) {
            String alarmPackageName = entry.getKey();
            ArrayList<AlarmManagerService.Alarm> alarmListTemp = entry.getValue();
            if (alarmListTemp != null) {
                VSlog.w(TAG_MORE_LOG, "  alarmPackageName:" + alarmPackageName + " alarmSize:" + alarmListTemp.size());
            }
            if (IS_LOG_CTRL_OPEN && alarmListTemp != null && alarmListTemp.size() > 100) {
                int index = 1;
                Iterator<AlarmManagerService.Alarm> it2 = alarmListTemp.iterator();
                while (it2.hasNext()) {
                    AlarmManagerService.Alarm alarmTemp = it2.next();
                    if (index <= 10) {
                        StringBuilder sb = new StringBuilder();
                        int index2 = index + 1;
                        sb.append(index);
                        sb.append(" alarm:");
                        sb.append(alarmTemp);
                        sb.append(" pendIntent:");
                        sb.append(alarmTemp.operation == null ? "listener:" + alarmTemp.listenerTag : " operation:" + alarmTemp.operation.toString());
                        sb.append(" wakeup:");
                        sb.append(alarmTemp.wakeup);
                        sb.append(" count:");
                        sb.append(alarmTemp.count);
                        sb.append(" rInternal:");
                        sb.append(alarmTemp.repeatInterval);
                        sb.append(" uid:");
                        sb.append(alarmTemp.uid);
                        sb.append(" Elapsed:");
                        sb.append(alarmTemp.whenElapsed);
                        VSlog.w(TAG_MORE_LOG, sb.toString());
                        index = index2;
                    }
                }
            }
        }
        alarmMap.clear();
        VSlog.w(TAG_MORE_LOG, "--end dumpAlarmInfoForMoreLog alarmSize:" + alarmMap.size() + " \n");
    }

    public void setLastTickTime(long time) {
        this.mLastTickTime = time;
    }

    public long getLastTickTime() {
        return this.mLastTickTime;
    }

    public void setAlarm(int type, long millis, long alarmSeconds, long alarmNanoseconds) {
        if (AlarmManagerService.localLOGV) {
            VSlog.d(TAG, "setLocked " + type + "  " + alarmSeconds + "  " + alarmNanoseconds + " when:" + millis + ",toRTC:" + this.mSdformat.format(new Date((System.currentTimeMillis() + millis) - SystemClock.elapsedRealtime())));
        }
    }

    public void sendCheckTimeTickTimeOutMsg(boolean isfromTickTimeTimeout) {
        this.mMainHandler.removeMessages(1000);
        AlarmMainHandler alarmMainHandler = this.mMainHandler;
        alarmMainHandler.sendMessageDelayed(alarmMainHandler.obtainMessage(1000), isfromTickTimeTimeout ? 60000L : 90000L);
    }

    public void trySendTimeTickTimeOutBroadcast() {
        long now = System.currentTimeMillis();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(this.mLastTickTime);
        int oldSec = calendar.get(12);
        calendar.setTimeInMillis(now);
        int newSec = calendar.get(12);
        if (IS_LOG_CTRL_OPEN) {
            VSlog.d(TAG, "ClockReceiver sreen_on...oldSec:" + oldSec + ", newSec:" + newSec);
        }
        if (newSec != oldSec) {
            Intent tickIntent = new Intent("android.intent.action.TIME_TICK").addFlags(1476395008);
            this.mContext.sendBroadcastAsUser(tickIntent, UserHandle.ALL);
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss E");
            if (IS_LOG_CTRL_OPEN) {
                VSlog.v(TAG, "ClockReceiver sending ACTION_TIME_TICK time=" + format.format(new Date(System.currentTimeMillis())));
            }
        }
    }

    public void removeMessages(int what) {
        this.mMainHandler.removeMessages(what);
    }

    public void sendMessageDelayed(Message msg, long delayMillis) {
        this.mMainHandler.sendMessageDelayed(msg, delayMillis);
    }

    public void sendMessageDelayed(int what, long delayMillis) {
        Message message = this.mMainHandler.obtainMessage(what);
        message.arg1 = Binder.getCallingPid();
        message.arg2 = Binder.getCallingUid();
        this.mMainHandler.sendMessageDelayed(message, delayMillis);
    }

    public void onShellCommand(String[] args) {
        String cmd;
        try {
            StringBuilder sb = new StringBuilder();
            if (args != null && args.length > 0) {
                for (int j = 0; j < args.length; j++) {
                    sb.append(args[j] + " ");
                }
                cmd = sb.toString();
            } else {
                cmd = null;
            }
            int callingUid = Binder.getCallingUid();
            if (IS_ENG || IS_LOG_CTRL_OPEN) {
                VSlog.d(TAG, "ALMS onShellCommand CallingPid=" + Binder.getCallingPid() + " ,callingUid=" + callingUid + " ,PackageName=" + getPackageNameFromCallingUid(callingUid) + ".Starting command :" + cmd);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean onDump(PrintWriter pw, String[] args) {
        int callingUid = Binder.getCallingUid();
        String callerPkgName = getPackageNameFromCallingUid(callingUid);
        if (IS_ENG || IS_LOG_CTRL_OPEN) {
            VSlog.d(TAG, "ALMS dump CallingPid=" + Binder.getCallingPid() + " ,callingUid=" + callingUid + " ,callerPkgName=" + callerPkgName);
        }
        if (dynamicEnableAlarmLog(pw, args)) {
            VSlog.d(TAG, "ALMS dump dynamicEnableAlarmLog return");
            return false;
        } else if (callingUid != 2000 && callerPkgName != null && callerPkgName.toLowerCase().indexOf("cts") == -1 && !DEBUG_DUMP) {
            if (IS_ENG || IS_LOG_CTRL_OPEN) {
                VSlog.d(TAG, "ALMS dump return.");
            }
            return false;
        } else {
            return true;
        }
    }

    private String getPackageNameFromCallingUid(int callingUid) {
        try {
            String callerPkgName = this.mContext.getPackageManager().getNameForUid(callingUid);
            return callerPkgName;
        } catch (Exception e) {
            e.printStackTrace();
            return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        }
    }

    private boolean dynamicEnableAlarmLog(PrintWriter pw, String[] args) {
        String opt;
        int opti = 0;
        while (opti < args.length && (opt = args[opti]) != null && opt.length() > 0 && opt.charAt(0) == '-') {
            opti++;
            if ("-h".equals(opt)) {
                pw.println("alarm manager dump options:");
                pw.println("  log  [on/off]");
                pw.println("  Example:");
                pw.println("  adb shell dumpsys alarm log on");
                pw.println("  adb shell dumpsys alarm log off");
                pw.println("  adb shell dumpsys alarm --proto");
                return true;
            } else if (!"--proto".equals(opt)) {
                pw.println("Unknown argument: " + opt + "; use -h for help");
            }
        }
        if (opti < args.length) {
            String cmd = args[opti];
            int opti2 = opti + 1;
            if ("log".equals(cmd)) {
                configLogTag(pw, args, opti2);
                return true;
            }
        }
        return false;
    }

    private void configLogTag(PrintWriter pw, String[] args, int opti) {
        if (opti >= args.length) {
            pw.println("  Invalid argument!");
        } else if ("on".equals(args[opti])) {
            AlarmManagerService.localLOGV = true;
            AlarmManagerService.DEBUG_BATCH = true;
            AlarmManagerService.DEBUG_VALIDATE = true;
            DEBUG_DUMP = true;
            pw.println("DEBUG_DUMP " + DEBUG_DUMP + " DEBUG_BATCH:" + AlarmManagerService.DEBUG_BATCH);
        } else if ("off".equals(args[opti])) {
            AlarmManagerService.localLOGV = false;
            AlarmManagerService.DEBUG_BATCH = false;
            AlarmManagerService.DEBUG_VALIDATE = false;
            DEBUG_DUMP = false;
            pw.println("DEBUG_DUMP " + DEBUG_DUMP + " DEBUG_BATCH:" + AlarmManagerService.DEBUG_BATCH);
        } else if ("all".equals(args[opti])) {
            AlarmManagerService.localLOGV = true;
            AlarmManagerService.DEBUG_BATCH = true;
            AlarmManagerService.DEBUG_VALIDATE = true;
            DEBUG_DUMP = true;
            SystemProperties.set("persist.debug.alarm.enable", "true");
            pw.print("after reboot still enable. DEBUG_DUMP = " + DEBUG_DUMP);
        } else {
            pw.println("  Invalid argument!");
        }
    }

    public void clearPkgDataIsNeedRemoveALarm(Intent intent, SparseArray<ArrayMap<String, AlarmManagerService.BroadcastStats>> broadcastStats) {
        if (intent == null || intent.getData() == null) {
            return;
        }
        String clearPkg = intent.getData().getSchemeSpecificPart();
        if (isSpecialApp(intent, clearPkg)) {
            if (IS_LOG_CTRL_OPEN) {
                VSlog.v(TAG, "ACTION_PACKAGE_DATA_CLEARD pkg=" + clearPkg);
            }
            this.mService.removeLocked(clearPkg);
            for (int i = broadcastStats.size() - 1; i >= 0; i--) {
                ArrayMap<String, AlarmManagerService.BroadcastStats> uidStats = broadcastStats.valueAt(i);
                if (uidStats.remove(clearPkg) != null && uidStats.size() <= 0) {
                    broadcastStats.removeAt(i);
                }
            }
        }
    }

    public void dumpAlarmInfoByUidLocked(ArrayList<AlarmManagerService.Batch> alarmBatches, int Uid) {
        String packageName;
        if (alarmBatches == null) {
            return;
        }
        HashMap<String, ArrayList<AlarmManagerService.Alarm>> alarmMap = new HashMap<>();
        Iterator<AlarmManagerService.Batch> it = alarmBatches.iterator();
        while (it.hasNext()) {
            AlarmManagerService.Batch batch = it.next();
            for (int i = 0; i < batch.size(); i++) {
                AlarmManagerService.Alarm alarm = batch.get(i);
                if (alarm != null && alarm.uid == Uid && (packageName = alarm.packageName) != null) {
                    if (alarmMap.containsKey(packageName)) {
                        alarmMap.get(packageName).add(batch.get(i));
                    } else {
                        ArrayList<AlarmManagerService.Alarm> alarmList = new ArrayList<>();
                        alarmList.add(batch.get(i));
                        alarmMap.put(packageName, alarmList);
                    }
                }
            }
        }
        Slog.w(TAG, "This will dump App, who set Alarm num exceed 100 alarmMapSize:" + alarmMap.size());
        for (Map.Entry<String, ArrayList<AlarmManagerService.Alarm>> entry : alarmMap.entrySet()) {
            String alarmPackageName = entry.getKey();
            ArrayList<AlarmManagerService.Alarm> alarmListTemp = entry.getValue();
            if (alarmListTemp != null) {
                Slog.w(TAG, "  alarmPackageName:" + alarmPackageName + " alarmSize:" + alarmListTemp.size());
            }
            if (alarmListTemp != null && alarmListTemp.size() > 100) {
                int index = 1;
                Iterator<AlarmManagerService.Alarm> it2 = alarmListTemp.iterator();
                while (it2.hasNext()) {
                    AlarmManagerService.Alarm alarmTemp = it2.next();
                    if (index <= 20) {
                        StringBuilder sb = new StringBuilder();
                        int index2 = index + 1;
                        sb.append(index);
                        sb.append(" alarm:");
                        sb.append(alarmTemp);
                        sb.append(" pendIntent:");
                        sb.append(alarmTemp.operation == null ? "listener:" + alarmTemp.listenerTag : " operation:" + alarmTemp.operation.toString());
                        sb.append(" wakeup:");
                        sb.append(alarmTemp.wakeup);
                        sb.append(" count:");
                        sb.append(alarmTemp.count);
                        sb.append(" rInternal:");
                        sb.append(alarmTemp.repeatInterval);
                        sb.append(" uid:");
                        sb.append(alarmTemp.uid);
                        sb.append(" Elapsed:");
                        sb.append(alarmTemp.whenElapsed);
                        Slog.w(TAG, sb.toString());
                        index = index2;
                    }
                }
            }
        }
        alarmMap.clear();
    }

    public long onGetNextBootTime() {
        if (AlarmManagerService.localLOGV) {
            VSlog.d(TAG, "----mNextBootTimeByBBKClock:" + this.mNextBootTimeByBBKClock + " mNextBootTimeByBBKCrontab:" + this.mNextBootTimeByBBKCrontab + " mNextBootTimeBybsptest:" + this.mNextBootTimeBybsptest);
        }
        long j = this.mNextBootTimeByBBKClock;
        if (j <= 0) {
            return getMinTime(this.mNextBootTimeByBBKCrontab, this.mNextBootTimeBybsptest);
        }
        long j2 = this.mNextBootTimeByBBKCrontab;
        if (j2 <= 0) {
            return getMinTime(j, this.mNextBootTimeBybsptest);
        }
        long j3 = this.mNextBootTimeBybsptest;
        if (j3 <= 0) {
            return getMinTime(j2, j);
        }
        if (j <= 0 || j2 <= 0 || j2 <= 0) {
            return 0L;
        }
        return getMinTime(j, j2, j3);
    }

    private boolean isInOneMinite(long t) {
        return t - System.currentTimeMillis() <= 90000;
    }

    private long getMinTime(long i, long j) {
        if (i <= 0) {
            if (j > 0) {
                return j;
            }
            return 0L;
        } else if (j <= 0) {
            if (i > 0) {
                return i;
            }
            return 0L;
        } else if (i <= 0 || j <= 0) {
            return 0L;
        } else {
            if (i < j) {
                if (!isInOneMinite(i)) {
                    return i;
                }
                if (!isInOneMinite(j)) {
                    return j;
                }
                return i;
            } else if (!isInOneMinite(j)) {
                return j;
            } else {
                if (!isInOneMinite(i)) {
                    return i;
                }
                return j;
            }
        }
    }

    private long getMinTime(long a, long b, long c) {
        long min = a < b ? a : b;
        long min2 = min < c ? min : c;
        if (!isInOneMinite(min2)) {
            return min2;
        }
        if (min2 == a) {
            return getMinTime(b, c);
        }
        if (min2 == b) {
            return getMinTime(a, c);
        }
        if (min2 == c) {
            return getMinTime(a, b);
        }
        return min2;
    }

    public boolean onSetNextBootTime(long nextBootTime, String packageName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.SET_TIME", "setNextBootTime");
        if (AlarmManagerService.localLOGV) {
            VSlog.d(TAG, "nextBootTime:" + nextBootTime + " packageName:" + packageName);
        }
        if (packageName != null) {
            if ("com.android.BBKClock".equals(packageName)) {
                this.mNextBootTimeByBBKClock = nextBootTime;
                return true;
            } else if ("com.android.BBKCrontab".equals(packageName)) {
                this.mNextBootTimeByBBKCrontab = nextBootTime;
                return true;
            } else if ("com.vivo.bsptest".equals(packageName)) {
                this.mNextBootTimeBybsptest = nextBootTime;
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public boolean setInterceptGMS(int flag) {
        if (mInterceptGMSalarm != flag) {
            mInterceptGMSalarm = flag;
            VSlog.v(TAG, "change intercept GMS ALARM to " + flag);
            return true;
        }
        return false;
    }

    public int getInterceptGMS() {
        return mInterceptGMSalarm;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class AlarmMainHandler extends Handler {
        public AlarmMainHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1000:
                    long now = System.currentTimeMillis();
                    if (VivoAlarmMgrServiceImpl.IS_LOG_CTRL_OPEN) {
                        VSlog.d(VivoAlarmMgrServiceImpl.TAG, "MSG_CHECK_TIME_TICK_IS_TIMEOUT mLastTickTime:" + VivoAlarmMgrServiceImpl.this.mLastTickTime + ", now:" + now);
                    }
                    Intent tickIntent = new Intent("android.intent.action.TIME_TICK").addFlags(1476395008);
                    tickIntent.putExtra("isfromTickTimeTimeout", true);
                    VivoAlarmMgrServiceImpl.this.mContext.sendBroadcastAsUser(tickIntent, UserHandle.ALL);
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss E");
                    if (VivoAlarmMgrServiceImpl.IS_LOG_CTRL_OPEN) {
                        VSlog.d(VivoAlarmMgrServiceImpl.TAG, "MSG_TIME_TICK_TIMEOUT sending ACTION_TIME_TICK time=" + format.format(new Date(System.currentTimeMillis())));
                        return;
                    }
                    return;
                case 1001:
                    int pid = msg.arg1;
                    int uid = msg.arg2;
                    if (pid != Process.myPid()) {
                        Process.killProcess(pid);
                        if (VivoAlarmMgrServiceImpl.IS_LOG_CTRL_OPEN) {
                            VSlog.i(VivoAlarmMgrServiceImpl.TAG, "AlarmManager kill pid:" + pid + " ,uid:" + uid + " ,reason: do dump take too long time.");
                            return;
                        }
                        return;
                    }
                    return;
                case 1002:
                    new VivoLogReceiver();
                    return;
                default:
                    return;
            }
        }
    }

    /* loaded from: classes.dex */
    class VivoLogReceiver extends BroadcastReceiver {
        public VivoLogReceiver() {
            IntentFilter filter = new IntentFilter();
            filter.addAction(VivoPolicyConstant.ACTION_VIVO_LOG_CHANGED);
            filter.addAction("android.intent.action.TIME_TICK");
            VivoAlarmMgrServiceImpl.this.mContext.registerReceiver(this, filter);
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.TIME_TICK")) {
                boolean isfromTickTimeTimeout = intent.getBooleanExtra("isfromTickTimeTimeout", false);
                if (isfromTickTimeTimeout) {
                    if (AlarmManagerService.localLOGV) {
                        VSlog.v(VivoAlarmMgrServiceImpl.TAG, "Received TIME_TICK alarm from Timeout;");
                    }
                    VivoAlarmMgrServiceImpl.this.mLastTickTime = System.currentTimeMillis();
                    VivoAlarmMgrServiceImpl.this.sendCheckTimeTickTimeOutMsg(true);
                }
            } else if (intent.getAction().equals(VivoPolicyConstant.ACTION_VIVO_LOG_CHANGED)) {
                boolean logCtrl = SystemProperties.get("persist.sys.log.ctrl", "no").equals("yes");
                AlarmManagerService.localLOGV = logCtrl;
                AlarmManagerService.DEBUG_WAKELOCK = logCtrl;
                VivoAlarmMgrServiceImpl.IS_LOG_CTRL_OPEN = logCtrl;
                VSlog.d(VivoAlarmMgrServiceImpl.TAG, "VivoLogReceiver  log.ctrl:" + logCtrl + " printLog " + VivoAlarmMgrServiceImpl.IS_LOG_CTRL_OPEN);
            }
        }
    }

    private void logDebugInit() {
        AlarmManagerService.localLOGV = IS_LOG_CTRL_OPEN;
        AlarmManagerService.DEBUG_BATCH = DEBUG_DUMP;
        AlarmManagerService.DEBUG_VALIDATE = DEBUG_DUMP;
        AlarmManagerService.DEBUG_ALARM_CLOCK = AlarmManagerService.localLOGV;
        AlarmManagerService.DEBUG_LISTENER_CALLBACK = AlarmManagerService.localLOGV;
        AlarmManagerService.DEBUG_WAKELOCK = AlarmManagerService.localLOGV;
    }

    public int backupFrozenAppAlarm(final int uid, String packageName) {
        ArrayList<AlarmManagerService.Alarm> alarmList = new ArrayList<>();
        synchronized (this.mService.mLock) {
            new Predicate() { // from class: com.android.server.-$$Lambda$VivoAlarmMgrServiceImpl$Qrz9-bFbBrF9nbdTtQB6iJOdzMc
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return VivoAlarmMgrServiceImpl.lambda$backupFrozenAppAlarm$0(uid, (AlarmManagerService.Alarm) obj);
                }
            };
            for (int i = this.mService.mAlarmBatches.size() - 1; i >= 0; i--) {
                AlarmManagerService.Batch b = (AlarmManagerService.Batch) this.mService.mAlarmBatches.get(i);
                if (b.hasPackage(packageName)) {
                    for (int j = 0; j < b.size(); j++) {
                        AlarmManagerService.Alarm a = b.get(j);
                        if (a.matches(packageName) && a.uid == uid) {
                            if (alarmList.isEmpty() || !alarmList.contains(a)) {
                                alarmList.add(a);
                            }
                            if (AlarmManagerService.localLOGV) {
                                VLog.v(TAG, "backupFrozenAppAlarm pkg: " + packageName + ", ramove alarm: " + a);
                            }
                        }
                    }
                }
            }
            for (int i2 = this.mService.mPendingWhileIdleAlarms.size() - 1; i2 >= 0; i2--) {
                AlarmManagerService.Alarm a2 = (AlarmManagerService.Alarm) this.mService.mPendingWhileIdleAlarms.get(i2);
                if (a2.matches(packageName) && a2.uid == uid && (alarmList.isEmpty() || !alarmList.contains(a2))) {
                    alarmList.add(a2);
                }
            }
            for (int i3 = this.mService.mPendingBackgroundAlarms.size() - 1; i3 >= 0; i3--) {
                ArrayList<AlarmManagerService.Alarm> alarmsForUid = (ArrayList) this.mService.mPendingBackgroundAlarms.valueAt(i3);
                for (int j2 = alarmsForUid.size() - 1; j2 >= 0; j2--) {
                    AlarmManagerService.Alarm alarm = alarmsForUid.get(j2);
                    if (alarm.matches(packageName) && alarm.uid == uid && (alarmList.isEmpty() || !alarmList.contains(alarm))) {
                        alarmList.add(alarm);
                    }
                }
            }
            String key = packageName + "_" + UserHandle.getUserId(uid);
            this.mAlarmBatchesFrozen.put(key, alarmList);
            this.mService.removeLocked(uid);
        }
        return alarmList.size();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$backupFrozenAppAlarm$0(int uid, AlarmManagerService.Alarm a) {
        return a.uid == uid;
    }

    public int restoreFrozenAppAlarm(int uid, String packageName) {
        Object obj;
        String key;
        Object obj2;
        ArrayList<AlarmManagerService.Alarm> alarmList;
        String key2;
        ArrayList<AlarmManagerService.Alarm> alarmList2;
        int i;
        VivoAlarmMgrServiceImpl vivoAlarmMgrServiceImpl = this;
        String str = packageName;
        Object obj3 = vivoAlarmMgrServiceImpl.mService.mLock;
        synchronized (obj3) {
            try {
                try {
                    String key3 = str + "_" + UserHandle.getUserId(uid);
                    ArrayList<AlarmManagerService.Alarm> alarmList3 = vivoAlarmMgrServiceImpl.mAlarmBatchesFrozen.get(key3);
                    if (alarmList3.isEmpty()) {
                        key = key3;
                        obj2 = obj3;
                        alarmList = alarmList3;
                    } else {
                        try {
                            int i2 = alarmList3.size() - 1;
                            while (i2 >= 0) {
                                AlarmManagerService.Alarm alarm = alarmList3.get(i2);
                                if (alarm.whenElapsed <= SystemClock.elapsedRealtime() - 60000) {
                                    key2 = key3;
                                    obj = obj3;
                                    alarmList2 = alarmList3;
                                    i = i2;
                                } else {
                                    if (AlarmManagerService.localLOGV) {
                                        VLog.v(TAG, "restoreFrozenAppAlarm pkg: " + str + ", add alarm: " + alarm + ", curTime: " + SystemClock.elapsedRealtime());
                                    }
                                    obj = obj3;
                                    try {
                                        key2 = key3;
                                        alarmList2 = alarmList3;
                                        i = i2;
                                        vivoAlarmMgrServiceImpl.mService.setImplLocked(alarm.type, alarm.when, alarm.whenElapsed, alarm.windowLength, alarm.maxWhenElapsed, alarm.repeatInterval, alarm.operation, alarm.listener, alarm.listenerTag, alarm.flags, true, alarm.workSource, alarm.alarmClock, alarm.uid, alarm.packageName);
                                    } catch (Throwable th) {
                                        th = th;
                                        throw th;
                                    }
                                }
                                i2 = i - 1;
                                vivoAlarmMgrServiceImpl = this;
                                str = packageName;
                                obj3 = obj;
                                key3 = key2;
                                alarmList3 = alarmList2;
                            }
                            key = key3;
                            obj2 = obj3;
                            alarmList = alarmList3;
                        } catch (Throwable th2) {
                            th = th2;
                            obj = obj3;
                        }
                    }
                    this.mAlarmBatchesFrozen.remove(key);
                    int size = alarmList.size();
                    return size;
                } catch (Throwable th3) {
                    th = th3;
                    obj = obj3;
                }
            } catch (Throwable th4) {
                th = th4;
            }
        }
    }

    public void vivoRebatch() {
        if (sALARM_ALIGN_RUNING) {
            this.mAlarmAlign.rebatch();
        }
    }

    public void vivoRemove(PendingIntent operation, IAlarmListener directReceiver) {
        if (AlarmManagerService.localLOGV) {
            VSlog.d(TAG, "--remove operation:" + operation + " PID:" + Binder.getCallingPid() + " UID:" + Binder.getCallingUid());
        }
        if (sALARM_ALIGN_RUNING) {
            this.mAlarmAlign.remove(operation, directReceiver);
        }
    }

    public void vivoRemove(String packageName) {
        if (sALARM_ALIGN_RUNING) {
            this.mAlarmAlign.remove(packageName);
        }
    }

    public void vivoRemove(int userHandle) {
        if (sALARM_ALIGN_RUNING) {
            this.mAlarmAlign.remove(userHandle);
        }
    }

    public void vivoRemoveWithUid(int uid) {
        if (sALARM_ALIGN_RUNING) {
            String pkgName = getPackageNameFromCallingUid(uid);
            this.mAlarmAlign.remove(pkgName);
        }
    }

    public boolean vivoSetConfig(int type, PendingIntent operation, long nextNonWakeup, long nextWakeup, ArrayList<AlarmManagerService.Batch> alarmBatches) {
        if (type < 0 && this.mAlarmAlign.setConfig(type, operation, nextNonWakeup, nextWakeup, alarmBatches)) {
            return true;
        }
        return false;
    }

    public int vivoAddAlarm(int type, long when, long whenElapsed, PendingIntent operation, int flags, String callPkg, IAlarmListener directReceiver, String listenerTag, long nextNonWakeup, long nextWakeup) {
        return sALARM_ALIGN_RUNING ? this.mAlarmAlign.addAlarm(type, when, whenElapsed, operation, flags, callPkg, directReceiver, listenerTag, nextNonWakeup, nextWakeup) : flags;
    }

    public long vivoCheckSet(int type, long when, long nextNonWakeup, long nextWakeup) {
        if (sALARM_ALIGN_RUNING) {
            long when2 = this.mAlarmAlign.checkSet(type, when, nextNonWakeup, nextWakeup);
            if (when2 < 0) {
                Objects.requireNonNull(this.mAlarmAlign);
                VSlog.w("AlarmAlign", "Doont set to kernel");
            }
            if (this.mAlarmAlign.DEBUG) {
                Objects.requireNonNull(this.mAlarmAlign);
                VLog.v("AlarmAlign", "setLocked to kernel, type = " + type + ", when = " + when2 + ", curTime = " + SystemClock.elapsedRealtime());
                return when2;
            }
            return when2;
        }
        return when;
    }

    public void vivoTriggerAlarm(long curTime, ArrayList<AlarmManagerService.Alarm> triggerList, ArrayList<AlarmManagerService.Batch> alarmBatches) {
        if (sALARM_ALIGN_RUNING) {
            Objects.requireNonNull(this.mAlarmAlign);
            VLog.v("AlarmAlign", "mAlarmBatches = " + alarmBatches.size() + ", triggerList = " + triggerList.size());
            this.mAlarmAlign.triggerAlarm(curTime);
        }
    }

    public void vivoTriggerAlarmEnd(long curTime, boolean hasWakeup, int why, long nextNonWakeup, long nextWakeup) {
        if (sALARM_ALIGN_RUNING) {
            this.mAlarmAlign.triggerAlarmEnd(curTime, hasWakeup, why, nextNonWakeup, nextWakeup);
        }
    }

    public boolean checkAllowInterceptGmsAlarms(PendingIntent operation, int callingUid) {
        String sourcePackage;
        if (!this.mAlarmAlign.RUN_GMS || this.mContext == null) {
            return false;
        }
        if (operation != null && operation.isActivity()) {
            VSlog.d(TAG, "not allow to block starting foreground components");
            return false;
        }
        if (operation != null) {
            sourcePackage = operation.getCreatorPackage();
        } else {
            sourcePackage = this.mContext.getPackageManager().getPackagesForUid(callingUid)[0];
        }
        return isGMSCorePackage(sourcePackage);
    }

    public boolean isGMSCorePackage(String packageName) {
        return packageName != null && packageName.startsWith("com.google.android.gms");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class AlarmAlign {
        public AlarmManagerService.Batch mBatch;
        public long mNextTime;
        public AlarmManagerService.Batch mNwBatch;
        public final String TAG = "AlarmAlign";
        public final String TAG_CMD = ProxyConfigs.CTRL_MODULE_PEM;
        public final String TAG_TIME = "time";
        public final String TAG_TIME_EX = "ex_time";
        public final String TAG_A_DEBUG = "adebug";
        public final String TAG_SCREEN = "screen";
        public final String TAG_SIZE = "size";
        public final String TAG_PKG = "pkg";
        public final String TAG_ACTION = "action";
        private final String TAG_NW_SIZE = "nwsize";
        private final String TAG_NW_PKG = "nwpkg";
        private final String TAG_NW_ACTION = "nwaction";
        public final String TAG_RUN_GMS = "run_gms";
        public final int START = ProcessList.INVALID_ADJ;
        public final int STOP = -20000;
        public final int MSG_SCREEN_ON = -12305;
        public final int MSG_SCREEN_OFF = -12306;
        public boolean DEBUG = false;
        public long STEP = 300000;
        public long MEX = 300;
        public boolean RUN_GMS = false;
        public boolean SCREEN_ON = true;
        public String[] mPkgList = new String[0];
        public String[][] mActionList = (String[][]) Array.newInstance(String.class, 0, 0);
        public String[] mNwPkgList = new String[0];
        public String[][] mNwActionList = (String[][]) Array.newInstance(String.class, 0, 0);
        public long mAlign_Set = 0;
        public long mAlign_NonWake_Set = 0;
        public boolean mWake_Set = false;
        public long mNextWakeup_Set = 0;
        public long mNextNonWakeup_Set = 0;

        AlarmAlign() {
        }

        public void init() {
            AlarmManagerService alarmManagerService = VivoAlarmMgrServiceImpl.this.mService;
            Objects.requireNonNull(alarmManagerService);
            this.mBatch = new AlarmManagerService.Batch(alarmManagerService);
            AlarmManagerService alarmManagerService2 = VivoAlarmMgrServiceImpl.this.mService;
            Objects.requireNonNull(alarmManagerService2);
            this.mNwBatch = new AlarmManagerService.Batch(alarmManagerService2);
        }

        public void triggerAlarm(long curTime) {
            this.mWake_Set = this.SCREEN_ON;
            if (this.mBatch.end <= curTime) {
                long newEnd = Long.MAX_VALUE;
                Iterator<AlarmManagerService.Alarm> it = this.mBatch.alarms.iterator();
                while (it.hasNext()) {
                    AlarmManagerService.Alarm alarm = it.next();
                    if (alarm.maxWhenElapsed <= curTime) {
                        it.remove();
                        if (AlarmManagerService.localLOGV) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("triggerAlarm v MustWakeAlarm whenElapsed = ");
                            sb.append(alarm.whenElapsed);
                            sb.append(", pkg = ");
                            sb.append(alarm.operation == null ? alarm.packageName : alarm.operation.getTargetPackage());
                            sb.append(", maxWhenElapsed = ");
                            sb.append(alarm.maxWhenElapsed);
                            VLog.v("AlarmAlign", sb.toString());
                        }
                    } else if (alarm.maxWhenElapsed < newEnd) {
                        newEnd = alarm.maxWhenElapsed;
                    }
                }
                this.mBatch.end = newEnd;
            }
            if (this.mNwBatch.end <= curTime) {
                long newEnd2 = Long.MAX_VALUE;
                Iterator<AlarmManagerService.Alarm> it2 = this.mNwBatch.alarms.iterator();
                while (it2.hasNext()) {
                    AlarmManagerService.Alarm alarm2 = it2.next();
                    if (alarm2.maxWhenElapsed <= curTime) {
                        it2.remove();
                        if (AlarmManagerService.localLOGV) {
                            StringBuilder sb2 = new StringBuilder();
                            sb2.append("triggerAlarm v MustNonWakeAlarm whenElapsed = ");
                            sb2.append(alarm2.whenElapsed);
                            sb2.append(", pkg = ");
                            sb2.append(alarm2.operation == null ? alarm2.packageName : alarm2.operation.getTargetPackage());
                            sb2.append(", action = ");
                            sb2.append(alarm2.operation == null ? alarm2.listenerTag : alarm2.operation.getIntent().getAction());
                            sb2.append(", maxWhenElapsed = ");
                            sb2.append(alarm2.maxWhenElapsed);
                            VLog.v("AlarmAlign", sb2.toString());
                        }
                    } else if (alarm2.maxWhenElapsed < newEnd2) {
                        newEnd2 = alarm2.maxWhenElapsed;
                    }
                }
                this.mNwBatch.end = newEnd2;
            }
        }

        public void triggerAlarmEnd(long curTime, boolean hasWakeup, int why, long nextNonWakeup, long nextWakeup) {
            if (AlarmManagerService.localLOGV) {
                Objects.requireNonNull(VivoAlarmMgrServiceImpl.this.mAlarmAlign);
                VLog.v("AlarmAlign", "triggerAlarm v at " + curTime + ", hasWakeup=" + hasWakeup + ", mWake_Set=" + this.mWake_Set + ", nextWakeup=" + nextWakeup + ", nextNonWakeup=" + nextNonWakeup + ", mBatch.end=" + this.mBatch.end + ", why=" + why);
            }
            if (!this.mWake_Set && nextWakeup >= curTime) {
                VivoAlarmMgrServiceImpl.this.mService.setLocked(2, nextWakeup);
            }
        }

        public void rebatch() {
            if (AlarmManagerService.localLOGV) {
                VLog.v("AlarmAlign", "rebatch MustBatch");
            }
            AlarmManagerService alarmManagerService = VivoAlarmMgrServiceImpl.this.mService;
            Objects.requireNonNull(alarmManagerService);
            AlarmManagerService.Batch batch = new AlarmManagerService.Batch(alarmManagerService);
            for (int i = 0; i < this.mBatch.size(); i++) {
                AlarmManagerService.Alarm a = this.mBatch.get(i);
                long whenElapsed = VivoAlarmMgrServiceImpl.this.mService.convertToElapsed(a.when, a.type);
                AlarmManagerService.Alarm a2 = new AlarmManagerService.Alarm(a.type, a.when, whenElapsed, 0L, whenElapsed + this.MEX, 0L, a.operation, a.listener, a.listenerTag, (WorkSource) null, 0, (AlarmManager.AlarmClockInfo) null, 0, a.packageName);
                addToBatch(batch, a2);
            }
            this.mBatch = batch;
            if (AlarmManagerService.localLOGV) {
                logMustBatch();
            }
            AlarmManagerService alarmManagerService2 = VivoAlarmMgrServiceImpl.this.mService;
            Objects.requireNonNull(alarmManagerService2);
            AlarmManagerService.Batch nwBatch = new AlarmManagerService.Batch(alarmManagerService2);
            for (int i2 = 0; i2 < this.mNwBatch.size(); i2++) {
                AlarmManagerService.Alarm a3 = this.mNwBatch.get(i2);
                long whenElapsed2 = VivoAlarmMgrServiceImpl.this.mService.convertToElapsed(a3.when, a3.type);
                AlarmManagerService.Alarm a22 = new AlarmManagerService.Alarm(a3.type, a3.when, whenElapsed2, 0L, whenElapsed2, 0L, a3.operation, a3.listener, a3.listenerTag, (WorkSource) null, 0, (AlarmManager.AlarmClockInfo) null, 0, a3.packageName);
                addToBatch(nwBatch, a22);
            }
            this.mNwBatch = nwBatch;
            if (AlarmManagerService.localLOGV) {
                logMustNwBatch();
            }
        }

        public int addAlarm(int type, long when, long whenElapsed, PendingIntent operation, int flags, String callPkg, IAlarmListener directReceiver, String listenerTag, long nextNonWakeup, long nextWakeup) {
            long j;
            if (AlarmManagerService.localLOGV) {
                StringBuilder sb = new StringBuilder();
                sb.append("addAlarm pkg = ");
                sb.append(operation == null ? callPkg : operation.getTargetPackage());
                sb.append(", whenElapsed = ");
                sb.append(whenElapsed);
                sb.append(" tag= ");
                sb.append(listenerTag);
                VLog.v("AlarmAlign", sb.toString());
            }
            AlarmManagerService unused = VivoAlarmMgrServiceImpl.this.mService;
            if ((type & 1) == 0) {
                if (isFilter(operation, callPkg, listenerTag)) {
                    if (AlarmManagerService.localLOGV) {
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append("addAlarm whenElapsed = ");
                        j = whenElapsed;
                        sb2.append(j);
                        sb2.append(", pkg = ");
                        sb2.append(operation == null ? callPkg : operation.getTargetPackage());
                        VLog.v("AlarmAlign", sb2.toString());
                    } else {
                        j = whenElapsed;
                    }
                    AlarmManagerService.Alarm a = new AlarmManagerService.Alarm(type, when, whenElapsed, 0L, j + this.MEX, 0L, operation, directReceiver, listenerTag, (WorkSource) null, 0, (AlarmManager.AlarmClockInfo) null, 0, callPkg);
                    addToBatch(this.mBatch, a);
                    if (AlarmManagerService.localLOGV) {
                        logMustBatch();
                    }
                    if (this.mAlign_Set > this.mBatch.end && !this.SCREEN_ON) {
                        VivoAlarmMgrServiceImpl.this.mService.setLocked(2, nextWakeup);
                    }
                    return flags | 1;
                }
            } else if (isFilterNw(operation, callPkg, listenerTag)) {
                if (AlarmManagerService.localLOGV) {
                    VLog.v("AlarmAlign", "add to nonwakeup AlarmAlarm whenElapsed = " + whenElapsed);
                }
                AlarmManagerService.Alarm a2 = new AlarmManagerService.Alarm(type, when, whenElapsed, 0L, whenElapsed + this.MEX, 0L, operation, directReceiver, listenerTag, (WorkSource) null, 0, (AlarmManager.AlarmClockInfo) null, 0, callPkg);
                addToBatch(this.mNwBatch, a2);
                return flags | 1;
            }
            return flags;
        }

        void remove(final int userHandle) {
            if (AlarmManagerService.localLOGV) {
                VLog.v("AlarmAlign", "remove userHandle = " + userHandle);
            }
            Predicate<AlarmManagerService.Alarm> whichAlarms = new Predicate() { // from class: com.android.server.-$$Lambda$VivoAlarmMgrServiceImpl$AlarmAlign$cQQSUUvriiwxXSJP5Wr8bViLAF0
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return VivoAlarmMgrServiceImpl.AlarmAlign.lambda$remove$0(userHandle, (AlarmManagerService.Alarm) obj);
                }
            };
            boolean didRemove = this.mBatch.remove(whichAlarms, true);
            boolean didRemoveNw = this.mNwBatch.remove(whichAlarms, true);
            if (AlarmManagerService.localLOGV && didRemove) {
                logMustBatch();
            }
            if (!AlarmManagerService.localLOGV || !didRemoveNw) {
                return;
            }
            logMustNwBatch();
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ boolean lambda$remove$0(int userHandle, AlarmManagerService.Alarm a) {
            return UserHandle.getUserId(a.creatorUid) == userHandle;
        }

        void remove(final PendingIntent operation, final IAlarmListener directReceiver) {
            Predicate<AlarmManagerService.Alarm> whichAlarms = new Predicate() { // from class: com.android.server.-$$Lambda$VivoAlarmMgrServiceImpl$AlarmAlign$DdRt51X68DNP3QXKbbBpejSmdV0
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    boolean matches;
                    matches = ((AlarmManagerService.Alarm) obj).matches(operation, directReceiver);
                    return matches;
                }
            };
            boolean didRemove = this.mBatch.remove(whichAlarms, true);
            if (AlarmManagerService.localLOGV && didRemove) {
                logMustBatch();
            }
            boolean didRemoveNw = this.mNwBatch.remove(whichAlarms, true);
            if (!AlarmManagerService.localLOGV || !didRemoveNw) {
                return;
            }
            logMustNwBatch();
        }

        void logMustBatch() {
            long ident = Binder.clearCallingIdentity();
            int printSize = this.mBatch.size() <= 2 ? this.mBatch.size() : 2;
            for (int i = 0; i < printSize; i++) {
                AlarmManagerService.Alarm a = this.mBatch.get(i);
                if (AlarmManagerService.localLOGV) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("list mBatch[");
                    sb.append(i);
                    sb.append("] whenElapsed = ");
                    sb.append(a.whenElapsed);
                    sb.append(", maxWhenElapsed = ");
                    sb.append(a.maxWhenElapsed);
                    sb.append(", pkg = ");
                    sb.append(a.operation == null ? a.packageName : a.operation.getTargetPackage());
                    sb.append(", action = ");
                    sb.append(a.operation == null ? a.listenerTag : a.operation.getIntent().getAction());
                    VLog.v("AlarmAlign", sb.toString());
                }
            }
            Binder.restoreCallingIdentity(ident);
        }

        void logMustNwBatch() {
            int printSize = this.mNwBatch.size() <= 2 ? this.mNwBatch.size() : 2;
            for (int i = 0; i < printSize; i++) {
                AlarmManagerService.Alarm a = this.mNwBatch.get(i);
                if (AlarmManagerService.localLOGV) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("list mNwBatch[");
                    sb.append(i);
                    sb.append("] whenElapsed = ");
                    sb.append(a.whenElapsed);
                    sb.append(", maxWhenElapsed = ");
                    sb.append(a.maxWhenElapsed);
                    sb.append(", pkg = ");
                    sb.append(a.operation == null ? a.packageName : a.operation.getTargetPackage());
                    sb.append(", action = ");
                    sb.append(a.operation == null ? a.listenerTag : a.operation.getIntent().getAction());
                    VLog.v("AlarmAlign", sb.toString());
                }
            }
        }

        void remove(final String packageName) {
            if (AlarmManagerService.localLOGV) {
                VLog.v("AlarmAlign", "remove packageName = " + packageName);
            }
            Predicate<AlarmManagerService.Alarm> whichAlarms = new Predicate() { // from class: com.android.server.-$$Lambda$VivoAlarmMgrServiceImpl$AlarmAlign$g_qLxIycT4y3boFrHM5SAkSIgxk
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    boolean matches;
                    matches = ((AlarmManagerService.Alarm) obj).matches(packageName);
                    return matches;
                }
            };
            int i = 0;
            while (true) {
                String[] strArr = this.mPkgList;
                if (i >= strArr.length) {
                    break;
                }
                if (strArr[i].equals(packageName)) {
                    boolean didRemove = this.mBatch.remove(whichAlarms, true);
                    if (AlarmManagerService.localLOGV && didRemove) {
                        logMustBatch();
                    }
                }
                i++;
            }
            int i2 = 0;
            while (true) {
                String[] strArr2 = this.mNwPkgList;
                if (i2 < strArr2.length) {
                    if (strArr2[i2].equals(packageName)) {
                        boolean didRemoveNw = this.mNwBatch.remove(whichAlarms, true);
                        if (AlarmManagerService.localLOGV && didRemoveNw) {
                            logMustNwBatch();
                        }
                    }
                    i2++;
                } else {
                    return;
                }
            }
        }

        private boolean addToBatch(AlarmManagerService.Batch b, AlarmManagerService.Alarm alarm) {
            boolean newStart = false;
            ArrayList arrayList = b.alarms;
            AlarmManagerService unused = VivoAlarmMgrServiceImpl.this.mService;
            int index = Collections.binarySearch(arrayList, alarm, AlarmManagerService.sIncreasingTimeOrder);
            if (index < 0) {
                index = (0 - index) - 1;
            }
            b.alarms.add(index, alarm);
            if (alarm.whenElapsed > b.start) {
                b.start = alarm.whenElapsed;
                newStart = true;
            }
            if (alarm.maxWhenElapsed < b.end) {
                b.end = alarm.maxWhenElapsed;
            }
            return newStart;
        }

        public long checkSet(int type, long when, long nextNonWakeup, long nextWakeup) {
            long j;
            long when2 = when;
            if (4 == type) {
                if (this.DEBUG) {
                    VLog.v("AlarmAlign", "checkSet RTC_POWEROFF_WAKEUP, when = " + when2);
                }
                return when2;
            } else if (!this.SCREEN_ON) {
                AlarmManagerService unused = VivoAlarmMgrServiceImpl.this.mService;
                boolean noWake = (type & 1) != 0;
                if (noWake) {
                    if (this.mNwBatch.size() > 0) {
                        long when3 = this.mNwBatch.end;
                        if (this.mAlign_NonWake_Set == when3) {
                            return -111L;
                        }
                        this.mAlign_NonWake_Set = when3;
                        return when3;
                    }
                    when2 = nextWakeup;
                }
                long curTime = SystemClock.elapsedRealtime();
                long j2 = this.mNextTime;
                if (curTime >= j2) {
                    long j3 = this.STEP;
                    this.mNextTime = j2 + (j3 * (((int) ((curTime - j2) / j3)) + 1));
                }
                long j4 = this.mNextTime;
                if (when2 < j4) {
                    j = 0;
                } else {
                    long j5 = this.STEP;
                    j = (((int) ((when2 - j4) / j5)) + 1) * j5;
                }
                long setTime = j4 + j;
                if (this.mBatch.size() > 0 && this.mBatch.end < setTime && !noWake) {
                    if (AlarmManagerService.localLOGV) {
                        VLog.v("AlarmAlign", "checkSet OFF, hasMustWake = " + this.mBatch.end + ", setTime = " + setTime);
                    }
                    setTime = this.mBatch.end;
                }
                if (AlarmManagerService.localLOGV) {
                    VLog.v("AlarmAlign", "checkSet OFF, type=" + type + ", when=" + when2 + ", set=" + setTime + ", now=" + curTime + ", next=" + this.mNextTime + ", align=" + this.mAlign_Set + ", nonAlign=" + this.mAlign_NonWake_Set + ", nextWakeup=" + nextWakeup + ", nextNonWakeup=" + nextNonWakeup);
                }
                if (!noWake) {
                    this.mWake_Set = true;
                    if (this.mAlign_Set == setTime) {
                        return -111L;
                    }
                    this.mAlign_Set = setTime;
                } else if (this.mAlign_NonWake_Set == setTime) {
                    return -111L;
                } else {
                    this.mAlign_NonWake_Set = setTime;
                }
                return setTime;
            } else {
                if (AlarmManagerService.localLOGV) {
                    VLog.v("AlarmAlign", "checkSet SCREEN_ON, type = " + type + ", when = " + when2);
                }
                if (when2 < 0) {
                    return 0L;
                }
                return when2;
            }
        }

        public boolean isFilter(PendingIntent operation, String pkg, String action) {
            boolean ret = false;
            if (operation != null) {
                pkg = operation.getTargetPackage();
            }
            int i = 0;
            while (true) {
                String[] strArr = this.mPkgList;
                if (i < strArr.length) {
                    if (strArr[i].equals(pkg)) {
                        if (this.mActionList[i].length <= 0) {
                            ret = true;
                        }
                        if (operation != null) {
                            long ident = Binder.clearCallingIdentity();
                            action = operation.getIntent().getAction();
                            Binder.restoreCallingIdentity(ident);
                        }
                        if (action != null) {
                            int j = 0;
                            while (true) {
                                String[][] strArr2 = this.mActionList;
                                if (j >= strArr2[i].length) {
                                    break;
                                }
                                if (action.startsWith(strArr2[i][j])) {
                                    ret = true;
                                }
                                if (AlarmManagerService.localLOGV && ret) {
                                    VLog.v("AlarmAlign", "alarm white app list: action= " + this.mActionList[i][j]);
                                }
                                j++;
                            }
                        }
                    }
                    if (AlarmManagerService.localLOGV && ret) {
                        VLog.v("AlarmAlign", "alarm white app list---------- appName = " + this.mPkgList[i] + " ,ret: " + ret);
                    }
                    i++;
                } else {
                    return ret;
                }
            }
        }

        /* JADX WARN: Code restructure failed: missing block: B:30:0x0046, code lost:
            continue;
         */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
            To view partially-correct code enable 'Show inconsistent code' option in preferences
        */
        public boolean isFilterNw(android.app.PendingIntent r6, java.lang.String r7, java.lang.String r8) {
            /*
                r5 = this;
                if (r6 == 0) goto L6
                java.lang.String r7 = r6.getTargetPackage()
            L6:
                r0 = 0
            L7:
                java.lang.String[] r1 = r5.mNwPkgList
                int r2 = r1.length
                if (r0 >= r2) goto L49
                r1 = r1[r0]
                boolean r1 = r1.equals(r7)
                if (r1 == 0) goto L46
                java.lang.String[][] r1 = r5.mNwActionList
                r1 = r1[r0]
                int r1 = r1.length
                r2 = 1
                if (r1 > 0) goto L1d
                return r2
            L1d:
                if (r6 == 0) goto L2e
                long r3 = android.os.Binder.clearCallingIdentity()
                android.content.Intent r1 = r6.getIntent()
                java.lang.String r8 = r1.getAction()
                android.os.Binder.restoreCallingIdentity(r3)
            L2e:
                if (r8 == 0) goto L46
                r1 = 0
            L31:
                java.lang.String[][] r3 = r5.mNwActionList
                r4 = r3[r0]
                int r4 = r4.length
                if (r1 >= r4) goto L46
                r3 = r3[r0]
                r3 = r3[r1]
                boolean r3 = r8.equals(r3)
                if (r3 == 0) goto L43
                return r2
            L43:
                int r1 = r1 + 1
                goto L31
            L46:
                int r0 = r0 + 1
                goto L7
            L49:
                r0 = 0
                return r0
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.server.VivoAlarmMgrServiceImpl.AlarmAlign.isFilterNw(android.app.PendingIntent, java.lang.String, java.lang.String):boolean");
        }

        public boolean setConfig(int type, PendingIntent operation, long nextNonWakeup, long nextWakeup, ArrayList<AlarmManagerService.Batch> alarmBatches) {
            String[] pkglist;
            String str;
            long curTime;
            if (operation != null && ProxyConfigs.CTRL_MODULE_PEM.equals(operation.getTargetPackage())) {
                long curTime2 = SystemClock.elapsedRealtime();
                String str2 = "AlarmAlign";
                if (type == -20000) {
                    VivoAlarmMgrServiceImpl.sALARM_ALIGN_RUNING = false;
                    VivoAlarmMgrServiceImpl.this.mService.setLocked(2, nextWakeup);
                    VivoAlarmMgrServiceImpl.this.mService.setLocked(3, nextNonWakeup);
                    if (AlarmManagerService.localLOGV) {
                        VLog.v("AlarmAlign", "go to stop!");
                        return true;
                    }
                    return true;
                } else if (type != -10000) {
                    if (type == -12306) {
                        if (!AlarmManagerService.localLOGV) {
                            curTime = curTime2;
                        } else {
                            StringBuilder sb = new StringBuilder();
                            sb.append("go to SCREEN_OFF, curTime = ");
                            sb.append(curTime2);
                            sb.append(", mNextTime = ");
                            curTime = curTime2;
                            sb.append(this.mNextTime);
                            sb.append(", nextWakeup = ");
                            sb.append(nextWakeup);
                            sb.append(", nextNonWakeup = ");
                            sb.append(nextNonWakeup);
                            sb.append(", mNextTime = ");
                            sb.append(this.mNextTime);
                            VLog.v("AlarmAlign", sb.toString());
                        }
                        this.mAlign_Set = 0L;
                        this.mAlign_NonWake_Set = 0L;
                        this.SCREEN_ON = false;
                        return true;
                    } else if (type != -12305) {
                        return false;
                    } else {
                        this.SCREEN_ON = true;
                        if (AlarmManagerService.localLOGV) {
                            VLog.v("AlarmAlign", "go to SCREEN_ON, curTime = " + curTime2 + ", mAlign_Set = " + this.mAlign_Set + ", mAlign_NonWake_Set = " + this.mAlign_NonWake_Set + ", nextWakeup = " + nextWakeup + ", nextNonWakeup = " + nextNonWakeup + ", mNextTime = " + this.mNextTime);
                        }
                        if (this.mAlign_NonWake_Set > 0) {
                            VivoAlarmMgrServiceImpl.this.mService.setLocked(3, nextNonWakeup);
                        }
                        if (this.mAlign_Set > 0) {
                            VivoAlarmMgrServiceImpl.this.mService.setLocked(2, nextWakeup);
                        }
                        return true;
                    }
                } else {
                    long curTime3 = curTime2;
                    long ident = Binder.clearCallingIdentity();
                    Intent intent = operation.getIntent();
                    Binder.restoreCallingIdentity(ident);
                    this.STEP = intent.getLongExtra("time", 300000L);
                    this.MEX = intent.getLongExtra("ex_time", 3000L);
                    this.DEBUG = intent.getIntExtra("adebug", 0) > 0;
                    this.SCREEN_ON = intent.getBooleanExtra("screen", true);
                    this.RUN_GMS = intent.getBooleanExtra("run_gms", false);
                    String str3 = "size";
                    int size = intent.getIntExtra("size", 0);
                    String[] pkglist2 = new String[size];
                    String[][] actionList = new String[size];
                    if (AlarmManagerService.localLOGV) {
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append("START STEP = ");
                        pkglist = pkglist2;
                        sb2.append(this.STEP);
                        sb2.append(", MEX = ");
                        sb2.append(this.MEX);
                        sb2.append(", SCREEN_ON = ");
                        sb2.append(this.SCREEN_ON);
                        sb2.append(", mPkgList size = ");
                        sb2.append(size);
                        VLog.v("AlarmAlign", sb2.toString());
                    } else {
                        pkglist = pkglist2;
                    }
                    int i = 0;
                    while (true) {
                        String str4 = ", actionSize = ";
                        if (i < size) {
                            StringBuilder sb3 = new StringBuilder();
                            int size2 = size;
                            sb3.append("pkg");
                            sb3.append(i);
                            pkglist[i] = intent.getStringExtra(sb3.toString());
                            if (pkglist[i] == null) {
                                if (AlarmManagerService.localLOGV) {
                                    VLog.v("AlarmAlign", "error pkg list for index = " + i);
                                    return false;
                                }
                                return false;
                            }
                            int actionSize = intent.getIntExtra(str3 + i, 0);
                            actionList[i] = new String[actionSize];
                            if (AlarmManagerService.localLOGV) {
                                VLog.v("AlarmAlign", "START pkglist[" + i + "] = " + pkglist[i] + ", actionSize = " + actionList[i].length);
                            }
                            int k = 0;
                            while (k < actionSize) {
                                String[] strArr = actionList[i];
                                StringBuilder sb4 = new StringBuilder();
                                String str5 = str3;
                                sb4.append("action");
                                sb4.append(i);
                                sb4.append("-");
                                sb4.append(k);
                                strArr[k] = intent.getStringExtra(sb4.toString());
                                if (actionList[i][k] == null) {
                                    if (AlarmManagerService.localLOGV) {
                                        VLog.v("AlarmAlign", "error action list for pkg index = " + i + ", action index = " + k);
                                        return false;
                                    }
                                    return false;
                                }
                                if (AlarmManagerService.localLOGV) {
                                    VLog.v("AlarmAlign", "START actionList[" + i + "][" + k + "] = " + actionList[i][k]);
                                }
                                k++;
                                str3 = str5;
                            }
                            i++;
                            size = size2;
                        } else {
                            int size3 = size;
                            this.mActionList = actionList;
                            String[] pkglist3 = pkglist;
                            this.mPkgList = pkglist3;
                            String str6 = "nwsize";
                            int nwSize = intent.getIntExtra("nwsize", 0);
                            String[] nwPkgList = new String[nwSize];
                            String[][] nwActionList = new String[nwSize];
                            int i2 = 0;
                            while (i2 < nwSize) {
                                String[] pkglist4 = pkglist3;
                                StringBuilder sb5 = new StringBuilder();
                                int nwSize2 = nwSize;
                                sb5.append("nwpkg");
                                sb5.append(i2);
                                nwPkgList[i2] = intent.getStringExtra(sb5.toString());
                                if (nwPkgList[i2] == null) {
                                    if (AlarmManagerService.localLOGV) {
                                        VLog.v("AlarmAlign", "error nwpkg list for index = " + i2);
                                        return false;
                                    }
                                    return false;
                                }
                                int nwActionSize = intent.getIntExtra(str6 + i2, 0);
                                nwActionList[i2] = new String[nwActionSize];
                                if (AlarmManagerService.localLOGV) {
                                    StringBuilder sb6 = new StringBuilder();
                                    sb6.append("START pkglist[");
                                    sb6.append(i2);
                                    sb6.append("] = ");
                                    str = str6;
                                    sb6.append(nwPkgList[i2]);
                                    sb6.append(str4);
                                    sb6.append(nwActionList[i2].length);
                                    VLog.v("AlarmAlign", sb6.toString());
                                } else {
                                    str = str6;
                                }
                                int k2 = 0;
                                while (k2 < nwActionSize) {
                                    String[] strArr2 = nwActionList[i2];
                                    String str7 = str4;
                                    StringBuilder sb7 = new StringBuilder();
                                    int nwActionSize2 = nwActionSize;
                                    sb7.append("nwaction");
                                    sb7.append(i2);
                                    sb7.append("-");
                                    sb7.append(k2);
                                    strArr2[k2] = intent.getStringExtra(sb7.toString());
                                    if (nwActionList[i2][k2] != null) {
                                        if (AlarmManagerService.localLOGV) {
                                            VLog.v("AlarmAlign", "START nwactionList[" + i2 + "][" + k2 + "] = " + nwActionList[i2][k2]);
                                        }
                                        k2++;
                                        str4 = str7;
                                        nwActionSize = nwActionSize2;
                                    } else if (AlarmManagerService.localLOGV) {
                                        VLog.v("AlarmAlign", "error action list for nwpkg index = " + i2 + ", nwaction index = " + k2);
                                        return false;
                                    } else {
                                        return false;
                                    }
                                }
                                i2++;
                                pkglist3 = pkglist4;
                                nwSize = nwSize2;
                                str6 = str;
                            }
                            String[] pkglist5 = pkglist3;
                            int i3 = 0;
                            this.mNwActionList = nwActionList;
                            this.mNwPkgList = nwPkgList;
                            int f = 0;
                            while (f < alarmBatches.size()) {
                                AlarmManagerService.Batch b = alarmBatches.get(f);
                                int e = 0;
                                while (e < b.size()) {
                                    AlarmManagerService.Alarm a = b.get(e);
                                    String[] nwPkgList2 = nwPkgList;
                                    String str8 = str2;
                                    addAlarm(a.type, ((b.flags & 1) == 0 ? i3 : 10000) + a.when, ((b.flags & 1) == 0 ? i3 : 10000) + a.whenElapsed, a.operation, b.flags, a.packageName, a.listener, a.listenerTag, nextNonWakeup, nextWakeup);
                                    e++;
                                    pkglist5 = pkglist5;
                                    nwActionList = nwActionList;
                                    intent = intent;
                                    b = b;
                                    f = f;
                                    nwPkgList = nwPkgList2;
                                    i3 = i3;
                                    str2 = str8;
                                    size3 = size3;
                                    curTime3 = curTime3;
                                }
                                f++;
                                i3 = i3;
                                size3 = size3;
                                curTime3 = curTime3;
                            }
                            String str9 = str2;
                            this.mNextTime = curTime3 + this.STEP;
                            if (VivoStats.mSupportPemAlarm) {
                                VivoAlarmMgrServiceImpl.sALARM_ALIGN_RUNING = true;
                            }
                            if (AlarmManagerService.localLOGV) {
                                VLog.v(str9, "go to start, mNextTime = " + this.mNextTime + ", mSupportPemAlarm = " + VivoStats.mSupportPemAlarm + ", is RUN_GMS = " + this.RUN_GMS);
                            }
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    public boolean getSupportPemAlarm() {
        return VivoStats.mSupportPemAlarm;
    }

    public void initVivoCustom() {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        this.mDpm = devicePolicyManager;
        if (devicePolicyManager != null) {
            int type = devicePolicyManager.getCustomType();
            if (type > 0) {
                this.mIsCustom = true;
                ArrayList<String> arrayList = (ArrayList) this.mDpm.getCustomPkgs();
                this.mCustomPkgs = arrayList;
                if (IS_LOG_CTRL_OPEN && arrayList != null) {
                    VSlog.d(TAG_MORE_LOG, "mIsCustom:" + this.mIsCustom + " size:" + this.mCustomPkgs.size());
                }
            }
            this.mDpm.getRestrictionInfoList(null, 1509);
            VivoPolicyManagerInternal vivoPolicyManagerInternal = (VivoPolicyManagerInternal) LocalServices.getService(VivoPolicyManagerInternal.class);
            this.mVivoPolicyManagerInternal = vivoPolicyManagerInternal;
            if (vivoPolicyManagerInternal != null) {
                vivoPolicyManagerInternal.setVivoPolicyListener(new VivoPolicyManagerInternal.VivoPolicyListener() { // from class: com.android.server.VivoAlarmMgrServiceImpl.2
                    public void onVivoPolicyChanged(int poId) {
                        if (poId == 0 || poId == 101) {
                            VivoAlarmMgrServiceImpl.this.mDpm.getRestrictionInfoList(null, 1509);
                        }
                        if (poId == -1) {
                            VivoAlarmMgrServiceImpl vivoAlarmMgrServiceImpl = VivoAlarmMgrServiceImpl.this;
                            vivoAlarmMgrServiceImpl.mCustomPkgs = (ArrayList) vivoAlarmMgrServiceImpl.mDpm.getCustomPkgs();
                            if (VivoAlarmMgrServiceImpl.IS_LOG_CTRL_OPEN && VivoAlarmMgrServiceImpl.this.mCustomPkgs != null) {
                                VSlog.d(VivoAlarmMgrServiceImpl.TAG_MORE_LOG, "mIsCustom:" + VivoAlarmMgrServiceImpl.this.mIsCustom + " size:" + VivoAlarmMgrServiceImpl.this.mCustomPkgs.size());
                            }
                        }
                    }
                });
            }
        }
    }

    public void reportExceptionInfo(AlarmManagerService.Alarm alarm) {
        if (this.mIsCustom && this.mCustomPkgs != null && alarm.packageName != null && this.mCustomPkgs.contains(alarm.packageName)) {
            if (IS_LOG_CTRL_OPEN) {
                VSlog.d(TAG_MORE_LOG, "reportExceptionInfo alarm.packageName:" + alarm.packageName);
            }
            Bundle data = new Bundle();
            data.putString("package_name", alarm.packageName);
            DevicePolicyManager devicePolicyManager = this.mDpm;
            if (devicePolicyManager != null) {
                devicePolicyManager.reportExceptionInfo(4003, data);
            } else if (IS_LOG_CTRL_OPEN) {
                VSlog.d(TAG_MORE_LOG, "DevicePolicyManager==null.");
            }
        }
    }

    public boolean shouldInterceptNITZ() {
        TZManagerService tzManagerService = TZManagerService.getInstance(this.mContext);
        if (tzManagerService == null) {
            return false;
        }
        return tzManagerService.shouldInterceptNITZ();
    }

    public void setTimeZoneImpl(String tz) {
        this.mService.setTimeZoneImpl(tz);
    }
}