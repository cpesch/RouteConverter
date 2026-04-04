package slash.common.helpers;

import slash.common.type.CompactCalendar;
import java.util.Calendar;

public interface DateTimeParserFormatter {

    String format(CompactCalendar timeStamp);

    Calendar parse(String stringValue) throws DateTimeParserException;

    String getPatternInfo();

    void setZone(String timeZone);
}
