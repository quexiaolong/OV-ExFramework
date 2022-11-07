package com.android.server.display;

import android.app.Presentation;
import android.content.Context;
import android.content.DialogInterface;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoCastDisplayUtil {
    private static final String DISABLE = "0";
    private static final String ENABLE = "1";
    private static final String KEY_VIVO_WFD_PRIVACY = "com.vivo.wifidisplay.privacy.enable";
    private static final String TAG = "CastDisplay";
    private static final int TRANSACTION_SYNC_WFD_PRIVACY_MODE = 32000;
    private static final int TRANSACTION_SYNC_WFD_PWD_MODE = 33000;
    private Context mContext;
    private final DisplayManager mDisplayService;
    private Handler mHandler;
    public Handler mUiHandler;
    public static boolean DEBUG_WFD_UTIL = "yes".equals(SystemProperties.get("persist.sys.log.ctrl", "no"));
    private static volatile VivoCastDisplayUtil sVivoCastDisplayUtil = null;
    private static boolean mInVirtualMode = false;
    private boolean bIsWfdPrivacyMode = false;
    private boolean mIsPasswordMode = false;
    private final SparseArray<PasswordPresentation> mPresentations = new SparseArray<>();
    private int mInValidateDisplayId = -1;
    private boolean mWifiDisplay = false;
    private int mWifiValidateTime = 0;
    private final Runnable mInValidateRunnable = new Runnable() { // from class: com.android.server.display.VivoCastDisplayUtil.2
        @Override // java.lang.Runnable
        public void run() {
            if (VivoCastDisplayUtil.this.mInValidateDisplayId != -1) {
                PasswordPresentation presentation = (PasswordPresentation) VivoCastDisplayUtil.this.mPresentations.get(VivoCastDisplayUtil.this.mInValidateDisplayId);
                if (presentation != null) {
                    presentation.invalidate();
                }
                if (VivoCastDisplayUtil.DEBUG_WFD_UTIL) {
                    VSlog.d(VivoCastDisplayUtil.TAG, "handleMessage MSG_POST_INVALIDATE displayId:" + VivoCastDisplayUtil.this.mInValidateDisplayId);
                }
                if (!VivoCastDisplayUtil.this.mWifiDisplay || VivoCastDisplayUtil.this.mWifiValidateTime <= 0) {
                    if (!VivoCastDisplayUtil.this.mWifiDisplay) {
                        VivoCastDisplayUtil.this.mUiHandler.postDelayed(VivoCastDisplayUtil.this.mInValidateRunnable, 10000L);
                        return;
                    }
                    return;
                }
                VivoCastDisplayUtil.this.mUiHandler.postDelayed(VivoCastDisplayUtil.this.mInValidateRunnable, 500L);
                VivoCastDisplayUtil.access$710(VivoCastDisplayUtil.this);
            }
        }
    };

    static /* synthetic */ int access$710(VivoCastDisplayUtil x0) {
        int i = x0.mWifiValidateTime;
        x0.mWifiValidateTime = i - 1;
        return i;
    }

    public static VivoCastDisplayUtil getInstance(Context context, Handler handler, Handler uiHandler) {
        if (sVivoCastDisplayUtil == null) {
            synchronized (VivoCastDisplayUtil.class) {
                if (sVivoCastDisplayUtil == null) {
                    sVivoCastDisplayUtil = new VivoCastDisplayUtil(context, handler, uiHandler);
                }
            }
        }
        return sVivoCastDisplayUtil;
    }

    public static VivoCastDisplayUtil getInstance() {
        return sVivoCastDisplayUtil;
    }

    private VivoCastDisplayUtil(Context context, Handler handler, Handler uiHandler) {
        this.mHandler = null;
        this.mUiHandler = null;
        this.mContext = context;
        this.mHandler = handler;
        this.mDisplayService = (DisplayManager) context.getSystemService(DisplayManager.class);
        this.mUiHandler = uiHandler;
        Handler handler2 = this.mHandler;
        if (handler2 != null) {
            handler2.post(new Runnable() { // from class: com.android.server.display.VivoCastDisplayUtil.1
                @Override // java.lang.Runnable
                public void run() {
                    VivoCastDisplayUtil.this.initPrivacyMode();
                    VivoCastDisplayUtil.this.registerObserver();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void initPrivacyMode() {
        boolean isPrivacy;
        String strValue = getSettings(KEY_VIVO_WFD_PRIVACY);
        if (TextUtils.isEmpty(strValue)) {
            isPrivacy = false;
        } else {
            isPrivacy = "1".equals(strValue);
        }
        syncWfdPrivacyMode(isPrivacy);
        SystemProperties.set("sys.castdisplay.support", "yes");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void registerObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(KEY_VIVO_WFD_PRIVACY), false, new SettingsDBObserver(null));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class SettingsDBObserver extends ContentObserver {
        public SettingsDBObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            VSlog.i(VivoCastDisplayUtil.TAG, "onChange SettingsDBObserver -- wfddemo " + uri);
            boolean isEnable = "1".equals(VivoCastDisplayUtil.this.getSettings(VivoCastDisplayUtil.KEY_VIVO_WFD_PRIVACY));
            VivoCastDisplayUtil.this.syncWfdPrivacyMode(isEnable);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getSettings(String key) {
        return Settings.Secure.getString(this.mContext.getContentResolver(), key);
    }

    private void putSettings(String key, String value) {
        Settings.Secure.putString(this.mContext.getContentResolver(), key, value);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void syncWfdPrivacyMode(boolean value) {
        Parcel data = null;
        try {
            try {
                long startTime = System.currentTimeMillis();
                IBinder surfaceClient = ServiceManager.getService("SurfaceFlinger");
                data = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                data.writeInt(value ? 1 : 0);
                try {
                    surfaceClient.transact(TRANSACTION_SYNC_WFD_PRIVACY_MODE, data, null, 0);
                } catch (Exception ee) {
                    VSlog.e(TAG, "syncWfdPrivacyMode transact " + ee + " " + ee.getStackTrace().toString());
                }
                this.bIsWfdPrivacyMode = value;
                long endTime = System.currentTimeMillis();
                if (DEBUG_WFD_UTIL) {
                    VSlog.i(TAG, "syncWfdPrivacyMode success, value: " + value + ", cost time: " + (endTime - startTime));
                }
            } catch (Exception e) {
                VSlog.e(TAG, "syncWfdPrivacyMode exception " + e + " " + e.getStackTrace().toString());
            }
        } finally {
            data.recycle();
        }
    }

    public static void setVirtualMode(boolean mode) {
        mInVirtualMode = mode;
    }

    public void updatePasswordModeIfNeed(final boolean pwdMode) {
        if (this.mUiHandler == null || !mInVirtualMode) {
            return;
        }
        if (DEBUG_WFD_UTIL) {
            VSlog.d(TAG, "updatePasswordMode " + pwdMode);
        }
        if (pwdMode != this.mIsPasswordMode) {
            this.mUiHandler.removeCallbacks(this.mInValidateRunnable);
            this.mInValidateDisplayId = -1;
            this.mWifiDisplay = false;
            this.mUiHandler.post(new Runnable() { // from class: com.android.server.display.VivoCastDisplayUtil.3
                @Override // java.lang.Runnable
                public void run() {
                    if (pwdMode) {
                        Display[] displays = VivoCastDisplayUtil.this.mDisplayService.getDisplays("android.hardware.display.category.PRESENTATION");
                        for (Display display : displays) {
                            if (display.getType() == 3) {
                                VivoCastDisplayUtil.this.mInValidateDisplayId = display.getDisplayId();
                                if (VivoCastDisplayUtil.DEBUG_WFD_UTIL) {
                                    VSlog.d(VivoCastDisplayUtil.TAG, "showPresentation Wifi displayId:" + VivoCastDisplayUtil.this.mInValidateDisplayId + " display:" + display);
                                }
                                VivoCastDisplayUtil.this.showPresentation(display);
                                VivoCastDisplayUtil.this.mWifiDisplay = true;
                                VivoCastDisplayUtil.this.mWifiValidateTime = 20;
                                VivoCastDisplayUtil.this.mUiHandler.postDelayed(VivoCastDisplayUtil.this.mInValidateRunnable, 300L);
                            } else if ("com.vivo.upnpserver".equals(display.getOwnerPackageName())) {
                                VivoCastDisplayUtil.this.mInValidateDisplayId = display.getDisplayId();
                                if (VivoCastDisplayUtil.DEBUG_WFD_UTIL) {
                                    VSlog.d(VivoCastDisplayUtil.TAG, "showPresentation Upnp displayId:" + VivoCastDisplayUtil.this.mInValidateDisplayId + " display:" + display);
                                }
                                VivoCastDisplayUtil.this.showPresentation(display);
                                VivoCastDisplayUtil.this.mUiHandler.postDelayed(VivoCastDisplayUtil.this.mInValidateRunnable, 300L);
                            }
                        }
                    } else {
                        for (int i = VivoCastDisplayUtil.this.mPresentations.size() - 1; i >= 0; i--) {
                            VivoCastDisplayUtil.this.mPresentations.keyAt(i);
                            ((PasswordPresentation) VivoCastDisplayUtil.this.mPresentations.valueAt(i)).dismiss();
                        }
                        VivoCastDisplayUtil.this.mPresentations.clear();
                    }
                    VivoCastDisplayUtil.this.mIsPasswordMode = pwdMode;
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean showPresentation(Display display) {
        final int displayId = display.getDisplayId();
        if (this.mPresentations.get(displayId) == null) {
            PasswordPresentation presentation = new PasswordPresentation(this.mContext, display);
            presentation.setOnDismissListener(new DialogInterface.OnDismissListener() { // from class: com.android.server.display.-$$Lambda$VivoCastDisplayUtil$n6u4jiCfb0_frgZcR62ZHe3HVY0
                @Override // android.content.DialogInterface.OnDismissListener
                public final void onDismiss(DialogInterface dialogInterface) {
                    VivoCastDisplayUtil.this.lambda$showPresentation$0$VivoCastDisplayUtil(displayId, dialogInterface);
                }
            });
            try {
                presentation.show();
            } catch (WindowManager.InvalidDisplayException ex) {
                VSlog.w(TAG, "Invalid display:", ex);
                presentation = null;
            }
            if (presentation != null) {
                this.mPresentations.append(displayId, presentation);
                return true;
            }
            return false;
        }
        return false;
    }

    public /* synthetic */ void lambda$showPresentation$0$VivoCastDisplayUtil(int displayId, DialogInterface dialog) {
        if (this.mPresentations.get(displayId) != null) {
            this.mPresentations.remove(displayId);
        }
    }

    private void syncPasswordMode(boolean value) {
        Parcel data = null;
        try {
            try {
                long startTime = System.currentTimeMillis();
                IBinder surfaceClient = ServiceManager.getService("SurfaceFlinger");
                data = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                data.writeInt(value ? 1 : 0);
                surfaceClient.transact(TRANSACTION_SYNC_WFD_PWD_MODE, data, null, 0);
                this.mIsPasswordMode = value;
                long endTime = System.currentTimeMillis();
                if (DEBUG_WFD_UTIL) {
                    VSlog.i(TAG, "syncPasswordMode success, value: " + value + ", cost time: " + (endTime - startTime));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } finally {
            data.recycle();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class PasswordPresentation extends Presentation {
        private View mRootView;

        PasswordPresentation(Context context, Display display) {
            super(context, display);
            getWindow().setType(2009);
            setCancelable(false);
        }

        public void invalidate() {
            this.mRootView.postInvalidate();
        }

        @Override // android.app.Dialog
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setTitle("PasswordPresentation");
            View inflate = LayoutInflater.from(getContext()).inflate(50528261, (ViewGroup) null);
            this.mRootView = inflate;
            setContentView(inflate);
            TextView tv = (TextView) findViewById(51183801);
            if (tv != null) {
                tv.setNightMode(0);
            }
        }
    }
}