package com.vivo.services.rms.proxy;

import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.util.SparseArray;
import com.android.server.ServiceThread;
import com.android.server.am.ProcessRecord;
import com.android.server.am.RMProcHelper;
import com.android.server.wm.VivoMultiWindowConfig;
import com.vivo.common.utils.VLog;
import com.vivo.services.rms.ProcessInfo;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import vivo.app.VivoFrameworkFactory;
import vivo.app.configuration.AbsConfigurationManager;
import vivo.app.configuration.ConfigurationObserver;
import vivo.app.configuration.StringList;

/* loaded from: classes.dex */
public final class VivoBinderProxy implements Handler.Callback, ProcessInfo.StateChangeListener {
    private static final String CONFIG_FILE = "/data/bbkcore/config_white_list_1.0.xml";
    private static final String CONFIG_LIST_NAME = "config_whitelist";
    private static boolean DEBUG = SystemProperties.getBoolean("persist.vivo.binderproxy.debug", false);
    static final String TAG = "VivoBinderProxy";
    private Handler mBinderProxyHandler;
    private StringList mConfigStringlist;
    private AbsConfigurationManager mConfigurationManager;
    private String mInputMethod;
    private Thread mProxyThread;
    private VivoFrameworkFactory mVivoFrameworkFactory;
    boolean isInit = false;
    final int MSG_UNFREEZE_APP = 1;
    final int MSG_APP_DIED = 2;
    final int MSG_APP_HAS_SHOWUI = 3;
    private final Object mLock = new Object();
    private final SparseArray<Runnable> frozenProcessList = new SparseArray<>(64);
    private ConfigurationObserver onConfigChangeListener = new ConfigurationObserver() { // from class: com.vivo.services.rms.proxy.VivoBinderProxy.1
        public void onConfigChange(String file, String name) {
            try {
                if (VivoBinderProxy.this.mConfigurationManager != null) {
                    VivoBinderProxy.this.mBinderProxyHandler.post(new Runnable() { // from class: com.vivo.services.rms.proxy.VivoBinderProxy.1.1
                        @Override // java.lang.Runnable
                        public void run() {
                            VivoBinderProxy.this.mConfigStringlist = VivoBinderProxy.this.mConfigurationManager.getStringList(VivoBinderProxy.CONFIG_FILE, VivoBinderProxy.CONFIG_LIST_NAME);
                            if (VivoBinderProxy.this.mConfigStringlist != null && VivoBinderProxy.this.mConfigStringlist.getValues().size() > 0) {
                                List<String> mValue = VivoBinderProxy.this.mConfigStringlist.getValues();
                                for (String item : mValue) {
                                    VivoBinderProxy.this.UI_SPECIAL_LIST.add(item);
                                }
                            }
                        }
                    });
                } else {
                    VLog.i(VivoBinderProxy.TAG, "ConfigurationManager is null");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    private final HashSet<String> UI_SPECIAL_LIST = new HashSet<String>() { // from class: com.vivo.services.rms.proxy.VivoBinderProxy.2
        {
            add("com.bbk.updater");
            add(VivoMultiWindowConfig.SMART_MULTIWINDOW_NAME);
            add("com.vivo.secime.service");
            add("com.vivo.minscreen");
            add("com.vivo.daemonService");
            add("com.eg.android.AlipayGphone");
        }
    };

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Instance {
        private static final VivoBinderProxy INSTANCE = new VivoBinderProxy();

        private Instance() {
        }
    }

    public static VivoBinderProxy getInstance() {
        return Instance.INSTANCE;
    }

    public void initialize() {
        ServiceThread mProxyThread = new ServiceThread(TAG, 0, false);
        mProxyThread.start();
        this.mBinderProxyHandler = new Handler(mProxyThread.getLooper(), this);
        this.isInit = true;
        VivoFrameworkFactory frameworkFactoryImpl = VivoFrameworkFactory.getFrameworkFactoryImpl();
        this.mVivoFrameworkFactory = frameworkFactoryImpl;
        if (frameworkFactoryImpl != null) {
            this.mConfigurationManager = frameworkFactoryImpl.getConfigurationManager();
            VLog.e(TAG, "mConfigurationManager " + this.mConfigurationManager);
            if (this.mConfigurationManager != null) {
                this.mBinderProxyHandler.postDelayed(new Runnable() { // from class: com.vivo.services.rms.proxy.VivoBinderProxy.3
                    @Override // java.lang.Runnable
                    public void run() {
                        VivoBinderProxy vivoBinderProxy = VivoBinderProxy.this;
                        vivoBinderProxy.mConfigStringlist = vivoBinderProxy.mConfigurationManager.getStringList(VivoBinderProxy.CONFIG_FILE, VivoBinderProxy.CONFIG_LIST_NAME);
                        VLog.e(VivoBinderProxy.TAG, "mConfigStringlist " + VivoBinderProxy.this.mConfigStringlist);
                        if (VivoBinderProxy.this.mConfigStringlist != null) {
                            VivoBinderProxy.this.mConfigurationManager.registerObserver(VivoBinderProxy.this.mConfigStringlist, VivoBinderProxy.this.onConfigChangeListener);
                            if (VivoBinderProxy.this.mConfigStringlist != null && VivoBinderProxy.this.mConfigStringlist.getValues().size() > 0) {
                                List<String> mValue = VivoBinderProxy.this.mConfigStringlist.getValues();
                                for (String item : mValue) {
                                    VivoBinderProxy.this.UI_SPECIAL_LIST.add(item);
                                }
                                VLog.e(VivoBinderProxy.TAG, "UI_SPECIAL_LIST" + VivoBinderProxy.this.UI_SPECIAL_LIST);
                            }
                        }
                    }
                }, 20000L);
            }
        }
    }

    public boolean hasShownUi(ProcessRecord pr) {
        ProcessInfo pi = (ProcessInfo) RMProcHelper.getInfo(pr);
        if (pi == null) {
            return false;
        }
        synchronized (this.mLock) {
            if (this.UI_SPECIAL_LIST.contains(pi.mPkgName)) {
                return true;
            }
            return pi.hasShownUi();
        }
    }

    public void updateInputMethod(String inputMethod) {
        synchronized (this.mLock) {
            this.UI_SPECIAL_LIST.remove(this.mInputMethod);
            this.UI_SPECIAL_LIST.add(inputMethod);
            this.mInputMethod = inputMethod;
        }
    }

    public void updateConfig(boolean enableFeature, ArrayList<String> configList) {
        VLog.e(TAG, "updateConfig enableFeature " + enableFeature + " configList " + configList);
        ProxyUtils.setFeatureEnable(enableFeature);
        if (configList == null || configList.isEmpty()) {
            return;
        }
        synchronized (this.mLock) {
            Iterator<String> it = configList.iterator();
            while (it.hasNext()) {
                String processName = it.next();
                this.UI_SPECIAL_LIST.add(processName);
            }
        }
    }

    private void handleUnfreezeApp(int pid) {
        synchronized (this.mLock) {
            Runnable callback = this.frozenProcessList.get(pid);
            if (callback != null) {
                try {
                    callback.run();
                } catch (Exception e) {
                    VLog.e(TAG, "Failed to schedule configuration change", e);
                }
                this.frozenProcessList.remove(pid);
            }
        }
    }

    @Override // android.os.Handler.Callback
    public boolean handleMessage(Message msg) {
        int i = msg.what;
        if (i == 1) {
            handleUnfreezeApp(msg.arg1);
        } else if (i == 2) {
            handleDiedEvent(msg.arg1);
        } else if (i == 3) {
            handleHasShownUi(msg.arg1);
        }
        return true;
    }

    @Override // com.vivo.services.rms.ProcessInfo.StateChangeListener
    public void onStateChanged(int mask, boolean visable, ProcessInfo processInfo) {
        if ((mask & 32) != 0) {
            VLog.i(TAG, "onStateChanged HASSHOWNUI " + processInfo.mProcName);
            Handler handler = this.mBinderProxyHandler;
            handler.sendMessage(handler.obtainMessage(3, processInfo.mPid, 0));
        }
    }

    public void saveToFrozenList(ProcessRecord processRecord, Runnable callback) {
        if (!this.isInit) {
            return;
        }
        synchronized (this.mLock) {
            if (this.frozenProcessList.get(processRecord.pid) != null) {
                this.frozenProcessList.remove(processRecord.pid);
            }
            this.frozenProcessList.put(processRecord.pid, callback);
        }
    }

    public void removeFromFrozenList(int pid) {
        synchronized (this.mLock) {
            this.frozenProcessList.remove(pid);
        }
    }

    public void reportUnfreezeApp(ArrayList<ProcessRecord> list) {
        if (!this.isInit || !ProxyUtils.feature_support || list == null) {
            return;
        }
        Iterator<ProcessRecord> it = list.iterator();
        while (it.hasNext()) {
            ProcessRecord proc = it.next();
            if (proc.pid > 1) {
                Message message = this.mBinderProxyHandler.obtainMessage(1, proc.pid, 0);
                this.mBinderProxyHandler.sendMessage(message);
                if (DEBUG) {
                    VLog.i(TAG, "reportUnfreezeApp configuration change to unfreeze app " + proc.pid);
                }
            }
        }
    }

    public void reportHasShownUi(int pid) {
        if (!this.isInit || !ProxyUtils.feature_support) {
            return;
        }
        Handler handler = this.mBinderProxyHandler;
        handler.sendMessage(handler.obtainMessage(3, pid, 0));
    }

    public void reportShowUiSyncHandle(int pid) {
        if (!this.isInit || !ProxyUtils.feature_support) {
            return;
        }
        VLog.i(TAG, "reportShowUiSyncHandle " + pid, new Throwable("debug config"));
        synchronized (this.mLock) {
            Runnable callback = this.frozenProcessList.get(pid);
            if (callback != null) {
                try {
                    callback.run();
                } catch (Exception e) {
                    VLog.e(TAG, "Failed to schedule configuration change", e);
                }
                this.frozenProcessList.remove(pid);
            }
        }
    }

    private void handleHasShownUi(int pid) {
        synchronized (this.mLock) {
            Runnable callback = this.frozenProcessList.get(pid);
            if (callback != null) {
                try {
                    callback.run();
                } catch (Exception e) {
                    VLog.e(TAG, "Failed to schedule configuration change", e);
                }
                this.frozenProcessList.remove(pid);
            }
        }
    }

    public void reportFreezeApp(ArrayList<ProcessRecord> list) {
        if (!this.isInit || !ProxyUtils.feature_support) {
        }
    }

    public void reportAppDied(int pid) {
        if (!this.isInit || !ProxyUtils.feature_support) {
            return;
        }
        Message message = this.mBinderProxyHandler.obtainMessage(2, pid, 0);
        this.mBinderProxyHandler.sendMessage(message);
    }

    public void handleDiedEvent(int pid) {
        if (!this.isInit) {
            return;
        }
        if (DEBUG) {
            VLog.i(TAG, "app died and clear configchange in list for pid " + pid);
        }
        synchronized (this.mLock) {
            this.frozenProcessList.remove(pid);
        }
    }
}