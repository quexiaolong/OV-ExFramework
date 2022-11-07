package android.net;

import android.os.Parcel;
import android.os.Parcelable;

/* loaded from: classes.dex */
public class Layer2PacketParcelable implements Parcelable {
    public static final Parcelable.Creator<Layer2PacketParcelable> CREATOR = new Parcelable.Creator<Layer2PacketParcelable>() { // from class: android.net.Layer2PacketParcelable.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public Layer2PacketParcelable createFromParcel(Parcel _aidl_source) {
            Layer2PacketParcelable _aidl_out = new Layer2PacketParcelable();
            _aidl_out.readFromParcel(_aidl_source);
            return _aidl_out;
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public Layer2PacketParcelable[] newArray(int _aidl_size) {
            return new Layer2PacketParcelable[_aidl_size];
        }
    };
    public MacAddress dstMacAddress;
    public byte[] payload;

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel _aidl_parcel, int _aidl_flag) {
        int _aidl_start_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.writeInt(0);
        if (this.dstMacAddress != null) {
            _aidl_parcel.writeInt(1);
            this.dstMacAddress.writeToParcel(_aidl_parcel, 0);
        } else {
            _aidl_parcel.writeInt(0);
        }
        _aidl_parcel.writeByteArray(this.payload);
        int _aidl_end_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.setDataPosition(_aidl_start_pos);
        _aidl_parcel.writeInt(_aidl_end_pos - _aidl_start_pos);
        _aidl_parcel.setDataPosition(_aidl_end_pos);
    }

    public final void readFromParcel(Parcel _aidl_parcel) {
        int _aidl_start_pos = _aidl_parcel.dataPosition();
        int _aidl_parcelable_size = _aidl_parcel.readInt();
        if (_aidl_parcelable_size < 0) {
            return;
        }
        try {
            if (_aidl_parcel.readInt() != 0) {
                this.dstMacAddress = (MacAddress) MacAddress.CREATOR.createFromParcel(_aidl_parcel);
            } else {
                this.dstMacAddress = null;
            }
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.payload = _aidl_parcel.createByteArray();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
            }
        } finally {
            _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
        }
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }
}