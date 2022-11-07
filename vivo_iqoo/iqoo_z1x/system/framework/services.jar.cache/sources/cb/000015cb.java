package com.android.server.pm;

import android.util.ArrayMap;
import com.android.internal.util.ArrayUtils;
import java.util.Map;

/* loaded from: classes.dex */
public class PackageKeySetData {
    static final long KEYSET_UNASSIGNED = -1;
    private final ArrayMap<String, Long> mKeySetAliases;
    private long mProperSigningKeySet;
    private long[] mUpgradeKeySets;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageKeySetData() {
        this.mKeySetAliases = new ArrayMap<>();
        this.mProperSigningKeySet = -1L;
    }

    PackageKeySetData(PackageKeySetData original) {
        this.mKeySetAliases = new ArrayMap<>();
        this.mProperSigningKeySet = original.mProperSigningKeySet;
        this.mUpgradeKeySets = ArrayUtils.cloneOrNull(original.mUpgradeKeySets);
        this.mKeySetAliases.putAll((ArrayMap<? extends String, ? extends Long>) original.mKeySetAliases);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void setProperSigningKeySet(long ks) {
        this.mProperSigningKeySet = ks;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public long getProperSigningKeySet() {
        return this.mProperSigningKeySet;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void addUpgradeKeySet(String alias) {
        if (alias == null) {
            return;
        }
        Long ks = this.mKeySetAliases.get(alias);
        if (ks != null) {
            this.mUpgradeKeySets = ArrayUtils.appendLong(this.mUpgradeKeySets, ks.longValue());
            return;
        }
        throw new IllegalArgumentException("Upgrade keyset alias " + alias + "does not refer to a defined keyset alias!");
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void addUpgradeKeySetById(long ks) {
        this.mUpgradeKeySets = ArrayUtils.appendLong(this.mUpgradeKeySets, ks);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void removeAllUpgradeKeySets() {
        this.mUpgradeKeySets = null;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public long[] getUpgradeKeySets() {
        return this.mUpgradeKeySets;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public ArrayMap<String, Long> getAliases() {
        return this.mKeySetAliases;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void setAliases(Map<String, Long> newAliases) {
        removeAllDefinedKeySets();
        this.mKeySetAliases.putAll(newAliases);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void addDefinedKeySet(long ks, String alias) {
        this.mKeySetAliases.put(alias, Long.valueOf(ks));
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void removeAllDefinedKeySets() {
        this.mKeySetAliases.erase();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isUsingDefinedKeySets() {
        return this.mKeySetAliases.size() > 0;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isUsingUpgradeKeySets() {
        long[] jArr = this.mUpgradeKeySets;
        return jArr != null && jArr.length > 0;
    }
}