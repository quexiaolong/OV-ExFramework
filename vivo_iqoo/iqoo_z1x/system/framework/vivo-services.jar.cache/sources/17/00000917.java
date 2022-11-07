package vendor.vivo.hardware.camera.vivopostproc.V1_0;

import android.hardware.graphics.common.V1_0.PixelFormat;
import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

/* loaded from: classes.dex */
public final class HalStream {
    public int streamId = 0;
    public int overrideFormat = 0;
    public long producerUsage = 0;
    public long consumerUsage = 0;
    public int maxBuffers = 0;

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != HalStream.class) {
            return false;
        }
        HalStream other = (HalStream) otherObject;
        if (this.streamId == other.streamId && this.overrideFormat == other.overrideFormat && this.producerUsage == other.producerUsage && this.consumerUsage == other.consumerUsage && this.maxBuffers == other.maxBuffers) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.streamId))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.overrideFormat))), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.producerUsage))), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.consumerUsage))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.maxBuffers))));
    }

    public final String toString() {
        return "{.streamId = " + this.streamId + ", .overrideFormat = " + PixelFormat.toString(this.overrideFormat) + ", .producerUsage = " + this.producerUsage + ", .consumerUsage = " + this.consumerUsage + ", .maxBuffers = " + this.maxBuffers + "}";
    }

    public final void readFromParcel(HwParcel parcel) {
        HwBlob blob = parcel.readBuffer(32L);
        readEmbeddedFromParcel(parcel, blob, 0L);
    }

    public static final ArrayList<HalStream> readVectorFromParcel(HwParcel parcel) {
        ArrayList<HalStream> _hidl_vec = new ArrayList<>();
        HwBlob _hidl_blob = parcel.readBuffer(16L);
        int _hidl_vec_size = _hidl_blob.getInt32(8L);
        HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 32, _hidl_blob.handle(), 0L, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            HalStream _hidl_vec_element = new HalStream();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 32);
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        this.streamId = _hidl_blob.getInt32(0 + _hidl_offset);
        this.overrideFormat = _hidl_blob.getInt32(4 + _hidl_offset);
        this.producerUsage = _hidl_blob.getInt64(8 + _hidl_offset);
        this.consumerUsage = _hidl_blob.getInt64(16 + _hidl_offset);
        this.maxBuffers = _hidl_blob.getInt32(24 + _hidl_offset);
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(32);
        writeEmbeddedToBlob(_hidl_blob, 0L);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<HalStream> _hidl_vec) {
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
        _hidl_blob.putInt32(0 + _hidl_offset, this.streamId);
        _hidl_blob.putInt32(4 + _hidl_offset, this.overrideFormat);
        _hidl_blob.putInt64(8 + _hidl_offset, this.producerUsage);
        _hidl_blob.putInt64(16 + _hidl_offset, this.consumerUsage);
        _hidl_blob.putInt32(24 + _hidl_offset, this.maxBuffers);
    }
}