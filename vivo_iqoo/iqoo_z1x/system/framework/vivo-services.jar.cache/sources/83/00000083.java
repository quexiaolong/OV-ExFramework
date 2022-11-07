package com.android.server.am;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.android.server.UnifiedConfigThread;
import com.vivo.face.common.data.Constants;
import com.vivo.services.backup.util.VivoBackupCommonUtil;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import vivo.app.epm.ExceptionPolicyManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class AmsDataManager {
    private static final int EVENT_TYPE = 48;
    private static final int MAX_COUNT = 50;
    private static final int MSG_SCHEDULE_REPORT = 100;
    private static final int ONE_SECOND = 1000;
    private static final String SEPARATOR = "_";
    public static final int TYPE_EXCEPTION = 3;
    public static final int TYPE_FREQUENTLY_ACCESS = 1;
    public static final int TYPE_INFO = 4;
    public static final int TYPE_TIMEOUT = 2;
    private static volatile AmsReportHandler mReportHandler;
    private static volatile AmsDataManager sInstance = null;
    private HashMap<ComponentName, HashMap<String, ServiceStartRecord>> mServiceStartRecordMap = new HashMap<>();
    private HashMap<ComponentName, HashMap<String, ServiceBindRecord>> mServiceBindRecordMap = new HashMap<>();
    private HashMap<String, HashMap<String, BroadcastSendRecord>> mBroadcastSendRecordMap = new HashMap<>();
    private HashMap<ComponentName, HashMap<String, ProviderGetRecord>> mProviderGetRecordMap = new HashMap<>();
    int mServiceStartCount = 0;
    int mServiceBindCount = 0;
    int mBroadcastSendCount = 0;
    int mProviderGetCount = 0;
    private final int MAX_BR_RECORD_SIZE = 2000;
    private final int MAX_RECORD_SIZE = 1000;
    private final String TAG = "AmsDataManager";

    public static synchronized AmsDataManager getInstance() {
        AmsDataManager amsDataManager;
        synchronized (AmsDataManager.class) {
            if (sInstance == null) {
                sInstance = new AmsDataManager();
            }
            amsDataManager = sInstance;
        }
        return amsDataManager;
    }

    private AmsDataManager() {
        if (mReportHandler == null) {
            synchronized (AmsDataManager.class) {
                if (mReportHandler == null) {
                    mReportHandler = new AmsReportHandler(UnifiedConfigThread.get().getLooper());
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class AmsReportHandler extends Handler {
        public AmsReportHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            VSlog.i("AmsDataManager", "--handleMessage " + msg.what);
            if (msg.what == 100) {
                AmsDataManager.this.handlerReport(msg.arg1, msg.obj);
            }
        }
    }

    public void scheduleReport(int type, Bundle reportInfo) {
        Message msg = mReportHandler.obtainMessage(100);
        msg.arg1 = type;
        msg.obj = reportInfo;
        if (type != 4) {
            mReportHandler.sendMessage(msg);
        } else {
            mReportHandler.sendMessageDelayed(msg, VivoBackupCommonUtil.TIMEOUT_MONITOR_INTERVAL);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class AmsDataRecord {
        long begintime;
        String callerPackage;
        int count = 0;
        boolean isReported = false;

        AmsDataRecord(long _time, String _callerPackage) {
            this.begintime = _time;
            this.callerPackage = _callerPackage;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ServiceStartRecord extends AmsDataRecord {
        ComponentName cpn;
        Intent intent;

        ServiceStartRecord(Intent _intent, long _time, ComponentName _cpn, String _PackageName) {
            super(_time, _PackageName);
            this.intent = _intent;
            this.cpn = _cpn;
        }
    }

    public void addServiceStartToHistory(ServiceRecord r, Intent service, ProcessRecord callerApp) {
        HashMap<String, ServiceStartRecord> hssr;
        if (callerApp == null || callerApp.processName == null) {
            return;
        }
        this.mServiceStartCount++;
        ComponentName cpn = r.name.clone();
        HashMap<String, ServiceStartRecord> hssr2 = this.mServiceStartRecordMap.get(cpn);
        if (hssr2 != null) {
            hssr = hssr2;
        } else {
            if (this.mServiceStartRecordMap.size() >= 1000) {
                this.mServiceStartRecordMap.clear();
            }
            HashMap<String, ServiceStartRecord> hssr3 = new HashMap<>();
            this.mServiceStartRecordMap.put(cpn, hssr3);
            hssr = hssr3;
        }
        ServiceStartRecord ssr = hssr.get(callerApp.processName);
        if (ssr == null) {
            ssr = new ServiceStartRecord(service, System.currentTimeMillis(), cpn, callerApp.processName);
            hssr.put(callerApp.processName, ssr);
        }
        if (!ssr.intent.filterEquals(service)) {
            ssr.intent = service;
        }
        if (System.currentTimeMillis() - ssr.begintime < 1000) {
            ssr.count++;
            if (!ssr.isReported && ssr.count >= 50) {
                reportFrequentlyAccessException(ssr);
                VSlog.w("AmsDataManager", "*****too many service(" + cpn + ") started by " + callerApp.processName + " at one second!!!");
                ssr.isReported = true;
                return;
            }
            return;
        }
        ssr.count = 1;
        ssr.begintime = System.currentTimeMillis();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ServiceBindRecord extends AmsDataRecord {
        ComponentName cpn;
        Intent intent;

        ServiceBindRecord(Intent _intent, long _time, ComponentName _cpn, String _PackageName) {
            super(_time, _PackageName);
            this.intent = _intent;
            this.cpn = _cpn;
        }
    }

    public void addServiceBindToHistory(ServiceRecord r, Intent service, ProcessRecord callerApp) {
        HashMap<String, ServiceBindRecord> hsbr;
        if (callerApp == null || callerApp.processName == null) {
            return;
        }
        this.mServiceBindCount++;
        ComponentName cpn = r.name.clone();
        HashMap<String, ServiceBindRecord> hsbr2 = this.mServiceBindRecordMap.get(cpn);
        if (hsbr2 != null) {
            hsbr = hsbr2;
        } else {
            if (this.mServiceStartRecordMap.size() >= 1000) {
                this.mServiceStartRecordMap.clear();
            }
            HashMap<String, ServiceBindRecord> hsbr3 = new HashMap<>();
            this.mServiceBindRecordMap.put(cpn, hsbr3);
            hsbr = hsbr3;
        }
        ServiceBindRecord sbr = hsbr.get(callerApp.processName);
        if (sbr == null) {
            sbr = new ServiceBindRecord(service, System.currentTimeMillis(), cpn, callerApp.processName);
            hsbr.put(callerApp.processName, sbr);
        }
        if (!sbr.intent.filterEquals(service)) {
            sbr.intent = service;
        }
        if (System.currentTimeMillis() - sbr.begintime < 1000) {
            sbr.count++;
            if (!sbr.isReported && sbr.count >= 50) {
                reportFrequentlyAccessException(sbr);
                VSlog.w("AmsDataManager", "*****too many service(" + cpn + ") bound by " + callerApp.processName + " at one second!!!");
                sbr.isReported = true;
                return;
            }
            return;
        }
        sbr.count = 1;
        sbr.begintime = System.currentTimeMillis();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class BroadcastSendRecord extends AmsDataRecord {
        Intent intent;

        BroadcastSendRecord(Intent _intent, long _time, String _PackageName) {
            super(_time, _PackageName);
            this.intent = _intent;
        }
    }

    public void addBroadcastToHistory(Intent intent, String callerPackage) {
        if (callerPackage == null || "android.intent.action.TIME_TICK".equals(intent.getAction())) {
            return;
        }
        this.mBroadcastSendCount++;
        String action = intent.getAction();
        HashMap<String, BroadcastSendRecord> hbsr = this.mBroadcastSendRecordMap.get(action);
        if (hbsr == null) {
            if (this.mBroadcastSendRecordMap.size() >= 2000) {
                this.mBroadcastSendRecordMap.clear();
            }
            hbsr = new HashMap<>();
            this.mBroadcastSendRecordMap.put(action, hbsr);
        }
        BroadcastSendRecord bsr = hbsr.get(callerPackage);
        if (bsr == null) {
            bsr = new BroadcastSendRecord(intent, System.currentTimeMillis(), callerPackage);
            hbsr.put(callerPackage, bsr);
        }
        if (System.currentTimeMillis() - bsr.begintime < 1000) {
            bsr.count++;
            if (!bsr.isReported && bsr.count >= 50) {
                reportFrequentlyAccessException(bsr);
                VSlog.w("AmsDataManager", "*****too many broadcast(" + action + ") send by " + callerPackage + " at one second!!!");
                bsr.isReported = true;
                return;
            }
            return;
        }
        bsr.count = 1;
        bsr.begintime = System.currentTimeMillis();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ProviderGetRecord extends AmsDataRecord {
        String author;
        ComponentName cpn;

        ProviderGetRecord(long _time, String _PackageName, ComponentName _cpn, String _author) {
            super(_time, _PackageName);
            this.cpn = _cpn;
            this.author = _author;
        }
    }

    public void addProviderToHistory(String author, String callerPackage, ComponentName comp) {
        HashMap<String, ProviderGetRecord> hpgr;
        if (callerPackage == null) {
            return;
        }
        this.mProviderGetCount++;
        ComponentName cpn = comp.clone();
        HashMap<String, ProviderGetRecord> hpgr2 = this.mProviderGetRecordMap.get(cpn);
        if (hpgr2 != null) {
            hpgr = hpgr2;
        } else {
            if (this.mProviderGetRecordMap.size() >= 1000) {
                this.mProviderGetRecordMap.clear();
            }
            HashMap<String, ProviderGetRecord> hpgr3 = new HashMap<>();
            this.mProviderGetRecordMap.put(cpn, hpgr3);
            hpgr = hpgr3;
        }
        ProviderGetRecord pgr = hpgr.get(callerPackage);
        if (pgr == null) {
            pgr = new ProviderGetRecord(System.currentTimeMillis(), callerPackage, cpn, author);
            hpgr.put(callerPackage, pgr);
        }
        if (System.currentTimeMillis() - pgr.begintime < 1000) {
            pgr.count++;
            if (!pgr.isReported && pgr.count >= 50) {
                reportFrequentlyAccessException(pgr);
                VSlog.w("AmsDataManager", "*****too many provider(" + cpn + ") get by " + callerPackage + " at one second!!!");
                pgr.isReported = true;
                return;
            }
            return;
        }
        pgr.count = 1;
        pgr.begintime = System.currentTimeMillis();
    }

    public void dumpAmsDataHistory(PrintWriter pw, String[] args, int opti) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        int i = -1;
        pw.println("*************************SERVICE START HISTORY*************************");
        pw.println("Service start Historical [Total：" + this.mServiceStartCount + "]summary :");
        boolean z = false;
        boolean z2 = true;
        if (this.mServiceStartRecordMap.size() > 0) {
            Iterator it = this.mServiceStartRecordMap.values().iterator();
            while (it.hasNext()) {
                HashMap<String, ServiceStartRecord> hssr = it.next();
                for (ServiceStartRecord ssr : hssr.values()) {
                    i++;
                    pw.print("  service #");
                    pw.print(i);
                    pw.print(": ");
                    pw.print(ssr.cpn);
                    pw.println(ssr.intent.toShortString(z, z2, z2, z));
                    pw.print("    started by ");
                    pw.print(ssr.callerPackage + " count:");
                    pw.print(ssr.count);
                    pw.print(" at ");
                    pw.println(sdf.format(new Date(ssr.begintime)));
                    hssr = hssr;
                    z = false;
                    z2 = true;
                }
                z = false;
                z2 = true;
            }
        }
        int i2 = -1;
        pw.println("\n*************************SERVICE BIND HISTORY*************************");
        pw.println("Service Bind Historical [Total：" + this.mServiceBindCount + "]summary :");
        if (this.mServiceBindRecordMap.size() > 0) {
            Iterator it2 = this.mServiceBindRecordMap.values().iterator();
            while (it2.hasNext()) {
                HashMap<String, ServiceBindRecord> hsbr = it2.next();
                for (ServiceBindRecord sbr : hsbr.values()) {
                    int i3 = i2 + 1;
                    pw.print("  service #");
                    pw.print(i3);
                    pw.print(": ");
                    pw.print(sbr.cpn);
                    pw.println(sbr.intent.toShortString(false, true, true, false));
                    pw.print("    bound by ");
                    pw.print(sbr.callerPackage);
                    pw.print(sbr.callerPackage + " count:");
                    pw.print(sbr.count);
                    pw.print(" at ");
                    pw.println(sdf.format(new Date(sbr.begintime)));
                    it2 = it2;
                    i2 = i3;
                }
            }
        }
        int i4 = -1;
        pw.println("\n*************************BROADCAST SEND HISTORY*************************");
        pw.println("Broadcast Send Historical [Total：" + this.mBroadcastSendCount + "]summary :");
        if (this.mBroadcastSendRecordMap.size() > 0) {
            for (HashMap<String, BroadcastSendRecord> hbsr : this.mBroadcastSendRecordMap.values()) {
                for (BroadcastSendRecord bsr : hbsr.values()) {
                    i4++;
                    pw.print("  broadcast #");
                    pw.print(i4);
                    pw.print(": ");
                    pw.print(bsr.intent.toShortString(false, true, true, false));
                    pw.print("    send by ");
                    pw.print(bsr.callerPackage + " count:");
                    pw.print(bsr.count);
                    pw.print(" at ");
                    pw.println(sdf.format(new Date(bsr.begintime)));
                }
            }
        }
        int i5 = -1;
        pw.println("\n*************************PROVIDER GET HISTORY*************************");
        pw.println("Provider Historical [Total：" + this.mProviderGetCount + "]summary :");
        if (this.mProviderGetRecordMap.size() > 0) {
            for (HashMap<String, ProviderGetRecord> hpgr : this.mProviderGetRecordMap.values()) {
                for (ProviderGetRecord pgr : hpgr.values()) {
                    i5++;
                    pw.print("  provider #");
                    pw.print(i5);
                    pw.print(": ");
                    pw.println(pgr.cpn + ",author:" + pgr.author);
                    pw.print("    acquire by ");
                    pw.print(pgr.callerPackage + " count:");
                    pw.print(pgr.count);
                    pw.print(" at ");
                    pw.println(sdf.format(new Date(pgr.begintime)));
                }
            }
        }
    }

    private void reportFrequentlyAccessException(AmsDataRecord adr) {
        Bundle reportInfo = new Bundle();
        reportInfo.putString("expsrc", adr.callerPackage);
        if (adr instanceof ServiceStartRecord) {
            reportInfo.putString("expose", ((ServiceStartRecord) adr).cpn.toString());
            reportInfo.putString("reason", "service_start");
            reportInfo.putString("data1", ((ServiceStartRecord) adr).intent.toShortString(true, true, true, false));
        } else if (adr instanceof ServiceBindRecord) {
            reportInfo.putString("expose", ((ServiceBindRecord) adr).cpn.toString());
            reportInfo.putString("reason", "service_bind");
            reportInfo.putString("data1", ((ServiceBindRecord) adr).intent.toShortString(true, true, true, false));
        } else if (adr instanceof BroadcastSendRecord) {
            reportInfo.putString("reason", "broadcast_send");
            reportInfo.putString("data1", ((BroadcastSendRecord) adr).intent.toShortString(true, true, true, false));
        } else if (adr instanceof ProviderGetRecord) {
            reportInfo.putString("expose", ((ProviderGetRecord) adr).cpn.toString());
            reportInfo.putString("reason", "provider_get");
            reportInfo.putString("data1", ((ProviderGetRecord) adr).author);
        }
        scheduleReport(1, reportInfo);
    }

    public void reportAmsTimeoutException(String callerPackage, String ComponentName, String reason, Intent intent) {
        Bundle reportInfo = new Bundle();
        reportInfo.putString("expsrc", callerPackage);
        reportInfo.putString("expose", ComponentName);
        reportInfo.putString("reason", reason);
        reportInfo.putString("data1", intent != null ? intent.toShortString(true, true, true, false) : Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        scheduleReport(2, reportInfo);
    }

    public void reportAmsException(String detailReason, String processName, String reason, String exp) {
        Bundle reportInfo = new Bundle();
        reportInfo.putString("expsrc", detailReason);
        reportInfo.putString("expose", processName);
        reportInfo.putString("reason", reason);
        reportInfo.putString("data1", exp);
        scheduleReport(3, reportInfo);
    }

    public void reportBootingActivityStartInfo(String callerPackage, String ComponentName, String reason, String trace) {
        Bundle reportInfo = new Bundle();
        reportInfo.putString("expsrc", callerPackage);
        reportInfo.putString("expose", ComponentName);
        reportInfo.putString("reason", reason);
        reportInfo.putString("data1", trace);
        scheduleReport(4, reportInfo);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handlerReport(int type, Object object) {
        if (object == null) {
            return;
        }
        try {
            Bundle reportInfo = (Bundle) object;
            ContentValues cv = new ContentValues();
            cv.put("expose", reportInfo.getString("expose", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK));
            cv.put("expsrc", reportInfo.getString("expsrc", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK));
            cv.put("reason", reportInfo.getString("reason", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK));
            cv.put("data1", reportInfo.getString("data1", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK));
            cv.put("level", (Integer) 4);
            cv.put("subtype", "48_" + type);
            VSlog.i("AmsDataManager", "handlerReport reportInfo " + cv);
            ExceptionPolicyManager.getInstance().recordEvent(48, System.currentTimeMillis(), cv);
        } catch (Exception e) {
            VSlog.w("AmsDataManager", "report to epm catch exception", e);
        }
    }
}