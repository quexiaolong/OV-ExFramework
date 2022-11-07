package com.vivo.services.backup;

import android.app.ActivityManager;
import android.app.IBackupAgent;
import android.app.backup.IFullBackupRestoreObserver;
import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.SparseArray;
import com.android.server.am.firewall.VivoFirewall;
import com.android.server.backup.BackupManagerService;
import com.vivo.face.common.data.Constants;
import com.vivo.services.backup.util.VivoBackupReflectUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import vivo.app.backup.IPackageBackupRestoreObserver;
import vivo.app.backup.IVivoBackupManager;
import vivo.app.backup.utils.DoubleInstanceUtil;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoBackupManagerService extends IVivoBackupManager.Stub {
    public static final String TAG = "VivoBackupManagerService";
    private Context mContext;
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakelock;
    public static int SUPPORT_FULL_BACKUP = 1;
    public static int SUPPORT_BACKUP_ZIP = 2;
    public static int SUPPORT_RESTORE_EXTERNAL_ZIP = 3;
    public static int SUPPORT_BACKUP_CUSTOMIZE_BACKUP = 6;
    public static int ERROR_COMMON = 1;
    public static int ERROR_NOT_SUPPORT = 2;
    public static int ERROR_TIMEOUT = 3;
    public static int ERROR_WAIT_PROCESS = 4;
    private static long WAIT_AGENT_BACKUP = 5000;
    private static long WAIT_AGENT_RESTORE = 10000;
    private static String KEY_EXCLUDE_DATA_DATA_BACKUP_DIR = "EXCLUDE_DATA_DATA_BACKUP_DIR";
    private static String KEY_EXCLUDE_ANDORID_DATA_BACKUP_DIR = "EXCLUDE_ANDORID_DATA_BACKUP_DIR";
    private static String KEY_EXCLUDE_DATA_DATA_RESTORE_DIR = "EXCLUDE_DATA_DATA_RESTORE_DIR";
    private static String KEY_EXCLUDE_ANDORID_DATA_RESTORE_DIR = "EXCLUDE_ANDORID_DATA_RESTORE_DIR";
    public static HashSet<Integer> BACKUP_SUPPORT_FEATURE = new HashSet<Integer>() { // from class: com.vivo.services.backup.VivoBackupManagerService.1
        {
            add(Integer.valueOf(VivoBackupManagerService.SUPPORT_BACKUP_ZIP));
            add(Integer.valueOf(VivoBackupManagerService.SUPPORT_FULL_BACKUP));
            add(Integer.valueOf(VivoBackupManagerService.SUPPORT_RESTORE_EXTERNAL_ZIP));
            add(Integer.valueOf(VivoBackupManagerService.SUPPORT_BACKUP_CUSTOMIZE_BACKUP));
        }
    };
    final Random mTokenGenerator = new Random();
    final AtomicInteger mNextToken = new AtomicInteger();
    private final ConcurrentHashMap<Object, Integer> mFdMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Object, Boolean> mDualUserMap = new ConcurrentHashMap<>();
    private boolean mDirFullBackupEnable = false;
    private Map<String, List<String>> mCustomizeBackupDirMap = new HashMap();
    private Map<String, List<String>> mCustomizeRestoreDirMap = new HashMap();
    private List<String> mDataDataExcludeDir = new ArrayList();
    private List<String> mAndroidDataExcludeDir = new ArrayList();
    private List<String> mDataDataRestoreIgnoreDir = new ArrayList();
    private List<String> mAndroidDataRestoreIgnoreDir = new ArrayList();
    private SparseArray<BackupRestoreParams> mParmslist = new SparseArray<>();

    public VivoBackupManagerService(Context context) {
        this.mContext = context;
        PowerManager powerManager = (PowerManager) context.getSystemService("power");
        this.mPowerManager = powerManager;
        this.mWakelock = powerManager.newWakeLock(1, "*vivo backup*");
    }

    public boolean checkSupportFeature(int feature) {
        return BACKUP_SUPPORT_FEATURE.contains(Integer.valueOf(feature));
    }

    /* JADX WARN: Removed duplicated region for block: B:90:0x0234  */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:47:0x0144 -> B:58:0x01a6). Please submit an issue!!! */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:73:0x01cc -> B:85:0x022e). Please submit an issue!!! */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:94:0x023f -> B:105:0x02a0). Please submit an issue!!! */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public boolean backupPackage(java.lang.String r20, android.os.ParcelFileDescriptor r21, vivo.app.backup.IPackageBackupRestoreObserver r22, boolean r23, boolean r24, boolean r25, boolean r26, boolean r27, boolean r28, boolean r29, boolean r30) throws android.os.RemoteException {
        /*
            Method dump skipped, instructions count: 723
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.vivo.services.backup.VivoBackupManagerService.backupPackage(java.lang.String, android.os.ParcelFileDescriptor, vivo.app.backup.IPackageBackupRestoreObserver, boolean, boolean, boolean, boolean, boolean, boolean, boolean, boolean):boolean");
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r0v15, types: [java.lang.String] */
    /* JADX WARN: Type inference failed for: r0v32, types: [int] */
    /* JADX WARN: Type inference failed for: r0v38, types: [java.lang.String] */
    /* JADX WARN: Type inference failed for: r0v9, types: [int] */
    public boolean restoreBackupFile(String pkgName, ParcelFileDescriptor readFile, String[] cmdList, IPackageBackupRestoreObserver observer) throws RemoteException {
        VSlog.d(TAG, "\n####################  [ " + pkgName + " ]  ####################");
        boolean z = false;
        if (pkgName == null || readFile == null || observer == null || cmdList == null) {
            VSlog.e(TAG, "get null params");
            if (observer != null) {
                try {
                    observer.onError(pkgName, 0, ERROR_COMMON);
                } catch (RemoteException e) {
                    VSlog.e(TAG, "cant connect PackageBackupRestoreObserver onError." + e);
                }
            }
            return false;
        } else if (!BACKUP_SUPPORT_FEATURE.contains(Integer.valueOf(SUPPORT_FULL_BACKUP))) {
            VSlog.e(TAG, "not support feature in restoreBackupFile");
            if (observer != null) {
                try {
                    observer.onError(pkgName, 0, ERROR_NOT_SUPPORT);
                } catch (RemoteException e2) {
                    VSlog.e(TAG, "cant connect PackageBackupRestoreObserver onError." + e2);
                }
            }
            return false;
        } else {
            this.mContext.enforceCallingPermission("android.permission.BACKUP", "backupPackages");
            synchronized (this.mParmslist) {
                for (int i = 0; i < this.mParmslist.size(); i++) {
                    if (this.mParmslist.valueAt(i).getCurrentPackage().equals(pkgName)) {
                        VSlog.w(TAG, "failed to restoreBackupFile because other BackupRestoreProcess is running for package:" + pkgName);
                        if (observer != null) {
                            try {
                                observer.onError(pkgName, 0, ERROR_WAIT_PROCESS);
                            } catch (RemoteException e3) {
                                VSlog.e(TAG, "cant connect PackageBackupRestoreObserver onError." + e3);
                            }
                        }
                        return false;
                    }
                }
                parseCustomizeRestoreDir();
                int callerFd = readFile.getFd();
                boolean result = true;
                this.mFdMap.put(pkgName, Integer.valueOf(callerFd));
                this.mDualUserMap.put(pkgName, false);
                BackupRestoreParams params = new BackupRestoreParams(pkgName, observer);
                params.setCmdList(cmdList);
                synchronized (this.mParmslist) {
                    this.mParmslist.put(callerFd, params);
                    VSlog.d(TAG, "restoreBackupFile -> " + pkgName + " , fd = " + callerFd + " , num = " + this.mParmslist.size());
                }
                boolean z2 = true;
                z2 = true;
                z2 = true;
                z2 = true;
                z2 = true;
                z2 = true;
                try {
                    try {
                        VivoBackupManagerServiceProxy.fullRestore(readFile);
                        synchronized (this.mParmslist) {
                            if (1 != 0) {
                                try {
                                    if (params.getResult()) {
                                        z = true;
                                    }
                                } finally {
                                }
                            }
                            result = z;
                            ?? indexOfKey = this.mParmslist.indexOfKey(callerFd);
                            z = indexOfKey;
                            if (indexOfKey >= 0) {
                                boolean containsKey = this.mFdMap.containsKey(pkgName);
                                z = containsKey;
                                if (containsKey) {
                                    this.mParmslist.remove(callerFd);
                                    this.mFdMap.remove(pkgName);
                                    String str = "Finish restoreBackupFile " + pkgName + " , total: " + params.getCompleteSize() + ", fd = " + callerFd + ", result: " + result + " , num = " + this.mParmslist.size();
                                    VSlog.d(TAG, str);
                                    z = TAG;
                                    z2 = str;
                                }
                            }
                        }
                    } catch (RemoteException e4) {
                        VSlog.e(TAG, "connect backup service failed!", e4);
                        synchronized (this.mParmslist) {
                            if (0 != 0) {
                                try {
                                    if (params.getResult()) {
                                        z = true;
                                    }
                                } finally {
                                }
                            }
                            result = z;
                            ?? indexOfKey2 = this.mParmslist.indexOfKey(callerFd);
                            z = indexOfKey2;
                            if (indexOfKey2 >= 0) {
                                boolean containsKey2 = this.mFdMap.containsKey(pkgName);
                                z = containsKey2;
                                if (containsKey2) {
                                    this.mParmslist.remove(callerFd);
                                    this.mFdMap.remove(pkgName);
                                    String str2 = "Finish restoreBackupFile " + pkgName + " , total: " + params.getCompleteSize() + ", fd = " + callerFd + ", result: " + result + " , num = " + this.mParmslist.size();
                                    VSlog.d(TAG, str2);
                                    z = TAG;
                                    z2 = str2;
                                }
                            }
                        }
                    }
                    return result;
                } catch (Throwable th) {
                    synchronized (this.mParmslist) {
                        if (result) {
                            try {
                                if (params.getResult()) {
                                    z = z2;
                                }
                            } finally {
                            }
                        }
                        boolean result2 = z;
                        if (this.mParmslist.indexOfKey(callerFd) >= 0 && this.mFdMap.containsKey(pkgName)) {
                            this.mParmslist.remove(callerFd);
                            this.mFdMap.remove(pkgName);
                            VSlog.d(TAG, "Finish restoreBackupFile " + pkgName + " , total: " + params.getCompleteSize() + ", fd = " + callerFd + ", result: " + result2 + " , num = " + this.mParmslist.size());
                        }
                        throw th;
                    }
                }
            }
        }
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Unreachable block: B:81:0x01f7
        	at jadx.core.dex.visitors.blocks.BlockProcessor.checkForUnreachableBlocks(BlockProcessor.java:81)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.processBlocksTree(BlockProcessor.java:47)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.visit(BlockProcessor.java:39)
        */
    public boolean backupPackageByZip(java.lang.String r35, android.os.ParcelFileDescriptor r36, boolean r37, java.lang.String[] r38, vivo.app.backup.IPackageBackupRestoreObserver r39) {
        /*
            Method dump skipped, instructions count: 3757
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.vivo.services.backup.VivoBackupManagerService.backupPackageByZip(java.lang.String, android.os.ParcelFileDescriptor, boolean, java.lang.String[], vivo.app.backup.IPackageBackupRestoreObserver):boolean");
    }

    /* JADX WARN: Can't wrap try/catch for region: R(3:(3:23|24|(5:27|28|(2:33|34)|30|31)(1:26))|20|21) */
    /* JADX WARN: Code restructure failed: missing block: B:444:0x0b00, code lost:
        r0 = th;
     */
    /* JADX WARN: Removed duplicated region for block: B:105:0x02cd  */
    /* JADX WARN: Removed duplicated region for block: B:366:0x0977  */
    /* JADX WARN: Removed duplicated region for block: B:374:0x09b2  */
    /* JADX WARN: Removed duplicated region for block: B:396:0x0a01  */
    /* JADX WARN: Removed duplicated region for block: B:415:0x0a87  */
    /* JADX WARN: Removed duplicated region for block: B:423:0x0ac2  */
    /* JADX WARN: Removed duplicated region for block: B:496:0x08f1 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:525:0x0206 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:97:0x0291  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public boolean restorePackageByZip(java.lang.String r29, android.os.ParcelFileDescriptor r30, boolean r31, java.lang.String[] r32, vivo.app.backup.IPackageBackupRestoreObserver r33) {
        /*
            Method dump skipped, instructions count: 2864
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.vivo.services.backup.VivoBackupManagerService.restorePackageByZip(java.lang.String, android.os.ParcelFileDescriptor, boolean, java.lang.String[], vivo.app.backup.IPackageBackupRestoreObserver):boolean");
    }

    /* JADX WARN: Can't wrap try/catch for region: R(9:(8:(3:148|149|(15:151|84|85|86|87|88|89|(6:129|130|131|132|133|(1:135))(1:91)|92|93|94|(2:96|(5:107|108|109|110|111)(1:98))(1:118)|(2:102|103)|100|101))|92|93|94|(0)(0)|(0)|100|101)|83|84|85|86|87|88|89|(0)(0)) */
    /* JADX WARN: Code restructure failed: missing block: B:215:0x0587, code lost:
        r0 = th;
     */
    /* JADX WARN: Code restructure failed: missing block: B:217:0x058f, code lost:
        r0 = th;
     */
    /* JADX WARN: Removed duplicated region for block: B:189:0x0510  */
    /* JADX WARN: Removed duplicated region for block: B:193:0x052c  */
    /* JADX WARN: Removed duplicated region for block: B:205:0x0555  */
    /* JADX WARN: Removed duplicated region for block: B:275:0x072b  */
    /* JADX WARN: Removed duplicated region for block: B:297:0x0777  */
    /* JADX WARN: Removed duplicated region for block: B:319:0x081c  */
    /* JADX WARN: Removed duplicated region for block: B:389:0x0682 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:396:0x055b A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:418:0x04ab A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:70:0x01e5  */
    /* JADX WARN: Removed duplicated region for block: B:96:0x0290  */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:301:0x0782 -> B:328:0x0846). Please submit an issue!!! */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public boolean restoreNoClearByZip(java.lang.String r27, android.os.ParcelFileDescriptor r28, boolean r29, java.lang.String[] r30, vivo.app.backup.IPackageBackupRestoreObserver r31, java.util.List<java.lang.String> r32, java.util.List<java.lang.String> r33, int r34) {
        /*
            Method dump skipped, instructions count: 2180
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.vivo.services.backup.VivoBackupManagerService.restoreNoClearByZip(java.lang.String, android.os.ParcelFileDescriptor, boolean, java.lang.String[], vivo.app.backup.IPackageBackupRestoreObserver, java.util.List, java.util.List, int):boolean");
    }

    private void killAppProcess(String pkgName, boolean enableDual) {
        ActivityManager am = (ActivityManager) this.mContext.getSystemService(VivoFirewall.TYPE_ACTIVITY);
        if (am != null) {
            am.forceStopPackageAsUser(pkgName, enableDual ? getDualUserId() : 0);
        } else {
            VSlog.e(TAG, "get am error !");
        }
    }

    public boolean isRunningFromVivoBackup(int fd) throws RemoteException {
        synchronized (this.mParmslist) {
            if (this.mParmslist.indexOfKey(fd) < 0) {
                return false;
            }
            return true;
        }
    }

    public boolean isDualPackageEnabled(String pkgName) throws RemoteException {
        return DoubleInstanceUtil.isCloneEnabled(pkgName);
    }

    public boolean enableDualPackage(String pkgName) throws RemoteException {
        if (isDualPackageEnabled(pkgName)) {
            return true;
        }
        return DoubleInstanceUtil.enableClone(pkgName);
    }

    public int getDualUserId() {
        return DoubleInstanceUtil.getDualUserId();
    }

    public boolean startConfirmationUi(final int token, String action, int fd) throws RemoteException {
        if (!isRunningFromVivoBackup(fd)) {
            VSlog.e(TAG, "Illegal call startConfirmationUi failed.");
            return false;
        }
        final FullBackupRestoreObserver fullBackupRestoreObserver = new FullBackupRestoreObserver(fd);
        new Thread(new Runnable() { // from class: com.vivo.services.backup.VivoBackupManagerService.2
            @Override // java.lang.Runnable
            public void run() {
                BackupManagerService backupManager = ServiceManager.getService("backup");
                try {
                    backupManager.acknowledgeFullBackupOrRestore(token, true, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, fullBackupRestoreObserver);
                } catch (RemoteException e) {
                    VSlog.e(VivoBackupManagerService.TAG, "invoke acknowledgeFullBackupOrRestore failed", e);
                }
            }
        }).start();
        return true;
    }

    public void addBackupCompleteSize(long size, int fd) {
        synchronized (this.mParmslist) {
            BackupRestoreParams params = this.mParmslist.get(fd);
            if (params != null) {
                params.setCompleteSize(params.getCompleteSize() + size);
                if (params.getObserver() != null) {
                    try {
                        params.getObserver().onProgress(params.getCurrentPackage(), 0, params.getCompleteSize(), -1L);
                    } catch (RemoteException e) {
                        VSlog.e(TAG, "cant connect PackageBackupRestoreObserver onProgress.");
                    }
                }
            }
        }
    }

    public void postRestoreCompleteSize(long size, int fd) {
        synchronized (this.mParmslist) {
            BackupRestoreParams params = this.mParmslist.get(fd);
            if (params != null) {
                params.setCompleteSize(size);
                if (params.getObserver() != null) {
                    try {
                        params.getObserver().onProgress(params.getCurrentPackage(), 0, params.getCompleteSize(), -1L);
                    } catch (RemoteException e) {
                        VSlog.e(TAG, "cant connect PackageBackupRestoreObserver onProgress.");
                    }
                }
            }
        }
    }

    public void onAgentBackupZipComplete(int fd, long result) {
        VSlog.d(TAG, "onAgentBackupZipComplete, fd=" + fd);
        synchronized (this.mParmslist) {
            BackupRestoreParams params = this.mParmslist.get(fd);
            if (params != null && params.getLatch() != null) {
                params.getLatch().countDown();
            }
        }
    }

    public void onAgentRestoreZipComplete(int fd, long result) {
        VSlog.d(TAG, "onAgentRestoreZipComplete, fd=" + fd);
        synchronized (this.mParmslist) {
            BackupRestoreParams params = this.mParmslist.get(fd);
            if (params != null && params.getLatch() != null) {
                params.getLatch().countDown();
            }
        }
    }

    public String[] getRestoreCmdList(int fd) {
        synchronized (this.mParmslist) {
            BackupRestoreParams params = this.mParmslist.get(fd);
            if (params != null) {
                return params.getCmdList();
            }
            return null;
        }
    }

    public void onError(int error, int fd) {
        String str;
        String str2;
        VSlog.d(TAG, "onError, error=" + error + " , fd=" + fd);
        synchronized (this.mParmslist) {
            BackupRestoreParams params = this.mParmslist.get(fd);
            if (params != null) {
                params.setResult(false);
                if (params.getObserver() != null) {
                    String currentPackage = params.getCurrentPackage();
                    try {
                        params.getObserver().onError(currentPackage, 0, error);
                    } catch (RemoteException e) {
                        VSlog.d(TAG, "cant connect PackageBackupRestoreObserver onError.");
                        if (this.mParmslist.indexOfKey(fd) >= 0 && this.mFdMap.containsKey(currentPackage)) {
                            this.mParmslist.remove(fd);
                            this.mFdMap.remove(currentPackage);
                            str = TAG;
                            str2 = "======== onError ======== , " + currentPackage + ", total: " + params.getCompleteSize() + ", remove params , fd == : " + fd + " ,paramsList size == " + this.mParmslist.size();
                        }
                    }
                    if (this.mParmslist.indexOfKey(fd) >= 0 && this.mFdMap.containsKey(currentPackage)) {
                        this.mParmslist.remove(fd);
                        this.mFdMap.remove(currentPackage);
                        str = TAG;
                        str2 = "======== onError ======== , " + currentPackage + ", total: " + params.getCompleteSize() + ", remove params , fd == : " + fd + " ,paramsList size == " + this.mParmslist.size();
                        VSlog.d(str, str2);
                    }
                }
            }
        }
    }

    public void putFdByToken(int token, int fd) throws RemoteException {
        if (!isFdMapContainsKey(Integer.valueOf(token))) {
            Integer tempFd = new Integer(fd);
            this.mFdMap.put(Integer.valueOf(token), tempFd);
        }
    }

    public int getFdByToken(int token) throws RemoteException {
        Integer fd;
        ConcurrentHashMap<Object, Integer> concurrentHashMap = this.mFdMap;
        if (concurrentHashMap != null && (fd = concurrentHashMap.get(Integer.valueOf(token))) != null) {
            return fd.intValue();
        }
        return -1;
    }

    public void removeToken(int token) throws RemoteException {
        if (isFdMapContainsKey(Integer.valueOf(token))) {
            this.mFdMap.remove(Integer.valueOf(token));
        }
    }

    public int getFdByPkg(String pkg) throws RemoteException {
        Integer fd;
        ConcurrentHashMap<Object, Integer> concurrentHashMap = this.mFdMap;
        if (concurrentHashMap != null && (fd = concurrentHashMap.get(pkg)) != null) {
            return fd.intValue();
        }
        return -1;
    }

    public void setDualUserByPkg(String pkg, boolean isDualUser) throws RemoteException {
        if (this.mDualUserMap != null) {
            Boolean tempIsDualUser = new Boolean(isDualUser);
            this.mDualUserMap.put(pkg, tempIsDualUser);
        }
    }

    public boolean isDualUserByPkg(String pkg) throws RemoteException {
        Boolean tempIsDualUser;
        ConcurrentHashMap<Object, Boolean> concurrentHashMap = this.mDualUserMap;
        if (concurrentHashMap != null && (tempIsDualUser = concurrentHashMap.get(pkg)) != null) {
            return tempIsDualUser.booleanValue();
        }
        return false;
    }

    public boolean isFdMapContainsKey(Object key) {
        ConcurrentHashMap<Object, Integer> concurrentHashMap = this.mFdMap;
        if (concurrentHashMap != null && key != null) {
            return concurrentHashMap.containsKey(key);
        }
        return false;
    }

    public void setDirFullBackupEnable(boolean isEnabled) throws RemoteException {
        this.mDirFullBackupEnable = isEnabled;
    }

    public boolean getDirFullBackupEnable() {
        return this.mDirFullBackupEnable;
    }

    public void setVivoBackupDir(Map customizeBackupDirMap) throws RemoteException {
        this.mCustomizeBackupDirMap = customizeBackupDirMap;
    }

    public void setVivoRestoreDir(Map customizeRestoreDirMap) throws RemoteException {
        this.mCustomizeRestoreDirMap = customizeRestoreDirMap;
    }

    public List<String> getDataDataExcludeDir() {
        return this.mDataDataExcludeDir;
    }

    public List<String> getAndroidDataExcludeDir() {
        return this.mAndroidDataExcludeDir;
    }

    public List<String> getDataDataRestoreIgnoreDir() {
        return this.mDataDataRestoreIgnoreDir;
    }

    public List<String> getAndroidDataRestoreIgnoreDir() {
        return this.mAndroidDataRestoreIgnoreDir;
    }

    private void parseCustomizeBackupDir() {
        Map<String, List<String>> map;
        if (this.mDirFullBackupEnable && (map = this.mCustomizeBackupDirMap) != null && !map.isEmpty()) {
            for (String mapKey : this.mCustomizeBackupDirMap.keySet()) {
                if (KEY_EXCLUDE_ANDORID_DATA_BACKUP_DIR.equals(mapKey)) {
                    this.mAndroidDataExcludeDir = this.mCustomizeBackupDirMap.get(mapKey);
                } else if (KEY_EXCLUDE_DATA_DATA_BACKUP_DIR.equals(mapKey)) {
                    this.mDataDataExcludeDir = this.mCustomizeBackupDirMap.get(mapKey);
                }
            }
        }
    }

    private void parseCustomizeRestoreDir() {
        Map<String, List<String>> map = this.mCustomizeRestoreDirMap;
        if (map != null && !map.isEmpty()) {
            for (String mapKey : this.mCustomizeRestoreDirMap.keySet()) {
                if (KEY_EXCLUDE_ANDORID_DATA_RESTORE_DIR.equals(mapKey)) {
                    this.mAndroidDataRestoreIgnoreDir = this.mCustomizeRestoreDirMap.get(mapKey);
                } else if (KEY_EXCLUDE_DATA_DATA_RESTORE_DIR.equals(mapKey)) {
                    this.mDataDataRestoreIgnoreDir = this.mCustomizeRestoreDirMap.get(mapKey);
                }
            }
        }
    }

    public int generateRandomIntegerToken() {
        int token = this.mTokenGenerator.nextInt();
        if (token < 0) {
            token = -token;
        }
        return (token & (-256)) | (this.mNextToken.incrementAndGet() & 255);
    }

    /* loaded from: classes.dex */
    public class FullBackupRestoreObserver extends IFullBackupRestoreObserver.Stub {
        int callerFd;

        public FullBackupRestoreObserver(int fd) {
            this.callerFd = fd;
        }

        public void onStartBackup() {
        }

        public void onBackupPackage(String pkgName) {
            synchronized (VivoBackupManagerService.this.mParmslist) {
                BackupRestoreParams params = (BackupRestoreParams) VivoBackupManagerService.this.mParmslist.get(this.callerFd);
                if (params != null && params.getObserver() != null) {
                    try {
                        params.getObserver().onStart(params.getCurrentPackage(), 0);
                    } catch (RemoteException e) {
                        VSlog.e(VivoBackupManagerService.TAG, "cant connect PackageBackupRestoreObserver backup onBackupPackage.");
                    }
                }
            }
        }

        public void onEndBackup() {
            synchronized (VivoBackupManagerService.this.mParmslist) {
                BackupRestoreParams params = (BackupRestoreParams) VivoBackupManagerService.this.mParmslist.get(this.callerFd);
                if (params != null && params.getObserver() != null) {
                    try {
                        params.getObserver().onEnd(params.getCurrentPackage(), 0);
                    } catch (RemoteException e) {
                        VSlog.d(VivoBackupManagerService.TAG, "cant connect PackageBackupRestoreObserver backup onEndBackup.");
                    }
                }
            }
        }

        public void onStartRestore() {
        }

        public void onRestorePackage(String pkgName) {
            synchronized (VivoBackupManagerService.this.mParmslist) {
                BackupRestoreParams params = (BackupRestoreParams) VivoBackupManagerService.this.mParmslist.get(this.callerFd);
                if (params != null && params.getObserver() != null) {
                    try {
                        params.getObserver().onStart(params.getCurrentPackage(), 0);
                    } catch (RemoteException e) {
                        VSlog.d(VivoBackupManagerService.TAG, "cant connect PackagBackupRestoreObserver onRestorePackage");
                    }
                }
            }
        }

        public void onEndRestore() {
            synchronized (VivoBackupManagerService.this.mParmslist) {
                BackupRestoreParams params = (BackupRestoreParams) VivoBackupManagerService.this.mParmslist.get(this.callerFd);
                if (params != null && params.getObserver() != null) {
                    try {
                        params.getObserver().onEnd(params.getCurrentPackage(), 0);
                    } catch (RemoteException e) {
                        VSlog.d(VivoBackupManagerService.TAG, "cant connect PackageBackupRestoreObserver restore onEndRestore.");
                    }
                }
            }
        }

        public void onTimeout() {
            String str;
            String str2;
            VSlog.d(VivoBackupManagerService.TAG, "onTimeout");
            synchronized (VivoBackupManagerService.this.mParmslist) {
                BackupRestoreParams params = (BackupRestoreParams) VivoBackupManagerService.this.mParmslist.get(this.callerFd);
                if (params != null) {
                    params.setResult(false);
                    if (params.getObserver() != null) {
                        String currentPackage = params.getCurrentPackage();
                        try {
                            params.getObserver().onError(currentPackage, 0, VivoBackupManagerService.ERROR_TIMEOUT);
                        } catch (RemoteException e) {
                            VSlog.d(VivoBackupManagerService.TAG, "cant connect PackageBackupRestoreObserver onTimeout.");
                            if (VivoBackupManagerService.this.mParmslist.indexOfKey(this.callerFd) >= 0 && VivoBackupManagerService.this.mFdMap.containsKey(currentPackage)) {
                                VivoBackupManagerService.this.mParmslist.remove(this.callerFd);
                                VivoBackupManagerService.this.mFdMap.remove(currentPackage);
                                str = VivoBackupManagerService.TAG;
                                str2 = "======== onTimeout ======== , " + currentPackage + ", total: " + params.getCompleteSize() + ", remove params , fd == : " + this.callerFd + " ,paramsList size == " + VivoBackupManagerService.this.mParmslist.size();
                            }
                        }
                        if (VivoBackupManagerService.this.mParmslist.indexOfKey(this.callerFd) >= 0 && VivoBackupManagerService.this.mFdMap.containsKey(currentPackage)) {
                            VivoBackupManagerService.this.mParmslist.remove(this.callerFd);
                            VivoBackupManagerService.this.mFdMap.remove(currentPackage);
                            str = VivoBackupManagerService.TAG;
                            str2 = "======== onTimeout ======== , " + currentPackage + ", total: " + params.getCompleteSize() + ", remove params , fd == : " + this.callerFd + " ,paramsList size == " + VivoBackupManagerService.this.mParmslist.size();
                            VSlog.d(str, str2);
                        }
                    }
                }
            }
        }
    }

    /* loaded from: classes.dex */
    class BackupZipRunner implements Runnable {
        IBackupAgent mAgent;
        boolean mAsDual;
        String[] mDirs;
        int mFd;
        IVivoBackupManager mListener;
        ParcelFileDescriptor mPipe;
        String mPkgName;

        BackupZipRunner(IVivoBackupManager listener, int fd, ParcelFileDescriptor pipe, String pkgName, String[] dirs, boolean asDual, IBackupAgent agent) {
            this.mListener = listener;
            this.mPkgName = pkgName;
            this.mPipe = pipe;
            this.mDirs = dirs;
            this.mAgent = agent;
            this.mAsDual = asDual;
            this.mFd = fd;
        }

        @Override // java.lang.Runnable
        public void run() {
            try {
                VivoBackupReflectUtil.callDoBackupByZip(this.mAgent, this.mFd, this.mPipe, this.mDirs, this.mAsDual, this.mListener);
            } catch (RemoteException e) {
                VSlog.e(VivoBackupManagerService.TAG, "Remote agent vanished during backupZip " + this.mPkgName);
            }
        }
    }

    /* loaded from: classes.dex */
    class RestoreZipRunner implements Runnable {
        IBackupAgent mAgent;
        String[] mCmdList;
        int mFd;
        IVivoBackupManager mListener;
        ParcelFileDescriptor mPipe;
        String mPkgName;

        RestoreZipRunner(IVivoBackupManager listener, int fd, ParcelFileDescriptor pipe, String pkgName, IBackupAgent agent, String[] cmdList) {
            this.mListener = listener;
            this.mPkgName = pkgName;
            this.mPipe = pipe;
            this.mAgent = agent;
            this.mCmdList = cmdList;
            this.mFd = fd;
        }

        @Override // java.lang.Runnable
        public void run() {
            try {
                VivoBackupReflectUtil.callDoRestoreByZip(this.mAgent, this.mFd, this.mPipe, this.mListener, this.mCmdList);
            } catch (RemoteException e) {
                VSlog.e(VivoBackupManagerService.TAG, "Remote agent vanished during RestoreZip " + this.mPkgName);
            }
        }
    }

    /* loaded from: classes.dex */
    class RestoreAndroidPathZipRunner implements Runnable {
        IBackupAgent mAgent;
        int mBufferSize;
        String[] mCmdList;
        int mFd;
        IVivoBackupManager mListener;
        List<String> mNewPath;
        List<String> mOldPath;
        ParcelFileDescriptor mPipe;
        String mPkgName;

        RestoreAndroidPathZipRunner(IVivoBackupManager listener, int fd, ParcelFileDescriptor pipe, String pkgName, IBackupAgent agent, String[] cmdList, List<String> oldPath, List<String> newPath, int bufferSize) {
            this.mListener = listener;
            this.mPkgName = pkgName;
            this.mPipe = pipe;
            this.mAgent = agent;
            this.mCmdList = cmdList;
            this.mFd = fd;
            this.mOldPath = oldPath;
            this.mNewPath = newPath;
            this.mBufferSize = bufferSize;
        }

        @Override // java.lang.Runnable
        public void run() {
            try {
                VivoBackupReflectUtil.callDoRestoreAndroidPathByZip(this.mAgent, this.mFd, this.mPipe, this.mListener, this.mCmdList, this.mOldPath, this.mNewPath, this.mBufferSize);
            } catch (RemoteException e) {
                VSlog.e(VivoBackupManagerService.TAG, "Remote agent vanished during RestoreZip " + this.mPkgName);
            }
        }
    }

    /* loaded from: classes.dex */
    public class BackupRestoreParams {
        private String currentPackage;
        private IPackageBackupRestoreObserver packageBackupRestoreObserver;
        private long completeSize = 0;
        private String[] cmdList = null;
        private CountDownLatch mLatch = null;
        private boolean result = true;

        public BackupRestoreParams(String pkg, IPackageBackupRestoreObserver observer) {
            this.currentPackage = pkg;
            this.packageBackupRestoreObserver = observer;
        }

        public long setCompleteSize(long size) {
            this.completeSize = size;
            return size;
        }

        public IPackageBackupRestoreObserver setObserver(IPackageBackupRestoreObserver observer) {
            this.packageBackupRestoreObserver = observer;
            return observer;
        }

        public String setCurrentPackage(String pkg) {
            this.currentPackage = pkg;
            return pkg;
        }

        public String[] setCmdList(String[] list) {
            this.cmdList = list;
            return list;
        }

        public long getCompleteSize() {
            return this.completeSize;
        }

        public IPackageBackupRestoreObserver getObserver() {
            return this.packageBackupRestoreObserver;
        }

        public String getCurrentPackage() {
            return this.currentPackage;
        }

        public String[] getCmdList() {
            return this.cmdList;
        }

        public CountDownLatch getLatch() {
            return this.mLatch;
        }

        public CountDownLatch setLatch(CountDownLatch latch) {
            this.mLatch = latch;
            return latch;
        }

        public boolean getResult() {
            return this.result;
        }

        public boolean setResult(boolean ret) {
            this.result = ret;
            return ret;
        }
    }
}