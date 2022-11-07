package android.net;

import android.os.Parcel;
import android.os.Parcelable;

/* loaded from: classes.dex */
public class ResolverParamsParcel implements Parcelable {
    public static final Parcelable.Creator<ResolverParamsParcel> CREATOR = new Parcelable.Creator<ResolverParamsParcel>() { // from class: android.net.ResolverParamsParcel.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ResolverParamsParcel createFromParcel(Parcel _aidl_source) {
            ResolverParamsParcel _aidl_out = new ResolverParamsParcel();
            _aidl_out.readFromParcel(_aidl_source);
            return _aidl_out;
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ResolverParamsParcel[] newArray(int _aidl_size) {
            return new ResolverParamsParcel[_aidl_size];
        }
    };
    public int baseTimeoutMsec;
    public String[] domains;
    public int maxSamples;
    public int minSamples;
    public int netId;
    public ResolverOptionsParcel resolverOptions;
    public int retryCount;
    public int sampleValiditySeconds;
    public String[] servers;
    public int successThreshold;
    public String tlsName;
    public String[] tlsServers;
    public String[] tlsFingerprints = new String[0];
    public String caCertificate = "";
    public int tlsConnectTimeoutMs = 0;
    public int[] transportTypes = new int[0];

    @Override // android.os.Parcelable
    public final void writeToParcel(Parcel _aidl_parcel, int _aidl_flag) {
        int _aidl_start_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.writeInt(0);
        _aidl_parcel.writeInt(this.netId);
        _aidl_parcel.writeInt(this.sampleValiditySeconds);
        _aidl_parcel.writeInt(this.successThreshold);
        _aidl_parcel.writeInt(this.minSamples);
        _aidl_parcel.writeInt(this.maxSamples);
        _aidl_parcel.writeInt(this.baseTimeoutMsec);
        _aidl_parcel.writeInt(this.retryCount);
        _aidl_parcel.writeStringArray(this.servers);
        _aidl_parcel.writeStringArray(this.domains);
        _aidl_parcel.writeString(this.tlsName);
        _aidl_parcel.writeStringArray(this.tlsServers);
        _aidl_parcel.writeStringArray(this.tlsFingerprints);
        _aidl_parcel.writeString(this.caCertificate);
        _aidl_parcel.writeInt(this.tlsConnectTimeoutMs);
        if (this.resolverOptions != null) {
            _aidl_parcel.writeInt(1);
            this.resolverOptions.writeToParcel(_aidl_parcel, 0);
        } else {
            _aidl_parcel.writeInt(0);
        }
        _aidl_parcel.writeIntArray(this.transportTypes);
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
            this.netId = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.sampleValiditySeconds = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.successThreshold = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.minSamples = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.maxSamples = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.baseTimeoutMsec = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.retryCount = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.servers = _aidl_parcel.createStringArray();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.domains = _aidl_parcel.createStringArray();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.tlsName = _aidl_parcel.readString();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.tlsServers = _aidl_parcel.createStringArray();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.tlsFingerprints = _aidl_parcel.createStringArray();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.caCertificate = _aidl_parcel.readString();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.tlsConnectTimeoutMs = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            if (_aidl_parcel.readInt() != 0) {
                this.resolverOptions = ResolverOptionsParcel.CREATOR.createFromParcel(_aidl_parcel);
            } else {
                this.resolverOptions = null;
            }
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) {
                return;
            }
            this.transportTypes = _aidl_parcel.createIntArray();
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