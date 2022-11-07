package android.net;

import android.net.util.NetworkConstants;
import android.os.Parcel;
import android.os.Parcelable;

/* loaded from: classes.dex */
public class TetherOffloadRuleParcel implements Parcelable {
    public static final Parcelable.Creator<TetherOffloadRuleParcel> CREATOR = new Parcelable.Creator<TetherOffloadRuleParcel>() { // from class: android.net.TetherOffloadRuleParcel.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TetherOffloadRuleParcel createFromParcel(Parcel _aidl_source) {
            TetherOffloadRuleParcel _aidl_out = new TetherOffloadRuleParcel();
            _aidl_out.readFromParcel(_aidl_source);
            return _aidl_out;
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public TetherOffloadRuleParcel[] newArray(int _aidl_size) {
            return new TetherOffloadRuleParcel[_aidl_size];
        }
    };
    public byte[] destination;
    public byte[] dstL2Address;
    public int inputInterfaceIndex;
    public int outputInterfaceIndex;
    public int pmtu = NetworkConstants.ETHER_MTU;
    public int prefixLength;
    public byte[] srcL2Address;

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel _aidl_parcel, int _aidl_flag) {
        int _aidl_start_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.writeInt(0);
        _aidl_parcel.writeInt(this.inputInterfaceIndex);
        _aidl_parcel.writeInt(this.outputInterfaceIndex);
        _aidl_parcel.writeByteArray(this.destination);
        _aidl_parcel.writeInt(this.prefixLength);
        _aidl_parcel.writeByteArray(this.srcL2Address);
        _aidl_parcel.writeByteArray(this.dstL2Address);
        _aidl_parcel.writeInt(this.pmtu);
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
            this.inputInterfaceIndex = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.outputInterfaceIndex = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.destination = _aidl_parcel.createByteArray();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.prefixLength = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.srcL2Address = _aidl_parcel.createByteArray();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.dstL2Address = _aidl_parcel.createByteArray();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.pmtu = _aidl_parcel.readInt();
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