package com.vivo.services.common;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.FileObserver;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.TextUtils;
import android.util.FtDeviceInfo;
import android.util.FtFeature;
import com.android.internal.util.DumpUtils;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import vendor.vivo.hardware.omnipotentservice.V1_0.IOmnipotentService;
import vivo.app.common.IVivoCommon;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoCommonService extends IVivoCommon.Stub {
    private static final String COMMAND_ARGS_DEMILITER = "####";
    private static final String COMMAND_GET_EMMC_ID = "get_emmc_id";
    private static final String COMMAND_GET_EMMC_NAME = "get_emmc_name";
    private static final String COMMAND_GET_FEATURE_ATTRIBUTE = "getFeatureAttribute";
    private static final String COMMAND_GET_ST_LIFE_A = "get_st_life_a";
    private static final String COMMAND_GET_ST_LIFE_B = "get_st_life_b";
    private static final String COMMAND_GET_UFS_ID = "get_ufs_id";
    private static final String COMMAND_GET_UFS_MODEL = "get_ufs_model";
    private static final String COMMAND_GET_WECHAT_COMPAT_DISABLED = "COMMAND_GET_WECHAT_COMPAT_DISABLED";
    private static final String COMMAND_IS_FEATURE_SUPPORT = "isFeatureSupport";
    private static final boolean DBG = false;
    private static final String TAG = "VivoCommonService";
    private static final String WECHAT_COMPAT_DISABLE_CONFIG_FILE_PATH = "/data/bbkcore/CompatDisable.xml";
    private static VivoCommonService sInstance;
    private String emmcId;
    private String emmcName;
    private Context mContext;
    private FileObserver mFileObserver;
    private Handler mHandler;
    private String mStLifeA;
    private String mStLifeB;
    private String ufsId;
    private String ufsModel;
    private Object mWeChatCompatLock = new Object();
    private boolean isWeChatCompatDisabled = true;
    private Runnable wechatConfigFileObserverRunnable = new Runnable() { // from class: com.vivo.services.common.VivoCommonService.1
        @Override // java.lang.Runnable
        public void run() {
            VivoCommonService.this.parseWeChatCompatConfig(VivoCommonService.WECHAT_COMPAT_DISABLE_CONFIG_FILE_PATH);
            VivoCommonService.this.observeWeChatCompatDisableConfigFile();
        }
    };
    private boolean mHasRTBlur = false;
    private boolean mRTBlurChecked = false;

    private static native String doCommonJobByNative(String str);

    private VivoCommonService(Context context) {
        VSlog.i(TAG, "Vivo Common Service");
        this.mContext = context;
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        this.mHandler = new CommonServiceHandler(thread.getLooper());
        initWechatCompatDisable();
        try {
            IOmnipotentService service = IOmnipotentService.getService();
            if (service != null) {
                this.ufsId = service.doOmnipotentTask(COMMAND_GET_UFS_ID);
            } else {
                this.ufsId = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            }
            if (service != null && TextUtils.isEmpty(this.ufsId)) {
                this.emmcId = service.doOmnipotentTask(COMMAND_GET_EMMC_ID);
            } else {
                this.emmcId = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            }
            if (service != null) {
                this.emmcName = service.doOmnipotentTask(COMMAND_GET_EMMC_NAME);
            } else {
                this.emmcName = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            }
            if (service != null) {
                this.ufsModel = service.doOmnipotentTask(COMMAND_GET_UFS_MODEL);
            } else {
                this.ufsModel = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            }
            if (service != null) {
                this.mStLifeA = service.doOmnipotentTask(COMMAND_GET_ST_LIFE_A);
            } else {
                this.mStLifeA = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            }
            if (service != null) {
                this.mStLifeB = service.doOmnipotentTask(COMMAND_GET_ST_LIFE_B);
            } else {
                this.mStLifeB = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            }
        } catch (Exception e) {
            VLog.e(TAG, "COMMAND_GET_EMMC_UFS_ID get exception");
            e.printStackTrace();
        }
    }

    private void initWechatCompatDisable() {
        parseWeChatCompatConfig(WECHAT_COMPAT_DISABLE_CONFIG_FILE_PATH);
        observeWeChatCompatDisableConfigFile();
    }

    private boolean getWeChatCompatConfigDisabled() {
        boolean z;
        synchronized (this.mWeChatCompatLock) {
            z = this.isWeChatCompatDisabled;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean parseWeChatCompatConfig(String filePath) {
        StringBuilder sb;
        String result;
        try {
            try {
                File file = new File(filePath);
                result = FileUtils.readTextFile(file, 0, null);
            } catch (Exception e) {
                this.isWeChatCompatDisabled = false;
                VSlog.e(TAG, "parseCompatConfig error! " + e.fillInStackTrace());
                sb = new StringBuilder();
            }
            if (result != null) {
                synchronized (this.mWeChatCompatLock) {
                    this.isWeChatCompatDisabled = parseWeChatDisableStateFromXml(new ByteArrayInputStream(result.getBytes()));
                }
                return true;
            }
            sb = new StringBuilder();
            sb.append("CompatConfigDisabled=");
            sb.append(this.isWeChatCompatDisabled);
            VSlog.d(TAG, sb.toString());
            return false;
        } finally {
            VSlog.d(TAG, "CompatConfigDisabled=" + this.isWeChatCompatDisabled);
        }
    }

    private boolean parseWeChatDisableStateFromXml(InputStream is) {
        boolean z = false;
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            try {
                parser.setInput(new InputStreamReader(is));
                while (parser.getEventType() != 1) {
                    try {
                        if (parser.getEventType() == 2 && "item".equalsIgnoreCase(parser.getName()) && "name".equalsIgnoreCase(parser.getAttributeName(0)) && "disable_wechat_compat".equalsIgnoreCase(parser.getAttributeValue(0)) && "value".equalsIgnoreCase(parser.getAttributeName(1))) {
                            z = "1".equalsIgnoreCase(parser.getAttributeValue(1));
                            return z;
                        }
                        parser.next();
                    } catch (Exception e) {
                        e.printStackTrace();
                        return z;
                    }
                }
                return false;
            } catch (Exception e2) {
                e2.printStackTrace();
                return false;
            }
        } catch (Exception e3) {
            e3.printStackTrace();
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void observeWeChatCompatDisableConfigFile() {
        FileObserver fileObserver = this.mFileObserver;
        if (fileObserver != null) {
            fileObserver.stopWatching();
        }
        File file = new File(WECHAT_COMPAT_DISABLE_CONFIG_FILE_PATH);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (Exception e) {
            VSlog.e(TAG, "observeWeChatCompatDisableConfigFile create file error");
        }
        FileObserver fileObserver2 = new FileObserver(WECHAT_COMPAT_DISABLE_CONFIG_FILE_PATH, 1544) { // from class: com.vivo.services.common.VivoCommonService.2
            @Override // android.os.FileObserver
            public void onEvent(int event, String path) {
                if (8 == event) {
                    VivoCommonService.this.parseWeChatCompatConfig(VivoCommonService.WECHAT_COMPAT_DISABLE_CONFIG_FILE_PATH);
                }
                if (event == 1024 || event == 512) {
                    VivoCommonService.this.mHandler.removeCallbacks(VivoCommonService.this.wechatConfigFileObserverRunnable);
                    VivoCommonService.this.mHandler.postDelayed(VivoCommonService.this.wechatConfigFileObserverRunnable, 2000L);
                }
            }
        };
        this.mFileObserver = fileObserver2;
        fileObserver2.startWatching();
    }

    public static synchronized VivoCommonService getInstance(Context context) {
        VivoCommonService vivoCommonService;
        synchronized (VivoCommonService.class) {
            if (sInstance == null) {
                sInstance = new VivoCommonService(context);
            }
            vivoCommonService = sInstance;
        }
        return vivoCommonService;
    }

    /* loaded from: classes.dex */
    final class CommonServiceHandler extends Handler {
        public CommonServiceHandler(Looper looper) {
            super(looper);
        }
    }

    public void ping(String msg) {
        VSlog.d(TAG, "ping msg = " + msg);
    }

    public String doCommonJob(String msg) {
        if (msg != null && msg.startsWith(COMMAND_IS_FEATURE_SUPPORT)) {
            return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + isFeatureSupport(msg);
        } else if (msg != null && msg.startsWith(COMMAND_GET_FEATURE_ATTRIBUTE)) {
            return getFeatureAttribute(msg);
        } else {
            if (COMMAND_GET_WECHAT_COMPAT_DISABLED.equals(msg)) {
                return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + getWeChatCompatConfigDisabled();
            } else if (COMMAND_GET_EMMC_ID.equals(msg)) {
                return this.emmcId;
            } else {
                if (COMMAND_GET_UFS_ID.equals(msg)) {
                    return this.ufsId;
                }
                if (COMMAND_GET_EMMC_NAME.equals(msg)) {
                    return this.emmcName;
                }
                if (COMMAND_GET_UFS_MODEL.equals(msg)) {
                    return this.ufsModel;
                }
                if (COMMAND_GET_ST_LIFE_A.equals(msg)) {
                    return this.mStLifeA;
                }
                if (COMMAND_GET_ST_LIFE_B.equals(msg)) {
                    return this.mStLifeB;
                }
                return null;
            }
        }
    }

    private boolean isFeatureSupport(String msg) {
        String[] args;
        if (msg == null || (args = msg.split(COMMAND_ARGS_DEMILITER)) == null || args.length != 2) {
            return false;
        }
        String feature = args[1];
        if ("vivo.software.rtblur".equals(feature)) {
            return hasRTBlur();
        }
        return FtFeature.isFeatureSupport(args[1]);
    }

    private boolean hasRTBlur() {
        if (this.mRTBlurChecked) {
            return this.mHasRTBlur;
        }
        PackageManager pm = this.mContext.getPackageManager();
        if (pm != null) {
            try {
                boolean isPackageAvailable = pm.isPackageAvailable("com.vivo.systemblur.server");
                this.mHasRTBlur = isPackageAvailable;
                this.mRTBlurChecked = true;
                return isPackageAvailable;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    private String getFeatureAttribute(String msg) {
        String[] args;
        if (msg == null || (args = msg.split(COMMAND_ARGS_DEMILITER)) == null || args.length != 4) {
            return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        }
        String feature = args[1];
        String attr = args[2];
        String defaultValue = args[3];
        return FtFeature.getFeatureAttribute(feature, attr, defaultValue);
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            pw.println("isWeChatCompatDisabled=" + this.isWeChatCompatDisabled);
            String emmcId = null;
            String ufsId = null;
            try {
                IOmnipotentService service = IOmnipotentService.getService();
                if (service != null) {
                    emmcId = service.doOmnipotentTask(COMMAND_GET_EMMC_ID);
                } else {
                    emmcId = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                }
                if (service != null) {
                    ufsId = service.doOmnipotentTask(COMMAND_GET_UFS_ID);
                } else {
                    ufsId = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                }
                if (service != null) {
                    this.emmcName = service.doOmnipotentTask(COMMAND_GET_EMMC_NAME);
                } else {
                    this.emmcName = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                }
                if (service != null) {
                    this.ufsModel = service.doOmnipotentTask(COMMAND_GET_UFS_MODEL);
                } else {
                    this.ufsModel = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                }
                if (service != null) {
                    this.mStLifeA = service.doOmnipotentTask(COMMAND_GET_ST_LIFE_A);
                } else {
                    this.mStLifeA = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                }
                if (service != null) {
                    this.mStLifeB = service.doOmnipotentTask(COMMAND_GET_ST_LIFE_B);
                } else {
                    this.mStLifeB = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            pw.println("emmcId=" + emmcId);
            pw.println("ufsId=" + ufsId);
            pw.println("emmcName=" + this.emmcName);
            pw.println("ufsModel=" + this.ufsModel);
            pw.println("stlifeA=" + this.mStLifeA);
            pw.println("stLifeB=" + this.mStLifeB);
            boolean isRoundPhone = FtFeature.isFeatureSupport(8);
            int upRoundRadius = FtDeviceInfo.getUpRoundRaidus(this.mContext);
            int downRoundRadius = FtDeviceInfo.getDnRoundRaidus(this.mContext);
            pw.println("isRoundPhone=" + isRoundPhone);
            if (isRoundPhone) {
                pw.println("upRoundRadius=" + upRoundRadius);
                pw.println("downRoundRadius=" + downRoundRadius);
            }
            boolean isEarPhone = FtFeature.isFeatureSupport(32);
            int earUpWidth = FtDeviceInfo.getEarUpWidth(this.mContext);
            int earDownWidth = FtDeviceInfo.getEarDnWidth(this.mContext);
            Point leftPoint = FtDeviceInfo.getPortraitEarPosition(this.mContext);
            pw.println("isEarPhone=" + isEarPhone);
            if (isEarPhone) {
                pw.println("earUpWidth=" + earUpWidth);
                pw.println("earDownWidth=" + earDownWidth);
                pw.println("earLeftUpPoint=" + leftPoint.toString());
            }
            int opti = 0;
            boolean dumpFeature = false;
            String feature = null;
            while (opti < args.length) {
                String opt = args[opti];
                if (opt == null || opt.length() <= 0) {
                    break;
                }
                boolean isRoundPhone2 = isRoundPhone;
                if (opt.charAt(0) != '-') {
                    break;
                }
                opti++;
                if ("--feature".equals(opt) && opti < args.length) {
                    String feature2 = args[opti];
                    opti++;
                    dumpFeature = true;
                    feature = feature2;
                }
                isRoundPhone = isRoundPhone2;
            }
            if (dumpFeature && !TextUtils.isEmpty(feature)) {
                if (FtFeature.isFeatureSupport(feature)) {
                    pw.println("feature {" + feature + "} is supported");
                    return;
                }
                pw.println("feature {" + feature + "} is not supported");
            }
        }
    }
}