package com.android.server.integrity.parser;

import android.util.Xml;
import com.android.server.integrity.model.RuleMetadata;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/* loaded from: classes.dex */
public class RuleMetadataParser {
    public static final String RULE_PROVIDER_TAG = "P";
    public static final String VERSION_TAG = "V";

    public static RuleMetadata parse(InputStream inputStream) throws XmlPullParserException, IOException {
        String ruleProvider = "";
        String version = "";
        XmlPullParser xmlPullParser = Xml.newPullParser();
        xmlPullParser.setInput(inputStream, StandardCharsets.UTF_8.name());
        while (true) {
            int eventType = xmlPullParser.next();
            if (eventType != 1) {
                if (eventType == 2) {
                    String tag = xmlPullParser.getName();
                    char c = 65535;
                    int hashCode = tag.hashCode();
                    if (hashCode != 80) {
                        if (hashCode == 86 && tag.equals(VERSION_TAG)) {
                            c = 1;
                        }
                    } else if (tag.equals(RULE_PROVIDER_TAG)) {
                        c = 0;
                    }
                    if (c == 0) {
                        ruleProvider = xmlPullParser.nextText();
                    } else if (c == 1) {
                        version = xmlPullParser.nextText();
                    } else {
                        throw new IllegalStateException("Unknown tag in metadata: " + tag);
                    }
                }
            } else {
                return new RuleMetadata(ruleProvider, version);
            }
        }
    }
}