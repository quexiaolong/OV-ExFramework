package com.vivo.services.security.server.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.SparseArray;
import com.vivo.services.security.client.VivoPermissionType;
import com.vivo.services.security.server.VivoPermissionConfig;
import com.vivo.services.security.server.VivoPermissionService;
import java.io.File;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoSecurityDBHelper extends SQLiteOpenHelper {
    public static final String ATTR_IS_BLACKLIST = "IS_BLACKLIST";
    public static final String ATTR_IS_CONFIGURED = "IS_CONFIGURED";
    public static final String ATTR_IS_WHITELIST = "IS_WHITELIST";
    public static final String ATTR_PACKAGE = "PACKAGE";
    private static final String DATABASE_NAME = "vivo_security.db";
    private static final int DATABASE_VERSION = 6;
    public static final String TABLE_PERMISSION = "PERMISSION";
    private static final byte[] mLock = new byte[0];
    private static SparseArray<VivoSecurityDBHelper> sDBHelpers = new SparseArray<>();
    private int userId;

    public static int getDatabaseVersion() {
        return 6;
    }

    public VivoSecurityDBHelper(Context context) {
        super(context, DATABASE_NAME, (SQLiteDatabase.CursorFactory) null, 6);
        this.userId = 0;
        this.userId = 0;
    }

    public VivoSecurityDBHelper(Context context, int userId) {
        super(context, userId + "__" + DATABASE_NAME, (SQLiteDatabase.CursorFactory) null, 6);
        this.userId = 0;
        this.userId = userId;
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onCreate(SQLiteDatabase db) {
        VivoPermissionService.printfInfo("VSDB onCreate START");
        createTables(db);
        VivoPermissionService.printfInfo("VSDB onCreate END");
        VivoPermissionConfig.setDataBaseState(2, this.userId);
    }

    private void createTables(SQLiteDatabase db) {
        VivoPermissionService.printfInfo("VSDB createTables 1");
        StringBuffer permissionSql = new StringBuffer();
        permissionSql.append("CREATE TABLE ");
        permissionSql.append(TABLE_PERMISSION);
        permissionSql.append(" (");
        permissionSql.append(ATTR_PACKAGE);
        permissionSql.append(" TEXT NOT NULL PRIMARY KEY,");
        permissionSql.append(ATTR_IS_WHITELIST);
        permissionSql.append(" BOOLEAN,");
        permissionSql.append(ATTR_IS_BLACKLIST);
        permissionSql.append(" BOOLEAN,");
        for (int index = 0; index < 32; index++) {
            permissionSql.append(VivoPermissionType.getVPType(index));
            permissionSql.append(" INTEGER NOT NULL,");
        }
        for (int ftimeIndex = 0; ftimeIndex < 32; ftimeIndex++) {
            permissionSql.append("FTime");
            permissionSql.append(VivoPermissionType.getVPType(ftimeIndex));
            permissionSql.append(" INTEGER NOT NULL,");
        }
        permissionSql.append(ATTR_IS_CONFIGURED);
        permissionSql.append(" BOOLEAN");
        permissionSql.append(");");
        VivoPermissionService.printfInfo("VSDB" + permissionSql.toString());
        VivoPermissionService.printfInfo("VSDB createTables 2");
        db.execSQL(permissionSql.toString());
        VivoPermissionService.printfInfo("VSDB createTables 3");
    }

    public void removeDb(SQLiteDatabase db) {
        File databaseFile = new File(db.getPath());
        onBeforeDelete(db);
        db.close();
        SQLiteDatabase.deleteDatabase(databaseFile);
    }

    private void clearTables(SQLiteDatabase db) {
        VivoPermissionService.printfInfo("VSDB clearTables START");
        db.execSQL("DROP TABLE PERMISSION");
        VivoPermissionService.printfInfo("VSDB clearTables END");
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        int oldVersion2 = oldVersion;
        VivoPermissionService.printfInfo("VSDB onUpgrade: start currentVersion : " + db.getVersion() + " oldVersion=" + oldVersion2 + ";newVersion=" + newVersion);
        long startWhen = System.nanoTime();
        if (1 == oldVersion2 && 2 == newVersion) {
            clearTables(db);
            createTables(db);
            oldVersion2 = 2;
        }
        if (2 == oldVersion2 && 3 == newVersion) {
            VivoPermissionService.printfInfo("VSDB onUpgrade23:1");
            for (int i = 24; i < 32; i++) {
                String columns_add1 = "ALTER TABLE PERMISSION ADD " + VivoPermissionType.getVPType(i) + " INTEGER NOT NULL DEFAULT -2";
                VivoPermissionService.printfInfo("VSDB onUpgrade columns_add1 =" + columns_add1);
                db.execSQL(columns_add1);
            }
            VivoPermissionService.printfInfo("VSDB onUpgrade23:2");
            for (int j = 0; j < 32; j++) {
                String columns_add2 = "ALTER TABLE PERMISSION ADD FTime" + VivoPermissionType.getVPType(j) + " INTEGER NOT NULL DEFAULT 3";
                VivoPermissionService.printfInfo("VSDB onUpgrade23 columns_add2 =" + columns_add2);
                db.execSQL(columns_add2);
            }
            oldVersion2 = 3;
            VivoPermissionService.printfInfo("VSDB onUpgrade23:3");
        }
        if (2 == oldVersion2 && 4 == newVersion) {
            VivoPermissionService.printfInfo("VSDB onUpgrade24:1");
            clearTables(db);
            VivoPermissionService.printfInfo("VSDB onUpgrade24:2");
            createTables(db);
            VivoPermissionService.printfInfo("VSDB onUpgrade24:3");
            oldVersion2 = 4;
        }
        if (3 == oldVersion2 && 4 == newVersion) {
            for (int j2 = 0; j2 < 32; j2++) {
                String columns_rename = "UPDATE PERMISSION SET " + VivoPermissionType.getVPType(j2) + "=3 WHERE " + VivoPermissionType.getVPType(j2) + "=1";
                VivoPermissionService.printfInfo("VSDB onUpgrade341-1 j = " + j2 + " columns_rename =" + columns_rename);
                db.execSQL(columns_rename);
                String columns_rename2 = "UPDATE PERMISSION SET " + VivoPermissionType.getVPType(j2) + "=2 WHERE " + VivoPermissionType.getVPType(j2) + "=-1";
                VivoPermissionService.printfInfo("VSDB onUpgrade341-2 j = " + j2 + " columns_rename =" + columns_rename2);
                db.execSQL(columns_rename2);
                String columns_rename3 = "UPDATE PERMISSION SET " + VivoPermissionType.getVPType(j2) + "=1 WHERE " + VivoPermissionType.getVPType(j2) + "=0";
                VivoPermissionService.printfInfo("VSDB onUpgrade341-3 j = " + j2 + " columns_rename =" + columns_rename3);
                db.execSQL(columns_rename3);
                String columns_rename4 = "UPDATE PERMISSION SET " + VivoPermissionType.getVPType(j2) + "=0 WHERE " + VivoPermissionType.getVPType(j2) + "=-2";
                VivoPermissionService.printfInfo("VSDB onUpgrade341-4 j = " + j2 + " columns_rename =" + columns_rename4);
                db.execSQL(columns_rename4);
            }
            String columns_rename5 = "UPDATE PERMISSION SET " + VivoPermissionType.getVPType(4) + "=2 WHERE " + VivoPermissionType.getVPType(6) + "=2";
            VivoPermissionService.printfInfo("VSDB onUpgrade342-1  columns_rename =" + columns_rename5);
            db.execSQL(columns_rename5);
            String columns_rename6 = "UPDATE PERMISSION SET " + VivoPermissionType.getVPType(6) + "=2 WHERE " + VivoPermissionType.getVPType(4) + "=2";
            VivoPermissionService.printfInfo("VSDB onUpgrade342-2  columns_rename =" + columns_rename6);
            db.execSQL(columns_rename6);
            String columns_rename7 = "UPDATE PERMISSION SET " + VivoPermissionType.getVPType(4) + "=3 WHERE " + VivoPermissionType.getVPType(6) + "=3";
            VivoPermissionService.printfInfo("VSDB onUpgrade342-3  columns_rename =" + columns_rename7);
            db.execSQL(columns_rename7);
            String columns_rename8 = "UPDATE PERMISSION SET " + VivoPermissionType.getVPType(5) + "=2 WHERE " + VivoPermissionType.getVPType(7) + "=2";
            VivoPermissionService.printfInfo("VSDB onUpgrade343-1  columns_rename =" + columns_rename8);
            db.execSQL(columns_rename8);
            String columns_rename9 = "UPDATE PERMISSION SET " + VivoPermissionType.getVPType(7) + "=2 WHERE " + VivoPermissionType.getVPType(5) + "=2";
            VivoPermissionService.printfInfo("VSDB onUpgrade343-2  columns_rename =" + columns_rename9);
            db.execSQL(columns_rename9);
            String columns_rename10 = "UPDATE PERMISSION SET " + VivoPermissionType.getVPType(5) + "=3 WHERE " + VivoPermissionType.getVPType(7) + "=3";
            VivoPermissionService.printfInfo("VSDB onUpgrade343-3  columns_rename =" + columns_rename10);
            db.execSQL(columns_rename10);
            String columns_rename11 = "UPDATE PERMISSION SET " + VivoPermissionType.getVPType(14) + "=2 WHERE " + VivoPermissionType.getVPType(15) + "=2";
            VivoPermissionService.printfInfo("VSDB onUpgrade344-1  columns_rename =" + columns_rename11);
            db.execSQL(columns_rename11);
            String columns_rename12 = "UPDATE PERMISSION SET " + VivoPermissionType.getVPType(15) + "=2 WHERE " + VivoPermissionType.getVPType(14) + "=2";
            VivoPermissionService.printfInfo("VSDB onUpgrade344-2  columns_rename =" + columns_rename12);
            db.execSQL(columns_rename12);
            String columns_rename13 = "UPDATE PERMISSION SET " + VivoPermissionType.getVPType(14) + "=3 WHERE " + VivoPermissionType.getVPType(15) + "=3";
            VivoPermissionService.printfInfo("VSDB onUpgrade344-3  columns_rename =" + columns_rename13);
            db.execSQL(columns_rename13);
            String columns_rename14 = "UPDATE PERMISSION SET " + VivoPermissionType.getVPType(6) + "=0";
            VivoPermissionService.printfInfo("VSDB onUpgrade345-1  columns_rename =" + columns_rename14);
            db.execSQL(columns_rename14);
            String columns_rename15 = "UPDATE PERMISSION SET " + VivoPermissionType.getVPType(7) + "=0";
            VivoPermissionService.printfInfo("VSDB onUpgrade345-1  columns_rename =" + columns_rename15);
            db.execSQL(columns_rename15);
            String columns_rename16 = "UPDATE PERMISSION SET " + VivoPermissionType.getVPType(15) + "=0";
            VivoPermissionService.printfInfo("VSDB onUpgrade345-1  columns_rename =" + columns_rename16);
            db.execSQL(columns_rename16);
            for (int k = 0; k < 32; k++) {
                String columns_rename17 = "UPDATE PERMISSION SET FTime" + VivoPermissionType.getVPType(k) + "=" + VivoPermissionType.getVPType(k);
                VivoPermissionService.printfInfo("VSDB onUpgrade346-1 k = " + k + " columns_rename =" + columns_rename17);
                db.execSQL(columns_rename17);
            }
            String columns_rename18 = "UPDATE PERMISSION SET " + VivoPermissionType.getVPType(15) + "=0";
            VivoPermissionService.printfInfo("VSDB onUpgrade347-1  columns_rename =" + columns_rename18);
            db.execSQL(columns_rename18);
            oldVersion2 = 4;
        }
        if (oldVersion2 < 6) {
            upgradeToVersion6(db);
            oldVersion2 = 6;
        }
        if (oldVersion2 != newVersion) {
            throw new IllegalStateException("error upgrading the database to version " + newVersion);
        }
        long endWhen = System.nanoTime();
        VivoPermissionService.printfInfo("VSDB onUpgrade: end " + ((endWhen - startWhen) / 1000000) + "ms");
        VivoPermissionConfig.setDataBaseState(3, this.userId);
    }

    private void upgradeToVersion6(SQLiteDatabase db) {
        VivoPermissionService.printfInfo("VSDB onUpgradeVersionTo6");
        try {
            if (!isColumnExist(db, TABLE_PERMISSION, VivoPermissionType.getVPType(30).toString())) {
                String columns_add1 = "ALTER TABLE PERMISSION ADD " + VivoPermissionType.getVPType(30) + " INTEGER NOT NULL DEFAULT 3";
                VivoPermissionService.printfInfo("VSDB onUpgrade45 columns_add1 =" + columns_add1);
                db.execSQL(columns_add1);
                String columns_add2 = "ALTER TABLE PERMISSION ADD FTime" + VivoPermissionType.getVPType(30) + " INTEGER NOT NULL DEFAULT 3";
                VivoPermissionService.printfInfo("VSDB onUpgrade45 columns_add2 =" + columns_add2);
                db.execSQL(columns_add2);
            }
            if (!isColumnExist(db, TABLE_PERMISSION, VivoPermissionType.getVPType(31).toString())) {
                String columns_add12 = "ALTER TABLE PERMISSION ADD " + VivoPermissionType.getVPType(31) + " INTEGER NOT NULL DEFAULT 3";
                VivoPermissionService.printfInfo("VSDB onUpgrade45 columns_add1 =" + columns_add12);
                db.execSQL(columns_add12);
                String columns_add22 = "ALTER TABLE PERMISSION ADD FTime" + VivoPermissionType.getVPType(31) + " INTEGER NOT NULL DEFAULT 3";
                VivoPermissionService.printfInfo("VSDB onUpgrade45 columns_add2 =" + columns_add22);
                db.execSQL(columns_add22);
            }
        } catch (Exception e) {
            VivoPermissionService.printfError(e.toString());
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:13:0x0026, code lost:
        if (r0 != null) goto L16;
     */
    /* JADX WARN: Code restructure failed: missing block: B:21:0x0033, code lost:
        if (0 == 0) goto L13;
     */
    /* JADX WARN: Code restructure failed: missing block: B:22:0x0035, code lost:
        r1 = true;
        r0.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:23:0x0039, code lost:
        com.vivo.services.security.server.VivoPermissionService.printfInfo("isColumnExist: exist = " + r1 + ", cursor = " + r0 + ", table = " + r14 + ", column = " + r15);
     */
    /* JADX WARN: Code restructure failed: missing block: B:24:0x0065, code lost:
        return r1;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private boolean isColumnExist(android.database.sqlite.SQLiteDatabase r13, java.lang.String r14, java.lang.String r15) {
        /*
            r12 = this;
            r0 = 0
            if (r15 == 0) goto L66
            int r1 = r15.length()
            if (r1 <= 0) goto L66
            if (r13 == 0) goto L66
            if (r14 == 0) goto L66
            int r1 = r14.length()
            if (r1 > 0) goto L14
            goto L66
        L14:
            r1 = 0
            r2 = 0
            r3 = 1
            java.lang.String[] r6 = new java.lang.String[r3]     // Catch: java.lang.Throwable -> L29 java.lang.Exception -> L31
            r6[r0] = r15     // Catch: java.lang.Throwable -> L29 java.lang.Exception -> L31
            r7 = 0
            r8 = 0
            r9 = 0
            r10 = 0
            r11 = 0
            r4 = r13
            r5 = r14
            android.database.Cursor r0 = r4.query(r5, r6, r7, r8, r9, r10, r11)     // Catch: java.lang.Throwable -> L29 java.lang.Exception -> L31
            if (r0 == 0) goto L39
            goto L35
        L29:
            r0 = move-exception
            if (r2 == 0) goto L30
            r1 = 1
            r2.close()
        L30:
            throw r0
        L31:
            r0 = move-exception
            r0 = 0
            if (r0 == 0) goto L39
        L35:
            r1 = 1
            r0.close()
        L39:
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.String r3 = "isColumnExist: exist = "
            r2.append(r3)
            r2.append(r1)
            java.lang.String r3 = ", cursor = "
            r2.append(r3)
            r2.append(r0)
            java.lang.String r3 = ", table = "
            r2.append(r3)
            r2.append(r14)
            java.lang.String r3 = ", column = "
            r2.append(r3)
            r2.append(r15)
            java.lang.String r2 = r2.toString()
            com.vivo.services.security.server.VivoPermissionService.printfInfo(r2)
            return r1
        L66:
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.vivo.services.security.server.db.VivoSecurityDBHelper.isColumnExist(android.database.sqlite.SQLiteDatabase, java.lang.String, java.lang.String):boolean");
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        VivoPermissionService.printfInfo("VSDB onDowngrade:oldVersion=" + oldVersion + ";newVersion=" + newVersion);
        if (oldVersion > newVersion) {
            clearTables(db);
            createTables(db);
        }
    }

    public static VivoSecurityDBHelper getInstance(Context paramContext, int userId) {
        VivoSecurityDBHelper sDBHelper;
        synchronized (sDBHelpers) {
            if (sDBHelpers.get(userId) == null) {
                if (userId == 0) {
                    try {
                        File dbFile_0 = paramContext.getDatabasePath(userId + "__" + DATABASE_NAME);
                        StringBuilder sb = new StringBuilder();
                        sb.append("dbFile_0 is exists: ");
                        sb.append(dbFile_0.exists());
                        VivoPermissionService.printfDebug(sb.toString());
                        if (dbFile_0.exists()) {
                            sDBHelper = new VivoSecurityDBHelper(paramContext, userId);
                        } else {
                            sDBHelper = new VivoSecurityDBHelper(paramContext);
                        }
                    } catch (Exception e) {
                        VSlog.e("VPS", "rename dbfile throw exception", e);
                        sDBHelper = new VivoSecurityDBHelper(paramContext);
                    }
                } else {
                    sDBHelper = new VivoSecurityDBHelper(paramContext, userId);
                }
                sDBHelpers.put(userId, sDBHelper);
                return sDBHelper;
            }
            return sDBHelpers.get(userId);
        }
    }
}