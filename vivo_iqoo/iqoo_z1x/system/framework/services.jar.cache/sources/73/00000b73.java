package com.android.server.compat.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/* loaded from: classes.dex */
public class Config {
    private List<Change> compatChange;

    public List<Change> getCompatChange() {
        if (this.compatChange == null) {
            this.compatChange = new ArrayList();
        }
        return this.compatChange;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Config read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        Config instance = new Config();
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("compat-change")) {
                    Change value = Change.read(parser);
                    instance.getCompatChange().add(value);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("Config is not closed");
        }
        return instance;
    }
}