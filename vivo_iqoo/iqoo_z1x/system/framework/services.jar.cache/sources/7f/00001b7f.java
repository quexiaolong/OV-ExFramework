package com.android.server.voiceinteraction;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.hardware.soundtrigger.SoundTrigger;
import android.text.TextUtils;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/* loaded from: classes2.dex */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String CREATE_TABLE_SOUND_MODEL = "CREATE TABLE sound_model(model_uuid TEXT,vendor_uuid TEXT,keyphrase_id INTEGER,type INTEGER,data BLOB,recognition_modes INTEGER,locale TEXT,hint_text TEXT,users TEXT,model_version INTEGER,PRIMARY KEY (keyphrase_id,locale,users))";
    static final boolean DBG = false;
    private static final String NAME = "sound_model.db";
    static final String TAG = "SoundModelDBHelper";
    private static final int VERSION = 7;

    /* loaded from: classes2.dex */
    public interface SoundModelContract {
        public static final String KEY_DATA = "data";
        public static final String KEY_HINT_TEXT = "hint_text";
        public static final String KEY_KEYPHRASE_ID = "keyphrase_id";
        public static final String KEY_LOCALE = "locale";
        public static final String KEY_MODEL_UUID = "model_uuid";
        public static final String KEY_MODEL_VERSION = "model_version";
        public static final String KEY_RECOGNITION_MODES = "recognition_modes";
        public static final String KEY_TYPE = "type";
        public static final String KEY_USERS = "users";
        public static final String KEY_VENDOR_UUID = "vendor_uuid";
        public static final String TABLE = "sound_model";
    }

    public DatabaseHelper(Context context) {
        super(context, NAME, (SQLiteDatabase.CursorFactory) null, 7);
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_SOUND_MODEL);
    }

    /* JADX WARN: Removed duplicated region for block: B:21:0x005b  */
    @Override // android.database.sqlite.SQLiteOpenHelper
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public void onUpgrade(android.database.sqlite.SQLiteDatabase r12, int r13, int r14) {
        /*
            r11 = this;
            java.lang.String r0 = "DROP TABLE IF EXISTS sound_model"
            r1 = 4
            java.lang.String r2 = "SoundModelDBHelper"
            if (r13 >= r1) goto Le
            r12.execSQL(r0)
            r11.onCreate(r12)
            goto L1c
        Le:
            if (r13 != r1) goto L1c
            java.lang.String r1 = "Adding vendor UUID column"
            android.util.Slog.d(r2, r1)
            java.lang.String r1 = "ALTER TABLE sound_model ADD COLUMN vendor_uuid TEXT"
            r12.execSQL(r1)
            int r13 = r13 + 1
        L1c:
            r1 = 6
            r3 = 5
            if (r13 != r3) goto Lb0
            java.lang.String r4 = "SELECT * FROM sound_model"
            r5 = 0
            android.database.Cursor r5 = r12.rawQuery(r4, r5)
            java.util.ArrayList r6 = new java.util.ArrayList
            r6.<init>()
            boolean r7 = r5.moveToFirst()     // Catch: java.lang.Throwable -> Lab
            if (r7 == 0) goto L47
        L32:
            com.android.server.voiceinteraction.DatabaseHelper$SoundModelRecord r7 = new com.android.server.voiceinteraction.DatabaseHelper$SoundModelRecord     // Catch: java.lang.Exception -> L3b java.lang.Throwable -> Lab
            r7.<init>(r3, r5)     // Catch: java.lang.Exception -> L3b java.lang.Throwable -> Lab
            r6.add(r7)     // Catch: java.lang.Exception -> L3b java.lang.Throwable -> Lab
            goto L41
        L3b:
            r7 = move-exception
            java.lang.String r8 = "Failed to extract V5 record"
            android.util.Slog.e(r2, r8, r7)     // Catch: java.lang.Throwable -> Lab
        L41:
            boolean r7 = r5.moveToNext()     // Catch: java.lang.Throwable -> Lab
            if (r7 != 0) goto L32
        L47:
            r5.close()
            r12.execSQL(r0)
            r11.onCreate(r12)
            java.util.Iterator r0 = r6.iterator()
        L55:
            boolean r3 = r0.hasNext()
            if (r3 == 0) goto La8
            java.lang.Object r3 = r0.next()
            com.android.server.voiceinteraction.DatabaseHelper$SoundModelRecord r3 = (com.android.server.voiceinteraction.DatabaseHelper.SoundModelRecord) r3
            boolean r7 = r3.ifViolatesV6PrimaryKeyIsFirstOfAnyDuplicates(r6)
            if (r7 == 0) goto La7
            long r7 = r3.writeToDatabase(r1, r12)     // Catch: java.lang.Exception -> L90
            r9 = -1
            int r9 = (r7 > r9 ? 1 : (r7 == r9 ? 0 : -1))
            if (r9 != 0) goto L8f
            java.lang.StringBuilder r9 = new java.lang.StringBuilder     // Catch: java.lang.Exception -> L90
            r9.<init>()     // Catch: java.lang.Exception -> L90
            java.lang.String r10 = "Database write failed "
            r9.append(r10)     // Catch: java.lang.Exception -> L90
            java.lang.String r10 = r3.modelUuid     // Catch: java.lang.Exception -> L90
            r9.append(r10)     // Catch: java.lang.Exception -> L90
            java.lang.String r10 = ": "
            r9.append(r10)     // Catch: java.lang.Exception -> L90
            r9.append(r7)     // Catch: java.lang.Exception -> L90
            java.lang.String r9 = r9.toString()     // Catch: java.lang.Exception -> L90
            android.util.Slog.e(r2, r9)     // Catch: java.lang.Exception -> L90
        L8f:
            goto La7
        L90:
            r7 = move-exception
            java.lang.StringBuilder r8 = new java.lang.StringBuilder
            r8.<init>()
            java.lang.String r9 = "Failed to update V6 record "
            r8.append(r9)
            java.lang.String r9 = r3.modelUuid
            r8.append(r9)
            java.lang.String r8 = r8.toString()
            android.util.Slog.e(r2, r8, r7)
        La7:
            goto L55
        La8:
            int r13 = r13 + 1
            goto Lb0
        Lab:
            r0 = move-exception
            r5.close()
            throw r0
        Lb0:
            if (r13 != r1) goto Lbe
            java.lang.String r0 = "Adding model version column"
            android.util.Slog.d(r2, r0)
            java.lang.String r0 = "ALTER TABLE sound_model ADD COLUMN model_version INTEGER DEFAULT -1"
            r12.execSQL(r0)
            int r13 = r13 + 1
        Lbe:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.voiceinteraction.DatabaseHelper.onUpgrade(android.database.sqlite.SQLiteDatabase, int, int):void");
    }

    public boolean updateKeyphraseSoundModel(SoundTrigger.KeyphraseSoundModel soundModel) {
        synchronized (this) {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("model_uuid", soundModel.getUuid().toString());
            if (soundModel.getVendorUuid() != null) {
                values.put("vendor_uuid", soundModel.getVendorUuid().toString());
            }
            values.put(SoundModelContract.KEY_TYPE, (Integer) 0);
            values.put("data", soundModel.getData());
            values.put("model_version", Integer.valueOf(soundModel.getVersion()));
            if (soundModel.getKeyphrases() == null || soundModel.getKeyphrases().length != 1) {
                return false;
            }
            values.put(SoundModelContract.KEY_KEYPHRASE_ID, Integer.valueOf(soundModel.getKeyphrases()[0].getId()));
            values.put(SoundModelContract.KEY_RECOGNITION_MODES, Integer.valueOf(soundModel.getKeyphrases()[0].getRecognitionModes()));
            values.put(SoundModelContract.KEY_USERS, getCommaSeparatedString(soundModel.getKeyphrases()[0].getUsers()));
            values.put(SoundModelContract.KEY_LOCALE, soundModel.getKeyphrases()[0].getLocale().toLanguageTag());
            values.put(SoundModelContract.KEY_HINT_TEXT, soundModel.getKeyphrases()[0].getText());
            boolean z = db.insertWithOnConflict(SoundModelContract.TABLE, null, values, 5) != -1;
            db.close();
            return z;
        }
    }

    public boolean deleteKeyphraseSoundModel(int keyphraseId, int userHandle, String bcp47Locale) {
        String bcp47Locale2 = Locale.forLanguageTag(bcp47Locale).toLanguageTag();
        synchronized (this) {
            SoundTrigger.KeyphraseSoundModel soundModel = getKeyphraseSoundModel(keyphraseId, userHandle, bcp47Locale2);
            if (soundModel == null) {
                return false;
            }
            SQLiteDatabase db = getWritableDatabase();
            String soundModelClause = "model_uuid='" + soundModel.getUuid().toString() + "'";
            boolean z = db.delete(SoundModelContract.TABLE, soundModelClause, null) != 0;
            db.close();
            return z;
        }
    }

    public SoundTrigger.KeyphraseSoundModel getKeyphraseSoundModel(int keyphraseId, int userHandle, String bcp47Locale) {
        SoundTrigger.KeyphraseSoundModel validKeyphraseSoundModelForUser;
        String bcp47Locale2 = Locale.forLanguageTag(bcp47Locale).toLanguageTag();
        synchronized (this) {
            String selectQuery = "SELECT  * FROM sound_model WHERE keyphrase_id= '" + keyphraseId + "' AND " + SoundModelContract.KEY_LOCALE + "='" + bcp47Locale2 + "'";
            validKeyphraseSoundModelForUser = getValidKeyphraseSoundModelForUser(selectQuery, userHandle);
        }
        return validKeyphraseSoundModelForUser;
    }

    public SoundTrigger.KeyphraseSoundModel getKeyphraseSoundModel(String keyphrase, int userHandle, String bcp47Locale) {
        SoundTrigger.KeyphraseSoundModel validKeyphraseSoundModelForUser;
        String bcp47Locale2 = Locale.forLanguageTag(bcp47Locale).toLanguageTag();
        synchronized (this) {
            String selectQuery = "SELECT  * FROM sound_model WHERE hint_text= '" + keyphrase + "' AND " + SoundModelContract.KEY_LOCALE + "='" + bcp47Locale2 + "'";
            validKeyphraseSoundModelForUser = getValidKeyphraseSoundModelForUser(selectQuery, userHandle);
        }
        return validKeyphraseSoundModelForUser;
    }

    /* JADX WARN: Code restructure failed: missing block: B:33:0x00cc, code lost:
        r0 = new android.hardware.soundtrigger.SoundTrigger.Keyphrase[]{new android.hardware.soundtrigger.SoundTrigger.Keyphrase(r11, r12, r13, r14, r9)};
        r10 = null;
     */
    /* JADX WARN: Code restructure failed: missing block: B:34:0x00da, code lost:
        if (r7 == null) goto L44;
     */
    /* JADX WARN: Code restructure failed: missing block: B:35:0x00dc, code lost:
        r10 = java.util.UUID.fromString(r7);
     */
    /* JADX WARN: Code restructure failed: missing block: B:36:0x00e1, code lost:
        r15 = new android.hardware.soundtrigger.SoundTrigger.KeyphraseSoundModel(java.util.UUID.fromString(r5), r10, r19, r0, r21);
     */
    /* JADX WARN: Code restructure failed: missing block: B:37:0x00f1, code lost:
        r3.close();
        r1.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:38:0x00f7, code lost:
        return r15;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private android.hardware.soundtrigger.SoundTrigger.KeyphraseSoundModel getValidKeyphraseSoundModelForUser(java.lang.String r23, int r24) {
        /*
            Method dump skipped, instructions count: 269
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.voiceinteraction.DatabaseHelper.getValidKeyphraseSoundModelForUser(java.lang.String, int):android.hardware.soundtrigger.SoundTrigger$KeyphraseSoundModel");
    }

    private static String getCommaSeparatedString(int[] users) {
        if (users == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < users.length; i++) {
            if (i != 0) {
                sb.append(',');
            }
            sb.append(users[i]);
        }
        return sb.toString();
    }

    private static int[] getArrayForCommaSeparatedString(String text) {
        if (TextUtils.isEmpty(text)) {
            return null;
        }
        String[] usersStr = text.split(",");
        int[] users = new int[usersStr.length];
        for (int i = 0; i < usersStr.length; i++) {
            users[i] = Integer.parseInt(usersStr[i]);
        }
        return users;
    }

    /* loaded from: classes2.dex */
    private static class SoundModelRecord {
        public final byte[] data;
        public final String hintText;
        public final int keyphraseId;
        public final String locale;
        public final String modelUuid;
        public final int recognitionModes;
        public final int type;
        public final String users;
        public final String vendorUuid;

        public SoundModelRecord(int version, Cursor c) {
            this.modelUuid = c.getString(c.getColumnIndex("model_uuid"));
            if (version >= 5) {
                this.vendorUuid = c.getString(c.getColumnIndex("vendor_uuid"));
            } else {
                this.vendorUuid = null;
            }
            this.keyphraseId = c.getInt(c.getColumnIndex(SoundModelContract.KEY_KEYPHRASE_ID));
            this.type = c.getInt(c.getColumnIndex(SoundModelContract.KEY_TYPE));
            this.data = c.getBlob(c.getColumnIndex("data"));
            this.recognitionModes = c.getInt(c.getColumnIndex(SoundModelContract.KEY_RECOGNITION_MODES));
            this.locale = c.getString(c.getColumnIndex(SoundModelContract.KEY_LOCALE));
            this.hintText = c.getString(c.getColumnIndex(SoundModelContract.KEY_HINT_TEXT));
            this.users = c.getString(c.getColumnIndex(SoundModelContract.KEY_USERS));
        }

        private boolean V6PrimaryKeyMatches(SoundModelRecord record) {
            return this.keyphraseId == record.keyphraseId && stringComparisonHelper(this.locale, record.locale) && stringComparisonHelper(this.users, record.users);
        }

        public boolean ifViolatesV6PrimaryKeyIsFirstOfAnyDuplicates(List<SoundModelRecord> records) {
            for (SoundModelRecord record : records) {
                if (this != record && V6PrimaryKeyMatches(record) && !Arrays.equals(this.data, record.data)) {
                    return false;
                }
            }
            Iterator<SoundModelRecord> it = records.iterator();
            while (it.hasNext()) {
                SoundModelRecord record2 = it.next();
                if (V6PrimaryKeyMatches(record2)) {
                    return this == record2;
                }
            }
            return true;
        }

        public long writeToDatabase(int version, SQLiteDatabase db) {
            ContentValues values = new ContentValues();
            values.put("model_uuid", this.modelUuid);
            if (version >= 5) {
                values.put("vendor_uuid", this.vendorUuid);
            }
            values.put(SoundModelContract.KEY_KEYPHRASE_ID, Integer.valueOf(this.keyphraseId));
            values.put(SoundModelContract.KEY_TYPE, Integer.valueOf(this.type));
            values.put("data", this.data);
            values.put(SoundModelContract.KEY_RECOGNITION_MODES, Integer.valueOf(this.recognitionModes));
            values.put(SoundModelContract.KEY_LOCALE, this.locale);
            values.put(SoundModelContract.KEY_HINT_TEXT, this.hintText);
            values.put(SoundModelContract.KEY_USERS, this.users);
            return db.insertWithOnConflict(SoundModelContract.TABLE, null, values, 5);
        }

        private static boolean stringComparisonHelper(String a, String b) {
            if (a != null) {
                return a.equals(b);
            }
            return a == b;
        }
    }

    public void dump(PrintWriter pw) {
        synchronized (this) {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery("SELECT  * FROM sound_model", null);
            pw.println("  Enrolled KeyphraseSoundModels:");
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