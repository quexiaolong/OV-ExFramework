package com.android.server.backup;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/* loaded from: classes.dex */
public class UserBackupPreferences {
    private static final String PREFERENCES_FILE = "backup_preferences";
    private final SharedPreferences.Editor mEditor;
    private final SharedPreferences mPreferences;

    /* JADX INFO: Access modifiers changed from: package-private */
    public UserBackupPreferences(Context conext, File storageDir) {
        File excludedKeysFile = new File(storageDir, PREFERENCES_FILE);
        SharedPreferences sharedPreferences = conext.getSharedPreferences(excludedKeysFile, 0);
        this.mPreferences = sharedPreferences;
        this.mEditor = sharedPreferences.edit();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addExcludedKeys(String packageName, List<String> keys) {
        Set<String> existingKeys = new HashSet<>(this.mPreferences.getStringSet(packageName, Collections.emptySet()));
        existingKeys.addAll(keys);
        this.mEditor.putStringSet(packageName, existingKeys);
        this.mEditor.commit();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Set<String> getExcludedRestoreKeysForPackage(String packageName) {
        return this.mPreferences.getStringSet(packageName, Collections.emptySet());
    }
}