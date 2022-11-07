package android.net.shared;

import android.net.Layer2InformationParcelable;
import android.net.MacAddress;
import java.util.Objects;

/* loaded from: classes.dex */
public class Layer2Information {
    public final MacAddress mBssid;
    public final String mCluster;
    public final String mL2Key;

    public Layer2Information(String l2Key, String cluster, MacAddress bssid) {
        this.mL2Key = l2Key;
        this.mCluster = cluster;
        this.mBssid = bssid;
    }

    public String toString() {
        StringBuffer str = new StringBuffer();
        str.append("L2Key: ");
        str.append(this.mL2Key);
        str.append(", Cluster: ");
        str.append(this.mCluster);
        str.append(", bssid: ");
        str.append(this.mBssid);
        return str.toString();
    }

    public Layer2InformationParcelable toStableParcelable() {
        Layer2InformationParcelable p = new Layer2InformationParcelable();
        p.l2Key = this.mL2Key;
        p.cluster = this.mCluster;
        p.bssid = this.mBssid;
        return p;
    }

    public static Layer2Information fromStableParcelable(Layer2InformationParcelable p) {
        if (p == null) {
            return null;
        }
        return new Layer2Information(p.l2Key, p.cluster, p.bssid);
    }

    public boolean equals(Object obj) {
        if (obj instanceof Layer2Information) {
            Layer2Information other = (Layer2Information) obj;
            return Objects.equals(this.mL2Key, other.mL2Key) && Objects.equals(this.mCluster, other.mCluster) && Objects.equals(this.mBssid, other.mBssid);
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(this.mL2Key, this.mCluster, this.mBssid);
    }
}