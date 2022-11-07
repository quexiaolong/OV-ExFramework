package com.android.server.notification;

import android.net.Uri;
import android.os.IBinder;
import android.os.UserHandle;
import android.util.ArraySet;

/* loaded from: classes.dex */
public final class InlineReplyUriRecord {
    private final String mKey;
    private final String mPackageName;
    private final IBinder mPermissionOwner;
    private final ArraySet<Uri> mUris = new ArraySet<>();
    private final UserHandle mUser;

    public InlineReplyUriRecord(IBinder owner, UserHandle user, String packageName, String key) {
        this.mPermissionOwner = owner;
        this.mUser = user;
        this.mPackageName = packageName;
        this.mKey = key;
    }

    public IBinder getPermissionOwner() {
        return this.mPermissionOwner;
    }

    public ArraySet<Uri> getUris() {
        return this.mUris;
    }

    public void addUri(Uri uri) {
        this.mUris.add(uri);
    }

    public int getUserId() {
        int userId = this.mUser.getIdentifier();
        if (userId == -1) {
            return 0;
        }
        return userId;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public String getKey() {
        return this.mKey;
    }
}