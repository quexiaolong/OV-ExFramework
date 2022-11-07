package com.android.server.inputmethod;

import android.app.ActivityThread;
import android.app.AlertDialog;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.multidisplay.MultiDisplayManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.FtFeature;
import android.view.ContextThemeWrapper;
import android.view.InputChannel;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import com.android.internal.view.IInputContext;
import com.android.internal.view.IInputMethod;
import com.android.internal.view.IInputMethodClient;
import com.android.internal.view.IInputMethodSession;
import com.android.internal.view.InputBindResult;
import com.android.server.LocalServices;
import com.android.server.SafeScreenUtil;
import com.android.server.SystemService;
import com.android.server.VivoDoubleInstanceServiceImpl;
import com.android.server.am.frozen.FrozenInjectorImpl;
import com.android.server.am.frozen.FrozenQuicker;
import com.android.server.display.VivoCastDisplayUtil;
import com.android.server.inputmethod.InputMethodManagerService;
import com.android.server.policy.VivoPolicyConstant;
import com.android.server.policy.VivoRatioControllerUtilsImpl;
import com.android.server.statusbar.StatusBarManagerService;
import com.android.server.wm.VivoAppShareManager;
import com.android.server.wm.VivoEasyShareManager;
import com.android.server.wm.VivoFreeformUtils;
import com.android.server.wm.VivoMultiWindowConfig;
import com.vivo.appshare.AppShareConfig;
import com.vivo.face.common.data.Constants;
import com.vivo.services.rms.proxy.VivoBinderProxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import vivo.util.VSlog;
import vivo.util.VivoThemeUtil;

/* loaded from: classes.dex */
public class VivoInputMethodManagerServiceImpl implements IVivoInputMethodManagerService {
    private static final String INPUT_METHOD_SERVICE_SECURE = "input_method_secure";
    private static final boolean IS_VIVO_FREEFORM_SUPPORT;
    private static final String SECURE_INPUTMETHOD = "vivo_secure_input_method";
    private static final String TAG_APPSHARE = "AppShare-InputMethodManagerService";
    private static ArrayList<String> mDefaultInputMethodList;
    private static int sFreeformInputNotiTimes;
    private boolean DEBUG;
    private String TAG;
    public boolean isCall;
    private boolean isLogOpen;
    private String mAppSharePackageName;
    private int mAppShareUserId;
    private HashMap<Integer, Integer> mCallMethodMap;
    private ContentResolver mContentResolver;
    private Context mContext;
    int mCurInputMethodUid;
    private String mCurMethodId;
    private int mCurrentUserId;
    private InputMethodManagerService mImms;
    InputMethodManagerService mImmsB;
    private InputMethodManagerService mImmsNomal;
    private InputMethodManagerService mImmsSecure;
    private boolean mIsSecImms;
    private VivoAppShareManager mVivoAppShareManager;
    private VivoDoubleInstanceServiceImpl mVivoDoubleInstanceService;
    private VivoRatioControllerUtilsImpl mVivoRatioControllerUtils;
    public static String VIVO_SECURE_INPUTMETHOD = "com.vivo.secime.service/.SecIME";
    public static String VIVO_SECURE_INPUTMETHOD_PACKAGENAME = "com.vivo.secime.service";
    public static String VIVO_SECURE_INPUTMETHOD_SERVICE = "com.vivo.secime.service.SecIME";
    static boolean mPasswordInputType = false;
    static boolean mSecureImsOn = true;
    static boolean mHasSecureIME = false;
    static final Integer INDEX_ADD_CLIENTS = 1;

    static {
        ArrayList<String> arrayList = new ArrayList<>();
        mDefaultInputMethodList = arrayList;
        arrayList.add("com.baidu.input_bbk.service");
        mDefaultInputMethodList.add("com.sohu.inputmethod.sogou.vivo");
        mDefaultInputMethodList.add("com.baidu.input_vivo");
        sFreeformInputNotiTimes = 0;
        IS_VIVO_FREEFORM_SUPPORT = FtFeature.isFeatureSupport("vivo.software.freeform");
    }

