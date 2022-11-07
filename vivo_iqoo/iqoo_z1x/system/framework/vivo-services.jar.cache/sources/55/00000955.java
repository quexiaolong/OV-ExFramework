package vendor.vivoservices.x.vendor.factory.hardware.vivoem.V1_0;

import android.os.HidlSupport;
import android.os.HwBinder;
import android.os.HwBlob;
import android.os.HwParcel;
import android.os.IHwBinder;
import android.os.IHwInterface;
import android.os.NativeHandle;
import android.os.RemoteException;
import com.android.vivoservices.x.android.hidl.base.V1_0.DebugInfo;
import com.android.vivoservices.x.android.hidl.base.V1_0.IBase;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

/* loaded from: classes.dex */
public interface IEmCallback extends IBase {
    public static final String kInterfaceName = "vendor.factory.hardware.vivoem@1.0::IEmCallback";

    @Override // com.android.vivoservices.x.android.hidl.base.V1_0.IBase
    IHwBinder asBinder();

    String callbackEngineerMode(String str) throws RemoteException;

    boolean callbackToClient(String str, String str2, String str3, String str4) throws RemoteException;

    @Override // com.android.vivoservices.x.android.hidl.base.V1_0.IBase
    void debug(NativeHandle nativeHandle, ArrayList<String> arrayList) throws RemoteException;

    @Override // com.android.vivoservices.x.android.hidl.base.V1_0.IBase
    DebugInfo getDebugInfo() throws RemoteException;

    @Override // com.android.vivoservices.x.android.hidl.base.V1_0.IBase
    ArrayList<byte[]> getHashChain() throws RemoteException;

    @Override // com.android.vivoservices.x.android.hidl.base.V1_0.IBase
    ArrayList<String> interfaceChain() throws RemoteException;

    @Override // com.android.vivoservices.x.android.hidl.base.V1_0.IBase
    String interfaceDescriptor() throws RemoteException;

