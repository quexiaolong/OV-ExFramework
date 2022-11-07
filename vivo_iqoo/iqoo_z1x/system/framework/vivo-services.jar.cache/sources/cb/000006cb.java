package com.vivo.services.proxy.transact;

import android.os.IBinder;
import android.view.IWindow;
import com.vivo.common.utils.VLog;
import java.lang.reflect.Field;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class IWindowProxy extends BpTransactProxy {
    private static int TRANSACTION_dispatchSystemUiVisibilityChanged;
    private static int TRANSACTION_insetsChanged;
    private static int TRANSACTION_moved;
    private static int TRANSACTION_resized;

    static {
        TRANSACTION_dispatchSystemUiVisibilityChanged = -1;
        TRANSACTION_resized = -1;
        TRANSACTION_moved = -1;
        TRANSACTION_insetsChanged = -1;
        try {
            Field f = IWindow.Stub.class.getDeclaredField("TRANSACTION_dispatchSystemUiVisibilityChanged");
            f.setAccessible(true);
            TRANSACTION_dispatchSystemUiVisibilityChanged = f.getInt(null);
        } catch (Exception e) {
            TransactProxyManager.CODE_READY = false;
            VLog.e(TransactProxyManager.TAG, "IWindowProxy init TRANSACTION_dispatchSystemUiVisibilityChanged fail:" + e.toString());
        }
        try {
            Field f2 = IWindow.Stub.class.getDeclaredField("TRANSACTION_resized");
            f2.setAccessible(true);
            TRANSACTION_resized = f2.getInt(null);
        } catch (Exception e2) {
            TransactProxyManager.CODE_READY = false;
            VLog.e(TransactProxyManager.TAG, "IWindowProxy init TRANSACTION_resized fail:" + e2.toString());
        }
        try {
            Field f3 = IWindow.Stub.class.getDeclaredField("TRANSACTION_moved");
            f3.setAccessible(true);
            TRANSACTION_moved = f3.getInt(null);
        } catch (Exception e3) {
            TransactProxyManager.CODE_READY = false;
            VLog.e(TransactProxyManager.TAG, "IWindowProxy init TRANSACTION_moved fail:" + e3.toString());
        }
        try {
            Field f4 = IWindow.Stub.class.getDeclaredField("TRANSACTION_insetsChanged");
            f4.setAccessible(true);
            TRANSACTION_insetsChanged = f4.getInt(null);
        } catch (Exception e4) {
            TransactProxyManager.CODE_READY = false;
            VLog.e(TransactProxyManager.TAG, "IWindowProxy init TRANSACTION_insetsChanged fail:" + e4.toString());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public IWindowProxy(String descriptor, IBinder bpBinder, String pkg, String process, int uid, int pid) {
        super(descriptor, bpBinder, pkg, process, uid, pid);
    }

    @Override // com.vivo.services.proxy.transact.BpTransactProxy
    protected boolean isSkip(int code) {
        return (TRANSACTION_dispatchSystemUiVisibilityChanged == code || TRANSACTION_resized == code || TRANSACTION_moved == code || TRANSACTION_insetsChanged == code) ? false : true;
    }
}