    public VivoInputMethodManagerServiceImpl(InputMethodManagerService imms, Context context) {
        boolean equals = "yes".equals(SystemProperties.get("persist.sys.log.ctrl", "no"));
        this.isLogOpen = equals;
        if (!equals) {
            Build.TYPE.equals("eng");
        }
        this.DEBUG = true;
        this.TAG = "InputMethodManagerService:";
        this.mIsSecImms = false;
        this.mCallMethodMap = new HashMap<>();
        this.isCall = false;
        this.mVivoDoubleInstanceService = null;
        this.mVivoAppShareManager = null;
        this.mAppSharePackageName = null;
        this.mAppShareUserId = -1;
        this.mCurInputMethodUid = 0;
        this.mImms = imms;
        this.TAG = imms.TAG;
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        updateSecureImsOn(UserHandle.getCallingUserId());
        this.mVivoDoubleInstanceService = VivoDoubleInstanceServiceImpl.getInstance();
        this.mVivoAppShareManager = VivoAppShareManager.getInstance();
        this.mVivoRatioControllerUtils = VivoRatioControllerUtilsImpl.getInstance();
    }

    private boolean hasSecureIME() {
        Context context;
        if (!FtFeature.isFeatureSupport("vivo.software.secureinput") || (context = this.mContext) == null || context.getPackageManager() == null) {
            return false;
        }
        List<ResolveInfo> list = this.mContext.getPackageManager().queryIntentServicesAsUser(new Intent("android.view.InputMethod"), 32896, this.mCurrentUserId);
        for (int i = 0; i < list.size(); i++) {
            ResolveInfo info = list.get(i);
            if (info != null && info.serviceInfo != null && VIVO_SECURE_INPUTMETHOD_SERVICE.equals(info.serviceInfo.name)) {
                mHasSecureIME = true;
                return true;
            }
        }
        return false;
    }

    public void setSecFlag(boolean isSecImms) {
        this.mIsSecImms = isSecImms;
        this.mImmsNomal = VivoInputMethodManagerServiceLifecycleImpl.mService;
        this.mImmsSecure = Lifecycle.mService;
        if (isSecImms) {
            this.TAG = "SecInputMethodManagerService";
            this.mImms.TAG = "SecInputMethodManagerService";
            InputMethodManagerService inputMethodManagerService = this.mImmsNomal;
            this.mImmsB = inputMethodManagerService;
            inputMethodManagerService.mVivoImms.setSecFlag(false);
            hasSecureIME();
        } else {
            this.TAG = "InputMethodManagerService";
            this.mImms.TAG = "InputMethodManagerService";
            this.mImmsB = this.mImmsSecure;
        }
        String str = this.TAG;
        VSlog.d(str, "setSecFlag mImms=" + this.mImms + " ,mImmsB=" + this.mImmsB + " ,mImmsNomal=" + this.mImmsNomal + " ,mImmsSecure=" + this.mImmsSecure);
    }

    public boolean isSecImms() {
        return this.mIsSecImms;
    }

    public void onUnlockUser(int userId, int curUserId) {
        String str = this.mImms.TAG;
        VSlog.d(str, "userId=" + userId + " ,curUserId=" + curUserId);
        if (isSecImms()) {
            return;
        }
        DefaultImeJobService.scheduleJob(this.mImmsNomal, this.mContext);
        String str2 = this.mImmsNomal.TAG;
        VSlog.d(str2, "mSecureImsOn=" + mSecureImsOn + " ,userId=" + userId);
        Uri uri = Settings.Secure.getUriFor(SECURE_INPUTMETHOD);
        SettingsDbObs obs = new SettingsDbObs(this.mImmsNomal.mHandler);
        this.mContentResolver.registerContentObserver(uri, false, obs, -1);
    }

