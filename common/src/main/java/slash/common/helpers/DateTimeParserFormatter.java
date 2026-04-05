package slash.common.helpers;

import slash.common.type.CompactCalendar;
import java.util.Calendar;

public interface DateTimeParserFormatter {

    String format(CompactCalendar timeStamp);

    /**
     * Reads a string in the given format
     * @param stringValue String to parse
     * @param referenceTimestamp Timestamp to be used for values not included in the pattern
     * @return The complete read timestamp
     * @throws DateTimeParserException on parser errors
     */
    Calendar parse(String stringValue, CompactCalendar referenceTimestamp) throws DateTimeParserException;

    /**
     * @return A description of the underlying pattern. It doesn't necessarily have to be the pattern string itself (if this cannot be determined - e.g., with the new Time API). It can also be an example format.
     */
    String getPatternInfo();

    void setZone(String timeZone);
}
