package com.android.server.am.frozen;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.ArraySet;
import android.util.SparseArray;
import com.android.server.am.frozen.WorkingStateManager;
import java.util.ArrayList;
import java.util.Iterator;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public abstract class BaseState {
    protected static final boolean DEBUG_STATE = SystemProperties.get("persist.sys.log.ctrl", "no").equals("yes");
    public static final long MAX_DELAYED_TIME = 60000;
    protected static final int MSG_ADD_STATE = 0;
    protected static final int MSG_DEFER_STATE = 1;
    protected static final int MSG_RECYCLE_DEFERRED_STATES = 3;
    protected static final int MSG_REMOVE_STATE = 2;
    public static final long RECYCLE_DEFERRED_STATES_TIME = 12000;
    public static final String TAG = "frozen-state";
    protected final MyHandler mHandler;
    protected final String mName;
    protected final int mState;
    protected final ArrayList<State> mCurrentStates = new ArrayList<>();
    protected final ArrayList<State> mDeferredStates = new ArrayList<>();
    protected final ArraySet<WorkingStateManager.StateChangeListener> mCallbacks = new ArraySet<>();

    public BaseState(Looper looper, int state) {
        this.mState = state;
        this.mName = WorkingStateManager.STATES_NAMES.get(state);
        this.mHandler = new MyHandler(looper);
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

    public int getState() {
        return this.mState;
    }

    public void addState(int uid, long extra) {
        this.mHandler.obtainMessage(0, uid, 0).sendToTarget();
    }

    public void removeUid(int uid) {
        if (findState(uid) != null) {
            this.mHandler.obtainMessage(2, uid, 0).sendToTarget();
        }
    }

    public void deferState(int uid) {
        deferState(uid, false);
    }

    public void deferState(int uid, boolean immediately) {
        if (findState(uid) != null) {
            this.mHandler.obtainMessage(1, uid, immediately ? 1 : 0).sendToTarget();
        }
    }

    public void fill(SparseArray<Integer> uids) {
        recycleDeferredStates();
        synchronized (this) {
            Iterator<State> it = this.mCurrentStates.iterator();
            while (it.hasNext()) {
                State u = it.next();
                int state = uids.get(u.mUid, 0).intValue();
                uids.put(u.mUid, Integer.valueOf(this.mState | state));
            }
        }
    }

    public int getState(int uid) {
        recycleDeferredStates();
        synchronized (this) {
            if (findState(uid) != null) {
                return this.mState;
            }
            return 0;
        }
    }

    public boolean isEmpty() {
        boolean isEmpty;
        recycleDeferredStates();
        synchronized (this) {
            isEmpty = this.mCurrentStates.isEmpty();
        }
        return isEmpty;
    }

    protected State findState(int uid) {
        synchronized (this) {
            Iterator<State> it = this.mCurrentStates.iterator();
            while (it.hasNext()) {
                State u = it.next();
                if (u.mUid == uid) {
                    return u;
                }
            }
            return null;
        }
    }

    protected long deferredTime(long duration) {
        return Math.min(duration / 12, 60000L);
    }

    protected void add(int uid) {
        recycleDeferredStates();
        synchronized (this) {
            State state = findState(uid);
            if (state != null) {
                this.mDeferredStates.remove(state);
                if (DEBUG_STATE) {
                    VSlog.e(TAG, String.format("survive %s %d", this.mName, Integer.valueOf(uid)));
                }
            } else if (DEBUG_STATE) {
                VSlog.e(TAG, String.format("add %s %d", this.mName, Integer.valueOf(uid)));
            }
            onStateAddedLocked(state != null ? state : new State(uid));
        }
    }

    protected void remove(int uid) {
        recycleDeferredStates();
        synchronized (this) {
            State u = findState(uid);
            if (u != null) {
                onStateRemovedLocked(u);
                this.mDeferredStates.remove(u);
                if (DEBUG_STATE) {
                    VSlog.e(TAG, String.format("remove %s %d", this.mName, Integer.valueOf(uid)));
                }
            }
        }
    }

    private void onStateRemovedLocked(State u) {
        this.mCurrentStates.remove(u);
        if (!this.mCallbacks.isEmpty()) {
            Iterator<WorkingStateManager.StateChangeListener> it = this.mCallbacks.iterator();
            while (it.hasNext()) {
                WorkingStateManager.StateChangeListener callback = it.next();
                callback.onStateChanged(this.mState, 0, u.mUid);
            }
        }
    }

    private void onStateAddedLocked(State u) {
        if (!this.mCurrentStates.contains(u)) {
            this.mCurrentStates.add(u);
        }
        if (!this.mCallbacks.isEmpty()) {
            Iterator<WorkingStateManager.StateChangeListener> it = this.mCallbacks.iterator();
            while (it.hasNext()) {
                WorkingStateManager.StateChangeListener callback = it.next();
                callback.onStateChanged(this.mState, 1, u.mUid);
            }
        }
    }

    protected void defer(int uid, boolean immediately) {
        recycleDeferredStates();
        synchronized (this) {
            State state = findState(uid);
            if (state != null) {
                long now = SystemClock.uptimeMillis();
                long delayed = immediately ? -1L : Math.min(deferredTime(now - state.mStartTime), 60000L);
                state.mDeferredTime = now + delayed;
                this.mDeferredStates.add(state);
                if (!immediately) {
                    this.mHandler.sendEmptyMessageDelayed(3, delayed);
                } else {
                    recycleDeferredStates();
                }
                if (DEBUG_STATE) {
                    VSlog.e(TAG, String.format("defer %s %d in %d ms", this.mName, Integer.valueOf(uid), Long.valueOf(delayed)));
                }
            }
        }
    }

    public void recycleDeferredStates() {
        synchronized (this) {
            if (this.mDeferredStates.isEmpty()) {
                return;
            }
            this.mHandler.removeMessages(3);
            long now = SystemClock.uptimeMillis();
            int index = 0;
            while (index < this.mDeferredStates.size()) {
                if (this.mDeferredStates.get(index).mDeferredTime < now) {
                    onStateRemovedLocked(this.mDeferredStates.get(index));
                    if (DEBUG_STATE) {
                        VSlog.e(TAG, String.format("recycle %s %d", this.mName, Integer.valueOf(this.mDeferredStates.get(index).mUid)));
                    }
                    this.mDeferredStates.remove(index);
                    index--;
                }
                index++;
            }
            if (!this.mDeferredStates.isEmpty()) {
                this.mHandler.sendEmptyMessageDelayed(3, RECYCLE_DEFERRED_STATES_TIME);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public class State {
        long mDeferredTime;
        long mStartTime = SystemClock.uptimeMillis();
        int mUid;

        State(int uid) {
            this.mUid = uid;
        }

        public boolean equals(Object obj) {
            return obj instanceof Integer ? this.mUid == ((Integer) obj).intValue() : (obj instanceof State) && this.mUid == ((State) obj).mUid;
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
            int uid = msg.arg1;
            int i = msg.what;
            if (i == 0) {
                BaseState.this.add(uid);
                return;
            }
            if (i == 1) {
                boolean immediately = msg.arg2 == 1;
                BaseState.this.defer(uid, immediately);
            } else if (i == 2) {
                BaseState.this.remove(uid);
            } else if (i == 3) {
                BaseState.this.recycleDeferredStates();
            }
        }
    }
}