package com.android.server.rollback;

import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.IBinder;
import java.util.function.Consumer;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public class LocalIntentReceiver {
    final Consumer<Intent> mConsumer;
    private IIntentSender.Stub mLocalSender = new IIntentSender.Stub() { // from class: com.android.server.rollback.LocalIntentReceiver.1
        public void send(int code, Intent intent, String resolvedType, IBinder whitelistToken, IIntentReceiver finishedReceiver, String requiredPermission, Bundle options) {
            LocalIntentReceiver.this.mConsumer.accept(intent);
        }
    };

    /* JADX INFO: Access modifiers changed from: package-private */
    public LocalIntentReceiver(Consumer<Intent> consumer) {
        this.mConsumer = consumer;
    }

    public IntentSender getIntentSender() {
        return new IntentSender(this.mLocalSender);
    }
}