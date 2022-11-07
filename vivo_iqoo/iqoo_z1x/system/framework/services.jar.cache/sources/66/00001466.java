package com.android.server.people.data;

import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog;
import android.text.TextUtils;
import android.util.Slog;
import android.util.SparseIntArray;
import com.android.server.people.data.Event;
import com.android.server.voiceinteraction.DatabaseHelper;
import java.util.function.BiConsumer;
import vivo.util.VSlog;

/* loaded from: classes.dex */
class CallLogQueryHelper {
    private static final SparseIntArray CALL_TYPE_TO_EVENT_TYPE;
    private static final String TAG = "CallLogQueryHelper";
    private final Context mContext;
    private final BiConsumer<String, Event> mEventConsumer;
    private long mLastCallTimestamp;

    static {
        SparseIntArray sparseIntArray = new SparseIntArray();
        CALL_TYPE_TO_EVENT_TYPE = sparseIntArray;
        sparseIntArray.put(1, 11);
        CALL_TYPE_TO_EVENT_TYPE.put(2, 10);
        CALL_TYPE_TO_EVENT_TYPE.put(3, 12);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public CallLogQueryHelper(Context context, BiConsumer<String, Event> eventConsumer) {
        this.mContext = context;
        this.mEventConsumer = eventConsumer;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean querySince(long sinceTime) {
        Throwable th;
        String str = "normalized_number";
        String str2 = "date";
        String str3 = "duration";
        String[] projection = {"normalized_number", "date", "duration", DatabaseHelper.SoundModelContract.KEY_TYPE};
        String[] selectionArgs = {Long.toString(sinceTime)};
        boolean hasResults = false;
        try {
            Cursor cursor = this.mContext.getContentResolver().query(CallLog.Calls.CONTENT_URI, projection, "date > ?", selectionArgs, "date DESC");
            if (cursor != null) {
                boolean hasResults2 = false;
                while (true) {
                    try {
                        boolean hasResults3 = cursor.moveToNext();
                        if (!hasResults3) {
                            break;
                        }
                        int numberIndex = cursor.getColumnIndex(str);
                        String phoneNumber = cursor.getString(numberIndex);
                        int dateIndex = cursor.getColumnIndex(str2);
                        long date = cursor.getLong(dateIndex);
                        int durationIndex = cursor.getColumnIndex(str3);
                        long durationSeconds = cursor.getLong(durationIndex);
                        String str4 = str2;
                        String str5 = str3;
                        int typeIndex = cursor.getColumnIndex(DatabaseHelper.SoundModelContract.KEY_TYPE);
                        int callType = cursor.getInt(typeIndex);
                        String str6 = str;
                        this.mLastCallTimestamp = Math.max(this.mLastCallTimestamp, date);
                        if (addEvent(phoneNumber, date, durationSeconds, callType)) {
                            hasResults2 = true;
                        }
                        str2 = str4;
                        str3 = str5;
                        str = str6;
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
                if (cursor != null) {
                    try {
                        cursor.close();
                        return hasResults2;
                    } catch (Exception e) {
                        e = e;
                        hasResults = hasResults2;
                        VSlog.w(TAG, "query call log Failed:" + e.getMessage());
                        return hasResults;
                    }
                }
                return hasResults2;
            }
            try {
                Slog.w(TAG, "Cursor is null when querying call log.");
                if (cursor != null) {
                    cursor.close();
                }
                return false;
            } catch (Throwable th3) {
                th = th3;
            }
            if (cursor != null) {
                cursor.close();
            }
            throw th;
        } catch (Exception e2) {
            e = e2;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getLastCallTimestamp() {
        return this.mLastCallTimestamp;
    }

    private boolean addEvent(String phoneNumber, long date, long durationSeconds, int callType) {
        if (!validateEvent(phoneNumber, date, callType)) {
            return false;
        }
        int eventType = CALL_TYPE_TO_EVENT_TYPE.get(callType);
        Event event = new Event.Builder(date, eventType).setDurationSeconds((int) durationSeconds).build();
        this.mEventConsumer.accept(phoneNumber, event);
        return true;
    }

    private boolean validateEvent(String phoneNumber, long date, int callType) {
        return !TextUtils.isEmpty(phoneNumber) && date > 0 && CALL_TYPE_TO_EVENT_TYPE.indexOfKey(callType) >= 0;
    }
}