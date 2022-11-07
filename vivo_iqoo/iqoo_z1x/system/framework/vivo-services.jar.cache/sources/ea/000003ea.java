package com.android.server.policy;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import com.vivo.common.utils.VLog;

/* loaded from: classes.dex */
public class VivoKeyActionObserver {
    private static final String TAG = "VivoKeyActionObserver";
    private SparseArray<Integer> mClickCountArray = new SparseArray<>();
    public Context mContext;
    private MessageHandler mMessageHandler;
    private static final long DOUBLE_CLICK_TIMEOUT = ViewConfiguration.getDoubleTapTimeout();
    private static final long HARD_KEY_LONG_PRESS_DELAY = ViewConfiguration.getKeyRepeatTimeout() * 2;
    private static final long VIRTUAL_KEY_LONG_PRESS_DELAY = ViewConfiguration.getKeyRepeatTimeout();

    public VivoKeyActionObserver(Context context) {
        this.mMessageHandler = null;
        this.mContext = context;
        this.mMessageHandler = new MessageHandler();
    }

    public void startKeyDoubleClickDetection(IVivoKeyCallback listener, KeyEvent event) {
        int keyCode = event.getKeyCode();
        int clickCount = increaseKeyClickCount(keyCode);
        cancelResetKeyClickCountAction();
        if (clickCount == 1) {
            postResetKeyClickCountAction(listener, event);
        } else if (clickCount == 2) {
            resetKeyClickCount(keyCode);
            printf("onKeyDoubleClick will be invoked: KeyCode=" + keyCode);
            listener.onKeyDoubleClick(keyCode, event);
        } else {
            VLog.e(TAG, "Error: This should not happen, clickCount=" + clickCount);
            resetKeyClickCount(keyCode);
        }
    }

    public void stopKeyDoubleClickDetection(IVivoKeyCallback listener, KeyEvent event) {
        int keyCode = event.getKeyCode();
        resetKeyClickCount(keyCode);
        cancelResetKeyClickCountAction();
    }

    private void postResetKeyClickCountAction(IVivoKeyCallback listener, KeyEvent event) {
        int keyCode = event.getKeyCode();
        MessageParam param = new MessageParam();
        param.mKeyEvent = event;
        param.mListener = listener;
        Message msg = this.mMessageHandler.obtainMessage(1, keyCode, 0, param);
        this.mMessageHandler.sendMessageDelayed(msg, DOUBLE_CLICK_TIMEOUT);
    }

    private void cancelResetKeyClickCountAction() {
        this.mMessageHandler.removeMessages(1);
    }

    private int getKeyClickCount(int keyCode) {
        Integer count = this.mClickCountArray.get(keyCode);
        if (count == null) {
            count = 0;
            this.mClickCountArray.put(keyCode, count);
        }
        return count.intValue();
    }

    private int increaseKeyClickCount(int keyCode) {
        int count = getKeyClickCount(keyCode) + 1;
        this.mClickCountArray.put(keyCode, Integer.valueOf(count));
        return count;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetKeyClickCount(int keyCode) {
        this.mClickCountArray.put(keyCode, 0);
    }

    public void postKeyLongPressAction(IVivoKeyCallback listener, KeyEvent event) {
        int keyCode = event.getKeyCode();
        long delayMillis = HARD_KEY_LONG_PRESS_DELAY;
        if (keyCode == 3) {
            delayMillis = VIRTUAL_KEY_LONG_PRESS_DELAY;
        }
        MessageParam param = new MessageParam();
        param.mKeyEvent = event;
        param.mListener = listener;
        Message msg = this.mMessageHandler.obtainMessage(2, keyCode, 0, param);
        this.mMessageHandler.sendMessageDelayed(msg, delayMillis);
    }

    public void cancelKeyLongPressAction() {
        this.mMessageHandler.removeMessages(2);
    }

    public boolean checkKeyLongPressAction() {
        return this.mMessageHandler.hasMessages(2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void printf(String msg) {
        VivoWMPHook.printf(msg);
    }

    /* loaded from: classes.dex */
    public final class MessageParam {
        public KeyEvent mKeyEvent;
        public IVivoKeyCallback mListener;

        public MessageParam() {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MessageHandler extends Handler {
        public static final int MSG_LONG_PRESS = 2;
        public static final int MSG_RESET_CLICK_COUNT = 1;

        private MessageHandler() {
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 1) {
                VivoKeyActionObserver.this.printf("Double-click-counting timeout");
                VivoKeyActionObserver.this.resetKeyClickCount(msg.arg1);
            } else if (i == 2) {
                VivoKeyActionObserver vivoKeyActionObserver = VivoKeyActionObserver.this;
                vivoKeyActionObserver.printf("onKeyLongPress will be invoked: KeyCode=" + msg.arg1);
                MessageParam param = (MessageParam) msg.obj;
                KeyEvent event = param.mKeyEvent;
                KeyEvent repeatEvent = KeyEvent.changeTimeRepeat(event, SystemClock.uptimeMillis(), 1, event.getFlags() | 128);
                param.mListener.onKeyLongPress(msg.arg1, repeatEvent);
            }
        }
    }
}