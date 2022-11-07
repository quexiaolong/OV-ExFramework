package com.android.server.soundtrigger_middleware;

import java.util.regex.Pattern;

/* loaded from: classes2.dex */
public class UuidUtil {
    static final String FORMAT = "%08x-%04x-%04x-%04x-%02x%02x%02x%02x%02x%02x";
    static final Pattern PATTERN = Pattern.compile("^([a-fA-F0-9]{8})-([a-fA-F0-9]{4})-([a-fA-F0-9]{4})-([a-fA-F0-9]{4})-([a-fA-F0-9]{2})([a-fA-F0-9]{2})([a-fA-F0-9]{2})([a-fA-F0-9]{2})([a-fA-F0-9]{2})([a-fA-F0-9]{2})$");
}