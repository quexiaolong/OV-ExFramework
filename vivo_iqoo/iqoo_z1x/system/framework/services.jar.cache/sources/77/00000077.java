package android.hardware.broadcastradio.V2_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

/* loaded from: classes.dex */
public final class AmFmRegionConfig {
    public byte fmDeemphasis;
    public byte fmRds;
    public ArrayList<AmFmBandRange> ranges = new ArrayList<>();

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != AmFmRegionConfig.class) {
            return false;
        }
        AmFmRegionConfig other = (AmFmRegionConfig) otherObject;
        if (HidlSupport.deepEquals(this.ranges, other.ranges) && HidlSupport.deepEquals(Byte.valueOf(this.fmDeemphasis), Byte.valueOf(other.fmDeemphasis)) && HidlSupport.deepEquals(Byte.valueOf(this.fmRds), Byte.valueOf(other.fmRds))) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.ranges)), Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.fmDeemphasis))), Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.fmRds))));
    }

    public final String toString() {
        return "{.ranges = " + this.ranges + ", .fmDeemphasis = " + Deemphasis.dumpBitfield(this.fmDeemphasis) + ", .fmRds = " + Rds.dumpBitfield(this.fmRds) + "}";
    }

    public final void readFromParcel(HwParcel parcel) {
        HwBlob blob = parcel.readBuffer(24L);
        readEmbeddedFromParcel(parcel, blob, 0L);
    }

    public static final ArrayList<AmFmRegionConfig> readVectorFromParcel(HwParcel parcel) {
        ArrayList<AmFmRegionConfig> _hidl_vec = new ArrayList<>();
        HwBlob _hidl_blob = parcel.readBuffer(16L);
        int _hidl_vec_size = _hidl_blob.getInt32(8L);
        HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 24, _hidl_blob.handle(), 0L, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            AmFmRegionConfig _hidl_vec_element = new AmFmRegionConfig();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 24);
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        int _hidl_vec_size = _hidl_blob.getInt32(_hidl_offset + 0 + 8);
        HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 16, _hidl_blob.handle(), _hidl_offset + 0 + 0, true);
        this.ranges.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            AmFmBandRange _hidl_vec_element = new AmFmBandRange();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 16);
            this.ranges.add(_hidl_vec_element);
        }
        this.fmDeemphasis = _hidl_blob.getInt8(_hidl_offset + 16);
        this.fmRds = _hidl_blob.getInt8(_hidl_offset + 17);
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(24);
        writeEmbeddedToBlob(_hidl_blob, 0L);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<AmFmRegionConfig> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8L, _hidl_vec_size);
        _hidl_blob.putBool(12L, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 24);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            _hidl_vec.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, _hidl_index_0 * 24);
        }
        _hidl_blob.putBlob(0L, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        int _hidl_vec_size = this.ranges.size();
        _hidl_blob.putInt32(_hidl_offset + 0 + 8, _hidl_vec_size);
        _hidl_blob.putBool(_hidl_offset + 0 + 12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 16);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            this.ranges.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, _hidl_index_0 * 16);
        }
        _hidl_blob.putBlob(_hidl_offset + 0 + 0, childBlob);
        _hidl_blob.putInt8(16 + _hidl_offset, this.fmDeemphasis);
        _hidl_blob.putInt8(17 + _hidl_offset, this.fmRds);
    }
}