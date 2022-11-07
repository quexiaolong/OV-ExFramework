package com.android.server.location;

import android.location.Location;
import android.net.wifi.ScanResult;
import com.vivo.common.utils.VLog;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public class VivoFusedGpsUtil {
    private static double EARTH_RADIUS = 6378137.0d;
    private static final String TAG = "VivoFusedGpsUtil";
    private boolean DEBUG = false;
    private HashMap<String, Integer> mScoreParameterHashMap = new HashMap<>();
    private int mLastWifiScan = 0;
    private int mLastL4WifiCount = 0;
    private double rateOfCn0AndOthers = 0.0d;
    private double[] mLast3RateOfCn0AndOthers = new double[3];
    private int[] mLast3GpsScore = new int[3];
    private int mGpsReportCount = 0;

    public void calculateLocation(Location mFinalLocation, Location mGpsLocaiotn, Location mNetworkLocation, double rateOfGps) {
        mFinalLocation.setLatitude((mGpsLocaiotn.getLatitude() * rateOfGps) + (mNetworkLocation.getLatitude() * (1.0d - rateOfGps)));
        mFinalLocation.setLongitude((mGpsLocaiotn.getLongitude() * rateOfGps) + (mNetworkLocation.getLongitude() * (1.0d - rateOfGps)));
    }

    public int calculateWifiScore(List<ScanResult> mWifiScanResults) {
        double score;
        double score2 = 0.0d;
        int level4Count = 0;
        int level3Count = 0;
        double level4Score = 0.0d;
        double level3Score = 0.0d;
        this.mLastL4WifiCount = 0;
        try {
            Iterator<ScanResult> it = mWifiScanResults.iterator();
            while (it.hasNext()) {
                ScanResult scanResult = it.next();
                Iterator<ScanResult> it2 = it;
                if (scanResult.level >= -70) {
                    level4Count++;
                } else if (scanResult.level >= -75) {
                    level3Count++;
                }
                it = it2;
            }
        } catch (Exception e) {
            VLog.e(TAG, "calculate scan results: ", e);
        }
        this.mLastL4WifiCount = level4Count;
        try {
            if (this.mScoreParameterHashMap != null) {
                if (!this.mScoreParameterHashMap.containsKey("L4WifiCount") || this.mScoreParameterHashMap.get("L4WifiCount").intValue() <= 0) {
                    score = 0.0d;
                } else if (!this.mScoreParameterHashMap.containsKey("L4WifiScore") || this.mScoreParameterHashMap.get("L4WifiScore").intValue() <= 0) {
                    score = 0.0d;
                } else {
                    score = 0.0d;
                    try {
                        level4Score = this.mScoreParameterHashMap.get("L4WifiScore").intValue() / (Math.exp((level4Count - this.mScoreParameterHashMap.get("L4WifiCount").intValue()) * (-0.45d)) + 1.0d);
                    } catch (Exception e2) {
                        e = e2;
                        score2 = score;
                        e.printStackTrace();
                        return (int) score2;
                    }
                }
                if (this.mScoreParameterHashMap.containsKey("L3WifiCount") && this.mScoreParameterHashMap.get("L3WifiCount").intValue() > 0 && this.mScoreParameterHashMap.containsKey("L3WifiScore") && this.mScoreParameterHashMap.get("L3WifiScore").intValue() > 0) {
                    level3Score = this.mScoreParameterHashMap.get("L3WifiScore").intValue() / (Math.exp((level3Count - this.mScoreParameterHashMap.get("L3WifiCount").intValue()) * (-0.45d)) + 1.0d);
                }
                score2 = level4Score + level3Score;
            }
        } catch (Exception e3) {
            e = e3;
        }
        try {
            log("calculateWifiScore:" + score2 + " level4Score:" + level4Score + " L4WifiCount:" + level4Count + " level3Score:" + level3Score + " level3Count:" + level3Count);
        } catch (Exception e4) {
            e = e4;
            e.printStackTrace();
            return (int) score2;
        }
        return (int) score2;
    }

    public int getLastL4WifiCount() {
        return this.mLastL4WifiCount;
    }

    public void setScoreParameterHashMap(HashMap<String, Integer> hashmap) {
        if (hashmap != null) {
            this.mScoreParameterHashMap = hashmap;
        }
    }

    public int calculateGpsScore(int svCount, float[] mCn0s, float[] mSvElevations, float[] mSvAzimuths) {
        int mCn0Score;
        int mAziScore;
        List<Integer> mTopCn0List = new ArrayList<>();
        List<Integer> mTopAziList = new ArrayList<>();
        int countOfEleScore = 0;
        this.rateOfCn0AndOthers = 0.0d;
        this.mGpsReportCount++;
        for (int i = 0; i < svCount; i++) {
            if (mSvElevations[i] >= 30.0d) {
                if (mCn0s[i] > 0.0f) {
                    mTopCn0List.add(Integer.valueOf((int) mCn0s[i]));
                }
                if (mCn0s[i] > 10.0f) {
                    log("mSvAzimuths: " + mSvAzimuths[i] + " mSvElevations: " + mSvElevations[i]);
                    mTopAziList.add(Integer.valueOf((int) mSvAzimuths[i]));
                    if (mSvElevations[i] > 40.0d) {
                        countOfEleScore++;
                    }
                }
            }
        }
        log("countOfEleScore: " + countOfEleScore);
        int countOfEleScore2 = countOfEleScore <= 10 ? countOfEleScore : 10;
        try {
            Comparator comparator = new Comparator<Integer>() { // from class: com.android.server.location.VivoFusedGpsUtil.1
                @Override // java.util.Comparator
                public int compare(Integer o1, Integer o2) {
                    return o2.compareTo(o1);
                }
            };
            mTopCn0List.sort(comparator);
            mTopAziList.sort(comparator);
        } catch (Exception e) {
            VLog.e(TAG, "sort failed", e);
        }
        int mCn0Score2 = 0;
        if (mTopCn0List.size() >= 8) {
            for (int i2 = 2; i2 < 8; i2++) {
                mCn0Score2 += mTopCn0List.get(i2).intValue();
            }
        }
        log("average cn0: " + mCn0Score2);
        int mCn0Score3 = mCn0Score2 / 6;
        if (mCn0Score3 >= 15) {
            int mCn0Score4 = (mCn0Score3 * 4) - 60;
            mCn0Score = mCn0Score4 <= 60 ? mCn0Score4 : 60;
        } else {
            mCn0Score = 0;
        }
        int mAziScore2 = 0;
        if (mTopAziList.size() >= 2) {
            for (int i3 = 0; i3 < mTopAziList.size(); i3++) {
                if (i3 < mTopAziList.size() - 1) {
                    int tempDiffAzi = mTopAziList.get(i3).intValue() - mTopAziList.get(i3 + 1).intValue();
                    if (tempDiffAzi > mAziScore2) {
                        mAziScore2 = tempDiffAzi;
                    }
                } else {
                    int tempDiffAzi2 = (360 - mTopAziList.get(0).intValue()) + mTopAziList.get(i3).intValue();
                    if (tempDiffAzi2 > mAziScore2) {
                        mAziScore2 = tempDiffAzi2;
                    }
                }
            }
        }
        log("max diff of azi: " + mAziScore2);
        if (mAziScore2 <= 0 || mAziScore2 > 120) {
            mAziScore = 0;
        } else if (mAziScore2 > 40) {
            mAziScore = (int) (((mAziScore2 - 40) * (-0.375d)) + 30.0d);
        } else {
            mAziScore = 30;
        }
        log("calculateGpsScore Cn0Score: " + mCn0Score + " EleScore: " + countOfEleScore2 + " AziScore: " + mAziScore);
        mTopCn0List.clear();
        mTopAziList.clear();
        if (mCn0Score > 0) {
            this.rateOfCn0AndOthers = calculateAverageRate(((countOfEleScore2 + mAziScore) * 1.0d) / mCn0Score);
        }
        return calculateAverageGpsScore(mCn0Score + countOfEleScore2 + mAziScore);
    }

    public int calculateAverageGpsScore(int gpsScore) {
        int i = this.mGpsReportCount;
        if (i == 1) {
            this.mLast3GpsScore[0] = gpsScore;
            return gpsScore;
        } else if (i == 2) {
            int[] iArr = this.mLast3GpsScore;
            iArr[1] = gpsScore;
            return (iArr[0] + iArr[1]) / 2;
        } else if (i == 3) {
            int[] iArr2 = this.mLast3GpsScore;
            iArr2[2] = gpsScore;
            return ((iArr2[0] + iArr2[1]) + iArr2[2]) / 3;
        } else if (i > 3) {
            int[] iArr3 = this.mLast3GpsScore;
            iArr3[0] = iArr3[1];
            iArr3[1] = iArr3[2];
            iArr3[2] = gpsScore;
            return ((iArr3[0] + iArr3[1]) + iArr3[2]) / 3;
        } else {
            return gpsScore;
        }
    }

    public double calculateAverageRate(double rate) {
        int i = this.mGpsReportCount;
        if (i == 1) {
            this.mLast3RateOfCn0AndOthers[0] = rate;
            return rate;
        } else if (i == 2) {
            double[] dArr = this.mLast3RateOfCn0AndOthers;
            dArr[1] = rate;
            return (dArr[0] + dArr[1]) / 2.0d;
        } else if (i == 3) {
            double[] dArr2 = this.mLast3RateOfCn0AndOthers;
            dArr2[2] = rate;
            return ((dArr2[0] + dArr2[1]) + dArr2[2]) / 3.0d;
        } else if (i > 3) {
            double[] dArr3 = this.mLast3RateOfCn0AndOthers;
            dArr3[0] = dArr3[1];
            dArr3[1] = dArr3[2];
            dArr3[2] = rate;
            return ((dArr3[0] + dArr3[1]) + dArr3[2]) / 3.0d;
        } else {
            return rate;
        }
    }

    public double getRateOfCn0AndOthers() {
        return this.rateOfCn0AndOthers;
    }

    public double calculateDistance(Location gpsLocation, Location networkLocation) {
        double gpsLatitude = rad(gpsLocation.getLatitude());
        double networkLatitude = rad(networkLocation.getLatitude());
        double diffOfLatitude = gpsLatitude - networkLatitude;
        double diffofLongitude = rad(gpsLocation.getLongitude()) - rad(networkLocation.getLongitude());
        double distance = Math.asin(Math.sqrt(Math.pow(Math.sin(diffOfLatitude / 2.0d), 2.0d) + (Math.cos(gpsLatitude) * Math.cos(networkLatitude) * Math.pow(Math.sin(diffofLongitude / 2.0d), 2.0d)))) * 2.0d;
        return Math.round((EARTH_RADIUS * distance) * 1000.0d) / 1000.0d;
    }

    private double rad(double d) {
        return (3.141592653589793d * d) / 180.0d;
    }

    public void clear() {
        this.mGpsReportCount = 0;
        for (int i = 0; i < 3; i++) {
            this.mLast3GpsScore[i] = 0;
            this.mLast3RateOfCn0AndOthers[i] = 0.0d;
        }
    }

    private void log(String msg) {
        if (this.DEBUG) {
            VLog.d(TAG, msg);
        }
    }

    public void setDebug(boolean debug) {
        this.DEBUG = debug;
    }
}