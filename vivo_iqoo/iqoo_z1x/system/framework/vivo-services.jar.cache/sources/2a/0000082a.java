package com.vivo.services.security.server.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public abstract class ASecurityDataBase<T> implements ICursorDataExtractor<T> {
    protected Context mContext;
    protected VivoSecurityDBHelper mDBHelper;

    public ASecurityDataBase(Context context, int userId) {
        if (context == null) {
            throw new NullPointerException("context should not be null");
        }
        this.mContext = context;
        this.mDBHelper = VivoSecurityDBHelper.getInstance(context, userId);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public static boolean isNull(Object object) {
        if (object == null) {
            return true;
        }
        if ((object instanceof List) && ((List) object).size() == 0) {
            return true;
        }
        return false;
    }

    protected List<T> find(SQLiteDatabase sd, String sql) {
        ArrayList<T> localArrayList = null;
        Cursor localCursor = sd.rawQuery(sql, null);
        if (localCursor != null) {
            if (localCursor.moveToFirst()) {
                ArrayList<T> localArrayList2 = new ArrayList<>();
                do {
                    T localObject = extractData(sd, localCursor);
                    localArrayList2.add(localObject);
                } while (localCursor.moveToNext());
                localArrayList = localArrayList2;
            }
            localCursor.deactivate();
            localCursor.close();
        }
        return localArrayList;
    }

    public List<T> find(String sql) {
        SQLiteDatabase localSQLiteDatabase = this.mDBHelper.getWritableDatabase();
        return find(localSQLiteDatabase, sql);
    }

    public List<T> find(String paramString, int paramInt1, int paramInt2) {
        int i = (paramInt1 - 1) * paramInt2;
        String str = "select * from (" + paramString + ") limit " + paramInt2 + " offset " + i;
        return find(str);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public T query(SQLiteDatabase paramSQLiteDatabase, String paramString) {
        T result = null;
        Cursor cursor = paramSQLiteDatabase.rawQuery(paramString, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                result = extractData(paramSQLiteDatabase, cursor);
            }
            cursor.deactivate();
            cursor.close();
        }
        return result;
    }

    public T query(String paramString) {
        SQLiteDatabase localSQLiteDatabase = this.mDBHelper.getWritableDatabase();
        return query(localSQLiteDatabase, paramString);
    }
}