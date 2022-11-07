package com.android.server.people.data;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Slog;
import vivo.util.VSlog;

/* loaded from: classes.dex */
class ContactsQueryHelper {
    private static final String TAG = "ContactsQueryHelper";
    private Uri mContactUri;
    private final Context mContext;
    private boolean mIsStarred;
    private long mLastUpdatedTimestamp;
    private String mPhoneNumber;

    /* JADX INFO: Access modifiers changed from: package-private */
    public ContactsQueryHelper(Context context) {
        this.mContext = context;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean query(String contactUri) {
        if (TextUtils.isEmpty(contactUri)) {
            return false;
        }
        Uri uri = Uri.parse(contactUri);
        if ("tel".equals(uri.getScheme())) {
            return queryWithPhoneNumber(uri.getSchemeSpecificPart());
        }
        if ("mailto".equals(uri.getScheme())) {
            return queryWithEmail(uri.getSchemeSpecificPart());
        }
        if (contactUri.startsWith(ContactsContract.Contacts.CONTENT_LOOKUP_URI.toString())) {
            return queryWithUri(uri);
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean querySince(long sinceTime) {
        String[] projection = {"_id", "lookup", "starred", "has_phone_number", "contact_last_updated_timestamp"};
        String[] selectionArgs = {Long.toString(sinceTime)};
        return queryContact(ContactsContract.Contacts.CONTENT_URI, projection, "contact_last_updated_timestamp > ?", selectionArgs);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Uri getContactUri() {
        return this.mContactUri;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isStarred() {
        return this.mIsStarred;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getPhoneNumber() {
        return this.mPhoneNumber;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public long getLastUpdatedTimestamp() {
        return this.mLastUpdatedTimestamp;
    }

    private boolean queryWithPhoneNumber(String phoneNumber) {
        Uri phoneUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        return queryWithUri(phoneUri);
    }

    private boolean queryWithEmail(String email) {
        Uri emailUri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Email.CONTENT_LOOKUP_URI, Uri.encode(email));
        return queryWithUri(emailUri);
    }

    private boolean queryWithUri(Uri uri) {
        String[] projection = {"_id", "lookup", "starred", "has_phone_number"};
        return queryContact(uri, projection, null, null);
    }

    private boolean queryContact(Uri uri, String[] projection, String selection, String[] selectionArgs) {
        Cursor cursor;
        String lookupKey = null;
        boolean hasPhoneNumber = false;
        boolean found = false;
        try {
            cursor = this.mContext.getContentResolver().query(uri, projection, selection, selectionArgs, null);
        } catch (Exception e) {
            VSlog.w(TAG, "queryContact Failed.");
        }
        if (cursor == null) {
            Slog.w(TAG, "Cursor is null when querying contact.");
            if (cursor != null) {
                cursor.close();
            }
            return false;
        }
        while (cursor.moveToNext()) {
            int idIndex = cursor.getColumnIndex("_id");
            long contactId = cursor.getLong(idIndex);
            int lookupKeyIndex = cursor.getColumnIndex("lookup");
            lookupKey = cursor.getString(lookupKeyIndex);
            this.mContactUri = ContactsContract.Contacts.getLookupUri(contactId, lookupKey);
            int starredIndex = cursor.getColumnIndex("starred");
            boolean z = true;
            this.mIsStarred = cursor.getInt(starredIndex) != 0;
            int hasPhoneNumIndex = cursor.getColumnIndex("has_phone_number");
            if (cursor.getInt(hasPhoneNumIndex) == 0) {
                z = false;
            }
            hasPhoneNumber = z;
            int lastUpdatedTimestampIndex = cursor.getColumnIndex("contact_last_updated_timestamp");
            if (lastUpdatedTimestampIndex >= 0) {
                this.mLastUpdatedTimestamp = cursor.getLong(lastUpdatedTimestampIndex);
            }
            found = true;
        }
        if (cursor != null) {
            cursor.close();
        }
        if (found && lookupKey != null && hasPhoneNumber) {
            return queryPhoneNumber(lookupKey);
        }
        return found;
    }

    private boolean queryPhoneNumber(String lookupKey) {
        String[] projection = {"data4"};
        String[] selectionArgs = {lookupKey};
        try {
            Cursor cursor = this.mContext.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, "lookup = ?", selectionArgs, null);
            if (cursor == null) {
                Slog.w(TAG, "Cursor is null when querying contact phone number.");
                if (cursor != null) {
                    cursor.close();
                }
                return false;
            }
            while (cursor.moveToNext()) {
                int phoneNumIdx = cursor.getColumnIndex("data4");
                if (phoneNumIdx >= 0) {
                    this.mPhoneNumber = cursor.getString(phoneNumIdx);
                }
            }
            if (cursor != null) {
                cursor.close();
            }
            return true;
        } catch (Exception e) {
            VSlog.w(TAG, "queryPhoneNumber Failed.");
            return false;
        }
    }
}