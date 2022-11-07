package com.android.server.net.watchlist;

import android.os.FileUtils;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.HexDump;
import com.android.internal.util.XmlUtils;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class WatchlistConfig {
    private static final String NETWORK_WATCHLIST_DB_FOR_TEST_PATH = "/data/misc/network_watchlist/network_watchlist_for_test.xml";
    private static final String NETWORK_WATCHLIST_DB_PATH = "/data/misc/network_watchlist/network_watchlist.xml";
    private static final String TAG = "WatchlistConfig";
    private static final WatchlistConfig sInstance = new WatchlistConfig();
    private volatile CrcShaDigests mDomainDigests;
    private volatile CrcShaDigests mIpDigests;
    private boolean mIsSecureConfig;
    private File mXmlFile;

    /* loaded from: classes.dex */
    private static class XmlTags {
        private static final String CRC32_DOMAIN = "crc32-domain";
        private static final String CRC32_IP = "crc32-ip";
        private static final String HASH = "hash";
        private static final String SHA256_DOMAIN = "sha256-domain";
        private static final String SHA256_IP = "sha256-ip";
        private static final String WATCHLIST_CONFIG = "watchlist-config";

        private XmlTags() {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class CrcShaDigests {
        public final HarmfulCrcs crc32s;
        public final HarmfulDigests sha256Digests;

        CrcShaDigests(HarmfulCrcs crc32s, HarmfulDigests sha256Digests) {
            this.crc32s = crc32s;
            this.sha256Digests = sha256Digests;
        }
    }

    public static WatchlistConfig getInstance() {
        return sInstance;
    }

    private WatchlistConfig() {
        this(new File(NETWORK_WATCHLIST_DB_PATH));
    }

    protected WatchlistConfig(File xmlFile) {
        this.mIsSecureConfig = true;
        this.mXmlFile = xmlFile;
        reloadConfig();
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public void reloadConfig() {
        if (!this.mXmlFile.exists()) {
            return;
        }
        try {
            FileInputStream stream = new FileInputStream(this.mXmlFile);
            List<byte[]> crc32DomainList = new ArrayList<>();
            List<byte[]> sha256DomainList = new ArrayList<>();
            List<byte[]> crc32IpList = new ArrayList<>();
            List<byte[]> sha256IpList = new ArrayList<>();
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(stream, StandardCharsets.UTF_8.name());
            parser.nextTag();
            parser.require(2, null, "watchlist-config");
            while (parser.nextTag() == 2) {
                String tagName = parser.getName();
                char c = 65535;
                switch (tagName.hashCode()) {
                    case -1862636386:
                        if (tagName.equals("crc32-domain")) {
                            c = 0;
                            break;
                        }
                        break;
                    case -14835926:
                        if (tagName.equals("sha256-domain")) {
                            c = 2;
                            break;
                        }
                        break;
                    case 835385997:
                        if (tagName.equals("sha256-ip")) {
                            c = 3;
                            break;
                        }
                        break;
                    case 1718657537:
                        if (tagName.equals("crc32-ip")) {
                            c = 1;
                            break;
                        }
                        break;
                }
                if (c == 0) {
                    parseHashes(parser, tagName, crc32DomainList);
                } else if (c == 1) {
                    parseHashes(parser, tagName, crc32IpList);
                } else if (c == 2) {
                    parseHashes(parser, tagName, sha256DomainList);
                } else if (c == 3) {
                    parseHashes(parser, tagName, sha256IpList);
                } else {
                    Log.w(TAG, "Unknown element: " + parser.getName());
                    XmlUtils.skipCurrentTag(parser);
                }
            }
            parser.require(3, null, "watchlist-config");
            this.mDomainDigests = new CrcShaDigests(new HarmfulCrcs(crc32DomainList), new HarmfulDigests(sha256DomainList));
            this.mIpDigests = new CrcShaDigests(new HarmfulCrcs(crc32IpList), new HarmfulDigests(sha256IpList));
            Log.i(TAG, "Reload watchlist done");
            stream.close();
        } catch (IOException | IllegalStateException | IndexOutOfBoundsException | NullPointerException | NumberFormatException | XmlPullParserException e) {
            Slog.e(TAG, "Failed parsing xml", e);
        }
    }

    private void parseHashes(XmlPullParser parser, String tagName, List<byte[]> hashList) throws IOException, XmlPullParserException {
        parser.require(2, null, tagName);
        while (parser.nextTag() == 2) {
            parser.require(2, null, "hash");
            byte[] hash = HexDump.hexStringToByteArray(parser.nextText());
            parser.require(3, null, "hash");
            hashList.add(hash);
        }
        parser.require(3, null, tagName);
    }

    public boolean containsDomain(String domain) {
        CrcShaDigests domainDigests = this.mDomainDigests;
        if (domainDigests == null) {
            return false;
        }
        int crc32 = getCrc32(domain);
        if (!domainDigests.crc32s.contains(crc32)) {
            return false;
        }
        byte[] sha256 = getSha256(domain);
        return domainDigests.sha256Digests.contains(sha256);
    }

    public boolean containsIp(String ip) {
        CrcShaDigests ipDigests = this.mIpDigests;
        if (ipDigests == null) {
            return false;
        }
        int crc32 = getCrc32(ip);
        if (!ipDigests.crc32s.contains(crc32)) {
            return false;
        }
        byte[] sha256 = getSha256(ip);
        return ipDigests.sha256Digests.contains(sha256);
    }

    private int getCrc32(String str) {
        CRC32 crc = new CRC32();
        crc.update(str.getBytes());
        return (int) crc.getValue();
    }

    private byte[] getSha256(String str) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA256");
            messageDigest.update(str.getBytes());
            return messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    public boolean isConfigSecure() {
        return this.mIsSecureConfig;
    }

    public byte[] getWatchlistConfigHash() {
        if (this.mXmlFile.exists()) {
            try {
                return DigestUtils.getSha256Hash(this.mXmlFile);
            } catch (IOException | NoSuchAlgorithmException e) {
                Log.e(TAG, "Unable to get watchlist config hash", e);
                return null;
            }
        }
        return null;
    }

    public void setTestMode(InputStream testConfigInputStream) throws IOException {
        Log.i(TAG, "Setting watchlist testing config");
        FileUtils.copyToFileOrThrow(testConfigInputStream, new File(NETWORK_WATCHLIST_DB_FOR_TEST_PATH));
        this.mIsSecureConfig = false;
        this.mXmlFile = new File(NETWORK_WATCHLIST_DB_FOR_TEST_PATH);
        reloadConfig();
    }

    public void removeTestModeConfig() {
        try {
            File f = new File(NETWORK_WATCHLIST_DB_FOR_TEST_PATH);
            if (f.exists()) {
                f.delete();
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to delete test config");
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        byte[] hash = getWatchlistConfigHash();
        StringBuilder sb = new StringBuilder();
        sb.append("Watchlist config hash: ");
        sb.append(hash != null ? HexDump.toHexString(hash) : null);
        pw.println(sb.toString());
        pw.println("Domain CRC32 digest list:");
        if (this.mDomainDigests != null) {
            this.mDomainDigests.crc32s.dump(fd, pw, args);
        }
        pw.println("Domain SHA256 digest list:");
        if (this.mDomainDigests != null) {
            this.mDomainDigests.sha256Digests.dump(fd, pw, args);
        }
        pw.println("Ip CRC32 digest list:");
        if (this.mIpDigests != null) {
            this.mIpDigests.crc32s.dump(fd, pw, args);
        }
        pw.println("Ip SHA256 digest list:");
        if (this.mIpDigests != null) {
            this.mIpDigests.sha256Digests.dump(fd, pw, args);
        }
    }
}