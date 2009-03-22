package slash.navigation.nmea;

import slash.navigation.BaseNavigationPosition;
import slash.navigation.RouteCharacteristics;
import slash.navigation.util.Conversion;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads and writes Magellan Explorist (.log) files.
 *
 * @author Christian Pesch
 */

public class MagellanExploristFormat extends BaseNmeaFormat {
    static {
        log = Logger.getLogger(MagellanExploristFormat.class.getName());
    }

    // $PMGNTRK,4914.967,N,00651.208,E,000199,M,152224,EARTH_RADIUS,KLLERTAL-RADWEG,210307*48
    private static final Pattern PMGNTRK_PATTERN = Pattern.
            compile("^\\$PMGNTRK" + SEPARATOR +
                    "([\\d\\.]+)" + SEPARATOR + "([NS])" + SEPARATOR +
                    "([\\d\\.]+)" + SEPARATOR + "([WE])" + SEPARATOR +
                    "(-?[\\d\\.]+)" + SEPARATOR +     // Elevation
                    "M" + SEPARATOR +
                    "([\\d\\.]*)" + SEPARATOR +     // UTC Time, hhmmss
                    "[A]" + SEPARATOR +
                    "(.*)" + SEPARATOR +            // Comment,
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

    public <P extends BaseNavigationPosition> NmeaRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new NmeaRoute(this, characteristics, (List<NmeaPosition>) positions);
    }

    protected boolean isPosition(String line) {
        Matcher trkMatcher = PMGNTRK_PATTERN.matcher(line);
        return trkMatcher.matches() && hasValidChecksum(line);
    }

    protected NmeaPosition parsePosition(String line) {
        Matcher trkMatcher = PMGNTRK_PATTERN.matcher(line);
        if (trkMatcher.matches()) {
            String latitude = trkMatcher.group(1);
            String northOrSouth = trkMatcher.group(2);
            String longitude = trkMatcher.group(3);
            String westOrEast = trkMatcher.group(4);
            String altitude = trkMatcher.group(5);
            String time = trkMatcher.group(6);
            String comment = trkMatcher.group(7);
            if (comment != null && comment.toUpperCase().equals(comment))
                comment = Conversion.toMixedCase(comment);
            String date = trkMatcher.group(8);
            return new NmeaPosition(Conversion.parseDouble(longitude), westOrEast, Conversion.parseDouble(latitude), northOrSouth,
                    Double.parseDouble(altitude), null, parseDateAndTime(date, time), Conversion.trim(comment));
        }

        throw new IllegalArgumentException("'" + line + "' does not match");
    }

    private String formatAltitude(Double aDouble) {
        if (aDouble == null)
            return "0";
        return ALTITUDE_NUMBER_FORMAT.format(aDouble);
    }

    protected void writePosition(NmeaPosition position, PrintWriter writer, int index) {
        String longitude = formatLongitude(position.getLongitudeAsDdmm());
        String westOrEast = position.getWestOrEast();
        String latitude = formatLatititude(position.getLatitudeAsDdmm());
        String northOrSouth = position.getNorthOrSouth();
        String comment = formatComment(position.getComment());
        String time = formatTime(position.getTime());
        String date = formatDate(position.getTime());
        String altitude = formatAltitude(position.getElevation());

        // $PMGNTRK,4914.967,N,00651.208,E,000199,M,152224,EARTH_RADIUS,KLLERTAL-RADWEG,210307*48
        String trk = "PMGNTRK" + SEPARATOR +
                latitude + SEPARATOR + northOrSouth + SEPARATOR + longitude + SEPARATOR + westOrEast + SEPARATOR +
                altitude + SEPARATOR + "M" + SEPARATOR + time + SEPARATOR + "A" + SEPARATOR +
                comment + SEPARATOR + date;
        writeSentence(writer, trk);
    }

    protected void writeFooter(PrintWriter writer) {
        // $PMGNCMD,END*3D 
        writeSentence(writer, "PMGNCMD,END");
    }
}
