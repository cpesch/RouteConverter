package slash.navigation.nmea;

import slash.navigation.BaseNavigationPosition;
import slash.navigation.RouteCharacteristics;
import slash.navigation.util.Conversion;

import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.NumberFormat;
import java.text.DecimalFormat;

/**
 * Reads and writes Magellan Route (.rte) files.
 * <p/>
 * Header: $PMGNFMT,%RTE,NUM_MSG,ID,FLAG,NUM,NAME,WPT_NAME1,ICON1,WPT_NAME2,ICON2,CHKSUM ?%WPL,LAT,HEMI,LON,HEMI,ALT,UNIT,NAME,MSG,ICON,CHKSUM,%META,ASCII<br/>
 * Format: $PMGNWPL,4809.43440,N,01135.06121,E,0,M,Muenchner-Freiheit,,a*10<br/>
 *         $PMGNRTE,3,1,c,1,Muenchen_Route,Muenchner-Freiheit,a,Engl-Garten-1,a*60
 *
 * @author Christian Pesch
 */

public class MagellanRouteFormat extends BaseNmeaFormat {
    static {
        log = Logger.getLogger(MagellanRouteFormat.class.getName());
    }

    private static final NumberFormat LONGITUDE_NUMBER_FORMAT = DecimalFormat.getNumberInstance(Locale.US);
    private static final NumberFormat LATITUDE_NUMBER_FORMAT = DecimalFormat.getNumberInstance(Locale.US);

    static {
        LONGITUDE_NUMBER_FORMAT.setGroupingUsed(false);
        LONGITUDE_NUMBER_FORMAT.setMinimumFractionDigits(5);
        LONGITUDE_NUMBER_FORMAT.setMaximumFractionDigits(5);
        LONGITUDE_NUMBER_FORMAT.setMinimumIntegerDigits(5);
        LONGITUDE_NUMBER_FORMAT.setMaximumIntegerDigits(5);
        LATITUDE_NUMBER_FORMAT.setGroupingUsed(false);
        LATITUDE_NUMBER_FORMAT.setMinimumFractionDigits(5);
        LATITUDE_NUMBER_FORMAT.setMaximumFractionDigits(5);
        LATITUDE_NUMBER_FORMAT.setMinimumIntegerDigits(4);
        LATITUDE_NUMBER_FORMAT.setMaximumIntegerDigits(4);
    }

    private static final String HEADER = "$PMGNFMT,%RTE,NUM_MSG,ID,FLAG,NUM,NAME,WPT_NAME1,ICON1,WPT_NAME2,ICON2,CHKSUM ?%WPL,LAT,HEMI,LON,HEMI,ALT,UNIT,NAME,MSG,ICON,CHKSUM,%META,ASCII";

    private static final Pattern WPL_PATTERN = Pattern.
            compile("^\\$PMGNWPL" + SEPARATOR +
                    "([\\d\\.]+)" + SEPARATOR + "([NS])" + SEPARATOR +
                    "([\\d\\.]+)" + SEPARATOR + "([WE])" + SEPARATOR +
                    "(-?[\\d\\.]+)" + SEPARATOR +
                    "M" + SEPARATOR +
                    "([^" + SEPARATOR + "]*)" + SEPARATOR +          // Comment
                    "[^" + SEPARATOR + "]*" + SEPARATOR +            // copy of the comment above
                    "a" +
                    END_OF_LINE);

    public String getExtension() {
        return ".rte";
    }

    public String getName() {
        return "Magellan Route (*" + getExtension() + ")";
    }

    protected RouteCharacteristics getCharacteristics() {
        return RouteCharacteristics.Route;
    }

