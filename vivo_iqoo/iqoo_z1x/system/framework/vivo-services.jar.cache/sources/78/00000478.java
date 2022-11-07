package com.android.server.policy.motion;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Pair;
import com.vivo.services.backup.util.VivoBackupCommonUtil;
import java.io.File;
import java.util.List;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class InputUsageDatabase {
    private static final String COL_ACTION = "action";
    private static final String COL_DISPLAY_ID = "display_id";
    private static final String COL_TIME_MS = "time_ms";
    private static final String DB_NAME = "input_usagestats.db";
    private static final int DB_VERSION = 1;
    private static final String DELETE_DATA_OUT_OF_TIME = "DELETE FROM %s WHERE time_ms<%d";
    private static final String DROP_TABLE = "DROP TABLE IF EXISTS %s";
    private static final int IDLE_CONNECTION_TIMEOUT_MS = 30000;
    private static final String INSERT_OR_REPLACE = "INSERT OR REPLACE INTO %s (action,display_id,time_ms) VALUES (?,?,?)";
    private static final String QUERY_ALL_TABLES = "SELECT name FROM sqlite_master WHERE TYPE='table' ORDER BY name";
    private static final String QUERY_COUNT_BETWEEN_TIME = "time_ms>? AND time_ms<? AND action=? AND display_id=?";
    private static final String TAB_USAGE_STATS = "usagestats_";
    private static final String TAB_USAGE_STATS_USER = "usagestats_%d";
    private static final String TAG = "InputUsageDatabase";
    SQLiteOpenHelper mHelper;

    public InputUsageDatabase(Context context) {
        this.mHelper = new SQLiteOpenHelper(context, DB_NAME, null, 1) { // from class: com.android.server.policy.motion.InputUsageDatabase.1
            @Override // android.database.sqlite.SQLiteOpenHelper
            public void onCreate(SQLiteDatabase db) {
                InputUsageDatabase.this.createTableForUserIfNotExist(db, 0);
            }

            @Override // android.database.sqlite.SQLiteOpenHelper
            public void onConfigure(SQLiteDatabase db) {
                setIdleConnectionTimeout(VivoBackupCommonUtil.TIMEOUT_MONITOR_INTERVAL);
            }

            @Override // android.database.sqlite.SQLiteOpenHelper
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            }
        };
    }

    /* JADX WARN: Removed duplicated region for block: B:33:0x0091  */
    /* JADX WARN: Removed duplicated region for block: B:34:0x00ba  */
    /* JADX WARN: Removed duplicated region for block: B:41:0x00c3  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public void writeEventList(java.util.ArrayList<com.android.server.policy.motion.VivoInputUsageStatsListener.EventEntry> r17, int r18) {
        /*
            r16 = this;
            r1 = r16
            java.lang.String r2 = "InputUsageDatabase"
            int r3 = r17.size()
            if (r3 > 0) goto Lb
            return
        Lb:
            long r4 = android.os.SystemClock.elapsedRealtime()
            android.database.sqlite.SQLiteOpenHelper r0 = r1.mHelper
            android.database.sqlite.SQLiteDatabase r6 = r0.getWritableDatabase()
            r0 = 1
            java.lang.Object[] r7 = new java.lang.Object[r0]
            java.lang.String r8 = getUserTableName(r18)
            r9 = 0
            r7[r9] = r8
            java.lang.String r8 = "INSERT OR REPLACE INTO %s (action,display_id,time_ms) VALUES (?,?,?)"
            java.lang.String r7 = java.lang.String.format(r8, r7)
            r8 = 0
            r6.beginTransaction()     // Catch: java.lang.Throwable -> L6a android.database.sqlite.SQLiteException -> L72
            r10 = 0
        L2a:
            if (r10 >= r3) goto L5b
            r11 = r17
            java.lang.Object r12 = r11.get(r10)     // Catch: android.database.sqlite.SQLiteException -> L68 java.lang.Throwable -> Lbf
            com.android.server.policy.motion.VivoInputUsageStatsListener$EventEntry r12 = (com.android.server.policy.motion.VivoInputUsageStatsListener.EventEntry) r12     // Catch: android.database.sqlite.SQLiteException -> L68 java.lang.Throwable -> Lbf
            if (r12 == 0) goto L55
            r13 = 3
            java.lang.Object[] r13 = new java.lang.Object[r13]     // Catch: android.database.sqlite.SQLiteException -> L68 java.lang.Throwable -> Lbf
            int r14 = r12.action     // Catch: android.database.sqlite.SQLiteException -> L68 java.lang.Throwable -> Lbf
            java.lang.Integer r14 = java.lang.Integer.valueOf(r14)     // Catch: android.database.sqlite.SQLiteException -> L68 java.lang.Throwable -> Lbf
            r13[r9] = r14     // Catch: android.database.sqlite.SQLiteException -> L68 java.lang.Throwable -> Lbf
            int r14 = r12.displayId     // Catch: android.database.sqlite.SQLiteException -> L68 java.lang.Throwable -> Lbf
            java.lang.Integer r14 = java.lang.Integer.valueOf(r14)     // Catch: android.database.sqlite.SQLiteException -> L68 java.lang.Throwable -> Lbf
            r13[r0] = r14     // Catch: android.database.sqlite.SQLiteException -> L68 java.lang.Throwable -> Lbf
            r14 = 2
            long r0 = r12.timeMs     // Catch: android.database.sqlite.SQLiteException -> L68 java.lang.Throwable -> Lbf
            java.lang.Long r0 = java.lang.Long.valueOf(r0)     // Catch: android.database.sqlite.SQLiteException -> L68 java.lang.Throwable -> Lbf
            r13[r14] = r0     // Catch: android.database.sqlite.SQLiteException -> L68 java.lang.Throwable -> Lbf
            r6.execSQL(r7, r13)     // Catch: android.database.sqlite.SQLiteException -> L68 java.lang.Throwable -> Lbf
        L55:
            int r10 = r10 + 1
            r0 = 1
            r1 = r16
            goto L2a
        L5b:
            r11 = r17
            r6.setTransactionSuccessful()     // Catch: android.database.sqlite.SQLiteException -> L68 java.lang.Throwable -> Lbf
            if (r8 != 0) goto L65
            r6.endTransaction()
        L65:
            r1 = r16
            goto L85
        L68:
            r0 = move-exception
            goto L75
        L6a:
            r0 = move-exception
            r11 = r17
        L6d:
            r1 = r16
        L6f:
            r12 = r18
            goto Lc1
        L72:
            r0 = move-exception
            r11 = r17
        L75:
            java.lang.String r1 = "save to database fail, delete db"
            vivo.util.VSlog.e(r2, r1, r0)     // Catch: java.lang.Throwable -> Lbf
            r8 = 1
            r1 = r16
            r1.deleteDatabase(r6)     // Catch: java.lang.Throwable -> Lbd
            if (r8 != 0) goto L85
            r6.endTransaction()
        L85:
            long r9 = android.os.SystemClock.elapsedRealtime()
            long r12 = r9 - r4
            r14 = 200(0xc8, double:9.9E-322)
            int r0 = (r12 > r14 ? 1 : (r12 == r14 ? 0 : -1))
            if (r0 <= 0) goto Lba
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r12 = "insert "
            r0.append(r12)
            r0.append(r3)
            java.lang.String r12 = " entries, cost "
            r0.append(r12)
            long r12 = r9 - r4
            r0.append(r12)
            java.lang.String r12 = " ms for user "
            r0.append(r12)
            r12 = r18
            r0.append(r12)
            java.lang.String r0 = r0.toString()
            vivo.util.VSlog.d(r2, r0)
            goto Lbc
        Lba:
            r12 = r18
        Lbc:
            return
        Lbd:
            r0 = move-exception
            goto L6f
        Lbf:
            r0 = move-exception
            goto L6d
        Lc1:
            if (r8 != 0) goto Lc6
            r6.endTransaction()
        Lc6:
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.policy.motion.InputUsageDatabase.writeEventList(java.util.ArrayList, int):void");
    }

    public void deleteDataOfUser(int userId) {
        SQLiteDatabase db = this.mHelper.getWritableDatabase();
        String sql = String.format(DROP_TABLE, getUserTableName(userId));
        try {
            db.execSQL(sql);
        } catch (Exception e) {
            VSlog.e(TAG, "delete user data fail user:" + userId, e);
        }
    }

    public void deleteDataOutOfTimeForAllUsers(long minTime) {
        SQLiteDatabase db = this.mHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery(QUERY_ALL_TABLES, null);
        while (cursor.moveToNext()) {
            try {
                try {
                    String table_name = cursor.getString(0);
                    if (checkTableNameIsUserData(table_name)) {
                        String sql = String.format(DELETE_DATA_OUT_OF_TIME, table_name, Long.valueOf(minTime));
                        db.execSQL(sql);
                    }
                } catch (Exception e) {
                    VSlog.e(TAG, "delete user data fail", e);
                }
            } finally {
                cursor.close();
            }
        }
    }

    public int queryEvents(int action, int displayId, int userId, long beginTime, long endTime) {
        int count;
        long begin = SystemClock.elapsedRealtime();
        SQLiteDatabase db = this.mHelper.getReadableDatabase();
        try {
            String[] selectArgs = {String.valueOf(beginTime), String.valueOf(endTime), String.valueOf(action), String.valueOf(displayId)};
            String tableName = getUserTableName(userId);
            count = (int) DatabaseUtils.queryNumEntries(db, tableName, QUERY_COUNT_BETWEEN_TIME, selectArgs);
        } catch (Exception e) {
            VSlog.e(TAG, "query table is not exist:" + e.fillInStackTrace());
            count = 0;
        }
        long end = SystemClock.elapsedRealtime();
        if (end - begin > 200) {
            VSlog.d(TAG, "query cost " + (end - begin) + " ms");
        }
        return count;
    }

    public void createTableForUserIfNotExist(int userId) {
        SQLiteDatabase db = this.mHelper.getWritableDatabase();
        createTableForUserIfNotExist(db, userId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void createTableForUserIfNotExist(SQLiteDatabase db, int userId) {
        String tableName = getUserTableName(userId);
        db.execSQL("CREATE TABLE IF NOT EXISTS " + tableName + " (_id INTEGER PRIMARY KEY AUTOINCREMENT," + COL_ACTION + " INT," + COL_DISPLAY_ID + " INT," + COL_TIME_MS + " INT UNIQUE)");
    }

    private static String getUserTableName(int userId) {
        return String.format(TAB_USAGE_STATS_USER, Integer.valueOf(userId));
    }

    private static boolean checkTableNameIsUserData(String tableName) {
        return !TextUtils.isEmpty(tableName) && tableName.contains(TAB_USAGE_STATS);
    }

    private void deleteDatabase(SQLiteDatabase dbObj) {
        if (!dbObj.isOpen()) {
            deleteDatabaseFile(dbObj.getPath());
            return;
        }
        List<Pair<String, String>> attachedDbs = null;
        try {
            try {
                attachedDbs = dbObj.getAttachedDbs();
            } finally {
                if (attachedDbs != null) {
                    for (Pair<String, String> p : attachedDbs) {
                        deleteDatabaseFile((String) p.second);
                    }
                } else {
                    deleteDatabaseFile(dbObj.getPath());
                }
            }
        } catch (SQLiteException e) {
        }
        try {
            dbObj.close();
        } catch (SQLiteException e2) {
        }
    }

    private void deleteDatabaseFile(String fileName) {
        if (fileName.equalsIgnoreCase(":memory:") || fileName.trim().length() == 0) {
            return;
        }
        VSlog.e(TAG, "deleting the database file: " + fileName);
        try {
            SQLiteDatabase.deleteDatabase(new File(fileName));
        } catch (Exception e) {
            VSlog.w(TAG, "delete failed: " + e.getMessage());
        }
    }
}