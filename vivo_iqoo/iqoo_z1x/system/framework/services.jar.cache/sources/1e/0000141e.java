package com.android.server.om;

import android.content.om.OverlayableInfo;
import android.content.pm.PackageInfo;
import java.io.IOException;
import java.util.Map;

/* loaded from: classes.dex */
public interface OverlayableInfoCallback {
    boolean doesTargetDefineOverlayable(String str, int i) throws IOException;

    void enforcePermission(String str, String str2) throws SecurityException;

    Map<String, Map<String, String>> getNamedActors();

    OverlayableInfo getOverlayableForTarget(String str, String str2, int i) throws IOException;

    PackageInfo getPackageInfo(String str, int i);

    String[] getPackagesForUid(int i);

    boolean signaturesMatching(String str, String str2, int i);
}