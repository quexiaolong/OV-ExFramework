package com.android.server;

import android.app.OplusExSystemServiceHelper;
import android.common.OplusFeatureCache;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.OplusPermissionManager;
import android.freeze.IFreezeManagerHelp;
import android.freeze.IFreezeManagerService;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.OplusTraceManager;
import android.os.Process;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.customize.OplusCustomizeManager;
import android.os.oplusdevicepolicy.OplusDevicepolicyManager;
import android.util.Slog;
import android.util.TimingsTraceLog;
import com.android.server.commscene.CommSceneService;
import com.android.server.net.OplusNetworkManagementService;
import com.android.server.net.OplusNetworkStackService;
import com.android.server.net.OplusNetworkStatsService;
import com.android.server.net.OplusUidPurStatsSerivce;
import com.android.server.net.datalimit.DataLimitService;
import com.android.server.net.olk.OlkService;
import com.android.server.neuron.NeuronSystemService;
import com.android.server.nwpower.OAppNetControlService;
import com.android.server.nwpower.OSysNetControlService;
import com.android.server.onetrace.OplusTraceService;
import com.android.server.operator.OplusOperatorManagerService;
import com.android.server.oplus.DropDetectionService;
import com.android.server.oplus.FelicadeviceService;
import com.android.server.oplus.MotorManagerService;
import com.android.server.oplus.SecurityPermissionService;
import com.android.server.oplus.TorchManagerService;
import com.android.server.oplus.customize.OplusCustomizeService;
import com.android.server.oplus.heimdall.HeimdallService;
import com.android.server.oplus.oplusdevicepolicy.OplusDevicePolicyManagerService;
import com.android.server.oplus.orms.OplusResourceManagerService;
import com.android.server.oplus.osense.OsenseResManagerService;
import com.android.server.zenmode.ZenModeManagerExtImpl;
import com.oplus.commscene.CommSceneManager;
import com.oplus.content.OplusFeatureConfigManager;
import com.oplus.exsystemservice.IOplusExSystemService;
import com.oplus.heimdall.HeimdallManager;
import com.oplus.nec.OplusNecManager;
import com.oplus.network.OlkManager;
import com.oplus.network.OplusDataLimitManager;
import com.oplus.network.OplusNetworkStackManager;
import com.oplus.network.OplusUidPurStats;
import com.oplus.neuron.NeuronSystemManager;
import com.oplus.nwpower.OSysNetControlManager;

/* loaded from: classes.dex */
public class OplusSystemServerEx extends OplusDefaultSystemServerEx implements IOplusSystemServerEx {
    private static final String SYSTEM_SERVER_TIMING_ASYNC_TAG = "OplusSystemServerTimingAsync";
    private static final String TAG = "OplusSystemServerEx";
    private HeimdallService heimdallService;
    CommSceneService mCommSceneService;
    private ServiceConnection mConn;
    private FelicadeviceService mFelicadeviceService;
    HandlerThread mNetControlHandlerThread;
    private NeuronSystemService mNeuronSystem;
    OAppNetControlService mOAppNetControlService;
    OSysNetControlService mOSysNetControlService;
    private OplusCustomizeService mOplusCustomize;
    private OplusDevicePolicyManagerService mOplusDevicePolicyManagerService;
    private OplusOperatorManagerService mOplusOperatorManagerService;
    private SecurityPermissionService mSecurityPermissionService;
    private Context mSystemContext;
    private static final String SYSTEM_SERVER_TIMING_TAG = "OplusSystemServerTiming";
    private static final TimingsTraceLog BOOT_TIMINGS_TRACE_LOG = new TimingsTraceLog(SYSTEM_SERVER_TIMING_TAG, 524288);

    public OplusSystemServerEx(Context context) {
        super(context);
        this.mCommSceneService = null;
        this.mOAppNetControlService = null;
        this.mOSysNetControlService = null;
        this.mNetControlHandlerThread = null;
        this.heimdallService = null;
        this.mOplusCustomize = null;
        this.mSecurityPermissionService = null;
        this.mNeuronSystem = null;
        this.mOplusDevicePolicyManagerService = null;
        this.mOplusOperatorManagerService = null;
        this.mFelicadeviceService = null;
        this.mConn = new ServiceConnection() { // from class: com.android.server.OplusSystemServerEx.1
            @Override // android.content.ServiceConnection
            public void onServiceConnected(ComponentName name, IBinder service) {
                Slog.i(OplusSystemServerEx.TAG, "bindOplusExSystemService onServiceConnected pid = " + Process.myPid() + ",name = " + name);
                IOplusExSystemService oplusSystemService = IOplusExSystemService.Stub.asInterface(service);
                OplusLocalServices.addService(IOplusExSystemService.class, oplusSystemService);
            }

            @Override // android.content.ServiceConnection
            public void onServiceDisconnected(ComponentName name) {
                Slog.i(OplusSystemServerEx.TAG, "bindOplusExSystemService onServiceDisconnected name = " + name);
            }
        };
        this.mSystemContext = context;
    }

