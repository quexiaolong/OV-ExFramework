package com.android.server.policy.key;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import com.android.server.policy.key.VivoSmartwakeCharContainer;

/* loaded from: classes.dex */
public class VivoSmartwakeView extends FrameLayout {
    private VivoSmartwakeCharContainer mCharContainer;

    public VivoSmartwakeView(Context context) {
        this(context, null);
    }

    public VivoSmartwakeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VivoSmartwakeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mCharContainer = null;
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-1, -1);
        params.gravity = 3;
        params.leftMargin = 0;
        params.topMargin = 0;
        VivoSmartwakeCharContainer vivoSmartwakeCharContainer = new VivoSmartwakeCharContainer(context);
        this.mCharContainer = vivoSmartwakeCharContainer;
        addView(vivoSmartwakeCharContainer, params);
    }

    public void setmKeyCode(int keyCode) {
        this.mCharContainer.setmKeyCode(keyCode);
    }

    public int getmKeyCode() {
        return this.mCharContainer.getmKeyCode();
    }

    public void setAnimEndlistener(VivoSmartwakeCharContainer.SmartWakeCallback callback) {
        this.mCharContainer.setAnimEndlistener(callback);
    }

    public void startTrackAnimation(boolean isSecure) {
        this.mCharContainer.startTrackAnimation(isSecure);
    }

    public void updateDisappearTime(int keyCode, boolean isSecure) {
        this.mCharContainer.updateDisappearTime(keyCode, isSecure);
    }

    public void startAlphaAnimation() {
        this.mCharContainer.startAlphaAnimation();
    }
}