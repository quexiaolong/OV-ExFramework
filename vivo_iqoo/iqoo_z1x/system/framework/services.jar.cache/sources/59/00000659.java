package com.android.server.accounts;

import android.accounts.Account;
import android.util.LruCache;
import android.util.Pair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class TokenCache {
    private static final int MAX_CACHE_CHARS = 64000;
    private TokenLruCache mCachedTokens = new TokenLruCache();

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Value {
        public final long expiryEpochMillis;
        public final String token;

        public Value(String token, long expiryEpochMillis) {
            this.token = token;
            this.expiryEpochMillis = expiryEpochMillis;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Key {
        public final Account account;
        public final String packageName;
        public final byte[] sigDigest;
        public final String tokenType;

        public Key(Account account, String tokenType, String packageName, byte[] sigDigest) {
            this.account = account;
            this.tokenType = tokenType;
            this.packageName = packageName;
            this.sigDigest = sigDigest;
        }

        public boolean equals(Object o) {
            if (o == null || !(o instanceof Key)) {
                return false;
            }
            Key cacheKey = (Key) o;
            return Objects.equals(this.account, cacheKey.account) && Objects.equals(this.packageName, cacheKey.packageName) && Objects.equals(this.tokenType, cacheKey.tokenType) && Arrays.equals(this.sigDigest, cacheKey.sigDigest);
        }

        public int hashCode() {
            return ((this.account.hashCode() ^ this.packageName.hashCode()) ^ this.tokenType.hashCode()) ^ Arrays.hashCode(this.sigDigest);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class TokenLruCache extends LruCache<Key, Value> {
        private HashMap<Account, Evictor> mAccountEvictors;
        private HashMap<Pair<String, String>, Evictor> mTokenEvictors;

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public class Evictor {
            private final List<Key> mKeys = new ArrayList();

            public Evictor() {
            }

            public void add(Key k) {
                this.mKeys.add(k);
            }

            public void evict() {
                for (Key k : this.mKeys) {
                    TokenLruCache.this.remove(k);
                }
            }
        }

        public TokenLruCache() {
            super(TokenCache.MAX_CACHE_CHARS);
            this.mTokenEvictors = new HashMap<>();
            this.mAccountEvictors = new HashMap<>();
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.util.LruCache
        public int sizeOf(Key k, Value v) {
            return v.token.length();
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.util.LruCache
        public void entryRemoved(boolean evicted, Key k, Value oldVal, Value newVal) {
            Evictor evictor;
            if (oldVal != null && newVal == null && (evictor = this.mTokenEvictors.remove(new Pair(k.account.type, oldVal.token))) != null) {
                evictor.evict();
            }
        }

        public void putToken(Key k, Value v) {
            Pair<String, String> mapKey = new Pair<>(k.account.type, v.token);
            Evictor tokenEvictor = this.mTokenEvictors.get(mapKey);
            if (tokenEvictor == null) {
                tokenEvictor = new Evictor();
            }
            tokenEvictor.add(k);
            this.mTokenEvictors.put(mapKey, tokenEvictor);
            Evictor accountEvictor = this.mAccountEvictors.get(k.account);
            if (accountEvictor == null) {
                accountEvictor = new Evictor();
            }
            accountEvictor.add(k);
            this.mAccountEvictors.put(k.account, tokenEvictor);
            put(k, v);
        }

        public void evict(String accountType, String token) {
            Evictor evictor = this.mTokenEvictors.get(new Pair(accountType, token));
            if (evictor != null) {
                evictor.evict();
            }
        }

        public void evict(Account account) {
            Evictor evictor = this.mAccountEvictors.get(account);
            if (evictor != null) {
                evictor.evict();
            }
        }
    }

    public void put(Account account, String token, String tokenType, String packageName, byte[] sigDigest, long expiryMillis) {
        Objects.requireNonNull(account);
        if (token == null || System.currentTimeMillis() > expiryMillis) {
            return;
        }
        Key k = new Key(account, tokenType, packageName, sigDigest);
        Value v = new Value(token, expiryMillis);
        this.mCachedTokens.putToken(k, v);
    }

    public void remove(String accountType, String token) {
        this.mCachedTokens.evict(accountType, token);
    }

    public void remove(Account account) {
        this.mCachedTokens.evict(account);
    }

    public String get(Account account, String tokenType, String packageName, byte[] sigDigest) {
        Key k = new Key(account, tokenType, packageName, sigDigest);
        Value v = this.mCachedTokens.get(k);
        long currentTime = System.currentTimeMillis();
        if (v != null && currentTime < v.expiryEpochMillis) {
            return v.token;
        }
        if (v != null) {
            remove(account.type, v.token);
            return null;
        }
        return null;
    }
}