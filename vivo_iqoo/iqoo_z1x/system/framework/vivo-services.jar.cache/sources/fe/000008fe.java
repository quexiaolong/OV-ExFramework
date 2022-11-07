package vendor.vivo.hardware.camera.jpegencoder.V1_0;

import android.os.HwBlob;
import android.os.HwParcel;
import android.os.NativeHandle;
import java.util.ArrayList;

/* loaded from: classes.dex */
public final class RequestParam {
    public int frame_num = 0;
    public int width = 0;
    public int height = 0;
    public int stride = 0;
    public int scanline = 0;
    public int input_size = 0;
    public int output_size = 0;
    public NativeHandle input_buffer = new NativeHandle();
    public NativeHandle output_buffer = new NativeHandle();
    public boolean isRotation = false;

    public final String toString() {
        return "{.frame_num = " + this.frame_num + ", .width = " + this.width + ", .height = " + this.height + ", .stride = " + this.stride + ", .scanline = " + this.scanline + ", .input_size = " + this.input_size + ", .output_size = " + this.output_size + ", .input_buffer = " + this.input_buffer + ", .output_buffer = " + this.output_buffer + ", .isRotation = " + this.isRotation + "}";
    }

    public final void readFromParcel(HwParcel parcel) {
        HwBlob blob = parcel.readBuffer(72L);
        readEmbeddedFromParcel(parcel, blob, 0L);
    }

    public static final ArrayList<RequestParam> readVectorFromParcel(HwParcel parcel) {
        ArrayList<RequestParam> _hidl_vec = new ArrayList<>();
        HwBlob _hidl_blob = parcel.readBuffer(16L);
        int _hidl_vec_size = _hidl_blob.getInt32(8L);
        HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 72, _hidl_blob.handle(), 0L, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            RequestParam _hidl_vec_element = new RequestParam();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 72);
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        this.frame_num = _hidl_blob.getInt32(_hidl_offset + 0);
        this.width = _hidl_blob.getInt32(4 + _hidl_offset);
        this.height = _hidl_blob.getInt32(8 + _hidl_offset);
        this.stride = _hidl_blob.getInt32(12 + _hidl_offset);
        this.scanline = _hidl_blob.getInt32(16 + _hidl_offset);
        this.input_size = _hidl_blob.getInt32(20 + _hidl_offset);
        this.output_size = _hidl_blob.getInt32(24 + _hidl_offset);
        this.input_buffer = parcel.readEmbeddedNativeHandle(_hidl_blob.handle(), 32 + _hidl_offset + 0);
        this.output_buffer = parcel.readEmbeddedNativeHandle(_hidl_blob.handle(), 48 + _hidl_offset + 0);
        this.isRotation = _hidl_blob.getBool(64 + _hidl_offset);
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(72);
        writeEmbeddedToBlob(_hidl_blob, 0L);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<RequestParam> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8L, _hidl_vec_size);
        _hidl_blob.putBool(12L, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 72);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            _hidl_vec.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, _hidl_index_0 * 72);
        }
        _hidl_blob.putBlob(0L, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        _hidl_blob.putInt32(0 + _hidl_offset, this.frame_num);
        _hidl_blob.putInt32(4 + _hidl_offset, this.width);
        _hidl_blob.putInt32(8 + _hidl_offset, this.height);
        _hidl_blob.putInt32(12 + _hidl_offset, this.stride);
        _hidl_blob.putInt32(16 + _hidl_offset, this.scanline);
        _hidl_blob.putInt32(20 + _hidl_offset, this.input_size);
        _hidl_blob.putInt32(24 + _hidl_offset, this.output_size);
        _hidl_blob.putNativeHandle(32 + _hidl_offset, this.input_buffer);
        _hidl_blob.putNativeHandle(48 + _hidl_offset, this.output_buffer);
        _hidl_blob.putBool(64 + _hidl_offset, this.isRotation);
    }
}