package vendor.vivo.hardware.camera.provider.V1_0;

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
import vendor.vivo.hardware.camera.jpegencoder.V1_0.IJpegEncoder;
import vendor.vivo.hardware.camera.vif.V1_0.ICameraVIF;
import vendor.vivo.hardware.camera.vif.V1_0.ICameraVIFCallback;
import vendor.vivo.hardware.camera.vif3ainfotransmitter.V1_0.IVIF3AInfoCallback;
import vendor.vivo.hardware.camera.vif3ainfotransmitter.V1_0.IVIF3AInfoCallbackToHAL;
import vendor.vivo.hardware.camera.vivodevice.V1_0.IVivoCameraDevice;
import vendor.vivo.hardware.camera.vivodevice.V1_0.IVivoCameraDeviceCallback;
import vendor.vivo.hardware.camera.vivopostproc.V1_0.IVivoPostProc;
import vendor.vivo.hardware.camera.vivoreprocess.V1_0.IVivoReprocessModule;

/* loaded from: classes.dex */
public interface IVivoCameraProvider extends IBase {
    public static final String kInterfaceName = "vendor.vivo.hardware.camera.provider@1.0::IVivoCameraProvider";

    @Override // com.android.vivoservices.x.android.hidl.base.V1_0.IBase
    IHwBinder asBinder();

    @Override // com.android.vivoservices.x.android.hidl.base.V1_0.IBase
    void debug(NativeHandle nativeHandle, ArrayList<String> arrayList) throws RemoteException;

    @Override // com.android.vivoservices.x.android.hidl.base.V1_0.IBase
    DebugInfo getDebugInfo() throws RemoteException;

    @Override // com.android.vivoservices.x.android.hidl.base.V1_0.IBase
    ArrayList<byte[]> getHashChain() throws RemoteException;

    IJpegEncoder getJpegInterface_V1_X() throws RemoteException;

    IVIF3AInfoCallback getVIF3AInfoCallback() throws RemoteException;

    int getVersion() throws RemoteException;

    ICameraVIF getVifInterface_V1_X(ICameraVIFCallback iCameraVIFCallback) throws RemoteException;

    IVivoCameraDevice getVivoCameraDevice() throws RemoteException;

    ICameraVIF getVivoCameraVIF() throws RemoteException;

    IVivoCameraDevice getVivoDeviceInterface_V1_X(IVivoCameraDeviceCallback iVivoCameraDeviceCallback) throws RemoteException;

    IVivoPostProc getVivoPostProcInterface_V1_X() throws RemoteException;

    IVivoReprocessModule getVivoReprocessModule_V1_X() throws RemoteException;

    int getparam(int i) throws RemoteException;

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

    void setVIF3AInfoCallback(IVIF3AInfoCallback iVIF3AInfoCallback) throws RemoteException;

    void setVIF3AInfoCallbackForHAL(IVIF3AInfoCallbackToHAL iVIF3AInfoCallbackToHAL) throws RemoteException;

    int setparam(int i, int i2) throws RemoteException;

