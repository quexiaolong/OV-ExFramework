package com.android.server.adb;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.debug.AdbNotifications;
import android.debug.PairDevice;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.AtomicFile;
import android.util.Base64;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.XmlUtils;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.internal.util.dump.DumpUtils;
import com.android.server.FgThread;
import com.android.server.IVivoRmsInjector;
import com.android.server.location.IVivoLocationManagerService;
import com.android.server.notification.SnoozeHelper;
import com.android.server.usage.UnixCalendar;
import com.android.server.usb.descriptors.UsbDescriptor;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/* loaded from: classes.dex */
public class AdbDebuggingManager {
    private static final String ADBD_SOCKET = "adbd";
    private static final String ADB_DIRECTORY = "misc/adb";
    private static final String ADB_KEYS_FILE = "adb_keys";
    private static final String ADB_TEMP_KEYS_FILE = "adb_temp_keys.xml";
    private static final int BUFFER_SIZE = 65536;
    private static final boolean DEBUG = false;
    private static final int FIX_MODE_USER_ID = 888;
    private static final boolean MDNS_DEBUG = false;
    private static final int PAIRING_CODE_LENGTH = 6;
    private static final String TAG = "AdbDebuggingManager";
    private static final String WIFI_PERSISTENT_CONFIG_PROPERTY = "persist.adb.tls_server.enable";
    private static final String WIFI_PERSISTENT_GUID = "persist.adb.wifi.guid";
    private AdbConnectionInfo mAdbConnectionInfo;
    private boolean mAdbUsbEnabled;
    private boolean mAdbWifiEnabled;
    private String mConfirmComponent;
    private final Map<String, Integer> mConnectedKeys;
    private AdbConnectionPortPoller mConnectionPortPoller;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private String mFingerprints;
    private final Handler mHandler;
    private PairingThread mPairingThread;
    private final PortListenerImpl mPortListener;
    private final File mTestUserKeyFile;
    private AdbDebuggingThread mThread;
    private final Set<String> mWifiConnectedKeys;

    /* loaded from: classes.dex */
    interface AdbConnectionPortListener {
        void onPortReceived(int i);
    }

    public AdbDebuggingManager(Context context) {
        this.mAdbUsbEnabled = false;
        this.mAdbWifiEnabled = false;
        this.mPairingThread = null;
        this.mPortListener = new PortListenerImpl();
        this.mHandler = new AdbDebuggingHandler(FgThread.get().getLooper());
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        this.mTestUserKeyFile = null;
        this.mConnectedKeys = new HashMap();
        this.mWifiConnectedKeys = new HashSet();
        this.mAdbConnectionInfo = new AdbConnectionInfo();
    }

    protected AdbDebuggingManager(Context context, String confirmComponent, File testUserKeyFile) {
        this.mAdbUsbEnabled = false;
        this.mAdbWifiEnabled = false;
        this.mPairingThread = null;
        this.mPortListener = new PortListenerImpl();
        this.mHandler = new AdbDebuggingHandler(FgThread.get().getLooper());
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        this.mConfirmComponent = confirmComponent;
        this.mTestUserKeyFile = testUserKeyFile;
        this.mConnectedKeys = new HashMap();
        this.mWifiConnectedKeys = new HashSet();
        this.mAdbConnectionInfo = new AdbConnectionInfo();
    }

    /* loaded from: classes.dex */
    class PairingThread extends Thread implements NsdManager.RegistrationListener {
        static final String SERVICE_PROTOCOL = "adb-tls-pairing";
        private String mGuid;
        private NsdManager mNsdManager;
        private String mPairingCode;
        private int mPort;
        private String mPublicKey;
        private String mServiceName;
        private final String mServiceType;

        private native void native_pairing_cancel();

        private native int native_pairing_start(String str, String str2);

        private native boolean native_pairing_wait();

        PairingThread(String pairingCode, String serviceName) {
            super(AdbDebuggingManager.TAG);
            this.mServiceType = String.format("_%s._tcp.", SERVICE_PROTOCOL);
            this.mPairingCode = pairingCode;
            this.mGuid = SystemProperties.get(AdbDebuggingManager.WIFI_PERSISTENT_GUID);
            this.mServiceName = serviceName;
            if (serviceName == null || serviceName.isEmpty()) {
                this.mServiceName = this.mGuid;
            }
            this.mPort = -1;
            this.mNsdManager = (NsdManager) AdbDebuggingManager.this.mContext.getSystemService("servicediscovery");
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            if (this.mGuid.isEmpty()) {
                Slog.e(AdbDebuggingManager.TAG, "adbwifi guid was not set");
                return;
            }
            int native_pairing_start = native_pairing_start(this.mGuid, this.mPairingCode);
            this.mPort = native_pairing_start;
            if (native_pairing_start <= 0 || native_pairing_start > 65535) {
                Slog.e(AdbDebuggingManager.TAG, "Unable to start pairing server");
                return;
            }
            NsdServiceInfo serviceInfo = new NsdServiceInfo();
            serviceInfo.setServiceName(this.mServiceName);
            serviceInfo.setServiceType(this.mServiceType);
            serviceInfo.setPort(this.mPort);
            this.mNsdManager.registerService(serviceInfo, 1, this);
            Message msg = AdbDebuggingManager.this.mHandler.obtainMessage(21);
            msg.obj = Integer.valueOf(this.mPort);
            AdbDebuggingManager.this.mHandler.sendMessage(msg);
            boolean paired = native_pairing_wait();
            this.mNsdManager.unregisterService(this);
            Bundle bundle = new Bundle();
            bundle.putString("publicKey", paired ? this.mPublicKey : null);
            Message message = Message.obtain(AdbDebuggingManager.this.mHandler, 20, bundle);
            AdbDebuggingManager.this.mHandler.sendMessage(message);
        }

        public void cancelPairing() {
            native_pairing_cancel();
        }

        @Override // android.net.nsd.NsdManager.RegistrationListener
        public void onServiceRegistered(NsdServiceInfo serviceInfo) {
        }

        @Override // android.net.nsd.NsdManager.RegistrationListener
        public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Slog.e(AdbDebuggingManager.TAG, "Failed to register pairing service(err=" + errorCode + "): " + serviceInfo);
            cancelPairing();
        }

