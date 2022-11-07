package com.android.server.accessibility;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.FtBuild;
import android.os.Handler;
import android.os.ServiceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.DisplayCutout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.accessibility.IAccessibilityManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.vivo.common.utils.VLog;
import java.util.HashSet;
import java.util.Set;

/* loaded from: classes.dex */
public class AccessibilityWaterMark {
    private static final String TAG = "AccessibilityWaterMark";
    private static final String TALKBACK_DESCRIPTOR_TEXT = "TalkBack";
    private static final String TALKBACK_SERVICE = "com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService";
    private Context mContext;
    private Handler mHandler;
    private WindowManager mWindowManager;
    private View mWaterMarkView = null;
    private boolean mIsKeyguardShowing = true;
    private String mEnabledAccessibilityService = null;
    private String mShortcutTargets = null;
    private boolean mEnabledonLockScreen = false;
    private Runnable mUpdateWaterMarkRunnable = new Runnable() { // from class: com.android.server.accessibility.AccessibilityWaterMark.2
        @Override // java.lang.Runnable
        public void run() {
            boolean isTalkbackOn = AccessibilityWaterMark.this.isTalkbackOn();
            AccessibilityWaterMark accessibilityWaterMark = AccessibilityWaterMark.this;
            boolean isShortcutTalkbackOn = accessibilityWaterMark.isShortcutTalkbackOn(accessibilityWaterMark.mIsKeyguardShowing);
            boolean isTalkbackInCrashServices = AccessibilityWaterMark.this.isTalkbackInCrashServices();
            VLog.d(AccessibilityWaterMark.TAG, "isTalkbackOn:" + isTalkbackOn + " isShortcutTalkbackOn:" + isShortcutTalkbackOn + " mIsKeyguardShowing:" + AccessibilityWaterMark.this.mIsKeyguardShowing + " isTalkbackInCrashServices:" + isTalkbackInCrashServices);
            if (!isTalkbackOn || !isShortcutTalkbackOn || isTalkbackInCrashServices) {
                AccessibilityWaterMark.this.removeWaterMark();
            } else {
                AccessibilityWaterMark.this.showWaterMark();
            }
        }
    };

    public AccessibilityWaterMark(Context context, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
        this.mWindowManager = (WindowManager) context.getSystemService(WindowManager.class);
    }

    public void onUserSwitched() {
        VLog.d(TAG, "onUserSwitched");
        onSettingsChanged();
    }

    public void onKeyGuardChange(boolean isShowing) {
        VLog.d(TAG, "onKeyGuardChange isShowing:" + isShowing);
        this.mIsKeyguardShowing = isShowing;
        updateWaterMark();
    }

    public void onSystemReady() {
        ContentObserver co = new ContentObserver(this.mHandler) { // from class: com.android.server.accessibility.AccessibilityWaterMark.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                AccessibilityWaterMark.this.onSettingsChanged();
            }
        };
        ContentResolver contentResolver = this.mContext.getContentResolver();
        contentResolver.registerContentObserver(Settings.Secure.getUriFor("accessibility_shortcut_target_service"), false, co, -1);
        contentResolver.registerContentObserver(Settings.Secure.getUriFor("accessibility_shortcut_on_lock_screen"), false, co, -1);
        contentResolver.registerContentObserver(Settings.Secure.getUriFor("accessibility_shortcut_dialog_shown"), false, co, -1);
        contentResolver.registerContentObserver(Settings.Secure.getUriFor("enabled_accessibility_services"), false, co, -1);
        onSettingsChanged();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onSettingsChanged() {
        ContentResolver cr = this.mContext.getContentResolver();
        String stringForUser = Settings.Secure.getStringForUser(cr, "accessibility_shortcut_target_service", -2);
        this.mShortcutTargets = stringForUser;
        if (stringForUser == null) {
            this.mShortcutTargets = this.mContext.getString(17039875);
        }
        int dialogAlreadyShown = Settings.Secure.getIntForUser(cr, "accessibility_shortcut_dialog_shown", 0, -2);
        this.mEnabledonLockScreen = Settings.Secure.getIntForUser(cr, "accessibility_shortcut_on_lock_screen", dialogAlreadyShown, -2) == 1;
        this.mEnabledAccessibilityService = Settings.Secure.getStringForUser(cr, "enabled_accessibility_services", -2);
        updateWaterMark();
    }

