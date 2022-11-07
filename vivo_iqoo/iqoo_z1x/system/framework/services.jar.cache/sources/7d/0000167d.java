package com.android.server.pm;

import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserManager;
import com.android.internal.util.Preconditions;
import java.io.PrintWriter;

/* loaded from: classes.dex */
public final class UserTypeDetails {
    public static final int UNLIMITED_NUMBER_OF_USERS = -1;
    private final int[] mBadgeColors;
    private final int[] mBadgeLabels;
    private final int mBadgeNoBackground;
    private final int mBadgePlain;
    private final int mBaseType;
    private final int[] mDarkThemeBadgeColors;
    private final Bundle mDefaultRestrictions;
    private final int mDefaultUserInfoPropertyFlags;
    private final boolean mEnabled;
    private final int mIconBadge;
    private final int mLabel;
    private final int mMaxAllowed;
    private final int mMaxAllowedPerParent;
    private final String mName;

    private UserTypeDetails(String name, boolean enabled, int maxAllowed, int baseType, int defaultUserInfoPropertyFlags, int label, int maxAllowedPerParent, int iconBadge, int badgePlain, int badgeNoBackground, int[] badgeLabels, int[] badgeColors, int[] darkThemeBadgeColors, Bundle defaultRestrictions) {
        this.mName = name;
        this.mEnabled = enabled;
        this.mMaxAllowed = maxAllowed;
        this.mMaxAllowedPerParent = maxAllowedPerParent;
        this.mBaseType = baseType;
        this.mDefaultUserInfoPropertyFlags = defaultUserInfoPropertyFlags;
        this.mDefaultRestrictions = defaultRestrictions;
        this.mIconBadge = iconBadge;
        this.mBadgePlain = badgePlain;
        this.mBadgeNoBackground = badgeNoBackground;
        this.mLabel = label;
        this.mBadgeLabels = badgeLabels;
        this.mBadgeColors = badgeColors;
        this.mDarkThemeBadgeColors = darkThemeBadgeColors;
    }

    public String getName() {
        return this.mName;
    }

    public boolean isEnabled() {
        return this.mEnabled;
    }

    public int getMaxAllowed() {
        return this.mMaxAllowed;
    }

    public int getMaxAllowedPerParent() {
        return this.mMaxAllowedPerParent;
    }

    public int getDefaultUserInfoFlags() {
        return this.mDefaultUserInfoPropertyFlags | this.mBaseType;
    }

    public int getLabel() {
        return this.mLabel;
    }

    public boolean hasBadge() {
        return this.mIconBadge != 0;
    }

    public int getIconBadge() {
        return this.mIconBadge;
    }

    public int getBadgePlain() {
        return this.mBadgePlain;
    }

    public int getBadgeNoBackground() {
        return this.mBadgeNoBackground;
    }

    public int getBadgeLabel(int badgeIndex) {
        int[] iArr = this.mBadgeLabels;
        if (iArr == null || iArr.length == 0 || badgeIndex < 0) {
            return 0;
        }
        return iArr[Math.min(badgeIndex, iArr.length - 1)];
    }

    public int getBadgeColor(int badgeIndex) {
        int[] iArr = this.mBadgeColors;
        if (iArr == null || iArr.length == 0 || badgeIndex < 0) {
            return 0;
        }
        return iArr[Math.min(badgeIndex, iArr.length - 1)];
    }

    public int getDarkThemeBadgeColor(int badgeIndex) {
        int[] iArr = this.mDarkThemeBadgeColors;
        if (iArr == null || iArr.length == 0 || badgeIndex < 0) {
            return getBadgeColor(badgeIndex);
        }
        return iArr[Math.min(badgeIndex, iArr.length - 1)];
    }

    public boolean isProfile() {
        return (this.mBaseType & 4096) != 0;
    }

    public boolean isFull() {
        return (this.mBaseType & 1024) != 0;
    }

    public boolean isSystem() {
        return (this.mBaseType & 2048) != 0;
    }

