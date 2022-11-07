package com.android.server.people.data;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.provider.Telephony;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Slog;
import android.util.SparseIntArray;
import com.android.server.voiceinteraction.DatabaseHelper;
import java.util.function.BiConsumer;
import vivo.util.VSlog;

/* loaded from: classes.dex */
class MmsQueryHelper {
    private static final long MILLIS_PER_SECONDS = 1000;
    private static final SparseIntArray MSG_BOX_TO_EVENT_TYPE;
    private static final String TAG = "MmsQueryHelper";
    private final Context mContext;
    private String mCurrentCountryIso;
    private final BiConsumer<String, Event> mEventConsumer;
    private long mLastMessageTimestamp;

    static {
        SparseIntArray sparseIntArray = new SparseIntArray();
        MSG_BOX_TO_EVENT_TYPE = sparseIntArray;
        sparseIntArray.put(1, 9);
        MSG_BOX_TO_EVENT_TYPE.put(2, 8);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public MmsQueryHelper(Context context, BiConsumer<String, Event> eventConsumer) {
        this.mContext = context;
        this.mEventConsumer = eventConsumer;
        this.mCurrentCountryIso = Utils.getCurrentCountryIso(context);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean querySince(long sinceTime) {
        Throwable th;
        String[] projection = {"_id", "date", "msg_box"};
        long j = 1000;
        String[] selectionArgs = {Long.toString(sinceTime / 1000)};
        boolean hasResults = false;
        Binder.allowBlockingForCurrentThread();
        try {
            Cursor cursor = this.mContext.getContentResolver().query(Telephony.Mms.CONTENT_URI, projection, "date > ?", selectionArgs, null);
            try {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        try {
                            int msgIdIndex = cursor.getColumnIndex("_id");
                            String msgId = cursor.getString(msgIdIndex);
                            int dateIndex = cursor.getColumnIndex("date");
                            String[] projection2 = projection;
                            long date = cursor.getLong(dateIndex) * j;
                            try {
                                int msgBoxIndex = cursor.getColumnIndex("msg_box");
                                int msgBox = cursor.getInt(msgBoxIndex);
                                this.mLastMessageTimestamp = Math.max(this.mLastMessageTimestamp, date);
                                String address = getMmsAddress(msgId, msgBox);
                                if (address != null && addEvent(address, date, msgBox)) {
                                    hasResults = true;
                                }
                                projection = projection2;
                                j = 1000;
                            } catch (Throwable th2) {
                                th = th2;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                        }
                    }
                    if (cursor != null) {
                        cursor.close();
                    }
                    Binder.defaultBlockingForCurrentThread();
                    return hasResults;
                }
                try {
                    Slog.w(TAG, "Cursor is null when querying MMS table.");
                    if (cursor != null) {
                        try {
                            cursor.close();
                        } catch (Throwable th4) {
                            th = th4;
                            Binder.defaultBlockingForCurrentThread();
                            throw th;
                        }
                    }
                    Binder.defaultBlockingForCurrentThread();
                    return false;
                } catch (Throwable th5) {
                    th = th5;
                }
                if (cursor != null) {
                    cursor.close();
                }
                throw th;
            } catch (Throwable th6) {
                th = th6;
            }
        } catch (Throwable th7) {
            th = th7;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getLastMessageTimestamp() {
        return this.mLastMessageTimestamp;
    }

    private String getMmsAddress(String msgId, int msgBox) {
        Cursor cursor;
        Uri addressUri = Telephony.Mms.Addr.getAddrUriForMessage(msgId);
        String[] projection = {"address", DatabaseHelper.SoundModelContract.KEY_TYPE};
        String address = null;
        try {
            cursor = this.mContext.getContentResolver().query(addressUri, projection, null, null, null);
        } catch (Exception e) {
            VSlog.w(TAG, "query mms log failed:" + e.getMessage());
        }
        if (cursor == null) {
            Slog.w(TAG, "Cursor is null when querying MMS address table.");
            if (cursor != null) {
                cursor.close();
            }
            return null;
        }
        while (cursor.moveToNext()) {
            int typeIndex = cursor.getColumnIndex(DatabaseHelper.SoundModelContract.KEY_TYPE);
            int type = cursor.getInt(typeIndex);
            if ((msgBox == 1 && type == 137) || (msgBox == 2 && type == 151)) {
                int addrIndex = cursor.getColumnIndex("address");
                address = cursor.getString(addrIndex);
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        if (!Telephony.Mms.isPhoneNumber(address)) {
            return null;
        }
        return PhoneNumberUtils.formatNumberToE164(address, this.mCurrentCountryIso);
    }

    private boolean addEvent(String phoneNumber, long date, int msgBox) {
        if (!validateEvent(phoneNumber, date, msgBox)) {
            return false;
        }
        int eventType = MSG_BOX_TO_EVENT_TYPE.get(msgBox);
        this.mEventConsumer.accept(phoneNumber, new Event(date, eventType));
        return true;
    }

    private boolean validateEvent(String phoneNumber, long date, int msgBox) {
        return !TextUtils.isEmpty(phoneNumber) && date > 0 && MSG_BOX_TO_EVENT_TYPE.indexOfKey(msgBox) >= 0;
    }
}