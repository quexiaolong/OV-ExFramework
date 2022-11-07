package com.android.server;

import android.os.Debug;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.SparseArray;
import com.android.server.lockmonitor.IVivoFrameworkLockMonitor;
import com.android.server.lockmonitor.LockMonitor;
import com.vivo.face.common.data.Constants;
import com.vivo.framework.configuration.ConfigurationManager;
import com.vivo.services.rms.ProcessInfo;
import com.vivo.services.rms.appmng.AppManager;
import com.vivo.statistics.sdk.GatherManager;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import vivo.app.configuration.ConfigurationObserver;
import vivo.app.configuration.ContentValuesList;

/* loaded from: classes.dex */
public class VivoFrameworkLockMonitor implements Handler.Callback, IVivoFrameworkLockMonitor {
    private static final int AMSLOCK = 0;
    private static final String FRAMELOCK_FILE = "/data/bbkcore/frameworklock_1.0.xml";
    private static final String FRAMELOCK_NAME = "framework_lock";
    private static final String FRAMELOCK_NAME_BACKTRACE = "framelock_name_backtrace";
    private static final String FRAMELOCK_NAME_MONITOR_ENABLE = "framelock_name_monitor_enable";
    private static final String FRAMELOCK_NAME_THRESHOLD = "framelock_name_threshold";
    private static final String FRAMELOCK_NAME_UPLOAD_NUM = "framelock_name_upload_num";
    private static final String FRAMELOCK_NAME_UPLOAD_TIME_INTERVAL = "framelock_name_upload_time_interval";
    private static final int MSG_UPLOAD_INFO = 4;
    private static final int OOMLOCK = 3;
    private static final int PMSLOCK = 2;
    static final String TAG = "frame_lock";
    private static final int TYPE_MAX = 3;
    private static final int WMSLOCK = 1;
    private Thread frameworkLockThread;
    private AppManager mAppManager;
    private ConfigurationManager mConfigurationManager;
    private ContentValuesList mFramelockList;
    private Handler mHandler;
    private static int MAX_INTERNAL = 5;
    private static final int[] INTERNAL_TIME = {10, 20, 50, 100, 200};
    private boolean isInit = false;
    private long mAmsMointorBegin = 0;
    private long mWmsMointorBegin = 0;
    private long mPmsMointorBegin = 0;
    private long mOomAdjBegin = 0;
    SparseArray<HashMap<String, ArrayList<LockInformation>>> mLockInformation = new SparseArray<>();
    private boolean mMonitorEnable = false;
    private int mThreshold = SystemProperties.getInt("persist.vivo.lock.monitortime", 5);
    private boolean mBacktrace = SystemProperties.getBoolean("persist.vivo.lock.backtrace", false);
    private int mUploadNum = 2;
    private int mUploadTimeInterval = 8;
    private ConfigurationObserver frameLockObserver = new ConfigurationObserver() { // from class: com.android.server.VivoFrameworkLockMonitor.1
        public void onConfigChange(String file, String name) {
            ContentValuesList list = VivoFrameworkLockMonitor.this.mConfigurationManager.getContentValuesList(VivoFrameworkLockMonitor.FRAMELOCK_FILE, VivoFrameworkLockMonitor.FRAMELOCK_NAME);
            VivoFrameworkLockMonitor.this.updateStatus(list, false);
        }
    };

    /* loaded from: classes.dex */
    private static class Instance {
        private static final VivoFrameworkLockMonitor INSTANCE = new VivoFrameworkLockMonitor();

        private Instance() {
        }
    }

    public static VivoFrameworkLockMonitor getInstance() {
        return Instance.INSTANCE;
    }

    public VivoFrameworkLockMonitor() {
        ServiceThread frameworkLockThread = new ServiceThread(TAG, 0, false);
        frameworkLockThread.start();
        this.mHandler = new Handler(frameworkLockThread.getLooper(), this);
        this.mAppManager = AppManager.getInstance();
        this.mConfigurationManager = ConfigurationManager.getInstance();
        this.mLockInformation.put(0, new HashMap<>());
        this.mLockInformation.put(1, new HashMap<>());
        this.mLockInformation.put(2, new HashMap<>());
        this.mLockInformation.put(3, new HashMap<>());
    }

    public void systemReady() {
        ConfigurationManager configurationManager = this.mConfigurationManager;
        if (configurationManager != null) {
            ContentValuesList contentValuesList = configurationManager.getContentValuesList(FRAMELOCK_FILE, FRAMELOCK_NAME);
            this.mFramelockList = contentValuesList;
            this.mConfigurationManager.registerObserver(contentValuesList, this.frameLockObserver);
            updateStatus(this.mFramelockList, true);
        }
        this.isInit = true;
    }

