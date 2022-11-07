package android.net;

import android.os.Parcel;
import android.os.Parcelable;

/* loaded from: classes.dex */
public class DataStallReportParcelable implements Parcelable {
    public static final Parcelable.Creator<DataStallReportParcelable> CREATOR = new Parcelable.Creator<DataStallReportParcelable>() { // from class: android.net.DataStallReportParcelable.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public DataStallReportParcelable createFromParcel(Parcel _aidl_source) {
            DataStallReportParcelable _aidl_out = new DataStallReportParcelable();
            _aidl_out.readFromParcel(_aidl_source);
            return _aidl_out;
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public DataStallReportParcelable[] newArray(int _aidl_size) {
            return new DataStallReportParcelable[_aidl_size];
        }
    };
    public long timestampMillis = 0;
    public int detectionMethod = 1;
    public int tcpPacketFailRate = 2;
    public int tcpMetricsCollectionPeriodMillis = 3;
    public int dnsConsecutiveTimeouts = 4;

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel _aidl_parcel, int _aidl_flag) {
        int _aidl_start_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.writeInt(0);
        _aidl_parcel.writeLong(this.timestampMillis);
        _aidl_parcel.writeInt(this.detectionMethod);
        _aidl_parcel.writeInt(this.tcpPacketFailRate);
        _aidl_parcel.writeInt(this.tcpMetricsCollectionPeriodMillis);
        _aidl_parcel.writeInt(this.dnsConsecutiveTimeouts);
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
            this.timestampMillis = _aidl_parcel.readLong();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.detectionMethod = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.tcpPacketFailRate = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.tcpMetricsCollectionPeriodMillis = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.dnsConsecutiveTimeouts = _aidl_parcel.readInt();
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