package android.net;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

/* loaded from: classes.dex */
public class NetworkFactory extends Handler {
    public static final int CMD_CANCEL_REQUEST = 2;
    public static final int CMD_REQUEST_NETWORK = 1;
    private static final int CMD_SET_FILTER = 4;
    private static final int CMD_SET_SCORE = 3;
    private static final boolean DBG = true;
    private static final boolean VDBG = false;
    private final String LOG_TAG;
    private NetworkCapabilities mCapabilityFilter;
    private final Context mContext;
    private final Map<NetworkRequest, NetworkRequestInfo> mNetworkRequests;
    private NetworkProvider mProvider;
    private int mRefCount;
    private int mScore;

    /* renamed from: lambda$BOTGlmxddm-dxfTs0rdTPrhrIk4 */
    public static /* synthetic */ void m0lambda$BOTGlmxddmdxfTs0rdTPrhrIk4(NetworkFactory networkFactory) {
        networkFactory.evalRequests();
    }

    public NetworkFactory(Looper looper, Context context, String logTag, NetworkCapabilities filter) {
        super(looper);
        this.mNetworkRequests = new LinkedHashMap();
        this.mRefCount = 0;
        this.mProvider = null;
        this.LOG_TAG = logTag;
        this.mContext = context;
        this.mCapabilityFilter = filter;
    }

    public void register() {
        if (this.mProvider != null) {
            throw new IllegalStateException("A NetworkFactory must only be registered once");
        }
        log("Registering NetworkFactory");
        this.mProvider = new NetworkProvider(this.mContext, getLooper(), this.LOG_TAG) { // from class: android.net.NetworkFactory.1
            {
                NetworkFactory.this = this;
            }

            public void onNetworkRequested(NetworkRequest request, int score, int servingProviderId) {
                NetworkFactory.this.handleAddRequest(request, score, servingProviderId);
            }

            public void onNetworkRequestWithdrawn(NetworkRequest request) {
                NetworkFactory.this.handleRemoveRequest(request);
            }
        };
        ((ConnectivityManager) this.mContext.getSystemService("connectivity")).registerNetworkProvider(this.mProvider);
    }

    public void terminate() {
        if (this.mProvider == null) {
            throw new IllegalStateException("This NetworkFactory was never registered");
        }
        log("Unregistering NetworkFactory");
        ((ConnectivityManager) this.mContext.getSystemService("connectivity")).unregisterNetworkProvider(this.mProvider);
        removeCallbacksAndMessages(null);
    }

    @Override // android.os.Handler
    public void handleMessage(Message msg) {
        int i = msg.what;
        if (i == 1) {
            handleAddRequest((NetworkRequest) msg.obj, msg.arg1, msg.arg2);
        } else if (i == 2) {
            handleRemoveRequest((NetworkRequest) msg.obj);
        } else if (i == 3) {
            handleSetScore(msg.arg1);
        } else if (i == 4) {
            handleSetFilter((NetworkCapabilities) msg.obj);
        }
    }

    /* loaded from: classes.dex */
    public static class NetworkRequestInfo {
        public int providerId;
        public final NetworkRequest request;
        public boolean requested = false;
        public int score;

        NetworkRequestInfo(NetworkRequest request, int score, int providerId) {
            this.request = request;
            this.score = score;
            this.providerId = providerId;
        }

        public String toString() {
            return "{" + this.request + ", score=" + this.score + ", requested=" + this.requested + "}";
        }
    }

    protected void handleAddRequest(NetworkRequest request, int score, int servingProviderId) {
        synchronized (this.mNetworkRequests) {
            NetworkRequestInfo n = this.mNetworkRequests.get(request);
            if (n == null) {
                log("got request " + request + " with score " + score + " and providerId " + servingProviderId);
                n = new NetworkRequestInfo(request, score, servingProviderId);
                this.mNetworkRequests.put(n.request, n);
            } else {
                n.score = score;
                n.providerId = servingProviderId;
            }
            evalRequest(n);
        }
    }

    protected void handleRemoveRequest(NetworkRequest request) {
        synchronized (this.mNetworkRequests) {
            NetworkRequestInfo n = this.mNetworkRequests.get(request);
            if (n != null) {
                this.mNetworkRequests.remove(request);
                if (n.requested) {
                    releaseNetworkFor(n.request);
                }
            }
        }
    }

    private void handleSetScore(int score) {
        this.mScore = score;
        evalRequests();
    }

    private void handleSetFilter(NetworkCapabilities netCap) {
        this.mCapabilityFilter = netCap;
        evalRequests();
    }

    public boolean acceptRequest(NetworkRequest request, int score) {
        return true;
    }

