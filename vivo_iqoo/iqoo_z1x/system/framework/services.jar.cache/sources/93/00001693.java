package com.android.server.pm.dex;

import android.util.AtomicFile;
import android.util.Slog;
import com.android.internal.util.FastPrintWriter;
import com.android.server.pm.AbstractStatsBase;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import libcore.io.IoUtils;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class PackageDynamicCodeLoading extends AbstractStatsBase<Void> {
    private static final char FIELD_SEPARATOR = ':';
    static final int FILE_TYPE_DEX = 68;
    static final int FILE_TYPE_NATIVE = 78;
    private static final String FILE_VERSION_HEADER = "DCL1";
    static final int MAX_FILES_PER_OWNER = 100;
    private static final Pattern PACKAGE_LINE_PATTERN = Pattern.compile("([A-Z]):([0-9]+):([^:]*):(.*)");
    private static final String PACKAGE_PREFIX = "P:";
    private static final String PACKAGE_SEPARATOR = ",";
    private static final String TAG = "PackageDynamicCodeLoading";
    private final Object mLock;
    private Map<String, PackageDynamicCode> mPackageMap;

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageDynamicCodeLoading() {
        super("package-dcl.list", "PackageDynamicCodeLoading_DiskWriter", false);
        this.mLock = new Object();
        this.mPackageMap = new HashMap();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean record(String owningPackageName, String filePath, int fileType, int ownerUserId, String loadingPackageName) {
        boolean add;
        if (!isValidFileType(fileType)) {
            throw new IllegalArgumentException("Bad file type: " + fileType);
        }
        synchronized (this.mLock) {
            PackageDynamicCode packageInfo = this.mPackageMap.get(owningPackageName);
            if (packageInfo == null) {
                packageInfo = new PackageDynamicCode();
                this.mPackageMap.put(owningPackageName, packageInfo);
            }
            add = packageInfo.add(filePath, (char) fileType, ownerUserId, loadingPackageName);
        }
        return add;
    }

    private static boolean isValidFileType(int fileType) {
        return fileType == 68 || fileType == 78;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Set<String> getAllPackagesWithDynamicCodeLoading() {
        HashSet hashSet;
        synchronized (this.mLock) {
            hashSet = new HashSet(this.mPackageMap.keySet());
        }
        return hashSet;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public PackageDynamicCode getPackageDynamicCodeInfo(String packageName) {
        PackageDynamicCode packageDynamicCode;
        synchronized (this.mLock) {
            PackageDynamicCode info = this.mPackageMap.get(packageName);
            packageDynamicCode = null;
            if (info != null) {
                packageDynamicCode = new PackageDynamicCode(info);
            }
        }
        return packageDynamicCode;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clear() {
        synchronized (this.mLock) {
            this.mPackageMap.clear();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean removePackage(String packageName) {
        boolean z;
        synchronized (this.mLock) {
            z = this.mPackageMap.remove(packageName) != null;
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean removeUserPackage(String packageName, int userId) {
        synchronized (this.mLock) {
            PackageDynamicCode packageDynamicCode = this.mPackageMap.get(packageName);
            if (packageDynamicCode == null) {
                return false;
            }
            if (!packageDynamicCode.removeUser(userId)) {
                return false;
            }
            if (packageDynamicCode.mFileUsageMap.isEmpty()) {
                this.mPackageMap.remove(packageName);
            }
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean removeFile(String packageName, String filePath, int userId) {
        synchronized (this.mLock) {
            PackageDynamicCode packageDynamicCode = this.mPackageMap.get(packageName);
            if (packageDynamicCode == null) {
                return false;
            }
            if (!packageDynamicCode.removeFile(filePath, userId)) {
                return false;
            }
            if (packageDynamicCode.mFileUsageMap.isEmpty()) {
                this.mPackageMap.remove(packageName);
            }
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void syncData(Map<String, Set<Integer>> packageToUsersMap) {
        synchronized (this.mLock) {
            Iterator<Map.Entry<String, PackageDynamicCode>> it = this.mPackageMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, PackageDynamicCode> entry = it.next();
                Set<Integer> packageUsers = packageToUsersMap.get(entry.getKey());
                if (packageUsers == null) {
                    it.remove();
                } else {
                    PackageDynamicCode packageDynamicCode = entry.getValue();
                    packageDynamicCode.syncData(packageToUsersMap, packageUsers);
                    if (packageDynamicCode.mFileUsageMap.isEmpty()) {
                        it.remove();
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void maybeWriteAsync() {
        super.maybeWriteAsync(null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void writeNow() {
        super.writeNow(null);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.pm.AbstractStatsBase
    public final void writeInternal(Void data) {
        AtomicFile file = getFile();
        FileOutputStream output = null;
        try {
            output = file.startWrite();
            write(output);
            file.finishWrite(output);
        } catch (IOException e) {
            file.failWrite(output);
            Slog.e(TAG, "Failed to write dynamic usage for secondary code files.", e);
        }
    }

    void write(OutputStream output) throws IOException {
        Map<String, PackageDynamicCode> copiedMap;
        synchronized (this.mLock) {
            copiedMap = new HashMap<>(this.mPackageMap.size());
            for (Map.Entry<String, PackageDynamicCode> entry : this.mPackageMap.entrySet()) {
                PackageDynamicCode copiedValue = new PackageDynamicCode(entry.getValue());
                copiedMap.put(entry.getKey(), copiedValue);
            }
        }
        write(output, copiedMap);
    }

    private static void write(OutputStream output, Map<String, PackageDynamicCode> packageMap) throws IOException {
        FastPrintWriter fastPrintWriter = new FastPrintWriter(output);
        fastPrintWriter.println(FILE_VERSION_HEADER);
        for (Map.Entry<String, PackageDynamicCode> packageEntry : packageMap.entrySet()) {
            fastPrintWriter.print(PACKAGE_PREFIX);
            fastPrintWriter.println(packageEntry.getKey());
            Map<String, DynamicCodeFile> mFileUsageMap = packageEntry.getValue().mFileUsageMap;
            for (Map.Entry<String, DynamicCodeFile> fileEntry : mFileUsageMap.entrySet()) {
                String path = fileEntry.getKey();
                DynamicCodeFile dynamicCodeFile = fileEntry.getValue();
                fastPrintWriter.print(dynamicCodeFile.mFileType);
                fastPrintWriter.print(FIELD_SEPARATOR);
                fastPrintWriter.print(dynamicCodeFile.mUserId);
                fastPrintWriter.print(FIELD_SEPARATOR);
                String prefix = "";
                for (String packageName : dynamicCodeFile.mLoadingPackages) {
                    fastPrintWriter.print(prefix);
                    fastPrintWriter.print(packageName);
                    prefix = PACKAGE_SEPARATOR;
                }
                fastPrintWriter.print(FIELD_SEPARATOR);
                fastPrintWriter.println(escape(path));
            }
        }
        fastPrintWriter.flush();
        if (fastPrintWriter.checkError()) {
            throw new IOException("Writer failed");
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void read() {
        super.read((PackageDynamicCodeLoading) null);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.pm.AbstractStatsBase
    public final void readInternal(Void data) {
        AtomicFile file = getFile();
        FileInputStream stream = null;
        try {
            try {
                stream = file.openRead();
                read((InputStream) stream);
            } catch (FileNotFoundException e) {
            } catch (IOException e2) {
                Slog.w(TAG, "Failed to parse dynamic usage for secondary code files.", e2);
            }
        } finally {
            IoUtils.closeQuietly(stream);
        }
    }

    void read(InputStream stream) throws IOException {
        Map<String, PackageDynamicCode> newPackageMap = new HashMap<>();
        read(stream, newPackageMap);
        synchronized (this.mLock) {
            this.mPackageMap = newPackageMap;
        }
    }

    private static void read(InputStream stream, Map<String, PackageDynamicCode> packageMap) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String versionLine = reader.readLine();
        if (!FILE_VERSION_HEADER.equals(versionLine)) {
            throw new IOException("Incorrect version line: " + versionLine);
        }
        String line = reader.readLine();
        if (line != null && !line.startsWith(PACKAGE_PREFIX)) {
            throw new IOException("Malformed line: " + line);
        }
        while (line != null) {
            String packageName = line.substring(PACKAGE_PREFIX.length());
            PackageDynamicCode packageInfo = new PackageDynamicCode();
            while (true) {
                line = reader.readLine();
                if (line == null || line.startsWith(PACKAGE_PREFIX)) {
                    break;
                }
                readFileInfo(line, packageInfo);
            }
            if (!packageInfo.mFileUsageMap.isEmpty()) {
                packageMap.put(packageName, packageInfo);
            }
        }
    }

    private static void readFileInfo(String line, PackageDynamicCode output) throws IOException {
        try {
            Matcher matcher = PACKAGE_LINE_PATTERN.matcher(line);
            if (!matcher.matches()) {
                throw new IOException("Malformed line: " + line);
            }
            char type = matcher.group(1).charAt(0);
            int user = Integer.parseInt(matcher.group(2));
            String[] packages = matcher.group(3).split(PACKAGE_SEPARATOR);
            String path = unescape(matcher.group(4));
            if (packages.length == 0) {
                throw new IOException("Malformed line: " + line);
            } else if (!isValidFileType(type)) {
                throw new IOException("Unknown file type: " + line);
            } else {
                output.mFileUsageMap.put(path, new DynamicCodeFile(type, user, packages));
            }
        } catch (RuntimeException e) {
            throw new IOException("Unable to parse line: " + line, e);
        }
    }

    static String escape(String path) {
        if (path.indexOf(92) == -1 && path.indexOf(10) == -1 && path.indexOf(13) == -1) {
            return path;
        }
        StringBuilder result = new StringBuilder(path.length() + 10);
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '\n') {
                result.append("\\n");
            } else if (c != '\r') {
                if (c == '\\') {
                    result.append("\\\\");
                } else {
                    result.append(c);
                }
            } else {
                result.append("\\r");
            }
        }
        return result.toString();
    }

    static String unescape(String escaped) throws IOException {
        int start = 0;
        int finish = escaped.indexOf(92);
        if (finish == -1) {
            return escaped;
        }
        StringBuilder result = new StringBuilder(escaped.length());
        while (finish < escaped.length() - 1) {
            result.append((CharSequence) escaped, start, finish);
            char charAt = escaped.charAt(finish + 1);
            if (charAt == '\\') {
                result.append('\\');
            } else if (charAt == 'n') {
                result.append('\n');
            } else if (charAt == 'r') {
                result.append('\r');
            } else {
                throw new IOException("Bad escape in: " + escaped);
            }
            start = finish + 2;
            finish = escaped.indexOf(92, start);
            if (finish == -1) {
                result.append((CharSequence) escaped, start, escaped.length());
                return result.toString();
            }
        }
        throw new IOException("Unexpected \\ in: " + escaped);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class PackageDynamicCode {
        final Map<String, DynamicCodeFile> mFileUsageMap;

        private PackageDynamicCode() {
            this.mFileUsageMap = new HashMap();
        }

        private PackageDynamicCode(PackageDynamicCode original) {
            this.mFileUsageMap = new HashMap(original.mFileUsageMap.size());
            for (Map.Entry<String, DynamicCodeFile> entry : original.mFileUsageMap.entrySet()) {
                DynamicCodeFile newValue = new DynamicCodeFile(entry.getValue());
                this.mFileUsageMap.put(entry.getKey(), newValue);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean add(String path, char fileType, int userId, String loadingPackage) {
            DynamicCodeFile fileInfo = this.mFileUsageMap.get(path);
            if (fileInfo == null) {
                if (this.mFileUsageMap.size() >= 100) {
                    return false;
                }
                this.mFileUsageMap.put(path, new DynamicCodeFile(fileType, userId, new String[]{loadingPackage}));
                return true;
            } else if (fileInfo.mUserId != userId) {
                throw new IllegalArgumentException("Cannot change userId for '" + path + "' from " + fileInfo.mUserId + " to " + userId);
            } else {
                return fileInfo.mLoadingPackages.add(loadingPackage);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean removeUser(int userId) {
            boolean updated = false;
            Iterator<DynamicCodeFile> it = this.mFileUsageMap.values().iterator();
            while (it.hasNext()) {
                DynamicCodeFile fileInfo = it.next();
                if (fileInfo.mUserId == userId) {
                    it.remove();
                    updated = true;
                }
            }
            return updated;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean removeFile(String filePath, int userId) {
            DynamicCodeFile fileInfo = this.mFileUsageMap.get(filePath);
            if (fileInfo == null || fileInfo.mUserId != userId) {
                return false;
            }
            this.mFileUsageMap.remove(filePath);
            return true;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void syncData(Map<String, Set<Integer>> packageToUsersMap, Set<Integer> owningPackageUsers) {
            Iterator<DynamicCodeFile> fileIt = this.mFileUsageMap.values().iterator();
            while (fileIt.hasNext()) {
                DynamicCodeFile fileInfo = fileIt.next();
                int fileUserId = fileInfo.mUserId;
                if (!owningPackageUsers.contains(Integer.valueOf(fileUserId))) {
                    fileIt.remove();
                } else {
                    Iterator<String> loaderIt = fileInfo.mLoadingPackages.iterator();
                    while (loaderIt.hasNext()) {
                        String loader = loaderIt.next();
                        Set<Integer> loadingPackageUsers = packageToUsersMap.get(loader);
                        if (loadingPackageUsers == null || !loadingPackageUsers.contains(Integer.valueOf(fileUserId))) {
                            loaderIt.remove();
                        }
                    }
                    if (fileInfo.mLoadingPackages.isEmpty()) {
                        fileIt.remove();
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class DynamicCodeFile {
        final char mFileType;
        final Set<String> mLoadingPackages;
        final int mUserId;

        private DynamicCodeFile(char type, int user, String... packages) {
            this.mFileType = type;
            this.mUserId = user;
            this.mLoadingPackages = new HashSet(Arrays.asList(packages));
        }

        private DynamicCodeFile(DynamicCodeFile original) {
            this.mFileType = original.mFileType;
            this.mUserId = original.mUserId;
            this.mLoadingPackages = new HashSet(original.mLoadingPackages);
        }
    }
}