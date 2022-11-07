package com.android.server.biometrics.face;

import android.content.Context;
import android.hardware.biometrics.BiometricAuthenticator;
import android.hardware.face.Face;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.Xml;
import com.android.server.biometrics.BiometricUserState;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

/* loaded from: classes.dex */
public class FaceUserState extends BiometricUserState {
    private static final String ATTR_DEVICE_ID = "deviceId";
    private static final String ATTR_FACE_ID = "faceId";
    private static final String ATTR_NAME = "name";
    private static final String FACE_FILE = "settings_face.xml";
    private static final String TAG = "FaceState";
    private static final String TAG_FACE = "face";
    private static final String TAG_FACES = "faces";

    public FaceUserState(Context ctx, int userId) {
        super(ctx, userId);
    }

    @Override // com.android.server.biometrics.BiometricUserState
    protected String getBiometricsTag() {
        return TAG_FACES;
    }

    @Override // com.android.server.biometrics.BiometricUserState
    protected String getBiometricFile() {
        return FACE_FILE;
    }

    @Override // com.android.server.biometrics.BiometricUserState
    protected int getNameTemplateResource() {
        return 17040248;
    }

    @Override // com.android.server.biometrics.BiometricUserState
    public void addBiometric(BiometricAuthenticator.Identifier identifier) {
        if (identifier instanceof Face) {
            super.addBiometric(identifier);
        } else {
            Slog.w(TAG, "Attempted to add non-face identifier");
        }
    }

    @Override // com.android.server.biometrics.BiometricUserState
    protected ArrayList getCopy(ArrayList array) {
        ArrayList<Face> result = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            Face f = (Face) array.get(i);
            result.add(new Face(f.getName(), f.getBiometricId(), f.getDeviceId()));
        }
        return result;
    }

    @Override // com.android.server.biometrics.BiometricUserState
    protected void doWriteState() {
        ArrayList<Face> faces;
        AtomicFile destination = new AtomicFile(this.mFile);
        synchronized (this) {
            faces = getCopy(this.mBiometrics);
        }
        FileOutputStream out = null;
        try {
            out = destination.startWrite();
            XmlSerializer serializer = Xml.newSerializer();
            serializer.setOutput(out, "utf-8");
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.startDocument(null, true);
            serializer.startTag(null, TAG_FACES);
            int count = faces.size();
            for (int i = 0; i < count; i++) {
                Face f = faces.get(i);
                serializer.startTag(null, TAG_FACE);
                serializer.attribute(null, ATTR_FACE_ID, Integer.toString(f.getBiometricId()));
                serializer.attribute(null, "name", f.getName().toString());
                serializer.attribute(null, ATTR_DEVICE_ID, Long.toString(f.getDeviceId()));
                serializer.endTag(null, TAG_FACE);
            }
            serializer.endTag(null, TAG_FACES);
            serializer.endDocument();
            destination.finishWrite(out);
        } finally {
        }
    }

    @Override // com.android.server.biometrics.BiometricUserState
    protected void parseBiometricsLocked(XmlPullParser parser) throws IOException, XmlPullParserException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type != 1) {
                if (type != 3 || parser.getDepth() > outerDepth) {
                    if (type != 3 && type != 4) {
                        String tagName = parser.getName();
                        if (tagName.equals(TAG_FACE)) {
                            String name = parser.getAttributeValue(null, "name");
                            String faceId = parser.getAttributeValue(null, ATTR_FACE_ID);
                            String deviceId = parser.getAttributeValue(null, ATTR_DEVICE_ID);
                            this.mBiometrics.add(new Face(name, Integer.parseInt(faceId), Long.parseLong(deviceId)));
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