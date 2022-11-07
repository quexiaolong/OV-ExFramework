package com.vivo.services.rms.util;

/* loaded from: classes.dex */
public class JniTool {
    public static native void initCgrp();

    public static native String readFile(String str);

    public static native int setProcessGroup(int i, int i2, int i3);

    public static native void setThreadGroup(int i, int i2);

    public static native int writeFile(String str, String str2);
}