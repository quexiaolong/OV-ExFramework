package com.android.server.notification;

import android.net.Uri;
import android.os.RemoteException;
import android.os.UserHandle;
import android.service.notification.IConditionProvider;
import android.service.notification.ZenModeConfig;
import android.text.TextUtils;
import android.util.ArrayMap;
import com.android.internal.util.XmlUtils;
import com.android.server.notification.ManagedServices;
import com.vivo.face.common.data.Constants;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoConditionProvidersImpl implements IVivoConditionProviders {
    private final String TAG;
    private final ConditionProviders mConditionProviders;
    private final ArrayMap<Integer, List<String>> mSpecialGrantedApps = new ArrayMap<>(16);
    private int mCurrentUser = 0;

    public VivoConditionProvidersImpl(ConditionProviders conditionProviders) {
        this.mConditionProviders = conditionProviders;
        this.TAG = conditionProviders.TAG;
    }

    public void subscribeForGrantedApps(Uri uriForGrantedApp, String id) {
        if (ZenModeConfig.isValidScheduleConditionId(uriForGrantedApp) && id != null) {
            Uri result = uriForGrantedApp.buildUpon().appendQueryParameter("zenRuleId", id).build();
            synchronized (this.mSpecialGrantedApps) {
                List<String> appsPerUser = this.mSpecialGrantedApps.get(Integer.valueOf(this.mCurrentUser));
                if (appsPerUser != null && !appsPerUser.isEmpty()) {
                    int size = appsPerUser.size();
                    for (int j = 0; j < size; j++) {
                        String pkg = appsPerUser.get(j);
                        IConditionProvider provider = findConditionProvider(pkg);
                        if (provider != null) {
                            try {
                                provider.onSubscribe(result);
                            } catch (RemoteException e) {
                                String str = this.TAG;
                                VSlog.e(str, "subscribeForGrantedApps failed, " + e.getMessage());
                            }
                        }
                    }
                }
            }
        }
    }

    public void unsubscribeForGrantedApps(Uri uriForGrantedApp) {
        if (ZenModeConfig.isValidScheduleConditionId(uriForGrantedApp)) {
            synchronized (this.mSpecialGrantedApps) {
                List<String> appsPerUser = this.mSpecialGrantedApps.get(Integer.valueOf(this.mCurrentUser));
                if (appsPerUser != null && !appsPerUser.isEmpty()) {
                    int size = appsPerUser.size();
                    for (int j = 0; j < size; j++) {
                        String pkg = appsPerUser.get(j);
                        IConditionProvider provider = findConditionProvider(pkg);
                        if (provider != null) {
                            try {
                                provider.onUnsubscribe(uriForGrantedApp);
                            } catch (RemoteException e) {
                                String str = this.TAG;
                                VSlog.e(str, "unsubscribeForGrantedApps failed, " + e.getMessage());
                            }
                        }
                    }
                }
            }
        }
    }

    public void writeXml(XmlSerializer out) {
        try {
            out.startTag(null, "vivo_zen_policy_manager_apps");
            synchronized (this.mSpecialGrantedApps) {
                int userSize = this.mSpecialGrantedApps.size();
                for (int i = 0; i < userSize; i++) {
                    List<String> appsByUser = this.mSpecialGrantedApps.valueAt(i);
                    int user = this.mSpecialGrantedApps.keyAt(i).intValue();
                    if (appsByUser != null && !appsByUser.isEmpty()) {
                        int size = appsByUser.size();
                        for (int j = 0; j < size; j++) {
                            if (!TextUtils.isEmpty(appsByUser.get(j))) {
                                out.startTag(null, "tag_special_granted_app");
                                out.attribute(null, "pkg", appsByUser.get(i));
                                out.attribute(null, "user", String.valueOf(user));
                                out.endTag(null, "tag_special_granted_app");
                            }
                        }
                    }
                }
            }
            out.endTag(null, "vivo_zen_policy_manager_apps");
        } catch (IOException e) {
            VSlog.e(this.TAG, "write xml error");
        }
    }

    public void readXml(XmlPullParser parser) {
        synchronized (this.mSpecialGrantedApps) {
            while (true) {
                try {
                    int type = parser.next();
                    if (type == 1) {
                        break;
                    }
                    String tag = parser.getName();
                    if (type == 3 && "vivo_zen_policy_manager_apps".equals(tag)) {
                        break;
                    } else if (type == 2 && "tag_special_granted_app".equals(tag)) {
                        String pkg = XmlUtils.readStringAttribute(parser, "pkg");
                        int uid = XmlUtils.readIntAttribute(parser, "user");
                        markPackageWithSpecialPermission(pkg, uid);
                    }
                } catch (IOException | XmlPullParserException e) {
                    VSlog.e(this.TAG, "read xml error");
                }
            }
        }
    }

    public void markPackageWithSpecialPermission(String pkg, int userId) {
        synchronized (this.mSpecialGrantedApps) {
            List<String> specialGrantedAppsByUser = this.mSpecialGrantedApps.get(Integer.valueOf(userId));
            if (specialGrantedAppsByUser == null) {
                specialGrantedAppsByUser = new ArrayList(10);
                this.mSpecialGrantedApps.put(Integer.valueOf(userId), specialGrantedAppsByUser);
            }
            if (!specialGrantedAppsByUser.contains(pkg)) {
                specialGrantedAppsByUser.add(pkg);
            }
        }
    }

    public void onPackageRemoved(String pkgName, int pkgUid) {
        int user = UserHandle.getUserId(pkgUid);
        synchronized (this.mSpecialGrantedApps) {
            List<String> specialGrantedAppsByUser = this.mSpecialGrantedApps.get(Integer.valueOf(user));
            if (specialGrantedAppsByUser != null) {
                specialGrantedAppsByUser.remove(pkgName);
            }
        }
    }

    public IConditionProvider findConditionProvider(String pkg) {
        if (pkg == null || Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK.equals(pkg)) {
            return null;
        }
        for (ManagedServices.ManagedServiceInfo service : this.mConditionProviders.getServices()) {
            if (pkg.equals(service.component.getPackageName())) {
                return provider(service);
            }
        }
        return null;
    }

    private static IConditionProvider provider(ManagedServices.ManagedServiceInfo info) {
        if (info == null) {
            return null;
        }
        return info.service;
    }

    public void onUserSwitched(int user) {
        synchronized (this.mSpecialGrantedApps) {
            this.mCurrentUser = user;
        }
    }
}