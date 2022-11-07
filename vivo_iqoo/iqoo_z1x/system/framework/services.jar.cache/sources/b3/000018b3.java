package com.android.server.security;

import android.os.SharedMemory;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Pair;
import android.util.Slog;
import android.util.apk.ApkSignatureVerifier;
import android.util.apk.ByteBufferFactory;
import android.util.apk.SignatureNotFoundException;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import libcore.util.HexEncoding;

/* loaded from: classes2.dex */
public abstract class VerityUtils {
    private static final boolean DEBUG = false;
    public static final String FSVERITY_SIGNATURE_FILE_EXTENSION = ".fsv_sig";
    private static final int MAX_SIGNATURE_FILE_SIZE_BYTES = 8192;
    private static final String TAG = "VerityUtils";

    private static native int enableFsverityNative(String str, byte[] bArr);

    private static native int statxForFsverityNative(String str);

    public static boolean isFsveritySignatureFile(File file) {
        return file.getName().endsWith(FSVERITY_SIGNATURE_FILE_EXTENSION);
    }

    public static String getFsveritySignatureFilePath(String filePath) {
        return filePath + FSVERITY_SIGNATURE_FILE_EXTENSION;
    }

    public static void setUpFsverity(String filePath, String signaturePath) throws IOException {
        if (Files.size(Paths.get(signaturePath, new String[0])) > 8192) {
            throw new SecurityException("Signature file is unexpectedly large: " + signaturePath);
        }
        byte[] pkcs7Signature = Files.readAllBytes(Paths.get(signaturePath, new String[0]));
        int errno = enableFsverityNative(filePath, pkcs7Signature);
        if (errno != 0) {
            throw new IOException("Failed to enable fs-verity on " + filePath + ": " + Os.strerror(errno));
        }
    }

    public static boolean hasFsverity(String filePath) {
        int retval = statxForFsverityNative(filePath);
        if (retval < 0) {
            Slog.e(TAG, "Failed to check whether fs-verity is enabled, errno " + (-retval) + ": " + filePath);
            return false;
        } else if (retval != 1) {
            return false;
        } else {
            return true;
        }
    }

    @Deprecated
    public static SetupResult generateApkVeritySetupData(String apkPath) {
        SharedMemory shm = null;
        try {
            try {
                byte[] signedVerityHash = ApkSignatureVerifier.getVerityRootHash(apkPath);
                if (signedVerityHash == null) {
                    SetupResult skipped = SetupResult.skipped();
                    if (0 != 0) {
                        shm.close();
                    }
                    return skipped;
                }
                Pair<SharedMemory, Integer> result = generateFsVerityIntoSharedMemory(apkPath, signedVerityHash);
                SharedMemory shm2 = (SharedMemory) result.first;
                int contentSize = ((Integer) result.second).intValue();
                FileDescriptor rfd = shm2.getFileDescriptor();
                if (rfd != null && rfd.valid()) {
                    SetupResult ok = SetupResult.ok(Os.dup(rfd), contentSize);
                    if (shm2 != null) {
                        shm2.close();
                    }
                    return ok;
                }
                SetupResult failed = SetupResult.failed();
                if (shm2 != null) {
                    shm2.close();
                }
                return failed;
            } catch (IOException | SecurityException | DigestException | NoSuchAlgorithmException | SignatureNotFoundException | ErrnoException e) {
                Slog.e(TAG, "Failed to set up apk verity: ", e);
                SetupResult failed2 = SetupResult.failed();
                if (0 != 0) {
                    shm.close();
                }
                return failed2;
            }
        } catch (Throwable th) {
            if (0 != 0) {
                shm.close();
            }
            throw th;
        }
    }

    @Deprecated
    public static byte[] generateApkVerityRootHash(String apkPath) throws NoSuchAlgorithmException, DigestException, IOException {
        return ApkSignatureVerifier.generateApkVerityRootHash(apkPath);
    }