    Bundle getDefaultRestrictions() {
        return UserRestrictionsUtils.clone(this.mDefaultRestrictions);
    }

    public void addDefaultRestrictionsTo(Bundle currentRestrictions) {
        UserRestrictionsUtils.merge(currentRestrictions, this.mDefaultRestrictions);
    }

    public void dump(PrintWriter pw) {
        pw.print("        ");
        pw.print("mName: ");
        pw.println(this.mName);
        pw.print("        ");
        pw.print("mBaseType: ");
        pw.println(UserInfo.flagsToString(this.mBaseType));
        pw.print("        ");
        pw.print("mEnabled: ");
        pw.println(this.mEnabled);
        pw.print("        ");
        pw.print("mMaxAllowed: ");
        pw.println(this.mMaxAllowed);
        pw.print("        ");
        pw.print("mMaxAllowedPerParent: ");
        pw.println(this.mMaxAllowedPerParent);
        pw.print("        ");
        pw.print("mDefaultUserInfoFlags: ");
        pw.println(UserInfo.flagsToString(this.mDefaultUserInfoPropertyFlags));
        pw.print("        ");
        pw.print("mLabel: ");
        pw.println(this.mLabel);
        if (isSystem()) {
            pw.print("        ");
            pw.println("config_defaultFirstUserRestrictions: ");
            try {
                Bundle restrictions = new Bundle();
                String[] defaultFirstUserRestrictions = Resources.getSystem().getStringArray(17236005);
                for (String userRestriction : defaultFirstUserRestrictions) {
                    if (UserRestrictionsUtils.isValidRestriction(userRestriction)) {
                        restrictions.putBoolean(userRestriction, true);
                    }
                }
                UserRestrictionsUtils.dumpRestrictions(pw, "            ", restrictions);
            } catch (Resources.NotFoundException e) {
                pw.print("        ");
                pw.println("    none - resource not found");
            }
        } else {
            pw.print("        ");
            pw.println("mDefaultRestrictions: ");
            UserRestrictionsUtils.dumpRestrictions(pw, "            ", this.mDefaultRestrictions);
        }
        pw.print("        ");
        pw.print("mIconBadge: ");
        pw.println(this.mIconBadge);
        pw.print("        ");
        pw.print("mBadgePlain: ");
        pw.println(this.mBadgePlain);
        pw.print("        ");
        pw.print("mBadgeNoBackground: ");
        pw.println(this.mBadgeNoBackground);
        pw.print("        ");
        pw.print("mBadgeLabels.length: ");
        int[] iArr = this.mBadgeLabels;
        pw.println(iArr != null ? Integer.valueOf(iArr.length) : "0(null)");
        pw.print("        ");
        pw.print("mBadgeColors.length: ");
        int[] iArr2 = this.mBadgeColors;
        pw.println(iArr2 != null ? Integer.valueOf(iArr2.length) : "0(null)");
        pw.print("        ");
        pw.print("mDarkThemeBadgeColors.length: ");
        int[] iArr3 = this.mDarkThemeBadgeColors;
        pw.println(iArr3 != null ? Integer.valueOf(iArr3.length) : "0(null)");
    }

    /* loaded from: classes.dex */
    public static final class Builder {
        private int mBaseType;
        private String mName;
        private int mMaxAllowed = -1;
        private int mMaxAllowedPerParent = -1;
        private int mDefaultUserInfoPropertyFlags = 0;
        private Bundle mDefaultRestrictions = null;
        private boolean mEnabled = true;
        private int mLabel = 0;
        private int[] mBadgeLabels = null;
        private int[] mBadgeColors = null;
        private int[] mDarkThemeBadgeColors = null;
        private int mIconBadge = 0;
        private int mBadgePlain = 0;
        private int mBadgeNoBackground = 0;

        public Builder setName(String name) {
            this.mName = name;
            return this;
        }

        public Builder setEnabled(boolean enabled) {
            this.mEnabled = enabled;
            return this;
        }

