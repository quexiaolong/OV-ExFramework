package com.android.server.pm.parsing.pkg;

import android.content.pm.ConfigurationInfo;
import android.content.pm.FeatureGroupInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.parsing.component.ParsedActivity;
import android.content.pm.parsing.component.ParsedInstrumentation;
import android.content.pm.parsing.component.ParsedPermission;
import android.content.pm.parsing.component.ParsedProvider;
import android.content.pm.parsing.component.ParsedService;
import java.util.List;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public interface PkgPackageInfo {
    List<ParsedActivity> getActivities();

    List<ConfigurationInfo> getConfigPreferences();

    List<FeatureGroupInfo> getFeatureGroups();

    List<ParsedInstrumentation> getInstrumentations();

    long getLongVersionCode();

    String getOverlayCategory();

    int getOverlayPriority();

    String getOverlayTarget();

    String getOverlayTargetName();

    List<ParsedPermission> getPermissions();

    List<ParsedProvider> getProviders();

    List<ParsedActivity> getReceivers();

    List<FeatureInfo> getReqFeatures();

    List<String> getRequestedPermissions();

    String getRequiredAccountType();

    String getRestrictedAccountType();

    List<ParsedService> getServices();

    @Deprecated
    String getSharedUserId();

    @Deprecated
    int getSharedUserLabel();

    int[] getSplitRevisionCodes();

    @Deprecated
    int getVersionCode();

    int getVersionCodeMajor();

    String getVersionName();

    boolean isCoreApp();

    boolean isOverlayIsStatic();

    boolean isRequiredForAllUsers();

    boolean isStub();
}