    public void updateStatus(ContentValuesList list, boolean isStart) {
        try {
            this.mMonitorEnable = Boolean.parseBoolean(list.getValue(FRAMELOCK_NAME_MONITOR_ENABLE));
            this.mThreshold = Integer.parseInt(list.getValue(FRAMELOCK_NAME_THRESHOLD));
            this.mBacktrace = Boolean.parseBoolean(list.getValue(FRAMELOCK_NAME_BACKTRACE));
            this.mUploadNum = Integer.parseInt(list.getValue(FRAMELOCK_NAME_UPLOAD_NUM));
            this.mUploadTimeInterval = Integer.parseInt(list.getValue(FRAMELOCK_NAME_UPLOAD_TIME_INTERVAL));
            this.mAmsMointorBegin = 0L;
            this.mWmsMointorBegin = 0L;
            this.mPmsMointorBegin = 0L;
            this.mOomAdjBegin = 0L;
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (this.mMonitorEnable) {
            this.mHandler.removeMessages(4);
            Handler handler = this.mHandler;
            handler.sendMessageDelayed(handler.obtainMessage(4), TimeUnit.HOURS.toMillis(this.mUploadTimeInterval));
            LockMonitor.MONITOR_ENABLE = true;
            return;
        }
        this.mHandler.removeMessages(4);
        LockMonitor.MONITOR_ENABLE = false;
        if (!isStart) {
            clearMonitorInformation();
        }
    }

    public void monitorLockBegin(int type) {
        if (type == 0) {
            this.mAmsMointorBegin = SystemClock.uptimeMillis();
        } else if (type == 1) {
            this.mWmsMointorBegin = SystemClock.uptimeMillis();
        } else if (type == 2) {
            this.mPmsMointorBegin = SystemClock.uptimeMillis();
        } else if (type == 3) {
            this.mOomAdjBegin = SystemClock.uptimeMillis();
        }
    }

    public void monitorLockEnd(int type, String caller, int pid) {
        int cost;
        String backtrace = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        if (type == 0) {
            cost = (int) (SystemClock.uptimeMillis() - this.mAmsMointorBegin);
        } else if (type == 1) {
            cost = (int) (SystemClock.uptimeMillis() - this.mWmsMointorBegin);
        } else if (type == 2) {
            cost = (int) (SystemClock.uptimeMillis() - this.mPmsMointorBegin);
        } else if (type == 3) {
            cost = (int) (SystemClock.uptimeMillis() - this.mOomAdjBegin);
        } else {
            return;
        }
        if (cost < this.mThreshold || cost > Integer.MAX_VALUE) {
            return;
        }
        if (this.mBacktrace) {
            backtrace = Debug.getCallers(5);
        }
        LockInformation lockInformation = new LockInformation(caller, cost, backtrace);
        Message msg = this.mHandler.obtainMessage(type, pid, 0, lockInformation);
        this.mHandler.sendMessage(msg);
    }

    public void addLockInfLocked(int type, int pid, LockInformation information) {
        ProcessInfo info;
        if (!this.isInit || (info = this.mAppManager.getProcessInfo(pid)) == null || type > 3) {
            return;
        }
        information.mProcessName = info.mProcName;
        HashMap<String, ArrayList<LockInformation>> lockMap = this.mLockInformation.get(type);
        String key = createKey(information.mcaller, info.mProcName);
        ArrayList<LockInformation> lockList = lockMap.get(key);
        if (lockList != null) {
            boolean bFound = false;
            Iterator<LockInformation> it = lockList.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                LockInformation tempInformation = it.next();
                if (tempInformation.backtraceEquals(information)) {
                    LockInformation.access$408(tempInformation);
                    LockInformation.access$512(tempInformation, information.mCostTime);
                    int[] iArr = tempInformation.mInterval;
                    int convertToIndex = convertToIndex(information.mCostTime);
                    iArr[convertToIndex] = iArr[convertToIndex] + 1;
                    bFound = true;
                    break;
                }
            }
            if (!bFound) {
                lockList.add(information);
                int[] iArr2 = information.mInterval;
                int convertToIndex2 = convertToIndex(information.mCostTime);
                iArr2[convertToIndex2] = iArr2[convertToIndex2] + 1;
                return;
            }
            return;
        }
        ArrayList<LockInformation> informationList = new ArrayList<>();
        informationList.add(information);
        lockMap.put(key, informationList);
        int[] iArr3 = information.mInterval;
        int convertToIndex3 = convertToIndex(information.mCostTime);
        iArr3[convertToIndex3] = iArr3[convertToIndex3] + 1;
    }

    int convertToIndex(int time) {
        int index = 0;
        while (true) {
            int i = MAX_INTERNAL;
            if (index < i) {
                if (time >= INTERNAL_TIME[index]) {
                    index++;
                } else {
                    return index;
                }
            } else {
                return i - 1;
            }
        }
    }

