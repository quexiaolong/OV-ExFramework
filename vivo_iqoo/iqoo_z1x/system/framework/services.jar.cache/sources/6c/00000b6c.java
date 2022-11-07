package com.android.server.compat;

import android.content.pm.ApplicationInfo;
import com.android.internal.compat.CompatibilityChangeInfo;
import com.android.server.compat.config.Change;
import java.util.HashMap;
import java.util.Map;

/* loaded from: classes.dex */
public final class CompatChange extends CompatibilityChangeInfo {
    private static final long CTS_SYSTEM_API_CHANGEID = 149391281;
    ChangeListener mListener;
    private Map<String, Boolean> mPackageOverrides;

    /* loaded from: classes.dex */
    public interface ChangeListener {
        void onCompatChange(String str);
    }

    public CompatChange(long changeId) {
        this(changeId, null, -1, false, false, null);
    }

    public CompatChange(long changeId, String name, int enableAfterTargetSdk, boolean disabled, boolean loggingOnly, String description) {
        super(Long.valueOf(changeId), name, enableAfterTargetSdk, disabled, loggingOnly, description);
        this.mListener = null;
    }

    public CompatChange(Change change) {
        super(Long.valueOf(change.getId()), change.getName(), change.getEnableAfterTargetSdk(), change.getDisabled(), change.getLoggingOnly(), change.getDescription());
        this.mListener = null;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerListener(ChangeListener listener) {
        if (this.mListener != null) {
            throw new IllegalStateException("Listener for change " + toString() + " already registered.");
        }
        this.mListener = listener;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addPackageOverride(String pname, boolean enabled) {
        if (getLoggingOnly()) {
            throw new IllegalArgumentException("Can't add overrides for a logging only change " + toString());
        }
        if (this.mPackageOverrides == null) {
            this.mPackageOverrides = new HashMap();
        }
        this.mPackageOverrides.put(pname, Boolean.valueOf(enabled));
        notifyListener(pname);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removePackageOverride(String pname) {
        Map<String, Boolean> map = this.mPackageOverrides;
        if (map != null && map.remove(pname) != null) {
            notifyListener(pname);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isEnabled(ApplicationInfo app) {
        Map<String, Boolean> map = this.mPackageOverrides;
        if (map != null && map.containsKey(app.packageName)) {
            return this.mPackageOverrides.get(app.packageName).booleanValue();
        }
        if (getDisabled()) {
            return false;
        }
        return getEnableAfterTargetSdk() == -1 || app.targetSdkVersion > getEnableAfterTargetSdk();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasOverride(String packageName) {
        Map<String, Boolean> map = this.mPackageOverrides;
        return map != null && map.containsKey(packageName);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("ChangeId(").append(getId());
        if (getName() != null) {
            sb.append("; name=");
            sb.append(getName());
        }
        if (getEnableAfterTargetSdk() != -1) {
            sb.append("; enableAfterTargetSdk=");
            sb.append(getEnableAfterTargetSdk());
        }
        if (getDisabled()) {
            sb.append("; disabled");
        }
        if (getLoggingOnly()) {
            sb.append("; loggingOnly");
        }
        Map<String, Boolean> map = this.mPackageOverrides;
        if (map != null && map.size() > 0) {
            sb.append("; packageOverrides=");
            sb.append(this.mPackageOverrides);
        }
        sb.append(")");
        return sb.toString();
    }

    private void notifyListener(String packageName) {
        ChangeListener changeListener = this.mListener;
        if (changeListener != null) {
            changeListener.onCompatChange(packageName);
        }
    }
}