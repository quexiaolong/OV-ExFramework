package com.android.server.media;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.os.Handler;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class MediaButtonReceiverHolder {
    private static final String COMPONENT_NAME_USER_ID_DELIM = ",";
    public static final int COMPONENT_TYPE_ACTIVITY = 2;
    public static final int COMPONENT_TYPE_BROADCAST = 1;
    public static final int COMPONENT_TYPE_INVALID = 0;
    public static final int COMPONENT_TYPE_SERVICE = 3;
    private static final boolean DEBUG_KEY_EVENT = true;
    private static final String TAG = "PendingIntentHolder";
    private boolean isInPackageList;
    private final ComponentName mComponentName;
    private final int mComponentType;
    private boolean mNeedChangeCode;
    private int mNewCode;
    private final String mPackageName;
    private final PendingIntent mPendingIntent;
    private final int mUserId;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface ComponentType {
    }

    public static MediaButtonReceiverHolder unflattenFromString(Context context, String mediaButtonReceiverInfo) {
        String[] tokens;
        ComponentName componentName;
        int componentType;
        if (TextUtils.isEmpty(mediaButtonReceiverInfo) || (tokens = mediaButtonReceiverInfo.split(COMPONENT_NAME_USER_ID_DELIM)) == null || ((tokens.length != 2 && tokens.length != 3) || (componentName = ComponentName.unflattenFromString(tokens[0])) == null)) {
            return null;
        }
        int userId = Integer.parseInt(tokens[1]);
        if (tokens.length == 3) {
            componentType = Integer.parseInt(tokens[2]);
        } else {
            componentType = getComponentType(context, componentName);
        }
        return new MediaButtonReceiverHolder(userId, null, componentName, componentType);
    }

    public static MediaButtonReceiverHolder create(Context context, int userId, PendingIntent pendingIntent) {
        ComponentName componentName;
        String packageName = null;
        if (pendingIntent == null) {
            return null;
        }
        if (pendingIntent == null || pendingIntent.getIntent() == null) {
            componentName = null;
        } else {
            componentName = pendingIntent.getIntent().getComponent();
        }
        if (componentName != null) {
            return new MediaButtonReceiverHolder(userId, pendingIntent, componentName, getComponentType(context, componentName));
        }
        PackageManager pm = context.getPackageManager();
        Intent intent = pendingIntent.getIntent();
        ComponentName componentName2 = resolveImplicitServiceIntent(pm, intent);
        if (componentName2 != null) {
            return new MediaButtonReceiverHolder(userId, pendingIntent, componentName2, 3);
        }
        ComponentName componentName3 = resolveManifestDeclaredBroadcastReceiverIntent(pm, intent);
        if (componentName3 != null) {
            return new MediaButtonReceiverHolder(userId, pendingIntent, componentName3, 1);
        }
        ComponentName componentName4 = resolveImplicitActivityIntent(pm, intent);
        if (componentName4 != null) {
            return new MediaButtonReceiverHolder(userId, pendingIntent, componentName4, 2);
        }
        Log.w(TAG, "Unresolvable implicit intent is set, pi=" + pendingIntent);
        if (pendingIntent != null && pendingIntent.getIntent() != null) {
            packageName = pendingIntent.getIntent().getPackage();
        }
        return new MediaButtonReceiverHolder(userId, pendingIntent, packageName != null ? packageName : "");
    }

    private MediaButtonReceiverHolder(int userId, PendingIntent pendingIntent, ComponentName componentName, int componentType) {
        this.mNeedChangeCode = false;
        this.mNewCode = -1;
        this.isInPackageList = false;
        this.mUserId = userId;
        this.mPendingIntent = pendingIntent;
        this.mComponentName = componentName;
        this.mPackageName = componentName.getPackageName();
        this.mComponentType = componentType;
    }

    private MediaButtonReceiverHolder(int userId, PendingIntent pendingIntent, String packageName) {
        this.mNeedChangeCode = false;
        this.mNewCode = -1;
        this.isInPackageList = false;
        this.mUserId = userId;
        this.mPendingIntent = pendingIntent;
        this.mComponentName = null;
        this.mPackageName = packageName;
        this.mComponentType = 0;
    }

    public int getUserId() {
        return this.mUserId;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public void changeKeyCode(Context context, KeyEvent keyEvent, int keyCode) {
        if (keyCode > 0) {
            this.mNeedChangeCode = true;
            this.mNewCode = keyCode;
        }
    }

    public void setPackageList(boolean enable) {
        this.isInPackageList = enable;
    }

    public boolean send(Context context, KeyEvent keyEvent, String callingPackageName, int resultCode, PendingIntent.OnFinished onFinishedListener, Handler handler) {
        Intent mediaButtonIntent = new Intent("android.intent.action.MEDIA_BUTTON");
        mediaButtonIntent.addFlags(AudioFormat.EVRC);
        mediaButtonIntent.putExtra("android.intent.extra.KEY_EVENT", keyEvent);
        mediaButtonIntent.putExtra("android.intent.extra.PACKAGE_NAME", callingPackageName);
        if (this.mNeedChangeCode) {
            mediaButtonIntent.putExtra("android.intent.extra.KEY_EVENT", KeyEvent.changeKeyCode(this.mNewCode, keyEvent));
            this.mNeedChangeCode = false;
            this.mNewCode = -1;
        }
        if (this.mPendingIntent != null) {
            Log.d(TAG, "Sending " + keyEvent + " to the last known PendingIntent " + this.mPendingIntent);
            try {
                this.mPendingIntent.send(context, resultCode, mediaButtonIntent, onFinishedListener, handler);
                return true;
            } catch (PendingIntent.CanceledException e) {
                Log.w(TAG, "Error sending key event to media button receiver " + this.mPendingIntent, e);
                return false;
            }
        } else if (this.mComponentName != null && this.isInPackageList) {
            Log.d(TAG, "Sending " + keyEvent + " to the restored intent " + this.mComponentName + ", type=" + this.mComponentType);
            mediaButtonIntent.setComponent(this.mComponentName);
            UserHandle userHandle = UserHandle.of(this.mUserId);
            try {
                int i = this.mComponentType;
                if (i == 2) {
                    context.startActivityAsUser(mediaButtonIntent, userHandle);
                } else if (i == 3) {
                    context.startForegroundServiceAsUser(mediaButtonIntent, userHandle);
                } else {
                    context.sendBroadcastAsUser(mediaButtonIntent, userHandle);
                }
                return true;
            } catch (Exception e2) {
                Log.w(TAG, "Error sending media button to the restored intent " + this.mComponentName + ", type=" + this.mComponentType, e2);
                return false;
            }
        } else {
            Log.e(TAG, "Shouldn't be happen -- pending intent or component name must be set");
            return false;
        }
    }

    public String toString() {
        if (this.mPendingIntent != null) {
            return "MBR {pi=" + this.mPendingIntent + ", type=" + this.mComponentType + "}";
        }
        return "Restored MBR {component=" + this.mComponentName + ", type=" + this.mComponentType + "}";
    }

    public String flattenToString() {
        ComponentName componentName = this.mComponentName;
        if (componentName == null) {
            return "";
        }
        return String.join(COMPONENT_NAME_USER_ID_DELIM, componentName.flattenToString(), String.valueOf(this.mUserId), String.valueOf(this.mComponentType));
    }

    private static int getComponentType(Context context, ComponentName componentName) {
        if (componentName == null) {
            return 0;
        }
        PackageManager pm = context.getPackageManager();
        try {
            ActivityInfo activityInfo = pm.getActivityInfo(componentName, 786433);
            if (activityInfo != null) {
                return 2;
            }
        } catch (PackageManager.NameNotFoundException e) {
        }
        try {
            ServiceInfo serviceInfo = pm.getServiceInfo(componentName, 786436);
            if (serviceInfo != null) {
                return 3;
            }
            return 1;
        } catch (PackageManager.NameNotFoundException e2) {
            return 1;
        }
    }

    private static ComponentName resolveImplicitServiceIntent(PackageManager pm, Intent intent) {
        return createComponentName(pm.resolveService(intent, 786436));
    }

    private static ComponentName resolveManifestDeclaredBroadcastReceiverIntent(PackageManager pm, Intent intent) {
        List<ResolveInfo> resolveInfos = pm.queryBroadcastReceivers(intent, 786432);
        if (resolveInfos == null || resolveInfos.isEmpty()) {
            return null;
        }
        return createComponentName(resolveInfos.get(0));
    }

    private static ComponentName resolveImplicitActivityIntent(PackageManager pm, Intent intent) {
        return createComponentName(pm.resolveActivity(intent, 851969));
    }

    private static ComponentName createComponentName(ResolveInfo resolveInfo) {
        ComponentInfo componentInfo;
        if (resolveInfo == null) {
            return null;
        }
        if (resolveInfo.activityInfo != null) {
            componentInfo = resolveInfo.activityInfo;
        } else {
            ComponentInfo componentInfo2 = resolveInfo.serviceInfo;
            if (componentInfo2 == null) {
                return null;
            }
            componentInfo = resolveInfo.serviceInfo;
        }
        try {
            return new ComponentName(componentInfo.packageName, componentInfo.name);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }
}