package com.android.server.notification;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Xml;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class NotificationBadBehaviorManager {
    private static final String ACTION_UNIFIED_CONFIG_UPDATE = "com.vivo.daemonService.unifiedconfig.update_finish_broadcast_NotificationBadBehaviorManager";
    public static final String CONFIGSYSTEM_URI = "content://com.vivo.abe.unifiedconfig.provider/configs";
    private static final int INTERNAL_NOTIFICATION_TYPE_PUSH = 1;
    private static final String TAG = "NotificationBadBehaviorManager";
    private Context mContext;
    private Handler mModifyBadNotificationHandler;
    private NotificationManagerService mNotificationManagerService;
    private static String tagType = "whitelist";
    private static String tagPkg = "package";
    private final HandlerThread mModifyBadNotificationThread = new HandlerThread("notification_modified");
    private ArrayList<String> mNotificationBadApps = new ArrayList<>();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() { // from class: com.android.server.notification.NotificationBadBehaviorManager.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (NotificationBadBehaviorManager.ACTION_UNIFIED_CONFIG_UPDATE.equals(action)) {
                NotificationBadBehaviorManager.this.mModifyBadNotificationHandler.removeCallbacks(NotificationBadBehaviorManager.this.mGetUnifiedConfigRunnable);
                NotificationBadBehaviorManager.this.mModifyBadNotificationHandler.postDelayed(NotificationBadBehaviorManager.this.mGetUnifiedConfigRunnable, 700L);
            }
        }
    };
    private final Runnable mGetUnifiedConfigRunnable = new Runnable() { // from class: com.android.server.notification.NotificationBadBehaviorManager.2
        @Override // java.lang.Runnable
        public void run() {
            if (NotificationManagerService.DBG) {
                VSlog.d(NotificationBadBehaviorManager.TAG, "mGetUnifiedConfigRunnable!");
            }
            NotificationBadBehaviorManager.this.getUnifiedConfig(NotificationBadBehaviorManager.TAG, "2", "1.0", "notification_bad_behavior_manager");
        }
    };

    public NotificationBadBehaviorManager(Context context, NotificationManagerService notificationManagerService) {
        this.mContext = context;
        this.mNotificationManagerService = notificationManagerService;
        this.mModifyBadNotificationThread.start();
        Handler handler = new Handler(this.mModifyBadNotificationThread.getLooper());
        this.mModifyBadNotificationHandler = handler;
        handler.postDelayed(new Runnable() { // from class: com.android.server.notification.NotificationBadBehaviorManager.3
            @Override // java.lang.Runnable
            public void run() {
                NotificationBadBehaviorManager.this.init();
            }
        }, 6000L);
    }

    public void onStart() {
        registerBroadcastReceiver();
    }

    public boolean isNeed2Modify(String pkg) {
        boolean res;
        synchronized (this.mNotificationBadApps) {
            res = !TextUtils.isEmpty(pkg) && this.mNotificationBadApps.contains(pkg);
        }
        return res;
    }

    public void modifyNotification(Notification notification) {
        if (notification.internalType == 1) {
            return;
        }
        notification.flags &= -513;
        notification.setGroup(null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void init() {
        this.mModifyBadNotificationHandler.removeCallbacks(this.mGetUnifiedConfigRunnable);
        this.mModifyBadNotificationHandler.postDelayed(this.mGetUnifiedConfigRunnable, 80000L);
    }

    private void registerBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_UNIFIED_CONFIG_UPDATE);
        this.mContext.registerReceiver(this.mReceiver, intentFilter);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean getUnifiedConfig(String moduleName, String type, String version, String identifier) {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        String[] selectionArgs = {moduleName, type, version, identifier};
        Cursor cursor = null;
        byte[] fileContent = null;
        try {
            try {
                try {
                    Cursor cursor2 = contentResolver.query(Uri.parse("content://com.vivo.abe.unifiedconfig.provider/configs"), null, null, selectionArgs, null);
                    if (cursor2 != null) {
                        cursor2.moveToFirst();
                        if (cursor2.getCount() > 0) {
                            while (!cursor2.isAfterLast()) {
                                fileContent = cursor2.getBlob(cursor2.getColumnIndex("filecontent"));
                                cursor2.moveToNext();
                            }
                        } else if (NotificationManagerService.DBG) {
                            VSlog.d(TAG, "no data!");
                        }
                    }
                    if (cursor2 != null) {
                        cursor2.close();
                    }
                } catch (Throwable th) {
                    if (0 != 0) {
                        try {
                            cursor.close();
                        } catch (Exception e) {
                        }
                    }
                    throw th;
                }
            } catch (Exception e2) {
                VSlog.e(TAG, "open database error! " + e2.fillInStackTrace());
                if (0 != 0) {
                    cursor.close();
                }
            }
        } catch (Exception e3) {
        }
        if (fileContent != null) {
            String tmpResult = new String(fileContent);
            parseXmlFormatData(new ByteArrayInputStream(tmpResult.getBytes()));
            return true;
        }
        return false;
    }

    private void parseXmlFormatData(InputStream inputStream) {
        synchronized (this.mNotificationBadApps) {
            try {
                this.mNotificationBadApps.clear();
                try {
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setInput(inputStream, null);
                    parser.getName();
                    for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                        String tag = parser.getName();
                        if (eventType == 2 && tagPkg.equals(tag)) {
                            parser.next();
                            String pkg = parser.getText();
                            if (pkg != null && !TextUtils.isEmpty(pkg)) {
                                this.mNotificationBadApps.add(pkg);
                            }
                        }
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                    if (inputStream != null) {
                        inputStream.close();
                    }
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception e2) {
            }
        }
    }
}