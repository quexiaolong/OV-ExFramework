package com.android.server.oplus.oplusdevicepolicy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.customize.OplusCustomizeManager;
import android.os.oplusdevicepolicy.IOplusDevicePolicyManagerService;
import android.os.oplusdevicepolicy.IOplusDevicePolicyObserver;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.server.oplus.IElsaManager;
import com.android.server.oplus.orms.config.IOrmsConfigConstant;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

/* loaded from: classes.dex */
public class OplusDevicePolicyManagerService extends IOplusDevicePolicyManagerService.Stub {
    private static final String ATTR_NAME = "name";
    private static final String ATTR_TYPE = "type";
    private static final String ATTR_USERID = "userId";
    private static final String ATTR_VALUE = "value";
    private static final int CUSTOMIZE_TYPE = 0;
    private static final int MSG_CUSTOMIZE_FILEUPDATE = 0;
    private static final int MSG_FILE_RESTORE_FINISH = 3;
    private static final int MSG_FILE_RESTORE_START = 2;
    private static final int MSG_NORMAL_FILEUPDATE = 1;
    private static final int MSG_NOTIFY_DATAUPDATE = 4;
    private static final int MSG_NOTIFY_LISTUPDATE = 5;
    private static final int NORMAL_TYPE = 1;
    private static final String OPLUS_DEVICEPOLICY_DATA = "oplus_devicepolicy_data.xml";
    private static final String OPLUS_DEVICEPOLICY_DATA_CUSTOMIZE = "oplus_devicepolicy_data_customize.xml";
    private static final int RSTORE_FILE_DELAY_TIME = 10000;
    private static final int RSTORE_FILE_STATE_CUSTOM_DATAFILE_SUCCESS = 1;
    private static final int RSTORE_FILE_STATE_NORMAL_DATAFILE_SUCCESS = 2;
    private static final int RSTORE_FILE_STATE_START = 0;
    private static final int RSTORE_FILE_STATE_SUCCESS = 3;
    private static final int STORE_FILE_DELAY_TIME = 30000;
    private static final String TAG = "OplusDevicePolicyManagerService";
    private static final String TAG_ITEM = "item";
    private static final String TAG_LIST = "List";
    private static final String TAG_ROOT = "root";
    private static final String TAG_SIGNLEVALUE = "SingleValue";
    private static final String USER_ID = "userid";
    private static final int WAKELOCK_TIMEOUT = 10000;
    private Context mContext;
    private OplusDevicePolicyHandler mHandler;
    private HandlerThread mThread;
    private Users mUsers;
    private static final boolean DEBUG = SystemProperties.getBoolean("persist.sys.opdpm.log", false);
    private static int sSoreFileDelayTime = 2000;
    private static int sFileRestoreSuccess = 0;
    RemoteCallbackList<IOplusDevicePolicyObserver> mObservers = new RemoteCallbackList<>();
    private final Object mCustomizeUpdateLock = new Object();
    private final Object mNormalFileUpdateLock = new Object();
    private final Object mLock = new Object();
    private HashMap<Integer, Users> mUsersCache = new HashMap<>();
    private BroadcastReceiver mOplusDevicePolicyBraodcastReceiver = new BroadcastReceiver() { // from class: com.android.server.oplus.oplusdevicepolicy.OplusDevicePolicyManagerService.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            synchronized (OplusDevicePolicyManagerService.this.mLock) {
                if (intent != null) {
                    if ("android.intent.action.ACTION_SHUTDOWN".equals(intent.getAction()) || "android.intent.action.REBOOT".equals(intent.getAction())) {
                        Log.d(OplusDevicePolicyManagerService.TAG, "receive ACTION_SHUTDOWN or ACTION_REBOOT intent.getAction():" + intent.getAction());
                        PowerManager powerManager = (PowerManager) OplusDevicePolicyManagerService.this.mContext.getSystemService("power");
                        PowerManager.WakeLock mWakeLock = powerManager.newWakeLock(1, OplusDevicePolicyManagerService.TAG.toString());
                        HashMap<Integer, Users> temp = (HashMap) OplusDevicePolicyManagerService.this.mUsersCache.clone();
                        mWakeLock.acquire();
                        OplusDevicePolicyManagerService.this.mHandler.removeMessages(1);
                        OplusDevicePolicyManagerService.this.mHandler.removeMessages(0);
                        OplusDevicePolicyManagerService.this.storeCustomizeData(temp);
                        OplusDevicePolicyManagerService.this.storeNormalData(temp);
                        Log.d(OplusDevicePolicyManagerService.TAG, "end to store the cache data to xml file");
                        mWakeLock.release();
                    }
                }
            }
        }
    };

    public OplusDevicePolicyManagerService(Context context) {
        this.mContext = context;
        Log.d(TAG, "OplusDevicePolicyManagerService start");
        HandlerThread handlerThread = new HandlerThread(TAG);
        this.mThread = handlerThread;
        handlerThread.start();
        if (this.mThread.getLooper() != null) {
            this.mHandler = new OplusDevicePolicyHandler(this.mThread.getLooper());
        }
        this.mHandler.sendEmptyMessage(2);
        Log.d(TAG, "successfully sent message to do RestoreDataFromFile job.");
    }

    public boolean isOplusDevicePolicyReady() {
        if (DEBUG) {
            Log.d(TAG, "isOplusDevicePolicyReady sFileRestoreSuccess = " + sFileRestoreSuccess);
        }
        if (sFileRestoreSuccess == 3) {
            return true;
        }
        return false;
    }

    public void systemReady() {
        Slog.i(TAG, "systemReady");
        sSoreFileDelayTime = 30000;
        registerOplusDevicePolicyBroadcastReceiver();
    }

    private void registerOplusDevicePolicyBroadcastReceiver() {
        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction("android.intent.action.ACTION_SHUTDOWN");
        mFilter.addAction("android.intent.action.REBOOT");
        this.mContext.registerReceiver(this.mOplusDevicePolicyBraodcastReceiver, mFilter);
    }

    private boolean checkPermission() {
        int uid = Binder.getCallingUid();
        if (DEBUG) {
            Log.d(TAG, "Callling uid :" + UserHandle.getAppId(uid));
        }
        if (UserHandle.getAppId(uid) != 1000 && !OplusCustomizeManager.getInstance().isPlatformSigned(uid)) {
            return false;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Users getUserFromCache(int userId) {
        Users users;
        synchronized (this.mLock) {
            users = this.mUsersCache.get(Integer.valueOf(userId));
            if (users == null) {
                if (DEBUG) {
                    Log.d(TAG, "getUserFromCache userId = " + userId + " is null, build a new");
                }
                users = new Users(userId);
                this.mUsersCache.put(Integer.valueOf(userId), users);
            }
        }
        return users;
    }

    private void updateCustomizeFileHandleMsg() {
        if (DEBUG) {
            Log.d(TAG, "updateCustomizeFileHandleMsg");
        }
        if (!this.mHandler.hasMessages(0)) {
            this.mHandler.sendEmptyMessageDelayed(0, sSoreFileDelayTime);
        }
    }

    private void updateFileHandleMsg() {
        if (DEBUG) {
            Log.d(TAG, "updateFileHandleMsg");
        }
        if (!this.mHandler.hasMessages(1)) {
            this.mHandler.sendEmptyMessageDelayed(1, sSoreFileDelayTime);
        }
    }

    private int getUserId() {
        UserHandle.getCallingUserId();
        return 0;
    }

    @Override // android.os.oplusdevicepolicy.IOplusDevicePolicyManagerService
    public boolean setData(String name, String value, int datatype) {
        int userId = getUserId();
        if (DEBUG) {
            Log.d(TAG, "setData userId = " + userId + ";name: " + name + ";value: " + value + ";datatype:" + datatype);
        }
        if (!isOplusDevicePolicyReady() || !checkPermission()) {
            Log.d(TAG, "Oplus Devciepolicy is not ready, return null object");
            return false;
        }
        synchronized (this.mLock) {
            Users userFromCache = getUserFromCache(userId);
            this.mUsers = userFromCache;
            if (userFromCache == null) {
                return false;
            }
            if (datatype == 1) {
                userFromCache.mOplusCustomizeDataCache.put(name, value);
                this.mUsersCache.put(Integer.valueOf(userId), this.mUsers);
                updateCustomizeFileHandleMsg();
            } else {
                userFromCache.mOplusDataCache.put(name, value);
                this.mUsersCache.put(Integer.valueOf(userId), this.mUsers);
                updateFileHandleMsg();
            }
            upDateDataCallBack(name, value);
            return true;
        }
    }

    @Override // android.os.oplusdevicepolicy.IOplusDevicePolicyManagerService
    public boolean setList(String name, List list, int datatype) {
        int userId = getUserId();
        ArrayList arrayList = new ArrayList(list);
        if (DEBUG) {
            Log.d(TAG, "setList userId = " + userId + ";name: " + name + ";arrayList: " + arrayList + ";datatype:" + datatype);
        }
        if (!isOplusDevicePolicyReady() || !checkPermission()) {
            Log.d(TAG, "Oplus Devciepolicy is not ready, return null object");
            return false;
        }
        synchronized (this.mLock) {
            Users userFromCache = getUserFromCache(userId);
            this.mUsers = userFromCache;
            if (userFromCache == null) {
                return false;
            }
            if (datatype == 1) {
                userFromCache.mOplusCustomizeListCache.put(name, arrayList);
                this.mUsersCache.put(Integer.valueOf(userId), this.mUsers);
                updateCustomizeFileHandleMsg();
            } else {
                userFromCache.mOplusListCache.put(name, arrayList);
                this.mUsersCache.put(Integer.valueOf(userId), this.mUsers);
                updateFileHandleMsg();
            }
            upDateListCallBack(name, arrayList);
            return true;
        }
    }

    @Override // android.os.oplusdevicepolicy.IOplusDevicePolicyManagerService
    public boolean addList(String name, List list, int datatype) {
        int userId = getUserId();
        List<String> arrayList = new ArrayList<>();
        if (DEBUG) {
            Log.d(TAG, "setList userId = " + userId + ";name: " + name + ";arrayList: " + arrayList + ";datatype:" + datatype);
        }
        if (!isOplusDevicePolicyReady() || !checkPermission()) {
            Log.d(TAG, "Oplus Devciepolicy is not ready, return null object");
            return false;
        }
        synchronized (this.mLock) {
            Users userFromCache = getUserFromCache(userId);
            this.mUsers = userFromCache;
            if (userFromCache == null) {
                return false;
            }
            List<String> arrayList2 = getList(name, datatype);
            if (arrayList2 == null) {
                arrayList2 = new ArrayList<>();
            }
            arrayList2.addAll(list);
            LinkedHashSet<String> set = new LinkedHashSet<>(arrayList2);
            arrayList2.clear();
            arrayList2.addAll(set);
            if (datatype == 1) {
                this.mUsers.mOplusCustomizeListCache.put(name, arrayList2);
                this.mUsersCache.put(Integer.valueOf(userId), this.mUsers);
                updateCustomizeFileHandleMsg();
            } else {
                this.mUsers.mOplusListCache.put(name, arrayList2);
                this.mUsersCache.put(Integer.valueOf(userId), this.mUsers);
                updateFileHandleMsg();
            }
            upDateListCallBack(name, arrayList2);
            return true;
        }
    }

    @Override // android.os.oplusdevicepolicy.IOplusDevicePolicyManagerService
    public String getData(String name, int datatype) {
        int userId = getUserId();
        if (!isOplusDevicePolicyReady()) {
            Log.d(TAG, "Oplus Devciepolicy is not ready, return null object");
            return null;
        }
        synchronized (this.mLock) {
            Users userFromCache = getUserFromCache(userId);
            this.mUsers = userFromCache;
            if (userFromCache == null) {
                return null;
            }
            String result = datatype == 1 ? (String) userFromCache.mOplusCustomizeDataCache.get(name) : (String) userFromCache.mOplusDataCache.get(name);
            if (DEBUG) {
                Log.d(TAG, "getData userId = " + userId + ";name:" + name + ";result:" + result + ";datatype:" + datatype);
            }
            return result;
        }
    }

    @Override // android.os.oplusdevicepolicy.IOplusDevicePolicyManagerService
    public List<String> getList(String name, int datatype) {
        int userId = getUserId();
        if (!isOplusDevicePolicyReady()) {
            Log.d(TAG, "Oplus Devciepolicy is not ready, return null object");
            return null;
        }
        synchronized (this.mLock) {
            Users userFromCache = getUserFromCache(userId);
            this.mUsers = userFromCache;
            if (userFromCache == null) {
                return null;
            }
            List<String> list = datatype == 1 ? (List) userFromCache.mOplusCustomizeListCache.get(name) : (List) userFromCache.mOplusListCache.get(name);
            if (DEBUG) {
                Log.d(TAG, "getList userId = " + userId + ";name:" + name + ";list:" + list + ";datatype:" + datatype);
            }
            return list;
        }
    }

    @Override // android.os.oplusdevicepolicy.IOplusDevicePolicyManagerService
    public boolean removeData(String name, int datatype) {
        int userId = getUserId();
        boolean ret = false;
        if (DEBUG) {
            Log.d(TAG, "removeData userId = " + userId + ";name:" + name + ";datatype:" + datatype);
        }
        if (!isOplusDevicePolicyReady() || !checkPermission()) {
            Log.d(TAG, "Oplus Devciepolicy is not ready, return null object");
            return false;
        }
        synchronized (this.mLock) {
            Users users = this.mUsersCache.get(Integer.valueOf(userId));
            this.mUsers = users;
            if (users == null) {
                return false;
            }
            if (users != null) {
                if (datatype == 1) {
                    users.mOplusCustomizeDataCache.remove(name);
                    this.mUsersCache.put(Integer.valueOf(userId), this.mUsers);
                    updateCustomizeFileHandleMsg();
                } else {
                    users.mOplusDataCache.remove(name);
                    this.mUsersCache.put(Integer.valueOf(userId), this.mUsers);
                    updateFileHandleMsg();
                }
                ret = true;
            } else {
                Log.d(TAG, "removeData userId = " + userId + " mUsers is null");
            }
            upDateDataCallBack(name, null);
            return ret;
        }
    }

    @Override // android.os.oplusdevicepolicy.IOplusDevicePolicyManagerService
    public boolean removeList(String name, int datatype) {
        int userId = getUserId();
        boolean ret = false;
        if (DEBUG) {
            Log.d(TAG, "removeList userId = " + userId + ";name:" + name + ";datatype:" + datatype);
        }
        if (!isOplusDevicePolicyReady() || !checkPermission()) {
            Log.d(TAG, "Oplus Devciepolicy is not ready, return null object");
            return false;
        }
        synchronized (this.mLock) {
            Users users = this.mUsersCache.get(Integer.valueOf(userId));
            this.mUsers = users;
            if (users == null) {
                return false;
            }
            if (users != null) {
                if (datatype == 1) {
                    users.mOplusCustomizeListCache.remove(name);
                    this.mUsersCache.put(Integer.valueOf(userId), this.mUsers);
                    updateCustomizeFileHandleMsg();
                } else {
                    users.mOplusListCache.remove(name);
                    this.mUsersCache.put(Integer.valueOf(userId), this.mUsers);
                    updateFileHandleMsg();
                }
                ret = true;
            } else {
                Log.d(TAG, "removeList userId = " + userId + " mUsers is null");
            }
            upDateListCallBack(name, null);
            return ret;
        }
    }

    @Override // android.os.oplusdevicepolicy.IOplusDevicePolicyManagerService
    public boolean removePartListData(String name, List list, int datatype) {
        int userId = getUserId();
        List<String> arrayList = new ArrayList<>();
        if (DEBUG) {
            Log.d(TAG, "setList userId = " + userId + ";name: " + name + ";arrayList: " + arrayList + ";datatype:" + datatype);
        }
        if (!isOplusDevicePolicyReady() || !checkPermission()) {
            Log.d(TAG, "Oplus Devciepolicy is not ready, return null object");
            return false;
        }
        synchronized (this.mLock) {
            Users userFromCache = getUserFromCache(userId);
            this.mUsers = userFromCache;
            if (userFromCache == null) {
                return false;
            }
            List<String> arrayList2 = getList(name, datatype);
            if (arrayList2 == null) {
                return true;
            }
            arrayList2.removeAll(list);
            if (datatype == 1) {
                this.mUsers.mOplusCustomizeListCache.put(name, arrayList2);
                this.mUsersCache.put(Integer.valueOf(userId), this.mUsers);
                updateCustomizeFileHandleMsg();
            } else {
                this.mUsers.mOplusListCache.put(name, arrayList2);
                this.mUsersCache.put(Integer.valueOf(userId), this.mUsers);
                updateFileHandleMsg();
            }
            upDateListCallBack(name, arrayList2);
            return true;
        }
    }

    @Override // android.os.oplusdevicepolicy.IOplusDevicePolicyManagerService
    public boolean clearData(int datatype) {
        int userId = getUserId();
        boolean ret = false;
        if (DEBUG) {
            Log.d(TAG, "removeData userId = " + userId + ";datatype:" + datatype);
        }
        if (!isOplusDevicePolicyReady() || !checkPermission()) {
            Log.d(TAG, "Oplus Devciepolicy is not ready, return null object");
            return false;
        }
        synchronized (this.mLock) {
            Users users = this.mUsersCache.get(Integer.valueOf(userId));
            this.mUsers = users;
            if (users == null) {
                return false;
            }
            if (users != null) {
                if (datatype == 1) {
                    for (String key : users.mOplusCustomizeDataCache.keySet()) {
                        upDateDataCallBack(key, null);
                    }
                    this.mUsers.mOplusCustomizeDataCache.clear();
                    this.mUsersCache.put(Integer.valueOf(userId), this.mUsers);
                    updateCustomizeFileHandleMsg();
                } else {
                    for (String key2 : users.mOplusDataCache.keySet()) {
                        upDateDataCallBack(key2, null);
                    }
                    this.mUsers.mOplusDataCache.clear();
                    this.mUsersCache.put(Integer.valueOf(userId), this.mUsers);
                    updateFileHandleMsg();
                }
                ret = true;
            } else {
                Log.d(TAG, "removeData userId = " + userId + " mUsers is null");
            }
            return ret;
        }
    }

    @Override // android.os.oplusdevicepolicy.IOplusDevicePolicyManagerService
    public boolean clearList(int datatype) {
        int userId = getUserId();
        boolean ret = false;
        if (DEBUG) {
            Log.d(TAG, "removeList userId = " + userId + ";datatype:" + datatype);
        }
        if (!isOplusDevicePolicyReady() || !checkPermission()) {
            Log.d(TAG, "Oplus Devciepolicy is not ready, return null object");
            return false;
        }
        synchronized (this.mLock) {
            Users users = this.mUsersCache.get(Integer.valueOf(userId));
            this.mUsers = users;
            if (users == null) {
                return false;
            }
            if (users != null) {
                if (datatype == 1) {
                    for (String key : users.mOplusCustomizeListCache.keySet()) {
                        upDateListCallBack(key, null);
                    }
                    this.mUsers.mOplusCustomizeListCache.clear();
                    this.mUsersCache.put(Integer.valueOf(userId), this.mUsers);
                    updateCustomizeFileHandleMsg();
                } else {
                    for (String key2 : users.mOplusListCache.keySet()) {
                        upDateListCallBack(key2, null);
                    }
                    this.mUsers.mOplusListCache.clear();
                    this.mUsersCache.put(Integer.valueOf(userId), this.mUsers);
                    updateFileHandleMsg();
                }
                ret = true;
            } else {
                Log.d(TAG, "removeList userId = " + userId + " mUsers is null");
            }
            return ret;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Users {
        int mUserId;
        private HashMap<String, List<String>> mOplusCustomizeListCache = new HashMap<>();
        private HashMap<String, String> mOplusCustomizeDataCache = new HashMap<>();
        private HashMap<String, List<String>> mOplusListCache = new HashMap<>();
        private HashMap<String, String> mOplusDataCache = new HashMap<>();

        public Users(int userId) {
            this.mUserId = userId;
        }

        private boolean updateListDataCached(String listname, List<String> list, int datatype) {
            List<String> arrlist = new ArrayList<>(list);
            switch (datatype) {
                case 0:
                    if (OplusDevicePolicyManagerService.DEBUG) {
                        Log.d(OplusDevicePolicyManagerService.TAG, "mOplusCustomizeListCache listname is :" + listname + ";arrlist:" + arrlist);
                    }
                    this.mOplusCustomizeListCache.put(listname, arrlist);
                    return true;
                case 1:
                    if (OplusDevicePolicyManagerService.DEBUG) {
                        Log.d(OplusDevicePolicyManagerService.TAG, "mOplusListCache listname is :" + listname + ";arrlist:" + arrlist);
                    }
                    this.mOplusListCache.put(listname, arrlist);
                    return true;
                default:
                    return false;
            }
        }

        private boolean updateSingleDataCached(String name, String value, int datatype) {
            switch (datatype) {
                case 0:
                    if (OplusDevicePolicyManagerService.DEBUG) {
                        Log.d(OplusDevicePolicyManagerService.TAG, "mOplusCustomizeDataCache name is :" + name + ";value:" + value);
                    }
                    this.mOplusCustomizeDataCache.put(name, value);
                    return true;
                case 1:
                    if (OplusDevicePolicyManagerService.DEBUG) {
                        Log.d(OplusDevicePolicyManagerService.TAG, "mOplusDataCache name is :" + name + ";value:" + value);
                    }
                    this.mOplusDataCache.put(name, value);
                    return true;
                default:
                    return false;
            }
        }

        public void readOplusDevicePolicyListFromXml(XmlPullParser parser, String listname, int datatype) {
            int type;
            int count = 0;
            List<String> list = new ArrayList<>();
            while (true) {
                try {
                    type = parser.next();
                } catch (IOException | XmlPullParserException e) {
                    Slog.e(OplusDevicePolicyManagerService.TAG, "Error parsing owners information file", e);
                }
                if (type == 3 && OplusDevicePolicyManagerService.TAG_LIST.equals(parser.getName())) {
                    updateListDataCached(listname, list, datatype);
                    return;
                }
                count++;
                switch (type) {
                    case 2:
                        String tag = parser.getName();
                        if (!"item".equals(tag)) {
                            break;
                        } else {
                            String value1 = parser.getAttributeValue(null, "name");
                            list.add(value1);
                            break;
                        }
                }
            }
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        public void readOplusDevicePolicyDataFromXml(XmlPullParser parser, int datatype) {
            while (true) {
                try {
                    int type = parser.next();
                    if (type == 3 && OplusDevicePolicyManagerService.USER_ID.equals(parser.getName())) {
                        return;
                    }
                    switch (type) {
                        case 2:
                            String tag = parser.getName();
                            char c = 65535;
                            switch (tag.hashCode()) {
                                case -72732183:
                                    if (tag.equals(OplusDevicePolicyManagerService.TAG_SIGNLEVALUE)) {
                                        c = 1;
                                        break;
                                    }
                                    break;
                                case 2368702:
                                    if (tag.equals(OplusDevicePolicyManagerService.TAG_LIST)) {
                                        c = 0;
                                        break;
                                    }
                                    break;
                            }
                            switch (c) {
                                case 0:
                                    String listname = parser.getAttributeValue(null, "name");
                                    readOplusDevicePolicyListFromXml(parser, listname, datatype);
                                    continue;
                                case 1:
                                    String name = parser.getAttributeValue(null, "name");
                                    String value = parser.getAttributeValue(null, "value");
                                    updateSingleDataCached(name, value, datatype);
                                    continue;
                            }
                    }
                } catch (IOException | XmlPullParserException e) {
                    Slog.e(OplusDevicePolicyManagerService.TAG, "Error parsing owners information file", e);
                    return;
                }
            }
        }
    }

    private void upDateListCallBack(String name, List<String> list) {
        List<String> mList;
        if (list == null) {
            mList = new ArrayList<>();
        } else {
            mList = new ArrayList<>(list);
        }
        if (DEBUG) {
            Slog.d(TAG, "upDateListCallBack name:" + name + ";List: " + list + ";mList:" + mList);
        }
        Message msg = Message.obtain(this.mHandler, 5);
        Bundle mBundle = new Bundle();
        mBundle.putString("name", name);
        mBundle.putStringArrayList(IOrmsConfigConstant.TAG_LIST, (ArrayList) mList);
        msg.setData(mBundle);
        msg.sendToTarget();
    }

    private void upDateDataCallBack(String name, String value) {
        if (DEBUG) {
            Slog.d(TAG, "upDateDataCallBack name:" + name + ";value: " + value);
        }
        Message msg = Message.obtain(this.mHandler, 4);
        Bundle mBundle = new Bundle();
        mBundle.putString("name", name);
        mBundle.putString("value", value);
        msg.setData(mBundle);
        msg.sendToTarget();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class OplusDevicePolicyHandler extends Handler {
        public OplusDevicePolicyHandler(Looper looper) {
            super(looper);
        }

        public OplusDevicePolicyHandler(Looper looper, Handler.Callback callback) {
            super(looper, callback);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            HashMap<Integer, Users> temp;
            synchronized (OplusDevicePolicyManagerService.this.mLock) {
                temp = (HashMap) OplusDevicePolicyManagerService.this.mUsersCache.clone();
            }
            try {
                switch (msg.what) {
                    case 0:
                        OplusDevicePolicyManagerService.this.storeCustomizeData(temp);
                        return;
                    case 1:
                        OplusDevicePolicyManagerService.this.storeNormalData(temp);
                        return;
                    case 2:
                        Log.d(OplusDevicePolicyManagerService.TAG, "RestoreDataFromFile start");
                        removeMessages(3);
                        sendEmptyMessageDelayed(3, 10000L);
                        OplusDevicePolicyManagerService.this.restoreCustomizeDataFromFile();
                        OplusDevicePolicyManagerService.this.restoreDataFromFile();
                        int unused = OplusDevicePolicyManagerService.sFileRestoreSuccess = 3;
                        removeMessages(3);
                        Log.d(OplusDevicePolicyManagerService.TAG, "RestoreDataFromFile end");
                        return;
                    case 3:
                        Log.d(OplusDevicePolicyManagerService.TAG, "OPLUS devicepolicy data file restore failed sFileRestoreSuccess = " + OplusDevicePolicyManagerService.sFileRestoreSuccess);
                        if (OplusDevicePolicyManagerService.sFileRestoreSuccess != 3) {
                            if (OplusDevicePolicyManagerService.sFileRestoreSuccess == 0) {
                                File mFile = OplusDevicePolicyManagerService.this.getOplusDevicePolicyCustomizeDataFile();
                                if (mFile.exists() && !mFile.delete()) {
                                    Slog.e(OplusDevicePolicyManagerService.TAG, "Failed to remove " + mFile.getPath());
                                    return;
                                }
                            }
                            if (OplusDevicePolicyManagerService.sFileRestoreSuccess == 1) {
                                File mFile2 = OplusDevicePolicyManagerService.this.getOplusDevicePolicyDataFile();
                                if (mFile2.exists() && !mFile2.delete()) {
                                    Slog.e(OplusDevicePolicyManagerService.TAG, "Failed to remove " + mFile2.getPath());
                                    return;
                                }
                            }
                        }
                        int unused2 = OplusDevicePolicyManagerService.sFileRestoreSuccess = 3;
                        return;
                    case 4:
                        Bundle mValueBundle = msg.getData();
                        OplusDevicePolicyManagerService.this.notifyPolusDevicePolicyValueUpdate(mValueBundle.getString("name"), mValueBundle.getString("value"));
                        return;
                    case 5:
                        Bundle mListBundle = msg.getData();
                        OplusDevicePolicyManagerService.this.notifyPolusDevicePolicyListUpdate(mListBundle.getString("name"), mListBundle.getStringArrayList(IOrmsConfigConstant.TAG_LIST));
                        return;
                    default:
                        return;
                }
            } catch (Exception e) {
                Log.w(OplusDevicePolicyManagerService.TAG, "exception while handlemsg:");
                e.printStackTrace();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static abstract class FileReadWriter {
        private final File mFile;

        abstract boolean readInner(XmlPullParser xmlPullParser, int i, String str);

        abstract boolean writeInner(XmlSerializer xmlSerializer, HashMap<Integer, Users> hashMap) throws IOException;

        protected FileReadWriter(File file) {
            this.mFile = file;
        }

        void writeToFileLocked(HashMap<Integer, Users> mHashMap) {
            if (OplusDevicePolicyManagerService.DEBUG) {
                Log.d(OplusDevicePolicyManagerService.TAG, "Writing to " + this.mFile);
            }
            AtomicFile f = new AtomicFile(this.mFile);
            FileOutputStream outputStream = null;
            try {
                outputStream = f.startWrite();
                XmlSerializer out = new FastXmlSerializer();
                out.setOutput(outputStream, StandardCharsets.UTF_8.name());
                out.startDocument(null, true);
                out.startTag(null, OplusDevicePolicyManagerService.TAG_ROOT);
                writeInner(out, mHashMap);
                out.endTag(null, OplusDevicePolicyManagerService.TAG_ROOT);
                out.endDocument();
                out.flush();
                f.finishWrite(outputStream);
            } catch (IOException e) {
                Slog.e(OplusDevicePolicyManagerService.TAG, "Exception when writing", e);
                if (outputStream != null) {
                    f.failWrite(outputStream);
                }
            }
        }

        void readFromFileLocked() {
            if (!this.mFile.exists()) {
                if (OplusDevicePolicyManagerService.DEBUG) {
                    Log.d(OplusDevicePolicyManagerService.TAG, IElsaManager.EMPTY_PACKAGE + this.mFile + " doesn't exist");
                    return;
                }
                return;
            }
            if (OplusDevicePolicyManagerService.DEBUG) {
                Log.d(OplusDevicePolicyManagerService.TAG, "Reading from " + this.mFile);
            }
            AtomicFile f = new AtomicFile(this.mFile);
            InputStream input = null;
            try {
                try {
                    input = f.openRead();
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setInput(input, StandardCharsets.UTF_8.name());
                    int depth = 0;
                    while (true) {
                        int type = parser.next();
                        if (type != 1) {
                            switch (type) {
                                case 2:
                                    depth++;
                                    String tag = parser.getName();
                                    if (depth != 1) {
                                        if (readInner(parser, depth, tag)) {
                                            break;
                                        } else {
                                            return;
                                        }
                                    } else if (!OplusDevicePolicyManagerService.TAG_ROOT.equals(tag)) {
                                        Slog.e(OplusDevicePolicyManagerService.TAG, "Invalid root tag: " + tag);
                                        return;
                                    } else {
                                        break;
                                    }
                                case 3:
                                    depth--;
                                    break;
                            }
                        }
                    }
                } catch (IOException | XmlPullParserException e) {
                    Slog.e(OplusDevicePolicyManagerService.TAG, "Error parsing owners information file", e);
                }
            } finally {
                IoUtils.closeQuietly(input);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void storeCustomizeData(HashMap<Integer, Users> mHashMap) {
        synchronized (this.mCustomizeUpdateLock) {
            if (DEBUG) {
                Log.d(TAG, "Writing to Customize data file");
            }
            new OplusDeviceCustomizeReadWriter().writeToFileLocked(mHashMap);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void storeNormalData(HashMap<Integer, Users> mHashMap) {
        synchronized (this.mNormalFileUpdateLock) {
            if (DEBUG) {
                Log.d(TAG, "Writing to data file");
            }
            new OplusDevicePolicyReadWriter().writeToFileLocked(mHashMap);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void restoreCustomizeDataFromFile() {
        if (DEBUG) {
            Log.d(TAG, "read Customize data file ");
        }
        new OplusDeviceCustomizeReadWriter().readFromFileLocked();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void restoreDataFromFile() {
        if (DEBUG) {
            Log.d(TAG, "read normal data file ");
        }
        new OplusDevicePolicyReadWriter().readFromFileLocked();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class OplusDeviceCustomizeReadWriter extends FileReadWriter {
        protected OplusDeviceCustomizeReadWriter() {
            super(OplusDevicePolicyManagerService.this.getOplusDevicePolicyCustomizeDataFile());
        }

        @Override // com.android.server.oplus.oplusdevicepolicy.OplusDevicePolicyManagerService.FileReadWriter
        boolean writeInner(XmlSerializer out, HashMap<Integer, Users> mUsersCacheTemp) throws IOException {
            for (Integer userid : mUsersCacheTemp.keySet()) {
                out.startTag(null, OplusDevicePolicyManagerService.USER_ID);
                out.attribute(null, OplusDevicePolicyManagerService.ATTR_USERID, String.valueOf(userid));
                Users mStoreUsers = mUsersCacheTemp.get(userid);
                for (String keyset : mStoreUsers.mOplusCustomizeListCache.keySet()) {
                    out.startTag(null, OplusDevicePolicyManagerService.TAG_LIST);
                    out.attribute(null, "name", keyset);
                    for (String p : (List) mStoreUsers.mOplusCustomizeListCache.get(keyset)) {
                        out.startTag(null, "item");
                        out.attribute(null, "name", p);
                        out.endTag(null, "item");
                    }
                    out.endTag(null, OplusDevicePolicyManagerService.TAG_LIST);
                }
                for (String keyset2 : mStoreUsers.mOplusCustomizeDataCache.keySet()) {
                    out.startTag(null, OplusDevicePolicyManagerService.TAG_SIGNLEVALUE);
                    out.attribute(null, "name", keyset2);
                    String mValue = (String) mStoreUsers.mOplusCustomizeDataCache.get(keyset2);
                    out.attribute(null, "value", mValue);
                    out.endTag(null, OplusDevicePolicyManagerService.TAG_SIGNLEVALUE);
                }
                out.endTag(null, OplusDevicePolicyManagerService.USER_ID);
            }
            return true;
        }

        @Override // com.android.server.oplus.oplusdevicepolicy.OplusDevicePolicyManagerService.FileReadWriter
        boolean readInner(XmlPullParser parser, int depth, String tag) {
            char c;
            switch (tag.hashCode()) {
                case -836029914:
                    if (tag.equals(OplusDevicePolicyManagerService.USER_ID)) {
                        c = 0;
                        break;
                    }
                default:
                    c = 65535;
                    break;
            }
            switch (c) {
                case 0:
                    int mUserId = Integer.parseInt(parser.getAttributeValue(null, OplusDevicePolicyManagerService.ATTR_USERID));
                    Users users = OplusDevicePolicyManagerService.this.getUserFromCache(mUserId);
                    users.readOplusDevicePolicyDataFromXml(parser, 0);
                    OplusDevicePolicyManagerService.this.mUsersCache.put(Integer.valueOf(mUserId), users);
                    int unused = OplusDevicePolicyManagerService.sFileRestoreSuccess = 1;
                    return true;
                default:
                    Slog.e(OplusDevicePolicyManagerService.TAG, "Unexpected tag: " + tag);
                    return false;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class OplusDevicePolicyReadWriter extends FileReadWriter {
        protected OplusDevicePolicyReadWriter() {
            super(OplusDevicePolicyManagerService.this.getOplusDevicePolicyDataFile());
        }

        @Override // com.android.server.oplus.oplusdevicepolicy.OplusDevicePolicyManagerService.FileReadWriter
        boolean writeInner(XmlSerializer out, HashMap<Integer, Users> mUsersCacheTemp) throws IOException {
            for (Integer userid : mUsersCacheTemp.keySet()) {
                out.startTag(null, OplusDevicePolicyManagerService.USER_ID);
                out.attribute(null, OplusDevicePolicyManagerService.ATTR_USERID, String.valueOf(userid));
                Users mStoreUsers = mUsersCacheTemp.get(userid);
                for (String keyset : mStoreUsers.mOplusListCache.keySet()) {
                    out.startTag(null, OplusDevicePolicyManagerService.TAG_LIST);
                    out.attribute(null, "name", keyset);
                    for (String p : (List) mStoreUsers.mOplusListCache.get(keyset)) {
                        out.startTag(null, "item");
                        out.attribute(null, "name", p);
                        out.endTag(null, "item");
                    }
                    out.endTag(null, OplusDevicePolicyManagerService.TAG_LIST);
                }
                for (String keyset2 : mStoreUsers.mOplusDataCache.keySet()) {
                    out.startTag(null, OplusDevicePolicyManagerService.TAG_SIGNLEVALUE);
                    out.attribute(null, "name", keyset2);
                    String mValue = (String) mStoreUsers.mOplusDataCache.get(keyset2);
                    out.attribute(null, "value", mValue);
                    out.endTag(null, OplusDevicePolicyManagerService.TAG_SIGNLEVALUE);
                }
                out.endTag(null, OplusDevicePolicyManagerService.USER_ID);
            }
            return true;
        }

        @Override // com.android.server.oplus.oplusdevicepolicy.OplusDevicePolicyManagerService.FileReadWriter
        boolean readInner(XmlPullParser parser, int depth, String tag) {
            char c;
            switch (tag.hashCode()) {
                case -836029914:
                    if (tag.equals(OplusDevicePolicyManagerService.USER_ID)) {
                        c = 0;
                        break;
                    }
                default:
                    c = 65535;
                    break;
            }
            switch (c) {
                case 0:
                    int mUserId = Integer.parseInt(parser.getAttributeValue(null, OplusDevicePolicyManagerService.ATTR_USERID));
                    Users users = OplusDevicePolicyManagerService.this.getUserFromCache(mUserId);
                    users.readOplusDevicePolicyDataFromXml(parser, 1);
                    OplusDevicePolicyManagerService.this.mUsersCache.put(Integer.valueOf(mUserId), users);
                    int unused = OplusDevicePolicyManagerService.sFileRestoreSuccess = 2;
                    return true;
                default:
                    Slog.e(OplusDevicePolicyManagerService.TAG, "Unexpected tag: " + tag);
                    return false;
            }
        }
    }

    File getOplusDevicePolicyCustomizeDataFile() {
        return new File(Environment.getDataSystemDirectory(), OPLUS_DEVICEPOLICY_DATA_CUSTOMIZE);
    }

    File getOplusDevicePolicyDataFile() {
        return new File(Environment.getDataSystemDirectory(), OPLUS_DEVICEPOLICY_DATA);
    }

    @Override // android.os.Binder
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (!DumpUtils.checkDumpAndUsageStatsPermission(this.mContext, TAG, pw)) {
            return;
        }
        pw.println("Dump Oplus Devicepolicy Service");
        synchronized (this.mLock) {
            for (Integer userid : this.mUsersCache.keySet()) {
                pw.println("userid:" + userid);
                pw.println("Customize Data:");
                Users mDumpUsers = this.mUsersCache.get(userid);
                for (String keyset : mDumpUsers.mOplusCustomizeListCache.keySet()) {
                    pw.println("List: " + keyset + " = " + mDumpUsers.mOplusCustomizeListCache.get(keyset));
                }
                for (String keyset2 : mDumpUsers.mOplusCustomizeDataCache.keySet()) {
                    pw.println("Data: " + keyset2 + " = " + ((String) mDumpUsers.mOplusCustomizeDataCache.get(keyset2)));
                }
                pw.println("Normal Data:");
                for (String keyset3 : mDumpUsers.mOplusListCache.keySet()) {
                    pw.println("List: " + keyset3 + " = " + mDumpUsers.mOplusListCache.get(keyset3));
                }
                for (String keyset4 : mDumpUsers.mOplusDataCache.keySet()) {
                    pw.println("Data: " + keyset4 + " = " + ((String) mDumpUsers.mOplusDataCache.get(keyset4)));
                }
            }
        }
    }

    @Override // android.os.oplusdevicepolicy.IOplusDevicePolicyManagerService
    public boolean registerOplusDevicePolicyObserver(String name, IOplusDevicePolicyObserver observer) {
        Slog.i(TAG, "registerOplusDevicePolicyObserver pid " + Binder.getCallingPid() + " uid " + Binder.getCallingUid());
        if (name == null || name.equals(IElsaManager.EMPTY_PACKAGE)) {
            return false;
        }
        synchronized (this.mObservers) {
            this.mObservers.register(observer, name);
        }
        return true;
    }

    @Override // android.os.oplusdevicepolicy.IOplusDevicePolicyManagerService
    public boolean unregisterOplusDevicePolicyObserver(IOplusDevicePolicyObserver observer) {
        boolean ret;
        if (DEBUG) {
            Slog.i(TAG, "unregisterOplusDevicePolicyObserver pid " + Binder.getCallingPid() + " uid " + Binder.getCallingUid() + ";observer:" + observer);
        }
        synchronized (this.mObservers) {
            if (observer != null) {
                try {
                    this.mObservers.unregister(observer);
                    ret = true;
                } catch (Throwable th) {
                    throw th;
                }
            } else {
                ret = false;
            }
        }
        return ret;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyPolusDevicePolicyValueUpdate(String name, String value) {
        synchronized (this.mObservers) {
            int count = this.mObservers.beginBroadcast();
            for (int i = 0; i < count; i++) {
                if (DEBUG) {
                    Log.d(TAG, "mObservers Number is " + count + ";i = " + i + ";Cookie:" + this.mObservers.getBroadcastCookie(i) + ";Item:" + this.mObservers.getBroadcastItem(i) + ";name:" + name + ";value:" + value);
                }
                try {
                    if (this.mObservers.getBroadcastCookie(i).equals(name)) {
                        IOplusDevicePolicyObserver observer = this.mObservers.getBroadcastItem(i);
                        observer.onOplusDevicePolicyValueUpdate(name, value);
                    }
                } catch (RemoteException e) {
                    Slog.e(TAG, "notifyFeaturesUpdate onFeatureUpdate ", e);
                }
            }
            this.mObservers.finishBroadcast();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void notifyPolusDevicePolicyListUpdate(String name, List<String> list) {
        synchronized (this.mObservers) {
            int count = this.mObservers.beginBroadcast();
            for (int i = 0; i < count; i++) {
                List<String> mList = new ArrayList<>(list);
                if (DEBUG) {
                    Log.d(TAG, "mObservers Number is " + count + ";i = " + i + ";Cookie:" + this.mObservers.getBroadcastCookie(i) + ";Item:" + this.mObservers.getBroadcastItem(i) + ";name:" + name + ";mList:" + mList);
                }
                try {
                    if (this.mObservers.getBroadcastCookie(i).equals(name)) {
                        IOplusDevicePolicyObserver observer = this.mObservers.getBroadcastItem(i);
                        observer.onOplusDevicePolicyListUpdate(name, mList);
                    }
                } catch (RemoteException e) {
                    Slog.e(TAG, "notifyFeaturesUpdate onFeatureUpdate ", e);
                }
            }
            this.mObservers.finishBroadcast();
        }
    }
}