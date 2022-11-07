package com.android.server.net.watchlist;

import com.android.internal.util.HexDump;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class HarmfulCrcs {
    private final Set<Integer> mCrcSet;

    /* JADX INFO: Access modifiers changed from: package-private */
    public HarmfulCrcs(List<byte[]> digests) {
        HashSet<Integer> crcSet = new HashSet<>();
        int size = digests.size();
        for (int i = 0; i < size; i++) {
            byte[] bytes = digests.get(i);
            if (bytes.length <= 4) {
                int crc = 0;
                for (byte b : bytes) {
                    crc = (crc << 8) | (b & 255);
                }
                crcSet.add(Integer.valueOf(crc));
            }
        }
        this.mCrcSet = Collections.unmodifiableSet(crcSet);
    }

    public boolean contains(int crc) {
        return this.mCrcSet.contains(Integer.valueOf(crc));
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        for (Integer num : this.mCrcSet) {
            int crc = num.intValue();
            pw.println(HexDump.toHexString(crc));
        }
        pw.println("");
    }
}