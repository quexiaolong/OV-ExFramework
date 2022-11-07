package com.android.server.devicepolicy;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.ArraySet;
import android.view.inputmethod.InputMethodInfo;
import com.android.internal.util.Preconditions;
import com.android.server.inputmethod.InputMethodManagerInternal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/* loaded from: classes.dex */
public class OverlayPackagesProvider {
    protected static final String TAG = "OverlayPackagesProvider";
    private final Context mContext;
    private final Injector mInjector;
    private final PackageManager mPm;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface Injector {
        List<InputMethodInfo> getInputMethodListAsUser(int i);
    }

    public OverlayPackagesProvider(Context context) {
        this(context, new DefaultInjector());
    }

    /* loaded from: classes.dex */
    private static final class DefaultInjector implements Injector {
        private DefaultInjector() {
        }

        @Override // com.android.server.devicepolicy.OverlayPackagesProvider.Injector
        public List<InputMethodInfo> getInputMethodListAsUser(int userId) {
            return InputMethodManagerInternal.get().getInputMethodListAsUser(userId);
        }
    }

    OverlayPackagesProvider(Context context, Injector injector) {
        this.mContext = context;
        this.mPm = (PackageManager) Preconditions.checkNotNull(context.getPackageManager());
        this.mInjector = (Injector) Preconditions.checkNotNull(injector);
    }

    public Set<String> getNonRequiredApps(ComponentName admin, int userId, String provisioningAction) {
        Set<String> nonRequiredApps = getLaunchableApps(userId);
        nonRequiredApps.removeAll(getRequiredApps(provisioningAction, admin.getPackageName()));
        nonRequiredApps.removeAll(getSystemInputMethods(userId));
        nonRequiredApps.addAll(getDisallowedApps(provisioningAction));
        return nonRequiredApps;
    }

    private Set<String> getLaunchableApps(int userId) {
        Intent launcherIntent = new Intent("android.intent.action.MAIN");
        launcherIntent.addCategory("android.intent.category.LAUNCHER");
        List<ResolveInfo> resolveInfos = this.mPm.queryIntentActivitiesAsUser(launcherIntent, 795136, userId);
        Set<String> apps = new ArraySet<>();
        for (ResolveInfo resolveInfo : resolveInfos) {
            apps.add(resolveInfo.activityInfo.packageName);
        }
        return apps;
    }

    private Set<String> getSystemInputMethods(int userId) {
        List<InputMethodInfo> inputMethods = this.mInjector.getInputMethodListAsUser(userId);
        Set<String> systemInputMethods = new ArraySet<>();
        for (InputMethodInfo inputMethodInfo : inputMethods) {
            ApplicationInfo applicationInfo = inputMethodInfo.getServiceInfo().applicationInfo;
            if (applicationInfo.isSystemApp()) {
                systemInputMethods.add(inputMethodInfo.getPackageName());
            }
        }
        return systemInputMethods;
    }

    private Set<String> getRequiredApps(String provisioningAction, String dpcPackageName) {
        Set<String> requiredApps = new ArraySet<>();
        requiredApps.addAll(getRequiredAppsSet(provisioningAction));
        requiredApps.addAll(getVendorRequiredAppsSet(provisioningAction));
        requiredApps.add(dpcPackageName);
        return requiredApps;
    }

    private Set<String> getDisallowedApps(String provisioningAction) {
        Set<String> disallowedApps = new ArraySet<>();
        disallowedApps.addAll(getDisallowedAppsSet(provisioningAction));
        disallowedApps.addAll(getVendorDisallowedAppsSet(provisioningAction));
        return disallowedApps;
    }

