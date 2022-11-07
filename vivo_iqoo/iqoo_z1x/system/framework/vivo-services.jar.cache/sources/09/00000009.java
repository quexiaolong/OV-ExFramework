package android.hardware.graphics.common.V1_0;

import java.util.ArrayList;

/* loaded from: classes.dex */
public final class BufferUsage {
    public static final long CAMERA_INPUT = 262144;
    public static final long CAMERA_OUTPUT = 131072;
    public static final long COMPOSER_CLIENT_TARGET = 4096;
    public static final long COMPOSER_CURSOR = 32768;
    public static final long COMPOSER_OVERLAY = 2048;
    public static final long CPU_READ_MASK = 15;
    public static final long CPU_READ_NEVER = 0;
    public static final long CPU_READ_OFTEN = 3;
    public static final long CPU_READ_RARELY = 2;
    public static final long CPU_WRITE_MASK = 240;
    public static final long CPU_WRITE_NEVER = 0;
    public static final long CPU_WRITE_OFTEN = 48;
    public static final long CPU_WRITE_RARELY = 32;
    public static final long GPU_DATA_BUFFER = 16777216;
    public static final long GPU_RENDER_TARGET = 512;
    public static final long GPU_TEXTURE = 256;
    public static final long PROTECTED = 16384;
    public static final long RENDERSCRIPT = 1048576;
    public static final long SENSOR_DIRECT_DATA = 8388608;
    public static final long VENDOR_MASK = 4026531840L;
    public static final long VENDOR_MASK_HI = -281474976710656L;
    public static final long VIDEO_DECODER = 4194304;
    public static final long VIDEO_ENCODER = 65536;

    public static final String toString(long o) {
        if (o == 15) {
            return "CPU_READ_MASK";
        }
        if (o == 0) {
            return "CPU_READ_NEVER";
        }
        if (o == 2) {
            return "CPU_READ_RARELY";
        }
        if (o == 3) {
            return "CPU_READ_OFTEN";
        }
        if (o == 240) {
            return "CPU_WRITE_MASK";
        }
        if (o == 0) {
            return "CPU_WRITE_NEVER";
        }
        if (o == 32) {
            return "CPU_WRITE_RARELY";
        }
        if (o == 48) {
            return "CPU_WRITE_OFTEN";
        }
        if (o == 256) {
            return "GPU_TEXTURE";
        }
        if (o == 512) {
            return "GPU_RENDER_TARGET";
        }
        if (o == COMPOSER_OVERLAY) {
            return "COMPOSER_OVERLAY";
        }
        if (o == COMPOSER_CLIENT_TARGET) {
            return "COMPOSER_CLIENT_TARGET";
        }
        if (o == PROTECTED) {
            return "PROTECTED";
        }
        if (o == COMPOSER_CURSOR) {
            return "COMPOSER_CURSOR";
        }
        if (o == VIDEO_ENCODER) {
            return "VIDEO_ENCODER";
        }
        if (o == CAMERA_OUTPUT) {
            return "CAMERA_OUTPUT";
        }
        if (o == CAMERA_INPUT) {
            return "CAMERA_INPUT";
        }
        if (o == RENDERSCRIPT) {
            return "RENDERSCRIPT";
        }
        if (o == VIDEO_DECODER) {
            return "VIDEO_DECODER";
        }
        if (o == SENSOR_DIRECT_DATA) {
            return "SENSOR_DIRECT_DATA";
        }
        if (o == GPU_DATA_BUFFER) {
            return "GPU_DATA_BUFFER";
        }
        if (o == VENDOR_MASK) {
            return "VENDOR_MASK";
        }
        if (o == VENDOR_MASK_HI) {
            return "VENDOR_MASK_HI";
        }
        return "0x" + Long.toHexString(o);
    }

    public static final String dumpBitfield(long o) {
        ArrayList<String> list = new ArrayList<>();
        long flipped = 0;
        if ((o & 15) == 15) {
            list.add("CPU_READ_MASK");
            flipped = 0 | 15;
        }
        list.add("CPU_READ_NEVER");
        if ((o & 2) == 2) {
            list.add("CPU_READ_RARELY");
            flipped |= 2;
        }
        if ((o & 3) == 3) {
            list.add("CPU_READ_OFTEN");
            flipped |= 3;
        }
        if ((o & 240) == 240) {
            list.add("CPU_WRITE_MASK");
            flipped |= 240;
        }
        list.add("CPU_WRITE_NEVER");
        if ((o & 32) == 32) {
            list.add("CPU_WRITE_RARELY");
            flipped |= 32;
        }
        if ((o & 48) == 48) {
            list.add("CPU_WRITE_OFTEN");
            flipped |= 48;
        }
        if ((o & 256) == 256) {
            list.add("GPU_TEXTURE");
            flipped |= 256;
        }
        if ((o & 512) == 512) {
            list.add("GPU_RENDER_TARGET");
            flipped |= 512;
        }
        if ((o & COMPOSER_OVERLAY) == COMPOSER_OVERLAY) {
            list.add("COMPOSER_OVERLAY");
            flipped |= COMPOSER_OVERLAY;
        }
        if ((o & COMPOSER_CLIENT_TARGET) == COMPOSER_CLIENT_TARGET) {
            list.add("COMPOSER_CLIENT_TARGET");
            flipped |= COMPOSER_CLIENT_TARGET;
        }
        if ((o & PROTECTED) == PROTECTED) {
            list.add("PROTECTED");
            flipped |= PROTECTED;
        }
        if ((o & COMPOSER_CURSOR) == COMPOSER_CURSOR) {
            list.add("COMPOSER_CURSOR");
            flipped |= COMPOSER_CURSOR;
        }
        if ((o & VIDEO_ENCODER) == VIDEO_ENCODER) {
            list.add("VIDEO_ENCODER");
            flipped |= VIDEO_ENCODER;
        }
        if ((o & CAMERA_OUTPUT) == CAMERA_OUTPUT) {
            list.add("CAMERA_OUTPUT");
            flipped |= CAMERA_OUTPUT;
        }
        if ((o & CAMERA_INPUT) == CAMERA_INPUT) {
            list.add("CAMERA_INPUT");
            flipped |= CAMERA_INPUT;
        }
        if ((o & RENDERSCRIPT) == RENDERSCRIPT) {
            list.add("RENDERSCRIPT");
            flipped |= RENDERSCRIPT;
        }
        if ((o & VIDEO_DECODER) == VIDEO_DECODER) {
            list.add("VIDEO_DECODER");
            flipped |= VIDEO_DECODER;
        }
        if ((o & SENSOR_DIRECT_DATA) == SENSOR_DIRECT_DATA) {
            list.add("SENSOR_DIRECT_DATA");
            flipped |= SENSOR_DIRECT_DATA;
        }
        if ((o & GPU_DATA_BUFFER) == GPU_DATA_BUFFER) {
            list.add("GPU_DATA_BUFFER");
            flipped |= GPU_DATA_BUFFER;
        }
        if ((o & VENDOR_MASK) == VENDOR_MASK) {
            list.add("VENDOR_MASK");
            flipped |= VENDOR_MASK;
        }
        if ((o & VENDOR_MASK_HI) == VENDOR_MASK_HI) {
            list.add("VENDOR_MASK_HI");
            flipped |= VENDOR_MASK_HI;
        }
        if (o != flipped) {
            list.add("0x" + Long.toHexString((~flipped) & o));
        }
        return String.join(" | ", list);
    }
}