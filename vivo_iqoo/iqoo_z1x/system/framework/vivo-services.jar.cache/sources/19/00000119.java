package com.android.server.am.frozen;

import android.hardware.graphics.common.V1_0.Dataspace;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.SparseArray;
import com.android.server.IVivoWorkingState;
import com.android.server.am.ProcessRecord;
import com.android.server.am.RMProcHelper;
import com.vivo.face.common.data.Constants;
import com.vivo.services.rms.PackageInfo;
import com.vivo.services.rms.ProcessInfo;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class FrozenAppRecord implements ProcessInfo.StateChangeListener, IVivoWorkingState {
    private static final int ACTIVE_MASKS = 1033;
    public static final int ALLOW_FROZEN = 0;
    public static final int HAVE_FROZEN = 2;
    public static final int NOT_ALLOW_FROZEN = 1;
    public static final SparseArray<String> ONOFF_NAMES;
    public static final SparseArray<String> STATES_NAMES;
    private static final String TAG = "quickfrozen";
    public int caller;
    public long enterBgTime;
    public long enterBgTimeForDump;
    long freezeUnfreezeTime;
    public boolean isCheckDownload;
    public boolean isInPemBlackList;
    public boolean isInPemWhiteList;
    public boolean isVisable;
    PackageInfo mOwner;
    public String pkgName;
    public long requestFrozenTime;
    public int retryFznCnt;
    public int uid;
    public int unfreezeReason;
    int virtualDisplaySize;
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd E a HH:mm:ss");
    private static final double[] DEFAULT_PERIODS_IN_MINUTES = {0.25d, 0.5d, 1.0d, 2.0d, 3.0d, 4.0d, 5.0d, 10.0d, 20.0d, 30.0d};
    public final ArrayList<ProcessInfo> mProcs = new ArrayList<>();
    public int mWorkingState = 0;
    public boolean isSystemApp = false;
    public double[] mPeriodsInMinutes = DEFAULT_PERIODS_IN_MINUTES;
    public int mPeriodIndex = 0;
    public boolean mEverForeground = false;
    boolean frozen = false;
    boolean allowFreeze = false;
    public String mKey = generateKey();
    private final long mUidRunningTime = System.currentTimeMillis();

    static {
        SparseArray<String> sparseArray = new SparseArray<>();
        STATES_NAMES = sparseArray;
        sparseArray.put(1, "record");
        STATES_NAMES.put(2, "audio");
        STATES_NAMES.put(4, WorkingStateManager.FLOAT_WINDOW_NAME);
        STATES_NAMES.put(8, "download");
        STATES_NAMES.put(16, "bluetooth");
        STATES_NAMES.put(32, "navigation");
        STATES_NAMES.put(16384, "bg_game");
        STATES_NAMES.put(256, "camera");
        STATES_NAMES.put(Dataspace.STANDARD_BT709, "virtualdisplay");
        SparseArray<String> sparseArray2 = new SparseArray<>();
        ONOFF_NAMES = sparseArray2;
        sparseArray2.put(0, "off");
        ONOFF_NAMES.put(1, "on");
    }

    public FrozenAppRecord(String pkgName, int uid) {
        this.pkgName = pkgName;
        this.uid = uid;
    }

    public void setWorkingState(int state, int mask) {
        int oldWorkingState = this.mWorkingState;
        int i = (this.mWorkingState & (~mask)) | (state & mask);
        this.mWorkingState = i;
        if (i == 0 && this.allowFreeze) {
            setBgTime();
        } else {
            clearBgTime();
        }
        if (mask == 8) {
            if (!this.mEverForeground && state != 0) {
                this.mWorkingState &= -9;
            }
            this.isCheckDownload = false;
        } else if (isAppInFrozenList()) {
            if (oldWorkingState != 0 && this.mWorkingState == 0 && !this.isCheckDownload && !isVisable()) {
                VSlog.d("download", "setWorkingState beginCheckDownloadStatus");
                beginCheckDownloadStatus();
            } else if (oldWorkingState == 0 && this.mWorkingState != 0 && this.isCheckDownload) {
                stopCheckDownloadStatus();
            }
        }
    }

    void beginCheckDownloadStatus() {
        FrozenQuicker.getInstance().beginToCheckDownloadStatus(this);
        this.isCheckDownload = true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void stopCheckDownloadStatus() {
        WorkingStateManager.getInstance().stopCheckDownloadStatus(this.uid, this.pkgName);
        this.isCheckDownload = false;
    }

    public void resetWorkingState() {
        this.mWorkingState = 0;
    }

    private void setBgTime() {
        this.enterBgTime = SystemClock.elapsedRealtime();
        this.enterBgTimeForDump = System.currentTimeMillis();
    }

    public void setFrozenTime() {
        this.requestFrozenTime = System.currentTimeMillis();
    }

    public void clearBgTime() {
        this.enterBgTime = 0L;
    }

    public boolean allowSkipDelay() {
        return System.currentTimeMillis() - this.mUidRunningTime > 5000;
    }

    public void addProc(ProcessInfo proc) {
        synchronized (this.mProcs) {
            this.mProcs.add(proc);
            proc.addStateChangedListener(this);
        }
    }

    public void removeStateChangedListener() {
        synchronized (this.mProcs) {
            Iterator<ProcessInfo> it = this.mProcs.iterator();
            while (it.hasNext()) {
                ProcessInfo r = it.next();
                r.removeStateChangedListener(this);
            }
        }
    }

    public void computeFreezeStatus() {
        boolean allow = true;
        synchronized (this.mProcs) {
            Iterator<ProcessInfo> it = this.mProcs.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                ProcessInfo r = it.next();
                if (!UserHandle.isIsolated(r.mUid) && !r.allowFreeze) {
                    allow = false;
                    break;
                }
            }
        }
        if (this.mWorkingState == 0 && allow && !this.allowFreeze) {
            setBgTime();
        }
        this.allowFreeze = allow;
    }

    public boolean hasFgService() {
        boolean hasFgService = false;
        synchronized (this.mProcs) {
            Iterator<ProcessInfo> it = this.mProcs.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                ProcessInfo r = it.next();
                if (r.hasFgService()) {
                    hasFgService = true;
                    break;
                }
            }
        }
        return hasFgService;
    }

    public void ComputeVisable() {
        boolean visable = false;
        synchronized (this.mProcs) {
            Iterator<ProcessInfo> it = this.mProcs.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                ProcessInfo r = it.next();
                if (r.isVisible()) {
                    visable = true;
                    break;
                }
            }
        }
        if (isAppInFrozenList()) {
            if (!visable && isVisable() && this.mWorkingState == 0) {
                beginCheckDownloadStatus();
            } else if (visable && !isVisable()) {
                setWorkingState(0, 8);
                stopCheckDownloadStatus();
            }
            if (visable && !isVisable()) {
                this.mPeriodIndex = 0;
                this.mEverForeground = true;
                VSlog.i(TAG, this.pkgName + ", uid[" + this.uid + "] ever enter foreground.");
            }
        }
        this.isVisable = visable;
    }

    public void setPeriods(double[] periods) {
        if (periods != null && periods.length > 0) {
            this.mPeriodIndex = 0;
            this.mPeriodsInMinutes = periods;
        }
    }

    public long getNextPeriod() {
        int i = this.mPeriodIndex;
        if (i < 0) {
            this.mPeriodIndex = 0;
        } else {
            double[] dArr = this.mPeriodsInMinutes;
            if (i >= dArr.length) {
                this.mPeriodIndex = dArr.length - 1;
            }
        }
        double[] dArr2 = this.mPeriodsInMinutes;
        int i2 = this.mPeriodIndex;
        this.mPeriodIndex = i2 + 1;
        return (long) (dArr2[i2] * 60.0d * 1000.0d);
    }

    public boolean isAudioOn() {
        return (this.mWorkingState & 2) != 0;
    }

    public boolean isAppInFrozenList() {
        if (FrozenQuicker.isFeatureSupport) {
            return !(FrozenQuicker.isBlackListFromPem && this.isInPemBlackList) && (!FrozenQuicker.isWhiteListFromPem || this.isInPemWhiteList);
        }
        return false;
    }

    public boolean isAllowFrozen() {
        if (this.allowFreeze && this.mWorkingState == 0 && !this.frozen && this.virtualDisplaySize == 0 && !this.isVisable) {
            return true;
        }
        return false;
    }

    public void removeProc(ProcessInfo proc) {
        synchronized (this.mProcs) {
            this.mProcs.remove(proc);
            proc.removeStateChangedListener(this);
        }
    }

    public ArrayList<ProcessRecord> getProcessList() {
        ArrayList<ProcessRecord> processInfos;
        synchronized (this.mProcs) {
            processInfos = new ArrayList<>();
            Iterator<ProcessInfo> it = this.mProcs.iterator();
            while (it.hasNext()) {
                ProcessInfo processInfo = it.next();
                if (processInfo.mPid != 0) {
                    if (processInfo.mParent != null) {
                        processInfos.add(processInfo.mParent);
                    }
                }
            }
        }
        return processInfos;
    }

    public ProcessInfo findProcess(int pid) {
        synchronized (this.mProcs) {
            Iterator<ProcessInfo> it = this.mProcs.iterator();
            while (it.hasNext()) {
                ProcessInfo r = it.next();
                if (r.mPid == pid) {
                    return r;
                }
            }
            return null;
        }
    }

    public void setUnfreezeInfo() {
        this.enterBgTime = 0L;
        this.enterBgTimeForDump = 0L;
        this.frozen = false;
        this.requestFrozenTime = System.currentTimeMillis();
        this.retryFznCnt = 0;
    }

    public void setFrozenInfo() {
        this.frozen = true;
        this.requestFrozenTime = System.currentTimeMillis();
    }

    public boolean hasTopUi() {
        return false;
    }

    @Override // com.vivo.services.rms.ProcessInfo.StateChangeListener
    public void onStateChanged(int mask, boolean visable, ProcessInfo processInfo) {
        if (!this.isSystemApp && !this.isInPemBlackList && (mask & ACTIVE_MASKS) != 0) {
            ComputeVisable();
        }
    }

    public boolean isVisable() {
        return this.isVisable;
    }

    public boolean isHome(int pid) {
        synchronized (this.mProcs) {
            Iterator<ProcessInfo> it = this.mProcs.iterator();
            while (it.hasNext()) {
                ProcessInfo r = it.next();
                if (r.mPid == pid) {
                    return true;
                }
            }
            return false;
        }
    }

    public ProcessInfo findProc(String procName) {
        synchronized (this.mProcs) {
            Iterator<ProcessInfo> it = this.mProcs.iterator();
            while (it.hasNext()) {
                ProcessInfo r = it.next();
                if (r.mProcName.equals(procName)) {
                    return r;
                }
            }
            return null;
        }
    }

    public boolean isEmpty() {
        boolean isEmpty;
        synchronized (this.mProcs) {
            isEmpty = this.mProcs.isEmpty();
        }
        return isEmpty;
    }

    public String generateKey() {
        return String.format(Locale.US, "%d_%s", Integer.valueOf(this.uid), this.pkgName);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(512);
        sb.append(this.pkgName);
        sb.append(" uid[");
        sb.append(this.uid + "]");
        sb.append(" everForeground: ");
        sb.append(this.mEverForeground);
        sb.append(" allowFreeze: ");
        sb.append(this.allowFreeze);
        sb.append(" mWorkingState:[ ");
        sb.append(getName(this.mWorkingState));
        sb.append(" ] virtualDisplaySize ");
        sb.append(this.virtualDisplaySize);
        sb.append(" frozen ");
        sb.append(this.frozen);
        sb.append(" retryFznCnt ");
        sb.append(this.retryFznCnt);
        if (this.frozen) {
            sb.append(" caller: ");
            sb.append(FrozenDataInfo.convertCaller(this.caller));
        }
        sb.append("\n\t      enterBgTime ");
        sb.append(fromatTime(this.enterBgTimeForDump));
        sb.append(" requestFrozenTime ");
        sb.append(fromatTime(this.requestFrozenTime));
        sb.append(" now:");
        sb.append(fromatTime(System.currentTimeMillis()));
        sb.append(" isInWhite ");
        sb.append(this.isInPemWhiteList);
        sb.append(" isInBlack ");
        sb.append(this.isInPemBlackList);
        sb.append(" isSystemApp ");
        sb.append(this.isSystemApp);
        sb.append("\n");
        synchronized (this.mProcs) {
            Iterator<ProcessInfo> it = this.mProcs.iterator();
            while (it.hasNext()) {
                ProcessInfo r = it.next();
                sb.append("\n\t  ");
                sb.append(r.toString());
                sb.append("\n\t    processState: ");
                sb.append(RMProcHelper.getInt(r.mParent, 2));
                sb.append(" freezeflag ");
                sb.append(RMProcHelper.getInt(r.mParent, 15));
                sb.append(" allowfreeze ");
                sb.append(r.allowFreeze);
                sb.append(" mRecord:");
                sb.append(r.mRecord == null ? null : r.mRecord.generateKey());
                sb.append("\n");
            }
        }
        sb.append("\n");
        return sb.toString();
    }

    public String getName(int state) {
        if (state == 0) {
            return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        }
        StringBuilder builder = new StringBuilder(48);
        for (int i = 0; i < STATES_NAMES.size(); i++) {
            if ((STATES_NAMES.keyAt(i) & state) != 0) {
                builder.append(STATES_NAMES.valueAt(i));
                builder.append(" ");
            }
        }
        int i2 = builder.length();
        if (i2 <= 1) {
            return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        }
        return builder.substring(0, builder.length() - 1);
    }

    public String fromatTime(long time) {
        if (time > 0) {
            return TIME_FORMAT.format(new Date(time));
        }
        return " ";
    }

    public FrozenAppRecord obtain(String pkgName, int uid) {
        this.pkgName = pkgName;
        this.uid = uid;
        return this;
    }

    /* renamed from: clone */
    public FrozenAppRecord m0clone() {
        return new FrozenAppRecord(this.pkgName, this.uid);
    }
}