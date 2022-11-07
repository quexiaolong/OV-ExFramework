package com.android.server.broadcastradio.hal1;

import android.hardware.radio.ITunerCallback;
import android.hardware.radio.ProgramList;
import android.hardware.radio.ProgramSelector;
import android.hardware.radio.RadioManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class TunerCallback implements ITunerCallback {
    private static final String TAG = "BroadcastRadioService.TunerCallback";
    private final ITunerCallback mClientCallback;
    private final long mNativeContext;
    private final Tuner mTuner;
    private final AtomicReference<ProgramList.Filter> mProgramListFilter = new AtomicReference<>();
    private boolean mInitialConfigurationDone = false;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public interface RunnableThrowingRemoteException {
        void run() throws RemoteException;
    }

    private native void nativeDetach(long j);

    private native void nativeFinalize(long j);

    private native long nativeInit(Tuner tuner, int i);

    /* JADX INFO: Access modifiers changed from: package-private */
    public TunerCallback(Tuner tuner, ITunerCallback clientCallback, int halRev) {
        this.mTuner = tuner;
        this.mClientCallback = clientCallback;
        this.mNativeContext = nativeInit(tuner, halRev);
    }

    protected void finalize() throws Throwable {
        nativeFinalize(this.mNativeContext);
        super.finalize();
    }

    public void detach() {
        nativeDetach(this.mNativeContext);
    }

    private void dispatch(RunnableThrowingRemoteException func) {
        try {
            func.run();
        } catch (RemoteException e) {
            Slog.e(TAG, "client died", e);
        }
    }

    private void handleHwFailure() {
        onError(0);
        this.mTuner.close();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void startProgramListUpdates(ProgramList.Filter filter) {
        if (filter == null) {
            filter = new ProgramList.Filter();
        }
        this.mProgramListFilter.set(filter);
        sendProgramListUpdate();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void stopProgramListUpdates() {
        this.mProgramListFilter.set(null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isInitialConfigurationDone() {
        return this.mInitialConfigurationDone;
    }

    public /* synthetic */ void lambda$onError$0$TunerCallback(int status) throws RemoteException {
        this.mClientCallback.onError(status);
    }

    public void onError(final int status) {
        dispatch(new RunnableThrowingRemoteException() { // from class: com.android.server.broadcastradio.hal1.-$$Lambda$TunerCallback$QwopTG5nMx1CO2s6KecqSuCqviA
            @Override // com.android.server.broadcastradio.hal1.TunerCallback.RunnableThrowingRemoteException
            public final void run() {
                TunerCallback.this.lambda$onError$0$TunerCallback(status);
            }
        });
    }

    public void onTuneFailed(int result, ProgramSelector selector) {
        Slog.e(TAG, "Not applicable for HAL 1.x");
    }

    public void onConfigurationChanged(final RadioManager.BandConfig config) {
        this.mInitialConfigurationDone = true;
        dispatch(new RunnableThrowingRemoteException() { // from class: com.android.server.broadcastradio.hal1.-$$Lambda$TunerCallback$qR-bdRNnpcaEQYaUWeumt5lHhtY
            @Override // com.android.server.broadcastradio.hal1.TunerCallback.RunnableThrowingRemoteException
            public final void run() {
                TunerCallback.this.lambda$onConfigurationChanged$1$TunerCallback(config);
            }
        });
    }

    public /* synthetic */ void lambda$onConfigurationChanged$1$TunerCallback(RadioManager.BandConfig config) throws RemoteException {
        this.mClientCallback.onConfigurationChanged(config);
    }

    public /* synthetic */ void lambda$onCurrentProgramInfoChanged$2$TunerCallback(RadioManager.ProgramInfo info) throws RemoteException {
        this.mClientCallback.onCurrentProgramInfoChanged(info);
    }

    public void onCurrentProgramInfoChanged(final RadioManager.ProgramInfo info) {
        dispatch(new RunnableThrowingRemoteException() { // from class: com.android.server.broadcastradio.hal1.-$$Lambda$TunerCallback$yDfY5pWuRHaQpNiYhPjLkNUUrc0
            @Override // com.android.server.broadcastradio.hal1.TunerCallback.RunnableThrowingRemoteException
            public final void run() {
                TunerCallback.this.lambda$onCurrentProgramInfoChanged$2$TunerCallback(info);
            }
        });
    }

    public /* synthetic */ void lambda$onTrafficAnnouncement$3$TunerCallback(boolean active) throws RemoteException {
        this.mClientCallback.onTrafficAnnouncement(active);
    }

    public void onTrafficAnnouncement(final boolean active) {
        dispatch(new RunnableThrowingRemoteException() { // from class: com.android.server.broadcastradio.hal1.-$$Lambda$TunerCallback$nm8WiKzJMmmFFCbXZdjr71O3V8Q
            @Override // com.android.server.broadcastradio.hal1.TunerCallback.RunnableThrowingRemoteException
            public final void run() {
                TunerCallback.this.lambda$onTrafficAnnouncement$3$TunerCallback(active);
            }
        });
    }

    public /* synthetic */ void lambda$onEmergencyAnnouncement$4$TunerCallback(boolean active) throws RemoteException {
        this.mClientCallback.onEmergencyAnnouncement(active);
    }

    public void onEmergencyAnnouncement(final boolean active) {
        dispatch(new RunnableThrowingRemoteException() { // from class: com.android.server.broadcastradio.hal1.-$$Lambda$TunerCallback$-h4udaDmWtN-rprVGi_U0x7oSJc
            @Override // com.android.server.broadcastradio.hal1.TunerCallback.RunnableThrowingRemoteException
            public final void run() {
                TunerCallback.this.lambda$onEmergencyAnnouncement$4$TunerCallback(active);
            }
        });
    }

    public /* synthetic */ void lambda$onAntennaState$5$TunerCallback(boolean connected) throws RemoteException {
        this.mClientCallback.onAntennaState(connected);
    }

    public void onAntennaState(final boolean connected) {
        dispatch(new RunnableThrowingRemoteException() { // from class: com.android.server.broadcastradio.hal1.-$$Lambda$TunerCallback$QNBMPvImBEGMe4jaw6iOF4QPjns
            @Override // com.android.server.broadcastradio.hal1.TunerCallback.RunnableThrowingRemoteException
            public final void run() {
                TunerCallback.this.lambda$onAntennaState$5$TunerCallback(connected);
            }
        });
    }

    public /* synthetic */ void lambda$onBackgroundScanAvailabilityChange$6$TunerCallback(boolean isAvailable) throws RemoteException {
        this.mClientCallback.onBackgroundScanAvailabilityChange(isAvailable);
    }

    public void onBackgroundScanAvailabilityChange(final boolean isAvailable) {
        dispatch(new RunnableThrowingRemoteException() { // from class: com.android.server.broadcastradio.hal1.-$$Lambda$TunerCallback$ndOBpfBmClsz77tzZfe3mvcA1lI
            @Override // com.android.server.broadcastradio.hal1.TunerCallback.RunnableThrowingRemoteException
            public final void run() {
                TunerCallback.this.lambda$onBackgroundScanAvailabilityChange$6$TunerCallback(isAvailable);
            }
        });
    }

    public /* synthetic */ void lambda$onBackgroundScanComplete$7$TunerCallback() throws RemoteException {
        this.mClientCallback.onBackgroundScanComplete();
    }

    public void onBackgroundScanComplete() {
        dispatch(new RunnableThrowingRemoteException() { // from class: com.android.server.broadcastradio.hal1.-$$Lambda$TunerCallback$YlDkqdeYbHPdKcgZh23aJ5Yw8mg
            @Override // com.android.server.broadcastradio.hal1.TunerCallback.RunnableThrowingRemoteException
            public final void run() {
                TunerCallback.this.lambda$onBackgroundScanComplete$7$TunerCallback();
            }
        });
    }

    public /* synthetic */ void lambda$onProgramListChanged$8$TunerCallback() throws RemoteException {
        this.mClientCallback.onProgramListChanged();
    }

    public void onProgramListChanged() {
        dispatch(new RunnableThrowingRemoteException() { // from class: com.android.server.broadcastradio.hal1.-$$Lambda$TunerCallback$mdqODkiuJlYCJRXqdXBC-d6vdp4
            @Override // com.android.server.broadcastradio.hal1.TunerCallback.RunnableThrowingRemoteException
            public final void run() {
                TunerCallback.this.lambda$onProgramListChanged$8$TunerCallback();
            }
        });
        sendProgramListUpdate();
    }

    private void sendProgramListUpdate() {
        ProgramList.Filter filter = this.mProgramListFilter.get();
        if (filter == null) {
            return;
        }
        try {
            List<RadioManager.ProgramInfo> modified = this.mTuner.getProgramList(filter.getVendorFilter());
            Set<RadioManager.ProgramInfo> modifiedSet = (Set) modified.stream().collect(Collectors.toSet());
            final ProgramList.Chunk chunk = new ProgramList.Chunk(true, true, modifiedSet, (Set) null);
            dispatch(new RunnableThrowingRemoteException() { // from class: com.android.server.broadcastradio.hal1.-$$Lambda$TunerCallback$C_-9BcvTpHXxQ-jC-hu9LBHT0XU
                @Override // com.android.server.broadcastradio.hal1.TunerCallback.RunnableThrowingRemoteException
                public final void run() {
                    TunerCallback.this.lambda$sendProgramListUpdate$9$TunerCallback(chunk);
                }
            });
        } catch (IllegalStateException e) {
            Slog.d(TAG, "Program list not ready yet");
        }
    }

    public /* synthetic */ void lambda$sendProgramListUpdate$9$TunerCallback(ProgramList.Chunk chunk) throws RemoteException {
        this.mClientCallback.onProgramListUpdated(chunk);
    }

    public /* synthetic */ void lambda$onProgramListUpdated$10$TunerCallback(ProgramList.Chunk chunk) throws RemoteException {
        this.mClientCallback.onProgramListUpdated(chunk);
    }

    public void onProgramListUpdated(final ProgramList.Chunk chunk) {
        dispatch(new RunnableThrowingRemoteException() { // from class: com.android.server.broadcastradio.hal1.-$$Lambda$TunerCallback$yVJR7oPW6kDozlkthdDAOaT7L-4
            @Override // com.android.server.broadcastradio.hal1.TunerCallback.RunnableThrowingRemoteException
            public final void run() {
                TunerCallback.this.lambda$onProgramListUpdated$10$TunerCallback(chunk);
            }
        });
    }

    public void onParametersUpdated(Map parameters) {
        Slog.e(TAG, "Not applicable for HAL 1.x");
    }

    public IBinder asBinder() {
        throw new RuntimeException("Not a binder");
    }
}