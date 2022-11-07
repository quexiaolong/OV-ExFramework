package com.vivo.services.sensorhub;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.SystemProperties;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import com.vivo.services.rms.sdk.Consts;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/* loaded from: classes.dex */
public class NVItemSocketClient {
    private static final String SERVER_NAME = "vivoEmSvr-service";
    private static final String TAG = "NVItemSocketClient";
    private LocalSocket client;
    private LocalSocketAddress localAddr;

    public NVItemSocketClient() {
        int retry = 3;
        while (true) {
            int retry2 = retry - 1;
            if (retry > 0) {
                String emsvr = SystemProperties.get("sys.emsvr.opt", "0");
                String emsvrBak = SystemProperties.get("sys.emsvr.opt.bak", "0");
                if (!emsvr.equals("1") || !emsvrBak.equals("1")) {
                    SystemProperties.set("sys.emsvr.opt", "1");
                    for (int i = 0; i < 1000; i++) {
                        String emsvrBak2 = SystemProperties.get("sys.emsvr.opt.bak", "0");
                        if (!emsvrBak2.equals("1")) {
                            try {
                                Thread.sleep(100L);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                try {
                    this.client = new LocalSocket();
                    LocalSocketAddress localSocketAddress = new LocalSocketAddress(SERVER_NAME, LocalSocketAddress.Namespace.RESERVED);
                    this.localAddr = localSocketAddress;
                    this.client.connect(localSocketAddress);
                } catch (Exception e2) {
                    try {
                        VLog.e(TAG, "create socket Exception, retry=" + retry2);
                        if (this.client != null) {
                            this.client.close();
                            this.client = null;
                        }
                        VLog.e(TAG, "stop _em_svr");
                        SystemProperties.set("sys.emsvr.opt", "0");
                    } catch (Exception e3) {
                        VLog.e(TAG, "NullPointerException ex2");
                    }
                }
                if (this.client == null) {
                    retry = retry2;
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    public synchronized String sendMessage(String message) throws IOException {
        OutputStream netOut = null;
        BufferedReader in = null;
        String response = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        if (this.client == null) {
            VLog.d(TAG, "the client is null");
            return "error";
        }
        try {
            netOut = this.client.getOutputStream();
            netOut.write(message.getBytes());
            in = new BufferedReader(new InputStreamReader(this.client.getInputStream()));
            while (true) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }
                response = response + line + "\n";
            }
            if (response.endsWith("\n")) {
                response = response.substring(0, response.length() - 1);
            }
            if (netOut != null) {
                netOut.close();
            }
            in.close();
            if (this.client != null) {
                this.client.close();
                this.client = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            response = "error";
            SystemProperties.set("sys.emsvr.opt", "1");
            if (netOut != null) {
                netOut.close();
            }
            if (in != null) {
                in.close();
            }
            if (this.client != null) {
                this.client.close();
                this.client = null;
            }
        } catch (Exception e2) {
            e2.printStackTrace();
            response = "error";
            if (netOut != null) {
                netOut.close();
            }
            if (in != null) {
                in.close();
            }
            if (this.client != null) {
                this.client.close();
                this.client = null;
            }
        }
        return response;
    }

    /* JADX WARN: Code restructure failed: missing block: B:32:0x009d, code lost:
        if (r3 == null) goto L24;
     */
    /* JADX WARN: Code restructure failed: missing block: B:33:0x009f, code lost:
        r3.close();
        r8.client = null;
     */
    /* JADX WARN: Code restructure failed: missing block: B:41:0x00b5, code lost:
        if (r3 == null) goto L24;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public java.lang.String sendMessage(java.lang.String r9, boolean r10) throws java.io.IOException {
        /*
            r8 = this;
            r0 = 0
            r1 = 0
            java.lang.String r2 = ""
            android.net.LocalSocket r3 = r8.client
            java.lang.String r4 = "NVItemSocketClient"
            if (r3 != 0) goto L10
            java.lang.String r3 = "the client is null"
            com.vivo.common.utils.VLog.d(r4, r3)
            return r2
        L10:
            r5 = 0
            java.io.OutputStream r3 = r3.getOutputStream()     // Catch: java.lang.Throwable -> L88 java.lang.Exception -> L8a java.io.IOException -> La5
            r0 = r3
            byte[] r3 = r9.getBytes()     // Catch: java.lang.Throwable -> L88 java.lang.Exception -> L8a java.io.IOException -> La5
            r0.write(r3)     // Catch: java.lang.Throwable -> L88 java.lang.Exception -> L8a java.io.IOException -> La5
            java.io.BufferedReader r3 = new java.io.BufferedReader     // Catch: java.lang.Throwable -> L88 java.lang.Exception -> L8a java.io.IOException -> La5
            java.io.InputStreamReader r6 = new java.io.InputStreamReader     // Catch: java.lang.Throwable -> L88 java.lang.Exception -> L8a java.io.IOException -> La5
            android.net.LocalSocket r7 = r8.client     // Catch: java.lang.Throwable -> L88 java.lang.Exception -> L8a java.io.IOException -> La5
            java.io.InputStream r7 = r7.getInputStream()     // Catch: java.lang.Throwable -> L88 java.lang.Exception -> L8a java.io.IOException -> La5
            r6.<init>(r7)     // Catch: java.lang.Throwable -> L88 java.lang.Exception -> L8a java.io.IOException -> La5
            r3.<init>(r6)     // Catch: java.lang.Throwable -> L88 java.lang.Exception -> L8a java.io.IOException -> La5
            r1 = r3
        L2e:
            java.lang.String r3 = r1.readLine()     // Catch: java.lang.Throwable -> L88 java.lang.Exception -> L8a java.io.IOException -> La5
            if (r3 != 0) goto L72
        L35:
            if (r10 != 0) goto L4b
            java.lang.String r6 = "ok\n"
            boolean r6 = r2.endsWith(r6)     // Catch: java.lang.Throwable -> L88 java.lang.Exception -> L8a java.io.IOException -> La5
            if (r6 == 0) goto L4b
            r6 = 0
            int r7 = r2.length()     // Catch: java.lang.Throwable -> L88 java.lang.Exception -> L8a java.io.IOException -> La5
            int r7 = r7 + (-3)
            java.lang.String r6 = r2.substring(r6, r7)     // Catch: java.lang.Throwable -> L88 java.lang.Exception -> L8a java.io.IOException -> La5
            r2 = r6
        L4b:
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L88 java.lang.Exception -> L8a java.io.IOException -> La5
            r6.<init>()     // Catch: java.lang.Throwable -> L88 java.lang.Exception -> L8a java.io.IOException -> La5
            java.lang.String r7 = "response:"
            r6.append(r7)     // Catch: java.lang.Throwable -> L88 java.lang.Exception -> L8a java.io.IOException -> La5
            r6.append(r2)     // Catch: java.lang.Throwable -> L88 java.lang.Exception -> L8a java.io.IOException -> La5
            java.lang.String r6 = r6.toString()     // Catch: java.lang.Throwable -> L88 java.lang.Exception -> L8a java.io.IOException -> La5
            com.vivo.common.utils.VLog.d(r4, r6)     // Catch: java.lang.Throwable -> L88 java.lang.Exception -> L8a java.io.IOException -> La5
            if (r0 == 0) goto L64
            r0.close()
        L64:
            r1.close()
            android.net.LocalSocket r4 = r8.client
            if (r4 == 0) goto Lb8
            r4.close()
            r8.client = r5
            goto Lb8
        L72:
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L88 java.lang.Exception -> L8a java.io.IOException -> La5
            r6.<init>()     // Catch: java.lang.Throwable -> L88 java.lang.Exception -> L8a java.io.IOException -> La5
            r6.append(r2)     // Catch: java.lang.Throwable -> L88 java.lang.Exception -> L8a java.io.IOException -> La5
            r6.append(r3)     // Catch: java.lang.Throwable -> L88 java.lang.Exception -> L8a java.io.IOException -> La5
            java.lang.String r7 = "\n"
            r6.append(r7)     // Catch: java.lang.Throwable -> L88 java.lang.Exception -> L8a java.io.IOException -> La5
            java.lang.String r6 = r6.toString()     // Catch: java.lang.Throwable -> L88 java.lang.Exception -> L8a java.io.IOException -> La5
            r2 = r6
            goto L2e
        L88:
            r3 = move-exception
            goto Lb9
        L8a:
            r3 = move-exception
            r3.printStackTrace()     // Catch: java.lang.Throwable -> L88
            java.lang.String r4 = "error"
            r2 = r4
            if (r0 == 0) goto L96
            r0.close()
        L96:
            if (r1 == 0) goto L9b
            r1.close()
        L9b:
            android.net.LocalSocket r3 = r8.client
            if (r3 == 0) goto Lb8
        L9f:
            r3.close()
            r8.client = r5
            goto Lb8
        La5:
            r3 = move-exception
            r3.printStackTrace()     // Catch: java.lang.Throwable -> L88
            if (r0 == 0) goto Lae
            r0.close()
        Lae:
            if (r1 == 0) goto Lb3
            r1.close()
        Lb3:
            android.net.LocalSocket r3 = r8.client
            if (r3 == 0) goto Lb8
            goto L9f
        Lb8:
            return r2
        Lb9:
            if (r0 == 0) goto Lbe
            r0.close()
        Lbe:
            if (r1 == 0) goto Lc3
            r1.close()
        Lc3:
            android.net.LocalSocket r4 = r8.client
            if (r4 == 0) goto Lcc
            r4.close()
            r8.client = r5
        Lcc:
            throw r3
        */
        throw new UnsupportedOperationException("Method not decompiled: com.vivo.services.sensorhub.NVItemSocketClient.sendMessage(java.lang.String, boolean):java.lang.String");
    }

    public synchronized String sendMessageReadByte(String message) throws IOException {
        String response;
        byte[] buffer;
        int length;
        OutputStream netOut = null;
        InputStream in = null;
        if (this.client == null) {
            VLog.d(TAG, "the client is null");
            return "error";
        }
        try {
            try {
                netOut = this.client.getOutputStream();
                netOut.write(message.getBytes());
                VLog.d(TAG, "read...");
                in = this.client.getInputStream();
                buffer = new byte[Consts.ProcessStates.FOCUS];
                length = in.read(buffer, 0, Consts.ProcessStates.FOCUS);
                VLog.d(TAG, "length:" + length);
            } catch (Exception e) {
                e.printStackTrace();
                response = "error";
                if (netOut != null) {
                    netOut.close();
                }
                if (in != null) {
                    in.close();
                }
                if (this.client != null) {
                    this.client.close();
                }
            }
        } catch (IOException e2) {
            e2.printStackTrace();
            response = "error";
            SystemProperties.set("sys.emsvr.opt", "1");
            if (netOut != null) {
                netOut.close();
            }
            if (in != null) {
                in.close();
            }
            if (this.client != null) {
                this.client.close();
            }
        }
        if (length < 0) {
            if (netOut != null) {
                netOut.close();
            }
            if (in != null) {
                in.close();
            }
            if (this.client != null) {
                this.client.close();
                this.client = null;
            }
            return null;
        }
        byte[] result = new byte[length];
        System.arraycopy(buffer, 0, result, 0, length);
        response = new String(result);
        VLog.d(TAG, "receive:" + response);
        if (response.endsWith("\n")) {
            response = response.substring(0, response.length() - 1);
        }
        if (netOut != null) {
            netOut.close();
        }
        if (in != null) {
            in.close();
        }
        if (this.client != null) {
            this.client.close();
            this.client = null;
        }
        return response;
    }
}