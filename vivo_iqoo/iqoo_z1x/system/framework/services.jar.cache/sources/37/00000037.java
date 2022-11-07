package android.hardware.audio.common.V2_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

/* loaded from: classes.dex */
public final class AudioGain {
    public int mode = 0;
    public int channelMask = 0;
    public int minValue = 0;
    public int maxValue = 0;
    public int defaultValue = 0;
    public int stepValue = 0;
    public int minRampMs = 0;
    public int maxRampMs = 0;

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != AudioGain.class) {
            return false;
        }
        AudioGain other = (AudioGain) otherObject;
        if (this.mode == other.mode && this.channelMask == other.channelMask && this.minValue == other.minValue && this.maxValue == other.maxValue && this.defaultValue == other.defaultValue && this.stepValue == other.stepValue && this.minRampMs == other.minRampMs && this.maxRampMs == other.maxRampMs) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.mode))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.channelMask))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.minValue))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.maxValue))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.defaultValue))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.stepValue))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.minRampMs))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.maxRampMs))));
    }

    public final String toString() {
        return "{.mode = " + AudioGainMode.toString(this.mode) + ", .channelMask = " + AudioChannelMask.toString(this.channelMask) + ", .minValue = " + this.minValue + ", .maxValue = " + this.maxValue + ", .defaultValue = " + this.defaultValue + ", .stepValue = " + this.stepValue + ", .minRampMs = " + this.minRampMs + ", .maxRampMs = " + this.maxRampMs + "}";
    }

    public final void readFromParcel(HwParcel parcel) {
        HwBlob blob = parcel.readBuffer(32L);
        readEmbeddedFromParcel(parcel, blob, 0L);
    }

    public static final ArrayList<AudioGain> readVectorFromParcel(HwParcel parcel) {
        ArrayList<AudioGain> _hidl_vec = new ArrayList<>();
        HwBlob _hidl_blob = parcel.readBuffer(16L);
        int _hidl_vec_size = _hidl_blob.getInt32(8L);
        HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 32, _hidl_blob.handle(), 0L, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            AudioGain _hidl_vec_element = new AudioGain();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 32);
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        this.mode = _hidl_blob.getInt32(0 + _hidl_offset);
        this.channelMask = _hidl_blob.getInt32(4 + _hidl_offset);
        this.minValue = _hidl_blob.getInt32(8 + _hidl_offset);
        this.maxValue = _hidl_blob.getInt32(12 + _hidl_offset);
        this.defaultValue = _hidl_blob.getInt32(16 + _hidl_offset);
        this.stepValue = _hidl_blob.getInt32(20 + _hidl_offset);
        this.minRampMs = _hidl_blob.getInt32(24 + _hidl_offset);
        this.maxRampMs = _hidl_blob.getInt32(28 + _hidl_offset);
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(32);
        writeEmbeddedToBlob(_hidl_blob, 0L);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<AudioGain> _hidl_vec) {
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
        _hidl_blob.putInt32(0 + _hidl_offset, this.mode);
        _hidl_blob.putInt32(4 + _hidl_offset, this.channelMask);
        _hidl_blob.putInt32(8 + _hidl_offset, this.minValue);
        _hidl_blob.putInt32(12 + _hidl_offset, this.maxValue);
        _hidl_blob.putInt32(16 + _hidl_offset, this.defaultValue);
        _hidl_blob.putInt32(20 + _hidl_offset, this.stepValue);
        _hidl_blob.putInt32(24 + _hidl_offset, this.minRampMs);
        _hidl_blob.putInt32(28 + _hidl_offset, this.maxRampMs);
    }
}