package android.os.oplusdevicepolicy;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.oplusdevicepolicy.IOplusDevicePolicyManagerService;
import android.os.oplusdevicepolicy.IOplusDevicePolicyObserver;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import java.util.List;

/* loaded from: classes.dex */
public class OplusDevicepolicyManager {
    public static final int CUSTOMIZE_DATA_TYPE = 1;
    public static final String SERVICE_NAME = "oplusdevicepolicy";
    public static final int SYSTEM_DATA_TYPE = 0;
    private static final String TAG = "OplusDevicePolicyManager";
    private final ArrayMap<OplusDevicePolicyObserver, IOplusDevicePolicyObserver> mOplusDevicePolicyObservers = new ArrayMap<>();
    private IOplusDevicePolicyManagerService mOplusDevicepolicyManagerService;
    private static final Object mServiceLock = new Object();
    private static final Object mLock = new Object();
    private static volatile OplusDevicepolicyManager sInstance = null;

    /* loaded from: classes.dex */
    public interface OplusDevicePolicyObserver {
        void onOplusDevicePolicyUpdate(String str, String str2);

        void onOplusDevicePolicyUpdate(String str, List<String> list);
    }

    private OplusDevicepolicyManager() {
        getOplusDevicepolicyManagerService();
    }

    public static final OplusDevicepolicyManager getInstance() {
        OplusDevicepolicyManager oplusDevicepolicyManager;
        if (sInstance == null) {
            synchronized (mLock) {
                if (sInstance == null) {
                    sInstance = new OplusDevicepolicyManager();
                }
                oplusDevicepolicyManager = sInstance;
            }
            return oplusDevicepolicyManager;
        }
        return sInstance;
    }

    private IOplusDevicePolicyManagerService getOplusDevicepolicyManagerService() {
        IOplusDevicePolicyManagerService iOplusDevicePolicyManagerService;
        synchronized (mServiceLock) {
            if (this.mOplusDevicepolicyManagerService == null) {
                this.mOplusDevicepolicyManagerService = IOplusDevicePolicyManagerService.Stub.asInterface(ServiceManager.getService(SERVICE_NAME));
            }
            iOplusDevicePolicyManagerService = this.mOplusDevicepolicyManagerService;
        }
        return iOplusDevicePolicyManagerService;
    }

    public boolean setData(String name, String value, int datatype) {
        String str = TAG;
        boolean ret = false;
        try {
            getOplusDevicepolicyManagerService();
            IOplusDevicePolicyManagerService iOplusDevicePolicyManagerService = this.mOplusDevicepolicyManagerService;
            if (iOplusDevicePolicyManagerService == null) {
                Slog.d(TAG, "mOplusDevicepolicyManagerService is null");
                str = str;
            } else {
                boolean data = iOplusDevicePolicyManagerService.setData(name, value, datatype);
                ret = data;
                str = data;
            }
        } catch (RemoteException e) {
            Slog.e(str, "setData fail!", e);
        } catch (Exception e2) {
            Slog.e(str, "setData Error" + e2);
        }
        return ret;
    }

    public String getData(String name, int datatype) {
        String str = TAG;
        String ret = null;
        try {
            getOplusDevicepolicyManagerService();
            if (this.mOplusDevicepolicyManagerService == null) {
                Slog.d(TAG, "mOplusDevicepolicyManagerService is null");
            } else {
                str = getOplusDevicepolicyManagerService().getData(name, datatype);
                ret = str;
            }
        } catch (RemoteException e) {
            Slog.e(str, "getData fail!", e);
        } catch (Exception e2) {
            Slog.e(str, "getData Error" + e2);
        }
        return ret;
    }

    public boolean getBoolean(String name, int datatype, boolean defaultvalue) {
        String mData = getData(name, datatype);
        if (mData == null) {
            return defaultvalue;
        }
        return Boolean.valueOf(mData).booleanValue();
    }

