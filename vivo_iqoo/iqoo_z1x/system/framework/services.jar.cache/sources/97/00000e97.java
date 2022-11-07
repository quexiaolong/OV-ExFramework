package com.android.server.hdmi;

import android.hardware.tv.cec.V1_0.CecMessageType;
import java.util.Arrays;
import java.util.Objects;
import libcore.util.EmptyArray;

/* loaded from: classes.dex */
public final class HdmiCecMessage {
    public static final byte[] EMPTY_PARAM = EmptyArray.BYTE;
    private final int mDestination;
    private final int mOpcode;
    private final byte[] mParams;
    private final int mSource;

    public HdmiCecMessage(int source, int destination, int opcode, byte[] params) {
        this.mSource = source;
        this.mDestination = destination;
        this.mOpcode = opcode & 255;
        this.mParams = Arrays.copyOf(params, params.length);
    }

    public boolean equals(Object message) {
        if (message instanceof HdmiCecMessage) {
            HdmiCecMessage that = (HdmiCecMessage) message;
            return this.mSource == that.getSource() && this.mDestination == that.getDestination() && this.mOpcode == that.getOpcode() && Arrays.equals(this.mParams, that.getParams());
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mSource), Integer.valueOf(this.mDestination), Integer.valueOf(this.mOpcode), Integer.valueOf(Arrays.hashCode(this.mParams)));
    }

    public int getSource() {
        return this.mSource;
    }

    public int getDestination() {
        return this.mDestination;
    }

    public int getOpcode() {
        return this.mOpcode;
    }

    public byte[] getParams() {
        return this.mParams;
    }

    public String toString() {
        byte[] bArr;
        StringBuffer s = new StringBuffer();
        s.append(String.format("<%s> %X%X:%02X", opcodeToString(this.mOpcode), Integer.valueOf(this.mSource), Integer.valueOf(this.mDestination), Integer.valueOf(this.mOpcode)));
        if (this.mParams.length > 0) {
            if (filterMessageParameters(this.mOpcode)) {
                s.append(String.format(" <Redacted len=%d>", Integer.valueOf(this.mParams.length)));
            } else {
                for (byte data : this.mParams) {
                    s.append(String.format(":%02X", Byte.valueOf(data)));
                }
            }
        }
        return s.toString();
    }

    private static String opcodeToString(int opcode) {
        if (opcode != 0) {
            if (opcode != 26) {
                if (opcode != 27) {
                    if (opcode != 125) {
                        if (opcode != 126) {
                            if (opcode != 153) {
                                if (opcode != 154) {
                                    switch (opcode) {
                                        case 0:
                                            return "Feature Abort";
                                        case 13:
                                            return "Text View On";
                                        case 15:
                                            return "Record Tv Screen";
                                        case 100:
                                            return "Set Osd String";
                                        case 103:
                                            return "Set Timer Program Title";
                                        case CecMessageType.REPORT_AUDIO_STATUS /* 122 */:
                                            return "Report Audio Status";
                                        case CecMessageType.VENDOR_COMMAND /* 137 */:
                                            return "Vendor Command";
                                        case CecMessageType.VENDOR_REMOTE_BUTTON_DOWN /* 138 */:
                                            return "Vendor Remote Button Down";
                                        case CecMessageType.VENDOR_REMOTE_BUTTON_UP /* 139 */:
                                            return "Vendor Remote Button Up";
                                        case CecMessageType.GIVE_DEVICE_VENDOR_ID /* 140 */:
                                            return "Give Device Vendor Id";
                                        case CecMessageType.MENU_REQUEST /* 141 */:
                                            return "Menu Request";
                                        case CecMessageType.MENU_STATUS /* 142 */:
                                            return "Menu Status";
                                        case CecMessageType.GIVE_DEVICE_POWER_STATUS /* 143 */:
                                            return "Give Device Power Status";
                                        case 144:
                                            return "Report Power Status";
                                        case 145:
                                            return "Get Menu Language";
                                        case CecMessageType.SELECT_ANALOG_SERVICE /* 146 */:
                                            return "Select Analog Service";
                                        case CecMessageType.SELECT_DIGITAL_SERVICE /* 147 */:
                                            return "Select Digital Service";
                                        case CecMessageType.SET_DIGITAL_TIMER /* 151 */:
                                            return "Set Digital Timer";
                                        case CecMessageType.INITIATE_ARC /* 192 */:
                                            return "Initiate ARC";
                                        case 193:
                                            return "Report ARC Initiated";
                                        case 194:
                                            return "Report ARC Terminated";
                                        case 195:
                                            return "Request ARC Initiation";
                                        case CecMessageType.REQUEST_ARC_TERMINATION /* 196 */:
                                            return "Request ARC Termination";
                                        case CecMessageType.TERMINATE_ARC /* 197 */:
                                            return "Terminate ARC";
                                        case 248:
                                            return "Cdc Message";
                                        case 255:
                                            return "Abort";
                                        default:
                                            switch (opcode) {
                                                case 4:
                                                    return "Image View On";
                                                case 5:
                                                    return "Tuner Step Increment";
                                                case 6:
                                                    return "Tuner Step Decrement";
                                                case 7:
                                                    return "Tuner Device Status";
                                                case 8:
                                                    return "Give Tuner Device Status";
                                                case 9:
                                                    return "Record On";
                                                case 10:
                                                    return "Record Status";
                                                case 11:
                                                    return "Record Off";
                                                default:
                                                    switch (opcode) {
                                                        case 50:
                                                            return "Set Menu Language";
                                                        case 51:
                                                            return "Clear Analog Timer";
                                                        case 52:
                                                            return "Set Analog Timer";
                                                        case 53:
                                                            return "Timer Status";
                                                        case 54:
                                                            return "Standby";
                                                        default:
                                                            switch (opcode) {
                                                                case 65:
                                                                    return "Play";
                                                                case 66:
                                                                    return "Deck Control";
                                                                case 67:
                                                                    return "Timer Cleared Status";
                                                                case 68:
                                                                    return "User Control Pressed";
                                                                case 69:
                                                                    return "User Control Release";
                                                                case 70:
                                                                    return "Give Osd Name";
                                                                case 71:
                                                                    return "Set Osd Name";
                                                                default:
                                                                    switch (opcode) {
                                                                        case 112:
                                                                            return "System Audio Mode Request";
                                                                        case 113:
                                                                            return "Give Audio Status";
                                                                        case 114:
                                                                            return "Set System Audio Mode";
                                                                        default:
                                                                            switch (opcode) {
                                                                                case 128:
                                                                                    return "Routing Change";
                                                                                case CecMessageType.ROUTING_INFORMATION /* 129 */:
                                                                                    return "Routing Information";
                                                                                case 130:
                                                                                    return "Active Source";
                                                                                case CecMessageType.GIVE_PHYSICAL_ADDRESS /* 131 */:
                                                                                    return "Give Physical Address";
                                                                                case CecMessageType.REPORT_PHYSICAL_ADDRESS /* 132 */:
                                                                                    return "Report Physical Address";
                                                                                case CecMessageType.REQUEST_ACTIVE_SOURCE /* 133 */:
                                                                                    return "Request Active Source";
                                                                                case CecMessageType.SET_STREAM_PATH /* 134 */:
                                                                                    return "Set Stream Path";
                                                                                case CecMessageType.DEVICE_VENDOR_ID /* 135 */:
                                                                                    return "Device Vendor Id";
                                                                                default:
                                                                                    switch (opcode) {
                                                                                        case CecMessageType.INACTIVE_SOURCE /* 157 */:
                                                                                            return "InActive Source";
                                                                                        case CecMessageType.CEC_VERSION /* 158 */:
                                                                                            return "Cec Version";
                                                                                        case CecMessageType.GET_CEC_VERSION /* 159 */:
                                                                                            return "Get Cec Version";
                                                                                        case 160:
                                                                                            return "Vendor Command With Id";
                                                                                        case CecMessageType.CLEAR_EXTERNAL_TIMER /* 161 */:
                                                                                            return "Clear External Timer";
                                                                                        case CecMessageType.SET_EXTERNAL_TIMER /* 162 */:
                                                                                            return "Set External Timer";
                                                                                        case CecMessageType.REPORT_SHORT_AUDIO_DESCRIPTOR /* 163 */:
                                                                                            return "Report Short Audio Descriptor";
                                                                                        case CecMessageType.REQUEST_SHORT_AUDIO_DESCRIPTOR /* 164 */:
                                                                                            return "Request Short Audio Descriptor";
                                                                                        default:
                                                                                            return String.format("Opcode: %02X", Integer.valueOf(opcode));
                                                                                    }
                                                                            }
                                                                    }
                                                            }
                                                    }
                                            }
                                    }
                                }
                                return "Set Audio Rate";
                            }
                            return "Clear Digital Timer";
                        }
                        return "System Audio Mode Status";
                    }
                    return "Give System Audio Mode Status";
                }
                return "Deck Status";
            }
            return "Give Deck Status";
        }
        return "Feature Abort";
    }

    private static boolean filterMessageParameters(int opcode) {
        if (opcode == 68 || opcode == 69 || opcode == 71 || opcode == 100 || opcode == 160) {
            return true;
        }
        switch (opcode) {
            case CecMessageType.VENDOR_COMMAND /* 137 */:
            case CecMessageType.VENDOR_REMOTE_BUTTON_DOWN /* 138 */:
            case CecMessageType.VENDOR_REMOTE_BUTTON_UP /* 139 */:
                return true;
            default:
                return false;
        }
    }
}