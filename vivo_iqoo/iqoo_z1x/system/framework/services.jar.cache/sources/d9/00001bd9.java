package com.android.server.webkit;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Slog;
import android.webkit.UserPackage;
import android.webkit.WebViewFactory;
import android.webkit.WebViewProviderInfo;
import android.webkit.WebViewProviderResponse;
import com.android.server.IVivoRmsInjector;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class WebViewUpdater {
    private static final String TAG = WebViewUpdater.class.getSimpleName();
    private static final int VALIDITY_INCORRECT_SDK_VERSION = 1;
    private static final int VALIDITY_INCORRECT_SIGNATURE = 3;
    private static final int VALIDITY_INCORRECT_VERSION_CODE = 2;
    private static final int VALIDITY_NO_LIBRARY_FLAG = 4;
    private static final int VALIDITY_OK = 0;
    private static final int WAIT_TIMEOUT_MS = 1000;
    private Context mContext;
    private SystemInterface mSystemInterface;
    private long mMinimumVersionCode = -1;
    private int mNumRelroCreationsStarted = 0;
    private int mNumRelroCreationsFinished = 0;
    private boolean mWebViewPackageDirty = false;
    private boolean mAnyWebViewInstalled = false;
    private int NUMBER_OF_RELROS_UNKNOWN = IVivoRmsInjector.QUIET_TYPE_ALL;
    private PackageInfo mCurrentWebViewPackage = null;
    private final Object mLock = new Object();

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class WebViewPackageMissingException extends Exception {
        public WebViewPackageMissingException(String message) {
            super(message);
        }

        public WebViewPackageMissingException(Exception e) {
            super(e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WebViewUpdater(Context context, SystemInterface systemInterface) {
        this.mContext = context;
        this.mSystemInterface = systemInterface;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void packageStateChanged(String packageName, int changedState) {
        WebViewProviderInfo[] webViewPackages;
        boolean z = false;
        for (WebViewProviderInfo provider : this.mSystemInterface.getWebViewPackages()) {
            String webviewPackage = provider.packageName;
            if (webviewPackage.equals(packageName)) {
                boolean updateWebView = false;
                boolean removedOrChangedOldPackage = false;
                String oldProviderName = null;
                synchronized (this.mLock) {
                    try {
                        PackageInfo newPackage = findPreferredWebViewPackage();
                        if (this.mCurrentWebViewPackage != null) {
                            oldProviderName = this.mCurrentWebViewPackage.packageName;
                        }
                        updateWebView = (provider.packageName.equals(newPackage.packageName) || provider.packageName.equals(oldProviderName) || this.mCurrentWebViewPackage == null) ? true : true;
                        removedOrChangedOldPackage = provider.packageName.equals(oldProviderName);
                        if (updateWebView) {
                            onWebViewProviderChanged(newPackage);
                        }
                    } catch (WebViewPackageMissingException e) {
                        this.mCurrentWebViewPackage = null;
                        Slog.e(TAG, "Could not find valid WebView package to create relro with " + e);
                    }
                }
                if (updateWebView && !removedOrChangedOldPackage && oldProviderName != null) {
                    this.mSystemInterface.killPackageDependents(oldProviderName);
                    return;
                }
                return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void prepareWebViewInSystemServer() {
        try {
            synchronized (this.mLock) {
                this.mCurrentWebViewPackage = findPreferredWebViewPackage();
                String userSetting = this.mSystemInterface.getUserChosenWebViewProvider(this.mContext);
                if (userSetting != null && !userSetting.equals(this.mCurrentWebViewPackage.packageName)) {
                    this.mSystemInterface.updateUserSetting(this.mContext, this.mCurrentWebViewPackage.packageName);
                }
                onWebViewProviderChanged(this.mCurrentWebViewPackage);
            }
        } catch (Throwable t) {
            Slog.e(TAG, "error preparing webview provider from system server", t);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String changeProviderAndSetting(String newProviderName) {
        PackageInfo newPackage = updateCurrentWebViewPackage(newProviderName);
        return newPackage == null ? "" : newPackage.packageName;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:16:0x002b A[Catch: all -> 0x0057, TRY_ENTER, TryCatch #0 {, blocks: (B:4:0x0006, B:6:0x000b, B:7:0x0012, B:9:0x0019, B:16:0x002b, B:17:0x002e, B:24:0x003d, B:25:0x0055), top: B:30:0x0006, inners: #1 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public android.content.pm.PackageInfo updateCurrentWebViewPackage(java.lang.String r10) {
        /*
            r9 = this;
            r0 = 0
            r1 = 0
            r2 = 0
            java.lang.Object r3 = r9.mLock
            monitor-enter(r3)
            android.content.pm.PackageInfo r4 = r9.mCurrentWebViewPackage     // Catch: java.lang.Throwable -> L57
            r0 = r4
            if (r10 == 0) goto L12
            com.android.server.webkit.SystemInterface r4 = r9.mSystemInterface     // Catch: java.lang.Throwable -> L57
            android.content.Context r5 = r9.mContext     // Catch: java.lang.Throwable -> L57
            r4.updateUserSetting(r5, r10)     // Catch: java.lang.Throwable -> L57
        L12:
            android.content.pm.PackageInfo r4 = r9.findPreferredWebViewPackage()     // Catch: com.android.server.webkit.WebViewUpdater.WebViewPackageMissingException -> L3b java.lang.Throwable -> L57
            r1 = r4
            if (r0 == 0) goto L26
            java.lang.String r4 = r1.packageName     // Catch: com.android.server.webkit.WebViewUpdater.WebViewPackageMissingException -> L3b java.lang.Throwable -> L57
            java.lang.String r5 = r0.packageName     // Catch: com.android.server.webkit.WebViewUpdater.WebViewPackageMissingException -> L3b java.lang.Throwable -> L57
            boolean r4 = r4.equals(r5)     // Catch: com.android.server.webkit.WebViewUpdater.WebViewPackageMissingException -> L3b java.lang.Throwable -> L57
            if (r4 != 0) goto L24
            goto L26
        L24:
            r4 = 0
            goto L27
        L26:
            r4 = 1
        L27:
            r2 = r4
            if (r2 == 0) goto L2e
            r9.onWebViewProviderChanged(r1)     // Catch: java.lang.Throwable -> L57
        L2e:
            monitor-exit(r3)     // Catch: java.lang.Throwable -> L57
            if (r2 == 0) goto L3a
            if (r0 == 0) goto L3a
            com.android.server.webkit.SystemInterface r3 = r9.mSystemInterface
            java.lang.String r4 = r0.packageName
            r3.killPackageDependents(r4)
        L3a:
            return r1
        L3b:
            r4 = move-exception
            r5 = 0
            r9.mCurrentWebViewPackage = r5     // Catch: java.lang.Throwable -> L57
            java.lang.String r6 = com.android.server.webkit.WebViewUpdater.TAG     // Catch: java.lang.Throwable -> L57
            java.lang.StringBuilder r7 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L57
            r7.<init>()     // Catch: java.lang.Throwable -> L57
            java.lang.String r8 = "Couldn't find WebView package to use "
            r7.append(r8)     // Catch: java.lang.Throwable -> L57
            r7.append(r4)     // Catch: java.lang.Throwable -> L57
            java.lang.String r7 = r7.toString()     // Catch: java.lang.Throwable -> L57
            android.util.Slog.e(r6, r7)     // Catch: java.lang.Throwable -> L57
            monitor-exit(r3)     // Catch: java.lang.Throwable -> L57
            return r5
        L57:
            r4 = move-exception
            monitor-exit(r3)     // Catch: java.lang.Throwable -> L57
            throw r4
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.webkit.WebViewUpdater.updateCurrentWebViewPackage(java.lang.String):android.content.pm.PackageInfo");
    }

    private void onWebViewProviderChanged(PackageInfo newPackage) {
        synchronized (this.mLock) {
            this.mAnyWebViewInstalled = true;
            if (this.mNumRelroCreationsStarted == this.mNumRelroCreationsFinished) {
                this.mCurrentWebViewPackage = newPackage;
                this.mNumRelroCreationsStarted = this.NUMBER_OF_RELROS_UNKNOWN;
                this.mNumRelroCreationsFinished = 0;
                this.mNumRelroCreationsStarted = this.mSystemInterface.onWebViewProviderChanged(newPackage);
                checkIfRelrosDoneLocked();
            } else {
                this.mWebViewPackageDirty = true;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WebViewProviderInfo[] getValidWebViewPackages() {
        ProviderAndPackageInfo[] providersAndPackageInfos = getValidWebViewPackagesAndInfos();
        WebViewProviderInfo[] providers = new WebViewProviderInfo[providersAndPackageInfos.length];
        for (int n = 0; n < providersAndPackageInfos.length; n++) {
            providers[n] = providersAndPackageInfos[n].provider;
        }
        return providers;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class ProviderAndPackageInfo {
        public final PackageInfo packageInfo;
        public final WebViewProviderInfo provider;

        public ProviderAndPackageInfo(WebViewProviderInfo provider, PackageInfo packageInfo) {
            this.provider = provider;
            this.packageInfo = packageInfo;
        }
    }

    private ProviderAndPackageInfo[] getValidWebViewPackagesAndInfos() {
        WebViewProviderInfo[] allProviders = this.mSystemInterface.getWebViewPackages();
        List<ProviderAndPackageInfo> providers = new ArrayList<>();
        for (int n = 0; n < allProviders.length; n++) {
            try {
                PackageInfo packageInfo = this.mSystemInterface.getPackageInfoForProvider(allProviders[n]);
                if (isValidProvider(allProviders[n], packageInfo)) {
                    providers.add(new ProviderAndPackageInfo(allProviders[n], packageInfo));
                }
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        int n2 = providers.size();
        return (ProviderAndPackageInfo[]) providers.toArray(new ProviderAndPackageInfo[n2]);
    }

    private PackageInfo findPreferredWebViewPackage() throws WebViewPackageMissingException {
        ProviderAndPackageInfo[] providers = getValidWebViewPackagesAndInfos();
        String userChosenProvider = this.mSystemInterface.getUserChosenWebViewProvider(this.mContext);
        for (ProviderAndPackageInfo providerAndPackage : providers) {
            if (providerAndPackage.provider.packageName.equals(userChosenProvider)) {
                List<UserPackage> userPackages = this.mSystemInterface.getPackageInfoForProviderAllUsers(this.mContext, providerAndPackage.provider);
                if (isInstalledAndEnabledForAllUsers(userPackages)) {
                    return providerAndPackage.packageInfo;
                }
            }
        }
        for (ProviderAndPackageInfo providerAndPackage2 : providers) {
            if (providerAndPackage2.provider.availableByDefault) {
                List<UserPackage> userPackages2 = this.mSystemInterface.getPackageInfoForProviderAllUsers(this.mContext, providerAndPackage2.provider);
                if (isInstalledAndEnabledForAllUsers(userPackages2)) {
                    return providerAndPackage2.packageInfo;
                }
            }
        }
        this.mAnyWebViewInstalled = false;
        throw new WebViewPackageMissingException("Could not find a loadable WebView package");
    }

    /* JADX WARN: Removed duplicated region for block: B:5:0x000a  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    static boolean isInstalledAndEnabledForAllUsers(java.util.List<android.webkit.UserPackage> r3) {
        /*
            java.util.Iterator r0 = r3.iterator()
        L4:
            boolean r1 = r0.hasNext()
            if (r1 == 0) goto L20
            java.lang.Object r1 = r0.next()
            android.webkit.UserPackage r1 = (android.webkit.UserPackage) r1
            boolean r2 = r1.isInstalledPackage()
            if (r2 == 0) goto L1e
            boolean r2 = r1.isEnabledPackage()
            if (r2 != 0) goto L1d
            goto L1e
        L1d:
            goto L4
        L1e:
            r0 = 0
            return r0
        L20:
            r0 = 1
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.webkit.WebViewUpdater.isInstalledAndEnabledForAllUsers(java.util.List):boolean");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void notifyRelroCreationCompleted() {
        synchronized (this.mLock) {
            this.mNumRelroCreationsFinished++;
            checkIfRelrosDoneLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public WebViewProviderResponse waitForAndGetProvider() {
        boolean webViewReady;
        PackageInfo webViewPackage;
        long timeoutTimeMs = (System.nanoTime() / 1000000) + 1000;
        int webViewStatus = 0;
        synchronized (this.mLock) {
            webViewReady = webViewIsReadyLocked();
            while (!webViewReady) {
                long timeNowMs = System.nanoTime() / 1000000;
                if (timeNowMs >= timeoutTimeMs) {
                    break;
                }
                try {
                    this.mLock.wait(timeoutTimeMs - timeNowMs);
                } catch (InterruptedException e) {
                }
                webViewReady = webViewIsReadyLocked();
            }
            webViewPackage = this.mCurrentWebViewPackage;
            if (!webViewReady) {
                if (!this.mAnyWebViewInstalled) {
                    webViewStatus = 4;
                } else {
                    webViewStatus = 3;
                    Slog.e(TAG, "Timed out waiting for relro creation, relros started " + this.mNumRelroCreationsStarted + " relros finished " + this.mNumRelroCreationsFinished + " package dirty? " + this.mWebViewPackageDirty);
                }
            }
        }
        if (!webViewReady) {
            Slog.w(TAG, "creating relro file timed out");
        }
        return new WebViewProviderResponse(webViewPackage, webViewStatus);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageInfo getCurrentWebViewPackage() {
        PackageInfo packageInfo;
        synchronized (this.mLock) {
            packageInfo = this.mCurrentWebViewPackage;
        }
        return packageInfo;
    }

    private boolean webViewIsReadyLocked() {
        return !this.mWebViewPackageDirty && this.mNumRelroCreationsStarted == this.mNumRelroCreationsFinished && this.mAnyWebViewInstalled;
    }

    private void checkIfRelrosDoneLocked() {
        if (this.mNumRelroCreationsStarted == this.mNumRelroCreationsFinished) {
            if (this.mWebViewPackageDirty) {
                this.mWebViewPackageDirty = false;
                try {
                    PackageInfo newPackage = findPreferredWebViewPackage();
                    onWebViewProviderChanged(newPackage);
                    return;
                } catch (WebViewPackageMissingException e) {
                    this.mCurrentWebViewPackage = null;
                    return;
                }
            }
            this.mLock.notifyAll();
        }
    }

    boolean isValidProvider(WebViewProviderInfo configInfo, PackageInfo packageInfo) {
        return validityResult(configInfo, packageInfo) == 0;
    }

    private int validityResult(WebViewProviderInfo configInfo, PackageInfo packageInfo) {
        if (!UserPackage.hasCorrectTargetSdkVersion(packageInfo)) {
            return 1;
        }
        if (!versionCodeGE(packageInfo.getLongVersionCode(), getMinimumVersionCode()) && !this.mSystemInterface.systemIsDebuggable()) {
            return 2;
        }
        if (!providerHasValidSignature(configInfo, packageInfo, this.mSystemInterface)) {
            return 3;
        }
        if (WebViewFactory.getWebViewLibrary(packageInfo.applicationInfo) == null) {
            return 4;
        }
        return 0;
    }

    private static boolean versionCodeGE(long versionCode1, long versionCode2) {
        long v1 = versionCode1 / 100000;
        long v2 = versionCode2 / 100000;
        return v1 >= v2;
    }

    private long getMinimumVersionCode() {
        WebViewProviderInfo[] webViewPackages;
        long j = this.mMinimumVersionCode;
        if (j > 0) {
            return j;
        }
        long minimumVersionCode = -1;
        for (WebViewProviderInfo provider : this.mSystemInterface.getWebViewPackages()) {
            if (provider.availableByDefault) {
                try {
                    long versionCode = this.mSystemInterface.getFactoryPackageVersion(provider.packageName);
                    if (minimumVersionCode < 0 || versionCode < minimumVersionCode) {
                        minimumVersionCode = versionCode;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
        }
        this.mMinimumVersionCode = minimumVersionCode;
        return minimumVersionCode;
    }

    private static boolean providerHasValidSignature(WebViewProviderInfo provider, PackageInfo packageInfo, SystemInterface systemInterface) {
        Signature[] signatureArr;
        if (systemInterface.systemIsDebuggable() || packageInfo.applicationInfo.isSystemApp()) {
            return true;
        }
        if (packageInfo.signatures.length != 1) {
            return false;
        }
        for (Signature signature : provider.signatures) {
            if (signature.equals(packageInfo.signatures[0])) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpState(PrintWriter pw) {
        synchronized (this.mLock) {
            if (this.mCurrentWebViewPackage == null) {
                pw.println("  Current WebView package is null");
            } else {
                pw.println(String.format("  Current WebView package (name, version): (%s, %s)", this.mCurrentWebViewPackage.packageName, this.mCurrentWebViewPackage.versionName));
            }
            pw.println(String.format("  Minimum targetSdkVersion: %d", 30));
            pw.println(String.format("  Minimum WebView version code: %d", Long.valueOf(this.mMinimumVersionCode)));
            pw.println(String.format("  Number of relros started: %d", Integer.valueOf(this.mNumRelroCreationsStarted)));
            pw.println(String.format("  Number of relros finished: %d", Integer.valueOf(this.mNumRelroCreationsFinished)));
            pw.println(String.format("  WebView package dirty: %b", Boolean.valueOf(this.mWebViewPackageDirty)));
            pw.println(String.format("  Any WebView package installed: %b", Boolean.valueOf(this.mAnyWebViewInstalled)));
            try {
                PackageInfo preferredWebViewPackage = findPreferredWebViewPackage();
                pw.println(String.format("  Preferred WebView package (name, version): (%s, %s)", preferredWebViewPackage.packageName, preferredWebViewPackage.versionName));
            } catch (WebViewPackageMissingException e) {
                pw.println(String.format("  Preferred WebView package: none", new Object[0]));
            }
            dumpAllPackageInformationLocked(pw);
        }
    }

    private void dumpAllPackageInformationLocked(PrintWriter pw) {
        WebViewProviderInfo[] allProviders = this.mSystemInterface.getWebViewPackages();
        pw.println("  WebView packages:");
        for (WebViewProviderInfo provider : allProviders) {
            List<UserPackage> userPackages = this.mSystemInterface.getPackageInfoForProviderAllUsers(this.mContext, provider);
            PackageInfo systemUserPackageInfo = userPackages.get(0).getPackageInfo();
            if (systemUserPackageInfo == null) {
                pw.println(String.format("    %s is NOT installed.", provider.packageName));
            } else {
                int validity = validityResult(provider, systemUserPackageInfo);
                String packageDetails = String.format("versionName: %s, versionCode: %d, targetSdkVersion: %d", systemUserPackageInfo.versionName, Long.valueOf(systemUserPackageInfo.getLongVersionCode()), Integer.valueOf(systemUserPackageInfo.applicationInfo.targetSdkVersion));
                if (validity == 0) {
                    boolean installedForAllUsers = isInstalledAndEnabledForAllUsers(this.mSystemInterface.getPackageInfoForProviderAllUsers(this.mContext, provider));
                    Object[] objArr = new Object[3];
                    objArr[0] = systemUserPackageInfo.packageName;
                    objArr[1] = packageDetails;
                    objArr[2] = installedForAllUsers ? "" : "NOT";
                    pw.println(String.format("    Valid package %s (%s) is %s installed/enabled for all users", objArr));
                } else {
                    pw.println(String.format("    Invalid package %s (%s), reason: %s", systemUserPackageInfo.packageName, packageDetails, getInvalidityReason(validity)));
                }
            }
        }
    }

    private static String getInvalidityReason(int invalidityReason) {
        if (invalidityReason != 1) {
            if (invalidityReason != 2) {
                if (invalidityReason != 3) {
                    if (invalidityReason == 4) {
                        return "No WebView-library manifest flag";
                    }
                    return "Unexcepted validity-reason";
                }
                return "Incorrect signature";
            }
            return "Version code too low";
        }
        return "SDK version too low";
    }
}