    /* JADX WARN: Multi-variable type inference failed */
    public boolean setList(String name, List list, int datatype) {
        String str = TAG;
        boolean ret = false;
        try {
            getOplusDevicepolicyManagerService();
            if (this.mOplusDevicepolicyManagerService == null) {
                Slog.d(TAG, "mOplusDevicepolicyManagerService is null");
                str = str;
            } else {
                boolean list2 = getOplusDevicepolicyManagerService().setList(name, list, datatype);
                ret = list2;
                str = list2;
            }
        } catch (RemoteException e) {
            Slog.e(str, "setList fail!", e);
        } catch (Exception e2) {
            Slog.e(str, "setList Error" + e2);
        }
        return ret;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public boolean addList(String name, List list, int datatype) {
        String str = TAG;
        boolean ret = false;
        try {
            getOplusDevicepolicyManagerService();
            if (this.mOplusDevicepolicyManagerService == null) {
                Slog.d(TAG, "mOplusDevicepolicyManagerService is null");
                str = str;
            } else {
                boolean addList = getOplusDevicepolicyManagerService().addList(name, list, datatype);
                ret = addList;
                str = addList;
            }
        } catch (RemoteException e) {
            Slog.e(str, "addList fail!", e);
        } catch (Exception e2) {
            Slog.e(str, "addList Error" + e2);
        }
        return ret;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public List<String> getList(String name, int datatype) {
        String str = TAG;
        List<String> ret = null;
        try {
            getOplusDevicepolicyManagerService();
            if (this.mOplusDevicepolicyManagerService == null) {
                Slog.d(TAG, "mOplusDevicepolicyManagerService is null");
                str = str;
            } else {
                List<String> list = getOplusDevicepolicyManagerService().getList(name, datatype);
                ret = list;
                str = list;
            }
        } catch (RemoteException e) {
            Slog.e(str, "getList fail!", e);
        } catch (Exception e2) {
            Slog.e(str, "getList Error" + e2);
        }
        return ret;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public boolean removeData(String name, int datatype) {
        String str = TAG;
        boolean ret = false;
        try {
            getOplusDevicepolicyManagerService();
            if (this.mOplusDevicepolicyManagerService == null) {
                Slog.d(TAG, "mOplusDevicepolicyManagerService is null");
                str = str;
            } else {
                boolean removeData = getOplusDevicepolicyManagerService().removeData(name, datatype);
                ret = removeData;
                str = removeData;
            }
        } catch (RemoteException e) {
            Slog.e(str, "removeData fail!", e);
        } catch (Exception e2) {
            Slog.e(str, "removeData Error" + e2);
        }
        return ret;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public boolean removeList(String name, int datatype) {
        String str = TAG;
        boolean ret = false;
        try {
            getOplusDevicepolicyManagerService();
            if (this.mOplusDevicepolicyManagerService == null) {
                Slog.d(TAG, "mOplusDevicepolicyManagerService is null");
                str = str;
            } else {
                boolean removeList = getOplusDevicepolicyManagerService().removeList(name, datatype);
                ret = removeList;
                str = removeList;
            }
        } catch (RemoteException e) {
            Slog.e(str, "removeList fail!", e);
        } catch (Exception e2) {
            Slog.e(str, "removeList Error" + e2);
        }
        return ret;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public boolean removePartListData(String name, List list, int datatype) {
        String str = TAG;
        boolean ret = false;
        try {
            getOplusDevicepolicyManagerService();
            if (this.mOplusDevicepolicyManagerService == null) {
                Slog.d(TAG, "mOplusDevicepolicyManagerService is null");
                str = str;
            } else {
                boolean removePartListData = getOplusDevicepolicyManagerService().removePartListData(name, list, datatype);
                ret = removePartListData;
                str = removePartListData;
            }
        } catch (RemoteException e) {
            Slog.e(str, "removePartListData fail!", e);
        } catch (Exception e2) {
            Slog.e(str, "removePartListData Error" + e2);
        }
        return ret;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public boolean clearData(int datatype) {
        String str = TAG;
        boolean ret = false;
        try {
            getOplusDevicepolicyManagerService();
            if (this.mOplusDevicepolicyManagerService == null) {
                Slog.d(TAG, "mOplusDevicepolicyManagerService is null");
                str = str;
            } else {
                boolean clearData = getOplusDevicepolicyManagerService().clearData(datatype);
                ret = clearData;
                str = clearData;
            }
        } catch (RemoteException e) {
            Slog.e(str, "clearData fail!", e);
        } catch (Exception e2) {
            Slog.e(str, "clearData Error" + e2);
        }
        return ret;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public boolean clearList(int datatype) {
        String str = TAG;
        boolean ret = false;
        try {
            getOplusDevicepolicyManagerService();
            if (this.mOplusDevicepolicyManagerService == null) {
                Slog.d(TAG, "mOplusDevicepolicyManagerService is null");
                str = str;
            } else {
                boolean clearList = getOplusDevicepolicyManagerService().clearList(datatype);
                ret = clearList;
                str = clearList;
            }
        } catch (RemoteException e) {
            Slog.e(str, "clearList fail!", e);
        } catch (Exception e2) {
            Slog.e(str, "clearList Error" + e2);
        }
        return ret;
    }

    public boolean registerOplusDevicepolicyObserver(String name, OplusDevicePolicyObserver observer) {
        boolean ret = false;
        if (observer == null) {
            Log.e(TAG, " registerOplusDevicePolicyObserver null observer");
            return false;
        }
        synchronized (this.mOplusDevicePolicyObservers) {
            if (this.mOplusDevicePolicyObservers.get(observer) != null) {
                Log.e(TAG, "already regiter before");
                return false;
            }
            OplusDevicePolicyObserverDelegate delegate = new OplusDevicePolicyObserverDelegate(observer);
            try {
                getOplusDevicepolicyManagerService();
                if (this.mOplusDevicepolicyManagerService != null) {
                    ret = getOplusDevicepolicyManagerService().registerOplusDevicePolicyObserver(name, delegate);
                } else {
                    Slog.d(TAG, "mOplusDevicepolicyManagerService is null");
                }
                if (ret) {
                    this.mOplusDevicePolicyObservers.put(observer, delegate);
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "registerOplusDevicePolicyObserver fail!", e);
            } catch (Exception e2) {
                Slog.e(TAG, "registerOplusDevicePolicyObserver Error" + e2);
            }
            return ret;
        }
    }

    public boolean unregisterOplusDevicePolicyObserver(OplusDevicePolicyObserver observer) {
        boolean ret = false;
        if (observer == null) {
            Log.i(TAG, "unregisterOplusDevicepolicyObserver null observer");
            return false;
        }
        synchronized (this.mOplusDevicePolicyObservers) {
            IOplusDevicePolicyObserver delegate = this.mOplusDevicePolicyObservers.get(observer);
            if (delegate != null) {
                try {
                    getOplusDevicepolicyManagerService();
                    if (this.mOplusDevicepolicyManagerService != null) {
                        ret = getOplusDevicepolicyManagerService().unregisterOplusDevicePolicyObserver(delegate);
                    } else {
                        Slog.d(TAG, "mOplusDevicepolicyManagerService is null");
                    }
                    if (ret) {
                        this.mOplusDevicePolicyObservers.remove(observer);
                    }
                } catch (RemoteException e) {
                    Slog.e(TAG, "unregisterOplusDevicePolicyObserver fail!", e);
                } catch (Exception e2) {
                    Slog.e(TAG, "unregisterOplusDevicePolicyObserver Error" + e2);
                }
            }
        }
        return ret;
    }

    /* loaded from: classes.dex */
    private class OplusDevicePolicyObserverDelegate extends IOplusDevicePolicyObserver.Stub {
        private final OplusDevicePolicyObserver mObserver;

        public OplusDevicePolicyObserverDelegate(OplusDevicePolicyObserver observer) {
            this.mObserver = observer;
        }

        @Override // android.os.oplusdevicepolicy.IOplusDevicePolicyObserver
        public void onOplusDevicePolicyListUpdate(String name, List<String> list) {
            this.mObserver.onOplusDevicePolicyUpdate(name, list);
        }

        @Override // android.os.oplusdevicepolicy.IOplusDevicePolicyObserver
        public void onOplusDevicePolicyValueUpdate(String name, String value) {
            this.mObserver.onOplusDevicePolicyUpdate(name, value);
        }
    }
}