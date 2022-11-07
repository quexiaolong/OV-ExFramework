package android.net.shared;

import android.net.LinkProperties;
import android.net.ProxyInfo;

/* loaded from: classes.dex */
public final class LinkPropertiesParcelableUtil {
    @Deprecated
    public static LinkProperties toStableParcelable(LinkProperties lp) {
        return lp;
    }

    @Deprecated
    public static ProxyInfo toStableParcelable(ProxyInfo info) {
        return info;
    }
}