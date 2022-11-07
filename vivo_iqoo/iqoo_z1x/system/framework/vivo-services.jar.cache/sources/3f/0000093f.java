package vendor.vivo.hardware.camera.vivoreprocess.V1_0;

import android.os.HwBlob;
import android.os.HwParcel;
import android.os.NativeHandle;
import java.util.ArrayList;

/* loaded from: classes.dex */
public final class ReprocessStreamBuffer {
    public int streamId = 0;
    public long bufferId = 0;
    public NativeHandle buffer = new NativeHandle();
    public int status = 0;

    public final String toString() {
        return "{.streamId = " + this.streamId + ", .bufferId = " + this.bufferId + ", .buffer = " + this.buffer + ", .status = " + ReprocessBufferStatus.toString(this.status) + "}";
    }

    public final void readFromParcel(HwParcel parcel) {
        HwBlob blob = parcel.readBuffer(40L);
        readEmbeddedFromParcel(parcel, blob, 0L);
    }

    public static final ArrayList<ReprocessStreamBuffer> readVectorFromParcel(HwParcel parcel) {
        ArrayList<ReprocessStreamBuffer> _hidl_vec = new ArrayList<>();
        HwBlob _hidl_blob = parcel.readBuffer(16L);
        int _hidl_vec_size = _hidl_blob.getInt32(8L);
        HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 40, _hidl_blob.handle(), 0L, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            ReprocessStreamBuffer _hidl_vec_element = new ReprocessStreamBuffer();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 40);
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        this.streamId = _hidl_blob.getInt32(_hidl_offset + 0);
        this.bufferId = _hidl_blob.getInt64(8 + _hidl_offset);
        this.buffer = parcel.readEmbeddedNativeHandle(_hidl_blob.handle(), 16 + _hidl_offset + 0);
        this.status = _hidl_blob.getInt32(32 + _hidl_offset);
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(40);
        writeEmbeddedToBlob(_hidl_blob, 0L);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<ReprocessStreamBuffer> _hidl_vec) {
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
        _hidl_blob.putInt32(0 + _hidl_offset, this.streamId);
        _hidl_blob.putInt64(8 + _hidl_offset, this.bufferId);
        _hidl_blob.putNativeHandle(16 + _hidl_offset, this.buffer);
        _hidl_blob.putInt32(32 + _hidl_offset, this.status);
    }
}