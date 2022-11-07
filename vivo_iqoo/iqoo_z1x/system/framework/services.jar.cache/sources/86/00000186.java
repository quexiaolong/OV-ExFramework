package android.hardware.usb.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

/* loaded from: classes.dex */
public final class PortStatus {
    public String portName = new String();
    public int currentDataRole = 0;
    public int currentPowerRole = 0;
    public int currentMode = 0;
    public boolean canChangeMode = false;
    public boolean canChangeDataRole = false;
    public boolean canChangePowerRole = false;
    public int supportedModes = 0;

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != PortStatus.class) {
            return false;
        }
        PortStatus other = (PortStatus) otherObject;
        if (HidlSupport.deepEquals(this.portName, other.portName) && this.currentDataRole == other.currentDataRole && this.currentPowerRole == other.currentPowerRole && this.currentMode == other.currentMode && this.canChangeMode == other.canChangeMode && this.canChangeDataRole == other.canChangeDataRole && this.canChangePowerRole == other.canChangePowerRole && this.supportedModes == other.supportedModes) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.portName)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.currentDataRole))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.currentPowerRole))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.currentMode))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.canChangeMode))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.canChangeDataRole))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.canChangePowerRole))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.supportedModes))));
    }

    public final String toString() {
        return "{.portName = " + this.portName + ", .currentDataRole = " + PortDataRole.toString(this.currentDataRole) + ", .currentPowerRole = " + PortPowerRole.toString(this.currentPowerRole) + ", .currentMode = " + PortMode.toString(this.currentMode) + ", .canChangeMode = " + this.canChangeMode + ", .canChangeDataRole = " + this.canChangeDataRole + ", .canChangePowerRole = " + this.canChangePowerRole + ", .supportedModes = " + PortMode.toString(this.supportedModes) + "}";
    }

    public final void readFromParcel(HwParcel parcel) {
        HwBlob blob = parcel.readBuffer(40L);
        readEmbeddedFromParcel(parcel, blob, 0L);
    }

    public static final ArrayList<PortStatus> readVectorFromParcel(HwParcel parcel) {
        ArrayList<PortStatus> _hidl_vec = new ArrayList<>();
        HwBlob _hidl_blob = parcel.readBuffer(16L);
        int _hidl_vec_size = _hidl_blob.getInt32(8L);
        HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 40, _hidl_blob.handle(), 0L, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            PortStatus _hidl_vec_element = new PortStatus();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 40);
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        String string = _hidl_blob.getString(_hidl_offset + 0);
        this.portName = string;
        parcel.readEmbeddedBuffer(string.getBytes().length + 1, _hidl_blob.handle(), _hidl_offset + 0 + 0, false);
        this.currentDataRole = _hidl_blob.getInt32(16 + _hidl_offset);
        this.currentPowerRole = _hidl_blob.getInt32(20 + _hidl_offset);
        this.currentMode = _hidl_blob.getInt32(24 + _hidl_offset);
        this.canChangeMode = _hidl_blob.getBool(28 + _hidl_offset);
        this.canChangeDataRole = _hidl_blob.getBool(29 + _hidl_offset);
        this.canChangePowerRole = _hidl_blob.getBool(30 + _hidl_offset);
        this.supportedModes = _hidl_blob.getInt32(32 + _hidl_offset);
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(40);
        writeEmbeddedToBlob(_hidl_blob, 0L);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<PortStatus> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8L, _hidl_vec_size);
        _hidl_blob.putBool(12L, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 40);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            _hidl_vec.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, _hidl_index_0 * 40);
        }
        _hidl_blob.putBlob(0L, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        _hidl_blob.putString(0 + _hidl_offset, this.portName);
        _hidl_blob.putInt32(16 + _hidl_offset, this.currentDataRole);
        _hidl_blob.putInt32(20 + _hidl_offset, this.currentPowerRole);
        _hidl_blob.putInt32(24 + _hidl_offset, this.currentMode);
        _hidl_blob.putBool(28 + _hidl_offset, this.canChangeMode);
        _hidl_blob.putBool(29 + _hidl_offset, this.canChangeDataRole);
        _hidl_blob.putBool(30 + _hidl_offset, this.canChangePowerRole);
        _hidl_blob.putInt32(32 + _hidl_offset, this.supportedModes);
    }
}