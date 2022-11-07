package com.android.server;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManagerInternal;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.hardware.graphics.common.V1_0.Dataspace;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IVold;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.vivo.services.security.server.VivoPermissionUtils;
import java.lang.reflect.Method;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoStorageMgrServiceImpl implements IVivoStorageMgrService {
    private static final String ACTION_FBE_WARN_NOTICE = "com.vivo.fbe.intent.action.WARN_NOTICE";
    private static final String FBE_WARN_NF_TAG1 = "fbe_warn_notificate";
    private static final int FBE_WARN_NOTIFICATE_ID = 101021;
    private static final String TAG = "StorageMgrImpl";
    private static final String TAG1 = "FBEWarnNotice";
    private static Method sCheckFileExistsFunc = null;
    private static boolean sIsCheckFileExistsFuncLoaded = false;
    private Context mContext;
    private Handler mHandler;
    private Object mLock = new Object();
    private boolean mNoticeShowed = false;

    public VivoStorageMgrServiceImpl(StorageManagerService service, Context context, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
    }

    public void dummy() {
    }

    public void showFBEWarnNotification(int userId) {
        showFBEWarnNotification(true, this.mContext, userId, this.mHandler);
    }

    public void cancelFBEWarnNotification(int userId) {
        cancelFBEWarnNotification(true, this.mContext, userId, this.mHandler);
    }

    private void showFBEWarnNotification(final boolean chatty, final Context context, final int userId, Handler handler) {
        if (handler == null) {
            VSlog.w(TAG1, "no handler, skip.");
        } else if (userId != 0) {
            if (chatty) {
                VSlog.i(TAG1, "not show notice for " + userId);
            }
        } else {
            handler.post(new Runnable() { // from class: com.android.server.VivoStorageMgrServiceImpl.1
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        if (chatty) {
                            VSlog.d(VivoStorageMgrServiceImpl.TAG1, "Go to show fbe notice..");
                        }
                        VivoStorageMgrServiceImpl.this.showFBEWarnNotificationInner(chatty, context, userId, VivoStorageMgrServiceImpl.FBE_WARN_NOTIFICATE_ID);
                    } catch (Exception e) {
                        VSlog.w(VivoStorageMgrServiceImpl.TAG1, "show notice," + e.toString());
                    }
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showFBEWarnNotificationInner(boolean chatty, Context context, int userId, int noticeId) {
        if (shouldFilter()) {
            VSlog.w(TAG1, "not allow show fbe_notice...");
        } else if (!StorageManager.isFileEncryptedNativeOnly()) {
            VSlog.i(TAG1, "not fbe.");
        } else {
            synchronized (this.mLock) {
                if (!this.mNoticeShowed) {
                    this.mNoticeShowed = true;
                    UserHandle user = getUserHandle(context, userId);
                    Resources r = context.getResources();
                    NotificationManager notificationManager = (NotificationManager) context.getSystemService("notification");
                    hideEncryptionNotification(user, notificationManager);
                    Intent unlockIntent = new Intent(ACTION_FBE_WARN_NOTICE);
                    unlockIntent.setFlags(1073741824);
                    unlockIntent.setPackage(VivoPermissionUtils.OS_PKG);
                    PendingIntent pIntent = PendingIntent.getBroadcast(context, 0, unlockIntent, Dataspace.RANGE_FULL);
                    CharSequence title = r.getText(51249657);
                    CharSequence message = r.getText(51249656);
                    Bundle bundle = new Bundle();
                    bundle.putInt("vivo.summaryIconRes", 50463538);
                    Notification.Builder builder = new Notification.Builder(context, SystemNotificationChannels.SECURITY).setSmallIcon(50463537).setExtras(bundle).setShowWhen(true).setOngoing(true).setTicker(title).setDefaults(0).setColor(context.getColor(17170460)).setContentTitle(title).setPriority(2).setContentText(message).setContentIntent(pIntent).setVisibility(1);
                    Notification notification = builder.build();
                    notificationManager.notifyAsUser(FBE_WARN_NF_TAG1, noticeId, notification, user);
                    VSlog.i(TAG1, "showing fbe_notice " + noticeId + " for " + userId);
                    return;
                }
                VSlog.i(TAG1, "fbe_notice " + noticeId + " was showed before..");
            }
        }
    }

    private void hideEncryptionNotification(UserHandle userHandle, NotificationManager notificationManager) {
        VSlog.i(TAG1, "hide encryption notification, user: " + userHandle + " id:9");
        notificationManager.cancelAsUser(null, 9, userHandle);
    }

    private void cancelFBEWarnNotification(final boolean chatty, final Context context, final int userId, Handler handler) {
        if (handler == null) {
            VSlog.w(TAG1, "no handler, skip.");
        } else if (userId != 0) {
            if (chatty) {
                VSlog.i(TAG1, "no need cancel notice for " + userId);
            }
        } else {
            handler.post(new Runnable() { // from class: com.android.server.VivoStorageMgrServiceImpl.2
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        if (chatty) {
                            VSlog.d(VivoStorageMgrServiceImpl.TAG1, "Go to cancel fbe_notice..");
                        }
                        VivoStorageMgrServiceImpl.this.cancelFBEWarnNotificationInner(context, userId, VivoStorageMgrServiceImpl.FBE_WARN_NOTIFICATE_ID);
                    } catch (Exception e) {
                        VSlog.w(VivoStorageMgrServiceImpl.TAG1, "cancel notice," + e.toString());
                    }
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void cancelFBEWarnNotificationInner(Context context, int userId, int noticeId) {
        if (shouldFilter()) {
            VSlog.w(TAG1, "not allow cancel fbe_notice...");
        } else if (!StorageManager.isFileEncryptedNativeOnly()) {
        } else {
            UserHandle user = getUserHandle(context, userId);
            NotificationManager notificationManager = (NotificationManager) context.getSystemService("notification");
            notificationManager.cancelAsUser(FBE_WARN_NF_TAG1, noticeId, user);
            VSlog.i(TAG1, "cancel fbe_notice " + noticeId + " for " + userId);
        }
    }

    private boolean shouldFilter() {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 1000) {
            VSlog.w(TAG1, "calling from " + callingUid);
            return true;
        }
        return false;
    }

    private UserHandle getUserHandle(Context context, int userId) {
        UserManager userMgr = (UserManager) context.getSystemService("user");
        UserInfo user = userMgr.getUserInfo(userId);
        UserHandle userHandle = user.getUserHandle();
        return userHandle;
    }

    public int BinderService_checkFileExists(String filePath, IVold mVold) {
        int callingUid = Binder.getCallingUid();
        int result = -1;
        if (UserHandle.getAppId(callingUid) != 1000) {
            VSlog.w(TAG, "checkFileExists permission denied.uid == " + callingUid);
        } else {
            try {
                initRefVoldFuns(mVold);
                if (sCheckFileExistsFunc != null) {
                    result = ((Integer) sCheckFileExistsFunc.invoke(mVold, filePath)).intValue();
                } else {
                    VSlog.w(TAG, "checkFileExists is null.");
                }
            } catch (Exception e) {
                VSlog.w(TAG, "catch exception in checkFileExists :" + e);
            }
        }
        VSlog.d(TAG, "checkFileExists filePath:" + filePath + ", result:" + result);
        return result;
    }

    private void initRefVoldFuns(IVold mVold) {
        synchronized (VivoStorageMgrServiceImpl.class) {
            if (!sIsCheckFileExistsFuncLoaded) {
                if (mVold == null) {
                    return;
                }
                try {
                    Method declaredMethod = mVold.getClass().getDeclaredMethod("checkFileExists", String.class);
                    sCheckFileExistsFunc = declaredMethod;
                    declaredMethod.setAccessible(true);
                    sIsCheckFileExistsFuncLoaded = true;
                } catch (Exception e) {
                    VSlog.w(TAG, "init ref fun ex " + e.toString());
                }
            }
            VSlog.i(TAG, "sIsCheckFileExistsFuncLoaded:" + sIsCheckFileExistsFuncLoaded);
        }
    }

    public void BinderService_fixupDir(String path, int uid, int gid, IVold mVold) {
        if (path == null || !path.startsWith("/data/media") || uid < 0 || mVold == null) {
            return;
        }
        int callingUid = Binder.getCallingUid();
        PackageManagerInternal pmInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        AndroidPackage callingPkg = pmInternal.getPackage(callingUid);
        AndroidPackage platform = pmInternal.getPackage(VivoPermissionUtils.OS_PKG);
        if (callingPkg == null || platform == null || callingPkg.getPackageName() == null) {
            return;
        }
        if (!callingPkg.getSigningDetails().signaturesMatchExactly(platform.getSigningDetails())) {
            VSlog.v(TAG, "finxupDir failed, because the calling do not have the platform signatures");
            return;
        }
        try {
            VSlog.v(TAG, "begin to finxupDir, path: " + path + ", callingPkg: " + callingPkg.getPackageName() + ", callingUid: " + callingUid);
            StringBuilder sb = new StringBuilder();
            sb.append(path);
            sb.append("/");
            mVold.fixupPermission(sb.toString(), uid, gid);
        } catch (RemoteException | ServiceSpecificException e) {
            VSlog.e(TAG, "Failed to fixup app dir for " + uid, e);
        }
    }
}