    public void registerLogBroadcast(Handler handler) {
        IntentFilter bbklogFilter = new IntentFilter();
        bbklogFilter.addAction(VivoPolicyConstant.ACTION_VIVO_LOG_CHANGED);
        this.mContext.registerReceiver(new BroadcastReceiver() { // from class: com.android.server.inputmethod.VivoInputMethodManagerServiceImpl.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                boolean status = "on".equals(intent.getStringExtra("adblog_status"));
                String str = VivoInputMethodManagerServiceImpl.this.TAG;
                VSlog.w(str, "*****SWITCH LOG TO " + status);
                InputMethodUtils.DEBUG = status;
            }
        }, bbklogFilter, null, handler);
    }

    public Notification.Builder chanageImeSwitchDialogUI(Notification.Builder builder) {
        builder.setSmallIcon(50463629);
        return builder;
    }

    public void updateInputMethod(String inputMethod) {
        FrozenInjectorImpl.getInstance().updateInputMethod(inputMethod);
        VivoBinderProxy.getInstance().updateInputMethod(inputMethod);
    }

    public AlertDialog.Builder chanageDialogUI(int displayId) {
        ActivityThread currentThread = ActivityThread.currentActivityThread();
        Context settingsContext = new ContextThemeWrapper((Context) (displayId == 0 ? currentThread.getSystemUiContext() : currentThread.createSystemUiContext(displayId)), VivoThemeUtil.getSystemThemeStyle(VivoThemeUtil.ThemeType.DIALOG_ALERT));
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(settingsContext, 51314792);
        dialogBuilder.setTitle(17041729);
        dialogBuilder.setIconAttribute(16843605);
        return dialogBuilder;
    }

    public void chanageDialogPosition(AlertDialog switchingDialog) {
        WindowManager.LayoutParams lp = switchingDialog.getWindow().getAttributes();
        lp.gravity = 80;
        lp.width = -1;
        lp.height = -2;
        switchingDialog.setCanceledOnTouchOutside(true);
    }

    public void setClientToNullLocked() {
        VSlog.d(this.mImmsB.TAG, "setClientToNullLocked-- set mCurClient to null");
        this.mImmsB.mCurClient = null;
    }

    public void unbindInputLocked() {
        if (this.mImmsB.mBoundToMethod && this.mImmsB.mCaller != null && this.mImmsB.mCurMethod != null) {
            this.mImmsB.mBoundToMethod = false;
            VSlog.d(this.mImmsB.TAG, "set mCurClient to null, mBoundToMethod = false, MSG_UNBIND_INPUT");
            InputMethodManagerService inputMethodManagerService = this.mImmsB;
            inputMethodManagerService.executeOrSendMessage(inputMethodManagerService.mCurMethod, this.mImmsB.mCaller.obtainMessageO(1000, this.mImmsB.mCurMethod));
            this.mImmsB.mCurClient = null;
        }
    }

    public void attachNewInputLocked(int startInputReason, boolean initial) {
        if (!this.mImmsB.mBoundToMethod && this.mImmsB.mCaller != null && this.mImmsB.mCurMethod != null && this.mImmsB.mCurClient != null) {
            InputMethodManagerService inputMethodManagerService = this.mImmsB;
            inputMethodManagerService.executeOrSendMessage(inputMethodManagerService.mCurMethod, this.mImmsB.mCaller.obtainMessageOO((int) Constants.CMD.CMD_HIDL_KEYGUARD_CLOSED, this.mImmsB.mCurMethod, this.mImmsB.mCurClient.binding));
            this.mImmsB.mBoundToMethod = true;
            VSlog.d(this.mImmsB.TAG, "mBoundToMethod = true, MSG_BIND_INPUT");
        }
    }

    public void setCurClientLocked(IBinder client, InputMethodManagerService.ClientState curClient) {
        String str = this.TAG;
        VSlog.d(str, "setCurClientLocked-- set mCurClient to " + curClient);
        InputMethodManagerService inputMethodManagerService = this.mImmsB;
        inputMethodManagerService.mCurClient = (InputMethodManagerService.ClientState) inputMethodManagerService.mClients.get(client);
        String str2 = this.mImmsB.TAG;
        VSlog.d(str2, "mImmsB setCurClientLocked-- set mCurClient to " + this.mImmsB.mCurClient);
    }

    public void setCurFocusedWindow(IBinder curFocusedWindow) {
        this.mImmsB.mCurFocusedWindow = curFocusedWindow;
        String str = this.mImmsB.TAG;
        VSlog.d(str, "mCurFocusedWindow = " + this.mImmsB.mCurFocusedWindow);
    }

    public void setInteractive(boolean interactive) {
        if (!isSecImms()) {
            this.mImmsB.mIsInteractive = interactive;
            String str = this.mImmsB.TAG;
            VSlog.d(str, "mIsInteractive = " + this.mImmsB.mIsInteractive);
        }
    }

    public void setSystemImeEnable(ArrayList<InputMethodInfo> methodList) {
        if (!isSecImms() && methodList != null) {
            for (int i = 0; i < methodList.size(); i++) {
                InputMethodInfo imi = methodList.get(i);
                if (imi.isSystem()) {
                    String str = this.mImms.TAG;
                    VSlog.d(str, "setSystemImeEnable  imi= " + imi);
                    this.mImms.setInputMethodEnabledLocked(imi.getId(), true);
                }
            }
        }
    }

    public boolean bindCurrentInputMethodServiceLocked(Intent service, ServiceConnection conn, int flags) {
        String str = this.mImms.TAG;
        VSlog.d(str, "bindCurrentInputMethodServiceLocked  service= " + service + ", conn = " + conn + " ,flags=" + flags + " ,mIsSecImms=" + this.mIsSecImms);
        return true;
    }

    public boolean onSessionCreated(IInputMethod method, IInputMethodSession session, InputChannel channel, InputMethodManagerService.SessionState sessionState, InputMethodManagerService.ClientState curClient) {
        String str = this.TAG;
        VSlog.d(str, "onSessionCreated  method=" + method + " ,session=" + session + " ,channel=" + channel + " ,mIsSecImms=" + this.mIsSecImms);
        String str2 = this.TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("sessionState=");
        sb.append(sessionState);
        sb.append(" ,mCurClient=");
        sb.append(curClient);
        VSlog.d(str2, sb.toString());
        return true;
    }

    public boolean onServiceConnected(ComponentName name, IBinder service) {
        String str = this.TAG;
        VSlog.d(str, "onServiceConnected  name=" + name + " ,service=" + service + " ,mIsSecImms=" + this.mIsSecImms);
        return true;
    }

    public boolean printMethodString(String curId, String curMethodId) {
        this.mCurMethodId = curMethodId;
        String str = this.TAG;
        VSlog.e(str, "mCurId=" + curId + " ,mCurMethodId=" + curMethodId);
        return true;
    }

    public boolean resetDefaultImeLocked() {
        if (SystemProperties.get("ro.vivo.op.entry", "no").contains("CMCC_RW")) {
            return true;
        }
        return false;
    }

    /* loaded from: classes.dex */
    public static final class Lifecycle extends SystemService {
        static InputMethodManagerService mService;

        public Lifecycle(Context context) {
            super(context);
            mService = new InputMethodManagerService(context);
            VSlog.i("SecInputMethodManagerService", "new secure service=" + mService);
        }

        public void onStart() {
            publishBinderService(VivoInputMethodManagerServiceImpl.INPUT_METHOD_SERVICE_SECURE, mService);
        }

        public void onSwitchUser(int userHandle) {
            mService.scheduleSwitchUserTaskLocked(userHandle, (IInputMethodClient) null);
        }

        public void onBootPhase(int phase) {
            if (phase == 550) {
                StatusBarManagerService statusBarService = ServiceManager.getService("statusbar");
                mService.systemRunning(statusBarService);
            }
        }

        public void onUnlockUser(int userHandle) {
            mService.mHandler.sendMessage(mService.mHandler.obtainMessage(FrozenQuicker.FREEZE_STATUS_CHECK_MS, userHandle, 0));
        }

        public InputMethodManagerService getService() {
            return mService;
        }

        public void setSecFlag(boolean secure) {
            mService.mVivoImms.setSecFlag(secure);
        }
    }

    public void scheduleSwitchUserTaskLocked(int userId) {
        this.mCurrentUserId = userId;
        updateSecureImsOn(userId);
    }

    /* loaded from: classes.dex */
    class SettingsDbObs extends ContentObserver {
        private ContentResolver mContentResolver;
        private Uri uri;

        public SettingsDbObs(Handler handler) {
            super(handler);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri, int userId) {
            VivoInputMethodManagerServiceImpl.this.updateSecureImsOn(userId);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateSecureImsOn(int userId) {
        String str = Settings.Secure.getStringForUser(this.mContentResolver, SECURE_INPUTMETHOD, userId);
        if ("1".equals(str)) {
            mSecureImsOn = true;
        } else if ("0".equals(str)) {
            mSecureImsOn = false;
        } else {
            mSecureImsOn = true;
            Settings.Secure.putStringForUser(this.mContext.getContentResolver(), SECURE_INPUTMETHOD, "1", userId);
        }
        String str2 = this.TAG;
        VSlog.d(str2, "mSecureImsOn:" + mSecureImsOn + " userId=" + userId + " ,mHasSecureIME=" + mHasSecureIME);
    }

    private boolean isPasswordInputType(EditorInfo editorInfoOut) {
        if (editorInfoOut == null) {
            return mPasswordInputType;
        }
        int variation = editorInfoOut.inputType & 4095;
        return variation == 129 || variation == 225 || variation == 18 || variation == 145;
    }

    public void setLastInputTypeInCaseBindFail(EditorInfo editorInfoOut) {
        mPasswordInputType = isPasswordInputType(editorInfoOut);
        String str = this.TAG;
        VSlog.w(str, "setLastInputTypeInCaseBindFail mPasswordInputType=" + mPasswordInputType);
    }

    private boolean isSecImsOn() {
        String str = this.mCurMethodId;
        return (str == null || !str.contains("com.android.cts.mockime")) && mSecureImsOn && mHasSecureIME;
    }

    public boolean showInputMethodPickerFromClient() {
        if (mSecureImsOn && mPasswordInputType) {
            return false;
        }
        return true;
    }

    public boolean setInputMethodEnabledLocked(String id, boolean enabled) {
        if (isSecImms() || !VIVO_SECURE_INPUTMETHOD.equals(id) || enabled) {
            return true;
        }
        String str = this.TAG;
        VSlog.w(str, "can not set " + id + " to false");
        return true;
    }

    public InputBindResult startInputOrWindowGainedFocus(int startInputReason, IInputMethodClient client, IBinder windowToken, int startInputFlags, int softInputMode, int windowFlags, EditorInfo attribute, IInputContext inputContext, int missingMethods, int unverifiedTargetSdkVersion, int userId) {
        boolean isPassword = isPasswordInputType(attribute);
        String str = this.TAG;
        VSlog.d(str, "startInputOrWindowGainedFocusInternalLocked client=" + client + " ,windowToken=" + windowToken + " ,isPassword=" + isPassword + " ,mIsSecImms=" + this.mIsSecImms);
        SafeScreenUtil.getInstance().updatePasswordMode(isPassword);
        if (VivoCastDisplayUtil.getInstance() != null) {
            VivoCastDisplayUtil.getInstance().updatePasswordModeIfNeed(isPassword);
        }
        if (!isPassword || !isSecImsOn()) {
            this.mImmsSecure.hideCurrentInputLocked(windowToken, 0, (ResultReceiver) null, 3);
            InputBindResult res = this.mImmsNomal.startInputOrWindowGainedFocusInternalLocked(startInputReason, client, windowToken, startInputFlags, softInputMode, windowFlags, attribute, inputContext, missingMethods, unverifiedTargetSdkVersion, userId);
            return res;
        }
        this.mImmsNomal.hideCurrentInputLocked(windowToken, 0, (ResultReceiver) null, 3);
        InputBindResult res2 = this.mImmsSecure.startInputOrWindowGainedFocusInternalLocked(startInputReason, client, windowToken, startInputFlags, softInputMode, windowFlags, attribute, inputContext, missingMethods, unverifiedTargetSdkVersion, userId);
        return res2;
    }

    public void addClient(IInputMethodClient client, IInputContext inputContext, int callerUid, int callerPid, int selfReportedDisplayId) {
        IBinder.DeathRecipient clientDeathRecipient = new InputMethodManagerService.ClientDeathRecipient(this.mImmsB, client);
        try {
            client.asBinder().linkToDeath(clientDeathRecipient, 0);
            InputMethodManagerService.ClientState cs = new InputMethodManagerService.ClientState(client, inputContext, callerUid, callerPid, selfReportedDisplayId, clientDeathRecipient);
            this.mImmsB.mClients.put(client.asBinder(), cs);
        } catch (RemoteException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean calledWithValidTokenLocked(IBinder token) {
        if (!isSecImms() && token == this.mImmsB.mCurToken) {
            return true;
        }
        return false;
    }

    public boolean hideMySoftInput(IBinder windowToken, IBinder token, int flags, int reason) {
        if (this.mImms.mCurToken == token) {
            return this.mImms.hideCurrentInputLocked(windowToken, flags, (ResultReceiver) null, reason);
        }
        String str = this.TAG;
        VSlog.e(str, "Ignoring due to an invalid token, hideMySoftInput mImms.mCurToken=" + this.mImms.mCurToken + " ,token=" + token + " ,flags=" + flags + " ,mIsSecImms=" + this.mIsSecImms);
        return false;
    }

    public boolean showMySoftInput(IBinder windowToken, IBinder token, int flags, int reason) {
        String str = this.TAG;
        VSlog.w(str, "showMySoftInput mCurToken=" + this.mImms.mCurToken + ",mLastImeTargetWindow=" + this.mImms.mLastImeTargetWindow + " ,token=" + token + " ,flags=" + flags);
        if (this.mImms.mCurToken == token) {
            return this.mImms.showCurrentInputLocked(windowToken, flags, (ResultReceiver) null, reason);
        }
        String str2 = this.TAG;
        VSlog.e(str2, "Ignoring due to an invalid token, showMySoftInput mImms.mCurToken=" + this.mImms.mCurToken + " ,token=" + token + " ,flags=" + flags + " ,mIsSecImms=" + this.mIsSecImms);
        return false;
    }

    public boolean hideSoftInput(IInputMethodClient client, IBinder windowToken, int flags, ResultReceiver resultReceiver, int reason) {
        if (isSecImsOn() && mPasswordInputType) {
            return this.mImmsSecure.hideCurrentInputLocked(windowToken, flags, resultReceiver, reason);
        }
        return this.mImmsNomal.hideCurrentInputLocked(windowToken, flags, resultReceiver, reason);
    }

    public boolean showSoftInput(IInputMethodClient client, IBinder windowToken, int flags, ResultReceiver resultReceiver, int reason) {
        if (isSecImsOn() && mPasswordInputType) {
            return this.mImmsSecure.showCurrentInputLocked(windowToken, flags, resultReceiver, reason);
        }
        return this.mImmsNomal.showCurrentInputLocked(windowToken, flags, resultReceiver, reason);
    }

    public boolean shouldBuildInputMethodList(String strId) {
        return isSecImms() ? !VIVO_SECURE_INPUTMETHOD_PACKAGENAME.equals(strId) : VIVO_SECURE_INPUTMETHOD_PACKAGENAME.equals(strId);
    }

    private boolean isExport() {
        return "yes".equals(SystemProperties.get("ro.vivo.product.overseas", "no"));
    }

    public void setInputMethodForSpecailMode(InputMethodInfo im, int subtypeId) {
        if (this.mImms.mWindowManagerInternal.isVivoMultiWindowSupport() && !isExport()) {
            final boolean isMultiWindow = this.mImms.mWindowManagerInternal.isInVivoMultiWindowIgnoreVisibilityFocusedDisplay();
            if (isMultiWindow) {
                if (isMultiWindow && (mDefaultInputMethodList.contains(im.getPackageName()) || VivoMultiWindowConfig.getInstance().isVivoAllowSplitInputMethod(im.getPackageName()))) {
                    if (VivoMultiWindowConfig.DEBUG) {
                        String str = this.TAG;
                        VSlog.i(str, "setInputMethod " + im.getPackageName() + " " + im.getId());
                    }
                    this.mImms.setInputMethodLocked(im.getId(), subtypeId);
                    return;
                }
                if (VivoMultiWindowConfig.DEBUG) {
                    String str2 = this.TAG;
                    VSlog.i(str2, "can not setInputMethod " + im.getPackageName() + " " + im.getId());
                }
                new Thread(new Runnable() { // from class: com.android.server.inputmethod.VivoInputMethodManagerServiceImpl.2
                    @Override // java.lang.Runnable
                    public void run() {
                        Looper.prepare();
                        if (isMultiWindow) {
                            Toast.makeText(VivoInputMethodManagerServiceImpl.this.mContext, 51249667, 0).show();
                        }
                        Looper.loop();
                    }
                }).start();
                return;
            }
            this.mImms.setInputMethodLocked(im.getId(), subtypeId);
            return;
        }
        this.mImms.setInputMethodLocked(im.getId(), subtypeId);
    }

    public void dummy() {
    }

    public boolean isDoubleInstanceUser(int userId) {
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl != null && vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable() && userId == 999) {
            return true;
        }
        return false;
    }

    public void remindForFreeformImeToDefault(boolean show) {
        try {
            if (sFreeformInputNotiTimes < 3) {
                sFreeformInputNotiTimes = Settings.System.getIntForUser(this.mContext.getContentResolver(), VivoFreeformUtils.VIVO_SETTINGS_IME_FREEFORM_REMIND_TIME, 0, this.mImms.mSettings.getCurrentUserId());
            }
            if (sFreeformInputNotiTimes < 3 && this.mImms.mIWindowManager.isWmInVivoFreeForm()) {
                String str = this.TAG;
                VSlog.d(str, "remindForFreeformImeToDefault show = " + show);
                Settings.System.putIntForUser(this.mContext.getContentResolver(), VivoFreeformUtils.VIVO_FREEFORM_IME_SETTINGS_STATE, show ? 1 : 0, this.mImms.mSettings.getCurrentUserId());
            }
        } catch (RemoteException e) {
            String str2 = this.TAG;
            VSlog.e(str2, "remindForFreeformImeToDefault e:" + e.getMessage());
        }
    }

    public void miniMizeFreeformWhenShowSoftInputIfNeed() {
        try {
            if (IS_VIVO_FREEFORM_SUPPORT && VivoFreeformUtils.sIsVosProduct && this.mImms.mIWindowManager.isWmInVivoFreeForm()) {
                this.mImms.mIWindowManager.miniMizeFreeformWhenShowSoftInputIfNeed(this.mImms.mCurFocusedWindow);
            }
        } catch (RemoteException e) {
            String str = this.TAG;
            VSlog.e(str, "miniMizeFreeformWhenShowSoftInputIfNeed e:" + e.getMessage());
        }
    }

    public void hideSoftInputFromPCShare() {
        if (VivoEasyShareManager.SUPPORT_PCSHARE && this.mImms.mInputShown) {
            this.mImms.closeInputMethodAppShare();
        }
    }

    public boolean isMotionEventFromPCShare() {
        if (!VivoEasyShareManager.SUPPORT_PCSHARE) {
            return false;
        }
        return this.mImms.mWindowManagerInternal.isMotionEventFromPCShare();
    }

    public void commitText(String text) {
        if (!VivoEasyShareManager.SUPPORT_PCSHARE || text == null || text.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK)) {
            return;
        }
        try {
            if (this.mImms.mCurInputContext != null) {
                this.mImms.mCurInputContext.commitText(text, text.length());
            }
        } catch (RemoteException e) {
            VSlog.e(this.TAG, "commitText error", e);
        }
    }

    public boolean isCarNetworkingInputMethodShown() {
        boolean z = false;
        if (MultiDisplayManager.SUPPORT_CAR_NETWORKING) {
            synchronized (InputMethodManagerService.mLock) {
                boolean softInputShow = this.mImms.mCurMethod != null && this.mImms.mInputShown;
                if (InputMethodManager.sDebugMethod) {
                    VSlog.d("VivoInputMethod", "softInputShow : " + softInputShow + " ,inputShown: " + this.mImms.mInputShown + " ,displayId: " + this.mImms.mCurTokenDisplayId);
                }
                if (softInputShow && this.mImms.mCurTokenDisplayId == 80000) {
                    z = true;
                }
            }
            return z;
        }
        return false;
    }

    public boolean hideSoftInputFromJovi(int flags, ResultReceiver resultReceiver) {
        boolean hideCurrentInputLocked;
        if (!MultiDisplayManager.SUPPORT_CAR_NETWORKING) {
            return false;
        }
        synchronized (InputMethodManagerService.mLock) {
            long ident = Binder.clearCallingIdentity();
            hideCurrentInputLocked = this.mImms.hideCurrentInputLocked((IBinder) null, flags, resultReceiver, 3);
            Binder.restoreCallingIdentity(ident);
        }
        return hideCurrentInputLocked;
    }

    public boolean isUsedByCarNetworking(int uid) {
        return MultiDisplayManager.SUPPORT_CAR_NETWORKING && this.mImms.mCurTokenDisplayId == 80000 && this.mCurInputMethodUid == uid;
    }

    public void updateInputMethodUid() {
        if (MultiDisplayManager.SUPPORT_CAR_NETWORKING && this.mImms.mCurTokenDisplayId == 80000) {
            PackageManagerInternal pmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
            this.mCurInputMethodUid = pmi.getUidForInputMethodApp(this.mImms.mCurIntent.getComponent().getPackageName());
        }
    }

    public boolean isInputShownInCarnetworking() {
        if (MultiDisplayManager.SUPPORT_CAR_NETWORKING && this.mImms.mCurTokenDisplayId == 80000) {
            if (InputMethodManager.sDebugMethod) {
                VSlog.d("VivoInputMethod", "isInputShownInCarnetworking true!");
                return true;
            }
            return true;
        }
        return false;
    }

    public void resetInputMethodUid() {
        if (!MultiDisplayManager.SUPPORT_CAR_NETWORKING) {
            return;
        }
        this.mCurInputMethodUid = 0;
    }

    public void updateImeReadyShown(int displayId, boolean ready) {
        this.mVivoAppShareManager.updateImeReadyShown(displayId, ready);
    }

    public void readyToSwitchDisplay(int displayId, boolean hidecurrent) {
        this.mVivoAppShareManager.readyToSwitchDisplay(displayId, hidecurrent);
    }

    public void updateImeMenuShowDisplay(int displayId) {
        this.mVivoAppShareManager.updateImeMenuShowDisplay(displayId);
    }

    public void onImeMenuDestory() {
        this.mVivoAppShareManager.onImeMenuDestory();
    }

    public void reportLastShownDisplayId(int displayId) {
        this.mVivoAppShareManager.reportLastShownDisplayId(displayId);
    }

    public void reportMoveToDisplayCompleted(int displayId) {
        this.mVivoAppShareManager.reportMoveToDisplayCompleted(displayId);
    }

    public void tryInjectMotionEvent(int displayId, int pointX, int pointY) {
        this.mVivoAppShareManager.tryInjectMotionEvent(displayId, pointX, pointY);
    }

    public boolean isAppSharedMode() {
        return this.mVivoAppShareManager.isAppsharedMode();
    }

    public boolean isImeShowInShareDisplay() {
        return this.mVivoAppShareManager.isImeShowInShareDisplay();
    }

    public void updateWakefulness(int wakeness) {
        this.mVivoAppShareManager.updateWakefulness(wakeness);
    }

    public boolean isCannotShowImeMenu(int displayId) {
        return this.mVivoAppShareManager.isCannotShowImeMenu(displayId);
    }

    public void updateImeSwitching(boolean switching) {
        this.mVivoAppShareManager.updateImeSwitchingStart(switching);
    }

    public boolean isInputMethodProcess(int uid, int pid) {
        return this.mVivoAppShareManager.isInputMethodProcess(uid, pid);
    }

    public int adjustShowMethodMenu(int displayId) {
        return this.mVivoAppShareManager.adjustShowMethodMenu(displayId);
    }

    public void notifyAppSharePackageChanged(String packageName, int userId) {
        this.mAppSharePackageName = packageName;
        this.mAppShareUserId = userId;
    }

    public boolean shouldBlockImeSetInputMethod(int pid, int uid, String id) {
        if (AppShareConfig.SUPPROT_APPSHARE) {
            String str = this.TAG;
            VSlog.i(str, "shouldBlockImeSetInputMethod: pid = " + pid + ", uid = " + uid + ", id = " + id + ", mCurMethodId = " + this.mImms.mCurMethodId + ", mAppSharePackageName = " + this.mAppSharePackageName + ", mAppShareUserId = " + this.mAppShareUserId);
            if (TextUtils.isEmpty(this.mAppSharePackageName) || this.mAppShareUserId == -1) {
                return false;
            }
            if (this.mImms.mCurMethodId != null && this.mImms.mCurMethodId.equals(id)) {
                VSlog.i(this.TAG, "shouldBlockImeSetInputMethod: target ime is the same as current!");
                return false;
            }
            return this.mVivoAppShareManager.shouldBlockImeSetInputMethod(pid, uid, id);
        }
        return false;
    }
}