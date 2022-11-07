package android.hardware.audio.common.V2_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/* loaded from: classes.dex */
public final class Uuid {
    public int timeLow = 0;
    public short timeMid = 0;
    public short versionAndTimeHigh = 0;
    public short variantAndClockSeqHigh = 0;
    public byte[] node = new byte[6];

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != Uuid.class) {
            return false;
        }
        Uuid other = (Uuid) otherObject;
        if (this.timeLow == other.timeLow && this.timeMid == other.timeMid && this.versionAndTimeHigh == other.versionAndTimeHigh && this.variantAndClockSeqHigh == other.variantAndClockSeqHigh && HidlSupport.deepEquals(this.node, other.node)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.timeLow))), Integer.valueOf(HidlSupport.deepHashCode(Short.valueOf(this.timeMid))), Integer.valueOf(HidlSupport.deepHashCode(Short.valueOf(this.versionAndTimeHigh))), Integer.valueOf(HidlSupport.deepHashCode(Short.valueOf(this.variantAndClockSeqHigh))), Integer.valueOf(HidlSupport.deepHashCode(this.node)));
    }

    public final String toString() {
        return "{.timeLow = " + this.timeLow + ", .timeMid = " + ((int) this.timeMid) + ", .versionAndTimeHigh = " + ((int) this.versionAndTimeHigh) + ", .variantAndClockSeqHigh = " + ((int) this.variantAndClockSeqHigh) + ", .node = " + Arrays.toString(this.node) + "}";
    }

    public final void readFromParcel(HwParcel parcel) {
        HwBlob blob = parcel.readBuffer(16L);
        readEmbeddedFromParcel(parcel, blob, 0L);
    }

    public static final ArrayList<Uuid> readVectorFromParcel(HwParcel parcel) {
        ArrayList<Uuid> _hidl_vec = new ArrayList<>();
        HwBlob _hidl_blob = parcel.readBuffer(16L);
        int _hidl_vec_size = _hidl_blob.getInt32(8L);
        HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 16, _hidl_blob.handle(), 0L, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            Uuid _hidl_vec_element = new Uuid();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 16);
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        this.timeLow = _hidl_blob.getInt32(0 + _hidl_offset);
        this.timeMid = _hidl_blob.getInt16(4 + _hidl_offset);
        this.versionAndTimeHigh = _hidl_blob.getInt16(6 + _hidl_offset);
        this.variantAndClockSeqHigh = _hidl_blob.getInt16(8 + _hidl_offset);
        long _hidl_array_offset_0 = 10 + _hidl_offset;
        _hidl_blob.copyToInt8Array(_hidl_array_offset_0, this.node, 6);
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(16);
        writeEmbeddedToBlob(_hidl_blob, 0L);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<Uuid> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8L, _hidl_vec_size);
        _hidl_blob.putBool(12L, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 16);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            _hidl_vec.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, _hidl_index_0 * 16);
        }
        _hidl_blob.putBlob(0L, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        _hidl_blob.putInt32(0 + _hidl_offset, this.timeLow);
        _hidl_blob.putInt16(4 + _hidl_offset, this.timeMid);
        _hidl_blob.putInt16(6 + _hidl_offset, this.versionAndTimeHigh);
        _hidl_blob.putInt16(8 + _hidl_offset, this.variantAndClockSeqHigh);
        long _hidl_array_offset_0 = 10 + _hidl_offset;
        byte[] _hidl_array_item_0 = this.node;
        if (_hidl_array_item_0 == null || _hidl_array_item_0.length != 6) {
            throw new IllegalArgumentException("Array element is not of the expected length");
        }
        _hidl_blob.putInt8Array(_hidl_array_offset_0, _hidl_array_item_0);
    }
}