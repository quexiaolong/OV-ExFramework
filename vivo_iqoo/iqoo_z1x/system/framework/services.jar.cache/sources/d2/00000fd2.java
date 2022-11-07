package com.android.server.integrity.serializer;

import android.util.Xml;
import com.android.server.integrity.model.RuleMetadata;
import com.android.server.integrity.parser.RuleMetadataParser;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.xmlpull.v1.XmlSerializer;

/* loaded from: classes.dex */
public class RuleMetadataSerializer {
    public static void serialize(RuleMetadata ruleMetadata, OutputStream outputStream) throws IOException {
        XmlSerializer xmlSerializer = Xml.newSerializer();
        xmlSerializer.setOutput(outputStream, StandardCharsets.UTF_8.name());
        serializeTaggedValue(xmlSerializer, RuleMetadataParser.RULE_PROVIDER_TAG, ruleMetadata.getRuleProvider());
        serializeTaggedValue(xmlSerializer, RuleMetadataParser.VERSION_TAG, ruleMetadata.getVersion());
        xmlSerializer.endDocument();
    }

    private static void serializeTaggedValue(XmlSerializer xmlSerializer, String tag, String value) throws IOException {
        xmlSerializer.startTag(null, tag);
        xmlSerializer.text(value);
        xmlSerializer.endTag(null, tag);
    }
}