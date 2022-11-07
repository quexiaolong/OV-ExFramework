package vendor.vivo.hardware.camera.vivopostproc.V1_0;

import android.os.HwBlob;
import android.os.HwParcel;
import android.os.NativeHandle;
import java.util.ArrayList;

/* loaded from: classes.dex */
public final class PostProcImage {
    public int size = 0;
    public int format = 0;
    public int width = 0;
    public int height = 0;
    public int stride = 0;
    public int scanline = 0;
    public NativeHandle buffer = new NativeHandle();

    public final String toString() {
        return "{.size = " + this.size + ", .format = " + this.format + ", .width = " + this.width + ", .height = " + this.height + ", .stride = " + this.stride + ", .scanline = " + this.scanline + ", .buffer = " + this.buffer + "}";
    }

    public final void readFromParcel(HwParcel parcel) {
        HwBlob blob = parcel.readBuffer(40L);
        readEmbeddedFromParcel(parcel, blob, 0L);
    }

    public static final ArrayList<PostProcImage> readVectorFromParcel(HwParcel parcel) {
        ArrayList<PostProcImage> _hidl_vec = new ArrayList<>();
        HwBlob _hidl_blob = parcel.readBuffer(16L);
        int _hidl_vec_size = _hidl_blob.getInt32(8L);
        HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 40, _hidl_blob.handle(), 0L, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            PostProcImage _hidl_vec_element = new PostProcImage();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 40);
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        this.size = _hidl_blob.getInt32(_hidl_offset + 0);
        this.format = _hidl_blob.getInt32(4 + _hidl_offset);
        this.width = _hidl_blob.getInt32(8 + _hidl_offset);
        this.height = _hidl_blob.getInt32(12 + _hidl_offset);
        this.stride = _hidl_blob.getInt32(16 + _hidl_offset);
        this.scanline = _hidl_blob.getInt32(20 + _hidl_offset);
        this.buffer = parcel.readEmbeddedNativeHandle(_hidl_blob.handle(), 24 + _hidl_offset + 0);
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(40);
        writeEmbeddedToBlob(_hidl_blob, 0L);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<PostProcImage> _hidl_vec) {
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
        _hidl_blob.putInt32(0 + _hidl_offset, this.size);
        _hidl_blob.putInt32(4 + _hidl_offset, this.format);
        _hidl_blob.putInt32(8 + _hidl_offset, this.width);
        _hidl_blob.putInt32(12 + _hidl_offset, this.height);
        _hidl_blob.putInt32(16 + _hidl_offset, this.stride);
        _hidl_blob.putInt32(20 + _hidl_offset, this.scanline);
        _hidl_blob.putNativeHandle(24 + _hidl_offset, this.buffer);
    }
}