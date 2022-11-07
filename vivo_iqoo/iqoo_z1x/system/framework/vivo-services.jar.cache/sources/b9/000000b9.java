package com.android.server.am;

import android.content.Intent;
import android.os.SystemProperties;
import com.android.server.IVivoProcFrozenManager;
import com.android.server.am.frozen.FrozenInjectorImpl;
import java.util.ArrayList;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public final class ProcessFreezeManager implements IVivoProcFrozenManager {
    public static final String TAG = "ActivityManager";
    private static ProcessFreezeManager sManager;
    private ActivityManagerService mAm;
    private FrozenInjectorImpl mFrozenInjector;
    private VivoFrozenPackageSupervisor mVivoFrozenPackageSupervisor = VivoFrozenPackageSupervisor.getInstance();
    public static boolean DEBUG = SystemProperties.getBoolean("persist.sys.ams_frozen.debug", false);
    private static boolean isFrozenEnable = SystemProperties.get("persist.sys.quickfrozen.enable", "yes").equals("yes");

    public static ProcessFreezeManager getInstance(ActivityManagerService ams) {
        if (sManager == null) {
            sManager = new ProcessFreezeManager(ams);
        }
        return sManager;
    }

    ProcessFreezeManager(ActivityManagerService ams) {
        this.mFrozenInjector = null;
        this.mFrozenInjector = FrozenInjectorImpl.getInstance();
        this.mAm = ams;
    }

    public void addFreezeFlags(ProcessRecord r, int flags) {
        if (!isFrozenEnable) {
            return;
        }
        r.mFreezeFlags |= flags;
    }

    public void removeFreezeFlags(ProcessRecord r, int flags) {
        if (!isFrozenEnable) {
            return;
        }
        r.mFreezeFlags &= ~flags;
        if (flags == 1) {
            reportFrozenEventIfNeed(r, r.getCurProcState());
        }
    }

    public boolean hasFreezeFlags(ProcessRecord r, int flags) {
        return (r.mFreezeFlags & flags) != 0;
    }

    private boolean permitFreezeFromProcState(ProcessRecord app, int proState) {
        if ((proState < 7 && proState != 4 && proState != 5) || proState == 11 || (app.mFreezeFlags & 3) != 0 || app.inFullBackup) {
            return false;
        }
        return true;
    }

    public void reportFrozenEventIfNeed(ProcessRecord r, int newProcState) {
        if (!isFrozenEnable) {
            return;
        }
        if (!r.firstAllowFreeze && r.allowFreeze) {
            r.firstAllowFreeze = true;
        }
        if (r.frozen) {
            if (!permitFreezeFromProcState(r, newProcState) && checkProcState(r)) {
                VivoFrozenPackageSupervisor vivoFrozenPackageSupervisor = this.mVivoFrozenPackageSupervisor;
                vivoFrozenPackageSupervisor.isKeepFrozenProcess(r, true, "processState = " + newProcState);
                VSlog.d(TAG, "reportFrozenEventIfNeed to unfreeze  " + r);
                r.allowFreeze = false;
                this.mFrozenInjector.reportFreezeStatus(r.uid, r.processName, r.pid, false, newProcState);
            } else if (!r.allowFreeze) {
                r.allowFreeze = true;
                this.mFrozenInjector.reportFreezeStatus(r.uid, r.processName, r.pid, true, newProcState);
            }
        } else if (r.allowFreeze) {
            if (!permitFreezeFromProcState(r, newProcState)) {
                r.allowFreeze = false;
                if (DEBUG) {
                    VSlog.d(TAG, "ProcessFreezeManager set " + r + " freeze no ");
                }
                this.mFrozenInjector.reportFreezeStatus(r.uid, r.processName, r.pid, r.allowFreeze, newProcState);
            }
        } else if (permitFreezeFromProcState(r, newProcState) && !r.forbidFreezeForImportantClientBind) {
            r.allowFreeze = true;
            if (DEBUG) {
                VSlog.d(TAG, "ProcessFreezeManager set " + r + " freeze yes ");
            }
            this.mFrozenInjector.reportFreezeStatus(r.uid, r.processName, r.pid, r.allowFreeze, newProcState);
        }
    }

    private boolean checkProcState(ProcessRecord app) {
        String pkgName = (app == null || app.info == null) ? null : app.info.packageName;
        if (this.mFrozenInjector.isWallpaperService(pkgName) || this.mFrozenInjector.isCurrentInputMethod(pkgName)) {
            return false;
        }
        return true;
    }

    private void reportFreezeStatus(ProcessRecord r, boolean allowFreeze, String reason) {
        if (!allowFreeze && r.frozen) {
            this.mVivoFrozenPackageSupervisor.isKeepFrozenProcess(r, true, reason);
        }
        this.mFrozenInjector.reportFreezeStatus(r.uid, r.processName, r.pid, allowFreeze, r.getCurProcState());
    }

    public boolean checkSystemBind(Intent intent, int clientPid, ProcessRecord app, int flag) {
        if (isFrozenEnable && clientPid == ActivityManagerService.MY_PID && app != null && !app.isInFrozenBlackList && checkIntent(intent)) {
            app.forbidFreezeForImportantClientBind = true;
            addFreezeFlags(app, flag);
            return true;
        }
        return false;
    }

    private boolean checkIntent(Intent intent) {
        if (intent == null) {
            return true;
        }
        if (!"android.vivo.JobService".equals(intent.getAction()) && !"android.content.SyncAdapter".equals(intent.getAction())) {
            return true;
        }
        return false;
    }

    public void forbitFreezeByClient(ProcessRecord client, ProcessRecord server, int flag) {
        if (isFrozenEnable && server != null && !server.isInFrozenBlackList && !client.allowFreeze && !server.info.packageName.equals(client.info.packageName)) {
            server.forbidFreezeForImportantClientBind = true;
            addFreezeFlags(server, flag);
            if (server.allowFreeze) {
                if (DEBUG) {
                    VSlog.d(TAG, " set " + server + " freeze no for bind client =" + client + " flag = " + flag);
                }
                reportFreezeStatus(server, false, "client foreground to unfreeze process flag = " + flag);
            }
            server.allowFreeze = false;
        }
    }

    public boolean initTryUnFreeze(ProcessRecord app) {
        return isFrozenEnable && permitFreezeFromProcState(app, app.getCurProcState()) && !app.isInFrozenBlackList && app.forbidFreezeForImportantClientBind;
    }

    public boolean continueTryUnFreeze(boolean tryUnFreeze, ProcessRecord client, ProcessRecord server, int flag) {
        if (isFrozenEnable) {
            if (tryUnFreeze && !server.info.packageName.equals(client.info.packageName) && checkProcState(server)) {
                if (client.pid == ActivityManagerService.MY_PID) {
                    server.forbidFreezeForImportantClientBind = true;
                    addFreezeFlags(server, flag);
                    if (!server.allowFreeze) {
                        return false;
                    }
                    server.allowFreeze = false;
                    if (DEBUG) {
                        VSlog.d(TAG, " No need to unfreeze " + server + " beacause system_server bind flag = " + flag);
                    }
                    reportFreezeStatus(server, false, "client system_server bind flag = " + flag);
                    return false;
                } else if (!permitFreezeFromProcState(client, client.getCurProcState())) {
                    return false;
                } else {
                    return tryUnFreeze;
                }
            }
            return tryUnFreeze;
        }
        return false;
    }

    public void resetFrozenStatusLocked(ArrayList<ProcessRecord> list, boolean enableFrozen) {
        if (enableFrozen == isFrozenEnable) {
            return;
        }
        isFrozenEnable = enableFrozen;
        if (!enableFrozen) {
            return;
        }
        for (int i = list.size() - 1; i >= 0; i--) {
            ProcessRecord app = list.get(i);
            if (!app.isInFrozenBlackList) {
                app.mFreezeFlags = 0;
                app.allowFreeze = false;
                app.forbidFreezeForImportantClientBind = false;
                app.firstAllowFreeze = true;
            }
        }
    }

    public void TryUnFreezeIfNeed(boolean tryUnFreeze, ProcessRecord app) {
        if (isFrozenEnable && tryUnFreeze && !app.allowFreeze) {
            if (DEBUG) {
                VSlog.d(TAG, " set " + app + " UnFreeze yes for not bind");
            }
            app.forbidFreezeForImportantClientBind = false;
            app.allowFreeze = true;
            reportFreezeStatus(app, true, "no bind yet");
        }
    }

    public void reportFrozenEventIfNeed(ProcessRecord app, int prevProcState, int procState) {
        if (!isFrozenEnable) {
            return;
        }
        if ((prevProcState != procState || !app.firstAllowFreeze) && !app.isInFrozenBlackList) {
            reportFrozenEventIfNeed(app, procState);
        }
    }
}