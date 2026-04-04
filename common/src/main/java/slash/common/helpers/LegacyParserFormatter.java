package slash.common.helpers;

import slash.common.type.CompactCalendar;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Supplier;

public class LegacyParserFormatter implements DateTimeParserFormatter {

    private final Supplier<DateFormat> formatSupplier;
    private DateFormat currentFormat;
    private Locale formatLocale;
    private String formatZone;

    public LegacyParserFormatter(Supplier<DateFormat> formatSupplier) {
        this.formatSupplier = formatSupplier;
    }

    @Override
    public String format(CompactCalendar timeStamp) {
        refreshIfNeeded();

        return currentFormat.format(timeStamp.getTime());
    }

    @Override
    public Calendar parse(String stringValue) throws DateTimeParserException {
        refreshIfNeeded();

        try {
            Date parsed = currentFormat.parse(stringValue);
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(formatZone));
            calendar.setTime(parsed);
            return calendar;
        } catch (ParseException e) {
            throw new DateTimeParserException(e);
        }
    }

    @Override
    public String getPatternInfo() {
        refreshIfNeeded();

        if (currentFormat instanceof SimpleDateFormat simpleDateFormat) {
            return simpleDateFormat.toLocalizedPattern();
        }

        return currentFormat.toString();
    }

    @Override
    public void setZone(String timeZone) {
        refreshIfNeeded();

        if (! Objects.equals(timeZone, formatZone)) {
            currentFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
            formatZone = timeZone;
        }
    }

    private void refreshIfNeeded() {
        if (currentFormat == null || ! Objects.equals(formatLocale, Locale.getDefault())) {
            currentFormat = formatSupplier.get();
            formatLocale = Locale.getDefault();

            if (formatZone != null) {
                currentFormat.setTimeZone(TimeZone.getTimeZone(formatZone));
            }
        }
    }
}
