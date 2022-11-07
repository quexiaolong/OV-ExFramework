package com.android.server.policy;

import android.content.Context;
import android.os.PowerManager;
import android.util.SparseArray;
import android.view.KeyEvent;
import com.android.internal.policy.KeyInterceptionInfo;
import com.vivo.common.utils.VLog;

/* loaded from: classes.dex */
public class VivoInterceptKeyRegister implements IVivoKeyBeforeQueueingListener, IVivoKeyBeforeDispatchingListener {
    private static final String TAG = "VivoInterceptKeyHandler";
    private static final long WAKE_LOCK_TIMEOUT = 5000;
    public Context mContext;
    private VivoKeyActionObserver mKeyActionObserver;
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mPartialWakeLock = null;
    private SparseArray<AVivoInterceptKeyCallback> mListeners = new SparseArray<>();

    public VivoInterceptKeyRegister(Context context) {
        this.mPowerManager = null;
        this.mKeyActionObserver = null;
        this.mContext = context;
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mKeyActionObserver = new VivoKeyActionObserver(this.mContext);
    }

    public void hookupListeners(VivoWMPHook vivoWMPHook) {
        int size = this.mListeners.size();
        for (int i = 0; i < size; i++) {
            int keyCode = this.mListeners.keyAt(i);
            vivoWMPHook.registerKeyBeforeQueueingListener(keyCode, this);
            vivoWMPHook.registerKeyBeforeDispatchingListener(keyCode, this);
        }
    }

    public AVivoInterceptKeyCallback getInterceptKeyListener(int keyCode) {
        return this.mListeners.get(keyCode);
    }

    public void registerInterceptKeyListener(int keyCode, AVivoInterceptKeyCallback listener) {
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

    @Override // com.android.server.policy.IVivoKeyBeforeQueueingListener
    public int onInterceptKeyBeforeQueueing(KeyEvent event, int policyFlags, boolean isScreenOn, boolean keyguardActive) {
        int result;
        int keyCode = event.getKeyCode();
        AVivoInterceptKeyCallback listener = this.mListeners.get(keyCode);
        if (listener == null) {
            printf("onInterceptKeyBeforeQueueing will drop keyCode=" + keyCode + ", because of listener is null.");
            return -100;
        }
        listener.mState = 0;
        listener.mKeyEvent = event;
        listener.mIsKeyguardActive = keyguardActive;
        listener.mIsScreenOn = isScreenOn;
        listener.mPolicyFlags = policyFlags;
        if (listener.onCheckForward(keyCode, event)) {
            printf("onInterceptKeyBeforeQueueing will drop keyCode=" + keyCode);
            return -100;
        }
        boolean down = event.getAction() == 0;
        boolean isDoubleClickEnabled = listener.onCheckDoubleClickEnabled(keyCode, event);
        if (down) {
            if (!isScreenOn && listener.onCheckNeedWakeLockWhenScreenOff(keyCode, event)) {
                acquireWakeLock();
            }
            printf("onKeyDown before queueing will be invoked: KeyCode=" + keyCode);
            result = listener.onKeyDown(keyCode, event);
            if (result != -100) {
                return result;
            }
            this.mKeyActionObserver.cancelKeyLongPressAction();
            this.mKeyActionObserver.postKeyLongPressAction(listener, new KeyEvent(event));
        } else {
            printf("onKeyUp before queueing will be invoked: KeyCode=" + keyCode);
            result = listener.onKeyUp(keyCode, event);
            if (result != -100) {
                return result;
            }
            if (this.mKeyActionObserver.checkKeyLongPressAction()) {
                this.mKeyActionObserver.cancelKeyLongPressAction();
                if (isDoubleClickEnabled) {
                    this.mKeyActionObserver.startKeyDoubleClickDetection(listener, new KeyEvent(event));
                } else {
                    this.mKeyActionObserver.stopKeyDoubleClickDetection(listener, new KeyEvent(event));
                }
            }
        }
        return result;
    }

    @Override // com.android.server.policy.IVivoKeyBeforeQueueingListener
    public void cancelPendingKeyAction(int keycode) {
        AVivoInterceptKeyCallback listener = this.mListeners.get(keycode);
        if (listener != null) {
            listener.cancelPendingKeyAction();
        }
    }

    @Override // com.android.server.policy.IVivoKeyBeforeDispatchingListener
    public int onInterceptKeyBeforeDispatching(KeyInterceptionInfo keyInterceptionInfo, KeyEvent event, int policyFlags, boolean keyguardOn) {
        int keyCode = event.getKeyCode();
        AVivoInterceptKeyCallback listener = this.mListeners.get(keyCode);
        if (listener == null) {
            printf("onInterceptKeyBeforeDispatching will drop keyCode=" + keyCode + ", because of listener is null.");
            return -100;
        }
        listener.mState = 1;
        listener.mKeyEvent = event;
        listener.mPolicyFlags = policyFlags;
        listener.mKeyInterceptionInfo = keyInterceptionInfo;
        if (listener.onCheckForward(keyCode, event)) {
            printf("onInterceptKeyBeforeDispatching will drop keyCode=" + keyCode);
            return -100;
        }
        boolean down = event.getAction() == 0;
        if (down) {
            printf("onKeyDown before dispatching will be invoked: KeyCode=" + keyCode);
            int result = listener.onKeyDown(keyCode, event);
            if (result != -100) {
                return result;
            }
        } else {
            printf("onKeyUp before dispatching will be invoked: KeyCode=" + keyCode);
            int result2 = listener.onKeyUp(keyCode, event);
            if (result2 != -100) {
                return result2;
            }
        }
        return -100;
    }

    private void initWakeLock() {
        if (this.mPartialWakeLock == null) {
            PowerManager.WakeLock newWakeLock = this.mPowerManager.newWakeLock(1, TAG);
            this.mPartialWakeLock = newWakeLock;
            newWakeLock.setReferenceCounted(false);
        }
    }

    private void acquireWakeLock() {
        printf("acquire mPartialWakeLock");
        initWakeLock();
        this.mPartialWakeLock.acquire(5000L);
    }

    private void releaseWakeLock() {
        if (this.mPartialWakeLock != null) {
            printf("release mPartialWakeLock");
            this.mPartialWakeLock.release();
        }
    }

    private void printf(String msg) {
        VivoWMPHook.printf(msg);
    }
}