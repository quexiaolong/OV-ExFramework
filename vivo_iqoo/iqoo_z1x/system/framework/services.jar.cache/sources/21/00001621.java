package com.android.server.pm;

/* loaded from: classes.dex */
public class PackageVerificationResponse {
    public final int callerUid;
    public final int code;

    public PackageVerificationResponse(int code, int callerUid) {
        this.code = code;
        this.callerUid = callerUid;
    }
}