package android.net;

import android.os.Parcel;
import android.os.Parcelable;

/* loaded from: classes.dex */
public class DhcpResultsParcelable implements Parcelable {
    public static final Parcelable.Creator<DhcpResultsParcelable> CREATOR = new Parcelable.Creator<DhcpResultsParcelable>() { // from class: android.net.DhcpResultsParcelable.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public DhcpResultsParcelable createFromParcel(Parcel _aidl_source) {
            DhcpResultsParcelable _aidl_out = new DhcpResultsParcelable();
            _aidl_out.readFromParcel(_aidl_source);
            return _aidl_out;
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public DhcpResultsParcelable[] newArray(int _aidl_size) {
            return new DhcpResultsParcelable[_aidl_size];
        }
    };
    public StaticIpConfiguration baseConfiguration;
    public String captivePortalApiUrl;
    public int leaseDuration;
    public int mtu;
    public String serverAddress;
    public String serverHostName;
    public String vendorInfo;

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel _aidl_parcel, int _aidl_flag) {
        int _aidl_start_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.writeInt(0);
        if (this.baseConfiguration != null) {
            _aidl_parcel.writeInt(1);
            this.baseConfiguration.writeToParcel(_aidl_parcel, 0);
        } else {
            _aidl_parcel.writeInt(0);
        }
        _aidl_parcel.writeInt(this.leaseDuration);
        _aidl_parcel.writeInt(this.mtu);
        _aidl_parcel.writeString(this.serverAddress);
        _aidl_parcel.writeString(this.vendorInfo);
        _aidl_parcel.writeString(this.serverHostName);
        _aidl_parcel.writeString(this.captivePortalApiUrl);
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
                this.baseConfiguration = (StaticIpConfiguration) StaticIpConfiguration.CREATOR.createFromParcel(_aidl_parcel);
            } else {
                this.baseConfiguration = null;
            }
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.leaseDuration = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.mtu = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.serverAddress = _aidl_parcel.readString();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.vendorInfo = _aidl_parcel.readString();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.serverHostName = _aidl_parcel.readString();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.captivePortalApiUrl = _aidl_parcel.readString();
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