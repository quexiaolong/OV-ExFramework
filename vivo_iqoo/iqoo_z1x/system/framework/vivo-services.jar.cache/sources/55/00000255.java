package com.android.server.hangvivodebug;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemProperties;
import java.io.FileWriter;
import java.io.IOException;
import vivo.app.VivoFrameworkFactory;
import vivo.app.configuration.AbsConfigurationManager;
import vivo.app.configuration.ConfigurationObserver;
import vivo.app.configuration.ContentValuesList;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class HangVivoDebugConfig {
    private static final String HANGVIVO_CONFIG_FILE = "/data/bbkcore/Hang_Vivo_Debug.xml";
    private static final String HANGVIVO_SWITCH = "hangvivo_switch";
    private static final String SWITCH_HANGVIVO_ON = "switch_hangvivo_on";
    private static final String TAG = "HangVivoDebugConfig";
    static Handler mHangVivoHandler;
    private static HangVivoDebugConfig sIntance;
    private final boolean IS_LOG_CTRL_OPEN;
    private AbsConfigurationManager mConfigurationManager;
    private Context mContext;
    private HandlerThread mHangVivoThread;
    private boolean mHangVivo_Switch;
    private ConfigurationObserver mSwitchObserver;
    private ContentValuesList mSwitchValuesList;
    private VivoFrameworkFactory mVivoFrameworkFactory;

    public static synchronized HangVivoDebugConfig getInstance() {
        HangVivoDebugConfig hangVivoDebugConfig;
        synchronized (HangVivoDebugConfig.class) {
            if (sIntance == null) {
                sIntance = new HangVivoDebugConfig();
            }
            hangVivoDebugConfig = sIntance;
        }
        return hangVivoDebugConfig;
    }

    private HangVivoDebugConfig() {
        this.IS_LOG_CTRL_OPEN = SystemProperties.get("persist.sys.log.ctrl", "no").equals("yes") || Build.TYPE.equals("eng");
        this.mHangVivo_Switch = true;
        this.mSwitchObserver = new ConfigurationObserver() { // from class: com.android.server.hangvivodebug.HangVivoDebugConfig.2
            public void onConfigChange(String file, String name) {
                if (HangVivoDebugConfig.this.IS_LOG_CTRL_OPEN) {
                    VSlog.i(HangVivoDebugConfig.TAG, "onConfigChange file:" + file + ",name=" + name);
                }
                if (HangVivoDebugConfig.this.mConfigurationManager == null) {
                    if (HangVivoDebugConfig.this.IS_LOG_CTRL_OPEN) {
                        VSlog.i(HangVivoDebugConfig.TAG, "mConfigurationManager is null");
                        return;
                    }
                    return;
                }
                HangVivoDebugConfig hangVivoDebugConfig = HangVivoDebugConfig.this;
                hangVivoDebugConfig.mSwitchValuesList = hangVivoDebugConfig.mConfigurationManager.getContentValuesList(HangVivoDebugConfig.HANGVIVO_CONFIG_FILE, HangVivoDebugConfig.HANGVIVO_SWITCH);
                HangVivoDebugConfig.mHangVivoHandler.post(HangVivoDebugConfig.this.hangVivoRegister());
            }
        };
    }

    public void init(Context context) {
        if (this.IS_LOG_CTRL_OPEN) {
            VSlog.i(TAG, "HangVivoDebugConfig inital..");
        }
        this.mContext = context;
        HandlerThread handlerThread = new HandlerThread("HangVivoDebug");
        this.mHangVivoThread = handlerThread;
        handlerThread.start();
        mHangVivoHandler = new Handler(this.mHangVivoThread.getLooper());
        VivoFrameworkFactory frameworkFactoryImpl = VivoFrameworkFactory.getFrameworkFactoryImpl();
        this.mVivoFrameworkFactory = frameworkFactoryImpl;
        if (frameworkFactoryImpl != null) {
            AbsConfigurationManager configurationManager = frameworkFactoryImpl.getConfigurationManager();
            this.mConfigurationManager = configurationManager;
            this.mSwitchValuesList = configurationManager.getContentValuesList(HANGVIVO_CONFIG_FILE, HANGVIVO_SWITCH);
            if (this.IS_LOG_CTRL_OPEN) {
                VSlog.i(TAG, "HangVivoDebugConfig registerObserver..");
            }
            this.mConfigurationManager.registerObserver(this.mSwitchValuesList, this.mSwitchObserver);
            mHangVivoHandler.post(hangVivoRegister());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Runnable hangVivoRegister() {
        return new Runnable() { // from class: com.android.server.hangvivodebug.HangVivoDebugConfig.1
            @Override // java.lang.Runnable
            public void run() {
                if (HangVivoDebugConfig.this.IS_LOG_CTRL_OPEN) {
                    VSlog.i(HangVivoDebugConfig.TAG, "start to update hangvivo_switch");
                }
                HangVivoDebugConfig.this.updateSwitchConfig();
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateSwitchConfig() {
        try {
            if (this.mSwitchValuesList != null) {
                this.mHangVivo_Switch = this.mSwitchValuesList.getValue(SWITCH_HANGVIVO_ON) != null ? Boolean.parseBoolean(this.mSwitchValuesList.getValue(SWITCH_HANGVIVO_ON)) : true;
            }
            if (this.IS_LOG_CTRL_OPEN) {
                VSlog.i(TAG, "mHangVivo_Switch:" + this.mHangVivo_Switch);
            }
            doSysKme(this.mHangVivo_Switch);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doSysKme(boolean switch_on) {
        try {
            FileWriter enable_monitor = new FileWriter("/sys/kernel/kme/wdt/enable_monitor");
            if (switch_on) {
                VSlog.i(TAG, "write: -100 to enable_monitor");
                enable_monitor.write("-100\n");
                VSlog.i(TAG, "write: -100 to end");
                enable_monitor.close();
            } else {
                VSlog.i(TAG, "write: -101 to enable_monitor");
                enable_monitor.write("-101\n");
                VSlog.i(TAG, "write: -101 to end");
                enable_monitor.close();
            }
        } catch (IOException e) {
            VSlog.i(TAG, "Failed to write to /sys/kernel/kme/wdt/enable_monitor");
        }
    }
}