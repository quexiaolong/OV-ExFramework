package android.net.shared;

import android.net.InetAddresses;
import java.net.InetAddress;

/* loaded from: classes.dex */
public final class IpConfigurationParcelableUtil {
    public static String parcelAddress(InetAddress addr) {
        if (addr == null) {
            return null;
        }
        return addr.getHostAddress();
    }

    public static InetAddress unparcelAddress(String addr) {
        if (addr == null) {
            return null;
        }
        return InetAddresses.parseNumericAddress(addr);
    }
}