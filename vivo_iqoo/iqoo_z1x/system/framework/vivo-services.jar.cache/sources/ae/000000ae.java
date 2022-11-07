package com.android.server.am;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.util.Xml;
import com.android.server.UnifiedConfigThread;
import com.android.server.am.frozen.FrozenQuicker;
import com.vivo.services.superresolution.Constant;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class EmergencyBroadcastManager {
    private static final String ACTION_UNIFIED_CONFIG_UPDATE = "com.vivo.daemonService.unifiedconfig.update_finish_broadcast_EmergencyBroadcast";
    private static final String BACKUP_EMERGENCY_BROADCAST_FILE_PATH = "/data/bbkcore/vivo_emergency_broadcast.xml";
    private static final String CONTENT_URI = "content://com.vivo.abe.unifiedconfig.provider/configs";
    private static final boolean DBG = ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT;
    public static final int FLAG_RECEIVER_EMERGENCY = 4096;
    public static final int FLAG_RECEIVER_KEYAPP = 8192;
    private static final String TAG = "EmergencyBroadcastManager";
    BroadcastEpmParam BgEpmParam;
    BroadcastEpmParam FgEpmParam;
    BroadcastEpmParam keyActionEpmParam;
    private Context mContext;
    private ActivityManagerService mService;
    boolean bEnableEmergencyBroadcast = true;
    ArrayList<String> emergencyBroadcastActionList = new ArrayList<>();
    ArrayList<String> emergencyReceiverList = new ArrayList<>();
    ArrayList<String> keyAppList = new ArrayList<>();
    final ArrayList<String> defaultEmergencyBroadcastActionList = new ArrayList<String>() { // from class: com.android.server.am.EmergencyBroadcastManager.1
        {
            add("launcher.action.CHANGE_APPLICATION_NOTIFICATION_NUM");
            add("android.intent.action.TIME_TICK");
            add("com.vivo.ted.parser");
            add("android.intent.action.PRE_BOOT_COMPLETED");
            add("android.intent.action.ALARM_KICK_NIGHT_PEARL_0");
            add("android.intent.action.ALARM_KICK_NIGHT_PEARL_4096");
            add("android.intent.action.ALARM_PROX_NIGHT_PEARL_0");
            add("android.intent.action.ALARM_PROX_NIGHT_PEARL_4096");
            add("com.android.incallui.ACTION_DECLINE_INCOMING_CALL");
            add("com.android.incallui.ACTION_HANG_UP_CALL");
            add("android.intent.action.NEW_OUTGOING_CALL");
            add("com.cn.google.AlertClock.ALARM_ALERT");
            add("org.codeaurora.poweroffalarm.action.SET_ALARM");
            add("org.codeaurora.poweroffalarm.action.CANCEL_ALARM");
            add("vivo.intent.action.CHANGE_STATE");
            add("com.android.deskclock.action.TIMER_EXPIRED");
            add("com.vivo.mms.transaction.MESSAGE_SENT");
            add("android.provider.Telephony.SMS_RECEIVED");
            add("android.provider.Telephony.SMS_DELIVER");
            add("android.provider.Telephony.SECRET_CODE");
        }
    };
    final ArrayList<String> defaultEmergencyReceiverList = new ArrayList<String>() { // from class: com.android.server.am.EmergencyBroadcastManager.2
        {
            add("com.tencent.mm/.booter.NotifyReceiver");
        }
    };
    final ArrayList<String> defaultKeyAppList = new ArrayList<String>() { // from class: com.android.server.am.EmergencyBroadcastManager.3
        {
            add(Constant.APP_WEIXIN);
            add("com.bbk.launcher2");
            add("com.android.dialer");
            add("com.android.incallui");
            add("com.android.mms");
            add("com.tencent.mobileqq");
            add("com.eg.android.AlipayGphone");
            add("com.android.contacts");
        }
    };
    private Runnable mReadUnifiedConfigFileRunnable = new Runnable() { // from class: com.android.server.am.EmergencyBroadcastManager.4
        @Override // java.lang.Runnable
        public void run() {
            File serverFile = new File(EmergencyBroadcastManager.BACKUP_EMERGENCY_BROADCAST_FILE_PATH);
            EmergencyBroadcastManager.this.readXmlFileFromUnifiedConfig("EmergencyBroadcast", "1", "1.0", "EmergencyBroadcast", serverFile);
        }
    };
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() { // from class: com.android.server.am.EmergencyBroadcastManager.5
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (EmergencyBroadcastManager.ACTION_UNIFIED_CONFIG_UPDATE.equals(action)) {
                VSlog.d(EmergencyBroadcastManager.TAG, "RECEIVED update_finish_broadcast");
                UnifiedConfigThread.getHandler().post(EmergencyBroadcastManager.this.mReadUnifiedConfigFileRunnable);
            }
        }
    };

    private void registerBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_UNIFIED_CONFIG_UPDATE);
        this.mService.mContext.registerReceiver(this.mIntentReceiver, filter);
    }

    public EmergencyBroadcastManager(ActivityManagerService activityManagerService) {
        this.mContext = activityManagerService.mContext;
        this.mService = activityManagerService;
        init();
    }

    private void init() {
        VSlog.v(TAG, "init. ");
        this.emergencyBroadcastActionList.addAll(this.defaultEmergencyBroadcastActionList);
        this.emergencyReceiverList.addAll(this.defaultEmergencyReceiverList);
        this.keyAppList.addAll(this.defaultKeyAppList);
        this.keyActionEpmParam = new BroadcastEpmParam("keyActionEpmParam", 120000, 50, FrozenQuicker.FREEZE_STATUS_CHECK_MS);
        this.FgEpmParam = new BroadcastEpmParam("FgEpmParam", 50000, 100, 120000);
        this.BgEpmParam = new BroadcastEpmParam("BgEpmParam", 180000, 100, 300000);
    }

    public void systemReady() {
        VSlog.v(TAG, "systemReady. ");
        registerBroadcastReceiver();
        UnifiedConfigThread.getHandler().postDelayed(new Runnable() { // from class: com.android.server.am.EmergencyBroadcastManager.6
            @Override // java.lang.Runnable
            public void run() {
                EmergencyBroadcastManager.this.getEmergencyBroadcastFromXmlFile(EmergencyBroadcastManager.BACKUP_EMERGENCY_BROADCAST_FILE_PATH);
            }
        }, 6000L);
        UnifiedConfigThread.getHandler().postDelayed(this.mReadUnifiedConfigFileRunnable, 80000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean readXmlFileFromUnifiedConfig(String module, String type, String version, String identifier, File file) {
        VSlog.e(TAG, "start readEmergencyBroadcastFromUnifiedConfig ");
        boolean res = false;
        ContentResolver contentResolver = this.mContext.getContentResolver();
        String[] selectionArgs = {"EmergencyBroadcast", "1", "1.0", "EmergencyBroadcast"};
        Cursor cursor = null;
        byte[] fileContent = null;
        try {
            try {
                try {
                    Cursor cursor2 = contentResolver.query(Uri.parse("content://com.vivo.abe.unifiedconfig.provider/configs"), null, null, selectionArgs, null);
                    if (cursor2 != null) {
                        cursor2.moveToFirst();
                        if (cursor2.getCount() > 0) {
                            while (!cursor2.isAfterLast()) {
                                fileContent = cursor2.getBlob(cursor2.getColumnIndex("filecontent"));
                                cursor2.moveToNext();
                            }
                        } else {
                            VSlog.d(TAG, "No data!");
                        }
                    } else {
                        VSlog.d(TAG, "Query failed!");
                    }
                    if (cursor2 != null) {
                        cursor2.close();
                    }
                } catch (Exception e) {
                    VSlog.e(TAG, "open database error: " + e.fillInStackTrace());
                    if (0 != 0) {
                        cursor.close();
                    }
                }
            } catch (Exception e2) {
            }
            if (fileContent != null) {
                res = true;
                String tmpResult = new String(fileContent);
                parseXmlFormatData(new ByteArrayInputStream(tmpResult.getBytes()));
                writeByBufferedWriter(tmpResult, file);
            }
            VSlog.e(TAG, "end readEmergencyBroadcastFromUnifiedConfig ");
            return res;
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    cursor.close();
                } catch (Exception e3) {
                }
            }
            throw th;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean getEmergencyBroadcastFromXmlFile(String srcPath) {
        boolean res = false;
        VSlog.e(TAG, "start getEmergencyBroadcastFromXmlFile ");
        try {
            try {
                File file = new File(srcPath);
                String result = readByBufferedReader(file);
                if (result != null) {
                    parseXmlFormatData(new ByteArrayInputStream(result.getBytes()));
                    res = true;
                }
            } catch (Exception e) {
                VSlog.e(TAG, "decode data error!" + e.fillInStackTrace());
            }
        } catch (Throwable th) {
        }
        VSlog.e(TAG, "end getEmergencyBroadcastFromXmlFile ");
        return res;
    }

    private String readByBufferedReader(File file) {
        if (file == null || !file.exists()) {
            return null;
        }
        BufferedReader bufferedReader = null;
        StringBuffer buffer = null;
        try {
            try {
                try {
                    bufferedReader = new BufferedReader(new FileReader(file));
                    for (String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
                        if (buffer == null) {
                            buffer = new StringBuffer();
                        }
                        buffer.append(line);
                        buffer.append("\n");
                    }
                    bufferedReader.close();
                } catch (Throwable th) {
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (Exception e) {
                        }
                    }
                    throw th;
                }
            } catch (Exception e2) {
            }
        } catch (Exception e3) {
            VSlog.e(TAG, "Buffered Reader failed! " + e3.fillInStackTrace());
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        }
        if (buffer != null) {
            return buffer.toString();
        }
        return null;
    }

    private void writeByBufferedWriter(String string, File desFile) {
        if (string == null) {
            return;
        }
        BufferedWriter bufferedWriter = null;
        try {
            try {
                try {
                    if (desFile.exists() && desFile.isFile()) {
                        desFile.delete();
                    }
                    desFile.createNewFile();
                    bufferedWriter = new BufferedWriter(new FileWriter(desFile));
                    bufferedWriter.write(string);
                    bufferedWriter.close();
                } catch (Throwable th) {
                    if (bufferedWriter != null) {
                        try {
                            bufferedWriter.close();
                        } catch (Exception e) {
                        }
                    }
                    throw th;
                }
            } catch (Exception e2) {
                VSlog.e(TAG, "Buffered write failed! " + e2.fillInStackTrace());
                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }
            }
        } catch (Exception e3) {
        }
    }

    private void parseXmlFormatData(InputStream inputStream) {
        int eventType;
        int enable;
        ArrayList<String> tempEmergencyBroadcastActionList = new ArrayList<>();
        ArrayList<String> tempEmergencyReceiverList = new ArrayList<>();
        ArrayList<String> tempkeyAppList = new ArrayList<>();
        try {
            try {
                try {
                    try {
                        XmlPullParser parser = Xml.newPullParser();
                        parser.setInput(inputStream, StandardCharsets.UTF_8.name());
                        int eventType2 = parser.getEventType();
                        parser.getName();
                        int eventType3 = eventType2;
                        while (true) {
                            if (eventType3 == 1) {
                                break;
                            }
                            String tag = parser.getName();
                            if (eventType3 != 2) {
                                eventType = eventType3;
                            } else if ("switch".equals(tag)) {
                                String itemName = parser.getAttributeValue(null, "name");
                                if ("enable_emergencyBroadcast".equals(itemName)) {
                                    parser.next();
                                    String text = parser.getText();
                                    try {
                                        enable = Integer.parseInt(text);
                                    } catch (NumberFormatException e) {
                                        VSlog.e(TAG, "parser enable_emergencyBroadcast failed! " + e.fillInStackTrace());
                                        enable = 0;
                                    }
                                    if (DBG) {
                                        VSlog.d(TAG, "enable: " + enable);
                                    }
                                    this.bEnableEmergencyBroadcast = enable != 0;
                                }
                                eventType3 = parser.next();
                            } else if ("action".equals(tag)) {
                                String action = parser.getAttributeValue(null, "name");
                                if (DBG) {
                                    VSlog.d(TAG, "action: " + action);
                                }
                                tempEmergencyBroadcastActionList.add(action);
                                eventType = eventType3;
                            } else if ("receiver".equals(tag)) {
                                String receiver = parser.getAttributeValue(null, "name");
                                if (DBG) {
                                    VSlog.d(TAG, "receiver: " + receiver);
                                }
                                tempEmergencyReceiverList.add(receiver);
                                eventType = eventType3;
                            } else if ("keyApp".equals(tag)) {
                                String keyApp = parser.getAttributeValue(null, "name");
                                if (DBG) {
                                    VSlog.d(TAG, "keyApp: " + keyApp);
                                }
                                tempkeyAppList.add(keyApp);
                                eventType = eventType3;
                            } else if ("EpmParam".equals(tag)) {
                                String name = parser.getAttributeValue(null, "name");
                                int dispatchToDoneTime = 0;
                                int maxBrocastCount = 0;
                                int enqueueToDispatchTime = 0;
                                boolean gotEpm = true;
                                try {
                                    dispatchToDoneTime = Integer.parseInt(parser.getAttributeValue(null, "dispatchToDoneTimeout"));
                                    maxBrocastCount = Integer.parseInt(parser.getAttributeValue(null, "maxOrderBrocastCount"));
                                    enqueueToDispatchTime = Integer.parseInt(parser.getAttributeValue(null, "enqueueToDispatchTimeout"));
                                    eventType = eventType3;
                                } catch (NumberFormatException e2) {
                                    StringBuilder sb = new StringBuilder();
                                    eventType = eventType3;
                                    sb.append("parser EpmParam name: ");
                                    sb.append(name);
                                    sb.append("failed! ");
                                    sb.append(e2.fillInStackTrace());
                                    VSlog.e(TAG, sb.toString());
                                    gotEpm = false;
                                }
                                if (DBG && gotEpm) {
                                    VSlog.d(TAG, "EpmParam name: " + name + " dispatchToDoneTime:" + dispatchToDoneTime + " maxBrocastCount:" + maxBrocastCount + " enqueueToDispatchTime:" + enqueueToDispatchTime);
                                }
                                if (gotEpm) {
                                    updateBroadcastEpmParam(name, dispatchToDoneTime, maxBrocastCount, enqueueToDispatchTime);
                                }
                            } else {
                                eventType = eventType3;
                            }
                            eventType3 = parser.next();
                        }
                        synchronized (this.emergencyBroadcastActionList) {
                            this.emergencyBroadcastActionList.clear();
                            this.emergencyReceiverList.clear();
                            this.keyAppList.clear();
                            this.emergencyBroadcastActionList.addAll(tempEmergencyBroadcastActionList);
                            this.emergencyReceiverList.addAll(tempEmergencyReceiverList);
                            this.keyAppList.addAll(tempkeyAppList);
                        }
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    } catch (Throwable th) {
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (Exception e3) {
                            }
                        }
                        throw th;
                    }
                } catch (IOException ioe) {
                    VSlog.d(TAG, "Error reading whitelist ioe: ", ioe);
                    if (inputStream != null) {
                        inputStream.close();
                    }
                }
            } catch (XmlPullParserException e4) {
                VSlog.d(TAG, "Error reading whitelist ext: ", e4);
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        } catch (Exception e5) {
        }
    }

    private void updateBroadcastEpmParam(String name, int dispatchToDoneTimeout, int maxOrderBrocastCount, int enqueueToDispatchTimeout) {
        if ("keyActionEpmParam".equals(name)) {
            this.keyActionEpmParam.dispatchToDoneTimeout = dispatchToDoneTimeout;
            this.keyActionEpmParam.maxOrderBrocastCount = maxOrderBrocastCount;
            this.keyActionEpmParam.enqueueToDispatchTimeout = enqueueToDispatchTimeout;
        } else if ("FgEpmParam".equals(name)) {
            this.FgEpmParam.dispatchToDoneTimeout = dispatchToDoneTimeout;
            this.FgEpmParam.maxOrderBrocastCount = maxOrderBrocastCount;
            this.FgEpmParam.enqueueToDispatchTimeout = enqueueToDispatchTimeout;
        } else if ("BgEpmParam".equals(name)) {
            this.BgEpmParam.dispatchToDoneTimeout = dispatchToDoneTimeout;
            this.BgEpmParam.maxOrderBrocastCount = maxOrderBrocastCount;
            this.BgEpmParam.enqueueToDispatchTimeout = enqueueToDispatchTimeout;
        }
        if (DBG) {
            VSlog.d(TAG, "updateBroadcastEpmParam name: " + name + " dispatchToDoneTimeout:" + dispatchToDoneTimeout + " maxOrderBrocastCount:" + maxOrderBrocastCount + " enqueueToDispatchTimeout:" + enqueueToDispatchTimeout);
        }
    }

    boolean isKeyApp(String callerPackage) {
        synchronized (this.emergencyBroadcastActionList) {
            if (callerPackage != null) {
                return this.keyAppList.contains(callerPackage);
            }
            return false;
        }
    }

    boolean isOnEmergencyQueue(Intent intent) {
        synchronized (this.emergencyBroadcastActionList) {
            String action = intent.getAction();
            if (action != null && this.emergencyBroadcastActionList.contains(action)) {
                return true;
            }
            ComponentName cmp = intent.getComponent();
            if (cmp != null && this.emergencyReceiverList.contains(cmp.flattenToShortString())) {
                return true;
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public BroadcastQueue broadcastQueueForIntent(Intent intent, String callerApp, BroadcastQueue emergencyBroadcastQueue, BroadcastQueue fgKeyAppBroadcastQueue, BroadcastQueue bgKeyAppBroadcastQueue) {
        if (this.bEnableEmergencyBroadcast && (!this.mService.mEnableOffloadQueue || (intent.getFlags() & Integer.MIN_VALUE) == 0)) {
            if (emergencyBroadcastQueue != null && fgKeyAppBroadcastQueue != null && bgKeyAppBroadcastQueue != null) {
                BroadcastQueue bestBroadcastQueue = null;
                boolean isFg = (intent.getFlags() & 268435456) != 0;
                boolean isKeyapp = isKeyApp(callerApp);
                boolean isEmergencyBroadcast = isOnEmergencyQueue(intent);
                int fgBroadcastSize = this.mService.mFgBroadcastQueue.mDispatcher.mOrderedBroadcasts.size();
                int bgBroadcastSize = this.mService.mBgBroadcastQueue.mDispatcher.mOrderedBroadcasts.size();
                int emergencyBroadcastSize = emergencyBroadcastQueue.mDispatcher.mOrderedBroadcasts.size();
                int fgKeyAppBroadcastSize = fgKeyAppBroadcastQueue.mDispatcher.mOrderedBroadcasts.size();
                int bgKeyAppBroadcastSize = bgKeyAppBroadcastQueue.mDispatcher.mOrderedBroadcasts.size();
                if (!isKeyapp && !isEmergencyBroadcast) {
                    return this.mService.broadcastQueueForIntent(intent);
                }
                if (isEmergencyBroadcast) {
                    if (isFg) {
                        if (fgBroadcastSize < emergencyBroadcastSize) {
                            bestBroadcastQueue = this.mService.mFgBroadcastQueue;
                        } else {
                            bestBroadcastQueue = emergencyBroadcastQueue;
                        }
                        if (isKeyapp && fgKeyAppBroadcastSize < bestBroadcastQueue.mDispatcher.mOrderedBroadcasts.size()) {
                            bestBroadcastQueue = fgKeyAppBroadcastQueue;
                        }
                    } else {
                        if (bgBroadcastSize < emergencyBroadcastSize) {
                            bestBroadcastQueue = this.mService.mBgBroadcastQueue;
                        } else {
                            bestBroadcastQueue = emergencyBroadcastQueue;
                        }
                        if (isKeyapp && bgKeyAppBroadcastSize < bestBroadcastQueue.mDispatcher.mOrderedBroadcasts.size()) {
                            bestBroadcastQueue = bgKeyAppBroadcastQueue;
                        }
                    }
                } else if (isKeyapp) {
                    if (isFg) {
                        if (fgBroadcastSize < fgKeyAppBroadcastSize) {
                            bestBroadcastQueue = this.mService.mFgBroadcastQueue;
                        } else {
                            bestBroadcastQueue = fgKeyAppBroadcastQueue;
                        }
                    } else if (bgBroadcastSize < bgKeyAppBroadcastSize) {
                        bestBroadcastQueue = this.mService.mBgBroadcastQueue;
                    } else {
                        bestBroadcastQueue = bgKeyAppBroadcastQueue;
                    }
                }
                if (bestBroadcastQueue == emergencyBroadcastQueue) {
                    intent.addFlags(4096);
                } else if (bestBroadcastQueue == fgKeyAppBroadcastQueue || bestBroadcastQueue == bgKeyAppBroadcastQueue) {
                    intent.addFlags(FLAG_RECEIVER_KEYAPP);
                }
                if (bestBroadcastQueue == null) {
                    bestBroadcastQueue = this.mService.broadcastQueueForIntent(intent);
                }
                VSlog.i("BroadcastQueue", "Broadcast intent " + intent + " on " + bestBroadcastQueue.mQueueName + " queue");
                return bestBroadcastQueue;
            }
        }
        return this.mService.broadcastQueueForIntent(intent);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public BroadcastRecord getMatchingOrderedReceiver(IBinder who, int flags, BroadcastQueue emergencyBroadcastQueue, BroadcastQueue fgKeyAppBroadcastQueue, BroadcastQueue bgKeyAppBroadcastQueue) {
        boolean isFg = (268435456 & flags) != 0;
        if ((flags & 4096) != 0) {
            if (emergencyBroadcastQueue != null) {
                return emergencyBroadcastQueue.getMatchingOrderedReceiver(who);
            }
            return null;
        } else if ((flags & FLAG_RECEIVER_KEYAPP) != 0) {
            if (isFg) {
                if (fgKeyAppBroadcastQueue != null) {
                    return fgKeyAppBroadcastQueue.getMatchingOrderedReceiver(who);
                }
                return null;
            } else if (bgKeyAppBroadcastQueue != null) {
                return bgKeyAppBroadcastQueue.getMatchingOrderedReceiver(who);
            } else {
                return null;
            }
        } else if (isFg) {
            return this.mService.mFgBroadcastQueue.getMatchingOrderedReceiver(who);
        } else {
            return this.mService.mBgBroadcastQueue.getMatchingOrderedReceiver(who);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean dumpBroadcastsLocked(FileDescriptor fd, PrintWriter pw, String[] args, int opti, boolean dumpAll, String dumpPackage) {
        if ("epm".equals(dumpPackage)) {
            synchronized (this.emergencyBroadcastActionList) {
                pw.println("bEnableEmergencyBroadcast :" + this.bEnableEmergencyBroadcast);
                pw.println("emergencyBroadcastActionList :");
                for (int i = 0; i < this.emergencyBroadcastActionList.size(); i++) {
                    String action = this.emergencyBroadcastActionList.get(i);
                    pw.println("  emergencyAction: " + action);
                }
                pw.println("emergencyBroadcastReceiverList :");
                for (int i2 = 0; i2 < this.emergencyReceiverList.size(); i2++) {
                    String receiver = this.emergencyReceiverList.get(i2);
                    pw.println("  emergencyReceiver: " + receiver);
                }
                pw.println("keyAppList :");
                for (int i3 = 0; i3 < this.keyAppList.size(); i3++) {
                    String keyApp = this.keyAppList.get(i3);
                    pw.println("  keyApp: " + keyApp);
                }
                pw.println(this.keyActionEpmParam);
                pw.println(this.BgEpmParam);
                pw.println(this.FgEpmParam);
            }
            return true;
        } else if ("enableEm".equals(dumpPackage)) {
            this.bEnableEmergencyBroadcast = true;
            pw.println("bEnableEmergencyBroadcast :" + this.bEnableEmergencyBroadcast);
            return true;
        } else if ("disableEm".equals(dumpPackage)) {
            this.bEnableEmergencyBroadcast = false;
            pw.println("bEnableEmergencyBroadcast :" + this.bEnableEmergencyBroadcast);
            return true;
        } else if ("rmAction".equals(dumpPackage)) {
            if (args.length >= 3) {
                pw.println("rmAction :" + args[2]);
                synchronized (this.emergencyBroadcastActionList) {
                    if (this.emergencyBroadcastActionList.contains(args[2])) {
                        this.emergencyBroadcastActionList.remove(args[2]);
                    }
                }
            }
            return true;
        } else if ("rmKeyapp".equals(dumpPackage)) {
            if (args.length >= 3) {
                pw.println("rmKeyapp :" + args[2]);
                synchronized (this.emergencyBroadcastActionList) {
                    if (this.keyAppList.contains(args[2])) {
                        this.keyAppList.remove(args[2]);
                    }
                }
            }
            return true;
        } else if ("addAction".equals(dumpPackage)) {
            if (args.length >= 3) {
                pw.println("addAction :" + args[2]);
                synchronized (this.emergencyBroadcastActionList) {
                    if (!this.emergencyBroadcastActionList.contains(args[2])) {
                        this.emergencyBroadcastActionList.add(args[2]);
                    }
                }
            }
            return true;
        } else if ("addKeyapp".equals(dumpPackage)) {
            if (args.length >= 3) {
                pw.println("rmAction :" + args[2]);
                synchronized (this.emergencyBroadcastActionList) {
                    if (!this.emergencyBroadcastActionList.contains(args[2])) {
                        this.emergencyBroadcastActionList.add(args[2]);
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public BroadcastEpmParam getEpmParam(String queueName, Intent intent) {
        if (intent != null) {
            if (isOnEmergencyQueue(intent)) {
                return this.keyActionEpmParam;
            }
            if ("foreground".equals(queueName)) {
                return this.FgEpmParam;
            }
            if ("background".equals(queueName)) {
                return this.BgEpmParam;
            }
            if ("emergency".equals(queueName)) {
                return this.keyActionEpmParam;
            }
            if ("fgKeyApp".equals(queueName)) {
                return this.FgEpmParam;
            }
            if ("bgKeyApp".equals(queueName)) {
                return this.BgEpmParam;
            }
            return this.BgEpmParam;
        }
        return this.BgEpmParam;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static final class BroadcastEpmParam {
        int dispatchToDoneTimeout;
        int enqueueToDispatchTimeout;
        String epmParamname;
        int maxOrderBrocastCount;

        BroadcastEpmParam(String name, int time, int count, int waitTime) {
            this.epmParamname = name;
            this.dispatchToDoneTimeout = time;
            this.maxOrderBrocastCount = count;
            this.enqueueToDispatchTimeout = waitTime;
        }

        public String toString() {
            return this.epmParamname + " dispatchToDoneTimeout:" + this.dispatchToDoneTimeout + " maxOrderBrocastCount:" + this.maxOrderBrocastCount + " enqueueToDispatchTimeout: " + this.enqueueToDispatchTimeout;
        }
    }
}