package com.android.server.am.frozen;

import android.hardware.graphics.common.V1_0.BufferUsage;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.util.ArraySet;
import com.android.server.am.frozen.WorkingStateManager;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class DownloadState {
    protected static final int BEGIN_CHECK = 0;
    protected static final int CHECK_STATE = 1;
    public static final long DELAY_BEGIN_CHECK_STATES_TIME = 5000;
    public static final long FG_SERVICE_DELAY_CHECK_TIME = 10000;
    public static final int FG_SERVICE_MAX_CHECK_COUNT = 6;
    protected static final int FINISH_STATE = 2;
    protected static final int MSG_BEGIN_CHECK = 0;
    protected static final int MSG_CHECK_STATE = 1;
    protected static final int MSG_FINISH_STATE = 2;
    protected static final int MSG_FORCE_SKIP_CHECK = 3;
    public static final long NORMAL_DELAY_CHECK_STATES_TIME = 10000;
    public static final int NORMAL_MAX_CHECK_COUNT = 9;
    public static final String TAG = "download";
    private static final int THRESHOLD_LEVEL0 = 1048576;
    private static final int THRESHOLD_LEVEL1 = 2097152;
    private static final int THRESHOLD_LEVEL2 = 3145728;
    private static final int THRESHOLD_TOTAL = 3145728;
    protected final MyHandler mHandler;
    protected final int mState;
    private static long mFlowDataThrehold = 2097152;
    private static final boolean DEBUG = SystemProperties.get("persist.sys.log.ctrl", "no").equals("yes");
    protected final ArraySet<WorkingStateManager.StateChangeListener> mCallbacks = new ArraySet<>();
    private final HashMap<String, DownloadData> mDownloadData = new HashMap<>();
    protected final String mName = "download";
    private final DecimalFormat mDecimalFormat = new DecimalFormat("#.00");

    public DownloadState(Looper looper, int state) {
        this.mHandler = new MyHandler(looper);
        this.mState = state;
    }

    public void beginCheckState(int uid, String packageName, boolean hasFgService, boolean skipCheckState) {
        synchronized (this) {
            DownloadData download = this.mDownloadData.get(createKey(uid, packageName));
            if (download == null) {
                download = new DownloadData(uid, packageName);
                this.mDownloadData.put(createKey(uid, packageName), download);
            }
            resetData(download, hasFgService);
            Message msg = this.mHandler.obtainMessage(skipCheckState ? 3 : 0, download);
            this.mHandler.sendMessageDelayed(msg, 5000L);
            if (DEBUG) {
                VSlog.e("download", String.format("beginCheckState [%s, %d]  delay check 5s, hasFgService = %b", download.mPkgName, Integer.valueOf(download.mUid), Boolean.valueOf(hasFgService)));
            }
        }
    }

    void resetData(DownloadData download, boolean hasFgService) {
        if (hasFgService) {
            download.threshold = mFlowDataThrehold / 2;
            download.duration = 10000L;
            download.maxCheckout = 6;
        } else {
            download.threshold = mFlowDataThrehold;
            download.duration = 10000L;
            download.maxCheckout = 9;
        }
        download.checkcount = 0;
        download.stopCheck = false;
        download.newDataRx = 0L;
        download.oldDataRx = 0L;
        download.flowRx = 0L;
        download.newDataTx = 0L;
        download.oldDataTx = 0L;
        download.flowTx = 0L;
    }

    void checkIsDownloadState(DownloadData download) {
        if (download.flowRx < download.threshold && download.flowTx < download.threshold) {
            notifyCallback(download.mPkgName, download.mUid, 0, this.mState);
            download.stopCheck = true;
        } else if (download.checkcount < download.maxCheckout) {
            continueCheckState(download);
        } else {
            notifyCallback(download.mPkgName, download.mUid, 1, this.mState);
            download.stopCheck = true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void forceSkipCheckDownload(DownloadData download) {
        notifyCallback(download.mPkgName, download.mUid, 0, this.mState);
        download.stopCheck = true;
    }

    public void continueCheckState(DownloadData download) {
        MyHandler myHandler = this.mHandler;
        myHandler.sendMessageDelayed(myHandler.obtainMessage(1, download), download.duration);
    }

    public void StopCheckState(int uid, String packageName) {
        synchronized (this) {
            DownloadData download = this.mDownloadData.get(createKey(uid, packageName));
            if (download == null) {
                return;
            }
            download.stopCheck = true;
            this.mHandler.removeMessages(0, download);
            this.mHandler.removeMessages(1, download);
            this.mHandler.removeMessages(3, download);
            if (DEBUG) {
                VSlog.e("download", String.format("StopCheckState %s %d", download.mPkgName, Integer.valueOf(download.mUid)));
            }
        }
    }

    public void setNetflowThreshold(long threshold) {
        long j = 1024 * threshold;
        mFlowDataThrehold = j;
        if (DEBUG) {
            VSlog.e("download", String.format("setNetflowThreshold  %d", Long.valueOf(j)));
        }
    }

    public String createKey(int uid, String pkgName) {
        return String.format(Locale.US, "%d_%s", Integer.valueOf(uid), pkgName);
    }

    void updateDownLoadInformation(DownloadData download) {
        try {
            download.oldDataRx = download.newDataRx;
            download.newDataRx = TrafficStats.getUidRxBytes(download.mUid);
            download.flowRx = download.newDataRx - download.oldDataRx;
            download.oldDataTx = download.newDataTx;
            download.newDataTx = TrafficStats.getUidTxBytes(download.mUid);
            download.flowTx = download.newDataTx - download.oldDataTx;
            download.checkcount++;
            if (DEBUG) {
                VSlog.e("download", String.format("updateDownLoadInformation [%s, %d]  flowRx: " + getFlowStr(download.flowRx) + ", flowTx: " + getFlowStr(download.flowTx), download.mPkgName, Integer.valueOf(download.mUid)));
            }
        } catch (Exception e) {
            VSlog.e("download", "updateDownLoadInformation exception", e);
        }
    }

    private String getFlowStr(long flow) {
        if (flow < 0) {
            return "error";
        }
        if (flow < 1024) {
            return flow + " Byte";
        } else if (flow < BufferUsage.RENDERSCRIPT) {
            return this.mDecimalFormat.format(flow / 1024.0d) + " KB";
        } else {
            return this.mDecimalFormat.format(flow / 1048576.0d) + " MB";
        }
    }

    private void notifyCallback(String packageName, int uid, int state, int model) {
        ArraySet<WorkingStateManager.StateChangeListener> tmpCallbacks = new ArraySet<>();
        synchronized (this) {
            if (this.mCallbacks.isEmpty()) {
                return;
            }
            tmpCallbacks.addAll(this.mCallbacks);
            Iterator<WorkingStateManager.StateChangeListener> it = tmpCallbacks.iterator();
            while (it.hasNext()) {
                WorkingStateManager.StateChangeListener callback = it.next();
                callback.onStateChanged(model, state, uid, packageName);
            }
            if (DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("notifyCallback [%s, %d] download = ");
                sb.append(state != 0);
                VSlog.e("download", String.format(sb.toString(), packageName, Integer.valueOf(uid)));
            }
        }
    }

    public void addCallback(WorkingStateManager.StateChangeListener callback) {
        synchronized (this) {
            this.mCallbacks.add(callback);
        }
    }

    public void removeCallback(WorkingStateManager.StateChangeListener callback) {
        synchronized (this) {
            this.mCallbacks.remove(callback);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public class DownloadData {
        int checkcount;
        long duration;
        long flowRx;
        long flowTx;
        String mPkgName;
        int mUid;
        int maxCheckout;
        long newDataRx;
        long newDataTx;
        long oldDataRx;
        long oldDataTx;
        boolean stopCheck;
        long threshold;

        DownloadData(int uid, String packageName) {
            this.mUid = uid;
            this.mPkgName = packageName;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public class MyHandler extends Handler {
        private MyHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 0) {
                DownloadData download = (DownloadData) msg.obj;
                if (!download.stopCheck) {
                    DownloadState.this.updateDownLoadInformation(download);
                    DownloadState.this.continueCheckState(download);
                }
            } else if (i == 1) {
                DownloadData download2 = (DownloadData) msg.obj;
                if (!download2.stopCheck) {
                    DownloadState.this.updateDownLoadInformation(download2);
                    DownloadState.this.checkIsDownloadState(download2);
                }
            } else if (i == 3) {
                DownloadData download3 = (DownloadData) msg.obj;
                if (!download3.stopCheck) {
                    DownloadState.this.forceSkipCheckDownload(download3);
                }
            }
        }
    }
}