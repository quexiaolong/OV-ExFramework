package com.android.server.policy;

import android.content.Context;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.View;
import com.vivo.common.utils.VLog;

/* loaded from: classes.dex */
public class VivoFallbackKeyRegister implements IVivoKeyFallbackListener {
    private static final String TAG = "VivoFallbackKeyHandler";
    public Context mContext;
    private VivoKeyActionObserver mKeyActionObserver;
    private SparseArray<IVivoKeyCallback> mListeners = new SparseArray<>();

    public VivoFallbackKeyRegister(Context context) {
        this.mKeyActionObserver = null;
        this.mContext = context;
        this.mKeyActionObserver = new VivoKeyActionObserver(this.mContext);
    }

    public IVivoKeyCallback getInterceptKeyListener(int keyCode) {
        return this.mListeners.get(keyCode);
    }

    public void registerInterceptKeyListener(int keyCode, IVivoKeyCallback listener) {
        if (keyCode <= 0) {
            VLog.e(TAG, "Invalid keyCode:" + keyCode);
        } else if (this.mListeners.get(keyCode) != null) {
            VLog.e(TAG, "registerInterceptKeyListener: KeyCode=" + keyCode + " has already been registered, please contact the manager.");
        } else {
            this.mListeners.put(keyCode, listener);
        }
    }

    public void unregisterInterceptKeyListener(int keyCode) {
        this.mListeners.remove(keyCode);
    }

    @Override // com.android.server.policy.IVivoKeyFallbackListener
    public int onInterceptKeyFallback(KeyEvent event, View view) {
        int keyCode = event.getKeyCode();
        IVivoKeyCallback listener = this.mListeners.get(keyCode);
        if (listener == null) {
            printf("onInterceptKeyFallback will drop keyCode=" + keyCode + ", and then forward, because of listener is null.");
            return -100;
        } else if (listener.onCheckForward(keyCode, event)) {
            printf("onInterceptKeyFallback will drop keyCode=" + keyCode + ", and then forward, because of onCheckForward.");
            return -100;
        } else {
            printf("onInterceptKeyFallback will be invoked: keyCode=" + keyCode);
            KeyEvent.DispatcherState dispatcher = view.getKeyDispatcherState();
            boolean down = event.getAction() == 0;
            int repeatCount = event.getRepeatCount();
            if (down) {
                printf("onKeyDown falback will be invoked: KeyCode=" + keyCode);
                int result = listener.onKeyDown(keyCode, event);
                if (result != -100) {
                    return result;
                }
                if (repeatCount == 0) {
                    dispatcher.startTracking(event, this);
                    return -101;
                } else if (event.isLongPress() && dispatcher.isTracking(event)) {
                    printf("onKeyLongPress will be invoked: KeyCode=" + keyCode);
                    listener.onKeyLongPress(keyCode, event);
                    return -101;
                } else {
                    return -101;
                }
            }
            printf("onKeyUp falback will be invoked: KeyCode=" + keyCode);
            int result2 = listener.onKeyUp(keyCode, event);
            if (result2 != -100) {
                return result2;
            }
            dispatcher.handleUpEvent(event);
            if (repeatCount == 0) {
                this.mKeyActionObserver.startKeyDoubleClickDetection(listener, event);
                return -101;
            }
            this.mKeyActionObserver.stopKeyDoubleClickDetection(listener, event);
            return -101;
        }
    }

    private void printf(String msg) {
        VivoWMPHook.printf(msg);
    }
}