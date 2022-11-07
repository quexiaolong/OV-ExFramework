package com.vivo.services.security.server.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.vivo.services.security.client.VivoPermissionInfo;
import com.vivo.services.security.client.VivoPermissionType;
import com.vivo.services.security.server.VivoPermissionService;
import java.util.List;

/* loaded from: classes.dex */
public class VivoPermissionDataBase extends ASecurityDataBase<VivoPermissionInfo> {
    public static final int GET_DENIED = 2;
    public static final int GET_GRANTED = 4;
    public static final int GET_MASK = 15;
    public static final int GET_UNKNOWN = 1;
    public static final int GET_WARNING = 8;

    public VivoPermissionDataBase(Context context, int userId) {
        super(context, userId);
    }

    @Override // com.vivo.services.security.server.db.ICursorDataExtractor
    public VivoPermissionInfo extractData(SQLiteDatabase sqlDatabase, Cursor cursor) {
        int columnIndex = cursor.getColumnIndex(VivoSecurityDBHelper.ATTR_PACKAGE);
        String packageName = cursor.getString(columnIndex);
        VivoPermissionInfo vpi = new VivoPermissionInfo(packageName);
        int columnIndex2 = cursor.getColumnIndex(VivoSecurityDBHelper.ATTR_IS_WHITELIST);
        int value = cursor.getInt(columnIndex2);
        vpi.setWhiteListApp(value == 1);
        int columnIndex3 = cursor.getColumnIndex(VivoSecurityDBHelper.ATTR_IS_BLACKLIST);
        int value2 = cursor.getInt(columnIndex3);
        vpi.setBlackListApp(value2 == 1);
        for (int index = 0; index < 32; index++) {
            int columnIndex4 = cursor.getColumnIndex(VivoPermissionType.getVPType(index).toString());
            int value3 = cursor.getInt(columnIndex4);
            vpi.setAllPermission(index, value3);
        }
        for (int ftimeIndex = 0; ftimeIndex < 32; ftimeIndex++) {
            int columnIndex5 = cursor.getColumnIndex("FTime" + VivoPermissionType.getVPType(ftimeIndex).toString());
            int value4 = cursor.getInt(columnIndex5);
            vpi.setAllPermissionBackup(ftimeIndex, value4);
        }
        int columnIndex6 = cursor.getColumnIndex(VivoSecurityDBHelper.ATTR_IS_CONFIGURED);
        int value5 = cursor.getInt(columnIndex6);
        vpi.setConfigured(value5 == 1);
        return vpi;
    }

    public void batchSave(List<VivoPermissionInfo> paramList) {
        if (isNull(paramList)) {
            return;
        }
        SQLiteDatabase localSQLiteDatabase = this.mDBHelper.getWritableDatabase();
        localSQLiteDatabase.beginTransaction();
        try {
            for (VivoPermissionInfo localUser : paramList) {
                save(localSQLiteDatabase, localUser);
            }
            localSQLiteDatabase.setTransactionSuccessful();
        } finally {
            localSQLiteDatabase.endTransaction();
        }
    }

    public int delete(String pkg) {
        if (pkg == null || pkg.length() == 0) {
            return -1;
        }
        StringBuffer deleteSql = new StringBuffer();
        deleteSql.append(VivoSecurityDBHelper.ATTR_PACKAGE);
        deleteSql.append(" = '");
        deleteSql.append(pkg);
        deleteSql.append("'");
        SQLiteDatabase localSQLiteDatabase = this.mDBHelper.getWritableDatabase();
        int result = localSQLiteDatabase.delete(VivoSecurityDBHelper.TABLE_PERMISSION, deleteSql.toString(), null);
        return result;
    }

    public int delete(VivoPermissionInfo vpi) {
        if (vpi == null) {
            return -1;
        }
        return delete(vpi.getPackageName());
    }

    protected VivoPermissionInfo findById(SQLiteDatabase sd, String pkg) {
        StringBuffer findSql = new StringBuffer();
        findSql.append("select * from ");
        findSql.append(VivoSecurityDBHelper.TABLE_PERMISSION);
        findSql.append(" where ");
        findSql.append(VivoSecurityDBHelper.ATTR_PACKAGE);
        findSql.append(" = '");
        findSql.append(pkg);
        findSql.append("'");
        return query(sd, findSql.toString());
    }

    public VivoPermissionInfo findById(String pkg) {
        if (pkg == null || pkg.length() == 0) {
            return null;
        }
        SQLiteDatabase localSQLiteDatabase = this.mDBHelper.getWritableDatabase();
        return findById(localSQLiteDatabase, pkg);
    }