    @Deprecated
    public static byte[] getVerityRootHash(String apkPath) throws IOException, SignatureNotFoundException {
        return ApkSignatureVerifier.getVerityRootHash(apkPath);
    }

    private static Pair<SharedMemory, Integer> generateFsVerityIntoSharedMemory(String apkPath, byte[] expectedRootHash) throws IOException, DigestException, NoSuchAlgorithmException, SignatureNotFoundException {
        TrackedShmBufferFactory shmBufferFactory = new TrackedShmBufferFactory();
        byte[] generatedRootHash = ApkSignatureVerifier.generateApkVerity(apkPath, shmBufferFactory);
        if (!Arrays.equals(expectedRootHash, generatedRootHash)) {
            throw new SecurityException("verity hash mismatch: " + bytesToString(generatedRootHash) + " != " + bytesToString(expectedRootHash));
        }
        int contentSize = shmBufferFactory.getBufferLimit();
        SharedMemory shm = shmBufferFactory.releaseSharedMemory();
        if (shm == null) {
            throw new IllegalStateException("Failed to generate verity tree into shared memory");
        }
        if (!shm.setProtect(OsConstants.PROT_READ)) {
            throw new SecurityException("Failed to set up shared memory correctly");
        }
        return Pair.create(shm, Integer.valueOf(contentSize));
    }

    private static String bytesToString(byte[] bytes) {
        return HexEncoding.encodeToString(bytes);
    }

    @Deprecated
    /* loaded from: classes2.dex */
    public static class SetupResult {
        private static final int RESULT_FAILED = 3;
        private static final int RESULT_OK = 1;
        private static final int RESULT_SKIPPED = 2;
        private final int mCode;
        private final int mContentSize;
        private final FileDescriptor mFileDescriptor;

        public static SetupResult ok(FileDescriptor fileDescriptor, int contentSize) {
            return new SetupResult(1, fileDescriptor, contentSize);
        }

        public static SetupResult skipped() {
            return new SetupResult(2, null, -1);
        }

        public static SetupResult failed() {
            return new SetupResult(3, null, -1);
        }

        private SetupResult(int code, FileDescriptor fileDescriptor, int contentSize) {
            this.mCode = code;
            this.mFileDescriptor = fileDescriptor;
            this.mContentSize = contentSize;
        }

        public boolean isFailed() {
            return this.mCode == 3;
        }

        public boolean isOk() {
            return this.mCode == 1;
        }

        public FileDescriptor getUnownedFileDescriptor() {
            return this.mFileDescriptor;
        }

        public int getContentSize() {
            return this.mContentSize;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class TrackedShmBufferFactory implements ByteBufferFactory {
        private ByteBuffer mBuffer;
        private SharedMemory mShm;

        private TrackedShmBufferFactory() {
        }

        public ByteBuffer create(int capacity) {
            try {
                if (this.mBuffer != null) {
                    throw new IllegalStateException("Multiple instantiation from this factory");
                }
                SharedMemory create = SharedMemory.create("apkverity", capacity);
                this.mShm = create;
                if (!create.setProtect(OsConstants.PROT_READ | OsConstants.PROT_WRITE)) {
                    throw new SecurityException("Failed to set protection");
                }
                ByteBuffer mapReadWrite = this.mShm.mapReadWrite();
                this.mBuffer = mapReadWrite;
                return mapReadWrite;
            } catch (ErrnoException e) {
                throw new SecurityException("Failed to set protection", e);
            }
        }

        public SharedMemory releaseSharedMemory() {
            ByteBuffer byteBuffer = this.mBuffer;
            if (byteBuffer != null) {
                SharedMemory.unmap(byteBuffer);
                this.mBuffer = null;
            }
            SharedMemory tmp = this.mShm;
            this.mShm = null;
            return tmp;
        }

        public int getBufferLimit() {
            ByteBuffer byteBuffer = this.mBuffer;
            if (byteBuffer == null) {
                return -1;
            }
            return byteBuffer.limit();
        }
    }
}