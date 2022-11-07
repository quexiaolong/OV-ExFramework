package vendor.vivo.hardware.camera.vif3ainfotransmitter.V1_0;

import android.os.HwBlob;
import android.os.HwParcel;
import android.os.NativeHandle;
import java.util.ArrayList;

/* loaded from: classes.dex */
public final class TransmitBuffer {
    public int buffer_size = 0;
    public int buffer_fd = 0;
    public int buffer_index = 0;
    public NativeHandle buffer_hdl = new NativeHandle();

    public final String toString() {
        return "{.buffer_size = " + this.buffer_size + ", .buffer_fd = " + this.buffer_fd + ", .buffer_index = " + this.buffer_index + ", .buffer_hdl = " + this.buffer_hdl + "}";
    }

    public final void readFromParcel(HwParcel parcel) {
        HwBlob blob = parcel.readBuffer(32L);
        readEmbeddedFromParcel(parcel, blob, 0L);
    }

    public static final ArrayList<TransmitBuffer> readVectorFromParcel(HwParcel parcel) {
        ArrayList<TransmitBuffer> _hidl_vec = new ArrayList<>();
        HwBlob _hidl_blob = parcel.readBuffer(16L);
        int _hidl_vec_size = _hidl_blob.getInt32(8L);
        HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 32, _hidl_blob.handle(), 0L, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            TransmitBuffer _hidl_vec_element = new TransmitBuffer();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 32);
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        this.buffer_size = _hidl_blob.getInt32(_hidl_offset + 0);
        this.buffer_fd = _hidl_blob.getInt32(4 + _hidl_offset);
        this.buffer_index = _hidl_blob.getInt32(8 + _hidl_offset);
        this.buffer_hdl = parcel.readEmbeddedNativeHandle(_hidl_blob.handle(), 16 + _hidl_offset + 0);
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(32);
        writeEmbeddedToBlob(_hidl_blob, 0L);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<TransmitBuffer> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8L, _hidl_vec_size);
        _hidl_blob.putBool(12L, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 32);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            _hidl_vec.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, _hidl_index_0 * 32);
        }
        _hidl_blob.putBlob(0L, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        _hidl_blob.putInt32(0 + _hidl_offset, this.buffer_size);
        _hidl_blob.putInt32(4 + _hidl_offset, this.buffer_fd);
        _hidl_blob.putInt32(8 + _hidl_offset, this.buffer_index);
        _hidl_blob.putNativeHandle(16 + _hidl_offset, this.buffer_hdl);
    }
}