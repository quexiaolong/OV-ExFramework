package com.android.server.usage;

import android.util.ArrayMap;
import android.util.Slog;
import android.util.SparseArray;
import java.util.ArrayList;

/* loaded from: classes2.dex */
public final class PackagesTokenData {
    private static final int PACKAGE_NAME_INDEX = 0;
    public static final int UNASSIGNED_TOKEN = -1;
    public int counter = 1;
    public final SparseArray<ArrayList<String>> tokensToPackagesMap = new SparseArray<>();
    public final ArrayMap<String, ArrayMap<String, Integer>> packagesToTokensMap = new ArrayMap<>();
    public final ArrayMap<String, Long> removedPackagesMap = new ArrayMap<>();

    public int getPackageTokenOrAdd(String packageName, long timeStamp) {
        Long timeRemoved = this.removedPackagesMap.get(packageName);
        if (timeRemoved != null && timeRemoved.longValue() > timeStamp) {
            return -1;
        }
        ArrayMap<String, Integer> packageTokensMap = this.packagesToTokensMap.get(packageName);
        if (packageTokensMap == null) {
            packageTokensMap = new ArrayMap<>();
            this.packagesToTokensMap.put(packageName, packageTokensMap);
        }
        int token = packageTokensMap.getOrDefault(packageName, -1).intValue();
        if (token == -1) {
            int token2 = this.counter;
            this.counter = token2 + 1;
            ArrayList<String> tokenPackages = new ArrayList<>();
            tokenPackages.add(packageName);
            packageTokensMap.put(packageName, Integer.valueOf(token2));
            this.tokensToPackagesMap.put(token2, tokenPackages);
            return token2;
        }
        return token;
    }

    public int getTokenOrAdd(int packageToken, String packageName, String key) {
        if (packageName.equals(key)) {
            return 0;
        }
        int token = this.packagesToTokensMap.get(packageName).getOrDefault(key, -1).intValue();
        if (token == -1) {
            int token2 = this.tokensToPackagesMap.get(packageToken).size();
            this.packagesToTokensMap.get(packageName).put(key, Integer.valueOf(token2));
            this.tokensToPackagesMap.get(packageToken).add(key);
            return token2;
        }
        return token;
    }

    public String getPackageString(int packageToken) {
        ArrayList<String> packageStrings = this.tokensToPackagesMap.get(packageToken);
        if (packageStrings == null) {
            return null;
        }
        return packageStrings.get(0);
    }

    public String getString(int packageToken, int token) {
        try {
            return this.tokensToPackagesMap.get(packageToken).get(token);
        } catch (IndexOutOfBoundsException e) {
            return null;
        } catch (NullPointerException npe) {
            Slog.e("PackagesTokenData", "Unable to find tokenized strings for package " + packageToken, npe);
            return null;
        }
    }

    public int removePackage(String packageName, long timeRemoved) {
        this.removedPackagesMap.put(packageName, Long.valueOf(timeRemoved));
        if (!this.packagesToTokensMap.containsKey(packageName)) {
            return -1;
        }
        int packageToken = this.packagesToTokensMap.get(packageName).get(packageName).intValue();
        this.packagesToTokensMap.remove(packageName);
        this.tokensToPackagesMap.delete(packageToken);
        return packageToken;
    }
}