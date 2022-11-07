package com.android.server.om;

import android.content.pm.PackageInfo;
import java.util.List;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public interface PackageManagerHelper {
    List<PackageInfo> getOverlayPackages(int i);

    PackageInfo getPackageInfo(String str, int i);

    boolean signaturesMatching(String str, String str2, int i);
}