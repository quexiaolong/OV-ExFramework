package com.vivo.sensor.sarpower;

import vivo.app.sarpower.IVivoSarPowerState;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public abstract class VivoSarConfig extends VivoSarPowerStateController {
    private IVivoSarPowerState mSarService;

    public VivoSarConfig(IVivoSarPowerState Service) {
        this.mSarService = null;
        this.mSarService = Service;
    }

    @Override // com.vivo.sensor.sarpower.VivoSarPowerStateController
    public void registerResource() {
        try {
            if (this.mSarService != null) {
                this.mSarService.registerProximity();
                this.mSarService.setAudioCallBack();
            }
        } catch (Exception e) {
            VSlog.e("SarPowerStateService", "Exception: " + e);
        }
    }

    public byte processStateChange() {
        if (this.mSarPowerRfDetectState == 1) {
            return (byte) 0;
        }
        if (this.mCardOneState == 1 || this.mCardTwoState == 1) {
            if (this.mAudioState == 1 && this.mWIFIState == 0) {
                return (byte) 5;
            }
            if (this.mProximityState == 0 && this.mAudioState == 0 && this.mWIFIState == 0) {
                return (byte) 5;
            }
            if (this.mProximityState == 5 && this.mAudioState == 0 && this.mWIFIState == 0) {
                return (byte) 6;
            }
            if (this.mAudioState == 1 && this.mWIFIState == 1) {
                return (byte) 7;
            }
            if (this.mProximityState == 0 && this.mAudioState == 0 && this.mWIFIState == 1) {
                return (byte) 7;
            }
            if (this.mProximityState != 5 || this.mAudioState != 0 || this.mWIFIState != 1) {
                return (byte) 0;
            }
            return (byte) 8;
        } else if (this.mCardOneState != 0 && this.mCardTwoState != 0) {
            return (byte) 0;
        } else {
            if (this.mAudioState == 1 && this.mWIFIState == 0) {
                return (byte) 1;
            }
            if (this.mAudioState == 0 && this.mWIFIState == 0) {
                return (byte) 2;
            }
            if (this.mAudioState == 1 && this.mWIFIState == 1) {
                return (byte) 3;
            }
            if (this.mAudioState != 0 || this.mWIFIState != 1) {
                return (byte) 0;
            }
            return (byte) 4;
        }
    }
}