package com.android.server.updates;

/* loaded from: classes2.dex */
public class ConversationActionsInstallReceiver extends ConfigUpdateInstallReceiver {
    public ConversationActionsInstallReceiver() {
        super("/data/misc/textclassifier/", "actions_suggestions.model", "metadata/actions_suggestions", "version");
    }

    @Override // com.android.server.updates.ConfigUpdateInstallReceiver
    protected boolean verifyVersion(int current, int alternative) {
        return true;
    }
}