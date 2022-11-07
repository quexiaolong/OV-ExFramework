package android.net;

import android.net.util.IpUtils;
import android.os.Parcel;
import android.os.Parcelable;
import android.system.OsConstants;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/* loaded from: classes.dex */
public class TcpKeepalivePacketData extends KeepalivePacketData implements Parcelable {
    public static final Parcelable.Creator<TcpKeepalivePacketData> CREATOR = new Parcelable.Creator<TcpKeepalivePacketData>() { // from class: android.net.TcpKeepalivePacketData.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TcpKeepalivePacketData createFromParcel(Parcel in) {
            try {
                return TcpKeepalivePacketData.readFromParcel(in);
            } catch (InvalidPacketException e) {
                throw new IllegalArgumentException("Invalid NAT-T keepalive data: " + e.getError());
            }
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TcpKeepalivePacketData[] newArray(int size) {
            return new TcpKeepalivePacketData[size];
        }
    };
    private static final int IPV4_HEADER_LENGTH = 20;
    private static final int IPV6_HEADER_LENGTH = 40;
    private static final String TAG = "TcpKeepalivePacketData";
    private static final int TCP_HEADER_LENGTH = 20;
    public final int ipTos;
    public final int ipTtl;
    public final int tcpAck;
    public final int tcpSeq;
    public final int tcpWnd;
    public final int tcpWndScale;

    private TcpKeepalivePacketData(TcpKeepalivePacketDataParcelable tcpDetails, byte[] data) throws InvalidPacketException, UnknownHostException {
        super(InetAddress.getByAddress(tcpDetails.srcAddress), tcpDetails.srcPort, InetAddress.getByAddress(tcpDetails.dstAddress), tcpDetails.dstPort, data);
        this.tcpSeq = tcpDetails.seq;
        this.tcpAck = tcpDetails.ack;
        this.tcpWnd = tcpDetails.rcvWnd;
        this.tcpWndScale = tcpDetails.rcvWndScale;
        this.ipTos = tcpDetails.tos;
        this.ipTtl = tcpDetails.ttl;
    }

    private TcpKeepalivePacketData(InetAddress srcAddress, int srcPort, InetAddress dstAddress, int dstPort, byte[] data, int tcpSeq, int tcpAck, int tcpWnd, int tcpWndScale, int ipTos, int ipTtl) throws InvalidPacketException {
        super(srcAddress, srcPort, dstAddress, dstPort, data);
        this.tcpSeq = tcpSeq;
        this.tcpAck = tcpAck;
        this.tcpWnd = tcpWnd;
        this.tcpWndScale = tcpWndScale;
        this.ipTos = ipTos;
        this.ipTtl = ipTtl;
    }

    public static TcpKeepalivePacketData tcpKeepalivePacket(TcpKeepalivePacketDataParcelable tcpDetails) throws InvalidPacketException {
        try {
            if (tcpDetails.srcAddress != null && tcpDetails.dstAddress != null && tcpDetails.srcAddress.length == 4 && tcpDetails.dstAddress.length == 4) {
                byte[] packet = buildV4Packet(tcpDetails);
                return new TcpKeepalivePacketData(tcpDetails, packet);
            }
            throw new InvalidPacketException(-21);
        } catch (UnknownHostException e) {
            throw new InvalidPacketException(-21);
        }
    }

    private static byte[] buildV4Packet(TcpKeepalivePacketDataParcelable tcpDetails) {
        ByteBuffer buf = ByteBuffer.allocate(40);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.put((byte) 69);
        buf.put((byte) tcpDetails.tos);
        buf.putShort((short) 40);
        buf.putInt(16384);
        buf.put((byte) tcpDetails.ttl);
        buf.put((byte) OsConstants.IPPROTO_TCP);
        int ipChecksumOffset = buf.position();
        buf.putShort((short) 0);
        buf.put(tcpDetails.srcAddress);
        buf.put(tcpDetails.dstAddress);
        buf.putShort((short) tcpDetails.srcPort);
        buf.putShort((short) tcpDetails.dstPort);
        buf.putInt(tcpDetails.seq);
        buf.putInt(tcpDetails.ack);
        buf.putShort((short) 20496);
        buf.putShort((short) (tcpDetails.rcvWnd >> tcpDetails.rcvWndScale));
        int tcpChecksumOffset = buf.position();
        buf.putShort((short) 0);
        buf.putShort((short) 0);
        buf.putShort(ipChecksumOffset, IpUtils.ipChecksum(buf, 0));
        buf.putShort(tcpChecksumOffset, IpUtils.tcpChecksum(buf, 0, 20, 20));
        return buf.array();
    }

