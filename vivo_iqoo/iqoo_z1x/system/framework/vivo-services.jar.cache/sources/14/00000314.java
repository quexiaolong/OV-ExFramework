package com.android.server.net.monitor;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.graphics.common.V1_0.Dataspace;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.VivoTelephonyApiParams;
import com.android.server.connectivity.NetworkAgentInfo;
import com.android.server.wm.VCD_FF_1;
import com.vivo.face.common.data.Constants;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoNetworkAnalyser {
    private static final int EVENT_BACKUP_DNS_OVERVIEW = 3;
    private static final int EVENT_DNS_OVERVIEW_COLLECT = 2752;
    private static final int EVENT_HANDLE_UNRECORD_DNS = 2;
    private static final int EVENT_RESTORE_DNS_OVERVIEW = 4;
    private static final int EVENT_VDC_DNS_OVERVIEW = 1;
    private static final String INTENT_DNS_OVERVIEW_VDC_ALARM = "com.vivo.intent.action.NETWORK_ANALYSER_DNS_OVERVIEW";
    private static final int NETWORK_CLASS_4_G = 3;
    private static final int NETWORK_CLASS_5_G = 4;
    private static final int NETWORK_CLASS_OTHER = 1;
    private static final int NETWORK_CLASS_WIFI = 2;
    private static final int NETWORK_TYPE_IWLAN = 18;
    private static final int NETWORK_TYPE_LTE = 13;
    private static final int NETWORK_TYPE_LTE_CA = 19;
    private static final int NETWORK_TYPE_NR = 20;
    private static final String TAG = "VivoNetworkAnalyser";
    private static final int VCODE_EVENTID_DNS_OVERVIEW = 0;
    private AlarmManager mAlarmManager;
    private Context mContext;
    private AnalyserHandler mHandler;
    private static final boolean sRTCScheduled = "RTC".equals(SystemProperties.get("persist.vivo.net_analyser_scheduled_type", "NONE"));
    private static final String[][] EVENTID = {new String[]{"F376", "F376|10005"}};
    private static String NETWORK_ANALYSER_BASE_DIR = "/data/system";
    private static String DNS_OVERVIEW_BACKUP_NAME = "dns_over_view";
    private static final String[] mDnsMonitorPackages = {"com.vivo.browser"};
    private static VivoNetworkAnalyser sMe = null;
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() { // from class: com.android.server.net.monitor.VivoNetworkAnalyser.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(VivoNetworkAnalyser.INTENT_DNS_OVERVIEW_VDC_ALARM)) {
                if (VivoNetworkAnalyser.this.mHandler != null && !VivoNetworkAnalyser.this.mHandler.hasMessages(1)) {
                    VivoNetworkAnalyser.this.mHandler.sendMessage(VivoNetworkAnalyser.this.mHandler.obtainMessage(1));
                }
            } else if (action.equals("android.intent.action.ACTION_SHUTDOWN") && VivoNetworkAnalyser.this.mHandler != null) {
                VivoNetworkAnalyser.this.mHandler.sendMessage(VivoNetworkAnalyser.this.mHandler.obtainMessage(3));
            }
        }
    };
    private Map<Integer, String> mDnsMonitorUids = new HashMap();
    private Map<DnsOverViewKey, DnsOverViewValue> mDnsOverView = new HashMap();
    private List<DnsEvent> mUnrecordEvent = new ArrayList();
    private Object mUnrecordEventLock = new Object();

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class DayDuration {
        public long mEnd;
        public long mStart;

        private DayDuration() {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class DnsEvent {
        int eventType;
        String hostname;
        String[] ipAddresses;
        int ipAddressesCount;
        NetworkAgentInfo nai;
        int returnCode;
        long timestamp;
        int uid;

        private DnsEvent() {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class DnsOverViewKey {
        String hostname;
        int netClass;
        String requestPackage;

        private DnsOverViewKey() {
        }

        public int hashCode() {
            int result = (1 * 31) + Objects.hash(this.hostname);
            return (((result * 31) + Objects.hash(this.requestPackage)) * 31) + Objects.hash(Integer.valueOf(this.netClass));
        }

        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof DnsOverViewKey)) {
                return false;
            }
            DnsOverViewKey e = (DnsOverViewKey) obj;
            if (!Objects.equals(this.hostname, e.hostname) || !Objects.equals(this.requestPackage, e.requestPackage) || this.netClass != e.netClass) {
                return false;
            }
            return true;
        }

        public String toString() {
            return "hostname = " + this.hostname + ",requestPackage = " + this.requestPackage + ",netClass = " + this.netClass;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class DnsOverViewValue {
        int failureCount;
        int successCount;

        private DnsOverViewValue() {
        }

        public String toString() {
            return "successCount = " + this.successCount + ",failureCount = " + this.failureCount;
        }
    }

    /* loaded from: classes.dex */
    private class AnalyserHandler extends Handler {
        public AnalyserHandler(Looper loop) {
            super(loop);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 1) {
                VivoNetworkAnalyser.this.VDCDnsOverView();
            } else if (i == 2) {
                VivoNetworkAnalyser.this.handleUnrecordDnsEvent();
            } else if (i == 3) {
                VivoNetworkAnalyser.this.backupDnsOverView();
            } else if (i == 4) {
                VivoNetworkAnalyser.this.restoreDnsOverView();
            }
        }
    }

    public static VivoNetworkAnalyser getInstance(Context context) {
        VivoNetworkAnalyser vivoNetworkAnalyser;
        synchronized (VivoNetworkAnalyser.class) {
            if (sMe == null) {
                sMe = new VivoNetworkAnalyser(context);
            }
            vivoNetworkAnalyser = sMe;
        }
        return vivoNetworkAnalyser;
    }

    private VivoNetworkAnalyser(Context context) {
        String[] strArr;
        this.mContext = null;
        this.mHandler = null;
        this.mAlarmManager = null;
        this.mContext = context;
        for (String pack : mDnsMonitorPackages) {
            int uid = getPackageUid(pack);
            log("add DnsMonitorPackages for [" + pack + ", " + uid + "]");
            if (-1 != uid) {
                this.mDnsMonitorUids.put(Integer.valueOf(uid), pack);
            }
        }
        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        this.mHandler = new AnalyserHandler(handlerThread.getLooper());
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        IntentFilter filter = new IntentFilter();
        filter.addAction(INTENT_DNS_OVERVIEW_VDC_ALARM);
        filter.addAction("android.intent.action.ACTION_SHUTDOWN");
        this.mContext.registerReceiver(this.mIntentReceiver, filter, null, this.mHandler);
        startNextScheduled();
        AnalyserHandler analyserHandler = this.mHandler;
        analyserHandler.sendMessage(analyserHandler.obtainMessage(4));
    }

    public void recordDnsEvent(NetworkAgentInfo nai, int eventType, int returnCode, String hostname, String[] ipAddresses, int ipAddressesCount, long timestamp, int uid) {
        log("recordDnsEvent eventType = " + eventType + " returnCode = " + returnCode + " hostname = " + hostname + " uid = " + uid);
        if (this.mDnsMonitorUids.containsKey(Integer.valueOf(uid))) {
            DnsEvent event = new DnsEvent();
            event.nai = nai;
            event.uid = uid;
            event.eventType = eventType;
            event.returnCode = returnCode;
            event.hostname = hostname;
            event.ipAddresses = ipAddresses;
            event.ipAddressesCount = ipAddressesCount;
            event.timestamp = timestamp;
            addUnrecordDnsEvent(event);
            AnalyserHandler analyserHandler = this.mHandler;
            if (analyserHandler != null && !analyserHandler.hasMessages(2)) {
                AnalyserHandler analyserHandler2 = this.mHandler;
                analyserHandler2.sendMessage(analyserHandler2.obtainMessage(2));
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleUnrecordDnsEvent() {
        DnsEvent event = popUnrecordDnsEvent();
        while (event != null) {
            DnsOverViewKey key = new DnsOverViewKey();
            key.hostname = event.hostname;
            key.requestPackage = this.mDnsMonitorUids.get(Integer.valueOf(event.uid));
            key.netClass = getNetworkClass(event.nai);
            DnsOverViewValue value = this.mDnsOverView.get(key);
            if (value == null) {
                value = new DnsOverViewValue();
            }
            if (event.returnCode == 0) {
                value.successCount++;
            } else {
                value.failureCount++;
            }
            this.mDnsOverView.put(key, value);
            log("handleUnrecordDnsEvent update key = [" + key.toString() + "], value = [" + value.toString() + "]");
            event = popUnrecordDnsEvent();
        }
    }

    private void addUnrecordDnsEvent(DnsEvent event) {
        synchronized (this.mUnrecordEventLock) {
            this.mUnrecordEvent.add(event);
        }
    }

    private DnsEvent popUnrecordDnsEvent() {
        synchronized (this.mUnrecordEventLock) {
            if (this.mUnrecordEvent.isEmpty()) {
                return null;
            }
            return this.mUnrecordEvent.remove(0);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void VDCDnsOverView() {
        if (!this.mDnsOverView.isEmpty()) {
            List<Map.Entry<DnsOverViewKey, DnsOverViewValue>> list = new ArrayList<>(this.mDnsOverView.entrySet());
            Collections.sort(list, new Comparator<Map.Entry<DnsOverViewKey, DnsOverViewValue>>() { // from class: com.android.server.net.monitor.VivoNetworkAnalyser.2
                @Override // java.util.Comparator
                public int compare(Map.Entry<DnsOverViewKey, DnsOverViewValue> e1, Map.Entry<DnsOverViewKey, DnsOverViewValue> e2) {
                    DnsOverViewValue value1 = e1.getValue();
                    DnsOverViewValue value2 = e2.getValue();
                    return (value2.successCount + value2.failureCount) - (value1.successCount + value1.failureCount);
                }
            });
            StringBuilder recordBuilder = new StringBuilder();
            int recordLength = 0;
            Iterator<Map.Entry<DnsOverViewKey, DnsOverViewValue>> iter = list.iterator();
            for (int i = 0; recordLength < 20000 && i < 200 && iter.hasNext(); i++) {
                Map.Entry<DnsOverViewKey, DnsOverViewValue> record = iter.next();
                recordBuilder.append("[");
                recordBuilder.append(record.getKey().toString());
                recordBuilder.append(",");
                recordBuilder.append(record.getValue().toString());
                recordBuilder.append("]");
                recordLength += record.getKey().toString().length() + record.getValue().toString().length() + 3;
            }
            try {
                ITelephony iTelephony = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
                VivoTelephonyApiParams param = new VivoTelephonyApiParams("API_TAG_setDataParam");
                param.put("eventId", Integer.valueOf((int) EVENT_DNS_OVERVIEW_COLLECT));
                param.put(VCD_FF_1.UUID_STR, UUID.randomUUID().toString());
                param.put("overview", recordBuilder.toString());
                iTelephony.vivoTelephonyApi(param);
            } catch (Exception e) {
                log("e = " + e.toString());
            }
            this.mDnsOverView.clear();
        }
        startNextScheduled();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void backupDnsOverView() {
        if (!this.mDnsOverView.isEmpty()) {
            FileOutputStream fos = null;
            BufferedOutputStream bos = null;
            DataOutputStream dos = null;
            try {
                try {
                    try {
                        File dnsTotalRecordFile = new File(NETWORK_ANALYSER_BASE_DIR, DNS_OVERVIEW_BACKUP_NAME);
                        if (dnsTotalRecordFile.exists()) {
                            dnsTotalRecordFile.delete();
                        }
                        dnsTotalRecordFile.createNewFile();
                        fos = new FileOutputStream(dnsTotalRecordFile);
                        bos = new BufferedOutputStream(fos);
                        dos = new DataOutputStream(bos);
                        dos.writeInt(this.mDnsOverView.size());
                        for (DnsOverViewKey key : this.mDnsOverView.keySet()) {
                            DnsOverViewValue value = this.mDnsOverView.get(key);
                            writeOptionalString(dos, key.hostname);
                            writeOptionalString(dos, key.requestPackage);
                            dos.writeInt(key.netClass);
                            dos.writeInt(value.successCount);
                            dos.writeInt(value.failureCount);
                            log("backupDnsOverView key = [" + key.toString() + "], value = [" + value.toString() + "]");
                        }
                        dos.close();
                        bos.close();
                        fos.close();
                    } catch (Throwable th) {
                        if (dos != null) {
                            try {
                                dos.close();
                            } catch (Exception e) {
                                throw th;
                            }
                        }
                        if (bos != null) {
                            bos.close();
                        }
                        if (fos != null) {
                            fos.close();
                        }
                        throw th;
                    }
                } catch (Exception e2) {
                    e2.printStackTrace();
                    if (dos != null) {
                        dos.close();
                    }
                    if (bos != null) {
                        bos.close();
                    }
                    if (fos != null) {
                        fos.close();
                    }
                }
            } catch (Exception e3) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void restoreDnsOverView() {
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        DataInputStream dis = null;
        try {
            try {
                try {
                    File dnsTotalRecordFile = new File(NETWORK_ANALYSER_BASE_DIR, DNS_OVERVIEW_BACKUP_NAME);
                    if (!dnsTotalRecordFile.exists()) {
                        log("restoreDnsOverView no exists record");
                        if (dis != null) {
                            try {
                            } catch (Exception e) {
                                return;
                            }
                        }
                        if (fis != null) {
                            return;
                        }
                        return;
                    }
                    FileInputStream fis2 = new FileInputStream(dnsTotalRecordFile);
                    BufferedInputStream bis2 = new BufferedInputStream(fis2);
                    DataInputStream dis2 = new DataInputStream(bis2);
                    int recordSize = dis2.readInt();
                    for (int i = 0; i < recordSize; i++) {
                        DnsOverViewKey key = new DnsOverViewKey();
                        key.hostname = readOptionalString(dis2);
                        key.requestPackage = readOptionalString(dis2);
                        key.netClass = dis2.readInt();
                        DnsOverViewValue value = this.mDnsOverView.get(key);
                        if (value == null) {
                            value = new DnsOverViewValue();
                        }
                        value.successCount += dis2.readInt();
                        value.failureCount += dis2.readInt();
                        log("restoreDnsOverView key = [" + key.toString() + "], value = [" + value.toString() + "]");
                        this.mDnsOverView.put(key, value);
                    }
                    dnsTotalRecordFile.delete();
                    dis2.close();
                    bis2.close();
                    fis2.close();
                } catch (Exception e2) {
                    e2.printStackTrace();
                    if (0 != 0) {
                        dis.close();
                    }
                    if (0 != 0) {
                        bis.close();
                    }
                    if (0 != 0) {
                        fis.close();
                    }
                }
            } finally {
                if (0 != 0) {
                    try {
                        dis.close();
                    } catch (Exception e3) {
                    }
                }
                if (0 != 0) {
                    bis.close();
                }
                if (0 != 0) {
                    fis.close();
                }
            }
        } catch (Exception e4) {
        }
    }

    private static String readOptionalString(DataInputStream in) throws IOException {
        if (in.readByte() != 0) {
            return in.readUTF();
        }
        return null;
    }

    private static void writeOptionalString(DataOutputStream out, String value) throws IOException {
        if (value != null) {
            out.writeByte(1);
            out.writeUTF(value);
            return;
        }
        out.writeByte(0);
    }

    private int getNetworkClass(NetworkAgentInfo nai) {
        if (nai == null || nai.networkInfo == null) {
            return 1;
        }
        NetworkInfo netInfo = nai.networkInfo;
        if (1 == netInfo.getType()) {
            return 2;
        }
        if (netInfo.getType() != 0) {
            return 1;
        }
        int networkClass = getNetworkClass(netInfo.getSubtype());
        return networkClass;
    }

    private int getNetworkClass(int networkType) {
        if (networkType != 13) {
            switch (networkType) {
                case 18:
                case 19:
                    return 3;
                case 20:
                    return 4;
                default:
                    return 1;
            }
        }
        return 3;
    }

    private String getPackageName(int uid) {
        String packageName = this.mContext.getPackageManager().getNameForUid(uid);
        return packageName == null ? Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK : packageName;
    }

    private int getPackageUid(String packagename) {
        try {
            PackageManager pm = this.mContext.getPackageManager();
            int uid = pm.getPackageUid(packagename, 0);
            return uid;
        } catch (Exception e) {
            return -1;
        }
    }

    private void startNextScheduled() {
        try {
            long currentTime = System.currentTimeMillis();
            Date today = new Date(currentTime);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(today);
            calendar.add(5, 1);
            int year = calendar.get(1);
            int month = calendar.get(2);
            int day = calendar.get(5);
            DayDuration tomorrow = getTargetDayDuration(year, month, day);
            long nextScheduledTime = tomorrow.mStart + ((long) (Math.random() * (tomorrow.mEnd - tomorrow.mStart)));
            long delay = nextScheduledTime - currentTime;
            log("startNextScheduled RTC = " + sRTCScheduled + ", delay = " + delay);
            Intent intent = new Intent(INTENT_DNS_OVERVIEW_VDC_ALARM);
            PendingIntent alarmIntent = PendingIntent.getBroadcast(this.mContext, 0, intent, Dataspace.RANGE_FULL);
            if (sRTCScheduled) {
                this.mAlarmManager.setExact(1, nextScheduledTime, alarmIntent);
            } else {
                this.mAlarmManager.setExact(3, SystemClock.elapsedRealtime() + delay, alarmIntent);
            }
        } catch (Exception e) {
        }
    }

    private DayDuration getTargetDayDuration(int year, int month, int day) {
        DayDuration duration = new DayDuration();
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, 0, 0, 0);
        duration.mStart = calendar.getTimeInMillis();
        calendar.set(year, month, day, 24, 0, 0);
        duration.mEnd = calendar.getTimeInMillis();
        return duration;
    }

    private static void log(String s) {
        VSlog.d(TAG, s);
    }
}