package android.gsi;

import android.os.Parcel;
import android.os.Parcelable;

/* loaded from: classes.dex */
public class AvbPublicKey implements Parcelable {
    public static final Parcelable.Creator<AvbPublicKey> CREATOR = new Parcelable.Creator<AvbPublicKey>() { // from class: android.gsi.AvbPublicKey.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public AvbPublicKey createFromParcel(Parcel _aidl_source) {
            AvbPublicKey _aidl_out = new AvbPublicKey();
            _aidl_out.readFromParcel(_aidl_source);
            return _aidl_out;
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public AvbPublicKey[] newArray(int _aidl_size) {
            return new AvbPublicKey[_aidl_size];
        }
    };
    public byte[] bytes;
    public byte[] sha1;

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel _aidl_parcel, int _aidl_flag) {
        int _aidl_start_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.writeInt(0);
        _aidl_parcel.writeByteArray(this.bytes);
        _aidl_parcel.writeByteArray(this.sha1);
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
            this.bytes = _aidl_parcel.createByteArray();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.sha1 = _aidl_parcel.createByteArray();
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