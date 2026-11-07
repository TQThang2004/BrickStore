package com.brickstore.common.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugUtils {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern MULTI_DASH = Pattern.compile("-+");

    private SlugUtils() {
    }

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        // Thay thế ký tự Đ/đ của tiếng Việt
        String normalizedInput = input.replace("Đ", "D").replace("đ", "d");

        // Chuẩn hóa Unicode NFD để loại bỏ dấu thanh
        String nowhitespace = WHITESPACE.matcher(normalizedInput).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NON_LATIN.matcher(normalized).replaceAll("");
        slug = MULTI_DASH.matcher(slug).replaceAll("-");

        return slug.toLowerCase(Locale.ENGLISH).replaceAll("^-|-$", "");
    }
}

