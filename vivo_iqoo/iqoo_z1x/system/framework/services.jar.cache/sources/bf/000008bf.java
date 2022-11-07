package com.android.server.audio;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioRecordingConfiguration;
import android.media.AudioSystem;
import android.media.IRecordingConfigDispatcher;
import android.media.MediaRecorder;
import android.media.audiofx.AudioEffect;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.android.server.audio.AudioEventLogger;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/* loaded from: classes.dex */
public final class RecordingActivityMonitor implements AudioSystem.AudioRecordingCallback {
    public static final String TAG = "AudioService.RecordingActivityMonitor";
    private static final AudioEventLogger sEventLogger = new AudioEventLogger(50, "recording activity received by AudioService");
    private final PackageManager mPackMan;
    private ArrayList<RecMonitorClient> mClients = new ArrayList<>();
    private boolean mHasPublicClients = false;
    private AtomicInteger mLegacyRemoteSubmixRiid = new AtomicInteger(-1);
    private AtomicBoolean mLegacyRemoteSubmixActive = new AtomicBoolean(false);
    private List<RecordingState> mRecordStates = new ArrayList();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class RecordingState {
        private AudioRecordingConfiguration mConfig;
        private final RecorderDeathHandler mDeathHandler;
        private boolean mIsActive;
        private final int mRiid;

        RecordingState(int riid, RecorderDeathHandler handler) {
            this.mRiid = riid;
            this.mDeathHandler = handler;
        }

        RecordingState(AudioRecordingConfiguration config) {
            this.mRiid = -1;
            this.mDeathHandler = null;
            this.mConfig = config;
        }

        int getRiid() {
            return this.mRiid;
        }

        int getPortId() {
            AudioRecordingConfiguration audioRecordingConfiguration = this.mConfig;
            if (audioRecordingConfiguration != null) {
                return audioRecordingConfiguration.getClientPortId();
            }
            return -1;
        }

        AudioRecordingConfiguration getConfig() {
            return this.mConfig;
        }

        boolean hasDeathHandler() {
            return this.mDeathHandler != null;
        }

        boolean isActiveConfiguration() {
            return this.mIsActive && this.mConfig != null;
        }

        void release() {
            RecorderDeathHandler recorderDeathHandler = this.mDeathHandler;
            if (recorderDeathHandler != null) {
                recorderDeathHandler.release();
            }
        }

        boolean setActive(boolean active) {
            if (this.mIsActive == active) {
                return false;
            }
            this.mIsActive = active;
            return this.mConfig != null;
        }

        boolean setConfig(AudioRecordingConfiguration config) {
            if (config.equals(this.mConfig)) {
                return false;
            }
            this.mConfig = config;
            return this.mIsActive;
        }

        void dump(PrintWriter pw) {
            pw.println("riid " + this.mRiid + "; active? " + this.mIsActive);
            AudioRecordingConfiguration audioRecordingConfiguration = this.mConfig;
            if (audioRecordingConfiguration != null) {
                audioRecordingConfiguration.dump(pw);
            } else {
                pw.println("  no config");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public RecordingActivityMonitor(Context ctxt) {
        RecMonitorClient.sMonitor = this;
        RecorderDeathHandler.sMonitor = this;
        this.mPackMan = ctxt.getPackageManager();
    }

    public void onRecordingConfigurationChanged(int event, int riid, int uid, int session, int source, int portId, boolean silenced, int[] recordingInfo, AudioEffect.Descriptor[] clientEffects, AudioEffect.Descriptor[] effects, int activeSource, String packName) {
        AudioDeviceInfo device;
        AudioRecordingConfiguration config = createRecordingConfiguration(uid, session, source, recordingInfo, portId, silenced, activeSource, clientEffects, effects);
        if (source == 8 && ((event == 0 || event == 2) && (device = config.getAudioDevice()) != null && "0".equals(device.getAddress()))) {
            this.mLegacyRemoteSubmixRiid.set(riid);
            this.mLegacyRemoteSubmixActive.set(true);
        }
        dispatchCallbacks(updateSnapshot(event, riid, config));
    }

    public int trackRecorder(IBinder recorder) {
        if (recorder == null) {
            Log.e(TAG, "trackRecorder called with null token");
            return -1;
        }
        int newRiid = AudioSystem.newAudioRecorderId();
        RecorderDeathHandler handler = new RecorderDeathHandler(newRiid, recorder);
        if (!handler.init()) {
            return -1;
        }
        synchronized (this.mRecordStates) {
            this.mRecordStates.add(new RecordingState(newRiid, handler));
        }
        return newRiid;
    }

    public void recorderEvent(int riid, int event) {
        int configEvent = 0;
        if (this.mLegacyRemoteSubmixRiid.get() == riid) {
            this.mLegacyRemoteSubmixActive.set(event == 0);
        }
        if (event != 0) {
            if (event != 1) {
                configEvent = -1;
            } else {
                configEvent = 1;
            }
        }
        if (riid == -1 || configEvent == -1) {
            sEventLogger.log(new RecordingEvent(event, riid, null).printLog(TAG));
        } else {
            dispatchCallbacks(updateSnapshot(configEvent, riid, null));
        }
    }

    public void releaseRecorder(int riid) {
        dispatchCallbacks(updateSnapshot(3, riid, null));
    }

    public boolean isRecordingActiveForUid(int uid) {
        synchronized (this.mRecordStates) {
            for (RecordingState state : this.mRecordStates) {
                if (state.isActiveConfiguration() && state.getConfig().getClientUid() == uid) {
                    return true;
                }
            }
            return false;
        }
    }

    private void dispatchCallbacks(List<AudioRecordingConfiguration> configs) {
        List<AudioRecordingConfiguration> configsPublic;
        if (configs == null) {
            return;
        }
        synchronized (this.mClients) {
            if (this.mHasPublicClients) {
                configsPublic = anonymizeForPublicConsumption(configs);
            } else {
                configsPublic = new ArrayList<>();
            }
            Iterator<RecMonitorClient> it = this.mClients.iterator();
            while (it.hasNext()) {
                RecMonitorClient rmc = it.next();
                try {
                    if (rmc.mIsPrivileged) {
                        rmc.mDispatcherCb.dispatchRecordingConfigChange(configs);
                    } else {
                        rmc.mDispatcherCb.dispatchRecordingConfigChange(configsPublic);
                    }
                } catch (RemoteException e) {
                    Log.w(TAG, "Could not call dispatchRecordingConfigChange() on client", e);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void dump(PrintWriter pw) {
        pw.println("\nRecordActivityMonitor dump time: " + DateFormat.getTimeInstance().format(new Date()));
        synchronized (this.mRecordStates) {
            for (RecordingState state : this.mRecordStates) {
                state.dump(pw);
            }
        }
        pw.println("\n");
        sEventLogger.dump(pw);
    }

    private static ArrayList<AudioRecordingConfiguration> anonymizeForPublicConsumption(List<AudioRecordingConfiguration> sysConfigs) {
        ArrayList<AudioRecordingConfiguration> publicConfigs = new ArrayList<>();
        for (AudioRecordingConfiguration config : sysConfigs) {
            publicConfigs.add(AudioRecordingConfiguration.anonymizedCopy(config));
        }
        return publicConfigs;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void initMonitor() {
        AudioSystem.setRecordingCallback(this);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onAudioServerDied() {
        List<AudioRecordingConfiguration> configs = null;
        synchronized (this.mRecordStates) {
            boolean configChanged = false;
            Iterator<RecordingState> it = this.mRecordStates.iterator();
            while (it.hasNext()) {
                RecordingState state = it.next();
                if (!state.hasDeathHandler()) {
                    if (state.isActiveConfiguration()) {
                        configChanged = true;
                        sEventLogger.log(new RecordingEvent(3, state.getRiid(), state.getConfig()));
                    }
                    it.remove();
                }
            }
            if (configChanged) {
                configs = getActiveRecordingConfigurations(true);
            }
        }
        dispatchCallbacks(configs);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerRecordingCallback(IRecordingConfigDispatcher rcdb, boolean isPrivileged) {
        if (rcdb == null) {
            return;
        }
        synchronized (this.mClients) {
            RecMonitorClient rmc = new RecMonitorClient(rcdb, isPrivileged);
            if (rmc.init()) {
                if (!isPrivileged) {
                    this.mHasPublicClients = true;
                }
                this.mClients.add(rmc);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unregisterRecordingCallback(IRecordingConfigDispatcher rcdb) {
        if (rcdb == null) {
            return;
        }
        synchronized (this.mClients) {
            Iterator<RecMonitorClient> clientIterator = this.mClients.iterator();
            boolean hasPublicClients = false;
            while (clientIterator.hasNext()) {
                RecMonitorClient rmc = clientIterator.next();
                if (rcdb.equals(rmc.mDispatcherCb)) {
                    rmc.release();
                    clientIterator.remove();
                } else if (!rmc.mIsPrivileged) {
                    hasPublicClients = true;
                }
            }
            this.mHasPublicClients = hasPublicClients;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<AudioRecordingConfiguration> getActiveRecordingConfigurations(boolean isPrivileged) {
        List<AudioRecordingConfiguration> configs = new ArrayList<>();
        synchronized (this.mRecordStates) {
            for (RecordingState state : this.mRecordStates) {
                if (state.isActiveConfiguration()) {
                    configs.add(state.getConfig());
                }
            }
        }
        if (!isPrivileged) {
            return anonymizeForPublicConsumption(configs);
        }
        return configs;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isLegacyRemoteSubmixActive() {
        return this.mLegacyRemoteSubmixActive.get();
    }

    private AudioRecordingConfiguration createRecordingConfiguration(int uid, int session, int source, int[] recordingInfo, int portId, boolean silenced, int activeSource, AudioEffect.Descriptor[] clientEffects, AudioEffect.Descriptor[] effects) {
        String packageName;
        AudioFormat clientFormat = new AudioFormat.Builder().setEncoding(recordingInfo[0]).setChannelMask(recordingInfo[1]).setSampleRate(recordingInfo[2]).build();
        AudioFormat deviceFormat = new AudioFormat.Builder().setEncoding(recordingInfo[3]).setChannelMask(recordingInfo[4]).setSampleRate(recordingInfo[5]).build();
        int patchHandle = recordingInfo[6];
        String[] packages = this.mPackMan.getPackagesForUid(uid);
        if (packages != null && packages.length > 0) {
            packageName = packages[0];
        } else {
            packageName = "";
        }
        return new AudioRecordingConfiguration(uid, session, source, clientFormat, deviceFormat, patchHandle, packageName, portId, silenced, activeSource, clientEffects, effects);
    }

    private List<AudioRecordingConfiguration> updateSnapshot(int event, int riid, AudioRecordingConfiguration config) {
        boolean configChanged;
        List<AudioRecordingConfiguration> configs = null;
        synchronized (this.mRecordStates) {
            int stateIndex = -1;
            try {
                if (riid != -1) {
                    stateIndex = findStateByRiid(riid);
                } else if (config != null) {
                    stateIndex = findStateByPortId(config.getClientPortId());
                }
                boolean z = false;
                if (stateIndex == -1) {
                    if (event == 0 && config != null) {
                        this.mRecordStates.add(new RecordingState(config));
                        stateIndex = this.mRecordStates.size() - 1;
                    } else {
                        if (config == null) {
                            Log.e(TAG, String.format("Unexpected event %d for riid %d", Integer.valueOf(event), Integer.valueOf(riid)));
                        }
                        return null;
                    }
                }
                RecordingState state = this.mRecordStates.get(stateIndex);
                if (event == 0) {
                    configChanged = state.setActive(true);
                    if (config != null) {
                        if (state.setConfig(config) || configChanged) {
                            z = true;
                        }
                        configChanged = z;
                    }
                } else if (event != 1) {
                    if (event == 2) {
                        configChanged = state.setConfig(config);
                    } else if (event == 3) {
                        configChanged = state.isActiveConfiguration();
                        state.release();
                        this.mRecordStates.remove(stateIndex);
                    } else {
                        Log.e(TAG, String.format("Unknown event %d for riid %d / portid %d", Integer.valueOf(event), Integer.valueOf(riid), Integer.valueOf(state.getPortId())));
                        configChanged = false;
                    }
                } else {
                    configChanged = state.setActive(false);
                    if (!state.hasDeathHandler()) {
                        this.mRecordStates.remove(stateIndex);
                    }
                }
                if (configChanged) {
                    sEventLogger.log(new RecordingEvent(event, riid, state.getConfig()));
                    configs = getActiveRecordingConfigurations(true);
                }
                return configs;
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private int findStateByRiid(int riid) {
        synchronized (this.mRecordStates) {
            for (int i = 0; i < this.mRecordStates.size(); i++) {
                if (this.mRecordStates.get(i).getRiid() == riid) {
                    return i;
                }
            }
            return -1;
        }
    }

    private int findStateByPortId(int portId) {
        synchronized (this.mRecordStates) {
            for (int i = 0; i < this.mRecordStates.size(); i++) {
                if (!this.mRecordStates.get(i).hasDeathHandler() && this.mRecordStates.get(i).getPortId() == portId) {
                    return i;
                }
            }
            return -1;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class RecMonitorClient implements IBinder.DeathRecipient {
        static RecordingActivityMonitor sMonitor;
        final IRecordingConfigDispatcher mDispatcherCb;
        final boolean mIsPrivileged;

        RecMonitorClient(IRecordingConfigDispatcher rcdb, boolean isPrivileged) {
            this.mDispatcherCb = rcdb;
            this.mIsPrivileged = isPrivileged;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            Log.w(RecordingActivityMonitor.TAG, "client died");
            sMonitor.unregisterRecordingCallback(this.mDispatcherCb);
        }

        boolean init() {
            try {
                this.mDispatcherCb.asBinder().linkToDeath(this, 0);
                return true;
            } catch (RemoteException e) {
                Log.w(RecordingActivityMonitor.TAG, "Could not link to client death", e);
                return false;
            }
        }

        void release() {
            this.mDispatcherCb.asBinder().unlinkToDeath(this, 0);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class RecorderDeathHandler implements IBinder.DeathRecipient {
        static RecordingActivityMonitor sMonitor;
        private final IBinder mRecorderToken;
        final int mRiid;

        RecorderDeathHandler(int riid, IBinder recorderToken) {
            this.mRiid = riid;
            this.mRecorderToken = recorderToken;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            sMonitor.releaseRecorder(this.mRiid);
        }

        boolean init() {
            try {
                this.mRecorderToken.linkToDeath(this, 0);
                return true;
            } catch (RemoteException e) {
                Log.w(RecordingActivityMonitor.TAG, "Could not link to recorder death", e);
                return false;
            }
        }

        void release() {
            this.mRecorderToken.unlinkToDeath(this, 0);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class RecordingEvent extends AudioEventLogger.Event {
        private final int mClientUid;
        private final String mPackName;
        private final int mRIId;
        private final int mRecEvent;
        private final int mSession;
        private final int mSource;

        RecordingEvent(int event, int riid, AudioRecordingConfiguration config) {
            this.mRecEvent = event;
            this.mRIId = riid;
            if (config != null) {
                this.mClientUid = config.getClientUid();
                this.mSession = config.getClientAudioSessionId();
                this.mSource = config.getClientAudioSource();
                this.mPackName = config.getClientPackageName();
                return;
            }
            this.mClientUid = -1;
            this.mSession = -1;
            this.mSource = -1;
            this.mPackName = null;
        }

        private static String recordEventToString(int recEvent) {
            if (recEvent != 0) {
                if (recEvent != 1) {
                    if (recEvent != 2) {
                        if (recEvent == 3) {
                            return "release";
                        }
                        return "unknown (" + recEvent + ")";
                    }
                    return "update";
                }
                return "stop";
            }
            return "start";
        }

        @Override // com.android.server.audio.AudioEventLogger.Event
        public String eventToString() {
            String str;
            StringBuilder sb = new StringBuilder("rec ");
            sb.append(recordEventToString(this.mRecEvent));
            sb.append(" riid:");
            sb.append(this.mRIId);
            sb.append(" uid:");
            sb.append(this.mClientUid);
            sb.append(" session:");
            sb.append(this.mSession);
            sb.append(" src:");
            sb.append(MediaRecorder.toLogFriendlyAudioSource(this.mSource));
            if (this.mPackName == null) {
                str = "";
            } else {
                str = " pack:" + this.mPackName;
            }
            sb.append(str);
            return sb.toString();
        }
    }
}