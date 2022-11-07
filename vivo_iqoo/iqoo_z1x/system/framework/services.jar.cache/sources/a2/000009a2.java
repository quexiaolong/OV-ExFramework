package com.android.server.backup;

import android.util.Slog;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/* loaded from: classes.dex */
public class DataChangedJournal {
    private static final int BUFFER_SIZE_BYTES = 8192;
    private static final String FILE_NAME_PREFIX = "journal";
    private static final String TAG = "DataChangedJournal";
    private final File mFile;

    DataChangedJournal(File file) {
        this.mFile = file;
    }

    public void addPackage(String packageName) throws IOException {
        RandomAccessFile out = new RandomAccessFile(this.mFile, "rws");
        try {
            out.seek(out.length());
            out.writeUTF(packageName);
            out.close();
        } catch (Throwable th) {
            try {
                out.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    public void forEach(Consumer<String> consumer) throws IOException {
        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(this.mFile), 8192);
        try {
            DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);
            while (dataInputStream.available() > 0) {
                String packageName = dataInputStream.readUTF();
                consumer.accept(packageName);
            }
            dataInputStream.close();
            bufferedInputStream.close();
        } catch (Throwable th) {
            try {
                bufferedInputStream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    public List<String> getPackages() throws IOException {
        final List<String> packages = new ArrayList<>();
        Objects.requireNonNull(packages);
        forEach(new Consumer() { // from class: com.android.server.backup.-$$Lambda$NsJlXDEZZRYyD6JsbnCsdcb4L4A
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                packages.add((String) obj);
            }
        });
        return packages;
    }

    public boolean delete() {
        return this.mFile.delete();
    }

    public boolean equals(Object object) {
        if (object instanceof DataChangedJournal) {
            DataChangedJournal that = (DataChangedJournal) object;
            try {
                return this.mFile.getCanonicalPath().equals(that.mFile.getCanonicalPath());
            } catch (IOException e) {
                return false;
            }
        }
        return false;
    }

    public String toString() {
        return this.mFile.toString();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static DataChangedJournal newJournal(File journalDirectory) throws IOException {
        return new DataChangedJournal(File.createTempFile(FILE_NAME_PREFIX, null, journalDirectory));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static ArrayList<DataChangedJournal> listJournals(File journalDirectory) {
        ArrayList<DataChangedJournal> journals = new ArrayList<>();
        File[] journalFiles = journalDirectory.listFiles();
        if (journalFiles == null) {
            Slog.w(TAG, "Failed to read journal files");
            return journals;
        }
        for (File file : journalFiles) {
            journals.add(new DataChangedJournal(file));
        }
        return journals;
    }
}