    @Override // com.android.server.OplusDefaultSystemServerEx, com.android.server.IOplusCommonSystemServerEx
    public void startBootstrapServices() {
        Slog.d(TAG, "startBootstrapServices");
        try {
            Slog.i(TAG, "Oplus Customize Service");
            OplusCustomizeService oplusCustomizeService = new OplusCustomizeService(this.mSystemContext);
            this.mOplusCustomize = oplusCustomizeService;
            ServiceManager.addService(OplusCustomizeManager.SERVICE_NAME, oplusCustomizeService);
        } catch (Throwable e) {
            Slog.e(TAG, "Failure starting Oplus Customize Service", e);
        }
    }

    @Override // com.android.server.OplusDefaultSystemServerEx, com.android.server.IOplusCommonSystemServerEx
    public void startCoreServices() {
        Slog.d(TAG, "startCoreServices");
        traceBeginAndSlog("Start OplusExSystemService");
        try {
            bindOplusExSystemService();
        } catch (Throwable e) {
            Slog.w(TAG, "***********************************************");
            Slog.wtf(TAG, "BOOT FAILURE starting OplusExSystemService ", e);
        }
        traceEnd();
        try {
            Slog.i(TAG, "add OplusTraceService");
            OplusTraceService onetrace = new OplusTraceService(this.mSystemContext);
            ServiceManager.addService(OplusTraceManager.ONETRACE_SERVICE, onetrace);
        } catch (Throwable e2) {
            Slog.e(TAG, "Failed to add OplusTraceService", e2);
        }
        traceBeginAndSlog("Start OplusResourceManagerService ORMS");
        try {
            OplusResourceManagerService orms = new OplusResourceManagerService(this.mSystemContext);
            ServiceManager.addService("OplusResourceManagerService", orms);
        } catch (Throwable e3) {
            Slog.e(TAG, "Start OplusResourceManagerService failed", e3);
        }
        traceEnd();
        if (CommSceneService.isFeatureEnable()) {
            try {
                Slog.i(TAG, "CommScene Service");
                CommSceneService commSceneService = new CommSceneService(this.mSystemContext);
                this.mCommSceneService = commSceneService;
                ServiceManager.addService(CommSceneManager.COMMSCENEMANAGER_SERVICE, commSceneService);
                return;
            } catch (Throwable e4) {
                Slog.e(TAG, "Failure starting CommScene Service", e4);
                return;
            }
        }
        Slog.i(TAG, "Starting CommScene Service failed, CommScene Feature Not Enable!");
    }

