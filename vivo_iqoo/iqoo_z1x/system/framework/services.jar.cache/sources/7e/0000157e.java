package com.android.server.pm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.DataLoaderParamsParcel;
import android.content.pm.IDataLoader;
import android.content.pm.IDataLoaderManager;
import android.content.pm.IDataLoaderStatusListener;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.SystemService;
import java.util.List;

/* loaded from: classes.dex */
public class DataLoaderManagerService extends SystemService {
    private static final String TAG = "DataLoaderManager";
    private final DataLoaderManagerBinderService mBinderService;
    private final Context mContext;
    private SparseArray<DataLoaderServiceConnection> mServiceConnections;

    public DataLoaderManagerService(Context context) {
        super(context);
        this.mServiceConnections = new SparseArray<>();
        this.mContext = context;
        this.mBinderService = new DataLoaderManagerBinderService();
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService("dataloader_manager", this.mBinderService);
    }

    /* loaded from: classes.dex */
    final class DataLoaderManagerBinderService extends IDataLoaderManager.Stub {
        DataLoaderManagerBinderService() {
        }

        public boolean bindToDataLoader(int dataLoaderId, DataLoaderParamsParcel params, IDataLoaderStatusListener listener) {
            synchronized (DataLoaderManagerService.this.mServiceConnections) {
                if (DataLoaderManagerService.this.mServiceConnections.get(dataLoaderId) != null) {
                    return true;
                }
                ComponentName componentName = new ComponentName(params.packageName, params.className);
                ComponentName dataLoaderComponent = resolveDataLoaderComponentName(componentName);
                if (dataLoaderComponent == null) {
                    Slog.e(DataLoaderManagerService.TAG, "Invalid component: " + componentName + " for ID=" + dataLoaderId);
                    return false;
                }
                DataLoaderServiceConnection connection = new DataLoaderServiceConnection(dataLoaderId, listener);
                Intent intent = new Intent();
                intent.setComponent(dataLoaderComponent);
                if (DataLoaderManagerService.this.mContext.bindServiceAsUser(intent, connection, 1, UserHandle.of(UserHandle.getCallingUserId()))) {
                    return true;
                }
                Slog.e(DataLoaderManagerService.TAG, "Failed to bind to: " + dataLoaderComponent + " for ID=" + dataLoaderId);
                DataLoaderManagerService.this.mContext.unbindService(connection);
                return false;
            }
        }

        private ComponentName resolveDataLoaderComponentName(ComponentName componentName) {
            PackageManager pm = DataLoaderManagerService.this.mContext.getPackageManager();
            if (pm == null) {
                Slog.e(DataLoaderManagerService.TAG, "PackageManager is not available.");
                return null;
            }
            Intent intent = new Intent("android.intent.action.LOAD_DATA");
            intent.setComponent(componentName);
            List<ResolveInfo> services = pm.queryIntentServicesAsUser(intent, 0, UserHandle.getCallingUserId());
            if (services == null || services.isEmpty()) {
                Slog.e(DataLoaderManagerService.TAG, "Failed to find data loader service provider in " + componentName);
                return null;
            }
            int numServices = services.size();
            if (0 < numServices) {
                ResolveInfo ri = services.get(0);
                ComponentName resolved = new ComponentName(ri.serviceInfo.packageName, ri.serviceInfo.name);
                return resolved;
            }
            Slog.e(DataLoaderManagerService.TAG, "Didn't find any matching data loader service provider.");
            return null;
        }

        public IDataLoader getDataLoader(int dataLoaderId) {
            synchronized (DataLoaderManagerService.this.mServiceConnections) {
                DataLoaderServiceConnection serviceConnection = (DataLoaderServiceConnection) DataLoaderManagerService.this.mServiceConnections.get(dataLoaderId, null);
                if (serviceConnection == null) {
                    return null;
                }
                return serviceConnection.getDataLoader();
            }
        }

        public void unbindFromDataLoader(int dataLoaderId) {
            synchronized (DataLoaderManagerService.this.mServiceConnections) {
                DataLoaderServiceConnection serviceConnection = (DataLoaderServiceConnection) DataLoaderManagerService.this.mServiceConnections.get(dataLoaderId, null);
                if (serviceConnection == null) {
                    return;
                }
                serviceConnection.destroy();
            }
        }
    }

    /* loaded from: classes.dex */
    private class DataLoaderServiceConnection implements ServiceConnection {
        IDataLoader mDataLoader = null;
        final int mId;
        final IDataLoaderStatusListener mListener;

        DataLoaderServiceConnection(int id, IDataLoaderStatusListener listener) {
            this.mId = id;
            this.mListener = listener;
        }

        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName className, IBinder service) {
            IDataLoader asInterface = IDataLoader.Stub.asInterface(service);
            this.mDataLoader = asInterface;
            if (asInterface == null) {
                onNullBinding(className);
            } else if (!append()) {
                DataLoaderManagerService.this.mContext.unbindService(this);
            } else {
                callListener(1);
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName arg0) {
            Slog.i(DataLoaderManagerService.TAG, "DataLoader " + this.mId + " disconnected, but will try to recover");
            callListener(0);
            destroy();
        }

        @Override // android.content.ServiceConnection
        public void onBindingDied(ComponentName name) {
            Slog.i(DataLoaderManagerService.TAG, "DataLoader " + this.mId + " died");
            callListener(0);
            destroy();
        }

        @Override // android.content.ServiceConnection
        public void onNullBinding(ComponentName name) {
            Slog.i(DataLoaderManagerService.TAG, "DataLoader " + this.mId + " failed to start");
            callListener(0);
            destroy();
        }

        IDataLoader getDataLoader() {
            return this.mDataLoader;
        }

        void destroy() {
            IDataLoader iDataLoader = this.mDataLoader;
            if (iDataLoader != null) {
                try {
                    iDataLoader.destroy(this.mId);
                } catch (RemoteException e) {
                }
                this.mDataLoader = null;
            }
            try {
                DataLoaderManagerService.this.mContext.unbindService(this);
            } catch (Exception e2) {
            }
            remove();
        }

        private boolean append() {
            synchronized (DataLoaderManagerService.this.mServiceConnections) {
                DataLoaderServiceConnection bound = (DataLoaderServiceConnection) DataLoaderManagerService.this.mServiceConnections.get(this.mId);
                if (bound == this) {
                    return true;
                }
                if (bound == null) {
                    DataLoaderManagerService.this.mServiceConnections.append(this.mId, this);
                    return true;
                }
                return false;
            }
        }

        private void remove() {
            synchronized (DataLoaderManagerService.this.mServiceConnections) {
                if (DataLoaderManagerService.this.mServiceConnections.get(this.mId) == this) {
                    DataLoaderManagerService.this.mServiceConnections.remove(this.mId);
                }
            }
        }

        private void callListener(int status) {
            IDataLoaderStatusListener iDataLoaderStatusListener = this.mListener;
            if (iDataLoaderStatusListener != null) {
                try {
                    iDataLoaderStatusListener.onStatusChanged(this.mId, status);
                } catch (RemoteException e) {
                }
            }
        }
    }
}