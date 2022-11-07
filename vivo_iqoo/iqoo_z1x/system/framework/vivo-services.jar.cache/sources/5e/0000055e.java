package com.android.server.wm;

import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.server.policy.InputExceptionReport;
import com.android.server.wm.ImmersiveModeConfirmation;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoImmersiveModeConfirmationImpl implements IVivoImmersiveModeConfirmation {
    private static final boolean DEBUG = false;
    static final String TAG = "VivoImmersiveModeConfirmationImpl";
    private static final boolean sInCtsTest = "yes".equals(SystemProperties.get("persist.vivo.cts.adb.enable", "no"));
    public static String sConfirmedState = null;
    public static String mShowState = null;

    public void dummy() {
        VSlog.i(TAG, "dummy, this=" + this);
    }

    public void loadSetting(int currentUserId, Context context) {
        sConfirmedState = null;
        ImmersiveModeConfirmation.sConfirmed = false;
        String value = null;
        try {
            value = Settings.Secure.getStringForUser(context.getContentResolver(), "immersive_mode_confirmations", -2);
            sConfirmedState = value;
            ImmersiveModeConfirmation.sConfirmed = InputExceptionReport.LEVEL_MEDIUM.equals(value);
        } catch (Throwable t) {
            VSlog.w(TAG, "Error loading confirmations, value=" + value, t);
        }
    }

    public void saveSetting(Context context) {
        String value;
        String lastValue = Settings.Secure.getStringForUser(context.getContentResolver(), "immersive_mode_confirmations", -2);
        WindowManager windowManager = (WindowManager) context.getSystemService("window");
        String rotation = windowManager.getDefaultDisplay().getRotation() == 0 ? "1" : "2";
        String str = mShowState;
        if (str != null && !str.equals(rotation)) {
            value = InputExceptionReport.LEVEL_MEDIUM;
            ImmersiveModeConfirmation.sConfirmed = true;
        } else if (lastValue == null || mShowState == null) {
            value = rotation;
        } else if ("1".equals(lastValue) && "2".equals(mShowState)) {
            value = InputExceptionReport.LEVEL_MEDIUM;
            ImmersiveModeConfirmation.sConfirmed = true;
        } else if ("2".equals(lastValue) && "1".equals(mShowState)) {
            value = InputExceptionReport.LEVEL_MEDIUM;
            ImmersiveModeConfirmation.sConfirmed = true;
        } else {
            value = mShowState;
        }
        sConfirmedState = value;
        Settings.Secure.putStringForUser(context.getContentResolver(), "immersive_mode_confirmations", value, -2);
    }

    public boolean shouldSkipShow(WindowManager windowManager, boolean skip, boolean disabled) {
        if (!skip && windowManager != null) {
            int rotation = windowManager.getDefaultDisplay().getRotation();
            if (!disabled) {
                mShowState = rotation == 0 ? "1" : "2";
            }
            skip = rotation == 0 ? "1".equals(sConfirmedState) : "2".equals(sConfirmedState);
        }
        if (sInCtsTest) {
            return true;
        }
        return skip;
    }

    public FrameLayout.LayoutParams getBubbleLayoutParams() {
        return new FrameLayout.LayoutParams(-1, -1, 49);
    }

    public void updateLayout(WindowManager windowManager, Context context, ImmersiveModeConfirmation.ClingWindowView clingWindowView) {
        clingWindowView.removeView(clingWindowView.mClingLayout);
        initClingLayout(windowManager, context, clingWindowView);
        clingWindowView.addView(clingWindowView.mClingLayout, getBubbleLayoutParams());
    }

    public void initClingLayout(WindowManager windowManager, Context context, final ImmersiveModeConfirmation.ClingWindowView clingWindowView) {
        int rotation = windowManager.getDefaultDisplay().getRotation();
        boolean vivoChangeNavPosition = Settings.Secure.getIntForUser(context.getContentResolver(), "nav_bar_landscape_position", 1, -2) == 1;
        if (vivoChangeNavPosition || rotation != 3) {
            clingWindowView.mClingLayout = (ViewGroup) View.inflate(clingWindowView.getContext(), 50528296, null);
        } else {
            clingWindowView.mClingLayout = (ViewGroup) View.inflate(clingWindowView.getContext(), 50528297, null);
        }
        if (!vivoChangeNavPosition) {
            TextView hint = (TextView) clingWindowView.mClingLayout.findViewById(51183743);
            hint.setText(51249722);
        }
        Button ok = (Button) clingWindowView.mClingLayout.findViewById(51183795);
        ok.setOnClickListener(new View.OnClickListener() { // from class: com.android.server.wm.VivoImmersiveModeConfirmationImpl.1
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                clingWindowView.mConfirm.run();
            }
        });
        clingWindowView.setNightMode(0);
    }

    public void adjustClingWindowLayoutParams(WindowManager.LayoutParams lp) {
        lp.layoutInDisplayCutoutMode = 3;
    }

    public void resetShowState() {
        mShowState = null;
    }
}