    @Override // com.android.vivoservices.x.android.hidl.base.V1_0.IBase
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    static IVivoCameraProvider asInterface(IHwBinder binder) {
        if (binder == null) {
            return null;
        }
        IHwInterface iface = binder.queryLocalInterface(kInterfaceName);
        if (iface != null && (iface instanceof IVivoCameraProvider)) {
            return (IVivoCameraProvider) iface;
        }
        IVivoCameraProvider proxy = new Proxy(binder);
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

    static IVivoCameraProvider castFrom(IHwInterface iface) {
        if (iface == null) {
            return null;
        }
        return asInterface(iface.asBinder());
    }

    static IVivoCameraProvider getService(String serviceName, boolean retry) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName, retry));
    }

    static IVivoCameraProvider getService(boolean retry) throws RemoteException {
        return getService("default", retry);
    }

    static IVivoCameraProvider getService(String serviceName) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName));
    }

    static IVivoCameraProvider getService() throws RemoteException {
        return getService("default");
    }

    /* loaded from: classes.dex */
    public static final class Proxy implements IVivoCameraProvider {
        private IHwBinder mRemote;

        public Proxy(IHwBinder remote) {
            Objects.requireNonNull(remote);
            this.mRemote = remote;
        }

        @Override // vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this.mRemote;
        }

        public String toString() {
            try {
                return interfaceDescriptor() + "@Proxy";
            } catch (RemoteException e) {
                return "[class or subclass of vendor.vivo.hardware.camera.provider@1.0::IVivoCameraProvider]@Proxy";
            }
        }

        public final boolean equals(Object other) {
            return HidlSupport.interfacesEqual(this, other);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override // vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider
        public int getVersion() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IVivoCameraProvider.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(1, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_version = _hidl_reply.readInt32();
                return _hidl_out_version;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider
        public ICameraVIF getVifInterface_V1_X(ICameraVIFCallback callback) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IVivoCameraProvider.kInterfaceName);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(2, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                ICameraVIF _hidl_out_vif = ICameraVIF.asInterface(_hidl_reply.readStrongBinder());
                return _hidl_out_vif;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider
        public IVivoCameraDevice getVivoDeviceInterface_V1_X(IVivoCameraDeviceCallback callback) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IVivoCameraProvider.kInterfaceName);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(3, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                IVivoCameraDevice _hidl_out_vivodevice = IVivoCameraDevice.asInterface(_hidl_reply.readStrongBinder());
                return _hidl_out_vivodevice;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider
        public ICameraVIF getVivoCameraVIF() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IVivoCameraProvider.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(4, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                ICameraVIF _hidl_out_vif = ICameraVIF.asInterface(_hidl_reply.readStrongBinder());
                return _hidl_out_vif;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider
        public IVivoCameraDevice getVivoCameraDevice() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IVivoCameraProvider.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(5, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                IVivoCameraDevice _hidl_out_vivodevice = IVivoCameraDevice.asInterface(_hidl_reply.readStrongBinder());
                return _hidl_out_vivodevice;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider
        public int setparam(int tag, int param) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IVivoCameraProvider.kInterfaceName);
            _hidl_request.writeInt32(tag);
            _hidl_request.writeInt32(param);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(6, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider
        public int getparam(int tag) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IVivoCameraProvider.kInterfaceName);
            _hidl_request.writeInt32(tag);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(7, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_ret = _hidl_reply.readInt32();
                return _hidl_out_ret;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider
        public void setVIF3AInfoCallback(IVIF3AInfoCallback callback) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IVivoCameraProvider.kInterfaceName);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(8, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider
        public IVIF3AInfoCallback getVIF3AInfoCallback() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IVivoCameraProvider.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(9, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                IVIF3AInfoCallback _hidl_out_callback = IVIF3AInfoCallback.asInterface(_hidl_reply.readStrongBinder());
                return _hidl_out_callback;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider
        public void setVIF3AInfoCallbackForHAL(IVIF3AInfoCallbackToHAL callback) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IVivoCameraProvider.kInterfaceName);
            _hidl_request.writeStrongBinder(callback == null ? null : callback.asBinder());
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(10, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider
        public IVivoReprocessModule getVivoReprocessModule_V1_X() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IVivoCameraProvider.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(11, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                IVivoReprocessModule _hidl_out_reprocessModule = IVivoReprocessModule.asInterface(_hidl_reply.readStrongBinder());
                return _hidl_out_reprocessModule;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider
        public IJpegEncoder getJpegInterface_V1_X() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IVivoCameraProvider.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(12, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                IJpegEncoder _hidl_out_jpegencoder = IJpegEncoder.asInterface(_hidl_reply.readStrongBinder());
                return _hidl_out_jpegencoder;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider
        public IVivoPostProc getVivoPostProcInterface_V1_X() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IVivoCameraProvider.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(13, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                IVivoPostProc _hidl_out_postproc = IVivoPostProc.asInterface(_hidl_reply.readStrongBinder());
                return _hidl_out_postproc;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
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

        @Override // vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
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

        @Override // vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
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

        @Override // vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
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

        @Override // vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
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

        @Override // vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) throws RemoteException {
            return this.mRemote.linkToDeath(recipient, cookie);
        }

        @Override // vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
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

        @Override // vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
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

        @Override // vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
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

        @Override // vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) throws RemoteException {
            return this.mRemote.unlinkToDeath(recipient);
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends HwBinder implements IVivoCameraProvider {
        @Override // vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this;
        }

        @Override // vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(IVivoCameraProvider.kInterfaceName, IBase.kInterfaceName));
        }

        @Override // vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public void debug(NativeHandle fd, ArrayList<String> options) {
        }

        @Override // vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public final String interfaceDescriptor() {
            return IVivoCameraProvider.kInterfaceName;
        }

        @Override // vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, new byte[]{-20, Byte.MAX_VALUE, -41, -98, -48, 45, -6, -123, -68, 73, -108, 38, -83, -82, 62, -66, 35, -17, 5, 36, -13, -51, 105, 87, 19, -109, 36, -72, 59, 24, -54, 76}));
        }

        @Override // vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public final void setHALInstrumentation() {
        }

        @Override // vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public final boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) {
            return true;
        }

        @Override // vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public final void ping() {
        }

        @Override // vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public final DebugInfo getDebugInfo() {
            DebugInfo info = new DebugInfo();
            info.pid = HidlSupport.getPidIfSharable();
            info.ptr = 0L;
            info.arch = 0;
            return info;
        }

        @Override // vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public final void notifySyspropsChanged() {
            HwBinder.enableInstrumentation();
        }

        @Override // vendor.vivo.hardware.camera.provider.V1_0.IVivoCameraProvider, com.android.vivoservices.x.android.hidl.base.V1_0.IBase
        public final boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) {
            return true;
        }

        public IHwInterface queryLocalInterface(String descriptor) {
            if (IVivoCameraProvider.kInterfaceName.equals(descriptor)) {
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
                    _hidl_request.enforceInterface(IVivoCameraProvider.kInterfaceName);
                    int _hidl_out_version = getVersion();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_version);
                    _hidl_reply.send();
                    return;
                case 2:
                    _hidl_request.enforceInterface(IVivoCameraProvider.kInterfaceName);
                    ICameraVIFCallback callback = ICameraVIFCallback.asInterface(_hidl_request.readStrongBinder());
                    ICameraVIF _hidl_out_vif = getVifInterface_V1_X(callback);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeStrongBinder(_hidl_out_vif != null ? _hidl_out_vif.asBinder() : null);
                    _hidl_reply.send();
                    return;
                case 3:
                    _hidl_request.enforceInterface(IVivoCameraProvider.kInterfaceName);
                    IVivoCameraDeviceCallback callback2 = IVivoCameraDeviceCallback.asInterface(_hidl_request.readStrongBinder());
                    IVivoCameraDevice _hidl_out_vivodevice = getVivoDeviceInterface_V1_X(callback2);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeStrongBinder(_hidl_out_vivodevice != null ? _hidl_out_vivodevice.asBinder() : null);
                    _hidl_reply.send();
                    return;
                case 4:
                    _hidl_request.enforceInterface(IVivoCameraProvider.kInterfaceName);
                    ICameraVIF _hidl_out_vif2 = getVivoCameraVIF();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeStrongBinder(_hidl_out_vif2 != null ? _hidl_out_vif2.asBinder() : null);
                    _hidl_reply.send();
                    return;
                case 5:
                    _hidl_request.enforceInterface(IVivoCameraProvider.kInterfaceName);
                    IVivoCameraDevice _hidl_out_vivodevice2 = getVivoCameraDevice();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeStrongBinder(_hidl_out_vivodevice2 != null ? _hidl_out_vivodevice2.asBinder() : null);
                    _hidl_reply.send();
                    return;
                case 6:
                    _hidl_request.enforceInterface(IVivoCameraProvider.kInterfaceName);
                    int tag = _hidl_request.readInt32();
                    int param = _hidl_request.readInt32();
                    int _hidl_out_ret = setparam(tag, param);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_ret);
                    _hidl_reply.send();
                    return;
                case 7:
                    _hidl_request.enforceInterface(IVivoCameraProvider.kInterfaceName);
                    int tag2 = _hidl_request.readInt32();
                    int _hidl_out_ret2 = getparam(tag2);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_ret2);
                    _hidl_reply.send();
                    return;
                case 8:
                    _hidl_request.enforceInterface(IVivoCameraProvider.kInterfaceName);
                    IVIF3AInfoCallback callback3 = IVIF3AInfoCallback.asInterface(_hidl_request.readStrongBinder());
                    setVIF3AInfoCallback(callback3);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.send();
                    return;
                case 9:
                    _hidl_request.enforceInterface(IVivoCameraProvider.kInterfaceName);
                    IVIF3AInfoCallback _hidl_out_callback = getVIF3AInfoCallback();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeStrongBinder(_hidl_out_callback != null ? _hidl_out_callback.asBinder() : null);
                    _hidl_reply.send();
                    return;
                case 10:
                    _hidl_request.enforceInterface(IVivoCameraProvider.kInterfaceName);
                    IVIF3AInfoCallbackToHAL callback4 = IVIF3AInfoCallbackToHAL.asInterface(_hidl_request.readStrongBinder());
                    setVIF3AInfoCallbackForHAL(callback4);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.send();
                    return;
                case 11:
                    _hidl_request.enforceInterface(IVivoCameraProvider.kInterfaceName);
                    IVivoReprocessModule _hidl_out_reprocessModule = getVivoReprocessModule_V1_X();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeStrongBinder(_hidl_out_reprocessModule != null ? _hidl_out_reprocessModule.asBinder() : null);
                    _hidl_reply.send();
                    return;
                case 12:
                    _hidl_request.enforceInterface(IVivoCameraProvider.kInterfaceName);
                    IJpegEncoder _hidl_out_jpegencoder = getJpegInterface_V1_X();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeStrongBinder(_hidl_out_jpegencoder != null ? _hidl_out_jpegencoder.asBinder() : null);
                    _hidl_reply.send();
                    return;
                case 13:
                    _hidl_request.enforceInterface(IVivoCameraProvider.kInterfaceName);
                    IVivoPostProc _hidl_out_postproc = getVivoPostProcInterface_V1_X();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeStrongBinder(_hidl_out_postproc != null ? _hidl_out_postproc.asBinder() : null);
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