package android.net.util;

import android.net.DhcpResults;
import android.net.DhcpResultsParcelable;
import android.net.shared.IpConfigurationParcelableUtil;
import java.net.Inet4Address;

/* loaded from: classes.dex */
public class DhcpResultsCompatUtil {
    public static DhcpResults fromStableParcelable(DhcpResultsParcelable p) {
        if (p == null) {
            return null;
        }
        DhcpResults results = new DhcpResults(p.baseConfiguration);
        results.leaseDuration = p.leaseDuration;
        results.mtu = p.mtu;
        results.serverAddress = (Inet4Address) IpConfigurationParcelableUtil.unparcelAddress(p.serverAddress);
        results.vendorInfo = p.vendorInfo;
        results.serverHostName = p.serverHostName;
        results.captivePortalApiUrl = p.captivePortalApiUrl;
        return results;
    }
}