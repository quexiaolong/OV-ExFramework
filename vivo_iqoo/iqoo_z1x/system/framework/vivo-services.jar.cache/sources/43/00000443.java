package com.android.server.policy.key;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;
import com.android.server.policy.VivoPolicyConstant;
import com.android.server.policy.key.VivoAIKeyHandler;
import com.vivo.common.utils.VLog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/* loaded from: classes.dex */
public class VivoAIKeyShortPress {
    public static final String ACTION = "vivo.settings.JOVI_KEY_SETTINGS_ACTION";
    public static final String ALWAYS_LONG_PRESS = "vivo.aikey.always_longpress";
    private static final String ORDER = "vivo.settings.order";
    private static final String TAG = "AIKeyShortPress";
    public static final String USER_SELECT_SHORT_PRESS = "jovi_key_function";
    private static final String VALUES = "vivo.settings.values";
    private String mCurrentChoose;
    private VivoAIKeyHandler mVivoAiKey;
    private final List<VivoAIKeyHandler.Target> targets = new ArrayList();

    /* JADX INFO: Access modifiers changed from: package-private */
    public VivoAIKeyShortPress(VivoAIKeyHandler handler) {
        this.mVivoAiKey = handler;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setShortChoose(String value) {
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
                this.mCurrentChoose = VivoAIKeyHandler.checkChooseData(TAG, this.targets, VALUES, this.mCurrentChoose, USER_SELECT_SHORT_PRESS, context);
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
    public Set<VivoAIKeyHandler.Target> dispatchShortDown() {
        String mode;
        Intent intent = new Intent(ACTION);
        intent.putExtra("keyCode", VivoPolicyConstant.KEYCODE_AI);
        intent.putExtra("keyEvent", "ai_down");
        long debugStart = System.currentTimeMillis();
        Set<VivoAIKeyHandler.Target> needUpEvents = new HashSet<>();
        for (VivoAIKeyHandler.Target target : this.targets) {
            try {
                if (target.metaData != null && !TextUtils.isEmpty(target.pkgName) && !TextUtils.isEmpty(target.componentName) && TextUtils.equals(target.action, ACTION) && (mode = target.metaData.getString(VALUES)) != null && TextUtils.equals(this.mCurrentChoose, mode)) {
                    ComponentName componentName = new ComponentName(target.pkgName, target.componentName);
                    this.mVivoAiKey.sendToTarget(componentName, intent, target, 1000);
                    needUpEvents.add(target);
                    if (VivoAIKeyHandler.isDebug()) {
                        VLog.v(TAG, "=> send down to Component  " + componentName);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        VLog.d(TAG, "target size is " + this.targets.size() + " ,dispatch SPENT " + (System.currentTimeMillis() - debugStart) + " ms, currentChoose = " + this.mCurrentChoose);
        return needUpEvents;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void disaptchUp(Set<VivoAIKeyHandler.Target> needUpEvents) {
        try {
            Intent intent = new Intent(ACTION);
            intent.putExtra("keyCode", VivoPolicyConstant.KEYCODE_AI);
            intent.putExtra("keyEvent", "ai_up");
            for (VivoAIKeyHandler.Target target : needUpEvents) {
                if (target.metaData != null && !TextUtils.isEmpty(target.pkgName) && !TextUtils.isEmpty(target.componentName) && TextUtils.equals(target.action, ACTION)) {
                    ComponentName componentName = new ComponentName(target.pkgName, target.componentName);
                    this.mVivoAiKey.sendToTarget(componentName, intent, target, 1001);
                    if (VivoAIKeyHandler.isDebug()) {
                        VLog.v(TAG, "=> send up to Component  " + componentName);
                    }
                }
            }
        } catch (Exception e) {
            VLog.d(TAG, "error " + e.getMessage());
        }
    }

    Set<VivoAIKeyHandler.Target> dispatchLongPress(VivoAIKeyHandler handler) {
        Intent intent = new Intent(ACTION);
        intent.putExtra("keyCode", VivoPolicyConstant.KEYCODE_AI);
        intent.putExtra("keyEvent", "ai_long_press");
        if (VivoAIKeyHandler.isDebug()) {
            VLog.v(TAG, "LONG PRESS target size = " + this.targets.size());
        }
        Set<VivoAIKeyHandler.Target> needEvents = new HashSet<>();
        for (VivoAIKeyHandler.Target target : this.targets) {
            try {
                if (target.metaData != null && !TextUtils.isEmpty(target.pkgName) && !TextUtils.isEmpty(target.componentName) && TextUtils.equals(target.action, ACTION)) {
                    ComponentName componentName = new ComponentName(target.pkgName, target.componentName);
                    boolean isAlwaysLongpress = TextUtils.equals(target.metaData.getString(ALWAYS_LONG_PRESS), "yes");
                    if (VivoAIKeyHandler.isDebug()) {
                        VLog.v(TAG, componentName + ", isAlwaysLongpress = " + isAlwaysLongpress);
                    }
                    if (isAlwaysLongpress) {
                        handler.sendToTarget(componentName, intent, target, 1002);
                        needEvents.add(target);
                        if (VivoAIKeyHandler.isDebug()) {
                            VLog.v(TAG, "=> dispatchLongPress to Component  " + componentName);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return needEvents;
    }

    void dispatchLongPressUp(Set<VivoAIKeyHandler.Target> needUpEvents) {
        try {
            Intent intent = new Intent(ACTION);
            intent.putExtra("keyCode", VivoPolicyConstant.KEYCODE_AI);
            intent.putExtra("keyEvent", "ai_up");
            for (VivoAIKeyHandler.Target target : needUpEvents) {
                if (target.metaData != null && !TextUtils.isEmpty(target.pkgName) && !TextUtils.isEmpty(target.componentName) && TextUtils.equals(target.action, ACTION)) {
                    ComponentName componentName = new ComponentName(target.pkgName, target.componentName);
                    boolean isAlwaysLongpress = TextUtils.equals(target.metaData.getString(ALWAYS_LONG_PRESS), "yes");
                    if (VivoAIKeyHandler.isDebug()) {
                        VLog.v(TAG, componentName + ", isAlwaysLongpress = " + isAlwaysLongpress);
                    }
                    if (isAlwaysLongpress) {
                        this.mVivoAiKey.sendToTarget(componentName, intent, target, 1001);
                        if (VivoAIKeyHandler.isDebug()) {
                            VLog.v(TAG, "=> dispatchLongPressUp to Component  " + componentName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            VLog.d(TAG, "error " + e.getMessage());
        }
    }
}