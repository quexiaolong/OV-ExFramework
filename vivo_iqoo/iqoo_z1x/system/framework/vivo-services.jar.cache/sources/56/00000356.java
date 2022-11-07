package com.android.server.notification;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Settings;
import com.android.server.notification.ValidateNotificationPeople;
import java.util.ArrayList;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoValidateNotificationPeopleImpl implements IVivoValidateNotificationPeople {
    private static final float GROUPED_BOTH = 2.0f;
    private static final float GROUPED_CONTACT = 1.5f;
    private static final float GROUPED_MESSAGE = 1.75f;
    private static final String TAG = "ValidateNoPeople";
    private static final int URI_TYPE_EMAIL = 2;
    private static final int URI_TYPE_LOOKUP = 0;
    private static final int URI_TYPE_PHONE = 1;
    private static final String ZEN_MODE_CONTACTS_GROUP = "zen_mode_contacts_group";
    private static final String ZEN_MODE_MESSAGE_GROUP = "zen_mode_messages_group";
    private static final String[] LOOKUP_PROJECTION = {"_id", "starred"};
    private static final String[] LOOKUP_PROJECTION_PLUS_ID = {"_id", "starred", "raw_contact_id"};
    private static final String[] LOOKUP_PROJECTION_PLUS_ID2 = {"_id", "starred", "name_raw_contact_id"};
    private static final String[] LOOKUP_PROJECTION_GROUP = {"data1", "title"};

    public float checkGroupAffinity(Context context, float originAffinity, ValidateNotificationPeople.LookupResult lookupResult) {
        float checkGroupResult = checkInGroup(context, lookupResult);
        if (checkGroupResult == 0.0f) {
            return originAffinity;
        }
        float affinity = Math.max(originAffinity, checkGroupResult);
        return affinity;
    }

    private float checkInGroup(Context context, ValidateNotificationPeople.LookupResult lookupResult) {
        long groupId = Settings.Global.getLong(context.getContentResolver(), ZEN_MODE_CONTACTS_GROUP, -1L);
        long messageGroupId = Settings.Global.getLong(context.getContentResolver(), ZEN_MODE_MESSAGE_GROUP, -1L);
        if (groupId == -1 && messageGroupId == -1) {
            return 0.0f;
        }
        ArrayList<Long> tmpAl = lookupResult.mGroupIds;
        float result = 0.0f;
        if (tmpAl != null) {
            int i = 0;
            while (true) {
                if (i >= tmpAl.size()) {
                    break;
                }
                if (groupId != -1 && tmpAl.get(i).longValue() == groupId) {
                    if (result == GROUPED_MESSAGE) {
                        result = GROUPED_BOTH;
                        break;
                    } else if (result == -4.0f) {
                        result = 1.9f;
                        break;
                    } else {
                        result = GROUPED_CONTACT;
                    }
                } else if (groupId == -1 && lookupResult.getAffinity() == 1.0f) {
                    result = -3.0f;
                }
                if (messageGroupId != -1 && tmpAl.get(i).longValue() == messageGroupId) {
                    if (result == GROUPED_CONTACT) {
                        result = GROUPED_BOTH;
                        break;
                    } else if (result == -3.0f) {
                        result = 1.8f;
                        break;
                    } else {
                        result = GROUPED_MESSAGE;
                    }
                } else if (messageGroupId == -1 && lookupResult.getAffinity() == 1.0f) {
                    if (result == GROUPED_CONTACT) {
                        result = 1.9f;
                    } else {
                        result = -4.0f;
                    }
                }
                i++;
            }
        }
        if (result < 0.0f) {
            return 0.0f;
        }
        return result;
    }

    public Cursor searchContacts(Context context, Uri lookupUri, int uriType) {
        if (NotificationManagerService.DBG) {
            VSlog.d(TAG, "DEBUG_GROUP:searchContacts lookupUri=" + lookupUri);
        }
        RuntimeException here = new RuntimeException("here");
        here.fillInStackTrace();
        if (NotificationManagerService.DBG) {
            VSlog.d(TAG, "searchContacts", here);
        }
        if (uriType == 0) {
            Cursor c = context.getContentResolver().query(lookupUri, LOOKUP_PROJECTION_PLUS_ID2, null, null, null);
            return c;
        } else if (uriType == 1) {
            Cursor c2 = context.getContentResolver().query(lookupUri, LOOKUP_PROJECTION_PLUS_ID, null, null, null);
            return c2;
        } else {
            Cursor c3 = context.getContentResolver().query(lookupUri, LOOKUP_PROJECTION, null, null, null);
            return c3;
        }
    }

    public void modifyGroupId(Context context, Uri lookupUri, ValidateNotificationPeople.LookupResult lookupResult) {
        if (lookupResult.mRawId != 0) {
            lookupResult.mGroupIds = searchGroups(context, lookupUri, lookupResult.mRawId);
        }
        if (NotificationManagerService.DBG) {
            VSlog.d(TAG, "searchContacts lookupResult.mGroupIds=" + lookupResult.mGroupIds + "mAffinity=" + lookupResult.getAffinity());
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:23:0x00a2, code lost:
        if (r11 != null) goto L29;
     */
    /* JADX WARN: Code restructure failed: missing block: B:24:0x00a4, code lost:
        r11.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:27:0x00af, code lost:
        if (0 == 0) goto L30;
     */
    /* JADX WARN: Code restructure failed: missing block: B:29:0x00b2, code lost:
        return r12;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private java.util.ArrayList<java.lang.Long> searchGroups(android.content.Context r15, android.net.Uri r16, long r17) {
        /*
            r14 = this;
            java.lang.String r1 = "ValidateNoPeople"
            r2 = r17
            android.net.Uri r0 = android.provider.ContactsContract.AUTHORITY_URI
            java.lang.String r4 = "get_groups_by_contact"
            android.net.Uri r0 = android.net.Uri.withAppendedPath(r0, r4)
            android.net.Uri r10 = getNonEncryptUri(r0)
            r11 = 0
            r12 = 0
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r4 = "account_type NOT NULL AND account_name NOT NULL AND auto_add=0 AND favorites=0 AND deleted=0 AND view_data.raw_contact_id="
            r0.append(r4)
            r0.append(r2)
            java.lang.String r13 = r0.toString()
            android.content.ContentResolver r4 = r15.getContentResolver()     // Catch: java.lang.Throwable -> La8
            java.lang.String[] r6 = com.android.server.notification.VivoValidateNotificationPeopleImpl.LOOKUP_PROJECTION_GROUP     // Catch: java.lang.Throwable -> La8
            r8 = 0
            r9 = 0
            r5 = r10
            r7 = r13
            android.database.Cursor r0 = r4.query(r5, r6, r7, r8, r9)     // Catch: java.lang.Throwable -> La8
            r11 = r0
            if (r11 != 0) goto L40
            java.lang.String r0 = "Null cursor from contacts group query."
            vivo.util.VSlog.w(r1, r0)     // Catch: java.lang.Throwable -> La8
            r0 = 0
            if (r11 == 0) goto L3f
            r11.close()
        L3f:
            return r0
        L40:
            int r0 = r11.getCount()     // Catch: java.lang.Throwable -> La8
            boolean r4 = com.android.server.notification.NotificationManagerService.DBG     // Catch: java.lang.Throwable -> La8
            if (r4 == 0) goto L60
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> La8
            r4.<init>()     // Catch: java.lang.Throwable -> La8
            java.lang.String r5 = "DEBUG_GROUP:cursor.count = "
            r4.append(r5)     // Catch: java.lang.Throwable -> La8
            int r5 = r11.getCount()     // Catch: java.lang.Throwable -> La8
            r4.append(r5)     // Catch: java.lang.Throwable -> La8
            java.lang.String r4 = r4.toString()     // Catch: java.lang.Throwable -> La8
            vivo.util.VSlog.d(r1, r4)     // Catch: java.lang.Throwable -> La8
        L60:
            if (r0 <= 0) goto La2
            java.util.ArrayList r4 = new java.util.ArrayList     // Catch: java.lang.Throwable -> La8
            r4.<init>()     // Catch: java.lang.Throwable -> La8
            r12 = r4
            r4 = 0
        L69:
            if (r4 >= r0) goto La2
            boolean r5 = r11.moveToNext()     // Catch: java.lang.Throwable -> La8
            if (r5 != 0) goto L72
            goto La2
        L72:
            r5 = 0
            long r5 = r11.getLong(r5)     // Catch: java.lang.Throwable -> La8
            java.lang.Long r5 = java.lang.Long.valueOf(r5)     // Catch: java.lang.Throwable -> La8
            java.lang.Long r6 = new java.lang.Long     // Catch: java.lang.Throwable -> La8
            long r7 = r5.longValue()     // Catch: java.lang.Throwable -> La8
            r6.<init>(r7)     // Catch: java.lang.Throwable -> La8
            r12.add(r6)     // Catch: java.lang.Throwable -> La8
            boolean r6 = com.android.server.notification.NotificationManagerService.DBG     // Catch: java.lang.Throwable -> La8
            if (r6 == 0) goto L9f
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> La8
            r6.<init>()     // Catch: java.lang.Throwable -> La8
            java.lang.String r7 = "DEBUG_GROUP: add = "
            r6.append(r7)     // Catch: java.lang.Throwable -> La8
            r6.append(r5)     // Catch: java.lang.Throwable -> La8
            java.lang.String r6 = r6.toString()     // Catch: java.lang.Throwable -> La8
            vivo.util.VSlog.d(r1, r6)     // Catch: java.lang.Throwable -> La8
        L9f:
            int r4 = r4 + 1
            goto L69
        La2:
            if (r11 == 0) goto Lb2
        La4:
            r11.close()
            goto Lb2
        La8:
            r0 = move-exception
            java.lang.String r4 = "Problem getting content resolver or performing contacts query."
            vivo.util.VSlog.w(r1, r4, r0)     // Catch: java.lang.Throwable -> Lb3
            if (r11 == 0) goto Lb2
            goto La4
        Lb2:
            return r12
        Lb3:
            r0 = move-exception
            if (r11 == 0) goto Lb9
            r11.close()
        Lb9:
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.notification.VivoValidateNotificationPeopleImpl.searchGroups(android.content.Context, android.net.Uri, long):java.util.ArrayList");
    }

    private static Uri getNonEncryptUri(Uri uri) {
        if (uri != null) {
            return uri.buildUpon().appendQueryParameter("encrypt", " < 2").build();
        }
        return uri;
    }
}