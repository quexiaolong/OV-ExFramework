package com.android.server.pm;

/* loaded from: classes.dex */
class IntentFilterVerificationKey {
    public String className;
    public String domains;
    public String packageName;

    public IntentFilterVerificationKey(String[] domains, String packageName, String className) {
        StringBuilder sb = new StringBuilder();
        for (String host : domains) {
            sb.append(host);
        }
        this.domains = sb.toString();
        this.packageName = packageName;
        this.className = className;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IntentFilterVerificationKey that = (IntentFilterVerificationKey) o;
        String str = this.domains;
        if (str == null ? that.domains != null : !str.equals(that.domains)) {
            return false;
        }
        String str2 = this.className;
        if (str2 == null ? that.className != null : !str2.equals(that.className)) {
            return false;
        }
        String str3 = this.packageName;
        if (str3 == null ? that.packageName == null : str3.equals(that.packageName)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        String str = this.domains;
        int result = str != null ? str.hashCode() : 0;
        int i = result * 31;
        String str2 = this.packageName;
        int result2 = i + (str2 != null ? str2.hashCode() : 0);
        int result3 = result2 * 31;
        String str3 = this.className;
        return result3 + (str3 != null ? str3.hashCode() : 0);
    }
}