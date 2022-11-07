package com.android.server.net;

import android.net.IpConfiguration;
import android.net.LinkAddress;
import android.net.ProxyInfo;
import android.net.StaticIpConfiguration;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;
import com.android.server.net.DelayedDiskWrite;
import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Iterator;

/* loaded from: classes.dex */
public class IpConfigStore {
    private static final boolean DBG = false;
    protected static final String DNS_KEY = "dns";
    protected static final String EOS = "eos";
    protected static final String EXCLUSION_LIST_KEY = "exclusionList";
    protected static final String GATEWAY_KEY = "gateway";
    protected static final String ID_KEY = "id";
    protected static final int IPCONFIG_FILE_VERSION = 3;
    protected static final String IP_ASSIGNMENT_KEY = "ipAssignment";
    protected static final String LINK_ADDRESS_KEY = "linkAddress";
    protected static final String PROXY_HOST_KEY = "proxyHost";
    protected static final String PROXY_PAC_FILE = "proxyPac";
    protected static final String PROXY_PORT_KEY = "proxyPort";
    protected static final String PROXY_SETTINGS_KEY = "proxySettings";
    private static final String TAG = "IpConfigStore";
    protected final DelayedDiskWrite mWriter;

    public IpConfigStore(DelayedDiskWrite writer) {
        this.mWriter = writer;
    }

    public IpConfigStore() {
        this(new DelayedDiskWrite());
    }

    private static boolean writeConfig(DataOutputStream out, String configKey, IpConfiguration config) throws IOException {
        return writeConfig(out, configKey, config, 3);
    }

    public static boolean writeConfig(DataOutputStream out, String configKey, IpConfiguration config, int version) throws IOException {
        boolean written = false;
        try {
            int i = AnonymousClass1.$SwitchMap$android$net$IpConfiguration$IpAssignment[config.ipAssignment.ordinal()];
            if (i == 1) {
                out.writeUTF(IP_ASSIGNMENT_KEY);
                out.writeUTF(config.ipAssignment.toString());
                StaticIpConfiguration staticIpConfiguration = config.staticIpConfiguration;
                if (staticIpConfiguration != null) {
                    if (staticIpConfiguration.ipAddress != null) {
                        LinkAddress ipAddress = staticIpConfiguration.ipAddress;
                        out.writeUTF(LINK_ADDRESS_KEY);
                        out.writeUTF(ipAddress.getAddress().getHostAddress());
                        out.writeInt(ipAddress.getPrefixLength());
                    }
                    if (staticIpConfiguration.gateway != null) {
                        out.writeUTF(GATEWAY_KEY);
                        out.writeInt(0);
                        out.writeInt(1);
                        out.writeUTF(staticIpConfiguration.gateway.getHostAddress());
                    }
                    Iterator it = staticIpConfiguration.dnsServers.iterator();
                    while (it.hasNext()) {
                        InetAddress inetAddr = (InetAddress) it.next();
                        out.writeUTF(DNS_KEY);
                        out.writeUTF(inetAddr.getHostAddress());
                    }
                }
                written = true;
            } else if (i == 2) {
                out.writeUTF(IP_ASSIGNMENT_KEY);
                out.writeUTF(config.ipAssignment.toString());
                written = true;
            } else if (i != 3) {
                loge("Ignore invalid ip assignment while writing");
            }
            int i2 = AnonymousClass1.$SwitchMap$android$net$IpConfiguration$ProxySettings[config.proxySettings.ordinal()];
            if (i2 == 1) {
                ProxyInfo proxyPacProperties = config.httpProxy;
                String exclusionList = proxyPacProperties.getExclusionListAsString();
                out.writeUTF(PROXY_SETTINGS_KEY);
                out.writeUTF(config.proxySettings.toString());
                out.writeUTF(PROXY_HOST_KEY);
                out.writeUTF(proxyPacProperties.getHost());
                out.writeUTF(PROXY_PORT_KEY);
                out.writeInt(proxyPacProperties.getPort());
                if (exclusionList != null) {
                    out.writeUTF(EXCLUSION_LIST_KEY);
                    out.writeUTF(exclusionList);
                }
                written = true;
            } else if (i2 == 2) {
                ProxyInfo proxyPacProperties2 = config.httpProxy;
                out.writeUTF(PROXY_SETTINGS_KEY);
                out.writeUTF(config.proxySettings.toString());
                out.writeUTF(PROXY_PAC_FILE);
                out.writeUTF(proxyPacProperties2.getPacFileUrl().toString());
                written = true;
            } else if (i2 == 3) {
                out.writeUTF(PROXY_SETTINGS_KEY);
                out.writeUTF(config.proxySettings.toString());
                written = true;
            } else if (i2 != 4) {
                loge("Ignore invalid proxy settings while writing");
            }
            if (written) {
                out.writeUTF(ID_KEY);
                if (version < 3) {
                    out.writeInt(Integer.valueOf(configKey).intValue());
                } else {
                    out.writeUTF(configKey);
                }
            }
        } catch (NullPointerException e) {
            loge("Failure in writing " + config + e);
        }
        out.writeUTF(EOS);
        return written;
    }

