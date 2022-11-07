package com.android.server.biometrics.fingerprint;

import android.hardware.audio.common.V2_0.AudioChannelMask;
import com.android.server.biometrics.Constants;

/* loaded from: classes.dex */
public class FingerprintConstants implements Constants {
    @Override // com.android.server.biometrics.Constants
    public String logTag() {
        return "FingerprintService";
    }

    @Override // com.android.server.biometrics.Constants
    public String tagHalDied() {
        return "fingerprintd_died";
    }

    @Override // com.android.server.biometrics.Constants
    public String tagAuthToken() {
        return "fingerprint_token";
    }

    @Override // com.android.server.biometrics.Constants
    public String tagAuthStartError() {
        return "fingerprintd_auth_start_error";
    }

    @Override // com.android.server.biometrics.Constants
    public String tagEnrollStartError() {
        return "fingerprintd_enroll_start_error";
    }

    @Override // com.android.server.biometrics.Constants
    public String tagEnumerateStartError() {
        return "fingerprintd_enum_start_error";
    }

    @Override // com.android.server.biometrics.Constants
    public String tagRemoveStartError() {
        return "fingerprintd_remove_start_error";
    }

    @Override // com.android.server.biometrics.Constants
    public int actionBiometricAuth() {
        return AudioChannelMask.IN_6;
    }

    @Override // com.android.server.biometrics.Constants
    public int actionBiometricEnroll() {
        return 251;
    }

    @Override // com.android.server.biometrics.Constants
    public int acquireVendorCode() {
        return 6;
    }
}