package com.android.server.notification;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import com.android.server.UnifiedConfigThread;
import com.vivo.face.common.data.Constants;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class NotificationIntelligentFilterManager {
    private static final String ACTION_UNIFIED_CONFIG_UPDATE = "com.vivo.daemonService.unifiedconfig.update_finish_broadcast_NotificationIntelligentFilter";
    private static final int INDEX_CHANNEL = 2;
    private static final int INDEX_ID = 1;
    private static final int INDEX_IMPORTANCE = 3;
    private static final int INDEX_PACKAGE_NAME = 0;
    static final int SHOULD_USE_CLASSIFY_RESULT = -101;
    private static final String TAG = "NotificationIntelligentFilterManager";
    private static final String UNIFIED_CONFIG_URI = "content://com.vivo.abe.unifiedconfig.provider/configs";
    private static final String XML_NAME_CHANNEL = "channelId";
    private static final String XML_NAME_ID = "notificationId";
    private static final String XML_NAME_IMPORTANCE = "importance";
    private static final String XML_NAME_PACKAGE_NAME = "packageName";
    private static final String XML_NAME_RULE = "rule";
    private static final String XML_ROOT_NAME = "NotificationIntelligentFilter";
    private final Context mContext;
    private final Handler mNotificationFilterHandler;
    private final Object mSynLock = new Object();
    private final BroadcastReceiver unifiedConfigReceiver = new BroadcastReceiver() { // from class: com.android.server.notification.NotificationIntelligentFilterManager.1
        {
            NotificationIntelligentFilterManager.this = this;
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (NotificationIntelligentFilterManager.ACTION_UNIFIED_CONFIG_UPDATE.equals(intent.getAction())) {
                NotificationIntelligentFilterManager.this.mNotificationFilterHandler.removeCallbacks(NotificationIntelligentFilterManager.this.getUnifiedConfigRunnable);
                NotificationIntelligentFilterManager.this.mNotificationFilterHandler.postDelayed(NotificationIntelligentFilterManager.this.getUnifiedConfigRunnable, 60000L);
            }
        }
    };
    private final Runnable getUnifiedConfigRunnable = new Runnable() { // from class: com.android.server.notification.-$$Lambda$NotificationIntelligentFilterManager$2_s9GfxUOnwkLdYfP_4y3sDWuGM
        @Override // java.lang.Runnable
        public final void run() {
            NotificationIntelligentFilterManager.this.lambda$new$0$NotificationIntelligentFilterManager();
        }
    };
    private List<FilterItem> mFilterRules = new ArrayList();

    public /* synthetic */ void lambda$new$0$NotificationIntelligentFilterManager() {
        if (NotificationManagerService.DBG) {
            VSlog.d(TAG, "getUnifiedConfigRunnable");
        }
        getUnifiedConfig(XML_ROOT_NAME, "1", "1.0", XML_ROOT_NAME);
    }

    public NotificationIntelligentFilterManager(Context context) {
        this.mContext = context;
        Handler handler = UnifiedConfigThread.getHandler();
        this.mNotificationFilterHandler = handler;
        handler.postDelayed(new $$Lambda$NotificationIntelligentFilterManager$HkG9A7tm1pbQEUgHMrPzYJ2hFZw(this), 5000L);
    }

    public void initFilterList() {
        this.mNotificationFilterHandler.removeCallbacks(this.getUnifiedConfigRunnable);
        this.mNotificationFilterHandler.postDelayed(this.getUnifiedConfigRunnable, 80000L);
    }

    /* JADX WARN: Code restructure failed: missing block: B:147:0x0067, code lost:
        r5 = r8.getName();
        r6 = 65535;
     */
    /* JADX WARN: Code restructure failed: missing block: B:148:0x0070, code lost:
        switch(r5.hashCode()) {
            case 788267878: goto L59;
            case 908759025: goto L56;
            case 1461735806: goto L53;
            case 2125650548: goto L50;
            default: goto L22;
        };
     */
    /* JADX WARN: Code restructure failed: missing block: B:151:0x007a, code lost:
        if (r5.equals(com.android.server.notification.NotificationIntelligentFilterManager.XML_NAME_IMPORTANCE) == false) goto L22;
     */
    /* JADX WARN: Code restructure failed: missing block: B:152:0x007c, code lost:
        r6 = 3;
     */
    /* JADX WARN: Code restructure failed: missing block: B:154:0x0084, code lost:
        if (r5.equals(com.android.server.notification.NotificationIntelligentFilterManager.XML_NAME_CHANNEL) == false) goto L22;
     */
    /* JADX WARN: Code restructure failed: missing block: B:155:0x0086, code lost:
        r6 = 2;
     */
    /* JADX WARN: Code restructure failed: missing block: B:157:0x008e, code lost:
        if (r5.equals(com.android.server.notification.NotificationIntelligentFilterManager.XML_NAME_PACKAGE_NAME) == false) goto L22;
     */
    /* JADX WARN: Code restructure failed: missing block: B:158:0x0090, code lost:
        r6 = 0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:160:0x0098, code lost:
        if (r5.equals(com.android.server.notification.NotificationIntelligentFilterManager.XML_NAME_ID) == false) goto L22;
     */
    /* JADX WARN: Code restructure failed: missing block: B:161:0x009a, code lost:
        r6 = 1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:162:0x009b, code lost:
        if (r6 == 0) goto L45;
     */
    /* JADX WARN: Code restructure failed: missing block: B:163:0x009d, code lost:
        if (r6 == 1) goto L40;
     */
    /* JADX WARN: Code restructure failed: missing block: B:164:0x009f, code lost:
        if (r6 == 2) goto L35;
     */
    /* JADX WARN: Code restructure failed: missing block: B:165:0x00a1, code lost:
        if (r6 == 3) goto L30;
     */
    /* JADX WARN: Code restructure failed: missing block: B:168:0x00aa, code lost:
        if (android.text.TextUtils.isEmpty(r2[3]) == false) goto L34;
     */
    /* JADX WARN: Code restructure failed: missing block: B:169:0x00ac, code lost:
        r2[3] = r8.nextText();
     */
    /* JADX WARN: Code restructure failed: missing block: B:171:0x00b9, code lost:
        if (android.text.TextUtils.isEmpty(r2[2]) == false) goto L39;
     */
    /* JADX WARN: Code restructure failed: missing block: B:172:0x00bb, code lost:
        r2[2] = r8.nextText();
     */
    /* JADX WARN: Code restructure failed: missing block: B:174:0x00c8, code lost:
        if (android.text.TextUtils.isEmpty(r2[1]) == false) goto L44;
     */
    /* JADX WARN: Code restructure failed: missing block: B:175:0x00ca, code lost:
        r2[1] = r8.nextText();
     */
    /* JADX WARN: Code restructure failed: missing block: B:177:0x00d7, code lost:
        if (android.text.TextUtils.isEmpty(r2[0]) == false) goto L49;
     */
    /* JADX WARN: Code restructure failed: missing block: B:178:0x00d9, code lost:
        r2[0] = r8.nextText();
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private void parseDataFromXml(java.lang.String r14) {
        /*
            Method dump skipped, instructions count: 438
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.notification.NotificationIntelligentFilterManager.parseDataFromXml(java.lang.String):void");
    }

    private boolean isRuleValid(String[] xmlData) {
        if (TextUtils.isEmpty(xmlData[0]) || TextUtils.isEmpty(xmlData[3])) {
            return false;
        }
        try {
            Integer.parseInt(xmlData[3]);
            if (xmlData[1] == null) {
                xmlData[1] = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            }
            if (xmlData[2] == null) {
                xmlData[2] = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean isNotificationNoNeedClassify(String packageName) {
        return checkHasRuleForPackage(packageName);
    }

    public int getTargetNotificationImportance(StatusBarNotification statusBarNotification) {
        int result = -101;
        String packageName = statusBarNotification.getPackageName();
        if (checkHasRuleForPackage(packageName)) {
            synchronized (this.mSynLock) {
                List<FilterItem> tempRules = getAllPackageRules(packageName);
                result = findFittestRule(tempRules, statusBarNotification).mImportance;
            }
            if (NotificationManagerService.DBG) {
                VSlog.d(TAG, "NotificationIntelligentFilter found result = " + result);
            }
        }
        return result;
    }

    private FilterItem findFittestRule(List<FilterItem> fromList, StatusBarNotification sbn) {
        FilterItem result = new FilterItem(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, -101);
        String notificationId = String.valueOf(sbn.getId());
        String channelId = sbn.getNotification().getChannelId();
        for (FilterItem item : fromList) {
            if (item.mNotificationId.equals(notificationId) && (item.mChannelId.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK) || item.mChannelId.equals(channelId))) {
                result = item;
                break;
            }
        }
        if (result.mImportance == -101 && !"miscellaneous".equals(channelId)) {
            Iterator<FilterItem> it = fromList.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                FilterItem item2 = it.next();
                if (item2.mChannelId.equals(channelId) && item2.mNotificationId.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK)) {
                    result = item2;
                    break;
                }
            }
        }
        if (result.mImportance == -101) {
            for (FilterItem item3 : fromList) {
                if (TextUtils.isEmpty(item3.mChannelId) && TextUtils.isEmpty(item3.mNotificationId)) {
                    return item3;
                }
            }
            return result;
        }
        return result;
    }

    private List<FilterItem> getAllPackageRules(String packageName) {
        List<FilterItem> filterRulesForTargetPak = new ArrayList<>();
        for (FilterItem item : this.mFilterRules) {
            if (item.mPackageName.equals(packageName)) {
                filterRulesForTargetPak.add(item);
            }
        }
        return filterRulesForTargetPak;
    }

    private boolean checkHasRuleForPackage(String packageName) {
        boolean result = false;
        synchronized (this.mSynLock) {
            Iterator<FilterItem> it = this.mFilterRules.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                FilterItem i = it.next();
                if (i.mPackageName.equals(packageName)) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    public void onStart() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_UNIFIED_CONFIG_UPDATE);
        this.mContext.registerReceiver(this.unifiedConfigReceiver, intentFilter);
        this.mNotificationFilterHandler.postDelayed(new $$Lambda$NotificationIntelligentFilterManager$HkG9A7tm1pbQEUgHMrPzYJ2hFZw(this), 5000L);
    }

    private boolean getUnifiedConfig(String moduleName, String type, String version, String identifier) {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        String[] selectionArgs = {moduleName, type, version, identifier};
        byte[] fileContent = null;
        try {
            Cursor cursor = contentResolver.query(Uri.parse("content://com.vivo.abe.unifiedconfig.provider/configs"), null, null, selectionArgs, null);
            if (cursor != null) {
                cursor.moveToFirst();
                if (cursor.getCount() > 0) {
                    while (!cursor.isAfterLast()) {
                        fileContent = cursor.getBlob(cursor.getColumnIndex("filecontent"));
                        cursor.moveToNext();
                    }
                } else if (NotificationManagerService.DBG) {
                    VSlog.d(TAG, "no data!");
                }
            }
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            VSlog.e(TAG, "open database error! " + e.fillInStackTrace());
        }
        if (fileContent != null) {
            String tmpResult = new String(fileContent);
            parseDataFromXml(tmpResult);
            return true;
        }
        return false;
    }

    /* loaded from: classes.dex */
    public class FilterItem {
        final String mChannelId;
        final int mImportance;
        final String mNotificationId;
        final String mPackageName;

        FilterItem(String packageName, String notificationId, String channelId, int importance) {
            NotificationIntelligentFilterManager.this = r1;
            this.mChannelId = channelId;
            this.mPackageName = packageName;
            this.mNotificationId = notificationId;
            this.mImportance = importance;
        }
    }
}