package vendor.vivo.hardware.camera.vivoreprocess.V1_0;

import android.hardware.graphics.common.V1_0.BufferUsage;
import android.hardware.graphics.common.V1_0.Dataspace;
import android.hardware.graphics.common.V1_0.PixelFormat;
import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

/* loaded from: classes.dex */
public final class ReprocessStream {
    public int dataSpace;
    public long usage;
    public int id = 0;
    public int streamType = 0;
    public int width = 0;
    public int height = 0;
    public int format = 0;
    public int rotation = 0;
    public int groupId = 0;
    public int physicalCameraId = 0;

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != ReprocessStream.class) {
            return false;
        }
        ReprocessStream other = (ReprocessStream) otherObject;
        if (this.id == other.id && this.streamType == other.streamType && this.width == other.width && this.height == other.height && this.format == other.format && HidlSupport.deepEquals(Long.valueOf(this.usage), Long.valueOf(other.usage)) && HidlSupport.deepEquals(Integer.valueOf(this.dataSpace), Integer.valueOf(other.dataSpace)) && this.rotation == other.rotation && this.groupId == other.groupId && this.physicalCameraId == other.physicalCameraId) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.id))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.streamType))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.width))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.height))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.format))), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.usage))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.dataSpace))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.rotation))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.groupId))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.physicalCameraId))));
    }

    public final String toString() {
        return "{.id = " + this.id + ", .streamType = " + ReprocessStreamType.toString(this.streamType) + ", .width = " + this.width + ", .height = " + this.height + ", .format = " + PixelFormat.toString(this.format) + ", .usage = " + BufferUsage.dumpBitfield(this.usage) + ", .dataSpace = " + Dataspace.dumpBitfield(this.dataSpace) + ", .rotation = " + ReprocessStreamRotation.toString(this.rotation) + ", .groupId = " + this.groupId + ", .physicalCameraId = " + this.physicalCameraId + "}";
    }

    public final void readFromParcel(HwParcel parcel) {
        HwBlob blob = parcel.readBuffer(48L);
        readEmbeddedFromParcel(parcel, blob, 0L);
    }

    public static final ArrayList<ReprocessStream> readVectorFromParcel(HwParcel parcel) {
        ArrayList<ReprocessStream> _hidl_vec = new ArrayList<>();
        HwBlob _hidl_blob = parcel.readBuffer(16L);
        int _hidl_vec_size = _hidl_blob.getInt32(8L);
        HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 48, _hidl_blob.handle(), 0L, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            ReprocessStream _hidl_vec_element = new ReprocessStream();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 48);
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        this.id = _hidl_blob.getInt32(0 + _hidl_offset);
        this.streamType = _hidl_blob.getInt32(4 + _hidl_offset);
        this.width = _hidl_blob.getInt32(8 + _hidl_offset);
        this.height = _hidl_blob.getInt32(12 + _hidl_offset);
        this.format = _hidl_blob.getInt32(16 + _hidl_offset);
        this.usage = _hidl_blob.getInt64(24 + _hidl_offset);
        this.dataSpace = _hidl_blob.getInt32(32 + _hidl_offset);
        this.rotation = _hidl_blob.getInt32(36 + _hidl_offset);
        this.groupId = _hidl_blob.getInt32(40 + _hidl_offset);
        this.physicalCameraId = _hidl_blob.getInt32(44 + _hidl_offset);
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(48);
        writeEmbeddedToBlob(_hidl_blob, 0L);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<ReprocessStream> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8L, _hidl_vec_size);
        _hidl_blob.putBool(12L, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 48);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            _hidl_vec.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, _hidl_index_0 * 48);
        }
        _hidl_blob.putBlob(0L, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        _hidl_blob.putInt32(0 + _hidl_offset, this.id);
        _hidl_blob.putInt32(4 + _hidl_offset, this.streamType);
        _hidl_blob.putInt32(8 + _hidl_offset, this.width);
        _hidl_blob.putInt32(12 + _hidl_offset, this.height);
        _hidl_blob.putInt32(16 + _hidl_offset, this.format);
        _hidl_blob.putInt64(24 + _hidl_offset, this.usage);
        _hidl_blob.putInt32(32 + _hidl_offset, this.dataSpace);
        _hidl_blob.putInt32(36 + _hidl_offset, this.rotation);
        _hidl_blob.putInt32(40 + _hidl_offset, this.groupId);
        _hidl_blob.putInt32(44 + _hidl_offset, this.physicalCameraId);
    }
}