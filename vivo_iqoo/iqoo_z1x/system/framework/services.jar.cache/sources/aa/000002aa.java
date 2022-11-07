package android.net.netlink;

import java.nio.ByteBuffer;

/* loaded from: classes.dex */
public class NdOption {
    public static final int STRUCT_SIZE = 2;
    public static final NdOption UNKNOWN = new NdOption((byte) 0, 0);
    public final int length;
    public final byte type;

    /* JADX INFO: Access modifiers changed from: package-private */
    public NdOption(byte type, int length) {
        this.type = type;
        this.length = length;
    }

    public static NdOption parse(ByteBuffer buf) {
        if (buf == null || buf.remaining() < 2) {
            return null;
        }
        byte type = buf.get(buf.position());
        int length = Byte.toUnsignedInt(buf.get(buf.position() + 1));
        if (length == 0) {
            return null;
        }
        if (type == 38) {
            return StructNdOptPref64.parse(buf);
        }
        int newPosition = Math.min(buf.limit(), buf.position() + (length * 8));
        buf.position(newPosition);
        return UNKNOWN;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writeToByteBuffer(ByteBuffer buf) {
        buf.put(this.type);
        buf.put((byte) this.length);
    }

    public String toString() {
        return String.format("NdOption(%d, %d)", Integer.valueOf(Byte.toUnsignedInt(this.type)), Integer.valueOf(this.length));
    }
}