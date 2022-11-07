package com.android.server.usb.descriptors;

import android.hardware.usb.UsbDeviceConnection;
import com.android.server.usb.descriptors.report.UsbStrings;

/* loaded from: classes2.dex */
public final class UsbBinaryParser {
    private static final boolean LOGGING = false;
    private static final String TAG = "UsbBinaryParser";

    private void dumpDescriptor(ByteStream stream, int length, byte type, StringBuilder builder) {
        builder.append("<p>");
        builder.append("<b> l: " + length + " t:0x" + Integer.toHexString(type) + " " + UsbStrings.getDescriptorName(type) + "</b><br>");
        for (int index = 2; index < length; index++) {
            builder.append("0x" + Integer.toHexString(stream.getByte() & 255) + " ");
        }
        builder.append("</p>");
    }

    public void parseDescriptors(UsbDeviceConnection connection, byte[] descriptors, StringBuilder builder) {
        builder.append("<tt>");
        ByteStream stream = new ByteStream(descriptors);
        while (stream.available() > 0) {
            int length = stream.getByte() & 255;
            byte type = stream.getByte();
            dumpDescriptor(stream, length, type, builder);
        }
        builder.append("</tt>");
    }
}