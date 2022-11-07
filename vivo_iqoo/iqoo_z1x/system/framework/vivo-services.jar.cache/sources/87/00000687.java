package com.vivo.services.popupcamera;

import android.os.Handler;

/* loaded from: classes.dex */
public class HallStatePollThread extends Thread {
    private static final boolean DEBUG = true;
    private static final String TAG = "HallStatePollThread";
    private Handler mHandler;

    public HallStatePollThread(String name, Handler handler) {
        super(name);
        this.mHandler = handler;
    }

    @Override // java.lang.Thread, java.lang.Runnable
    public void run() {
        while (true) {
            try {
                Thread.sleep(60000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void startPollHallState() {
    }

    public void stopPollHallState() {
    }
}