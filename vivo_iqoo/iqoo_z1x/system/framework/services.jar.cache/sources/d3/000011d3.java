package com.android.server.locksettings.recoverablekeystore.certificate;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.w3c.dom.Element;

/* loaded from: classes.dex */
public final class SigXml {
    private static final String INTERMEDIATE_CERT_ITEM_TAG = "cert";
    private static final String INTERMEDIATE_CERT_LIST_TAG = "intermediates";
    private static final String SIGNATURE_NODE_TAG = "value";
    private static final String SIGNER_CERT_NODE_TAG = "certificate";
    private final List<X509Certificate> intermediateCerts;
    private final byte[] signature;
    private final X509Certificate signerCert;

    private SigXml(List<X509Certificate> intermediateCerts, X509Certificate signerCert, byte[] signature) {
        this.intermediateCerts = intermediateCerts;
        this.signerCert = signerCert;
        this.signature = signature;
    }

    public void verifyFileSignature(X509Certificate trustedRoot, byte[] signedFileBytes) throws CertValidationException {
        verifyFileSignature(trustedRoot, signedFileBytes, null);
    }

    void verifyFileSignature(X509Certificate trustedRoot, byte[] signedFileBytes, Date validationDate) throws CertValidationException {
        CertUtils.validateCert(validationDate, trustedRoot, this.intermediateCerts, this.signerCert);
        CertUtils.verifyRsaSha256Signature(this.signerCert.getPublicKey(), this.signature, signedFileBytes);
    }

    public static SigXml parse(byte[] bytes) throws CertParsingException {
        Element rootNode = CertUtils.getXmlRootNode(bytes);
        return new SigXml(parseIntermediateCerts(rootNode), parseSignerCert(rootNode), parseFileSignature(rootNode));
    }

    private static List<X509Certificate> parseIntermediateCerts(Element rootNode) throws CertParsingException {
        List<String> contents = CertUtils.getXmlNodeContents(0, rootNode, INTERMEDIATE_CERT_LIST_TAG, INTERMEDIATE_CERT_ITEM_TAG);
        List<X509Certificate> res = new ArrayList<>();
        for (String content2 : contents) {
            res.add(CertUtils.decodeCert(CertUtils.decodeBase64(content2)));
        }
        return Collections.unmodifiableList(res);
    }

    private static X509Certificate parseSignerCert(Element rootNode) throws CertParsingException {
        List<String> contents = CertUtils.getXmlNodeContents(1, rootNode, SIGNER_CERT_NODE_TAG);
        return CertUtils.decodeCert(CertUtils.decodeBase64(contents.get(0)));
    }

    private static byte[] parseFileSignature(Element rootNode) throws CertParsingException {
        List<String> contents = CertUtils.getXmlNodeContents(1, rootNode, SIGNATURE_NODE_TAG);
        return CertUtils.decodeBase64(contents.get(0));
    }
}