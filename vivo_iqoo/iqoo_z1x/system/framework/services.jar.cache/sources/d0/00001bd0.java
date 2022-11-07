package com.android.server.webkit;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.XmlResourceParser;
import android.os.Build;
import android.os.RemoteException;
import android.os.UserManager;
import android.provider.Settings;
import android.util.AndroidRuntimeException;
import android.util.Log;
import android.webkit.UserPackage;
import android.webkit.WebViewFactory;
import android.webkit.WebViewProviderInfo;
import android.webkit.WebViewZygote;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;

/* loaded from: classes2.dex */
public class SystemImpl implements SystemInterface {
    private static final int PACKAGE_FLAGS = 272630976;
    private static final String TAG = SystemImpl.class.getSimpleName();
    private static final String TAG_AVAILABILITY = "availableByDefault";
    private static final String TAG_DESCRIPTION = "description";
    private static final String TAG_FALLBACK = "isFallback";
    private static final String TAG_PACKAGE_NAME = "packageName";
    private static final String TAG_SIGNATURE = "signature";
    private static final String TAG_START = "webviewproviders";
    private static final String TAG_WEBVIEW_PROVIDER = "webviewprovider";
    private final WebViewProviderInfo[] mWebViewProviderPackages;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class LazyHolder {
        private static final SystemImpl INSTANCE = new SystemImpl();

        private LazyHolder() {
        }
    }

    public static SystemImpl getInstance() {
        return LazyHolder.INSTANCE;
    }

    private SystemImpl() {
        int numFallbackPackages = 0;
        int numAvailableByDefaultPackages = 0;
        XmlResourceParser parser = null;
        List<WebViewProviderInfo> webViewProviders = new ArrayList<>();
        try {
            try {
                parser = AppGlobals.getInitialApplication().getResources().getXml(18284552);
                XmlUtils.beginDocument(parser, TAG_START);
                while (true) {
                    XmlUtils.nextElement(parser);
                    String element = parser.getName();
                    if (element != null) {
                        if (element.equals(TAG_WEBVIEW_PROVIDER)) {
                            String packageName = parser.getAttributeValue(null, TAG_PACKAGE_NAME);
                            if (packageName == null) {
                                throw new AndroidRuntimeException("WebView provider in framework resources missing package name");
                            }
                            String description = parser.getAttributeValue(null, TAG_DESCRIPTION);
                            if (description == null) {
                                throw new AndroidRuntimeException("WebView provider in framework resources missing description");
                            }
                            boolean availableByDefault = "true".equals(parser.getAttributeValue(null, TAG_AVAILABILITY));
                            boolean isFallback = "true".equals(parser.getAttributeValue(null, TAG_FALLBACK));
                            WebViewProviderInfo currentProvider = new WebViewProviderInfo(packageName, description, availableByDefault, isFallback, readSignatures(parser));
                            if (currentProvider.isFallback) {
                                numFallbackPackages++;
                                if (!currentProvider.availableByDefault) {
                                    throw new AndroidRuntimeException("Each WebView fallback package must be available by default.");
                                }
                                if (numFallbackPackages > 1) {
                                    throw new AndroidRuntimeException("There can be at most one WebView fallback package.");
                                }
                            }
                            numAvailableByDefaultPackages = currentProvider.availableByDefault ? numAvailableByDefaultPackages + 1 : numAvailableByDefaultPackages;
                            webViewProviders.add(currentProvider);
                        } else {
                            Log.e(TAG, "Found an element that is not a WebView provider");
                        }
                    } else if (numAvailableByDefaultPackages == 0) {
                        throw new AndroidRuntimeException("There must be at least one WebView package that is available by default");
                    } else {
                        this.mWebViewProviderPackages = (WebViewProviderInfo[]) webViewProviders.toArray(new WebViewProviderInfo[webViewProviders.size()]);
                        return;
                    }
                }
            } catch (IOException | XmlPullParserException e) {
                throw new AndroidRuntimeException("Error when parsing WebView config " + e);
            }
        } finally {
            if (parser != null) {
                parser.close();
            }
        }
    }

    @Override // com.android.server.webkit.SystemInterface
    public WebViewProviderInfo[] getWebViewPackages() {
        return this.mWebViewProviderPackages;
    }

    @Override // com.android.server.webkit.SystemInterface
    public long getFactoryPackageVersion(String packageName) throws PackageManager.NameNotFoundException {
        PackageManager pm = AppGlobals.getInitialApplication().getPackageManager();
        return pm.getPackageInfo(packageName, 2097152).getLongVersionCode();
    }

