package android.net.ipmemorystore;

import android.os.Parcel;
import android.os.Parcelable;

/* loaded from: classes.dex */
public class NetworkAttributesParcelable implements Parcelable {
    public static final Parcelable.Creator<NetworkAttributesParcelable> CREATOR = new Parcelable.Creator<NetworkAttributesParcelable>() { // from class: android.net.ipmemorystore.NetworkAttributesParcelable.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public NetworkAttributesParcelable createFromParcel(Parcel _aidl_source) {
            NetworkAttributesParcelable _aidl_out = new NetworkAttributesParcelable();
            _aidl_out.readFromParcel(_aidl_source);
            return _aidl_out;
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public NetworkAttributesParcelable[] newArray(int _aidl_size) {
            return new NetworkAttributesParcelable[_aidl_size];
        }
    };
    public byte[] assignedV4Address;
    public long assignedV4AddressExpiry;
    public String cluster;
    public Blob[] dnsAddresses;
    public int mtu;

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel _aidl_parcel, int _aidl_flag) {
        int _aidl_start_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.writeInt(0);
        _aidl_parcel.writeByteArray(this.assignedV4Address);
        _aidl_parcel.writeLong(this.assignedV4AddressExpiry);
        _aidl_parcel.writeString(this.cluster);
        _aidl_parcel.writeTypedArray(this.dnsAddresses, 0);
        _aidl_parcel.writeInt(this.mtu);
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
            this.assignedV4Address = _aidl_parcel.createByteArray();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.assignedV4AddressExpiry = _aidl_parcel.readLong();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.cluster = _aidl_parcel.readString();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.dnsAddresses = (Blob[]) _aidl_parcel.createTypedArray(Blob.CREATOR);
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.mtu = _aidl_parcel.readInt();
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