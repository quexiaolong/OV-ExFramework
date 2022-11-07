package vendor.vivo.hardware.camera.vivopostproc.V1_0;

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
public interface IVivoPostProc extends IBase {
    public static final String kInterfaceName = "vendor.vivo.hardware.camera.vivopostproc@1.0::IVivoPostProc";

    int DoPostProcess(RequestParams requestParams, ArrayList<Byte> arrayList, IVivoPostProcCallback iVivoPostProcCallback) throws RemoteException;

    int GetProcessCapability() throws RemoteException;

    @Override // com.android.vivoservices.x.android.hidl.base.V1_0.IBase
    IHwBinder asBinder();

    void close() throws RemoteException;

    int configureStreams() throws RemoteException;

    @Override // com.android.vivoservices.x.android.hidl.base.V1_0.IBase
    void debug(NativeHandle nativeHandle, ArrayList<String> arrayList) throws RemoteException;

    int flush() throws RemoteException;

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

    int processPostProcRequest() throws RemoteException;

    @Override // com.android.vivoservices.x.android.hidl.base.V1_0.IBase
    void setHALInstrumentation() throws RemoteException;

    @Override // com.android.vivoservices.x.android.hidl.base.V1_0.IBase
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    static IVivoPostProc asInterface(IHwBinder binder) {
        if (binder == null) {
            return null;
        }
        IHwInterface iface = binder.queryLocalInterface(kInterfaceName);
        if (iface != null && (iface instanceof IVivoPostProc)) {
            return (IVivoPostProc) iface;
        }
        IVivoPostProc proxy = new Proxy(binder);
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

    static IVivoPostProc castFrom(IHwInterface iface) {
        if (iface == null) {
            return null;
        }
        return asInterface(iface.asBinder());
    }

    static IVivoPostProc getService(String serviceName, boolean retry) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName, retry));
    }

    static IVivoPostProc getService(boolean retry) throws RemoteException {
        return getService("default", retry);
    }

    static IVivoPostProc getService(String serviceName) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName));
    }

    static IVivoPostProc getService() throws RemoteException {
        return getService("default");
    }

    /* loaded from: classes.dex */
    public static final class Proxy implements IVivoPostProc {
        private IHwBinder mRemote;

        public Proxy(IHwBinder remote) {
            Objects.requireNonNull(remote);
            this.mRemote = remote;
        }

        @Override // vendor.vivo.hardware.camera.vivopostproc.V1_0.IVivoPostProc, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this.mRemote;
        }

        public String toString() {
            try {
                return interfaceDescriptor() + "@Proxy";
            } catch (RemoteException e) {
                return "[class or subclass of vendor.vivo.hardware.camera.vivopostproc@1.0::IVivoPostProc]@Proxy";
            }
        }

        public final boolean equals(Object other) {
            return HidlSupport.interfacesEqual(this, other);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override // vendor.vivo.hardware.camera.vivopostproc.V1_0.IVivoPostProc
        public int configureStreams() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IVivoPostProc.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(1, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_status = _hidl_reply.readInt32();
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.vivo.hardware.camera.vivopostproc.V1_0.IVivoPostProc
        public int processPostProcRequest() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IVivoPostProc.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(2, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_status = _hidl_reply.readInt32();
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.vivo.hardware.camera.vivopostproc.V1_0.IVivoPostProc
        public int flush() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IVivoPostProc.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(3, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_status = _hidl_reply.readInt32();
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.vivo.hardware.camera.vivopostproc.V1_0.IVivoPostProc
        public void close() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IVivoPostProc.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(4, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.vivo.hardware.camera.vivopostproc.V1_0.IVivoPostProc
        public int GetProcessCapability() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IVivoPostProc.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(5, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_cap = _hidl_reply.readInt32();
                return _hidl_out_cap;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.vivo.hardware.camera.vivopostproc.V1_0.IVivoPostProc
        public int DoPostProcess(RequestParams RequestInfo, ArrayList<Byte> metadata, IVivoPostProcCallback callback) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IVivoPostProc.kInterfaceName);
            RequestInfo.writeToParcel(_hidl_request);
            _hidl_request.writeInt8Vector(metadata);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(6, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_result = _hidl_reply.readInt32();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.vivo.hardware.camera.vivopostproc.V1_0.IVivoPostProc, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
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

        @Override // vendor.vivo.hardware.camera.vivopostproc.V1_0.IVivoPostProc, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
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

        @Override // vendor.vivo.hardware.camera.vivopostproc.V1_0.IVivoPostProc, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
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

        @Override // vendor.vivo.hardware.camera.vivopostproc.V1_0.IVivoPostProc, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
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

        @Override // vendor.vivo.hardware.camera.vivopostproc.V1_0.IVivoPostProc, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
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

        @Override // vendor.vivo.hardware.camera.vivopostproc.V1_0.IVivoPostProc, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) throws RemoteException {
            return this.mRemote.linkToDeath(recipient, cookie);
        }

        @Override // vendor.vivo.hardware.camera.vivopostproc.V1_0.IVivoPostProc, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
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

        @Override // vendor.vivo.hardware.camera.vivopostproc.V1_0.IVivoPostProc, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
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

        @Override // vendor.vivo.hardware.camera.vivopostproc.V1_0.IVivoPostProc, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
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

        @Override // vendor.vivo.hardware.camera.vivopostproc.V1_0.IVivoPostProc, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) throws RemoteException {
            return this.mRemote.unlinkToDeath(recipient);
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends HwBinder implements IVivoPostProc {
        @Override // vendor.vivo.hardware.camera.vivopostproc.V1_0.IVivoPostProc, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this;
        }

        @Override // vendor.vivo.hardware.camera.vivopostproc.V1_0.IVivoPostProc, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(IVivoPostProc.kInterfaceName, IBase.kInterfaceName));
        }

        @Override // vendor.vivo.hardware.camera.vivopostproc.V1_0.IVivoPostProc, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public void debug(NativeHandle fd, ArrayList<String> options) {
        }

        @Override // vendor.vivo.hardware.camera.vivopostproc.V1_0.IVivoPostProc, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public final String interfaceDescriptor() {
            return IVivoPostProc.kInterfaceName;
        }

        @Override // vendor.vivo.hardware.camera.vivopostproc.V1_0.IVivoPostProc, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, new byte[]{-20, Byte.MAX_VALUE, -41, -98, -48, 45, -6, -123, -68, 73, -108, 38, -83, -82, 62, -66, 35, -17, 5, 36, -13, -51, 105, 87, 19, -109, 36, -72, 59, 24, -54, 76}));
        }

        @Override // vendor.vivo.hardware.camera.vivopostproc.V1_0.IVivoPostProc, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public final void setHALInstrumentation() {
        }

        @Override // vendor.vivo.hardware.camera.vivopostproc.V1_0.IVivoPostProc, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public final boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) {
            return true;
        }

        @Override // vendor.vivo.hardware.camera.vivopostproc.V1_0.IVivoPostProc, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public final void ping() {
        }

        @Override // vendor.vivo.hardware.camera.vivopostproc.V1_0.IVivoPostProc, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public final DebugInfo getDebugInfo() {
            DebugInfo info = new DebugInfo();
            info.pid = HidlSupport.getPidIfSharable();
            info.ptr = 0L;
            info.arch = 0;
            return info;
        }

        @Override // vendor.vivo.hardware.camera.vivopostproc.V1_0.IVivoPostProc, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public final void notifySyspropsChanged() {
            HwBinder.enableInstrumentation();
        }

        @Override // vendor.vivo.hardware.camera.vivopostproc.V1_0.IVivoPostProc, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public final boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) {
            return true;
        }

        public IHwInterface queryLocalInterface(String descriptor) {
            if (IVivoPostProc.kInterfaceName.equals(descriptor)) {
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
            switch (_hidl_code) {
                case 1:
                    _hidl_request.enforceInterface(IVivoPostProc.kInterfaceName);
                    int _hidl_out_status = configureStreams();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_status);
                    _hidl_reply.send();
                    return;
                case 2:
                    _hidl_request.enforceInterface(IVivoPostProc.kInterfaceName);
                    int _hidl_out_status2 = processPostProcRequest();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_status2);
                    _hidl_reply.send();
                    return;
                case 3:
                    _hidl_request.enforceInterface(IVivoPostProc.kInterfaceName);
                    int _hidl_out_status3 = flush();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_status3);
                    _hidl_reply.send();
                    return;
                case 4:
                    _hidl_request.enforceInterface(IVivoPostProc.kInterfaceName);
                    close();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.send();
                    return;
                case 5:
                    _hidl_request.enforceInterface(IVivoPostProc.kInterfaceName);
                    int _hidl_out_cap = GetProcessCapability();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_cap);
                    _hidl_reply.send();
                    return;
                case 6:
                    _hidl_request.enforceInterface(IVivoPostProc.kInterfaceName);
                    RequestParams RequestInfo = new RequestParams();
                    RequestInfo.readFromParcel(_hidl_request);
                    ArrayList<Byte> metadata = _hidl_request.readInt8Vector();
                    IVivoPostProcCallback callback = IVivoPostProcCallback.asInterface(_hidl_request.readStrongBinder());
                    int _hidl_out_result = DoPostProcess(RequestInfo, metadata, callback);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_result);
                    _hidl_reply.send();
                    return;
                default:
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