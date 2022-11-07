package com.android.server;

import com.oplus.reflect.RefClass;
import com.oplus.reflect.RefConstructor;
import com.oplus.reflect.RefStaticMethod;

/* loaded from: classes.dex */
public class SystemServerExtPlugin {
    public static Class<?> TYPE = RefClass.load(SystemServerExtPlugin.class, "com.android.server.SystemServerExtImpl");
    public static RefConstructor<ISystemServerExt> constructor;
    public static RefStaticMethod<Integer> getSystemThemeStyle;
}