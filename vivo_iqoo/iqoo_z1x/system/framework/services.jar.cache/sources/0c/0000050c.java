package com.android.server;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.IActivityManager;
import android.app.IUidObserver;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserManager;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.app.ResolverActivity;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.pm.Settings;
import com.android.server.wm.ActivityTaskManagerInternal;
import dalvik.system.DexFile;
import dalvik.system.VMRuntime;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/* loaded from: classes.dex */
public final class PinnerService extends SystemService {
    private static final boolean DEBUG = false;
    private static final int KEY_ASSISTANT = 2;
    private static final int KEY_CAMERA = 0;
    private static final int KEY_HOME = 1;
    private static final int MATCH_FLAGS = 851968;
    private static final int MAX_ASSISTANT_PIN_SIZE = 62914560;
    private static final int MAX_CAMERA_PIN_SIZE = 83886080;
    private static final int MAX_HOME_PIN_SIZE = 6291456;
    private static final String PIN_META_FILENAME = "pinlist.meta";
    private static final String TAG = "PinnerService";
    private final IActivityManager mAm;
    private final ActivityManagerInternal mAmInternal;
    private final ActivityTaskManagerInternal mAtmInternal;
    private BinderService mBinderService;
    private final BroadcastReceiver mBroadcastReceiver;
    private final Context mContext;
    private final ArrayMap<Integer, Integer> mPendingRepin;
    private final ArraySet<Integer> mPinKeys;
    private final ArrayMap<Integer, PinnedApp> mPinnedApps;
    private final ArrayList<PinnedFile> mPinnedFiles;
    private PinnerHandler mPinnerHandler;
    private SearchManager mSearchManager;
    private final UserManager mUserManager;
    private static final int PAGE_SIZE = (int) Os.sysconf(OsConstants._SC_PAGESIZE);
    private static boolean PROP_PIN_CAMERA = DeviceConfig.getBoolean("runtime_native_boot", "pin_camera", SystemProperties.getBoolean("pinner.pin_camera", false));
    private static boolean PROP_PIN_PINLIST = SystemProperties.getBoolean("pinner.use_pinlist", true);
    private static boolean PROP_PIN_ODEX = SystemProperties.getBoolean("pinner.whole_odex", true);

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface AppKey {
    }

