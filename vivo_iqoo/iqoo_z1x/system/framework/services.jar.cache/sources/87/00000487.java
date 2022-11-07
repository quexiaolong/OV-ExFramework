package com.android.server;

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;

/* loaded from: classes.dex */
public interface IVivoFrozenInjector {
    public static final int BY_ADD_WIDGET = 2;
    public static final int BY_ADUIO_FOCUS = 0;
    public static final int BY_DRAWN_TIMEOUT = 11;
    public static final int BY_DUMP = 10;
    public static final int BY_KILL_SERVICE = 13;
    public static final int BY_MEDIA_KEY = 7;
    public static final int BY_MEDIA_RES = 3;
    public static final int BY_NOTIFICATION = 9;
    public static final int BY_PHONE_STATE = 6;
    public static final int BY_PID_DIED = 4;
    public static final int BY_RELATED_START = 5;
    public static final int BY_START_ACTIVITY = 1;
    public static final int BY_STOP_SERVICE = 14;
    public static final int BY_SYNC_BINDER = 12;
    public static final int BY_UNBIND_SERVICE = 15;
    public static final int BY_VIVO_PUSH = 8;
    public static final int FROM_FG = 2;
    public static final int FROM_PEM = 0;
    public static final int FROM_RMS = 1;
    public static final int FROZEN = 1;
    public static final int REASON_UNKNOWN = 99;
    public static final int STATUS_FROZEN_NONE = 0;
    public static final int STATUS_FROZEN_RUNNING = 1;
    public static final int STATUS_FROZEN_SUCCESS = 3;
    public static final int STATUS_UNFROZEN_RUNNING = 2;
    public static final int UNFROZEN = 0;

    default void notifyIdle(String packageName, String processName, int pid, int uid) {
    }

    default void setHomeProcess(int pid) {
    }

    default void reportFreezeStatus(int uid, String procName, int pid, boolean canFreeze, int procState) {
    }

    default void initialize(Context context) {
    }

    default void notifyActivityFromRecents(String packageName, String ProcName, int pid, int uid) {
    }

    default void setQuickFrozenEnable(boolean frozenEnable, long downloadThd) {
    }

    default void notifyQuickFrozenPause(boolean pause) {
    }

    default void dumpQuickFrozenInformation(PrintWriter pw, String[] args, int opti) {
    }

    default void notifyProcDied(String processName, int pid) {
    }

    default void setWorkingState(int model, int state, int uid) {
    }

    default void setWorkingState(int model, int state, int uid, int pid) {
    }

    default void setPhoneState(int state) {
    }

    default void noteScreenState(int state) {
    }

    default void reportWallPaperService(ComponentName wallpaper) {
    }

    default void reportInputMethod(String packageName) {
    }

    default boolean setFrozenPkgBlacklist(List<String> pkgNames, int len) {
        return true;
    }

    default boolean setFrozenPkgWhitelist(List<String> pkgNames, int len) {
        return true;
    }

    default boolean setPackageList(String type, Bundle data) {
        return true;
    }

    default HashSet<String> getDefaultBackList() {
        return null;
    }

    default boolean isAudioOn(int uid, String pkgName) {
        return true;
    }

    default void systemReady() {
    }

    default void addWindow(String pkgName, int uid) {
    }
}