    private void evalRequest(NetworkRequestInfo n) {
        if (shouldNeedNetworkFor(n)) {
            needNetworkFor(n.request, n.score);
            n.requested = true;
        } else if (shouldReleaseNetworkFor(n)) {
            releaseNetworkFor(n.request);
            n.requested = false;
        } else if (!n.requested) {
            if ((n.score < this.mScore || n.providerId == getProviderId()) && n.request.canBeSatisfiedBy(this.mCapabilityFilter) && acceptRequest(n.request, n.score)) {
                n.requested = vivoEvalRequest(n.request, n.score);
            }
        }
    }

    private boolean shouldNeedNetworkFor(NetworkRequestInfo n) {
        return !n.requested && (n.score < this.mScore || n.providerId == getProviderId()) && n.request.canBeSatisfiedBy(this.mCapabilityFilter) && acceptRequest(n.request, n.score);
    }

    private boolean shouldReleaseNetworkFor(NetworkRequestInfo n) {
        return n.requested && !((n.score <= this.mScore || n.providerId == getProviderId()) && n.request.canBeSatisfiedBy(this.mCapabilityFilter) && acceptRequest(n.request, n.score));
    }

    public void evalRequests() {
        synchronized (this.mNetworkRequests) {
            for (NetworkRequestInfo n : this.mNetworkRequests.values()) {
                if (n != null) {
                    evalRequest(n);
                }
            }
        }
    }

    protected void reevaluateAllRequests() {
        post(new Runnable() { // from class: android.net.-$$Lambda$NetworkFactory$BOTGlmxddm-dxfTs0rdTPrhrIk4
            @Override // java.lang.Runnable
            public final void run() {
                NetworkFactory.m0lambda$BOTGlmxddmdxfTs0rdTPrhrIk4(NetworkFactory.this);
            }
        });
    }

    protected void releaseRequestAsUnfulfillableByAnyFactory(final NetworkRequest r) {
        post(new Runnable() { // from class: android.net.-$$Lambda$NetworkFactory$6X9egP3VUFw6n6aZoN0JJN7Lxgc
            @Override // java.lang.Runnable
            public final void run() {
                NetworkFactory.this.lambda$releaseRequestAsUnfulfillableByAnyFactory$0$NetworkFactory(r);
            }
        });
    }

    public /* synthetic */ void lambda$releaseRequestAsUnfulfillableByAnyFactory$0$NetworkFactory(NetworkRequest r) {
        log("releaseRequestAsUnfulfillableByAnyFactory: " + r);
        NetworkProvider provider = this.mProvider;
        if (provider == null) {
            Log.e(this.LOG_TAG, "Ignoring attempt to release unregistered request as unfulfillable");
        } else {
            provider.declareNetworkRequestUnfulfillable(r);
        }
    }

    protected void startNetwork() {
    }

    protected void stopNetwork() {
    }

    protected void needNetworkFor(NetworkRequest networkRequest, int score) {
        int i = this.mRefCount + 1;
        this.mRefCount = i;
        if (i == 1) {
            startNetwork();
        }
    }

    protected void releaseNetworkFor(NetworkRequest networkRequest) {
        int i = this.mRefCount - 1;
        this.mRefCount = i;
        if (i == 0) {
            stopNetwork();
        }
    }

    protected boolean vivoEvalRequest(NetworkRequest networkRequest, int score) {
        return false;
    }

    public void setScoreFilter(int score) {
        sendMessage(obtainMessage(3, score, 0));
    }

    public void setCapabilityFilter(NetworkCapabilities netCap) {
        sendMessage(obtainMessage(4, new NetworkCapabilities(netCap)));
    }

    protected int getRequestCount() {
        int size;
        synchronized (this.mNetworkRequests) {
            size = this.mNetworkRequests.size();
        }
        return size;
    }

    public int getSerialNumber() {
        return getProviderId();
    }

    public NetworkProvider getProvider() {
        return this.mProvider;
    }

    private int getProviderId() {
        NetworkProvider networkProvider = this.mProvider;
        if (networkProvider == null) {
            return -1;
        }
        return networkProvider.getProviderId();
    }

    protected void log(String s) {
        Log.d(this.LOG_TAG, s);
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        writer.println(toString());
        synchronized (this.mNetworkRequests) {
            for (NetworkRequestInfo n : this.mNetworkRequests.values()) {
                writer.println("  " + n);
            }
        }
    }

    @Override // android.os.Handler
    public String toString() {
        String str;
        synchronized (this.mNetworkRequests) {
            str = "{" + this.LOG_TAG + " - providerId=" + getProviderId() + ", ScoreFilter=" + this.mScore + ", Filter=" + this.mCapabilityFilter + ", requests=" + this.mNetworkRequests.size() + ", refCount=" + this.mRefCount + "}";
        }
        return str;
    }
}