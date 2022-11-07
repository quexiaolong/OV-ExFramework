package com.android.server.compat.config;

import java.io.IOException;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/* loaded from: classes.dex */
public class Change {
    private String description;
    private boolean disabled;
    private int enableAfterTargetSdk;
    private long id;
    private boolean loggingOnly;
    private String name;
    private String value;

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean getDisabled() {
        return this.disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public boolean getLoggingOnly() {
        return this.loggingOnly;
    }

    public void setLoggingOnly(boolean loggingOnly) {
        this.loggingOnly = loggingOnly;
    }

    public int getEnableAfterTargetSdk() {
        return this.enableAfterTargetSdk;
    }

    public void setEnableAfterTargetSdk(int enableAfterTargetSdk) {
        this.enableAfterTargetSdk = enableAfterTargetSdk;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Change read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        Change instance = new Change();
        String raw = parser.getAttributeValue(null, "id");
        if (raw != null) {
            long value = Long.parseLong(raw);
            instance.setId(value);
        }
        String raw2 = parser.getAttributeValue(null, "name");
        if (raw2 != null) {
            instance.setName(raw2);
        }
        String raw3 = parser.getAttributeValue(null, "disabled");
        if (raw3 != null) {
            boolean value2 = Boolean.parseBoolean(raw3);
            instance.setDisabled(value2);
        }
        String raw4 = parser.getAttributeValue(null, "loggingOnly");
        if (raw4 != null) {
            boolean value3 = Boolean.parseBoolean(raw4);
            instance.setLoggingOnly(value3);
        }
        String raw5 = parser.getAttributeValue(null, "enableAfterTargetSdk");
        if (raw5 != null) {
            int value4 = Integer.parseInt(raw5);
            instance.setEnableAfterTargetSdk(value4);
        }
        String raw6 = parser.getAttributeValue(null, "description");
        if (raw6 != null) {
            instance.setDescription(raw6);
        }
        String raw7 = XmlParser.readText(parser);
        if (raw7 != null) {
            instance.setValue(raw7);
        }
        return instance;
    }
}