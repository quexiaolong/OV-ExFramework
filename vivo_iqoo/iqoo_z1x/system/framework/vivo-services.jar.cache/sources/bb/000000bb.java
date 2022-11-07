package com.android.server.am;

import com.android.server.wm.WindowState;
import java.util.ArrayList;
import java.util.HashSet;

/* loaded from: classes.dex */
public class RMProcInfo {
    public boolean allowFreeze;
    public String mAdjType;
    public boolean mAlive;
    public String mCreateReason;
    public String mKillReason;
    public long mLastActiveElapsedTime;
    public long mLastActiveTime;
    public long mLastAnimationTime;
    public long mLastInvisibleTime;
    public int mOom;
    public ProcessRecord mParent;
    public int mPid;
    public int mPkgFlags;
    public String mPkgName;
    public String mProcName;
    public int mProcState;
    public int mStates;
    public int mUid;
    public int mAdj = 1001;
    public int mSchedGroup = -1;
    public int mOriginGroup = -1;
    public final ArrayList<String> mPkgList = new ArrayList<>();
    public final ArrayList<String> mDepPkgList = new ArrayList<>();
    public final HashSet<WindowState> mWindows = new HashSet<>();
    public int mCgrpGroup = -1;
    public boolean rmsPreloaded = false;
    public boolean needKeepQuiet = false;
    public int mBinderGroup = 0;
    public int mBinderSetGroup = 0;
}