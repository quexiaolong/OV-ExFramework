package com.android.server.wallpaper;

import android.app.WallpaperInfo;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.FtFeature;
import android.util.Slog;
import com.android.internal.app.IBatteryStats;
import com.android.server.wallpaper.WallpaperManagerService;
import com.vivo.face.common.data.Constants;
import com.vivo.face.common.state.FaceUIState;
import com.vivo.services.security.client.VivoPermissionManager;
import java.io.File;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoWallpaperManagerServiceImpl implements IVivoWallpaperManagerService {
    static final String TAG = "VivoWallpaperManagerServiceImpl";
    private boolean mColortoneEnabledSetting;
    private Context mContext;
    int mCount;
    private HandlerThread mHandlerThread;
    int mLastFromTheme;
    WallpaperInfo mLastWi;
    private SettingsObserver mSettingsObserver;
    WallpaperColorToneJudger mWallpaperColorToneJudger;
    private WallpaperManagerService mWallpaperManagerService;
    static final boolean DEBUG = SystemProperties.get("persist.sys.log.ctrl", "no").equals("yes");
    private static final IBinder sFlinger = ServiceManager.getService("SurfaceFlinger");
    private int mNoteUid = -1;
    public Runnable mSafeCheck = new Runnable() { // from class: com.android.server.wallpaper.VivoWallpaperManagerServiceImpl.1
        @Override // java.lang.Runnable
        public void run() {
            int userId = UserHandle.getCallingUserId();
            synchronized (VivoWallpaperManagerServiceImpl.this.mWallpaperManagerService.mLock) {
                WallpaperManagerService.WallpaperData wallpaper = VivoWallpaperManagerServiceImpl.this.mWallpaperManagerService.mLastWallpaper;
                if (wallpaper == null || wallpaper.connection == null || wallpaper.connection.mService == null) {
                    VSlog.w(VivoWallpaperManagerServiceImpl.TAG, "mSafeCheck works!");
                    WallpaperManagerService.WallpaperData fallback = new WallpaperManagerService.WallpaperData(wallpaper != null ? wallpaper.userId : VivoWallpaperManagerServiceImpl.this.mWallpaperManagerService.mCurrentUserId, VivoWallpaperManagerServiceImpl.this.mWallpaperManagerService.getWallpaperDir(userId), "wallpaper_lock_orig", "wallpaper_lock");
                    VivoWallpaperManagerServiceImpl.this.mWallpaperManagerService.ensureSaneWallpaperData(fallback);
                    VivoWallpaperManagerServiceImpl.this.mWallpaperManagerService.bindWallpaperComponentLocked(VivoWallpaperManagerServiceImpl.this.mWallpaperManagerService.mImageWallpaper, true, false, fallback, (IRemoteCallback) null);
                }
            }
        }
    };
    boolean IS_DEBUG = SystemProperties.get("persist.colortone.debug", "false").equals("true");
    String mLastWallpaperPkg = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;

    public VivoWallpaperManagerServiceImpl(WallpaperManagerService wallpaperMgrService, Context context) {
        if (wallpaperMgrService == null) {
            VSlog.i(TAG, "container is " + wallpaperMgrService);
        }
        this.mWallpaperManagerService = wallpaperMgrService;
        this.mContext = context;
        this.mWallpaperColorToneJudger = new WallpaperColorToneJudger();
    }

    public void dummy() {
    }

    public void systemReady() {
        if (FtFeature.isFeatureSupport("vivo.software.colortone")) {
            this.mSettingsObserver = new SettingsObserver(this.mContext.getMainThreadHandler());
            ContentResolver resolver = this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor("pref_replace_icon"), true, this.mSettingsObserver, -1);
            int defValue = getDefValue();
            this.mColortoneEnabledSetting = Settings.System.getIntForUser(resolver, "pref_replace_icon", defValue, -2) != 0;
            if (DEBUG) {
                VSlog.d("colortone", "mColortoneEnabledSetting 001 = " + this.mColortoneEnabledSetting);
            }
        }
    }

    private int getDefValue() {
        int firstSdk = Build.VERSION.FIRST_SDK_INT;
        int curSdk = Build.VERSION.SDK_INT;
        if (firstSdk == curSdk) {
            return 0;
        }
        return 1;
    }

    public void safeCheck(boolean forPost) {
        this.mContext.getMainThreadHandler().removeCallbacks(this.mSafeCheck);
        if (forPost) {
            this.mContext.getMainThreadHandler().postDelayed(this.mSafeCheck, 10000L);
        }
    }

    public void sysWallpaperChanged(WallpaperManagerService.WallpaperData wallpaper) {
        adjustWallpaperWidth(wallpaper, 0);
        judgeColorTone(wallpaper);
    }

    /* loaded from: classes.dex */
    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            synchronized (VivoWallpaperManagerServiceImpl.this.mWallpaperManagerService.mLock) {
                ContentResolver resolver = VivoWallpaperManagerServiceImpl.this.mContext.getContentResolver();
                VivoWallpaperManagerServiceImpl.this.mColortoneEnabledSetting = Settings.System.getIntForUser(resolver, "pref_replace_icon", 0, -2) != 0;
                if (VivoWallpaperManagerServiceImpl.DEBUG) {
                    VSlog.d("colortone", "mColortoneEnabledSetting = " + VivoWallpaperManagerServiceImpl.this.mColortoneEnabledSetting);
                }
                Settings.System.putIntForUser(resolver, "setting_from_theme", VivoWallpaperManagerServiceImpl.this.mLastFromTheme, -2);
                VivoWallpaperManagerServiceImpl.this.judgeColorTone((WallpaperManagerService.WallpaperData) VivoWallpaperManagerServiceImpl.this.mWallpaperManagerService.mWallpaperMap.get(0));
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:47:0x0143 A[Catch: all -> 0x0158, OutOfMemoryError -> 0x015a, TRY_LEAVE, TryCatch #2 {OutOfMemoryError -> 0x015a, blocks: (B:25:0x00ad, B:27:0x00c3, B:29:0x00c7, B:32:0x0117, B:43:0x012d, B:44:0x0131, B:42:0x0129, B:45:0x0132, B:47:0x0143), top: B:84:0x00ad, outer: #4 }] */
    /* JADX WARN: Removed duplicated region for block: B:64:0x0176  */
    /* JADX WARN: Removed duplicated region for block: B:67:0x017f  */
    /* JADX WARN: Removed duplicated region for block: B:70:0x01b7  */
    /* JADX WARN: Removed duplicated region for block: B:79:0x01d2  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public void judgeColorTone(com.android.server.wallpaper.WallpaperManagerService.WallpaperData r18) {
        /*
            Method dump skipped, instructions count: 473
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wallpaper.VivoWallpaperManagerServiceImpl.judgeColorTone(com.android.server.wallpaper.WallpaperManagerService$WallpaperData):void");
    }

    public void updateLastWi(WallpaperInfo lastWi) {
        this.mLastWi = lastWi;
        notifyWallpaperChanged();
    }

    public WallpaperInfo getLastWi() {
        return this.mLastWi;
    }

    public void adjustWallpaperWidth(WallpaperManagerService.WallpaperData wallpaper, int displayId) {
        Bitmap mBitmap = null;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        this.mContext.getDisplay().getRealMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        if (screenWidth > screenHeight) {
            screenWidth = screenHeight;
            screenHeight = screenWidth;
        }
        if (wallpaper == null) {
            return;
        }
        if (0 == 0) {
            String path = this.mWallpaperManagerService.getWallpaperDir(wallpaper.userId).getAbsolutePath() + "/wallpaper_orig";
            File file = new File(path);
            if (!file.exists()) {
                mBitmap = BitmapFactory.decodeResource(this.mContext.getResources(), 17302154);
            } else {
                mBitmap = BitmapFactory.decodeFile(path);
            }
        }
        if (mBitmap == null) {
            return;
        }
        int bmpWidth = mBitmap.getWidth();
        int bmpHeight = mBitmap.getHeight();
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mBitmap.recycle();
        }
        if (bmpHeight == screenHeight && bmpWidth == screenWidth) {
            setWallpaperPortraitFlag(0);
            VSlog.w(TAG, "Wallpaper width equals screen width!!!");
            return;
        }
        setWallpaperPortraitFlag(1);
    }

    private void setWallpaperPortraitFlag(int portrait) {
        long origId = Binder.clearCallingIdentity();
        try {
            Settings.System.putInt(this.mContext.getContentResolver(), "portrait", portrait);
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void loadSettingsForWpNullCmp(WallpaperManagerService.WallpaperData wallpaper, ComponentName defaultCmp) {
        if (DEBUG) {
            VSlog.d(TAG, "nextWallpaperComponent get form parser = " + wallpaper.nextWallpaperComponent + " mDefaultWallpaperComponent = " + defaultCmp);
        }
        if ((wallpaper.wallpaperFile != null && wallpaper.wallpaperFile.exists()) || defaultCmp == null) {
            wallpaper.nextWallpaperComponent = this.mWallpaperManagerService.mImageWallpaper;
        }
    }

    public boolean judgeShouldBeSuccessForSettingsRestored(WallpaperManagerService.WallpaperData wallpaper, ComponentName defaultCmp) {
        return wallpaper.nextWallpaperComponent == null && defaultCmp != null;
    }

    private HandlerThread getThreadForConnect() {
        if (this.mHandlerThread == null) {
            HandlerThread handlerThread = new HandlerThread("WallpaperMSThread");
            this.mHandlerThread = handlerThread;
            handlerThread.start();
        }
        return this.mHandlerThread;
    }

    public boolean bindServiceWithHandler(Intent intent, WallpaperManagerService.WallpaperConnection connection, int serviceUserId) {
        return this.mContext.bindServiceAsUser(intent, connection, 570429441, getThreadForConnect().getThreadHandler(), new UserHandle(serviceUserId));
    }

    public boolean checkVivoSetWallPermissionPermission() {
        return VivoPermissionManager.checkCallingVivoPermission("android.permission.SET_WALLPAPER");
    }

    public void noteWallpaper(int userId) {
        WallpaperInfo wallpaperInfo;
        int noteUid = 0;
        try {
            WallpaperManagerService.WallpaperData bwallpaper = (WallpaperManagerService.WallpaperData) this.mWallpaperManagerService.mWallpaperMap.get(userId);
            if (bwallpaper != null && bwallpaper.connection != null && (wallpaperInfo = bwallpaper.connection.mInfo) != null && wallpaperInfo.getServiceInfo() != null) {
                noteUid = wallpaperInfo.getServiceInfo().applicationInfo.uid;
            }
            if (this.mNoteUid != noteUid) {
                this.mNoteUid = noteUid;
                IBatteryStats mBatteryStats = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));
                if (mBatteryStats != null) {
                    try {
                        mBatteryStats.notePem(18, noteUid, 0);
                    } catch (RemoteException e) {
                        VSlog.e(TAG, "notePem:", e);
                    }
                }
            }
        } catch (Exception e2) {
            VSlog.e(TAG, "notePem:", e2);
        }
    }

    private void notifyWallpaperChanged() {
        String newWallpaperPkg = FaceUIState.PKG_SYSTEMUI;
        WallpaperInfo wallpaperInfo = this.mLastWi;
        if (wallpaperInfo != null) {
            newWallpaperPkg = wallpaperInfo.getPackageName();
        }
        if (newWallpaperPkg != null && !newWallpaperPkg.equals(this.mLastWallpaperPkg)) {
            this.mLastWallpaperPkg = newWallpaperPkg;
            if (sFlinger != null) {
                Parcel data = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                data.writeString(newWallpaperPkg);
                try {
                    try {
                        sFlinger.transact(31503, data, null, 0);
                    } catch (Exception ex) {
                        Slog.e(TAG, "Failed to notifyWallpaperChanged", ex);
                    }
                } finally {
                    data.recycle();
                }
            }
        }
    }
}