package android.net.dhcp;

import android.os.Parcel;
import android.os.Parcelable;

/* loaded from: classes.dex */
public class DhcpLeaseParcelable implements Parcelable {
    public static final Parcelable.Creator<DhcpLeaseParcelable> CREATOR = new Parcelable.Creator<DhcpLeaseParcelable>() { // from class: android.net.dhcp.DhcpLeaseParcelable.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public DhcpLeaseParcelable createFromParcel(Parcel _aidl_source) {
            DhcpLeaseParcelable _aidl_out = new DhcpLeaseParcelable();
            _aidl_out.readFromParcel(_aidl_source);
            return _aidl_out;
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public DhcpLeaseParcelable[] newArray(int _aidl_size) {
            return new DhcpLeaseParcelable[_aidl_size];
        }
    };
    public byte[] clientId;
    public long expTime;
    public String hostname;
    public byte[] hwAddr;
    public int netAddr;
    public int prefixLength;

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel _aidl_parcel, int _aidl_flag) {
        int _aidl_start_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.writeInt(0);
        _aidl_parcel.writeByteArray(this.clientId);
        _aidl_parcel.writeByteArray(this.hwAddr);
        _aidl_parcel.writeInt(this.netAddr);
        _aidl_parcel.writeInt(this.prefixLength);
        _aidl_parcel.writeLong(this.expTime);
        _aidl_parcel.writeString(this.hostname);
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
            this.clientId = _aidl_parcel.createByteArray();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.hwAddr = _aidl_parcel.createByteArray();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.netAddr = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.prefixLength = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.expTime = _aidl_parcel.readLong();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.hostname = _aidl_parcel.readString();
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