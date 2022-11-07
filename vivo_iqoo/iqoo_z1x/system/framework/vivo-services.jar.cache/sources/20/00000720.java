package com.vivo.services.rms.display;

import android.os.SystemClock;
import com.vivo.services.rms.ProcessInfo;
import com.vivo.services.rms.display.DisplayConfigsManager;
import com.vivo.services.rms.display.scene.BaseScene;
import com.vivo.services.rms.display.scene.PowerScene;
import com.vivo.services.rms.sdk.Consts;

/* loaded from: classes.dex */
public class RefreshRateRequest implements ProcessInfo.StateChangeListener {
    public boolean active;
    public ProcessInfo caller;
    public ProcessInfo client;
    public int configBits;
    public long createTime;
    public boolean dfps;
    public int duration;
    public int extra;
    public int fps;
    public String handle;
    public boolean listenerRegistered = false;
    public DisplayConfigsManager.DisplayMode mode;
    public BaseScene owner;
    public int priority;
    public String reason;
    public int reqConfigBits;
    public int reqDFps;
    public int reqFps;
    public int reqStates;
    public int resolution;
    public boolean usePrivateFps;

    public boolean isTimeout() {
        return isTimeout(SystemClock.uptimeMillis());
    }

    public boolean isTimeout(long now) {
        int i = this.duration;
        return i > 0 && now - this.createTime > ((long) i);
    }

    public boolean update(int flags, int value) {
        return this.owner.updateRequest(this, flags, value);
    }

    public boolean isThermal() {
        return this.priority == PowerScene.thermalPriority();
    }

    public void registerStateChangeListener() {
        ProcessInfo processInfo;
        if (!this.listenerRegistered && (processInfo = this.client) != null) {
            processInfo.addStateChangedListener(this);
            this.listenerRegistered = true;
        }
    }

    public void unregisterStateChangeListener() {
        if (this.listenerRegistered) {
            this.client.removeStateChangedListener(this);
            this.listenerRegistered = false;
        }
    }

    @Override // com.vivo.services.rms.ProcessInfo.StateChangeListener
    public void onStateChanged(int mask, boolean hasState, ProcessInfo processInfo) {
        if (this.client != null && (this.reqStates & mask) != 0) {
            RefreshRateAdjuster.getInstance().requestSetActiveMode();
        }
    }

    public boolean hasFocus() {
        ProcessInfo processInfo = this.client;
        return processInfo != null && processInfo.hasFocus();
    }

    public String toString() {
        StringBuilder builder = new StringBuilder(128);
        builder.append("{");
        builder.append("scene=");
        builder.append(this.owner.getName());
        builder.append(",");
        builder.append("handle=");
        builder.append(this.handle);
        builder.append(",");
        builder.append("reason=");
        builder.append(this.reason);
        builder.append(",");
        builder.append("reqFps=");
        builder.append(this.reqFps);
        builder.append(",");
        builder.append("reqDFps=");
        builder.append(this.reqDFps);
        builder.append(",");
        builder.append("fps=");
        builder.append(this.fps);
        builder.append(",");
        builder.append("duration=");
        builder.append(this.duration);
        builder.append(",");
        builder.append("priority=");
        builder.append(this.priority);
        builder.append(",");
        builder.append("caller=");
        builder.append(this.caller.mPid);
        builder.append(",");
        builder.append("dfps=");
        builder.append(this.dfps);
        builder.append(",");
        builder.append("usePrivateFps=");
        builder.append(this.usePrivateFps);
        builder.append(",");
        if (this.reqStates != 0) {
            builder.append("reqStates=");
            builder.append(Consts.ProcessStates.getName(this.reqStates));
            builder.append(",");
        }
        if (this.client != null) {
            builder.append("client=");
            builder.append(this.client.mPid);
            builder.append(",");
        }
        if (this.extra != 0) {
            builder.append("extra=");
            builder.append(String.format("0x%x", Integer.valueOf(this.extra)));
            builder.append(",");
        }
        builder.append("configsBits=");
        builder.append(String.format("0x%x/0x%x", Integer.valueOf(this.reqConfigBits), Integer.valueOf(this.configBits)));
        builder.append(",");
        builder.append("resolution=");
        builder.append(String.format("%dx%d", Integer.valueOf(DisplayConfigsManager.DisplayMode.toWidth(this.resolution)), Integer.valueOf(DisplayConfigsManager.DisplayMode.toHeight(this.resolution))));
        builder.append(",");
        if (this.mode != null) {
            builder.append("mode=");
            builder.append(this.mode.id);
            builder.append(",");
        }
        builder.append("active=");
        builder.append(this.active);
        builder.append(",");
        builder.append("valid=");
        builder.append(this.owner.isValid(this));
        builder.append(",");
        builder.append("elapsed=");
        builder.append(SystemClock.uptimeMillis() - this.createTime);
        builder.append("ms");
        builder.append("}");
        return builder.toString();
    }
}