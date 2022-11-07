package com.android.server.inputmethod;

import android.content.Context;
import com.android.server.inputmethod.InputMethodManagerService;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoInputMethodManagerServiceLifecycleImpl implements IVivoInputMethodManagerServiceLifecycle {
    static InputMethodManagerService mService;
    private Context mContext;
    private InputMethodManagerService.Lifecycle mImmsLifecycle;

    public VivoInputMethodManagerServiceLifecycleImpl(Context context, InputMethodManagerService.Lifecycle immsLifecycle) {
        this.mImmsLifecycle = immsLifecycle;
        this.mContext = context;
    }

    public InputMethodManagerService createInputMethodManagerService() {
        mService = new InputMethodManagerService(this.mContext);
        VSlog.i("InputMethodManagerService", "new normal service=" + mService);
        return mService;
    }

    public InputMethodManagerService getService() {
        return mService;
    }

    public void dummy() {
    }
}