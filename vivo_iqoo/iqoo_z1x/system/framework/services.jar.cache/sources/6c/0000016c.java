package android.hardware.tv.cec.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

/* loaded from: classes.dex */
public final class HdmiPortInfo {
    public int type = 0;
    public int portId = 0;
    public boolean cecSupported = false;
    public boolean arcSupported = false;
    public short physicalAddress = 0;

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != HdmiPortInfo.class) {
            return false;
        }
        HdmiPortInfo other = (HdmiPortInfo) otherObject;
        if (this.type == other.type && this.portId == other.portId && this.cecSupported == other.cecSupported && this.arcSupported == other.arcSupported && this.physicalAddress == other.physicalAddress) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.type))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.portId))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.cecSupported))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.arcSupported))), Integer.valueOf(HidlSupport.deepHashCode(Short.valueOf(this.physicalAddress))));
    }

    public final String toString() {
        return "{.type = " + HdmiPortType.toString(this.type) + ", .portId = " + this.portId + ", .cecSupported = " + this.cecSupported + ", .arcSupported = " + this.arcSupported + ", .physicalAddress = " + ((int) this.physicalAddress) + "}";
    }

    public final void readFromParcel(HwParcel parcel) {
        HwBlob blob = parcel.readBuffer(12L);
        readEmbeddedFromParcel(parcel, blob, 0L);
    }

    public static final ArrayList<HdmiPortInfo> readVectorFromParcel(HwParcel parcel) {
        ArrayList<HdmiPortInfo> _hidl_vec = new ArrayList<>();
        HwBlob _hidl_blob = parcel.readBuffer(16L);
        int _hidl_vec_size = _hidl_blob.getInt32(8L);
        HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 12, _hidl_blob.handle(), 0L, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            HdmiPortInfo _hidl_vec_element = new HdmiPortInfo();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 12);
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        this.type = _hidl_blob.getInt32(0 + _hidl_offset);
        this.portId = _hidl_blob.getInt32(4 + _hidl_offset);
        this.cecSupported = _hidl_blob.getBool(8 + _hidl_offset);
        this.arcSupported = _hidl_blob.getBool(9 + _hidl_offset);
        this.physicalAddress = _hidl_blob.getInt16(10 + _hidl_offset);
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(12);
        writeEmbeddedToBlob(_hidl_blob, 0L);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<HdmiPortInfo> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8L, _hidl_vec_size);
        _hidl_blob.putBool(12L, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 12);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            _hidl_vec.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, _hidl_index_0 * 12);
        }
        _hidl_blob.putBlob(0L, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        _hidl_blob.putInt32(0 + _hidl_offset, this.type);
        _hidl_blob.putInt32(4 + _hidl_offset, this.portId);
        _hidl_blob.putBool(8 + _hidl_offset, this.cecSupported);
        _hidl_blob.putBool(9 + _hidl_offset, this.arcSupported);
        _hidl_blob.putInt16(10 + _hidl_offset, this.physicalAddress);
    }
}