package com.vivo.services.security.server.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/* loaded from: classes.dex */
public interface ICursorDataExtractor<T> {
    T extractData(SQLiteDatabase sQLiteDatabase, Cursor cursor);
}