package com.android.server.people.data;

import android.content.Context;
import android.location.Country;
import android.location.CountryDetector;
import java.util.Locale;

/* loaded from: classes.dex */
class Utils {
    /* JADX INFO: Access modifiers changed from: package-private */
    public static String getCurrentCountryIso(Context context) {
        Country country;
        String countryIso = null;
        CountryDetector detector = (CountryDetector) context.getSystemService("country_detector");
        if (detector != null && (country = detector.detectCountry()) != null) {
            countryIso = country.getCountryIso();
        }
        if (countryIso == null) {
            return Locale.getDefault().getCountry();
        }
        return countryIso;
    }

    private Utils() {
    }
}