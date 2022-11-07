package com.android.server.audio;

import android.media.AudioDeviceAttributes;
import android.media.AudioSystem;

/* loaded from: classes.dex */
public class AudioSystemAdapter {
    /* JADX INFO: Access modifiers changed from: package-private */
    public static final AudioSystemAdapter getDefaultAdapter() {
        return new AudioSystemAdapter();
    }

    public int setDeviceConnectionState(int device, int state, String deviceAddress, String deviceName, int codecFormat) {
        return AudioSystem.setDeviceConnectionState(device, state, deviceAddress, deviceName, codecFormat);
    }

    public int getDeviceConnectionState(int device, String deviceAddress) {
        return AudioSystem.getDeviceConnectionState(device, deviceAddress);
    }

    public int handleDeviceConfigChange(int device, String deviceAddress, String deviceName, int codecFormat) {
        return AudioSystem.handleDeviceConfigChange(device, deviceAddress, deviceName, codecFormat);
    }

    public int setPreferredDeviceForStrategy(int strategy, AudioDeviceAttributes device) {
        return AudioSystem.setPreferredDeviceForStrategy(strategy, device);
    }

    public int removePreferredDeviceForStrategy(int strategy) {
        return AudioSystem.removePreferredDeviceForStrategy(strategy);
    }

    public int setParameters(String keyValuePairs) {
        return AudioSystem.setParameters(keyValuePairs);
    }

    public boolean isMicrophoneMuted() {
        return AudioSystem.isMicrophoneMuted();
    }

    public int muteMicrophone(boolean on) {
        return AudioSystem.muteMicrophone(on);
    }

    public int setCurrentImeUid(int uid) {
        return AudioSystem.setCurrentImeUid(uid);
    }

    public boolean isStreamActive(int stream, int inPastMs) {
        return AudioSystem.isStreamActive(stream, inPastMs);
    }
}