package com.android.server.inputmethod;

import android.os.Environment;
import android.util.AtomicFile;
import java.io.File;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class AdditionalSubtypeUtils {
    private static final String ADDITIONAL_SUBTYPES_FILE_NAME = "subtypes.xml";
    private static final String ATTR_ICON = "icon";
    private static final String ATTR_ID = "id";
    private static final String ATTR_IME_SUBTYPE_EXTRA_VALUE = "imeSubtypeExtraValue";
    private static final String ATTR_IME_SUBTYPE_ID = "subtypeId";
    private static final String ATTR_IME_SUBTYPE_LANGUAGE_TAG = "languageTag";
    private static final String ATTR_IME_SUBTYPE_LOCALE = "imeSubtypeLocale";
    private static final String ATTR_IME_SUBTYPE_MODE = "imeSubtypeMode";
    private static final String ATTR_IS_ASCII_CAPABLE = "isAsciiCapable";
    private static final String ATTR_IS_AUXILIARY = "isAuxiliary";
    private static final String ATTR_LABEL = "label";
    private static final String INPUT_METHOD_PATH = "inputmethod";
    private static final String NODE_IMI = "imi";
    private static final String NODE_SUBTYPE = "subtype";
    private static final String NODE_SUBTYPES = "subtypes";
    private static final String SYSTEM_PATH = "system";
    private static final String TAG = "AdditionalSubtypeUtils";

    private AdditionalSubtypeUtils() {
    }

    private static File getInputMethodDir(int userId) {
        File systemDir;
        if (userId == 0) {
            systemDir = new File(Environment.getDataDirectory(), SYSTEM_PATH);
        } else {
            systemDir = Environment.getUserSystemDirectory(userId);
        }
        return new File(systemDir, INPUT_METHOD_PATH);
    }

    private static AtomicFile getAdditionalSubtypeFile(File inputMethodDir) {
        File subtypeFile = new File(inputMethodDir, ADDITIONAL_SUBTYPES_FILE_NAME);
        return new AtomicFile(subtypeFile, "input-subtypes");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:67:0x01bb  */
    /* JADX WARN: Removed duplicated region for block: B:87:? A[RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public static void save(android.util.ArrayMap<java.lang.String, java.util.List<android.view.inputmethod.InputMethodSubtype>> r20, android.util.ArrayMap<java.lang.String, android.view.inputmethod.InputMethodInfo> r21, int r22) {
        /*
            Method dump skipped, instructions count: 447
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.inputmethod.AdditionalSubtypeUtils.save(android.util.ArrayMap, android.util.ArrayMap, int):void");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:70:0x01e7 A[Catch: all -> 0x01eb, TRY_ENTER, TRY_LEAVE, TryCatch #1 {IOException | NumberFormatException | XmlPullParserException -> 0x01f1, blocks: (B:59:0x01ca, B:75:0x01f0, B:70:0x01e7), top: B:82:0x0016 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public static void load(android.util.ArrayMap<java.lang.String, java.util.List<android.view.inputmethod.InputMethodSubtype>> r23, int r24) {
        /*
            Method dump skipped, instructions count: 510
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.inputmethod.AdditionalSubtypeUtils.load(android.util.ArrayMap, int):void");
    }
}