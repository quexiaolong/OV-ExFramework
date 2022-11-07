package com.android.server.policy;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManagerInternal;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.RemoteException;
import android.os.SystemClock;
import android.view.Display;
import android.view.IWindowManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManagerPolicyConstants;
import com.android.internal.policy.KeyInterceptionInfo;
import com.android.internal.policy.KeyguardDismissCallback;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.LocalServices;
import com.android.server.policy.VivoPhoneWindowManagerImpl;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.VivoAppShareManager;
import com.android.server.wm.WindowManagerInternal;
import com.vivo.face.common.data.Constants;
import com.vivo.fingerprint.FingerprintConfig;
import com.vivo.services.rms.AppPreviewAdjuster;
import com.vivo.services.superresolution.Constant;
import java.io.PrintWriter;
import java.util.ArrayList;
import vivo.app.VivoFrameworkFactory;
import vivo.app.nightmode.IVivoNightModeManager;
import vivo.app.nightmode.NightModeController;
import vivo.app.phonelock.AbsVivoPhoneLockManager;
import vivo.app.vperf.AbsVivoPerfManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoPhoneWindowManagerImpl implements IVivoPhoneWindowManager {
    private static final ArrayList<String> NIGHT_MODE_SPLAH_EXCLUDE_LIST;
    private static final String TAG = "VivoPhoneWindowManagerImpl";
    private boolean cancelGoToSleep;
    private Context mContext;
    private DisplayManagerInternal mDisplayManagerInternal;
    private Handler mHandler;
    private boolean mIsLightButton;
    private Object mLock;
    private LockPatternUtils mLockPatternUtils;
    private PhoneWindowManager mPhoneWindowManager;
    private VivoAppShareManager mVivoAppShareManager;
    private VivoInputPolicy mVivoInputPolicy;
    private AbsVivoPhoneLockManager mVivoPhoneLockManager;
    private WindowManagerPolicy.WindowManagerFuncs mWindowManagerFuncs;
    public AbsVivoPerfManager mPerf = null;
    private int mDisplayBacklight = -1;
    WindowManagerInternal mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);

    static {
        ArrayList<String> arrayList = new ArrayList<>();
        NIGHT_MODE_SPLAH_EXCLUDE_LIST = arrayList;
        arrayList.add("com.taobao.taobao");
        NIGHT_MODE_SPLAH_EXCLUDE_LIST.add("com.taobao.idlefish");
        NIGHT_MODE_SPLAH_EXCLUDE_LIST.add("com.ximalaya.ting.android");
        NIGHT_MODE_SPLAH_EXCLUDE_LIST.add(Constant.APP_TOUTIAO);
        NIGHT_MODE_SPLAH_EXCLUDE_LIST.add("com.shuqi.controller");
    }

    private boolean isInNightModeSplashExcludeList(String packageName) {
        ArrayList<String> arrayList;
        if (packageName == null || (arrayList = NIGHT_MODE_SPLAH_EXCLUDE_LIST) == null) {
            return false;
        }
        return arrayList.contains(packageName);
    }

    public VivoPhoneWindowManagerImpl(PhoneWindowManager phoneWindowManager, WindowManagerPolicy.WindowManagerFuncs windowManagerFuncs, Context context, Object lock, Handler handler) {
        this.mVivoAppShareManager = null;
        this.mPhoneWindowManager = phoneWindowManager;
        this.mWindowManagerFuncs = windowManagerFuncs;
        this.mContext = context;
        this.mHandler = handler;
        this.mLock = lock;
        this.mVivoInputPolicy = new VivoInputPolicy(phoneWindowManager, context, handler);
        this.mVivoAppShareManager = VivoAppShareManager.getInstance();
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
    }

    public void registerPointerEventListener(WindowManagerPolicyConstants.PointerEventListener listener, int displayId) {
        this.mWindowManagerFuncs.registerPointerEventListener(listener, displayId);
    }

    public void forceHideNavBar(IBinder token, boolean hide) {
        this.mPhoneWindowManager.mDefaultDisplayPolicy.forceHideNavBar(token, hide);
    }

    public boolean setKeyguardHide(int hide) {
        int userId = ActivityManager.getCurrentUser();
        if (hide == 1 && this.mLockPatternUtils.getVivoLockoutDeadline(userId) > 0) {
            VSlog.d("WindowManager", "setKeyguardHide refuse hide for lockout attempt deadline.");
            return false;
        }
        if (!this.mPhoneWindowManager.mKeyguardDelegate.isShowing()) {
            if (hide == 1) {
                VSlog.d("WindowManager", "setKeyguardHide refuse hide keyguard for keyguard is not showing.");
                return false;
            }
            hide = 2;
            VSlog.d("WindowManager", "setKeyguardHide restore keyguard");
        }
        this.mPhoneWindowManager.mKeyguardDelegate.setKeyguardHide(hide);
        return true;
    }

    public void notifySoftKeyboardShown(boolean shown) {
        if (this.mPhoneWindowManager.mKeyguardDelegate != null) {
            this.mPhoneWindowManager.mKeyguardDelegate.notifySoftKeyboardShown(shown);
        }
    }

    public boolean isButtonNeedReflect() {
        boolean wasButtonNeedReflect = this.mIsLightButton;
        this.mIsLightButton = false;
        return wasButtonNeedReflect;
    }

    public void updateLightButton(KeyEvent event, boolean down) {
        if (down && !this.mPhoneWindowManager.mDefaultDisplayPolicy.hasNavigationBar()) {
            int keyCode = event.getKeyCode();
            if (event.getRepeatCount() == 0 && (keyCode == 4 || keyCode == 82)) {
                this.mIsLightButton = true;
            } else {
                this.mIsLightButton = false;
            }
        }
    }

    private boolean isSpeLock() {
        if (this.mVivoPhoneLockManager == null) {
            this.mVivoPhoneLockManager = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoPhoneLockManager();
        }
        AbsVivoPhoneLockManager absVivoPhoneLockManager = this.mVivoPhoneLockManager;
        return absVivoPhoneLockManager != null && absVivoPhoneLockManager.isPhoneLockedEnable() == 1 && this.mVivoPhoneLockManager.isPhoneLocked() == 1;
    }

    public boolean dismissKeyguardLaunchHome(int displayId, boolean awakenFromDreams) {
        if (this.mPhoneWindowManager.mKeyguardOccluded && this.mPhoneWindowManager.mKeyguardDelegate.isShowing() && !this.mPhoneWindowManager.isKeyguardSecure(ActivityManager.getCurrentUser()) && !isSpeLock()) {
            this.mPhoneWindowManager.mKeyguardDelegate.dismiss(new AnonymousClass1(displayId, awakenFromDreams), (CharSequence) null);
            return true;
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.android.server.policy.VivoPhoneWindowManagerImpl$1  reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass1 extends KeyguardDismissCallback {
        final /* synthetic */ boolean val$awakenFromDreams;
        final /* synthetic */ int val$displayId;

        AnonymousClass1(int i, boolean z) {
            this.val$displayId = i;
            this.val$awakenFromDreams = z;
        }

        public void onDismissSucceeded() throws RemoteException {
            Handler handler = VivoPhoneWindowManagerImpl.this.mHandler;
            final int i = this.val$displayId;
            final boolean z = this.val$awakenFromDreams;
            handler.post(new Runnable() { // from class: com.android.server.policy.-$$Lambda$VivoPhoneWindowManagerImpl$1$uIBE1TrxO9Ju1CaNXI2biL1-s6o
                @Override // java.lang.Runnable
                public final void run() {
                    VivoPhoneWindowManagerImpl.AnonymousClass1.this.lambda$onDismissSucceeded$0$VivoPhoneWindowManagerImpl$1(i, z);
                }
            });
        }

        public /* synthetic */ void lambda$onDismissSucceeded$0$VivoPhoneWindowManagerImpl$1(int displayId, boolean awakenFromDreams) {
            VivoPhoneWindowManagerImpl.this.mPhoneWindowManager.startDockOrHome(displayId, true, awakenFromDreams);
        }
    }

    public String getWakeKeyDetails(int keyCode) {
        if (keyCode != 3) {
            if (keyCode != 304) {
                if (keyCode != 309) {
                    return "android.policy:KEY";
                }
                return "Face";
            }
            return "Fingerprint";
        }
        return "HomeKey";
    }

    public void initVivoInputPolicy(Context context, IWindowManager windowManager, WindowManagerPolicy.WindowManagerFuncs windowManagerFuncs) {
        this.mVivoInputPolicy.initVivoInputPolicy(context, windowManager, windowManagerFuncs);
    }

    public boolean isMusicActive() {
        return this.mVivoInputPolicy.isMusicActive();
    }

    public void finishScreenTurningOn() {
        this.mVivoInputPolicy.finishScreenTurningOn();
    }

    public boolean isPhysiscalHomeKey(int keyCode) {
        return this.mVivoInputPolicy.isPhysiscalHomeKey(keyCode);
    }

    public int interceptKeyBeforeQueueing(KeyEvent event, int policyFlags, boolean isScreenOn, boolean keyguardActive) {
        return this.mVivoInputPolicy.interceptKeyBeforeQueueing(event, policyFlags, isScreenOn, keyguardActive);
    }

    public int interceptKeyBeforeDispatching(IBinder focusedToken, KeyEvent event, int policyFlags, boolean keyguardOn) {
        KeyInterceptionInfo keyInterceptionInfo = this.mWindowManagerInternal.getKeyInterceptionInfoFromToken(focusedToken);
        return this.mVivoInputPolicy.interceptKeyBeforeDispatching(keyInterceptionInfo, event, policyFlags, keyguardOn);
    }

    public void interceptScreenshotChord() {
        this.mVivoInputPolicy.interceptScreenshotChord();
    }

    public boolean checkDisableGlobalActionsDialog() {
        return this.mVivoInputPolicy.checkDisableGlobalActionsDialog();
    }

    public void screenTurningOff() {
        this.mVivoInputPolicy.screenTurningOff();
    }

    public void onUserSwitched() {
        this.mVivoInputPolicy.onUserSwitched();
    }

    public void startVivoPay() {
        this.mVivoInputPolicy.startVivoPay();
    }

    public void cancelJoviVoice() {
        this.mVivoInputPolicy.cancelJoviVoice();
    }

    public boolean getScreenshotChordDelay() {
        return this.mVivoInputPolicy.getScreenshotChordDelay();
    }

    public void postScreenShotTimeOutRunnable(long delta) {
        this.mVivoInputPolicy.postScreenShotTimeOutRunnable(delta);
    }

    public void postDelayScreenShotTimeOutRunnable() {
        this.mVivoInputPolicy.postDelayScreenShotTimeOutRunnable();
    }

    public void triggerScreenshotChordVolumeUp(long time) {
        this.mVivoInputPolicy.triggerScreenshotChordVolumeUp(time);
    }

    public void notifyScreenOff() {
        this.mVivoInputPolicy.notifyScreenOff();
    }

    public boolean isSupportForLongPressHome() {
        return this.mVivoInputPolicy.isSupportForLongPressHome();
    }

    public boolean interceptHomeKey() {
        return this.mVivoInputPolicy.interceptHomeKey();
    }

    public boolean getInterceptWhenAlarm() {
        return this.mVivoInputPolicy.getInterceptWhenAlarm();
    }

    public void setInterceptWhenAlarm(boolean intercept) {
        this.mVivoInputPolicy.setInterceptWhenAlarm(intercept);
    }

    public int interceptPowerKeyDown(KeyEvent event, boolean interactive, boolean hungUp, boolean screenshotChordVolumeDownKeyTriggered, boolean a11yShortcutChordVolumeUpKeyTriggered, boolean gesturedServiceIntercepted) {
        return this.mVivoInputPolicy.interceptPowerKeyDown(event, interactive, hungUp, screenshotChordVolumeDownKeyTriggered, a11yShortcutChordVolumeUpKeyTriggered, gesturedServiceIntercepted);
    }

    public boolean interceptPowerKeyUp(KeyEvent event, boolean interactive, boolean canceled) {
        return this.mVivoInputPolicy.interceptPowerKeyUp(event, interactive, canceled);
    }

    public int interceptPowerKeyUpForMultiDisplay(boolean interactive) {
        return this.mVivoInputPolicy.interceptPowerKeyUpForMultiDisplay(interactive);
    }

    public int handleHomeButton(IBinder focusedToken) {
        KeyInterceptionInfo keyInterceptionInfo = this.mWindowManagerInternal.getKeyInterceptionInfoFromToken(focusedToken);
        return this.mVivoInputPolicy.handleHomeButton(keyInterceptionInfo);
    }

    public void adjustSplashScreen(Context context, String packageName, View view, WindowManager.LayoutParams params) {
        try {
            if (NightModeController.getInstance().isSupportVivoNightMode()) {
                ApplicationInfo info = context.getPackageManager().getApplicationInfo(packageName, 128);
                IVivoNightModeManager iNm = NightModeController.getInstance().getNightModeService();
                boolean forceInvert = false;
                boolean invert = true;
                boolean appSupport = true;
                if (info.metaData != null) {
                    String invertSplash = String.valueOf(info.metaData.get("android.vivo_invert_splash"));
                    forceInvert = "true".equals(invertSplash);
                    boolean z = true;
                    invert = !"false".equals(invertSplash);
                    boolean appSupport2 = info.metaData.getBoolean("android.vivo_nightmode_support", true);
                    if (!appSupport2 && !NightModeController.getInstance().isForceAppEnabled(packageName)) {
                        z = false;
                    }
                    appSupport = z;
                }
                boolean disable = false;
                if (iNm != null) {
                    disable = iNm.isDisableNightMode(packageName);
                }
                if (isInNightModeSplashExcludeList(packageName)) {
                    view.setNightMode(11);
                } else if (!forceInvert && (!invert || !appSupport || disable)) {
                    view.setNightMode(0);
                }
            }
            AppPreviewAdjuster.getInstance().adjustPreview(packageName, view, params);
        } catch (Exception e) {
        }
    }

    public void updateOrientationListenerFromSplit() {
        this.mPhoneWindowManager.mDefaultDisplayRotation.updateOrientationListener();
    }

    public void startDockOrHomeBoost() {
        if (this.mPerf == null) {
            this.mPerf = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoPerfManager((Context) null);
        }
        AbsVivoPerfManager absVivoPerfManager = this.mPerf;
        if (absVivoPerfManager != null) {
            absVivoPerfManager.perfHint(4229, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK, -1, 1);
        }
    }

    public void onBootMessageDialogShown(boolean shown) {
        if (this.mPhoneWindowManager.mKeyguardDelegate != null) {
            this.mPhoneWindowManager.mKeyguardDelegate.onBootMessageDialogShown(shown);
            return;
        }
        VSlog.w(TAG, "keyguard not ready to notify boot message dialog shown: " + shown);
    }

    public void systemReady() {
        this.mVivoInputPolicy.systemReady();
    }

    public void dump(String prefix, PrintWriter pw) {
        this.mVivoInputPolicy.dump(prefix, pw);
    }

    public boolean shouldDisablePilferPointers(String opPackageName) {
        return this.mVivoInputPolicy.shouldDisablePilferPointers(opPackageName);
    }

    public boolean shouldDispatchDisplay(int displayId) {
        return this.mVivoAppShareManager.isAppsharedDisplayId(displayId);
    }

    public boolean shouldShowWhenKeyguardLocked(WindowManagerPolicy.WindowState win, boolean allowWhenLocked) {
        if (win == null || !win.isInputMethodWindow()) {
            return allowWhenLocked;
        }
        if (!this.mVivoAppShareManager.isAppsharedMode()) {
            return allowWhenLocked;
        }
        return true;
    }

    public boolean shouldHideIme() {
        return !this.mVivoAppShareManager.isAppsharedMode();
    }

    public boolean isSpecialVolumeKey(int keyCode, Display display) {
        boolean displayOn = display != null && display.getState() == 2;
        return (keyCode == 0 && !displayOn) || keyCode == 313 || keyCode == 314 || keyCode == 315 || keyCode == 318 || keyCode == 319 || keyCode == 320;
    }

    public boolean shouldDisablPowerKeyForFingerprint(PowerManagerInternal powerManagerInternal, int delaytime) {
        if (FingerprintConfig.isSideFingerprint()) {
            PowerManager.WakeData lastWakeUp = powerManagerInternal.getLastWakeup();
            if (lastWakeUp != null && "android.policy:FINGERPRINT".equals(powerManagerInternal.getDetails())) {
                long now = SystemClock.uptimeMillis();
                if (now < lastWakeUp.wakeTime + delaytime) {
                    VSlog.i(TAG, "Sleep from power button suppressed. Time since fingerprint wakeup: " + (now - lastWakeUp.wakeTime) + "ms");
                    return true;
                }
            }
            if (FingerprintConfig.isPowerKeyDisabledByFingerprint(this.mContext)) {
                VSlog.i(TAG, "power goto sleep disable by Fingerprint");
                return true;
            }
            return false;
        }
        return false;
    }

    public void vibratorPro(int effectId) {
        this.mVivoInputPolicy.vibratorPro(effectId);
    }

    public void handleEasyShareWakeup(KeyEvent event) {
        if (this.mDisplayManagerInternal == null) {
            this.mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
        }
        DisplayManagerInternal displayManagerInternal = this.mDisplayManagerInternal;
        if (displayManagerInternal != null && displayManagerInternal.shouldWakeUpWhileInteractive()) {
            this.mPhoneWindowManager.wakeUpFromPowerKey(event.getDownTime());
            this.cancelGoToSleep = true;
        }
    }

    public boolean cancelGoToSleep() {
        if (this.cancelGoToSleep) {
            this.cancelGoToSleep = false;
            return true;
        }
        return false;
    }

    public void onBacklightStateChanged(final int displayId, final int state, final int brightness) {
        Handler handler;
        PowerManagerInternal powerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
        if (powerManagerInternal != null && (handler = powerManagerInternal.getPowerThreadHandler()) != null) {
            handler.post(new Runnable() { // from class: com.android.server.policy.VivoPhoneWindowManagerImpl.2
                @Override // java.lang.Runnable
                public void run() {
                    VivoPhoneWindowManagerImpl.this.onBacklightStateChangedInternal(displayId, state, brightness);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onBacklightStateChangedInternal(int displayId, int state, int brightness) {
        if (displayId != 0) {
            VSlog.e(TAG, "onBacklightStateChangedInternal invalid displayId " + displayId);
            return;
        }
        if ((this.mDisplayBacklight > 0 && brightness <= 0) || (-1 == this.mDisplayBacklight && brightness == 0)) {
            this.mDisplayBacklight = brightness;
            VSlog.i(TAG, "onDisplayStateBeginChange: turns off");
            this.mPhoneWindowManager.mKeyguardDelegate.screenFadingOn(false, 0);
        }
        if (this.mDisplayBacklight <= 0 && brightness > 0) {
            this.mDisplayBacklight = brightness;
            VSlog.i(TAG, "onDisplayStateBeginChange: turns on");
            this.mPhoneWindowManager.mKeyguardDelegate.screenFadingOn(true, 0);
        }
    }

    public void onKeyguardLockChanged() {
        if (this.mDisplayManagerInternal == null) {
            this.mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
        }
        DisplayManagerInternal displayManagerInternal = this.mDisplayManagerInternal;
        if (displayManagerInternal != null) {
            displayManagerInternal.onKeyguardLockChanged();
        } else {
            VSlog.d(TAG, "get DisplayManagerInternal failed!");
        }
    }

    public void setOccluded(boolean occluded) {
        if (this.mDisplayManagerInternal == null) {
            this.mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
        }
        DisplayManagerInternal displayManagerInternal = this.mDisplayManagerInternal;
        if (displayManagerInternal != null) {
            displayManagerInternal.setOccluded(occluded);
        } else {
            VSlog.d(TAG, "get DisplayManagerInternal failed!");
        }
    }

    public void screenFadingOn(boolean isOn, int code) {
        PhoneWindowManager phoneWindowManager = this.mPhoneWindowManager;
        if (phoneWindowManager != null && phoneWindowManager.mKeyguardDelegate != null) {
            this.mPhoneWindowManager.mKeyguardDelegate.screenFadingOn(isOn, code);
        }
    }

    public void setInterceptInputKeyStatus(boolean enable) {
        this.mVivoInputPolicy.setInterceptInputKeyStatus(enable);
    }

    public void addWhiteBackgroundIfNeeded(View view, String packageName) {
        if (packageName.contains("com.bbk.launcher")) {
            return;
        }
        Drawable background = view.getBackground();
        if (!view.isOpaque() && background != null && (background instanceof ColorDrawable) && ((ColorDrawable) background).getColor() == 0) {
            VSlog.d(TAG, "addStartingWindow splash screen background is transparent, add a white background.");
            view.setBackgroundColor(-1);
        }
    }

    public void onKeyGuardChange(boolean phoneLocked) {
        this.mVivoInputPolicy.onKeyGuardChange(phoneLocked);
    }
}