package com.android.server.locksettings.recoverablekeystore.certificate;

import java.security.SecureRandom;
import java.security.cert.CertPath;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.w3c.dom.Element;

/* loaded from: classes.dex */
public final class CertXml {
    private static final String ENDPOINT_CERT_ITEM_TAG = "cert";
    private static final String ENDPOINT_CERT_LIST_TAG = "endpoints";
    private static final String INTERMEDIATE_CERT_ITEM_TAG = "cert";
    private static final String INTERMEDIATE_CERT_LIST_TAG = "intermediates";
    private static final String METADATA_NODE_TAG = "metadata";
    private static final String METADATA_SERIAL_NODE_TAG = "serial";
    private final List<X509Certificate> endpointCerts;
    private final List<X509Certificate> intermediateCerts;
    private final long serial;

    private CertXml(long serial, List<X509Certificate> intermediateCerts, List<X509Certificate> endpointCerts) {
        this.serial = serial;
        this.intermediateCerts = intermediateCerts;
        this.endpointCerts = endpointCerts;
    }

    public long getSerial() {
        return this.serial;
    }

    List<X509Certificate> getAllIntermediateCerts() {
        return this.intermediateCerts;
    }

    List<X509Certificate> getAllEndpointCerts() {
        return this.endpointCerts;
    }

    public CertPath getRandomEndpointCert(X509Certificate trustedRoot) throws CertValidationException {
        return getEndpointCert(new SecureRandom().nextInt(this.endpointCerts.size()), null, trustedRoot);
    }

    CertPath getEndpointCert(int index, Date validationDate, X509Certificate trustedRoot) throws CertValidationException {
        X509Certificate chosenCert = this.endpointCerts.get(index);
        return CertUtils.validateCert(validationDate, trustedRoot, this.intermediateCerts, chosenCert);
    }

    public static CertXml parse(byte[] bytes) throws CertParsingException {
        Element rootNode = CertUtils.getXmlRootNode(bytes);
        return new CertXml(parseSerial(rootNode), parseIntermediateCerts(rootNode), parseEndpointCerts(rootNode));
    }

    private static long parseSerial(Element rootNode) throws CertParsingException {
        List<String> contents = CertUtils.getXmlNodeContents(1, rootNode, METADATA_NODE_TAG, METADATA_SERIAL_NODE_TAG);
        return Long.parseLong(contents.get(0));
    }

    private static List<X509Certificate> parseIntermediateCerts(Element rootNode) throws CertParsingException {
        List<String> contents = CertUtils.getXmlNodeContents(0, rootNode, INTERMEDIATE_CERT_LIST_TAG, "cert");
        List<X509Certificate> res = new ArrayList<>();
        for (String content2 : contents) {
            res.add(CertUtils.decodeCert(CertUtils.decodeBase64(content2)));
        }
        return Collections.unmodifiableList(res);
    }

    private static List<X509Certificate> parseEndpointCerts(Element rootNode) throws CertParsingException {
        List<String> contents = CertUtils.getXmlNodeContents(2, rootNode, ENDPOINT_CERT_LIST_TAG, "cert");
        List<X509Certificate> res = new ArrayList<>();
        for (String content2 : contents) {
            res.add(CertUtils.decodeCert(CertUtils.decodeBase64(content2)));
        }
        return Collections.unmodifiableList(res);
    }
}