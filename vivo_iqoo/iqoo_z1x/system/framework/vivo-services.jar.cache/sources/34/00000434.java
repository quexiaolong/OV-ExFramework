package com.android.server.policy.key;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.PowerManager;
import android.text.TextUtils;
import com.android.server.policy.VivoPolicyConstant;
import com.android.server.policy.key.VivoAIKeyHandler;
import com.vivo.common.utils.VLog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/* loaded from: classes.dex */
public class VivoAIKeyDoubleClick {
    private static final String ACTION = "vivo.intent.action.JOVI_KEY_DOUBLE_CLICK";
    private static final String ORDER = "vivo.settings.double_click_order";
    private static final String TAG = "AIKeyDoubleClick";
    public static final String USER_SELECT = "jovi_double_click_key_function";
    private static final String VALUES = "vivo.settings.double_click_values";
    private String mCurrentChoose;
    private VivoAIKeyHandler mVivoAiKey;
    private PowerManager.WakeLock mWakeLock;
    private final List<VivoAIKeyHandler.Target> targets = new ArrayList();

    /* JADX INFO: Access modifiers changed from: package-private */
    public VivoAIKeyDoubleClick(VivoAIKeyHandler handler, Context mContext) {
        this.mVivoAiKey = handler;
        PowerManager powerManager = (PowerManager) mContext.getSystemService("power");
        this.mWakeLock = powerManager.newWakeLock(1, TAG);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setChoose(String value) {
        this.mCurrentChoose = value;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int updateData(Context context) {
        long debugStart = System.currentTimeMillis();
        List<ResolveInfo> infos = null;
        try {
            try {
                HashMap<String, ResolveInfo> resolveInfoHashMap = new HashMap<>();
                PackageManager packageManager = context.getPackageManager();
                Intent intent = new Intent(ACTION);
                infos = packageManager.queryIntentServices(intent, 786624);
                if (infos == null) {
                    infos = new ArrayList<>();
                }
                infos.addAll(packageManager.queryIntentActivities(intent, 786624));
                String signature = context.getPackageName();
                this.targets.clear();
                VivoAIKeyHandler.handleComponents(TAG, this.targets, infos, resolveInfoHashMap, packageManager, ORDER, ACTION, signature);
                this.mCurrentChoose = VivoAIKeyHandler.checkChooseData(TAG, this.targets, VALUES, this.mCurrentChoose, USER_SELECT, context);
                VLog.d(TAG, "infos size = " + infos.size());
                if (VivoAIKeyHandler.isDebug()) {
                    VLog.d(TAG, "updateData SPENT " + (System.currentTimeMillis() - debugStart) + " ms");
                }
                if (infos == null) {
                    return 0;
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (VivoAIKeyHandler.isDebug()) {
                    VLog.d(TAG, "updateData SPENT " + (System.currentTimeMillis() - debugStart) + " ms");
                }
                if (infos == null) {
                    return 0;
                }
            }
        } catch (Throwable th) {
            if (VivoAIKeyHandler.isDebug()) {
                VLog.d(TAG, "updateData SPENT " + (System.currentTimeMillis() - debugStart) + " ms");
            }
            if (infos == null) {
                return 0;
            }
        }
        return infos.size();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void disaptchUp() {
        String clickMode;
        try {
            Intent intent = new Intent(ACTION);
            intent.putExtra("keyCode", VivoPolicyConstant.KEYCODE_AI);
            intent.putExtra("keyEvent", "ai_double_click_up");
            VLog.v(TAG, "disaptchUp mCurrentChoose = " + this.mCurrentChoose);
            if (this.mWakeLock != null && !this.mWakeLock.isHeld()) {
                this.mWakeLock.acquire(1000L);
            }
            for (VivoAIKeyHandler.Target target : this.targets) {
                if (target.metaData != null && !TextUtils.isEmpty(target.pkgName) && !TextUtils.isEmpty(target.componentName) && TextUtils.equals(target.action, ACTION) && (clickMode = target.metaData.getString(VALUES)) != null && TextUtils.equals(this.mCurrentChoose, clickMode)) {
                    ComponentName componentName = new ComponentName(target.pkgName, target.componentName);
                    this.mVivoAiKey.sendToTarget(componentName, intent, target, 1003);
                    if (VivoAIKeyHandler.isDebug()) {
                        VLog.v(TAG, "=> send DOUBLE CLICK UP to Component  " + componentName);
                    }
                }
            }
        } catch (Exception e) {
            VLog.d(TAG, "error " + e.getMessage());
        }
    }
}