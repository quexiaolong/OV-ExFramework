package com.vivo.services.proxy.transact;

import android.database.IContentObserver;
import android.net.Uri;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import com.vivo.common.utils.VLog;
import java.lang.reflect.Field;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class IContentObserverProxy extends BpTransactProxy implements IContentObserver {
    private static final String DESCRIPTOR = "android.database.IContentObserver";
    private static final String MEDIA_AUTHORITY = "media";
    private static int TRANSACTION_onChange;
    private static int TRANSACTION_onChangeEtc;

    static {
        TRANSACTION_onChange = -1;
        TRANSACTION_onChangeEtc = -1;
        try {
            Field f = IContentObserver.Stub.class.getDeclaredField("TRANSACTION_onChange");
            f.setAccessible(true);
            TRANSACTION_onChange = f.getInt(null);
        } catch (Exception e) {
            TransactProxyManager.CODE_READY = false;
            VLog.e(TransactProxyManager.TAG, "IContentObserverProxy init TRANSACTION_onChange fail:" + e.toString());
        }
        try {
            Field f2 = IContentObserver.Stub.class.getDeclaredField("TRANSACTION_onChangeEtc");
            f2.setAccessible(true);
            TRANSACTION_onChangeEtc = f2.getInt(null);
        } catch (Exception e2) {
            TransactProxyManager.CODE_READY = false;
            VLog.e(TransactProxyManager.TAG, "IContentObserverProxy init TRANSACTION_onChangeEtc fail:" + e2.toString());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public IContentObserverProxy(String descriptor, IBinder bpBinder, String pkg, String process, int uid, int pid) {
        super(descriptor, bpBinder, pkg, process, uid, pid);
    }

    @Override // com.vivo.services.proxy.transact.BpTransactProxy
    protected Transaction onTransactionCreated(int code, Parcel data) {
        return new MyTransaction(code, data);
    }

    @Override // com.vivo.services.proxy.transact.BpTransactProxy
    protected boolean onTansactionPreAdded(Transaction t) {
        MyTransaction my = (MyTransaction) t;
        return my.mPath != null && my.mMedia;
    }

    /* loaded from: classes.dex */
    private static class MyTransaction extends Transaction {
        boolean mMedia;
        String mPath;
        Uri mUri;
        int mUserId;

        MyTransaction(int code, Parcel data) {
            super(code, data);
            data.setDataPosition(0);
            data.enforceInterface(IContentObserverProxy.DESCRIPTOR);
            data.readInt();
            if (this.mCode == IContentObserverProxy.TRANSACTION_onChangeEtc) {
                Uri[] uris = (Uri[]) data.createTypedArray(Uri.CREATOR);
                if (uris != null && uris.length > 0) {
                    this.mUri = uris[uris.length - 1];
                }
                data.readInt();
            } else if (data.readInt() != 0) {
                this.mUri = (Uri) Uri.CREATOR.createFromParcel(data);
            }
            this.mUserId = data.readInt();
            initUri();
            recycleData();
        }

        void initUri() {
            Uri uri = this.mUri;
            if (uri != null) {
                String uri2 = uri.toString();
                int endIndex = uri2.lastIndexOf(47);
                if (endIndex > 0 && endIndex < uri2.length() - 2 && isNum(endIndex + 1, uri2)) {
                    this.mPath = uri2.substring(0, endIndex);
                } else {
                    this.mPath = uri2;
                }
                if (IContentObserverProxy.MEDIA_AUTHORITY.equals(this.mUri.getAuthority())) {
                    this.mMedia = true;
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        @Override // com.vivo.services.proxy.transact.Transaction
        public void send2Target(IBinder binder) throws RemoteException {
            if (binder == null || IContentObserverProxy.TRANSACTION_onChangeEtc == -1) {
                return;
            }
            Parcel data = Parcel.obtain();
            try {
                data.writeInterfaceToken(IContentObserverProxy.DESCRIPTOR);
                data.writeInt(0);
                if (this.mMedia) {
                    data.writeTypedArray(new Uri[]{Uri.parse(this.mPath)}, 0);
                } else {
                    data.writeTypedArray(new Uri[]{this.mUri}, 0);
                }
                data.writeInt(0);
                data.writeInt(this.mUserId);
                binder.transact(IContentObserverProxy.TRANSACTION_onChangeEtc, data, null, 1);
            } finally {
                data.recycle();
            }
        }

        boolean isNum(int fromIndex, String nums) {
            for (int i = fromIndex; i < nums.length(); i++) {
                if (!Character.isDigit(nums.charAt(i))) {
                    return false;
                }
            }
            return true;
        }

        @Override // com.vivo.services.proxy.transact.Transaction
        public String toString() {
            return String.format("code=%d uri=%s path=%s userId=%d", Integer.valueOf(this.mCode), this.mUri, this.mPath, Integer.valueOf(this.mUserId));
        }

        @Override // com.vivo.services.proxy.transact.Transaction
        public boolean isEqual(Transaction t) {
            MyTransaction other = (MyTransaction) t;
            if (this.mMedia != other.mMedia || this.mUserId != other.mUserId) {
                return false;
            }
            return this.mPath.equals(other.mPath);
        }
    }

    public void onChange(boolean selfUpdate, Uri uri, int userId) throws RemoteException {
    }

    public void onChangeEtc(boolean selfChange, Uri[] uris, int flags, int userId) {
    }

    public IBinder asBinder() {
        return null;
    }
}