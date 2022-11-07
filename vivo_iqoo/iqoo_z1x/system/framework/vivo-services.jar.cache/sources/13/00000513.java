package com.android.server.wm;

import android.content.Intent;
import android.os.SystemProperties;
import com.android.server.VivoDoubleInstanceServiceImpl;
import com.vivo.common.doubleinstance.DoubleInstanceConfig;
import com.vivo.services.superresolution.Constant;
import java.util.ArrayList;
import java.util.Iterator;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoActivityStarterRequestImpl implements IVivoActivityStarterRequest {
    static final String TAG = "VivoActivityStarterRequestImpl";
    private VivoDoubleInstanceServiceImpl mVivoDoubleInstanceService;

    public VivoActivityStarterRequestImpl() {
        this.mVivoDoubleInstanceService = null;
        this.mVivoDoubleInstanceService = VivoDoubleInstanceServiceImpl.getInstance();
    }

    public int startActivityMayWaitForDoubleInstance(Intent intent, String callingPackage, int userId, int realCallingUid) {
        int userIdTmp = userId;
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl != null && vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable() && 999 == userId) {
            if (intent.getComponent() != null || intent.getPackage() != null) {
                ArrayList<String> doubleAppPkgNamesWhiteList = DoubleInstanceConfig.getInstance().getSupportedAppPackageName();
                ArrayList<String> systemAppInDoubleUser = DoubleInstanceConfig.getInstance().getSystemAppInDoubleUser();
                boolean isDoubleAppCalling = false;
                boolean isSelfCalling = true;
                boolean isSystemAppInDoubleUserCalled = false;
                synchronized (DoubleInstanceConfig.getInstance()) {
                    Iterator<String> it = doubleAppPkgNamesWhiteList.iterator();
                    while (it.hasNext()) {
                        String doubleAppPkgName = it.next();
                        if (doubleAppPkgName.equals(callingPackage)) {
                            isDoubleAppCalling = true;
                        }
                    }
                    Iterator<String> it2 = systemAppInDoubleUser.iterator();
                    while (it2.hasNext()) {
                        String pkgName = it2.next();
                        if ((intent.getComponent() != null && intent.getComponent().getPackageName().equals(pkgName)) || (intent.getPackage() != null && intent.getPackage().equals(pkgName))) {
                            isSystemAppInDoubleUserCalled = true;
                            break;
                        }
                    }
                }
                if ((intent.getComponent() != null && !intent.getComponent().getPackageName().equals(callingPackage)) || (intent.getPackage() != null && !intent.getPackage().equals(callingPackage))) {
                    isSelfCalling = false;
                }
                if ("com.whatsapp".equals(intent.getPackage()) && "com.instagram.android".equals(callingPackage)) {
                    isSelfCalling = true;
                    intent.fixUris(userId);
                }
                if ("jp.naver.line.android".equals(callingPackage) && "com.google.android.gms".equals(intent.getPackage())) {
                    isSystemAppInDoubleUserCalled = true;
                }
                if ("android.intent.action.VIEW".equals(intent.getAction()) && "com.google.android.packageinstaller".equals(intent.getPackage())) {
                    isSystemAppInDoubleUserCalled = false;
                }
                if (isDoubleAppCalling && !isSelfCalling && !isSystemAppInDoubleUserCalled) {
                    if (this.mVivoDoubleInstanceService.isDoubleInstanceDebugEnable()) {
                        VSlog.d(TAG, "dobule app calling nonself activity");
                    }
                    userIdTmp = 0;
                }
                if (intent.getComponent() != null && "com.android.systemui.chooser.ChooserActivity".equals(intent.getComponent().getClassName())) {
                    return 0;
                }
                return userIdTmp;
            } else if ("android.intent.action.VIEW".equals(intent.getAction()) && "market".equals(intent.getScheme())) {
                return 0;
            } else {
                if ("android.intent.action.CHOOSER".equals(intent.getAction())) {
                    Intent extraIntent = (Intent) intent.getParcelableExtra("android.intent.extra.INTENT");
                    if (extraIntent != null && "com.whatsapp.action.WHATSAPP_RECORDING".equals(extraIntent.getAction())) {
                        extraIntent.putExtra("userId", userId);
                        return 0;
                    }
                    return userIdTmp;
                } else if ("android.intent.action.INSERT_OR_EDIT".equals(intent.getAction()) && "vnd.android.cursor.item/contact".equals(intent.getType())) {
                    return 0;
                } else {
                    if ("android.intent.action.INSERT".equals(intent.getAction()) && "vnd.android.cursor.dir/raw_contact".equals(intent.getType())) {
                        return 0;
                    }
                    if (("android.settings.MANAGE_APPLICATIONS_SETTINGS".equals(intent.getAction()) || "android.settings.action.MANAGE_WRITE_SETTINGS".equals(intent.getAction()) || "android.settings.NFC_SETTINGS".equals(intent.getAction())) && "no".equals(SystemProperties.get("ro.vivo.product.overseas", "no"))) {
                        return 0;
                    }
                    if ("android.settings.action.MANAGE_OVERLAY_PERMISSION".equals(intent.getAction()) && "no".equals(SystemProperties.get("ro.vivo.product.overseas", "no"))) {
                        return 0;
                    }
                    if (("android.settings.CHANNEL_NOTIFICATION_SETTINGS".equals(intent.getAction()) || "android.settings.APP_NOTIFICATION_SETTINGS".equals(intent.getAction())) && "no".equals(SystemProperties.get("ro.vivo.product.overseas", "no"))) {
                        intent.putExtra("extra_double_app_uid", realCallingUid);
                        return 0;
                    } else if (Constant.APP_WEIXIN.equals(callingPackage) && "android.intent.action.MAIN".equals(intent.getAction()) && intent.getCategories() != null && intent.getCategories().contains("android.intent.category.HOME")) {
                        return 0;
                    } else {
                        if ("com.viber.voip".equals(callingPackage) && "android.intent.action.CREATE_DOCUMENT".equals(intent.getAction()) && intent.getCategories() != null && intent.getCategories().contains("android.intent.category.OPENABLE")) {
                            return 0;
                        }
                        return userIdTmp;
                    }
                }
            }
        } else if ("com.whatsapp.action.WHATSAPP_RECORDING".equals(intent.getAction())) {
            return intent.getIntExtra("userId", 0);
        } else {
            return userIdTmp;
        }
    }
}