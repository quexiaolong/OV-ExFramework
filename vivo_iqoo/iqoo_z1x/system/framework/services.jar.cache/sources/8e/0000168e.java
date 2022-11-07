package com.android.server.pm.dex;

import android.content.pm.ApplicationInfo;
import java.util.Map;

/* loaded from: classes.dex */
public interface IVivoDexManager {
    boolean isTinkerPatch(Map<String, String> map);

    void notifyTinkerLoad(ApplicationInfo applicationInfo, Map<String, String> map, String str, int i);
}