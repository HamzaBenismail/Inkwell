package app.inkwell.utilities;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public class SlugUtils {
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern MULTIPLE_HYPHENS = Pattern.compile("-+");

    public static String createSlug(String input) {
        if (input == null) {
            return "";
        }

        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        slug = MULTIPLE_HYPHENS.matcher(slug).replaceAll("-");
        slug = slug.toLowerCase(Locale.ENGLISH);
        slug = slug.replaceAll("^-|-$", "");
        return slug;
    }
}