    public boolean equals(Object o) {
        if (o instanceof TcpKeepalivePacketData) {
            TcpKeepalivePacketData other = (TcpKeepalivePacketData) o;
            InetAddress srcAddress = getSrcAddress();
            InetAddress dstAddress = getDstAddress();
            return srcAddress.equals(other.getSrcAddress()) && dstAddress.equals(other.getDstAddress()) && getSrcPort() == other.getSrcPort() && getDstPort() == other.getDstPort() && this.tcpAck == other.tcpAck && this.tcpSeq == other.tcpSeq && this.tcpWnd == other.tcpWnd && this.tcpWndScale == other.tcpWndScale && this.ipTos == other.ipTos && this.ipTtl == other.ipTtl;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(getSrcAddress(), getDstAddress(), Integer.valueOf(getSrcPort()), Integer.valueOf(getDstPort()), Integer.valueOf(this.tcpAck), Integer.valueOf(this.tcpSeq), Integer.valueOf(this.tcpWnd), Integer.valueOf(this.tcpWndScale), Integer.valueOf(this.ipTos), Integer.valueOf(this.ipTtl));
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(getSrcAddress().getHostAddress());
        out.writeString(getDstAddress().getHostAddress());
        out.writeInt(getSrcPort());
        out.writeInt(getDstPort());
        out.writeByteArray(getPacket());
        out.writeInt(this.tcpSeq);
        out.writeInt(this.tcpAck);
        out.writeInt(this.tcpWnd);
        out.writeInt(this.tcpWndScale);
        out.writeInt(this.ipTos);
        out.writeInt(this.ipTtl);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static TcpKeepalivePacketData readFromParcel(Parcel in) throws InvalidPacketException {
        InetAddress srcAddress = InetAddresses.parseNumericAddress(in.readString());
        InetAddress dstAddress = InetAddresses.parseNumericAddress(in.readString());
        int srcPort = in.readInt();
        int dstPort = in.readInt();
        byte[] packet = in.createByteArray();
        int tcpSeq = in.readInt();
        int tcpAck = in.readInt();
        int tcpWnd = in.readInt();
        int tcpWndScale = in.readInt();
        int ipTos = in.readInt();
        int ipTtl = in.readInt();
        return new TcpKeepalivePacketData(srcAddress, srcPort, dstAddress, dstPort, packet, tcpSeq, tcpAck, tcpWnd, tcpWndScale, ipTos, ipTtl);
    }

    public TcpKeepalivePacketDataParcelable toStableParcelable() {
        TcpKeepalivePacketDataParcelable parcel = new TcpKeepalivePacketDataParcelable();
        InetAddress srcAddress = getSrcAddress();
        InetAddress dstAddress = getDstAddress();
        parcel.srcAddress = srcAddress.getAddress();
        parcel.srcPort = getSrcPort();
        parcel.dstAddress = dstAddress.getAddress();
        parcel.dstPort = getDstPort();
        parcel.seq = this.tcpSeq;
        parcel.ack = this.tcpAck;
        parcel.rcvWnd = this.tcpWnd;
        parcel.rcvWndScale = this.tcpWndScale;
        parcel.tos = this.ipTos;
        parcel.ttl = this.ipTtl;
        return parcel;
    }

    public String toString() {
        return "saddr: " + getSrcAddress() + " daddr: " + getDstAddress() + " sport: " + getSrcPort() + " dport: " + getDstPort() + " seq: " + this.tcpSeq + " ack: " + this.tcpAck + " wnd: " + this.tcpWnd + " wndScale: " + this.tcpWndScale + " tos: " + this.ipTos + " ttl: " + this.ipTtl;
    }
}