    @Override // com.android.server.OplusDefaultSystemServerEx, com.android.server.IOplusCommonSystemServerEx
    public void startOtherServices() {
        Slog.d(TAG, "startOtherServices");
        startTencentTRPEngService();
        try {
            Slog.i(TAG, "Oplus Nec Service");
            OplusNecService oplusNecService = new OplusNecService(this.mSystemContext);
            ServiceManager.addService(OplusNecManager.SRV_NAME, oplusNecService);
        } catch (Throwable e) {
            Slog.e(TAG, "Failure starting Oplus Networking Control Service", e);
        }
        try {
            Slog.i(TAG, "Oplus NetworkStats Service");
            OplusNetworkStatsService oplusNetworkStatsService = new OplusNetworkStatsService(this.mSystemContext);
            ServiceManager.addService("oplusnetworkstats", oplusNetworkStatsService);
        } catch (Throwable e2) {
            Slog.e(TAG, "Failure start Oplus network stats service", e2);
        }
        try {
            Slog.i(TAG, "Oplus NetworkStack Service");
            OplusNetworkStackService oplusNetworkStackService = new OplusNetworkStackService(this.mSystemContext);
            ServiceManager.addService(OplusNetworkStackManager.SRV_NAME, oplusNetworkStackService);
        } catch (Throwable e3) {
            Slog.e(TAG, "Failure starting Oplus NetworkStack Service", e3);
        }
        try {
            Slog.i(TAG, "Oplus Uid pur stats Service");
            OplusUidPurStatsSerivce oplusUidPurStatsSerivce = OplusUidPurStatsSerivce.getInstance();
            ServiceManager.addService(OplusUidPurStats.SRV_NAME, oplusUidPurStatsSerivce);
        } catch (Throwable e4) {
            Slog.e(TAG, "Failure start oplusUidPurStatsSerivce service", e4);
        }
        try {
            Slog.i(TAG, "OLK Service");
            OlkService olkService = new OlkService(this.mSystemContext);
            ServiceManager.addService(OlkManager.SRV_NAME, olkService);
        } catch (Throwable e5) {
            Slog.e(TAG, "Failure starting OlkService", e5);
        }
        try {
            Slog.i(TAG, "Oplus DataLimit Service");
            DataLimitService oplusDataLimitService = new DataLimitService(this.mSystemContext);
            ServiceManager.addService(OplusDataLimitManager.SRV_NAME, oplusDataLimitService);
        } catch (Throwable e6) {
            Slog.e(TAG, "Failure starting Oplus DataLimit Service", e6);
        }
        startSecurityPermissionService();
        startNeuronSystemService(this.mSystemContext);
        if (OAppNetControlService.isAnyOAppNetFeatureEnable()) {
            try {
                Slog.i(TAG, "OAppNetControl Service");
                OAppNetControlService oAppNetControlService = new OAppNetControlService(this.mSystemContext);
                this.mOAppNetControlService = oAppNetControlService;
                ServiceManager.addService("oappnetcontrol", oAppNetControlService);
            } catch (Throwable e7) {
                Slog.e(TAG, "Failure starting OAppNetControl Service", e7);
            }
        } else {
            Slog.i(TAG, "OAppNetControl Feature Not Enable!");
        }
        if (OSysNetControlService.isFeatureEnable()) {
            try {
                Slog.i(TAG, "OSysNetControl Service");
                OSysNetControlService oSysNetControlService = new OSysNetControlService(this.mSystemContext);
                this.mOSysNetControlService = oSysNetControlService;
                ServiceManager.addService(OSysNetControlManager.OSYSNETCONTROL_SERVICE, oSysNetControlService);
            } catch (Throwable e8) {
                Slog.e(TAG, "Failure starting OSysNetControl Service", e8);
            }
        } else {
            Slog.i(TAG, "Starting OSysNetControl Service failed, OSysNetControl Feature Not Enable!");
        }
        boolean hasFeatureNfcAny = this.mSystemContext.getPackageManager().hasSystemFeature("android.hardware.nfc.any");
        boolean hasFeaturFelica = OplusFeatureConfigManager.getInstacne().hasFeature("oplus.software.nfc.felica_support");
        Slog.i(TAG, "oplus feature support, nfc=" + hasFeatureNfcAny + ";felica=" + hasFeaturFelica);
        if (hasFeatureNfcAny && hasFeaturFelica) {
            traceBeginAndSlog("felicalock Service");
            try {
                Slog.i(TAG, "try to add oplus felicaser Service start");
                FelicadeviceService felicadeviceService = new FelicadeviceService(this.mSystemContext);
                this.mFelicadeviceService = felicadeviceService;
                ServiceManager.addService("felicaser", felicadeviceService);
            } catch (Throwable e9) {
                Slog.e(TAG, "Failling to add felicaser Service", e9);
            }
            traceEnd();
        }
        if (OplusFeatureConfigManager.getInstance().hasFeature("oplus.software.forwardly_freeze")) {
            OplusFeatureCache.set((IFreezeManagerHelp) OplusServiceFactory.getInstance().getFeature(IFreezeManagerHelp.DEFAULT, new Object[0]));
            OplusFeatureCache.set((IFreezeManagerService) OplusServiceFactory.getInstance().getFeature(IFreezeManagerService.DEFAULT, new Object[]{this.mSystemContext}));
        }
        try {
            Slog.i(TAG, "Torch Service");
            TorchManagerService.getInstance(this.mSystemContext).systemReady();
        } catch (Throwable e10) {
            Slog.e(TAG, "Failure start Torch Service", e10);
        }
        try {
            Slog.i(TAG, "Heimdall Service");
            HeimdallService heimdallService = new HeimdallService(this.mSystemContext);
            this.heimdallService = heimdallService;
            ServiceManager.addService(HeimdallManager.HEIMDALL_SERVICE, heimdallService);
        } catch (Throwable e11) {
            Slog.e(TAG, "Failure start Oplus Heimdall service", e11);
        }
        if (OplusFeatureConfigManager.getInstacne().hasFeature("oplus.software.camera.dropdetection_support")) {
            try {
                Slog.i(TAG, "Drop Detection Service starting");
                DropDetectionService.getInstance(this.mSystemContext).systemReady();
            } catch (Throwable e12) {
                Slog.e(TAG, "starting Drop Detection Service", e12);
            }
        }
        if (OplusFeatureConfigManager.getInstacne().hasFeature("oplus.software.motor_support")) {
            try {
                Slog.i(TAG, "Motor Service");
                MotorManagerService.getInstance(this.mSystemContext).systemReady();
            } catch (Throwable e13) {
                Slog.e(TAG, "starting Motor Service", e13);
            }
        }
        startOplusOperatorService();
        try {
            Slog.i(TAG, "Oplus NetworkManagement Service");
            OplusNetworkManagementService oplusNMService = OplusNetworkManagementService.getInstance(this.mSystemContext);
            ServiceManager.addService("oplusnetworkmanagement", oplusNMService);
        } catch (Throwable e14) {
            Slog.e(TAG, "Failure starting Oplus NetworkStack Service", e14);
        }
        startOplusCarService();
        traceBeginAndSlog("Start OsenseResManagerService oSense");
        try {
            Slog.i(TAG, "start osense service");
            this.mSystemServiceManager.startService(OsenseResManagerService.class);
        } catch (Throwable e15) {
            Slog.wtf(TAG, "Failed to start OsenseResManagerService :", e15);
        }
        traceEnd();
    }