    public void uploadInformation() {
        if (!this.isInit || !this.mMonitorEnable) {
            return;
        }
        synchronized (this.mLockInformation) {
            ArrayList<LockInformation> lockList = new ArrayList<>();
            for (int type = 0; type <= 3; type++) {
                HashMap<String, ArrayList<LockInformation>> lockMap = this.mLockInformation.get(type);
                for (Map.Entry<String, ArrayList<LockInformation>> entry : lockMap.entrySet()) {
                    lockList.addAll(entry.getValue());
                }
                Collections.sort(lockList);
                int index = 0;
                Iterator<LockInformation> it = lockList.iterator();
                while (it.hasNext()) {
                    LockInformation tempInformation = it.next();
                    GatherManager.getInstance().gather(TAG, new Object[]{tempInformation.mcaller, tempInformation.mProcessName, tempInformation.mbacktrace, Integer.valueOf(tempInformation.mcount), Integer.valueOf(tempInformation.mCostTime), tempInformation.mInterval});
                    resetInformation(tempInformation);
                    index++;
                    if (index > this.mUploadNum) {
                        break;
                    }
                }
                lockList.clear();
            }
        }
        this.mHandler.removeMessages(4);
        Handler handler = this.mHandler;
        handler.sendMessageDelayed(handler.obtainMessage(4), TimeUnit.HOURS.toMillis(this.mUploadTimeInterval));
    }

    void resetInformation(LockInformation information) {
        information.mcount = 0;
        information.mCostTime = 0;
        for (int i = 0; i < MAX_INTERNAL; i++) {
            information.mInterval[i] = 0;
        }
    }

    @Override // android.os.Handler.Callback
    public boolean handleMessage(Message msg) {
        int i = msg.what;
        if (i == 0 || i == 1 || i == 2 || i == 3) {
            synchronized (this.mLockInformation) {
                addLockInfLocked(msg.what, msg.arg1, (LockInformation) msg.obj);
            }
        } else if (i == 4) {
            uploadInformation();
        }
        return true;
    }

    private void clearMonitorInformation() {
        synchronized (this.mLockInformation) {
            for (int type = 0; type <= 3; type++) {
                HashMap<String, ArrayList<LockInformation>> lockMap = this.mLockInformation.get(type);
                lockMap.clear();
            }
        }
    }

    public void dumpFrameworkLockInformation(PrintWriter pw, String[] args, int opti) {
        String typeArgs;
        if (args.length <= 1) {
            typeArgs = args[opti - 1];
        } else {
            typeArgs = args[opti];
        }
        if ("clear".equals(typeArgs)) {
            clearMonitorInformation();
            return;
        }
        synchronized (this.mLockInformation) {
            ArrayList<LockInformation> lockList = new ArrayList<>();
            for (int type = 0; type <= 3; type++) {
                if (type == 0) {
                    pw.println("---------AMS lock inforamtion --------- begin");
                } else if (type == 1) {
                    pw.println("---------WMS lock inforamtion --------- begin");
                } else if (type == 2) {
                    pw.println("---------PMS lock inforamtion --------- begin");
                } else {
                    pw.println("---------OOMADJ inforamtion --------- begin");
                }
                HashMap<String, ArrayList<LockInformation>> lockMap = this.mLockInformation.get(type);
                for (Map.Entry<String, ArrayList<LockInformation>> entry : lockMap.entrySet()) {
                    lockList.addAll(entry.getValue());
                }
                Collections.sort(lockList);
                Iterator<LockInformation> it = lockList.iterator();
                while (it.hasNext()) {
                    LockInformation tempInformation = it.next();
                    pw.println("caller: " + tempInformation.mcaller + " proc: " + tempInformation.mProcessName + " backtrace: " + tempInformation.mbacktrace + " count: " + tempInformation.mcount + " costTime: " + tempInformation.mCostTime + " interval: " + Arrays.toString(tempInformation.mInterval));
                }
                if (type == 0) {
                    pw.println("---------AMS lock inforamtion --------- end");
                } else if (type == 1) {
                    pw.println("---------WMS lock inforamtion --------- end");
                } else if (type == 2) {
                    pw.println("---------PMS lock inforamtion --------- end");
                } else {
                    pw.println("---------OOMADJ inforamtion --------- begin");
                }
                lockList.clear();
            }
        }
    }

    public String createKey(String caller, String procName) {
        return String.format(Locale.US, "%s_%s", caller, procName);
    }

    /* loaded from: classes.dex */
    public class LockInformation implements Comparable<LockInformation> {
        private int mCostTime;
        private String mbacktrace;
        private String mcaller;
        private String mProcessName = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        private int[] mInterval = new int[VivoFrameworkLockMonitor.MAX_INTERNAL];
        private int mcount = 1;

        static /* synthetic */ int access$408(LockInformation x0) {
            int i = x0.mcount;
            x0.mcount = i + 1;
            return i;
        }

        static /* synthetic */ int access$512(LockInformation x0, int x1) {
            int i = x0.mCostTime + x1;
            x0.mCostTime = i;
            return i;
        }

        public LockInformation(String caller, int cost, String backtrace) {
            this.mcaller = caller;
            this.mbacktrace = backtrace;
            this.mCostTime = cost;
        }

        @Override // java.lang.Comparable
        public int compareTo(LockInformation o) {
            return Integer.compare(o.mCostTime, this.mCostTime);
        }

        public boolean backtraceEquals(LockInformation arg0) {
            String str = this.mbacktrace;
            if (str != null && str.equals(arg0.mbacktrace)) {
                return true;
            }
            return false;
        }
    }
}