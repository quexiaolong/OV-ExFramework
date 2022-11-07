package com.android.server;

/* loaded from: classes.dex */
public class NativeDaemonTimeoutException extends NativeDaemonConnectorException {
    public NativeDaemonTimeoutException(String command, NativeDaemonEvent event) {
        super(command, event);
    }
}