package com.android.server.tv;

import android.os.IBinder;
import dalvik.system.CloseGuard;
import java.io.IOException;

/* loaded from: classes2.dex */
public final class UinputBridge {
    private final CloseGuard mCloseGuard;
    private long mPtr;
    private IBinder mToken;

    private static native void nativeClear(long j);

    private static native void nativeClose(long j);

    private static native long nativeGamepadOpen(String str, String str2);

    private static native long nativeOpen(String str, String str2, int i, int i2, int i3);

    private static native void nativeSendGamepadAxisValue(long j, int i, float f);

    private static native void nativeSendGamepadKey(long j, int i, boolean z);

    private static native void nativeSendKey(long j, int i, boolean z);

    private static native void nativeSendPointerDown(long j, int i, int i2, int i3);

    private static native void nativeSendPointerSync(long j);

    private static native void nativeSendPointerUp(long j, int i);

    public UinputBridge(IBinder token, String name, int width, int height, int maxPointers) throws IOException {
        this.mCloseGuard = CloseGuard.get();
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("Touchpad must be at least 1x1.");
        }
        if (maxPointers < 1 || maxPointers > 32) {
            throw new IllegalArgumentException("Touchpad must support between 1 and 32 pointers.");
        }
        if (token == null) {
            throw new IllegalArgumentException("Token cannot be null");
        }
        long nativeOpen = nativeOpen(name, token.toString(), width, height, maxPointers);
        this.mPtr = nativeOpen;
        if (nativeOpen == 0) {
            throw new IOException("Could not open uinput device " + name);
        }
        this.mToken = token;
        this.mCloseGuard.open("close");
    }

    private UinputBridge(IBinder token, long ptr) {
        CloseGuard closeGuard = CloseGuard.get();
        this.mCloseGuard = closeGuard;
        this.mPtr = ptr;
        this.mToken = token;
        closeGuard.open("close");
    }

    public static UinputBridge openGamepad(IBinder token, String name) throws IOException {
        if (token == null) {
            throw new IllegalArgumentException("Token cannot be null");
        }
        long ptr = nativeGamepadOpen(name, token.toString());
        if (ptr == 0) {
            throw new IOException("Could not open uinput device " + name);
        }
        return new UinputBridge(token, ptr);
    }

    protected void finalize() throws Throwable {
        try {
            this.mCloseGuard.warnIfOpen();
            close(this.mToken);
        } finally {
            this.mToken = null;
            super.finalize();
        }
    }

    public void close(IBinder token) {
        if (isTokenValid(token) && this.mPtr != 0) {
            clear(token);
            nativeClose(this.mPtr);
            this.mPtr = 0L;
            this.mCloseGuard.close();
        }
    }

    public IBinder getToken() {
        return this.mToken;
    }

    protected boolean isTokenValid(IBinder token) {
        return this.mToken.equals(token);
    }

    public void sendKeyDown(IBinder token, int keyCode) {
        if (isTokenValid(token)) {
            nativeSendKey(this.mPtr, keyCode, true);
        }
    }

    public void sendKeyUp(IBinder token, int keyCode) {
        if (isTokenValid(token)) {
            nativeSendKey(this.mPtr, keyCode, false);
        }
    }

    public void sendPointerDown(IBinder token, int pointerId, int x, int y) {
        if (isTokenValid(token)) {
            nativeSendPointerDown(this.mPtr, pointerId, x, y);
        }
    }

    public void sendPointerUp(IBinder token, int pointerId) {
        if (isTokenValid(token)) {
            nativeSendPointerUp(this.mPtr, pointerId);
        }
    }

    public void sendPointerSync(IBinder token) {
        if (isTokenValid(token)) {
            nativeSendPointerSync(this.mPtr);
        }
    }

    public void sendGamepadKey(IBinder token, int keyCode, boolean down) {
        if (isTokenValid(token)) {
            nativeSendGamepadKey(this.mPtr, keyCode, down);
        }
    }

    public void sendGamepadAxisValue(IBinder token, int axis, float value) {
        if (isTokenValid(token)) {
            nativeSendGamepadAxisValue(this.mPtr, axis, value);
        }
    }

    public void clear(IBinder token) {
        if (isTokenValid(token)) {
            nativeClear(this.mPtr);
        }
    }
}