    private static String[] readSignatures(XmlResourceParser parser) throws IOException, XmlPullParserException {
        List<String> signatures = new ArrayList<>();
        int outerDepth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            if (parser.getName().equals(TAG_SIGNATURE)) {
                String signature = parser.nextText();
                signatures.add(signature);
            } else {
                Log.e(TAG, "Found an element in a webview provider that is not a signature");
            }
        }
        return (String[]) signatures.toArray(new String[signatures.size()]);
    }

    @Override // com.android.server.webkit.SystemInterface
    public int onWebViewProviderChanged(PackageInfo packageInfo) {
        return WebViewFactory.onWebViewProviderChanged(packageInfo);
    }

    @Override // com.android.server.webkit.SystemInterface
    public String getUserChosenWebViewProvider(Context context) {
        return Settings.Global.getString(context.getContentResolver(), "webview_provider");
    }

    @Override // com.android.server.webkit.SystemInterface
    public void updateUserSetting(Context context, String newProviderName) {
        Settings.Global.putString(context.getContentResolver(), "webview_provider", newProviderName == null ? "" : newProviderName);
    }

    @Override // com.android.server.webkit.SystemInterface
    public void killPackageDependents(String packageName) {
        try {
            ActivityManager.getService().killPackageDependents(packageName, -1);
        } catch (RemoteException e) {
        }
    }

    @Override // com.android.server.webkit.SystemInterface
    public boolean isFallbackLogicEnabled() {
        return Settings.Global.getInt(AppGlobals.getInitialApplication().getContentResolver(), "webview_fallback_logic_enabled", 1) == 1;
    }

    @Override // com.android.server.webkit.SystemInterface
    public void enableFallbackLogic(boolean enable) {
        Settings.Global.putInt(AppGlobals.getInitialApplication().getContentResolver(), "webview_fallback_logic_enabled", enable ? 1 : 0);
    }

    @Override // com.android.server.webkit.SystemInterface
    public void enablePackageForAllUsers(Context context, String packageName, boolean enable) {
        UserManager userManager = (UserManager) context.getSystemService("user");
        for (UserInfo userInfo : userManager.getUsers()) {
            enablePackageForUser(packageName, enable, userInfo.id);
        }
    }

    private void enablePackageForUser(String packageName, boolean enable, int userId) {
        try {
            AppGlobals.getPackageManager().setApplicationEnabledSetting(packageName, enable ? 0 : 3, 0, userId, (String) null);
        } catch (RemoteException | IllegalArgumentException e) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("Tried to ");
            sb.append(enable ? "enable " : "disable ");
            sb.append(packageName);
            sb.append(" for user ");
            sb.append(userId);
            sb.append(": ");
            sb.append(e);
            Log.w(str, sb.toString());
        }
    }

    @Override // com.android.server.webkit.SystemInterface
    public boolean systemIsDebuggable() {
        return Build.IS_DEBUGGABLE;
    }

    @Override // com.android.server.webkit.SystemInterface
    public PackageInfo getPackageInfoForProvider(WebViewProviderInfo configInfo) throws PackageManager.NameNotFoundException {
        PackageManager pm = AppGlobals.getInitialApplication().getPackageManager();
        return pm.getPackageInfo(configInfo.packageName, PACKAGE_FLAGS);
    }

    @Override // com.android.server.webkit.SystemInterface
    public List<UserPackage> getPackageInfoForProviderAllUsers(Context context, WebViewProviderInfo configInfo) {
        return UserPackage.getPackageInfosAllUsers(context, configInfo.packageName, (int) PACKAGE_FLAGS);
    }

    @Override // com.android.server.webkit.SystemInterface
    public int getMultiProcessSetting(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), "webview_multiprocess", 0);
    }

    @Override // com.android.server.webkit.SystemInterface
    public void setMultiProcessSetting(Context context, int value) {
        Settings.Global.putInt(context.getContentResolver(), "webview_multiprocess", value);
    }

    @Override // com.android.server.webkit.SystemInterface
    public void notifyZygote(boolean enableMultiProcess) {
        WebViewZygote.setMultiprocessEnabled(enableMultiProcess);
    }

    @Override // com.android.server.webkit.SystemInterface
    public void ensureZygoteStarted() {
        WebViewZygote.getProcess();
    }

    @Override // com.android.server.webkit.SystemInterface
    public boolean isMultiProcessDefaultEnabled() {
        return true;
    }
}