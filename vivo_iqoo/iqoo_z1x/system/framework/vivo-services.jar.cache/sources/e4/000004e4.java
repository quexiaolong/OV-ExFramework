package com.android.server.wm;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.ArraySet;
import android.util.Slog;
import android.util.Xml;
import com.android.server.UnifiedConfigThread;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class GraphicsConfigController {
    private static final String ACTION_UPDATE_CONFIG_CENTER = "com.vivo.daemonService.unifiedconfig.update_finish_broadcast_Graphics";
    private static final String ATTR_ITEM_PKG = "pkg";
    private static final String ATTR_ITEM_VERSION = "version";
    private static final String ATTR_STUCK_ANALYSIS_ENABLED = "enabled";
    private static final String ATTR_STUCK_ANALYSIS_THRESHOLD = "threshold";
    private static final String CATON_TRACKING_TIMEOUT_THRESHOLD = "caton_threshold";
    private static final String CONFIG_IDENTIFY = "identify5";
    private static final String CONFIG_MODULE = "Graphics";
    private static final String CONFIG_TYPE = "10";
    private static final String CONFIG_VERSION = "1.5";
    private static final String FRAME_DURATION_THRESHOLD = "frame_threshold";
    private static final String FRAME_MISSED_INTERVAL_THRESHOLD = "sf_trace_interval";
    private static final String MISSED_FRAME_AMOUNT_THRESHOLD = "sf_trace_num";
    private static final String MISSED_FRAME_THRESHOLD = "missedframe_threshold";
    private static final String MISSED_JANKED_FRAME_THRESHOLD = "missedjankedframe_threshold";
    private static final String TAG = "GraphicsConfigController";
    private static final String TAG_ITEM = "item";
    private static final String TAG_SPECIAL_LIST = "special-list";
    private static final String TAG_STUCK_ANALYSIS = "stuck-analysis";
    private static final String TOTAL_TRACE_NUM = "totaltrace_threshold";
    private static final Uri UNIFIED_CONFIG_CENTER_URI = Uri.parse("content://com.vivo.abe.unifiedconfig.provider/configs");
    private static final String URI = "content://com.vivo.abe.unifiedconfig.provider/configs";
    private Context mContext;
    private final boolean DBG = SystemProperties.getBoolean("persist.vivopolicy.debug", true);
    private Runnable retriveFileRunnable = new Runnable() { // from class: com.android.server.wm.GraphicsConfigController.2
        @Override // java.lang.Runnable
        public void run() {
            if (GraphicsConfigController.this.DBG) {
                VSlog.d(GraphicsConfigController.TAG, "retriveFileRunnable!");
            }
            GraphicsConfigController.this.readXmlFileFromUnifiedConfig(GraphicsConfigController.CONFIG_MODULE, GraphicsConfigController.CONFIG_TYPE, GraphicsConfigController.CONFIG_VERSION, GraphicsConfigController.CONFIG_IDENTIFY);
        }
    };
    private Handler mIoHandler = UnifiedConfigThread.getHandler();

    public GraphicsConfigController(Context context) {
        this.mContext = context;
        registerBroadcast();
    }

    public void systemReady() {
        postRetriveFile();
    }

    private void registerBroadcast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_UPDATE_CONFIG_CENTER);
        this.mContext.registerReceiver(new BroadcastReceiver() { // from class: com.android.server.wm.GraphicsConfigController.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                if (GraphicsConfigController.ACTION_UPDATE_CONFIG_CENTER.equals(intent.getAction())) {
                    if (GraphicsConfigController.this.DBG) {
                        VSlog.d(GraphicsConfigController.TAG, "onReceive intent:" + intent);
                    }
                    Bundle extra = intent.getExtras();
                    String[] identifiers = (String[]) extra.get("identifiers");
                    if (identifiers != null && identifiers.length > 0) {
                        for (String identify : identifiers) {
                            VSlog.d(GraphicsConfigController.TAG, "onReceive identify:" + identify);
                        }
                    }
                    GraphicsConfigController.this.postRetriveFile();
                }
            }
        }, filter);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void postRetriveFile() {
        if (this.DBG) {
            VSlog.d(TAG, "postRetriveFile");
        }
        this.mIoHandler.removeCallbacks(this.retriveFileRunnable);
        this.mIoHandler.postDelayed(this.retriveFileRunnable, 10000L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void readXmlFileFromUnifiedConfig(String module, String type, String version, String identifier) {
        Cursor cursor;
        ContentResolver resolver = this.mContext.getContentResolver();
        String[] selectionArgs = {module, type, version, identifier};
        Cursor cursor2 = null;
        byte[] filecontent = null;
        try {
            try {
                cursor = resolver.query(Uri.parse("content://com.vivo.abe.unifiedconfig.provider/configs"), null, null, selectionArgs, null);
                if (cursor != null) {
                    cursor.moveToFirst();
                    if (cursor.getCount() > 0) {
                        while (!cursor.isAfterLast()) {
                            filecontent = cursor.getBlob(cursor.getColumnIndex("filecontent"));
                            cursor.moveToNext();
                            if (this.DBG) {
                                VSlog.d(TAG, "content = " + new String(filecontent));
                            }
                        }
                    } else if (this.DBG) {
                        VSlog.d(TAG, "no data!");
                    }
                }
            } catch (Exception e) {
                VSlog.e(TAG, "open database error! " + e.fillInStackTrace());
                if (0 != 0) {
                    cursor2.close();
                }
            }
            if (cursor != null) {
                cursor.close();
            }
            if (filecontent != null) {
                String result = new String(filecontent);
                DataBuilder dataBuilder = readXmlFile(new ByteArrayInputStream(result.getBytes()));
                if (dataBuilder != null) {
                    notifySF(dataBuilder);
                }
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    cursor2.close();
                } catch (Exception e2) {
                }
            }
            throw th;
        }
    }

    private DataBuilder readXmlFile(InputStream inputStream) {
        if (this.DBG) {
            VSlog.d(TAG, "readXmlFile FROM " + inputStream);
        }
        DataBuilder builder = new DataBuilder();
        try {
            try {
                try {
                    XmlPullParser parser = Xml.newPullParser();
                    String str = null;
                    parser.setInput(inputStream, null);
                    int eventType = parser.getEventType();
                    parser.getName();
                    while (eventType != 1) {
                        String tag = parser.getName();
                        if (eventType == 2) {
                            if (this.DBG) {
                                VSlog.d(TAG, "Start tag " + tag);
                            }
                            if (tag.equals(TAG_STUCK_ANALYSIS)) {
                                String enabled = parser.getAttributeValue(str, ATTR_STUCK_ANALYSIS_ENABLED);
                                builder.setEnabled(Boolean.parseBoolean(enabled));
                                String caton_t = parser.getAttributeValue(str, CATON_TRACKING_TIMEOUT_THRESHOLD);
                                builder.setCatonThreshold(Integer.parseInt(caton_t));
                                String frame_duration_threshold = parser.getAttributeValue(str, FRAME_DURATION_THRESHOLD);
                                builder.setframe_duration_threshold(Integer.parseInt(frame_duration_threshold));
                                String missed_frame_threshold = parser.getAttributeValue(str, MISSED_FRAME_THRESHOLD);
                                builder.setmissed_frame_threshold(Integer.parseInt(missed_frame_threshold));
                                String missed_janked_frame_threshold = parser.getAttributeValue(str, MISSED_JANKED_FRAME_THRESHOLD);
                                builder.setmissed_janked_frame_threshold(Integer.parseInt(missed_janked_frame_threshold));
                                String total_trace_num = parser.getAttributeValue(str, TOTAL_TRACE_NUM);
                                builder.settotal_trace_num(Integer.parseInt(total_trace_num));
                                String frame_missed_interval_threshold = parser.getAttributeValue(str, FRAME_MISSED_INTERVAL_THRESHOLD);
                                builder.setframe_missed_interval_threshold(Integer.parseInt(frame_missed_interval_threshold));
                                String missed_frame_amount_threshold = parser.getAttributeValue(str, MISSED_FRAME_AMOUNT_THRESHOLD);
                                builder.setmissed_frame_amount_threshold(Integer.parseInt(missed_frame_amount_threshold));
                            }
                            if (tag.equals(TAG_SPECIAL_LIST)) {
                                builder.setSpeciallist(parseSpecialList(parser));
                            }
                        } else if (eventType == 3 && this.DBG) {
                            VSlog.d(TAG, "End tag " + tag);
                        }
                        eventType = parser.next();
                        str = null;
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    if (inputStream != null) {
                        inputStream.close();
                    }
                }
            } catch (XmlPullParserException e) {
                e.printStackTrace();
                if (inputStream != null) {
                    inputStream.close();
                }
            }
            if (inputStream != null) {
                inputStream.close();
            }
            return builder;
        } catch (Throwable th) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e2) {
                }
            }
            throw th;
        }
    }

    private ArraySet<String> parseSpecialList(XmlPullParser parser) throws XmlPullParserException, IOException {
        ArraySet<String> list = new ArraySet<>();
        while (parser.next() != 1) {
            int eventType = parser.getEventType();
            if (eventType == 2) {
                if (TAG_ITEM.equals(parser.getName())) {
                    int eventType2 = parser.next();
                    if (eventType2 == 4) {
                        list.add(parser.getText());
                    }
                }
            } else if (eventType == 3 && TAG_SPECIAL_LIST.equals(parser.getName())) {
                return list;
            }
        }
        throw new XmlPullParserException("special-list section is incomplete or section ending tag is missing");
    }

    private void notifySF(DataBuilder dataBuilder) {
        IBinder flinger = ServiceManager.getService("SurfaceFlinger");
        if (flinger != null) {
            Parcel data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            data.writeBoolean(dataBuilder.getEnabled());
            data.writeInt(dataBuilder.getCatonThreshold());
            data.writeInt(dataBuilder.getframe_duration_threshold());
            data.writeInt(dataBuilder.getmissed_frame_threshold());
            data.writeInt(dataBuilder.getmissed_janked_frame_threshold());
            data.writeInt(dataBuilder.gettotal_trace_num());
            data.writeInt(dataBuilder.getframe_missed_interval_threshold());
            data.writeInt(dataBuilder.getmissed_frame_amount_threshold());
            ArraySet<String> speciallist = dataBuilder.getSpeciallist();
            if (speciallist.size() == 0) {
                return;
            }
            data.writeStringArray((String[]) speciallist.toArray(new String[speciallist.size()]));
            try {
                try {
                    flinger.transact(31111, data, null, 0);
                    if (this.DBG) {
                        VSlog.d(TAG, "notifySF " + data);
                    }
                } catch (RemoteException ex) {
                    Slog.e(TAG, "Failed to set color transform", ex);
                }
            } finally {
                data.recycle();
            }
        }
    }

    /* loaded from: classes.dex */
    public static class DataBuilder {
        private int frame_duration_threshold;
        private int frame_missed_interval_threshold;
        private int mCaton_threshold;
        private boolean mEnabled;
        private ArraySet<String> mSpeciallist = new ArraySet<>();
        private int missed_frame_amount_threshold;
        private int missed_frame_threshold;
        private int missed_janked_frame_threshold;
        private int total_trace_num;

        public void setEnabled(boolean enabled) {
            this.mEnabled = enabled;
        }

        public void setSpeciallist(ArraySet<String> speciallist) {
            this.mSpeciallist = speciallist;
        }

        public void setCatonThreshold(int val) {
            this.mCaton_threshold = val;
        }

        public int getCatonThreshold() {
            return this.mCaton_threshold;
        }

        public void setframe_duration_threshold(int val) {
            this.frame_duration_threshold = val;
        }

        public int getframe_duration_threshold() {
            return this.frame_duration_threshold;
        }

        public void setmissed_frame_threshold(int val) {
            this.missed_frame_threshold = val;
        }

        public int getmissed_frame_threshold() {
            return this.missed_frame_threshold;
        }

        public void setmissed_janked_frame_threshold(int val) {
            this.missed_janked_frame_threshold = val;
        }

        public int getmissed_janked_frame_threshold() {
            return this.missed_janked_frame_threshold;
        }

        public void settotal_trace_num(int val) {
            this.total_trace_num = val;
        }

        public int gettotal_trace_num() {
            return this.total_trace_num;
        }

        public void setframe_missed_interval_threshold(int val) {
            this.frame_missed_interval_threshold = val;
        }

        public int getframe_missed_interval_threshold() {
            return this.frame_missed_interval_threshold;
        }

        public void setmissed_frame_amount_threshold(int val) {
            this.missed_frame_amount_threshold = val;
        }

        public int getmissed_frame_amount_threshold() {
            return this.missed_frame_amount_threshold;
        }

        public boolean getEnabled() {
            return this.mEnabled;
        }

        public ArraySet<String> getSpeciallist() {
            return this.mSpeciallist;
        }
    }
}