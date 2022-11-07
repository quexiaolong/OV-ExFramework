package com.android.server.locksettings.recoverablekeystore.storage;

import android.util.SparseArray;
import com.android.server.locksettings.recoverablekeystore.storage.RecoverySessionStorage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Predicate;
import javax.security.auth.Destroyable;

/* loaded from: classes.dex */
public class RecoverySessionStorage implements Destroyable {
    private final SparseArray<ArrayList<Entry>> mSessionsByUid = new SparseArray<>();

    public Entry get(int uid, String sessionId) {
        ArrayList<Entry> userEntries = this.mSessionsByUid.get(uid);
        if (userEntries == null) {
            return null;
        }
        Iterator<Entry> it = userEntries.iterator();
        while (it.hasNext()) {
            Entry entry = it.next();
            if (sessionId.equals(entry.mSessionId)) {
                return entry;
            }
        }
        return null;
    }

    public void add(int uid, Entry entry) {
        if (this.mSessionsByUid.get(uid) == null) {
            this.mSessionsByUid.put(uid, new ArrayList<>());
        }
        this.mSessionsByUid.get(uid).add(entry);
    }

    public void remove(int uid, final String sessionId) {
        if (this.mSessionsByUid.get(uid) == null) {
            return;
        }
        this.mSessionsByUid.get(uid).removeIf(new Predicate() { // from class: com.android.server.locksettings.recoverablekeystore.storage.-$$Lambda$RecoverySessionStorage$1ayqf2qqdJH00fvbhBUKWso4cdc
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return RecoverySessionStorage.lambda$remove$0(sessionId, (RecoverySessionStorage.Entry) obj);
            }
        });
    }

    public static /* synthetic */ boolean lambda$remove$0(String sessionId, Entry session) {
        return session.mSessionId.equals(sessionId);
    }

    public void remove(int uid) {
        ArrayList<Entry> entries = this.mSessionsByUid.get(uid);
        if (entries == null) {
            return;
        }
        Iterator<Entry> it = entries.iterator();
        while (it.hasNext()) {
            Entry entry = it.next();
            entry.destroy();
        }
        this.mSessionsByUid.remove(uid);
    }

    public int size() {
        int size = 0;
        int numberOfUsers = this.mSessionsByUid.size();
        for (int i = 0; i < numberOfUsers; i++) {
            ArrayList<Entry> entries = this.mSessionsByUid.valueAt(i);
            size += entries.size();
        }
        return size;
    }

    @Override // javax.security.auth.Destroyable
    public void destroy() {
        int numberOfUids = this.mSessionsByUid.size();
        for (int i = 0; i < numberOfUids; i++) {
            ArrayList<Entry> entries = this.mSessionsByUid.valueAt(i);
            Iterator<Entry> it = entries.iterator();
            while (it.hasNext()) {
                Entry entry = it.next();
                entry.destroy();
            }
        }
    }

    /* loaded from: classes.dex */
    public static class Entry implements Destroyable {
        private final byte[] mKeyClaimant;
        private final byte[] mLskfHash;
        private final String mSessionId;
        private final byte[] mVaultParams;

        public Entry(String sessionId, byte[] lskfHash, byte[] keyClaimant, byte[] vaultParams) {
            this.mLskfHash = lskfHash;
            this.mSessionId = sessionId;
            this.mKeyClaimant = keyClaimant;
            this.mVaultParams = vaultParams;
        }

        public byte[] getLskfHash() {
            return this.mLskfHash;
        }

        public byte[] getKeyClaimant() {
            return this.mKeyClaimant;
        }

        public byte[] getVaultParams() {
            return this.mVaultParams;
        }

        @Override // javax.security.auth.Destroyable
        public void destroy() {
            Arrays.fill(this.mLskfHash, (byte) 0);
            Arrays.fill(this.mKeyClaimant, (byte) 0);
        }
    }
}