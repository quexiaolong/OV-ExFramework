package android.sysprop;

import android.os.SystemProperties;
import com.android.server.notification.SnoozeHelper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Function;

/* loaded from: classes.dex */
public final class SurfaceFlingerProperties {
    private SurfaceFlingerProperties() {
    }

    private static Boolean tryParseBoolean(String str) {
        char c;
        String lowerCase = str.toLowerCase(Locale.US);
        int hashCode = lowerCase.hashCode();
        if (hashCode == 48) {
            if (lowerCase.equals("0")) {
                c = 2;
            }
            c = 65535;
        } else if (hashCode == 49) {
            if (lowerCase.equals(SnoozeHelper.XML_SNOOZED_NOTIFICATION_VERSION)) {
                c = 0;
            }
            c = 65535;
        } else if (hashCode != 3569038) {
            if (hashCode == 97196323 && lowerCase.equals("false")) {
                c = 3;
            }
            c = 65535;
        } else {
            if (lowerCase.equals("true")) {
                c = 1;
            }
            c = 65535;
        }
        if (c == 0 || c == 1) {
            return Boolean.TRUE;
        }
        if (c == 2 || c == 3) {
            return Boolean.FALSE;
        }
        return null;
    }

    private static Integer tryParseInteger(String str) {
        try {
            return Integer.valueOf(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long tryParseLong(String str) {
        try {
            return Long.valueOf(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Double tryParseDouble(String str) {
        try {
            return Double.valueOf(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String tryParseString(String str) {
        if ("".equals(str)) {
            return null;
        }
        return str;
    }

    private static <T extends Enum<T>> T tryParseEnum(Class<T> enumType, String str) {
        try {
            return (T) Enum.valueOf(enumType, str.toUpperCase(Locale.US));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static <T> List<T> tryParseList(Function<String, T> elementParser, String str) {
        if ("".equals(str)) {
            return new ArrayList();
        }
        List<T> ret = new ArrayList<>();
        int p = 0;
        while (true) {
            StringBuilder sb = new StringBuilder();
            while (p < str.length() && str.charAt(p) != ',') {
                if (str.charAt(p) == '\\') {
                    p++;
                }
                if (p == str.length()) {
                    break;
                }
                sb.append(str.charAt(p));
                p++;
            }
            ret.add(elementParser.apply(sb.toString()));
            if (p == str.length()) {
                return ret;
            }
            p++;
        }
    }

    private static <T extends Enum<T>> List<T> tryParseEnumList(Class<T> enumType, String str) {
        String[] split;
        if ("".equals(str)) {
            return new ArrayList();
        }
        ArrayList arrayList = new ArrayList();
        for (String element : str.split(",")) {
            arrayList.add(tryParseEnum(enumType, element));
        }
        return arrayList;
    }

    private static String escape(String str) {
        return str.replaceAll("([\\\\,])", "\\\\$1");
    }

    private static <T> String formatList(List<T> list) {
        StringJoiner joiner = new StringJoiner(",");
        Iterator<T> it = list.iterator();
        while (it.hasNext()) {
            T element = it.next();
            joiner.add(element == null ? "" : escape(element.toString()));
        }
        return joiner.toString();
    }

    private static <T extends Enum<T>> String formatEnumList(List<T> list, Function<T, String> elementFormatter) {
        StringJoiner joiner = new StringJoiner(",");
        Iterator<T> it = list.iterator();
        while (it.hasNext()) {
            T element = it.next();
            joiner.add(element == null ? "" : elementFormatter.apply(element));
        }
        return joiner.toString();
    }

    public static Optional<Long> vsync_event_phase_offset_ns() {
        String value = SystemProperties.get("ro.surface_flinger.vsync_event_phase_offset_ns");
        return Optional.ofNullable(tryParseLong(value));
    }

    public static Optional<Long> vsync_sf_event_phase_offset_ns() {
        String value = SystemProperties.get("ro.surface_flinger.vsync_sf_event_phase_offset_ns");
        return Optional.ofNullable(tryParseLong(value));
    }

    public static Optional<Boolean> use_context_priority() {
        String value = SystemProperties.get("ro.surface_flinger.use_context_priority");
        return Optional.ofNullable(tryParseBoolean(value));
    }

    public static Optional<Long> max_frame_buffer_acquired_buffers() {
        String value = SystemProperties.get("ro.surface_flinger.max_frame_buffer_acquired_buffers");
        return Optional.ofNullable(tryParseLong(value));
    }

    public static Optional<Integer> max_graphics_width() {
        String value = SystemProperties.get("ro.surface_flinger.max_graphics_width");
        return Optional.ofNullable(tryParseInteger(value));
    }

    public static Optional<Integer> max_graphics_height() {
        String value = SystemProperties.get("ro.surface_flinger.max_graphics_height");
        return Optional.ofNullable(tryParseInteger(value));
    }

    public static Optional<Boolean> has_wide_color_display() {
        String value = SystemProperties.get("ro.surface_flinger.has_wide_color_display");
        return Optional.ofNullable(tryParseBoolean(value));
    }

    public static Optional<Boolean> running_without_sync_framework() {
        String value = SystemProperties.get("ro.surface_flinger.running_without_sync_framework");
        return Optional.ofNullable(tryParseBoolean(value));
    }

    public static Optional<Boolean> has_HDR_display() {
        String value = SystemProperties.get("ro.surface_flinger.has_HDR_display");
        return Optional.ofNullable(tryParseBoolean(value));
    }

    public static Optional<Long> present_time_offset_from_vsync_ns() {
        String value = SystemProperties.get("ro.surface_flinger.present_time_offset_from_vsync_ns");
        return Optional.ofNullable(tryParseLong(value));
    }

    public static Optional<Boolean> force_hwc_copy_for_virtual_displays() {
        String value = SystemProperties.get("ro.surface_flinger.force_hwc_copy_for_virtual_displays");
        return Optional.ofNullable(tryParseBoolean(value));
    }

    public static Optional<Long> max_virtual_display_dimension() {
        String value = SystemProperties.get("ro.surface_flinger.max_virtual_display_dimension");
        return Optional.ofNullable(tryParseLong(value));
    }

    public static Optional<Boolean> use_vr_flinger() {
        String value = SystemProperties.get("ro.surface_flinger.use_vr_flinger");
        return Optional.ofNullable(tryParseBoolean(value));
    }

    public static Optional<Boolean> start_graphics_allocator_service() {
        String value = SystemProperties.get("ro.surface_flinger.start_graphics_allocator_service");
        return Optional.ofNullable(tryParseBoolean(value));
    }

    /* loaded from: classes.dex */
    public enum primary_display_orientation_values {
        ORIENTATION_0("ORIENTATION_0"),
        ORIENTATION_90("ORIENTATION_90"),
        ORIENTATION_180("ORIENTATION_180"),
        ORIENTATION_270("ORIENTATION_270");
        
        private final String propValue;

        primary_display_orientation_values(String propValue) {
            this.propValue = propValue;
        }

        public String getPropValue() {
            return this.propValue;
        }
    }

    public static Optional<primary_display_orientation_values> primary_display_orientation() {
        String value = SystemProperties.get("ro.surface_flinger.primary_display_orientation");
        return Optional.ofNullable((primary_display_orientation_values) tryParseEnum(primary_display_orientation_values.class, value));
    }

    public static Optional<Boolean> use_color_management() {
        String value = SystemProperties.get("ro.surface_flinger.use_color_management");
        return Optional.ofNullable(tryParseBoolean(value));
    }

    public static Optional<Long> default_composition_dataspace() {
        String value = SystemProperties.get("ro.surface_flinger.default_composition_dataspace");
        return Optional.ofNullable(tryParseLong(value));
    }

    public static Optional<Integer> default_composition_pixel_format() {
        String value = SystemProperties.get("ro.surface_flinger.default_composition_pixel_format");
        return Optional.ofNullable(tryParseInteger(value));
    }

    public static Optional<Long> wcg_composition_dataspace() {
        String value = SystemProperties.get("ro.surface_flinger.wcg_composition_dataspace");
        return Optional.ofNullable(tryParseLong(value));
    }

    public static Optional<Integer> wcg_composition_pixel_format() {
        String value = SystemProperties.get("ro.surface_flinger.wcg_composition_pixel_format");
        return Optional.ofNullable(tryParseInteger(value));
    }

    public static Optional<Long> color_space_agnostic_dataspace() {
        String value = SystemProperties.get("ro.surface_flinger.color_space_agnostic_dataspace");
        return Optional.ofNullable(tryParseLong(value));
    }

    public static List<Double> display_primary_red() {
        String value = SystemProperties.get("ro.surface_flinger.display_primary_red");
        return tryParseList($$Lambda$SurfaceFlingerProperties$WvTfK7vefne56zQHS13TZl7XsQ.INSTANCE, value);
    }

    public static List<Double> display_primary_green() {
        String value = SystemProperties.get("ro.surface_flinger.display_primary_green");
        return tryParseList($$Lambda$SurfaceFlingerProperties$ujhN5VXpsRSABl9ZdmqOppPQ4.INSTANCE, value);
    }

    public static List<Double> display_primary_blue() {
        String value = SystemProperties.get("ro.surface_flinger.display_primary_blue");
        return tryParseList($$Lambda$SurfaceFlingerProperties$jxj1aANBXH483fjcw36qrxGbYu8.INSTANCE, value);
    }

    public static List<Double> display_primary_white() {
        String value = SystemProperties.get("ro.surface_flinger.display_primary_white");
        return tryParseList($$Lambda$SurfaceFlingerProperties$uEcQnNtGA6nu3Sq3OLtSVMFDQrc.INSTANCE, value);
    }

    @Deprecated
    public static Optional<Boolean> refresh_rate_switching() {
        String value = SystemProperties.get("ro.surface_flinger.refresh_rate_switching");
        return Optional.ofNullable(tryParseBoolean(value));
    }

    public static Optional<Integer> set_idle_timer_ms() {
        String value = SystemProperties.get("ro.surface_flinger.set_idle_timer_ms");
        return Optional.ofNullable(tryParseInteger(value));
    }

    public static Optional<Integer> set_touch_timer_ms() {
        String value = SystemProperties.get("ro.surface_flinger.set_touch_timer_ms");
        return Optional.ofNullable(tryParseInteger(value));
    }

    public static Optional<Integer> set_display_power_timer_ms() {
        String value = SystemProperties.get("ro.surface_flinger.set_display_power_timer_ms");
        return Optional.ofNullable(tryParseInteger(value));
    }

    public static Optional<Boolean> use_content_detection_for_refresh_rate() {
        String value = SystemProperties.get("ro.surface_flinger.use_content_detection_for_refresh_rate");
        return Optional.ofNullable(tryParseBoolean(value));
    }

    @Deprecated
    public static Optional<Boolean> use_smart_90_for_video() {
        String value = SystemProperties.get("ro.surface_flinger.use_smart_90_for_video");
        return Optional.ofNullable(tryParseBoolean(value));
    }

    public static Optional<Boolean> enable_protected_contents() {
        String value = SystemProperties.get("ro.surface_flinger.protected_contents");
        return Optional.ofNullable(tryParseBoolean(value));
    }

    public static Optional<Boolean> support_kernel_idle_timer() {
        String value = SystemProperties.get("ro.surface_flinger.support_kernel_idle_timer");
        return Optional.ofNullable(tryParseBoolean(value));
    }

    public static Optional<Boolean> supports_background_blur() {
        String value = SystemProperties.get("ro.surface_flinger.supports_background_blur");
        return Optional.ofNullable(tryParseBoolean(value));
    }

    public static Optional<Boolean> use_frame_rate_api() {
        String value = SystemProperties.get("ro.surface_flinger.use_frame_rate_api");
        return Optional.ofNullable(tryParseBoolean(value));
    }

    public static Optional<Integer> display_update_imminent_timeout_ms() {
        String value = SystemProperties.get("ro.surface_flinger.display_update_imminent_timeout_ms");
        return Optional.ofNullable(tryParseInteger(value));
    }
}