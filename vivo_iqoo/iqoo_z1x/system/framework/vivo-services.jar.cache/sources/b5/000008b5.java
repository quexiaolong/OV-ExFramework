package com.vivo.services.vgc.cbs;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerInternal;
import android.os.Binder;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import com.android.internal.telephony.ITelephony;
import com.android.server.LocalServices;
import com.vivo.face.common.data.Constants;
import com.vivo.framework.vgc.VivoCbsManager;
import java.util.ArrayList;
import java.util.Map;
import vivo.app.vgc.IVgcCallback;
import vivo.util.VSlog;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class MiscHelper {
    public static final String COTA_MAP_KEY = "map_key";
    public static final String DEFAULT_MAPKEY = "0";
    private static final String TAG = CbsUtils.TAG;
    private final String ACTION_START_COTA = "com.vivo.cota.START_COTA_SERVICE";
    private final String PACKAGE_NAME_COTA = "com.vivo.cota";
    private final VivoCbsServiceImpl mCbsService;
    private Context mContext;
    private IVgcCallback mCotaInstallCallback;
    private int mCotaInstallPkgTotalNum;
    private ArrayMap<String, Bundle> mCotaInstallResult;
    private boolean mCotaInstallWithReplace;
    private int mCotaInstalledPkgNum;
    private boolean mCotaWithReboot;
    private boolean mPMSInstalling;
    private int mStartCotaCount;
    private int mStartCotaPhoneId;

    /* JADX INFO: Access modifiers changed from: package-private */
    public MiscHelper(Context context, VivoCbsServiceImpl cbsService) {
        this.mContext = context;
        this.mCbsService = cbsService;
        if (cbsService.getCurrentState() == 4) {
            this.mCbsService.setCurrentState(1);
        } else if (this.mCbsService.getCurrentState() == 6) {
            this.mCbsService.setCurrentState(7);
        }
        this.mCotaInstallPkgTotalNum = 0;
        this.mCotaInstallWithReplace = false;
        this.mPMSInstalling = false;
    }

    private Intent getCotaIntent() {
        Intent intent = new Intent();
        intent.setAction("com.vivo.cota.START_COTA_SERVICE");
        intent.setPackage("com.vivo.cota");
        return intent;
    }

    public void startCotaService() {
        int defDataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        this.mStartCotaCount = 1;
        this.mStartCotaPhoneId = SubscriptionManager.getPhoneId(defDataSubId);
        startCotaServiceImpl();
    }

    private void startCotaServiceByNextPhoneIdOrNot() {
        boolean start = true;
        this.mStartCotaCount++;
        int nextPhoneId = CbsUtils.getNextPhoneId(this.mStartCotaPhoneId);
        this.mStartCotaPhoneId = nextPhoneId;
        int subId = getSubId(this.mContext, nextPhoneId);
        start = (!SubscriptionManager.isValidSubscriptionId(subId) || this.mStartCotaCount > TelephonyManager.getDefault().getSimCount()) ? false : false;
        if (start) {
            startCotaServiceImpl();
        }
    }

    private void startCotaServiceImpl() {
        VSlog.d(TAG, "startCotaServiceImpl mStartCotaCount=" + this.mStartCotaCount + ", mStartCotaPhoneId=" + this.mStartCotaPhoneId);
        CbsSimInfo mapSimInfo = new CbsSimInfo();
        if (this.mCbsService.getCurrentState() >= 5 || this.mStartCotaCount > TelephonyManager.getDefault().getSimCount()) {
            return;
        }
        for (int i = this.mStartCotaCount; i <= TelephonyManager.getDefault().getSimCount(); i++) {
            int subId = getSubId(this.mContext, this.mStartCotaPhoneId);
            CbsSimInfo currSimInfo = getCbsSimInfo(this.mContext, subId, 1);
            mapSimInfo = this.mCbsService.getMapSimInfo(currSimInfo);
            if (CbsUtils.DEBUG) {
                VSlog.d(TAG, "mStartCotaCount=" + this.mStartCotaCount + ", mStartCotaPhoneId=" + this.mStartCotaPhoneId + ", subId=" + subId + ", mapkey=" + mapSimInfo.getMapKey());
            }
            if (mapSimInfo != null && !TextUtils.isEmpty(mapSimInfo.getMapKey())) {
                break;
            }
            this.mStartCotaPhoneId = CbsUtils.getNextPhoneId(this.mStartCotaPhoneId);
            this.mStartCotaCount++;
        }
        String mapkey = TextUtils.isEmpty(mapSimInfo.getMapKey()) ? "0" : mapSimInfo.getMapKey();
        Intent intent = getCotaIntent();
        intent.putExtra("map_key", mapkey);
        this.mCbsService.updateSimInfo(mapSimInfo);
        this.mCbsService.setCurrentState(4);
        if (CbsUtils.DEBUG) {
            VSlog.d(TAG, "startCotaService with mapkey=" + mapkey);
        }
        try {
            this.mContext.startServiceAsUser(intent, UserHandle.OWNER);
        } catch (Throwable e) {
            if (CbsUtils.DEBUG) {
                VSlog.e(TAG, "startServiceAsUser fail");
            }
            e.printStackTrace();
        }
    }

    public synchronized void handleCotaSetParam(Bundle bundle) {
        String invokerPkg = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
        if (bundle != null && bundle.containsKey("COTA_STATE") && "com.vivo.cota".equals(invokerPkg)) {
            int defDataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
            SubscriptionManager.getPhoneId(defDataSubId);
            String cotaState = bundle.getString("COTA_STATE", "COTA_STATE_NO_RESOURCE");
            char c = 65535;
            int hashCode = cotaState.hashCode();
            if (hashCode != -732460184) {
                if (hashCode != -691500185) {
                    if (hashCode == 1617689368 && cotaState.equals("COTA_STATE_NO_RESOURCE")) {
                        c = 1;
                    }
                } else if (cotaState.equals("COTA_STATE_HAS_RESOURCE")) {
                    c = 0;
                }
            } else if (cotaState.equals("COTA_STATE_DOWNLOAD_COMPLETED")) {
                c = 2;
            }
            if (c == 0) {
                this.mCbsService.writeUpdatedSettings(false);
                this.mCbsService.setCurrentState(5);
            } else if (c == 1) {
                startCotaServiceByNextPhoneIdOrNot();
                this.mPMSInstalling = false;
            } else if (c == 2) {
                if (this.mPMSInstalling) {
                    VSlog.w(TAG, "PMS installing ignore this operate");
                    return;
                }
                this.mCbsService.setCurrentState(7);
                this.mCbsService.vgcUpdate();
                this.mCotaWithReboot = bundle.getBoolean("reboot", false);
                this.mCotaInstallWithReplace = bundle.getBoolean("replace", false);
                this.mCotaInstallPkgTotalNum = bundle.getInt("pkg_number", 0);
                this.mCotaInstallCallback = IVgcCallback.Stub.asInterface(bundle.getBinder("callback_obj"));
                this.mCotaInstallResult = new ArrayMap<>();
                if (this.mCotaInstallPkgTotalNum == 0) {
                    this.mCbsService.saveImmediately();
                    if (!this.mCotaWithReboot) {
                        this.mCbsService.sendCBSBroadcast();
                    }
                } else {
                    this.mPMSInstalling = true;
                    PackageManagerInternal packageManagerInt = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
                    packageManagerInt.installVgcCotaApp(false, new Runnable() { // from class: com.vivo.services.vgc.cbs.MiscHelper.1
                        @Override // java.lang.Runnable
                        public void run() {
                            MiscHelper.this.handleCotaInstallFinish(true);
                        }
                    });
                }
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:25:0x006d  */
    /* JADX WARN: Removed duplicated region for block: B:28:0x007e A[Catch: all -> 0x00a9, TryCatch #0 {, blocks: (B:3:0x0001, B:5:0x0005, B:6:0x001b, B:8:0x0030, B:10:0x0038, B:13:0x0041, B:27:0x0070, B:28:0x007e, B:30:0x0092, B:32:0x009c, B:33:0x00a1, B:18:0x0058, B:21:0x0062), top: B:41:0x0001 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public synchronized android.os.Bundle handleCotaGetParam(android.os.Bundle r9) {
        /*
            r8 = this;
            monitor-enter(r8)
            boolean r0 = com.vivo.services.vgc.cbs.CbsUtils.DEBUG     // Catch: java.lang.Throwable -> La9
            if (r0 == 0) goto L1b
            java.lang.String r0 = com.vivo.services.vgc.cbs.MiscHelper.TAG     // Catch: java.lang.Throwable -> La9
            java.lang.StringBuilder r1 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> La9
            r1.<init>()     // Catch: java.lang.Throwable -> La9
            java.lang.String r2 = "handleCotaGetParam bundle="
            r1.append(r2)     // Catch: java.lang.Throwable -> La9
            r1.append(r9)     // Catch: java.lang.Throwable -> La9
            java.lang.String r1 = r1.toString()     // Catch: java.lang.Throwable -> La9
            vivo.util.VSlog.d(r0, r1)     // Catch: java.lang.Throwable -> La9
        L1b:
            android.os.Bundle r0 = new android.os.Bundle     // Catch: java.lang.Throwable -> La9
            r0.<init>()     // Catch: java.lang.Throwable -> La9
            android.content.Context r1 = r8.mContext     // Catch: java.lang.Throwable -> La9
            android.content.pm.PackageManager r1 = r1.getPackageManager()     // Catch: java.lang.Throwable -> La9
            int r2 = android.os.Binder.getCallingUid()     // Catch: java.lang.Throwable -> La9
            java.lang.String r1 = r1.getNameForUid(r2)     // Catch: java.lang.Throwable -> La9
            if (r9 == 0) goto La7
            java.lang.String r2 = "key"
            boolean r2 = r9.containsKey(r2)     // Catch: java.lang.Throwable -> La9
            if (r2 == 0) goto La7
            java.lang.String r2 = "com.vivo.cota"
            boolean r2 = r2.equals(r1)     // Catch: java.lang.Throwable -> La9
            if (r2 != 0) goto L41
            goto La7
        L41:
            java.lang.String r2 = "key"
            java.lang.String r2 = r9.getString(r2)     // Catch: java.lang.Throwable -> La9
            r3 = -1
            int r4 = r2.hashCode()     // Catch: java.lang.Throwable -> La9
            r5 = -134402520(0xfffffffff7fd2e28, float:-1.0270213E34)
            r6 = 1
            if (r4 == r5) goto L62
            r5 = 1179789685(0x46522975, float:13450.364)
            if (r4 == r5) goto L58
        L57:
            goto L6b
        L58:
            java.lang.String r4 = "trigger_map_key"
            boolean r4 = r2.equals(r4)     // Catch: java.lang.Throwable -> La9
            if (r4 == 0) goto L57
            r3 = r6
            goto L6b
        L62:
            java.lang.String r4 = "defaultdata_map_key"
            boolean r4 = r2.equals(r4)     // Catch: java.lang.Throwable -> La9
            if (r4 == 0) goto L57
            r3 = 0
        L6b:
            if (r3 == 0) goto L7e
            if (r3 == r6) goto L70
            goto La5
        L70:
            com.vivo.services.vgc.cbs.VivoCbsServiceImpl r3 = r8.mCbsService     // Catch: java.lang.Throwable -> La9
            java.lang.String r4 = "map_key"
            java.lang.String r5 = "NA"
            java.lang.String r3 = r3.getSettingPropValue(r4, r5)     // Catch: java.lang.Throwable -> La9
            r0.putString(r2, r3)     // Catch: java.lang.Throwable -> La9
            goto La5
        L7e:
            java.lang.String r3 = "NA"
            int r4 = android.telephony.SubscriptionManager.getDefaultDataSubscriptionId()     // Catch: java.lang.Throwable -> La9
            android.content.Context r5 = r8.mContext     // Catch: java.lang.Throwable -> La9
            com.vivo.services.vgc.cbs.CbsSimInfo r5 = getCbsSimInfo(r5, r4, r6)     // Catch: java.lang.Throwable -> La9
            com.vivo.services.vgc.cbs.VivoCbsServiceImpl r6 = r8.mCbsService     // Catch: java.lang.Throwable -> La9
            com.vivo.services.vgc.cbs.CbsSimInfo r6 = r6.getMapSimInfo(r5)     // Catch: java.lang.Throwable -> La9
            if (r6 == 0) goto La1
            java.lang.String r7 = r6.getMapKey()     // Catch: java.lang.Throwable -> La9
            boolean r7 = android.text.TextUtils.isEmpty(r7)     // Catch: java.lang.Throwable -> La9
            if (r7 != 0) goto La1
            java.lang.String r7 = r6.getMapKey()     // Catch: java.lang.Throwable -> La9
            r3 = r7
        La1:
            r0.putString(r2, r3)     // Catch: java.lang.Throwable -> La9
        La5:
            monitor-exit(r8)
            return r0
        La7:
            monitor-exit(r8)
            return r0
        La9:
            r9 = move-exception
            monitor-exit(r8)
            throw r9
        */
        throw new UnsupportedOperationException("Method not decompiled: com.vivo.services.vgc.cbs.MiscHelper.handleCotaGetParam(android.os.Bundle):android.os.Bundle");
    }

    public synchronized void handlePMSSetParam(Bundle bundle) {
        if (CbsUtils.DEBUG) {
            String str = TAG;
            VSlog.d(str, "handlePMSSetParam bundle=" + bundle);
        }
        if (Binder.getCallingUid() == 1000 && this.mCotaInstallResult != null && bundle != null && bundle.containsKey("result") && bundle.containsKey("path") && bundle.containsKey("pkg")) {
            this.mCotaInstallResult.put(bundle.getString("path"), bundle);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void handleCotaInstallFinish(boolean reInstall) {
        if (CbsUtils.DEBUG) {
            String str = TAG;
            VSlog.d(str, "handleCotaInstallFinish reInstall=" + reInstall);
        }
        if (this.mCotaInstallWithReplace && reInstall && reInstallOrNot(this.mCotaInstallResult)) {
            return;
        }
        if (this.mCotaInstallCallback != null) {
            Bundle reply = new Bundle();
            int buffSize = this.mCotaInstallResult.size();
            String[] pkg = new String[buffSize];
            String[] path = new String[buffSize];
            int[] result = new int[buffSize];
            int idx = 0;
            for (Map.Entry<String, Bundle> entry : this.mCotaInstallResult.entrySet()) {
                Bundle b = entry.getValue();
                pkg[idx] = b.getString("pkg");
                path[idx] = b.getString("path");
                result[idx] = b.getInt("result");
                idx++;
            }
            reply.putStringArray("map_name_set", path);
            reply.putIntArray("map_value_set", result);
            reply.putStringArray("pkg", pkg);
            this.mCotaInstallCallback.onCallback(reply);
        }
        if (!this.mCotaWithReboot) {
            this.mCbsService.sendCBSBroadcast();
        }
        this.mCotaInstallResult.clear();
        this.mPMSInstalling = false;
    }

    private boolean reInstallOrNot(ArrayMap<String, Bundle> list) {
        ArrayList<String> reInstallList = new ArrayList<>();
        PackageManagerInternal packageManagerInt = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        PackageManager packageManager = this.mContext.getPackageManager();
        for (Map.Entry<String, Bundle> entry : list.entrySet()) {
            Bundle b = entry.getValue();
            int installResult = b.getInt("result", 0);
            if (installResult <= -100 && installResult >= -109) {
                installResult = -100;
            }
            if (installResult != -100 && installResult != 1 && installResult != -2 && installResult != -1) {
                switch (installResult) {
                    case -28:
                    case -20:
                    case -19:
                    case -18:
                    case -17:
                    case -16:
                    case -15:
                    case -14:
                    case -13:
                    case -12:
                    case -11:
                    case -10:
                    case -9:
                    case -6:
                    case -4:
                        break;
                    case -27:
                    case -26:
                    case -25:
                    case -24:
                    case -23:
                    case -22:
                    case -21:
                    case -8:
                    case -7:
                    case -5:
                        try {
                            packageManager.deletePackageAsUser(b.getString("pkg"), null, 0, UserHandle.myUserId());
                            reInstallList.add(b.getString("path"));
                            continue;
                        } catch (Throwable e) {
                            reInstallList.remove(b.getString("path"));
                            if (CbsUtils.DEBUG) {
                                e.printStackTrace();
                                break;
                            } else {
                                break;
                            }
                        }
                    default:
                        if (CbsUtils.DEBUG) {
                            String str = TAG;
                            VSlog.d(str, "reInstallOrNot unkown result=" + installResult);
                        }
                        reInstallList.add(b.getString("path"));
                        continue;
                }
            }
        }
        if (CbsUtils.DEBUG) {
            String str2 = TAG;
            VSlog.d(str2, "reInstallOrNot reInstallList.size()=" + reInstallList.size());
        }
        if (reInstallList.size() > 0) {
            packageManagerInt.reInstallCotaApps(reInstallList, new Runnable() { // from class: com.vivo.services.vgc.cbs.MiscHelper.2
                @Override // java.lang.Runnable
                public void run() {
                    MiscHelper.this.handleCotaInstallFinish(false);
                }
            });
        }
        return reInstallList.size() > 0;
    }

    public static CbsSimInfo getCbsSimInfo(Context context, int subId, int mode) {
        int mainCard = 1;
        String imsi = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        String iccid = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        if (subId == -1) {
            return new CbsSimInfo(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        }
        String str = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        if (mode == 1) {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
            mainCard = SubscriptionManager.getPhoneId(subId) + 1;
            String iccid2 = telephonyManager.getSimSerialNumber(subId);
            String imsi2 = telephonyManager.getSubscriberId(subId);
            iccid = iccid2 == null ? Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK : iccid2;
            if (imsi2 != null) {
                str = imsi2;
            }
            imsi = str;
        } else if (mode == 0) {
            mainCard = 1;
            iccid = SystemProperties.get("persist.radio.sim1.iccid", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
            imsi = SystemProperties.get("persist.radio.sim1.imsi", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        }
        String spn = VivoCbsManager.getSpnFromProperty(mode, mainCard);
        String mccmnc = VivoCbsManager.getMccMncFromProperty(mode, mainCard);
        String gid1 = VivoCbsManager.getGid1FromProperty(mode, mainCard);
        CbsSimInfo info = new CbsSimInfo(mccmnc, gid1, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, spn, iccid, imsi);
        return info;
    }

    public static int getSubId(Context context, int phoneId) {
        int[] subIds;
        SubscriptionManager subscriptionManager = (SubscriptionManager) context.getSystemService("telephony_subscription_service");
        if (subscriptionManager == null || (subIds = subscriptionManager.getSubscriptionIds(phoneId)) == null || subIds.length == 0) {
            return -1;
        }
        return subIds[0];
    }

    public static int getSubscriptionCarrierId(Context context, int phoneId) {
        try {
            ITelephony service = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
            if (service != null) {
                return service.getSubscriptionCarrierId(getSubId(context, phoneId));
            }
            return -1;
        } catch (RemoteException e) {
            return -1;
        }
    }

    public static int getSubscriptionCarrierIdBySubId(Context context, int subId) {
        try {
            ITelephony service = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
            if (service != null) {
                return service.getSubscriptionCarrierId(subId);
            }
            return -1;
        } catch (RemoteException e) {
            return -1;
        }
    }

    public static int getSimSpecificCarrierId(Context context, int phoneId) {
        try {
            ITelephony service = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
            if (service != null) {
                return service.getSubscriptionSpecificCarrierId(getSubId(context, phoneId));
            }
            return -1;
        } catch (RemoteException e) {
            return -1;
        }
    }

    public String getSimCarrierIdName(Context context, int phoneId) {
        CharSequence name;
        try {
            ITelephony service = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
            if (service == null || (name = service.getSubscriptionCarrierName(getSubId(context, phoneId))) == null) {
                return null;
            }
            return name.toString();
        } catch (RemoteException e) {
        }
        return null;
    }
}