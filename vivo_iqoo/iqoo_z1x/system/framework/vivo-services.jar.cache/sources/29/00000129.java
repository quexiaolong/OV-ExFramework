package com.android.server.am.frozen;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.SparseArray;
import com.android.server.IVivoWorkingState;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class WorkingStateManager implements IVivoWorkingState {
    public static final String AUDIO_NAME = "audio";
    public static final String BG_GAME_NAME = "gb game";
    public static final int DOWNLOADING = 1;
    public static final String DOWNLOAD_NAME = "download";
    public static final String FLOAT_WINDOW_NAME = "floatwindow";
    private static final int FROZEN_STATE_MASK = 24895;
    private static final int FROZEN_STATE_MASK_BY_UID = 306;
    private static final String KEY_MODEL = "model";
    private static final String KEY_PID = "pid";
    private static final String KEY_PKG = "pkg";
    private static final String KEY_STATE = "state";
    private static final String KEY_UID = "uid";
    public static final String NAVIGATION_NAME = "navigation";
    public static final int NONE = 0;
    public static final String NONE_NAME = "none";
    public static final int NO_DOWNLOADING = 0;
    public static final String RECORD_NAME = "record";
    public static final SparseArray<String> STATES_NAMES;
    public static final int UNKNOW = 2;
    private static final int UNKNOWN = -1;
    private static final int WHAT_AUDIO_STATE_CHANGED = 5;
    private static final int WHAT_BUNDLE = 4;
    private static final int WHAT_PID = 2;
    private static final int WHAT_PKG = 3;
    private static final int WHAT_UID = 1;
    private final String TAG;
    private final AudioState mAudioState;
    private final HashSet<String> mCheckGPSList;
    private final DownloadState mDownloadState;
    private final BgHandler mHandler;
    private final long mInitDownloadTime;
    private long mLastTimeQuery;
    private final ArrayList<StateChangeListener> mListeners;
    private final byte[] mLock;
    private final SparseArray<String> mUidPkgCache;
    private final HashSet<String> mUncheckAudioList;
    private final HashSet<String> mUncheckDownloadList;

    /* loaded from: classes.dex */
    public interface StateChangeListener {
        void onStateChanged(int i, int i2, int i3);

        void onStateChanged(int i, int i2, int i3, int i4);

        void onStateChanged(int i, int i2, int i3, String str);
    }

    static {
        SparseArray<String> sparseArray = new SparseArray<>();
        STATES_NAMES = sparseArray;
        sparseArray.put(1, "record");
        STATES_NAMES.put(2, "audio");
        STATES_NAMES.put(8, "download");
        STATES_NAMES.put(4, FLOAT_WINDOW_NAME);
        STATES_NAMES.put(32, "navigation");
        STATES_NAMES.put(16384, BG_GAME_NAME);
    }

    private WorkingStateManager() {
        this.mUncheckAudioList = new HashSet<>();
        this.mUncheckDownloadList = new HashSet<>();
        this.mCheckGPSList = new HashSet<>();
        this.mUidPkgCache = new SparseArray<>(128);
        this.mListeners = new ArrayList<>();
        this.mLock = new byte[0];
        this.mInitDownloadTime = System.currentTimeMillis();
        this.mLastTimeQuery = 0L;
        this.TAG = "quickfrozen";
        HandlerThread ht = new HandlerThread("WorkingStateManager");
        ht.start();
        this.mHandler = new BgHandler(ht.getLooper());
        this.mAudioState = new AudioState(this.mHandler.getLooper(), 2);
        this.mDownloadState = new DownloadState(this.mHandler.getLooper(), 8);
    }

    public static WorkingStateManager getInstance() {
        return Holder.INSTANCE;
    }

    public void beginCheckDownloadStatus(int uid, String packageName, boolean hasFgService) {
        this.mDownloadState.beginCheckState(uid, packageName, hasFgService, skipCheckDownloadState(packageName, uid));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void uidRunning(int uid, String pkgName) {
        if (UserHandle.isApp(uid) && !TextUtils.isEmpty(pkgName)) {
            synchronized (this.mUidPkgCache) {
                this.mUidPkgCache.put(uid, pkgName);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void uidStopped(int uid) {
        synchronized (this.mUidPkgCache) {
            this.mUidPkgCache.remove(uid);
        }
    }

    public void stopCheckDownloadStatus(int uid, String packageName) {
        this.mDownloadState.StopCheckState(uid, packageName);
    }

    public void init(Context context) {
    }

    public void registerListener(StateChangeListener l) {
        synchronized (this.mListeners) {
            if (l != null) {
                if (!this.mListeners.contains(l)) {
                    this.mListeners.add(l);
                }
            }
            if (this.mAudioState != null) {
                this.mAudioState.addCallback(l);
            }
            this.mDownloadState.addCallback(l);
        }
    }

    public void unregisterListener(StateChangeListener l) {
        synchronized (this.mListeners) {
            this.mListeners.remove(l);
            if (this.mAudioState != null) {
                this.mAudioState.removeCallback(l);
            }
            this.mDownloadState.removeCallback(l);
        }
    }

    public void updateWorkingStateByRms(String pkgName, int uid, int mask, boolean state) {
        if (uid >= 10000 && (mask & 1) != 0) {
            setState(1, state ? 1 : 0, uid, pkgName);
        }
    }

    public void setNetflowThreshold(long threshold) {
        DownloadState downloadState = this.mDownloadState;
        if (downloadState != null) {
            downloadState.setNetflowThreshold(threshold);
        }
    }

    public void setState(int model, int state, int uid) {
        if (model == 2) {
            scheduleAudioStateChanged(state, uid);
        } else if ((model != 32 || checkNavigationState(null, uid)) && (model & FROZEN_STATE_MASK_BY_UID) != 0) {
            BgHandler bgHandler = this.mHandler;
            bgHandler.sendMessage(bgHandler.obtainMessage(1, model, state, Integer.valueOf(uid)));
        }
    }

    private void scheduleAudioStateChanged(int state, int uid) {
        BgHandler bgHandler = this.mHandler;
        bgHandler.sendMessage(Message.obtain(bgHandler, 5, state, uid));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleAudioStateChanged(int state, int uid) {
        if (skipCheckAudioState(null, uid)) {
            return;
        }
        if (state == 1) {
            this.mAudioState.addState(uid, 0L);
        } else if (state == 0) {
            this.mAudioState.deferState(uid, true ^ isPerceptible(uid));
        }
    }

    public void setState(int model, int state, int uid, int pid) {
        if (model == 2) {
            if (skipCheckAudioState(null, uid)) {
                return;
            }
            if (state == 1) {
                this.mAudioState.addState(uid, 0L);
            } else if (state == 0) {
                this.mAudioState.deferState(uid, true ^ isPerceptible(uid));
            }
        } else if ((model != 32 || checkNavigationState(null, uid)) && (model & FROZEN_STATE_MASK) != 0) {
            Message msg = this.mHandler.obtainMessage(2);
            Bundle b = new Bundle();
            b.putInt(KEY_MODEL, model);
            b.putInt(KEY_STATE, state);
            b.putInt(KEY_UID, uid);
            b.putInt(KEY_PID, pid);
            msg.setData(b);
            this.mHandler.sendMessage(msg);
        }
    }

    public void setState(int model, int state, int uid, String pkgName) {
        if (model == 2) {
            if (skipCheckAudioState(pkgName, uid)) {
                return;
            }
            if (state == 1) {
                this.mAudioState.addState(uid, 0L);
            } else if (state == 0) {
                this.mAudioState.deferState(uid, true ^ isPerceptible(uid));
            }
        } else if ((model != 32 || checkNavigationState(pkgName, uid)) && (model & FROZEN_STATE_MASK) != 0) {
            Message msg = this.mHandler.obtainMessage(3);
            Bundle b = new Bundle();
            b.putInt(KEY_MODEL, model);
            b.putInt(KEY_STATE, state);
            b.putInt(KEY_UID, uid);
            b.putString(KEY_PKG, pkgName);
            msg.setData(b);
            this.mHandler.sendMessage(msg);
        }
    }

    public void setState(int model, Bundle b) {
        Message msg = this.mHandler.obtainMessage(4);
        b.putInt(KEY_MODEL, model);
        msg.setData(b);
        this.mHandler.sendMessage(msg);
    }

    private String getPackageName(int uid, int pid) {
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Holder {
        private static final WorkingStateManager INSTANCE = new WorkingStateManager();

        private Holder() {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class BgHandler extends Handler {
        public BgHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 1) {
                int model = msg.arg1;
                int state = msg.arg2;
                int uid = ((Integer) msg.obj).intValue();
                notifyStateChanged(model, state, uid);
            } else if (i == 2) {
                Bundle b = msg.getData();
                notifyStateChanged(b.getInt(WorkingStateManager.KEY_MODEL), b.getInt(WorkingStateManager.KEY_STATE), b.getInt(WorkingStateManager.KEY_UID), b.getInt(WorkingStateManager.KEY_PID));
            } else if (i == 3) {
                Bundle b2 = msg.getData();
                notifyStateChanged(b2.getInt(WorkingStateManager.KEY_MODEL), b2.getInt(WorkingStateManager.KEY_STATE), b2.getInt(WorkingStateManager.KEY_UID), b2.getString(WorkingStateManager.KEY_PKG));
            } else if (i == 4) {
                Bundle b3 = msg.getData();
                notifyStateChanged(b3.getInt(WorkingStateManager.KEY_MODEL), b3.getInt(WorkingStateManager.KEY_STATE), -1, b3.getInt(WorkingStateManager.KEY_PID));
            } else if (i == 5) {
                WorkingStateManager.this.handleAudioStateChanged(msg.arg1, msg.arg2);
            }
        }

        private void notifyStateChanged(int model, int state, int uid) {
            synchronized (WorkingStateManager.this.mListeners) {
                Iterator it = WorkingStateManager.this.mListeners.iterator();
                while (it.hasNext()) {
                    StateChangeListener listener = (StateChangeListener) it.next();
                    listener.onStateChanged(model, state, uid);
                }
            }
        }

        private void notifyStateChanged(int model, int state, int uid, int pid) {
            synchronized (WorkingStateManager.this.mListeners) {
                Iterator it = WorkingStateManager.this.mListeners.iterator();
                while (it.hasNext()) {
                    StateChangeListener listener = (StateChangeListener) it.next();
                    listener.onStateChanged(model, state, uid, pid);
                }
            }
        }

        private void notifyStateChanged(int model, int state, int uid, String pkgName) {
            synchronized (WorkingStateManager.this.mListeners) {
                Iterator it = WorkingStateManager.this.mListeners.iterator();
                while (it.hasNext()) {
                    StateChangeListener listener = (StateChangeListener) it.next();
                    listener.onStateChanged(model, state, uid, pkgName);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean setPackageList(String type, Bundle data) {
        ArrayList<String> list;
        if (!TextUtils.isEmpty(type) && data != null && (list = data.getStringArrayList("list")) != null && !list.isEmpty()) {
            char c = 65535;
            int hashCode = type.hashCode();
            if (hashCode != 93166550) {
                if (hashCode != 1427818632) {
                    if (hashCode == 1862666772 && type.equals("navigation")) {
                        c = 2;
                    }
                } else if (type.equals("download")) {
                    c = 1;
                }
            } else if (type.equals("audio")) {
                c = 0;
            }
            if (c == 0) {
                synchronized (this.mUncheckAudioList) {
                    this.mUncheckAudioList.clear();
                    this.mUncheckAudioList.addAll(list);
                    VSlog.i("quickfrozen", "setPackageList, type = " + type + ", pkg list size = " + list.size());
                }
                return true;
            } else if (c == 1) {
                synchronized (this.mUncheckDownloadList) {
                    this.mUncheckDownloadList.clear();
                    this.mUncheckDownloadList.addAll(list);
                    VSlog.i("quickfrozen", "setPackageList, type = " + type + ", pkg list size = " + list.size());
                }
                return true;
            } else if (c == 2) {
                synchronized (this.mCheckGPSList) {
                    this.mCheckGPSList.clear();
                    this.mCheckGPSList.addAll(list);
                    VSlog.i("quickfrozen", "setPackageList, type = " + type + ", pkg list size = " + list.size());
                }
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public HashSet<String> getPackageList(String type) {
        char c;
        HashSet<String> hashSet;
        HashSet<String> hashSet2;
        HashSet<String> hashSet3;
        int hashCode = type.hashCode();
        if (hashCode == 93166550) {
            if (type.equals("audio")) {
                c = 0;
            }
            c = 65535;
        } else if (hashCode != 1427818632) {
            if (hashCode == 1862666772 && type.equals("navigation")) {
                c = 2;
            }
            c = 65535;
        } else {
            if (type.equals("download")) {
                c = 1;
            }
            c = 65535;
        }
        if (c == 0) {
            synchronized (this.mUncheckAudioList) {
                hashSet = this.mUncheckAudioList;
            }
            return hashSet;
        } else if (c == 1) {
            synchronized (this.mUncheckDownloadList) {
                hashSet2 = this.mUncheckDownloadList;
            }
            return hashSet2;
        } else if (c == 2) {
            synchronized (this.mCheckGPSList) {
                hashSet3 = this.mCheckGPSList;
            }
            return hashSet3;
        } else {
            return new HashSet<>();
        }
    }

    private boolean skipCheckAudioState(String pkgName, int uid) {
        boolean skip;
        if (UserHandle.isApp(uid)) {
            if (TextUtils.isEmpty(pkgName)) {
                synchronized (this.mUidPkgCache) {
                    pkgName = this.mUidPkgCache.get(uid);
                }
            }
            if (TextUtils.isEmpty(pkgName)) {
                return false;
            }
            synchronized (this.mUncheckAudioList) {
                skip = this.mUncheckAudioList.contains(pkgName);
                if (skip) {
                    VSlog.i("quickfrozen", "skipCheckAudioState for uid:[" + uid + "], pkg:[" + pkgName + "]");
                }
            }
            return skip;
        }
        return false;
    }

    private boolean skipCheckDownloadState(String pkgName, int uid) {
        boolean skip;
        if (!UserHandle.isApp(uid)) {
            return false;
        }
        synchronized (this.mUncheckDownloadList) {
            skip = this.mUncheckDownloadList.contains(pkgName);
            if (skip) {
                VSlog.i("quickfrozen", "skipCheckDownloadState for uid:[" + uid + "], pkg:[" + pkgName + "]");
            }
        }
        return skip;
    }

    private boolean checkNavigationState(String pkgName, int uid) {
        boolean check;
        if (UserHandle.isApp(uid)) {
            if (TextUtils.isEmpty(pkgName)) {
                synchronized (this.mUidPkgCache) {
                    pkgName = this.mUidPkgCache.get(uid);
                }
            }
            if (TextUtils.isEmpty(pkgName)) {
                return false;
            }
            synchronized (this.mCheckGPSList) {
                check = this.mCheckGPSList.contains(pkgName);
                if (check) {
                    VSlog.i("quickfrozen", "checkNavigationState for uid:[" + uid + "], pkg:[" + pkgName + "]");
                }
            }
            return check;
        }
        return false;
    }

    private boolean isPerceptible(int uid) {
        return FrozenQuicker.getInstance().isPerceptible(uid);
    }
}