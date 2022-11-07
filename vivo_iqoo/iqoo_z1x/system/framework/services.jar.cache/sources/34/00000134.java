package android.hardware.soundtrigger.V2_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

/* loaded from: classes.dex */
public final class PhraseRecognitionExtra {
    public int id = 0;
    public int recognitionModes = 0;
    public int confidenceLevel = 0;
    public ArrayList<ConfidenceLevel> levels = new ArrayList<>();

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != PhraseRecognitionExtra.class) {
            return false;
        }
        PhraseRecognitionExtra other = (PhraseRecognitionExtra) otherObject;
        if (this.id == other.id && this.recognitionModes == other.recognitionModes && this.confidenceLevel == other.confidenceLevel && HidlSupport.deepEquals(this.levels, other.levels)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.id))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.recognitionModes))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.confidenceLevel))), Integer.valueOf(HidlSupport.deepHashCode(this.levels)));
    }

    public final String toString() {
        return "{.id = " + this.id + ", .recognitionModes = " + this.recognitionModes + ", .confidenceLevel = " + this.confidenceLevel + ", .levels = " + this.levels + "}";
    }

    public final void readFromParcel(HwParcel parcel) {
        HwBlob blob = parcel.readBuffer(32L);
        readEmbeddedFromParcel(parcel, blob, 0L);
    }

    public static final ArrayList<PhraseRecognitionExtra> readVectorFromParcel(HwParcel parcel) {
        ArrayList<PhraseRecognitionExtra> _hidl_vec = new ArrayList<>();
        HwBlob _hidl_blob = parcel.readBuffer(16L);
        int _hidl_vec_size = _hidl_blob.getInt32(8L);
        HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 32, _hidl_blob.handle(), 0L, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            PhraseRecognitionExtra _hidl_vec_element = new PhraseRecognitionExtra();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 32);
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        this.id = _hidl_blob.getInt32(_hidl_offset + 0);
        this.recognitionModes = _hidl_blob.getInt32(_hidl_offset + 4);
        this.confidenceLevel = _hidl_blob.getInt32(_hidl_offset + 8);
        int _hidl_vec_size = _hidl_blob.getInt32(_hidl_offset + 16 + 8);
        HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 8, _hidl_blob.handle(), _hidl_offset + 16 + 0, true);
        this.levels.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            ConfidenceLevel _hidl_vec_element = new ConfidenceLevel();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 8);
            this.levels.add(_hidl_vec_element);
        }
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(32);
        writeEmbeddedToBlob(_hidl_blob, 0L);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<PhraseRecognitionExtra> _hidl_vec) {
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
        _hidl_blob.putInt32(_hidl_offset + 0, this.id);
        _hidl_blob.putInt32(4 + _hidl_offset, this.recognitionModes);
        _hidl_blob.putInt32(_hidl_offset + 8, this.confidenceLevel);
        int _hidl_vec_size = this.levels.size();
        _hidl_blob.putInt32(_hidl_offset + 16 + 8, _hidl_vec_size);
        _hidl_blob.putBool(_hidl_offset + 16 + 12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 8);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            this.levels.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, _hidl_index_0 * 8);
        }
        _hidl_blob.putBlob(16 + _hidl_offset + 0, childBlob);
    }
}