package com.android.server.notification;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.CalendarContract;
import android.service.notification.ZenModeConfig;
import android.util.Log;
import android.util.Slog;
import java.io.PrintWriter;
import java.util.Objects;

/* loaded from: classes.dex */
public class CalendarTracker {
    private static final String ATTENDEE_SELECTION = "event_id = ? AND attendeeEmail = ?";
    private static final boolean DEBUG_ATTENDEES = false;
    private static final int EVENT_CHECK_LOOKAHEAD = 86400000;
    private static final String INSTANCE_ORDER_BY = "begin ASC";
    private static final String TAG = "ConditionProviders.CT";
    private Callback mCallback;
    private final ContentObserver mObserver = new ContentObserver(null) { // from class: com.android.server.notification.CalendarTracker.1
        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri u) {
            if (CalendarTracker.DEBUG) {
                Log.d(CalendarTracker.TAG, "onChange selfChange=" + selfChange + " uri=" + u + " u=" + CalendarTracker.this.mUserContext.getUserId());
            }
            CalendarTracker.this.mCallback.onChanged();
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            if (CalendarTracker.DEBUG) {
                Log.d(CalendarTracker.TAG, "onChange selfChange=" + selfChange);
            }
        }
    };
    private boolean mRegistered;
    private final Context mSystemContext;
    private final Context mUserContext;
    private static final boolean DEBUG = Log.isLoggable("ConditionProviders", 3);
    private static final String[] INSTANCE_PROJECTION = {"begin", "end", "title", "visible", "event_id", "calendar_displayName", "ownerAccount", "calendar_id", "availability"};
    private static final String[] ATTENDEE_PROJECTION = {"event_id", "attendeeEmail", "attendeeStatus"};

    /* loaded from: classes.dex */
    public interface Callback {
        void onChanged();
    }

    /* loaded from: classes.dex */
    public static class CheckEventResult {
        public boolean inEvent;
        public long recheckAt;
    }

    public CalendarTracker(Context systemContext, Context userContext) {
        this.mSystemContext = systemContext;
        this.mUserContext = userContext;
    }

    public void setCallback(Callback callback) {
        if (this.mCallback == callback) {
            return;
        }
        this.mCallback = callback;
        setRegistered(callback != null);
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.print(prefix);
        pw.print("mCallback=");
        pw.println(this.mCallback);
        pw.print(prefix);
        pw.print("mRegistered=");
        pw.println(this.mRegistered);
        pw.print(prefix);
        pw.print("u=");
        pw.println(this.mUserContext.getUserId());
    }

    /* JADX WARN: Code restructure failed: missing block: B:15:0x0049, code lost:
        if (r11 == null) goto L11;
     */
    /* JADX WARN: Code restructure failed: missing block: B:18:0x004e, code lost:
        if (com.android.server.notification.CalendarTracker.DEBUG == false) goto L14;
     */
    /* JADX WARN: Code restructure failed: missing block: B:19:0x0050, code lost:
        android.util.Log.d(com.android.server.notification.CalendarTracker.TAG, "getCalendarsWithAccess took " + (java.lang.System.currentTimeMillis() - r1));
     */
    /* JADX WARN: Code restructure failed: missing block: B:20:0x0069, code lost:
        return r3;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private android.util.ArraySet<java.lang.Long> getCalendarsWithAccess() {
        /*
            r12 = this;
            java.lang.String r0 = "ConditionProviders.CT"
            long r1 = java.lang.System.currentTimeMillis()
            android.util.ArraySet r3 = new android.util.ArraySet
            r3.<init>()
            java.lang.String r4 = "_id"
            java.lang.String[] r7 = new java.lang.String[]{r4}
            java.lang.String r4 = "calendar_access_level >= 500 AND sync_events = 1"
            r11 = 0
            android.content.Context r5 = r12.mUserContext     // Catch: java.lang.Throwable -> L40 android.database.sqlite.SQLiteException -> L42
            android.content.ContentResolver r5 = r5.getContentResolver()     // Catch: java.lang.Throwable -> L40 android.database.sqlite.SQLiteException -> L42
            android.net.Uri r6 = android.provider.CalendarContract.Calendars.CONTENT_URI     // Catch: java.lang.Throwable -> L40 android.database.sqlite.SQLiteException -> L42
            java.lang.String r8 = "calendar_access_level >= 500 AND sync_events = 1"
            r9 = 0
            r10 = 0
            android.database.Cursor r5 = r5.query(r6, r7, r8, r9, r10)     // Catch: java.lang.Throwable -> L40 android.database.sqlite.SQLiteException -> L42
            r11 = r5
        L25:
            if (r11 == 0) goto L3a
            boolean r5 = r11.moveToNext()     // Catch: java.lang.Throwable -> L40 android.database.sqlite.SQLiteException -> L42
            if (r5 == 0) goto L3a
            r5 = 0
            long r5 = r11.getLong(r5)     // Catch: java.lang.Throwable -> L40 android.database.sqlite.SQLiteException -> L42
            java.lang.Long r5 = java.lang.Long.valueOf(r5)     // Catch: java.lang.Throwable -> L40 android.database.sqlite.SQLiteException -> L42
            r3.add(r5)     // Catch: java.lang.Throwable -> L40 android.database.sqlite.SQLiteException -> L42
            goto L25
        L3a:
            if (r11 == 0) goto L4c
        L3c:
            r11.close()
            goto L4c
        L40:
            r0 = move-exception
            goto L6a
        L42:
            r5 = move-exception
            java.lang.String r6 = "error querying calendar content provider"
            android.util.Slog.w(r0, r6, r5)     // Catch: java.lang.Throwable -> L40
            if (r11 == 0) goto L4c
            goto L3c
        L4c:
            boolean r5 = com.android.server.notification.CalendarTracker.DEBUG
            if (r5 == 0) goto L69
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            java.lang.String r6 = "getCalendarsWithAccess took "
            r5.append(r6)
            long r8 = java.lang.System.currentTimeMillis()
            long r8 = r8 - r1
            r5.append(r8)
            java.lang.String r5 = r5.toString()
            android.util.Log.d(r0, r5)
        L69:
            return r3
        L6a:
            if (r11 == 0) goto L6f
            r11.close()
        L6f:
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.notification.CalendarTracker.getCalendarsWithAccess():android.util.ArraySet");
    }

    /* JADX WARN: Code restructure failed: missing block: B:50:0x0146, code lost:
        if (java.util.Objects.equals(r35.calName, r9) == false) goto L86;
     */
    /* JADX WARN: Removed duplicated region for block: B:115:0x020f  */
    /* JADX WARN: Removed duplicated region for block: B:60:0x015f  */
    /* JADX WARN: Removed duplicated region for block: B:63:0x0165 A[ADDED_TO_REGION] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public com.android.server.notification.CalendarTracker.CheckEventResult checkEvent(android.service.notification.ZenModeConfig.EventInfo r35, long r36) {
        /*
            Method dump skipped, instructions count: 531
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.notification.CalendarTracker.checkEvent(android.service.notification.ZenModeConfig$EventInfo, long):com.android.server.notification.CalendarTracker$CheckEventResult");
    }

    private boolean meetsAttendee(ZenModeConfig.EventInfo filter, int eventId, String email) {
        String[] selectionArgs;
        String selection;
        long start = System.currentTimeMillis();
        String selection2 = ATTENDEE_SELECTION;
        int i = 2;
        int i2 = 0;
        int i3 = 1;
        String[] selectionArgs2 = {Integer.toString(eventId), email};
        Cursor cursor = null;
        try {
            try {
                cursor = this.mUserContext.getContentResolver().query(CalendarContract.Attendees.CONTENT_URI, ATTENDEE_PROJECTION, ATTENDEE_SELECTION, selectionArgs2, null);
                try {
                    if (cursor != null && cursor.getCount() != 0) {
                        boolean rt = false;
                        while (cursor != null) {
                            if (!cursor.moveToNext()) {
                                break;
                            }
                            long rowEventId = cursor.getLong(i2);
                            String rowEmail = cursor.getString(i3);
                            int status = cursor.getInt(i);
                            boolean meetsReply = meetsReply(filter.reply, status);
                            if (DEBUG) {
                                StringBuilder sb = new StringBuilder();
                                selectionArgs = selectionArgs2;
                                try {
                                    sb.append("");
                                    selection = selection2;
                                } catch (SQLiteException e) {
                                    e = e;
                                } catch (Throwable th) {
                                    e = th;
                                }
                                try {
                                    sb.append(String.format("status=%s, meetsReply=%s", attendeeStatusToString(status), Boolean.valueOf(meetsReply)));
                                    Log.d(TAG, sb.toString());
                                } catch (SQLiteException e2) {
                                    e = e2;
                                    Slog.w(TAG, "error querying attendees content provider", e);
                                    if (cursor != null) {
                                        cursor.close();
                                    }
                                    if (DEBUG) {
                                        Log.d(TAG, "meetsAttendee took " + (System.currentTimeMillis() - start));
                                        return false;
                                    }
                                    return false;
                                } catch (Throwable th2) {
                                    e = th2;
                                    if (cursor != null) {
                                        cursor.close();
                                    }
                                    if (DEBUG) {
                                        Log.d(TAG, "meetsAttendee took " + (System.currentTimeMillis() - start));
                                    }
                                    throw e;
                                }
                            } else {
                                selectionArgs = selectionArgs2;
                                selection = selection2;
                            }
                            boolean eventMeets = rowEventId == ((long) eventId) && Objects.equals(rowEmail, email) && meetsReply;
                            rt |= eventMeets;
                            selectionArgs2 = selectionArgs;
                            selection2 = selection;
                            i = 2;
                            i2 = 0;
                            i3 = 1;
                        }
                        if (cursor != null) {
                            cursor.close();
                        }
                        if (DEBUG) {
                            Log.d(TAG, "meetsAttendee took " + (System.currentTimeMillis() - start));
                        }
                        return rt;
                    }
                    if (DEBUG) {
                        Log.d(TAG, "No attendees found");
                    }
                    if (cursor != null) {
                        cursor.close();
                    }
                    if (DEBUG) {
                        Log.d(TAG, "meetsAttendee took " + (System.currentTimeMillis() - start));
                        return true;
                    }
                    return true;
                } catch (SQLiteException e3) {
                    e = e3;
                }
            } catch (SQLiteException e4) {
                e = e4;
            } catch (Throwable th3) {
                e = th3;
            }
        } catch (Throwable th4) {
            e = th4;
        }
    }

    private void setRegistered(boolean registered) {
        if (this.mRegistered == registered) {
            return;
        }
        ContentResolver cr = this.mSystemContext.getContentResolver();
        int userId = this.mUserContext.getUserId();
        if (this.mRegistered) {
            if (DEBUG) {
                Log.d(TAG, "unregister content observer u=" + userId);
            }
            cr.unregisterContentObserver(this.mObserver);
        }
        this.mRegistered = registered;
        if (DEBUG) {
            Log.d(TAG, "mRegistered = " + registered + " u=" + userId);
        }
        if (this.mRegistered) {
            if (DEBUG) {
                Log.d(TAG, "register content observer u=" + userId);
            }
            cr.registerContentObserver(CalendarContract.Instances.CONTENT_URI, true, this.mObserver, userId);
            cr.registerContentObserver(CalendarContract.Events.CONTENT_URI, true, this.mObserver, userId);
            cr.registerContentObserver(CalendarContract.Calendars.CONTENT_URI, true, this.mObserver, userId);
        }
    }

    private static String attendeeStatusToString(int status) {
        if (status != 0) {
            if (status != 1) {
                if (status != 2) {
                    if (status != 3) {
                        if (status == 4) {
                            return "ATTENDEE_STATUS_TENTATIVE";
                        }
                        return "ATTENDEE_STATUS_UNKNOWN_" + status;
                    }
                    return "ATTENDEE_STATUS_INVITED";
                }
                return "ATTENDEE_STATUS_DECLINED";
            }
            return "ATTENDEE_STATUS_ACCEPTED";
        }
        return "ATTENDEE_STATUS_NONE";
    }

    private static String availabilityToString(int availability) {
        if (availability != 0) {
            if (availability != 1) {
                if (availability == 2) {
                    return "AVAILABILITY_TENTATIVE";
                }
                return "AVAILABILITY_UNKNOWN_" + availability;
            }
            return "AVAILABILITY_FREE";
        }
        return "AVAILABILITY_BUSY";
    }

    private static boolean meetsReply(int reply, int attendeeStatus) {
        return reply != 0 ? reply != 1 ? reply == 2 && attendeeStatus == 1 : attendeeStatus == 1 || attendeeStatus == 4 : attendeeStatus != 2;
    }
}