package vendor.vivo.hardware.camera.vivoreprocess.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

/* loaded from: classes.dex */
public final class ReprocessHalStreamConfiguration {
    public ArrayList<ReprocessHalStream> streams = new ArrayList<>();

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != ReprocessHalStreamConfiguration.class) {
            return false;
        }
        ReprocessHalStreamConfiguration other = (ReprocessHalStreamConfiguration) otherObject;
        if (HidlSupport.deepEquals(this.streams, other.streams)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.streams)));
    }

    public final String toString() {
        return "{.streams = " + this.streams + "}";
    }

    public final void readFromParcel(HwParcel parcel) {
        HwBlob blob = parcel.readBuffer(16L);
        readEmbeddedFromParcel(parcel, blob, 0L);
    }

    public static final ArrayList<ReprocessHalStreamConfiguration> readVectorFromParcel(HwParcel parcel) {
        ArrayList<ReprocessHalStreamConfiguration> _hidl_vec = new ArrayList<>();
        HwBlob _hidl_blob = parcel.readBuffer(16L);
        int _hidl_vec_size = _hidl_blob.getInt32(8L);
        HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 16, _hidl_blob.handle(), 0L, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            ReprocessHalStreamConfiguration _hidl_vec_element = new ReprocessHalStreamConfiguration();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 16);
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        int _hidl_vec_size = _hidl_blob.getInt32(_hidl_offset + 0 + 8);
        HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 32, _hidl_blob.handle(), _hidl_offset + 0 + 0, true);
        this.streams.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            ReprocessHalStream _hidl_vec_element = new ReprocessHalStream();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 32);
            this.streams.add(_hidl_vec_element);
        }
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(16);
        writeEmbeddedToBlob(_hidl_blob, 0L);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<ReprocessHalStreamConfiguration> _hidl_vec) {
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
        int _hidl_vec_size = this.streams.size();
        _hidl_blob.putInt32(_hidl_offset + 0 + 8, _hidl_vec_size);
        _hidl_blob.putBool(_hidl_offset + 0 + 12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 32);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            this.streams.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, _hidl_index_0 * 32);
        }
        _hidl_blob.putBlob(_hidl_offset + 0 + 0, childBlob);
    }
}