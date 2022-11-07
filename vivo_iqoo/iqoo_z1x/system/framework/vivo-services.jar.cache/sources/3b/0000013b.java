package com.android.server.backup;

import android.app.IBackupAgent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.graphics.common.V1_0.Dataspace;
import android.os.ParcelFileDescriptor;
import com.vivo.services.backup.util.VivoBackupCommonUtil;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import vivo.app.backup.AbsVivoBackupManager;
import vivo.app.backup.BRTimeoutMonitor;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoFullBackupEngineImpl implements IVivoFullBackupEngine {
    private static final String TAG = "VIVO_BACKUP_VivoFullBackupEngineImpl";
    private int mFd = -1;
    private boolean mRunFromVivo = false;
    private int mBackupResult = -1;
    private BRTimeoutMonitor mMonitor = null;
    private ParcelFileDescriptor[] mPipes = null;

    public void setUp(int fd, boolean isRunFromVivo) {
        this.mFd = fd;
        this.mRunFromVivo = isRunFromVivo;
    }

    public int getFd() {
        return this.mFd;
    }

    public boolean isRunFromVivo() {
        return this.mRunFromVivo;
    }

    public void setPipes(ParcelFileDescriptor[] pipes) {
        this.mPipes = pipes;
    }

    public void setMonitor(BRTimeoutMonitor monitor) {
        this.mMonitor = monitor;
    }

    public int getBackupResult() {
        return this.mBackupResult;
    }

    public void setBackupResult(int result) {
        this.mBackupResult = result;
    }

    public void putFdByToken(int token) {
        AbsVivoBackupManager vivoBackupManager;
        if (this.mRunFromVivo && (vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager()) != null) {
            VSlog.d(TAG, "putFdByToken token = " + token + " fd = " + this.mFd);
            vivoBackupManager.putFdByToken(token, this.mFd);
        }
    }

    public void removeToken(int token) {
        AbsVivoBackupManager vivoBackupManager;
        if (this.mRunFromVivo && (vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager()) != null) {
            VSlog.d(TAG, "fd = " + this.mFd + ", current token is " + token + ", remove token");
            vivoBackupManager.removeToken(token);
        }
    }

    public boolean isDualUserByPkg(String pkg) {
        AbsVivoBackupManager vivoBackupManager;
        if (this.mRunFromVivo && (vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager()) != null) {
            return vivoBackupManager.isDualUserByPkg(pkg);
        }
        return false;
    }

    public void setDualUserByPkg(String pkg, boolean isDualUser) {
        AbsVivoBackupManager vivoBackupManager;
        if (this.mRunFromVivo && (vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager()) != null) {
            vivoBackupManager.setDualUserByPkg(pkg, isDualUser);
        }
    }

    public PackageInfo getPkgInfoIfDualUser(PackageInfo packageInfo, Object userBackupManagerService) {
        AbsVivoBackupManager vivoBackupManager;
        if (this.mRunFromVivo && (vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager()) != null && isDualUserByPkg(packageInfo.packageName)) {
            UserBackupManagerService ubms = (UserBackupManagerService) userBackupManagerService;
            try {
                VSlog.w(TAG, "getPkgInfoIfDualUser pkg = " + packageInfo.packageName + " userId = " + vivoBackupManager.getDualUserId());
                return ubms.getPackageManager().getPackageInfoAsUser(packageInfo.packageName, Dataspace.RANGE_FULL, vivoBackupManager.getDualUserId());
            } catch (PackageManager.NameNotFoundException e) {
                VSlog.w(TAG, "Couldn't find package info: " + packageInfo.packageName, e);
            }
        }
        return packageInfo;
    }

    public void tearDownPipes() {
        ParcelFileDescriptor[] parcelFileDescriptorArr = this.mPipes;
        if (parcelFileDescriptorArr != null) {
            try {
                if (parcelFileDescriptorArr[0] != null) {
                    parcelFileDescriptorArr[0].close();
                }
            } catch (IOException e) {
                VSlog.w(TAG, "Couldn't close agent pipes[0]", e);
            }
            try {
                if (this.mPipes[1] != null) {
                    this.mPipes[1].close();
                }
            } catch (IOException e2) {
                VSlog.w(TAG, "Couldn't close agent pipes[1]", e2);
            }
        }
    }

    public void routeSocketDataToOutputFromVivo(ParcelFileDescriptor inPipe, OutputStream out) throws IOException {
        AbsVivoBackupManager vivoBackupManager;
        if (this.mRunFromVivo && (vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager()) != null) {
            DataInputStream in = new DataInputStream(new FileInputStream(inPipe.getFileDescriptor()));
            byte[] buffer = new byte[32768];
            long tmp = 0;
            long lastPostTime = 0;
            while (true) {
                int readInt = in.readInt();
                int chunkTotal = readInt;
                if (readInt <= 0) {
                    vivoBackupManager.addBackupCompleteSize(tmp, this.mFd);
                    return;
                }
                while (chunkTotal > 0) {
                    int toRead = chunkTotal > buffer.length ? buffer.length : chunkTotal;
                    int nRead = in.read(buffer, 0, toRead);
                    if (nRead < 0) {
                        VSlog.e(TAG, "Unexpectedly reached end of file while reading data");
                        throw new EOFException();
                    }
                    this.mMonitor.pulse("/* [read] */");
                    out.write(buffer, 0, nRead);
                    chunkTotal -= nRead;
                    this.mMonitor.pulse("/* [write] */");
                    tmp += nRead;
                    if (System.currentTimeMillis() - lastPostTime > 1000) {
                        vivoBackupManager.addBackupCompleteSize(tmp, this.mFd);
                        tmp = 0;
                        lastPostTime = System.currentTimeMillis();
                    }
                }
            }
        }
    }

    public void handleOneResult(int result, int token) {
        if (this.mRunFromVivo) {
            AbsVivoBackupManager vivoBackupManager = VivoBackupCommonUtil.getVivoBackupManager();
            if (vivoBackupManager != null) {
                removeToken(token);
                setBackupResult(result);
            }
        }
    }

    public IBackupAgent retryBindIfNull(IBackupAgent agent, ApplicationInfo app, Object userBackupManagerService) {
        int retryTimes = 0;
        UserBackupManagerService ubms = (UserBackupManagerService) userBackupManagerService;
        while (agent == null && ubms != null && retryTimes < 3) {
            retryTimes++;
            VSlog.i(TAG, "======== get agent null, try again time [" + retryTimes + "] in 5000 ========");
            try {
                Thread.sleep(5000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            agent = ubms.bindToAgentSynchronous(app, 1);
        }
        return agent;
    }
}