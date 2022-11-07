package com.android.server.locksettings;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import com.android.server.locksettings.LockSettingsStorage;
import com.vivo.services.backup.util.VivoBackupCommonUtil;
import java.io.File;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoLockSettingsStorageImpl implements IVivoLockSettingsStorage {
    private static final String TAG = "VivoLockSettingsStorageImpl";
    private SQLiteOpenHelper mBackupDBHelper;
    private Context mContext;
    private LockSettingsStorage.DatabaseHelper mDatabaseHelper;
    private Handler mHandler = LockSettingsService.sHandlerThread.getThreadHandler();
    private LockSettingsStorage mLockSettingsStorage;

    public VivoLockSettingsStorageImpl(LockSettingsStorage locksettingsstorage, LockSettingsStorage.DatabaseHelper dbHelper, Context context) {
        this.mLockSettingsStorage = locksettingsstorage;
        this.mDatabaseHelper = dbHelper;
        this.mContext = context;
        initializeBackupDB();
    }

    public void writeKeyValue(String key, String value, int userId) {
        writeBackupDB(key, value, userId);
        try {
            this.mLockSettingsStorage.writeKeyValue(this.mDatabaseHelper.getWritableDatabase(), key, value, userId);
        } catch (SQLiteException | IllegalStateException e) {
            VSlog.e(TAG, "catch Exception in writeKeyValue.", e);
        }
    }

    public String readKeyValue(String key, String defaultValue, int userId) {
        try {
            String result = this.mLockSettingsStorage.readKeyValue(key, defaultValue, userId);
            return result;
        } catch (SQLiteException e) {
            VSlog.e(TAG, "catch SQLiteException in readKeyValue.", e);
            return defaultValue;
        }
    }

    private void writeBackupDB(String key, String value, int userId) {
        try {
            if (this.mBackupDBHelper != null) {
                this.mLockSettingsStorage.writeKeyValue(this.mBackupDBHelper.getWritableDatabase(), key, value, userId);
            }
        } catch (Exception e) {
            VSlog.e(TAG, "catch Exception in writeBackupDB, just delete locksettings.db.bak", e);
            try {
                try {
                    this.mBackupDBHelper.close();
                } catch (Exception e1) {
                    VSlog.e(TAG, "catch Exception when closing mBackupDBHelper.", e1);
                }
            } finally {
                this.mBackupDBHelper = null;
                LockSettingsStorage.getDBFile("locksettings.db.bak").delete();
            }
        }
    }

    private void initializeBackupDB() {
        File backupDB = LockSettingsStorage.getDBFile("locksettings.db.bak");
        if (backupDB.exists()) {
            setBackupDBHelper();
        } else {
            this.mHandler.post(new Runnable() { // from class: com.android.server.locksettings.-$$Lambda$VivoLockSettingsStorageImpl$wyztc5G5FEnAIA9spI9AQfJJIe8
                @Override // java.lang.Runnable
                public final void run() {
                    VivoLockSettingsStorageImpl.this.lambda$initializeBackupDB$0$VivoLockSettingsStorageImpl();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: maybeCopyBackupDB */
    public void lambda$maybeCopyBackupDB$1$VivoLockSettingsStorageImpl() {
        File originDB = LockSettingsStorage.getDBFile("locksettings.db");
        File originDBWal = LockSettingsStorage.getDBFile("locksettings.db-wal");
        if (originDB.exists() && !originDBWal.exists()) {
            LockSettingsStorage.copyFile(originDB, LockSettingsStorage.getDBFile("locksettings.db.bak"));
            setBackupDBHelper();
            return;
        }
        VSlog.d(TAG, "Copy backup database later.");
        this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.locksettings.-$$Lambda$VivoLockSettingsStorageImpl$DwuPSP23iHB7-1Vo39huh9R6azA
            @Override // java.lang.Runnable
            public final void run() {
                VivoLockSettingsStorageImpl.this.lambda$maybeCopyBackupDB$1$VivoLockSettingsStorageImpl();
            }
        }, VivoBackupCommonUtil.TIMEOUT_MONITOR_INTERVAL);
    }

    private void setBackupDBHelper() {
        if (this.mBackupDBHelper != null) {
            return;
        }
        SQLiteOpenHelper sQLiteOpenHelper = new SQLiteOpenHelper(this.mContext, "locksettings.db.bak", null, 2) { // from class: com.android.server.locksettings.VivoLockSettingsStorageImpl.1
            @Override // android.database.sqlite.SQLiteOpenHelper
            public void onCreate(SQLiteDatabase db) {
            }

            @Override // android.database.sqlite.SQLiteOpenHelper
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            }
        };
        this.mBackupDBHelper = sQLiteOpenHelper;
        sQLiteOpenHelper.setWriteAheadLoggingEnabled(false);
    }
}