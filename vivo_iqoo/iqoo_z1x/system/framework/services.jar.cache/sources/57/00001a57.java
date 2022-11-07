package com.android.server.tv.tunerresourcemanager;

import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.Xml;
import com.android.server.SystemService;
import com.android.server.display.color.DisplayTransformManager;
import com.android.server.voiceinteraction.DatabaseHelper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/* loaded from: classes2.dex */
public class UseCasePriorityHints {
    private static final int INVALID_PRIORITY_VALUE = -1;
    private static final int INVALID_USE_CASE = -1;
    private static final String PATH_TO_VENDOR_CONFIG_XML = "/vendor/etc/tunerResourceManagerUseCaseConfig.xml";
    private static final String TAG = "UseCasePriorityHints";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final String NS = null;
    SparseArray<int[]> mPriorityHints = new SparseArray<>();
    Set<Integer> mVendorDefinedUseCase = new HashSet();
    private int mDefaultForeground = DisplayTransformManager.LEVEL_COLOR_MATRIX_SATURATION;
    private int mDefaultBackground = 50;

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getForegroundPriority(int useCase) {
        if (this.mPriorityHints.get(useCase) != null && this.mPriorityHints.get(useCase).length == 2) {
            return this.mPriorityHints.get(useCase)[0];
        }
        return this.mDefaultForeground;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getBackgroundPriority(int useCase) {
        if (this.mPriorityHints.get(useCase) != null && this.mPriorityHints.get(useCase).length == 2) {
            return this.mPriorityHints.get(useCase)[1];
        }
        return this.mDefaultBackground;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isDefinedUseCase(int useCase) {
        return this.mVendorDefinedUseCase.contains(Integer.valueOf(useCase)) || isPredefinedUseCase(useCase);
    }

    public void parse() {
        File file = new File(PATH_TO_VENDOR_CONFIG_XML);
        if (file.exists()) {
            try {
                InputStream in = new FileInputStream(file);
                parseInternal(in);
                return;
            } catch (IOException e) {
                Slog.e(TAG, "Error reading vendor file: " + file, e);
                return;
            } catch (XmlPullParserException e2) {
                Slog.e(TAG, "Unable to parse vendor file: " + file, e2);
                return;
            }
        }
        if (DEBUG) {
            Slog.i(TAG, "no vendor priority configuration available. Using default priority");
        }
        addNewUseCasePriority(100, 180, 100);
        addNewUseCasePriority(200, 450, 200);
        addNewUseCasePriority(DisplayTransformManager.LEVEL_COLOR_MATRIX_INVERT_COLOR, 480, DisplayTransformManager.LEVEL_COLOR_MATRIX_INVERT_COLOR);
        addNewUseCasePriority(DisplayTransformManager.LEVEL_NLL_COLOR_MATRIX, 490, DisplayTransformManager.LEVEL_NLL_COLOR_MATRIX);
        addNewUseCasePriority(SystemService.PHASE_SYSTEM_SERVICES_READY, SystemService.PHASE_THIRD_PARTY_APPS_CAN_START, SystemService.PHASE_SYSTEM_SERVICES_READY);
    }

    protected void parseInternal(InputStream in) throws IOException, XmlPullParserException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature("http://xmlpull.org/v1/doc/features.html#process-namespaces", false);
            parser.setInput(in, null);
            parser.nextTag();
            readUseCase(parser);
            in.close();
            for (int i = 0; i < this.mPriorityHints.size(); i++) {
                int useCase = this.mPriorityHints.keyAt(i);
                int[] priorities = this.mPriorityHints.get(useCase);
                if (DEBUG) {
                    Slog.d(TAG, "{defaultFg=" + this.mDefaultForeground + ", defaultBg=" + this.mDefaultBackground + "}");
                    Slog.d(TAG, "{useCase=" + useCase + ", fg=" + priorities[0] + ", bg=" + priorities[1] + "}");
                }
            }
        } catch (IOException | XmlPullParserException e) {
            throw e;
        }
    }

    private void readUseCase(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(2, NS, "config");
        while (parser.next() != 3) {
            if (parser.getEventType() == 2) {
                String name = parser.getName();
                if (name.equals("useCaseDefault")) {
                    this.mDefaultForeground = readAttributeToInt("fgPriority", parser);
                    this.mDefaultBackground = readAttributeToInt("bgPriority", parser);
                    parser.nextTag();
                    parser.require(3, NS, name);
                } else if (name.equals("useCasePreDefined")) {
                    int useCase = formatTypeToNum(DatabaseHelper.SoundModelContract.KEY_TYPE, parser);
                    if (useCase == -1) {
                        Slog.e(TAG, "Wrong predefined use case name given in the vendor config.");
                    } else {
                        addNewUseCasePriority(useCase, readAttributeToInt("fgPriority", parser), readAttributeToInt("bgPriority", parser));
                        parser.nextTag();
                        parser.require(3, NS, name);
                    }
                } else if (name.equals("useCaseVendor")) {
                    int useCase2 = readAttributeToInt("id", parser);
                    addNewUseCasePriority(useCase2, readAttributeToInt("fgPriority", parser), readAttributeToInt("bgPriority", parser));
                    this.mVendorDefinedUseCase.add(Integer.valueOf(useCase2));
                    parser.nextTag();
                    parser.require(3, NS, name);
                } else {
                    skip(parser);
                }
            }
        }
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != 2) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            int next = parser.next();
            if (next == 2) {
                depth++;
            } else if (next == 3) {
                depth--;
            }
        }
    }

    private int readAttributeToInt(String attributeName, XmlPullParser parser) {
        return Integer.valueOf(parser.getAttributeValue(null, attributeName)).intValue();
    }

    private void addNewUseCasePriority(int useCase, int fgPriority, int bgPriority) {
        int[] priorities = {fgPriority, bgPriority};
        this.mPriorityHints.append(useCase, priorities);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private static int formatTypeToNum(String attributeName, XmlPullParser parser) {
        boolean z;
        String useCaseName = parser.getAttributeValue(null, attributeName);
        switch (useCaseName.hashCode()) {
            case -884787515:
                if (useCaseName.equals("USE_CASE_BACKGROUND")) {
                    z = false;
                    break;
                }
                z = true;
                break;
            case 377959794:
                if (useCaseName.equals("USE_CASE_PLAYBACK")) {
                    z = true;
                    break;
                }
                z = true;
                break;
            case 1222007747:
                if (useCaseName.equals("USE_CASE_LIVE")) {
                    z = true;
                    break;
                }
                z = true;
                break;
            case 1222209876:
                if (useCaseName.equals("USE_CASE_SCAN")) {
                    z = true;
                    break;
                }
                z = true;
                break;
            case 1990900072:
                if (useCaseName.equals("USE_CASE_RECORD")) {
                    z = true;
                    break;
                }
                z = true;
                break;
            default:
                z = true;
                break;
        }
        if (z) {
            if (!z) {
                if (!z) {
                    if (!z) {
                        if (!z) {
                            return -1;
                        }
                        return SystemService.PHASE_SYSTEM_SERVICES_READY;
                    }
                    return DisplayTransformManager.LEVEL_NLL_COLOR_MATRIX;
                }
                return DisplayTransformManager.LEVEL_COLOR_MATRIX_INVERT_COLOR;
            }
            return 200;
        }
        return 100;
    }

    private static boolean isPredefinedUseCase(int useCase) {
        if (useCase == 100 || useCase == 200 || useCase == 300 || useCase == 400 || useCase == 500) {
            return true;
        }
        return false;
    }
}