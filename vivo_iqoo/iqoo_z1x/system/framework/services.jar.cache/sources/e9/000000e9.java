package android.hardware.health.V2_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

/* loaded from: classes.dex */
public final class StorageInfo {
    public StorageAttribute attr = new StorageAttribute();
    public short eol = 0;
    public short lifetimeA = 0;
    public short lifetimeB = 0;
    public String version = new String();

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != StorageInfo.class) {
            return false;
        }
        StorageInfo other = (StorageInfo) otherObject;
        if (HidlSupport.deepEquals(this.attr, other.attr) && this.eol == other.eol && this.lifetimeA == other.lifetimeA && this.lifetimeB == other.lifetimeB && HidlSupport.deepEquals(this.version, other.version)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.attr)), Integer.valueOf(HidlSupport.deepHashCode(Short.valueOf(this.eol))), Integer.valueOf(HidlSupport.deepHashCode(Short.valueOf(this.lifetimeA))), Integer.valueOf(HidlSupport.deepHashCode(Short.valueOf(this.lifetimeB))), Integer.valueOf(HidlSupport.deepHashCode(this.version)));
    }

    public final String toString() {
        return "{.attr = " + this.attr + ", .eol = " + ((int) this.eol) + ", .lifetimeA = " + ((int) this.lifetimeA) + ", .lifetimeB = " + ((int) this.lifetimeB) + ", .version = " + this.version + "}";
    }

    public final void readFromParcel(HwParcel parcel) {
        HwBlob blob = parcel.readBuffer(48L);
        readEmbeddedFromParcel(parcel, blob, 0L);
    }

    public static final ArrayList<StorageInfo> readVectorFromParcel(HwParcel parcel) {
        ArrayList<StorageInfo> _hidl_vec = new ArrayList<>();
        HwBlob _hidl_blob = parcel.readBuffer(16L);
        int _hidl_vec_size = _hidl_blob.getInt32(8L);
        HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 48, _hidl_blob.handle(), 0L, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            StorageInfo _hidl_vec_element = new StorageInfo();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 48);
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        this.attr.readEmbeddedFromParcel(parcel, _hidl_blob, _hidl_offset + 0);
        this.eol = _hidl_blob.getInt16(_hidl_offset + 24);
        this.lifetimeA = _hidl_blob.getInt16(_hidl_offset + 26);
        this.lifetimeB = _hidl_blob.getInt16(_hidl_offset + 28);
        String string = _hidl_blob.getString(_hidl_offset + 32);
        this.version = string;
        parcel.readEmbeddedBuffer(string.getBytes().length + 1, _hidl_blob.handle(), _hidl_offset + 32 + 0, false);
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(48);
        writeEmbeddedToBlob(_hidl_blob, 0L);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<StorageInfo> _hidl_vec) {
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
        this.attr.writeEmbeddedToBlob(_hidl_blob, 0 + _hidl_offset);
        _hidl_blob.putInt16(24 + _hidl_offset, this.eol);
        _hidl_blob.putInt16(26 + _hidl_offset, this.lifetimeA);
        _hidl_blob.putInt16(28 + _hidl_offset, this.lifetimeB);
        _hidl_blob.putString(32 + _hidl_offset, this.version);
    }
}