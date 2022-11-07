package android.net;

import android.os.Parcel;
import android.os.Parcelable;

/* loaded from: classes.dex */
public class TetherStatsParcel implements Parcelable {
    public static final Parcelable.Creator<TetherStatsParcel> CREATOR = new Parcelable.Creator<TetherStatsParcel>() { // from class: android.net.TetherStatsParcel.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TetherStatsParcel createFromParcel(Parcel _aidl_source) {
            TetherStatsParcel _aidl_out = new TetherStatsParcel();
            _aidl_out.readFromParcel(_aidl_source);
            return _aidl_out;
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TetherStatsParcel[] newArray(int _aidl_size) {
            return new TetherStatsParcel[_aidl_size];
        }
    };
    public int ifIndex = 0;
    public String iface;
    public long rxBytes;
    public long rxPackets;
    public long txBytes;
    public long txPackets;

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel _aidl_parcel, int _aidl_flag) {
        int _aidl_start_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.writeInt(0);
        _aidl_parcel.writeString(this.iface);
        _aidl_parcel.writeLong(this.rxBytes);
        _aidl_parcel.writeLong(this.rxPackets);
        _aidl_parcel.writeLong(this.txBytes);
        _aidl_parcel.writeLong(this.txPackets);
        _aidl_parcel.writeInt(this.ifIndex);
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
            this.iface = _aidl_parcel.readString();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.rxBytes = _aidl_parcel.readLong();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.rxPackets = _aidl_parcel.readLong();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.txBytes = _aidl_parcel.readLong();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.txPackets = _aidl_parcel.readLong();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.ifIndex = _aidl_parcel.readInt();
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