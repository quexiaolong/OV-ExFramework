package android.hardware.broadcastradio.V2_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

/* loaded from: classes.dex */
public final class Properties {
    public String maker = new String();
    public String product = new String();
    public String version = new String();
    public String serial = new String();
    public ArrayList<Integer> supportedIdentifierTypes = new ArrayList<>();
    public ArrayList<VendorKeyValue> vendorInfo = new ArrayList<>();

    public final boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || otherObject.getClass() != Properties.class) {
            return false;
        }
        Properties other = (Properties) otherObject;
        if (HidlSupport.deepEquals(this.maker, other.maker) && HidlSupport.deepEquals(this.product, other.product) && HidlSupport.deepEquals(this.version, other.version) && HidlSupport.deepEquals(this.serial, other.serial) && HidlSupport.deepEquals(this.supportedIdentifierTypes, other.supportedIdentifierTypes) && HidlSupport.deepEquals(this.vendorInfo, other.vendorInfo)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.maker)), Integer.valueOf(HidlSupport.deepHashCode(this.product)), Integer.valueOf(HidlSupport.deepHashCode(this.version)), Integer.valueOf(HidlSupport.deepHashCode(this.serial)), Integer.valueOf(HidlSupport.deepHashCode(this.supportedIdentifierTypes)), Integer.valueOf(HidlSupport.deepHashCode(this.vendorInfo)));
    }

    public final String toString() {
        return "{.maker = " + this.maker + ", .product = " + this.product + ", .version = " + this.version + ", .serial = " + this.serial + ", .supportedIdentifierTypes = " + this.supportedIdentifierTypes + ", .vendorInfo = " + this.vendorInfo + "}";
    }

    public final void readFromParcel(HwParcel parcel) {
        HwBlob blob = parcel.readBuffer(96L);
        readEmbeddedFromParcel(parcel, blob, 0L);
    }

    public static final ArrayList<Properties> readVectorFromParcel(HwParcel parcel) {
        ArrayList<Properties> _hidl_vec = new ArrayList<>();
        HwBlob _hidl_blob = parcel.readBuffer(16L);
        int _hidl_vec_size = _hidl_blob.getInt32(8L);
        HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 96, _hidl_blob.handle(), 0L, true);
        _hidl_vec.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            Properties _hidl_vec_element = new Properties();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob, _hidl_index_0 * 96);
            _hidl_vec.add(_hidl_vec_element);
        }
        return _hidl_vec;
    }

    public final void readEmbeddedFromParcel(HwParcel parcel, HwBlob _hidl_blob, long _hidl_offset) {
        String string = _hidl_blob.getString(_hidl_offset + 0);
        this.maker = string;
        parcel.readEmbeddedBuffer(string.getBytes().length + 1, _hidl_blob.handle(), _hidl_offset + 0 + 0, false);
        String string2 = _hidl_blob.getString(_hidl_offset + 16);
        this.product = string2;
        parcel.readEmbeddedBuffer(string2.getBytes().length + 1, _hidl_blob.handle(), _hidl_offset + 16 + 0, false);
        String string3 = _hidl_blob.getString(_hidl_offset + 32);
        this.version = string3;
        parcel.readEmbeddedBuffer(string3.getBytes().length + 1, _hidl_blob.handle(), _hidl_offset + 32 + 0, false);
        String string4 = _hidl_blob.getString(_hidl_offset + 48);
        this.serial = string4;
        parcel.readEmbeddedBuffer(string4.getBytes().length + 1, _hidl_blob.handle(), _hidl_offset + 48 + 0, false);
        int _hidl_vec_size = _hidl_blob.getInt32(_hidl_offset + 64 + 8);
        HwBlob childBlob = parcel.readEmbeddedBuffer(_hidl_vec_size * 4, _hidl_blob.handle(), _hidl_offset + 64 + 0, true);
        this.supportedIdentifierTypes.clear();
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            this.supportedIdentifierTypes.add(Integer.valueOf(childBlob.getInt32(_hidl_index_0 * 4)));
        }
        int _hidl_vec_size2 = _hidl_blob.getInt32(_hidl_offset + 80 + 8);
        HwBlob childBlob2 = parcel.readEmbeddedBuffer(_hidl_vec_size2 * 32, _hidl_blob.handle(), _hidl_offset + 80 + 0, true);
        this.vendorInfo.clear();
        for (int _hidl_index_02 = 0; _hidl_index_02 < _hidl_vec_size2; _hidl_index_02++) {
            VendorKeyValue _hidl_vec_element = new VendorKeyValue();
            _hidl_vec_element.readEmbeddedFromParcel(parcel, childBlob2, _hidl_index_02 * 32);
            this.vendorInfo.add(_hidl_vec_element);
        }
    }

    public final void writeToParcel(HwParcel parcel) {
        HwBlob _hidl_blob = new HwBlob(96);
        writeEmbeddedToBlob(_hidl_blob, 0L);
        parcel.writeBuffer(_hidl_blob);
    }

    public static final void writeVectorToParcel(HwParcel parcel, ArrayList<Properties> _hidl_vec) {
        HwBlob _hidl_blob = new HwBlob(16);
        int _hidl_vec_size = _hidl_vec.size();
        _hidl_blob.putInt32(8L, _hidl_vec_size);
        _hidl_blob.putBool(12L, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 96);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            _hidl_vec.get(_hidl_index_0).writeEmbeddedToBlob(childBlob, _hidl_index_0 * 96);
        }
        _hidl_blob.putBlob(0L, childBlob);
        parcel.writeBuffer(_hidl_blob);
    }

    public final void writeEmbeddedToBlob(HwBlob _hidl_blob, long _hidl_offset) {
        _hidl_blob.putString(_hidl_offset + 0, this.maker);
        _hidl_blob.putString(_hidl_offset + 16, this.product);
        _hidl_blob.putString(_hidl_offset + 32, this.version);
        _hidl_blob.putString(_hidl_offset + 48, this.serial);
        int _hidl_vec_size = this.supportedIdentifierTypes.size();
        _hidl_blob.putInt32(_hidl_offset + 64 + 8, _hidl_vec_size);
        _hidl_blob.putBool(_hidl_offset + 64 + 12, false);
        HwBlob childBlob = new HwBlob(_hidl_vec_size * 4);
        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
            childBlob.putInt32(_hidl_index_0 * 4, this.supportedIdentifierTypes.get(_hidl_index_0).intValue());
        }
        _hidl_blob.putBlob(_hidl_offset + 64 + 0, childBlob);
        int _hidl_vec_size2 = this.vendorInfo.size();
        _hidl_blob.putInt32(_hidl_offset + 80 + 8, _hidl_vec_size2);
        _hidl_blob.putBool(_hidl_offset + 80 + 12, false);
        HwBlob childBlob2 = new HwBlob(_hidl_vec_size2 * 32);
        for (int _hidl_index_02 = 0; _hidl_index_02 < _hidl_vec_size2; _hidl_index_02++) {
            this.vendorInfo.get(_hidl_index_02).writeEmbeddedToBlob(childBlob2, _hidl_index_02 * 32);
        }
        _hidl_blob.putBlob(_hidl_offset + 80 + 0, childBlob2);
    }
}