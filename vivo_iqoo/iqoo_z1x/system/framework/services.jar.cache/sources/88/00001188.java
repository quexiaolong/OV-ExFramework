package com.android.server.locksettings;

import java.io.PrintWriter;

/* loaded from: classes.dex */
public interface IVivoLockSettingsShellCommand {
    void runSetVivoConvenienceLevelStrongauhTimeout(String str, PrintWriter printWriter, int i);

    void runSetVivoNonStrongTimeout(String str, PrintWriter printWriter, int i);

    void runSetVivoTimeout(String str, PrintWriter printWriter);

    void runSetVivoTimeout(String str, PrintWriter printWriter, int i);
}