        public Builder setMaxAllowed(int maxAllowed) {
            this.mMaxAllowed = maxAllowed;
            return this;
        }

        public Builder setMaxAllowedPerParent(int maxAllowedPerParent) {
            this.mMaxAllowedPerParent = maxAllowedPerParent;
            return this;
        }

        public Builder setBaseType(int baseType) {
            this.mBaseType = baseType;
            return this;
        }

        public Builder setDefaultUserInfoPropertyFlags(int flags) {
            this.mDefaultUserInfoPropertyFlags = flags;
            return this;
        }

        public Builder setBadgeLabels(int... badgeLabels) {
            this.mBadgeLabels = badgeLabels;
            return this;
        }

        public Builder setBadgeColors(int... badgeColors) {
            this.mBadgeColors = badgeColors;
            return this;
        }

        public Builder setDarkThemeBadgeColors(int... darkThemeBadgeColors) {
            this.mDarkThemeBadgeColors = darkThemeBadgeColors;
            return this;
        }

        public Builder setIconBadge(int badgeIcon) {
            this.mIconBadge = badgeIcon;
            return this;
        }

        public Builder setBadgePlain(int badgePlain) {
            this.mBadgePlain = badgePlain;
            return this;
        }

        public Builder setBadgeNoBackground(int badgeNoBackground) {
            this.mBadgeNoBackground = badgeNoBackground;
            return this;
        }

        public Builder setLabel(int label) {
            this.mLabel = label;
            return this;
        }

        public Builder setDefaultRestrictions(Bundle restrictions) {
            this.mDefaultRestrictions = restrictions;
            return this;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public int getBaseType() {
            return this.mBaseType;
        }

        public UserTypeDetails createUserTypeDetails() {
            boolean z = true;
            Preconditions.checkArgument(this.mName != null, "Cannot create a UserTypeDetails with no name.");
            Preconditions.checkArgument(hasValidBaseType(), "UserTypeDetails " + this.mName + " has invalid baseType: " + this.mBaseType);
            Preconditions.checkArgument(hasValidPropertyFlags(), "UserTypeDetails " + this.mName + " has invalid flags: " + Integer.toHexString(this.mDefaultUserInfoPropertyFlags));
            if (hasBadge()) {
                int[] iArr = this.mBadgeLabels;
                Preconditions.checkArgument((iArr == null || iArr.length == 0) ? false : true, "UserTypeDetails " + this.mName + " has badge but no badgeLabels.");
                int[] iArr2 = this.mBadgeColors;
                if (iArr2 == null || iArr2.length == 0) {
                    z = false;
                }
                Preconditions.checkArgument(z, "UserTypeDetails " + this.mName + " has badge but no badgeColors.");
            }
            String str = this.mName;
            boolean z2 = this.mEnabled;
            int i = this.mMaxAllowed;
            int i2 = this.mBaseType;
            int i3 = this.mDefaultUserInfoPropertyFlags;
            int i4 = this.mLabel;
            int i5 = this.mMaxAllowedPerParent;
            int i6 = this.mIconBadge;
            int i7 = this.mBadgePlain;
            int i8 = this.mBadgeNoBackground;
            int[] iArr3 = this.mBadgeLabels;
            int[] iArr4 = this.mBadgeColors;
            int[] iArr5 = this.mDarkThemeBadgeColors;
            return new UserTypeDetails(str, z2, i, i2, i3, i4, i5, i6, i7, i8, iArr3, iArr4, iArr5 == null ? iArr4 : iArr5, this.mDefaultRestrictions);
        }

        private boolean hasBadge() {
            return this.mIconBadge != 0;
        }

        private boolean hasValidBaseType() {
            int i = this.mBaseType;
            return i == 1024 || i == 4096 || i == 2048 || i == 3072;
        }

        private boolean hasValidPropertyFlags() {
            return (this.mDefaultUserInfoPropertyFlags & 7315) == 0;
        }
    }

    public boolean isManagedProfile() {
        return UserManager.isUserTypeManagedProfile(this.mName);
    }
}