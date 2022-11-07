package com.android.server.policy;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.hardware.audio.common.V2_0.AudioFormat;
import android.os.UserHandle;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import java.io.PrintWriter;
import org.xmlpull.v1.XmlPullParserException;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class GlobalKeyManager {
    private static final String ATTR_COMPONENT = "component";
    private static final String ATTR_KEY_CODE = "keyCode";
    private static final String ATTR_VERSION = "version";
    private static final int GLOBAL_KEY_FILE_VERSION = 1;
    private static final String TAG = "GlobalKeyManager";
    private static final String TAG_GLOBAL_KEYS = "global_keys";
    private static final String TAG_KEY = "key";
    private SparseArray<ComponentName> mKeyMapping = new SparseArray<>();

    public GlobalKeyManager(Context context) {
        loadGlobalKeys(context);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean handleGlobalKey(Context context, int keyCode, KeyEvent event) {
        ComponentName component;
        if (this.mKeyMapping.size() > 0 && (component = this.mKeyMapping.get(keyCode)) != null) {
            Intent intent = new Intent("android.intent.action.GLOBAL_BUTTON").setComponent(component).setFlags(AudioFormat.EVRC).putExtra("android.intent.extra.KEY_EVENT", new KeyEvent(event));
            context.sendBroadcastAsUser(intent, UserHandle.CURRENT, null);
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean shouldHandleGlobalKey(int keyCode, KeyEvent event) {
        return this.mKeyMapping.get(keyCode) != null;
    }

    private void loadGlobalKeys(Context context) {
        XmlResourceParser parser = null;
        try {
            try {
                try {
                    try {
                        parser = context.getResources().getXml(18284554);
                        XmlUtils.beginDocument(parser, TAG_GLOBAL_KEYS);
                        int version = parser.getAttributeIntValue(null, ATTR_VERSION, 0);
                        if (1 == version) {
                            while (true) {
                                XmlUtils.nextElement(parser);
                                String element = parser.getName();
                                if (element == null) {
                                    break;
                                } else if (TAG_KEY.equals(element)) {
                                    String keyCodeName = parser.getAttributeValue(null, ATTR_KEY_CODE);
                                    String componentName = parser.getAttributeValue(null, ATTR_COMPONENT);
                                    int keyCode = KeyEvent.keyCodeFromString(keyCodeName);
                                    if (keyCode != 0) {
                                        this.mKeyMapping.put(keyCode, ComponentName.unflattenFromString(componentName));
                                    }
                                }
                            }
                        }
                        if (parser == null) {
                            return;
                        }
                    } catch (XmlPullParserException e) {
                        Log.w(TAG, "XML parser exception reading global keys file", e);
                        if (parser == null) {
                            return;
                        }
                    }
                } catch (Resources.NotFoundException e2) {
                    Log.w(TAG, "global keys file not found", e2);
                    if (parser == null) {
                        return;
                    }
                }
            } catch (IOException e3) {
                Log.w(TAG, "I/O exception reading global keys file", e3);
                if (parser == null) {
                    return;
                }
            }
            parser.close();
        } catch (Throwable th) {
            if (parser != null) {
                parser.close();
            }
            throw th;
        }
    }

    public void dump(String prefix, PrintWriter pw) {
        int numKeys = this.mKeyMapping.size();
        if (numKeys == 0) {
            pw.print(prefix);
            pw.println("mKeyMapping.size=0");
            return;
        }
        pw.print(prefix);
        pw.println("mKeyMapping={");
        for (int i = 0; i < numKeys; i++) {
            pw.print("  ");
            pw.print(prefix);
            pw.print(KeyEvent.keyCodeToString(this.mKeyMapping.keyAt(i)));
            pw.print("=");
            pw.println(this.mKeyMapping.valueAt(i).flattenToString());
        }
        pw.print(prefix);
        pw.println("}");
    }
}