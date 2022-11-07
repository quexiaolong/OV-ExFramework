package com.android.server.updates;

import android.os.FileUtils;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Base64;
import android.util.Slog;
import com.android.internal.util.HexDump;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import libcore.io.Streams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes2.dex */
public class CertificateTransparencyLogInstallReceiver extends ConfigUpdateInstallReceiver {
    private static final String LOGDIR_PREFIX = "logs-";
    private static final String TAG = "CTLogInstallReceiver";

    public CertificateTransparencyLogInstallReceiver() {
        super("/data/misc/keychain/trusted_ct_logs/", "ct_logs", "metadata/", "version");
    }

    @Override // com.android.server.updates.ConfigUpdateInstallReceiver
    protected void install(InputStream inputStream, int version) throws IOException {
        this.updateDir.mkdir();
        if (!this.updateDir.isDirectory()) {
            throw new IOException("Unable to make directory " + this.updateDir.getCanonicalPath());
        } else if (!this.updateDir.setReadable(true, false)) {
            throw new IOException("Unable to set permissions on " + this.updateDir.getCanonicalPath());
        } else {
            File currentSymlink = new File(this.updateDir, "current");
            File file = this.updateDir;
            File newVersion = new File(file, LOGDIR_PREFIX + String.valueOf(version));
            if (newVersion.exists()) {
                if (newVersion.getCanonicalPath().equals(currentSymlink.getCanonicalPath())) {
                    writeUpdate(this.updateDir, this.updateVersion, new ByteArrayInputStream(Long.toString(version).getBytes()));
                    deleteOldLogDirectories();
                    return;
                }
                FileUtils.deleteContentsAndDir(newVersion);
            }
            try {
                newVersion.mkdir();
                if (!newVersion.isDirectory()) {
                    throw new IOException("Unable to make directory " + newVersion.getCanonicalPath());
                } else if (!newVersion.setReadable(true, false)) {
                    throw new IOException("Failed to set " + newVersion.getCanonicalPath() + " readable");
                } else {
                    try {
                        byte[] content2 = Streams.readFullyNoClose(inputStream);
                        JSONObject json = new JSONObject(new String(content2, StandardCharsets.UTF_8));
                        JSONArray logs = json.getJSONArray("logs");
                        for (int i = 0; i < logs.length(); i++) {
                            JSONObject log = logs.getJSONObject(i);
                            installLog(newVersion, log);
                        }
                        File tempSymlink = new File(this.updateDir, "new_symlink");
                        try {
                            Os.symlink(newVersion.getCanonicalPath(), tempSymlink.getCanonicalPath());
                            tempSymlink.renameTo(currentSymlink.getAbsoluteFile());
                            Slog.i(TAG, "CT log directory updated to " + newVersion.getAbsolutePath());
                            writeUpdate(this.updateDir, this.updateVersion, new ByteArrayInputStream(Long.toString((long) version).getBytes()));
                            deleteOldLogDirectories();
                        } catch (ErrnoException e) {
                            throw new IOException("Failed to create symlink", e);
                        }
                    } catch (JSONException e2) {
                        throw new IOException("Failed to parse logs", e2);
                    }
                }
            } catch (IOException | RuntimeException e3) {
                FileUtils.deleteContentsAndDir(newVersion);
                throw e3;
            }
        }
    }

    private void installLog(File directory, JSONObject logObject) throws IOException {
        try {
            String logFilename = getLogFileName(logObject.getString("key"));
            File file = new File(directory, logFilename);
            OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
            writeLogEntry(out, "key", logObject.getString("key"));
            writeLogEntry(out, "url", logObject.getString("url"));
            writeLogEntry(out, "description", logObject.getString("description"));
            out.close();
            if (!file.setReadable(true, false)) {
                throw new IOException("Failed to set permissions on " + file.getCanonicalPath());
            }
        } catch (JSONException e) {
            throw new IOException("Failed to parse log", e);
        }
    }

    private String getLogFileName(String base64PublicKey) {
        byte[] keyBytes = Base64.decode(base64PublicKey, 0);
        try {
            byte[] id = MessageDigest.getInstance("SHA-256").digest(keyBytes);
            return HexDump.toHexString(id, false);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeLogEntry(OutputStreamWriter out, String key, String value) throws IOException {
        out.write(key + ":" + value + "\n");
    }

    private void deleteOldLogDirectories() throws IOException {
        File[] listFiles;
        if (!this.updateDir.exists()) {
            return;
        }
        final File currentTarget = new File(this.updateDir, "current").getCanonicalFile();
        FileFilter filter = new FileFilter() { // from class: com.android.server.updates.CertificateTransparencyLogInstallReceiver.1
            @Override // java.io.FileFilter
            public boolean accept(File file) {
                return !currentTarget.equals(file) && file.getName().startsWith(CertificateTransparencyLogInstallReceiver.LOGDIR_PREFIX);
            }
        };
        for (File f : this.updateDir.listFiles(filter)) {
            FileUtils.deleteContentsAndDir(f);
        }
    }
}