package com.android.server.power;

import android.content.Context;
import android.hardware.display.VivoDisplayStateInternal;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.ArraySet;
import android.util.Pair;
import android.util.Slog;
import com.android.internal.statusbar.IStatusBarService;
import com.android.server.LocalServices;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/* loaded from: classes2.dex */
public class AmbientDisplaySuppressionController {
    private static final String TAG = "AmbientDisplaySuppressionController";
    private final Context mContext;
    private IStatusBarService mStatusBarService;
    private final Set<Pair<String, Integer>> mSuppressionTokens;
    private VivoDisplayStateInternal mVivoDisplayStateInternal;

    /* JADX INFO: Access modifiers changed from: package-private */
    public AmbientDisplaySuppressionController(Context context) {
        Objects.requireNonNull(context);
        this.mContext = context;
        this.mSuppressionTokens = Collections.synchronizedSet(new ArraySet());
    }

    public void suppress(String token, int callingUid, boolean suppress) {
        Objects.requireNonNull(token);
        Pair<String, Integer> suppressionToken = Pair.create(token, Integer.valueOf(callingUid));
        if (suppress) {
            this.mSuppressionTokens.add(suppressionToken);
        } else {
            this.mSuppressionTokens.remove(suppressionToken);
        }
        try {
            synchronized (this.mSuppressionTokens) {
                getStatusBar().suppressAmbientDisplay(isSuppressed());
                getDisplayStateInternal().suppressAmbientDisplay(isSuppressed());
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to suppress ambient display", e);
        }
    }

    public boolean isSuppressed(String token, int callingUid) {
        Set<Pair<String, Integer>> set = this.mSuppressionTokens;
        Objects.requireNonNull(token);
        return set.contains(Pair.create(token, Integer.valueOf(callingUid)));
    }

    public boolean isSuppressed() {
        return !this.mSuppressionTokens.isEmpty();
    }

    public void dump(PrintWriter pw) {
        pw.println("AmbientDisplaySuppressionController:");
        pw.println(" ambientDisplaySuppressed=" + isSuppressed());
        pw.println(" mSuppressionTokens=" + this.mSuppressionTokens);
    }

    private synchronized IStatusBarService getStatusBar() {
        if (this.mStatusBarService == null) {
            this.mStatusBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
        }
        return this.mStatusBarService;
    }

    private synchronized VivoDisplayStateInternal getDisplayStateInternal() {
        if (this.mVivoDisplayStateInternal == null) {
            this.mVivoDisplayStateInternal = (VivoDisplayStateInternal) LocalServices.getService(VivoDisplayStateInternal.class);
        }
        return this.mVivoDisplayStateInternal;
    }
}