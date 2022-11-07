package com.android.server.am;

import com.vivo.framework.systemdefence.SystemDefenceManager;

/* loaded from: classes.dex */
public class VivoServiceRecordImpl implements IVivoServiceRecord {
    public boolean checkSmallIconNULLPackage(String packageName) {
        return SystemDefenceManager.getInstance().checkSmallIconNULLPackage(packageName);
    }
}