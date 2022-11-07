package com.android.server.policy.motion;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.view.MotionEvent;
import android.view.WindowManagerPolicyConstants;
import com.android.server.UnifiedConfigThread;
import com.android.server.wm.WindowManagerService;
import java.util.ArrayList;
import java.util.Calendar;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoInputUsageStatsListener implements WindowManagerPolicyConstants.PointerEventListener {
    private static final int MAX_SAVE_TIME = 7;
    private static final int MSG_PRUNE = 3;
    private static final int MSG_REMOVE_USER = 2;
    private static final int MSG_SAVE = 1;
    private static final int MSG_USER_SWITCH = 4;
    private static final long PRUNE_MIN_MS = 21600000;
    private static final int SAVE_TO_FILE_INTERVEL = 120000;
    private static final String TAG = "VivoInputUsageStatsListener";
    private static long sLastPruneMs;
    private Context mContext;
    private InputUsageDatabase mInputUsageDatabase;
    private Object mLock = new Object();
    private int mCurrentUserId = 0;
    private ArrayMap<Integer, ArrayList<EventEntry>> mListToSaveMap = new ArrayMap<>();
    private long mLastSaveTime = 0;
    private Runnable mForceSaveToFileRunnable = new Runnable() { // from class: com.android.server.policy.motion.VivoInputUsageStatsListener.1
        @Override // java.lang.Runnable
        public void run() {
            synchronized (VivoInputUsageStatsListener.this.mLock) {
                int size = VivoInputUsageStatsListener.this.mListToSaveMap.size();
                for (int i = 0; i < size; i++) {
                    int userId = ((Integer) VivoInputUsageStatsListener.this.mListToSaveMap.keyAt(i)).intValue();
                    VivoInputUsageStatsListener.this.saveEventOfUser(userId);
                }
                VSlog.d(VivoInputUsageStatsListener.TAG, "force push all user data to database," + size + " users");
            }
        }
    };
    private Handler mHandler = new Handler(UnifiedConfigThread.getHandler().getLooper()) { // from class: com.android.server.policy.motion.VivoInputUsageStatsListener.2
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 1) {
                VivoInputUsageStatsListener.this.saveEventOfUser(msg.arg1);
            } else if (i == 2) {
                int removeUserId = msg.arg1;
                VSlog.d(VivoInputUsageStatsListener.TAG, "MSG_REMOVE_USER " + removeUserId);
                synchronized (VivoInputUsageStatsListener.this.mLock) {
                    if (VivoInputUsageStatsListener.this.mListToSaveMap.containsKey(Integer.valueOf(removeUserId))) {
                        VivoInputUsageStatsListener.this.mListToSaveMap.remove(Integer.valueOf(removeUserId));
                    }
                }
                VivoInputUsageStatsListener.this.mInputUsageDatabase.deleteDataOfUser(removeUserId);
            } else if (i != 3) {
                if (i == 4) {
                    int oldUserId = msg.arg1;
                    int newUserId = msg.arg2;
                    VivoInputUsageStatsListener.this.mInputUsageDatabase.createTableForUserIfNotExist(newUserId);
                    VivoInputUsageStatsListener.this.saveEventOfUser(oldUserId);
                }
            } else {
                VSlog.d(VivoInputUsageStatsListener.TAG, "MSG_PRUNE all user data");
                long now = System.currentTimeMillis();
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(now);
                calendar.set(11, 0);
                calendar.set(12, 0);
                calendar.set(13, 0);
                calendar.set(14, 0);
                calendar.add(5, -6);
                long beginTime = calendar.getTimeInMillis();
                VivoInputUsageStatsListener.this.mInputUsageDatabase.deleteDataOutOfTimeForAllUsers(beginTime);
            }
        }
    };

    public VivoInputUsageStatsListener(Context context) {
        this.mContext = context;
        this.mInputUsageDatabase = new InputUsageDatabase(this.mContext);
    }

    public void onSystemReady() {
        WindowManagerService wm = ServiceManager.getService("window");
        wm.registerPointerEventListener(this, 0);
        registerBroadcast();
    }

    public int getScreenOperationCountDaily() {
        int count = 0;
        long now = System.currentTimeMillis();
        long begin = calculate(now);
        int userId = this.mCurrentUserId;
        synchronized (this.mLock) {
            if (this.mListToSaveMap.containsKey(Integer.valueOf(userId))) {
                ArrayList<EventEntry> list = this.mListToSaveMap.get(Integer.valueOf(userId));
                int size = list.size();
                for (int i = 0; i < size; i++) {
                    EventEntry eventEntry = list.get(i);
                    if (eventEntry.timeMs > begin && eventEntry.timeMs < now) {
                        count++;
                    }
                }
            }
        }
        int queryFromDatabase = this.mInputUsageDatabase.queryEvents(0, 0, userId, begin, now);
        return count + queryFromDatabase;
    }

    public void onPointerEvent(MotionEvent event) {
        if (event.getAction() == 0) {
            long now = System.currentTimeMillis();
            EventEntry eventEntry = new EventEntry();
            eventEntry.action = event.getAction();
            eventEntry.displayId = event.getDisplayId();
            eventEntry.timeMs = now;
            synchronized (this.mLock) {
                ArrayList<EventEntry> list = this.mListToSaveMap.get(Integer.valueOf(this.mCurrentUserId));
                if (list == null) {
                    list = new ArrayList<>();
                    this.mListToSaveMap.put(Integer.valueOf(this.mCurrentUserId), list);
                }
                list.add(eventEntry);
            }
            long elapsedTime = SystemClock.elapsedRealtime();
            if (elapsedTime - this.mLastSaveTime > 120000) {
                Message msg = this.mHandler.obtainMessage(1);
                msg.arg1 = this.mCurrentUserId;
                this.mHandler.sendMessage(msg);
            }
            if (this.mHandler.hasCallbacks(this.mForceSaveToFileRunnable)) {
                this.mHandler.removeCallbacks(this.mForceSaveToFileRunnable);
            }
            this.mHandler.postDelayed(this.mForceSaveToFileRunnable, 240000L);
            checkNeedPrune();
        }
    }

    private void registerBroadcast() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        intentFilter.addAction("android.intent.action.USER_REMOVED");
        this.mContext.registerReceiverAsUser(new BroadcastReceiver() { // from class: com.android.server.policy.motion.VivoInputUsageStatsListener.3
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("android.intent.action.USER_SWITCHED".equals(action)) {
                    VivoInputUsageStatsListener.this.switchUser(intent.getIntExtra("android.intent.extra.user_handle", 0));
                } else if ("android.intent.action.USER_REMOVED".equals(action)) {
                    VivoInputUsageStatsListener.this.removeUser(intent.getIntExtra("android.intent.extra.user_handle", 0));
                }
            }
        }, UserHandle.ALL, intentFilter, null, null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void switchUser(int userId) {
        if (this.mCurrentUserId == userId) {
            return;
        }
        Message msg = this.mHandler.obtainMessage(4);
        msg.arg1 = this.mCurrentUserId;
        msg.arg2 = userId;
        this.mHandler.sendMessage(msg);
        this.mCurrentUserId = userId;
        VSlog.d(TAG, "user switch to " + userId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeUser(int userId) {
        Message msg = this.mHandler.obtainMessage(2);
        msg.arg1 = userId;
        this.mHandler.sendMessage(msg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void saveEventOfUser(int userId) {
        synchronized (this.mLock) {
            ArrayList<EventEntry> listToSave = this.mListToSaveMap.get(Integer.valueOf(userId));
            int count = listToSave == null ? 0 : listToSave.size();
            if (count <= 0) {
                return;
            }
            this.mInputUsageDatabase.writeEventList(listToSave, userId);
            listToSave.clear();
            this.mLastSaveTime = SystemClock.elapsedRealtime();
        }
    }

    private void checkNeedPrune() {
        long elapsedTime = SystemClock.elapsedRealtime();
        if (elapsedTime - sLastPruneMs > 21600000) {
            sLastPruneMs = elapsedTime;
            this.mHandler.sendEmptyMessage(3);
        }
    }

    private long calculate(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        calendar.set(11, 0);
        calendar.set(12, 0);
        calendar.set(13, 0);
        calendar.set(14, 0);
        return calendar.getTimeInMillis();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class EventEntry {
        int action;
        int displayId;
        long timeMs;

        EventEntry() {
        }
    }
}