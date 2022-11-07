package com.android.server.pm;

import com.android.internal.util.Preconditions;
import java.util.Objects;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class InstallSource {
    static final InstallSource EMPTY = new InstallSource(null, null, null, false, false, null);
    private static final InstallSource EMPTY_ORPHANED = new InstallSource(null, null, null, true, false, null);
    final String initiatingPackageName;
    final PackageSignatures initiatingPackageSignatures;
    final String installerPackageName;
    final boolean isInitiatingPackageUninstalled;
    final boolean isOrphaned;
    final String originatingPackageName;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static InstallSource create(String initiatingPackageName, String originatingPackageName, String installerPackageName) {
        return create(initiatingPackageName, originatingPackageName, installerPackageName, false, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static InstallSource create(String initiatingPackageName, String originatingPackageName, String installerPackageName, boolean isOrphaned, boolean isInitiatingPackageUninstalled) {
        return createInternal(intern(initiatingPackageName), intern(originatingPackageName), intern(installerPackageName), isOrphaned, isInitiatingPackageUninstalled, null);
    }

    private static InstallSource createInternal(String initiatingPackageName, String originatingPackageName, String installerPackageName, boolean isOrphaned, boolean isInitiatingPackageUninstalled, PackageSignatures initiatingPackageSignatures) {
        if (initiatingPackageName == null && originatingPackageName == null && installerPackageName == null && initiatingPackageSignatures == null && !isInitiatingPackageUninstalled) {
            return isOrphaned ? EMPTY_ORPHANED : EMPTY;
        }
        return new InstallSource(initiatingPackageName, originatingPackageName, installerPackageName, isOrphaned, isInitiatingPackageUninstalled, initiatingPackageSignatures);
    }

    private InstallSource(String initiatingPackageName, String originatingPackageName, String installerPackageName, boolean isOrphaned, boolean isInitiatingPackageUninstalled, PackageSignatures initiatingPackageSignatures) {
        if (initiatingPackageName == null) {
            Preconditions.checkArgument(initiatingPackageSignatures == null);
            Preconditions.checkArgument(!isInitiatingPackageUninstalled);
        }
        this.initiatingPackageName = initiatingPackageName;
        this.originatingPackageName = originatingPackageName;
        this.installerPackageName = installerPackageName;
        this.isOrphaned = isOrphaned;
        this.isInitiatingPackageUninstalled = isInitiatingPackageUninstalled;
        this.initiatingPackageSignatures = initiatingPackageSignatures;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InstallSource setInstallerPackage(String installerPackageName) {
        if (Objects.equals(installerPackageName, this.installerPackageName)) {
            return this;
        }
        return createInternal(this.initiatingPackageName, this.originatingPackageName, intern(installerPackageName), this.isOrphaned, this.isInitiatingPackageUninstalled, this.initiatingPackageSignatures);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InstallSource setIsOrphaned(boolean isOrphaned) {
        if (isOrphaned == this.isOrphaned) {
            return this;
        }
        return createInternal(this.initiatingPackageName, this.originatingPackageName, this.installerPackageName, isOrphaned, this.isInitiatingPackageUninstalled, this.initiatingPackageSignatures);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InstallSource setInitiatingPackageSignatures(PackageSignatures signatures) {
        if (signatures == this.initiatingPackageSignatures) {
            return this;
        }
        return createInternal(this.initiatingPackageName, this.originatingPackageName, this.installerPackageName, this.isOrphaned, this.isInitiatingPackageUninstalled, signatures);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public InstallSource removeInstallerPackage(String packageName) {
        if (packageName == null) {
            return this;
        }
        boolean modified = false;
        boolean isInitiatingPackageUninstalled = this.isInitiatingPackageUninstalled;
        String originatingPackageName = this.originatingPackageName;
        String installerPackageName = this.installerPackageName;
        boolean isOrphaned = this.isOrphaned;
        if (packageName.equals(this.initiatingPackageName) && !isInitiatingPackageUninstalled) {
            isInitiatingPackageUninstalled = true;
            modified = true;
        }
        if (packageName.equals(originatingPackageName)) {
            originatingPackageName = null;
            modified = true;
        }
        if (packageName.equals(installerPackageName)) {
            installerPackageName = null;
            isOrphaned = true;
            modified = true;
        }
        if (!modified) {
            return this;
        }
        return createInternal(this.initiatingPackageName, originatingPackageName, installerPackageName, isOrphaned, isInitiatingPackageUninstalled, this.initiatingPackageSignatures);
    }

    private static String intern(String packageName) {
        if (packageName == null) {
            return null;
        }
        return packageName.intern();
    }
}