package com.android.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Slog;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

/* loaded from: classes.dex */
public class EntropyMixer extends Binder {
    private static final int ENTROPY_WHAT = 1;
    private static final int ENTROPY_WRITE_PERIOD = 10800000;
    private static final String TAG = "EntropyMixer";
    private final String entropyFile;
    private final String hwRandomDevice;
    private final BroadcastReceiver mBroadcastReceiver;
    private final Handler mHandler;
    private final String randomDevice;
    private static final long START_TIME = System.currentTimeMillis();
    private static final long START_NANOTIME = System.nanoTime();

    public EntropyMixer(Context context) {
        this(context, getSystemDir() + "/entropy.dat", "/dev/urandom", "/dev/hw_random");
    }

    public EntropyMixer(Context context, String entropyFile, String randomDevice, String hwRandomDevice) {
        this.mHandler = new Handler(IoThread.getHandler().getLooper()) { // from class: com.android.server.EntropyMixer.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    EntropyMixer.this.addHwRandomEntropy();
                    EntropyMixer.this.writeEntropy();
                    EntropyMixer.this.scheduleEntropyWriter();
                    return;
                }
                Slog.e(EntropyMixer.TAG, "Will not process invalid message");
            }
        };
        this.mBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.EntropyMixer.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                EntropyMixer.this.writeEntropy();
            }
        };
        if (randomDevice == null) {
            throw new NullPointerException("randomDevice");
        }
        if (hwRandomDevice == null) {
            throw new NullPointerException("hwRandomDevice");
        }
        if (entropyFile == null) {
            throw new NullPointerException("entropyFile");
        }
        this.randomDevice = randomDevice;
        this.hwRandomDevice = hwRandomDevice;
        this.entropyFile = entropyFile;
        loadInitialEntropy();
        addDeviceSpecificEntropy();
        addHwRandomEntropy();
        writeEntropy();
        scheduleEntropyWriter();
        IntentFilter broadcastFilter = new IntentFilter("android.intent.action.ACTION_SHUTDOWN");
        broadcastFilter.addAction("android.intent.action.ACTION_POWER_CONNECTED");
        broadcastFilter.addAction("android.intent.action.REBOOT");
        context.registerReceiver(this.mBroadcastReceiver, broadcastFilter, null, this.mHandler);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void scheduleEntropyWriter() {
        this.mHandler.removeMessages(1);
        this.mHandler.sendEmptyMessageDelayed(1, 10800000L);
    }

    private void loadInitialEntropy() {
        try {
            RandomBlock.fromFile(this.entropyFile).toFile(this.randomDevice, false);
        } catch (FileNotFoundException e) {
            Slog.w(TAG, "No existing entropy file -- first boot?");
        } catch (IOException e2) {
            Slog.w(TAG, "Failure loading existing entropy file", e2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void writeEntropy() {
        try {
            Slog.i(TAG, "Writing entropy...");
            RandomBlock.fromFile(this.randomDevice).toFile(this.entropyFile, true);
        } catch (IOException e) {
            Slog.w(TAG, "Unable to write entropy", e);
        }
    }

    private void addDeviceSpecificEntropy() {
        PrintWriter out = null;
        try {
            try {
                out = new PrintWriter(new FileOutputStream(this.randomDevice));
                out.println("Copyright (C) 2009 The Android Open Source Project");
                out.println("All Your Randomness Are Belong To Us");
                out.println(START_TIME);
                out.println(START_NANOTIME);
                out.println(SystemProperties.get("ro.serialno"));
                out.println(SystemProperties.get("ro.bootmode"));
                out.println(SystemProperties.get("ro.baseband"));
                out.println(SystemProperties.get("ro.carrier"));
                out.println(SystemProperties.get("ro.bootloader"));
                out.println(SystemProperties.get("ro.hardware"));
                out.println(SystemProperties.get("ro.revision"));
                out.println(SystemProperties.get("ro.build.fingerprint"));
                out.println(new Object().hashCode());
                out.println(System.currentTimeMillis());
                out.println(System.nanoTime());
            } catch (IOException e) {
                Slog.w(TAG, "Unable to add device specific data to the entropy pool", e);
                if (out == null) {
                    return;
                }
            }
            out.close();
        } catch (Throwable th) {
            if (out != null) {
                out.close();
            }
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addHwRandomEntropy() {
        if (!new File(this.hwRandomDevice).exists()) {
            return;
        }
        try {
            RandomBlock.fromFile(this.hwRandomDevice).toFile(this.randomDevice, false);
            Slog.i(TAG, "Added HW RNG output to entropy pool");
        } catch (IOException e) {
            Slog.w(TAG, "Failed to add HW RNG output to entropy pool", e);
        }
    }

    private static String getSystemDir() {
        File dataDir = Environment.getDataDirectory();
        File systemDir = new File(dataDir, "system");
        systemDir.mkdirs();
        return systemDir.toString();
    }
}