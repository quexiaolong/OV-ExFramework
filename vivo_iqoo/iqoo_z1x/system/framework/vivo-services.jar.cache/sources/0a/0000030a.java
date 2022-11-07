package com.android.server.net;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.UidRange;
import android.os.Binder;
import android.os.Bundle;
import android.os.FtBuild;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.os.Trace;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.LocalLog;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.util.IndentingPrintWriter;
import com.vivo.common.utils.VLog;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import vivo.app.VivoFrameworkFactory;
import vivo.app.vgc.AbsVivoVgcManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoNetworkPolicyManagerServiceImpl implements IVivoNetworkPolicyManagerService {
    private static final int ALL_UID = -1;
    private static final int[] CLEAR_UID = {0};
    private static final int MAX_CLOSE_SOCKET_RECORD = 10;
    private static final int RANGE_UIDS = -100;
    private static final int RAW_CMD = -200;
    static final String TAG = "VivoNpmsImpl";
    private LocalLog mCloseSocketLog;
    private Context mContext;
    private NetworkPolicyManagerService mNpms;
    private AbsVivoVgcManager vivoVgcManager;
    final Object mUidRulesSecondLock = new Object();
    private final ArrayMap<Integer, NetworkFirewallInfo> mMobileUidsFirewall = new ArrayMap<>();
    private final ArrayMap<Integer, NetworkFirewallInfo> mWifiUidsFirewall = new ArrayMap<>();
    private final ArrayMap<Integer, String> mUserChainFirewall = new ArrayMap<>();

    public VivoNetworkPolicyManagerServiceImpl(NetworkPolicyManagerService npms, Context c) {
        this.vivoVgcManager = null;
        if (npms == null) {
            VSlog.i(TAG, "container is " + npms);
        }
        this.mNpms = npms;
        this.mContext = c;
        this.mCloseSocketLog = new LocalLog(10);
        if (VivoFrameworkFactory.getFrameworkFactoryImpl() != null) {
            this.vivoVgcManager = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoVgcManager();
        }
    }

    public void showOrCancelNotify(boolean restrictBackground) {
        VSlog.d(TAG, "showOrCancelNotify,restrictBackground = " + restrictBackground);
        if (FtBuild.getTierLevel() == 1) {
            return;
        }
        this.mContext.getPackageName();
        try {
            if (!restrictBackground) {
                ((NotificationManager) this.mContext.getSystemService(NotificationManager.class)).cancelAsUser(null, 10210, UserHandle.ALL);
                ((NotificationManager) this.mContext.getSystemService(NotificationManager.class)).cancelAsUser(null, 10211, UserHandle.ALL);
            } else {
                try {
                    Notification.Builder builder1 = new Notification.Builder(this.mContext, SystemNotificationChannels.NETWORK_STATUS);
                    Notification.Builder builder2 = new Notification.Builder(this.mContext, SystemNotificationChannels.NETWORK_STATUS);
                    String notificationTitle = this.mContext.getString(51249533);
                    String notificationText = this.mContext.getString(51249535);
                    Intent intent = new Intent("vivo.intent.action.RESTRICTEDDATAACCESS");
                    PendingIntent pendingIntent = PendingIntent.getActivity(this.mContext, 0, intent, 0);
                    Bundle bundle = new Bundle();
                    bundle.putInt("vivo.summaryIconRes", 50463081);
                    Notification group = builder1.setSmallIcon(50463068).setContentTitle(notificationTitle).setGroup("restrict_data_mode").setOngoing(true).setExtras(bundle).setGroupSummary(true).build();
                    Notification notification = builder2.setSmallIcon(50463068).setContentTitle(notificationTitle).setContentText(notificationText).setContentIntent(pendingIntent).setOngoing(true).setExtras(bundle).setGroup("restrict_data_mode").build();
                    ((NotificationManager) this.mContext.getSystemService(NotificationManager.class)).notifyAsUser(null, 10211, group, UserHandle.ALL);
                    ((NotificationManager) this.mContext.getSystemService(NotificationManager.class)).notifyAsUser(null, 10210, notification, UserHandle.ALL);
                } catch (RuntimeException e) {
                    e = e;
                    VSlog.d(TAG, "showOrCancelNotify,e = " + e);
                }
            }
        } catch (RuntimeException e2) {
            e = e2;
        }
    }

    /* loaded from: classes.dex */
    private static class NetworkFirewallInfo {
        int mCallingPid;
        int mCallingUid;
        String mTime;
        int mUid;
        private static final SimpleDateFormat sFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss:SSS");
        private static final Date sDate = new Date();

        public NetworkFirewallInfo(int callingPid, int callingUid, int uid) {
            this.mCallingPid = callingPid;
            this.mCallingUid = callingUid;
            this.mUid = uid;
            sDate.setTime(System.currentTimeMillis());
            this.mTime = sFormatter.format(sDate);
        }

        public String toString() {
            return "mCallingPid/mCallingUid: " + this.mCallingPid + "/" + this.mCallingUid + ", mUid/mTime: " + this.mUid + "/" + this.mTime;
        }
    }

    public boolean setMobileUidFirewall(List uids, INetworkManagementService nms) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        synchronized (this.mUidRulesSecondLock) {
            if (uids == null) {
                return false;
            }
            try {
                try {
                    int callingPid = Binder.getCallingPid();
                    int callingUid = Binder.getCallingUid();
                    long identity = Binder.clearCallingIdentity();
                    ArrayList firewallUids = (ArrayList) uids;
                    int size = firewallUids.size();
                    if (size == 0) {
                        boolean result = nms.setMobileUidFirewall(CLEAR_UID);
                        if (result) {
                            this.mMobileUidsFirewall.clear();
                        }
                        if (identity != -1) {
                            Binder.restoreCallingIdentity(identity);
                        }
                        Trace.traceEnd(2097152L);
                        return result;
                    }
                    int[] uidArray = new int[size];
                    ArrayMap<Integer, NetworkFirewallInfo> newInfos = new ArrayMap<>();
                    int i = 0;
                    while (i < size) {
                        int uid = ((Integer) firewallUids.get(i)).intValue();
                        if (!UserHandle.isApp(uid)) {
                            throw new IllegalArgumentException("cannot apply policy to UID " + uid);
                        }
                        uidArray[i] = uid;
                        ArrayMap<Integer, NetworkFirewallInfo> newInfos2 = newInfos;
                        newInfos2.put(Integer.valueOf(uid), new NetworkFirewallInfo(callingPid, callingUid, uid));
                        i++;
                        newInfos = newInfos2;
                    }
                    ArrayMap<Integer, NetworkFirewallInfo> newInfos3 = newInfos;
                    if (Trace.isTagEnabled(2097152L)) {
                        Trace.traceBegin(2097152L, "setMobileUidFirewall");
                    }
                    boolean result2 = nms.setMobileUidFirewall(uidArray);
                    if (result2) {
                        this.mMobileUidsFirewall.clear();
                        this.mMobileUidsFirewall.putAll((ArrayMap<? extends Integer, ? extends NetworkFirewallInfo>) newInfos3);
                    } else {
                        VSlog.e(TAG, "setMobileUidFirewall failed");
                    }
                    if (identity != -1) {
                        Binder.restoreCallingIdentity(identity);
                    }
                    Trace.traceEnd(2097152L);
                    return result2;
                } catch (ClassCastException e) {
                    VLog.e(TAG, "e = " + e);
                    throw new IllegalArgumentException("Argument error");
                } catch (IllegalStateException e2) {
                    VLog.wtf(TAG, "problem setting wifi uid rules", e2);
                    if (-1 != -1) {
                        Binder.restoreCallingIdentity(-1L);
                    }
                    Trace.traceEnd(2097152L);
                    return false;
                } catch (NumberFormatException e3) {
                    throw new IllegalArgumentException("Argument error");
                }
            } catch (RemoteException e4) {
                if (-1 != -1) {
                    Binder.restoreCallingIdentity(-1L);
                }
                Trace.traceEnd(2097152L);
                return false;
            }
        }
    }

    public boolean setWifiUidFirewall(List uids, INetworkManagementService nms) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
        synchronized (this.mUidRulesSecondLock) {
            if (uids == null) {
                return false;
            }
            try {
                try {
                    int callingPid = Binder.getCallingPid();
                    int callingUid = Binder.getCallingUid();
                    long identity = Binder.clearCallingIdentity();
                    ArrayList firewallUids = (ArrayList) uids;
                    int size = firewallUids.size();
                    if (size == 0) {
                        boolean result = nms.setWifiUidFirewall(CLEAR_UID);
                        if (result) {
                            this.mWifiUidsFirewall.clear();
                        }
                        if (identity != -1) {
                            Binder.restoreCallingIdentity(identity);
                        }
                        Trace.traceEnd(2097152L);
                        return result;
                    }
                    int[] uidArray = new int[size];
                    ArrayMap<Integer, NetworkFirewallInfo> newInfos = new ArrayMap<>();
                    int i = 0;
                    while (i < size) {
                        int uid = ((Integer) firewallUids.get(i)).intValue();
                        if (!UserHandle.isApp(uid)) {
                            throw new IllegalArgumentException("cannot apply policy to UID " + uid);
                        }
                        uidArray[i] = uid;
                        ArrayMap<Integer, NetworkFirewallInfo> newInfos2 = newInfos;
                        newInfos2.put(Integer.valueOf(uid), new NetworkFirewallInfo(callingPid, callingUid, uid));
                        i++;
                        newInfos = newInfos2;
                    }
                    ArrayMap<Integer, NetworkFirewallInfo> newInfos3 = newInfos;
                    if (Trace.isTagEnabled(2097152L)) {
                        Trace.traceBegin(2097152L, "setWifiUidFirewall");
                    }
                    boolean result2 = nms.setWifiUidFirewall(uidArray);
                    if (result2) {
                        this.mWifiUidsFirewall.clear();
                        this.mWifiUidsFirewall.putAll((ArrayMap<? extends Integer, ? extends NetworkFirewallInfo>) newInfos3);
                    } else {
                        VSlog.e(TAG, "setWifiUidFirewall failed");
                    }
                    if (identity != -1) {
                        Binder.restoreCallingIdentity(identity);
                    }
                    Trace.traceEnd(2097152L);
                    return result2;
                } catch (ClassCastException e) {
                    VLog.e(TAG, "e = " + e);
                    throw new IllegalArgumentException("Argument error");
                } catch (IllegalStateException e2) {
                    VLog.wtf(TAG, "problem setting wifi uid rules", e2);
                    if (-1 != -1) {
                        Binder.restoreCallingIdentity(-1L);
                    }
                    Trace.traceEnd(2097152L);
                    return false;
                } catch (NumberFormatException e3) {
                    throw new IllegalArgumentException("Argument error");
                }
            } catch (RemoteException e4) {
                if (-1 != -1) {
                    Binder.restoreCallingIdentity(-1L);
                }
                Trace.traceEnd(2097152L);
                return false;
            }
        }
    }

    private void setMobileUidRule(int uid, boolean allow, INetworkManagementService nms) {
        synchronized (this.mUidRulesSecondLock) {
            if (Trace.isTagEnabled(2097152L)) {
                Trace.traceBegin(2097152L, "setMobileUidRule: " + uid + "/" + allow);
            }
            try {
                nms.setMobileUidRule(uid, allow);
            } catch (RemoteException e) {
            } catch (IllegalStateException e2) {
                VLog.wtf(TAG, "problem setting mobile uid rules", e2);
            }
            Trace.traceEnd(2097152L);
        }
    }

    private void setWifiUidRule(int uid, boolean allow, INetworkManagementService nms) {
        synchronized (this.mUidRulesSecondLock) {
            if (Trace.isTagEnabled(2097152L)) {
                Trace.traceBegin(2097152L, "setWifiUidRule: " + uid + "/" + allow);
            }
            try {
                nms.setWifiUidRule(uid, allow);
            } catch (RemoteException e) {
            } catch (IllegalStateException e2) {
                VLog.wtf(TAG, "problem setting wifi uid rules", e2);
            }
            Trace.traceEnd(2097152L);
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:65:0x00f1 A[Catch: all -> 0x0134, TRY_ENTER, TryCatch #8 {, blocks: (B:5:0x000e, B:7:0x0014, B:8:0x0030, B:81:0x011f, B:82:0x0122, B:83:0x0129, B:65:0x00f1, B:66:0x00f4, B:67:0x00fa, B:74:0x010d, B:75:0x0110, B:76:0x0116, B:37:0x00b7, B:38:0x00ba, B:39:0x00c0, B:84:0x012a, B:85:0x0133), top: B:89:0x000e }] */
    /* JADX WARN: Removed duplicated region for block: B:74:0x010d A[Catch: all -> 0x0134, TRY_ENTER, TryCatch #8 {, blocks: (B:5:0x000e, B:7:0x0014, B:8:0x0030, B:81:0x011f, B:82:0x0122, B:83:0x0129, B:65:0x00f1, B:66:0x00f4, B:67:0x00fa, B:74:0x010d, B:75:0x0110, B:76:0x0116, B:37:0x00b7, B:38:0x00ba, B:39:0x00c0, B:84:0x012a, B:85:0x0133), top: B:89:0x000e }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public boolean setUidFirewallForUserChain(int r20, java.lang.String r21, java.util.List r22, android.os.INetworkManagementService r23) {
        /*
            Method dump skipped, instructions count: 311
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.net.VivoNetworkPolicyManagerServiceImpl.setUidFirewallForUserChain(int, java.lang.String, java.util.List, android.os.INetworkManagementService):boolean");
    }

    public void onUidDeletedUL(int uid) {
        synchronized (this.mUidRulesSecondLock) {
            this.mMobileUidsFirewall.remove(Integer.valueOf(uid));
            this.mWifiUidsFirewall.remove(Integer.valueOf(uid));
        }
    }

    public void resetUidFirewallRules(int uid, INetworkManagementService nms) {
        synchronized (this.mUidRulesSecondLock) {
            if (this.mMobileUidsFirewall.get(Integer.valueOf(uid)) == null) {
                setMobileUidRule(uid, true, nms);
            }
            if (this.mWifiUidsFirewall.get(Integer.valueOf(uid)) == null) {
                setWifiUidRule(uid, true, nms);
            }
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:61:0x0182, code lost:
        if (r3 == (-1)) goto L38;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public void closeSockets(java.util.List r19, java.util.List r20, android.os.INetworkManagementService r21) {
        /*
            Method dump skipped, instructions count: 408
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.net.VivoNetworkPolicyManagerServiceImpl.closeSockets(java.util.List, java.util.List, android.os.INetworkManagementService):void");
    }

    private UidRange makeUidRange(int start, int stop) {
        return new UidRange(start, stop);
    }

    public boolean isAllowUnPrivilegedAppAddAsDefault() {
        boolean allowed = false;
        AbsVivoVgcManager absVivoVgcManager = this.vivoVgcManager;
        if (absVivoVgcManager != null && absVivoVgcManager.isVgcActivated()) {
            allowed = this.vivoVgcManager.getBool("datausage_unprivileged_add_to_default", false);
        }
        VSlog.d(TAG, "isAllowUnPrivilegedAppAddAsDefault = " + allowed);
        return allowed;
    }

    public void dump(IndentingPrintWriter fout) {
        synchronized (this.mUidRulesSecondLock) {
            fout.println("Mobile firewall of uids:");
            fout.increaseIndent();
            int size = this.mMobileUidsFirewall.size();
            for (int i = 0; i < size; i++) {
                fout.println(this.mMobileUidsFirewall.valueAt(i));
            }
            fout.decreaseIndent();
            fout.println("Wifi firewall of uids:");
            fout.increaseIndent();
            int size2 = this.mWifiUidsFirewall.size();
            for (int i2 = 0; i2 < size2; i2++) {
                fout.println(this.mWifiUidsFirewall.valueAt(i2));
            }
            fout.decreaseIndent();
            fout.println("User chain firewall:");
            fout.increaseIndent();
            int size3 = this.mUserChainFirewall.size();
            for (int i3 = 0; i3 < size3; i3++) {
                fout.println(this.mUserChainFirewall.valueAt(i3) + ":" + this.mUserChainFirewall.keyAt(i3));
            }
            fout.decreaseIndent();
            this.mCloseSocketLog.dump(fout);
        }
    }

    public void dummy() {
    }
}