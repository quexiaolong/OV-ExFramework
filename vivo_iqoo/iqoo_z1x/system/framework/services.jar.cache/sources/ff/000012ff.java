package com.android.server.net.watchlist;

import android.privacy.DifferentialPrivacyEncoder;
import android.privacy.internal.longitudinalreporting.LongitudinalReportingConfig;
import android.privacy.internal.longitudinalreporting.LongitudinalReportingEncoder;
import com.android.server.net.watchlist.WatchlistReportDbHelper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* loaded from: classes.dex */
class PrivacyUtils {
    private static final boolean DEBUG = false;
    private static final String ENCODER_ID_PREFIX = "watchlist_encoder:";
    private static final double PROB_F = 0.469d;
    private static final double PROB_P = 0.28d;
    private static final double PROB_Q = 1.0d;
    private static final String TAG = "PrivacyUtils";

    private PrivacyUtils() {
    }

    static DifferentialPrivacyEncoder createInsecureDPEncoderForTest(String appDigest) {
        LongitudinalReportingConfig config = createLongitudinalReportingConfig(appDigest);
        return LongitudinalReportingEncoder.createInsecureEncoderForTest(config);
    }

    static DifferentialPrivacyEncoder createSecureDPEncoder(byte[] userSecret, String appDigest) {
        LongitudinalReportingConfig config = createLongitudinalReportingConfig(appDigest);
        return LongitudinalReportingEncoder.createEncoder(config, userSecret);
    }

    private static LongitudinalReportingConfig createLongitudinalReportingConfig(String appDigest) {
        return new LongitudinalReportingConfig(ENCODER_ID_PREFIX + appDigest, (double) PROB_F, (double) PROB_P, (double) PROB_Q);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Map<String, Boolean> createDpEncodedReportMap(boolean isSecure, byte[] userSecret, List<String> appDigestList, WatchlistReportDbHelper.AggregatedResult aggregatedResult) {
        DifferentialPrivacyEncoder encoder;
        int appDigestListSize = appDigestList.size();
        HashMap<String, Boolean> resultMap = new HashMap<>(appDigestListSize);
        for (int i = 0; i < appDigestListSize; i++) {
            String appDigest = appDigestList.get(i);
            if (isSecure) {
                encoder = createSecureDPEncoder(userSecret, appDigest);
            } else {
                encoder = createInsecureDPEncoderForTest(appDigest);
            }
            boolean visitedWatchlist = aggregatedResult.appDigestList.contains(appDigest);
            boolean z = false;
            if ((encoder.encodeBoolean(visitedWatchlist)[0] & 1) == 1) {
                z = true;
            }
            boolean encodedVisitedWatchlist = z;
            resultMap.put(appDigest, Boolean.valueOf(encodedVisitedWatchlist));
        }
        return resultMap;
    }
}