package com.android.server.display.color;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import com.android.server.wm.VCD_FF_1;
import com.vivo.vcodetransbase.EventTransfer;
import java.util.HashMap;
import java.util.UUID;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoLcmEventTransferUtils {
    public static final String ET_EVENT_ID_LCD_SRE = "F497|10005";
    public static final String ET_EVENT_ID_MTK_DRE_EN = "F497|10007";
    public static final String ET_EVENT_ID_OLED_HBM = "F497|10001";
    public static final String ET_EVENT_ID_OLED_HBM_1 = "F497|10002";
    public static final String ET_EVENT_ID_OLED_ORE = "F497|10003";
    public static final String ET_EVENT_ID_OLED_ORE_1 = "F497|10004";
    public static final String ET_EVENT_ID_QCOM_LTM = "F497|10006";
    public static final int ET_LCD_SRE = 5;
    public static final String ET_MODULE_ID = "F497";
    private static final int ET_MTK_DRE_EN = 7;
    public static final int ET_OLED_HBM = 1;
    public static final int ET_OLED_HBM_1 = 2;
    public static final int ET_OLED_ORE = 3;
    public static final int ET_OLED_ORE_1 = 4;
    public static final int ET_QCOM_LTM = 6;
    private static final String TAG = "VivoLcmEventTransferUtils";
    private static VivoLcmEventTransferUtils sEventTransferUtils;
    private EtHandler mEtHandler;
    private HandlerThread mEtThread;
    private EventTransfer mLcmEventTransfer;

    private VivoLcmEventTransferUtils() {
        this.mLcmEventTransfer = null;
        if (this.mEtHandler == null) {
            HandlerThread handlerThread = new HandlerThread("VivoLcmEventTransfer");
            this.mEtThread = handlerThread;
            if (handlerThread != null) {
                handlerThread.start();
                this.mEtHandler = new EtHandler(this.mEtThread.getLooper());
            }
        }
        this.mLcmEventTransfer = EventTransfer.getInstance();
    }

    public static synchronized VivoLcmEventTransferUtils getInstance() {
        VivoLcmEventTransferUtils vivoLcmEventTransferUtils;
        synchronized (VivoLcmEventTransferUtils.class) {
            if (sEventTransferUtils == null) {
                sEventTransferUtils = new VivoLcmEventTransferUtils();
            }
            vivoLcmEventTransferUtils = sEventTransferUtils;
        }
        return vivoLcmEventTransferUtils;
    }

    public void send(int what, long startTime, long duration, int level) {
        Message msg = this.mEtHandler.obtainMessage();
        msg.what = what;
        msg.obj = new LcmEtInfo(startTime, duration, level);
        this.mEtHandler.sendMessage(msg);
    }

    public void send(int what, long startTime, long duration) {
        Message msg = this.mEtHandler.obtainMessage();
        msg.what = what;
        msg.obj = new LcmEtInfo(startTime, duration);
        this.mEtHandler.sendMessage(msg);
    }

    public static void destroy() {
        VivoLcmEventTransferUtils vivoLcmEventTransferUtils = sEventTransferUtils;
        if (vivoLcmEventTransferUtils != null) {
            HandlerThread handlerThread = vivoLcmEventTransferUtils.mEtThread;
            if (handlerThread != null) {
                handlerThread.quitSafely();
                try {
                    sEventTransferUtils.mEtThread.join();
                } catch (InterruptedException e) {
                    VSlog.e(TAG, "InterruptedException:", e);
                }
                sEventTransferUtils.mEtThread = null;
            }
            sEventTransferUtils = null;
        }
    }

    /* loaded from: classes.dex */
    class EtHandler extends Handler {
        public EtHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (VivoLcmEventTransferUtils.this.mLcmEventTransfer != null) {
                LcmEtInfo info = (LcmEtInfo) msg.obj;
                HashMap<String, String> map = new HashMap<>();
                switch (msg.what) {
                    case 1:
                        map.put(VCD_FF_1.UUID_STR, UUID.randomUUID().toString());
                        map.put("start_time", String.valueOf(info.mStartTime));
                        map.put("end_time", String.valueOf(info.mStartTime + info.mDuration));
                        map.put("strength_level", String.valueOf(info.mLevel));
                        VivoLcmEventTransferUtils.this.mLcmEventTransfer.singleEvent(VivoLcmEventTransferUtils.ET_MODULE_ID, VivoLcmEventTransferUtils.ET_EVENT_ID_OLED_HBM, info.mStartTime, info.mDuration, map);
                        VSlog.d(VivoLcmEventTransferUtils.TAG, "EventTransferHandler  ET_OLED_HBM   " + info.mStartTime + " " + info.mDuration);
                        return;
                    case 2:
                        map.put(VCD_FF_1.UUID_STR, UUID.randomUUID().toString());
                        map.put("start_time", String.valueOf(info.mStartTime));
                        map.put("end_time", String.valueOf(info.mStartTime + info.mDuration));
                        map.put("strength_level", String.valueOf(info.mLevel));
                        VivoLcmEventTransferUtils.this.mLcmEventTransfer.singleEvent(VivoLcmEventTransferUtils.ET_MODULE_ID, VivoLcmEventTransferUtils.ET_EVENT_ID_OLED_HBM_1, info.mStartTime, info.mDuration, map);
                        VSlog.d(VivoLcmEventTransferUtils.TAG, "EventTransferHandler  ET_OLED_HBM_1  " + info.mStartTime + " " + info.mDuration);
                        return;
                    case 3:
                        map.put(VCD_FF_1.UUID_STR, UUID.randomUUID().toString());
                        map.put("start_time", String.valueOf(info.mStartTime));
                        map.put("end_time", String.valueOf(info.mStartTime + info.mDuration));
                        map.put("strength_level", String.valueOf(info.mLevel));
                        VivoLcmEventTransferUtils.this.mLcmEventTransfer.singleEvent(VivoLcmEventTransferUtils.ET_MODULE_ID, VivoLcmEventTransferUtils.ET_EVENT_ID_OLED_ORE, info.mStartTime, info.mDuration, map);
                        VSlog.d(VivoLcmEventTransferUtils.TAG, "EventTransferHandler  ET_OLED_ORE  " + info.mStartTime + " " + info.mDuration + " " + info.mLevel);
                        return;
                    case 4:
                        map.put(VCD_FF_1.UUID_STR, UUID.randomUUID().toString());
                        map.put("start_time", String.valueOf(info.mStartTime));
                        map.put("end_time", String.valueOf(info.mStartTime + info.mDuration));
                        map.put("strength_level", String.valueOf(info.mLevel));
                        VivoLcmEventTransferUtils.this.mLcmEventTransfer.singleEvent(VivoLcmEventTransferUtils.ET_MODULE_ID, VivoLcmEventTransferUtils.ET_EVENT_ID_OLED_ORE_1, info.mStartTime, info.mDuration, map);
                        VSlog.d(VivoLcmEventTransferUtils.TAG, "EventTransferHandler  ET_OLED_ORE_1  " + info.mStartTime + " " + info.mDuration);
                        return;
                    case 5:
                        map.put(VCD_FF_1.UUID_STR, UUID.randomUUID().toString());
                        map.put("start_time", String.valueOf(info.mStartTime));
                        map.put("end_time", String.valueOf(info.mStartTime + info.mDuration));
                        map.put("strength_level", String.valueOf(info.mLevel));
                        VivoLcmEventTransferUtils.this.mLcmEventTransfer.singleEvent(VivoLcmEventTransferUtils.ET_MODULE_ID, VivoLcmEventTransferUtils.ET_EVENT_ID_LCD_SRE, info.mStartTime, info.mDuration, map);
                        VSlog.d(VivoLcmEventTransferUtils.TAG, "EventTransferHandler  ET_LCD_SRE   " + info.mStartTime + " " + info.mDuration);
                        return;
                    case 6:
                        map.put(VCD_FF_1.UUID_STR, UUID.randomUUID().toString());
                        map.put("start_time", String.valueOf(info.mStartTime));
                        map.put("end_time", String.valueOf(info.mStartTime + info.mDuration));
                        map.put("state_on", "1");
                        VivoLcmEventTransferUtils.this.mLcmEventTransfer.singleEvent(VivoLcmEventTransferUtils.ET_MODULE_ID, VivoLcmEventTransferUtils.ET_EVENT_ID_QCOM_LTM, info.mStartTime, info.mDuration, map);
                        VSlog.d(VivoLcmEventTransferUtils.TAG, "EventTransferHandler  ET_QCOM_LTM    " + info.mStartTime + " " + info.mDuration);
                        return;
                    default:
                        return;
                }
            }
        }
    }

    /* loaded from: classes.dex */
    static class LcmEtInfo {
        public long mDuration;
        public int mLevel;
        public long mStartTime;

        public LcmEtInfo(long startTime, long duration, int level) {
            this.mStartTime = startTime;
            this.mDuration = duration;
            this.mLevel = level;
        }

        public LcmEtInfo(long startTime, long duration) {
            this.mStartTime = startTime;
            this.mDuration = duration;
        }
    }
}