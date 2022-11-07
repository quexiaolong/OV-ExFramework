package com.android.server.backup.transport;

import com.android.internal.backup.IBackupTransport;

/* loaded from: classes.dex */
public interface TransportConnectionListener {
    void onTransportConnectionResult(IBackupTransport iBackupTransport, TransportClient transportClient);
}