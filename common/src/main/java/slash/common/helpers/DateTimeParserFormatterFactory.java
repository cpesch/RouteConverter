package slash.common.helpers;

import java.text.DateFormat;

public class DateTimeParserFormatterFactory {

    public static DateTimeParserFormatter createDateTimeFormat() {
        return new LegacyParserFormatter(() -> DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM));
    }

    public static DateTimeParserFormatter createDateFormat() {
        return new LegacyParserFormatter(() -> DateFormat.getDateInstance(DateFormat.SHORT));
    }

    public static DateTimeParserFormatter createTimeFormat() {
        return new LegacyParserFormatter(() -> DateFormat.getTimeInstance(DateFormat.MEDIUM));
    }
}
