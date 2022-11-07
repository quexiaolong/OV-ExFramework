package com.android.server.storage;

import android.os.FileUtils;
import android.os.ParcelFileDescriptor;
import android.util.SparseArray;
import com.android.internal.os.FuseUnavailableMountException;
import com.android.internal.util.Preconditions;
import com.android.server.NativeDaemonConnectorException;
import java.util.concurrent.CountDownLatch;
import libcore.io.IoUtils;

/* loaded from: classes2.dex */
public class AppFuseBridge implements Runnable {
    private static final String APPFUSE_MOUNT_NAME_TEMPLATE = "/mnt/appfuse/%d_%d";
    public static final String TAG = "AppFuseBridge";
    private final SparseArray<MountScope> mScopes = new SparseArray<>();
    private long mNativeLoop = native_new();

    private native int native_add_bridge(long j, int i, int i2);

    private native void native_delete(long j);

    private native void native_lock();

    private native long native_new();

    private native void native_start_loop(long j);

    private native void native_unlock();

    public ParcelFileDescriptor addBridge(MountScope mountScope) throws FuseUnavailableMountException, NativeDaemonConnectorException {
        ParcelFileDescriptor result;
        native_lock();
        try {
            synchronized (this) {
                Preconditions.checkArgument(this.mScopes.indexOfKey(mountScope.mountId) < 0);
                if (this.mNativeLoop == 0) {
                    throw new FuseUnavailableMountException(mountScope.mountId);
                }
                int fd = native_add_bridge(this.mNativeLoop, mountScope.mountId, mountScope.open().detachFd());
                if (fd == -1) {
                    throw new FuseUnavailableMountException(mountScope.mountId);
                }
                result = ParcelFileDescriptor.adoptFd(fd);
                this.mScopes.put(mountScope.mountId, mountScope);
                mountScope = null;
            }
            return result;
        } finally {
            native_unlock();
            IoUtils.closeQuietly(mountScope);
        }
    }

    @Override // java.lang.Runnable
    public void run() {
        native_start_loop(this.mNativeLoop);
        synchronized (this) {
            native_delete(this.mNativeLoop);
            this.mNativeLoop = 0L;
        }
    }

    public ParcelFileDescriptor openFile(int mountId, int fileId, int mode) throws FuseUnavailableMountException, InterruptedException {
        MountScope scope;
        synchronized (this) {
            scope = this.mScopes.get(mountId);
            if (scope == null) {
                throw new FuseUnavailableMountException(mountId);
            }
        }
        boolean result = scope.waitForMount();
        if (!result) {
            throw new FuseUnavailableMountException(mountId);
        }
        try {
            int flags = FileUtils.translateModePfdToPosix(mode);
            return scope.openFile(mountId, fileId, flags);
        } catch (NativeDaemonConnectorException e) {
            throw new FuseUnavailableMountException(mountId);
        }
    }

    private synchronized void onMount(int mountId) {
        MountScope scope = this.mScopes.get(mountId);
        if (scope != null) {
            scope.setMountResultLocked(true);
        }
    }

    private synchronized void onClosed(int mountId) {
        MountScope scope = this.mScopes.get(mountId);
        if (scope != null) {
            scope.setMountResultLocked(false);
            IoUtils.closeQuietly(scope);
            this.mScopes.remove(mountId);
        }
    }

    /* loaded from: classes2.dex */
    public static abstract class MountScope implements AutoCloseable {
        public final int mountId;
        public final int uid;
        private final CountDownLatch mMounted = new CountDownLatch(1);
        private boolean mMountResult = false;

        public abstract ParcelFileDescriptor open() throws NativeDaemonConnectorException;

        public abstract ParcelFileDescriptor openFile(int i, int i2, int i3) throws NativeDaemonConnectorException;

        public MountScope(int uid, int mountId) {
            this.uid = uid;
            this.mountId = mountId;
        }

        void setMountResultLocked(boolean result) {
            if (this.mMounted.getCount() == 0) {
                return;
            }
            this.mMountResult = result;
            this.mMounted.countDown();
        }

        boolean waitForMount() throws InterruptedException {
            this.mMounted.await();
            return this.mMountResult;
        }
    }
}