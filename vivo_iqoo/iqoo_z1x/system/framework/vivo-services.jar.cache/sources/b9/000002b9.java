package com.android.server.location;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

/* loaded from: classes.dex */
public final class VivoLocThread {
    private static VivoLocThread sVivoLocThread = null;
    private Looper mLocationServiceLooper = null;
    private Looper mGnssProviderLooper = null;
    private Handler mDiagnosticThreadHandler = null;

    public static VivoLocThread getInstance() {
        if (sVivoLocThread == null) {
            sVivoLocThread = new VivoLocThread();
        }
        return sVivoLocThread;
    }

    private VivoLocThread() {
    }

    public Looper getLocationServiceLooper() {
        if (this.mLocationServiceLooper == null) {
            HandlerThread thread = new HandlerThread("VivoLocationManagerServiceExt");
            thread.start();
            this.mLocationServiceLooper = thread.getLooper();
        }
        return this.mLocationServiceLooper;
    }

    public Looper getGnssProviderLooper() {
        if (this.mGnssProviderLooper == null) {
            HandlerThread thread = new HandlerThread("vivo_gnss_provider_thread");
            thread.start();
            this.mGnssProviderLooper = thread.getLooper();
        }
        return this.mGnssProviderLooper;
    }

    public Handler getDiagnosticThreadHandler() {
        if (this.mDiagnosticThreadHandler == null) {
            HandlerThread thread = new HandlerThread("vivo_gnss_provider_thread");
            thread.start();
            this.mDiagnosticThreadHandler = thread.getThreadHandler();
        }
        return this.mDiagnosticThreadHandler;
    }
}