    @Override // com.android.vivoservices.x.android.hidl.base.V1_0.IBase
    boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) throws RemoteException;

    @Override // com.android.vivoservices.x.android.hidl.base.V1_0.IBase
    void notifySyspropsChanged() throws RemoteException;

    @Override // com.android.vivoservices.x.android.hidl.base.V1_0.IBase
    void ping() throws RemoteException;

    @Override // com.android.vivoservices.x.android.hidl.base.V1_0.IBase
    void setHALInstrumentation() throws RemoteException;

    @Override // com.android.vivoservices.x.android.hidl.base.V1_0.IBase
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    static IEmCallback asInterface(IHwBinder binder) {
        if (binder == null) {
            return null;
        }
        IHwInterface iface = binder.queryLocalInterface(kInterfaceName);
        if (iface != null && (iface instanceof IEmCallback)) {
            return (IEmCallback) iface;
        }
        IEmCallback proxy = new Proxy(binder);
        try {
            Iterator<String> it = proxy.interfaceChain().iterator();
            while (it.hasNext()) {
                String descriptor = it.next();
                if (descriptor.equals(kInterfaceName)) {
                    return proxy;
                }
            }
        } catch (RemoteException e) {
        }
        return null;
    }

    static IEmCallback castFrom(IHwInterface iface) {
        if (iface == null) {
            return null;
        }
        return asInterface(iface.asBinder());
    }

    static IEmCallback getService(String serviceName, boolean retry) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName, retry));
    }

    static IEmCallback getService(boolean retry) throws RemoteException {
        return getService("default", retry);
    }

    static IEmCallback getService(String serviceName) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName));
    }

    static IEmCallback getService() throws RemoteException {
        return getService("default");
    }

    /* loaded from: classes.dex */
    public static final class Proxy implements IEmCallback {
        private IHwBinder mRemote;

        public Proxy(IHwBinder remote) {
            Objects.requireNonNull(remote);
            this.mRemote = remote;
        }

        @Override // vendor.vivoservices.x.vendor.factory.hardware.vivoem.V1_0.IEmCallback, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this.mRemote;
        }

        public String toString() {
            try {
                return interfaceDescriptor() + "@Proxy";
            } catch (RemoteException e) {
                return "[class or subclass of vendor.factory.hardware.vivoem@1.0::IEmCallback]@Proxy";
            }
        }

        public final boolean equals(Object other) {
            return HidlSupport.interfacesEqual(this, other);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override // vendor.vivoservices.x.vendor.factory.hardware.vivoem.V1_0.IEmCallback
        public boolean callbackToClient(String data, String name, String action, String extra) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IEmCallback.kInterfaceName);
            _hidl_request.writeString(data);
            _hidl_request.writeString(name);
            _hidl_request.writeString(action);
            _hidl_request.writeString(extra);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(1, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                boolean _hidl_out_success = _hidl_reply.readBool();
                return _hidl_out_success;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.vivoservices.x.vendor.factory.hardware.vivoem.V1_0.IEmCallback
        public String callbackEngineerMode(String data) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IEmCallback.kInterfaceName);
            _hidl_request.writeString(data);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(2, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                String _hidl_out_rsp = _hidl_reply.readString();
                return _hidl_out_rsp;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.vivoservices.x.vendor.factory.hardware.vivoem.V1_0.IEmCallback, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public ArrayList<String> interfaceChain() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256067662, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                ArrayList<String> _hidl_out_descriptors = _hidl_reply.readStringVector();
                return _hidl_out_descriptors;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.vivoservices.x.vendor.factory.hardware.vivoem.V1_0.IEmCallback, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public void debug(NativeHandle fd, ArrayList<String> options) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            _hidl_request.writeNativeHandle(fd);
            _hidl_request.writeStringVector(options);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256131655, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.vivoservices.x.vendor.factory.hardware.vivoem.V1_0.IEmCallback, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public String interfaceDescriptor() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256136003, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                String _hidl_out_descriptor = _hidl_reply.readString();
                return _hidl_out_descriptor;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.vivoservices.x.vendor.factory.hardware.vivoem.V1_0.IEmCallback, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public ArrayList<byte[]> getHashChain() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256398152, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                ArrayList<byte[]> _hidl_out_hashchain = new ArrayList<>();
                HwBlob _hidl_blob = _hidl_reply.readBuffer(16L);
                int _hidl_vec_size = _hidl_blob.getInt32(8L);
                HwBlob childBlob = _hidl_reply.readEmbeddedBuffer(_hidl_vec_size * 32, _hidl_blob.handle(), 0L, true);
                _hidl_out_hashchain.clear();
                for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                    byte[] _hidl_vec_element = new byte[32];
                    long _hidl_array_offset_1 = _hidl_index_0 * 32;
                    childBlob.copyToInt8Array(_hidl_array_offset_1, _hidl_vec_element, 32);
                    _hidl_out_hashchain.add(_hidl_vec_element);
                }
                return _hidl_out_hashchain;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.vivoservices.x.vendor.factory.hardware.vivoem.V1_0.IEmCallback, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public void setHALInstrumentation() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256462420, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.vivoservices.x.vendor.factory.hardware.vivoem.V1_0.IEmCallback, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) throws RemoteException {
            return this.mRemote.linkToDeath(recipient, cookie);
        }

        @Override // vendor.vivoservices.x.vendor.factory.hardware.vivoem.V1_0.IEmCallback, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public void ping() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256921159, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.vivoservices.x.vendor.factory.hardware.vivoem.V1_0.IEmCallback, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public DebugInfo getDebugInfo() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(257049926, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                DebugInfo _hidl_out_info = new DebugInfo();
                _hidl_out_info.readFromParcel(_hidl_reply);
                return _hidl_out_info;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.vivoservices.x.vendor.factory.hardware.vivoem.V1_0.IEmCallback, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public void notifySyspropsChanged() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(257120595, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.vivoservices.x.vendor.factory.hardware.vivoem.V1_0.IEmCallback, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) throws RemoteException {
            return this.mRemote.unlinkToDeath(recipient);
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends HwBinder implements IEmCallback {
        @Override // vendor.vivoservices.x.vendor.factory.hardware.vivoem.V1_0.IEmCallback, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this;
        }

        @Override // vendor.vivoservices.x.vendor.factory.hardware.vivoem.V1_0.IEmCallback, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(IEmCallback.kInterfaceName, IBase.kInterfaceName));
        }

        @Override // vendor.vivoservices.x.vendor.factory.hardware.vivoem.V1_0.IEmCallback, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public void debug(NativeHandle fd, ArrayList<String> options) {
        }

        @Override // vendor.vivoservices.x.vendor.factory.hardware.vivoem.V1_0.IEmCallback, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public final String interfaceDescriptor() {
            return IEmCallback.kInterfaceName;
        }

        @Override // vendor.vivoservices.x.vendor.factory.hardware.vivoem.V1_0.IEmCallback, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{-111, 86, -70, 102, -95, -47, -19, 39, 93, -74, 55, 56, -56, -41, 120, -79, 56, -61, 100, 93, -23, -95, -24, 62, 55, 3, 72, 93, -80, -14, -45, -99}, new byte[]{-20, Byte.MAX_VALUE, -41, -98, -48, 45, -6, -123, -68, 73, -108, 38, -83, -82, 62, -66, 35, -17, 5, 36, -13, -51, 105, 87, 19, -109, 36, -72, 59, 24, -54, 76}));
        }

        @Override // vendor.vivoservices.x.vendor.factory.hardware.vivoem.V1_0.IEmCallback, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public final void setHALInstrumentation() {
        }

        @Override // vendor.vivoservices.x.vendor.factory.hardware.vivoem.V1_0.IEmCallback, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public final boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) {
            return true;
        }

        @Override // vendor.vivoservices.x.vendor.factory.hardware.vivoem.V1_0.IEmCallback, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public final void ping() {
        }

        @Override // vendor.vivoservices.x.vendor.factory.hardware.vivoem.V1_0.IEmCallback, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public final DebugInfo getDebugInfo() {
            DebugInfo info = new DebugInfo();
            info.pid = HidlSupport.getPidIfSharable();
            info.ptr = 0L;
            info.arch = 0;
            return info;
        }

        @Override // vendor.vivoservices.x.vendor.factory.hardware.vivoem.V1_0.IEmCallback, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public final void notifySyspropsChanged() {
            HwBinder.enableInstrumentation();
        }

        @Override // vendor.vivoservices.x.vendor.factory.hardware.vivoem.V1_0.IEmCallback, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public final boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) {
            return true;
        }

        public IHwInterface queryLocalInterface(String descriptor) {
            if (IEmCallback.kInterfaceName.equals(descriptor)) {
                return this;
            }
            return null;
        }

        public void registerAsService(String serviceName) throws RemoteException {
            registerService(serviceName);
        }

        public String toString() {
            return interfaceDescriptor() + "@Stub";
        }

        public void onTransact(int _hidl_code, HwParcel _hidl_request, HwParcel _hidl_reply, int _hidl_flags) throws RemoteException {
            if (_hidl_code == 1) {
                _hidl_request.enforceInterface(IEmCallback.kInterfaceName);
                String data = _hidl_request.readString();
                String name = _hidl_request.readString();
                String action = _hidl_request.readString();
                String extra = _hidl_request.readString();
                boolean _hidl_out_success = callbackToClient(data, name, action, extra);
                _hidl_reply.writeStatus(0);
                _hidl_reply.writeBool(_hidl_out_success);
                _hidl_reply.send();
            } else if (_hidl_code == 2) {
                _hidl_request.enforceInterface(IEmCallback.kInterfaceName);
                String data2 = _hidl_request.readString();
                String _hidl_out_rsp = callbackEngineerMode(data2);
                _hidl_reply.writeStatus(0);
                _hidl_reply.writeString(_hidl_out_rsp);
                _hidl_reply.send();
            } else {
                switch (_hidl_code) {
                    case 256067662:
                        _hidl_request.enforceInterface(IBase.kInterfaceName);
                        ArrayList<String> _hidl_out_descriptors = interfaceChain();
                        _hidl_reply.writeStatus(0);
                        _hidl_reply.writeStringVector(_hidl_out_descriptors);
                        _hidl_reply.send();
                        return;
                    case 256131655:
                        _hidl_request.enforceInterface(IBase.kInterfaceName);
                        NativeHandle fd = _hidl_request.readNativeHandle();
                        ArrayList<String> options = _hidl_request.readStringVector();
                        debug(fd, options);
                        _hidl_reply.writeStatus(0);
                        _hidl_reply.send();
                        return;
                    case 256136003:
                        _hidl_request.enforceInterface(IBase.kInterfaceName);
                        String _hidl_out_descriptor = interfaceDescriptor();
                        _hidl_reply.writeStatus(0);
                        _hidl_reply.writeString(_hidl_out_descriptor);
                        _hidl_reply.send();
                        return;
                    case 256398152:
                        _hidl_request.enforceInterface(IBase.kInterfaceName);
                        ArrayList<byte[]> _hidl_out_hashchain = getHashChain();
                        _hidl_reply.writeStatus(0);
                        HwBlob _hidl_blob = new HwBlob(16);
                        int _hidl_vec_size = _hidl_out_hashchain.size();
                        _hidl_blob.putInt32(8L, _hidl_vec_size);
                        _hidl_blob.putBool(12L, false);
                        HwBlob childBlob = new HwBlob(_hidl_vec_size * 32);
                        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                            long _hidl_array_offset_1 = _hidl_index_0 * 32;
                            byte[] _hidl_array_item_1 = _hidl_out_hashchain.get(_hidl_index_0);
                            if (_hidl_array_item_1 == null || _hidl_array_item_1.length != 32) {
                                throw new IllegalArgumentException("Array element is not of the expected length");
                            }
                            childBlob.putInt8Array(_hidl_array_offset_1, _hidl_array_item_1);
                        }
                        _hidl_blob.putBlob(0L, childBlob);
                        _hidl_reply.writeBuffer(_hidl_blob);
                        _hidl_reply.send();
                        return;
                    case 256462420:
                        _hidl_request.enforceInterface(IBase.kInterfaceName);
                        setHALInstrumentation();
                        return;
                    case 256921159:
                        _hidl_request.enforceInterface(IBase.kInterfaceName);
                        ping();
                        _hidl_reply.writeStatus(0);
                        _hidl_reply.send();
                        return;
                    case 257049926:
                        _hidl_request.enforceInterface(IBase.kInterfaceName);
                        DebugInfo _hidl_out_info = getDebugInfo();
                        _hidl_reply.writeStatus(0);
                        _hidl_out_info.writeToParcel(_hidl_reply);
                        _hidl_reply.send();
                        return;
                    case 257120595:
                        _hidl_request.enforceInterface(IBase.kInterfaceName);
                        notifySyspropsChanged();
                        return;
                    default:
                        return;
                }
            }
        }
    }
}