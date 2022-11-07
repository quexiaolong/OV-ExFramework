package com.vivo.services.security.server;

import com.vivo.services.superresolution.Constant;
import java.util.ArrayList;

/* loaded from: classes.dex */
public class VivoDeleteUtils {
    public static final ArrayList<String> mWhitePkgs;

    static {
        ArrayList<String> arrayList = new ArrayList<>();
        mWhitePkgs = arrayList;
        arrayList.add("com.tencent.mobileqq");
        mWhitePkgs.add(Constant.APP_WEIXIN);
        mWhitePkgs.add(Constant.APP_WEIBO);
    }
}