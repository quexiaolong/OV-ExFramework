package com.android.server.clipboard;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.os.Binder;
import android.os.FtBuild;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IUserManager;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;
import android.view.IWindowManager;
import android.view.Window;
import android.view.WindowManager;
import android.widget.IVigourTierConfig;
import com.android.server.LocalServices;
import com.android.server.wm.WindowManagerInternal;
import com.vivo.common.utils.VLog;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Timer;
import vivo.app.clipboard.ClipBoardHistoryDataHelper;
import vivo.app.clipboard.ClipboardData;
import vivo.app.clipboard.ClipboardDataAdapter;
import vivo.app.clipboard.FuzzyUtils;
import vivo.app.clipboard.IClipboardDialogListener;
import vivo.app.clipboard.VivoClipboardGridDialog;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoClipboardServiceImpl implements IVivoClipboardService {
    private static final String ALLOW_TO_SHOW_CLIPBOARD = "1";
    private static final String CLIPBOARD = "show_clipboard";
    private static final int MAX_DATA_DEADLINE = 3600000;
    private static final int MAX_HISTORY_COUNT = 20;
    private static final String TAG = "VivoClipboardServiceImpl";
    private static final long WRITE_DATA_DELAY_TIME = 8000;
    private VivoClipboardGridDialog.OnApplyClipboardVisibileCallback applyClipboardVisibleCallback;
    private Runnable initHistoryClipdata;
    private ClipBoardHistoryDataHelper mClipBoardHistoryDataHelper;
    private ClipboardDataAdapter mClipboardDataAdapter;
    private IClipboardDialogListener mClipboardDialogListener;
    private ClipboardService mClipboardService;
    private Handler mClipsHandler;
    private HandlerThread mClipsThread;
    private Context mContext;
    private IBinder mCurToken;
    private DevicePolicyManager mDpm;
    private IWindowManager mIWindowManager;
    private String mNoteImageString;
    private String mPackageName;
    private IBinder mTextViewWindowToken;
    private Timer mTimer;
    private IUserManager mUm;
    private int mUserId;
    private VivoClipboardGridDialog mVivoClipboardGridDialog;
    boolean mVivoClipboradEnable;
    private WindowManagerInternal mWindowManagerInternal;
    private static boolean onStartShowing = false;
    private static final Object mStartLock = new Object();
    private LinkedList<ClipboardData> historyClips = null;
    private boolean isWriteTaskRunning = false;

    public VivoClipboardServiceImpl(ClipboardService service) {
        boolean z = false;
        this.mVivoClipboradEnable = (FtBuild.getRomVersion() >= 5.0f || IVigourTierConfig.CLIPBOARD_VOS_2_1) ? true : true;
        this.mNoteImageString = "__END_OF_PART__IMG_";
        this.mContext = null;
        this.mUserId = -1;
        this.mClipsThread = null;
        this.mClipsHandler = null;
        this.mTextViewWindowToken = null;
        this.mWindowManagerInternal = null;
        this.initHistoryClipdata = new Runnable() { // from class: com.android.server.clipboard.VivoClipboardServiceImpl.1
            @Override // java.lang.Runnable
            public void run() {
                if (VivoClipboardServiceImpl.this.mClipBoardHistoryDataHelper != null) {
                    VivoClipboardServiceImpl vivoClipboardServiceImpl = VivoClipboardServiceImpl.this;
                    vivoClipboardServiceImpl.historyClips = vivoClipboardServiceImpl.mClipBoardHistoryDataHelper.readDataFromFile();
                    if (IVigourTierConfig.CLIPBOARD_VOS_2_1) {
                        VivoClipboardServiceImpl.this.deleteDeadlineClipData();
                    }
                    VivoClipboardServiceImpl.this.mClipboardDataAdapter.setClipDatasList(VivoClipboardServiceImpl.this.historyClips);
                }
            }
        };
        this.applyClipboardVisibleCallback = new VivoClipboardGridDialog.OnApplyClipboardVisibileCallback() { // from class: com.android.server.clipboard.VivoClipboardServiceImpl.6
            public void onApplyClipboardVisible() {
                if (VivoClipboardServiceImpl.this.mWindowManagerInternal != null && VivoClipboardServiceImpl.this.mTextViewWindowToken != null) {
                    VivoClipboardServiceImpl.this.mWindowManagerInternal.showImePostLayout(VivoClipboardServiceImpl.this.mTextViewWindowToken);
                }
            }

            public void onHideClipboard() {
                if (VivoClipboardServiceImpl.this.mWindowManagerInternal != null && VivoClipboardServiceImpl.this.mTextViewWindowToken != null) {
                    VivoClipboardServiceImpl.this.mWindowManagerInternal.hideIme(VivoClipboardServiceImpl.this.mTextViewWindowToken, 0);
                }
            }
        };
        this.mClipboardService = service;
    }

    public void clipboardInit(Context context, int userId) {
        this.mContext = context;
        this.mCurToken = new Binder();
        this.mTimer = new Timer();
        this.mIWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        this.mUm = ServiceManager.getService("user");
        this.mUserId = userId;
        VLog.d(TAG, "clipboardInit mUserId: " + this.mUserId);
        this.mClipBoardHistoryDataHelper = new ClipBoardHistoryDataHelper(context);
        this.mClipboardDataAdapter = new ClipboardDataAdapter(context);
        new Thread(this.initHistoryClipdata).start();
        initClipsWriteThread();
        this.mDpm = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        this.mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() { // from class: com.android.server.clipboard.VivoClipboardServiceImpl.2
            @Override // java.lang.Runnable
            public void run() {
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.LOCALE_CHANGED");
                LanguageChangedReceiver mReceiver = new LanguageChangedReceiver();
                VivoClipboardServiceImpl.this.mContext.registerReceiver(mReceiver, filter);
                if (IVigourTierConfig.CLIPBOARD_VOS_2_1) {
                    IntentFilter timefilter = new IntentFilter();
                    timefilter.addAction("android.intent.action.TIME_SET");
                    TimeChangedReceiver timeChangedReceiver = new TimeChangedReceiver();
                    VivoClipboardServiceImpl.this.mContext.registerReceiver(timeChangedReceiver, timefilter);
                }
            }
        });
    }

    public void setPrimaryClip(ClipData clip, String callingPackage) {
        this.mClipboardService.mWm.notifyPrimaryClip(clip);
        try {
            if (this.mVivoClipboradEnable) {
                boolean isNote = "com.android.notes".equals(callingPackage);
                String data = clip.getItemAt(0).getText().toString();
                if (isNote && data.indexOf(this.mNoteImageString) != -1) {
                    return;
                }
                setHistoryClipsInternal(clip);
            }
        } catch (Exception e) {
        }
    }

    public void writePropertyToSetting() {
        long identity = Binder.clearCallingIdentity();
        Settings.Global.putString(this.mContext.getContentResolver(), CLIPBOARD, "1");
        Binder.restoreCallingIdentity(identity);
    }

    public void writeDataToSetting(String key, String value) {
        long identity = Binder.clearCallingIdentity();
        Settings.Global.putString(this.mContext.getContentResolver(), key, value);
        Binder.restoreCallingIdentity(identity);
    }

    public void setClipboardListener(IClipboardDialogListener listener) {
        this.mClipboardDialogListener = listener;
        VivoClipboardGridDialog vivoClipboardGridDialog = this.mVivoClipboardGridDialog;
        if (vivoClipboardGridDialog != null) {
            vivoClipboardGridDialog.setClipboardDialogListener(listener);
        }
    }

    public void showClipboardDialog(final IBinder windowToken) {
        synchronized (mStartLock) {
            onStartShowing = true;
        }
        if (IVigourTierConfig.CLIPBOARD_VOS_2_1) {
            deleteDeadlineClipData();
        }
        this.mTextViewWindowToken = windowToken;
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() { // from class: com.android.server.clipboard.VivoClipboardServiceImpl.3
            @Override // java.lang.Runnable
            public void run() {
                try {
                    VivoClipboardServiceImpl.this.mIWindowManager.addWindowToken(VivoClipboardServiceImpl.this.mCurToken, 2011, 0);
                } catch (RemoteException e) {
                    VSlog.wtf(VivoClipboardServiceImpl.TAG, "RemoteException: Failed to addWindowToken");
                }
                VivoClipboardServiceImpl.this.mVivoClipboardGridDialog = new VivoClipboardGridDialog(VivoClipboardServiceImpl.this.mContext);
                Window window = VivoClipboardServiceImpl.this.mVivoClipboardGridDialog.getWindow();
                WindowManager.LayoutParams attrs = window.getAttributes();
                attrs.token = VivoClipboardServiceImpl.this.mCurToken;
                window.setAttributes(attrs);
                VivoClipboardServiceImpl.this.mVivoClipboardGridDialog.setClipboardAdapter(VivoClipboardServiceImpl.this.mClipboardDataAdapter);
                VivoClipboardServiceImpl.this.mVivoClipboardGridDialog.setClipboardDialogListener(VivoClipboardServiceImpl.this.mClipboardDialogListener);
                VivoClipboardServiceImpl.this.mVivoClipboardGridDialog.setTextViewWindowToken(windowToken);
                VivoClipboardServiceImpl.this.mVivoClipboardGridDialog.show(VivoClipboardServiceImpl.this.applyClipboardVisibleCallback);
                synchronized (VivoClipboardServiceImpl.mStartLock) {
                    boolean unused = VivoClipboardServiceImpl.onStartShowing = false;
                }
            }
        });
    }

    public void hideClipboardDialog() {
        VivoClipboardGridDialog vivoClipboardGridDialog = this.mVivoClipboardGridDialog;
        if (vivoClipboardGridDialog != null && vivoClipboardGridDialog.isShowing()) {
            this.mVivoClipboardGridDialog.setClipboardDialogListener((IClipboardDialogListener) null);
            this.mVivoClipboardGridDialog.dismissAllDialog();
            this.mVivoClipboardGridDialog = null;
        }
    }

    public boolean shouldShowClipboardDialog(int userId) {
        onCheckUserChanged(userId);
        if (this.historyClips.isEmpty() || isClipboardDialogShowing()) {
            return false;
        }
        return true;
    }

    public boolean isClipboardDialogShowing() {
        VivoClipboardGridDialog vivoClipboardGridDialog = this.mVivoClipboardGridDialog;
        if (vivoClipboardGridDialog == null) {
            return false;
        }
        if (vivoClipboardGridDialog.isShowing()) {
            return true;
        }
        synchronized (mStartLock) {
            return onStartShowing;
        }
    }

    public void writeClipdataToFile() {
        VSlog.wtf(TAG, "writeClipdataToFile@");
        writeClipdatatoHistoryFileInternal();
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public void setPackageName(String packageName) {
        this.mPackageName = packageName;
    }

    private void setHistoryClipsInternal(ClipData clip) {
        String checked = Settings.Global.getString(this.mContext.getContentResolver(), VivoClipboardGridDialog.getSwitchChecked());
        if (clip == null || this.historyClips == null) {
            return;
        }
        if (IVigourTierConfig.CLIPBOARD_VOS_2_1 && !VivoClipboardGridDialog.getSwitchChecked().equals(checked)) {
            return;
        }
        ClipboardData hisClipData = new ClipboardData(clip, false, this.mContext);
        if (isClipDataDoNotContainCharacter(hisClipData)) {
            return;
        }
        if (IVigourTierConfig.CLIPBOARD_VOS_2_1) {
            deleteDeadlineClipData();
        }
        deleteDuplicateData(hisClipData);
        addClipDataToHistoryList(hisClipData);
        writeClipdatatoHistoryFileInternal();
        if (this.mClipboardDataAdapter != null) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() { // from class: com.android.server.clipboard.VivoClipboardServiceImpl.4
                @Override // java.lang.Runnable
                public void run() {
                    VivoClipboardServiceImpl.this.mClipboardDataAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    private boolean isClipDataDoNotContainCharacter(ClipboardData clip) {
        String str = clip.getClipDataText().toString().trim();
        if (str.isEmpty()) {
            return true;
        }
        return false;
    }

    private void deleteDuplicateData(ClipboardData clip) {
        int index = 0;
        boolean exitDumplicateData = false;
        Iterator<ClipboardData> it = this.historyClips.iterator();
        while (it.hasNext()) {
            ClipboardData clipdata = it.next();
            String newClipData = clip.getClipDataText().toString().trim();
            String temp = clipdata.getClipDataText().toString().trim();
            if (newClipData.equals(temp)) {
                index = this.historyClips.indexOf(clipdata);
                exitDumplicateData = true;
            }
        }
        if (exitDumplicateData) {
            boolean islocked = this.historyClips.get(index).getClipDataLockState();
            clip.setClipDataLockState(islocked);
            if (IVigourTierConfig.CLIPBOARD_VOS_2_1) {
                clip.setFuzzyText(this.historyClips.get(index).getFuzzyText());
            }
            this.historyClips.remove(index);
        } else if (IVigourTierConfig.CLIPBOARD_VOS_2_1) {
            generateFuzzyText(clip);
        }
    }

    public void deleteDeadlineClipData() {
        if (this.historyClips == null) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        synchronized (mStartLock) {
            int i = 0;
            while (i < this.historyClips.size()) {
                ClipboardData temp = this.historyClips.get(i);
                if (currentTime - temp.getStartAgedTime() >= 3600000 && !temp.getClipDataLockState()) {
                    this.historyClips.remove(i);
                    i--;
                }
                i++;
            }
        }
    }

    private void generateFuzzyText(ClipboardData hisClipData) {
        String temp = hisClipData.getClipDataText().toString();
        String fuzzyText = FuzzyUtils.middleReplaceStar(temp);
        hisClipData.setFuzzyText(fuzzyText);
    }

    private void addClipDataToHistoryList(ClipboardData hisClipData) {
        int count = this.historyClips.size();
        if (count < 20) {
            this.historyClips.add(hisClipData);
            return;
        }
        int index = 0;
        Iterator<ClipboardData> it = this.historyClips.iterator();
        while (it.hasNext()) {
            ClipboardData clipdata = it.next();
            if (!clipdata.getClipDataLockState()) {
                break;
            }
            index++;
        }
        if (index < this.historyClips.size()) {
            this.historyClips.remove(index);
            this.historyClips.add(hisClipData);
        }
    }

    private void writeClipdatatoHistoryFileInternal() {
        sendClipsWriteMessage(this.mClipBoardHistoryDataHelper, this.historyClips, this.mUserId);
    }

    public void onCheckUserChanged(int userId) {
        VLog.d(TAG, "onCheckUserChanged userId: " + userId + "mUserId: " + this.mUserId);
        int i = this.mUserId;
        if (i != userId) {
            int oldProfileUserId = getProfileUserId(i);
            int newProfileUserId = getProfileUserId(userId);
            this.mUserId = userId;
            VLog.d(TAG, "onCheckUserChanged oldProfileUserId: " + oldProfileUserId + "newProfileUserId: " + newProfileUserId);
            if (oldProfileUserId != newProfileUserId) {
                onUserChanged(userId, newProfileUserId);
            }
        }
    }

    public int getProfileUserId(int userId) {
        UserInfo info = null;
        int pgid = userId;
        long origId = Binder.clearCallingIdentity();
        try {
            try {
                info = this.mUm.getUserInfo(userId);
            } catch (RemoteException e) {
                VSlog.e(TAG, "Remote Exception calling UserManager.getUserInfo: ", e);
            }
            if (info != null) {
                if (userId == info.profileGroupId || info.profileGroupId < 0) {
                    pgid = userId;
                } else {
                    boolean canCopy = !hasRestriction("no_cross_profile_copy_paste", info.profileGroupId);
                    boolean canShare = !hasRestriction("no_sharing_into_profile", userId);
                    if (canCopy && canShare) {
                        pgid = info.profileGroupId;
                    }
                }
            }
            VLog.d(TAG, "getParentUserId userId: " + userId + " parentGroupId: " + pgid);
            return pgid;
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    private boolean hasRestriction(String restriction, int userId) {
        try {
            return this.mUm.hasUserRestriction(restriction, userId);
        } catch (RemoteException e) {
            VSlog.e(TAG, "Remote Exception calling UserManager.getUserRestrictions: ", e);
            return true;
        }
    }

    public void onUserChanged(int userId, int parentUserId) {
        VLog.d(TAG, "onUserChanged userId: " + userId + " parentUserId: " + parentUserId);
        if (parentUserId >= 0) {
            this.mClipBoardHistoryDataHelper = new ClipBoardHistoryDataHelper(this.mContext, parentUserId);
        } else {
            this.mClipBoardHistoryDataHelper = new ClipBoardHistoryDataHelper(this.mContext, userId);
        }
        this.mClipboardDataAdapter = new ClipboardDataAdapter(this.mContext);
        LinkedList<ClipboardData> readDataFromFile = this.mClipBoardHistoryDataHelper.readDataFromFile();
        this.historyClips = readDataFromFile;
        this.mClipboardDataAdapter.setClipDatasList(readDataFromFile);
        this.mVivoClipboardGridDialog = null;
    }

    private void initClipsWriteThread() {
        HandlerThread handlerThread = new HandlerThread("Clips-Thread");
        this.mClipsThread = handlerThread;
        handlerThread.start();
        this.mClipsHandler = new Handler(this.mClipsThread.getLooper()) { // from class: com.android.server.clipboard.VivoClipboardServiceImpl.5
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                VivoClipboardServiceImpl.this.handleClipsWriteMessage(msg);
            }
        };
        VLog.d(TAG, "initClipsWriteThread");
    }

    private void sendClipsWriteMessage(ClipBoardHistoryDataHelper clip, LinkedList<ClipboardData> data, int userId) {
        VLog.d(TAG, "sendClipsWriteMessage userId: " + userId);
        this.mClipsHandler.removeMessages(userId);
        ClipsWriteBean clips = new ClipsWriteBean(clip, data);
        Message msg = this.mClipsHandler.obtainMessage();
        msg.what = userId;
        msg.obj = clips;
        this.mClipsHandler.sendMessageDelayed(msg, WRITE_DATA_DELAY_TIME);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleClipsWriteMessage(Message msg) {
        int userId = msg.what;
        ClipsWriteBean clips = (ClipsWriteBean) msg.obj;
        VLog.d(TAG, "handleClipsWriteMessage userId: " + userId);
        clips.getClip().writeDataToFile(clips.getData());
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ClipsWriteBean {
        private ClipBoardHistoryDataHelper mClip;
        private LinkedList<ClipboardData> mData;

        public ClipsWriteBean(ClipBoardHistoryDataHelper clip, LinkedList<ClipboardData> data) {
            this.mClip = null;
            this.mData = null;
            this.mClip = clip;
            LinkedList<ClipboardData> linkedList = new LinkedList<>();
            this.mData = linkedList;
            linkedList.addAll(data);
        }

        public ClipBoardHistoryDataHelper getClip() {
            return this.mClip;
        }

        public LinkedList<ClipboardData> getData() {
            return this.mData;
        }
    }

    /* loaded from: classes.dex */
    private class LanguageChangedReceiver extends BroadcastReceiver {
        private LanguageChangedReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (VivoClipboardServiceImpl.this.mVivoClipboradEnable && intent.getAction().equals("android.intent.action.LOCALE_CHANGED")) {
                VivoClipboardServiceImpl.this.mVivoClipboardGridDialog = null;
            }
        }
    }

    /* loaded from: classes.dex */
    private class TimeChangedReceiver extends BroadcastReceiver {
        private TimeChangedReceiver() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.intent.action.TIME_SET") && IVigourTierConfig.CLIPBOARD_VOS_2_1) {
                VivoClipboardServiceImpl.this.deleteDeadlineClipData();
            }
        }
    }

    public boolean checkPolicyPermisson(int callingUid) {
        int userId = UserHandle.getUserId(callingUid);
        long origId = Binder.clearCallingIdentity();
        DevicePolicyManager devicePolicyManager = this.mDpm;
        if (devicePolicyManager == null) {
            return true;
        }
        try {
            try {
                int type = devicePolicyManager.getCustomType();
                if (type > 0) {
                    int policy = this.mDpm.getRestrictionPolicy(null, 306, userId);
                    if (policy == 1) {
                        return false;
                    }
                }
            } catch (Exception e) {
                Slog.i(TAG, "clipboardAccessAllowed Remote Exception calling dpm: " + e);
            }
            return true;
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }
}