    @Override // com.android.server.OplusDefaultSystemServerEx, com.android.server.IOplusCommonSystemServerEx
    public void systemReady() {
        Slog.d(TAG, "systemReady");
        traceBeginAndSlog("MakeOplusDevicepolicyServiceReady");
        try {
            this.mOplusDevicePolicyManagerService.systemReady();
        } catch (Throwable e) {
            Slog.e(TAG, "making OplusDevicepolicy Service ready", e);
        }
        traceEnd();
        traceBeginAndSlog("MakeOplusCustomizeServiceReady");
        try {
            OplusCustomizeService oplusCustomizeService = this.mOplusCustomize;
            if (oplusCustomizeService != null) {
                oplusCustomizeService.systemReady();
            }
        } catch (Throwable e2) {
            Slog.d(TAG, "making OplusCustomizeService ready " + e2);
        }
        this.mSecurityPermissionService.systemReady();
        traceEnd();
        if (CommSceneService.isFeatureEnable() || OAppNetControlService.isAnyOAppNetFeatureEnable() || OSysNetControlService.isFeatureEnable()) {
            HandlerThread handlerThread = new HandlerThread(OAppNetControlService.LOG_TAG);
            this.mNetControlHandlerThread = handlerThread;
            handlerThread.start();
        }
        if (CommSceneService.isFeatureEnable()) {
            try {
                Slog.i(TAG, "CommScene Service ready!");
                CommSceneService commSceneService = this.mCommSceneService;
                if (commSceneService != null) {
                    commSceneService.systemReady(this.mNetControlHandlerThread);
                }
            } catch (Throwable e3) {
                Slog.e(TAG, "Making CommScene Service ready:", e3);
            }
        } else {
            Slog.i(TAG, "Making CommScene Service ready failed, CommScene Feature Not Enable!");
        }
        if (OAppNetControlService.isAnyOAppNetFeatureEnable()) {
            try {
                Slog.i(TAG, "OAppNetControl Service ready");
                OAppNetControlService oAppNetControlService = this.mOAppNetControlService;
                if (oAppNetControlService != null) {
                    oAppNetControlService.systemReady(this.mNetControlHandlerThread);
                }
            } catch (Throwable e4) {
                Slog.e(TAG, "Making OAppNetControl Service ready", e4);
            }
        }
        if (OSysNetControlService.isFeatureEnable()) {
            try {
                Slog.i(TAG, "OSysNetControl Service ready");
                OSysNetControlService oSysNetControlService = this.mOSysNetControlService;
                if (oSysNetControlService != null) {
                    oSysNetControlService.systemReady(this.mNetControlHandlerThread);
                }
            } catch (Throwable e5) {
                Slog.e(TAG, "Making OSysNetControl Service ready", e5);
            }
        } else {
            Slog.i(TAG, "Making OSysNetControl Service ready failed, OSysNetControl Feature Not Enable!");
        }
        try {
            if (this.mFelicadeviceService != null) {
                Slog.i(TAG, "mFelicadeviceService systemReady");
                this.mFelicadeviceService.systemReady();
            }
        } catch (Throwable e6) {
            Slog.e(TAG, "making mFelicadeviceService Service ready", e6);
        }
        ZenModeManagerExtImpl.getInstance(null).initEnv(this.mSystemContext);
        if (this.mOplusOperatorManagerService != null) {
            try {
                Slog.i(TAG, "OppoOperatorManagerService systemReady");
                this.mOplusOperatorManagerService.systemReady();
            } catch (Throwable t) {
                Slog.e(TAG, "making Operator Service ready", t);
            }
        }
    }

