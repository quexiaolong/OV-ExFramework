package com.android.server.tv;

import android.media.tv.ITvRemoteProvider;
import android.media.tv.ITvRemoteServiceInput;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.Slog;
import java.io.IOException;
import java.util.Map;

/* loaded from: classes2.dex */
final class TvRemoteServiceInput extends ITvRemoteServiceInput.Stub {
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_KEYS = false;
    private static final String TAG = "TvRemoteServiceInput";
    private final Map<IBinder, UinputBridge> mBridgeMap = new ArrayMap();
    private final Object mLock;
    private final ITvRemoteProvider mProvider;

    /* JADX INFO: Access modifiers changed from: package-private */
    public TvRemoteServiceInput(Object lock, ITvRemoteProvider provider) {
        this.mLock = lock;
        this.mProvider = provider;
    }

    public void openInputBridge(final IBinder token, String name, int width, int height, int maxPointers) {
        synchronized (this.mLock) {
            try {
            } catch (Throwable th) {
                th = th;
            }
            try {
                if (!this.mBridgeMap.containsKey(token)) {
                    long idToken = Binder.clearCallingIdentity();
                    try {
                        try {
                            try {
                                this.mBridgeMap.put(token, new UinputBridge(token, name, width, height, maxPointers));
                                token.linkToDeath(new IBinder.DeathRecipient() { // from class: com.android.server.tv.TvRemoteServiceInput.1
                                    @Override // android.os.IBinder.DeathRecipient
                                    public void binderDied() {
                                        TvRemoteServiceInput.this.closeInputBridge(token);
                                    }
                                }, 0);
                                Binder.restoreCallingIdentity(idToken);
                            } catch (RemoteException e) {
                                Slog.e(TAG, "Token is already dead");
                                closeInputBridge(token);
                                Binder.restoreCallingIdentity(idToken);
                                return;
                            }
                        } catch (IOException e2) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("Cannot create device for ");
                            try {
                                sb.append(name);
                                Slog.e(TAG, sb.toString());
                                Binder.restoreCallingIdentity(idToken);
                                return;
                            } catch (Throwable th2) {
                                e = th2;
                                Binder.restoreCallingIdentity(idToken);
                                throw e;
                            }
                        }
                    } catch (Throwable th3) {
                        e = th3;
                        Binder.restoreCallingIdentity(idToken);
                        throw e;
                    }
                }
                try {
                    this.mProvider.onInputBridgeConnected(token);
                } catch (RemoteException e3) {
                    Slog.e(TAG, "Failed remote call to onInputBridgeConnected");
                }
            } catch (Throwable th4) {
                th = th4;
                throw th;
            }
        }
    }

    public void openGamepadBridge(final IBinder token, String name) throws RemoteException {
        synchronized (this.mLock) {
            if (!this.mBridgeMap.containsKey(token)) {
                long idToken = Binder.clearCallingIdentity();
                try {
                    this.mBridgeMap.put(token, UinputBridge.openGamepad(token, name));
                    token.linkToDeath(new IBinder.DeathRecipient() { // from class: com.android.server.tv.TvRemoteServiceInput.2
                        @Override // android.os.IBinder.DeathRecipient
                        public void binderDied() {
                            TvRemoteServiceInput.this.closeInputBridge(token);
                        }
                    }, 0);
                    Binder.restoreCallingIdentity(idToken);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Token is already dead");
                    closeInputBridge(token);
                    Binder.restoreCallingIdentity(idToken);
                    return;
                } catch (IOException e2) {
                    Slog.e(TAG, "Cannot create device for " + name);
                    Binder.restoreCallingIdentity(idToken);
                    return;
                }
            }
        }
        try {
            this.mProvider.onInputBridgeConnected(token);
        } catch (RemoteException e3) {
            Slog.e(TAG, "Failed remote call to onInputBridgeConnected");
        }
    }

    public void closeInputBridge(IBinder token) {
        synchronized (this.mLock) {
            UinputBridge inputBridge = this.mBridgeMap.remove(token);
            if (inputBridge == null) {
                Slog.w(TAG, String.format("Input bridge not found for token: %s", token));
                return;
            }
            long idToken = Binder.clearCallingIdentity();
            inputBridge.close(token);
            Binder.restoreCallingIdentity(idToken);
        }
    }

    public void clearInputBridge(IBinder token) {
        synchronized (this.mLock) {
            UinputBridge inputBridge = this.mBridgeMap.get(token);
            if (inputBridge == null) {
                Slog.w(TAG, String.format("Input bridge not found for token: %s", token));
                return;
            }
            long idToken = Binder.clearCallingIdentity();
            inputBridge.clear(token);
            Binder.restoreCallingIdentity(idToken);
        }
    }

    public void sendTimestamp(IBinder token, long timestamp) {
    }

    public void sendKeyDown(IBinder token, int keyCode) {
        synchronized (this.mLock) {
            UinputBridge inputBridge = this.mBridgeMap.get(token);
            if (inputBridge == null) {
                Slog.w(TAG, String.format("Input bridge not found for token: %s", token));
                return;
            }
            long idToken = Binder.clearCallingIdentity();
            inputBridge.sendKeyDown(token, keyCode);
            Binder.restoreCallingIdentity(idToken);
        }
    }

    public void sendKeyUp(IBinder token, int keyCode) {
        synchronized (this.mLock) {
            UinputBridge inputBridge = this.mBridgeMap.get(token);
            if (inputBridge == null) {
                Slog.w(TAG, String.format("Input bridge not found for token: %s", token));
                return;
            }
            long idToken = Binder.clearCallingIdentity();
            inputBridge.sendKeyUp(token, keyCode);
            Binder.restoreCallingIdentity(idToken);
        }
    }

    public void sendPointerDown(IBinder token, int pointerId, int x, int y) {
        synchronized (this.mLock) {
            UinputBridge inputBridge = this.mBridgeMap.get(token);
            if (inputBridge == null) {
                Slog.w(TAG, String.format("Input bridge not found for token: %s", token));
                return;
            }
            long idToken = Binder.clearCallingIdentity();
            inputBridge.sendPointerDown(token, pointerId, x, y);
            Binder.restoreCallingIdentity(idToken);
        }
    }

    public void sendPointerUp(IBinder token, int pointerId) {
        synchronized (this.mLock) {
            UinputBridge inputBridge = this.mBridgeMap.get(token);
            if (inputBridge == null) {
                Slog.w(TAG, String.format("Input bridge not found for token: %s", token));
                return;
            }
            long idToken = Binder.clearCallingIdentity();
            inputBridge.sendPointerUp(token, pointerId);
            Binder.restoreCallingIdentity(idToken);
        }
    }

    public void sendPointerSync(IBinder token) {
        synchronized (this.mLock) {
            UinputBridge inputBridge = this.mBridgeMap.get(token);
            if (inputBridge == null) {
                Slog.w(TAG, String.format("Input bridge not found for token: %s", token));
                return;
            }
            long idToken = Binder.clearCallingIdentity();
            inputBridge.sendPointerSync(token);
            Binder.restoreCallingIdentity(idToken);
        }
    }

    public void sendGamepadKeyUp(IBinder token, int keyIndex) {
        synchronized (this.mLock) {
            UinputBridge inputBridge = this.mBridgeMap.get(token);
            if (inputBridge == null) {
                Slog.w(TAG, String.format("Input bridge not found for token: %s", token));
                return;
            }
            long idToken = Binder.clearCallingIdentity();
            inputBridge.sendGamepadKey(token, keyIndex, false);
            Binder.restoreCallingIdentity(idToken);
        }
    }

    public void sendGamepadKeyDown(IBinder token, int keyCode) {
        synchronized (this.mLock) {
            UinputBridge inputBridge = this.mBridgeMap.get(token);
            if (inputBridge == null) {
                Slog.w(TAG, String.format("Input bridge not found for token: %s", token));
                return;
            }
            long idToken = Binder.clearCallingIdentity();
            inputBridge.sendGamepadKey(token, keyCode, true);
            Binder.restoreCallingIdentity(idToken);
        }
    }

    public void sendGamepadAxisValue(IBinder token, int axis, float value) {
        synchronized (this.mLock) {
            UinputBridge inputBridge = this.mBridgeMap.get(token);
            if (inputBridge == null) {
                Slog.w(TAG, String.format("Input bridge not found for token: %s", token));
                return;
            }
            long idToken = Binder.clearCallingIdentity();
            inputBridge.sendGamepadAxisValue(token, axis, value);
            Binder.restoreCallingIdentity(idToken);
        }
    }
}