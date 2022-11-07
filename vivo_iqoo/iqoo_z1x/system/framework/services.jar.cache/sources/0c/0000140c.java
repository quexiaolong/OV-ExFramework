package com.android.server.om;

import android.content.om.OverlayInfo;
import android.content.om.OverlayableInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Pair;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.CollectionUtils;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/* loaded from: classes.dex */
public class OverlayActorEnforcer {
    private static final boolean DEBUG_REASON = false;
    private final OverlayableInfoCallback mOverlayableCallback;

    /* loaded from: classes.dex */
    public enum ActorState {
        ALLOWED,
        INVALID_ACTOR,
        MISSING_NAMESPACE,
        MISSING_PACKAGE,
        MISSING_APP_INFO,
        ACTOR_NOT_PREINSTALLED,
        NO_PACKAGES_FOR_UID,
        MISSING_ACTOR_NAME,
        ERROR_READING_OVERLAYABLE,
        MISSING_TARGET_OVERLAYABLE_NAME,
        MISSING_OVERLAYABLE,
        INVALID_OVERLAYABLE_ACTOR_NAME,
        NO_NAMED_ACTORS,
        UNABLE_TO_GET_TARGET,
        MISSING_LEGACY_PERMISSION
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Pair<String, ActorState> getPackageNameForActor(String actorUriString, Map<String, Map<String, String>> namedActors) {
        Uri actorUri = Uri.parse(actorUriString);
        String actorScheme = actorUri.getScheme();
        List<String> actorPathSegments = actorUri.getPathSegments();
        if (!"overlay".equals(actorScheme) || CollectionUtils.size(actorPathSegments) != 1) {
            return Pair.create(null, ActorState.INVALID_OVERLAYABLE_ACTOR_NAME);
        }
        if (namedActors.isEmpty()) {
            return Pair.create(null, ActorState.NO_NAMED_ACTORS);
        }
        String actorNamespace = actorUri.getAuthority();
        Map<String, String> namespace = namedActors.get(actorNamespace);
        if (namespace == null) {
            return Pair.create(null, ActorState.MISSING_NAMESPACE);
        }
        String actorName = actorPathSegments.get(0);
        String packageName = namespace.get(actorName);
        if (TextUtils.isEmpty(packageName)) {
            return Pair.create(null, ActorState.MISSING_ACTOR_NAME);
        }
        return Pair.create(packageName, ActorState.ALLOWED);
    }

    public OverlayActorEnforcer(OverlayableInfoCallback overlayableCallback) {
        this.mOverlayableCallback = overlayableCallback;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void enforceActor(OverlayInfo overlayInfo, String methodName, int callingUid, int userId) throws SecurityException {
        String str;
        ActorState actorState = isAllowedActor(methodName, overlayInfo, callingUid, userId);
        if (actorState == ActorState.ALLOWED) {
            return;
        }
        String targetOverlayableName = overlayInfo.targetOverlayableName;
        StringBuilder sb = new StringBuilder();
        sb.append("UID");
        sb.append(callingUid);
        sb.append(" is not allowed to call ");
        sb.append(methodName);
        sb.append(" for ");
        if (TextUtils.isEmpty(targetOverlayableName)) {
            str = "";
        } else {
            str = targetOverlayableName + " in ";
        }
        sb.append(str);
        sb.append(overlayInfo.targetPackageName);
        sb.append("");
        throw new SecurityException(sb.toString());
    }

    private ActorState isAllowedActor(String methodName, OverlayInfo overlayInfo, int callingUid, int userId) {
        if (callingUid == 0 || callingUid == 1000) {
            return ActorState.ALLOWED;
        }
        String[] callingPackageNames = this.mOverlayableCallback.getPackagesForUid(callingUid);
        if (ArrayUtils.isEmpty(callingPackageNames)) {
            return ActorState.NO_PACKAGES_FOR_UID;
        }
        String targetPackageName = overlayInfo.targetPackageName;
        if (ArrayUtils.contains(callingPackageNames, targetPackageName)) {
            return ActorState.ALLOWED;
        }
        String targetOverlayableName = overlayInfo.targetOverlayableName;
        if (TextUtils.isEmpty(targetOverlayableName)) {
            try {
                if (this.mOverlayableCallback.doesTargetDefineOverlayable(targetPackageName, userId)) {
                    return ActorState.MISSING_TARGET_OVERLAYABLE_NAME;
                }
                try {
                    this.mOverlayableCallback.enforcePermission("android.permission.CHANGE_OVERLAY_PACKAGES", methodName);
                    return ActorState.ALLOWED;
                } catch (SecurityException e) {
                    return ActorState.MISSING_LEGACY_PERMISSION;
                }
            } catch (IOException e2) {
                return ActorState.ERROR_READING_OVERLAYABLE;
            }
        }
        try {
            OverlayableInfo targetOverlayable = this.mOverlayableCallback.getOverlayableForTarget(targetPackageName, targetOverlayableName, userId);
            if (targetOverlayable == null) {
                return ActorState.MISSING_OVERLAYABLE;
            }
            String actor = targetOverlayable.actor;
            if (TextUtils.isEmpty(actor)) {
                try {
                    this.mOverlayableCallback.enforcePermission("android.permission.CHANGE_OVERLAY_PACKAGES", methodName);
                    return ActorState.ALLOWED;
                } catch (SecurityException e3) {
                    return ActorState.MISSING_LEGACY_PERMISSION;
                }
            }
            Map<String, Map<String, String>> namedActors = this.mOverlayableCallback.getNamedActors();
            Pair<String, ActorState> actorUriPair = getPackageNameForActor(actor, namedActors);
            ActorState actorUriState = (ActorState) actorUriPair.second;
            if (actorUriState != ActorState.ALLOWED) {
                return actorUriState;
            }
            String packageName = (String) actorUriPair.first;
            PackageInfo packageInfo = this.mOverlayableCallback.getPackageInfo(packageName, userId);
            if (packageInfo == null) {
                return ActorState.MISSING_APP_INFO;
            }
            ApplicationInfo appInfo = packageInfo.applicationInfo;
            if (appInfo == null) {
                return ActorState.MISSING_APP_INFO;
            }
            if (!appInfo.isSystemApp() && !appInfo.isUpdatedSystemApp()) {
                return ActorState.ACTOR_NOT_PREINSTALLED;
            }
            if (ArrayUtils.contains(callingPackageNames, packageName)) {
                return ActorState.ALLOWED;
            }
            return ActorState.INVALID_ACTOR;
        } catch (IOException e4) {
            return ActorState.UNABLE_TO_GET_TARGET;
        }
    }
}