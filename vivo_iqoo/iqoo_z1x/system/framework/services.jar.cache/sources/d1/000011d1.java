package com.android.server.locksettings.recoverablekeystore.certificate;

/* loaded from: classes.dex */
public class CertValidationException extends Exception {
    public CertValidationException(String message) {
        super(message);
    }

    public CertValidationException(Exception cause) {
        super(cause);
    }

    public CertValidationException(String message, Exception cause) {
        super(message, cause);
    }
}