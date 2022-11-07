package com.android.server.input;

import android.text.TextUtils;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.XmlUtils;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class ConfigurationProcessor {
    private static final String TAG = "ConfigurationProcessor";

    ConfigurationProcessor() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static List<String> processExcludedDeviceNames(InputStream xml) throws Exception {
        List<String> names = new ArrayList<>();
        InputStreamReader confReader = new InputStreamReader(xml);
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(confReader);
            XmlUtils.beginDocument(parser, "devices");
            while (true) {
                XmlUtils.nextElement(parser);
                if ("device".equals(parser.getName())) {
                    String name = parser.getAttributeValue(null, "name");
                    if (name != null) {
                        names.add(name);
                    }
                } else {
                    confReader.close();
                    return names;
                }
            }
        } catch (Throwable th) {
            try {
                confReader.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Map<String, Integer> processInputPortAssociations(InputStream xml) throws Exception {
        Map<String, Integer> associations = new HashMap<>();
        InputStreamReader confReader = new InputStreamReader(xml);
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(confReader);
            XmlUtils.beginDocument(parser, "ports");
            while (true) {
                XmlUtils.nextElement(parser);
                String entryName = parser.getName();
                if ("port".equals(entryName)) {
                    String inputPort = parser.getAttributeValue(null, "input");
                    String displayPortStr = parser.getAttributeValue(null, "display");
                    if (TextUtils.isEmpty(inputPort) || TextUtils.isEmpty(displayPortStr)) {
                        Slog.wtf(TAG, "Ignoring incomplete entry");
                    } else {
                        try {
                            int displayPort = Integer.parseUnsignedInt(displayPortStr);
                            associations.put(inputPort, Integer.valueOf(displayPort));
                        } catch (NumberFormatException e) {
                            Slog.wtf(TAG, "Display port should be an integer");
                        }
                    }
                } else {
                    confReader.close();
                    return associations;
                }
            }
        } catch (Throwable th) {
            try {
                confReader.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }
}