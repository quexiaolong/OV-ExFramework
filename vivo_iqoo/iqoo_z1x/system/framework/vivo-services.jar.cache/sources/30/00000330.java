package com.android.server.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Xml;
import com.vivo.aiengine.flsdk.FlRequest;
import com.vivo.aiengine.flsdk.FlResponse;
import com.vivo.aiengine.flsdk.nlp.INlpCallback;
import com.vivo.aiengine.flsdk.nlp.INlpRequest;
import com.vivo.face.common.data.Constants;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;
import vivo.util.AESUtils;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class NotificationClassifyManager {
    private static final String ALLOW = "allow";
    private static final boolean ALLOW_OVERSEAS = false;
    private static final String CLASSIFY = "NotificationClassify";
    private static final String CONFIG_XML_PATH = "/data/bbkcore/notification_classify_config.xml";
    private static final boolean DEFAULT_ALLOW = true;
    private static final int DEFAULT_IMPORTANCE = -1;
    private static final int DEFAULT_IMPORTANCE_FOR_LUCKY_MONEY = 3;
    private static final int DEFAULT_IMPORTANCE_FOR_PROCESSBAR = -1;
    public static final int IMPORTANCE_DEFAULT = -1;
    public static final int IMPORTANCE_HIGH = 3;
    public static final int IMPORTANCE_INVALID = Integer.MIN_VALUE;
    public static final int IMPORTANCE_NON = 1;
    public static final int IMPORTANCE_NORMAL = 2;
    private static final int INTERNAL_NOTIFICATION_TYPE_PUSH = 1;
    private static final String KEY_WECHAT_LUCKY_MONEY = "MainUI_User_Last_Msg_Type";
    private static final int MAX_CACHE_INTERNAL_TIME = 10000;
    private static final int MSG_TYPE_WECHAT_LUCKY_MONEY = 436207665;
    private static final String NAME = "name";
    private static final String PKG = "package";
    private static final String REGEX = ".*[\\|]-?[1-9]\\d*$";
    private static final String SEPARATOR = "|";
    private static final String SEPARATOR_SPLIT = "\\|";
    private static final String TAG = "NotificationClassifyManager";
    private static final String TYPE = "notificationClassify";
    private static final String UID = "uid";
    private static final String WECHAT_PACKAGE_NAME = "com.tencent.mm";
    private static final long intervalTime = 600000;
    private static final String key_app_name = "app_name";
    private static final String key_classifies = "classifies";
    private static final String key_classifies_confidence = "confidence";
    private static final String key_classifies_label = "label";
    private static final String key_clearable = "clearable";
    private static final String key_content = "content";
    private static final String key_data = "data";
    private static final String key_error = "errorCode";
    private static final String key_id = "id";
    private static final String key_intent = "intent";
    private static final String key_intent_action = "action";
    private static final String key_intent_categories = "categories";
    private static final String key_intent_component = "component";
    private static final String key_intent_component_className = "className";
    private static final String key_intent_component_packageName = "packageName";
    private static final String key_intent_intentUri = "intentUri";
    private static final String key_isForeground = "isForeground";
    private static final String key_model = "model";
    private static final String key_ongoing = "ongoing";
    private static final String key_package_name = "package_name";
    private static final String key_text = "text";
    private static final String key_title = "title";
    private static final String key_type = "type";
    private static final String key_ver = "ver";
    private static final String key_when = "when";
    private static final String serviceAction = "com.vivo.aiengine.intent.action.NLP_SERVICE";
    private static final String servicePkg = "com.vivo.aiengine";
    private static final int version = 1;
    Context context;
    Handler mHandler;
    private NotificationIntelligentFilterManager mNotificationIntelligentFilterManager;
    NotificationManagerService mNotificationManagerService;
    PackageManager packageManager;
    private static final boolean IS_OVERSEAS = "yes".equals(SystemProperties.get("ro.vivo.product.overseas", "no"));
    private static final long RESTRICTION_INTERVAL = SystemProperties.getLong("persist.sys.notification_classify_interval", 10000);
    private static final int MAX_INVOKE_SERVICE_COUNT = SystemProperties.getInt("persist.sys.notification_classify_max_invoke", 30);
    private static final long SUSPEND_INVOKE_MILLISECONDS = SystemProperties.getLong("persist.sys.notification_classify_suspend", 60000);
    private static final String[] LOCAL_WHITE_LIST = {"com.tencent.mm", "com.tencent.mobileqq"};
    final Object mNotificationClassifyLock = new Object();
    private ArrayMap<String, NotificationClassifiedInternal> mEnqueuedNotificationList = new ArrayMap<>();
    private ArrayMap<String, NotificationClassifiedInternal> mDelayedNotificationList = new ArrayMap<>();
    private boolean mAllow = true;
    private ArrayList<String> mNotDefaultAllowPackagesList = new ArrayList<>();
    private ArrayMap<String, RestrictionRule> mRestrictionRules = new ArrayMap<>();
    INlpRequest mNLPService = null;
    INlpCallback.Stub mNLPResponse = new INlpCallback.Stub() { // from class: com.android.server.notification.NotificationClassifyManager.1
        public void onCallback(FlResponse response) {
            if (NotificationManagerService.DBG) {
                StringBuilder toStringBuilder = new StringBuilder();
                toStringBuilder.append(response.getClass().getName());
                toStringBuilder.append(":");
                toStringBuilder.append(" requestID=");
                toStringBuilder.append(response.getRequestId());
                toStringBuilder.append(" resultCode=");
                toStringBuilder.append(response.getRetCode());
                toStringBuilder.append(" type=");
                toStringBuilder.append(response.getType());
                if (response.getData() != null && !response.getData().isEmpty()) {
                    toStringBuilder.append(" bundle=");
                    toStringBuilder.append(response.getData().toString());
                } else {
                    toStringBuilder.append(" bundle is empty");
                }
                VSlog.v(NotificationClassifyManager.TAG, "onCallback response=" + response.toString());
            }
            synchronized (NotificationClassifyManager.this.mNotificationClassifyLock) {
                if (response != null) {
                    if (!NotificationClassifyManager.this.isJsonResultError(response.getRetCode())) {
                        int index = NotificationClassifyManager.this.mEnqueuedNotificationList.indexOfKey(response.getRequestId());
                        boolean isExist = false;
                        if (index >= 0) {
                            VSlog.v(NotificationClassifyManager.TAG, "notification classified succeeded.");
                            NotificationClassifiedInternal internalRecord = (NotificationClassifiedInternal) NotificationClassifyManager.this.mEnqueuedNotificationList.valueAt(index);
                            if (internalRecord != null) {
                                isExist = true;
                            }
                            if (isExist) {
                                NotificationClassifyManager.this.parseResponse2Record(response, internalRecord);
                                RestrictionRule rule = null;
                                String pkg = internalRecord.getPkg();
                                if (NotificationClassifyManager.this.mRestrictionRules.containsKey(pkg)) {
                                    rule = (RestrictionRule) NotificationClassifyManager.this.mRestrictionRules.get(pkg);
                                }
                                if (rule == null) {
                                    rule = new RestrictionRule();
                                }
                                NotificationClassifyManager.this.mRestrictionRules.put(pkg, rule);
                                rule.setLastClassifyResultOfPkg(internalRecord.getImportance());
                            } else {
                                NotificationClassifyManager.this.mEnqueuedNotificationList.remove(response.getRequestId());
                            }
                            return;
                        }
                        VSlog.d(NotificationClassifyManager.TAG, "notification classified failed.The value of serial returned by the server is not found. requestId=" + response.getRequestId());
                        return;
                    }
                }
                VSlog.d(NotificationClassifyManager.TAG, "notification classified failed.The value of jsonResult is null.");
                if (response != null) {
                    NotificationClassifyManager.this.mEnqueuedNotificationList.remove(response.getRequestId());
                }
            }
        }
    };
    ServiceConnection mServiceConnection = new ServiceConnection() { // from class: com.android.server.notification.NotificationClassifyManager.2
        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service == null) {
                VSlog.d(NotificationClassifyManager.TAG, "bind return service is null");
                return;
            }
            NotificationClassifyManager.this.mNLPService = INlpRequest.Stub.asInterface(service);
            if (NotificationClassifyManager.this.mNLPService != null) {
                NotificationClassifyManager.this.resolveDelayedClassifyRequest();
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            synchronized (NotificationClassifyManager.this.mNotificationClassifyLock) {
                NotificationClassifyManager.this.unBind2Service();
                VSlog.e(NotificationClassifyManager.TAG, "Connection to the Service has been lost.");
            }
        }
    };
    Runnable unbindServiceRunnable = new Runnable() { // from class: com.android.server.notification.NotificationClassifyManager.3
        @Override // java.lang.Runnable
        public void run() {
            synchronized (NotificationClassifyManager.this.mNotificationClassifyLock) {
                NotificationClassifyManager.this.unBind2Service();
            }
        }
    };
    Runnable overrideXmlRunnalbe = new Runnable() { // from class: com.android.server.notification.NotificationClassifyManager.4
        @Override // java.lang.Runnable
        public void run() {
            String str;
            String str2;
            synchronized (NotificationClassifyManager.this.mNotificationClassifyLock) {
                VSlog.v(NotificationClassifyManager.TAG, "save notification classify config.");
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                NotificationClassifyManager.this.writeXml(baos, NotificationClassifyManager.this.mNotDefaultAllowPackagesList);
                File dstFile = new File(NotificationClassifyManager.CONFIG_XML_PATH);
                if (dstFile.exists() && dstFile.isFile()) {
                    dstFile.delete();
                }
                FileWriter fileWriter = null;
                BufferedWriter bufferedWriter = null;
                try {
                    dstFile.createNewFile();
                    fileWriter = new FileWriter(dstFile);
                    bufferedWriter = new BufferedWriter(fileWriter);
                    String content = baos.toString();
                    bufferedWriter.write(AESUtils.aesEncryptForP(content));
                } catch (Exception e) {
                    VSlog.d(NotificationClassifyManager.TAG, "overrideXmlRunnalbe cause exception: " + e.fillInStackTrace());
                    if (bufferedWriter != null) {
                        try {
                            bufferedWriter.close();
                        } catch (Exception ex) {
                            str = NotificationClassifyManager.TAG;
                            str2 = "overrideXmlRunnalbe close resource cause exception: " + ex.fillInStackTrace();
                            VSlog.d(str, str2);
                        }
                    }
                    if (fileWriter != null) {
                        fileWriter.close();
                    }
                    baos.close();
                }
                try {
                    bufferedWriter.close();
                    fileWriter.close();
                    baos.close();
                } catch (Exception ex2) {
                    str = NotificationClassifyManager.TAG;
                    str2 = "overrideXmlRunnalbe close resource cause exception: " + ex2.fillInStackTrace();
                    VSlog.d(str, str2);
                }
            }
        }
    };

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isJsonResultError(int result) {
        switch (result) {
            case -8:
            case -7:
            case -6:
            case -5:
            case -4:
            case -3:
            case -2:
            case -1:
                try {
                    VSlog.e(TAG, "remote call function failed. resultCode: " + result);
                    return true;
                } catch (Exception e) {
                    VSlog.e(TAG, "parse returned json failed.", e);
                    return false;
                }
            default:
                return false;
        }
    }

    public NotificationClassifyManager(PackageManager packageManager, Context context, Handler mHandler, NotificationManagerService notificationManagerService) {
        this.packageManager = packageManager;
        this.context = context;
        this.mHandler = mHandler;
        this.mNotificationManagerService = notificationManagerService;
        this.mNotificationIntelligentFilterManager = new NotificationIntelligentFilterManager(context);
    }

    public void onStart() {
        if (IS_OVERSEAS) {
            return;
        }
        this.mNotificationIntelligentFilterManager.onStart();
        init();
    }

    public void enqueueClassified(StatusBarNotification statusBarNotification) {
        if (IS_OVERSEAS) {
            return;
        }
        if (statusBarNotification == null || (statusBarNotification != null && statusBarNotification.getNotification().isCustomNotification())) {
            VSlog.d(TAG, "notification is null or custom.");
            return;
        }
        synchronized (this.mNotificationClassifyLock) {
            NotificationChannel channel = this.mNotificationManagerService.getInternalService().getNotificationChannel(statusBarNotification.getPackageName(), statusBarNotification.getUid(), statusBarNotification.getNotification().getChannelId());
            if (this.mAllow && areVivoCustomNotificationEnabledForPackage(statusBarNotification.getPackageName(), statusBarNotification.getUid()) && channel != null && channel.isAcceptNotificationClassifyManage()) {
                NotificationClassifiedInternal internalRecord = new NotificationClassifiedInternal();
                String key = statusBarNotification.getKey();
                internalRecord.setKeyOfNotification(key);
                internalRecord.setNotificationPostTime(statusBarNotification.getPostTime());
                FlRequest classifyRequest = new FlRequest();
                classifyRequest.setType(1001);
                String serial = classifyRequest.getRequestId();
                internalRecord.setSerial(serial);
                String pkg = statusBarNotification.getPackageName();
                internalRecord.setPkg(pkg);
                if (preClassify(statusBarNotification, internalRecord)) {
                    this.mEnqueuedNotificationList.put(serial, internalRecord);
                    VSlog.d(TAG, "preproccess notification.");
                    return;
                }
                String jsonNotification = toJsonFormat(statusBarNotification);
                if (jsonNotification != null && (jsonNotification == null || !jsonNotification.isEmpty())) {
                    RestrictionRule rule = null;
                    if (this.mRestrictionRules.containsKey(pkg)) {
                        rule = this.mRestrictionRules.get(pkg);
                    }
                    if (rule == null) {
                        rule = new RestrictionRule();
                    }
                    this.mRestrictionRules.put(pkg, rule);
                    if (rule.isNeedSuspend()) {
                        this.mEnqueuedNotificationList.put(serial, internalRecord);
                        internalRecord.setImportance(rule.getLastClassifyResultOfPkg());
                        VSlog.d(TAG, "not allow to classify by restriction rule. pkg=" + pkg + rule.toString());
                        return;
                    }
                    rule.registerInvokeService();
                    VSlog.d(TAG, "send to service. key=" + key);
                    if (checkConnect()) {
                        this.mEnqueuedNotificationList.put(serial, internalRecord);
                        Bundle classifyContent = new Bundle();
                        classifyContent.putString(key_content, jsonNotification);
                        classifyRequest.setData(classifyContent);
                        send2Service(classifyRequest);
                    } else {
                        VSlog.d(TAG, "Check connection failed,So send jsonNotification until the service ready");
                        internalRecord.setJsonNotification(jsonNotification);
                        if (this.mDelayedNotificationList == null) {
                            this.mDelayedNotificationList = new ArrayMap<>();
                        }
                        this.mDelayedNotificationList.put(serial, internalRecord);
                    }
                    return;
                }
                VSlog.d(TAG, "json notification is null or empty. key=" + statusBarNotification.getKey());
                return;
            }
            VSlog.d(TAG, "pkg=" + statusBarNotification.getPackageName() + " is not allow to classify.");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resolveDelayedClassifyRequest() {
        synchronized (this.mNotificationClassifyLock) {
            if (this.mDelayedNotificationList != null && !this.mDelayedNotificationList.isEmpty()) {
                for (NotificationClassifiedInternal record : this.mDelayedNotificationList.values()) {
                    FlRequest classifyRequest = new FlRequest();
                    classifyRequest.setType(1001);
                    classifyRequest.setRequestId(record.getSerial());
                    Bundle requestData = new Bundle();
                    requestData.putString(key_content, record.getJsonNotification());
                    classifyRequest.setData(requestData);
                    this.mEnqueuedNotificationList.put(classifyRequest.getRequestId(), record);
                    send2Service(classifyRequest);
                }
            }
        }
    }

    public int getNotificationClassifiedResult(String key, long postTime) {
        if (IS_OVERSEAS) {
            return -1;
        }
        int importance = -1;
        synchronized (this.mNotificationClassifyLock) {
            NotificationClassifiedInternal record = findNotificationClassifiedResult(key, postTime);
            if (record != null) {
                importance = record.getImportance();
                String serial = record.getSerial();
                if (this.mEnqueuedNotificationList.containsKey(serial)) {
                    this.mEnqueuedNotificationList.remove(serial);
                }
                if (this.mDelayedNotificationList.containsKey(serial)) {
                    this.mDelayedNotificationList.remove(serial);
                }
                VSlog.d(TAG, "get notification classified result.  " + key + " postTime=" + postTime + " serial=" + serial);
            } else {
                VSlog.d(TAG, "get notification classified result: result not found. key=" + key + " postTime=" + postTime);
            }
        }
        if (NotificationManagerService.DBG) {
            VSlog.v(TAG, "getNotificationClassifiedResult key=" + key + " postTime=" + postTime + " classifiedImportance=" + importance);
        }
        return importance;
    }

    public void removeNotificationClassifiedResult(String key, long postTime) {
        if (IS_OVERSEAS) {
            return;
        }
        synchronized (this.mNotificationClassifyLock) {
            if (NotificationManagerService.DBG) {
                VSlog.d(TAG, "remove notification classified result by key." + key);
            }
            removeClassifiedResultsByKey(key, this.mEnqueuedNotificationList);
            removeClassifiedResultsByKey(key, this.mDelayedNotificationList);
        }
    }

    private void removeClassifiedResultsByKey(String key, ArrayMap<String, NotificationClassifiedInternal> srcList) {
        if (key == null) {
            return;
        }
        Iterator<Map.Entry<String, NotificationClassifiedInternal>> it = srcList.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, NotificationClassifiedInternal> entry = it.next();
            NotificationClassifiedInternal tmp = entry.getValue();
            if (key.equals(tmp.keyOfNotification)) {
                if (NotificationManagerService.DBG) {
                    VSlog.v(TAG, "remove result by key." + tmp.keyOfNotification + " postTime=" + tmp.notificationPostTime + " serial=" + tmp.getSerial());
                }
                it.remove();
            }
        }
    }

    private boolean checkConnect() {
        boolean isConnect = true;
        if (this.mNLPService == null) {
            VSlog.v(TAG, "need to rebind.");
            isConnect = false;
            bind2Service();
        }
        this.mHandler.removeCallbacks(this.unbindServiceRunnable);
        this.mHandler.postDelayed(this.unbindServiceRunnable, intervalTime);
        return isConnect;
    }

    public boolean areVivoCustomNotificationEnabled() {
        if (IS_OVERSEAS) {
            return true;
        }
        return this.mAllow;
    }

    public boolean areVivoCustomNotificationEnabledForPackage(String pkg, int uid) {
        boolean result;
        if (IS_OVERSEAS) {
            return true;
        }
        synchronized (this.mNotificationClassifyLock) {
            result = true;
            if (this.mNotDefaultAllowPackagesList.contains(toKey(pkg, uid))) {
                result = false;
            }
        }
        return result;
    }

    public void setVivoCustomNotificationEnabled(boolean enabled) {
        if (IS_OVERSEAS) {
            return;
        }
        this.mAllow = enabled;
        overrideXml();
    }

    public void setVivoCustomNotificationEnabledForPackage(String pkg, int uid, boolean enabled) {
        if (IS_OVERSEAS) {
            return;
        }
        synchronized (this.mNotificationClassifyLock) {
            try {
                try {
                    this.packageManager.getPackageInfo(pkg, 0);
                    if (true != enabled) {
                        String key = toKey(pkg, uid);
                        if (key != null) {
                            this.mNotDefaultAllowPackagesList.add(key);
                        }
                    } else {
                        this.mNotDefaultAllowPackagesList.remove(toKey(pkg, uid));
                    }
                    overrideXml();
                } catch (PackageManager.NameNotFoundException e) {
                    VSlog.d(TAG, "checkout " + pkg + " NameNotFoundException");
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public void checkWhenAppUnInstalled(Intent intent, int changeUserId) {
        Uri uri;
        String pkgName;
        if (IS_OVERSEAS || (uri = intent.getData()) == null || (pkgName = uri.getSchemeSpecificPart()) == null) {
            return;
        }
        try {
            PackageInfo info = this.packageManager.getPackageInfoAsUser(pkgName, 0, changeUserId);
            int uid = info.applicationInfo.uid;
            boolean isNeed2Write = false;
            synchronized (this.mNotificationClassifyLock) {
                if (this.mNotDefaultAllowPackagesList.contains(toKey(pkgName, uid))) {
                    this.mNotDefaultAllowPackagesList.remove(toKey(pkgName, uid));
                    isNeed2Write = true;
                }
                this.mRestrictionRules.remove(pkgName);
            }
            if (isNeed2Write) {
                overrideXml();
            }
        } catch (PackageManager.NameNotFoundException e) {
        }
    }

    private void overrideXml() {
        this.mHandler.removeCallbacks(this.overrideXmlRunnalbe);
        this.mHandler.postDelayed(this.overrideXmlRunnalbe, 3000L);
    }

    private void init() {
        synchronized (this.mNotificationClassifyLock) {
            File file = new File(CONFIG_XML_PATH);
            String result = readByBufferedReader(file);
            if (result != null) {
                String decryptedResult = AESUtils.aesDecryptForP(result);
                if (decryptedResult != null) {
                    readXml(new ByteArrayInputStream(decryptedResult.getBytes()), this.mNotDefaultAllowPackagesList);
                } else {
                    VSlog.d(TAG, "Decrypt config file failed.");
                }
            }
        }
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
                    while (true) {
                        String line = bufferedReader.readLine();
                        if (line == null) {
                            break;
                        }
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
                VSlog.e(TAG, "Buffered Reader failed! " + e2.fillInStackTrace());
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            }
        } catch (Exception e3) {
        }
        if (buffer != null) {
            return buffer.toString();
        }
        return null;
    }

    private void send2Service(FlRequest request) {
        INlpRequest iNlpRequest = this.mNLPService;
        if (iNlpRequest == null) {
            VSlog.e(TAG, "service is null.");
            this.mEnqueuedNotificationList.remove(request.getRequestId());
            return;
        }
        try {
            iNlpRequest.asyncRequest(request, this.mNLPResponse);
        } catch (RemoteException e) {
            VSlog.e(TAG, "Notification classify failed.", e);
            this.mEnqueuedNotificationList.remove(request.getRequestId());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unBind2Service() {
        VSlog.d(TAG, "unbind service.");
        ServiceConnection serviceConnection = this.mServiceConnection;
        if (serviceConnection != null && this.mNLPService != null) {
            try {
                this.context.unbindService(serviceConnection);
            } catch (Exception e) {
                VSlog.d(TAG, " unbind error:" + e);
            }
            this.mNLPService = null;
            ArrayMap<String, NotificationClassifiedInternal> arrayMap = this.mEnqueuedNotificationList;
            if (arrayMap != null) {
                arrayMap.clear();
            }
            ArrayMap<String, NotificationClassifiedInternal> arrayMap2 = this.mDelayedNotificationList;
            if (arrayMap2 != null) {
                arrayMap2.clear();
            }
            this.mRestrictionRules.clear();
        }
    }

    private void bind2Service() {
        Intent intent = new Intent();
        intent.setAction(serviceAction);
        intent.setPackage(servicePkg);
        this.context.bindService(intent, this.mServiceConnection, 1);
    }

    private NotificationClassifiedInternal findNotificationClassifiedResult(String key, long postTime) {
        int size = this.mEnqueuedNotificationList.size();
        for (int i = 0; i < size; i++) {
            NotificationClassifiedInternal tmp = this.mEnqueuedNotificationList.valueAt(i);
            if (tmp != null && key != null && key.equals(tmp.getKeyOfNotification()) && tmp.getNotificationPostTime() == postTime) {
                return tmp;
            }
        }
        int size2 = this.mDelayedNotificationList.size();
        for (int i2 = 0; i2 < size2; i2++) {
            NotificationClassifiedInternal tmp2 = this.mDelayedNotificationList.valueAt(i2);
            if (tmp2 != null && key != null && key.equals(tmp2.getKeyOfNotification()) && tmp2.getNotificationPostTime() == postTime) {
                return tmp2;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void parseResponse2Record(FlResponse response, NotificationClassifiedInternal internalRecord) {
        if (internalRecord == null) {
            return;
        }
        String type = response.getType() + Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        Bundle data = response.getData();
        if (data == null) {
            VSlog.d(TAG, "parseResponse Error with bundle data = null");
            return;
        }
        int importance = -1;
        try {
            importance = Integer.parseInt(data.getString(key_classifies_label));
        } catch (Exception e) {
            VSlog.e(TAG, "parse classify result error");
        }
        internalRecord.setType(type);
        internalRecord.setImportance(importance);
    }

    private String toJsonFormat(StatusBarNotification n) {
        String appName = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        if (n == null) {
            return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        }
        ApplicationInfo applicationInfo = (ApplicationInfo) n.getNotification().extras.getParcelable("android.appInfo");
        if (applicationInfo != null) {
            if (applicationInfo.loadLabel(this.packageManager) != null) {
                appName = applicationInfo.loadLabel(this.packageManager).toString();
            }
        } else {
            try {
                int userId = n.getUserId();
                String pkg = n.getPackageName();
                appName = this.packageManager.getApplicationInfoAsUser(pkg, 128, userId == -1 ? 0 : userId).loadLabel(this.packageManager).toString();
            } catch (PackageManager.NameNotFoundException e) {
                VSlog.e(TAG, "pkg not found.", e);
                appName = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            }
        }
        String appName2 = n.getPackageName();
        return toJsonFormat(appName2, appName, n.getId(), n.getNotification());
    }

    private String toJsonFormat(String pkg, String appName, int id, Notification notification) {
        Intent intent;
        String intentUri;
        if (notification == null) {
            return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        }
        JSONObject jsonObject = new JSONObject();
        JSONObject jsonObject_Intent = new JSONObject();
        JSONArray jsonArray_Categories = new JSONArray();
        JSONObject jsonObject_Component = new JSONObject();
        if (notification.contentIntent == null) {
            intent = null;
        } else {
            Intent intent2 = notification.contentIntent.getIntent();
            intent = intent2;
        }
        try {
            jsonObject.put(key_package_name, pkg);
            jsonObject.put(key_app_name, appName);
            Bundle extras = notification.extras;
            CharSequence titleCharSequence = extras.getCharSequence("android.title");
            CharSequence textCharSequence = extras.getCharSequence("android.text");
            try {
                if (titleCharSequence == null && textCharSequence == null) {
                    VSlog.d(TAG, "notification title and text is null.");
                    return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                }
                String title = titleCharSequence == null ? Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK : titleCharSequence.toString();
                String text = textCharSequence == null ? Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK : textCharSequence.toString();
                String title2 = title == null ? Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK : title;
                String text2 = text == null ? Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK : text;
                if (title2.isEmpty() && text2.isEmpty()) {
                    VSlog.d(TAG, "notification title and text is null or empty");
                    return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                }
                try {
                    jsonObject.put(key_title, title2);
                    jsonObject.put(key_text, text2);
                    int flags = notification.flags;
                    jsonObject.put(key_isForeground, (flags & 64) != 0);
                    jsonObject.put(key_ongoing, (flags & 2) != 0);
                    jsonObject.put(key_clearable, (flags & 32) != 0);
                    jsonObject.put(key_id, id);
                    jsonObject.put(key_when, notification.when);
                    jsonObject.put(key_intent, jsonObject_Intent);
                    if (intent != null) {
                        jsonObject_Intent.put(key_intent_action, intent.getAction());
                        Set<String> categories = intent.getCategories();
                        if (categories != null) {
                            for (String category : categories) {
                                jsonArray_Categories.put(category);
                            }
                        }
                        jsonObject_Intent.put(key_intent_categories, jsonArray_Categories);
                        ComponentName componentName = intent.getComponent();
                        if (componentName != null) {
                            jsonObject_Component.put(key_intent_component_className, componentName.getClassName());
                            jsonObject_Component.put(key_intent_component_packageName, componentName.getPackageName());
                        }
                        jsonObject_Intent.put(key_intent_component, jsonObject_Component);
                        try {
                            intentUri = intent.toURI();
                        } catch (Exception e) {
                            VSlog.e(TAG, "Intent to URI has a exception.", e.fillInStackTrace());
                            intentUri = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                        }
                        jsonObject_Intent.put(key_intent_intentUri, intentUri);
                    }
                    String result = jsonObject.toString();
                    return result;
                } catch (JSONException e2) {
                    e = e2;
                    VSlog.e(TAG, "Change notification to json failed.", e.fillInStackTrace());
                    return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                }
            } catch (JSONException e3) {
                e = e3;
            }
        } catch (JSONException e4) {
            e = e4;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void writeXml(OutputStream stream, ArrayList<String> dstList) {
        XmlSerializer serializer = Xml.newSerializer();
        try {
            serializer.setOutput(stream, StandardCharsets.UTF_8.name());
            serializer.startDocument(null, true);
            serializer.startTag(null, CLASSIFY);
            serializer.attribute(null, ALLOW, Boolean.toString(this.mAllow));
            for (int i = 0; i < dstList.size(); i++) {
                String key = dstList.get(i);
                if (key != null && key.matches(REGEX)) {
                    String[] array = key.split(SEPARATOR_SPLIT);
                    serializer.startTag(null, PKG);
                    serializer.attribute(null, NAME, array[0]);
                    serializer.attribute(null, UID, array[1]);
                    serializer.endTag(null, PKG);
                }
            }
            serializer.endTag(null, CLASSIFY);
            serializer.endDocument();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readXml(InputStream inputStream, ArrayList<String> dstList) {
        dstList.clear();
        try {
            try {
                try {
                    try {
                        XmlPullParser parser = Xml.newPullParser();
                        parser.setInput(inputStream, null);
                        parser.getName();
                        for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                            String tag = parser.getName();
                            if (eventType == 2) {
                                if (CLASSIFY.equals(tag)) {
                                    this.mAllow = safeBoolean(parser, ALLOW, true);
                                }
                                if (PKG.equals(tag) && parser.getAttributeValue(null, NAME) != null && parser.getAttributeValue(null, UID) != null) {
                                    String pkg = parser.getAttributeValue(null, NAME);
                                    int uid = Integer.valueOf(parser.getAttributeValue(null, UID)).intValue();
                                    String key = toKey(pkg, uid);
                                    if (key != null) {
                                        dstList.add(key);
                                    }
                                }
                            }
                        }
                    } catch (XmlPullParserException e) {
                        e.printStackTrace();
                        if (inputStream == null) {
                            return;
                        }
                        inputStream.close();
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    if (inputStream == null) {
                        return;
                    }
                    inputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Throwable th) {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Exception e2) {
                    }
                }
                throw th;
            }
        } catch (Exception e3) {
        }
    }

    private String toKey(String pkg, int uid) {
        if (pkg != null) {
            return pkg + SEPARATOR + uid;
        }
        return null;
    }

    private static boolean safeBoolean(XmlPullParser parser, String att, boolean defValue) {
        String val = parser.getAttributeValue(null, att);
        return TextUtils.isEmpty(val) ? defValue : Boolean.valueOf(val).booleanValue();
    }

    private boolean preClassify(StatusBarNotification statusBarNotification, NotificationClassifiedInternal record) {
        int filterResult;
        if (statusBarNotification == null) {
            return false;
        }
        Notification n = statusBarNotification.getNotification();
        Bundle extras = n.extras;
        if (n.internalType == 1 && record != null) {
            VSlog.d(TAG, "process notification for vPush");
            record.importance = -1;
            return true;
        } else if (this.mNotificationIntelligentFilterManager.isNotificationNoNeedClassify(statusBarNotification.getPackageName()) && record != null && (filterResult = this.mNotificationIntelligentFilterManager.getTargetNotificationImportance(statusBarNotification)) != -101) {
            record.importance = filterResult;
            return true;
        } else {
            CharSequence contentCharSequence = extras.getCharSequence("android.text");
            String content = contentCharSequence == null ? null : contentCharSequence.toString();
            if ("com.tencent.mm".equals(statusBarNotification.getPackageName()) && content != null && content.contains("[微信红包]") && isTrueLuckyMoneyListener(n) && record != null) {
                record.setImportance(3);
                VSlog.d(TAG, "key=" + statusBarNotification.getKey() + " is lucky money.");
                return true;
            } else if (isProcessBarNotification(n) && record != null) {
                record.setImportance(-1);
                VSlog.d(TAG, "processbar notification.");
                return true;
            } else if ((n.flags & 2) != 0 && record != null) {
                record.setImportance(-1);
                VSlog.d(TAG, "is ongoing notification.");
                return true;
            } else if (n.internalType != 1 && isSystemAppNotification(statusBarNotification) && record != null) {
                record.setImportance(-1);
                VSlog.d(TAG, "is system app and not push notification.");
                return true;
            } else {
                String packageName = statusBarNotification.getPackageName();
                int i = 0;
                while (true) {
                    String[] strArr = LOCAL_WHITE_LIST;
                    if (i >= strArr.length) {
                        return false;
                    }
                    if (!strArr[i].equals(packageName)) {
                        i++;
                    } else {
                        VSlog.d(TAG, "in local white list");
                        return true;
                    }
                }
            }
        }
    }

    private boolean isSystemAppNotification(StatusBarNotification n) {
        ApplicationInfo info = (ApplicationInfo) n.getNotification().extras.getParcelable("android.appInfo");
        if (info == null) {
            try {
                int userId = n.getUserId();
                info = this.packageManager.getApplicationInfoAsUser(n.getPackageName(), 0, userId == -1 ? 0 : userId);
            } catch (PackageManager.NameNotFoundException e) {
                VSlog.e(TAG, "pkg not found.", e);
            }
        }
        return (info == null || ((info.flags & 1) == 0 && (info.flags & 128) == 0)) ? false : true;
    }

    private boolean isTrueLuckyMoneyListener(Notification notification) {
        PendingIntent pendingIntent = notification.contentIntent;
        int TrueWechatValue = 0;
        try {
            VSlog.i(TAG, "isTrueLuckyMoneyListener_pendingIntent=" + pendingIntent.toString());
            Method getIntent = pendingIntent.getClass().getMethod("getIntent", new Class[0]);
            Intent intent = (Intent) getIntent.invoke(pendingIntent, new Object[0]);
            if (intent != null) {
                Bundle extras = intent.getExtras();
                TrueWechatValue = extras.getInt(KEY_WECHAT_LUCKY_MONEY);
            }
            VSlog.i(TAG, "isTrueLuckyMoneyListener=" + TrueWechatValue);
        } catch (Exception e) {
            VSlog.e(TAG, "isTrueLuckyMoneyListener exception.", e.fillInStackTrace());
        }
        if (TrueWechatValue == MSG_TYPE_WECHAT_LUCKY_MONEY) {
            VSlog.i(TAG, "isTrueLuckyMoneyListener_valur=true");
            return true;
        }
        return false;
    }

    private boolean isProcessBarNotification(Notification n) {
        if (n == null) {
            return false;
        }
        Bundle extras = n.extras;
        int max = extras.getInt("android.progressMax", 0);
        boolean ind = extras.getBoolean("android.progressIndeterminate");
        if (max == 0 && !ind) {
            return false;
        }
        return true;
    }

    /* loaded from: classes.dex */
    public class NotificationClassifiedInternal {
        private String pkg;
        String serial;
        int version = -1;
        String type = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        String keyOfNotification = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        String jsonNotification = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        long notificationPostTime = 0;
        int importance = -1;

        public NotificationClassifiedInternal() {
        }

        public void setImportance(int importance) {
            this.importance = importance;
        }

        public int getImportance() {
            return this.importance;
        }

        public void setJsonNotification(String jsonNotification) {
            this.jsonNotification = jsonNotification;
        }

        public String getJsonNotification() {
            return this.jsonNotification;
        }

        public void setSerial(String serial) {
            this.serial = serial;
        }

        public String getSerial() {
            return this.serial;
        }

        public void setKeyOfNotification(String keyOfNotification) {
            this.keyOfNotification = keyOfNotification;
        }

        public String getKeyOfNotification() {
            return this.keyOfNotification;
        }

        public void setNotificationPostTime(long notificationPostTime) {
            this.notificationPostTime = notificationPostTime;
        }

        public long getNotificationPostTime() {
            return this.notificationPostTime;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        public void setType(String type) {
            this.type = type;
        }

        public void setPkg(String pkg) {
            this.pkg = pkg;
        }

        public int getVersion() {
            return this.version;
        }

        public String getType() {
            return this.type;
        }

        public String getPkg() {
            return this.pkg;
        }

        public boolean equals(NotificationClassifiedInternal obj) {
            String str;
            if (obj == null || (str = this.keyOfNotification) == null || !str.equals(obj.keyOfNotification) || this.notificationPostTime != obj.notificationPostTime) {
                return false;
            }
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class RestrictionRule {
        private int lastClassifyResultOfPkg = -1;
        private long nextAllowTimeMillis = -1;
        ArrayList<Long> millisecondRecords = new ArrayList<>(NotificationClassifyManager.MAX_INVOKE_SERVICE_COUNT);

        RestrictionRule() {
        }

        public void registerInvokeService() {
            long now = SystemClock.elapsedRealtime();
            long last = now - NotificationClassifyManager.RESTRICTION_INTERVAL;
            Iterator<Long> iterator = this.millisecondRecords.iterator();
            while (iterator.hasNext()) {
                Long millisReocrd = iterator.next();
                if (millisReocrd.longValue() < last) {
                    iterator.remove();
                }
            }
            if (now < this.nextAllowTimeMillis) {
                return;
            }
            int size = this.millisecondRecords.size();
            if (size + 1 > NotificationClassifyManager.MAX_INVOKE_SERVICE_COUNT) {
                this.nextAllowTimeMillis = NotificationClassifyManager.SUSPEND_INVOKE_MILLISECONDS + now;
                this.millisecondRecords.clear();
                return;
            }
            this.millisecondRecords.add(Long.valueOf(now));
        }

        public boolean isNeedSuspend() {
            long now = SystemClock.elapsedRealtime();
            return now < this.nextAllowTimeMillis;
        }

        public void setLastClassifyResultOfPkg(int importance) {
            this.lastClassifyResultOfPkg = importance;
        }

        public int getLastClassifyResultOfPkg() {
            return this.lastClassifyResultOfPkg;
        }

        public String toString() {
            String str;
            long now = SystemClock.elapsedRealtime();
            long interval = this.nextAllowTimeMillis - now;
            StringBuilder sb = new StringBuilder();
            sb.append("rule(lastResult=");
            sb.append(this.lastClassifyResultOfPkg);
            sb.append(" count=");
            sb.append(this.millisecondRecords.size());
            sb.append(" state=");
            if (interval < 0) {
                str = NotificationClassifyManager.ALLOW;
            } else {
                str = String.valueOf(interval) + " ms";
            }
            sb.append(str);
            sb.append(")");
            return sb.toString();
        }
    }
}