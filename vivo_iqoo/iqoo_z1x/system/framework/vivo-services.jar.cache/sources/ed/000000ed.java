package com.android.server.am;

import android.app.AppGlobals;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.hardware.graphics.common.V1_0.BufferUsage;
import android.os.Message;
import android.os.Process;
import android.os.SystemProperties;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.os.ProcessCpuTracker;
import com.android.server.VivoDoubleInstanceServiceImpl;
import com.android.server.am.firewall.VivoFirewall;
import com.android.server.am.frozen.FrozenQuicker;
import com.android.server.wm.VivoSoftwareLock;
import com.vivo.services.proxy.ProxyConfigs;
import com.vivo.services.rms.ProcessList;
import com.vivo.services.rms.display.SceneManager;
import com.vivo.services.rms.sdk.Consts;
import com.vivo.services.rms.sp.SpManagerImpl;
import com.vivo.services.security.server.VivoPermissionUtils;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import vivo.app.VivoFrameworkFactory;
import vivo.app.vperf.AbsVivoPerfManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoProcessListImpl implements IVivoProcessList {
    static final String TAG = "VivoProcessListImpl";
    public static AbsVivoPerfManager mPerfServiceStartHint;
    private static HashMap<String, ArrayList<Integer>> sPkgGidsPairs;
    private ProcessList mProcessList;
    private VivoDoubleInstanceServiceImpl mVivoDoubleInstanceService;
    private static ArrayList<String> DOUBLE_APP_PERMISSION_COMPONENTS = new ArrayList<>(Arrays.asList("com.android.permissioncontroller", "com.google.android.permissioncontroller"));
    public static final int[] OOM_ADJ_OVERRIDE = {0, 100, 200, ProcessList.BACKUP_APP_ADJ, 501, ProcessList.CACHED_APP_MIN_ADJ};
    public static final int[][] OOM_MINFREE_OVERRIDE = {new int[]{30000, 50000, FrozenQuicker.ONE_MIN, SceneManager.INTERACTION_PRIORITY, SceneManager.POWER_PRIORITY, SceneManager.ANIMATION_PRIORITY}, new int[]{40000, FrozenQuicker.ONE_MIN, SceneManager.INTERACTION_PRIORITY, SceneManager.POWER_PRIORITY, SceneManager.FIX_RATE_PRIORITY, 125000}, new int[]{50000, 65000, SceneManager.APP_REQUEST_PRIORITY, SceneManager.ANIMATION_PRIORITY, 120000, 150000}, new int[]{50000, SceneManager.INTERACTION_PRIORITY, SceneManager.POWER_PRIORITY, 110000, 140000, 175000}, new int[]{FrozenQuicker.ONE_MIN, SceneManager.APP_REQUEST_PRIORITY, SceneManager.ANIMATION_PRIORITY, 120000, 150000, 200000}, new int[]{FrozenQuicker.ONE_MIN, SceneManager.APP_REQUEST_PRIORITY, SceneManager.ANIMATION_PRIORITY, 120000, 150000, 200000}, new int[]{SceneManager.APP_REQUEST_PRIORITY, SceneManager.ANIMATION_PRIORITY, 120000, 150000, 200000, 250000}};
    private static final long[] OOM_MEMORY_INDEX_MAP = {BufferUsage.COMPOSER_OVERLAY, 3072, BufferUsage.COMPOSER_CLIENT_TARGET, 6144, 8192, 10240};

    static {
        HashMap<String, ArrayList<Integer>> hashMap = new HashMap<>();
        sPkgGidsPairs = hashMap;
        hashMap.put("com.vivo.abe", new ArrayList<>(Arrays.asList(3009)));
        sPkgGidsPairs.put(ProxyConfigs.CTRL_MODULE_PEM, new ArrayList<>(Arrays.asList(3009)));
        sPkgGidsPairs.put("com.vivo.epm", new ArrayList<>(Arrays.asList(3009)));
        sPkgGidsPairs.put("com.vivo.gamewatch", new ArrayList<>(Arrays.asList(3009)));
        sPkgGidsPairs.put("com.vivo.sps", new ArrayList<>(Arrays.asList(3009)));
        mPerfServiceStartHint = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoPerfManager((Context) null);
    }

    public VivoProcessListImpl(ProcessList processList) {
        this.mVivoDoubleInstanceService = null;
        if (processList == null) {
            VSlog.i(TAG, "container is " + processList);
        }
        this.mProcessList = processList;
        this.mVivoDoubleInstanceService = VivoDoubleInstanceServiceImpl.getInstance();
    }

    public boolean isDoubleAppUser(int userId) {
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        return vivoDoubleInstanceServiceImpl != null && vivoDoubleInstanceServiceImpl.getDoubleAppFixedUserId() == userId;
    }

    public boolean isNeedCrossUserAccess(int userId) {
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl;
        if ((userId == 0 || isDoubleAppUser(userId)) && (vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService) != null) {
            return vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable();
        }
        return false;
    }

    private boolean ensureCorrectProcess(int pid, int uid) {
        int puid = Process.getUidForPid(pid);
        if (puid != uid) {
            Slog.i(TAG, "The uid of process:" + pid + " is " + puid + " does not match uid " + uid);
            return false;
        }
        int tgid = Process.getThreadGroupLeader(pid);
        if (tgid == Process.myPid()) {
            Slog.i(TAG, "The process that wants to be killed is actually itself. (pid=" + uid + " tgid=" + tgid + ")");
            return false;
        }
        return true;
    }

    public void overrideRmsLmkParams(long memoryInMb, int reserve) {
        updateOomLevels(OOM_MINFREE_OVERRIDE[myMinFreeIndex(memoryInMb)], OOM_ADJ_OVERRIDE);
        SystemProperties.set("sys.sysctl.extra_free_kbytes", Integer.toString(reserve));
    }

    public void updateOomLevels(int[] minFrees, int[] oomAdjs) {
        if (minFrees == null || oomAdjs == null || minFrees.length != oomAdjs.length || minFrees.length <= 0) {
            VSlog.e("rms", "failed to updateOomLevels! fress:" + minFrees + " adjs:" + oomAdjs);
            return;
        }
        ByteBuffer buf = ByteBuffer.allocate(((oomAdjs.length * 2) + 1) * 4);
        buf.putInt(0);
        for (int i = 0; i < oomAdjs.length; i++) {
            buf.putInt(minFrees[i]);
            buf.putInt(oomAdjs[i]);
        }
        boolean ret = this.mProcessList.writeLmkdByRms(buf);
        if (!ret) {
            VSlog.e("rms", "failed to updateOomLevels!");
        }
    }

    public void restoreOomLevels(long memroyInMb) {
        updateOomLevels(OOM_MINFREE_OVERRIDE[myMinFreeIndex(memroyInMb)], OOM_ADJ_OVERRIDE);
    }

    public static int myMinFreeIndex(long memoryInMb) {
        int size = OOM_MEMORY_INDEX_MAP.length;
        for (int i = 0; i < size; i++) {
            if (memoryInMb < OOM_MEMORY_INDEX_MAP[i]) {
                return i;
            }
        }
        return size;
    }

    public int[] appendGids(ProcessRecord app, int[] gids) {
        ArrayList<Integer> toAppend;
        if (app != null && app.info != null && (toAppend = sPkgGidsPairs.get(app.info.packageName)) != null && toAppend.size() > 0) {
            int toAppendSize = toAppend.size();
            int oldSize = gids == null ? 0 : gids.length;
            int[] newGids = new int[oldSize + toAppendSize];
            if (oldSize > 0) {
                System.arraycopy(gids, 0, newGids, 0, oldSize);
            }
            for (int i = 0; i < toAppendSize; i++) {
                newGids[oldSize + i] = toAppend.get(i).intValue();
            }
            return newGids;
        }
        return gids;
    }

    public void handleActivityControllerTimeout() {
        int controllerPid = VivoSoftwareLock.getControllerPid();
        int controllerUid = VivoSoftwareLock.getControllerUid();
        VSlog.i(TAG, "ControllerPid: " + controllerPid + " ControllerUid:" + controllerUid);
        if (controllerPid <= 0) {
            return;
        }
        VSlog.i(TAG, "kill controller because of timeout : " + controllerPid);
        if (!ensureCorrectProcess(controllerPid, controllerUid)) {
            return;
        }
        ArrayList<Integer> pids = new ArrayList<>();
        pids.add(Integer.valueOf(controllerPid));
        ActivityManagerService.dumpStackTraces(pids, (ProcessCpuTracker) null, (SparseArray) null, (ArrayList) null, (StringWriter) null);
        Process.killProcess(controllerPid);
        VivoSoftwareLock.restoreControllerValue();
    }

    public void handleVivoActivityControllerTimeout() {
        int vivoControllerPid = VivoSoftwareLock.getVivoControllerPid();
        int vivoControllerUid = VivoSoftwareLock.getVivoControllerUid();
        VSlog.i(TAG, "VivoControllerPid: " + vivoControllerPid + " VivoControllerUid:" + vivoControllerUid);
        if (vivoControllerPid <= 0) {
            return;
        }
        VSlog.i(TAG, "kill vivo controller because of timeout : " + vivoControllerPid);
        if (!ensureCorrectProcess(vivoControllerPid, vivoControllerUid)) {
            return;
        }
        ArrayList<Integer> pids = new ArrayList<>();
        pids.add(Integer.valueOf(vivoControllerPid));
        ActivityManagerService.dumpStackTraces(pids, (ProcessCpuTracker) null, (SparseArray) null, (ArrayList) null, (StringWriter) null);
        Process.killProcess(vivoControllerPid);
        VivoSoftwareLock.restoreVivoControllerValue();
    }

    public void handleAmsDumpTimeout(Message msg) {
        int pid = msg.arg1;
        int uid = msg.arg2;
        if (pid != Process.myPid()) {
            Process.killProcess(pid);
            VSlog.i(TAG, "AMS kill pid:" + pid + " ,uid:" + uid + " ,reason: do dump take too long time.");
        }
    }

    public boolean isNeedsOptimizeAppCloneStorage(int userId, String packageName) {
        if (userId == 999) {
            boolean optimizeSuccess = SystemProperties.getBoolean("persist.vivo.optimize.state", true);
            return (optimizeSuccess || DOUBLE_APP_PERMISSION_COMPONENTS.contains(packageName)) ? false : true;
        }
        return false;
    }

    public void startProcessBoost(ProcessRecord app, HostingRecord hostingRecord, Process.ProcessStartResult startResult) {
        if (mPerfServiceStartHint != null && hostingRecord.getType() != null && hostingRecord.getType().equals(VivoFirewall.TYPE_ACTIVITY) && startResult != null) {
            mPerfServiceStartHint.perfHintAsync(4225, app.processName, startResult.pid, 101);
        }
    }

    public void startPersistentAppsForSps(int matchFlags) {
        if (!this.mProcessList.mService.mSpsReady) {
            this.mProcessList.mService.mSpsReady = true;
            return;
        }
        try {
            List<ApplicationInfo> apps = AppGlobals.getPackageManager().getPersistentApplications(matchFlags | Consts.ProcessStates.FOCUS | 128).getList();
            VSlog.i(TAG, "startPersistentAppsForSps apps:" + apps);
            for (final ApplicationInfo appInfo : apps) {
                if (!VivoPermissionUtils.OS_PKG.equals(appInfo.packageName) && !"com.vivo.sps".equals(appInfo.packageName) && appInfo.metaData != null && "com.vivo.sps".equals(appInfo.metaData.getString("vivo_process")) && SpManagerImpl.getInstance().canStartOnSuperProcess(appInfo.packageName, appInfo.uid)) {
                    VSlog.i(TAG, "startPersistentAppsForSps add PERSISTENT_SERVICE for:" + appInfo);
                    this.mProcessList.mService.mHandler.post(new Runnable() { // from class: com.android.server.am.VivoProcessListImpl.1
                        @Override // java.lang.Runnable
                        public void run() {
                            Intent intent = new Intent();
                            intent.setAction("com.sp.sdk.PERSISTENT_SERVICE");
                            intent.setPackage(appInfo.packageName);
                            try {
                                VivoProcessListImpl.this.mProcessList.mService.mContext.startService(intent);
                            } catch (Exception e) {
                                VSlog.w(VivoProcessListImpl.TAG, "start sps exp intent:" + intent, e);
                            }
                        }
                    });
                }
            }
        } catch (Exception ex) {
            VSlog.w(TAG, "start sps exp:", ex);
        }
    }
}