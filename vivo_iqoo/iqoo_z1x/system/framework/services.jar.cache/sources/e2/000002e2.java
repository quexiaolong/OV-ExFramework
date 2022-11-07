package android.net.util;

import android.net.NattKeepalivePacketData;
import android.net.NattKeepalivePacketDataParcelable;
import java.net.InetAddress;

/* loaded from: classes.dex */
public final class KeepalivePacketDataUtil {
    public static NattKeepalivePacketDataParcelable toStableParcelable(NattKeepalivePacketData pkt) {
        NattKeepalivePacketDataParcelable parcel = new NattKeepalivePacketDataParcelable();
        InetAddress srcAddress = pkt.getSrcAddress();
        InetAddress dstAddress = pkt.getDstAddress();
        parcel.srcAddress = srcAddress.getAddress();
        parcel.srcPort = pkt.getSrcPort();
        parcel.dstAddress = dstAddress.getAddress();
        parcel.dstPort = pkt.getDstPort();
        return parcel;
    }
}