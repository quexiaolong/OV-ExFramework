package android.hardware.audio.common.V2_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/* loaded from: classes.dex */
public final class AudioGainConfig {
    public int index = 0;
    public int mode = 0;
    public int channelMask = 0;
    public int[] values = new int[32];
    public int rampDurationMs = 0;

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != AudioGainConfig.class) {
            return false;
        }
        AudioGainConfig other = (AudioGainConfig) otherObject;
        if (this.index == other.index && this.mode == other.mode && this.channelMask == other.channelMask && HidlSupport.deepEquals(this.values, other.values) && this.rampDurationMs == other.rampDurationMs) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.index))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.mode))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.channelMask))), Integer.valueOf(HidlSupport.deepHashCode(this.values)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.rampDurationMs))));
    }

    public final String toString() {
        return "{.index = " + this.index + ", .mode = " + AudioGainMode.toString(this.mode) + ", .channelMask = " + AudioChannelMask.toString(this.channelMask) + ", .values = " + Arrays.toString(this.values) + ", .rampDurationMs = " + this.rampDurationMs + "}";
    }

    public final void readFromParcel(HwParcel parcel) {
        HwBlob blob = parcel.readBuffer(144L);
        readEmbeddedFromParcel(parcel, blob, 0L);
    }

    public static final ArrayList<AudioGainConfig> readVectorFromParcel(HwParcel parcel) {
        ArrayList<AudioGainConfig> _hidl_vec = new ArrayList<>();
        HwBlob _hidl_blob = parcel.readBuffer(16L);
        int _hidl_vec_size = _hidl_blob.getInt32(8L);
        HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 144, _hidl_blob.handle(), 0L, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            AudioGainConfig _hidl_vec_element = new AudioGainConfig();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 144);
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        this.index = _hidl_blob.getInt32(0 + _hidl_offset);
        this.mode = _hidl_blob.getInt32(4 + _hidl_offset);
        this.channelMask = _hidl_blob.getInt32(8 + _hidl_offset);
        long _hidl_array_offset_0 = 12 + _hidl_offset;
        _hidl_blob.copyToInt32Array(_hidl_array_offset_0, this.values, 32);
        this.rampDurationMs = _hidl_blob.getInt32(140 + _hidl_offset);
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(144);
        writeEmbeddedToBlob(_hidl_blob, 0L);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<AudioGainConfig> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8L, _hidl_vec_size);
        _hidl_blob.putBool(12L, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 144);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            _hidl_vec.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, _hidl_index_0 * 144);
        }
        _hidl_blob.putBlob(0L, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        _hidl_blob.putInt32(0 + _hidl_offset, this.index);
        _hidl_blob.putInt32(4 + _hidl_offset, this.mode);
        _hidl_blob.putInt32(8 + _hidl_offset, this.channelMask);
        long _hidl_array_offset_0 = 12 + _hidl_offset;
        int[] _hidl_array_item_0 = this.values;
        if (_hidl_array_item_0 == null || _hidl_array_item_0.length != 32) {
            throw new IllegalArgumentException("Array element is not of the expected length");
        }
        _hidl_blob.putInt32Array(_hidl_array_offset_0, _hidl_array_item_0);
        _hidl_blob.putInt32(140 + _hidl_offset, this.rampDurationMs);
    }
}