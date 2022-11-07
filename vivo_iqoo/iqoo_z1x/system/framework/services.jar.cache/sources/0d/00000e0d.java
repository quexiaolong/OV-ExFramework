package com.android.server.emergency;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.SystemService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public class EmergencyAffordanceService extends SystemService {
    private static final boolean DBG = false;
    private static final String EMERGENCY_AFFORDANCE_OVERRIDE_ISO = "emergency_affordance_override_iso";
    private static final int INITIALIZE_STATE = 1;
    private static final int NETWORK_COUNTRY_CHANGED = 2;
    private static final String SERVICE_NAME = "emergency_affordance";
    private static final int SUBSCRIPTION_CHANGED = 3;
    private static final String TAG = "EmergencyAffordanceService";
    private static final int UPDATE_AIRPLANE_MODE_STATUS = 4;
    private boolean mAirplaneModeEnabled;
    private boolean mAnyNetworkNeedsEmergencyAffordance;
    private boolean mAnySimNeedsEmergencyAffordance;
    private BroadcastReceiver mBroadcastReceiver;
    private final Context mContext;
    private boolean mEmergencyAffordanceNeeded;
    private final ArrayList<String> mEmergencyCallCountryIsos;
    private MyHandler mHandler;
    private SubscriptionManager.OnSubscriptionsChangedListener mSubscriptionChangedListener;
    private SubscriptionManager mSubscriptionManager;
    private TelephonyManager mTelephonyManager;
    private boolean mVoiceCapable;

    public EmergencyAffordanceService(Context context) {
        super(context);
        this.mBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.emergency.EmergencyAffordanceService.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                if ("android.telephony.action.NETWORK_COUNTRY_CHANGED".equals(intent.getAction())) {
                    String countryCode = intent.getStringExtra("android.telephony.extra.NETWORK_COUNTRY");
                    int slotId = intent.getIntExtra("android.telephony.extra.SLOT_INDEX", -1);
                    EmergencyAffordanceService.this.mHandler.obtainMessage(2, slotId, 0, countryCode).sendToTarget();
                } else if ("android.intent.action.AIRPLANE_MODE".equals(intent.getAction())) {
                    EmergencyAffordanceService.this.mHandler.obtainMessage(4).sendToTarget();
                }
            }
        };
        this.mSubscriptionChangedListener = new SubscriptionManager.OnSubscriptionsChangedListener() { // from class: com.android.server.emergency.EmergencyAffordanceService.2
            @Override // android.telephony.SubscriptionManager.OnSubscriptionsChangedListener
            public void onSubscriptionsChanged() {
                EmergencyAffordanceService.this.mHandler.obtainMessage(3).sendToTarget();
            }
        };
        this.mContext = context;
        String[] isos = context.getResources().getStringArray(17236030);
        this.mEmergencyCallCountryIsos = new ArrayList<>(isos.length);
        for (String iso : isos) {
            this.mEmergencyCallCountryIsos.add(iso);
        }
        if (Build.IS_DEBUGGABLE) {
            String overrideIso = Settings.Global.getString(this.mContext.getContentResolver(), EMERGENCY_AFFORDANCE_OVERRIDE_ISO);
            if (!TextUtils.isEmpty(overrideIso)) {
                this.mEmergencyCallCountryIsos.clear();
                this.mEmergencyCallCountryIsos.add(overrideIso);
            }
        }
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService(SERVICE_NAME, new BinderService());
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 600) {
            handleThirdPartyBootPhase();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MyHandler extends Handler {
        public MyHandler(Looper l) {
            super(l);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 1) {
                EmergencyAffordanceService.this.handleInitializeState();
            } else if (i == 2) {
                String countryIso = (String) msg.obj;
                int slotId = msg.arg1;
                EmergencyAffordanceService.this.handleNetworkCountryChanged(countryIso, slotId);
            } else if (i == 3) {
                EmergencyAffordanceService.this.handleUpdateSimSubscriptionInfo();
            } else if (i == 4) {
                EmergencyAffordanceService.this.handleUpdateAirplaneModeStatus();
            } else {
                Slog.e(EmergencyAffordanceService.TAG, "Unexpected message received: " + msg.what);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleInitializeState() {
        handleUpdateAirplaneModeStatus();
        handleUpdateSimSubscriptionInfo();
        updateNetworkCountry();
        updateEmergencyAffordanceNeeded();
    }

    private void handleThirdPartyBootPhase() {
        TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService(TelephonyManager.class);
        this.mTelephonyManager = telephonyManager;
        boolean isVoiceCapable = telephonyManager.isVoiceCapable();
        this.mVoiceCapable = isVoiceCapable;
        if (!isVoiceCapable) {
            updateEmergencyAffordanceNeeded();
            return;
        }
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        this.mHandler = new MyHandler(thread.getLooper());
        SubscriptionManager from = SubscriptionManager.from(this.mContext);
        this.mSubscriptionManager = from;
        from.addOnSubscriptionsChangedListener(this.mSubscriptionChangedListener);
        IntentFilter filter = new IntentFilter("android.intent.action.AIRPLANE_MODE");
        filter.addAction("android.telephony.action.NETWORK_COUNTRY_CHANGED");
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
        this.mHandler.obtainMessage(1).sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleUpdateAirplaneModeStatus() {
        this.mAirplaneModeEnabled = Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleUpdateSimSubscriptionInfo() {
        List<SubscriptionInfo> activeSubscriptionInfoList = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        if (activeSubscriptionInfoList == null) {
            return;
        }
        boolean needsAffordance = false;
        Iterator<SubscriptionInfo> it = activeSubscriptionInfoList.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            SubscriptionInfo info = it.next();
            if (isoRequiresEmergencyAffordance(info.getCountryIso())) {
                needsAffordance = true;
                break;
            }
        }
        this.mAnySimNeedsEmergencyAffordance = needsAffordance;
        updateEmergencyAffordanceNeeded();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleNetworkCountryChanged(String countryIso, int slotId) {
        if (TextUtils.isEmpty(countryIso) && this.mAirplaneModeEnabled) {
            Slog.w(TAG, "Ignore empty countryIso report when APM is on.");
            return;
        }
        updateNetworkCountry();
        updateEmergencyAffordanceNeeded();
    }

    private void updateNetworkCountry() {
        boolean needsAffordance = false;
        int activeModems = this.mTelephonyManager.getActiveModemCount();
        int i = 0;
        while (true) {
            if (i >= activeModems) {
                break;
            }
            String countryIso = this.mTelephonyManager.getNetworkCountryIso(i);
            if (!isoRequiresEmergencyAffordance(countryIso)) {
                i++;
            } else {
                needsAffordance = true;
                break;
            }
        }
        this.mAnyNetworkNeedsEmergencyAffordance = needsAffordance;
        updateEmergencyAffordanceNeeded();
    }

    private boolean isoRequiresEmergencyAffordance(String iso) {
        return this.mEmergencyCallCountryIsos.contains(iso);
    }

    private void updateEmergencyAffordanceNeeded() {
        boolean lastAffordanceNeeded = this.mEmergencyAffordanceNeeded;
        boolean z = this.mVoiceCapable && (this.mAnySimNeedsEmergencyAffordance || this.mAnyNetworkNeedsEmergencyAffordance);
        this.mEmergencyAffordanceNeeded = z;
        if (lastAffordanceNeeded != z) {
            Settings.Global.putInt(this.mContext.getContentResolver(), "emergency_affordance_needed", this.mEmergencyAffordanceNeeded ? 1 : 0);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dumpInternal(IndentingPrintWriter ipw) {
        ipw.println("EmergencyAffordanceService (dumpsys emergency_affordance) state:\n");
        ipw.println("mEmergencyAffordanceNeeded=" + this.mEmergencyAffordanceNeeded);
        ipw.println("mVoiceCapable=" + this.mVoiceCapable);
        ipw.println("mAnySimNeedsEmergencyAffordance=" + this.mAnySimNeedsEmergencyAffordance);
        ipw.println("mAnyNetworkNeedsEmergencyAffordance=" + this.mAnyNetworkNeedsEmergencyAffordance);
        ipw.println("mEmergencyCallCountryIsos=" + String.join(",", this.mEmergencyCallCountryIsos));
    }

    /* loaded from: classes.dex */
    private final class BinderService extends Binder {
        private BinderService() {
        }

        @Override // android.os.Binder
        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(EmergencyAffordanceService.this.mContext, EmergencyAffordanceService.TAG, pw)) {
                EmergencyAffordanceService.this.dumpInternal(new IndentingPrintWriter(pw, "  "));
            }
        }
    }
}