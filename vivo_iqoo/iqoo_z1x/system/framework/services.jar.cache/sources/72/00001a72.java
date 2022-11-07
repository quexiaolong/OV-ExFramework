package com.android.server.uri;

import android.util.ArraySet;
import android.util.proto.ProtoOutputStream;

/* loaded from: classes2.dex */
public class NeededUriGrants {
    final int flags;
    final String targetPkg;
    final int targetUid;
    final ArraySet<GrantUri> uris = new ArraySet<>();

    public NeededUriGrants(String targetPkg, int targetUid, int flags) {
        this.targetPkg = targetPkg;
        this.targetUid = targetUid;
        this.flags = flags;
    }

    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(1138166333441L, this.targetPkg);
        proto.write(1120986464258L, this.targetUid);
        proto.write(1120986464259L, this.flags);
        int N = this.uris.size();
        for (int i = 0; i < N; i++) {
            this.uris.valueAt(i).dumpDebug(proto, 2246267895812L);
        }
        proto.end(token);
    }
}