package com.android.server.notification;

import android.net.Uri;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

/* loaded from: classes.dex */
public interface IVivoConditionProviders {

    /* loaded from: classes.dex */
    public interface IVivoConditionProvidersExport {
        IVivoConditionProviders getVivoInjectionInstance();
    }

    void markPackageWithSpecialPermission(String str, int i);

    void onPackageRemoved(String str, int i);

    void onUserSwitched(int i);

    void readXml(XmlPullParser xmlPullParser);

    void subscribeForGrantedApps(Uri uri, String str);

    void unsubscribeForGrantedApps(Uri uri);

    void writeXml(XmlSerializer xmlSerializer);
}