        @Override // android.net.nsd.NsdManager.RegistrationListener
        public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
        }

        @Override // android.net.nsd.NsdManager.RegistrationListener
        public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Slog.w(AdbDebuggingManager.TAG, "Failed to unregister pairing service(err=" + errorCode + "): " + serviceInfo);
        }
    }

    /* loaded from: classes.dex */
    static class AdbConnectionPortPoller extends Thread {
        private AdbConnectionPortListener mListener;
        private final String mAdbPortProp = "service.adb.tls.port";
        private final int mDurationSecs = 10;
        private AtomicBoolean mCanceled = new AtomicBoolean(false);

        /* JADX INFO: Access modifiers changed from: package-private */
        public AdbConnectionPortPoller(AdbConnectionPortListener listener) {
            this.mListener = listener;
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            for (int i = 0; i < 10; i++) {
                if (this.mCanceled.get()) {
                    return;
                }
                int port = SystemProperties.getInt("service.adb.tls.port", (int) IVivoRmsInjector.QUIET_TYPE_ALL);
                if (port == -1 || (port > 0 && port <= 65535)) {
                    this.mListener.onPortReceived(port);
                    return;
                }
                SystemClock.sleep(1000L);
            }
            Slog.w(AdbDebuggingManager.TAG, "Failed to receive adb connection port");
            this.mListener.onPortReceived(-1);
        }

        public void cancelAndWait() {
            this.mCanceled.set(true);
            if (isAlive()) {
                try {
                    join();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    /* loaded from: classes.dex */
    class PortListenerImpl implements AdbConnectionPortListener {
        PortListenerImpl() {
        }

        @Override // com.android.server.adb.AdbDebuggingManager.AdbConnectionPortListener
        public void onPortReceived(int port) {
            int i;
            Handler handler = AdbDebuggingManager.this.mHandler;
            if (port > 0) {
                i = 24;
            } else {
                i = 25;
            }
            Message msg = handler.obtainMessage(i);
            msg.obj = Integer.valueOf(port);
            AdbDebuggingManager.this.mHandler.sendMessage(msg);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class AdbDebuggingThread extends Thread {
        private InputStream mInputStream;
        private OutputStream mOutputStream;
        private LocalSocket mSocket;
        private boolean mStopped;

        AdbDebuggingThread() {
            super(AdbDebuggingManager.TAG);
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            while (true) {
                synchronized (this) {
                    if (this.mStopped) {
                        return;
                    }
                    try {
                        openSocketLocked();
                    } catch (Exception e) {
                        SystemClock.sleep(1000L);
                    }
                }
                try {
                    listenToSocket();
                } catch (Exception e2) {
                    SystemClock.sleep(1000L);
                }
            }
        }

        private void openSocketLocked() throws IOException {
            try {
                LocalSocketAddress address = new LocalSocketAddress(AdbDebuggingManager.ADBD_SOCKET, LocalSocketAddress.Namespace.RESERVED);
                this.mInputStream = null;
                LocalSocket localSocket = new LocalSocket(3);
                this.mSocket = localSocket;
                localSocket.connect(address);
                this.mOutputStream = this.mSocket.getOutputStream();
                this.mInputStream = this.mSocket.getInputStream();
                AdbDebuggingManager.this.mHandler.sendEmptyMessage(26);
            } catch (IOException ioe) {
                Slog.e(AdbDebuggingManager.TAG, "Caught an exception opening the socket: " + ioe);
                closeSocketLocked();
                throw ioe;
            }
        }

        private void listenToSocket() throws IOException {
            try {
                byte[] buffer = new byte[65536];
                while (true) {
                    int count = this.mInputStream.read(buffer);
                    if (count < 2) {
                        Slog.w(AdbDebuggingManager.TAG, "Read failed with count " + count);
                        break;
                    } else if (buffer[0] == 80 && buffer[1] == 75) {
                        String key = new String(Arrays.copyOfRange(buffer, 2, count));
                        Slog.d(AdbDebuggingManager.TAG, "Received public key: " + key);
                        Message msg = AdbDebuggingManager.this.mHandler.obtainMessage(5);
                        msg.obj = key;
                        AdbDebuggingManager.this.mHandler.sendMessage(msg);
                    } else if (buffer[0] == 68 && buffer[1] == 67) {
                        String key2 = new String(Arrays.copyOfRange(buffer, 2, count));
                        Slog.d(AdbDebuggingManager.TAG, "Received disconnected message: " + key2);
                        Message msg2 = AdbDebuggingManager.this.mHandler.obtainMessage(7);
                        msg2.obj = key2;
                        AdbDebuggingManager.this.mHandler.sendMessage(msg2);
                    } else if (buffer[0] == 67 && buffer[1] == 75) {
                        String key3 = new String(Arrays.copyOfRange(buffer, 2, count));
                        Slog.d(AdbDebuggingManager.TAG, "Received connected key message: " + key3);
                        Message msg3 = AdbDebuggingManager.this.mHandler.obtainMessage(10);
                        msg3.obj = key3;
                        AdbDebuggingManager.this.mHandler.sendMessage(msg3);
                    } else if (buffer[0] == 87 && buffer[1] == 69) {
                        byte transportType = buffer[2];
                        String key4 = new String(Arrays.copyOfRange(buffer, 3, count));
                        if (transportType == 0) {
                            Slog.d(AdbDebuggingManager.TAG, "Received USB TLS connected key message: " + key4);
                            Message msg4 = AdbDebuggingManager.this.mHandler.obtainMessage(10);
                            msg4.obj = key4;
                            AdbDebuggingManager.this.mHandler.sendMessage(msg4);
                        } else if (transportType == 1) {
                            Slog.d(AdbDebuggingManager.TAG, "Received WIFI TLS connected key message: " + key4);
                            Message msg5 = AdbDebuggingManager.this.mHandler.obtainMessage(22);
                            msg5.obj = key4;
                            AdbDebuggingManager.this.mHandler.sendMessage(msg5);
                        } else {
                            Slog.e(AdbDebuggingManager.TAG, "Got unknown transport type from adbd (" + ((int) transportType) + ")");
                        }
                    } else if (buffer[0] != 87 || buffer[1] != 70) {
                        break;
                    } else {
                        byte transportType2 = buffer[2];
                        String key5 = new String(Arrays.copyOfRange(buffer, 3, count));
                        if (transportType2 == 0) {
                            Slog.d(AdbDebuggingManager.TAG, "Received USB TLS disconnect message: " + key5);
                            Message msg6 = AdbDebuggingManager.this.mHandler.obtainMessage(7);
                            msg6.obj = key5;
                            AdbDebuggingManager.this.mHandler.sendMessage(msg6);
                        } else if (transportType2 == 1) {
                            Slog.d(AdbDebuggingManager.TAG, "Received WIFI TLS disconnect key message: " + key5);
                            Message msg7 = AdbDebuggingManager.this.mHandler.obtainMessage(23);
                            msg7.obj = key5;
                            AdbDebuggingManager.this.mHandler.sendMessage(msg7);
                        } else {
                            Slog.e(AdbDebuggingManager.TAG, "Got unknown transport type from adbd (" + ((int) transportType2) + ")");
                        }
                    }
                }
                Slog.e(AdbDebuggingManager.TAG, "Wrong message: " + new String(Arrays.copyOfRange(buffer, 0, 2)));
                synchronized (this) {
                    closeSocketLocked();
                }
            } catch (Throwable th) {
                synchronized (this) {
                    closeSocketLocked();
                    throw th;
                }
            }
        }

        private void closeSocketLocked() {
            try {
                if (this.mOutputStream != null) {
                    this.mOutputStream.close();
                    this.mOutputStream = null;
                }
            } catch (IOException e) {
                Slog.e(AdbDebuggingManager.TAG, "Failed closing output stream: " + e);
            }
            try {
                if (this.mSocket != null) {
                    this.mSocket.close();
                    this.mSocket = null;
                }
            } catch (IOException ex) {
                Slog.e(AdbDebuggingManager.TAG, "Failed closing socket: " + ex);
            }
            AdbDebuggingManager.this.mHandler.sendEmptyMessage(27);
        }

        void stopListening() {
            synchronized (this) {
                this.mStopped = true;
                closeSocketLocked();
            }
        }

        void sendResponse(String msg) {
            synchronized (this) {
                if (!this.mStopped && this.mOutputStream != null) {
                    try {
                        this.mOutputStream.write(msg.getBytes());
                    } catch (IOException ex) {
                        Slog.e(AdbDebuggingManager.TAG, "Failed to write response:", ex);
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class AdbConnectionInfo {
        private String mBssid;
        private int mPort;
        private String mSsid;

        AdbConnectionInfo() {
            this.mBssid = "";
            this.mSsid = "";
            this.mPort = -1;
        }

        AdbConnectionInfo(String bssid, String ssid) {
            this.mBssid = bssid;
            this.mSsid = ssid;
        }

        AdbConnectionInfo(AdbConnectionInfo other) {
            this.mBssid = other.mBssid;
            this.mSsid = other.mSsid;
            this.mPort = other.mPort;
        }

        public String getBSSID() {
            return this.mBssid;
        }

        public String getSSID() {
            return this.mSsid;
        }

        public int getPort() {
            return this.mPort;
        }

        public void setPort(int port) {
            this.mPort = port;
        }

        public void clear() {
            this.mBssid = "";
            this.mSsid = "";
            this.mPort = -1;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setAdbConnectionInfo(AdbConnectionInfo info) {
        synchronized (this.mAdbConnectionInfo) {
            if (info == null) {
                this.mAdbConnectionInfo.clear();
            } else {
                this.mAdbConnectionInfo = info;
            }
        }
    }

    private AdbConnectionInfo getAdbConnectionInfo() {
        AdbConnectionInfo adbConnectionInfo;
        synchronized (this.mAdbConnectionInfo) {
            adbConnectionInfo = new AdbConnectionInfo(this.mAdbConnectionInfo);
        }
        return adbConnectionInfo;
    }

    /* loaded from: classes.dex */
    class AdbDebuggingHandler extends Handler {
        private static final String ADB_NOTIFICATION_CHANNEL_ID_TV = "usbdevicemanager.adb.tv";
        static final int MESSAGE_ADB_ALLOW = 3;
        static final int MESSAGE_ADB_CLEAR = 6;
        static final int MESSAGE_ADB_CONFIRM = 5;
        static final int MESSAGE_ADB_CONNECTED_KEY = 10;
        static final int MESSAGE_ADB_DENY = 4;
        static final int MESSAGE_ADB_DISABLED = 2;
        static final int MESSAGE_ADB_DISCONNECT = 7;
        static final int MESSAGE_ADB_ENABLED = 1;
        static final int MESSAGE_ADB_PERSIST_KEYSTORE = 8;
        static final int MESSAGE_ADB_UPDATE_KEYSTORE = 9;
        static final int MSG_ADBDWIFI_DISABLE = 12;
        static final int MSG_ADBDWIFI_ENABLE = 11;
        static final int MSG_ADBD_SOCKET_CONNECTED = 26;
        static final int MSG_ADBD_SOCKET_DISCONNECTED = 27;
        static final int MSG_ADBWIFI_ALLOW = 18;
        static final int MSG_ADBWIFI_DENY = 19;
        static final String MSG_DISABLE_ADBDWIFI = "DA";
        static final String MSG_DISCONNECT_DEVICE = "DD";
        static final int MSG_PAIRING_CANCEL = 14;
        static final int MSG_PAIR_PAIRING_CODE = 15;
        static final int MSG_PAIR_QR_CODE = 16;
        static final int MSG_REQ_UNPAIR = 17;
        static final int MSG_RESPONSE_PAIRING_PORT = 21;
        static final int MSG_RESPONSE_PAIRING_RESULT = 20;
        static final int MSG_SERVER_CONNECTED = 24;
        static final int MSG_SERVER_DISCONNECTED = 25;
        static final int MSG_WIFI_DEVICE_CONNECTED = 22;
        static final int MSG_WIFI_DEVICE_DISCONNECTED = 23;
        static final long UPDATE_KEYSTORE_JOB_INTERVAL = 86400000;
        static final long UPDATE_KEYSTORE_MIN_JOB_INTERVAL = 60000;
        private int mAdbEnabledRefCount;
        private AdbKeyStore mAdbKeyStore;
        private boolean mAdbNotificationShown;
        private ContentObserver mAuthTimeObserver;
        private final BroadcastReceiver mBroadcastReceiver;
        private NotificationManager mNotificationManager;

        private boolean isTv() {
            return AdbDebuggingManager.this.mContext.getPackageManager().hasSystemFeature("android.software.leanback");
        }

        private void setupNotifications() {
            if (this.mNotificationManager == null) {
                NotificationManager notificationManager = (NotificationManager) AdbDebuggingManager.this.mContext.getSystemService("notification");
                this.mNotificationManager = notificationManager;
                if (notificationManager == null) {
                    Slog.e(AdbDebuggingManager.TAG, "Unable to setup notifications for wireless debugging");
                } else if (isTv()) {
                    this.mNotificationManager.createNotificationChannel(new NotificationChannel(ADB_NOTIFICATION_CHANNEL_ID_TV, AdbDebuggingManager.this.mContext.getString(17039618), 4));
                }
            }
        }

        AdbDebuggingHandler(Looper looper) {
            super(looper);
            this.mBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.adb.AdbDebuggingManager.AdbDebuggingHandler.1
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if ("android.net.wifi.WIFI_STATE_CHANGED".equals(action)) {
                        int state = intent.getIntExtra("wifi_state", 1);
                        if (state == 1) {
                            Slog.i(AdbDebuggingManager.TAG, "Wifi disabled. Disabling adbwifi.");
                            Settings.Global.putInt(AdbDebuggingManager.this.mContentResolver, "adb_wifi_enabled", 0);
                        }
                    } else if ("android.net.wifi.STATE_CHANGE".equals(action)) {
                        NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                        if (networkInfo.getType() == 1) {
                            if (networkInfo.isConnected()) {
                                WifiManager wifiManager = (WifiManager) AdbDebuggingManager.this.mContext.getSystemService("wifi");
                                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                                if (wifiInfo == null || wifiInfo.getNetworkId() == -1) {
                                    Slog.i(AdbDebuggingManager.TAG, "Not connected to any wireless network. Not enabling adbwifi.");
                                    Settings.Global.putInt(AdbDebuggingManager.this.mContentResolver, "adb_wifi_enabled", 0);
                                }
                                String bssid = wifiInfo.getBSSID();
                                if (bssid == null || bssid.isEmpty()) {
                                    Slog.e(AdbDebuggingManager.TAG, "Unable to get the wifi ap's BSSID. Disabling adbwifi.");
                                    Settings.Global.putInt(AdbDebuggingManager.this.mContentResolver, "adb_wifi_enabled", 0);
                                }
                                synchronized (AdbDebuggingManager.this.mAdbConnectionInfo) {
                                    if (!bssid.equals(AdbDebuggingManager.this.mAdbConnectionInfo.getBSSID())) {
                                        Slog.i(AdbDebuggingManager.TAG, "Detected wifi network change. Disabling adbwifi.");
                                        Settings.Global.putInt(AdbDebuggingManager.this.mContentResolver, "adb_wifi_enabled", 0);
                                    }
                                }
                                return;
                            }
                            Slog.i(AdbDebuggingManager.TAG, "Network disconnected. Disabling adbwifi.");
                            Settings.Global.putInt(AdbDebuggingManager.this.mContentResolver, "adb_wifi_enabled", 0);
                        }
                    }
                }
            };
            this.mAdbEnabledRefCount = 0;
            this.mAuthTimeObserver = new ContentObserver(this) { // from class: com.android.server.adb.AdbDebuggingManager.AdbDebuggingHandler.2
                @Override // android.database.ContentObserver
                public void onChange(boolean selfChange, Uri uri) {
                    Slog.d(AdbDebuggingManager.TAG, "Received notification that uri " + uri + " was modified; rescheduling keystore job");
                    AdbDebuggingHandler.this.scheduleJobToUpdateAdbKeyStore();
                }
            };
        }

        AdbDebuggingHandler(Looper looper, AdbDebuggingThread thread, AdbKeyStore adbKeyStore) {
            super(looper);
            this.mBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.adb.AdbDebuggingManager.AdbDebuggingHandler.1
                @Override // android.content.BroadcastReceiver
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if ("android.net.wifi.WIFI_STATE_CHANGED".equals(action)) {
                        int state = intent.getIntExtra("wifi_state", 1);
                        if (state == 1) {
                            Slog.i(AdbDebuggingManager.TAG, "Wifi disabled. Disabling adbwifi.");
                            Settings.Global.putInt(AdbDebuggingManager.this.mContentResolver, "adb_wifi_enabled", 0);
                        }
                    } else if ("android.net.wifi.STATE_CHANGE".equals(action)) {
                        NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                        if (networkInfo.getType() == 1) {
                            if (networkInfo.isConnected()) {
                                WifiManager wifiManager = (WifiManager) AdbDebuggingManager.this.mContext.getSystemService("wifi");
                                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                                if (wifiInfo == null || wifiInfo.getNetworkId() == -1) {
                                    Slog.i(AdbDebuggingManager.TAG, "Not connected to any wireless network. Not enabling adbwifi.");
                                    Settings.Global.putInt(AdbDebuggingManager.this.mContentResolver, "adb_wifi_enabled", 0);
                                }
                                String bssid = wifiInfo.getBSSID();
                                if (bssid == null || bssid.isEmpty()) {
                                    Slog.e(AdbDebuggingManager.TAG, "Unable to get the wifi ap's BSSID. Disabling adbwifi.");
                                    Settings.Global.putInt(AdbDebuggingManager.this.mContentResolver, "adb_wifi_enabled", 0);
                                }
                                synchronized (AdbDebuggingManager.this.mAdbConnectionInfo) {
                                    if (!bssid.equals(AdbDebuggingManager.this.mAdbConnectionInfo.getBSSID())) {
                                        Slog.i(AdbDebuggingManager.TAG, "Detected wifi network change. Disabling adbwifi.");
                                        Settings.Global.putInt(AdbDebuggingManager.this.mContentResolver, "adb_wifi_enabled", 0);
                                    }
                                }
                                return;
                            }
                            Slog.i(AdbDebuggingManager.TAG, "Network disconnected. Disabling adbwifi.");
                            Settings.Global.putInt(AdbDebuggingManager.this.mContentResolver, "adb_wifi_enabled", 0);
                        }
                    }
                }
            };
            this.mAdbEnabledRefCount = 0;
            this.mAuthTimeObserver = new ContentObserver(this) { // from class: com.android.server.adb.AdbDebuggingManager.AdbDebuggingHandler.2
                @Override // android.database.ContentObserver
                public void onChange(boolean selfChange, Uri uri) {
                    Slog.d(AdbDebuggingManager.TAG, "Received notification that uri " + uri + " was modified; rescheduling keystore job");
                    AdbDebuggingHandler.this.scheduleJobToUpdateAdbKeyStore();
                }
            };
            AdbDebuggingManager.this.mThread = thread;
            this.mAdbKeyStore = adbKeyStore;
        }

        public void showAdbConnectedNotification(boolean show) {
            if (show == this.mAdbNotificationShown) {
                return;
            }
            setupNotifications();
            if (!this.mAdbNotificationShown) {
                Notification notification = AdbNotifications.createNotification(AdbDebuggingManager.this.mContext, (byte) 1);
                this.mAdbNotificationShown = true;
                this.mNotificationManager.notifyAsUser(null, 62, notification, UserHandle.ALL);
                return;
            }
            this.mAdbNotificationShown = false;
            this.mNotificationManager.cancelAsUser(null, 62, UserHandle.ALL);
        }

        private void startAdbDebuggingThread() {
            int i = this.mAdbEnabledRefCount + 1;
            this.mAdbEnabledRefCount = i;
            if (i > 1) {
                return;
            }
            registerForAuthTimeChanges();
            AdbDebuggingManager.this.mThread = new AdbDebuggingThread();
            AdbDebuggingManager.this.mThread.start();
            this.mAdbKeyStore.updateKeyStore();
            scheduleJobToUpdateAdbKeyStore();
        }

        private void stopAdbDebuggingThread() {
            int i = this.mAdbEnabledRefCount - 1;
            this.mAdbEnabledRefCount = i;
            if (i <= 0) {
                if (AdbDebuggingManager.this.mThread != null) {
                    AdbDebuggingManager.this.mThread.stopListening();
                    AdbDebuggingManager.this.mThread = null;
                }
                if (!AdbDebuggingManager.this.mConnectedKeys.isEmpty()) {
                    for (Map.Entry<String, Integer> entry : AdbDebuggingManager.this.mConnectedKeys.entrySet()) {
                        this.mAdbKeyStore.setLastConnectionTime(entry.getKey(), System.currentTimeMillis());
                    }
                    AdbDebuggingManager.this.sendPersistKeyStoreMessage();
                    AdbDebuggingManager.this.mConnectedKeys.clear();
                    AdbDebuggingManager.this.mWifiConnectedKeys.clear();
                }
                scheduleJobToUpdateAdbKeyStore();
            }
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (this.mAdbKeyStore == null) {
                this.mAdbKeyStore = new AdbKeyStore();
            }
            switch (msg.what) {
                case 1:
                    if (!AdbDebuggingManager.this.mAdbUsbEnabled) {
                        startAdbDebuggingThread();
                        AdbDebuggingManager.this.mAdbUsbEnabled = true;
                        return;
                    }
                    return;
                case 2:
                    if (AdbDebuggingManager.this.mAdbUsbEnabled) {
                        stopAdbDebuggingThread();
                        AdbDebuggingManager.this.mAdbUsbEnabled = false;
                        return;
                    }
                    return;
                case 3:
                    String key = (String) msg.obj;
                    String fingerprints = AdbDebuggingManager.this.getFingerprints(key);
                    if (!fingerprints.equals(AdbDebuggingManager.this.mFingerprints)) {
                        Slog.e(AdbDebuggingManager.TAG, "Fingerprints do not match. Got " + fingerprints + ", expected " + AdbDebuggingManager.this.mFingerprints);
                        return;
                    }
                    boolean alwaysAllow = msg.arg1 == 1;
                    if (AdbDebuggingManager.this.mThread != null) {
                        AdbDebuggingManager.this.mThread.sendResponse("OK");
                        if (alwaysAllow) {
                            if (!AdbDebuggingManager.this.mConnectedKeys.containsKey(key)) {
                                AdbDebuggingManager.this.mConnectedKeys.put(key, 1);
                            }
                            this.mAdbKeyStore.setLastConnectionTime(key, System.currentTimeMillis());
                            AdbDebuggingManager.this.sendPersistKeyStoreMessage();
                            scheduleJobToUpdateAdbKeyStore();
                        }
                        logAdbConnectionChanged(key, 2, alwaysAllow);
                        return;
                    }
                    return;
                case 4:
                    if (AdbDebuggingManager.this.mThread != null) {
                        Slog.w(AdbDebuggingManager.TAG, "Denying adb confirmation");
                        AdbDebuggingManager.this.mThread.sendResponse("NO");
                        logAdbConnectionChanged(null, 3, false);
                        return;
                    }
                    return;
                case 5:
                    String key2 = (String) msg.obj;
                    if (!"trigger_restart_min_framework".equals(SystemProperties.get("vold.decrypt"))) {
                        String fingerprints2 = AdbDebuggingManager.this.getFingerprints(key2);
                        if ("".equals(fingerprints2)) {
                            if (AdbDebuggingManager.this.mThread != null) {
                                AdbDebuggingManager.this.mThread.sendResponse("NO");
                                logAdbConnectionChanged(key2, 5, false);
                                return;
                            }
                            return;
                        }
                        logAdbConnectionChanged(key2, 1, false);
                        AdbDebuggingManager.this.mFingerprints = fingerprints2;
                        AdbDebuggingManager adbDebuggingManager = AdbDebuggingManager.this;
                        adbDebuggingManager.startConfirmationForKey(key2, adbDebuggingManager.mFingerprints);
                        return;
                    }
                    Slog.w(AdbDebuggingManager.TAG, "Deferring adb confirmation until after vold decrypt");
                    if (AdbDebuggingManager.this.mThread != null) {
                        AdbDebuggingManager.this.mThread.sendResponse("NO");
                        logAdbConnectionChanged(key2, 6, false);
                        return;
                    }
                    return;
                case 6:
                    Slog.d(AdbDebuggingManager.TAG, "Received a request to clear the adb authorizations");
                    AdbDebuggingManager.this.mConnectedKeys.clear();
                    if (this.mAdbKeyStore == null) {
                        this.mAdbKeyStore = new AdbKeyStore();
                    }
                    AdbDebuggingManager.this.mWifiConnectedKeys.clear();
                    this.mAdbKeyStore.deleteKeyStore();
                    cancelJobToUpdateAdbKeyStore();
                    return;
                case 7:
                    String key3 = (String) msg.obj;
                    boolean alwaysAllow2 = false;
                    if (key3 != null && key3.length() > 0) {
                        if (AdbDebuggingManager.this.mConnectedKeys.containsKey(key3)) {
                            alwaysAllow2 = true;
                            int refcount = ((Integer) AdbDebuggingManager.this.mConnectedKeys.get(key3)).intValue() - 1;
                            if (refcount != 0) {
                                AdbDebuggingManager.this.mConnectedKeys.put(key3, Integer.valueOf(refcount));
                            } else {
                                this.mAdbKeyStore.setLastConnectionTime(key3, System.currentTimeMillis());
                                AdbDebuggingManager.this.sendPersistKeyStoreMessage();
                                scheduleJobToUpdateAdbKeyStore();
                                AdbDebuggingManager.this.mConnectedKeys.remove(key3);
                            }
                        }
                    } else {
                        Slog.w(AdbDebuggingManager.TAG, "Received a disconnected key message with an empty key");
                    }
                    logAdbConnectionChanged(key3, 7, alwaysAllow2);
                    return;
                case 8:
                    AdbKeyStore adbKeyStore = this.mAdbKeyStore;
                    if (adbKeyStore != null) {
                        adbKeyStore.persistKeyStore();
                        return;
                    }
                    return;
                case 9:
                    if (!AdbDebuggingManager.this.mConnectedKeys.isEmpty()) {
                        for (Map.Entry<String, Integer> entry : AdbDebuggingManager.this.mConnectedKeys.entrySet()) {
                            this.mAdbKeyStore.setLastConnectionTime(entry.getKey(), System.currentTimeMillis());
                        }
                        AdbDebuggingManager.this.sendPersistKeyStoreMessage();
                        scheduleJobToUpdateAdbKeyStore();
                        return;
                    } else if (!this.mAdbKeyStore.isEmpty()) {
                        this.mAdbKeyStore.updateKeyStore();
                        scheduleJobToUpdateAdbKeyStore();
                        return;
                    } else {
                        return;
                    }
                case 10:
                    String key4 = (String) msg.obj;
                    if (key4 != null && key4.length() != 0) {
                        if (!AdbDebuggingManager.this.mConnectedKeys.containsKey(key4)) {
                            AdbDebuggingManager.this.mConnectedKeys.put(key4, 1);
                        } else {
                            AdbDebuggingManager.this.mConnectedKeys.put(key4, Integer.valueOf(((Integer) AdbDebuggingManager.this.mConnectedKeys.get(key4)).intValue() + 1));
                        }
                        this.mAdbKeyStore.setLastConnectionTime(key4, System.currentTimeMillis());
                        AdbDebuggingManager.this.sendPersistKeyStoreMessage();
                        scheduleJobToUpdateAdbKeyStore();
                        logAdbConnectionChanged(key4, 4, true);
                        return;
                    }
                    Slog.w(AdbDebuggingManager.TAG, "Received a connected key message with an empty key");
                    return;
                case 11:
                    if (!AdbDebuggingManager.this.mAdbWifiEnabled) {
                        AdbConnectionInfo currentInfo = getCurrentWifiApInfo();
                        if (currentInfo == null) {
                            Settings.Global.putInt(AdbDebuggingManager.this.mContentResolver, "adb_wifi_enabled", 0);
                            return;
                        } else if (!verifyWifiNetwork(currentInfo.getBSSID(), currentInfo.getSSID())) {
                            Settings.Global.putInt(AdbDebuggingManager.this.mContentResolver, "adb_wifi_enabled", 0);
                            return;
                        } else {
                            AdbDebuggingManager.this.setAdbConnectionInfo(currentInfo);
                            IntentFilter intentFilter = new IntentFilter("android.net.wifi.WIFI_STATE_CHANGED");
                            intentFilter.addAction("android.net.wifi.STATE_CHANGE");
                            AdbDebuggingManager.this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter);
                            SystemProperties.set(AdbDebuggingManager.WIFI_PERSISTENT_CONFIG_PROPERTY, SnoozeHelper.XML_SNOOZED_NOTIFICATION_VERSION);
                            AdbDebuggingManager.this.mConnectionPortPoller = new AdbConnectionPortPoller(AdbDebuggingManager.this.mPortListener);
                            AdbDebuggingManager.this.mConnectionPortPoller.start();
                            startAdbDebuggingThread();
                            AdbDebuggingManager.this.mAdbWifiEnabled = true;
                            return;
                        }
                    }
                    return;
                case 12:
                    if (AdbDebuggingManager.this.mAdbWifiEnabled) {
                        AdbDebuggingManager.this.mAdbWifiEnabled = false;
                        AdbDebuggingManager.this.setAdbConnectionInfo(null);
                        AdbDebuggingManager.this.mContext.unregisterReceiver(this.mBroadcastReceiver);
                        if (AdbDebuggingManager.this.mThread != null) {
                            AdbDebuggingManager.this.mThread.sendResponse(MSG_DISABLE_ADBDWIFI);
                        }
                        onAdbdWifiServerDisconnected(-1);
                        stopAdbDebuggingThread();
                        return;
                    }
                    return;
                case 13:
                default:
                    return;
                case 14:
                    if (AdbDebuggingManager.this.mPairingThread != null) {
                        AdbDebuggingManager.this.mPairingThread.cancelPairing();
                        try {
                            AdbDebuggingManager.this.mPairingThread.join();
                        } catch (InterruptedException e) {
                            Slog.w(AdbDebuggingManager.TAG, "Error while waiting for pairing thread to quit.");
                            e.printStackTrace();
                        }
                        AdbDebuggingManager.this.mPairingThread = null;
                        return;
                    }
                    return;
                case 15:
                    String pairingCode = createPairingCode(6);
                    updateUIPairCode(pairingCode);
                    AdbDebuggingManager.this.mPairingThread = new PairingThread(pairingCode, null);
                    AdbDebuggingManager.this.mPairingThread.start();
                    return;
                case 16:
                    Bundle bundle = (Bundle) msg.obj;
                    String serviceName = bundle.getString("serviceName");
                    String password = bundle.getString("password");
                    AdbDebuggingManager.this.mPairingThread = new PairingThread(password, serviceName);
                    AdbDebuggingManager.this.mPairingThread.start();
                    return;
                case 17:
                    String fingerprint = (String) msg.obj;
                    String publicKey = this.mAdbKeyStore.findKeyFromFingerprint(fingerprint);
                    if (publicKey == null || publicKey.isEmpty()) {
                        Slog.e(AdbDebuggingManager.TAG, "Not a known fingerprint [" + fingerprint + "]");
                        return;
                    }
                    String cmdStr = MSG_DISCONNECT_DEVICE + publicKey;
                    if (AdbDebuggingManager.this.mThread != null) {
                        AdbDebuggingManager.this.mThread.sendResponse(cmdStr);
                    }
                    this.mAdbKeyStore.removeKey(publicKey);
                    sendPairedDevicesToUI(this.mAdbKeyStore.getPairedDevices());
                    return;
                case 18:
                    if (!AdbDebuggingManager.this.mAdbWifiEnabled) {
                        String bssid = (String) msg.obj;
                        if (msg.arg1 == 1) {
                            this.mAdbKeyStore.addTrustedNetwork(bssid);
                        }
                        AdbConnectionInfo newInfo = getCurrentWifiApInfo();
                        if (newInfo != null && bssid.equals(newInfo.getBSSID())) {
                            AdbDebuggingManager.this.setAdbConnectionInfo(newInfo);
                            Settings.Global.putInt(AdbDebuggingManager.this.mContentResolver, "adb_wifi_enabled", 1);
                            IntentFilter intentFilter2 = new IntentFilter("android.net.wifi.WIFI_STATE_CHANGED");
                            intentFilter2.addAction("android.net.wifi.STATE_CHANGE");
                            AdbDebuggingManager.this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter2);
                            SystemProperties.set(AdbDebuggingManager.WIFI_PERSISTENT_CONFIG_PROPERTY, SnoozeHelper.XML_SNOOZED_NOTIFICATION_VERSION);
                            AdbDebuggingManager.this.mConnectionPortPoller = new AdbConnectionPortPoller(AdbDebuggingManager.this.mPortListener);
                            AdbDebuggingManager.this.mConnectionPortPoller.start();
                            startAdbDebuggingThread();
                            AdbDebuggingManager.this.mAdbWifiEnabled = true;
                            return;
                        }
                        return;
                    }
                    return;
                case 19:
                    Settings.Global.putInt(AdbDebuggingManager.this.mContentResolver, "adb_wifi_enabled", 0);
                    sendServerConnectionState(false, -1);
                    return;
                case 20:
                    onPairingResult(((Bundle) msg.obj).getString("publicKey"));
                    sendPairedDevicesToUI(this.mAdbKeyStore.getPairedDevices());
                    return;
                case 21:
                    sendPairingPortToUI(((Integer) msg.obj).intValue());
                    return;
                case 22:
                    if (AdbDebuggingManager.this.mWifiConnectedKeys.add((String) msg.obj)) {
                        sendPairedDevicesToUI(this.mAdbKeyStore.getPairedDevices());
                        showAdbConnectedNotification(true);
                        return;
                    }
                    return;
                case 23:
                    if (AdbDebuggingManager.this.mWifiConnectedKeys.remove((String) msg.obj)) {
                        sendPairedDevicesToUI(this.mAdbKeyStore.getPairedDevices());
                        if (AdbDebuggingManager.this.mWifiConnectedKeys.isEmpty()) {
                            showAdbConnectedNotification(false);
                            return;
                        }
                        return;
                    }
                    return;
                case 24:
                    int port = ((Integer) msg.obj).intValue();
                    onAdbdWifiServerConnected(port);
                    synchronized (AdbDebuggingManager.this.mAdbConnectionInfo) {
                        AdbDebuggingManager.this.mAdbConnectionInfo.setPort(port);
                    }
                    Settings.Global.putInt(AdbDebuggingManager.this.mContentResolver, "adb_wifi_enabled", 1);
                    return;
                case 25:
                    if (AdbDebuggingManager.this.mAdbWifiEnabled) {
                        onAdbdWifiServerDisconnected(((Integer) msg.obj).intValue());
                        Settings.Global.putInt(AdbDebuggingManager.this.mContentResolver, "adb_wifi_enabled", 0);
                        stopAdbDebuggingThread();
                        if (AdbDebuggingManager.this.mConnectionPortPoller != null) {
                            AdbDebuggingManager.this.mConnectionPortPoller.cancelAndWait();
                            AdbDebuggingManager.this.mConnectionPortPoller = null;
                            return;
                        }
                        return;
                    }
                    return;
                case 26:
                    if (AdbDebuggingManager.this.mAdbWifiEnabled) {
                        AdbDebuggingManager.this.mConnectionPortPoller = new AdbConnectionPortPoller(AdbDebuggingManager.this.mPortListener);
                        AdbDebuggingManager.this.mConnectionPortPoller.start();
                        return;
                    }
                    return;
                case 27:
                    if (AdbDebuggingManager.this.mConnectionPortPoller != null) {
                        AdbDebuggingManager.this.mConnectionPortPoller.cancelAndWait();
                        AdbDebuggingManager.this.mConnectionPortPoller = null;
                    }
                    if (AdbDebuggingManager.this.mAdbWifiEnabled) {
                        onAdbdWifiServerDisconnected(-1);
                        return;
                    }
                    return;
            }
        }

        void registerForAuthTimeChanges() {
            Uri uri = Settings.Global.getUriFor("adb_allowed_connection_time");
            AdbDebuggingManager.this.mContext.getContentResolver().registerContentObserver(uri, false, this.mAuthTimeObserver);
        }

        private void logAdbConnectionChanged(String key, int state, boolean alwaysAllow) {
            long lastConnectionTime = this.mAdbKeyStore.getLastConnectionTime(key);
            long authWindow = this.mAdbKeyStore.getAllowedConnectionTime();
            Slog.d(AdbDebuggingManager.TAG, "Logging key " + key + ", state = " + state + ", alwaysAllow = " + alwaysAllow + ", lastConnectionTime = " + lastConnectionTime + ", authWindow = " + authWindow);
            FrameworkStatsLog.write(144, lastConnectionTime, authWindow, state, alwaysAllow);
        }

        long scheduleJobToUpdateAdbKeyStore() {
            long delay;
            cancelJobToUpdateAdbKeyStore();
            long keyExpiration = this.mAdbKeyStore.getNextExpirationTime();
            if (keyExpiration == -1) {
                return -1L;
            }
            if (keyExpiration == 0) {
                delay = 0;
            } else {
                delay = Math.max(Math.min(86400000L, keyExpiration), 60000L);
            }
            Message message = obtainMessage(9);
            sendMessageDelayed(message, delay);
            return delay;
        }

        private void cancelJobToUpdateAdbKeyStore() {
            removeMessages(9);
        }

        private String createPairingCode(int size) {
            String res = "";
            SecureRandom rand = new SecureRandom();
            for (int i = 0; i < size; i++) {
                res = res + rand.nextInt(10);
            }
            return res;
        }

        private void sendServerConnectionState(boolean connected, int port) {
            int i;
            Intent intent = new Intent("com.android.server.adb.WIRELESS_DEBUG_STATUS");
            if (connected) {
                i = 4;
            } else {
                i = 5;
            }
            intent.putExtra(IVivoLocationManagerService.INDEX_LISTEN_GNSS_STATUS, i);
            intent.putExtra("adb_port", port);
            AdbDebuggingManager.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }

        private void onAdbdWifiServerConnected(int port) {
            sendPairedDevicesToUI(this.mAdbKeyStore.getPairedDevices());
            sendServerConnectionState(true, port);
        }

        private void onAdbdWifiServerDisconnected(int port) {
            AdbDebuggingManager.this.mWifiConnectedKeys.clear();
            showAdbConnectedNotification(false);
            sendServerConnectionState(false, port);
        }

        private AdbConnectionInfo getCurrentWifiApInfo() {
            String ssid;
            WifiManager wifiManager = (WifiManager) AdbDebuggingManager.this.mContext.getSystemService("wifi");
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo == null || wifiInfo.getNetworkId() == -1) {
                Slog.i(AdbDebuggingManager.TAG, "Not connected to any wireless network. Not enabling adbwifi.");
                return null;
            }
            if (wifiInfo.isPasspointAp() || wifiInfo.isOsuAp()) {
                ssid = wifiInfo.getPasspointProviderFriendlyName();
            } else {
                ssid = wifiInfo.getSSID();
                if (ssid == null || "<unknown ssid>".equals(ssid)) {
                    List<WifiConfiguration> networks = wifiManager.getConfiguredNetworks();
                    int length = networks.size();
                    for (int i = 0; i < length; i++) {
                        if (networks.get(i).networkId == wifiInfo.getNetworkId()) {
                            ssid = networks.get(i).SSID;
                        }
                    }
                    if (ssid == null) {
                        Slog.e(AdbDebuggingManager.TAG, "Unable to get ssid of the wifi AP.");
                        return null;
                    }
                }
            }
            String bssid = wifiInfo.getBSSID();
            if (bssid == null || bssid.isEmpty()) {
                Slog.e(AdbDebuggingManager.TAG, "Unable to get the wifi ap's BSSID.");
                return null;
            }
            return new AdbConnectionInfo(bssid, ssid);
        }

        private boolean verifyWifiNetwork(String bssid, String ssid) {
            if (!this.mAdbKeyStore.isTrustedNetwork(bssid)) {
                AdbDebuggingManager.this.startConfirmationForNetwork(ssid, bssid);
                return false;
            }
            return true;
        }

        private void onPairingResult(String publicKey) {
            if (publicKey == null) {
                Intent intent = new Intent("com.android.server.adb.WIRELESS_DEBUG_PAIRING_RESULT");
                intent.putExtra(IVivoLocationManagerService.INDEX_LISTEN_GNSS_STATUS, 0);
                AdbDebuggingManager.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
                return;
            }
            Intent intent2 = new Intent("com.android.server.adb.WIRELESS_DEBUG_PAIRING_RESULT");
            intent2.putExtra(IVivoLocationManagerService.INDEX_LISTEN_GNSS_STATUS, 1);
            String fingerprints = AdbDebuggingManager.this.getFingerprints(publicKey);
            String hostname = "nouser@nohostname";
            String[] args = publicKey.split("\\s+");
            if (args.length > 1) {
                hostname = args[1];
            }
            PairDevice device = new PairDevice(fingerprints, hostname, false);
            intent2.putExtra("pair_device", (Parcelable) device);
            AdbDebuggingManager.this.mContext.sendBroadcastAsUser(intent2, UserHandle.ALL);
            this.mAdbKeyStore.setLastConnectionTime(publicKey, System.currentTimeMillis());
            AdbDebuggingManager.this.sendPersistKeyStoreMessage();
            scheduleJobToUpdateAdbKeyStore();
        }

        private void sendPairingPortToUI(int port) {
            Intent intent = new Intent("com.android.server.adb.WIRELESS_DEBUG_PAIRING_RESULT");
            intent.putExtra(IVivoLocationManagerService.INDEX_LISTEN_GNSS_STATUS, 4);
            intent.putExtra("adb_port", port);
            AdbDebuggingManager.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }

        private void sendPairedDevicesToUI(Map<String, PairDevice> devices) {
            Intent intent = new Intent("com.android.server.adb.WIRELESS_DEBUG_PAIRED_DEVICES");
            intent.putExtra("devices_map", (HashMap) devices);
            AdbDebuggingManager.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }

        private void updateUIPairCode(String code) {
            Intent intent = new Intent("com.android.server.adb.WIRELESS_DEBUG_PAIRING_RESULT");
            intent.putExtra("pairing_code", code);
            intent.putExtra(IVivoLocationManagerService.INDEX_LISTEN_GNSS_STATUS, 3);
            AdbDebuggingManager.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getFingerprints(String key) {
        StringBuilder sb = new StringBuilder();
        if (key == null) {
            return "";
        }
        try {
            MessageDigest digester = MessageDigest.getInstance("MD5");
            byte[] base64_data = key.split("\\s+")[0].getBytes();
            try {
                byte[] digest = digester.digest(Base64.decode(base64_data, 0));
                for (int i = 0; i < digest.length; i++) {
                    sb.append("0123456789ABCDEF".charAt((digest[i] >> 4) & 15));
                    sb.append("0123456789ABCDEF".charAt(digest[i] & UsbDescriptor.DESCRIPTORTYPE_BOS));
                    if (i < digest.length - 1) {
                        sb.append(":");
                    }
                }
                return sb.toString();
            } catch (IllegalArgumentException e) {
                Slog.e(TAG, "error doing base64 decoding", e);
                return "";
            }
        } catch (Exception ex) {
            Slog.e(TAG, "Error getting digester", ex);
            return "";
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startConfirmationForNetwork(String ssid, String bssid) {
        String componentString;
        List<Map.Entry<String, String>> extras = new ArrayList<>();
        extras.add(new AbstractMap.SimpleEntry<>("ssid", ssid));
        extras.add(new AbstractMap.SimpleEntry<>("bssid", bssid));
        int currentUserId = ActivityManager.getCurrentUser();
        UserInfo userInfo = UserManager.get(this.mContext).getUserInfo(currentUserId);
        if (userInfo.isAdmin() || currentUserId == FIX_MODE_USER_ID) {
            componentString = Resources.getSystem().getString(17039865);
        } else {
            componentString = Resources.getSystem().getString(17039865);
        }
        ComponentName componentName = ComponentName.unflattenFromString(componentString);
        if (startConfirmationActivity(componentName, userInfo.getUserHandle(), extras) || startConfirmationService(componentName, userInfo.getUserHandle(), extras)) {
            return;
        }
        Slog.e(TAG, "Unable to start customAdbWifiNetworkConfirmation[SecondaryUser]Component " + componentString + " as an Activity or a Service");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startConfirmationForKey(String key, String fingerprints) {
        String componentString;
        List<Map.Entry<String, String>> extras = new ArrayList<>();
        extras.add(new AbstractMap.SimpleEntry<>("key", key));
        extras.add(new AbstractMap.SimpleEntry<>("fingerprints", fingerprints));
        int currentUserId = ActivityManager.getCurrentUser();
        UserInfo userInfo = UserManager.get(this.mContext).getUserInfo(currentUserId);
        if (userInfo.isAdmin() || currentUserId == FIX_MODE_USER_ID) {
            componentString = this.mConfirmComponent;
            if (componentString == null) {
                componentString = Resources.getSystem().getString(17039863);
            }
        } else {
            componentString = Resources.getSystem().getString(17039864);
        }
        ComponentName componentName = ComponentName.unflattenFromString(componentString);
        if (startConfirmationActivity(componentName, userInfo.getUserHandle(), extras) || startConfirmationService(componentName, userInfo.getUserHandle(), extras)) {
            return;
        }
        Slog.e(TAG, "unable to start customAdbPublicKeyConfirmation[SecondaryUser]Component " + componentString + " as an Activity or a Service");
    }

    private boolean startConfirmationActivity(ComponentName componentName, UserHandle userHandle, List<Map.Entry<String, String>> extras) {
        PackageManager packageManager = this.mContext.getPackageManager();
        Intent intent = createConfirmationIntent(componentName, extras);
        intent.addFlags(AudioFormat.EVRC);
        if (packageManager.resolveActivity(intent, 65536) != null) {
            try {
                this.mContext.startActivityAsUser(intent, userHandle);
                return true;
            } catch (ActivityNotFoundException e) {
                Slog.e(TAG, "unable to start adb whitelist activity: " + componentName, e);
                return false;
            }
        }
        return false;
    }

    private boolean startConfirmationService(ComponentName componentName, UserHandle userHandle, List<Map.Entry<String, String>> extras) {
        Intent intent = createConfirmationIntent(componentName, extras);
        try {
            if (this.mContext.startServiceAsUser(intent, userHandle) != null) {
                return true;
            }
            return false;
        } catch (SecurityException e) {
            Slog.e(TAG, "unable to start adb whitelist service: " + componentName, e);
            return false;
        }
    }

    private Intent createConfirmationIntent(ComponentName componentName, List<Map.Entry<String, String>> extras) {
        Intent intent = new Intent();
        intent.setClassName(componentName.getPackageName(), componentName.getClassName());
        for (Map.Entry<String, String> entry : extras) {
            intent.putExtra(entry.getKey(), entry.getValue());
        }
        return intent;
    }

    private File getAdbFile(String fileName) {
        File dataDir = Environment.getDataDirectory();
        File adbDir = new File(dataDir, ADB_DIRECTORY);
        if (!adbDir.exists()) {
            Slog.e(TAG, "ADB data directory does not exist");
            return null;
        }
        return new File(adbDir, fileName);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public File getAdbTempKeysFile() {
        return getAdbFile(ADB_TEMP_KEYS_FILE);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public File getUserKeyFile() {
        File file = this.mTestUserKeyFile;
        return file == null ? getAdbFile(ADB_KEYS_FILE) : file;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void writeKey(String key) {
        try {
            File keyFile = getUserKeyFile();
            if (keyFile == null) {
                return;
            }
            FileOutputStream fo = new FileOutputStream(keyFile, true);
            fo.write(key.getBytes());
            fo.write(10);
            fo.close();
            FileUtils.setPermissions(keyFile.toString(), 416, -1, -1);
        } catch (IOException ex) {
            Slog.e(TAG, "Error writing key:" + ex);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void writeKeys(Iterable<String> keys) {
        AtomicFile atomicKeyFile = null;
        FileOutputStream fo = null;
        try {
            File keyFile = getUserKeyFile();
            if (keyFile == null) {
                return;
            }
            atomicKeyFile = new AtomicFile(keyFile);
            fo = atomicKeyFile.startWrite();
            for (String key : keys) {
                fo.write(key.getBytes());
                fo.write(10);
            }
            atomicKeyFile.finishWrite(fo);
            FileUtils.setPermissions(keyFile.toString(), 416, -1, -1);
        } catch (IOException ex) {
            Slog.e(TAG, "Error writing keys: " + ex);
            if (atomicKeyFile != null) {
                atomicKeyFile.failWrite(fo);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void deleteKeyFile() {
        File keyFile = getUserKeyFile();
        if (keyFile != null) {
            keyFile.delete();
        }
    }

    public void setAdbEnabled(boolean enabled, byte transportType) {
        int i = 1;
        if (transportType == 0) {
            Handler handler = this.mHandler;
            if (!enabled) {
                i = 2;
            }
            handler.sendEmptyMessage(i);
        } else if (transportType == 1) {
            this.mHandler.sendEmptyMessage(enabled ? 11 : 12);
        } else {
            throw new IllegalArgumentException("setAdbEnabled called with unimplemented transport type=" + ((int) transportType));
        }
    }

    public void allowDebugging(boolean alwaysAllow, String publicKey) {
        Message msg = this.mHandler.obtainMessage(3);
        msg.arg1 = alwaysAllow ? 1 : 0;
        msg.obj = publicKey;
        this.mHandler.sendMessage(msg);
    }

    public void denyDebugging() {
        this.mHandler.sendEmptyMessage(4);
    }

    public void clearDebuggingKeys() {
        this.mHandler.sendEmptyMessage(6);
    }

    public void allowWirelessDebugging(boolean alwaysAllow, String bssid) {
        Message msg = this.mHandler.obtainMessage(18);
        msg.arg1 = alwaysAllow ? 1 : 0;
        msg.obj = bssid;
        this.mHandler.sendMessage(msg);
    }

    public void denyWirelessDebugging() {
        this.mHandler.sendEmptyMessage(19);
    }

    public int getAdbWirelessPort() {
        AdbConnectionInfo info = getAdbConnectionInfo();
        if (info == null) {
            return 0;
        }
        return info.getPort();
    }

    public Map<String, PairDevice> getPairedDevices() {
        AdbKeyStore keystore = new AdbKeyStore();
        return keystore.getPairedDevices();
    }

    public void unpairDevice(String fingerprint) {
        Message message = Message.obtain(this.mHandler, 17, fingerprint);
        this.mHandler.sendMessage(message);
    }

    public void enablePairingByPairingCode() {
        this.mHandler.sendEmptyMessage(15);
    }

    public void enablePairingByQrCode(String serviceName, String password) {
        Bundle bundle = new Bundle();
        bundle.putString("serviceName", serviceName);
        bundle.putString("password", password);
        Message message = Message.obtain(this.mHandler, 16, bundle);
        this.mHandler.sendMessage(message);
    }

    public void disablePairing() {
        this.mHandler.sendEmptyMessage(14);
    }

    public boolean isAdbWifiEnabled() {
        return this.mAdbWifiEnabled;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendPersistKeyStoreMessage() {
        Message msg = this.mHandler.obtainMessage(8);
        this.mHandler.sendMessage(msg);
    }

    public void dump(DualDumpOutputStream dump, String idName, long id) {
        long token = dump.start(idName, id);
        dump.write("connected_to_adb", 1133871366145L, this.mThread != null);
        DumpUtils.writeStringIfNotNull(dump, "last_key_received", 1138166333442L, this.mFingerprints);
        try {
            dump.write("user_keys", 1138166333443L, FileUtils.readTextFile(new File("/data/misc/adb/adb_keys"), 0, null));
        } catch (IOException e) {
            Slog.e(TAG, "Cannot read user keys", e);
        }
        try {
            dump.write("system_keys", 1138166333444L, FileUtils.readTextFile(new File("/adb_keys"), 0, null));
        } catch (IOException e2) {
            Slog.e(TAG, "Cannot read system keys", e2);
        }
        try {
            dump.write("keystore", 1138166333445L, FileUtils.readTextFile(getAdbTempKeysFile(), 0, null));
        } catch (IOException e3) {
            Slog.e(TAG, "Cannot read keystore: ", e3);
        }
        dump.end(token);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class AdbKeyStore {
        private static final int KEYSTORE_VERSION = 1;
        private static final int MAX_SUPPORTED_KEYSTORE_VERSION = 1;
        public static final long NO_PREVIOUS_CONNECTION = 0;
        private static final String SYSTEM_KEY_FILE = "/adb_keys";
        private static final String XML_ATTRIBUTE_KEY = "key";
        private static final String XML_ATTRIBUTE_LAST_CONNECTION = "lastConnection";
        private static final String XML_ATTRIBUTE_VERSION = "version";
        private static final String XML_ATTRIBUTE_WIFI_BSSID = "bssid";
        private static final String XML_KEYSTORE_START_TAG = "keyStore";
        private static final String XML_TAG_ADB_KEY = "adbKey";
        private static final String XML_TAG_WIFI_ACCESS_POINT = "wifiAP";
        private AtomicFile mAtomicKeyFile;
        private File mKeyFile;
        private Map<String, Long> mKeyMap;
        private Set<String> mSystemKeys;
        private List<String> mTrustedNetworks;

        AdbKeyStore() {
            init();
        }

        AdbKeyStore(File keyFile) {
            this.mKeyFile = keyFile;
            init();
        }

        private void init() {
            initKeyFile();
            this.mKeyMap = getKeyMap();
            this.mTrustedNetworks = getTrustedNetworks();
            this.mSystemKeys = getSystemKeysFromFile(SYSTEM_KEY_FILE);
            addUserKeysToKeyStore();
        }

        public void addTrustedNetwork(String bssid) {
            this.mTrustedNetworks.add(bssid);
            AdbDebuggingManager.this.sendPersistKeyStoreMessage();
        }

        public Map<String, PairDevice> getPairedDevices() {
            Map<String, PairDevice> pairedDevices = new HashMap<>();
            for (Map.Entry<String, Long> keyEntry : this.mKeyMap.entrySet()) {
                String fingerprints = AdbDebuggingManager.this.getFingerprints(keyEntry.getKey());
                String hostname = "nouser@nohostname";
                String[] args = keyEntry.getKey().split("\\s+");
                if (args.length > 1) {
                    hostname = args[1];
                }
                pairedDevices.put(keyEntry.getKey(), new PairDevice(hostname, fingerprints, AdbDebuggingManager.this.mWifiConnectedKeys.contains(keyEntry.getKey())));
            }
            return pairedDevices;
        }

        public String findKeyFromFingerprint(String fingerprint) {
            for (Map.Entry<String, Long> entry : this.mKeyMap.entrySet()) {
                String f = AdbDebuggingManager.this.getFingerprints(entry.getKey());
                if (fingerprint.equals(f)) {
                    return entry.getKey();
                }
            }
            return null;
        }

        public void removeKey(String key) {
            if (this.mKeyMap.containsKey(key)) {
                this.mKeyMap.remove(key);
                AdbDebuggingManager.this.writeKeys(this.mKeyMap.keySet());
                AdbDebuggingManager.this.sendPersistKeyStoreMessage();
            }
        }

        private void initKeyFile() {
            if (this.mKeyFile == null) {
                this.mKeyFile = AdbDebuggingManager.this.getAdbTempKeysFile();
            }
            if (this.mKeyFile != null) {
                this.mAtomicKeyFile = new AtomicFile(this.mKeyFile);
            }
        }

        private Set<String> getSystemKeysFromFile(String fileName) {
            Set<String> systemKeys = new HashSet<>();
            File systemKeyFile = new File(fileName);
            if (systemKeyFile.exists()) {
                try {
                    BufferedReader in = new BufferedReader(new FileReader(systemKeyFile));
                    while (true) {
                        String key = in.readLine();
                        if (key == null) {
                            break;
                        }
                        String key2 = key.trim();
                        if (key2.length() > 0) {
                            systemKeys.add(key2);
                        }
                    }
                    in.close();
                } catch (IOException e) {
                    Slog.e(AdbDebuggingManager.TAG, "Caught an exception reading " + fileName + ": " + e);
                }
            }
            return systemKeys;
        }

        public boolean isEmpty() {
            return this.mKeyMap.isEmpty();
        }

        public void updateKeyStore() {
            if (filterOutOldKeys()) {
                AdbDebuggingManager.this.sendPersistKeyStoreMessage();
            }
        }

        private Map<String, Long> getKeyMap() {
            String tagName;
            Map<String, Long> keyMap = new HashMap<>();
            if (this.mAtomicKeyFile == null) {
                initKeyFile();
                if (this.mAtomicKeyFile == null) {
                    Slog.e(AdbDebuggingManager.TAG, "Unable to obtain the key file, " + this.mKeyFile + ", for reading");
                    return keyMap;
                }
            }
            if (this.mAtomicKeyFile.exists()) {
                try {
                    FileInputStream keyStream = this.mAtomicKeyFile.openRead();
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setInput(keyStream, StandardCharsets.UTF_8.name());
                    XmlUtils.beginDocument(parser, XML_KEYSTORE_START_TAG);
                    if (parser.next() != 1) {
                        String tagName2 = parser.getName();
                        if (tagName2 != null && XML_KEYSTORE_START_TAG.equals(tagName2)) {
                            int keystoreVersion = Integer.parseInt(parser.getAttributeValue(null, XML_ATTRIBUTE_VERSION));
                            if (keystoreVersion > 1) {
                                Slog.e(AdbDebuggingManager.TAG, "Keystore version=" + keystoreVersion + " not supported (max_supported=1)");
                                if (keyStream != null) {
                                    keyStream.close();
                                }
                                return keyMap;
                            }
                        }
                        Slog.e(AdbDebuggingManager.TAG, "Expected keyStore, but got tag=" + tagName2);
                        if (keyStream != null) {
                            keyStream.close();
                        }
                        return keyMap;
                    }
                    while (parser.next() != 1 && (tagName = parser.getName()) != null) {
                        if (tagName.equals(XML_TAG_ADB_KEY)) {
                            String key = parser.getAttributeValue(null, XML_ATTRIBUTE_KEY);
                            try {
                                long connectionTime = Long.valueOf(parser.getAttributeValue(null, XML_ATTRIBUTE_LAST_CONNECTION)).longValue();
                                keyMap.put(key, Long.valueOf(connectionTime));
                            } catch (NumberFormatException e) {
                                Slog.e(AdbDebuggingManager.TAG, "Caught a NumberFormatException parsing the last connection time: " + e);
                                XmlUtils.skipCurrentTag(parser);
                            }
                        } else {
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }
                    if (keyStream != null) {
                        keyStream.close();
                    }
                } catch (IOException e2) {
                    Slog.e(AdbDebuggingManager.TAG, "Caught an IOException parsing the XML key file: ", e2);
                } catch (XmlPullParserException e3) {
                    Slog.w(AdbDebuggingManager.TAG, "Caught XmlPullParserException parsing the XML key file: ", e3);
                    return getKeyMapBeforeKeystoreVersion();
                }
                return keyMap;
            }
            return keyMap;
        }

        private Map<String, Long> getKeyMapBeforeKeystoreVersion() {
            String tagName;
            Map<String, Long> keyMap = new HashMap<>();
            if (this.mAtomicKeyFile == null) {
                initKeyFile();
                if (this.mAtomicKeyFile == null) {
                    Slog.e(AdbDebuggingManager.TAG, "Unable to obtain the key file, " + this.mKeyFile + ", for reading");
                    return keyMap;
                }
            }
            if (!this.mAtomicKeyFile.exists()) {
                return keyMap;
            }
            try {
                FileInputStream keyStream = this.mAtomicKeyFile.openRead();
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(keyStream, StandardCharsets.UTF_8.name());
                XmlUtils.beginDocument(parser, XML_TAG_ADB_KEY);
                while (parser.next() != 1 && (tagName = parser.getName()) != null) {
                    if (!tagName.equals(XML_TAG_ADB_KEY)) {
                        XmlUtils.skipCurrentTag(parser);
                    } else {
                        String key = parser.getAttributeValue(null, XML_ATTRIBUTE_KEY);
                        try {
                            long connectionTime = Long.valueOf(parser.getAttributeValue(null, XML_ATTRIBUTE_LAST_CONNECTION)).longValue();
                            keyMap.put(key, Long.valueOf(connectionTime));
                        } catch (NumberFormatException e) {
                            Slog.e(AdbDebuggingManager.TAG, "Caught a NumberFormatException parsing the last connection time: " + e);
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }
                }
                if (keyStream != null) {
                    keyStream.close();
                }
            } catch (IOException | XmlPullParserException e2) {
                Slog.e(AdbDebuggingManager.TAG, "Caught an exception parsing the XML key file: ", e2);
            }
            return keyMap;
        }

        private List<String> getTrustedNetworks() {
            String tagName;
            List<String> trustedNetworks = new ArrayList<>();
            if (this.mAtomicKeyFile == null) {
                initKeyFile();
                if (this.mAtomicKeyFile == null) {
                    Slog.e(AdbDebuggingManager.TAG, "Unable to obtain the key file, " + this.mKeyFile + ", for reading");
                    return trustedNetworks;
                }
            }
            if (this.mAtomicKeyFile.exists()) {
                try {
                    FileInputStream keyStream = this.mAtomicKeyFile.openRead();
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setInput(keyStream, StandardCharsets.UTF_8.name());
                    XmlUtils.beginDocument(parser, XML_KEYSTORE_START_TAG);
                    if (parser.next() != 1) {
                        String tagName2 = parser.getName();
                        if (tagName2 != null && XML_KEYSTORE_START_TAG.equals(tagName2)) {
                            int keystoreVersion = Integer.parseInt(parser.getAttributeValue(null, XML_ATTRIBUTE_VERSION));
                            if (keystoreVersion > 1) {
                                Slog.e(AdbDebuggingManager.TAG, "Keystore version=" + keystoreVersion + " not supported (max_supported=1");
                                if (keyStream != null) {
                                    keyStream.close();
                                }
                                return trustedNetworks;
                            }
                        }
                        Slog.e(AdbDebuggingManager.TAG, "Expected keyStore, but got tag=" + tagName2);
                        if (keyStream != null) {
                            keyStream.close();
                        }
                        return trustedNetworks;
                    }
                    while (parser.next() != 1 && (tagName = parser.getName()) != null) {
                        if (tagName.equals(XML_TAG_WIFI_ACCESS_POINT)) {
                            String bssid = parser.getAttributeValue(null, XML_ATTRIBUTE_WIFI_BSSID);
                            trustedNetworks.add(bssid);
                        } else {
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }
                    if (keyStream != null) {
                        keyStream.close();
                    }
                } catch (IOException | NumberFormatException | XmlPullParserException e) {
                    Slog.e(AdbDebuggingManager.TAG, "Caught an exception parsing the XML key file: ", e);
                }
                return trustedNetworks;
            }
            return trustedNetworks;
        }

        private void addUserKeysToKeyStore() {
            File userKeyFile = AdbDebuggingManager.this.getUserKeyFile();
            boolean mapUpdated = false;
            if (userKeyFile != null && userKeyFile.exists()) {
                try {
                    BufferedReader in = new BufferedReader(new FileReader(userKeyFile));
                    long time = System.currentTimeMillis();
                    while (true) {
                        String key = in.readLine();
                        if (key == null) {
                            break;
                        } else if (!this.mKeyMap.containsKey(key)) {
                            this.mKeyMap.put(key, Long.valueOf(time));
                            mapUpdated = true;
                        }
                    }
                    in.close();
                } catch (IOException e) {
                    Slog.e(AdbDebuggingManager.TAG, "Caught an exception reading " + userKeyFile + ": " + e);
                }
            }
            if (mapUpdated) {
                AdbDebuggingManager.this.sendPersistKeyStoreMessage();
            }
        }

        public void persistKeyStore() {
            filterOutOldKeys();
            if (this.mKeyMap.isEmpty() && this.mTrustedNetworks.isEmpty()) {
                deleteKeyStore();
                return;
            }
            if (this.mAtomicKeyFile == null) {
                initKeyFile();
                if (this.mAtomicKeyFile == null) {
                    Slog.e(AdbDebuggingManager.TAG, "Unable to obtain the key file, " + this.mKeyFile + ", for writing");
                    return;
                }
            }
            FileOutputStream keyStream = null;
            try {
                FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                keyStream = this.mAtomicKeyFile.startWrite();
                fastXmlSerializer.setOutput(keyStream, StandardCharsets.UTF_8.name());
                fastXmlSerializer.startDocument(null, true);
                fastXmlSerializer.startTag(null, XML_KEYSTORE_START_TAG);
                fastXmlSerializer.attribute(null, XML_ATTRIBUTE_VERSION, String.valueOf(1));
                for (Map.Entry<String, Long> keyEntry : this.mKeyMap.entrySet()) {
                    fastXmlSerializer.startTag(null, XML_TAG_ADB_KEY);
                    fastXmlSerializer.attribute(null, XML_ATTRIBUTE_KEY, keyEntry.getKey());
                    fastXmlSerializer.attribute(null, XML_ATTRIBUTE_LAST_CONNECTION, String.valueOf(keyEntry.getValue()));
                    fastXmlSerializer.endTag(null, XML_TAG_ADB_KEY);
                }
                for (String bssid : this.mTrustedNetworks) {
                    fastXmlSerializer.startTag(null, XML_TAG_WIFI_ACCESS_POINT);
                    fastXmlSerializer.attribute(null, XML_ATTRIBUTE_WIFI_BSSID, bssid);
                    fastXmlSerializer.endTag(null, XML_TAG_WIFI_ACCESS_POINT);
                }
                fastXmlSerializer.endTag(null, XML_KEYSTORE_START_TAG);
                fastXmlSerializer.endDocument();
                this.mAtomicKeyFile.finishWrite(keyStream);
            } catch (IOException e) {
                Slog.e(AdbDebuggingManager.TAG, "Caught an exception writing the key map: ", e);
                this.mAtomicKeyFile.failWrite(keyStream);
            }
        }

        private boolean filterOutOldKeys() {
            boolean keysDeleted = false;
            long allowedTime = getAllowedConnectionTime();
            long systemTime = System.currentTimeMillis();
            Iterator<Map.Entry<String, Long>> keyMapIterator = this.mKeyMap.entrySet().iterator();
            while (keyMapIterator.hasNext()) {
                Map.Entry<String, Long> keyEntry = keyMapIterator.next();
                long connectionTime = keyEntry.getValue().longValue();
                if (allowedTime != 0 && systemTime > connectionTime + allowedTime) {
                    keyMapIterator.remove();
                    keysDeleted = true;
                }
            }
            if (keysDeleted) {
                AdbDebuggingManager.this.writeKeys(this.mKeyMap.keySet());
            }
            return keysDeleted;
        }

        public long getNextExpirationTime() {
            long minExpiration = -1;
            long allowedTime = getAllowedConnectionTime();
            if (allowedTime == 0) {
                return -1L;
            }
            long systemTime = System.currentTimeMillis();
            for (Map.Entry<String, Long> keyEntry : this.mKeyMap.entrySet()) {
                long connectionTime = keyEntry.getValue().longValue();
                long keyExpiration = Math.max(0L, (connectionTime + allowedTime) - systemTime);
                if (minExpiration == -1 || keyExpiration < minExpiration) {
                    minExpiration = keyExpiration;
                }
            }
            return minExpiration;
        }

        public void deleteKeyStore() {
            this.mKeyMap.clear();
            this.mTrustedNetworks.clear();
            AdbDebuggingManager.this.deleteKeyFile();
            AtomicFile atomicFile = this.mAtomicKeyFile;
            if (atomicFile == null) {
                return;
            }
            atomicFile.delete();
        }

        public long getLastConnectionTime(String key) {
            return this.mKeyMap.getOrDefault(key, 0L).longValue();
        }

        public void setLastConnectionTime(String key, long connectionTime) {
            setLastConnectionTime(key, connectionTime, false);
        }

        public void setLastConnectionTime(String key, long connectionTime, boolean force) {
            if ((this.mKeyMap.containsKey(key) && this.mKeyMap.get(key).longValue() >= connectionTime && !force) || this.mSystemKeys.contains(key)) {
                return;
            }
            if (!this.mKeyMap.containsKey(key)) {
                AdbDebuggingManager.this.writeKey(key);
            }
            this.mKeyMap.put(key, Long.valueOf(connectionTime));
        }

        public long getAllowedConnectionTime() {
            return Settings.Global.getLong(AdbDebuggingManager.this.mContext.getContentResolver(), "adb_allowed_connection_time", UnixCalendar.WEEK_IN_MILLIS);
        }

        public boolean isKeyAuthorized(String key) {
            if (this.mSystemKeys.contains(key)) {
                return true;
            }
            long lastConnectionTime = getLastConnectionTime(key);
            if (lastConnectionTime == 0) {
                return false;
            }
            long allowedConnectionTime = getAllowedConnectionTime();
            return allowedConnectionTime == 0 || System.currentTimeMillis() < lastConnectionTime + allowedConnectionTime;
        }

        public boolean isTrustedNetwork(String bssid) {
            return this.mTrustedNetworks.contains(bssid);
        }
    }
}