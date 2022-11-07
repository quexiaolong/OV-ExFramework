package vendor.vivo.hardware.camera.vivopostproc.V1_0;

import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;

/* loaded from: classes.dex */
public final class PostProcRequest {
    public int frameNumber = 0;
    public ArrayList<Byte> settings = new ArrayList<>();
    public StreamBuffer inputBuffer = new StreamBuffer();
    public ArrayList<StreamBuffer> outputBuffers = new ArrayList<>();

    public final String toString() {
        return "{.frameNumber = " + this.frameNumber + ", .settings = " + this.settings + ", .inputBuffer = " + this.inputBuffer + ", .outputBuffers = " + this.outputBuffers + "}";
    }

    public final void readFromParcel(HwParcel parcel) {
        HwBlob blob = parcel.readBuffer(80L);
        readEmbeddedFromParcel(parcel, blob, 0L);
    }

    public static final ArrayList<PostProcRequest> readVectorFromParcel(HwParcel parcel) {
        ArrayList<PostProcRequest> _hidl_vec = new ArrayList<>();
        HwBlob _hidl_blob = parcel.readBuffer(16L);
        int _hidl_vec_size = _hidl_blob.getInt32(8L);
        HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 80, _hidl_blob.handle(), 0L, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            PostProcRequest _hidl_vec_element = new PostProcRequest();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 80);
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        this.frameNumber = _hidl_blob.getInt32(_hidl_offset + 0);
        int _hidl_vec_size = _hidl_blob.getInt32(_hidl_offset + 8 + 8);
        HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 1, _hidl_blob.handle(), _hidl_offset + 8 + 0, true);
        this.settings.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            this.settings.add(Byte.valueOf(childBlob.getInt8(_hidl_index_0 * 1)));
        }
        this.inputBuffer.readEmbeddedFromParcel(parcel, _hidl_blob, _hidl_offset + 24);
        int _hidl_vec_size2 = _hidl_blob.getInt32(_hidl_offset + 64 + 8);
        HwBlob childBlob2 = parcel.readEmbeddedBuffer(_hidl_vec_size2 * 40, _hidl_blob.handle(), _hidl_offset + 64 + 0, true);
        this.outputBuffers.clear();
        for (int _hidl_index_02 = 0; _hidl_index_02 < _hidl_vec_size2; _hidl_index_02++) {
            StreamBuffer _hidl_vec_element = new StreamBuffer();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob2, _hidl_index_02 * 40);
            this.outputBuffers.add(_hidl_vec_element);
        }
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(80);
        writeEmbeddedToBlob(_hidl_blob, 0L);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<PostProcRequest> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8L, _hidl_vec_size);
        _hidl_blob.putBool(12L, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 80);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            _hidl_vec.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, _hidl_index_0 * 80);
        }
        _hidl_blob.putBlob(0L, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        _hidl_blob.putInt32(_hidl_offset + 0, this.frameNumber);
        int _hidl_vec_size = this.settings.size();
        _hidl_blob.putInt32(_hidl_offset + 8 + 8, _hidl_vec_size);
        _hidl_blob.putBool(_hidl_offset + 8 + 12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 1);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            childBlob.putInt8(_hidl_index_0 * 1, this.settings.get(_hidl_index_0).byteValue());
        }
        _hidl_blob.putBlob(_hidl_offset + 8 + 0, childBlob);
        this.inputBuffer.writeEmbeddedToBlob(_hidl_blob, _hidl_offset + 24);
        int _hidl_vec_size2 = this.outputBuffers.size();
        _hidl_blob.putInt32(_hidl_offset + 64 + 8, _hidl_vec_size2);
        _hidl_blob.putBool(_hidl_offset + 64 + 12, false);
        HwBlob childBlob2 = new HwBlob(_hidl_vec_size2 * 40);
        for (int _hidl_index_02 = 0; _hidl_index_02 < _hidl_vec_size2; _hidl_index_02++) {
            this.outputBuffers.get(_hidl_index_02).writeEmbeddedToBlob(childBlob2, _hidl_index_02 * 40);
        }
        _hidl_blob.putBlob(_hidl_offset + 64 + 0, childBlob2);
    }
}