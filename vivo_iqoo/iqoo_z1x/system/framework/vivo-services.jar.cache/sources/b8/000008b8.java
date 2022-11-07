package com.vivo.services.vgc.cbs;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.server.SystemService;
import com.android.server.pm.VivoPKMSLocManager;
import com.vivo.face.common.data.Constants;
import java.util.ArrayList;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoCbsService extends SystemService {
    private static final String HOTPLUG_DEBUG = "cbs_hotplug_debug";
    private boolean inMainSystem;
    private ContentResolver mContentResolver;
    private Context mContext;
    private MiscHelper mHelper;
    private ArrayList<String> mIccState;
    private final VivoCbsServiceImpl mImpl;
    private String[] mMapkeyArray;
    private int mMode;
    private BroadcastReceiver mReceiverSimStateChanged;
    private int[] mSubIdArray;
    private static final String TAG = CbsUtils.TAG;
    private static final boolean SIMTRIGGER_LOCAL_CBS = SystemProperties.getBoolean("ro.vivo.simtrigger.activate", false);
    private static final boolean SIMTRIGGER_COTA = SystemProperties.getBoolean("ro.vivo.cota.enable", false);

    public VivoCbsService(Context context) {
        super(context);
        this.inMainSystem = true;
        this.mReceiverSimStateChanged = new BroadcastReceiver() { // from class: com.vivo.services.vgc.cbs.VivoCbsService.1
            private Runnable mStartActivity = new Runnable() { // from class: com.vivo.services.vgc.cbs.VivoCbsService.1.1
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        if (VivoCbsService.this.mIccState.contains("LOADED")) {
                            VivoCbsService.this.checkIfNeedtoStartDialog();
                        }
                    } catch (Exception e) {
                        if (CbsUtils.DEBUG) {
                            VSlog.d(VivoCbsService.TAG, "checkIfNeedtoStartDialog exception");
                        }
                    }
                }
            };

            private void showVgcCbsDialog(int delayMs) {
                Handler handler = VivoCbsService.this.mImpl.getHandler();
                handler.removeCallbacks(this.mStartActivity);
                handler.postDelayed(this.mStartActivity, delayMs);
            }

            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                int defDataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
                int defDataPhoneId = SubscriptionManager.getPhoneId(defDataSubId);
                TelephonyManager telephonyManager = (TelephonyManager) VivoCbsService.this.mContext.getSystemService("phone");
                if (CbsUtils.DEBUG) {
                    String str = VivoCbsService.TAG;
                    VSlog.d(str, "onReceive, action = " + action);
                }
                if (!VivoCbsService.this.inMainSystem) {
                    return;
                }
                if (!"android.intent.action.SIM_STATE_CHANGED".equals(action)) {
                    if ("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED".equals(action)) {
                        intent.getIntExtra("android.telephony.extra.SUBSCRIPTION_INDEX", -1);
                        if (CbsUtils.DEBUG) {
                            String str2 = VivoCbsService.TAG;
                            VSlog.d(str2, "ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED defDataSubId:" + defDataSubId + ", defDataPhoneId:" + defDataPhoneId);
                        }
                        if (!SubscriptionManager.isValidSubscriptionId(defDataSubId) || !SubscriptionManager.isValidPhoneId(defDataPhoneId)) {
                            VivoCbsService.this.mImpl.clearUiNotify();
                            return;
                        } else if (SubscriptionManager.isValidPhoneId(defDataPhoneId) && ((String) VivoCbsService.this.mIccState.get(defDataPhoneId)).equals("LOADED")) {
                            VivoCbsService.this.mMode = 1;
                            showVgcCbsDialog(VivoPKMSLocManager.MAX_LOCATION_WAIT_TIME);
                            return;
                        } else {
                            return;
                        }
                    } else if (VivoCbsServiceImpl.ACTION_NOTIF_CLICK.equals(action) && defDataSubId != -1) {
                        VivoCbsService.this.mImpl.clearUiNotify();
                        showVgcCbsDialog(500);
                        return;
                    } else {
                        return;
                    }
                }
                String stateExtra = intent.getStringExtra("ss");
                int subId = intent.getIntExtra("android.telephony.extra.SUBSCRIPTION_INDEX", -1);
                int phoneId = intent.getIntExtra("phone", -1);
                Bundle data = new Bundle();
                if (CbsUtils.DEBUG) {
                    String str3 = VivoCbsService.TAG;
                    VSlog.d(str3, "onReceive, subid=" + subId + ", slotId=" + SubscriptionManager.getSlotIndex(subId) + ", phoneId=" + phoneId + ", stateExtra=" + stateExtra);
                }
                VivoCbsService.this.mIccState.set(phoneId == -1 ? 0 : phoneId, stateExtra);
                if ("ABSENT".equals(stateExtra)) {
                    if (SubscriptionManager.isValidPhoneId(phoneId) && !TextUtils.isEmpty(VivoCbsService.this.mMapkeyArray[phoneId])) {
                        data.putInt("state", 0);
                        data.putString("mapkey", VivoCbsService.this.mMapkeyArray[phoneId]);
                        data.putInt("subId", VivoCbsService.this.mSubIdArray[phoneId]);
                        VivoCbsService.this.mMapkeyArray[phoneId] = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                        VivoCbsService.this.mSubIdArray[phoneId] = subId;
                    }
                    if (!CbsUtils.isSimInsertinSlot(telephonyManager, 0) && !CbsUtils.isSimInsertinSlot(telephonyManager, 1)) {
                        VivoCbsService.this.mImpl.clearUiNotify();
                    }
                } else if ("LOADED".equals(stateExtra)) {
                    String mapkey = VivoCbsService.this.mImpl.queryMapkey(MiscHelper.getCbsSimInfo(VivoCbsService.this.mContext, subId, VivoCbsService.this.mMode));
                    data.putString("mapkey", mapkey);
                    data.putInt("state", 1);
                    data.putInt("subId", subId);
                    VivoCbsService.this.mMapkeyArray[phoneId] = mapkey;
                    VivoCbsService.this.mSubIdArray[phoneId] = subId;
                    VivoCbsService.this.mMode = 1;
                    showVgcCbsDialog(VivoPKMSLocManager.MAX_LOCATION_WAIT_TIME);
                }
                if (SubscriptionManager.isValidPhoneId(phoneId) && data.containsKey("state")) {
                    data.putInt("slotId", phoneId);
                    Handler handler = VivoCbsService.this.mImpl.getHandler();
                    handler.removeMessages(7, Integer.valueOf(phoneId));
                    Message msg = handler.obtainMessage(7, Integer.valueOf(phoneId));
                    msg.setData(data);
                    handler.sendMessageDelayed(msg, 3000L);
                }
            }
        };
        this.mContext = context;
        this.mImpl = new VivoCbsServiceImpl(context);
        MiscHelper miscHelper = new MiscHelper(context, this.mImpl);
        this.mHelper = miscHelper;
        this.mImpl.setHelperHandle(miscHelper);
        this.mMode = 1;
        ArrayList<String> arrayList = new ArrayList<>();
        this.mIccState = arrayList;
        arrayList.add(0, "ABSENT");
        this.mIccState.add(1, "ABSENT");
        this.mIccState.add(2, "ABSENT");
        this.mMapkeyArray = new String[2];
        this.mSubIdArray = new int[2];
    }

    public void onStart() {
        try {
            publishBinderService("cbs_service", this.mImpl);
        } catch (Throwable e) {
            VSlog.e(TAG, "Failure starting cbs_service", e);
        }
    }

    public void onBootPhase(int phase) {
        super.onBootPhase(phase);
        if (CbsUtils.DEBUG) {
            String str = TAG;
            VSlog.d(str, "onBootPhase phase=" + phase);
        }
        if (phase != 550) {
            if (phase == 1000 && CbsUtils.DEBUG) {
                String str2 = TAG;
                VSlog.d(str2, "onBootPhase PHASE_BOOT_COMPLETED  getTransition:" + this.mImpl.getTransition());
            }
        } else {
            registerReceiver();
            ContentResolver contentResolver = this.mContext.getContentResolver();
            this.mContentResolver = contentResolver;
            contentResolver.registerContentObserver(Settings.Global.getUriFor(HOTPLUG_DEBUG), false, new HotPlugDebugObserver());
        }
        this.mImpl.onBootPhase(phase);
    }

    public void onStartUser(int userHandle) {
        super.onStartUser(userHandle);
        if (userHandle == 999) {
            return;
        }
        this.inMainSystem = userHandle == 0;
        if (CbsUtils.DEBUG) {
            String str = TAG;
            VSlog.d(str, "onStartUser userHandle=" + userHandle);
        }
    }

    /* loaded from: classes.dex */
    private class HotPlugDebugObserver extends ContentObserver {
        public HotPlugDebugObserver() {
            super(null);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            int hotPlug = Settings.Global.getInt(VivoCbsService.this.mContentResolver, VivoCbsService.HOTPLUG_DEBUG, 0);
            if (CbsUtils.DEBUG) {
                String str = VivoCbsService.TAG;
                VSlog.d(str, "hotPlug change to:" + hotPlug);
            }
            if (hotPlug == 0) {
                VivoCbsService.this.mMode = 1;
                VivoCbsService.this.mImpl.clearUiNotify();
                return;
            }
            VivoCbsService.this.mMode = 0;
            VivoCbsService.this.checkIfNeedtoStartDialog();
        }
    }

    private void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
        intentFilter.addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
        intentFilter.addAction(VivoCbsServiceImpl.ACTION_NOTIF_CLICK);
        this.mContext.registerReceiver(this.mReceiverSimStateChanged, intentFilter);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkIfNeedtoStartDialog() {
        int state = this.mImpl.getCurrentState();
        boolean hasCustomized = state == 2 || state == 3 || state == 7;
        TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        int defDataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        int defDataMainCard = SubscriptionManager.getPhoneId(defDataSubId) + 1;
        CbsSimInfo dataCbsSimInfo = MiscHelper.getCbsSimInfo(this.mContext, defDataSubId, this.mMode);
        int nonDefDataSubId = MiscHelper.getSubId(this.mContext, defDataMainCard % 2);
        int nonDefDataMainCard = (defDataMainCard % 2) + 1;
        CbsSimInfo nonDataCbsSimInfo = new CbsSimInfo();
        if (CbsUtils.isSimInsertinSlot(telephonyManager, nonDefDataMainCard)) {
            nonDataCbsSimInfo = MiscHelper.getCbsSimInfo(this.mContext, nonDefDataSubId, this.mMode);
        }
        if (CbsUtils.DEBUG) {
            String str = TAG;
            VSlog.d(str, "dataCbsSimInfo:" + dataCbsSimInfo);
            String str2 = TAG;
            VSlog.d(str2, "nonDataCbsSimInfo:" + nonDataCbsSimInfo);
        }
        if (!hasCustomized && SIMTRIGGER_LOCAL_CBS) {
            if (this.mImpl.isCustomizedSiminfo(dataCbsSimInfo)) {
                this.mImpl.updateSimInfoAndTransition(dataCbsSimInfo);
                this.mImpl.startCbsComponent(defDataMainCard);
            } else if (CbsUtils.CONFIG_TRIGGER_STRATEGY == CbsUtils.STRATEGY_DEFAULT_DATA_CARD_PREFER && CbsUtils.isSimInsertinSlot(telephonyManager, nonDefDataMainCard - 1) && this.mImpl.isCustomizedSiminfo(nonDataCbsSimInfo)) {
                this.mImpl.updateSimInfoAndTransition(nonDataCbsSimInfo);
                this.mImpl.startCbsComponent(nonDefDataMainCard);
            }
        } else if (!hasCustomized && SIMTRIGGER_COTA) {
            this.mImpl.clearUiNotify();
            this.mHelper.startCotaService();
        }
    }
}