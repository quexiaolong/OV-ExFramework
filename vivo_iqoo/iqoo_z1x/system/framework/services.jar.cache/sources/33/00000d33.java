package com.android.server.display;

import android.hardware.display.DeviceProductInfo;
import android.view.Display;
import android.view.DisplayAddress;
import android.view.DisplayCutout;
import com.android.server.policy.IVivoRatioControllerUtils;
import com.android.server.vr.Vr2dDisplay;
import java.util.Arrays;
import java.util.Objects;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class DisplayDeviceInfo {
    public static final int DIFF_COLOR_MODE = 4;
    public static final int DIFF_OTHER = 2;
    public static final int DIFF_STATE = 1;
    public static final int FLAG_CAN_SHOW_WITH_INSECURE_KEYGUARD = 512;
    public static final int FLAG_DEFAULT_DISPLAY = 1;
    public static final int FLAG_DESTROY_CONTENT_ON_REMOVAL = 1024;
    public static final int FLAG_MASK_DISPLAY_CUTOUT = 2048;
    public static final int FLAG_NEVER_BLANK = 32;
    public static final int FLAG_OWN_CONTENT_ONLY = 128;
    public static final int FLAG_PRESENTATION = 64;
    public static final int FLAG_PRIVATE = 16;
    public static final int FLAG_ROTATES_WITH_CONTENT = 2;
    public static final int FLAG_ROUND = 256;
    public static final int FLAG_SECURE = 4;
    public static final int FLAG_SHOULD_SHOW_SYSTEM_DECORATIONS = 4096;
    public static final int FLAG_SUPPORTS_PROTECTED_BUFFERS = 8;
    public static final int FLAG_TRUSTED = 8192;
    public static final int TOUCH_EXTERNAL = 2;
    public static final int TOUCH_INTERNAL = 1;
    public static final int TOUCH_NONE = 0;
    public static final int TOUCH_VIRTUAL = 3;
    public DisplayAddress address;
    public boolean allmSupported;
    public long appVsyncOffsetNanos;
    public int colorMode;
    public int defaultModeId;
    public int densityDpi;
    public DeviceProductInfo deviceProductInfo;
    public DisplayCutout displayCutout;
    public int flags;
    public boolean gameContentTypeSupported;
    public Display.HdrCapabilities hdrCapabilities;
    public int height;
    public int modeId;
    public String name;
    public String ownerPackageName;
    public int ownerUid;
    public long presentationDeadlineNanos;
    public int touch;
    public int type;
    public String uniqueId;
    public int width;
    public float xDpi;
    public float yDpi;
    public Display.Mode[] supportedModes = Display.Mode.EMPTY_ARRAY;
    public int[] supportedColorModes = {0};
    public int rotation = 0;
    public int state = 2;

    public void setAssumedDensityForExternalDisplay(int width, int height) {
        int min = (Math.min(width, height) * Vr2dDisplay.DEFAULT_VIRTUAL_DISPLAY_DPI) / IVivoRatioControllerUtils.WIDTH_1080P;
        this.densityDpi = min;
        this.xDpi = min;
        this.yDpi = min;
    }

    public boolean equals(Object o) {
        return (o instanceof DisplayDeviceInfo) && equals((DisplayDeviceInfo) o);
    }

    public boolean equals(DisplayDeviceInfo other) {
        return other != null && diff(other) == 0;
    }

    public int diff(DisplayDeviceInfo other) {
        int diff = 0;
        if (this.state != other.state) {
            diff = 0 | 1;
        }
        if (this.colorMode != other.colorMode) {
            diff |= 4;
        }
        if (!Objects.equals(this.name, other.name) || !Objects.equals(this.uniqueId, other.uniqueId) || this.width != other.width || this.height != other.height || this.modeId != other.modeId || this.defaultModeId != other.defaultModeId || !Arrays.equals(this.supportedModes, other.supportedModes) || !Arrays.equals(this.supportedColorModes, other.supportedColorModes) || !Objects.equals(this.hdrCapabilities, other.hdrCapabilities) || this.allmSupported != other.allmSupported || this.gameContentTypeSupported != other.gameContentTypeSupported || this.densityDpi != other.densityDpi || this.xDpi != other.xDpi || this.yDpi != other.yDpi || this.appVsyncOffsetNanos != other.appVsyncOffsetNanos || this.presentationDeadlineNanos != other.presentationDeadlineNanos || this.flags != other.flags || !Objects.equals(this.displayCutout, other.displayCutout) || this.touch != other.touch || this.rotation != other.rotation || this.type != other.type || !Objects.equals(this.address, other.address) || !Objects.equals(this.deviceProductInfo, other.deviceProductInfo) || this.ownerUid != other.ownerUid || !Objects.equals(this.ownerPackageName, other.ownerPackageName)) {
            return diff | 2;
        }
        return diff;
    }

    public int hashCode() {
        return 0;
    }

    public void copyFrom(DisplayDeviceInfo other) {
        this.name = other.name;
        this.uniqueId = other.uniqueId;
        this.width = other.width;
        this.height = other.height;
        this.modeId = other.modeId;
        this.defaultModeId = other.defaultModeId;
        this.supportedModes = other.supportedModes;
        this.colorMode = other.colorMode;
        this.supportedColorModes = other.supportedColorModes;
        this.hdrCapabilities = other.hdrCapabilities;
        this.allmSupported = other.allmSupported;
        this.gameContentTypeSupported = other.gameContentTypeSupported;
        this.densityDpi = other.densityDpi;
        this.xDpi = other.xDpi;
        this.yDpi = other.yDpi;
        this.appVsyncOffsetNanos = other.appVsyncOffsetNanos;
        this.presentationDeadlineNanos = other.presentationDeadlineNanos;
        this.flags = other.flags;
        this.displayCutout = other.displayCutout;
        this.touch = other.touch;
        this.rotation = other.rotation;
        this.type = other.type;
        this.address = other.address;
        this.deviceProductInfo = other.deviceProductInfo;
        this.state = other.state;
        this.ownerUid = other.ownerUid;
        this.ownerPackageName = other.ownerPackageName;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DisplayDeviceInfo{\"");
        sb.append(this.name);
        sb.append("\": uniqueId=\"");
        sb.append(this.uniqueId);
        sb.append("\", ");
        sb.append(this.width);
        sb.append(" x ");
        sb.append(this.height);
        sb.append(", modeId ");
        sb.append(this.modeId);
        sb.append(", defaultModeId ");
        sb.append(this.defaultModeId);
        sb.append(", supportedModes ");
        sb.append(Arrays.toString(this.supportedModes));
        sb.append(", colorMode ");
        sb.append(this.colorMode);
        sb.append(", supportedColorModes ");
        sb.append(Arrays.toString(this.supportedColorModes));
        sb.append(", HdrCapabilities ");
        sb.append(this.hdrCapabilities);
        sb.append(", allmSupported ");
        sb.append(this.allmSupported);
        sb.append(", gameContentTypeSupported ");
        sb.append(this.gameContentTypeSupported);
        sb.append(", density ");
        sb.append(this.densityDpi);
        sb.append(", ");
        sb.append(this.xDpi);
        sb.append(" x ");
        sb.append(this.yDpi);
        sb.append(" dpi");
        sb.append(", appVsyncOff ");
        sb.append(this.appVsyncOffsetNanos);
        sb.append(", presDeadline ");
        sb.append(this.presentationDeadlineNanos);
        if (this.displayCutout != null) {
            sb.append(", cutout ");
            sb.append(this.displayCutout);
        }
        sb.append(", touch ");
        sb.append(touchToString(this.touch));
        sb.append(", rotation ");
        sb.append(this.rotation);
        sb.append(", type ");
        sb.append(Display.typeToString(this.type));
        if (this.address != null) {
            sb.append(", address ");
            sb.append(this.address);
        }
        sb.append(", deviceProductInfo ");
        sb.append(this.deviceProductInfo);
        sb.append(", state ");
        sb.append(Display.stateToString(this.state));
        if (this.ownerUid != 0 || this.ownerPackageName != null) {
            sb.append(", owner ");
            sb.append(this.ownerPackageName);
            sb.append(" (uid ");
            sb.append(this.ownerUid);
            sb.append(")");
        }
        sb.append(flagsToString(this.flags));
        sb.append("}");
        return sb.toString();
    }

    private static String touchToString(int touch) {
        if (touch != 0) {
            if (touch != 1) {
                if (touch != 2) {
                    if (touch == 3) {
                        return "VIRTUAL";
                    }
                    return Integer.toString(touch);
                }
                return "EXTERNAL";
            }
            return "INTERNAL";
        }
        return "NONE";
    }

    private static String flagsToString(int flags) {
        StringBuilder msg = new StringBuilder();
        if ((flags & 1) != 0) {
            msg.append(", FLAG_DEFAULT_DISPLAY");
        }
        if ((flags & 2) != 0) {
            msg.append(", FLAG_ROTATES_WITH_CONTENT");
        }
        if ((flags & 4) != 0) {
            msg.append(", FLAG_SECURE");
        }
        if ((flags & 8) != 0) {
            msg.append(", FLAG_SUPPORTS_PROTECTED_BUFFERS");
        }
        if ((flags & 16) != 0) {
            msg.append(", FLAG_PRIVATE");
        }
        if ((flags & 32) != 0) {
            msg.append(", FLAG_NEVER_BLANK");
        }
        if ((flags & 64) != 0) {
            msg.append(", FLAG_PRESENTATION");
        }
        if ((flags & 128) != 0) {
            msg.append(", FLAG_OWN_CONTENT_ONLY");
        }
        if ((flags & 256) != 0) {
            msg.append(", FLAG_ROUND");
        }
        if ((flags & 512) != 0) {
            msg.append(", FLAG_CAN_SHOW_WITH_INSECURE_KEYGUARD");
        }
        if ((flags & 2048) != 0) {
            msg.append(", FLAG_MASK_DISPLAY_CUTOUT");
        }
        return msg.toString();
    }
}