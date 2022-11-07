package com.android.server.am.frozen;

import android.os.Looper;

/* loaded from: classes.dex */
public class AudioState extends BaseState {
    public static final long MAX_AUDIO_DELAY = 60000;
    public static final long MIN_AUDIO_DELAY = 20000;

    public AudioState(Looper looper, int state) {
        super(looper, state);
    }

    @Override // com.android.server.am.frozen.BaseState
    protected long deferredTime(long duration) {
        long min = Math.min(duration / 12, 60000L);
        long max = min >= 20000 ? min : 20000L;
        return max > duration ? duration : max;
    }
}