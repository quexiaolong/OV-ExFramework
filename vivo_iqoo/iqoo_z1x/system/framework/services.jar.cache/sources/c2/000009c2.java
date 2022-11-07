package com.android.server.backup;

import android.app.backup.BlobBackupHelper;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.people.PeopleServiceInternal;

/* loaded from: classes.dex */
class PeopleBackupHelper extends BlobBackupHelper {
    private static final boolean DEBUG = false;
    private static final String KEY_CONVERSATIONS = "people_conversation_infos";
    private static final int STATE_VERSION = 1;
    private static final String TAG = PeopleBackupHelper.class.getSimpleName();
    private final int mUserId;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PeopleBackupHelper(int userId) {
        super(1, new String[]{KEY_CONVERSATIONS});
        this.mUserId = userId;
    }

    protected byte[] getBackupPayload(String key) {
        if (!KEY_CONVERSATIONS.equals(key)) {
            String str = TAG;
            Slog.w(str, "Unexpected backup key " + key);
            return new byte[0];
        }
        PeopleServiceInternal ps = (PeopleServiceInternal) LocalServices.getService(PeopleServiceInternal.class);
        return ps.getBackupPayload(this.mUserId);
    }

    protected void applyRestoredPayload(String key, byte[] payload) {
        if (!KEY_CONVERSATIONS.equals(key)) {
            String str = TAG;
            Slog.w(str, "Unexpected restore key " + key);
            return;
        }
        PeopleServiceInternal ps = (PeopleServiceInternal) LocalServices.getService(PeopleServiceInternal.class);
        ps.restore(this.mUserId, payload);
    }
}