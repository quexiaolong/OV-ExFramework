package com.android.server.wm;

import android.app.ActivityTaskManager;
import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import com.android.server.am.VivoFrozenPackageSupervisor;
import com.vivo.appshare.AppShareConfig;
import java.util.ArrayList;
import vivo.app.VivoFrameworkFactory;
import vivo.app.vperf.AbsVivoPerfManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoRecentTasksImpl implements IVivoRecentTasks {
    static final String TAG = "VivoRecentTasksImpl";
    static final int mMaxOldTwinStackNum = 9;
    private final AbsVivoPerfManager mUxPerf = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoPerfManager((Context) null);
    final ArrayList<String> NON_TRIMMABLE_WHITELIST = new ArrayList<String>() { // from class: com.android.server.wm.VivoRecentTasksImpl.1
        {
            add("com.vivo.simplelauncher");
            add("com.android.incallui");
            add("com.vivo.bsptest");
        }
    };
    private VivoFrozenPackageSupervisor mVivoFrozenPackageSupervisor = VivoFrozenPackageSupervisor.getInstance();

    public void dummy() {
        VSlog.i(TAG, "dummy, this=" + this);
    }

    public boolean containsFrozenActivities(Task tr) {
        if (tr != null && tr.getParent() != null) {
            for (int i = tr.getChildCount() - 1; i >= 0; i--) {
                ActivityRecord r = tr.getChildAt(i).asActivityRecord();
                if (r != null && r.app != null && r.packageName != null) {
                    int uid = r.app.mInfo != null ? r.app.mInfo.uid : -1;
                    if (this.mVivoFrozenPackageSupervisor.isFrozenPackage(r.packageName, uid)) {
                        VSlog.d(TAG, "find a frozen activity: " + r);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isTrimmableIfNeed(Task task) {
        if (task.realActivity == null || !this.NON_TRIMMABLE_WHITELIST.contains(task.realActivity.getPackageName())) {
            return (AppShareConfig.SUPPROT_APPSHARE && task.realActivity != null && AppShareConfig.APP_SHARE_ACTIVITY_SHORT_STRING.equals(task.realActivity.flattenToShortString())) ? false : true;
        }
        VSlog.i(TAG, "Prevent task: " + task + " from removing by onRecentTaskRemoved");
        return false;
    }

    public void removeOldTwinStacks(Task removedTask, Task newTaskRecord) {
        ActivityStack removedStack;
        if (removedTask != null && newTaskRecord != null && removedTask.mAtmService.mAmInternal.isInActivityNumControl() && removedTask.getStack() != newTaskRecord.getStack() && (removedStack = removedTask.getStack()) != null) {
            removedStack.getDisplay();
            TaskDisplayArea taskDisplayArea = removedTask.getDisplayArea();
            if (taskDisplayArea != null && taskDisplayArea.getStackCount() > ActivityTaskManager.getMaxRecentTasksStatic()) {
                int activityCount = removedTask.getChildCount();
                ActivityRecord topActivityInRemovedTask = activityCount >= 1 ? removedTask.getChildAt(activityCount - 1).asActivityRecord() : null;
                VSlog.i(TAG, "removeOldTwinStacks topActivityInRemovedTask=" + topActivityInRemovedTask + ",taskDisplayArea.getStackCount=" + taskDisplayArea.getStackCount());
                if (topActivityInRemovedTask == null) {
                    return;
                }
                ComponentName topActivityComponentNameOfRemovedTask = topActivityInRemovedTask.mActivityComponent;
                int userIdofRemovedTask = topActivityInRemovedTask.mUserId;
                ArrayList<ActivityStack> activityStacks = new ArrayList<>();
                for (int i = taskDisplayArea.getIndexOf(removedStack); i >= 0; i--) {
                    ActivityStack activityStack = taskDisplayArea.getChildAt(i);
                    if (activityStack.getWindowingMode() != removedStack.getWindowingMode()) {
                        VSlog.d(TAG, "windowmode of removedStack: " + removedStack.getWindowingMode() + " windowmode of me:" + activityStack.getWindowingMode());
                    } else {
                        ActivityRecord topActivityRecord = activityStack.getTopActivity(true, true);
                        if (topActivityRecord != null && topActivityRecord.getTask() != null && !topActivityRecord.getTask().inRecents && topActivityRecord.mUserId == userIdofRemovedTask && topActivityRecord.mActivityComponent != null && topActivityRecord.mActivityComponent.equals(topActivityComponentNameOfRemovedTask)) {
                            activityStacks.add(activityStack);
                        }
                    }
                }
                int i2 = activityStacks.size();
                if (i2 > 9) {
                    for (int i3 = activityStacks.size() - 1; i3 >= 9; i3 += -1) {
                        VSlog.d(TAG, "finish stack " + activityStacks.get(i3).getRootTaskId() + " because there are too many activity:" + topActivityComponentNameOfRemovedTask);
                        activityStacks.get(i3).finishAllActivitiesImmediately();
                    }
                }
            }
        }
    }

    public boolean isSimilarForInvalidTaskInSplit(ActivityTaskManagerService mService, Task task, Task other, String reason) {
        if (mService != null && mService.isMultiWindowSupport() && task != null && other != null && reason != null) {
            if (mService.isInMultiWindowFocusedDisplay() && "get".equals(reason) && task.mTaskId == other.mTaskId) {
                if (mService.isSplitLogDebug()) {
                    VSlog.d(TAG, "similarForInvalidTaskInSplit with task = " + task + ",other task = " + other + ", with same task?");
                }
                return true;
            } else if ("find".equals(reason) && task.getWindowingMode() != other.getWindowingMode()) {
                int activityType = task.getActivityType();
                boolean isUndefinedType = activityType == 0;
                int otherActivityType = other.getActivityType();
                boolean isOtherUndefinedType = otherActivityType == 0;
                boolean isCompatibleType = activityType == otherActivityType || isUndefinedType || isOtherUndefinedType;
                if (isCompatibleType) {
                    if ((task.inFreeformWindowingMode() && mService.isVivoFreeFormValid()) || (other.inFreeformWindowingMode() && !mService.isVivoFreeFormValid())) {
                        Intent intent = task.intent;
                        Intent trIntent = other.intent;
                        boolean sameAffinity = task.affinity != null && task.affinity.equals(other.affinity);
                        boolean sameIntent = intent != null && intent.filterEquals(trIntent);
                        if (mService.isSplitLogDebug()) {
                            VSlog.d(TAG, "similarForInvalidTaskInSplit with task = " + task + ",other task = " + other + ",sameAffinity is " + sameAffinity + ",sameIntent is " + sameIntent);
                        }
                        return sameIntent || sameAffinity;
                    }
                    return false;
                }
                return false;
            } else {
                return false;
            }
        }
        return false;
    }

    public void removeBoost(Task task) {
        Intent intent;
        ComponentName componentName;
        if (task == null || (intent = task.getBaseIntent()) == null || (componentName = intent.getComponent()) == null) {
            return;
        }
        String taskPkgName = componentName.getPackageName();
        AbsVivoPerfManager absVivoPerfManager = this.mUxPerf;
        if (absVivoPerfManager != null) {
            absVivoPerfManager.perfUXEngine_events(4, 0, taskPkgName, 0);
        }
    }

    public boolean shouldSkipForUpdate(Task task) {
        String packageName = null;
        Intent baseIntent = task.getBaseIntent();
        if (baseIntent != null) {
            if (baseIntent.getComponent() != null) {
                packageName = baseIntent.getComponent().getPackageName();
            } else {
                packageName = baseIntent.getPackage();
            }
        }
        if (packageName != null) {
            try {
                return AppGlobals.getPackageManager().isPackageFrozen(packageName);
            } catch (RemoteException e) {
                return false;
            }
        }
        return false;
    }
}