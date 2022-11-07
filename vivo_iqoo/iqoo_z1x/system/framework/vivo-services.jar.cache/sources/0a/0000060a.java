package com.vivo.services.autorecover;

import android.content.ContentValues;
import android.os.SystemProperties;
import android.text.TextUtils;
import com.android.internal.util.FastPrintWriter;
import com.vivo.face.common.data.Constants;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import vivo.app.configuration.ContentValuesList;
import vivo.util.VSlog;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class ExceptionMatcher {
    private static final String CURRENT_VERSION = SystemProperties.get("ro.vivo.product.version", "UNKNOWN");
    static final String ITEM_SEPARATOR = "ITEM-SEPARATOR";
    static final String KEY_FULL_MATCH_FINGERPRINT = "FULL_MATCH_FINGERPRINT";
    static final String KEY_MESSAGES = "MESSAGES";
    static final String KEY_TARGET_VERSION = "TARGET_VERSION";
    static final String KEY_TRACES = "TRACES";
    public static final String MATCH_FULL = "MATCH_FULL";
    public static final String MATCH_MESSAGE = "MATCH_MESSAGE";
    public static final String MATCH_PARTIAL_TRACE_WITHOUT_MESSAGE = "MATCH_PARTIAL_TRACE_WITHOUT_MESSAGE";
    public static final String MATCH_PARTIAL_TRACE_WITH_MESSAGE = "MATCH_PARTIAL_TRACE_WITH_MESSAGE";
    static final String MESSAGE_SEPARATOR = "MESSAGE-SEPARATOR";
    static final String TARGET_AT = "at";
    static final String TARGET_BRACKETS = "(";
    static final String TARGET_CAUSE_BY = "Caused by";
    static final String TRACE_SEPARATOR = "TRACE-SEPARATOR";
    static final String VERSION_SEPARATOR = ":";
    private final ArrayList<ExceptionMatcherRecord> mExceptionMatchers = new ArrayList<>();
    private ContentValuesList mOriginData;

    @Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface VivoExceptionMatcherType {
    }

    public boolean matchException(Throwable t) {
        if (t instanceof OutOfMemoryError) {
            return false;
        }
        synchronized (this.mExceptionMatchers) {
            Iterator<ExceptionMatcherRecord> it = this.mExceptionMatchers.iterator();
            while (it.hasNext()) {
                ExceptionMatcherRecord r = it.next();
                if (r.isTargetVersion() && r.match(t)) {
                    VSlog.e(SystemAutoRecoverService.TAG, "exception matched, system will not crash, matcher is " + r.type);
                    return true;
                }
            }
            return false;
        }
    }

    public void updateMatcher(ContentValuesList originData) {
        synchronized (this.mExceptionMatchers) {
            if (originData != null) {
                this.mExceptionMatchers.clear();
                this.mOriginData = originData;
                if (!originData.isEmpty()) {
                    HashMap<String, ContentValues> map = originData.getValues();
                    map.forEach(new BiConsumer() { // from class: com.vivo.services.autorecover.-$$Lambda$ExceptionMatcher$N9TodH8Kp1HcY_B68X9zKidovmU
                        @Override // java.util.function.BiConsumer
                        public final void accept(Object obj, Object obj2) {
                            ExceptionMatcher.this.lambda$updateMatcher$0$ExceptionMatcher((String) obj, (ContentValues) obj2);
                        }
                    });
                }
            }
        }
    }

    public /* synthetic */ void lambda$updateMatcher$0$ExceptionMatcher(String s, ContentValues contentValues) {
        List<String> fingerprints = new ArrayList<>();
        List<List<String>> traces = new ArrayList<>();
        List<List<String>> messages = new ArrayList<>();
        List<String> targetVersions = new ArrayList<>();
        if (verifyData(contentValues, fingerprints, traces, messages, targetVersions)) {
            char c = 65535;
            switch (s.hashCode()) {
                case -1969656640:
                    if (s.equals(MATCH_PARTIAL_TRACE_WITH_MESSAGE)) {
                        c = 2;
                        break;
                    }
                    break;
                case -1915473687:
                    if (s.equals(MATCH_FULL)) {
                        c = 0;
                        break;
                    }
                    break;
                case -1595554114:
                    if (s.equals(MATCH_PARTIAL_TRACE_WITHOUT_MESSAGE)) {
                        c = 3;
                        break;
                    }
                    break;
                case 525115213:
                    if (s.equals(MATCH_MESSAGE)) {
                        c = 1;
                        break;
                    }
                    break;
            }
            if (c == 0) {
                for (String fingerprint : fingerprints) {
                    this.mExceptionMatchers.add(new FullMatchRecord(MATCH_FULL, fingerprint, targetVersions));
                }
            } else if (c == 1) {
                for (List<String> message : messages) {
                    this.mExceptionMatchers.add(new MessagesMatchRecord(MATCH_MESSAGE, message, targetVersions));
                }
            } else if (c != 2) {
                if (c == 3) {
                    for (List<String> trace : traces) {
                        this.mExceptionMatchers.add(new PartialTracesRecord(MATCH_PARTIAL_TRACE_WITHOUT_MESSAGE, trace, true, null, targetVersions));
                    }
                }
            } else if (traces.size() == messages.size()) {
                int size = traces.size();
                for (int i = 0; i < size; i++) {
                    this.mExceptionMatchers.add(new PartialTracesRecord(MATCH_PARTIAL_TRACE_WITH_MESSAGE, traces.get(i), true, messages.get(i), targetVersions));
                }
            }
        }
    }

    private boolean verifyData(ContentValues contentValues, List<String> fingerprint, List<List<String>> tracesContainer, List<List<String>> messagesContainer, List<String> version) {
        String fingerprints = contentValues.getAsString(KEY_FULL_MATCH_FINGERPRINT);
        if (!TextUtils.isEmpty(fingerprints)) {
            fingerprint.addAll(separateRawData(fingerprints.split(ITEM_SEPARATOR)));
        }
        String rawTracesList = contentValues.getAsString(KEY_TRACES);
        if (!TextUtils.isEmpty(rawTracesList)) {
            String[] rawTracesItem = rawTracesList.split(ITEM_SEPARATOR);
            for (String rawTraces : rawTracesItem) {
                if (!TextUtils.isEmpty(rawTraces)) {
                    List<String> tempTraces = separateRawData(rawTraces.split(TRACE_SEPARATOR));
                    if (!tempTraces.isEmpty()) {
                        tracesContainer.add(tempTraces);
                    }
                }
            }
        }
        String rawMessagesList = contentValues.getAsString(KEY_MESSAGES);
        if (!TextUtils.isEmpty(rawMessagesList)) {
            String[] rawMessagesItem = rawMessagesList.split(ITEM_SEPARATOR);
            for (String rawMessages : rawMessagesItem) {
                List<String> tempMessages = separateRawData(rawMessages.split(MESSAGE_SEPARATOR));
                if (!tempMessages.isEmpty()) {
                    messagesContainer.add(tempMessages);
                }
            }
        }
        String targetVersions = contentValues.getAsString(KEY_TARGET_VERSION);
        if (!TextUtils.isEmpty(targetVersions)) {
            String[] versionItem = targetVersions.split(VERSION_SEPARATOR);
            if (versionItem.length > 0) {
                for (String targetVersion : versionItem) {
                    if (!TextUtils.isEmpty(targetVersion)) {
                        version.add(targetVersion);
                    }
                }
            }
        }
        return (fingerprint.isEmpty() && tracesContainer.isEmpty() && messagesContainer.isEmpty()) ? false : true;
    }

    private List<String> separateRawData(String[] rawData) {
        List<String> result = new ArrayList<>();
        for (String data : rawData) {
            if (!TextUtils.isEmpty(data)) {
                String trimmedString = data.trim();
                if (!TextUtils.isEmpty(data)) {
                    result.add(trimmedString);
                }
            }
        }
        return result;
    }

    public ContentValuesList getCurrentList() {
        return this.mOriginData;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static abstract class ExceptionMatcherRecord {
        protected List<String> mTargetVersion;
        protected String type;

        abstract boolean match(Throwable th);

        public ExceptionMatcherRecord(String type, List<String> targetVersion) {
            this.type = type;
            this.mTargetVersion = targetVersion;
        }

        boolean isTargetVersion() {
            if (this.mTargetVersion.isEmpty() || "UNKNOWN".equals(ExceptionMatcher.CURRENT_VERSION)) {
                return true;
            }
            for (String version : this.mTargetVersion) {
                if (ExceptionMatcher.CURRENT_VERSION.equals(version)) {
                    return true;
                }
                if (!TextUtils.isEmpty(version) && !version.contains("_") && ExceptionMatcher.CURRENT_VERSION.startsWith(version)) {
                    return true;
                }
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class FullMatchRecord extends ExceptionMatcherRecord {
        private final String mMd5Fingerprint;

        public FullMatchRecord(String type, String md5Fingerprint, List<String> targetVersion) {
            super(type, targetVersion);
            this.mMd5Fingerprint = md5Fingerprint;
        }

        @Override // com.vivo.services.autorecover.ExceptionMatcher.ExceptionMatcherRecord
        boolean match(Throwable t) {
            String translatedString = ExceptionMatcher.translateThrowableToMD5Fingerprint(t);
            return (TextUtils.isEmpty(translatedString) || TextUtils.isEmpty(this.mMd5Fingerprint) || !this.mMd5Fingerprint.equals(translatedString)) ? false : true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class MessagesMatchRecord extends ExceptionMatcherRecord {
        private final List<String> mMessages;

        public MessagesMatchRecord(String type, List<String> messages, List<String> targetVersion) {
            super(type, targetVersion);
            ArrayList arrayList = new ArrayList(5);
            this.mMessages = arrayList;
            arrayList.addAll(messages);
        }

        @Override // com.vivo.services.autorecover.ExceptionMatcher.ExceptionMatcherRecord
        boolean match(Throwable t) {
            List<String> tempMessages = new ArrayList<>(this.mMessages);
            int tempSize = tempMessages.size();
            List<String> exceptionMessages = getAllMessages(t);
            for (int rawIndex = tempSize - 1; rawIndex > -1; rawIndex--) {
                String message = tempMessages.get(rawIndex);
                int size = exceptionMessages.size();
                int i = size - 1;
                while (true) {
                    if (i > -1) {
                        if (exceptionMessages.get(i).trim().contains(message)) {
                            tempMessages.remove(rawIndex);
                            exceptionMessages.remove(i);
                            break;
                        } else {
                            i--;
                        }
                    } else {
                        break;
                    }
                }
            }
            int rawIndex2 = tempMessages.size();
            return rawIndex2 == 0 && exceptionMessages.size() == 0;
        }

        private List<String> getAllMessages(Throwable t) {
            List<String> tempMessages = new ArrayList<>();
            if (!TextUtils.isEmpty(t.getMessage())) {
                tempMessages.add(t.getMessage());
            }
            for (Throwable causes = t.getCause(); causes != null; causes = causes.getCause()) {
                if (causes.getMessage() != null) {
                    tempMessages.add(causes.getMessage());
                }
            }
            return tempMessages;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class PartialTracesRecord extends ExceptionMatcherRecord {
        private final boolean mMatchMessage;
        private final boolean mMatchPartialTrace;
        private final List<String> mServerMessages;
        private final List<String> mServerTraces;

        public PartialTracesRecord(String type, List<String> traces, boolean partialTrace, List<String> messages, List<String> targetVersion) {
            super(type, targetVersion);
            this.mServerMessages = messages;
            this.mMatchMessage = messages != null && messages.size() > 0;
            this.mServerTraces = traces;
            this.mMatchPartialTrace = partialTrace;
        }

        @Override // com.vivo.services.autorecover.ExceptionMatcher.ExceptionMatcherRecord
        boolean match(Throwable t) {
            List<String> currentTraces = new ArrayList<>();
            List<String> currentMessages = new ArrayList<>();
            loadTracesAndMessagesToList(t, currentMessages, currentTraces);
            return matchInternal(currentTraces, this.mServerTraces, this.mMatchPartialTrace) && (!this.mMatchMessage || matchInternal(currentMessages, this.mServerMessages, true));
        }

        private boolean matchInternal(List<String> currentData, List<String> serverData, boolean partial) {
            List<String> rawData = new ArrayList<>(serverData);
            int rawSize = rawData.size();
            for (int rawIndex = rawSize - 1; rawIndex > -1; rawIndex--) {
                String trace = rawData.get(rawIndex);
                int size = currentData.size();
                int i = size - 1;
                while (true) {
                    if (i > -1) {
                        if (currentData.get(i).trim().contains(trace)) {
                            rawData.remove(rawIndex);
                            currentData.remove(i);
                            break;
                        } else {
                            i--;
                        }
                    } else {
                        break;
                    }
                }
            }
            return partial ? rawData.size() == 0 : currentData.size() == 0 && rawData.size() == 0;
        }

        private void loadTracesAndMessagesToList(Throwable t, List<String> targetMessages, List<String> targetTraces) {
            StackTraceElement[] stackTrace;
            StackTraceElement[] stackTrace2;
            if (!TextUtils.isEmpty(t.getMessage())) {
                targetMessages.add(t.getMessage());
            }
            for (StackTraceElement element : t.getStackTrace()) {
                if (!this.mMatchPartialTrace || !targetTraces.contains(element.toString())) {
                    targetTraces.add(element.toString());
                }
            }
            for (Throwable cause = t.getCause(); cause != null; cause = cause.getCause()) {
                if (!TextUtils.isEmpty(cause.getMessage()) && !targetMessages.contains(cause.getMessage())) {
                    targetMessages.add(cause.getMessage());
                }
                for (StackTraceElement element2 : cause.getStackTrace()) {
                    if (!this.mMatchPartialTrace || !targetTraces.contains(element2.toString())) {
                        targetTraces.add(element2.toString());
                    }
                }
            }
        }
    }

    public static String translateThrowableToMD5Fingerprint(Throwable t) {
        StringWriter sw = new StringWriter();
        FastPrintWriter fastPrintWriter = new FastPrintWriter(sw, false, 256);
        t.printStackTrace((PrintWriter) fastPrintWriter);
        fastPrintWriter.flush();
        String traceString = sanitizeString(sw.toString());
        fastPrintWriter.close();
        List<String> originTraceList = Arrays.asList(traceString.split("\n"));
        int size = originTraceList.size();
        for (int i = size - 1; i >= 0; i--) {
            String finalLine = originTraceList.get(i).replaceFirst("\t", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
            originTraceList.set(i, finalLine.trim());
        }
        List<String> traceList = new ArrayList<>(originTraceList);
        String result = fetchLog(traceList);
        return md5(result);
    }

    private static String sanitizeString(String s) {
        int acceptableLength = 10240 + 10240;
        if (s != null && s.length() > acceptableLength) {
            String replacement = "\n[TRUNCATED " + (s.length() - acceptableLength) + " CHARS]\n";
            StringBuilder sb = new StringBuilder(replacement.length() + acceptableLength);
            sb.append(s.substring(0, 10240));
            sb.append(replacement);
            sb.append(s.substring(s.length() - 10240));
            return sb.toString().trim();
        }
        return s;
    }

    private static String fetchLog(List<String> lines) {
        int size = lines.size();
        if (size == 0) {
            return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        }
        int causedByMark = -1;
        int i = 0;
        while (true) {
            if (i < lines.size()) {
                if (!lines.get(i).trim().startsWith(TARGET_CAUSE_BY)) {
                    i++;
                } else {
                    causedByMark = i;
                    break;
                }
            } else {
                break;
            }
        }
        StringBuilder trace = new StringBuilder();
        if (causedByMark > -1) {
            String endLine = lines.get(size - 1);
            if (!TextUtils.isEmpty(endLine) && endLine.startsWith("...") && endLine.endsWith("more")) {
                size--;
                lines.remove(size);
            }
            for (int i2 = causedByMark; i2 < size; i2++) {
                String line = lines.get(i2);
                if (line.startsWith(TARGET_AT)) {
                    String temp = line.trim().replaceFirst(TARGET_AT, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK).trim();
                    if (temp.contains(TARGET_BRACKETS)) {
                        temp = temp.substring(0, temp.indexOf(TARGET_BRACKETS)).trim();
                    }
                    trace.append(temp);
                } else {
                    trace.append(line);
                }
                if (lines.indexOf(line) != lines.size() - 1) {
                    trace.append("\n");
                }
            }
        } else {
            for (String line2 : lines) {
                if (line2.startsWith(TARGET_AT)) {
                    String temp2 = line2.replaceFirst(TARGET_AT, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK).trim();
                    if (temp2.contains(TARGET_BRACKETS)) {
                        temp2 = temp2.substring(0, temp2.indexOf(TARGET_BRACKETS)).trim();
                    }
                    trace.append(temp2);
                    if (lines.indexOf(line2) != size - 1) {
                        trace.append("\n");
                    }
                }
            }
        }
        return trace.toString();
    }

    private static String md5(String s) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte[] messageDigest = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(aMessageDigest & 255);
                if (h.length() < 2) {
                    h = "0" + h;
                }
                hexString.append(h);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return s;
        }
    }
}