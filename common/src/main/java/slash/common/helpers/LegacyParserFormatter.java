package slash.common.helpers;

import slash.common.type.CompactCalendar;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Supplier;

class LegacyParserFormatter implements DateTimeParserFormatter {

    enum ParserType {
        DATETIME,
        TIME,
        DATE
    }

    private final ParserType parserType;
    private final Supplier<Locale> localeSupplier;
    private DateFormat currentFormat;
    private DateFormat currentParser;
    private Locale formatLocale;
    private String formatZone;

    public LegacyParserFormatter(ParserType parserType,
                                 Supplier<Locale> localeSupplier) {
        this.parserType = parserType;
        this.localeSupplier = localeSupplier;
    }

    @Override
    public String format(CompactCalendar timeStamp) {
        refreshIfNeeded();

        return currentFormat.format(timeStamp.getTime());
    }

    @Override
    public Calendar parse(String stringValue, CompactCalendar referenceTimestamp) throws DateTimeParserException {
        refreshIfNeeded();

        try {
            Date parsed = currentParser.parse(stringValue);
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(formatZone));
            calendar.setTime(parsed);

            if (referenceTimestamp != null) {
                final Calendar referenceCal = referenceTimestamp.getCalendar();
                referenceCal.setTimeZone(calendar.getTimeZone());

                if (parserType == ParserType.DATE) {
                    calendar.set(Calendar.HOUR_OF_DAY, referenceCal.get(Calendar.HOUR_OF_DAY));
                    calendar.set(Calendar.MINUTE, referenceCal.get(Calendar.MINUTE));
                    calendar.set(Calendar.SECOND, referenceCal.get(Calendar.SECOND));
                    calendar.set(Calendar.MILLISECOND, referenceCal.get(Calendar.MILLISECOND));
                }

                if (parserType == ParserType.TIME) {
                    calendar.set(Calendar.YEAR, referenceCal.get(Calendar.YEAR));
                    calendar.set(Calendar.MONTH, referenceCal.get(Calendar.MONTH));
                    calendar.set(Calendar.DAY_OF_MONTH, referenceCal.get(Calendar.DAY_OF_MONTH));
                }
            }

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
            TimeZone zone = TimeZone.getTimeZone(timeZone);
            currentFormat.setTimeZone(zone);
            currentParser.setTimeZone(zone);
            formatZone = timeZone;
        }
    }

    private void refreshIfNeeded() {
        if (currentFormat == null || ! Objects.equals(formatLocale, localeSupplier.get())) {
            currentFormat = createFormatter();

            if (currentFormat instanceof SimpleDateFormat simpleDateFormat) {
                String pattern = simpleDateFormat.toLocalizedPattern();
                pattern = pattern.replaceAll("(?<!y)y(?!y)", "yy");
                currentParser = new SimpleDateFormat(pattern);
            }
            else {
                currentParser = createFormatter();
            }

            formatLocale = localeSupplier.get();

            if (formatZone != null) {
                final TimeZone timeZone = TimeZone.getTimeZone(formatZone);
                currentFormat.setTimeZone(timeZone);
                currentParser.setTimeZone(timeZone);
            }
        }
    }

    private DateFormat createFormatter() {
        return switch (parserType) {
            case DATETIME -> DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, localeSupplier.get());
            case DATE -> DateFormat.getDateInstance(DateFormat.SHORT, localeSupplier.get());
            case TIME -> DateFormat.getTimeInstance(DateFormat.MEDIUM, localeSupplier.get());
        };
    }
}
