package com.android.server.location;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import com.android.server.wm.VCD_FF_1;
import com.vivo.common.VivoCollectData;
import com.vivo.face.common.data.Constants;
import com.vivo.vcodetransbase.EventTransfer;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;
import vivo.util.VSlog;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class VivoRequestRecordData {
    private static final int MSG_GET_FROM_LOCATION = 2003;
    private static final int MSG_GET_FROM_LOCATION_NAME = 2004;
    private static final int MSG_HANDLE_LOCATION_CHANGED = 2002;
    private static final int MSG_OVER_TEN_SECOND = 2005;
    private static final int MSG_REPORT_REQ_LOCATION_BIG_DATA_COLLECT_TO_DATABASE = 2000;
    private static final int MSG_REQUEST_LOCATION_UPDATES = 2001;
    private static final String TAG = "VivoRequestRecordData";
    private ConnectivityManager connMgr;
    private HandlerThread mCollectThread;
    private Context mContext;
    private MyHandler mDCHandler;
    private LinkedList<String> mRequestDataList;
    private Object mVCD;
    public static boolean DEBUG = true;
    static boolean NLP_LOCATION_REPORT_SWITCH = true;
    private SimpleDateFormat mSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private int countRequest = 0;
    private int RECORD_MAX_LIMIT = 30;
    private int OVER_TIME = 10;
    private RequestRecord requestRecord = null;

    static /* synthetic */ int access$408(VivoRequestRecordData x0) {
        int i = x0.countRequest;
        x0.countRequest = i + 1;
        return i;
    }

    public VivoRequestRecordData(Context context) {
        this.mContext = null;
        this.mVCD = null;
        this.mCollectThread = null;
        this.mDCHandler = null;
        this.mRequestDataList = null;
        this.mContext = context;
        this.connMgr = (ConnectivityManager) context.getSystemService("connectivity");
        this.mVCD = getVCD(context);
        this.mRequestDataList = new LinkedList<>();
        HandlerThread handlerThread = new HandlerThread("location_request_record_data");
        this.mCollectThread = handlerThread;
        handlerThread.start();
        this.mDCHandler = new MyHandler(this.mCollectThread.getLooper());
    }

    private Object getVCD(Context context) {
        if (this.mVCD == null) {
            this.mVCD = new VivoCollectData(context);
        }
        return this.mVCD;
    }

    /* loaded from: classes.dex */
    public class GeoRecord {
        private String locationName;
        private String packageName;
        private boolean succ;
        private long time;
        private String type;

        public GeoRecord(String pkgName, boolean succ, String type, long time, String loc) {
            this.packageName = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            this.succ = false;
            this.type = "unknow";
            this.time = 0L;
            this.locationName = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            this.packageName = pkgName;
            this.succ = succ;
            this.type = type;
            this.time = time;
            this.locationName = loc;
        }

        public String toString() {
            String result = "geo pkg:" + this.packageName + " succ:" + this.succ + " ty:" + this.type + " time:" + VivoRequestRecordData.this.mSdf.format(Long.valueOf(this.time)) + " loc:" + this.locationName;
            return result;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class RequestRecord {
        private String packageName;
        private long requestTime;
        private boolean success = false;
        private long succTime = 0;
        private int netType = 5;

        public RequestRecord(String pkgName, long reqTime) {
            this.packageName = pkgName;
            this.requestTime = reqTime;
        }

        public void setSuccess(long succ) {
            this.succTime = succ;
            this.success = true;
        }

        public void setNetType(int type) {
            this.netType = type;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("Req pk:" + this.packageName);
            sb.append(" rt:" + this.requestTime);
            sb.append(" s:" + this.success);
            sb.append(" st:" + this.succTime);
            sb.append(" net:" + this.netType);
            return sb.toString();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg == null) {
                return;
            }
            switch (msg.what) {
                case VivoRequestRecordData.MSG_REQUEST_LOCATION_UPDATES /* 2001 */:
                    String packageName = (String) msg.obj;
                    long now = System.currentTimeMillis();
                    VivoRequestRecordData vivoRequestRecordData = VivoRequestRecordData.this;
                    vivoRequestRecordData.requestRecord = new RequestRecord(packageName, now);
                    Message msg1 = VivoRequestRecordData.this.mDCHandler.obtainMessage(VivoRequestRecordData.MSG_OVER_TEN_SECOND);
                    VivoRequestRecordData.this.mDCHandler.sendMessageDelayed(msg1, 10000L);
                    break;
                case VivoRequestRecordData.MSG_HANDLE_LOCATION_CHANGED /* 2002 */:
                    if (VivoRequestRecordData.this.requestRecord != null) {
                        long succ = System.currentTimeMillis();
                        VivoRequestRecordData.this.requestRecord.setSuccess(succ);
                        VivoRequestRecordData.this.mRequestDataList.add(VivoRequestRecordData.this.requestRecord.toString());
                        VivoRequestRecordData.access$408(VivoRequestRecordData.this);
                        VivoRequestRecordData.this.requestRecord = null;
                        break;
                    }
                    break;
                case VivoRequestRecordData.MSG_GET_FROM_LOCATION /* 2003 */:
                    GeoRecord geoRecord1 = (GeoRecord) msg.obj;
                    VivoRequestRecordData.this.mRequestDataList.add(geoRecord1.toString());
                    VivoRequestRecordData.access$408(VivoRequestRecordData.this);
                    break;
                case VivoRequestRecordData.MSG_GET_FROM_LOCATION_NAME /* 2004 */:
                    GeoRecord geoRecord2 = (GeoRecord) msg.obj;
                    VivoRequestRecordData.this.mRequestDataList.add(geoRecord2.toString());
                    VivoRequestRecordData.access$408(VivoRequestRecordData.this);
                    break;
                case VivoRequestRecordData.MSG_OVER_TEN_SECOND /* 2005 */:
                    if (VivoRequestRecordData.this.requestRecord != null) {
                        VivoRequestRecordData vivoRequestRecordData2 = VivoRequestRecordData.this;
                        int netType = vivoRequestRecordData2.getAPNType(vivoRequestRecordData2.mContext);
                        VivoRequestRecordData.this.requestRecord.setNetType(netType);
                        VivoRequestRecordData.this.mRequestDataList.add(VivoRequestRecordData.this.requestRecord.toString());
                        VivoRequestRecordData.access$408(VivoRequestRecordData.this);
                        VivoRequestRecordData.this.requestRecord = null;
                        break;
                    }
                    break;
            }
            if (VivoRequestRecordData.this.countRequest == VivoRequestRecordData.this.RECORD_MAX_LIMIT) {
                VivoRequestRecordData.this.countRequest = 0;
                VivoRequestRecordData.this.handleReportAppRequestLocationToBigDataDatabase();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleReportAppRequestLocationToBigDataDatabase() {
        String content = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        for (int i = 0; i < this.RECORD_MAX_LIMIT; i++) {
            content = content + this.mRequestDataList.removeFirst() + ";";
        }
        try {
            if (this.mVCD != null && ((VivoCollectData) this.mVCD).getControlInfo("203")) {
                HashMap<String, String> params = new HashMap<>(1);
                if (DEBUG) {
                    VSlog.i(TAG, "writeData" + content);
                }
                params.put("info", content);
                ((VivoCollectData) this.mVCD).writeData("203", "2036", 0L, 0L, System.currentTimeMillis(), 1, params);
                ((VivoCollectData) this.mVCD).flush();
                HashMap<String, String> params1 = new HashMap<>(3);
                params1.put(VCD_FF_1.UUID_STR, UUID.randomUUID().toString());
                params1.put("info", content);
                EventTransfer.getInstance().singleEvent("F500", "F500|10006", System.currentTimeMillis(), 0L, params1);
                return;
            }
            VSlog.w(TAG, "V collect is not open for 203, Wait for Next time check.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void requestLocationUpdatesLocked(String packName) {
        if (DEBUG) {
            VSlog.e(TAG, "test requestLocationUpdatesLocked " + packName);
        }
        Message msg = this.mDCHandler.obtainMessage(MSG_REQUEST_LOCATION_UPDATES);
        msg.obj = packName;
        this.mDCHandler.sendMessage(msg);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void handleLocationChangedLocked() {
        if (this.requestRecord == null) {
            return;
        }
        if (DEBUG) {
            VSlog.e(TAG, "test handleLocationChangedLocked");
        }
        this.mDCHandler.removeMessages(MSG_OVER_TEN_SECOND);
        Message msg = this.mDCHandler.obtainMessage(MSG_HANDLE_LOCATION_CHANGED);
        this.mDCHandler.sendMessage(msg);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void getFromLocation(String pkgName, boolean success) {
        GeoRecord geoRecord;
        if (DEBUG) {
            VSlog.i(TAG, "test getFromLocation" + pkgName);
        }
        long now = System.currentTimeMillis();
        if (success) {
            geoRecord = new GeoRecord(pkgName, true, "rgc", now, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        } else {
            geoRecord = new GeoRecord(pkgName, false, "rgc", now, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        }
        Message msg = this.mDCHandler.obtainMessage(MSG_GET_FROM_LOCATION);
        msg.obj = geoRecord;
        this.mDCHandler.sendMessage(msg);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void getFromLocationName(String pkgName, boolean success, String location) {
        GeoRecord geoRecord;
        if (DEBUG) {
            VSlog.i(TAG, "test getFromLocationName" + pkgName);
        }
        long now = System.currentTimeMillis();
        if (success) {
            geoRecord = new GeoRecord(pkgName, true, "gc", now, location);
        } else {
            geoRecord = new GeoRecord(pkgName, false, "gc", now, location);
        }
        Message msg = this.mDCHandler.obtainMessage(MSG_GET_FROM_LOCATION_NAME);
        msg.obj = geoRecord;
        this.mDCHandler.sendMessage(msg);
    }

    private void tryCheckIfNeedWriteToDB() {
        if (DEBUG) {
            VSlog.i(TAG, "tryCheckIfNeedWriteToDB");
        }
        this.mDCHandler.removeMessages(2000);
        Message msg = this.mDCHandler.obtainMessage(2000);
        this.mDCHandler.sendMessage(msg);
    }

    public int getAPNType(Context context) {
        try {
            NetworkInfo networkInfo = this.connMgr.getActiveNetworkInfo();
            if (networkInfo == null) {
                return 0;
            }
            int nType = networkInfo.getType();
            if (nType == 1) {
                return 1;
            }
            if (nType != 0) {
                return 0;
            }
            return 2;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}