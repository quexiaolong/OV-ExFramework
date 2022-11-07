package com.android.server.biometrics;

import android.content.Context;
import android.hardware.biometrics.BiometricAuthenticator;
import android.hardware.biometrics.BiometricAuthenticator.Identifier;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Slog;
import android.util.Xml;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public abstract class BiometricUserState<T extends BiometricAuthenticator.Identifier> {
    private static final String TAG = "UserState";
    protected final Context mContext;
    protected final File mFile;
    protected final ArrayList<T> mBiometrics = new ArrayList<>();
    private final Runnable mWriteStateRunnable = new Runnable() { // from class: com.android.server.biometrics.BiometricUserState.1
        @Override // java.lang.Runnable
        public void run() {
            BiometricUserState.this.doWriteState();
        }
    };

    protected abstract void doWriteState();

    protected abstract String getBiometricFile();

    protected abstract String getBiometricsTag();

    protected abstract ArrayList<T> getCopy(ArrayList<T> arrayList);

    protected abstract int getNameTemplateResource();

    protected abstract void parseBiometricsLocked(XmlPullParser xmlPullParser) throws IOException, XmlPullParserException;

    public BiometricUserState(Context context, int userId) {
        this.mFile = getFileForUser(userId);
        this.mContext = context;
        synchronized (this) {
            readStateSyncLocked();
        }
    }

    public void addBiometric(T identifier) {
        synchronized (this) {
            this.mBiometrics.add(identifier);
            scheduleWriteStateLocked();
        }
    }

    public void removeBiometric(int biometricId) {
        synchronized (this) {
            int i = 0;
            while (true) {
                if (i >= this.mBiometrics.size()) {
                    break;
                } else if (this.mBiometrics.get(i).getBiometricId() != biometricId) {
                    i++;
                } else {
                    this.mBiometrics.remove(i);
                    scheduleWriteStateLocked();
                    break;
                }
            }
        }
    }

    public void renameBiometric(int biometricId, CharSequence name) {
        synchronized (this) {
            int i = 0;
            while (true) {
                if (i >= this.mBiometrics.size()) {
                    break;
                } else if (this.mBiometrics.get(i).getBiometricId() != biometricId) {
                    i++;
                } else {
                    BiometricAuthenticator.Identifier identifier = this.mBiometrics.get(i);
                    identifier.setName(name);
                    scheduleWriteStateLocked();
                    break;
                }
            }
        }
    }

    public List<T> getBiometrics() {
        ArrayList<T> copy;
        synchronized (this) {
            copy = getCopy(this.mBiometrics);
        }
        return copy;
    }

    public String getUniqueName() {
        int guess = 1;
        while (true) {
            String name = this.mContext.getString(getNameTemplateResource(), Integer.valueOf(guess));
            if (isUnique(name)) {
                return name;
            }
            guess++;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean isUnique(String name) {
        Iterator<T> it = this.mBiometrics.iterator();
        while (it.hasNext()) {
            T identifier = it.next();
            if (identifier.getName().equals(name)) {
                return false;
            }
        }
        return true;
    }

    private File getFileForUser(int userId) {
        return new File(Environment.getUserSystemDirectory(userId), getBiometricFile());
    }

    private void scheduleWriteStateLocked() {
        AsyncTask.execute(this.mWriteStateRunnable);
    }

    private void readStateSyncLocked() {
        if (!this.mFile.exists()) {
            return;
        }
        try {
            FileInputStream in = new FileInputStream(this.mFile);
            try {
                try {
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setInput(in, null);
                    parseStateLocked(parser);
                } catch (IOException | XmlPullParserException e) {
                    VSlog.e(TAG, "Failed parsing settings file: " + this.mFile, e);
                }
            } finally {
                IoUtils.closeQuietly(in);
            }
        } catch (FileNotFoundException e2) {
            Slog.i(TAG, "No fingerprint state");
        }
    }

    private void parseStateLocked(XmlPullParser parser) throws IOException, XmlPullParserException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type != 1) {
                if (type != 3 || parser.getDepth() > outerDepth) {
                    if (type != 3 && type != 4) {
                        String tagName = parser.getName();
                        if (tagName.equals(getBiometricsTag())) {
                            parseBiometricsLocked(parser);
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }
}