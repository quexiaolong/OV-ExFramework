package com.android.server.display.config;

import java.io.IOException;
import java.math.BigDecimal;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/* loaded from: classes.dex */
public class Point {
    private BigDecimal nits;
    private BigDecimal value;

    public final BigDecimal getValue() {
        return this.value;
    }

    public final void setValue(BigDecimal value) {
        this.value = value;
    }

    public final BigDecimal getNits() {
        return this.nits;
    }

    public final void setNits(BigDecimal nits) {
        this.nits = nits;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Point read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        Point instance = new Point();
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("value")) {
                    String raw = XmlParser.readText(parser);
                    BigDecimal value = new BigDecimal(raw);
                    instance.setValue(value);
                } else if (tagName.equals("nits")) {
                    String raw2 = XmlParser.readText(parser);
                    BigDecimal value2 = new BigDecimal(raw2);
                    instance.setNits(value2);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("Point is not closed");
        }
        return instance;
    }
}