package android.os;

/* loaded from: classes.dex */
public interface OverlayablePolicy extends IInterface {
    public static final int ACTOR_SIGNATURE = 128;
    public static final int ODM_PARTITION = 32;
    public static final int OEM_PARTITION = 64;
    public static final int PRODUCT_PARTITION = 8;
    public static final int PUBLIC = 1;
    public static final int SIGNATURE = 16;
    public static final int SYSTEM_PARTITION = 2;
    public static final int VENDOR_PARTITION = 4;

    /* loaded from: classes.dex */
    public static class Default implements OverlayablePolicy {
        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements OverlayablePolicy {
        private static final String DESCRIPTOR = "android$os$OverlayablePolicy".replace('$', '.');

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static OverlayablePolicy asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof OverlayablePolicy)) {
                return (OverlayablePolicy) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code == 1598968902) {
                reply.writeString(descriptor);
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }

        /* loaded from: classes.dex */
        private static class Proxy implements OverlayablePolicy {
            public static OverlayablePolicy sDefaultImpl;
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }
        }

        public static boolean setDefaultImpl(OverlayablePolicy impl) {
            if (Proxy.sDefaultImpl != null) {
                throw new IllegalStateException("setDefaultImpl() called twice");
            }
            if (impl != null) {
                Proxy.sDefaultImpl = impl;
                return true;
            }
            return false;
        }

        public static OverlayablePolicy getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}