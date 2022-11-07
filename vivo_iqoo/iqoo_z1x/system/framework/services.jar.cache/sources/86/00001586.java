package com.android.server.pm;

import android.content.pm.PackageInstaller;
import java.io.File;

/* loaded from: classes.dex */
public interface IVivoPackageInstallerSession {
    boolean checkDelayUpdate(String str);

    void dummy();

    void openWriteBoost();

    void openWriteBoostRelease();

    void parserApkNotCheckSignature(File file);

    void reportInstallFailedException(int i, String str, String str2, String str3, long j, int i2, PackageInstaller.SessionParams sessionParams, String str4);
}