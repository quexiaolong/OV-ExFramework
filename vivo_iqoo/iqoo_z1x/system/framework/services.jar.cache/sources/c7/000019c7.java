package com.android.server.textservices;

import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Locale;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public final class LocaleUtils {
    LocaleUtils() {
    }

    public static ArrayList<Locale> getSuitableLocalesForSpellChecker(Locale systemLocale) {
        Locale systemLocaleLanguageCountryVariant;
        Locale systemLocaleLanguageCountry;
        Locale systemLocaleLanguage;
        if (systemLocale != null) {
            String language = systemLocale.getLanguage();
            boolean hasLanguage = !TextUtils.isEmpty(language);
            String country = systemLocale.getCountry();
            boolean hasCountry = !TextUtils.isEmpty(country);
            String variant = systemLocale.getVariant();
            boolean hasVariant = !TextUtils.isEmpty(variant);
            if (hasLanguage && hasCountry && hasVariant) {
                systemLocaleLanguageCountryVariant = new Locale(language, country, variant);
            } else {
                systemLocaleLanguageCountryVariant = null;
            }
            if (hasLanguage && hasCountry) {
                systemLocaleLanguageCountry = new Locale(language, country);
            } else {
                systemLocaleLanguageCountry = null;
            }
            if (hasLanguage) {
                systemLocaleLanguage = new Locale(language);
            } else {
                systemLocaleLanguage = null;
            }
        } else {
            systemLocaleLanguageCountryVariant = null;
            systemLocaleLanguageCountry = null;
            systemLocaleLanguage = null;
        }
        ArrayList<Locale> locales = new ArrayList<>();
        if (systemLocaleLanguageCountryVariant != null) {
            locales.add(systemLocaleLanguageCountryVariant);
        }
        if (Locale.ENGLISH.equals(systemLocaleLanguage)) {
            if (systemLocaleLanguageCountry != null) {
                if (systemLocaleLanguageCountry != null) {
                    locales.add(systemLocaleLanguageCountry);
                }
                if (!Locale.US.equals(systemLocaleLanguageCountry)) {
                    locales.add(Locale.US);
                }
                if (!Locale.UK.equals(systemLocaleLanguageCountry)) {
                    locales.add(Locale.UK);
                }
                locales.add(Locale.ENGLISH);
            } else {
                locales.add(Locale.ENGLISH);
                locales.add(Locale.US);
                locales.add(Locale.UK);
            }
        } else {
            if (systemLocaleLanguageCountry != null) {
                locales.add(systemLocaleLanguageCountry);
            }
            if (systemLocaleLanguage != null) {
                locales.add(systemLocaleLanguage);
            }
            locales.add(Locale.US);
            locales.add(Locale.UK);
            locales.add(Locale.ENGLISH);
        }
        return locales;
    }
}