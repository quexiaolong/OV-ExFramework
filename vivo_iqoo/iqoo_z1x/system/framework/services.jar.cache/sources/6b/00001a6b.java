package com.android.server.updates;

/* loaded from: classes2.dex */
public class LangIdInstallReceiver extends ConfigUpdateInstallReceiver {
    public LangIdInstallReceiver() {
        super("/data/misc/textclassifier/", "lang_id.model", "metadata/lang_id", "version");
    }

    @Override // com.android.server.updates.ConfigUpdateInstallReceiver
    protected boolean verifyVersion(int current, int alternative) {
        return true;
    }
}