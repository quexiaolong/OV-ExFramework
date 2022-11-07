package com.vivo.services.nightmode;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class VivoNightModeState {
    static final int FLAG_DEFAULT = 0;
    static final int FLAG_OVERLAY = 2;
    static final int FLAG_USER_SET = 1;
    private boolean disabled;
    private int flag;
    private String packageName;

    /* JADX INFO: Access modifiers changed from: package-private */
    public VivoNightModeState(String packageName, boolean disabled, int flag) {
        this.disabled = true;
        this.packageName = packageName;
        this.disabled = disabled;
        this.flag = flag;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public VivoNightModeState(String packageName) {
        this(packageName, true, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setStates(boolean disabled, int flag) {
        this.disabled = disabled;
        this.flag = flag;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isDisabled() {
        return this.disabled;
    }

    boolean isUserSet() {
        return this.flag == 1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getPkgName() {
        return this.packageName;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getFlag() {
        return this.flag;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void copyFrom(VivoNightModeState vns) {
        this.packageName = vns.packageName;
        this.disabled = vns.disabled;
        this.flag = vns.flag;
    }
}