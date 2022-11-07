package com.android.server;

import android.app.ActivityManagerNative;
import android.app.IProcessObserver;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.NetworkUtils;
import android.net.RouteInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.Messenger;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.FtFeature;
import com.android.internal.widget.VivoSystemOverlayToast;
import com.android.server.ConnectivityService;
import com.android.server.connectivity.NetworkAgentInfo;
import com.android.server.net.monitor.VivoNetworkAnalyser;
import com.android.server.wm.VCD_FF_1;
import com.vivo.common.VivoCollectData;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import com.vivo.services.superresolution.Constant;
import com.vivo.vcodetransbase.EventTransfer;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import vivo.app.VivoFrameworkFactory;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoConnectivityServiceImpl implements IVivoConnectivityService {
    private static final String APP_EASYSHARE = "easyshare";
    private static final String ATTR_SUPPORT = "support";
    private static final String ATTR_VERSION = "version";
    protected static final boolean DBG = true;
    private static final String DEFAULT_HTTPS_URL = "https://www.google.com/generate_204";
    private static final String DEFAULT_HTTP_URL = "http://connectivitycheck.gstatic.com/generate_204";
    private static final String EASYSHARE_PACKAGE_NAME = "com.vivo.easyshare";
    private static final String EASYSHARE_SUPPORT_KEY = "is_supprot_netcoexist";
    private static final String FEATURE_NETCOEXIST = "vivo.software.netcoexist";
    private static final String FEATURE_WIFIDNSOPTIMIZATION = "vivo.software.wifidnsoptimization";
    private static final int FROM_BOOT = 3;
    private static final int FROM_SETINTERFACE = 1;
    private static final int FROM_SETTINGSCHANGE = 2;
    private static final String GAME_ACCELERATOR_STATE = "game_accelerator_state";
    private static final String STR_JUDGE_SYS_OR_APP = "net.wifi.vivosoftap.uid";
    private static final String TAG = "VivoConnectivityService";
    private static final int WHEN_USED_BOOT = 1;
    private static final int WHEN_USED_METHODINVOKE = 3;
    private static final int WHEN_USED_SETTINGSCHANGE = 2;
    public static final String WIFI_NETCOEXIST = "F348|10016";
    public static final String WIFI_VCODE = "F348";
    protected final ConnectivityService mConnectivityService;
    private final Context mContext;
    private InetAddress mDefaultMobileDns;
    private Handler mHandler;
    protected INetworkManagementService mNetworkManagementService;
    private ConnectivityService.NetworkStateChangeCallback mNetworkStateChangeCallback;
    private VivoNetworkAnalyser mVivoNetworkAnalyser;
    private static final Pattern AP_NAME_PATTERN = Pattern.compile("vivo@(.+?)@\\w{2}");
    private static final Pattern AP_NAME_HJ_PATTERN = Pattern.compile("vivo#(.+?)#\\w{2}");
    private static boolean hasSendNetCoexistBroadcast = false;
    private static WifiManager mWifiManager = null;
    private static ArrayList<String> GameAcceleratorMap = new ArrayList<>();
    private static boolean hasAddNetCoexistNetworkRule = false;
    private static final Object mSyncNetcoexistWhiteListLock = new Object();
    private static ArrayList<String> mNetcoexistWhiteList = new ArrayList<>();
    protected static boolean hasAddPolicyRoute = false;
    private boolean isOverseas = false;
    private boolean kidSwitchOn = false;
    private boolean mWifiNoDns = false;
    private final String NOT_ALLOWED_ASSEMBLE_PACKAGE = "tencent;bbk;iqoo;qiyi;baidu;.ss.;vivo;video;music;game;down;smile;kuaishou;youku;kugou;tv;live;netease";
    private final String[] noAllowedPackageList = "tencent;bbk;iqoo;qiyi;baidu;.ss.;vivo;video;music;game;down;smile;kuaishou;youku;kugou;tv;live;netease".split(";");
    private int VCD_WIFI_P_2 = 0;
    private String mForgroundAppName = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    private InetAddress mDefaultWlanDns1 = null;
    private InetAddress mDefaultWlanDns2 = null;
    private InetAddress mDefaultWlanDns3 = null;
    private Runnable VCD_WIFI_P_1 = new Runnable() { // from class: com.android.server.VivoConnectivityServiceImpl.2
        @Override // java.lang.Runnable
        public void run() {
            VivoConnectivityServiceImpl.this.VCD_WIFI_2();
        }
    };
    private IProcessObserver mProcessObserver = new IProcessObserver.Stub() { // from class: com.android.server.VivoConnectivityServiceImpl.3
        public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
            VivoConnectivityServiceImpl.log("onForegroundActivitiesChanged: pid=" + pid + ", uid=" + uid + ", foregroundActivities=" + foregroundActivities);
            if (foregroundActivities) {
                VivoConnectivityServiceImpl vivoConnectivityServiceImpl = VivoConnectivityServiceImpl.this;
                vivoConnectivityServiceImpl.mForgroundAppName = vivoConnectivityServiceImpl.getPackageName(uid);
                VivoConnectivityServiceImpl.log("mForgroundAppName=" + VivoConnectivityServiceImpl.this.mForgroundAppName);
            }
        }

        public void onProcessDied(int pid, int uid) {
        }

        public void onForegroundServicesChanged(int pid, int uid, int serviceTypes) {
        }
    };
    private Runnable mAddNetcoexistNetworkRule = new Runnable() { // from class: com.android.server.VivoConnectivityServiceImpl.5
        @Override // java.lang.Runnable
        public void run() {
            VivoConnectivityServiceImpl.this.addNetcoexistNetworkRule();
        }
    };
    private Runnable mDelNetcoexistNetworkRule = new Runnable() { // from class: com.android.server.VivoConnectivityServiceImpl.6
        @Override // java.lang.Runnable
        public void run() {
            VivoConnectivityServiceImpl.this.delNetcoexistNetworkRule();
        }
    };
    private boolean change_boot_flag = false;
    private final Object mNetworkStateChangeCallbackLock = new Object();
    private boolean mWifiDnsOptimizationEnabled = false;
    private boolean netcoexist_support = false;
    protected int netcoexist_version = 1;

    public VivoConnectivityServiceImpl(Context context, ConnectivityService service, INetworkManagementService managementService, Handler handler) {
        this.mContext = context;
        this.mConnectivityService = service;
        this.mNetworkManagementService = managementService;
        this.mHandler = handler;
        try {
            if (isOverseasWithCountryCodeCheck()) {
                this.mDefaultMobileDns = NetworkUtils.numericToInetAddress("8.8.8.8");
            } else {
                this.mDefaultMobileDns = NetworkUtils.numericToInetAddress("114.114.114.114");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        updatePortalCheckSites(true);
        initWifiDefautDns();
        setupVivoBroadcastReceiver();
        registerProcessObserver();
        initGameAcceleratorAppList();
        initNetcoexistWhiteList();
        registerPdnsSettingsObserver();
        this.mVivoNetworkAnalyser = VivoNetworkAnalyser.getInstance(this.mContext);
        registerKidswitchObserver();
        initNetCoexistConfig();
        initWifiDnsOptimizationEnabled();
    }

    public void onNotifyNetworkConnected(NetworkAgentInfo nai) {
        updatePortalCheckSites(false);
    }

    public boolean onNetworkTested(NetworkAgentInfo nai, boolean valid) {
        boolean isValid = valid;
        if (isMobile(nai)) {
            VLog.d(TAG, "onNetworkTested mobile valid");
            notifyMobileNetworkValidInfo(valid);
        }
        if (isWifi(nai) || isExtWifi(nai)) {
            boolean ctsOn = Settings.Global.getInt(this.mContext.getContentResolver(), "vivo_wifi_cts_on", 0) == 1;
            if (!ctsOn) {
                if (this.mWifiNoDns) {
                    isValid = false;
                } else if (!isOverseas()) {
                    VLog.d(TAG, "onNetworkTested is wifi return true");
                    isValid = true;
                }
            }
        }
        synchronized (this.mNetworkStateChangeCallbackLock) {
            if (this.mNetworkStateChangeCallback != null) {
                this.mNetworkStateChangeCallback.onNetworkConnected(nai, valid);
            }
        }
        return isValid;
    }

    public boolean vivoAvoidBadWifi() {
        return false;
    }

    private void updatePortalCheckSites(boolean isInit) {
        boolean currentOverseas = isOverseas();
        if (this.isOverseas != currentOverseas || isInit) {
            Settings.Global.putString(this.mContext.getContentResolver(), "captive_portal_https_url", currentOverseas ? DEFAULT_HTTPS_URL : "https://wifi.vivo.com.cn/generate_204");
            Settings.Global.putString(this.mContext.getContentResolver(), "captive_portal_http_url", currentOverseas ? DEFAULT_HTTP_URL : "http://wifi.vivo.com.cn/generate_204");
            this.isOverseas = currentOverseas;
        }
        if (isOverseas() && !isOverseasWithCountryCodeCheck()) {
            Settings.Global.putString(this.mContext.getContentResolver(), "captive_portal_https_url", "https://connectivitycheck.gstatic.com/generate_204");
            VLog.d(TAG, "updatePortalCheckSites overseas networkcheck in China.");
        }
    }

    public boolean isOverseas() {
        String overseas = SystemProperties.get("ro.vivo.product.overseas", "no");
        return "yes".equals(overseas);
    }

    private boolean isOverseasWithCountryCodeCheck() {
        String overseas = SystemProperties.get("ro.vivo.product.overseas", "no");
        String countryCode = SystemProperties.get("gsm.vivo.countrycode");
        if (!TextUtils.isEmpty(countryCode)) {
            return !countryCode.equalsIgnoreCase("CN");
        }
        return overseas.equalsIgnoreCase("yes");
    }

    private void setNetworkCheckState(NetworkInfo ni) {
    }

    public String getInterfaceNameForType(int networkType, Collection<NetworkAgentInfo> networkAgentInfos) {
        String extra;
        try {
            for (NetworkAgentInfo netAgentInfo : networkAgentInfos) {
                NetworkInfo networkInfo = netAgentInfo.networkInfo;
                if (networkInfo != null && networkType == networkInfo.getType() && ((extra = networkInfo.getExtraInfo()) == null || !extra.equalsIgnoreCase("ims"))) {
                    LinkProperties linkProperties = netAgentInfo.getNetworkState().linkProperties;
                    VLog.d(TAG, "getInterfaceNameForType " + networkType + " linkProperties:" + linkProperties);
                    if (linkProperties == null) {
                        return null;
                    }
                    return linkProperties.getInterfaceName();
                }
            }
        } catch (Exception ex) {
            loge("getInterfaceNameForType", ex);
        }
        return null;
    }

    private void updateMobileDefaultDns(LinkProperties lp) {
        try {
            if (this.mDefaultMobileDns != null && !lp.getDnsServers().contains(this.mDefaultMobileDns)) {
                lp.addDnsServer(this.mDefaultMobileDns);
                log("addDnsServer for mobile");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isWifiPoorLink(NetworkAgentInfo nai) {
        boolean z = true;
        boolean isWifiPoorLink = (nai.networkInfo.getType() == 1 && nai.networkInfo.getDetailedState() == NetworkInfo.DetailedState.VERIFYING_POOR_LINK) ? false : false;
        log("isWifiPoorLink:" + isWifiPoorLink + ", nai.everValidated:" + nai.everValidated + ", networkType:" + nai.networkInfo.getType() + ", networkDetailedState:" + nai.networkInfo.getDetailedState() + ", nai.isLingering():" + nai.isLingering());
        return isWifiPoorLink;
    }

    public void vivoHandleNetworkAgentMessage(Message msg, HashMap<Messenger, NetworkAgentInfo> networkAgentInfos) {
        if (msg.what == 528484) {
            log("EVENT_VIVO_EXPLICITLY_CLEAR from NetworkAgent");
            NetworkAgentInfo vivoNai = networkAgentInfos.get(msg.replyTo);
            if (vivoNai == null) {
                loge("EVENT_VIVO_EXPLICITLY_CLEAR from unknown NetworkAgent");
                return;
            }
            vivoNai.networkAgentConfig.explicitlySelected = false;
            vivoNai.networkAgentConfig.acceptUnvalidated = false;
        }
    }

    private void initWifiDefautDns() {
        try {
            if (isOverseasWithCountryCodeCheck()) {
                this.mDefaultWlanDns1 = NetworkUtils.numericToInetAddress("8.8.8.8");
            } else {
                this.mDefaultWlanDns1 = NetworkUtils.numericToInetAddress("114.114.114.114");
            }
            this.mDefaultWlanDns3 = NetworkUtils.numericToInetAddress("0.0.0.0");
        } catch (IllegalArgumentException e) {
            loge("Error setting defaultDns " + e);
        }
    }

    public void updateDefaultDns(LinkProperties lp) {
        try {
            String iface = lp.getInterfaceName();
            if (iface == null) {
                log("updateDefaultDns with empty iface ");
            } else if (iface.contains("wlan")) {
                updateWifiDefaultDns(lp);
            } else {
                updateMobileDefaultDns(lp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isNeedUpdateDnsPolicyForNetCoexist() {
        try {
            Class clazz = getWifiManager().getClass();
            Method method = clazz.getDeclaredMethod("isNeedUpdateDnsPolicyForNetCoexist", new Class[0]);
            return ((Boolean) method.invoke(mWifiManager, new Object[0])).booleanValue();
        } catch (Exception e) {
            return false;
        }
    }

    private void updateWifiConfigDns(boolean hasDns) {
        try {
            Class clazz = getWifiManager().getClass();
            Method method = clazz.getDeclaredMethod("updateWifiConfigDns", Boolean.TYPE);
            method.invoke(mWifiManager, Boolean.valueOf(hasDns));
        } catch (Exception e) {
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:14:0x001e A[RETURN] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private void updateWifiDefaultDns(android.net.LinkProperties r4) {
        /*
            r3 = this;
            boolean r0 = r3.getSwitchStateForNetCoexist()     // Catch: java.lang.Exception -> Laf
            if (r0 == 0) goto L1f
            boolean r0 = r4.hasIPv4DnsServer()     // Catch: java.lang.Exception -> Laf
            if (r0 != 0) goto L1b
            boolean r0 = r3.isOverseas()     // Catch: java.lang.Exception -> Laf
            if (r0 != 0) goto L1b
            boolean r0 = r3.isNeedUpdateDnsPolicyForNetCoexist()     // Catch: java.lang.Exception -> Laf
            if (r0 == 0) goto L19
            goto L1b
        L19:
            r0 = 0
            goto L1c
        L1b:
            r0 = 1
        L1c:
            if (r0 != 0) goto L1f
            return
        L1f:
            java.net.InetAddress r0 = r3.mDefaultWlanDns3     // Catch: java.lang.Exception -> Laf
            if (r0 == 0) goto L34
            java.util.List r0 = r4.getDnsServers()     // Catch: java.lang.Exception -> Laf
            java.net.InetAddress r1 = r3.mDefaultWlanDns3     // Catch: java.lang.Exception -> Laf
            boolean r0 = r0.contains(r1)     // Catch: java.lang.Exception -> Laf
            if (r0 == 0) goto L34
            java.net.InetAddress r0 = r3.mDefaultWlanDns3     // Catch: java.lang.Exception -> Laf
            r4.removeDnsServer(r0)     // Catch: java.lang.Exception -> Laf
        L34:
            boolean r0 = r3.mWifiDnsOptimizationEnabled     // Catch: java.lang.Exception -> Laf
            if (r0 != 0) goto L4e
            java.lang.String r0 = "PD1986"
            java.lang.String r1 = "ro.vivo.product.model"
            java.lang.String r2 = "null"
            java.lang.String r1 = android.os.SystemProperties.get(r1, r2)     // Catch: java.lang.Exception -> Laf
            boolean r0 = r0.equals(r1)     // Catch: java.lang.Exception -> Laf
            if (r0 == 0) goto L4e
            java.lang.String r0 = "Remove backup dns servers for PD1986."
            log(r0)     // Catch: java.lang.Exception -> Laf
            return
        L4e:
            java.net.InetAddress r0 = r3.mDefaultWlanDns1     // Catch: java.lang.Exception -> Laf
            java.lang.String r1 = "add dns provided for using "
            if (r0 == 0) goto L7d
            java.util.List r0 = r4.getDnsServers()     // Catch: java.lang.Exception -> Laf
            java.net.InetAddress r2 = r3.mDefaultWlanDns1     // Catch: java.lang.Exception -> Laf
            boolean r0 = r0.contains(r2)     // Catch: java.lang.Exception -> Laf
            if (r0 != 0) goto L7d
            java.net.InetAddress r0 = r3.mDefaultWlanDns1     // Catch: java.lang.Exception -> Laf
            r4.addDnsServer(r0)     // Catch: java.lang.Exception -> Laf
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch: java.lang.Exception -> Laf
            r0.<init>()     // Catch: java.lang.Exception -> Laf
            r0.append(r1)     // Catch: java.lang.Exception -> Laf
            java.net.InetAddress r2 = r3.mDefaultWlanDns1     // Catch: java.lang.Exception -> Laf
            java.lang.String r2 = r2.getHostAddress()     // Catch: java.lang.Exception -> Laf
            r0.append(r2)     // Catch: java.lang.Exception -> Laf
            java.lang.String r0 = r0.toString()     // Catch: java.lang.Exception -> Laf
            log(r0)     // Catch: java.lang.Exception -> Laf
        L7d:
            boolean r0 = r3.mWifiDnsOptimizationEnabled     // Catch: java.lang.Exception -> Laf
            if (r0 != 0) goto Lae
            java.net.InetAddress r0 = r3.mDefaultWlanDns2     // Catch: java.lang.Exception -> Laf
            if (r0 == 0) goto Lae
            java.util.List r0 = r4.getDnsServers()     // Catch: java.lang.Exception -> Laf
            java.net.InetAddress r2 = r3.mDefaultWlanDns2     // Catch: java.lang.Exception -> Laf
            boolean r0 = r0.contains(r2)     // Catch: java.lang.Exception -> Laf
            if (r0 != 0) goto Lae
            java.net.InetAddress r0 = r3.mDefaultWlanDns2     // Catch: java.lang.Exception -> Laf
            r4.addDnsServer(r0)     // Catch: java.lang.Exception -> Laf
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch: java.lang.Exception -> Laf
            r0.<init>()     // Catch: java.lang.Exception -> Laf
            r0.append(r1)     // Catch: java.lang.Exception -> Laf
            java.net.InetAddress r1 = r3.mDefaultWlanDns2     // Catch: java.lang.Exception -> Laf
            java.lang.String r1 = r1.getHostAddress()     // Catch: java.lang.Exception -> Laf
            r0.append(r1)     // Catch: java.lang.Exception -> Laf
            java.lang.String r0 = r0.toString()     // Catch: java.lang.Exception -> Laf
            log(r0)     // Catch: java.lang.Exception -> Laf
        Lae:
            goto Lb3
        Laf:
            r0 = move-exception
            r0.printStackTrace()
        Lb3:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.VivoConnectivityServiceImpl.updateWifiDefaultDns(android.net.LinkProperties):void");
    }

    public void handleUpdateLinkProperties(NetworkAgentInfo networkAgent) {
        try {
            if (getSwitchStateForNetCoexist() && !this.isOverseas) {
                setWifiHasNoDnsVariable(networkAgent);
                if (needAddPolicyRoute()) {
                    addPolicyRoute(networkAgent);
                }
            }
            if (networkAgent.everConnected || networkAgent.networkInfo.getDetailedState() == NetworkInfo.DetailedState.CAPTIVE_PORTAL_CHECK) {
                updateCurrentLinkProperties(networkAgent.network.netId, new LinkProperties(networkAgent.linkProperties));
            }
        } catch (Exception ex) {
            log("catch Exception:" + ex);
        }
    }

    public void handleUpdateNetworkInfo(NetworkInfo ni) {
        setWifiHasNoDnsVariable(ni);
        setNetworkCheckState(ni);
    }

    private void setWifiHasNoDnsVariable(NetworkAgentInfo networkAgent) {
        if (networkAgent == null) {
            return;
        }
        boolean wifiNoDnsFlag = false;
        LinkProperties lp = new LinkProperties(networkAgent.linkProperties);
        NetworkInfo ni = networkAgent.networkInfo;
        boolean change = false;
        if (ni != null && ni.getType() == 1 && lp.hasIPv4Address()) {
            if (!lp.hasIPv4DnsServer()) {
                if (!this.mWifiNoDns) {
                    wifiNoDnsFlag = true;
                    change = true;
                    log("switch to no-dns wifi, so need set mWifiNoDns : true");
                }
            } else {
                String notSupportApp = getBlackListForNetCoexist();
                boolean isNotSupported = notSupportApp.contains(APP_EASYSHARE);
                WifiInfo mWifiInfo = getWifiManager().getConnectionInfo();
                if (mWifiInfo != null && isIntertransSoftAp(mWifiInfo.getSSID()) && !isNotSupported && isSupportNetcoexist()) {
                    if (!this.mWifiNoDns) {
                        wifiNoDnsFlag = true;
                        change = true;
                        log("Intertrans SoftAp, set mWifiNoDns : true");
                    }
                } else if (this.mWifiNoDns) {
                    wifiNoDnsFlag = false;
                    change = true;
                    log("switch to has-dns wifi, so need set mWifiNoDns : false");
                }
            }
            updateWifiConfigDns(lp.hasIPv4DnsServer());
        }
        if (change) {
            if (wifiNoDnsFlag) {
                if (setNetidGwForNetCoexist(1, lp, networkAgent.network)) {
                    this.mWifiNoDns = true;
                    Settings.Global.putInt(this.mContext.getContentResolver(), "wifi_no_dns", 1);
                    this.mHandler.postDelayed(this.VCD_WIFI_P_1, 3000L);
                    return;
                }
                return;
            }
            setNetidGwForNetCoexist(0, null, null);
            this.mWifiNoDns = false;
            Settings.Global.putInt(this.mContext.getContentResolver(), "wifi_no_dns", 0);
            hasSendNetCoexistBroadcast = false;
            hasAddPolicyRoute = false;
            this.VCD_WIFI_P_2 = 0;
            this.mHandler.removeCallbacks(this.VCD_WIFI_P_1);
            if (hasAddNetCoexistNetworkRule) {
                hasAddNetCoexistNetworkRule = false;
                this.mHandler.removeCallbacks(this.mAddNetcoexistNetworkRule);
                this.mHandler.post(this.mDelNetcoexistNetworkRule);
            }
        }
    }

    private void setWifiHasNoDnsVariable(NetworkInfo ni) {
        if (ni != null && 1 == ni.getType() && NetworkInfo.State.DISCONNECTED == ni.getState() && this.mWifiNoDns) {
            this.mWifiNoDns = false;
            Settings.Global.putInt(this.mContext.getContentResolver(), "wifi_no_dns", 0);
            setNetidGwForNetCoexist(0, null, null);
            log("wifi disconnect, so set mWifiNoDns : " + this.mWifiNoDns);
            hasSendNetCoexistBroadcast = false;
            hasAddPolicyRoute = false;
            this.VCD_WIFI_P_2 = 0;
            this.mHandler.removeCallbacks(this.VCD_WIFI_P_1);
            if (hasAddNetCoexistNetworkRule) {
                hasAddNetCoexistNetworkRule = false;
                this.mHandler.removeCallbacks(this.mAddNetcoexistNetworkRule);
                this.mHandler.post(this.mDelNetcoexistNetworkRule);
            }
        }
    }

    protected void addPolicyRoute(NetworkAgentInfo networkAgent) {
    }

    private boolean needAddPolicyRoute() {
        return this.mWifiNoDns;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public String getNetAddress(LinkProperties lp) {
        List<LinkAddress> linkAddrs;
        if (lp == null || (linkAddrs = lp.getLinkAddresses()) == null) {
            return null;
        }
        for (LinkAddress linkAddr : linkAddrs) {
            InetAddress addr = linkAddr.getAddress();
            if (addr != null && (addr instanceof Inet4Address)) {
                int prefix = linkAddr.getPrefixLength();
                InetAddress netAddr_ia = NetworkUtils.getNetworkPart(addr, prefix);
                String netAddr = netAddr_ia.getHostName();
                return netAddr + "/" + prefix;
            }
        }
        return null;
    }

    private boolean isIntertransSoftAp(String ssid) {
        Matcher match;
        if (ssid == null || ssid.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK)) {
            log("current ssid is null");
            return false;
        }
        try {
            match = AP_NAME_PATTERN.matcher(ssid);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (match.find()) {
            log("ssid:" + ssid + " parse AP_NAME_PATTERN string success");
            return true;
        }
        Matcher match2 = AP_NAME_HJ_PATTERN.matcher(ssid);
        if (!match2.find()) {
            return isEasyShareAp(ssid);
        }
        log("ssid:" + ssid + " parse AP_NAME_HJ_PATTERN string success");
        return true;
    }

    private boolean isEasyShareAp(String ssid) {
        Matcher match;
        if (TextUtils.isEmpty(ssid)) {
            return false;
        }
        try {
            PackageManager pm = this.mContext.getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(EASYSHARE_PACKAGE_NAME, 128);
            String ssidPattern = info.metaData.getString("ssid_pattern");
            VLog.d(TAG, "isEasyShareAp ssid_pattern:" + ssidPattern);
            Pattern newPattern = Pattern.compile(ssidPattern);
            match = newPattern.matcher(ssid);
        } catch (Exception e) {
            VLog.e(TAG, "isEasyShareAp error:" + e);
        }
        if (match.find()) {
            VLog.d(TAG, "isEasyShareAp ssid:" + ssid + " parse newPattern string success");
            return true;
        }
        return false;
    }

    private WifiManager getWifiManager() {
        if (mWifiManager == null) {
            mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        }
        return mWifiManager;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void sendNetCoexistAvailableBroadcast() {
        new Thread(new Runnable() { // from class: com.android.server.VivoConnectivityServiceImpl.1
            @Override // java.lang.Runnable
            public void run() {
                try {
                    Thread.sleep(100L);
                    Intent intent = new Intent("vivo_wifi_netcoexist_available");
                    intent.addFlags(67108864);
                    VivoConnectivityServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
                    VivoConnectivityServiceImpl.log("send netcoexist available broadcast");
                } catch (Exception e) {
                }
            }
        }).start();
    }

    private boolean isSupportNetcoexist() {
        try {
            PackageManager pm = this.mContext.getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(EASYSHARE_PACKAGE_NAME, 128);
            boolean sIsSupport = info.metaData.getBoolean(EASYSHARE_SUPPORT_KEY);
            return sIsSupport;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getPackageName(int uid) {
        String packageName = this.mContext.getPackageManager().getNameForUid(uid);
        return packageName == null ? Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK : packageName;
    }

    private Network findNetwork(int nettype) {
        try {
            Network[] networks = this.mConnectivityService.getAllNetworks();
            if (networks == null || networks.length <= 0) {
                return null;
            }
            for (Network net : networks) {
                NetworkInfo info = this.mConnectivityService.getNetworkInfoForUid(net, 1000, false);
                if (info != null && info.getType() == nettype) {
                    return net;
                }
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public NetworkInfo assembleWifiNetworkInfo(int networkType, int uid) {
        try {
            String packageName = getPackageName(uid);
            if (this.mWifiNoDns && networkType == 1 && ((!TextUtils.isEmpty(packageName) && packageName.equals(EASYSHARE_PACKAGE_NAME)) || isAllowedAssemblePackage(uid, packageName))) {
                NetworkInfo ni = new NetworkInfo(1, 0, "WIFI", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                ni.setDetailedState(NetworkInfo.DetailedState.CONNECTED, null, null);
                ni.setIsAvailable(true);
                return ni;
            }
        } catch (Exception e) {
        }
        return null;
    }

    public Network assembleWifiNetwork(int uid) {
        try {
            String packageName = getPackageName(uid);
            if (this.mWifiNoDns && isAllowedAssemblePackage(uid, packageName)) {
                return findNetwork(1);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public void handleRematchForNetCoexist(NetworkAgentInfo oldDefaultNetwork, NetworkAgentInfo newDefaultNetwork) {
        boolean isNewDefault = false;
        try {
            if (this.mWifiNoDns) {
                if (oldDefaultNetwork != null && newDefaultNetwork != null) {
                    if (oldDefaultNetwork == newDefaultNetwork) {
                        isNewDefault = true;
                    }
                } else if (oldDefaultNetwork == null && newDefaultNetwork != null && newDefaultNetwork.networkInfo.getType() == 1) {
                    isNewDefault = true;
                }
                StringBuilder sb = new StringBuilder();
                sb.append("old:");
                sb.append(oldDefaultNetwork != null ? Integer.valueOf(oldDefaultNetwork.networkInfo.getType()) : Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                sb.append(", new:");
                sb.append(newDefaultNetwork != null ? Integer.valueOf(newDefaultNetwork.networkInfo.getType()) : Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                sb.append(", nodns:");
                sb.append(this.mWifiNoDns);
                sb.append(", isNewDefault:");
                sb.append(isNewDefault);
                sb.append(", hasSendNetCoexistBroadcast:");
                sb.append(hasSendNetCoexistBroadcast);
                log(sb.toString());
            }
            if (isNewDefault) {
                if (!hasAddNetCoexistNetworkRule && this.netcoexist_version != 2 && this.kidSwitchOn) {
                    hasAddNetCoexistNetworkRule = true;
                    this.mHandler.post(this.mAddNetcoexistNetworkRule);
                }
                if (!hasSendNetCoexistBroadcast) {
                    log("connected to no-dns wifi, send wifi connected broadcast");
                    hasSendNetCoexistBroadcast = true;
                    NetworkInfo info = new NetworkInfo(1, 0, "WIFI", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                    info.setDetailedState(NetworkInfo.DetailedState.CONNECTED, null, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                    info.setIsAvailable(true);
                    this.mConnectivityService.sendConnectedBroadcast(info);
                }
            }
        } catch (Exception e) {
            log("handleRematchForNetCoexist error");
        }
    }

    private boolean isAllowedAssemblePackage(int uid, String packageName) {
        if (uid == 1000 || notAllowedAssemblePackage(packageName)) {
            return false;
        }
        return true;
    }

    private boolean notAllowedAssemblePackage(String packagename) {
        String[] strArr;
        if (TextUtils.isEmpty(packagename)) {
            return false;
        }
        for (String str : this.noAllowedPackageList) {
            if (packagename.toLowerCase().contains(str)) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void VCD_WIFI_2() {
        try {
            this.VCD_WIFI_P_2++;
            log("forgroundApp = " + this.mForgroundAppName);
            HashMap<String, String> netcoexistData = new HashMap<>();
            netcoexistData.put("driverecorder", this.mForgroundAppName);
            WifiInfo info = getWifiManager().getConnectionInfo();
            if (info != null) {
                netcoexistData.put("drivessid", info.getSSID());
                netcoexistData.put("drivebssid", info.getBSSID());
            }
            VCD_WIFI_1(netcoexistData);
            if (this.VCD_WIFI_P_2 == 1) {
                this.mHandler.postDelayed(this.VCD_WIFI_P_1, 5000L);
            }
            if (this.VCD_WIFI_P_2 >= 2) {
                this.VCD_WIFI_P_2 = 0;
            }
        } catch (Exception ex) {
            log("writeNetcoxistData exception: " + ex);
        }
    }

    private void VCD_WIFI_1(HashMap<String, String> params) {
        if (params == null || params.size() == 0) {
            return;
        }
        try {
            VivoCollectData vivoCollectData = new VivoCollectData(this.mContext);
            if (vivoCollectData.getControlInfo("201")) {
                HashMap<String, String> currentParams = new HashMap<>(params);
                currentParams.put(VCD_FF_1.UUID_STR, UUID.randomUUID().toString());
                EventTransfer.getInstance().singleEvent(WIFI_VCODE, WIFI_NETCOEXIST, System.currentTimeMillis(), 0L, currentParams);
                vivoCollectData.writeData("201", "20127", System.currentTimeMillis(), System.currentTimeMillis(), 0L, 1, currentParams);
                StringBuffer sbuf = new StringBuffer("VCD_WIFI_1:");
                for (Map.Entry entry : currentParams.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    sbuf.append(" " + key + "[" + value + "]");
                }
                log(sbuf.toString());
            }
        } catch (Exception e) {
            log(VLog.getStackTraceString(e));
        }
    }

    private void registerProcessObserver() {
        try {
            log("registerProcessObserver");
            ActivityManagerNative.getDefault().registerProcessObserver(this.mProcessObserver);
        } catch (Exception e) {
            log("error registerProcessObserver " + e);
        }
    }

    private void setupVivoBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("vivo_wifi_featureconfig_update");
        this.mContext.registerReceiver(new BroadcastReceiver() { // from class: com.android.server.VivoConnectivityServiceImpl.4
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("vivo_wifi_featureconfig_update".equals(action)) {
                    try {
                        VivoConnectivityServiceImpl.log("setupVivoBroadcastReceiver action:" + action);
                        if (action.equals("vivo_wifi_featureconfig_update")) {
                            String blackList = VivoConnectivityServiceImpl.this.getBlackListForNetCoexist();
                            if (blackList.contains("kidswitch:on")) {
                                VivoConnectivityServiceImpl.this.kidSwitchOn = true;
                            } else {
                                VivoConnectivityServiceImpl.this.kidSwitchOn = false;
                            }
                            if (blackList.contains("version:1")) {
                                VivoConnectivityServiceImpl.this.netcoexist_version = 1;
                            }
                            VivoConnectivityServiceImpl.this.updateNetcoexistList();
                        }
                    } catch (Exception ex) {
                        VivoConnectivityServiceImpl.loge("setupVivoBroadcastReceiver exception:" + ex);
                    }
                    VivoConnectivityServiceImpl.log("kidSwitchOn=" + VivoConnectivityServiceImpl.this.kidSwitchOn);
                }
            }
        }, filter);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public static void log(String s) {
        VSlog.d(TAG, s);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void loge(String s) {
        VSlog.e(TAG, s);
    }

    private static void loge(String s, Throwable t) {
        VSlog.e(TAG, s, t);
    }

    private void initGameAcceleratorAppList() {
        GameAcceleratorMap.add("com.netease.uu");
        GameAcceleratorMap.add("com.tencent.cmocmna");
        GameAcceleratorMap.add("cn.wsds.gamemaster");
    }

    public void VivoHandleCallbackForRequest(int notificationType, int uid, NetworkRequest nr, NetworkAgentInfo nai) {
        if (nr == null || nai == null || uid == 1000) {
            return;
        }
        try {
            String packageName = getPackageName(uid);
            if (!packageName.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK) && GameAcceleratorMap.contains(packageName)) {
                NetworkCapabilities nc = nai.networkCapabilities;
                if (nc.hasTransport(8)) {
                    if (notificationType == 524290 && nr.isRequest()) {
                        Settings.Global.putInt(this.mContext.getContentResolver(), GAME_ACCELERATOR_STATE, 1);
                    }
                    log("VivoHandleCallbackForRequest notificationType:" + notificationType + " packagename:" + getPackageName(uid) + " request:" + nr.isRequest() + " hasExtWifiTransport:" + nc.hasTransport(8));
                }
            }
        } catch (Exception ex) {
            loge("VivoHandleCallbackForRequest exception:" + ex);
        }
    }

    private void initNetcoexistWhiteList() {
        mNetcoexistWhiteList.clear();
        mNetcoexistWhiteList.add("com.tencent.qqlite");
        mNetcoexistWhiteList.add("com.tencent.mobileqq");
        mNetcoexistWhiteList.add("com.tencent.minihd.qq");
        mNetcoexistWhiteList.add("com.tencent.mobileqqi");
        mNetcoexistWhiteList.add(Constant.APP_WEIXIN);
        mNetcoexistWhiteList.add("cn.kuwo.kwmusichd");
        mNetcoexistWhiteList.add("cn.kuwo.player");
        mNetcoexistWhiteList.add("com.kugou.android.lite");
        mNetcoexistWhiteList.add("com.kugou.android");
        mNetcoexistWhiteList.add("com.kugou.android.elder");
        mNetcoexistWhiteList.add("com.android.bbkmusic");
        mNetcoexistWhiteList.add("com.tencent.qqmusic");
        mNetcoexistWhiteList.add("com.tencent.qqmusiccar");
        mNetcoexistWhiteList.add("com.tencent.qqmusicpad");
        mNetcoexistWhiteList.add("com.netease.cloudmusic");
        mNetcoexistWhiteList.add("com.ximalaya.ting.android");
        mNetcoexistWhiteList.add("com.ximalaya.ting.lite");
        mNetcoexistWhiteList.add("fm.xiami.main");
        mNetcoexistWhiteList.add("com.tencent.map");
        mNetcoexistWhiteList.add("com.baidu.BaiduMap");
        mNetcoexistWhiteList.add("com.autonavi.minimap");
    }

    private int getPackageUid(String packagename) {
        try {
            PackageManager pm = this.mContext.getPackageManager();
            int uid = pm.getPackageUid(packagename, 0);
            return uid;
        } catch (Exception e) {
            return -1;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addNetcoexistNetworkRule() {
        try {
            log("netcoexist addNetcoexistNetworkRule begin ....................");
            this.mNetworkManagementService.setNetworkForbidRule(0, true);
            synchronized (mSyncNetcoexistWhiteListLock) {
                int size = mNetcoexistWhiteList.size();
                log("netcoexist NetcoexistWhiteList size = " + size);
                for (int i = 0; i < size; i++) {
                    String packageName = mNetcoexistWhiteList.get(i);
                    int uid = getPackageUid(packageName);
                    if (uid != -1) {
                        log("netcoexist addNetcoexistNetworkRule packagename:" + packageName);
                        this.mNetworkManagementService.setNetworkAccessRuleForUid(0, uid, true);
                    }
                }
            }
            log("netcoexist addNetcoexistNetworkRule end ....................");
        } catch (Exception ex) {
            loge("netcoexist add NetCoexist network rule exception:" + ex);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void delNetcoexistNetworkRule() {
        try {
            log("netcoexist delNetcoexistNetworkRule begin ....................");
            this.mNetworkManagementService.setNetworkForbidRule(0, false);
            synchronized (mSyncNetcoexistWhiteListLock) {
                int size = mNetcoexistWhiteList.size();
                for (int i = 0; i < size; i++) {
                    String packageName = mNetcoexistWhiteList.get(i);
                    int uid = getPackageUid(packageName);
                    if (uid != -1) {
                        log("netcoexist delNetcoexistNetworkRule packagename:" + packageName);
                        this.mNetworkManagementService.setNetworkAccessRuleForUid(0, uid, false);
                    }
                }
            }
            log("netcoexist delNetcoexistNetworkRule end ....................");
        } catch (Exception ex) {
            loge("netcoexist del NetCoexist network rule exception:" + ex);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateNetcoexistList() {
        String whitelist = getWhiteListForNetCoexist();
        log("netcoexist whitelist: " + whitelist);
        String blacklist = getBlackListForNetCoexist();
        log("netcoexist blacklist: " + blacklist);
        synchronized (mSyncNetcoexistWhiteListLock) {
            try {
                if (!TextUtils.isEmpty(whitelist)) {
                    String[] whitelistApps = whitelist.split(";");
                    for (String app : whitelistApps) {
                        if (!mNetcoexistWhiteList.contains(app)) {
                            mNetcoexistWhiteList.add(app);
                        }
                    }
                }
                if (!TextUtils.isEmpty(blacklist)) {
                    String[] blacklistApps = blacklist.split(";");
                    for (String app2 : blacklistApps) {
                        if (!app2.equals("kidswitch:on") && mNetcoexistWhiteList.contains(app2)) {
                            mNetcoexistWhiteList.remove(app2);
                        }
                    }
                }
            } catch (Exception ex) {
                loge("updateNetcoexistList exception:" + ex);
            }
        }
    }

    public boolean shouldBlockNotificationForAIS() {
        int lockFlag = 0;
        try {
            lockFlag = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoPhoneLockManager().isPhoneLocked();
        } catch (Exception e) {
        }
        StringBuilder sb = new StringBuilder();
        sb.append("shouldBlockNotificationForAIS=");
        sb.append(lockFlag == 1);
        VSlog.d(TAG, sb.toString());
        return lockFlag == 1;
    }

    public void showBlockedToastForAIS() {
        VivoSystemOverlayToast.makeText(this.mContext, 51249232, 1).show();
    }

    public void recordPrivateDnsInfo(ComponentName who, int mode, String privateDnsHost) {
        this.mHandler.post(new RecordPdnsInfo(1, who, mode, privateDnsHost));
    }

    /* loaded from: classes.dex */
    public class RecordPdnsInfo implements Runnable {
        private int mFrom;
        private int mMode;
        private String mPrivateDnsHost;
        private ComponentName mWho;

        public RecordPdnsInfo(int from, ComponentName who, int mode, String privateDnsHost) {
            this.mFrom = from;
            this.mWho = who;
            this.mMode = mode;
            this.mPrivateDnsHost = privateDnsHost;
        }

        @Override // java.lang.Runnable
        public void run() {
            VivoConnectivityServiceImpl.this.writedata(this.mFrom, this.mWho, this.mMode, this.mPrivateDnsHost);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void writedata(int from, ComponentName who, int mode, String privateDnsHost) {
        try {
            HashMap<String, String> pDnsInfo = new HashMap<>();
            try {
                if (from == 1) {
                    pDnsInfo.put("from", "setinterface");
                    pDnsInfo.put("ComponentName", who != null ? who.toString() : Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                    pDnsInfo.put("mode", String.valueOf(mode));
                    pDnsInfo.put("privateDnsHost", privateDnsHost);
                } else if (from == 2 || from == 3) {
                    String private_dns_mode = read_private_dns_mode(3);
                    String private_dns_default_mode = read_private_dns_default_mode(3);
                    String private_dns_specifier = read_private_dns_specifier(3);
                    if (from == 2) {
                        pDnsInfo.put("from", "settingschange");
                    } else if (from == 3) {
                        pDnsInfo.put("from", "boot");
                    }
                    pDnsInfo.put("private_dns_mode", private_dns_mode);
                    pDnsInfo.put("private_dns_default_mode", private_dns_default_mode);
                    pDnsInfo.put("private_dns_specifier", private_dns_specifier);
                    pDnsInfo.put("private_dns_fallback", "off");
                }
                HashMap<String, String> currentParams = new HashMap<>(pDnsInfo);
                EventTransfer.getInstance().singleEvent(WIFI_VCODE, "F348|10001", System.currentTimeMillis(), 0L, currentParams);
                StringBuffer sbuf = new StringBuffer("reportVcode:");
                for (Map.Entry entry : currentParams.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    sbuf.append("[" + key + ":" + value + "] ");
                }
                log(sbuf.toString());
            } catch (Exception e) {
            }
        } catch (Exception e2) {
        }
    }

    public void setNetworkStateChangeCallback(ConnectivityService.NetworkStateChangeCallback callback) {
        synchronized (this.mNetworkStateChangeCallbackLock) {
            this.mNetworkStateChangeCallback = callback;
        }
    }

    private void registerPdnsSettingsObserver() {
        read_private_dns_mode(1);
        read_private_dns_default_mode(1);
        read_private_dns_specifier(1);
        read_private_dns_fallback(1);
        this.change_boot_flag = true;
        this.mHandler.postDelayed(new RecordPdnsInfo(3, null, 0, null), 20000L);
        this.change_boot_flag = false;
        ContentObserver contentObserver = new ContentObserver(this.mHandler) { // from class: com.android.server.VivoConnectivityServiceImpl.7
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                VivoConnectivityServiceImpl.this.read_private_dns_mode(2);
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("private_dns_mode"), false, contentObserver);
        ContentObserver contentObserver1 = new ContentObserver(this.mHandler) { // from class: com.android.server.VivoConnectivityServiceImpl.8
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                VivoConnectivityServiceImpl.this.read_private_dns_default_mode(2);
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("private_dns_default_mode"), false, contentObserver1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String read_private_dns_mode(int change) {
        ContentResolver cr = this.mContext.getContentResolver();
        String private_dns_mode = Settings.Global.getString(cr, "private_dns_mode");
        log("read_private_dns_mode:" + private_dns_mode + ", change:" + change);
        if (change == 2) {
            this.mHandler.post(new RecordPdnsInfo(2, null, 0, null));
        } else if (change == 1 && !TextUtils.isEmpty(private_dns_mode)) {
            this.change_boot_flag = true;
        }
        return private_dns_mode;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String read_private_dns_default_mode(int change) {
        ContentResolver cr = this.mContext.getContentResolver();
        String private_dns_default_mode = Settings.Global.getString(cr, "private_dns_default_mode");
        log("read_private_dns_default_mode:" + private_dns_default_mode);
        if (change == 2) {
            this.mHandler.post(new RecordPdnsInfo(2, null, 0, null));
        } else if (change == 1 && !TextUtils.isEmpty(private_dns_default_mode)) {
            this.change_boot_flag = true;
        }
        return private_dns_default_mode;
    }

    private String read_private_dns_specifier(int change) {
        ContentResolver cr = this.mContext.getContentResolver();
        String private_dns_specifier = Settings.Global.getString(cr, "private_dns_specifier");
        log("read_private_dns_specifier:" + private_dns_specifier);
        return private_dns_specifier;
    }

    private String read_private_dns_fallback(int change) {
        log("read_private_dns_fallback:off");
        if (change == 2) {
            this.mHandler.post(new RecordPdnsInfo(2, null, 0, null));
        } else if (change == 1 && !TextUtils.isEmpty("off")) {
            this.change_boot_flag = true;
        }
        return "off";
    }

    private boolean getSwitchStateForNetCoexist() {
        try {
            Class clazz = getWifiManager().getClass();
            Method method = clazz.getDeclaredMethod("getSwitchStateForNetCoexist", new Class[0]);
            return ((Boolean) method.invoke(mWifiManager, new Object[0])).booleanValue();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void updateCurrentLinkProperties(int netId, LinkProperties linkProperties) {
        try {
            Class clazz = getWifiManager().getClass();
            Method method = clazz.getDeclaredMethod("updateCurrentLinkProperties", Integer.TYPE, LinkProperties.class);
            method.invoke(mWifiManager, Integer.valueOf(netId), linkProperties);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getBlackListForNetCoexist() {
        try {
            Class clazz = getWifiManager().getClass();
            Method method = clazz.getDeclaredMethod("getBlackListForNetCoexist", new Class[0]);
            return (String) method.invoke(mWifiManager, new Object[0]);
        } catch (Exception e) {
            e.printStackTrace();
            return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        }
    }

    private String getWhiteListForNetCoexist() {
        try {
            Class clazz = getWifiManager().getClass();
            Method method = clazz.getDeclaredMethod("getWhiteListForNetCoexist", new Class[0]);
            return (String) method.invoke(mWifiManager, new Object[0]);
        } catch (Exception e) {
            e.printStackTrace();
            return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        }
    }

    public void recordDnsEvent(NetworkAgentInfo nai, int eventType, int returnCode, String hostname, String[] ipAddresses, int ipAddressesCount, long timestamp, int uid) {
        VivoNetworkAnalyser vivoNetworkAnalyser = this.mVivoNetworkAnalyser;
        if (vivoNetworkAnalyser != null) {
            vivoNetworkAnalyser.recordDnsEvent(nai, eventType, returnCode, hostname, ipAddresses, ipAddressesCount, timestamp, uid);
        }
    }

    private boolean isWifi(NetworkAgentInfo info) {
        boolean wifi = false;
        if (info != null) {
            try {
                if (info.networkInfo != null) {
                    if (info.networkInfo.getType() == 1) {
                        wifi = true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        VSlog.d(TAG, "isWifi " + wifi);
        return wifi;
    }

    private boolean isExtWifi(NetworkAgentInfo info) {
        boolean extwifi = false;
        if (info != null) {
            try {
                if (info.networkInfo != null) {
                    if (info.networkInfo.getType() == 38) {
                        extwifi = true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        VSlog.d(TAG, "isExtWifi " + extwifi);
        return extwifi;
    }

    private boolean isMobile(NetworkAgentInfo info) {
        boolean mobile = false;
        if (info != null) {
            try {
                if (info.networkInfo != null) {
                    if (info.networkInfo.getType() == 0) {
                        mobile = true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        VSlog.d(TAG, "isMobile " + mobile);
        return mobile;
    }

    private void registerKidswitchObserver() {
        ContentObserver contentObserver = new ContentObserver(this.mHandler) { // from class: com.android.server.VivoConnectivityServiceImpl.9
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                VivoConnectivityServiceImpl.this.read_kidswitch();
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("netcoexistkidswitch"), false, contentObserver);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void read_kidswitch() {
        ContentResolver cr = this.mContext.getContentResolver();
        this.kidSwitchOn = 1 == Settings.Global.getInt(cr, "netcoexistkidswitch", 0);
    }

    public boolean isNetCoexistScene() {
        return this.mWifiNoDns;
    }

    private void initWifiDnsOptimizationEnabled() {
        if (FtFeature.isFeatureSupport(FEATURE_WIFIDNSOPTIMIZATION) && "true".equals(FtFeature.getFeatureAttribute(FEATURE_WIFIDNSOPTIMIZATION, "enabled", "false"))) {
            this.mWifiDnsOptimizationEnabled = true;
        }
    }

    private void initNetCoexistConfig() {
        if (FtFeature.isFeatureSupport(FEATURE_NETCOEXIST) && "true".equals(FtFeature.getFeatureAttribute(FEATURE_NETCOEXIST, ATTR_SUPPORT, "false"))) {
            this.netcoexist_support = true;
            if ("2".equals(FtFeature.getFeatureAttribute(FEATURE_NETCOEXIST, ATTR_VERSION, "1"))) {
                this.netcoexist_version = 2;
            }
        }
    }

    private boolean setNetidGwForNetCoexist(int state, LinkProperties lp, Network network) {
        String wifiGw = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        if (this.netcoexist_version < 2) {
            return false;
        }
        if (state == 1 || state == 0) {
            try {
                if (state == 1) {
                    if (lp != null && network != null && lp.getInterfaceName().equals("wlan0")) {
                        for (RouteInfo route : lp.getRoutes()) {
                            if (route.hasGateway()) {
                                InetAddress gateway = route.getGateway();
                                if (gateway instanceof Inet4Address) {
                                    wifiGw = gateway.getHostAddress();
                                    if (!TextUtils.isEmpty(wifiGw)) {
                                        break;
                                    }
                                } else {
                                    continue;
                                }
                            }
                        }
                        int wifiNetId = network.netId;
                        String netSeg = getNetAddress(lp);
                        String netmask = getNetmaskAddress(lp);
                        if (wifiNetId >= 100 && !TextUtils.isEmpty(netSeg) && !TextUtils.isEmpty(netmask)) {
                            VSlog.d(TAG, "setNetidGwForNetCoexist state:" + state + ", wifigw:" + wifiGw + ", wifiNetId:" + wifiNetId);
                            this.mNetworkManagementService.setNetidGwForNetCoexist(state, wifiNetId, netSeg, netmask);
                            return true;
                        }
                    }
                } else if (state == 0) {
                    VSlog.d(TAG, "setNetidGwForNetCoexist reset");
                    this.mNetworkManagementService.setNetidGwForNetCoexist(0, 0, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
                    return true;
                }
            } catch (Exception e) {
            }
            return false;
        }
        return false;
    }

    private String getNetmaskAddress(LinkProperties lp) {
        List<LinkAddress> linkAddrs;
        String str = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        if (lp == null || (linkAddrs = lp.getLinkAddresses()) == null) {
            return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        }
        for (LinkAddress linkAddr : linkAddrs) {
            InetAddress addr = linkAddr.getAddress();
            if (addr != null && (addr instanceof Inet4Address)) {
                int prefix = linkAddr.getPrefixLength();
                try {
                    return NetworkUtils.intToInetAddress(NetworkUtils.prefixLengthToNetmaskInt(prefix)).getHostName();
                } catch (Exception ex) {
                    VSlog.e(TAG, "getNetmaskAddress ex:" + ex);
                }
            }
        }
        return str;
    }

    private String getNetsegAddress(LinkProperties lp) {
        List<LinkAddress> linkAddrs;
        String str = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        if (lp == null || (linkAddrs = lp.getLinkAddresses()) == null) {
            return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        }
        for (LinkAddress linkAddr : linkAddrs) {
            InetAddress addr = linkAddr.getAddress();
            if (addr != null && (addr instanceof Inet4Address)) {
                try {
                    return NetworkUtils.getNetworkPart(addr, linkAddr.getPrefixLength()).getHostName();
                } catch (Exception ex) {
                    VSlog.e(TAG, "getNetsegAddress ex:" + ex);
                }
            }
        }
        return str;
    }

    public void handleStartWifiTethering(int uid, String packageName) {
        if (!SystemProperties.get(STR_JUDGE_SYS_OR_APP, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK).equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK)) {
            VSlog.d(TAG, "setWifiTethering already set uid in tethersettings");
        } else if (isWhitelistApp(uid, packageName)) {
            SystemProperties.set(STR_JUDGE_SYS_OR_APP, "1000");
        } else {
            SystemProperties.set(STR_JUDGE_SYS_OR_APP, uid + Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        }
    }

    private boolean isWhitelistApp(int uid, String packageName) {
        String systemUiName = "android.uid.systemui:" + uid;
        if (systemUiName.equals(packageName) || "com.vivo.globalsearch".equals(packageName)) {
            return true;
        }
        return false;
    }

    private void notifyMobileNetworkValidInfo(boolean valid) {
        try {
            Class clazz = getWifiManager().getClass();
            Method method = clazz.getDeclaredMethod("notifyMobileNetworkValidInfo", Boolean.TYPE);
            method.invoke(mWifiManager, Boolean.valueOf(valid));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}