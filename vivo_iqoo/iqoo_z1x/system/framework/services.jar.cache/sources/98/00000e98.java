package com.android.server.hdmi;

import android.hardware.tv.cec.V1_0.CecMessageType;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/* loaded from: classes.dex */
public class HdmiCecMessageBuilder {
    private static final int OSD_NAME_MAX_LENGTH = 13;

    private HdmiCecMessageBuilder() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage of(int src, int dest, byte[] body) {
        byte opcode = body[0];
        byte[] params = Arrays.copyOfRange(body, 1, body.length);
        return new HdmiCecMessage(src, dest, opcode, params);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildFeatureAbortCommand(int src, int dest, int originalOpcode, int reason) {
        byte[] params = {(byte) (originalOpcode & 255), (byte) (reason & 255)};
        return buildCommand(src, dest, 0, params);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildGivePhysicalAddress(int src, int dest) {
        return buildCommand(src, dest, CecMessageType.GIVE_PHYSICAL_ADDRESS);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildGiveOsdNameCommand(int src, int dest) {
        return buildCommand(src, dest, 70);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildGiveDeviceVendorIdCommand(int src, int dest) {
        return buildCommand(src, dest, CecMessageType.GIVE_DEVICE_VENDOR_ID);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildSetMenuLanguageCommand(int src, String language) {
        if (language.length() != 3) {
            return null;
        }
        String normalized = language.toLowerCase();
        byte[] params = {(byte) (normalized.charAt(0) & 255), (byte) (normalized.charAt(1) & 255), (byte) (normalized.charAt(2) & 255)};
        return buildCommand(src, 15, 50, params);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildSetOsdNameCommand(int src, int dest, String name) {
        int length = Math.min(name.length(), 13);
        try {
            byte[] params = name.substring(0, length).getBytes("US-ASCII");
            return buildCommand(src, dest, 71, params);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildReportPhysicalAddressCommand(int src, int address, int deviceType) {
        byte[] params = {(byte) ((address >> 8) & 255), (byte) (address & 255), (byte) (deviceType & 255)};
        return buildCommand(src, 15, CecMessageType.REPORT_PHYSICAL_ADDRESS, params);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildDeviceVendorIdCommand(int src, int vendorId) {
        byte[] params = {(byte) ((vendorId >> 16) & 255), (byte) ((vendorId >> 8) & 255), (byte) (vendorId & 255)};
        return buildCommand(src, 15, CecMessageType.DEVICE_VENDOR_ID, params);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildCecVersion(int src, int dest, int version) {
        byte[] params = {(byte) (version & 255)};
        return buildCommand(src, dest, CecMessageType.CEC_VERSION, params);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildRequestArcInitiation(int src, int dest) {
        return buildCommand(src, dest, 195);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildInitiateArc(int src, int dest) {
        return buildCommand(src, dest, CecMessageType.INITIATE_ARC);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildTerminateArc(int src, int dest) {
        return buildCommand(src, dest, CecMessageType.TERMINATE_ARC);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildRequestArcTermination(int src, int dest) {
        return buildCommand(src, dest, CecMessageType.REQUEST_ARC_TERMINATION);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildReportArcInitiated(int src, int dest) {
        return buildCommand(src, dest, 193);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildReportArcTerminated(int src, int dest) {
        return buildCommand(src, dest, 194);
    }

    static HdmiCecMessage buildRequestShortAudioDescriptor(int src, int dest, int[] audioFormats) {
        byte[] params = new byte[Math.min(audioFormats.length, 4)];
        for (int i = 0; i < params.length; i++) {
            params[i] = (byte) (audioFormats[i] & 255);
        }
        return buildCommand(src, dest, CecMessageType.REQUEST_SHORT_AUDIO_DESCRIPTOR, params);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildTextViewOn(int src, int dest) {
        return buildCommand(src, dest, 13);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildRequestActiveSource(int src) {
        return buildCommand(src, 15, CecMessageType.REQUEST_ACTIVE_SOURCE);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildActiveSource(int src, int physicalAddress) {
        return buildCommand(src, 15, 130, physicalAddressToParam(physicalAddress));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildInactiveSource(int src, int physicalAddress) {
        return buildCommand(src, 0, CecMessageType.INACTIVE_SOURCE, physicalAddressToParam(physicalAddress));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildSetStreamPath(int src, int streamPath) {
        return buildCommand(src, 15, CecMessageType.SET_STREAM_PATH, physicalAddressToParam(streamPath));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildRoutingChange(int src, int oldPath, int newPath) {
        byte[] param = {(byte) ((oldPath >> 8) & 255), (byte) (oldPath & 255), (byte) ((newPath >> 8) & 255), (byte) (newPath & 255)};
        return buildCommand(src, 15, 128, param);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildRoutingInformation(int src, int physicalAddress) {
        return buildCommand(src, 15, CecMessageType.ROUTING_INFORMATION, physicalAddressToParam(physicalAddress));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildGiveDevicePowerStatus(int src, int dest) {
        return buildCommand(src, dest, CecMessageType.GIVE_DEVICE_POWER_STATUS);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildReportPowerStatus(int src, int dest, int powerStatus) {
        byte[] param = {(byte) (powerStatus & 255)};
        return buildCommand(src, dest, 144, param);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildReportMenuStatus(int src, int dest, int menuStatus) {
        byte[] param = {(byte) (menuStatus & 255)};
        return buildCommand(src, dest, CecMessageType.MENU_STATUS, param);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildSystemAudioModeRequest(int src, int avr, int avrPhysicalAddress, boolean enableSystemAudio) {
        if (enableSystemAudio) {
            return buildCommand(src, avr, 112, physicalAddressToParam(avrPhysicalAddress));
        }
        return buildCommand(src, avr, 112);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildSetSystemAudioMode(int src, int des, boolean systemAudioStatus) {
        return buildCommandWithBooleanParam(src, des, 114, systemAudioStatus);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildReportSystemAudioMode(int src, int des, boolean systemAudioStatus) {
        return buildCommandWithBooleanParam(src, des, CecMessageType.SYSTEM_AUDIO_MODE_STATUS, systemAudioStatus);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildReportShortAudioDescriptor(int src, int des, byte[] sadBytes) {
        return buildCommand(src, des, CecMessageType.REPORT_SHORT_AUDIO_DESCRIPTOR, sadBytes);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildGiveAudioStatus(int src, int dest) {
        return buildCommand(src, dest, 113);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildReportAudioStatus(int src, int dest, int volume, boolean mute) {
        byte status = (byte) (((byte) (mute ? 128 : 0)) | (((byte) volume) & Byte.MAX_VALUE));
        byte[] params = {status};
        return buildCommand(src, dest, CecMessageType.REPORT_AUDIO_STATUS, params);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildUserControlPressed(int src, int dest, int uiCommand) {
        return buildUserControlPressed(src, dest, new byte[]{(byte) (uiCommand & 255)});
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildUserControlPressed(int src, int dest, byte[] commandParam) {
        return buildCommand(src, dest, 68, commandParam);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildUserControlReleased(int src, int dest) {
        return buildCommand(src, dest, 69);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildGiveSystemAudioModeStatus(int src, int dest) {
        return buildCommand(src, dest, 125);
    }

    public static HdmiCecMessage buildStandby(int src, int dest) {
        return buildCommand(src, dest, 54);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildVendorCommand(int src, int dest, byte[] params) {
        return buildCommand(src, dest, CecMessageType.VENDOR_COMMAND, params);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildVendorCommandWithId(int src, int dest, int vendorId, byte[] operands) {
        byte[] params = new byte[operands.length + 3];
        params[0] = (byte) ((vendorId >> 16) & 255);
        params[1] = (byte) ((vendorId >> 8) & 255);
        params[2] = (byte) (vendorId & 255);
        System.arraycopy(operands, 0, params, 3, operands.length);
        return buildCommand(src, dest, 160, params);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildRecordOn(int src, int dest, byte[] params) {
        return buildCommand(src, dest, 9, params);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildRecordOff(int src, int dest) {
        return buildCommand(src, dest, 11);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildSetDigitalTimer(int src, int dest, byte[] params) {
        return buildCommand(src, dest, CecMessageType.SET_DIGITAL_TIMER, params);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildSetAnalogueTimer(int src, int dest, byte[] params) {
        return buildCommand(src, dest, 52, params);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildSetExternalTimer(int src, int dest, byte[] params) {
        return buildCommand(src, dest, CecMessageType.SET_EXTERNAL_TIMER, params);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildClearDigitalTimer(int src, int dest, byte[] params) {
        return buildCommand(src, dest, CecMessageType.CLEAR_DIGITAL_TIMER, params);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildClearAnalogueTimer(int src, int dest, byte[] params) {
        return buildCommand(src, dest, 51, params);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static HdmiCecMessage buildClearExternalTimer(int src, int dest, byte[] params) {
        return buildCommand(src, dest, CecMessageType.CLEAR_EXTERNAL_TIMER, params);
    }

    private static HdmiCecMessage buildCommand(int src, int dest, int opcode) {
        return new HdmiCecMessage(src, dest, opcode, HdmiCecMessage.EMPTY_PARAM);
    }

    private static HdmiCecMessage buildCommand(int src, int dest, int opcode, byte[] params) {
        return new HdmiCecMessage(src, dest, opcode, params);
    }

    private static HdmiCecMessage buildCommandWithBooleanParam(int src, int des, int opcode, boolean param) {
        byte[] params = {param ? (byte) 1 : (byte) 0};
        return buildCommand(src, des, opcode, params);
    }

    private static byte[] physicalAddressToParam(int physicalAddress) {
        return new byte[]{(byte) ((physicalAddress >> 8) & 255), (byte) (physicalAddress & 255)};
    }
}