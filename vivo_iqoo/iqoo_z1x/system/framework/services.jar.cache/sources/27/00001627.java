package com.android.server.pm;

import android.content.pm.PackageParser;
import android.content.pm.Signature;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/* JADX INFO: Access modifiers changed from: package-private */
/* compiled from: SELinuxMMAC.java */
/* loaded from: classes.dex */
public final class Policy {
    private final Set<Signature> mCerts;
    private final Map<String, String> mPkgMap;
    private final String mSeinfo;

    private Policy(PolicyBuilder builder) {
        this.mSeinfo = builder.mSeinfo;
        this.mCerts = Collections.unmodifiableSet(builder.mCerts);
        this.mPkgMap = Collections.unmodifiableMap(builder.mPkgMap);
    }

    public Set<Signature> getSignatures() {
        return this.mCerts;
    }

    public boolean hasInnerPackages() {
        return !this.mPkgMap.isEmpty();
    }

    public Map<String, String> getInnerPackages() {
        return this.mPkgMap;
    }

    public boolean hasGlobalSeinfo() {
        return this.mSeinfo != null;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Signature cert : this.mCerts) {
            sb.append("cert=" + cert.toCharsString().substring(0, 11) + "... ");
        }
        if (this.mSeinfo != null) {
            sb.append("seinfo=" + this.mSeinfo);
        }
        for (String name : this.mPkgMap.keySet()) {
            sb.append(" " + name + "=" + this.mPkgMap.get(name));
        }
        return sb.toString();
    }

    public String getMatchedSeInfo(AndroidPackage pkg) {
        Signature[] certs = (Signature[]) this.mCerts.toArray(new Signature[0]);
        if (pkg.getSigningDetails() != PackageParser.SigningDetails.UNKNOWN && !Signature.areExactMatch(certs, pkg.getSigningDetails().signatures) && (certs.length > 1 || !pkg.getSigningDetails().hasCertificate(certs[0]))) {
            return null;
        }
        String seinfoValue = this.mPkgMap.get(pkg.getPackageName());
        if (seinfoValue != null) {
            return seinfoValue;
        }
        return this.mSeinfo;
    }

    /* compiled from: SELinuxMMAC.java */
    /* loaded from: classes.dex */
    public static final class PolicyBuilder {
        private final Set<Signature> mCerts = new HashSet(2);
        private final Map<String, String> mPkgMap = new HashMap(2);
        private String mSeinfo;

        public PolicyBuilder addSignature(String cert) {
            if (cert == null) {
                String err = "Invalid signature value " + cert;
                throw new IllegalArgumentException(err);
            }
            this.mCerts.add(new Signature(cert));
            return this;
        }

        public PolicyBuilder setGlobalSeinfoOrThrow(String seinfo) {
            if (!validateValue(seinfo)) {
                String err = "Invalid seinfo value " + seinfo;
                throw new IllegalArgumentException(err);
            }
            String str = this.mSeinfo;
            if (str != null && !str.equals(seinfo)) {
                throw new IllegalStateException("Duplicate seinfo tag found");
            }
            this.mSeinfo = seinfo;
            return this;
        }

        public PolicyBuilder addInnerPackageMapOrThrow(String pkgName, String seinfo) {
            if (!validateValue(pkgName)) {
                String err = "Invalid package name " + pkgName;
                throw new IllegalArgumentException(err);
            } else if (!validateValue(seinfo)) {
                String err2 = "Invalid seinfo value " + seinfo;
                throw new IllegalArgumentException(err2);
            } else {
                String pkgValue = this.mPkgMap.get(pkgName);
                if (pkgValue != null && !pkgValue.equals(seinfo)) {
                    throw new IllegalStateException("Conflicting seinfo value found");
                }
                this.mPkgMap.put(pkgName, seinfo);
                return this;
            }
        }

        private boolean validateValue(String name) {
            if (name == null || !name.matches("\\A[\\.\\w]+\\z")) {
                return false;
            }
            return true;
        }

        public Policy build() {
            Policy p = new Policy(this);
            if (!p.mCerts.isEmpty()) {
                if (!((p.mSeinfo == null) ^ p.mPkgMap.isEmpty())) {
                    throw new IllegalStateException("Only seinfo tag XOR package tags are allowed within a signer stanza.");
                }
                return p;
            }
            throw new IllegalStateException("Missing certs with signer tag. Expecting at least one.");
        }
    }
}