    @SuppressWarnings({"unchecked"})
    public <P extends BaseNavigationPosition> NmeaRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new NmeaRoute(this, characteristics, (List<NmeaPosition>) positions);
    }

    protected boolean isPosition(String line) {
        Matcher matcher = WPL_PATTERN.matcher(line);
        return matcher.matches() && hasValidChecksum(line);
    }

    protected NmeaPosition parsePosition(String line) {
        Matcher matcher = WPL_PATTERN.matcher(line);
        if (matcher.matches()) {
            String latitude = matcher.group(1);
            String northOrSouth = matcher.group(2);
            String longitude = matcher.group(3);
            String westOrEast = matcher.group(4);
            String altitude = matcher.group(5);
            String comment = matcher.group(6);
            if (comment != null && comment.toUpperCase().equals(comment))
                comment = Conversion.toMixedCase(comment);
            return new NmeaPosition(Conversion.parseDouble(longitude), westOrEast, Conversion.parseDouble(latitude), northOrSouth,
                    Double.parseDouble(altitude), null, null, Conversion.trim(comment));
        }

        throw new IllegalArgumentException("'" + line + "' does not match");
    }


    protected String formatLongitude(Double longitude) {
        if (longitude == null)
            return "";
        return LONGITUDE_NUMBER_FORMAT.format(longitude);
    }

    protected String formatLatititude(Double latitude) {
        if (latitude == null)
            return "";
        return LATITUDE_NUMBER_FORMAT.format(latitude);
    }

    String formatName(String name) {
        if (name != null) {
            /* TODO really encode route and waypoint names?
            StringBuffer buffer = new StringBuffer(name.toLowerCase().trim().replaceAll(" ", "-"));
            /*
            int i = 0;
            while (i < buffer.length()) {
                char c = buffer.charAt(i);
                if ((c < 'a' || c > 'z') && c != '-') {
                    buffer.deleteCharAt(i);
                }
                i++;
            }
            if (buffer.length() > 0)
                return buffer.toString().substring(0, Math.min(buffer.length(), 8));
            */
            return formatComment(name);
        }
        return "route01";
    }

    public void write(NmeaRoute route, PrintWriter writer, int startIndex, int endIndex) {
        writeHeader(writer);

        List<NmeaPosition> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            NmeaPosition position = positions.get(i);
            writePosition(position, writer, i);
        }

        String routeName = formatName(route.getName());
        int count = Conversion.ceiling(endIndex - startIndex, 2, true);
        for (int i = startIndex; i < endIndex; i+=2) {
            NmeaPosition start = positions.get(i);
            NmeaPosition end = positions.size() > i+1 ? positions.get(i+1) : null;
            writeRte(start, end, writer, count, i / 2, routeName);
        }

        writeFooter(writer);
    }

    protected void writeHeader(PrintWriter writer) {
        writer.println(HEADER);
    }

    protected void writePosition(NmeaPosition position, PrintWriter writer, int index) {
        String longitude = formatLongitude(position.getLongitudeAsDdmm());
        String westOrEast = position.getWestOrEast();
        String latitude = formatLatititude(position.getLatitudeAsDdmm());
        String northOrSouth = position.getNorthOrSouth();
        String comment = formatName(position.getComment());
        String altitude = Conversion.formatIntAsString(position.getElevation() != null ? position.getElevation().intValue() : null);

        String wpl = "PMGNWPL" + SEPARATOR +
                latitude + SEPARATOR + northOrSouth + SEPARATOR + longitude + SEPARATOR + westOrEast + SEPARATOR +
                altitude + SEPARATOR + "M" + SEPARATOR + comment + SEPARATOR + SEPARATOR + "a";
        writeSentence(writer, wpl);
    }

    private void writeRte(NmeaPosition start, NmeaPosition end, PrintWriter writer, int count, int index, String routeName) {
        String startName = formatName(start.getComment());

        String rte = "PMGNRTE" + SEPARATOR + count + SEPARATOR + (index+1) + SEPARATOR +
                "c" + SEPARATOR + "01" + SEPARATOR + routeName + SEPARATOR + startName + SEPARATOR + "a";
        if(end != null) {
            String endName = formatName(end.getComment());
            rte += SEPARATOR + endName + SEPARATOR + "a";
        }
        writeSentence(writer, rte);
    }

    protected void writeFooter(PrintWriter writer) {
        writeSentence(writer, "PMGNCMD,END");
    }
}