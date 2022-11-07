package com.android.server.audio;

import android.bluetooth.BluetoothDevice;
import android.os.IBinder;
import android.os.Message;
import java.io.PrintWriter;

/* loaded from: classes.dex */
public interface IVivoAudioService {

    /* loaded from: classes.dex */
    public interface IVivoVolumeStreamState {
    }

    void PomModeChangeBroadcast(int i, int i2, int i3, IBinder iBinder);

    void PomRemoveSetModeDeathHandler(int i);

    void applyStreamVolumeDeltaIndexHook(int i, boolean z, int i2);

    void checkDeltaVolumeForAdjustStream(int i, int i2, int i3, int i4, int i5, int i6, int i7, String str);

    void checkDeltaVolumeForSetStream(int i, int i2, int i3, String str);

    int checkGetMode(int i);

    boolean checkSetSpeakerAllow(IBinder iBinder, int i, String str, int i2, boolean z);

    boolean checkSetStreamVolumeAllow(int i, int i2, int i3, String str, int i4);

    boolean createVivoStreamStates(int i, int[] iArr);

    void dummy();

    void dumpVivoStreamState(PrintWriter printWriter, int i);

    int getAbsVolumeIndexByVivo(int i, int i2);

    void getBluetoothType(BluetoothDevice bluetoothDevice);

    int getDeltaVolumeForStream(int i, int i2);

    int getIntForUser(String str, int i, int i2);

    int getRingerModeExternalAsUser(int i);

    int getStreamVolumeDelta(int i);

    int getStreamVolumeMaxDelta(int i);

    boolean getVibeInSilentFromVivo();

    int getVibrateSettingAsUser(int i, int i2);

    boolean handleMessageExt(Message message);

    boolean isDialingOrInCall(String str);

    void landFillLoudVolume();

    void landFillNewReceiver(int i, String str);

    void notifyAppSharePackageChanged(String str, int i);

    void playSoundEffectVolumeWithApp(int i, float f, int i2, String str);

    void playerEvent(int i, int i2, int i3, int i4);

    void processVolumeEventForVivoAudio(int i, int i2);

    void putIntForUser(String str, int i, int i2);

    boolean readCameraSoundForcedFromVivoInt();

    int readPersistedVibrateSettingVivoAsUser(int i, boolean z, int i2);

    void readRestrictionPolicy();

    void restoreStreamVolumeMaxMin();

    void setMode(int i, IBinder iBinder, String str);

    void setMode_Pom(int i, int i2, String str, IBinder iBinder);

    void setRingerModeExtAsUser(int i, int i2);

    void setStreamVolumeDelta(int i, int i2, int i3, String str);

    void setStreamVolumeDeltaIndex(int i, int i2, int i3, boolean z);

    boolean setVibrateSettingAsUser(int i, int i2, boolean z, int i3);

    boolean shouldBlockSetStreamVolumeForAppShare(String str, int i);

    void updateAppShareInputHandle(boolean z);

    /* loaded from: classes.dex */
    public interface IAudioServiceExport {
        IVivoAudioService getIVivoAudioServiceInstance();

        default void dummyExport() {
            if (getIVivoAudioServiceInstance() != null) {
                getIVivoAudioServiceInstance().dummy();
            }
        }

        default int getStreamVolumeDelta(int streamType) {
            if (getIVivoAudioServiceInstance() != null) {
                return getIVivoAudioServiceInstance().getStreamVolumeDelta(streamType);
            }
            return -1;
        }

        default void setStreamVolumeDelta(int streamType, int index, int flags, String callingPackage) {
            if (getIVivoAudioServiceInstance() != null) {
                getIVivoAudioServiceInstance().setStreamVolumeDelta(streamType, index, flags, callingPackage);
            }
        }

        default int getStreamVolumeMaxDelta(int streamType) {
            if (getIVivoAudioServiceInstance() != null) {
                return getIVivoAudioServiceInstance().getStreamVolumeMaxDelta(streamType);
            }
            return -1;
        }
    }
}