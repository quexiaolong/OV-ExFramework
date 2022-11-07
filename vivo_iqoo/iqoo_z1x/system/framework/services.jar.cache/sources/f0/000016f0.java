package com.android.server.pm.permission;

import android.content.pm.PackageManager;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import com.android.internal.util.ArrayUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/* loaded from: classes.dex */
public final class PermissionsState {
    private static final int[] NO_GIDS = new int[0];
    public static final int PERMISSION_OPERATION_FAILURE = -1;
    public static final int PERMISSION_OPERATION_SUCCESS = 0;
    public static final int PERMISSION_OPERATION_SUCCESS_GIDS_CHANGED = 1;
    private SparseBooleanArray mMissing;
    private SparseBooleanArray mPermissionReviewRequired;
    private ArrayMap<String, PermissionData> mPermissions;
    private final Object mLock = new Object();
    private int[] mGlobalGids = NO_GIDS;

    public PermissionsState() {
    }

    public PermissionsState(PermissionsState prototype) {
        copyFrom(prototype);
    }

    public void setGlobalGids(int[] globalGids) {
        if (!ArrayUtils.isEmpty(globalGids)) {
            this.mGlobalGids = Arrays.copyOf(globalGids, globalGids.length);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void invalidateCache() {
        PackageManager.invalidatePackageInfoCache();
    }

    public void copyFrom(PermissionsState other) {
        if (other == this) {
            return;
        }
        synchronized (this.mLock) {
            if (this.mPermissions != null) {
                if (other.mPermissions == null) {
                    this.mPermissions = null;
                } else {
                    this.mPermissions.clear();
                }
            }
            if (other.mPermissions != null) {
                if (this.mPermissions == null) {
                    this.mPermissions = new ArrayMap<>();
                }
                int permissionCount = other.mPermissions.size();
                for (int i = 0; i < permissionCount; i++) {
                    String name = other.mPermissions.keyAt(i);
                    PermissionData permissionData = other.mPermissions.valueAt(i);
                    this.mPermissions.put(name, new PermissionData(permissionData));
                }
            }
        }
        int[] iArr = NO_GIDS;
        this.mGlobalGids = iArr;
        int[] iArr2 = other.mGlobalGids;
        if (iArr2 != iArr) {
            this.mGlobalGids = Arrays.copyOf(iArr2, iArr2.length);
        }
        SparseBooleanArray sparseBooleanArray = this.mMissing;
        if (sparseBooleanArray != null) {
            if (other.mMissing == null) {
                this.mMissing = null;
            } else {
                sparseBooleanArray.clear();
            }
        }
        if (other.mMissing != null) {
            if (this.mMissing == null) {
                this.mMissing = new SparseBooleanArray();
            }
            int missingSize = other.mMissing.size();
            for (int i2 = 0; i2 < missingSize; i2++) {
                this.mMissing.put(other.mMissing.keyAt(i2), other.mMissing.valueAt(i2));
            }
        }
        SparseBooleanArray sparseBooleanArray2 = this.mPermissionReviewRequired;
        if (sparseBooleanArray2 != null) {
            if (other.mPermissionReviewRequired == null) {
                this.mPermissionReviewRequired = null;
            } else {
                sparseBooleanArray2.clear();
            }
        }
        if (other.mPermissionReviewRequired != null) {
            if (this.mPermissionReviewRequired == null) {
                this.mPermissionReviewRequired = new SparseBooleanArray();
            }
            int userCount = other.mPermissionReviewRequired.size();
            for (int i3 = 0; i3 < userCount; i3++) {
                boolean reviewRequired = other.mPermissionReviewRequired.valueAt(i3);
                this.mPermissionReviewRequired.put(other.mPermissionReviewRequired.keyAt(i3), reviewRequired);
            }
        }
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PermissionsState other = (PermissionsState) obj;
        synchronized (this.mLock) {
            if (this.mPermissions == null) {
                if (other.mPermissions != null) {
                    return false;
                }
            } else if (!this.mPermissions.equals(other.mPermissions)) {
                return false;
            }
            if (!Objects.equals(this.mMissing, other.mMissing)) {
                return false;
            }
            SparseBooleanArray sparseBooleanArray = this.mPermissionReviewRequired;
            if (sparseBooleanArray == null) {
                if (other.mPermissionReviewRequired != null) {
                    return false;
                }
            } else if (!sparseBooleanArray.equals(other.mPermissionReviewRequired)) {
                return false;
            }
            return Arrays.equals(this.mGlobalGids, other.mGlobalGids);
        }
    }

    public boolean isMissing(int userId) {
        SparseBooleanArray sparseBooleanArray = this.mMissing;
        return sparseBooleanArray != null && sparseBooleanArray.get(userId);
    }

    public void setMissing(boolean missing, int userId) {
        if (missing) {
            if (this.mMissing == null) {
                this.mMissing = new SparseBooleanArray();
            }
            this.mMissing.put(userId, true);
            return;
        }
        SparseBooleanArray sparseBooleanArray = this.mMissing;
        if (sparseBooleanArray != null) {
            sparseBooleanArray.delete(userId);
            if (this.mMissing.size() == 0) {
                this.mMissing = null;
            }
        }
    }

    public boolean isPermissionReviewRequired(int userId) {
        SparseBooleanArray sparseBooleanArray = this.mPermissionReviewRequired;
        return sparseBooleanArray != null && sparseBooleanArray.get(userId);
    }

    public int grantInstallPermission(BasePermission permission) {
        return grantPermission(permission, -1);
    }

    public int revokeInstallPermission(BasePermission permission) {
        return revokePermission(permission, -1);
    }

    public int grantRuntimePermission(BasePermission permission, int userId) {
        enforceValidUserId(userId);
        if (userId == -1) {
            return -1;
        }
        return grantPermission(permission, userId);
    }

    public int revokeRuntimePermission(BasePermission permission, int userId) {
        enforceValidUserId(userId);
        if (userId == -1) {
            return -1;
        }
        return revokePermission(permission, userId);
    }

    public boolean hasRuntimePermission(String name, int userId) {
        enforceValidUserId(userId);
        return !hasInstallPermission(name) && hasPermission(name, userId);
    }

    public boolean hasInstallPermission(String name) {
        return hasPermission(name, -1);
    }

    public boolean hasPermission(String name, int userId) {
        enforceValidUserId(userId);
        synchronized (this.mLock) {
            boolean z = false;
            if (this.mPermissions == null) {
                return false;
            }
            PermissionData permissionData = this.mPermissions.get(name);
            if (permissionData != null && permissionData.isGranted(userId)) {
                z = true;
            }
            return z;
        }
    }

    public boolean hasRequestedPermission(ArraySet<String> names) {
        synchronized (this.mLock) {
            if (this.mPermissions == null) {
                return false;
            }
            for (int i = names.size() - 1; i >= 0; i--) {
                if (this.mPermissions.get(names.valueAt(i)) != null) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean hasRequestedPermission(String name) {
        ArrayMap<String, PermissionData> arrayMap = this.mPermissions;
        return (arrayMap == null || arrayMap.get(name) == null) ? false : true;
    }

    public Set<String> getPermissions(int userId) {
        enforceValidUserId(userId);
        synchronized (this.mLock) {
            if (this.mPermissions == null) {
                return Collections.emptySet();
            }
            Set<String> permissions = new ArraySet<>(this.mPermissions.size());
            int permissionCount = this.mPermissions.size();
            for (int i = 0; i < permissionCount; i++) {
                String permission = this.mPermissions.keyAt(i);
                if (hasInstallPermission(permission)) {
                    permissions.add(permission);
                } else if (userId != -1 && hasRuntimePermission(permission, userId)) {
                    permissions.add(permission);
                }
            }
            return permissions;
        }
    }

    public PermissionState getInstallPermissionState(String name) {
        return getPermissionState(name, -1);
    }

    public PermissionState getRuntimePermissionState(String name, int userId) {
        enforceValidUserId(userId);
        return getPermissionState(name, userId);
    }

    public List<PermissionState> getInstallPermissionStates() {
        return getPermissionStatesInternal(-1);
    }

    public List<PermissionState> getRuntimePermissionStates(int userId) {
        enforceValidUserId(userId);
        return getPermissionStatesInternal(userId);
    }

    public int getPermissionFlags(String name, int userId) {
        PermissionState installPermState = getInstallPermissionState(name);
        if (installPermState != null) {
            return installPermState.getFlags();
        }
        PermissionState runtimePermState = getRuntimePermissionState(name, userId);
        if (runtimePermState != null) {
            return runtimePermState.getFlags();
        }
        return 0;
    }

    public boolean updatePermissionFlags(BasePermission permission, int userId, int flagMask, int flagValues) {
        PermissionData permissionData;
        enforceValidUserId(userId);
        boolean mayChangeFlags = (flagValues == 0 && flagMask == 0) ? false : true;
        synchronized (this.mLock) {
            if (this.mPermissions == null) {
                if (!mayChangeFlags) {
                    return false;
                }
                ensurePermissionData(permission);
            }
            synchronized (this.mLock) {
                permissionData = this.mPermissions.get(permission.getName());
            }
            if (permissionData == null) {
                if (!mayChangeFlags) {
                    return false;
                }
                permissionData = ensurePermissionData(permission);
            }
            int oldFlags = permissionData.getFlags(userId);
            boolean updated = permissionData.updateFlags(userId, flagMask, flagValues);
            if (updated) {
                int newFlags = permissionData.getFlags(userId);
                if ((oldFlags & 64) == 0 && (newFlags & 64) != 0) {
                    if (this.mPermissionReviewRequired == null) {
                        this.mPermissionReviewRequired = new SparseBooleanArray();
                    }
                    this.mPermissionReviewRequired.put(userId, true);
                } else if ((oldFlags & 64) != 0 && (newFlags & 64) == 0 && this.mPermissionReviewRequired != null && !hasPermissionRequiringReview(userId)) {
                    this.mPermissionReviewRequired.delete(userId);
                    if (this.mPermissionReviewRequired.size() <= 0) {
                        this.mPermissionReviewRequired = null;
                    }
                }
            }
            return updated;
        }
    }

    private boolean hasPermissionRequiringReview(int userId) {
        synchronized (this.mLock) {
            int permissionCount = this.mPermissions.size();
            for (int i = 0; i < permissionCount; i++) {
                PermissionData permission = this.mPermissions.valueAt(i);
                if ((permission.getFlags(userId) & 64) != 0) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean updatePermissionFlagsForAllPermissions(int userId, int flagMask, int flagValues) {
        enforceValidUserId(userId);
        synchronized (this.mLock) {
            if (this.mPermissions == null) {
                return false;
            }
            boolean changed = false;
            int permissionCount = this.mPermissions.size();
            for (int i = 0; i < permissionCount; i++) {
                PermissionData permissionData = this.mPermissions.valueAt(i);
                changed |= permissionData.updateFlags(userId, flagMask, flagValues);
            }
            return changed;
        }
    }

    public int[] computeGids(int userId) {
        enforceValidUserId(userId);
        int[] gids = this.mGlobalGids;
        synchronized (this.mLock) {
            if (this.mPermissions != null) {
                int permissionCount = this.mPermissions.size();
                for (int i = 0; i < permissionCount; i++) {
                    String permission = this.mPermissions.keyAt(i);
                    if (hasPermission(permission, userId)) {
                        PermissionData permissionData = this.mPermissions.valueAt(i);
                        int[] permGids = permissionData.computeGids(userId);
                        if (permGids != NO_GIDS) {
                            gids = appendInts(gids, permGids);
                        }
                    }
                }
            }
        }
        return gids;
    }

    public int[] computeGids(int[] userIds) {
        int[] gids = this.mGlobalGids;
        for (int userId : userIds) {
            int[] userGids = computeGids(userId);
            gids = appendInts(gids, userGids);
        }
        return gids;
    }

    public void reset() {
        this.mGlobalGids = NO_GIDS;
        synchronized (this.mLock) {
            this.mPermissions = null;
            invalidateCache();
        }
        this.mMissing = null;
        this.mPermissionReviewRequired = null;
    }

    private PermissionState getPermissionState(String name, int userId) {
        synchronized (this.mLock) {
            if (this.mPermissions == null) {
                return null;
            }
            PermissionData permissionData = this.mPermissions.get(name);
            if (permissionData == null) {
                return null;
            }
            return permissionData.getPermissionState(userId);
        }
    }

    private List<PermissionState> getPermissionStatesInternal(int userId) {
        enforceValidUserId(userId);
        synchronized (this.mLock) {
            if (this.mPermissions == null) {
                return Collections.emptyList();
            }
            List<PermissionState> permissionStates = new ArrayList<>();
            int permissionCount = this.mPermissions.size();
            for (int i = 0; i < permissionCount; i++) {
                PermissionData permissionData = this.mPermissions.valueAt(i);
                PermissionState permissionState = permissionData.getPermissionState(userId);
                if (permissionState != null) {
                    permissionStates.add(permissionState);
                }
            }
            return permissionStates;
        }
    }

    private int grantPermission(BasePermission permission, int userId) {
        if (hasPermission(permission.getName(), userId)) {
            return 0;
        }
        boolean hasGids = !ArrayUtils.isEmpty(permission.computeGids(userId));
        int[] oldGids = hasGids ? computeGids(userId) : NO_GIDS;
        PermissionData permissionData = ensurePermissionData(permission);
        if (!permissionData.grant(userId)) {
            return -1;
        }
        if (hasGids) {
            int[] newGids = computeGids(userId);
            if (oldGids.length != newGids.length) {
                return 1;
            }
        }
        return 0;
    }

    private int revokePermission(BasePermission permission, int userId) {
        PermissionData permissionData;
        String permName = permission.getName();
        if (hasPermission(permName, userId)) {
            boolean hasGids = !ArrayUtils.isEmpty(permission.computeGids(userId));
            int[] oldGids = hasGids ? computeGids(userId) : NO_GIDS;
            synchronized (this.mLock) {
                permissionData = this.mPermissions.get(permName);
            }
            if (!permissionData.revoke(userId)) {
                return -1;
            }
            if (permissionData.isDefault()) {
                ensureNoPermissionData(permName);
            }
            if (hasGids) {
                int[] newGids = computeGids(userId);
                if (oldGids.length != newGids.length) {
                    return 1;
                }
            }
            return 0;
        }
        return 0;
    }

    private static int[] appendInts(int[] current, int[] added) {
        if (current != null && added != null) {
            for (int guid : added) {
                current = ArrayUtils.appendInt(current, guid);
            }
        }
        return current;
    }

    private static void enforceValidUserId(int userId) {
        if (userId != -1 && userId < 0) {
            throw new IllegalArgumentException("Invalid userId:" + userId);
        }
    }

    private PermissionData ensurePermissionData(BasePermission permission) {
        PermissionData permissionData;
        String permName = permission.getName();
        synchronized (this.mLock) {
            if (this.mPermissions == null) {
                this.mPermissions = new ArrayMap<>();
            }
            permissionData = this.mPermissions.get(permName);
            if (permissionData == null) {
                permissionData = new PermissionData(permission);
                this.mPermissions.put(permName, permissionData);
            }
        }
        return permissionData;
    }

    private void ensureNoPermissionData(String name) {
        synchronized (this.mLock) {
            if (this.mPermissions == null) {
                return;
            }
            this.mPermissions.remove(name);
            if (this.mPermissions.isEmpty()) {
                this.mPermissions = null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class PermissionData {
        private final Object mLock;
        private final BasePermission mPerm;
        private SparseArray<PermissionState> mUserStates;

        public PermissionData(BasePermission perm) {
            this.mLock = new Object();
            this.mUserStates = new SparseArray<>();
            this.mPerm = perm;
        }

        public PermissionData(PermissionData other) {
            this(other.mPerm);
            synchronized (this.mLock) {
                int otherStateCount = other.mUserStates.size();
                for (int i = 0; i < otherStateCount; i++) {
                    int otherUserId = other.mUserStates.keyAt(i);
                    PermissionState otherState = other.mUserStates.valueAt(i);
                    this.mUserStates.put(otherUserId, new PermissionState(otherState));
                }
            }
        }

        public int[] computeGids(int userId) {
            return this.mPerm.computeGids(userId);
        }

        public boolean isGranted(int userId) {
            synchronized (this.mLock) {
                if (isInstallPermission()) {
                    userId = -1;
                }
                PermissionState userState = this.mUserStates.get(userId);
                if (userState == null) {
                    return false;
                }
                return userState.mGranted;
            }
        }

        public boolean grant(int userId) {
            synchronized (this.mLock) {
                if (isCompatibleUserId(userId)) {
                    if (isGranted(userId)) {
                        return false;
                    }
                    PermissionState userState = this.mUserStates.get(userId);
                    if (userState == null) {
                        userState = new PermissionState(this.mPerm.getName());
                        this.mUserStates.put(userId, userState);
                    }
                    userState.mGranted = true;
                    PermissionsState.invalidateCache();
                    return true;
                }
                return false;
            }
        }

        public boolean revoke(int userId) {
            synchronized (this.mLock) {
                if (isCompatibleUserId(userId)) {
                    if (isGranted(userId)) {
                        PermissionState userState = this.mUserStates.get(userId);
                        userState.mGranted = false;
                        if (userState.isDefault()) {
                            this.mUserStates.remove(userId);
                        }
                        PermissionsState.invalidateCache();
                        return true;
                    }
                    return false;
                }
                return false;
            }
        }

        public PermissionState getPermissionState(int userId) {
            PermissionState permissionState;
            synchronized (this.mLock) {
                permissionState = this.mUserStates.get(userId);
            }
            return permissionState;
        }

        public int getFlags(int userId) {
            synchronized (this.mLock) {
                PermissionState userState = this.mUserStates.get(userId);
                if (userState == null) {
                    return 0;
                }
                return userState.mFlags;
            }
        }

        public boolean isDefault() {
            boolean z;
            synchronized (this.mLock) {
                z = this.mUserStates.size() <= 0;
            }
            return z;
        }

        public static boolean isInstallPermissionKey(int userId) {
            return userId == -1;
        }

        public boolean updateFlags(int userId, int flagMask, int flagValues) {
            synchronized (this.mLock) {
                if (isInstallPermission()) {
                    userId = -1;
                }
                if (isCompatibleUserId(userId)) {
                    int newFlags = flagValues & flagMask;
                    PermissionsState.invalidateCache();
                    PermissionState userState = this.mUserStates.get(userId);
                    if (userState == null) {
                        if (newFlags != 0) {
                            PermissionState userState2 = new PermissionState(this.mPerm.getName());
                            userState2.mFlags = newFlags;
                            this.mUserStates.put(userId, userState2);
                            return true;
                        }
                        return false;
                    }
                    int oldFlags = userState.mFlags;
                    userState.mFlags = (userState.mFlags & (~flagMask)) | newFlags;
                    if (userState.isDefault()) {
                        this.mUserStates.remove(userId);
                    }
                    return userState.mFlags != oldFlags;
                }
                return false;
            }
        }

        private boolean isCompatibleUserId(int userId) {
            return isDefault() || !(isInstallPermission() ^ isInstallPermissionKey(userId));
        }

        private boolean isInstallPermission() {
            return this.mUserStates.size() == 1 && this.mUserStates.get(-1) != null;
        }
    }

    /* loaded from: classes.dex */
    public static final class PermissionState {
        private int mFlags;
        private boolean mGranted;
        private final String mName;

        public PermissionState(String name) {
            this.mName = name;
        }

        public PermissionState(PermissionState other) {
            this.mName = other.mName;
            this.mGranted = other.mGranted;
            this.mFlags = other.mFlags;
        }

        public boolean isDefault() {
            return !this.mGranted && this.mFlags == 0;
        }

        public String getName() {
            return this.mName;
        }

        public boolean isGranted() {
            return this.mGranted;
        }

        public int getFlags() {
            return this.mFlags;
        }
    }
}