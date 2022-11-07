package com.android.server.backup.keyvalue;

/* loaded from: classes.dex */
class AgentException extends BackupException {
    private final boolean mTransitory;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static AgentException transitory() {
        return new AgentException(true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static AgentException transitory(Exception cause) {
        return new AgentException(true, cause);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static AgentException permanent() {
        return new AgentException(false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static AgentException permanent(Exception cause) {
        return new AgentException(false, cause);
    }

    private AgentException(boolean transitory) {
        this.mTransitory = transitory;
    }

    private AgentException(boolean transitory, Exception cause) {
        super(cause);
        this.mTransitory = transitory;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isTransitory() {
        return this.mTransitory;
    }
}