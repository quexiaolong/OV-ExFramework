package com.android.server.audio;

import android.content.Context;
import android.content.Intent;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.os.Binder;
import android.os.UserHandle;
import java.util.Objects;

/* loaded from: classes.dex */
public class SystemServerAdapter {
    protected final Context mContext;

    protected SystemServerAdapter(Context context) {
        this.mContext = context;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static final SystemServerAdapter getDefaultAdapter(Context context) {
        Objects.requireNonNull(context);
        return new SystemServerAdapter(context);
    }

    public boolean isPrivileged() {
        return true;
    }

    public void sendMicrophoneMuteChangedIntent() {
        this.mContext.sendBroadcastAsUser(new Intent("android.media.action.MICROPHONE_MUTE_CHANGED").setFlags(1073741824), UserHandle.ALL);
    }

    public void sendDeviceBecomingNoisyIntent() {
        if (this.mContext == null) {
            return;
        }
        Intent intent = new Intent("android.media.AUDIO_BECOMING_NOISY");
        intent.addFlags(67108864);
        intent.addFlags(AudioFormat.EVRC);
        long ident = Binder.clearCallingIdentity();
        try {
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }
}