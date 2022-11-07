package com.android.server.am;

import android.content.ComponentName;

/* loaded from: classes.dex */
public final class HostingRecord {
    private static final int APP_ZYGOTE = 2;
    private static final int REGULAR_ZYGOTE = 0;
    private static final int WEBVIEW_ZYGOTE = 1;
    private final String mDefiningPackageName;
    private final int mDefiningUid;
    private final String mHostingName;
    private final String mHostingType;
    private final int mHostingZygote;
    private final boolean mIsTopApp;

    public HostingRecord(String hostingType) {
        this(hostingType, null, 0, null, -1, false);
    }

    public HostingRecord(String hostingType, ComponentName hostingName) {
        this(hostingType, hostingName, 0);
    }

    public HostingRecord(String hostingType, ComponentName hostingName, boolean isTopApp) {
        this(hostingType, hostingName.toShortString(), 0, null, -1, isTopApp);
    }

    public HostingRecord(String hostingType, String hostingName) {
        this(hostingType, hostingName, 0);
    }

    public HostingRecord(String hostingType, String hostingName, boolean isTopApp) {
        this(hostingType, hostingName, 0, null, -1, isTopApp);
    }

    private HostingRecord(String hostingType, ComponentName hostingName, int hostingZygote) {
        this(hostingType, hostingName.toShortString(), hostingZygote);
    }

    private HostingRecord(String hostingType, String hostingName, int hostingZygote) {
        this(hostingType, hostingName, hostingZygote, null, -1, false);
    }

    private HostingRecord(String hostingType, String hostingName, int hostingZygote, String definingPackageName, int definingUid, boolean isTopApp) {
        this.mHostingType = hostingType;
        this.mHostingName = hostingName;
        this.mHostingZygote = hostingZygote;
        this.mDefiningPackageName = definingPackageName;
        this.mDefiningUid = definingUid;
        this.mIsTopApp = isTopApp;
    }

    public String getType() {
        return this.mHostingType;
    }

    public String getName() {
        return this.mHostingName;
    }

    public boolean isTopApp() {
        return this.mIsTopApp;
    }

    public int getDefiningUid() {
        return this.mDefiningUid;
    }

    public String getDefiningPackageName() {
        return this.mDefiningPackageName;
    }

    public static HostingRecord byWebviewZygote(ComponentName hostingName) {
        return new HostingRecord("", hostingName.toShortString(), 1);
    }

    public static HostingRecord byAppZygote(ComponentName hostingName, String definingPackageName, int definingUid) {
        return new HostingRecord("", hostingName.toShortString(), 2, definingPackageName, definingUid, false);
    }

    public boolean usesAppZygote() {
        return this.mHostingZygote == 2;
    }

    public boolean usesWebviewZygote() {
        return this.mHostingZygote == 1;
    }
}