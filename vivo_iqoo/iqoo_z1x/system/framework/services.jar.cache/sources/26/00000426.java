package com.android.server;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Binder;
import android.os.FileUtils;
import android.provider.Settings;
import android.util.Slog;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import libcore.io.IoUtils;

/* loaded from: classes.dex */
public class CertBlacklister extends Binder {
    public static final String PUBKEY_BLACKLIST_KEY = "pubkey_blacklist";
    public static final String SERIAL_BLACKLIST_KEY = "serial_blacklist";
    private static final String TAG = "CertBlacklister";
    private static final String BLACKLIST_ROOT = System.getenv("ANDROID_DATA") + "/misc/keychain/";
    public static final String PUBKEY_PATH = BLACKLIST_ROOT + "pubkey_blacklist.txt";
    public static final String SERIAL_PATH = BLACKLIST_ROOT + "serial_blacklist.txt";

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class BlacklistObserver extends ContentObserver {
        private final ContentResolver mContentResolver;
        private final String mKey;
        private final String mName;
        private final String mPath;
        private final File mTmpDir;

        public BlacklistObserver(String key, String name, String path, ContentResolver cr) {
            super(null);
            this.mKey = key;
            this.mName = name;
            this.mPath = path;
            this.mTmpDir = new File(this.mPath).getParentFile();
            this.mContentResolver = cr;
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            writeBlacklist();
        }

        public String getValue() {
            return Settings.Secure.getString(this.mContentResolver, this.mKey);
        }

        /* JADX WARN: Type inference failed for: r0v0, types: [com.android.server.CertBlacklister$BlacklistObserver$1] */
        private void writeBlacklist() {
            new Thread("BlacklistUpdater") { // from class: com.android.server.CertBlacklister.BlacklistObserver.1
                @Override // java.lang.Thread, java.lang.Runnable
                public void run() {
                    synchronized (BlacklistObserver.this.mTmpDir) {
                        String blacklist = BlacklistObserver.this.getValue();
                        if (blacklist != null) {
                            Slog.i(CertBlacklister.TAG, "Certificate blacklist changed, updating...");
                            FileOutputStream out = null;
                            try {
                                File tmp = File.createTempFile("journal", "", BlacklistObserver.this.mTmpDir);
                                tmp.setReadable(true, false);
                                out = new FileOutputStream(tmp);
                                out.write(blacklist.getBytes());
                                FileUtils.sync(out);
                                tmp.renameTo(new File(BlacklistObserver.this.mPath));
                                Slog.i(CertBlacklister.TAG, "Certificate blacklist updated");
                            } catch (IOException e) {
                                Slog.e(CertBlacklister.TAG, "Failed to write blacklist", e);
                            }
                            IoUtils.closeQuietly(out);
                        }
                    }
                }
            }.start();
        }
    }

    public CertBlacklister(Context context) {
        registerObservers(context.getContentResolver());
    }

    private BlacklistObserver buildPubkeyObserver(ContentResolver cr) {
        return new BlacklistObserver(PUBKEY_BLACKLIST_KEY, "pubkey", PUBKEY_PATH, cr);
    }

    private BlacklistObserver buildSerialObserver(ContentResolver cr) {
        return new BlacklistObserver(SERIAL_BLACKLIST_KEY, "serial", SERIAL_PATH, cr);
    }

    private void registerObservers(ContentResolver cr) {
        cr.registerContentObserver(Settings.Secure.getUriFor(PUBKEY_BLACKLIST_KEY), true, buildPubkeyObserver(cr));
        cr.registerContentObserver(Settings.Secure.getUriFor(SERIAL_BLACKLIST_KEY), true, buildSerialObserver(cr));
    }
}