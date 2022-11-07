package com.android.server.display.color.displayenhance;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.FtBuild;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.FtFeature;
import com.android.server.FgThread;
import com.android.server.display.color.VivoColorManagerService;
import com.android.server.display.color.VivoLtmController;
import com.vivo.services.rms.ProcessList;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParserException;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class HawkeyeDisplayEnhanceController implements DisplayEnhanceListener {
    private static final String DISPLAY_ENHANCE_HAWKEYE_ACTIVATED = "gamecube_hawkeye_effective";
    static final String TAG = "HawkeyeDisplayEnhanceController";
    public static final int VIVO_COLOR_MODE_GAME_OFF = 200;
    public static final int VIVO_COLOR_MODE_GAME_ON = 258;
    private ApplicationPackageObserver mAppObserver;
    private Context mContext;
    private DisplayEnhanceIrisConfig mDisplayEnhanceConfig;
    private VivoColorManagerService mVivoColorManager;
    private VivoLtmController mVivoLtmController;
    private static boolean DBG = SystemProperties.getBoolean("persist.vivo.display.enhance.debug", false);
    private static volatile HawkeyeDisplayEnhanceController mHawkeyeController = null;
    private static final boolean mSupportGameHdr = FtFeature.isFeatureSupport("vivo.hardware.game.sdr2hdr");
    private static final boolean mSupportHawkeye = FtFeature.isFeatureSupport("vivo.software.hawkeye");
    private boolean mHawkeyeActivated = false;
    private int mCurrentUser = ProcessList.INVALID_ADJ;
    private final HashMap<String, PackageHawkeyeInfo> mPackageHawkeyeMap = new HashMap<>();
    private Handler mHandler = new Handler(FgThread.get().getLooper());
    private final ContentObserver mContentObserver = new ContentObserver(this.mHandler) { // from class: com.android.server.display.color.displayenhance.HawkeyeDisplayEnhanceController.2
        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            try {
                if (HawkeyeDisplayEnhanceController.this.isDisplayEnhanceHawkeyeActivated() != HawkeyeDisplayEnhanceController.this.mHawkeyeActivated) {
                    HawkeyeDisplayEnhanceController.this.mHawkeyeActivated = HawkeyeDisplayEnhanceController.this.isDisplayEnhanceHawkeyeActivated();
                    HawkeyeDisplayEnhanceController.this.onDisplayEnhanceHawkeyeActivated(Boolean.valueOf(HawkeyeDisplayEnhanceController.this.mHawkeyeActivated));
                }
            } catch (Exception e) {
                VSlog.e(HawkeyeDisplayEnhanceController.TAG, "onChange: SettingNotFoundException:" + e.getMessage());
            }
        }
    };

    private HawkeyeDisplayEnhanceController(VivoColorManagerService colorManager, Context context) {
        this.mVivoLtmController = null;
        this.mAppObserver = null;
        this.mVivoColorManager = null;
        this.mDisplayEnhanceConfig = null;
        this.mContext = context;
        this.mVivoColorManager = colorManager;
        this.mVivoLtmController = VivoLtmController.getInstance(context);
        this.mAppObserver = ApplicationPackageObserver.getInstance(context);
        if (mSupportGameHdr) {
            this.mDisplayEnhanceConfig = DisplayEnhanceIrisConfig.getInstance(context);
        }
        this.mHandler.post(new Runnable() { // from class: com.android.server.display.color.displayenhance.HawkeyeDisplayEnhanceController.1
            @Override // java.lang.Runnable
            public void run() {
                HawkeyeDisplayEnhanceController.this.parserAppInfo();
            }
        });
        VivoDisplayEnhanceManagerService.registerDisplayEnhanceListener(this);
    }

    public static HawkeyeDisplayEnhanceController getInstance(VivoColorManagerService colorManager, Context context) {
        if (mHawkeyeController == null) {
            synchronized (HawkeyeDisplayEnhanceController.class) {
                if (mHawkeyeController == null) {
                    mHawkeyeController = new HawkeyeDisplayEnhanceController(colorManager, context);
                }
            }
        }
        return mHawkeyeController;
    }

    public void setUp(int userHandle) {
        if (mSupportHawkeye || mSupportGameHdr) {
            this.mCurrentUser = userHandle;
            if (userHandle == -10000) {
                this.mCurrentUser = -2;
            }
            this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(DISPLAY_ENHANCE_HAWKEYE_ACTIVATED), false, this.mContentObserver, this.mCurrentUser);
        }
    }

    public void tearDown() {
        if ((mSupportHawkeye || mSupportGameHdr) && this.mContentObserver != null) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mContentObserver);
        }
    }

    public void onStartUser(int userHandle) {
        if (mSupportHawkeye || mSupportGameHdr) {
            Settings.System.putIntForUser(this.mContext.getContentResolver(), DISPLAY_ENHANCE_HAWKEYE_ACTIVATED, 0, -2);
        }
    }

    private int getForegroundAppColorMode(String packageName) {
        PackageHawkeyeInfo info = this.mPackageHawkeyeMap.get(packageName);
        if (info == null) {
            return -1;
        }
        int colorMode = info.colorMode;
        return colorMode;
    }

    private float getForegroundAppEyeProRatio(String packageName) {
        PackageHawkeyeInfo info = this.mPackageHawkeyeMap.get(packageName);
        if (info == null) {
            return 1.0f;
        }
        float ratio = info.eyeProRatio;
        if (!isDisplayEnhanceHawkeyeActivated()) {
            return ratio;
        }
        float ratio2 = info.hawkeyeProRatio;
        return ratio2;
    }

    private boolean isInForeground(String packageName) {
        PackageHawkeyeInfo info = this.mPackageHawkeyeMap.get(packageName);
        if (info != null) {
            return true;
        }
        return false;
    }

    @Override // com.android.server.display.color.displayenhance.DisplayEnhanceListener
    public void onScreenStateChanged(int state) {
    }

    @Override // com.android.server.display.color.displayenhance.DisplayEnhanceListener
    public void onForegroundPackageChanged(String name) {
        if (this.mAppObserver.isLauncher(name) || name == null) {
            if (this.mVivoColorManager.getEyeProtectionRatio() != 1.0f) {
                this.mVivoColorManager.setEyeProtectionRatio(1.0f);
            }
        } else if (isInForeground(name)) {
            this.mVivoColorManager.setEyeProtectionRatio(getForegroundAppEyeProRatio(name));
        }
    }

    @Override // com.android.server.display.color.displayenhance.DisplayEnhanceListener
    public void onForegroundActivityChanged(String name, int state) {
    }

    @Override // com.android.server.display.color.displayenhance.DisplayEnhanceListener
    public void dump(PrintWriter pw) {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isDisplayEnhanceHawkeyeActivated() {
        return Settings.System.getIntForUser(this.mContext.getContentResolver(), DISPLAY_ENHANCE_HAWKEYE_ACTIVATED, 0, -2) == 1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onDisplayEnhanceHawkeyeActivated(Boolean activated) {
        if (mSupportHawkeye && !mSupportGameHdr) {
            int targetColorMode = -1;
            if (!FtBuild.isMTKPlatform()) {
                this.mVivoLtmController.setLtmOn(activated.booleanValue(), activated.booleanValue() ? 1 : 0);
            }
            String packageName = this.mAppObserver.getForegroundAppPackageName();
            this.mVivoColorManager.setEyeProtectionRatio(getForegroundAppEyeProRatio(packageName));
            if (activated.booleanValue()) {
                targetColorMode = getForegroundAppColorMode(packageName);
                if (targetColorMode == -1 || (this.mVivoColorManager.mDtm != null && !this.mVivoColorManager.mDtm.isDisplayColorSupport(targetColorMode))) {
                    targetColorMode = 258;
                }
            } else if (FtBuild.isMTKPlatform()) {
                targetColorMode = 200;
            } else {
                int currentColorMode = this.mVivoColorManager.getActualColorModeSetting();
                if (currentColorMode == 509) {
                    targetColorMode = currentColorMode;
                } else if (currentColorMode >= 256 && currentColorMode <= 511) {
                    targetColorMode = this.mVivoColorManager.getUserColorModeSetting();
                }
            }
            VSlog.d(TAG, "onDisplayEnhanceHawkeyeActivated: activated=" + activated + ", targetColorMode=" + targetColorMode);
            if (targetColorMode >= 0) {
                this.mVivoColorManager.setActualColorModeSetting(targetColorMode);
            }
        } else if (this.mDisplayEnhanceConfig != null) {
            int hdrMode = activated.booleanValue() ? 3 : 0;
            int dsiplayMode = this.mDisplayEnhanceConfig.getDisplayMode();
            int memcMode = this.mDisplayEnhanceConfig.getMemcMode();
            if (activated.booleanValue() && dsiplayMode != 0) {
                this.mDisplayEnhanceConfig.setDisplayMode(0);
            }
            this.mDisplayEnhanceConfig.setSdr2HdrMode(hdrMode);
            if (!activated.booleanValue() && memcMode == 0) {
                this.mDisplayEnhanceConfig.setDisplayMode(1);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class PackageHawkeyeInfo {
        private int colorMode;
        private float eyeProRatio;
        private float hawkeyeProRatio;

        private PackageHawkeyeInfo() {
            this.colorMode = -1;
            this.eyeProRatio = 1.0f;
            this.hawkeyeProRatio = 1.0f;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void parserAppInfo() {
        Resources res = this.mContext.getResources();
        if (res == null) {
            return;
        }
        try {
            XmlResourceParser xmlParser = res.getXml(51576833);
            try {
                PackageHawkeyeInfo info = null;
                if (DBG) {
                    VSlog.d(TAG, "paser PackageHawkeyeInfo start");
                }
                for (int event = xmlParser.getEventType(); event != 1; event = xmlParser.next()) {
                    if (event == 2) {
                        if ("package".equals(xmlParser.getName())) {
                            String pkg = xmlParser.getAttributeValue(0);
                            if (DBG) {
                                VSlog.d(TAG, "<<<== package : " + pkg + " ==>>>");
                            }
                            info = new PackageHawkeyeInfo();
                            this.mPackageHawkeyeMap.put(pkg, info);
                        } else if ("feature".equals(xmlParser.getName())) {
                            String feature = xmlParser.getAttributeValue(0);
                            if (info != null) {
                                if ("eye.pro.ratio".equals(feature)) {
                                    String featureState = xmlParser.nextText();
                                    try {
                                        info.eyeProRatio = Float.parseFloat(featureState);
                                        if (DBG) {
                                            VSlog.d(TAG, "feature : " + feature + ", eyeProRatio : " + info.eyeProRatio);
                                        }
                                    } catch (NumberFormatException e) {
                                    }
                                } else if ("hawkeye.pro.ratio".equals(feature)) {
                                    String featureState2 = xmlParser.nextText();
                                    try {
                                        info.hawkeyeProRatio = Float.parseFloat(featureState2);
                                        if (DBG) {
                                            VSlog.d(TAG, "feature : " + feature + ", hawkeyeProRatio : " + info.hawkeyeProRatio);
                                        }
                                    } catch (NumberFormatException e2) {
                                    }
                                } else if ("colorMode".equals(feature)) {
                                    String featureState3 = xmlParser.nextText();
                                    try {
                                        info.colorMode = Integer.parseInt(featureState3);
                                        if (DBG) {
                                            VSlog.d(TAG, "feature : " + feature + ", colorMode : " + info.colorMode);
                                        }
                                    } catch (NumberFormatException e3) {
                                    }
                                }
                            }
                        }
                    }
                }
                if (DBG) {
                    VSlog.d(TAG, "paser PackageHawkeyeInfo end");
                }
            } catch (XmlPullParserException e4) {
                e4.printStackTrace();
            }
        } catch (Resources.NotFoundException e5) {
            e5.printStackTrace();
        } catch (IOException e6) {
            e6.printStackTrace();
        }
    }
}