    public PinnerService(Context context) {
        super(context);
        this.mPinnedFiles = new ArrayList<>();
        this.mPinnedApps = new ArrayMap<>();
        this.mPendingRepin = new ArrayMap<>();
        this.mPinKeys = new ArraySet<>();
        this.mPinnerHandler = null;
        this.mBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.PinnerService.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                if ("android.intent.action.PACKAGE_REPLACED".equals(intent.getAction())) {
                    Uri packageUri = intent.getData();
                    String packageName = packageUri.getSchemeSpecificPart();
                    ArraySet<String> updatedPackages = new ArraySet<>();
                    updatedPackages.add(packageName);
                    PinnerService.this.update(updatedPackages, true);
                }
            }
        };
        this.mContext = context;
        boolean shouldPinCamera = context.getResources().getBoolean(17891503);
        boolean shouldPinHome = context.getResources().getBoolean(17891504);
        boolean shouldPinAssistant = context.getResources().getBoolean(17891502);
        if (shouldPinCamera && PROP_PIN_CAMERA) {
            this.mPinKeys.add(0);
        }
        if (shouldPinHome) {
            this.mPinKeys.add(1);
        }
        if (shouldPinAssistant) {
            this.mPinKeys.add(2);
        }
        this.mPinnerHandler = new PinnerHandler(BackgroundThread.get().getLooper());
        this.mAtmInternal = (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);
        this.mAmInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        this.mAm = ActivityManager.getService();
        this.mUserManager = (UserManager) this.mContext.getSystemService(UserManager.class);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_REPLACED");
        filter.addDataScheme(Settings.ATTR_PACKAGE);
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
        registerUidListener();
        registerUserSetupCompleteListener();
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        BinderService binderService = new BinderService();
        this.mBinderService = binderService;
        publishBinderService("pinner", binderService);
        publishLocalService(PinnerService.class, this);
        this.mPinnerHandler.obtainMessage(4001).sendToTarget();
        sendPinAppsMessage(0);
    }

    @Override // com.android.server.SystemService
    public void onBootPhase(int phase) {
        if (phase == 500) {
            this.mSearchManager = (SearchManager) this.mContext.getSystemService("search");
            sendPinAppsMessage(0);
        }
    }

    @Override // com.android.server.SystemService
    public void onSwitchUser(int userHandle) {
        if (!this.mUserManager.isManagedProfile(userHandle)) {
            sendPinAppsMessage(userHandle);
        }
    }

    @Override // com.android.server.SystemService
    public void onUnlockUser(int userHandle) {
        if (!this.mUserManager.isManagedProfile(userHandle)) {
            sendPinAppsMessage(userHandle);
        }
    }

    public void update(ArraySet<String> updatedPackages, boolean force) {
        int currentUser = ActivityManager.getCurrentUser();
        for (int i = this.mPinKeys.size() - 1; i >= 0; i--) {
            int key = this.mPinKeys.valueAt(i).intValue();
            ApplicationInfo info = getInfoForKey(key, currentUser);
            if (info != null && updatedPackages.contains(info.packageName)) {
                Slog.i(TAG, "Updating pinned files for " + info.packageName + " force=" + force);
                sendPinAppMessage(key, currentUser, force);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handlePinOnStart() {
        String[] filesToPin;
        String bootImage = SystemProperties.get("dalvik.vm.boot-image", "");
        if (bootImage.endsWith("boot-image.prof")) {
            filesToPin = this.mContext.getResources().getStringArray(17236044);
        } else {
            filesToPin = this.mContext.getResources().getStringArray(17236009);
        }
        for (String fileToPin : filesToPin) {
            PinnedFile pf = pinFile(fileToPin, IVivoRmsInjector.QUIET_TYPE_ALL, false);
            if (pf == null) {
                Slog.e(TAG, "Failed to pin file = " + fileToPin);
                continue;
            } else {
                synchronized (this) {
                    this.mPinnedFiles.add(pf);
                }
                continue;
            }
        }
    }

    private void registerUserSetupCompleteListener() {
        final Uri userSetupCompleteUri = Settings.Secure.getUriFor("user_setup_complete");
        this.mContext.getContentResolver().registerContentObserver(userSetupCompleteUri, false, new ContentObserver(null) { // from class: com.android.server.PinnerService.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (userSetupCompleteUri.equals(uri)) {
                    PinnerService.this.sendPinAppMessage(1, ActivityManager.getCurrentUser(), true);
                }
            }
        }, -1);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.PinnerService$3  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass3 extends IUidObserver.Stub {
        AnonymousClass3() {
        }

        public void onUidGone(int uid, boolean disabled) throws RemoteException {
            PinnerService.this.mPinnerHandler.sendMessage(PooledLambda.obtainMessage($$Lambda$PinnerService$3$RQBbrt9b8esLBxJImxDgVTsP34I.INSTANCE, PinnerService.this, Integer.valueOf(uid)));
        }

        public void onUidActive(int uid) throws RemoteException {
            PinnerService.this.mPinnerHandler.sendMessage(PooledLambda.obtainMessage($$Lambda$PinnerService$3$3Ta6TX4Jq9YbpUYE5Y0r8Xt8rBw.INSTANCE, PinnerService.this, Integer.valueOf(uid)));
        }

        public void onUidIdle(int uid, boolean disabled) throws RemoteException {
        }

        public void onUidStateChanged(int uid, int procState, long procStateSeq, int capability) throws RemoteException {
        }

        public void onUidCachedChanged(int uid, boolean cached) throws RemoteException {
        }
    }

    private void registerUidListener() {
        try {
            this.mAm.registerUidObserver(new AnonymousClass3(), 10, 0, (String) null);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to register uid observer", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleUidGone(int uid) {
        updateActiveState(uid, false);
        synchronized (this) {
            int key = this.mPendingRepin.getOrDefault(Integer.valueOf(uid), -1).intValue();
            if (key == -1) {
                return;
            }
            this.mPendingRepin.remove(Integer.valueOf(uid));
            pinApp(key, ActivityManager.getCurrentUser(), false);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleUidActive(int uid) {
        updateActiveState(uid, true);
    }

    private void updateActiveState(int uid, boolean active) {
        synchronized (this) {
            for (int i = this.mPinnedApps.size() - 1; i >= 0; i--) {
                PinnedApp app = this.mPinnedApps.valueAt(i);
                if (app.uid == uid) {
                    app.active = active;
                }
            }
        }
    }

    private void unpinApp(int key) {
        synchronized (this) {
            PinnedApp app = this.mPinnedApps.get(Integer.valueOf(key));
            if (app == null) {
                return;
            }
            this.mPinnedApps.remove(Integer.valueOf(key));
            ArrayList<PinnedFile> pinnedAppFiles = new ArrayList<>(app.mFiles);
            Iterator<PinnedFile> it = pinnedAppFiles.iterator();
            while (it.hasNext()) {
                PinnedFile pinnedFile = it.next();
                pinnedFile.close();
            }
        }
    }

    private boolean isResolverActivity(ActivityInfo info) {
        return ResolverActivity.class.getName().equals(info.name);
    }

    private ApplicationInfo getCameraInfo(int userHandle) {
        Intent cameraIntent = new Intent("android.media.action.STILL_IMAGE_CAMERA");
        ApplicationInfo info = getApplicationInfoForIntent(cameraIntent, userHandle, false);
        if (info == null) {
            Intent cameraIntent2 = new Intent("android.media.action.STILL_IMAGE_CAMERA_SECURE");
            info = getApplicationInfoForIntent(cameraIntent2, userHandle, false);
        }
        if (info == null) {
            Intent cameraIntent3 = new Intent("android.media.action.STILL_IMAGE_CAMERA");
            return getApplicationInfoForIntent(cameraIntent3, userHandle, true);
        }
        return info;
    }

    private ApplicationInfo getHomeInfo(int userHandle) {
        Intent intent = this.mAtmInternal.getHomeIntent();
        return getApplicationInfoForIntent(intent, userHandle, false);
    }

    private ApplicationInfo getAssistantInfo(int userHandle) {
        SearchManager searchManager = this.mSearchManager;
        if (searchManager != null) {
            Intent intent = searchManager.getAssistIntent(false);
            return getApplicationInfoForIntent(intent, userHandle, true);
        }
        return null;
    }

    private ApplicationInfo getApplicationInfoForIntent(Intent intent, int userHandle, boolean defaultToSystemApp) {
        ResolveInfo resolveInfo;
        if (intent == null || (resolveInfo = this.mContext.getPackageManager().resolveActivityAsUser(intent, MATCH_FLAGS, userHandle)) == null) {
            return null;
        }
        if (!isResolverActivity(resolveInfo.activityInfo)) {
            return resolveInfo.activityInfo.applicationInfo;
        }
        if (!defaultToSystemApp) {
            return null;
        }
        List<ResolveInfo> infoList = this.mContext.getPackageManager().queryIntentActivitiesAsUser(intent, MATCH_FLAGS, userHandle);
        ApplicationInfo systemAppInfo = null;
        for (ResolveInfo info : infoList) {
            if ((info.activityInfo.applicationInfo.flags & 1) != 0) {
                if (systemAppInfo != null) {
                    return null;
                }
                systemAppInfo = info.activityInfo.applicationInfo;
            }
        }
        return systemAppInfo;
    }

    private void sendPinAppsMessage(int userHandle) {
        this.mPinnerHandler.sendMessage(PooledLambda.obtainMessage($$Lambda$PinnerService$GeEX8XoHeV0LEszxat7jOSlrs4.INSTANCE, this, Integer.valueOf(userHandle)));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void pinApps(int userHandle) {
        for (int i = this.mPinKeys.size() - 1; i >= 0; i--) {
            int key = this.mPinKeys.valueAt(i).intValue();
            pinApp(key, userHandle, true);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendPinAppMessage(int key, int userHandle, boolean force) {
        this.mPinnerHandler.sendMessage(PooledLambda.obtainMessage($$Lambda$PinnerService$6bekYOn4YXi0x7vYNWO40QyAs8.INSTANCE, this, Integer.valueOf(key), Integer.valueOf(userHandle), Boolean.valueOf(force)));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void pinApp(int key, int userHandle, boolean force) {
        int uid = getUidForKey(key);
        if (!force && uid != -1) {
            synchronized (this) {
                this.mPendingRepin.put(Integer.valueOf(uid), Integer.valueOf(key));
            }
            return;
        }
        unpinApp(key);
        ApplicationInfo info = getInfoForKey(key, userHandle);
        if (info != null) {
            pinApp(key, info);
        }
    }

    private int getUidForKey(int key) {
        int i;
        synchronized (this) {
            PinnedApp existing = this.mPinnedApps.get(Integer.valueOf(key));
            if (existing != null && existing.active) {
                i = existing.uid;
            } else {
                i = -1;
            }
        }
        return i;
    }

    private ApplicationInfo getInfoForKey(int key, int userHandle) {
        if (key != 0) {
            if (key != 1) {
                if (key == 2) {
                    return getAssistantInfo(userHandle);
                }
                return null;
            }
            return getHomeInfo(userHandle);
        }
        return getCameraInfo(userHandle);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getNameForKey(int key) {
        if (key != 0) {
            if (key != 1) {
                if (key == 2) {
                    return "Assistant";
                }
                return null;
            }
            return "Home";
        }
        return "Camera";
    }

    private int getSizeLimitForKey(int key) {
        if (key != 0) {
            if (key != 1) {
                if (key == 2) {
                    return MAX_ASSISTANT_PIN_SIZE;
                }
                return 0;
            }
            return MAX_HOME_PIN_SIZE;
        }
        return 83886080;
    }

    private void pinApp(int key, ApplicationInfo appInfo) {
        if (appInfo == null) {
            return;
        }
        PinnedApp pinnedApp = new PinnedApp(appInfo);
        synchronized (this) {
            this.mPinnedApps.put(Integer.valueOf(key), pinnedApp);
        }
        int pinSizeLimit = getSizeLimitForKey(key);
        String apk = appInfo.sourceDir;
        PinnedFile pf = pinFile(apk, pinSizeLimit, true);
        if (pf == null) {
            Slog.e(TAG, "Failed to pin " + apk);
            return;
        }
        synchronized (this) {
            pinnedApp.mFiles.add(pf);
        }
        String abi = appInfo.primaryCpuAbi != null ? appInfo.primaryCpuAbi : Build.SUPPORTED_ABIS[0];
        String arch = VMRuntime.getInstructionSet(abi);
        String baseCodePath = appInfo.getBaseCodePath();
        String[] files = null;
        try {
            files = DexFile.getDexFileOutputPaths(baseCodePath, arch);
        } catch (IOException e) {
        }
        if (files == null) {
            return;
        }
        for (String file : files) {
            PinnedFile pf2 = pinFile(file, pinSizeLimit, false);
            if (pf2 != null) {
                synchronized (this) {
                    if (PROP_PIN_ODEX) {
                        pinnedApp.mFiles.add(pf2);
                    }
                }
            }
        }
    }

    private static PinnedFile pinFile(String fileToPin, int maxBytesToPin, boolean attemptPinIntrospection) {
        PinRangeSource pinRangeSource;
        ZipFile fileAsZip = null;
        InputStream pinRangeStream = null;
        if (attemptPinIntrospection) {
            try {
                fileAsZip = maybeOpenZip(fileToPin);
            } catch (Throwable th) {
                safeClose(pinRangeStream);
                safeClose(fileAsZip);
                throw th;
            }
        }
        if (fileAsZip != null) {
            pinRangeStream = maybeOpenPinMetaInZip(fileAsZip, fileToPin);
        }
        Slog.d(TAG, "pinRangeStream: " + pinRangeStream);
        if (pinRangeStream != null) {
            pinRangeSource = new PinRangeSourceStream(pinRangeStream);
        } else {
            pinRangeSource = new PinRangeSourceStatic(0, IVivoRmsInjector.QUIET_TYPE_ALL);
        }
        PinnedFile pinFileRanges = pinFileRanges(fileToPin, maxBytesToPin, pinRangeSource);
        safeClose(pinRangeStream);
        safeClose(fileAsZip);
        return pinFileRanges;
    }

    private static ZipFile maybeOpenZip(String fileName) {
        try {
            ZipFile zip = new ZipFile(fileName);
            return zip;
        } catch (IOException ex) {
            Slog.w(TAG, String.format("could not open \"%s\" as zip: pinning as blob", fileName), ex);
            return null;
        }
    }

    private static InputStream maybeOpenPinMetaInZip(ZipFile zipFile, String fileName) {
        ZipEntry pinMetaEntry;
        if (!PROP_PIN_PINLIST || (pinMetaEntry = zipFile.getEntry(PIN_META_FILENAME)) == null) {
            return null;
        }
        try {
            InputStream pinMetaStream = zipFile.getInputStream(pinMetaEntry);
            return pinMetaStream;
        } catch (IOException ex) {
            Slog.w(TAG, String.format("error reading pin metadata \"%s\": pinning as blob", fileName), ex);
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static abstract class PinRangeSource {
        abstract boolean read(PinRange pinRange);

        private PinRangeSource() {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class PinRangeSourceStatic extends PinRangeSource {
        private boolean mDone;
        private final int mPinLength;
        private final int mPinStart;

        PinRangeSourceStatic(int pinStart, int pinLength) {
            super();
            this.mDone = false;
            this.mPinStart = pinStart;
            this.mPinLength = pinLength;
        }

        @Override // com.android.server.PinnerService.PinRangeSource
        boolean read(PinRange outPinRange) {
            outPinRange.start = this.mPinStart;
            outPinRange.length = this.mPinLength;
            boolean done = this.mDone;
            this.mDone = true;
            return !done;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class PinRangeSourceStream extends PinRangeSource {
        private boolean mDone;
        private final DataInputStream mStream;

        PinRangeSourceStream(InputStream stream) {
            super();
            this.mDone = false;
            this.mStream = new DataInputStream(stream);
        }

        @Override // com.android.server.PinnerService.PinRangeSource
        boolean read(PinRange outPinRange) {
            if (!this.mDone) {
                try {
                    outPinRange.start = this.mStream.readInt();
                    outPinRange.length = this.mStream.readInt();
                } catch (IOException e) {
                    this.mDone = true;
                }
            }
            return !this.mDone;
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:66:0x015e  */
    /* JADX WARN: Removed duplicated region for block: B:71:0x016d  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private static com.android.server.PinnerService.PinnedFile pinFileRanges(java.lang.String r19, int r20, com.android.server.PinnerService.PinRangeSource r21) {
        /*
            Method dump skipped, instructions count: 370
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.PinnerService.pinFileRanges(java.lang.String, int, com.android.server.PinnerService$PinRangeSource):com.android.server.PinnerService$PinnedFile");
    }

    private static int clamp(int min, int value, int max) {
        return Math.max(min, Math.min(value, max));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void safeMunmap(long address, long mapSize) {
        try {
            Os.munmap(address, mapSize);
        } catch (ErrnoException ex) {
            Slog.w(TAG, "ignoring error in unmap", ex);
        }
    }

    private static void safeClose(FileDescriptor fd) {
        if (fd != null && fd.valid()) {
            try {
                Os.close(fd);
            } catch (ErrnoException ex) {
                if (ex.errno == OsConstants.EBADF) {
                    throw new AssertionError(ex);
                }
            }
        }
    }

    private static void safeClose(Closeable thing) {
        if (thing != null) {
            try {
                thing.close();
            } catch (IOException ex) {
                Slog.w(TAG, "ignoring error closing resource: " + thing, ex);
            }
        }
    }

    /* loaded from: classes.dex */
    private final class BinderService extends Binder {
        private BinderService() {
        }

        @Override // android.os.Binder
        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(PinnerService.this.mContext, PinnerService.TAG, pw)) {
                synchronized (PinnerService.this) {
                    long totalSize = 0;
                    Iterator it = PinnerService.this.mPinnedFiles.iterator();
                    while (it.hasNext()) {
                        PinnedFile pinnedFile = (PinnedFile) it.next();
                        pw.format("%s %s\n", pinnedFile.fileName, Integer.valueOf(pinnedFile.bytesPinned));
                        totalSize += pinnedFile.bytesPinned;
                    }
                    pw.println();
                    for (Integer num : PinnerService.this.mPinnedApps.keySet()) {
                        int key = num.intValue();
                        PinnedApp app = (PinnedApp) PinnerService.this.mPinnedApps.get(Integer.valueOf(key));
                        pw.print(PinnerService.this.getNameForKey(key));
                        pw.print(" uid=");
                        pw.print(app.uid);
                        pw.print(" active=");
                        pw.print(app.active);
                        pw.println();
                        Iterator<PinnedFile> it2 = ((PinnedApp) PinnerService.this.mPinnedApps.get(Integer.valueOf(key))).mFiles.iterator();
                        while (it2.hasNext()) {
                            PinnedFile pf = it2.next();
                            pw.print("  ");
                            pw.format("%s %s\n", pf.fileName, Integer.valueOf(pf.bytesPinned));
                            totalSize += pf.bytesPinned;
                        }
                    }
                    pw.format("Total size: %s\n", Long.valueOf(totalSize));
                    pw.println();
                    if (!PinnerService.this.mPendingRepin.isEmpty()) {
                        pw.print("Pending repin: ");
                        for (Integer num2 : PinnerService.this.mPendingRepin.values()) {
                            pw.print(PinnerService.this.getNameForKey(num2.intValue()));
                            pw.print(' ');
                        }
                        pw.println();
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class PinnedFile implements AutoCloseable {
        final int bytesPinned;
        final String fileName;
        private long mAddress;
        final int mapSize;

        PinnedFile(long address, int mapSize, String fileName, int bytesPinned) {
            this.mAddress = address;
            this.mapSize = mapSize;
            this.fileName = fileName;
            this.bytesPinned = bytesPinned;
        }

        @Override // java.lang.AutoCloseable
        public void close() {
            long j = this.mAddress;
            if (j >= 0) {
                PinnerService.safeMunmap(j, this.mapSize);
                this.mAddress = -1L;
            }
        }

        public void finalize() {
            close();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class PinRange {
        int length;
        int start;

        PinRange() {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class PinnedApp {
        boolean active;
        final ArrayList<PinnedFile> mFiles;
        final int uid;

        private PinnedApp(ApplicationInfo appInfo) {
            this.mFiles = new ArrayList<>();
            this.uid = appInfo.uid;
            this.active = PinnerService.this.mAmInternal.isUidActive(this.uid);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class PinnerHandler extends Handler {
        static final int PIN_ONSTART_MSG = 4001;

        public PinnerHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what == PIN_ONSTART_MSG) {
                PinnerService.this.handlePinOnStart();
            } else {
                super.handleMessage(msg);
            }
        }
    }
}