package com.vivo.services.vgc.cbs;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.graphics.common.V1_0.Dataspace;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import com.android.internal.app.LocalePicker;
import com.android.internal.util.DumpUtils;
import com.android.server.ServiceThread;
import com.vivo.face.common.data.Constants;
import com.vivo.services.rms.sdk.Consts;
import com.vivo.services.vgc.VivoVgcService;
import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import vivo.app.vgc.IVgcBootPhaseListener;
import vivo.app.vgc.IVivoCbsService;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoCbsServiceImpl extends IVivoCbsService.Stub {
    public static final String ACTION_NOTIF_CLICK = "vgc_cbs_notif_click";
    private static final String CHANNEL_GROUP_ID = "group";
    private static final String CHANNEL_ID = "id";
    public static final int MSG_ACTION_HANDLE_CUSTOMIZE = 8;
    public static final int MSG_ACTION_MAPKEY_CHANGED = 7;
    public static final int MSG_ACTION_REBOOT = 6;
    public static final int MSG_UI_DISMISS_DIALOG = 3;
    public static final int MSG_UI_INIT_VIEW = 1;
    public static final int MSG_UI_REMOVE_NOTIFICATION = 5;
    public static final int MSG_UI_SHOW_DIALOG = 2;
    public static final int MSG_UI_SHOW_NOTIFICATION = 4;
    private static final int NOTIFICATION_ID = 1;
    private static final int SET_BOOTPHASE_LISTENER_FAIL = -1;
    private static final String TAG = CbsUtils.TAG;
    AlertDialog mCbsDialog;
    private CbsSettings mCbsSettings;
    private Context mContext;
    private CbsSimInfo mCurrentSimInfo;
    View mDlgLayout;
    ExecutorService mExeService;
    private Handler mHandler;
    MiscHelper mHelper;
    private ServiceThread mMainThread;
    TextView mMsgTextView;
    Notification mNotification;
    NotificationManager mNotificationManager;
    boolean mNotificationShowing;
    CheckBox mRebootChkbox;
    private List<String> mSimTriggerMapkeys;
    private boolean mSimTriggerWithDialog;
    private VivoVgcService mVgcService;
    private int mTransition = 0;
    private List<String> mTempChangeModule = new ArrayList();
    private List<IVgcBootPhaseListener> mBootPhaseList = new ArrayList();

    public VivoCbsServiceImpl(Context context) {
        if (CbsUtils.DEBUG) {
            VSlog.i(TAG, "VivoCbsServiceImpl create");
        }
        this.mContext = context;
        ServiceThread serviceThread = new ServiceThread(TAG, -2, true);
        this.mMainThread = serviceThread;
        serviceThread.start();
        this.mHandler = new CbsHandler(this.mMainThread.getLooper());
        this.mVgcService = VivoVgcService.getInstance(this.mContext);
        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        this.mNotificationShowing = false;
        this.mCbsSettings = new CbsSettings(this.mHandler);
        this.mCurrentSimInfo = new CbsSimInfo();
        this.mExeService = Executors.newCachedThreadPool();
        List<String> simInfoList = this.mVgcService.getFileList("vgc_cbs_carrier_siminfo_path", CbsUtils.defSimInfoList);
        this.mCbsSettings.loadSettings(simInfoList);
        initTransition();
        this.mSimTriggerWithDialog = this.mVgcService.getBool("vgc_sim_trigger_with_dialog", true);
        List<String> stringList = this.mVgcService.getStringList("vgc_cbs_sim_trigger_mapkeys", null);
        this.mSimTriggerMapkeys = stringList;
        if (stringList == null) {
            this.mSimTriggerMapkeys = this.mCbsSettings.getSimTriggerMapkeys();
        }
    }

    public void setHelperHandle(MiscHelper helper) {
        this.mHelper = helper;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getNewState() {
        CbsSimInfo cbsSimInfo = this.mCurrentSimInfo;
        int cardType = cbsSimInfo != null ? cbsSimInfo.getCardType() : CbsSimInfo.CARDTYPE_NO_CARD;
        if (cardType == CbsSimInfo.CARDTYPE_MAIN_OP) {
            return 2;
        }
        if (cardType == CbsSimInfo.CARDTYPE_SUB_OP) {
            return 3;
        }
        return 1;
    }

    private synchronized void initTransition() {
        if (CbsUtils.DEBUG) {
            VSlog.d(TAG, "...init transition...");
        }
        this.mTransition = 0;
        int currentState = this.mCbsSettings.getCurrentState();
        int newState = getNewState();
        if (CbsUtils.DEBUG) {
            String str = TAG;
            VSlog.d(str, "currentState=" + currentState + ", newState=" + newState);
        }
        if (currentState != 0) {
            if (currentState == 1) {
                if (newState == 2) {
                    this.mTransition = 3;
                } else if (newState == 3) {
                    this.mTransition = 7;
                } else if (newState == 1) {
                    this.mTransition = 1;
                }
            }
        } else if (newState == 1) {
            this.mTransition = 1;
        } else if (newState == 2) {
            this.mTransition = 2;
        } else if (newState == 3) {
            this.mTransition = 6;
        }
        if (CbsUtils.DEBUG) {
            String str2 = TAG;
            VSlog.d(str2, "...transition=" + this.mTransition);
        }
    }

    private void initView() {
        this.mHandler.sendEmptyMessage(1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initNotificationLocked() {
        String title = this.mContext.getResources().getString(51249987);
        String message = this.mContext.getResources().getString(51249986);
        Intent intentClick = new Intent();
        intentClick.setAction(ACTION_NOTIF_CLICK);
        intentClick.putExtra(CHANNEL_ID, 1);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.mContext, 0, intentClick, Dataspace.RANGE_FULL);
        Notification.Builder builder = new Notification.Builder(this.mContext, CHANNEL_ID).setSmallIcon(50463072).setContentTitle(title).setContentText(message).setPriority(1000).setAutoCancel(true).setContentIntent(pendingIntent).setDefaults(0).setOngoing(true);
        this.mNotification = builder.build();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initDialogLocked() {
        LayoutInflater inflater = LayoutInflater.from(this.mContext);
        View inflate = inflater.inflate(50528438, (ViewGroup) null);
        this.mDlgLayout = inflate;
        this.mRebootChkbox = (CheckBox) inflate.findViewById(51183936);
        this.mMsgTextView = (TextView) this.mDlgLayout.findViewById(51183937);
        AlertDialog create = new AlertDialog.Builder(this.mContext, 51314792).setTitle(51249987).setView(this.mDlgLayout).setCancelable(false).setPositiveButton(51249983, new DialogInterface.OnClickListener() { // from class: com.vivo.services.vgc.cbs.VivoCbsServiceImpl.2
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialog, int which) {
                VivoCbsServiceImpl.this.writeUpdatedSettings(true);
                VivoCbsServiceImpl vivoCbsServiceImpl = VivoCbsServiceImpl.this;
                vivoCbsServiceImpl.setCurrentState(vivoCbsServiceImpl.getNewState());
                if (VivoCbsServiceImpl.this.mRebootChkbox.isChecked()) {
                    VivoCbsServiceImpl.this.mHandler.sendEmptyMessageDelayed(6, 1500L);
                } else {
                    VivoCbsServiceImpl.this.sendUpdatedBroadcast();
                }
            }
        }).setNegativeButton(51249984, new DialogInterface.OnClickListener() { // from class: com.vivo.services.vgc.cbs.VivoCbsServiceImpl.1
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialog, int which) {
                VivoCbsServiceImpl.this.setCurrentState(1);
                VivoCbsServiceImpl.this.mHandler.sendEmptyMessage(4);
            }
        }).create();
        this.mCbsDialog = create;
        create.getWindow().setType(2003);
    }

    @Deprecated
    public boolean isModuleNeedChange(String moduleId) {
        return true;
    }

    @Deprecated
    public void moduleChangeComplete(String moduleId) {
    }

    public void setParam(String name, Bundle param) {
        if (CbsUtils.DEBUG) {
            VSlog.d(TAG, "setParam name=" + name + ", param=" + param);
        }
        if (name == null || param == null) {
            return;
        }
        char c = 65535;
        int hashCode = name.hashCode();
        if (hashCode != 79350) {
            if (hashCode == 2074585 && name.equals("COTA")) {
                c = 0;
            }
        } else if (name.equals("PMS")) {
            c = 1;
        }
        if (c == 0) {
            this.mHelper.handleCotaSetParam(param);
        } else if (c == 1) {
            this.mHelper.handlePMSSetParam(param);
        } else if (CbsUtils.DEBUG) {
            VSlog.v(TAG, "setParam, unknow name=" + name);
        }
    }

    public Bundle getParam(String name, Bundle extParam) {
        if (CbsUtils.DEBUG) {
            String str = TAG;
            VSlog.d(str, "getParam name=" + name + ", param=" + extParam);
        }
        Bundle reply = new Bundle();
        if (name == null || extParam == null) {
            return reply;
        }
        char c = 65535;
        if (name.hashCode() == 2074585 && name.equals("COTA")) {
            c = 0;
        }
        if (c == 0) {
            return this.mHelper.handleCotaGetParam(extParam);
        }
        if (CbsUtils.DEBUG) {
            String str2 = TAG;
            VSlog.v(str2, "getParam, unknow name=" + name);
            return reply;
        }
        return reply;
    }

    public boolean hasParam(String name, Bundle extParam) {
        return false;
    }

    public Handler getHandler() {
        return this.mHandler;
    }

    public int getSimCardFlag() {
        return this.mCbsSettings.getSimCardFlag();
    }

    public synchronized int getTransition() {
        return this.mTransition;
    }

    public void updateResourceByMainCard(boolean isUpdate) {
        if (CbsUtils.DEBUG) {
            String str = TAG;
            VSlog.v(str, "updateResourceByMainCard isUpdate:" + isUpdate);
        }
        if (isUpdate) {
            writeUpdatedSettings(true);
            sendUpdatedBroadcast();
        }
    }

    public void writeUpdatedSettings(boolean writeCBSProp) {
        CbsSimInfo cbsSimInfo = this.mCurrentSimInfo;
        if (cbsSimInfo != null) {
            this.mCbsSettings.setPropValue(CbsSettings.KEY_PREV_MCCMNC, cbsSimInfo.getMccMnc());
            this.mCbsSettings.setPropValue(CbsSettings.KEY_PREV_GID1, this.mCurrentSimInfo.getGid1());
            this.mCbsSettings.setPropValue(CbsSettings.KEY_PREV_GID2, this.mCurrentSimInfo.getGid2());
            this.mCbsSettings.setPropValue(CbsSettings.KEY_PREV_SPN, this.mCurrentSimInfo.getSpn());
            this.mCbsSettings.setPropValue(CbsSettings.KEY_PREV_ICCID, this.mCurrentSimInfo.getIccid());
            this.mCbsSettings.setPropValue(CbsSettings.KEY_PREV_IMSI, this.mCurrentSimInfo.getImsi());
            this.mCbsSettings.setPropValue("map_key", this.mCurrentSimInfo.getMapKey());
            if (writeCBSProp) {
                SystemProperties.set("persist.product.carrier.name", this.mCurrentSimInfo.getMapKey());
            }
        }
    }

    public void vgcUpdate() {
        VivoVgcService.getInstance(this.mContext).updateVgcRes(null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendUpdatedBroadcast() {
        VivoVgcService.getInstance(this.mContext).updateVgcRes(new Runnable() { // from class: com.vivo.services.vgc.cbs.VivoCbsServiceImpl.3
            @Override // java.lang.Runnable
            public void run() {
                VivoCbsServiceImpl.this.sendCBSBroadcast();
            }
        });
    }

    public void sendCBSBroadcast() {
        Intent intent = new Intent("vivo.intent.action.CBS_UPDATE_RES");
        intent.addFlags(285212672);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT, null);
        if (CbsUtils.DEBUG) {
            String str = TAG;
            VSlog.e(str, "sendBroadcast intent:" + intent.getAction());
        }
    }

    public void saveImmediately() {
        this.mCbsSettings.saveImmediately();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateMapkeyResource(final int slotId, final int subId, final int state, final String mapkey) {
        if (!TextUtils.isEmpty(mapkey)) {
            this.mVgcService.updateVgcRes(slotId, state, mapkey, new Runnable() { // from class: com.vivo.services.vgc.cbs.VivoCbsServiceImpl.4
                @Override // java.lang.Runnable
                public void run() {
                    Intent intent = new Intent("vivo.intent.action.MAPKEY_CHANGED");
                    intent.putExtra("slotId", slotId);
                    intent.putExtra("subId", subId);
                    intent.putExtra("state", state);
                    intent.putExtra("mapkey", mapkey);
                    intent.addFlags(285212672);
                    VivoCbsServiceImpl.this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
                    if (CbsUtils.DEBUG) {
                        String str = VivoCbsServiceImpl.TAG;
                        VSlog.i(str, "updateMapkeyResource intent:" + intent);
                        String str2 = VivoCbsServiceImpl.TAG;
                        VSlog.d(str2, "updateMapkeyResource mapkey=" + mapkey + ", slotId=" + slotId + ", subId=" + subId + ", state=" + state);
                    }
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onBootPhase(int phase) {
        if (phase == 550) {
            setDefLanguage();
        } else if (phase != 600 && phase != 1000) {
            return;
        }
        callbackBootPhase(phase);
    }

    public synchronized int setOnBootPhaseListener(IVgcBootPhaseListener listener) {
        if (listener == null) {
            return -1;
        }
        this.mBootPhaseList.add(listener);
        return this.mBootPhaseList.size();
    }

    private synchronized void callbackBootPhase(final int phase) {
        final Iterator it = this.mBootPhaseList.iterator();
        while (it.hasNext()) {
            final IVgcBootPhaseListener listener = it.next();
            this.mExeService.execute(new Runnable() { // from class: com.vivo.services.vgc.cbs.VivoCbsServiceImpl.5
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        listener.onBootPhase(phase);
                        if (phase == 1000) {
                            it.remove();
                        }
                    } catch (Throwable e) {
                        String str = VivoCbsServiceImpl.TAG;
                        VSlog.e(str, "listener.onBootPhase error, phase=" + phase);
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void setDefLanguage() {
        boolean userSetupCompleted = Settings.Secure.getInt(this.mContext.getContentResolver(), "user_setup_complete", 0) == 1;
        String strLocal = VivoVgcService.getInstance(this.mContext).getString("setupwizard_default_language", null);
        String strRegion = VivoVgcService.getInstance(this.mContext).getString("settings_default_region", null);
        boolean vgcChangeLanguage = false;
        String iccid = CbsUtils.getDiscern();
        ContentValues languageMap = VivoVgcService.getInstance(this.mContext).getContentValues("VGC_iccid_language_map", null);
        if (languageMap != null && iccid != null && !iccid.trim().isEmpty() && iccid.length() >= 5) {
            vgcChangeLanguage = languageMap.containsKey(iccid.substring(2, 3)) || languageMap.containsKey(iccid.substring(2, 4)) || languageMap.containsKey(iccid.substring(2, 5));
        }
        if (strRegion != null && vgcChangeLanguage) {
            vgcChangeLanguage = VivoVgcService.getInstance(this.mContext).getStringList("vgc_region_limit_list", new ArrayList()).contains(strRegion);
        }
        if (!userSetupCompleted && strLocal != null && vgcChangeLanguage) {
            String[] temp = strLocal.split("_");
            int length = temp.length;
            String str = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            String language = (length < 1 || temp[0] == null) ? Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK : temp[0];
            if (temp.length >= 2 && temp[1] != null) {
                str = temp[1];
            }
            String country = str;
            if (CbsUtils.DEBUG) {
                String str2 = TAG;
                VSlog.i(str2, "setDefLanguage to " + strLocal);
            }
            Locale simCardlanguage = new Locale(language, country);
            LocalePicker.updateLocale(simCardlanguage);
        }
    }

    public void setCurrentState(int state) {
        if (CbsUtils.DEBUG) {
            String str = TAG;
            VSlog.v(str, "setCurrentState(): " + state);
        }
        this.mCbsSettings.setCurrentState(state);
    }

    protected void setSettingPropValue(String name, String value) {
        this.mCbsSettings.setPropValue(name, value);
    }

    protected void setSettingPropValue(String name, int value) {
        this.mCbsSettings.setPropValue(name, value);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public String getSettingPropValue(String name, String value) {
        return this.mCbsSettings.getPropValue(name, value);
    }

    protected int getSettingPropValue(String name, int value) {
        return this.mCbsSettings.getPropValue(name, value);
    }

    public int getCurrentState() {
        return this.mCbsSettings.getCurrentState();
    }

    public boolean isCustomizedSiminfo(CbsSimInfo simInfo) {
        String mapkey = queryMapkey(simInfo);
        File mapkeyDir = new File(CbsUtils.CARRIER_DIR, mapkey);
        return !TextUtils.isEmpty(mapkey) && this.mSimTriggerMapkeys.contains(mapkey) && mapkeyDir.isDirectory() && mapkeyDir.exists();
    }

    public String queryMapkey(CbsSimInfo simInfo) {
        CbsSimInfo mapSimInfo = this.mCbsSettings.getMapSimInfo(simInfo);
        return mapSimInfo.getMapKey();
    }

    public CbsSimInfo getMapSimInfo(CbsSimInfo simInfo) {
        return this.mCbsSettings.getMapSimInfo(simInfo);
    }

    public void updateSimInfo(CbsSimInfo simInfo) {
        this.mCurrentSimInfo = simInfo;
    }

    public void updateSimInfoAndTransition(CbsSimInfo originalSimInfo) {
        CbsSimInfo mapSimInfo = this.mCbsSettings.getMapSimInfo(originalSimInfo);
        if (CbsUtils.DEBUG) {
            String str = TAG;
            VSlog.i(str, "mCurrentSimInfo change from " + this.mCurrentSimInfo + " to " + mapSimInfo);
        }
        this.mCurrentSimInfo = mapSimInfo;
        initTransition();
    }

    public void startCbsComponent(int maincard) {
        synchronized (this) {
            if (CbsUtils.DEBUG) {
                String str = TAG;
                VSlog.d(str, "startCbsComponent maincard = " + maincard + " getTransition()==" + getTransition());
            }
            if (getTransition() > 1 && !notifyUiExist()) {
                if (this.mSimTriggerWithDialog) {
                    Message msg = this.mHandler.obtainMessage(2, maincard, 0);
                    this.mHandler.sendMessage(msg);
                } else {
                    this.mHandler.sendEmptyMessage(8);
                }
            } else if (getTransition() == 1) {
                setCurrentState(1);
                clearUiNotify();
            }
        }
    }

    public synchronized void clearUiNotify() {
        if (this.mCbsDialog != null && this.mCbsDialog.isShowing()) {
            this.mHandler.sendEmptyMessage(3);
        }
        if (this.mNotificationShowing) {
            this.mHandler.sendEmptyMessage(5);
        }
    }

    public boolean notifyUiExist() {
        AlertDialog alertDialog = this.mCbsDialog;
        return (alertDialog != null && alertDialog.isShowing()) || this.mNotificationShowing;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showNotification() {
        this.mNotificationShowing = true;
        String title = this.mContext.getResources().getString(51249987);
        this.mNotificationManager.createNotificationChannelGroup(new NotificationChannelGroup(CHANNEL_GROUP_ID, null));
        NotificationChannel notifChannel = new NotificationChannel(CHANNEL_ID, title, 4);
        notifChannel.setGroup(CHANNEL_GROUP_ID);
        this.mNotificationManager.createNotificationChannel(notifChannel);
        this.mNotificationManager.notify(1, this.mNotification);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeNotification() {
        this.mNotificationShowing = false;
        this.mNotificationManager.deleteNotificationChannel(CHANNEL_ID);
        this.mNotificationManager.deleteNotificationChannelGroup(CHANNEL_GROUP_ID);
        this.mNotificationManager.cancel(1);
    }

    /* loaded from: classes.dex */
    private class CbsHandler extends Handler {
        public CbsHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (CbsUtils.DEBUG) {
                String str = VivoCbsServiceImpl.TAG;
                VSlog.d(str, "handleMessage msg.what=" + msg.what);
            }
            synchronized (this) {
                switch (msg.what) {
                    case 1:
                        VivoCbsServiceImpl.this.initDialogLocked();
                        VivoCbsServiceImpl.this.initNotificationLocked();
                        break;
                    case 2:
                        if (!VivoCbsServiceImpl.this.notifyUiExist()) {
                            VivoCbsServiceImpl.this.initDialogLocked();
                            int maincard = msg.arg1;
                            TelephonyManager telephonyManager = (TelephonyManager) VivoCbsServiceImpl.this.mContext.getSystemService("phone");
                            boolean dualSimCardsInsert = telephonyManager.getPhoneCount() == 2 && CbsUtils.isSimInsertinSlot(telephonyManager, 0) && CbsUtils.isSimInsertinSlot(telephonyManager, 1);
                            TextView textView = VivoCbsServiceImpl.this.mMsgTextView;
                            String string = VivoCbsServiceImpl.this.mContext.getResources().getString(51249985);
                            Object[] objArr = new Object[1];
                            objArr[0] = dualSimCardsInsert ? String.valueOf(maincard) : Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                            textView.setText(String.format(string, objArr));
                            VivoCbsServiceImpl.this.mCbsDialog.show();
                            break;
                        } else {
                            return;
                        }
                        break;
                    case 3:
                        VivoCbsServiceImpl.this.mCbsDialog.dismiss();
                        break;
                    case 4:
                        VivoCbsServiceImpl.this.initNotificationLocked();
                        VivoCbsServiceImpl.this.showNotification();
                        break;
                    case 5:
                        VivoCbsServiceImpl.this.removeNotification();
                        break;
                    case 6:
                        Intent rebootIntent = new Intent("android.intent.action.REBOOT");
                        rebootIntent.putExtra("nowait", 1);
                        rebootIntent.putExtra("interval", 1);
                        rebootIntent.putExtra("window", 0);
                        VivoCbsServiceImpl.this.mContext.sendBroadcast(rebootIntent);
                        break;
                    case 7:
                        Bundle data = msg.getData();
                        int slotId = data.getInt("slotId");
                        int subId = data.getInt("subId");
                        int state = data.getInt("state");
                        String mapkey = data.getString("mapkey", null);
                        VivoCbsServiceImpl.this.updateMapkeyResource(slotId, subId, state, mapkey);
                        break;
                    case 8:
                        VivoCbsServiceImpl.this.writeUpdatedSettings(true);
                        VivoCbsServiceImpl.this.setCurrentState(VivoCbsServiceImpl.this.getNewState());
                        VivoCbsServiceImpl.this.sendUpdatedBroadcast();
                        break;
                }
            }
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpAndUsageStatsPermission(this.mContext, TAG, pw)) {
            StringBuilder sb = new StringBuilder((int) Consts.ProcessStates.FOCUS);
            if (args.length == 2 && "debug".equals(args[0])) {
                if (CbsUtils.DEBUG && "state".equals(args[1])) {
                    if (this.mCurrentSimInfo != null) {
                        sb.append("mCurrentSimInfo: ");
                        sb.append(this.mCurrentSimInfo);
                        sb.append("\n");
                    }
                    sb.append("mTransition: ");
                    sb.append(getTransition());
                    sb.append(", currentState=");
                    sb.append(getCurrentState());
                    sb.append("\n");
                    String prevMccMnc = this.mCbsSettings.getPropValue(CbsSettings.KEY_PREV_MCCMNC, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                    String prevGid1 = this.mCbsSettings.getPropValue(CbsSettings.KEY_PREV_GID1, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                    String prevGid2 = this.mCbsSettings.getPropValue(CbsSettings.KEY_PREV_GID2, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                    String prevSpn = this.mCbsSettings.getPropValue(CbsSettings.KEY_PREV_SPN, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                    String prevIccid = this.mCbsSettings.getPropValue(CbsSettings.KEY_PREV_ICCID, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                    String prevImsi = this.mCbsSettings.getPropValue(CbsSettings.KEY_PREV_IMSI, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                    CbsSimInfo cbsSimInfo = new CbsSimInfo(prevMccMnc, prevGid1, prevGid2, prevSpn, prevIccid, prevImsi);
                    CbsSimInfo prevSimInfo = this.mCbsSettings.getMapSimInfo(cbsSimInfo);
                    sb.append("mPrevSimInfo: ");
                    sb.append(prevSimInfo);
                    sb.append("\n");
                    sb.append("SimCardFlag: ");
                    sb.append(getSimCardFlag());
                    sb.append("\n");
                    List<CbsSimInfo> carrierSimInfos = this.mCbsSettings.getCarrierSimInfos().getSimInfoList();
                    if (carrierSimInfos != null) {
                        sb.append("carrierSimInfos: ");
                        sb.append("\n");
                        for (int i = 0; i < carrierSimInfos.size(); i++) {
                            sb.append("carrierSimInfos[" + i + "]:");
                            sb.append("\n");
                            sb.append(carrierSimInfos.get(i));
                            sb.append("\n");
                        }
                    }
                }
                pw.println(sb);
            }
        }
    }
}