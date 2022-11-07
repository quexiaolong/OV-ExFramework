package com.android.server.backup.utils;

import android.os.ParcelFileDescriptor;
import android.util.Slog;
import com.android.server.backup.BackupManagerService;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

/* loaded from: classes.dex */
public class FullBackupUtils {
    public static void routeSocketDataToOutput(ParcelFileDescriptor inPipe, OutputStream out) throws IOException {
        FileInputStream raw = new FileInputStream(inPipe.getFileDescriptor());
        DataInputStream in = new DataInputStream(raw);
        byte[] buffer = new byte[32768];
        while (true) {
            int readInt = in.readInt();
            int chunkTotal = readInt;
            if (readInt > 0) {
                while (chunkTotal > 0) {
                    int toRead = chunkTotal > buffer.length ? buffer.length : chunkTotal;
                    int nRead = in.read(buffer, 0, toRead);
                    if (nRead < 0) {
                        Slog.e(BackupManagerService.TAG, "Unexpectedly reached end of file while reading data");
                        throw new EOFException();
                    } else {
                        out.write(buffer, 0, nRead);
                        chunkTotal -= nRead;
                    }
                }
            } else {
                return;
            }
        }
    }
}