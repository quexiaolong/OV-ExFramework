package com.android.server.utils.quota;

import android.util.proto.ProtoOutputStream;
import java.util.Objects;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class Uptc {
    private final int mHash;
    public final String packageName;
    public final String tag;
    public final int userId;

    /* JADX INFO: Access modifiers changed from: package-private */
    public Uptc(int userId, String packageName, String tag) {
        this.userId = userId;
        this.packageName = packageName;
        this.tag = tag;
        StringBuilder sb = new StringBuilder();
        sb.append((userId * 31) + (packageName.hashCode() * 31));
        sb.append(tag);
        this.mHash = sb.toString() == null ? 0 : tag.hashCode() * 31;
    }

    public String toString() {
        return string(this.userId, this.packageName, this.tag);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(1120986464257L, this.userId);
        proto.write(1138166333442L, this.packageName);
        proto.write(1138166333443L, this.tag);
        proto.end(token);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Uptc) {
            Uptc other = (Uptc) obj;
            return this.userId == other.userId && Objects.equals(this.packageName, other.packageName) && Objects.equals(this.tag, other.tag);
        }
        return false;
    }

    public int hashCode() {
        return this.mHash;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String string(int userId, String packageName, String tag) {
        String str;
        StringBuilder sb = new StringBuilder();
        sb.append("<");
        sb.append(userId);
        sb.append(">");
        sb.append(packageName);
        if (tag == null) {
            str = "";
        } else {
            str = "::" + tag;
        }
        sb.append(str);
        return sb.toString();
    }
}