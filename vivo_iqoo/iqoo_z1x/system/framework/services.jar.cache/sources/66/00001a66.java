package com.android.server.updates;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.util.EventLog;
import android.util.Slog;
import com.android.internal.util.HexDump;
import com.android.server.EventLogTags;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import libcore.io.IoUtils;
import libcore.io.Streams;

/* loaded from: classes2.dex */
public class ConfigUpdateInstallReceiver extends BroadcastReceiver {
    private static final String EXTRA_REQUIRED_HASH = "REQUIRED_HASH";
    private static final String EXTRA_VERSION_NUMBER = "VERSION";
    private static final String TAG = "ConfigUpdateInstallReceiver";
    protected final File updateContent;
    protected final File updateDir;
    protected final File updateVersion;

    public ConfigUpdateInstallReceiver(String updateDir, String updateContentPath, String updateMetadataPath, String updateVersionPath) {
        this.updateDir = new File(updateDir);
        this.updateContent = new File(updateDir, updateContentPath);
        File updateMetadataDir = new File(updateDir, updateMetadataPath);
        this.updateVersion = new File(updateMetadataDir, updateVersionPath);
    }

    /* JADX WARN: Type inference failed for: r0v0, types: [com.android.server.updates.ConfigUpdateInstallReceiver$1] */
    @Override // android.content.BroadcastReceiver
    public void onReceive(final Context context, final Intent intent) {
        new Thread() { // from class: com.android.server.updates.ConfigUpdateInstallReceiver.1
            @Override // java.lang.Thread, java.lang.Runnable
            public void run() {
                try {
                    int altVersion = ConfigUpdateInstallReceiver.this.getVersionFromIntent(intent);
                    String altRequiredHash = ConfigUpdateInstallReceiver.this.getRequiredHashFromIntent(intent);
                    int currentVersion = ConfigUpdateInstallReceiver.this.getCurrentVersion();
                    String currentHash = ConfigUpdateInstallReceiver.getCurrentHash(ConfigUpdateInstallReceiver.this.getCurrentContent());
                    if (ConfigUpdateInstallReceiver.this.verifyVersion(currentVersion, altVersion)) {
                        if (ConfigUpdateInstallReceiver.this.verifyPreviousHash(currentHash, altRequiredHash)) {
                            Slog.i(ConfigUpdateInstallReceiver.TAG, "Found new update, installing...");
                            BufferedInputStream altContent = ConfigUpdateInstallReceiver.this.getAltContent(context, intent);
                            ConfigUpdateInstallReceiver.this.install(altContent, altVersion);
                            if (altContent != null) {
                                altContent.close();
                            }
                            Slog.i(ConfigUpdateInstallReceiver.TAG, "Installation successful");
                            ConfigUpdateInstallReceiver.this.postInstall(context, intent);
                            return;
                        }
                        EventLog.writeEvent((int) EventLogTags.CONFIG_INSTALL_FAILED, "Current hash did not match required value");
                        return;
                    }
                    Slog.i(ConfigUpdateInstallReceiver.TAG, "Not installing, new version is <= current version");
                } catch (Exception e) {
                    Slog.e(ConfigUpdateInstallReceiver.TAG, "Could not update content!", e);
                    String errMsg = e.toString();
                    if (errMsg.length() > 100) {
                        errMsg = errMsg.substring(0, 99);
                    }
                    EventLog.writeEvent((int) EventLogTags.CONFIG_INSTALL_FAILED, errMsg);
                }
            }
        }.start();
    }

    private Uri getContentFromIntent(Intent i) {
        Uri data = i.getData();
        if (data == null) {
            throw new IllegalStateException("Missing required content path, ignoring.");
        }
        return data;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getVersionFromIntent(Intent i) throws NumberFormatException {
        String extraValue = i.getStringExtra(EXTRA_VERSION_NUMBER);
        if (extraValue == null) {
            throw new IllegalStateException("Missing required version number, ignoring.");
        }
        return Integer.parseInt(extraValue.trim());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getRequiredHashFromIntent(Intent i) {
        String extraValue = i.getStringExtra(EXTRA_REQUIRED_HASH);
        if (extraValue == null) {
            throw new IllegalStateException("Missing required previous hash, ignoring.");
        }
        return extraValue.trim();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getCurrentVersion() throws NumberFormatException {
        try {
            String strVersion = IoUtils.readFileAsString(this.updateVersion.getCanonicalPath()).trim();
            return Integer.parseInt(strVersion);
        } catch (IOException e) {
            Slog.i(TAG, "Couldn't find current metadata, assuming first update");
            return 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public BufferedInputStream getAltContent(Context c, Intent i) throws IOException {
        Uri content2 = getContentFromIntent(i);
        Binder.allowBlockingForCurrentThread();
        try {
            return new BufferedInputStream(c.getContentResolver().openInputStream(content2));
        } finally {
            Binder.defaultBlockingForCurrentThread();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public byte[] getCurrentContent() {
        try {
            return IoUtils.readFileAsByteArray(this.updateContent.getCanonicalPath());
        } catch (IOException e) {
            Slog.i(TAG, "Failed to read current content, assuming first update!");
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String getCurrentHash(byte[] content2) {
        if (content2 == null) {
            return "0";
        }
        try {
            MessageDigest dgst = MessageDigest.getInstance("SHA512");
            byte[] fingerprint = dgst.digest(content2);
            return HexDump.toHexString(fingerprint, false);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    protected boolean verifyVersion(int current, int alternative) {
        return current < alternative;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean verifyPreviousHash(String current, String required) {
        if (required.equals("NONE")) {
            return true;
        }
        return current.equals(required);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void writeUpdate(File dir, File file, InputStream inputStream) throws IOException {
        FileOutputStream out = null;
        File tmp = null;
        try {
            File parent = file.getParentFile();
            parent.mkdirs();
            if (!parent.exists()) {
                throw new IOException("Failed to create directory " + parent.getCanonicalPath());
            }
            tmp = File.createTempFile("journal", "", dir);
            tmp.setReadable(true, false);
            out = new FileOutputStream(tmp);
            Streams.copy(inputStream, out);
            out.getFD().sync();
            if (!tmp.renameTo(file)) {
                throw new IOException("Failed to atomically rename " + file.getCanonicalPath());
            }
        } finally {
            if (tmp != null) {
                tmp.delete();
            }
            IoUtils.closeQuietly(out);
        }
    }

    protected void install(InputStream inputStream, int version) throws IOException {
        writeUpdate(this.updateDir, this.updateContent, inputStream);
        writeUpdate(this.updateDir, this.updateVersion, new ByteArrayInputStream(Long.toString(version).getBytes()));
    }

    protected void postInstall(Context context, Intent intent) {
    }
}