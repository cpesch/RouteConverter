package slash.common.helpers;

import java.util.Locale;

public class DateTimeParserFormatterFactory {

    enum FormatterType {
        /**
         * uses the legacy DateTime-API with the Locale that Java automatically sets at startup
         */
        LEGACY_SYSTEMLOCALE,

        /**
         * uses the legacy DateTime-API with the Locale that configured in RouteConverter
         */
        LEGACY_ROUTECONVERTER_LOCALE
    }

    private static final Locale startupLocale = getCurrentLocale();
    private static final FormatterType formatterType = FormatterType.LEGACY_ROUTECONVERTER_LOCALE;

    public static DateTimeParserFormatter createDateTimeFormat() {
        return switch(formatterType) {
            case LEGACY_ROUTECONVERTER_LOCALE -> new LegacyParserFormatter(LegacyParserFormatter.ParserType.DATETIME, () -> getCurrentLocale());
            case LEGACY_SYSTEMLOCALE -> new LegacyParserFormatter(LegacyParserFormatter.ParserType.DATETIME, () -> startupLocale);
        };
    }

    public static DateTimeParserFormatter createDateFormat() {
        return switch(formatterType) {
            case LEGACY_ROUTECONVERTER_LOCALE -> new LegacyParserFormatter(LegacyParserFormatter.ParserType.DATE, () -> getCurrentLocale());
            case LEGACY_SYSTEMLOCALE -> new LegacyParserFormatter(LegacyParserFormatter.ParserType.DATE, () -> startupLocale);
        };
    }

    public static DateTimeParserFormatter createTimeFormat() {
        return switch(formatterType) {
            case LEGACY_ROUTECONVERTER_LOCALE -> new LegacyParserFormatter(LegacyParserFormatter.ParserType.TIME, () -> getCurrentLocale());
            case LEGACY_SYSTEMLOCALE -> new LegacyParserFormatter(LegacyParserFormatter.ParserType.TIME, () -> startupLocale);
        };
    }

    private static Locale getCurrentLocale() {
        final Locale currentLocale = Locale.getDefault(Locale.Category.FORMAT);
        return currentLocale.equals(Locale.ROOT) ? startupLocale : currentLocale;
    }
}