    /* renamed from: com.android.server.net.IpConfigStore$1 */
    /* loaded from: classes.dex */
    public static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$android$net$IpConfiguration$IpAssignment;
        static final /* synthetic */ int[] $SwitchMap$android$net$IpConfiguration$ProxySettings;

        static {
            int[] iArr = new int[IpConfiguration.ProxySettings.values().length];
            $SwitchMap$android$net$IpConfiguration$ProxySettings = iArr;
            try {
                iArr[IpConfiguration.ProxySettings.STATIC.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$net$IpConfiguration$ProxySettings[IpConfiguration.ProxySettings.PAC.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$net$IpConfiguration$ProxySettings[IpConfiguration.ProxySettings.NONE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$android$net$IpConfiguration$ProxySettings[IpConfiguration.ProxySettings.UNASSIGNED.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            int[] iArr2 = new int[IpConfiguration.IpAssignment.values().length];
            $SwitchMap$android$net$IpConfiguration$IpAssignment = iArr2;
            try {
                iArr2[IpConfiguration.IpAssignment.STATIC.ordinal()] = 1;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$android$net$IpConfiguration$IpAssignment[IpConfiguration.IpAssignment.DHCP.ordinal()] = 2;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$android$net$IpConfiguration$IpAssignment[IpConfiguration.IpAssignment.UNASSIGNED.ordinal()] = 3;
            } catch (NoSuchFieldError e7) {
            }
        }
    }

    @Deprecated
    public void writeIpAndProxyConfigurationsToFile(String filePath, final SparseArray<IpConfiguration> networks) {
        this.mWriter.write(filePath, new DelayedDiskWrite.Writer() { // from class: com.android.server.net.-$$Lambda$IpConfigStore$O2tmBZ0pfEt3xGZYo5ZrQq4edzM
            @Override // com.android.server.net.DelayedDiskWrite.Writer
            public final void onWriteCalled(DataOutputStream dataOutputStream) {
                IpConfigStore.lambda$writeIpAndProxyConfigurationsToFile$0(networks, dataOutputStream);
            }
        });
    }

    public static /* synthetic */ void lambda$writeIpAndProxyConfigurationsToFile$0(SparseArray networks, DataOutputStream out) throws IOException {
        out.writeInt(3);
        for (int i = 0; i < networks.size(); i++) {
            writeConfig(out, String.valueOf(networks.keyAt(i)), (IpConfiguration) networks.valueAt(i));
        }
    }

    public void writeIpConfigurations(String filePath, final ArrayMap<String, IpConfiguration> networks) {
        this.mWriter.write(filePath, new DelayedDiskWrite.Writer() { // from class: com.android.server.net.-$$Lambda$IpConfigStore$rFY3yG3j6RGRgrQey7yYfi0Yze0
            @Override // com.android.server.net.DelayedDiskWrite.Writer
            public final void onWriteCalled(DataOutputStream dataOutputStream) {
                IpConfigStore.lambda$writeIpConfigurations$1(networks, dataOutputStream);
            }
        });
    }

    public static /* synthetic */ void lambda$writeIpConfigurations$1(ArrayMap networks, DataOutputStream out) throws IOException {
        out.writeInt(3);
        for (int i = 0; i < networks.size(); i++) {
            writeConfig(out, (String) networks.keyAt(i), (IpConfiguration) networks.valueAt(i));
        }
    }

    public static ArrayMap<String, IpConfiguration> readIpConfigurations(String filePath) {
        try {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(filePath));
            return readIpConfigurations(bufferedInputStream);
        } catch (FileNotFoundException e) {
            loge("Error opening configuration file: " + e);
            return new ArrayMap<>(0);
        }
    }

    @Deprecated
    public static SparseArray<IpConfiguration> readIpAndProxyConfigurations(String filePath) {
        try {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(filePath));
            return readIpAndProxyConfigurations(bufferedInputStream);
        } catch (FileNotFoundException e) {
            loge("Error opening configuration file: " + e);
            return new SparseArray<>();
        }
    }

    @Deprecated
    public static SparseArray<IpConfiguration> readIpAndProxyConfigurations(InputStream inputStream) {
        ArrayMap<String, IpConfiguration> networks = readIpConfigurations(inputStream);
        if (networks == null) {
            return null;
        }
        SparseArray<IpConfiguration> networksById = new SparseArray<>();
        for (int i = 0; i < networks.size(); i++) {
            int id = Integer.valueOf(networks.keyAt(i)).intValue();
            networksById.put(id, networks.valueAt(i));
        }
        return networksById;
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Unreachable block: B:305:0x0285
        	at jadx.core.dex.visitors.blocks.BlockProcessor.checkForUnreachableBlocks(BlockProcessor.java:81)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.processBlocksTree(BlockProcessor.java:47)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.visit(BlockProcessor.java:39)
        */
    public static android.util.ArrayMap<java.lang.String, android.net.IpConfiguration> readIpConfigurations(java.io.InputStream r19) {
        /*
            Method dump skipped, instructions count: 731
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.net.IpConfigStore.readIpConfigurations(java.io.InputStream):android.util.ArrayMap");
    }

    protected static void loge(String s) {
        Log.e(TAG, s);
    }

    protected static void log(String s) {
        Log.d(TAG, s);
    }
}