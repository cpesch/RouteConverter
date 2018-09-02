package slash.navigation.nmea;

import slash.navigation.base.RouteCharacteristics;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.ValueAndOrientation;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static slash.common.io.Transfer.escape;
import static slash.common.io.Transfer.parseDouble;
import static slash.common.io.Transfer.toMixedCase;
import static slash.common.io.Transfer.trim;

/**
 * Reads and writes Magellan Explorist (.log) files.
 *
 * Header: $PMGNFMT,%TRK,LAT,HEMI,LON,HEMI,ALT,UNIT,TIME,VALID,NAME,%META,ASCII
 * Format: $PMGNTRK,4914.967,N,00651.208,E,000199,M,152224,A,KLLERTAL-RADWEG,210307*48
 *
 * @author Christian Pesch
 */

public class MagellanExploristFormat extends BaseNmeaFormat {
    private static final String HEADER_LINE = "$PMGNFMT,%TRK,LAT,HEMI,LON,HEMI,ALT,UNIT,TIME,VALID,NAME,%META,ASCII";
    
    private static final Pattern TRK_PATTERN = Pattern.
            compile("^\\$PMGNTRK" + SEPARATOR +
                    "([\\d\\.]+)" + SEPARATOR + "([NS])" + SEPARATOR +
                    "([\\d\\.]+)" + SEPARATOR + "([WE])" + SEPARATOR +
                    "(-?[\\d\\.]+)" + SEPARATOR +
                    "M" + SEPARATOR +
                    "([\\d\\.]*)" + SEPARATOR +     // UTC Time, hhmmss
                    "[A]" + SEPARATOR +
                    "(.*)" + SEPARATOR +            // description,
                    "(\\d*)" +                      // Date, ddmmyy
                    END_OF_LINE);

    private static final NumberFormat ALTITUDE_NUMBER_FORMAT = DecimalFormat.getNumberInstance(Locale.US);

    static {
        ALTITUDE_NUMBER_FORMAT.setGroupingUsed(false);
        ALTITUDE_NUMBER_FORMAT.setMinimumFractionDigits(0);
        ALTITUDE_NUMBER_FORMAT.setMaximumFractionDigits(0);
        ALTITUDE_NUMBER_FORMAT.setMinimumIntegerDigits(6);
        ALTITUDE_NUMBER_FORMAT.setMaximumIntegerDigits(6);
    }

    public String getExtension() {
        return ".log";
    }

    public String getName() {
        return "Magellan Explorist (*" + getExtension() + ")";
    }

    @SuppressWarnings("unchecked")
    public <P extends NavigationPosition> NmeaRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new NmeaRoute(this, characteristics, (List<NmeaPosition>) positions);
    }

    protected boolean isPosition(String line) {
        Matcher matcher = TRK_PATTERN.matcher(line);
        return matcher.matches() && hasValidChecksum(line);
    }

    protected NmeaPosition parsePosition(String line) {
        Matcher matcher = TRK_PATTERN.matcher(line);
        if (matcher.matches()) {
            String latitude = matcher.group(1);
            String northOrSouth = matcher.group(2);
            String longitude = matcher.group(3);
            String westOrEast = matcher.group(4);
            String altitude = matcher.group(5);
            String time = matcher.group(6);
            String description = toMixedCase(matcher.group(7));
            String date = matcher.group(8);
            return new NmeaPosition(parseDouble(longitude), westOrEast, parseDouble(latitude), northOrSouth,
                    parseDouble(altitude), null, null, parseDateAndTime(date, time), trim(description));
        }

        throw new IllegalArgumentException("'" + line + "' does not match");
    }

    protected void writeHeader(PrintWriter writer) {
        writer.println(HEADER_LINE);
    }

    private String formatAltitude(Double aDouble) {
        if (aDouble == null)
            return "0";
        return ALTITUDE_NUMBER_FORMAT.format(aDouble);
    }

    protected void writePosition(NmeaPosition position, PrintWriter writer) {
        ValueAndOrientation longitudeAsValueAndOrientation = position.getLongitudeAsValueAndOrientation();
        String longitude = formatLongitude(longitudeAsValueAndOrientation.getValue());
        String westOrEast = longitudeAsValueAndOrientation.getOrientation().value();
        ValueAndOrientation latitudeAsValueAndOrientation = position.getLatitudeAsValueAndOrientation();
        String latitude = formatLatitude(latitudeAsValueAndOrientation.getValue());
        String northOrSouth = latitudeAsValueAndOrientation.getOrientation().value();
        String description = escape(position.getDescription(), SEPARATOR, ';');
        String time = formatTime(position.getTime());
        String date = formatDate(position.getTime());
        String altitude = formatAltitude(position.getElevation());

        String trk = "PMGNTRK" + SEPARATOR +
                latitude + SEPARATOR + northOrSouth + SEPARATOR + longitude + SEPARATOR + westOrEast + SEPARATOR +
                altitude + SEPARATOR + "M" + SEPARATOR + time + SEPARATOR + "A" + SEPARATOR +
                description + SEPARATOR + date;
        writeSentence(writer, trk);
    }

    protected void writeFooter(PrintWriter writer) {
        writeSentence(writer, "PMGNCMD,END");
    }
}