    private Set<String> getRequiredAppsSet(String provisioningAction) {
        char c;
        int resId;
        int hashCode = provisioningAction.hashCode();
        if (hashCode == -920528692) {
            if (provisioningAction.equals("android.app.action.PROVISION_MANAGED_DEVICE")) {
                c = 2;
            }
            c = 65535;
        } else if (hashCode != -514404415) {
            if (hashCode == -340845101 && provisioningAction.equals("android.app.action.PROVISION_MANAGED_PROFILE")) {
                c = 1;
            }
            c = 65535;
        } else {
            if (provisioningAction.equals("android.app.action.PROVISION_MANAGED_USER")) {
                c = 0;
            }
            c = 65535;
        }
        if (c == 0) {
            resId = 17236120;
        } else if (c == 1) {
            resId = 17236119;
        } else if (c == 2) {
            resId = 17236118;
        } else {
            throw new IllegalArgumentException("Provisioning type " + provisioningAction + " not supported.");
        }
        return new ArraySet(Arrays.asList(this.mContext.getResources().getStringArray(resId)));
    }

    private Set<String> getDisallowedAppsSet(String provisioningAction) {
        char c;
        int resId;
        int hashCode = provisioningAction.hashCode();
        if (hashCode == -920528692) {
            if (provisioningAction.equals("android.app.action.PROVISION_MANAGED_DEVICE")) {
                c = 2;
            }
            c = 65535;
        } else if (hashCode != -514404415) {
            if (hashCode == -340845101 && provisioningAction.equals("android.app.action.PROVISION_MANAGED_PROFILE")) {
                c = 1;
            }
            c = 65535;
        } else {
            if (provisioningAction.equals("android.app.action.PROVISION_MANAGED_USER")) {
                c = 0;
            }
            c = 65535;
        }
        if (c == 0) {
            resId = 17236101;
        } else if (c == 1) {
            resId = 17236100;
        } else if (c == 2) {
            resId = 17236099;
        } else {
            throw new IllegalArgumentException("Provisioning type " + provisioningAction + " not supported.");
        }
        return new ArraySet(Arrays.asList(this.mContext.getResources().getStringArray(resId)));
    }

    private Set<String> getVendorRequiredAppsSet(String provisioningAction) {
        char c;
        int resId;
        int hashCode = provisioningAction.hashCode();
        if (hashCode == -920528692) {
            if (provisioningAction.equals("android.app.action.PROVISION_MANAGED_DEVICE")) {
                c = 2;
            }
            c = 65535;
        } else if (hashCode != -514404415) {
            if (hashCode == -340845101 && provisioningAction.equals("android.app.action.PROVISION_MANAGED_PROFILE")) {
                c = 1;
            }
            c = 65535;
        } else {
            if (provisioningAction.equals("android.app.action.PROVISION_MANAGED_USER")) {
                c = 0;
            }
            c = 65535;
        }
        if (c == 0) {
            resId = 17236135;
        } else if (c == 1) {
            resId = 17236134;
        } else if (c == 2) {
            resId = 17236133;
        } else {
            throw new IllegalArgumentException("Provisioning type " + provisioningAction + " not supported.");
        }
        return new ArraySet(Arrays.asList(this.mContext.getResources().getStringArray(resId)));
    }

    private Set<String> getVendorDisallowedAppsSet(String provisioningAction) {
        char c;
        int resId;
        int hashCode = provisioningAction.hashCode();
        if (hashCode == -920528692) {
            if (provisioningAction.equals("android.app.action.PROVISION_MANAGED_DEVICE")) {
                c = 2;
            }
            c = 65535;
        } else if (hashCode != -514404415) {
            if (hashCode == -340845101 && provisioningAction.equals("android.app.action.PROVISION_MANAGED_PROFILE")) {
                c = 1;
            }
            c = 65535;
        } else {
            if (provisioningAction.equals("android.app.action.PROVISION_MANAGED_USER")) {
                c = 0;
            }
            c = 65535;
        }
        if (c == 0) {
            resId = 17236132;
        } else if (c == 1) {
            resId = 17236131;
        } else if (c == 2) {
            resId = 17236130;
        } else {
            throw new IllegalArgumentException("Provisioning type " + provisioningAction + " not supported.");
        }
        return new ArraySet(Arrays.asList(this.mContext.getResources().getStringArray(resId)));
    }
}