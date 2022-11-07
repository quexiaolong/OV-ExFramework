package com.android.server.audio;

import android.media.AudioAttributes;
import android.media.AudioFocusInfo;
import android.media.IAudioFocusDispatcher;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.android.server.audio.MediaFocusControl;
import java.io.PrintWriter;
import java.util.NoSuchElementException;

/* loaded from: classes.dex */
public class FocusRequester {
    private static final boolean DEBUG = false;
    private static final String TAG = "MediaFocusControl";
    private final AudioAttributes mAttributes;
    private final int mCallingUid;
    private final String mClientId;
    private MediaFocusControl.AudioFocusDeathHandler mDeathHandler;
    private final MediaFocusControl mFocusController;
    private IAudioFocusDispatcher mFocusDispatcher;
    private final int mFocusGainRequest;
    private final int mGrantFlags;
    private final String mPackageName;
    private final int mSdkTarget;
    private final IBinder mSourceRef;
    private FocusRequester mDuckWinner = null;
    public int mFocusLossReceived = 0;
    private boolean mFocusLossWasNotified = true;

    /* JADX INFO: Access modifiers changed from: package-private */
    public FocusRequester(AudioAttributes aa, int focusRequest, int grantFlags, IAudioFocusDispatcher afl, IBinder source, String id, MediaFocusControl.AudioFocusDeathHandler hdlr, String pn, int uid, MediaFocusControl ctlr, int sdk) {
        this.mAttributes = aa;
        this.mFocusDispatcher = afl;
        this.mSourceRef = source;
        this.mClientId = id;
        this.mDeathHandler = hdlr;
        this.mPackageName = pn;
        this.mCallingUid = uid;
        this.mFocusGainRequest = focusRequest;
        this.mGrantFlags = grantFlags;
        this.mFocusController = ctlr;
        this.mSdkTarget = sdk;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public FocusRequester(AudioFocusInfo afi, IAudioFocusDispatcher afl, IBinder source, MediaFocusControl.AudioFocusDeathHandler hdlr, MediaFocusControl ctlr) {
        this.mAttributes = afi.getAttributes();
        this.mClientId = afi.getClientId();
        this.mPackageName = afi.getPackageName();
        this.mCallingUid = afi.getClientUid();
        this.mFocusGainRequest = afi.getGainRequest();
        this.mGrantFlags = afi.getFlags();
        this.mSdkTarget = afi.getSdkTarget();
        this.mFocusDispatcher = afl;
        this.mSourceRef = source;
        this.mDeathHandler = hdlr;
        this.mFocusController = ctlr;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasSameClient(String otherClient) {
        return this.mClientId.compareTo(otherClient) == 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isLockedFocusOwner() {
        return (this.mGrantFlags & 4) != 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasSameBinder(IBinder ib) {
        IBinder iBinder = this.mSourceRef;
        return iBinder != null && iBinder.equals(ib);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasSameDispatcher(IAudioFocusDispatcher fd) {
        IAudioFocusDispatcher iAudioFocusDispatcher = this.mFocusDispatcher;
        return iAudioFocusDispatcher != null && iAudioFocusDispatcher.equals(fd);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasSamePackage(String pack) {
        return this.mPackageName.compareTo(pack) == 0;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasSameUid(int uid) {
        return this.mCallingUid == uid;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public IAudioFocusDispatcher getFocusDispatcher() {
        return this.mFocusDispatcher;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getClientUid() {
        return this.mCallingUid;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getPackageName() {
        return this.mPackageName;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getClientId() {
        return this.mClientId;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getGainRequest() {
        return this.mFocusGainRequest;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getGrantFlags() {
        return this.mGrantFlags;
    }

    AudioAttributes getAudioAttributes() {
        return this.mAttributes;
    }

    int getSdkTarget() {
        return this.mSdkTarget;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public FocusRequester getWinDuckFr() {
        return this.mDuckWinner;
    }

    private static String focusChangeToString(int focus) {
        switch (focus) {
            case -3:
                return "LOSS_TRANSIENT_CAN_DUCK";
            case -2:
                return "LOSS_TRANSIENT";
            case -1:
                return "LOSS";
            case 0:
                return "none";
            case 1:
                return "GAIN";
            case 2:
                return "GAIN_TRANSIENT";
            case 3:
                return "GAIN_TRANSIENT_MAY_DUCK";
            case 4:
                return "GAIN_TRANSIENT_EXCLUSIVE";
            default:
                return "[invalid focus change" + focus + "]";
        }
    }

    private String focusGainToString() {
        return focusChangeToString(this.mFocusGainRequest);
    }

    private String focusLossToString() {
        return focusChangeToString(this.mFocusLossReceived);
    }

    private static String flagsToString(int flags) {
        String msg = new String();
        if ((flags & 1) != 0) {
            msg = msg + "DELAY_OK";
        }
        if ((flags & 4) != 0) {
            if (!msg.isEmpty()) {
                msg = msg + "|";
            }
            msg = msg + "LOCK";
        }
        if ((flags & 2) != 0) {
            if (!msg.isEmpty()) {
                msg = msg + "|";
            }
            return msg + "PAUSES_ON_DUCKABLE_LOSS";
        }
        return msg;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw) {
        pw.println("  source:" + this.mSourceRef + " -- pack: " + this.mPackageName + " -- client: " + this.mClientId + " -- gain: " + focusGainToString() + " -- flags: " + flagsToString(this.mGrantFlags) + " -- loss: " + focusLossToString() + " -- notified: " + this.mFocusLossWasNotified + " -- uid: " + this.mCallingUid + " -- attr: " + this.mAttributes + " -- sdk:" + this.mSdkTarget);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void release() {
        IBinder srcRef = this.mSourceRef;
        MediaFocusControl.AudioFocusDeathHandler deathHdlr = this.mDeathHandler;
        this.mFocusController.unduckPlayers(this);
        if (srcRef != null && deathHdlr != null) {
            try {
                srcRef.unlinkToDeath(deathHdlr, 0);
            } catch (NoSuchElementException e) {
            }
        }
        this.mDeathHandler = null;
        this.mFocusDispatcher = null;
    }

    protected void finalize() throws Throwable {
        release();
        super.finalize();
    }

    /* JADX WARN: Code restructure failed: missing block: B:20:0x0022, code lost:
        if (r0 != 0) goto L12;
     */
    /* JADX WARN: Code restructure failed: missing block: B:25:0x002c, code lost:
        if (r0 != 0) goto L9;
     */
    /* JADX WARN: Code restructure failed: missing block: B:26:0x002e, code lost:
        android.util.Log.e(com.android.server.audio.FocusRequester.TAG, "focusLossForGainRequest() for invalid focus request " + r5);
     */
    /* JADX WARN: Code restructure failed: missing block: B:27:0x0045, code lost:
        return 0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:9:0x000d, code lost:
        if (r5 != 4) goto L9;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private int focusLossForGainRequest(int r5) {
        /*
            r4 = this;
            r0 = 1
            r1 = -3
            r2 = -2
            r3 = -1
            if (r5 == r0) goto L10
            r0 = 2
            if (r5 == r0) goto L1a
            r0 = 3
            if (r5 == r0) goto L24
            r0 = 4
            if (r5 == r0) goto L1a
            goto L2e
        L10:
            int r0 = r4.mFocusLossReceived
            if (r0 == r1) goto L4b
            if (r0 == r2) goto L4b
            if (r0 == r3) goto L4b
            if (r0 == 0) goto L4b
        L1a:
            int r0 = r4.mFocusLossReceived
            if (r0 == r1) goto L4a
            if (r0 == r2) goto L4a
            if (r0 == r3) goto L49
            if (r0 == 0) goto L4a
        L24:
            int r0 = r4.mFocusLossReceived
            if (r0 == r1) goto L48
            if (r0 == r2) goto L47
            if (r0 == r3) goto L46
            if (r0 == 0) goto L48
        L2e:
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r1 = "focusLossForGainRequest() for invalid focus request "
            r0.append(r1)
            r0.append(r5)
            java.lang.String r0 = r0.toString()
            java.lang.String r1 = "MediaFocusControl"
            android.util.Log.e(r1, r0)
            r0 = 0
            return r0
        L46:
            return r3
        L47:
            return r2
        L48:
            return r1
        L49:
            return r3
        L4a:
            return r2
        L4b:
            return r3
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.audio.FocusRequester.focusLossForGainRequest(int):int");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean handleFocusLossFromGain(int focusGain, FocusRequester frWinner, boolean forceDuck) {
        int focusLoss = focusLossForGainRequest(focusGain);
        handleFocusLoss(focusLoss, frWinner, forceDuck);
        return focusLoss == -1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleFocusGain(int focusGain) {
        try {
            this.mFocusLossReceived = 0;
            this.mFocusController.notifyExtPolicyFocusGrant_syncAf(toAudioFocusInfo(), 1);
            IAudioFocusDispatcher fd = this.mFocusDispatcher;
            if (fd != null && this.mFocusLossWasNotified) {
                fd.dispatchAudioFocusChange(focusGain, this.mClientId);
            }
            this.mFocusController.unduckPlayers(this);
            this.mDuckWinner = null;
        } catch (RemoteException e) {
            Log.e(TAG, "Failure to signal gain of audio focus due to: ", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleFocusGainFromRequest(int focusRequestResult) {
        if (focusRequestResult == 1) {
            this.mFocusController.unduckPlayers(this);
            this.mDuckWinner = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleFocusLoss(int focusLoss, FocusRequester frWinner, boolean forceDuck) {
        try {
            if (focusLoss != this.mFocusLossReceived) {
                this.mFocusLossReceived = focusLoss;
                this.mFocusLossWasNotified = false;
                if (!this.mFocusController.mustNotifyFocusOwnerOnDuck() && this.mFocusLossReceived == -3 && (this.mGrantFlags & 2) == 0) {
                    this.mFocusController.notifyExtPolicyFocusLoss_syncAf(toAudioFocusInfo(), false);
                    return;
                }
                boolean handled = false;
                if (frWinner != null) {
                    handled = frameworkHandleFocusLoss(focusLoss, frWinner, forceDuck);
                }
                if (handled) {
                    this.mDuckWinner = frWinner;
                    this.mFocusController.notifyExtPolicyFocusLoss_syncAf(toAudioFocusInfo(), false);
                    return;
                }
                IAudioFocusDispatcher fd = this.mFocusDispatcher;
                if (fd != null) {
                    this.mFocusController.notifyExtPolicyFocusLoss_syncAf(toAudioFocusInfo(), true);
                    this.mFocusLossWasNotified = true;
                    fd.dispatchAudioFocusChange(this.mFocusLossReceived, this.mClientId);
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failure to signal loss of audio focus due to:", e);
        }
    }

    private boolean frameworkHandleFocusLoss(int focusLoss, FocusRequester frWinner, boolean forceDuck) {
        if (frWinner.mCallingUid != this.mCallingUid && focusLoss == -3) {
            if (!forceDuck && (this.mGrantFlags & 2) != 0) {
                Log.v(TAG, "not ducking uid " + this.mCallingUid + " - flags");
                return false;
            } else if (!forceDuck && getSdkTarget() <= 25) {
                Log.v(TAG, "not ducking uid " + this.mCallingUid + " - old SDK");
                return false;
            } else {
                return this.mFocusController.duckPlayers(frWinner, this, forceDuck);
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int dispatchFocusChange(int focusChange) {
        IAudioFocusDispatcher fd = this.mFocusDispatcher;
        if (fd == null || focusChange == 0) {
            return 0;
        }
        if ((focusChange == 3 || focusChange == 4 || focusChange == 2 || focusChange == 1) && this.mFocusGainRequest != focusChange) {
            Log.w(TAG, "focus gain was requested with " + this.mFocusGainRequest + ", dispatching " + focusChange);
        } else if (focusChange == -3 || focusChange == -2 || focusChange == -1) {
            this.mFocusLossReceived = focusChange;
        }
        try {
            fd.dispatchAudioFocusChange(focusChange, this.mClientId);
            return 1;
        } catch (RemoteException e) {
            Log.e(TAG, "dispatchFocusChange: error talking to focus listener " + this.mClientId, e);
            return 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchFocusResultFromExtPolicy(int requestResult) {
        IAudioFocusDispatcher fd = this.mFocusDispatcher;
        if (fd == null) {
            return;
        }
        try {
            fd.dispatchFocusResultFromExtPolicy(requestResult, this.mClientId);
        } catch (RemoteException e) {
            Log.e(TAG, "dispatchFocusResultFromExtPolicy: error talking to focus listener" + this.mClientId, e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public AudioFocusInfo toAudioFocusInfo() {
        return new AudioFocusInfo(this.mAttributes, this.mCallingUid, this.mClientId, this.mPackageName, this.mFocusGainRequest, this.mFocusLossReceived, this.mGrantFlags, this.mSdkTarget);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void handleFocusUnduck() {
        this.mFocusController.unduckPlayers(this);
    }
}