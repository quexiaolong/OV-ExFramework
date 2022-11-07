package com.vivo.services.configurationManager;

import android.os.FileObserver;
import android.os.Handler;
import android.os.Message;
import com.vivo.common.utils.VLog;

/* loaded from: classes.dex */
public class ConfigFileObserver extends FileObserver {
    protected static final int MSG_CONFIG_FILE_CLOSE_WRITE = 1001;
    protected static final int MSG_CONFIG_FILE_DELETE = 1000;
    private static final String TAG = "ConfigurationManager";
    private Handler handler;
    private String mFilePath;
    private int type;

    public ConfigFileObserver(String path, int type, int flags, Handler handler) {
        super(path, flags);
        this.handler = handler;
        this.type = type;
        this.mFilePath = path;
    }

    @Override // android.os.FileObserver
    public void onEvent(int event, String path) {
        if (event == 8) {
            VLog.d(TAG, this.mFilePath + "write finished");
            Message m = Message.obtain(this.handler, 1001, this.mFilePath);
            m.arg1 = this.type;
            m.sendToTarget();
        } else if (event == 512 || event == 1024) {
            VLog.d(TAG, this.mFilePath + " delete event");
            Message msg = Message.obtain(this.handler, 1000, this.mFilePath);
            msg.arg1 = this.type;
            msg.sendToTarget();
        }
    }
}