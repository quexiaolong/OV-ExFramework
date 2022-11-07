package com.android.server.biometrics.fingerprint;

import android.content.Context;
import android.hardware.biometrics.BiometricAuthenticator;
import android.hardware.fingerprint.Fingerprint;
import android.text.TextUtils;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.Xml;
import com.android.server.biometrics.BiometricUserState;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class FingerprintUserState extends BiometricUserState {
    private static final String ATTR_DEVICE_ID = "deviceId";
    private static final String ATTR_FINGER_ID = "fingerId";
    private static final String ATTR_GROUP_ID = "groupId";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_TYPE = "type";
    private static final String FINGERPRINT_FILE = "settings_fingerprint.xml";
    private static final String TAG = "FingerprintState";
    private static final String TAG_FINGERPRINT = "fingerprint";
    private static final String TAG_FINGERPRINTS = "fingerprints";

    public FingerprintUserState(Context context, int userId) {
        super(context, userId);
    }

    @Override // com.android.server.biometrics.BiometricUserState
    protected String getBiometricsTag() {
        return TAG_FINGERPRINTS;
    }

    @Override // com.android.server.biometrics.BiometricUserState
    protected String getBiometricFile() {
        return FINGERPRINT_FILE;
    }

    @Override // com.android.server.biometrics.BiometricUserState
    protected int getNameTemplateResource() {
        return 17040286;
    }

    @Override // com.android.server.biometrics.BiometricUserState
    public void addBiometric(BiometricAuthenticator.Identifier identifier) {
        if (identifier instanceof Fingerprint) {
            super.addBiometric(identifier);
        } else {
            Slog.w(TAG, "Attempted to add non-fingerprint identifier");
        }
    }

    @Override // com.android.server.biometrics.BiometricUserState
    protected ArrayList getCopy(ArrayList array) {
        return getCopy(array, -1);
    }

    public ArrayList getCopy(ArrayList array, int type) {
        ArrayList<Fingerprint> result = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            Fingerprint fp = (Fingerprint) array.get(i);
            if (type != -1 && (fp.getType() & type) == 0) {
                VSlog.d(TAG, "skip " + fp.getType());
            } else {
                result.add(new Fingerprint(fp.getName(), fp.getGroupId(), fp.getBiometricId(), fp.getDeviceId(), fp.getType()));
            }
        }
        return result;
    }

    @Override // com.android.server.biometrics.BiometricUserState
    public List<Fingerprint> getBiometrics() {
        return getBiometrics(-1);
    }

    public List<Fingerprint> getBiometrics(int type) {
        ArrayList copy;
        synchronized (this) {
            copy = getCopy(this.mBiometrics, type);
        }
        return copy;
    }

    @Override // com.android.server.biometrics.BiometricUserState
    public String getUniqueName() {
        return getUniqueName(-1);
    }

    public String getUniqueName(int type) {
        String name;
        int guess = 1;
        while (true) {
            if (type == 2) {
                name = this.mContext.getString(51249723, Integer.valueOf(guess));
            } else {
                name = this.mContext.getString(51249798, Integer.valueOf(guess));
            }
            if (isUnique(name)) {
                return name;
            }
            guess++;
        }
    }

    @Override // com.android.server.biometrics.BiometricUserState
    protected void doWriteState() {
        ArrayList<Fingerprint> fingerprints;
        AtomicFile destination = new AtomicFile(this.mFile);
        synchronized (this) {
            fingerprints = getCopy(this.mBiometrics, -1);
        }
        FileOutputStream out = null;
        try {
            out = destination.startWrite();
            XmlSerializer serializer = Xml.newSerializer();
            serializer.setOutput(out, "utf-8");
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.startDocument(null, true);
            serializer.startTag(null, TAG_FINGERPRINTS);
            int count = fingerprints.size();
            for (int i = 0; i < count; i++) {
                Fingerprint fp = fingerprints.get(i);
                serializer.startTag(null, TAG_FINGERPRINT);
                serializer.attribute(null, ATTR_FINGER_ID, Integer.toString(fp.getBiometricId()));
                serializer.attribute(null, "name", fp.getName().toString());
                serializer.attribute(null, ATTR_GROUP_ID, Integer.toString(fp.getGroupId()));
                serializer.attribute(null, ATTR_DEVICE_ID, Long.toString(fp.getDeviceId()));
                serializer.attribute(null, "type", Long.toString(fp.getType()));
                serializer.endTag(null, TAG_FINGERPRINT);
            }
            serializer.endTag(null, TAG_FINGERPRINTS);
            serializer.endDocument();
            destination.finishWrite(out);
        } finally {
        }
    }

    @Override // com.android.server.biometrics.BiometricUserState
    protected void parseBiometricsLocked(XmlPullParser parser) throws IOException, XmlPullParserException {
        int outerDepth;
        int fingerTypeInt;
        XmlPullParser xmlPullParser = parser;
        int outerDepth2 = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type != 1) {
                if (type != 3 || parser.getDepth() > outerDepth2) {
                    if (type == 3) {
                        xmlPullParser = parser;
                    } else if (type != 4) {
                        String tagName = parser.getName();
                        if (!tagName.equals(TAG_FINGERPRINT)) {
                            outerDepth = outerDepth2;
                        } else {
                            String name = xmlPullParser.getAttributeValue(null, "name");
                            String groupId = xmlPullParser.getAttributeValue(null, ATTR_GROUP_ID);
                            String fingerId = xmlPullParser.getAttributeValue(null, ATTR_FINGER_ID);
                            String deviceId = xmlPullParser.getAttributeValue(null, ATTR_DEVICE_ID);
                            String fingerType = xmlPullParser.getAttributeValue(null, "type");
                            if (!TextUtils.isEmpty(fingerType)) {
                                try {
                                    fingerTypeInt = Integer.parseInt(fingerType);
                                } catch (NumberFormatException e) {
                                }
                                outerDepth = outerDepth2;
                                this.mBiometrics.add(new Fingerprint(name, Integer.parseInt(groupId), Integer.parseInt(fingerId), Long.parseLong(deviceId), fingerTypeInt));
                            }
                            fingerTypeInt = 1;
                            outerDepth = outerDepth2;
                            this.mBiometrics.add(new Fingerprint(name, Integer.parseInt(groupId), Integer.parseInt(fingerId), Long.parseLong(deviceId), fingerTypeInt));
                        }
                        xmlPullParser = parser;
                        outerDepth2 = outerDepth;
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