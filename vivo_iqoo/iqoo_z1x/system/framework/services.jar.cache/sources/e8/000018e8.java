package com.android.server.soundtrigger;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.hardware.soundtrigger.SoundTrigger;
import android.util.Slog;
import java.io.PrintWriter;
import java.util.UUID;

/* loaded from: classes2.dex */
public class SoundTriggerDbHelper extends SQLiteOpenHelper {
    private static final String CREATE_TABLE_ST_SOUND_MODEL = "CREATE TABLE st_sound_model(model_uuid TEXT PRIMARY KEY,vendor_uuid TEXT,data BLOB,model_version INTEGER )";
    static final boolean DBG = false;
    private static final String NAME = "st_sound_model.db";
    static final String TAG = "SoundTriggerDbHelper";
    private static final int VERSION = 2;

    /* loaded from: classes2.dex */
    public interface GenericSoundModelContract {
        public static final String KEY_DATA = "data";
        public static final String KEY_MODEL_UUID = "model_uuid";
        public static final String KEY_MODEL_VERSION = "model_version";
        public static final String KEY_VENDOR_UUID = "vendor_uuid";
        public static final String TABLE = "st_sound_model";
    }

    public SoundTriggerDbHelper(Context context) {
        super(context, NAME, (SQLiteDatabase.CursorFactory) null, 2);
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_ST_SOUND_MODEL);
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1) {
            Slog.d(TAG, "Adding model version column");
            db.execSQL("ALTER TABLE st_sound_model ADD COLUMN model_version INTEGER DEFAULT -1");
            int i = oldVersion + 1;
        }
    }

    public boolean updateGenericSoundModel(SoundTrigger.GenericSoundModel soundModel) {
        boolean z;
        synchronized (this) {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("model_uuid", soundModel.getUuid().toString());
            values.put("vendor_uuid", soundModel.getVendorUuid().toString());
            values.put("data", soundModel.getData());
            values.put("model_version", Integer.valueOf(soundModel.getVersion()));
            z = db.insertWithOnConflict(GenericSoundModelContract.TABLE, null, values, 5) != -1;
            db.close();
        }
        return z;
    }

    public SoundTrigger.GenericSoundModel getGenericSoundModel(UUID model_uuid) {
        synchronized (this) {
            String selectQuery = "SELECT  * FROM st_sound_model WHERE model_uuid= '" + model_uuid + "'";
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery(selectQuery, null);
            if (c.moveToFirst()) {
                byte[] data = c.getBlob(c.getColumnIndex("data"));
                String vendor_uuid = c.getString(c.getColumnIndex("vendor_uuid"));
                int version = c.getInt(c.getColumnIndex("model_version"));
                SoundTrigger.GenericSoundModel genericSoundModel = new SoundTrigger.GenericSoundModel(model_uuid, UUID.fromString(vendor_uuid), data, version);
                c.close();
                db.close();
                return genericSoundModel;
            }
            c.close();
            db.close();
            return null;
        }
    }

    public boolean deleteGenericSoundModel(UUID model_uuid) {
        synchronized (this) {
            SoundTrigger.GenericSoundModel soundModel = getGenericSoundModel(model_uuid);
            if (soundModel == null) {
                return false;
            }
            SQLiteDatabase db = getWritableDatabase();
            String soundModelClause = "model_uuid='" + soundModel.getUuid().toString() + "'";
            boolean z = db.delete(GenericSoundModelContract.TABLE, soundModelClause, null) != 0;
            db.close();
            return z;
        }
    }

    public void dump(PrintWriter pw) {
        synchronized (this) {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery("SELECT  * FROM st_sound_model", null);
            pw.println("  Enrolled GenericSoundModels:");
            if (c.moveToFirst()) {
                String[] columnNames = c.getColumnNames();
                do {
                    for (String name : columnNames) {
                        int colNameIndex = c.getColumnIndex(name);
                        int type = c.getType(colNameIndex);
                        if (type != 0) {
                            if (type == 1) {
                                pw.printf("    %s: %d\n", name, Integer.valueOf(c.getInt(colNameIndex)));
                            } else if (type == 2) {
                                pw.printf("    %s: %f\n", name, Float.valueOf(c.getFloat(colNameIndex)));
                            } else if (type == 3) {
                                pw.printf("    %s: %s\n", name, c.getString(colNameIndex));
                            } else if (type == 4) {
                                pw.printf("    %s: data blob\n", name);
                            }
                        } else {
                            pw.printf("    %s: null\n", name);
                        }
                    }
                    pw.println();
                } while (c.moveToNext());
                c.close();
                db.close();
            } else {
                c.close();
                db.close();
            }
        }
    }
}