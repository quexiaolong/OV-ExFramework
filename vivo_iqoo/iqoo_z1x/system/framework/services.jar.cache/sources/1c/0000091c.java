package com.android.server.autofill;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.hardware.tv.cec.V1_0.CecMessageType;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.UserHandle;
import android.service.autofill.IAutofillFieldClassificationService;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import android.view.autofill.AutofillValue;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class FieldClassificationStrategy {
    private static final String TAG = "FieldClassificationStrategy";
    private final Context mContext;
    private final Object mLock = new Object();
    private ArrayList<Command> mQueuedCommands;
    private IAutofillFieldClassificationService mRemoteService;
    private ServiceConnection mServiceConnection;
    private final int mUserId;

    /* loaded from: classes.dex */
    public interface Command {
        void run(IAutofillFieldClassificationService iAutofillFieldClassificationService) throws RemoteException;
    }

    /* loaded from: classes.dex */
    public interface MetadataParser<T> {
        T get(Resources resources, int i);
    }

    public FieldClassificationStrategy(Context context, int userId) {
        this.mContext = context;
        this.mUserId = userId;
    }

    public ServiceInfo getServiceInfo() {
        String packageName = this.mContext.getPackageManager().getServicesSystemSharedLibraryPackageName();
        if (packageName == null) {
            Slog.w(TAG, "no external services package!");
            return null;
        }
        Intent intent = new Intent("android.service.autofill.AutofillFieldClassificationService");
        intent.setPackage(packageName);
        ResolveInfo resolveInfo = this.mContext.getPackageManager().resolveService(intent, CecMessageType.REPORT_PHYSICAL_ADDRESS);
        if (resolveInfo == null || resolveInfo.serviceInfo == null) {
            Slog.w(TAG, "No valid components found.");
            return null;
        }
        return resolveInfo.serviceInfo;
    }

    private ComponentName getServiceComponentName() {
        ServiceInfo serviceInfo = getServiceInfo();
        if (serviceInfo == null) {
            return null;
        }
        ComponentName name = new ComponentName(serviceInfo.packageName, serviceInfo.name);
        if (!"android.permission.BIND_AUTOFILL_FIELD_CLASSIFICATION_SERVICE".equals(serviceInfo.permission)) {
            Slog.w(TAG, name.flattenToShortString() + " does not require permission android.permission.BIND_AUTOFILL_FIELD_CLASSIFICATION_SERVICE");
            return null;
        }
        if (Helper.sVerbose) {
            Slog.v(TAG, "getServiceComponentName(): " + name);
        }
        return name;
    }

    public void reset() {
        synchronized (this.mLock) {
            if (this.mServiceConnection != null) {
                if (Helper.sDebug) {
                    Slog.d(TAG, "reset(): unbinding service.");
                }
                try {
                    this.mContext.unbindService(this.mServiceConnection);
                } catch (IllegalArgumentException e) {
                    Slog.w(TAG, "reset(): " + e.getMessage());
                }
                this.mServiceConnection = null;
            } else if (Helper.sDebug) {
                Slog.d(TAG, "reset(): service is not bound. Do nothing.");
            }
        }
    }

    private void connectAndRun(Command command) {
        synchronized (this.mLock) {
            if (this.mRemoteService != null) {
                try {
                    if (Helper.sVerbose) {
                        Slog.v(TAG, "running command right away");
                    }
                    command.run(this.mRemoteService);
                } catch (RemoteException e) {
                    Slog.w(TAG, "exception calling service: " + e);
                }
                return;
            }
            if (Helper.sDebug) {
                Slog.d(TAG, "service is null; queuing command");
            }
            if (this.mQueuedCommands == null) {
                this.mQueuedCommands = new ArrayList<>(1);
            }
            this.mQueuedCommands.add(command);
            if (this.mServiceConnection != null) {
                return;
            }
            if (Helper.sVerbose) {
                Slog.v(TAG, "creating connection");
            }
            this.mServiceConnection = new ServiceConnection() { // from class: com.android.server.autofill.FieldClassificationStrategy.1
                {
                    FieldClassificationStrategy.this = this;
                }

                @Override // android.content.ServiceConnection
                public void onServiceConnected(ComponentName name, IBinder service) {
                    if (Helper.sVerbose) {
                        Slog.v(FieldClassificationStrategy.TAG, "onServiceConnected(): " + name);
                    }
                    synchronized (FieldClassificationStrategy.this.mLock) {
                        FieldClassificationStrategy.this.mRemoteService = IAutofillFieldClassificationService.Stub.asInterface(service);
                        if (FieldClassificationStrategy.this.mQueuedCommands != null) {
                            int size = FieldClassificationStrategy.this.mQueuedCommands.size();
                            if (Helper.sDebug) {
                                Slog.d(FieldClassificationStrategy.TAG, "running " + size + " queued commands");
                            }
                            for (int i = 0; i < size; i++) {
                                Command queuedCommand = (Command) FieldClassificationStrategy.this.mQueuedCommands.get(i);
                                try {
                                    if (Helper.sVerbose) {
                                        Slog.v(FieldClassificationStrategy.TAG, "running queued command #" + i);
                                    }
                                    queuedCommand.run(FieldClassificationStrategy.this.mRemoteService);
                                } catch (RemoteException e2) {
                                    Slog.w(FieldClassificationStrategy.TAG, "exception calling " + name + ": " + e2);
                                }
                            }
                            FieldClassificationStrategy.this.mQueuedCommands = null;
                        } else if (Helper.sDebug) {
                            Slog.d(FieldClassificationStrategy.TAG, "no queued commands");
                        }
                    }
                }

                @Override // android.content.ServiceConnection
                public void onServiceDisconnected(ComponentName name) {
                    if (Helper.sVerbose) {
                        Slog.v(FieldClassificationStrategy.TAG, "onServiceDisconnected(): " + name);
                    }
                    synchronized (FieldClassificationStrategy.this.mLock) {
                        FieldClassificationStrategy.this.mRemoteService = null;
                    }
                }

                @Override // android.content.ServiceConnection
                public void onBindingDied(ComponentName name) {
                    if (Helper.sVerbose) {
                        Slog.v(FieldClassificationStrategy.TAG, "onBindingDied(): " + name);
                    }
                    synchronized (FieldClassificationStrategy.this.mLock) {
                        FieldClassificationStrategy.this.mRemoteService = null;
                    }
                }

                @Override // android.content.ServiceConnection
                public void onNullBinding(ComponentName name) {
                    if (Helper.sVerbose) {
                        Slog.v(FieldClassificationStrategy.TAG, "onNullBinding(): " + name);
                    }
                    synchronized (FieldClassificationStrategy.this.mLock) {
                        FieldClassificationStrategy.this.mRemoteService = null;
                    }
                }
            };
            ComponentName component = getServiceComponentName();
            if (Helper.sVerbose) {
                Slog.v(TAG, "binding to: " + component);
            }
            if (component != null) {
                Intent intent = new Intent();
                intent.setComponent(component);
                long token = Binder.clearCallingIdentity();
                this.mContext.bindServiceAsUser(intent, this.mServiceConnection, 1, UserHandle.of(this.mUserId));
                if (Helper.sVerbose) {
                    Slog.v(TAG, "bound");
                }
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    public String[] getAvailableAlgorithms() {
        return (String[]) getMetadataValue("android.autofill.field_classification.available_algorithms", $$Lambda$FieldClassificationStrategy$NQQgQ63vxhPkiwOWrnwRyuYSHTM.INSTANCE);
    }

    public String getDefaultAlgorithm() {
        return (String) getMetadataValue("android.autofill.field_classification.default_algorithm", $$Lambda$FieldClassificationStrategy$vGIL1YGX_9ksoSV74T7gO4fkEBE.INSTANCE);
    }

    private <T> T getMetadataValue(String field, MetadataParser<T> parser) {
        ServiceInfo serviceInfo = getServiceInfo();
        if (serviceInfo == null) {
            return null;
        }
        PackageManager pm = this.mContext.getPackageManager();
        try {
            Resources res = pm.getResourcesForApplication(serviceInfo.applicationInfo);
            int resourceId = serviceInfo.metaData.getInt(field);
            return parser.get(res, resourceId);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting application resources for " + serviceInfo, e);
            return null;
        }
    }

    public static /* synthetic */ void lambda$calculateScores$2(RemoteCallback callback, List actualValues, String[] userDataValues, String[] categoryIds, String defaultAlgorithm, Bundle defaultArgs, ArrayMap algorithms, ArrayMap args, IAutofillFieldClassificationService service) throws RemoteException {
        service.calculateScores(callback, actualValues, userDataValues, categoryIds, defaultAlgorithm, defaultArgs, algorithms, args);
    }

    public void calculateScores(final RemoteCallback callback, final List<AutofillValue> actualValues, final String[] userDataValues, final String[] categoryIds, final String defaultAlgorithm, final Bundle defaultArgs, final ArrayMap<String, String> algorithms, final ArrayMap<String, Bundle> args) {
        connectAndRun(new Command() { // from class: com.android.server.autofill.-$$Lambda$FieldClassificationStrategy$cXTbqmCb6-V5mVc5dTOipqK5X_E
            @Override // com.android.server.autofill.FieldClassificationStrategy.Command
            public final void run(IAutofillFieldClassificationService iAutofillFieldClassificationService) {
                FieldClassificationStrategy.lambda$calculateScores$2(callback, actualValues, userDataValues, categoryIds, defaultAlgorithm, defaultArgs, algorithms, args, iAutofillFieldClassificationService);
            }
        });
    }

    public void dump(String prefix, PrintWriter pw) {
        ComponentName impl = getServiceComponentName();
        pw.print(prefix);
        pw.print("User ID: ");
        pw.println(this.mUserId);
        pw.print(prefix);
        pw.print("Queued commands: ");
        ArrayList<Command> arrayList = this.mQueuedCommands;
        if (arrayList == null) {
            pw.println("N/A");
        } else {
            pw.println(arrayList.size());
        }
        pw.print(prefix);
        pw.print("Implementation: ");
        if (impl == null) {
            pw.println("N/A");
            return;
        }
        pw.println(impl.flattenToShortString());
        try {
            pw.print(prefix);
            pw.print("Available algorithms: ");
            pw.println(Arrays.toString(getAvailableAlgorithms()));
            pw.print(prefix);
            pw.print("Default algorithm: ");
            pw.println(getDefaultAlgorithm());
        } catch (Exception e) {
            pw.print("ERROR CALLING SERVICE: ");
            pw.println(e);
        }
    }
}