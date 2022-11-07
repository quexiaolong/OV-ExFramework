package com.android.server.am;

import android.content.IIntentReceiver;
import android.content.pm.ResolveInfo;
import android.os.SystemClock;
import android.os.UserHandle;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class BroadcastProxyRecord {
    public static final int BROADCASTFILTER_TARGET = 1;
    public static final int RESOLVEINFO_TARGET = 2;
    public static final int UNKONW_TARGET = 0;
    public String mAction;
    public BroadcastRecord mBr;
    public long mCreateTime;
    public ProcessRecord mCurApp;
    public int mFlags;
    public boolean mOrdered;
    public BroadcastProxyQueue mOwner;
    public int mPid;
    public String mPkgName;
    public String mProcess;
    public Object mTarget;
    public int mType;
    public int mUid;
    public int mUserId;

    public BroadcastProxyRecord(BroadcastProxyQueue queue, BroadcastRecord r, Object target) {
        this.mUid = -1;
        this.mPid = -1;
        this.mOwner = queue;
        this.mTarget = target;
        this.mAction = r.intent.getAction();
        this.mOrdered = r.ordered;
        if (this.mAction == null) {
            this.mAction = "null";
        }
        this.mCreateTime = SystemClock.elapsedRealtime();
        Object obj = this.mTarget;
        if (obj instanceof BroadcastFilter) {
            BroadcastFilter filter = (BroadcastFilter) obj;
            this.mPkgName = filter.packageName;
            this.mType = 1;
            this.mUid = filter.receiverList.uid;
            this.mPid = filter.receiverList.pid;
            ProcessRecord processRecord = filter.receiverList.app;
            this.mCurApp = processRecord;
            if (processRecord != null) {
                this.mProcess = processRecord.processName;
                if (this.mPid == -1) {
                    this.mPid = this.mCurApp.pid;
                }
                if (this.mUid == -1) {
                    this.mUid = this.mCurApp.uid;
                }
            }
        } else if (obj instanceof ResolveInfo) {
            ResolveInfo info = (ResolveInfo) obj;
            this.mType = 2;
            if (info.activityInfo != null && info.activityInfo.applicationInfo != null) {
                this.mPkgName = info.activityInfo.applicationInfo.packageName;
                this.mUid = info.activityInfo.applicationInfo.uid;
                this.mProcess = info.activityInfo.processName;
            }
        }
        this.mUserId = UserHandle.getUserId(this.mUid);
    }

    public void initTargetProcessLocked() {
        if (this.mType == 2 && this.mPid == -1) {
            ProcessRecord processRecordLocked = this.mOwner.getProcessRecordLocked(this.mProcess, this.mUid);
            this.mCurApp = processRecordLocked;
            if (processRecordLocked != null) {
                this.mPid = processRecordLocked.pid;
            } else {
                this.mPid = 0;
            }
        }
    }

    public void initBroadcastRecordLocked(BroadcastRecord r, Object target) {
        if (this.mBr == null) {
            List<Object> receiver = new ArrayList<>();
            receiver.add(target);
            this.mBr = new BroadcastRecord(r.queue, r.intent, r.callerApp, r.callerPackage, r.callerFeatureId, r.callingPid, r.callingUid, r.callerInstantApp, r.resolvedType, r.requiredPermissions, r.appOp, r.options, receiver, (IIntentReceiver) null, r.resultCode, r.resultData, r.resultExtras, r.ordered, r.sticky, r.initialSticky, r.userId, r.allowBackgroundActivityStarts, r.timeoutExempt);
        }
    }

    public String toString() {
        if (this.mType == 1) {
            BroadcastFilter filter = (BroadcastFilter) this.mTarget;
            return String.format("action=%s queue=%s uid=%d process=%s sendByOrdered=%s target=%s", this.mAction, this.mOwner.mParent.mQueueName, Integer.valueOf(this.mUid), this.mProcess, String.valueOf(this.mOrdered), filter.receiverList.toString());
        }
        return String.format("action=%s, queue=%s uid=%d process=%s sendByOrdered=%s target=%s", this.mAction, this.mOwner.mParent.mQueueName, Integer.valueOf(this.mUid), this.mProcess, String.valueOf(this.mOrdered), this.mTarget.toString());
    }
}