    private void updateWaterMark() {
        if (this.mHandler.hasCallbacks(this.mUpdateWaterMarkRunnable)) {
            this.mHandler.removeCallbacks(this.mUpdateWaterMarkRunnable);
        }
        this.mHandler.post(this.mUpdateWaterMarkRunnable);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isShortcutTalkbackOn(boolean phoneLocked) {
        if (TextUtils.isEmpty(this.mShortcutTargets)) {
            return false;
        }
        if (!phoneLocked || this.mEnabledonLockScreen) {
            Set<ComponentName> cnSets = parseComponentFromString(this.mShortcutTargets);
            ComponentName talkbackCN = ComponentName.unflattenFromString(TALKBACK_SERVICE);
            return cnSets.contains(talkbackCN);
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isTalkbackOn() {
        Set<ComponentName> cnSets = parseComponentFromString(this.mEnabledAccessibilityService);
        ComponentName talkbackCN = ComponentName.unflattenFromString(TALKBACK_SERVICE);
        if (cnSets.contains(talkbackCN)) {
            return true;
        }
        return false;
    }

    private Set<ComponentName> parseComponentFromString(String services) {
        Set<ComponentName> sets = new HashSet<>();
        if (TextUtils.isEmpty(services)) {
            return sets;
        }
        TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
        colonSplitter.setString(services);
        while (colonSplitter.hasNext()) {
            ComponentName cn = ComponentName.unflattenFromString(colonSplitter.next());
            if (cn != null) {
                sets.add(cn);
            }
        }
        return sets;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showWaterMark() {
        if (FtBuild.isOverSeas() || this.mWaterMarkView != null) {
            return;
        }
        WaterMarkView waterMarkView = new WaterMarkView(this, this.mContext);
        this.mWaterMarkView = waterMarkView;
        TextView titleView = (TextView) waterMarkView.findViewById(51183909);
        TextView textView = (TextView) this.mWaterMarkView.findViewById(51183908);
        String showTitle = this.mContext.getString(51249172, TALKBACK_DESCRIPTOR_TEXT);
        String showText = this.mContext.getString(51249171);
        titleView.setText(showTitle);
        textView.setText(showText);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.type = 2015;
        lp.width = -2;
        lp.height = -2;
        lp.flags = 24;
        lp.setFitInsetsTypes(0);
        lp.layoutInDisplayCutoutMode = 1;
        lp.format = -3;
        lp.setTitle(TAG);
        lp.inputFeatures |= 2;
        lp.privateFlags |= 16;
        lp.gravity = 49;
        this.mWindowManager.addView(this.mWaterMarkView, lp);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeWaterMark() {
        if (this.mWaterMarkView == null) {
            return;
        }
        VLog.d(TAG, "removeWaterMark");
        this.mWindowManager.removeView(this.mWaterMarkView);
        this.mWaterMarkView = null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isTalkbackInCrashServices() {
        boolean inCrashServices = false;
        try {
            AccessibilityManagerService a11ms = IAccessibilityManager.Stub.asInterface(ServiceManager.getServiceOrThrow("accessibility"));
            inCrashServices = a11ms.isTalkbackInCrashServices(ComponentName.unflattenFromString(TALKBACK_SERVICE));
        } catch (Exception e) {
            VLog.e(TAG, "isTalkbackInCrashServices", e);
        }
        VLog.d(TAG, "is talkback in crash services:" + inCrashServices);
        return inCrashServices;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class WaterMarkView extends FrameLayout {
        public WaterMarkView(AccessibilityWaterMark accessibilityWaterMark, Context context) {
            this(accessibilityWaterMark, context, null);
        }

        public WaterMarkView(AccessibilityWaterMark accessibilityWaterMark, Context context, AttributeSet attrs) {
            this(context, attrs, 0);
        }

        public WaterMarkView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
            LayoutInflater.from(this.mContext).inflate(50528262, this);
        }

        @Override // android.view.View
        public WindowInsets onApplyWindowInsets(WindowInsets insets) {
            DisplayCutout displayCutout = insets.getDisplayCutout();
            if (displayCutout != null) {
                setPadding(0, displayCutout.getSafeInsetTop(), 0, 0);
            } else {
                setPadding(0, 0, 0, 0);
            }
            return super.onApplyWindowInsets(insets);
        }
    }
}