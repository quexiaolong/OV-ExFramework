package android.net.ipmemorystore;

/* loaded from: classes.dex */
public class Status {
    public static final int ERROR_DATABASE_CANNOT_BE_OPENED = -3;
    public static final int ERROR_GENERIC = -1;
    public static final int ERROR_ILLEGAL_ARGUMENT = -2;
    public static final int ERROR_STORAGE = -4;
    public static final int ERROR_UNKNOWN = -5;
    public static final int SUCCESS = 0;
    public final int resultCode;

    public Status(int resultCode) {
        this.resultCode = resultCode;
    }

    public Status(StatusParcelable parcelable) {
        this(parcelable.resultCode);
    }

    public StatusParcelable toParcelable() {
        StatusParcelable parcelable = new StatusParcelable();
        parcelable.resultCode = this.resultCode;
        return parcelable;
    }

    public boolean isSuccess() {
        return this.resultCode == 0;
    }

    public String toString() {
        int i = this.resultCode;
        if (i != -4) {
            if (i != -3) {
                if (i != -2) {
                    if (i != -1) {
                        if (i == 0) {
                            return "SUCCESS";
                        }
                        return "Unknown value ?!";
                    }
                    return "GENERIC ERROR";
                }
                return "ILLEGAL ARGUMENT";
            }
            return "DATABASE CANNOT BE OPENED";
        }
        return "DATABASE STORAGE ERROR";
    }
}