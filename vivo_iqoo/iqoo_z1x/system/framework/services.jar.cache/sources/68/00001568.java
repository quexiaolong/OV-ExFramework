package com.android.server.pm;

import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.Log;
import com.android.internal.util.FastPrintWriter;
import com.android.internal.util.IndentingPrintWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import libcore.io.IoUtils;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class CompilerStats extends AbstractStatsBase<Void> {
    private static final int COMPILER_STATS_VERSION = 1;
    private static final String COMPILER_STATS_VERSION_HEADER = "PACKAGE_MANAGER__COMPILER_STATS__";
    private final Map<String, PackageStats> packageStats;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class PackageStats {
        private final Map<String, Long> compileTimePerCodePath = new ArrayMap(2);
        private final String packageName;

        public PackageStats(String packageName) {
            this.packageName = packageName;
        }

        public String getPackageName() {
            return this.packageName;
        }

        public long getCompileTime(String codePath) {
            String storagePath = getStoredPathFromCodePath(codePath);
            synchronized (this.compileTimePerCodePath) {
                Long l = this.compileTimePerCodePath.get(storagePath);
                if (l == null) {
                    return 0L;
                }
                return l.longValue();
            }
        }

        public void setCompileTime(String codePath, long compileTimeInMs) {
            String storagePath = getStoredPathFromCodePath(codePath);
            synchronized (this.compileTimePerCodePath) {
                if (compileTimeInMs <= 0) {
                    this.compileTimePerCodePath.remove(storagePath);
                } else {
                    this.compileTimePerCodePath.put(storagePath, Long.valueOf(compileTimeInMs));
                }
            }
        }

        private static String getStoredPathFromCodePath(String codePath) {
            int lastSlash = codePath.lastIndexOf(File.separatorChar);
            return codePath.substring(lastSlash + 1);
        }

        public void dump(IndentingPrintWriter ipw) {
            synchronized (this.compileTimePerCodePath) {
                if (this.compileTimePerCodePath.size() == 0) {
                    ipw.println("(No recorded stats)");
                } else {
                    for (Map.Entry<String, Long> e : this.compileTimePerCodePath.entrySet()) {
                        ipw.println(" " + e.getKey() + " - " + e.getValue());
                    }
                }
            }
        }
    }

    public CompilerStats() {
        super("package-cstats.list", "CompilerStats_DiskWriter", false);
        this.packageStats = new HashMap();
    }

    public PackageStats getPackageStats(String packageName) {
        PackageStats packageStats;
        synchronized (this.packageStats) {
            packageStats = this.packageStats.get(packageName);
        }
        return packageStats;
    }

    public void setPackageStats(String packageName, PackageStats stats) {
        synchronized (this.packageStats) {
            this.packageStats.put(packageName, stats);
        }
    }

    public PackageStats createPackageStats(String packageName) {
        PackageStats newStats;
        synchronized (this.packageStats) {
            newStats = new PackageStats(packageName);
            this.packageStats.put(packageName, newStats);
        }
        return newStats;
    }

    public PackageStats getOrCreatePackageStats(String packageName) {
        synchronized (this.packageStats) {
            PackageStats existingStats = this.packageStats.get(packageName);
            if (existingStats != null) {
                return existingStats;
            }
            return createPackageStats(packageName);
        }
    }

    public void deletePackageStats(String packageName) {
        synchronized (this.packageStats) {
            this.packageStats.remove(packageName);
        }
    }

    public void write(Writer out) {
        FastPrintWriter fpw = new FastPrintWriter(out);
        fpw.print(COMPILER_STATS_VERSION_HEADER);
        fpw.println(1);
        synchronized (this.packageStats) {
            for (PackageStats pkg : this.packageStats.values()) {
                synchronized (pkg.compileTimePerCodePath) {
                    if (!pkg.compileTimePerCodePath.isEmpty()) {
                        fpw.println(pkg.getPackageName());
                        for (Map.Entry<String, Long> e : pkg.compileTimePerCodePath.entrySet()) {
                            fpw.println("-" + e.getKey() + ":" + e.getValue());
                        }
                    }
                }
            }
        }
        fpw.flush();
    }

    /* JADX WARN: Code restructure failed: missing block: B:21:0x0076, code lost:
        throw new java.lang.IllegalArgumentException("Could not parse data " + r6);
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public boolean read(java.io.Reader r11) {
        /*
            r10 = this;
            java.util.Map<java.lang.String, com.android.server.pm.CompilerStats$PackageStats> r0 = r10.packageStats
            monitor-enter(r0)
            java.util.Map<java.lang.String, com.android.server.pm.CompilerStats$PackageStats> r1 = r10.packageStats     // Catch: java.lang.Throwable -> Lc1
            r1.clear()     // Catch: java.lang.Throwable -> Lc1
            java.io.BufferedReader r1 = new java.io.BufferedReader     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            r1.<init>(r11)     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            java.lang.String r2 = r1.readLine()     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            if (r2 == 0) goto Lae
            java.lang.String r3 = "PACKAGE_MANAGER__COMPILER_STATS__"
            boolean r3 = r2.startsWith(r3)     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            if (r3 == 0) goto L97
            java.lang.String r3 = "PACKAGE_MANAGER__COMPILER_STATS__"
            int r3 = r3.length()     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            java.lang.String r3 = r2.substring(r3)     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            int r3 = java.lang.Integer.parseInt(r3)     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            r4 = 1
            if (r3 != r4) goto L80
            com.android.server.pm.CompilerStats$PackageStats r3 = new com.android.server.pm.CompilerStats$PackageStats     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            java.lang.String r5 = "fake package"
            r3.<init>(r5)     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            r5 = 0
        L34:
            java.lang.String r6 = r1.readLine()     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            r5 = r6
            if (r6 == 0) goto L7d
            java.lang.String r6 = "-"
            boolean r6 = r5.startsWith(r6)     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            if (r6 == 0) goto L77
            r6 = 58
            int r6 = r5.indexOf(r6)     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            r7 = -1
            if (r6 == r7) goto L60
            if (r6 == r4) goto L60
            java.lang.String r7 = r5.substring(r4, r6)     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            int r8 = r6 + 1
            java.lang.String r8 = r5.substring(r8)     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            long r8 = java.lang.Long.parseLong(r8)     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            r3.setCompileTime(r7, r8)     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            goto L34
        L60:
            java.lang.IllegalArgumentException r4 = new java.lang.IllegalArgumentException     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            java.lang.StringBuilder r7 = new java.lang.StringBuilder     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            r7.<init>()     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            java.lang.String r8 = "Could not parse data "
            r7.append(r8)     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            r7.append(r5)     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            java.lang.String r7 = r7.toString()     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            r4.<init>(r7)     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            throw r4     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
        L77:
            com.android.server.pm.CompilerStats$PackageStats r6 = r10.getOrCreatePackageStats(r5)     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            r3 = r6
            goto L34
        L7d:
            monitor-exit(r0)     // Catch: java.lang.Throwable -> Lc1
            return r4
        L80:
            java.lang.IllegalArgumentException r4 = new java.lang.IllegalArgumentException     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            java.lang.StringBuilder r5 = new java.lang.StringBuilder     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            r5.<init>()     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            java.lang.String r6 = "Unexpected version: "
            r5.append(r6)     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            r5.append(r3)     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            java.lang.String r5 = r5.toString()     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            r4.<init>(r5)     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            throw r4     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
        L97:
            java.lang.IllegalArgumentException r3 = new java.lang.IllegalArgumentException     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            r4.<init>()     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            java.lang.String r5 = "Invalid version line: "
            r4.append(r5)     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            r4.append(r2)     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            java.lang.String r4 = r4.toString()     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            r3.<init>(r4)     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            throw r3     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
        Lae:
            java.lang.IllegalArgumentException r3 = new java.lang.IllegalArgumentException     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            java.lang.String r4 = "No version line found."
            r3.<init>(r4)     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
            throw r3     // Catch: java.lang.Exception -> Lb6 java.lang.Throwable -> Lc1
        Lb6:
            r1 = move-exception
            java.lang.String r2 = "PackageManager"
            java.lang.String r3 = "Error parsing compiler stats"
            android.util.Log.e(r2, r3, r1)     // Catch: java.lang.Throwable -> Lc1
            r2 = 0
            monitor-exit(r0)     // Catch: java.lang.Throwable -> Lc1
            return r2
        Lc1:
            r1 = move-exception
            monitor-exit(r0)     // Catch: java.lang.Throwable -> Lc1
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.CompilerStats.read(java.io.Reader):boolean");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writeNow() {
        writeNow(null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean maybeWriteAsync() {
        return maybeWriteAsync(null);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.pm.AbstractStatsBase
    public void writeInternal(Void data) {
        AtomicFile file = getFile();
        FileOutputStream f = null;
        try {
            f = file.startWrite();
            OutputStreamWriter osw = new OutputStreamWriter(f);
            write(osw);
            osw.flush();
            file.finishWrite(f);
        } catch (IOException e) {
            if (f != null) {
                file.failWrite(f);
            }
            Log.e("PackageManager", "Failed to write compiler stats", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void read() {
        read((CompilerStats) null);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.pm.AbstractStatsBase
    public void readInternal(Void data) {
        AtomicFile file = getFile();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(file.openRead()));
            read((Reader) in);
        } catch (FileNotFoundException e) {
        } catch (Throwable th) {
            IoUtils.closeQuietly(in);
            throw th;
        }
        IoUtils.closeQuietly(in);
    }
}