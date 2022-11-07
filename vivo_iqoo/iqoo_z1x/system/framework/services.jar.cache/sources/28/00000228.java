package android.net;

import android.os.Parcel;
import android.os.Parcelable;

/* loaded from: classes.dex */
public class Layer2InformationParcelable implements Parcelable {
    public static final Parcelable.Creator<Layer2InformationParcelable> CREATOR = new Parcelable.Creator<Layer2InformationParcelable>() { // from class: android.net.Layer2InformationParcelable.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public Layer2InformationParcelable createFromParcel(Parcel _aidl_source) {
            Layer2InformationParcelable _aidl_out = new Layer2InformationParcelable();
            _aidl_out.readFromParcel(_aidl_source);
            return _aidl_out;
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public Layer2InformationParcelable[] newArray(int _aidl_size) {
            return new Layer2InformationParcelable[_aidl_size];
        }
    };
    public MacAddress bssid;
    public String cluster;
    public String l2Key;

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel _aidl_parcel, int _aidl_flag) {
        int _aidl_start_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.writeInt(0);
        _aidl_parcel.writeString(this.l2Key);
        _aidl_parcel.writeString(this.cluster);
        if (this.bssid != null) {
            _aidl_parcel.writeInt(1);
            this.bssid.writeToParcel(_aidl_parcel, 0);
        } else {
            _aidl_parcel.writeInt(0);
        }
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
            this.l2Key = _aidl_parcel.readString();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.cluster = _aidl_parcel.readString();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            if (_aidl_parcel.readInt() != 0) {
                this.bssid = (MacAddress) MacAddress.CREATOR.createFromParcel(_aidl_parcel);
            } else {
                this.bssid = null;
            }
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