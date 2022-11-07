package com.android.server.appop;

import android.app.AppOpsManager;
import android.media.AudioAttributes;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import java.io.PrintWriter;

/* loaded from: classes.dex */
public class AudioRestrictionManager {
    static final SparseArray<SparseBooleanArray> CAMERA_AUDIO_RESTRICTIONS;
    static final String TAG = "AudioRestriction";
    final SparseArray<SparseArray<Restriction>> mZenModeAudioRestrictions = new SparseArray<>();
    int mCameraAudioRestriction = 0;

    static {
        int[] iArr;
        SparseBooleanArray audioMutedUsages = new SparseBooleanArray();
        SparseBooleanArray vibrationMutedUsages = new SparseBooleanArray();
        for (int usage : AudioAttributes.SDK_USAGES) {
            int suppressionBehavior = AudioAttributes.SUPPRESSIBLE_USAGES.get(usage);
            if (suppressionBehavior == 1 || suppressionBehavior == 2 || suppressionBehavior == 4) {
                audioMutedUsages.append(usage, true);
                vibrationMutedUsages.append(usage, true);
            } else if (suppressionBehavior != 5 && suppressionBehavior != 6 && suppressionBehavior != 3) {
                Slog.e(TAG, "Unknown audio suppression behavior" + suppressionBehavior);
            }
        }
        SparseArray<SparseBooleanArray> sparseArray = new SparseArray<>();
        CAMERA_AUDIO_RESTRICTIONS = sparseArray;
        sparseArray.append(28, audioMutedUsages);
        CAMERA_AUDIO_RESTRICTIONS.append(3, vibrationMutedUsages);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class Restriction {
        private static final ArraySet<String> NO_EXCEPTIONS = new ArraySet<>();
        ArraySet<String> exceptionPackages;
        int mode;

        private Restriction() {
            this.exceptionPackages = NO_EXCEPTIONS;
        }
    }

    public int checkAudioOperation(int code, int usage, int uid, String packageName) {
        SparseBooleanArray mutedUsages;
        synchronized (this) {
            if (this.mCameraAudioRestriction != 0 && ((code == 3 || (code == 28 && this.mCameraAudioRestriction == 3)) && (mutedUsages = CAMERA_AUDIO_RESTRICTIONS.get(code)) != null && mutedUsages.get(usage))) {
                return 1;
            }
            int mode = checkZenModeRestrictionLocked(code, usage, uid, packageName);
            if (mode != 0) {
                return mode;
            }
            return 0;
        }
    }

    private int checkZenModeRestrictionLocked(int code, int usage, int uid, String packageName) {
        Restriction r;
        SparseArray<Restriction> usageRestrictions = this.mZenModeAudioRestrictions.get(code);
        if (usageRestrictions != null && (r = usageRestrictions.get(usage)) != null && !r.exceptionPackages.contains(packageName)) {
            return r.mode;
        }
        return 0;
    }

    public void setZenModeAudioRestriction(int code, int usage, int uid, int mode, String[] exceptionPackages) {
        synchronized (this) {
            SparseArray<Restriction> usageRestrictions = this.mZenModeAudioRestrictions.get(code);
            if (usageRestrictions == null) {
                usageRestrictions = new SparseArray<>();
                this.mZenModeAudioRestrictions.put(code, usageRestrictions);
            }
            usageRestrictions.remove(usage);
            if (mode != 0) {
                Restriction r = new Restriction();
                r.mode = mode;
                if (exceptionPackages != null) {
                    int N = exceptionPackages.length;
                    r.exceptionPackages = new ArraySet<>(N);
                    for (String pkg : exceptionPackages) {
                        if (pkg != null) {
                            r.exceptionPackages.add(pkg.trim());
                        }
                    }
                }
                usageRestrictions.put(usage, r);
            }
        }
    }

    public void setCameraAudioRestriction(int mode) {
        synchronized (this) {
            this.mCameraAudioRestriction = mode;
        }
    }

    public boolean hasActiveRestrictions() {
        boolean z;
        boolean hasActiveRestrictions;
        synchronized (this) {
            if (this.mZenModeAudioRestrictions.size() <= 0 && this.mCameraAudioRestriction == 0) {
                z = false;
                hasActiveRestrictions = z;
            }
            z = true;
            hasActiveRestrictions = z;
        }
        return hasActiveRestrictions;
    }

    public boolean dump(PrintWriter pw) {
        boolean printedHeader = false;
        boolean needSep = hasActiveRestrictions();
        synchronized (this) {
            for (int o = 0; o < this.mZenModeAudioRestrictions.size(); o++) {
                String op = AppOpsManager.opToName(this.mZenModeAudioRestrictions.keyAt(o));
                SparseArray<Restriction> restrictions = this.mZenModeAudioRestrictions.valueAt(o);
                for (int i = 0; i < restrictions.size(); i++) {
                    if (!printedHeader) {
                        pw.println("  Zen Mode Audio Restrictions:");
                        printedHeader = true;
                    }
                    int usage = restrictions.keyAt(i);
                    pw.print("    ");
                    pw.print(op);
                    pw.print(" usage=");
                    pw.print(AudioAttributes.usageToString(usage));
                    Restriction r = restrictions.valueAt(i);
                    pw.print(": mode=");
                    pw.println(AppOpsManager.modeToName(r.mode));
                    if (!r.exceptionPackages.isEmpty()) {
                        pw.println("      Exceptions:");
                        for (int j = 0; j < r.exceptionPackages.size(); j++) {
                            pw.print("        ");
                            pw.println(r.exceptionPackages.valueAt(j));
                        }
                    }
                }
            }
            int o2 = this.mCameraAudioRestriction;
            if (o2 != 0) {
                pw.println("  Camera Audio Restriction Mode: " + cameraRestrictionModeToName(this.mCameraAudioRestriction));
            }
        }
        return needSep;
    }

    private static String cameraRestrictionModeToName(int mode) {
        if (mode != 0) {
            if (mode != 1) {
                if (mode == 3) {
                    return "MuteVibrationAndSound";
                }
                return "Unknown";
            }
            return "MuteVibration";
        }
        return "None";
    }
}