    public List<VivoPermissionInfo> findVPIsByWhiteList(boolean inWhiteList) {
        StringBuffer findSql = new StringBuffer();
        findSql.append("select * from ");
        findSql.append(VivoSecurityDBHelper.TABLE_PERMISSION);
        findSql.append(" where ");
        findSql.append(VivoSecurityDBHelper.ATTR_IS_WHITELIST);
        findSql.append(" = ");
        findSql.append(inWhiteList ? 1 : 0);
        return find(findSql.toString());
    }

    public List<VivoPermissionInfo> findVPIsByBlackList(boolean inBlackList) {
        StringBuffer findSql = new StringBuffer();
        findSql.append("select * from ");
        findSql.append(VivoSecurityDBHelper.TABLE_PERMISSION);
        findSql.append(" where ");
        findSql.append(VivoSecurityDBHelper.ATTR_IS_BLACKLIST);
        findSql.append(" = ");
        findSql.append(inBlackList ? 1 : 0);
        return find(findSql.toString());
    }

    public List<VivoPermissionInfo> findAllVPIs() {
        StringBuffer findSql = new StringBuffer();
        findSql.append("select * from ");
        findSql.append(VivoSecurityDBHelper.TABLE_PERMISSION);
        String sql = findSql.toString();
        VivoPermissionService.printfInfo("findAllVPIs:sql=" + sql);
        return find(sql);
    }

    protected void save(SQLiteDatabase paramSQLiteDatabase, VivoPermissionInfo vpi) {
        if (vpi == null) {
            return;
        }
        ContentValues localContentValues = new ContentValues();
        localContentValues.put(VivoSecurityDBHelper.ATTR_PACKAGE, vpi.getPackageName());
        localContentValues.put(VivoSecurityDBHelper.ATTR_IS_WHITELIST, Boolean.valueOf(vpi.isWhiteListApp()));
        localContentValues.put(VivoSecurityDBHelper.ATTR_IS_BLACKLIST, Boolean.valueOf(vpi.isBlackListApp()));
        for (int index = 0; index < 32; index++) {
            localContentValues.put(VivoPermissionType.getVPType(index).toString(), Integer.valueOf(vpi.getAllPermission(index)));
        }
        for (int ftimeIndex = 0; ftimeIndex < 32; ftimeIndex++) {
            localContentValues.put("FTime" + VivoPermissionType.getVPType(ftimeIndex).toString(), Integer.valueOf(vpi.getAllPermissionBackup(ftimeIndex)));
        }
        localContentValues.put(VivoSecurityDBHelper.ATTR_IS_CONFIGURED, Boolean.valueOf(vpi.isConfigured()));
        paramSQLiteDatabase.replace(VivoSecurityDBHelper.TABLE_PERMISSION, null, localContentValues);
    }

    public void save(VivoPermissionInfo vpi) {
        if (vpi == null) {
            return;
        }
        SQLiteDatabase localSQLiteDatabase = this.mDBHelper.getWritableDatabase();
        save(localSQLiteDatabase, vpi);
    }

    public void save(String packageName, int permTypeId, int result) {
        if (packageName == null || packageName.length() == 0) {
            return;
        }
        SQLiteDatabase localSQLiteDatabase = this.mDBHelper.getWritableDatabase();
        ContentValues localContentValues = new ContentValues();
        localContentValues.put(VivoSecurityDBHelper.ATTR_PACKAGE, packageName);
        localContentValues.put(VivoPermissionType.getVPType(permTypeId).toString(), Integer.valueOf(result));
        localSQLiteDatabase.replace(VivoSecurityDBHelper.TABLE_PERMISSION, null, localContentValues);
    }

    public void save(String packageName, boolean isWhiteList, boolean isBlackList) {
        if (packageName == null || packageName.length() == 0) {
            return;
        }
        SQLiteDatabase localSQLiteDatabase = this.mDBHelper.getWritableDatabase();
        ContentValues localContentValues = new ContentValues();
        localContentValues.put(VivoSecurityDBHelper.ATTR_PACKAGE, packageName);
        localContentValues.put(VivoSecurityDBHelper.ATTR_IS_WHITELIST, Boolean.valueOf(isWhiteList));
        localContentValues.put(VivoSecurityDBHelper.ATTR_IS_BLACKLIST, Boolean.valueOf(isBlackList));
        localSQLiteDatabase.replace(VivoSecurityDBHelper.TABLE_PERMISSION, null, localContentValues);
    }

    public void removeDb() {
        this.mDBHelper.removeDb(this.mDBHelper.getWritableDatabase());
    }
}