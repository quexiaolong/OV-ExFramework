package com.android.server.display;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.Trace;
import android.view.View;
import android.view.WindowManager;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public final class VivoDisplayOverlayController {
    private static boolean DEBUG = SystemProperties.get("persist.sys.log.ctrl", "no").equals("yes");
    private static final String KEY_VIVO_LOG_CTRL = "persist.sys.log.ctrl";
    private static final int MSG_DISABLE_BLACK_OVERLAY = 1001;
    private static final int MSG_ENABLE_BLACK_OVERLAY = 1000;
    private static final String OVERLAY_NAME_PRIMARY = "VivoDisplayOverlay";
    private static final String OVERLAY_NAME_SECONDARY = "SecondaryDisplayOverlay";
    private static final String TAG = "VivoDisplayOverlayController";
    private Context mContext;
    IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() { // from class: com.android.server.display.VivoDisplayOverlayController.1
        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            VSlog.w(VivoDisplayOverlayController.TAG, "binderDied");
            VivoDisplayOverlayController.this.mSurfaceFlinger = null;
        }
    };
    private int mDisplayId;
    private String mDisplayStr;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private boolean mOverlayAdded;
    private IBinder mSurfaceFlinger;
    private View mVivoDisplayOverlay;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowParams;

    public VivoDisplayOverlayController(Context context, int displayId) {
        this.mContext = context;
        this.mDisplayId = displayId;
        this.mDisplayStr = displayId == 0 ? "primary-display" : "secondary-display";
        HandlerThread handlerThread = new HandlerThread(TAG);
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        this.mHandler = new DisplayHandler(this.mHandlerThread.getLooper());
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        this.mWindowParams = layoutParams;
        layoutParams.width = -1;
        this.mWindowParams.height = -1;
        this.mWindowParams.type = 2015;
        this.mWindowParams.flags = 16779032;
        this.mWindowParams.setFitInsetsTypes(0);
        this.mWindowParams.layoutInDisplayCutoutMode = 3;
        if (this.mDisplayId == 0) {
            this.mWindowParams.privateFlags = -2147483632;
        } else {
            this.mWindowParams.privateFlags = -2147483632;
        }
        this.mWindowParams.format = -3;
        this.mWindowParams.rotationAnimation = 2;
        this.mWindowParams.screenOrientation = 5;
        this.mWindowParams.setTitle(this.mDisplayId == 0 ? OVERLAY_NAME_PRIMARY : OVERLAY_NAME_SECONDARY);
    }

    public void enableDisableBlackOverlay(boolean enable) {
        int what = enable ? 1000 : 1001;
        sendMessage(what, 0, 0, null, 0L);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void enableDisableBlackOverlayInternal(boolean enable) {
        VSlog.i(TAG, "enableDisableBlackOverlayInternal: " + this.mDisplayStr + " enable: " + enable + " mOverlayAdded: " + this.mOverlayAdded);
        if (enable && !this.mOverlayAdded) {
            this.mOverlayAdded = true;
            if (this.mVivoDisplayOverlay == null) {
                View view = new View(this.mContext);
                this.mVivoDisplayOverlay = view;
                view.setBackgroundResource(17170444);
            }
            View view2 = this.mVivoDisplayOverlay;
            if (view2 != null) {
                this.mWindowManager.addView(view2, this.mWindowParams);
            } else {
                VSlog.e(TAG, "enable display overlay denied/invalid vivo display overlay");
            }
        } else if (!enable && this.mOverlayAdded) {
            this.mOverlayAdded = false;
            if (this.mVivoDisplayOverlay != null) {
                if (this.mDisplayId == 0) {
                    setBlackOverlayInvisibleViaSurface();
                    VSlog.d(TAG, "disable display overlay via");
                }
                StringBuilder sb = new StringBuilder();
                sb.append("removeView ");
                sb.append(this.mDisplayId == 0 ? OVERLAY_NAME_PRIMARY : OVERLAY_NAME_SECONDARY);
                Trace.traceBegin(2L, sb.toString());
                this.mWindowManager.removeViewImmediate(this.mVivoDisplayOverlay);
                Trace.traceEnd(2L);
                VSlog.d(TAG, "disable display overlay finished");
                return;
            }
            VSlog.e(TAG, "disable display overlay denied/invalid vivo display overlay");
        }
    }

    private void sendMessage(int what, int arg1, int arg2, Object obj, long delayMillis) {
        Handler handler = this.mHandler;
        if (handler != null) {
            Message msg = handler.obtainMessage(what, arg1, arg2, obj);
            this.mHandler.sendMessageDelayed(msg, delayMillis);
            return;
        }
        VSlog.e(TAG, "sendMessage failed");
    }

    private void removeMessage(int what) {
        Handler handler = this.mHandler;
        if (handler != null && handler.hasMessages(what)) {
            this.mHandler.removeMessages(what);
        } else {
            VSlog.e(TAG, "removeMessage failed");
        }
    }

    private void setBlackOverlayInvisibleViaSurface() {
        try {
            sendTransaction(30003, 1, false, 0.0f);
        } catch (Exception e) {
            VSlog.e(TAG, "sendTransaction failed ! Exception is : " + e);
        }
    }

    private boolean sendTransaction(int transaction, int value, boolean withFloat, float floatValue) {
        boolean success;
        IBinder surfaceFlinger = checkService();
        if (surfaceFlinger == null) {
            VSlog.w(TAG, "operation denied/invalid instance");
            return false;
        }
        VSlog.d(TAG, String.format("update %d, value:%d, withFloat:%b, floatValue:%f", Integer.valueOf(transaction), Integer.valueOf(value), Boolean.valueOf(withFloat), Float.valueOf(floatValue)));
        Trace.traceBegin(2L, "sendTransaction-blackoverlay");
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            try {
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                data.writeInt(value);
                if (withFloat) {
                    data.writeFloat(floatValue);
                }
                success = surfaceFlinger.transact(transaction, data, reply, 0);
            } catch (RemoteException e) {
                VSlog.e(TAG, "operation exception");
                success = false;
            }
            data.recycle();
            reply.recycle();
            Trace.traceEnd(2L);
            return success;
        } catch (Throwable th) {
            data.recycle();
            reply.recycle();
            throw th;
        }
    }

    private IBinder checkService() {
        IBinder iBinder = this.mSurfaceFlinger;
        if (iBinder != null) {
            return iBinder;
        }
        IBinder surfaceFlinger = ServiceManager.checkService("SurfaceFlinger");
        if (surfaceFlinger == null) {
            VSlog.w(TAG, "preparation denied/invalid instance");
            return null;
        }
        try {
            surfaceFlinger.linkToDeath(this.mDeathRecipient, 0);
            this.mSurfaceFlinger = surfaceFlinger;
        } catch (RemoteException e) {
            VSlog.e(TAG, "preparation exception");
        }
        return this.mSurfaceFlinger;
    }

    /* loaded from: classes.dex */
    private final class DisplayHandler extends Handler {
        public DisplayHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 1000) {
                VivoDisplayOverlayController.this.enableDisableBlackOverlayInternal(true);
            } else if (i == 1001) {
                VivoDisplayOverlayController.this.enableDisableBlackOverlayInternal(false);
            }
        }
    }
}