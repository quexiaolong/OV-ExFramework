package com.android.server.backup.utils;

import android.util.Slog;
import com.android.server.backup.BackupManagerService;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/* loaded from: classes.dex */
public final class RandomAccessFileUtils {
    private static RandomAccessFile getRandomAccessFile(File file) throws FileNotFoundException {
        return new RandomAccessFile(file, "rwd");
    }

    public static void writeBoolean(File file, boolean b) {
        try {
            RandomAccessFile af = getRandomAccessFile(file);
            af.writeBoolean(b);
            if (af != null) {
                af.close();
            }
        } catch (IOException e) {
            Slog.w(BackupManagerService.TAG, "Error writing file:" + file.getAbsolutePath(), e);
        }
    }

    public static boolean readBoolean(File file, boolean def) {
        try {
            RandomAccessFile af = getRandomAccessFile(file);
            boolean readBoolean = af.readBoolean();
            if (af != null) {
                af.close();
            }
            return readBoolean;
        } catch (IOException e) {
            Slog.w(BackupManagerService.TAG, "Error reading file:" + file.getAbsolutePath(), e);
            return def;
        }
    }
}