package com.vivo.face.common.memory;

import android.os.MemoryFile;
import com.vivo.face.common.utils.FaceLog;
import java.io.FileDescriptor;

/* loaded from: classes.dex */
public final class FaceSharedMemory {
    public static final String SHARED_MEMORY_NAME = "face-data";
    private static final String TAG = "FaceSharedMemory";
    private FileDescriptor mFd;
    private MemoryFile mMemoryFile;

    public FaceSharedMemory(String name, int length) {
        try {
            this.mMemoryFile = new MemoryFile(name, length);
        } catch (Exception e) {
            FaceLog.e(TAG, "Failed to get memory file " + name, e);
        }
    }

    public FileDescriptor getFileDescriptor() {
        try {
            if (this.mMemoryFile != null) {
                FileDescriptor fileDescriptor = this.mMemoryFile.getFileDescriptor();
                this.mFd = fileDescriptor;
                return fileDescriptor;
            }
        } catch (Exception e) {
            FaceLog.e(TAG, "Failed to get filedescriptor", e);
        }
        return this.mFd;
    }

    public int getSize() {
        try {
            if (this.mMemoryFile == null) {
                return 0;
            }
            return this.mMemoryFile.length();
        } catch (Exception e) {
            FaceLog.e(TAG, "get size exception: ", e);
            return 0;
        }
    }

    public void clearData() {
        if (this.mMemoryFile != null) {
            int size = getSize();
            byte[] zero = new byte[size];
            try {
                this.mMemoryFile.writeBytes(zero, 0, 0, size);
            } catch (Exception e) {
                FaceLog.e(TAG, "Failed to clear share memory", e);
            }
        }
    }

    public void closeSharedMemory() {
        MemoryFile memoryFile = this.mMemoryFile;
        if (memoryFile != null) {
            memoryFile.close();
        }
        this.mFd = null;
        this.mMemoryFile = null;
    }
}