package com.android.server.pm.parsing.pkg;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageParser;
import android.content.pm.parsing.ParsingPackageRead;
import android.content.pm.parsing.component.ParsedAttribution;
import android.content.pm.parsing.component.ParsedIntentInfo;
import android.content.pm.parsing.component.ParsedPermissionGroup;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.ArraySet;
import android.util.Pair;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/* loaded from: classes.dex */
public interface AndroidPackage extends PkgAppInfo, PkgPackageInfo, ParsingPackageRead, Parcelable {
    List<String> getAdoptPermissions();

    List<ParsedAttribution> getAttributions();

    String getBaseCodePath();

    int getBaseRevisionCode();

    String getCodePath();

    List<String> getImplicitPermissions();

    Map<String, ArraySet<PublicKey>> getKeySetMapping();

    List<String> getLibraryNames();

    String getManifestPackageName();

    Bundle getMetaData();

    List<String> getOriginalPackages();

    Map<String, String> getOverlayables();

    String getPackageName();

    List<ParsedPermissionGroup> getPermissionGroups();

    List<Pair<String, ParsedIntentInfo>> getPreferredActivityFilters();

    List<String> getProtectedBroadcasts();

    List<Intent> getQueriesIntents();

    List<String> getQueriesPackages();

    String getRealPackage();

    byte[] getRestrictUpdateHash();

    PackageParser.SigningDetails getSigningDetails();

    int[] getSplitFlags();

    String[] getSplitNames();

    String getStaticSharedLibName();

    long getStaticSharedLibVersion();

    UUID getStorageUuid();

    Set<String> getUpgradeKeySets();

    List<String> getUsesLibraries();

    List<String> getUsesOptionalLibraries();

    List<String> getUsesStaticLibraries();

    String[][] getUsesStaticLibrariesCertDigests();

    long[] getUsesStaticLibrariesVersions();

    boolean isCrossProfile();

    boolean isForceQueryable();

    boolean isUse32BitAbi();

    boolean isVisibleToInstantApps();

    @Deprecated
    String toAppInfoToString();

    @Deprecated
    ApplicationInfo toAppInfoWithoutState();

    ApplicationInfo toAppInfoWithoutStateWithoutFlags();
}