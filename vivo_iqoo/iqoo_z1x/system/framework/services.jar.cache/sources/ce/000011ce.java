package com.android.server.locksettings.recoverablekeystore.certificate;

/* loaded from: classes.dex */
public class CertParsingException extends Exception {
    public CertParsingException(String message) {
        super(message);
    }

    public CertParsingException(Exception cause) {
        super(cause);
    }

    public CertParsingException(String message, Exception cause) {
        super(message, cause);
    }
}