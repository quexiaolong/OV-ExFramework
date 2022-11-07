package com.android.server.pm;

import android.content.pm.PackageParser;
import android.content.pm.Signature;
import com.android.internal.util.XmlUtils;
import com.android.server.am.AssistDataRequester;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class PackageSignatures {
    PackageParser.SigningDetails mSigningDetails;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageSignatures(PackageSignatures orig) {
        if (orig != null && orig.mSigningDetails != PackageParser.SigningDetails.UNKNOWN) {
            this.mSigningDetails = new PackageParser.SigningDetails(orig.mSigningDetails);
        } else {
            this.mSigningDetails = PackageParser.SigningDetails.UNKNOWN;
        }
    }

    PackageSignatures(PackageParser.SigningDetails signingDetails) {
        this.mSigningDetails = signingDetails;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageSignatures() {
        this.mSigningDetails = PackageParser.SigningDetails.UNKNOWN;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writeXml(XmlSerializer serializer, String tagName, ArrayList<Signature> writtenSignatures) throws IOException {
        if (this.mSigningDetails.signatures == null) {
            return;
        }
        serializer.startTag(null, tagName);
        serializer.attribute(null, AssistDataRequester.KEY_RECEIVER_EXTRA_COUNT, Integer.toString(this.mSigningDetails.signatures.length));
        serializer.attribute(null, "schemeVersion", Integer.toString(this.mSigningDetails.signatureSchemeVersion));
        writeCertsListXml(serializer, writtenSignatures, this.mSigningDetails.signatures, false);
        if (this.mSigningDetails.pastSigningCertificates != null) {
            serializer.startTag(null, "pastSigs");
            serializer.attribute(null, AssistDataRequester.KEY_RECEIVER_EXTRA_COUNT, Integer.toString(this.mSigningDetails.pastSigningCertificates.length));
            writeCertsListXml(serializer, writtenSignatures, this.mSigningDetails.pastSigningCertificates, true);
            serializer.endTag(null, "pastSigs");
        }
        serializer.endTag(null, tagName);
    }

    private void writeCertsListXml(XmlSerializer serializer, ArrayList<Signature> writtenSignatures, Signature[] signatures, boolean isPastSigs) throws IOException {
        for (Signature sig : signatures) {
            serializer.startTag(null, "cert");
            int sigHash = sig.hashCode();
            int numWritten = writtenSignatures.size();
            int j = 0;
            while (true) {
                if (j >= numWritten) {
                    break;
                }
                Signature writtenSig = writtenSignatures.get(j);
                if (writtenSig.hashCode() == sigHash && writtenSig.equals(sig)) {
                    serializer.attribute(null, AssistDataRequester.KEY_RECEIVER_EXTRA_INDEX, Integer.toString(j));
                    break;
                }
                j++;
            }
            if (j >= numWritten) {
                writtenSignatures.add(sig);
                serializer.attribute(null, AssistDataRequester.KEY_RECEIVER_EXTRA_INDEX, Integer.toString(numWritten));
                serializer.attribute(null, "key", sig.toCharsString());
            }
            if (isPastSigs) {
                serializer.attribute(null, "flags", Integer.toString(sig.getFlags()));
            }
            serializer.endTag(null, "cert");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void readXml(XmlPullParser parser, ArrayList<Signature> readSignatures) throws IOException, XmlPullParserException {
        int signatureSchemeVersion;
        int size;
        PackageParser.SigningDetails.Builder builder = new PackageParser.SigningDetails.Builder();
        String countStr = parser.getAttributeValue(null, AssistDataRequester.KEY_RECEIVER_EXTRA_COUNT);
        if (countStr == null) {
            PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <sigs> has no count at " + parser.getPositionDescription());
            XmlUtils.skipCurrentTag(parser);
            return;
        }
        int count = Integer.parseInt(countStr);
        String schemeVersionStr = parser.getAttributeValue(null, "schemeVersion");
        if (schemeVersionStr == null) {
            PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <sigs> has no schemeVersion at " + parser.getPositionDescription());
            signatureSchemeVersion = 0;
        } else {
            int signatureSchemeVersion2 = Integer.parseInt(schemeVersionStr);
            signatureSchemeVersion = signatureSchemeVersion2;
        }
        builder.setSignatureSchemeVersion(signatureSchemeVersion);
        ArrayList<Signature> signatureList = new ArrayList<>();
        int pos = readCertsListXml(parser, readSignatures, signatureList, count, false, builder);
        if (signatureList.size() <= 0) {
            size = count;
        } else {
            int size2 = signatureList.size();
            size = size2;
        }
        Signature[] signatures = (Signature[]) signatureList.toArray(new Signature[size]);
        builder.setSignatures(signatures);
        if (pos < count) {
            PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <sigs> count does not match number of  <cert> entries" + parser.getPositionDescription());
        }
        try {
            this.mSigningDetails = builder.build();
        } catch (CertificateException e) {
            PackageManagerService.reportSettingsProblem(5, "Error in package manager settings: <sigs> unable to convert certificate(s) to public key(s).");
            this.mSigningDetails = PackageParser.SigningDetails.UNKNOWN;
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:47:0x014c  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private int readCertsListXml(org.xmlpull.v1.XmlPullParser r25, java.util.ArrayList<android.content.pm.Signature> r26, java.util.ArrayList<android.content.pm.Signature> r27, int r28, boolean r29, android.content.pm.PackageParser.SigningDetails.Builder r30) throws java.io.IOException, org.xmlpull.v1.XmlPullParserException {
        /*
            Method dump skipped, instructions count: 813
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.PackageSignatures.readCertsListXml(org.xmlpull.v1.XmlPullParser, java.util.ArrayList, java.util.ArrayList, int, boolean, android.content.pm.PackageParser$SigningDetails$Builder):int");
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(128);
        buf.append("PackageSignatures{");
        buf.append(Integer.toHexString(System.identityHashCode(this)));
        buf.append(" version:");
        buf.append(this.mSigningDetails.signatureSchemeVersion);
        buf.append(", signatures:[");
        if (this.mSigningDetails.signatures != null) {
            for (int i = 0; i < this.mSigningDetails.signatures.length; i++) {
                if (i > 0) {
                    buf.append(", ");
                }
                buf.append(Integer.toHexString(this.mSigningDetails.signatures[i].hashCode()));
            }
        }
        buf.append("]");
        buf.append(", past signatures:[");
        if (this.mSigningDetails.pastSigningCertificates != null) {
            for (int i2 = 0; i2 < this.mSigningDetails.pastSigningCertificates.length; i2++) {
                if (i2 > 0) {
                    buf.append(", ");
                }
                buf.append(Integer.toHexString(this.mSigningDetails.pastSigningCertificates[i2].hashCode()));
                buf.append(" flags: ");
                buf.append(Integer.toHexString(this.mSigningDetails.pastSigningCertificates[i2].getFlags()));
            }
        }
        buf.append("]}");
        return buf.toString();
    }
}