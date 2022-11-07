package android.net;

import android.os.Parcel;
import android.os.Parcelable;

/* loaded from: classes.dex */
public class ResolverOptionsParcel implements Parcelable {
    public static final Parcelable.Creator<ResolverOptionsParcel> CREATOR = new Parcelable.Creator<ResolverOptionsParcel>() { // from class: android.net.ResolverOptionsParcel.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ResolverOptionsParcel createFromParcel(Parcel _aidl_source) {
            ResolverOptionsParcel _aidl_out = new ResolverOptionsParcel();
            _aidl_out.readFromParcel(_aidl_source);
            return _aidl_out;
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ResolverOptionsParcel[] newArray(int _aidl_size) {
            return new ResolverOptionsParcel[_aidl_size];
        }
    };
    public ResolverHostsParcel[] hosts = new ResolverHostsParcel[0];
    public int tcMode = 0;
    public boolean enforceDnsUid = false;

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel _aidl_parcel, int _aidl_flag) {
        int _aidl_start_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.writeInt(0);
        _aidl_parcel.writeTypedArray(this.hosts, 0);
        _aidl_parcel.writeInt(this.tcMode);
        _aidl_parcel.writeInt(this.enforceDnsUid ? 1 : 0);
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
            this.hosts = (ResolverHostsParcel[]) _aidl_parcel.createTypedArray(ResolverHostsParcel.CREATOR);
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.tcMode = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.enforceDnsUid = _aidl_parcel.readInt() != 0;
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