    @Override // com.android.server.OplusDefaultSystemServerEx, com.android.server.IOplusCommonSystemServerEx
    public void systemRunning() {
        Slog.i(TAG, "systemRunning");
    }

    @Override // com.android.server.IOplusSystemServerEx
    public boolean startOplusLightService() {
        return true;
    }

    @Override // com.android.server.IOplusSystemServerEx
    public boolean startOplusAccessibilityService() {
        return false;
    }

    @Override // com.android.server.IOplusSystemServerEx
    public void addOplusDevicePolicyService() {
        traceBeginAndSlog("Start OplusDevicePolicy");
        try {
            Slog.i(TAG, "Starting OplusDevicePolicy...");
            OplusDevicePolicyManagerService oplusDevicePolicyManagerService = new OplusDevicePolicyManagerService(this.mSystemContext);
            this.mOplusDevicePolicyManagerService = oplusDevicePolicyManagerService;
            ServiceManager.addService(OplusDevicepolicyManager.SERVICE_NAME, oplusDevicePolicyManagerService);
        } catch (Throwable t) {
            Slog.e(TAG, "Failure starting Oppo Devicepolicy Service", t);
        }
        traceEnd();
    }

    private void startTencentTRPEngService() {
    }

    private void bindOplusExSystemService() {
        Intent intent = new Intent();
        intent.addFlags(256);
        intent.setComponent(OplusExSystemServiceHelper.getInstance().getComponentName());
        this.mSystemContext.bindServiceAsUser(intent, this.mConn, 1, UserHandle.SYSTEM);
    }

    private void startSecurityPermissionService() {
        try {
            Slog.i(TAG, "start SecurityPermissionService");
            SecurityPermissionService securityPermissionService = new SecurityPermissionService(this.mSystemContext);
            this.mSecurityPermissionService = securityPermissionService;
            ServiceManager.addService(OplusPermissionManager.SERVICE_NAME, securityPermissionService.asBinder());
        } catch (Throwable e) {
            Slog.e(TAG, "Failure starting SecurityPermissionService Service", e);
        }
    }

    private void startNeuronSystemService(Context context) {
        if (NeuronSystemManager.isEnable()) {
            NeuronSystemService neuronSystemService = new NeuronSystemService();
            this.mNeuronSystem = neuronSystemService;
            neuronSystemService.publish(context);
        }
    }

    private void startOplusOperatorService() {
        traceBeginAndSlog("StartOplusOperatorManagerService");
        try {
            this.mOplusOperatorManagerService = (OplusOperatorManagerService) this.mSystemServiceManager.startService(OplusOperatorManagerService.class);
        } catch (Throwable t) {
            Slog.e(TAG, "Failure starting OppoOperatorManagerService ", t);
        }
        traceEnd();
    }

    private void startOplusCarService() {
        traceBeginAndSlog("StartOplusCarService");
        try {
            Slog.i(TAG, "starting OplusCarService");
            this.mSystemServiceManager.startService("com.android.server.car.OplusCarService$Lifecycle");
        } catch (Throwable e) {
            Slog.e(TAG, "Failed to start OplusCarService", e);
        }
        traceEnd();
    }

    private void traceBeginAndSlog(String name) {
        Slog.i(TAG, name);
        BOOT_TIMINGS_TRACE_LOG.traceBegin(name);
    }

    private void traceEnd() {
        BOOT_TIMINGS_TRACE_LOG.traceEnd();
    }
}