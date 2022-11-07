package com.android.server.display.color.displayenhance;

import android.content.Context;
import android.os.Handler;
import android.provider.Settings;
import android.widget.Toast;
import com.android.server.FgThread;
import com.vivo.face.common.data.Constants;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import vivo.util.VSlog;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class DisplayEnhanceToast {
    private static final String APPS_TOAST_STATUS = "apps_toast_status";
    private static final String TAG = "DisplayEnhanceToast";
    private static DisplayEnhanceToast mDisplayEnhanceToast = null;
    private Context mContext;
    private Map<String, Integer> mToastStatus = new HashMap();
    private final Object mToastStatusLock = new Object();
    private Handler mHandler = new Handler(FgThread.get().getLooper());

    private DisplayEnhanceToast(Context context) {
        this.mContext = context;
    }

    public static DisplayEnhanceToast getInstance(Context context) {
        if (mDisplayEnhanceToast == null) {
            synchronized (DisplayEnhanceToast.class) {
                if (mDisplayEnhanceToast == null) {
                    mDisplayEnhanceToast = new DisplayEnhanceToast(context);
                }
            }
        }
        return mDisplayEnhanceToast;
    }

    public void showHighTemperatureToast(int type) {
        String toastRes = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        if (type == 1) {
            toastRes = this.mContext.getString(51249314);
        } else if (type == 2) {
            toastRes = this.mContext.getString(51249315);
        }
        VSlog.d(TAG, "showHighTemperatureToast");
        toastTips(toastRes);
    }

    public void showActivatedToast(int type) {
        String toastRes = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        if (type == 1) {
            toastRes = this.mContext.getString(51249337);
        } else if (type == 2) {
            toastRes = this.mContext.getString(51249685);
        }
        VSlog.d(TAG, "showActivatedToast");
        toastTips(toastRes);
    }

    public void showCommonActivatedToast() {
        String toastRes = this.mContext.getString(51249257);
        VSlog.d(TAG, "showCommonActivatedToast");
        toastTips(toastRes);
    }

    public void showResumedToast(int type) {
        String toastRes = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        if (type == 1) {
            toastRes = this.mContext.getString(51249338);
        } else if (type == 2) {
            toastRes = this.mContext.getString(51249686);
        }
        VSlog.d(TAG, "showResumedToast");
        toastTips(toastRes);
    }

    private void toastTips(final String toast) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.display.color.displayenhance.DisplayEnhanceToast.1
            @Override // java.lang.Runnable
            public void run() {
                Toast.makeText(DisplayEnhanceToast.this.mContext, toast, 0).show();
            }
        });
    }

    private void putAppToastStatus() {
        try {
            synchronized (this.mToastStatusLock) {
                JSONObject object = new JSONObject(this.mToastStatus);
                VSlog.d(TAG, "putAppMemcToastStatus:  json = " + object.toString());
                Settings.System.putStringForUser(this.mContext.getContentResolver(), APPS_TOAST_STATUS, object.toString(), -2);
            }
        } catch (Exception e) {
            VSlog.e(TAG, "putAppMemcToastStatus: json exception ");
        }
    }

    private String getAppToastStatus() {
        return Settings.System.getStringForUser(this.mContext.getContentResolver(), APPS_TOAST_STATUS, -2);
    }

    public void initToastStatus(HashMap<String, HashMap<String, String>> packageMap) {
        synchronized (this.mToastStatusLock) {
            for (String key : packageMap.keySet()) {
                this.mToastStatus.put(key, 0);
            }
        }
        String toastStatus = getAppToastStatus();
        if (toastStatus != null) {
            try {
                JSONObject object = new JSONObject(toastStatus);
                synchronized (this.mToastStatusLock) {
                    for (String name : this.mToastStatus.keySet()) {
                        if (object.has(name)) {
                            int oldState = ((Integer) object.get(name)).intValue();
                            if (this.mToastStatus.get(name) != null && this.mToastStatus.get(name).intValue() != oldState) {
                                this.mToastStatus.put(name, Integer.valueOf(oldState));
                                VSlog.d(TAG, "set ToastStatus: app=" + name + "  value=" + oldState);
                            }
                        }
                    }
                }
            } catch (JSONException e) {
                VSlog.e(TAG, "initToastStatus: json parse exception " + e);
            }
        }
        putAppToastStatus();
    }

    public void setPackageToastStatus(String name, int state) {
        if (name == null) {
            return;
        }
        synchronized (this.mToastStatusLock) {
            Integer oldStateInteger = this.mToastStatus.get(name);
            int oldState = oldStateInteger != null ? oldStateInteger.intValue() : 0;
            VSlog.d(TAG, "setPackageToastStatus: name=" + name + ", state=" + state + ", oldState=" + oldState);
            if (oldState == state) {
                return;
            }
            this.mToastStatus.put(name, Integer.valueOf(state));
            putAppToastStatus();
        }
    }

    public int getPackageToastStatus(String name) {
        synchronized (this.mToastStatusLock) {
            if (!this.mToastStatus.isEmpty() && name != null) {
                Integer state = this.mToastStatus.get(name);
                if (state != null) {
                    return state.intValue();
                }
                return 1;
            }
            return 1;
        }
    }
}