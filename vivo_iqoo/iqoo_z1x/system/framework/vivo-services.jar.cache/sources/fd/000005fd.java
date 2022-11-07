package com.vivo.services.artkeeper;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Slog;
import com.android.server.ServiceThread;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import vivo.app.artkeeper.IVivoArtKeeperManager;

/* loaded from: classes.dex */
public class VivoArtKeeperService extends IVivoArtKeeperManager.Stub {
    static final int ARTAPP_DISABLE = -1;
    static final int ARTAPP_ENABLE = 1;
    static final int ARTAPP_UNKNOW = 0;
    static final int CONNECT_TIMEOUT = 1000;
    static final int DATA_CONNECT = 2;
    private static final boolean DBG = false;
    static final int DEFAULT_TASK_TIME_OUT = 5;
    static final int DEFULAT_READ_DATABASE_TIMEOUT = 1;
    static final int DOWNLOAD_BUFFER_SIZE = 4096;
    static final int MAX = 8096;
    private static final int MAX_THREAD_COUNT = 4;
    static final int NET_DISCONNECT = 0;
    static final int PROF_MAX_SIZE = 2097152;
    static final int READ_TIMEOUT = 3000;
    private static final String TAG = "VivoArtKeeperService";
    static final int WIFI_CONNECT = 1;
    static final String artAppEnable = "artpp_enabled";
    static final String localAbsProfileDir = "/data/misc/usable-profiles/";
    private static VivoArtKeeperService sInstance;
    private Context mContext;
    private final ArtKeeperHandler mHandler;
    private final ServiceThread mHandlerThread;
    private PackageManager mPackageManager;
    private Handler mUiHandler = new Handler(Looper.getMainLooper()) { // from class: com.vivo.services.artkeeper.VivoArtKeeperService.1
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
        }
    };
    static final Boolean artAppSupport = Boolean.valueOf(SystemProperties.getBoolean("ro.build.artpp.support", false));
    static final String profileVersion = "ro.build.artpp.profile_version";
    static final String defaultUrl = "https://artpp-vivofs.vivo.com.cn/artpp/profiles/" + SystemProperties.get(profileVersion, "010") + "/";
    static final String defaultUrlOverseas = "https://artpp-vivofs.vivoglobal.com/artpp/profiles/" + SystemProperties.get(profileVersion, "010") + "/";
    private static ExecutorService mThreadPool = Executors.newFixedThreadPool(4);
    private static int mArtAppStatus = -1;
    public static boolean SERIES_OVERSEAS = SystemProperties.getBoolean("ro.vivo.product.overseas", false);

    private VivoArtKeeperService(Context context) {
        this.mPackageManager = null;
        Slog.i(TAG, "artKeeper Service start");
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
        ServiceThread serviceThread = new ServiceThread(TAG, 10, true);
        this.mHandlerThread = serviceThread;
        serviceThread.start();
        this.mHandler = new ArtKeeperHandler(this.mHandlerThread.getLooper());
        ArtKeeperObserver artkeeperObserver = new ArtKeeperObserver(this.mHandler);
        artkeeperObserver.observe();
    }

    public static synchronized VivoArtKeeperService getInstance(Context context) {
        VivoArtKeeperService vivoArtKeeperService;
        synchronized (VivoArtKeeperService.class) {
            if (sInstance == null) {
                sInstance = new VivoArtKeeperService(context);
            }
            vivoArtKeeperService = sInstance;
        }
        return vivoArtKeeperService;
    }

    private boolean checkIsSystemApp(String packageName) {
        try {
            ApplicationInfo info = this.mPackageManager.getApplicationInfo(packageName, 0);
            return (info.flags & 1) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "Failed get application info for uid: " + packageName + ", " + e);
            return false;
        }
    }

    public boolean prepareUsableProfileMethodTask(String packageName, String md5, boolean forceDownload, int timeOut, boolean block) {
        if (!artAppSupport.booleanValue()) {
            Slog.d(TAG, "artApp isn't support");
            return false;
        }
        return prepareUsableProfileMethodTaskLI(packageName, md5, forceDownload, timeOut, block);
    }

    public boolean isArtppSupported() {
        return artAppSupport.booleanValue() && (checkIsSystemApp("com.bbk.appstore") || checkIsSystemApp("com.vivo.appstore"));
    }

    private int downloadFile(HttpURLConnection connection, FileOutputStream outputStream) {
        if (connection == null || outputStream == null) {
            return -1;
        }
        InputStream inputStream = null;
        int totalBytesRead = 0;
        byte[] buffer = new byte[4096];
        try {
            try {
                inputStream = connection.getInputStream();
                while (true) {
                    int bytesRead = inputStream.read(buffer);
                    if (bytesRead == -1) {
                        break;
                    }
                    totalBytesRead += bytesRead;
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
                Slog.d(TAG, "finish downloadFile");
                try {
                    inputStream.close();
                } catch (Exception ex) {
                    Slog.e(TAG, "downloadFile close failed:" + ex);
                }
                return totalBytesRead;
            } catch (Exception e) {
                Slog.e(TAG, "Downloaded failed:" + e);
                try {
                    inputStream.close();
                } catch (Exception ex2) {
                    Slog.e(TAG, "downloadFile close failed:" + ex2);
                }
                return -1;
            }
        } catch (Throwable th) {
            try {
                inputStream.close();
            } catch (Exception ex3) {
                Slog.e(TAG, "downloadFile close failed:" + ex3);
            }
            throw th;
        }
    }

    boolean prepareWithDownload(URL url, FileOutputStream output) {
        if (url == null || output == null) {
            return false;
        }
        HttpURLConnection connection = null;
        try {
            try {
                if (getConnectStatus() == 0) {
                    if (0 != 0) {
                        connection.disconnect();
                    }
                    return false;
                }
                HttpURLConnection connection2 = (HttpURLConnection) url.openConnection();
                connection2.setConnectTimeout(1000);
                connection2.setReadTimeout(3000);
                connection2.setRequestProperty("Accept-Encoding", "identity");
                connection2.setRequestMethod("GET");
                connection2.connect();
                if (connection2.getResponseCode() != 200) {
                    Slog.e(TAG, "Server returned HTTP " + connection2.getResponseCode() + " " + connection2.getResponseMessage());
                    if (connection2 != null) {
                        connection2.disconnect();
                    }
                    return false;
                }
                long fileSize = connection2.getContentLength();
                if (fileSize > 0 && fileSize <= 2097152) {
                    long bytesRead = downloadFile(connection2, output);
                    Slog.d(TAG, "bytesRead = " + bytesRead);
                    if (bytesRead == fileSize) {
                        if (connection2 != null) {
                            connection2.disconnect();
                            return true;
                        }
                        return true;
                    }
                    Slog.e(TAG, "Failed to download file fileSize=" + fileSize);
                    if (connection2 != null) {
                        connection2.disconnect();
                    }
                    return false;
                }
                Slog.e(TAG, "illegal prof file fileSize=" + fileSize);
                if (connection2 != null) {
                    connection2.disconnect();
                }
                return false;
            } catch (Exception e) {
                Slog.e(TAG, "prepareWithDownload exception:" + e);
                if (0 != 0) {
                    connection.disconnect();
                }
                return false;
            }
        } catch (Throwable th) {
            if (0 != 0) {
                connection.disconnect();
            }
            throw th;
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:158:0x034c A[Catch: Exception -> 0x0362, TryCatch #19 {Exception -> 0x0362, blocks: (B:156:0x0346, B:158:0x034c, B:160:0x0352, B:161:0x0359), top: B:237:0x0346 }] */
    /* JADX WARN: Removed duplicated region for block: B:161:0x0359 A[Catch: Exception -> 0x0362, TRY_LEAVE, TryCatch #19 {Exception -> 0x0362, blocks: (B:156:0x0346, B:158:0x034c, B:160:0x0352, B:161:0x0359), top: B:237:0x0346 }] */
    /* JADX WARN: Removed duplicated region for block: B:189:0x03d0  */
    /* JADX WARN: Removed duplicated region for block: B:205:0x0412  */
    /* JADX WARN: Removed duplicated region for block: B:219:0x032b A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:254:0x0377 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    boolean prepareUsableProfileMethod(java.lang.String r27, java.lang.String r28, boolean r29) {
        /*
            Method dump skipped, instructions count: 1100
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.vivo.services.artkeeper.VivoArtKeeperService.prepareUsableProfileMethod(java.lang.String, java.lang.String, boolean):boolean");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateArtAppEnable() {
        try {
            synchronized (this) {
                mArtAppStatus = Settings.Global.getInt(this.mContext.getContentResolver(), artAppEnable, -1);
                Slog.d(TAG, "updateArtAppEnable = " + mArtAppStatus);
            }
        } catch (Exception e) {
            Slog.e(TAG, "Failed get artAppStatus:" + e);
        }
    }

    boolean confirmArtKeeperStatus() {
        synchronized (this) {
            if (mArtAppStatus == 1) {
                Slog.d(TAG, "artApp is enabled");
                return true;
            } else if (mArtAppStatus == -1) {
                Slog.d(TAG, "artApp is disable and not ask");
                return false;
            } else {
                final VivoArtKeeperWarnDialog vivoArtKeeperWarnDialog = VivoArtKeeperWarnDialog.getInstance(this.mContext, this.mUiHandler);
                this.mUiHandler.post(new Runnable() { // from class: com.vivo.services.artkeeper.VivoArtKeeperService.2
                    @Override // java.lang.Runnable
                    public void run() {
                        vivoArtKeeperWarnDialog.ShowArtKeeperWarnDialog();
                    }
                });
                return false;
            }
        }
    }

    int getConnectStatus() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
            NetworkInfo currentConnection = connectivityManager.getActiveNetworkInfo();
            boolean isConnected = currentConnection != null && currentConnection.isConnected();
            if (!isConnected) {
                Slog.d(TAG, "net isn't connect");
                return 0;
            } else if (currentConnection.getType() == 1) {
                Slog.d(TAG, "is wifi connect");
                return 1;
            } else {
                Slog.d(TAG, "wifi isn't connect");
                return 2;
            }
        } catch (Exception e) {
            Slog.e(TAG, "getConnectStatus: " + e);
            return 0;
        }
    }

    boolean checkIfAllow(int uid) {
        if (uid == 1000) {
            return true;
        }
        try {
            String packageName = this.mPackageManager.getNameForUid(uid);
            if (packageName != null) {
                Slog.d(TAG, "check packageName = " + packageName);
            }
            if (packageName != null) {
                if (!packageName.equals("com.bbk.appstore") && !packageName.equals("com.android.packageinstaller") && !packageName.equals("com.vivo.appstore")) {
                    return false;
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            Slog.e(TAG, "Failed get name for uid:" + uid + " " + e);
            return false;
        }
    }

    boolean prepareUsableProfileMethodTaskLI(final String packageName, final String md5, final boolean forceDownload, int timeOut, boolean block) {
        Slog.d(TAG, "packageName=" + packageName + " md5=" + md5 + " forceDownload=" + forceDownload + " timeOut=" + timeOut + " block=" + block);
        if (!forceDownload) {
            Slog.d(TAG, "forceDownload is false");
            return false;
        } else if (!checkIfAllow(Binder.getCallingUid())) {
            Slog.d(TAG, "check uid is denied");
            return false;
        } else if (!confirmArtKeeperStatus()) {
            Slog.d(TAG, "skip profile download");
            return false;
        } else {
            timeOut = (timeOut < 0 || timeOut > 10) ? 5 : 5;
            Slog.d(TAG, "prepareUsableProfileMethod start taks");
            FutureTask<Boolean> task = new FutureTask<>(new Callable<Boolean>() { // from class: com.vivo.services.artkeeper.VivoArtKeeperService.3
                /* JADX WARN: Can't rename method to resolve collision */
                @Override // java.util.concurrent.Callable
                public Boolean call() {
                    return Boolean.valueOf(VivoArtKeeperService.this.prepareUsableProfileMethod(packageName, md5, forceDownload));
                }
            });
            mThreadPool.submit(task);
            if (!block) {
                return true;
            }
            try {
                boolean result = task.get(timeOut, TimeUnit.SECONDS).booleanValue();
                return result;
            } catch (InterruptedException e) {
                Slog.e(TAG, "InterruptedException :" + e);
                return false;
            } catch (CancellationException e2) {
                Slog.e(TAG, "CancellationException :" + e2);
                return false;
            } catch (ExecutionException e3) {
                Slog.e(TAG, "ExecutionException :" + e3);
                return false;
            } catch (TimeoutException e4) {
                task.cancel(true);
                Slog.e(TAG, "TimeoutException :" + e4);
                return false;
            }
        }
    }

    /* loaded from: classes.dex */
    class ArtKeeperHandler extends Handler {
        ArtKeeperHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
        }
    }

    /* loaded from: classes.dex */
    private final class ArtKeeperObserver extends ContentObserver {
        public ArtKeeperObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = VivoArtKeeperService.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.Global.getUriFor(VivoArtKeeperService.artAppEnable), false, this, -1);
            VivoArtKeeperService.this.updateArtAppEnable();
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            VivoArtKeeperService.this.updateArtAppEnable();
        }
    }
}